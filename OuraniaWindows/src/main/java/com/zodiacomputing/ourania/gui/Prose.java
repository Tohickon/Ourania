package com.zodiacomputing.ourania.gui;

/**
 * Turning interpretation markup into text a plain-text surface can print.
 *
 * The interpretation data is HTML - `&lt;b&gt;summary.&lt;/b&gt;&lt;br&gt;&lt;br&gt;body` - and the
 * hand-rolled loader does not convert the backslash-u escapes it was stored with. Three
 * separate surfaces have now pasted that straight into plain text and shown readers
 * literal tags mid-sentence: Snapshot.paragraph (fixed 2026-08-21 by deletion),
 * Snapshot.report, and TimelinePredictor.
 *
 * So the conversion lives here once. A caller that wants prose out of the interpretation
 * data calls this; a caller that is building HTML does not need it at all. The failure
 * mode when it is skipped is not subtle but it is silent - nothing throws, the reader just
 * gets markup - which is exactly why it should not be reimplemented per surface.
 */
public final class Prose {

    private Prose() { }

    /**
     * Interpretation markup as plain prose: tags removed, escapes converted, the paragraph
     * break turned into a real one, runs of space collapsed.
     *
     * Returns "" for null, empty, or a not-found placeholder, so a caller can test the
     * result rather than test the input against a list of placeholder prefixes.
     */
    public static String plainText(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty() || s.startsWith("Interpretation not found") || s.startsWith("General ")) {
            return "";
        }
        // The double break is a paragraph boundary in the source and the only structure
        // worth keeping; a single break is a soft wrap and becomes a space.
        s = s.replace("<br><br>", "\n\n").replace("<br/><br/>", "\n\n")
             .replace("<br>", " ").replace("<br/>", " ").replace("<br />", " ");
        s = s.replaceAll("<[^>]*>", "");
        s = unescape(s);
        s = s.replace("&nbsp;", " ").replace("&amp;", "&")
             .replace("&lt;", "<").replace("&gt;", ">")
             .replace("&quot;", "\"").replace("&mdash;", "-").replace("&ndash;", "-")
             .replace("&ldquo;", "\"").replace("&rdquo;", "\"")
             .replace("&lsquo;", "'").replace("&rsquo;", "'")
             .replace("&deg;", " degrees");
        // Collapse spaces and tabs only - the paragraph breaks above must survive.
        s = s.replaceAll("[ \\t]+", " ").replaceAll(" *\\n *", "\n");
        return s.trim();
    }

    /**
     * The one-line summary an interpretation opens with, stripped of its label and of its
     * closing full stop.
     *
     * Three shapes live in this data:
     *
     *   "Ceres in Aries: Nurturing is expressed through..."   name, colon, fragment
     *   "South Node - Innate talent, and the comfort..."      name, dash, fragment
     *   "Chiron in Cancer carries a devastating wound..."     already a whole sentence
     *
     * Only the first two carry a label, and leaving it on is what produced
     * "South Node (south Node - innate talent...)" - the name printed twice, the second
     * copy lowercased by a caller moving it mid-clause.
     *
     * The trailing stop goes because every caller supplies its own punctuation; leaving it
     * gave "...respect for authority.." throughout the Predict output.
     */
    public static String summary(String raw) {
        String s = plainText(raw);
        if (s.isEmpty()) {
            return "";
        }
        int para = s.indexOf('\n');
        if (para > 0) {
            s = s.substring(0, para).trim();
        }
        s = stripLabel(s);
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    /** Longest label this will strip. Past this it is a sentence, not a name. */
    private static final int LABEL_LIMIT = 40;

    /**
     * Drops a leading "Name:" or "Name -" label.
     *
     * Bounded at LABEL_LIMIT and required to be the FIRST such separator, so a dash used
     * as punctuation later in a real sentence is left alone. The dash forms are the em
     * dash the data was written with, the en dash, and a spaced hyphen.
     */
    private static String stripLabel(String s) {
        int cut = -1;
        int colon = s.indexOf(':');
        if (colon > 0 && colon < LABEL_LIMIT) {
            cut = colon + 1;
        }
        for (String dash : new String[]{" — ", " – ", " - "}) {
            int i = s.indexOf(dash);
            if (i > 0 && i < LABEL_LIMIT && (cut < 0 || i + dash.length() < cut)) {
                cut = i + dash.length();
            }
        }
        return cut > 0 ? s.substring(cut).trim() : s;
    }

    /**
     * The backslash-u escapes the loader leaves behind, as real characters.
     *
     * Public because it is the half of the conversion a caller sometimes needs on its own,
     * and because a second copy of this loop is how "&mdash;" started rendering as literal
     * backslash-u-2-0-1-4 in three places at once.
     */
    public static String unescape(String s) {
        if (s == null || s.indexOf("\\u") < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 5 < s.length() && s.charAt(i + 1) == 'u') {
                try {
                    sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                    i += 5;
                    continue;
                } catch (NumberFormatException ignored) {
                    // not an escape after all: keep the backslash as written
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
