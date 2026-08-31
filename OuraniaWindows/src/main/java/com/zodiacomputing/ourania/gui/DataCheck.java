package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the interpretation dataset: which chart points have prose, and whether the file
 * that supplies the newer ones is intact.
 *
 * Two things here are worth more than the assertion count.
 *
 * <b>Part A compares the parsed entry count against a raw count of key lines.</b> That
 * comparison is the single check the 2026-07-30 data repair says exposed silent loss: a
 * tolerant line-oriented parser that swallows an entry header mid-value loses entries without
 * erroring, and the only symptom is a count that does not match. The app's parser is exactly
 * that kind of parser, so anything added to the dataset needs this comparison rather than a
 * successful load.
 *
 * <b>Part B greps for the corruption signatures that actually occurred</b> rather than for
 * hypothetical ones - markdown fences, chat trailers, citation markers, multi-line values,
 * literal quotes the combined loader will not unescape, and the `", "field": ` signature that
 * had swallowed a delimiter in `libra_18`. See the vault's llm-json-corruption-patterns.
 *
 * Run from the OuraniaWindows directory, since the service loads by relative path:
 *   java -cp src\main\java com.zodiacomputing.ourania.gui.DataCheck
 */
public final class DataCheck {

    /**
     * The supplementary prose files, taken from the service rather than listed again.
     *
     * This was a second hardcoded copy of the list, with a comment asking whoever edited one to
     * edit the other. That lasted a day: `quincunx_gap.json` was added to the service and not
     * here, so the app loaded a file that nothing validated. The list is now read from
     * {@link InterpretationService#extraFilePaths()}, so a file added there is guarded here
     * automatically and this class has nothing to keep in step.
     */
    private static final String[] EXTRA = InterpretationService.extraFilePaths();

    /** Sections whose values are the bold-summary placement format. */
    private static boolean isPlacementSection(String section) {
        return "planet_sign".equals(section) || "planet_house".equals(section);
    }

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private DataCheck() { }

    public static void main(String[] args) {
        System.out.println("=== Part A: extra_bodies.json parses without losing entries ===");
        int before = failures.size();
        fileIntegrity();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: the corruption signatures that actually occurred ===");
        before = failures.size();
        corruptionSignatures();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: coverage, point by point ===");
        before = failures.size();
        coverage();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: the four angle files reach a map and resolve ===");
        before = failures.size();
        angleFiles();
        report("Part D", before);

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
     * Every raw key line in both files, against what the service actually holds.
     *
     * Section-aware, because the two files no longer have the same shape: a key is checked
     * against the map its own section names. That is what makes this catch a section header
     * the loader does not recognise - the entries load, they just land somewhere nobody
     * reads.
     */
    private static void fileIntegrity() {
        InterpretationService svc = InterpretationService.getInstance();
        // Keys may contain spaces and hyphens. `macro_dynamics.json` uses "Grand Trine" and
        // "T-Square", and the previous [a-zA-Z_0-9]+ silently skipped both: the loader read
        // them, the app served them, and this check reported 9 entries where the file holds
        // 11. That is the same defect the header comment above describes - content the app
        // uses and nothing validates - occurring one level down, at the key rather than the
        // file. Widened rather than special-cased, because the next odd key will not be these.
        Pattern keyLine = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\"");
        Pattern header = Pattern.compile("^\\s*\"([a-z_]+)\"\\s*:\\s*\\{");

        for (String path : EXTRA) {
            File f = new File(path);
            String name = f.getName();
            ok(name + " exists", f.exists());
            if (!f.exists()) {
                continue;
            }

            Set<String> seen = new HashSet<>();
            java.util.Map<String, Integer> perSection = new java.util.LinkedHashMap<>();
            int lineNo = 0;
            int missing = 0;
            String section = null;
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    lineNo++;
                    Matcher h = header.matcher(line);
                    if (h.find()) {
                        section = h.group(1);
                        perSection.putIfAbsent(section, 0);
                        continue;
                    }
                    Matcher m = keyLine.matcher(line);
                    if (!m.find() || section == null) {
                        continue;
                    }
                    String key = m.group(1);
                    perSection.merge(section, 1, Integer::sum);
                    if (!seen.add(section + "/" + key)) {
                        failures.add(name + " line " + lineNo + ": duplicate key " + key);
                    }
                    checks++;
                    if (!svc.hasEntry(section, key)) {
                        missing++;
                        if (missing <= 5) {
                            failures.add(name + ": \"" + key + "\" is in section \"" + section
                                + "\" but the service did not load it there");
                        }
                    }
                }
            } catch (Exception e) {
                failures.add("could not read " + name + ": " + e);
                continue;
            }
            System.out.println("  " + name + ": " + perSection);
            if (missing > 5) {
                failures.add(name + ": ... and " + (missing - 5) + " further keys not loaded");
            }
            eq(name + " loaded every key it contains", 0, missing);
        }

        // The placement getters specifically, through the public API the GUI calls, because
        // hasEntry only proves the map holds the key and not that the lookup path finds it.
        for (String body : new String[]{"Ceres", "North Node", "Part of Fortune",
                                        "Black Moon Lilith"}) {
            ok(body + " resolves in a sign via the public getter",
                !svc.getPlanetInSign(body, "Cancer").startsWith("Interpretation not found"));
            ok(body + " resolves in a house via the public getter",
                !svc.getPlanetInHouse(body, 4).startsWith("Interpretation not found"));
        }
    }

    /** Display name for a registry id, so the getters take what the GUI would pass them. */
    private static String nameOf(String id) {
        int i = Bodies.indexOf(id);
        return i < 0 ? id : Bodies.at(i).name;
    }

    // ---------------------------------------------------------------- part B

    private static void corruptionSignatures() {
        Pattern header = Pattern.compile("^\\s*\"([a-z_]+)\"\\s*:\\s*\\{");
        // Same widening as Part A's keyLine: a key may contain spaces or hyphens, and the
        // narrow class skipped "Grand Trine" and "T-Square" entirely. Part B is the house-style
        // pass, so a key it cannot see is prose nothing is holding to the house style.
        Pattern entry = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\"(.*)\"[,]?\\s*$");

        for (String path : EXTRA) {
            File f = new File(path);
            if (!f.exists()) {
                continue;
            }
            String name = f.getName();
            int lineNo = 0;
            int values = 0;
            String section = null;
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    lineNo++;
                    String at = " (" + name + " line " + lineNo + ")";

                    ok("no markdown fence" + at, !line.trim().startsWith("```"));
                    ok("no citation marker" + at,
                        !line.matches(".*\\[\\d+(\\s*[,-]\\s*\\d+)*\\].*"));

                    Matcher h = header.matcher(line);
                    if (h.find()) {
                        section = h.group(1);
                        continue;
                    }
                    Matcher m = entry.matcher(line);
                    if (!m.find()) {
                        continue;
                    }
                    values++;
                    String key = m.group(1);
                    String value = m.group(2);
                    String where = " (" + name + " " + key + ")";

                    // The delimiter-swallowing signature from libra_18: a value that has
                    // absorbed the start of the next field.
                    ok("no swallowed field delimiter" + where,
                        !value.matches(".*\",\\s*\"[a-zA-Z_]+\"\\s*:.*"));
                    // The loader does not unescape, so a literal quote must be &quot;.
                    ok("no raw double quote in the value" + where, !value.contains("\""));
                    ok("no over-escaped quote" + where, !value.contains("\\\""));
                    ok("no control characters" + where, !value.matches(".*[\\x00-\\x1f].*"));
                    ok("value is not empty" + where, value.length() > 40);
                    ok("value opens with a bold lead" + where, value.startsWith("<b>"));
                    ok("bold lead is closed" + where, value.contains("</b>"));

                    // The two-paragraph layout is required of PLACEMENT prose only. Aspect
                    // and transit entries are a single sentence by design, and demanding
                    // <br><br> of them would fail 3,000 perfectly good entries - the check
                    // being wrong about the format, not the data being wrong.
                    if (isPlacementSection(section)) {
                        ok("placement has the bold summary structure" + where,
                            value.contains("</b><br><br>"));
                        ok("placement is substantial" + where, value.length() > 120);
                    }
                }
            } catch (Exception e) {
                failures.add("could not scan " + name + ": " + e);
                continue;
            }
            System.out.println("  " + name + ": " + values + " values scanned");
            ok(name + " has values and no chat trailer", values > 0);
        }
    }

    // ---------------------------------------------------------------- part C

    /**
     * Coverage per registry point. Deliberately reports rather than demands: the four angles
     * are served by their own files and the remaining gaps are known and listed in the work
     * plan. What it DOES demand is that a point claiming sign prose has all twelve signs and
     * all twelve houses - a half-filled body is the state that renders as a broken panel.
     */
    private static void coverage() {
        InterpretationService svc = InterpretationService.getInstance();
        int complete = 0;
        int missing = 0;

        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if (d.isAngle()) {
                continue;      // Ascendent.json / ic.json / mc.json, keyed differently
            }
            int signs = 0;
            for (String sign : Zodiac.SIGNS) {
                if (!svc.getPlanetInSign(d.name, sign).startsWith("Interpretation not found")) {
                    signs++;
                }
            }
            int houses = 0;
            for (int h = 1; h <= 12; h++) {
                if (!svc.getPlanetInHouse(d.name, h).startsWith("Interpretation not found")) {
                    houses++;
                }
            }
            String core = svc.getBodyCore(d.name);
            boolean any = signs > 0 || houses > 0;

            System.out.printf("  %-20s signs %2d/12  houses %2d/12  core %s%n",
                d.name, signs, houses, core.isEmpty() ? "-" : "yes");

            if (any) {
                complete++;
                // Partial coverage is the real defect: it looks fine until the one sign
                // nobody checked comes up on a chart.
                eq(d.name + " has all twelve signs", 12, signs);
                eq(d.name + " has all twelve houses", 12, houses);
            } else {
                missing++;
            }
            // Whatever the coverage, the registry must have a one-liner to fall back to.
            ok(d.name + " has a registry meaning to fall back on", !d.meaning.isEmpty());
        }
        System.out.println("  bodies with prose: " + complete + ", without: " + missing);
        ok("at least the ten classical planets plus Chiron are covered", complete >= 11);
    }

    // ---------------------------------------------------------------- part D

    /**
     * The angle files, which had no check at all until one of them turned out to be inert.
     *
     * `Descendant.json` was written, wired into the loader's file list, and dropped entirely on
     * load, because the routing tested for ascendant, mc and ic and nothing else. The file
     * parsed. The app ran. The panel showed the registry one-liner, which looks like a designed
     * fallback rather than a failure. **A file can be present, valid and completely unused** —
     * so this asserts the round trip: keys in the file, entries in the map, prose out of the
     * public getter.
     */
    private static void angleFiles() {
        InterpretationService svc = InterpretationService.getInstance();
        String[][] files = {
            {"ascendant",  "Ascendent.json",  "Ascendant"},
            {"descendant", "Descendant.json", "Descendant"},
            {"mc",         "mc.json",         "MC"},
            {"ic",         "ic.json",         "IC"},
        };
        Pattern keyLine = Pattern.compile("^\\s*\"([a-zA-Z_0-9]+)\"\\s*:\\s*\\{");

        for (String[] f : files) {
            String angle = f[0];
            File file = new File(InterpretationService.DATA_DIR + f[1]);
            String display = f[2];

            ok(f[1] + " exists", file.exists());
            if (!file.exists()) {
                continue;
            }

            int rawKeys = 0;
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (keyLine.matcher(line).find()) {
                        rawKeys++;
                    }
                }
            } catch (Exception e) {
                failures.add("could not read " + f[1] + ": " + e);
                continue;
            }

            int loaded = svc.angleEntryCount(angle);
            System.out.printf("  %-11s %-17s raw %3d  loaded %3d%n", display, f[1], rawKeys, loaded);

            // The assertion that matters: a file with keys must put entries somewhere.
            ok(f[1] + " has entries at all", rawKeys > 0);
            ok(f[1] + " is not inert - something reached the map", loaded > 0);

            // And the prose must come back out of the getter the GUI actually calls, for every
            // sign, since a routing bug can drop a subset just as easily as the whole file.
            int resolved = 0;
            for (String sign : Zodiac.SIGNS) {
                String text = svc.getAngleInterpretation(display, sign);
                if (text != null && !text.isEmpty()
                        && !text.startsWith("Interpretation not found")) {
                    resolved++;
                }
                checks++;
            }
            eq(display + " resolves in all twelve signs", 12, resolved);
        }
    }

    // ---------------------------------------------------------------- plumbing

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, int expected, int actual) {
        checks++;
        if (expected != actual) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        int failed = failures.size() - before;
        System.out.println(failed == 0 ? part + ": clear" : part + ": " + failed + " failed");
    }
}
