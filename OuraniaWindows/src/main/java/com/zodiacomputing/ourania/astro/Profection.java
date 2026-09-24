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

    /**
     * When each sub-period runs, Julian day UT, or NaN before {@link #computeSubPeriods}.
     *
     * <b>Carried because a period nobody can date is half a period.</b> "Your month is Cancer"
     * invites exactly one question - until when - and the arithmetic that answers it is already
     * done here. Leaving the caller to redo it is how one rule becomes two.
     */
    public double monthlyFrom = Double.NaN;
    public double monthlyUntil = Double.NaN;
    public double dailyFrom = Double.NaN;
    public double dailyUntil = Double.NaN;

    /** How long one profected month runs, in days. NaN until the sub-periods are computed. */
    public double monthLength = Double.NaN;

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
     * The monthly and daily profections, dividing this year into twelve and each month again.
     *
     * <b>The year is the one this chart actually has, not a round number.</b> This used 30-day
     * months, which its own comment called idealised and which produced a <b>thirteenth
     * month</b>: twelve thirties are 360, the year is 365.2422, and the remaining five and a
     * quarter days came out as {@code sign + 12} - the annual sign over again. Measured on
     * 2026-09-24: at day 359.9 the month index is 11, and at day 360.0 it is 12. No tradition
     * has a thirteenth month, and F6 was about to put this on screen.
     *
     * David chose the truest of the three ways out: divide the span between this solar return
     * and the next. A mean year would have removed the thirteenth month too; this also gets the
     * length right for the particular year, which runs about 30.43 to 30.45 days a month
     * depending on where the Earth is in its orbit.
     *
     * Each month divides again by twelve, which is the daily profection - so a daily period is a
     * hundred and forty-fourth of the year, a little over two and a half days.
     *
     * @param targetJd       the moment being read
     * @param solarReturnJd  this year's return, where the year starts
     * @param nextReturnJd   next year's return, where it ends
     */
    public void computeSubPeriods(double targetJd, double solarReturnJd, double nextReturnJd) {
        if (Double.isNaN(solarReturnJd) || Double.isNaN(nextReturnJd)) {
            return;
        }
        double yearLength = nextReturnJd - solarReturnJd;
        if (!(yearLength > 0.0)) {
            return;
        }
        // <b>The year turns at the return, and at() could only guess that.</b> at() takes the
        // age from the calendar month and day, having no ephemeris to ask; the javadoc on
        // solarReturnJd already says why that is not the boundary - "a reading generated within
        // a day of a birthday can land in the wrong year if it trusts the calendar". This method
        // is handed the real boundary, so it is the one place that can put right what at() had
        // to assume, and it corrects the annual profection along with the sub-periods rather
        // than leaving an object that disagrees with itself.
        //
        // Found by ProfectionCheck asserting that the year's last month differs from its first.
        // Both read the same sign: near the birthday the annual sign had already stepped on
        // while the month index still counted from the old year, and the two errors cancelled
        // exactly - which is how a wrong answer comes to look right.
        //
        // The neighbouring year's length stands in for the one being stepped into. They differ
        // by seconds, against a second root-find for a boundary this is only crossing.
        double sinceReturn = targetJd - solarReturnJd;
        if (sinceReturn < 0.0) {
            solarReturnJd -= yearLength;
            sinceReturn += yearLength;
            setAge(this.age - 1);
        } else if (sinceReturn >= yearLength) {
            solarReturnJd += yearLength;
            sinceReturn -= yearLength;
            setAge(this.age + 1);
        }

        this.monthLength = yearLength / 12.0;
        // <b>Clamped, not wrapped.</b> A target sitting exactly on the next return divides to
        // 12, which is the thirteenth month this method exists to be rid of.
        int elapsedMonths = Math.min(11, (int) Math.floor(sinceReturn / this.monthLength));
        this.monthlySign = Math.floorMod(this.sign + elapsedMonths, 12);
        this.monthlyLord = Dignity.domicileRulerOf(this.monthlySign);
        this.monthlyFrom = solarReturnJd + elapsedMonths * this.monthLength;
        this.monthlyUntil = this.monthlyFrom + this.monthLength;

        double dayLength = this.monthLength / 12.0;
        int elapsedDays = Math.max(0, Math.min(11,
            (int) Math.floor((targetJd - this.monthlyFrom) / dayLength)));
        this.dailySign = Math.floorMod(this.monthlySign + elapsedDays, 12);
        this.dailyLord = Dignity.domicileRulerOf(this.dailySign);
        this.dailyFrom = this.monthlyFrom + elapsedDays * dayLength;
        this.dailyUntil = this.dailyFrom + dayLength;
    }

    /**
     * Move the annual profection to another age, re-deriving everything that follows from it.
     *
     * Kept beside {@link #at}, which is the only other place these four fields are set, so the
     * derivation lives once. A second copy of "house is age mod twelve, sign is the Ascendant's
     * plus that, lord rules the sign" is how the annual and the monthly would come apart again.
     */
    private void setAge(int newAge) {
        this.age = newAge;
        int step = Math.floorMod(newAge, 12);
        this.house = step + 1;
        this.sign = Math.floorMod(this.ascendantSign + step, 12);
        this.lord = Dignity.domicileRulerOf(this.sign);
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
