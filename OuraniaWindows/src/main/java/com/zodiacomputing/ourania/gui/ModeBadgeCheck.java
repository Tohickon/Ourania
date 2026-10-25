package com.zodiacomputing.ourania.gui;

import javax.swing.SwingUtilities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The mode pill and the charts line above the four tabs (reading framework spec, 2026-10-24).
 *
 * <b>The rules are asked directly, every tab against every combination,</b> so a tab that lost
 * its mode, or a mode no tab can reach, fails here by name. The window part asks only what the
 * pure rules cannot: that the badge is on screen, over the strip.
 */
public final class ModeBadgeCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        System.out.println("=== Part A: every tab has a mode ===");
        List<String> seen = new ArrayList<>();
        for (ChartTab tab : ChartTab.values()) {
            for (boolean sky : new boolean[] {false, true}) {
                for (boolean a : new boolean[] {false, true}) {
                    for (boolean b : new boolean[] {false, true}) {
                        String mode = ModeBadge.modeOf(tab, sky, a, b);
                        yes(tab + " sky=" + sky + " a=" + a + " b=" + b + " has a mode",
                            mode != null && !mode.isEmpty());
                        if (!seen.contains(mode)) {
                            seen.add(mode);
                        }
                    }
                }
            }
        }
        yes("the seven modes of the spec are all reachable: " + seen, seen.containsAll(List.of(
            ModeBadge.NATAL, ModeBadge.SKY, ModeBadge.TRANSITS, ModeBadge.SYNASTRY,
            ModeBadge.COMPOSITE, ModeBadge.TRANSITING_SYNASTRY,
            ModeBadge.TRANSITING_COMPOSITE)) && seen.size() == 7);

        eq("a person's tab is natal", ModeBadge.NATAL,
            ModeBadge.modeOf(ChartTab.CHART_A, true, true, true));
        eq("so is the other person's", ModeBadge.NATAL,
            ModeBadge.modeOf(ChartTab.CHART_B, false, false, false));
        eq("the sky tab is the sky", ModeBadge.SKY,
            ModeBadge.modeOf(ChartTab.SKY, false, true, true));
        eq("transits on one person are transits", ModeBadge.TRANSITS,
            ModeBadge.modeOf(ChartTab.TRANSITS, true, true, false));
        eq("transits on two people are the pair's transits", ModeBadge.TRANSITING_SYNASTRY,
            ModeBadge.modeOf(ChartTab.TRANSITS, true, true, true));
        eq("a pair without the sky is synastry", ModeBadge.SYNASTRY,
            ModeBadge.modeOf(ChartTab.SYNASTRY, false, true, true));
        eq("a pair under the sky is transiting synastry", ModeBadge.TRANSITING_SYNASTRY,
            ModeBadge.modeOf(ChartTab.SYNASTRY, true, true, true));
        eq("a composite without the sky is a composite", ModeBadge.COMPOSITE,
            ModeBadge.modeOf(ChartTab.COMPOSITE, false, true, true));
        eq("a composite under the sky is transiting", ModeBadge.TRANSITING_COMPOSITE,
            ModeBadge.modeOf(ChartTab.COMPOSITE, true, true, true));

        System.out.println("=== Part B: the line names the charts ===");
        ZonedDateTime at = ZonedDateTime.of(2026, 10, 24, 9, 5, 0, 0, ZoneId.of("UTC"));
        eq("Chart A: name and place", "David · Los Angeles, CA",
            ModeBadge.contextOf(ChartTab.CHART_A, "David", "Los Angeles, CA", "Erika", "Bonn",
                ChartMode.SINGLE, at));
        eq("Chart A with no place is just the name", "David",
            ModeBadge.contextOf(ChartTab.CHART_A, "David", null, "Erika", "Bonn",
                ChartMode.SINGLE, at));
        eq("a blank place is no place", "David",
            ModeBadge.contextOf(ChartTab.CHART_A, "David", "  ", "Erika", "Bonn",
                ChartMode.SINGLE, at));
        eq("Chart B names the other person", "Erika · Bonn",
            ModeBadge.contextOf(ChartTab.CHART_B, "David", "LA", "Erika", "Bonn",
                ChartMode.SINGLE, at));
        eq("the sky says when", "The sky · 24 Oct 2026, 09:05",
            ModeBadge.contextOf(ChartTab.SKY, "David", "LA", "Erika", "Bonn",
                ChartMode.SINGLE, at));
        eq("the sky with no moment still reads", "The sky",
            ModeBadge.contextOf(ChartTab.SKY, "David", null, null, null, ChartMode.SINGLE, null));
        eq("transits on one person", "The current sky on David",
            ModeBadge.contextOf(ChartTab.TRANSITS, "David", null, null, null,
                ChartMode.TRANSIT, at));
        eq("transits on two", "The current sky on David and Erika",
            ModeBadge.contextOf(ChartTab.TRANSITS, "David", null, "Erika", null,
                ChartMode.TRANSIT, at));
        eq("a pair", "David & Erika",
            ModeBadge.contextOf(ChartTab.SYNASTRY, "David", null, "Erika", null,
                ChartMode.SYNASTRY, at));
        eq("a midpoint composite", "David & Erika · Midpoint",
            ModeBadge.contextOf(ChartTab.COMPOSITE, "David", null, "Erika", null,
                ChartMode.COMPOSITE_MIDPOINT, at));
        eq("a Davison composite", "David & Erika · Davison",
            ModeBadge.contextOf(ChartTab.COMPOSITE, "David", null, "Erika", null,
                ChartMode.COMPOSITE_DAVISON, at));

        System.out.println("=== Part C: it is on screen, over the strip ===");
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            ModeBadge badge = w.modeBadge();
            yes("the window has a badge", badge != null);
            if (badge != null) {
                yes("it opens on the sky, which is the first tab", ModeBadge.SKY.equals(badge.mode()));
                yes("it sits over the tabs, in the same column",
                    badge.getParent() != null && badge.getParent() == w.pagesRail().getParent());
                yes("and the column is on screen under a chart tab",
                    badge.getParent().getParent() != null);
                yes("the line is not empty", !badge.line().trim().isEmpty());
            }
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }
        report();
    }

    private static void report() {
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
