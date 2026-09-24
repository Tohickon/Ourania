package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * F10, the electional measures: the planetary day and hour with what each favours, and angularity.
 *
 * <p><b>The named rules are asserted over their whole domain, not through the sky.</b>
 * {@link Electional#angular} is walked across all twelve houses and {@link Electional#fortifies}
 * across all fifteen aspect types, because that is the only way either can be held against a
 * mutation. The lesson is {@link Horary#throughDifficulty}'s: on 21 September a mutation making
 * the trine an aspect of difficulty <b>survived</b> a suite that sampled a fortnight of sky, since
 * no pair in that fortnight ever perfected a trine. A rule observed only through the weather is
 * not held at all.
 *
 * <p><b>Every measure is asserted twice: that it fires, and that it does not.</b> Each of these
 * testimonies has two or three load-bearing conditions - a malefic must be both on an angle and
 * undignified, the South Node must be both near a significator and within its own orb - and a
 * check that only ever sees the positive case cannot tell a conjunction from a disjunction. The
 * negatives here are the assertions that would catch a dropped condition.
 */
public final class ElectionalCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final double LAT = 39.95;
    private static final double LON = -75.17;

    /**
     * Twenty minutes, which turns the Ascendant about five degrees.
     *
     * <b>Not five minutes.</b> Every step costs an {@code assess}, and every {@code assess} costs
     * a sunrise search; a day at five minutes is 288 of them per part and the suite would be the
     * slowest in the tree for no extra coverage. What has to vary here is the houses, and a whole
     * day of them at twenty minutes is seventy-two charts covering all twelve.
     */
    private static final double STEP = 20.0 / (24.0 * 60.0);

    /** An hour, for the walks that repeat over all seven matters. Seventy-two calls become 168. */
    private static final double MATTER_STEP = 1.0 / 24.0;

    /**
     * 2026-01-07. <b>Found, not chosen</b> - and the first run is why.
     *
     * <p>Two measures here need the sky to supply a case, and an arbitrary day supplies neither.
     * A malefic must appear on an angle <i>with</i> dignity, or nothing proves that dignity is
     * load-bearing rather than decoration; and the South Node must reach a significator, or the
     * whole of Part E is negatives. On 2026-09-24 the suite went red on exactly those two, which
     * is the right failure: the rule was sound and the day was not.
     *
     * <p>ElectionalSearch walked three years for a day carrying both. This one has <b>Mars
     * exalted in Capricorn at +5 and Saturn peregrine in Pisces at -2</b>, so one malefic crosses
     * the angles strong and the other crosses them weak within the same day - which is what makes
     * the pair of assertions in Part D a real conjunction test rather than half of one. It also
     * has the <b>Moon 0.34 degrees from the South Node</b> at noon, and the Moon is a
     * co-significator of every matter, so Part E has something to find for several hours either
     * side of it and nothing to find at the ends of the day.
     *
     * <p>The Ascendant turns through all twelve signs in any day, so nothing else here depends on
     * the date.
     */
    private static final double FOUND_JD = new SweDate(2026, 1, 7, 0.0).getJulDay();

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = FOUND_JD;

        partA();
        partB(sw, jd);
        partC();
        partD(sw, jd);
        partE(sw, jd);
        partF(sw, jd);

        report();
    }

    // ---------------------------------------------------------------- A: what each hour favours

    private static void partA() {
        System.out.println("Part A - the planetary day and hour, and what each favours");

        List<String> seen = new ArrayList<>();
        for (String ruler : PlanetaryHours.CHALDEAN) {
            String what = Electional.favours(ruler);
            ok(ruler + " favours something, in words", !what.isEmpty());
            ok("and nothing else favours exactly what " + ruler + " does", !seen.contains(what));
            seen.add(what);
        }
        ok("all seven of the Chaldean rulers were covered", seen.size() == 7);

        // The empty string is the answer for "no ruler", which is what PlanetaryHours returns
        // inside the polar night. A sentence invented for it would be printed there.
        ok("a name that is not one of the seven favours nothing",
            Electional.favours("Chiron").isEmpty());
        ok("and neither does no name at all", Electional.favours(null).isEmpty());
    }

    // ------------------------------------------------------------------------- B: angularity

    private static void partB(SwissEph sw, double jd) {
        System.out.println();
        System.out.println("Part B - angularity, by the chart's own cusps");

        // The rule over its whole domain, which is the only place a mutation cannot hide.
        for (int h = 1; h <= 12; h++) {
            boolean expected = h == 1 || h == 4 || h == 7 || h == 10;
            ok("house " + h + (expected ? " is angular" : " is not angular"),
                Electional.angular(h) == expected);
        }
        ok("nothing outside the twelve is angular",
            !Electional.angular(0) && !Electional.angular(13));

        // <b>Walked across a whole day.</b> A benefic sits in one house for days at a time, so a
        // single chart proves nothing about the other eleven; the houses are what turn.
        boolean sawAngular = false;
        boolean sawCadent = false;
        int mismatchedSignAndHouse = 0;
        for (double t = jd; t < jd + 1.0; t += STEP) {
            ChartFrame f = ChartFrame.compute(sw, t, LAT, LON, 'P', false, 0.0);
            Electional.Quality q = Electional.assess(sw, f, t, LAT, LON, null);
            for (String name : new String[] {"Jupiter", "Venus"}) {
                ChartFrame.Body b = f.body(name);
                if (b == null || !b.ok) {
                    continue;
                }
                int house = Zodiac.houseOf(b.lon, f.cusps);
                boolean noted = hasNote(q, name, Electional.Weight.RAISES, "angular");
                if (Electional.angular(house)) {
                    sawAngular = true;
                    if (!noted) {
                        fail(name + " was in the " + house + " house and no note said so");
                    }
                } else {
                    sawCadent = true;
                    if (noted) {
                        fail(name + " was in the " + house + " house and a note called it angular");
                    }
                }

                // The house is not the sign counted from the Ascendant, and in Placidus at this
                // latitude the two disagree often. Counting how often is what makes the
                // assertion below meaningful rather than vacuous.
                int bySign = ((Zodiac.signIndex(b.lon) - Zodiac.signIndex(f.asc)) % 12 + 12) % 12 + 1;
                if (bySign != house) {
                    mismatchedSignAndHouse++;
                }
            }
        }
        ok("a benefic was angular at some point in the day", sawAngular);
        ok("and not angular at another, so both branches were walked", sawCadent);
        ok("the day contained charts where the house and the counted sign disagree, "
            + "so reading one for the other would have been caught",
            mismatchedSignAndHouse > 0);
        ok("every angular benefic in the day was noted, and no other was", noNewFailures());
    }

    // ------------------------------------------------- C: what fortifies the Moon, over all types

    private static void partC() {
        System.out.println();
        System.out.println("Part C - a benefic fortifies the Moon by the soft Ptolemaic aspects");

        // <b>The three are written out by hand, not derived.</b> Asking whether fortifies(t)
        // equals isPtolemaic(t) && !throughDifficulty(t) asserts only that the method equals its
        // own body, which is true of any mutation of it. NavigationCheck states the principle for
        // its own list of screens and it is the same principle here.
        for (Aspects.Type type : Aspects.Type.values()) {
            boolean expected = type == Aspects.Type.CONJUNCTION
                || type == Aspects.Type.SEXTILE
                || type == Aspects.Type.TRINE;
            ok(type.label + (expected ? " fortifies" : " does not fortify"),
                Electional.fortifies(type) == expected);
        }
        ok("the conjunction, the sextile and the trine fortify",
            Electional.fortifies(Aspects.Type.CONJUNCTION)
                && Electional.fortifies(Aspects.Type.SEXTILE)
                && Electional.fortifies(Aspects.Type.TRINE));
        ok("the square and the opposition do not",
            !Electional.fortifies(Aspects.Type.SQUARE)
                && !Electional.fortifies(Aspects.Type.OPPOSITION));
        ok("and no minor aspect does", noMinorFortifies());
        ok("nothing at all does not fortify", !Electional.fortifies(null));
    }

    private static boolean noMinorFortifies() {
        for (Aspects.Type type : Aspects.Type.values()) {
            if (type.isMinor() && Electional.fortifies(type)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------- D: the malefics, both conditions

    private static void partD(SwissEph sw, double jd) {
        System.out.println();
        System.out.println("Part D - a malefic lowers only when it is both placed and weak");

        int onAngleAndWeak = 0;
        int onAngleAndStrong = 0;
        int weakOffAngle = 0;
        int onLowerAngles = 0;
        for (double t = jd; t < jd + 1.0; t += STEP) {
            ChartFrame f = ChartFrame.compute(sw, t, LAT, LON, 'P', false, 0.0);
            Electional.Quality q = Electional.assess(sw, f, t, LAT, LON, null);
            ChartFrame.Body sun = f.body("Sun");
            boolean diurnal = sun != null && sun.ok && Sect.isDiurnal(sun.lon, f.asc);

            for (String name : new String[] {"Mars", "Saturn"}) {
                ChartFrame.Body b = f.body(name);
                if (b == null || !b.ok) {
                    continue;
                }
                boolean onAsc = Aspects.separation(b.lon, f.asc) <= Electional.ANGLE_ORB;
                boolean onMc = Aspects.separation(b.lon, f.mc) <= Electional.ANGLE_ORB;
                boolean onDsc = Aspects.separation(b.lon, f.dsc) <= Electional.ANGLE_ORB;
                boolean onIc = Aspects.separation(b.lon, f.ic) <= Electional.ANGLE_ORB;
                Dignity.Result d = Dignity.evaluate(name, b.lon, diurnal);
                boolean weak = d != null && d.score < 0;
                boolean noted = hasNote(q, name, Electional.Weight.LOWERS, "undignified");

                if ((onAsc || onMc) && weak) {
                    onAngleAndWeak++;
                    if (!noted) {
                        fail(name + " was weak on an angle and nothing lowered the moment");
                    }
                } else {
                    if (noted) {
                        fail(name + " lowered the moment without being both weak and on an angle");
                    }
                    if ((onAsc || onMc) && !weak) {
                        onAngleAndStrong++;
                    }
                    if (weak && !onAsc && !onMc) {
                        weakOffAngle++;
                    }
                    if ((onDsc || onIc) && !onAsc && !onMc && weak) {
                        onLowerAngles++;
                    }
                }
            }
        }
        ok("a malefic was weak on the Ascendant or Midheaven somewhere in the day, "
            + "so the positive case was walked", onAngleAndWeak > 0);
        ok("a malefic was weak away from those angles, and did not lower the moment - "
            + "so placement is load-bearing", weakOffAngle > 0);
        ok("a malefic was on one of the two angles with dignity, and did not lower the moment - "
            + "so dignity is load-bearing", onAngleAndStrong > 0);
        ok("a malefic was weak on the Descendant or the Imum Coeli and did not lower the moment - "
            + "the decision names the Ascendant and Midheaven, and that is not an oversight",
            onLowerAngles > 0);
        ok("no malefic note fired on half a condition", noNewFailures());
    }

    // --------------------------------------------------------------------- E: the South Node

    private static void partE(SwissEph sw, double jd) {
        System.out.println();
        System.out.println("Part E - the South Node on a significator");

        // Without a matter there is no house of the matter, so there are no significators to sit
        // on. The measure must be silent rather than inventing one.
        int nodeNotesWithoutMatter = 0;
        for (double t = jd; t < jd + 1.0; t += STEP) {
            ChartFrame f = ChartFrame.compute(sw, t, LAT, LON, 'P', false, 0.0);
            Electional.Quality q = Electional.assess(sw, f, t, LAT, LON, null);
            for (Electional.Note n : q.notes) {
                if (n.because.contains("South Node")) {
                    nodeNotesWithoutMatter++;
                }
            }
        }
        ok("with no matter given, the South Node testimony never fires",
            nodeNotesWithoutMatter == 0);

        int fired = 0;
        int withinOrb = 0;
        for (double t = jd; t < jd + 1.0; t += MATTER_STEP) {
            ChartFrame f = ChartFrame.compute(sw, t, LAT, LON, 'P', false, 0.0);
            for (Horary.Matter m : Horary.Matter.values()) {
                Electional.Quality q = Electional.assess(sw, f, t, LAT, LON, m);
                Horary.Significators s = Horary.significators(f, m);
                boolean near = near(f, s.querent) || near(f, s.quesited) || near(f, s.moon);
                boolean noted = false;
                for (Electional.Note n : q.notes) {
                    if (n.because.contains("South Node")) {
                        noted = true;
                        fired++;
                    }
                }
                if (near) {
                    withinOrb++;
                    if (!noted) {
                        fail("the South Node was on a significator of " + m + " and nothing said so");
                    }
                } else if (noted) {
                    fail("a South Node note fired for " + m + " with no significator within orb");
                }
            }
        }
        ok("the South Node reached a significator somewhere in the day, over the seven matters",
            withinOrb > 0);
        ok("and it did not reach one at every moment, so the orb is load-bearing",
            fired < 7 * (int) Math.round(1.0 / MATTER_STEP));
        ok("every significator the node sat on was reported, and no other", noNewFailures());
        ok("the node's orb is tighter than an angle's, deliberately",
            Electional.NODE_ORB < Electional.ANGLE_ORB);
    }

    private static boolean near(ChartFrame f, String name) {
        if (name == null) {
            return false;
        }
        ChartFrame.Body b = f.body(name);
        return b != null && b.ok
            && Aspects.separation(b.lon, f.southNode) <= Electional.NODE_ORB;
    }

    // ------------------------------------------------- F: the tally, and where there are no hours

    private static void partF(SwissEph sw, double jd) {
        System.out.println();
        System.out.println("Part F - the standing, and the polar case");

        ChartFrame f = ChartFrame.compute(sw, jd, LAT, LON, 'P', false, 0.0);
        Electional.Quality q = Electional.assess(sw, f, jd, LAT, LON, Horary.Matter.CAREER);
        ok("the day ruler is the one PlanetaryHours names",
            q.dayRuler != null && q.dayRuler.equals(PlanetaryHours.at(sw, jd, LAT, LON).ruler));
        ok("the hour ruler is the one PlanetaryHours names",
            q.hourRuler != null
                && q.hourRuler.equals(PlanetaryHours.rulerAt(sw, jd, LAT, LON)));
        ok("and each carries what it favours, rather than only its name",
            q.dayFavours.equals(Electional.favours(q.dayRuler))
                && q.hourFavours.equals(Electional.favours(q.hourRuler))
                && !q.hourFavours.isEmpty());

        ok("the notes counted are the notes held",
            q.raises() + q.lowers() == q.notes.size());

        // The tally is this class's own convention, so it is asserted directly rather than
        // through whatever the sky happens to supply.
        ok("more raising than lowering is favoured",
            built(2, 1).standing() == Electional.Standing.FAVOURED);
        ok("more lowering than raising is ill-favoured",
            built(1, 2).standing() == Electional.Standing.ILL_FAVOURED);
        ok("an even split is mixed", built(2, 2).standing() == Electional.Standing.MIXED);
        ok("no testimony at all is neutral, not favoured",
            built(0, 0).standing() == Electional.Standing.NEUTRAL);

        // 78.2N in late September: the Sun still rises, so this is the polar winter instead.
        double polar = new SweDate(2026, 12, 21, 12.0).getJulDay();
        ChartFrame arctic = ChartFrame.compute(sw, polar, 78.2, 15.6, 'P', false, 0.0);
        Electional.Quality none = Electional.assess(sw, arctic, polar, 78.2, 15.6,
            Horary.Matter.CAREER);
        ok("inside the polar night there are no hours, so the moment is not weighed", none.unknown);
        ok("and its standing says unknown rather than neutral",
            none.standing() == Electional.Standing.UNKNOWN);
        ok("and it carries no testimony at all", none.notes.isEmpty());
        ok("and no ruler is invented for it",
            none.hourFavours.isEmpty() && none.dayFavours.isEmpty());
    }

    private static Electional.Quality built(int up, int down) {
        Electional.Quality q = new Electional.Quality();
        for (int i = 0; i < up; i++) {
            q.notes.add(note(Electional.Weight.RAISES));
        }
        for (int i = 0; i < down; i++) {
            q.notes.add(note(Electional.Weight.LOWERS));
        }
        return q;
    }

    private static Electional.Note note(Electional.Weight w) {
        Electional.Note n = new Electional.Note();
        n.weight = w;
        n.body = "Venus";
        n.because = "constructed for the tally";
        return n;
    }

    // ---------------------------------------------------------------------------- the harness

    private static boolean hasNote(Electional.Quality q, String body,
                                   Electional.Weight w, String word) {
        for (Electional.Note n : q.notes) {
            if (n.weight == w && body.equals(n.body) && n.because.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static int loopFailures = 0;
    private static int loopFailuresSeen = 0;

    /**
     * True when no assertion inside a loop has failed since this was last asked.
     *
     * <b>Counts only the failures {@link #fail} added, not the ones {@link #ok} added.</b> The
     * first version compared {@code failures.size()} against a remembered total, which meant a
     * failing {@code ok} on the line above turned the next one of these red as well - two
     * failures reported for one defect, and the second naming a loop that had found nothing
     * wrong. That is noise in exactly the place a reader is trying to count what broke.
     */
    private static boolean noNewFailures() {
        boolean clean = loopFailures == loopFailuresSeen;
        loopFailuresSeen = loopFailures;
        return clean;
    }

    /** A failure found inside a loop, where the assertion has no single line of its own. */
    private static void fail(String label) {
        checks++;
        failures.add(label);
        loopFailures++;
        System.out.println("  FAIL " + label);
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
        }
        System.exit(1);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
