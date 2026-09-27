package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AppPaths;

/**
 * Where a problem gets written down, decided in one place (J8).
 *
 * <p><b>What this replaces was worse than the list said.</b> Three places in {@code SkymapPanel}
 * carried the same six lines:
 *
 * <pre>
 * new java.io.PrintWriter(new java.io.FileWriter(
 *     "C:\\Users\\daver\\Desktop\\Ourania\\OuraniaWindows\\error_log.txt"))
 * </pre>
 *
 * The master list called that "a hard-coded absolute path" that "fails silently on anyone else's
 * machine". Measured on 27 Sep it was not failing at all - <b>that directory still exists on this
 * machine</b>, holding an older copy of the tree, while the repository being built is under
 * {@code Projects\Ourania}. So every logged problem was being written into a stale second copy of
 * the project: not lost, which would at least be obvious, but filed somewhere that looks exactly
 * like the right place.
 *
 * <p><b>And each write truncated the file.</b> {@code new FileWriter(path)} without the append flag
 * starts again from nothing, so the log held one entry - the last one - and the run-up to a crash
 * was overwritten by the crash. A log that keeps one line is not a log.
 *
 * <p><b>Where it goes is not this class's decision either.</b> {@link AppPaths#userDir} already
 * answers "where do this reader's files live", and already answers it differently for a packaged
 * app, where {@code Program Files} cannot be written to. Asking it is what makes the log land beside
 * the settings and the chart book instead of beside the source tree.
 */
final class ErrorLog {

    private ErrorLog() { }

    /** The log's name, under {@link AppPaths#userDir}. */
    static final String FILE = "error_log.txt";

    /** How many bytes the log is allowed to reach before it is rolled once. */
    static final long MAX_BYTES = 2L * 1024 * 1024;

    /**
     * A directory for the log instead of the reader's, or null for the reader's.
     *
     * <b>A seam the suite needs and nothing else uses.</b> From classes {@link AppPaths#userDir}
     * is the working directory, so a check that exercised this class for real would append to the
     * {@code error_log.txt} a reader actually has - and J14's whole point is that no suite touches
     * the reader's files. {@code userDir} is resolved once per run and cannot be redirected part way
     * through, so the seam is here rather than there. Package-private, and the check asserts that
     * with the seam unset the log lands where {@code AppPaths} says.
     */
    static java.io.File dirOverride;

    /** Where the log is, which is a question the suite asks and a reader may too. */
    static java.io.File file() {
        java.io.File to = dirOverride;
        return to == null ? new java.io.File(AppPaths.userDir() + FILE) : new java.io.File(to, FILE);
    }

    /**
     * Writes a problem down, and never throws.
     *
     * <b>A logger that can fail the thing it is logging is a defect, not a safeguard.</b> Every
     * call site here is already inside a {@code catch}, so an exception escaping this method would
     * replace a recoverable problem with an unrecoverable one - and the reason the old code was
     * wrapped in its own bare {@code catch (Exception ex) {}} was exactly this fear, expressed three
     * times and without a comment.
     */
    static void record(String what, Throwable t) {
        try {
            java.io.File f = file();
            roll(f);
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter(f, true))) {
                pw.println("---- " + java.time.ZonedDateTime.now() + "  " + what);
                if (t != null) {
                    t.printStackTrace(pw);
                }
            }
        } catch (Throwable ignored) {
            // Deliberately silent, and deliberately catching Throwable: see above. The stack trace
            // still reaches the console at every call site.
        }
    }

    /**
     * Keeps one previous log beside the current one, once it passes {@link #MAX_BYTES}.
     *
     * <b>Rolling rather than truncating is the point of this class</b>, so the run-up to a problem
     * survives the problem. One generation, because two would be a retention policy and nobody has
     * asked for one.
     */
    private static void roll(java.io.File f) {
        if (!f.isFile() || f.length() < MAX_BYTES) {
            return;
        }
        java.io.File old = new java.io.File(f.getPath() + ".1");
        if (old.isFile() && !old.delete()) {
            return;
        }
        if (!f.renameTo(old)) {
            // Could not roll: keep appending rather than lose what is there.
            return;
        }
    }

    /**
     * Sends anything that reaches the top of a thread here as well as to the console.
     *
     * <b>Called from {@code main}, so it covers the event thread too.</b> Before this, an exception
     * escaping a listener printed to a console that a packaged app does not have, and then vanished
     * - which is why several defects this month were found by rendering the screen rather than by
     * reading a log.
     */
    static void install() {
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            record("uncaught on thread " + thread.getName(), t);
            t.printStackTrace();
        });
    }
}
