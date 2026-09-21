package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Master list E6: zoom and pan on the flat wheel.
 *
 * <p>The defect this guards against is the one Geometry was built to end: a glyph drawn in one
 * place and clicked in another. So Part B does not call the hit tests. It sends real mouse
 * events to the chart panel at the point on screen where each body is drawn once zoomed and
 * panned, and asks what lit and what opened.
 */
public final class ZoomPanCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SkymapPanel sky = (SkymapPanel) field(w[0], "skymapPanel");
            Component chart = (Component) field(sky, "chartPanel");
            SwingUtilities.invokeAndWait(() -> chart.setSize(900, 820));

            part("A: the view is one invertible transform, clamped to the wheel", () -> view(sky, chart));
            part("B: zoomed and panned, each body lights and opens where it is drawn", () -> clicks(w[0], sky, chart));
            part("C: the wheel, the chip, and a saved image", () -> painting(sky, chart));
            part("D: the view is its own object, and needs no window", () -> theView(sky));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
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

    private static void view(SkymapPanel sky, Component chart) {
        int W = chart.getWidth();
        int H = chart.getHeight();
        ok("a new wheel is at fit", sky.viewIsFit());
        ok("at fit a point is its own wheel point", sky.toWheel(123, 456).equals(new Point(123, 456)));

        java.util.Random rnd = new java.util.Random(6);
        double worstInverse = 0.0;
        double worstAnchor = 0.0;
        for (int trial = 0; trial < 300; trial++) {
            sky.fitView();
            int zx = rnd.nextInt(W);
            int zy = rnd.nextInt(H);
            // What is under the cursor before the zoom stays under it after, unless the clamp had
            // to move the view - so this is measured only where the clamp left the pan alone.
            Point before = sky.toWheel(zx, zy);
            sky.zoomAt(zx, zy, -(1 + rnd.nextInt(10)));
            Point after = sky.toWheel(zx, zy);
            double limX = (sky.view.zoom() - 1.0) * W / 2.0;
            double limY = (sky.view.zoom() - 1.0) * H / 2.0;
            if (Math.abs(sky.view.panX()) < limX - 1 && Math.abs(sky.view.panY()) < limY - 1) {
                worstAnchor = Math.max(worstAnchor, before.distance(after));
            }
            sky.panBy(rnd.nextInt(400) - 200, rnd.nextInt(400) - 200);
            java.awt.geom.AffineTransform t = sky.viewTransform(W, H);
            for (int k = 0; k < 5; k++) {
                int x = rnd.nextInt(W);
                int y = rnd.nextInt(H);
                Point p = sky.toWheel(x, y);
                Point2D back = t.transform(new Point2D.Double(p.x, p.y), null);
                // The wheel point is rounded to a pixel, so the round trip is good to one wheel
                // pixel - viewZoom screen pixels.
                worstInverse = Math.max(worstInverse, back.distance(x, y) / sky.view.zoom());
            }
            ok("zoom stays within 1x to 8x, trial " + trial, sky.view.zoom() >= 1.0 && sky.view.zoom() <= WheelView.MAX_ZOOM);
            ok("the pan never shows past the wheel's canvas, trial " + trial,
                Math.abs(sky.view.panX()) <= limX + 1e-9 && Math.abs(sky.view.panY()) <= limY + 1e-9);
        }
        ok("toWheel inverts the painter's transform, worst " + String.format("%.3f", worstInverse) + " wheel px",
            worstInverse <= 0.75);
        ok("zooming keeps the point under the cursor, worst " + String.format("%.2f", worstAnchor) + " px",
            worstAnchor <= 1.5);

        sky.fitView();
        sky.zoomAt(200, 200, -12);
        ok("twelve notches in stops at the ceiling, " + sky.view.zoom(), sky.view.zoom() == WheelView.MAX_ZOOM);
        sky.zoomAt(200, 200, 40);
        ok("zooming all the way out lands exactly on fit", sky.viewIsFit());
        sky.panBy(300, -300);
        ok("fit cannot be panned off centre", sky.viewIsFit());
        sky.zoomAt(450, 410, -4);
        ok("four notches is about double, " + String.format("%.2f", sky.view.zoom()),
            sky.view.zoom() > 1.9 && sky.view.zoom() < 2.1);
        sky.fitView();
        ok("fit is zoom 1 and no pan", sky.viewIsFit());
    }

    private static void clicks(OuraniaWindow w, SkymapPanel sky, Component chart) throws Exception {
        Method bodyAt = SkymapPanel.class.getDeclaredMethod("bodyAt", int.class, int.class);
        bodyAt.setAccessible(true);
        Method geometry = SkymapPanel.class.getDeclaredMethod("geometry");
        geometry.setAccessible(true);
        javax.swing.JEditorPane selection = (javax.swing.JEditorPane) field(w, "selectionPane");
        boolean[] bValid = (boolean[]) field(sky, "natalRing.valid");
        double[] bLon = (double[]) field(sky, "natalRing.lon");

        int W = chart.getWidth();
        int H = chart.getHeight();
        final int[] tested = {0};
        int lit = 0;
        int opened = 0;
        int dropped = 0;
        List<String> wrongLight = new ArrayList<>();
        List<String> wrongOpen = new ArrayList<>();
        // A view per body and per depth: zoomed in about the body's own region, then panned so it
        // is well away from where it sits at fit. Zooming about the body alone would leave its
        // screen point equal to its wheel point, which tests nothing.
        Object fitGeometry = geometry.invoke(sky);
        int[] fitRadii = (int[]) fitGeometry.getClass().getDeclaredMethod("natalRadii").invoke(fitGeometry);
        double fitPin = fitGeometry.getClass().getDeclaredField("pin").getDouble(fitGeometry);
        List<double[]> views = new ArrayList<>();
        for (int i = 0; i < Bodies.count(); i++) {
            if (!bValid[i]) {
                continue;
            }
            double a = Math.toRadians(180.0 + fitPin - bLon[i]);
            int bx = W / 2 + (int) (fitRadii[i] * Math.cos(a));
            int by = H / 2 + (int) (fitRadii[i] * Math.sin(a));
            views.add(new double[] {W / 2 + (bx - W / 2) * 0.8, H / 2 + (by - H / 2) * 0.8, -5, 70, -45, i});
            views.add(new double[] {W / 2 + (bx - W / 2) * 0.9, H / 2 + (by - H / 2) * 0.9, -9, -90, 60, i});
        }
        for (double[] v : views) {
            sky.fitView();
            sky.zoomAt((int) v[0], (int) v[1], v[2]);
            sky.panBy(v[3], v[4]);
            Object g = geometry.invoke(sky);
            int[] radii = (int[]) g.getClass().getDeclaredMethod("natalRadii").invoke(g);
            int cx = g.getClass().getDeclaredField("cx").getInt(g);
            int cy = g.getClass().getDeclaredField("cy").getInt(g);
            double pin = g.getClass().getDeclaredField("pin").getDouble(g);
            java.awt.geom.AffineTransform t = sky.viewTransform(W, H);
            for (int i = (int) v[5]; i <= (int) v[5]; i++) {
                double a = Math.toRadians(180.0 + pin - bLon[i]);
                int wx = cx + (int) (radii[i] * Math.cos(a));
                int wy = cy + (int) (radii[i] * Math.sin(a));
                // Only a body the wheel itself would pick at its own drawn position is a fair
                // question - crowded glyphs resolve to a neighbour at fit too.
                if ((int) bodyAt.invoke(sky, wx, wy) != i) {
                    continue;
                }
                Point2D s = t.transform(new Point2D.Double(wx, wy), null);
                int sx = (int) Math.round(s.getX());
                int sy = (int) Math.round(s.getY());
                if (sx < 2 || sy < 2 || sx > W - 3 || sy > H - 3 || SkymapPanel.fitChipBounds().contains(sx, sy)) {
                    dropped++;
                    continue;
                }
                tested[0]++;
                final int fx = sx;
                final int fy = sy;
                SwingUtilities.invokeAndWait(() -> send(chart, MouseEvent.MOUSE_MOVED, fx, fy, 0));
                if (sky.hoverBody == i) {
                    lit++;
                } else {
                    wrongLight.add(Bodies.at(i).name + " lit " + sky.hoverBody);
                }
                // What the same body opens with no zoom at all, asked of the click handler at its
                // wheel position - the page a zoomed click has to reproduce exactly.
                final Method handle = SkymapPanel.class.getDeclaredMethod("handleChartClick", int.class, int.class);
                handle.setAccessible(true);
                final String[] expected = new String[1];
                final int fwx = wx;
                final int fwy = wy;
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        selection.setText("<html><body>zp-nothing-opened</body></html>");
                        handle.invoke(sky, fwx, fwy);
                        expected[0] = selection.getText();
                        selection.setText("<html><body>zp-nothing-opened</body></html>");
                        send(chart, MouseEvent.MOUSE_PRESSED, fx, fy, 1);
                        send(chart, MouseEvent.MOUSE_RELEASED, fx, fy, 1);
                        send(chart, MouseEvent.MOUSE_CLICKED, fx, fy, 1);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                SwingUtilities.invokeAndWait(() -> { });
                String shown = selection.getText();
                // The reset page carries a marker no reading contains - "none" did, in the Ascendant's
                // prose. Compared with whitespace ignored: Swing re-serialises the document, and the break it
                // writes after </body> is not the same on every render.
                if (!shown.contains("zp-nothing-opened") && squash(shown).equals(squash(expected[0]))) {
                    opened++;
                } else {
                    wrongOpen.add(Bodies.at(i).name);

                }
                // The tooltip is asked at the screen point too.
                String tip = ((javax.swing.JComponent) chart).getToolTipText(
                    new MouseEvent(chart, MouseEvent.MOUSE_MOVED, 0, 0, fx, fy, 0, false));
                ok(Bodies.at(i).name + " has its hover card at its zoomed position", tip != null && !tip.isEmpty());
            }
        }
        ok("enough views put a body on screen to mean something, " + tested[0]
            + " tested, " + dropped + " off screen", tested[0] >= 20);
        ok("every tested body lit where it is drawn, " + lit + " of " + tested[0] + " " + wrongLight,
            lit == tested[0]);
        ok("every tested body opened where it is drawn, " + opened + " of " + tested[0] + " " + wrongOpen,
            opened == tested[0]);

        // A drag pans, and the click that ends it selects nothing.
        sky.fitView();
        sky.zoomAt(450, 410, -6);
        double panX = sky.view.panX();
        SwingUtilities.invokeAndWait(() -> {
            selection.setText("<html><body>zp-nothing-opened</body></html>");
            send(chart, MouseEvent.MOUSE_PRESSED, 400, 400, 1);
            send(chart, MouseEvent.MOUSE_DRAGGED, 430, 400, 1);
            send(chart, MouseEvent.MOUSE_DRAGGED, 470, 410, 1);
            send(chart, MouseEvent.MOUSE_RELEASED, 470, 410, 1);
            send(chart, MouseEvent.MOUSE_CLICKED, 470, 410, 1);
        });
        ok("a drag on the zoomed wheel pans by the distance dragged, "
            + String.format("%.0f", sky.view.panX() - panX), Math.abs(sky.view.panX() - panX - 70) < 1.5);
        ok("and the click that ends the drag opens nothing", selection.getText().contains("zp-nothing-opened"));
        // At fit, a drag is still nothing at all.
        sky.fitView();
        SwingUtilities.invokeAndWait(() -> {
            send(chart, MouseEvent.MOUSE_PRESSED, 400, 400, 1);
            send(chart, MouseEvent.MOUSE_DRAGGED, 480, 400, 1);
            send(chart, MouseEvent.MOUSE_RELEASED, 480, 400, 1);
        });
        ok("at fit a drag leaves the view where it is", sky.viewIsFit());

        // The wheel, through its real listener.
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(new MouseWheelEvent(chart,
            MouseEvent.MOUSE_WHEEL, 0, 0, 300, 300, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -3)));
        ok("the mouse wheel zooms the flat wheel, " + String.format("%.2f", sky.view.zoom()), sky.view.zoom() > 1.5);

        // The chip puts it back; so does a double-click.
        java.awt.Rectangle chip = SkymapPanel.fitChipBounds();
        SwingUtilities.invokeAndWait(() -> {
            send(chart, MouseEvent.MOUSE_PRESSED, chip.x + 20, chip.y + 10, 1);
            send(chart, MouseEvent.MOUSE_RELEASED, chip.x + 20, chip.y + 10, 1);
            send(chart, MouseEvent.MOUSE_CLICKED, chip.x + 20, chip.y + 10, 1);
        });
        ok("the Fit chip returns the wheel to fit", sky.viewIsFit());
        sky.zoomAt(450, 410, -6);
        SwingUtilities.invokeAndWait(() -> {
            send(chart, MouseEvent.MOUSE_PRESSED, 600, 600, 1);
            send(chart, MouseEvent.MOUSE_RELEASED, 600, 600, 1);
            send(chart, MouseEvent.MOUSE_CLICKED, 600, 600, 1);
            send(chart, MouseEvent.MOUSE_PRESSED, 600, 600, 2);
            send(chart, MouseEvent.MOUSE_RELEASED, 600, 600, 2);
            send(chart, MouseEvent.MOUSE_CLICKED, 600, 600, 2);
        });
        ok("a double-click returns the wheel to fit", sky.viewIsFit());
    }

    private static void painting(SkymapPanel sky, Component chart) throws Exception {
        int W = chart.getWidth();
        int H = chart.getHeight();
        // Painted the way Swing paints the panel on screen - no export mark.
        sky.fitView();
        BufferedImage fitScreen = screenPaint(chart, W, H);
        sky.zoomAt(W / 2, H / 2, -8);
        BufferedImage zoomScreen = screenPaint(chart, W, H);
        ok("on screen the zoomed wheel is drawn differently from fit", differing(fitScreen, zoomScreen) > W * H / 20);
        // The disc's rim at fit sits near the edge of the shorter side; zoomed about the centre
        // it is off the panel, so the corners of a zoomed view are inside the wheel's area.
        ok("the chip is drawn while zoomed", !sameRegion(fitScreen, zoomScreen, SkymapPanel.fitChipBounds()));

        // A saved image is the whole chart whatever the screen is zoomed to.
        BufferedImage zoomedExport = ChartExporter.render(chart, 1);
        sky.fitView();
        BufferedImage fitExport = ChartExporter.render(chart, 1);
        int diff = differing(zoomedExport, fitExport);
        ok("a saved image ignores the screen's zoom, " + diff + " pixels differ", diff < W * H / 500);
        ok("and has no chip", sameRegion(zoomedExport, fitExport, SkymapPanel.fitChipBounds()));
    }

    private static BufferedImage screenPaint(Component chart, int w, int h) throws Exception {
        final BufferedImage[] out = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            chart.paint(g);
            g.dispose();
            out[0] = img;
        });
        return out[0];
    }

    private static String squash(String html) {
        return html.replaceAll("\\s+", "");
    }

    private static int differing(BufferedImage a, BufferedImage b) {
        int n = 0;
        for (int y = 0; y < Math.min(a.getHeight(), b.getHeight()); y += 2) {
            for (int x = 0; x < Math.min(a.getWidth(), b.getWidth()); x += 2) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n += 4;
                }
            }
        }
        return n;
    }

    private static boolean sameRegion(BufferedImage a, BufferedImage b, java.awt.Rectangle r) {
        for (int y = r.y + 4; y < r.y + r.height - 4; y++) {
            for (int x = r.x + 4; x < r.x + r.width - 4; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * J13, step 2: the zoom and pan are a {@link WheelView}, and the panel keeps no copy.
     *
     * <b>Two things worth pinning, and one of them is the reason for the move.</b> The view
     * takes the panel's size as an argument rather than reading it off a component, so it can
     * be asked what it would do at any size with no window open at all - which is what the
     * whole of this part does, on a bare object. And the panel must hold no zoom or pan state
     * of its own: a refactor that leaves the old fields behind "for now" is how one fact comes
     * to be kept in two places, which is the defect J13 is unpicking rather than an acceptable
     * step on the way.
     */
    private static void theView(SkymapPanel sky) throws Exception {
        // <b>No second copy on the panel.</b> Read by reflection over every field it declares,
        // so a field added later under any name is caught by its type and its use, not by this
        // check happening to know what it was called.
        java.util.List<String> strays = new java.util.ArrayList<>();
        for (java.lang.reflect.Field f : SkymapPanel.class.getDeclaredFields()) {
            String n = f.getName().toLowerCase();
            boolean looksLikeView = n.startsWith("viewzoom") || n.startsWith("viewpan")
                || n.equals("zoom") || n.equals("panx") || n.equals("pany");
            if (looksLikeView) {
                strays.add(f.getName());
            }
        }
        ok("the panel keeps no zoom or pan state of its own" + (strays.isEmpty() ? "" : ": " + strays),
            strays.isEmpty());
        ok("it holds a view instead", field(sky, "view") instanceof WheelView);

        // ---- and the view answers without a window
        WheelView v = new WheelView();
        ok("a fresh view is fitted", v.isFit() && v.zoom() == 1.0
            && v.panX() == 0.0 && v.panY() == 0.0);

        // <b>The inverse, round-tripped at several sizes and zooms.</b> This is what the two
        // copies of the unprojection used to risk: transform a wheel point to the screen, ask
        // the view where that screen point is in the wheel, and land back where you began.
        double worst = 0.0;
        int[][] sizes = {{900, 820}, {640, 480}, {1400, 300}, {301, 1103}};
        for (int[] wh : sizes) {
            for (double notches : new double[] {-1, -3, -6, -12}) {
                WheelView t = new WheelView();
                t.zoomAt(wh[0] / 3, wh[1] / 4, notches, wh[0], wh[1]);
                java.awt.geom.AffineTransform at = t.transform(wh[0], wh[1]);
                for (int[] pt : new int[][] {{0, 0}, {wh[0] / 2, wh[1] / 2}, {wh[0] - 1, wh[1] - 1}}) {
                    java.awt.geom.Point2D screen =
                        at.transform(new java.awt.Point(pt[0], pt[1]), null);
                    java.awt.Point back = t.toWheel((int) Math.round(screen.getX()),
                        (int) Math.round(screen.getY()), wh[0], wh[1]);
                    worst = Math.max(worst, back.distance(pt[0], pt[1]));
                }
            }
        }
        ok(String.format("the transform and its inverse agree at every size and zoom "
            + "(worst %.2f px)", worst), worst <= 1.5);

        // ---- the bounds, on a bare object
        WheelView deep = new WheelView();
        deep.zoomAt(100, 100, -40, 800, 600);
        ok("zooming in stops at the ceiling: " + deep.zoom(), deep.zoom() == WheelView.MAX_ZOOM);
        deep.zoomAt(100, 100, 40, 800, 600);
        ok("and zooming out lands back on fit", deep.isFit());

        WheelView slid = new WheelView();
        slid.zoomAt(400, 300, -4, 800, 600);
        slid.panBy(100000, 100000, 800, 600);
        double limX = (slid.zoom() - 1.0) * 800 / 2.0;
        ok("a pan cannot run past the wheel's edge: " + (int) slid.panX() + " of " + (int) limX,
            Math.abs(slid.panX() - limX) < 1e-9);

        // <b>Re-clamped at the size being painted.</b> The window made smaller while zoomed is
        // the case this exists for: the pan was measured against the old width and now points
        // past the edge, and the reader would see background where the chart should be.
        double wide = slid.panX();
        slid.reclamp(400, 300);
        ok("shrinking the window pulls the pan back inside: " + (int) wide + " to "
            + (int) slid.panX(), Math.abs(slid.panX()) < Math.abs(wide));

        // <b>The invariant behind the clamp's last line, asserted as behaviour.</b> "Not zoomed
        // means centred" cannot be reached through the zoom floor, so it is tested by what a
        // reader can actually do: drag hard at fit, and go nowhere.
        WheelView atFit = new WheelView();
        atFit.panBy(500, -400, 800, 600);
        ok("panning while fitted moves nothing", atFit.isFit());

        WheelView flag = new WheelView();
        ok("a fresh view has not been panned", !flag.panned());
        flag.panned(true);
        ok("and remembers when it has", flag.panned());
    }

    private static void send(Component c, int id, int x, int y, int clicks) {
        int mods = id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.BUTTON1_DOWN_MASK : 0;
        c.dispatchEvent(new MouseEvent(c, id, System.currentTimeMillis(), mods, x, y, clicks, false,
            id == MouseEvent.MOUSE_MOVED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
    }

    private static Object field(Object o, String name) throws Exception {
        return CheckReflect.get(o, name);
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
