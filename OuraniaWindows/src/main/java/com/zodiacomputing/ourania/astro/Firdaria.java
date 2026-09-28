package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * F8: Firdaria - the Persian time lords, seventy-five years of them.
 *
 * <p>Life is divided into nine consecutive periods, each governed by one lord, and the seven
 * planetary ones subdivide again into seven. Which lord opens the sequence depends on the
 * <b>sect</b> of the chart: a day birth begins with the Sun, a night birth with the Moon.
 *
 * <p><b>One order, entered at two points.</b> The day and night sequences are not two tables -
 * they are the same Chaldean cycle started in different places, day at the Sun and night at the
 * Moon, with the two nodes appended after the seven planets in both. Written as two arrays they
 * could drift, and this project's most expensive defect is a rule implemented twice; written as
 * one cycle with an entry point, a night sequence that disagreed with the day one would be a
 * compile error rather than a wrong reading. {@link FirdariaCheck} asserts the rotation directly.
 *
 * <p><b>Conventions, stated because they are choices.</b>
 * <ul>
 *   <li><b>The year is the tropical year</b>, borrowed from {@link SolarArc#DAYS_PER_YEAR} rather
 *       than written again here. Releasing's idealized 360-day year is deliberately not used -
 *       that convention belongs to releasing's own month-and-day subdivisions, and firdaria
 *       periods are counted in years of life.</li>
 *   <li><b>The nodes have no sub-periods.</b> Bonatti gives sub-periods for the seven planets
 *       and not for the Head and Tail, and this follows him. A reader is told the node period is
 *       undivided rather than shown seven invented divisions of it.</li>
 *   <li><b>A sub-period begins with its own major lord</b> and continues through the same cycle,
 *       so the first seventh of the Sun's decade is Sun-Sun and the second is Sun-Venus.</li>
 *   <li><b>After seventy-five years the sequence repeats</b> from the beginning, which is what
 *       the tradition does with a native who outlives it.</li>
 * </ul>
 */
public final class Firdaria {

    private Firdaria() { }

    /**
     * The seven planetary lords in Chaldean order, which is the order the periods follow.
     *
     * Day begins at index 0, the Sun; night begins at index 3, the Moon. That is the whole of
     * the difference between the two sequences.
     */
    static final String[] CYCLE = {"Sun", "Venus", "Mercury", "Moon", "Saturn", "Jupiter", "Mars"};

    /** Where a night nativity enters {@link #CYCLE}. */
    static final int NIGHT_START = 3;

    /** The two lunar nodes, which close the sequence in both sects and rule no sub-periods. */
    static final String[] NODES = {"North Node", "South Node"};

    /** Years each lord holds. Keyed by name so the table cannot slip against the order. */
    private static double yearsOf(String lord) {
        switch (lord) {
            case "Sun":     return 10.0;
            case "Venus":   return 8.0;
            case "Mercury": return 13.0;
            case "Moon":    return 9.0;
            case "Saturn":  return 11.0;
            case "Jupiter": return 12.0;
            case "Mars":    return 7.0;
            case "North Node": return 3.0;
            case "South Node": return 2.0;
            default: throw new IllegalArgumentException("no firdaria period for " + lord);
        }
    }

    /** Seventy-five years: the seven planetary periods and the two nodes. */
    public static final double TOTAL_YEARS = 75.0;

    /** How many equal parts a planetary period divides into. */
    public static final int SUBDIVISIONS = 7;

    /** One period, major or sub, with the ages and the moments it spans. */
    public static final class Period {
        /** The lord of the major period. */
        public final String lord;
        /** The lord of the sub-period, or null when this row is a major period. */
        public final String sublord;
        public final double startAge;
        public final double endAge;
        public final double startJd;
        public final double endJd;

        Period(String lord, String sublord, double startAge, double endAge,
               double natalJd) {
            this.lord = lord;
            this.sublord = sublord;
            this.startAge = startAge;
            this.endAge = endAge;
            this.startJd = natalJd + startAge * SolarArc.DAYS_PER_YEAR;
            this.endJd = natalJd + endAge * SolarArc.DAYS_PER_YEAR;
        }

        /** Half-open, so consecutive periods cannot both claim the instant between them. */
        public boolean contains(double jd) {
            return jd >= this.startJd && jd < this.endJd;
        }

        /** How long this period runs, in years. */
        public double years() {
            return this.endAge - this.startAge;
        }

        @Override
        public String toString() {
            return this.sublord == null ? this.lord : this.lord + " / " + this.sublord;
        }
    }

    /**
     * The nine lords in the order this nativity meets them.
     *
     * The seven planets from the sect's entry point, then the two nodes.
     */
    public static List<String> order(boolean diurnal) {
        List<String> out = new ArrayList<>(CYCLE.length + NODES.length);
        int start = diurnal ? 0 : NIGHT_START;
        for (int i = 0; i < CYCLE.length; i++) {
            out.add(CYCLE[(start + i) % CYCLE.length]);
        }
        for (String n : NODES) {
            out.add(n);
        }
        return out;
    }

    /**
     * The major periods, from birth, for as many seventy-five-year cycles as asked for.
     *
     * @param cycles at least 1; a second cycle covers a native past seventy-five.
     */
    public static List<Period> majors(double natalJd, boolean diurnal, int cycles) {
        List<Period> out = new ArrayList<>();
        double age = 0.0;
        for (int c = 0; c < Math.max(1, cycles); c++) {
            for (String lord : order(diurnal)) {
                double len = yearsOf(lord);
                out.add(new Period(lord, null, age, age + len, natalJd));
                age += len;
            }
        }
        return out;
    }

    /**
     * The seven sub-periods of a major period, or empty for a node's.
     *
     * Each is a seventh of the major, beginning with the major's own lord and continuing
     * through the cycle - so the order of the sub-periods does not depend on the sect, only on
     * where their own lord sits in the one cycle.
     */
    public static List<Period> subPeriods(Period major, double natalJd) {
        List<Period> out = new ArrayList<>();
        int at = indexIn(CYCLE, major.lord);
        if (at < 0) {
            return out;                      // a node: undivided, by Bonatti
        }
        double each = major.years() / SUBDIVISIONS;
        for (int i = 0; i < SUBDIVISIONS; i++) {
            String sub = CYCLE[(at + i) % CYCLE.length];
            double from = major.startAge + i * each;
            // The last sub-period ends exactly where the major does rather than at the sum of
            // seven sevenths, so no gap can open between one major period and the next.
            double to = i == SUBDIVISIONS - 1 ? major.endAge : from + each;
            out.add(new Period(major.lord, sub, from, to, natalJd));
        }
        return out;
    }

    /** The major period holding this moment, or null before birth. */
    public static Period majorAt(double natalJd, boolean diurnal, double jd) {
        if (jd < natalJd) {
            return null;
        }
        // Enough cycles to reach the moment asked about, however old the native is.
        int cycles = (int) Math.floor((jd - natalJd) / (TOTAL_YEARS * SolarArc.DAYS_PER_YEAR)) + 1;
        for (Period p : majors(natalJd, diurnal, cycles)) {
            if (p.contains(jd)) {
                return p;
            }
        }
        return null;
    }

    /** The sub-period holding this moment, or null before birth or inside a node's period. */
    public static Period subAt(double natalJd, boolean diurnal, double jd) {
        Period major = majorAt(natalJd, diurnal, jd);
        if (major == null) {
            return null;
        }
        for (Period p : subPeriods(major, natalJd)) {
            if (p.contains(jd)) {
                return p;
            }
        }
        return null;
    }

    private static int indexIn(String[] arr, String want) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(want)) {
                return i;
            }
        }
        return -1;
    }
}
