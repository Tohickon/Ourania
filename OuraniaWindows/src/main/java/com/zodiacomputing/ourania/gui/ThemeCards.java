package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Themes;

import java.util.ArrayList;
import java.util.List;

/**
 * The Synthesize page: the chart's own themes, each one said by at least three independent
 * parts of the chart (2026-10-20).
 *
 * <b>Why this page exists apart from Your year.</b> The tab spec David adopted on 2026-10-20 puts
 * Synthesize under Interpretation - "only major themes supported by at least three independent
 * chart configurations" - and Your year under Prediction. The app had one page doing the year's
 * job under the Synthesize key. The natal half already existed: {@link Themes} (L7) counts
 * witnesses with an independence rule and has a threshold of three, but its signatures reached
 * a reader only as one sentence in At a glance and a block of the text report.
 *
 * <b>The theme words are the engine's, the sentences are the data file's.</b> L7 names a theme
 * with an adjective ("fiery", "saturnian", "vocational"), which is a label for counting and not
 * a thing to say. Each word has a headline, a body and a takeaway in synthesize_cards.json; the
 * witnesses themselves, which are the engine's own technical lines, go in the "why" box that
 * Astrologer view opens.
 */
final class ThemeCards {

    private ThemeCards() { }

    /**
     * A theme word, said plainly: the headline, what it is like in an ordinary week, and the
     * takeaway. From {@code synthesize_cards.json} (2026-10-20), in the voice David set: seven
     * themes and two tensions are his wording, the rest were written to match.
     */
    record Plain(String headline, String body, String takeaway) { }

    /** The plain words for a key ("watery", "fiery_vs_watery"), or null when none are written. */
    static Plain plain(String key) {
        InterpretationService svc = InterpretationService.getInstance();
        String h = svc.getSynthesizePart(key + ".headline");
        String b = svc.getSynthesizePart(key + ".body");
        String t = svc.getSynthesizePart(key + ".takeaway");
        return h == null || b == null || t == null ? null : new Plain(h, b, t);
    }

    /** A tension's words, whichever way round the engine names the pair. */
    static Plain tension(String a, String b) {
        Plain p = plain(a + "_vs_" + b);
        return p != null ? p : plain(b + "_vs_" + a);
    }

    /** What a card says when the words for a theme are missing: honest, and plain. */
    private static final Plain MISSING = new Plain("A repeated note in your chart.",
        "Several separate parts of your chart say the same thing here.",
        "Read the parts listed under the astrology to see what they share.");

    /** The cards: each theme at the threshold, strongest first, then each tension. */
    static List<ReadingCard> cards(Themes.Result t) {
        List<ReadingCard> out = new ArrayList<>();
        if (t == null) {
            return out;
        }
        for (Themes.Signature s : Themes.signaturesAtThreshold(t)) {
            Plain p = plain(s.theme);
            if (p == null) {
                p = MISSING;
            }
            List<String> why = new ArrayList<>();
            why.add(s.witnessCount + " independent witnesses for \"" + s.theme + "\"");
            for (List<Themes.Candidate> comp : s.components) {
                why.add(comp.get(0).text);
            }
            out.add(card("[ " + s.witnessCount + " PARTS AGREE ]", p, why));
        }
        for (Themes.Contradiction c : t.contradictions) {
            Plain p = tension(c.themeA, c.themeB);
            if (p == null) {
                p = MISSING;
            }
            out.add(card("[ A PULL IN TWO DIRECTIONS ]", p,
                List.of(c.themeA + " (" + c.witnessesA + ") against " + c.themeB
                    + " (" + c.witnessesB + ")", c.strongestA.text, c.strongestB.text)));
        }
        return out;
    }

    /** The takeaway is its own paragraph, as the "What to do now" line is on a transit card. */
    private static ReadingCard card(String label, Plain p, List<String> why) {
        return new ReadingCard(label, p.headline(),
            p.body() + "\n\nTakeaway: " + p.takeaway(), why);
    }

    /** The whole page. */
    static String html(Themes.Result t, boolean showWhy) {
        List<ReadingCard> cards = cards(t);
        StringBuilder sb = ReadingCard.page("Synthesize",
            "Only what at least " + Themes.signatureThreshold + " separate parts of your chart"
                + " agree on. One placement alone is a detail; this is the pattern.");
        if (cards.isEmpty()) {
            sb.append(ReadingCard.html(List.of(new ReadingCard("NO STRONG PATTERN",
                "No theme repeats " + Themes.signatureThreshold + " times",
                "Your chart spreads its weight. At a glance and Reading describe the parts.",
                List.of())), false));
        } else {
            sb.append(ReadingCard.html(cards, showWhy));
        }
        return sb.append("</body></html>").toString();
    }
}
