package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ThemeConvergence;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a reading's reveal shows, read off the engine's own verdict (storyboard "The chart
 * reveals the reading", stage 1, 2026-10-16).
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
 * @param points    the natal chart factors the counted votes landed on, each once
 */
record RevealPlan(String title, String body, List<String> ticks, boolean confirmed,
        List<String> points) {

    RevealPlan {
        ticks = java.util.Collections.unmodifiableList(new ArrayList<>(ticks));
        points = List.copyOf(points);
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
