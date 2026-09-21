package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.DblObj;
import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

/**
 * The planetary day and the planetary hours: K13's first piece, and the one both branches need.
 *
 * <h3>What this is</h3>
 *
 * <p>The oldest timekeeping in the tradition, and the only part of it that is pure arithmetic
 * once you have sunrise. Daylight is divided into twelve <b>unequal</b> hours and the night into
 * twelve more, so a summer day hour runs long and its night hours run short; the two are equal
 * only at an equinox. Each hour is ruled by a planet, walking the Chaldean order - Saturn,
 * Jupiter, Mars, Sun, Venus, Mercury, Moon - and the day's <b>first</b> hour, beginning at
 * sunrise, is ruled by the planet the day is named for.
 *
 * <p><b>That rule is what makes the week's order the week's order</b>, and it is worth stating
 * because it is the check that this class is right. Twenty-four hours stepped through a
 * seven-planet cycle advances by three ({@code 24 mod 7 == 3}), and stepping three places
 * through Saturn-Jupiter-Mars-Sun-Venus-Mercury-Moon gives Saturn, Sun, Moon, Mars, Mercury,
 * Jupiter, Venus: Saturday, Sunday, Monday, Tuesday, Wednesday, Thursday, Friday. The names of
 * the days of the week are a fossil of this calculation, so a bug here disagrees with the
 * calendar on the wall.
 *
 * <h3>The day begins at sunrise, not at midnight</h3>
 *
 * <p>Which is the trap. An hour after midnight on a Tuesday is in <i>Monday's</i> planetary day,
 * because Tuesday's has not begun - the ruler is one of Monday's night hours. Anything reading a
 * calendar date and looking up a day ruler is wrong for the hours between midnight and dawn,
 * every single day.
 *
 * <h3>K13, and why this comes first</h3>
 *
 * <p>Horary's <b>radicality</b> test asks whether the hour ruler shares the nature or
 * triplicity of the Ascendant's ruler - a chart that fails it is held to be not fit to judge.
 * Electional asks the opposite question of the same machinery: which hours favour which work.
 * One calculation, both branches, so it is built and proved on its own before either reads it.
 */
public final class PlanetaryHours {

    private PlanetaryHours() { }

    /**
     * Chaldean order, slowest to fastest, which is the order the hours walk.
     *
     * <p><b>Not the cycle in {@link Zodiac}</b>, which is the same seven rotated to begin at
     * Mars because Mars rules the first decan of Aries. Same planets, same order, different
     * starting point for a different purpose - and rotating one to serve the other would be two
     * meanings in one array, so they stay apart.
     */
    public static final String[] CHALDEAN = {
        "Saturn", "Jupiter", "Mars", "Sun", "Venus", "Mercury", "Moon"
    };

    /** The planet each weekday is named for, indexed as {@code java.time.DayOfWeek} minus one. */
    private static final String[] DAY_RULERS = {
        "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Sun"
    };

    /** One planetary hour: when it runs, who rules it, and whether it is a day or night hour. */
    public static final class Hour {
        /** 1 to 24, counting from the first hour after sunrise. */
        public int index;
        public String ruler;
        public double from;
        public double to;
        public boolean daytime;
        /** The planetary day this hour belongs to - the day that began at its sunrise. */
        public String dayRuler;

        /** Length in ordinary minutes, which is only sixty at an equinox. */
        public double minutes() {
            return (this.to - this.from) * 24.0 * 60.0;
        }

        @Override
        public String toString() {
            return String.format("hour %d of the %s day, ruled by %s (%s, %.0f min)",
                index, dayRuler, ruler, daytime ? "day" : "night", minutes());
        }
    }

    /** A whole planetary day: sunrise to sunrise, with its twenty-four unequal hours. */
    public static final class Day {
        public String ruler;
        public double sunrise;
        public double sunset;
        public double nextSunrise;
        public final java.util.List<Hour> hours = new java.util.ArrayList<>();

        /** The hour holding an instant, or null if it falls outside this day. */
        public Hour at(double jd) {
            for (Hour h : this.hours) {
                if (jd >= h.from && jd < h.to) {
                    return h;
                }
            }
            return null;
        }
    }

    /**
     * The planetary day containing an instant, at a place.
     *
     * <p><b>The instant decides which day, and it is not the calendar's.</b> Before that
     * morning's sunrise the reader is still in yesterday's planetary day, so this looks back a
     * day when it has to rather than trusting the date.
     *
     * @return the day, or null if the Sun neither rises nor sets there then - the polar case,
     *     where unequal hours have no meaning at all and inventing some would be worse than
     *     saying so
     */
    public static Day at(SwissEph sw, double jd, double lat, double lon) {
        double rise = riseOrSet(sw, jd - 1.2, lat, lon, true);
        double set = Double.NaN;
        double next = Double.NaN;
        // Walk forward to the last sunrise at or before the instant.
        for (int i = 0; i < 4 && !Double.isNaN(rise); i++) {
            double after = riseOrSet(sw, rise + 0.01, lat, lon, true);
            if (Double.isNaN(after) || after > jd) {
                next = after;
                break;
            }
            rise = after;
        }
        if (Double.isNaN(rise) || rise > jd || Double.isNaN(next)) {
            return null;
        }
        // <b>Defensive, and searched for rather than assumed.</b> A mutation that replaces this
        // refusal with an invented midday sunset survives the suite, so it is worth saying why:
        // wherever the Sun rises and then fails to set, it also fails to rise again, and the
        // {@code Double.isNaN(next)} test above has already answered. Four latitudes from 66.6
        // to 71 north, across the fortnight either side of the midnight sun beginning, produced
        // no case that reaches here with a good rise and a good next.
        //
        // It stays because the failure it guards against is silent: a NaN sunset makes every
        // hour NaN, {@code at()} then finds no hour for any instant, and the caller is told
        // nothing is wrong. A refusal the reader can see beats twenty-four hours of NaN.
        set = riseOrSet(sw, rise, lat, lon, false);
        if (Double.isNaN(set) || set <= rise || set >= next) {
            return null;
        }

        Day d = new Day();
        d.sunrise = rise;
        d.sunset = set;
        d.nextSunrise = next;
        d.ruler = rulerOfDayStartingAt(rise);

        int first = indexOf(d.ruler);
        double dayHour = (set - rise) / 12.0;
        double nightHour = (next - set) / 12.0;
        for (int i = 0; i < 24; i++) {
            Hour h = new Hour();
            h.index = i + 1;
            h.daytime = i < 12;
            h.ruler = CHALDEAN[(first + i) % CHALDEAN.length];
            h.dayRuler = d.ruler;
            h.from = h.daytime ? rise + dayHour * i : set + nightHour * (i - 12);
            h.to = h.daytime ? rise + dayHour * (i + 1) : set + nightHour * (i - 11);
            d.hours.add(h);
        }
        // Floating point: the last hour must reach the next sunrise exactly, or an instant in
        // the last few milliseconds of the night belongs to no hour at all.
        d.hours.get(11).to = set;
        d.hours.get(23).to = next;
        return d;
    }

    /** The ruler of the hour containing an instant, or the empty string if there is none. */
    public static String rulerAt(SwissEph sw, double jd, double lat, double lon) {
        Day d = at(sw, jd, lat, lon);
        if (d == null) {
            return "";
        }
        Hour h = d.at(jd);
        return h == null ? "" : h.ruler;
    }

    /**
     * Which planet rules a planetary day, from the sunrise that begins it.
     *
     * <b>Named for the calendar day the sunrise falls in</b>, which is the whole of the rule:
     * the planetary day is the one whose morning this is, and it then runs on past midnight
     * into the next calendar date. Read from the Julian day in UT, which is what the rest of
     * the engine speaks.
     */
    static String rulerOfDayStartingAt(double sunriseJd) {
        de.thmac.swisseph.SweDate sd = new de.thmac.swisseph.SweDate();
        sd.setJulDay(sunriseJd);
        java.time.LocalDate date = java.time.LocalDate.of(sd.getYear(), sd.getMonth(), sd.getDay());
        return DAY_RULERS[date.getDayOfWeek().getValue() - 1];
    }

    private static int indexOf(String planet) {
        for (int i = 0; i < CHALDEAN.length; i++) {
            if (CHALDEAN[i].equals(planet)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * The next sunrise or sunset at or after an instant, or NaN.
     *
     * <b>Disc centre, with refraction.</b> The tradition's sunrise is the one a person watching
     * the horizon sees, so the default apparent rise is right; the centre flag is off. What
     * matters more is that rise and set use the same definition, because the twelve day hours
     * are the span between them and a mismatched pair would make every hour slightly wrong in
     * one direction.
     */
    private static double riseOrSet(SwissEph sw, double fromJd, double lat, double lon,
                                    boolean rising) {
        DblObj out = new DblObj();
        StringBuffer err = new StringBuffer();
        int flag = rising ? SweConst.SE_CALC_RISE : SweConst.SE_CALC_SET;
        int rc = sw.swe_rise_trans(fromJd, SweConst.SE_SUN, null,
            Ephemeris.flags(sw, SweConst.SEFLG_SWIEPH), flag,
            new double[] {lon, lat, 0.0}, 1013.25, 10.0, out, err);
        return rc < 0 ? Double.NaN : out.val;
    }
}
