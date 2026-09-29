package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartSubject;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Four wheels at once: natal, progressed, directed and the sky (G17).
 *
 * <pre>
 *  Part A - when the directed chart earns a band of its own, and when it rides the middle ring.
 *  Part B - the arithmetic in the new band. ONE arc added to everything, and demonstrably not
 *           what the progressed ring holds - the two rings must be two techniques, or the
 *           fourth wheel is decoration.
 *  Part C - the band nests without moving anything. With the directed band shut the radii are
 *           the old radii to the pixel; with it open, four bands nest strictly inward.
 *  Part D - AND IT IS ACTUALLY PAINTED. Everything above can pass on a wheel that draws three
 *           rings, which is exactly the state this file was written to end.
 *
 *   java -cp "src\main\java;lib\*" com.zodiacomputing.ourania.gui.FourWheelCheck
 * </pre>
 *
 * <p>The fixture is deliberately not the app's usual 1982 test chart. Nothing here needs to be
 * comparable with another suite's numbers, and there was no reason to add a twenty-sixth file
 * carrying that date.
 */
public final class FourWheelCheck {

    private FourWheelCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            castAChart(sky);
            part("A: when the directed chart gets a band", () -> theRule(sky));
            part("B: one arc, in a band of its own", () -> theArithmetic(sky));
            part("C: the band nests without moving anything", FourWheelCheck::theGeometry);
            part("D: and it is painted", () -> thePainting(sky));
        } finally {
            Settings.setOuterWheel(Settings.OUTER_TRANSITS);
            SwingUtilities.invokeAndWait(w::dispose);
        }
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
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
    }

    /** Put the wheel into a named state and recompute everything that follows from it. */
    private static void show(SkymapPanel sky, boolean progressed, boolean arc) throws Exception {
        List<String> on = new ArrayList<>();
        on.add(Settings.OUTER_TRANSITS);
        if (progressed) {
            on.add(Settings.OUTER_PROGRESSED);
        }
        if (arc) {
            on.add(Settings.OUTER_SOLAR_ARC);
        }
        Settings.setOuterWheels(on);
        CheckReflect.set(sky, "showProgressed", Boolean.valueOf(progressed));
        CheckReflect.set(sky, "showSolarArc", Boolean.valueOf(arc));
        invoke(sky, "applyRingFlags");
        sky.updateChartData();
    }

    private static void invoke(Object target, String name) throws Exception {
        java.lang.reflect.Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }

    /**
     * <b>The rule is asked of the middle ring, not restated.</b> assignRingKinds is the one
     * place that works out what that ring carries, and a second copy of its precedence order
     * would be the defect this panel has recorded against itself twice.
     */
    private static void theRule(SkymapPanel sky) throws Exception {
        show(sky, false, true);
        ok("solar arc alone still rides the middle ring",
            sky.outerRing.kind == WheelRing.Kind.SOLAR_ARC);
        ok("and takes no band of its own, so nothing about that layout changed",
            !sky.arcRingShown());

        show(sky, true, true);
        ok("with progressions up, the middle ring is the progressed chart",
            sky.outerRing.kind == WheelRing.Kind.PROGRESSED);
        ok("and the directed chart gets a band of its own", sky.arcRingShown());
        ok("which is four wheels: natal, progressed, directed, sky",
            sky.natalRing.kind == WheelRing.Kind.CHART_A
                && sky.outerRing.kind == WheelRing.Kind.PROGRESSED
                && sky.arcRing.kind == WheelRing.Kind.SOLAR_ARC
                && sky.skyRing.kind == WheelRing.Kind.SKY);

        show(sky, true, false);
        ok("without solar arc there is no directed band", !sky.arcRingShown());
        ok("and the ring holds nothing", !anyValid(sky.arcRing));

        // The order the wheel nests them in is WheelStack's, and it is astrology: the person's
        // own derived charts sit inside anyone else's, and the sky is outermost.
        List<WheelRing.Kind> order = java.util.Arrays.asList(WheelStack.ORDER);
        ok("the directed band nests outside the progressed one",
            order.indexOf(WheelRing.Kind.SOLAR_ARC)
                > order.indexOf(WheelRing.Kind.PROGRESSED));
        ok("and inside the sky",
            order.indexOf(WheelRing.Kind.SOLAR_ARC) < order.indexOf(WheelRing.Kind.SKY));
    }

    private static void theArithmetic(SkymapPanel sky) throws Exception {
        show(sky, true, true);
        ok("the directed band has positions", anyValid(sky.arcRing));

        // <b>The whole claim of a directed chart in one assertion.</b> One number is added to
        // every position, so every body's distance from its natal place is the SAME distance.
        double first = Double.NaN;
        double worst = 0.0;
        int counted = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            if (!sky.arcRing.valid[i] || !sky.natalRing.valid[i] || Bodies.at(i).isAngle()) {
                continue;
            }
            double moved = Zodiac.normalise(sky.arcRing.lon[i] - sky.natalRing.lon[i]);
            if (Double.isNaN(first)) {
                first = moved;
            }
            worst = Math.max(worst, apart(moved, first));
            counted++;
        }
        ok("enough bodies to mean anything (" + counted + ")", counted >= 8);
        ok("every body moved by the same arc (worst disagreement "
            + String.format("%.6f", worst) + " degrees)", worst < 1e-6);
        // <b>"Not zero" is too weak, and the mutation run is what says so.</b> The arc is
        // measured from the date being ASKED about, and an arc worked out from the progressed
        // moment instead - a day about forty days after birth - is still comfortably non-zero
        // at about a tenth of a degree. Solar arc is near enough a degree a year and this
        // fixture runs 1969 to 2026, so the arc has to be a lifetime's worth.
        double years = 2026 - 1969;
        ok("and the arc is a lifetime's worth, not a few days (" + String.format("%.2f", first)
            + " degrees over about " + (int) years + " years)",
            first > years * 0.75 && first < years * 1.25);

        // <b>And the contrast that makes the fourth wheel worth drawing.</b> If the two rings
        // held the same thing there would be no reason for both to be on the wheel - so the
        // progressed ring must NOT be one shared offset.
        double spread = 0.0;
        double progressedFirst = Double.NaN;
        for (int i = 0; i < Bodies.count(); i++) {
            if (!sky.outerRing.valid[i] || !sky.natalRing.valid[i] || Bodies.at(i).isAngle()) {
                continue;
            }
            double moved = Zodiac.normalise(sky.outerRing.lon[i] - sky.natalRing.lon[i]);
            if (Double.isNaN(progressedFirst)) {
                progressedFirst = moved;
            }
            spread = Math.max(spread, apart(moved, progressedFirst));
        }
        ok("the progressed ring is NOT one shared offset (spread "
            + String.format("%.2f", spread) + " degrees)", spread > 5.0);

        // Nothing in a directed chart moves under its own power.
        boolean anySpeed = false;
        for (int i = 0; i < Bodies.count(); i++) {
            if (sky.arcRing.valid[i] && sky.arcRing.speed[i] != 0.0) {
                anySpeed = true;
            }
        }
        ok("no directed body has a speed, so none can be retrograde", !anySpeed);

        // The houses stay natal: directed BODIES in the birth frame, like the progressed ring.
        boolean cuspsMatch = true;
        for (int i = 0; i < sky.arcRing.cusps.length; i++) {
            if (Math.abs(sky.arcRing.cusps[i] - sky.natalRing.cusps[i]) > 1e-9) {
                cuspsMatch = false;
            }
        }
        ok("and the houses are the birth chart's", cuspsMatch);
    }

    private static void theGeometry() {
        for (int size : new int[] {700, 900, 1100, 1400}) {
            int[] shut = SkymapPanel.ringRadii(size, size, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0);
            int[] open = SkymapPanel.ringRadii(size, size, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0,
                1.0);

            // <b>The no-regression claim, and it is the one that had to be true.</b> A shut
            // band takes no room, so a wheel not showing a directed chart is the wheel it was.
            int[] was = SkymapPanel.ringRadii(size, size, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
            boolean same = true;
            for (int i = 0; i < was.length; i++) {
                if (was[i] != shut[i]) {
                    same = false;
                }
            }
            ok("with the directed band shut the wheel is unmoved at " + size, same);
            ok("and its two edges coincide at " + size,
                shut[SkymapPanel.RING_ARC_INNER] == shut[SkymapPanel.RING_TRANSIT]);

            // Open, the four bands nest strictly inward and the natal wheel keeps its floor.
            ok("the directed band is inside the sky's at " + size,
                open[SkymapPanel.RING_ARC_INNER] < open[SkymapPanel.RING_TRANSIT]);
            ok("and outside the middle ring's at " + size,
                open[SkymapPanel.RING_ARC_INNER] > open[SkymapPanel.RING_BODY_TOP]);
            ok("the sky band still hangs from the zodiac at " + size,
                open[SkymapPanel.RING_TRI] == open[SkymapPanel.RING_DEGREE_INNER]);
            ok("and the natal wheel keeps its floor at " + size,
                open[SkymapPanel.RING_BODY_TOP] >= SkymapPanel.minNatalRadius());

            // <b>A fourth band is paid for by the bands, not by the chart.</b> The first
            // version of this asserted the natal wheel never grows, and at 700 it grows by a
            // pixel: three bands sharing the budget come to 111 where two came to 112. That is
            // the sharing working, not a defect - so the assertion is the one that is actually
            // wanted, which is that the chart never pays more than one band's worth.
            int cost = shut[SkymapPanel.RING_BODY_TOP] - open[SkymapPanel.RING_BODY_TOP];
            ok("a fourth band costs the chart no more than a band, at " + size + " (" + cost
                + ")", cost <= SkymapPanel.outerBandDepth(size / 2 - 10, 3));

            // <b>Each fraction has to reach its OWN band, and only unequal ones can show it.</b>
            // Everything above opens all three bands fully, and with equal fractions the order
            // they are passed in does not matter - a mutation that reversed them survived the
            // whole of this part. Distinct fractions pin which band each one governs.
            int depth = SkymapPanel.outerBandDepth(size / 2 - 10, 3);
            int[] tilted = SkymapPanel.ringRadii(size, size, 0.25, 1.0, 1.0, 1.0, 1.0, 1.0,
                0.0, 0.5);
            int skyBand = tilted[SkymapPanel.RING_DEGREE_INNER]
                - tilted[SkymapPanel.RING_TRANSIT];
            int arcBand = tilted[SkymapPanel.RING_TRANSIT] - tilted[SkymapPanel.RING_ARC_INNER];
            int midBand = tilted[SkymapPanel.RING_ARC_INNER]
                - tilted[SkymapPanel.RING_BODY_TOP];
            ok("the sky's fraction opens the sky's band at " + size + " (" + skyBand + ")",
                Math.abs(skyBand - depth) <= 1);
            ok("the directed fraction opens the directed band at " + size + " (" + arcBand
                + ")", Math.abs(arcBand - (int) Math.round(depth * 0.5)) <= 1);
            ok("and the middle ring's opens the middle band at " + size + " (" + midBand + ")",
                Math.abs(midBand - (int) Math.round(depth * 0.25)) <= 1);
        }
    }

    /**
     * <b>Everything above this can pass on a wheel that draws three rings.</b> The arithmetic
     * can be right, the radii can nest, the flags can be true, and the painter can quietly
     * never read any of it - which is precisely the state the first two commits of G17 left
     * the app in, and the state this part exists to make impossible to leave behind again.
     */
    private static void thePainting(SkymapPanel sky) throws Exception {
        JComponent chart = (JComponent) CheckReflect.get(sky, "chartPanel");
        ok("there is a chart panel to paint", chart != null);
        if (chart == null) {
            return;
        }
        final int w = 900;
        final int h = 900;
        SwingUtilities.invokeAndWait(() -> {
            chart.setSize(w, h);
            chart.doLayout();
        });

        // <b>The layout is held STILL and only the ring's contents change.</b> The first
        // version of this rendered with the band shut and again with it open, and a mutation
        // that stopped the band being painted at all SURVIVED: opening a fourth band changes
        // the depth every band gets, so the whole wheel reflows and tens of thousands of
        // pixels differ whether or not a single directed glyph is drawn. It was measuring
        // reflow and reporting it as drawing.
        //
        // So both renders here have the band open, the same radii and the same bloom. The only
        // difference is whether the ring has anything in it - and then every differing pixel
        // is a directed glyph, because nothing else moved.
        show(sky, true, true);
        settle(sky, "arcBloom", true);
        BufferedImage with = render(chart, w, h);

        boolean[] held = sky.arcRing.valid.clone();
        java.util.Arrays.fill(sky.arcRing.valid, false);
        BufferedImage blank = render(chart, w, h);
        System.arraycopy(held, 0, sky.arcRing.valid, 0, held.length);

        int differing = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (blank.getRGB(x, y) != with.getRGB(x, y)) {
                    differing++;
                }
            }
        }
        ok("the directed ring's own bodies are painted (" + differing + " pixels)",
            differing > 500);

        int[] radii = SkymapPanel.ringRadii(w, h, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0);
        double inner = radii[SkymapPanel.RING_ARC_INNER];
        double outer = radii[SkymapPanel.RING_TRANSIT];
        ok("the directed band has real depth (" + (int) (outer - inner) + " pixels)",
            outer - inner >= SkymapPanel.MIN_BAND_DEPTH);

        // <b>And they are painted in their OWN band.</b> Glyphs drawn from the wrong pair of
        // edges still appear, still differ, and land on top of another ring - which reads as a
        // crowded wheel rather than as a defect. A margin either side, because a bead and a
        // halo legitimately overhang the band they sit on.
        int cx = w / 2;
        int cy = h / 2;
        int inBand = 0;
        int strayed = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (blank.getRGB(x, y) == with.getRGB(x, y)) {
                    continue;
                }
                double r = Math.hypot(x - cx, y - cy);
                if (r >= inner - 16 && r <= outer + 16) {
                    inBand++;
                } else {
                    strayed++;
                }
            }
        }
        ok("and they are painted in the directed band (" + inBand + " in, " + strayed
            + " out)", inBand > 500 && strayed * 10 < inBand);

        // <b>Turning it off again must put the wheel back, or the band has leaked.</b> Taken
        // against a render made BEFORE the band was ever opened, so this is a round trip and
        // not a comparison with itself. The fold is an animation, so the bloom is settled at
        // each end - comparing pixels mid-fold would be asserting the frame rate, which is a
        // flake rather than a check.
        show(sky, true, false);
        settle(sky, "arcBloom", false);
        BufferedImage shut = render(chart, w, h);
        show(sky, true, true);
        settle(sky, "arcBloom", true);
        render(chart, w, h);
        show(sky, true, false);
        settle(sky, "arcBloom", false);
        BufferedImage again = render(chart, w, h);
        int lingering = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (shut.getRGB(x, y) != again.getRGB(x, y)) {
                    lingering++;
                }
            }
        }
        ok("and switching it off puts the wheel back (" + lingering + " pixels left over)",
            lingering == 0);
    }

    /** Put a bloom where it is going without waiting for it to travel. */
    private static void settle(SkymapPanel sky, String field, boolean open) throws Exception {
        Object bloom = CheckReflect.get(sky, field);
        java.lang.reflect.Method m =
            bloom.getClass().getDeclaredMethod("setImmediately", boolean.class);
        m.setAccessible(true);
        m.invoke(bloom, Boolean.valueOf(open));
    }

    private static BufferedImage render(JComponent chart, int w, int h) throws Exception {
        final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g2 = img.createGraphics();
            chart.printAll(g2);
            g2.dispose();
        });
        return img;
    }

    /**
     * How far apart two longitudes are, the short way round.
     *
     * <p>Local, because {@code Zodiac} has no such helper and adding one to a shared astro
     * class to serve a single check would be a change with a much wider blast radius than the
     * three lines it saves. If a third caller appears, that is the moment to move it.
     */
    private static double apart(double a, double b) {
        double d = Math.abs(Zodiac.normalise(a - b));
        return d > 180.0 ? 360.0 - d : d;
    }

    private static boolean anyValid(WheelRing ring) {
        for (boolean v : ring.valid) {
            if (v) {
                return true;
            }
        }
        return false;
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable thrown) {
            ok(title + " ran to the end (" + thrown + ")", false);
            thrown.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
