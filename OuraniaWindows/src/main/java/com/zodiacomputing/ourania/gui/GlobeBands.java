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
    static final GlobeBands GLOBE = new GlobeBands(0.0, null, 1.0);

    private final double flatness;
    private final int[] rings;
    private final double scale;

    private GlobeBands(double flatness, int[] rings, double scale) {
        this.flatness = flatness;
        this.rings = rings;
        this.scale = scale;
    }

    /**
     * The bands for this camera and this panel.
     *
     * <p><b>The wheel side is the wheel's own chain.</b> {@link WheelLayout#ringRadii}, with
     * the zodiac's layers as open as the reader has them, so a band folded on the wheel is
     * folded here too. Both outer body bands are taken as open, which is the globe's rule for
     * the charts - see {@link GlobeRenderer#wheelRadii}, which reads its rings from here so
     * the charts and the bands are measured on one wheel.
     */
    static GlobeBands of(GlobeSource panel, Globe cam, int w, int h) {
        double f = GlobeRenderer.flatness(cam);
        int[] rings = wheelRings(panel, w, h);
        return new GlobeBands(f, rings, scaleOf(rings));
    }

    /** The wheel's rings at this panel size, the outer body bands open. */
    static int[] wheelRings(GlobeSource panel, int w, int h) {
        return WheelLayout.ringRadii(w, h, 1.0, 1.0,
            panel.layerOpen(Layer.DECANS), panel.layerOpen(Layer.SIGNS),
            panel.layerOpen(Layer.BOUNDS), panel.layerOpen(Layer.DEGREES),
            panel.layerOpen(Layer.MANSIONS), 0.0);
    }

    /**
     * Wheel pixels to globe units: the wheel's sign band meets the globe's at its inner edge,
     * so every ring stays inside the frame that measures it.
     */
    static double scaleOf(int[] rings) {
        return Globe.SHELL_SIGN_INNER / Math.max(1, rings[WheelLayout.RING_SIGN_INNER]);
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
