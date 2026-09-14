package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Antiscia: the mirror degrees are the ones the sky agrees with, and the contacts are complete.
 *
 * <p>Part B does not trust the formula. The whole claim of an antiscion is that the two degrees
 * stand at the same declination - so it converts both to equatorial coordinates through Swiss
 * Ephemeris and compares, which a wrong reflection (about the wrong axis, or 360 - x where
 * 180 - x belongs) cannot pass.
 */
public final class AntisciaCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double birth = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);

        part("A: the tables every traditional text prints", AntisciaCheck::known);
        part("B: a mirror shares a declination", () -> declination(sw, birth));
        part("C: the contact rule", AntisciaCheck::rule);
        part("D: a real chart, complete", () -> realChart(sw, birth));
        part("E: no birth time, no angles", () -> noTime(sw, birth));
        part("F: the table says it", () -> table(sw, birth));

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

    private static void known() {
        // Sign pairs Zodiac documents, degree within the sign reversed: d of one is 30 - d of the other.
        near("15 Taurus mirrors to 15 Leo", Zodiac.antiscion(45.0), 135.0);
        near("10 Aries mirrors to 20 Virgo", Zodiac.antiscion(10.0), 170.0);
        near("0 Cancer is its own antiscion", Zodiac.antiscion(90.0), 90.0);
        near("0 Capricorn is its own antiscion", Zodiac.antiscion(270.0), 270.0);
        near("5 Sagittarius mirrors to 25 Capricorn", Zodiac.antiscion(245.0), 295.0);
        near("10 Aries contra-mirrors to 20 Pisces", Zodiac.contraAntiscion(10.0), 350.0);
        near("0 Libra is its own contra-antiscion", Zodiac.contraAntiscion(180.0), 180.0);
        near("the antiscion of the antiscion is the degree", Zodiac.antiscion(Zodiac.antiscion(123.4)), 123.4);
        near("the antiscion is the contra-antiscion's opposite",
            Zodiac.normalise(Zodiac.antiscion(77.0) + 180.0), Zodiac.contraAntiscion(77.0));
    }

    private static void near(String label, double got, double want) {
        ok(label + ", got " + got, Aspects.separation(got, want) < 1e-9);
    }

    private static void declination(SwissEph sw, double jd) {
        double[] eclnut = new double[6];
        sw.swe_calc_ut(jd, SweConst.SE_ECL_NUT, 0, eclnut, new StringBuffer());
        double eps = eclnut[0];
        double worstAnti = 0.0;
        double worstContra = 0.0;
        for (double lon = 0.5; lon < 360.0; lon += 7.3) {
            double d = dec(sw, lon, eps);
            worstAnti = Math.max(worstAnti, Math.abs(dec(sw, Zodiac.antiscion(lon), eps) - d));
            worstContra = Math.max(worstContra, Math.abs(dec(sw, Zodiac.contraAntiscion(lon), eps) + d));
        }
        ok("every antiscion at the same declination, worst " + worstAnti, worstAnti < 1e-9);
        ok("every contra-antiscion at the opposite declination, worst " + worstContra,
            worstContra < 1e-9);
    }

    /** Declination of an ecliptic degree on the ecliptic, through Swiss Ephemeris. */
    private static double dec(SwissEph sw, double lon, double eps) {
        double[] in = {lon, 0.0, 1.0};
        double[] out = new double[3];
        new de.thmac.swisseph.SwissLib().swe_cotrans(in, out, -eps);
        return out[1];
    }

    private static void rule() {
        double keep = Antiscia.orb;
        Antiscia.orb = 1.0;
        try {
            Antiscia.Point taurus15 = point("A", 45.0);
            ok("a body 0.8 from the mirror is in contact",
                contact(taurus15, point("B", 135.8)) != null);
            ok("a body 1.2 from it is not", contact(taurus15, point("B", 136.2)) == null);
            Antiscia.Contact c = contact(point("A", 10.0), point("B", 350.4));
            ok("on the contra-antiscion it is contra, 0.4", c != null && c.contra
                && Math.abs(c.off - 0.4) < 1e-9);
            Antiscia.Contact ab = contact(point("A", 45.0), point("B", 135.5));
            Antiscia.Contact ba = contact(point("B", 135.5), point("A", 45.0));
            ok("the same either way round", ab != null && ba != null && ab.contra == ba.contra
                && Math.abs(ab.off - ba.off) < 1e-9);
            // A pair can never be in orb both ways: the two mirrors are always opposite, so their
            // distances to any degree add to 180.
            double worst = 0.0;
            for (double x = 0.0; x < 360.0; x += 3.7) {
                for (double y = 0.0; y < 360.0; y += 5.3) {
                    Antiscia.Point p = point("A", x);
                    worst = Math.max(worst, Math.abs(Aspects.separation(p.antiscion, y)
                        + Aspects.separation(p.contraAntiscion, y) - 180.0));
                }
            }
            ok("the two mirror distances always add to 180, worst " + worst, worst < 1e-9);
            ok("across the 0 Aries seam", contact(point("A", 179.7), point("B", 0.6)) != null);
        } finally {
            Antiscia.orb = keep;
        }
    }

    private static Antiscia.Point point(String name, double lon) {
        return new Antiscia.Point(name, lon);
    }

    private static Antiscia.Contact contact(Antiscia.Point a, Antiscia.Point b) {
        return Antiscia.contact(a, b);
    }

    private static void realChart(SwissEph sw, double jd) {
        ChartFrame f = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        Antiscia.Result r = Antiscia.of(f);
        List<String> names = new ArrayList<>();
        for (Antiscia.Point p : r.points) {
            names.add(p.name);
        }
        ok("the lights, planets and Ascendant, got " + names, names.contains("Sun")
            && names.contains("Pluto") && names.contains("Ascendant") && !names.contains("Ceres")
            && !names.contains("North Node"));
        int expected = 0;
        for (int i = 0; i < r.points.size(); i++) {
            for (int j = i + 1; j < r.points.size(); j++) {
                double a = Aspects.separation(Zodiac.antiscion(r.points.get(i).longitude),
                    r.points.get(j).longitude);
                double c = Aspects.separation(Zodiac.contraAntiscion(r.points.get(i).longitude),
                    r.points.get(j).longitude);
                expected += Math.min(a, c) <= Antiscia.orb ? 1 : 0;
            }
        }
        ok("every pair in orb, once: " + r.contacts.size() + " of " + expected,
            r.contacts.size() == expected);
        boolean sorted = true;
        for (int i = 1; i < r.contacts.size(); i++) {
            sorted &= r.contacts.get(i - 1).off <= r.contacts.get(i).off;
        }
        ok("tightest first", sorted);
        // The same pairs over a year of charts, so the count is not one chart's accident.
        int total = 0;
        int missed = 0;
        for (int k = 0; k < 24; k++) {
            ChartFrame g = ChartFrame.compute(sw, jd + k * 15.2, 39.9526, -75.1652, 'W', false, 0.0);
            Antiscia.Result s = Antiscia.of(g);
            int want = 0;
            for (int i = 0; i < s.points.size(); i++) {
                for (int j = i + 1; j < s.points.size(); j++) {
                    double a = Aspects.separation(Zodiac.antiscion(s.points.get(i).longitude),
                        s.points.get(j).longitude);
                    double c = Aspects.separation(Zodiac.contraAntiscion(s.points.get(i).longitude),
                        s.points.get(j).longitude);
                    want += Math.min(a, c) <= Antiscia.orb ? 1 : 0;
                }
            }
            total += want;
            missed += Math.abs(want - s.contacts.size());
        }
        ok("24 charts across a year: " + total + " contacts, " + missed + " missed or extra",
            missed == 0 && total > 0);
    }

    private static void noTime(SwissEph sw, double jd) {
        ChartFrame f = ChartFrame.computeTimeUnknown(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        boolean angles = false;
        for (Antiscia.Point p : Antiscia.of(f).points) {
            angles |= p.name.equals("Ascendant") || p.name.equals("MC");
        }
        ok("a chart with no birth time mirrors no angles", !angles);
    }

    private static void table(SwissEph sw, double jd) {
        ChartFrame f = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        String html = com.zodiacomputing.ourania.gui.ChartTables.antiscia(f);
        Antiscia.Result r = Antiscia.of(f);
        ok("the table lists every point", r.points.stream().allMatch(p -> html.contains(">" + p.name + "<")));
        ok("and every contact", r.contacts.stream().allMatch(c -> html.contains(c.a + " &ndash; " + c.b)));
        ok("or says there are none", !r.contacts.isEmpty() || html.contains("No point stands"));
    }

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
