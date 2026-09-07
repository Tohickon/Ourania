package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Zodiac;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Paints the chart as nested shells rather than as flat bands.
 *
 * <b>The same chart, from inside it.</b> This reads the arrays the flat wheel reads - bLon,
 * tLon, cLon and the cusps - so there is one chart and two ways of looking at it, not two
 * charts. Every number it needs about which rings are open comes from the same
 * outerOpenFraction and triOpenFraction the bands use, so opening a ring moves a shell outward
 * by exactly the amount it would have widened a band.
 *
 * <b>Painter's algorithm, because the scene is small enough for it.</b> Everything drawn is
 * collected as a list of pieces with a depth, sorted far-to-near, and painted in that order.
 * A z-buffer would be the general answer; for a few hundred line segments and glyphs it would
 * be a lot of machinery to arrive at the same picture. The one thing it costs is that two
 * pieces which genuinely interpenetrate - a line passing through a sphere - resolve by their
 * midpoint rather than per pixel, and nothing in this scene does that.
 */
final class GlobeRenderer {

    /** One thing to draw, and how far away it is. */
    private static final class Piece implements Comparable<Piece> {
        final double depth;
        final Runnable draw;

        Piece(double depth, Runnable draw) {
            this.depth = depth;
            this.draw = draw;
        }

        @Override
        public int compareTo(Piece other) {
            return Double.compare(other.depth, this.depth);   // far first
        }
    }

    private final Graphics2D g;
    private final Globe cam;
    private final int w;
    private final int h;
    private final double origin;
    private final List<Piece> pieces = new ArrayList<>();

    private GlobeRenderer(Graphics2D g, Globe cam, int w, int h, double origin) {
        this.g = g;
        this.cam = cam;
        this.w = w;
        this.h = h;
        this.origin = origin;
    }

    /**
     * Draws the whole scene.
     *
     * @param panel the chart this is a view of - read only, and read rather than copied so the
     *     two views cannot disagree about what the chart is
     */
    static void paint(Graphics2D g, Globe cam, int w, int h, SkymapPanel panel) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GlobeRenderer r = new GlobeRenderer(g, cam, w, h, panel.pinLongitude());

        double[] shells = shellRadii(panel);
        double natalR = shells[0];
        double partnerR = shells[1];
        double skyR = shells[2];

        r.zodiacBand();
        r.houseMeridians(panel.activeCusps);
        r.shell(natalR, new Color(120, 132, 150, 60), 8, 3);
        if (panel.outerRingDrawn()) {
            r.shell(partnerR, new Color(150, 130, 90, 46), 8, 2);
        }
        if (panel.triRingDrawn()) {
            r.shell(skyR, new Color(110, 150, 190, 38), 10, 2);
        }

        r.aspectChords(panel, natalR);
        r.bodies(panel.bLon, panel.bValid, natalR, SkymapPanel.AngleRole.ANCHOR, panel, false);
        if (panel.outerRingDrawn()) {
            r.bodies(panel.tLon, panel.tValid, partnerR,
                panel.angleRoleFor(false, true), panel, true);
        }
        if (panel.triRingDrawn()) {
            r.bodies(panel.cLon, panel.cValid, skyR, SkymapPanel.AngleRole.SKY, panel, true);
        }

        r.flush();
    }

    /**
     * Where the three body shells are, given which rings are open.
     *
     * <b>One statement of it, because the painter and the hit test both need it.</b> This is
     * the flat wheel's Geometry problem in a second view: a shell radius computed twice is a
     * body you can see and cannot click, and that defect has already been found in this panel
     * once. The blooms are the same numbers that widen a band, so a ring half open is a shell
     * half way out and switching views mid-animation does not jump.
     *
     * @return natal, partner and sky radii, in that order
     */
    static double[] shellRadii(SkymapPanel panel) {
        double outerOpen = panel.outerOpenFraction();
        double triOpen = panel.triOpenFraction();
        return new double[] {
            Globe.SHELL_NATAL - 0.08 * outerOpen - 0.06 * triOpen,
            lerp(Globe.SHELL_NATAL, Globe.SHELL_PARTNER, outerOpen),
            lerp(Globe.SHELL_NATAL + 0.08, Globe.SHELL_SKY,
                triOpen > 0.001 ? triOpen : outerOpen),
        };
    }

    /**
     * The body under the cursor on the globe, packed the way {@code bodyAt} packs it, or -1.
     *
     * <b>Reads the same shells the painter does, and applies the same stack.</b> A hit test
     * that recomputed either would be the defect Geometry exists to prevent, in a view where
     * it would be harder to notice - on a globe a reader who clicks and gets the wrong body
     * assumes they missed.
     *
     * Nearest wins rather than first, and only within a glyph's radius, so a click on empty
     * sky selects nothing instead of the closest thing on the far side of the world.
     */
    static int bodyAt(Globe cam, int w, int h, SkymapPanel panel, int px, int py) {
        double origin = panel.pinLongitude();
        double[] shells = shellRadii(panel);
        int best = -1;
        double bestDist = 15.0;                 // a glyph's bead, in pixels

        // Outermost first only matters for ties; nearest-wins settles the rest.
        for (int ring = 0; ring < 3; ring++) {
            double[] lon = ring == 0 ? panel.bLon : (ring == 1 ? panel.tLon : panel.cLon);
            boolean[] valid = ring == 0 ? panel.bValid
                : (ring == 1 ? panel.tValid : panel.cValid);
            if (ring == 1 && !panel.outerRingDrawn()) {
                continue;
            }
            if (ring == 2 && !panel.triRingDrawn()) {
                continue;
            }
            int[] level = Globe.stackLevels(lon, valid, 7.0);
            for (int i = 0; i < SkymapPanel.BODY_COUNT && i < lon.length; i++) {
                if (!valid[i]) {
                    continue;
                }
                double[] p = Globe.onShell(lon[i], origin, shells[ring],
                    level[i] * Globe.STACK_STEP);
                Globe.Projected q = cam.project(p[0], p[1], p[2], w, h);
                if (!q.visible) {
                    continue;
                }
                double d = Math.hypot(q.x - px, q.y - py);
                if (d < bestDist) {
                    bestDist = d;
                    best = ring == 0 ? i : (i | SkymapPanel.TRANSIT_BIT);
                }
            }
        }
        return best;
    }

    /** Sorts everything collected and paints it far to near. */
    private void flush() {
        java.util.Collections.sort(this.pieces);
        for (Piece p : this.pieces) {
            p.draw.run();
        }
    }

    private Globe.Projected at(double[] world) {
        return this.cam.project(world[0], world[1], world[2], this.w, this.h);
    }

    /** A great-circle wireframe, so a shell reads as a surface rather than as a hoop. */
    private void shell(double radius, Color ink, int meridians, int parallels) {
        double[][] eq = Globe.equator(this.origin, radius, 120);
        polyline(eq, ink, 1.1f);
        for (int i = 0; i < meridians; i++) {
            polyline(Globe.meridian(this.origin + (360.0 * i) / meridians, this.origin,
                radius, 28), ink, 0.7f);
        }
        // Latitude circles, thinning toward the poles the way a globe's do.
        for (int i = 1; i <= parallels; i++) {
            double phi = (Globe.MERIDIAN_SPAN * i) / (parallels + 1);
            for (int sign = -1; sign <= 1; sign += 2) {
                double y = radius * Math.sin(phi * sign);
                double[][] ring = new double[97][];
                for (int k = 0; k <= 96; k++) {
                    ring[k] = Globe.onShell(this.origin + (360.0 * k) / 96, this.origin,
                        radius, y);
                }
                polyline(ring, ink, 0.5f);
            }
        }
    }

    /**
     * The zodiac, as a band around the equator of the sign shell.
     *
     * Each sign gets its own arc in its element's colour and a glyph at its midpoint, so the
     * band reads the way the flat wheel's sign ring does - the frame the positions are
     * measured against, outside everything it measures.
     */
    private void zodiacBand() {
        for (int sign = 0; sign < 12; sign++) {
            double from = sign * 30.0;
            Color ink = SkymapPanel.elementColorFor(Zodiac.elementIndex(sign));

            double[][] arc = new double[25][];
            for (int i = 0; i <= 24; i++) {
                arc[i] = Globe.onShell(from + (30.0 * i) / 24, this.origin,
                    Globe.SHELL_SIGN_OUTER, 0.0);
            }
            polyline(arc, new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 190), 2.4f);

            // The division at the sign's start, drawn across the band's depth.
            double[] a = Globe.onShell(from, this.origin, Globe.SHELL_SIGN_INNER, 0.0);
            double[] b = Globe.onShell(from, this.origin, Globe.SHELL_SIGN_OUTER, 0.0);
            segment(a, b, new Color(150, 150, 150, 140), 1.0f);

            double[] mid = Globe.onShell(from + 15.0, this.origin,
                (Globe.SHELL_SIGN_INNER + Globe.SHELL_SIGN_OUTER) / 2, 0.0);
            billboard(mid, SkymapPanel.zodiacSymbol(sign), ink, 15);
        }
        // The degree scale, at every ten, on the tick shell just inside the signs.
        for (int d = 0; d < 360; d += 10) {
            double[] a = Globe.onShell(d, this.origin, Globe.SHELL_TICK, 0.0);
            double[] b = Globe.onShell(d, this.origin, Globe.SHELL_TICK - 0.06, 0.0);
            segment(a, b, new Color(140, 148, 160, 130), d % 30 == 0 ? 1.4f : 0.6f);
        }
    }

    /**
     * The houses, as meridians from pole to pole.
     *
     * <b>A house is a slice of the sphere, not a wedge of a disc.</b> Drawing the cusps as
     * great circles is the one place the globe says something the flat wheel cannot: the
     * houses divide the whole sky, and on a disc that is only ever implied.
     */
    private void houseMeridians(double[] cusps) {
        if (cusps == null || cusps.length < 13) {
            return;                             // no chart cast yet; the shells still draw
        }
        for (int i = 1; i <= 12; i++) {
            boolean angle = i == 1 || i == 4 || i == 7 || i == 10;
            polyline(Globe.meridian(cusps[i], this.origin, Globe.SHELL_HOUSE, 26),
                angle ? new Color(210, 200, 175, 190) : new Color(120, 120, 130, 110),
                angle ? 1.6f : 0.8f);
        }
    }

    /**
     * Aspect chords, drawn through the globe rather than around it.
     *
     * <b>This is the view's argument for itself.</b> On the flat wheel an aspect is a line
     * across a disc; here it is a chord through a sphere, and a reader turning the globe sees
     * the figure from the side - which is the one thing the flat chart genuinely cannot show.
     * Drawn on the core shell so they stay inside every body ring rather than crossing them.
     */
    private void aspectChords(SkymapPanel panel, double natalR) {
        double r = Math.min(Globe.SHELL_CORE, natalR - 0.12);
        for (int a = 0; a < SkymapPanel.BODY_COUNT; a++) {
            if (!SkymapPanel.aspecting(a, panel.bValid)) {
                continue;
            }
            for (int b = a + 1; b < SkymapPanel.BODY_COUNT; b++) {
                if (!SkymapPanel.aspecting(b, panel.bValid)
                    || Bodies.isOppositePair(a, b)) {
                    continue;
                }
                Color ink = panel.aspectInkFor(panel.bLon[a], panel.bLon[b], a, b, false);
                if (ink == null) {
                    continue;
                }
                segment(Globe.onShell(panel.bLon[a], this.origin, r, 0.0),
                    Globe.onShell(panel.bLon[b], this.origin, r, 0.0), ink, 1.0f);
            }
        }
    }

    /** One ring of bodies on its shell, stacked up the shell where longitudes crowd. */
    private void bodies(double[] lon, boolean[] valid, double radius,
                        SkymapPanel.AngleRole role, SkymapPanel panel, boolean outer) {
        int[] level = Globe.stackLevels(lon, valid, 7.0);
        for (int i = 0; i < SkymapPanel.BODY_COUNT && i < lon.length; i++) {
            if (!valid[i]) {
                continue;
            }
            double y = level[i] * Globe.STACK_STEP;
            double[] p = Globe.onShell(lon[i], this.origin, radius, y);
            Globe.Projected q = at(p);
            if (!q.visible) {
                continue;
            }
            boolean lit = panel.onGlobeFocus(i, outer);
            Color ink = panel.ringInk(i, role);
            Color bead = SkymapPanel.ringBead(role);
            String glyph = SkymapPanel.glyphOf(i);
            // Bodies on the far side of their own shell are dimmed rather than hidden: a
            // reader turning the globe should see what is coming round, not have it appear.
            // Far enough back to read as behind, near enough to still be legible - at 0.38
            // the whole back hemisphere disappeared into the ground.
            double far = q.depth > this.cam.distance ? 0.55 : 1.0;
            final int alpha = (int) Math.round(255 * far);

            this.pieces.add(new Piece(q.depth, () -> {
                int rad = lit ? 11 : 9;
                this.g.setColor(shade(bead, alpha));
                this.g.fillOval((int) q.x - rad, (int) q.y - rad, rad * 2, rad * 2);
                this.g.setStroke(new BasicStroke(lit ? 2.0f : 1.0f));
                this.g.setColor(shade(lit ? new Color(255, 238, 170) : ink, alpha));
                this.g.drawOval((int) q.x - rad, (int) q.y - rad, rad * 2, rad * 2);
                this.g.setFont(new Font("SansSerif", 0, 13));
                this.g.setColor(shade(ink, alpha));
                this.g.drawString(glyph,
                    (int) q.x - this.g.getFontMetrics().stringWidth(glyph) / 2,
                    (int) q.y + 5);
            }));
        }
    }

    // ------------------------------------------------------------------ drawing primitives

    private void polyline(double[][] pts, Color ink, float width) {
        for (int i = 0; i + 1 < pts.length; i++) {
            segment(pts[i], pts[i + 1], ink, width);
        }
    }

    private void segment(double[] a, double[] b, Color ink, float width) {
        Globe.Projected pa = at(a);
        Globe.Projected pb = at(b);
        if (!pa.visible || !pb.visible) {
            return;
        }
        double depth = (pa.depth + pb.depth) / 2.0;
        this.pieces.add(new Piece(depth, () -> {
            Stroke was = this.g.getStroke();
            this.g.setStroke(new BasicStroke(width));
            this.g.setColor(ink);
            this.g.drawLine((int) pa.x, (int) pa.y, (int) pb.x, (int) pb.y);
            this.g.setStroke(was);
        }));
    }

    /** Text that always faces the reader, however the globe is turned. */
    private void billboard(double[] world, String text, Color ink, int points) {
        Globe.Projected p = at(world);
        if (!p.visible) {
            return;
        }
        this.pieces.add(new Piece(p.depth, () -> {
            this.g.setFont(new Font("SansSerif", 0, points));
            this.g.setColor(ink);
            this.g.drawString(text,
                (int) p.x - this.g.getFontMetrics().stringWidth(text) / 2, (int) p.y + points / 3);
        }));
    }

    private static Color shade(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
            Math.max(0, Math.min(255, alpha)));
    }

    private static double lerp(double a, double b, double t) {
        double u = Math.max(0.0, Math.min(1.0, t));
        return a + (b - a) * u;
    }
}
