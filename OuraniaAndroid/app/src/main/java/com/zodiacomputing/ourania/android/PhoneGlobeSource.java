package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.LunarMansions;
import com.zodiacomputing.ourania.gui.AngleRole;
import com.zodiacomputing.ourania.gui.AspectGate;
import com.zodiacomputing.ourania.gui.GlobeRenderer;
import com.zodiacomputing.ourania.gui.GlobeSource;
import com.zodiacomputing.ourania.gui.Ink;
import com.zodiacomputing.ourania.gui.Layer;
import com.zodiacomputing.ourania.gui.Settings;
import com.zodiacomputing.ourania.gui.WheelLayout;

import java.util.function.Supplier;

/**
 * The phone's answers to everything the globe asks of a chart (M11).
 *
 * <p><b>The desktop's renderer, not a second one.</b> {@link GlobeRenderer} draws the globe
 * from a {@link GlobeSource} and nothing else - that was M11 stage 2's whole point, and stage
 * 4 finished it by taking the renderer's last seventy references into {@code SkymapPanel}
 * away. So the phone's globe is this file plus {@code AndroidPen}: the same thirty-one
 * questions, answered from a {@link ChartFrame} instead of from a Swing component, and the same
 * arithmetic drawing the result.
 *
 * <p><b>No Android in here</b>, like {@link PhoneWheel} and {@link PhoneChart}, so
 * {@code PhoneGlobeSourceTest} can hold it to the invariants on the JVM. What cannot be tested
 * off a device is the painting, and that is {@code AndroidPen}'s, which has its own test for
 * everything it converts.
 *
 * <h3>Rings</h3>
 *
 * <p>The renderer numbers them 0 the inner chart, 1 the outer ring, 2 the sky. The phone shows
 * a birth chart and, when the reader asks for it, the sky at a moment - the same pair
 * {@link PhoneWheel} draws - so <b>0 is the birth chart and 1 is the sky</b>. Ring 2 is empty:
 * the phone has no tri-wheel, and a tri-wheel is a second person wrapped in the sky, which the
 * phone cannot yet show.
 *
 * <h3>Focus</h3>
 *
 * <p>Every focus question is cursor state on the desktop - what the pointer is resting on. A
 * phone has no pointer, it has a tap, so these start at "nothing" ({@code -1}, the sentinel the
 * desktop uses) and are set by {@link #focusBody(int)} when a tap lands. The renderer reads
 * {@code -1} as "light nothing", which is what an untouched globe should look like.
 */
final class PhoneGlobeSource implements GlobeSource {

    private final ChartFrame frame;
    /** The sky at the scrubbed moment, or null for the birth chart alone. */
    private final ChartFrame sky;

    private int focusBody = -1;
    private int focusHouse = -1;
    private int focusMansion = -1;
    private int focusDegree = -1;

    /**
     * The chords, built once and kept.
     *
     * <p>The renderer hands in a supplier and expects the source to cache it - "computed once
     * per chart rather than once per frame", which on a phone is the difference between a
     * globe that turns and one that stutters. Cleared by {@link #chartChanged()}.
     */
    private int[][] chords;

    /**
     * The reader's aspect selection and mode, read once rather than per pair.
     *
     * <p>{@code globeAspectInk} is asked for every pair of every ring on every frame -
     * hundreds of calls - and both of these go to disk through {@code Settings}. The desktop
     * reads them into fields when the chart is built for the same reason. Cleared by
     * {@link #chartChanged()}, which is what the Settings screen's save has to call.
     */
    private boolean[] aspectShown;
    private String aspectMode;

    private PhoneGlobeSource(ChartFrame frame, ChartFrame sky) {
        this.frame = frame;
        this.sky = sky;
    }

    /** The birth chart alone. */
    static PhoneGlobeSource of(ChartFrame frame) {
        return new PhoneGlobeSource(frame, null);
    }

    /** The birth chart with the sky at a moment on the ring outside it. */
    static PhoneGlobeSource of(ChartFrame frame, ChartFrame sky) {
        return new PhoneGlobeSource(frame, sky);
    }

    /** Throws the chord cache away, for when the moment or the settings have moved. */
    void chartChanged() {
        this.chords = null;
        this.aspectShown = null;
        this.aspectMode = null;
    }

    private boolean[] aspectShown() {
        if (this.aspectShown == null) {
            this.aspectShown = Settings.loadAspectSelection();
        }
        return this.aspectShown;
    }

    private String aspectMode() {
        if (this.aspectMode == null) {
            this.aspectMode = Settings.aspectMode();
        }
        return this.aspectMode;
    }

    // ------------------------------------------------------------ the chart

    @Override
    public double pinLongitude() {
        return this.frame.asc;
    }

    @Override
    public double[] activeCusps() {
        return this.frame.cusps;
    }

    @Override
    public double[] ringLon(int ring) {
        ChartFrame f = this.frameFor(ring);
        if (f == null) {
            return EMPTY_LON;
        }
        double[] lon = new double[Bodies.count()];
        for (int i = 0; i < lon.length && i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            lon[i] = b == null ? 0.0 : b.lon;
        }
        return lon;
    }

    @Override
    public boolean[] ringValid(int ring) {
        ChartFrame f = this.frameFor(ring);
        if (f == null) {
            return EMPTY_VALID;
        }
        boolean[] valid = new boolean[Bodies.count()];
        for (int i = 0; i < valid.length && i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            valid[i] = b != null && b.ok;
        }
        return valid;
    }

    private ChartFrame frameFor(int ring) {
        if (ring == 0) {
            return this.frame;
        }
        return ring == 1 ? this.sky : null;
    }

    @Override
    public boolean outerRingDrawn() {
        return this.sky != null;
    }

    @Override
    public boolean triRingDrawn() {
        // No tri-wheel on the phone: it is a second person wrapped in the sky, and the phone
        // cannot show a second person on the globe yet.
        return false;
    }

    /**
     * Which deck each ring rides on.
     *
     * <p><b>The invariant is the thing, not the mapping: two rings must never land on one
     * deck.</b> The desktop's comment is blunt about why - they would be drawn at one radius in
     * one plane and the reader would see a single ring holding two charts. With a sky shown the
     * sky owns the middle and the birth chart floats above it, which is the desktop's rule for
     * the same pair; with no sky the birth chart IS the chart and takes the middle itself,
     * because a lone ribbon above an empty middle says there is something underneath it that
     * is not there.
     *
     * <p>{@code PhoneGlobeSourceTest} asserts the invariant rather than the table, so a third
     * ring arriving cannot quietly double up.
     */
    @Override
    public int ringDeck(int ring) {
        if (ring == 1) {
            return GlobeRenderer.DECK_MIDDLE;
        }
        if (ring == 0) {
            return this.sky == null ? GlobeRenderer.DECK_MIDDLE : GlobeRenderer.DECK_UPPER;
        }
        return GlobeRenderer.DECK_UPPER;
    }

    @Override
    public int bodyBaseRadius(int[] rings) {
        // The desktop's answer exactly: one function of the ring chain and the reader's
        // body-ring setting, both of them shared.
        return WheelLayout.bodyBase(rings, Settings.bodyRing());
    }

    @Override
    public LunarMansions.Mansion moonMansion() {
        return LunarMansions.ofMoon(this.frame);
    }

    // ------------------------------------------------------------ what is shown

    /**
     * Whether a layer is drawn.
     *
     * <p>Read from {@link Settings}, which the phone already shares with the desktop (M9), so
     * a layer the reader folded on one is folded on the other. The phone has no bloom - layers
     * appear and disappear rather than unfurling - so {@link #layerOpen} is the same answer as
     * a 0-or-1, and the renderer multiplies alpha by it exactly as it does mid-bloom.
     */
    @Override
    public boolean layerShown(Layer layer) {
        return this.layerOpen(layer) > 0.004;
    }

    @Override
    public double layerOpen(Layer layer) {
        switch (layer) {
            case NATAL: return 1.0;
            case DEGREES: return Settings.showDegreeLines() ? 1.0 : 0.0;
            case HOUSES: return 1.0;
            case ASPECTS: return 1.0;
            case SIGNS: return 1.0;
            case DECANS: return 1.0;
            case BOUNDS: return 1.0;
            case MANSIONS: return 1.0;
            default: return 1.0;
        }
    }

    /**
     * What an angle on a given ring is.
     *
     * <p>The phone shows one person, so nothing it draws is ever a BRIDGE - that role is
     * Chart B's angle visiting Chart A's houses, and there is no Chart B here. The sky's
     * angles are a moment passing over the chart; everything else is the reader's own.
     */
    @Override
    public AngleRole angleRoleFor(boolean isSky, boolean isTransit) {
        return isSky || isTransit ? AngleRole.SKY : AngleRole.ANCHOR;
    }

    // ------------------------------------------------------------ focus

    @Override
    public int focusedBody() {
        return this.focusBody;
    }

    @Override
    public double focusedLongitude() {
        if (this.focusBody < 0) {
            return Double.NaN;
        }
        if (this.focusBody >= this.frame.bodies.length) {
            return Double.NaN;
        }
        ChartFrame.Body b = this.frame.bodies[this.focusBody];
        return b == null || !b.ok ? Double.NaN : b.lon;
    }

    @Override
    public int focusedHouse() {
        return this.focusHouse;
    }

    @Override
    public int focusedMansion() {
        return this.focusMansion;
    }

    @Override
    public int focusedDegree() {
        return this.focusDegree;
    }

    @Override
    public boolean onGlobeFocus(int body, boolean outer) {
        // The phone focuses a body of the birth chart; the sky's copy of the same planet is a
        // different thing and must not light with it.
        return this.focusBody == body && !outer;
    }

    /** What a tap landed on, or -1 for nothing. Returns true when the globe needs redrawing. */
    boolean focusBody(int body) {
        if (this.focusBody == body) {
            return false;
        }
        this.focusBody = body;
        return true;
    }

    /** The house, mansion and degree a tap landed in, each -1 for nothing. */
    boolean focusPlace(int house, int mansion, int degree) {
        if (this.focusHouse == house && this.focusMansion == mansion
                && this.focusDegree == degree) {
            return false;
        }
        this.focusHouse = house;
        this.focusMansion = mansion;
        this.focusDegree = degree;
        return true;
    }

    // ------------------------------------------------------------ aspects

    @Override
    public int[][] globeChords(Supplier<int[][]> build) {
        if (this.chords == null) {
            this.chords = build.get();
        }
        return this.chords;
    }

    /**
     * Which chords are drawn.
     *
     * <p>The desktop reads its aspect-filter dropdown; the phone has no such control, so it
     * draws the birth chart's own aspects always and the sky's to the chart whenever a sky is
     * shown. That is the desktop's "Both", which is its default.
     */
    @Override
    public boolean drawsNatalAspects() {
        return true;
    }

    @Override
    public boolean drawsCrossAspects() {
        return this.sky != null;
    }

    @Override
    public boolean lightsChord(int a, int b, int ring) {
        // Nothing is hovered on a phone, so no chord is lit on hover. A tapped body lights its
        // own chords, which is the nearest thing a tap has to a hover.
        return this.focusBody >= 0 && (this.focusBody == a || this.focusBody == b);
    }

    /**
     * An aspect chord's ink, or null for a pair that is not in aspect or not selected.
     *
     * <p><b>The null half is the important half, and it is the engine's.</b> Whether two points
     * are in aspect at all, within which orb, and whether the reader has that aspect switched
     * on, is {@link Aspects} and {@link Settings} - both shared - so the phone's globe draws
     * exactly the chords the desktop's would. Only the colour is the phone's own, and
     * {@link PhonePalette#aspect} records why it is one ink rather than fifteen.
     */
    @Override
    public Ink globeAspectInk(double lonA, double lonB, int a, int b, boolean cross) {
        double sep = Math.abs(lonA - lonB);
        if (sep > 180.0) {
            sep = 360.0 - sep;
        }
        if (!AspectGate.draws(a, b, this.aspectMode())) {
            return null;
        }
        Aspects.Type type = AspectGate.visible(sep, a, b,
            cross ? Aspects.Profile.TRANSIT : Aspects.Profile.NATAL, this.aspectShown());
        return type == null ? null : PhonePalette.aspect(type.label);
    }

    // ------------------------------------------------------------ colour

    @Override
    public Ink globeRingInk(int body, AngleRole role) {
        return PhonePalette.ring(body, role);
    }

    @Override
    public Ink elementInk(int element) {
        return PhonePalette.element(element);
    }

    @Override
    public Ink bodyInk(int body) {
        return PhonePalette.body(body);
    }

    @Override
    public Ink mansionInk() {
        return PhonePalette.mansion();
    }

    @Override
    public Ink leaderInk() {
        return PhonePalette.leader();
    }

    @Override
    public Ink beadInk(AngleRole role) {
        return PhonePalette.bead(role);
    }

    /**
     * The arrays handed back for a ring that is not there.
     *
     * <p>Empty rather than null, and shared rather than allocated per call: the renderer reads
     * these once per frame per ring and indexes them without asking their length first, so a
     * null would be a crash on the frame a reader switches the sky off.
     */
    private static final double[] EMPTY_LON = new double[0];
    private static final boolean[] EMPTY_VALID = new boolean[0];
}
