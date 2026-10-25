package com.zodiacomputing.ourania.gui;

/**
 * The line at the top of a You page that says which chart the page describes.
 *
 * <b>The words come from {@link SkymapPanel#whoseChart}</b>, which reads the chip state the
 * wheel already holds. This class only dresses them, so there is one answer to "whose chart is
 * this?" and many places that show it.
 */
final class WhoseChart {

    private WhoseChart() { }

    /** The banner as a paragraph. Empty when there is no wheel to ask. */
    static String html(SkymapPanel skymap) {
        if (skymap == null) {
            return "";
        }
        return "<p id='whose-chart' style='color:#9AA5B1; font-size:11px; margin:2px 0 10px 0;'>"
            + escape(skymap.whoseChart()) + "</p>";
    }

    /** Puts the banner just inside the {@code <body>} of a finished page. */
    static String into(String page, SkymapPanel skymap) {
        String banner = html(skymap);
        if (page == null || banner.isEmpty()) {
            return page;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("<body[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(page);
        return m.find() ? page.substring(0, m.end()) + banner + page.substring(m.end())
            : banner + page;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
