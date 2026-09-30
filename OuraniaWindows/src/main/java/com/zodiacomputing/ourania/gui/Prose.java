package com.zodiacomputing.ourania.gui;

/**
 * Interpretation HTML dressed for a screen that renders HTML: every chart term coloured and
 * linked ({@link #decorate}).
 *
 * <p>The plain-text half of what this class used to do - markup to prose, the one-line
 * summary, the escapes the loader leaves behind - is
 * {@link com.zodiacomputing.ourania.astro.PlainText} now. It moved on 30 Sep (M1) because the
 * engine needs it and this class borrows the wheel's colours from {@code SkymapPanel}, so the
 * engine calling it pulled in the whole desktop app. Colour stays here, with the desktop.
 */
public final class Prose {

    private Prose() { }

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
            // <b>Trailing punctuation goes INSIDE the anchor.</b> Swing's HTMLEditorKit gives
            // an inline anchor its own margin, so a link followed by a comma renders as
            // "Mercury , Virgo" however tight the markup is. Two earlier attempts missed:
            // closing the tag hard against the comma changed nothing, because the generated
            // HTML was already tight; and a CSS margin/padding reset on `a` does not override
            // the kit. Swallowing the punctuation moves the renderer's space to AFTER the
            // comma, which is where a space belongs, so the artefact becomes correct spacing.
            //
            // Only closing punctuation, and only one character - enough for the comma and
            // full stop that actually follow a body name in this prose, without absorbing a
            // whole clause into a link the reader would then be able to click.
            int wordEnd = i + hit.word.length();
            int linkEnd = wordEnd;
            if (linkEnd < text.length() && isTrailingPunctuation(text.charAt(linkEnd))) {
                linkEnd++;
            }
            seen.add(hit.word);
            sb.append("<a href='").append(hit.href)
              .append("' style='color:").append(hit.hex)
              .append("; text-decoration:none;'>")
              .append(text, i, linkEnd)
              .append("</a>");
            i = linkEnd;
        }
        return sb.toString();
    }

    /** Punctuation that may be pulled inside a link to defeat the renderer's margin. */
    private static boolean isTrailingPunctuation(char c) {
        return c == ',' || c == '.' || c == ';' || c == ':' || c == '!' || c == '?';
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
