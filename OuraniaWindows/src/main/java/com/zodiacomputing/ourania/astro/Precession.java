package com.zodiacomputing.ourania.astro;

/**
 * General precession in longitude: how far the equinox has moved since a date.
 *
 * <p><b>Why this is not the ayanamsa.</b> {@link Ephemeris#ayanamsa} answers "how far apart are the
 * tropical and sidereal zodiacs right now", and it returns zero in a tropical chart, which is the
 * app's default. A precessed return asks a different question - how much has the equinox moved
 * between <i>these two moments</i> - and that answer must be the same whichever zodiac the reader
 * has chosen, because it is a fact about the Earth rather than about a convention. The two agree on
 * the difference by construction: every ayanamsa is the same precession with its own epoch
 * constant, so the constant cancels when you subtract one date from another.
 *
 * <p><b>The model is the IAU 2006 expression for general precession in longitude</b>, Capitaine's,
 * as adopted by the IAU and used by Swiss Ephemeris for its own ayanamsas:
 *
 * <pre>
 *   p(T) = 5028.796195" T + 1.1054348" T^2 + ...   T in Julian centuries from J2000.0
 * </pre>
 *
 * The two terms carried here are good to a fraction of an arcsecond over the centuries a birth
 * chart lives in; the higher terms matter over millennia. That is about 50.3 arcseconds a year,
 * which is the number the technique is usually quoted by.
 */
public final class Precession {

    private Precession() { }

    /** Julian day of J2000.0. */
    public static final double J2000 = 2451545.0;

    /** The linear term, arcseconds per Julian century (IAU 2006). */
    public static final double P1 = 5028.796195;

    /** The quadratic term, arcseconds per Julian century squared. */
    public static final double P2 = 1.1054348;

    /** General precession in longitude accumulated since J2000.0, in degrees. Negative before it. */
    public static double sinceJ2000(double jd) {
        double t = (jd - J2000) / 36525.0;
        return (P1 * t + P2 * t * t) / 3600.0;
    }

    /**
     * How far the equinox moves between two moments, in degrees - positive when {@code to} is the
     * later one.
     *
     * A degree every 71.6 years, which is why a precessed solar return falls about twenty minutes
     * later for each year of age: the Sun covers a degree a day, so 50.3 arcseconds of it is about
     * twenty minutes of clock.
     */
    public static double between(double fromJd, double toJd) {
        return sinceJ2000(toJd) - sinceJ2000(fromJd);
    }

    /** Arcseconds a year at a moment, for a reading that wants to say the rate. */
    public static double rateArcsecPerYear(double jd) {
        double t = (jd - J2000) / 36525.0;
        return (P1 + 2.0 * P2 * t) / 100.0;
    }
}
