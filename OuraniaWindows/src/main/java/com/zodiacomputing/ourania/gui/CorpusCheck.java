package com.zodiacomputing.ourania.gui;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Where the reading text came from (C7), and whether a square reads differently from a trine (C5).
 *
 * <p><b>C7 was "no automated guard exists".</b> With 600,000+ entries no reader can tell text
 * somebody wrote from text a script assembled, and PROVENANCE.md, which says so, is prose: nothing
 * stops a file arriving without a word about its origin. {@code provenance.tsv}, beside the data,
 * is the mechanism. Every file the app opens has a row saying <i>who</i> wrote it and what
 * <i>shape</i> it is, and Part A fails on a file with no row - so the question is asked at the
 * moment a file is added, the only moment it is cheap to answer.
 *
 * <p><b>The writer is declared; the shape is measured.</b> Who wrote a file cannot be read off the
 * text, and this suite does not pretend to: it holds the declaration to a closed vocabulary and
 * makes {@code unknown} say what is unknown. But whether a file was written entry by entry or
 * <i>composed</i> - the same sentences reassembled around a changing name - can be measured, and
 * Part B does: the share of a file's sentences that recur in three or more of its entries. Measured
 * on 28 Sep over the 55 files the app opens, written files sit at 0.000 to 0.051 and composed files
 * at 0.197 to 0.889, with nothing between, so 0.10 is a boundary in an empty gap rather than a line
 * drawn through a crowd. A file declared written that measures composed is a wrong row, and so is
 * the reverse.
 *
 * <p><b>Part E is C5, measured where the reader stands</b> - on what the service serves after the
 * merge, not on the files, because under putIfAbsent a file's text can be shadowed by an earlier
 * one's. The master list's figure was "260 pairs at 66%" and it reproduces exactly: 182 pairs in
 * {@code composite_aspects_expanded.json} and 78 in {@code composite_asteroid_pairs.json}. <b>It
 * also found what the list never counted</b>: the composite transit readings carry the same defect
 * over roughly three times as many pairs. It is red on purpose and its count is in
 * {@code known-red.txt}; the fix is prose, which this app does not write (DECISIONS.md, the C7
 * reason), so the count comes down as replacement readings arrive.
 */
public final class CorpusCheck {

    private CorpusCheck() { }

    /** The manifest, beside the files it describes so a packaged app carries it too. */
    static final String MANIFEST = InterpretationService.DATA_DIR + "provenance.tsv";

    /** Who wrote a file. Closed, so a typo is a failure rather than a fifth category. */
    static final Set<String> WRITERS = Set.of("david", "agent", "external", "unknown");

    /** What shape a file is: each entry its own text, or entries built from shared parts. */
    static final Set<String> SHAPES = Set.of("written", "composed");

    /** Where written ends and composed begins, in the share of sentences repeated (Part B). */
    static final double COMPOSED_AT = 0.10;

    /** A sentence in this many entries of one file is a part, not something written for one. */
    static final int REPEATS = 3;

    /** Shorter than this and a sentence is a formula ("Keywords: ...") rather than prose. */
    static final int MIN_WORDS = 6;

    /** A square and trine sharing at least this much of their wording read as one text (Part E). */
    static final double SAME_TEXT = 0.5;

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        Map<String, String[]> rows = manifest();
        part("A: every file the app opens says where it came from", () -> declared(rows));
        part("B: a file's declared shape is the shape it measures", () -> shapes(rows));
        part("C: the measure can tell the two shapes apart", CorpusCheck::measureWorks);
        part("D: the similarity measure can tell two texts apart", CorpusCheck::similarityWorks);
        part("E: a square and a trine of the same pair read differently (C5)",
            CorpusCheck::squareTrine);
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- the manifest

    /**
     * file -> {writer, shape, note}. Blank lines and lines starting # are skipped; anything else
     * must have four tab-separated fields, and a line that does not is kept as a malformed row so
     * Part A can name it rather than the reader dropping it quietly.
     */
    static Map<String, String[]> manifest() throws Exception {
        Map<String, String[]> out = new LinkedHashMap<>();
        File f = new File(MANIFEST);
        if (!f.isFile()) {
            return out;
        }
        for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cols = line.split("\t", -1);
            String[] row = new String[3];
            for (int i = 0; i < 3; i++) {
                row[i] = i + 1 < cols.length ? cols[i + 1].trim() : "";
            }
            if (cols.length != 4) {
                row[0] = "malformed (" + cols.length + " fields)";
            }
            out.put(cols[0].trim(), row);
        }
        return out;
    }

    // ---------------------------------------------------------------- Part A

    private static void declared(Map<String, String[]> rows) {
        ok("the manifest exists at " + MANIFEST, new File(MANIFEST).isFile());
        Set<String> opened = new LinkedHashSet<>(List.of(InterpretationService.everyFileName()));
        for (String name : opened) {
            String[] row = rows.get(name);
            ok(name + " has a provenance row", row != null);
            if (row == null) {
                continue;
            }
            ok(name + ": writer is one of " + WRITERS + " (got '" + row[0] + "')",
                WRITERS.contains(row[0]));
            ok(name + ": shape is one of " + SHAPES + " (got '" + row[1] + "')",
                SHAPES.contains(row[1]));
            // The note is where "external" names its source and "unknown" says what is unknown.
            // A bare "unknown" is the state PROVENANCE.md was written to end.
            ok(name + ": the note says how this is known", !row[2].isEmpty());
        }
        // The other direction: a row for a file the app no longer opens describes nothing, and
        // a manifest that keeps rows for deleted files stops being read as a statement of fact.
        for (String name : rows.keySet()) {
            ok("the row for " + name + " names a file the app opens", opened.contains(name));
        }
        int unknown = 0;
        for (String[] row : rows.values()) {
            if ("unknown".equals(row[0])) {
                unknown++;
            }
        }
        System.out.println("  (" + unknown + " of " + rows.size()
            + " files have a writer nobody has recorded - see PROVENANCE.md)");
    }

    // ---------------------------------------------------------------- Part B

    private static void shapes(Map<String, String[]> rows) throws Exception {
        for (String name : InterpretationService.everyFileName()) {
            String[] row = rows.get(name);
            File f = new File(InterpretationService.DATA_DIR + name);
            if (row == null || !f.isFile()) {
                continue;    // Part A has already said so
            }
            List<String> texts = new ArrayList<>();
            strings(Json.parse(f), texts);
            double share = repeatedShare(texts);
            String measured = share >= COMPOSED_AT ? "composed" : "written";
            System.out.printf("  %-60s %6d entries  repeated %.3f  %s%n",
                name, texts.size(), share, measured);
            ok(name + " is declared " + row[1] + " and measures " + measured
                + String.format(" (%.3f)", share), measured.equals(row[1]));
        }
    }

    /** Every string anywhere in a parsed document, in order. */
    @SuppressWarnings("unchecked")
    static void strings(Object node, List<String> out) {
        if (node instanceof String) {
            out.add((String) node);
        } else if (node instanceof Map) {
            for (Object v : ((Map<String, Object>) node).values()) {
                strings(v, out);
            }
        } else if (node instanceof List) {
            for (Object v : (List<Object>) node) {
                strings(v, out);
            }
        }
    }

    /**
     * The share of sentences, counted once per entry, that recur in {@link #REPEATS} or more
     * entries. 0 for a file where every entry was written as itself; high for one assembled from
     * a stock of parts.
     */
    static double repeatedShare(List<String> texts) {
        List<Set<String>> per = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (String t : texts) {
            Set<String> s = new HashSet<>(sentences(t));
            per.add(s);
            for (String x : s) {
                seen.merge(x, 1, Integer::sum);
            }
        }
        int all = 0;
        int repeated = 0;
        for (Set<String> s : per) {
            for (String x : s) {
                all++;
                if (seen.get(x) >= REPEATS) {
                    repeated++;
                }
            }
        }
        return all == 0 ? 0 : (double) repeated / all;
    }

    static List<String> sentences(String text) {
        String plain = text.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        List<String> out = new ArrayList<>();
        for (String s : plain.split("(?<=[.!?])\\s+")) {
            if (s.trim().split(" ").length >= MIN_WORDS) {
                out.add(s.trim());
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- Part C

    /**
     * The measure on texts whose answer is known by construction. Without this a measure that
     * always returned 0 would pass every file declared written and fail only the composed ones -
     * and a manifest edited to call everything written would then go green.
     */
    private static void measureWorks() {
        List<String> composed = new ArrayList<>();
        List<String> written = new ArrayList<>();
        String[] names = {"Mars", "Venus", "Saturn", "Jupiter"};
        for (String n : names) {
            composed.add(n + " is here. This placement asks the pair to work through what they share."
                + " The contact is a structural feature of the relationship as a whole.");
            written.add("On the morning " + n + " rose the harbour was already full of ships."
                + " Nobody in town had expected " + n + " to carry the argument that far.");
        }
        double c = repeatedShare(composed);
        double w = repeatedShare(written);
        ok(String.format("four entries sharing two sentences measure composed (%.3f)", c),
            c >= COMPOSED_AT);
        ok(String.format("four entries each written as itself measure written (%.3f)", w),
            w < COMPOSED_AT);
        ok("a sentence shorter than " + MIN_WORDS + " words is not counted",
            sentences("Mars is here.").isEmpty());
        ok("markup is not part of a sentence",
            sentences("<b>One two three four five six.</b>").get(0).equals("One two three four five six."));
    }

    // ---------------------------------------------------------------- Part D

    private static void similarityWorks() {
        String a = "the pair builds something lasting through steady and patient effort together";
        String b = "the pair builds something lasting through friction and repeated effort together";
        String c = "a sudden storm scatters the fleet before anyone can reach the harbour wall";
        ok("identical texts measure 1", similarity(a, a) == 1.0);
        ok(String.format("texts sharing most of their words measure at least %.1f (%.3f)",
            SAME_TEXT, similarity(a, b)), similarity(a, b) >= SAME_TEXT);
        ok(String.format("unrelated texts measure below %.1f (%.3f)", SAME_TEXT, similarity(a, c)),
            similarity(a, c) < SAME_TEXT);
        ok("the measure does not depend on which text comes first",
            similarity(a, b) == similarity(b, a));
    }

    /**
     * Shared wording, 0 to 1: twice the longest common word sequence over the two lengths. Word
     * ORDER counts, which is what makes a template visible - two readings built from the same
     * paragraph share its sentences in its order, where two independently written ones only
     * share vocabulary.
     */
    static double similarity(String x, String y) {
        String[] a = words(x);
        String[] b = words(y);
        if (a.length + b.length == 0) {
            return 1.0;
        }
        int[] prev = new int[b.length + 1];
        int[] cur = new int[b.length + 1];
        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                cur[j] = a[i - 1].equals(b[j - 1]) ? prev[j - 1] + 1
                    : Math.max(prev[j], cur[j - 1]);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return 2.0 * prev[b.length] / (a.length + b.length);
    }

    private static String[] words(String text) {
        String plain = text.replaceAll("<[^>]+>", " ").toLowerCase().trim();
        return plain.isEmpty() ? new String[0] : plain.split("\\s+");
    }

    // ---------------------------------------------------------------- Part E

    /**
     * One assertion per pair the reader can be served, so the count in known-red.txt falls by
     * one for every pair that gets its own reading. The sections are taken from the files the
     * app opens rather than listed here - a list would be a second copy of sectionFor.
     */
    private static void squareTrine() throws Exception {
        InterpretationService svc = InterpretationService.getInstance();
        Set<String> sections = new LinkedHashSet<>();
        for (String name : InterpretationService.everyFileName()) {
            File f = new File(InterpretationService.DATA_DIR + name);
            if (!f.isFile()) {
                continue;
            }
            Object doc = Json.parse(f);
            if (doc instanceof Map) {
                for (Map.Entry<String, Object> e : Json.object(doc).entrySet()) {
                    if (e.getValue() instanceof Map) {
                        sections.add(e.getKey());
                    }
                }
            }
        }
        int pairs = 0;
        for (String section : sections) {
            Map<String, String> served = svc.servedSection(section);
            if (served == null) {
                continue;
            }
            int here = 0;
            int same = 0;
            for (Map.Entry<String, String> e : served.entrySet()) {
                if (!e.getKey().contains("_square_")) {
                    continue;
                }
                String trine = served.get(e.getKey().replace("_square_", "_trine_"));
                if (trine == null) {
                    continue;
                }
                here++;
                double s = similarity(e.getValue(), trine);
                if (s >= SAME_TEXT) {
                    same++;
                }
                // Printed only when it fails: a thousand passing lines would bury the ones
                // somebody needs to rewrite.
                check(section + " " + e.getKey() + " and its trine share "
                    + Math.round(100 * s) + "% of their wording", s < SAME_TEXT);
            }
            if (here > 0) {
                System.out.printf("  %-28s %5d square/trine pairs, %5d read as one text%n",
                    section, here, same);
            }
            pairs += here;
        }
        ok("the served corpus has square/trine pairs to compare (" + pairs + ")", pairs > 0);
    }

    // ---------------------------------------------------------------- harness

    @FunctionalInterface
    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + name.charAt(0) + ": "
            + (failures.size() == before ? "PASS" : (failures.size() - before) + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String what, boolean pass) {
        check(what, pass);
        System.out.println("  " + (pass ? "ok  " : "FAIL") + "  " + what);
    }

    /** An assertion that is counted and recorded but only printed in the summary. */
    private static void check(String what, boolean pass) {
        checks++;
        if (!pass) {
            failures.add(what);
        }
    }
}
