package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;

/**
 * L8: annual profections.
 *
 * The whole technique is one modulo. Each completed year of life advances the chart by one
 * whole sign from the Ascendant; the ruler of the sign arrived at is the lord of the year,
 * and that body's natal condition is the interpretive payload.
 *
 * Two things make this the cheapest timing technique worth having:
 *
 *   - It needs a birth DATE only. No birth time, so it survives a chart whose time is
 *     unknown or wrong - which is most charts, and currently every chart in this app.
 *   - It hands L8 a filter. A transit matters when its natal target matters, and "lord of
 *     the year" is one of the few things that promotes an otherwise ordinary natal body
 *     into significance for a specific twelve months.
 *
 * The year turns on the birthday, not on 1 January. The precise turn is the solar return -
 * the moment the Sun regains its natal longitude - which can differ from the calendar
 * anniversary by up to a day or so; see solarReturnJd.
 */
public final class Profection {

    /** Whole-sign house profected to, 1..12. */
    public int house;
    /** Sign index of that house, 0..11, indexed as Zodiac.SIGNS. */
    public int sign;
    /** Domicile ruler of the profected sign. */
    public String lord;
    /** Completed years of age at the target date. */
    public int age;
    /** The natal Ascendant's sign index, which house 1 always is. */
    public int ascendantSign;
    
    /** Sub-periods (monthly and daily) */
    public int monthlySign = -1;
    public String monthlyLord;
    public int dailySign = -1;
    public String dailyLord;

    private Profection() { }

    /**
     * @param natalJd     birth moment, Julian day UT. Only its calendar date is used.
     * @param targetJd    the date being asked about.
     * @param natalAsc    natal Ascendant longitude in degrees.
     */
    public static Profection at(double natalJd, double targetJd, double natalAsc) {
        Profection p = new Profection();
        p.age = completedYears(natalJd, targetJd);
        p.ascendantSign = Zodiac.signIndex(natalAsc);
        int step = Math.floorMod(p.age, 12);
        p.house = step + 1;
        p.sign = Math.floorMod(p.ascendantSign + step, 12);
        p.lord = Dignity.domicileRulerOf(p.sign);
        return p;
    }

    /**
     * Completed years from the birthday, not calendar-year difference.
     *
     * Someone born in December is 0 in the following January, not 1. Getting this wrong
     * shifts every profection by a whole house for the part of the year before the
     * birthday, which is a plausible-looking answer that is simply the wrong house.
     */
    public static int completedYears(double natalJd, double targetJd) {
        SweDate n = new SweDate(natalJd);
        SweDate t = new SweDate(targetJd);
        int years = t.getYear() - n.getYear();
        if (t.getMonth() < n.getMonth()
                || (t.getMonth() == n.getMonth() && t.getDay() < n.getDay())) {
            years--;
        }
        return years;
    }

    /**
     * The exact solar return for the profection year containing targetJd: the moment the
     * Sun regains its natal longitude.
     *
     * This is the true boundary of a profection year. The calendar anniversary is close
     * but not identical - the tropical year is not 365 days - so a reading generated
     * within a day of a birthday can land in the wrong year if it trusts the calendar.
     *
     * Uses the Almanac root finder rather than a second implementation of bisection.
     */
    public static double solarReturnJd(de.thmac.swisseph.SwissEph sw, double natalJd,
                                       double natalSunLon, int forAge) {
        // Bracket generously around the calendar anniversary; the return is within days.
        double guess = natalJd + forAge * 365.2422;
        double lo = guess - 5.0;
        double hi = guess + 5.0;
        Almanac.OfTime f = jd -> Almanac.signedDelta(Almanac.bodyLongitude(sw, jd, "Sun"),
            natalSunLon);
        java.util.List<Double> roots = Almanac.roots(f, lo, hi, 0.5);
        if (roots.isEmpty()) {
            return Double.NaN;
        }
        // If the bracket caught more than one crossing, take the one nearest the guess.
        double best = roots.get(0);
        for (double r : roots) {
            if (Math.abs(r - guess) < Math.abs(best - guess)) {
                best = r;
            }
        }
        return best;
    }

    /** "Age 43, 8th house profected, Cancer, lord Moon". */
    @Override
    public String toString() {
        String base = String.format("age %d, house %d profected, %s, lord %s",
            age, house, capitalise(Zodiac.SIGNS[sign]), lord);
        if (monthlyLord != null && dailyLord != null) {
            base += String.format(" | month: %s (%s) | day: %s (%s)",
                capitalise(Zodiac.SIGNS[monthlySign]), monthlyLord,
                capitalise(Zodiac.SIGNS[dailySign]), dailyLord);
        }
        return base;
    }

    /**
     * Compute sub-periods using 360-day idealized math.
     * 1 month = 30 days. 1 day = 2.5 days.
     */
    public void computeSubPeriods(double targetJd, double solarReturnJd) {
        if (Double.isNaN(solarReturnJd)) return;
        double daysSinceReturn = targetJd - solarReturnJd;
        if (daysSinceReturn < 0) {
            daysSinceReturn += 365.2422; // handle pre-return gap by wrapping from previous
        }
        
        int elapsedMonths = (int) Math.floor(daysSinceReturn / 30.0);
        this.monthlySign = Math.floorMod(this.sign + elapsedMonths, 12);
        this.monthlyLord = Dignity.domicileRulerOf(this.monthlySign);

        double daysInMonth = daysSinceReturn - (elapsedMonths * 30.0);
        int elapsedDays = (int) Math.floor(daysInMonth / 2.5);
        this.dailySign = Math.floorMod(this.monthlySign + elapsedDays, 12);
        this.dailyLord = Dignity.domicileRulerOf(this.dailySign);
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
