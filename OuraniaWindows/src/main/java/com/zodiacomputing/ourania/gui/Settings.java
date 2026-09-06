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

    /** The schema this build writes. Bump it when a key changes meaning, not when one is added. */
    public static final int SCHEMA = 1;

    /** Key holding the schema a file was written by. Absent means "before this existed". */
    public static final String SCHEMA_KEY = "settings.version";

    /**
     * True when the last {@link #load} could not read the file that was there.
     *
     * <b>An unreadable settings file used to be indistinguishable from a fresh install.</b>
     * The read threw, the exception went to the console, an empty Properties came back, and
     * every preference silently reverted to its default - then the next save wrote that empty
     * set over the damaged file, so whatever was recoverable went with it. This is what lets
     * the write path tell the two apart.
     */
    private static volatile boolean lastLoadFailed;

    /** Everything currently on disk, or an empty set if there is no file yet. */
    public static Properties load() {
        Properties p = new Properties();
        File f = new File(FILE);
        lastLoadFailed = false;
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                p.load(fis);
            } catch (Exception ex) {
                lastLoadFailed = true;
                ex.printStackTrace();
            }
        }
        return p;
    }

    /** True when the file on disk exists but could not be read. */
    public static boolean loadFailed() {
        return lastLoadFailed;
    }

    /**
     * The schema a stored file claims, or 0 for one written before versioning existed.
     *
     * Nothing migrates on 0 today - every key this build reads has the same meaning it always
     * had - but a file can now say which build wrote it, which is the thing a migration will
     * need and cannot be added retroactively.
     */
    public static int storedSchema() {
        try {
            return Integer.parseInt(load().getProperty(SCHEMA_KEY, "0").trim());
        } catch (NumberFormatException bad) {
            return 0;
        }
    }

    public static String get(String key, String fallback) {
        return load().getProperty(key, fallback);
    }

    /**
     * Read, apply the mutation, write back. Other keys are preserved.
     *
     * <b>A file that would not parse is set aside, never overwritten.</b> Before this, a
     * corrupt settings file was read as empty and then replaced by the next save - so the one
     * artefact that could have said what went wrong was destroyed by the act of carrying on.
     * It is renamed with a {@code .corrupt} suffix instead, which costs nothing and leaves
     * something to look at.
     */
    public static void update(Consumer<Properties> mutation) {
        try {
            Properties p = load();
            if (lastLoadFailed) {
                preserveCorrupt();
            }
            mutation.accept(p);
            p.setProperty(SCHEMA_KEY, String.valueOf(SCHEMA));
            try (FileOutputStream fos = new FileOutputStream(FILE)) {
                p.store(fos, "Ourania Settings");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Moves an unreadable settings file aside so the next write does not destroy it. */
    private static void preserveCorrupt() {
        File f = new File(FILE);
        if (!f.exists()) {
            return;
        }
        File aside = new File(FILE + ".corrupt");
        for (int i = 2; aside.exists() && i < 100; i++) {
            aside = new File(FILE + ".corrupt." + i);
        }
        if (f.renameTo(aside)) {
            System.out.println("Settings file could not be read; kept as " + aside.getName());
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

    /** Settings key for whether rings animate when they open and fold. */
    public static final String ANIMATE_RINGS_KEY = "chart.animateRings";

    /**
     * Whether a ring unfurls when it opens, or simply appears.
     *
     * <b>On by default, and genuinely optional.</b> Swing exposes no equivalent of the web's
     * {@code prefers-reduced-motion}, so the only honest way to respect a reader who does not
     * want motion is to ask. Off makes every bloom instantaneous - the rings still open and
     * fold, they just do it in one frame.
     */
    public static boolean animateRings() {
        return !"false".equals(get(ANIMATE_RINGS_KEY, "true"));
    }

    public static void setAnimateRings(boolean on) {
        set(ANIMATE_RINGS_KEY, on ? "true" : "false");
    }

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

    // ------------------------------------------------------------------- marker shapes

    /**
     * The bead shapes a body can be drawn on.
     *
     * <b>Shape is what tells the rings apart; colour could not do it alone.</b> A synastry
     * tri-wheel draws three sets of bodies at once - this chart, the other person's, and the
     * sky - and two of the three were cubes separated only by a tint. That distinction is the
     * first thing lost to a small window, a projector or a printout, and it asks the reader to
     * compare two shades on opposite sides of the wheel to decide whose Mars they are looking
     * at. A sphere, a pyramid and a cube are told apart at a glance and survive all three.
     */
    public static final String MARKER_SPHERE = "Sphere";
    public static final String MARKER_CUBE = "Cube";
    public static final String MARKER_PYRAMID = "Pyramid";
    /** No bead: the bare glyph, with the ring lines and ticks visible behind it. */
    public static final String MARKER_NONE = "Bare glyph";

    public static final String[] MARKER_SHAPES =
        {MARKER_SPHERE, MARKER_CUBE, MARKER_PYRAMID, MARKER_NONE};

    public static final String MARKER_NATAL_KEY = "chart.marker.natal";
    public static final String MARKER_SYNASTRY_KEY = "chart.marker.synastry";
    public static final String MARKER_TRANSIT_KEY = "chart.marker.transit";

    /**
     * A stored shape name, or the fallback when it is not one this app draws.
     *
     * <b>Split from the getters so it can be checked without touching the file.</b> The
     * settings file is edited by hand often enough that a typo reaching the wheel as "draw
     * nothing" is a real outcome, and a fallback that is only exercised through disk I/O is a
     * fallback nobody tests.
     */
    static String markerOr(String value, String fallback) {
        for (String s : MARKER_SHAPES) {
            if (s.equals(value)) {
                return s;
            }
        }
        return fallback;
    }

    /** The chart at the centre of the wheel - yours. Spheres, as this app has always drawn. */
    public static String natalMarker() {
        return markerOr(get(MARKER_NATAL_KEY, MARKER_SPHERE), MARKER_SPHERE);
    }

    /** Chart B of a synastry: the second person, never the sky. */
    public static String synastryMarker() {
        return markerOr(get(MARKER_SYNASTRY_KEY, MARKER_PYRAMID), MARKER_PYRAMID);
    }

    /** The sky at the transit moment, wherever it is drawn. */
    public static String transitMarker() {
        return markerOr(get(MARKER_TRANSIT_KEY, MARKER_CUBE), MARKER_CUBE);
    }

    public static void setNatalMarker(String shape) {
        set(MARKER_NATAL_KEY, markerOr(shape, MARKER_SPHERE));
    }

    public static void setSynastryMarker(String shape) {
        set(MARKER_SYNASTRY_KEY, markerOr(shape, MARKER_PYRAMID));
    }

    public static void setTransitMarker(String shape) {
        set(MARKER_TRANSIT_KEY, markerOr(shape, MARKER_CUBE));
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

    /**
     * Which decan scheme the wheel's decan band draws. Visual only, and deliberately so.
     *
     * <b>This is not a choice of decan system, because that choice cannot be offered.</b> The
     * app carries two schemes bound to different datasets - Chaldean to the Sabian
     * decan_ruler field and the Golden Dawn tarot cards, triplicity to the decan prose - and
     * they disagree for 30 of the 36. A global toggle would leave the ruler on screen
     * contradicting the prose beside it, which is why the DecanSystem enum was deleted on
     * 2026-09-02 rather than wired up.
     *
     * This setting moves one band of glyphs and nothing else. Both schemes stay named on
     * every surface that reports a ruler, so a reader can always see which is which.
     */
    public static final String DECAN_RING_KEY = "chart.decanRing";

    /** The triplicity decan sign, which is what this band has always drawn. */
    public static final String DECAN_RING_TRIPLICITY = "Triplicity signs";
    /** The Chaldean face ruler, as a planet glyph. */
    public static final String DECAN_RING_CHALDEAN = "Chaldean faces";

    public static final String[] DECAN_RINGS = {DECAN_RING_TRIPLICITY, DECAN_RING_CHALDEAN};

    public static String decanRing() {
        String v = get(DECAN_RING_KEY, DECAN_RING_TRIPLICITY);
        for (String r : DECAN_RINGS) {
            if (r.equals(v)) {
                return r;
            }
        }
        return DECAN_RING_TRIPLICITY;
    }

    public static void setDecanRing(String ring) {
        set(DECAN_RING_KEY, ring);
    }

    /**
     * What the outer wheel carries when there is one: the sky, or the progressed chart.
     *
     * Read here rather than threaded through applyChartSettings because it behaves like the
     * house system and the node variant - it changes what the wheel means, the panel asks for
     * it when it computes, and nothing between the two needs to carry it.
     */
    public static final String OUTER_WHEEL_KEY = "chart.outerWheel";

    /** The sky at the transit moment - what the outer wheel has always shown. */
    public static final String OUTER_TRANSITS = "Transits";
    /** The chart advanced a day per year of life, drawn round the natal frame. */
    public static final String OUTER_PROGRESSED = "Progressions";

    public static final String[] OUTER_WHEELS = {OUTER_TRANSITS, OUTER_PROGRESSED};

    public static String outerWheel() {
        String v = get(OUTER_WHEEL_KEY, OUTER_TRANSITS);
        for (String o : OUTER_WHEELS) {
            if (o.equals(v)) {
                return o;
            }
        }
        return OUTER_TRANSITS;
    }

    public static void setOuterWheel(String which) {
        set(OUTER_WHEEL_KEY, which);
    }

    /**
     * How much of the aspect geometry the wheel draws. Settled 2026-09-03; DECISIONS.md, K5.
     *
     * <b>Drawing is not the same question as computing.</b> Every mode here computes the same
     * aspects and lists them in the grid and the placements; what changes is how many become
     * lines across the middle of the wheel. Tompkins puts it directly - note the aspects to
     * the minor bodies, do not draw them in, "so that the essentials can be more quickly
     * located". Nothing is lost by any of these settings, only relocated.
     */
    public static final String ASPECT_MODE_KEY = "chart.aspectMode";

    /** Lines between the ten classical planets only. The default. */
    public static final String ASPECTS_ESSENTIAL = "Essential (planets)";
    /** Adds the four angles, which is where internal pressure becomes an outward event. */
    public static final String ASPECTS_MANIFESTATION = "Manifestation (planets and angles)";
    /** Everything the engine finds, with the minor bodies drawn faintly. */
    public static final String ASPECTS_ESOTERIC = "Esoteric (all bodies)";

    public static final String[] ASPECT_MODES =
        {ASPECTS_ESSENTIAL, ASPECTS_MANIFESTATION, ASPECTS_ESOTERIC};

    public static String aspectMode() {
        String v = get(ASPECT_MODE_KEY, ASPECTS_ESSENTIAL);
        for (String m : ASPECT_MODES) {
            if (m.equals(v)) {
                return m;
            }
        }
        return ASPECTS_ESSENTIAL;
    }

    public static void setAspectMode(String mode) {
        set(ASPECT_MODE_KEY, mode);
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
