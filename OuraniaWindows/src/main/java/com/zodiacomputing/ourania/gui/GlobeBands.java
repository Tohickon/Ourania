package com.zodiacomputing.ourania.gui;

/**
 * Where each band of the zodiac sits on the globe from this camera: the globe's own shells at
 * an ordinary tilt, the wheel's rings looking straight down, and in between as
 * {@link GlobeRenderer#flatness} says.
 *
 * <p><b>The second step of the seamless body.</b> David, 30 Sep: "the wheel chart is really
 * just the top down view of the globe - is there a way we can make the wheel and the globe one
 * seamless body", and again 18 Oct: "keep the wheel looking as it does now but by moving its
 * axis it becomes the globe that we have now". The first step (`e1528ac9`) brought the three
 * charts to the wheel's bands as the camera climbs. This brings the rest of the rings: the
 * houses, the bounds, the signs, the decans, the rim degree scale and the mansions.
 *
 * <p><b>A table of bands, not one radius map.</b> When this was written the globe and the
 * wheel had their rings in different orders, and no map that keeps its order can swap two
 * rings. Since 2026-10-18 the order is the same on both, and the table stays because the
 * proportions are not: the globe's glyph rings overlap the ticks that run through them, the
 * wheel's bands abut. Each band has its two edges on the globe and its two edges on the wheel,
 * and a radius drawn in a band is carried as a share of that band's width.
 *
 * <p><b>The painter and every hit test ask this, and nothing else.</b> A band drawn at one
 * radius and clicked at another is the see-it-but-cannot-click-it defect this view has had
 * once already (2026-09-07).
 *
 * <p>Pure, like {@link Globe}: no Swing, so the phone can draw the same thing.
 */
final class GlobeBands {

    /**
     * The bands of the zodiac, from the middle outward - the same order on the wheel and the
     * globe since 2026-10-18, when the bounds moved outside the decans on both and the globe
     * gained the wheel's inner degree scale and hung its rim scale from the mansions.
     */
    enum Band { HOUSES, INNER_SCALE, SIGNS, DECANS, BOUNDS, DEGREES, MANSIONS }

    /** Half the bound band's width on the globe: halfway to the decans on either side. */
    private static final double BOUND_HALF = (Globe.SHELL_BOUND - Globe.SHELL_DECAN) / 2.0;

    /**
     * Each band's two edges on the globe, inner then outer, in world units.
     *
     * <p>Taken from where the globe draws and hit-tests each band today, so that at an
     * ordinary tilt nothing moves. The bounds and the decans are drawn as glyphs on one shell,
     * so their edges are chosen to put that shell near the middle of the band, which is where
     * the wheel puts its glyphs. The degree scale is the tick shell the globe has drawn since
     * 2026-09-16: from just outside the signs out to the length of a sign-boundary tick.
     */
    private static final double[][] GLOBE_EDGES = {
        {0.0, Globe.SHELL_HOUSE},                                         // HOUSES
        {Globe.SHELL_INNER_SCALE, Globe.SHELL_SIGN_INNER},                // INNER_SCALE
        {Globe.SHELL_SIGN_INNER, Globe.SHELL_SIGN_OUTER},                 // SIGNS
        {Globe.SHELL_SIGN_OUTER, 2.0 * Globe.SHELL_DECAN - Globe.SHELL_SIGN_OUTER}, // DECANS
        {Globe.SHELL_BOUND - BOUND_HALF, Globe.SHELL_BOUND + BOUND_HALF}, // BOUNDS
        {Globe.SHELL_BOUND + BOUND_HALF, Globe.SHELL_RIM_SCALE},          // DEGREES
        {Globe.SHELL_MANSION_INNER, Globe.SHELL_MANSION_OUTER},           // MANSIONS
    };

    /** The globe as it is at an ordinary tilt: every radius is its own. */
    static final GlobeBands GLOBE = new GlobeBands(0.0, null, 1.0, 0, 0);

    private final double flatness;
    private final int[] rings;
    private final double scale;
    /** The natal band's two edges on the wheel, in wheel pixels. */
    private final int natalTop;
    private final int natalFloor;

    private GlobeBands(double flatness, int[] rings, double scale, int natalTop,
                       int natalFloor) {
        this.flatness = flatness;
        this.rings = rings;
        this.scale = scale;
        this.natalTop = natalTop;
        this.natalFloor = natalFloor;
    }

    /**
     * The bands for this camera and this panel.
     *
     * <p><b>The wheel side is the wheel's own chain.</b> {@link WheelLayout#ringRadii}, with
     * the zodiac's layers as open as the reader has them, so a band folded on the wheel is
     * folded here too, and the outer body bands as open as the wheel has them, so with only
     * Chart A in the chart its ring takes the width the wheel gives it. That gave up the globe's
     * 30 Sep rule that a chart keeps the place it would have with every ring open: straight down
     * is to be the wheel as it is drawn, not a wheel with rings that are not there. See
     * {@link GlobeRenderer#wheelRadii}, which reads its rings from here so the charts and the
     * bands are measured on one wheel.
     */
    static GlobeBands of(GlobeSource panel, Globe cam, int w, int h) {
        double f = GlobeRenderer.flatness(cam);
        int[] rings = wheelRings(panel, w, h);
        int natalTop = panel.bodyBaseRadius(rings);
        int natalFloor = natalTop - WheelLayout.natalBandDepth(Math.max(1, natalTop));
        return new GlobeBands(f, rings, scaleOf(w, h), natalTop, natalFloor);
    }

    /** The wheel's rings at this panel size, every band as open as the wheel has it. */
    static int[] wheelRings(GlobeSource panel, int w, int h) {
        return WheelLayout.ringRadii(w, h, panel.outerOpenFraction(), panel.triOpenFraction(),
            panel.layerOpen(Layer.DECANS), panel.layerOpen(Layer.SIGNS),
            panel.layerOpen(Layer.BOUNDS), panel.layerOpen(Layer.DEGREES),
            panel.layerOpen(Layer.MANSIONS), 0.0);
    }

    /** Wheel pixels to globe units, such that straight down a wheel radius is drawn in place. */
    static double scaleOf(int w, int h) {
        // <b>The camera's own pixels, so straight down is the wheel at its own size.</b> This
        // made the wheel's sign band meet the globe's, which kept the proportions and left the
        // globe straight down smaller than the wheel at fit. Seen from straight above, every
        // point in the plane is the camera's distance away, so a world radius R draws at
        // focal * R / distance pixels; at the opening distance this scale makes a wheel radius
        // in pixels draw at exactly that many pixels. The opening distance, not the current
        // one, so a reader who zooms still zooms.
        return Globe.DEFAULT_DISTANCE / Math.max(1.0, Globe.focal(w, h));
    }

    /** The wheel's rings this was built from, or null for {@link #GLOBE}. */
    int[] rings() {
        return this.rings;
    }

    /** Wheel pixels to globe units. */
    double scale() {
        return this.scale;
    }

    /** How far toward the wheel, 0 to 1. */
    double flatness() {
        return this.flatness;
    }

    /**
     * Each band's two edges on the wheel, inner then outer, in wheel pixels.
     *
     * <p>Outward from the middle: the houses to the floor of the zodiac, the inner degree
     * scale, the signs, the decans, the bounds, the rim degree scale from the bounds to the
     * mansions, and the mansions to the rim. With the mansions folded the rim scale has the
     * whole rim, as on the wheel.
     */
    static double[] wheelEdges(int[] rings, Band band) {
        switch (band) {
            case HOUSES:
                return new double[] {0.0, rings[WheelLayout.RING_DEGREE_INNER]};
            case INNER_SCALE:
                return new double[] {rings[WheelLayout.RING_DEGREE_INNER],
                    rings[WheelLayout.RING_SIGN_INNER]};
            case BOUNDS:
                return new double[] {rings[WheelLayout.RING_TERM_INNER],
                    rings[WheelLayout.RING_TERM_OUTER]};
            case SIGNS:
                return new double[] {rings[WheelLayout.RING_SIGN_INNER],
                    rings[WheelLayout.RING_SIGN_OUTER]};
            case DECANS:
                return new double[] {rings[WheelLayout.RING_SIGN_OUTER],
                    rings[WheelLayout.RING_DECAN_OUTER]};
            case DEGREES:
                return new double[] {rings[WheelLayout.RING_TERM_OUTER],
                    rings[WheelLayout.RING_MANSION_INNER]};
            default:
                return new double[] {rings[WheelLayout.RING_MANSION_INNER],
                    rings[WheelLayout.RING_OUTER]};
        }
    }

    /**
     * Where each body of a ring sits on the wheel, in globe units: the wheel's own spread, so
     * a crowd fans outward along the radius as it does there instead of climbing the shell.
     *
     * <p>Read from {@link WheelLayout#bandRadii}, the method the wheel places its bodies with,
     * between the same two edges the wheel gives each ring. Null for {@link #GLOBE}, which has
     * no wheel to ask.
     *
     * @param ring 0 Chart A, 1 the outer ring, 2 the sky
     */
    double[] wheelBodyRadii(int ring, double[] lon, boolean[] valid) {
        if (this.rings == null) {
            return null;
        }
        int[] px = ring == 0
            ? WheelLayout.bandRadii(lon, valid, this.natalTop, this.natalFloor,
                WheelLayout.NATAL_EDGE, WheelLayout.NATAL_SPACING)
            : (ring == 1
                ? WheelLayout.bandRadii(lon, valid, this.rings[WheelLayout.RING_ARC_INNER],
                    this.rings[WheelLayout.RING_BODY_TOP])
                : WheelLayout.bandRadii(lon, valid, this.rings[WheelLayout.RING_TRI],
                    this.rings[WheelLayout.RING_TRANSIT]));
        double[] out = new double[px.length];
        for (int i = 0; i < px.length; i++) {
            out[i] = px[i] * this.scale;
        }
        return out;
    }

    /**
     * A radius that is not a band's, carried from the globe's value to a wheel radius given in
     * wheel pixels. Returned unchanged at an ordinary tilt, like {@link #at}.
     */
    double toward(double globeRadius, double wheelPixels) {
        if (this.flatness <= 0.0 || this.rings == null) {
            return globeRadius;
        }
        return globeRadius + (wheelPixels * this.scale - globeRadius) * this.flatness;
    }

    /**
     * A point carried from where the globe draws it to where the wheel draws it, in a straight
     * line. The globe's own point is handed back at an ordinary tilt, not a copy of it.
     */
    double[] blend(double[] globe, double[] wheel) {
        if (this.flatness <= 0.0 || this.rings == null) {
            return globe;
        }
        double f = this.flatness;
        return new double[] {
            globe[0] + (wheel[0] - globe[0]) * f,
            globe[1] + (wheel[1] - globe[1]) * f,
            globe[2] + (wheel[2] - globe[2]) * f,
        };
    }

    /** The wheel's house-number ring, in globe units. */
    double houseNumberRadius() {
        return this.rings == null ? 0.0
            : WheelLayout.houseNumberRadius(this.natalFloor) * this.scale;
    }

    /**
     * The disc a ring's aspect lines are drawn on, in globe units: the wheel ends a line at
     * each body's longitude on one of three nested discs, not at the glyph.
     *
     * @param level 0 Chart A's own aspects, 1 the outer ring's, 2 the sky's
     */
    double aspectDisc(int level) {
        if (this.rings == null) {
            return 0.0;
        }
        int[] discs = WheelLayout.aspectDiscs(this.natalFloor);
        return discs[Math.max(0, Math.min(discs.length - 1, level))] * this.scale;
    }

    /**
     * Where a leader line ends: the inner edge of the signs on the globe, and the rim degree
     * scale on the wheel, which is where David asked the wheel's leaders to reach on
     * 2026-09-16.
     */
    double leaderRadius() {
        double globe = at(Band.SIGNS, Globe.SHELL_SIGN_INNER);
        return this.rings == null ? globe
            : toward(globe, this.rings[WheelLayout.RING_MANSION_INNER]);
    }

    /** A band's two edges on the globe, inner then outer. */
    static double[] globeEdges(Band band) {
        return GLOBE_EDGES[band.ordinal()].clone();
    }

    /**
     * Where a radius the globe draws in this band is drawn from this camera.
     *
     * <p>The radius is carried as a share of the band's width, so a mark a little outside its
     * band on the globe - a highlight that swells past the edge - stays a little outside it
     * on the wheel. At an ordinary tilt it is returned exactly as given, not recomputed, so
     * the globe a reader has been using is untouched to the last bit.
     */
    double at(Band band, double globeRadius) {
        if (this.flatness <= 0.0 || this.rings == null) {
            return globeRadius;
        }
        double[] g = GLOBE_EDGES[band.ordinal()];
        double[] wheel = wheelEdges(this.rings, band);
        double share = (globeRadius - g[0]) / (g[1] - g[0]);
        double inner = g[0] + (wheel[0] * this.scale - g[0]) * this.flatness;
        double outer = g[1] + (wheel[1] * this.scale - g[1]) * this.flatness;
        return inner + share * (outer - inner);
    }
}
