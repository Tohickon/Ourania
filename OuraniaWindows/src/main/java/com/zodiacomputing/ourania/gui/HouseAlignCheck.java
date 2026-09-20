package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartSubject;

import java.awt.Component;
import java.awt.Container;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

/**
 * Whose houses the wheel is read against, and the control that says so.
 *
 * <p><b>What this exists for.</b> A synastry has two house frames and the wheel can only be read
 * in one of them at a time. The selector offered "Natal" and "Transit" - the engine's names for
 * its two rings - which in a synastry describe neither person, and it offered no way to use the
 * sky's own houses at all. It also lived in the settings sidebar, where a reader looking at the
 * wheel would not find it. David, 2026-09-20: "for synastry charts possibly down near the track
 * buttons have a selector to align to either Chart A or Chart B or Transit houses. Only for
 * synastry though."
 *
 * <p>So: the three frames are reachable by name, each one really moves the cusps the wheel and
 * the globe draw, a frame whose chart is absent falls back rather than casting from nothing, and
 * the control sits on the scrub row beside the transport and shows only when there are two
 * charts to choose between.
 */
public final class HouseAlignCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        Settings.set("home.location", "39.95, -75.17");
        OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        Thread.sleep(2500);
        SkymapPanel p = (SkymapPanel) field(hold[0], OuraniaWindow.class, "skymapPanel");
        @SuppressWarnings("unchecked")
        JComboBox<String> align = (JComboBox<String>) field(p, SkymapPanel.class, "alignCombo");
        Component label = (Component) field(p, SkymapPanel.class, "alignLabel");
        Container scrubRow = (Container) field(p, SkymapPanel.class, "scrubRow");

        // ---- the control, where it is and what it offers
        List<String> items = new ArrayList<>();
        for (int i = 0; i < align.getItemCount(); i++) {
            items.add(String.valueOf(align.getItemAt(i)));
        }
        ok("the frames are offered by the reader's names for them: " + items,
            items.equals(List.of("Chart A", "Chart B", "Sky", "Both")));
        ok("neither engine name is offered any more",
            !items.contains("Natal") && !items.contains("Transit"));
        ok("it sits on the scrub row, beside the transport", align.getParent() == scrubRow);
        ok("and so does its caption", label.getParent() == scrubRow);
        Container chartControls = (Container) field(p, SkymapPanel.class, "chartControls");
        ok("and not also in the settings strip, which would be two of one control",
            !contains(chartControls, align));

        // ---- only where there is a choice
        SwingUtilities.invokeAndWait(() -> {
            p.setComposition(true, false);
            p.updateChartData();
        });
        Thread.sleep(600);
        ok("with one chart on the wheel the selector is hidden", !align.isVisible());
        ok("and its caption with it", !label.isVisible());

        ZonedDateTime when = ZonedDateTime.now();
        ChartSubject a = ChartSubject.of("Chart A", when.minusYears(41), "Philadelphia",
            39.95, -75.17, "America/New_York", false);
        ChartSubject b = ChartSubject.of("Chart B", when.minusYears(38), "London",
            51.5, -0.12, "Europe/London", false);
        // <b>Synastry is a mode, not a composition.</b> setComposition says who is on the
        // wheel; the mode says what the wheel is. The first cut of this check set only the
        // composition, and the selector stayed hidden because isSynastryChart() was still
        // false - the check was wrong, not the panel.
        SwingUtilities.invokeAndWait(() -> {
            p.installSubjects(a, b, ChartSubject.empty("Sky").atNow());
            p.applyChartMode(ChartMode.SYNASTRY, true, true, true);
            p.updateChartData();
        });
        Thread.sleep(900);
        ok("the panel is in synastry, so the premise of what follows holds",
            p.isSynastryChart() && p.showTransitChart);
        ok("with two charts it appears", align.isVisible() && label.isVisible());

        // ---- each frame really moves the cusps
        double[] chartA = pick(p, align, "Chart A");
        double[] chartB = pick(p, align, "Chart B");
        double[] sky = pick(p, align, "Sky");
        ok("Chart A's frame is Chart A's cusps", same(chartA, cusps(p, "natalRing")));
        ok("Chart B's frame is Chart B's cusps", same(chartB, cusps(p, "outerRing")));
        ok("the sky's frame is the sky's cusps", same(sky, cusps(p, "skyRing")));
        ok("and the three are not one frame under three names",
            !same(chartA, chartB) && !same(chartB, sky) && !same(chartA, sky));

        pick(p, align, "Chart B");
        ok("the Ascendant follows the frame",
            Math.abs(ascendant(p) - ringAscendant(p, "outerRing")) < 1e-9);
        pick(p, align, "Chart A");
        ok("and back again",
            Math.abs(ascendant(p) - ringAscendant(p, "natalRing")) < 1e-9);

        // ---- and the globe reads the same frame the wheel does
        pick(p, align, "Sky");
        ok("the globe's houses are the same cusps the wheel is using",
            same(p.activeCusps, cusps(p, "skyRing")));

        // ---- a frame whose chart is not there falls back rather than casting from nothing
        pick(p, align, "Chart B");
        SwingUtilities.invokeAndWait(() -> {
            p.setComposition(true, false);
            p.updateChartData();
        });
        Thread.sleep(700);
        ok("Chart B switched off, its frame falls back to Chart A rather than to nothing",
            same(p.activeCusps, cusps(p, "natalRing")));
        ok("and no cusp is left at zero", nonZero(p.activeCusps));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
        }
        System.exit(1);
    }

    /** Chooses a frame through the control the reader uses, and returns the cusps it produced. */
    private static double[] pick(SkymapPanel p, JComboBox<String> align, String frame)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> align.setSelectedItem(frame));
        Thread.sleep(500);
        return p.activeCusps.clone();
    }

    private static double[] cusps(SkymapPanel p, String ring) throws Exception {
        Object r = field(p, SkymapPanel.class, ring);
        return (double[]) field(r, r.getClass(), "cusps");
    }

    private static double ringAscendant(SkymapPanel p, String ring) throws Exception {
        Object r = field(p, SkymapPanel.class, ring);
        return (Double) field(r, r.getClass(), "ascendant");
    }

    private static double ascendant(SkymapPanel p) throws Exception {
        return (Double) field(p, SkymapPanel.class, "activeAscendant");
    }

    private static boolean same(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length) {
            return false;
        }
        for (int i = 1; i < x.length; i++) {
            if (Math.abs(x[i] - y[i]) > 1e-9) {
                return false;
            }
        }
        return true;
    }

    private static boolean nonZero(double[] cusps) {
        for (int i = 1; i <= 12; i++) {
            if (cusps[i] != 0.0) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(Container root, Component wanted) {
        if (root == null) {
            return false;
        }
        for (Component c : root.getComponents()) {
            if (c == wanted || (c instanceof Container && contains((Container) c, wanted))) {
                return true;
            }
        }
        return false;
    }

    private static Object field(Object on, Class<?> type, String name) throws Exception {
        java.lang.reflect.Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(on);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
