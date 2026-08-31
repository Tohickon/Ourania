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
            return DEFAULT;
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
}
