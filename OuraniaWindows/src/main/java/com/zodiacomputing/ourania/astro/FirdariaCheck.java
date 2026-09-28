package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification and measurement for F8, the firdaria.
 *
 *  Part A - the table and the two sequences. The night order is asserted to BE the day order
 *           rotated to the Moon rather than compared against a second list typed out here,
 *           because a second list is the thing the engine was written to avoid.
 *  Part B - the periods tile a life: they start at birth, they are consecutive, no gap and no
 *           overlap, and they sum to seventy-five years.
 *  Part C - the sub-periods tile their major exactly, begin with their own lord, and the nodes
 *           have none.
 *  Part D - the lookups agree with the lists, and the boundary between two periods belongs to
 *           exactly one of them. Half-open intervals are easy to write and easy to get wrong in
 *           the direction nobody notices - both periods claiming the instant.
 *  Part E - measurement on a real chart: which lord is running, and when it hands over.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.FirdariaCheck
 */
public final class FirdariaCheck {

    // The synthetic reference chart TopicCheck and JoyCheck read - 1984-09-08 07:33 UT,
    // 41.8781 N 87.6298 W. Not anyone's real birth data.
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

        System.out.println("=== Part A: the table, and one cycle entered at two points ===");
        theTable();

        System.out.println();
        System.out.println("=== Part B: the periods tile a life ===");
        theyTile();

        System.out.println();
        System.out.println("=== Part C: the sub-periods tile their major ===");
        subPeriods();

        System.out.println();
        System.out.println("=== Part D: the lookups, and the instant between two periods ===");
        lookups();

        System.out.println();
        System.out.println("=== Part E: a real chart ===");
        measurement();

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

    private static void theTable() {
        List<String> day = Firdaria.order(true);
        List<String> night = Firdaria.order(false);

        eq("nine lords rule a day nativity", 9, day.size());
        eq("and nine a night one", 9, night.size());
        eq("a day nativity opens with the Sun", "Sun", day.get(0));
        eq("a night nativity opens with the Moon", "Moon", night.get(0));

        // <b>The property, not a second table.</b> If the night sequence were typed out here it
        // could agree with a wrong engine, and the two lists in the engine could drift apart
        // without either check noticing. Asserting the rotation says the only thing that is
        // actually true: there is one order, entered in two places.
        for (int i = 0; i < Firdaria.CYCLE.length; i++) {
            eq("night lord " + i + " is the day sequence rotated to the Moon",
                day.get((Firdaria.NIGHT_START + i) % Firdaria.CYCLE.length), night.get(i));
        }
        eq("both sects close on the same two nodes",
            day.subList(7, 9).toString(), night.subList(7, 9).toString());
        eq("and the nodes come last", "South Node", day.get(8));

        // Every planet appears once and only once in each sect's sequence.
        for (String lord : Firdaria.CYCLE) {
            eq("the day sequence holds " + lord + " once", 1, count(day, lord));
            eq("the night sequence holds " + lord + " once", 1, count(night, lord));
        }
    }

    // ---------------------------------------------------------------- part B

    private static void theyTile() {
        double natalJd = natalJd();
        for (boolean diurnal : new boolean[] {true, false}) {
            String sect = diurnal ? "day" : "night";
            List<Firdaria.Period> majors = Firdaria.majors(natalJd, diurnal, 1);
            eq("a " + sect + " nativity has nine major periods", 9, majors.size());
            eq("the first begins at birth", 0.0, round(majors.get(0).startAge));
            eq("the last ends at seventy-five",
                Firdaria.TOTAL_YEARS, round(majors.get(majors.size() - 1).endAge));

            double sum = 0.0;
            for (int i = 0; i < majors.size(); i++) {
                Firdaria.Period p = majors.get(i);
                sum += p.years();
                yes(sect + ": " + p.lord + " runs a positive length", p.years() > 0.0);
                if (i > 0) {
                    // No gap and no overlap: one period's end IS the next one's start.
                    eq(sect + ": " + p.lord + " starts where " + majors.get(i - 1).lord + " ends",
                        round(majors.get(i - 1).endAge), round(p.startAge));
                    eq(sect + ": and by julian day too",
                        round(majors.get(i - 1).endJd), round(p.startJd));
                }
            }
            eq(sect + ": the nine periods sum to seventy-five years", Firdaria.TOTAL_YEARS, round(sum));

            // A second cycle continues rather than restarting the clock.
            List<Firdaria.Period> two = Firdaria.majors(natalJd, diurnal, 2);
            eq(sect + ": two cycles are eighteen periods", 18, two.size());
            eq(sect + ": the second cycle opens where the first closed",
                Firdaria.TOTAL_YEARS, round(two.get(9).startAge));
            eq(sect + ": and with the same lord it opened with",
                two.get(0).lord, two.get(9).lord);
        }
    }

    // ---------------------------------------------------------------- part C

    private static void subPeriods() {
        double natalJd = natalJd();
        int planetary = 0;
        int nodal = 0;
        for (Firdaria.Period major : Firdaria.majors(natalJd, true, 1)) {
            List<Firdaria.Period> subs = Firdaria.subPeriods(major, natalJd);
            boolean isNode = major.lord.endsWith("Node");
            if (isNode) {
                nodal++;
                // Bonatti gives the nodes no sub-periods. Showing seven invented ones would be
                // a confident answer where the tradition declines to give one.
                eq("the " + major.lord + " period is undivided", 0, subs.size());
                continue;
            }
            planetary++;
            eq(major.lord + " divides into seven", Firdaria.SUBDIVISIONS, subs.size());
            eq("and the first seventh is its own lord", major.lord, subs.get(0).sublord);
            eq("each sub-period names its major", major.lord, subs.get(0).lord);
            eq("the subs begin where the major begins",
                round(major.startAge), round(subs.get(0).startAge));
            eq("and end exactly where the major ends",
                round(major.endAge), round(subs.get(subs.size() - 1).endAge));

            double sum = 0.0;
            for (int i = 0; i < subs.size(); i++) {
                sum += subs.get(i).years();
                if (i > 0) {
                    eq(major.lord + " sub " + i + " starts where the previous ends",
                        round(subs.get(i - 1).endAge), round(subs.get(i).startAge));
                }
            }
            eq(major.lord + "'s sevenths sum to the whole", round(major.years()), round(sum));

            // Each of the seven planets rules exactly one seventh, whichever lord owns the major.
            for (String planet : Firdaria.CYCLE) {
                int seen = 0;
                for (Firdaria.Period s : subs) {
                    if (planet.equals(s.sublord)) {
                        seen++;
                    }
                }
                eq(major.lord + "'s sevenths include " + planet + " once", 1, seen);
            }
        }
        eq("seven planetary periods divide", 7, planetary);
        eq("two nodal periods do not", 2, nodal);
    }

    // ---------------------------------------------------------------- part D

    private static void lookups() {
        double natalJd = natalJd();
        List<Firdaria.Period> majors = Firdaria.majors(natalJd, true, 1);

        yes("nothing rules before birth", Firdaria.majorAt(natalJd, true, natalJd - 1.0) == null);
        Firdaria.Period atBirth = Firdaria.majorAt(natalJd, true, natalJd);
        yes("the first lord rules the birth moment itself",
            atBirth != null && atBirth.lord.equals(majors.get(0).lord));

        for (Firdaria.Period p : majors) {
            double middle = (p.startJd + p.endJd) / 2.0;
            Firdaria.Period found = Firdaria.majorAt(natalJd, true, middle);
            yes("the middle of " + p.lord + "'s period is " + p.lord + "'s",
                found != null && found.lord.equals(p.lord));
        }

        // <b>The boundary is the case worth asserting.</b> Half-open intervals are easy to write
        // and easy to get wrong in the direction nobody sees: both periods claiming the instant,
        // which shows up as a lookup that depends on list order rather than on time.
        for (int i = 1; i < majors.size(); i++) {
            double boundary = majors.get(i).startJd;
            yes(majors.get(i - 1).lord + " has let go by its own end",
                !majors.get(i - 1).contains(boundary));
            yes("and " + majors.get(i).lord + " has taken over exactly there",
                majors.get(i).contains(boundary));
        }

        // Past seventy-five the sequence repeats rather than running out.
        double past = natalJd + 80.0 * SolarArc.DAYS_PER_YEAR;
        Firdaria.Period late = Firdaria.majorAt(natalJd, true, past);
        yes("a native of eighty still has a lord", late != null);
        if (late != null) {
            yes("and it is a second turn of the same wheel, at age "
                + String.format("%.1f", late.startAge), late.startAge >= Firdaria.TOTAL_YEARS);
        }

        Firdaria.Period sub = Firdaria.subAt(natalJd, true, natalJd + 365.0);
        yes("a sub-period is found inside a planetary period", sub != null);
        if (sub != null) {
            yes("and it names both its lords", sub.sublord != null && sub.lord != null);
        }
    }

    // ---------------------------------------------------------------- part E

    private static void measurement() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double natalJd = natalJd();
        ChartFrame f = ChartFrame.compute(sw, natalJd, NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        ChartFrame.Body sun = f.body("Sun");
        yes("the reference chart has a Sun to take the sect from", sun != null && sun.ok);
        if (sun == null || !sun.ok) {
            return;
        }
        boolean diurnal = Sect.isDiurnal(sun.lon, f.asc);
        System.out.println("  reference chart is a " + (diurnal ? "day" : "night") + " nativity");
        System.out.println("  lords in order: " + String.join(", ", Firdaria.order(diurnal)));

        double now = new SweDate().getJulDay();
        Firdaria.Period major = Firdaria.majorAt(natalJd, diurnal, now);
        yes("a lord is running now", major != null);
        if (major == null) {
            return;
        }
        Firdaria.Period sub = Firdaria.subAt(natalJd, diurnal, now);
        System.out.printf("  now: %s%s, ages %.1f to %.1f%n", major.lord,
            sub == null ? " (undivided)" : " / " + sub.sublord, major.startAge, major.endAge);
        yes("the running period contains this moment", major.contains(now));
        yes("and its sub-period sits inside it",
            sub == null || (sub.startAge >= major.startAge && sub.endAge <= major.endAge));
    }

    // ---------------------------------------------------------------- harness

    private static double natalJd() {
        return new SweDate(NATAL_Y, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
    }

    private static int count(List<String> in, String want) {
        int n = 0;
        for (String s : in) {
            if (s.equals(want)) {
                n++;
            }
        }
        return n;
    }

    /** Rounded to a thousandth, so a tiling assertion is not defeated by floating point. */
    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

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
}
