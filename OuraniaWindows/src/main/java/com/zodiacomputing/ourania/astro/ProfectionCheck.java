package com.zodiacomputing.ourania.astro;

import com.zodiacomputing.ourania.gui.Settings;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Master list F6: the monthly and daily profections.
 *
 * <p>The annual profection has been computed and read for a long time. The two shorter scales
 * were computed too and never shown, which is what made F6 a door rather than a technique - and
 * a door is worth nothing if what is behind it is wrong, so this holds the arithmetic first.
 *
 * <p><b>The thirteenth month.</b> The sub-periods used 30-day months. Twelve of those are 360
 * days and the year is 365.2422, so the last five and a quarter days of every profection year
 * came out as {@code sign + 12} - the annual sign a second time. Measured on 2026-09-24, at day
 * 359.9 the month index was 11 and at day 360.0 it was 12. It is now the span between this solar
 * return and the next, divided by twelve, so there are twelve.
 *
 * <ul>
 * <li><b>A, the year divides into twelve</b> - and into twelve only, walked across a whole year
 * rather than sampled.</li>
 * <li><b>B, the moment is inside the period</b> reported for it, monthly and daily. The single
 * property that catches almost any slip in the arithmetic.</li>
 * <li><b>C, the periods tile the year</b> - contiguous, equal, and covering it exactly.</li>
 * <li><b>D, the signs advance</b> one per month from the annual sign, and one per day from the
 * month's.</li>
 * </ul>
 */
public final class ProfectionCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** David's chart, the one the other suites use. */
    private static final double LAT = 39.9526;
    private static final double LON = -75.1652;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double natalJd = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);
        ChartFrame natal = ChartFrame.compute(sw, natalJd, LAT, LON, 'P', false, 0.0);
        ChartFrame.Body sun = natal.body("Sun");
        if (sun == null || !sun.ok) {
            System.out.println("FAILURES (1 of 1 checks):");
            System.out.println("  the natal Sun is needed to find a solar return");
            System.exit(1);
        }

        // A year in the middle of life, far from any edge case in the root finder.
        int age = 40;
        ephemeris = sw;
        natalSunLon = sun.lon;
        double thisReturn = Profection.solarReturnJd(sw, natalJd, sun.lon, age);
        double nextReturn = Profection.solarReturnJd(sw, natalJd, sun.lon, age + 1);

        part("A: the year divides into twelve", () -> twelve(natalJd, natal, thisReturn, nextReturn));
        part("B: the moment is inside its period",
            () -> inside(natalJd, natal, thisReturn, nextReturn));
        part("C: the periods tile the year", () -> tiling(natalJd, natal, thisReturn, nextReturn));
        part("D: the signs advance", () -> advancing(natalJd, natal, thisReturn, nextReturn));

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

    /** Solar returns by age, so walking a year does not root-find thousands of times. */
    private static final java.util.Map<Integer, double[]> RETURNS = new java.util.HashMap<>();

    private static SwissEph ephemeris;
    private static double natalSunLon;

    /**
     * A profection built the way the reading worker builds one.
     *
     * <b>The span comes from the profection's own age.</b> The first version of this passed one
     * fixed year's returns to every sample, which near the start of the year is a span the
     * profection did not choose - the Sun has returned but the calendar anniversary has not, so
     * at() reports the previous age. That gave an object with an annual sign from one year and a
     * month index from the next, whose errors cancelled exactly and made two real assertions
     * read as failures. The worker asks solarReturnJd for the age it has; so does this.
     */
    private static Profection at(double natalJd, ChartFrame natal, double when) {
        Profection p = Profection.at(natalJd, when, natal.asc);
        double[] span = RETURNS.computeIfAbsent(p.age, a -> new double[] {
            Profection.solarReturnJd(ephemeris, natalJd, natalSunLon, a),
            Profection.solarReturnJd(ephemeris, natalJd, natalSunLon, a + 1)});
        p.computeSubPeriods(when, span[0], span[1]);
        return p;
    }

    /**
     * <b>Twelve months, walked rather than sampled.</b> The defect this replaces lived in the
     * last five days of the year, which any sampling coarser than the tail would step over -
     * so this walks the whole year at a quarter-day and collects every distinct month it is
     * told it is in.
     */
    private static void twelve(double natalJd, ChartFrame natal, double from, double to) {
        double year = to - from;
        Set<Integer> months = new LinkedHashSet<>();
        for (double t = from; t < to; t += 0.25) {
            months.add(at(natalJd, natal, t).monthlySign);
        }
        ok("the year visits twelve months, not thirteen: " + months.size(), months.size() == 12);

        // And the last instant of the year is still in the twelfth, which is where the 30-day
        // scheme wrapped back to the first.
        Profection last = at(natalJd, natal, to - 0.01);
        Profection first = at(natalJd, natal, from + 0.01);
        ok("the year ends in a different month from the one it began in: "
            + last.monthlySign + " against " + first.monthlySign,
            last.monthlySign != first.monthlySign);
        ok("and that month is the twelfth from the annual sign",
            Math.floorMod(last.monthlySign - first.monthlySign, 12) == 11);

        near("a month is a twelfth of this chart's own year", year / 12.0,
            first.monthLength, 1e-9);
        ok("which is not the round 30 that produced the thirteenth: "
            + String.format("%.4f", first.monthLength), Math.abs(first.monthLength - 30.0) > 0.4);
    }

    /**
     * <b>The moment must lie in the period named for it.</b> Everything else here could pass on
     * arithmetic that is internally tidy and off by one; this cannot.
     */
    private static void inside(double natalJd, ChartFrame natal, double from, double to) {
        boolean monthHolds = true;
        boolean dayHolds = true;
        boolean dayInMonth = true;
        String worst = "";
        for (double t = from; t < to; t += 0.37) {
            Profection p = at(natalJd, natal, t);
            boolean m = p.monthlyFrom <= t + 1e-9 && t < p.monthlyUntil + 1e-9;
            boolean d = p.dailyFrom <= t + 1e-9 && t < p.dailyUntil + 1e-9;
            if (!m && worst.isEmpty()) {
                worst = String.format("at %.2f days in: month %.4f..%.4f", t - from,
                    p.monthlyFrom - from, p.monthlyUntil - from);
            }
            monthHolds &= m;
            dayHolds &= d;
            dayInMonth &= p.dailyFrom >= p.monthlyFrom - 1e-9
                && p.dailyUntil <= p.monthlyUntil + 1e-9;
        }
        ok("the moment is inside its monthly period, all year " + worst, monthHolds);
        ok("and inside its daily period", dayHolds);
        ok("and the daily period is inside the monthly one", dayInMonth);
    }

    /** <b>Contiguous and equal.</b> Twelve months that do not meet leave gaps a reader falls into. */
    private static void tiling(double natalJd, ChartFrame natal, double from, double to) {
        double year = to - from;
        double step = year / 12.0;
        boolean meets = true;
        boolean equal = true;
        for (int m = 0; m < 12; m++) {
            Profection p = at(natalJd, natal, from + (m + 0.5) * step);
            meets &= Math.abs(p.monthlyFrom - (from + m * step)) < 1e-6;
            equal &= Math.abs((p.monthlyUntil - p.monthlyFrom) - step) < 1e-9;
        }
        ok("each month begins where the one before it ended", meets);
        ok("and they are all the same length", equal);

        Profection p = at(natalJd, natal, from + 0.5 * step);
        near("the first month begins at the solar return", from, p.monthlyFrom, 1e-6);
        Profection q = at(natalJd, natal, to - 0.01);
        near("and the twelfth ends at the next one", to, q.monthlyUntil, 1e-6);

        near("a daily period is a twelfth of a month", step / 12.0,
            p.dailyUntil - p.dailyFrom, 1e-9);
    }

    /** <b>One sign a month from the annual, one a day from the month's.</b> */
    private static void advancing(double natalJd, ChartFrame natal, double from, double to) {
        double step = (to - from) / 12.0;
        boolean walks = true;
        for (int m = 0; m < 12; m++) {
            Profection p = at(natalJd, natal, from + (m + 0.5) * step);
            walks &= p.monthlySign == Math.floorMod(p.sign + m, 12);
        }
        ok("the month walks one sign at a time from the annual sign", walks);

        boolean days = true;
        double dayLen = step / 12.0;
        Profection anchor = at(natalJd, natal, from + 0.5 * step);
        for (int d = 0; d < 12; d++) {
            Profection p = at(natalJd, natal, from + (d + 0.5) * dayLen);
            days &= p.dailySign == Math.floorMod(anchor.monthlySign + d, 12);
        }
        ok("and the day walks one sign at a time from the month's", days);

        Profection p = at(natalJd, natal, from + 1.0);
        ok("every period names a ruler", p.monthlyLord != null && !p.monthlyLord.isEmpty()
            && p.dailyLord != null && !p.dailyLord.isEmpty());
        ok("and the annual lord rules the annual sign",
            p.lord.equals(Dignity.domicileRulerOf(p.sign)));
    }

    private static void part(String title, Runnable body) {
        System.out.println("=== Part " + title + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + title.charAt(0)
            + (failures.size() == before ? ": clear"
                : ": " + (failures.size() - before) + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void near(String label, double expect, double got, double tol) {
        checks++;
        if (!(Math.abs(expect - got) <= tol)) {
            failures.add(label + ": expected " + expect + ", got " + got);
        }
    }
}
