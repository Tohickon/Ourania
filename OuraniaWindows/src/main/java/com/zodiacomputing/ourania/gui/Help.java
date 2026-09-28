package com.zodiacomputing.ourania.gui;

/**
 * The help page, generated from the app rather than written about it (G11).
 *
 * <p><b>A help page written by hand is a second description of the program.</b> This project has
 * watched four of those come apart - four lists of house systems, six spellings of the ring word,
 * three copies of the isMinor rule - and a help page is the copy least likely to be corrected,
 * because nothing fails when it goes stale. So every screen, table and export listed here is read
 * from {@link SidePanel}'s own arrays, and every keystroke from {@link Shortcuts.Key}. A screen
 * added to the menu appears here without anybody remembering, and {@code HelpPageCheck} fails if
 * one ever does not.
 *
 * <p><b>What is written by hand is the part that is not derivable:</b> what the app is for, and the
 * glossary. The glossary exists because this app uses a handful of words in a particular way -
 * "ring", "frame", "layer" - and a reader meeting them in a tooltip has nowhere to look them up.
 *
 * <p><b>Onboarding is deliberately not here.</b> The row asks for it, and it belongs after G16: a
 * guided tour of a screen that is about to be restructured is work thrown away. That is a decision,
 * not an omission, and it is recorded on the master list.
 */
final class Help {

    private Help() { }

    /** The app's own words, which a reader meets in tooltips and nowhere else. */
    static final String[][] GLOSSARY = {
        {"Ring", "One band of the wheel. The chart has three - the natal chart innermost, then "
            + "an optional second chart or a symbolic one, then the sky. Which ring a reading "
            + "is about is named on the page it appears on."},
        {"Frame", "One cast chart: a moment, a place and a house system, with every position "
            + "worked out. The wheel draws a frame; a reading is written from one."},
        {"Chart A / Chart B", "The two people, or the two moments, a synastry or a composite is "
            + "made of. Chart A is always the inner ring."},
        {"Sky", "The real positions at a moment, as against a chart that has been progressed, "
            + "directed or otherwise moved symbolically."},
        {"Progressed", "The chart advanced a day of ephemeris for a year of life. Everything "
            + "moves, at its own rate."},
        {"Directed", "The birth chart with one arc added to every point. Nothing moves under "
            + "its own power - the whole chart is carried."},
        {"Orb", "How far from exact an aspect may be and still count. Each point has its own "
            + "width and each aspect its own ceiling, and both are yours to set."},
        {"Preset", "Which set of orbs is in force: Natal, Transits, Synastry or Composite. A "
            + "transit is judged more tightly than a birth chart, so they are kept apart."},
        {"Rodden rating", "How well a birth time is known, from AA (a record) to X (no time at "
            + "all). An X chart is cast for noon and its angles are withheld rather than guessed."},
        {"Witness", "In a reading, one independent statement for a theme. Statements that rest "
            + "on the same placement count once between them, however many there are."},
        {"Known-red", "A check suite failure that is understood and written down, so the build "
            + "can tell a defect somebody knows about from one that has just appeared."},
    };

    /** The whole page. */
    static String html() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Ourania+</h1>");
        sb.append("<p>An astrology workbench: it casts charts, it reads them, and it shows "
            + "its working. Every reading it writes comes from positions you can see on the "
            + "wheel and settings you can change.</p>");
        sb.append("<p><b>Version ").append(escape(Version.display())).append("</b>. ")
            .append("Settings &rarr; Diagnostics says where this build keeps your files and what "
                + "it found when it started.</p>");

        section(sb, "The screens");
        rows(sb, SidePanel.SCREENS);

        section(sb, "The tables");
        sb.append("<p>Each opens beside the wheel and reads the chart currently drawn.</p>");
        rows(sb, SidePanel.TABLE_ROWS);

        section(sb, "Sending something out");
        rows(sb, SidePanel.EXPORT_ROWS);

        section(sb, "Keyboard");
        sb.append("<table cellpadding='3'>");
        for (Shortcuts.Key k : Shortcuts.Key.values()) {
            sb.append("<tr><td><b>").append(escape(k.written())).append("</b></td><td>")
                .append(escape(k.label)).append("</td><td>").append(escape(k.about))
                .append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p>Every one of these is Control-modified on purpose, except the two that "
            + "are not keys you could be typing: a bare letter is a character, and the scrub "
            + "bars already own the bare arrows.</p>");

        section(sb, "Words this app uses in a particular way");
        sb.append("<dl>");
        for (String[] term : GLOSSARY) {
            sb.append("<dt><b>").append(escape(term[0])).append("</b></dt><dd>")
                .append(escape(term[1])).append("</dd>");
        }
        sb.append("</dl>");

        section(sb, "When something looks wrong");
        sb.append("<p>Settings &rarr; Diagnostics checks the ephemeris files, the interpretation "
            + "corpus and your settings, and can save a problem report: one file holding this "
            + "build's version, where it is reading from and the end of the log. It holds no "
            + "birth data, no saved charts and no place names, and nothing is sent anywhere "
            + "unless you attach it yourself.</p>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title) {
        sb.append("<h2>").append(escape(title)).append("</h2>");
    }

    /** One list of {label, id, description}, as a definition list. */
    private static void rows(StringBuilder sb, String[][] table) {
        sb.append("<dl>");
        for (String[] row : table) {
            sb.append("<dt><b>").append(escape(row[0])).append("</b></dt><dd>")
                .append(escape(row[2])).append("</dd>");
        }
        sb.append("</dl>");
    }

    /** These are pane contents, and a description may carry an ampersand or a dash. */
    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
