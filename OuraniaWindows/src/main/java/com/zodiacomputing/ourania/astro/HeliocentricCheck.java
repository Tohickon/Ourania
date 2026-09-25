package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * The heliocentric chart, held against the geocentric one it is the other end of.
 *
 * <p><b>Part B is the assertion this suite exists for.</b> Everything else here could pass with a
 * heliocentric frame that was quietly wrong by a constant, because there is nothing familiar to
 * check a heliocentric longitude against by eye. But the Earth seen from the Sun and the Sun seen
 * from the Earth are one measurement read from opposite ends, so Earth must sit at exactly the
 * geocentric Sun plus 180&deg; - and that ties the new frame to a geocentric frame the app has
 * been checking since it was written.
 *
 * <p><b>Part C proves a claim the class makes in prose.</b> {@link Heliocentric} says nothing is
 * ever retrograde and explains why; a comment that says so is worth nothing, so this walks ten
 * years and looks. It also walks the geocentric case alongside, because an assertion that
 * everything is direct would pass just as well against a frame that had silently lost its speeds.
 */
public final class HeliocentricCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double birth = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);

        part("A: which bodies exist from the Sun", HeliocentricCheck::rule);
        part("B: the Earth is the geocentric Sun, read from the other end",
            () -> earthOpposesSun(sw));
        part("C: nothing is retrograde, and the speeds are real", () -> direct(sw, birth));
        part("D: no body is plotted at the origin", () -> noOrigin(sw, birth));
        part("E: the aspects", () -> aspects(sw, birth));
        part("F: one rule, one list", HeliocentricCheck::oneRule);
        part("G: the zodiac in force applies", () -> zodiac(sw, birth));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    // ------------------------------------------------------------------ A

    private static Bodies.Def byId(String id) {
        for (int i = 0; i < Bodies.count(); i++) {
            if (id.equals(Bodies.at(i).id)) {
                return Bodies.at(i);
            }
        }
        return null;
    }

    private static void rule() {
        // Out, each for its own reason - and the reason is asserted too, because a body dropped
        // with no explanation reads to a maintainer as a defect to be fixed.
        for (String id : new String[] {"sun", "moon", "north_node", "lilith"}) {
            Bodies.Def d = byId(id);
            ok(id + " is registered at all", d != null);
            if (d == null) {
                continue;
            }
            ok(id + " has no heliocentric position",
                !Heliocentric.hasHeliocentricPosition(d));
            ok("and says why", Heliocentric.whyExcluded(d) != null
                && Heliocentric.whyExcluded(d).length() > 20);
        }

        // In. Chiron and the asteroids matter here: the tempting wrong rule is "only the
        // classical planets", and they orbit the Sun exactly as the planets do.
        for (String id : new String[] {"mercury", "venus", "mars", "jupiter", "saturn",
                                       "uranus", "neptune", "pluto", "chiron"}) {
            Bodies.Def d = byId(id);
            ok(id + " is registered at all", d != null);
            if (d == null) {
                continue;
            }
            ok(id + " has a heliocentric position", Heliocentric.hasHeliocentricPosition(d));
            ok("and no exclusion reason", Heliocentric.whyExcluded(d) == null);
        }

        // Every derived point goes, together, for the one reason that covers them all.
        int derived = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if (d.source != Bodies.Source.EPHEMERIS) {
                derived++;
                ok(d.name + " is out", !Heliocentric.hasHeliocentricPosition(d));
            }
        }
        ok("there were derived points to exclude", derived > 5);
    }

    // ------------------------------------------------------------------ B

    private static void earthOpposesSun(SwissEph sw) {
        // Across a long span and at irregular steps, so a coincidence at one date cannot carry it.
        double start = SweDate.getJulDay(1900, 1, 1, 0.0);
        int tested = 0;
        double worst = 0.0;
        for (double jd = start; jd < start + 55000; jd += 379.3) {
            Heliocentric.Frame f = Heliocentric.compute(sw, jd);
            Heliocentric.Place earth = f.byName(Heliocentric.EARTH);
            if (earth == null || !earth.ok) {
                ok("the Earth answered at jd " + (long) jd, false);
                continue;
            }
            double[] xx = new double[6];
            StringBuffer err = new StringBuffer();
            int flags = Ephemeris.flags(sw, SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED);
            if (sw.swe_calc_ut(jd, SweConst.SE_SUN, flags, xx, err) < 0) {
                continue;
            }
            double delta = Math.abs(Aspects.separation(earth.longitude, Zodiac.normalise(xx[0]))
                - 180.0);
            worst = Math.max(worst, delta);
            tested++;
        }
        ok("a century and a half was walked", tested > 130);
        // Arcsecond scale. Not zero: the two calls round independently.
        ok("the Earth is opposite the geocentric Sun everywhere, to an arcsecond "
            + "(worst " + String.format("%.6f", worst) + " deg)", worst < 1.0 / 3600.0);

        // <b>The Sun itself must not appear.</b> The registry loop that inserts the Earth runs on
        // the Sun's entry, and swapping one line there would insert both.
        Heliocentric.Frame f = Heliocentric.compute(sw, start);
        ok("the Sun is not in its own chart", f.byName("Sun") == null);
        ok("but the Earth is", f.byName(Heliocentric.EARTH) != null);
        ok("and the Moon is not", f.byName("Moon") == null);
    }

    // ------------------------------------------------------------------ C

    private static void direct(SwissEph sw, double birth) {
        int helioRetro = 0;
        int geoRetro = 0;
        int samples = 0;
        int flags = Ephemeris.flags(sw, SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED);
        for (double jd = birth; jd < birth + 3650; jd += 10) {
            Heliocentric.Frame f = Heliocentric.compute(sw, jd);
            for (Heliocentric.Place p : f.places) {
                if (!p.ok) {
                    continue;
                }
                samples++;
                if (p.speed < 0) {
                    helioRetro++;
                }
                // A speed of exactly zero over a ten-year walk would mean the field was never
                // filled, which the no-retrograde assertion alone cannot tell from success.
                if (p.speed == 0.0) {
                    ok(p.name + " is actually moving at jd " + (long) jd, false);
                }
            }
            double[] xx = new double[6];
            StringBuffer err = new StringBuffer();
            for (int ipl : new int[] {SweConst.SE_MERCURY, SweConst.SE_MARS, SweConst.SE_SATURN}) {
                if (sw.swe_calc_ut(jd, ipl, flags, xx, err) >= 0 && xx[3] < 0) {
                    geoRetro++;
                }
            }
        }
        ok("ten years of every body were walked", samples > 4000);
        ok("nothing is ever retrograde from the Sun (" + helioRetro + " of " + samples + ")",
            helioRetro == 0);
        // The control: the same walk geocentrically must find plenty, or the test above is
        // passing because the speeds are not being read at all.
        ok("the same walk finds retrogrades from the Earth (" + geoRetro + ")", geoRetro > 200);
    }

    // ------------------------------------------------------------------ D

    private static void noOrigin(SwissEph sw, double birth) {
        Heliocentric.Frame f = Heliocentric.compute(sw, birth);
        ok("there are bodies", f.places.size() >= 10);
        for (Heliocentric.Place p : f.places) {
            if (!p.ok) {
                continue;
            }
            // The Sun's own answer is longitude 0 at distance 0. Anything that reaches the chart
            // holding that is not a body, and must not be drawn at 0 degrees Aries.
            ok(p.name + " is somewhere, at a distance", p.distanceAu > 0.0);
            ok(p.name + " has a real longitude", p.longitude >= 0.0 && p.longitude < 360.0);
        }
        // Distances put the bodies in orbital order, which is the one ordering a heliocentric
        // chart can assert and a geocentric one cannot.
        double mercury = f.byName("Mercury").distanceAu;
        double earth = f.byName(Heliocentric.EARTH).distanceAu;
        double jupiter = f.byName("Jupiter").distanceAu;
        ok("Mercury is inside the Earth's orbit", mercury < earth);
        ok("Jupiter is outside it", jupiter > earth);
        ok("and the Earth is about one AU away", Math.abs(earth - 1.0) < 0.02);

        // <b>Why the origin guard exists, measured rather than asserted.</b> No registered body
        // reaches that branch, so nothing above exercises it; this shows the premise is real. Ask
        // the Sun for its own heliocentric position and it answers longitude 0 at distance 0 - a
        // well-formed reply meaning "here", which would plot at 0 degrees Aries if it were drawn.
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED | SweConst.SEFLG_HELCTR;
        int rc = sw.swe_calc_ut(birth, SweConst.SE_SUN, flags, xx, err);
        ok("the Sun's own heliocentric call does not fail", rc >= 0);
        ok("it answers the origin, which is why the guard is there",
            xx[0] == 0.0 && xx[2] == 0.0);

        // <b>And the guard refuses it.</b> Deleting that branch survived this suite untouched
        // until this assertion existed, because nothing reachable through compute() can trip it.
        // Hand it the Sun directly: if the exclusion rule is ever loosened by accident, this is
        // what stops a body being drawn at 0 degrees Aries instead.
        Heliocentric.Frame scratch = new Heliocentric.Frame();
        Heliocentric.Place origin = Heliocentric.read(sw, birth, SweConst.SE_SUN, "Sun",
            "☉", "Su", flags, scratch);
        ok("a body that answers the origin is refused", !origin.ok);
        ok("and says so in words", origin.error != null && origin.error.contains("origin"));
        ok("and the frame carries the warning", scratch.warnings.size() == 1);

        // The same call for a real body must pass, or the assertion above would hold just as
        // well against a read() that refused everything.
        Heliocentric.Place mars = Heliocentric.read(sw, birth, SweConst.SE_MARS, "Mars",
            "♂", "Ma", flags, scratch);
        ok("but a real body is not", mars.ok);
        ok("and adds no warning", scratch.warnings.size() == 1);
    }

    // ------------------------------------------------------------------ E

    private static void aspects(SwissEph sw, double birth) {
        Heliocentric.Frame f = Heliocentric.compute(sw, birth);
        List<Heliocentric.Contact> cs = Heliocentric.aspects(f);
        ok("a real chart has aspects", !cs.isEmpty());
        for (Heliocentric.Contact c : cs) {
            ok(c.a + " " + c.type.label + " " + c.b + " is within the width it was judged at",
                c.orb <= c.orbUsed + 1e-9);
            ok("and names two different bodies", !c.a.equals(c.b));
            ok("both of which are in the chart",
                f.byName(c.a) != null && f.byName(c.b) != null);
            // The separation the contact claims must be the one the two longitudes actually have.
            double sep = Aspects.separation(f.byName(c.a).longitude, f.byName(c.b).longitude);
            ok("and the orb is the real distance from exact",
                Math.abs(Math.abs(sep - c.type.exactAngle) - c.orb) < 1e-9);
        }
        // Each pair once, not twice: a symmetric loop written with two full passes would double
        // every aspect and no assertion above would notice.
        java.util.Set<String> seen = new java.util.HashSet<>();
        boolean dupes = false;
        for (Heliocentric.Contact c : cs) {
            String key = (c.a.compareTo(c.b) < 0 ? c.a + "|" + c.b : c.b + "|" + c.a)
                + "|" + c.type;
            dupes |= !seen.add(key);
        }
        ok("each pair is reported once", !dupes);

        // <b>Which profile's widths, asserted rather than commented.</b> Everything above would
        // pass identically if these were judged at the Transits preset, because the width and the
        // orb reported both come from whichever profile is used - the check would move with the
        // defect. This is the rule and its only consumer being checked together, which is the
        // mistake AspectGridCheck's own comment names and which this session has already hit twice.
        java.util.Map<String, Double> natalWas = Aspects.customOrbs(Aspects.Profile.NATAL);
        java.util.Map<String, Double> transitWas = Aspects.customOrbs(Aspects.Profile.TRANSIT);
        try {
            int before = cs.size();

            // Widen everything natally: more pairs must come into orb.
            java.util.Map<String, Double> wide = new java.util.HashMap<>();
            for (Heliocentric.Place p : f.places) {
                wide.put(p.name, Double.valueOf(Aspects.MAX_BODY_ORB));
            }
            Aspects.setCustomOrbs(Aspects.Profile.NATAL, wide);
            int widened = Heliocentric.aspects(Heliocentric.compute(sw, birth)).size();
            ok("widening the NATAL preset finds more (" + before + " -> " + widened + ")",
                widened > before);

            // Now put natal back and widen the TRANSIT preset instead. Nothing may move.
            Aspects.setCustomOrbs(Aspects.Profile.NATAL, natalWas);
            Aspects.setCustomOrbs(Aspects.Profile.TRANSIT, wide);
            int unmoved = Heliocentric.aspects(Heliocentric.compute(sw, birth)).size();
            ok("widening the TRANSIT preset changes nothing (" + unmoved + ")",
                unmoved == before);
        } finally {
            Aspects.setCustomOrbs(Aspects.Profile.NATAL, natalWas);
            Aspects.setCustomOrbs(Aspects.Profile.TRANSIT, transitWas);
        }
    }

    // ------------------------------------------------------------------ F

    private static void oneRule() {
        List<String> shown = Heliocentric.shownBodies();
        ok("the Earth is listed", shown.contains(Heliocentric.EARTH));
        ok("the Sun is not", !shown.contains("Sun"));
        ok("the Moon is not", !shown.contains("Moon"));

        // <b>The list and the rule cannot drift.</b> This is the assertion for this project's
        // most logged defect - one rule implemented twice - and it is why the screen asks
        // shownBodies rather than keeping a list of its own.
        int expected = 1;   // the Earth
        for (int i = 0; i < Bodies.count(); i++) {
            if (Heliocentric.hasHeliocentricPosition(Bodies.at(i))) {
                expected++;
            }
        }
        ok("the list is exactly what the rule admits", shown.size() == expected);
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            ok(d.name + " is listed if and only if the rule admits it",
                shown.contains(d.name) == Heliocentric.hasHeliocentricPosition(d));
        }
        // The Earth takes the Sun's seat rather than being appended, so a reader finds it where
        // a luminary belongs.
        ok("the Earth sits first, where the Sun was",
            Heliocentric.EARTH.equals(shown.get(0)));
    }

    // ------------------------------------------------------------------ G

    private static void zodiac(SwissEph sw, double birth) {
        String was = Ephemeris.zodiacLabel();
        try {
            Ephemeris.setZodiac(Ephemeris.ZODIACS[0][0]);
            Heliocentric.Frame tropical = Heliocentric.compute(sw, birth);
            double ayan = 0.0;
            String sidereal = null;
            for (String[] z : Ephemeris.ZODIACS) {
                if (Integer.parseInt(z[1]) >= 0) {
                    sidereal = z[0];
                    break;
                }
            }
            ok("there is a sidereal zodiac to switch to", sidereal != null);
            if (sidereal == null) {
                return;
            }
            Ephemeris.setZodiac(sidereal);
            Heliocentric.Frame sid = Heliocentric.compute(sw, birth);
            ayan = Ephemeris.ayanamsa(sw, birth);
            ok("the ayanamsa is a real figure", ayan > 20.0 && ayan < 30.0);

            // Every body moves back by the ayanamsa, because the zodiac is a frame for measuring
            // longitude and is independent of where the measurement is taken from.
            for (Heliocentric.Place p : tropical.places) {
                Heliocentric.Place q = sid.byName(p.name);
                ok(p.name + " is in the sidereal chart too", q != null);
                if (q == null || !p.ok) {
                    continue;
                }
                double moved = Aspects.separation(p.longitude, q.longitude);
                ok(p.name + " moved by the ayanamsa", Math.abs(moved - ayan) < 0.01);
            }
            // The distance is a fact about the orbit, not about the zodiac, and must not move.
            Heliocentric.Place a = tropical.byName("Jupiter");
            Heliocentric.Place b = sid.byName("Jupiter");
            ok("but the distance did not", Math.abs(a.distanceAu - b.distanceAu) < 1e-9);
        } finally {
            Ephemeris.setZodiac(was);
        }
    }

    // ------------------------------------------------------------------

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
