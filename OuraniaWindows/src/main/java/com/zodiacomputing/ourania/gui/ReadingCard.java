package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.PlacementVectorCalculator;
import java.util.ArrayList;
import java.util.List;

/**
 * One plain-English card: what this is, what it looks like in an ordinary week, and - one
 * click down - the astrology behind it.
 *
 * <b>The reader meets the meaning before the machinery.</b> David, 2026-10-13: the tabs "only
 * show the user the back end math side". A card leads with a headline a non-astrologer can
 * read, and the placements that produced it go in a "why" box that is shown only when
 * {@link Settings#astrologerView()} is on - off for a new install. Plan:
 * 2026-10-13-ourania-plain-english-tabs-plan in the vault.
 *
 * <b>A setting, not a per-card toggle,</b> for the reason {@link Settings#READING_MECHANICS_KEY}
 * gives: the pane is HTML 3.2, with no {@code <details>} to collapse and no script to collapse
 * it with, so the choice is made before the page is built.
 *
 * @param label    a small heading naming the card's question, or null
 * @param headline the one sentence a reader takes away in three seconds
 * @param body     the paragraph under it, plain prose
 * @param why      the astrology behind it, one line each, e.g. "Sun in Libra, 11th house"
 */
public record ReadingCard(String label, String headline, String body, List<String> why) {

    public ReadingCard {
        why = why == null ? List.of() : List.copyOf(why);
    }

    /**
     * A card for one theme of the year, from {@link PlacementVectorCalculator#readings}.
     *
     * <b>The label is the theme's plain name and nothing else.</b> The first draft put the
     * band there ("[MAJOR] Career"), which is the number in plain view by another name. The
     * band, the chart weight and the testimonies go in the why lines, which only Astrologer
     * view shows; whether this is a headline at all is the Rule of Three's, not the weight's.
     *
     * @param headline the sentence the caller chose; null falls back to the theme's name
     * @param takeaway appended as "How to handle it: ..."; null for none
     */
    public static ReadingCard fromTheme(PlacementVectorCalculator.ThemeReading r,
            String headline, String body, String takeaway) {
        StringBuilder b = new StringBuilder(body == null ? "" : body.trim());
        if (takeaway != null && !takeaway.isBlank()) {
            if (b.length() > 0) b.append("\n\n");
            b.append("How to handle it: ").append(takeaway.trim());
        }
        List<String> why = new ArrayList<>();
        why.add(r.convergence().families.size() + " independent techniques agree"
            + (r.headline() ? " (Rule of Three met)" : " (background, below the Rule of Three)"));
        why.add(String.format(java.util.Locale.ROOT, "Chart weight: %s, %.2f of the strongest theme",
            r.band().label, r.share()));
        why.addAll(r.convergence().testimonies);
        return new ReadingCard(r.theme().label,
            headline == null || headline.isBlank() ? r.theme().label : headline,
            b.toString(), why);
    }

    /** The cards as page body HTML; the caller supplies the document around it. */
    public static String html(List<ReadingCard> cards, boolean showWhy) {
        StringBuilder sb = new StringBuilder();
        for (ReadingCard c : cards) {
            sb.append("<table width='100%' cellpadding='10' cellspacing='0' bgcolor='#1B2230'"
                + " style='margin-bottom:12px;'><tr><td>");
            if (c.label != null && !c.label.isEmpty()) {
                sb.append("<div style='color:#7FA7C9; font-size:10px; font-weight:bold;'>")
                  .append(escape(c.label)).append("</div>");
            }
            if (c.headline != null && !c.headline.isEmpty()) {
                sb.append("<div style='color:#FFFFFF; font-size:15px; font-weight:bold;"
                    + " margin-top:4px;'>").append(escape(c.headline)).append("</div>");
            }
            if (c.body != null && !c.body.isEmpty()) {
                sb.append("<p style='font-size:13px; margin-top:6px;'>")
                  .append(escape(c.body)).append("</p>");
            }
            if (showWhy && !c.why.isEmpty()) {
                sb.append("<div style='color:#9AA5B1; font-size:11px; margin-top:6px;'>"
                    + "<b>Why &mdash; the astrology:</b> ")
                  .append(escape(String.join("; ", c.why))).append("</div>");
            }
            sb.append("</td></tr></table>");
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
