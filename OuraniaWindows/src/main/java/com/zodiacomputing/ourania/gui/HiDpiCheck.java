package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Declinations;
import com.zodiacomputing.ourania.astro.Ephemeris;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Master list E: "Anti-aliased high-DPI rendering - HiDPI scaling on 4K displays unverified".
 *
 * <p><b>Sharp and stretched are told apart by measurement, not by eye.</b> An image drawn at 1x
 * and stretched three times is made of 3-by-3 blocks of one colour; the same picture drawn at 3x
 * has a curve or a diagonal changing colour within a block. So a surface is counted sharp when the
 * share of its inked 3-by-3 blocks that are not uniform is well above what stretching gives. Every
 * surface the reader looks at on a scaled screen is measured that way: the wheel, the globe, the
 * dial, the declination graph and a saved image.
 */
public final class HiDpiCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;
    private static final int S = 3;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: the screen's scale, and the choices it makes", HiDpiCheck::scale);
        part("B: the surfaces Swing draws are sharp at 3x", HiDpiCheck::surfaces);
        part("C: the images the app makes itself are drawn at the screen's scale", HiDpiCheck::images);

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

    private static void scale() {
        double here = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
            .getDefaultConfiguration().getDefaultTransform().getScaleX();
        System.out.println("  this screen: " + here + "x, "
            + java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth()
            + " px wide");
        ok("HiDpi reads this screen's own scale, " + HiDpi.scale(null), HiDpi.scale(null) == HiDpi.round(here));
        ok("a scale rounds to a quarter and never falls below 1",
            HiDpi.round(1.2499) == 1.25 && HiDpi.round(0.8) == 1.0 && HiDpi.round(Double.NaN) == 1.0 && HiDpi.round(2.0) == 2.0);
        eq("labels", "3x 1.5x", HiDpi.label(3.0) + " " + HiDpi.label(1.5));
        eq("at 100% a saved image offers 1x, 2x and 4x", "[1.0, 2.0, 4.0]", java.util.Arrays.toString(ChartExporter.scales(1.0)));
        eq("at 150% it offers 1.5x first", "[1.5, 2.0, 4.0]", java.util.Arrays.toString(ChartExporter.scales(1.5)));
        eq("at 200% the screen and 2x are one choice", "[2.0, 4.0]", java.util.Arrays.toString(ChartExporter.scales(2.0)));
        eq("at 300% - this machine - the screen's 3x is among them", "[2.0, 3.0, 4.0]", java.util.Arrays.toString(ChartExporter.scales(3.0)));
    }

    private static void surfaces() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field f = OuraniaWindow.class.getDeclaredField("skymapPanel");
            f.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) f.get(w[0]);
            Component chart = sky.chartComponent();
            SwingUtilities.invokeAndWait(() -> chart.setSize(600, 560));

            double wheel = sharpness(paint(chart, S), S);
            double wheelStretched = sharpness(stretch(paint(chart, 1), S), S);
            report("the flat wheel", wheel, wheelStretched);

            SwingUtilities.invokeAndWait(() -> sky.setGlobeMode(true));
            double globe = sharpness(paint(chart, S), S);
            double globeStretched = sharpness(stretch(paint(chart, 1), S), S);
            report("the globe", globe, globeStretched);
            SwingUtilities.invokeAndWait(() -> sky.setGlobeMode(false));

            java.lang.reflect.Field fd = OuraniaWindow.class.getDeclaredField("dialPanel");
            fd.setAccessible(true);
            DialPanel dial = (DialPanel) fd.get(w[0]);
            SwingUtilities.invokeAndWait(() -> {
                dial.refreshChart();
                dial.canvas.setSize(520, 520);
            });
            double d = sharpness(paint(dial.canvas, S), S);
            double dStretched = sharpness(stretch(paint(dial.canvas, 1), S), S);
            report("the dial", d, dStretched);
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static void images() throws Exception {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        ChartFrame f = ChartFrame.compute(sw, SweDate.getJulDay(2006, 3, 20, 12.0), 40.0, -75.0, 'P', false, 0.0);
        Declinations.Result r = Declinations.of(f);

        BufferedImage graph = DeclinationGraph.render(r, (double) S);
        eq("the graph at 3x is three times its size", (DeclinationGraph.WIDTH * S) + "x" + (DeclinationGraph.HEIGHT * S),
            graph.getWidth() + "x" + graph.getHeight());
        report("the declination graph drawn at 3x", sharpness(graph, S),
            sharpness(stretch(DeclinationGraph.render(r, 1.0), S), S));

        // The tag keeps the graph's own size, so the pane lays out a 3x image in the same space.
        String tag = DeclinationGraph.imgTag(r, S);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<img src='([^']+)'").matcher(tag);
        ok("the tag names a file", m.find());
        BufferedImage file = javax.imageio.ImageIO.read(new java.io.File(new java.net.URI(m.group(1))));
        eq("the file holds the 3x image", graph.getWidth(), file.getWidth());
        final int[] height = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
            pane.setContentType("text/html");
            pane.getDocument().putProperty("LoadsSynchronously", Boolean.TRUE);
            pane.setText("<html><body>" + tag + "</body></html>");
            pane.setSize(400, 10);
            height[0] = pane.getPreferredSize().height;
        });
        ok("and the pane lays it out at the graph's size, not three times it, " + height[0] + " px",
            height[0] >= DeclinationGraph.HEIGHT && height[0] < DeclinationGraph.HEIGHT + 60);

        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            Component chart = ((SkymapPanel) fs.get(w[0])).chartComponent();
            SwingUtilities.invokeAndWait(() -> chart.setSize(500, 460));
            BufferedImage saved = ChartExporter.render(chart, (double) S);
            eq("a saved image at the screen's 3x is three times the wheel's size", "1500x1380",
                saved.getWidth() + "x" + saved.getHeight());
            report("a saved image at 3x", sharpness(saved, S), sharpness(stretch(ChartExporter.render(chart, 1.0), S), S));
            BufferedImage half = ChartExporter.render(chart, 1.5);
            eq("a fractional scale rounds to whole pixels", "750x690", half.getWidth() + "x" + half.getHeight());
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static void report(String what, double sharp, double stretched) {
        ok(what + " is drawn at the scale, not stretched: " + pct(sharp) + " of inked blocks vary, against "
            + pct(stretched) + " stretched", sharp > 0.2 && sharp > stretched * 3 + 0.05);
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }

    /** Paints a component into an image through a scale, the way Swing paints it on a scaled screen. */
    private static BufferedImage paint(Component c, int scale) throws Exception {
        final BufferedImage[] out = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage img = new BufferedImage(c.getWidth() * scale, c.getHeight() * scale, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.scale(scale, scale);
            c.paint(g);
            g.dispose();
            out[0] = img;
        });
        return out[0];
    }

    /** Every pixel made a block, which is what a stretched image is. */
    private static BufferedImage stretch(BufferedImage src, int scale) {
        BufferedImage out = new BufferedImage(src.getWidth() * scale, src.getHeight() * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, src.getRGB(x / scale, y / scale));
            }
        }
        return out;
    }

    /** Of the blocks that are not flat background, the share whose pixels are not all one colour. */
    private static double sharpness(BufferedImage img, int block) {
        int inked = 0;
        int varied = 0;
        for (int by = 0; by + block <= img.getHeight(); by += block) {
            for (int bx = 0; bx + block <= img.getWidth(); bx += block) {
                int first = img.getRGB(bx, by);
                boolean same = true;
                for (int y = by; y < by + block && same; y++) {
                    for (int x = bx; x < bx + block; x++) {
                        if (img.getRGB(x, y) != first) {
                            same = false;
                            break;
                        }
                    }
                }
                // A block is inked if it differs from its left neighbour block's first pixel, or varies:
                // flat ground on both sides says nothing either way.
                int left = bx >= block ? img.getRGB(bx - block, by) : first;
                if (!same || left != first) {
                    inked++;
                    if (!same) {
                        varied++;
                    }
                }
            }
        }
        return inked == 0 ? 0 : varied / (double) inked;
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

    private static void eq(String label, Object want, Object got) {
        ok(label + ": got " + got + ", expected " + want, want.equals(got));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
