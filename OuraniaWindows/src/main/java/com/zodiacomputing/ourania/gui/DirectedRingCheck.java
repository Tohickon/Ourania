package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartSubject;
import com.zodiacomputing.ourania.astro.SolarArc;
import com.zodiacomputing.ourania.astro.Zodiac;

import javax.swing.SwingUtilities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The middle ring carrying a directed chart, and the chips that ask for it.
 *
 *  Part A - the kind. Every {@code WheelRing.Kind} carries its own heading now, because the
 *           headings used to live in a switch with a default - which is where a seventh kind
 *           gets silently called "Chart". No two kinds may share a heading or a dash.
 *  Part B - the setting: three choices, and garbage falls back rather than sticking.
 *  Part C - the arithmetic, and it is the whole claim. A directed chart is the birth chart
 *           with ONE NUMBER added to every position, so every body's distance from its natal
 *           place must be the SAME distance - and it must be the arc SolarArc computes.
 *  Part D - the contrast that proves the two rings are different things: under progressions
 *           those distances are all different, because bodies move at their own speeds.
 *  Part E - the chips are lit by the setting, and unavailable where the ring is a person.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.gui.DirectedRingCheck
 */
public final class DirectedRingCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        System.out.println("=== Part A: every kind names itself ===");
        theKinds();

        System.out.println();
        System.out.println("=== Part B: the setting ===");
        theSetting();

        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            castAChart(sky);

            System.out.println();
            System.out.println("=== Part C: one arc, added to everything ===");
            double arc = theDirectedRing(sky);

            System.out.println();
            System.out.println("=== Part D: progressions are not that ===");
            theProgressedRing(sky, arc);

            System.out.println();
            System.out.println("=== Part E: the chips ===");
            theChips(w);
        } finally {
            Settings.setOuterWheel(Settings.OUTER_TRANSITS);
            SwingUtilities.invokeAndWait(w::dispose);
        }

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String s : failures) {
                System.out.println("  " + s);
            }
            System.exit(1);
        }
        // A window keeps the JVM alive; a suite that has built one has to say when it is done.
        System.exit(0);
    }

    // ---------------------------------------------------------------- part A

    private static void theKinds() {
        Set<String> headings = new HashSet<>();
        Set<String> dashes = new HashSet<>();
        for (WheelRing.Kind k : WheelRing.Kind.values()) {
            yes(k + " has a heading", k.heading != null && !k.heading.isEmpty());
            yes(k + " has a word for prose", k.label != null && !k.label.isEmpty());
            // <b>The point of moving headings onto the enum.</b> A kind added later cannot be
            // silently called "Chart", because the constructor will not let it be nameless.
            yes(k + "'s heading is its own: " + k.heading, headings.add(k.heading));
            if (k.dash != null) {
                StringBuilder d = new StringBuilder();
                for (float f : k.dash) {
                    d.append(f).append(',');
                }
                // Dashing is how a reader tells WHICH chart a line belongs to, so two rings
                // sharing a pattern would be two charts that look alike.
                yes(k + "'s dash pattern is its own: " + d, dashes.add(d.toString()));
            }
        }
        WheelRing.Kind arcKind = WheelRing.Kind.SOLAR_ARC;
        eq("the directed ring is headed Solar Arc", "Solar Arc", arcKind.heading);
        eq("and called directed in prose", "directed", arcKind.label);
        yes("it is a moment's ring, not a person's", !arcKind.isPerson);
        eq("the page title comes from the kind", arcKind.heading,
            OuraniaWindow.pageTitleOf(arcKind));
    }

    // ---------------------------------------------------------------- part B

    private static void theSetting() {
        eq("three things the middle ring can carry", 3, Settings.OUTER_WHEELS.length);
        List<String> all = java.util.Arrays.asList(Settings.OUTER_WHEELS);
        yes("the sky is one of them", all.contains(Settings.OUTER_TRANSITS));
        yes("progressions another", all.contains(Settings.OUTER_PROGRESSED));
        yes("and the directed chart the third", all.contains(Settings.OUTER_SOLAR_ARC));

        Settings.setOuterWheel(Settings.OUTER_SOLAR_ARC);
        eq("what is chosen is what comes back", Settings.OUTER_SOLAR_ARC, Settings.outerWheel());
        Settings.set(Settings.OUTER_WHEEL_KEY, "Regiomontanus");
        eq("and something that is not on the list falls back rather than sticking",
            Settings.OUTER_TRANSITS, Settings.outerWheel());
    }

    // ---------------------------------------------------------------- the chart

    private static void castAChart(SkymapPanel sky) throws Exception {
        ChartSubject a = ChartSubject.of("Chart A",
            ZonedDateTime.of(1982, 8, 10, 15, 1, 0, 0, ZoneId.of("America/New_York")),
            "Philadelphia", 39.95, -75.16, "America/New_York", false);
        ChartSubject skySubject = ChartSubject.of("Sky",
            ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")),
            "Philadelphia", 39.95, -75.16, "America/New_York", false);
        java.lang.reflect.Method install = SkymapPanel.class.getDeclaredMethod(
            "installSubjects", ChartSubject.class, ChartSubject.class, ChartSubject.class);
        install.setAccessible(true);
        install.invoke(sky, a, ChartSubject.empty("Chart B"), skySubject);
        CheckReflect.set(sky, "chartMode", ChartMode.TRANSIT);
        CheckReflect.set(sky, "transitsEnabled", Boolean.TRUE);
    }

    private static void refresh(SkymapPanel sky) throws Exception {
        sky.reloadAspectSelection();
        java.lang.reflect.Method flags = SkymapPanel.class.getDeclaredMethod("applyRingFlags");
        flags.setAccessible(true);
        flags.invoke(sky);
        sky.updateChartData();
        SwingUtilities.invokeAndWait(() -> { });
    }

    // ---------------------------------------------------------------- part C

    private static double theDirectedRing(SkymapPanel sky) throws Exception {
        Settings.setOuterWheel(Settings.OUTER_SOLAR_ARC);
        refresh(sky);

        eq("the middle ring says it is directed", WheelRing.Kind.SOLAR_ARC, sky.outerRing.kind);

        List<Double> offsets = new ArrayList<>();
        int moved = 0;
        for (int i = 0; i < sky.outerRing.lon.length; i++) {
            if (!sky.outerRing.valid[i] || !sky.natalRing.valid[i]) {
                continue;
            }
            moved++;
            offsets.add(Zodiac.normalise(sky.outerRing.lon[i] - sky.natalRing.lon[i]));
            // Nothing in a directed chart is moving under its own power, so drawing a
            // retrograde mark on one would be a confident wrong answer.
            eq(Bodies.at(i).name + " is carried, not moving", 0.0, sky.outerRing.speed[i]);
        }
        yes("the directed ring has bodies on it: " + moved, moved > 5);
        yes("offsets were collected", !offsets.isEmpty());
        if (offsets.isEmpty()) {
            return Double.NaN;
        }

        // <b>The defining property.</b> A solar arc direction is ONE number added to every
        // position. If any body has moved a different distance from its natal place, this is
        // not a directed chart - it is something else wearing the name.
        double first = offsets.get(0);
        double widest = 0.0;
        for (double o : offsets) {
            double apart = Math.abs(((o - first) % 360.0 + 540.0) % 360.0 - 180.0);
            widest = Math.max(widest, apart);
        }
        System.out.printf("  %d bodies, all moved %.4f degrees, widest disagreement %.9f%n",
            moved, first, widest);
        yes("every body has moved the SAME arc, within a millionth of a degree", widest < 1e-6);

        // Both through CheckReflect: the ephemeris handle and the moment the ring was cast
        // at are the panel's own business, and a suite reaching for them does not make them
        // public - which is the seam J14 and ErrorLog.dirOverride already set the pattern for.
        de.thmac.swisseph.SwissEph swe =
            (de.thmac.swisseph.SwissEph) CheckReflect.get(sky, "sw");
        de.thmac.swisseph.SweDate castAt =
            (de.thmac.swisseph.SweDate) CheckReflect.get(sky, "outerCastAt");
        double expected = SolarArc.arcAt(swe, sky.natalRing.sd.getJulDay(),
            sky.natalRing.lon[Bodies.indexOfName("Sun")], castAt.getJulDay());
        yes("and the arc is the one SolarArc computes ("
            + String.format("%.4f against %.4f", first, expected) + ")",
            Math.abs(Zodiac.normalise(first - expected)) < 1e-6
                || Math.abs(Zodiac.normalise(expected - first)) < 1e-6);
        return first;
    }

    // ---------------------------------------------------------------- part D

    private static void theProgressedRing(SkymapPanel sky, double arc) throws Exception {
        Settings.setOuterWheel(Settings.OUTER_PROGRESSED);
        refresh(sky);
        eq("the middle ring says it is progressed", WheelRing.Kind.PROGRESSED, sky.outerRing.kind);

        List<Double> offsets = new ArrayList<>();
        for (int i = 0; i < sky.outerRing.lon.length; i++) {
            if (sky.outerRing.valid[i] && sky.natalRing.valid[i]) {
                offsets.add(Zodiac.normalise(sky.outerRing.lon[i] - sky.natalRing.lon[i]));
            }
        }
        yes("the progressed ring has bodies on it", offsets.size() > 5);
        if (offsets.isEmpty()) {
            return;
        }
        double widest = 0.0;
        for (double o : offsets) {
            double apart = Math.abs(((o - offsets.get(0)) % 360.0 + 540.0) % 360.0 - 180.0);
            widest = Math.max(widest, apart);
        }
        System.out.printf("  progressed: the same bodies disagree by up to %.3f degrees%n", widest);
        // <b>The contrast is the assertion.</b> Under progressions each body has moved at its
        // own speed, so the offsets must NOT agree - if they did, the two rings would be
        // computing the same thing under two names and Part C would prove nothing.
        yes("under progressions the bodies have moved by DIFFERENT amounts ("
            + String.format("%.3f", widest) + " apart)", widest > 1.0);
    }

    // ---------------------------------------------------------------- part E

    private static void theChips(OuraniaWindow w) throws Exception {
        RingBar bar = findBar(w);
        yes("the wheel has a ring bar", bar != null);
        if (bar == null) {
            return;
        }
        Settings.setOuterWheel(Settings.OUTER_SOLAR_ARC);
        yes("the Solar Arc chip is lit when the ring is directed", chipLit(bar, "Solar Arc"));
        yes("and the Progressed chip is not", !chipLit(bar, "Progressed"));
        Settings.setOuterWheel(Settings.OUTER_PROGRESSED);
        yes("they swap when the setting does", chipLit(bar, "Progressed"));
        yes("because there is one ring between them", !chipLit(bar, "Solar Arc"));
        Settings.setOuterWheel(Settings.OUTER_TRANSITS);
        yes("and with the sky on the ring neither is lit",
            !chipLit(bar, "Progressed") && !chipLit(bar, "Solar Arc"));
    }

    private static RingBar findBar(java.awt.Container c) {
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof RingBar) {
                return (RingBar) child;
            }
            if (child instanceof java.awt.Container) {
                RingBar found = findBar((java.awt.Container) child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Asks the chip itself, through the same method that paints it. */
    private static boolean chipLit(RingBar bar, String label) throws Exception {
        for (java.awt.Component child : bar.getComponents()) {
            java.lang.reflect.Field f;
            try {
                f = child.getClass().getDeclaredField("label");
            } catch (NoSuchFieldException e) {
                continue;
            }
            f.setAccessible(true);
            if (!label.equals(f.get(child))) {
                continue;
            }
            java.lang.reflect.Method open = child.getClass().getDeclaredMethod("open");
            open.setAccessible(true);
            return (Boolean) open.invoke(child);
        }
        failures.add("no chip labelled " + label);
        checks++;
        return false;
    }

    // ---------------------------------------------------------------- harness

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }
}
