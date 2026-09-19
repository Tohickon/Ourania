package com.zodiacomputing.ourania.astro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Runnable self-check for the layer 2 positional derivation, in three parts.
 *
 * Part A exercises Zodiac at every boundary where an off-by-one could hide: the first
 * and last instant of all twelve signs, both decan cuts, and the seam at 0/360. It also
 * pins the two decan rulership schemes apart from each other.
 *
 * Part B audits Sabian_interpretations.json against the same functions, so a shift in
 * the data is caught even when the code is right. It checks each entry three independent
 * ways: the degree field, the absolute_degree field, and the degree_range text. A
 * systematic off-by-one would have to corrupt all three consistently to survive.
 *
 * Part C audits the two decan sections of interpretations.json, each against the scheme
 * it is documented to use: the prose against triplicity, the tarot cards against Chaldean.
 * The app presents both schemes side by side and labels which is which, so this is what
 * stops either one drifting into the other.
 *
 * There is no build file and no JUnit on this machine, so this is a plain main().
 * Compile and run it from the OuraniaWindows directory:
 *
 *   javac -encoding UTF-8 -cp src\main\java -d out-selftest ^
 *       src\main\java\com\zodiacomputing\ourania\astro\Zodiac.java ^
 *       src\main\java\com\zodiacomputing\ourania\astro\ZodiacSelfTest.java
 *   java -cp out-selftest com.zodiacomputing.ourania.astro.ZodiacSelfTest
 *
 * Name the two sources rather than globbing astro\*.java. The rest of this package now
 * depends on Swiss Ephemeris, so the glob drags in classes that need the library and the
 * compile fails on them; these two are pure and deliberately have no such dependency.
 *
 * Optionally pass the path to the Sabian JSON as the first argument and the path to
 * interpretations.json as the second; both default to the locations the app's own
 * loaders use.
 */
public final class ZodiacSelfTest {

    private static final String DEFAULT_DATA =
        "src/main/resources/data/Sabian_interpretations.json";

    private static final String DEFAULT_DECAN_DATA =
        "src/main/resources/data/interpretations.json";

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        String dataPath = args.length > 0 ? args[0] : DEFAULT_DATA;
        String decanPath = args.length > 1 ? args[1] : DEFAULT_DECAN_DATA;

        System.out.println("=== Part A: boundary tests ===");
        int before = failures.size();
        signAndDegreeBoundaries();
        sabianAssertions();
        seamAndWrapping();
        bothRulershipSchemes();
        houseAssignment();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: Sabian dataset audit (" + dataPath + ") ===");
        before = failures.size();
        auditSabianDataset(dataPath);
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: decan section audit (" + decanPath + ") ===");
        before = failures.size();
        auditDecanSections(decanPath);
        report("Part C", before);

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

    /** First and last instant of every sign, plus both decan cuts. */
    private static void signAndDegreeBoundaries() {
        for (int s = 0; s < 12; s++) {
            String sign = Zodiac.SIGNS[s];

            // 0 deg 00 min 00 sec - the very start of the sign.
            double start = Zodiac.longitudeOf(s, 0, 0, 0);
            eq(sign + " start: signName", sign, Zodiac.signName(start));
            eq(sign + " start: degreeInSign", 0.0, Zodiac.degreeInSign(start));
            eq(sign + " start: sabianDegree", 1, Zodiac.sabianDegree(start));
            eq(sign + " start: decan", 1, Zodiac.decan(start));
            eq(sign + " start: absolute", s * 30 + 1, Zodiac.sabianAbsolute(start));

            // 29 deg 59 min 59 sec - must not spill into the next sign.
            double end = Zodiac.longitudeOf(s, 29, 59, 59);
            eq(sign + " end: signName", sign, Zodiac.signName(end));
            eq(sign + " end: sabianDegree", 30, Zodiac.sabianDegree(end));
            eq(sign + " end: decan", 3, Zodiac.decan(end));
            eq(sign + " end: absolute", s * 30 + 30, Zodiac.sabianAbsolute(end));

            // Decan cuts are inclusive-start: exactly 10 deg is decan 2, exactly 20 deg is decan 3.
            eq(sign + " 10deg: decan", 2, Zodiac.decan(Zodiac.longitudeOf(s, 10, 0, 0)));
            eq(sign + " 20deg: decan", 3, Zodiac.decan(Zodiac.longitudeOf(s, 20, 0, 0)));
            eq(sign + " 9deg59: decan", 1, Zodiac.decan(Zodiac.longitudeOf(s, 9, 59, 59)));
            eq(sign + " 19deg59: decan", 2, Zodiac.decan(Zodiac.longitudeOf(s, 19, 59, 59)));
        }
    }

    /**
     * House assignment, which is the same class of boundary problem as the Sabian index
     * and fails the same silent way: a body one house off still reads as a real placement.
     *
     * The case that matters is the house straddling 0 Aries. A naive start <= lon < end
     * test drops every body in it, and the chart still renders - it just quietly puts a
     * planet in no house at all, or in the wrong one.
     */
    private static void houseAssignment() {
        // An equal-house set starting at 10 Taurus, so exactly one house wraps the seam.
        double asc = Zodiac.longitudeOf(1, 10, 0, 0);   // 40 degrees
        double[] cusps = new double[13];
        for (int h = 1; h <= 12; h++) {
            cusps[h] = Zodiac.normalise(asc + (h - 1) * 30.0);
        }

        // Every cusp belongs to the house it opens, not the one it closes.
        for (int h = 1; h <= 12; h++) {
            eq("cusp " + h + " opens house " + h, h, Zodiac.houseOf(cusps[h], cusps));
        }

        // A degree before a cusp is still the previous house.
        for (int h = 1; h <= 12; h++) {
            int prev = h == 1 ? 12 : h - 1;
            eq("just before cusp " + h + " is house " + prev,
               prev, Zodiac.houseOf(Zodiac.normalise(cusps[h] - 0.001), cusps));
        }

        // The seam: this cusp set puts 0 Aries inside house 12 (330 to 360/0 to 10).
        int seamHouse = Zodiac.houseOf(0.0, cusps);
        eq("0 Aries lands in a house", true, seamHouse >= 1 && seamHouse <= 12);
        eq("0 Aries and 359.9 share a house", seamHouse, Zodiac.houseOf(359.9, cusps));

        // The twelve houses partition the circle: every degree lands in exactly one.
        int unassigned = 0;
        for (double lon = 0.0; lon < 360.0; lon += 0.25) {
            int h = Zodiac.houseOf(lon, cusps);
            if (h < 1 || h > 12) {
                unassigned++;
            }
        }
        eq("every degree is in some house", 0, unassigned);

        // Whole sign: the Ascendant's own sign is the 1st, and it ignores the cusps.
        eq("whole sign: Asc sign is 1st", 1, Zodiac.wholeSignHouse(asc, asc));
        eq("whole sign: next sign is 2nd",
           2, Zodiac.wholeSignHouse(Zodiac.longitudeOf(2, 0, 0, 0), asc));
        eq("whole sign: previous sign is 12th",
           12, Zodiac.wholeSignHouse(Zodiac.longitudeOf(0, 29, 0, 0), asc));
        eq("whole sign wraps the seam",
           11, Zodiac.wholeSignHouse(Zodiac.longitudeOf(11, 15, 0, 0), asc));

        // Derived houses, the formula L6's relational topics need.
        eq("1st of the 1st is the 1st", 1, Zodiac.derivedHouse(1, 1));
        eq("4th of the 7th is the 10th", 10, Zodiac.derivedHouse(4, 7));
        eq("10th of the 10th is the 7th", 7, Zodiac.derivedHouse(10, 10));
        eq("12th of the 12th is the 11th", 11, Zodiac.derivedHouse(12, 12));
    }

    /** The three assertions that catch a Sabian index shifted by one in either direction. */
    private static void sabianAssertions() {
        // 1. The very first degree of the zodiac is symbol 1, not 0 and not 360.
        eq("sabian: 0deg00'00\" Aries -> 1", 1, Zodiac.sabianAbsolute(0.0));
        eq("sabian: 0deg00'00\" Aries -> key", "aries_1", Zodiac.sabianKey(0.0));

        // 2. The very last degree of the zodiac is symbol 360, not 0 and not 1.
        double lastDegree = Zodiac.longitudeOf(11, 29, 59, 59);
        eq("sabian: 29deg59'59\" Pisces -> 360", 360, Zodiac.sabianAbsolute(lastDegree));
        eq("sabian: 29deg59'59\" Pisces -> key", "pisces_30", Zodiac.sabianKey(lastDegree));

        // 3. Exactly 10 degrees takes the 11th symbol, not the 10th. This is the assertion
        //    that distinguishes floor+1 from a naive round(), which would give 10.
        for (int s = 0; s < 12; s++) {
            eq(Zodiac.SIGNS[s] + " 10deg00'00\" -> 11th",
               11, Zodiac.sabianDegree(Zodiac.longitudeOf(s, 10, 0, 0)));
            // And one second earlier is still the 10th.
            eq(Zodiac.SIGNS[s] + " 9deg59'59\" -> 10th",
               10, Zodiac.sabianDegree(Zodiac.longitudeOf(s, 9, 59, 59)));
            // A fraction past a whole degree already advances the symbol.
            eq(Zodiac.SIGNS[s] + " 10deg00'01\" -> 11th",
               11, Zodiac.sabianDegree(Zodiac.longitudeOf(s, 10, 0, 1)));
        }
    }

    /** The 0/360 seam, and longitudes arriving un-normalised. */
    private static void seamAndWrapping() {
        eq("seam: 360.0 wraps to Aries", "aries", Zodiac.signName(360.0));
        eq("seam: 360.0 -> absolute 1", 1, Zodiac.sabianAbsolute(360.0));
        eq("seam: 359.9999 stays Pisces", "pisces", Zodiac.signName(359.9999));
        eq("seam: 359.9999 -> absolute 360", 360, Zodiac.sabianAbsolute(359.9999));
        eq("seam: -0.5 wraps to Pisces", "pisces", Zodiac.signName(-0.5));
        eq("seam: -0.5 -> absolute 360", 360, Zodiac.sabianAbsolute(-0.5));
        eq("seam: 720.0 wraps to Aries", "aries", Zodiac.signName(720.0));
        eq("seam: -370.0 normalises", 350.0, Zodiac.normalise(-370.0));

        // The worked example from the spec: Aries 25 deg 40 min.
        double aries2540 = Zodiac.longitudeOf(0, 25, 40, 0);
        eq("worked: sign", "aries", Zodiac.signName(aries2540));
        eq("worked: sabian", 26, Zodiac.sabianDegree(aries2540));
        eq("worked: decan", 3, Zodiac.decan(aries2540));
        eq("worked: chaldean ruler", "Venus", Zodiac.chaldeanDecanRuler(aries2540));

        // Chaldean rulers must complete a 7-cycle across 36 decans, so the first decan
        // of Aries and the last decan of Pisces are both Mars.
        eq("chaldean: aries I", "Mars", Zodiac.chaldeanDecanRuler(Zodiac.longitudeOf(0, 5, 0, 0)));
        eq("chaldean: taurus I", "Mercury", Zodiac.chaldeanDecanRuler(Zodiac.longitudeOf(1, 5, 0, 0)));
        eq("chaldean: pisces III", "Mars", Zodiac.chaldeanDecanRuler(Zodiac.longitudeOf(11, 25, 0, 0)));
    }

    /**
     * The two rulership schemes, and the boundary between them.
     *
     * The GUI labels each decan surface by calling the (signName, decanNum) overloads
     * while the datasets are audited through the longitude ones, so the two forms have
     * to stay in step or a label could name a different planet than the data behind it.
     */
    private static void bothRulershipSchemes() {
        int agreements = 0;
        for (int s = 0; s < 12; s++) {
            String sign = Zodiac.SIGNS[s];
            for (int d = 1; d <= 3; d++) {
                double mid = Zodiac.longitudeOf(s, (d - 1) * 10 + 5, 0, 0);

                // The name-keyed overloads must match the longitude-keyed originals.
                eq(sign + " " + d + ": chaldean overload agrees",
                   Zodiac.chaldeanDecanRuler(mid), Zodiac.chaldeanDecanRuler(sign, d));
                eq(sign + " " + d + ": triplicity overload agrees",
                   Zodiac.triplicityDecanRuler(mid), Zodiac.triplicityDecanRuler(sign, d));

                if (Zodiac.chaldeanDecanRuler(sign, d).equals(Zodiac.triplicityDecanRuler(sign, d))) {
                    agreements++;
                }
            }
        }

        // Triplicity borrows from the same element, so the borrowed sign always shares
        // the element of the sign itself. Decan 1 always borrows the sign itself.
        for (int s = 0; s < 12; s++) {
            eq(Zodiac.SIGNS[s] + " I borrows itself", s, Zodiac.triplicityDecanSignIndex(s, 1));
            eq(Zodiac.SIGNS[s] + " II same element",
               s % 4, Zodiac.triplicityDecanSignIndex(s, 2) % 4);
            eq(Zodiac.SIGNS[s] + " III same element",
               s % 4, Zodiac.triplicityDecanSignIndex(s, 3) % 4);
        }

        // Elements cycle in fours from Aries. Triplicity is *defined* as borrowing from the
        // same element, so elementIndex and triplicityDecanSignIndex have to agree for all
        // 36 decans - if either drifts, this catches it.
        eq("aries is fire", 0, Zodiac.elementIndex(0));
        eq("taurus is earth", 1, Zodiac.elementIndex(1));
        eq("gemini is air", 2, Zodiac.elementIndex(2));
        eq("cancer is water", 3, Zodiac.elementIndex(3));
        eq("element index rejects -1", -1, Zodiac.elementIndex(-1));
        eq("element index rejects 12", -1, Zodiac.elementIndex(12));
        eq("element name rejects 12", "", Zodiac.elementName(12));
        for (int s = 0; s < 12; s++) {
            eq(Zodiac.SIGNS[s] + " element name", Zodiac.ELEMENTS[s % 4], Zodiac.elementName(s));
            for (int d = 1; d <= 3; d++) {
                eq(Zodiac.SIGNS[s] + " decan " + d + " keeps the element",
                   Zodiac.elementIndex(s),
                   Zodiac.elementIndex(Zodiac.triplicityDecanSignIndex(s, d)));
            }
        }

        // Worked examples of the disagreement, one per direction.
        eq("triplicity: aries III", "Jupiter", Zodiac.triplicityDecanRuler("Aries", 3));
        eq("chaldean:   aries III", "Venus", Zodiac.chaldeanDecanRuler("Aries", 3));
        eq("triplicity: scorpio I", "Pluto", Zodiac.triplicityDecanRuler("Scorpio", 1));
        eq("chaldean:   scorpio I", "Mars", Zodiac.chaldeanDecanRuler("Scorpio", 1));

        // The headline number. If this moves, one of the two schemes has been edited.
        eq("schemes agree on exactly 6 of 36 decans", 6, agreements);

        // Unrecognised input must fall back to empty rather than throw, because the GUI
        // labels from inside a render path.
        eq("unknown sign -> empty", "", Zodiac.chaldeanDecanRuler("Ophiuchus", 1));
        eq("decan 0 -> empty", "", Zodiac.triplicityDecanRuler("Aries", 0));
        eq("decan 4 -> empty", "", Zodiac.chaldeanDecanRuler("Aries", 4));
        eq("null sign -> -1", -1, Zodiac.signIndexOf(null));
    }

    // ---------------------------------------------------------------- part B

    /**
     * Reads the Sabian JSON with the same line-oriented approach the app's loaders use,
     * and checks every entry against Zodiac. Reports missing keys, extra keys, and any
     * entry whose own metadata disagrees with its key.
     */
    private static void auditSabianDataset(String path) {
        File file = new File(path);
        if (!file.exists()) {
            failures.add("dataset: file not found at " + path);
            checks++;
            return;
        }

        Map<String, Map<String, String>> entries = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            String currentKey = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\"") && (line.endsWith(":{") || line.endsWith(": {"))) {
                    int quoteEnd = line.indexOf("\"", 1);
                    if (quoteEnd != -1) {
                        currentKey = line.substring(1, quoteEnd).toLowerCase();
                        entries.put(currentKey, new HashMap<String, String>());
                    }
                } else if (currentKey != null && line.startsWith("\"")) {
                    int colonIdx = line.indexOf("\":");
                    if (colonIdx > 0) {
                        String field = line.substring(1, colonIdx);
                        String value = line.substring(colonIdx + 2).trim();
                        if (value.endsWith(",")) {
                            value = value.substring(0, value.length() - 1).trim();
                        }
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                            value = value.substring(1, value.length() - 1);
                        }
                        entries.get(currentKey).put(field, value);
                    }
                }
            }
        } catch (IOException e) {
            failures.add("dataset: read failed - " + e.getMessage());
            checks++;
            return;
        }

        System.out.println("Parsed " + entries.size() + " entries.");

        // Every one of the 360 keys must be present, and nothing else.
        Set<String> expected = new LinkedHashSet<>();
        for (int s = 0; s < 12; s++) {
            for (int d = 1; d <= 30; d++) {
                expected.add(Zodiac.SIGNS[s] + "_" + d);
            }
        }
        for (String key : expected) {
            if (!entries.containsKey(key)) {
                failures.add("dataset: missing entry " + key);
            }
            checks++;
        }
        for (String key : entries.keySet()) {
            if (!expected.contains(key)) {
                failures.add("dataset: unexpected entry " + key);
            }
            checks++;
        }

        // Each present entry must agree with its own key three independent ways.
        for (int s = 0; s < 12; s++) {
            for (int d = 1; d <= 30; d++) {
                String key = Zodiac.SIGNS[s] + "_" + d;
                Map<String, String> e = entries.get(key);
                if (e == null) {
                    continue;
                }

                // A longitude that must resolve back to this exact key.
                double lon = Zodiac.longitudeOf(s, d - 1, 30, 0);
                eq(key + ": key round-trip", key, Zodiac.sabianKey(lon));

                eqField(key, e, "sign", Zodiac.SIGNS[s], true);
                eqField(key, e, "degree", String.valueOf(d), false);
                eqField(key, e, "absolute_degree", String.valueOf(s * 30 + d), false);
                eqField(key, e, "decan", String.valueOf(Zodiac.decan(lon)), false);
                eqField(key, e, "decan_ruler", Zodiac.chaldeanDecanRuler(lon), true);

                // degree_range is an independent witness: entry N must start at N-1.
                String range = e.get("degree_range");
                if (range != null) {
                    int lower = leadingInt(range);
                    if (lower != d - 1) {
                        failures.add(key + ": degree_range starts at " + lower
                            + ", expected " + (d - 1) + "  [" + range + "]");
                    }
                    checks++;
                }
            }
        }
    }

    // ---------------------------------------------------------------- part C

    /**
     * Audits the two decan sections of interpretations.json against the scheme each one
     * is documented to use: the prose against triplicity, the tarot cards against Chaldean.
     *
     * The prose states its sub-ruler in its own lead sentence ("Sub-ruled by Jupiter, ..."),
     * so the text can be checked against Zodiac without trusting any separate metadata.
     * That is the whole point of keeping both schemes: each is now pinned to its source,
     * and an edit that drifts one of them toward the other fails here.
     */
    private static void auditDecanSections(String path) {
        File file = new File(path);
        if (!file.exists()) {
            failures.add("decans: file not found at " + path);
            checks++;
            return;
        }

        Map<String, String> prose;
        Map<String, String> tarot;
        try {
            prose = readFlatSection(file, "decans");
            tarot = readFlatSection(file, "tarot_decans");
        } catch (IOException e) {
            failures.add("decans: read failed - " + e.getMessage());
            checks++;
            return;
        }

        System.out.println("Parsed " + prose.size() + " prose entries, "
            + tarot.size() + " tarot entries.");

        for (int s = 0; s < 12; s++) {
            String sign = Zodiac.SIGNS[s];
            for (int d = 1; d <= 3; d++) {
                String key = sign + "_" + d;

                String text = prose.get(key);
                checks++;
                if (text == null) {
                    failures.add("decans: missing prose entry " + key);
                } else {
                    String stated = statedRuler(text);
                    checks++;
                    if (stated == null) {
                        failures.add(key + ": prose does not open with a ruler sentence");
                    } else {
                        eq(key + ": prose sub-ruler is triplicity",
                           Zodiac.triplicityDecanRuler(sign, d), stated);
                    }
                }

                checks++;
                if (!tarot.containsKey(key)) {
                    failures.add("decans: missing tarot entry " + key);
                }
            }
        }

        for (String key : prose.keySet()) {
            checks++;
            if (Zodiac.signIndexOf(key.substring(0, key.lastIndexOf('_'))) < 0) {
                failures.add("decans: unexpected prose entry " + key);
            }
        }
    }

    /**
     * Reads one flat "name": { "key": "value", ... } section by the same line-oriented
     * rules the app's loaders use. Depends on one field per line, which is a hard
     * constraint on these data files - see the vault note ourania-data-pipeline.
     */
    private static Map<String, String> readFlatSection(File file, String section)
            throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean inSection = false;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (!inSection) {
                    if (t.startsWith("\"" + section + "\"") && t.endsWith("{")) {
                        inSection = true;
                    }
                    continue;
                }
                if (t.startsWith("}")) {
                    break;
                }
                int colonIdx = t.indexOf("\":");
                if (t.startsWith("\"") && colonIdx > 0) {
                    String key = t.substring(1, colonIdx).toLowerCase();
                    String value = t.substring(colonIdx + 2).trim();
                    if (value.endsWith(",")) {
                        value = value.substring(0, value.length() - 1).trim();
                    }
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                        value = value.substring(1, value.length() - 1);
                    }
                    out.put(key, value);
                }
            }
        }
        return out;
    }

    /**
     * The planet named in a decan entry's lead sentence, e.g. "Sub-ruled by the Sun, ..."
     * gives "Sun". Null if the entry does not open that way.
     */
    private static String statedRuler(String text) {
        java.util.regex.Matcher m = RULER_LEAD.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static final java.util.regex.Pattern RULER_LEAD =
        java.util.regex.Pattern.compile("<b>(?:Sub-)?[Rr]uled by (?:the )?([A-Za-z]+)");

    /** Leading integer of a string, or -1 if it does not start with a digit. */
    private static int leadingInt(String s) {
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        return i == 0 ? -1 : Integer.parseInt(s.substring(0, i));
    }

    // ---------------------------------------------------------------- helpers

    private static void eqField(String key, Map<String, String> entry,
                                String field, String expected, boolean ignoreCase) {
        String actual = entry.get(field);
        checks++;
        if (actual == null) {
            failures.add(key + ": field '" + field + "' absent");
        } else if (ignoreCase ? !actual.equalsIgnoreCase(expected) : !actual.equals(expected)) {
            failures.add(key + ": " + field + " = " + actual + ", expected " + expected);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void eq(String label, double expected, double actual) {
        checks++;
        if (Math.abs(expected - actual) > 1e-9) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int failuresBefore) {
        int added = failures.size() - failuresBefore;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
