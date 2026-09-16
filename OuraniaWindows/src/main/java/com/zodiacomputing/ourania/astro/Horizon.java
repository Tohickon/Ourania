package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * The sky as it stands over a place: each planet's altitude above the horizon and its compass
 * direction, and the ecliptic with the chart's angles on it.
 *
 * <p>Master list E: "'Grid Skymap' menu entry shows the 2D wheel - a real horizon / celestial-sphere
 * view does not exist." The wheel is the chart - the ecliptic laid flat and turned so the
 * Ascendant is on the left. This is the sky the chart was cast from: where you would look, and how
 * high.
 *
 * <p><b>Positions are equatorial, not ecliptic, going in.</b> Swiss Ephemeris turns right ascension
 * and declination into azimuth and altitude directly, and those do not depend on the zodiac in
 * force - a sidereal chart's Mars is in the same place in the sky as a tropical chart's. The
 * ecliptic line and the angles are placed from tropical longitudes computed here for the same
 * reason.
 *
 * <p><b>Azimuth is a compass bearing</b>: north 0, east 90, south 180, west 270. Swiss Ephemeris
 * measures from the south, clockwise through the west, and that is converted once, below.
 */
public final class Horizon {

    private Horizon() { }

    /** Standard air for the refraction of the apparent altitude: sea level, 10 C. */
    static final double PRESSURE = 1013.25;
    static final double TEMPERATURE = 10.0;

    /** One thing in the sky. */
    public static final class Place {
        public final String name;
        /** Compass bearing, 0 north, 90 east. */
        public final double azimuth;
        /** Geometric altitude, degrees; negative below the horizon. */
        public final double altitude;
        /** Altitude as seen, lifted by refraction near the horizon. */
        public final double apparent;
        /** True when the altitude is climbing: rising or before its culmination. */
        public final boolean climbing;

        Place(String name, double azimuth, double altitude, double apparent, boolean climbing) {
            this.name = name;
            this.azimuth = azimuth;
            this.altitude = altitude;
            this.apparent = apparent;
            this.climbing = climbing;
        }

        /** Above the horizon as seen, refraction included. */
        public boolean visible() {
            return this.apparent > 0.0;
        }
    }

    /** Swiss Ephemeris azimuth (from south, through west) as a compass bearing. */
    public static double compass(double sweAzimuth) {
        return Zodiac.normalise(sweAzimuth + 180.0);
    }

    private static final String[] WINDS = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

    /** The sixteen-point compass name for a bearing. */
    public static String wind(double azimuth) {
        return WINDS[(int) Math.floor(Zodiac.normalise(azimuth + 11.25) / 22.5) % 16];
    }

    /** Azimuth and altitude, [compass, true, apparent], for a right ascension and declination. */
    static double[] fromEquatorial(SwissEph sw, double jdUt, double lat, double lon, double ra, double dec) {
        double[] xaz = new double[3];
        sw.swe_azalt(jdUt, SweConst.SE_EQU2HOR, new double[] {lon, lat, 0.0}, PRESSURE, TEMPERATURE,
            new double[] {ra, dec, 1.0}, xaz);
        return new double[] {compass(xaz[0]), xaz[1], xaz[2]};
    }

    /** Azimuth and altitude, [compass, true, apparent], for a tropical ecliptic longitude on the ecliptic. */
    public static double[] fromEcliptic(SwissEph sw, double jdUt, double lat, double lon, double tropicalLon) {
        double[] xaz = new double[3];
        sw.swe_azalt(jdUt, SweConst.SE_ECL2HOR, new double[] {lon, lat, 0.0}, PRESSURE, TEMPERATURE,
            new double[] {tropicalLon, 0.0, 1.0}, xaz);
        return new double[] {compass(xaz[0]), xaz[1], xaz[2]};
    }

    /** A body's right ascension and declination of date, or null when it will not compute. */
    static double[] equatorial(SwissEph sw, double jdUt, int ipl) {
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        int rc = sw.swe_calc_ut(jdUt, ipl, SweConst.SEFLG_SWIEPH | SweConst.SEFLG_EQUATORIAL, xx, err);
        return rc < 0 ? null : new double[] {xx[0], xx[1]};
    }

    /**
     * The chosen points in the sky over a place, using the reader's body selection.
     *
     * <b>Every selected point the ephemeris can place, not only the planets.</b> It drew the ten
     * planets alone and ignored the selection, so a reader who had switched Ceres or Eros on saw
     * them on the wheel and not in the sky - David, 2026-09-15: "it only shows the planets and not
     * the asteroids when they are selected". The asteroids, the centaurs and the nodes are real
     * bodies at real places in the sky; there is no reason the horizon should know less about them
     * than the wheel does.
     *
     * The points that are <i>not</i> here are the ones with no place of their own: the lots, the
     * Vertex and the East Point are derived from a chart's angles rather than from the sky, and the
     * four angles come from {@link #angleplaces} because they are where the ecliptic meets the
     * horizon and meridian. The South Node is included, as the degree opposite the North Node.
     */
    public static List<Place> bodies(SwissEph sw, double jdUt, double lat, double lon) {
        return bodies(sw, jdUt, lat, lon, com.zodiacomputing.ourania.gui.Settings.loadBodySelection());
    }

    /** As {@link #bodies}, with the selection given rather than read - for a check. */
    public static List<Place> bodies(SwissEph sw, double jdUt, double lat, double lon,
                                     boolean[] selected) {
        List<Place> out = new ArrayList<>();
        for (int i = 0; i < Bodies.count(); i++) {
            if (selected != null && i < selected.length && !selected[i]) {
                continue;
            }
            Bodies.Def d = Bodies.at(i);
            Place p = null;
            if (d.source == Bodies.Source.EPHEMERIS) {
                p = place(sw, jdUt, lat, lon, d.name, d.getIpl());
            } else if (d.source == Bodies.Source.SOUTH_NODE) {
                p = onEcliptic(sw, jdUt, lat, lon, d.name,
                    Almanac.bodyLongitude(sw, jdUt, "North Node") + 180.0);
            }
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** A point that sits on the ecliptic by definition, with whether it is climbing. */
    static Place onEcliptic(SwissEph sw, double jdUt, double lat, double lon, String name,
                            double tropicalLon) {
        if (Double.isNaN(tropicalLon)) {
            return null;
        }
        double[] now = fromEcliptic(sw, jdUt, lat, lon, Zodiac.normalise(tropicalLon));
        double[] then = fromEcliptic(sw, jdUt + 10.0 / 1440.0, lat, lon, Zodiac.normalise(tropicalLon));
        return new Place(name, now[0], now[1], now[2], then[1] > now[1]);
    }

    /** One body in the sky, with whether it is climbing ten minutes on. */
    public static Place place(SwissEph sw, double jdUt, double lat, double lon, String name, int ipl) {
        double[] eq = equatorial(sw, jdUt, ipl);
        if (eq == null) {
            return null;
        }
        double[] now = fromEquatorial(sw, jdUt, lat, lon, eq[0], eq[1]);
        double later = jdUt + 10.0 / 1440.0;
        double[] eqLater = equatorial(sw, later, ipl);
        double[] then = eqLater == null ? now : fromEquatorial(sw, later, lat, lon, eqLater[0], eqLater[1]);
        return new Place(name, now[0], now[1], now[2], then[1] > now[1]);
    }

    /** The tropical Ascendant and Midheaven for a moment and place: [asc, mc]. */
    public static double[] angles(SwissEph sw, double jdUt, double lat, double lon) {
        double[] cusps = new double[13];
        double[] ascmc = new double[10];
        sw.swe_houses(jdUt, 0, lat, lon, 'P', cusps, ascmc);
        return new double[] {ascmc[0], ascmc[1]};
    }

    /** The chart's four angles as points on the ecliptic in the sky: Ascendant, MC, Descendant, IC. */
    public static List<Place> angleplaces(SwissEph sw, double jdUt, double lat, double lon) {
        double[] a = angles(sw, jdUt, lat, lon);
        List<Place> out = new ArrayList<>();
        String[] names = {"Ascendant", "MC", "Descendant", "IC"};
        double[] lons = {a[0], a[1], a[0] + 180.0, a[1] + 180.0};
        for (int k = 0; k < 4; k++) {
            double[] h = fromEcliptic(sw, jdUt, lat, lon, Zodiac.normalise(lons[k]));
            out.add(new Place(names[k], h[0], h[1], h[2], false));
        }
        return out;
    }

    /** The ecliptic as a ring of points, one every step degrees of tropical longitude: [compass, true altitude]. */
    public static List<double[]> ecliptic(SwissEph sw, double jdUt, double lat, double lon, double step) {
        List<double[]> out = new ArrayList<>();
        for (double l = 0; l < 360.0; l += step) {
            double[] h = fromEcliptic(sw, jdUt, lat, lon, l);
            out.add(new double[] {h[0], h[1], l});
        }
        return out;
    }
}
