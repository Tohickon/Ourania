package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * A person's Patterns page as one set of modern cards (reading framework spec, Phase 2A).
 *
 * <b>The page is checked against the engine's own findings,</b> not against its own list: the
 * figures and stelliums on the page are counted and compared with {@code Gestalt}'s, for several
 * fixed birth charts. And it is checked for what the old page was: no "Show the detail" link
 * and no fold-open state, since Astrologer view opens every why line, and no day-or-night card,
 * which is the badge on At a glance.
 */
public final class PatternsPageCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** Birth moments with place; spread so some have figures and some stelliums. */
    private static final String[][] CHARTS = {
        {"1984-05-17", "06:00", "40.71, -74.01"},
        {"1961-11-02", "21:00", "51.5074, -0.1278"},
        {"2003-02-09", "14:00", "34.05, -118.24"},
        {"1976-08-30", "03:00", "48.85, 2.35"},
        {"1990-06-15", "12:00", "51.5074, -0.1278"},
    };

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        boolean was = Settings.astrologerView();
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            long end = System.nanoTime() + 20_000_000_000L;
            while (sky.radixChart() == null && System.nanoTime() < end) {
                Thread.sleep(50);
            }
            int withFigures = 0;
            int withStelliums = 0;
            for (String[] c : CHARTS) {
                int casts = sky.castsApplied();
                SwingUtilities.invokeAndWait(() -> sky.applyChartSettings(c[0], c[1], c[2],
                    ChartMode.SINGLE, "", "", "", false));
                long cast = System.nanoTime() + 20_000_000_000L;
                while ((sky.castsApplied() <= casts || !sky.innerIsBirthChart())
                        && System.nanoTime() < cast) {
                    Thread.sleep(50);
                }
                SwingUtilities.invokeAndWait(() -> w.chartRail().choose(ChartTab.CHART_A.door));
                ChartFrame f = sky.getCurrentChart();
                yes(c[0] + ": a chart was cast", f != null && sky.innerIsBirthChart());
                if (f == null) {
                    continue;
                }
                Gestalt.Result g = Gestalt.compute(f);
                withFigures += g.aspectPatterns.isEmpty() ? 0 : 1;
                withStelliums += g.stelliumMembers.isEmpty() ? 0 : 1;
                Settings.setAstrologerView(false);
                String plain = page(w);
                Settings.setAstrologerView(true);
                String astro = page(w);
                check(c[0], g, plain, astro);
                clickThrough(w, c[0], g);
            }
            System.out.println("charts with figures: " + withFigures + ", with stelliums: "
                + withStelliums);
            yes("the fixed charts include one with a closed figure, so the click is asked ("
                + withFigures + ")", withFigures >= 1);
        } finally {
            Settings.setAstrologerView(was);
            SwingUtilities.invokeAndWait(w::dispose);
        }
        report();
    }

    private static String page(OuraniaWindow w) throws Exception {
        final String[] out = new String[1];
        SwingUtilities.invokeAndWait(() -> out[0] = w.overviewPanel().html());
        return out[0];
    }

    private static void check(String at, Gestalt.Result g, String plain, String astro) {
        yes(at + ": it is the Patterns page", plain.contains("<h2>Patterns</h2>"));
        yes(at + ": there is no fold-open link left",
            !plain.contains("toggle:") && !plain.contains("Show the detail")
                && !astro.contains("toggle:") && !astro.contains("Hide the detail"));
        yes(at + ": the day-or-night card is on At a glance, not here",
            !plain.contains("WHO HAS THE EASY RUN"));
        int shape = plain.indexOf(">Chart shape</h3>");
        int figures = plain.indexOf("Patterns that tie your chart together");
        int also = plain.indexOf("Also in this chart");
        yes(at + ": the figures section is on the page", figures > 0);
        yes(at + ": the shape comes before the figures", shape < 0 || shape < figures);
        yes(at + ": the rest comes after the figures", also < 0 || also > figures);

        yes(at + ": one figure card for each closed figure the engine found ("
            + g.aspectPatterns.size() + ")",
            count(plain, PatternCards.LABEL) == g.aspectPatterns.size());
        yes(at + ": one stellium card for each sign with three planets ("
            + g.stelliumMembers.size() + ")",
            count(plain, PatternCards.STELLIUM_LABEL) == g.stelliumMembers.size());
        for (java.util.Map.Entry<String, List<String>> e : g.stelliumMembers.entrySet()) {
            yes(at + ": the stellium in " + e.getKey() + " names its members in Astrologer view",
                astro.contains("Members: " + String.join(", ", e.getValue())));
        }

        // Plain: David's list, on everything a reader sees (the why lines are not shown).
        String words = plain.replaceAll("<[^>]+>", " ");
        yes(at + ": plain view has no banned word: " + JargonCheck.banned(words),
            JargonCheck.banned(words).isEmpty());
        yes(at + ": plain view shows no why line",
            !plain.contains("Why &mdash;") && !plain.contains("Classical figure"));
        yes(at + ": Astrologer view shows the classical figure", astro.contains("Classical figure"));
    }

    /**
     * "The figure in full" opens the figure's reading in the Selected drawer. The link was on the
     * Placements page's banner; asked here, in a real window, because the click is what was at
     * risk when the banner moved.
     */
    private static void clickThrough(OuraniaWindow w, String at, Gestalt.Result g)
            throws Exception {
        for (String link : PatternCards.figureLinks(g)) {
            String[] parsed = InterpretationPanel.parsePatternHref(link);
            yes(at + ": the link parses as a figure", parsed != null);
            if (parsed == null) {
                continue;
            }
            SwingUtilities.invokeAndWait(() -> w.handlePlacementClick(link));
            String drawer = selected(w);
            yes(at + ": clicking " + parsed[0] + " fills the Selected drawer", drawer.length() > 40);
            yes(at + ": and it is about that figure", drawer.toLowerCase().contains(
                parsed[0].toLowerCase()));
        }
    }

    private static String selected(OuraniaWindow w) throws Exception {
        javax.swing.JEditorPane pane = (javax.swing.JEditorPane) CheckReflect.get(w,
            "selectionPane");
        final String[] out = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                javax.swing.text.Document doc = pane.getDocument();
                out[0] = doc.getText(0, doc.getLength());
            } catch (javax.swing.text.BadLocationException e) {
                out[0] = "";
            }
        });
        return out[0];
    }

    private static int count(String s, String sub) {
        int n = 0;
        for (int at = s.indexOf(sub); at >= 0; at = s.indexOf(sub, at + sub.length())) {
            n++;
        }
        return n;
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (int i = 0; i < Math.min(40, failures.size()); i++) {
            System.out.println("  " + failures.get(i));
        }
        System.exit(1);
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }
}
