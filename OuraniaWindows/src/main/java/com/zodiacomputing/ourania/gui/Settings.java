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

    // ------------------------------------------------------------------ chart rendering

    /** Settings key for the bead behind each glyph. */
    public static final String SPHERES_KEY = "chart.spheres";

    /**
     * Whether each body is drawn on a metallic bead.
     *
     * <b>On by default</b>, because that is what the app has always drawn and a rendering
     * change should not arrive unasked. Off gives the classical look: a bare glyph on the ring
     * with the degree ticks and house lines visible behind it.
     */
    public static boolean showPlanetSpheres() {
        return !"false".equals(get(SPHERES_KEY, "true"));
    }

    public static void setShowPlanetSpheres(boolean on) {
        set(SPHERES_KEY, on ? "true" : "false");
    }

    /** Settings key for the line from a body to its exact degree. */
    public static final String DEGREE_LINE_KEY = "chart.degreeLines";

    /**
     * Whether a body is joined to its exact degree on the ring by a leader line.
     *
     * <b>On by default - it has always been drawn</b>, faintly, at alpha 30. Bodies are spread
     * outward when they crowd, so the glyph is often not at the degree it names; the leader is
     * what says where it actually is. Worth being able to turn off all the same: on a busy
     * chart it is twenty more lines.
     */
    public static boolean showDegreeLines() {
        return !"false".equals(get(DEGREE_LINE_KEY, "true"));
    }

    public static void setShowDegreeLines(boolean on) {
        set(DEGREE_LINE_KEY, on ? "true" : "false");
    }

    /** Settings key for where the bodies sit. */
    public static final String BODY_RING_KEY = "chart.bodyRing";

    /** Bodies just inside the sign ring - where this app has always drawn them. */
    public static final String RING_DEFAULT = "Inside the sign ring";
    /** Bodies pulled into the middle, leaving the rings clear. */
    public static final String RING_CENTRE = "In the centre";
    /** Bodies pushed outside the sign ring, the way many traditional charts print them. */
    public static final String RING_OUTSIDE = "Outside the sign ring";

    public static final String[] BODY_RINGS = {RING_DEFAULT, RING_CENTRE, RING_OUTSIDE};

    public static String bodyRing() {
        String v = get(BODY_RING_KEY, RING_DEFAULT);
        for (String r : BODY_RINGS) {
            if (r.equals(v)) {
                return r;
            }
        }
        return RING_DEFAULT;
    }

    public static void setBodyRing(String ring) {
        set(BODY_RING_KEY, ring);
    }

    // ------------------------------------------------------------------ aspect selection

    /** Settings key for which aspects are drawn. */
    public static final String ASPECTS_KEY = "aspects.enabled";

    /**
     * Which aspects the wheel and the grid show.
     *
     * <b>Absent means all of them</b>, the same distinction {@link #loadBodySelection} draws:
     * getProperty returns null for a key never written and "" for one written empty, and those
     * are two different answers - a fresh install shows every aspect, while a user who
     * unticked every box gets what they asked for. Defaulting the string would make "show
     * none" silently revert on the next launch, which is the defect the body selection
     * already shipped once.
     */
    public static boolean[] loadAspectSelection() {
        String csv = load().getProperty(ASPECTS_KEY);
        boolean[] on = new boolean[com.zodiacomputing.ourania.astro.Aspects.Type.values().length];
        if (csv == null) {
            java.util.Arrays.fill(on, true);
            return on;
        }
        for (String part : csv.split(",")) {
            String want = part.trim();
            for (com.zodiacomputing.ourania.astro.Aspects.Type t
                    : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
                if (t.label.equalsIgnoreCase(want)) {
                    on[t.ordinal()] = true;
                }
            }
        }
        return on;
    }

    public static void saveAspectSelection(boolean[] enabled) {
        StringBuilder sb = new StringBuilder();
        for (com.zodiacomputing.ourania.astro.Aspects.Type t
                : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            if (t.ordinal() < enabled.length && enabled[t.ordinal()]) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(t.label);
            }
        }
        set(ASPECTS_KEY, sb.toString());
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
