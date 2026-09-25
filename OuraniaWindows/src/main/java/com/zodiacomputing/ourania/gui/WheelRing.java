package com.zodiacomputing.ourania.gui;

import de.thmac.swisseph.SweDate;
import java.time.ZonedDateTime;

/**
 * One ring of the wheel: every body's position on it, and the moment and place it was cast for.
 *
 * <p><b>J13, step 1 (2026-09-19).</b> These were thirty loose fields on SkymapPanel, ten per ring,
 * named three different ways - {@code bLon} / {@code baseChartTime}, {@code tLon} /
 * {@code transitChartTime}, {@code cLon} / {@code triCusps} / {@code skyChartTime} - and declared
 * across a thousand lines. Every part of the panel read them directly: the painter, the hover
 * text, the placements table, the click handling, the scrub bars. That shared, scattered state is
 * what made the file impossible to split; a piece moved out would have had to reach back into
 * the panel for every number it drew. Gathered here, a ring is one thing that can be handed over.
 *
 * <p>The fields stay public and the arrays stay replaceable, exactly as the loose fields were:
 * this step changes where the data lives, not who may touch it. Narrowing that comes later,
 * once the pieces that read it have moved out.
 */
public final class WheelRing {

    /**
     * What this ring holds - not where it is drawn.
     *
     * <b>The distinction this enum exists for.</b> Two of the three rings were named for their
     * contents ({@code natalRing}, {@code skyRing}) and one for its position ({@code outerRing}),
     * and the positional one is where the defects were, because its contents change with the
     * mode. Everything that needed to know what was in it re-derived the answer from some
     * combination of {@code chartMode}, {@code showTransitChart}, {@code transitsEnabled},
     * {@code showTriWheel}, {@code isSynastryChart()} and {@code showProgressed}. Those
     * re-derivations disagreed with each other, one at a time, for months:
     *
     * <ul>
     *   <li>a partner's Venus read as "Transiting Venus" - a person described as a passing
     *       event (31 Aug);</li>
     *   <li>a composite reading read the transit array ({@code showTransitChart}'s own
     *       javadoc says so);</li>
     *   <li>a progressed body needed a hand-written guard against being headed as a transit;</li>
     *   <li>{@code ringWord} was implemented six separate times (24 Sep);</li>
     *   <li>"Read in full" opened Chart A whatever ring the card came from (25 Sep).</li>
     * </ul>
     *
     * A ring that knows what it is cannot be asked the wrong question.
     */
    public enum Kind {
        /** The chart being read: the inner wheel, and the thing everything else is read against. */
        CHART_A("Chart A", null, false, null),
        /** One chart made out of two. Still the inner wheel, but it belongs to nobody. */
        COMPOSITE("the composite", null, false, null),
        /** A second person, in a synastry. Their placements are their NATAL placements. */
        CHART_B("Chart B", "Chart B", true, new float[] {9.0f, 4.0f}),
        /** The same person, moved on. A placement, not a passing event. */
        PROGRESSED("progressed", "progressed", false, new float[] {2.0f, 3.0f}),
        /** The sky at a chosen moment, laid over the chart. */
        TRANSIT("transiting", "transiting", false, new float[] {5.0f, 5.0f}),
        /** The sky now, wrapped around everything else. */
        SKY("sky", SkymapPanel.SKY_RING_WORD, false, new float[] {1.0f, 4.0f});

        /** How a reader refers to this ring in running prose. */
        public final String label;
        /**
         * The qualifier that stands in front of a body name from this ring, or null when the
         * ring takes none - the inner wheel is understood to be the chart being read.
         */
        public final String ringWord;
        /** True when this ring is a person rather than a moment, so prose is read as theirs. */
        public final boolean isPerson;

        /**
         * How an aspect line from this ring is dashed, or null for a solid line.
         *
         * <b>Dashing tells a reader WHICH CHART a line belongs to</b>, which is a different
         * question from what aspect it is - that is what colour says. Before 25 Sep the rule was
         * a boolean, {@code wheel != WHEEL_NATAL}, so a partner's line, a transit and the sky
         * were all dashed 5/5 and looked identical. The same shape as every other defect this
         * enum was created to end.
         *
         * <p>TRANSIT keeps 5/5 so that the commonest chart looks as it always has.
         */
        public final float[] dash;

        Kind(String label, String ringWord, boolean isPerson) {
            this(label, ringWord, isPerson, null);
        }

        Kind(String label, String ringWord, boolean isPerson, float[] dash) {
            this.label = label;
            this.ringWord = ringWord;
            this.isPerson = isPerson;
            this.dash = dash;
        }

        /** A copy, because a caller handing this to BasicStroke must not be able to edit it. */
        public float[] dashPattern() {
            return this.dash == null ? null : this.dash.clone();
        }

        /**
         * Whether a body from this ring is read as a passing event rather than a placement.
         *
         * <b>PROGRESSED and CHART_B are both false, and both were once true.</b> A progressed
         * body is where the person has moved to, and a partner's Venus really is their natal
         * Venus - neither is an event passing over the chart. Those were the two defects.
         */
        public boolean readsAsEvent() {
            return this == TRANSIT || this == SKY;
        }

        /**
         * Whether a body from this ring carries the {@code transit_} prefix on its name.
         *
         * <b>Deliberately NOT the same question as {@link #readsAsEvent}, and this is the
         * trap.</b> That prefix is the wire format, and it means "not the inner wheel" - it is
         * what {@code generatePlanetHtml} splits on before asking, separately, whether the ring
         * is a person. Making the prefix follow the meaning instead drops CHART_B out of the
         * prefixed set, and a synastry reading silently loses its "Their Venus in Scorpio"
         * framing and its overlay house prose: the partner branch is behind the prefix.
         *
         * <p>So the two stay apart until the wire format itself is narrowed, which is a change
         * with a much wider blast radius - saved links, aspect row labels, {@code isTransitLabel}
         * and the pipe-delimited grid rows all read it.
         *
         * <p>PROGRESSED is the one outer kind that takes no prefix, which is the behaviour the
         * click path already had and the reason it carried a hand-written guard.
         */
        public boolean takesTransitPrefix() {
            return this == CHART_B || this == TRANSIT || this == SKY;
        }
    }

    /**
     * What this ring holds. Assigned in one place - {@code SkymapPanel.assignRingKinds} - so it
     * cannot be derived differently by two callers.
     */
    public Kind kind = Kind.CHART_A;

    /** A ring that starts out knowing what it is. */
    public static WheelRing of(Kind kind) {
        WheelRing r = new WheelRing();
        r.kind = kind;
        return r;
    }

    /** Each body's ecliptic longitude on this ring, by registry index. */
    public double[] lon = new double[SkymapPanel.BODY_COUNT];
    /** Each body's speed in longitude, degrees per day; negative is retrograde. */
    public double[] speed = new double[SkymapPanel.BODY_COUNT];
    /** Whether the body is on this ring: computed, and switched on. */
    public boolean[] valid = new boolean[SkymapPanel.BODY_COUNT];
    /** Whether the ephemeris could place the body at all, whatever the selection. */
    public boolean[] ok = new boolean[SkymapPanel.BODY_COUNT];
    /** House cusps 1-12 (index 0 unused), as this ring was cast. */
    public double[] cusps = new double[13];
    /** The Ascendant this ring was cast with. */
    public double ascendant;
    /** The moment this ring shows. */
    public ZonedDateTime time;
    /** The same moment, as the ephemeris takes it. */
    public SweDate sd;
    /** Where this ring was cast for. Greenwich until something says otherwise. */
    public double latitude = 51.4779;
    public double longitude = 0.0;
}
