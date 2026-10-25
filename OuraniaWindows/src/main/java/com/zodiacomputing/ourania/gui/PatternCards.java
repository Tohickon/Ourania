package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AspectPatterns;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.TensionRelease;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The aspect patterns as plain cards, for the Patterns tab (reading framework spec, 2026-10-24).
 *
 * <b>One card per closed figure, read from the corpus and not from here.</b> The reading is the
 * macro-dynamics entry the Reading page already uses, under the same keys and in the same order
 * of preference ({@link AspectPatterns.Pattern#detailKey}, then {@code variantKey}, then the
 * name). A figure with no entry gets no card rather than words written here; the engine's
 * AspectPatternCheck says no name it can emit lacks one.
 *
 * <b>The figure's own name stays out of the plain view.</b> "T-square" carries a banned word, and
 * a reader does not need the name to be told that three planets are pulling on one point. The
 * label says what the figure does for any of them, and the name, the members, the apex, the
 * widest leg and what releases it go in the why lines, which only Astrologer view shows.
 */
final class PatternCards {

    /** The label on every pattern card. Plain, and true of every figure the engine finds. */
    static final String LABEL = "PLANETS WORKING AS ONE";

    private PatternCards() {
    }

    /** The patterns' cards, in the order the engine found them. */
    static List<ReadingCard> cards(Gestalt.Result g) {
        List<ReadingCard> out = new ArrayList<>();
        if (g == null) {
            return out;
        }
        InterpretationService svc = InterpretationService.getInstance();
        for (AspectPatterns.Pattern ap : g.aspectPatterns) {
            String prose = first(svc.getMacroDynamic(ap.detailKey()),
                svc.getMacroDynamic(ap.variantKey()), svc.getMacroDynamic(ap.name));
            if (prose == null) {
                continue;
            }
            List<String> why = why(ap, g);
            // The reading is in the astrologer's register (measured 2026-10-24: 7 of the 21
            // entries open on a sentence the banned list rejects), so the plain card takes the
            // reading's own clean sentences, in order, and the whole reading goes to the why
            // lines. A sentence is chosen or skipped, never changed. With none clean, the card
            // says only what the engine found.
            List<String> clean = PlainWords.plainSentences(prose);
            String headline = clean.isEmpty()
                ? TogetherCards.joinAnd(ap.bodies) + " work as one unit."
                : clean.get(0);
            String body = clean.size() > 1
                ? String.join(" ", clean.subList(1, Math.min(clean.size(), 3))) : "";
            why.add("In full: " + prose.replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("(?i)<[^>]+>", "").replaceAll("\\s+", " ").trim());
            out.add(new ReadingCard(LABEL, headline, body, why));
        }
        return out;
    }

    /** The page's section: a heading, how many figures there are, and the cards. */
    static String html(Gestalt.Result g, boolean showWhy) {
        StringBuilder sb = new StringBuilder("<h3 style='color:#FFD166; margin-top:18px; "
            + "margin-bottom:4px;'>Patterns that tie your chart together</h3>");
        List<ReadingCard> cards = cards(g);
        if (cards.isEmpty()) {
            return sb.append("<div style='color:#9AA5B1; font-size:12px; margin-bottom:10px;'>"
                + "No group of planets forms a closed figure in this chart.</div>").toString();
        }
        sb.append("<div style='color:#9AA5B1; font-size:12px; margin-bottom:6px;'>")
          .append(cards.size()).append(cards.size() == 1 ? " group of planets works"
              : " groups of planets work").append(" as one unit.</div>");
        return sb.append(ReadingCard.html(cards, showWhy)).toString();
    }

    /** The astrology behind a card: name, members, apex, how loose it is, what releases it. */
    private static List<String> why(AspectPatterns.Pattern ap, Gestalt.Result g) {
        List<String> why = new ArrayList<>();
        String qualifier = ap.modality != null && (ap.name.equals("T-square")
                || ap.name.equals("Grand cross")) ? ap.modality
            : ap.element != null && (ap.name.equals("Grand trine") || ap.name.equals("Kite"))
                ? ap.element : null;
        why.add(ap.name + (qualifier == null ? "" : ", " + qualifier.toLowerCase(Locale.ROOT)));
        why.add("Members: " + String.join(", ", ap.bodies));
        if (ap.apex != null) {
            why.add("Focal point: " + ap.apex);
        }
        why.add(String.format(Locale.ROOT, "Widest leg %.2f°", ap.widestOrb));
        for (TensionRelease.Release rel : g.releases) {
            if (rel.source.equals(ap.name) && rel.bodies.equals(ap.bodies)) {
                why.add(rel.sentence);
            }
        }
        return why;
    }

    /** The first entry that says something: the service answers "" for a key it lacks. */
    private static String first(String... entries) {
        for (String e : entries) {
            if (e != null && !e.isEmpty() && !e.startsWith("Interpretation not found")) {
                return e;
            }
        }
        return null;
    }
}
