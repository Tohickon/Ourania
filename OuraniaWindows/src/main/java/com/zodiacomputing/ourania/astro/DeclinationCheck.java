package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Declinations: out of bounds when the sky says so, and parallels by the rule.
 *
 * <p>Part A leans on facts about the Moon that do not come from this code. The Moon's greatest
 * declination swings over an 18.6-year cycle: at the minor standstill of late 2015 it reached
 * only about 18&deg;, so it could not leave the Sun's bounds all year; at the major standstill
 * of 2024-25 it reached about 28.5&deg;, and was out of bounds for several days of every month.
 * A rule that measured the wrong thing - right ascension, ecliptic latitude, a fixed 23.5 - would
 * get one of the two years wrong.
 */
public final class DeclinationCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        part("A: the Moon's standstills", () -> standstills(sw));
        part("B: the Sun defines the bound and is never outside it", () -> theSun(sw));
        part("C: parallel and contraparallel, by the rule", DeclinationCheck::rule);
        part("D: a real chart", () -> realChart(sw));
        part("E: the table says it", () -> table(sw));

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

    private static ChartFrame frame(SwissEph sw, double jd) {
        return ChartFrame.compute(sw, jd, 51.4779, 0.0, 'W', false, 0.0);
    }

    private static Declinations.Entry moon(Declinations.Result r) {
        for (Declinations.Entry e : r.entries) {
            if (e.name.equals("Moon")) {
                return e;
            }
        }
        return null;
    }

    /**
     * The year is scanned with a direct declination call - a chart frame costs far more, and a
     * first version that built 2,000 of them ran past ten minutes - and a full frame is then
     * built at the year's extreme moment and on both sides of the bound, which is where the
     * rule under test can be wrong.
     */
    private static void standstills(SwissEph sw) {
        double[] minor = extreme(sw, de.thmac.swisseph.SweConst.SE_MOON, 2015);
        ok("2015, minor standstill: the Moon's greatest declination is near 18 degrees, got "
            + String.format("%.2f", minor[1]), minor[1] > 17.5 && minor[1] < 19.0);
        Declinations.Entry m15 = moon(Declinations.of(frame(sw, minor[0])));
        ok("and at that moment it is in bounds", m15 != null && !m15.outOfBounds && m15.beyond == 0.0);

        double[] major = extreme(sw, de.thmac.swisseph.SweConst.SE_MOON, 2025);
        ok("2025, major standstill: the Moon's greatest declination is near 28.5 degrees, got "
            + String.format("%.2f", major[1]), major[1] > 28.0 && major[1] < 29.0);
        ChartFrame at = frame(sw, major[0]);
        Declinations.Entry m25 = moon(Declinations.of(at));
        ok("and at that moment it is out of bounds", m25 != null && m25.outOfBounds);
        ok("by its declination less the obliquity, got " + (m25 == null ? null : m25.beyond),
            m25 != null && Math.abs(m25.beyond - (Math.abs(m25.declination) - at.trueObliquity)) < 1e-9
                && m25.beyond > 4.0);

        // Either side of the bound: walk from the extreme back until the Moon returns inside
        // it, and check the frame a quarter-day either side of the crossing.
        double jd = major[0];
        while (Math.abs(dec(sw, de.thmac.swisseph.SweConst.SE_MOON, jd)) > at.trueObliquity) {
            jd -= 0.25;
        }
        Declinations.Entry inside = moon(Declinations.of(frame(sw, jd)));
        Declinations.Entry outside = moon(Declinations.of(frame(sw, jd + 0.25)));
        ok("just inside the bound it is in bounds", inside != null && !inside.outOfBounds);
        ok("a quarter-day later it is out", outside != null && outside.outOfBounds);
    }

    private static double dec(SwissEph sw, int ipl, double jd) {
        double[] xx = new double[6];
        sw.swe_calc_ut(jd, ipl, de.thmac.swisseph.SweConst.SEFLG_SWIEPH
            | de.thmac.swisseph.SweConst.SEFLG_EQUATORIAL, xx, new StringBuffer());
        return xx[1];
    }

    /** {moment, |declination|} of a body's greatest declination in a year, to a quarter-day. */
    private static double[] extreme(SwissEph sw, int ipl, int year) {
        double from = SweDate.getJulDay(year, 1, 1, 0.0);
        double best = 0.0;
        double when = from;
        for (double jd = from; jd < from + 365.0; jd += 0.25) {
            double d = Math.abs(dec(sw, ipl, jd));
            if (d > best) {
                best = d;
                when = jd;
            }
        }
        return new double[]{when, best};
    }

    private static void theSun(SwissEph sw) {
        double[] solstice = extreme(sw, de.thmac.swisseph.SweConst.SE_SUN, 2026);
        ChartFrame f = frame(sw, solstice[0]);
        Declinations.Entry sun = null;
        for (Declinations.Entry e : Declinations.of(f).entries) {
            if (e.name.equals("Sun")) {
                sun = e;
            }
        }
        ok("at the 2026 solstice the Sun is in the report", sun != null);
        ok("and not out of bounds", sun != null && !sun.outOfBounds);
        ok("its declination is the obliquity, to a hundredth, got "
            + (sun == null ? null : String.format("%.4f vs %.4f", sun.declination, f.trueObliquity)),
            sun != null && Math.abs(Math.abs(sun.declination) - f.trueObliquity) < 0.01);
        // Why the exemption exists, printed rather than asserted since it is arcseconds either way.
        if (sun != null) {
            System.out.println("  the Sun past the obliquity at the solstice: "
                + String.format("%.6f", Math.abs(sun.declination) - f.trueObliquity) + " degrees");
        }
    }

    private static void rule() {
        double keep = Declinations.parallelOrb;
        Declinations.parallelOrb = 1.0;
        try {
            Declinations.Entry a = new Declinations.Entry("A", 10.0, false, 0.0);
            ok("same side, 0.9 apart: parallel",
                isParallel(Declinations.contact(a, new Declinations.Entry("B", 10.9, false, 0.0))));
            ok("same side, 1.1 apart: nothing",
                Declinations.contact(a, new Declinations.Entry("B", 11.1, false, 0.0)) == null);
            Declinations.Contact c = Declinations.contact(a,
                new Declinations.Entry("B", -10.5, false, 0.0));
            ok("opposite sides, 0.5 apart: contraparallel", c != null && c.contra);
            ok("with the orb measured across the equator, got " + (c == null ? null : c.off),
                c != null && Math.abs(c.off - 0.5) < 1e-9);
            ok("opposite sides, 1.5 apart: nothing",
                Declinations.contact(a, new Declinations.Entry("B", -11.5, false, 0.0)) == null);
            Declinations.Contact near = Declinations.contact(
                new Declinations.Entry("A", 0.2, false, 0.0),
                new Declinations.Entry("B", -0.3, false, 0.0));
            ok("within orb both ways near the equator, the tighter is reported: contraparallel 0.1",
                near != null && near.contra && Math.abs(near.off - 0.1) < 1e-9);
            Declinations.Contact tie = Declinations.contact(
                new Declinations.Entry("A", 0.0, false, 0.0),
                new Declinations.Entry("B", 0.3, false, 0.0));
            ok("and a tie is a parallel", tie != null && !tie.contra);
            Declinations.Contact ab = Declinations.contact(a,
                new Declinations.Entry("B", 10.4, false, 0.0));
            Declinations.Contact ba = Declinations.contact(
                new Declinations.Entry("B", 10.4, false, 0.0), a);
            ok("the same either way round", ab != null && ba != null && ab.contra == ba.contra
                && ab.off == ba.off);
        } finally {
            Declinations.parallelOrb = keep;
        }
    }

    private static boolean isParallel(Declinations.Contact c) {
        return c != null && !c.contra;
    }

    private static void realChart(SwissEph sw) {
        double birth = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);
        ChartFrame f = ChartFrame.compute(sw, birth, 39.9526, -75.1652, 'P', false, 0.0);
        Declinations.Result r = Declinations.of(f);
        List<String> names = new ArrayList<>();
        for (Declinations.Entry e : r.entries) {
            names.add(e.name);
        }
        ok("the lights and the planets, got " + names, names.contains("Sun")
            && names.contains("Moon") && names.contains("Pluto") && !names.contains("Ceres")
            && !names.contains("North Node") && !names.contains("Ascendant"));
        boolean agree = true;
        for (Declinations.Entry e : r.entries) {
            ChartFrame.Body b = f.body(e.name);
            agree &= b != null && b.dec == e.declination;
        }
        ok("every declination is the frame's own", agree);
        boolean within = true;
        boolean sorted = true;
        for (int i = 0; i < r.contacts.size(); i++) {
            Declinations.Contact c = r.contacts.get(i);
            within &= c.off <= Declinations.parallelOrb;
            sorted &= i == 0 || r.contacts.get(i - 1).off <= c.off;
        }
        ok("every contact is within orb", within);
        ok("tightest first", sorted);
        // Brute force: every pair within orb is reported exactly once.
        int expected = 0;
        for (int i = 0; i < r.entries.size(); i++) {
            for (int j = i + 1; j < r.entries.size(); j++) {
                double p = Math.abs(r.entries.get(i).declination - r.entries.get(j).declination);
                double q = Math.abs(r.entries.get(i).declination + r.entries.get(j).declination);
                expected += Math.min(p, q) <= Declinations.parallelOrb ? 1 : 0;
            }
        }
        ok("every pair within orb, once: " + r.contacts.size() + " of " + expected,
            r.contacts.size() == expected);
    }

    private static void table(SwissEph sw) {
        // A moment in the 2025 standstill with the Moon out of bounds, found rather than assumed.
        double from = SweDate.getJulDay(2025, 1, 1, 0.0);
        ChartFrame oob = null;
        for (double jd = from; jd < from + 60.0 && oob == null; jd += 0.25) {
            ChartFrame f = frame(sw, jd);
            Declinations.Entry m = moon(Declinations.of(f));
            if (m != null && m.outOfBounds) {
                oob = f;
            }
        }
        ok("found an out-of-bounds Moon in early 2025", oob != null);
        if (oob == null) {
            return;
        }
        String html = com.zodiacomputing.ourania.gui.ChartTables.declinations(oob);
        String moonRow = "";
        for (String row : html.split("<tr>")) {
            if (row.contains(">Moon<")) {
                moonRow = row;
            }
        }
        ok("the Moon's row says out of bounds: " + moonRow, moonRow.contains("out of bounds"));
        ok("with its declination printed", moonRow.contains("&deg;") && (moonRow.contains(" N")
            || moonRow.contains(" S")));
        ok("and the table names the parallels section", html.contains("Parallels and contraparallels"));
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
