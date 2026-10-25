package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The words a plain card may not use, and the one test for them.
 *
 * <b>The list is David's</b>, from his source document for the plain tabs ("The structure
 * needed", the "Frontend Banned Words Filter"). It lived in {@link JargonCheck}, which is a test;
 * a page that chooses its sentences by this list cannot depend on a test, so it lives here and the
 * suite asks it. Words in the "why" drawer are exempt - that is where the real terms belong.
 *
 * <b>Whole words, any case</b>; the patterns take the plural and other endings.
 */
final class PlainWords {

    static final String[] BANNED = {
        // Astrological mechanics
        "profections?", "lords? of the year", "time-?lords?", "dispositors?", "sect", "diurnal",
        "houses?", "cusps?", "decans?", "chaldean faces?", "sabian symbols?", "sabian", "septiles?",
        // Aspect and transit words
        "conjunctions?", "conjunct", "squares?", "trines?", "sextiles?", "oppositions?",
        "inconjuncts?", "orbs?", "applying", "separating", "transit\\w*", "progress(ed|ion\\w*)",
        "solar arcs?", "solar returns?", "translation of light", "collection of light",
        // Mystical jargon
        "karmic", "alchemical", "sacred catalysts?", "vibrational frequenc(y|ies)", "cosmic web",
        "akashic", "destiny"
    };

    private static final Pattern WORDS = Pattern.compile(
        "(?i)\\b(" + String.join("|", BANNED) + ")\\b");

    private PlainWords() {
    }

    /** The banned words in a piece of plain text, each once, in the order they appear. */
    static Set<String> banned(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) {
            return out;
        }
        Matcher m = WORDS.matcher(text);
        while (m.find()) {
            out.add(m.group(1).toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /** Whether the text is free of every banned word. */
    static boolean isPlain(String text) {
        return banned(text).isEmpty();
    }

    /**
     * The sentences of a reading that are plain, in order, with markup removed.
     *
     * <b>For choosing among a reading's own sentences, never for rewriting one.</b> A reading
     * written in the astrologer's register has plain sentences among the others; picking one is
     * honest, and changing a word in a sentence to make it pass is not.
     */
    static List<String> plainSentences(String reading) {
        List<String> out = new ArrayList<>();
        if (reading == null) {
            return out;
        }
        String text = reading.replaceAll("(?i)<br\\s*/?>", ". ").replaceAll("(?i)<[^>]+>", " ")
            .replaceAll("\\s+", " ").trim();
        for (String s : text.split("(?<=[.!?])\\s+")) {
            String t = s.trim();
            if (t.length() > 15 && isPlain(t)) {
                out.add(t);
            }
        }
        return out;
    }
}
