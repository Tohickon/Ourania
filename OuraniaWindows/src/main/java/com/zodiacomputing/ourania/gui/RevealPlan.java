package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ThemeConvergence;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a reading's reveal shows, read off the engine's own verdict (storyboard "The chart
 * reveals the reading", stage 1, 2026-10-16), or off a placement's own reading (stage 2).
 *
 * <b>Nothing here decides anything.</b> The chart factors are the points the theme's counted
 * votes landed on; the ticks are the votes that added a family; "confirmed" is
 * {@link ThemeConvergence.Result#headline}, the same Rule of Three {@link ReadingCard#fromTheme}
 * states. A plan that counted for itself could show a theme forming from factors the engine did
 * not use, which is the one thing a reveal meant to earn trust must never do.
 *
 * <b>The words are ones the app already says.</b> The title is the theme's plain name and the
 * body is the part of life its house stands for, from {@link LifeAreas}. The technique names on
 * the ticks are the synthesis page's own ({@link NarrativeSynthesizer#familyLabel}), and only
 * Astrologer view shows them; plain view shows the ticks with no names.
 *
 * @param title     the card's heading: the theme's plain name
 * @param body      the line under it
 * @param ticks     one entry per technique that counted, in the order the engine counted them;
 *                  the technique's name in Astrologer view, null in plain view
 * @param confirmed whether the Rule of Three is met
 * @param points    the natal chart factors the counted votes landed on, each once; a point
 *                  on the sky ring is written {@link #SKY} + its name
 * @param label     the card's small heading, or null for the title in capitals
 */
record RevealPlan(String title, String body, List<String> ticks, boolean confirmed,
        List<String> points, String label) {

    /** The prefix of a point that stands on the sky ring, not the natal one. */
    static final String SKY = "sky:";

    RevealPlan {
        ticks = java.util.Collections.unmodifiableList(new ArrayList<>(ticks));
        points = List.copyOf(points);
    }

    /** A plan whose card is headed by its title in capitals, as stage 1 and 2 draw it. */
    RevealPlan(String title, String body, List<String> ticks, boolean confirmed,
            List<String> points) {
        this(title, body, ticks, confirmed, points, null);
    }

    /**
     * The plan for one transit's plain card (2026-10-17): the card itself, word for word, and
     * its two factors - the planet in the sky, where the card forms, and the natal point it
     * touches. Nothing is counted.
     */
    static RevealPlan forTransit(NowCards.TransitHit hit, ReadingCard card) {
        if (hit == null || card == null) {
            return null;
        }
        return new RevealPlan(card.headline(), card.body(), List.of(), false,
            List.of(SKY + hit.transiting(), hit.natal()), card.label());
    }

    /** The plan for one theme, or null when the theme is not a headline - stage 1 shows only those. */
    static RevealPlan forTheme(ThemeConvergence.Result r, boolean astrologer) {
        if (r == null || !r.headline()) {
            return null;
        }
        List<String> ticks = new ArrayList<>();
        Set<String> points = new LinkedHashSet<>();
        for (ThemeConvergence.Vote v : r.votes) {
            if (v.first()) {
                ticks.add(astrologer ? NarrativeSynthesizer.familyLabel(v.family().name()) : null);
            }
            if (v.point() != null) {
                points.add(v.point());
            }
        }
        String area = LifeAreas.of(r.theme.house());
        String body = area == null ? "" : "Shows up in: " + area + ".";
        return new RevealPlan(r.theme.label, body, ticks, r.headline(), new ArrayList<>(points));
    }

    /**
     * The plan for the first headline of a year, or null when no theme reaches three.
     * ThemeConvergence sorts headlines first, so the first headline is the one tier 2 opens on.
     */
    static RevealPlan firstHeadline(List<ThemeConvergence.Result> themes, boolean astrologer) {
        if (themes == null) {
            return null;
        }
        for (ThemeConvergence.Result r : themes) {
            if (r.headline()) {
                return forTheme(r, astrologer);
            }
        }
        return null;
    }

    /**
     * The plan for one natal placement the reader clicked (stage 2), or null when its reading
     * has no heading to show.
     *
     * <b>Every part is lifted from the reading the click opens.</b> The title is that reading's
     * first heading ("Venus in Libra"); the body is the first sentence under that heading, by
     * {@link InterpretationPanel#pieceLead}, the rule the main account already uses. Under the
     * heading, not at the top: the top is the planet's core text, which for some planets opens
     * with a myth rather than with what this placement means. The factors
     * are the planet and each body in {@code aspects}, the list the reading was written from.
     * Nothing is counted: there are no ticks, and a placement is never "confirmed".
     *
     * @param body    the clicked body's name, which is also the focal point
     * @param aspects the reading's own aspect rows, the other body's name first in each
     * @param reading the reading's HTML
     */
    static RevealPlan forPlacement(String body, List<String[]> aspects, String reading) {
        if (body == null || reading == null || reading.isEmpty()) {
            return null;
        }
        List<String[]> sections = SkymapPanel.readingSections(reading);
        if (sections.isEmpty()) {
            return null;
        }
        String lead = InterpretationPanel.pieceLead(sections.get(0)[1]);
        if (lead.toLowerCase(java.util.Locale.ROOT).contains("not found")) {
            lead = "";
        }
        Set<String> points = new LinkedHashSet<>();
        points.add(body);
        if (aspects != null) {
            for (String[] a : aspects) {
                if (a != null && a.length > 0 && a[0] != null) {
                    points.add(a[0]);
                }
            }
        }
        return new RevealPlan(sections.get(0)[0], lead, List.of(), false,
            new ArrayList<>(points));
    }

    /** The id of the real card on the page, which the flying card lands on. */
    static final String CARD_ID = "reveal-card";

    /**
     * The card the reveal forms, as one {@link ReadingCard}: the painter draws it and the page
     * shows it through {@link ReadingCard#html}, so the two cannot say different things.
     */
    ReadingCard card() {
        return new ReadingCard(label != null ? label : title.toUpperCase(java.util.Locale.ROOT),
            title, body, List.of());
    }

    /**
     * The page with this card at the top of its body, as real HTML that stays.
     *
     * <b>The card that flies must land on something.</b> David, 2026-10-17: the painted card
     * "disappears too soon and makes no sense". It landed on the top of a page that said
     * something else, and when it faded nothing on the page matched it. Now the page opens on
     * the same card, and the flying one lands exactly on it.
     */
    String onTop(String html) {
        String card = "<div id='" + CARD_ID + "'>" + ReadingCard.html(List.of(card()), false)
            + "</div>";
        if (html == null) {
            return card;
        }
        int body = html.indexOf("<body");
        int open = body < 0 ? -1 : html.indexOf('>', body);
        return open < 0 ? card + html : html.substring(0, open + 1) + card + html.substring(open + 1);
    }

    /** Whether the ticks carry technique names, which only Astrologer view gives them. */
    boolean labelled() {
        for (String t : ticks) {
            if (t != null) {
                return true;
            }
        }
        return false;
    }
}
