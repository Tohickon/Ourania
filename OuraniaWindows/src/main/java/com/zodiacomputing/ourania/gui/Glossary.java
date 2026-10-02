package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.DataFiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * David's glossary of astrology (added 2 Oct 2026): 827 terms in 19 chapters, read from
 * {@code glossary.json}, which {@code generate_glossary.py} builds from his text in
 * {@code reference/glossary}.
 *
 * <p><b>One reader for both apps.</b> It is opened through {@link DataFiles}, as the corpus and
 * the atlas are, so the desktop reads it from the data folder and the phone from its assets;
 * neither has a copy of its own (it is on {@code EngineIsolationCheck}'s shared list).
 *
 * <p><b>Search finds the term first.</b> A reader typing "cazimi" wants the entry called Cazimi,
 * not every entry that mentions it in passing, so a search ranks a term that starts with the
 * words typed, then one that contains them, and only then entries whose text does. Accents and
 * case are ignored, as the atlas ignores them.
 */
public final class Glossary {

    public static final String FILE_NAME = "glossary.json";

    /** One entry: the term as written, where it sits, and its definition as simple HTML. */
    public static final class Term {
        public final String term;
        public final String chapter;
        public final String section;
        /** The definition, with only b, i and br tags. */
        public final String text;
        final String foldedTerm;
        final String foldedText;

        Term(String term, String chapter, String section, String text) {
            this.term = term;
            this.chapter = chapter;
            this.section = section;
            this.text = text;
            this.foldedTerm = Atlas.fold(term);
            this.foldedText = Atlas.fold(text.replaceAll("<[^>]+>", " "));
        }

        @Override
        public String toString() {
            return this.term;
        }
    }

    private static volatile List<Term> terms;
    private static volatile String failure;

    private Glossary() { }

    /** Every term, in the file's order. Empty, never null, when the file could not be read. */
    public static List<Term> all() {
        List<Term> t = terms;
        if (t == null) {
            synchronized (Glossary.class) {
                if (terms == null) {
                    terms = load();
                }
                t = terms;
            }
        }
        return t;
    }

    /** Why the glossary is empty, or null when it loaded. */
    public static String failure() {
        all();
        return failure;
    }

    @SuppressWarnings("unchecked")
    private static List<Term> load() {
        try {
            Map<String, Object> doc = Json.object(Json.parse(DataFiles.entry(FILE_NAME)));
            List<Object> rows = (List<Object>) doc.get("entries");
            List<Term> out = new ArrayList<>(rows.size());
            for (Object row : rows) {
                Map<String, Object> e = (Map<String, Object>) row;
                out.add(new Term(str(e.get("term")), str(e.get("chapter")),
                    str(e.get("section")), str(e.get("text"))));
            }
            failure = null;
            return Collections.unmodifiableList(out);
        } catch (Exception e) {                       // Json.Malformed included
            failure = FILE_NAME + " could not be read: " + e.getMessage();
            return Collections.emptyList();
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /** The chapters, in the file's order. */
    public static List<String> chapters() {
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (Term t : all()) {
            seen.put(t.chapter, Boolean.TRUE);
        }
        return new ArrayList<>(seen.keySet());
    }

    /** One chapter's terms, in the file's order. */
    public static List<Term> chapter(String name) {
        List<Term> out = new ArrayList<>();
        for (Term t : all()) {
            if (t.chapter.equals(name)) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Terms matching what was typed, best first: the term starts with it, then the term
     * contains it, then the definition does. At most {@code limit}.
     */
    public static List<Term> search(String query, int limit) {
        String q = Atlas.fold(query == null ? "" : query).trim();
        List<Term> out = new ArrayList<>();
        if (q.isEmpty()) {
            return out;
        }
        List<Term> starts = new ArrayList<>();
        List<Term> inTerm = new ArrayList<>();
        List<Term> inText = new ArrayList<>();
        for (Term t : all()) {
            if (t.foldedTerm.startsWith(q) || stripNumber(t.foldedTerm).startsWith(q)) {
                starts.add(t);
            } else if (t.foldedTerm.contains(q)) {
                inTerm.add(t);
            } else if (t.foldedText.contains(q)) {
                inText.add(t);
            }
        }
        for (List<Term> group : List.of(starts, inTerm, inText)) {
            for (Term t : group) {
                if (out.size() >= limit) {
                    return out;
                }
                out.add(t);
            }
        }
        return out;
    }

    /** "12. al-zubana" reads as "al-zubana" for the purpose of starting with what was typed. */
    private static String stripNumber(String folded) {
        return folded.replaceFirst("^\\d+\\.\\s*", "");
    }

    /**
     * The entry for a term by its name, ignoring case, accents and anything in brackets -
     * "Cazimi", or "Ascendant" for "Ascendant (Rising Sign)". The first in the file wins.
     * Null when there is none.
     */
    public static Term find(String name) {
        String want = bare(Atlas.fold(name == null ? "" : name));
        if (want.isEmpty()) {
            return null;
        }
        for (Term t : all()) {
            if (bare(t.foldedTerm).equals(want)) {
                return t;
            }
        }
        return null;
    }

    private static String bare(String folded) {
        return stripNumber(folded.replaceAll("\\([^)]*\\)", "")).trim()
            .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
