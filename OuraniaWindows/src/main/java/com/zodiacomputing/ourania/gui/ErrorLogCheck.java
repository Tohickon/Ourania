package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AppPaths;

/**
 * Where a problem is written down, and that no absolute path is written into the app (J8).
 *
 * <p><b>The row this holds was measured worse than it was recorded.</b> The master list said the
 * logger's hard-coded path "fails silently on anyone else's machine". On 27 Sep it was not failing:
 * the directory it names still exists on this machine, holding an older copy of the tree, so
 * problems were being filed into a stale second copy of the project. Three call sites carried the
 * same six lines, and each one truncated the file, so the log held exactly one entry.
 *
 * <p>Parts A to D hold the new one place. <b>Part E is the part that keeps it fixed</b>: it sweeps
 * every production source for an absolute path in a string literal, and it is the inverse of
 * {@code NavigationCheck}'s door sweep - same scanner, opposite half.
 */
public final class ErrorLogCheck {

    private ErrorLogCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        // J14's rule, and its own suite caught this one missing: reg29 went red on
        // "every suite calls useScratchFile; missing: [ErrorLogCheck.java]". A suite that has not
        // asked for a scratch state is reading the reader's, which is exactly what J14 closed.
        Settings.useScratchFile();
        java.io.File sandbox = new java.io.File("out-errorlog-check");
        try {
            part("A: the log lands where the reader's files live", ErrorLogCheck::where);
            ErrorLog.dirOverride = fresh(sandbox);
            part("B: a second problem does not erase the first", ErrorLogCheck::appends);
            part("C: writing a problem down cannot make things worse", ErrorLogCheck::neverThrows);
            part("D: what reaches the top of a thread is written down", ErrorLogCheck::uncaught);
            part("E: no absolute path is written into the app", ErrorLogCheck::noAbsolutePaths);
        } finally {
            ErrorLog.dirOverride = null;
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
     * With the seam unset, the log is under {@link AppPaths#userDir} - the question this row is
     * about. The negative assertion names the old path, because that is the defect and a check that
     * only says "it is somewhere sensible" would have passed before the fix as well.
     */
    private static void where() {
        ErrorLog.dirOverride = null;
        java.io.File f = ErrorLog.file();
        String path = f.getPath().replace('\\', '/');
        ok("the log sits under AppPaths.userDir: " + path,
            path.equals((AppPaths.userDir() + ErrorLog.FILE).replace('\\', '/')));
        ok("and not in a copy of the source tree on one machine: " + path,
            !path.toLowerCase().contains("desktop/ourania"));
        ok("and it is named for what it is", f.getName().equals("error_log.txt"));
    }

    // ---------------------------------------------------------------- Part B

    /**
     * <b>The assertion the old code would have failed.</b> {@code new FileWriter(path)} without the
     * append flag starts the file again, so the run-up to a crash was erased by the crash. Two
     * records, and both have to still be there.
     */
    private static void appends() throws Exception {
        ErrorLog.record("first problem", new IllegalStateException("one"));
        ErrorLog.record("second problem", new IllegalStateException("two"));
        String text = read(ErrorLog.file());
        ok("the first problem is still there", text.contains("first problem"));
        ok("and so is the second", text.contains("second problem"));
        ok("with both stack traces", text.contains("one") && text.contains("two"));
        ok("each entry says when it happened",
            text.contains("---- " + java.time.ZonedDateTime.now().getYear()));
        int first = text.indexOf("first problem");
        int second = text.indexOf("second problem");
        ok("in the order they happened", first >= 0 && second > first);
    }

    // ---------------------------------------------------------------- Part C

    /**
     * <b>A logger that can fail the thing it is logging is a defect.</b> Every call site is inside a
     * catch already, so an exception escaping here would turn a recoverable problem into a crash.
     * The read-only case is the real one: on Windows an append to a read-only file throws.
     */
    private static void neverThrows() throws Exception {
        boolean threw = false;
        try {
            ErrorLog.record(null, null);
        } catch (Throwable t) {
            threw = true;
        }
        ok("a problem with no name and no exception is still written down without throwing", !threw);

        java.io.File f = ErrorLog.file();
        ok("the log is there to be made unwritable", f.isFile());
        boolean locked = f.setWritable(false);
        threw = false;
        try {
            ErrorLog.record("while the log cannot be written", new IllegalStateException("x"));
        } catch (Throwable t) {
            threw = true;
        } finally {
            f.setWritable(true);
        }
        // Reported rather than asserted when the lock did not take: a machine or filesystem that
        // ignores setWritable would otherwise fail a check about something else entirely.
        if (locked) {
            ok("a log that cannot be written throws nothing at the caller", !threw);
        } else {
            System.out.println("    (this filesystem ignored setWritable(false); case not exercised)");
        }
        ok("and the app carries on: the next problem still records", recordsOnce("after the lock"));
    }

    private static boolean recordsOnce(String what) throws Exception {
        ErrorLog.record(what, null);
        return read(ErrorLog.file()).contains(what);
    }

    // ---------------------------------------------------------------- Part D

    /**
     * <b>Thrown for real on a real thread, not handed to the handler by hand.</b> Calling the
     * handler directly would pass whether or not it was ever installed as the default, which is the
     * whole claim. Waited for with a deadline rather than slept through - {@code ec44b792}'s rule,
     * and the reason {@code ScrubCheck} went red in CI on 27 Sep.
     */
    private static void uncaught() throws Exception {
        ErrorLog.install();
        ok("a handler is installed", Thread.getDefaultUncaughtExceptionHandler() != null);
        final String marker = "deliberate-" + System.nanoTime();
        Thread t = new Thread(() -> {
            throw new IllegalStateException(marker);
        }, "check-thread");
        t.start();
        boolean arrived = waitFor(() -> read(ErrorLog.file()).contains(marker), 4000);
        ok("an exception nobody caught reaches the log within 4 s", arrived);
        String text = read(ErrorLog.file());
        ok("and says which thread it came from", text.contains("check-thread"));
        t.join(2000);
    }

    // ---------------------------------------------------------------- Part E

    /**
     * No string literal in a production source is an absolute path.
     *
     * <p><b>One known exemption, named and counted</b>, the way {@code known-red.txt} carries an
     * expected count rather than a name: {@code Ephemeris.DEFAULT} is still
     * {@code C:/Users/daver/Desktop/Ourania/decoded_apk/assets}, and it is where the ephemeris files
     * on this machine actually are. Removing it without moving those files would send every chart to
     * the Moshier fallback and change the answers of most of the astro suites, so it is a separate
     * piece of work - it needs the ephemeris inside the repository, or {@code OURANIA_EPHE} set. It is
     * exempt here so that <b>a new absolute path is a failure</b>, and the count is asserted so the
     * exemption cannot quietly grow.
     *
     * <p><b>Suites are not swept.</b> {@code ChartSetupCheck} deliberately names
     * {@code C:/no-such-ephemeris} to prove what happens when the ephemeris is missing. A check may
     * name a path that must not exist; a reader's app may not.
     */
    private static void noAbsolutePaths() throws Exception {
        // The sweep can see one when it is there, and does not see one in a comment - held here
        // because a sweep nobody checks is the defect this project has logged most.
        ok("the sweep finds an absolute path in a literal",
            isAbsolute(JavaSource.literals("String s = \"C:/Users/x\";").get(0)));
        ok("and one written with backslashes",
            isAbsolute(JavaSource.literals("String s = \"C:\\\\Users\\\\x\";").get(0)));
        ok("and a UNC path", isAbsolute(JavaSource.literals("String s = \"\\\\\\\\box\\\\x\";").get(0)));
        ok("and does not see one in a comment",
            JavaSource.literals("// C:/Users/daver/thing\nint i = 1;").isEmpty());
        ok("and does not see one in a javadoc block",
            JavaSource.literals("/* C:/Users/daver/thing */ int i = 1;").isEmpty());
        ok("a relative path is not absolute", !isAbsolute("src/main/resources/data/"));
        ok("nor is a URL", !isAbsolute("https://example.org/x"));
        // The exact literal that caught the first version of this sweep out. InterpretationService
        // carries "\\\"" - an escaped backslash then an escaped quote - whose VALUE is one backslash
        // and a quote. Read raw it looks like a UNC path; read as a value it is not one, and four
        // false positives in one file is what sent me to fix the scanner rather than the tree.
        String escapedQuote = JavaSource.literals("String s = \"\\\\\\\"\";").get(0);
        ok("an escaped backslash resolves to one backslash: " + escapedQuote.length() + " chars",
            escapedQuote.equals("\\\""));
        ok("and an escaped quote behind it is not a UNC path", !isAbsolute(escapedQuote));

        java.util.List<String> found = new java.util.ArrayList<>();
        int swept = 0;
        for (java.io.File f : JavaSource.allSources()) {
            String name = f.getName();
            if (name.endsWith("Check.java") || name.endsWith("SelfTest.java")
                || name.equals("FittingHarness.java")) {
                continue;
            }
            swept++;
            for (String lit : JavaSource.literals(read(f))) {
                if (isAbsolute(lit)) {
                    found.add(name + ": " + lit);
                }
            }
        }
        ok("the sweep reached the tree: " + swept + " production sources", swept > 100);

        java.util.List<String> unexpected = new java.util.ArrayList<>();
        int exempt = 0;
        for (String hit : found) {
            if (hit.startsWith("Ephemeris.java: ")) {
                exempt++;
            } else {
                unexpected.add(hit);
            }
        }
        ok("no absolute path outside the one known exemption: " + unexpected, unexpected.isEmpty());
        // Fewer is reported, not failed, so fixing Ephemeris does not turn this red - the same
        // posture the regression gate takes when a known-red suite improves.
        if (exempt == 1) {
            ok("the exemption is still exactly one: Ephemeris.DEFAULT", true);
        } else if (exempt == 0) {
            System.out.println("    (Ephemeris.DEFAULT is gone - delete the exemption in Part E)");
            ok("the exemption is still exactly one: Ephemeris.DEFAULT", true);
        } else {
            ok("the exemption is still exactly one, found " + exempt + ": " + found, false);
        }
        ok("and SkymapPanel carries none, which is what J8 was",
            unexpected.stream().noneMatch(h -> h.startsWith("SkymapPanel.java")));
    }

    /**
     * A Windows drive path or a UNC path, tested against the literal's <b>value</b>.
     *
     * <b>No unescaping here, deliberately.</b> The first version of this did its own
     * {@code replace("\\\\", "\\")} on the raw source text and reported four UNC paths in
     * {@code InterpretationService}, which carries {@code "\\\""} - escapes that begin with
     * backslashes and are not paths at all. Resolving escapes is the scanner's job, once, in
     * {@link JavaSource#literals}; doing it again here is how the two would come to disagree.
     *
     * <p>A single leading slash is not treated as absolute: in this tree those are resource paths,
     * not filesystem paths.
     */
    private static boolean isAbsolute(String s) {
        if (s == null || s.length() < 3) {
            return false;
        }
        if (s.startsWith("\\\\") || s.startsWith("//")) {
            return true;
        }
        return Character.isLetter(s.charAt(0)) && s.charAt(1) == ':'
            && (s.charAt(2) == '/' || s.charAt(2) == '\\');
    }

    // ---------------------------------------------------------------- plumbing

    private interface Body {
        void run() throws Exception;
    }

    private interface Condition {
        boolean met() throws Exception;
    }

    private static boolean waitFor(Condition c, long limitMs) throws Exception {
        long end = System.nanoTime() + limitMs * 1000000L;
        while (System.nanoTime() < end) {
            if (c.met()) {
                return true;
            }
            Thread.sleep(10);
        }
        return c.met();
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
        checks++;
        if (!pass) {
            failures.add(what);
        }
        System.out.println("  " + (pass ? "ok  " : "FAIL") + "  " + what);
    }

    private static String read(java.io.File f) throws Exception {
        if (!f.isFile()) {
            return "";
        }
        return new String(java.nio.file.Files.readAllBytes(f.toPath()),
            java.nio.charset.StandardCharsets.UTF_8);
    }

    private static java.io.File fresh(java.io.File dir) {
        delete(dir);
        dir.mkdirs();
        return dir;
    }

    private static void delete(java.io.File dir) {
        java.io.File[] kids = dir.listFiles();
        if (kids != null) {
            for (java.io.File k : kids) {
                k.setWritable(true);
                k.delete();
            }
        }
        dir.delete();
    }
}
