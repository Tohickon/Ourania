package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

/**
 * Four things David asked for on 2026-09-16, while looking at the wheel.
 *
 * <ul>
 * <li><b>The mansions covered the outer degree scale.</b> "When lunar mansions are selected they
 * cover over the second outer sabian ring." They were drawn from {@code outer - 9} outward and
 * the ticks reached {@code outer - 6}, in the same band.</li>
 * <li><b>The leader lines stopped short.</b> "Can we extend the line all the way to the second
 * sabian ring degree." They ended at the inner degree scale.</li>
 * <li><b>The globe's other fills had no switch.</b> The sign shell could be turned off; the
 * house, degree and mansion washes could not. There is no decan switch, because the globe's decan
 * band has no fill to switch - Part E holds that absence too.</li>
 * <li><b>Select all was screen-wide only.</b> "A select all button for each section of toggled
 * items in settings, like for asteroids etc."</li>
 * </ul>
 *
 * <p>Parts A and B are the arithmetic, held without painting. Parts C, D and E paint - the flat
 * wheel and the globe - and measure ink, because every one of these is a drawing claim and the
 * question is where the drawing actually lands, not what a variable says.
 */
public final class RimAndFillsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final int W = 900;
    private static final int H = 820;
    private static final int GLOBE = 700;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        // No blooms: a frame painted after a layer changes should be of the settled wheel.
        Settings.set(Settings.ANIMATE_RINGS_KEY, "false");

        part("A: the mansion band is carved from the rim, and nothing else moves",
            RimAndFillsCheck::chain);
        part("B: the rim is two targets, disjoint and without a gap", RimAndFillsCheck::targets);

        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            final SkymapPanel sky = (SkymapPanel) fs.get(hold[0]);
            java.lang.reflect.Field fc = SkymapPanel.class.getDeclaredField("chartPanel");
            fc.setAccessible(true);
            final Component chart = (Component) fc.get(sky);
            SwingUtilities.invokeAndWait(() -> chart.setSize(W, H));
            Thread.sleep(2500);
            sky.updateChartData();
            Thread.sleep(1200);

            part("C: with the mansions open, the outer scale hangs below them",
                () -> scale(sky, chart));
            part("D: the leader lines reach the outer scale", () -> leaders(sky, chart));
            part("E: the globe's fills answer to their switches", () -> globeFills(sky));
            part("F: the controls, including the one that is not there",
                () -> controls(hold[0]));
            part("G: each section selects its own points", () -> sections(hold[0]));
        } finally {
            Settings.setShowDegreeLines(true);
            Settings.setGlobeHouseFill(true);
            Settings.setGlobeDegreeFill(true);
            Settings.setGlobeMansionFill(true);
            final OuraniaWindow w = hold[0];
            SwingUtilities.invokeAndWait(() -> w.dispose());
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

    // ------------------------------------------------------------------ A

    private static void chain() {
        int[][] sizes = {{600, 600}, {900, 820}, {1400, 1000}, {820, 1200}};
        for (int[] s : sizes) {
            for (double outerOpen : new double[] {0.0, 1.0}) {
                for (double triOpen : new double[] {0.0, 1.0}) {
                    String at = " at " + s[0] + "x" + s[1] + " outer=" + outerOpen
                        + " tri=" + triOpen;
                    int[] shut = SkymapPanel.ringRadii(s[0], s[1], outerOpen, triOpen,
                        1.0, 1.0, 1.0, 1.0, 0.0);
                    int[] open = SkymapPanel.ringRadii(s[0], s[1], outerOpen, triOpen,
                        1.0, 1.0, 1.0, 1.0, 1.0);
                    int[] legacy = SkymapPanel.ringRadii(s[0], s[1], outerOpen, triOpen,
                        1.0, 1.0, 1.0, 1.0);
                    int outer = open[SkymapPanel.RING_OUTER];

                    eq("the chain has ten radii" + at, 10, open.length);
                    eq("folded, the band has no depth" + at,
                        outer, shut[SkymapPanel.RING_MANSION_INNER]);
                    eq("a caller that predates the band sees it folded" + at,
                        outer, legacy[SkymapPanel.RING_MANSION_INNER]);
                    eq("open, the band is MANSION_BAND_DEPTH deep" + at,
                        outer - SkymapPanel.MANSION_BAND_DEPTH,
                        open[SkymapPanel.RING_MANSION_INNER]);

                    // <b>The whole point of carving it from the rim.</b> Every radius from the
                    // decans inward is where the bodies, the hit tests and natalRadii come
                    // from; if opening the mansions moved any of them, the wheel would jump.
                    boolean still = true;
                    for (int i = 0; i < SkymapPanel.RING_MANSION_INNER; i++) {
                        if (open[i] != shut[i] || open[i] != legacy[i]) {
                            still = false;
                        }
                    }
                    ok("opening the mansions moves no other ring" + at, still);

                    // The band and the ticks below it both fit inside the rim.
                    ok("the band and the outer scale fit above the decans" + at,
                        open[SkymapPanel.RING_MANSION_INNER] - 6
                            > open[SkymapPanel.RING_DECAN_OUTER]);
                }
            }
            int[] half = SkymapPanel.ringRadii(s[0], s[1], 0.0, 0.0, 1, 1, 1, 1, 0.5);
            int outer = half[SkymapPanel.RING_OUTER];
            ok("half open lies between folded and open at " + s[0] + "x" + s[1],
                half[SkymapPanel.RING_MANSION_INNER] < outer
                    && half[SkymapPanel.RING_MANSION_INNER]
                        > outer - SkymapPanel.MANSION_BAND_DEPTH);
        }
    }

    // ------------------------------------------------------------------ B

    private static void targets() {
        int[] open = SkymapPanel.ringRadii(W, H, 0.0, 0.0, 1, 1, 1, 1, 1.0);
        int[] shut = SkymapPanel.ringRadii(W, H, 0.0, 0.0, 1, 1, 1, 1, 0.0);
        int outer = open[SkymapPanel.RING_OUTER];
        int inner = open[SkymapPanel.RING_MANSION_INNER];

        int both = 0;
        int gaps = 0;
        for (double r = outer - SkymapPanel.RIM_BAND_DEPTH; r < outer + 15; r += 0.25) {
            boolean m = SkymapPanel.inMansionRing(r, open);
            boolean d = SkymapPanel.inRimDegreeBand(r, open);
            if (m && d) {
                both++;
            }
            if (!m && !d) {
                gaps++;
            }
        }
        eq("no radius is both the mansions and the scale", 0, both);
        eq("no radius on the rim answers nothing", 0, gaps);
        ok("the outer edge of the rim is the mansions", SkymapPanel.inMansionRing(outer - 2, open));
        ok("just under the band is the scale", SkymapPanel.inRimDegreeBand(inner - 2, open));
        ok("and the scale is not the mansions", !SkymapPanel.inMansionRing(inner - 2, open));

        boolean anyMansion = false;
        boolean allScale = true;
        for (double r = outer - SkymapPanel.RIM_BAND_DEPTH; r < outer + 15; r += 0.25) {
            anyMansion |= SkymapPanel.inMansionRing(r, shut);
            allScale &= SkymapPanel.inRimDegreeBand(r, shut);
        }
        ok("folded, nothing on the rim points at a mansion", !anyMansion);
        ok("folded, the scale has the whole rim back", allScale);
    }

    // ------------------------------------------------------------------ C

    /**
     * Where the outer scale's ink lands, measured rather than read from a variable.
     *
     * The scale is isolated by painting the wheel twice, once with the degree layer shown and
     * once with it folded, and keeping only what differs inside the rim. Folding the degrees does
     * move the inner scale and the bodies - but all of that is inside the decan ring, so within
     * the rim the only thing that can differ is the outer scale itself.
     */
    private static void scale(SkymapPanel sky, Component chart) throws Exception {
        Settings.setShowDegreeLines(false);
        Object g = geometry(sky);
        int cx = intField(g, "cx");
        int cy = intField(g, "cy");

        on(sky, SkymapPanel.Layer.MANSIONS, true);
        int[] rings = ringsOf(sky);
        int outer = rings[SkymapPanel.RING_OUTER];
        int mansionInner = rings[SkymapPanel.RING_MANSION_INNER];
        int decanOuter = rings[SkymapPanel.RING_DECAN_OUTER];
        ok("the painted wheel has the band open", mansionInner < outer);

        on(sky, SkymapPanel.Layer.DEGREES, true);
        BufferedImage withScale = stable(chart);
        on(sky, SkymapPanel.Layer.DEGREES, false);
        BufferedImage without = stable(chart);
        double[] reach = inkRadii(withScale, without, cx, cy, decanOuter + 2, outer + 3);
        System.out.printf("  mansions open: outer scale ink from %.1f to %.1f px, band %d to %d%n",
            reach[0], reach[1], mansionInner, outer);
        ok("the outer scale is drawn at all", reach[2] > 200);
        ok("the outer scale stays below the mansion band: reaches " + reach[1]
            + ", band starts " + mansionInner, reach[1] <= mansionInner + 2.0);

        // And folded, the scale has the rim back - the layout before the band existed.
        on(sky, SkymapPanel.Layer.MANSIONS, false);
        on(sky, SkymapPanel.Layer.DEGREES, true);
        BufferedImage foldedWith = stable(chart);
        on(sky, SkymapPanel.Layer.DEGREES, false);
        BufferedImage foldedWithout = stable(chart);
        double[] back = inkRadii(foldedWith, foldedWithout, cx, cy, decanOuter + 2, outer + 3);
        System.out.printf("  mansions folded: outer scale ink reaches %.1f px, rim %d%n",
            back[1], outer);
        ok("with the mansions folded the scale reaches the rim again: " + back[1],
            back[1] >= outer - 2.0);

        on(sky, SkymapPanel.Layer.DEGREES, true);
        on(sky, SkymapPanel.Layer.MANSIONS, true);
    }

    // ------------------------------------------------------------------ D

    private static void leaders(SkymapPanel sky, Component chart) throws Exception {
        on(sky, SkymapPanel.Layer.MANSIONS, true);
        on(sky, SkymapPanel.Layer.DEGREES, true);
        Object g = geometry(sky);
        int cx = intField(g, "cx");
        int cy = intField(g, "cy");
        int[] rings = ringsOf(sky);
        int mansionInner = rings[SkymapPanel.RING_MANSION_INNER];
        int degreeInner = rings[SkymapPanel.RING_DEGREE_INNER];
        int outer = rings[SkymapPanel.RING_OUTER];

        Settings.setShowDegreeLines(true);
        BufferedImage lines = stable(chart);
        Settings.setShowDegreeLines(false);
        BufferedImage none = stable(chart);
        double[] reach = inkRadii(lines, none, cx, cy, 0, outer + 3);
        System.out.printf("  leader ink reaches %.1f px; inner scale %d, outer scale %d%n",
            reach[1], degreeInner, mansionInner);
        ok("the leaders are drawn", reach[2] > 50);
        ok("the leaders run past the inner scale, where they used to stop: " + reach[1],
            reach[1] > degreeInner + 10);
        ok("the leaders reach the outer scale: " + reach[1] + " against " + mansionInner,
            reach[1] >= mansionInner - 3.0);
        ok("and stop there, not in the mansion band: " + reach[1],
            reach[1] <= mansionInner + 3.0);
        Settings.setShowDegreeLines(true);
    }

    // ------------------------------------------------------------------ E

    private static void globeFills(SkymapPanel sky) throws Exception {
        on(sky, SkymapPanel.Layer.MANSIONS, true);
        on(sky, SkymapPanel.Layer.DEGREES, true);
        on(sky, SkymapPanel.Layer.HOUSES, true);

        // The mansions carry a standing wash, so no focus is needed to see it.
        fill("mansions", Settings::setGlobeMansionFill, sky, () -> { });

        // The house and degree washes are on the item being pointed at.
        fill("house", Settings::setGlobeHouseFill, sky, () -> sky.setFocusHouse(5));
        sky.setFocusHouse(-1);
        fill("degree", Settings::setGlobeDegreeFill, sky, () -> sky.setFocusDegree(100));
        sky.setFocusDegree(-1);

        // <b>Off is not folded.</b> With the mansion fill off the band keeps its edges, its 28
        // divisions and its numbers; a switch that removed the ring would be the layer chip
        // again under another name.
        Settings.setGlobeMansionFill(false);
        BufferedImage unfilled = stableGlobe(sky);
        on(sky, SkymapPanel.Layer.MANSIONS, false);
        BufferedImage folded = stableGlobe(sky);
        on(sky, SkymapPanel.Layer.MANSIONS, true);
        Settings.setGlobeMansionFill(true);
        ok("the mansion band without its fill is still a band, not a folded layer",
            differing(unfilled, folded) > 500);
    }

    /** One fill: on and off differ, off removes ink, and on again restores the frame exactly. */
    private static void fill(String name, java.util.function.Consumer<Boolean> set,
                             SkymapPanel sky, Runnable focus) throws Exception {
        focus.run();
        set.accept(true);
        BufferedImage filled = stableGlobe(sky);
        set.accept(false);
        BufferedImage bare = stableGlobe(sky);
        set.accept(true);
        BufferedImage again = stableGlobe(sky);
        int diff = differing(filled, bare);
        System.out.printf("  %s fill: %d pixels change%n", name, diff);
        ok("the " + name + " fill changes the globe when switched off: " + diff, diff > 60);
        ok("switching the " + name + " fill off removes ink rather than moving it",
            ink(bare) < ink(filled));
        eq("switching the " + name + " fill back on restores the frame", 0,
            differing(filled, again));
    }

    // ------------------------------------------------------------------ F

    private static void controls(OuraniaWindow window) throws Exception {
        String[] keys = {Settings.GLOBE_HOUSE_FILL_KEY, Settings.GLOBE_DEGREE_FILL_KEY,
                         Settings.GLOBE_MANSION_FILL_KEY};
        for (String key : keys) {
            Settings.update(p -> p.remove(key));
        }
        ok("a fresh install fills the house", Settings.globeHouseFill());
        ok("a fresh install fills the degree", Settings.globeDegreeFill());
        ok("a fresh install fills the mansions", Settings.globeMansionFill());

        Settings.setGlobeHouseFill(false);
        ok("the house fill saves off", !Settings.globeHouseFill());
        Settings.setGlobeDegreeFill(false);
        ok("the degree fill saves off", !Settings.globeDegreeFill());
        Settings.setGlobeMansionFill(false);
        ok("the mansion fill saves off", !Settings.globeMansionFill());
        Settings.setGlobeHouseFill(true);
        Settings.setGlobeDegreeFill(true);
        Settings.setGlobeMansionFill(true);

        final SettingsPanel[] panel = new SettingsPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new SettingsPanel(window));
        String[][] boxes = {
            {"On the globe, fill the current house", Settings.GLOBE_HOUSE_FILL_KEY},
            {"On the globe, fill the degree under the cursor", Settings.GLOBE_DEGREE_FILL_KEY},
            {"On the globe, fill the lunar mansion stations", Settings.GLOBE_MANSION_FILL_KEY},
        };
        for (String[] b : boxes) {
            final JCheckBox box = findBox(panel[0], b[0]);
            ok("Settings offers \"" + b[0] + "\"", box != null);
            if (box == null) {
                continue;
            }
            ok("\"" + b[0] + "\" opens ticked", box.isSelected());
            SwingUtilities.invokeAndWait(() -> box.setSelected(false));
            ok("unticking \"" + b[0] + "\" turns the fill off",
                "false".equals(Settings.get(b[1], "true")));
            SwingUtilities.invokeAndWait(() -> box.setSelected(true));
        }

        // The absence is the decision: the globe's decan band has no fill for a box to govern.
        boolean decanFill = false;
        for (JCheckBox box : allBoxes(panel[0])) {
            String t = box.getText().toLowerCase();
            if (t.contains("decan") && t.contains("fill")) {
                decanFill = true;
            }
        }
        ok("there is no decan fill box, because there is no decan fill", !decanFill);
    }

    // ------------------------------------------------------------------ G

    private static void sections(OuraniaWindow window) throws Exception {
        final SettingsPanel[] panel = new SettingsPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new SettingsPanel(window));

        int alls = 0;
        int nones = 0;
        for (JButton b : allButtons(panel[0])) {
            if ("All".equals(b.getText())) {
                alls++;
            }
            if ("None".equals(b.getText())) {
                nones++;
            }
        }
        int groups = Bodies.Group.values().length;
        eq("every section has its own All", groups, alls);
        eq("every section has its own None", groups, nones);

        for (final Bodies.Group group : Bodies.Group.values()) {
            // A known mixed start, so a section that moves another section shows up.
            boolean[] start = new boolean[Bodies.count()];
            for (int i = 0; i < start.length; i++) {
                start[i] = i % 2 == 0;
            }
            Settings.saveBodySelection(start);
            SwingUtilities.invokeAndWait(() -> panel[0] = new SettingsPanel(window));

            final JButton none = sectionButton(panel[0], "None", group);
            final JButton all = sectionButton(panel[0], "All", group);
            ok(group.title + " has a None button", none != null);
            ok(group.title + " has an All button", all != null);
            if (none == null || all == null) {
                continue;
            }

            SwingUtilities.invokeAndWait(none::doClick);
            boolean[] after = Settings.loadBodySelection();
            ok("None clears every point in " + group.title, allAre(after, group, false));
            ok("None in " + group.title + " leaves every other section alone",
                othersUnchanged(start, after, group));

            SwingUtilities.invokeAndWait(all::doClick);
            after = Settings.loadBodySelection();
            ok("All selects every point in " + group.title, allAre(after, group, true));
            ok("All in " + group.title + " leaves every other section alone",
                othersUnchanged(start, after, group));
        }
    }

    private static boolean allAre(boolean[] sel, Bodies.Group group, boolean on) {
        for (int i = 0; i < sel.length; i++) {
            if (Bodies.at(i).group == group && sel[i] != on) {
                return false;
            }
        }
        return true;
    }

    private static boolean othersUnchanged(boolean[] before, boolean[] after, Bodies.Group group) {
        for (int i = 0; i < before.length; i++) {
            if (Bodies.at(i).group != group && before[i] != after[i]) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ painting

    private static void on(SkymapPanel sky, SkymapPanel.Layer layer, boolean open)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> sky.setLayer(layer, open));
        Thread.sleep(80);
    }

    private static Object geometry(SkymapPanel sky) throws Exception {
        java.lang.reflect.Method m = SkymapPanel.class.getDeclaredMethod("geometry",
            int.class, int.class);
        m.setAccessible(true);
        return m.invoke(sky, W, H);
    }

    private static int[] ringsOf(SkymapPanel sky) throws Exception {
        Object g = geometry(sky);
        java.lang.reflect.Field f = g.getClass().getDeclaredField("rings");
        f.setAccessible(true);
        return (int[]) f.get(g);
    }

    private static int intField(Object o, String name) throws Exception {
        java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(o);
    }

    private static BufferedImage paint(Component chart) throws Exception {
        final BufferedImage[] out = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            chart.paint(g);
            g.dispose();
            out[0] = img;
        });
        return out[0];
    }

    /** Paints until two frames agree, so a frame is of a settled wheel rather than a bloom. */
    private static BufferedImage stable(Component chart) throws Exception {
        BufferedImage last = paint(chart);
        for (int i = 0; i < 60; i++) {
            Thread.sleep(25);
            BufferedImage next = paint(chart);
            if (differing(last, next) == 0) {
                return next;
            }
            last = next;
        }
        return last;
    }

    private static BufferedImage globe(SkymapPanel sky) {
        BufferedImage im = new BufferedImage(GLOBE, GLOBE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(10, 12, 16));
        g.fillRect(0, 0, GLOBE, GLOBE);
        GlobeRenderer.paint(g, new Globe(), GLOBE, GLOBE, sky, false);
        g.dispose();
        return im;
    }

    private static BufferedImage stableGlobe(SkymapPanel sky) throws Exception {
        BufferedImage last = globe(sky);
        for (int i = 0; i < 60; i++) {
            Thread.sleep(25);
            BufferedImage next = globe(sky);
            if (differing(last, next) == 0) {
                return next;
            }
            last = next;
        }
        return last;
    }

    /**
     * The radii at which two frames differ, within an annulus: {nearest, farthest, count}.
     *
     * Only pixels that differ in a visible way are counted - a change of a few levels is
     * antialiasing noise from something moving elsewhere, not ink.
     */
    private static double[] inkRadii(BufferedImage a, BufferedImage b, int cx, int cy,
                                     double from, double to) {
        double near = Double.MAX_VALUE;
        double far = 0.0;
        int n = 0;
        for (int y = 0; y < Math.min(a.getHeight(), b.getHeight()); y++) {
            for (int x = 0; x < Math.min(a.getWidth(), b.getWidth()); x++) {
                double r = Math.hypot(x - cx, y - cy);
                if (r < from || r > to) {
                    continue;
                }
                if (visiblyDifferent(a.getRGB(x, y), b.getRGB(x, y))) {
                    n++;
                    near = Math.min(near, r);
                    far = Math.max(far, r);
                }
            }
        }
        return new double[] {n == 0 ? 0.0 : near, far, n};
    }

    private static boolean visiblyDifferent(int p, int q) {
        int dr = Math.abs(((p >> 16) & 255) - ((q >> 16) & 255));
        int dg = Math.abs(((p >> 8) & 255) - ((q >> 8) & 255));
        int db = Math.abs((p & 255) - (q & 255));
        return dr + dg + db > 24;
    }

    private static int differing(BufferedImage a, BufferedImage b) {
        int n = 0;
        for (int y = 0; y < Math.min(a.getHeight(), b.getHeight()); y++) {
            for (int x = 0; x < Math.min(a.getWidth(), b.getWidth()); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Total brightness above the ground, as a measure of how much has been drawn. */
    private static long ink(BufferedImage im) {
        long sum = 0;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int p = im.getRGB(x, y);
                sum += ((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255);
            }
        }
        return sum;
    }

    // ------------------------------------------------------------------ components

    private static JCheckBox findBox(Container root, String text) {
        for (JCheckBox b : allBoxes(root)) {
            if (text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }

    private static List<JCheckBox> allBoxes(Container root) {
        List<JCheckBox> out = new ArrayList<>();
        walk(root, out, JCheckBox.class);
        return out;
    }

    private static List<JButton> allButtons(Container root) {
        List<JButton> out = new ArrayList<>();
        walk(root, out, JButton.class);
        return out;
    }

    /** A section's own button, found by the tooltip that names its section. */
    private static JButton sectionButton(Container root, String text, Bodies.Group group) {
        for (JButton b : allButtons(root)) {
            String tip = b.getToolTipText();
            if (text.equals(b.getText()) && tip != null && tip.endsWith(" in " + group.title)) {
                return b;
            }
        }
        return null;
    }

    private static <T> void walk(Container c, List<T> out, Class<T> type) {
        for (Component child : c.getComponents()) {
            if (type.isInstance(child)) {
                out.add(type.cast(child));
            }
            if (child instanceof Container) {
                walk((Container) child, out, type);
            }
        }
    }

    // ------------------------------------------------------------------ harness

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expect, Object got) {
        ok(label + ": expected " + expect + ", got " + got,
            expect == null ? got == null : expect.equals(got));
    }

    private RimAndFillsCheck() { }
}
