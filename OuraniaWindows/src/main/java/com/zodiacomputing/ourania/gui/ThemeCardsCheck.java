package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The Synthesize page (ThemeCards, 2026-10-20).
 *
 * <b>The claim worth checking is that no theme reaches a reader as an engine word.</b> L7 names
 * themes with adjectives meant for counting; the page has a plain sentence for each. A word L7
 * learns later and the page does not know would fall to the default card - honest, but saying
 * nothing - so the sweep runs the real engine over many charts and lists every word it meets
 * that has no plain entry. Then: only themes at the threshold are cards, and the plain text
 * carries none of JargonCheck's banned words.
 */
public final class ThemeCardsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        Settings.useScratchFile();
        System.out.println("Running com.zodiacomputing.ourania.gui.ThemeCardsCheck");

        SwissEph sw = new SwissEph(Ephemeris.PATH);
        Random rnd = new Random(20261020L);
        Set<String> unknown = new TreeSet<>();
        Set<String> met = new TreeSet<>();
        List<String> belowThreshold = new ArrayList<>();
        List<String> jargon = new ArrayList<>();
        int withCards = 0;
        int charts = 200;
        Pattern banned = Pattern.compile("\\b(" + String.join("|", JargonCheck.BANNED) + ")\\b",
            Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < charts; i++) {
            double jd = SweDate.getJulDay(1940 + rnd.nextInt(70), 1 + rnd.nextInt(12),
                1 + rnd.nextInt(28), rnd.nextDouble() * 24.0);
            ChartFrame f = ChartFrame.compute(sw, jd, -50 + rnd.nextDouble() * 110,
                -180 + rnd.nextDouble() * 360, 'P', false, 0.0);
            Gestalt.Result g = Gestalt.compute(f);
            List<BodyScore.Vector> ranked = BodyScore.rank(f, g);
            Themes.Result t = Themes.extract(f, g, ranked, Topics.analyse(f, ranked));
            for (Themes.Signature s : Themes.signaturesAtThreshold(t)) {
                met.add(s.theme);
                if (!ThemeCards.PLAIN.containsKey(s.theme)) {
                    unknown.add(s.theme);
                }
            }
            for (Themes.Contradiction c : t.contradictions) {
                for (String w : new String[] {c.themeA, c.themeB}) {
                    met.add(w);
                    if (!ThemeCards.PLAIN.containsKey(w)) {
                        unknown.add(w);
                    }
                }
            }
            List<ReadingCard> cards = ThemeCards.cards(t);
            withCards += cards.isEmpty() ? 0 : 1;
            int sigs = Themes.signaturesAtThreshold(t).size();
            if (cards.size() != sigs + t.contradictions.size()) {
                belowThreshold.add("chart " + i + ": " + cards.size() + " cards for " + sigs
                    + " themes and " + t.contradictions.size() + " tensions");
            }
            for (ReadingCard c : cards) {
                String plain = c.headline() + " " + c.body();
                if (banned.matcher(plain).find()) {
                    jargon.add(plain);
                }
            }
        }
        yes("the sweep met real themes, " + met.size() + " words: " + met, met.size() >= 10);
        yes("most charts have something to say, " + withCards + " of " + charts,
            withCards > charts / 2);
        eq("every theme the engine names has plain words", "[]", unknown.toString());
        eq("one card per theme at the threshold and per tension", "[]",
            belowThreshold.toString());
        eq("and no plain card uses a banned word", "[]", jargon.toString());

        // The page itself.
        String empty = ThemeCards.html(null, false);
        yes("with nothing to read the page says so", empty.contains("No theme repeats"));
        yes("and names itself Synthesize", empty.contains(">Synthesize</h2>"));

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
    }

    private static void yes(String claim, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(claim);
            System.out.println("  FAIL: " + claim);
        } else {
            System.out.println("  ok: " + claim);
        }
    }

    private static void eq(String claim, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(claim + " (expected <" + expected + ">, got <" + actual + ">)");
            System.out.println("  FAIL: " + claim + " (expected <" + expected + ">, got <"
                + actual + ">)");
        } else {
            System.out.println("  ok: " + claim);
        }
    }
}
