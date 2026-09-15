package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Dial;
import com.zodiacomputing.ourania.astro.Ephemeris;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The dials: master list E, "Modulo-22.5 dial (16th harmonic) for semi-square / sesquiquadrate
 * family contacts".
 *
 * <p>Part A holds the arithmetic to facts that do not come from it: a square is one point on the
 * 90 dial and not on a finer one's terms alone, a semi-square joins on the 45, a 22.5 only on the
 * 22.5; and every planetary picture the engine reports is re-derived by brute force from raw
 * longitudes on real charts - the member within orb of some multiple of the modulus from the
 * focus - with none missing.
 */
public final class DialCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: the fold", DialCheck::fold);
        part("B: pictures, against brute force on real charts", DialCheck::pictures);
        part("C: the screen turns its pointer and says what is under it", DialCheck::screen);

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

    private static void fold() {
        near("a square is one point on the 90 dial", 0.0, Dial.separation(10.0, 100.0, 90.0));
        near("an opposition is one point on the 90 dial", 0.0, Dial.separation(10.0, 190.0, 90.0));
        near("a semi-square is half the 90 dial apart", 45.0, Dial.separation(10.0, 55.0, 90.0));
        near("a semi-square is one point on the 45 dial", 0.0, Dial.separation(10.0, 55.0, 45.0));
        near("a sesquiquadrate is one point on the 45 dial", 0.0, Dial.separation(10.0, 145.0, 45.0));
        near("a 22.5 is half the 45 dial apart", 22.5, Dial.separation(0.0, 22.5, 45.0));
        near("a 22.5 is one point on the 22.5 dial", 0.0, Dial.separation(0.0, 22.5, 22.5));
        near("a trine is not a hard aspect on the 90 dial", 30.0, Dial.separation(0.0, 120.0, 90.0));
        near("across Aries 0: 359 and 91 are 2 apart on the 90", 2.0, Dial.separation(359.0, 91.0, 90.0));
        near("the dial's angle is the fold scaled to a turn", 180.0, Dial.angle(45.0 + 360.0, 90.0));

        java.util.Random rnd = new java.util.Random(22);
        double worstSym = 0;
        double worstMid = 0;
        double worstNear = 0;
        int outOfRange = 0;
        for (int i = 0; i < 5000; i++) {
            double a = rnd.nextDouble() * 720 - 180;
            double b = rnd.nextDouble() * 720 - 180;
            for (double m : Dial.MODULI) {
                double s = Dial.separation(a, b, m);
                worstSym = Math.max(worstSym, Math.abs(s - Dial.separation(b, a, m)));
                if (s < 0 || s > m / 2 + 1e-9 || Dial.folded(a, m) < 0 || Dial.folded(a, m) >= m) {
                    outOfRange++;
                }
                Dial.Point x = Dial.point("x", a);
                Dial.Point y = Dial.point("y", b);
                Dial.Point mid = Dial.midpoint(x, y);
                // Either midpoint of a pair is the same place on every one of these dials.
                double far = mid.lon + 180.0;
                worstMid = Math.max(worstMid, Dial.separation(mid.lon, far, m));
                // And the one chosen is equidistant from both, by the shorter arc.
                double dx = com.zodiacomputing.ourania.astro.Aspects.separation(mid.lon, x.lon);
                double dy = com.zodiacomputing.ourania.astro.Aspects.separation(mid.lon, y.lon);
                worstNear = Math.max(worstNear, Math.abs(dx - dy));
            }
        }
        ok("separation is symmetric, worst " + worstSym, worstSym < 1e-9);
        ok("fold and separation stay in range, " + outOfRange + " out", outOfRange == 0);
        ok("a midpoint and its opposite are one point on every dial, worst " + worstMid, worstMid < 1e-9);
        ok("the midpoint is halfway by the shorter arc, worst " + worstNear, worstNear < 1e-9);
    }

    private static void pictures() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double orb = 1.0;
        int reported = 0;
        int wrong = 0;
        int missing = 0;
        int selfHalves = 0;
        int semisOnly45 = 0;
        for (int k = 0; k < 40; k++) {
            double jd = SweDate.getJulDay(1950, 1, 1, 0.0) + k * 491.7;
            ChartFrame f = ChartFrame.compute(sw, jd, 51.5, -0.1, 'P', false, 0.0);
            List<Dial.Point> points = Dial.points(f);
            List<Dial.Point> mids = Dial.midpoints(points);
            for (double m : Dial.MODULI) {
                List<Dial.Picture> pics = Dial.pictures(points, mids, m, orb);
                java.util.Set<String> engine = new java.util.HashSet<>();
                for (Dial.Picture pic : pics) {
                    for (Dial.Point p : pic.members) {
                        reported++;
                        engine.add(pic.focus.name + "=" + p.name);
                        if (!family(p.lon, pic.focus.lon, m, orb)) {
                            wrong++;
                        }
                        if (p.isMidpoint() && (pic.focus.name.equals(p.a) || pic.focus.name.equals(p.b))) {
                            selfHalves++;
                        }
                    }
                }
                // Brute force, from the raw longitudes and nothing of the engine's but the lists.
                for (Dial.Point focus : points) {
                    List<Dial.Point> all = new ArrayList<>(points);
                    all.addAll(mids);
                    for (Dial.Point p : all) {
                        if (p == focus || (p.isMidpoint() && (focus.name.equals(p.a) || focus.name.equals(p.b)))) {
                            continue;
                        }
                        if (family(p.lon, focus.lon, m, orb) && !engine.contains(focus.name + "=" + p.name)) {
                            missing++;
                        }
                    }
                }
                if (m == 45.0) {
                    for (Dial.Picture pic : pics) {
                        for (Dial.Point p : pic.members) {
                            if (!p.isMidpoint() && !family(p.lon, pic.focus.lon, 90.0, orb)) {
                                semisOnly45++;
                            }
                        }
                    }
                }
            }
        }
        ok("the 40 charts give the three dials something to find, " + reported + " members", reported > 500);
        ok("every member reported is within orb of a multiple of the modulus from its focus, " + wrong + " not", wrong == 0);
        ok("no member the brute force finds is missing, " + missing + " missing", missing == 0);
        ok("no point is pictured on a midpoint it is half of, " + selfHalves, selfHalves == 0);
        ok("the 45 dial finds contacts the 90 dial cannot - semi-squares and sesquiquadrates, " + semisOnly45,
            semisOnly45 > 0);
    }

    /** Within orb of some multiple of the modulus, computed from the raw zodiac separation. */
    private static boolean family(double lonA, double lonB, double modulus, double orb) {
        double sep = com.zodiacomputing.ourania.astro.Aspects.separation(lonA, lonB);
        for (double k = 0; k <= 180.0 / modulus + 1e-9; k++) {
            if (Math.abs(sep - k * modulus) <= orb + 1e-9) {
                return true;
            }
        }
        return false;
    }

    private static void screen() throws Exception {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        ChartFrame f = ChartFrame.compute(sw, SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0),
            39.9526, -75.1652, 'P', false, 0.0);
        final DialPanel[] holder = new DialPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new DialPanel(null);
            holder[0].setSize(1000, 700);
            holder[0].doLayout();
            holder[0].canvas.setSize(600, 600);
        });
        DialPanel d = holder[0];
        SwingUtilities.invokeAndWait(() -> d.setChart(null));
        ok("with no chart the list says so", d.list.getText().contains("no chart"));
        SwingUtilities.invokeAndWait(() -> d.setChart(f));
        ok("the chart's planets and angles are on the dial, " + d.points.size(), d.points.size() >= 12);
        ok("the derived half of an axis is not listed twice",
            d.points.stream().noneMatch(p -> p.name.equals("Descendant") || p.name.equals("IC") || p.name.equals("South Node")));
        eq("every pair has a midpoint", d.points.size() * (d.points.size() - 1) / 2, d.midpoints.size());

        for (int mi = 0; mi < Dial.MODULI.length; mi++) {
            final int fmi = mi;
            SwingUtilities.invokeAndWait(() -> d.dialChoice.setSelectedIndex(fmi));
            double m = Dial.MODULI[mi];
            int[] radii = d.canvas.glyphRadii();
            int snapped = 0;
            int listed = 0;
            for (int i = 0; i < d.points.size(); i++) {
                Dial.Point p = d.points.get(i);
                java.awt.Point g = d.canvas.at(Dial.angle(p.lon, m), radii[i]);
                SwingUtilities.invokeAndWait(() -> d.canvas.dispatchEvent(new MouseEvent(d.canvas,
                    MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, g.x + 2, g.y - 1, 1, false, MouseEvent.BUTTON1)));
                // A click on a glyph that another glyph overlaps may take the other; either way
                // the pointer must be exactly on a point, and that point must be listed.
                boolean exact = false;
                for (Dial.Point q : d.points) {
                    if (Math.abs(Dial.separation(q.lon, d.pointer, m)) < 1e-9) {
                        exact = true;
                    }
                }
                if (exact && Math.abs(Dial.separation(p.lon, d.pointer, m)) < 1e-9) {
                    snapped++;
                }
                if (d.onPointer().contains(p) || !Double.isNaN(d.pointer) && d.list.getText().contains(p.name)) {
                    listed++;
                }
            }
            ok(DialPanel.DIAL_NAMES[mi] + ": a click on each planet sets the pointer exactly on it, "
                + snapped + " of " + d.points.size(), snapped >= d.points.size() - 2);
            ok(DialPanel.DIAL_NAMES[mi] + ": and the list names what the pointer is on, " + listed + " of "
                + d.points.size(), listed == d.points.size());

            // A drag away from every glyph turns the pointer to the dial angle under the mouse.
            java.awt.Point spot = d.canvas.at(123.0, 40);
            SwingUtilities.invokeAndWait(() -> d.canvas.dispatchEvent(new MouseEvent(d.canvas,
                MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, spot.x, spot.y, 0, false,
                MouseEvent.BUTTON1)));
            ok(DialPanel.DIAL_NAMES[mi] + ": a drag turns the pointer to the angle under it, "
                + String.format("%.2f", d.pointer), Math.abs(d.pointer - 123.0 / 360.0 * m) < m / 90.0);
            // What is on the pointer is exactly what the engine finds there.
            List<Dial.Point> all = new ArrayList<>(d.points);
            all.addAll(d.midpoints);
            ok(DialPanel.DIAL_NAMES[mi] + ": on the pointer is exactly the engine's answer",
                d.onPointer().equals(Dial.near(d.pointer, all, m, d.orbDegrees())));
        }

        // A picture's link turns the pointer to its focus.
        SwingUtilities.invokeAndWait(() -> {
            d.dialChoice.setSelectedIndex(0);
            d.setPointer(7.0);
            d.list.fireHyperlinkUpdate(new javax.swing.event.HyperlinkEvent(d.list,
                javax.swing.event.HyperlinkEvent.EventType.ACTIVATED, null, "dial|Sun"));
        });
        double sun = f.body("Sun").lon;
        near("a picture's link turns the pointer to its planet", Dial.folded(sun, 90.0), d.pointer);
        ok("and the Sun is then on the pointer", d.onPointer().stream().anyMatch(p -> p.name.equals("Sun")));
        SwingUtilities.invokeAndWait(() -> d.showMidpoints.doClick());
        ok("with midpoints off, none is on the pointer", d.onPointer().stream().noneMatch(Dial.Point::isMidpoint));
        SwingUtilities.invokeAndWait(() -> d.showMidpoints.doClick());

        // Switching dials keeps the pointer on the dial.
        SwingUtilities.invokeAndWait(() -> {
            d.setPointer(80.0);
            d.dialChoice.setSelectedIndex(2);
        });
        ok("switching to the 22.5 dial folds the pointer onto it, " + d.pointer, d.pointer >= 0 && d.pointer < 22.5);

        // The door, and the chart on the wheel.
        ok("the menu offers the Dial screen",
            java.util.Arrays.stream(SidePanel.SCREENS).anyMatch(s -> s[1].equals("DIAL")));
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SwingUtilities.invokeAndWait(() -> w[0].switchScreen("DIAL"));
            java.lang.reflect.Field fd = OuraniaWindow.class.getDeclaredField("dialPanel");
            fd.setAccessible(true);
            DialPanel panel = (DialPanel) fd.get(w[0]);
            ok("opening it reads the chart on the wheel", panel != null && panel.chart() != null && !panel.points.isEmpty());
            ok("and it is the screen showing", panel != null && panel.isVisible());
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static void near(String label, double want, double got) {
        ok(label + String.format(": got %.6f, expected %.6f", got, want), Math.abs(got - want) < 1e-6);
    }

    private static void eq(String label, Object want, Object got) {
        ok(label + ": got " + got + ", expected " + want, want.equals(got));
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
