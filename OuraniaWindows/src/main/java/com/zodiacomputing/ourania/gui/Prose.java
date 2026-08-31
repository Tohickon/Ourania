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

    // ------------------------------------------------------------------ decorate

    /**
     * Colour and link every chart term in a block of interpretation HTML.
     *
     * <b>The reading and the wheel should agree.</b> A reading names bodies, signs and
     * aspects constantly and rendered every one of them as plain body text, so the word
     * "Venus" in a paragraph looked like the word "and" - while three inches away on the
     * wheel Venus was a coloured glyph you could click. This closes that gap: each term
     * takes its element colour from the same table the wheel paints from
     * ({@link SkymapPanel#elementTextHex}) and becomes a link to the entry it names.
     *
     * <b>Tag interiors and existing anchors are skipped.</b> The input is already HTML -
     * attributes carry words like "color", and the prose contains hand-written links - so a
     * naive replace would corrupt markup and nest anchors, which Swing's HTML renderer shows
     * as raw text. This walks the string and only rewrites runs that sit outside a tag and
     * outside an anchor.
     *
     * <b>Longest match wins.</b> "North Node" must beat "Node" and "Part of Fortune" must
     * beat "Fortune", so the vocabulary is sorted by descending length before matching.
     */
    public static String decorate(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        java.util.List<Term> vocab = vocabulary();
        StringBuilder out = new StringBuilder(html.length() + 512);
        int i = 0;
        int anchorDepth = 0;
        // <b>First mention per paragraph, not per occurrence.</b> Colouring every instance
        // turned a paragraph naming six bodies into confetti and buried the emphasis it was
        // supposed to add. The set clears on each block-level boundary, so a long reading
        // still lights each term up once per paragraph rather than once per document.
        java.util.Set<String> seen = new java.util.HashSet<String>();
        while (i < html.length()) {
            if (html.charAt(i) == '<') {
                int end = html.indexOf('>', i);
                if (end < 0) {
                    out.append(html, i, html.length());
                    break;
                }
                String tag = html.substring(i, end + 1);
                String lower = tag.toLowerCase();
                if (lower.startsWith("<a ") || lower.equals("<a>")) {
                    anchorDepth++;
                } else if (lower.startsWith("</a")) {
                    anchorDepth = Math.max(0, anchorDepth - 1);
                }
                if (isBlockBoundary(lower)) {
                    seen.clear();
                }
                out.append(tag);
                i = end + 1;
                continue;
            }
            int nextTag = html.indexOf('<', i);
            int stop = nextTag < 0 ? html.length() : nextTag;
            String text = html.substring(i, stop);
            out.append(anchorDepth > 0 ? text : decorateText(text, vocab, seen));
            i = stop;
        }
        return out.toString();
    }

    /** One term of the vocabulary: what to match, where it links, what colour it takes. */
    private static final class Term {
        final String word;
        final String href;
        final String hex;
        Term(String word, String href, String hex) {
            this.word = word;
            this.href = href;
            this.hex = hex;
        }
    }

    private static java.util.List<Term> vocab;

    /**
     * Bodies, signs and aspect names, longest first.
     *
     * Built once. The registry does not change at runtime, and rebuilding this per block
     * would walk 29 bodies and 12 signs for every paragraph of a 240,000-character reading.
     */
    private static synchronized java.util.List<Term> vocabulary() {
        if (vocab != null) {
            return vocab;
        }
        java.util.List<Term> v = new java.util.ArrayList<Term>();
        for (int i = 0; i < com.zodiacomputing.ourania.astro.Bodies.count(); i++) {
            com.zodiacomputing.ourania.astro.Bodies.Def d =
                com.zodiacomputing.ourania.astro.Bodies.at(i);
            v.add(new Term(d.name, "body|" + i,
                SkymapPanel.elementTextHex(SkymapPanel.elementOfBody(i))));
        }
        for (int si = 0; si < 12; si++) {
            String sign = com.zodiacomputing.ourania.astro.Zodiac.SIGNS[si];
            String display = Character.toUpperCase(sign.charAt(0)) + sign.substring(1);
            v.add(new Term(display, "sign|" + display,
                SkymapPanel.elementTextHex(
                    com.zodiacomputing.ourania.astro.Zodiac.elementIndex(si))));
        }
        for (com.zodiacomputing.ourania.astro.Aspects.Type t
                : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            String label = t.label;
            String display = Character.toUpperCase(label.charAt(0)) + label.substring(1);
            String hex = SkymapPanel.getAspectColorHex(label);
            v.add(new Term(display, "aspecttype|" + label, hex));
            v.add(new Term(label, "aspecttype|" + label, hex));
        }
        v.sort(new java.util.Comparator<Term>() {
            @Override
            public int compare(Term a, Term b) {
                return Integer.compare(b.word.length(), a.word.length());
            }
        });
        vocab = v;
        return vocab;
    }

    /** Rewrite one run of plain text. Whole words only, longest match first. */
    /** Which tags end a paragraph, for the purposes of "first mention". */
    private static boolean isBlockBoundary(String lowerTag) {
        return lowerTag.startsWith("<p") || lowerTag.startsWith("</p")
            || lowerTag.startsWith("<br") || lowerTag.startsWith("<li")
            || lowerTag.startsWith("<h") || lowerTag.startsWith("</h")
            || lowerTag.startsWith("<div") || lowerTag.startsWith("</div")
            || lowerTag.startsWith("<tr") || lowerTag.startsWith("<ul")
            || lowerTag.startsWith("<ol");
    }

    private static String decorateText(String text, java.util.List<Term> vocab,
                                       java.util.Set<String> seen) {
        StringBuilder sb = new StringBuilder(text.length() + 128);
        int i = 0;
        while (i < text.length()) {
            Term hit = null;
            for (Term t : vocab) {
                if (text.regionMatches(true, i, t.word, 0, t.word.length())
                        && isWordBoundary(text, i, t.word.length())
                        && !seen.contains(t.word)) {
                    hit = t;
                    break;
                }
            }
            if (hit == null) {
                sb.append(text.charAt(i));
                i++;
                continue;
            }
            // <b>No whitespace between the word and </a>.</b> Swing's HTML renderer treats a
            // trailing space inside an anchor as part of the link box, which pushed the
            // following comma away from the word and produced "Mercury , Virgo". The anchor
            // now wraps exactly the matched characters and nothing else.
            seen.add(hit.word);
            sb.append("<a href='").append(hit.href)
              .append("' style='color:").append(hit.hex)
              .append("; text-decoration:none;'>")
              .append(text, i, i + hit.word.length())
              .append("</a>");
            i += hit.word.length();
        }
        return sb.toString();
    }

    /** A match must not sit inside a longer word: "Mars" yes, "Marshal" no. */
    private static boolean isWordBoundary(String text, int start, int len) {
        if (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1))
                || text.charAt(start - 1) == '_')) {
            return false;
        }
        int after = start + len;
        return after >= text.length()
            || !(Character.isLetterOrDigit(text.charAt(after)) || text.charAt(after) == '_');
    }
}
