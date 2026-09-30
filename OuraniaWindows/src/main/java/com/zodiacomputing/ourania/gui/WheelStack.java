package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Which rings the wheel is carrying, and in what order they nest (G17).
 *
 * <h3>The defect this exists to end</h3>
 *
 * <p>The outer wheel was a <b>radio button</b>. {@code Settings.OUTER_WHEELS} offered Transits,
 * Progressions and Solar Arc, and {@code SkymapPanel} read that one value into two mutually
 * exclusive booleans - so the chart could show any one of the three and never two.
 *
 * <p>That is not a missing feature, it is a contradiction with what the app already computes.
 * {@link com.zodiacomputing.ourania.astro.Convergence} exists to say that confidence in a
 * period scales with the number of <i>independent</i> techniques naming the same natal point,
 * and its families include {@code TRANSIT}, {@code PROGRESSION}, {@code PROGRESSED_MOON} and
 * {@code SOLAR_ARC} - all four populated, all four voting. So the engine can already work out
 * that the progressed Moon, directed Mars and transiting Saturn all land on natal Venus this
 * year, and then the wheel could draw at most one of them. <b>The reading said "these three
 * agree" and the picture could not show two.</b>
 *
 * <p>Three slots was never a decision either. It is what a bi-wheel becomes when a third ring
 * is added to it, twice.
 *
 * <h3>The order is astrology, not layout</h3>
 *
 * <p>{@link #ORDER} nests the rings the way the technique reads them, innermost first: the
 * chart itself, then the person's own derived charts, then anyone else, then the sky. Two
 * things follow from that and both are the point.
 *
 * <p><b>Progressed and directed sit next to the natal wheel because they are still that
 * person.</b> A progressed Venus is where their Venus has got to and a directed Venus is their
 * natal Venus pushed forward by one arc; neither is a visitor. A partner's Venus is somebody
 * else's, and a transiting Venus is the actual planet passing overhead. Putting the two derived
 * rings inside the partner keeps the person's own material together and the strangers outside
 * it, which is also the order {@code WheelRing.Kind.isPerson} and {@code readsAsEvent} already
 * divide the kinds by.
 *
 * <p><b>The sky is always outermost</b>, which {@link RingBar} already says in prose: "natal is
 * always there, a partner blooms around it, the sky blooms around both."
 *
 * <h3>A stack that does not fit is refused rather than drawn</h3>
 *
 * <p>Bands have a floor - {@code WheelLayout.MIN_BAND_DEPTH}, below which a band cannot hold a
 * glyph - so on a small window they stop sharing the radius and start taking it, and the natal
 * wheel is drawn underneath them without anything saying so. {@link #trimmedTo} makes that a
 * stated outcome instead: the stack comes back at the size that fits and {@link #dropped}
 * names what did not, so a reader is told the sky is not shown rather than shown a wheel that
 * is quietly wrong at the centre.
 *
 * <p>This class holds the composition only. The ring <i>data</i> stays on {@link WheelRing}
 * objects owned by the panel, so this can be reasoned about - and checked - without a window.
 */
public final class WheelStack {

    /**
     * The most body bands the wheel will carry, and so six wheels counting the natal one.
     *
     * <p>Six is what {@link #ORDER} has to offer rather than a limit imposed on it: natal,
     * progressed, directed, partner, transits, sky. There is no seventh technique waiting, and
     * a number larger than the order can fill would be a cap that never binds.
     */
    public static final int MOST_BANDS = WheelLayout.MOST_BANDS;

    /**
     * Every kind that can be an inner wheel. Both belong to the chart being read; one of them
     * belongs to nobody, which is what a composite is.
     */
    private static final Set<WheelRing.Kind> INNER =
        EnumSet.of(WheelRing.Kind.CHART_A, WheelRing.Kind.COMPOSITE);

    /**
     * The nesting order, innermost first. See the class comment: this is the order the
     * technique reads them in, and the two derived rings sit inside the partner because they
     * are still the same person.
     *
     * <p><b>Order lives here and nowhere else.</b> The wheel, the scrub bars, the aspect-line
     * dashing and the placements table all have to agree about which ring is outside which, and
     * four copies of an ordering is four chances for the picture and the table to disagree
     * about what the reader is looking at.
     */
    public static final WheelRing.Kind[] ORDER = {
        WheelRing.Kind.PROGRESSED,
        WheelRing.Kind.SOLAR_ARC,
        WheelRing.Kind.CHART_B,
        WheelRing.Kind.TRANSIT,
        WheelRing.Kind.SKY,
    };

    private final WheelRing.Kind inner;
    private final List<WheelRing.Kind> bands;
    private final List<WheelRing.Kind> dropped;

    private WheelStack(WheelRing.Kind inner, List<WheelRing.Kind> bands,
                       List<WheelRing.Kind> dropped) {
        this.inner = inner;
        this.bands = Collections.unmodifiableList(bands);
        this.dropped = Collections.unmodifiableList(dropped);
    }

    /**
     * A stack from an inner wheel and whatever bands were asked for, in any order.
     *
     * <p><b>The caller's order is discarded on purpose.</b> Which rings a reader wants is a
     * choice; which is drawn outside which is not, and letting the order of a checkbox list
     * decide it would put the sky under the partner on a Tuesday. The asked-for set is sorted
     * into {@link #ORDER}.
     *
     * @param inner {@code CHART_A} or {@code COMPOSITE}; anything else is a programming error
     *     and is read as {@code CHART_A} rather than throwing out of a paint
     * @param wanted the bands to wrap around it; duplicates, nulls and inner kinds are ignored
     */
    public static WheelStack of(WheelRing.Kind inner, Collection<WheelRing.Kind> wanted) {
        WheelRing.Kind seat = INNER.contains(inner) ? inner : WheelRing.Kind.CHART_A;
        Set<WheelRing.Kind> asked = EnumSet.noneOf(WheelRing.Kind.class);
        if (wanted != null) {
            for (WheelRing.Kind k : wanted) {
                if (k != null) {
                    asked.add(k);
                }
            }
        }
        // <b>An inner kind asked for as a band is dropped by the order, not by a guard.</b>
        // This loop wrote `k != null && !INNER.contains(k)` until the mutation run showed that
        // removing the second half changed nothing any assertion could see: ORDER holds no
        // inner kinds, so CHART_A can be put into `asked` and is simply never read back out.
        // A guard no failure can reach is not a guard - the seventh found in this tree this
        // month - so it is gone and the structural reason is recorded here instead. INNER is
        // still what decides the seat, which is a question that IS asked.
        List<WheelRing.Kind> ordered = new ArrayList<>();
        for (WheelRing.Kind k : ORDER) {
            if (asked.contains(k)) {
                ordered.add(k);
            }
        }
        return new WheelStack(seat, ordered, new ArrayList<>());
    }

    /** The inner wheel alone, which is what a chart with nothing switched on is. */
    public static WheelStack single(WheelRing.Kind inner) {
        return WheelStack.of(inner, Collections.emptyList());
    }

    /** What sits at the centre: the chart being read, or the composite that belongs to nobody. */
    public WheelRing.Kind inner() {
        return this.inner;
    }

    /** The bands around it, innermost first. Never contains the inner kind. */
    public List<WheelRing.Kind> bands() {
        return this.bands;
    }

    /** How many bands the geometry has to find room for. */
    public int bandCount() {
        return this.bands.size();
    }

    /** The whole stack as a reader describes it, inner first. */
    public List<WheelRing.Kind> kinds() {
        List<WheelRing.Kind> all = new ArrayList<>();
        all.add(this.inner);
        all.addAll(this.bands);
        return Collections.unmodifiableList(all);
    }

    /** Whether this kind is on the wheel at all. */
    public boolean has(WheelRing.Kind kind) {
        return this.inner == kind || this.bands.contains(kind);
    }

    /**
     * Where this kind sits, counting the inner wheel as 0, or -1 when it is not on the wheel.
     *
     * <p>This is the index the scrub bars, the aspect discs and the placements table all order
     * themselves by, so that a ring is in the same position in every one of them.
     */
    public int indexOf(WheelRing.Kind kind) {
        if (this.inner == kind) {
            return 0;
        }
        int at = this.bands.indexOf(kind);
        return at < 0 ? -1 : at + 1;
    }

    /**
     * The bands that were asked for and are not drawn, outermost first.
     *
     * <p>Empty unless {@link #trimmedTo} dropped some. Kept rather than forgotten because a
     * reader who asked for the sky and is not being shown it is owed the sentence.
     */
    public List<WheelRing.Kind> dropped() {
        return this.dropped;
    }

    /**
     * The same stack cut down to what a wheel of this size can carry.
     *
     * <p><b>The outermost go first, and that is a judgement worth stating.</b> Any rule here
     * loses something a reader asked for; this one keeps the chart nearest the middle, which is
     * the one being read against. Dropping from the inside would leave the sky wrapped around
     * nothing in particular, and dropping the band the reader most recently opened would make
     * the answer depend on the order they clicked.
     *
     * @param most how many bands fit, from {@code WheelLayout.maxBodyBands}
     */
    public WheelStack trimmedTo(int most) {
        int keep = Math.max(0, Math.min(most, this.bands.size()));
        if (keep == this.bands.size()) {
            return this;
        }
        List<WheelRing.Kind> kept = new ArrayList<>(this.bands.subList(0, keep));
        List<WheelRing.Kind> lost = new ArrayList<>(this.bands.subList(keep, this.bands.size()));
        Collections.reverse(lost);
        lost.addAll(this.dropped);
        return new WheelStack(this.inner, kept, lost);
    }

    /**
     * How far each band is open, <b>outermost first</b>, which is the order the radii chain in.
     *
     * <p><b>The two orders are opposite and this is the seam between them.</b> A reader counts
     * outward from the chart, so {@link #bands} is innermost first; the geometry hangs each
     * band below the one outside it, so {@code WheelLayout.bodyBands} counts inward from the
     * zodiac. Reversing in one named place beats reversing at each call site, which is where an
     * off-by-one puts the sky's glyphs in the partner's band.
     *
     * @param open how far open each band is by its position in {@link #bands}, or null for all
     *     the way open, which is what a wheel that is not mid-animation wants
     */
    public double[] opens(double[] open) {
        double[] out = new double[this.bands.size()];
        for (int i = 0; i < out.length; i++) {
            int fromInside = out.length - 1 - i;
            out[i] = open == null || fromInside >= open.length ? 1.0 : open[fromInside];
        }
        return out;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.inner.heading);
        for (WheelRing.Kind k : this.bands) {
            sb.append(" + ").append(k.heading);
        }
        if (!this.dropped.isEmpty()) {
            sb.append(" (no room for");
            for (WheelRing.Kind k : this.dropped) {
                sb.append(' ').append(k.heading);
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
