package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Where everything on the flat wheel goes: the ring chain from the rim inward, how deep each
 * band is, how bodies are spread across the bands they sit in, how big each glyph is drawn,
 * and how a ring opens.
 *
 * <h3>Why this is its own class (J13, step 7)</h3>
 *
 * <p>These rules were static members of {@link SkymapPanel}, and after steps 5 and 6 the
 * painter, the hit tests and the globe all reached into the panel for them - so a second
 * screen could not lay out a wheel without bringing the panel with it. None of them reads any
 * state: every one is arithmetic on a panel size, a list of longitudes or a body index. They
 * are one class now, and the panel, {@link WheelCanvas}, {@link GlobeRenderer} and the suites
 * that assert the formula all read them from here.
 *
 * <p>Moved rather than changed. Each rule keeps its own note on why it is the way it is;
 * several of them record the day a second copy of the same arithmetic went wrong, which is
 * the reason there is only one copy now.
 */
public final class WheelLayout {

    private WheelLayout() {
    }

    /** The registry's size and its two lights, asked of the registry rather than the panel. */
    private static final int BODY_COUNT = Bodies.count();
    private static final int SUN = Bodies.indexOf("sun");
    private static final int MOON = Bodies.indexOf("moon");

    /**
     * The most body bands the wheel will lay out. Here because the layout is what it limits;
     * {@code WheelStack.MOST_BANDS} names the same number for the stack's own readers.
     */
    static final int MOST_BANDS = 5;

    /**
     * How far one of {@code count} items has come, at time {@code t} of a staggered opening.
     *
     * <p>Pure arithmetic, so it lives with the layout that uses it and a phone can open a ring
     * the same way; {@code Bloom.stagger} is this. Each item starts {@code spread} of the way
     * later than the one before and takes the rest of the time to arrive.
     */
    static double stagger(double t, int index, int count, double spread) {
        double offset = (index / (double) Math.max(1, count)) * spread;
        double span = 1.0 - spread;
        double local = span <= 0 ? 1.0 : (t - offset) / span;
        return Math.min(1.0, Math.max(0.0, local));
    }

    // ---------------------------------------------------------------- glyph sizes
    //
    // Size carries the same information the rings do, on a second channel: the Sun and
    // Moon are the two biggest objects on the wheel, then the personal planets, then the
    // slow ones, with the asteroids smallest. A reader should be able to find the lights
    // without reading a single glyph.
    //
    // Radius and font are kept together because they cannot be tuned apart - a 20pt glyph
    // in an 11px sphere spills over the edge of it. The baseline offset is derived from
    // the font rather than stored, so there is one number to change per tier, not three.

    /**
     * How much of the bloom is spent letting earlier bodies lead. See {@link #stagger}.
     *
     * At 0.45 the last body starts a little under halfway through, so the ring reads as
     * unfurling rather than as one object sliding outward.
     */
    static final double RING_SPREAD = 0.45;
    /**
     * A ring's radii, part-way out of the band they unfurl from.
     *
     * <b>Applied here rather than in the painter, so the hit test moves with the glyphs.</b>
     * Both sides read these arrays through Geometry - the note on that class records the day
     * they did not - so blooming the array is the only way to bloom the ring without
     * reintroducing exactly the defect Geometry exists to prevent. A glyph half-way out is
     * clickable half-way out.
     *
     * Bodies are staggered so the ring opens around the wheel instead of expanding as a disc.
     * Angles ride the same bloom as everything else: they are drawn on the ring, so they
     * arrive with it.
     *
     * @param settled where each body sits once the ring is fully open
     * @param inner   the band's inner edge - where a folded ring is gathered
     */
    static int[] bloomed(int[] settled, int inner, double v) {
        if (v >= 0.999) {
            return settled;                     // open: the array as computed, untouched
        }
        int[] out = new int[settled.length];
        for (int i = 0; i < settled.length; i++) {
            double p = WheelLayout.stagger(v, i, settled.length, RING_SPREAD);
            out[i] = (int) Math.round(inner + (settled[i] - inner) * p);
        }
        return out;
    }
    /** The Sun and the Moon. */
    static final int TIER_LIGHT = 0;
    /** Mercury, Venus, Mars - the rest of Group.LUMINARIES. */
    static final int TIER_INNER = 1;
    /** Jupiter through Pluto. */
    static final int TIER_OUTER = 2;
    /** Asteroids, centaurs, nodes and calculated points. */
    static final int TIER_SMALL = 3;
    public static final class GlyphSize {
        public final int radius;
        /**
         * The glyph's point size. A number rather than a {@code java.awt.Font}, so this table
         * is the same on the phone, which has no AWT; each screen makes its own font from it
         * ({@code WheelCanvas.glyphFont} on the desktop).
         */
        final int fontPoints;
        /** Distance below centre to sit the glyph baseline so it looks centred. */
        final int baseline;

        GlyphSize(int radius, int fontPoints) {
            this.radius = radius;
            this.fontPoints = fontPoints;
            this.baseline = Math.round((float)fontPoints * 0.25f);
        }
    }
    /**
     * <b>Four tiers, close together on purpose.</b> The lights used to be drawn at nearly
     * twice an asteroid's radius, which read as a hierarchy of importance the chart does not
     * actually claim - and, more practically, made a Sun and a Moon next to each other wide
     * enough to push their neighbours out of the band they belong to. They are still the
     * largest, by enough to find at a glance and no more.
     *
     * The largest transit radius here is what BAND_EDGE is set from; raising one without the
     * other is what lets a glyph overhang the ring it is drawn on.
     */
    static final GlyphSize[] NATAL_SIZES = {
        new GlyphSize(14, 24),      // lights
        new GlyphSize(13, 22),      // inner planets
        new GlyphSize(12, 21),      // outer planets
        new GlyphSize(11, 20)       // asteroids and points
    };

    static final GlyphSize[] TRANSIT_SIZES = {
        new GlyphSize(12, 20),
        new GlyphSize(11, 19),
        new GlyphSize(11, 18),
        new GlyphSize(10, 17)
    };

    /**
     * Size tier for a registry point, or -1 for the four angles, which are drawn as
     * labelled cubes at a fixed size and are not competing with the bodies for attention.
     */
    static int tierOf(int n) {
        Bodies.Def def = Bodies.at(n);
        if (def.isAngle()) {
            return -1;
        }
        if (n == SUN || n == MOON) {
            return TIER_LIGHT;
        }
        switch (def.group) {
            case LUMINARIES:
                return TIER_INNER;
            case SOCIAL:
                return TIER_OUTER;
            default:
                return TIER_SMALL;
        }
    }
    public static GlyphSize natalSize(int n) {
        int n2 = WheelLayout.tierOf(n);
        return NATAL_SIZES[n2 < 0 ? TIER_INNER : n2];
    }
    public static GlyphSize transitSize(int n) {
        int n2 = WheelLayout.tierOf(n);
        return TRANSIT_SIZES[n2 < 0 ? TIER_INNER : n2];
    }
    /** Outermost ring: Sun through Mars - the fast, personal bodies. */
    static final int RING_INNER_PLANETS = 0;
    /** Middle ring: Jupiter through Pluto - the slow, generational ones. */
    static final int RING_OUTER_PLANETS = 1;
    /** Innermost ring: asteroids, centaurs, nodes and the calculated points. */
    static final int RING_ASTEROIDS = 2;
    static final int RING_COUNT = 3;
    /**
     * Clearance kept at each edge of an outer band, so a glyph on the outermost sub-ring
     * does not overhang the boundary it is drawn against. One more than the largest radius
     * in TRANSIT_SIZES.
     */
    static final int BAND_EDGE = 13;
    /** Shallowest an outer band is allowed to get before the wheel is simply too small. */
    static final int MIN_BAND_DEPTH = 22;
    /**
     * How deep a band the partner ring and the sky ring each get.
     *
     * <b>Derived from what the band has to hold, not chosen.</b> The two bands used to be 22
     * and 25 pixels, while the layout inside them ran RING_COUNT sub-rings apart - three rings
     * twenty pixels apart is sixty pixels of content in a twenty-five pixel band, so the
     * asteroid sub-ring landed in the decans and the signs. Reading it as "a ring" was
     * accurate: it was a line with things scattered on both sides of it. A band is an area,
     * and its depth is the space its own sub-rings need.
     *
     * Capped at a sixth of the wheel, because on a small window a band that insists on its
     * ideal depth eats the chart it is wrapped around; there it compresses, and bandRadii
     * closes the sub-rings up to match.
     */
    static int outerBandDepth(int outer) {
        return WheelLayout.outerBandDepth(outer, 2);
    }
    /**
     * The same depth, shared out when more than two bands are open (G17).
     *
     * <p><b>The cap was never about one band, it was about the budget.</b> Two bands at
     * {@code outer / 6} each is a third of the radius spent on context, leaving two thirds for
     * the chart being read - and that ratio is the judgement in the sixth, not the six. So the
     * budget is stated as the third it has always been and divided among however many bands are
     * open, which is why this returns {@code outer / 6} unchanged at two bands. That is exact
     * rather than approximate: {@code (outer / 3) / 2} and {@code outer / 6} are the same
     * integer for every {@code outer}, so {@code AspectGridCheck} goes on asserting the formula
     * it has always asserted.
     *
     * <p><b>{@link #MIN_BAND_DEPTH} can still win, and then the bands overrun their budget.</b>
     * A band shallower than 22 pixels cannot hold a glyph, so the floor is right - but six
     * bands at the floor need 132 pixels of a wheel that may be 150 across, and the natal wheel
     * is what pays. The geometry cannot fix that, because the answer is not a smaller band: it
     * is fewer bands. {@link WheelStack#fits} is where that is decided, and it is decided
     * before a ring is opened rather than discovered when one is drawn over the other.
     *
     * <p>Note that {@code RING_COUNT} above is <i>not</i> this count. It is the three sub-rings
     * <i>within</i> one band - inner planets, outer planets, asteroids - and the collision of
     * names is old; it is what {@code ideal} is derived from and has nothing to do with how
     * many bands the wheel carries.
     *
     * @param bands how many body bands are open; fewer than two is treated as two, because the
     *     budget is not handed to a single band merely because it is alone
     */
    static int outerBandDepth(int outer, int bands) {
        int ideal = (RING_COUNT - 1) * (int) TRANSIT_RING_GAP + 2 * BAND_EDGE;
        return Math.max(MIN_BAND_DEPTH, Math.min(ideal, (outer / 3) / Math.max(2, bands)));
    }
    /**
     * Clearance and sub-ring spacing for the natal band.
     *
     * Wider than the outer rings' on both counts, because the natal wheel carries the largest
     * glyphs and is the thing being read - the outer rings are context around it.
     */
    public static final int NATAL_EDGE = 15;
    static final int NATAL_SUB_RING_GAP = 24;
    public static final double NATAL_SPACING = 32.0;
    /**
     * The least radius the natal wheel can be drawn in, derived the way a band's depth is.
     *
     * <p><b>Derived from what it has to hold.</b> The natal wheel lays its bodies out on
     * {@code RING_COUNT} sub-rings {@link #NATAL_SUB_RING_GAP} apart with {@link #NATAL_EDGE}
     * of clearance at each end - exactly as an outer band does with its own two numbers - so
     * the floor is that same sum and moves if either number does. A round number chosen here
     * instead would be a second statement of the layout that nothing keeps in step with the
     * first, which is this project's most-found defect.
     */
    static int minNatalRadius() {
        return (RING_COUNT - 1) * NATAL_SUB_RING_GAP + 2 * NATAL_EDGE;
    }
    /**
     * How deep the zodiac and its scales are, from the rim down to where the bodies may start.
     *
     * <p>Read outward-in: the rim, the decans, the signs, the Egyptian bounds and the inner
     * degree scale - the chain {@link #ringRadii} subtracts with every band fully open, which
     * is the worst case and therefore the one a fit has to survive.
     */
    static int zodiacDepth() {
        return 20 + 20 + 35 + TERM_BAND_DEPTH + DEGREE_RING_DEPTH;
    }
    /**
     * How many body bands fit on a wheel of this radius (G17).
     *
     * <p><b>Asked before a ring is opened, not discovered when one is drawn.</b>
     * {@link #MIN_BAND_DEPTH} is a floor a band cannot go below and still hold a glyph, so on a
     * small window the bands stop sharing the budget and start taking whatever they need - and
     * what pays is the natal wheel, silently, by being drawn underneath them. The failure mode
     * is a chart that looks fine and is unreadable at the centre, which is the worst kind:
     * nothing reports it.
     *
     * <p>So the question is turned round. Rather than letting any number of bands open and
     * hoping, this says how many the wheel can carry and {@link WheelStack} refuses the rest. A
     * reader who asks for six wheels on a small window is told they have four - a true answer
     * they can act on, by making the window bigger - instead of a drawing that lies.
     *
     * <p><b>Zero is a real answer, and finding that out was the point of asking.</b> The first
     * version of this floored at one, on the reasoning that a wheel too small for a single band
     * is too small for the chart and that is not this method's argument to make. The check
     * disagreed, and it was right: below about a 470-pixel window there is genuinely no room
     * for a band, and the wheel has been drawing one anyway. At 300 pixels the zodiac and its
     * scales take the radius down to 31, one band takes 23 more, and the natal wheel is left
     * with <b>-15</b> - a negative radius, drawn every time, reported by nothing.
     *
     * <p>That is older than any of this and is not a regression; it is what the assertion found
     * when the layout was finally asked whether it fits instead of told to lay out. Returning
     * zero is what lets the caller say "this window is too small for an outer wheel" rather
     * than draw one on top of the chart.
     */
    static int maxBodyBands(int outer) {
        int room = outer - WheelLayout.zodiacDepth() - WheelLayout.minNatalRadius();
        int most = 0;
        for (int n = 1; n <= MOST_BANDS; n++) {
            // <b>Stops at the first count that does not fit rather than taking the largest
            // that does</b>, and the difference is not pedantry. Band depth is integer
            // division, which throws away up to n-1 pixels per band, and MIN_BAND_DEPTH
            // clamps it from below - so the total is NOT monotonic in the count. At an outer
            // radius of 275 there is room for 88: two bands want 45 each and cost 90, four
            // want 22 each and cost 88. Scanning for the largest that fits therefore answered
            // FOUR bands on a wheel that cannot carry two. "How many fit" has to mean they can
            // be added one at a time.
            if ((long) n * WheelLayout.outerBandDepth(outer, n) > room) {
                break;
            }
            most = n;
        }
        return most;
    }
    /**
     * How deep a band the natal wheel gets.
     *
     * <b>The natal wheel used to have no floor at all.</b> It ran from its ceiling inward at
     * forty pixels a sub-ring with no lower bound, so twenty-nine bodies sprawled across a
     * hundred and ten pixels - "not on a singular ring but all over the place", which is how
     * David put it - and whatever was left over became the aspect area by accident. Giving it
     * a floor does two things at once: the bodies gather onto three tight sub-rings, and the
     * space inside the floor becomes a field the aspect lines can be laid out in deliberately.
     *
     * The forty-pixel gap was sized for nineteen-pixel glyphs. They are fourteen now.
     *
     * Capped at a third of its own ceiling, so the band cannot crowd out the fields inside it
     * on a small wheel.
     */
    public static int natalBandDepth(int natalTop) {
        int ideal = (RING_COUNT - 1) * NATAL_SUB_RING_GAP + 2 * NATAL_EDGE;
        return Math.max(MIN_BAND_DEPTH, Math.min(ideal, natalTop / 3));
    }
    /** Stagger applied to a body that collides with one already placed in its ring. */
    /** The transit band is thinner than the natal wheel, so its rings sit closer. */
    static final double TRANSIT_RING_GAP = 20.0;
    static final double TRANSIT_RING_STEP = 12.0;
    /**
     * Where one outer band's bodies sit, contained inside the band by construction.
     *
     * <b>This was two methods, and the second one's own header said it "mirrors" the first.</b>
     * That is the project's most expensive defect written down as a comment: one rule, two
     * implementations, drifting. The partner ring and the sky ring differ only in which
     * longitude array they read and which two radii bound them, so they are one method taking
     * those as arguments.
     *
     * <b>The sub-rings are derived from the band, not fixed.</b> The old pair asked for three
     * sub-rings twenty pixels apart regardless of how deep the band actually was, which is how
     * bodies ended up in the decans. Here the gap is whatever divides the usable depth, so the
     * innermost sub-ring lands exactly on the floor and nothing can be laid outside
     * {@code [bandInner + edge, bandOuter - edge]} - not by a wide glyph, not by a crowded
     * collision level, and not on a window too small to give the band its ideal depth.
     * AspectGridCheck asserts that containment across sizes rather than trusting it.
     *
     * @param bandOuter the band's outer boundary
     * @param bandInner the band's inner boundary
     */
    public static int[] bandRadii(double[] lon, boolean[] valid, int bandOuter, int bandInner) {
        return WheelLayout.bandRadii(lon, valid, bandOuter, bandInner, BAND_EDGE, 28.0);
    }
    /**
     * As above, for a ring whose glyphs are a different size from the outer rings'.
     *
     * <b>The clearance and the spacing are the ring's, not the method's.</b> The natal wheel
     * draws the largest glyphs on the chart and wants more room between them; hard-coding the
     * outer rings' numbers here and calling it shared would be sharing the name and not the
     * rule.
     *
     * @param maxEdge  the most clearance to keep at each boundary
     * @param spacing  minimum glyph separation in pixels, before a body steps to a new level
     */
    public static int[] bandRadii(double[] lon, boolean[] valid, int bandOuter, int bandInner,
                           int maxEdge, double spacing) {
        // On a band too shallow for full clearance, give up half of what there is at each
        // edge rather than letting top and floor cross - crossed bounds put every body on the
        // wrong side of the boundary, which is worse than a tight fit.
        int edge = Math.min(maxEdge, Math.max(0, (bandOuter - bandInner) / 2));
        int top = bandOuter - edge;
        int floor = bandInner + edge;
        double usable = Math.max(0.0, top - floor);
        double gap = usable / (double) (RING_COUNT - 1);
        double step = Math.min(gap * 0.35, TRANSIT_RING_STEP);

        int[] bodies = WheelLayout.ringedRadii(lon, WheelLayout.restrict(valid, false),
            top, gap, step, spacing, floor);

        // Angles ride the middle of the band, where they read as belonging to it rather than
        // to either neighbour.
        double mid = (top + floor) / 2.0;
        double angleStep = Math.min(gap * 0.5, 18.0);
        int[] levels = WheelLayout.radialLevels(lon, WheelLayout.restrict(valid, true),
            mid, angleStep, spacing);

        int[] out = new int[BODY_COUNT];
        for (int i = 0; i < BODY_COUNT; i++) {
            out[i] = Bodies.at(i).isAngle()
                ? (int) Math.max(mid - (double) levels[i] * angleStep, floor)
                : bodies[i];
        }
        return out;
    }
    /** How far in from the outer ring the mansion band starts. The ring is drawn there. */
    static final int MANSION_BAND_DEPTH = 9;
    /**
     * How far in from the outer ring the rim's click target reaches: all the way to the decan
     * ring, 20 px.
     *
     * <b>The whole rim is one target now.</b> It was split three ways - the mansion band from 9 px
     * in, an undrawn "Sabian strip" from 15 to 9, and a dead strip from 20 to 15 - so a click on
     * the inner half of the visible rim opened a Sabian symbol nothing on screen pointed to, or
     * nothing at all. Measured by colouring every pixel of the wheel by what the real click
     * handler opens (2026-09-14). David: "each click should be the entire space of the item". The
     * Sabian symbols have their own drawn ring, the inner degree scale, where every degree cell
     * opens its symbol.
     */
    static final int RIM_BAND_DEPTH = 20;
    /**
     * True when a click radius lands on the lunar mansion ring, given the whole chain.
     *
     * <b>The rim is two targets now, because it draws two things.</b> While the mansions were
     * drawn over the degree scale one target for the whole rim was the honest answer - there was
     * no way to point at the scale. Now the band has an edge, the mansions answer above it and
     * the scale answers below it, which is David's rule from 2026-09-14: each click is the
     * entire space of the item. With the mansions folded the band is empty and the scale takes
     * the rim back, so nothing is pointing at an invisible ring.
     */
    static boolean inMansionRing(double radius, int[] rings) {
        return rings[RING_MANSION_INNER] < rings[RING_OUTER]
            && radius >= rings[RING_MANSION_INNER] && radius <= rings[RING_OUTER] + 15;
    }
    /** True when a click radius lands on the rim degree scale, below any mansion band. */
    static boolean inRimDegreeBand(double radius, int[] rings) {
        double ceiling = rings[RING_MANSION_INNER] < rings[RING_OUTER]
            ? rings[RING_MANSION_INNER] : rings[RING_OUTER] + 15;
        return radius >= rings[RING_OUTER] - RIM_BAND_DEPTH && radius < ceiling;
    }
    /** Indices into {@link #ringRadii}. */
    public static final int RING_OUTER = 0;
    static final int RING_TRI = 1;
    public static final int RING_TRANSIT = 2;
    public static final int RING_DECAN_OUTER = 3;
    public static final int RING_SIGN_OUTER = 4;
    public static final int RING_SIGN_INNER = 5;
    /**
     * Inner edge of the partner band, and so the ceiling of the natal wheel.
     *
     * Added when the zodiac moved outward: with the body bands nested underneath the signs
     * there has to be a name for where the innermost of them stops, because that is where the
     * natal wheel is now allowed to start. Before the reorder this was RING_SIGN_INNER, and
     * with no outer ring open it still equals it exactly.
     */
    public static final int RING_BODY_TOP = 6;
    /**
     * Floor of the bound (term) band, whose ceiling is {@code RING_SIGN_INNER}.
     *
     * <b>The signs sit between their two subdivisions now, not under both.</b> Decans outside,
     * signs, then the Egyptian bounds inside - so the sign band is framed by the two rings
     * that divide it rather than carrying them both on one side.
     */
    public static final int RING_TERM_INNER = 7;
    /**
     * Floor of the inner degree ring, whose ceiling is {@code RING_TERM_INNER}.
     *
     * <b>The second degree scale, and the one the bodies point at.</b> The outer ticks sit at
     * the rim with the lunar mansions, a long way from any glyph; this one sits directly above
     * the wheels, so a body's leader line has somewhere near to land. Everything between the
     * two scales - decans, signs, bounds - is sandwiched by them.
     */
    public static final int RING_DEGREE_INNER = 8;
    /**
     * Inner edge of the lunar mansion band, and so the outer edge of the rim degree scale.
     *
     * <b>The mansions used to share the rim with the degree ticks, and covered them.</b> They
     * were drawn from {@code outer - 9} outward while the ticks reached {@code outer - 6}, so
     * with the mansions open the outer scale was underneath a lavender wash and its numbers -
     * David, 2026-09-16: "when lunar mansions are selected they cover over the second outer
     * sabian ring". The band now has an edge of its own and the ticks hang below it.
     *
     * <b>Appended rather than inserted, and zero when the mansions are folded.</b> Every index
     * before this one is load-bearing in four places, so an insert would move the bodies; and a
     * caller that knows nothing about mansions gets {@code outer} back, which is where the rim
     * scale has always sat. With the layer folded the two are equal, so the wheel lays out
     * exactly as it did - which is what lets AspectGridCheck go on asserting its formula.
     */
    static final int RING_MANSION_INNER = 9;

    /**
     * Floor of the directed chart's band, whose ceiling is {@code RING_TRANSIT} (G17).
     *
     * <p><b>Appended rather than inserted, for the same reason the mansions were.</b> Every
     * index before this one is read by name in dozens of places and by position in the suites,
     * so renumbering to put the new band in its geometric order would move the bodies. The
     * array's order is the order the edges were added; the wheel's order is the arithmetic in
     * {@link #ringRadii}.
     *
     * <p>With the directed band shut this equals {@code RING_TRANSIT} exactly, because a shut
     * band takes no room - so a wheel that is not showing a directed chart lays out to the
     * pixel as it did before this existed.
     */
    public static final int RING_ARC_INNER = 10;
    /** How deep the bound band is. Two pixels shallower than the decans, being finer. */
    static final int TERM_BAND_DEPTH = 18;
    /** How deep the inner degree scale is. Ticks only, so it needs little. */
    static final int DEGREE_RING_DEPTH = 16;
    /**
     * The wheel's ring radii, outermost first, derived in one place.
     *
     * <b>This chain was written out inline in four places</b> - click, paint, hover and the
     * ring code - all computing {@code min/2-10, -20, +-25, -20, -35} independently. The
     * handover has listed it as "the two-surfaces defect waiting to happen" since 2026-08-21,
     * and it was extracted here before adding a fifth copy for the lunar mansions rather than
     * after. The historical formula is asserted against this method in AspectGridCheck, so the
     * extraction cannot have silently moved the wheel.
     *
     * <b>The zodiac is the outermost thing, and the bodies nest underneath it.</b> Reading
     * inward from the rim: degree ticks inside {@code RING_OUTER}, the decan band from
     * {@code RING_DECAN_OUTER} to {@code RING_SIGN_OUTER}, the sign band from there to
     * {@code RING_SIGN_INNER}, the Egyptian bounds down to {@code RING_TERM_INNER}, and the
     * inner degree scale down to {@code RING_DEGREE_INNER}. The signs are framed by their two
     * subdivisions, and the whole zodiac is framed by the two degree scales. The body bands hang below that - the sky from
     * {@code RING_SIGN_INNER} (== {@code RING_TRI}) to {@code RING_TRANSIT}, the partner from
     * there to {@code RING_BODY_TOP}, and the natal wheel inside all of it.
     *
     * <b>It used to be the other way up</b>, with the two body bands wrapped around the
     * outside of the zodiac. Two things were wrong with that. The signs are the frame every
     * position is read against, and a frame drawn inside the things it measures reads as one
     * more ring rather than as the scale; and the outer bands, being widest, gave the most
     * room to the wheels with the fewest reasons to need it. Turning it over puts the zodiac
     * where it is read and the bodies where they are compared.
     *
     * With nothing open, RING_TRI == RING_TRANSIT == RING_BODY_TOP == RING_SIGN_INNER, so a
     * single wheel lays out exactly as it always has - which is asserted rather than assumed.
     */
    static int[] ringRadii(int width, int height, boolean showTransit, boolean showTri) {
        return ringRadii(width, height, showTransit ? 1.0 : 0.0, showTri ? 1.0 : 0.0);
    }
    /**
     * The same rings, with each outer band part-way open.
     *
     * <b>The bands have to widen with the bloom, or the wheel jumps.</b> A ring being switched
     * on costs the wheel inside it 25 pixels; done as a boolean that happens on the first
     * frame, so the reader sees the natal wheel snap smaller and only then watches the new
     * ring unfurl into the gap. Carving the band open at the same rate as the ring that fills
     * it is what makes the whole thing one movement.
     *
     * The two booleans were only ever switching a fixed inset on and off - 22 pixels for the
     * sky band, 25 for the band inside it - so the fractional form is the same arithmetic with
     * the insets scaled, and at every one of the four corners it is the historical formula to
     * the pixel. That is what lets AspectGridCheck keep asserting the formula it has always
     * asserted, and Part J of that suite is what caught the first attempt at this: scaling
     * decanOuter's inset off the already-scaled transit radius looked equivalent and was not,
     * because the boolean form ignores transit entirely when there is no outer wheel. The
     * band is therefore interpolated between the two layouts it actually has, not derived.
     */
    static int[] ringRadii(int width, int height, double outerOpen, double triOpen) {
        return ringRadii(width, height, outerOpen, triOpen, 1.0, 1.0, 1.0, 1.0);
    }
    /**
     * As above, with the zodiac's own bands able to fold away.
     *
     * <b>A folded band takes no room, so the wheel gets it back.</b> Fading a band's contents
     * and leaving its width allocated would be a layer that hides without helping - the reader
     * folds the decans because they want the space, and a gap where the decans were is not
     * the space. Each band's depth is scaled by how far its layer is open, which is the same
     * arithmetic the partner and sky bands already use.
     *
     * At all ones this is the chain exactly as it was, which is what lets AspectGridCheck go
     * on asserting the formula it has always asserted.
     */
    static int[] ringRadii(int width, int height, double outerOpen, double triOpen,
                           double decanOpen, double signOpen, double boundOpen,
                           double degreeOpen) {
        // Mansions folded: the rim degree scale keeps the whole rim, which is the layout every
        // caller of this arity was written against.
        return ringRadii(width, height, outerOpen, triOpen, decanOpen, signOpen, boundOpen,
            degreeOpen, 0.0);
    }
    /**
     * The same chain, with the lunar mansion band able to open at the rim.
     *
     * <b>The band has to carve its own space, not borrow the scale's.</b> The rim is 20 pixels
     * between {@code RING_OUTER} and {@code RING_DECAN_OUTER}; the mansions take the outermost
     * {@link #MANSION_BAND_DEPTH} of it as they open, and the degree ticks hang from whatever
     * is left. At {@code mansionOpen == 0} the two edges coincide and this is the old layout
     * to the pixel.
     */
    static int[] ringRadii(int width, int height, double outerOpen, double triOpen,
                           double decanOpen, double signOpen, double boundOpen,
                           double degreeOpen, double mansionOpen) {
        // The directed band shut, which is every layout that existed before G17 and is what
        // every caller of this arity was written against.
        return ringRadii(width, height, outerOpen, triOpen, decanOpen, signOpen, boundOpen,
            degreeOpen, mansionOpen, 0.0);
    }

    /**
     * The same chain with a third body band, between the sky and the middle ring (G17).
     *
     * <p><b>A new band goes in without any existing index changing meaning, because a shut band
     * takes no room and its two edges coincide.</b> That is the property the whole chain is
     * built on - it is what lets a single wheel lay out as it always has - and it is what makes
     * this addition safe rather than a renumbering. {@code RING_TRANSIT} is still the floor of
     * the sky's band and {@code RING_BODY_TOP} is still the ceiling of the natal wheel; the new
     * {@link #RING_ARC_INNER} sits between them and, at {@code arcOpen == 0}, equals
     * {@code RING_TRANSIT}. Every call site that predates this reads the same number it always
     * read.
     *
     * <p><b>Why the directed chart gets a band of its own rather than the middle one.</b> The
     * middle ring already carries a partner, or the sky, or the progressed chart, and those are
     * the things a directed chart is meant to be read <i>against</i>. Sharing one slot is what
     * made {@code Settings.OUTER_WHEELS} a radio button, and
     * {@link com.zodiacomputing.ourania.astro.Convergence} counts solar arc and progressions as
     * two independent witnesses precisely because they are not the same technique.
     *
     * <p>It nests just inside the sky and outside the middle ring, which is
     * {@link WheelStack#ORDER}: the person's own derived charts sit inside anyone else's, and
     * the sky is outermost.
     */
    static int[] ringRadii(int width, int height, double outerOpen, double triOpen,
                           double decanOpen, double signOpen, double boundOpen,
                           double degreeOpen, double mansionOpen, double arcOpen) {
        double o = Math.max(0.0, Math.min(1.0, outerOpen));
        double t = Math.max(0.0, Math.min(1.0, triOpen));
        double dc = Math.max(0.0, Math.min(1.0, decanOpen));
        double sg = Math.max(0.0, Math.min(1.0, signOpen));
        double bd = Math.max(0.0, Math.min(1.0, boundOpen));
        double dg = Math.max(0.0, Math.min(1.0, degreeOpen));
        double ar = Math.max(0.0, Math.min(1.0, arcOpen));
        int outer = Math.min(width, height) / 2 - 10;
        // <b>Every body band gets the same depth, and the depth knows how many there are.</b>
        // Counting the OPEN ones rather than the declared ones is what keeps a wheel with the
        // directed band shut laying out to the pixel as it did before G17 - an always-three
        // count would have narrowed the other two bands the moment this parameter existed.
        int open = (t > 0.001 ? 1 : 0) + (ar > 0.001 ? 1 : 0) + (o > 0.001 ? 1 : 0);
        int depth = WheelLayout.outerBandDepth(outer, open);
        // The zodiac sits at fixed radii just inside the rim; it no longer moves when a body
        // ring opens, which is the point of putting it outside them.
        int decanOuter = outer - 20;
        int signOuter = (int) Math.round(decanOuter - 20 * dc);
        int signInner = (int) Math.round(signOuter - 35 * sg);
        int termInner = (int) Math.round(signInner - TERM_BAND_DEPTH * bd);
        int degreeInner = (int) Math.round(termInner - DEGREE_RING_DEPTH * dg);
        // The body bands hang below the inner degree scale, each opening downward, outermost
        // first: the sky, then the directed chart, then the middle ring. A band at zero takes
        // no room, so with the directed band shut this is the two-link chain it always was.
        int[] body = WheelLayout.bodyBands(degreeInner, depth, new double[] {t, ar, o});
        int tri      = body[0];
        int transit  = body[1];
        int arcInner = body[2];
        int bodyTop  = body[3];
        double mn = Math.max(0.0, Math.min(1.0, mansionOpen));
        int mansionInner = (int) Math.round(outer - MANSION_BAND_DEPTH * mn);
        return new int[] {
            outer, tri, transit, decanOuter, signOuter, signInner, bodyTop,
            termInner, degreeInner, mansionInner, arcInner };
    }
    /**
     * Where each body band's edge falls, for any number of bands (G17).
     *
     * <p><b>This is the loop that was already here, unrolled.</b> The wheel has always laid its
     * body bands out as a chain - each one hangs one {@code depth} below the edge of the band
     * outside it, scaled by how far that band is open - but the chain was written as two
     * statements, {@code transit = tri - depth*t} and {@code bodyTop = transit - depth*o}, which
     * is the recurrence with the loop written out twice. Two statements is also two bands, and
     * the whole of G17 is that four techniques - natal, progressed, directed and transiting -
     * cannot be read together while the wheel has room for three.
     *
     * <p><b>The rounding is the part that has to be copied exactly.</b> Each edge is rounded to
     * a whole pixel and the <i>next</i> one is measured from the rounded value, not from the
     * exact one. Measuring every band from an unrounded running total would be the more obvious
     * code and would move the wheel by a pixel at some sizes - which {@code AspectGridCheck}
     * asserts against, and its Part J has already caught one restructuring of this that looked
     * equivalent and was not.
     *
     * <p>A band whose open fraction is zero takes no room, so its two edges coincide. That is
     * what lets a single wheel lay out exactly as it did before any of this existed, and it is
     * why the returned array is always one longer than {@code opens} - the last entry is the
     * ceiling of the natal wheel, which is what is left when every outer band has taken its.
     *
     * @param ceiling the inner edge of the zodiac, which the outermost body band hangs from
     * @param depth how deep one fully-open band is, from {@link #outerBandDepth}
     * @param opens how far each band is open, outermost first, each clamped to 0..1
     * @return {@code opens.length + 1} edges, outermost first
     */
    static int[] bodyBands(int ceiling, int depth, double[] opens) {
        int[] edges = new int[(opens == null ? 0 : opens.length) + 1];
        edges[0] = ceiling;
        for (int i = 0; opens != null && i < opens.length; i++) {
            double open = Math.max(0.0, Math.min(1.0, opens[i]));
            edges[i + 1] = (int) Math.round((double) edges[i] - (double) depth * open);
        }
        return edges;
    }
    /** Overload for callers that pre-date the tri-wheel; preserves the old contract. */
    public static int[] ringRadii(int width, int height, boolean showTransit) {
        return ringRadii(width, height, showTransit, false);
    }

    static int[] radialLevels(double[] dArray, boolean[] blArray, double d, double d2, double d3) {
        int n3 = dArray.length;
        int[] nArray = new int[n3];
        Integer[] integerArray = new Integer[n3];
        for (int i = 0; i < n3; ++i) {
            integerArray[i] = i;
        }
        Arrays.sort(integerArray, (n, n2) -> Double.compare(dArray[n], dArray[n2]));
        ArrayList arrayList = new ArrayList();
        Integer[] integerArray2 = integerArray;
        int n4 = integerArray2.length;
        block1: for (int i = 0; i < n4; ++i) {
            int n5 = integerArray2[i];
            if (!blArray[n5]) continue;
            double d4 = (dArray[n5] % 360.0 + 360.0) % 360.0;
            int n6 = 0;
            while (true) {
                if (arrayList.size() <= n6) {
                    arrayList.add(new ArrayList());
                    continue;
                }
                double d5 = Math.max(d - (double)n6 * d2, 12.0);
                double d6 = Math.toDegrees(d3 / d5);
                boolean bl = false;
                Iterator iterator = ((List)arrayList.get(n6)).iterator();
                while (iterator.hasNext()) {
                    double d7 = (Double)iterator.next();
                    if (!(WheelLayout.angularGap(d4, d7) < d6)) continue;
                    bl = true;
                    break;
                }
                if (!bl) {
                    ((List)arrayList.get(n6)).add(d4);
                    nArray[n5] = n6;
                    continue block1;
                }
                ++n6;
            }
        }
        return nArray;
    }
    static boolean[] restrict(boolean[] blArray, boolean bl) {
        boolean[] blArray2 = new boolean[blArray.length];
        for (int i = 0; i < blArray.length; ++i) {
            blArray2[i] = blArray[i] && Bodies.at(i).isAngle() == bl;
        }
        return blArray2;
    }
    /**
     * Radius per body, one ring per kind of body.
     *
     * @param dArray   longitudes, natal or transit
     * @param blArray  which of them are valid to draw
     * @param base     radius of the outermost ring
     * @param gap      distance to the next ring in
     * @param step     within-ring collision stagger; keep it under gap or a crowded ring
     *                 spills into the one inside it and the banding stops reading
     * @param spacing  minimum glyph separation in pixels, passed through to radialLevels
     * @param floorPx  never draw closer to the centre than this
     */
    static int[] ringedRadii(double[] dArray, boolean[] blArray, double base, double gap,
                              double step, double spacing, double floorPx) {
        int[][] nArray = new int[RING_COUNT][];
        for (int i = 0; i < RING_COUNT; ++i) {
            nArray[i] = WheelLayout.radialLevels(dArray, WheelLayout.restrictToRing(blArray, i),
                base - (double)i * gap, step, spacing);
        }
        int[] nArray2 = new int[BODY_COUNT];
        for (int i = 0; i < BODY_COUNT; ++i) {
            int n = WheelLayout.ringOf(i);
            if (n < 0) {
                continue;                       // an angle; the caller fills these in
            }
            nArray2[i] = (int)Math.max(
                base - (double)n * gap - (double)nArray[n][i] * step, floorPx);
        }
        return nArray2;
    }

    static double angularGap(double d, double d2) {
        double d3 = Math.abs(d - d2) % 360.0;
        return d3 > 180.0 ? 360.0 - d3 : d3;
    }
    /**
     * Which ring a registry point belongs to, or -1 for the four angles.
     *
     * Reads Bodies.Group rather than a list of names here, so a point added to the
     * registry lands in a ring automatically instead of silently defaulting to the
     * outermost one and looking like a planet.
     */
    static int ringOf(int n) {
        Bodies.Def def = Bodies.at(n);
        if (def.isAngle()) {
            return -1;
        }
        switch (def.group) {
            case LUMINARIES:
                return RING_INNER_PLANETS;
            case SOCIAL:
                return RING_OUTER_PLANETS;
            default:
                return RING_ASTEROIDS;
        }
    }
    /** The validity mask narrowed to one ring, so each ring staggers independently. */
    static boolean[] restrictToRing(boolean[] blArray, int n) {
        boolean[] blArray2 = new boolean[blArray.length];
        for (int i = 0; i < blArray.length; ++i) {
            blArray2[i] = blArray[i] && WheelLayout.ringOf(i) == n;
        }
        return blArray2;
    }

    /**
     * Where the natal bodies sit - the top of their band - for the reader's placement setting.
     *
     * <p>Here rather than on the panel so the phone places bodies by the same rule (M4); the
     * panel's {@code bodyBaseRadius} passes {@code Settings.bodyRing()}.
     */
    public static int bodyBase(int[] rings, String placement) {
        int base = rings[RING_BODY_TOP];
        String ring = placement;
        if (Settings.RING_CENTRE.equals(ring)) {
            // Well inside the rings, leaving the body bands and the zodiac clear.
            return (int) (base * 0.62);
        }
        if (Settings.RING_OUTSIDE.equals(ring)) {
            // <b>This option lost its old destination in the reorder.</b> "Outside the sign
            // ring" used to mean between the signs and the transit wheel; with the zodiac now
            // outermost there is nothing out there but the degree ticks, and putting bodies
            // there would place them beyond the frame that measures them. It now means as far
            // out as the natal wheel goes - hard against whatever ring is above it.
            return base;
        }
        return base - 14;
    }

    /**
     * The three circles aspect lines are drawn on, innermost first: natal to natal, the ring
     * outside it, the sky. Sized from the natal band's floor so they hold still as the chart
     * changes; see {@code Geometry.aspectDisc} for why each ring has its own. Here so the phone
     * draws its lines on the same circles (M4).
     */
    public static int[] aspectDiscs(int natalFloor) {
        int top = Math.max(30, natalFloor - 14);
        return new int[] {
            (int) Math.round(top * 0.52),
            (int) Math.round(top * 0.76),
            top,
        };
    }
}
