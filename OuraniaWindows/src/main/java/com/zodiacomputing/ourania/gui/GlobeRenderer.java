package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.LunarMansions;
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
    private final SkymapPanel panel;
    private final List<Piece> pieces = new ArrayList<>();

    private GlobeRenderer(Graphics2D g, Globe cam, int w, int h, double origin,
                          SkymapPanel panel) {
        this.g = g;
        this.cam = cam;
        this.w = w;
        this.h = h;
        this.origin = origin;
        this.panel = panel;
    }

    /**
     * A colour faded by how far its layer is open.
     *
     * <b>Alpha rather than a flag, because a layer folds rather than vanishing.</b> Each layer
     * carries a Bloom, so folding one is a movement the reader can follow - the same thing the
     * ring blooms do to the bands, applied to everything else that is drawn. At zero the layer
     * is skipped entirely, so a folded layer costs nothing to draw.
     */
    private Color faded(Color c, SkymapPanel.Layer layer) {
        double open = this.panel.layerOpen(layer);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
            (int) Math.round(c.getAlpha() * open));
    }

    private boolean shown(SkymapPanel.Layer layer) {
        return this.panel.layerShown(layer);
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
        GlobeRenderer r = new GlobeRenderer(g, cam, w, h, panel.pinLongitude(), panel);
        r.turning = turning;
        r.stacked = Settings.globeStackedRings();
        r.bowed = Settings.globeAspectArcs();

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
        if (!turning && r.shown(SkymapPanel.Layer.SIGNS)) {
            r.signPlane();
        }

        r.focusWedges(panel);
        if (r.shown(SkymapPanel.Layer.DEGREES)) {
            r.degreeRing();
        }
        if (r.shown(SkymapPanel.Layer.MANSIONS)) {
            r.mansionRing();
        }
        if (r.shown(SkymapPanel.Layer.SIGNS)) {
            r.zodiacBand();
            r.signMeridians();
        }
        if (r.shown(SkymapPanel.Layer.BOUNDS)) {
            r.boundRing();
        }
        if (r.shown(SkymapPanel.Layer.DECANS)) {
            r.decanRing();
        }
        if (r.shown(SkymapPanel.Layer.HOUSES)) {
            r.houseMeridians(panel.activeCusps);
            r.houseNumbers(panel.activeCusps);
        }
        // The three charts as ribbons, each lying in its own plane. Painted before the
        // chords and the bodies so they read as the ground those sit on.
        if (panel.outerRingDrawn()) {
            int outerDeck = panel.ringDeck(1);
            r.ribbon(partnerR, shade(chartInk(outerDeck), 54),
                inclinationOf(outerDeck, r.stacked), liftOf(outerDeck, r.stacked), turning);
        }
        if (panel.triRingDrawn()) {
            int triDeck = panel.ringDeck(2);
            r.ribbon(skyR, shade(chartInk(triDeck), 54),
                inclinationOf(triDeck, r.stacked), liftOf(triDeck, r.stacked), turning);
        }

        if (r.shown(SkymapPanel.Layer.ASPECTS)) {
            r.aspectChords(panel, shells);
        }
        if (r.shown(SkymapPanel.Layer.NATAL)) {
            int innerDeck = panel.ringDeck(0);
            r.ribbon(natalR, r.faded(shade(chartInk(innerDeck), 54), SkymapPanel.Layer.NATAL),
                inclinationOf(innerDeck, r.stacked), liftOf(innerDeck, r.stacked), turning);
            r.bodies(panel.bLon, panel.bValid, natalR, SkymapPanel.AngleRole.ANCHOR, panel,
                false, 0);
        }
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
     * Where each wheel's bodies ride, given which chart is on it.
     *
     * <b>One statement of it, because the painter and the hit test both need it.</b> This is
     * the flat wheel's Geometry problem in a second view: a shell radius computed twice is a
     * body you can see and cannot click, and that defect has already been found in this panel
     * once.
     *
     * <b>By chart rather than by slot, and fixed rather than bloomed.</b> These used to slide
     * inward as rings were folded, so the space was always filled - and the cost was that
     * taking one chart out moved the other two. On a globe that reads as the remaining charts
     * changing rank, which is exactly the confusion the decks were introduced to end. Chart A
     * is at the natal radius whenever it is drawn, Chart B at the partner radius, the sky at
     * the sky radius, and folding one leaves a gap where it was rather than closing ranks.
     *
     * @return the radius for wheel 0, 1 and 2, in that order
     */
    static double[] shellRadii(SkymapPanel panel) {
        return new double[] {
            DECK_RADIUS[panel.ringDeck(0)],
            DECK_RADIUS[panel.ringDeck(1)],
            DECK_RADIUS[panel.ringDeck(2)],
        };
    }

    /** The radius of each deck, middle then lower then upper. */
    private static final double[] DECK_RADIUS = {
        Globe.SHELL_NATAL, Globe.SHELL_PARTNER, Globe.SHELL_SKY,
    };

    /**
     * Which plane a deck rides in: the middle one flat, the other two either tilted across it
     * or stacked above and below it.
     *
     * <b>Beside shellRadii, and for the same reason.</b> Where a body is on the globe is a
     * radius and a plane; if the hit test knew one and not the other it would be the
     * see-it-but-cannot-click-it defect again, in the half that is easier to miss. Both take a
     * deck rather than a wheel, so which chart a chip has taken out cannot move the others.
     *
     * <b>The layout comes in as an argument rather than being read here.</b> Settings.get
     * opens and parses the file on every call, and this is asked once per body, once per
     * aspect line and once per ribbon - so reading it inside would put several hundred file
     * reads in a frame that is meant to take forty milliseconds. It is read once where a frame
     * or a click begins and carried down.
     */
    static double inclinationOf(int deck, boolean stacked) {
        if (stacked) {
            return 0.0;
        }
        return deck == SkymapPanel.DECK_LOWER ? Globe.INCLINE_PARTNER
            : (deck == SkymapPanel.DECK_UPPER ? Globe.INCLINE_SKY : 0.0);
    }

    /**
     * How far off the middle plane a deck sits, in world units.
     *
     * Only one of this and the tilt is ever non-zero: the rings are crossed or they are
     * stacked, never both. The height joins a body stack rather than replacing it, so a crowd
     * on a lifted ring still steps up its shell from wherever that ring starts.
     */
    static double liftOf(int deck, boolean stacked) {
        if (!stacked) {
            return 0.0;
        }
        return deck == SkymapPanel.DECK_LOWER ? Globe.LIFT_PARTNER
            : (deck == SkymapPanel.DECK_UPPER ? Globe.LIFT_SKY : 0.0);
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
        // Once for the whole hit test, for the reason set out on inclinationOf.
        boolean stacked = Settings.globeStackedRings();
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
                int deck = panel.ringDeck(ring);
                double[] p = Globe.onShell(lon[i], origin, shells[ring],
                    liftOf(deck, stacked) + level[i] * Globe.STACK_STEP,
                    inclinationOf(deck, stacked));
                Globe.Projected q = cam.project(p[0], p[1], p[2], w, h);
                if (!q.visible) {
                    continue;
                }
                double d = Math.hypot(q.x - px, q.y - py);
                if (d < bestDist) {
                    bestDist = d;
                    best = SkymapPanel.packHit(i, ring);
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

    /** Half the width of a chart's band - the part you see looking at it from the side. */
    private static final double RIBBON_HALF = 0.075;

    /**
     * One point on a chart's band: at its radius, standing a height above its own plane.
     *
     * <b>A cylinder, not a sphere.</b> Globe.onShell puts a point on a sphere of the given
     * radius, so its horizontal reach shrinks as it rises - which is right for a body stacked
     * up its shell and wrong for a band, whose wall has to stay the same distance out all the
     * way round. The tilt is the same x-axis rotation onShell applies, so a band and the
     * bodies riding on it agree about where the plane is.
     *
     * <b>A lifted band is a cylinder cut from higher up the same sphere.</b> The bodies on a
     * stacked ring ride at a latitude, so their horizontal reach has already shrunk; a band
     * drawn at the full radius would stand outside its own planets by the whole difference.
     * The shrink is taken once, here, from the same square root onShell uses.
     */
    private double onBand(double lon, double radius, double height, double inclination,
            double lift, int axis) {
        double t = Math.toRadians(lon - this.origin);
        double reach = Math.sqrt(Math.max(0.0, radius * radius - lift * lift));
        double x = -reach * Math.cos(t);
        double z = reach * Math.sin(t);
        double y = height + lift;
        double c = Math.cos(inclination);
        double s = Math.sin(inclination);
        if (axis == 0) {
            return x;
        }
        return axis == 1 ? y * c - z * s : y * s + z * c;
    }

    private double[] bandPoint(double lon, double radius, double height, double inclination,
            double lift) {
        return new double[] {
            onBand(lon, radius, height, inclination, lift, 0),
            onBand(lon, radius, height, inclination, lift, 1),
            onBand(lon, radius, height, inclination, lift, 2),
        };
    }

    /**
     * One chart's ring as a band standing in its own plane, the way a ring sits on a finger.
     *
     * <b>A line does not say which way a plane faces.</b> The three charts were three thin
     * circles, and two of them are tilted - partner one way, sky the other - so the only cue
     * to which plane a body belonged to was how its circle happened to cross the others at
     * that camera angle. Turn the globe and the cue changes.
     *
     * <b>Standing, not lying.</b> The first attempt was a flat washer in the plane, which is
     * a disc seen edge-on from the side and vanishes exactly where the camera spends most of
     * its time. David: "like how a ring wraps flat around a finger, the thinness only visible
     * looking up and down at it from the side view." So it is the wall of a very short
     * cylinder: full width from the side, a paper edge from directly above. That is the way
     * round that shows a plane at the angles this globe is actually read at.
     *
     * Each longitude step is its own quad rather than one polygon round the whole circle -
     * the wall's near half and far half overlap in projection, and a single polygon under an
     * even-odd fill would cancel where they meet. That is the same defect that once tore holes
     * in the sphere; separate quads cannot express it, and they depth-sort individually so the
     * near wall paints over the far one.
     */
    private void ribbon(double radius, Color fill, double inclination, double lift,
                        boolean turning) {
        final int steps = 96;
        // <b>Rims only while the hand is moving.</b> Three bands are two hundred and
        // eighty-eight quads a frame, and measuring rather than assuming: they took a drag at
        // 1100 pixels from 24ms to 40ms, which is the difference between turning a globe and
        // fighting one. The filled shells are already left out for exactly this reason. The
        // rims are two polylines and they carry the shape on their own, so a band still reads
        // as a plane mid-drag and fills back in the moment the hand stops - which is when a
        // reader is looking at it rather than at the motion.
        for (int i = 0; !turning && i < steps; i++) {
            double la = (i * 360.0) / steps;
            double lb = ((i + 1) * 360.0) / steps;
            quad(bandPoint(la, radius, -RIBBON_HALF, inclination, lift),
                bandPoint(lb, radius, -RIBBON_HALF, inclination, lift),
                bandPoint(lb, radius, RIBBON_HALF, inclination, lift),
                bandPoint(la, radius, RIBBON_HALF, inclination, lift),
                fill);
        }
        // The two rims, which are what a band reads by when it turns edge-on.
        Color edge = shade(fill, Math.min(255, fill.getAlpha() * 3));
        double[][] top = new double[steps + 1][];
        double[][] bottom = new double[steps + 1][];
        for (int i = 0; i <= steps; i++) {
            double lon = (i * 360.0) / steps;
            top[i] = bandPoint(lon, radius, RIBBON_HALF, inclination, lift);
            bottom[i] = bandPoint(lon, radius, -RIBBON_HALF, inclination, lift);
        }
        polyline(top, edge, 1.0f);
        polyline(bottom, edge, 1.0f);
    }

    /**
     * The colour each chart's ribbon is drawn in - Chart A gold, Chart B blue, the sky silver.
     *
     * <b>One statement for the three ribbons.</b> David named these three, and they live in
     * one place so that renaming them is one edit rather than three.
     *
     * The glyphs are deliberately not on this palette: their colour comes from AngleRole and
     * encodes a different fact - whether a point is an anchor, a second person or a passing
     * sky - which stays true whatever a ribbon is tinted. Two palettes for two questions is
     * not the duplication this project keeps finding; it would only become that if one were
     * made to stand in for the other.
     */
    static Color chartInk(int ring) {
        if (ring == 1) {
            return new Color(120, 170, 225);        // Chart B, blue
        }
        if (ring == 2) {
            return new Color(198, 206, 216);        // the sky, silver
        }
        return new Color(226, 196, 96);             // Chart A, gold
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
            polyline(arc, faded(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 190),
                SkymapPanel.Layer.SIGNS), 2.4f);

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

    /** Degrees from one longitude round to the next, always forward. */
    private static double arc(double from, double to) {
        return ((to - from) % 360.0 + 360.0) % 360.0;
    }


    /**
     * The sign and the house the hovered body stands in, lit and pushed outward.
     *
     * <b>The globe could not say where a body was.</b> A glyph sits on a ring at a longitude,
     * and reading its sign off meant following the ring round to the zodiac by eye and its
     * house by counting cusps - work the flat wheel does for you by putting the glyph inside a
     * wedge. This puts it back: rest on a body and its two wedges light up, so the answer is
     * the shape around it rather than something to work out.
     *
     * <b>Bulged, because colour alone is not enough here.</b> Everything in this plane is
     * translucent and half of it overlaps, so a brighter patch reads as one more overlap. A
     * wedge that reaches past the ring it belongs to is unambiguous from any angle.
     */
    private void focusWedges(SkymapPanel panel) {
        int body = panel.focusedBody();
        if (body < 0) {
            return;
        }
        double lon = panel.focusedLongitude();
        if (Double.isNaN(lon)) {
            return;
        }
        int sign = ((int) Math.floor(lon / 30.0) % 12 + 12) % 12;
        Color ink = SkymapPanel.elementColorFor(Zodiac.elementIndex(sign));

        // <b>The sphere wedge is the thing that lights.</b> This lit only the flat sector for
        // a while, which made the twelve wedges over the sphere decoration - they were the
        // shape the body is actually standing in, and selecting it lit something else. The
        // wedge brightens and swells past the surface, so it reads as the slice of sky coming
        // forward rather than as a patch of colour changing.
        if (this.shown(SkymapPanel.Layer.SIGNS)) {
            wedgeOnSphere(sign * 30.0, sign * 30.0 + 30.0, Globe.SHELL_SIGN_INNER + 0.09,
                faded(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 74),
                    SkymapPanel.Layer.SIGNS));
            // And its segment of the flat ring, so the answer is legible from edge-on too -
            // seen along the plane the sphere wedge is a sliver and the ring is not.
            quadRing(sign * 30.0, sign * 30.0 + 30.0,
                Globe.SHELL_SIGN_INNER, Globe.SHELL_SIGN_OUTER + 0.07,
                faded(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 150),
                    SkymapPanel.Layer.SIGNS));
        }

        double[] cusps = panel.activeCusps;
        if (cusps == null || cusps.length < 13) {
            return;
        }
        int house = Zodiac.houseOf(lon, cusps);
        if (house >= 1 && house <= 12 && this.shown(SkymapPanel.Layer.HOUSES)) {
            double from = cusps[house];
            double span = arc(from, cusps[house == 12 ? 1 : house + 1]);
            // The house stays flat because the houses are: they are spokes in the plane, and
            // a house wedge standing up out of it would be claiming a shape nothing else in
            // the view gives them.
            wedge(from, from + span, 0.10, Globe.SHELL_HOUSE + 0.06,
                faded(new Color(236, 224, 188, 70), SkymapPanel.Layer.HOUSES));
        }
    }

    /**
     * A filled sector of the plane between two longitudes, from one radius out to another.
     *
     * Flat, like everything else the reader measures against. It filled a slice of the sphere
     * pole to pole until the divisions were flattened; a wedge standing up out of the plane
     * was the same wireframe-globe mistake in a brighter colour.
     *
     * Tessellated along the arc rather than drawn as one polygon, because the outer edge is a
     * curve and a single quadrilateral would cut the corner off it.
     */
    private void wedge(double lon0, double lon1, double r0, double r1, Color fill) {
        int steps = Math.max(3, (int) Math.ceil(Math.abs(lon1 - lon0) / 6.0));
        for (int i = 0; i < steps; i++) {
            double la = lon0 + ((lon1 - lon0) * i) / steps;
            double lb = lon0 + ((lon1 - lon0) * (i + 1)) / steps;
            quad(Globe.onShell(la, this.origin, r0, 0.0),
                Globe.onShell(lb, this.origin, r0, 0.0),
                Globe.onShell(lb, this.origin, r1, 0.0),
                Globe.onShell(la, this.origin, r1, 0.0),
                fill);
        }
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
            double from = sign * 30.0;

            // <b>A wedge over the sphere, faint.</b> Twelve of these are the surface the
            // bodies sit inside, and they carry their own edges - which is what the sign
            // boundaries were being drawn twice for. Great circles on top of them read as the
            // wireframe of a globe; the fills alone read as a sphere divided into signs.
            wedgeOnSphere(from, from + 30.0, Globe.SHELL_SIGN_INNER,
                faded(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 26),
                    SkymapPanel.Layer.SIGNS));

            // <b>And a flat ring at the equator, brighter.</b> The plane is where a longitude
            // means what it says, so the zodiac gets a band there that a reader can measure
            // against - the degree scale hangs off its outer edge. This was a full disc from
            // the band to the centre, which washed the whole chart in sign colour and left
            // the aspect network to be read through it.
            quadRing(from, from + 30.0, Globe.SHELL_SIGN_INNER, Globe.SHELL_SIGN_OUTER,
                faded(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 96),
                    SkymapPanel.Layer.SIGNS));
        }
    }

    /**
     * A translucent slice of the sphere between two longitudes, pole to pole.
     *
     * <b>Shaded by how much each cell faces the reader, which is what makes it round.</b> Flat
     * fill on a tessellated wedge reads as a folded paper fan: the silhouette is a circle and
     * nothing inside it says so. A cell turned toward the camera is nearest and strongest; one
     * at the limb is edge-on and nearly gone. That single term is the difference between a
     * faceted shape and a sphere.
     *
     * <b>And finer than it was.</b> Seven rows across a hundred and eighty degrees is
     * twenty-six degrees of arc each, which the eye reads as flats. The cells are cheap; the
     * shading was the missing thing, but the two together are what finish it.
     */
    private void wedgeOnSphere(double lon0, double lon1, double radius, Color fill) {
        // <b>Bands with curved edges, not a grid of flats.</b> A wedge was a mesh of
        // quadrilaterals, and a quadrilateral has straight sides - so every cell boundary was
        // a chord across an arc and the sphere came out faceted however many cells it was cut
        // into. More cells made the facets smaller and never made them curves. Each band is
        // one filled path now, its two long edges traced along the meridians themselves, so
        // the edge is the curve rather than an approximation of it.
        //
        // It is also far cheaper: six bands to a sign instead of seventy cells, which is what
        // paid for tracing thirty-six points down each edge.
        int bands = Math.max(2, (int) Math.ceil(Math.abs(lon1 - lon0) / 5.0));
        for (int i = 0; i < bands; i++) {
            double la = lon0 + ((lon1 - lon0) * i) / bands;
            double lb = lon0 + ((lon1 - lon0) * (i + 1)) / bands;
            bandPath(la, lb, radius, fill);
        }
    }

    /**
     * One band of a sphere between two longitudes, as a single filled path.
     *
     * The outline runs up one meridian and back down the other, so both long edges are the
     * arcs they are meant to be. Shaded by the band's own facing, which is smooth enough at
     * five degrees a band that the reader sees a gradient rather than steps.
     */
    private void bandPath(double lonA, double lonB, double radius, Color fill) {
        final int steps = 36;
        double[] ax = new double[steps + 1];
        double[] ay = new double[steps + 1];
        double[] ad = new double[steps + 1];
        double[] bx = new double[steps + 1];
        double[] by = new double[steps + 1];
        double[] bd = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double phi = -Globe.FILL_SPAN + (2 * Globe.FILL_SPAN * i) / steps;
            double y = radius * Math.sin(phi);
            Globe.Projected qa = at(Globe.onShell(lonA, this.origin, radius, y));
            Globe.Projected qb = at(Globe.onShell(lonB, this.origin, radius, y));
            if (!qa.visible || !qb.visible) {
                return;                         // the band crosses the lens; skip it whole
            }
            ax[i] = qa.x;
            ay[i] = qa.y;
            ad[i] = qa.depth;
            bx[i] = qb.x;
            by[i] = qb.y;
            bd[i] = qb.depth;
        }
        // <b>Cut where the strip would lie across itself.</b> Filled whole, the far half of
        // a band lands on the near half and the even-odd rule subtracts one from the other -
        // those were the notches bitten out of the sphere - and the same thing happens again
        // where each meridian folds over its apex near the crown. Globe.bandRuns says where
        // to cut for both; each run is then its own polygon, its own shade and its own place
        // in the depth sort, which is the honest answer to all three.
        double[] mid = new double[steps];
        for (int j = 0; j < steps; j++) {
            mid[j] = (ad[j] + ad[j + 1] + bd[j] + bd[j + 1]) / 4.0;
        }
        int[] runs = Globe.bandRuns(mid, this.cam.distance, ay, by);
        for (int r = 0; r + 1 < runs.length; r++) {
            fillRun(ax, ay, ad, bx, by, bd, runs[r], runs[r + 1], radius, fill);
        }
    }

    /**
     * One same-facing stretch of a band, as a polygon that does not cross itself.
     *
     * Up one meridian from {@code from} to {@code to} and back down the other, so both long
     * edges stay the arcs they are. Runs share their boundary vertex, so neighbours meet
     * without a seam between them.
     */
    private void fillRun(double[] ax, double[] ay, double[] ad,
            double[] bx, double[] by, double[] bd,
            int from, int to, double radius, Color fill) {
        int span = (to - from + 1) * 2;
        int[] xs = new int[span];
        int[] ys = new int[span];
        double depth = 0;
        int n = 0;
        for (int i = from; i <= to; i++) {
            xs[n] = (int) Math.round(ax[i]);
            ys[n] = (int) Math.round(ay[i]);
            depth += ad[i];
            n++;
        }
        for (int i = to; i >= from; i--) {
            xs[n] = (int) Math.round(bx[i]);
            ys[n] = (int) Math.round(by[i]);
            depth += bd[i];
            n++;
        }
        double sits = depth / n;
        double away = (sits - this.cam.distance) / Math.max(1e-6, radius);
        double facing = Math.max(0.0, Math.min(1.0, 0.5 - 0.5 * away));
        final Color ink = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(),
            (int) Math.round(fill.getAlpha() * (0.30 + 0.70 * facing)));
        final int count = n;
        this.pieces.add(new Piece(sits, () -> {
            this.g.setColor(ink);
            this.g.fillPolygon(xs, ys, count);
        }));
    }

    /**
     * A cell dimmed by how far it has turned away from the reader.
     *
     * The outward normal at a point on a sphere is the point itself, so how much a cell faces
     * the camera is carried by its depth: a cell on the near face is one radius closer than
     * the centre, one at the limb is level with it, one on the far face is a radius beyond.
     * Reading it off the projection saves unrotating a normal per cell and cannot drift from
     * the projection the way a separately derived one could.
     *
     * Never to nothing - the far side is meant to be seen through, not erased.
     */
    private Color shadeByFacing(Color fill, double[] a, double[] b, double[] c, double[] d,
                                double radius) {
        double depth = 0;
        int seen = 0;
        for (double[] pt : new double[][] {a, b, c, d}) {
            Globe.Projected q = at(pt);
            if (q.visible) {
                depth += q.depth;
                seen++;
            }
        }
        if (seen == 0) {
            return fill;
        }
        double away = ((depth / seen) - this.cam.distance) / Math.max(1e-6, radius);
        double facing = Math.max(0.0, Math.min(1.0, 0.5 - 0.5 * away));
        double lift = 0.30 + 0.70 * facing;
        return new Color(fill.getRed(), fill.getGreen(), fill.getBlue(),
            (int) Math.round(fill.getAlpha() * lift));
    }

    /** One segment of a flat ring in the ecliptic plane. */
    private void quadRing(double lon0, double lon1, double r0, double r1, Color fill) {
        int steps = Math.max(3, (int) Math.ceil(Math.abs(lon1 - lon0) / 6.0));
        for (int i = 0; i < steps; i++) {
            double la = lon0 + ((lon1 - lon0) * i) / steps;
            double lb = lon0 + ((lon1 - lon0) * (i + 1)) / steps;
            quad(Globe.onShell(la, this.origin, r0, 0.0),
                Globe.onShell(lb, this.origin, r0, 0.0),
                Globe.onShell(lb, this.origin, r1, 0.0),
                Globe.onShell(la, this.origin, r1, 0.0),
                fill);
        }
    }

    /**
     * The house numbers, in the plane, each in the middle of its own house.
     *
     * <b>They followed the houses.</b> They were at the poles because the houses were wedges
     * of a sphere and a pole is where those converge; with the cusps drawn as flat spokes the
     * poles have nothing to do with a house any more, and a number floating there would be
     * labelling empty sky. In the middle of the sector it names is where the flat wheel puts
     * it and where a reader looks for it.
     */
    /** Where house i's number is written, in world coordinates. */
    private static double[] houseLabelAt(double[] cusps, int house, double origin) {
        double span = ((cusps[house == 12 ? 1 : house + 1] - cusps[house]) % 360.0 + 360.0)
            % 360.0;
        return Globe.onShell(cusps[house] + span / 2.0, origin, Globe.SHELL_HOUSE - 0.10, 0.0);
    }

    /**
     * Which house number the cursor is over, or -1.
     *
     * <b>Reads houseLabelAt, which is where the number is drawn.</b> Same agreement the body
     * and degree hit tests keep: a label the reader can see and cannot point at is worse than
     * no label, because they will try.
     */
    static int houseNumberAt(Globe cam, int w, int h, SkymapPanel panel, int px, int py) {
        double[] cusps = panel.activeCusps;
        if (cusps == null || cusps.length < 13
            || !panel.layerShown(SkymapPanel.Layer.HOUSES)) {
            return -1;
        }
        double origin = panel.pinLongitude();
        int best = -1;
        double bestDist = 14.0;
        for (int i = 1; i <= 12; i++) {
            double[] pt = houseLabelAt(cusps, i, origin);
            Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
            if (!q.visible) {
                continue;
            }
            double dist = Math.hypot(q.x - px, q.y - py);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private void houseNumbers(double[] cusps) {
        if (cusps == null || cusps.length < 13) {
            return;
        }
        int lit = this.panel.focusedHouse();
        for (int i = 1; i <= 12; i++) {
            boolean here = i == lit;
            billboard(houseLabelAt(cusps, i, this.origin), String.valueOf(i),
                faded(here ? new Color(255, 238, 170) : new Color(206, 208, 216),
                    SkymapPanel.Layer.HOUSES), here ? 15 : 12);
        }

        // <b>The house cut through the sphere, and only from its number.</b> A house is a
        // division of the whole sky, so it does have a shape up there - but nothing else in
        // this view gives the houses one, and carving the sphere every time a body was picked
        // would put a second solid wedge over the sign wedge that body is standing in. Asking
        // for it by pointing at the number is a deliberate act, and the only time the reader
        // wants the house rather than the placement.
        if (lit >= 1 && lit <= 12) {
            double span = arc(cusps[lit], cusps[lit == 12 ? 1 : lit + 1]);
            wedgeOnSphere(cusps[lit], cusps[lit] + span, Globe.SHELL_SIGN_INNER + 0.11,
                faded(new Color(236, 224, 188, 66), SkymapPanel.Layer.HOUSES));
            wedge(cusps[lit], cusps[lit] + span, 0.10, Globe.SHELL_HOUSE + 0.06,
                faded(new Color(236, 224, 188, 74), SkymapPanel.Layer.HOUSES));
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
    /**
     * Which whole degree the cursor is over, or -1.
     *
     * <b>The same tick positions the ring draws, so the hover lands where the mark is.</b>
     * Three hundred and sixty of them is too many to test one at a time from a mouse move, so
     * this goes the other way round: turn the cursor back into a longitude by projecting the
     * candidate degrees and taking the nearest. Cheap, and it cannot disagree with the drawing
     * the way an independently derived angle would.
     */
    static int degreeAt(Globe cam, int w, int h, SkymapPanel panel, int px, int py) {
        if (!panel.layerShown(SkymapPanel.Layer.DEGREES)) {
            return -1;
        }
        double origin = panel.pinLongitude();
        double inner = Globe.SHELL_SIGN_OUTER + 0.03;
        int best = -1;
        double bestDist = 14.0;
        for (int d = 0; d < 360; d++) {
            // Measured at the middle of the tick, so a long tick and a short one are as easy
            // to hit as each other.
            double[] pt = Globe.onShell(d + 0.5, origin, inner + 0.09, 0.0);
            Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
            if (!q.visible) {
                continue;
            }
            double dist = Math.hypot(q.x - px, q.y - py);
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
    }

    /**
     * The lunar station under a point, 1 to 28, or -1.
     *
     * <b>Sampled along the arc rather than at a label.</b> The house numbers are hit-tested at
     * the number itself, which is right for twelve of them - a house is read by its number and
     * the number is what a reader points at. Twenty-eight stations in one thin band is a
     * different problem: the numbers are small and close, and pointing at the band is the
     * natural gesture. Sampling every degree across each station means anywhere on the band
     * answers, and it needs no inverse projection - which is what keeps this honest under a
     * camera the reader can turn to any angle.
     */
    static int mansionAt(Globe cam, int w, int h, SkymapPanel panel, int px, int py) {
        if (!panel.layerShown(SkymapPanel.Layer.MANSIONS)) {
            return -1;
        }
        double origin = panel.pinLongitude();
        double mid = (Globe.SHELL_MANSION_INNER + Globe.SHELL_MANSION_OUTER) / 2.0;
        int best = -1;
        double bestDist = 13.0;
        for (int m = 1; m <= LunarMansions.COUNT; m++) {
            double start = (m - 1) * LunarMansions.WIDTH;
            for (double step = 0.5; step < LunarMansions.WIDTH; step += 1.0) {
                double[] pt = Globe.onShell(start + step, origin, mid, 0.0);
                Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
                if (!q.visible) {
                    continue;
                }
                double dist = Math.hypot(q.x - px, q.y - py);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = m;
                }
            }
        }
        return best;
    }

    /**
     * The 28 lunar mansions, as the outermost band of the globe.
     *
     * <b>The flat wheel has had them all along and the globe had nothing.</b> Every other
     * division of the zodiac this chart draws - signs, decans, bounds, degrees - was on the
     * sphere already; the mansions were the one ring a reader could find in one view and not
     * the other, which makes switching views lose information rather than change how it is
     * shown.
     *
     * Drawn in the plane, like the degree scale and the sign ring, because a station is a span
     * of longitude and nothing else - it says nothing about latitude, so wrapping it around
     * the sphere would be claiming something the tradition does not.
     *
     * <b>Folded by its own chip, in both views.</b> It was drawn unconditionally in both to
     * begin with, matching each other but matching nothing else - every neighbouring ring can
     * be put away and this one could not. The chip drives Layer.MANSIONS, which the flat
     * wheel's ring, this one, and both hit tests all read, so one button means one thing.
     */
    private void mansionRing() {
        double inner = Globe.SHELL_MANSION_INNER;
        double outer = Globe.SHELL_MANSION_OUTER;
        double mid = (inner + outer) / 2.0;
        // The one hue, from the one place the flat ring reads it, so the two bands cannot
        // drift to different lavenders.
        // <b>The fade goes on at each use, not once on the base.</b> shade() replaces an
        // alpha rather than scaling it, so a base faded up front would have its bloom thrown
        // away by the very next call - the ring would snap in and out instead of blooming
        // with its chip the way every ring beside it does.
        Color base = ChartPalette.colorOr(ChartPalette.mansionHex(null),
            new Color(181, 160, 227));
        LunarMansions.Mansion moon = this.panel.moonMansion();
        int lit = this.panel.focusedMansion();

        // The Moon's own station, washed in first so the boundaries sit on top of it.
        if (moon != null) {
            quadRing(moon.start, moon.end(), inner, outer, wash(base, 70));
        }
        // And the station under the cursor, brighter, plus its slice of the sphere - the same
        // answer a hovered degree tick gives, for the same reason: a band that lights only its
        // own thickness is pointing at itself rather than at the sky it names.
        if (lit >= 1) {
            LunarMansions.Mansion m = LunarMansions.byNumber(lit);
            quadRing(m.start, m.end(), inner, outer, wash(base, 130));
            wedgeOnSphere(m.start, m.end(), Globe.SHELL_SIGN_INNER + 0.09, wash(base, 60));
        }

        // The band's two edges, so the stations read as divisions of something.
        polyline(Globe.equator(this.origin, inner, 144), wash(base, 130), 1.0f);
        polyline(Globe.equator(this.origin, outer, 144), wash(base, 130), 1.0f);

        for (int i = 1; i <= LunarMansions.COUNT; i++) {
            LunarMansions.Mansion m = LunarMansions.byNumber(i);
            boolean here = i == lit;
            boolean moonHere = moon != null && moon.number == i;
            segment(Globe.onShell(m.start, this.origin, inner, 0.0),
                Globe.onShell(m.start, this.origin, outer, 0.0),
                wash(base, here || moonHere ? 235 : 150),
                here ? 2.0f : (moonHere ? 1.6f : 0.9f));
            // The number sits in the middle of the station, not on its cusp - a boundary
            // belongs to neither side and a number on one reads as labelling both.
            billboard(Globe.onShell(m.start + LunarMansions.WIDTH / 2.0, this.origin, mid, 0.0),
                String.valueOf(i),
                here ? wash(new Color(255, 255, 255), 245)
                    : (moonHere ? wash(base, 245) : wash(base, 185)),
                here ? 11 : 9);
        }
    }

    /** One of the mansion band's inks, at the alpha it wants and the bloom it is at. */
    private Color wash(Color c, int alpha) {
        return faded(shade(c, alpha), SkymapPanel.Layer.MANSIONS);
    }

    private void degreeRing() {
        // <b>Outside the signs, where a scale belongs.</b> It sat inside the sign band, which
        // put the finest division of the zodiac underneath the coarsest and left the ticks
        // competing with the aspect network for the same space. Around the outside it is the
        // rim of the whole thing, which is where the flat wheel has always kept it.
        double inner = Globe.SHELL_SIGN_OUTER + 0.03;
        int lit = this.panel.focusedDegree();
        for (int d = 0; d < 360; d++) {
            boolean sign = d % 30 == 0;
            boolean ten = d % 10 == 0;
            boolean here = d == lit;
            // <b>The hovered tick stands out of the scale.</b> A degree is a hair's width on a
            // ring of three hundred and sixty, so brightening one is not enough to find it -
            // it has to be longer than its neighbours to be the one the reader is pointing at.
            double depth = here ? Globe.TICK_HOVER_REACH
                : (sign ? 0.20 : (ten ? 0.13 : (d % 5 == 0 ? 0.08 : 0.05)));
            Color ink = here ? new Color(255, 238, 170, 245)
                : (sign ? new Color(214, 218, 226, 225)
                    : (ten ? new Color(172, 178, 190, 195) : new Color(138, 144, 156, 150)));
            segment(Globe.onShell(d, this.origin, inner, 0.0),
                Globe.onShell(d, this.origin, inner + depth, 0.0),
                faded(ink, SkymapPanel.Layer.DEGREES),
                here ? 2.2f : (sign ? 1.4f : (ten ? 1.0f : 0.6f)));
        }

        // <b>And the degree's own slice, so a tick names a place rather than a mark.</b>
        // One degree of the chart, which is what the reader is asking about when they point
        // at a tick: not the line, what stands behind it.
        //
        // <b>Through the sphere as well as across the plane.</b> Lighting only the flat sector
        // made the same mistake the sign highlight made - the sphere is where the bodies are,
        // so a degree that lights only the disc is pointing at half of itself.
        if (lit >= 0) {
            wedgeOnSphere(lit, lit + 1.0, Globe.SHELL_SIGN_INNER + 0.09,
                faded(new Color(255, 238, 170, 96), SkymapPanel.Layer.DEGREES));
            quadRing(lit, lit + 1.0, 0.10, inner + Globe.TICK_HOVER_REACH,
                faded(new Color(255, 238, 170, 70), SkymapPanel.Layer.DEGREES));
        }
        // The scale itself, so the ticks hang off a line rather than floating.
        polyline(Globe.equator(this.origin, inner, 144),
            faded(new Color(158, 164, 176, 170), SkymapPanel.Layer.DEGREES), 1.0f);
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
                    SkymapPanel.glyphOf(bi),
                    faded(SkymapPanel.bodyInkFor(bi), SkymapPanel.Layer.BOUNDS), 10);
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
                        SkymapPanel.glyphOf(bi),
                        faded(SkymapPanel.bodyInkFor(bi), SkymapPanel.Layer.DECANS), 10);
                    continue;
                }
            }
            int face = Zodiac.triplicityDecanSignIndex(sign, d % 3 + 1);
            billboard(Globe.onShell(mid, this.origin, Globe.SHELL_DECAN, 0.0),
                SkymapPanel.zodiacSymbol(face),
                faded(SkymapPanel.elementColorFor(Zodiac.elementIndex(face)),
                    SkymapPanel.Layer.DECANS), 10);
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
        // <b>Flat spokes, not great circles.</b> They were drawn as meridians pole to pole,
        // which is where a house boundary genuinely goes - a house divides the whole sky - and
        // it was the wrong picture anyway: twelve great circles read as the wireframe of a
        // globe rather than as the divisions of a chart, and they wrapped over a zodiac that
        // is a flat plane. Everything the reader measures against is in the ecliptic plane, so
        // the divisions of it belong there too. Same spokes the flat wheel draws, seen in
        // perspective.
        for (int i = 1; i <= 12; i++) {
            boolean angle = i == 1 || i == 4 || i == 7 || i == 10;
            segment(Globe.onShell(cusps[i], this.origin, 0.02, 0.0),
                Globe.onShell(cusps[i], this.origin, Globe.SHELL_HOUSE, 0.0),
                faded(angle ? new Color(226, 214, 184, 220) : new Color(150, 152, 164, 140),
                    SkymapPanel.Layer.HOUSES), angle ? 1.8f : 1.0f);
        }
        // The rim the spokes end on, so the houses read as a ring rather than as loose lines.
        polyline(Globe.equator(this.origin, Globe.SHELL_HOUSE, 96),
            faded(new Color(150, 152, 164, 120), SkymapPanel.Layer.HOUSES), 0.9f);
    }

    /**
     * The twelve sign boundaries, in the plane.
     *
     * <b>The zodiac is a flat plane, so its divisions are flat too.</b> These were great
     * circles wrapping the sphere, which is what put a wireframe globe on screen the moment
     * the sign layer was switched on - the one thing this view was supposed to stop being. A
     * boundary of a disc is a radius of that disc.
     *
     * They run from the centre out through the whole plane rather than only across the sign
     * band, so a body anywhere inside can be read against the sign it falls in - which is the
     * job the flat wheel's sign spokes do.
     */
    private void signMeridians() {
        for (int sign = 0; sign < 12; sign++) {
            segment(Globe.onShell(sign * 30.0, this.origin, Globe.SHELL_SIGN_INNER, 0.0),
                Globe.onShell(sign * 30.0, this.origin, Globe.SHELL_SIGN_OUTER, 0.0),
                faded(new Color(196, 204, 216, 190), SkymapPanel.Layer.SIGNS), 1.1f);
        }
    }

    /**
     * Aspect lines, drawn through the globe rather than around it.
     *
     * <b>This is the view's argument for itself.</b> On the flat wheel an aspect is a line
     * across a disc; here it is a span over a sphere, and a reader turning the globe sees the
     * figure from the side - which is the one thing the flat chart genuinely cannot show.
     *
     * <b>Bowed rather than straight, and the bow is what makes it legible.</b> A chord is the
     * honest line and it is the one that hides: it runs through the crowded interior, and an
     * opposition is the diameter that goes through the exact centre where every other line
     * already is. Lifted, each aspect climbs over the middle to a height set by how wide it is
     * - see Globe.arc - so the widest ride highest and the figure has a silhouette from the
     * side as well as a shape from above.
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
            int deck = panel.ringDeck(ring);
            int innerDeck = panel.ringDeck(0);
            double[] from = Globe.onShell(lons[ring][c[1]], origin, shells[ring],
                liftOf(deck, this.stacked) + levels[ring][c[1]] * Globe.STACK_STEP,
                inclinationOf(deck, this.stacked));
            double[] to = Globe.onShell(panel.bLon[c[2]], origin, shells[0],
                liftOf(innerDeck, this.stacked) + levels[0][c[2]] * Globe.STACK_STEP,
                inclinationOf(innerDeck, this.stacked));
            // <b>The hovered chord, at full strength and on its own ring.</b> The globe drew
            // every chord alike, so pointing at a cell of the grid lit the flat wheel and did
            // nothing at all here - a reader who had switched views lost the one gesture that
            // says which two points a line joins. The ring index is the same 0/1/2 the wheel
            // now uses, so the sky ring lights when the sky ring is hovered and not when the
            // partner ring is.
            boolean lit = panel.lightsChord(c[1], c[2], ring);
            Color ink = new Color(c[3], true);
            double rise = riseFor(panel, ring, this.bowed);
            if (lit) {
                chord(from, to, rise, faded(new Color(255, 255, 255, 110),
                    SkymapPanel.Layer.ASPECTS), 4.0f, false);
                ink = new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 255);
            }
            chord(from, to, rise, faded(ink, SkymapPanel.Layer.ASPECTS), lit ? 2.4f : 1.0f,
                !lit);
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
                    // Only the partner ring is a synastry pair; the sky ring is a moment and
                    // is judged at natal orbs, the same as the flat wheel and the grid judge
                    // it. Passing "cross" for both halved the sky ring's orbs in a synastry
                    // chart and dropped chords the flat view was drawing.
                    Color ink = panel.aspectInkFor(lons[ring][a], panel.bLon[b], a, b,
                        ring == SkymapPanel.WHEEL_OUTER);
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
            int deck = this.panel.ringDeck(ring);
            double y = liftOf(deck, this.stacked) + level[i] * Globe.STACK_STEP;
            double[] p = Globe.onShell(lon[i], this.origin, radius, y,
                inclinationOf(deck, this.stacked));
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

            // <b>The planets themselves, when the reader asks for it.</b> This was Chart A
            // alone, because three Jupiters drawn as Jupiter are one Jupiter three times over
            // and the ring colour was all that told them apart. The answer is not to keep the
            // other two as beads but to keep the colour: a planet on Chart B or the sky is
            // circled in its own chart's ink below, so what it is and whose it is are two
            // different marks and neither has to carry the other.
            final boolean asPlanet = Settings.globePlanets() && PLANET_FACE[i] != FACE_NONE
                && (!outer || Settings.globePlanetsAllRings());
            final Color chartRim = shade(chartInk(ring), alpha);
            final int bodyIndex = i;
            final int half = this.g.getFontMetrics(font(13)).stringWidth(glyph) / 2;
            this.pieces.add(new Piece(q.depth, () -> {
                int rad = lit ? 11 : 9;
                if (asPlanet) {
                    drawPlanet(bodyIndex, (int) q.x, (int) q.y, rad, alpha, lit);
                    // Whose planet it is. Only on the outer rings: Chart A is the reader's own
                    // and needs no telling apart, and a circle round every body on a full
                    // three-ring globe is the clutter this drawing is trying to avoid.
                    if (outer) {
                        this.g.setStroke(stroke(lit ? 2.0f : 1.4f));
                        this.g.setColor(lit ? shade(new Color(255, 238, 170), alpha) : chartRim);
                        this.g.drawOval((int) q.x - rad - 2, (int) q.y - rad - 2,
                            (rad + 2) * 2, (rad + 2) * 2);
                    }
                    // <b>The glyph stays, beside it.</b> A planet at nine pixels is
                    // recognisable and not readable - two grey worlds are two grey worlds -
                    // and the glyph is how this chart has always named a body. Set off to
                    // the right so it labels the planet rather than sitting on it.
                    this.g.setFont(font(12));
                    this.g.setColor(shade(ink, (int) (alpha * 0.92)));
                    this.g.drawString(glyph, (int) q.x + rad + 3, (int) q.y + 4);
                    return;
                }
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

    // ------------------------------------------------------------------ the planets

    private static final int FACE_NONE = 0;
    private static final int FACE_SUN = 1;
    private static final int FACE_BANDED = 2;
    private static final int FACE_RINGED = 3;
    private static final int FACE_PLAIN = 4;

    /**
     * Which bodies are drawn as themselves, and how.
     *
     * <b>Only the ones that look like something.</b> A Sun with a corona, a banded Jupiter,
     * Saturn with its rings and the plain worlds are recognisable at nine pixels. Much of the
     * registry - the nodes, the Lots, the angles, most asteroids - is not a body anyone has a
     * picture of, and inventing one would be worse than the glyph. Those keep the bead, and
     * the two kinds sit on the ring together without the reader needing to be told which.
     *
     * Built against the registry rather than written as a switch at the call site, so a body
     * added later lands here as FACE_NONE and keeps its glyph instead of falling through to
     * whatever the last case happened to be.
     */
    private static final int[] PLANET_FACE = buildFaces();

    private static int[] buildFaces() {
        int[] faces = new int[SkymapPanel.BODY_COUNT];
        for (int i = 0; i < faces.length; i++) {
            switch (Bodies.at(i).name) {
                case "Sun":
                    faces[i] = FACE_SUN;
                    break;
                case "Jupiter":
                    faces[i] = FACE_BANDED;
                    break;
                case "Saturn":
                    faces[i] = FACE_RINGED;
                    break;
                case "Mercury":
                case "Venus":
                case "Mars":
                case "Uranus":
                case "Neptune":
                case "Pluto":
                    faces[i] = FACE_PLAIN;
                    break;
                default:
                    faces[i] = FACE_NONE;
                    break;
            }
        }
        return faces;
    }

    /** The face colour of a body, warm to cold. */
    private static Color faceColour(int body) {
        switch (Bodies.at(body).name) {
            case "Sun":
                return new Color(255, 196, 84);
            case "Mercury":
                return new Color(178, 172, 160);
            case "Venus":
                return new Color(226, 200, 148);
            case "Mars":
                return new Color(198, 96, 66);
            case "Jupiter":
                return new Color(206, 176, 138);
            case "Saturn":
                return new Color(214, 194, 146);
            case "Uranus":
                return new Color(150, 206, 208);
            case "Neptune":
                return new Color(104, 138, 214);
            case "Pluto":
                return new Color(164, 146, 132);
            default:
                return new Color(190, 190, 196);
        }
    }

    /**
     * One body drawn as itself.
     *
     * <b>Lit from the upper left, all of them.</b> A disc needs a light to read as a sphere,
     * and the direction has to be the same for every body - a ring of objects lit from
     * different places reads as a mistake even when the reader could not say what is wrong.
     */
    private void drawPlanet(int body, int x, int y, int rad, int alpha, boolean lit) {
        int face = PLANET_FACE[body];
        Color base = faceColour(body);
        int d = rad * 2;

        if (face == FACE_SUN) {
            // <b>The corona reaches further and the core is white.</b> It read as one more
            // orange bead: the glow was three faint rings inside the disc's own radius, which
            // is not a corona, it is a soft edge. Five rings out to twice the radius, and a
            // near-white centre, so the Sun is the brightest thing on the ring - which is the
            // one fact about it nobody has to be taught.
            for (int i = 5; i >= 1; i--) {
                int glow = rad + i * 5;
                this.g.setColor(new Color(255, 186, 82,
                    Math.max(0, Math.min(255, (int) (alpha * 0.16 / i)))));
                this.g.fillOval(x - glow, y - glow, glow * 2, glow * 2);
            }
            this.g.setPaint(new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(x - rad * 0.22f, y - rad * 0.22f),
                Math.max(1f, rad * 1.5f), new float[] {0f, 0.35f, 0.75f, 1f},
                new Color[] {shade(new Color(255, 255, 246), alpha),
                    shade(new Color(255, 232, 158), alpha),
                    shade(new Color(255, 168, 56), alpha),
                    shade(new Color(214, 96, 28), alpha)}));
            this.g.fillOval(x - rad, y - rad, d, d);
            this.g.setPaint(null);
            this.g.setColor(shade(new Color(255, 214, 130), alpha));
            this.g.setStroke(stroke(lit ? 2.0f : 1.0f));
            this.g.drawOval(x - rad, y - rad, d, d);
            return;
        }

        int rw = (int) Math.round(rad * 2.15);
        int rh = Math.max(2, (int) Math.round(rad * 0.52));
        if (face == FACE_RINGED) {
            // Behind the planet, then the planet, then in front - which is the whole reason
            // Saturn reads as Saturn rather than as a disc with a line through it.
            this.g.setColor(shade(new Color(206, 186, 142), (int) (alpha * 0.72)));
            this.g.setStroke(stroke(1.4f));
            this.g.drawArc(x - rw, y - rh, rw * 2, rh * 2, 0, 180);
        }

        this.g.setPaint(new java.awt.RadialGradientPaint(
            new java.awt.geom.Point2D.Float(x - rad * 0.35f, y - rad * 0.35f),
            Math.max(1f, rad * 1.5f), new float[] {0f, 1f},
            new Color[] {shade(lighten(base, 0.45), alpha), shade(darken(base, 0.5), alpha)}));
        this.g.fillOval(x - rad, y - rad, d, d);
        this.g.setPaint(null);

        if (face == FACE_BANDED) {
            // Jupiter's belts, clipped to the disc so they end where it does.
            java.awt.Shape was = this.g.getClip();
            this.g.setClip(new java.awt.geom.Ellipse2D.Float(x - rad, y - rad, d, d));
            this.g.setStroke(stroke(1.0f));
            for (int i = -2; i <= 2; i++) {
                if (i == 0) {
                    continue;
                }
                int by = y + (int) Math.round(i * rad * 0.34);
                this.g.setColor(shade(i % 2 == 0 ? darken(base, 0.72) : lighten(base, 0.25),
                    (int) (alpha * 0.85)));
                this.g.drawLine(x - rad, by, x + rad, by);
            }
            this.g.setClip(was);
        }

        if (face == FACE_RINGED) {
            this.g.setColor(shade(new Color(232, 214, 170), alpha));
            this.g.setStroke(stroke(1.6f));
            this.g.drawArc(x - rw, y - rh, rw * 2, rh * 2, 180, 180);
        }

        this.g.setColor(shade(lit ? new Color(255, 238, 170) : darken(base, 0.5), alpha));
        this.g.setStroke(stroke(lit ? 2.0f : 0.8f));
        this.g.drawOval(x - rad, y - rad, d, d);
    }

    private static Color lighten(Color c, double t) {
        return new Color((int) (c.getRed() + (255 - c.getRed()) * t),
            (int) (c.getGreen() + (255 - c.getGreen()) * t),
            (int) (c.getBlue() + (255 - c.getBlue()) * t));
    }

    private static Color darken(Color c, double t) {
        return new Color((int) (c.getRed() * t), (int) (c.getGreen() * t),
            (int) (c.getBlue() * t));
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

    /**
     * How much of its colour a chord keeps at a given depth.
     *
     * <b>A chord through the far side was as bright as one across the front.</b> The bodies
     * have dimmed round the back since they were drawn - a reader turning the globe should see
     * what is coming round, not have it appear - but the lines between them never got the same
     * treatment, so a dense chart painted its whole interior at one strength and the figure in
     * front of the reader had to be picked out of everything behind it.
     *
     * The ramp is smooth rather than the bodies' hard step. A body is at one depth and reads as
     * near or far; a chord spans depth, and stepping it at the centre would make lines flicker
     * between two strengths as the globe turns through the moment their midpoint crosses over.
     *
     * @param depth distance from the camera - {@code cam.distance} is the globe's own centre
     * @return 1.0 in front, falling to {@link #CHORD_BEHIND} at the back of the sphere
     */
    private double chordDepthFade(double depth) {
        // <b>Across the whole sphere, not just the half behind its centre.</b> The first
        // version ramped from cam.distance - the centre - which measured as very nearly a
        // no-op: a chord's depth is the mean of its two ends, and a chord with one end in
        // front and one behind averages back to the centre and came out at full strength.
        // Only chords with both ends well round the back faded at all, which is a small
        // minority, and the picture barely changed. Front of the sphere to back of it is the
        // range a chord actually varies over, so that is the range the ramp covers.
        double front = this.cam.distance - Globe.SHELL_SKY;
        double t = (depth - front) / (2.0 * Globe.SHELL_SKY);
        t = Math.max(0.0, Math.min(1.0, t));
        return 1.0 - (1.0 - CHORD_BEHIND) * t;
    }

    /** What a chord at the very back of the sphere keeps of its colour. */
    private static final double CHORD_BEHIND = 0.28;

    /** The most solid steps a straight chord is drawn in, when not turning. */
    private static final int CHORD_STEPS = 6;

    /**
     * One aspect line - bowed over the middle by default, straight through it if the reader
     * has asked for that - dimmed by how far through the globe it runs.
     *
     * Beside {@link #segment} rather than a flag on it: the cusps, the sign boundaries and the
     * mansion ticks are drawn on the surface and have their own treatment, and fading those by
     * depth would dim the scaffolding a reader uses to place what they are looking at.
     *
     * @param dim false for the chord under the cursor, which is the reader's own gesture and
     *     stays at full strength wherever it runs
     */
    /**
     * Which way an aspect line bows, and how far: up for the sky and this chart, down for
     * Chart B.
     *
     * <b>David's call, and it does two things at once.</b> "For partner B we had the aspect
     * arch lines on the bottom so it doesn't get cluttered with the lines from chart A and sky
     * lines, plus it would give the center a more rounded look." Every bow went up, so a
     * synastry put three charts' worth of arcs into one dome over the middle and nothing
     * below it. Chart B already lives on the lower deck; its lines now bow toward it, so the
     * partner's network is a bowl under the sign plane, the rest is a dome over it, and the
     * two together close the middle into something round.
     *
     * <b>Decided by the deck of the line's own wheel.</b> Wheel 0's deck is lower only when
     * Chart B has been promoted to be the chart, so that one rule covers a synastry's partner
     * lines and a promoted Chart B's own lines alike, while the sky's transits to either stay
     * up with the sky. Shared with the check suite for the same reason inclinationOf is: a
     * sample that bowed the other way would look for ink on the wrong side of the globe.
     *
     * @param bowed false when the reader has switched arcs off, which is the chord either way
     */
    static double riseFor(SkymapPanel panel, int ring, boolean bowed) {
        if (!bowed) {
            return 0.0;
        }
        return panel.ringDeck(ring) == SkymapPanel.DECK_LOWER ? -Globe.ARC_RISE : Globe.ARC_RISE;
    }

    /** True while the reader is dragging - what every "draw less" decision here reads. */
    private boolean turning;

    /**
     * The two globe settings this frame was painted under, read once each.
     *
     * <b>Read where the frame begins, not where they are used.</b> Settings.get opens and
     * parses the file on every call, and these are wanted once per body and once per aspect
     * line - several hundred file reads in a frame that is meant to take forty milliseconds.
     * Reading them once also means one frame cannot be painted half under each answer, if the
     * reader flips a checkbox while it is being drawn.
     */
    private boolean stacked;

    /** Whether an aspect bows over the middle in this frame. */
    private boolean bowed;

    private void chord(double[] a, double[] b, double rise, Color ink, float width,
                       boolean dim) {
        Globe.Projected pa = at(a);
        Globe.Projected pb = at(b);
        if (!pa.visible || !pb.visible) {
            return;
        }
        // <b>Bowed or straight, the same path either way.</b> The reader can switch the arc
        // off, and the chord it goes back to is this method with a rise of zero rather than a
        // second route through the painter - so the fade, the depth sort and the step count
        // cannot drift apart between the two looks.
        int steps = stepsFor(pa, pb, this.bowed, this.turning);
        // <b>Each piece is a short polyline, not one straight segment.</b> That is what makes
        // the bow smooth without making it expensive: the pieces are what get sorted, coloured
        // and handed to Java2D, and a polyline of a dozen points is one drawing call exactly as
        // a single segment is. See subdivisions for how fine.
        int sub = bowed ? subdivisions(pa, pb, steps, this.turning) : 1;
        double[][] path = Globe.arc(a, b, steps * sub, rise);
        Globe.Projected[] p = new Globe.Projected[path.length];
        for (int i = 0; i < path.length; i++) {
            p[i] = at(path[i]);
            if (!p[i].visible) {
                return;
            }
        }
        if (!dim) {
            // <b>The lit line as one piece, at its mean depth.</b> It is the reader gesture
            // and it is drawn at full strength wherever it runs, so it has no gradient to sort
            // piece by piece - and one polyline is one drawing call for the whole curve.
            int[] xs = new int[p.length];
            int[] ys = new int[p.length];
            double sum = 0.0;
            for (int i = 0; i < p.length; i++) {
                xs[i] = (int) Math.round(p[i].x);
                ys[i] = (int) Math.round(p[i].y);
                sum += p[i].depth;
            }
            final double mean = sum / p.length;
            this.pieces.add(new Piece(mean, () -> {
                this.g.setStroke(stroke(width));
                this.g.setColor(ink);
                this.g.drawPolyline(xs, ys, xs.length);
            }));
            return;
        }
        // <b>Along the line, in steps, and with a solid colour on every one of them.</b>
        // A single alpha for the whole line is not enough - one taken at the mean separated
        // the deepest from the shallowest by twelve percent, because a line from a near body
        // to a far one averages back to the centre and comes out middling when its far half is
        // exactly the half doing the cluttering.
        //
        // <b>The obvious answer, a GradientPaint, is unaffordable here, and measured rather
        // than assumed.</b> One per chord took a frame from 45ms to 182ms at rest and a drag
        // from 13ms to 78ms - four times over, because any Paint that is not a solid Color
        // drops Java2D off its fast path, and this view has always cost per drawing call.
        // Steps of a solid colour buy most of the gradient at a stroke price.
        for (int i = 0; i < steps; i++) {
            int lo = i * sub;
            // Each step sorts at its own depth, so a line that dives through the middle is
            // correctly overpainted piece by piece rather than all at its mean. The middle of
            // the piece is the depth its colour is mixed at.
            final double stepDepth = p[lo + sub / 2].depth;
            final Color step = shade(ink, (int) Math.round(ink.getAlpha()
                * chordDepthFade(stepDepth)));
            final int[] xs = new int[sub + 1];
            final int[] ys = new int[sub + 1];
            for (int k = 0; k <= sub; k++) {
                xs[k] = (int) Math.round(p[lo + k].x);
                ys[k] = (int) Math.round(p[lo + k].y);
            }
            this.pieces.add(new Piece(stepDepth, () -> {
                this.g.setStroke(stroke(width));
                this.g.setColor(step);
                this.g.drawPolyline(xs, ys, xs.length);
            }));
        }
    }

    /**
     * How many straight pieces one piece of a bow is drawn from.
     *
     * <b>Smoothness and cost are separate knobs, and this is the cheap one.</b> The piece count
     * is what costs: every piece is a sort, a colour and a drawing call. The points inside a
     * piece ride in the same call, so buying smoothness here is nearly free where buying it by
     * cutting more pieces is not. One straight piece per colour left a long line eight pixels
     * out at its apex and the corners read plainly.
     *
     * <b>As the square root of the span, because that is how the error falls.</b> A polyline
     * across a curve is out by about the span over the square of the segment count - measured
     * on this arc at 0.5 times that - so holding a fixed error as lines get longer costs only
     * the root of the extra length. A line spanning the globe takes about twenty-four segments
     * and a short one takes ten, where anything proportional would have given the long line
     * three times what it needed or the short one a third of it.
     *
     * <b>Curves were the other answer, and they were measured and dropped.</b> A quadratic
     * pinned to three points of the arc is a tenth of a pixel out with no subdivision at all -
     * better than this - but any shape that is not a line or a polyline leaves Java2D on its
     * general path. Stroking 240 of them costs 3.9ms against 1.8ms for 240 seven-point
     * polylines and 0.13ms for 240 straight lines, all at width one and antialiased. So a
     * polyline is half the price of the curve for a third of a pixel more, and a straight line
     * is a tenth of either - which is why the arc costs a few milliseconds a frame more than
     * the chord did and not a few tens of them. It is the lesson the GradientPaint taught one
     * change earlier, in a new costume.
     */
    static int subdivisions(Globe.Projected pa, Globe.Projected pb, int steps,
                            boolean turning) {
        double span = Math.hypot(pb.x - pa.x, pb.y - pa.y);
        double fineness = turning ? ARC_FINENESS_TURNING : ARC_FINENESS;
        int most = turning ? 8 : 16;
        return Math.max(2,
            Math.min(most, (int) Math.ceil(fineness * Math.sqrt(span) / steps)));
    }

    /** Segments per root pixel of span, at rest - set to hold the line within a third of one. */
    private static final double ARC_FINENESS = 1.3;

    /** As above, while the globe is being turned, where a gap of half a pixel does not read. */
    private static final double ARC_FINENESS_TURNING = 0.95;

    /**
     * How many pieces one aspect line is drawn in.
     *
     * <b>One job again, now that the points inside a piece carry the shape.</b> For a while the
     * count served two masters: the gradient wants steps where the line changes depth and none
     * where it does not, and the bow wanted steps wherever the line was long on screen, because
     * a straight piece was the only piece there was. So a long arc took fourteen pieces it did
     * not need for colour and paid a sort and a drawing call for each. A piece is a short
     * polyline now, so the shape is bought inside the call - see subdivisions - and this goes
     * back to the depth rule it had before the arc, which is why the arc costs about what the
     * chord did.
     *
     * <b>The floor is about the bow and the ceiling is about the colour.</b> One piece would
     * put the whole gradient of a line at one alpha, which is the thing the fade exists to
     * stop, so a bowed line takes four even when its depth does not ask for them. Six is where
     * the gradient stopped being worth another call, and that number predates the arc.
     *
     * <b>And fewer under the hand.</b> The bands and the sign shells already thin out while
     * the globe is being dragged, for the reason set out in paint: eighteen milliseconds of a
     * frame is the difference between a globe that follows the hand and one that fights it.
     *
     * <b>Static, because the check suite has to build the same pieces.</b> It asserts that the
     * painted line stays within half a pixel of the arc it stands for, and it cannot do that
     * without knowing how the painter cut the arc up.
     */
    static int stepsFor(Globe.Projected pa, Globe.Projected pb, boolean bowed,
                        boolean turning) {
        double spread = Math.abs(pb.depth - pa.depth) / (2.0 * Globe.SHELL_SKY);
        int wanted = (int) Math.round(spread * CHORD_STEPS);
        if (!bowed) {
            return Math.max(1, Math.min(CHORD_STEPS, wanted));
        }
        return Math.max(turning ? ARC_PIECES_TURNING : ARC_PIECES_LEAST,
            Math.min(CHORD_STEPS, wanted));
    }

    /** The fewest pieces a bow is cut into at rest - the gradient, rather than the shape. */
    private static final int ARC_PIECES_LEAST = 4;

    /** As above, while the globe is being turned. */
    private static final int ARC_PIECES_TURNING = 3;

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
