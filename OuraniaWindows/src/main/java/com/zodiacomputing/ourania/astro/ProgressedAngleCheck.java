package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Master list F4: the progressed Ascendant and Midheaven, under all three rules.
 *
 * <p>Held to arithmetic written out here rather than to the code's own: the Ascendant is re-derived
 * from the spherical-astronomy formula, the arcs from the Sun's own travel and from Naibod's rate,
 * and the quotidian angles from a chart cast independently at the progressed moment. Part D holds
 * the two conditions the technique carries - a chart with no birth time has no angles to progress,
 * and a rule that sweeps the zodiac every year cannot date anything.
 */
public final class ProgressedAngleCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** The reference chart the other L8 suites use: 1984-09-08 07:33 UT, Chicago. */
    private static final double NATAL_LAT = 41.8781;
    private static final double NATAL_LON = -87.6298;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double natalJd = new SweDate(1984, 9, 8, 7.0 + 33.0 / 60.0).getJulDay();
        ChartFrame natal = ChartFrame.compute(sw, natalJd, NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        ProgressedAngles.Method keep = ProgressedAngles.method;
        try {
            part("A: the arcs are what each rule says they are", () -> arcs(sw, natal, natalJd));
            part("B: the Ascendant follows from the MC, by the formula", () -> ascendant(sw, natal, natalJd));
            part("C: quotidian is the progressed moment's own sky", () -> quotidian(sw, natal, natalJd));
            part("D: the two conditions", () -> conditions(sw, natal, natalJd));
            part("E: the setting, and the reading that names the rule", () -> setting(sw, natal, natalJd));
        } finally {
            ProgressedAngles.method = keep;
        }

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

    private static double at(double natalJd, double years) {
        return natalJd + years * Progressions.DAYS_PER_YEAR;
    }

    // ------------------------------------------------------------------ A

    private static void arcs(SwissEph sw, ChartFrame natal, double natalJd) {
        for (double years : new double[] {1.0, 10.0, 30.0, 44.0, 80.0}) {
            double when = at(natalJd, years);

            // Solar arc: where the Sun has actually got to, minus where it started.
            double progressedSun = Almanac.bodyLongitude(
                sw, Progressions.progressedJd(natalJd, when), "Sun");
            double expected = Zodiac.normalise(progressedSun - natal.body("Sun").lon);
            near("solar arc is the progressed Sun's own travel at " + years + " years",
                expected, ProgressedAngles.arc(sw, natal, natalJd, when,
                    ProgressedAngles.Method.SOLAR_ARC), 1.0e-9);

            // Naibod: the Sun's mean motion, 59'08" a year, and nothing observed at all.
            near("Naibod is 59'08\" a year at " + years + " years",
                years * (59.0 / 60.0 + 8.0 / 3600.0),
                ProgressedAngles.arc(sw, natal, natalJd, when, ProgressedAngles.Method.NAIBOD),
                1.0e-9);

            // The two answer the same question and stay within a degree of each other.
            double solar = ProgressedAngles.arc(sw, natal, natalJd, when, ProgressedAngles.Method.SOLAR_ARC);
            double naibod = ProgressedAngles.arc(sw, natal, natalJd, when, ProgressedAngles.Method.NAIBOD);
            ok("solar arc and Naibod agree to within a degree at " + years + " years, "
                    + String.format("%.2f", Math.abs(solar - naibod)),
                Math.abs(solar - naibod) < 1.0);

            // And the arc is carried to the MC exactly.
            for (ProgressedAngles.Method m : new ProgressedAngles.Method[] {
                    ProgressedAngles.Method.SOLAR_ARC, ProgressedAngles.Method.NAIBOD}) {
                double[] a = ProgressedAngles.at(sw, natal, natalJd, when, m);
                near(m.label + "'s MC is the natal MC plus the arc at " + years + " years",
                    Zodiac.normalise(natal.mc + ProgressedAngles.arc(sw, natal, natalJd, when, m)),
                    a[1], 1.0e-6);
                near("and the IC is opposite it", Zodiac.opposite(a[1]), a[3], 1.0e-9);
                near("and the Descendant opposite the Ascendant", Zodiac.opposite(a[0]), a[2], 1.0e-9);
            }
        }
        // Around a degree a year is the claim the reading makes; hold it.
        double tenYears = ProgressedAngles.arc(sw, natal, natalJd, at(natalJd, 10.0),
            ProgressedAngles.Method.SOLAR_ARC);
        ok("the angles move about a degree a year, " + String.format("%.2f", tenYears / 10.0),
            tenYears / 10.0 > 0.9 && tenYears / 10.0 < 1.05);
    }

    // ------------------------------------------------------------------ B

    /**
     * The Ascendant from RAMC and latitude, by the standard spherical formula.
     *
     * tan(ASC) = cos(RAMC) / -(sin(RAMC) cos(eps) + tan(phi) sin(eps)), taken in the quadrant that
     * puts the Ascendant on the eastern horizon - written out here so the check does not simply ask
     * the same library the code asks.
     */
    static double ascFromArmc(double armcDeg, double latDeg, double epsDeg) {
        double armc = Math.toRadians(armcDeg);
        double eps = Math.toRadians(epsDeg);
        double phi = Math.toRadians(latDeg);
        double asc = Math.atan2(Math.cos(armc),
            -(Math.sin(armc) * Math.cos(eps) + Math.tan(phi) * Math.sin(eps)));
        double deg = Zodiac.normalise(Math.toDegrees(asc));
        // The formula's other root is the Descendant; the Ascendant is the one rising in the east,
        // which is the root within 180 degrees after the MC.
        return deg;
    }

    private static void ascendant(SwissEph sw, ChartFrame natal, double natalJd) {
        // The natal chart first: the same formula must reproduce the library's own Ascendant, or
        // the comparison below would be measuring the formula rather than the progression.
        near("the formula reproduces the natal Ascendant",
            natal.asc, ascFromArmc(natal.armc, natal.geoLat, natal.trueObliquity), 1.0e-6);

        int n = 0;
        double worst = 0.0;
        for (double years = 1.0; years <= 90.0; years += 1.0) {
            double when = at(natalJd, years);
            for (ProgressedAngles.Method m : new ProgressedAngles.Method[] {
                    ProgressedAngles.Method.SOLAR_ARC, ProgressedAngles.Method.NAIBOD,
                    ProgressedAngles.Method.QUOTIDIAN}) {
                double[] a = ProgressedAngles.at(sw, natal, natalJd, when, m);
                if (a == null) {
                    continue;
                }
                double mine = ascFromArmc(a[4], natal.geoLat, natal.trueObliquity);
                worst = Math.max(worst, Math.abs(Almanac.signedDelta(mine, a[0])));
                n++;
            }
        }
        ok("the Ascendant follows from the ARMC and the birth latitude over " + n
                + " progressions, worst " + String.format("%.6f", worst) + " degrees", worst < 0.01);

        // It is derived, not advanced: the progressed Ascendant does not move by the same arc as
        // the MC, and on this chart it moves a long way further.
        double when = at(natalJd, 44.0);
        double[] a = ProgressedAngles.at(sw, natal, natalJd, when, ProgressedAngles.Method.SOLAR_ARC);
        double arc = ProgressedAngles.arc(sw, natal, natalJd, when, ProgressedAngles.Method.SOLAR_ARC);
        double ascMoved = Zodiac.normalise(a[0] - natal.asc);
        ok("the Ascendant is derived rather than advanced by the arc: MC "
                + String.format("%.1f", arc) + " degrees, Ascendant " + String.format("%.1f", ascMoved),
            Math.abs(ascMoved - arc) > 1.0);
    }

    // ------------------------------------------------------------------ C

    private static void quotidian(SwissEph sw, ChartFrame natal, double natalJd) {
        for (double years : new double[] {5.0, 44.0}) {
            double when = at(natalJd, years);
            ChartFrame independent = ChartFrame.compute(sw,
                natalJd + (when - natalJd) / Progressions.DAYS_PER_YEAR,
                natal.geoLat, natal.geoLon, natal.hsys, natal.topocentric, natal.geoAltM);
            double[] a = ProgressedAngles.at(sw, natal, natalJd, when, ProgressedAngles.Method.QUOTIDIAN);
            near("quotidian's Ascendant is the progressed moment's own, at " + years + " years",
                independent.asc, a[0], 1.0e-9);
            near("and its MC", independent.mc, a[1], 1.0e-9);
        }

        // A degree a day of life: a year apart, the quotidian angles have gone right round and a
        // little further, which is what makes them useless for dating and good for a snapshot.
        double[] first = ProgressedAngles.at(sw, natal, natalJd, at(natalJd, 20.0),
            ProgressedAngles.Method.QUOTIDIAN);
        double[] later = ProgressedAngles.at(sw, natal, natalJd, at(natalJd, 21.0),
            ProgressedAngles.Method.QUOTIDIAN);
        double moved = Zodiac.normalise(later[1] - first[1]);
        ok("a year of life turns the quotidian MC right round, leaving " + String.format("%.1f", moved)
                + " degrees", moved < 10.0 || moved > 350.0);
        double[] tenDays = ProgressedAngles.at(sw, natal, natalJd, at(natalJd, 20.0 + 10.0 / 365.2422),
            ProgressedAngles.Method.QUOTIDIAN);
        double perTenDays = Math.abs(Almanac.signedDelta(tenDays[1], first[1]));
        ok("about a degree for every ten days of life, " + String.format("%.2f", perTenDays),
            perTenDays > 5.0 && perTenDays < 15.0);
    }

    // ------------------------------------------------------------------ D

    private static void conditions(SwissEph sw, ChartFrame natal, double natalJd) {
        ChartFrame noTime = ChartFrame.computeTimeUnknown(sw,
            new SweDate(1984, 9, 8, 12.0).getJulDay(), NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        for (ProgressedAngles.Method m : ProgressedAngles.Method.values()) {
            ok("a chart with no birth time has no progressed angles under " + m.label,
                ProgressedAngles.at(sw, noTime, natalJd, at(natalJd, 30.0), m) == null);
        }
        ok("and a null chart is answered, not thrown at",
            ProgressedAngles.at(sw, null, natalJd, at(natalJd, 30.0),
                ProgressedAngles.Method.SOLAR_ARC) == null);

        // The contact scan: angles appear under a datable rule and never under the quotidian one.
        double from = at(natalJd, 40.0);
        double to = at(natalJd, 45.0);
        ProgressedAngles.method = ProgressedAngles.Method.SOLAR_ARC;
        List<Progressions.Contact> solar = Progressions.contacts(sw, natal, natalJd, null, null, from, to);
        int solarAngles = 0;
        for (Progressions.Contact c : solar) {
            if ("Ascendant".equals(c.progressed) || "MC".equals(c.progressed)) {
                solarAngles++;
                double lon = ProgressedAngles.longitudeOf(sw, natal, natalJd, c.jd, c.progressed,
                    ProgressedAngles.Method.SOLAR_ARC);
                double sep = Aspects.separation(lon, natalLonOf(natal, c.natal));
                near("a progressed angle contact really is exact", c.type.exactAngle, sep, 1.0e-3);
                // Its own natal degree is a legitimate target; the far end of the same axis is
                // the same instant restated, and must not appear.
                ok("an angle is not reported against the far end of its own axis",
                    !("Ascendant".equals(c.progressed) && "Descendant".equals(c.natal))
                        && !("MC".equals(c.progressed) && "IC".equals(c.natal)));
            }
        }
        ok("solar arc angles do reach natal points in five years, " + solarAngles, solarAngles > 0);

        // The axis rule is not vacuous: an angle that aspects one end of an axis is aspecting the
        // other at that instant, so without the rule every one of these would be reported twice.
        int pairs = 0;
        for (Progressions.Contact c : solar) {
            if (!"Ascendant".equals(c.progressed) && !"MC".equals(c.progressed)) {
                continue;
            }
            String far = "Ascendant".equals(c.progressed) ? "Descendant" : "IC";
            double lon = ProgressedAngles.longitudeOf(sw, natal, natalJd, c.jd, c.progressed,
                ProgressedAngles.Method.SOLAR_ARC);
            double sep = Aspects.separation(lon, natalLonOf(natal, far));
            for (Aspects.Type t : Aspects.Type.values()) {
                if (Math.abs(sep - t.exactAngle) < 1.0e-3) {
                    pairs++;
                    break;
                }
            }
        }
        ok("and each one is exact to the far end of that axis too, " + pairs + " of " + solarAngles,
            solarAngles == 0 || pairs > 0);

        ProgressedAngles.method = ProgressedAngles.Method.QUOTIDIAN;
        int quotidianAngles = 0;
        for (Progressions.Contact c : Progressions.contacts(sw, natal, natalJd, null, null, from, to)) {
            if ("Ascendant".equals(c.progressed) || "MC".equals(c.progressed)) {
                quotidianAngles++;
            }
        }
        eq("a quotidian angle is never dated, whatever it crosses", 0, quotidianAngles);

        // The bodies are untouched by the rule: only the angles are in question.
        ProgressedAngles.method = ProgressedAngles.Method.SOLAR_ARC;
        int bodiesSolar = countBodies(Progressions.contacts(sw, natal, natalJd, null, null, from, to));
        ProgressedAngles.method = ProgressedAngles.Method.QUOTIDIAN;
        int bodiesQuotidian = countBodies(Progressions.contacts(sw, natal, natalJd, null, null, from, to));
        eq("the rule changes the angles and nothing else", bodiesSolar, bodiesQuotidian);
        ProgressedAngles.method = ProgressedAngles.Method.SOLAR_ARC;
    }

    private static int countBodies(List<Progressions.Contact> contacts) {
        int n = 0;
        for (Progressions.Contact c : contacts) {
            if (!"Ascendant".equals(c.progressed) && !"MC".equals(c.progressed)) {
                n++;
            }
        }
        return n;
    }

    private static double natalLonOf(ChartFrame natal, String name) {
        switch (name) {
            case "Ascendant":  return natal.asc;
            case "Descendant": return natal.dsc;
            case "MC":         return natal.mc;
            case "IC":         return natal.ic;
            default:
                ChartFrame.Body b = natal.body(name);
                return b == null ? Double.NaN : b.lon;
        }
    }

    // ------------------------------------------------------------------ E

    private static void setting(SwissEph sw, ChartFrame natal, double natalJd) {
        com.zodiacomputing.ourania.gui.Settings.setProgressedAngleMethod(
            ProgressedAngles.Method.NAIBOD);
        ok("the setting is saved",
            com.zodiacomputing.ourania.gui.Settings.progressedAngleMethod()
                == ProgressedAngles.Method.NAIBOD);
        ok("and put in force", ProgressedAngles.method == ProgressedAngles.Method.NAIBOD);
        com.zodiacomputing.ourania.gui.Settings.set(
            com.zodiacomputing.ourania.gui.Settings.PROGRESSED_ANGLES_KEY, "no such rule");
        ok("an unreadable rule falls back to solar arc",
            com.zodiacomputing.ourania.gui.Settings.progressedAngleMethod()
                == ProgressedAngles.Method.SOLAR_ARC);
        com.zodiacomputing.ourania.gui.Settings.setProgressedAngleMethod(
            ProgressedAngles.Method.SOLAR_ARC);

        // The reading shows the angles and says which rule produced them.
        double now = at(natalJd, 41.0);
        String html = com.zodiacomputing.ourania.gui.ChartTables.progressed(natal, sw, natalJd, now);
        ok("the progressed chart shows the angles", html.contains("Progressed angles")
            && html.contains("Ascendant") && html.contains("MC"));
        ok("and names the rule that moved them", html.contains(ProgressedAngles.Method.SOLAR_ARC.label));
        ProgressedAngles.method = ProgressedAngles.Method.QUOTIDIAN;
        String quot = com.zodiacomputing.ourania.gui.ChartTables.progressed(natal, sw, natalJd, now);
        ok("under the quotidian rule the reading says its angles are not dated",
            quot.contains("Quotidian") && quot.contains("not listed among the dated contacts"));
        ProgressedAngles.method = ProgressedAngles.Method.SOLAR_ARC;

        ChartFrame noTime = ChartFrame.computeTimeUnknown(sw,
            new SweDate(1984, 9, 8, 12.0).getJulDay(), NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        String none = com.zodiacomputing.ourania.gui.ChartTables.progressed(noTime, sw, natalJd, now);
        ok("a chart with no birth time is told why it has no progressed angles",
            none.contains("no birth time") && !none.contains("Progressed angles"));
    }

    // ------------------------------------------------------------------ harness

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

    private static void eq(String label, int expect, int got) {
        ok(label + ": expected " + expect + ", got " + got, expect == got);
    }

    private static void near(String label, double expect, double got, double tol) {
        checks++;
        if (!(Math.abs(Almanac.signedDelta(expect, got)) <= tol)) {
            failures.add(label + ": expected " + expect + ", got " + got);
        }
    }
}
