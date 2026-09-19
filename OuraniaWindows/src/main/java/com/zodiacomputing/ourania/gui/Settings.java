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
    /**
     * The reader's settings file - or, for a check suite, a scratch copy of it.
     *
     * <b>The suites were writing this file, and one of them overwrote a saved birth
     * chart.</b> settings.properties held David's natal data; a run of NavigationCheck left it
     * holding the suite's fixture instead, 1972-09-22 in Los Angeles where 1982-08-10 in
     * Philadelphia had been. Separately, `aspects.enabled` was found empty - which this file's
     * own comment says means "the reader unticked every aspect" - and every check that read it
     * then saw no aspects at all and reported failures against code that was fine.
     *
     * Per-suite discipline is not the fix, because the writes happen four layers down from
     * anything a suite can see: generating a chart persists it, and generating a chart is what
     * half these panels do when they are built. So the path itself is redirectable, and the
     * suites point it at a temporary copy. A check cannot damage what it cannot address.
     *
     * The application never sets this property, so it always gets the real file.
     */
    static final String FILE_PROPERTY = "ourania.settings";

    private static String file() {
        String override = System.getProperty(FILE_PROPERTY);
        return override == null || override.isEmpty() ? "settings.properties" : override;
    }

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
    /**
     * Points this process at a scratch copy of the reader's settings.
     *
     * <b>For check suites, and the reason is a repair rather than a precaution.</b> A run of
     * NavigationCheck left settings.properties holding the suite's fixture birth data instead
     * of David's - 1972-09-22 in Los Angeles where 1982-08-10 in Philadelphia had been - and
     * separately `aspects.enabled` was found written empty, which this file reads as "the
     * reader unticked every aspect" and which then failed three checks against correct code.
     *
     * The writes are not something a suite can simply refrain from: generating a chart
     * persists it, and building half these panels generates a chart. Copying the file and
     * redirecting to the copy is the only version of this that cannot be forgotten in one
     * place. There is no cache to invalidate - load() reads the file every time.
     */
    public static void useScratchFile() {
        try {
            File real = new File("settings.properties");
            File scratch = File.createTempFile("ourania-settings-", ".properties");
            scratch.deleteOnExit();
            if (real.exists()) {
                java.nio.file.Files.copy(real.toPath(), scratch.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            System.setProperty(FILE_PROPERTY, scratch.getAbsolutePath());
            // The file underneath has changed identity, so anything parsed from the old one is
            // now the wrong answer.
            forget();
        } catch (Exception ex) {
            // A suite that cannot get a scratch file must not silently write the real one.
            throw new IllegalStateException(
                "could not isolate settings.properties for this run", ex);
        }
    }

    /** The last file parsed, and what it was stamped with when it was parsed. */
    private static Properties cached;
    private static String cachedPath;
    private static long cachedStamp;
    private static long cachedLength;
    /** When the file was last asked whether it had changed. */
    private static long lastStatNanos;

    /**
     * How long a settings file edited outside the app can go unnoticed, in milliseconds.
     *
     * Half a second: shorter than anyone can retype a value and switch windows, and long enough
     * that a frame drawing hundreds of coloured things asks the filesystem once rather than 694
     * times.
     */
    public static final long STAT_INTERVAL_MS = 500;

    /**
     * The stored settings, parsed at most once per change of the file.
     *
     * <b>This used to read and parse the file on every call, and the cost was the whole frame.</b>
     * Measured 2026-09-15: one repaint of the wheel asked for settings <b>694 times</b> - the
     * palette is consulted per body, per aspect, per ring - at about 139 microseconds a call,
     * which is 96 of the 109 milliseconds that repaint took. Building the window asked 1,244
     * times. The file is about a kilobyte; the work was all in opening and parsing it again.
     *
     * <b>Still one read per change, not one read per session.</b> The cache is keyed on the file's
     * own timestamp and length, so an edit made outside the app - by hand, or by the other agent
     * working this tree - is picked up on the next call rather than ignored until restart. Our own
     * writes clear it outright in {@link #update}, so a save is never served stale, and
     * {@link #useScratchFile} clears it because the file underneath has changed identity.
     *
     * Synchronized because the ephemeris workers read settings off the event thread, and two
     * threads parsing the same file into two Properties is work done twice to get one answer.
     */
    public static synchronized Properties load() {
        // <b>And the check itself is rationed.</b> Asking the filesystem for a timestamp and a
        // length is two system calls, which measured 53 microseconds a time on this machine - so a
        // frame that consults the settings 694 times was still spending 37 milliseconds proving
        // the file had not changed. The stamp is re-read at most every STAT_INTERVAL_MS, which
        // bounds how long an edit made outside the app can go unnoticed rather than removing the
        // check. Our own writes clear the cache outright and do not wait for it.
        long now = System.nanoTime();
        if (cached != null && now - lastStatNanos < STAT_INTERVAL_MS * 1000000L) {
            return cached;
        }
        File f = new File(file());
        String path = f.getPath();
        long stamp = f.lastModified();
        long length = f.length();
        lastStatNanos = now;
        if (cached != null && path.equals(cachedPath) && stamp == cachedStamp
                && length == cachedLength) {
            return cached;
        }
        Properties p = new Properties();
        lastLoadFailed = false;
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                p.load(fis);
            } catch (Exception ex) {
                lastLoadFailed = true;
                ex.printStackTrace();
            }
        }
        // A file that would not parse is not cached: the next call should try again rather than
        // serve an empty set for the rest of the session.
        if (!lastLoadFailed) {
            cached = p;
            cachedPath = path;
            cachedStamp = stamp;
            cachedLength = length;
        } else {
            forget();
        }
        return p;
    }

    /** Drops the parsed copy, so the next read goes back to the file. */
    public static synchronized void forget() {
        cached = null;
        cachedPath = null;
        cachedStamp = 0L;
        cachedLength = 0L;
        lastStatNanos = 0L;
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
            // A write must not be served from the copy that was read a moment ago: mutate a copy
            // of what is on disk, and drop the cache either side so nothing can observe the file
            // and the cache disagreeing.
            Properties write = new Properties();
            write.putAll(p);
            mutation.accept(write);
            write.setProperty(SCHEMA_KEY, String.valueOf(SCHEMA));
            forget();
            try (FileOutputStream fos = new FileOutputStream(file())) {
                write.store(fos, "Ourania Settings");
            }
            forget();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Moves an unreadable settings file aside so the next write does not destroy it. */
    private static void preserveCorrupt() {
        File f = new File(file());
        if (!f.exists()) {
            return;
        }
        File aside = new File(file() + ".corrupt");
        for (int i = 2; aside.exists() && i < 100; i++) {
            aside = new File(file() + ".corrupt." + i);
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

    /** Settings key for the translucent sign shell on the globe. */
    public static final String GLOBE_SIGN_PLANE_KEY = "chart.globeSignPlane";

    /**
     * Whether the globe fills the signs as a translucent coloured shell.
     *
     * On by default, because it is what makes the globe a zodiac rather than a wireframe: a body
     * seen through it is in a sign and in a house at once, which is the thing the flat wheel
     * cannot draw. Off leaves the sign boundaries, the band at the equator and the degree scale,
     * so nothing is lost except the wash of colour - which is worth having when the aspect
     * network underneath is what the reader is actually looking at.
     */
    public static boolean globeSignPlane() {
        return !"false".equals(get(GLOBE_SIGN_PLANE_KEY, "true"));
    }

    public static void setGlobeSignPlane(boolean on) {
        set(GLOBE_SIGN_PLANE_KEY, on ? "true" : "false");
    }

    /** Settings key for filling the current house on the globe. */
    public static final String GLOBE_HOUSE_FILL_KEY = "chart.globeHouseFill";

    /** Settings key for filling the pointed-at degree on the globe. */
    public static final String GLOBE_DEGREE_FILL_KEY = "chart.globeDegreeFill";

    /** Settings key for filling the lunar mansion stations on the globe. */
    public static final String GLOBE_MANSION_FILL_KEY = "chart.globeMansionFill";

    /**
     * Whether the globe washes the house the reader is on with colour.
     *
     * <b>The same bargain the sign shell strikes, band by band.</b> David asked for the sign
     * shell's switch on the other bands too, 2026-09-16. Off leaves the cusp spokes, the glyphs
     * and the numbers exactly where they are and takes only the wash, so a reader who is
     * looking at the aspect network underneath can clear the colour without losing the frame
     * they are reading positions against. On by default, because the wash is what answers
     * "which house is this" without a click.
     *
     * <b>There is no decan control, and that is not an oversight.</b> The decan band on the
     * globe is glyph billboards and nothing else - no shell, no wash, not even a hover fill -
     * so a switch for it would govern nothing. A control that does nothing is worse than an
     * absent one; the two empty screens removed in September are the same lesson.
     */
    public static boolean globeHouseFill() {
        return !"false".equals(get(GLOBE_HOUSE_FILL_KEY, "true"));
    }

    public static void setGlobeHouseFill(boolean on) {
        set(GLOBE_HOUSE_FILL_KEY, on ? "true" : "false");
    }

    /**
     * Whether the globe washes the degree under the cursor - the Sabian scale's own fill.
     *
     * The ticks, the scale line they hang from and the sign marks all stay when this is off;
     * what goes is the one-degree slice that lights behind the tick being pointed at.
     */
    public static boolean globeDegreeFill() {
        return !"false".equals(get(GLOBE_DEGREE_FILL_KEY, "true"));
    }

    public static void setGlobeDegreeFill(boolean on) {
        set(GLOBE_DEGREE_FILL_KEY, on ? "true" : "false");
    }

    /**
     * Whether the globe fills the lunar mansion stations with colour.
     *
     * <b>The most visible of the three.</b> The mansion band carries a standing wash on the
     * Moon's own station and a brighter one on the station under the cursor, over a band that
     * sits outside the whole zodiac. Off keeps the band's two edges, its 28 divisions and its
     * numbers - the stations stay countable, they simply stop being tinted.
     */
    public static boolean globeMansionFill() {
        return !"false".equals(get(GLOBE_MANSION_FILL_KEY, "true"));
    }

    public static void setGlobeMansionFill(boolean on) {
        set(GLOBE_MANSION_FILL_KEY, on ? "true" : "false");
    }

    /** Settings key for drawing the natal bodies as planets on the globe. */
    public static final String GLOBE_PLANETS_KEY = "chart.globePlanets";

    /**
     * Whether the globe draws the natal bodies as the planets themselves.
     *
     * <b>The globe is the one view where this is not a costume.</b> On the flat wheel a body
     * is a position and a glyph says which; on a sphere the reader is looking at a sky, and a
     * banded Jupiter or a ringed Saturn is what is actually up there. Off gives the plain
     * beads, which stay easier to read when every point is switched on.
     */
    public static boolean globePlanets() {
        return !"false".equals(get(GLOBE_PLANETS_KEY, "true"));
    }

    public static void setGlobePlanets(boolean on) {
        set(GLOBE_PLANETS_KEY, on ? "true" : "false");
    }

    /** Settings key for drawing Chart B's and the sky's bodies as planets too. */
    public static final String GLOBE_PLANETS_ALL_KEY = "chart.globePlanetsAllRings";

    /**
     * Whether every ring gets the planets, or only Chart A.
     *
     * <b>The objection this answers is real, so it is answered rather than dropped.</b> Only
     * Chart A drew planets, on the reasoning that a partner's Jupiter drawn as Jupiter is
     * indistinguishable from the reader's - the ring colour was the only thing telling three
     * Jupiters apart, and a photograph of Jupiter overrides a colour. David asked for all three
     * anyway, so the planet keeps a ring of its own chart's ink around it: whose it is stays a
     * colour, and what it is becomes a picture. Off restores Chart A only.
     */
    public static boolean globePlanetsAllRings() {
        return !"false".equals(get(GLOBE_PLANETS_ALL_KEY, "true"));
    }

    public static void setGlobePlanetsAllRings(boolean on) {
        set(GLOBE_PLANETS_ALL_KEY, on ? "true" : "false");
    }

    /** Settings key for bowing the globe's aspect lines over the centre. */
    public static final String GLOBE_ASPECT_ARCS_KEY = "chart.globeAspectArcs";

    /**
     * Whether an aspect on the globe arcs over the middle or runs straight through it.
     *
     * <b>The straight line is the true one and the arc is the readable one.</b> Two bodies in
     * aspect are joined by a chord, and on a flat wheel that is all a line can be. On a sphere
     * the chord spends its length in the crowded interior, and an opposition - the aspect a
     * reader most wants to see - is the diameter that goes through the exact middle, where
     * every other line already is. Arced, it climbs over the centre instead, so the widest
     * aspects ride highest and the figure a chart makes has a shape from the side. Off gives
     * the chords back, which stay easier to trace when only two or three are drawn.
     */
    public static boolean globeAspectArcs() {
        return !"false".equals(get(GLOBE_ASPECT_ARCS_KEY, "true"));
    }

    public static void setGlobeAspectArcs(boolean on) {
        set(GLOBE_ASPECT_ARCS_KEY, on ? "true" : "false");
    }

    /** Settings key for stacking the globe rings instead of crossing them. */
    public static final String GLOBE_STACKED_RINGS_KEY = "chart.globeStackedRings";

    /**
     * Whether the partner and sky rings are stacked above and below the natal plane, or tilted
     * across it.
     *
     * <b>Two ways to keep three rings apart, and they keep different promises.</b> Crossed is
     * the older one: the partner tips one way and the sky the other, so the three planes are
     * unmistakably three planes and all of them meet at the Ascendant. What it costs is that a
     * tilted ring turns longitude into something other than the angle you see, so a transit
     * conjunct a natal planet sits over it only at the Ascendant and the Descendant.
     *
     * Stacked lifts the sky just above the natal plane and the partner just below it, leaving
     * all three parallel. Every degree keeps the direction it has on the natal ring, so a
     * conjunction across charts is one body directly above another and can be read at a
     * glance. Nothing crosses anything any more, which is the trade.
     */
    public static boolean globeStackedRings() {
        return !"false".equals(get(GLOBE_STACKED_RINGS_KEY, "true"));
    }

    public static void setGlobeStackedRings(boolean on) {
        set(GLOBE_STACKED_RINGS_KEY, on ? "true" : "false");
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
    /**
     * Bodies as far out as the natal wheel goes.
     *
     * <b>The stored value still says "outside the sign ring", and it is kept that way
     * deliberately.</b> It used to be literally true - the natal bodies were drawn between the
     * signs and the transit wheel - until the zodiac moved outermost on 2026-09-06 and there
     * stopped being anywhere out there for them to go. Changing the constant would change what
     * is written in every reader's settings file, and a preference that silently resets itself
     * because a label was corrected is a worse outcome than a stale string on disk. The
     * displayed name comes from {@link #bodyRingLabel} instead.
     */
    public static final String RING_OUTSIDE = "Outside the sign ring";

    public static final String[] BODY_RINGS = {RING_DEFAULT, RING_CENTRE, RING_OUTSIDE};

    /** What to show the reader for a placement, which is not always what is stored. */
    public static String bodyRingLabel(String stored) {
        return RING_OUTSIDE.equals(stored) ? "Against the ring above" : stored;
    }

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

    /** Tropical, or one of the sidereal ayanamsas; the labels are Ephemeris.ZODIACS. */
    public static final String ZODIAC_KEY = "chart.zodiac";

    public static String zodiac() {
        return get(ZODIAC_KEY, com.zodiacomputing.ourania.astro.Ephemeris.ZODIACS[0][0]);
    }

    /** Saves the zodiac and puts it in force for every calculation from here on. */
    public static void setZodiac(String label) {
        set(ZODIAC_KEY, label);
        com.zodiacomputing.ourania.astro.Ephemeris.setZodiac(label);
    }

    /** The one transit orb in degrees; see Transits.orb for why it is one flat width. */
    public static final String TRANSIT_ORB_KEY = "transit.orb";
    public static final double TRANSIT_ORB_MIN = 0.25;
    public static final double TRANSIT_ORB_MAX = 5.0;

    /** The stored transit orb, or the default when it is missing, unreadable or out of range. */
    public static double transitOrb() {
        double fallback = com.zodiacomputing.ourania.astro.Transits.DEFAULT_ORB;
        try {
            double v = Double.parseDouble(get(TRANSIT_ORB_KEY, String.valueOf(fallback)).trim());
            return v >= TRANSIT_ORB_MIN && v <= TRANSIT_ORB_MAX ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Saves the transit orb and puts it in force for every transit surface from here on. */
    public static void setTransitOrb(double degrees) {
        double v = Math.max(TRANSIT_ORB_MIN, Math.min(TRANSIT_ORB_MAX, degrees));
        set(TRANSIT_ORB_KEY, String.valueOf(v));
        com.zodiacomputing.ourania.astro.Transits.orb = v;
    }

    /** Which rule progresses the angles; see astro.ProgressedAngles for the three. */
    public static final String PROGRESSED_ANGLES_KEY = "progressions.angleMethod";

    public static com.zodiacomputing.ourania.astro.ProgressedAngles.Method progressedAngleMethod() {
        return com.zodiacomputing.ourania.astro.ProgressedAngles.Method.of(
            get(PROGRESSED_ANGLES_KEY, null));
    }

    /** Saves the rule and puts it in force for every progressed reading from here on. */
    public static void setProgressedAngleMethod(
            com.zodiacomputing.ourania.astro.ProgressedAngles.Method m) {
        com.zodiacomputing.ourania.astro.ProgressedAngles.Method use =
            m == null ? com.zodiacomputing.ourania.astro.ProgressedAngles.Method.SOLAR_ARC : m;
        set(PROGRESSED_ANGLES_KEY, use.label);
        com.zodiacomputing.ourania.astro.ProgressedAngles.method = use;
    }

    public static void setTrueNode(boolean useTrue) {
        set(NODE_VARIANT_KEY, useTrue ? "true" : "mean");
    }

    public static void setTrueLilith(boolean useTrue) {
        set(LILITH_VARIANT_KEY, useTrue ? "true" : "mean");
    }
}
