package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * settings.properties, in one place.
 *
 * Three separate copies of "load the file, change one key, store it back" existed before
 * this: ChartSetupPanel had a private writeSettings, SkymapPanel open-coded the same
 * sequence to save the house system, and SkymapPanel's constructor open-coded the read.
 * They had already diverged in the way that matters - one of them stored with a comment
 * header and one without, and only one of them tolerated a missing file - and a fourth
 * copy for the body selection would have been the point at which a key started getting
 * clobbered by whichever screen wrote last.
 *
 * Every mutation here is read-modify-write, so writing one key never drops another.
 */
public final class Settings {

    /** Beside the working directory, which is where the app is launched from. */
    private static final String FILE = "settings.properties";

    /** Comma-separated body ids; see {@link Bodies#parse}. */
    public static final String BODIES_KEY = "bodies.enabled";

    public static final String NODE_VARIANT_KEY = "node.variant";
    public static final String LILITH_VARIANT_KEY = "lilith.variant";

    private Settings() { }

    /** Everything currently on disk, or an empty set if there is no file yet. */
    public static Properties load() {
        Properties p = new Properties();
        File f = new File(FILE);
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                p.load(fis);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return p;
    }

    public static String get(String key, String fallback) {
        return load().getProperty(key, fallback);
    }

    /** Read, apply the mutation, write back. Other keys are preserved. */
    public static void update(Consumer<Properties> mutation) {
        try {
            Properties p = load();
            mutation.accept(p);
            try (FileOutputStream fos = new FileOutputStream(FILE)) {
                p.store(fos, "Ourania Settings");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void set(String key, String value) {
        update(p -> p.setProperty(key, value));
    }

    // ------------------------------------------------------------------ body selection

    /**
     * Which points the chart shows. Defaults when the key has never been written, so a
     * fresh install opens on the classical chart rather than on nothing at all.
     *
     * getProperty returns null for an absent key and "" for a present but empty one, and
     * Bodies.parse reads those as two different answers - the defaults, and nothing
     * selected. Do not "tidy" this by defaulting the string: that is what made Select none
     * silently revert at the next launch.
     */
    public static boolean[] loadBodySelection() {
        return Bodies.parse(load().getProperty(BODIES_KEY));
    }

    public static void saveBodySelection(boolean[] enabled) {
        set(BODIES_KEY, Bodies.format(enabled));
    }

    // ------------------------------------------------------------------ variants

    public static boolean useTrueNode() {
        return "true".equals(get(NODE_VARIANT_KEY, "true"));
    }

    public static boolean useTrueLilith() {
        return "true".equals(get(LILITH_VARIANT_KEY, "false"));
    }

    public static void setTrueNode(boolean useTrue) {
        set(NODE_VARIANT_KEY, useTrue ? "true" : "mean");
    }

    public static void setTrueLilith(boolean useTrue) {
        set(LILITH_VARIANT_KEY, useTrue ? "true" : "mean");
    }
}
