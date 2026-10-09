package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Dignity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Stroke;
import java.awt.geom.Point2D;

/**
 * The pieces the flat wheel is drawn from: the bead under a glyph, the halo round a
 * highlighted one, a body's label, the Moon's phase disc, and three of the rings - the inner
 * degree scale, the bounds and the lunar mansions.
 *
 * <h3>Why this is its own class (J13, step 5a)</h3>
 *
 * <p>These lived on {@link SkymapPanel} and its inner painter, and every one of them already
 * drew only from its arguments - bar two lookups, which are arguments now: the bound ring is
 * handed the body-colour rule, and the mansion ring the Moon's mansion. Everything here is
 * static, so a second screen - the phone's wheel (M4) among them - can draw a bead or a ring
 * without a panel behind it.
 *
 * <p>Moved rather than changed, and measured as such: the wheel rendered in forty chart states
 * before and after gives the same pixels.
 */
final class WheelShapes {

    private WheelShapes() {
    }

    /**
     * The bead a glyph sits on, in whatever shape its ring's role asks for.
     *
     * <b>One gate for the classical look.</b> {@code showPlanetSpheres} used to be checked
     * inside drawMetallicSphere alone, so switching it off cleared the natal ring and left
     * the outer wheels drawing cubes - a chart that had asked for bare glyphs and got them
     * on one ring out of three. Every marker now comes through here, so the switch means
     * what it says.
     */
    /**
     * The natal wheel's bead: the body drawn as itself where there is a picture of it, and
     * the plain marker where there is not.
     *
     * <p><b>Every natal bead used to be the same grey.</b> {@code new Color(192, 192, 192)},
     * for the Sun, for Jupiter and for Vesta alike - three sizes of one object, told apart
     * only by the glyph stamped on top. The globe had the Sun's corona, Jupiter's belts and
     * Saturn's rings the whole time, behind a {@link Pen} and on the shared list, and the
     * wheel simply never called it. See {@link Planets}.
     *
     * <p><b>The natal ring only, and that is not caution.</b> The outer rings tint their beads
     * to say whose body it is - chart B's Mars against the sky's Mars - and MarkerShapeCheck
     * exists because two rings once differed by a tint alone. A planet face overwrites that
     * tint with Mars's own red on every ring at once, which would undo the one distinction
     * those rings have. The globe solved this by circling an outer body in its chart's ink;
     * the wheel can do the same, but that is a change to how the outer rings are read and not
     * a side effect of giving the natal wheel its planets.
     *
     * <p>Tied to {@code Settings.globePlanets()} rather than a new key: the preference is
     * "draw bodies as themselves", and it should not be answerable twice. The key is named
     * for the globe because that is where the feature started and a published key is never
     * renamed.
     */
    static void drawNatalBody(Graphics2D graphics2D, int body, int x, int y, int radius) {
        if (drawsFace(body)) {
            // Opaque and unlit: the wheel has no light source and no selection rim of its
            // own - a body being looked at is already answered by bulge() and the halo.
            Planets.draw(new AwtPen(graphics2D), body, x, y, radius, 255, false, false);
            return;
        }
        drawBodyMarker(graphics2D, x, y, radius, NATAL_BEAD, Settings.natalMarker());
    }

    /** The plain natal bead, for every body with no picture of its own. */
    private static final Color NATAL_BEAD = new Color(192, 192, 192);

    /**
     * The ink for a natal glyph, given the ink it would have had.
     *
     * <b>Asked of the same method that decides whether a face is drawn at all.</b> The glyph
     * and the bead under it are one decision taken twice if the painter answers it, and the
     * two would drift the first time a condition changed on one side - a body drawn as a
     * planet with its element ink on top is exactly the smudge this exists to prevent.
     */
    static Color natalGlyphInk(int body, Color element) {
        if (!drawsFace(body)) {
            return element;
        }
        return SkymapPanel.readableOn(new Color(Planets.faceColour(body).getRGB(), true));
    }

    /** Whether the natal ring draws this body as itself. One condition, asked twice. */
    private static boolean drawsFace(int body) {
        return Settings.showPlanetSpheres() && Settings.globePlanets()
            && Settings.MARKER_SPHERE.equals(Settings.natalMarker())
            && Planets.hasFace(body);
    }

    static void drawBodyMarker(Graphics2D graphics2D, int n, int n2, int n3, Color color,
                                String shape) {
        if (!Settings.showPlanetSpheres()) {
            return;
        }
        if (Settings.MARKER_CUBE.equals(shape)) {
            drawMetallicCube(graphics2D, n, n2, n3, color);
        } else if (Settings.MARKER_PYRAMID.equals(shape)) {
            drawMetallicPyramid(graphics2D, n, n2, n3, color);
        } else if (Settings.MARKER_SPHERE.equals(shape)) {
            drawMetallicSphere(graphics2D, n, n2, n3, color);
        }
        // MARKER_NONE draws nothing, which is the whole of what it is for.
    }

    /**
     * A four-sided pyramid seen from the same angle as the cube.
     *
     * <b>Two faces, because only two are ever visible.</b> Drawing the back pair as well
     * would put seams across a bead thirteen pixels wide. The lit face is on the left and
     * the shaded one on the right, which is the cube's convention - the two have to read
     * as the same chart lit by the same lamp, or the wheel looks like two drawings.
     */
    private static void drawMetallicPyramid(Graphics2D graphics2D, int n, int n2, int n3,
                                     Color color) {
        // <b>Wider at the base than the cube is.</b> A pyramid's mass is at the bottom, so
        // at the height the glyph is drawn it is only about two thirds as wide as a cube of
        // the same radius - and the glyph overhung the sides. Widening the base restores
        // the width where the glyph actually sits, and keeps the three shapes reading as
        // one weight on the ring.
        int n4 = (int)((double)n3 * 1.18);
        int n5 = (int)((double)n3 * 0.52);
        int n6 = (int)((double)n3 * 0.95);
        int apexY = n2 - n6 / 2 - n5;
        int baseY = n2 + n6 / 2;
        int footY = baseY + n5;
        Polygon left = new Polygon(new int[]{n, n - n4, n},
            new int[]{apexY, baseY, footY}, 3);
        Polygon right = new Polygon(new int[]{n, n + n4, n},
            new int[]{apexY, baseY, footY}, 3);
        Paint paint = graphics2D.getPaint();
        Stroke stroke = graphics2D.getStroke();
        Color color2 = color.brighter();
        Color color3 = color.darker();
        graphics2D.setPaint(new GradientPaint(n - n4, apexY, color2, n, footY,
            color2.darker()));
        graphics2D.fill(left);
        graphics2D.setPaint(new GradientPaint(n, apexY, color3.brighter(), n + n4, footY,
            color3.darker().darker()));
        graphics2D.fill(right);
        graphics2D.setPaint(paint);
        graphics2D.setColor(color.darker().darker());
        graphics2D.setStroke(new BasicStroke(1.0f));
        graphics2D.draw(left);
        graphics2D.draw(right);
        graphics2D.setStroke(stroke);
    }

    private static void drawMetallicCube(Graphics2D graphics2D, int n, int n2, int n3, Color color) {
        int n4 = n3;
        int n5 = (int)((double)n3 * 0.52);
        int n6 = (int)((double)n3 * 0.95);
        int n7 = n2 - n6 / 2;
        int n8 = n2 + n6 / 2;
        Polygon polygon = new Polygon(new int[]{n, n + n4, n, n - n4}, new int[]{n7 - n5, n7, n7 + n5, n7}, 4);
        Polygon polygon2 = new Polygon(new int[]{n - n4, n, n, n - n4}, new int[]{n7, n7 + n5, n8 + n5, n8}, 4);
        Polygon polygon3 = new Polygon(new int[]{n + n4, n, n, n + n4}, new int[]{n7, n7 + n5, n8 + n5, n8}, 4);
        Paint paint = graphics2D.getPaint();
        Stroke stroke = graphics2D.getStroke();
        Color color2 = color.brighter();
        Color color3 = color.darker();
        Color color4 = color.darker().darker();
        graphics2D.setPaint(new GradientPaint(n - n4, n7 - n5, color2, n + n4, n7 + n5, color2.darker()));
        graphics2D.fill(polygon);
        graphics2D.setPaint(new GradientPaint(n - n4, n7, color3.brighter(), n, n8 + n5, color3.darker()));
        graphics2D.fill(polygon2);
        graphics2D.setPaint(new GradientPaint(n, n7 + n5, color4.brighter(), n + n4, n8, color4.darker().darker()));
        graphics2D.fill(polygon3);
        graphics2D.setPaint(paint);
        graphics2D.setColor(color.darker().darker());
        graphics2D.setStroke(new BasicStroke(1.0f));
        graphics2D.draw(polygon);
        graphics2D.draw(polygon2);
        graphics2D.draw(polygon3);
        graphics2D.setStroke(stroke);
    }

    /**
     * The bead a glyph sits on: the metallic sphere, and the default for a natal ring.
     *
     * <b>Whether any bead is drawn is decided by drawBodyMarker, not here.</b> This method
     * used to test showPlanetSpheres itself, which read as the switch belonging to the
     * sphere rather than to the beads - and it did, which is exactly how the cubes went on
     * being drawn on a chart that had asked for bare glyphs.
     */
    private static void drawMetallicSphere(Graphics2D graphics2D, int n, int n2, int n3, Color color) {
        float[] fArray = new float[]{0.0f, 0.4f, 1.0f};
        Color color2 = new Color(255, 255, 255, 200);
        Color color3 = color.darker().darker();
        RadialGradientPaint radialGradientPaint = new RadialGradientPaint(new Point2D.Float((float)n - (float)n3 * 0.3f, (float)n2 - (float)n3 * 0.3f), n3, fArray, new Color[]{color2, color, color3});
        Paint paint = graphics2D.getPaint();
        graphics2D.setPaint(radialGradientPaint);
        graphics2D.fillOval(n - n3, n2 - n3, n3 * 2, n3 * 2);
        graphics2D.setPaint(paint);
        graphics2D.setColor(color3);
        graphics2D.drawOval(n - n3, n2 - n3, n3 * 2, n3 * 2);
    }

    /** A ring of light around a glyph at the end of the hovered aspect line. */
    static void drawHighlightHalo(Graphics2D g, int x, int y, int r) {
        Stroke was = g.getStroke();
        g.setStroke(new BasicStroke(2.0f));
        g.setColor(new Color(255, 238, 170, 225));
        g.drawOval(x - r - 4, y - r - 4, (r + 4) * 2, (r + 4) * 2);
        g.setColor(new Color(255, 238, 170, 80));
        g.setStroke(new BasicStroke(1.0f));
        g.drawOval(x - r - 8, y - r - 8, (r + 8) * 2, (r + 8) * 2);
        g.setStroke(was);
    }

    /**
     * A body's label centred on its marker.
     *
     * <b>A two-letter label is drawn smaller.</b> The font is sized for one glyph on a sphere; a
     * fallback like Nessus' "Ns", or the Vertex's "Vx" and the lots added on 2026-09-14, drawn at
     * that size spilled past the marker on both sides and ran into its neighbours.
     */
    static void drawBodyLabel(Graphics2D g2, String text, int x, int y, int baseline) {
        Font was = g2.getFont();
        boolean wide = text.codePointCount(0, text.length()) > 1;
        if (wide) {
            g2.setFont(was.deriveFont(was.getSize2D() * 0.62f));
        }
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x - fm.stringWidth(text) / 2, wide ? y + fm.getAscent() / 2 - 1 : y + baseline);
        g2.setFont(was);
    }

    static void drawMoonPhase(Graphics2D graphics2D, int n, int n2, int n3, double d) {
        int n4 = n3 * 2;
        int n5 = n - n3;
        int n6 = n2 - n3;
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillOval(n5, n6, n4, n4);
        if (d <= 0.5) {
            graphics2D.setColor(Color.WHITE);
            graphics2D.fillArc(n5, n6, n4, n4, 270, 180);
            if (d <= 0.25) {
                double d2 = 1.0 - d / 0.25;
                int n7 = (int)((double)n4 * d2);
                graphics2D.setColor(Color.BLACK);
                graphics2D.fillOval(n - n7 / 2, n6, n7, n4);
            } else {
                double d3 = (d - 0.25) / 0.25;
                int n8 = (int)((double)n4 * d3);
                graphics2D.setColor(Color.WHITE);
                graphics2D.fillOval(n - n8 / 2, n6, n8, n4);
            }
        } else {
            graphics2D.setColor(Color.WHITE);
            graphics2D.fillArc(n5, n6, n4, n4, 90, 180);
            if (d <= 0.75) {
                double d4 = 1.0 - (d - 0.5) / 0.25;
                int n9 = (int)((double)n4 * d4);
                graphics2D.setColor(Color.WHITE);
                graphics2D.fillOval(n - n9 / 2, n6, n9, n4);
            } else {
                double d5 = (d - 0.75) / 0.25;
                int n10 = (int)((double)n4 * d5);
                graphics2D.setColor(Color.BLACK);
                graphics2D.fillOval(n - n10 / 2, n6, n10, n4);
            }
        }
        graphics2D.setColor(new Color(150, 150, 150));
        graphics2D.drawOval(n5, n6, n4, n4);
    }

    /**
     * The Egyptian bounds, five segments a sign, in the band between decans and signs.
     *
     * <b>Both halves come from Dignity.</b> The edges from {@code boundEdges} and the ruler
     * from {@code boundRulerOf}, so the ring cannot disagree with the score: a planet the
     * dignity table says is in its own bound is a planet standing in the segment this ring
     * draws for it. Writing the table out again here would have been a second statement of a
     * rule whose whole point is being stated once - and the two would not diverge on the day
     * it was copied, but on the day one of them was corrected.
     *
     * Drawn in the ruler's own body colour so the band is scannable at a glance: the run of
     * Saturn segments at the ends of the signs reads as a band of one colour.
     *
     * @param outer the band's outer edge   (RING_SIGN_INNER)
     * @param inner the band's inner edge   (RING_TERM_INNER)
     */
    /**
     * The inner degree scale: 360 ticks, sitting directly above the wheels.
     *
     * <b>A second scale rather than a busier first one.</b> The outer ticks live at the rim
     * with the lunar mansions, and a body drawn near the middle of the wheel is a long way
     * from them - the leader line that connects the two crossed the decans, the signs and the
     * bounds to get there, which is a lot of chart for one thin line to survive. This scale is
     * the near edge of the same measurement, so the leader has a short run and lands on ticks
     * the reader can actually count.
     *
     * Marked in tens and fives like the outer one, with the degree-in-sign written at every
     * ten so the number is readable without counting from the sign boundary.
     *
     * @param outer the scale's outer edge   (RING_TERM_INNER)
     * @param inner the scale's inner edge   (RING_DEGREE_INNER)
     */
    static void drawInnerDegreeRing(Graphics2D g, int cx, int cy, int outer, int inner,
                                     double pin) {
        java.awt.Font was = g.getFont();
        g.setColor(new Color(150, 150, 150));
        for (int d = 0; d < 360; d++) {
            double a = Math.toRadians(180.0 + pin - d);
            int depth = d % 10 == 0 ? outer - inner : (d % 5 == 0 ? 6 : 3);
            int from = outer - depth;
            g.setStroke(new BasicStroke(d % 10 == 0 ? 1.2f : 0.5f));
            g.drawLine(cx + (int) (from * Math.cos(a)), cy + (int) (from * Math.sin(a)),
                cx + (int) (outer * Math.cos(a)), cy + (int) (outer * Math.sin(a)));
        }
        // The degree within its sign, at every ten. Written between the tens rather than on
        // them, so a number never sits on the tick it labels.
        g.setFont(new Font("SansSerif", 0, 8));
        g.setColor(new Color(130, 138, 148));
        for (int d = 0; d < 360; d += 10) {
            double a = Math.toRadians(180.0 + pin - (d + 5.0));
            int mid = (outer + inner) / 2;
            String label = String.valueOf(d % 30);
            g.drawString(label,
                cx + (int) (mid * Math.cos(a)) - g.getFontMetrics().stringWidth(label) / 2,
                cy + (int) (mid * Math.sin(a)) + 3);
        }
        g.setFont(was);
    }

    static void drawBoundRing(Graphics2D g, int cx, int cy, int outer, int inner, double pin,
            java.util.function.IntFunction<Color> bodyColor) {
        java.awt.Font was = g.getFont();
        g.setFont(new Font("SansSerif", 0, 11));
        int mid = (outer + inner) / 2;
        for (int sign = 0; sign < 12; sign++) {
            double[] edges = Dignity.boundEdges(sign);
            for (int i = 0; i < edges.length - 1; i++) {
                double startLon = sign * 30.0 + edges[i];
                double endLon = sign * 30.0 + edges[i + 1];

                // The division at the segment's start. The sign boundary already has a line
                // of its own, so the first edge of each sign is left to it.
                if (i > 0) {
                    double a = Math.toRadians(180.0 + pin - startLon);
                    g.setColor(new Color(150, 150, 150, 140));
                    g.setStroke(new BasicStroke(1.0f));
                    g.drawLine(cx + (int) (outer * Math.cos(a)), cy + (int) (outer * Math.sin(a)),
                        cx + (int) (inner * Math.cos(a)), cy + (int) (inner * Math.sin(a)));
                }

                // The ruler goes at the segment's midpoint, asked for at that longitude so
                // this ring and the dignity score are answering the same question.
                double centreLon = (startLon + endLon) / 2.0;
                String ruler = Dignity.boundRulerOf(centreLon % 360.0);
                int bi = Bodies.indexOfName(ruler);
                if (bi < 0 || bi >= SkymapPanel.BODY_GLYPHS.length) {
                    continue;                   // unresolvable ruler: leave the segment blank
                }
                double c = Math.toRadians(180.0 + pin - centreLon);
                int gx = cx + (int) (mid * Math.cos(c));
                int gy = cy + (int) (mid * Math.sin(c));
                g.setColor(bodyColor.apply(bi));
                String glyph = SkymapPanel.BODY_GLYPHS[bi];
                g.drawString(glyph, gx - g.getFontMetrics().stringWidth(glyph) / 2, gy + 4);
            }
        }
        g.setFont(was);
    }

    static void drawMansionRing(Graphics2D g, int cx, int cy, int outer, int inner,
                                double pin,
                                com.zodiacomputing.ourania.astro.LunarMansions.Mansion moonMansion) {
        try {
            java.util.List<com.zodiacomputing.ourania.astro.LunarMansions.Mansion> all =
                com.zodiacomputing.ourania.astro.LunarMansions.all();

            int bandOuter = outer;
            // <b>From the chain, not from outer - 9 written here.</b> The band's inner edge is
            // also the degree scale's outer edge, so the two have to be one number: while this
            // method computed its own, the scale had no way to know where the band ended and
            // was drawn underneath it.
            int bandInner = Math.min(inner, outer - 1);
            Stroke saved = g.getStroke();
            Font savedFont = g.getFont();

            // The Moon's station first, so the boundary ticks sit on top of it.
            if (moonMansion != null) {
                double startTheta = 180.0 + pin - moonMansion.start;
                int r = (bandOuter + bandInner) / 2;
                // The mansion ring's colour, overridable in Settings. Alpha kept here: the
                // ring sits under the glyphs and a solid band would bury them.
                Color mansion = AwtPen.colorOr(ChartPalette.mansionHex(null),
                    new Color(181, 160, 227));
                g.setColor(new Color(mansion.getRed(), mansion.getGreen(), mansion.getBlue(), 110));
                g.setStroke(new BasicStroke(bandOuter - bandInner));
                g.drawArc(cx - r, cy - r, r * 2, r * 2,
                    (int) Math.round(-startTheta),
                    (int) Math.round(com.zodiacomputing.ourania.astro.LunarMansions.WIDTH));
            }

            g.setFont(new Font("SansSerif", 0, 9));
            for (com.zodiacomputing.ourania.astro.LunarMansions.Mansion m : all) {
                boolean current = moonMansion != null && moonMansion.number == m.number;
                double theta = Math.toRadians(180.0 + pin - m.start);
                g.setColor(current ? new Color(181, 160, 227) : new Color(120, 105, 150));
                g.setStroke(new BasicStroke(current ? 2.0f : 1.0f));
                g.drawLine(cx + (int) (bandOuter * Math.cos(theta)),
                    cy + (int) (bandOuter * Math.sin(theta)),
                    cx + (int) (bandInner * Math.cos(theta)),
                    cy + (int) (bandInner * Math.sin(theta)));

                // The number sits at the middle of the station, not on its cusp.
                double mid = Math.toRadians(180.0 + pin
                    - (m.start + com.zodiacomputing.ourania.astro.LunarMansions.WIDTH / 2.0));
                int labelR = (bandOuter + bandInner) / 2;
                int lx = cx + (int) (labelR * Math.cos(mid));
                int ly = cy + (int) (labelR * Math.sin(mid));
                g.setColor(current ? Color.WHITE : new Color(150, 135, 180));
                String label = String.valueOf(m.number);
                g.drawString(label, lx - (label.length() * 3), ly + 3);
            }
            g.setStroke(saved);
            g.setFont(savedFont);
        } catch (Exception e) {
            // A decorative ring is not worth losing the chart over.
        }
    }
}
