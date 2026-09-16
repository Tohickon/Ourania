package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

/**
 * The progressed Ascendant and Midheaven: master list F4.
 *
 * <p><b>Why they were missing, and why they are here now.</b> {@link Progressions} moves the
 * planets by "one day of ephemeris time for one year of life", and that rule answers nothing about
 * the angles: they turn with the Earth, not with the solar system, so a progressed day of rotation
 * is a whole turn of the horizon. The angles therefore need a rule of their own, and the technique
 * has never settled on one. The exclusion was also held in place by a second thing - the app could
 * not tell a real birth time from a placeholder - and that is gone: a chart carries a
 * {@link Rodden} rating, and one with no time at all is cast with its angles withheld
 * ({@link ChartFrame#timeUnknown}), which is exactly the distinction this needed.
 *
 * <p><b>Three rules, because astrologers use three.</b> Each is named in the reading that uses it,
 * so nothing here pretends there is one answer:
 *
 * <ul>
 * <li><b>Solar arc</b> - the MC advances by the arc the progressed Sun has actually travelled,
 * roughly a degree a year. The default, and the same arc {@link SolarArc} directs the whole chart
 * by, so the two techniques agree about how far the chart has moved.</li>
 * <li><b>Naibod</b> - the MC advances by the Sun's <i>mean</i> motion, 59 minutes 08 seconds of arc
 * a year, so the rate is even rather than following the Sun's real speed. Within about a degree of
 * solar arc at any age; the difference is whether the Sun's own fast and slow years are carried
 * into the angles.</li>
 * <li><b>Quotidian</b> - the angles of the progressed moment itself, taken at the birthplace. They
 * turn about a degree a day of life, so they cross the whole zodiac in a year.</li>
 * </ul>
 *
 * <p><b>Only the first two produce dated contacts.</b> A quotidian angle sweeps the entire zodiac
 * every year, so it makes exact aspects to every natal point several times over - hundreds of
 * perfections a year, which is the wall of true and useless statements {@link Transits} exists to
 * prevent. Its positions are shown; its contacts are not scanned. Solar arc and Naibod move about a
 * degree a year, so a contact from them is an event.
 *
 * <p>The Ascendant is not progressed on its own. It is derived from the progressed MC at the birth
 * latitude through {@link ChartFrame#frameFromMc}, the same derivation the midpoint composite uses,
 * because an Ascendant and an MC advanced independently correspond to no real horizon.
 */
public final class ProgressedAngles {

    private ProgressedAngles() { }

    /** How the angles are progressed. */
    public enum Method {
        SOLAR_ARC("Solar arc", "The MC advances by the arc the progressed Sun has travelled."),
        NAIBOD("Naibod", "The MC advances by the Sun's mean motion, 59 minutes 08 seconds a year."),
        QUOTIDIAN("Quotidian", "The real angles of the progressed moment, turning about a degree "
            + "a day of life.");

        public final String label;
        public final String meaning;

        Method(String label, String meaning) {
            this.label = label;
            this.meaning = meaning;
        }

        /** True when this rule moves the angles slowly enough for a perfection to be an event. */
        public boolean datable() {
            return this != QUOTIDIAN;
        }

        @Override
        public String toString() {
            return label;
        }

        /** The method for a stored label, defaulting to solar arc. */
        public static Method of(String label) {
            if (label != null) {
                String want = label.trim();
                for (Method m : values()) {
                    if (m.label.equalsIgnoreCase(want) || m.name().equalsIgnoreCase(want)) {
                        return m;
                    }
                }
            }
            return SOLAR_ARC;
        }
    }

    /** The rule in force, set from Settings at startup. David's call, 2026-09-15: solar arc. */
    public static Method method = Method.SOLAR_ARC;

    /**
     * Naibod's rate: the Sun's mean daily motion, 59 minutes 08 seconds of arc, taken as a year of
     * life. Named after Valentin Naibod, and quoted in the literature in exactly those minutes and
     * seconds rather than as a decimal, so it is written that way here.
     */
    public static final double NAIBOD_PER_YEAR = 59.0 / 60.0 + 8.0 / 3600.0;

    /** Years of life at a moment, on the same year length the progression itself uses. */
    public static double yearsOfLife(double natalJd, double targetJd) {
        return (targetJd - natalJd) / Progressions.DAYS_PER_YEAR;
    }

    /**
     * The arc the MC has advanced by a date, in degrees, or NaN for the quotidian rule, which has
     * no arc - its angles come from a moment rather than from a displacement.
     */
    public static double arc(SwissEph sw, ChartFrame natal, double natalJd, double targetJd,
                             Method m) {
        double years = yearsOfLife(natalJd, targetJd);
        if (m == Method.NAIBOD) {
            return years * NAIBOD_PER_YEAR;
        }
        if (m == Method.SOLAR_ARC) {
            double progressed = Almanac.bodyLongitude(
                sw, Progressions.progressedJd(natalJd, targetJd), "Sun");
            ChartFrame.Body sun = natal == null ? null : natal.body("Sun");
            if (sun == null || !sun.ok || Double.isNaN(progressed)) {
                return Double.NaN;
            }
            // Whole turns are added back: the Sun's arc is normalised into a circle, and a life
            // long enough to pass one would otherwise fold back to nothing.
            return Zodiac.normalise(progressed - sun.lon) + 360.0 * Math.floor(years / 365.2422);
        }
        return Double.NaN;
    }

    /**
     * The progressed angles at a date: {@code [asc, mc, dsc, ic, armc]}, or null when the chart has
     * no birth time to progress.
     *
     * <b>Null for a time-unknown chart, and that is the point of the field.</b> A chart cast for
     * noon because nobody knows the hour has an Ascendant that could be anything; progressing it
     * would turn a blank into a date.
     */
    public static double[] at(SwissEph sw, ChartFrame natal, double natalJd, double targetJd,
                              Method m) {
        if (natal == null || natal.timeUnknown) {
            return null;
        }
        if (m == Method.QUOTIDIAN) {
            ChartFrame p = ChartFrame.compute(sw, Progressions.progressedJd(natalJd, targetJd),
                natal.geoLat, natal.geoLon, natal.hsys, natal.topocentric, natal.geoAltM);
            return new double[] {p.asc, p.mc, p.dsc, p.ic, p.armc};
        }
        double a = arc(sw, natal, natalJd, targetJd, m);
        if (Double.isNaN(a)) {
            return null;
        }
        double mc = Zodiac.normalise(natal.mc + a);
        double[] frame = ChartFrame.frameFromMc(sw, mc, natal.geoLat, natal.trueObliquity,
            natal.ayanamsa, natal.hsys, new double[13]);
        if (Double.isNaN(frame[1])) {
            return null;
        }
        return new double[] {frame[1], frame[2], Zodiac.opposite(frame[1]),
            Zodiac.opposite(frame[2]), frame[0]};
    }

    /** The angles by the rule in force. */
    public static double[] at(SwissEph sw, ChartFrame natal, double natalJd, double targetJd) {
        return at(sw, natal, natalJd, targetJd, method);
    }

    /** The progressed longitude of one angle by name, or NaN. */
    public static double longitudeOf(SwissEph sw, ChartFrame natal, double natalJd, double targetJd,
                                     String angle, Method m) {
        double[] a = at(sw, natal, natalJd, targetJd, m);
        if (a == null) {
            return Double.NaN;
        }
        switch (angle) {
            case "Ascendant":  return a[0];
            case "MC":         return a[1];
            case "Descendant": return a[2];
            case "IC":         return a[3];
            default:           return Double.NaN;
        }
    }
}
