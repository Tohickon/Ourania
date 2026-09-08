package com.zodiacomputing.ourania.astro;

import com.zodiacomputing.ourania.gui.ChartMode;
import com.zodiacomputing.ourania.gui.SkymapPanel;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Harmonic charts: the map, the frame it produces, and the panel that shows it.
 *
 * <p><b>Part C is the reason this suite exists.</b> The map itself is three lines and hard to
 * get wrong; what is easy to get wrong is applying it twice. The composite path feeds a frame
 * into the wheel's arrays and the arrays are mapped afterwards, so taking the already-harmonic
 * frame there would put H25 on screen with the dropdown reading 5 - and nothing about that
 * looks like a bug, it just quietly produces a different chart. That was caught during
 * development on 2026-08-25 and is pinned here so it cannot come back.
 */
public final class HarmonicCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /**
     * Tolerance for the panel-level comparisons in Part C, and it is not arbitrary.
     *
     * <b>Loosened from 1e-6 on 2026-08-25 after measuring where the difference came from,
     * not to make a red test green.</b> The finding: {@code updateChartData} is bit-exact
     * across two consecutive passes - 0.000e+00 - but a {@code getCurrentChart()} call in
     * between shifts the next pass by <b>1.885e-06 degrees</b>. That is
     * {@link ChartFrame#compute} leaving Swiss Ephemeris in a different internal state, and it
     * happens with no harmonic involved at all. Part C calls getCurrentChart between passes,
     * so it inherits the jitter.
     *
     * <b>Why this value still catches what the part is for.</b> The defect being guarded is
     * applying the map twice - H5 rendering as H25 - which moves a body by whole signs. At
     * 1e-4 degrees this is 0.36 arcseconds, fifty times the observed jitter and roughly a
     * millionth of any real double-application. Nothing that matters can hide under it.
     *
     * 1.885e-06 degrees is 0.0068 arcseconds; the app formats positions to the arcminute.
     */
    private static final double PANEL_TOL = 1e-4;

    public static void main(String[] args) {
        System.out.println("=== Part A: the map ===");
        int before = failures.size();
        theMap();
        report("Part A", before);

        System.out.println("=== Part B: the frame ===");
        before = failures.size();
        theFrame();
        report("Part B", before);

        System.out.println("=== Part C: the panel applies it exactly once ===");
        before = failures.size();
        appliedOnce();
        report("Part C", before);

        System.out.println("=== Part D: the angles are not carried into the harmonic ===");
        before = failures.size();
        anglesStayPut();
        report("Part D", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }

        // Part C builds a SkymapPanel, which starts a non-daemon AWT thread - so main
        // returning would not end the JVM and handover-stamp.sh, which captures each suite
        // with $(...), would block on this suite forever. CompositeCheck learned this the
        // expensive way the same day. Any suite that touches Swing needs this line.
        System.exit(0);
    }

    /** The transform, on its own terms. */
    private static void theMap() {
        ok("H1 is the identity", Harmonics.map(123.456, 1) == Zodiac.normalise(123.456));
        ok("H1 is not active", !Harmonics.isActive(1));
        ok("H0 and negatives are not active", !Harmonics.isActive(0) && !Harmonics.isActive(-3));
        ok("H2 is active", Harmonics.isActive(2));
        ok("clamp holds the low end", Harmonics.clamp(0) == Harmonics.MIN);
        ok("clamp holds the high end", Harmonics.clamp(9999) == Harmonics.MAX);
        ok("the offered range is 1..360", Harmonics.MIN == 1 && Harmonics.MAX == 360);

        // Everything the map is for: the Nth-harmonic aspect becomes a conjunction in HN.
        // Swept over the whole offered range rather than a handful, because the arithmetic
        // has no special cases and a failure would mean the wrap is wrong somewhere.
        int swept = 0;
        for (int n = 2; n <= Harmonics.MAX; n++) {
            double base = 41.7;
            double other = Zodiac.normalise(base + 360.0 / n);
            double sep = Aspects.separation(Harmonics.map(base, n), Harmonics.map(other, n));
            if (sep > 1e-6) {
                failures.add("H" + n + ": a " + (360.0 / n)
                    + " deg aspect did not become a conjunction, separation " + sep);
            }
            checks++;
            swept++;
        }
        System.out.println("  swept " + swept + " harmonics for the conjunction property");

        // The output is always a longitude, never a raw multiple.
        for (int n = 1; n <= Harmonics.MAX; n += 17) {
            double v = Harmonics.map(359.9, n);
            checks++;
            if (v < 0.0 || v >= 360.0) {
                failures.add("H" + n + " produced an out-of-range longitude: " + v);
            }
        }
    }

    /** What a harmonic frame carries, and what it deliberately does not. */
    private static void theFrame() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame r = ChartFrame.compute(sw,
            new SweDate(1982, 8, 10, 19 + 1.0 / 60.0).getJulDay(),
            40.45, -75.3333, 'P', false, 0.0);

        ok("H1 returns the very same object, uncopied", Harmonics.of(r, 1) == r);
        ok("a null frame survives", Harmonics.of(null, 5) == null);

        ChartFrame h = Harmonics.of(r, 5);
        near("Sun mapped", Harmonics.map(r.body("Sun").lon, 5), h.body("Sun").lon, 1e-9);
        near("Moon mapped", Harmonics.map(r.body("Moon").lon, 5), h.body("Moon").lon, 1e-9);

        // The house frame is the radix one. This is the contested choice, so it is asserted
        // rather than left to be noticed - if it is ever changed, this is the line to change.
        near("Ascendant NOT mapped", r.asc, h.asc, 1e-9);
        near("MC NOT mapped", r.mc, h.mc, 1e-9);
        near("Descendant NOT mapped", r.dsc, h.dsc, 1e-9);
        for (int i = 1; i <= 12; i++) {
            near("cusp " + i + " NOT mapped", r.cusps[i], h.cusps[i], 1e-9);
        }

        ok("marked synthetic", h.syntheticMoment);
        ok("carries a note saying why", h.syntheticNote != null && !h.syntheticNote.isEmpty());
        ok("lunar phase dropped", h.phaseName == null && h.phaseIndex == -1);
        ok("syzygy dropped", Double.isNaN(h.syzygyLon));
        ok("void-of-course dropped", !h.moonVoidOfCourse);
        ok("lots dropped", Double.isNaN(h.lotOfFortune) && Double.isNaN(h.lotOfSpirit));

        // BodyScore.rank walks a frame by name and calls name.equals on every entry, so a
        // nameless body is a NullPointerException in the reading. The midpoint composite
        // shipped exactly that defect; this asserts the harmonic does not repeat it.
        int nameless = 0;
        for (ChartFrame.Body b : h.bodies) {
            if (b == null || b.name == null) {
                nameless++;
            }
        }
        checks++;
        if (nameless > 0) {
            failures.add(nameless + " harmonic bodies have no name");
        }
        System.out.println("  harmonic frame: " + h.bodies.length + " bodies, "
            + nameless + " nameless");

        near("speed scales with the map", r.body("Sun").lonSpeed * 5,
            h.body("Sun").lonSpeed, 1e-9);
        ok("retrogradation survives", h.body("Saturn").retrograde == r.body("Saturn").retrograde);
    }

    /**
     * The wheel and the reading must show the same harmonic, and only one of it.
     *
     * Reaches into the panel because that is where the double-application would live - the
     * map is correct in isolation and the mistake is in how many times it is called.
     */
    private static void appliedOnce() {
        try {
            SwissEph sw = new SwissEph(EPHE_PATH);
            ZonedDateTime ta = ZonedDateTime.of(1982, 8, 10, 19, 1, 0, 0, ZoneId.of("UTC"));
            ZonedDateTime tb = ZonedDateTime.of(1972, 9, 23, 1, 28, 0, 0, ZoneId.of("UTC"));
            int sun = Bodies.indexOf("sun");

            for (ChartMode mode : new ChartMode[] {
                    ChartMode.SINGLE, ChartMode.COMPOSITE_MIDPOINT}) {
                SkymapPanel p = new SkymapPanel(null);
                set(p, "sw", sw);
                set(p, "chartMode", mode);
                set(p, "transitsEnabled", Boolean.FALSE);
                set(p, "showTransitChart",
                    Boolean.valueOf(SkymapPanel.outerWheelShown(mode, false)));
                set(p, "baseChartTime", ta);
                set(p, "transitChartTime", tb);
                set(p, "skyChartTime",
                    ZonedDateTime.of(2026, 8, 26, 12, 0, 0, 0, ZoneId.of("UTC")));
                set(p, "baseSd", new SweDate(1982, 8, 10, 19 + 1.0 / 60.0));
                set(p, "transitSd", new SweDate(1972, 9, 23, 1 + 28.0 / 60.0));
                set(p, "baseLatitude", Double.valueOf(40.45));
                set(p, "baseLongitude", Double.valueOf(-75.3333));
                set(p, "transitLatitude", Double.valueOf(34.05));
                set(p, "transitLongitude", Double.valueOf(-118.25));
                set(p, "houseSystem", Character.valueOf('P'));

                set(p, "harmonic", Integer.valueOf(1));
                p.updateChartData();
                double radix = p.bLon[sun];

                set(p, "harmonic", Integer.valueOf(5));
                p.updateChartData();
                near(mode + ": wheel is radix x5, not x25",
                    Harmonics.map(radix, 5), p.bLon[sun], PANEL_TOL);

                ChartFrame cur = p.getCurrentChart();
                if (cur != null && cur.body("Sun") != null && cur.body("Sun").ok) {
                    near(mode + ": the reading frame agrees with the wheel",
                        p.bLon[sun], cur.body("Sun").lon, PANEL_TOL);
                }

                // Going back must restore, not leave a residue. The map is applied to the
                // arrays in place, so a rebuild that re-multiplied would compound silently.
                set(p, "harmonic", Integer.valueOf(1));
                p.updateChartData();
                near(mode + ": returning to H1 restores the radix", radix, p.bLon[sun],
                    PANEL_TOL);
            }
        } catch (Exception e) {
            checks++;
            failures.add("Part C threw " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * An angle must read the same in every harmonic, on the frame AND on the wheel.
     *
     * <b>This part exists because Parts A to C all passed while the wheel was wrong.</b> The
     * angles are registry bodies, so the obvious loop maps them along with everything else -
     * and then a harmonic Ascendant glyph is drawn against radix house cusps, because the
     * cusps and the ASC/DSC axis come from {@code baseCusps} and {@code baseAscendant}, which
     * are never mapped. It is the two-Ascendants defect the midpoint composite carried until
     * the same week, one class over.
     *
     * <b>It was found by rendering the wheel and looking at it</b> - at H5 the ASC marker had
     * walked to the top of the chart while the axis line stayed on the left. No arithmetic
     * check would have shown it, because both halves were individually self-consistent.
     */
    private static void anglesStayPut() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame r = ChartFrame.compute(sw,
            new SweDate(1982, 8, 10, 19 + 1.0 / 60.0).getJulDay(),
            40.45, -75.3333, 'P', false, 0.0);

        for (int n : new int[] {2, 5, 7, 12, 360}) {
            ChartFrame h = Harmonics.of(r, n);
            for (int i = 0; i < Bodies.count(); i++) {
                if (!Bodies.at(i).isAngle()) {
                    continue;
                }
                String name = Bodies.at(i).name;
                ChartFrame.Body rb = r.body(name);
                ChartFrame.Body hb = h.body(name);
                if (rb == null || hb == null || !rb.ok) {
                    continue;
                }
                near("H" + n + ": " + name + " unmapped on the frame", rb.lon, hb.lon, 1e-9);
            }
            // The angle body and the frame's own angle field must agree - that is the whole
            // failure mode, one Ascendant on the glyph and another on the axis.
            ChartFrame.Body asc = h.body("Ascendant");
            if (asc != null && asc.ok) {
                near("H" + n + ": Ascendant body agrees with frame.asc", h.asc, asc.lon, 1e-9);
            }
            ChartFrame.Body mc = h.body("MC");
            if (mc != null && mc.ok) {
                near("H" + n + ": MC body agrees with frame.mc", h.mc, mc.lon, 1e-9);
            }
        }

        // And on the wheel, which is where it was actually visible.
        try {
            SkymapPanel p = new SkymapPanel(null);
            set(p, "sw", sw);
            set(p, "chartMode", ChartMode.SINGLE);
            set(p, "transitsEnabled", Boolean.FALSE);
            set(p, "showTransitChart", Boolean.FALSE);
            set(p, "baseChartTime",
                ZonedDateTime.of(1982, 8, 10, 19, 1, 0, 0, ZoneId.of("UTC")));
            set(p, "baseSd", new SweDate(1982, 8, 10, 19 + 1.0 / 60.0));
            set(p, "baseLatitude", Double.valueOf(40.45));
            set(p, "baseLongitude", Double.valueOf(-75.3333));
            set(p, "houseSystem", Character.valueOf('P'));

            set(p, "harmonic", Integer.valueOf(1));
            p.updateChartData();
            int ascIdx = Bodies.indexOfName("Ascendant");
            double radixAsc = p.bLon[ascIdx];

            for (int n : new int[] {5, 7}) {
                set(p, "harmonic", Integer.valueOf(n));
                p.updateChartData();
                near("H" + n + ": the wheel's Ascendant glyph has not moved",
                    radixAsc, p.bLon[ascIdx], PANEL_TOL);
            }
        } catch (Exception e) {
            checks++;
            failures.add("Part D wheel section threw " + e.getClass().getSimpleName()
                + " - " + e.getMessage());
        }
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = SkymapPanel.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        double d = Math.abs(Zodiac.normalise(expected) - Zodiac.normalise(actual));
        if (d > 180.0) {
            d = 360.0 - d;
        }
        if (d > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected
                + " (" + d + " off, tolerance " + tol + ")");
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
