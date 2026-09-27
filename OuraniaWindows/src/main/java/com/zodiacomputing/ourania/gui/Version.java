package com.zodiacomputing.ourania.gui;

/**
 * What version this is, in one place (J6).
 *
 * <p><b>Swept for on 27 Sep: there was no version string anywhere in the tree.</b> No tags, no
 * changelog, and nothing in the app a bug report could name. That is fine while one person runs it
 * from a source tree and stops being fine the moment an installer or an APK exists - M8 cannot have a
 * store listing without it.
 *
 * <p><b>The number lives in a file, not in this class.</b> {@code OuraniaWindows/VERSION} is the one
 * place it is written, and the build stamps it into
 * {@code ourania-version.properties} beside the jar along with the commit it was built from. A
 * constant here would be a second copy of the version and would drift from the tag the way four
 * lists of house systems drifted from each other.
 *
 * <p><b>Running from classes there is no stamp, and this says so rather than inventing one.</b>
 * {@code "development build"} is the honest answer for a tree that was compiled in place, and it is
 * what the window title shows. A made-up number in a bug report is worse than no number, because it
 * looks like information.
 */
final class Version {

    private Version() { }

    /** The resource the build writes beside the jar. Absent when running from classes. */
    static final String RESOURCE = "/ourania-version.properties";

    /** What the app calls itself when there is no stamp to read. */
    static final String DEVELOPMENT = "development build";

    /** The version, or {@link #DEVELOPMENT}. */
    static String number() {
        return read("version", DEVELOPMENT);
    }

    /** The commit it was built from, or empty. */
    static String commit() {
        return read("commit", "");
    }

    /**
     * What the title bar and a bug report should say.
     *
     * The commit is included when there is one, because two builds of the same version are a real
     * situation and the commit is the only thing that separates them.
     */
    static String display() {
        String v = number();
        String c = commit();
        return c.isEmpty() ? v : v + " (" + c + ")";
    }

    /** Read once: the resource cannot change while the app runs. */
    private static volatile java.util.Properties stamp;

    private static String read(String key, String fallback) {
        java.util.Properties p = stamp;
        if (p == null) {
            p = new java.util.Properties();
            try (java.io.InputStream in = Version.class.getResourceAsStream(RESOURCE)) {
                if (in != null) {
                    p.load(in);
                }
            } catch (Exception unreadable) {
                // An unreadable stamp is a build that did not finish writing it; the app still runs
                // and still says something true about itself.
            }
            stamp = p;
        }
        String v = p.getProperty(key, "").trim();
        return v.isEmpty() ? fallback : v;
    }

    /** Drops the cached stamp. For the suite. */
    static void forget() {
        stamp = null;
    }
}
