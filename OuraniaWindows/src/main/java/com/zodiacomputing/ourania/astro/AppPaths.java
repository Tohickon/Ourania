package com.zodiacomputing.ourania.astro;

import java.io.File;

/**
 * Where the app finds its data, and where it keeps the reader's own files. One statement of it,
 * for the source tree and for a packaged install alike (J4, J5).
 *
 * <p><b>Two ways the app runs, told apart by what is running, not by where.</b>
 * <ul>
 *   <li><b>From classes</b> - {@code src\main\java} through Run_Ourania.bat, or a suite built
 *       by build.ps1. Everything is relative to the working directory, exactly as it always was:
 *       prose under {@code src/main/resources/data/}, settings and chart book beside it.</li>
 *   <li><b>From Ourania.jar</b> - the packaged app. Prose is in a {@code data} folder beside the
 *       jar, the ephemeris in an {@code ephe} folder beside it, and the reader's settings and
 *       chart book in {@code %APPDATA%\Ourania}, because an installed app starts from Program
 *       Files, which a normal user cannot write to.</li>
 * </ul>
 * Guessing from the working directory was the alternative, and it would have moved the reader's
 * files whenever the app was started from anywhere else - SettingsIsolationCheck's child JVM
 * among them.
 *
 * <p>Each can be overridden without editing source: {@code -Dourania.data=},
 * {@code -Dourania.home=}, or the environment variables {@code OURANIA_DATA} and
 * {@code OURANIA_HOME}. The ephemeris keeps its own, in {@link Ephemeris}.
 */
public final class AppPaths {

    private AppPaths() { }

    public static final String DATA_PROPERTY = "ourania.data";
    public static final String DATA_ENV = "OURANIA_DATA";
    public static final String HOME_PROPERTY = "ourania.home";
    public static final String HOME_ENV = "OURANIA_HOME";

    /** The folder holding Ourania.jar, or null when running from classes. */
    public static File jarDir() {
        try {
            File where = new File(AppPaths.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI());
            return where.isFile() && where.getName().toLowerCase().endsWith(".jar")
                ? where.getParentFile() : null;
        } catch (Exception | Error ex) {
            return null;
        }
    }

    /** True when running from Ourania.jar rather than from classes. */
    public static boolean packaged() {
        return jarDir() != null;
    }

    /** The prose, atlas and other data files, as a path ending in a slash. */
    public static String dataDir() {
        String o = override(DATA_PROPERTY, DATA_ENV);
        if (o != null) {
            return slash(o);
        }
        File jar = jarDir();
        return jar == null ? "src/main/resources/data/" : slash(new File(jar, "data").getPath());
    }

    /**
     * Where the reader's settings and chart book live, as a prefix: empty from classes (the
     * working directory, as always), {@code %APPDATA%\Ourania\} when packaged. Created on first
     * use so the first save does not fail.
     */
    public static String userDir() {
        String cached = userDir;
        if (cached != null) {
            return cached;
        }
        String o = override(HOME_PROPERTY, HOME_ENV);
        boolean chosen = o != null;
        if (o == null) {
            if (!packaged()) {
                return userDir = "";
            }
            String appData = System.getenv("APPDATA");
            o = new File(appData != null && !appData.isEmpty() ? appData
                : System.getProperty("user.home"), "Ourania").getPath();
        }
        File dir = new File(o);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        if (!chosen) {
            importOnce(dir);
        }
        return userDir = slash(dir.getPath());
    }

    /** Asked for on every settings read, so worked out once per run. */
    private static volatile String userDir;

    /** The files a reader's own state lives in, all carried across together. */
    private static final String[] READER_FILES =
        {"settings.properties", "saved_charts.properties", "saved_transits.properties"};

    /** Left in the reader's folder once the copy has been made, so it is never made again. */
    static final String IMPORTED_MARKER = ".imported-from-source-tree";

    /**
     * <b>The first packaged launch brings the reader's settings and chart book across, once.</b>
     *
     * Moving the files to %APPDATA% would otherwise have greeted David's first launch of the jar
     * with a fresh install: no natal chart, no saved charts, every preference back at its
     * default. build.ps1 -Jar writes {@code import-from.txt} beside the jar naming the source
     * tree those files live in (and does not on a CI runner, so a downloaded app starts fresh).
     *
     * <b>Copy, never move, and never overwrite.</b> Nothing happens if the folder already holds
     * settings, and the marker stops it happening a second time - so a reader who later deletes
     * their settings to start over gets a fresh start, not the old file back. The source tree's
     * files are left exactly where they are, for running from source.
     */
    static void importOnce(File dir) {
        try {
            File marker = new File(dir, IMPORTED_MARKER);
            if (marker.exists() || new File(dir, READER_FILES[0]).exists()) {
                return;
            }
            File jar = jarDir();
            File pointer = jar == null ? null : new File(jar, "import-from.txt");
            if (pointer == null || !pointer.isFile()) {
                return;
            }
            String from = new String(java.nio.file.Files.readAllBytes(pointer.toPath()),
                java.nio.charset.StandardCharsets.UTF_8).trim();
            File source = new File(from);
            if (!new File(source, READER_FILES[0]).isFile()) {
                return;
            }
            StringBuilder copied = new StringBuilder();
            for (String name : READER_FILES) {
                File f = new File(source, name);
                if (f.isFile()) {
                    java.nio.file.Files.copy(f.toPath(), new File(dir, name).toPath());
                    copied.append(name).append('\n');
                }
            }
            java.nio.file.Files.write(marker.toPath(), ("Copied once from " + source.getPath()
                + " on " + java.time.LocalDate.now() + ":\n" + copied)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) {
            // A failed copy leaves a fresh install, which is what there would have been anyway.
            System.err.println("Could not bring settings across: " + ex);
        }
    }

    /** The ephemeris folder beside the jar, or null when running from classes. */
    public static String packagedEphemeris() {
        File jar = jarDir();
        return jar == null ? null : new File(jar, "ephe").getPath().replace('\\', '/');
    }

    private static String override(String property, String env) {
        String v = System.getProperty(property);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(env);
        }
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }

    private static String slash(String p) {
        p = p.replace('\\', '/');
        return p.endsWith("/") ? p : p + "/";
    }
}
