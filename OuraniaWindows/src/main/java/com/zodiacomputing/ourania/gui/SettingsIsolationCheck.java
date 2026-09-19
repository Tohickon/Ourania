package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Master list J14: no check suite answers for what the reader last saved.
 *
 * <p>On 2026-09-18 ten suites were red for no reason but a preference. David had selected only
 * the ten planets and the Ascendant and set the transit orb to 0.25, and every one of the ten
 * failed identically on the commit before - so the regression that verifies every other item on
 * the list could not say whether the code was right. Two causes, both measured:
 *
 * <ol>
 *   <li><b>{@code useScratchFile} copied the reader's settings.</b> It was written to stop suites
 *       writing the reader's file, and it did; but a copy carries the reader's choices in.</li>
 *   <li><b>24 suites never called it.</b> Every astro suite among them - and the engine reads the
 *       body selection, the transit orb and the node variant underneath code that never names
 *       Settings, so those suites read the reader's <i>live</i> file.</li>
 * </ol>
 *
 * <p>And the chart book had no redirect at all: two suites wrote the reader's real
 * {@code saved_charts.properties}, one of them first moving the legacy book into a temp file,
 * protected only by a {@code finally} that a killed run never reaches.
 *
 * <p>Part A holds every suite to calling {@code useScratchFile} before anything else, and the
 * application to reaching the reader's files only through Settings and SavedCharts. Parts B and
 * C run the real mechanism in a separate JVM whose working directory holds a reader's settings,
 * chart book and legacy book full of recognisable values, and check what it can see and touch.
 */
public final class SettingsIsolationCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** Values no fresh install could hold, so seeing any of them means the reader's file leaked. */
    private static final String SENTINEL = "the-readers-own-value";
    /**
     * Inside the allowed range and not the default. 7.5 was the first choice, and transitOrb()
     * falls back to the default for anything past its 5-degree ceiling - so a leaked 7.5 would
     * have read as 1.0 on both sides and Part B would have passed for the wrong reason.
     */
    private static final String ORB = "3.5";

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        // The two halves that run in a child JVM, in a directory set up as a reader's.
        if (args.length > 0 && ("probe".equals(args[0]) || "app".equals(args[0]))) {
            childReport("probe".equals(args[0]));
            System.exit(0);
        }

        part("A: every suite isolates itself before anything else", SettingsIsolationCheck::sources);
        File reader = readerDirectory();
        part("B: a suite sees a fresh install, and cannot reach the reader's files",
            () -> suiteSide(reader));
        part("C: the application still gets the reader's real files", () -> appSide(reader));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    // ------------------------------------------------------------------ A

    /**
     * Read from the sources, because the failure being guarded against is a suite someone writes
     * next month without the call. Every class with a {@code main} that is a check - the names
     * ending in Check, and ZodiacSelfTest - must make it the first statement.
     *
     * <b>First, not merely present.</b> Anything that runs before it runs against the reader's
     * settings. ReadingPdfCheck was the one exception when this was written, answering a
     * command-line probe first; it was moved rather than excused.
     */
    private static void sources() throws Exception {
        File base = new File("src/main/java/com/zodiacomputing/ourania");
        Pattern main = Pattern.compile("public static void main\\s*\\([^)]*\\)[^{]*\\{");
        int suites = 0;
        List<String> missing = new ArrayList<>();
        List<String> late = new ArrayList<>();
        for (String pkg : new String[] {"astro", "gui"}) {
            File[] files = new File(base, pkg).listFiles();
            if (files == null) {
                continue;
            }
            Arrays.sort(files);
            for (File f : files) {
                String name = f.getName();
                if (!name.endsWith("Check.java") && !name.equals("ZodiacSelfTest.java")) {
                    continue;
                }
                String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Matcher m = main.matcher(src);
                if (!m.find()) {
                    continue;
                }
                suites++;
                String body = src.substring(m.end())
                    .replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("//[^\\n]*", "")
                    .trim();
                String first = body.split(";", 2)[0].trim();
                if (!src.contains("useScratchFile()")) {
                    missing.add(name);
                } else if (!first.endsWith("useScratchFile()")) {
                    late.add(name);
                }
            }
        }
        System.out.println("  " + suites + " suites read");
        ok("the suites were found at all", suites >= 50);
        ok("every suite calls useScratchFile; missing: " + missing, missing.isEmpty());
        ok("and calls it before anything else; late: " + late, late.isEmpty());

        // <b>And the redirect only helps code that goes through it.</b> ChartSetupPanel opened
        // settings.properties by name to find the home location and the last natal chart, so
        // NavigationCheck - isolated, first statement and all - still drew the reader's saved
        // chart on a cold open, and its Part K failed 8 under one reader's file and 0 under
        // another's. Only Settings and SavedCharts may name the reader's files in code.
        List<String> byName = new ArrayList<>();
        Pattern literal = Pattern.compile(
            "\"(settings\\.properties|saved_charts\\.properties|saved_transits\\.properties)\"");
        for (String pkg : new String[] {"astro", "gui"}) {
            File[] files = new File(base, pkg).listFiles();
            if (files == null) {
                continue;
            }
            Arrays.sort(files);
            for (File f : files) {
                String name = f.getName();
                if (!name.endsWith(".java") || name.endsWith("Check.java")
                        || name.equals("ZodiacSelfTest.java")
                        || name.equals("Settings.java") || name.equals("SavedCharts.java")) {
                    continue;
                }
                String code = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("//[^\\n]*", "");
                if (literal.matcher(code).find()) {
                    byName.add(pkg + "/" + name);
                }
            }
        }
        ok("no application code opens the reader's files by name; found in: " + byName,
            byName.isEmpty());
    }

    // ------------------------------------------------------------------ B and C

    /** A working directory holding a reader's settings, chart book and legacy book. */
    private static File readerDirectory() throws Exception {
        File dir = Files.createTempDirectory("ourania-reader").toFile();
        dir.deleteOnExit();

        Properties settings = new Properties();
        settings.setProperty("sentinel.key", SENTINEL);
        settings.setProperty(Settings.TRANSIT_ORB_KEY, ORB);
        settings.setProperty(Settings.BODIES_KEY, "sun");
        write(new File(dir, "settings.properties"), settings);

        Properties book = new Properties();
        book.setProperty("ReaderChart.date", "1982-08-10");
        book.setProperty("ReaderChart.time", "15:01");
        book.setProperty("ReaderChart.location", "Philadelphia, PA");
        write(new File(dir, "saved_charts.properties"), book);

        Properties legacy = new Properties();
        legacy.setProperty("LegacyChart.date", "1972-09-22");
        legacy.setProperty("LegacyChart.time", "06:00");
        legacy.setProperty("LegacyChart.location", "Los Angeles, CA");
        write(new File(dir, "saved_transits.properties"), legacy);
        return dir;
    }

    private static void write(File f, Properties p) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "a reader's file");
        }
        f.deleteOnExit();
    }

    private static void suiteSide(File reader) throws Exception {
        byte[][] before = snapshot(reader);
        List<String> out = runChild(reader, "probe");
        System.out.println("  suite side saw: " + out);

        eq("a suite does not see a key the reader saved", "absent", value(out, "sentinel"));
        eq("a suite's transit orb is a fresh install's, not the reader's",
            String.valueOf(com.zodiacomputing.ourania.astro.Transits.DEFAULT_ORB), value(out, "orb"));
        eq("a suite's body selection is the shipped default, not the reader's",
            Bodies.format(Bodies.defaults()), value(out, "bodies"));
        eq("a suite sees an empty chart book, not the reader's charts", "[]", value(out, "charts"));
        eq("the child's own writes went somewhere", "yes", value(out, "wrote"));

        byte[][] after = snapshot(reader);
        String[] names = {"settings.properties", "saved_charts.properties",
                          "saved_transits.properties"};
        for (int i = 0; i < names.length; i++) {
            ok("the reader's " + names[i] + " is byte for byte what it was",
                Arrays.equals(before[i], after[i]));
        }
    }

    /**
     * The same directory, without the call - which is what the application does.
     *
     * This half is the proof the probe can tell the two apart: if the redirect had broken the
     * application's path, or the sentinel could not be read at all, Part B would pass for the
     * wrong reason.
     */
    private static void appSide(File reader) throws Exception {
        List<String> out = runChild(reader, "app");
        System.out.println("  application side saw: " + out);
        eq("the application reads the reader's settings", SENTINEL, value(out, "sentinel"));
        eq("and the reader's transit orb", ORB, value(out, "orb"));
        ok("and the reader's charts, legacy book included",
            value(out, "charts").contains("ReaderChart")
                && value(out, "charts").contains("LegacyChart"));
    }

    private static byte[][] snapshot(File dir) throws Exception {
        String[] names = {"settings.properties", "saved_charts.properties",
                          "saved_transits.properties"};
        byte[][] out = new byte[names.length][];
        for (int i = 0; i < names.length; i++) {
            File f = new File(dir, names[i]);
            out[i] = f.exists() ? Files.readAllBytes(f.toPath()) : new byte[0];
        }
        return out;
    }

    /** Runs this class in a child JVM whose working directory is the reader's. */
    private static List<String> runChild(File dir, String mode) throws Exception {
        String java = ProcessHandle.current().info().command().orElse("java");
        StringBuilder cp = new StringBuilder();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (cp.length() > 0) {
                cp.append(File.pathSeparator);
            }
            // Relative entries were relative to this JVM's directory, not the child's.
            cp.append(entry.endsWith("*")
                ? new File(entry.substring(0, entry.length() - 1)).getAbsolutePath()
                    + File.separator + "*"
                : new File(entry).getAbsolutePath());
        }
        Process p = new ProcessBuilder(java, "-cp", cp.toString(),
            SettingsIsolationCheck.class.getName(), mode)
            .directory(dir).redirectErrorStream(true).start();
        String text = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        List<String> lines = new ArrayList<>();
        for (String l : text.split("\\R")) {
            if (l.startsWith("ISOLATION ")) {
                lines.add(l.substring("ISOLATION ".length()));
            }
        }
        if (lines.isEmpty()) {
            System.out.println("  child printed nothing usable:\n" + text);
        }
        return lines;
    }

    private static String value(List<String> lines, String key) {
        for (String l : lines) {
            if (l.startsWith(key + "=")) {
                return l.substring(key.length() + 1);
            }
        }
        return "(missing)";
    }

    /**
     * The child's half. {@code scratch} is whether it isolates first, as a suite does; without it
     * the child is the application, reading and writing where the application would.
     *
     * The child has already been isolated by main's first line, so the application half undoes
     * that - clearing the two properties is exactly the state a process is in when nothing ever
     * set them.
     */
    private static void childReport(boolean scratch) {
        if (!scratch) {
            System.clearProperty(Settings.FILE_PROPERTY);
            System.clearProperty(SavedCharts.FILE_PROPERTY);
            Settings.forget();
        }
        System.out.println("ISOLATION sentinel=" + Settings.get("sentinel.key", "absent"));
        System.out.println("ISOLATION orb=" + Settings.transitOrb());
        System.out.println("ISOLATION bodies=" + Bodies.format(Settings.loadBodySelection()));
        List<String> names = new ArrayList<>(SavedCharts.names());
        java.util.Collections.sort(names);
        System.out.println("ISOLATION charts=" + names);
        if (scratch) {
            // A suite writing: this must land in its scratch files, never the reader's.
            Settings.set("probe.wrote", "yes");
            SavedCharts.put("ProbeChart", "2000-01-01", "12:00", "Nowhere");
            System.out.println("ISOLATION wrote="
                + (SavedCharts.get("ProbeChart") != null
                    && "yes".equals(Settings.get("probe.wrote", "")) ? "yes" : "no"));
        }
    }

    // ------------------------------------------------------------------ harness

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expect, Object got) {
        ok(label + ": expected " + expect + ", got " + got,
            expect == null ? got == null : expect.equals(got));
    }

    private SettingsIsolationCheck() { }
}
