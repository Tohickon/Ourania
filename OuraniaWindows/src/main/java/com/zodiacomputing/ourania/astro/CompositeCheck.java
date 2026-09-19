package com.zodiacomputing.ourania.astro;

import com.zodiacomputing.ourania.gui.ChartMode;
import com.zodiacomputing.ourania.gui.Settings;
import com.zodiacomputing.ourania.gui.SkymapPanel;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies the composite chart features: Midpoint Composite and Davison Relationship Chart.
 */
public final class CompositeCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    // Los Angeles. PDT in July is UTC-7.
    private static final double LAT = 34.05;
    private static final double LON = -118.24;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        // Never the reader's own settings file: a suite that generates a chart persists it,
        // and one of these once overwrote a saved birth chart. See Settings.useScratchFile.
        Settings.useScratchFile();
        System.out.println("build: " + CompositeCheck.class.getProtectionDomain().getCodeSource().getLocation());
        System.out.println("=== Part A: Composite Checks ===");
        int before = failures.size();
        liveCharts();
        report("Part A", before);

        System.out.println("=== Part B: A composite is a usable frame ===");
        before = failures.size();
        usableFrame();
        report("Part B", before);

        System.out.println("=== Part C: the panel keeps serving the composite ===");
        before = failures.size();
        panelKeepsComposite();
        report("Part C", before);

        System.out.println("=== Part D: transits are a choice, not a mode ===");
        before = failures.size();
        transitsAreAChoice();
        report("Part D", before);

        System.out.println("=== Part E: the reference pair, pinned ===");
        before = failures.size();
        referencePair();
        report("Part E", before);

        System.out.println("=== Part F: equal-house fallback in computeMidpointComposite ===");
        before = failures.size();
        equalHouseFallback();
        report("Part F", before);

        System.out.println("=== Part G: the geographic midpoint is spherical, not a grid average ===");
        before = failures.size();
        geographicMidpoint();
        report("Part G", before);

        System.out.println("=== Part H: the composite reference place is settable and used ===");
        before = failures.size();
        referencePlaceIsSettable();
        report("Part H", before);

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

        // <b>Required, and only since Part C.</b> That part builds a SkymapPanel, which
        // initialises AWT and starts a non-daemon event dispatch thread - so returning from
        // main no longer ends the JVM. handover-stamp.sh captures each suite with $(...),
        // which blocks until the process closes stdout, so without this the stamp reaches
        // CompositeCheck at the end of batch one and hangs there forever.
        //
        // Found the hard way on 2026-08-25: a java process 67 minutes old holding 47 seconds
        // of CPU, which is the signature of work finished and a thread still alive.
        // AspectGridCheck has carried the same line since it began building a window; any
        // suite that touches Swing needs it.
        System.exit(0);
    }

    private static void liveCharts() {
        SwissEph sw = new SwissEph(EPHE_PATH);

        ChartFrame c1 = run(sw, 1980, 5, 15, 12.0, "Person 1");
        ChartFrame c2 = run(sw, 1990, 8, 20, 8.0, "Person 2");

        ChartFrame midpointComp = ChartFrame.computeMidpointComposite(sw, c1, c2);
        ChartFrame davisonComp = ChartFrame.computeDavisonComposite(sw, c1, c2);

        // 1. Spans sum to 360 (cusp intervals)
        checkCusps(midpointComp, "Midpoint");
        checkCusps(davisonComp, "Davison");

        // 2. Cusp 1 and 7 are exactly 180 apart (to 1e-9)
        near("Midpoint ASC/DSC opposed", 180.0, ChartFrame.separation(midpointComp.cusps[1], midpointComp.cusps[7]), 1e-9);
        near("Davison ASC/DSC opposed", 180.0, ChartFrame.separation(davisonComp.cusps[1], davisonComp.cusps[7]), 1e-9);

        // 3. Cusp 10 and 4 are exactly 180 apart (to 1e-9)
        near("Midpoint MC/IC opposed", 180.0, ChartFrame.separation(midpointComp.cusps[10], midpointComp.cusps[4]), 1e-9);
        near("Davison MC/IC opposed", 180.0, ChartFrame.separation(davisonComp.cusps[10], davisonComp.cusps[4]), 1e-9);

        // 4. ASC derived rather than averaged (midpoint composite)
        double avgAsc = ChartFrame.midpoint(c1.asc, c2.asc);
        checks++;
        if (Math.abs(ChartFrame.separation(midpointComp.asc, avgAsc)) < 1e-9) {
            failures.add("Midpoint ASC is a simple average, not derived!");
        }

        // 5. Synthetic fields genuinely unset for Midpoint, but Davison unaffected
        eq("Midpoint phase unset", null, midpointComp.phaseName);
        eq("Midpoint phase index unset", -1, midpointComp.phaseIndex);
        eq("Midpoint VOC unset", false, midpointComp.moonVoidOfCourse);
        eq("Midpoint syzygy NaN", true, Double.isNaN(midpointComp.syzygyLon));

        checks++;
        if (davisonComp.phaseName == null) failures.add("Davison phase is unset!");
        checks++;
        if (davisonComp.phaseIndex == -1) failures.add("Davison phase index is unset!");
        checks++;
        if (Double.isNaN(davisonComp.syzygyLon)) failures.add("Davison syzygy is NaN!");

        // 6. Retrograde forced false for midpoint composite bodies
        for (ChartFrame.Body b : midpointComp.bodies) {
            if (b.ok) {
                eq("Midpoint body " + b.name + " retrograde false", false, b.retrograde);
            }
        }

        // 7. The flag the UI keys off, and the sentence it shows. Added by Claude on top of
        //    Antigravity's suite: every assertion above tests a suppressed VALUE, but nothing
        //    tested the FLAG that tells the interface those values are absent on purpose. Flip
        //    syntheticMoment and the chart would render a missing phase as though it were simply
        //    unknown, with no caveat and no failing check.
        eq("Midpoint is marked synthetic", true, midpointComp.syntheticMoment);
        eq("Davison is NOT marked synthetic", false, davisonComp.syntheticMoment);
        checks++;
        if (midpointComp.syntheticNote == null || midpointComp.syntheticNote.isEmpty()) {
            failures.add("Midpoint composite carries no explanation for its absent fields");
        }
        eq("Davison carries no synthetic note", null, davisonComp.syntheticNote);

        // 8. Sect and the lots are COMPUTED for a midpoint composite, not suppressed with the
        //    rest. They follow from the Ascendant, the Sun and the Moon, all of which the chart
        //    genuinely has, and the convention says so explicitly - so a future tidy-up that
        //    suppressed them along with the time-derived fields would be wrong, and until now
        //    nothing would have caught it.
        checks++;
        if (midpointComp.lotOfFortune == 0.0 && midpointComp.lotOfSpirit == 0.0) {
            failures.add("Midpoint composite lots were not computed");
        }
        near("Midpoint Spirit mirrors Fortune across the asc", 0.0,
            ChartFrame.separation(Zodiac.normalise(2 * midpointComp.asc - midpointComp.lotOfFortune),
                                  midpointComp.lotOfSpirit), 1e-9);
        checks++;
        if (midpointComp.elongation == 0.0) {
            failures.add("Midpoint composite elongation was not computed");
        }
        // Sect must agree with the geometry it is derived from, synthetic ASC or not.
        eq("Midpoint sect matches its own Sun and Ascendant",
            Sect.isDiurnal(midpointComp.body("Sun").lon, midpointComp.asc),
            midpointComp.diurnal);
    }

    /**
     * A composite frame can be walked, ranked and interpreted like any other.
     *
     * <b>Written because it could not be, and the failure was total rather than partial.</b>
     * {@code computeMidpointComposite} set ok=false and an error message for any body missing
     * from either base chart but <b>never set that body&#39;s name</b>. {@code body(String)}
     * calls name.equals(..) on every entry, so it threw a NullPointerException - and the Part
     * of Spirit is not ok in an ordinary natal chart, so that branch fired on essentially
     * every midpoint composite the app has ever built.
     *
     * The consequence was that BodyScore.rank -&gt; Gestalt.compute -&gt; TransferOfLight -&gt;
     * body(String) crashed on any composite, which is the whole interpretation path. Nothing
     * caught it because every existing check reads {@code bodies[i]} by index and never asks
     * the frame for a body by name.
     *
     * <b>ok says whether to interpret a body, not whether it is there.</b> An entry that
     * exists must be identifiable either way, and that is what these assert.
     */
    private static void usableFrame() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame a = run(sw, 1980, 5, 15, 12.0, "Person 1");
        ChartFrame b = run(sw, 1990, 8, 20, 8.0, "Person 2");

        ChartFrame[] frames = {
            ChartFrame.computeMidpointComposite(sw, a, b),
            ChartFrame.computeDavisonComposite(sw, a, b),
        };
        String[] names = {"midpoint", "davison"};

        for (int f = 0; f < frames.length; f++) {
            ChartFrame comp = frames[f];
            int nameless = 0;
            for (int i = 0; i < comp.bodies.length; i++) {
                ChartFrame.Body body = comp.bodies[i];
                checks++;
                if (body == null) {
                    failures.add(names[f] + ": body slot " + Bodies.at(i).id + " is null");
                    continue;
                }
                if (body.name == null) {
                    failures.add(names[f] + ": " + Bodies.at(i).id
                        + " has no name - body(String) will throw on this frame");
                    nameless++;
                    continue;
                }
                eq(names[f] + ": " + Bodies.at(i).id + " is named from the registry",
                    Bodies.at(i).name, body.name);
            }

            // The call that actually threw. Every registry name, not a sample.
            for (int i = 0; i < Bodies.count(); i++) {
                checks++;
                try {
                    comp.body(Bodies.at(i).name);
                } catch (IllegalArgumentException expected) {
                    failures.add(names[f] + ": body(" + Bodies.at(i).name + ") not found");
                } catch (RuntimeException e) {
                    failures.add(names[f] + ": body(" + Bodies.at(i).name + ") threw " + e);
                }
            }

            // And the whole path that crashed, end to end. A rank that completes is the
            // evidence; the ranking itself is BodyScore&#39;s business, not this suite&#39;s.
            checks++;
            try {
                int ranked = BodyScore.rank(comp).size();
                if (ranked <= 0) {
                    failures.add(names[f] + ": ranked no bodies at all");
                }
            } catch (RuntimeException e) {
                failures.add(names[f] + ": BodyScore.rank threw " + e
                    + " - the interpretation path is broken for this frame");
            }

            System.out.println("  " + names[f] + ": " + comp.bodies.length + " bodies, "
                + nameless + " nameless");
        }
    }

    private static void checkCusps(ChartFrame f, String label) {
        double totalSpan = 0.0;
        for (int i = 1; i <= 12; i++) {
            int next = i == 12 ? 1 : i + 1;
            double span = f.cusps[next] - f.cusps[i];
            if (span < 0) span += 360.0;
            totalSpan += span;
        }
        near(label + " cusps sum to 360", 360.0, totalSpan, 1e-9);
    }

    private static ChartFrame run(SwissEph sw, int y, int m, int d, double hourUt, String label) {
        SweDate sd = new SweDate(y, m, d, hourUt);
        return ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'W', false, 0.0);
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null) {
            if (actual != null) failures.add(label + ": got " + actual + ", expected null");
        } else if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected + " +/- " + tol);
        }
    }

    /**
     * The composite must survive anything else asking the panel for a frame.
     *
     * <b>This part exists because Parts A and B could not have caught the defect it guards.</b>
     * They test {@link ChartFrame#computeMidpointComposite} in isolation, and that method was
     * always correct. The bug was one field further out: {@code SkymapPanel.getCurrentChart}
     * cached the composite in the same field {@code frameForCurrentChart} memoises natal charts
     * in, so the first call to that method <b>replaced the composite with person A's natal
     * chart</b>, and every later read - the wheel's included - got the natal chart back.
     * Measured at the time: the Sun 66.9 degrees out, the Ascendant 237.
     *
     * So this reaches through to the panel deliberately, from an astro-package check, because
     * the seam between the engine and the view is precisely where the defect lived and neither
     * side's own tests could see it.
     */
    private static void panelKeepsComposite() {
        try {
            SwissEph sw = new SwissEph(EPHE_PATH);
            SweDate a = new SweDate(1985, 3, 14, 9.5);
            SweDate b = new SweDate(1991, 11, 2, 22.25);
            double aLat = 34.05, aLon = -118.24, bLat = 51.51, bLon = -0.13;
            char hsys = 'P';

            ChartFrame fa = ChartFrame.compute(sw, a.getJulDay(), aLat, aLon, hsys, false, 0.0);
            ChartFrame fb = ChartFrame.compute(sw, b.getJulDay(), bLat, bLon, hsys, false, 0.0);
            ChartFrame truth = ChartFrame.computeMidpointComposite(sw, fa, fb);
            double truthSun = truth.body("Sun").lon;
            double natalSun = fa.body("Sun").lon;

            // The two must be far apart or this check proves nothing.
            near("Part C fixture: composite and natal Sun differ by more than 10 deg",
                1.0, Math.abs(truthSun - natalSun) > 10.0 ? 1.0 : 0.0, 1e-9);

            Class<?> panelCls = Class.forName("com.zodiacomputing.ourania.gui.SkymapPanel");
            Class<?> modeCls = Class.forName("com.zodiacomputing.ourania.gui.ChartMode");
            Object panel = panelCls.getConstructor(
                Class.forName("com.zodiacomputing.ourania.gui.OuraniaWindow"))
                .newInstance(new Object[] {null});

            set(panelCls, panel, "sw", sw);
            set(panelCls, panel, "baseSd", a);
            set(panelCls, panel, "transitSd", b);
            set(panelCls, panel, "baseLatitude", aLat);
            set(panelCls, panel, "baseLongitude", aLon);
            set(panelCls, panel, "transitLatitude", bLat);
            set(panelCls, panel, "transitLongitude", bLon);
            set(panelCls, panel, "houseSystem", hsys);
            set(panelCls, panel, "chartMode",
                Enum.valueOf((Class<Enum>) modeCls.asSubclass(Enum.class), "COMPOSITE_MIDPOINT"));

            java.lang.reflect.Method get = panelCls.getMethod("getCurrentChart");
            ChartFrame first = (ChartFrame) get.invoke(panel);
            near("Part C: first getCurrentChart is the composite",
                truthSun, first.body("Sun").lon, 1e-6);
            eq("Part C: first frame is synthetic", Boolean.TRUE, first.syntheticMoment);

            // What the reading panel does. This is the call that used to poison the cache.
            java.lang.reflect.Method ffc = panelCls.getDeclaredMethod(
                "frameForCurrentChart", double.class, double.class, double.class, int.class);
            ffc.setAccessible(true);
            ffc.invoke(panel, a.getJulDay(), aLat, aLon, (int) hsys);

            ChartFrame second = (ChartFrame) get.invoke(panel);
            near("Part C: composite survives frameForCurrentChart",
                truthSun, second.body("Sun").lon, 1e-6);

            // <b>What the reading actually reads.</b> Everything above proves the composite
            // CACHE is intact; none of it proved the reading tiers use it, and until
            // 2026-08-30 they did not - showReading called frameForCurrentChart and every
            // reading in a composite mode described person A's natal chart. frameForReading
            // is the seam showReading calls, so asserting it here asserts the binding rather
            // than a restatement of it.
            java.lang.reflect.Method reading = panelCls.getDeclaredMethod("frameForReading");
            reading.setAccessible(true);
            ChartFrame read = (ChartFrame) reading.invoke(panel);
            near("Part C: the READING frame is the composite, not chart A",
                truthSun, read.body("Sun").lon, 1e-6);
            eq("Part C: the reading frame is marked synthetic",
                Boolean.TRUE, read.syntheticMoment);
            checks++;
            if (Math.abs(read.body("Sun").lon - natalSun) < 1e-6) {
                failures.add("Part C: the reading frame is person A's natal chart");
            }
            eq("Part C: still synthetic afterwards", Boolean.TRUE, second.syntheticMoment);
            near("Part C: Ascendant survives too", first.asc, second.asc, 1e-6);

            // And it must not have quietly become the natal chart specifically.
            checks++;
            if (Math.abs(second.body("Sun").lon - natalSun) < 1e-6) {
                failures.add("Part C: getCurrentChart returned person A's NATAL chart, not the "
                    + "composite - the frame cache has been shared again");
            }
        } catch (Exception e) {
            checks++;
            failures.add("Part C: threw " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * A composite stands on its own unless transits were asked for.
     *
     * <b>The expectations are a literal table, not the same boolean expression restated.</b>
     * Re-deriving them here would assert only that a copy of the rule agrees with the rule.
     * This is David's decision of 2026-08-25 written down as data: transits become an explicit
     * choice, and the composite no longer forces an outer wheel on merely because it is a
     * composite - which is what used to hand a composite reading the transit array.
     *
     * <b>Synastry's outer-wheel column is true in both rows and that is not a bug.</b> Its outer
     * wheel is the second person, so one is always drawn whatever the checkbox says.
     *
     * <b>The checkbox drives TWO flags, so the table has two expectation columns.</b> It grew
     * one on 2026-08-28, when the tri-wheel landed and this table did not notice. It asserted
     * {@code outerWheelShown} only - which the tri-wheel did not change - so it stayed green
     * while half of David's decision went uncovered, and its own prose still said synastry
     * transits "would be a third wheel and this app does not draw one yet". It does now:
     * SYNASTRY + transits is the tri-wheel, and that is the one true cell in the third column.
     */
    private static void transitsAreAChoice() {
        //                                          transits  outer   tri
        Object[][] spec = {
            {ChartMode.SINGLE,             false, false, false},
            {ChartMode.SINGLE,             true,  false, false},
            {ChartMode.TRANSIT,            false, false, false},
            {ChartMode.TRANSIT,            true,  true , false},
            {ChartMode.SYNASTRY,           false, true , false},
            {ChartMode.SYNASTRY,           true,  true , true },
            {ChartMode.COMPOSITE_MIDPOINT, false, false, false},
            {ChartMode.COMPOSITE_MIDPOINT, true,  true , false},
            {ChartMode.COMPOSITE_DAVISON,  false, false, false},
            {ChartMode.COMPOSITE_DAVISON,  true,  true , false},
        };
        for (Object[] row : spec) {
            ChartMode mode = (ChartMode) row[0];
            boolean transits = (Boolean) row[1];
            boolean wantOuter = (Boolean) row[2];
            boolean wantTri = (Boolean) row[3];
            eq("Part D: " + mode + " with transits=" + transits + " draws an outer wheel",
                Boolean.valueOf(wantOuter),
                Boolean.valueOf(SkymapPanel.outerWheelShown(mode, transits)));
            eq("Part D: " + mode + " with transits=" + transits + " draws a tri-wheel",
                Boolean.valueOf(wantTri),
                Boolean.valueOf(SkymapPanel.triWheelShown(mode, transits)));
        }
        // A tri-wheel is a third ring around an outer wheel, never a ring on its own. Stated
        // as a relationship between the two flags rather than a third copy of the rule.
        for (Object[] row : spec) {
            ChartMode mode = (ChartMode) row[0];
            boolean transits = (Boolean) row[1];
            eq("Part D: " + mode + "/" + transits + " - a tri-wheel implies an outer wheel",
                Boolean.TRUE,
                Boolean.valueOf(!SkymapPanel.triWheelShown(mode, transits)
                    || SkymapPanel.outerWheelShown(mode, transits)));
        }
    }

    /**
     * A pinned pair, chosen for its geometry: the defects it exposes and the figure it found.
     *
     * <b>Why a specific pair rather than swept inputs.</b> Both defects here were invisible to
     * generated data. The two-Ascendants disagreement hides completely under whole-sign houses
     * because both values fall in the same sign, and the near-opposition instability only shows
     * when two charts happen to put a point ~178 degrees apart. A pair has to be selected for
     * that; sweeping will not stumble into it.
     *
     * <b>Synthetic, and not anyone's birth data.</b> The original fixture was a real couple.
     * It was replaced on 2026-08-28 because this repository is public and the charts were
     * identifiable. A is the same synthetic reference chart the other suites share; B was
     * found by scanning candidate dates for one that reproduces the required geometry.
     *
     * <b>If this pair ever needs replacing, it must be searched for, not guessed.</b> B was
     * found by scanning ~65,000 candidates for one satisfying EVERY property this part
     * asserts, so the replacement changed only the pinned longitudes and not the shape of the
     * test. The search condition, worth reusing:
     * <ul>
     *   <li>{@code Part of Spirit} flagged {@code unstableMidpoint}, and the Sun NOT flagged -
     *       otherwise the test would pass on a rule that flags everything;</li>
     *   <li>a warning left on the frame;</li>
     *   <li>the Ascendant not flagged, and the derived angles equal to their registry bodies;</li>
     *   <li>a T-square with apex Venus and modality fixed, carrying a vacant empty leg.</li>
     * </ul>
     * Two candidates matched; this is the earlier of them.
     *
     * A 1984-09-08 07:33 UT, 41.88 N 87.63 W; B 1967-04-10 19:00 UT, 34.05 N 118.24 W.
     */
    private static void referencePair() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame a = ChartFrame.compute(sw,
            new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay(),
            41.8781, -87.6298, 'W', false, 0.0);
        ChartFrame b = ChartFrame.compute(sw,
            new SweDate(1967, 4, 10, 19.0).getJulDay(),
            34.05, -118.24, 'W', false, 0.0);
        ChartFrame c = ChartFrame.computeMidpointComposite(sw, a, b);

        // 1. The angles as registry bodies must BE the derived angles. Before 2026-08-25 the
        //    Ascendant disagreed with itself by 12.93 degrees on the ORIGINAL fixture pair,
        //    which this synthetic one replaced on 2026-08-28. The assertion is an invariant
        //    and holds for any pair; the 12.93 figure is kept as the history of why it exists.
        near("Part E: Ascendant body == derived asc", c.asc, c.body("Ascendant").lon, 1e-9);
        near("Part E: Descendant body == derived dsc", c.dsc, c.body("Descendant").lon, 1e-9);
        near("Part E: MC body == derived mc", c.mc, c.body("MC").lon, 1e-9);
        near("Part E: IC body == derived ic", c.ic, c.body("IC").lon, 1e-9);
        checks++;
        if (c.body("Ascendant").unstableMidpoint) {
            failures.add("Part E: a derived angle is flagged as an unstable midpoint");
        }

        // 2. Part of Spirit IS unstable for this pair - the two charts put it 178.45 apart,
        //    and near an exact opposition a few minutes of birth time swings the composite
        //    value by ~178 degrees. B was selected to reproduce this; see the javadoc.
        ChartFrame.Body spirit = c.body("Part of Spirit");
        checks++;
        if (spirit == null || !spirit.ok || !spirit.unstableMidpoint) {
            failures.add("Part E: Part of Spirit should be flagged unstable for this pair");
        }
        checks++;
        if (c.warnings.isEmpty()) {
            failures.add("Part E: an unstable midpoint should leave a warning on the frame");
        }
        // And the Sun, which the two charts put nowhere near opposite, must NOT be flagged -
        // otherwise the test passes on a rule that flags everything.
        checks++;
        if (c.body("Sun").unstableMidpoint) {
            failures.add("Part E: the Sun is not near-opposition and must not be flagged");
        }

        // 3. The figure this pair actually has, so a change to pattern detection or to the
        //    empty leg shows up against known-good output rather than silently.
        //
        //    <b>Regenerated 2026-08-30 when the fixture pair was replaced.</b> The longitudes
        //    are characterization values - what a verified-green build produces for THIS pair -
        //    exactly as the previous set was for the previous pair. They catch a regression in
        //    pattern detection; they are not an independent derivation.
        //
        //    <b>The apex and modality are NOT regenerated.</b> B was selected so the T-square
        //    still comes out Venus/fixed, as it did on the original pair, which keeps these two
        //    assertions verifying the same thing rather than whatever the new pair happened to
        //    produce. Only the positions moved, because positions must.
        Gestalt.Result g = Gestalt.compute(c);
        AspectPatterns.Pattern t = null;
        for (AspectPatterns.Pattern p : g.aspectPatterns) {
            if (p.name.equals("T-square")) {
                t = p;
            }
        }
        checks++;
        if (t == null) {
            failures.add("Part E: the reference composite's T-square has gone");
        } else {
            eq("Part E: T-square apex", "Venus", t.apex);
            eq("Part E: T-square modality", "fixed", t.modality);
            near("Part E: composite Venus", 122.21, c.body("Venus").lon, 0.05);
            near("Part E: composite Chiron", 32.84, c.body("Chiron").lon, 0.05);
            near("Part E: composite Uranus", 210.48, c.body("Uranus").lon, 0.05);
            for (TensionRelease.Release r : g.releases) {
                if (r.source.equals("T-square") && r.hasEmptyLeg) {
                    near("Part E: empty leg opposite the apex", 302.21, r.emptyLegLon, 0.05);
                    eq("Part E: empty leg is vacant", null, r.emptyLegOccupant);
                }
            }
        }
    }

    /**
     * Verifies that computeMidpointComposite falls back gracefully to whole-sign-ish equal houses
     * from the midpoint MC when the quadrant house system has no solution or when sw is null.
     */
    /**
     * The midpoint place is on the great circle, not the average of the two coordinates.
     *
     * <b>The expected values were computed outside this codebase</b> - an independent
     * implementation of the standard great-circle midpoint - so this verifies the formula
     * rather than recording whatever the method currently returns. Averaging the coordinates
     * separately treats the globe as a flat grid; great circles bow poleward, so the two
     * answers separate as the pair spreads out, reaching 31 degrees of latitude for New
     * York/Tokyo. That is house cusps for the wrong part of the world.
     */
    /**
     * The reference place actually reaches the cusps, and reaches nothing else.
     *
     * <b>Two halves, and the second is the one that catches a wrong implementation.</b> It is
     * easy to write a setting that changes the chart; the claim here is narrower - latitude
     * moves the house frame and moves NOTHING else, because the bodies are midpoints of two
     * charts and cannot depend on where you stand to look at them. A version that rebuilt the
     * bodies from the reference place would pass a "does it change the chart" test and fail
     * this one.
     */
    private static void referencePlaceIsSettable() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame a = ChartFrame.compute(sw,
            new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay(),
            41.8781, -87.6298, 'P', false, 0.0);
        ChartFrame b = ChartFrame.compute(sw,
            new SweDate(1967, 4, 10, 19.0).getJulDay(), 34.05, -118.24, 'P', false, 0.0);

        ChartFrame dflt = ChartFrame.computeMidpointComposite(sw, a, b);
        double[] mid = ChartFrame.geographicMidpoint(a.geoLat, a.geoLon, b.geoLat, b.geoLon);

        // 1. The no-argument form is exactly the great-circle midpoint form.
        ChartFrame explicitMid = ChartFrame.computeMidpointComposite(sw, a, b, mid[0], mid[1]);
        near("Part H: default equals the explicit midpoint (asc)", dflt.asc, explicitMid.asc, 1e-9);
        near("Part H: default equals the explicit midpoint (lat)", mid[0], dflt.geoLat, 1e-9);

        // 2. A different latitude derives a different house frame.
        ChartFrame far = ChartFrame.computeMidpointComposite(sw, a, b, 12.0, 77.0);
        near("Part H: the reference latitude is recorded", 12.0, far.geoLat, 1e-9);
        near("Part H: the reference longitude is recorded", 77.0, far.geoLon, 1e-9);
        checks++;
        if (Math.abs(ChartFrame.separation(far.asc, dflt.asc)) < 1.0) {
            failures.add("Part H: moving the reference place 26 degrees of latitude left the "
                + "Ascendant within a degree - the setting is not reaching the house frame");
        }
        checks++;
        boolean anyCuspMoved = false;
        for (int i = 1; i <= 12; i++) {
            if (Math.abs(ChartFrame.separation(far.cusps[i], dflt.cusps[i])) > 1e-6) {
                anyCuspMoved = true;
            }
        }
        if (!anyCuspMoved) {
            failures.add("Part H: no house cusp moved with the reference place");
        }

        // 3. And it reaches NOTHING else. Bodies are midpoints of two charts; where you stand
        //    cannot move them, and the MC comes from the two MCs, not from the reference place.
        for (int i = 0; i < dflt.bodies.length; i++) {
            ChartFrame.Body d = dflt.bodies[i];
            ChartFrame.Body g = far.bodies[i];
            if (d == null || g == null || !d.ok || !g.ok || Bodies.at(i).isAngle()) {
                continue;
            }
            near("Part H: " + d.name + " is unmoved by the reference place", d.lon, g.lon, 1e-9);
        }
        near("Part H: the composite MC is unmoved by the reference place", dflt.mc, far.mc, 1e-9);

        // 4. Longitude alone changes nothing - stated because the UI invites the opposite guess.
        ChartFrame sameLatOtherLon =
            ChartFrame.computeMidpointComposite(sw, a, b, mid[0], mid[1] + 40.0);
        near("Part H: longitude alone does not move the Ascendant",
            dflt.asc, sameLatOtherLon.asc, 1e-9);
        for (int i = 1; i <= 12; i++) {
            near("Part H: longitude alone does not move cusp " + i,
                dflt.cusps[i], sameLatOtherLon.cusps[i], 1e-9);
        }
    }

    private static void geographicMidpoint() {
        // {lat1, lon1, lat2, lon2, expectedLat, expectedLon}
        double[][] spec = {
            // Philadelphia / Los Angeles - grid average would say 37.00, -96.70
            {39.9526, -75.1652, 34.05, -118.24, 39.010, -97.581},
            // Chicago / Los Angeles - grid average 37.96, -102.94
            {41.8781, -87.6298, 34.05, -118.24, 38.969, -103.772},
            // London / Sydney - grid average 8.82, 75.54
            {51.5074, -0.1278, -33.8688, 151.2093, 28.672, 104.797},
            // New York / Tokyo - grid average 38.20, -147.18, out by 31 degrees of latitude
            {40.7128, -74.0060, 35.6762, 139.6503, 69.677, -153.704},
            // Oslo / Vancouver - grid average 54.60, -56.18
            {59.9139, 10.7522, 49.2827, -123.1207, 73.760, -73.275},
        };
        for (double[] r : spec) {
            double[] got = ChartFrame.geographicMidpoint(r[0], r[1], r[2], r[3]);
            near("Part G: midpoint lat of (" + r[0] + "," + r[1] + ")/(" + r[2] + "," + r[3] + ")",
                r[4], got[0], 0.005);
            near("Part G: midpoint lon of (" + r[0] + "," + r[1] + ")/(" + r[2] + "," + r[3] + ")",
                r[5], got[1], 0.005);
        }

        // Symmetric: the midpoint cannot depend on which person is named first.
        for (double[] r : spec) {
            double[] ab = ChartFrame.geographicMidpoint(r[0], r[1], r[2], r[3]);
            double[] ba = ChartFrame.geographicMidpoint(r[2], r[3], r[0], r[1]);
            near("Part G: symmetric in lat", ab[0], ba[0], 1e-9);
            near("Part G: symmetric in lon", ab[1], ba[1], 1e-9);
        }

        // A point with itself is itself.
        double[] same = ChartFrame.geographicMidpoint(51.5074, -0.1278, 51.5074, -0.1278);
        near("Part G: a place with itself is itself (lat)", 51.5074, same[0], 1e-9);
        near("Part G: a place with itself is itself (lon)", -0.1278, same[1], 1e-9);

        // Two points on the equator stay on the equator - the one case where the grid
        // average is also right, so a broken implementation cannot hide here.
        double[] eq = ChartFrame.geographicMidpoint(0.0, 10.0, 0.0, 50.0);
        near("Part G: equator stays on the equator", 0.0, eq[0], 1e-9);
        near("Part G: equator midpoint longitude", 30.0, eq[1], 1e-9);

        // Across the dateline: 179E and 179W are 2 degrees apart, not 358.
        double[] dl = ChartFrame.geographicMidpoint(0.0, 179.0, 0.0, -179.0);
        near("Part G: dateline midpoint stays on the equator", 0.0, dl[0], 1e-9);
        checks++;
        if (Math.abs(Math.abs(dl[1]) - 180.0) > 1e-6) {
            failures.add("Part G: dateline midpoint should be at +/-180, got " + dl[1]);
        }

        // Antipodal points have no unique midpoint. The contract is a defined number, not NaN.
        double[] anti = ChartFrame.geographicMidpoint(45.0, 0.0, -45.0, 180.0);
        checks++;
        if (Double.isNaN(anti[0]) || Double.isNaN(anti[1])) {
            failures.add("Part G: antipodal midpoint returned NaN instead of falling back");
        }
    }

    private static void equalHouseFallback() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame a = ChartFrame.compute(sw, new SweDate(1980, 5, 15, 12.0).getJulDay(), LAT, LON, 'P', false, 0.0);
        ChartFrame b = ChartFrame.compute(sw, new SweDate(1990, 8, 20, 8.0).getJulDay(), LAT, LON, 'P', false, 0.0);

        // Force fallback by passing sw = null
        ChartFrame fallbackChart = ChartFrame.computeMidpointComposite(null, a, b);

        checks++;
        if (fallbackChart == null) {
            failures.add("Part F: computeMidpointComposite returned null on fallback");
            return;
        }

        // Verify cusps array has 12 valid cusps
        checks++;
        boolean validCusps = fallbackChart.cusps != null && fallbackChart.cusps.length == 13;
        if (!validCusps) {
            failures.add("Part F: fallback chart cusps array is missing or invalid size");
        } else {
            // Verify equal-house spacing (30 degrees apart)
            double firstCusp = fallbackChart.cusps[1];
            boolean equalSpaced = true;
            for (int i = 1; i <= 12; i++) {
                double expectedCusp = Zodiac.normalise(firstCusp + (i - 1) * 30.0);
                if (Math.abs(Aspects.separation(fallbackChart.cusps[i], expectedCusp)) > 0.001) {
                    equalSpaced = false;
                    break;
                }
            }
            checks++;
            if (!equalSpaced) {
                failures.add("Part F: fallback chart cusps are not equal-house 30 degrees apart");
            }
        }

        // Verify syntheticNote records the fallback explanation
        checks++;
        if (fallbackChart.syntheticNote == null || !fallbackChart.syntheticNote.contains("fell back to equal houses")) {
            failures.add("Part F: syntheticNote does not document the equal-house fallback");
        }
    }

    private static void set(Class<?> cls, Object target, String field, Object value)
            throws Exception {
        java.lang.reflect.Field f = cls.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
