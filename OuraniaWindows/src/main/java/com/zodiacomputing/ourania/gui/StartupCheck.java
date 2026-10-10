package com.zodiacomputing.ourania.gui;

/**
 * What start-up checks, what it says, and what a problem report may carry (J8, J9).
 *
 * <p><b>The row said nothing verified the ephemeris directory, the data files or the settings
 * before the window opened. The first thing the new code measured was a defect four weeks old:</b>
 * {@code semo_18.se1} was not in this machine's ephemeris directory, every {@code swe_calc_ut}
 * in the app carried "file 'semo_18.se1' not found" in its error buffer and returned success
 * anyway, and the North Node was computed by approximation on every chart cast here. Part G was
 * written so that a directory which grew the file would make this suite say so rather than
 * quietly pass.
 *
 * <p><b>On 2026-10-10 the file was put in place, and Part G said so.</b> It failed on the one
 * assertion that pinned the absence, which is the whole of what it was for. Its assertions now
 * read the other way round - the directory is complete - so a directory that loses a file again
 * fails them. The corpus moved with it: {@code ephemeris-corpus.tsv} was re-recorded, the North
 * Node went from approximation to files on 50 rows, and nothing went the other way.
 *
 * <p><b>Part B holds a defect this suite's own subject had.</b> The first
 * {@link Startup#namedFiles} looked for "not found" <i>before</i> the quoted filename; Swiss
 * Ephemeris puts it after. It reported no missing files on a machine missing one - a validator
 * that says everything is fine is the worst possible failure mode for a validator, and it was
 * found by running it rather than by reading it.
 *
 * <p><b>Part H asserts an absence before it asserts anything else.</b> A problem report is the one
 * artefact in this app designed to be handed to somebody else, and the app holds birth data.
 */
public final class StartupCheck {

    private StartupCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        java.io.File sandbox = new java.io.File("out-startup-check");
        try {
            part("A: a data file that is not there is named", StartupCheck::missingData);
            part("B: the missing ephemeris files are read out of the library's own message",
                StartupCheck::namedFiles);
            part("C: a settings file that could not be read is reported", () -> settings(sandbox));
            part("D: a problem that was written down is mentioned", () -> log(sandbox));
            part("E: every data file a loader opens is in a list start-up can check",
                StartupCheck::oneList);
            part("F: the same finding is announced once, a new one again", StartupCheck::once);
            part("G: what the ephemeris can answer, measured rather than assumed",
                StartupCheck::ephemeris);
            part("H: a problem report carries the build and not the reader",
                StartupCheck::report);
        } finally {
            Startup.dataDirOverride = null;
            Startup.jdOverride = null;
            ErrorLog.dirOverride = null;
            System.clearProperty(Settings.FILE_PROPERTY);
            Settings.forget();
            delete(sandbox);
        }
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

    // ---------------------------------------------------------------- Part A

    /**
     * An empty data folder is reported as missing files, by name, and as DEGRADED.
     *
     * <b>Pointed at an empty directory rather than at one file removed.</b> Removing a file from
     * the real corpus to test this would be a suite editing the reader's data, and J10 exists
     * because a tool that rewrites the corpus once cost 192 entries.
     */
    private static void missingData() throws Exception {
        java.io.File empty = java.nio.file.Files.createTempDirectory("startup-empty").toFile();
        empty.deleteOnExit();
        Startup.dataDirOverride = empty.getPath().replace('\\', '/') + "/";
        try {
            Startup.Finding f = only(Startup.inspect(), "interpretation");
            ok("an empty data folder is a finding", f != null);
            if (f == null) {
                return;
            }
            ok("and it costs the reader something", f.level == Startup.Level.DEGRADED);
            // The two files whose absence is silent in different ways: the corpus core, and the
            // gazetteer, which is not part of the corpus and was checked by nothing until J9.
            ok("it names the core corpus file", f.detail.contains("interpretations.json"));
            ok("it names the gazetteer", f.detail.contains(Atlas.FILE_NAME));
            ok("it says where it looked", f.detail.contains(empty.getName()));

            // <b>And the real folder does not produce it.</b> Without this the part passes
            // against a check that always reports everything missing.
            Startup.dataDirOverride = null;
            ok("the real data folder is complete",
                only(Startup.inspect(), "interpretation") != null
                && only(Startup.inspect(), "interpretation").level == Startup.Level.NOTE);
        } finally {
            Startup.dataDirOverride = null;
        }
    }

    // ---------------------------------------------------------------- Part B

    /**
     * The reader of Swiss Ephemeris' error buffer, against the real messages.
     *
     * <b>Every string here was copied from a run, not invented.</b> The trailing path is quoted
     * too, and the first draft of this method took it as a filename until the suffix test was
     * added; the "sun:" prefixed form arrives when a body's computation needs another body first.
     */
    private static void namedFiles() {
        String real = "SwissEph file 'semo_18.se1' not found in the paths of: "
            + "'C:/Users/daver/Desktop/Ourania/decoded_apk/assets'\n";
        java.util.List<String> got = Startup.namedFiles(real);
        ok("the missing file is found (" + got + ")",
            got.size() == 1 && got.get(0).equals("semo_18.se1"));
        ok("and the quoted path it looked in is not taken for a file",
            !String.join(",", got).contains("assets"));

        java.util.List<String> nested = Startup.namedFiles(
            "sun: SwissEph file 'sepl_12.se1' not found in the paths of: 'D:/ephe'\n");
        ok("a message about another body's file is still read (" + nested + ")",
            nested.size() == 1 && nested.get(0).equals("sepl_12.se1"));

        ok("two missing files both come back", Startup.namedFiles(
            "SwissEph file 'sepl_12.se1' not found in x. SwissEph file 'semo_12.se1' not found in y")
            .size() == 2);

        // The messages that are NOT about a missing file, each of which the first draft would
        // have had to be told about separately.
        ok("an empty buffer names nothing", Startup.namedFiles("").isEmpty());
        ok("null names nothing", Startup.namedFiles(null).isEmpty());
        ok("a range complaint names nothing",
            Startup.namedFiles("jd 2615905.0 > Swiss Eph. upper limit 2488922.5;").isEmpty());
        ok("a file that was found names nothing",
            Startup.namedFiles("SwissEph file 'sepl_18.se1' was read from 'D:/ephe'").isEmpty());
    }

    // ---------------------------------------------------------------- Part C

    /**
     * A settings file that cannot be read, and the ones set aside earlier.
     *
     * <b>Forced by pointing the settings path at a directory</b>, which is the shortest real way
     * to make the read throw - a Properties file will parse almost any text, so corrupting the
     * contents does not reach this branch. The J11 machinery this reports on was written for
     * exactly the condition it could not make happen in a test.
     */
    private static void settings(java.io.File sandbox) throws Exception {
        java.io.File dir = fresh(sandbox);
        java.io.File notAFile = new java.io.File(dir, "settings.properties");
        ok("the stand-in is a directory", notAFile.mkdirs());
        String was = System.getProperty(Settings.FILE_PROPERTY);
        try {
            System.setProperty(Settings.FILE_PROPERTY, notAFile.getPath());
            Settings.forget();
            Settings.load();
            ok("the settings file could not be read", Settings.loadFailed());
            Startup.Finding f = only(Startup.inspect(), "settings file could not");
            ok("start-up says so", f != null && f.level == Startup.Level.DEGRADED);
            ok("and names the file", f != null && f.detail.contains("settings.properties"));

            // The second half, and the reason this is a reader-facing row: J11 kept the evidence
            // and announced it to a console a packaged app does not have.
            ok("an aside file is made", new java.io.File(dir,
                "settings.properties.corrupt").mkdirs());
            Startup.Finding aside = only(Startup.inspect(), "set aside");
            ok("start-up mentions it", aside != null);
            ok("as a note rather than a cost", aside == null
                || aside.level == Startup.Level.NOTE);
            ok("naming the file kept", aside != null && aside.detail.contains(".corrupt"));
        } finally {
            if (was == null) {
                System.clearProperty(Settings.FILE_PROPERTY);
            } else {
                System.setProperty(Settings.FILE_PROPERTY, was);
            }
            Settings.forget();
        }
        // Nothing set aside, nothing said - the assertion that stops this part passing against
        // a finding that is always produced.
        Settings.useScratchFile();
        ok("a clean install is not told about corrupt files",
            only(Startup.inspect(), "set aside") == null);
    }

    // ---------------------------------------------------------------- Part D

    /** A log with something in it is mentioned; an empty one is not (J8). */
    private static void log(java.io.File sandbox) throws Exception {
        java.io.File dir = fresh(sandbox);
        ErrorLog.dirOverride = dir;
        try {
            ok("nothing written down, nothing said",
                only(Startup.inspect(), "written down") == null);
            ErrorLog.record("something to find", new IllegalStateException("for the suite"));
            Startup.Finding f = only(Startup.inspect(), "written down");
            ok("once something is written down, start-up mentions it", f != null);
            ok("as a note, because the log is not itself a problem",
                f != null && f.level == Startup.Level.NOTE);
            ok("and says where it is", f != null && f.detail.contains(ErrorLog.FILE));
        } finally {
            ErrorLog.dirOverride = null;
        }
    }

    // ---------------------------------------------------------------- Part E

    /**
     * Every file the loader names - {@code DataFiles.entry("...")} since M1, {@code DATA_DIR + "..."}
     * before it - is in a list start-up can check against.
     *
     * <b>The part that keeps this row closed.</b> {@code CORE_FILES} is a hand-written list beside
     * eight loaders, which is the arrangement this project has watched drift four times - four
     * copies of the house systems, six of {@code ringWord}, three of the isMinor rule. A ninth
     * core loader added without touching the list would leave start-up reporting a complete corpus
     * while the file it needs is absent, which is the exact failure J9 exists to end.
     */
    private static void oneList() throws Exception {
        java.io.File src = new java.io.File(
            "src/main/java/com/zodiacomputing/ourania/gui/InterpretationService.java");
        ok("the loader's source is there", src.isFile());
        if (!src.isFile()) {
            return;
        }
        // <b>Comments out, literals kept.</b> Scanned raw, this part failed against a sentence
        // in its own javadoc that quotes the pattern it is looking for. A sweep that cannot tell
        // a comment about the rule from the rule teaches people to stop writing the comment.
        String text = JavaSource.withoutComments(new String(
            java.nio.file.Files.readAllBytes(src.toPath()),
            java.nio.charset.StandardCharsets.UTF_8));
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:DATA_DIR\\s*\\+\\s*|DataFiles\\.entry\\(\\s*)\"([^\"]+)\"").matcher(text);
        java.util.Set<String> known = new java.util.HashSet<>(java.util.Arrays.asList(
            InterpretationService.everyFileName()));
        int found = 0;
        while (m.find()) {
            found++;
            ok(m.group(1) + " is in everyFileName()", known.contains(m.group(1)));
        }
        // <b>A count, because a regex that matches nothing passes every assertion above it.</b>
        ok("the loader names files this way at all (" + found + ")", found >= 8);
        ok("and the list is longer than that, because most files come from arrays ("
            + known.size() + ")", known.size() > found);
        ok("allFileNames is the supplementary set and is smaller",
            InterpretationService.allFileNames().length < known.size());
    }

    // ---------------------------------------------------------------- Part F

    /**
     * The same problem is mentioned once; a different one is mentioned again.
     *
     * <b>A notice a reader sees every morning is one they learn to dismiss unread</b>, which would
     * cost exactly the case it exists for. The detail carries the date it was measured at, so the
     * signature deliberately does not.
     */
    private static void once() {
        java.util.List<Startup.Finding> one = java.util.Arrays.asList(
            new Startup.Finding(Startup.Level.DEGRADED, "The ephemeris is missing a file",
                "measured 2026-09-28"),
            new Startup.Finding(Startup.Level.NOTE, "Problems have been written down", "x"));
        java.util.List<Startup.Finding> sameTomorrow = java.util.Arrays.asList(
            new Startup.Finding(Startup.Level.DEGRADED, "The ephemeris is missing a file",
                "measured 2026-09-29"),
            new Startup.Finding(Startup.Level.NOTE, "Problems have been written down", "y"));
        java.util.List<Startup.Finding> alsoSomethingElse = java.util.Arrays.asList(
            new Startup.Finding(Startup.Level.DEGRADED, "The ephemeris is missing a file", "z"),
            new Startup.Finding(Startup.Level.DEGRADED, "The data folder is not there", "z"));
        java.util.List<Startup.Finding> onlyNotes = java.util.Arrays.asList(
            new Startup.Finding(Startup.Level.NOTE, "Everything answered", "x"));

        ok("the same problem tomorrow has the same signature",
            Startup.signature(one).equals(Startup.signature(sameTomorrow)));
        ok("a second problem changes it",
            !Startup.signature(one).equals(Startup.signature(alsoSomethingElse)));
        ok("notes alone have no signature", Startup.signature(onlyNotes).isEmpty());
        ok("and are never announced", !Startup.unannounced(onlyNotes));

        ok("a problem never seen is announced", Startup.unannounced(one));
        Startup.announced(one);
        ok("and not a second time", !Startup.unannounced(one));
        ok("nor tomorrow", !Startup.unannounced(sameTomorrow));
        ok("but a new one is", Startup.unannounced(alsoSomethingElse));
        // The key belongs to the machine, not to a configuration that can be shared.
        ok("what has been seen is the reader's, not their configuration",
            Settings.isPersonal(Startup.SEEN_KEY));
        Settings.set(Startup.SEEN_KEY, "");
    }

    // ---------------------------------------------------------------- Part G

    /**
     * What the ephemeris can and cannot answer, at two instants.
     *
     * <b>Asserted as a shape rather than as a list of filenames.</b> Which file covers which years
     * is Swiss Ephemeris' rule and restating it here would be a second copy of it. What this holds
     * is that the inspection <i>notices</i>: inside the files' era there is at most a note about
     * approximation, and three centuries before them the finding exists and says which bodies.
     */
    private static void ephemeris() {
        Startup.dataDirOverride = null;
        try {
            Startup.jdOverride = new de.thmac.swisseph.SweDate(1500, 6, 1, 12.0).getJulDay();
            java.util.List<Startup.Finding> old = Startup.inspect();
            Startup.Finding fell = only(old, "approximation");
            Startup.Finding none = only(old, "cannot be computed");
            ok("three centuries before the files, bodies fall back", fell != null);
            ok("and it says which", fell != null && fell.detail.contains("Pluto"));
            ok("and at what moment it measured", fell != null && fell.detail.contains("1500"));
            ok("the asteroids cannot be computed at all there", none != null);
            ok("named, so a reader knows what is absent from the chart",
                none != null && none.detail.contains("Ceres"));

            Startup.jdOverride = new de.thmac.swisseph.SweDate(1975, 7, 4, 12.0).getJulDay();
            java.util.List<Startup.Finding> now = Startup.inspect();
            ok("inside the files' era nothing is beyond computing",
                only(now, "cannot be computed") == null);

            // <b>Written to change when the directory does, and on 2026-10-10 it did.</b> The
            // assertions here used to pin semo_18.se1's absence and what that absence cost; the
            // absence ended, and they are what reported it rather than passing through it. They
            // read the other way round now, so a directory that loses a file fails them again.
            //
            // <b>The pair they replace shows how a guard stops guarding.</b> The second one was
            // "the North Node is what it costs", written as {@code !lacksMoon || ...} - so the
            // moment the file arrived it passed vacuously, on a machine it no longer described.
            // The three below are independent claims and not one leaning on another: no file is
            // absent, start-up says the directory is whole, and nothing falls back here. A
            // missing file is not the only way a body can fall back - being outside the files'
            // era is another, which is what the 1500 half of this part asserts - so the third is
            // not implied by the first.
            Startup.Finding missing = only(now, "missing");
            Startup.Finding fellNow = only(now, "approximation");
            Startup.Finding whole = only(now, "answered from the ephemeris files");
            ok("no file is missing from this machine's ephemeris directory ("
                + (missing == null ? "none" : missing.detail) + ")", missing == null);
            ok("and start-up says the directory is whole in as many words", whole != null);
            ok("and nothing on a modern chart falls back to approximation ("
                + (fellNow == null ? "nothing" : fellNow.detail) + ")", fellNow == null);
        } finally {
            Startup.jdOverride = null;
        }
    }

    // ---------------------------------------------------------------- Part H

    /**
     * What a problem report may and may not carry.
     *
     * <b>The absence first.</b> A report is written to be handed to somebody else and this app
     * holds birth data; a check that only asserted the version was present would pass on a report
     * carrying a natal chart.
     */
    private static void report() {
        Settings.set("natal.date", "1982-08-10");
        Settings.set("natal.location", "a place the reader typed");
        try {
            String text = CrashReport.compose("pressed Synthesize",
                new IllegalStateException("for the suite"));

            ok("no birth date", !text.contains("1982-08-10"));
            ok("no birth place", !text.contains("a place the reader typed"));
            ok("no settings values at all", !text.contains("natal."));

            ok("it says which build", text.contains(Version.display()));
            ok("which Java", text.contains(System.getProperty("java.version")));
            ok("where the ephemeris is",
                text.contains(com.zodiacomputing.ourania.astro.Ephemeris.PATH));
            ok("where the data is", text.contains(InterpretationService.DATA_DIR));
            ok("what the reader was doing", text.contains("pressed Synthesize"));
            ok("what went wrong", text.contains("IllegalStateException"));
            ok("and where it threw", text.contains("StartupCheck"));
            ok("what start-up found", text.contains("Start-up inspection"));
            ok("and the log", text.contains("Log, last"));

            String asked = CrashReport.compose(null, null);
            ok("a report a reader asked for has no stack trace section",
                !asked.contains("What went wrong"));
            ok("and still says which build", asked.contains(Version.display()));

            java.io.File a = CrashReport.file(java.time.LocalDateTime.of(2026, 9, 28, 14, 3, 9));
            ok("a report is named for when it was written (" + a.getName() + ")",
                a.getName().equals("ourania-report-20260928-140309.txt"));
            ok("and sits beside the log",
                a.getParentFile().equals(ErrorLog.file().getAbsoluteFile().getParentFile()));
        } finally {
            Settings.useScratchFile();
        }
    }

    // ---------------------------------------------------------------- plumbing

    /** The one finding whose headline contains this, or null - and a failure if there are two. */
    private static Startup.Finding only(java.util.List<Startup.Finding> all, String words) {
        Startup.Finding found = null;
        for (Startup.Finding f : all) {
            if (f.what.toLowerCase().contains(words.toLowerCase())) {
                if (found != null) {
                    ok("exactly one finding says \"" + words + "\"", false);
                    return found;
                }
                found = f;
            }
        }
        return found;
    }

    private static java.io.File fresh(java.io.File sandbox) {
        delete(sandbox);
        java.io.File dir = new java.io.File(sandbox, "run" + System.nanoTime());
        return dir.mkdirs() ? dir : sandbox;
    }

    private static void delete(java.io.File f) {
        java.io.File[] kids = f.listFiles();
        if (kids != null) {
            for (java.io.File k : kids) {
                delete(k);
            }
        }
        f.delete();
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            // A crashed part reports no failures at all, which reads as a pass; see the four
            // suites that did exactly that this month.
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
