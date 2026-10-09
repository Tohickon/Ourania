package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartSubject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * The natal wheel drawing its bodies as the bodies themselves, and the one ring that must not.
 *
 * <p><b>Why this suite exists.</b> {@link Planets} - the Sun's corona, Jupiter's belts,
 * Saturn's rings - was private inside GlobeRenderer while the wheel painted every natal bead
 * the same {@code Color(192, 192, 192)}. The renderer was already shared and already behind a
 * {@link Pen}; the wheel simply never called it. Moving it out is the kind of change that
 * passes every existing suite whether or not the wheel actually draws anything, because
 * nothing asserted what colour a bead was.
 *
 * <p><b>What would otherwise rot is Part D.</b> The outer rings tint their beads to say whose
 * body they are - chart B's Mars against the sky's Mars - and MarkerShapeCheck exists because
 * two rings once differed by a tint alone. A planet face paints Mars's own red regardless of
 * ring, so extending faces outward without also carrying the ring's ink would silently undo
 * the only distinction those rings have. Part D pins the outer rings as unfaced, so that
 * extension has to be a decision rather than a one-word edit.
 *
 * <p><b>Part B is the one that cannot be faked.</b> Every other assertion here is about
 * tables and conditions, and all of them can pass on a wheel that draws nothing: the test
 * that matters is two renders of the same chart, planets on and planets off, differing in
 * pixels. That is FourWheelCheck Part D's lesson applied - hold the layout still and change
 * exactly one thing, so every differing pixel has one explanation.
 *
 * <p><b>Deliberately not asserted:</b> that the Sun's corona stays inside the natal band. It
 * does not - it reaches {@code rad + 25}, which is past NATAL_EDGE, and bleeds faintly into
 * the zodiac ring. That is light rather than a disc, it is drawn at a sixteenth of alpha and
 * less, and it is what a sun looks like. The containment rule is about beads, which are
 * opaque and are where a reader reads a position.
 */
public final class PlanetFaceCheck {

    private PlanetFaceCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** The bodies there is a picture of. Everything else keeps its bead. */
    private static final String[] FACED = {"Sun", "Mercury", "Venus", "Mars", "Jupiter",
                                           "Saturn", "Uranus", "Neptune", "Pluto"};
    /** A sample of the rest - including the Moon, which is drawn as its own phase instead. */
    private static final String[] UNFACED = {"Moon", "Ceres", "Vesta", "Chiron", "Eris"};

    public static void main(String[] args) throws Exception {
        // Never the reader's own settings file: a suite that generates a chart persists it,
        // and one of these once overwrote a saved birth chart. See Settings.useScratchFile.
        Settings.useScratchFile();

        System.out.println("=== Part A: which bodies there is a picture of ===");
        int before = failures.size();
        theRoster();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: and the wheel actually draws them ===");
        before = failures.size();
        thePainting();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: the glyph still reads on top of one ===");
        before = failures.size();
        theInk();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: the outer rings keep their beads ===");
        before = failures.size();
        theOuterRings();
        report("Part D", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            // <b>Explicit, because this suite builds a window.</b> Returning from main does
            // not end a JVM with a live AWT event thread in it, and a suite that passes and
            // then hangs forever is worse than one that fails: build.ps1 -All waits on it.
            // Found by this suite doing exactly that - it sat at 18 seconds of CPU and no
            // output for three minutes, and the missing output was buffered stdout that a
            // non-exiting process never flushed. FourWheelCheck exits for the same reason.
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    /** The roster, and that a face is a distinct colour rather than a shared default. */
    private static void theRoster() {
        for (String name : FACED) {
            int n = Bodies.indexOfName(name);
            ok(name + " is in the registry", n >= 0);
            if (n >= 0) {
                ok(name + " is drawn as itself", Planets.hasFace(n));
            }
        }
        for (String name : UNFACED) {
            int n = Bodies.indexOfName(name);
            if (n >= 0) {
                ok(name + " keeps its bead - there is no picture of it", !Planets.hasFace(n));
            }
        }

        // <b>Nine faces, nine colours, and none of them the fallback.</b> faceColour has a
        // default arm, and a faced body that fell through to it would be drawn as a planet
        // in the bead's own grey - the bead it was supposed to stop being, now without the
        // bead's shading.
        //
        // <b>Uniqueness alone did not catch that, and the mutation proved it.</b> Dropping
        // Saturn to the fallback left the nine still distinct from each other, because no
        // other faced body uses the fallback either - so the check passed on exactly the
        // failure the paragraph above describes. The fallback is named by ASKING for it: an
        // unfaced body's colour is the default arm by construction, so this cannot drift the
        // way a copied literal would.
        int fallback = Planets.faceColour(Bodies.indexOfName("Vesta")).getRGB();
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (String name : FACED) {
            int n = Bodies.indexOfName(name);
            if (n < 0) {
                continue;
            }
            int rgb = Planets.faceColour(n).getRGB();
            ok(name + " has a colour no other faced body has", seen.add(rgb));
            ok(name + " has a face colour of its own, not the fallback grey",
                rgb != fallback);
        }
        ok("every faced body answered a colour of its own", seen.size() == FACED.length);

        // An index outside the registry must answer false rather than throw: hasFace is
        // asked before a bounds check at more than one call site.
        ok("an index below the registry has no face", !Planets.hasFace(-1));
        ok("an index above the registry has no face", !Planets.hasFace(Bodies.count() + 5));
    }

    /**
     * Two renders of one chart, planets on and planets off.
     *
     * <b>The layout is held still and only the drawing changes.</b> Both renders are the same
     * window, the same chart and the same radii; the only difference is the setting. So every
     * differing pixel is a planet face, and a mutation that stops the wheel calling Planets
     * at all makes the two images identical - which is the failure this part is for.
     */
    private static void thePainting() throws Exception {
        final boolean wasOn = Settings.globePlanets();
        final String wasMarker = Settings.natalMarker();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            castAChart(sky);
            Settings.setNatalMarker(Settings.MARKER_SPHERE);

            final JComponent chart = (JComponent) CheckReflect.get(sky, "chartPanel");
            ok("there is a chart panel to paint", chart != null);
            if (chart == null) {
                return;
            }
            final int size = 900;
            SwingUtilities.invokeAndWait(() -> {
                chart.setSize(size, size);
                chart.doLayout();
            });
            settle(sky, "outerBloom", sky.showTransitChart);
            settle(sky, "triBloom", sky.showTriWheel);

            BufferedImage on = render(chart, size, true);
            BufferedImage off = render(chart, size, false);

            int differing = 0;
            long warmer = 0;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int a = on.getRGB(x, y);
                    int b = off.getRGB(x, y);
                    if (a == b) {
                        continue;
                    }
                    differing++;
                    // A planet face is warm or coloured; the bead it replaced is neutral
                    // grey. Counting the pixels that gained red over blue separates "the
                    // wheel drew planets" from "the wheel drew something, anywhere".
                    int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
                    int db = (a & 0xFF) - (b & 0xFF);
                    if (dr > db) {
                        warmer++;
                    }
                }
            }
            ok("planets on and planets off are different drawings (" + differing
                + " pixels)", differing > 2000);
            ok("and the difference is warm - faces, not noise (" + warmer + " of "
                + differing + ")", differing > 0 && warmer > differing / 3);

            // That the setting reaches the drawing at all is Part C's job, and it asks the
            // question directly rather than through a rendered image.
        } finally {
            Settings.setGlobePlanets(wasOn);
            Settings.setNatalMarker(wasMarker);
            SwingUtilities.invokeAndWait(w::dispose);
        }
    }

    /**
     * The glyph's ink against the face it sits on.
     *
     * <b>This is a legibility rule, so it is asserted as one.</b> "Not the element colour" is
     * too weak - any other colour would pass, including another unreadable one. The rule is
     * that the ink and the face are far apart in luma, which is what readableOn exists to
     * guarantee and what the Sun's red-on-orange glyph failed.
     */
    private static void theInk() {
        final boolean wasOn = Settings.globePlanets();
        final String wasMarker = Settings.natalMarker();
        try {
            Settings.setGlobePlanets(true);
            Settings.setNatalMarker(Settings.MARKER_SPHERE);
            Settings.setShowPlanetSpheres(true);

            for (String name : FACED) {
                int n = Bodies.indexOfName(name);
                if (n < 0) {
                    continue;
                }
                Color face = new Color(Planets.faceColour(n).getRGB(), true);
                Color ink = WheelShapes.natalGlyphInk(n, Color.RED);
                ok(name + "'s glyph is legible on its own face (luma gap "
                    + Math.round(Math.abs(luma(ink) - luma(face))) + ")",
                    Math.abs(luma(ink) - luma(face)) > 90);
            }

            // A body with no face keeps the element ink, which is where that colour means
            // something. Handing every glyph a contrast ink would throw the elements away.
            for (String name : UNFACED) {
                int n = Bodies.indexOfName(name);
                if (n >= 0) {
                    eq(name + " keeps its element ink", Color.RED,
                        WheelShapes.natalGlyphInk(n, Color.RED));
                }
            }

            // And with the faces switched off, so does everything else.
            Settings.setGlobePlanets(false);
            for (String name : FACED) {
                int n = Bodies.indexOfName(name);
                if (n >= 0) {
                    eq(name + " keeps its element ink when faces are off", Color.RED,
                        WheelShapes.natalGlyphInk(n, Color.RED));
                }
            }
        } finally {
            Settings.setGlobePlanets(wasOn);
            Settings.setNatalMarker(wasMarker);
        }
    }

    /**
     * <b>The negative pin.</b> A planet face overrides a ring tint, and the outer rings have
     * nothing else to say whose body they are. The wheel draws faces on the natal ring only,
     * and the way to keep that true is to assert that the outer rings are drawn by the method
     * that does not know about faces at all.
     */
    private static void theOuterRings() throws Exception {
        String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
            "src/main/java/com/zodiacomputing/ourania/gui/WheelCanvas.java")),
            java.nio.charset.StandardCharsets.UTF_8);

        int faced = count(src, "drawNatalBody(");
        eq("exactly one ring asks for a body drawn as itself", 1, faced);

        // The outer, directed and sky rings each still call the plain marker. If one of them
        // were switched to drawNatalBody the count above would rise and this would fall.
        ok("the outer rings still draw plain markers (" + count(src, "drawBodyMarker(")
            + " calls)", count(src, "drawBodyMarker(") >= 4);

        // And the thing that makes the distinction worth keeping: the outer rings tint their
        // beads per ring, which a face would paint over.
        ok("the outer rings tint their beads by ring", src.contains("ringBead("));
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static double luma(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static BufferedImage render(JComponent chart, int size, boolean planets)
            throws Exception {
        Settings.setGlobePlanets(planets);
        final BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g2 = img.createGraphics();
            chart.printAll(g2);
            g2.dispose();
        });
        return img;
    }

    private static void castAChart(SkymapPanel sky) throws Exception {
        ChartSubject a = ChartSubject.of("Chart A",
            ZonedDateTime.of(1969, 7, 20, 12, 0, 0, 0, ZoneId.of("America/New_York")),
            "Philadelphia", 39.95, -75.16, "America/New_York", false);
        ChartSubject when = ChartSubject.of("Sky",
            ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")),
            "Philadelphia", 39.95, -75.16, "America/New_York", false);
        java.lang.reflect.Method install = SkymapPanel.class.getDeclaredMethod(
            "installSubjects", ChartSubject.class, ChartSubject.class, ChartSubject.class);
        install.setAccessible(true);
        install.invoke(sky, a, ChartSubject.empty("Chart B"), when);
        CheckReflect.set(sky, "chartMode", ChartMode.TRANSIT);
        CheckReflect.set(sky, "transitsEnabled", Boolean.TRUE);
        sky.updateChartData();
    }

    private static void settle(SkymapPanel sky, String field, boolean open) throws Exception {
        Object bloom = CheckReflect.get(sky, field);
        java.lang.reflect.Method m =
            bloom.getClass().getDeclaredMethod("setImmediately", boolean.class);
        m.setAccessible(true);
        m.invoke(bloom, Boolean.valueOf(open));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
