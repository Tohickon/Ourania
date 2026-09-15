package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Declinations;
import com.zodiacomputing.ourania.astro.Ephemeris;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The declination graph: master list E, "parallel aspects invisible without it".
 *
 * <p>The graph is checked against the numbers it draws, over real skies: a search through two
 * decades of dates finds charts with out-of-bounds planets, parallels and contraparallels, so
 * every assertion below is tested on something real rather than on a chart built to pass.
 */
public final class DeclinationGraphCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        List<Declinations.Result> sample = new ArrayList<>();
        Declinations.Result showcase = null;
        int showcaseScore = -1;
        // A chart every 23 days and 7 hours from 1998 through 2017, which crosses a major lunar
        // standstill (2006) where the Moon runs well out of bounds.
        double start = SweDate.getJulDay(1998, 1, 1, 0.0);
        for (int k = 0; k < 320; k++) {
            double jd = start + k * 23.3;
            ChartFrame f = ChartFrame.compute(sw, jd, 40.0, -75.0, 'P', false, 0.0);
            Declinations.Result r = Declinations.of(f);
            sample.add(r);
            int score = r.outOfBounds().size() * 3 + r.contacts.size()
                + (int) r.contacts.stream().filter(c -> c.contra).count() * 2;
            if (score > showcaseScore) {
                showcaseScore = score;
                showcase = r;
            }
        }

        part("A: every planet is drawn at its declination", () -> geometry(sample));
        part("B: the picture carries the contacts and the bounds", () -> picture(sample));
        final Declinations.Result show = showcase;
        part("C: the table carries the graph, and Swing shows it", () -> table(show));

        BufferedImage img = DeclinationGraph.render(showcase);
        java.io.File out = new java.io.File(System.getProperty("java.io.tmpdir"), "declination-graph-sample.png");
        javax.imageio.ImageIO.write(img, "png", out);
        System.out.println("  sample written to " + out);

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

    private static void geometry(List<Declinations.Result> sample) {
        int W = DeclinationGraph.WIDTH;
        int H = DeclinationGraph.HEIGHT;
        int outside = 0;
        int misplaced = 0;
        int disordered = 0;
        int parallelsApart = 0;
        int parallels = 0;
        for (Declinations.Result r : sample) {
            DeclinationGraph.Layout l = DeclinationGraph.layout(r, W, H);
            for (int i = 0; i < r.entries.size(); i++) {
                double dec = r.entries.get(i).declination;
                if (l.x[i] - 9 < DeclinationGraph.LEFT - 2 || l.x[i] + 9 > W || l.y[i] - 9 < 0
                        || l.y[i] + 9 > H - DeclinationGraph.LEGEND) {
                    outside++;
                }
                if (Math.abs(Math.abs(l.y[i] - l.equator) - Math.abs(dec) * l.scale) > 1.0
                        || (dec * l.scale >= 1.0 && l.y[i] >= l.equator)
                        || (dec * l.scale <= -1.0 && l.y[i] <= l.equator)) {
                    misplaced++;
                }
                for (int j = 0; j < r.entries.size(); j++) {
                    if (r.entries.get(j).declination > dec + 0.2 && l.y[j] >= l.y[i]) {
                        disordered++;
                    }
                }
            }
            for (Declinations.Contact c : r.contacts) {
                if (c.contra) {
                    continue;
                }
                parallels++;
                int a = DeclinationGraph.index(r, c.a);
                int b = DeclinationGraph.index(r, c.b);
                if (Math.abs(l.y[a] - l.y[b]) > Declinations.parallelOrb * l.scale + 1.0) {
                    parallelsApart++;
                }
            }
            ok("the columns are spread across the plot, left to right",
                r.entries.size() < 2 || l.x[r.entries.size() - 1] > l.x[0]);
        }
        ok("every glyph lies inside the plot, " + outside + " outside", outside == 0);
        ok("every planet stands its declination times the scale from the equator, north up, " + misplaced + " off", misplaced == 0);
        ok("a planet further north is always drawn higher, " + disordered + " inversions", disordered == 0);
        ok("parallel planets are drawn level, within the orb, " + parallelsApart + " of " + parallels + " apart",
            parallelsApart == 0);
    }

    private static void picture(List<Declinations.Result> sample) {
        int oob = 0;
        int oobRed = 0;
        int par = 0;
        int parInk = 0;
        int contra = 0;
        int contraRing = 0;
        int bandsShaded = 0;
        int images = 0;
        for (Declinations.Result r : sample) {
            if (r.outOfBounds().isEmpty() && r.contacts.isEmpty()) {
                continue;
            }
            BufferedImage img = DeclinationGraph.render(r);
            DeclinationGraph.Layout l = DeclinationGraph.layout(r, img.getWidth(), img.getHeight());
            images++;
            int ground = DeclinationGraph.GROUND.getRGB() & 0xffffff;
            // Shading beyond the bound, sampled at the plot's right edge above the northern bound.
            int north = l.yFor(r.obliquity);
            if (north - 4 > DeclinationGraph.TOP) {
                int px = img.getRGB(img.getWidth() - DeclinationGraph.RIGHT - 3, north - 4) & 0xffffff;
                if (px != ground && ((px >> 16) & 0xff) > ((px >> 8) & 0xff)) {
                    bandsShaded++;
                }
            }
            for (int i = 0; i < r.entries.size(); i++) {
                if (!r.entries.get(i).outOfBounds) {
                    continue;
                }
                oob++;
                // The ring, on its right-hand side.
                int px = img.getRGB(l.x[i] + 9, l.y[i]) & 0xffffff;
                int red = (px >> 16) & 0xff;
                int green = (px >> 8) & 0xff;
                if (red > 150 && red > green + 60) {
                    oobRed++;
                }
            }
            for (Declinations.Contact c : r.contacts) {
                int a = DeclinationGraph.index(r, c.a);
                int b = DeclinationGraph.index(r, c.b);
                if (Math.abs(l.x[a] - l.x[b]) < 40) {
                    continue;                // neighbours: the line hides under the two beads
                }
                if (!c.contra) {
                    // Sampled along the line, away from every bead, since another planet's bead
                    // can sit on the line between the two and cover a single sample point.
                    int open = 0;
                    int inked = 0;
                    for (int t = 1; t < 10; t++) {
                        int px = l.x[a] + (l.x[b] - l.x[a]) * t / 10;
                        int py = l.y[a] + (l.y[b] - l.y[a]) * t / 10;
                        if (underBead(l, px, py, 12) || onStem(l, px, py)) {
                            continue;
                        }
                        open++;
                        if (nearInk(img, px, py, DeclinationGraph.PARALLEL)) {
                            inked++;
                        }
                    }
                    if (open == 0) {
                        continue;
                    }
                    par++;
                    if (inked == open) {
                        parInk++;
                    }
                } else {
                    int mirror = l.yFor(-r.entries.get(b).declination);
                    if (underBead(l, l.x[b], mirror, 16)) {
                        continue;            // the mirror point is under a bead of its own
                    }
                    contra++;
                    if (nearInk(img, l.x[b] + 5, mirror, DeclinationGraph.CONTRA)
                            || nearInk(img, l.x[b] - 5, mirror, DeclinationGraph.CONTRA)) {
                        contraRing++;
                    }
                }
            }
        }
        ok("the sample reaches every case: " + oob + " out of bounds, " + par + " parallels, " + contra
            + " contraparallels, over " + images + " charts", oob >= 5 && par >= 5 && contra >= 3);
        ok("every out-of-bounds planet has a red ring, " + oobRed + " of " + oob, oobRed == oob);
        ok("the band past the bound is shaded, " + bandsShaded + " of " + images, bandsShaded == images);
        ok("every parallel has its line between the two planets, " + parInk + " of " + par, parInk == par);
        ok("every contraparallel has its ring at the other planet's mirror point, " + contraRing + " of " + contra,
            contraRing == contra);
    }

    /** Whether (x, y) lies on another planet's stem, which is drawn over the contact lines. */
    private static boolean onStem(DeclinationGraph.Layout l, int x, int y) {
        for (int i = 0; i < l.x.length; i++) {
            int lo = Math.min(l.equator, l.y[i]);
            int hi = Math.max(l.equator, l.y[i]);
            if (Math.abs(x - l.x[i]) <= 4 && y >= lo - 2 && y <= hi + 2) {
                return true;
            }
        }
        return false;
    }

    /** Whether (x, y) lies on or next to any planet's bead. */
    private static boolean underBead(DeclinationGraph.Layout l, int x, int y, int radius) {
        for (int i = 0; i < l.x.length; i++) {
            if (Math.hypot(x - l.x[i], y - l.y[i]) <= radius) {
                return true;
            }
        }
        return false;
    }

    /** Whether a colour close to this one is within two pixels of (x, y). */
    private static boolean nearInk(BufferedImage img, int x, int y, java.awt.Color c) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= img.getWidth() || py >= img.getHeight()) {
                    continue;
                }
                int v = img.getRGB(px, py);
                int dr = Math.abs(((v >> 16) & 0xff) - c.getRed());
                int dg = Math.abs(((v >> 8) & 0xff) - c.getGreen());
                int db = Math.abs((v & 0xff) - c.getBlue());
                if (dr + dg + db < 90) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void table(Declinations.Result showcase) throws Exception {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        ChartFrame f = ChartFrame.compute(sw, SweDate.getJulDay(2006, 3, 20, 12.0), 40.0, -75.0, 'P', false, 0.0);
        String html = ChartTables.declinations(f);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<img src='([^']+)'").matcher(html);
        ok("the Declinations table carries the graph", m.find());
        if (!m.find(0)) {
            return;
        }
        java.io.File file = new java.io.File(new java.net.URI(m.group(1)));
        ok("its image file exists", file.isFile());
        BufferedImage read = javax.imageio.ImageIO.read(file);
        ok("and is the graph's size", read != null && read.getWidth() == DeclinationGraph.WIDTH
            && read.getHeight() == DeclinationGraph.HEIGHT);
        ok("the graph comes before the table it draws", html.indexOf("<img") < html.indexOf("<table"));

        String second = ChartTables.declinations(f);
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("<img src='([^']+)'").matcher(second);
        ok("a second table gets a new file, so Swing's image cache cannot show the old chart",
            m2.find() && !m2.group(1).equals(m.group(1)));
        for (int i = 0; i < 12; i++) {
            ChartTables.declinations(f);
        }
        java.io.File[] kept = DeclinationGraph.folder().listFiles((d, n) -> n.startsWith("declinations-"));
        ok("old graphs are cleared away, " + (kept == null ? 0 : kept.length) + " kept", kept != null && kept.length <= 9);

        // Swing lays the image out in the pane at its size.
        final int[] height = new int[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
            pane.setContentType("text/html");
            pane.getDocument().putProperty("LoadsSynchronously", Boolean.TRUE);
            ((javax.swing.text.html.HTMLDocument) pane.getDocument()).putProperty("LoadsSynchronously", Boolean.TRUE);
            pane.setText("<html><body><p>above</p><p>" + DeclinationGraph.imgTag(showcase) + "</p><p>below</p></body></html>");
            pane.setSize(360, 10);
            height[0] = pane.getPreferredSize().height;
        });
        ok("the reading pane makes room for the whole graph, " + height[0] + " px tall", height[0] >= DeclinationGraph.HEIGHT);
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
