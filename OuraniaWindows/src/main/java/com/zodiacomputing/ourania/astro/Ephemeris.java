package com.zodiacomputing.ourania.astro;

import java.io.File;

/**
 * Where the Swiss Ephemeris data lives. One statement of it, for the whole app.
 *
 * <p><b>Was twenty-five copies of the same absolute string</b> - the two panels, {@link Bodies}
 * and thirteen check suites each carried their own
 * {@code private static final String EPHE_PATH = "C:/Users/daver/..."}. Moving the ephemeris,
 * or running this project on any other machine, meant finding and editing all of them, and a
 * missed one fails as "SwissEph file not found" and a silent fall back to Moshier rather than
 * as an error anybody would chase.
 *
 * <p><b>Overridable, which is the part that makes this a fix rather than a tidy-up.</b> The
 * default is still David's machine, so nothing changes for him; but a different checkout can
 * now say where its files are without editing source:
 *
 * <pre>
 *   -Dourania.ephe=D:/wherever/ephe        (system property, wins)
 *   set OURANIA_EPHE=D:/wherever/ephe      (environment variable)
 * </pre>
 *
 * <p><b>Do not put a path separator in it.</b> Swiss Ephemeris splits a path list on
 * {@code path.separator} - which is why a Windows drive colon used to cut every path in half
 * until {@code SwissData.PATH_SEPARATOR} was made platform-aware on 2026-08-27. One directory,
 * no lists; see {@code Resources/wiki/ourania-ephemeris-fallback.md}.
 *
 * <p>Note what is <i>not</i> here: the four numbered-asteroid files under {@code ast0},
 * {@code ast7} and {@code ast136} came from a source nobody recorded, and the planetary file
 * {@code sepl_18.se1} has never been present at all - every body falls back to Moshier. Both
 * are in the wiki note above.
 */
public final class Ephemeris {

    private Ephemeris() { }

    /** The default, and the only path this project has ever used. */
    private static final String DEFAULT = "C:/Users/daver/Desktop/Ourania/decoded_apk/assets";

    /** System property that overrides the default. */
    public static final String PROPERTY = "ourania.ephe";

    /** Environment variable that overrides the default, when the property is unset. */
    public static final String ENV = "OURANIA_EPHE";

    /**
     * The ephemeris directory: system property, then environment variable, then the default.
     *
     * Resolved once at class load. The ephemeris path is set on a SwissEph instance at
     * construction and changing it later would leave already-built frames inconsistent with
     * later ones, so this is deliberately not re-read.
     */
    public static final String PATH = resolve();

    private static String resolve() {
        String p = System.getProperty(PROPERTY);
        if (p == null || p.trim().isEmpty()) {
            p = System.getenv(ENV);
        }
        if (p == null || p.trim().isEmpty()) {
            // Packaged, the files travel with the app, in an ephe folder beside Ourania.jar;
            // from classes it is still the one path this project has always used (AppPaths).
            String bundled = AppPaths.packagedEphemeris();
            return bundled != null ? bundled : DEFAULT;
        }
        return p.trim();
    }

    /** True when the directory is actually there. Nothing calls this to decide anything -
     *  it exists so a check or a startup log can say "the path is wrong" out loud rather
     *  than leaving Moshier to stand in silently. */
    public static boolean present() {
        File f = new File(PATH);
        return f.isDirectory();
    }

    /** Whether the path came from an override rather than the built-in default. */
    public static boolean overridden() {
        return !DEFAULT.equals(PATH);
    }

    // ------------------------------------------------------------------ zodiac

    /**
     * The zodiacs the app offers: tropical, and four sidereal ayanamsas. Master list D3.
     *
     * Label to Swiss Ephemeris sidereal mode, -1 for tropical. Lahiri is India's official
     * ayanamsa and the default for Vedic work; Fagan-Bradley is the Western sidereal standard;
     * Krishnamurti and Raman are the two other ayanamsas in common Jyotish use.
     */
    public static final String[][] ZODIACS = {
        {"Tropical", "-1"},
        {"Sidereal (Lahiri)", String.valueOf(de.thmac.swisseph.SweConst.SE_SIDM_LAHIRI)},
        {"Sidereal (Fagan-Bradley)", String.valueOf(de.thmac.swisseph.SweConst.SE_SIDM_FAGAN_BRADLEY)},
        {"Sidereal (Krishnamurti)", String.valueOf(de.thmac.swisseph.SweConst.SE_SIDM_KRISHNAMURTI)},
        {"Sidereal (Raman)", String.valueOf(de.thmac.swisseph.SweConst.SE_SIDM_RAMAN)},
    };

    /**
     * The sidereal mode in force, or -1 for tropical.
     *
     * <b>One switch, read at every ephemeris call</b> through {@link #flags}. Every longitude in
     * the app - the wheel, the chart frame, the almanac, transits, returns, progressions, the
     * transit search - comes from one of about twenty calls, and all of them have to agree:
     * a sidereal natal chart read against tropical transits is out by the precession between
     * birth and now, about a degree in seventy years, which moves exact dates by weeks for the
     * slow planets. A static rather than a settings read, because the hot paths make hundreds
     * of thousands of calls and settings are a file.
     *
     * Tropical by default, so nothing changes until a reader chooses otherwise, and every check
     * suite - which never sets it - runs tropical.
     */
    private static volatile int siderealMode = -1;

    /** Instances already told the current mode; see {@link #flags}. */
    private static final java.util.Map<de.thmac.swisseph.SwissEph, Integer> TOLD =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static int siderealMode() {
        return siderealMode;
    }

    public static boolean sidereal() {
        return siderealMode >= 0;
    }

    /** Sets the zodiac by its label from {@link #ZODIACS}; an unknown label means tropical. */
    public static void setZodiac(String label) {
        int mode = -1;
        for (String[] z : ZODIACS) {
            if (z[0].equals(label)) {
                mode = Integer.parseInt(z[1]);
            }
        }
        siderealMode = mode;
    }

    /** The label of the zodiac in force. */
    public static String zodiacLabel() {
        for (String[] z : ZODIACS) {
            if (Integer.parseInt(z[1]) == siderealMode) {
                return z[0];
            }
        }
        return ZODIACS[0][0];
    }

    /**
     * Ephemeris flags for a longitude call in the zodiac in force.
     *
     * <b>The instance is told the ayanamsa once per change, not per call.</b>
     * {@code swe_set_sid_mode} ends in {@code swi_force_app_pos_etc}, which throws away the
     * instance's cached positions; setting it on every call would defeat the cache the scans
     * depend on.
     *
     * Not for obliquity, nutation or equatorial coordinates: those are defined against the true
     * equinox, and the callers asking for them keep their tropical flags.
     */
    public static int flags(de.thmac.swisseph.SwissEph sw, int base) {
        int mode = siderealMode;
        if (mode < 0) {
            return base;
        }
        Integer told = TOLD.get(sw);
        if (told == null || told != mode) {
            sw.swe_set_sid_mode(mode);
            TOLD.put(sw, mode);
        }
        return base | de.thmac.swisseph.SweConst.SEFLG_SIDEREAL;
    }

    /**
     * Tropical longitude less sidereal longitude at a moment, in degrees; zero when tropical.
     *
     * <b>The ayanamsa plus the nutation in longitude, not the ayanamsa alone.</b>
     * {@code swe_get_ayanamsa_ut} is the mean figure, while a sidereal position from
     * {@code swe_calc_ut} has the nutation removed with it - so subtracting the bare ayanamsa
     * from a tropical position missed by up to 17 arcseconds (measured 0.0044 degrees on
     * 1982-08-10). This is the figure a conversion between the two needs.
     */
    public static double ayanamsa(de.thmac.swisseph.SwissEph sw, double jdUt) {
        if (siderealMode < 0) {
            return 0.0;
        }
        flags(sw, 0);
        double[] eclnut = new double[6];
        sw.swe_calc_ut(jdUt, de.thmac.swisseph.SweConst.SE_ECL_NUT,
            de.thmac.swisseph.SweConst.SEFLG_SWIEPH, eclnut, new StringBuffer());
        return sw.swe_get_ayanamsa_ut(jdUt) + eclnut[2];
    }
}
