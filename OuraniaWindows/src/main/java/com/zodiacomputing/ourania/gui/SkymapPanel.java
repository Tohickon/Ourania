/*
 * Decompiled with CFR 0.152.
 */
package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
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
    private SweDate baseSd;
    private ZonedDateTime baseChartTime;
    private double baseLatitude = 51.4779;
    private double baseLongitude = 0.0;
    private String baseLocationName = "Los Angeles, CA";
    private String baseTimeZoneId = ZoneId.systemDefault().getId();
    public boolean showTransitChart = false;
    public ChartMode chartMode = ChartMode.SINGLE;

    /**
     * Whether a third (transit-sky) ring is drawn around the synastry bi-wheel.
     *
     * <b>Separate from {@link #showTransitChart}.</b> In SYNASTRY mode that flag means
     * "chart B exists" and is always true; this flag means "the sky is wrapped around
     * both people". The sky data lives in {@link #compositeTransitTime}, which already
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
        return mode == ChartMode.SYNASTRY && transits;
    }
    private SweDate transitSd;
    private ZonedDateTime transitChartTime;
    private double transitLatitude = 51.4779;
    private double transitLongitude = 0.0;

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
    public double[] bLon = new double[BODY_COUNT];
    public double[] bSpeed = new double[BODY_COUNT];
    public boolean[] bValid = new boolean[BODY_COUNT];
    private final boolean[] bOk = new boolean[BODY_COUNT];
    public double[] tLon = new double[BODY_COUNT];
    public double[] tSpeed = new double[BODY_COUNT];
    public boolean[] tValid = new boolean[BODY_COUNT];
    private final boolean[] tOk = new boolean[BODY_COUNT];
    /** Tri-wheel sky positions. Only populated when {@link #showTriWheel} is true. */
    public double[] cLon = new double[BODY_COUNT];
    public double[] cSpeed = new double[BODY_COUNT];
    public boolean[] cValid = new boolean[BODY_COUNT];
    private final boolean[] cOk = new boolean[BODY_COUNT];
    public double[] triCusps = new double[13];
    public double triAscendant;
    private boolean[] shown = Settings.loadBodySelection();
    public double[] activeCusps = new double[13];
    public double activeAscendant;
    public double[] baseCusps = new double[13];
    public double baseAscendant;
    public double[] transitCusps = new double[13];
    public double transitAscendant;
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
     * baseChartTime and transitChartTime are the two PEOPLE - they are what the composite is
     * built from - so there is nowhere to put "now" without a slot of its own. This is that
     * slot, and it is the only reason transits to a composite were not already possible: the
     * astro layer needed no change at all. {@code Transits.toNatal} takes two ChartFrames and
     * has never cared whether the first one is a birth chart.
     *
     * Defaults to now, and the existing Now / Play / step controls drive it while a composite
     * is on screen, which is how someone actually reads composite transits - by sweeping.
     */
    private ZonedDateTime compositeTransitTime;
    private SweDate compositeTransitSd;
    private Timer refreshDebounce;
    public static final String[] SIGN_NAMES;
    private static final double ANGLE_SPEED = 361.0;

    public ZonedDateTime getChartTime() {
        return this.baseChartTime;
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

    private int[] radialLevels(double[] dArray, boolean[] blArray, double d, double d2, double d3) {
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

    private static String glyphFor(int n, Font font) {
        Bodies.Def def = Bodies.at(n);
        return font.canDisplay(def.glyph.codePointAt(0)) ? def.glyph : def.fallback;
    }

    private static boolean aspecting(int n, boolean[] blArray) {
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
        if (this.sw == null || this.baseSd == null || this.chartPanel == null) {
            return null;
        }
        int w = this.chartPanel.getWidth();
        int h = this.chartPanel.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        int cx = w / 2;
        int cy = h / 2;
        int[] rings = SkymapPanel.ringRadii(w, h, this.showTransitChart, this.showTriWheel);
        int triOuter   = rings[RING_TRI];
        int transitRing = rings[RING_TRANSIT];
        int decanOuter  = rings[RING_DECAN_OUTER];
        int bodyBase    = this.bodyBaseRadius(rings);
        double pin = this.getPinLongitude();

        if (this.showTriWheel) {
            int[] radii = this.triWheelRadii(triOuter, transitRing);
            int i = this.nearestPoint(x, y, cx, cy, pin, this.cLon, this.cValid, radii, true);
            if (i >= 0) {
                return this.hoverHtml(i, this.cLon[i], this.cSpeed[i], true);
            }
        }
        if (this.showTransitChart) {
            int[] radii = this.transitRadii(transitRing, decanOuter);
            int i = this.nearestPoint(x, y, cx, cy, pin, this.tLon, this.tValid, radii, true);
            if (i >= 0) {
                return this.hoverHtml(i, this.tLon[i], this.tSpeed[i], true);
            }
        }
        int[] radii = this.natalRadii(bodyBase);
        int i = this.nearestPoint(x, y, cx, cy, pin, this.bLon, this.bValid, radii, false);
        return i >= 0 ? this.hoverHtml(i, this.bLon[i], this.bSpeed[i], false) : null;
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
    private int bodyAt(int x, int y) {
        if (this.sw == null || this.baseSd == null || this.chartPanel == null) {
            return -1;
        }
        int w = this.chartPanel.getWidth();
        int h = this.chartPanel.getHeight();
        if (w <= 0 || h <= 0) {
            return -1;
        }
        int cx = w / 2;
        int cy = h / 2;
        int[] rings = SkymapPanel.ringRadii(w, h, this.showTransitChart, this.showTriWheel);
        double pin = this.getPinLongitude();

        if (this.showTriWheel) {
            int[] radii = this.triWheelRadii(rings[RING_TRI], rings[RING_TRANSIT]);
            int i = this.nearestPoint(x, y, cx, cy, pin, this.cLon, this.cValid, radii, true);
            if (i >= 0) {
                return i | TRANSIT_BIT;
            }
        }
        if (this.showTransitChart) {
            int[] radii = this.transitRadii(rings[RING_TRANSIT], rings[RING_DECAN_OUTER]);
            int i = this.nearestPoint(x, y, cx, cy, pin, this.tLon, this.tValid, radii, true);
            if (i >= 0) {
                return i | TRANSIT_BIT;
            }
        }
        int[] radii = this.natalRadii(this.bodyBaseRadius(rings));
        return this.nearestPoint(x, y, cx, cy, pin, this.bLon, this.bValid, radii, false);
    }

    /** Marks a packed hit as belonging to the outer wheel. Above any registry index. */
    private static final int TRANSIT_BIT = 1 << 16;

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
        double[] mine = transit ? this.tLon : this.bLon;
        double[] theirs = this.focusTransit ? this.tLon : this.bLon;
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
    private String hoverHtml(int n, double lon, double speed, boolean transit) {
        Bodies.Def def = Bodies.at(n);
        int signIdx = Zodiac.signIndex(lon);
        String sign = Zodiac.SIGNS[signIdx];
        int degInSign = (int)(lon % 30.0);
        int minInSign = (int)((lon % 30.0 - (double)degInSign) * 60.0);
        int decan = Zodiac.decan(lon);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:250px; font-family:SansSerif; font-size:11px;'>");
        sb.append("<div style='font-size:13px;'><b>").append(def.name).append("</b>");
        if (transit) {
            sb.append(" <span style='color:#5A7FBF;'>(transiting)</span>");
        }
        if (!def.isAngle() && speed < 0.0) {
            sb.append(" <span style='color:#B03030;'><b>R</b></span>");
        }
        sb.append("</div>");

        sb.append("<div><b>").append(degInSign).append("&deg;")
          .append(minInSign < 10 ? "0" : "").append(minInSign).append("'</b> ")
          .append(SkymapPanel.capitalise(sign));
        int house = Zodiac.houseOf(lon, transit ? this.transitCusps : this.activeCusps);
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

    private static final GlyphSize[] NATAL_SIZES = {
        new GlyphSize(19, 32),      // lights
        new GlyphSize(16, 28),      // inner planets
        new GlyphSize(14, 25),      // outer planets
        new GlyphSize(11, 20)       // asteroids and points
    };

    private static final GlyphSize[] TRANSIT_SIZES = {
        new GlyphSize(17, 28),
        new GlyphSize(14, 24),
        new GlyphSize(12, 21),
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

    /** Radial distance between one ring and the next, natal wheel. */
    private static final double NATAL_RING_GAP = 40.0;
    /** Stagger applied to a body that collides with one already placed in its ring. */
    private static final double NATAL_RING_STEP = 16.0;
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
    private int[] ringedRadii(double[] dArray, boolean[] blArray, double base, double gap,
                              double step, double spacing, double floorPx) {
        int[][] nArray = new int[RING_COUNT][];
        for (int i = 0; i < RING_COUNT; ++i) {
            nArray[i] = this.radialLevels(dArray, SkymapPanel.restrictToRing(blArray, i),
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
        int base = rings[RING_SIGN_INNER];
        String ring = Settings.bodyRing();
        if (Settings.RING_CENTRE.equals(ring)) {
            // Well inside the rings, leaving the sign band and the ticks clear.
            return (int) (base * 0.62);
        }
        if (Settings.RING_OUTSIDE.equals(ring)) {
            // Just beyond the sign band, the way many traditional charts print them.
            return rings[RING_SIGN_OUTER] + 14;
        }
        return base;
    }

    private int[] natalRadii(int n) {
        double d = (double)n - 30.0;
        double d2 = (double)n - 14.0;
        int[] nArray = this.ringedRadii(this.bLon, SkymapPanel.restrict(this.bValid, false),
            d, NATAL_RING_GAP, NATAL_RING_STEP, 32.0, 34.0);
        int[] nArray2 = this.radialLevels(this.bLon, SkymapPanel.restrict(this.bValid, true), d2, 22.0, 30.0);
        int[] nArray3 = new int[BODY_COUNT];
        for (int i = 0; i < BODY_COUNT; ++i) {
            nArray3[i] = Bodies.at(i).isAngle() ? (int)Math.max(d2 - (double)nArray2[i] * 22.0, 40.0) : nArray[i];
        }
        return nArray3;
    }

    private int[] transitRadii(int n, int n2) {
        double d = (double)(n2 + n) / 2.0;
        int[] nArray = this.ringedRadii(this.tLon, SkymapPanel.restrict(this.tValid, false),
            (double)n, TRANSIT_RING_GAP, TRANSIT_RING_STEP, 28.0, 40.0);
        int[] nArray2 = this.radialLevels(this.tLon, SkymapPanel.restrict(this.tValid, true), d, 18.0, 28.0);
        int[] nArray3 = new int[BODY_COUNT];
        for (int i = 0; i < BODY_COUNT; ++i) {
            nArray3[i] = Bodies.at(i).isAngle() ? (int)Math.max(d - (double)nArray2[i] * 18.0, 40.0) : nArray[i];
        }
        return nArray3;
    }

    /**
     * Per-body radii for the tri-wheel (outermost sky ring).
     *
     * Mirrors {@link #transitRadii} but reads {@link #cLon}/{@link #cValid} and sits in
     * the band between {@code RING_OUTER} and {@code RING_TRI}. The ring gap is the same
     * 20-pixel transit gap — the band is the same width.
     *
     * @param triOuter  outer edge of the tri-wheel band   (rings[RING_TRI])
     * @param triInner  inner edge of the tri-wheel band   (rings[RING_TRANSIT])
     */
    private int[] triWheelRadii(int triOuter, int triInner) {
        double mid = (double)(triInner + triOuter) / 2.0;
        int[] nArray = this.ringedRadii(this.cLon, SkymapPanel.restrict(this.cValid, false),
            (double)triOuter, TRANSIT_RING_GAP, TRANSIT_RING_STEP, 28.0, 40.0);
        int[] nArray2 = this.radialLevels(this.cLon, SkymapPanel.restrict(this.cValid, true), mid, 18.0, 28.0);
        int[] nArray3 = new int[BODY_COUNT];
        for (int i = 0; i < BODY_COUNT; ++i) {
            nArray3[i] = Bodies.at(i).isAngle()
                ? (int)Math.max(mid - (double)nArray2[i] * 18.0, 40.0) : nArray[i];
        }
        return nArray3;
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
                    this.baseLatitude = ((Geocoder.Result)object).lat;
                    this.baseLongitude = ((Geocoder.Result)object).lon;
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
        this.baseChartTime = ZonedDateTime.now(ZoneId.of(this.baseTimeZoneId));
        this.transitChartTime = ZonedDateTime.now(ZoneId.of(this.transitTimeZoneId));
        this.updateChartData();
        this.chartPanel = new ChartPanel();
        this.chartPanel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                SkymapPanel.this.handleChartClick(mouseEvent.getX(), mouseEvent.getY());
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                // Without this the chart stays dimmed around whatever the cursor left on,
                // which reads as a rendering fault rather than as a selection.
                if (SkymapPanel.this.setFocus(-1)) {
                    SkymapPanel.this.chartPanel.repaint();
                }
            }
        });
        this.chartPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter(){

            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                // The tooltip is no longer pushed from here. ChartPanel overrides
                // getToolTipText(MouseEvent) and ToolTipManager asks it, which is the whole
                // fix: see the note on that override.
                // Focus follows the cursor. setFocus reports whether anything actually
                // changed, so sweeping across one glyph repaints once rather than on every
                // pixel of travel - the same guard setHighlightedAspect already uses.
                if (SkymapPanel.this.setFocus(
                        SkymapPanel.this.bodyAt(mouseEvent.getX(), mouseEvent.getY()))) {
                    SkymapPanel.this.chartPanel.repaint();
                }
            }
        });
        // A chart is read by sweeping across it, so the default 750ms feels broken here.
        // <b>Registered once, with a non-null placeholder, and never unregistered.</b>
        // The text itself comes from getToolTipText(MouseEvent) below.
        this.chartPanel.setToolTipText("");
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay(220);
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(20000);
        this.add((Component)this.chartPanel, "Center");
        controlPanel = this.createControlPanel();
        this.add((Component)controlPanel, "South");
        this.animationTimer = new Timer(50, actionEvent -> {
            if (this.isPlaying) {
                this.stepTime();
                this.updateChartData();
                this.chartPanel.repaint();
            }
        });
        this.animationTimer.start();
    }

    public void applyChartSettings(final String string, final String string2, final String string3, ChartMode chartMode, final String string4, final String string5, final String string6, final boolean transits) {
        this.chartMode = chartMode;
        final boolean bl = chartMode != ChartMode.SINGLE;
        this.transitsEnabled = transits;
        this.showTransitChart = SkymapPanel.outerWheelShown(chartMode, transits);
        this.showTriWheel    = SkymapPanel.triWheelShown(chartMode, transits);
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
        final boolean bl2 = this.isPlaying;
        this.isPlaying = false;
        SwingWorker<Void, Void> swingWorker = new SwingWorker<Void, Void>(){

            @Override
            protected Void doInBackground() throws Exception {
                Object comparable;
                Object object;
                Geocoder.Result result = SkymapPanel.this.syncGeocode(string3);
                if (result != null) {
                    SkymapPanel.this.baseLatitude = result.lat;
                    SkymapPanel.this.baseLongitude = result.lon;
                    SkymapPanel.this.baseLocationName = result.name;
                    SkymapPanel.this.baseTimeZoneId = result.tzId;
                }
                try {
                    object = LocalDate.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    comparable = (Object) LocalTime.parse(string2, DateTimeFormatter.ofPattern("HH:mm"));
                    SkymapPanel.this.baseChartTime = ZonedDateTime.of((LocalDate)object, (LocalTime)comparable, ZoneId.of(SkymapPanel.this.baseTimeZoneId));
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
                if (bl) {
                    object = SkymapPanel.this.syncGeocode(string6);
                    if (object != null) {
                        SkymapPanel.this.transitLatitude = ((Geocoder.Result)object).lat;
                        SkymapPanel.this.transitLongitude = ((Geocoder.Result)object).lon;
                        SkymapPanel.this.transitLocationName = ((Geocoder.Result)object).name;
                        SkymapPanel.this.transitTimeZoneId = ((Geocoder.Result)object).tzId;
                    }
                    try {
                        comparable = (Object) LocalDate.parse(string4, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        LocalTime localTime = LocalTime.parse(string5, DateTimeFormatter.ofPattern("HH:mm"));
                        SkymapPanel.this.transitChartTime = ZonedDateTime.of((LocalDate)comparable, localTime, ZoneId.of(SkymapPanel.this.transitTimeZoneId));
                    }
                    catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                SkymapPanel.this.updateChartData();
                SkymapPanel.this.chartPanel.repaint();
                SkymapPanel.this.isPlaying = bl2;
            }
        };
        swingWorker.execute();
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
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        label.setToolTipText(fullName);
        return label;
    }

    private void showAnnualCalendar() {
        if (this.sw == null || this.baseChartTime == null || this.window == null) {
            return;
        }
        final int n = this.baseChartTime.getYear();
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
     * Computed rather than read off tLon/tValid on purpose. Those arrays carry longitudes
     * and nothing else; the cross-chart questions need chart B's HOUSE CUSPS and its four
     * angles, which only a ChartFrame has. The two agree by construction - same julian day,
     * same place, same house system, same ephemeris - so this is a second view of chart B,
     * not a second opinion about it.
     */
    private ChartFrame synastryChartB() {
        if (this.sw == null || this.transitSd == null) {
            return null;
        }
        String string = SkymapPanel.frameKey(this.transitSd.getJulDay(), this.transitLatitude,
            this.transitLongitude, this.houseSystem);
        ChartFrame chartFrame = this.synastryBFrame;
        if (chartFrame != null && string.equals(this.synastryBKey)) {
            return chartFrame;
        }
        this.synastryBFrame = chartFrame = ChartFrame.compute(this.sw, this.transitSd.getJulDay(),
            this.transitLatitude, this.transitLongitude, this.houseSystem, false, 0.0);
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
                || this.sw == null || this.baseSd == null || this.transitSd == null) {
            return null;
        }
        ChartFrame host = this.frameForCurrentChart(this.baseSd.getJulDay(),
            this.baseLatitude, this.baseLongitude, this.houseSystem);
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
        if (this.sw == null || this.baseSd == null || this.transitSd == null) {
            return "";
        }
        ChartFrame chartFrame = this.frameForCurrentChart(this.baseSd.getJulDay(),
            this.baseLatitude, this.baseLongitude, this.houseSystem);
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
        return d + "|" + d2 + "|" + d3 + "|" + n;
    }

    private ChartFrame frameForCurrentChart(double d, double d2, double d3, int n) {
        ChartFrame chartFrame;
        String string = SkymapPanel.frameKey(d, d2, d3, n);
        ChartFrame chartFrame2 = this.cachedFrame;
        if (chartFrame2 != null && string.equals(this.frameCacheKey)) {
            return chartFrame2;
        }
        this.cachedFrame = chartFrame = ChartFrame.compute(this.sw, d, d2, d3, n, false, 0.0);
        this.frameCacheKey = string;
        return chartFrame;
    }

    /**
     * What stands in a span of the zodiac on the TRANSIT wheel; empty when none is shown.
     *
     * One method rather than three accessors on purpose. tLon/tSpeed/tValid are public and
     * a caller could read them directly, but three parallel arrays are three chances to
     * index one of them differently, and the interpretation panel has no business knowing
     * how the transit wheel stores itself.
     */
    public java.util.List<Occupants.Hit> transitOccupants(double start, double span) {
        if (!this.showTransitChart) {
            return java.util.Collections.emptyList();
        }
        return Occupants.inSpan(this.tLon, this.tValid, this.tSpeed, start, span);
    }

    /**
     * The chart on screen, in whatever harmonic is selected.
     *
     * <b>Wraps the radix accessor rather than replacing it</b> so the harmonic is applied in
     * one place to whatever the mode produced - natal, midpoint composite or Davison - and a
     * mode added later inherits it without anyone remembering to. At H1 this returns the
     * radix frame itself, unchanged and uncopied.
     */
    public ChartFrame getCurrentChart() {
        return com.zodiacomputing.ourania.astro.Harmonics.of(radixChart(), this.harmonic);
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
        if (composite && this.baseSd != null && this.transitSd != null) {
            // Its own field and its own key. Sharing cachedFrame with frameForCurrentChart is
            // what made a composite silently become person A's natal chart - see the field.
            // The reference place is part of the key. It changes the cusps, so leaving it out
            // would serve the previous location's house frame from cache after the user
            // changed it - visible only as houses that quietly disagree with the setting.
            String key = this.chartMode + "|" + SkymapPanel.frameKey(this.baseSd.getJulDay(),
                    this.baseLatitude, this.baseLongitude, this.houseSystem)
                + "|" + SkymapPanel.frameKey(this.transitSd.getJulDay(),
                    this.transitLatitude, this.transitLongitude, this.houseSystem)
                + "|ref:" + this.compositeRefLat + "," + this.compositeRefLon;
            if (this.relationshipFrame != null && key.equals(this.relationshipCacheKey)) {
                return this.relationshipFrame;
            }
            ChartFrame a = ChartFrame.compute(this.sw, this.baseSd.getJulDay(),
                this.baseLatitude, this.baseLongitude, this.houseSystem, false, 0.0);
            ChartFrame b = ChartFrame.compute(this.sw, this.transitSd.getJulDay(),
                this.transitLatitude, this.transitLongitude, this.houseSystem, false, 0.0);
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
        if (this.baseSd != null) {
            return this.frameForCurrentChart(this.baseSd.getJulDay(), this.baseLatitude,
                this.baseLongitude, this.houseSystem);
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
        if (this.sw == null || this.baseSd == null || this.window == null) {
            return;
        }
        final double jd = this.baseSd.getJulDay();
        final double lat = this.baseLatitude;
        final double lon = this.baseLongitude;
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
                // These two are read against a moment, not just a chart, so they take the
                // birth instant and today. The transit clock drives them when it is set, so
                // stepping time moves the progressed chart with it.
                double now = SkymapPanel.this.showTransitChart && SkymapPanel.this.transitSd != null
                        ? SkymapPanel.this.transitSd.getJulDay()
                        : new SweDate().getJulDay();
                if ("PROGRESSED".equals(kind)) {
                    return ChartTables.progressed(f, SkymapPanel.this.sw, jd, now);
                }
                if ("SOLARARC".equals(kind)) {
                    return ChartTables.solarArc(f, SkymapPanel.this.sw, jd, now);
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
        if (this.sw == null || this.baseSd == null || this.window == null || readingTier == ReadingTier.NONE) {
            return;
        }
        this.readingTier = readingTier;
        final double d = this.baseSd.getJulDay();
        final double d2 = this.baseLatitude;
        final double d3 = this.baseLongitude;
        final char c = this.houseSystem;
        final String string = this.baseLocationName;
        final String string2 = this.baseChartTime == null ? "" : this.baseChartTime.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm z"));
        final boolean bl = this.showTransitChart && this.transitSd != null;
        final double d4 = bl ? this.transitSd.getJulDay() : Double.NaN;
        final double d5 = this.transitLatitude;
        final double d6 = this.transitLongitude;
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
                        list3 = Convergence.collect(profection, yearScan.perfections, yearScan.events, yearScan.arcs, yearScan.progressions, yearScan.returns);
                    }
                }
                if (readingTier == ReadingTier.REPORT) {
                    object = Snapshot.report(chartFrame, result, list, result2) + Snapshot.topicLayer(Topics.analyse(chartFrame, list));
                    if (bl) {
                        // The frame and the moment carry the releasing section, which was
                        // computed to four levels and appeared in no reading until 2026-09-02.
                        object = (String)object + Snapshot.timeLayer(profection, list, list2,
                            yearScan.events, list3, chartFrame, d4);   // d4 is the moment;
                        // d2 is baseLatitude in this decompiled scope, and passing it here
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
                this.baseChartTime = ZonedDateTime.now(ZoneId.of(this.baseTimeZoneId));
            }
            if (bl) {
                if (this.isRelationshipChart()) {
                    this.compositeTransitTime = ZonedDateTime.now(ZoneId.of(this.transitTimeZoneId));
                } else {
                    this.transitChartTime = ZonedDateTime.now(ZoneId.of(this.transitTimeZoneId));
                }
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
        });
        SkymapPanel.styleCompactCombo(jComboBox2);
        JLabel jLabel6 = compactLabel("Houses:", "House System");
        jLabel6.setForeground(Color.WHITE);
        this.chartControls.add(jLabel6);
        this.chartControls.add(jComboBox2);
        jPanel.add(jPanel2);
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
                jPanel3.invalidate();
                jPanel.revalidate();
            }
        });
        return jPanel;
    }

    private void handleChartClick(int n, int n2) {
        // <b>Pin first, before the branches below decide what was clicked.</b> This method has
        // several exits - angle, transit body, natal body, aspect line, empty space - and
        // pinning inside one of them would leave the others silently unpinned. Asked through
        // bodyAt, which is the same hit test the hover card and the highlight use.
        if (this.chartPanel != null && this.pinFocus(this.bodyAt(n, n2))) {
            this.chartPanel.repaint();
        }
        if (this.window != null && this.focusBody >= 0) {
            double[] lon = this.focusTransit ? this.tLon : this.bLon;
            double[] spd = this.focusTransit ? this.tSpeed : this.bSpeed;
            if (this.focusBody < lon.length) {
                // The same card the tooltip builds - one description of a body, shown in two
                // places rather than written twice.
                this.window.showSelection(
                    this.hoverHtml(this.focusBody, lon[this.focusBody], spd[this.focusBody],
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
                    return;
                }
            }
        }
        double d;
        double d2;
        double d3;
        int n3;
        int n4;
        if (this.sw == null || this.baseSd == null || this.window == null) {
            return;
        }
        int n5 = this.chartPanel.getWidth();
        int n6 = this.chartPanel.getHeight();
        int n7 = n5 / 2;
        int n8 = n6 / 2;
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
        int[] rings = SkymapPanel.ringRadii(n5, n6, this.showTransitChart, this.showTriWheel);
        int n9 = n4 = rings[RING_OUTER];
        int nTri = rings[RING_TRI];
        int n10 = rings[RING_TRANSIT];
        int n11 = rings[RING_DECAN_OUTER];
        int n12 = rings[RING_SIGN_OUTER];
        int n13 = this.bodyBaseRadius(rings);
        double[] dArray = this.activeCusps;
        double d8 = this.getPinLongitude();
        double d9 = (180.0 + d8 - d7) % 360.0;
        if (d9 < 0.0) {
            d9 += 360.0;
        }
        int[] nArray = this.natalRadii(n13);
        int[] nArray2 = this.transitRadii(n10, n11);
        int[] nArrayC = this.triWheelRadii(nTri, n10);
        for (n3 = 0; n3 < BODY_COUNT; ++n3) {
            if (!this.bValid[n3] || !Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArray[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.bLon[n3]))), (double)n2 - (d = (double)n8 + (double)nArray[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, false))) continue;
            this.showAngleAt(n3, this.bLon[n3]);
            return;
        }
        // Tri-wheel angle hit test before the synastry ring, because the tri ring is outermost.
        if (this.showTriWheel) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                if (!this.cValid[n3] || !Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArrayC[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.cLon[n3]))), (double)n2 - (d = (double)n8 + (double)nArrayC[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                this.showAngleAt(n3, this.cLon[n3], true);
                return;
            }
        }
        if (this.showTransitChart) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                if (!this.tValid[n3] || !Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArray2[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.tLon[n3]))), (double)n2 - (d = (double)n8 + (double)nArray2[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                this.showAngleAt(n3, this.tLon[n3]);
                return;
            }
        }
        if (this.showTransitChart && "Both".equals(this.houseAlignment) && d6 >= (double)(n11 - 6) && d6 <= (double)(n10 + 6)) {
            for (n3 = 1; n3 <= 12; ++n3) {
                d2 = (180.0 + d8 - this.transitCusps[n3]) % 360.0;
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
        if (this.showTriWheel && d6 >= (double)n10 && d6 <= (double)(nTri + 10)) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                int n14c;
                if (!this.cValid[n3] || Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArrayC[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.cLon[n3]))), (double)n2 - (d = (double)n8 + (double)nArrayC[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                int n15c = (int)(this.cLon[n3] / 30.0);
                int n16c = (int)(this.cLon[n3] % 30.0 / 10.0) + 1;
                int n17c = 1;
                for (int i = 1; i <= 12; ++i) {
                    double d10; double d11;
                    double d12 = dArray[i];
                    double d13 = d11 = i == 12 ? dArray[1] : dArray[i + 1];
                    if (d11 < d12) d11 += 360.0;
                    if ((d10 = this.cLon[n3]) < d12 && d11 > 360.0) d10 += 360.0;
                    if (!(d10 >= d12) || !(d10 < d11)) continue;
                    n17c = i; break;
                }
                // The sky ring aspects BOTH people, so this walks bLon (chart A) and then
                // tLon (chart B). Listing only chart A was the tri-wheel's first shipped
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
                    if (!this.bValid[n14c]) continue;
                    double d14 = Math.abs(this.cLon[n3] - this.bLon[n14c]);
                    if (d14 > 180.0) d14 = 360.0 - d14;
                    if ((string = this.getAspectType(d14, n3, n14c, this.isSynastryPair(false))) == null) continue;
                    boolean blA = this.isAspectApplying(this.cLon[n3], this.cSpeed[n3], this.bLon[n14c], this.bSpeed[n14c], string);
                    String string2 = this.describeAspect(d14, string, n3, n14c, this.cLon[n3], this.bLon[n14c], blA);
                    arrayListC.add(new String[]{BODY_NAMES[n14c], string, string2, "Chart A"});
                }
                for (n14c = 0; n14c < BODY_COUNT; ++n14c) {
                    String string;
                    if (!this.tValid[n14c]) continue;
                    double d14 = Math.abs(this.cLon[n3] - this.tLon[n14c]);
                    if (d14 > 180.0) d14 = 360.0 - d14;
                    if ((string = this.getAspectType(d14, n3, n14c, this.isSynastryPair(false))) == null) continue;
                    boolean blB = this.isAspectApplying(this.cLon[n3], this.cSpeed[n3], this.tLon[n14c], this.tSpeed[n14c], string);
                    String string2 = this.describeAspect(d14, string, n3, n14c, this.cLon[n3], this.tLon[n14c], blB);
                    arrayListC.add(new String[]{BODY_NAMES[n14c], string, string2, "Chart B"});
                }
                n14c = (int)(this.cLon[n3] % 30.0) + 1;
                this.window.showInterpretationForPlanet("transit_" + BODY_NAMES[n3].toLowerCase(), SIGN_NAMES[n15c], n14c, n16c, n17c, arrayListC);
                return;
            }
        }
        if (this.showTransitChart && d6 >= (double)n11 && d6 <= (double)(n10 + 10)) {
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                int n14;
                if (!this.tValid[n3] || Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d3 = (double)n7 + (double)nArray2[n3] * Math.cos(d2 = Math.toRadians(180.0 + d8 - this.tLon[n3]))), (double)n2 - (d = (double)n8 + (double)nArray2[n3] * Math.sin(d2))) < (double)SkymapPanel.hitRadius(n3, true))) continue;
                int n15 = (int)(this.tLon[n3] / 30.0);
                int n16 = (int)(this.tLon[n3] % 30.0 / 10.0) + 1;
                int n17 = 1;
                for (int i = 1; i <= 12; ++i) {
                    double d10;
                    double d11;
                    double d12 = dArray[i];
                    double d13 = d11 = i == 12 ? dArray[1] : dArray[i + 1];
                    if (d11 < d12) {
                        d11 += 360.0;
                    }
                    if ((d10 = this.tLon[n3]) < d12 && d11 > 360.0) {
                        d10 += 360.0;
                    }
                    if (!(d10 >= d12) || !(d10 < d11)) continue;
                    n17 = i;
                    break;
                }
                ArrayList<String[]> arrayList = new ArrayList<String[]>();
                for (n14 = 0; n14 < BODY_COUNT; ++n14) {
                    String string;
                    if (!this.bValid[n14]) continue;
                    double d14 = Math.abs(this.tLon[n3] - this.bLon[n14]);
                    if (d14 > 180.0) {
                        d14 = 360.0 - d14;
                    }
                    if ((string = this.getAspectType(d14, n3, n14, this.isSynastryPair(true))) == null) continue;
                    boolean bl = this.isAspectApplying(this.tLon[n3], this.tSpeed[n3], this.bLon[n14], this.bSpeed[n14], string);
                    String string2 = this.describeAspect(d14, string, n3, n14, this.tLon[n3], this.bLon[n14], bl);
                    arrayList.add(new String[]{BODY_NAMES[n14], string, string2});
                }
                n14 = (int)(this.tLon[n3] % 30.0) + 1;
                this.window.showInterpretationForPlanet("transit_" + BODY_NAMES[n3].toLowerCase(), SIGN_NAMES[n15], n14, n16, n17, arrayList);
                return;
            }
        }
        if (d6 < (double)n13) {
            int n18;
            boolean bl;
            for (n3 = 0; n3 < BODY_COUNT; ++n3) {
                int n19;
                double d15;
                if (!this.bValid[n3] || Bodies.at(n3).isAngle() || !(Math.hypot((double)n - (d = (double)n7 + (double)nArray[n3] * Math.cos(d3 = Math.toRadians(180.0 + d8 - (d2 = this.bLon[n3])))), (double)n2 - (d15 = (double)n8 + (double)nArray[n3] * Math.sin(d3))) < (double)SkymapPanel.hitRadius(n3, false))) continue;
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
                    if (n3 == n19 || !this.bValid[n19]) continue;
                    double d20 = Math.abs(d2 - this.bLon[n19]);
                    if (d20 > 180.0) {
                        d20 = 360.0 - d20;
                    }
                    if ((string = this.getAspectType(d20, n3, n19, false)) == null) continue;
                    boolean bl2 = this.isAspectApplying(d2, this.bSpeed[n3], this.bLon[n19], this.bSpeed[n19], string);
                    String string3 = this.describeAspect(d20, string, n3, n19, d2, this.bLon[n19], bl2);
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
                    if (!SkymapPanel.aspecting(n18, this.bValid)) continue;
                    for (int i = n18 + 1; i < BODY_COUNT; ++i) {
                        if (!SkymapPanel.aspecting(i, this.bValid) || Bodies.isOppositePair(n18, i) || !this.checkAspectHit(n, n2, n7, n8, d8, nArray[n18], nArray[i], this.bLon[n18], this.bLon[i], BODY_NAMES[n18], BODY_NAMES[i])) continue;
                        return;
                    }
                }
            }
            if (bl) {
                for (n18 = 0; n18 < BODY_COUNT; ++n18) {
                    if (!SkymapPanel.aspecting(n18, this.tValid)) continue;
                    for (int i = 0; i < BODY_COUNT; ++i) {
                        if (!SkymapPanel.aspecting(i, this.bValid) || !this.checkAspectHit(n, n2, n7, n8, d8, nArray2[n18], nArray[i], this.tLon[n18], this.bLon[i], "transit_" + BODY_NAMES[n18].toLowerCase(), BODY_NAMES[i])) continue;
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
        if (d6 >= (double)n13 && d6 < (double)n12) {
            n3 = (int)(d9 / 30.0);
            this.window.showInterpretationForSign(SIGN_NAMES[n3]);
            return;
        }
        if (d6 >= (double)n12 && d6 < (double)n11) {
            n3 = (int)(d9 / 30.0);
            int n23 = (int)(d9 % 30.0 / 10.0) + 1;
            this.window.showInterpretationForDecan(SIGN_NAMES[n3], n23);
            return;
        }
        // The lunar mansion ring, tested BEFORE the Sabian degree because the two share this
        // band and the ring is the half you can see.
        //
        // The mansions are drawn from n9-9 outward; the degree ticks reach n9-6. Anything on
        // that visible band is a mansion click. The Sabian symbol keeps the strip just inside
        // it, n9-15 to n9-9, where nothing is drawn - it is a per-degree reading with no ring
        // of its own, so it loses the contested pixels rather than the feature that has a
        // ring drawn on them.
        if (SkymapPanel.inMansionBand(d6, n9)) {
            this.window.showInterpretationForMansion(
                com.zodiacomputing.ourania.astro.LunarMansions.at(d9).number);
            return;
        }
        if (SkymapPanel.inSabianBand(d6, n9)) {
            n3 = (int)Math.round(d9) % 360;
            if (n3 < 0) {
                n3 += 360;
            }
            int n24 = n3 / 30;
            int n25 = n3 % 30 + 1;
            this.window.showInterpretationForSabianSymbol(SIGN_NAMES[n24], n25);
            return;
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
    private void showAngleAt(int n, double d) {
        this.showAngleAt(n, d, false);
    }

    /**
     * The reading for a clicked angle, with its aspects to the bodies on the other wheel.
     *
     * <b>One other wheel in every mode but one.</b> A natal angle reads against the natal
     * bodies; a composite angle reads against the composite's, because a composite mode loads
     * the composite INTO the base arrays (see updateChartData) rather than special-casing it
     * downstream; a transit angle reads against the natal; and chart B's angle reads against
     * chart A. In each of those there is exactly one other chart and {@code bLon} is it.
     *
     * <b>The tri-wheel is the exception, and the reason for {@code fromSky}.</b> A sky angle
     * has TWO other charts under it, and walking only {@code bLon} silently dropped every
     * contact to chart B - the same defect the sky BODY click had. When {@code fromSky} is
     * true and a tri-wheel is up, both passes run and each row is tagged with the chart it
     * came from, because "Ascendant square Sun" is a different statement about each person.
     *
     * <b>Rows are tagged only in that case.</b> Every other caller still emits three-element
     * rows and renders exactly as it did, so no existing reading changes wording.
     */
    private void showAngleAt(int n, double d, boolean fromSky) {
        int n2 = (int)(d / 30.0) % 12;
        int n3 = (int)(d % 30.0) + 1;
        boolean bothCharts = fromSky && this.showTriWheel;
        ArrayList<String[]> arrayList = new ArrayList<String[]>();
        for (int i = 0; i < BODY_COUNT; ++i) {
            String string;
            if (!SkymapPanel.aspecting(i, this.bValid) || Bodies.isOppositePair(n, i)) continue;
            double d2 = Math.abs(d - this.bLon[i]);
            if (d2 > 180.0) {
                d2 = 360.0 - d2;
            }
            if ((string = this.getAspectType(d2, n, i, false)) == null) continue;
            boolean bl = this.isAspectApplying(d, 361.0, this.bLon[i], this.bSpeed[i], string);
            String string2 = this.describeAspect(d2, string, n, i, d, this.bLon[i], bl);
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
                if (!SkymapPanel.aspecting(i, this.tValid) || Bodies.isOppositePair(n, i)) continue;
                double d2 = Math.abs(d - this.tLon[i]);
                if (d2 > 180.0) {
                    d2 = 360.0 - d2;
                }
                if ((string = this.getAspectType(d2, n, i, this.isSynastryPair(false))) == null) continue;
                boolean bl = this.isAspectApplying(d, 361.0, this.tLon[i], this.tSpeed[i], string);
                String string2 = this.describeAspect(d2, string, n, i, d, this.tLon[i], bl);
                arrayList.add(new String[]{BODY_NAMES[i], string, string2, "Chart B"});
            }
        }
        this.window.showInterpretationForAngle(BODY_NAMES[n], SIGN_NAMES[n2], n3, arrayList);
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

    /** The Settings screen changed which aspects are drawn. */
    public void reloadAspectSelection() {
        this.aspectShown = Settings.loadAspectSelection();
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

    /** The aspect the grid is currently hovering, as body indices. -1 when nothing is hovered. */
    private int highlightA = -1;
    private int highlightB = -1;
    private boolean highlightTransit;

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

    /** How far in the Sabian degree target reaches. Nothing is drawn on this strip. */
    static final int SABIAN_BAND_DEPTH = 15;

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
        return radius >= outer - MANSION_BAND_DEPTH && radius <= outer + 15;
    }

    /** True when a click radius lands on the Sabian degree strip inside the mansion ring. */
    static boolean inSabianBand(double radius, int outer) {
        return radius >= outer - SABIAN_BAND_DEPTH && radius < outer - MANSION_BAND_DEPTH;
    }

    /** Indices into {@link #ringRadii}. */
    static final int RING_OUTER = 0;
    static final int RING_TRI = 1;
    static final int RING_TRANSIT = 2;
    static final int RING_DECAN_OUTER = 3;
    static final int RING_SIGN_OUTER = 4;
    static final int RING_SIGN_INNER = 5;

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
     * Reading outward: the sign band runs from {@code RING_SIGN_INNER} to
     * {@code RING_SIGN_OUTER}, the decan band from there to {@code RING_DECAN_OUTER}, and the
     * degree ticks hang inside {@code RING_OUTER}. {@code RING_TRANSIT} is only meaningful
     * when the transit wheel is shown. {@code RING_TRI} is the outermost band and only
     * meaningful when the tri-wheel is shown; it sits between RING_OUTER and RING_TRANSIT.
     *
     * When neither tri-wheel nor transit is shown, RING_TRI == RING_TRANSIT == RING_OUTER-20
     * so existing callers that read only RING_TRANSIT, RING_DECAN_OUTER, RING_SIGN_OUTER and
     * RING_SIGN_INNER are unaffected.
     */
    static int[] ringRadii(int width, int height, boolean showTransit, boolean showTri) {
        int outer = Math.min(width, height) / 2 - 10;
        // When the tri-wheel is on, carve out a 22-pixel band for it outside the synastry ring.
        int tri     = outer - 20;  // always outer-20; transit steps 22 px inward when tri-wheel is on
        int transit = showTri    ? tri - 22           : outer - 20;
        int decanOuter = showTransit ? transit - 25 : outer - 20;
        int signOuter = decanOuter - 20;
        int signInner = signOuter - 35;
        return new int[] { outer, tri, transit, decanOuter, signOuter, signInner };
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
        return "aspect|" + rowLabel + "|" + columnBody + "|" + aspectLabel;
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
        return parts.length == 4 ? new String[]{parts[1], parts[2], parts[3]} : null;
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
        boolean transit = parsed[0].startsWith(TRANSIT_PREFIX);
        int a = bodyIndexOfLabel(parsed[0]);
        int b = bodyIndexOfLabel(parsed[1]);
        Aspects.Type type = Aspects.Type.fromLabel(parsed[2]);
        if (a < 0 || b < 0 || type == null) {
            return null;
        }
        double lonA = transit ? this.tLon[a] : this.bLon[a];
        double lonB = this.bLon[b];
        double speedA = transit ? this.tSpeed[a] : this.bSpeed[a];
        double speedB = this.bSpeed[b];

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
        sb.append("<html><body style='width:250px; font-family:SansSerif; font-size:11px;'>");
        sb.append("<div style='font-size:13px;'><b>").append(BODY_NAMES[a]);
        if (transit) {
            sb.append(" <span style='color:#5A7FBF;'>(transiting)</span>");
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

        String lead = aspectSummary(parsed[0], parsed[1], type.label, transit);
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
    private String aspectSummary(String rowLabel, String colBody, String type, boolean transit) {
        try {
            InterpretationService svc = InterpretationService.getInstance();
            // <b>The same fork as the click handler, and it has to stay the same fork.</b> A
            // synastry row carries the transit prefix but is not a transit, so hovering used
            // to summarise a passing event for a standing relationship - while clicking the
            // identical cell now shows the synastry reading. Two surfaces, one rule.
            String body;
            if (transit && this.isSynastryChart()) {
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
        boolean transit = false;
        if (parsed != null) {
            transit = parsed[0].startsWith(TRANSIT_PREFIX);
            a = bodyIndexOfLabel(parsed[0]);
            b = bodyIndexOfLabel(parsed[1]);
            if (a < 0 || b < 0) {
                a = -1;
                b = -1;
            }
        }
        if (a == this.highlightA && b == this.highlightB && transit == this.highlightTransit) {
            return false;
        }
        this.highlightA = a;
        this.highlightB = b;
        this.highlightTransit = transit;
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
    private void drawMansionRing(Graphics2D g, int cx, int cy, int outer, double pin) {
        try {
            java.util.List<com.zodiacomputing.ourania.astro.LunarMansions.Mansion> all =
                com.zodiacomputing.ourania.astro.LunarMansions.all();
            com.zodiacomputing.ourania.astro.ChartFrame frame = this.getCurrentChart();
            com.zodiacomputing.ourania.astro.LunarMansions.Mansion moonMansion =
                frame == null ? null
                    : com.zodiacomputing.ourania.astro.LunarMansions.ofMoon(frame);

            int bandOuter = outer;
            int bandInner = outer - 9;
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
    private boolean isHighlighted(int a, int b, boolean transit) {
        if (isPatternMemberPair(a, b, transit)) {
            return true;
        }
        if (this.highlightA < 0 || transit != this.highlightTransit) {
            return false;
        }
        // Natal lines are drawn once per unordered pair, so match either ordering.
        return transit ? (a == this.highlightA && b == this.highlightB)
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
                if (aspectParts[0].startsWith(TRANSIT_PREFIX)) {
                    // <b>A synastry row is not a transit row.</b> Both carry the prefix,
                    // because both put the outer wheel first, and until 2026-08-24 both went
                    // to the transit reading - so clicking a cell about two people produced
                    // the heading "Transiting Mars Square natal Venus" and a paragraph about
                    // a passing event. The geometry is shared; the meaning is not.
                    String outer = aspectParts[0].substring(TRANSIT_PREFIX.length());
                    if (this.isSynastryChart()) {
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
            double[] dArray = isSky ? this.cLon : (isTransit ? this.tLon : this.bLon);
            blArray = isSky ? this.cValid : (isTransit ? this.tValid : this.bValid);
            double[] speedArray = isSky ? this.cSpeed : (isTransit ? this.tSpeed : this.bSpeed);
            if (n < 0 || n >= BODY_COUNT || !blArray[n]) {
                return;
            }
            if (Bodies.at(n).isAngle()) {
                // isSky carries the prefix through, so a sky_ / tri_ link from the placements
                // panel reads both charts exactly as clicking the glyph on the wheel does.
                this.showAngleAt(n, dArray[n], isSky);
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
                    if (!this.bValid[i]) continue;
                    double d6 = Math.abs(d2 - this.bLon[i]);
                    if (d6 > 180.0) d6 = 360.0 - d6;
                    String string2 = this.getAspectType(d6, n, i, this.isSynastryPair(false));
                    if (string2 == null) continue;
                    boolean blA = this.isAspectApplying(d2, speedArray[n], this.bLon[i], this.bSpeed[i], string2);
                    String string3 = this.describeAspect(d6, string2, n, i, d2, this.bLon[i], blA);
                    arrayList.add(new String[]{BODY_NAMES[i], string2, string3, "Chart A"});
                }
                for (int i = 0; i < BODY_COUNT; ++i) {
                    if (!this.tValid[i]) continue;
                    double d6 = Math.abs(d2 - this.tLon[i]);
                    if (d6 > 180.0) d6 = 360.0 - d6;
                    String string2 = this.getAspectType(d6, n, i, this.isSynastryPair(false));
                    if (string2 == null) continue;
                    boolean blB = this.isAspectApplying(d2, speedArray[n], this.tLon[i], this.tSpeed[i], string2);
                    String string3 = this.describeAspect(d6, string2, n, i, d2, this.tLon[i], blB);
                    arrayList.add(new String[]{BODY_NAMES[i], string2, string3, "Chart B"});
                }
            } else {
                for (int i = 0; i < BODY_COUNT; ++i) {
                    String string2;
                    if (n == i || !SkymapPanel.aspecting(i, this.bValid) || Bodies.isOppositePair(n, i)) continue;
                    double d6 = Math.abs(d2 - this.bLon[i]);
                    if (d6 > 180.0) {
                        d6 = 360.0 - d6;
                    }
                    if ((string2 = this.getAspectType(d6, n, i, this.isSynastryPair(isTransit))) == null) continue;
                    d = speedArray[n];
                    boolean bl2 = this.isAspectApplying(d2, d, this.bLon[i], this.bSpeed[i], string2);
                    String string3 = this.describeAspect(d6, string2, n, i, d2, this.bLon[i], bl2);
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
        double[] dArray = bl ? this.tLon : this.bLon;
        boolean[] blArray2 = blArray = bl ? this.tValid : this.bValid;
        if (n < 0 || n >= BODY_COUNT || !blArray[n]) {
            return arrayList;
        }
        double d2 = dArray[n];
        double d3 = d = bl ? this.tSpeed[n] : this.bSpeed[n];
        if (Bodies.at(n).isAngle()) {
            d = 361.0;
        }
        for (int i = 0; i < BODY_COUNT; ++i) {
            String string;
            if (n == i || !SkymapPanel.aspecting(i, this.bValid) || Bodies.isOppositePair(n, i)) continue;
            double d4 = Math.abs(d2 - this.bLon[i]);
            if (d4 > 180.0) {
                d4 = 360.0 - d4;
            }
            if ((string = this.getAspectType(d4, n, i, this.isSynastryPair(bl))) == null) continue;
            boolean bl2 = this.isAspectApplying(d2, d, this.bLon[i], this.bSpeed[i], string);
            String string2 = this.describeAspect(d4, string, n, i, d2, this.bLon[i], bl2);
            arrayList.add(new String[]{BODY_NAMES[i], string, string2});
        }
        return arrayList;
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
            double d8 = Math.toRadians(180.0 + d - d2);
            double d9 = Math.toRadians(180.0 + d - d3);
            double d10 = (double)n3 + (double)n5 * Math.cos(d8);
            double d11 = (double)n4 + (double)n5 * Math.sin(d8);
            double d12 = (double)n3 + (double)n6 * Math.cos(d9);
            double d13 = Math.max(0.0, Math.min(1.0, (((double)n - d10) * (d12 - d10) + ((double)n2 - d11) * ((d7 = (double)n4 + (double)n6 * Math.sin(d9)) - d11)) / (d6 = Math.pow(d10 - d12, 2.0) + Math.pow(d11 - d7, 2.0))));
            double d14 = d10 + d13 * (d12 - d10);
            double d15 = Math.hypot((double)n - d14, (double)n2 - (d5 = d11 + d13 * (d7 - d11)));
            if (d15 <= 8.0) {
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
        // <b>In a composite mode the outer wheel is the third time, not the second person.</b>
        // Animating "Transit" there must move the sky, not one of the two birth charts - moving
        // a birth time would silently rebuild the composite under the reader.
        if (this.isRelationshipChart()) {
            if (bl) {
                this.compositeTransitTime = "Real Time".equals(this.stepAmount)
                    ? ZonedDateTime.now(ZoneId.of(this.transitTimeZoneId))
                    : this.stepped(this.compositeTransitTime, n);
            }
            return;
        }
        if ("Real Time".equals(this.stepAmount)) {
            if (bl2) {
                this.baseChartTime = ZonedDateTime.now(ZoneId.of(this.baseTimeZoneId));
            }
            if (bl) {
                this.transitChartTime = ZonedDateTime.now(ZoneId.of(this.transitTimeZoneId));
            }
            return;
        }
        switch (this.stepAmount) {
            case "1 Minute": {
                if (bl2) {
                    this.baseChartTime = this.baseChartTime.plusMinutes(n);
                }
                if (!bl) break;
                this.transitChartTime = this.transitChartTime.plusMinutes(n);
                break;
            }
            case "1 Hour": {
                if (bl2) {
                    this.baseChartTime = this.baseChartTime.plusHours(n);
                }
                if (!bl) break;
                this.transitChartTime = this.transitChartTime.plusHours(n);
                break;
            }
            case "1 Day": {
                if (bl2) {
                    this.baseChartTime = this.baseChartTime.plusDays(n);
                }
                if (!bl) break;
                this.transitChartTime = this.transitChartTime.plusDays(n);
                break;
            }
            case "1 Week": {
                if (bl2) {
                    this.baseChartTime = this.baseChartTime.plusWeeks(n);
                }
                if (!bl) break;
                this.transitChartTime = this.transitChartTime.plusWeeks(n);
                break;
            }
            case "1 Month": {
                if (bl2) {
                    this.baseChartTime = this.baseChartTime.plusMonths(n);
                }
                if (!bl) break;
                this.transitChartTime = this.transitChartTime.plusMonths(n);
                break;
            }
            case "1 Year": {
                if (bl2) {
                    this.baseChartTime = this.baseChartTime.plusYears(n);
                }
                if (!bl) break;
                this.transitChartTime = this.transitChartTime.plusYears(n);
            }
        }
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

    private SweDate createSweDate(ZonedDateTime zonedDateTime) {
        ZonedDateTime zonedDateTime2 = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
        return new SweDate(zonedDateTime2.getYear(), zonedDateTime2.getMonthValue(), zonedDateTime2.getDayOfMonth(), (double)zonedDateTime2.getHour() + (double)zonedDateTime2.getMinute() / 60.0 + (double)zonedDateTime2.getSecond() / 3600.0);
    }

    public void updateChartData() {
        if (this.baseChartTime != null) {
            this.baseSd = this.createSweDate(this.baseChartTime);
        }
        if (this.transitChartTime != null) {
            this.transitSd = this.createSweDate(this.transitChartTime);
        }
        if (this.sw == null) {
            return;
        }
        if (this.compositeTransitTime == null) {
            this.compositeTransitTime = ZonedDateTime.now(ZoneId.of(this.transitTimeZoneId));
        }
        this.compositeTransitSd = this.createSweDate(this.compositeTransitTime);

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

        double[] dArray = new double[10];
        if (this.baseSd != null && !relationship) {
            this.sw.swe_houses(this.baseSd.getJulDay(), 2, this.baseLatitude, this.baseLongitude, this.houseSystem, this.baseCusps, dArray);
            this.baseAscendant = dArray[0];
        }
        SweDate outerSd = relationship ? this.compositeTransitSd : this.transitSd;
        if (this.showTransitChart && outerSd != null) {
            double[] dArray2 = new double[10];
            this.sw.swe_houses(outerSd.getJulDay(), 2, this.transitLatitude, this.transitLongitude, this.houseSystem, this.transitCusps, dArray2);
            this.transitAscendant = dArray2[0];
        } else {
            System.arraycopy(this.baseCusps, 0, this.transitCusps, 0, this.transitCusps.length);
            this.transitAscendant = this.baseAscendant;
        }
        if (this.baseSd != null && !relationship) {
            this.computeBodies(this.baseSd, this.baseCusps, this.bLon, this.bSpeed, this.bOk, this.bValid);
        }
        if (this.showTransitChart && outerSd != null) {
            this.computeBodies(outerSd, this.transitCusps, this.tLon, this.tSpeed, this.tOk, this.tValid);
        } else {
            Arrays.fill(this.tValid, false);
            Arrays.fill(this.tOk, false);
        }
        // Tri-wheel: the sky at compositeTransitTime wrapped around the synastry pair.
        // compositeTransitTime defaults to now and is driven by the same Now/Play controls,
        // so the animation sweeps the sky without touching either person's birth data.
        if (this.showTriWheel && this.compositeTransitSd != null) {
            double[] triAux = new double[10];
            this.sw.swe_houses(this.compositeTransitSd.getJulDay(), 2,
                this.transitLatitude, this.transitLongitude, this.houseSystem,
                this.triCusps, triAux);
            this.triAscendant = triAux[0];
            this.computeBodies(this.compositeTransitSd, this.triCusps,
                this.cLon, this.cSpeed, this.cOk, this.cValid);
        } else {
            Arrays.fill(this.cValid, false);
            Arrays.fill(this.cOk, false);
        }
        // The harmonic, applied once, after every other source has finished writing positions.
        //
        // <b>Here rather than at each filler.</b> bLon is written by two different paths - the
        // composite goes through loadFrameIntoBase, everything else through computeBodies - and
        // mapping in both would be one rule in two places. This is the point where the arrays
        // are settled and nothing else has read them yet.
        //
        // The transit wheel is mapped too: a harmonic is a view of the screen, and showing H5
        // bodies against radix transits would be two charts in one wheel.
        if (com.zodiacomputing.ourania.astro.Harmonics.isActive(this.harmonic)) {
            for (int i = 0; i < BODY_COUNT; i++) {
                // <b>Angles are skipped, matching Harmonics.of.</b> The house cusps and the
                // ASC/DSC axis are drawn from baseCusps and baseAscendant, which are never
                // mapped - so mapping the angle GLYPHS here put a harmonic Ascendant marker
                // on a radix horizon. At H5 it had walked to the top of the wheel while the
                // axis line stayed on the left. Caught by rendering the wheel, not by any
                // check; HarmonicCheck Part D now asserts it.
                if (Bodies.at(i).isAngle()) {
                    continue;
                }
                this.bLon[i] = com.zodiacomputing.ourania.astro.Harmonics.map(
                    this.bLon[i], this.harmonic);
                this.bSpeed[i] *= this.harmonic;
                this.tLon[i] = com.zodiacomputing.ourania.astro.Harmonics.map(
                    this.tLon[i], this.harmonic);
                this.tSpeed[i] *= this.harmonic;
                this.cLon[i] = com.zodiacomputing.ourania.astro.Harmonics.map(
                    this.cLon[i], this.harmonic);
                this.cSpeed[i] *= this.harmonic;
            }
        }

        if (this.showTransitChart && "Transit".equals(this.houseAlignment)) {
            System.arraycopy(this.transitCusps, 0, this.activeCusps, 0, this.activeCusps.length);
            this.activeAscendant = this.transitAscendant;
        } else {
            System.arraycopy(this.baseCusps, 0, this.activeCusps, 0, this.activeCusps.length);
            this.activeAscendant = this.baseAscendant;
        }
        if (this.window != null) {
            // Three parts, three accordion sections. One document would mean the
            // sidebar could only show or hide all of it at once, which is what it did before.
            this.window.updateChartSections(
                this.generatePlanetPlacementsHtml(PlacementPart.NATAL),
                this.generatePlanetPlacementsHtml(PlacementPart.TRANSITS),
                this.generatePlanetPlacementsHtml(PlacementPart.GRID));
        }
        this.refreshReadingIfShown();
    }

    /**
     * Copy a computed frame into the inner-wheel arrays.
     *
     * <b>Index-for-index, and that is safe because both are the Bodies registry.</b>
     * ChartFrame.bodies and this panel's bLon/bValid are each built by walking
     * {@link Bodies} in order, so entry i is the same body in both. The NAME remains the key
     * anywhere something is looked up rather than indexed.
     *
     * {@code ok} already accounts for the user's body selection - ChartFrame.compute applies
     * Settings.loadBodySelection before returning - so bValid follows it rather than being
     * recomputed here from a second reading of the settings.
     */
    private void loadFrameIntoBase(ChartFrame frame) {
        System.arraycopy(frame.cusps, 0, this.baseCusps, 0,
            Math.min(frame.cusps.length, this.baseCusps.length));
        this.baseAscendant = frame.asc;
        for (int i = 0; i < BODY_COUNT && i < frame.bodies.length; i++) {
            ChartFrame.Body b = frame.bodies[i];
            boolean usable = b != null && b.ok;
            this.bLon[i] = usable ? b.lon : 0.0;
            this.bSpeed[i] = usable ? b.lonSpeed : 0.0;
            this.bOk[i] = usable;
            this.bValid[i] = usable;
        }
    }

    private void computeBodies(SweDate sweDate, double[] dArray, double[] dArray2, double[] dArray3, boolean[] blArray, boolean[] blArray2) {
        int n;
        double d = sweDate.getJulDay();
        for (int i = 0; i < BODY_COUNT; ++i) {
            if (Bodies.at((int)i).source != Bodies.Source.EPHEMERIS) continue;
            double[] dArray4 = new double[6];
            StringBuffer stringBuffer = new StringBuffer();
            if (this.sw.swe_calc_ut(d, Bodies.at(i).getIpl(), 258, dArray4, stringBuffer) != -1) {
                dArray2[i] = Zodiac.normalise(dArray4[0]);
                dArray3[i] = dArray4[3];
                blArray[i] = true;
                continue;
            }
            blArray[i] = false;
        }
        double d2 = Zodiac.normalise(dArray[1]);
        double d3 = Zodiac.normalise(dArray[10]);
        for (n = 0; n < BODY_COUNT; ++n) {
            Bodies.Def def = Bodies.at(n);
            if (def.source == Bodies.Source.EPHEMERIS) continue;
            dArray2[n] = Bodies.derive(def.source, d2, d3, dArray2[SUN], dArray2[MOON], dArray2[NORTH_NODE]);
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
            case FORTUNE: {
                return blArray[SUN] && blArray[MOON];
            }
        }
        return true;
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
            return this.transitAscendant;
        }
        return this.baseAscendant;
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
        String string = this.chartMode == ChartMode.SYNASTRY ? "Chart A (Inner)" : (this.chartMode == ChartMode.COMPOSITE_MIDPOINT || this.chartMode == ChartMode.COMPOSITE_DAVISON ? "Composite Chart" : "Natal Chart");
        // Hoisted above the gate: the transit block below formats its own timestamps with
        // this same formatter, so leaving it inside the natal section made it invisible there.
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm z");
        if (wantNatal) {
        stringBuilder.append("<h2 style='color:#ffa500; margin-bottom: 2px;'>").append(string).append("</h2>");
        String string2 = this.baseChartTime != null ? this.baseChartTime.format(dateTimeFormatter) : "";
        String string3 = String.format("%.2f, %.2f", this.baseLatitude, this.baseLongitude);
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
                stringBuilder.append("<div style='margin-bottom:3px;'>")
                    .append("<a href='")
                    .append(InterpretationPanel.patternHref(p.name, p.bodies))
                    .append("' style='color:#FFD166; text-decoration:none;'><b>")
                    .append(p.name).append("</b></a>");
                String quality = p.modality != null
                    && (p.name.equals("T-square") || p.name.equals("Grand cross"))
                        ? p.modality
                        : p.element != null
                            && (p.name.equals("Grand trine") || p.name.equals("Kite"))
                                ? p.element : null;
                if (quality != null) {
                    stringBuilder.append(" <span style='color:#dddddd;'>(").append(quality)
                        .append(")</span>");
                }
                stringBuilder.append("<div style='color:#dddddd; font-size:11px;'>")
                    .append(String.join(", ", p.bodies));
                if (p.apex != null) {
                    stringBuilder.append(" &nbsp;|&nbsp; apex ").append(p.apex);
                }
                stringBuilder.append("</div></div>");
            }
            stringBuilder.append("<div style='color:#9AA5B1; font-size:10px;'>Click a figure "
                + "for the full reading.</div>");
            stringBuilder.append("</div>");
        }

        stringBuilder.append("<h3 style='color:#add8e6;'>Placements</h3>");
        for (n3 = 0; n3 < BODY_COUNT; ++n3) {
            if (!this.bValid[n3]) continue;
            stringBuilder.append(this.formatPlanetPlacement(n3, this.bLon[n3], this.bSpeed[n3], "base_"));
        }
        }
        if (wantTransits && this.showTransitChart) {
            String string4 = this.chartMode == ChartMode.SYNASTRY ? "Chart B (Outer)" : "Transit Chart";
            stringBuilder.append("<br><h2 style='color:#ffa500; margin-bottom: 2px;'>").append(string4).append("</h2>");
            stringArray = this.transitChartTime != null ? this.transitChartTime.format(dateTimeFormatter) : "";
            String stringArray2 = String.format("%.2f, %.2f", this.transitLatitude, this.transitLongitude);
            stringBuilder.append("<div style='color:#dddddd; font-size:11px; margin-bottom: 10px;'>").append(stringArray).append("<br>").append(stringArray2).append("</div>");
            stringBuilder.append("<h3 style='color:#ffa500;'>Placements</h3>");
            for (n2 = 0; n2 < BODY_COUNT; ++n2) {
                if (!this.tValid[n2]) continue;
                stringBuilder.append(this.formatPlanetPlacement(n2, this.tLon[n2], this.tSpeed[n2], "transit_"));
            }
        }
        if (wantTransits && this.showTriWheel) {
            stringBuilder.append("<br><h2 style='color:#a0d2ff; margin-bottom: 2px;'>Sky (Transiting)</h2>");
            String stringSkyTime = this.compositeTransitTime != null ? this.compositeTransitTime.format(dateTimeFormatter) : "";
            String stringSkyLoc = String.format("%.2f, %.2f", this.transitLatitude, this.transitLongitude);
            stringBuilder.append("<div style='color:#dddddd; font-size:11px; margin-bottom: 10px;'>").append(stringSkyTime).append("<br>").append(stringSkyLoc).append("</div>");
            stringBuilder.append("<h3 style='color:#a0d2ff;'>Placements</h3>");
            for (int k = 0; k < BODY_COUNT; ++k) {
                if (!this.cValid[k]) continue;
                stringBuilder.append(this.formatPlanetPlacement(k, this.cLon[k], this.cSpeed[k], "sky_"));
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
            stringBuilder.append("<h3 style='color:white; margin-bottom: 4px;'>Natal Aspects Grid</h3>");
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
            if (SkymapPanel.aspecting(n, this.bValid)) {
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
            if (!SkymapPanel.aspecting(n, this.bValid)) continue;
            stringBuilder.append("<td style='color:").append(this.bodyColorHex(n)).append("; font-size:").append(glyph).append("px; width:").append(cell).append("px;'>").append(BODY_GLYPHS[n]).append("</td>");
        }
        stringBuilder.append("</tr>");
        for (n = 0; n < BODY_COUNT; ++n) {
            n2 = (n3 != 0 ? SkymapPanel.aspecting(n, this.tValid) : SkymapPanel.aspecting(n, this.bValid)) ? 1 : 0;
            if (n2 == 0) continue;
            stringBuilder.append("<tr>");
            stringBuilder.append("<td style='color:").append(this.bodyColorHex(n)).append("; font-size:").append(glyph).append("px; width:").append(cell).append("px;'>").append(BODY_GLYPHS[n]).append("</td>");
            for (int i = 0; i < BODY_COUNT; ++i) {
                String string6;
                double d;
                if (!SkymapPanel.aspecting(i, this.bValid)) continue;
                if (n3 == 0 && i >= n) {
                    stringBuilder.append("<td style='background-color:#111;'></td>");
                    continue;
                }
                if (Bodies.isOppositePair(n, i)) {
                    stringBuilder.append("<td style='background-color:#111;'></td>");
                    continue;
                }
                double d2 = n3 != 0 ? this.tLon[n] : this.bLon[n];
                double d3 = Math.abs(d2 - (d = this.bLon[i]));
                if (d3 > 180.0) {
                    d3 = 360.0 - d3;
                }
                if ((string6 = this.getAspectType(d3, n, i, this.isSynastryPair(n3 != 0))) != null) {
                    String string7 = n3 != 0 ? "transit_" + BODY_NAMES[n].toLowerCase() : BODY_NAMES[n];
                    String string8 = BODY_NAMES[i];
                    // Through aspectHref, never formatted inline: the parser is the only other
                    // place that knows this format and the two must not be able to drift.
                    String string9 = SkymapPanel.aspectHref(string7, string8, string6);
                    stringBuilder.append("<td style='background-color:#222;'><a href='").append(string9).append("' style='text-decoration:none;'>").append("<span style='color:").append(this.getAspectColorHex(string6)).append("; font-size:").append(glyph).append("px;'>").append(this.getAspectSymbol(string6)).append("</span></a></td>");
                    continue;
                }
                stringBuilder.append("<td style='background-color:#222;'></td>");
            }
            stringBuilder.append("</tr>");
        }
        stringBuilder.append("</table>");
        stringBuilder.append("</body></html>");
        return stringBuilder.toString();
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
            return SkymapPanel.this.hoverTextAt(event.getX(), event.getY());
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
            if (SkymapPanel.this.sw == null || SkymapPanel.this.baseSd == null) {
                return;
            }
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int n10 = this.getWidth();
            int n11 = this.getHeight();
            int n12 = n10 / 2;
            int n13 = n11 / 2;
            int[] rings = SkymapPanel.ringRadii(n10, n11, SkymapPanel.this.showTransitChart,
                SkymapPanel.this.showTriWheel);
            int n14 = n9 = rings[RING_OUTER];
            int nTriOuter = rings[RING_TRI];
            int n15 = rings[RING_TRANSIT];
            int n16 = rings[RING_DECAN_OUTER];
            int n17 = rings[RING_SIGN_OUTER];
            int n18 = rings[RING_SIGN_INNER];
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
            graphics2D.drawOval(n12 - n18, n13 - n18, n18 * 2, n18 * 2);
            graphics2D.drawOval(n12 - n17, n13 - n17, n17 * 2, n17 * 2);
            graphics2D.drawOval(n12 - n16, n13 - n16, n16 * 2, n16 * 2);
            if (SkymapPanel.this.showTransitChart) {
                graphics2D.setColor(new Color(80, 40, 80));
                graphics2D.drawOval(n12 - n15, n13 - n15, n15 * 2, n15 * 2);
            }
            // Tri-wheel: an additional ring outside the synastry ring.
            if (SkymapPanel.this.showTriWheel) {
                graphics2D.setColor(new Color(30, 60, 80));
                graphics2D.drawOval(n12 - nTriOuter, n13 - nTriOuter, nTriOuter * 2, nTriOuter * 2);
            }
            graphics2D.setFont(new Font("SansSerif", 0, 12));
            for (n8 = 0; n8 < 36; ++n8) {
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
            // The 28 lunar mansions, in the outermost band alongside the degree ticks.
            //
            // Placed here rather than as a band of its own because every other ring's radius
            // is load-bearing: the glyph rings, the hit test and natalRadii all derive from
            // the same chain, so carving out a new band would move the bodies. This band holds
            // only tick marks, so the mansions can share it without anything else shifting.
            SkymapPanel.this.drawMansionRing(graphics2D, n12, n13, n14, d4);

            graphics2D.setFont(new Font("SansSerif", 0, 22));
            for (n8 = 0; n8 < 12; ++n8) {
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
            for (n8 = 0; n8 < 360; ++n8) {
                d3 = Math.toRadians(180.0 + d4 - (double)n8);
                int n21 = n14;
                n = n8 % 10 == 0 ? n21 - 6 : (n8 % 5 == 0 ? n21 - 4 : n21 - 2);
                n7 = n12 + (int)((double)n * Math.cos(d3));
                n6 = n13 + (int)((double)n * Math.sin(d3));
                n5 = n12 + (int)((double)n21 * Math.cos(d3));
                n4 = n13 + (int)((double)n21 * Math.sin(d3));
                graphics2D.setStroke(new BasicStroke(n8 % 10 == 0 ? 1.5f : 0.5f));
                graphics2D.drawLine(n7, n6, n5, n4);
            }
            for (n8 = 1; n8 <= 12; ++n8) {
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
                int n22 = n12 + (int)((double)(n18 - 15) * Math.cos(d8));
                int n23 = n13 + (int)((double)(n18 - 15) * Math.sin(d8));
                graphics2D.setColor(SkymapPanel.this.getElementColor(Zodiac.elementIndex(n8 - 1)));
                graphics2D.setFont(new Font("Arial", 1, 11));
                graphics2D.drawString(String.valueOf(n8), n22 - 3, n23 + 3);
            }
            if (SkymapPanel.this.showTransitChart && "Both".equals(SkymapPanel.this.houseAlignment)) {
                Stroke stroke = graphics2D.getStroke();
                graphics2D.setStroke(new BasicStroke(2.0f, 0, 0, 10.0f, new float[]{4.0f, 4.0f}, 0.0f));
                for (int i = 1; i <= 12; ++i) {
                    double d9 = Math.toRadians(180.0 + d4 - SkymapPanel.this.transitCusps[i]);
                    n = n12 + (int)((double)n16 * Math.cos(d9));
                    n7 = n13 + (int)((double)n16 * Math.sin(d9));
                    n6 = n12 + (int)((double)n15 * Math.cos(d9));
                    n5 = n13 + (int)((double)n15 * Math.sin(d9));
                    n4 = i == 1 || i == 4 || i == 7 || i == 10 ? 1 : 0;
                    graphics2D.setColor(n4 != 0 ? new Color(210, 150, 230) : new Color(120, 80, 140));
                    graphics2D.drawLine(n, n7, n6, n5);
                }
                graphics2D.setStroke(stroke);
            }
            // <b>The painter must ask bodyBaseRadius too, and it never did.</b> n18 is
            // RING_SIGN_INNER, the default placement, so the glyphs were drawn there whatever
            // the reader chose while bodyAt tested natalRadii(bodyBaseRadius(rings)) - the
            // setting moved the clicks and not the glyphs. At 820px: painter 325 for all three
            // settings, hit test 325 inside the sign ring, 201 in the centre, 374 outside. So
            // bodies were selectable on the default alone, which is exactly how it was
            // reported. bodyBaseRadius's own header names this failure and says both sides
            // must call it; only one did.
            //
            // n18 keeps RING_SIGN_INNER for everything else here - the sign circle it strokes
            // and the spoke endpoints - because those genuinely are the sign ring.
            int[] nArray = SkymapPanel.this.natalRadii(
                SkymapPanel.this.bodyBaseRadius(rings));
            int[] nArray2 = SkymapPanel.this.transitRadii(n15, n16);
            int[] nArrayC = SkymapPanel.this.triWheelRadii(nTriOuter, n15);
            int n24 = n18 - 60;
            graphics2D.setStroke(new BasicStroke(0.5f));
            boolean bl = SkymapPanel.this.aspectFilter.equals("Natal-Natal") || SkymapPanel.this.aspectFilter.equals("Both");
            int n25 = n = (SkymapPanel.this.aspectFilter.equals("Transit-Natal") || SkymapPanel.this.aspectFilter.equals("Both")) && SkymapPanel.this.showTransitChart ? 1 : 0;
            if (bl) {
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.aspecting(n7, SkymapPanel.this.bValid)) continue;
                    for (n6 = n7 + 1; n6 < BODY_COUNT; ++n6) {
                        if (!SkymapPanel.aspecting(n6, SkymapPanel.this.bValid) || Bodies.isOppositePair(n7, n6)) continue;
                        this.drawAspectLine(graphics2D, SkymapPanel.this.bLon[n7], SkymapPanel.this.bLon[n6], d4, n12, n13, nArray[n7], nArray[n6], false, n7, n6);
                    }
                }
            }
            if (n != 0) {
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.aspecting(n7, SkymapPanel.this.tValid)) continue;
                    for (n6 = 0; n6 < BODY_COUNT; ++n6) {
                        if (!SkymapPanel.aspecting(n6, SkymapPanel.this.bValid)) continue;
                        this.drawAspectLine(graphics2D, SkymapPanel.this.tLon[n7], SkymapPanel.this.bLon[n6], d4, n12, n13, nArray2[n7], nArray[n6], true, n7, n6);
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
                        if (!SkymapPanel.aspecting(pa, SkymapPanel.this.bValid)
                            || !SkymapPanel.aspecting(pb, SkymapPanel.this.bValid)) {
                            continue;
                        }
                        this.drawAspectLine(graphics2D, SkymapPanel.this.bLon[pa],
                            SkymapPanel.this.bLon[pb], d4, n12, n13, nArray[pa], nArray[pb],
                            false, pa, pb);
                    }
                }
            }

            int hlA = SkymapPanel.this.highlightA;
            int hlB = SkymapPanel.this.highlightB;
            if (hlA >= 0 && hlB >= 0) {
                if (SkymapPanel.this.highlightTransit) {
                    if (SkymapPanel.aspecting(hlA, SkymapPanel.this.tValid)
                        && SkymapPanel.aspecting(hlB, SkymapPanel.this.bValid)) {
                        this.drawAspectLine(graphics2D, SkymapPanel.this.tLon[hlA], SkymapPanel.this.bLon[hlB],
                            d4, n12, n13, nArray2[hlA], nArray[hlB], true, hlA, hlB);
                    }
                } else if (SkymapPanel.aspecting(hlA, SkymapPanel.this.bValid)
                    && SkymapPanel.aspecting(hlB, SkymapPanel.this.bValid)) {
                    this.drawAspectLine(graphics2D, SkymapPanel.this.bLon[hlA], SkymapPanel.this.bLon[hlB],
                        d4, n12, n13, nArray[hlA], nArray[hlB], false, hlA, hlB);
                }
            }
            for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                if (!SkymapPanel.this.bValid[n7]) continue;
                double d10 = Math.toRadians(180.0 + d4 - SkymapPanel.this.bLon[n7]);
                n4 = n12 + (int)((double)nArray[n7] * Math.cos(d10));
                int n26 = n13 + (int)((double)nArray[n7] * Math.sin(d10));
                if (Bodies.at(n7).isAngle()) {
                    graphics2D.setFont(ANGLE_FONT);
                    // The angle marker follows the template: gold is invisible on a white
                    // ground and is the only warm thing in a cool palette.
                    Color angle = ChartPalette.colorOr(ChartPalette.angleHex("#D4AF37"),
                        new Color(212, 175, 55));
                    this.drawMetallicSphere(graphics2D, n4, n26, 13, angle);
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
                this.drawMetallicSphere(graphics2D, n4, n26, glyphSize.radius, new Color(192, 192, 192));
                if (n7 == MOON && SkymapPanel.this.bValid[SUN]) {
                    double d11 = (SkymapPanel.this.bLon[MOON] - SkymapPanel.this.bLon[SUN]) % 360.0;
                    if (d11 < 0.0) {
                        d11 += 360.0;
                    }
                    SkymapPanel.this.drawMoonPhase(graphics2D, n4, n26, Math.round((float)glyphSize.radius * 0.47f), d11 / 360.0);
                } else {
                    graphics2D.setColor(SkymapPanel.this.bodyColor(n7));
                    object = SkymapPanel.glyphFor(n7, glyphSize.font);
                    graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n26 + glyphSize.baseline);
                }
                // The leader from the glyph to the degree it actually occupies. Bodies are
                // spread outward when they crowd, so without this a reader cannot tell which
                // degree a glyph belongs to. Colour and visibility are both settings; the
                // alpha stays here because a solid leader would compete with the aspect lines.
                graphics2D.setComposite(priorComposite);
                if (Settings.showDegreeLines()) {
                    Color leader = ChartPalette.colorOr(ChartPalette.leaderHex(null),
                        Color.WHITE);
                    graphics2D.setColor(new Color(leader.getRed(), leader.getGreen(),
                        leader.getBlue(), 60));
                    graphics2D.drawLine(n4, n26, n12 + (int)((double)n18 * Math.cos(d10)), n13 + (int)((double)n18 * Math.sin(d10)));
                }
            }
            if (SkymapPanel.this.showTransitChart) {
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.this.tValid[n7]) continue;
                    double d12 = Math.toRadians(180.0 + d4 - SkymapPanel.this.tLon[n7]);
                    n4 = n12 + (int)((double)nArray2[n7] * Math.cos(d12));
                    int n27 = n13 + (int)((double)nArray2[n7] * Math.sin(d12));
                    if (Bodies.at(n7).isAngle()) {
                        graphics2D.setFont(ANGLE_FONT);
                        this.drawMetallicCube(graphics2D, n4, n27, 13, new Color(94, 82, 46));
                        graphics2D.setColor(new Color(255, 228, 160));
                        object = Bodies.at((int)n7).glyph;
                        graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n27 + 4);
                        continue;
                    }
                    GlyphSize glyphSize2 = SkymapPanel.transitSize(n7);
                    graphics2D.setFont(glyphSize2.font);
                    object = SkymapPanel.lighten(SkymapPanel.this.bodyColor(n7), 0.45);
                    this.drawMetallicCube(graphics2D, n4, n27, glyphSize2.radius, new Color(62, 66, 76));
                    if (n7 == MOON && SkymapPanel.this.tValid[SUN]) {
                        double d13 = (SkymapPanel.this.tLon[MOON] - SkymapPanel.this.tLon[SUN]) % 360.0;
                        if (d13 < 0.0) {
                            d13 += 360.0;
                        }
                        SkymapPanel.this.drawMoonPhase(graphics2D, n4, n27, Math.round((float)glyphSize2.radius * 0.44f), d13 / 360.0);
                        continue;
                    }
                    graphics2D.setColor((Color)object);
                    String string = SkymapPanel.glyphFor(n7, glyphSize2.font);
                    graphics2D.drawString(string, n4 - graphics2D.getFontMetrics().stringWidth(string) / 2, n27 + glyphSize2.baseline);
                }
            }
            // Tri-wheel: sky positions in the outermost ring, distinguished by a blue-tinted
            // cube to tell them apart from the synastry ring's golden cubes.
            if (SkymapPanel.this.showTriWheel) {
                for (n7 = 0; n7 < BODY_COUNT; ++n7) {
                    if (!SkymapPanel.this.cValid[n7]) continue;
                    double d14 = Math.toRadians(180.0 + d4 - SkymapPanel.this.cLon[n7]);
                    n4 = n12 + (int)((double)nArrayC[n7] * Math.cos(d14));
                    int n28 = n13 + (int)((double)nArrayC[n7] * Math.sin(d14));
                    if (Bodies.at(n7).isAngle()) {
                        graphics2D.setFont(ANGLE_FONT);
                        this.drawMetallicCube(graphics2D, n4, n28, 13, new Color(20, 60, 90));
                        graphics2D.setColor(new Color(160, 210, 255));
                        object = Bodies.at((int)n7).glyph;
                        graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n28 + 4);
                        continue;
                    }
                    GlyphSize glyphSize3 = SkymapPanel.transitSize(n7);
                    graphics2D.setFont(glyphSize3.font);
                    Color cColor = SkymapPanel.lighten(SkymapPanel.this.bodyColor(n7), 0.6);
                    this.drawMetallicCube(graphics2D, n4, n28, glyphSize3.radius, new Color(20, 50, 80));
                    if (n7 == MOON && SkymapPanel.this.cValid[SUN]) {
                        double d15 = (SkymapPanel.this.cLon[MOON] - SkymapPanel.this.cLon[SUN]) % 360.0;
                        if (d15 < 0.0) d15 += 360.0;
                        SkymapPanel.this.drawMoonPhase(graphics2D, n4, n28, Math.round((float)glyphSize3.radius * 0.44f), d15 / 360.0);
                        continue;
                    }
                    graphics2D.setColor(cColor);
                    String stringC = SkymapPanel.glyphFor(n7, glyphSize3.font);
                    graphics2D.drawString(stringC, n4 - graphics2D.getFontMetrics().stringWidth(stringC) / 2, n28 + glyphSize3.baseline);
                }
            }
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
         * The bead a glyph sits on.
         *
         * <b>Optional, for a traditional chart.</b> The metallic sphere is a modern rendering
         * convention; a classical wheel puts the bare glyph on the ring and nothing behind it.
         * Switched off, this draws nothing at all rather than a flat disc - a disc would still
         * cover the ring lines and degree ticks the traditional look is meant to show.
         */
        private void drawMetallicSphere(Graphics2D graphics2D, int n, int n2, int n3, Color color) {
            if (!Settings.showPlanetSpheres()) {
                return;
            }
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

        private void drawAspectLine(Graphics2D graphics2D, double d, double d2, double d3, int n, int n2, int n3, int n4, boolean bl, int n5, int n6) {
            double d4 = Math.abs(d - d2);
            if (d4 > 180.0) {
                d4 = 360.0 - d4;
            }
            // bl is this method's existing "the first body is on the outer wheel" flag - the
            // callers pass tLon/bLon with true and bLon/bLon with false - so the line is drawn
            // on exactly the width the grid and the hit test judge the same pair by.
            boolean syn = SkymapPanel.this.isSynastryPair(bl);
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
                boolean highlighted = SkymapPanel.this.isHighlighted(n5, n6, bl);
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
