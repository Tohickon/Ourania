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
    /**
     * @param turning true while the reader is dragging the globe
     */
    static void paint(Graphics2D g, Globe cam, int w, int h, SkymapPanel panel,
                      boolean turning) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GlobeRenderer r = new GlobeRenderer(g, cam, w, h, panel.pinLongitude());

        double[] shells = shellRadii(panel);
        double natalR = shells[0];
        double partnerR = shells[1];
        double skyR = shells[2];

        // <b>The two translucent shells, innermost first.</b> Houses inside, signs outside,
        // so looking in from anywhere the reader sees a sign colour laid over a house shade
        // and the two read as one surface where they cross. That intersection is the thing
        // the flat wheel cannot draw: there, a body is in a sign and in a house and the two
        // facts sit in separate rings; here they are one colour.
        //
        // <b>Left out while the globe is being turned.</b> Measured across five panel sizes,
        // a frame costs the same whatever the resolution - so this is per-call overhead, not
        // pixels - and the two filled shells are about eighteen milliseconds of it, taking a
        // drag from forty frames a second to twenty. Twenty is not a slow globe, it is a
        // globe that fights the hand moving it. The shells come back the moment the drag ends,
        // which is when a reader is actually looking at them rather than at the motion.
        if (!turning) {
            r.houseShell(panel.activeCusps);
            r.signPlane();
        }

        r.degreeRing();
        r.zodiacBand();
        r.boundRing();
        r.decanRing();
        r.houseMeridians(panel.activeCusps);
        r.houseNumbers(panel.activeCusps);
        r.ringCircle(natalR, new Color(120, 132, 150, 90), 0.0);
        if (panel.outerRingDrawn()) {
            r.ringCircle(partnerR, new Color(190, 165, 110, 90), Globe.INCLINE_PARTNER);
        }
        if (panel.triRingDrawn()) {
            r.ringCircle(skyR, new Color(120, 170, 215, 90), Globe.INCLINE_SKY);
        }

        r.aspectChords(panel, shells);
        r.bodies(panel.bLon, panel.bValid, natalR, SkymapPanel.AngleRole.ANCHOR, panel, false, 0);
        if (panel.outerRingDrawn()) {
            r.bodies(panel.tLon, panel.tValid, partnerR,
                panel.angleRoleFor(false, true), panel, true, 1);
        }
        if (panel.triRingDrawn()) {
            r.bodies(panel.cLon, panel.cValid, skyR, SkymapPanel.AngleRole.SKY, panel, true, 2);
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
    /**
     * The plane each ring rides in: natal horizontal, partner and sky opposed either side.
     *
     * <b>Beside shellRadii, and for the same reason.</b> Where a body is on the globe is a
     * radius and a plane; if the hit test knew one and not the other it would be the
     * see-it-but-cannot-click-it defect again, in the half that is easier to miss.
     */
    static double inclinationOf(int ring) {
        return ring == 1 ? Globe.INCLINE_PARTNER : (ring == 2 ? Globe.INCLINE_SKY : 0.0);
    }

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
                    level[i] * Globe.STACK_STEP, inclinationOf(ring));
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

    /**
     * One body ring, as the single great circle it rides on.
     *
     * <b>A circle, not a wireframe sphere.</b> The shells stopped needing to be drawn as
     * surfaces once the houses and signs were filled - three wireframe globes inside two
     * translucent ones was a thicket. What a reader needs from a body ring is its plane, and
     * one bright circle says that better than sixty faint lines, especially now the three
     * planes are tilted apart and the crossing points are the thing to see.
     */
    private void ringCircle(double radius, Color ink, double inclination) {
        polyline(Globe.equator(this.origin, radius, 96, inclination), ink, 1.4f);
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
     * The houses, as translucent slices of the sphere, grey to black.
     *
     * <b>Wedges again, now that the signs are a plane.</b> These were latitude bands for a
     * while, which crossed the sign wedges into a readable grid but put the shading somewhere
     * a house is not - a house divides the ecliptic, so a band of declination was a scale
     * pretending to be a map. With the zodiac moved onto the horizontal plane the collision
     * that forced that compromise is gone, and the houses can be what they are.
     *
     * An unequal house is a different size, and on a globe that is visible in a way a wedge of
     * a disc never quite is: it is an amount of sky rather than a label.
     */
    private void houseShell(double[] cusps) {
        if (cusps == null || cusps.length < 13) {
            return;
        }
        for (int i = 1; i <= 12; i++) {
            double from = cusps[i];
            double span = arc(from, cusps[i == 12 ? 1 : i + 1]);
            if (span < 0.01) {
                continue;                       // a degenerate cusp pair; nothing to fill
            }
            int level = 112 - (i - 1) * 9;      // house 1 lightest, house 12 nearly black
            sector(from, from + span, Globe.SHELL_HOUSE,
                new Color(level, level, level + 6, 58));
        }
    }

    /** Degrees from one longitude round to the next, always forward. */
    private static double arc(double from, double to) {
        return ((to - from) % 360.0 + 360.0) % 360.0;
    }


    /**
     * The zodiac as a coloured plane, running inward from the sign band.
     *
     * <b>A plane, not a wedge wrapped over the sphere.</b> The wedges filled the whole globe
     * pole to pole, so a sign's colour claimed sky that has nothing to do with that sign - the
     * zodiac is a band around the ecliptic, and everything above and below it was being
     * painted anyway. As a disc the colour starts at the sign it belongs to and runs in toward
     * the centre, which is both what the sign actually covers and what the flat wheel has
     * always drawn. It also leaves the house bands the whole sphere to be read against instead
     * of competing with them for it.
     *
     * Faint, because the aspect network lives in this plane and has to be read through it.
     */
    private void signPlane() {
        for (int sign = 0; sign < 12; sign++) {
            Color ink = SkymapPanel.elementColorFor(Zodiac.elementIndex(sign));
            Color fill = new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 30);
            double from = sign * 30.0;
            int steps = 4;
            for (int i = 0; i < steps; i++) {
                double la = from + (30.0 * i) / steps;
                double lb = from + (30.0 * (i + 1)) / steps;
                // One ring of cells from the sign band in to the middle; the innermost cell
                // stops short of dead centre, where every sector would fight for one pixel.
                double[] radii = {Globe.SHELL_SIGN_INNER, 1.45, 1.00, 0.55, 0.12};
                for (int r = 0; r + 1 < radii.length; r++) {
                    quad(Globe.onShell(la, this.origin, radii[r], 0.0),
                        Globe.onShell(lb, this.origin, radii[r], 0.0),
                        Globe.onShell(lb, this.origin, radii[r + 1], 0.0),
                        Globe.onShell(la, this.origin, radii[r + 1], 0.0),
                        fill);
                }
            }
        }
    }

    /**
     * The house numbers, ringed around the top of the globe and around the bottom.
     *
     * <b>Where the wedges converge, which is where a segment can be labelled.</b> They ran
     * down the side of the globe while the houses were latitude bands, and a column of numbers
     * top to bottom is not how anyone reads a sphere - it says the houses are stacked, when
     * they are twelve slices around it. Near a pole every slice is present and narrow, so the
     * twelve fit in a ring the way they do on the top of a beach ball.
     *
     * Both poles, because half a globe is always facing away: whichever end the reader is
     * looking down, the ring nearest them is the readable one.
     */
    private void houseNumbers(double[] cusps) {
        if (cusps == null || cusps.length < 13) {
            return;
        }
        for (int i = 1; i <= 12; i++) {
            double span = arc(cusps[i], cusps[i == 12 ? 1 : i + 1]);
            double mid = cusps[i] + span / 2.0;
            String label = String.valueOf(i);
            for (int pole = -1; pole <= 1; pole += 2) {
                // Close to the pole, where the wedges are narrow and the twelve sit in a tight
                // ring. Far enough off it that they do not pile onto the point itself.
                double phi = pole * (Globe.FILL_SPAN - 0.30);
                billboard(Globe.onShell(mid, this.origin, Globe.SHELL_HOUSE,
                    Globe.SHELL_HOUSE * Math.sin(phi)), label, new Color(206, 208, 216), 12);
            }
        }
    }

    /**
     * The full 360-degree scale, lying flat in the plane of the ecliptic.
     *
     * <b>Horizontal, because that is the plane every position is measured in.</b> The flat
     * wheel has always carried a degree ring; the globe had tick marks at every ten on a
     * shell, which is enough to orient by and not enough to read a degree off. This is the
     * same scale in the one plane where a longitude means what it says, so a body on any of
     * the three rings can be dropped onto it by eye.
     *
     * Ticks in four weights: every degree short, every fifth longer, every tenth longer and
     * brighter, and the sign boundaries reaching furthest.
     */
    private void degreeRing() {
        double outer = Globe.SHELL_SIGN_INNER - 0.02;
        for (int d = 0; d < 360; d++) {
            boolean sign = d % 30 == 0;
            boolean ten = d % 10 == 0;
            double depth = sign ? 0.20 : (ten ? 0.13 : (d % 5 == 0 ? 0.08 : 0.045));
            Color ink = sign ? new Color(196, 200, 210, 210)
                : (ten ? new Color(160, 166, 178, 180) : new Color(128, 134, 146, 130));
            segment(Globe.onShell(d, this.origin, outer, 0.0),
                Globe.onShell(d, this.origin, outer - depth, 0.0),
                ink, sign ? 1.4f : (ten ? 1.0f : 0.6f));
        }
        // The scale itself, so the ticks hang off a line rather than floating.
        polyline(Globe.equator(this.origin, outer, 144), new Color(150, 156, 168, 150), 1.0f);
    }

    /**
     * A filled slice of a shell, between two longitudes and short of both poles.
     *
     * <b>Tessellated rather than drawn as one polygon.</b> A sector of a sphere is not flat,
     * so its projection is not a quadrilateral - filling it as one would give straight edges
     * where the globe curves, and the error is worst exactly where the slice is widest. Small
     * quads, each with its own depth, also let the painter's sort put a near slice over a far
     * one, which is what makes the shell read as a surface rather than as a stencil.
     */
    private void sector(double lon0, double lon1, double radius, Color fill) {
        // <b>The rows at the poles collapse to triangles, and that is the point.</b> At
        // ninety degrees of latitude every longitude on a shell is the same point, so the top
        // and bottom cells of each wedge come out with two corners coincident. Filled as
        // polygons they are triangles, twelve of them tiling each cap with no overlap - which
        // is why the cap can be closed without the wedges darkening where they meet.
        int steps = Math.max(3, (int) Math.ceil(Math.abs(lon1 - lon0) / 11.0));
        int rows = 7;
        for (int i = 0; i < steps; i++) {
            double la = lon0 + ((lon1 - lon0) * i) / steps;
            double lb = lon0 + ((lon1 - lon0) * (i + 1)) / steps;
            for (int j = 0; j < rows; j++) {
                double p0 = -Globe.FILL_SPAN + (2 * Globe.FILL_SPAN * j) / rows;
                double p1 = -Globe.FILL_SPAN + (2 * Globe.FILL_SPAN * (j + 1)) / rows;
                quad(Globe.onShell(la, this.origin, radius, radius * Math.sin(p0)),
                    Globe.onShell(lb, this.origin, radius, radius * Math.sin(p0)),
                    Globe.onShell(lb, this.origin, radius, radius * Math.sin(p1)),
                    Globe.onShell(la, this.origin, radius, radius * Math.sin(p1)),
                    fill);
            }
        }
    }

    /** One tessellation cell, filled flat. */
    private void quad(double[] a, double[] b, double[] c, double[] d, Color fill) {
        Globe.Projected pa = at(a);
        Globe.Projected pb = at(b);
        Globe.Projected pc = at(c);
        Globe.Projected pd = at(d);
        if (!pa.visible || !pb.visible || !pc.visible || !pd.visible) {
            return;
        }
        double depth = (pa.depth + pb.depth + pc.depth + pd.depth) / 4.0;
        this.pieces.add(new Piece(depth, () -> {
            // <b>Antialiasing off for the wash.</b> These are large translucent polygons and
            // the smoothing is both the expensive part and invisible - each cell abuts its
            // neighbours in the same colour, so the only edges that could show are the ones
            // at the outside of a sector, and there the fill is faint enough that a hard edge
            // reads as a boundary rather than as a jagged one. Lines and glyphs keep it.
            java.awt.Polygon poly = new java.awt.Polygon();
            poly.addPoint((int) Math.round(pa.x), (int) Math.round(pa.y));
            poly.addPoint((int) Math.round(pb.x), (int) Math.round(pb.y));
            poly.addPoint((int) Math.round(pc.x), (int) Math.round(pc.y));
            poly.addPoint((int) Math.round(pd.x), (int) Math.round(pd.y));
            this.g.setColor(fill);
            this.g.fillPolygon(poly);
        }));
    }

    /**
     * The Egyptian bounds, on a shell just inside the signs.
     *
     * Both halves from Dignity, as on the flat wheel - boundEdges for where the divisions go,
     * boundRulerOf for what is written in each - so the globe cannot disagree with the score
     * about whose bound a body stands in.
     */
    private void boundRing() {
        for (int sign = 0; sign < 12; sign++) {
            double[] edges = com.zodiacomputing.ourania.astro.Dignity.boundEdges(sign);
            for (int i = 0; i < edges.length - 1; i++) {
                double mid = sign * 30.0 + (edges[i] + edges[i + 1]) / 2.0;
                String ruler = com.zodiacomputing.ourania.astro.Dignity
                    .boundRulerOf(mid % 360.0);
                int bi = Bodies.indexOfName(ruler);
                if (bi < 0) {
                    continue;
                }
                billboard(Globe.onShell(mid, this.origin, Globe.SHELL_BOUND, 0.0),
                    SkymapPanel.glyphOf(bi), SkymapPanel.bodyInkFor(bi), 10);
            }
        }
    }

    /** The decans, on a shell just outside the signs, in whichever scheme the reader chose. */
    private void decanRing() {
        boolean chaldean = Settings.DECAN_RING_CHALDEAN.equals(Settings.decanRing());
        for (int d = 0; d < 36; d++) {
            int sign = d / 3;
            double mid = d * 10.0 + 5.0;
            if (chaldean) {
                String ruler = Zodiac.chaldeanDecanRuler(Zodiac.SIGNS[sign], d % 3 + 1);
                int bi = Bodies.indexOfName(ruler);
                if (bi >= 0) {
                    billboard(Globe.onShell(mid, this.origin, Globe.SHELL_DECAN, 0.0),
                        SkymapPanel.glyphOf(bi), SkymapPanel.bodyInkFor(bi), 10);
                    continue;
                }
            }
            int face = Zodiac.triplicityDecanSignIndex(sign, d % 3 + 1);
            billboard(Globe.onShell(mid, this.origin, Globe.SHELL_DECAN, 0.0),
                SkymapPanel.zodiacSymbol(face),
                SkymapPanel.elementColorFor(Zodiac.elementIndex(face)), 10);
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
            polyline(Globe.meridian(cusps[i], this.origin, Globe.SHELL_HOUSE, 20),
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
    private void aspectChords(SkymapPanel panel, double[] shells) {
        // <b>Body to body, so every glyph is a node.</b> The chords were drawn on nested
        // circles inside the wheels, which is what the flat chart has to do - a line between
        // two glyphs on a disc crosses everything between them. A sphere does not have that
        // problem: the lines have depth to spread into, they pass through the middle rather
        // than across the face, and a chord that starts and ends on a visible glyph says which
        // two bodies it joins without the reader tracing an angle. Drawn to the same stacked
        // positions the glyphs use, so a line lands on a bead rather than near it.
        //
        // A cross-chart chord now visibly leaves one tilted plane and arrives on another,
        // which is the clearest thing this view does: a synastry contact looks like a contact.
        int[][] chords = panel.globeChords(() -> buildChords(panel));
        double origin = this.origin;
        int[][] levels = {
            Globe.stackLevels(panel.bLon, panel.bValid, 7.0),
            Globe.stackLevels(panel.tLon, panel.tValid, 7.0),
            Globe.stackLevels(panel.cLon, panel.cValid, 7.0),
        };
        double[][] lons = {panel.bLon, panel.tLon, panel.cLon};

        // <b>The reader's filter, applied when drawing rather than when building.</b> It
        // decides which families are shown, not which pairs are in aspect, so it belongs here
        // - and keeping it out of the cache means changing it costs a repaint rather than a
        // recompute of every pair.
        boolean natal = panel.drawsNatalAspects();
        boolean cross = panel.drawsCrossAspects();
        for (int[] c : chords) {
            int ring = c[0];
            if (ring == 0 && !natal) {
                continue;
            }
            if (ring > 0 && !cross) {
                continue;
            }
            if (ring == 1 && !panel.outerRingDrawn()) {
                continue;
            }
            if (ring == 2 && !panel.triRingDrawn()) {
                continue;
            }
            double[] from = Globe.onShell(lons[ring][c[1]], origin, shells[ring],
                levels[ring][c[1]] * Globe.STACK_STEP, inclinationOf(ring));
            double[] to = Globe.onShell(panel.bLon[c[2]], origin, shells[0],
                levels[0][c[2]] * Globe.STACK_STEP, 0.0);
            segment(from, to, new Color(c[3], true), 1.0f);
        }
    }

    /**
     * Which pairs are in aspect, and in what colour - built once per chart, not per frame.
     *
     * Every ring back to the natal wheel. Within one chart a pair is counted once; across two
     * charts the pairing is directional, so every body meets every body.
     */
    private static int[][] buildChords(SkymapPanel panel) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        double[][] lons = {panel.bLon, panel.tLon, panel.cLon};
        boolean[][] valids = {panel.bValid, panel.tValid, panel.cValid};
        for (int ring = 0; ring < 3; ring++) {
            boolean cross = ring > 0;
            for (int a = 0; a < SkymapPanel.BODY_COUNT && a < lons[ring].length; a++) {
                if (!SkymapPanel.aspecting(a, valids[ring])) {
                    continue;
                }
                for (int b = cross ? 0 : a + 1; b < SkymapPanel.BODY_COUNT; b++) {
                    if (!SkymapPanel.aspecting(b, panel.bValid)) {
                        continue;
                    }
                    if (!cross && Bodies.isOppositePair(a, b)) {
                        continue;
                    }
                    Color ink = panel.aspectInkFor(lons[ring][a], panel.bLon[b], a, b, cross);
                    if (ink != null) {
                        out.add(new int[] {ring, a, b, ink.getRGB()});
                    }
                }
            }
        }
        return out.toArray(new int[0][]);
    }

    /** One ring of bodies on its shell, stacked up the shell where longitudes crowd. */
    private void bodies(double[] lon, boolean[] valid, double radius,
                        SkymapPanel.AngleRole role, SkymapPanel panel, boolean outer,
                        int ring) {
        int[] level = Globe.stackLevels(lon, valid, 7.0);
        for (int i = 0; i < SkymapPanel.BODY_COUNT && i < lon.length; i++) {
            if (!valid[i]) {
                continue;
            }
            double y = level[i] * Globe.STACK_STEP;
            double[] p = Globe.onShell(lon[i], this.origin, radius, y, inclinationOf(ring));
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

            final int half = this.g.getFontMetrics(font(13)).stringWidth(glyph) / 2;
            this.pieces.add(new Piece(q.depth, () -> {
                int rad = lit ? 11 : 9;
                this.g.setColor(shade(bead, alpha));
                this.g.fillOval((int) q.x - rad, (int) q.y - rad, rad * 2, rad * 2);
                this.g.setStroke(stroke(lit ? 2.0f : 1.0f));
                this.g.setColor(shade(lit ? new Color(255, 238, 170) : ink, alpha));
                this.g.drawOval((int) q.x - rad, (int) q.y - rad, rad * 2, rad * 2);
                this.g.setFont(font(13));
                this.g.setColor(shade(ink, alpha));
                this.g.drawString(glyph, (int) q.x - half, (int) q.y + 5);
            }));
        }
    }

    // ------------------------------------------------------------------ drawing primitives

    /**
     * A run of points as one stroked path, in a few chunks rather than one piece per segment.
     *
     * <b>This was one Piece per segment and it cost the view its interactivity.</b> A frame
     * held roughly two thousand four hundred line segments, each a separate object with its
     * own closure and its own freshly allocated stroke, and a globe at 1100 pixels took 141
     * milliseconds - seven frames a second, which under a drag is not a slow globe but a
     * broken one. Drawn as chunked polylines it is a few dozen pieces.
     *
     * <b>Chunked rather than whole</b>, because an equator wraps from the near side of the
     * globe to the far side and back: one piece for the whole circle would take a single mean
     * depth, and the half that is behind would sort as though it were in front. Twelve points
     * a chunk is short enough that a chunk is entirely near or entirely far.
     */
    private void polyline(double[][] pts, Color ink, float width) {
        final int chunk = 12;
        for (int start = 0; start + 1 < pts.length; start += chunk) {
            int end = Math.min(pts.length - 1, start + chunk);
            int n = end - start + 1;
            int[] xs = new int[n];
            int[] ys = new int[n];
            double depth = 0;
            int kept = 0;
            for (int i = start; i <= end; i++) {
                Globe.Projected q = at(pts[i]);
                if (!q.visible) {
                    continue;                   // a chunk crossing the lens is simply dropped
                }
                xs[kept] = (int) Math.round(q.x);
                ys[kept] = (int) Math.round(q.y);
                depth += q.depth;
                kept++;
            }
            if (kept < 2) {
                continue;
            }
            final int count = kept;
            this.pieces.add(new Piece(depth / kept, () -> {
                this.g.setStroke(stroke(width));
                this.g.setColor(ink);
                this.g.drawPolyline(xs, ys, count);
            }));
        }
    }

    /**
     * Strokes, cached by width.
     *
     * A new BasicStroke per segment was allocating thousands of identical objects a frame.
     * There are four widths in this scene.
     */
    private static final java.util.Map<Float, Stroke> STROKES =
        new java.util.concurrent.ConcurrentHashMap<>();

    private static Stroke stroke(float width) {
        return STROKES.computeIfAbsent(width, BasicStroke::new);
    }

    /**
     * Fonts, cached by point size, for the same reason as the strokes.
     *
     * A frame writes about two hundred glyphs - bodies, bounds, decans, signs - and each one
     * was allocating a Font and asking for fresh FontMetrics. There are three sizes.
     */
    private static final java.util.Map<Integer, Font> FONTS =
        new java.util.concurrent.ConcurrentHashMap<>();

    private static Font font(int points) {
        return FONTS.computeIfAbsent(points, p -> new Font("SansSerif", 0, p));
    }

    private void segment(double[] a, double[] b, Color ink, float width) {
        Globe.Projected pa = at(a);
        Globe.Projected pb = at(b);
        if (!pa.visible || !pb.visible) {
            return;
        }
        double depth = (pa.depth + pb.depth) / 2.0;
        this.pieces.add(new Piece(depth, () -> {
            this.g.setStroke(stroke(width));
            this.g.setColor(ink);
            this.g.drawLine((int) pa.x, (int) pa.y, (int) pb.x, (int) pb.y);
        }));
    }

    /** Text that always faces the reader, however the globe is turned. */
    private void billboard(double[] world, String text, Color ink, int points) {
        Globe.Projected p = at(world);
        if (!p.visible) {
            return;
        }
        // Width measured once, here, rather than inside the draw - the draw runs after the
        // sort and the metrics do not depend on it.
        final int half = this.g.getFontMetrics(font(points)).stringWidth(text) / 2;
        this.pieces.add(new Piece(p.depth, () -> {
            this.g.setFont(font(points));
            this.g.setColor(ink);
            this.g.drawString(text, (int) p.x - half, (int) p.y + points / 3);
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
