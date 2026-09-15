package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Declinations;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * The declination graph: every planet north or south of the equator, drawn.
 *
 * <p>Master list E: "Declination graph / declination wheel - parallel aspects invisible
 * without it." The Declinations table has listed the numbers and the contacts since D4, but a
 * parallel is a thing seen - two planets level with each other - and a list of pairs with orbs
 * does not show which planets stand together, how far the Moon has run past the Sun's reach, or
 * that the whole chart leans south.
 *
 * <p><b>The picture:</b> the equator across the middle, the obliquity dashed above and below it,
 * and the bands beyond shaded as out of bounds. Each planet is a stem from the equator to its
 * declination with its glyph at the tip. A parallel is a solid line joining two tips at the same
 * height; a contraparallel is a dashed line from one tip to the other planet's mirror point,
 * marked with a hollow ring, because the contact is with where the planet would be on the other
 * side.
 *
 * <p><b>Drawn to an image and set in the table's HTML.</b> The table is HTML in the reading
 * pane, which cannot draw; an image embedded there scrolls with the table, prints and copies
 * with it, and needs no second panel.
 */
final class DeclinationGraph {

    private DeclinationGraph() { }

    static final int WIDTH = 300;
    static final int HEIGHT = 380;
    static final int LEFT = 58;
    static final int RIGHT = 10;
    static final int TOP = 14;
    /** The strip under the plot that carries the legend. */
    static final int LEGEND = 28;

    static final Color GROUND = new Color(20, 24, 31);
    static final Color OUT_OF_BOUNDS = new Color(226, 88, 88, 46);
    static final Color OOB_RING = new Color(236, 96, 96);
    static final Color PARALLEL = new Color(226, 178, 88);
    static final Color CONTRA = new Color(80, 200, 220);
    static final Color NORTH = new Color(127, 179, 255);
    static final Color SOUTH = new Color(200, 150, 255);

    /** Where everything goes, computed once for the painter and the check alike. */
    static final class Layout {
        final int w;
        final int h;
        /** Degrees from the equator to the top edge of the plot, and to the bottom. */
        final double reach;
        /** Pixels per degree of declination. */
        final double scale;
        final int equator;
        final int[] x;
        final int[] y;

        Layout(int w, int h, double reach, double scale, int equator, int n) {
            this.w = w;
            this.h = h;
            this.reach = reach;
            this.scale = scale;
            this.equator = equator;
            this.x = new int[n];
            this.y = new int[n];
        }

        /** The height of a declination, north up. */
        int yFor(double dec) {
            return (int) Math.round(this.equator - dec * this.scale);
        }
    }

    static Layout layout(Declinations.Result r, int w, int h) {
        double most = r.obliquity;
        for (Declinations.Entry e : r.entries) {
            most = Math.max(most, Math.abs(e.declination));
        }
        // Symmetric about the equator, with room past the furthest planet for its glyph.
        double reach = Math.ceil(most) + 3.0;
        int plot = h - TOP - LEGEND;
        double scale = plot / (2.0 * reach);
        int equator = TOP + (int) Math.round(reach * scale);
        Layout l = new Layout(w, h, reach, scale, equator, r.entries.size());
        int n = Math.max(1, r.entries.size());
        double column = (w - LEFT - RIGHT) / (double) n;
        for (int i = 0; i < r.entries.size(); i++) {
            l.x[i] = LEFT + (int) Math.round((i + 0.5) * column);
            l.y[i] = l.yFor(r.entries.get(i).declination);
        }
        return l;
    }

    static BufferedImage render(Declinations.Result r) {
        return render(r, WIDTH, HEIGHT);
    }

    /**
     * The graph at a pixel scale: laid out at its own size and drawn that many times over, so on
     * a 3x screen it has three real pixels for each of Swing's and is as sharp as the text beside
     * it. See HiDpi - drawn at 1x, the reading pane stretched it and it came out soft.
     */
    static BufferedImage render(Declinations.Result r, double scale) {
        int w = (int) Math.round(WIDTH * scale);
        int h = (int) Math.round(HEIGHT * scale);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        draw(g, r, WIDTH, HEIGHT);
        g.dispose();
        return img;
    }

    static BufferedImage render(Declinations.Result r, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        draw(g, r, w, h);
        g.dispose();
        return img;
    }

    private static void draw(Graphics2D g, Declinations.Result r, int w, int h) {
        Layout l = layout(r, w, h);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(GROUND);
        g.fillRect(0, 0, w, h);

        int plotBottom = h - LEGEND;
        int north = l.yFor(r.obliquity);
        int south = l.yFor(-r.obliquity);
        // Out of bounds: beyond the Sun's furthest reach, north and south.
        g.setColor(OUT_OF_BOUNDS);
        g.fillRect(LEFT, TOP, w - LEFT - RIGHT, Math.max(0, north - TOP));
        g.fillRect(LEFT, south, w - LEFT - RIGHT, Math.max(0, plotBottom - south));

        g.setFont(Theme.font("Segoe UI", Font.PLAIN, 10));
        FontMetrics small = g.getFontMetrics();
        // The equator, and the obliquity dashed either side of it.
        g.setColor(new Color(150, 160, 180));
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(LEFT, l.equator, w - RIGHT, l.equator);
        g.setColor(PARALLEL);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {4f, 4f}, 0f));
        g.drawLine(LEFT, north, w - RIGHT, north);
        g.drawLine(LEFT, south, w - RIGHT, south);
        g.setStroke(new BasicStroke(1f));
        // ChartTables.declination writes HTML entities for the table; the image needs the
        // characters themselves, or the axis reads "23&deg;26'".
        String bound = ChartTables.declination(r.obliquity).replace("&deg;", "°")
            .replace("&#39;", "'").replace("&prime;", "′").replace(" N", "").replace("N", "").trim();
        label(g, small, "0°", l.equator, new Color(150, 160, 180));
        label(g, small, bound + " N", north, PARALLEL);
        label(g, small, bound + " S", south, PARALLEL);

        // Contacts under the planets, so the glyphs sit on top of their lines - and the dashed
        // contraparallels before the solid parallels, so where the two cross the solid line
        // stays whole and the dash breaks, which is how a crossing reads anyway.
        for (boolean contraPass : new boolean[] {true, false}) {
            for (Declinations.Contact c : r.contacts) {
                int a = index(r, c.a);
                int b = index(r, c.b);
                if (a < 0 || b < 0 || c.contra != contraPass) {
                    continue;
                }
                if (!c.contra) {
                    g.setColor(PARALLEL);
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine(l.x[a], l.y[a], l.x[b], l.y[b]);
                } else {
                    int mirror = l.yFor(-r.entries.get(b).declination);
                    g.setColor(CONTRA);
                    g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {5f, 4f}, 0f));
                    g.drawLine(l.x[a], l.y[a], l.x[b], mirror);
                    g.setStroke(new BasicStroke(1.4f));
                    g.drawOval(l.x[b] - 5, mirror - 5, 10, 10);
                }
            }
        }
        g.setStroke(new BasicStroke(1f));

        Font glyphFont = new Font("SansSerif", Font.PLAIN, 13);
        for (int i = 0; i < r.entries.size(); i++) {
            Declinations.Entry e = r.entries.get(i);
            Color ink = e.declination >= 0 ? NORTH : SOUTH;
            g.setColor(ink);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(l.x[i], l.equator, l.x[i], l.y[i]);
            g.setStroke(new BasicStroke(1f));
            // An out-of-bounds bead is filled dark red as well as ringed: at this size a ring
            // alone is a hairline, and "past the Sun's reach" is the fact most worth seeing.
            g.setColor(e.outOfBounds ? new Color(96, 30, 34) : GROUND);
            g.fillOval(l.x[i] - 9, l.y[i] - 9, 18, 18);
            g.setColor(e.outOfBounds ? OOB_RING : ink);
            g.setStroke(new BasicStroke(e.outOfBounds ? 2.4f : 1.2f));
            g.drawOval(l.x[i] - 9, l.y[i] - 9, 18, 18);
            g.setStroke(new BasicStroke(1f));
            Bodies.Def d = Bodies.byName(e.name);
            String glyph = d == null || d.glyph == null || d.glyph.isEmpty() ? e.name.substring(0, 2) : d.glyph;
            g.setFont(glyphFont);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString(glyph, l.x[i] - fm.stringWidth(glyph) / 2, l.y[i] + (fm.getAscent() - fm.getDescent()) / 2);
        }

        // The legend.
        g.setFont(Theme.font("Segoe UI", Font.PLAIN, 10));
        int ly = h - LEGEND / 2 + 4;
        int lx = 8;
        g.setColor(PARALLEL);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(lx, ly - 4, lx + 18, ly - 4);
        g.setColor(new Color(200, 205, 215));
        g.drawString("parallel", lx + 22, ly);
        lx += 22 + small.stringWidth("parallel") + 12;
        g.setColor(CONTRA);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {5f, 4f}, 0f));
        g.drawLine(lx, ly - 4, lx + 18, ly - 4);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(200, 205, 215));
        g.drawString("contraparallel", lx + 22, ly);
        lx += 22 + small.stringWidth("contraparallel") + 12;
        g.setColor(new Color(226, 88, 88, 110));
        g.fillRect(lx, ly - 9, 14, 10);
        g.setColor(new Color(200, 205, 215));
        g.drawString("out of bounds", lx + 18, ly);
    }

    private static void label(Graphics2D g, FontMetrics fm, String text, int y, Color ink) {
        g.setColor(ink);
        g.drawString(text, LEFT - 6 - fm.stringWidth(text), y + (fm.getAscent() - fm.getDescent()) / 2);
    }

    static int index(Declinations.Result r, String name) {
        for (int i = 0; i < r.entries.size(); i++) {
            if (r.entries.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** Where the graph images are written: one folder, emptied of old graphs as new ones go in. */
    static File folder() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "ourania-graphs");
        dir.mkdirs();
        return dir;
    }

    private static int written;

    /**
     * The graph as an HTML img tag, its image written to a file.
     *
     * <b>A new file name every time.</b> Swing's HTML view caches images by URL, so rewriting
     * one file would show the first chart's graph under every later chart's table. The folder
     * keeps the last few and deletes the rest, so it does not grow for the life of the machine.
     */
    static String imgTag(Declinations.Result r) {
        return imgTag(r, HiDpi.scale(null));
    }

    /** At a given pixel scale; the tag keeps the graph's own size, so the pane lays it out alike. */
    static String imgTag(Declinations.Result r, double scale) {
        try {
            File dir = folder();
            File[] old = dir.listFiles((d, n) -> n.startsWith("declinations-") && n.endsWith(".png"));
            if (old != null && old.length > 8) {
                java.util.Arrays.sort(old, java.util.Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < old.length - 8; i++) {
                    old[i].delete();
                }
            }
            File out = new File(dir, "declinations-" + System.currentTimeMillis() + "-" + (written++) + ".png");
            javax.imageio.ImageIO.write(render(r, scale), "png", out);
            out.deleteOnExit();
            return "<img src='" + out.toURI() + "' width='" + WIDTH + "' height='" + HEIGHT
                + "' alt='Declination graph'>";
        } catch (java.io.IOException e) {
            return "<p><i>The declination graph could not be drawn: " + e.getMessage() + "</i></p>";
        }
    }
}
