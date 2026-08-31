package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies the sect layer, and with it the parts of ChartFrame that sect depends on.
 *
 * Part A is pure geometry and needs no ephemeris: the diurnal test, the oriental test,
 * and the valence table. Part B runs two real charts - local noon and local midnight at
 * the same place - and checks invariants that must hold regardless of ephemeris accuracy.
 *
 * The Part B assertions are deliberately self-verifying rather than checked against an
 * external chart: at local noon the Sun must be nearer the MC than any other angle and
 * the chart must be diurnal; at local midnight, nearer the IC and nocturnal. Those hold
 * whatever the ephemeris says, so they test the geometry rather than the positions.
 *
 *   javac -encoding UTF-8 -cp src\main\java -d out-selftest src\main\java\com\zodiacomputing\ourania\astro\*.java
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.SectCheck
 */
public final class SectCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    // Los Angeles. PDT in July is UTC-7.
    private static final double LAT = 34.05;
    private static final double LON = -118.24;
    private static final int UTC_OFFSET = -7;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        System.out.println("=== Part A: geometry and valence, no ephemeris ===");
        int before = failures.size();
        diurnalGeometry();
        orientalGeometry();
        valenceTable();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: live charts ===");
        before = failures.size();
        liveCharts();
        report("Part B", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- part A

    /**
     * The diurnal test derived from first principles, at two different Ascendants so a
     * formula that accidentally depends on asc == 0 cannot pass.
     */
    private static void diurnalGeometry() {
        for (double asc : new double[] {0.0, 200.0, 359.5}) {
            String at = " (asc " + asc + ")";
            // MC is 90 degrees earlier in zodiacal order. Local noon.
            eq("Sun at MC is diurnal" + at, true, Sect.isDiurnal(asc - 90.0, asc));
            // IC is 90 degrees later. Local midnight.
            eq("Sun at IC is nocturnal" + at, false, Sect.isDiurnal(asc + 90.0, asc));
            // One degree before the Ascendant is the 12th house: above the horizon,
            // just after sunrise.
            eq("Sun 1 deg before asc is diurnal" + at, true, Sect.isDiurnal(asc - 1.0, asc));
            // One degree after is the 1st house: below the horizon, just before sunrise.
            eq("Sun 1 deg after asc is nocturnal" + at, false, Sect.isDiurnal(asc + 1.0, asc));
            // Well above the western horizon, late afternoon.
            eq("Sun 30 deg before DSC is diurnal" + at, true, Sect.isDiurnal(asc + 210.0, asc));

            eq("borderline at the asc" + at, true, Sect.isBorderline(asc + 2.0, asc, 7.0));
            eq("borderline at the dsc" + at, true, Sect.isBorderline(asc + 178.0, asc, 7.0));
            eq("not borderline at the mc" + at, false, Sect.isBorderline(asc - 90.0, asc, 7.0));
        }
    }

    /** Oriental means preceding the Sun in zodiacal order. Tested across the 0/360 seam. */
    private static void orientalGeometry() {
        eq("10 deg behind the Sun is oriental", true, Sect.isOriental(90.0, 100.0));
        eq("10 deg ahead of the Sun is occidental", false, Sect.isOriental(110.0, 100.0));
        eq("oriental across the seam", true, Sect.isOriental(355.0, 5.0));
        eq("occidental across the seam", false, Sect.isOriental(5.0, 355.0));
    }

    /** The valence assignments, both sects, all seven bodies. */
    private static void valenceTable() {
        // Day chart. Sun at the MC so Mercury's phase is unambiguous.
        checkValence(true, "Jupiter", Sect.Role.BENEFIC_OF_SECT, 2);
        checkValence(true, "Venus", Sect.Role.BENEFIC_CONTRARY, 1);
        checkValence(true, "Saturn", Sect.Role.MALEFIC_OF_SECT, -1);
        checkValence(true, "Mars", Sect.Role.MALEFIC_CONTRARY, -2);
        checkValence(true, "Sun", Sect.Role.SECT_LIGHT, 0);
        checkValence(true, "Moon", Sect.Role.CONTRARY_LIGHT, 0);

        // Night chart: every one of them flips.
        checkValence(false, "Jupiter", Sect.Role.BENEFIC_CONTRARY, 1);
        checkValence(false, "Venus", Sect.Role.BENEFIC_OF_SECT, 2);
        checkValence(false, "Saturn", Sect.Role.MALEFIC_CONTRARY, -2);
        checkValence(false, "Mars", Sect.Role.MALEFIC_OF_SECT, -1);
        checkValence(false, "Sun", Sect.Role.CONTRARY_LIGHT, 0);
        checkValence(false, "Moon", Sect.Role.SECT_LIGHT, 0);

        // Mercury takes its sect from its solar phase, not its nature.
        eq("Mercury oriental by day is of sect", Sect.Membership.OF_SECT,
            Sect.evaluate("Mercury", true, 90.0, 100.0).membership);
        eq("Mercury occidental by day is contrary", Sect.Membership.CONTRARY_TO_SECT,
            Sect.evaluate("Mercury", true, 110.0, 100.0).membership);
        eq("Mercury occidental by night is of sect", Sect.Membership.OF_SECT,
            Sect.evaluate("Mercury", false, 110.0, 100.0).membership);
        eq("Mercury oriental by night is contrary", Sect.Membership.CONTRARY_TO_SECT,
            Sect.evaluate("Mercury", false, 90.0, 100.0).membership);

        // Outer bodies have no sect, and should say so rather than score zero silently.
        eq("Pluto has no sect role", Sect.Role.NEUTRAL,
            Sect.evaluate("Pluto", true, 0.0, 0.0).role);

        eq("out-of-sect malefic by day", "Mars", Sect.outOfSectMalefic(true));
        eq("out-of-sect malefic by night", "Saturn", Sect.outOfSectMalefic(false));
        eq("benefic of sect by day", "Jupiter", Sect.beneficOfSect(true));
        eq("benefic of sect by night", "Venus", Sect.beneficOfSect(false));
    }

    private static void checkValence(boolean diurnal, String body, Sect.Role role, int score) {
        Sect.Valence v = Sect.evaluate(body, diurnal, 0.0, 180.0);
        String tag = (diurnal ? "day " : "night ") + body;
        eq(tag + " role", role, v.role);
        eq(tag + " score", score, v.score);
    }

    // ---------------------------------------------------------------- part B

    private static void liveCharts() {
        SwissEph sw = new SwissEph(EPHE_PATH);

        // Local noon and local midnight on the same day, same place.
        double noonUt = 12.0 - UTC_OFFSET;        // 19:00 UT
        double midnightUt = 24.0 - UTC_OFFSET;    // 07:00 UT the next day, expressed as hour 31

        ChartFrame noon = run(sw, 2026, 7, 31, noonUt, "local noon");
        ChartFrame midnight = run(sw, 2026, 8, 1, midnightUt - 24.0, "local midnight");

        // Invariant 1: sect must follow the time of day.
        eq("noon chart is diurnal", true, noon.diurnal);
        eq("midnight chart is nocturnal", false, midnight.diurnal);

        // Invariant 2: at local noon the Sun is nearer the MC than any other angle, and
        // at local midnight nearer the IC. Tolerance-free, so clock-noon drifting from
        // true solar noon cannot break it.
        eq("noon: Sun's nearest angle is the MC", "MC", nearestAngle(noon, noon.body("Sun").lon));
        eq("midnight: Sun's nearest angle is the IC", "IC",
            nearestAngle(midnight, midnight.body("Sun").lon));

        // Invariant 3: the angles are opposite each other.
        near("DSC opposes ASC", 180.0, ChartFrame.separation(noon.asc, noon.dsc), 1e-6);
        near("IC opposes MC", 180.0, ChartFrame.separation(noon.mc, noon.ic), 1e-6);

        // Invariant 4: the Lots, recomputed here independently of ChartFrame.
        for (ChartFrame f : new ChartFrame[] {noon, midnight}) {
            double sun = f.body("Sun").lon;
            double moon = f.body("Moon").lon;
            double expectFortune = f.diurnal
                ? Zodiac.normalise(f.asc + moon - sun)
                : Zodiac.normalise(f.asc + sun - moon);
            near("Lot of Fortune", expectFortune, f.lotOfFortune, 1e-9);
            // Spirit is Fortune reflected across the Ascendant.
            near("Spirit reflects Fortune across the asc", 0.0,
                ChartFrame.separation(Zodiac.normalise(2 * f.asc - f.lotOfFortune), f.lotOfSpirit),
                1e-9);
        }

        // Invariant 5: the Sun should be in Leo in late July, and the two charts are
        // half a day apart so the Sun barely moves.
        eq("Sun is in Leo on 31 July", "leo", Zodiac.signName(noon.body("Sun").lon));
        near("Sun moves about half a degree in 12 hours", 0.5,
            ChartFrame.separation(noon.body("Sun").lon, midnight.body("Sun").lon), 0.15);

        // Invariant 6: the prenatal syzygy must actually be a syzygy. Recompute the Sun
        // and Moon at the returned moment and check they were conjunct or opposed, and
        // that the moment is in the past and within one lunar month.
        checkSyzygy(sw, noon, "noon");
        checkSyzygy(sw, midnight, "midnight");

        // Invariant 7: surface ephemeris warnings, and fail only on unexpected ones.
        // The Moshier fallback is a known environmental condition on this machine, not a
        // code defect, so it is reported rather than failed - but anything else is a bug.
        reportWarnings(noon);
    }

    private static void reportWarnings(ChartFrame f) {
        int moshier = 0;
        List<String> unexpected = new ArrayList<>();
        for (String w : f.warnings) {
            if (w.contains("using Moshier")) {
                moshier++;
            } else {
                unexpected.add(w);
            }
        }
        if (moshier > 0) {
            System.out.println();
            System.out.println("  NOTE: " + moshier + " of " + f.bodies.length
                + " bodies fell back to the Moshier ephemeris.");
            System.out.println("        sepl_18.se1 and semo_18.se1 are absent; only seas_18.se1 was");
            System.out.println("        extracted from the APK. Positions are still good to well under");
            System.out.println("        an arcminute, but this is not the Swiss ephemeris proper.");
        }
        checks++;
        if (!unexpected.isEmpty()) {
            failures.add("unexpected ephemeris warnings: " + String.join(" | ", unexpected));
        }
    }

    private static void checkSyzygy(SwissEph sw, ChartFrame f, String label) {
        if (Double.isNaN(f.syzygyLon)) {
            failures.add("syzygy (" + label + "): root-find returned NaN");
            checks++;
            return;
        }
        double sunThen = lonAt(sw, f.syzygyJd, de.thmac.swisseph.SweConst.SE_SUN);
        double moonThen = lonAt(sw, f.syzygyJd, de.thmac.swisseph.SweConst.SE_MOON);
        double elong = ChartFrame.separation(sunThen, moonThen);
        double offBy = f.syzygyWasNewMoon ? elong : Math.abs(180.0 - elong);

        checks++;
        if (offBy > 0.05) {
            failures.add("syzygy (" + label + "): Sun and Moon were " + elong
                + " apart, not a " + (f.syzygyWasNewMoon ? "conjunction" : "opposition"));
        }
        double daysAgo = f.julianDayUt - f.syzygyJd;
        checks++;
        if (daysAgo < 0.0 || daysAgo > 29.6) {
            failures.add("syzygy (" + label + "): " + daysAgo + " days before the chart");
        }
        near("syzygy (" + label + ") Moon matches stored longitude", 0.0,
            ChartFrame.separation(moonThen, f.syzygyLon), 1e-6);

        System.out.printf("  syzygy (%s): %s %s, %.2f days earlier, elongation %.4f%n",
            label, Zodiac.format(f.syzygyLon),
            f.syzygyWasNewMoon ? "new moon" : "full moon", daysAgo, elong);
    }

    private static double lonAt(SwissEph sw, double jd, int ipl) {
        double[] xx = new double[6];
        StringBuffer serr = new StringBuffer();
        sw.swe_calc_ut(jd, ipl, de.thmac.swisseph.SweConst.SEFLG_SWIEPH
            | de.thmac.swisseph.SweConst.SEFLG_SPEED, xx, serr);
        return Zodiac.normalise(xx[0]);
    }

    private static ChartFrame run(SwissEph sw, int y, int m, int d, double hourUt, String label) {
        SweDate sd = new SweDate(y, m, d, hourUt);
        ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'W', false, 0.0);
        dump(f, label);
        return f;
    }

    private static String nearestAngle(ChartFrame f, double lon) {
        String best = null;
        double bestSep = 999.0;
        String[] names = {"ASC", "MC", "DSC", "IC"};
        double[] vals = {f.asc, f.mc, f.dsc, f.ic};
        for (int i = 0; i < 4; i++) {
            double s = ChartFrame.separation(lon, vals[i]);
            if (s < bestSep) {
                bestSep = s;
                best = names[i];
            }
        }
        return best;
    }

    private static void dump(ChartFrame f, String label) {
        System.out.println();
        System.out.println("--- " + label + " ---");
        System.out.printf("  ASC %-18s MC %-18s%n", Zodiac.format(f.asc), Zodiac.format(f.mc));
        System.out.printf("  sect: %s%s%n", f.diurnal ? "DIURNAL" : "NOCTURNAL",
            Sect.isBorderline(f.body("Sun").lon, f.asc, 7.0) ? "  [BORDERLINE]" : "");
        System.out.printf("  Fortune %-18s Spirit %s%n",
            Zodiac.format(f.lotOfFortune), Zodiac.format(f.lotOfSpirit));
        System.out.printf("  moon phase: %s (elongation %.1f)   void of course: %s%n",
            f.phaseName, f.elongation, f.moonVoidOfCourse);
        System.out.println("  loud bodies: sect light " + Sect.sectLight(f.diurnal)
            + ", benefic of sect " + Sect.beneficOfSect(f.diurnal)
            + ", out-of-sect malefic " + Sect.outOfSectMalefic(f.diurnal));
        System.out.println();
        for (ChartFrame.Body b : f.bodies) {
            if (!b.ok) {
                System.out.printf("  %-11s UNAVAILABLE - %s%n", b.name, b.error);
                continue;
            }
            Sect.Valence v = Sect.evaluate(b.name, f.diurnal, b.lon, f.body("Sun").lon);
            System.out.printf("  %-11s %-18s %s%s%s  |  %s%n",
                b.name, Zodiac.format(b.lon),
                b.retrograde ? "Rx " : "   ",
                b.outOfBounds ? "OOB " : "    ",
                String.format("dec %+6.2f", b.dec),
                v.role == Sect.Role.NEUTRAL && v.score == 0 && b.name.equals("Mercury")
                    ? v.role + " (" + v.membership + ")"
                    : v.role.toString());
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected + " +/- " + tol);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
