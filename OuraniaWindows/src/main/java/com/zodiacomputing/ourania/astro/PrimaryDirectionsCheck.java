package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification and measurement for F9, primary directions.
 *
 *  Part A - the three keys, and the distance between them. The drift is measured rather than
 *           asserted from a table, because the drift is the reason the key is a setting.
 *  Part B - the coordinate conversions, on the four points of the ecliptic where the answer is
 *           known without computing it, and the circumpolar case that has no answer at all.
 *  Part C - the arc's defining properties. The Midheaven is the one significator whose arc can
 *           be written down independently - its meridian distance is zero, so the arc is simply
 *           the difference in right ascension - and that is asserted against the formula rather
 *           than against a recorded number.
 *  Part D - the directions of a real chart: sorted, inside the span, both forms kept apart.
 *  Part E - measurement. What the first years of a life actually contain.
 *
 * <b>No fixture dates.</b> There is no published table of directions for this chart to check
 * against, and inventing one from this engine's own output would assert only that the code has
 * not changed. Everything below is a property that would still be true if the implementation
 * were replaced.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.PrimaryDirectionsCheck
 */
public final class PrimaryDirectionsCheck {

    private static final int NATAL_Y = 1984;
    private static final int NATAL_M = 9;
    private static final int NATAL_D = 8;
    private static final double NATAL_UT = 7.0 + 33.0 / 60.0;
    private static final double NATAL_LAT = 41.8781;
    private static final double NATAL_LON = -87.6298;

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double natalJd = new SweDate(NATAL_Y, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, natalJd, NATAL_LAT, NATAL_LON, 'P', false, 0.0);

        System.out.println("=== Part A: three keys, and how far apart they are ===");
        theKeys(sw, natalJd);

        System.out.println();
        System.out.println("=== Part B: the coordinates ===");
        theCoordinates();

        System.out.println();
        System.out.println("=== Part C: what an arc of direction is ===");
        theArc(f);

        System.out.println();
        System.out.println("=== Part D: the directions of a real chart ===");
        theDirections(sw, f, natalJd);

        System.out.println();
        System.out.println("=== Part E: measurement ===");
        measurement(sw, f, natalJd);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String s : failures) {
                System.out.println("  " + s);
            }
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- part A

    private static void theKeys(SwissEph sw, double natalJd) {
        eq("Ptolemy is one degree a year", 1.0,
            PrimaryDirections.Key.PTOLEMY.degreesPerYear);
        near("under Ptolemy an arc IS the age", 37.0,
            PrimaryDirections.years(PrimaryDirections.Key.PTOLEMY, 37.0, sw, natalJd), 1e-9);

        // Naibod's key is 0 degrees 59 minutes 08 seconds. Asserted against that reading of it
        // rather than against the constant, so a mistyped constant cannot agree with itself.
        double naibod = PrimaryDirections.Key.NAIBOD.degreesPerYear;
        double stated = 59.0 / 60.0 + 8.0 / 3600.0;
        near("Naibod is 0 deg 59' 08\" to within an arcsecond", stated, naibod, 1.0 / 3600.0);
        yes("and it is slower than a degree, so its years run longer", naibod < 1.0);

        // <b>The drift is the reason this is a setting.</b> Measured, not quoted.
        double atSixty = PrimaryDirections.years(PrimaryDirections.Key.NAIBOD, 60.0, sw, natalJd)
            - PrimaryDirections.years(PrimaryDirections.Key.PTOLEMY, 60.0, sw, natalJd);
        System.out.printf("  a 60 degree arc: Ptolemy 60.00 years, Naibod %.2f - %.2f years apart%n",
            60.0 + atSixty, atSixty);
        yes("sixty degrees of arc separates the two keys by most of a year ("
            + String.format("%.2f", atSixty) + ")", atSixty > 0.5 && atSixty < 1.5);

        // Placidus is measured from this chart's own Sun, so it sits near Naibod but not on it.
        double placidus = PrimaryDirections.years(PrimaryDirections.Key.PLACIDUS, 60.0, sw, natalJd);
        double ratio = placidus / (60.0 + atSixty);
        System.out.printf("  and Placidus, from this chart's own Sun: %.2f years - %.3f of Naibod%n",
            placidus, ratio);
        yes("the Placidus key answers at all", !Double.isNaN(placidus));

        // <b>"Within a year of Naibod" was my assertion and it was wrong.</b> This chart is born
        // on 8 September, a fortnight from the equinox, where the Sun's advance in RIGHT
        // ASCENSION is at its slowest - so a true solar key runs three years longer over sixty
        // degrees, not a few months. The bound that actually holds comes from the obliquity: a
        // day's motion in right ascension is the motion in longitude times something between
        // cos(obliquity) at the equinoxes and 1/cos(obliquity) at the solstices, so a solar key
        // can differ from the mean key by up to nine per cent either way and no further. That is
        // a property of the sky rather than a tolerance chosen to make this pass.
        double cos = Math.cos(Math.toRadians(23.4392911));
        yes("a solar key stays inside what the obliquity allows, " + String.format("%.3f", ratio)
            + " against " + String.format("%.3f to %.3f", cos, 1.0 / cos),
            ratio >= cos - 0.01 && ratio <= 1.0 / cos + 0.01);
        yes("and this chart, born near an equinox, runs slower than the mean", ratio > 1.0);

        for (PrimaryDirections.Key k : PrimaryDirections.Key.values()) {
            double small = PrimaryDirections.years(k, 10.0, sw, natalJd);
            double large = PrimaryDirections.years(k, 40.0, sw, natalJd);
            yes(k.label + ": a longer arc is a later year", large > small);
            near(k.label + ": a zero arc is the birth moment", 0.0,
                PrimaryDirections.years(k, 0.0, sw, natalJd), 1e-6);
        }

        eq("an unknown key name falls back to the default",
            PrimaryDirections.Key.NAIBOD, PrimaryDirections.Key.named("Regiomontanus"));
        eq("and a known one is honoured", PrimaryDirections.Key.PTOLEMY,
            PrimaryDirections.Key.named("ptolemy"));
    }

    // ---------------------------------------------------------------- part B

    private static void theCoordinates() {
        double e = 23.4392911;
        // The four points where the answer is known without computing it.
        near("0 Aries has no right ascension", 0.0, PrimaryDirections.equatorial(0.0, e)[0], 1e-9);
        near("and no declination", 0.0, PrimaryDirections.equatorial(0.0, e)[1], 1e-9);
        near("0 Cancer is 90 degrees of right ascension", 90.0,
            PrimaryDirections.equatorial(90.0, e)[0], 1e-9);
        near("and stands at the obliquity itself", e,
            PrimaryDirections.equatorial(90.0, e)[1], 1e-9);
        near("0 Libra is 180", 180.0, PrimaryDirections.equatorial(180.0, e)[0], 1e-9);
        near("and back on the equator", 0.0, PrimaryDirections.equatorial(180.0, e)[1], 1e-9);
        near("0 Capricorn is as far south as Cancer is north", -e,
            PrimaryDirections.equatorial(270.0, e)[1], 1e-9);

        near("a body on the equator has no ascensional difference", 0.0,
            PrimaryDirections.ascensionalDifference(0.0, NATAL_LAT), 1e-9);
        near("and nor has any body at the equator of the earth", 0.0,
            PrimaryDirections.ascensionalDifference(20.0, 0.0), 1e-9);
        yes("a northern declination rises early in the north",
            PrimaryDirections.ascensionalDifference(20.0, NATAL_LAT) > 0.0);
        yes("and late in the south",
            PrimaryDirections.ascensionalDifference(20.0, -NATAL_LAT) < 0.0);

        // <b>Circumpolar has no answer, and says so.</b> Clamping here would hand back a
        // confident arc for a direction that cannot happen.
        yes("a circumpolar point has no ascensional difference",
            Double.isNaN(PrimaryDirections.ascensionalDifference(80.0, 60.0)));
        yes("and so it cannot be directed",
            Double.isNaN(PrimaryDirections.arc(0.0, 80.0, 10.0, 0.0, 60.0, 0.0)));

        near("meridian distance is measured from the meridian", 0.0,
            PrimaryDirections.meridianDistance(100.0, 100.0), 1e-9);
        near("and wraps the short way round", -10.0,
            PrimaryDirections.meridianDistance(90.0, 100.0), 1e-9);
        near("even across zero", 20.0,
            PrimaryDirections.meridianDistance(10.0, 350.0), 1e-9);
    }

    // ---------------------------------------------------------------- part C

    private static void theArc(ChartFrame f) {
        yes("the chart has a right ascension of the midheaven", !Double.isNaN(f.armc));
        double[] mc = PrimaryDirections.pointOf(f, "Midheaven", f.trueObliquity);
        yes("the Midheaven is a point", mc != null);
        if (mc == null) {
            return;
        }
        near("the Midheaven's right ascension IS the RAMC", Zodiac.normalise(f.armc), mc[0], 1e-9);

        // <b>The one significator whose arc can be written down independently.</b> The
        // Midheaven's meridian distance is zero, so its proportional place is zero, so the
        // promissor must simply arrive at the meridian: the arc is the difference in right
        // ascension and nothing else. Any error in the semi-arc arithmetic shows here.
        for (String prom : PrimaryDirections.PROMISSORS) {
            ChartFrame.Body b = f.body(prom);
            if (b == null || !b.ok) {
                continue;
            }
            double expected = Zodiac.normalise(f.armc - b.ra);
            near("directing " + prom + " to the Midheaven is the difference in right ascension",
                expected,
                PrimaryDirections.arc(mc[0], mc[1], b.ra, b.dec, f.geoLat, f.armc), 1e-6);
        }

        // A promissor already standing at the significator's place has nowhere to travel.
        for (String prom : PrimaryDirections.PROMISSORS) {
            ChartFrame.Body b = f.body(prom);
            if (b == null || !b.ok) {
                continue;
            }
            double self = PrimaryDirections.arc(b.ra, b.dec, b.ra, b.dec, f.geoLat, f.armc);
            yes(prom + " directed to its own place travels nothing ("
                + String.format("%.6f", self) + ")", self < 1e-6 || self > 360.0 - 1e-6);
        }

        // Every arc is a direction of travel, never negative.
        int counted = 0;
        for (String sig : PrimaryDirections.SIGNIFICATORS) {
            double[] s = PrimaryDirections.pointOf(f, sig, f.trueObliquity);
            if (s == null) {
                continue;
            }
            for (String prom : PrimaryDirections.PROMISSORS) {
                ChartFrame.Body b = f.body(prom);
                if (b == null || !b.ok) {
                    continue;
                }
                double a = PrimaryDirections.arc(s[0], s[1], b.ra, b.dec, f.geoLat, f.armc);
                if (Double.isNaN(a)) {
                    continue;
                }
                counted++;
                yes(prom + " to " + sig + " is an arc of travel, 0 to 360 ("
                    + String.format("%.2f", a) + ")", a >= 0.0 && a < 360.0);
            }
        }
        yes("arcs were computed at all: " + counted, counted > 20);
    }

    // ---------------------------------------------------------------- part D

    private static void theDirections(SwissEph sw, ChartFrame f, double natalJd) {
        List<PrimaryDirections.Direction> all =
            PrimaryDirections.of(sw, f, natalJd, 90.0, PrimaryDirections.Key.NAIBOD);
        yes("a life of ninety years has directions in it: " + all.size(), !all.isEmpty());

        int mundane = 0;
        int zodiacal = 0;
        double last = -1.0;
        for (PrimaryDirections.Direction d : all) {
            yes("soonest first: " + d, d.years >= last - 1e-9);
            last = d.years;
            yes("inside the span asked for: " + d, d.years >= 0.0 && d.years <= 90.0);
            yes("its moment follows from its years: " + d,
                Math.abs(d.jd - (natalJd + d.years * SolarArc.DAYS_PER_YEAR)) < 1e-6);
            yes("it names a significator and a promissor: " + d,
                d.significator != null && d.promissor != null);
            // A body directed to its own degrees is not a direction; the first run of this
            // suite produced "Sun conjunction Sun in mundo" at age zero, which is the birth
            // moment wearing the costume of an event.
            yes("and they are not the same body: " + d, !d.significator.equals(d.promissor));
            if (d.form == PrimaryDirections.Form.MUNDANE) {
                mundane++;
                // The mundane list is conjunctions in mundo, by the decision recorded in the
                // engine - mundane aspects are a technique of their own and are not computed.
                eq("a mundane direction is a conjunction in mundo", "conjunction", d.aspect);
            } else {
                zodiacal++;
                yes("a zodiacal direction names its aspect: " + d.aspect,
                    d.aspect != null && !d.aspect.isEmpty());
            }
        }
        yes("both forms are present - mundane " + mundane + ", zodiacal " + zodiacal,
            mundane > 0 && zodiacal > 0);

        // <b>The two forms are never mixed.</b> The same pair in the two forms gives different
        // arcs, and a reader has to be able to tell which they are looking at.
        int samePairDifferentArc = 0;
        for (PrimaryDirections.Direction a : all) {
            if (a.form != PrimaryDirections.Form.MUNDANE) {
                continue;
            }
            for (PrimaryDirections.Direction b : all) {
                if (b.form == PrimaryDirections.Form.ZODIACAL
                        && b.aspect.equals("conjunction")
                        && b.significator.equals(a.significator)
                        && b.promissor.equals(a.promissor)
                        && Math.abs(b.arc - a.arc) > 1e-6) {
                    samePairDifferentArc++;
                }
            }
        }
        yes("the same pair really does give two different answers in the two forms ("
            + samePairDifferentArc + " of them)", samePairDifferentArc > 0);

        // A shorter span is a prefix of a longer one, not a different calculation.
        List<PrimaryDirections.Direction> thirty =
            PrimaryDirections.of(sw, f, natalJd, 30.0, PrimaryDirections.Key.NAIBOD);
        yes("thirty years is fewer directions than ninety", thirty.size() <= all.size());
        for (PrimaryDirections.Direction d : thirty) {
            yes("and nothing in it falls outside thirty: " + d, d.years <= 30.0);
        }

        // The key changes the dates and not the arcs.
        List<PrimaryDirections.Direction> ptolemy =
            PrimaryDirections.of(sw, f, natalJd, 90.0, PrimaryDirections.Key.PTOLEMY);
        yes("the same chart under another key still has directions", !ptolemy.isEmpty());
    }

    // ---------------------------------------------------------------- part E

    private static void measurement(SwissEph sw, ChartFrame f, double natalJd) {
        List<PrimaryDirections.Direction> all =
            PrimaryDirections.of(sw, f, natalJd, 45.0, PrimaryDirections.Key.NAIBOD);
        System.out.println("  " + all.size() + " directions in the first forty-five years, Naibod");
        int shown = 0;
        for (PrimaryDirections.Direction d : all) {
            if (shown++ >= 8) {
                break;
            }
            System.out.printf("    %6.2f yr  %-9s %-12s %-15s %s  (arc %.2f)%n", d.years,
                d.promissor, d.aspect, d.significator,
                d.form == PrimaryDirections.Form.MUNDANE ? "in mundo " : "zodiacal ", d.arc);
        }
    }

    // ---------------------------------------------------------------- harness

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tolerance) {
        checks++;
        if (Double.isNaN(actual) || Math.abs(expected - actual) > tolerance) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }
}
