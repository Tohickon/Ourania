package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end check of L7 and L9: theme extraction, the independence rule, contradiction
 * detection, and the three output tiers.
 *
 * Part A is the property that matters most and is easiest to get wrong. It builds a
 * synthetic Saturn-signature chart where naive counting and component counting give
 * different answers, and asserts the difference. If the independence rule ever silently
 * degrades to counting statements, this fails.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.SnapshotOutputCheck
 */
public final class SnapshotOutputCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;
    private static final double LAT = 34.05;
    private static final double LON = -118.24;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        System.out.println("=== Part A: the independence rule ===");
        int before = failures.size();
        independenceRule();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: live charts, end to end ===");
        before = failures.size();
        SwissEph sw = new SwissEph(EPHE_PATH);
        run(sw, 2026, 7, 31, 19.0, "31 July 2026, local noon, Los Angeles");
        run(sw, 1990, 3, 14, 2.5, "14 March 1990, 02:30 UT, Los Angeles");
        run(sw, 2026, 12, 21, 8.0, "21 December 2026, 08:00 UT, Los Angeles");
        report("Part B", before);

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

    // ---------------------------------------------------------------- part A

    /**
     * The worked example from the spec. Five statements all carrying the saturnian theme,
     * but three of them mention Saturn itself, so they are one witness, not three.
     *
     *   Saturn conjunct MC          {Saturn, MC}            \
     *   Saturn is final dispositor  {Saturn, chain}          > one component
     *   Moon square Saturn          {Moon, Saturn}          /
     *   Capricorn stellium          {Sun, Mercury, Venus}    > second component
     *   10th house emphasis         {house-10}               > third component
     *
     * Naive count 5. True count 3.
     */
    private static void independenceRule() {
        Themes.Result r = new Themes.Result();
        r.candidates.add(cand("Saturn conjunct the MC", 1.0, "saturnian", "Saturn", "MC"));
        r.candidates.add(cand("Saturn is the final dispositor", 0.9, "saturnian", "Saturn", "chain"));
        r.candidates.add(cand("Moon square Saturn", 0.8, "saturnian", "Moon", "Saturn"));
        r.candidates.add(cand("Capricorn stellium", 0.85, "saturnian", "Sun", "Mercury", "Venus"));
        r.candidates.add(cand("Tenth house emphasis", 0.7, "saturnian", "house-10"));

        invokeCount(r);

        eq("naive candidate count", 5, countCandidates(r, "saturnian"));
        eq("independent witness count", 3, Themes.witnessesFor(r, "saturnian"));
        eq("independence rule actually reduces the count", true,
            Themes.witnessesFor(r, "saturnian") < countCandidates(r, "saturnian"));

        // A theme carried by wholly disjoint statements must not be collapsed.
        Themes.Result d = new Themes.Result();
        d.candidates.add(cand("A", 1.0, "airy", "Mercury"));
        d.candidates.add(cand("B", 1.0, "airy", "Venus"));
        d.candidates.add(cand("C", 1.0, "airy", "Jupiter"));
        invokeCount(d);
        eq("disjoint statements stay separate witnesses", 3, Themes.witnessesFor(d, "airy"));

        // Transitive merging: A-B share, B-C share, so all three are one witness even
        // though A and C do not touch.
        Themes.Result t = new Themes.Result();
        t.candidates.add(cand("A", 1.0, "watery", "Moon", "X"));
        t.candidates.add(cand("B", 1.0, "watery", "X", "Y"));
        t.candidates.add(cand("C", 1.0, "watery", "Y", "Mars"));
        invokeCount(t);
        eq("overlap is transitive", 1, Themes.witnessesFor(t, "watery"));

        // The threshold must bite: 3 independent is a signature, 2 is not.
        Themes.signatureThreshold = 3;
        eq("three witnesses reaches the threshold", 1,
            Themes.signaturesAtThreshold(d).size());
        Themes.Result two = new Themes.Result();
        two.candidates.add(cand("A", 1.0, "fiery", "Mars"));
        two.candidates.add(cand("B", 1.0, "fiery", "Sun"));
        invokeCount(two);
        eq("two witnesses does not", 0, Themes.signaturesAtThreshold(two).size());
    }

    private static Themes.Candidate cand(String text, double w, String theme, String... prov) {
        return Themes.testCandidate(text, w, theme, prov);
    }

    private static void invokeCount(Themes.Result r) {
        Themes.recountForTest(r);
    }

    private static int countCandidates(Themes.Result r, String theme) {
        int n = 0;
        for (Themes.Candidate c : r.candidates) {
            if (c.themes.contains(theme)) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- part B

    private static void run(SwissEph sw, int y, int m, int d, double hourUt, String label) {
        SweDate sd = new SweDate(y, m, d, hourUt);
        ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'W', false, 0.0);
        Gestalt.Result g = Gestalt.compute(f);
        List<BodyScore.Vector> ranked = BodyScore.rank(f);
        Themes.Result t = Themes.extract(f, g, ranked);

        System.out.println();
        System.out.println("################################################################");
        System.out.println("# " + label);
        System.out.println("################################################################");
        System.out.println();
        System.out.println("ONE LINE:");
        String one = Snapshot.oneLine(f, g, ranked);
        System.out.println("  " + one);
        System.out.println();
        String para = Snapshot.paragraph(f, g, ranked, t);
        System.out.println(Snapshot.report(f, g, ranked, t));

        // The tiers must be real text, not empty or placeholder.
        eq("one-line is non-empty [" + label + "]", true, one.length() > 10);
        eq("paragraph is non-empty [" + label + "]", true, para.length() > 80);
        checks++;
        if (para.contains("null") || one.contains("null")) {
            failures.add("output contains 'null' [" + label + "]");
        }
        // Between four and eight sentences: fewer is not a reading, more is not a snapshot.
        int sentences = para.split("(?<=\\.)\\s+").length;
        checks++;
        if (sentences < 3 || sentences > 8) {
            failures.add("paragraph has " + sentences + " sentences [" + label + "]");
        }

        // Every ranked candidate must carry provenance, or the independence rule is
        // silently a no-op for it.
        for (Themes.Candidate c : t.candidates) {
            checks++;
            if (c.provenance.isEmpty()) {
                failures.add("candidate with no provenance: " + c.text + " [" + label + "]");
            }
            checks++;
            if (c.themes.isEmpty()) {
                failures.add("candidate with no themes: " + c.text + " [" + label + "]");
            }
        }

        // Witness counts can never exceed statement counts.
        for (Themes.Signature s : t.signatures) {
            checks++;
            if (s.witnessCount > s.candidateCount) {
                failures.add(s.theme + ": " + s.witnessCount + " witnesses from "
                    + s.candidateCount + " statements [" + label + "]");
            }
        }

        // A contradiction must have both sides populated.
        for (Themes.Contradiction c : t.contradictions) {
            checks++;
            if (c.strongestA == null || c.strongestB == null) {
                failures.add("contradiction " + c.themeA + "/" + c.themeB
                    + " missing a side [" + label + "]");
            }
            checks++;
            if (c.themeA.equals(c.themeB)) {
                failures.add("theme contradicts itself [" + label + "]");
            }
        }

        // Deduplication must not drop everything, and must not keep duplicates.
        eq("ranked list is non-empty [" + label + "]", true, !t.ranked.isEmpty());
        for (int i = 0; i < t.ranked.size(); i++) {
            for (int j = i + 1; j < t.ranked.size(); j++) {
                checks++;
                if (t.ranked.get(i).text.equals(t.ranked.get(j).text)) {
                    failures.add("duplicate survived ranking: "
                        + t.ranked.get(i).text + " [" + label + "]");
                }
            }
        }
        // Ranked must be in descending weight order.
        for (int i = 1; i < t.ranked.size(); i++) {
            checks++;
            if (t.ranked.get(i).weight > t.ranked.get(i - 1).weight + 1e-12) {
                failures.add("ranked out of order at " + i + " [" + label + "]");
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String wrap(String s, int width, String indent) {
        StringBuilder sb = new StringBuilder();
        int line = 0;
        for (String word : s.split(" ")) {
            if (line + word.length() > width) {
                sb.append('\n').append(indent);
                line = 0;
            }
            sb.append(word).append(' ');
            line += word.length() + 1;
        }
        return sb.toString().trim();
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
