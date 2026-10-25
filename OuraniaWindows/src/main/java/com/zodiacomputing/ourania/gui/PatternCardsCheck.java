package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AspectPatterns;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Gestalt;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Patterns tab's figures as plain cards (reading framework spec, 2026-10-24).
 *
 * <b>Most charts have no figure,</b> so a fixed run of dates is scanned for charts that do, and
 * the cards are checked on those: one card for every figure the engine found, each carrying
 * that figure's members, and none of the figure's own name in the plain view. The run is dates
 * and not the clock, for the reason NavigationCheck learned the hard way.
 */
public final class PatternCardsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** One chart every 11 days for about 18 years: enough to meet most of the figures. */
    private static final int SCAN = 600;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        Set<String> kinds = new LinkedHashSet<>();
        int withFigures = 0;
        int withStelliums = 0;
        int empties = 0;
        double start = new SweDate(1960, 1, 1, 12.0).getJulDay();
        for (int i = 0; i < SCAN; i++) {
            ChartFrame f = ChartFrame.compute(sw, start + 11.0 * i, 40.71, -74.01, 'P', false, 0.0);
            Gestalt.Result g = Gestalt.compute(f);
            // A chart with neither a closed figure nor a stellium has nothing to say here.
            if (g.aspectPatterns.isEmpty() && g.stelliumMembers.isEmpty()) {
                empties++;
                if (empties <= 3) {
                    empty(g);
                }
                continue;
            }
            if (!g.aspectPatterns.isEmpty()) {
                withFigures++;
            }
            if (!g.stelliumMembers.isEmpty()) {
                withStelliums++;
                stelliums(g, i);
            }
            for (AspectPatterns.Pattern ap : g.aspectPatterns) {
                kinds.add(ap.name);
            }
            figures(g, i);
        }
        System.out.println("scanned " + SCAN + " charts: " + withFigures + " with figures, "
            + withStelliums + " with stelliums, kinds " + kinds);
        yes("the scan met charts with stelliums (" + withStelliums + ")", withStelliums >= 10);
        yes("the scan met charts with figures (" + withFigures + ")", withFigures >= 20);
        yes("and more than one kind of figure (" + kinds + ")", kinds.size() >= 3);
        yes("and charts without any (" + empties + ")", empties >= 1);
        report();
    }

    private static void figures(Gestalt.Result g, int n) {
        String at = " (chart " + n + ")";
        List<ReadingCard> cards = PatternCards.cards(g);
        yes("one card for every figure" + at + ": " + cards.size() + " of "
            + g.aspectPatterns.size(), cards.size() == g.aspectPatterns.size());
        for (int i = 0; i < Math.min(cards.size(), g.aspectPatterns.size()); i++) {
            AspectPatterns.Pattern ap = g.aspectPatterns.get(i);
            ReadingCard c = cards.get(i);
            yes("the card is in the engine's order" + at + ": " + ap.name,
                c.why().get(0).startsWith(ap.name));
            String members = "Members: " + String.join(", ", ap.bodies);
            yes("it names every member" + at + ": " + members, c.why().contains(members));
            yes("a figure with a focal point says so" + at,
                (ap.apex == null) == c.why().stream().noneMatch(w -> w.startsWith("Focal point")));
            yes("the label is the plain one" + at, PatternCards.LABEL.equals(c.label()));
            yes("no banned word in the label, headline or body" + at + ": " + c.headline(),
                JargonCheck.banned(c.label() + " " + c.headline() + " " + c.body()).isEmpty());
            yes("the headline is a sentence" + at,
                c.headline() != null && c.headline().length() > 15);
            yes("the whole reading is kept for Astrologer view" + at,
                c.why().stream().anyMatch(w -> w.startsWith("In full: ")));
        }
        // Every closed figure keeps the link to its whole reading, which was on the Placements
        // page's banner before the banner moved here.
        List<String> links = PatternCards.figureLinks(g);
        yes("one link for every figure card" + at, links.size() == cards.size());
        for (int i = 0; i < Math.min(links.size(), g.aspectPatterns.size()); i++) {
            AspectPatterns.Pattern ap = g.aspectPatterns.get(i);
            String[] parsed = InterpretationPanel.parsePatternHref(links.get(i));
            yes("the link opens the right figure" + at + ": " + ap.name,
                links.get(i).equals(InterpretationPanel.patternHref(ap.name, ap.bodies))
                    && parsed != null && parsed[0].equals(ap.name));
        }
        String withLinks = PatternCards.html(g, false);
        yes("every link is on the page, once" + at,
            links.stream().allMatch(l -> withLinks.contains("href='" + l + "'")));
        yes("and says what it opens" + at, count(withLinks, "The figure in full") == links.size());
        String plain = PatternCards.html(g, false);
        String astro = PatternCards.html(g, true);
        yes("plain view hides the astrology" + at, !plain.contains("Why &mdash;")
            && !plain.contains("Members:"));
        yes("Astrologer view shows it" + at, astro.contains("Members:"));
        int all = PatternCards.allCards(g).size();
        yes("plain view says how many" + at,
            plain.contains(all + (all == 1 ? " group of planets works"
                : " groups of planets work")));
        // The whole plain page, section heading and counts included, against David's list.
        String words = plain.replaceAll("<[^>]+>", " ");
        yes("the plain page has no banned word" + at + ": " + JargonCheck.banned(words),
            JargonCheck.banned(words).isEmpty());
    }

    /** One card for each sign holding three or more planets, each naming its members. */
    private static void stelliums(Gestalt.Result g, int n) {
        String at = " (chart " + n + ")";
        List<ReadingCard> cards = PatternCards.stelliumCards(g);
        yes("one stellium card for every sign that holds three" + at + ": " + cards.size() + " of "
            + g.stelliumMembers.size(), cards.size() == g.stelliumMembers.size());
        int i = 0;
        for (java.util.Map.Entry<String, List<String>> e : g.stelliumMembers.entrySet()) {
            if (i >= cards.size()) {
                break;
            }
            ReadingCard c = cards.get(i++);
            yes("the card is in the engine's order and sign" + at + ": " + e.getKey(),
                c.why().get(0).equalsIgnoreCase("Stellium in " + e.getKey()));
            yes("it names every member" + at + ": " + e.getValue(),
                c.why().contains("Members: " + String.join(", ", e.getValue())));
            yes("at least three planets" + at, e.getValue().size() >= 3);
            yes("the label is the plain one" + at,
                PatternCards.STELLIUM_LABEL.equals(c.label()));
            yes("no banned word in the label, headline or body" + at + ": " + c.headline(),
                JargonCheck.banned(c.label() + " " + c.headline() + " " + c.body()).isEmpty());
        }
        String plain = PatternCards.html(g, false).replaceAll("<[^>]+>", " ");
        yes("the plain section with stelliums has no banned word" + at,
            JargonCheck.banned(plain).isEmpty());
    }

    private static void empty(Gestalt.Result g) {
        String plain = PatternCards.html(g, false);
        yes("no figure is said, not left out", plain.contains("No group of planets forms a closed figure"));
        yes("and no cards", PatternCards.cards(g).isEmpty());
        yes("a missing chart is the same sentence, not a crash",
            PatternCards.html(null, false).contains("No group of planets forms a closed figure"));
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
