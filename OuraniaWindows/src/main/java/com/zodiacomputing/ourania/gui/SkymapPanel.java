/*
 * Decompiled with CFR 0.152.
 */
package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.Convergence;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Progressions;
import com.zodiacomputing.ourania.astro.Returns;
import com.zodiacomputing.ourania.astro.Snapshot;
import com.zodiacomputing.ourania.astro.Synastry;
import com.zodiacomputing.ourania.astro.SolarArc;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.OuraniaWindow;
import com.zodiacomputing.ourania.gui.ChartMode;
import com.zodiacomputing.ourania.gui.Geocoder;
import com.zodiacomputing.ourania.gui.NarrativeSynthesizer;
import com.zodiacomputing.ourania.gui.Settings;
import com.zodiacomputing.ourania.gui.Widgets;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public class SkymapPanel
extends JPanel {
    private SwissEph sw;
    /**
     * The wheel's three rings - the chart, the outer ring (transits, progressions or Chart B),
     * and the sky. See {@link WheelRing}: these were thirty loose fields until J13.
     */
    public final WheelRing natalRing = new WheelRing();
    public final WheelRing outerRing = new WheelRing();
    public final WheelRing skyRing = new WheelRing();
    private String baseLocationName = "Los Angeles, CA";
    private String baseTimeZoneId = ZoneId.systemDefault().getId();
    public boolean showTransitChart = false;

    /**
     * Whether the outer wheel carries the progressed chart instead of the sky.
     *
     * <b>It is the same ring, moved to a different moment.</b> Secondary progression advances
     * the chart a day per year of life, so the outer bodies are computed at
     * {@code Progressions.progressedJd} rather than at the transit date - the ring, its hit
     * tests, its aspect lines and its glyph spreading all work unchanged.
     *
     * <b>What does NOT come free is the vocabulary.</b> Every label on that ring says
     * "transiting", and progressed contacts shown as transits would be the defect this file
     * already carries a comment about, where synastry rows read as transit rows for months.
     * So the label is asked for through {@link #outerRingWord}, and a progressed body is read
     * as a placement - Venus in Scorpio, in a house - rather than through the transit prose,
     * which is what a progressed chart actually is.
     *
     * Houses stay natal. A progressed bi-wheel is progressed bodies around the natal frame;
     * recomputing cusps at the progressed moment would be a different technique.
     */
    /**
     * Whether the outer ring carries progressions, refreshed from Settings on every compute.
     *
     * <b>A field rather than a question asked of Settings, so a suite can pin it.</b> Asking
     * Settings directly made AspectGridCheck settings-dependent the moment this setting
     * existed - 6509 checks under Progressions against 6522 under Transits, green both times,
     * which is the drift that hid the empty aspect selection for a whole morning. aspectShown
     * is seeded the same way and pinned in the same place for the same reason.
     */
    private boolean showProgressed = Settings.OUTER_PROGRESSED.equals(Settings.outerWheel());

    private boolean showProgressed() {
        return this.showProgressed;
    }

    /** What the outer ring's bodies are, for any label that has to name them. */
    private String outerRingWord() {
        return this.showProgressed() ? "progressed" : "transiting";
    }

    /**
     * What the sky ring's bodies are.
     *
     * The outer ring can be a second person or a later moment; the sky ring is only ever the
     * sky, which is why this one is a constant and outerRingWord is not.
     */
    static final String SKY_RING_WORD = "sky";

    /**
     * What to call the ring a body sits on, or null for the natal ring, which needs no tag.
     *
     * <b>The hover card had one boolean where there are three rings.</b> It said "transiting"
     * for anything that was not the inner wheel, so in a synastry Chart B's Sun was labelled a
     * transit and the sky's Sun was labelled the same thing. The distinction already existed -
     * the aspect readout has drawn it since the sky got its own row - it had simply never
     * reached the tooltip. One statement of it now, and both read it.
     */
    String ringWord(int ring) {
        if (ring == WHEEL_NATAL) {
            return null;
        }
        if (ring == WHEEL_SKY) {
            return SKY_RING_WORD;
        }
        // The outer ring is a second person in a synastry and a later moment everywhere else -
        // the same rule updateChartData follows when it decides which moment to cast it from.
        return this.isSynastryChart() ? "Chart B" : this.outerRingWord();
    }
    public ChartMode chartMode = ChartMode.SINGLE;

    /**
     * Whether a third (transit-sky) ring is drawn around the synastry bi-wheel.
     *
     * <b>Separate from {@link #showTransitChart}.</b> In SYNASTRY mode that flag means
     * "chart B exists" and is always true; this flag means "the sky is wrapped around
     * both people". The sky data lives in {@link #skyRing.time}, which already
     * exists for exactly this purpose and whose javadoc says the same thing.
     */
    public boolean showTriWheel = false;

    /**
     * Whether the user asked for transits, as opposed to whether an outer wheel is drawn.
     *
     * <b>Kept apart from {@link #showTransitChart} on purpose.</b> That flag means "there is
     * an outer wheel", and in synastry the outer wheel is the second person, not a transit -
     * so the two are only the same thing in transit and composite modes. Conflating them is
     * what made a composite reading read the transit array.
     */
    public boolean transitsEnabled;

    /**
     * Which harmonic the wheel is showing. 1 is the radix and is the default.
     *
     * Applied at exactly two points, both in {@link #updateChartData}: the position arrays
     * after they are filled, and the frame {@link #getCurrentChart} hands out. Both call
     * {@link com.zodiacomputing.ourania.astro.Harmonics}, which is the only statement of the
     * rule - so the wheel, the aspect grid, the hit test and the interpretation panel cannot
     * end up showing different harmonics of the same chart.
     */
    public int harmonic = 1;

    /**
     * Whether an outer wheel is drawn at all, given the mode and the user's transits box.
     *
     * <b>A named function rather than an expression inside applyChartSettings, so a check can
     * assert the rule instead of a copy of the rule.</b> The obvious way to test this is to
     * restate the condition in the test, which proves only that two copies agree.
     *
     * <b>{@code showTransitChart} means "an outer wheel exists", not "the outer wheel is
     * transits".</b> All thirty of its uses read it that way, and in synastry the outer wheel
     * is the second person - so synastry draws one whatever the checkbox says, and the two
     * meanings must never be merged.
     *
     * The composite is the case that changed on 2026-08-25. It used to force an outer wheel
     * on, which is why a composite reading was handed the transit array and never listed the
     * composite's own bodies; now the third moment is drawn only when it was asked for, and a
     * composite by itself is a single wheel.
     */
    public static boolean outerWheelShown(ChartMode mode, boolean transits) {
        if (mode == ChartMode.SYNASTRY) {
            return true;
        }
        return (mode == ChartMode.TRANSIT
            || mode == ChartMode.COMPOSITE_MIDPOINT
            || mode == ChartMode.COMPOSITE_DAVISON) && transits;
    }

    /**
     * Choose where a midpoint composite's houses are derived, or clear it back to the default.
     *
     * <b>Pass NaN, or a null name, to go back to the couple's geographic midpoint.</b> Anything
     * else pins the frame to that place. The relationship cache is dropped here rather than at
     * the caller, because forgetting to do so is invisible: the setting changes, the wheel does
     * not, and nothing reports an error.
     *
     * @param lat  latitude to derive at, or NaN for the midpoint default
     * @param lon  recorded only; does not affect the cusps
     * @param name what to show in the panel, or null for the default
     */
    public void setCompositeReferencePlace(double lat, double lon, String name) {
        this.compositeRefLat = lat;
        this.compositeRefLon = lon;
        this.compositeRefPlace = (name == null || name.trim().isEmpty()) ? null : name.trim();
        this.relationshipFrame = null;
        this.relationshipCacheKey = null;
        Settings.update(p -> {
            if (Double.isNaN(lat)) {
                p.remove("composite.reference.lat");
                p.remove("composite.reference.lon");
                p.remove("composite.reference.name");
            } else {
                p.setProperty("composite.reference.lat", String.valueOf(lat));
                p.setProperty("composite.reference.lon", String.valueOf(lon));
                p.setProperty("composite.reference.name",
                    this.compositeRefPlace == null ? "" : this.compositeRefPlace);
            }
        });
        this.repaint();
    }

    /** The reference place name, or null when the couple's midpoint is being used. */
    public String getCompositeReferencePlace() {
        return this.compositeRefPlace;
    }

    /** Restore a saved reference place. Silently keeps the default if the setting is absent. */
    private void loadCompositeReferencePlace() {
        String lat = Settings.get("composite.reference.lat", null);
        String lon = Settings.get("composite.reference.lon", null);
        if (lat == null || lon == null) {
            return;
        }
        try {
            this.compositeRefLat = Double.parseDouble(lat);
            this.compositeRefLon = Double.parseDouble(lon);
            String n = Settings.get("composite.reference.name", "");
            this.compositeRefPlace = n.isEmpty() ? null : n;
        } catch (NumberFormatException ex) {
            // A corrupt setting is not worth refusing to start over.
            this.compositeRefLat = Double.NaN;
            this.compositeRefLon = Double.NaN;
            this.compositeRefPlace = null;
        }
    }

    /**
     * Whether a third (sky) ring is shown, given the mode and the user's transits box.
     *
     * Only SYNASTRY can have a tri-wheel: the outer ring is chart B and transits would
     * be a fourth ring in every other mode that already has an outer wheel.
     */
    public static boolean triWheelShown(ChartMode mode, boolean transits) {
        return triWheelShown(mode, transits, false);
    }

    /**
     * Whether a third ring is drawn, given what the middle one is carrying.
     *
     * <b>A third ring appears when the middle one is occupied by something that is not the
     * sky.</b> That was only ever true of a synastry - the middle ring is the second person and
     * the sky goes outside them both - so the rule was written as "synastry and transits", which
     * is the same answer arrived at by naming the one case rather than the reason.
     *
     * Progressions are the second case. The middle ring carries the progressed chart, cast at
     * {@link com.zodiacomputing.ourania.astro.Progressions#progressedJd} rather than at the
     * transit date; before this it and the sky competed for that one ring, so the reader could
     * have progressions or transits and never both. Natal inner, progressed middle, transits
     * outer is the standard way the technique is read, and every piece of it already existed -
     * what was missing was permission for the sky to take a ring of its own.
     *
     * @param progressed whether the middle ring is carrying the progressed chart
     */
    public static boolean triWheelShown(ChartMode mode, boolean transits, boolean progressed) {
        if (mode == ChartMode.SYNASTRY) {
            return transits;
        }
        // Only where the middle ring is genuinely the progressed chart: a composite already
        // derives its inner wheel from two people, and progressing that is a different
        // technique from the one this draws.
        return progressed && transits && mode == ChartMode.TRANSIT;
    }

    /**
     * Which bead shape the outer wheel wears, which depends on what it is carrying.
     *
     * <b>Asked of the mode, not of the ring number</b>, for the same reason
     * {@link #outerWheelShown} is: in synastry the outer wheel is the second person and the
     * sky goes to the tri-wheel ring outside it, while in every other mode the outer wheel IS
     * the sky. A rule written per-ring would give chart B the transit shape in exactly the
     * chart - two people plus transits - that this shape exists to disentangle.
     */
    public static String outerRingMarker(ChartMode mode) {
        return mode == ChartMode.SYNASTRY
            ? Settings.synastryMarker()
            : Settings.transitMarker();
    }
    /**
     * True when Chart A's birth time is not known, so the chart is cast for noon.
     *
     * Set from the Rodden rating in Chart Setup - X, and only X, changes the calculation.
     * See {@code ChartFrame.computeTimeUnknown} for what is withheld and why.
     */
    private boolean baseTimeUnknown;

    /** The ring chips above the transport row - see RingBar. */
    private RingBar ringBar;

    /**
     * Whether the chart screen is showing the globe instead of the flat wheel.
     *
     * <b>A mode of this screen, not a screen of its own.</b> The drawers, the controls, the
     * readings and the ring chips all describe the chart rather than the drawing of it, so a
     * separate screen would have needed a second copy of every one of them - and the copies
     * would have drifted. Switching views changes what the wheel panel paints and nothing
     * else; everything around it keeps working because nothing around it was ever about the
     * flat projection.
     */
    private boolean globeMode;

    /**
     * The layers of the chart the reader can fold away, each with its own bloom.
     *
     * <b>Partner and Sky are not here, and that is the distinction.</b> Those two change what
     * the chart <i>is</i> - opening the partner ring makes it a synastry, and the engine has to
     * be told - so they go through ChartMode and keep the blooms that already drive the bands.
     * These change only what is <i>drawn</i>. Mixing the two would put a second writer on the
     * mode, which is the defect this panel has shipped twice.
     *
     * Natal is here rather than fixed because David asked for it to fold like the others, and
     * it can: hiding the natal glyphs does not stop the chart being a natal chart.
     */
    enum Layer { NATAL, DEGREES, SIGNS, DECANS, BOUNDS, MANSIONS, HOUSES, ASPECTS }

    private final java.util.EnumMap<Layer, Bloom> layerBlooms =
        new java.util.EnumMap<>(Layer.class);

    /** How far a layer is open, 0 folded to 1 shown. */
    double layerOpen(Layer layer) {
        Bloom b = this.layerBlooms.get(layer);
        return b == null ? 1.0 : b.value();
    }

    /** Whether a layer is drawn at all - folded layers are skipped rather than drawn at zero. */
    boolean layerShown(Layer layer) {
        return this.layerOpen(layer) > 0.004;
    }

    /** Whether the reader has this layer open, whatever the bloom is doing this instant. */
    boolean layerWanted(Layer layer) {
        Bloom b = this.layerBlooms.get(layer);
        return b == null || b.opening();
    }

    /** Folds or unfolds a layer, animating either way. */
    void setLayer(Layer layer, boolean open) {
        this.layerBlooms.computeIfAbsent(layer,
            k -> new Bloom(true, this::repaintWheel)).set(open);
    }

    /** The camera, kept across a switch so returning to the globe finds it where it was. */
    private final Globe globe = new Globe();

    /**
     * The globe's aspect list, held between frames.
     *
     * <b>It was recomputed on every repaint, and that is what made the view slow.</b> Every
     * frame walked up to two and a half thousand pairs, and each pair ran visibleAspect and
     * drawsPair - orb lookups, a settings read, an enum scan - to answer a question whose
     * inputs had not changed since the chart was cast. Dragging a globe is the one thing that
     * repaints continuously, so the cost landed exactly where it hurt.
     *
     * Held as flat ints - ring, a, b, packed colour - because a list of small objects for
     * something rebuilt on every chart change is a lot of garbage for no clarity.
     *
     * <b>Cleared rather than dated.</b> A timestamp would need every input to remember to
     * touch it; one method that empties this, called from the places that change what an
     * aspect is, is a thing that can be grepped for.
     */
    private int[][] globeChords;

    /**
     * Forget the cached aspect list. Called wherever the answer could have changed - the
     * chart being recast, the reader's aspect selection, the filter, the aspect mode.
     */
    void invalidateGlobeChords() {
        this.globeChords = null;
    }

    /**
     * Whether natal-to-natal aspects are drawn, given the reader's filter.
     *
     * <b>Named because there are two views now.</b> This was an expression inside the flat
     * painter, and the globe simply did not ask it - so setting the filter to Natal-Natal
     * left the globe still drawing every transit chord. Two surfaces answering the same
     * question differently is this project's most-found defect, and a filter that half works
     * is worse than one that does not exist, because the reader believes it.
     */
    boolean drawsNatalAspects() {
        return "Natal-Natal".equals(this.aspectFilter) || "Both".equals(this.aspectFilter);
    }

    /** As above, for aspects between an outer ring and the natal wheel. */
    boolean drawsCrossAspects() {
        return ("Transit-Natal".equals(this.aspectFilter) || "Both".equals(this.aspectFilter))
            && this.showTransitChart;
    }

    /** The globe's aspects, computed once per chart rather than once per frame. */
    int[][] globeChords(java.util.function.Supplier<int[][]> build) {
        if (this.globeChords == null) {
            this.globeChords = build.get();
        }
        return this.globeChords;
    }

    /** Where the current drag started, or null when no button is down. */
    private java.awt.Point dragFrom;

    /**
     * Whether the drag in progress has moved far enough to be a turn rather than a click.
     *
     * <b>Every drag ends in a click event too.</b> Without this, turning the globe and
     * releasing opens whatever card happened to be under the cursor at the end of the turn,
     * which reads as the globe selecting things at random.
     */
    private boolean globeTurned;

    /**
     * True only while a button is down and the globe is actually moving.
     *
     * Separate from globeTurned, which lives on until the click that ends the drag is
     * swallowed. This one clears on release, and the release repaints - so the full picture
     * arrives the instant the hand stops.
     */
    private boolean globeDragging;

    /** The chips above the transport row, for the window to keep in step with the view. */
    RingBar ringBarComponent() {
        return this.ringBar;
    }

    /** Whether the chart is being shown as a globe. */
    boolean isGlobeMode() {
        return this.globeMode;
    }

    /** Switches between the flat wheel and the globe, repainting either way. */
    void setGlobeMode(boolean on) {
        if (this.globeMode == on) {
            return;
        }
        this.globeMode = on;
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
    }

    /**
     * How far the outer ring is open, and the sky ring beyond it.
     *
     * <b>One bloom per ring that is DRAWN, not per idea it carries.</b> The outer ring holds a
     * partner in a synastry and the sky in a transit chart, and the painter does not care
     * which - it draws a ring of glyphs from outerRing.lon either way. Keying the blooms to the arrays
     * rather than to the meaning is what stops this needing a branch per mode.
     *
     * The ring keeps drawing while a bloom is folding even though the flag that gated it has
     * already gone false, which is the whole point: the ring leaves rather than vanishing.
     */
    private final Bloom outerBloom = new Bloom(false, this::repaintWheel);
    private final Bloom triBloom = new Bloom(false, this::repaintWheel);

    /**
     * How much of the bloom is spent letting earlier bodies lead. See Bloom.stagger.
     *
     * At 0.45 the last body starts a little under halfway through, so the ring reads as
     * unfurling rather than as one object sliding outward.
     */
    private static final double RING_SPREAD = 0.45;

    /**
     * Whether the outer ring is <b>on screen</b>, as opposed to whether it exists.
     *
     * <b>This is a different question from {@link #showTransitChart}, not a second answer to
     * the same one.</b> That flag is intent - the reader asked for an outer wheel, so compute
     * its data, set the aspect filter, enable the alignment control. This predicate is about
     * pixels: a ring the reader has just folded away is still being drawn for the length of
     * the fold, and a ring being drawn must be laid out, painted and clicked on.
     *
     * Everything that answers "where is it" - the ring layout, the painter, the hit test -
     * asks this. Everything that answers "should it exist" asks the flag. Merging them is how
     * a fold turns into a disappearance, and splitting the wrong way is how a glyph you can
     * see becomes a glyph you cannot click.
     */
    boolean outerRingDrawn() {
        return this.outerOpenFraction() > 0.001;
    }

    /** As above, for the outermost sky ring. */
    boolean triRingDrawn() {
        return this.triOpenFraction() > 0.001;
    }

    /**
     * How far the outer ring is open, 0 to 1 - the one number every ring question resolves to.
     *
     * <b>The flag wins when the two disagree.</b> showTransitChart is a public field and code
     * outside this class writes it, so a bloom that was never told about a change must not be
     * able to hide a ring the flag says is there: an un-animated ring is a cosmetic loss, a
     * missing one is a broken chart. The one case where the bloom knows better is a fold, and
     * there the flag has already gone false, so the two cannot fight.
     */
    double outerOpenFraction() {
        if (this.showTransitChart && !this.outerBloom.opening()) {
            return 1.0;
        }
        return this.outerBloom.value();
    }

    /** As above, for the outermost sky ring. */
    double triOpenFraction() {
        if (this.showTriWheel && !this.triBloom.opening()) {
            return 1.0;
        }
        return this.triBloom.value();
    }

    /**
     * Where and when the sky is, from the Sky row of the setup form.
     *
     * Its own entry point rather than three more parameters on applyChartSettings, which
     * already carries eleven: the sky is a third chart now, not a variation on the second.
     */
    public void applySkySettings(String date, String time, String location,
            String zoneOverride) {
        final String loc = location == null ? "" : location.trim();
        final String d = date == null ? "" : date.trim();
        final String t = time == null ? "" : time.trim();
        final String zone = zoneOverride == null ? "" : zoneOverride.trim();
        if (loc.isEmpty()) {
            this.applySkyZone(zone);
            this.setSkyMoment(d, t);
            return;
        }
        new javax.swing.SwingWorker<Geocoder.Result, Void>() {
            @Override
            protected Geocoder.Result doInBackground() {
                return Geocoder.lookup(loc);
            }

            @Override
            protected void done() {
                try {
                    Geocoder.Result r = get();
                    if (r != null) {
                        SkymapPanel.this.installSubjects(SkymapPanel.this.subjectA,
                            SkymapPanel.this.subjectB,
                            SkymapPanel.this.subjectSky.movedTo(r.name, r.lat, r.lon, r.tzId));
                    }
                } catch (Exception ignored) {
                    // An unreachable geocoder leaves the sky where it was, which is better
                    // than moving it somewhere wrong.
                }
                // The reader's zone wins over the geocoder's, exactly as it does for Chart A.
                SkymapPanel.this.applySkyZone(zone);
                SkymapPanel.this.setSkyMoment(d, t);
                // <b>This worker can finish before the panel exists.</b> A geocoder that
                // answers quickly - or, in a sandbox, fails instantly - brings done() back on
                // the event thread while the constructor is still running, and updateChartData
                // then reaches for a SwissEph that has not been made yet. Recording the moment
                // is safe; drawing with it is not, and the constructor's own update is a few
                // lines behind and will use what was just stored.
                if (SkymapPanel.this.sw == null || SkymapPanel.this.chartPanel == null) {
                    return;
                }
                SkymapPanel.this.updateChartData();
                SkymapPanel.this.chartPanel.repaint();
            }
        }.execute();
    }

    /** The sky's chosen zone, when the reader has overridden what the place resolved to. */
    private void applySkyZone(String zoneOverride) {
        if (zoneOverride == null || zoneOverride.isEmpty()) {
            return;
        }
        try {
            this.installSubjects(this.subjectA, this.subjectB,
                this.subjectSky.movedTo(this.subjectSky.placeName, this.skyRing.latitude,
                    this.skyRing.longitude, ZoneId.of(zoneOverride).getId()));
        } catch (Exception bad) {
            System.out.println("Ignoring unknown sky time zone \"" + zoneOverride + "\"");
        }
    }

    /** The sky's moment, or now in the sky's own zone when the fields are blank. */
    private void setSkyMoment(String date, String time) {
        // <b>Through the subject, not past it.</b> This wrote skyRing.time directly and left
        // subjectSky empty, so the two disagreed about what the sky was - the field said this
        // evening and the record said "not entered", and the chip that reads the record could
        // not name the chart it draws. A loose field written beside its own record is the
        // divergence ChartSubject exists to prevent, arriving by the back door.
        ZonedDateTime when;
        try {
            when = date.isEmpty() || time.isEmpty() ? null : ZonedDateTime.of(
                java.time.LocalDate.parse(date),
                java.time.LocalTime.parse(time),
                ZoneId.of(this.skyTimeZoneId));
        } catch (Exception notAMoment) {
            when = null;                    // a half-typed date is not a moment
        }
        if (when == null) {
            when = ZonedDateTime.now(ZoneId.of(this.skyTimeZoneId));
        }
        this.installSubjects(this.subjectA, this.subjectB,
            this.subjectSky.movedTo(this.subjectSky.placeName, this.skyRing.latitude,
                this.skyRing.longitude, this.skyTimeZoneId).at(when));
    }

    // ---------------------------------------------------------------- the three subjects
    //
    // <b>One record per chart, and the loose fields below are written from them in one
    // place.</b> The wheel's three charts were nine separate fields filled by eleven
    // positional strings, and which string meant what depended on the mode - so the sky could
    // be handed Chart B's birthplace, the partner ring could be handed this moment, and
    // pressing Play could walk somebody's birth time forward. Every one of those was writeable
    // because nothing knew that "Chart B" and "the sky" were different things.
    //
    // The loose fields stay, because seven thousand lines read them and rewriting all of that
    // would be a larger risk than the bug. What changes is that nothing else assigns them:
    // installSubjects is the only writer, and it takes each value from the subject it belongs
    // to. A crossing now has to be written into one visible method rather than being possible
    // anywhere.

    /** Chart A - the person or event the chart is cast for. */
    private com.zodiacomputing.ourania.astro.ChartSubject subjectA =
        com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart A");

    /** Chart B - the second person, in a synastry or a composite. */
    private com.zodiacomputing.ourania.astro.ChartSubject subjectB =
        com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart B");

    /** The sky - this moment, where the reader is, and what transits are read from. */
    private com.zodiacomputing.ourania.astro.ChartSubject subjectSky =
        com.zodiacomputing.ourania.astro.ChartSubject.empty("Sky");

    /** Chart A, for anything that needs to ask what is on the inner wheel. */
    public com.zodiacomputing.ourania.astro.ChartSubject chartASubject() {
        return this.subjectA;
    }

    /** Chart B. */
    public com.zodiacomputing.ourania.astro.ChartSubject chartBSubject() {
        return this.subjectB;
    }

    /** The sky. */
    public com.zodiacomputing.ourania.astro.ChartSubject skySubject() {
        return this.subjectSky;
    }

    /**
     * Which wheels are drawn, without touching what is on them.
     *
     * <b>Pressing a ring chip used to rebuild the chart from the setup form.</b> applyRings
     * set the mode and then called generateChart, which re-read eleven fields, re-ran the
     * geocoder and re-parsed every date - so a button meaning "show me the sky as well" went
     * out to the network and re-derived two birth charts to answer it. That is also where the
     * crossing lived: re-reading a form whose rows meant different things in different modes
     * is precisely what let a chip change what a field meant.
     *
     * A chart is a function of its subjects. The mode chooses which subjects are drawn; it
     * does not decide what they are. That separation is the whole lesson from the libraries
     * that do this well, and this is the door for it - which is also why a chip press is now
     * instant and needs no network.
     */
    public void applyChartMode(ChartMode mode, boolean transits) {
        this.applyChartMode(mode, transits, this.ringAOn, this.ringBOn);
    }

    /**
     * The mode and who is in it, applied as one thing.
     *
     * <b>Two calls would be two frames.</b> Setting the composition and then the mode casts
     * once with the old pairing - a synastry ring around a chart that no longer has two people
     * in it - and the reader sees that frame before the second call corrects it.
     */
    public void applyChartMode(ChartMode mode, boolean transits, boolean chartAIn,
            boolean chartBIn) {
        this.ringAOn = chartAIn;
        this.ringBOn = chartBIn;
        this.castRoles();
        this.installMode(mode, transits);
        this.updateChartData();
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
        if (this.window != null && this.ringBar != null) {
            this.window.syncRingBar(this.ringBar);
        }
    }

    /**
     * The mode, and everything that follows from it. One statement, two callers.
     *
     * Generating a chart and switching which rings it shows had two copies of this, which is
     * how they came to differ - and the difference was that only one of them rebuilt the
     * subjects, so which of the two you went through decided whether your chart survived.
     */
    private void installMode(ChartMode mode, boolean transits) {
        this.chartMode = mode;
        this.transitsEnabled = transits;
        this.applyRingFlags();
        this.cachedFrame = null;
        this.relationshipFrame = null;
        this.relationshipCacheKey = null;
        if (this.showTransitChart) {
            this.animateTarget = "Both";
            if (this.animateCombo != null) {
                this.animateCombo.setSelectedItem("Both");
            }
            this.aspectFilter = "Both";
            if (this.filterCombo != null) {
                this.filterCombo.setSelectedItem("Both");
            }
            if (this.alignCombo != null) {
                this.alignCombo.setEnabled(true);
            }
        } else {
            this.animateTarget = "Natal";
            if (this.animateCombo != null) {
                this.animateCombo.setSelectedItem("Natal");
            }
            this.aspectFilter = "Natal-Natal";
            if (this.filterCombo != null) {
                this.filterCombo.setSelectedItem("Natal-Natal");
            }
            this.houseAlignment = "Natal";
            if (this.alignCombo != null) {
                this.alignCombo.setSelectedItem("Natal");
                this.alignCombo.setEnabled(false);
            }
            this.syncPinToTransitAvailability();
        }
    }

    /**
     * Installs the three subjects, and writes every per-wheel field from its own subject.
     *
     * <b>The one writer.</b> Each line below takes a value from the subject whose name it
     * carries and from no other, which is the property the old code could not state and
     * therefore could not keep. NavigationCheck asserts it by installing three deliberately
     * distinct subjects and reading back every field: swap any two of these lines and it
     * fails.
     *
     * A subject nobody has entered leaves its wheel's moment null rather than defaulting to
     * now, so "no chart" and "a chart of this instant" stay different answers.
     */
    void installSubjects(com.zodiacomputing.ourania.astro.ChartSubject a,
            com.zodiacomputing.ourania.astro.ChartSubject b,
            com.zodiacomputing.ourania.astro.ChartSubject sky) {
        this.subjectA = a;
        this.subjectB = b;
        this.subjectSky = sky;
        this.castRoles();
    }

    /** Whether Chart A and Chart B are in the chart at all, as opposed to merely entered. */
    private boolean ringAOn = true;
    private boolean ringBOn = true;

    /** Which subject is on the inner wheel - Chart A, or Chart B, or nobody and it is the sky. */
    private com.zodiacomputing.ourania.astro.ChartSubject anchorSubject;

    /**
     * Which chart is on which wheel, given which chips the reader has open.
     *
     * <b>Who is in the chart and who is drawn were the same switch, and they are not the same
     * question.</b> Chart A's chip only folded its glyphs away: deselect it in a synastry and
     * the wheel still cast a synastry, still computed cross-aspects to a chart it was no longer
     * showing, and still called itself one. David described what it should do instead, and it
     * is a rule about roles rather than about drawing: "I deselect Chart A, now I'm no longer
     * seeing a synastry, I'm seeing a natal-transit chart... I then deselect Chart B, I am
     * seeing a current time and place natal chart."
     *
     * So the innermost chart still selected is the natal one, and the rest ring outward.
     * Deselecting the inner wheel promotes whatever was outside it rather than leaving a hole.
     * The sky is last in that order and always available, which is what makes every combination
     * land on a chart that exists - there is no selection that means "draw nothing".
     *
     * <b>The identity does not move with the role.</b> subjectA stays Chart A even when Chart B
     * is the one being cast, so a chip still describes its own chart and the readout names the
     * wheel by the label of whoever is actually on it.
     */
    private void castRoles() {
        com.zodiacomputing.ourania.astro.ChartSubject a = this.subjectA;
        com.zodiacomputing.ourania.astro.ChartSubject b = this.subjectB;
        com.zodiacomputing.ourania.astro.ChartSubject sky = this.subjectSky;

        boolean useA = this.ringAOn && a.entered();
        boolean useB = this.ringBOn && b.entered();
        com.zodiacomputing.ourania.astro.ChartSubject anchor = useA ? a : (useB ? b : null);
        // Only ever Chart B: the engine's outer wheel is a second person in a synastry and the
        // sky everywhere else, and the sky reaches it through its own slot rather than this one.
        com.zodiacomputing.ourania.astro.ChartSubject outer =
            useA && useB ? b : com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart B");

        this.anchorSubject = anchor;
        this.innerIsBirthChart = anchor != null;
        com.zodiacomputing.ourania.astro.ChartSubject inner =
            anchor == null ? com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart A")
                : anchor;

        this.natalRing.time = inner.moment;
        this.natalRing.latitude = inner.latitude;
        this.natalRing.longitude = inner.longitude;
        this.baseLocationName = inner.placeName;
        this.baseTimeZoneId = inner.zoneId;
        this.baseTimeUnknown = inner.timeUnknown;

        this.outerRing.time = outer.moment;
        this.outerRing.latitude = outer.latitude;
        this.outerRing.longitude = outer.longitude;
        this.transitLocationName = outer.placeName;
        this.transitTimeZoneId = outer.zoneId;

        this.skyRing.time = sky.moment;
        this.skyRing.latitude = sky.latitude;
        this.skyRing.longitude = sky.longitude;
        this.skyTimeZoneId = sky.zoneId;
    }

    /**
     * Puts Chart A and Chart B in or out of the chart, and re-casts from what is left.
     *
     * The rings' door. The mode that goes with a composition is still decided in Chart Setup -
     * this says who is in the chart, not what kind of chart it is, and those two have to be set
     * together or the wheel draws a synastry ring for a chart with one person in it.
     */
    void setComposition(boolean chartAIn, boolean chartBIn) {
        this.ringAOn = chartAIn;
        this.ringBOn = chartBIn;
        this.castRoles();
    }

    /** Whether Chart A is in the chart, for a chip that has to show what it did. */
    boolean chartAIn() {
        return this.ringAOn;
    }

    /** Whether Chart B is in the chart. */
    boolean chartBIn() {
        return this.ringBOn;
    }

    /**
     * Moves the sky to a new moment, keeping its place.
     *
     * The transport's door. It used to step whichever field the mode pointed at, which is how
     * Play came to walk a birth time; there is one thing it can move now and it is named.
     */
    void moveSky(ZonedDateTime when) {
        this.installSubjects(this.subjectA, this.subjectB, this.subjectSky.at(when));
    }

    /**
     * One subject, from the strings a form supplies and the lookups they imply.
     *
     * <b>The same construction for every wheel, which is the point.</b> Chart A used to get a
     * zone override and a relocation and Chart B got neither, because the code that built them
     * was written twice and only one copy grew the features. Passing empty strings for the two
     * that do not apply is how a caller says "not this one" without there being a second path.
     *
     * A date that will not parse yields a subject that was never entered, rather than one
     * quietly dated today - which is the distinction the whole class exists to keep.
     *
     * @param fallback the subject to keep the place of when the lookup fails, so an
     *                 unreachable geocoder leaves a chart where it was rather than at zero
     */
    private com.zodiacomputing.ourania.astro.ChartSubject subjectFrom(String label,
            String date, String time, String place, String zoneOverride, String relocateTo,
            boolean timeUnknown,
            com.zodiacomputing.ourania.astro.ChartSubject fallback) {
        double lat = fallback.latitude;
        double lon = fallback.longitude;
        String name = fallback.placeName;
        String zone = fallback.zoneId;

        Geocoder.Result found = this.syncGeocode(place);
        if (found != null) {
            lat = found.lat;
            lon = found.lon;
            name = found.name;
            zone = found.tzId;
        }
        // <b>The reader's zone wins over the geocoder's.</b> Applied after the lookup rather
        // than instead of it, because the place still supplies the latitude and longitude the
        // houses are cast from - it is only the zone that was in doubt. Validated here: an id
        // Java does not know would throw inside ZonedDateTime.of below, on a worker, and lose
        // the whole chart to a stack trace.
        if (zoneOverride != null && !zoneOverride.isEmpty()) {
            try {
                zone = ZoneId.of(zoneOverride).getId();
            } catch (Exception bad) {
                System.out.println("Ignoring unknown time zone \"" + zoneOverride + "\"");
            }
        }
        // <b>Relocation, applied after the birthplace and before the time is read.</b> The
        // coordinates move and the zone does not. Ordering matters: the zone above was
        // resolved from the BIRTHPLACE, which is what the birth time was written in, and
        // taking the new place's zone here would shift the instant as well as the houses.
        if (relocateTo != null && !relocateTo.isEmpty()) {
            Geocoder.Result there = this.syncGeocode(relocateTo);
            if (there != null) {
                lat = there.lat;
                lon = there.lon;
                name = there.name + " (relocated)";
            }
        }
        try {
            java.time.LocalDate d = LocalDate.parse(date,
                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime t = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            // <b>Through Moments, which had been written and never called.</b> Twice a year a
            // local time is not one instant: the clocks go forward and an hour never happens,
            // or they go back and it happens twice. ZonedDateTime.of resolves both silently -
            // it shifts the first and takes the earlier of the second - so a birth in that hour
            // was cast on an assumption nobody was told about. Moments.resolve returns the same
            // instant Java would, deliberately, so no saved chart moves; what it adds is that
            // the assumption now has a sentence and the other reading has a handle.
            com.zodiacomputing.ourania.astro.Moments.Resolved r =
                com.zodiacomputing.ourania.astro.Moments.resolve(d, t, ZoneId.of(zone));
            return com.zodiacomputing.ourania.astro.ChartSubject.of(label,
                r.when, name, lat, lon, zone, timeUnknown)
                .withTimeNote(r.note, r.other);
        } catch (Exception notADate) {
            // Half a date is not a moment. The place is kept, so a reader who has typed a
            // location and not yet a birthday does not lose the location too.
            return com.zodiacomputing.ourania.astro.ChartSubject.empty(label)
                .movedTo(name, lat, lon, zone);
        }
    }

    /** Repaints just the wheel, for a bloom frame. */
    private void repaintWheel() {
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
    }

    /**
     * A time zone the reader chose, overriding the one the geocoder inferred.
     *
     * <b>The geocoder guesses, and for an ambiguous place name it guesses wrong.</b> There are
     * a dozen Springfields and two Bostons; picking the wrong one moves the chart by hours,
     * and until now there was no way to say so - the zone was whatever the lookup returned and
     * the reader had no vote. Empty means "trust the location", which is still the default.
     */
    private String baseZoneOverride = "";

    /** Chart B's chosen zone, by the same rule as Chart A's. */
    private String transitZoneOverride = "";

    /**
     * Where the chart is relocated to, or empty for the birthplace.
     *
     * <b>Relocation moves the houses, not the moment.</b> A relocated chart asks what the same
     * instant looks like from somewhere else: every planet is in the degree it was in - they
     * depend on time alone - while the Ascendant, the Midheaven and all twelve cusps are recast
     * for the new latitude and longitude. So this replaces the coordinates and deliberately
     * does <b>not</b> touch the time zone: the birth instant is fixed, and reading the birth
     * time in the new place's zone would move the chart in time as well, which is a different
     * chart of a different moment.
     */
    private String relocateTo = "";
    /** The bottom drawer: transport controls, with the live moment on its handle. */
    private Drawer timeDrawer;

    /**
     * Where the midpoint composite's house frame is derived, or NaN for the couple's own
     * geographic midpoint.
     *
     * <b>NaN means "not chosen", not "zero".</b> Latitude 0 is the equator and a perfectly
     * legitimate answer, so a sentinel that a user could also type would make "unset" and
     * "on the equator" the same state.
     *
     * <b>Only the latitude changes the chart</b> - see the javadoc on the four-argument
     * computeMidpointComposite. The longitude is carried so the frame can say where it was
     * derived for, and for nothing else.
     */
    private double compositeRefLat = Double.NaN;
    private double compositeRefLon = Double.NaN;
    /** The name the user picked, for the panel to show. Null when using the midpoint. */
    private String compositeRefPlace;
    private String transitLocationName = "Los Angeles, CA";
    private String transitTimeZoneId = ZoneId.systemDefault().getId();

    /**
     * Where the sky is read from, and when.
     *
     * <b>The sky borrowed Chart B's place, and that is the same crossing as borrowing its
     * moment.</b> outerRing.latitude and outerRing.longitude are the second person's birthplace in a
     * synastry, so the third ring - the sky over both of them - was being cast for wherever
     * Chart B was born rather than for where the reader is. It looked right whenever the two
     * happened to be the same city and was wrong the rest of the time, which is the worst way
     * for a coordinate to be wrong.
     *
     * The sky now has its own row on the setup form and its own three fields here, so nothing
     * it needs is shared with a birth chart.
     */
    /**
     * Whether a Chart A has actually been entered.
     *
     * <b>A cold open drew the current sky and called it the natal wheel.</b> natalRing.time is
     * initialised to now, so before anything is generated the inner wheel holds this moment -
     * and the setup form disagreed with it from the first frame, since the form's own base
     * date read 1990-01-01. Two surfaces, one chart, and neither of them right.
     *
     * With no Chart A the inner wheel now draws the sky, and everything that names it says so:
     * the chip is greyed with a reason, and the readout calls it Sky. Drawing the sky is not
     * the defect - claiming it is somebody's birth chart was.
     */
    /**
     * Whether the inner wheel is somebody's birth chart, as opposed to the sky.
     *
     * <b>This was called chartALoaded and answered two questions.</b> It meant "there is a
     * Chart A" and it meant "the inner wheel is a real chart", which were the same sentence
     * only while Chart A was the only chart that could sit there. Chart B can be promoted onto
     * that wheel now, so they have come apart: the readouts, the houses and the aspect grid all
     * want this one, and a chip wants hasChartA. One field with two meanings is the defect this
     * project logs most often, so it is two.
     */
    private boolean innerIsBirthChart;

    /** True when a Chart A has been generated, for the chip that folds it. */
    public boolean hasChartA() {
        return this.subjectA != null && this.subjectA.entered();
    }

    /** Whether the inner wheel is a birth chart rather than the sky. */
    public boolean innerIsBirthChart() {
        return this.innerIsBirthChart;
    }

    private String skyTimeZoneId = ZoneId.systemDefault().getId();
    private String animateTarget = "Transit";
    private String aspectFilter = "Natal-Natal";
    /** The chart's own settings, shown on the Settings screen rather than under the wheel. */
    private JPanel chartControls;

    private JComboBox<String> animateCombo;
    private JComboBox<String> filterCombo;
    private JComboBox<String> alignCombo;
    private String houseAlignment = "Natal";
    private String wheelPin = "Natal Asc";
    private JComboBox<String> pinCombo;
    /**
     * Which rings are drawn, from the mode, the transits flag and what the middle ring carries.
     *
     * <b>Extracted because a second caller appeared and copying it would have been the bug.</b>
     * The Settings screen can change whether the middle ring is progressed, which changes
     * whether the sky gets a ring of its own - and the hook that reloads that setting recomputed
     * the flag it reads and not the flags that follow from it, so choosing Progressions left the
     * third ring off until something else happened to change the mode. One statement, two
     * callers, and neither can drift from the other.
     */
    private void applyRingFlags() {
        this.showTransitChart = SkymapPanel.outerWheelShown(this.chartMode, this.transitsEnabled);
        this.showTriWheel = SkymapPanel.triWheelShown(this.chartMode, this.transitsEnabled,
            this.showProgressed());
        // The rings open or fold to match. Set after the flags, so a bloom never disagrees
        // with the thing it is animating.
        this.outerBloom.set(this.showTransitChart);
        this.triBloom.set(this.showTriWheel);
    }

    /**
     * The moment the middle ring was cast at, whatever it is carrying.
     *
     * Chart B's birth moment in a synastry, this moment for a transit ring, and a date a few
     * weeks after birth for a progressed one. Written by updateChartData, read by anything that
     * wants to print what the ring is showing.
     */
    private SweDate outerCastAt;

    /** The middle ring's moment as text, or empty when there is nothing on that ring. */
    private String outerCastLabel() {
        SweDate d = this.outerCastAt;
        if (d == null || !this.showTransitChart) {
            return "";
        }
        int hour = (int) d.getHour();
        int minute = (int) Math.round((d.getHour() - hour) * 60.0);
        if (minute == 60) {
            minute = 0;
            hour = (hour + 1) % 24;
        }
        return String.format("%04d-%02d-%02d %02d:%02d UT",
            d.getYear(), d.getMonth(), d.getDay(), hour, minute);
    }

    /** Tri-wheel sky positions. Only populated when {@link #showTriWheel} is true. */
    private boolean[] shown = Settings.loadBodySelection();
    public double[] activeCusps = new double[13];
    public double activeAscendant;
    private Timer animationTimer;
    private boolean isPlaying = false;
    private int animationDirection = 1;
    /**
     * How far one animation tick moves the chart.
     *
     * <b>"1 Hour" because that is what the Step combo has always displayed.</b> Until
     * 2026-08-24 this field said "1 Day" while the combo was constructed showing "1 Hour",
     * and the two never met: the combo was seeded with a literal BEFORE its listener was
     * attached, so no event fired and nothing corrected the field. Every user who did not
     * touch the dropdown got day-sized steps from a control reading "1 Hour", and it fixed
     * itself the moment they changed the selection to anything and back.
     *
     * The combo is now seeded from this field, which is what every other combo in this class
     * already did - it was the only one seeded from a literal.
     */
    private String stepAmount = "1 Hour";
    private ChartPanel chartPanel;
    /** Which aspects are drawn, by {@code Aspects.Type} ordinal. Absent setting means all. */
    private boolean[] aspectShown = Settings.loadAspectSelection();

    /** Which pairs get a line. See {@link #drawsPair}. */
    private String aspectMode = Settings.aspectMode();

    private char houseSystem = (char)80;
    private String currentHouseSystemName = "Placidus";
    public static final int BODY_COUNT = Bodies.count();
    private static final String[] BODY_NAMES = new String[BODY_COUNT];
    private static final String[] BODY_GLYPHS = new String[BODY_COUNT];
    private static final int[] BODY_ELEMENTS = new int[BODY_COUNT];
    private static final int SUN = Bodies.indexOf("sun");
    private static final int MOON = Bodies.indexOf("moon");
    private static final int NORTH_NODE = Bodies.indexOf("north_node");
    /**
     * Package-private rather than private so InterpretationPanel's cards can use the same
     * twelve strings the wheel draws. <b>Widened, not copied.</b> A second table of sign
     * glyphs in the panel is the defect this project logs more than any other - one rule in
     * two places, drifting - and these carry U+FE0E deliberately, which a hand-typed copy
     * would silently lose and render as colour emoji on some faces.
     */
    static final String[] ZODIAC_SYMBOLS;
    private static final Color FIRE;
    private static final Color EARTH;
    private static final Color AIR;
    private static final Color WATER;
    private static final Color[] ELEMENT_COLORS;
    private static final double TEXT_TINT = 0.35;
    private static final String[] ELEMENT_TEXT_HEX;
    private static final Font NATAL_GLYPH_FONT;
    private static final Font TRANSIT_GLYPH_FONT;
    private static final Font ANGLE_FONT;
    private OuraniaWindow window;
    public static final String EPHE_PATH = com.zodiacomputing.ourania.astro.Ephemeris.PATH;
    private ReadingTier readingTier = ReadingTier.NONE;
    private String frameCacheKey;

    /**
     * The composite, cached separately from {@link #cachedFrame}, and it must stay separate.
     *
     * <b>The two were one field until 2026-08-25, and a composite chart was drawn wrong from
     * the second redraw onward.</b> {@code getCurrentChart} built the composite into
     * {@code cachedFrame} and set {@code frameCacheKey} to null; {@code frameForCurrentChart}
     * then found null unequal to its own key, <b>overwrote the composite with person A's natal
     * chart</b>, and stamped a key on it. Since {@code getCurrentChart} only rebuilds when the
     * field is null, every later call - the wheel's own {@code loadFrameIntoBase} among them -
     * got the natal chart back and drew it captioned "Composite Chart".
     *
     * Measured on a 1985/1991 pair before the fix: the composite Sun is 286.87 and person A's
     * is 353.75, so the wheel was <b>66.9 degrees out, better than two signs</b>, with the
     * Ascendant 237 degrees out. It was right on the first draw and wrong on every one after,
     * because {@code updateChartData} ends by calling {@code refreshReadingIfShown} and the
     * reading is what poisons the cache.
     *
     * Keyed on everything that defines the composite, so editing either person's data rebuilds
     * it without anyone having to remember to invalidate by hand.
     */
    private ChartFrame relationshipFrame;
    private String relationshipCacheKey;
    private ChartFrame cachedFrame;

    /**
     * Chart B's own frame, for synastry mode only, cached on its own inputs.
     *
     * A second cache rather than a second use of {@link #cachedFrame}, because that field
     * holds chart A and the two are needed at the same time. Keyed the same way through
     * {@link #frameKey}, so a change to either person's date or place recomputes only that
     * person's frame.
     */
    private String synastryBKey;
    private ChartFrame synastryBFrame;

    /**
     * The moment the outer wheel shows when the inner wheel is a composite.
     *
     * <b>A third time, because the other two are spoken for.</b> In a composite mode
     * natalRing.time and outerRing.time are the two PEOPLE - they are what the composite is
     * built from - so there is nowhere to put "now" without a slot of its own. This is that
     * slot, and it is the only reason transits to a composite were not already possible: the
     * astro layer needed no change at all. {@code Transits.toNatal} takes two ChartFrames and
     * has never cared whether the first one is a birth chart.
     *
     * Defaults to now, and the existing Now / Play / step controls drive it while a composite
     * is on screen, which is how someone actually reads composite transits - by sweeping.
     */
    private Timer refreshDebounce;
    public static final String[] SIGN_NAMES;
    private static final double ANGLE_SPEED = 361.0;

    public ZonedDateTime getChartTime() {
        return this.natalRing.time;
    }

    private static String[] buildElementTextHex() {
        String[] stringArray = new String[ELEMENT_COLORS.length];
        for (int i = 0; i < ELEMENT_COLORS.length; ++i) {
            Color color = SkymapPanel.lighten(ELEMENT_COLORS[i], 0.35);
            stringArray[i] = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }
        return stringArray;
    }

    private static Color lighten(Color color, double d) {
        return new Color((int)Math.round((double)color.getRed() + (double)(255 - color.getRed()) * d), (int)Math.round((double)color.getGreen() + (double)(255 - color.getGreen()) * d), (int)Math.round((double)color.getBlue() + (double)(255 - color.getBlue()) * d));
    }

    private static double angularGap(double d, double d2) {
        double d3 = Math.abs(d - d2) % 360.0;
        return d3 > 180.0 ? 360.0 - d3 : d3;
    }

    private static int[] radialLevels(double[] dArray, boolean[] blArray, double d, double d2, double d3) {
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
                    if (!(SkymapPanel.angularGap(d4, d7) < d6)) continue;
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

    private static String glyphFor(int n, Font font) {
        Bodies.Def def = Bodies.at(n);
        return font.canDisplay(def.glyph.codePointAt(0)) ? def.glyph : def.fallback;
    }

    /**
     * What the globe view needs from this panel, and nothing more.
     *
     * <b>Accessors rather than a copy of the chart.</b> The globe is a second view of one
     * chart, not a second chart: it reads these, so it cannot drift from what the wheel draws.
     * Each one already existed in some form - as a private method, an instance colour lookup,
     * or a static array - and is exposed here rather than reimplemented there.
     */
    static String zodiacSymbol(int signIndex) {
        return ZODIAC_SYMBOLS[((signIndex % 12) + 12) % 12];
    }

    /** A body's glyph, by registry index. */
    static String glyphOf(int body) {
        return body >= 0 && body < BODY_GLYPHS.length ? BODY_GLYPHS[body] : "?";
    }

    /** An element's colour, for surfaces that have no panel instance to hand. */
    static Color elementColorFor(int elementIndex) {
        Color c = ChartPalette.colorOr(SkymapPanel.elementTextHex(elementIndex), null);
        return c == null ? new Color(228, 229, 234) : c;
    }

    /**
     * A body's own colour, for surfaces with no panel instance to hand.
     *
     * The globe's bound and decan rings write a ruling planet's glyph, and it has to be the
     * colour that planet is everywhere else - the same element lookup the wheel's glyphs use.
     */
    static Color bodyInkFor(int body) {
        return SkymapPanel.elementColorFor(SkymapPanel.elementOfBody(body));
    }

    /** Longitude the view is pinned to - the Ascendant unless the reader chose otherwise. */
    double pinLongitude() {
        return this.getPinLongitude();
    }

    /**
     * The colour an aspect between two bodies is drawn in, or null when there is none.
     *
     * <b>Through visibleAspect and drawsPair, the same two gates drawAspectLine uses.</b> An
     * aspect the reader has switched off, or one the aspect mode excludes, must be absent from
     * the globe for the same reason it is absent from the wheel - otherwise the two views
     * disagree about what is in aspect, which is worse than either being wrong alone.
     */
    Color aspectInkFor(double lonA, double lonB, int a, int b, boolean cross) {
        double sep = Math.abs(lonA - lonB);
        if (sep > 180.0) {
            sep = 360.0 - sep;
        }
        Aspects.Type type = this.visibleAspect(sep, a, b, this.isSynastryPair(cross));
        if (type == null || !this.drawsPair(a, b)) {
            return null;
        }
        // Cached: this runs for every pair on every globe frame, and Color.decode parses a
        // string each time. There are fifteen aspects.
        return GLOBE_ASPECT_INK.computeIfAbsent(type.label, label -> {
            Color base = Color.decode(SkymapPanel.getAspectColorHex(label));
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), 150);
        });
    }

    /**
     * Whether the globe should light this chord.
     *
     * <b>The same rule the flat wheel draws by, not a second one.</b> The globe reads the
     * hover the aspect grid sets, so pointing at a cell lights the chord in whichever view is
     * on screen - which is the whole reason the grid and the wheel share a highlight at all.
     * A rule with two implementations is how the sky ring came to be lit by the partner ring's
     * hover in the first place.
     */
    boolean lightsChord(int a, int b, int wheel) {
        return this.isHighlighted(a, b, wheel);
    }

    private static final java.util.Map<String, Color> GLOBE_ASPECT_INK =
        new java.util.concurrent.ConcurrentHashMap<>();

    private static Color unusedAspectInkTail() {
        return null;
    }

    /**
     * The whole degree the cursor is over on the globe's scale, or -1.
     *
     * <b>Kept beside focusBody rather than inside it.</b> A body and a degree are different
     * things to be pointing at - one is a placement, the other is a place - and the reader can
     * be over a tick with no body anywhere near it. Folding the two into one field would mean
     * choosing which of them a hover means, and the answer differs by where the cursor is.
     */
    private int focusDegree = -1;

    /**
     * The house whose number the cursor is over, or -1.
     *
     * Beside focusBody and focusDegree for the same reason those are beside each other: a
     * body, a degree and a house are three different things to be pointing at, and which of
     * them a hover means depends on where the cursor is rather than on a mode.
     */
    private int focusHouse = -1;

    /**
     * The lunar mansion the cursor is over, 1 to 28, or -1.
     *
     * Beside focusDegree and focusHouse for the reason those are beside each other: a station
     * is a fourth thing to be pointing at, and which of the four a hover means depends on
     * where the cursor is rather than on a mode.
     */
    private int focusMansion = -1;

    /** The mansion the cursor is over, for the globe's band to light. */
    int focusedMansion() {
        return this.focusMansion;
    }

    /** Records the mansion under the cursor. True when it changed and a repaint is due. */
    boolean setFocusMansion(int mansion) {
        if (this.focusMansion == mansion) {
            return false;
        }
        this.focusMansion = mansion;
        return true;
    }

    /**
     * Which station the Moon is in, or null when there is no Moon to place.
     *
     * <b>Asked once, because two rings draw it.</b> The flat wheel fills the Moon's station
     * and the globe's band does the same, and a second copy of "which Moon" is how the two
     * views come to disagree about the one station that is highlighted - the defect this
     * project keeps finding in the seams between surfaces rather than in the arithmetic.
     */
    com.zodiacomputing.ourania.astro.LunarMansions.Mansion moonMansion() {
        com.zodiacomputing.ourania.astro.ChartFrame frame = this.getCurrentChart();
        return frame == null ? null
            : com.zodiacomputing.ourania.astro.LunarMansions.ofMoon(frame);
    }

    /** The house the cursor is over, for the globe to carve. */
    int focusedHouse() {
        return this.focusHouse;
    }

    /** Records the house under the cursor. True when it changed and a repaint is due. */
    boolean setFocusHouse(int house) {
        if (this.focusHouse == house) {
            return false;
        }
        this.focusHouse = house;
        return true;
    }

    /** The degree under the cursor, for the globe's scale to light. */
    int focusedDegree() {
        return this.focusDegree;
    }

    /** Records the degree under the cursor. True when it changed and a repaint is due. */
    boolean setFocusDegree(int degree) {
        if (this.focusDegree == degree) {
            return false;
        }
        this.focusDegree = degree;
        return true;
    }

    /** The body the cursor is resting on, or -1. Read by the globe to light its wedges. */
    int focusedBody() {
        return this.focusBody;
    }

    /**
     * The focused body's longitude, or NaN when nothing is focused.
     *
     * <b>From the array the focus is actually on.</b> focusTransit says which ring the cursor
     * found the body on, and reading natalRing.lon regardless would light the sign a natal body of the
     * same index happens to occupy - a wedge confidently highlighting the wrong sign, which
     * is worse than not highlighting at all.
     */
    double focusedLongitude() {
        if (this.focusBody < 0) {
            return Double.NaN;
        }
        double[] lon = this.focusTransit ? this.outerRing.lon : this.natalRing.lon;
        return this.focusBody < lon.length ? lon[this.focusBody] : Double.NaN;
    }

    /** Whether the globe should light this body - the cursor is resting on it. */
    boolean onGlobeFocus(int body, boolean outer) {
        return this.focusBody == body && this.focusTransit == outer;
    }

    static boolean aspecting(int n, boolean[] blArray) {
        return blArray[n] && !Bodies.at(n).isAngle();
    }

    private static boolean[] restrict(boolean[] blArray, boolean bl) {
        boolean[] blArray2 = new boolean[blArray.length];
        for (int i = 0; i < blArray.length; ++i) {
            blArray2[i] = blArray[i] && Bodies.at(i).isAngle() == bl;
        }
        return blArray2;
    }

    // ---------------------------------------------------------------- concentric rings
    //
    // Every non-angle point used to be drawn at ONE base radius, separated only when two
    // of them collided. With 24 of them in the registry that reads as a single crowded
    // band, so they are now grouped into three concentric rings by what kind of body they
    // are. Collision staggering still happens, but WITHIN a ring, so a ring stays legible
    // as a ring.
    //
    // This is the only place ring geometry is decided, and that is deliberate. The hit
    // test (mouseMoved/mouseClicked) and the aspect-line painter both call natalRadii and
    // transitRadii rather than computing a radius of their own, so a glyph you can see is
    // a glyph you can click and a line that lands where the glyph is. The grid, the
    // painter and the hit test disagreeing about a body's position is a defect this
    // project has already logged once - do not reintroduce it by inlining a radius.

    // ---------------------------------------------------------------- hover and hit
    //
    // A body's screen position is (centre + radius * unit vector at 180 + pin - longitude).
    // That formula is also inlined into the decompiled click and paint expressions below;
    // these are the canonical form and new code should use them.

    /** Screen x of a longitude at a radius, for a wheel centred at cx with this pin. */
    private static int screenX(int cx, double pin, double lon, int radius) {
        return cx + (int)((double)radius * Math.cos(Math.toRadians(180.0 + pin - lon)));
    }

    /** Screen y of a longitude at a radius, for a wheel centred at cy with this pin. */
    private static int screenY(int cy, double pin, double lon, int radius) {
        return cy + (int)((double)radius * Math.sin(Math.toRadians(180.0 + pin - lon)));
    }

    /**
     * How near the cursor has to be to count as pointing at a body.
     *
     * Was a flat 18.0 everywhere, which was right while every glyph was drawn at 15px and
     * is wrong now that they run from 11px to 19px: too tight for the lights, and loose
     * enough on the asteroids to let a neighbour answer for them.
     *
     * Capped at half the collision spacing its ring was laid out with, so two glyphs can
     * never both claim the same pixel - radialLevels guarantees 32px between natal glyphs
     * and 28px between transit ones, so 16 and 14 are the largest safe values.
     */
    private static int hitRadius(int n, boolean transit) {
        if (Bodies.at(n).isAngle()) {
            return ANGLE_HIT_RADIUS;
        }
        int r = (transit ? SkymapPanel.transitSize(n) : SkymapPanel.natalSize(n)).radius + 2;
        return Math.min(r, transit ? 14 : 16);
    }

    /** The angles are drawn as fixed-size labelled cubes rather than scaled spheres. */
    private static final int ANGLE_HIT_RADIUS = 15;

    /**
     * Index of the point nearest the cursor and inside its hit radius, or -1.
     *
     * Nearest rather than first-found: the old click handler took whichever body the loop
     * reached first, so where two were close the answer depended on registry order rather
     * than on where you actually pointed.
     */
    private int nearestPoint(int x, int y, int cx, int cy, double pin,
                             double[] lon, boolean[] valid, int[] radii, boolean transit) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < BODY_COUNT && i < lon.length; ++i) {
            if (!valid[i]) {
                continue;
            }
            double dx = x - SkymapPanel.screenX(cx, pin, lon[i], radii[i]);
            double dy = y - SkymapPanel.screenY(cy, pin, lon[i], radii[i]);
            double dist = Math.hypot(dx, dy);
            if (dist < SkymapPanel.hitRadius(i, transit) && dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    /**
     * Tooltip for whatever is under the cursor, or null for empty space.
     *
     * The outermost ring is tested first: tri-wheel sky (if present), then the transit/
     * synastry ring, then natal. Each is drawn on top of the one inside it, so this
     * matches what the eye sees where rings overlap.
     */
    private String hoverTextAt(int x, int y) {
        if (this.sw == null || this.natalRing.sd == null || this.chartPanel == null) {
            return null;
        }
        // The same three hit tests, in the same order, as bodyAt - and on the globe, the same
        // one. The note on bodyAt says why the hover card and the focus highlight must agree.
        if (this.globeMode) {
            int hit = GlobeRenderer.bodyAt(this.globe, this.chartPanel.getWidth(),
                this.chartPanel.getHeight(), this, x, y);
            if (hit < 0) {
                return null;
            }
            int ring = hitRing(hit);
            int i = hitBody(hit);
            // <b>Its own ring's numbers.</b> This chose between two arrays on a boolean, so a
            // body found on the sky ring was described with Chart B's longitude and speed -
            // the same card, twice, for two different charts.
            double[] lon = ring == WHEEL_SKY ? this.skyRing.lon
                : (ring == WHEEL_OUTER ? this.outerRing.lon : this.natalRing.lon);
            double[] spd = ring == WHEEL_SKY ? this.skyRing.speed
                : (ring == WHEEL_OUTER ? this.outerRing.speed : this.natalRing.speed);
            return i < lon.length ? this.hoverHtml(i, lon[i], spd[i], ring) : null;
        }
        Geometry g = this.geometry();
        if (g == null) {
            return null;
        }
        if (this.triRingDrawn()) {
            int i = this.nearestPoint(x, y, g.cx, g.cy, g.pin, this.skyRing.lon, this.skyRing.valid,
                g.triRadii(), true);
            if (i >= 0) {
                return this.hoverHtml(i, this.skyRing.lon[i], this.skyRing.speed[i], WHEEL_SKY);
            }
        }
        if (this.outerRingDrawn()) {
            int i = this.nearestPoint(x, y, g.cx, g.cy, g.pin, this.outerRing.lon, this.outerRing.valid,
                g.transitRadii(), true);
            if (i >= 0) {
                return this.hoverHtml(i, this.outerRing.lon[i], this.outerRing.speed[i], WHEEL_OUTER);
            }
        }
        int i = this.nearestPoint(x, y, g.cx, g.cy, g.pin, this.natalRing.lon, this.natalRing.valid,
            g.natalRadii(), false);
        return i >= 0 ? this.hoverHtml(i, this.natalRing.lon[i], this.natalRing.speed[i], WHEEL_NATAL) : null;
    }

    /**
     * The body under the cursor, packed as {@code index | (transit ? TRANSIT_BIT : 0)},
     * or -1.
     *
     * <b>Deliberately the same three hit tests, in the same order, as {@code hoverTextAt}
     * above.</b> The hover card and the focus highlight must agree about which body the cursor
     * is on: a second, subtly different hit test would light one body while describing another,
     * and the reader would have no way to tell which was lying. The wheel has already shipped
     * one bug of that shape - the note above natalRadii records it.
     */
    /**
     * Every radius and angle one paint or one hit test works from.
     *
     * <b>Four places derived this chain independently, and that is how a glyph you can see
     * becomes a glyph you cannot click.</b> On 2026-09-03 the painter drew bodies at
     * RING_SIGN_INNER while bodyAt tested natalRadii(bodyBaseRadius(rings)); with the reader's
     * placement set to the centre the two were 124 pixels apart and nothing on the wheel could
     * be selected. bodyBaseRadius's own header had warned of exactly that and asked both sides
     * to call it - a comment cannot enforce anything, so this does.
     *
     * The three body-radius arrays are built on demand. hoverTextAt runs on every mouse move
     * and wants one of them; computing all three there to look tidy would be a real cost for
     * no gain.
     */
    final class Geometry {
        final int cx;
        final int cy;
        final int[] rings;
        /** Where the bodies sit, after the reader's ring choice - the natal band's ceiling. */
        final int bodyBase;
        /** The natal band's floor, and so the ceiling of the aspect fields. */
        final int natalFloor;
        /** The rotation, so a hit test and the painter agree on where zero is. */
        final double pin;

        private int[] natal;
        private int[] transit;
        private int[] tri;
        private int[] discs;

        private Geometry(int w, int h) {
            this.cx = w / 2;
            this.cy = h / 2;
            // Laid out from what is on screen, not from what was asked for. A ring folding
            // away still occupies its band until the fold ends - otherwise the whole wheel
            // resizes on the first frame of the fold and there is nothing left to animate.
            this.rings = SkymapPanel.ringRadii(w, h,
                SkymapPanel.this.outerOpenFraction(), SkymapPanel.this.triOpenFraction(),
                SkymapPanel.this.layerOpen(Layer.DECANS),
                SkymapPanel.this.layerOpen(Layer.SIGNS),
                SkymapPanel.this.layerOpen(Layer.BOUNDS),
                SkymapPanel.this.layerOpen(Layer.DEGREES),
                SkymapPanel.this.layerOpen(Layer.MANSIONS));
            this.bodyBase = SkymapPanel.this.bodyBaseRadius(this.rings);
            this.natalFloor = this.bodyBase
                - SkymapPanel.natalBandDepth(Math.max(1, this.bodyBase));
            this.pin = SkymapPanel.this.getPinLongitude();
        }

        int ring(int which) {
            return this.rings[which];
        }

        int[] natalRadii() {
            if (this.natal == null) {
                this.natal = SkymapPanel.bandRadii(SkymapPanel.this.natalRing.lon,
                    SkymapPanel.this.natalRing.valid, this.bodyBase, this.natalFloor,
                    SkymapPanel.NATAL_EDGE, SkymapPanel.NATAL_SPACING);
            }
            return this.natal;
        }

        int[] transitRadii() {
            if (this.transit == null) {
                this.transit = SkymapPanel.bloomed(
                    SkymapPanel.bandRadii(SkymapPanel.this.outerRing.lon, SkymapPanel.this.outerRing.valid,
                        this.rings[RING_TRANSIT], this.rings[RING_BODY_TOP]),
                    this.rings[RING_BODY_TOP], SkymapPanel.this.outerOpenFraction());
            }
            return this.transit;
        }

        /**
         * The circle an aspect line between two rings is drawn on.
         *
         * <b>One field per ring, nested at the depth its ring sits at.</b> Natal-to-natal
         * lines in the innermost circle, the first outer ring's in the next, the sky's in the
         * widest - so three sets of lines that used to cross each other across the whole wheel
         * now occupy three separate bands, and a reader can follow one without the other two
         * running through it.
         *
         * <b>The endpoints stay at each body's own longitude</b>, so a line still points at
         * the bodies it joins even though it no longer touches them: an opposition is still a
         * diameter and a sextile is still a sixth of the way round. What it loses is the leg
         * out to the glyph, which is the part that was doing the tangling.
         *
         * Sized from the natal band's floor, so the fields fill the space the bodies do not
         * use and hold still while the chart changes.
         *
         * @param level 0 for the natal wheel, 1 for the ring outside it, 2 for the sky
         */
        int aspectDisc(int level) {
            if (this.discs == null) {
                // <b>From the band's floor, not from the innermost body.</b> Measuring the
                // bodies made the fields resize as the chart changed - switch a point off and
                // every aspect line moves - which reads as the wheel breathing rather than as
                // a layout. The floor is a property of the wheel, so the fields hold still.
                int top = Math.max(30, this.natalFloor - 14);
                this.discs = new int[] {
                    (int) Math.round(top * 0.52),
                    (int) Math.round(top * 0.76),
                    top,
                };
            }
            return this.discs[Math.max(0, Math.min(this.discs.length - 1, level))];
        }

        int[] triRadii() {
            if (this.tri == null) {
                this.tri = SkymapPanel.bloomed(
                    SkymapPanel.bandRadii(SkymapPanel.this.skyRing.lon, SkymapPanel.this.skyRing.valid,
                        this.rings[RING_TRI], this.rings[RING_TRANSIT]),
                    this.rings[RING_TRANSIT], SkymapPanel.this.triOpenFraction());
            }
            return this.tri;
        }
    }

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
            double p = Bloom.stagger(v, i, settled.length, RING_SPREAD);
            out[i] = (int) Math.round(inner + (settled[i] - inner) * p);
        }
        return out;
    }

    /**
     * How opaque a blooming ring is drawn, 0 to 1.
     *
     * Reaches full well before the bloom does, so the reader watches the glyphs travel rather
     * than watching them fade in once they have already arrived.
     */
    private static float ringAlpha(double open) {
        return (float) Bloom.smoothstep(0.0, 0.45, open);
    }

    /** The wheel's geometry at its current size, or null when it has none to speak of. */
    private Geometry geometry() {
        if (this.chartPanel == null) {
            return null;
        }
        return this.geometry(this.chartPanel.getWidth(), this.chartPanel.getHeight());
    }

    /** As above, for the painter, which is handed the size it is painting into. */
    private Geometry geometry(int w, int h) {
        return w <= 0 || h <= 0 ? null : new Geometry(w, h);
    }

    private int bodyAt(int x, int y) {
        if (this.sw == null || this.natalRing.sd == null || this.chartPanel == null) {
            return -1;
        }
        // <b>Whichever view is on screen answers.</b> The flat geometry below describes bands
        // that are not being drawn when the globe is up, so asking it would light a body the
        // reader is not looking at - and on a globe a wrong answer reads as the reader having
        // missed rather than as the chart being wrong.
        if (this.globeMode) {
            return GlobeRenderer.bodyAt(this.globe, this.chartPanel.getWidth(),
                this.chartPanel.getHeight(), this, x, y);
        }
        Geometry g = this.geometry();
        if (g == null) {
            return -1;
        }
        if (this.triRingDrawn()) {
            int i = this.nearestPoint(x, y, g.cx, g.cy, g.pin, this.skyRing.lon, this.skyRing.valid,
                g.triRadii(), true);
            if (i >= 0) {
                return i | TRANSIT_BIT;
            }
        }
        if (this.outerRingDrawn()) {
            int i = this.nearestPoint(x, y, g.cx, g.cy, g.pin, this.outerRing.lon, this.outerRing.valid,
                g.transitRadii(), true);
            if (i >= 0) {
                return i | TRANSIT_BIT;
            }
        }
        return this.nearestPoint(x, y, g.cx, g.cy, g.pin, this.natalRing.lon, this.natalRing.valid,
            g.natalRadii(), false);
    }

    /** Marks a packed hit as belonging to the outer wheel. Above any registry index. */
    static final int TRANSIT_BIT = 1 << 16;

    /**
     * Set alongside {@link #TRANSIT_BIT} when the hit was on the sky ring.
     *
     * <b>One bit could not say which of three rings.</b> Every hit test packed "not the natal
     * ring" and stopped there, so the sky ring and Chart B came back indistinguishable - and
     * the globe's hover then read the outer ring's longitudes for both, which is why David saw
     * Chart B's Sun and the sky's Sun describing the same degree. Added as a second bit rather
     * than a new encoding so that every existing reader of TRANSIT_BIT still answers the
     * question it was asking; the ring is available to whoever needs the finer answer.
     */
    static final int SKY_BIT = 1 << 17;

    /** Packs a body index and the ring it was found on, the way every hit test returns it. */
    static int packHit(int body, int ring) {
        if (ring == WHEEL_NATAL) {
            return body;
        }
        return ring == WHEEL_SKY ? (body | TRANSIT_BIT | SKY_BIT) : (body | TRANSIT_BIT);
    }

    /** The body index out of a packed hit. */
    static int hitBody(int packed) {
        return packed & (TRANSIT_BIT - 1);
    }

    /** Which wheel a packed hit was on - WHEEL_NATAL, WHEEL_OUTER or WHEEL_SKY. */
    static int hitRing(int packed) {
        if ((packed & TRANSIT_BIT) == 0) {
            return WHEEL_NATAL;
        }
        return (packed & SKY_BIT) != 0 ? WHEEL_SKY : WHEEL_OUTER;
    }

    /**
     * The body the cursor is resting on, or -1.
     *
     * <b>Hover, not click.</b> Focus follows the cursor and lets go when it leaves, so a reader
     * sweeping the wheel sees each body's own web in turn without having to select and
     * deselect. Clicking still opens the reading - that is a separate, deliberate act.
     */
    private int focusBody = -1;
    private boolean focusTransit;

    /**
     * True when the focus was set by a click rather than by the cursor resting on a body.
     *
     * <b>Without this the highlight dies on the way to reading it.</b> Focus follows the
     * cursor, so clicking a body and then moving toward the drawer to read its detail cleared
     * the very highlight the detail is about. A click means "keep this"; hover no longer
     * overrides it, and leaving the wheel no longer clears it. Clicking empty space lets go.
     */
    private boolean focusPinned;

    /** True when nothing is focused, so every aspect draws at its ordinary strength. */
    private boolean noFocus() {
        return this.focusBody < 0;
    }

    /**
     * How strongly to draw a line, given what is focused.
     *
     * 1.0 when nothing is focused or the line touches the focused body; otherwise
     * {@link #FOCUS_DIM}. <b>Dimmed rather than hidden</b>: the rest of the chart is the
     * context that makes one body's web mean anything, and removing it would leave a reader
     * looking at five lines in an empty circle.
     */
    private double focusWeight(int a, int b, boolean transitPair) {
        if (this.noFocus()) {
            return 1.0;
        }
        boolean touches = transitPair == this.focusTransit
            ? (a == this.focusBody || b == this.focusBody)
            : (b == this.focusBody);
        return touches ? 1.0 : FOCUS_DIM;
    }

    /**
     * How strongly to draw body {@code i}'s glyph, given what is focused.
     *
     * Bright when it is the focused body itself, or when it makes a visible aspect to it -
     * asked through {@code visibleAspect}, the same gate the lines use, so a glyph cannot stay
     * lit for an aspect that was switched off in Settings.
     */
    private double glyphWeight(int i, boolean transit) {
        if (this.noFocus()) {
            return 1.0;
        }
        if (i == this.focusBody && transit == this.focusTransit) {
            return 1.0;
        }
        double[] mine = transit ? this.outerRing.lon : this.natalRing.lon;
        double[] theirs = this.focusTransit ? this.outerRing.lon : this.natalRing.lon;
        if (this.focusBody >= mine.length || this.focusBody >= theirs.length
                || i >= mine.length) {
            return FOCUS_DIM;
        }
        boolean synastry = transit != this.focusTransit;
        double sep = Aspects.separation(mine[i], theirs[this.focusBody]);
        return this.visibleAspect(sep, i, this.focusBody, synastry) != null ? 1.0 : FOCUS_DIM;
    }

    /** What an unfocused line and glyph fade to. Enough to recede, not enough to vanish. */
    private static final double FOCUS_DIM = 0.16;

    /**
     * Pins the focus to a clicked body, or lets go when the click missed everything.
     *
     * Returns true when the wheel needs repainting.
     */
    private boolean pinFocus(int packed) {
        this.focusPinned = false;
        boolean changed = this.setFocus(packed);
        this.focusPinned = packed >= 0;
        return changed || !this.focusPinned;
    }

    // ---------------------------------------------------------------- hover swell

    /**
     * What a hover swells: a ring target, packed as kind * 1000 + index.
     *
     * <b>David, 2026-09-14: "make everything clickable bulge a little when hovered, to know it's
     * clickable".</b> The wheel answered clicks on bodies, signs, decans, the degree scale,
     * houses and mansions, and gave no sign which of them it would answer before the click - so
     * a ring that did nothing and a ring whose reading went to a hidden tab looked the same. The
     * hovered body now swells by {@link #HOVER_SCALE}; a hovered ring target gets a soft wash and
     * its label drawn larger; and the cursor turns to a hand over anything that opens something.
     *
     * The target is found by {@link #ringTargetAt}, which walks the same bands in the same order
     * as handleChartClick, so what swells is what the click opens.
     */
    static final int HOVER_SIGN = 1;
    static final int HOVER_DECAN = 2;
    static final int HOVER_DEGREE = 3;
    static final int HOVER_HOUSE = 4;
    static final int HOVER_MANSION = 5;
    /** A bound, indexed by the whole degree of longitude under the cursor. */
    static final int HOVER_BOUND = 6;
    /** A degree on the rim when the mansions are folded away. */
    static final int HOVER_RIM_DEGREE = 7;
    static final double HOVER_SCALE = 1.3;

    /** The ring target under the cursor, packed, or -1. */
    int hoverRing = -1;
    /** The body under the cursor, packed as bodyAt packs it, or -1 - independent of the pin. */
    int hoverBody = -1;

    /**
     * "Sidereal (Lahiri)" in the wheel's lower-left corner when a sidereal zodiac is in force.
     *
     * On the wheel itself as well as on the time readout, because a saved or printed chart
     * image carries the wheel and not the drawer handle - and a sidereal chart with no label is
     * a chart that will be read as a tropical one with its planets in the wrong signs.
     */
    static void paintZodiacTag(Graphics2D g2, int w, int h) {
        if (!com.zodiacomputing.ourania.astro.Ephemeris.sidereal()) {
            return;
        }
        String tag = com.zodiacomputing.ourania.astro.Ephemeris.zodiacLabel();
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(new Color(226, 178, 88));
        g2.drawString(tag, 12, h - 12);
    }

    /** Sets the drawing transform to base, swollen about (x, y) when on. */
    static void bulge(Graphics2D g2, java.awt.geom.AffineTransform base, int x, int y, boolean on) {
        g2.setTransform(base);
        if (on) {
            g2.translate(x, y);
            g2.scale(HOVER_SCALE, HOVER_SCALE);
            g2.translate(-x, -y);
        }
    }

    // ------------------------------------------------------------------ zoom and pan

    /**
     * Master list E6: "Zoom &amp; pan - wheel is fixed to the panel; no zoom, pan, or
     * fit-to-window."
     *
     * <b>One view transform, applied at the two ends and nowhere in between.</b> The painter
     * draws the wheel through it, and every mouse position is carried back through its inverse
     * before any hit test sees it. Geometry, bodyAt, chordAt, ringTargetAt and
     * handleChartClick are untouched and still work in the wheel's own unzoomed coordinates -
     * the alternative, a zoom factor threaded into the radius chain, is the four-copies defect
     * Geometry exists to end, and a hit test that missed the factor would put clicks beside
     * what is drawn.
     *
     * <b>Screen only.</b> A saved PNG or a print is the whole chart whatever the screen is
     * zoomed to: ChartExporter marks the panel while it paints, and the transform is skipped.
     *
     * Fit is zoom 1 with no pan: the wheel already sizes itself to the panel.
     */
    double viewZoom = 1.0;
    /** The view's offset in screen pixels, applied after the zoom. */
    double viewPanX;
    double viewPanY;
    static final double VIEW_MAX_ZOOM = 8.0;
    /** Each wheel notch zooms by this much, so four notches is about double. */
    static final double VIEW_STEP = 1.19;
    /** Set while a drag has been panning, so its closing click does not also select. */
    private boolean viewPanned;

    /** Screen from wheel: scale about the panel's centre, then shift by the pan. */
    java.awt.geom.AffineTransform viewTransform(int w, int h) {
        java.awt.geom.AffineTransform t = new java.awt.geom.AffineTransform();
        t.translate(w / 2.0 + this.viewPanX, h / 2.0 + this.viewPanY);
        t.scale(this.viewZoom, this.viewZoom);
        t.translate(-w / 2.0, -h / 2.0);
        return t;
    }

    boolean viewIsFit() {
        return this.viewZoom == 1.0 && this.viewPanX == 0.0 && this.viewPanY == 0.0;
    }

    /** A screen point in the wheel's own coordinates - what every hit test is asked about. */
    java.awt.Point toWheel(int x, int y) {
        if (this.viewIsFit() || this.chartPanel == null) {
            return new java.awt.Point(x, y);
        }
        double w = this.chartPanel.getWidth();
        double h = this.chartPanel.getHeight();
        double wx = (x - w / 2.0 - this.viewPanX) / this.viewZoom + w / 2.0;
        double wy = (y - h / 2.0 - this.viewPanY) / this.viewZoom + h / 2.0;
        return new java.awt.Point((int) Math.round(wx), (int) Math.round(wy));
    }

    /**
     * Zooms by a number of wheel notches, keeping the point under the cursor where it is.
     *
     * <b>About the cursor, not the centre.</b> Zooming about the centre sends whatever the
     * reader was pointing at off towards the edge, so every zoom would need a pan after it.
     */
    void zoomAt(int x, int y, double notches) {
        if (this.chartPanel == null) {
            return;
        }
        double w = this.chartPanel.getWidth();
        double h = this.chartPanel.getHeight();
        double z = Math.max(1.0, Math.min(VIEW_MAX_ZOOM, this.viewZoom * Math.pow(VIEW_STEP, -notches)));
        // The wheel point under the cursor, before the change, in the wheel's coordinates.
        double wx = (x - w / 2.0 - this.viewPanX) / this.viewZoom + w / 2.0;
        double wy = (y - h / 2.0 - this.viewPanY) / this.viewZoom + h / 2.0;
        this.viewZoom = z;
        this.viewPanX = x - w / 2.0 - z * (wx - w / 2.0);
        this.viewPanY = y - h / 2.0 - z * (wy - h / 2.0);
        this.clampView();
    }

    /** Moves the view by a screen distance. */
    void panBy(double dx, double dy) {
        this.viewPanX += dx;
        this.viewPanY += dy;
        this.clampView();
    }

    /** Back to the whole wheel, fitted to the panel. */
    void fitView() {
        this.viewZoom = 1.0;
        this.viewPanX = 0.0;
        this.viewPanY = 0.0;
    }

    /**
     * Keeps the zoomed wheel covering the panel.
     *
     * The scaled canvas is viewZoom times the panel, so it can slide (viewZoom - 1) half-panels
     * each way before an edge comes into view. At zoom 1 that is nothing: fit cannot be panned
     * off centre, and zooming all the way out always lands back on it.
     */
    private void clampView() {
        if (this.chartPanel == null) {
            return;
        }
        double limX = (this.viewZoom - 1.0) * this.chartPanel.getWidth() / 2.0;
        double limY = (this.viewZoom - 1.0) * this.chartPanel.getHeight() / 2.0;
        this.viewPanX = Math.max(-limX, Math.min(limX, this.viewPanX));
        this.viewPanY = Math.max(-limY, Math.min(limY, this.viewPanY));
        if (this.viewZoom <= 1.0) {
            this.viewZoom = 1.0;
            this.viewPanX = 0.0;
            this.viewPanY = 0.0;
        }
    }

    /** Where the "Fit" chip sits while zoomed: the top-left corner, clear of the wheel's rim. */
    static java.awt.Rectangle fitChipBounds() {
        return new java.awt.Rectangle(10, 10, 104, 24);
    }

    /** The chip that says how far in the view is and puts it back, drawn in screen space. */
    void paintFitChip(Graphics2D g2) {
        if (this.viewIsFit()) {
            return;
        }
        java.awt.Rectangle r = fitChipBounds();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(22, 28, 43, 225));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setColor(new Color(56, 132, 190));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setFont(Theme.font("Segoe UI", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        String label = Math.round(this.viewZoom * 100) + "%  ·  Fit";
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, r.x + (r.width - fm.stringWidth(label)) / 2,
            r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
    }

    static final int GLOBE_NONE = 0;
    static final int GLOBE_HOUSE = 2;
    static final int GLOBE_DECAN = 3;
    static final int GLOBE_DEGREE = 4;
    static final int GLOBE_MANSION = 5;
    static final int GLOBE_SIGN = 6;
    static final int GLOBE_BOUND = 7;

    /**
     * What a point on the globe opens, other than a body: {kind, value}, kind GLOBE_NONE when
     * nothing. The one order for the globe's click and its hover, so what lights is what opens.
     *
     * House numbers first (a label the reader aimed at), then the decan band before the degree
     * ticks that crowd it, the mansions, the signs, the bounds, and last the house wedges of the
     * plane, which lie under everything else.
     */
    int[] globeTargetAt(int x, int y) {
        int w = this.chartPanel.getWidth();
        int h = this.chartPanel.getHeight();
        int v = GlobeRenderer.houseNumberAt(this.globe, w, h, this, x, y);
        if (v >= 1) {
            return new int[]{GLOBE_HOUSE, v};
        }
        v = GlobeRenderer.decanAt(this.globe, w, h, this, x, y);
        if (v >= 0) {
            return new int[]{GLOBE_DECAN, v};
        }
        v = GlobeRenderer.degreeAt(this.globe, w, h, this, x, y);
        if (v >= 0) {
            return new int[]{GLOBE_DEGREE, v};
        }
        v = GlobeRenderer.mansionAt(this.globe, w, h, this, x, y);
        if (v >= 1) {
            return new int[]{GLOBE_MANSION, v};
        }
        v = GlobeRenderer.signAt(this.globe, w, h, this, x, y);
        if (v >= 0) {
            return new int[]{GLOBE_SIGN, v};
        }
        v = GlobeRenderer.boundAt(this.globe, w, h, this, x, y);
        if (v >= 0) {
            return new int[]{GLOBE_BOUND, v};
        }
        v = GlobeRenderer.houseAreaAt(this.globe, w, h, this, x, y);
        if (v >= 1) {
            return new int[]{GLOBE_HOUSE, v};
        }
        return new int[]{GLOBE_NONE, -1};
    }

    /** Records what is under the cursor; true when that changed and the wheel should repaint. */
    boolean setHover(int body, int ring) {
        if (body == this.hoverBody && ring == this.hoverRing) {
            return false;
        }
        this.hoverBody = body;
        this.hoverRing = ring;
        return true;
    }

    /**
     * The ring target under a point on the flat wheel, packed, or -1 - for a point that is not
     * on a body or an aspect line, which the caller has already asked about.
     *
     * The bands in handleChartClick's order: the house field inside the bodies, then the decan,
     * sign and degree bands by bandAt, then the mansion strip, then the Sabian strip.
     */
    int ringTargetAt(int x, int y) {
        if (this.sw == null || this.natalRing.sd == null) {
            return -1;
        }
        Geometry g = this.geometry();
        if (g == null) {
            return -1;
        }
        double dx = x - g.cx;
        double dy = y - g.cy;
        double r = Math.hypot(dx, dy);
        double screen = Math.toDegrees(Math.atan2(dy, dx));
        if (screen < 0.0) {
            screen += 360.0;
        }
        double lon = (180.0 + g.pin - screen) % 360.0;
        if (lon < 0.0) {
            lon += 360.0;
        }
        if (r < g.bodyBase) {
            double[] cusps = this.activeCusps;
            for (int h = 1; h <= 12; h++) {
                double from = cusps[h];
                double to = h == 12 ? cusps[1] : cusps[h + 1];
                double at = lon;
                if (to < from) {
                    to += 360.0;
                }
                if (at < from && to > 360.0) {
                    at += 360.0;
                }
                if (at >= from && at < to) {
                    return HOVER_HOUSE * 1000 + h;
                }
            }
            return -1;
        }
        switch (SkymapPanel.bandAt(r, g.rings, g.bodyBase)) {
            case BAND_DECAN:
                return HOVER_DECAN * 1000 + (int) (lon / 10.0);
            case BAND_SIGN:
            case BAND_OPEN:
                return HOVER_SIGN * 1000 + (int) (lon / 30.0);
            case BAND_DEGREE:
                return HOVER_DEGREE * 1000 + ((int) Math.round(lon) % 360);
            case BAND_BOUND:
                return HOVER_BOUND * 1000 + (int) Math.floor(lon);
            default:
                break;
        }
        if (this.layerShown(Layer.MANSIONS) && SkymapPanel.inMansionRing(r, g.rings)) {
            return HOVER_MANSION * 1000
                + com.zodiacomputing.ourania.astro.LunarMansions.at(lon).number;
        }
        if (this.layerShown(Layer.DEGREES) && SkymapPanel.inRimDegreeBand(r, g.rings)) {
            return HOVER_RIM_DEGREE * 1000 + ((int) Math.round(lon) % 360);
        }
        // The rim with both folded away: nothing is drawn there, so nothing answers.
        if (SkymapPanel.inMansionBand(r, g.rings[RING_OUTER])) {
            return this.layerShown(Layer.MANSIONS)
                ? HOVER_MANSION * 1000
                    + com.zodiacomputing.ourania.astro.LunarMansions.at(lon).number
                : HOVER_RIM_DEGREE * 1000 + ((int) Math.round(lon) % 360);
        }
        return -1;
    }

    /** An annular wedge between two radii and two longitudes, in screen coordinates. */
    private static java.awt.Shape wedge(Geometry g, double r0, double r1, double lonFrom,
                                        double lonTo) {
        java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
        int steps = Math.max(2, (int) Math.ceil(Math.abs(lonTo - lonFrom) / 2.0));
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(180.0 + g.pin - (lonFrom + (lonTo - lonFrom) * i / steps));
            double px = g.cx + r1 * Math.cos(a);
            double py = g.cy + r1 * Math.sin(a);
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        for (int i = steps; i >= 0; i--) {
            double a = Math.toRadians(180.0 + g.pin - (lonFrom + (lonTo - lonFrom) * i / steps));
            p.lineTo(g.cx + r0 * Math.cos(a), g.cy + r0 * Math.sin(a));
        }
        p.closePath();
        return p;
    }

    /** Draws text centred on a point at a longitude and radius. */
    private static void centred(Graphics2D g2, Geometry g, String text, double lon, double r) {
        double a = Math.toRadians(180.0 + g.pin - lon);
        int x = g.cx + (int) Math.round(r * Math.cos(a));
        int y = g.cy + (int) Math.round(r * Math.sin(a));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x - fm.stringWidth(text) / 2, y + fm.getAscent() / 2 - 2);
    }

    /** The swell for a hovered ring target, painted over the finished wheel. */
    void paintHover(Graphics2D g2, Geometry g) {
        if (this.hoverRing < 0 || g == null) {
            return;
        }
        int kind = this.hoverRing / 1000;
        int idx = this.hoverRing % 1000;
        int[] rings = g.rings;
        java.awt.Composite was = g2.getComposite();
        Color wash = new Color(255, 255, 255, 38);
        switch (kind) {
            case HOVER_SIGN: {
                g2.setColor(wash);
                g2.fill(wedge(g, rings[RING_SIGN_INNER], rings[RING_SIGN_OUTER], idx * 30.0,
                    idx * 30.0 + 30.0));
                g2.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.round(22 * HOVER_SCALE)));
                g2.setColor(this.getElementColor(Zodiac.elementIndex(idx)));
                centred(g2, g, ZODIAC_SYMBOLS[idx], idx * 30.0 + 15.0, rings[RING_SIGN_OUTER] - 18);
                break;
            }
            case HOVER_DECAN: {
                g2.setColor(wash);
                g2.fill(wedge(g, rings[RING_SIGN_OUTER], rings[RING_DECAN_OUTER], idx * 10.0,
                    idx * 10.0 + 10.0));
                g2.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.round(12 * HOVER_SCALE * 1.2)));
                int sign = idx / 3;
                String glyph = null;
                if (Settings.DECAN_RING_CHALDEAN.equals(Settings.decanRing())) {
                    int fi = Bodies.indexOfName(Zodiac.chaldeanDecanRuler(Zodiac.SIGNS[sign], idx % 3 + 1));
                    if (fi >= 0 && fi < BODY_GLYPHS.length) {
                        g2.setColor(this.bodyColor(fi));
                        glyph = BODY_GLYPHS[fi];
                    }
                }
                if (glyph == null) {
                    int face = Zodiac.triplicityDecanSignIndex(sign, idx % 3 + 1);
                    g2.setColor(this.getElementColor(Zodiac.elementIndex(face)));
                    glyph = ZODIAC_SYMBOLS[face];
                }
                centred(g2, g, glyph, idx * 10.0 + 5.0, rings[RING_DECAN_OUTER] - 10);
                break;
            }
            case HOVER_DEGREE: {
                g2.setColor(wash);
                g2.fill(wedge(g, rings[RING_DEGREE_INNER], rings[RING_TERM_INNER] + 2, idx - 0.5,
                    idx + 0.5));
                double a = Math.toRadians(180.0 + g.pin - idx);
                g2.setColor(new Color(255, 255, 255, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(g.cx + (int) (rings[RING_DEGREE_INNER] * Math.cos(a)),
                    g.cy + (int) (rings[RING_DEGREE_INNER] * Math.sin(a)),
                    g.cx + (int) ((rings[RING_TERM_INNER] + 4) * Math.cos(a)),
                    g.cy + (int) ((rings[RING_TERM_INNER] + 4) * Math.sin(a)));
                break;
            }
            case HOVER_HOUSE: {
                double from = this.activeCusps[idx];
                double to = idx == 12 ? this.activeCusps[1] : this.activeCusps[idx + 1];
                if (to < from) {
                    to += 360.0;
                }
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fill(wedge(g, 0, g.natalFloor, from, to));
                g2.setFont(Theme.font("Arial", Font.BOLD, (int) Math.round(15 * HOVER_SCALE)));
                g2.setColor(this.getElementColor(Zodiac.elementIndex(idx - 1)));
                centred(g2, g, String.valueOf(idx), from + (to - from) / 2.0, g.natalFloor - 16);
                break;
            }
            // <b>Each swell covers its own band, not the whole rim.</b> Both used to wash the
            // full 20 pixels, which was right while the rim was one target and is wrong now
            // that the mansions and the scale are drawn apart: a swell that reaches over the
            // ring next door says the reader is pointing at both.
            case HOVER_MANSION: {
                double start = (idx - 1) * com.zodiacomputing.ourania.astro.LunarMansions.WIDTH;
                int floor = rings[RING_MANSION_INNER] < rings[RING_OUTER]
                    ? rings[RING_MANSION_INNER] : rings[RING_OUTER] - RIM_BAND_DEPTH;
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fill(wedge(g, floor, rings[RING_OUTER] + 4,
                    start, start + com.zodiacomputing.ourania.astro.LunarMansions.WIDTH));
                break;
            }
            case HOVER_RIM_DEGREE: {
                int ceiling = rings[RING_MANSION_INNER] < rings[RING_OUTER]
                    ? rings[RING_MANSION_INNER] : rings[RING_OUTER] + 4;
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fill(wedge(g, rings[RING_OUTER] - RIM_BAND_DEPTH, ceiling,
                    idx - 0.5, idx + 0.5));
                break;
            }
            case HOVER_BOUND: {
                int sign = idx / 30;
                double[] edges = com.zodiacomputing.ourania.astro.Dignity.boundEdges(sign);
                double inSign = idx % 30 + 0.5;
                for (int e = 0; e < edges.length - 1; e++) {
                    if (inSign >= edges[e] && inSign < edges[e + 1]) {
                        double from = sign * 30.0 + edges[e];
                        double to = sign * 30.0 + edges[e + 1];
                        g2.setColor(wash);
                        g2.fill(wedge(g, rings[RING_TERM_INNER], rings[RING_SIGN_INNER], from, to));
                        int bi = Bodies.indexOfName(
                            com.zodiacomputing.ourania.astro.Dignity.boundRulerOf((from + to) / 2.0));
                        if (bi >= 0 && bi < BODY_GLYPHS.length) {
                            g2.setFont(new Font("SansSerif", Font.PLAIN,
                                (int) Math.round(11 * HOVER_SCALE * 1.2)));
                            g2.setColor(this.bodyColor(bi));
                            centred(g2, g, BODY_GLYPHS[bi], (from + to) / 2.0,
                                (rings[RING_TERM_INNER] + rings[RING_SIGN_INNER]) / 2.0);
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
        g2.setComposite(was);
    }

    /** Sets the focused body and reports whether anything changed, so hover repaints once. */
    private boolean setFocus(int packed) {
        if (this.focusPinned) {
            return false;
        }
        int body = packed < 0 ? -1 : (packed & (TRANSIT_BIT - 1));
        boolean transit = packed >= 0 && (packed & TRANSIT_BIT) != 0;
        if (body == this.focusBody && transit == this.focusTransit) {
            return false;
        }
        this.focusBody = body;
        this.focusTransit = transit;
        return true;
    }

    /**
     * The hover card: what it is, where it is, and the degree symbolism that goes with it.
     *
     * Swing tooltips take a subset of HTML, so this stays to a table-free stack of divs -
     * anything cleverer renders as literal markup, which is the same defect the Snapshot
     * panel was carrying until it was fixed.
     */
    /**
     * The body the selection card on screen is describing, so the card can redraw itself.
     *
     * <b>The card used to redraw from the hover focus, which is not the same thing.</b> The
     * focus follows the cursor; the card stays where it was put. Every expander on it was a
     * round trip through the focus, so a card whose body was no longer under the mouse either
     * redrew as a different body or, with the focus on nothing, did not redraw at all - which
     * is what a reader sees as a heading that will not open. Recorded where the card is built,
     * so any path that opens one gets working expanders rather than only the paths that
     * remembered to pin.
     */
    private int selectionBody = -1;
    private double selectionLon;
    private double selectionSpeed;
    private boolean selectionTransit;

    /** Which sections of the selection card the reader has opened. Survives re-selection. */
    private final java.util.Set<String> selectionOpen = new java.util.LinkedHashSet<>();

    /** The href prefix a selection card's expander links carry. */
    static final String SELECT_EXPAND = "selexp|";

    /**
     * The selection card: the hover summary, plus sections that open in place.
     *
     * <b>The card told you where a body is and stopped.</b> Reading what the placement meant
     * took four more steps - Interpretations, the index, Planets, the body you had already
     * clicked - which is a long way to travel for text the app already has in hand.
     *
     * Swing's HTML renderer has no scripting and no details element, so the sections toggle
     * by round trip: each heading is a link, the click records the section and rebuilds this
     * card. That is why the open set lives on the panel rather than in the markup.
     *
     * The tooltip deliberately does not use this. A card that cannot be clicked has no use
     * for expanders, and hoverHtml stays the one description of a body that both share.
     */
    String selectionHtml(int n, double lon, double speed, boolean transit) {
        this.selectionBody = n;
        this.selectionLon = lon;
        this.selectionSpeed = speed;
        this.selectionTransit = transit;
        StringBuilder sb = new StringBuilder(this.hoverHtml(n, lon, speed,
            transit ? WHEEL_OUTER : WHEEL_NATAL));
        int close = sb.lastIndexOf("</body>");
        if (close < 0) {
            close = sb.length();
        }

        int signIdx = Zodiac.signIndex(lon);
        int degree = (int) (lon % 30.0) + 1;
        int decan = Zodiac.decan(lon);
        int house = Zodiac.houseOf(lon, transit ? this.outerRing.cusps : this.activeCusps);
        String reading = this.window == null ? "" : this.window.planetReadingHtml(
            BODY_NAMES[n], SIGN_NAMES[signIdx], degree, decan, house,
            this.getActiveAspectsFor(n, transit), lon);

        StringBuilder extra = new StringBuilder();
        String lead = leadParagraph(reading);
        if (!lead.isEmpty()) {
            extra.append(section("core", "What it is", lead));
        }
        int i = 0;
        java.util.List<String[]> parts = readingSections(reading);
        parts = fold(parts, "This degree", true, "sabian", "degree interpretation");
        parts = fold(parts, "Correspondences", false, "tarot", "lunar mansion");
        parts = moveAfterFirst(parts, "in house");
        for (String[] part : parts) {
            extra.append(section("s" + i++, part[0], part[1]));
        }
        if (i == 0 && lead.isEmpty()) {
            extra.append("<div style='margin-top:6px; color:#9FB4C7;'><i>No reading recorded "
                + "for this point.</i></div>");
        }
        sb.insert(close, extra.toString());
        return sb.toString();
    }

    /**
     * Lifts a section to sit immediately after the first one.
     *
     * The reading writes the house well down the page, after the degree and the tarot, which
     * suits a page read top to bottom. In a column of collapsed headings the sign and the
     * house are the two facts a reader checks together - where the body is, twice - and
     * putting the symbolic material between them made the card read as if the house were an
     * afterthought.
     *
     * Applied after the folds, so it moves a finished section rather than one that a later
     * merge would move again. A no-op when nothing matches, which is what angles do.
     */
    private static java.util.List<String[]> moveAfterFirst(java.util.List<String[]> in,
                                                           String keyword) {
        if (in.size() < 3) {
            return in;
        }
        int found = -1;
        for (int i = 1; i < in.size(); i++) {
            if (in.get(i)[0].toLowerCase().contains(keyword)) {
                found = i;
                break;
            }
        }
        if (found <= 1) {
            return in;          // absent, or already where it belongs
        }
        java.util.List<String[]> out = new java.util.ArrayList<String[]>(in);
        out.add(1, out.remove(found));
        return out;
    }

    /**
     * Merges the sections whose headings match any keyword into one, at the first one's place.
     *
     * <b>Written once because there are two of these and there will be a third.</b> The first
     * fold was Sabian plus degree; grouping tarot with the lunar mansion the same afternoon
     * would have made it two near-identical methods, which is the defect this project logs
     * more than any other.
     *
     * Merges across gaps rather than swallowing a range. The reading puts the lunar mansion
     * between the Sabian and the degree, so a range grab would take the mansion with them -
     * and the mansion is a different division of the zodiac, not a reading of that degree.
     * Matching on heading text also means a fold survives the reading reordering its sections
     * or relabelling a degree.
     *
     * Each merged part keeps its own heading inside as a bold line, so a fold costs a door
     * and never a distinction.
     *
     * @param carryLabel take the bracketed part of the first match into the new heading, so
     *                   "Sabian Symbol (Libra 26)" yields "This degree (Libra 26)".
     */
    private static java.util.List<String[]> fold(java.util.List<String[]> in, String title,
                                                 boolean carryLabel, String... keywords) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        StringBuilder merged = new StringBuilder();
        String suffix = "";
        int at = -1;
        for (String[] part : in) {
            String lower = part[0].toLowerCase();
            boolean hit = false;
            for (String k : keywords) {
                if (lower.contains(k)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                out.add(part);
                continue;
            }
            if (at < 0) {
                at = out.size();
                out.add(null);          // held, and filled once every match is in
                int open = carryLabel ? part[0].indexOf('(') : -1;
                int shut = open < 0 ? -1 : part[0].indexOf(')', open + 1);
                if (shut > open) {
                    suffix = " " + part[0].substring(open, shut + 1);
                }
            }
            merged.append("<div style='margin-top:4px; color:#9FB4C7;'><b>")
                  .append(part[0]).append("</b></div>").append(part[1]);
        }
        if (at >= 0) {
            out.set(at, new String[] {title + suffix, merged.toString()});
        }
        return out;
    }

    /**
     * The reading's sections, in the order it wrote them.
     *
     * Cut on the h2 and h3 headings the reading emits rather than rebuilt from the service,
     * so the card carries whatever the reading carries - if a section is added there it
     * appears here, and neither can quietly fall behind the other. Index-keyed, so the open
     * set survives moving between bodies with the same shape of reading.
     */
    private static java.util.List<String[]> readingSections(String html) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        if (html == null || html.isEmpty()) {
            return out;
        }
        int i = 0;
        while (i < html.length()) {
            int h = nextHeading(html, i);
            if (h < 0) {
                break;
            }
            String tag = html.startsWith("<h2", h) ? "h2" : "h3";
            int open = html.indexOf('>', h);
            int shut = open < 0 ? -1 : html.indexOf("</" + tag + ">", open);
            if (open < 0 || shut < 0) {
                break;
            }
            String title = stripTags(html.substring(open + 1, shut)).trim();
            int from = shut + tag.length() + 3;
            // <b>Aspects group under their own heading.</b> The reading gives each contact an
            // h3 of its own, which is right on a full page and twenty collapsed headings in a
            // 250px column. Everything from the Active Aspects banner to the next h2 - which
            // is the end of the reading - becomes one section, and the per-aspect headings
            // stay inside it as the sub-headings they already are.
            boolean groups = title.toLowerCase().contains("aspect");
            int next = groups ? nextH2(html, from) : nextHeading(html, from);
            int to = next < 0 ? html.length() : next;
            String body = html.substring(from, Math.max(from, to));
            int tail = body.indexOf("</body>");
            if (tail >= 0) {
                body = body.substring(0, tail);
            }
            if (!title.isEmpty()) {
                out.add(new String[] {title, body});
            }
            i = to;
        }
        return out;
    }

    /** Whatever the reading says before its first heading - the body's core paragraph. */
    private static String leadParagraph(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        int h = nextHeading(html, 0);
        String lead = h < 0 ? html : html.substring(0, h);
        int b = lead.indexOf("<body");
        if (b >= 0) {
            int gt = lead.indexOf('>', b);
            if (gt >= 0) {
                lead = lead.substring(gt + 1);
            }
        }
        return stripTags(lead).trim().isEmpty() ? "" : lead;
    }

    /** The next h2 only, so a grouped section can swallow the h3s underneath it. */
    private static int nextH2(String html, int from) {
        return html.indexOf("<h2", from);
    }

    private static int nextHeading(String html, int from) {
        int a = html.indexOf("<h2", from);
        int b = html.indexOf("<h3", from);
        if (a < 0) {
            return b;
        }
        return b < 0 ? a : Math.min(a, b);
    }

    /** Tags out, text kept - used only to decide whether a fragment says anything. */
    private static String stripTags(String html) {
        StringBuilder sb = new StringBuilder();
        boolean in = false;
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '<') {
                in = true;
            } else if (c == '>') {
                in = false;
            } else if (!in) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** One expander: a heading that is always a link, and a body only when it is open. */
    private String section(String key, String title, String body) {
        boolean open = this.selectionOpen.contains(key);
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-top:6px; border-top:1px solid #2A3244; padding-top:4px;'>")
          .append("<a href='").append(SELECT_EXPAND).append(key)
          .append("' style='color:#7FB3FF; text-decoration:none; font-size:11px;'>")
          .append(open ? "&#9662; " : "&#9656; ").append(title).append("</a>");
        if (open) {
            sb.append("<div style='margin-top:3px; color:#cfd6e4;'>")
              .append(body == null || body.isEmpty() ? "<i>Nothing recorded.</i>" : body)
              .append("</div>");
        }
        return sb.append("</div>").toString();
    }

    /**
     * Opens or closes one section of the card and redraws it in place.
     *
     * Redrawn from the body the card is describing rather than from anything the link carries,
     * so the card cannot end up describing one body with another's sections open - and rather
     * than from the hover focus, which is where it used to come from and is a different fact:
     * the focus follows the cursor, and the cursor has to leave the wheel to reach the card.
     */
    public void toggleSelectionSection(String key) {
        if (key == null || this.window == null) {
            return;
        }
        if (!this.selectionOpen.remove(key)) {
            this.selectionOpen.add(key);
        }
        if (this.selectionBody < 0) {
            return;
        }
        this.window.showSelection(this.selectionHtml(this.selectionBody, this.selectionLon,
            this.selectionSpeed, this.selectionTransit));
    }

    private String hoverHtml(int n, double lon, double speed, int ring) {
        final boolean transit = ring != WHEEL_NATAL;
        Bodies.Def def = Bodies.at(n);
        int signIdx = Zodiac.signIndex(lon);
        String sign = Zodiac.SIGNS[signIdx];
        int degInSign = (int)(lon % 30.0);
        int minInSign = (int)((lon % 30.0 - (double)degInSign) * 60.0);
        int decan = Zodiac.decan(lon);

        StringBuilder sb = new StringBuilder();
        // <b>Its own background and its own ink, because this card lands in two places.</b>
        // It is a tooltip over the wheel and it is also the top of the Selection drawer -
        // selectionHtml builds on it - and it set neither colour, so it inherited whatever it
        // landed on. Over a pale tooltip the default black read fine; in the dark drawer the
        // point's name was black on near-black, which is what David saw. A fragment reused on
        // two surfaces has to carry its own contrast rather than borrow one.
        sb.append("<html><body style='width:250px; font-family:SansSerif; font-size:11px;"
            + " background:#12151A; color:#E0E0E0;'>");
        sb.append("<div style='font-size:13px;'><b>").append(def.name).append("</b>");
        if (transit) {
            sb.append(" <span style='color:#5A7FBF;'>(")
                  .append(this.ringWord(ring)).append(")</span>");
        }
        if (!def.isAngle() && speed < 0.0) {
            sb.append(" <span style='color:#B03030;'><b>R</b></span>");
        }
        sb.append("</div>");

        sb.append("<div><b>").append(degInSign).append("&deg;")
          .append(minInSign < 10 ? "0" : "").append(minInSign).append("'</b> ")
          .append(SkymapPanel.capitalise(sign));
        int house = Zodiac.houseOf(lon, transit ? this.outerRing.cusps : this.activeCusps);
        if (house > 0) {
            sb.append(" &nbsp;&middot;&nbsp; House ").append(house);
        }
        sb.append("</div>");

        // <b>Both decan schemes, each named.</b> This line used to read "sub-ruler Mercury"
        // without saying which of the two systems that was, which is the one thing a reader
        // cannot afford not to know here: they disagree for 30 of the 36 decans, and the app
        // uses both at once. The triplicity ruler is the one the decan prose is written to;
        // the Chaldean face is the one the Golden Dawn tarot cards and the Sabian decan_ruler
        // field encode. Naming them is what lets a practitioner reconcile the two surfaces
        // instead of reading the difference as a fault. Zodiac's header carries the full note.
        // <b>The degree's own condition, when it has one.</b> Open in the work plan since
        // 21 Aug and settled 2026-09-03: anaretic is exactly 29d00'00" to 29d59'59" with no
        // tolerance either side, and 0d is the opposite condition rather than the same one.
        // See DECISIONS.md, K3.
        Zodiac.DegreeStatus status = Zodiac.degreeStatus(lon);
        if (status == Zodiac.DegreeStatus.ANARETIC) {
            sb.append("<div style='color:#E8B24A;'><b>Anaretic</b> &middot; the 30th degree, ")
              .append("completing this sign</div>");
        } else if (status == Zodiac.DegreeStatus.INITIATION) {
            sb.append("<div style='color:#7FB3FF;'><b>First degree</b> &middot; the sign just ")
              .append("begun</div>");
        }
        String triplicity = Zodiac.triplicityDecanRuler(lon);
        String face = Zodiac.chaldeanDecanRuler(lon);
        int decanFrom = (decan - 1) * 10;
        sb.append("<div style='color:#9FB4C7;'>Decan ").append(decan)
          .append(" &middot; ").append(decanFrom).append("&deg;&ndash;").append(decanFrom + 10)
          .append("&deg;</div>");
        if ((triplicity != null && !triplicity.isEmpty()) || (face != null && !face.isEmpty())) {
            sb.append("<div style='color:#9FB4C7; font-size:10px;'>");
            if (triplicity != null && !triplicity.isEmpty()) {
                sb.append("Triplicity <b>").append(triplicity).append("</b>");
            }
            if (face != null && !face.isEmpty()) {
                if (triplicity != null && !triplicity.isEmpty()) {
                    sb.append(" &nbsp;&middot;&nbsp; ");
                }
                sb.append("Chaldean face <b>").append(face).append("</b>");
            }
            sb.append("</div>");
        }

        try {
            String sabian = InterpretationService.getInstance()
                .getSabianSymbol(SkymapPanel.capitalise(sign), degInSign + 1);
            if (sabian != null && !sabian.isEmpty() && !sabian.startsWith("Interpretation not found")) {
                sb.append("<div style='margin-top:4px; color:#C9BFA0; font-style:italic;'>")
                  .append(degInSign + 1).append("&deg; ").append(SkymapPanel.capitalise(sign))
                  .append(": &ldquo;").append(sabian).append("&rdquo;</div>");
            }
        } catch (Exception e) {
            // A tooltip is not worth throwing out of a mouse-moved handler for.
        }
        if (!def.meaning.isEmpty()) {
            sb.append("<div style='margin-top:4px; color:#8FA98F;'>").append(def.meaning).append("</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
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

    /** The Sun and the Moon. */
    private static final int TIER_LIGHT = 0;
    /** Mercury, Venus, Mars - the rest of Group.LUMINARIES. */
    private static final int TIER_INNER = 1;
    /** Jupiter through Pluto. */
    private static final int TIER_OUTER = 2;
    /** Asteroids, centaurs, nodes and calculated points. */
    private static final int TIER_SMALL = 3;

    private static final class GlyphSize {
        final int radius;
        final Font font;
        /** Distance below centre to sit the glyph baseline so it looks centred. */
        final int baseline;

        GlyphSize(int radius, int fontPoints) {
            this.radius = radius;
            this.font = new Font("SansSerif", 0, fontPoints);
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
    private static final GlyphSize[] NATAL_SIZES = {
        new GlyphSize(14, 24),      // lights
        new GlyphSize(13, 22),      // inner planets
        new GlyphSize(12, 21),      // outer planets
        new GlyphSize(11, 20)       // asteroids and points
    };

    private static final GlyphSize[] TRANSIT_SIZES = {
        new GlyphSize(12, 20),
        new GlyphSize(11, 19),
        new GlyphSize(11, 18),
        new GlyphSize(10, 17)
    };

    /**
     * Size tier for a registry point, or -1 for the four angles, which are drawn as
     * labelled cubes at a fixed size and are not competing with the bodies for attention.
     */
    private static int tierOf(int n) {
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

    private static GlyphSize natalSize(int n) {
        int n2 = SkymapPanel.tierOf(n);
        return NATAL_SIZES[n2 < 0 ? TIER_INNER : n2];
    }

    private static GlyphSize transitSize(int n) {
        int n2 = SkymapPanel.tierOf(n);
        return TRANSIT_SIZES[n2 < 0 ? TIER_INNER : n2];
    }

    /** Outermost ring: Sun through Mars - the fast, personal bodies. */
    private static final int RING_INNER_PLANETS = 0;
    /** Middle ring: Jupiter through Pluto - the slow, generational ones. */
    private static final int RING_OUTER_PLANETS = 1;
    /** Innermost ring: asteroids, centaurs, nodes and the calculated points. */
    private static final int RING_ASTEROIDS = 2;
    private static final int RING_COUNT = 3;

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
        int ideal = (RING_COUNT - 1) * (int) TRANSIT_RING_GAP + 2 * BAND_EDGE;
        return Math.max(MIN_BAND_DEPTH, Math.min(ideal, outer / 6));
    }

    /**
     * Clearance and sub-ring spacing for the natal band.
     *
     * Wider than the outer rings' on both counts, because the natal wheel carries the largest
     * glyphs and is the thing being read - the outer rings are context around it.
     */
    static final int NATAL_EDGE = 15;
    private static final int NATAL_SUB_RING_GAP = 24;
    private static final double NATAL_SPACING = 32.0;

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
    static int natalBandDepth(int natalTop) {
        int ideal = (RING_COUNT - 1) * NATAL_SUB_RING_GAP + 2 * NATAL_EDGE;
        return Math.max(MIN_BAND_DEPTH, Math.min(ideal, natalTop / 3));
    }
    /** Stagger applied to a body that collides with one already placed in its ring. */
    /** The transit band is thinner than the natal wheel, so its rings sit closer. */
    private static final double TRANSIT_RING_GAP = 20.0;
    private static final double TRANSIT_RING_STEP = 12.0;

    /**
     * Which ring a registry point belongs to, or -1 for the four angles.
     *
     * Reads Bodies.Group rather than a list of names here, so a point added to the
     * registry lands in a ring automatically instead of silently defaulting to the
     * outermost one and looking like a planet.
     */
    private static int ringOf(int n) {
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
    private static boolean[] restrictToRing(boolean[] blArray, int n) {
        boolean[] blArray2 = new boolean[blArray.length];
        for (int i = 0; i < blArray.length; ++i) {
            blArray2[i] = blArray[i] && SkymapPanel.ringOf(i) == n;
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
    private static int[] ringedRadii(double[] dArray, boolean[] blArray, double base, double gap,
                              double step, double spacing, double floorPx) {
        int[][] nArray = new int[RING_COUNT][];
        for (int i = 0; i < RING_COUNT; ++i) {
            nArray[i] = SkymapPanel.radialLevels(dArray, SkymapPanel.restrictToRing(blArray, i),
                base - (double)i * gap, step, spacing);
        }
        int[] nArray2 = new int[BODY_COUNT];
        for (int i = 0; i < BODY_COUNT; ++i) {
            int n = SkymapPanel.ringOf(i);
            if (n < 0) {
                continue;                       // an angle; the caller fills these in
            }
            nArray2[i] = (int)Math.max(
                base - (double)n * gap - (double)nArray[n][i] * step, floorPx);
        }
        return nArray2;
    }

    /**
     * Where the bodies sit, from the ring table and the reader's choice.
     *
     * <b>Both the painter and the hit test must ask this.</b> They already both call
     * natalRadii - the note above that method says so - and a placement offset applied to only
     * one of them would put the glyphs somewhere the clicks are not. That is invisible until
     * someone tries to click a planet and nothing happens.
     */
    int bodyBaseRadius(int[] rings) {
        int base = rings[RING_BODY_TOP];
        String ring = Settings.bodyRing();
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
    static int[] bandRadii(double[] lon, boolean[] valid, int bandOuter, int bandInner) {
        return SkymapPanel.bandRadii(lon, valid, bandOuter, bandInner, BAND_EDGE, 28.0);
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
    static int[] bandRadii(double[] lon, boolean[] valid, int bandOuter, int bandInner,
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

        int[] bodies = SkymapPanel.ringedRadii(lon, SkymapPanel.restrict(valid, false),
            top, gap, step, spacing, floor);

        // Angles ride the middle of the band, where they read as belonging to it rather than
        // to either neighbour.
        double mid = (top + floor) / 2.0;
        double angleStep = Math.min(gap * 0.5, 18.0);
        int[] levels = SkymapPanel.radialLevels(lon, SkymapPanel.restrict(valid, true),
            mid, angleStep, spacing);

        int[] out = new int[BODY_COUNT];
        for (int i = 0; i < BODY_COUNT; i++) {
            out[i] = Bodies.at(i).isAngle()
                ? (int) Math.max(mid - (double) levels[i] * angleStep, floor)
                : bodies[i];
        }
        return out;
    }

    /**
     * The colour a glyph is drawn in, by element.
     *
     * <b>The no-element fallback depends on what the glyph is drawn ON.</b> Nine chart points
     * carry no element - south_node, eris, eros, hygiea, nessus, pholus, fortune, spirit and
     * lilith - because their correspondence is genuinely contested and the registry declines
     * to invent one. There is no single right colour for them, only a right contrast:
     *
     * <ul>
     *   <li>the natal ring draws on metallic SILVER spheres (192,192,192), so they take
     *       black - near-white was pale ink on a pale ground and made Pholus, Nessus and the
     *       two lots the hardest glyphs on the wheel to read;</li>
     *   <li>the transit and sky rings draw on DARK cubes (62,66,76) and (20,50,80), so they
     *       take near-white. Black there is the same mistake in reverse, and briefly was one:
     *       this method returned a flat black on 2026-08-31 and those two rings rendered their
     *       elementless glyphs as mid-grey on dark slate until it was corrected the same day.</li>
     * </ul>
     *
     * <b>The prose colour is a third answer.</b> {@link #elementTextHex} keeps a light
     * fallback, because a reading is light text on a dark panel. Three surfaces, three
     * grounds - the element is the same, the legible ink is not.
     */
    private Color getElementColor(int n) {
        return getElementColor(n, true);
    }

    /**
     * @param lightBacking true when the glyph sits on the pale natal sphere, false when it
     *                     sits on one of the dark transit or sky cubes
     */
    /**
     * The colour body {@code i} is drawn in: its own override, else its element's.
     *
     * <b>Introduced so there is exactly one answer.</b> Five call sites asked
     * {@code getElementColor(BODY_ELEMENTS[i])} directly - three on the wheel, two in the grid -
     * and a per-body override added to some of them would have produced a chart where a body
     * was one colour on the wheel and another in the table.
     */
    Color bodyColor(int bodyIndex) {
        return ChartPalette.colorOr(
            ChartPalette.bodyHex(Bodies.at(bodyIndex).id),
            this.getElementColor(BODY_ELEMENTS[bodyIndex]));
    }

    String bodyColorHex(int bodyIndex) {
        String own = ChartPalette.bodyHex(Bodies.at(bodyIndex).id);
        return own != null ? own : this.getElementColorHex(BODY_ELEMENTS[bodyIndex]);
    }

    private Color getElementColor(int n, boolean lightBacking) {
        if (n < 0 || n >= ELEMENT_COLORS.length) {
            return lightBacking ? Color.BLACK : new Color(226, 228, 234);
        }
        // <b>Through the palette, with this panel's own constant as the fallback.</b> The
        // constants stay as the last word on what Classic means; what changed is that they are
        // no longer the ONLY word - Settings now shows the same four colours the wheel paints,
        // which it did not before.
        return ChartPalette.colorOr(
            ChartPalette.elementHex(n, hex(ELEMENT_COLORS[n])), ELEMENT_COLORS[n]);
    }

    /**
     * The chart's structural lines: rings, spokes, the centre crosshair.
     *
     * <b>Was a hardcoded light grey at four call sites.</b> That is why a white-ground template
     * drew an invisible wheel - near-white rings on white - with the glyphs apparently floating
     * unattached. A template that moves the background has to move this with it.
     */
    /** Black or white, whichever is legible on the given fill. */
    static Color readableOn(Color fill) {
        double luma = (0.299 * fill.getRed() + 0.587 * fill.getGreen() + 0.114 * fill.getBlue());
        return luma > 140 ? new Color(24, 20, 6) : new Color(240, 240, 240);
    }

    static Color inkColor() {
        return ChartPalette.colorOr(ChartPalette.inkHex("#DCDCDC"), new Color(220, 220, 220));
    }

    /** A Color as #RRGGBB, for handing this panel's constants to the palette as fallbacks. */
    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void drawMoonPhase(Graphics2D graphics2D, int n, int n2, int n3, double d) {
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

    public SkymapPanel(OuraniaWindow ouraniaWindow) {
        java.util.Properties serializable;
    JPanel controlPanel;
        this.window = ouraniaWindow;
        this.setLayout(new BorderLayout());
        this.setBackground(Color.BLACK);
        // Before any chart is built, so a saved reference place is in force for the first
        // composite rather than taking effect only after the user next touches the setting.
        this.loadCompositeReferencePlace();
        try {
            this.sw = new SwissEph(EPHE_PATH);
        }
        catch (Exception exception) {
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\error_log.txt"));
                exception.printStackTrace(pw);
                pw.close();
            } catch (Exception ex) {}
            exception.printStackTrace();
        }
        try {
            serializable = Settings.load();
            String string = serializable.getProperty("home.location");
            this.baseLocationName = string != null ? string : serializable.getProperty("default.base.location", "Los Angeles, CA");
            this.transitLocationName = string != null ? string : serializable.getProperty("default.transit.location", "Los Angeles, CA");
            this.currentHouseSystemName = serializable.getProperty("default.house.system", "Placidus");
            switch (this.currentHouseSystemName) {
                case "Koch": {
                    this.houseSystem = (char)75;
                    break;
                }
                case "Equal": {
                    this.houseSystem = (char)69;
                    break;
                }
                case "Whole Sign": {
                    this.houseSystem = (char)87;
                    break;
                }
                case "Campanus": {
                    this.houseSystem = (char)67;
                    break;
                }
                case "Regiomontanus": {
                    this.houseSystem = (char)82;
                    break;
                }
                default: {
                    this.houseSystem = (char)80;
                }
            }
            try {
                Object object = this.syncGeocode(this.baseLocationName);
                if (object != null) {
                    this.natalRing.latitude = ((Geocoder.Result)object).lat;
                    this.natalRing.longitude = ((Geocoder.Result)object).lon;
                    this.baseLocationName = ((Geocoder.Result)object).name;
                    this.baseTimeZoneId = ((Geocoder.Result)object).tzId;
                }
            }
            catch (Exception exception) {}
        }
        catch (Exception exception) {
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\error_log.txt"));
                exception.printStackTrace(pw);
                pw.close();
            } catch (Exception ex) {}
            exception.printStackTrace();
        }
        this.natalRing.time = ZonedDateTime.now(ZoneId.of(this.baseTimeZoneId));
        this.outerRing.time = ZonedDateTime.now(ZoneId.of(this.transitTimeZoneId));
        this.updateChartData();
        this.chartPanel = new ChartPanel();
        this.chartPanel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                // A scrub moved the time; the click that ends it chose nothing.
                if (SkymapPanel.this.scrubDragged) {
                    SkymapPanel.this.scrubDragged = false;
                    SkymapPanel.this.globeTurned = false;
                    SkymapPanel.this.viewPanned = false;
                    return;
                }
                // A drag on the globe turns it; a click still selects. The threshold is what
                // separates the two, because every drag ends in a click event as well.
                if (SkymapPanel.this.globeMode && SkymapPanel.this.globeTurned) {
                    SkymapPanel.this.globeTurned = false;
                    return;
                }
                if (SkymapPanel.this.globeMode) {
                    SkymapPanel.this.handleChartClick(mouseEvent.getX(), mouseEvent.getY());
                    return;
                }
                // A drag that panned ends in a click as well; it moved the view, it chose nothing.
                if (SkymapPanel.this.viewPanned) {
                    SkymapPanel.this.viewPanned = false;
                    return;
                }
                if (!SkymapPanel.this.viewIsFit() && fitChipBounds().contains(mouseEvent.getPoint())) {
                    SkymapPanel.this.fitView();
                    SkymapPanel.this.chartPanel.repaint();
                    return;
                }
                // A double-click on the wheel also fits it, so there is a way back that needs no
                // aim. The first click of the pair has already selected, which is harmless.
                if (mouseEvent.getClickCount() == 2 && !SkymapPanel.this.viewIsFit()) {
                    SkymapPanel.this.fitView();
                    SkymapPanel.this.chartPanel.repaint();
                    return;
                }
                java.awt.Point at = SkymapPanel.this.toWheel(mouseEvent.getX(), mouseEvent.getY());
                SkymapPanel.this.handleChartClick(at.x, at.y);
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                SkymapPanel.this.dragFrom = mouseEvent.getPoint();
                SkymapPanel.this.globeTurned = false;
                SkymapPanel.this.viewPanned = false;
                SkymapPanel.this.scrubDragged = false;
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                SkymapPanel.this.dragFrom = null;
                if (SkymapPanel.this.scrubbing && SkymapPanel.this.scrubDragged) {
                    SkymapPanel.this.endScrub();
                    SkymapPanel.this.chartPanel.setCursor(java.awt.Cursor.getDefaultCursor());
                }
                if (SkymapPanel.this.globeDragging) {
                    SkymapPanel.this.globeDragging = false;
                    SkymapPanel.this.chartPanel.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                // Without this the chart stays dimmed around whatever the cursor left on,
                // which reads as a rendering fault rather than as a selection.
                boolean cleared = SkymapPanel.this.setFocus(-1);
                cleared |= SkymapPanel.this.setHover(-1, -1);
                if (cleared) {
                    SkymapPanel.this.chartPanel.repaint();
                }
            }
        });
        this.chartPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter(){

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                if (SkymapPanel.this.dragFrom == null) {
                    return;
                }
                // <b>Shift+drag scrubs time</b>, on the wheel and on the globe: right is later,
                // one step of the Step setting per SCRUB_PX. Measured from where the press was,
                // so dragging back to it returns to the moment the scrub began. E8.
                if (mouseEvent.isShiftDown() || SkymapPanel.this.scrubDragged) {
                    int sdx = mouseEvent.getX() - SkymapPanel.this.dragFrom.x;
                    if (!SkymapPanel.this.scrubDragged && Math.abs(sdx) < SCRUB_PX) {
                        return;
                    }
                    SkymapPanel.this.scrubDragged = true;
                    SkymapPanel.this.beginScrub();
                    SkymapPanel.this.scrubTo(sdx / SCRUB_PX);
                    SkymapPanel.this.chartPanel.setCursor(
                        java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR));
                    SkymapPanel.this.chartPanel.repaint();
                    return;
                }
                // <b>The flat wheel pans when it is zoomed in.</b> At fit there is nowhere to go,
                // so a drag stays a click with a shaky hand, as it always was.
                if (!SkymapPanel.this.globeMode) {
                    if (SkymapPanel.this.viewIsFit()) {
                        return;
                    }
                    int pdx = mouseEvent.getX() - SkymapPanel.this.dragFrom.x;
                    int pdy = mouseEvent.getY() - SkymapPanel.this.dragFrom.y;
                    if (!SkymapPanel.this.viewPanned && pdx * pdx + pdy * pdy < 9) {
                        return;
                    }
                    SkymapPanel.this.viewPanned = true;
                    SkymapPanel.this.panBy(pdx, pdy);
                    SkymapPanel.this.dragFrom = mouseEvent.getPoint();
                    SkymapPanel.this.chartPanel.setCursor(
                        java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
                    SkymapPanel.this.chartPanel.repaint();
                    return;
                }
                int dx = mouseEvent.getX() - SkymapPanel.this.dragFrom.x;
                int dy = mouseEvent.getY() - SkymapPanel.this.dragFrom.y;
                // Below a few pixels this is a click with a shaky hand, not a turn.
                if (!SkymapPanel.this.globeTurned && dx * dx + dy * dy < 9) {
                    return;
                }
                SkymapPanel.this.globeTurned = true;
                SkymapPanel.this.globeDragging = true;
                SkymapPanel.this.globe.drag(dx, dy, SkymapPanel.this.chartPanel.getWidth());
                SkymapPanel.this.dragFrom = mouseEvent.getPoint();
                SkymapPanel.this.chartPanel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                if (SkymapPanel.this.globeMode) {
                    // <b>Body, then house number, then degree tick.</b> Nearest-first would
                    // read better than a fixed order if the three shared a radius, and they
                    // do not: a glyph is what the reader means when they are on one, and a
                    // tick at the rim should never take a click aimed at the wheel.
                    int x = mouseEvent.getX();
                    int y = mouseEvent.getY();
                    int w2 = SkymapPanel.this.chartPanel.getWidth();
                    int h2 = SkymapPanel.this.chartPanel.getHeight();
                    int over = SkymapPanel.this.bodyAt(x, y);
                    // The click's own resolver, so whatever lights under the cursor is what
                    // opens when it is clicked.
                    int[] t = over >= 0 ? new int[]{GLOBE_NONE, -1}
                        : SkymapPanel.this.globeTargetAt(x, y);
                    int house = t[0] == GLOBE_HOUSE ? t[1] : -1;
                    int deg = t[0] == GLOBE_DEGREE ? t[1] : -1;
                    int station = t[0] == GLOBE_MANSION ? t[1] : -1;
                    boolean moved = SkymapPanel.this.setFocusDegree(deg);
                    moved |= SkymapPanel.this.setFocusHouse(house);
                    moved |= SkymapPanel.this.setFocusMansion(station);
                    // The hand over anything the globe answers.
                    boolean clickable = over >= 0 || t[0] != GLOBE_NONE;
                    SkymapPanel.this.chartPanel.setCursor(java.awt.Cursor.getPredefinedCursor(
                        clickable ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
                    moved |= SkymapPanel.this.setHover(over, -1);
                    if (SkymapPanel.this.setFocus(over) || moved) {
                        SkymapPanel.this.chartPanel.repaint();
                    }
                    return;
                }
                // The tooltip is no longer pushed from here. ChartPanel overrides
                // getToolTipText(MouseEvent) and ToolTipManager asks it, which is the whole
                // fix: see the note on that override.
                // Focus follows the cursor. setFocus reports whether anything actually
                // changed, so sweeping across one glyph repaints once rather than on every
                // pixel of travel - the same guard setHighlightedAspect already uses.
                // Asked in the wheel's own coordinates, whatever the zoom - see viewTransform.
                java.awt.Point atWheel = SkymapPanel.this.toWheel(mouseEvent.getX(), mouseEvent.getY());
                int px = atWheel.x;
                int py = atWheel.y;
                if (!SkymapPanel.this.viewIsFit() && fitChipBounds().contains(mouseEvent.getPoint())) {
                    boolean left = SkymapPanel.this.setFocus(-1);
                    left |= SkymapPanel.this.setHighlightedChord(null);
                    left |= SkymapPanel.this.setHover(-1, -1);
                    SkymapPanel.this.chartPanel.setCursor(
                        java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    if (left) {
                        SkymapPanel.this.chartPanel.repaint();
                    }
                    return;
                }
                int over = SkymapPanel.this.bodyAt(px, py);
                boolean moved = SkymapPanel.this.setFocus(over);
                // <b>And the lines themselves.</b> Until now the only place an aspect could
                // be hovered was the grid in the drawer - the chords on the wheel, which is
                // what a reader is actually looking at, lit nothing on any ring. A glyph wins
                // the cursor when there is one under it, because a body is what someone
                // resting on a body means.
                int[] chord = over >= 0 ? null : SkymapPanel.this.chordAt(px, py);
                moved |= SkymapPanel.this.setHighlightedChord(chord);
                // The swell: whatever this point would open, shown before the click.
                int ring = over >= 0 || chord != null ? -1 : SkymapPanel.this.ringTargetAt(px, py);
                moved |= SkymapPanel.this.setHover(over, ring);
                SkymapPanel.this.chartPanel.setCursor(java.awt.Cursor.getPredefinedCursor(
                    over >= 0 || chord != null || ring >= 0
                        ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
                if (moved) {
                    SkymapPanel.this.chartPanel.repaint();
                }
            }
        });
        this.chartPanel.addMouseWheelListener(e -> {
            if (!SkymapPanel.this.globeMode) {
                // Zoom about the cursor. It had nothing to scroll; E6.
                SkymapPanel.this.zoomAt(e.getX(), e.getY(), e.getPreciseWheelRotation());
                SkymapPanel.this.chartPanel.repaint();
                return;
            }
            SkymapPanel.this.globe.zoom(e.getPreciseWheelRotation());
            SkymapPanel.this.chartPanel.repaint();
        });
        // A chart is read by sweeping across it, so the default 750ms feels broken here.
        // <b>Registered once, with a non-null placeholder, and never unregistered.</b>
        // The text itself comes from getToolTipText(MouseEvent) below.
        this.chartPanel.setToolTipText("");
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay(220);
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(20000);
        // chartPanel goes in via the OverlayDock below, so the transport drawer can lie
        // over the bottom of the wheel's area rather than taking height from it.
        controlPanel = this.createControlPanel();
        this.refreshScrubBars();
        // <b>The transport row goes in a drawer, and its handle is the clock.</b> The strip
        // was two wrapped rows of controls permanently across the bottom of the window, most
        // of them set once and left alone. Put away, the one thing that must stay readable is
        // the moment being drawn - which is exactly what a handle spanning the window has
        // room for, so the handle carries it rather than a separate always-on label competing
        // for the same space.
        this.timeDrawer = new Drawer(Drawer.Side.BOTTOM, "", controlPanel, 120);
        // <b>Over the wheel's area, not under it.</b> Pushing moved the wheel up and rescaled
        // it every time the row was opened, so glancing at the transport buttons redrew the
        // whole chart. Overlaid, the wheel does not move at all - the row covers the empty
        // band below a circle inscribed in a rectangle, which is space the chart was not
        // using.
        this.add(new OverlayDock(this.chartPanel, null, this.timeDrawer), "Center");
        this.refreshTimeReadout();
        // The chips reflect the chart that was actually built, not the last thing clicked.
        if (this.window != null && this.ringBar != null) {
            this.window.syncRingBar(this.ringBar);
        }
        this.animationTimer = new Timer(50, actionEvent -> {
            if (this.isPlaying) {
                this.stepTime();
                this.updateChartData();
                this.chartPanel.repaint();
            }
        });
        this.animationTimer.start();
    }

    /** The eight-argument form: Chart A's time is known. */
    public void applyChartSettings(String string, String string2, String string3,
                                   ChartMode chartMode, String string4, String string5,
                                   String string6, boolean transits) {
        applyChartSettings(string, string2, string3, chartMode, string4, string5, string6,
            transits, false, "", "");
    }

    /**
     * @param baseUnknown Chart A's birth time is not known: cast for noon, angles withheld.
     * @param zoneOverride an IANA zone id the reader chose, or empty to trust the location.
     * @param relocate a place to recast the houses for, or empty for the birthplace.
     */
    public void applyChartSettings(final String string, final String string2, final String string3, ChartMode chartMode, final String string4, final String string5, final String string6, final boolean transits, final boolean baseUnknown, final String zoneOverride, final String relocate) {
        this.applyChartSettings(string, string2, string3, chartMode, string4, string5, string6,
            transits, baseUnknown, zoneOverride, relocate, "");
    }

    /** As above, with Chart B's own zone override - every subject has one now. */
    public void applyChartSettings(final String string, final String string2, final String string3, ChartMode chartMode, final String string4, final String string5, final String string6, final boolean transits, final boolean baseUnknown, final String zoneOverride, final String relocate, final String tZoneOverride) {
        this.baseTimeUnknown = baseUnknown;
        // A chart cast from the form is a new birth time, not a scrubbed one: nothing to reset to.
        this.scrubOrigins.clear();
        this.baseZoneOverride = zoneOverride == null ? "" : zoneOverride.trim();
        this.transitZoneOverride = tZoneOverride == null ? "" : tZoneOverride.trim();
        this.relocateTo = relocate == null ? "" : relocate.trim();
        final boolean bl = chartMode != ChartMode.SINGLE;
        this.installMode(chartMode, transits);
        final boolean bl2 = this.isPlaying;
        this.isPlaying = false;
        SwingWorker<Void, Void> swingWorker = new SwingWorker<Void, Void>(){

            @Override
            protected Void doInBackground() throws Exception {
                // <b>Two subjects are built here and installed together.</b> This method used
                // to write fourteen fields one at a time, interleaved with the lookups that
                // produced them, and which field a value landed in was decided by where in the
                // sequence it appeared. That is how the sky came to be cast for Chart B's
                // birthplace: not a wrong line, a value written into the wrong wheel because
                // nothing said which wheel it belonged to. A subject carries its own name.
                com.zodiacomputing.ourania.astro.ChartSubject a =
                    SkymapPanel.this.subjectFrom("Chart A", string, string2, string3,
                        SkymapPanel.this.baseZoneOverride, SkymapPanel.this.relocateTo,
                        SkymapPanel.this.baseTimeUnknown, SkymapPanel.this.subjectA);
                com.zodiacomputing.ourania.astro.ChartSubject b = bl
                    ? SkymapPanel.this.subjectFrom("Chart B", string4, string5, string6,
                        SkymapPanel.this.transitZoneOverride, "", false,
                        SkymapPanel.this.subjectB)
                    : SkymapPanel.this.subjectB;
                SkymapPanel.this.installSubjects(a, b, SkymapPanel.this.subjectSky);
                return null;
            }

            @Override
            protected void done() {
                SkymapPanel.this.updateChartData();
                SkymapPanel.this.chartPanel.repaint();
                SkymapPanel.this.isPlaying = bl2;
                // The chips name the charts they would draw, so they have to be told when the
                // charts change. Without this they kept the text their constructor gave them.
                if (SkymapPanel.this.window != null && SkymapPanel.this.ringBar != null) {
                    SkymapPanel.this.window.syncRingBar(SkymapPanel.this.ringBar);
                }
                // <b>And the setup form is told what had to be assumed.</b> Moments.resolve
                // computes it, subjectFrom keeps it, and until this line nothing showed it -
                // which is how it came to be written, checked, and never once seen by a reader.
                if (SkymapPanel.this.window != null) {
                    SkymapPanel.this.window.showClockNotice(
                        SkymapPanel.this.subjectA, SkymapPanel.this.subjectB);
                }
                SkymapPanel.this.refreshPrecisionNotice();
            }
        };
        swingWorker.execute();
    }

    /** Hands the setup form what the ephemeris could not do for the charts now drawn. */
    void refreshPrecisionNotice() {
        if (this.window != null) {
            this.window.showPrecisionNotice(this.precisionNotes());
        }
    }

    /**
     * What the ephemeris substituted for the rings actually on the wheel, one sentence each.
     *
     * <b>Asked of the rings the painter draws, by the rule the painter uses.</b> The wheel casts
     * its own cusps in updateChartData rather than reading a ChartFrame, so a flag on the frame
     * would describe a chart the wheel is not showing whenever the two disagree about which
     * moment or place a ring stands for. The branches below are that method's: the inner ring
     * is Chart A or the sky, the outer is Chart B only in a synastry, the third ring is the sky.
     *
     * A composite's inner ring is left out. Its cusps come from swe_houses_armc at a reference
     * latitude, which is a separate cast with its own failure; saying nothing there is a gap,
     * saying the wrong thing would be worse.
     */
    java.util.List<String> precisionNotes() {
        java.util.LinkedHashSet<String> notes = new java.util.LinkedHashSet<>();
        if (this.sw == null) {
            return new java.util.ArrayList<>();
        }
        boolean relationship = this.isRelationshipChart();
        double bodiesAt = Double.NaN;
        if (this.natalRing.sd != null && !relationship) {
            String label = this.innerIsBirthChart ? "Chart A" : "Sky";
            double lat = this.innerIsBirthChart ? this.natalRing.latitude : this.skyRing.latitude;
            double lon = this.innerIsBirthChart ? this.natalRing.longitude : this.skyRing.longitude;
            bodiesAt = this.natalRing.sd.getJulDay();
            if (com.zodiacomputing.ourania.astro.Precision.housesFellBack(
                    this.sw, bodiesAt, lat, lon, this.houseSystem)) {
                notes.add(com.zodiacomputing.ourania.astro.Precision.housesNote(
                    label, lat, this.houseSystem));
            }
        }
        SweDate outerSd = this.isSynastryChart() ? this.outerRing.sd : this.skyRing.sd;
        if (this.showTransitChart && outerSd != null && !this.showProgressed()) {
            boolean synastry = this.isSynastryChart();
            double lat = synastry ? this.outerRing.latitude : this.skyRing.latitude;
            double lon = synastry ? this.outerRing.longitude : this.skyRing.longitude;
            if (com.zodiacomputing.ourania.astro.Precision.housesFellBack(
                    this.sw, outerSd.getJulDay(), lat, lon, this.houseSystem)) {
                notes.add(com.zodiacomputing.ourania.astro.Precision.housesNote(
                    synastry ? "Chart B" : "Sky", lat, this.houseSystem));
            }
        }
        if (this.showTriWheel && this.skyRing.sd != null
                && com.zodiacomputing.ourania.astro.Precision.housesFellBack(this.sw,
                    this.skyRing.sd.getJulDay(), this.skyRing.latitude, this.skyRing.longitude,
                    this.houseSystem)) {
            notes.add(com.zodiacomputing.ourania.astro.Precision.housesNote(
                "Sky", this.skyRing.latitude, this.houseSystem));
        }
        // A missing data file loses the same bodies from every ring, so one moment is enough to
        // find them. Chiron's date limits are the exception, and the inner ring is the one a
        // reader is most likely to have cast at an unusual date.
        if (Double.isNaN(bodiesAt) && this.skyRing.sd != null) {
            bodiesAt = this.skyRing.sd.getJulDay();
        }
        if (!Double.isNaN(bodiesAt)) {
            String bodies = com.zodiacomputing.ourania.astro.Precision.bodiesNote(
                com.zodiacomputing.ourania.astro.Precision.unplaceable(
                    this.sw, bodiesAt, Settings.loadBodySelection()));
            if (bodies != null) {
                notes.add(bodies);
            }
        }
        return new java.util.ArrayList<>(notes);
    }

    private Geocoder.Result syncGeocode(String string) {
        return Geocoder.lookup(string);
    }

    private static void styleCombo(JComboBox<String> jComboBox) {
        Widgets.styleCombo(jComboBox);
    }

    /**
     * The aspect grid on its own, rather than at the bottom of a reading.
     *
     * The grid has always been generated as the tail of
     * {@code generatePlanetPlacementsHtml}, so the only way to see it was to open a reading
     * and scroll past the placements. It is a table, it is clickable, and people go to it
     * directly - so it gets a way in of its own.
     */
    private void showAspectTable() {
        if (this.window == null) {
            return;
        }
        this.window.showInterpretationHtml(this.generatePlanetPlacementsHtml());
    }

    /** The chart-setting dropdowns, for the Settings screen to display. */
    public JPanel chartControlsPanel() {
        return this.chartControls;
    }

    /**
     * The control strip's dropdowns, narrower than the app's default.
     *
     * Seven labelled dropdowns do not fit one 1024-wide row at bold 12, and House System was
     * the one that fell off the end. This buys the room back; {@link Widgets.WrapLayout} on
     * the row is what makes it safe at any width rather than at one width.
     */
    private static void styleCompactCombo(JComboBox<String> jComboBox) {
        Widgets.styleCompactCombo(jComboBox);
    }

    /**
     * A control-strip label short enough to leave room for its dropdown.
     *
     * <b>The label text was the actual overflow.</b> "Harmonic:", "Animate:", "Align Houses:"
     * and "House System:" cost more of the row than several of the dropdowns they name, and
     * House System - last in the row - was the one pushed off the end of a 1024-wide window.
     * <b>The full name is not lost, it moves to the tooltip</b>, so an abbreviation nobody
     * recognises is one hover from being spelled out.
     */
    private static JLabel compactLabel(String shortText, String fullName) {
        JLabel label = new JLabel(shortText);
        label.setForeground(Color.WHITE);
        label.setFont(Theme.font("Arial", Font.PLAIN, 11));
        label.setToolTipText(fullName);
        return label;
    }

    private void showAnnualCalendar() {
        if (this.sw == null || this.natalRing.time == null || this.window == null) {
            return;
        }
        final int n = this.natalRing.time.getYear();
        this.readingTier = ReadingTier.NONE;
        this.window.showCalendar(n, "Scanning the ephemeris for " + n + "...");
        new SwingWorker<String, Void>(){

            @Override
            protected String doInBackground() {
                SwissEph swissEph = new SwissEph(SkymapPanel.EPHE_PATH);
                SweDate sweDate = new SweDate(n, 1, 1, 0.0);
                SweDate sweDate2 = new SweDate(n + 1, 1, 1, 0.0);
                List<Almanac.Event> list = Almanac.annualCalendar(swissEph, sweDate.getJulDay(), sweDate2.getJulDay());
                return SkymapPanel.formatCalendar(list);
            }

            @Override
            protected void done() {
                try {
                    SkymapPanel.this.window.showCalendar(n, (String)this.get());
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }.execute();
    }

    private static YearScan scanProfectionYear(ChartFrame chartFrame, double d, double d2, Profection profection, List<BodyScore.Vector> list, double d3, double d4, int n) {
        Returns.Return return_;
        SwissEph swissEph = new SwissEph(EPHE_PATH);
        double d5 = Double.NaN;
        double d6 = Double.NaN;
        ChartFrame.Body body = chartFrame.body("Sun");
        if (body != null && body.ok) {
            d5 = Profection.solarReturnJd(swissEph, d, body.lon, profection.age);
            d6 = Profection.solarReturnJd(swissEph, d, body.lon, profection.age + 1);
        }
        if (Double.isNaN(d5) || Double.isNaN(d6) || d6 <= d5) {
            d5 = d2 - 182.6;
            d6 = d2 + 182.6;
        }
        YearScan yearScan = new YearScan();
        yearScan.events = Transits.eventsToNatal(Almanac.datedMoments(swissEph, d5, d6), chartFrame, list, profection.lord);
        yearScan.perfections = Transits.perfectionsOverRange(swissEph, chartFrame, list, profection.lord, d5, d6);
        yearScan.arcs = SolarArc.contacts(swissEph, chartFrame, d, list, profection.lord, d5, d6);
        yearScan.progressions = Progressions.contacts(swissEph, chartFrame, d, list, profection.lord, d5, d6);
        ChartFrame.Body body2 = chartFrame.body("Sun");
        if (body2 != null && body2.ok && (return_ = Returns.solar(swissEph, d, body2.lon, profection.age, d3, d4, n)) != null) {
            yearScan.returns = Returns.contacts(return_, chartFrame, list, profection.lord);
        }
        return yearScan;
    }

    private static String formatCalendar(List<Almanac.Event> list) {
        String[] stringArray = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        StringBuilder stringBuilder = new StringBuilder();
        int n = -1;
        for (Almanac.Event event : list) {
            SweDate sweDate = new SweDate();
            sweDate.setJulDay(event.jd);
            if (sweDate.getMonth() != n) {
                n = sweDate.getMonth();
                stringBuilder.append(n == 1 ? "" : "\n").append(stringArray[n - 1]).append('\n');
            }
            double d = sweDate.getHour();
            stringBuilder.append(String.format("  %2d  %02d:%02d   %s%n", sweDate.getDay(), (int)d, (int)(d % 1.0 * 60.0), event));
        }
        if (list.isEmpty()) {
            stringBuilder.append("  (nothing found - check the ephemeris range)\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Chart B's frame in synastry mode, or null when there is no second chart.
     *
     * Computed rather than read off outerRing.lon/outerRing.valid on purpose. Those arrays carry longitudes
     * and nothing else; the cross-chart questions need chart B's HOUSE CUSPS and its four
     * angles, which only a ChartFrame has. The two agree by construction - same julian day,
     * same place, same house system, same ephemeris - so this is a second view of chart B,
     * not a second opinion about it.
     */
    private ChartFrame synastryChartB() {
        if (this.sw == null || this.outerRing.sd == null) {
            return null;
        }
        String string = SkymapPanel.frameKey(this.outerRing.sd.getJulDay(), this.outerRing.latitude,
            this.outerRing.longitude, this.houseSystem);
        ChartFrame chartFrame = this.synastryBFrame;
        if (chartFrame != null && string.equals(this.synastryBKey)) {
            return chartFrame;
        }
        this.synastryBFrame = chartFrame = ChartFrame.compute(this.sw, this.outerRing.sd.getJulDay(),
            this.outerRing.latitude, this.outerRing.longitude, this.houseSystem, false, 0.0);
        this.synastryBKey = string;
        return chartFrame;
    }

    /**
     * The cross-chart placement block: what the aspect grid below it cannot say.
     *
     * Four sections, angle contacts before overlays because a body on an angle is the
     * loudest thing in a synastry and belongs above a list of twenty-five house
     * assignments. Each technique is run twice with the charts swapped: A into B and B into
     * A are different readings about different people, and showing one direction would
     * silently privilege whoever happened to be entered first.
     *
     * <b>The orb here is not the orb the grid below uses.</b> The angle contacts read at
     * the halved synastry orb; the grid still reads at full natal orbs. See
     * {@link Synastry#angleContacts} - the difference is deliberate, recorded, and David's
     * to settle.
     */
    /**
     * Which of chart A's angles this chart-B body sits on, or null.
     *
     * <b>Uses {@link Synastry#angleContacts} rather than a threshold of its own.</b> A
     * "contact" is already defined there - a conjunction inside the HALVED synastry orb, with
     * opposite-pair bodies skipped so one placement is not counted twice against an axis. A
     * second definition here would be the same rule implemented twice, which is the defect
     * this codebase names most often.
     *
     * Note the orb caveat recorded on generateSynastryCrossHtml: these contacts read at the
     * halved orb while the aspect grid reads at full natal orbs. That difference is open and
     * David's to settle; this method inherits whichever answer angleContacts gives.
     */
    public String synastryAngleContact(String bodyName) {
        if (bodyName == null || !this.isSynastryChart()
                || this.sw == null || this.natalRing.sd == null || this.outerRing.sd == null) {
            return null;
        }
        ChartFrame host = this.frameForCurrentChart(this.natalRing.sd.getJulDay(),
            this.natalRing.latitude, this.natalRing.longitude, this.houseSystem);
        ChartFrame visitor = this.synastryChartB();
        if (host == null || visitor == null) {
            return null;
        }
        // Tightest first, so the first match for this body is its closest angle.
        for (Synastry.AngleContact c : Synastry.angleContacts(visitor, host)) {
            if (c.body != null && c.body.equalsIgnoreCase(bodyName)) {
                return c.angle;
            }
        }
        return null;
    }

    private String generateSynastryCrossHtml() {
        if (this.sw == null || this.natalRing.sd == null || this.outerRing.sd == null) {
            return "";
        }
        ChartFrame chartFrame = this.frameForCurrentChart(this.natalRing.sd.getJulDay(),
            this.natalRing.latitude, this.natalRing.longitude, this.houseSystem);
        ChartFrame chartFrame2 = this.synastryChartB();
        if (chartFrame == null || chartFrame2 == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<br><hr style='border-color:#444;'><br>");
        stringBuilder.append(this.angleContactsHtml("Chart A on Chart B's angles",
            chartFrame, chartFrame2));
        stringBuilder.append(this.angleContactsHtml("Chart B on Chart A's angles",
            chartFrame2, chartFrame));
        stringBuilder.append(this.houseOverlayHtml("Chart A's placements in Chart B's houses",
            chartFrame, chartFrame2));
        stringBuilder.append(this.houseOverlayHtml("Chart B's placements in Chart A's houses",
            chartFrame2, chartFrame));
        return stringBuilder.toString();
    }

    /** One direction of angle contacts, tightest first. */
    private String angleContactsHtml(String string, ChartFrame chartFrame, ChartFrame chartFrame2) {
        List<Synastry.AngleContact> list = Synastry.angleContacts(chartFrame, chartFrame2);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<h3 style='color:#FFD166; margin:0 0 4px 0;'>Angle contacts &middot; ")
            .append(string).append("</h3>");
        if (list.isEmpty()) {
            // Said out loud rather than left blank, on the same reasoning as the aspect
            // pattern box: a section that vanishes cannot be told from a section that broke.
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:10px;'>")
                .append("Nothing sits on the Ascendant, Descendant, MC or IC within orb.</div>");
            return stringBuilder.toString();
        }
        stringBuilder.append("<div style='border:1px solid #FFD166; padding:6px; margin-bottom:10px;'>");
        for (Synastry.AngleContact angleContact : list) {
            stringBuilder.append("<div style='margin-bottom:3px; color:#cccccc; font-size:12px;'>")
                .append("<span style='font-size:15px;'>").append(SkymapPanel.namedGlyph(angleContact.bodyIndex))
                .append("</span> ").append(angleContact.body).append(" ")
                .append(Zodiac.format(angleContact.bodyLon))
                .append(" <span style='color:#FFD166;'>on</span> ").append(angleContact.angle)
                .append(" ").append(Zodiac.format(angleContact.angleLon))
                .append(" <span style='color:#9AA5B1;'>&mdash; orb ")
                .append(SkymapPanel.formatOrb(angleContact.orb)).append(" of ")
                .append(SkymapPanel.formatOrb(angleContact.maxOrb)).append("</span></div>");
        }
        // What landing on that particular angle means, once per angle actually involved.
        // Once, not once per row: two bodies on the same Ascendant is one fact about the
        // Ascendant repeated, and printing the paragraph twice would say so twice.
        java.util.Set<String> said = new java.util.LinkedHashSet<>();
        for (Synastry.AngleContact angleContact : list) {
            if (!said.add(angleContact.angle)) {
                continue;
            }
            String prose = InterpretationService.getInstance()
                .getAngleContact(angleContact.angle.toLowerCase());
            if (prose != null) {
                stringBuilder.append("<div style='color:#dddddd; font-size:11px; ")
                    .append("margin-top:6px;'>").append(prose).append("</div>");
            }
        }
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }

    /**
     * One direction of house overlays, grouped by the host's house.
     *
     * Grouped rather than listed body by body because the reading is about the house: six
     * of A's bodies in B's 12th is one fact, not six. The houses nothing lands in are named
     * on their own line for the same reason the empty case above is - so that an absent
     * house reads as empty rather than as missing.
     */
    private String houseOverlayHtml(String string, ChartFrame chartFrame, ChartFrame chartFrame2) {
        List<Synastry.Overlay> list = Synastry.houseOverlays(chartFrame, chartFrame2);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<h3 style='color:#add8e6; margin:0 0 4px 0;'>House overlays &middot; ")
            .append(string).append("</h3>");
        if (list.isEmpty()) {
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:10px;'>")
                .append("The visiting chart computed no bodies.</div>");
            return stringBuilder.toString();
        }
        StringBuilder[] stringBuilderArray = new StringBuilder[13];
        StringBuilder stringBuilder2 = new StringBuilder();
        for (Synastry.Overlay overlay : list) {
            StringBuilder stringBuilder3;
            if (overlay.house < 1 || overlay.house > 12) {
                stringBuilder3 = stringBuilder2;
            } else {
                if (stringBuilderArray[overlay.house] == null) {
                    stringBuilderArray[overlay.house] = new StringBuilder();
                }
                stringBuilder3 = stringBuilderArray[overlay.house];
            }
            if (stringBuilder3.length() > 0) {
                stringBuilder3.append(", ");
            }
            stringBuilder3.append("<span style='font-size:15px;'>")
                .append(SkymapPanel.namedGlyph(overlay.bodyIndex)).append("</span> ").append(overlay.body);
        }
        stringBuilder.append("<div style='margin-bottom:10px;'>");
        StringBuilder stringBuilder4 = new StringBuilder();
        for (int i = 1; i <= 12; ++i) {
            if (stringBuilderArray[i] == null) {
                if (stringBuilder4.length() > 0) {
                    stringBuilder4.append(", ");
                }
                stringBuilder4.append(this.romanNumeral(i));
                continue;
            }
            boolean bl = i == 1 || i == 4 || i == 7 || i == 10;
            stringBuilder.append("<div style='margin-bottom:3px; color:#cccccc; font-size:12px;'>")
                .append("<span style='color:").append(bl ? "#FFD166" : "#add8e6").append(";'>House ")
                .append(this.romanNumeral(i)).append(bl ? " (angular)" : "").append("</span> &nbsp;")
                .append(stringBuilderArray[i]);
            // The lead only. The full overlay paragraph is worth reading for one house and
            // unreadable twelve times over, and the lead is the sentence the format exists to
            // provide - see boldLead.
            String lead = SkymapPanel.boldLead(
                InterpretationService.getInstance().getOverlayHouse(i));
            if (lead != null) {
                stringBuilder.append("<div style='color:#9AA5B1; font-size:11px; ")
                    .append("margin-left:14px;'>").append(lead).append("</div>");
            }
            stringBuilder.append("</div>");
        }
        if (stringBuilder4.length() > 0) {
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px;'>Nothing of theirs in: ")
                .append(stringBuilder4).append(".</div>");
        }
        if (stringBuilder2.length() > 0) {
            // House 0 from Zodiac.houseOf means the receiving chart's cusp set is degenerate,
            // which is a fact about that chart and not about these bodies.
            stringBuilder.append("<div style='color:#ff8080; font-size:11px;'>No house could be "
                + "determined for: ").append(stringBuilder2)
                .append(" &mdash; the receiving chart's cusps are degenerate at its latitude.</div>");
        }
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }

    /** Degrees and arcminutes, for an orb. Truncated to the minute, as Zodiac.format is. */
    private static String formatOrb(double d) {
        int n = (int)Math.floor(d);
        int n2 = (int)Math.floor((d - (double)n) * 60.0);
        return n + "&deg;" + String.format("%02d", n2) + "'";
    }

    /**
     * The glyph to print in front of a body's NAME, which is not always its glyph.
     *
     * The registry's glyph field is documented as "the glyph drawn on the wheel, or the text
     * label for an angle", so for the MC and the IC it is the name itself - and this is the
     * only place in the panel that prints a glyph and a name together, which is why "IC IC"
     * appears here and nowhere else. "ASC Ascendant" and "Ns Nessus" are abbreviations in
     * front of a name and read correctly; only the exact repeat is wrong.
     *
     * <b>The span is still emitted when the glyph is dropped.</b> It renders as nothing, and
     * keeping the markup uniform means AspectGridCheck Part K can count rows by a single
     * delimiter instead of carrying a special case for two of the twenty-nine bodies.
     */
    static String namedGlyph(int index) {
        String glyph = BODY_GLYPHS[index];
        return glyph.equals(BODY_NAMES[index]) ? "" : glyph;
    }

    private static String frameKey(double d, double d2, double d3, int n) {
        // The zodiac is part of the chart: without it, switching to sidereal served the cached
        // tropical frame to every reading until something else changed.
        return d + "|" + d2 + "|" + d3 + "|" + n
            + "|z" + com.zodiacomputing.ourania.astro.Ephemeris.siderealMode();
    }

    private ChartFrame frameForCurrentChart(double d, double d2, double d3, int n) {
        ChartFrame chartFrame;
        // <b>The flag belongs in the key.</b> Without it, choosing "no time" hands back the
        // cached chart that still has its angles, and the rating appears to do nothing until
        // something else happens to invalidate the cache.
        String string = SkymapPanel.frameKey(d, d2, d3, n)
            + (this.baseTimeUnknown ? "|noon" : "");
        ChartFrame chartFrame2 = this.cachedFrame;
        if (chartFrame2 != null && string.equals(this.frameCacheKey)) {
            return chartFrame2;
        }
        // Chart A withholds its angles when its birth time was never known.
        this.cachedFrame = chartFrame = this.baseTimeUnknown
            ? ChartFrame.computeTimeUnknown(this.sw, d, d2, d3, n, false, 0.0)
            : ChartFrame.compute(this.sw, d, d2, d3, n, false, 0.0);
        this.frameCacheKey = string;
        return chartFrame;
    }

    /**
     * What stands in a span of the zodiac on the TRANSIT wheel; empty when none is shown.
     *
     * One method rather than three accessors on purpose. outerRing.lon/outerRing.speed/outerRing.valid are public and
     * a caller could read them directly, but three parallel arrays are three chances to
     * index one of them differently, and the interpretation panel has no business knowing
     * how the transit wheel stores itself.
     */
    public java.util.List<Occupants.Hit> transitOccupants(double start, double span) {
        if (!this.showTransitChart) {
            return java.util.Collections.emptyList();
        }
        return Occupants.inSpan(this.outerRing.lon, this.outerRing.valid, this.outerRing.speed, start, span);
    }

    /**
     * The chart on screen, in whatever harmonic is selected.
     *
     * <b>Wraps the radix accessor rather than replacing it</b> so the harmonic is applied in
     * one place to whatever the mode produced - natal, midpoint composite or Davison - and a
     * mode added later inherits it without anyone remembering to. At H1 this returns the
     * radix frame itself, unchanged and uncopied.
     */
    /**
     * The moment and place the Sky View draws: the sky's, or Chart A's birth sky.
     * {jdUt, latitude, longitude}; the sky's when there is no Chart A.
     */
    double[] skyMoment(boolean chartA) {
        if (chartA && this.innerIsBirthChart && this.natalRing.sd != null) {
            return new double[] {this.natalRing.sd.getJulDay(), this.natalRing.latitude, this.natalRing.longitude};
        }
        double jd = this.skyRing.sd != null ? this.skyRing.sd.getJulDay() : new SweDate().getJulDay();
        return new double[] {jd, this.skyRing.latitude, this.skyRing.longitude};
    }

    public ChartFrame getCurrentChart() {
        return com.zodiacomputing.ourania.astro.Harmonics.of(radixChart(), this.harmonic);
    }
    /**
     * The inner panel that paints the wheel, for the exporter to paint into an image.
     *
     * <b>Read-only.</b> The caller paints it into a BufferedImage or onto a printer graphics;
     * nothing here is changed by either. Exposed as a Component rather than the panel type so
     * a caller cannot reach into the wheel's state through it.
     */
    public java.awt.Component chartComponent() {
        return this.chartPanel;
    }

    /**
     * Every drawn position as tab-separated text, for a spreadsheet or a note.
     *
     * Built from the same arrays the wheel draws from, so what is copied is what is on screen
     * - including the reader's body selection. A copy that quietly held more than the chart
     * showed would be the same defect as a reading that describes a chart the wheel is not
     * drawing.
     */
    public String positionsText() {
        StringBuilder sb = new StringBuilder("Body\tLongitude\tSign\tDegree\tHouse\tRetrograde\n");
        for (int i = 0; i < BODY_COUNT; i++) {
            if (!this.natalRing.valid[i]) {
                continue;
            }
            double lon = this.natalRing.lon[i];
            int house = Zodiac.houseOf(lon, this.activeCusps);
            sb.append(BODY_NAMES[i]).append('\t')
              .append(String.format("%.4f", lon)).append('\t')
              .append(SkymapPanel.capitalise(Zodiac.SIGNS[Zodiac.signIndex(lon)])).append('\t')
              .append(String.format("%.2f", Zodiac.degreeInSign(lon))).append('\t')
              .append(house > 0 ? String.valueOf(house) : "").append('\t')
              .append(SkymapPanel.showsDirection(i) && this.natalRing.speed[i] < 0.0 ? "R" : "")
              .append('\n');
        }
        return sb.toString();
    }

    /**
     * The chart a reading describes.
     *
     * <b>Named, so a check can assert it rather than a copy of it</b> - the same reason
     * {@link #outerWheelShown} is a named function. Until 2026-08-30 {@code showReading} called
     * {@link #frameForCurrentChart}, which knows nothing about composites: it recomputes a
     * natal chart from the BASE date and place. So in either composite mode every reading tier
     * - Paragraph, Report, Synthesize, Predict - described <b>person A's natal chart</b> while
     * the wheel drew the composite, with nothing to say the two disagreed.
     *
     * <b>Part C of CompositeCheck was adjacent to this and missed it.</b> It calls
     * frameForCurrentChart, commented "What the reading panel does", and then asserts only that
     * the composite CACHE survives the call. What the reading went on to use was never checked.
     */
    ChartFrame frameForReading() {
        return this.radixChart();
    }

    /** The chart as cast, before any harmonic. The house frame the harmonic keeps. */
    public ChartFrame radixChart() {
        boolean composite = this.chartMode == ChartMode.COMPOSITE_MIDPOINT
            || this.chartMode == ChartMode.COMPOSITE_DAVISON;
        if (composite && this.natalRing.sd != null && this.outerRing.sd != null) {
            // Its own field and its own key. Sharing cachedFrame with frameForCurrentChart is
            // what made a composite silently become person A's natal chart - see the field.
            // The reference place is part of the key. It changes the cusps, so leaving it out
            // would serve the previous location's house frame from cache after the user
            // changed it - visible only as houses that quietly disagree with the setting.
            String key = this.chartMode + "|" + SkymapPanel.frameKey(this.natalRing.sd.getJulDay(),
                    this.natalRing.latitude, this.natalRing.longitude, this.houseSystem)
                + "|" + SkymapPanel.frameKey(this.outerRing.sd.getJulDay(),
                    this.outerRing.latitude, this.outerRing.longitude, this.houseSystem)
                + "|ref:" + this.compositeRefLat + "," + this.compositeRefLon;
            if (this.relationshipFrame != null && key.equals(this.relationshipCacheKey)) {
                return this.relationshipFrame;
            }
            ChartFrame a = ChartFrame.compute(this.sw, this.natalRing.sd.getJulDay(),
                this.natalRing.latitude, this.natalRing.longitude, this.houseSystem, false, 0.0);
            ChartFrame b = ChartFrame.compute(this.sw, this.outerRing.sd.getJulDay(),
                this.outerRing.latitude, this.outerRing.longitude, this.houseSystem, false, 0.0);
            this.relationshipFrame = this.chartMode == ChartMode.COMPOSITE_MIDPOINT
                ? (Double.isNaN(this.compositeRefLat)
                    ? ChartFrame.computeMidpointComposite(this.sw, a, b)
                    : ChartFrame.computeMidpointComposite(this.sw, a, b,
                        this.compositeRefLat, this.compositeRefLon))
                : ChartFrame.computeDavisonComposite(this.sw, a, b);
            this.relationshipCacheKey = key;
            return this.relationshipFrame;
        }
        // Always through frameForCurrentChart, which compares the cache KEY. The previous
        // form returned this.cachedFrame whenever it was non-null, so after the date or place
        // changed it served the old chart - harmless while only the wheel called this, and not
        // harmless now that readings do.
        if (this.natalRing.sd != null) {
            return this.frameForCurrentChart(this.natalRing.sd.getJulDay(), this.natalRing.latitude,
                this.natalRing.longitude, this.houseSystem);
        }
        return this.cachedFrame;
    }

    /**
     * The readings, addressable by name from the sidebar's Readings section.
     *
     * <b>By name rather than by exposing {@link ReadingTier}</b>, which is private and stays
     * that way: the sidebar has no business knowing the wheel's internal tiers, and an enum
     * widened to public for one caller is how a private detail becomes an API nobody meant to
     * publish. Calendar is not a tier at all - it has its own path - which is precisely the
     * sort of thing a shared enum would have hidden.
     */
    public void runReading(String name) {
        if ("CALENDAR".equals(name)) {
            this.showAnnualCalendar();
            return;
        }
        if ("SNAPSHOT".equals(name)) {
            this.showReading(ReadingTier.PARAGRAPH);
        } else if ("REPORT".equals(name)) {
            this.showReading(ReadingTier.REPORT);
        } else if ("SYNTHESIZE".equals(name)) {
            this.showReading(ReadingTier.SYNTHESIZE);
        } else if ("TIMELINE".equals(name)) {
            this.showReading(ReadingTier.TIMELINE);
        }
    }

    /**
     * Doors onto three engines that had none. See {@link ChartTables}.
     *
     * Midpoints, Rulership and HarmonicResonance were all implemented, all covered by suites,
     * and all unreachable: zero references from this package on 2026-09-01. Casting the frame
     * takes long enough to stutter the wheel, so it happens on a worker exactly as a reading
     * does, and only the finished HTML crosses back to the event thread.
     */
    public void showTable(final String kind) {
        if (this.sw == null || this.natalRing.sd == null || this.window == null) {
            return;
        }
        final double jd = this.natalRing.sd.getJulDay();
        final double lat = this.natalRing.latitude;
        final double lon = this.natalRing.longitude;
        final char hsys = this.houseSystem;
        new javax.swing.SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                ChartFrame f = ChartFrame.compute(SkymapPanel.this.sw, jd, lat, lon, hsys, false, 0.0);
                if ("MIDPOINTS".equals(kind)) {
                    return ChartTables.midpoints(f);
                }
                if ("DISPOSITORS".equals(kind)) {
                    return ChartTables.dispositors(f);
                }
                if ("RESONANCE".equals(kind)) {
                    return ChartTables.resonance(f);
                }
                if ("DRACONIC".equals(kind)) {
                    return ChartTables.draconic(f);
                }
                if ("DECLINATIONS".equals(kind)) {
                    return ChartTables.declinations(f);
                }
                if ("ANTISCIA".equals(kind)) {
                    return ChartTables.antiscia(f);
                }
                if ("FIXED_STARS".equals(kind)) {
                    return ChartTables.fixedStars(f, SkymapPanel.this.sw);
                }
                // These two are read against a moment, not just a chart, so they take the
                // birth instant and today. The transit clock drives them when it is set, so
                // stepping time moves the progressed chart with it.
                double now = SkymapPanel.this.showTransitChart && SkymapPanel.this.outerRing.sd != null
                        ? SkymapPanel.this.outerRing.sd.getJulDay()
                        : new SweDate().getJulDay();
                if ("PROGRESSED".equals(kind)) {
                    return ChartTables.progressed(f, SkymapPanel.this.sw, jd, now);
                }
                if ("SOLARARC".equals(kind)) {
                    return ChartTables.solarArc(f, SkymapPanel.this.sw, jd, now);
                }
                // A year either side of the clock: enough for one solar return, the lunar
                // returns inside it, and the annual three. Cast for the chart's own place -
                // relocation is a claim the user has to make, and there is no field for it
                // yet, so the table says which place it used rather than assuming.
                if ("RETURNS".equals(kind)) {
                    return ChartTables.returns(f, SkymapPanel.this.sw, jd,
                        now, now + 365.2422, lat, lon, hsys);
                }
                return "";
            }

            @Override
            protected void done() {
                try {
                    String html = get();
                    if (html != null && !html.isEmpty()) {
                        SkymapPanel.this.window.showInterpretationHtml(html);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void showReading(final ReadingTier readingTier) {
        if (this.sw == null || this.natalRing.sd == null || this.window == null || readingTier == ReadingTier.NONE) {
            return;
        }
        this.readingTier = readingTier;
        final double d = this.natalRing.sd.getJulDay();
        final double d2 = this.natalRing.latitude;
        final double d3 = this.natalRing.longitude;
        final char c = this.houseSystem;
        final String string = this.baseLocationName;
        final String string2 = this.natalRing.time == null ? "" : this.natalRing.time.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm z"));
        final boolean bl = this.showTransitChart && this.outerRing.sd != null;
        final double d4 = bl ? this.outerRing.sd.getJulDay() : Double.NaN;
        final double d5 = this.outerRing.latitude;
        final double d6 = this.outerRing.longitude;
        new SwingWorker<String, Void>(){

            @Override
            protected String doInBackground() {
                Object object;
                ChartFrame chartFrame = SkymapPanel.this.frameForReading();
                Gestalt.Result result = Gestalt.compute(chartFrame);
                List<BodyScore.Vector> list = BodyScore.rank(chartFrame, result);
                Themes.Result result2 = Themes.extract(chartFrame, result, list);
                ChartFrame chartFrame2 = null;
                Profection profection = null;
                List<Transits.Hit> list2 = null;
                YearScan yearScan = null;
                List<Convergence.Target> list3 = null;
                if (bl) {
                    chartFrame2 = ChartFrame.compute(SkymapPanel.this.sw, d4, d5, d6, c, false, 0.0);
                    profection = Profection.at(d, d4, chartFrame.asc);
                    object = chartFrame.body("Sun");
                    if (object != null && ((ChartFrame.Body)object).ok) {
                        double d7 = Profection.solarReturnJd(SkymapPanel.this.sw, d, ((ChartFrame.Body)object).lon, profection.age);
                        profection.computeSubPeriods(d4, d7);
                    }
                    list2 = Transits.toNatal(chartFrame, chartFrame2, list, profection.lord);
                    if (readingTier == ReadingTier.REPORT || readingTier == ReadingTier.SYNTHESIZE || readingTier == ReadingTier.TIMELINE) {
                        yearScan = SkymapPanel.scanProfectionYear(chartFrame, d, d4, profection, list, d2, d3, c);
                        // Releasing gates the ranking rather than voting in it, which is what
                        // finally puts it inside a reading. The chart carries the lots already,
                        // so the gate costs one release walk and no ephemeris. d4 is the moment.
                        Convergence.Gate gate = Convergence.Gate.at(
                            d, chartFrame.lotOfFortune, chartFrame.lotOfSpirit, d4);
                        list3 = Convergence.collect(profection, yearScan.perfections,
                            yearScan.events, yearScan.arcs, yearScan.progressions,
                            yearScan.returns, gate);
                    }
                }
                if (readingTier == ReadingTier.REPORT) {
                    object = Snapshot.report(chartFrame, result, list, result2) + Snapshot.topicLayer(Topics.analyse(chartFrame, list));
                    if (bl) {
                        // The frame and the moment carry the releasing section, which was
                        // computed to four levels and appeared in no reading until 2026-09-02.
                        object = (String)object + Snapshot.timeLayer(profection, list, list2,
                            yearScan.events, list3, chartFrame, d4);   // d4 is the moment;
                        // d2 is natalRing.latitude in this decompiled scope, and passing it here
                        // would have produced an empty section rather than an error.
                    }
                    return (String)object;
                }
                
if (readingTier == ReadingTier.TIMELINE) {
    return com.zodiacomputing.ourania.gui.TimelinePredictor.generate(chartFrame, chartFrame2);
}
if (readingTier == ReadingTier.SYNTHESIZE) {
                    // The mode decides which prose the reading speaks. Captured from the
                    // panel rather than sniffed from the frame: syntheticMoment is true for
                    // a midpoint composite and false for a Davison, so it cannot answer this.
                    return NarrativeSynthesizer.generateReport(chartFrame, result, list, result2, chartFrame2, profection, list2, yearScan, list3, bl,
                        SkymapPanel.this.isRelationshipChart());
                }
                object = PlainSnapshot.generate(chartFrame, result, list);
                if (bl) {
                    return (String)object + " " + Snapshot.timeSentence(profection, list2);
                }
                return (String)object;
            }

            @Override
            protected void done() {
                try {
                    if (readingTier == ReadingTier.REPORT) {
                        SkymapPanel.this.window.showReport(string2, string, (String)this.get());
                    } else if (readingTier == ReadingTier.SYNTHESIZE || readingTier == ReadingTier.TIMELINE) {
                        SkymapPanel.this.window.showSynthesis((String)this.get());
                    } else {
                        SkymapPanel.this.window.showSnapshot(string2, string, (String)this.get());
                    }
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }.execute();
    }

    private void refreshReadingIfShown() {
        if (this.readingTier == ReadingTier.NONE || this.isPlaying) {
            return;
        }
        if (this.refreshDebounce != null && this.refreshDebounce.isRunning()) {
            this.refreshDebounce.restart();
            return;
        }
        this.refreshDebounce = new Timer(400, actionEvent -> {
            this.refreshDebounce.stop();
            if (!this.isPlaying && this.readingTier != ReadingTier.NONE) {
                this.showReading(this.readingTier);
            }
        });
        this.refreshDebounce.setRepeats(false);
        this.refreshDebounce.start();
    }

    /**
     * Puts the moment being drawn onto the bottom drawer's handle.
     *
     * Called on every chart update, so stepping time with the transport buttons moves the
     * readout whether the drawer is open or shut - which is the point of putting it there.
     */
    private void refreshTimeReadout() {
        if (this.timeDrawer == null) {
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy  HH:mm");
        StringBuilder out = new StringBuilder();
        // <b>The inner wheel is named too.</b> It printed a bare timestamp, so a reader had
        // no way to tell a birth chart from the sky standing in for one - which is exactly
        // the confusion a cold open produced.
        ZonedDateTime inner = this.innerIsBirthChart ? this.natalRing.time : this.skyRing.time;
        if (inner != null) {
            // Whoever is actually on the inner wheel. This said "Chart A" for anything that
            // was not the sky, which was true while Chart A was the only chart that could be
            // cast and became a lie the moment Chart B could be promoted into that place.
            out.append(this.anchorSubject == null ? "Sky: " : this.anchorSubject.label + ": ")
                .append(inner.format(fmt));
        }
        out.append(String.format("   %.2f, %.2f",
            this.innerIsBirthChart ? this.natalRing.latitude : this.skyRing.latitude,
            this.innerIsBirthChart ? this.natalRing.longitude : this.skyRing.longitude));
        // The outer wheel is a second moment, and it is the one the transport usually moves,
        // so it is named rather than left for the reader to infer from a changing number.
        // Named for what it is rather than for where it sits: the outer wheel is Chart B in
        // a synastry and the sky everywhere else, and calling both of them "sky" is how a
        // reader comes to believe a birth chart is this moment.
        ZonedDateTime outer = this.isSynastryChart() ? this.outerRing.time : this.skyRing.time;
        // <b>Each ring named by what is on it, and the moment it was cast at.</b> This chose
        // between two labels on isSynastryChart and printed the entered field beside them -
        // which said "Sky" over the progressed ring, printed Chart B's birth moment for a ring
        // showing this instant, and dropped the sky row entirely from any tri-wheel that was
        // not a synastry. Three faults, one boolean, and ringWord already existed.
        if (this.showTransitChart) {
            String when = this.outerCastLabel();
            if (when.isEmpty() && outer != null) {
                when = outer.format(fmt);
            }
            out.append("      ").append(capitalise(this.ringWord(WHEEL_OUTER)))
               .append(": ").append(when);
        }
        if (this.triRingDrawn() && this.skyRing.time != null) {
            out.append("      Sky: ").append(this.skyRing.time.format(fmt));
        }
        // <b>The zodiac, when it is not the one a reader assumes.</b> Switched to sidereal in
        // Settings and forgotten, a chart shows the Sun a sign early with nothing on screen to
        // say why - the gap left open when the sidereal zodiac shipped. Tropical says nothing,
        // because it is what every other chart the reader has seen was drawn in.
        if (com.zodiacomputing.ourania.astro.Ephemeris.sidereal()) {
            out.append("      ").append(com.zodiacomputing.ourania.astro.Ephemeris.zodiacLabel());
        }
        this.timeDrawer.setLabel(out.toString());
    }

    private JPanel createControlPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, 1));
        jPanel.setBackground(Color.BLACK);
        // <b>WrapLayout, not FlowLayout.</b> Both rows outgrow a 1024-wide window - eleven
        // buttons on one, seven labelled dropdowns on the other - and a plain FlowLayout wraps
        // them while still reporting one row's height to the BoxLayout above. The parent then
        // reserves that one row and the second is drawn outside the panel's bounds, which is
        // how House System, the last control added, came to be cut off the bottom with nothing
        // logged. See Widgets.WrapLayout.
        JPanel jPanel2 = new JPanel(new Widgets.WrapLayout(1, 5, 5));
        jPanel2.setBackground(Color.BLACK);
        JPanel jPanel3 = new JPanel(new Widgets.WrapLayout(1, 8, 4));
        jPanel3.setBackground(Color.BLACK);
        // <b>The strip keeps time, and nothing else.</b> It carried seven labelled dropdowns -
        // harmonic, animate target, aspect filter, house alignment, pin and house system -
        // beside the transport buttons, which is a settings panel laid across the bottom of
        // the chart. They are chart settings, so they live on the Settings screen now and the
        // strip is left with the one control that belongs beside a play button: the step.
        this.chartControls = new JPanel(new Widgets.WrapLayout(0, 10, 6));
        this.chartControls.setBackground(Theme.BG);
        this.chartControls.setMaximumSize(
            new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        // <b>Without this the WrapLayout above can never wrap, and that is not obvious.</b>
        // BoxLayout sizes a child along the axis it does not manage using the child's MAXIMUM
        // size, which for a JPanel defaults to its preferred size - so each row was made as
        // wide as one unwrapped line needed, overflowed the window, and was clipped at the
        // edge. The layout was never given a width narrow enough to wrap against. Releasing
        // the maximum width lets the row be sized to the window, which is the input WrapLayout
        // measures. Measured at 900px wide: without it the strip stays 60px and House System
        // is off the edge; with it the strip grows a row and every control is on screen.
        jPanel2.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        jPanel3.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        JButton jButton = new JButton("Fast <<");
        JButton jButton2 = new JButton("< Slow");
        JButton jButton3 = new JButton("Play / Pause");
        JButton jButton4 = new JButton("Slow >");
        JButton jButton5 = new JButton("Fast >>");
        JButton jButton6 = new JButton("Now");
        jButton.addActionListener(actionEvent -> {
            this.animationDirection = -1;
            this.animationTimer.setDelay(50);
            this.isPlaying = true;
        });
        jButton2.addActionListener(actionEvent -> {
            this.animationDirection = -1;
            this.animationTimer.setDelay(500);
            this.isPlaying = true;
        });
        jButton4.addActionListener(actionEvent -> {
            this.animationDirection = 1;
            this.animationTimer.setDelay(500);
            this.isPlaying = true;
        });
        jButton5.addActionListener(actionEvent -> {
            this.animationDirection = 1;
            this.animationTimer.setDelay(50);
            this.isPlaying = true;
        });
        jButton3.addActionListener(actionEvent -> {
            this.isPlaying = !this.isPlaying;
        });
        jButton6.addActionListener(actionEvent -> {
            boolean bl;
            this.isPlaying = false;
            boolean bl2 = this.animateTarget.equals("Natal") || this.animateTarget.equals("Both") || !this.showTransitChart;
            boolean bl3 = bl = (this.animateTarget.equals("Transit") || this.animateTarget.equals("Both")) && this.showTransitChart;
            if (bl2) {
                this.natalRing.time = ZonedDateTime.now(ZoneId.of(this.baseTimeZoneId));
            }
            if (bl) {
                this.skyRing.time = ZonedDateTime.now(ZoneId.of(this.skyTimeZoneId));
            }
            this.updateChartData();
            this.chartPanel.repaint();
        });
        JButton jButton7 = new JButton("Snapshot");
        jButton7.setToolTipText("Read the natal chart as it currently stands");
        jButton7.addActionListener(actionEvent -> this.showReading(ReadingTier.PARAGRAPH));
        JButton jButton8 = new JButton("Report");
        jButton8.setToolTipText("The full reading: shape, weights, repeated themes, tensions");
        jButton8.addActionListener(actionEvent -> this.showReading(ReadingTier.REPORT));
        JButton jButton9 = new JButton("Synthesize");
        jButton9.setToolTipText("Generate a narrative reading based on the chart");
        jButton9.addActionListener(actionEvent -> this.showReading(ReadingTier.SYNTHESIZE));
        JButton jButton9_2 = new JButton("Predict");
        jButton9_2.setToolTipText("Generate a timeline prediction based on the chart");
        jButton9_2.addActionListener(actionEvent -> this.showReading(ReadingTier.TIMELINE));
        JButton jButton10 = new JButton("Calendar");
        jButton10.setToolTipText("Ingresses, stations, lunations and mundane aspects for the year");
        jButton10.addActionListener(actionEvent -> this.showAnnualCalendar());
        // <b>Colour by role, not by button.</b> These were eleven hand-picked colours that
        // encoded nothing - see Widgets.Role. Transport is quiet because it is used
        // constantly and says little; Play/Pause is the control the row is built around; the
        // five readings are peers and now look like peers. "Now" left red for months, which
        // reads as destructive when jumping to the present is the safest thing here.
        // <b>Transport stays on the strip; the readings move into a drawer.</b> Eleven buttons
        // beside seven dropdowns is what put the control area over a 1024-wide window in the
        // first place, and the five readings are the ones a reader presses occasionally rather
        // than constantly. Transport is the row's working set and stays one press away.
        JButton[] jButtonArray = new JButton[]{jButton, jButton2, jButton3, jButton4, jButton5, jButton6};
        Widgets.Role[] buttonRoles = {
            Widgets.Role.TRANSPORT,  // Fast <<
            Widgets.Role.TRANSPORT,  // < Slow
            Widgets.Role.PRIMARY,    // Play / Pause
            Widgets.Role.TRANSPORT,  // Slow >
            Widgets.Role.TRANSPORT,  // Fast >>
            Widgets.Role.TRANSPORT,  // Now
        };
        for (int bi = 0; bi < jButtonArray.length; bi++) {
            Widgets.styleButton(jButtonArray[bi], buttonRoles[bi]);
            jPanel2.add(jButtonArray[bi]);
        }
        // <b>A bar per chart, on a row of its own.</b> One Scrub slider moved whatever the
        // transport moves; David asked for "different scrub bars that control different charts".
        // Each is shown only while its chart is on the wheel - see refreshScrubBars.
        scrubRow = new JPanel(new Widgets.WrapLayout(1, 14, 2));
        scrubRow.setBackground(Color.BLACK);
        scrubRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        JLabel scrubLabel = new JLabel("Scrub:");
        scrubLabel.setForeground(Color.WHITE);
        scrubRow.add(scrubLabel);
        for (ScrubTarget t : new ScrubTarget[] {ScrubTarget.CHART_A, ScrubTarget.CHART_B, ScrubTarget.SKY}) {
            scrubRow.add(this.scrubBar(t));
        }
        this.scrubSlider = this.scrubSliders.get(ScrubTarget.SKY);

        // <b>Readings, Tables and Tools have moved to the sidebar.</b> They were popup
        // menus here, which grouped the controls but stayed menus: they floated over the
        // chart, shut on the next click, and could only hold rows of buttons. They are now
        // accordion sections inside the sidebar's drawer, which is where the placements and
        // the aspect grid live too - see MainMenuPanel. The strip keeps transport only, which
        // is the working set: the controls pressed constantly rather than occasionally.
        Object[] objectArray = new String[]{"Real Time", "1 Minute", "1 Hour", "1 Day", "1 Week", "1 Month", "1 Year"};
        JComboBox<Object> jComboBox = new JComboBox<Object>(objectArray);
        // Seeded FROM the field, not from a literal. A literal here is a second statement of
        // the default that no mechanism keeps in step with the first.
        jComboBox.setSelectedItem(this.stepAmount);
        jComboBox.addActionListener(actionEvent -> {
            this.stepAmount = (String)jComboBox.getSelectedItem();
        });
        SkymapPanel.styleCompactCombo((JComboBox<String>)(Object)jComboBox);
        JLabel jLabel = new JLabel("Step:");
        jLabel.setForeground(Color.WHITE);
        jPanel3.add(jLabel);
        jPanel3.add(jComboBox);

        // Harmonic 1-360. H1 is the radix, so the control starts where the chart already is
        // and the list is built from the Harmonics bounds rather than from literals here.
        // <b>Strings, not Integers.</b> Widgets.styleCombo measures its widest entry with
        // FontMetrics.stringWidth and so declares JComboBox<String>; handing it a
        // JComboBox<Integer> compiles cleanly - the generic is erased - and then throws
        // ClassCastException inside the SkymapPanel constructor, which takes the whole app
        // down at startup. It did exactly that on 2026-08-25 and javac was green throughout.
        String[] harmonics =
            new String[com.zodiacomputing.ourania.astro.Harmonics.MAX
                       - com.zodiacomputing.ourania.astro.Harmonics.MIN + 1];
        for (int h = 0; h < harmonics.length; h++) {
            harmonics[h] = String.valueOf(com.zodiacomputing.ourania.astro.Harmonics.MIN + h);
        }
        JComboBox<String> harmonicCombo = new JComboBox<String>(harmonics);
        // Seeded FROM the field, like the Step combo beside it, and for the reason recorded
        // there: a literal is a second statement of the default that nothing keeps in step.
        harmonicCombo.setSelectedItem(String.valueOf(this.harmonic));
        harmonicCombo.setToolTipText("H1 is the chart as cast. Higher harmonics multiply every "
            + "longitude by N, so an Nth-harmonic aspect becomes a conjunction - H5 shows "
            + "quintiles, H7 septiles. Cusps and angles stay as cast.");
        harmonicCombo.addActionListener(actionEvent -> {
            int picked = 1;
            try {
                picked = Integer.parseInt(String.valueOf(harmonicCombo.getSelectedItem()));
            } catch (NumberFormatException ignored) {
                // Cannot happen from this list, and falling back to the radix beats throwing
                // out of a listener on the event thread.
            }
            this.harmonic = com.zodiacomputing.ourania.astro.Harmonics.clamp(picked);
            // The frame cache holds a radix chart, and the harmonic is applied on the way out,
            // so nothing needs invalidating - but the arrays are mapped in place and must be
            // rebuilt from the ephemeris rather than re-multiplied.
            this.updateChartData();
            this.repaint();
        });
        SkymapPanel.styleCompactCombo(harmonicCombo);
        JLabel harmonicLabel = compactLabel("H:", "Harmonic");
        harmonicLabel.setForeground(Color.WHITE);
        this.chartControls.add(harmonicLabel);
        this.chartControls.add(harmonicCombo);
        String[] stringArray = new String[]{"Natal", "Transit", "Both"};
        this.animateCombo = new JComboBox<String>(stringArray);
        this.animateCombo.setSelectedItem(this.animateTarget);
        this.animateCombo.addActionListener(actionEvent -> {
            this.animateTarget = (String)this.animateCombo.getSelectedItem();
        });
        this.animateCombo.setToolTipText("Which chart advances when the animation runs");
        SkymapPanel.styleCompactCombo(this.animateCombo);
        JLabel jLabel2 = compactLabel("Anim:", "Animate");
        jLabel2.setForeground(Color.WHITE);
        this.chartControls.add(jLabel2);
        this.chartControls.add(this.animateCombo);
        String[] stringArray2 = new String[]{"Natal-Natal", "Transit-Natal", "Both"};
        this.filterCombo = new JComboBox<String>(stringArray2);
        this.filterCombo.setSelectedItem(this.aspectFilter);
        this.filterCombo.addActionListener(actionEvent -> {
            this.aspectFilter = (String)this.filterCombo.getSelectedItem();
            this.chartPanel.repaint();
        });
        this.filterCombo.setToolTipText("Which aspect lines are drawn: natal-to-natal, transit-to-natal, or both");
        SkymapPanel.styleCompactCombo(this.filterCombo);
        JLabel jLabel3 = new JLabel("Aspects:");
        jLabel3.setForeground(Color.WHITE);
        this.chartControls.add(jLabel3);
        this.chartControls.add(this.filterCombo);
        String[] stringArray3 = new String[]{"Natal", "Transit", "Both"};
        this.alignCombo = new JComboBox<String>(stringArray3);
        this.alignCombo.setSelectedItem(this.houseAlignment);
        this.alignCombo.addActionListener(actionEvent -> {
            this.houseAlignment = (String)this.alignCombo.getSelectedItem();
            this.updateChartData();
            this.chartPanel.repaint();
        });
        this.alignCombo.setToolTipText("Which house ring is drawn - natal (inner wheel) or transit (outer wheel)");
        SkymapPanel.styleCompactCombo(this.alignCombo);
        this.alignCombo.setEnabled(this.showTransitChart);
        JLabel jLabel4 = compactLabel("Align:", "Align Houses");
        jLabel4.setForeground(Color.WHITE);
        this.chartControls.add(jLabel4);
        this.chartControls.add(this.alignCombo);
        String[] stringArray4 = new String[]{"Aries", "Natal Asc", "Transit Asc"};
        this.pinCombo = new JComboBox<String>(stringArray4);
        this.pinCombo.setSelectedItem(this.wheelPin);
        this.pinCombo.addActionListener(actionEvent -> {
            String string = (String)this.pinCombo.getSelectedItem();
            if ("Transit Asc".equals(string) && !this.showTransitChart) {
                this.pinCombo.setSelectedItem(this.wheelPin);
                JOptionPane.showMessageDialog(this, "\"Transit Asc\" pins the wheel to the transit Ascendant,\nwhich is the outer wheel. Enable the transit bi-wheel in\nChart Setup to use it.", "No transit wheel", 1);
                return;
            }
            this.wheelPin = string;
            this.updateChartData();
            this.chartPanel.repaint();
        });
        this.pinCombo.setToolTipText("What stays still on screen. Natal Asc = inner wheel Ascendant, Transit Asc = outer wheel Ascendant");
        SkymapPanel.styleCompactCombo(this.pinCombo);
        JLabel jLabel5 = new JLabel("Pin:");
        jLabel5.setForeground(Color.WHITE);
        this.chartControls.add(jLabel5);
        this.chartControls.add(this.pinCombo);
        String[] stringArray5 = new String[]{"Placidus", "Koch", "Equal", "Whole Sign", "Campanus", "Regiomontanus"};
        JComboBox<String> jComboBox2 = new JComboBox<String>(stringArray5);
        jComboBox2.setSelectedItem(this.currentHouseSystemName);
        jComboBox2.addActionListener(actionEvent -> {
            String string;
            this.currentHouseSystemName = string = (String)jComboBox2.getSelectedItem();
            switch (string) {
                case "Placidus": {
                    this.houseSystem = (char)80;
                    break;
                }
                case "Koch": {
                    this.houseSystem = (char)75;
                    break;
                }
                case "Equal": {
                    this.houseSystem = (char)69;
                    break;
                }
                case "Whole Sign": {
                    this.houseSystem = (char)87;
                    break;
                }
                case "Campanus": {
                    this.houseSystem = (char)67;
                    break;
                }
                case "Regiomontanus": {
                    this.houseSystem = (char)82;
                }
            }
            Settings.set("default.house.system", string);
            this.updateChartData();
            this.chartPanel.repaint();
            // Placidus can fail where Whole Sign cannot, so the notice is a fact about this choice.
            this.refreshPrecisionNotice();
        });
        SkymapPanel.styleCompactCombo(jComboBox2);
        JLabel jLabel6 = compactLabel("Houses:", "House System");
        jLabel6.setForeground(Color.WHITE);
        this.chartControls.add(jLabel6);
        this.chartControls.add(jComboBox2);
        // <b>The rings, above the controls that move time.</b> Which rings are open is a
        // question about what the chart IS; the row below is about when it is. Putting them
        // together in one strip was what made the mode chooser feel like a settings screen.
        this.ringBar = new RingBar(this.window);
        JPanel ringRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        ringRow.setBackground(Color.BLACK);
        ringRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30));
        ringRow.add(this.ringBar);
        jPanel.add(ringRow);
        jPanel.add(jPanel2);
        jPanel.add(scrubRow);
        jPanel.add(jPanel3);

        // <b>The rows have to be re-measured when the width changes, and nothing does that on
        // its own.</b> A WrapLayout reports how tall it will be for the width it currently
        // has; the enclosing BoxLayout asks once, caches, and does not ask again when the
        // window is resized - so widening never reclaims the second row and narrowing clips
        // the last control. invalidate() discards the cached answer; revalidate() schedules
        // the pass that asks again. Measured at 760px: without this the strip stays 60px with
        // House System off the edge, with it the strip becomes 79px and wraps.
        jPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                jPanel2.invalidate();
                scrubRow.invalidate();
                jPanel3.invalidate();
                jPanel.revalidate();
            }
        });
        return jPanel;
    }

    private void handleChartClick(int n, int n2) {
        // On the globe the only targets are the bodies; the bands the click dispatcher below
        // resolves by radius are not drawn, so resolving against them would open a card for a
        // ring the reader cannot see.
        if (this.globeMode) {
            int hit = GlobeRenderer.bodyAt(this.globe, this.chartPanel.getWidth(),
                this.chartPanel.getHeight(), this, n, n2);
            if (hit < 0) {
                // Nothing under the cursor but the scaffolding: a house number opens its
                // house, a tick opens its degree. Same order the hover uses, so what lights
                // under the cursor is what opens when it is clicked.
                int[] target = this.globeTargetAt(n, n2);
                if (this.window == null) {
                    return;
                }
                switch (target[0]) {
                    case GLOBE_HOUSE:
                        this.window.showInterpretationForHouse(target[1]);
                        break;
                    case GLOBE_DECAN:
                        this.window.showInterpretationForDecan(SIGN_NAMES[target[1] / 3],
                            target[1] % 3 + 1);
                        break;
                    case GLOBE_DEGREE:
                        this.window.showInterpretationForSabianSymbol(
                            SIGN_NAMES[(target[1] / 30) % 12], target[1] % 30 + 1);
                        break;
                    case GLOBE_MANSION:
                        this.window.showInterpretationForMansion(target[1]);
                        break;
                    case GLOBE_SIGN:
                        this.window.showInterpretationForSign(SIGN_NAMES[target[1]]);
                        break;
                    case GLOBE_BOUND:
                        this.window.showInterpretationForBound(target[1] + 0.5);
                        break;
                    default:
                        break;
                }
                return;
            }
            boolean outer = (hit & TRANSIT_BIT) != 0;
            int i = hit & ~TRANSIT_BIT;
            double[] lon = outer ? this.outerRing.lon : this.natalRing.lon;
            double[] spd = outer ? this.outerRing.speed : this.natalRing.speed;
            if (i >= lon.length) {
                return;
            }
            if (Bodies.at(i).isAngle()) {
                this.showAngleAt(i, lon[i], this.angleRoleFor(false, outer));
            } else if (this.window != null) {
                // The same card the flat wheel opens, built by the same method. A second way
                // of describing a body would be a second thing to keep in step with the
                // interpretation panel, and it would drift the first time either changed.
                //
                // <b>Pinned, the way the flat wheel pins.</b> This called setFocus, which does
                // not pin - so the focus stayed loose and the first mouse movement after the
                // click, which is the movement toward the card that just opened, put it back
                // on nothing. The glyph unlit itself on the way to reading about it, and every
                // expander on the card went dead, because the card redraws from the focus and
                // there was no longer a focus to redraw from.
                if (this.pinFocus(hit) && this.chartPanel != null) {
                    this.chartPanel.repaint();
                }
                this.window.showSelection(this.selectionHtml(i, lon[i], spd[i], outer));
            }
            return;
        }
        // <b>Pin first, before the branches below decide what was clicked.</b> This method has
        // several exits - angle, transit body, natal body, aspect line, empty space - and
        // pinning inside one of them would leave the others silently unpinned. Asked through
        // bodyAt, which is the same hit test the hover card and the highlight use.
        if (this.chartPanel != null && this.pinFocus(this.bodyAt(n, n2))) {
            this.chartPanel.repaint();
        }
        if (this.window != null && this.focusBody >= 0) {
            double[] lon = this.focusTransit ? this.outerRing.lon : this.natalRing.lon;
            double[] spd = this.focusTransit ? this.outerRing.speed : this.natalRing.speed;
            if (this.focusBody < lon.length) {
                // The same card the tooltip builds - one description of a body, shown in two
                // places rather than written twice.
                this.window.showSelection(
                    this.selectionHtml(this.focusBody, lon[this.focusBody], spd[this.focusBody],
                        this.focusTransit));
                // <b>The natal-body exit, which this method's own list of exits promised and
                // did not have.</b> Everything below resolves by radius, and the last two
                // branches are "inside the body ring is a house, outside it is a sign" - so a
                // body click fell through, showed its card, and had it overwritten by the
                // house underneath the glyph. Invisible at the default ring, where the glyphs
                // sit near the sign band; total with bodies set to the centre, which puts
                // every glyph inside the house branch. Reported as "I select a body and all I
                // get is a sign or a house".
                //
                // Two kinds of hit are deliberately NOT returned here, because the branches
                // below give them more than the card does and are reached by the same test:
                // angles, which showAngleAt reads in full, and anything on the transit or sky
                // ring, whose branches list its contacts back to Chart A and Chart B. Guarding
                // only on isAngle cost exactly those two sky assertions in AspectGridCheck.
                if (!this.focusTransit && !Bodies.at(this.focusBody).isAngle()) {
                    // <b>And the full reading, not just the card.</b> The card is a summary -
                    // degree, house, decan, one line of Sabian - and reaching the placement's
                    // actual meaning took four more steps: Interpretations, then the index,
                    // then Planets, then the body you had already clicked. The placements list
                    // has had a one-click path to this reading all along, through a "base_N"
                    // href; the wheel simply never used it. Same call, same href format, so
                    // the two doors cannot drift apart.
                    // The reading is in the card now, so the panel is not opened as well.
                    // It was one click producing two answers in two places, which is the
                    // redundancy this replaced.
                    return;
                }
            }
        }
        double d;
        double d2;
        double d3;
        int n3;
        int n4;
        if (this.sw == null || this.natalRing.sd == null || this.window == null) {
            return;
        }
        // One geometry, shared with the painter and both hit tests. The machine-named locals
        // are kept and bound to it rather than renamed: this is decompiler output and each is
        // read a dozen times below, so rebinding is the change that cannot go wrong.
        Geometry g = this.geometry();
        if (g == null) {
            return;
        }
        int n7 = g.cx;
        int n8 = g.cy;
        double d4 = n - n7;
        double d5 = n2 - n8;
        double d6 = Math.sqrt(d4 * d4 + d5 * d5);
        double d7 = Math.toDegrees(Math.atan2(d5, d4));
        if (d7 < 0.0) {
            d7 += 360.0;
        }
        // The same chain the painter uses, from the same method. This was a second inline copy
        // until 2026-08-23 - the hit test computing the rings independently of the code that
        // draws them, which is how a glyph you can see becomes a glyph you cannot click.
        int[] rings = g.rings;
        int n9 = n4 = rings[RING_OUTER];
        int nTri = rings[RING_TRI];
        int n10 = rings[RING_TRANSIT];
        int n11 = rings[RING_BODY_TOP];      // floor of the partner band, since the reorder
        int n12 = rings[RING_SIGN_OUTER];
        int n13 = g.bodyBase;
        double[] dArray = this.activeCusps;
        double d8 = g.pin;
        double d9 = (180.0 + d8 - d7) % 360.0;
        if (d9 < 0.0) {
            d9 += 360.0;
        }
        int[] nArray = g.natalRadii();
        int[] nArray2 = g.transitRadii();
        int[] nArrayC = g.triRadii();
        for (n3 = 0; n3 < BODY_COUNT; ++n3) {
            if (!this.natalRing.valid[n3] || !Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArray[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.natalRing.lon[n3]))), (double)n2 - (d = (double)n8 + (double)nArray[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, false))) continue;
            this.showAngleAt(n3, this.natalRing.lon[n3], AngleRole.ANCHOR);
            return;
        }
        // Tri-wheel angle hit test before the synastry ring, because the tri ring is outermost.
        if (this.triRingDrawn()) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                if (!this.skyRing.valid[n3] || !Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArrayC[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.skyRing.lon[n3]))), (double)n2 - (d = (double)n8 + (double)nArrayC[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                this.showAngleAt(n3, this.skyRing.lon[n3], true, AngleRole.SKY);
                return;
            }
        }
        if (this.outerRingDrawn()) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                if (!this.outerRing.valid[n3] || !Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArray2[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.outerRing.lon[n3]))), (double)n2 - (d = (double)n8 + (double)nArray2[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                this.showAngleAt(n3, this.outerRing.lon[n3], this.angleRoleFor(false, true));
                return;
            }
        }
        if (this.showTransitChart && "Both".equals(this.houseAlignment) && d6 >= (double)(n11 - 6) && d6 <= (double)(n10 + 6)) {
            for (n3 = 1; n3 <= 12; ++n3) {
                d2 = (180.0 + d8 - this.outerRing.cusps[n3]) % 360.0;
                if (d2 < 0.0) {
                    d2 += 360.0;
                }
                if ((d3 = Math.abs(d7 - d2)) > 180.0) {
                    d3 = 360.0 - d3;
                }
                if (!(d3 < 4.0)) continue;
                this.window.showInterpretationForHouse(n3);
                return;
            }
        }
        // Tri-wheel body hit test — outermost ring, so test it before the transit ring.
        if (this.triRingDrawn() && d6 >= (double)n10 && d6 <= (double)(nTri + 10)) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                int n14c;
                if (!this.skyRing.valid[n3] || Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArrayC[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.skyRing.lon[n3]))), (double)n2 - (d = (double)n8 + (double)nArrayC[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                int n15c = (int)(this.skyRing.lon[n3] / 30.0);
                int n16c = (int)(this.skyRing.lon[n3] % 30.0 / 10.0) + 1;
                int n17c = 1;
                for (int i = 1; i <= 12; ++i) {
                    double d10; double d11;
                    double d12 = dArray[i];
                    double d13 = d11 = i == 12 ? dArray[1] : dArray[i + 1];
                    if (d11 < d12) d11 += 360.0;
                    if ((d10 = this.skyRing.lon[n3]) < d12 && d11 > 360.0) d10 += 360.0;
                    if (!(d10 >= d12) || !(d10 < d11)) continue;
                    n17c = i; break;
                }
                // The sky ring aspects BOTH people, so this walks natalRing.lon (chart A) and then
                // outerRing.lon (chart B). Listing only chart A was the tri-wheel's first shipped
                // behaviour and it was silent: half the contacts simply were not there, and
                // nothing in the panel said a chart had been skipped.
                //
                // <b>Neither pass is a synastry pair.</b> chartMode IS SYNASTRY here, so
                // isSynastryPair(true) would answer yes and halve the orb - but that rule is
                // for one person's chart against another's. The sky is not a person. Both
                // passes are sky-to-person, which is a transit and takes the full orb, so
                // both ask isSynastryPair(false) rather than passing a bare literal.
                ArrayList<String[]> arrayListC = new ArrayList<String[]>();
                for (n14c = 0; n14c < BODY_COUNT; ++n14c) {
                    String string;
                    if (!this.natalRing.valid[n14c]) continue;
                    double d14 = Math.abs(this.skyRing.lon[n3] - this.natalRing.lon[n14c]);
                    if (d14 > 180.0) d14 = 360.0 - d14;
                    if ((string = this.getAspectType(d14, n3, n14c, this.isSynastryPair(false))) == null) continue;
                    boolean blA = this.isAspectApplying(this.skyRing.lon[n3], this.skyRing.speed[n3], this.natalRing.lon[n14c], this.natalRing.speed[n14c], string);
                    String string2 = this.describeAspect(d14, string, n3, n14c, this.skyRing.lon[n3], this.natalRing.lon[n14c], blA);
                    arrayListC.add(new String[]{BODY_NAMES[n14c], string, string2, "Chart A"});
                }
                for (n14c = 0; n14c < BODY_COUNT; ++n14c) {
                    String string;
                    if (!this.outerRing.valid[n14c]) continue;
                    double d14 = Math.abs(this.skyRing.lon[n3] - this.outerRing.lon[n14c]);
                    if (d14 > 180.0) d14 = 360.0 - d14;
                    if ((string = this.getAspectType(d14, n3, n14c, this.isSynastryPair(false))) == null) continue;
                    boolean blB = this.isAspectApplying(this.skyRing.lon[n3], this.skyRing.speed[n3], this.outerRing.lon[n14c], this.outerRing.speed[n14c], string);
                    String string2 = this.describeAspect(d14, string, n3, n14c, this.skyRing.lon[n3], this.outerRing.lon[n14c], blB);
                    arrayListC.add(new String[]{BODY_NAMES[n14c], string, string2, "Chart B"});
                }
                n14c = (int)(this.skyRing.lon[n3] % 30.0) + 1;
                // A progressed body is a placement, not a transit. The transit route would head
                // the page "Transiting Venus" and describe a passing event; what a progressed
                // chart says is where the body has moved to. Same defect shape as the synastry
                // rows that read as transit rows, guarded before it could happen rather than
                // after.
                this.window.showInterpretationForPlanet(
                    this.showProgressed() ? BODY_NAMES[n3]
                        : "transit_" + BODY_NAMES[n3].toLowerCase(),
                    SIGN_NAMES[n15c], n14c, n16c, n17c, arrayListC);
                return;
            }
        }
        if (this.outerRingDrawn() && d6 >= (double)n11 && d6 <= (double)(n10 + 10)) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                int n14;
                if (!this.outerRing.valid[n3] || Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArray2[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.outerRing.lon[n3]))), (double)n2 - (d = (double)n8 + (double)nArray2[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                int n15 = (int)(this.outerRing.lon[n3] / 30.0);
                int n16 = (int)(this.outerRing.lon[n3] % 30.0 / 10.0) + 1;
                int n17 = 1;
                for (int i = 1; i <= 12; ++i) {
                    double d10;
                    double d11;
                    double d12 = dArray[i];
                    double d13 = d11 = i == 12 ? dArray[1] : dArray[i + 1];
                    if (d11 < d12) {
                        d11 += 360.0;
                    }
                    if ((d10 = this.outerRing.lon[n3]) < d12 && d11 > 360.0) {
                        d10 += 360.0;
                    }
                    if (!(d10 >= d12) || !(d10 < d11)) continue;
                    n17 = i;
                    break;
                }
                ArrayList<String[]> arrayList = new ArrayList<String[]>();
                for (n14 = 0; n14 < BODY_COUNT; ++n14) {
                    String string;
                    if (!this.natalRing.valid[n14]) continue;
                    double d14 = Math.abs(this.outerRing.lon[n3] - this.natalRing.lon[n14]);
                    if (d14 > 180.0) {
                        d14 = 360.0 - d14;
                    }
                    if ((string = this.getAspectType(d14, n3, n14, this.isSynastryPair(true))) == null) continue;
                    boolean bl = this.isAspectApplying(this.outerRing.lon[n3], this.outerRing.speed[n3], this.natalRing.lon[n14], this.natalRing.speed[n14], string);
                    String string2 = this.describeAspect(d14, string, n3, n14, this.outerRing.lon[n3], this.natalRing.lon[n14], bl);
                    arrayList.add(new String[]{BODY_NAMES[n14], string, string2});
                }
                n14 = (int)(this.outerRing.lon[n3] % 30.0) + 1;
                this.window.showInterpretationForPlanet(
                    this.showProgressed() ? BODY_NAMES[n3]
                        : "transit_" + BODY_NAMES[n3].toLowerCase(),
                    SIGN_NAMES[n15], n14, n16, n17, arrayList);
                return;
            }
        }
        if (d6 < (double)n13) {
            int n18;
            boolean bl;
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                int n19;
                double d15;
                if (!this.natalRing.valid[n3] || Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d = (double)n7 + (double)nArray[n3] * Math.cos(d3 = Math.toRadians(180.0 + d8 - (d2 = this.natalRing.lon[n3])))), (double)n2 - (d15 = (double)n8 + (double)nArray[n3] * Math.sin(d3))) < (double)SkymapPanel.hitRadius(n3, false))) continue;
                int n20 = (int)(d2 / 30.0);
                int n21 = (int)(d2 % 30.0 / 10.0) + 1;
                int n22 = 1;
                for (int i = 1; i <= 12; ++i) {
                    double d16;
                    double d17;
                    double d18 = dArray[i];
                    double d19 = d17 = i == 12 ? dArray[1] : dArray[i + 1];
                    if (d17 < d18) {
                        d17 += 360.0;
                    }
                    if ((d16 = d2) < d18 && d17 > 360.0) {
                        d16 += 360.0;
                    }
                    if (!(d16 >= d18) || !(d16 < d17)) continue;
                    n22 = i;
                    break;
                }
                ArrayList<String[]> arrayList = new ArrayList<String[]>();
                for (n19 = 0; n19 < BODY_COUNT; ++n19) {
                    String string;
                    if (n3 == n19 || !this.natalRing.valid[n19]) continue;
                    double d20 = Math.abs(d2 - this.natalRing.lon[n19]);
                    if (d20 > 180.0) {
                        d20 = 360.0 - d20;
                    }
                    if ((string = this.getAspectType(d20, n3, n19, false)) == null) continue;
                    boolean bl2 = this.isAspectApplying(d2, this.natalRing.speed[n3], this.natalRing.lon[n19], this.natalRing.speed[n19], string);
                    String string3 = this.describeAspect(d20, string, n3, n19, d2, this.natalRing.lon[n19], bl2);
                    arrayList.add(new String[]{BODY_NAMES[n19], string, string3});
                }
                n19 = (int)(d2 % 30.0) + 1;
                // d2 is this body's longitude, which the mansion lookup needs exactly.
                this.window.showInterpretationForPlanet(BODY_NAMES[n3], SIGN_NAMES[n20], n19, n21, n22, arrayList, d2);
                return;
            }
            n3 = this.aspectFilter.equals("Natal-Natal") || this.aspectFilter.equals("Both") ? 1 : 0;
            boolean bl3 = bl = (this.aspectFilter.equals("Transit-Natal") || this.aspectFilter.equals("Both")) && this.showTransitChart;
            if (n3 != 0) {
                for (n18 = 0; n18 < BODY_COUNT; ++n18) {
                    if (!SkymapPanel.aspecting(n18, this.natalRing.valid)) continue;
                    for (int i = n18 + 1; i < BODY_COUNT; ++i) {
                        if (!SkymapPanel.aspecting(i, this.natalRing.valid) || Bodies.isOppositePair(n18, i) || !this.checkAspectHit(n, n2, n7, n8, d8, g.aspectDisc(0), g.aspectDisc(0), this.natalRing.lon[n18], this.natalRing.lon[i], BODY_NAMES[n18], BODY_NAMES[i])) continue;
                        return;
                    }
                }
            }
            if (bl) {
                for (n18 = 0; n18 < BODY_COUNT; ++n18) {
                    if (!SkymapPanel.aspecting(n18, this.outerRing.valid)) continue;
                    for (int i = 0; i < BODY_COUNT; ++i) {
                        if (!SkymapPanel.aspecting(i, this.natalRing.valid) || !this.checkAspectHit(n, n2, n7, n8, d8, g.aspectDisc(1), g.aspectDisc(1), this.outerRing.lon[n18], this.natalRing.lon[i], "transit_" + BODY_NAMES[n18].toLowerCase(), BODY_NAMES[i])) continue;
                        return;
                    }
                }
            }
            // The sky ring's chords, which were drawn and were not clickable - the click walked
            // the natal field and the partner field and stopped, so a reader could point at a
            // sky aspect and get whatever happened to be behind it.
            if (bl && this.triRingDrawn()) {
                for (n18 = 0; n18 < BODY_COUNT; ++n18) {
                    if (!SkymapPanel.aspecting(n18, this.skyRing.valid)) continue;
                    for (int i = 0; i < BODY_COUNT; ++i) {
                        if (!SkymapPanel.aspecting(i, this.natalRing.valid)
                            || !this.checkAspectHit(n, n2, n7, n8, d8, g.aspectDisc(2),
                                g.aspectDisc(2), this.skyRing.lon[n18], this.natalRing.lon[i],
                                "transit_" + BODY_NAMES[n18].toLowerCase(), BODY_NAMES[i])) {
                            continue;
                        }
                        return;
                    }
                }
            }
            n18 = 1;
            for (int i = 1; i <= 12; ++i) {
                double d21;
                double d22;
                double d23 = dArray[i];
                double d24 = d22 = i == 12 ? dArray[1] : dArray[i + 1];
                if (d22 < d23) {
                    d22 += 360.0;
                }
                if ((d21 = (540.0 + d8 - d7) % 360.0) < d23 && d22 > 360.0) {
                    d21 += 360.0;
                }
                if (!(d21 >= d23) || !(d21 < d22)) continue;
                n18 = i;
                break;
            }
            this.window.showInterpretationForHouse(n18);
            return;
        }
        // <b>Each ring against its own two radii, outside in.</b> These tests were written
        // for the old chain and never re-derived when the rings were reordered outward, so
        // they had come to describe a wheel that no longer exists. The sign test ran from the
        // body base all the way out to the sign ring's outer edge - one band swallowing the
        // decans, the sign ring, the bounds and the inner degree scale - and the decan test
        // read from the sign ring outward to the body top, which is inside it, so it covered
        // nothing at all and the decan ring answered a click with silence. Measured with a
        // probe that walks the wheel from the middle out and reports what each radius opens:
        // every ring from 226 to 500 pixels said "Virgo", and the decan ring said nothing.
        //
        // Each band now names the two radii it lies between, so a ring that moves takes its
        // own hit test with it. The open middle keeps the sign, which is what clicking inside
        // a sign's wedge has always meant.
        // Through bandAt, so the reading a ring opens and the radii it occupies cannot drift
        // apart again. The inner degree scale reads a Sabian symbol: it was drawn with 360
        // ticks and answered nothing - a scale the reader can count along and cannot ask.
        // The bounds have no reading of their own yet, so they fall in with the open middle
        // and name the sign, which is better than silence.
        switch (SkymapPanel.bandAt(d6, rings, n13)) {
            case BAND_DECAN: {
                n3 = (int)(d9 / 30.0);
                int n23 = (int)(d9 % 30.0 / 10.0) + 1;
                this.window.showInterpretationForDecan(SIGN_NAMES[n3], n23);
                return;
            }
            case BAND_SIGN:
            case BAND_OPEN: {
                n3 = (int)(d9 / 30.0);
                this.window.showInterpretationForSign(SIGN_NAMES[n3]);
                return;
            }
            case BAND_BOUND: {
                this.window.showInterpretationForBound(d9);
                return;
            }
            case BAND_DEGREE: {
                n3 = (int)Math.round(d9) % 360;
                if (n3 < 0) {
                    n3 += 360;
                }
                this.window.showInterpretationForSabianSymbol(
                    SIGN_NAMES[n3 / 30], n3 % 30 + 1);
                return;
            }
            default:
                break;
        }
        // The rim, from the decan ring out, and it is two targets now: the mansion band above
        // RING_MANSION_INNER, the degree scale below it. With the mansions folded the scale has
        // the whole rim and answers everywhere in it, which is what it did before the band was
        // carved out.
        if (this.layerShown(Layer.MANSIONS) && SkymapPanel.inMansionRing(d6, rings)) {
            this.window.showInterpretationForMansion(
                com.zodiacomputing.ourania.astro.LunarMansions.at(d9).number);
            return;
        }
        if (SkymapPanel.inMansionBand(d6, n9)) {
            if (this.layerShown(Layer.MANSIONS) && !SkymapPanel.inRimDegreeBand(d6, rings)) {
                this.window.showInterpretationForMansion(
                    com.zodiacomputing.ourania.astro.LunarMansions.at(d9).number);
                return;
            }
            n3 = (int)Math.round(d9) % 360;
            if (n3 < 0) {
                n3 += 360;
            }
            this.window.showInterpretationForSabianSymbol(SIGN_NAMES[n3 / 30], n3 % 30 + 1);
        }
    }

    /**
     * True when the single wheel is a RELATIONSHIP rather than a person.
     *
     * <b>Gated on the mode, not on {@code ChartFrame.syntheticMoment}.</b> That flag marks a
     * chart with no moment behind it, which is the midpoint composite only - a Davison is a
     * real chart cast for a real instant and carries the flag false. Both are charts of a
     * relationship and both need the relationship voice, so keying the prose off
     * syntheticMoment would have framed the midpoint composite correctly and left the Davison
     * reading as though it described a person.
     */
    public boolean isRelationshipChart() {
        return this.chartMode == ChartMode.COMPOSITE_MIDPOINT
            || this.chartMode == ChartMode.COMPOSITE_DAVISON;
    }

    /** True when two people are on the wheel at once. */
    public boolean isSynastryChart() {
        return this.chartMode == ChartMode.SYNASTRY;
    }

    /** True when an aspect label names a body on the OUTER wheel. */
    private static boolean isTransitLabel(String string) {
        return string != null && string.startsWith(TRANSIT_PREFIX);
    }

    /**
     * Whether this pair is judged at the halved synastry orb.
     *
     * <b>Two conditions, and leaving either one out is a silent defect.</b>
     *
     * <ul>
     *   <li><b>The pair must be cross-chart.</b> A synastry screen still shows each person's
     *       own natal aspects - the wheel draws them, the grid lists them, clicking a body
     *       reports them - and those are not synastry contacts. Tightening a person's own
     *       Sun-square-Saturn because you happen to be viewing them next to someone else
     *       would be nonsense, and nothing on screen would look wrong.</li>
     *   <li><b>The mode must be SYNASTRY.</b> Transit-to-natal is cross-chart by the same
     *       arithmetic and must NOT be halved: the halved orb is a convention about two
     *       people, not about a chart and a moment. Gating on cross-chart alone would have
     *       silently retuned every transit reading in the app.</li>
     * </ul>
     *
     * One definition for all eight places that ask. See WORK-PLAN for the measurement behind
     * the halving itself, and {@link Aspects#effectiveOrb} for what it does.
     */
    private boolean isSynastryPair(boolean crossChart) {
        return crossChart && this.chartMode == ChartMode.SYNASTRY;
    }

    private static String planetName(int n) {
        return n >= 0 && n < BODY_NAMES.length ? BODY_NAMES[n] : null;
    }

    /** An angle click from any wheel except the tri-wheel's sky ring. */
    /**
     * Which chart a clicked angle belongs to, and therefore what the card is claiming.
     *
     * <b>The K7 decision: the asymmetry is a relational law, not a bug.</b> Chart A owns the
     * twelve houses on screen, so clicking Chart A's angle reads natally - it is the baseline
     * the whole session is framed by. Chart B has no house boundaries here; its angles are
     * visiting, falling into Chart A's houses, so clicking one is inherently a cross-chart
     * event. Forcing symmetry breaks it either way: "both natal" makes the reader work out by
     * hand where B's Ascendant lands, and "both cross-chart" denies A their own baseline.
     *
     * <b>The behaviour was already right; what was missing was saying so.</b> The two cards
     * were identical in appearance while making different claims, which is the one thing a
     * deliberate asymmetry cannot afford - indistinguishable, it reads as inconsistency.
     */
    enum AngleRole {
        /** Chart A's own angle: the frame everything else is measured against. */
        ANCHOR,
        /** Chart B's angle, projected into Chart A's houses. */
        BRIDGE,
        /** A transit or sky angle - a moment passing over the chart, not a person. */
        SKY
    }

    /**
     * Which chart an angle belongs to, asked once for both surfaces that can open one.
     *
     * <b>The wheel and the placements list are two doors onto the same card.</b> Clicking a
     * glyph and following a {@code transit_} link have to answer this identically, and a
     * ternary written out at each site is the one-rule-two-implementations defect this
     * project has shipped several times - the copies do not diverge on the day they are
     * written, they diverge the day one of them is edited.
     */
    /**
     * The hue a ring's glyphs are pulled toward, so you can tell whose they are.
     *
     * <b>Extending a convention the wheel already half-had.</b> A partner's angle was already
     * drawn in warm gold and a sky angle in cool blue - four glyphs out of twenty-nine on each
     * ring - while every body on those rings took the same element colour as the natal wheel,
     * differing only by how much it had been lightened. Lightness alone is not a distinction a
     * reader can hold: with three rings of the same palette, a Venus is a Venus and there is
     * nothing on the glyph that says which chart it belongs to.
     *
     * <b>Flat per ring, not the element colour tinted.</b> Blending was tried first and makes
     * mud: a cyan Moon mixed halfway to gold comes out pale green, a red Sun mixed halfway to
     * blue comes out grey, and the two outer rings ended up 67 apart in RGB - technically
     * different, useless at a glance, and no longer saying "gold ring" or "blue ring" either.
     * The flat hues are 136 apart and say exactly one thing each.
     *
     * The element colour is kept where it earns its place - the natal wheel, the chart being
     * read. On an outer ring it was close to redundant anyway: a transiting Mars is fire
     * because it is Mars, and the glyph already says so.
     */
    private static final Color BRIDGE_HUE = new Color(255, 210, 122);
    private static final Color SKY_HUE = new Color(143, 208, 255);

    /** A body's glyph colour on a given ring. ANCHOR keeps the element colour untouched. */
    Color ringInk(int body, AngleRole role) {
        if (role == AngleRole.ANCHOR) {
            return this.bodyColor(body);
        }
        return role == AngleRole.BRIDGE ? BRIDGE_HUE : SKY_HUE;
    }

    /** The bead a ring's glyphs sit on, dark enough for its own ink to read against. */
    static Color ringBead(AngleRole role) {
        switch (role) {
            case BRIDGE:
                return new Color(62, 54, 38);
            case SKY:
                return new Color(24, 48, 74);
            default:
                return new Color(62, 66, 76);
        }
    }

    /**
     * The label colour for an angle on a ring - the same hue as its bodies.
     *
     * <b>Through ringInk, not beside it.</b> The angles and the bodies on one ring being two
     * colour decisions is how the wheel got here: gold and blue existed for four glyphs a ring
     * and nothing else. Body index -1 is not a body, and ANCHOR never reaches the branch that
     * would use it.
     */
    Color ringAngleInk(AngleRole role) {
        return role == AngleRole.ANCHOR ? new Color(255, 228, 160) : this.ringInk(-1, role);
    }

    AngleRole angleRoleFor(boolean isSky, boolean isTransit) {
        if (isSky) {
            return AngleRole.SKY;
        }
        if (isTransit) {
            // The outer wheel is a second person in a synastry and a moment otherwise.
            return this.chartMode == ChartMode.SYNASTRY ? AngleRole.BRIDGE : AngleRole.SKY;
        }
        return AngleRole.ANCHOR;
    }

    private void showAngleAt(int n, double d) {
        this.showAngleAt(n, d, false, AngleRole.ANCHOR);
    }

    private void showAngleAt(int n, double d, AngleRole role) {
        this.showAngleAt(n, d, false, role);
    }

    /**
     * The reading for a clicked angle, with its aspects to the bodies on the other wheel.
     *
     * <b>One other wheel in every mode but one.</b> A natal angle reads against the natal
     * bodies; a composite angle reads against the composite's, because a composite mode loads
     * the composite INTO the base arrays (see updateChartData) rather than special-casing it
     * downstream; a transit angle reads against the natal; and chart B's angle reads against
     * chart A. In each of those there is exactly one other chart and {@code natalRing.lon} is it.
     *
     * <b>The tri-wheel is the exception, and the reason for {@code fromSky}.</b> A sky angle
     * has TWO other charts under it, and walking only {@code natalRing.lon} silently dropped every
     * contact to chart B - the same defect the sky BODY click had. When {@code fromSky} is
     * true and a tri-wheel is up, both passes run and each row is tagged with the chart it
     * came from, because "Ascendant square Sun" is a different statement about each person.
     *
     * <b>Rows are tagged only in that case.</b> Every other caller still emits three-element
     * rows and renders exactly as it did, so no existing reading changes wording.
     */
    private void showAngleAt(int n, double d, boolean fromSky, AngleRole role) {
        int n2 = (int)(d / 30.0) % 12;
        int n3 = (int)(d % 30.0) + 1;
        boolean bothCharts = fromSky && this.showTriWheel;
        ArrayList<String[]> arrayList = new ArrayList<String[]>();
        for (int i = 0; i < BODY_COUNT; ++i) {
            String string;
            if (!SkymapPanel.aspecting(i, this.natalRing.valid) || Bodies.isOppositePair(n, i)) continue;
            double d2 = Math.abs(d - this.natalRing.lon[i]);
            if (d2 > 180.0) {
                d2 = 360.0 - d2;
            }
            if ((string = this.getAspectType(d2, n, i, false)) == null) continue;
            boolean bl = this.isAspectApplying(d, 361.0, this.natalRing.lon[i], this.natalRing.speed[i], string);
            String string2 = this.describeAspect(d2, string, n, i, d, this.natalRing.lon[i], bl);
            arrayList.add(bothCharts
                ? new String[]{BODY_NAMES[i], string, string2, "Chart A"}
                : new String[]{BODY_NAMES[i], string, string2});
        }
        // The second chart, for a sky angle only. Sky-to-person is a transit and takes the
        // full orb, so this asks isSynastryPair(false) - chartMode IS SYNASTRY here and
        // answering true would halve an orb that belongs to person-against-person.
        if (bothCharts) {
            for (int i = 0; i < BODY_COUNT; ++i) {
                String string;
                if (!SkymapPanel.aspecting(i, this.outerRing.valid) || Bodies.isOppositePair(n, i)) continue;
                double d2 = Math.abs(d - this.outerRing.lon[i]);
                if (d2 > 180.0) {
                    d2 = 360.0 - d2;
                }
                if ((string = this.getAspectType(d2, n, i, this.isSynastryPair(false))) == null) continue;
                boolean bl = this.isAspectApplying(d, 361.0, this.outerRing.lon[i], this.outerRing.speed[i], string);
                String string2 = this.describeAspect(d2, string, n, i, d, this.outerRing.lon[i], bl);
                arrayList.add(new String[]{BODY_NAMES[i], string, string2, "Chart B"});
            }
        }
        // <b>Where a visiting angle lands is the whole content of a bridge card.</b> "Your
        // Ascendant falls in my 7th" is the reading; without it the card states a sign and a
        // degree belonging to someone whose houses are not on the screen.
        int hostHouse = role == AngleRole.BRIDGE
            ? Zodiac.houseOf(d, this.activeCusps) : -1;
        this.window.showInterpretationForAngle(BODY_NAMES[n], SIGN_NAMES[n2], n3, arrayList,
            role.name(), hostHouse);
    }

    private double getOrbFor(int n, int n2, boolean synastry) {
        return Aspects.orbFor(SkymapPanel.planetName(n), SkymapPanel.planetName(n2), synastry);
    }

    private String getAspectType(double d) {
        // No caller as of 2026-08-24. Left rather than deleted, and made to state its answer
        // like every other caller rather than inheriting a default.
        return this.getAspectType(d, -1, -1, false);
    }

    /**
     * The aspect label for a separation, or null.
     *
     * <b>{@code synastry} is not optional and there is deliberately no overload that omits
     * it.</b> Every one of this method's callers knows whether it is comparing one person's
     * chart to another's; a defaulting overload would let a new caller be wrong by saying
     * nothing, and being wrong here is invisible - a cell that should be empty is filled, or
     * a cell that should be filled is empty, and the wheel and the grid disagree quietly.
     * Ask {@link #isSynastryPair} rather than passing a literal.
     */
    private String getAspectType(double d, int n, int n2, boolean synastry) {
        Aspects.Type type = this.visibleAspect(d, n, n2, synastry);
        return type == null ? null : type.label;
    }

    /**
     * <b>The one gate: which aspect a separation makes, and whether it is switched on.</b>
     *
     * There are two draw paths - the grid asks through {@code getAspectType}, the wheel's
     * {@code drawAspectLine} used to call {@code Aspects.typeOf} directly - and a visibility
     * filter added to only one of them would have produced a chart whose grid and whose lines
     * disagreed about which aspects exist. That is this project's most-logged defect and it
     * would have been invisible: both surfaces look entirely plausible on their own.
     */
    private Aspects.Type visibleAspect(double sep, int a, int b, boolean synastry) {
        Aspects.Type type = Aspects.typeOf(sep, SkymapPanel.planetName(a),
            SkymapPanel.planetName(b), synastry);
        if (type == null) {
            return null;
        }
        return this.aspectShown == null || type.ordinal() >= this.aspectShown.length
            || this.aspectShown[type.ordinal()] ? type : null;
    }

    /**
     * Whether this pair earns a line across the wheel, under the reader's chosen mode.
     *
     * <b>The aspect still exists either way.</b> It is computed, it is in the grid, it is in
     * the placements and the reading; the mode decides only what becomes a line through the
     * middle. Tompkins' rule, which is about legibility rather than significance: note the
     * aspects to the minor bodies, do not draw them, so the essentials can be found quickly.
     *
     * <b>Asked here and not inside visibleAspect, which was the first attempt.</b> That method
     * is the one gate the wheel and the grid share, so putting the mode in it emptied the grid
     * of every asteroid and point as well - 286 failures in AspectGridCheck, all of the form
     * "Sun/North Node is blank and really has no aspect at that orb", because the cell was
     * blank for a reason the check could not see. Listed and drawn are different questions.
     *
     * Seeded at declaration and refreshed on the settings hook, not per call: this runs once
     * per body pair per repaint, and reading a properties file inside that loop would be felt.
     */
    private boolean drawsPair(int a, int b) {
        if (Settings.ASPECTS_ESOTERIC.equals(this.aspectMode)) {
            return true;
        }
        boolean angles = Settings.ASPECTS_MANIFESTATION.equals(this.aspectMode);
        return SkymapPanel.drawable(a, angles) && SkymapPanel.drawable(b, angles);
    }

    /** A classical planet always; an angle too once Manifestation is chosen. */
    private static boolean drawable(int index, boolean anglesCount) {
        Bodies.Def d = Bodies.at(index);
        if (d == null) {
            return false;
        }
        if (d.kind == Bodies.Kind.LUMINARY || d.kind == Bodies.Kind.PLANET) {
            return true;
        }
        return anglesCount && d.kind == Bodies.Kind.ANGLE;
    }

    /** The Settings screen changed which aspects are drawn. */
    public void reloadAspectSelection() {
        // No invalidateGlobeChords here: updateChartData below does it, and a second call
        // that a mutation cannot kill is not a safety net, it is a claim that the clearing
        // happens in two places when it happens in one.
        this.aspectShown = Settings.loadAspectSelection();
        // Read here and not in updateChartData. Refreshing on every compute overwrote the pin
        // a suite had just set, so AspectGridCheck still drifted 6509 against 6522 with the
        // pin apparently in place - a guard that a later line quietly undoes is worse than
        // none, because the suite goes green either way. This is the hook SettingsPanel calls
        // when anything it owns changes, which is exactly when this needs re-reading.
        this.showProgressed = Settings.OUTER_PROGRESSED.equals(Settings.outerWheel());
        // And the rings that follow from it. Without this, choosing Progressions here changed
        // what the middle ring carried and not whether the sky had anywhere to go.
        this.applyRingFlags();
        this.aspectMode = Settings.aspectMode();
        this.updateChartData();
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
    }

    private String getSolarCondition(int n, double d, double d2) {
        if (n < 0) {
            return null;
        }
        Aspects.Solar solar = Aspects.solarCondition(SkymapPanel.planetName(n), d, d2);
        return solar == null ? null : solar.label;
    }

    private boolean isPartile(double d, String string) {
        Aspects.Type type = Aspects.Type.fromLabel(string);
        return type != null && Aspects.isPartile(d, type);
    }

    private int indexOfPlanetName(String string) {
        if (string == null) {
            return -1;
        }
        for (int i = 0; i < BODY_NAMES.length; ++i) {
            if (!BODY_NAMES[i].equalsIgnoreCase(string)) continue;
            return i;
        }
        return -1;
    }

    private String describeAspect(double d, String string, int n, int n2, double d2, double d3, boolean bl) {
        StringBuilder stringBuilder = new StringBuilder(bl ? "Applying" : "Separating");
        if (this.isPartile(d, string)) {
            stringBuilder.append(" \u00b7 Partile");
        }
        if ("Conjunction".equals(string)) {
            String string2 = null;
            if (n == SUN) {
                string2 = this.getSolarCondition(n2, d3, d2);
            } else if (n2 == SUN) {
                string2 = this.getSolarCondition(n, d2, d3);
            }
            if (string2 != null) {
                stringBuilder.append(" \u00b7 ").append(string2);
            }
        }
        return stringBuilder.toString();
    }

    private double getExactAngleForAspect(String string) {
        Aspects.Type type = Aspects.Type.fromLabel(string);
        return type == null ? 0.0 : type.exactAngle;
    }

    private boolean isAspectApplying(double d, double d2, double d3, double d4, String string) {
        Aspects.Type type = Aspects.Type.fromLabel(string);
        return type != null && Aspects.isApplying(d, d2, d3, d4, type);
    }

    // ------------------------------------------------- aspect grid: click, hover, highlight

    /** Prefix the aspect grid puts on the row body when the row is a transiting point. */
    private static final String TRANSIT_PREFIX = "transit_";

    /**
     * Which wheel an aspect line runs from.
     *
     * <b>One bit could not name three rings.</b> Every cross-chart aspect - partner and sky
     * alike - was flagged with a single boolean called "transit", because when it was written
     * there were only two wheels. The sky ring arrived with a field of its own and its own
     * chords, and inherited that boolean: hovering a partner aspect lit the sky line of the
     * same pair and hovering a sky aspect lit the partner line, and neither could be told
     * apart because nothing in the highlight knew there was a third ring to tell apart. The
     * sky ring had no rows in the grid at all, so in practice its lines could not be hovered
     * from anywhere.
     *
     * These are the wheel a line leaves from; every line still lands on the natal wheel.
     */
    static final int WHEEL_NATAL = 0;
    static final int WHEEL_OUTER = 1;
    static final int WHEEL_SKY = 2;

    /** The globe's three decks: this chart in the middle, Chart B below, the sky above. */
    static final int DECK_MIDDLE = 0;
    static final int DECK_LOWER = 1;
    static final int DECK_UPPER = 2;

    /**
     * Which deck of the globe a wheel's bodies belong on.
     *
     * <b>A wheel is a role and a deck is a chart, and the globe was drawing the role.</b> The
     * engine has three slots - inner, outer, sky - and which chart sits in which slot moves
     * with the selection: take Chart A out and Chart B is promoted to the inner slot, take
     * Chart B out of a synastry and the sky moves into the outer one. That promotion is
     * deliberate and it is about what kind of chart is being cast. Drawing straight from the
     * slot index made it a visual promotion too, so a chip that took one chart out sent the
     * other two climbing between decks - and David, watching it, could not tell whether he had
     * deselected the chart he meant to.
     *
     * So the deck is decided by whose chart it is rather than by which slot holds it. The sky
     * is always the top ring when it is transits, Chart B always the bottom one, and the chart
     * being read is always the middle - which is stable under every selection, because the
     * only thing a chip can do is empty a deck.
     *
     * The three values are the same 0/1/2 the globe already used for natal, partner and sky
     * radii and planes, so nothing downstream has to learn a second vocabulary - the callers
     * pass this instead of the wheel index and everything else is as it was.
     */
    int ringDeck(int wheel) {
        if (wheel == WHEEL_SKY) {
            return DECK_UPPER;
        }
        if (wheel == WHEEL_OUTER) {
            // The outer slot is a second person in a synastry and the sky in every other mode
            // - the same rule outerRingMarker states, read here for the same reason.
            return this.isSynastryChart() ? DECK_LOWER : DECK_UPPER;
        }
        // <b>The inner slot is the middle deck unless Chart B has been promoted into it.</b>
        // A composite is its own chart rather than either person's and belongs in the middle;
        // so does the sky when the sky is the whole chart, because then it is not transits
        // over something, it is the thing being read, and a lone ring floating above an empty
        // middle would say otherwise.
        boolean chartBIsTheChart = !this.isRelationshipChart()
            && this.anchorSubject != null && this.anchorSubject == this.subjectB;
        // <b>Two wheels must never land on one deck.</b> They would be drawn at one radius in
        // one plane and the reader would see one ring holding two charts. A synastry already
        // has Chart B on the lower deck, and a chart with two people in it has Chart A on the
        // inner wheel anyway - so this only ever fires for a half-built state, and it fires
        // toward the middle rather than into a collision.
        return chartBIsTheChart && !this.isSynastryChart() ? DECK_LOWER : DECK_MIDDLE;
    }

    /** The wheel a grid row label names, for the rows that predate the wheel field. */
    static int wheelOfLabel(String label) {
        return isTransitLabel(label) ? WHEEL_OUTER : WHEEL_NATAL;
    }

    /**
     * The natal half of the placement href, which was a bare literal in one place and is now
     * read by two: the placements list that writes it and the wheel click that sends it.
     */
    private static final String BASE_PREFIX = "base_";

    /** The aspect the grid is currently hovering, as body indices. -1 when nothing is hovered. */
    private int highlightA = -1;
    private int highlightB = -1;

    /**
     * Whether this body is one end of the aspect line the cursor is resting on.
     *
     * <b>Since the lines moved onto the discs they no longer touch their bodies</b>, so
     * hovering one had nothing to say about which two points it joined - the reader could see
     * a chord light up and still have to work out its ends from the angle. Lighting the two
     * glyphs is what puts that back, and it has to be one predicate rather than a test written
     * into each of the glyph loops, because the natal loop and the outer loop read the
     * highlight from opposite ends of the pair.
     */
    private boolean onHighlightedLine(int body, int wheel) {
        if (this.highlightA < 0 || this.highlightB < 0) {
            return false;
        }
        if (this.highlightWheel == WHEEL_NATAL) {
            return wheel == WHEEL_NATAL
                && (body == this.highlightA || body == this.highlightB);
        }
        // A cross-chart line: A is on the wheel the line leaves, B is the natal one it lands
        // on. Asking by wheel rather than by a single outer/not-outer flag is what lets a
        // partner glyph stay dark while the sky glyph of the same body lights.
        if (wheel == this.highlightWheel) {
            return body == this.highlightA;
        }
        return wheel == WHEEL_NATAL && body == this.highlightB;
    }

    /** The wheel the hovered line leaves from. Meaningless while nothing is hovered. */
    private int highlightWheel = WHEEL_NATAL;

    /** A ring of light around a glyph at the end of the hovered aspect line. */
    private static void drawHighlightHalo(Graphics2D g, int x, int y, int r) {
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
     * Registry indices of an aspect pattern's members, lit as a group. Empty when none.
     *
     * Separate from the single-pair highlight above rather than folded into it, because they
     * answer different questions and can be on at once: the pair is "the cell I am pointing
     * at", the pattern is "the figure I opened". Natal only - a pattern is a closed circuit
     * within one chart, and {@link com.zodiacomputing.ourania.astro.AspectPatterns} is only
     * ever handed natal aspects.
     */
    private int[] highlightPattern = new int[0];

    /**
     * Every aspect pattern in the current chart, lit without being asked.
     *
     * <b>One array per figure, never a union of them all.</b> A union would light the pair
     * Sun-Venus whenever a T-square held the Sun and a grand trine held Venus, drawing a leg
     * that belongs to no figure at all - the wheel would be asserting a relationship the
     * engine never found.
     *
     * Recomputed once per chart update in {@link #generatePlanetPlacementsHtml}, not per
     * repaint: finding patterns means aspecting every pair of bodies, and the painter runs on
     * every mouse move.
     */
    private int[][] autoPatterns = new int[0][];

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
     * True when a click radius lands on the lunar mansion ring.
     *
     * <b>The mansion ring and the Sabian degree share this band, and the ring wins.</b> The
     * mansions are drawn from {@code outer - 9} outward and the degree ticks reach
     * {@code outer - 6}, so everything visible out here belongs to the ring; the Sabian symbol
     * is a per-degree reading with nothing drawn for it and keeps the strip just inside.
     * Extracted as statics rather than left inline in the hit test so the two bands can be
     * asserted disjoint - a hit test that overlaps another silently gives one of them away.
     */
    static boolean inMansionBand(double radius, int outer) {
        return radius >= outer - RIM_BAND_DEPTH && radius <= outer + 15;
    }

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
    static final int RING_OUTER = 0;
    static final int RING_TRI = 1;
    static final int RING_TRANSIT = 2;
    static final int RING_DECAN_OUTER = 3;
    static final int RING_SIGN_OUTER = 4;
    static final int RING_SIGN_INNER = 5;
    /**
     * Inner edge of the partner band, and so the ceiling of the natal wheel.
     *
     * Added when the zodiac moved outward: with the body bands nested underneath the signs
     * there has to be a name for where the innermost of them stops, because that is where the
     * natal wheel is now allowed to start. Before the reorder this was RING_SIGN_INNER, and
     * with no outer ring open it still equals it exactly.
     */
    static final int RING_BODY_TOP = 6;
    /**
     * Floor of the bound (term) band, whose ceiling is {@code RING_SIGN_INNER}.
     *
     * <b>The signs sit between their two subdivisions now, not under both.</b> Decans outside,
     * signs, then the Egyptian bounds inside - so the sign band is framed by the two rings
     * that divide it rather than carrying them both on one side.
     */
    static final int RING_TERM_INNER = 7;

    /**
     * Floor of the inner degree ring, whose ceiling is {@code RING_TERM_INNER}.
     *
     * <b>The second degree scale, and the one the bodies point at.</b> The outer ticks sit at
     * the rim with the lunar mansions, a long way from any glyph; this one sits directly above
     * the wheels, so a body's leader line has somewhere near to land. Everything between the
     * two scales - decans, signs, bounds - is sandwiched by them.
     */
    static final int RING_DEGREE_INNER = 8;

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
        double o = Math.max(0.0, Math.min(1.0, outerOpen));
        double t = Math.max(0.0, Math.min(1.0, triOpen));
        double dc = Math.max(0.0, Math.min(1.0, decanOpen));
        double sg = Math.max(0.0, Math.min(1.0, signOpen));
        double bd = Math.max(0.0, Math.min(1.0, boundOpen));
        double dg = Math.max(0.0, Math.min(1.0, degreeOpen));
        int outer = Math.min(width, height) / 2 - 10;
        // Both body bands get the same depth, deep enough to hold their own sub-rings.
        int depth = SkymapPanel.outerBandDepth(outer);
        // The zodiac sits at fixed radii just inside the rim; it no longer moves when a body
        // ring opens, which is the point of putting it outside them.
        int decanOuter = outer - 20;
        int signOuter = (int) Math.round(decanOuter - 20 * dc);
        int signInner = (int) Math.round(signOuter - 35 * sg);
        int termInner = (int) Math.round(signInner - TERM_BAND_DEPTH * bd);
        int degreeInner = (int) Math.round(termInner - DEGREE_RING_DEPTH * dg);
        // The body bands hang below the inner degree scale, each opening downward.
        int tri     = degreeInner;
        int transit = (int) Math.round((double) tri - (double) depth * t);
        int bodyTop = (int) Math.round((double) transit - (double) depth * o);
        double mn = Math.max(0.0, Math.min(1.0, mansionOpen));
        int mansionInner = (int) Math.round(outer - MANSION_BAND_DEPTH * mn);
        return new int[] {
            outer, tri, transit, decanOuter, signOuter, signInner, bodyTop,
            termInner, degreeInner, mansionInner };
    }

    /** Overload for callers that pre-date the tri-wheel; preserves the old contract. */
    static int[] ringRadii(int width, int height, boolean showTransit) {
        return ringRadii(width, height, showTransit, false);
    }

    /** The same figures as objects, for the banner at the top of the aspect chart. */
    private java.util.List<com.zodiacomputing.ourania.astro.AspectPatterns.Pattern>
        currentPatterns = new java.util.ArrayList<>();

    /**
     * The href for one cell of the aspect grid.
     *
     * <b>Exists so that there is exactly one definition of this format.</b> The grid wrote the
     * href and the click handler parsed it, and the two were allowed to disagree - the handler
     * split on underscore while the row label was "transit_sun", so every transit cell was
     * silently unclickable. Builder and parser now sit next to each other and AspectGridCheck
     * drives the round trip through both, so neither can move alone.
     */
    static String aspectHref(String rowLabel, String columnBody, String aspectLabel) {
        return aspectHref(rowLabel, columnBody, aspectLabel, wheelOfLabel(rowLabel));
    }

    /**
     * The same, saying which wheel the row body is on.
     *
     * <b>The prefix cannot carry this.</b> A sky row and a partner row both put the outer body
     * first and both wear the transit prefix, so a reader hovering one got the other's line
     * lit. The wheel is its own field rather than a third prefix because the prefix is read by
     * five interpretation lookups that have no business knowing about rings, and a new prefix
     * would have had to be taught to all of them.
     */
    static String aspectHref(String rowLabel, String columnBody, String aspectLabel, int wheel) {
        return "aspect|" + rowLabel + "|" + columnBody + "|" + aspectLabel + "|" + wheel;
    }

    /**
     * Split an aspect-grid href into {rowLabel, columnBody, aspectLabel}, or null.
     *
     * <b>Pipe-delimited on purpose.</b> The row label is "transit_sun" on a transit row and
     * body names contain spaces ("North Node"), so neither underscore nor space can be the
     * separator. A pipe cannot occur in either.
     */
    private static String[] parseAspectHref(String href) {
        if (href == null || !href.startsWith("aspect|")) {
            return null;
        }
        String[] parts = href.split("\\|");
        if (parts.length == 5) {
            // A wheel field that is not a wheel makes the whole href untrustworthy. Falling
            // back to the natal wheel would light a natal line for a malformed link, which is
            // worse than lighting nothing: it looks like an answer.
            return readsAsWheel(parts[4])
                ? new String[]{parts[1], parts[2], parts[3], parts[4]} : null;
        }
        // An href from before the wheel field, which only the two-wheel prefix can describe.
        return parts.length == 4
            ? new String[]{parts[1], parts[2], parts[3],
                String.valueOf(wheelOfLabel(parts[1]))}
            : null;
    }

    /** Whether this href field names a wheel. */
    private static boolean readsAsWheel(String field) {
        try {
            int wheel = Integer.parseInt(field);
            return wheel >= WHEEL_NATAL && wheel <= WHEEL_SKY;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** The wheel field of a parsed href. Parsing already refused anything else. */
    private static int wheelOf(String[] parsed) {
        return readsAsWheel(parsed[3]) ? Integer.parseInt(parsed[3]) : WHEEL_NATAL;
    }

    /** The longitudes of one wheel. */
    private double[] wheelLon(int wheel) {
        return wheel == WHEEL_SKY ? this.skyRing.lon : (wheel == WHEEL_OUTER ? this.outerRing.lon : this.natalRing.lon);
    }

    /** The speeds of one wheel. */
    private double[] wheelSpeed(int wheel) {
        return wheel == WHEEL_SKY ? this.skyRing.speed
            : (wheel == WHEEL_OUTER ? this.outerRing.speed : this.natalRing.speed);
    }

    /** Which points of one wheel are computed. */
    private boolean[] wheelValid(int wheel) {
        return wheel == WHEEL_SKY ? this.skyRing.valid
            : (wheel == WHEEL_OUTER ? this.outerRing.valid : this.natalRing.valid);
    }

    /** The registry index for a grid label, which may carry the transit prefix. -1 if unknown. */
    private static int bodyIndexOfLabel(String label) {
        String name = label.startsWith(TRANSIT_PREFIX) ? label.substring(TRANSIT_PREFIX.length()) : label;
        for (int i = 0; i < BODY_COUNT; i++) {
            if (BODY_NAMES[i].equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The hover card for one cell of the aspect grid.
     *
     * Deliberately the same shape as {@link #hoverHtml} - a table-free stack of divs, because
     * Swing tooltips render anything cleverer as literal markup. The summary line is the bold
     * lead of the interpretation rather than a separate one-liner: since 2026-08-22 that lead
     * is guaranteed to be a standing finding rather than a restatement of the pair's names,
     * which is exactly what a hover wants.
     */
    public String aspectHoverHtml(String href) {
        String[] parsed = parseAspectHref(href);
        if (parsed == null) {
            return null;
        }
        int wheel = wheelOf(parsed);
        boolean transit = wheel != WHEEL_NATAL;
        int a = bodyIndexOfLabel(parsed[0]);
        int b = bodyIndexOfLabel(parsed[1]);
        Aspects.Type type = Aspects.Type.fromLabel(parsed[2]);
        if (a < 0 || b < 0 || type == null) {
            return null;
        }
        double lonA = this.wheelLon(wheel)[a];
        double lonB = this.natalRing.lon[b];
        double speedA = this.wheelSpeed(wheel)[a];
        double speedB = this.natalRing.speed[b];

        double sep = Math.abs(lonA - lonB);
        if (sep > 180.0) {
            sep = 360.0 - sep;
        }
        double orb = Math.abs(sep - type.exactAngle);
        int orbDeg = (int) orb;
        int orbMin = (int) Math.round((orb - orbDeg) * 60.0);
        if (orbMin == 60) {
            orbDeg++;
            orbMin = 0;
        }
        boolean applying = Aspects.isApplying(lonA, speedA, lonB, speedB, type);

        StringBuilder sb = new StringBuilder();
        // <b>Its own background and its own ink, because this card lands in two places.</b>
        // It is a tooltip over the wheel and it is also the top of the Selection drawer -
        // selectionHtml builds on it - and it set neither colour, so it inherited whatever it
        // landed on. Over a pale tooltip the default black read fine; in the dark drawer the
        // point's name was black on near-black, which is what David saw. A fragment reused on
        // two surfaces has to carry its own contrast rather than borrow one.
        sb.append("<html><body style='width:250px; font-family:SansSerif; font-size:11px;"
            + " background:#12151A; color:#E0E0E0;'>");
        sb.append("<div style='font-size:13px;'><b>").append(BODY_NAMES[a]);
        if (transit) {
            // Named for its own ring: "(partner)" and "(sky)" are different claims about the
            // same glyph, and the card used to say the outer wheel's word for both.
            sb.append(" <span style='color:#5A7FBF;'>(")
                  .append(wheel == WHEEL_SKY ? SKY_RING_WORD : this.outerRingWord())
                  .append(")</span>");
        }
        sb.append("</b> <span style='color:").append(this.getAspectColorHex(type.label))
          .append("; font-size:15px;'>").append(this.getAspectSymbol(type.label))
          .append("</span> <b>").append(BODY_NAMES[b]).append("</b></div>");

        sb.append("<div>").append(type.label)
          .append(" &nbsp;&middot;&nbsp; exact at ").append((int) type.exactAngle).append("&deg;")
          .append("</div>");

        sb.append("<div><b>Orb ").append(orbDeg).append("&deg;")
          .append(orbMin < 10 ? "0" : "").append(orbMin).append("'</b> ")
          .append(applying ? "<span style='color:#C08A3E;'>applying</span>"
                           : "<span style='color:#6E8CA0;'>separating</span>")
          .append(" &nbsp;&middot;&nbsp; ").append(String.format("%.2f", sep)).append("&deg; apart")
          .append("</div>");

        sb.append("<div style='color:#9FB4C7;'>")
          .append(SkymapPanel.degreeLabel(lonA)).append(" &nbsp;&mdash;&nbsp; ")
          .append(SkymapPanel.degreeLabel(lonB)).append("</div>");

        String lead = aspectSummary(parsed[0], parsed[1], type.label, wheel);
        if (lead != null && !lead.isEmpty()) {
            sb.append("<div style='margin-top:4px; color:#8FA98F;'>").append(lead).append("</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /** "14&deg;07' Leo" for a longitude. */
    private static String degreeLabel(double lon) {
        int deg = (int) (lon % 30.0);
        int min = (int) ((lon % 30.0 - deg) * 60.0);
        return deg + "&deg;" + (min < 10 ? "0" : "") + min + "' "
            + SkymapPanel.capitalise(Zodiac.SIGNS[Zodiac.signIndex(lon)]);
    }

    /**
     * The bold lead of a prose entry - the one-sentence finding - or null.
     *
     * Named because four surfaces want it now: the aspect hover card, the house-overlay rows,
     * the angle-contact rows and the tarot line in InterpretationPanel. It was written inline
     * inside the hover card, and a second and third copy of five lines of string arithmetic is
     * how the leads would end up trimmed differently in several places. Package-private for
     * the fourth caller rather than copied into it.
     */
    static String boldLead(String prose) {
        if (prose == null) {
            return null;
        }
        int open = prose.indexOf("<b>");
        int close = prose.indexOf("</b>", open + 3);
        return open >= 0 && close > open ? prose.substring(open + 3, close) : null;
    }

    /** The bold lead of the interpretation - the one-sentence finding - or null. */
    private String aspectSummary(String rowLabel, String colBody, String type, int wheel) {
        try {
            InterpretationService svc = InterpretationService.getInstance();
            // <b>The same fork as the click handler, and it has to stay the same fork.</b> A
            // synastry row carries the transit prefix but is not a transit, so hovering used
            // to summarise a passing event for a standing relationship - while clicking the
            // identical cell now shows the synastry reading. Two surfaces, one rule.
            String body;
            boolean transit = wheel != WHEEL_NATAL;
            // <b>The sky ring is a moment, whatever chart it is drawn over.</b> A synastry
            // chart with the sky ring open has two kinds of cross-chart row on the grid at
            // once, and only the partner one is about two people.
            if (wheel == WHEEL_OUTER && this.isSynastryChart()) {
                // Same order of preference as the click, so the summary a reader hovers is the
                // lead of the paragraph they get when they click. Two surfaces, one rule -
                // this method has already been wrong about that once.
                String outer = rowLabel.substring(TRANSIT_PREFIX.length());
                String pair = svc.getSynastryInteraspect(outer, colBody, type);
                body = pair != null ? pair : svc.getAspect(outer, colBody, type);
            } else if (transit) {
                body = svc.getTransitAspect(rowLabel.substring(TRANSIT_PREFIX.length()), colBody, type);
            } else {
                body = svc.getAspect(rowLabel, colBody, type);
            }
            if (body == null || body.startsWith("Interpretation not found")
                || body.startsWith("Transit aspect interpretation not found")) {
                return null;
            }
            return SkymapPanel.boldLead(body);
        } catch (Exception e) {
            // A tooltip is not worth throwing out of a mouse-moved handler for.
            return null;
        }
    }

    /**
     * Mark one aspect for emphasis on the wheel. Returns true if the highlight changed, so the
     * caller can repaint only when something actually moved - this runs from mouseMoved.
     */
    public boolean setHighlightedAspect(String href) {
        String[] parsed = parseAspectHref(href);
        int a = -1;
        int b = -1;
        int wheel = WHEEL_NATAL;
        if (parsed != null) {
            wheel = wheelOf(parsed);
            a = bodyIndexOfLabel(parsed[0]);
            b = bodyIndexOfLabel(parsed[1]);
            if (a < 0 || b < 0) {
                a = -1;
                b = -1;
            }
        }
        if (a == this.highlightA && b == this.highlightB && wheel == this.highlightWheel) {
            return false;
        }
        this.highlightA = a;
        this.highlightB = b;
        this.highlightWheel = wheel;
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
        return true;
    }

    /**
     * Light a whole aspect pattern on the wheel, by body name. Null or empty clears it.
     *
     * Returns true when the highlight changed, the same contract as
     * {@link #setHighlightedAspect}, so a caller can repaint only when something moved.
     *
     * Names that are not registry points are dropped rather than refused: the caller is a
     * panel rendering whatever the engine handed it, and a figure is still worth lighting if
     * one member happens to be switched off on the wheel.
     */
    public boolean setHighlightedPattern(java.util.List<String> bodyNames) {
        int[] next = patternIndices(bodyNames);
        if (java.util.Arrays.equals(next, this.highlightPattern)) {
            return false;
        }
        this.highlightPattern = next;
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
        return true;
    }

    /**
     * Body names to registry indices, dropping anything that is not a registry point.
     *
     * Static and pure so it can be checked without building a panel. A live SkymapPanel casts
     * a chart and raises Swing components, which is minutes per run and tests the ephemeris
     * rather than this - AspectGridCheck's own header says the same about the grid.
     */
    static int[] patternIndices(java.util.List<String> bodyNames) {
        if (bodyNames == null || bodyNames.isEmpty()) {
            return new int[0];
        }
        int[] buffer = new int[bodyNames.size()];
        int n = 0;
        for (String name : bodyNames) {
            int i = Bodies.indexOfName(name);
            if (i >= 0 && i < BODY_COUNT) {
                buffer[n++] = i;
            }
        }
        return java.util.Arrays.copyOf(buffer, n);
    }

    /** True when both ends of a natal line are members of this pattern. Static for the same reason. */
    static boolean patternCovers(int[] members, int a, int b, boolean transit) {
        if (transit || members == null || members.length == 0 || a == b) {
            return false;
        }
        boolean hasA = false;
        boolean hasB = false;
        for (int i : members) {
            if (i == a) hasA = true;
            if (i == b) hasB = true;
        }
        return hasA && hasB;
    }

    /**
     * True when both ends of a natal line belong to one figure that is currently lit -
     * either every pattern in the chart, or the one the reader opened.
     */
    private boolean isPatternMemberPair(int a, int b, boolean transit) {
        if (patternCovers(this.highlightPattern, a, b, transit)) {
            return true;
        }
        for (int[] members : this.autoPatterns) {
            if (patternCovers(members, a, b, transit)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The 28 lunar mansions as a ring: a boundary tick per station, its number, and the
     * Moon's own station picked out as an arc.
     *
     * <b>The screen angle convention is the wheel's, not Java's.</b> A longitude maps to
     * {@code 180 + pin - lon} and is then used with {@code cos}/{@code sin} against a
     * y-down coordinate system, so an arc drawn with {@link Graphics2D#drawArc} - which
     * measures counterclockwise as displayed - needs that angle negated. Getting this wrong
     * produces a ring that is mirrored rather than absent, which is the kind of error that
     * survives a screenshot.
     *
     * Defensive throughout: this is decoration on a chart that must draw with or without it.
     */
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
    private void drawInnerDegreeRing(Graphics2D g, int cx, int cy, int outer, int inner,
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

    private void drawBoundRing(Graphics2D g, int cx, int cy, int outer, int inner, double pin) {
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
                if (bi < 0 || bi >= BODY_GLYPHS.length) {
                    continue;                   // unresolvable ruler: leave the segment blank
                }
                double c = Math.toRadians(180.0 + pin - centreLon);
                int gx = cx + (int) (mid * Math.cos(c));
                int gy = cy + (int) (mid * Math.sin(c));
                g.setColor(this.bodyColor(bi));
                String glyph = BODY_GLYPHS[bi];
                g.drawString(glyph, gx - g.getFontMetrics().stringWidth(glyph) / 2, gy + 4);
            }
        }
        g.setFont(was);
    }

    private void drawMansionRing(Graphics2D g, int cx, int cy, int outer, int inner,
                                 double pin) {
        try {
            java.util.List<com.zodiacomputing.ourania.astro.LunarMansions.Mansion> all =
                com.zodiacomputing.ourania.astro.LunarMansions.all();
            com.zodiacomputing.ourania.astro.LunarMansions.Mansion moonMansion =
                this.moonMansion();

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
                Color mansion = ChartPalette.colorOr(ChartPalette.mansionHex(null),
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

    /**
     * Recompute the figures in the current chart and light them.
     *
     * Called from the one place that already runs once per chart change. Failure here must
     * never take the chart down with it: a wheel that draws without its patterns is a
     * degraded chart, a wheel that throws is no chart at all.
     */
    private void refreshPatterns() {
        java.util.List<com.zodiacomputing.ourania.astro.AspectPatterns.Pattern> found =
            new java.util.ArrayList<>();
        try {
            com.zodiacomputing.ourania.astro.ChartFrame frame = this.getCurrentChart();
            if (frame != null) {
                found = com.zodiacomputing.ourania.astro.AspectPatterns.findPatterns(frame,
                    com.zodiacomputing.ourania.astro.Aspects.betweenBodies(frame));
            }
        } catch (Exception e) {
            found = new java.util.ArrayList<>();
        }
        this.currentPatterns = found;
        int[][] members = new int[found.size()][];
        for (int i = 0; i < found.size(); i++) {
            members[i] = patternIndices(found.get(i).bodies);
        }
        this.autoPatterns = members;
    }

    /** True when this line is the one the grid is hovering, or part of the lit pattern. */
    private boolean isHighlighted(int a, int b, int wheel) {
        if (isPatternMemberPair(a, b, wheel != WHEEL_NATAL)) {
            return true;
        }
        if (this.highlightA < 0 || wheel != this.highlightWheel) {
            return false;
        }
        // Natal lines are drawn once per unordered pair, so match either ordering.
        return wheel != WHEEL_NATAL
            ? (a == this.highlightA && b == this.highlightB)
            : (a == this.highlightA && b == this.highlightB)
              || (a == this.highlightB && b == this.highlightA);
    }

    public void triggerPlanetInterpretation(String string) {
        if (string == null) {
            return;
        }
        try {
            double d;
            boolean[] blArray;
            // Aspect hrefs are pipe-delimited and are parsed before the underscore split,
            // because their first field ("transit_sun") legitimately contains underscores.
            String[] aspectParts = parseAspectHref(string);
            if (aspectParts != null) {
                // A transit row wants the transit reading, not the natal one. getAspect would
                // be handed "transit_sun" as a body name and return "Interpretation not found".
                if (wheelOf(aspectParts) != WHEEL_NATAL) {
                    // <b>A synastry row is not a transit row.</b> Both carry the prefix,
                    // because both put the outer wheel first, and until 2026-08-24 both went
                    // to the transit reading - so clicking a cell about two people produced
                    // the heading "Transiting Mars Square natal Venus" and a paragraph about
                    // a passing event. The geometry is shared; the meaning is not.
                    String outer = aspectParts[0].substring(TRANSIT_PREFIX.length());
                    // The sky ring is a moment even in a synastry chart. Same fork as
                    // aspectSummary, which is what keeps hover and click saying one thing.
                    if (wheelOf(aspectParts) == WHEEL_OUTER && this.isSynastryChart()) {
                        this.window.showInterpretationForSynastryAspect(
                            outer, aspectParts[1], aspectParts[2]);
                    } else {
                        this.window.showInterpretationForTransitAspect(
                            outer, aspectParts[1], aspectParts[2]);
                    }
                } else {
                    this.window.showInterpretationForAspect(aspectParts[0], aspectParts[1], aspectParts[2]);
                }
                return;
            }
            String[] stringArray = string.split("_");
            if (stringArray[0].equals("aspect") && stringArray.length == 4) {
                this.window.showInterpretationForAspect(stringArray[1], stringArray[2], stringArray[3]);
                return;
            }
            boolean isTransit = stringArray[0].equals("transit");
            boolean isSky = stringArray[0].equals("sky") || stringArray[0].equals("tri");
            int n = Integer.parseInt(stringArray[1]);
            double[] dArray = isSky ? this.skyRing.lon : (isTransit ? this.outerRing.lon : this.natalRing.lon);
            blArray = isSky ? this.skyRing.valid : (isTransit ? this.outerRing.valid : this.natalRing.valid);
            double[] speedArray = isSky ? this.skyRing.speed : (isTransit ? this.outerRing.speed : this.natalRing.speed);
            if (n < 0 || n >= BODY_COUNT || !blArray[n]) {
                return;
            }
            if (Bodies.at(n).isAngle()) {
                // isSky carries the prefix through, so a sky_ / tri_ link from the placements
                // panel reads both charts exactly as clicking the glyph on the wheel does.
                this.showAngleAt(n, dArray[n], isSky,
                    this.angleRoleFor(isSky, isTransit));
                return;
            }
            double d2 = dArray[n];
            int n2 = (int)(d2 / 30.0);
            int n3 = (int)(d2 % 30.0 / 10.0) + 1;
            int n4 = (int)(d2 % 30.0) + 1;
            int n5 = 1;
            for (int i = 1; i <= 12; ++i) {
                double d3;
                double d4 = this.activeCusps[i];
                double d5 = d3 = i == 12 ? this.activeCusps[1] : this.activeCusps[i + 1];
                if (d3 < d4) {
                    d3 += 360.0;
                }
                if ((d = d2) < d4 && d3 > 360.0) {
                    d += 360.0;
                }
                if (!(d >= d4) || !(d < d3)) continue;
                n5 = i;
                break;
            }
            ArrayList<String[]> arrayList = new ArrayList<String[]>();
            if (isSky) {
                for (int i = 0; i < BODY_COUNT; ++i) {
                    if (!this.natalRing.valid[i]) continue;
                    double d6 = Math.abs(d2 - this.natalRing.lon[i]);
                    if (d6 > 180.0) d6 = 360.0 - d6;
                    String string2 = this.getAspectType(d6, n, i, this.isSynastryPair(false));
                    if (string2 == null) continue;
                    boolean blA = this.isAspectApplying(d2, speedArray[n], this.natalRing.lon[i], this.natalRing.speed[i], string2);
                    String string3 = this.describeAspect(d6, string2, n, i, d2, this.natalRing.lon[i], blA);
                    arrayList.add(new String[]{BODY_NAMES[i], string2, string3, "Chart A"});
                }
                for (int i = 0; i < BODY_COUNT; ++i) {
                    if (!this.outerRing.valid[i]) continue;
                    double d6 = Math.abs(d2 - this.outerRing.lon[i]);
                    if (d6 > 180.0) d6 = 360.0 - d6;
                    String string2 = this.getAspectType(d6, n, i, this.isSynastryPair(false));
                    if (string2 == null) continue;
                    boolean blB = this.isAspectApplying(d2, speedArray[n], this.outerRing.lon[i], this.outerRing.speed[i], string2);
                    String string3 = this.describeAspect(d6, string2, n, i, d2, this.outerRing.lon[i], blB);
                    arrayList.add(new String[]{BODY_NAMES[i], string2, string3, "Chart B"});
                }
            } else {
                for (int i = 0; i < BODY_COUNT; ++i) {
                    String string2;
                    if (n == i || !SkymapPanel.aspecting(i, this.natalRing.valid) || Bodies.isOppositePair(n, i)) continue;
                    double d6 = Math.abs(d2 - this.natalRing.lon[i]);
                    if (d6 > 180.0) {
                        d6 = 360.0 - d6;
                    }
                    if ((string2 = this.getAspectType(d6, n, i, this.isSynastryPair(isTransit))) == null) continue;
                    d = speedArray[n];
                    boolean bl2 = this.isAspectApplying(d2, d, this.natalRing.lon[i], this.natalRing.speed[i], string2);
                    String string3 = this.describeAspect(d6, string2, n, i, d2, this.natalRing.lon[i], bl2);
                    arrayList.add(new String[]{BODY_NAMES[i], string2, string3});
                }
            }
            String string4 = (isTransit || isSky) ? "transit_" + BODY_NAMES[n].toLowerCase() : BODY_NAMES[n];
            this.window.showInterpretationForPlanet(string4, SIGN_NAMES[n2], n4, n3, n5, arrayList);
        }
        catch (Exception exception) {
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\error_log.txt"));
                exception.printStackTrace(pw);
                pw.close();
            } catch (Exception ex) {}
            exception.printStackTrace();
        }
    }

    public List<String[]> getActiveAspectsFor(int n, boolean bl) {
        double d;
        boolean[] blArray;
        ArrayList<String[]> arrayList = new ArrayList<String[]>();
        double[] dArray = bl ? this.outerRing.lon : this.natalRing.lon;
        boolean[] blArray2 = blArray = bl ? this.outerRing.valid : this.natalRing.valid;
        if (n < 0 || n >= BODY_COUNT || !blArray[n]) {
            return arrayList;
        }
        double d2 = dArray[n];
        double d3 = d = bl ? this.outerRing.speed[n] : this.natalRing.speed[n];
        if (Bodies.at(n).isAngle()) {
            d = 361.0;
        }
        for (int i = 0; i < BODY_COUNT; ++i) {
            String string;
            if (n == i || !SkymapPanel.aspecting(i, this.natalRing.valid) || Bodies.isOppositePair(n, i)) continue;
            double d4 = Math.abs(d2 - this.natalRing.lon[i]);
            if (d4 > 180.0) {
                d4 = 360.0 - d4;
            }
            if ((string = this.getAspectType(d4, n, i, this.isSynastryPair(bl))) == null) continue;
            boolean bl2 = this.isAspectApplying(d2, d, this.natalRing.lon[i], this.natalRing.speed[i], string);
            String string2 = this.describeAspect(d4, string, n, i, d2, this.natalRing.lon[i], bl2);
            arrayList.add(new String[]{BODY_NAMES[i], string, string2});
        }
        return arrayList;
    }

    /**
     * How far a point is from the chord drawn between two longitudes, in pixels.
     *
     * <b>The one statement of where an aspect line is.</b> The click test carried this inline;
     * the hover needs the same answer, and a second copy of it is how a line you can click
     * becomes a line you cannot hover - or worse, the reverse, where the two disagree by a few
     * pixels and the chart lights one line while opening another.
     */
    private static double chordDistance(int x, int y, int cx, int cy, double pin,
            int discA, int discB, double lonA, double lonB) {
        double ra = Math.toRadians(180.0 + pin - lonA);
        double rb = Math.toRadians(180.0 + pin - lonB);
        double ax = cx + discA * Math.cos(ra);
        double ay = cy + discA * Math.sin(ra);
        double bx = cx + discB * Math.cos(rb);
        double by = cy + discB * Math.sin(rb);
        double len = (ax - bx) * (ax - bx) + (ay - by) * (ay - by);
        if (len <= 0.0) {
            return Math.hypot(x - ax, y - ay);
        }
        double t = Math.max(0.0, Math.min(1.0,
            ((x - ax) * (bx - ax) + (y - ay) * (by - ay)) / len));
        return Math.hypot(x - (ax + t * (bx - ax)), y - (ay + t * (by - ay)));
    }

    /** How near the cursor has to be to a chord to have meant it. */
    private static final double CHORD_GRAB = 8.0;

    /** Nothing readable at this radius. */
    static final int BAND_NONE = 0;
    /** The decan ring. */
    static final int BAND_DECAN = 1;
    /** The sign ring. */
    static final int BAND_SIGN = 2;
    /** The inner degree scale, which reads a Sabian symbol. */
    static final int BAND_DEGREE = 3;
    /** The bounds ring and the open middle, which name the sign the wedge belongs to. */
    static final int BAND_OPEN = 4;
    /** The Egyptian bounds ring, between the degree scale and the signs. */
    static final int BAND_BOUND = 5;

    /**
     * Which readable ring a radius falls in.
     *
     * <b>One statement, because the tests had drifted from the rings.</b> The click handler
     * carried these bounds inline and they were written for the chain as it stood before the
     * rings were reordered outward: the sign test ran from the body base out to the sign
     * ring's outer edge, one band swallowing the decans, the bounds and the whole inner degree
     * scale, while the decan test read from the sign ring outward to the body top - which is
     * inside it - so it described an empty band and the decan ring answered a click with
     * silence. A probe walking the wheel from the middle out found every radius from 226 to
     * 500 pixels saying "Virgo", and 500 to 520 saying nothing.
     *
     * Written as arithmetic over the ring chain so it can be checked without a window, and so
     * a ring that moves takes its own hit test with it.
     *
     * @param r        distance from the centre, pixels
     * @param rings    the ring chain from {@link #ringRadii}
     * @param bodyBase the floor of the natal band, where the open middle starts
     */
    static int bandAt(double r, int[] rings, int bodyBase) {
        if (r >= rings[RING_SIGN_OUTER] && r < rings[RING_DECAN_OUTER]) {
            return BAND_DECAN;
        }
        if (r >= rings[RING_SIGN_INNER] && r < rings[RING_SIGN_OUTER]) {
            return BAND_SIGN;
        }
        // Before the open middle, because it lies inside it: the degree scale is a drawn ring
        // and the middle is the space around it.
        if (r >= rings[RING_DEGREE_INNER] && r < rings[RING_TERM_INNER]) {
            return BAND_DEGREE;
        }
        // The bounds ring answers for its own bound. It named the sign until 2026-09-14, for
        // want of a reading - which made a click on a bound open something else.
        if (r >= rings[RING_TERM_INNER] && r < rings[RING_SIGN_INNER]) {
            return BAND_BOUND;
        }
        if (r >= bodyBase && r < rings[RING_SIGN_INNER]) {
            return BAND_OPEN;
        }
        return BAND_NONE;
    }

    /**
     * The aspect line under a point, as {wheel, outer body, natal body}, or null.
     *
     * <b>Hovering a line on the wheel lit nothing, on any ring.</b> mouseMoved asked only
     * which body was under the cursor; the only place an aspect could be hovered was the grid
     * in the drawer, so the chords themselves - the thing a reader is actually looking at -
     * were inert. Every ring is walked here, so the sky ring's chords answer the cursor the
     * same way the natal wheel's do.
     *
     * Nearest wins rather than first: the three fields are nested discs and a cursor between
     * two lines meant the closer one.
     *
     * Gated on aspectInkFor, which is what the painter draws by, so only a line that is on
     * screen can be pointed at.
     */
    private int[] chordAt(int x, int y) {
        Geometry g = this.geometry();
        if (g == null || !this.layerShown(Layer.ASPECTS)) {
            return null;
        }
        // Outside the widest field there is nothing to test, which is most of the wheel and
        // most of the mouse moves.
        if (Math.hypot(x - g.cx, y - g.cy) > g.aspectDisc(2) + CHORD_GRAB) {
            return null;
        }
        double pin = g.pin;
        double near = CHORD_GRAB;
        int[] found = null;
        if (this.drawsNatalAspects()) {
            int disc = g.aspectDisc(0);
            for (int a = 0; a < BODY_COUNT; a++) {
                if (!SkymapPanel.aspecting(a, this.natalRing.valid)) {
                    continue;
                }
                for (int b = a + 1; b < BODY_COUNT; b++) {
                    if (!SkymapPanel.aspecting(b, this.natalRing.valid)
                        || Bodies.isOppositePair(a, b)
                        || this.aspectInkFor(this.natalRing.lon[a], this.natalRing.lon[b], a, b, false) == null) {
                        continue;
                    }
                    double d = chordDistance(x, y, g.cx, g.cy, pin, disc, disc,
                        this.natalRing.lon[a], this.natalRing.lon[b]);
                    if (d < near) {
                        near = d;
                        found = new int[] {WHEEL_NATAL, a, b};
                    }
                }
            }
        }
        if (this.drawsCrossAspects()) {
            for (int wheel = WHEEL_OUTER; wheel <= WHEEL_SKY; wheel++) {
                if (wheel == WHEEL_OUTER ? !this.outerRingDrawn() : !this.triRingDrawn()) {
                    continue;
                }
                int disc = g.aspectDisc(wheel);
                double[] lon = this.wheelLon(wheel);
                boolean[] valid = this.wheelValid(wheel);
                for (int a = 0; a < BODY_COUNT; a++) {
                    if (!SkymapPanel.aspecting(a, valid)) {
                        continue;
                    }
                    for (int b = 0; b < BODY_COUNT; b++) {
                        if (!SkymapPanel.aspecting(b, this.natalRing.valid)
                            || this.aspectInkFor(lon[a], this.natalRing.lon[b], a, b,
                                wheel == WHEEL_OUTER) == null) {
                            continue;
                        }
                        double d = chordDistance(x, y, g.cx, g.cy, pin, disc, disc,
                            lon[a], this.natalRing.lon[b]);
                        if (d < near) {
                            near = d;
                            found = new int[] {wheel, a, b};
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * Point the highlight at one chord, or clear it with null.
     *
     * Reports whether anything moved, the same contract setHighlightedAspect has, so sweeping
     * the cursor along a line repaints once rather than on every pixel of travel.
     */
    private boolean setHighlightedChord(int[] chord) {
        int a = chord == null ? -1 : chord[1];
        int b = chord == null ? -1 : chord[2];
        int wheel = chord == null ? WHEEL_NATAL : chord[0];
        if (a == this.highlightA && b == this.highlightB && wheel == this.highlightWheel) {
            return false;
        }
        this.highlightA = a;
        this.highlightB = b;
        this.highlightWheel = wheel;
        return true;
    }

    private boolean checkAspectHit(int n, int n2, int n3, int n4, double d, int n5, int n6, double d2, double d3, String string, String string2) {
        String string3;
        double d4 = Math.abs(d2 - d3);
        if (d4 > 180.0) {
            d4 = 360.0 - d4;
        }
        // The labels arrive prefixed for the outer wheel, so this method can tell what it is
        // testing without being told.
        //
        // <b>The prefix also has to be stripped before the name is resolved, and it was not
        // being.</b> indexOfPlanetName matches against BODY_NAMES, so "transit_sun" matched
        // nothing and returned -1, planetName(-1) returned null, and Aspects fell to its
        // default width for an unrecognised point - the 8-degree ANGLE_ORB - for the outer
        // body of EVERY transit and synastry aspect-line hit test. The line beside it was
        // drawn on the real orb, so the two disagreed: a transiting Sun (10) drew a line the
        // click could not find between 8 and 10 degrees, and a transiting asteroid (2) was
        // clickable across a band where nothing was drawn.
        // bodyIndexOfLabel already strips the prefix and already existed; indexOfPlanetName
        // does not strip it, which is the whole bug described above. One resolver, not two.
        boolean cross = SkymapPanel.isTransitLabel(string) || SkymapPanel.isTransitLabel(string2);
        int idxA = SkymapPanel.bodyIndexOfLabel(string);
        int idxB = SkymapPanel.bodyIndexOfLabel(string2);
        if ((string3 = this.getAspectType(d4, idxA, idxB, this.isSynastryPair(cross))) != null) {
            double d5;
            double d6;
            double d7;
            // Through chordDistance, which the hover reads too - one statement of where a
            // line is, so pointing at one and clicking it cannot come apart.
            if (chordDistance(n, n2, n3, n4, d, n5, n6, d2, d3) <= CHORD_GRAB) {
                this.window.showInterpretationForAspect(string, string2, string3);
                return true;
            }
        }
        return false;
    }

    private void stepTime() {
        boolean bl;
        int n = this.animationDirection;
        boolean bl2 = this.animateTarget.equals("Natal") || this.animateTarget.equals("Both") || !this.showTransitChart;
        boolean bl3 = bl = (this.animateTarget.equals("Transit") || this.animateTarget.equals("Both")) && this.showTransitChart;
        // <b>The transport moves the sky, and never a birth time.</b> This guarded the
        // composite modes only - "animating Transit there must move the sky, not one of the
        // two birth charts" - and the same sentence is true of a synastry, where the outer
        // wheel is the second person. It was not guarded, so pressing Play on a synastry
        // walked person B's birth moment forward a day at a time and rebuilt their chart
        // under the reader. Every mode but the synastry's own Chart B now steps the sky.
        if (this.isRelationshipChart() || this.isSynastryChart()) {
            if (bl) {
                this.skyRing.time = "Real Time".equals(this.stepAmount)
                    ? ZonedDateTime.now(ZoneId.of(this.skyTimeZoneId))
                    : this.stepped(this.skyRing.time, n);
            }
            return;
        }
        // <b>With no Chart A the inner wheel is the sky, so the sky is what moves.</b>
        // updateChartData copies skyRing.time over natalRing.time whenever the inner wheel is the
        // sky, so the branch below - which moved natalRing.time - was undone by the very next
        // recompute, and Play, Fast and Slow did nothing at all on the chart the app opens
        // onto. Found by ScrubCheck on 2026-09-15, since a scrub steps through here too.
        if (!this.innerIsBirthChart) {
            if (bl2 || bl) {
                this.skyRing.time = "Real Time".equals(this.stepAmount)
                    ? ZonedDateTime.now(ZoneId.of(this.skyTimeZoneId))
                    : this.stepped(this.skyRing.time, n);
            }
            return;
        }
        if ("Real Time".equals(this.stepAmount)) {
            if (bl2) {
                this.natalRing.time = ZonedDateTime.now(ZoneId.of(this.baseTimeZoneId));
            }
            if (bl) {
                this.skyRing.time = ZonedDateTime.now(ZoneId.of(this.skyTimeZoneId));
            }
            return;
        }
        switch (this.stepAmount) {
            case "1 Minute": {
                if (bl2) {
                    this.natalRing.time = this.natalRing.time.plusMinutes(n);
                }
                if (!bl) break;
                this.skyRing.time = this.skyRing.time.plusMinutes(n);
                break;
            }
            case "1 Hour": {
                if (bl2) {
                    this.natalRing.time = this.natalRing.time.plusHours(n);
                }
                if (!bl) break;
                this.skyRing.time = this.skyRing.time.plusHours(n);
                break;
            }
            case "1 Day": {
                if (bl2) {
                    this.natalRing.time = this.natalRing.time.plusDays(n);
                }
                if (!bl) break;
                this.skyRing.time = this.skyRing.time.plusDays(n);
                break;
            }
            case "1 Week": {
                if (bl2) {
                    this.natalRing.time = this.natalRing.time.plusWeeks(n);
                }
                if (!bl) break;
                this.skyRing.time = this.skyRing.time.plusWeeks(n);
                break;
            }
            case "1 Month": {
                if (bl2) {
                    this.natalRing.time = this.natalRing.time.plusMonths(n);
                }
                if (!bl) break;
                this.skyRing.time = this.skyRing.time.plusMonths(n);
                break;
            }
            case "1 Year": {
                if (bl2) {
                    this.natalRing.time = this.natalRing.time.plusYears(n);
                }
                if (!bl) break;
                this.skyRing.time = this.skyRing.time.plusYears(n);
            }
        }
    }

    // ------------------------------------------------------------------ scrubbing

    /**
     * Master list E8: "Drag-to-time scrubbing - transport buttons step time; no timeline slider
     * or drag gesture."
     *
     * <b>A scrub is an offset from where it began, not a run of steps.</b> Every change puts the
     * times back where the scrub started and moves them once by the whole offset, so dragging
     * out and back lands exactly where it began - a run of one-month steps from the 31st would
     * not (31 January, 28 February, 28 March). And it goes through {@link #stepTime} with the
     * offset as its step count, so which chart moves is the transport's own rule: the sky, and
     * never a birth time on a synastry or a composite.
     *
     * Shift+drag on the wheel, or the Scrub slider beside the transport buttons. A plain drag
     * is left to the pan.
     */
    private boolean scrubbing;
    private ZonedDateTime scrubBase;
    private ZonedDateTime scrubSky;
    /** Steps from where the scrub began. */
    int scrubSteps;
    /** Set while a Shift+drag has scrubbed, so its closing click does not also select. */
    private boolean scrubDragged;
    private boolean scrubRefreshQueued;
    /** Screen pixels of drag per step. */
    static final int SCRUB_PX = 10;

    boolean isScrubbing() {
        return this.scrubbing;
    }

    /** The unit a scrub moves in: the Step setting, or an hour when Step follows the clock. */
    String scrubUnit() {
        return "Real Time".equals(this.stepAmount) ? "1 Hour" : this.stepAmount;
    }

    /**
     * What a scrub moves.
     *
     * <b>TRANSPORT</b> is the transport's own rule - the sky, and never a birth time on a
     * synastry or composite - and is what Shift+drag on the wheel uses. The other three are the
     * per-chart bars David asked for: "different scrub bars that control different charts in the
     * wheel and globe". Each moves one moment only, whichever ring it drives, so a birth time
     * can be walked for rectification while the sky holds still, or the sky walked across a
     * relationship without touching either person.
     */
    enum ScrubTarget {
        TRANSPORT("the chart"), CHART_A("Chart A"), CHART_B("Chart B"), SKY("Sky");

        final String label;

        ScrubTarget(String label) {
            this.label = label;
        }
    }

    ScrubTarget scrubTarget = ScrubTarget.TRANSPORT;
    private ZonedDateTime scrubTransit;
    /**
     * A birth time as it was before any bar moved it, so its ↺ can put it back. Kept for Chart A
     * and Chart B only - the sky has no "true" moment to return to - and cleared whenever a chart
     * is cast from Chart Setup, because that is a new birth time rather than a scrubbed one.
     */
    final java.util.EnumMap<ScrubTarget, ZonedDateTime> scrubOrigins =
        new java.util.EnumMap<>(ScrubTarget.class);

    void beginScrub() {
        this.beginScrub(ScrubTarget.TRANSPORT);
    }

    void beginScrub(ScrubTarget target) {
        if (this.scrubbing) {
            return;
        }
        this.isPlaying = false;
        this.scrubTarget = target;
        this.scrubBase = this.natalRing.time;
        this.scrubSky = this.skyRing.time;
        this.scrubTransit = this.outerRing.time;
        if (target == ScrubTarget.CHART_A && this.natalRing.time != null) {
            this.scrubOrigins.putIfAbsent(target, this.natalRing.time);
        }
        if (target == ScrubTarget.CHART_B && this.outerRing.time != null) {
            this.scrubOrigins.putIfAbsent(target, this.outerRing.time);
        }
        this.scrubSteps = 0;
        this.scrubbing = true;
    }

    /** Moves the chart to this many steps from where the scrub began. */
    void scrubTo(int steps) {
        if (!this.scrubbing) {
            this.beginScrub();
        }
        if (steps == this.scrubSteps) {
            return;
        }
        this.natalRing.time = this.scrubBase;
        this.skyRing.time = this.scrubSky;
        this.outerRing.time = this.scrubTransit;
        String unit = this.stepAmount;
        int direction = this.animationDirection;
        try {
            this.stepAmount = this.scrubUnit();
            this.animationDirection = steps;
            switch (this.scrubTarget) {
                case CHART_A:
                    this.natalRing.time = this.stepped(this.scrubBase, steps);
                    break;
                case CHART_B:
                    this.outerRing.time = this.stepped(this.scrubTransit, steps);
                    break;
                case SKY:
                    this.skyRing.time = this.stepped(this.scrubSky, steps);
                    break;
                default:
                    if (steps != 0) {
                        this.stepTime();
                    }
            }
        } finally {
            this.stepAmount = unit;
            this.animationDirection = direction;
        }
        this.scrubSteps = steps;
        this.requestScrubRefresh();
    }

    /** Puts a scrubbed birth time back where it was before any bar moved it. */
    void resetScrub(ScrubTarget target) {
        ZonedDateTime origin = this.scrubOrigins.remove(target);
        if (origin == null) {
            return;
        }
        if (target == ScrubTarget.CHART_A) {
            this.natalRing.time = origin;
        } else if (target == ScrubTarget.CHART_B) {
            this.outerRing.time = origin;
        }
        this.updateChartData();
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
    }

    /** The per-chart bars, by what they move. */
    final java.util.EnumMap<ScrubTarget, javax.swing.JSlider> scrubSliders =
        new java.util.EnumMap<>(ScrubTarget.class);
    /** Each bar's label, slider and reset, shown and hidden together. */
    final java.util.EnumMap<ScrubTarget, JPanel> scrubBars = new java.util.EnumMap<>(ScrubTarget.class);
    final java.util.EnumMap<ScrubTarget, JButton> scrubResets = new java.util.EnumMap<>(ScrubTarget.class);

    /**
     * Shows the bars for the charts that are on the wheel.
     *
     * Chart A's bar only when Chart A is a birth chart - with no Chart A the inner wheel IS the
     * sky, and a second bar moving the same moment would be two controls for one thing. Chart
     * B's on a synastry or a composite, where there is a second person. The sky's always.
     */
    void refreshScrubBars() {
        boolean relationship = this.isRelationshipChart();
        this.showBar(ScrubTarget.CHART_A, this.innerIsBirthChart || relationship);
        this.showBar(ScrubTarget.CHART_B, this.isSynastryChart() || relationship);
        this.showBar(ScrubTarget.SKY, true);
        for (java.util.Map.Entry<ScrubTarget, JButton> e : this.scrubResets.entrySet()) {
            e.getValue().setEnabled(this.scrubOrigins.containsKey(e.getKey()));
        }
    }

    private void showBar(ScrubTarget target, boolean on) {
        JPanel bar = this.scrubBars.get(target);
        if (bar != null && bar.isVisible() != on) {
            bar.setVisible(on);
            if (bar.getParent() != null) {
                bar.getParent().revalidate();
            }
        }
    }

    /** One labelled bar: the chart's name, its slider, and for a birth chart a reset. */
    private JPanel scrubBar(ScrubTarget target) {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        JLabel label = new JLabel(target.label);
        label.setForeground(Theme.TEXT_DIM);
        label.setFont(Theme.SMALL);
        // Room after the name: the slider's track starts at its bounds and clipped the last
        // letter, so "Chart B" read "Chart E".
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 4));
        javax.swing.JSlider slider = SkymapPanel.scrubSlider(this, target);
        slider.setPreferredSize(new java.awt.Dimension(170, 24));
        bar.add(label);
        bar.add(slider);
        if (target == ScrubTarget.CHART_A || target == ScrubTarget.CHART_B) {
            JButton reset = new JButton("↺");
            Widgets.styleButton(reset, Widgets.Role.TRANSPORT);
            reset.setFont(Theme.font("Segoe UI", Font.PLAIN, 13));
            reset.setToolTipText("Put " + target.label + "'s birth time back where it was");
            reset.setEnabled(false);
            reset.addActionListener(e -> {
                this.resetScrub(target);
                this.refreshScrubBars();
            });
            bar.add(reset);
            this.scrubResets.put(target, reset);
        }
        this.scrubSliders.put(target, slider);
        this.scrubBars.put(target, bar);
        return bar;
    }

    /** Ends the scrub where it stands, and draws that moment at once. */
    void endScrub() {
        if (!this.scrubbing) {
            return;
        }
        this.scrubbing = false;
        this.scrubBase = null;
        this.scrubSky = null;
        this.scrubRefreshQueued = false;
        this.updateChartData();
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
    }

    /**
     * One recompute for however many drag events arrived before it could run.
     *
     * A drag delivers an event per pixel; recomputing the chart for each would queue work
     * faster than it is done and the wheel would trail the mouse by seconds.
     */
    private void requestScrubRefresh() {
        if (this.scrubRefreshQueued) {
            return;
        }
        this.scrubRefreshQueued = true;
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (!this.scrubRefreshQueued) {
                return;
            }
            this.scrubRefreshQueued = false;
            this.updateChartData();
            if (this.chartPanel != null) {
                this.chartPanel.repaint();
            }
        });
    }

    /** The sky's scrub bar, kept for the check. */
    javax.swing.JSlider scrubSlider;
    /** The row the per-chart scrub bars sit on. */
    private JPanel scrubRow;

    /** How many steps the slider reaches each way from where the scrub began. */
    static final int SCRUB_SLIDER_REACH = 60;

    /**
     * A slider that springs back: drag the knob and the chart follows it, let go and the chart
     * stays where it was put while the knob returns to the middle, ready to go again.
     *
     * <b>Why it springs back.</b> A slider with a fixed range pinned to a date would need a
     * range, and any range is wrong - a day's worth for a transit, a century for a progression.
     * Springing back makes the range relative: sixty steps of the Step setting each way, from
     * wherever the chart is, as many times as the reader likes. An arrow key on the focused
     * slider is a single step.
     */
    static javax.swing.JSlider scrubSlider(SkymapPanel panel, ScrubTarget target) {
        javax.swing.JSlider slider = new javax.swing.JSlider(-SCRUB_SLIDER_REACH, SCRUB_SLIDER_REACH, 0);
        slider.setOpaque(false);
        slider.setPreferredSize(new java.awt.Dimension(220, 26));
        slider.setToolTipText(target == ScrubTarget.SKY
            ? "Drag to move the sky through time by the Step setting. Hold it pulled and time keeps "
                + "going that way, faster the further you pull; it stays where you let go."
            : "Drag to move " + target.label + "'s birth time by the Step setting - for trying a "
                + "time, not saving one. Hold it pulled and time keeps going until you let go. "
                + "The ↺ beside it puts the birth time back.");
        final boolean[] resetting = {false};
        // Steps run up while the knob is held off the dead zone, on top of where the knob sits.
        final double[] travel = {0.0};
        // <b>By the clock, not by the tick.</b> Each tick recomputes the chart on the event thread,
        // and Swing coalesces ticks that pile up behind it: counted per tick, a bar held at 48 for
        // two seconds ran 6 steps where its rate said 23.
        final long[] lastTick = {0L};
        final javax.swing.Timer shuttle = new javax.swing.Timer(SHUTTLE_TICK_MS, null);
        shuttle.addActionListener(ev -> {
            if (!slider.getValueIsAdjusting() || !panel.isScrubbing()) {
                shuttle.stop();
                return;
            }
            long now = System.nanoTime();
            travel[0] += shuttleRate(slider.getValue()) * (now - lastTick[0]) / 1.0e9;
            lastTick[0] = now;
            panel.scrubTo(slider.getValue() + (int) travel[0]);
            if (panel.chartPanel != null) {
                panel.chartPanel.repaint();
            }
        });
        slider.addChangeListener(e -> {
            if (resetting[0]) {
                return;
            }
            panel.beginScrub(target);
            panel.scrubTo(slider.getValue() + (int) travel[0]);
            if (panel.chartPanel != null) {
                panel.chartPanel.repaint();
            }
            if (slider.getValueIsAdjusting()) {
                if (!shuttle.isRunning()) {
                    lastTick[0] = System.nanoTime();
                    shuttle.start();
                }
                return;
            }
            shuttle.stop();
            travel[0] = 0.0;
            panel.endScrub();
            panel.refreshScrubBars();
            resetting[0] = true;
            try {
                slider.setValue(0);
            } finally {
                resetting[0] = false;
            }
        });
        return slider;
    }

    /** How often a held bar redraws while it runs on, in milliseconds; the distance is by the clock. */
    static final int SHUTTLE_TICK_MS = 50;
    /** Steps a second with the knob pulled all the way. */
    static final double SHUTTLE_MAX_RATE = 20.0;
    /** Knob positions either side of the middle that only offset, and do not run on. */
    static final int SHUTTLE_DEAD_ZONE = 10;

    /**
     * Steps a second a bar runs at with its knob held here.
     *
     * David: "when you pull them forward or backward can you have the time keep going until
     * released". The knob still offsets the chart by where it sits, so a short flick is still a
     * few steps; held past the dead zone it also runs on, like a shuttle, and the rate climbs with
     * the square of the pull - a gentle pull creeps, a full one covers twenty steps a second.
     */
    static double shuttleRate(int value) {
        int pull = Math.abs(value) - SHUTTLE_DEAD_ZONE;
        if (pull <= 0) {
            return 0.0;
        }
        double share = (double) pull / (SCRUB_SLIDER_REACH - SHUTTLE_DEAD_ZONE);
        return Math.signum(value) * SHUTTLE_MAX_RATE * share * share;
    }

    /** "+3 days", "-1 month": the offset a scrub stands at, in words. */
    static String scrubLabel(int steps, String unit) {
        String word = unit.replaceFirst("^1 ", "").toLowerCase();
        String sign = steps > 0 ? "+" : steps < 0 ? "−" : "±";
        int n = Math.abs(steps);
        return sign + n + " " + word + (n == 1 ? "" : "s");
    }

    /** The offset, over the top of the wheel while a scrub is running. */
    void paintScrubTag(Graphics2D g2, int w) {
        if (!this.scrubbing) {
            return;
        }
        String text = "Scrubbing " + (this.scrubTarget == ScrubTarget.TRANSPORT ? "" : this.scrubTarget.label + " ")
            + scrubLabel(this.scrubSteps, this.scrubUnit());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(Theme.font("Segoe UI", Font.BOLD, 13));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text) + 24;
        int x = (w - tw) / 2;
        g2.setColor(new Color(22, 28, 43, 230));
        g2.fillRoundRect(x, 10, tw, 26, 12, 12);
        g2.setColor(new Color(226, 178, 88));
        g2.drawRoundRect(x, 10, tw, 26, 12, 12);
        g2.drawString(text, x + 12, 10 + (26 + fm.getAscent() - fm.getDescent()) / 2);
    }

    /**
     * One time advanced by one step, in whichever unit is selected.
     *
     * Extracted so the composite branch of {@link #stepTime} applies the same units as the
     * switch below it rather than carrying a second copy of them - which is how the two would
     * come to disagree about what "1 Week" means.
     */
    private ZonedDateTime stepped(ZonedDateTime when, int n) {
        if (when == null) {
            return null;
        }
        switch (this.stepAmount) {
            case "1 Minute": return when.plusMinutes(n);
            case "1 Hour":   return when.plusHours(n);
            case "1 Day":    return when.plusDays(n);
            case "1 Week":   return when.plusWeeks(n);
            case "1 Month":  return when.plusMonths(n);
            case "1 Year":   return when.plusYears(n);
            default:         return when;
        }
    }

    /**
     * A subject nobody has given a place to. 0,0 is the Atlantic off Africa, not a choice.
     */
    private static boolean placeless(com.zodiacomputing.ourania.astro.ChartSubject s) {
        return s == null || (s.latitude == 0.0 && s.longitude == 0.0
            && (s.placeName == null || s.placeName.trim().isEmpty()));
    }

    /** Set once the sky's place has been settled, so a missing home is not looked up every frame. */
    private boolean skyPlaceSettled;

    /**
     * <b>The sky is read from where the reader is, and had no place at all.</b>
     *
     * Chart A and Chart B are built from the form every time Generate runs; the sky subject was
     * passed through untouched, and the two calls that do move it - the sky's Now and its zone
     * override - keep the coordinates it already has. Nothing ever gave it any, so it stayed at
     * the empty subject's 0,0 and every house it cast was cast in the Atlantic.
     *
     * It showed up as David's houses: with only the sky on the wheel the Ascendant sat in
     * Sagittarius, switching Chart A on corrected it (the inner wheel then takes Chart A's
     * place), and switching Chart A off put it back. Measured on a fresh install, 2026-09-19:
     * the drawn Ascendant was Gemini 11.52, which is exactly the Ascendant at 0,0 for that
     * moment - Pisces 21 at Philadelphia.
     *
     * So: the home location the reader has already set, which is what "the sky" means; then
     * Chart A's place if there is no home; and if neither exists the sky keeps no place rather
     * than inventing one, because a chart cast at a guessed place is the defect this replaces.
     * Through Geocoder, which answers coordinates and known places from the offline atlas
     * before it reaches for the network.
     */
    private void ensureSkyPlace() {
        if (this.skyPlaceSettled || !placeless(this.subjectSky)) {
            return;
        }
        this.skyPlaceSettled = true;
        String home = Settings.get("home.location",
            Settings.get("default.transit.location", "")).trim();
        Geocoder.Result there = home.isEmpty() ? null : this.syncGeocode(home);
        if (there != null) {
            this.subjectSky = this.subjectSky.movedTo(there.name, there.lat, there.lon,
                there.tzId == null || there.tzId.isEmpty() ? this.skyTimeZoneId : there.tzId);
        } else if (!placeless(this.subjectA)) {
            this.subjectSky = this.subjectSky.movedTo(this.subjectA.placeName,
                this.subjectA.latitude, this.subjectA.longitude, this.subjectA.zoneId);
        } else {
            return;
        }
        this.castRoles();
    }

    /**
     * Where the sky is read from, as the reader typed it. The Sky row's location field's door:
     * it had none, so what the field said and what the wheel cast were never connected.
     */
    void setSkyPlace(String place) {
        if (place == null || place.trim().isEmpty()) {
            return;
        }
        Geocoder.Result there = this.syncGeocode(place.trim());
        if (there == null) {
            return;
        }
        this.skyPlaceSettled = true;
        this.installSubjects(this.subjectA, this.subjectB,
            this.subjectSky.movedTo(there.name, there.lat, there.lon,
                there.tzId == null || there.tzId.isEmpty() ? this.skyTimeZoneId : there.tzId));
        this.updateChartData();
        this.repaintWheel();
    }

    private SweDate createSweDate(ZonedDateTime zonedDateTime) {
        ZonedDateTime zonedDateTime2 = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
        return new SweDate(zonedDateTime2.getYear(), zonedDateTime2.getMonthValue(), zonedDateTime2.getDayOfMonth(), (double)zonedDateTime2.getHour() + (double)zonedDateTime2.getMinute() / 60.0 + (double)zonedDateTime2.getSecond() / 3600.0);
    }

    public void updateChartData() {
        this.invalidateGlobeChords();
        this.ensureSkyPlace();
        if (this.natalRing.time != null) {
            this.natalRing.sd = this.createSweDate(this.natalRing.time);
        }
        if (this.outerRing.time != null) {
            this.outerRing.sd = this.createSweDate(this.outerRing.time);
        }
        if (this.sw == null) {
            return;
        }
        if (this.skyRing.time == null) {
            this.skyRing.time = ZonedDateTime.now(ZoneId.of(this.skyTimeZoneId));
        }
        this.skyRing.sd = this.createSweDate(this.skyRing.time);

        // <b>In a composite mode the INNER wheel is the composite itself.</b> Everything below
        // reads the base arrays, so the composite is loaded into them rather than special-cased
        // through the drawing, the grid, the hit test and the interpretation panel in turn.
        boolean relationship = this.isRelationshipChart();
        if (relationship) {
            // <b>radixChart, not getCurrentChart.</b> The harmonic is applied to the arrays
            // further down, once, for every mode; taking the already-harmonic frame here would
            // multiply by N twice and put H25 on screen with the dropdown reading 5.
            ChartFrame composite = this.radixChart();
            if (composite != null) {
                this.loadFrameIntoBase(composite);
            }
        }

        // <b>With no Chart A the inner wheel is the sky.</b> Not a birth chart cast from
        // whatever moment the field happened to hold - there is no chart, and the honest
        // thing to draw is the one thing that always exists.
        if (!this.innerIsBirthChart && !relationship && this.skyRing.sd != null) {
            this.natalRing.sd = this.skyRing.sd;
            this.natalRing.time = this.skyRing.time;
        }
        double[] dArray = new double[10];
        if (this.natalRing.sd != null && !relationship) {
            double innerLat = this.innerIsBirthChart ? this.natalRing.latitude : this.skyRing.latitude;
            double innerLon = this.innerIsBirthChart ? this.natalRing.longitude : this.skyRing.longitude;
            this.sw.swe_houses(this.natalRing.sd.getJulDay(), com.zodiacomputing.ourania.astro.Ephemeris.flags(this.sw, 2), innerLat, innerLon, this.houseSystem, this.natalRing.cusps, dArray);
            this.natalRing.ascendant = dArray[0];
            System.arraycopy(dArray, 0, this.baseAscmc, 0, this.baseAscmc.length);
        }
        // <b>The outer wheel is a second person only in a synastry.</b> Everywhere else it
        // is the sky, and it was reading the Chart B fields to find out when the sky was -
        // which is why pressing Sky after setting up a partner drew that partner's birth
        // chart where this moment should have been, and pressing Partner after looking at the
        // sky drew this moment where the partner should have been. One pair of fields, two
        // meanings, and the chips switched the meaning without switching the value.
        SweDate outerSd = this.isSynastryChart() ? this.outerRing.sd : this.skyRing.sd;
        // The place follows the moment: Chart B's birthplace for Chart B, the sky's own
        // location for the sky.
        double outerLat = this.isSynastryChart() ? this.outerRing.latitude : this.skyRing.latitude;
        double outerLon = this.isSynastryChart() ? this.outerRing.longitude : this.skyRing.longitude;
        // A day of ephemeris for a year of life. Everything downstream - houses, bodies, the
        // ring, the aspect lines - is unchanged; only the moment it is asked about moves.
        boolean progressedRing = this.showProgressed() && !relationship
            && this.natalRing.sd != null && outerSd != null;
        if (progressedRing) {
            outerSd = new SweDate(com.zodiacomputing.ourania.astro.Progressions.progressedJd(
                this.natalRing.sd.getJulDay(), outerSd.getJulDay()));
        }
        // <b>What the middle ring was actually cast at, kept.</b> The readouts printed
        // outerRing.time beside it, which is Chart B's birth moment - right in a synastry and
        // wrong everywhere else, and wrong in a new way for a progressed ring, whose moment is
        // a date in the reader's infancy that no field on the form holds. One place computes
        // this; now one place remembers it, and the readouts ask rather than guess.
        this.outerCastAt = outerSd;
        if (this.showTransitChart && outerSd != null && !progressedRing) {
            double[] dArray2 = new double[10];
            this.sw.swe_houses(outerSd.getJulDay(), com.zodiacomputing.ourania.astro.Ephemeris.flags(this.sw, 2), outerLat, outerLon, this.houseSystem, this.outerRing.cusps, dArray2);
            this.outerRing.ascendant = dArray2[0];
            System.arraycopy(dArray2, 0, this.transitAscmc, 0, this.transitAscmc.length);
        } else {
            System.arraycopy(this.natalRing.cusps, 0, this.outerRing.cusps, 0, this.outerRing.cusps.length);
            this.outerRing.ascendant = this.natalRing.ascendant;
            System.arraycopy(this.baseAscmc, 0, this.transitAscmc, 0, this.transitAscmc.length);
        }
        if (this.natalRing.sd != null && !relationship) {
            this.computeBodies(this.natalRing.sd, this.natalRing.cusps, this.natalRing.lon, this.natalRing.speed, this.natalRing.ok, this.natalRing.valid);
        }
        if (this.showTransitChart && outerSd != null) {
            this.computeBodies(outerSd, this.outerRing.cusps, this.outerRing.lon, this.outerRing.speed, this.outerRing.ok, this.outerRing.valid);
        } else {
            Arrays.fill(this.outerRing.valid, false);
            Arrays.fill(this.outerRing.ok, false);
        }
        // Tri-wheel: the sky at skyRing.time wrapped around the synastry pair.
        // skyRing.time defaults to now and is driven by the same Now/Play controls,
        // so the animation sweeps the sky without touching either person's birth data.
        if (this.showTriWheel && this.skyRing.sd != null) {
            double[] triAux = new double[10];
            this.sw.swe_houses(this.skyRing.sd.getJulDay(), com.zodiacomputing.ourania.astro.Ephemeris.flags(this.sw, 2),
                this.skyRing.latitude, this.skyRing.longitude, this.houseSystem,
                this.skyRing.cusps, triAux);
            this.skyRing.ascendant = triAux[0];
            System.arraycopy(triAux, 0, this.triAscmc, 0, this.triAscmc.length);
            this.computeBodies(this.skyRing.sd, this.skyRing.cusps,
                this.skyRing.lon, this.skyRing.speed, this.skyRing.ok, this.skyRing.valid);
        } else {
            Arrays.fill(this.skyRing.valid, false);
            Arrays.fill(this.skyRing.ok, false);
        }
        // The harmonic, applied once, after every other source has finished writing positions.
        //
        // <b>Here rather than at each filler.</b> natalRing.lon is written by two different paths - the
        // composite goes through loadFrameIntoBase, everything else through computeBodies - and
        // mapping in both would be one rule in two places. This is the point where the arrays
        // are settled and nothing else has read them yet.
        //
        // The transit wheel is mapped too: a harmonic is a view of the screen, and showing H5
        // bodies against radix transits would be two charts in one wheel.
        if (com.zodiacomputing.ourania.astro.Harmonics.isActive(this.harmonic)) {
            for (int i = 0; i < BODY_COUNT; i++) {
                // <b>Angles are skipped, matching Harmonics.of.</b> The house cusps and the
                // ASC/DSC axis are drawn from natalRing.cusps and natalRing.ascendant, which are never
                // mapped - so mapping the angle GLYPHS here put a harmonic Ascendant marker
                // on a radix horizon. At H5 it had walked to the top of the wheel while the
                // axis line stayed on the left. Caught by rendering the wheel, not by any
                // check; HarmonicCheck Part D now asserts it.
                if (Bodies.at(i).isAngle()) {
                    continue;
                }
                this.natalRing.lon[i] = com.zodiacomputing.ourania.astro.Harmonics.map(
                    this.natalRing.lon[i], this.harmonic);
                this.natalRing.speed[i] *= this.harmonic;
                this.outerRing.lon[i] = com.zodiacomputing.ourania.astro.Harmonics.map(
                    this.outerRing.lon[i], this.harmonic);
                this.outerRing.speed[i] *= this.harmonic;
                this.skyRing.lon[i] = com.zodiacomputing.ourania.astro.Harmonics.map(
                    this.skyRing.lon[i], this.harmonic);
                this.skyRing.speed[i] *= this.harmonic;
            }
        }

        if (this.showTransitChart && "Transit".equals(this.houseAlignment)) {
            System.arraycopy(this.outerRing.cusps, 0, this.activeCusps, 0, this.activeCusps.length);
            this.activeAscendant = this.outerRing.ascendant;
        } else {
            System.arraycopy(this.natalRing.cusps, 0, this.activeCusps, 0, this.activeCusps.length);
            this.activeAscendant = this.natalRing.ascendant;
        }
        if (this.window != null) {
            // Three parts, three accordion sections. One document would mean the
            // sidebar could only show or hide all of it at once, which is what it did before.
            this.window.updateChartSections(
                this.generatePlanetPlacementsHtml(PlacementPart.NATAL),
                this.generatePlanetPlacementsHtml(PlacementPart.TRANSITS),
                this.generatePlanetPlacementsHtml(PlacementPart.GRID));
        }
        this.refreshTimeReadout();
        this.refreshReadingIfShown();
        // Which charts are on the wheel decides which scrub bars there are.
        this.refreshScrubBars();
    }

    /**
     * Copy a computed frame into the inner-wheel arrays.
     *
     * <b>Index-for-index, and that is safe because both are the Bodies registry.</b>
     * ChartFrame.bodies and this panel's natalRing.lon/natalRing.valid are each built by walking
     * {@link Bodies} in order, so entry i is the same body in both. The NAME remains the key
     * anywhere something is looked up rather than indexed.
     *
     * {@code ok} already accounts for the user's body selection - ChartFrame.compute applies
     * Settings.loadBodySelection before returning - so natalRing.valid follows it rather than being
     * recomputed here from a second reading of the settings.
     */
    private void loadFrameIntoBase(ChartFrame frame) {
        System.arraycopy(frame.cusps, 0, this.natalRing.cusps, 0,
            Math.min(frame.cusps.length, this.natalRing.cusps.length));
        this.natalRing.ascendant = frame.asc;
        for (int i = 0; i < BODY_COUNT && i < frame.bodies.length; i++) {
            ChartFrame.Body b = frame.bodies[i];
            boolean usable = b != null && b.ok;
            this.natalRing.lon[i] = usable ? b.lon : 0.0;
            this.natalRing.speed[i] = usable ? b.lonSpeed : 0.0;
            this.natalRing.ok[i] = usable;
            this.natalRing.valid[i] = usable;
        }
    }

    private void computeBodies(SweDate sweDate, double[] dArray, double[] dArray2, double[] dArray3, boolean[] blArray, boolean[] blArray2) {
        int n;
        double d = sweDate.getJulDay();
        for (int i = 0; i < BODY_COUNT; ++i) {
            if (Bodies.at((int)i).source != Bodies.Source.EPHEMERIS) continue;
            double[] dArray4 = new double[6];
            StringBuffer stringBuffer = new StringBuffer();
            if (this.sw.swe_calc_ut(d, Bodies.at(i).getIpl(), com.zodiacomputing.ourania.astro.Ephemeris.flags(this.sw, 258), dArray4, stringBuffer) != -1) {
                dArray2[i] = Zodiac.normalise(dArray4[0]);
                dArray3[i] = dArray4[3];
                blArray[i] = true;
                continue;
            }
            blArray[i] = false;
        }
        double d2 = Zodiac.normalise(dArray[1]);
        double d3 = Zodiac.normalise(dArray[10]);
        // The Vertex and East Point come from the same swe_houses call as these cusps, whose
        // ascmc array was kept beside them for exactly this.
        double[] ascmc = this.ascmcFor(dArray);
        for (n = 0; n < BODY_COUNT; ++n) {
            Bodies.Def def = Bodies.at(n);
            if (def.source == Bodies.Source.EPHEMERIS) continue;
            dArray2[n] = Bodies.derive(def.source, d2, d3, ascmc[3], ascmc[4], dArray2);
            blArray[n] = !Double.isNaN(dArray2[n]) && this.derivable(def.source, blArray);
            dArray3[n] = def.source == Bodies.Source.SOUTH_NODE ? dArray3[NORTH_NODE] : 361.0;
        }
        for (n = 0; n < BODY_COUNT; ++n) {
            blArray2[n] = blArray[n] && this.shown[n];
        }
    }

    private boolean derivable(Bodies.Source source, boolean[] blArray) {
        switch (source) {
            case SOUTH_NODE: {
                return blArray[NORTH_NODE];
            }
            case FORTUNE:
            case SPIRIT: {
                return blArray[SUN] && blArray[MOON];
            }
            case LOT_EROS:
                return blArray[SUN] && blArray[MOON] && blArray[Bodies.indexOf("venus")];
            case LOT_NECESSITY:
                return blArray[SUN] && blArray[MOON] && blArray[Bodies.indexOf("mercury")];
            case LOT_COURAGE:
                return blArray[SUN] && blArray[MOON] && blArray[Bodies.indexOf("mars")];
            case LOT_VICTORY:
                return blArray[SUN] && blArray[MOON] && blArray[Bodies.indexOf("jupiter")];
            case LOT_NEMESIS:
                return blArray[SUN] && blArray[MOON] && blArray[Bodies.indexOf("saturn")];
            default:
                break;
        }
        return true;
    }

    /** The ascmc array of the wheel whose cusps these are: {asc, mc, armc, vertex, east point}. */
    private double[] baseAscmc = new double[10];
    private double[] transitAscmc = new double[10];
    private double[] triAscmc = new double[10];

    private double[] ascmcFor(double[] cusps) {
        if (cusps == this.outerRing.cusps) {
            return this.transitAscmc;
        }
        if (cusps == this.skyRing.cusps) {
            return this.triAscmc;
        }
        return this.baseAscmc;
    }

    public void reloadBodySelection() {
        this.shown = Settings.loadBodySelection();
        this.updateChartData();
        if (this.chartPanel != null) {
            this.chartPanel.repaint();
        }
    }

    private void syncPinToTransitAvailability() {
        if (!this.showTransitChart && "Transit Asc".equals(this.wheelPin)) {
            this.wheelPin = "Natal Asc";
            if (this.pinCombo != null) {
                this.pinCombo.setSelectedItem("Natal Asc");
            }
        }
    }

    private double getPinLongitude() {
        if ("Aries".equals(this.wheelPin)) {
            return 0.0;
        }
        if ("Transit Asc".equals(this.wheelPin) && this.showTransitChart) {
            return this.outerRing.ascendant;
        }
        return this.natalRing.ascendant;
    }

    /** Pure lookup, static so a check can call it without standing up a whole window. */
    /**
     * The colour an aspect is drawn in.
     *
     * <b>Was a fifteen-case switch; now asks {@link ChartPalette}.</b> The switch was already
     * the single source six callers read - wheel, grid, legend, interpretation panel, hover
     * card and Prose - which is why moving it was a one-line change rather than a hunt. What it
     * could not do was vary: there was no template to choose, and a reader who cannot separate
     * the red square from the green trine had no recourse at all.
     *
     * Kept static and kept at this name so those six callers, and AspectGridCheck's reflection
     * into it, all still resolve.
     */
    static String getAspectColorHex(String string) {
        return ChartPalette.aspectHex(string);
    }

    /** Pure lookup, static so a check can call it without standing up a whole window. */
    static String getAspectSymbol(String string) {
        switch (string) {
            case "Conjunction": {
                return "\u260c";
            }
            case "Sextile": {
                return "\u26b9";
            }
            case "Square": {
                return "\u25a1";
            }
            case "Trine": {
                return "\u25b3";
            }
            case "Quincunx": {
                // U+26BB, the inconjunct. This case was missing, so every quincunx cell in the
                // grid fell through to the empty string below: 19 cells in a natal grid and 43
                // in a transit one, rendered as blank white spans. They were links with no text
                // to click or hover, which is why quincunx aspects appeared to do nothing.
                return "\u26bb";
            }
            // The five added 2026-08-23. Unicode has no accepted astrological glyph for most
            // of these, so they take the conventional typographic stand-ins rather than an
            // invented mark: a letter reads as a letter, where a wrong symbol reads as a bug.
            case "Semisextile": {
                return "\u26ba";
            }
            case "Semisquare": {
                return "\u2220";
            }
            case "Quintile": {
                return "Q";
            }
            case "Sesquiquintile": {
                return "bQ";
            }
            case "Sesquiquadrate": {
                return "\u221f";
            }
            // <b>Without these four the cells render blank</b>, not wrong - the same failure
            // the quincunx had above, where a missing case fell through to the empty string
            // and produced clickable links with no text in them.
            case "Septile": {
                return "S";
            }
            case "Novile": {
                return "N";
            }
            case "Decile": {
                return "D";
            }
            // <b>"bQ" is conventionally the biquintile mark and this codebase already spends
            // it on the sesquiquintile</b> (108 degrees, the tredecile). Rather than silently
            // reassign a symbol that has been on screen since 2026-08-23, the newcomer takes
            // "2Q" and the collision is written down. Which should own "bQ" is David to settle.
            case "Biquintile": {
                return "2Q";
            }
            case "Opposition": {
                return "\u260d";
            }
        }
        return "";
    }

    /**
     * Which part of the sidebar's HTML to build.
     *
     * <b>A parameter rather than three methods carved out of this one.</b> This builder is
     * decompiler output: 130 lines of interleaved StringBuilder calls whose loop counters
     * (n, n2, n3, n4) are shared across its three sections - n3 in particular is the base
     * placements' loop variable and is then reused as the transit-grid flag. Cutting it apart
     * would mean untangling that by hand for no gain; gating the blocks it already has is a
     * change a reader can check line by line against the original.
     */
    enum PlacementPart {
        /** Everything, one document - what the reading panel takes. */
        ALL,
        /** Chart header, aspect patterns, and the base chart's placements. */
        NATAL,
        /** Transit or Chart B placements, the sky ring, and synastry cross-contacts. */
        TRANSITS,
        /** The aspect grid and its legend. */
        GRID
    }

    private String generatePlanetPlacementsHtml() {
        return this.generatePlanetPlacementsHtml(PlacementPart.ALL);
    }

    /**
     * One figure in the pattern banner, every word of it a link to the figure's reading.
     *
     * <b>Only the bold name was a link.</b> The quality after it and the line of planets under
     * it were plain text, so a reader clicking "Mercury, Pluto, Uranus" under a grand trine got
     * nothing. David, 2026-09-14: "Grand trine (air) Mercury, Pluto, Uranus ... not letting me
     * select them". Swing's HTML cannot wrap a block in one anchor, so each run carries the same
     * href in its own colour. NavigationCheck Part Q clicks every character of this markup.
     */
    static String patternEntryHtml(com.zodiacomputing.ourania.astro.AspectPatterns.Pattern p) {
        String href = InterpretationPanel.patternHref(p.name, p.bodies);
        StringBuilder sb = new StringBuilder("<div style='margin-bottom:3px;'>");
        sb.append("<a href='").append(href)
            .append("' style='color:#FFD166; text-decoration:none;'><b>").append(p.name).append("</b>");
        String quality = p.modality != null
            && (p.name.equals("T-square") || p.name.equals("Grand cross"))
                ? p.modality
                : p.element != null
                    && (p.name.equals("Grand trine") || p.name.equals("Kite"))
                        ? p.element : null;
        if (quality != null) {
            sb.append(" <span style='color:#dddddd;'>(").append(quality).append(")</span>");
        }
        sb.append("</a>");
        sb.append("<div style='font-size:11px;'><a href='").append(href)
            .append("' style='color:#dddddd; text-decoration:none;'>")
            .append(String.join(", ", p.bodies));
        if (p.apex != null) {
            sb.append(" &nbsp;|&nbsp; apex ").append(p.apex);
        }
        sb.append("</a></div></div>");
        return sb.toString();
    }

    /** The width the aspect grid has to fit into: the drawer, less padding and scrollbar. */
    private static final int GRID_FIT_WIDTH = 276;

    /** Below this the glyphs stop being distinguishable, so the grid scrolls rather than lies. */
    private static final int GRID_MIN_CELL = 11;

    private String generatePlanetPlacementsHtml(PlacementPart part) {
        final boolean wantNatal = part == PlacementPart.ALL || part == PlacementPart.NATAL;
        final boolean wantTransits = part == PlacementPart.ALL || part == PlacementPart.TRANSITS;
        final boolean wantGrid = part == PlacementPart.ALL || part == PlacementPart.GRID;
        int n;
        int n2;
        String stringArray;
        int n3;
        StringBuilder stringBuilder = new StringBuilder("<html><body style='font-family:Arial; font-size:12px; color:white;'>");
        // <b>A loaded chart is somebody's, and says so.</b> The heading read "Natal Chart"
        // whichever profile was open, so with several charts saved the one fact a reader most
        // needs - which of them am I looking at - was the one the panel would not state. The
        // possessive is dropped for a composite, which belongs to two people rather than one,
        // and for hand-entered data, which genuinely has no name.
        String who = this.window == null ? "" : OuraniaWindow.possessive(this.window.chartName());
        String string;
        if (this.chartMode == ChartMode.SYNASTRY) {
            string = who.isEmpty() ? "Chart A (Inner)" : who + "Chart (Inner)";
        } else if (this.chartMode == ChartMode.COMPOSITE_MIDPOINT
                || this.chartMode == ChartMode.COMPOSITE_DAVISON) {
            string = "Composite Chart";
        } else if (!this.innerIsBirthChart) {
            // <b>No Chart A means no natal chart, and this is the last place that said
            // otherwise.</b> David: "if no persons data is selected to go into a or b then
            // neither would be selectable nor cast a chart - only thing that could is the sky
            // because it defaults to current astrological chart." The wheel already draws the
            // sky when Chart A is empty; calling that drawing somebody's natal chart is the
            // same mislabelling from the other end, and it is what a reader would believe.
            string = "The Sky Now";
        } else if (this.anchorSubject == this.subjectB) {
            // <b>And a promoted Chart B is not the reader's own chart either.</b> "who" is the
            // saved profile's name, which belongs to Chart A - printing it over Chart B's
            // placements is the same fault as calling the sky a natal chart, one seat along.
            string = "Chart B · Natal Chart";
        } else {
            string = who + "Natal Chart";
        }
        // Hoisted above the gate: the transit block below formats its own timestamps with
        // this same formatter, so leaving it inside the natal section made it invisible there.
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm z");
        if (wantNatal) {
        stringBuilder.append("<h2 style='color:#ffa500; margin-bottom: 2px;'>").append(string).append("</h2>");
        String string2 = this.natalRing.time != null ? this.natalRing.time.format(dateTimeFormatter) : "";
        String string3 = String.format("%.2f, %.2f", this.natalRing.latitude, this.natalRing.longitude);
        stringBuilder.append("<div style='color:#dddddd; font-size:11px; margin-bottom: 10px;'>").append(string2).append("<br>").append(string3).append("</div>");
        // <b>The one link that is always on screen.</b> Everything else in the index is
        // reached from a page you have to open first; this panel is up from the moment the
        // app starts, which makes it the only place a door actually works.
        stringBuilder.append("<div style='margin-bottom:10px;'><a href='index' "
            + "style='color:#7FB3FF; font-size:11px; text-decoration:none;'>"
            + "&#9776; Index &middot; browse bodies, signs, houses, aspects, dignities, "
            + "decans, Sabians, tarot and mansions</a></div>");

        // Aspect patterns, at the top of the aspect chart and without being asked for.
        //
        // These are closed circuits - several placements behaving as one unit - so they
        // outrank everything below them, and a chart that has one should say so before it
        // lists a single degree. Recomputed here because this method already runs once per
        // chart change; the wheel's highlight is refreshed from the same call.
        this.refreshPatterns();
        if (this.currentPatterns.isEmpty()) {
            // Said out loud rather than left blank. Patterns are held to physical bodies at a
            // tight orb, so about six charts in ten have none - and a section that simply
            // vanishes is indistinguishable from a section that is broken.
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px; "
                + "margin-bottom:10px;'>No aspect patterns in this chart - no T-square, "
                + "grand cross, grand trine, kite, yod or boomerang among the planets "
                + "within 5&deg;.</div>");
        } else {
            stringBuilder.append("<div style='border:1px solid #FFD166; padding:6px; "
                + "margin-bottom:10px;'>");
            stringBuilder.append("<h3 style='color:#FFD166; margin:0 0 4px 0;'>Aspect "
                + "Pattern").append(this.currentPatterns.size() > 1 ? "s" : "")
                .append(" &middot; lit on the wheel</h3>");
            for (com.zodiacomputing.ourania.astro.AspectPatterns.Pattern p
                    : this.currentPatterns) {
                stringBuilder.append(SkymapPanel.patternEntryHtml(p));
            }
            stringBuilder.append("<div style='color:#9AA5B1; font-size:10px;'>Click a figure "
                + "for the full reading.</div>");
            stringBuilder.append("</div>");
        }

        stringBuilder.append("<h3 style='color:#add8e6;'>Placements</h3>");
        for (n3 = 0; n3 < BODY_COUNT; ++n3) {
            if (!this.natalRing.valid[n3]) continue;
            stringBuilder.append(this.formatPlanetPlacement(n3, this.natalRing.lon[n3], this.natalRing.speed[n3], BASE_PREFIX));
        }
        }
        if (wantTransits && this.showTransitChart) {
            // The third copy of the same choice, and the one that called a progressed ring a
            // transit chart. ringWord is the statement of it; this only capitalises.
            String string4 = this.chartMode == ChartMode.SYNASTRY ? "Chart B (Outer)"
                : (this.showProgressed() ? "Progressed Chart" : "Transit Chart");
            stringBuilder.append("<br><h2 style='color:#ffa500; margin-bottom: 2px;'>").append(string4).append("</h2>");
            stringArray = this.outerCastLabel();
            if (stringArray.isEmpty() && this.outerRing.time != null) {
                stringArray = this.outerRing.time.format(dateTimeFormatter);
            }
            String stringArray2 = String.format("%.2f, %.2f", this.outerRing.latitude, this.outerRing.longitude);
            stringBuilder.append("<div style='color:#dddddd; font-size:11px; margin-bottom: 10px;'>").append(stringArray).append("<br>").append(stringArray2).append("</div>");
            stringBuilder.append("<h3 style='color:#ffa500;'>Placements</h3>");
            for (n2 = 0; n2 < BODY_COUNT; ++n2) {
                if (!this.outerRing.valid[n2]) continue;
                stringBuilder.append(this.formatPlanetPlacement(n2, this.outerRing.lon[n2], this.outerRing.speed[n2], "transit_"));
            }
        }
        if (wantTransits && this.showTriWheel) {
            stringBuilder.append("<br><h2 style='color:#a0d2ff; margin-bottom: 2px;'>Sky (Transiting)</h2>");
            String stringSkyTime = this.skyRing.time != null ? this.skyRing.time.format(dateTimeFormatter) : "";
            // Its own coordinates. This printed Chart B's, which is the same borrowing the
            // houses were doing until the sky got a row of its own - right whenever the two
            // happened to be the same city and quietly wrong otherwise.
            String stringSkyLoc = String.format("%.2f, %.2f", this.skyRing.latitude, this.skyRing.longitude);
            stringBuilder.append("<div style='color:#dddddd; font-size:11px; margin-bottom: 10px;'>").append(stringSkyTime).append("<br>").append(stringSkyLoc).append("</div>");
            stringBuilder.append("<h3 style='color:#a0d2ff;'>Placements</h3>");
            for (int k = 0; k < BODY_COUNT; ++k) {
                if (!this.skyRing.valid[k]) continue;
                stringBuilder.append(this.formatPlanetPlacement(k, this.skyRing.lon[k], this.skyRing.speed[k], "sky_"));
            }
        }
        // Cross-chart placement, above the grid because it outranks it: a body on the
        // other person's Ascendant is a larger fact than any single cell of the
        // matrix, and house overlays are invisible to that matrix altogether.
        if (wantTransits && this.chartMode == ChartMode.SYNASTRY) {
            stringBuilder.append(this.generateSynastryCrossHtml());
        }
        // A drawer that asked only for placements stops here; the grid part, or the whole
        // document, goes on to build the table.
        if (!wantGrid) {
            stringBuilder.append("</body></html>");
            return stringBuilder.toString();
        }
        if (part == PlacementPart.ALL) {
            stringBuilder.append("<br><hr style='border-color:#444;'><br>");
        }
        int n4 = n3 = this.showTransitChart && (this.aspectFilter.equals("Transit-Natal") || this.aspectFilter.equals("Both")) ? 1 : 0;
        if (n3 != 0) {
            if (this.chartMode == ChartMode.SYNASTRY) {
                stringBuilder.append("<h3 style='color:white; margin-bottom: 4px;'>Synastry Aspects Grid</h3>");
            } else {
                stringBuilder.append("<h3 style='color:white; margin-bottom: 4px;'>Transit to Natal Grid</h3>");
            }
        } else {
            stringBuilder.append("<h3 style='color:white; margin-bottom: 4px;'>")
                .append(this.innerIsBirthChart ? "Natal Aspects Grid" : "Sky Aspects Grid")
                .append("</h3>");
        }
        stringBuilder.append("<div style='font-size:10px; margin-bottom:10px;'>");
        // Quincunx belongs here: the grid has always emitted quincunx cells, so leaving it out
        // of the legend left a symbol on the table that nothing explained.
        // Driven from Aspects.Type rather than a hand-written list, so an aspect the engine
        // can emit cannot be missing from the legend. That is exactly how the quincunx came to
        // be drawn on the grid with nothing explaining it.
        for (Aspects.Type aspectType : Aspects.Type.values()) {
            String string5 = aspectType.label;
            stringBuilder.append("<span style='color:").append(this.getAspectColorHex(string5)).append("; font-size:14px;'>").append(this.getAspectSymbol(string5)).append("</span> <span style='color:#ccc;'>").append(string5).append("</span> &nbsp; ");
        }
        stringBuilder.append("</div>");
        // <b>The grid sizes itself to the points that are switched on.</b> It was a fixed
        // 22px cell at 14px type, which is a table as wide as the number of bodies enabled -
        // with the asteroids and angles on that is far wider than any drawer, and the columns
        // ran off the edge where they could not be read at all. Scaled to fit instead of
        // scrolled: a triangular grid is read by scanning across a row, and a table you have
        // to drag sideways to finish one row is worse than a small one you can take in whole.
        int gridCols = 0;
        for (n = 0; n < BODY_COUNT; ++n) {
            if (SkymapPanel.aspecting(n, this.natalRing.valid)) {
                gridCols++;
            }
        }
        // The drawer's content width, less its padding and the vertical scrollbar.
        int cell = gridCols > 0 ? (GRID_FIT_WIDTH / (gridCols + 1)) : 22;
        cell = Math.max(GRID_MIN_CELL, Math.min(22, cell));
        int glyph = Math.max(8, cell - 3);
        stringBuilder.append("<table border='1' cellspacing='0' cellpadding='0' style='border-collapse: collapse; border-color: #555; text-align:center;'>");
        stringBuilder.append("<tr><td style='width:").append(cell).append("px;'></td>");
        for (n = 0; n < BODY_COUNT; ++n) {
            if (!SkymapPanel.aspecting(n, this.natalRing.valid)) continue;
            stringBuilder.append("<td style='color:").append(this.bodyColorHex(n)).append("; font-size:").append(glyph).append("px; width:").append(cell).append("px;'>").append(BODY_GLYPHS[n]).append("</td>");
        }
        stringBuilder.append("</tr>");
        this.appendGridRows(stringBuilder, n3 != 0 ? WHEEL_OUTER : WHEEL_NATAL, cell, glyph);
        // <b>The sky ring had no rows here at all.</b> It is drawn, it is hovered, it has its
        // own field of chords - and the grid, which is where a reader goes to find an aspect
        // by name rather than by eye, stopped at the ring below it. So its lines could be seen
        // and never pointed at.
        // Gated on the same n3 the partner rows are, and on the ring being drawn - the two
        // gates the wheel's own sky chord loop uses. A grid that listed sky aspects while the
        // reader's filter had cross-chart lines switched off would be naming pairs the wheel
        // deliberately does not draw.
        if (n3 != 0 && this.triRingDrawn()) {
            stringBuilder.append("<tr><td colspan='").append(gridCols + 1)
                .append("' style='color:#8FD0FF; font-size:10px; text-align:left; padding:3px 0 1px 2px; background-color:#111;'>")
                .append("sky</td></tr>");
            this.appendGridRows(stringBuilder, WHEEL_SKY, cell, glyph);
        }
        stringBuilder.append("</table>");
        stringBuilder.append("</body></html>");
        return stringBuilder.toString();
    }

    /**
     * One wheel's worth of rows in the aspect grid.
     *
     * <b>Written once because there are three wheels now.</b> The rows used to be built inline
     * with the wheel chosen by a boolean in three separate expressions, which is the shape the
     * sky ring could not be added to without a fourth copy - and a fourth copy of a cell test
     * is a fourth chance for the grid to disagree with the wheel about what is in aspect.
     *
     * Columns are always the natal points: every aspect on this grid is something aspecting
     * the chart. The natal rows are the triangular half, since a natal pair appears once.
     */
    private void appendGridRows(StringBuilder out, int wheel, int cell, int glyph) {
        double[] lon = this.wheelLon(wheel);
        boolean[] valid = this.wheelValid(wheel);
        for (int n = 0; n < BODY_COUNT; ++n) {
            if (!SkymapPanel.aspecting(n, valid)) continue;
            out.append("<tr>");
            out.append("<td style='color:").append(this.bodyColorHex(n))
               .append("; font-size:").append(glyph).append("px; width:").append(cell)
               .append("px;'>").append(BODY_GLYPHS[n]).append("</td>");
            for (int i = 0; i < BODY_COUNT; ++i) {
                if (!SkymapPanel.aspecting(i, this.natalRing.valid)) continue;
                if ((wheel == WHEEL_NATAL && i >= n) || Bodies.isOppositePair(n, i)) {
                    out.append("<td style='background-color:#111;'></td>");
                    continue;
                }
                double sep = Math.abs(lon[n] - this.natalRing.lon[i]);
                if (sep > 180.0) {
                    sep = 360.0 - sep;
                }
                // Only the partner ring is a synastry pair. The sky ring is a moment and is
                // judged at natal orbs - the same call the wheel and the hit test make.
                String type = this.getAspectType(sep, n, i,
                    this.isSynastryPair(wheel == WHEEL_OUTER));
                if (type == null) {
                    out.append("<td style='background-color:#222;'></td>");
                    continue;
                }
                String row = wheel == WHEEL_NATAL ? BODY_NAMES[n]
                    : TRANSIT_PREFIX + BODY_NAMES[n].toLowerCase();
                // Through aspectHref, never formatted inline: the parser is the only other
                // place that knows this format and the two must not be able to drift.
                String href = SkymapPanel.aspectHref(row, BODY_NAMES[i], type, wheel);
                out.append("<td style='background-color:#222;'><a href='").append(href)
                   .append("' style='text-decoration:none;'>").append("<span style='color:")
                   .append(this.getAspectColorHex(type)).append("; font-size:").append(glyph)
                   .append("px;'>").append(this.getAspectSymbol(type))
                   .append("</span></a></td>");
            }
            out.append("</tr>");
        }
    }

    private String formatPlanetPlacement(int n, double d, double d2, String string) {
        int n2 = (int)(d / 30.0);
        int n3 = (int)(d % 30.0);
        int n4 = (int)((d - Math.floor(d)) * 60.0);
        int n5 = 1;
        for (int i = 1; i <= 12; ++i) {
            double d3;
            double d4;
            double d5 = this.activeCusps[i];
            double d6 = d4 = i == 12 ? this.activeCusps[1] : this.activeCusps[i + 1];
            if (d4 < d5) {
                d4 += 360.0;
            }
            if ((d3 = d) < d5 && d4 > 360.0) {
                d3 += 360.0;
            }
            if (!(d3 >= d5) || !(d3 < d4)) continue;
            n5 = i;
            break;
        }
        String string2 = SkymapPanel.showsDirection(n) ? (d2 < 0.0 ? " R" : " D") : "";
        String string3 = this.getElementColorHex(Zodiac.elementIndex(n2));
        // <b>Both decan rulers, each named, under the placement.</b> The app runs two schemes
        // at once and they disagree for 30 of the 36 decans, so an unlabelled "sub-ruler" here
        // would be worse than none: the reader cannot tell whether it governs the prose or the
        // tarot. Triplicity rules the decan prose; the Chaldean face rules the Golden Dawn
        // cards and the Sabian decan_ruler field. Zodiac's header carries the full note.
        String decanLine = SkymapPanel.decanRulers(d);
        return String.format("<div style='margin-bottom:4px;'><a href='%s%d' style='color:#cccccc; text-decoration:none; font-family:SansSerif; font-size:14px;'><span style='font-size:16px;'>%s</span> %d&deg; %02d'%s <span style='color:%s; font-size:16px;'>%s</span> House %s</a>%s</div>", string, n, BODY_GLYPHS[n], n3, n4, string2, string3, ZODIAC_SYMBOLS[n2], this.romanNumeral(n5), decanLine);
    }

    /**
     * The decan and its two rulers, as one small line for a placement row.
     *
     * Static and shared so the placements list and the hover card cannot drift apart - they
     * are the two surfaces a reader compares, and a decan named differently on each would be
     * read as a bug in the chart rather than a difference between two traditions.
     */
    static String decanRulers(double lon) {
        int decan = Zodiac.decan(lon);
        String triplicity = Zodiac.triplicityDecanRuler(lon);
        String face = Zodiac.chaldeanDecanRuler(lon);
        if ((triplicity == null || triplicity.isEmpty()) && (face == null || face.isEmpty())) {
            return "";
        }
        StringBuilder sb = new StringBuilder(
            "<div style='color:#9FB4C7; font-size:10px; margin-left:20px;'>Decan ");
        sb.append(decan).append(" &middot; ");
        if (triplicity != null && !triplicity.isEmpty()) {
            sb.append("triplicity <b>").append(triplicity).append("</b>");
        }
        if (face != null && !face.isEmpty()) {
            if (triplicity != null && !triplicity.isEmpty()) {
                sb.append(" &middot; ");
            }
            sb.append("Chaldean <b>").append(face).append("</b>");
        }
        return sb.append("</div>").toString();
    }

    private static boolean showsDirection(int n) {
        Bodies.Source source = Bodies.at((int)n).source;
        return source == Bodies.Source.EPHEMERIS || source == Bodies.Source.SOUTH_NODE;
    }

    private String romanNumeral(int n) {
        String[] stringArray = new String[]{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
        if (n >= 1 && n <= 12) {
            return stringArray[n];
        }
        return String.valueOf(n);
    }

    /**
     * The text-safe element palette, for surfaces outside this panel.
     *
     * <b>Package-private and static rather than a second copy.</b> Prose.decorate colours
     * every body and sign it links, and it must use the SAME lightened element colours the
     * wheel and the placements list use, or a reading would disagree with the chart beside
     * it about what colour Water is.
     */
    static String elementTextHex(int elementIndex) {
        if (elementIndex < 0 || elementIndex >= ELEMENT_TEXT_HEX.length) {
            return "#e4e5ea";
        }
        return ELEMENT_TEXT_HEX[elementIndex];
    }

    /** Element index for a registry body, or -1. Same source the wheel colours from. */
    static int elementOfBody(int bodyIndex) {
        if (bodyIndex < 0 || bodyIndex >= BODY_ELEMENTS.length) {
            return -1;
        }
        return BODY_ELEMENTS[bodyIndex];
    }

    private String getElementColorHex(int n) {
        if (n < 0 || n >= ELEMENT_TEXT_HEX.length) {
            return "#ffffff";
        }
        return ELEMENT_TEXT_HEX[n];
    }

    static {
        for (int i = 0; i < BODY_COUNT; ++i) {
            Bodies.Def def = Bodies.at(i);
            SkymapPanel.BODY_NAMES[i] = def.name;
            SkymapPanel.BODY_GLYPHS[i] = def.glyph;
            SkymapPanel.BODY_ELEMENTS[i] = def.element;
        }
        ZODIAC_SYMBOLS = new String[]{"\u2648\ufe0e", "\u2649\ufe0e", "\u264a\ufe0e", "\u264b\ufe0e", "\u264c\ufe0e", "\u264d\ufe0e", "\u264e\ufe0e", "\u264f\ufe0e", "\u2650\ufe0e", "\u2651\ufe0e", "\u2652\ufe0e", "\u2653\ufe0e"};
        FIRE = new Color(255, 69, 0);
        EARTH = new Color(50, 205, 50);
        AIR = new Color(255, 215, 0);
        WATER = new Color(30, 144, 255);
        ELEMENT_COLORS = new Color[]{FIRE, EARTH, AIR, WATER};
        ELEMENT_TEXT_HEX = SkymapPanel.buildElementTextHex();
        NATAL_GLYPH_FONT = new Font("SansSerif", 0, 28);
        TRANSIT_GLYPH_FONT = new Font("SansSerif", 0, 24);
        ANGLE_FONT = new Font("SansSerif", 1, 10);
        SIGN_NAMES = new String[]{"Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"};
    }

    private static enum ReadingTier {
        NONE,
        PARAGRAPH,
        REPORT,
        SYNTHESIZE,
        TIMELINE;

    }

    private class ChartPanel
    extends JPanel {
        private ChartPanel() {
        }

        /**
         * The hover card, asked for rather than pushed.
         *
         * <b>This is why hovering a body showed nothing.</b> mouseMoved used to call
         * setToolTipText with whatever hoverTextAt returned, and a comment here said that
         * passing null over empty space "unregisters the component, which is exactly the
         * behaviour wanted". It is not: ToolTipManager.unregisterComponent removes the
         * listeners it uses to track the pointer. The wheel is mostly empty space, so the
         * first move over blank chart unregistered the panel; moving on to a glyph set the
         * text again, but no mouseEntered can fire while the cursor is already inside the
         * component, so the manager never woke up and no card was ever shown.
         *
         * Overriding this instead keeps the panel registered for the life of the window and
         * lets the answer be per position. Returning null here suppresses the card without
         * touching registration, which is the behaviour that comment actually wanted.
         */
        @Override
        public String getToolTipText(MouseEvent event) {
            if (SkymapPanel.this.globeMode) {
                return SkymapPanel.this.hoverTextAt(event.getX(), event.getY());
            }
            java.awt.Point at = SkymapPanel.this.toWheel(event.getX(), event.getY());
            return SkymapPanel.this.hoverTextAt(at.x, at.y);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Object object;
            int n;
            int n2;
            int n3;
            double d;
            int n4;
            int n5;
            int n6;
            int n7;
            double d2;
            double d3;
            int n8;
            int n9;
            super.paintComponent(graphics);
            // <b>Jet black, deliberately flat.</b> A radial gradient was tried here on
            // 2026-08-31 to suggest depth and removed the same day: against the muted
            // element palette the lifted centre greyed the ground and the copper and sage
            // glyphs lost contrast. Pure black is the strongest backing those colours have.
            // <b>The wheel's ground, and the last surface that was not on the theme.</b>
            // Black since the app existed - which left the chart reading as a hole cut in the
            // window once everything around it moved to a navy ground. Overridable in
            // Settings; black is still what it falls back to, so a reader who never opens
            // Settings sees the chart they had yesterday.
            graphics.setColor(ChartPalette.colorOr(ChartPalette.backgroundHex(null),
                Color.BLACK));
            graphics.fillRect(0, 0, this.getWidth(), this.getHeight());
            if (SkymapPanel.this.sw == null || SkymapPanel.this.natalRing.sd == null) {
                return;
            }
            Graphics2D graphics2D = (Graphics2D)graphics;
            // <b>The other view of the same chart.</b> Everything below this line draws the
            // flat wheel; the globe reads the same arrays and paints them as shells. One
            // branch, at the top, because the two share no drawing at all - the alternative
            // is a flag threaded through five thousand lines of painter.
            if (SkymapPanel.this.globeMode) {
                GlobeRenderer.paint(graphics2D, SkymapPanel.this.globe,
                    this.getWidth(), this.getHeight(), SkymapPanel.this,
                    SkymapPanel.this.globeDragging);
                SkymapPanel.paintZodiacTag(graphics2D, this.getWidth(), this.getHeight());
                SkymapPanel.this.paintScrubTag(graphics2D, this.getWidth());
                return;
            }
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int n10 = this.getWidth();
            int n11 = this.getHeight();
            // The zoom and pan, for the screen only - see viewTransform. Everything drawn from
            // here to the hover swell is in the wheel's own coordinates.
            final java.awt.geom.AffineTransform screenTx = graphics2D.getTransform();
            final boolean onScreen = this.getClientProperty(ChartExporter.EXPORTING) == null;
            if (onScreen) {
                // Re-clamped at the size being painted: a window made smaller while zoomed would
                // otherwise keep a pan that now runs past the wheel's edge.
                SkymapPanel.this.clampView();
                graphics2D.transform(SkymapPanel.this.viewTransform(n10, n11));
            }
            // The same geometry both hit tests and the click dispatcher use. This painter is
            // the site that diverged: it drew bodies at RING_SIGN_INNER while bodyAt tested
            // bodyBaseRadius, so with the reader's placement anywhere but the default the
            // glyphs and the clicks were tens of pixels apart.
            Geometry g = SkymapPanel.this.geometry(n10, n11);
            if (g == null) {
                return;
            }
            int n12 = g.cx;
            int n13 = g.cy;
            int[] rings = g.rings;
            int n14 = n9 = rings[RING_OUTER];
            int nTriOuter = rings[RING_TRI];
            int n15 = rings[RING_TRANSIT];
            int n16 = rings[RING_DECAN_OUTER];
            int n17 = rings[RING_SIGN_OUTER];
            int n18 = rings[RING_SIGN_INNER];
            int nBodyTop = rings[RING_BODY_TOP];
            int nTermInner = rings[RING_TERM_INNER];
            int nDegreeInner = rings[RING_DEGREE_INNER];
            // The rim scale's outer edge: the rim itself with the mansions folded, and the
            // underside of the mansion band once they open.
            int nMansionInner = rings[RING_MANSION_INNER];
            // <b>The disc, filled separately from the page.</b> One colour used to do both -
            // "Wheel" repainted the whole panel - so the chart could never sit ON anything.
            // Filled before any ring is drawn, so every stroke below lands on top of it.
            Color disc = ChartPalette.colorOr(ChartPalette.wheelHex(null), null);
            if (disc != null) {
                graphics2D.setColor(disc);
                graphics2D.fillOval(n12 - n14, n13 - n14, n14 * 2, n14 * 2);
            }
            double[] dArray = SkymapPanel.this.activeCusps;
            double d4 = SkymapPanel.this.getPinLongitude();
            graphics2D.setColor(SkymapPanel.inkColor());
            graphics2D.setStroke(new BasicStroke(1.0f));
            graphics2D.drawLine(n12 - 10, n13, n12 + 10, n13);
            graphics2D.drawLine(n12, n13 - 10, n12, n13 + 10);
            graphics2D.setColor(SkymapPanel.inkColor());
            graphics2D.setStroke(new BasicStroke(2.0f));
            if (SkymapPanel.this.layerShown(Layer.SIGNS)) {
                graphics2D.drawOval(n12 - n18, n13 - n18, n18 * 2, n18 * 2);
                graphics2D.drawOval(n12 - n17, n13 - n17, n17 * 2, n17 * 2);
            }
            if (SkymapPanel.this.layerShown(Layer.DECANS)) {
                graphics2D.drawOval(n12 - n16, n13 - n16, n16 * 2, n16 * 2);
            }
            graphics2D.drawOval(n12 - nTermInner, n13 - nTermInner,
                nTermInner * 2, nTermInner * 2);
            graphics2D.drawOval(n12 - nDegreeInner, n13 - nDegreeInner,
                nDegreeInner * 2, nDegreeInner * 2);
            if (SkymapPanel.this.outerRingDrawn()) {
                // Both boundaries of the partner band. Since the reorder it hangs below the
                // zodiac rather than outside it, so its floor is a line of its own - without
                // it the band bleeds into the natal wheel and stops reading as a field.
                graphics2D.setColor(new Color(80, 40, 80));
                graphics2D.drawOval(n12 - n15, n13 - n15, n15 * 2, n15 * 2);
                graphics2D.drawOval(n12 - nBodyTop, n13 - nBodyTop,
                    nBodyTop * 2, nBodyTop * 2);
            }
            // Tri-wheel: an additional ring outside the synastry ring.
            if (SkymapPanel.this.triRingDrawn()) {
                graphics2D.setColor(new Color(30, 60, 80));
                graphics2D.drawOval(n12 - nTriOuter, n13 - nTriOuter, nTriOuter * 2, nTriOuter * 2);
            }
            graphics2D.setFont(new Font("SansSerif", 0, 12));
            for (n8 = 0; SkymapPanel.this.layerShown(Layer.DECANS) && n8 < 36; ++n8) {
                d3 = (double)n8 * 10.0;
                d2 = Math.toRadians(180.0 + d4 - d3);
                n7 = n12 + (int)((double)n17 * Math.cos(d2));
                n6 = n13 + (int)((double)n17 * Math.sin(d2));
                n5 = n12 + (int)((double)n16 * Math.cos(d2));
                n4 = n13 + (int)((double)n16 * Math.sin(d2));
                graphics2D.setColor(Color.LIGHT_GRAY);
                graphics2D.setStroke(new BasicStroke(1.0f));
                graphics2D.drawLine(n7, n6, n5, n4);
                d = Math.toRadians(180.0 + d4 - (d3 + 5.0));
                n3 = n12 + (int)((double)(n16 - 10) * Math.cos(d));
                n2 = n13 + (int)((double)(n16 - 10) * Math.sin(d));
                // <b>One band, two traditions, and the reader picks which one it draws.</b>
                // Visual only: the prose and the tarot keep their own schemes whatever this
                // says, because they are bound to datasets that cannot be remapped. See
                // Settings.decanRing for why a global decan-system toggle is not on offer.
                int n19 = n8 / 3;
                if (Settings.DECAN_RING_CHALDEAN.equals(Settings.decanRing())) {
                    String faceRuler = Zodiac.chaldeanDecanRuler(Zodiac.SIGNS[n19], n8 % 3 + 1);
                    int fi = Bodies.indexOfName(faceRuler);
                    if (fi >= 0 && fi < BODY_GLYPHS.length) {
                        graphics2D.setColor(SkymapPanel.this.bodyColor(fi));
                        graphics2D.drawString(BODY_GLYPHS[fi], n3 - 4, n2 + 4);
                        continue;
                    }
                    // An unresolvable ruler falls through to the sign glyph rather than
                    // leaving a gap in the band, which would read as a rendering fault.
                }
                int n20 = Zodiac.triplicityDecanSignIndex(n19, n8 % 3 + 1);
                graphics2D.setColor(SkymapPanel.this.getElementColor(Zodiac.elementIndex(n20)));
                graphics2D.drawString(ZODIAC_SYMBOLS[n20], n3 - 4, n2 + 4);
            }
            // <b>The Egyptian bounds, one glyph per segment.</b> Dignity has scored bound
            // placements since it was written and nothing drew them, so a reader comparing
            // this wheel against another program found the terms simply missing. Sixty
            // segments, five to a sign, each ruled by one of the five non-luminary planets -
            // read from Dignity rather than from a second copy of the table.
            if (SkymapPanel.this.layerShown(Layer.BOUNDS)) {
                SkymapPanel.this.drawBoundRing(graphics2D, n12, n13, n18, nTermInner, d4);
            }

            // <b>The second degree scale, sitting directly above the wheels.</b> The outer
            // ticks are at the rim beside the lunar mansions, too far from any glyph to read
            // a body against; this one is where a body's leader line lands, so a reader can
            // follow a glyph out to the degree it actually occupies.
            if (SkymapPanel.this.layerShown(Layer.DEGREES)) {
                SkymapPanel.this.drawInnerDegreeRing(graphics2D, n12, n13, nTermInner,
                    nDegreeInner, d4);
            }

            // The 28 lunar mansions, in a band of their own at the rim.
            //
            // <b>It used to share the rim with the degree ticks and cover them.</b> The band is
            // now carved out of the rim by the chain - RING_MANSION_INNER - and the ticks below
            // read from the same number, so the two cannot overlap. Carving it here rather than
            // inside the zodiac is what keeps the bodies where they are: every radius from
            // RING_DECAN_OUTER inward is untouched.
            if (SkymapPanel.this.layerShown(Layer.MANSIONS)) {
                SkymapPanel.this.drawMansionRing(graphics2D, n12, n13, n14, nMansionInner, d4);
            }

            graphics2D.setFont(new Font("SansSerif", 0, 22));
            for (n8 = 0; SkymapPanel.this.layerShown(Layer.SIGNS) && n8 < 12; ++n8) {
                d3 = (double)n8 * 30.0;
                d2 = Math.toRadians(180.0 + d4 - d3);
                n7 = n12 + (int)((double)n18 * Math.cos(d2));
                n6 = n13 + (int)((double)n18 * Math.sin(d2));
                n5 = n12 + (int)((double)n17 * Math.cos(d2));
                n4 = n13 + (int)((double)n17 * Math.sin(d2));
                graphics2D.setColor(SkymapPanel.inkColor());
                graphics2D.setStroke(new BasicStroke(2.0f));
                graphics2D.drawLine(n7, n6, n5, n4);
                d = Math.toRadians(180.0 + d4 - (d3 + 15.0));
                n3 = n12 + (int)((double)(n17 - 18) * Math.cos(d));
                n2 = n13 + (int)((double)(n17 - 18) * Math.sin(d));
                graphics2D.setColor(SkymapPanel.this.getElementColor(Zodiac.elementIndex(n8)));
                graphics2D.drawString(ZODIAC_SYMBOLS[n8], n3 - 9, n2 + 6);
            }
            graphics2D.setColor(new Color(150, 150, 150));
            for (n8 = 0; SkymapPanel.this.layerShown(Layer.DEGREES) && n8 < 360; ++n8) {
                d3 = Math.toRadians(180.0 + d4 - (double)n8);
                // Hanging from the mansion band's underside rather than from the rim, so the
                // two scales and the mansions are three separate bands the reader can tell
                // apart. With the mansions folded this is the rim, exactly as before.
                int n21 = nMansionInner;
                n = n8 % 10 == 0 ? n21 - 6 : (n8 % 5 == 0 ? n21 - 4 : n21 - 2);
                n7 = n12 + (int)((double)n * Math.cos(d3));
                n6 = n13 + (int)((double)n * Math.sin(d3));
                n5 = n12 + (int)((double)n21 * Math.cos(d3));
                n4 = n13 + (int)((double)n21 * Math.sin(d3));
                graphics2D.setStroke(new BasicStroke(n8 % 10 == 0 ? 1.5f : 0.5f));
                graphics2D.drawLine(n7, n6, n5, n4);
            }
            for (n8 = 1; SkymapPanel.this.layerShown(Layer.HOUSES) && n8 <= 12; ++n8) {
                d3 = dArray[n8];
                double d5 = Math.toRadians(180.0 + d4 - d3);
                n7 = n12;
                n6 = n13;
                n5 = n12 + (int)((double)n18 * Math.cos(d5));
                n4 = n13 + (int)((double)n18 * Math.sin(d5));
                if (n8 == 1 || n8 == 4 || n8 == 7 || n8 == 10) {
                    graphics2D.setColor(SkymapPanel.inkColor());
                    graphics2D.setStroke(new BasicStroke(3.0f));
                } else {
                    graphics2D.setColor(Color.DARK_GRAY);
                    graphics2D.setStroke(new BasicStroke(2.0f));
                }
                graphics2D.drawLine(n7, n6, n5, n4);
                double d6 = d = n8 == 12 ? dArray[1] : dArray[n8 + 1];
                if (d < d3) {
                    d += 360.0;
                }
                double d7 = d3 + (d - d3) / 2.0;
                double d8 = Math.toRadians(180.0 + d4 - d7);
                // <b>Just inside the aspect field's rim, not under the sign ring.</b> The number
                // sat 15 px inside the sign ring's inner edge, which was open space when it was
                // placed; the bounds ring and the degree scale have since grown into exactly that
                // band, and an 11 px digit among the bound glyphs and the 0/10/20 labels read as
                // no house numbers at all (reported 2026-09-14). The rim of the aspect disc is
                // the one ring on the wheel nothing else is drawn on.
                int houseR = g.natalFloor - 16;
                int n22 = n12 + (int)((double)houseR * Math.cos(d8));
                int n23 = n13 + (int)((double)houseR * Math.sin(d8));
                graphics2D.setColor(SkymapPanel.this.getElementColor(Zodiac.elementIndex(n8 - 1)));
                graphics2D.setFont(Theme.font("Arial", 1, 15));
                java.awt.FontMetrics houseFm = graphics2D.getFontMetrics();
                String houseText = String.valueOf(n8);
                graphics2D.drawString(houseText, n22 - houseFm.stringWidth(houseText) / 2,
                    n23 + houseFm.getAscent() / 2 - 1);
            }
            if (SkymapPanel.this.showTransitChart && "Both".equals(SkymapPanel.this.houseAlignment)) {
                Stroke stroke = graphics2D.getStroke();
                graphics2D.setStroke(new BasicStroke(2.0f, 0, 0, 10.0f, new float[]{4.0f, 4.0f}, 0.0f));
                for (int i = 1; i <= 12; ++i) {
                    // Across the partner band, which is where those cusps belong - not from
                    // the decan ring, which the reorder moved to the other side of the wheel.
                    double d9 = Math.toRadians(180.0 + d4 - SkymapPanel.this.outerRing.cusps[i]);
                    n = n12 + (int)((double)nBodyTop * Math.cos(d9));
                    n7 = n13 + (int)((double)nBodyTop * Math.sin(d9));
                    n6 = n12 + (int)((double)n15 * Math.cos(d9));
                    n5 = n13 + (int)((double)n15 * Math.sin(d9));
                    n4 = i == 1 || i == 4 || i == 7 || i == 10 ? 1 : 0;
                    graphics2D.setColor(n4 != 0 ? new Color(210, 150, 230) : new Color(120, 80, 140));
                    graphics2D.drawLine(n, n7, n6, n5);
                }
                graphics2D.setStroke(stroke);
            }
            // Body radii come from the shared geometry, not from n18. n18 is RING_SIGN_INNER
            // and this painter used to draw bodies there whatever placement the reader chose,
            // while bodyAt tested bodyBaseRadius - 325 against 201 in the centre at 820px, so
            // nothing on the wheel could be clicked. n18 keeps RING_SIGN_INNER for the sign
            // circle it strokes and the spokes it ends, because those are the sign ring.
            int[] nArray = g.natalRadii();
            int[] nArray2 = g.transitRadii();
            int[] nArrayC = g.triRadii();
            // The three aspect fields. Lines are drawn on these, not between the glyphs -
            // see Geometry.aspectDisc for why, and note that the hit test reads the same
            // three numbers, because a line you can see has to be a line you can hover.
            int discNatal = g.aspectDisc(0);
            int discOuter = g.aspectDisc(1);
            int discSky = g.aspectDisc(2);
            // <b>One circle, where the lines stop and the bodies start.</b> Three nested
            // fields of chords with nothing around them read as weather in the middle of the
            // wheel; a single boundary at the natal band's floor turns them into a thing with
            // an edge. Nothing has to be clipped to it: every chord has both ends on a disc
            // inside this radius, so the widest line the wheel can draw is a diameter of the
            // outermost field and still falls short of the circle around it.
            //
            // Alpha 190 rather than the 120 it was first drawn at. A circle sampled across ten
            // spokes of the rendered wheel registered on six of them at 120, where every other
            // ring line registered on ten - present in the file and absent to a reader, which
            // is the one outcome a boundary cannot have. Still below the zodiac's full-weight
            // strokes, because it marks an edge rather than another ring.
            Color edge = SkymapPanel.inkColor();
            graphics2D.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 190));
            graphics2D.setStroke(new BasicStroke(1.5f));
            graphics2D.drawOval(n12 - g.natalFloor, n13 - g.natalFloor,
                g.natalFloor * 2, g.natalFloor * 2);
            int n24 = n18 - 60;
            graphics2D.setStroke(new BasicStroke(0.5f));
            // <b>The layer folds the lines; the filter chooses which families.</b> Two
            // different questions, and a reader who folded the aspects away expects all of
            // them gone whatever the filter says.
            boolean aspectLayer = SkymapPanel.this.layerShown(Layer.ASPECTS);
            boolean bl = aspectLayer && SkymapPanel.this.drawsNatalAspects();
            int n25 = n = aspectLayer && SkymapPanel.this.drawsCrossAspects() ? 1 : 0;
            if (bl) {
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.aspecting(n7, SkymapPanel.this.natalRing.valid)) continue;
                    for (n6 = n7 + 1; n6 < BODY_COUNT; ++n6) {
                        if (!SkymapPanel.aspecting(n6, SkymapPanel.this.natalRing.valid) || Bodies.isOppositePair(n7, n6)) continue;
                        this.drawAspectLine(graphics2D, SkymapPanel.this.natalRing.lon[n7], SkymapPanel.this.natalRing.lon[n6], d4, n12, n13, discNatal, discNatal, WHEEL_NATAL, n7, n6);
                    }
                }
            }
            if (n != 0) {
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.aspecting(n7, SkymapPanel.this.outerRing.valid)) continue;
                    for (n6 = 0; n6 < BODY_COUNT; ++n6) {
                        if (!SkymapPanel.aspecting(n6, SkymapPanel.this.natalRing.valid)) continue;
                        this.drawAspectLine(graphics2D, SkymapPanel.this.outerRing.lon[n7], SkymapPanel.this.natalRing.lon[n6], d4, n12, n13, discOuter, discOuter, WHEEL_OUTER, n7, n6);
                    }
                }
            }
            // <b>The sky ring had no aspect lines at all.</b> It could be drawn, hovered and
            // read, and the one thing the wheel is for - showing what is in aspect to what -
            // stopped at the ring below it. With a field of its own there is now somewhere to
            // put them where they do not cross the other two.
            if (n != 0 && SkymapPanel.this.triRingDrawn()) {
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.aspecting(n7, SkymapPanel.this.skyRing.valid)) continue;
                    for (n6 = 0; n6 < BODY_COUNT; ++n6) {
                        if (!SkymapPanel.aspecting(n6, SkymapPanel.this.natalRing.valid)) continue;
                        this.drawAspectLine(graphics2D, SkymapPanel.this.skyRing.lon[n7],
                            SkymapPanel.this.natalRing.lon[n6], d4, n12, n13, discSky, discSky,
                            WHEEL_SKY, n7, n6);
                    }
                }
            }
            // Draw the hovered line once more, last, so it sits on top of the weave. Emphasis
            // alone is not enough: with 28 points switched on, a highlighted line drawn in
            // registry order disappears under every pair that comes after it.
            // The lit pattern goes down before the hovered line, for the same reason the
            // hovered line goes down after the weave: emphasis alone loses to draw order once
            // there are 28 points on the wheel. A figure is several lines, so all of its
            // member pairs are redrawn here - drawAspectLine draws nothing for a pair that
            // holds no aspect, so the loop can be over members rather than over the figure's
            // own legs, which the Pattern does not record.
            java.util.List<int[]> litFigures = new java.util.ArrayList<>();
            for (int[] members : SkymapPanel.this.autoPatterns) {
                litFigures.add(members);
            }
            if (SkymapPanel.this.highlightPattern.length > 0) {
                litFigures.add(SkymapPanel.this.highlightPattern);
            }
            for (int[] lit : litFigures) {
                for (int li = 0; li < lit.length; li++) {
                    for (int lj = li + 1; lj < lit.length; lj++) {
                        int pa = lit[li];
                        int pb = lit[lj];
                        if (!SkymapPanel.aspecting(pa, SkymapPanel.this.natalRing.valid)
                            || !SkymapPanel.aspecting(pb, SkymapPanel.this.natalRing.valid)) {
                            continue;
                        }
                        this.drawAspectLine(graphics2D, SkymapPanel.this.natalRing.lon[pa],
                            SkymapPanel.this.natalRing.lon[pb], d4, n12, n13, discNatal, discNatal,
                            WHEEL_NATAL, pa, pb);
                    }
                }
            }

            int hlA = SkymapPanel.this.highlightA;
            int hlB = SkymapPanel.this.highlightB;
            int hlWheel = SkymapPanel.this.highlightWheel;
            if (hlA >= 0 && hlB >= 0) {
                // <b>Off the ring it belongs to.</b> This redraw picked the outer wheel for
                // every cross-chart line, so a hovered sky aspect was drawn again on the
                // partner ring - a bright chord in the wrong field, next to the faint one it
                // was meant to be.
                int hlDisc = hlWheel == WHEEL_SKY ? discSky
                    : (hlWheel == WHEEL_OUTER ? discOuter : discNatal);
                if (SkymapPanel.aspecting(hlA, SkymapPanel.this.wheelValid(hlWheel))
                    && SkymapPanel.aspecting(hlB, SkymapPanel.this.natalRing.valid)) {
                    this.drawAspectLine(graphics2D,
                        SkymapPanel.this.wheelLon(hlWheel)[hlA], SkymapPanel.this.natalRing.lon[hlB],
                        d4, n12, n13, hlDisc, hlDisc, hlWheel, hlA, hlB);
                }
            }
            // The transform the bodies are drawn in, so a hovered glyph's swell is undone
            // before anything else is painted - every loop below resets to it.
            final java.awt.geom.AffineTransform bodyTx = graphics2D.getTransform();
            for (n7 = 0; SkymapPanel.this.layerShown(Layer.NATAL) && n7 < BODY_COUNT; ++n7) {
                if (!SkymapPanel.this.natalRing.valid[n7]) continue;
                double d10 = Math.toRadians(180.0 + d4 - SkymapPanel.this.natalRing.lon[n7]);
                n4 = n12 + (int)((double)nArray[n7] * Math.cos(d10));
                int n26 = n13 + (int)((double)nArray[n7] * Math.sin(d10));
                SkymapPanel.bulge(graphics2D, bodyTx, n4, n26, SkymapPanel.this.hoverBody == n7);
                if (SkymapPanel.this.onHighlightedLine(n7, WHEEL_NATAL)) {
                    SkymapPanel.drawHighlightHalo(graphics2D, n4, n26,
                        Bodies.at(n7).isAngle() ? 13 : SkymapPanel.natalSize(n7).radius);
                }
                if (Bodies.at(n7).isAngle()) {
                    graphics2D.setFont(ANGLE_FONT);
                    // The angle marker follows the template: gold is invisible on a white
                    // ground and is the only warm thing in a cool palette.
                    Color angle = ChartPalette.colorOr(ChartPalette.angleHex("#D4AF37"),
                        new Color(212, 175, 55));
                    this.drawBodyMarker(graphics2D, n4, n26, 13, angle,
                        Settings.natalMarker());
                    // The glyph reads against its own bead rather than against a fixed dark
                    // brown, which disappears on a pale marker.
                    graphics2D.setColor(SkymapPanel.readableOn(angle));
                    object = Bodies.at((int)n7).glyph;
                    graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n26 + 4);
                    continue;
                }
                // A body recedes with its lines unless it IS the focus or aspects it. Without
                // this the web dims and the glyphs stay bright, which reads as the lines being
                // broken rather than as one body being singled out.
                java.awt.Composite priorComposite = graphics2D.getComposite();
                if (!SkymapPanel.this.noFocus()) {
                    graphics2D.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER,
                        (float) SkymapPanel.this.glyphWeight(n7, false)));
                }
                GlyphSize glyphSize = SkymapPanel.natalSize(n7);
                graphics2D.setFont(glyphSize.font);
                this.drawBodyMarker(graphics2D, n4, n26, glyphSize.radius,
                    new Color(192, 192, 192), Settings.natalMarker());
                if (n7 == MOON && SkymapPanel.this.natalRing.valid[SUN]) {
                    double d11 = (SkymapPanel.this.natalRing.lon[MOON] - SkymapPanel.this.natalRing.lon[SUN]) % 360.0;
                    if (d11 < 0.0) {
                        d11 += 360.0;
                    }
                    SkymapPanel.this.drawMoonPhase(graphics2D, n4, n26, Math.round((float)glyphSize.radius * 0.47f), d11 / 360.0);
                } else {
                    graphics2D.setColor(SkymapPanel.this.bodyColor(n7));
                    object = SkymapPanel.glyphFor(n7, glyphSize.font);
                    SkymapPanel.drawBodyLabel(graphics2D, (String)object, n4, n26, glyphSize.baseline);
                }
                graphics2D.setTransform(bodyTx);
                // The leader from the glyph to the degree it actually occupies. Bodies are
                // spread outward when they crowd, so without this a reader cannot tell which
                // degree a glyph belongs to. Colour and visibility are both settings; the
                // alpha stays here because a solid leader would compete with the aspect lines.
                graphics2D.setComposite(priorComposite);
                if (Settings.showDegreeLines()) {
                    // <b>To the far scale, at the reader's asking.</b> These ran to the inner
                    // degree scale on the argument that a leader crossing the decans and the
                    // bounds is a line the reader has to trace rather than read. David,
                    // 2026-09-16: "can we extend the line all the way to the second sabian ring
                    // degree" - so they now reach the rim scale, which is the outer of the two.
                    // The inner scale is still drawn and still readable; what changed is where
                    // the line ends, and that is his call to make while looking at it.
                    //
                    // <b>The hovered body's leader is drawn to be seen.</b> At alpha 60 every
                    // leader is a hint and none of them is an answer; the one the cursor is on
                    // is the reader asking which degree this glyph occupies, so it is drawn
                    // solid and the rest stay a background.
                    boolean lit = SkymapPanel.this.focusBody == n7
                        && !SkymapPanel.this.focusTransit;
                    Color leader = ChartPalette.colorOr(ChartPalette.leaderHex(null),
                        Color.WHITE);
                    Stroke priorLeader = graphics2D.getStroke();
                    graphics2D.setColor(new Color(leader.getRed(), leader.getGreen(),
                        leader.getBlue(), lit ? 235 : 60));
                    graphics2D.setStroke(new BasicStroke(lit ? 1.8f : 1.0f));
                    graphics2D.drawLine(n4, n26,
                        n12 + (int)((double)nMansionInner * Math.cos(d10)),
                        n13 + (int)((double)nMansionInner * Math.sin(d10)));
                    graphics2D.setStroke(priorLeader);
                }
            }
            graphics2D.setTransform(bodyTx);
            if (SkymapPanel.this.outerRingDrawn()) {
                Composite outerWas = graphics2D.getComposite();
                float outerA = SkymapPanel.ringAlpha(SkymapPanel.this.outerOpenFraction());
                if (outerA < 0.999f) {
                    graphics2D.setComposite(
                        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, outerA));
                }
                // The same question the angle cards ask, asked once for the whole ring: in a
                // synastry this wheel is a second person, anywhere else it is a moment.
                final AngleRole outerRole = SkymapPanel.this.angleRoleFor(false, true);
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.this.outerRing.valid[n7]) continue;
                    double d12 = Math.toRadians(180.0 + d4 - SkymapPanel.this.outerRing.lon[n7]);
                    n4 = n12 + (int)((double)nArray2[n7] * Math.cos(d12));
                    int n27 = n13 + (int)((double)nArray2[n7] * Math.sin(d12));
                    SkymapPanel.bulge(graphics2D, bodyTx, n4, n27,
                        SkymapPanel.this.hoverBody == (n7 | TRANSIT_BIT));
                    if (SkymapPanel.this.onHighlightedLine(n7, WHEEL_OUTER)) {
                        SkymapPanel.drawHighlightHalo(graphics2D, n4, n27,
                            Bodies.at(n7).isAngle() ? 13
                                : SkymapPanel.transitSize(n7).radius);
                    }
                    if (Bodies.at(n7).isAngle()) {
                        graphics2D.setFont(ANGLE_FONT);
                        this.drawBodyMarker(graphics2D, n4, n27, 13,
                            SkymapPanel.ringBead(outerRole),
                            SkymapPanel.outerRingMarker(SkymapPanel.this.chartMode));
                        graphics2D.setColor(SkymapPanel.this.ringAngleInk(outerRole));
                        object = Bodies.at((int)n7).glyph;
                        graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n27 + 4);
                        continue;
                    }
                    GlyphSize glyphSize2 = SkymapPanel.transitSize(n7);
                    graphics2D.setFont(glyphSize2.font);
                    object = SkymapPanel.this.ringInk(n7, outerRole);
                    this.drawBodyMarker(graphics2D, n4, n27, glyphSize2.radius,
                        SkymapPanel.ringBead(outerRole),
                        SkymapPanel.outerRingMarker(SkymapPanel.this.chartMode));
                    if (n7 == MOON && SkymapPanel.this.outerRing.valid[SUN]) {
                        double d13 = (SkymapPanel.this.outerRing.lon[MOON] - SkymapPanel.this.outerRing.lon[SUN]) % 360.0;
                        if (d13 < 0.0) {
                            d13 += 360.0;
                        }
                        SkymapPanel.this.drawMoonPhase(graphics2D, n4, n27, Math.round((float)glyphSize2.radius * 0.44f), d13 / 360.0);
                        continue;
                    }
                    graphics2D.setColor((Color)object);
                    String string = SkymapPanel.glyphFor(n7, glyphSize2.font);
                    SkymapPanel.drawBodyLabel(graphics2D, string, n4, n27, glyphSize2.baseline);
                }
                graphics2D.setComposite(outerWas);
            }
            // Tri-wheel: sky positions in the outermost ring. Blue-tinted, and by default a
            // different shape from the synastry ring inside it - see Settings.MARKER_SHAPES
            // for why the tint alone was not enough.
            graphics2D.setTransform(bodyTx);
            if (SkymapPanel.this.triRingDrawn()) {
                Composite triWas = graphics2D.getComposite();
                float triA = SkymapPanel.ringAlpha(SkymapPanel.this.triOpenFraction());
                if (triA < 0.999f) {
                    graphics2D.setComposite(
                        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, triA));
                }
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.this.skyRing.valid[n7]) continue;
                    double d14 = Math.toRadians(180.0 + d4 - SkymapPanel.this.skyRing.lon[n7]);
                    n4 = n12 + (int)((double)nArrayC[n7] * Math.cos(d14));
                    int n28 = n13 + (int)((double)nArrayC[n7] * Math.sin(d14));
                    // The sky ring never had this at all: its chords could light and the two
                    // glyphs at their ends stayed dark, so a reader could see a sky aspect
                    // and still have to work out which points it joined.
                    if (SkymapPanel.this.onHighlightedLine(n7, WHEEL_SKY)) {
                        SkymapPanel.drawHighlightHalo(graphics2D, n4, n28,
                            Bodies.at(n7).isAngle() ? 13
                                : SkymapPanel.transitSize(n7).radius);
                    }
                    if (Bodies.at(n7).isAngle()) {
                        graphics2D.setFont(ANGLE_FONT);
                        this.drawBodyMarker(graphics2D, n4, n28, 13,
                            SkymapPanel.ringBead(AngleRole.SKY), Settings.transitMarker());
                        graphics2D.setColor(SkymapPanel.this.ringAngleInk(AngleRole.SKY));
                        object = Bodies.at((int)n7).glyph;
                        graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n28 + 4);
                        continue;
                    }
                    GlyphSize glyphSize3 = SkymapPanel.transitSize(n7);
                    graphics2D.setFont(glyphSize3.font);
                    Color cColor = SkymapPanel.this.ringInk(n7, AngleRole.SKY);
                    this.drawBodyMarker(graphics2D, n4, n28, glyphSize3.radius,
                        SkymapPanel.ringBead(AngleRole.SKY), Settings.transitMarker());
                    if (n7 == MOON && SkymapPanel.this.skyRing.valid[SUN]) {
                        double d15 = (SkymapPanel.this.skyRing.lon[MOON] - SkymapPanel.this.skyRing.lon[SUN]) % 360.0;
                        if (d15 < 0.0) d15 += 360.0;
                        SkymapPanel.this.drawMoonPhase(graphics2D, n4, n28, Math.round((float)glyphSize3.radius * 0.44f), d15 / 360.0);
                        continue;
                    }
                    graphics2D.setColor(cColor);
                    String stringC = SkymapPanel.glyphFor(n7, glyphSize3.font);
                    SkymapPanel.drawBodyLabel(graphics2D, stringC, n4, n28, glyphSize3.baseline);
                }
                graphics2D.setComposite(triWas);
            }
            graphics2D.setTransform(bodyTx);
            SkymapPanel.this.paintHover(graphics2D, g);
            graphics2D.setTransform(screenTx);
            SkymapPanel.paintZodiacTag(graphics2D, n10, n11);
            if (onScreen) {
                SkymapPanel.this.paintFitChip(graphics2D);
                SkymapPanel.this.paintScrubTag(graphics2D, n10);
            }
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
        private void drawBodyMarker(Graphics2D graphics2D, int n, int n2, int n3, Color color,
                                    String shape) {
            if (!Settings.showPlanetSpheres()) {
                return;
            }
            if (Settings.MARKER_CUBE.equals(shape)) {
                this.drawMetallicCube(graphics2D, n, n2, n3, color);
            } else if (Settings.MARKER_PYRAMID.equals(shape)) {
                this.drawMetallicPyramid(graphics2D, n, n2, n3, color);
            } else if (Settings.MARKER_SPHERE.equals(shape)) {
                this.drawMetallicSphere(graphics2D, n, n2, n3, color);
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
        private void drawMetallicPyramid(Graphics2D graphics2D, int n, int n2, int n3,
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

        private void drawMetallicCube(Graphics2D graphics2D, int n, int n2, int n3, Color color) {
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
        private void drawMetallicSphere(Graphics2D graphics2D, int n, int n2, int n3, Color color) {
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

        /**
         * How many widening passes make the glow.
         *
         * Three is where it stops being worth it: a fourth pass is wide enough to overlap its
         * neighbours and turns a chart with forty aspects into a haze. Measured by eye on a
         * chart with every point switched on, which is the case that breaks first.
         */
        private static final int GLOW_PASSES = 3;

        private void drawAspectLine(Graphics2D graphics2D, double d, double d2, double d3, int n, int n2, int n3, int n4, int wheel, int n5, int n6) {
            double d4 = Math.abs(d - d2);
            if (d4 > 180.0) {
                d4 = 360.0 - d4;
            }
            // wheel says which ring the first body is on, and used to be a boolean meaning
            // "the outer one" - which could not tell the partner ring from the sky ring, so
            // the two lit each other's lines. Only the partner ring is ever a synastry pair;
            // the sky ring is a moment and is judged at natal orbs, the same as the grid and
            // the hit test judge it.
            boolean bl = wheel != SkymapPanel.WHEEL_NATAL;
            boolean syn = SkymapPanel.this.isSynastryPair(wheel == SkymapPanel.WHEEL_OUTER);
            double d5 = SkymapPanel.this.getOrbFor(n5, n6, syn);
            Color color = null;
            double d6 = 0.0;
            // Which aspect this is, and what colour it takes, both come from the one place
            // that already knows.
            //
            // <b>This used to be a hand-written if/else ladder</b> re-testing 0/60/90/120/180
            // with the colours inlined as RGB triples - a third copy of Aspects.typeOf and a
            // second copy of getAspectColorHex. It had no 150-degree branch for months, so the
            // grid listed quincunxes the wheel could not draw, and adding five more aspects to
            // a hand-written ladder would have been five more chances at the same silence.
            // Now a new constant on Aspects.Type appears here automatically.
            // Through the same gate the grid uses, so a switched-off aspect disappears from
            // both or from neither.
            Aspects.Type drawn = SkymapPanel.this.visibleAspect(d4, n5, n6, syn);
            // The reader's aspect mode applies to the line and not to the aspect. A pair the
            // mode excludes is still computed, still in the grid, still in the reading - it
            // simply does not cross the middle of the wheel. See drawsPair.
            if (drawn != null && !SkymapPanel.this.drawsPair(n5, n6)) {
                drawn = null;
            }
            if (drawn != null) {
                color = Color.decode(SkymapPanel.getAspectColorHex(drawn.label));
                d6 = drawn.exactAngle;
                // The alpha ramp below fades a line as it widens, and it has to fade against
                // the orb this aspect was actually judged by. Against the raw body orb a
                // 1-degree semisextile would render at nearly full strength however loose it is.
                d5 = Aspects.effectiveOrb(SkymapPanel.planetName(n5),
                    SkymapPanel.planetName(n6), drawn, syn);
            }
            if (color != null) {
                double d7 = Math.min(1.0, Math.abs(d4 - d6) / d5);
                float f = (float)Math.pow(1.0 - d7, 2.0);
                int n7 = (int)(35.0f + 220.0f * f);
                n7 = Math.max(20, Math.min(255, n7));
                // Focus, applied on top of the orb fade rather than instead of it: a loose
                // aspect to the focused body is still a loose aspect and should still look
                // like one.
                double focus = SkymapPanel.this.focusWeight(n5, n6, bl);
                n7 = Math.max(6, (int)(n7 * focus));
                // A hovered line is drawn at full strength regardless of how wide its orb is.
                // The normal alpha ramp fades a loose aspect almost to nothing, which is right
                // for the background weave and useless for "show me the one I am pointing at".
                boolean highlighted = SkymapPanel.this.isHighlighted(n5, n6, wheel);
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    highlighted ? 255 : n7);
                float f2 = highlighted ? 3.0f : 0.3f + 0.7f * f;
                double d8 = Math.toRadians(180.0 + d3 - d);
                double d9 = Math.toRadians(180.0 + d3 - d2);
                int n8 = n + (int)((double)n3 * Math.cos(d8));
                int n9 = n2 + (int)((double)n3 * Math.sin(d8));
                int n10 = n + (int)((double)n4 * Math.cos(d9));
                int n11 = n2 + (int)((double)n4 * Math.sin(d9));
                Stroke stroke = graphics2D.getStroke();
                if (highlighted) {
                    // White halo underneath, so the line reads against whichever aspect colour
                    // it is and against the wheel's own spokes.
                    graphics2D.setColor(new Color(255, 255, 255, 90));
                    graphics2D.setStroke(new BasicStroke(f2 + 4.0f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                    graphics2D.drawLine(n8, n9, n10, n11);
                }
                // <b>The glow: a wide, faint pass of the same colour under the crisp line.</b>
                // Real bloom needs a blur, which Java2D will not do cheaply on every frame -
                // but two or three widening strokes at low alpha read as light spilling off a
                // strand, which is the whole effect. The passes are driven by the SAME f as the
                // line, so a tight aspect glows and a loose one barely does: the fade already
                // in this method is what makes the weave legible, and the glow must not undo it
                // by lighting up the aspects the ramp is busy hiding.
                if (!bl) {
                    for (int pass = GLOW_PASSES; pass >= 1; pass--) {
                        int glowAlpha = (int) (n7 * 0.10f * f / pass);
                        if (glowAlpha < 3) {
                            continue;
                        }
                        graphics2D.setColor(new Color(color.getRed(), color.getGreen(),
                            color.getBlue(), Math.min(60, glowAlpha * 3)));
                        graphics2D.setStroke(new BasicStroke(f2 + pass * 2.4f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        graphics2D.drawLine(n8, n9, n10, n11);
                    }
                }

                graphics2D.setColor(color);
                if (bl) {
                    graphics2D.setStroke(new BasicStroke(f2, 0, 0, 10.0f, new float[]{5.0f, 5.0f}, 0.0f));
                } else {
                    // Thinner core than before (was f2 alone at up to 1.0). A strand reads as
                    // light when the bright part is narrow and the spill is wide; a thick core
                    // with a glow around it just looks like a thick line.
                    graphics2D.setStroke(new BasicStroke(Math.max(0.6f, f2 * 0.8f),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }
                graphics2D.drawLine(n8, n9, n10, n11);
                graphics2D.setStroke(stroke);
            }
        }
    }

    public static final class YearScan {
        public List<Transits.EventHit> events = Collections.emptyList();
        public List<Transits.Perfection> perfections = Collections.emptyList();
        public List<SolarArc.Contact> arcs = Collections.emptyList();
        public List<Progressions.Contact> progressions = Collections.emptyList();
        public List<Returns.Contact> returns = Collections.emptyList();
    }
}
