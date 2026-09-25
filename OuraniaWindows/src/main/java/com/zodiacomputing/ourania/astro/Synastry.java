package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L5 cross-chart placement: where one person's chart lands inside the other's frame.
 *
 * The synastry grid answers one of the questions synastry asks - which of A's bodies
 * aspect which of B's. This class answers the other two, both of which the app has drawn
 * the data for since 2026-08-16 without ever computing:
 *
 * <ul>
 *   <li><b>House overlays.</b> A's bodies plotted in B's houses. The aspect grid is blind
 *       to these: two charts can have no cross aspect at all and still have A's Sun
 *       sitting in B's 7th, which is a contact about a department of B's life rather than
 *       about a geometry between two degrees.</li>
 *   <li><b>Angle contacts.</b> A body of A's landing on one of B's four angles. The grid
 *       does emit these as cells, but as one cell among several hundred; nothing on screen
 *       said that a planet on the Ascendant is a different order of event from the same
 *       planet squaring Vesta.</li>
 * </ul>
 *
 * <b>Direction is the whole subject.</b> Every method here takes a visitor and a host and
 * means only that direction. A's Mars in B's 8th and B's Mars in A's 8th are two different
 * facts about two different people, and unlike the aspect matrix - where the separation at
 * least is symmetric - there is nothing shared between the two directions to be tempted
 * into computing once. Callers wanting both run the method twice with the arguments
 * swapped, and the panel does exactly that.
 *
 * <b>Nothing is filtered and nothing is ranked.</b> Every body the registry carries and
 * the frame computed appears in the overlay list, in registry order. A filter - physical
 * bodies only, personal planets only, the "important" ones - would be a judgement about
 * what the reading is allowed to mention, and this layer does not get to make it. The
 * angle contacts are sorted by orb because tightest-first is what an orb is for, not
 * because the loose ones were deemed unworthy.
 *
 * <b>The orb is the halved synastry orb</b>, which this class was the first caller of and
 * which the wheel, the grid and the hit test have used for cross-chart pairs since
 * 2026-08-24. See {@link #angleContacts} and {@link Aspects#effectiveOrb}.
 */
public final class Synastry {

    /**
     * The four angles, in the order a reading names them.
     *
     * These are the display names from the {@link Bodies} registry, which is also the key
     * {@link Aspects#orbFor} looks up - so the string that goes on screen and the string
     * that picks the orb are one string. A second private list of angle names here is
     * precisely how the two would drift.
     */
    public static final String[] ANGLES = {"Ascendant", "Descendant", "MC", "IC"};

    /** One of the visitor's bodies, located in the host's house frame. */
    public static final class Overlay {
        /** The visiting body's display name. */
        public final String body;
        /**
         * Its index in the {@link Bodies} registry.
         *
         * Carried so a renderer can reach a glyph without a name-to-index scan. Valid in
         * any array built by walking the registry, which is what both {@code ChartFrame
         * .bodies} and SkymapPanel's glyph, name and element tables are. The NAME remains
         * the key for anything that looks a body up rather than indexes it - orbs, prose,
         * settings - because those are keyed by name and always have been.
         */
        public final int bodyIndex;
        /** Its longitude, unchanged - the visitor's chart is not moved, only read. */
        public final double lon;
        /** 1..12 in the host's frame, or 0 when the host's cusps are degenerate. */
        public final int house;
        /** House 1, 4, 7 or 10 - the loudest quarter of the host's chart. */
        public final boolean angularHouse;

        Overlay(String body, int bodyIndex, double lon, int house) {
            this.body = body;
            this.bodyIndex = bodyIndex;
            this.lon = lon;
            this.house = house;
            this.angularHouse = house == 1 || house == 4 || house == 7 || house == 10;
        }
    }

    /** One of the visitor's bodies sitting on one of the host's angles. */
    public static final class AngleContact {
        public final String body;
        /** Registry index, on the same terms as {@link Overlay#bodyIndex}. */
        public final int bodyIndex;
        public final double bodyLon;
        /** One of {@link #ANGLES}. */
        public final String angle;
        public final double angleLon;
        /** Degrees from exact, always positive. */
        public final double orb;
        /** The orb this contact was judged against, so the reading can show its own margin. */
        public final double maxOrb;

        AngleContact(String body, int bodyIndex, double bodyLon, String angle, double angleLon,
                     double orb, double maxOrb) {
            this.body = body;
            this.bodyIndex = bodyIndex;
            this.bodyLon = bodyLon;
            this.angle = angle;
            this.angleLon = angleLon;
            this.orb = orb;
            this.maxOrb = maxOrb;
        }
    }

    private Synastry() { }

    /**
     * Every body of the visitor's chart, placed in the host's houses.
     *
     * <b>House assignment is {@link Zodiac#houseOf}, not arithmetic written here.</b> That
     * is the same call BodyScore, Gestalt, Rulership and TensionRelease make for a natal
     * chart, and it has to be: an overlay that used a different rule would put A's Sun in
     * B's 7th on this panel and B's own Sun in the 6th on the wheel for the same degree.
     * The consequence worth stating is that overlays inherit the host's house system
     * whole - a whole-sign host gives whole-sign overlays - which is correct, because the
     * houses being visited are the host's houses and there is no second opinion to have.
     *
     * <b>House 0 is passed through rather than dropped.</b> A degenerate cusp set - the
     * polar latitudes where a quadrant system has no solution - makes houseOf return 0,
     * and a body silently missing from this list would look like a body the ephemeris
     * failed on. The renderer says so instead.
     *
     * @param visitor the chart being read INTO the other; its degrees are not altered
     * @param host    the chart whose houses are being occupied
     */
    public static List<Overlay> houseOverlays(ChartFrame visitor, ChartFrame host) {
        List<Overlay> out = new ArrayList<>();
        if (visitor == null || host == null) {
            return out;
        }
        for (int i = 0; i < visitor.bodies.length; i++) {
            ChartFrame.Body b = visitor.bodies[i];
            if (b == null || !b.ok) {
                continue;
            }
            out.add(new Overlay(b.name, i, b.lon, Zodiac.houseOf(b.lon, host.cusps)));
        }
        return out;
    }

    /**
     * Every body of the visitor's chart conjunct one of the host's four angles.
     *
     * <b>Conjunction only, and that loses nothing.</b> A body opposite the host's
     * Ascendant is conjunct the host's Descendant, and testing all four angles catches it
     * there at the same orb. Squares to the angles are squares to the whole axis and
     * belong on the grid with every other square; what makes an angle contact its own
     * finding is a body sitting ON the axis, which is what a conjunction to one of the
     * four ends is.
     *
     * <b>The orb is the halved synastry orb</b> - {@code Aspects.typeOf(.., true)} - which
     * halves the body orb AND the aspect's own cap. Why both, and what happens if only the
     * first is halved, is in {@link Aspects#effectiveOrb}; the short version is that the six
     * minor aspects are capped at 1.0 and a body-orb halving cannot touch them, so halving
     * that alone shortens a synastry while making it more minor-weighted.
     *
     * <b>This method was the halved orb's first caller, and for six days its only one.</b>
     * The wheel, the grid and the hit test read at full natal orbs until 2026-08-24, so this
     * section and the grid below it on the same panel disagreed. They no longer do: the panel
     * asks {@code SkymapPanel.isSynastryPair} at all eight of its aspect call sites, and
     * AspectGridCheck Part L reads the emitted grid back and asserts every cell against the
     * orb its mode implies. <b>There is one regime on that screen now, and it is this one.</b>
     *
     * The measured effect on the grid, from Part L on its reference pair: 196 aspect cells
     * to 105. Over a 300-pair corpus, 114.3 per pair to 57.6, with 49-52% of every one of the
     * eleven aspect types surviving and the minors' share of a reading holding at a natal
     * chart's 16%. WORK-PLAN carries the ten schemes that were measured before this one was
     * picked, including the per-aspect table the source literature recommends - which came out
     * worst of them.
     *
     * <b>Transits are deliberately NOT halved.</b> Transit-to-natal is cross-chart by the same
     * arithmetic, and {@code isSynastryPair} requires the mode as well as the pair for exactly
     * that reason. Halving is a convention about two people, not about a chart and a moment.
     *
     * <b>The widths that result are 5 degrees for the Sun and Moon and 4 for absolutely
     * everything else</b>, measured across the whole registry. A pair is judged on the
     * LARGER of the two orbs and an angle's is 8, so against an angle a body's own orb is
     * used only by the two luminaries, whose 10 beats it. The consequence worth naming: the
     * 2 degrees the {@link Aspects} table gives the asteroids - so that six small objects
     * cannot treble the aspect lines on the wheel "without adding a comparable amount of
     * meaning" - has no effect here whatever. Pholus is given Mars's width. That is not
     * this method's doing and not new: it is how {@code orbFor} has always worked, and it
     * applies equally to every angle aspect the wheel and the grid have ever drawn. It is
     * merely newly visible, because this list sorts by orb and puts the winner on top.
     *
     * <b>The angles are read from the frame's {@code asc/mc/dsc/ic}, which are the same
     * degrees the registry's four ANGLE bodies carry</b> - ChartFrame derives those bodies
     * from these fields - so a contact reported here is a contact to the point the wheel
     * draws, not to a cusp that happens to be near it.
     *
     * <b>Definitional restatements are suppressed on the visiting side.</b> Three points in
     * the registry are defined as another point's opposition - the South Node, the
     * Descendant and the IC - and each of them can only ever produce a row that restates
     * the row its counterpart already produced. If A's North Node is 3 degrees 16 from B's
     * IC, then A's South Node is 3 degrees 16 from B's MC, necessarily, at an identical
     * orb, in every chart ever cast. The two rows are one fact.
     *
     * <b>Which three they are comes from {@link Bodies#oppositeOf}, not from a list here.</b>
     * That is the same call the wheel and the grid make to suppress the same restatement as
     * a line and as a cell, so a fourth definitional point added to the registry is handled
     * in all three places at once.
     *
     * This applies to the visiting side only: all four of the HOST's angles are always
     * tested, so an axis contact is never missed, only stated once and from one end.
     * SynastryCheck Part F asserts the restatement really is exact before trusting that.
     */
    public static List<AngleContact> angleContacts(ChartFrame visitor, ChartFrame host) {
        List<AngleContact> out = new ArrayList<>();
        if (visitor == null || host == null) {
            return out;
        }
        for (int i = 0; i < visitor.bodies.length; i++) {
            ChartFrame.Body b = visitor.bodies[i];
            if (b == null || !b.ok) {
                continue;
            }
            if (Bodies.oppositeOf(i) >= 0) {
                continue;
            }
            for (String angle : ANGLES) {
                double angleLon = angleLon(host, angle);
                double sep = Aspects.separation(b.lon, angleLon);
                if (Aspects.typeOf(sep, b.name, angle, Aspects.Profile.SYNASTRY) != Aspects.Type.CONJUNCTION) {
                    continue;
                }
                out.add(new AngleContact(b.name, i, b.lon, angle, angleLon, sep,
                    Aspects.orbFor(b.name, angle, Aspects.Profile.SYNASTRY)));
            }
        }
        // Tightest first. Two contacts at the same orb keep the order they were found in,
        // which is registry order, so the list is stable across runs.
        Collections.sort(out, (x, y) -> Double.compare(x.orb, y.orb));
        return out;
    }

    /**
     * The frame's degree for one of {@link #ANGLES}, or NaN for anything else.
     *
     * A switch rather than a map because there are four of them and they are fields on the
     * frame, not entries in a table.
     */
    public static double angleLon(ChartFrame f, String angle) {
        if (f == null || angle == null) {
            return Double.NaN;
        }
        switch (angle) {
            case "Ascendant":  return f.asc;
            case "Descendant": return f.dsc;
            case "MC":         return f.mc;
            case "IC":         return f.ic;
            default:           return Double.NaN;
        }
    }
}
