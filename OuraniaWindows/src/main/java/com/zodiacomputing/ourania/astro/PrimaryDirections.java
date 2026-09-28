package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * F9: primary directions - the chart turned by the diurnal rotation, a degree for a year.
 *
 * <p>The oldest of the timing techniques and the one traditional practitioners ask for. Where
 * transits move the sky over a fixed chart and progressions move the chart a day for a year,
 * primary directions turn the <b>whole chart on the earth's own axis</b>: the arc a promissor
 * must travel, measured on the celestial equator, to arrive where a significator stands.
 *
 * <h3>The method: proportional semi-arc</h3>
 *
 * <p>A point's place is not its longitude but <i>where it sits between the horizon and the
 * meridian</i> - its meridian distance as a fraction of its own semi-arc. To direct a promissor
 * to a significator is to ask what right ascension the promissor must reach to hold the same
 * fraction of <i>its</i> semi-arc. That difference, in degrees of right ascension, is the arc of
 * direction.
 *
 * <p>This is the Placidian or semi-arc method, which is what the tradition means by primary
 * directions. It is stated here rather than assumed because the alternatives - Regiomontanus and
 * Campanus poles - give different arcs for the same chart, and a reader comparing this against
 * other software needs to know which they are looking at.
 *
 * <h3>Two forms, reported separately</h3>
 *
 * <p><b>Mundane</b> directs a promissor as a body, carrying its own declination - the planet
 * itself arriving at the significator's place in the houses. <b>Zodiacal</b> directs to a degree
 * of the ecliptic taken without latitude, so the promissor is a point in the zodiac rather than
 * a body, and aspects to it can be directed as well.
 *
 * <p><b>They give different dates for the same pair and are never mixed.</b> David's decision,
 * 2026-09-27: report both, separately. Merging them into one list would be the pattern this
 * project treats as the worst kind of defect - one confident answer assembled out of two
 * different questions.
 *
 * <h3>The key is the reader's</h3>
 *
 * <p>The arc is in degrees; the key turns degrees into years, and the three in use disagree by
 * about a year over a lifetime. It is a setting with a default rather than a constant, the way
 * house systems and orbs already are - see {@link Key}.
 *
 * <h3>What this deliberately does not do</h3>
 *
 * <p><b>Converse directions</b> - the chart turned backwards - are not computed. They are a
 * second technique wearing the same arithmetic, they double every list, and whether they belong
 * in a reading is a judgement nobody has made here. Building half of them would be worse than
 * not building them. <b>Mundane aspects</b> (a promissor arriving at the square of a
 * significator's place in the houses, rather than at the place itself) are likewise left out:
 * the mundane list is conjunctions in mundo.
 */
public final class PrimaryDirections {

    private PrimaryDirections() { }

    /** How degrees of arc become years of life. */
    public enum Key {
        /**
         * One degree, one year. The oldest and simplest, and the one every textbook states
         * first. It runs ahead of the Sun's real motion by about 52 arcseconds a year, so by
         * sixty it is nearly a whole degree - ten to twelve months - ahead of Naibod.
         */
        PTOLEMY("Ptolemy", 1.0),
        /**
         * The Sun's mean daily motion, 360 degrees over a tropical year: 0&deg;59'08".
         * Naibod's key, and the one most traditional practice expects. The default.
         */
        NAIBOD("Naibod", 360.0 / SolarArc.DAYS_PER_YEAR),
        /**
         * The arc the Sun actually travelled in right ascension, day for a year, from this
         * chart's own birth moment. The only key that varies by chart, because the Sun moves
         * faster in northern winter than in northern summer. Costs an ephemeris walk.
         */
        PLACIDUS("Placidian", Double.NaN);

        /** What a reader is shown, and what the setting stores. */
        public final String label;
        /** Degrees of arc per year, or NaN for a key that has to be measured per chart. */
        public final double degreesPerYear;

        Key(String label, double degreesPerYear) {
            this.label = label;
            this.degreesPerYear = degreesPerYear;
        }

        /** The key of this name, or the default when the name is unknown. */
        public static Key named(String label) {
            for (Key k : values()) {
                if (k.label.equalsIgnoreCase(label)) {
                    return k;
                }
            }
            return NAIBOD;
        }
    }

    /** Which question the arc answers. */
    public enum Form {
        /** The promissor as a body, with its own declination, arriving in the houses. */
        MUNDANE,
        /** A degree of the ecliptic, taken without latitude. */
        ZODIACAL
    }

    /** The significators directed, in the order a reader meets them. */
    static final String[] SIGNIFICATORS = {"Ascendant", "Midheaven", "Sun", "Moon", "Part of Fortune"};

    /** The promissors, the seven classical bodies. */
    static final String[] PROMISSORS = {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn"};

    /** Aspects directed in the zodiacal form, by name and by degrees from the promissor. */
    static final String[] ASPECT_NAMES = {"conjunction", "sextile", "square", "trine", "opposition"};
    static final double[] ASPECT_ANGLES = {0.0, 60.0, 90.0, 120.0, 180.0};

    /** One direction: what arrives where, how far it had to travel, and when that falls. */
    public static final class Direction {
        public final String significator;
        public final String promissor;
        public final Form form;
        /** The aspect directed, for a zodiacal direction; "conjunction" in mundo. */
        public final String aspect;
        /** Degrees of right ascension the promissor must travel. Always positive. */
        public final double arc;
        /** Years of life that arc stands for, under the key in force. */
        public final double years;
        /** The moment it falls. */
        public final double jd;

        Direction(String significator, String promissor, Form form, String aspect,
                  double arc, double years, double jd) {
            this.significator = significator;
            this.promissor = promissor;
            this.form = form;
            this.aspect = aspect;
            this.arc = arc;
            this.years = years;
            this.jd = jd;
        }

        @Override
        public String toString() {
            return promissor + " " + aspect + " " + significator
                + (form == Form.MUNDANE ? " in mundo" : "")
                + String.format(" at %.2f years", years);
        }
    }

    // ------------------------------------------------------------------ the astronomy

    private static double rad(double deg) {
        return Math.toRadians(deg);
    }

    /**
     * Right ascension and declination of an ecliptic degree taken without latitude.
     *
     * The zodiacal form's promissors are degrees rather than bodies, so they have no latitude
     * of their own and this is the whole of their position.
     */
    public static double[] equatorial(double longitude, double obliquity) {
        double l = rad(Zodiac.normalise(longitude));
        double e = rad(obliquity);
        double ra = Math.toDegrees(Math.atan2(Math.sin(l) * Math.cos(e), Math.cos(l)));
        double dec = Math.toDegrees(Math.asin(Math.sin(l) * Math.sin(e)));
        return new double[] {Zodiac.normalise(ra), dec};
    }

    /**
     * Ascensional difference: how far a declination shifts rising from due east.
     *
     * NaN when the point is circumpolar at this latitude - it neither rises nor sets, so it has
     * no semi-arc and cannot be directed by this method. Returning NaN rather than clamping is
     * deliberate: a clamped value would produce a confident arc for a direction that does not
     * exist.
     */
    public static double ascensionalDifference(double declination, double geoLat) {
        double x = Math.tan(rad(geoLat)) * Math.tan(rad(declination));
        if (Math.abs(x) > 1.0) {
            return Double.NaN;
        }
        return Math.toDegrees(Math.asin(x));
    }

    /** Meridian distance, -180 to 180: east of the meridian is positive. */
    static double meridianDistance(double ra, double armc) {
        double d = Zodiac.normalise(ra - armc);
        return d > 180.0 ? d - 360.0 : d;
    }

    /**
     * The arc a promissor must travel to reach a significator's place.
     *
     * <p>Both points are given by right ascension and declination. The significator's meridian
     * distance as a fraction of its own semi-arc is its place; the promissor must reach the
     * right ascension that gives it the same fraction of <i>its</i> semi-arc, of the same kind -
     * a significator above the horizon is matched against the promissor's diurnal semi-arc.
     *
     * @return degrees of right ascension, 0 to 360, or NaN if either point is circumpolar here.
     */
    public static double arc(double raSig, double decSig, double raProm, double decProm,
                             double geoLat, double armc) {
        double adSig = ascensionalDifference(decSig, geoLat);
        double adProm = ascensionalDifference(decProm, geoLat);
        if (Double.isNaN(adSig) || Double.isNaN(adProm)) {
            return Double.NaN;
        }
        double mdSig = meridianDistance(raSig, armc);
        double diurnalSa = 90.0 + adSig;
        boolean aboveHorizon = Math.abs(mdSig) <= diurnalSa;
        double saSig = aboveHorizon ? diurnalSa : 90.0 - adSig;
        if (saSig == 0.0) {
            return Double.NaN;
        }
        // Below the horizon the meridian distance is measured from the IC, not the MC.
        double fromMeridian = aboveHorizon ? mdSig
            : (mdSig > 0 ? mdSig - 180.0 : mdSig + 180.0);
        double proportion = fromMeridian / saSig;

        double saProm = aboveHorizon ? 90.0 + adProm : 90.0 - adProm;
        double meridian = aboveHorizon ? armc : armc + 180.0;
        double needed = meridian + proportion * saProm;
        return Zodiac.normalise(needed - raProm);
    }

    // ------------------------------------------------------------------ the key

    /**
     * Years of life for an arc, under a key.
     *
     * The Placidus key is measured rather than divided: it walks the Sun's right ascension
     * forward a day at a time - a day for a year - until it has covered the arc, then
     * interpolates inside the last day.
     */
    public static double years(Key key, double arc, SwissEph sw, double natalJd) {
        if (Double.isNaN(arc)) {
            return Double.NaN;
        }
        if (key != Key.PLACIDUS) {
            return arc / key.degreesPerYear;
        }
        double start = sunRightAscension(sw, natalJd);
        if (Double.isNaN(start)) {
            return Double.NaN;
        }
        double travelled = 0.0;
        double previous = start;
        // One day per year of life; a lifetime of directions is a few tens of days of walking.
        for (int day = 1; day <= 400; day++) {
            double ra = sunRightAscension(sw, natalJd + day);
            if (Double.isNaN(ra)) {
                return Double.NaN;
            }
            double step = Zodiac.normalise(ra - previous);
            if (step > 180.0) {
                step -= 360.0;              // never, for the Sun, but a wrap must not become 359
            }
            if (travelled + step >= arc) {
                return (day - 1) + (arc - travelled) / step;
            }
            travelled += step;
            previous = ra;
        }
        return Double.NaN;
    }

    private static double sunRightAscension(SwissEph sw, double jd) {
        double[] xx = new double[6];
        StringBuffer serr = new StringBuffer();
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_EQUATORIAL;
        if (sw.swe_calc_ut(jd, SweConst.SE_SUN, flags, xx, serr) < 0) {
            return Double.NaN;
        }
        return Zodiac.normalise(xx[0]);
    }

    // ------------------------------------------------------------------ the directions

    /**
     * Every direction that falls inside a span of years, both forms, soonest first.
     *
     * @param maxYears how far into the life to look; arcs beyond it are not returned.
     */
    public static List<Direction> of(SwissEph sw, ChartFrame f, double natalJd,
                                     double maxYears, Key key) {
        List<Direction> out = new ArrayList<>();
        if (f == null || Double.isNaN(f.armc)) {
            return out;
        }
        double lat = f.geoLat;
        double obliquity = f.trueObliquity;

        for (String sigName : SIGNIFICATORS) {
            double[] sig = pointOf(f, sigName, obliquity);
            if (sig == null) {
                continue;
            }
            for (String promName : PROMISSORS) {
                ChartFrame.Body body = f.body(promName);
                if (body == null || !body.ok) {
                    continue;
                }
                // <b>A body is not directed to itself.</b> The Sun conjunct the Sun in mundo is
                // an arc of zero - the birth moment restated as an event - and the Sun to its
                // own square is a body arriving at a degree of its own, which no source this
                // project holds reads as a direction. Both fell out of the first run as
                // directions at age zero, which is how they were noticed.
                if (promName.equals(sigName)) {
                    continue;
                }
                // Mundane: the body itself, carrying its own declination.
                add(out, sw, natalJd, maxYears, key, sigName, promName, Form.MUNDANE,
                    "conjunction", arc(sig[0], sig[1], body.ra, body.dec, lat, f.armc));

                // Zodiacal: degrees on the ecliptic, the promissor's own and its aspects.
                for (int i = 0; i < ASPECT_ANGLES.length; i++) {
                    double degree = Zodiac.normalise(body.lon + ASPECT_ANGLES[i]);
                    double[] eq = equatorial(degree, obliquity);
                    add(out, sw, natalJd, maxYears, key, sigName, promName, Form.ZODIACAL,
                        ASPECT_NAMES[i], arc(sig[0], sig[1], eq[0], eq[1], lat, f.armc));
                    if (ASPECT_ANGLES[i] != 0.0 && ASPECT_ANGLES[i] != 180.0) {
                        // The other side of the promissor: a square is two degrees, not one.
                        double other = Zodiac.normalise(body.lon - ASPECT_ANGLES[i]);
                        double[] eq2 = equatorial(other, obliquity);
                        add(out, sw, natalJd, maxYears, key, sigName, promName, Form.ZODIACAL,
                            ASPECT_NAMES[i], arc(sig[0], sig[1], eq2[0], eq2[1], lat, f.armc));
                    }
                }
            }
        }
        out.sort((a, b) -> Double.compare(a.years, b.years));
        return out;
    }

    private static void add(List<Direction> out, SwissEph sw, double natalJd, double maxYears,
                            Key key, String sig, String prom, Form form, String aspect,
                            double arc) {
        if (Double.isNaN(arc)) {
            return;
        }
        double yrs = years(key, arc, sw, natalJd);
        if (Double.isNaN(yrs) || yrs < 0.0 || yrs > maxYears) {
            return;
        }
        out.add(new Direction(sig, prom, form, aspect, arc, yrs,
            natalJd + yrs * SolarArc.DAYS_PER_YEAR));
    }

    /**
     * A significator's right ascension and declination.
     *
     * The angles are ecliptic degrees taken without latitude; the bodies carry their own, which
     * the frame has already computed.
     */
    static double[] pointOf(ChartFrame f, String name, double obliquity) {
        switch (name) {
            case "Ascendant":
                return equatorial(f.asc, obliquity);
            case "Midheaven":
                // By definition the MC's right ascension IS the RAMC. Taking it from the
                // ecliptic conversion instead would agree to rounding and hide any error in
                // the frame's own armc, which several other techniques depend on.
                return new double[] {Zodiac.normalise(f.armc), equatorial(f.mc, obliquity)[1]};
            case "Part of Fortune":
                return equatorial(f.lotOfFortune, obliquity);
            default:
                ChartFrame.Body b = f.body(name);
                return b == null || !b.ok ? null : new double[] {b.ra, b.dec};
        }
    }
}
