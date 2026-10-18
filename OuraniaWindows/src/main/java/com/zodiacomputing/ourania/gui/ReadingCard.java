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

    /** The id of the card a reveal lands on, wherever a page puts it. */
    static final String REVEAL_ID = "reveal-card";

    /** The line that starts a year card's direction, on its own paragraph. */
    static final String WHAT_TO_DO_THIS_YEAR = "What to do this year: ";

    /**
     * The card for one theme of the year: what the year asks, the part of life it shows up in,
     * and what to do this year (2026-10-17). One card for the page and for the reveal that
     * lands on it, and here rather than in the reveal so the phone's reading, which is this
     * same page, can make it too.
     */
    static ReadingCard forYearTheme(com.zodiacomputing.ourania.astro.ThemeConvergence.Result r) {
        String area = LifeAreas.of(r.theme.house());
        String body = area == null ? "" : "Shows up in: " + area + ".";
        String key = r.theme.name().toLowerCase(java.util.Locale.ROOT);
        InterpretationService svc = InterpretationService.getInstance();
        String asks = svc.getTransitCardPart("year." + key + ".asks");
        String todo = svc.getTransitCardPart("year." + key + ".todo");
        if (asks != null) {
            body = asks + (body.isEmpty() ? "" : " " + body);
        }
        if (todo != null) {
            body = body + "\n\n" + WHAT_TO_DO_THIS_YEAR + todo;
        }
        return new ReadingCard("THIS YEAR", r.theme.label, body, List.of());
    }

    /**
     * A card from a reading whose bold lead is its finding, or null when it has no lead.
     *
     * <b>The lead is the headline and the rest is the body,</b> tags stripped. One method for
     * the Now and Together pages, so a reading is split into a card the same way wherever it is
     * shown. A reading with nothing after its lead keeps the lead as its body too, so the card
     * never opens on an empty paragraph.
     */
    static ReadingCard fromLead(String label, String prose, List<String> why) {
        String lead = boldLead(prose);
        if (lead == null || lead.isEmpty()) {
            return null;
        }
        String rest = prose.replace("<b>" + lead + "</b>", "")
                           .replaceAll("(?i)<br\\s*/?>", " ")
                           .replaceAll("(?i)<[^>]+>", "")
                           .replaceAll("\\s+", " ")
                           .trim();
        return new ReadingCard(label, lead, rest.isEmpty() ? lead : rest, why);
    }

    /**
     * A card whose headline is the first sentence AFTER the bold lead, or null when the
     * reading has no such sentence.
     *
     * <b>For readings whose lead is a template, not a finding.</b> Measured 2026-10-16 on the
     * major synastry inter-aspects: 615 readings, 31 distinct leads, and five of them cover
     * about 590 - "A dynamic, high-friction ... catalyst for growth" heads 117 different
     * pairs. The first sentence after the lead is distinct in all 615. A page of cards that
     * all open on the same sentence says nothing about these two people, so the sentence that
     * does is promoted, word for word. Nothing is written here; the lead is dropped, not
     * rewritten, and the caller's label carries the tone it described.
     */
    static ReadingCard fromFirstSentence(String label, String prose, List<String> why) {
        if (prose == null) {
            return null;
        }
        String lead = boldLead(prose);
        String text = (lead == null ? prose : prose.replace("<b>" + lead + "</b>", ""))
            .replaceAll("(?i)<br\\s*/?>", " ")
            .replaceAll("(?i)<[^>]+>", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (text.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?[.!?])(\\s+|$)").matcher(text);
        String first = m.find() ? m.group(1) : text;
        String rest = text.substring(first.length()).trim();
        return new ReadingCard(label, first, rest.isEmpty() ? first : rest, why);
    }

    /**
     * The bold lead of a prose entry - the one-sentence finding - or null.
     *
     * Here rather than on the wheel panel, where it began, because what counts as a reading's
     * lead is the card's question. SkymapPanel.boldLead still answers by asking this.
     */
    static String boldLead(String prose) {
        if (prose == null) {
            return null;
        }
        int open = prose.indexOf("<b>");
        int close = prose.indexOf("</b>", open + 3);
        return open >= 0 && close > open ? prose.substring(open + 3, close) : null;
    }

    /**
     * The start of a plain page: the document, its title and a line saying what it is for.
     * The caller appends the cards and closes it with {@code </body></html>}. One header for
     * the Now and Together pages, so they cannot drift apart in how a page opens.
     */
    static StringBuilder page(String title, String intro) {
        return new StringBuilder("<html><body style='font-family:Arial; font-size:12px; color:white;'>")
            .append("<h2 style='color:#ffa500; margin-bottom:2px;'>").append(title).append("</h2>")
            .append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:14px;'>")
            .append(intro).append("</div>");
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
                // A blank line in the body is a new paragraph: fromTheme's "How to handle it"
                // and a transit card's "What to do now" stand on their own line.
                for (String para : c.body.split("\n\\s*\n")) {
                    sb.append("<p style='font-size:13px; margin-top:6px;'>")
                      .append(escape(para.trim())).append("</p>");
                }
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
