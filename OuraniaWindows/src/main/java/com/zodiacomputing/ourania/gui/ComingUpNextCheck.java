package com.zodiacomputing.ourania.gui;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * "Coming up next" on the Transits At a glance (reading framework spec, Mode 3).
 *
 * <b>The rules are asked of made-up timelines,</b> where the answer is known by construction: a
 * contact in the past, one beyond a fortnight, one with no moment, and several inside it that must
 * come back soonest first and no more than three. The window part asks only that the section is
 * on the page the Transits tab opens.
 */
public final class ComingUpNextCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    private static final double NOW = 2461000.25;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        System.out.println("=== Part A: which contacts, and in what order ===");
        selection();
        System.out.println("=== Part B: the date, and the empty news ===");
        wording();
        System.out.println("=== Part C: it is on the Transits glance ===");
        window();
        report();
    }

    private static NowTimeline.Entry at(double jd, String headline) {
        return new NowTimeline.Entry(null, jd, NowTimeline.Window.THIS_WEEK,
            new ReadingCard("A LABEL", headline, "The long body.", List.of("a why line")));
    }

    private static void selection() {
        List<NowTimeline.Entry> all = List.of(
            at(NOW + 20, "too far"), at(NOW - 1, "past"), at(Double.NaN, "no moment"),
            at(NOW + 5, "fifth day"), at(NOW + 1, "first day"), at(NOW + 13.9, "late"),
            at(NOW, "this very moment"), at(NOW + 14.0, "on the line"),
            at(NOW + 14.0001, "just over"));
        List<ReadingCard> cards = ComingUpNext.cards(all, NOW);
        yes("at most three: " + cards.size(), cards.size() == ComingUpNext.LIMIT);
        yes("soonest first", cards.get(0).headline().equals("this very moment")
            && cards.get(1).headline().equals("first day")
            && cards.get(2).headline().equals("fifth day"));
        List<NowTimeline.Entry> few = List.of(at(NOW + 14.0, "on the line"),
            at(NOW + 14.0001, "just over"), at(NOW - 0.001, "just past"));
        List<ReadingCard> onLine = ComingUpNext.cards(few, NOW);
        yes("a contact exactly a fortnight ahead counts", onLine.size() == 1
            && onLine.get(0).headline().equals("on the line"));
        yes("one just past, or just over, does not", onLine.stream().noneMatch(
            c -> c.headline().equals("just over") || c.headline().equals("just past")));
        yes("nothing in, nothing out", ComingUpNext.cards(List.of(), NOW).isEmpty());
        yes("nothing in range, nothing out", ComingUpNext.cards(
            List.of(at(NOW + 30, "far"), at(NOW - 3, "gone")), NOW).isEmpty());
        ReadingCard c = ComingUpNext.cards(List.of(at(NOW + 2, "the headline")), NOW).get(0);
        yes("the card keeps the contact's own headline", c.headline().equals("the headline"));
        yes("and is cut to the headline: no body, so a glance stays a glance", c.body().isEmpty());
        yes("and keeps its why lines for Astrologer view", c.why().equals(List.of("a why line")));
        yes("the label says what it is", c.label().startsWith("COMING UP NEXT · "));
        yes("the label is banned-word free: " + c.label(),
            JargonCheck.banned(c.label()).isEmpty());
    }

    private static void wording() {
        // 2026-10-29 is a Thursday. Noon UTC is the same calendar day in nearly every zone.
        double noon = 2440587.5 + java.time.LocalDate.of(2026, 10, 29).toEpochDay() + 0.5;
        String label = ComingUpNext.dateLabel(noon);
        yes("a date reads weekday, day, month: " + label,
            label.matches("[A-Z][a-z]{2} \\d{1,2} [A-Z][a-z]{2}"));
        yes("and it is the 28th, 29th or 30th of October (zone-safe): " + label,
            label.endsWith("Oct") && label.matches(".* (28|29|30) Oct"));
        yes("a later day gives a later date", !ComingUpNext.dateLabel(noon + 3).equals(label));

        String none = ComingUpNext.html("Dana", List.of(), false);
        yes("no contacts: the section says so", none.contains("Nothing turns exact on this chart"));
        yes("and still has its heading", none.contains("Coming up next on"));
        String some = ComingUpNext.html("Dana", ComingUpNext.cards(
            List.of(at(NOW + 1, "something")), NOW), false);
        yes("with contacts: the cards are there", some.contains("something")
            && !some.contains("Nothing turns exact"));
        yes("plain view has no why line", !some.contains("Why &mdash;"));
        yes("Astrologer view has it", ComingUpNext.html("Dana", ComingUpNext.cards(
            List.of(at(NOW + 1, "something")), NOW), true).contains("a why line"));
        String words = (none + some).replaceAll("<[^>]+>", " ");
        yes("no banned word in the section: " + JargonCheck.banned(words),
            JargonCheck.banned(words).isEmpty());
    }

    private static void window() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            long end = System.nanoTime() + 20_000_000_000L;
            while (sky.radixChart() == null && System.nanoTime() < end) {
                Thread.sleep(50);
            }
            int casts = sky.castsApplied();
            SwingUtilities.invokeAndWait(() -> sky.applyChartSettings("1990-06-15", "12:00",
                "51.5074, -0.1278", ChartMode.SINGLE, "", "", "", false));
            long cast = System.nanoTime() + 25_000_000_000L;
            while ((sky.castsApplied() <= casts || !sky.innerIsBirthChart())
                    && System.nanoTime() < cast) {
                Thread.sleep(50);
            }
            SwingUtilities.invokeAndWait(() -> {
                w.chartRail().choose(ChartTab.TRANSITS.door);
                w.setPageEnabled(OuraniaWindow.SKY_GLANCE_PAGE, true);
                w.revealPage(OuraniaWindow.SKY_GLANCE_PAGE);
                w.refreshSkyPages();
            });
            SwingUtilities.invokeAndWait(() -> { });
            yes("the Transits glance is in front", OuraniaWindow.SKY_GLANCE_PAGE.equals(
                w.currentPage()));
            JEditorPane pane = w.paneOf(OuraniaWindow.SKY_GLANCE_PAGE);
            final String[] text = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    javax.swing.text.Document d = pane.getDocument();
                    text[0] = d.getText(0, d.getLength());
                } catch (javax.swing.text.BadLocationException e) {
                    text[0] = "";
                }
            });
            yes("it carries the Coming up next section", text[0].contains("Coming up next on"));
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }
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
