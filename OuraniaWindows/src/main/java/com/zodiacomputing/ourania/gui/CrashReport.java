package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AppPaths;
import com.zodiacomputing.ourania.astro.Ephemeris;

/**
 * One file a reader can send when something goes wrong (J8).
 *
 * <p><b>The half of J8 that kept it open.</b> 27 September gave the app one honest place to write
 * a problem down and left two things undone: nothing ever told the reader a problem had been
 * written, and there was nothing to <i>send</i>. A log alone is not a crash report - it says what
 * threw and says nothing about the build it threw in, the ephemeris it was reading, or the screen
 * it happened on, which is most of what a second person needs to reproduce it.
 *
 * <p><b>It is written, not uploaded.</b> A file on the reader's disk that they choose to attach is
 * the whole design: nothing leaves this machine unless a person sends it. That also settles what
 * may go in it - see {@link #compose}.
 *
 * <p><b>What is deliberately not in it.</b> No birth data, no saved charts, no place names, no
 * settings values. A crash report gets the version, the machine's Java and OS, the ephemeris and
 * data paths, the start-up findings and the tail of the log. A reader's chart is the most personal
 * thing this app holds and a report is the one artefact designed to be handed to somebody else;
 * {@code CrashReportCheck} Part C asserts the absence rather than trusting this paragraph.
 */
final class CrashReport {

    private CrashReport() { }

    /** How much of the log goes in. Enough for the run-up, not so much that nobody reads it. */
    static final int LOG_TAIL_BYTES = 64 * 1024;

    /** Where reports are written, under the reader's own folder beside the log. */
    static java.io.File file(java.time.LocalDateTime at) {
        String stamp = String.format("%04d%02d%02d-%02d%02d%02d", at.getYear(),
            at.getMonthValue(), at.getDayOfMonth(), at.getHour(), at.getMinute(), at.getSecond());
        java.io.File dir = ErrorLog.file().getAbsoluteFile().getParentFile();
        return new java.io.File(dir, "ourania-report-" + stamp + ".txt");
    }

    /**
     * The report's text.
     *
     * <b>Separate from writing it so the suite can read it without a disk.</b> The assertions that
     * matter here are about what the text does and does not contain, and a check that had to write
     * a file to ask would be answering a question about the file system instead.
     *
     * @param what  what the reader was doing, or null - free text, and it is theirs
     * @param cause the throwable that prompted this, or null when a reader asked for a report
     */
    static String compose(String what, Throwable cause) {
        StringBuilder sb = new StringBuilder();
        line(sb, "Ourania+ problem report");
        line(sb, "written", java.time.ZonedDateTime.now().toString());
        line(sb, "version", Version.display());
        sb.append('\n');

        line(sb, "java", System.getProperty("java.version") + "  "
            + System.getProperty("java.vendor"));
        line(sb, "os", System.getProperty("os.name") + " " + System.getProperty("os.version")
            + " " + System.getProperty("os.arch"));
        // <b>Paths, not contents.</b> Where the app was reading from is the single most useful
        // thing in a report about an app whose defects this month were mostly about where files
        // are; what is in those files is the reader's.
        line(sb, "packaged", String.valueOf(AppPaths.packaged()));
        line(sb, "ephemeris", Ephemeris.PATH + (Ephemeris.present() ? "" : "   (NOT PRESENT)"));
        line(sb, "data", InterpretationService.DATA_DIR);
        line(sb, "log", ErrorLog.file().getPath());
        sb.append('\n');

        if (what != null && !what.trim().isEmpty()) {
            line(sb, "What was happening");
            sb.append(what.trim()).append("\n\n");
        }

        if (cause != null) {
            line(sb, "What went wrong");
            java.io.StringWriter w = new java.io.StringWriter();
            cause.printStackTrace(new java.io.PrintWriter(w));
            sb.append(w).append('\n');
        }

        line(sb, "Start-up inspection");
        java.util.List<Startup.Finding> findings;
        try {
            findings = Startup.inspect();
        } catch (Throwable t) {
            findings = java.util.Collections.emptyList();
            sb.append("  (the inspection itself failed: ").append(t).append(")\n");
        }
        for (Startup.Finding f : findings) {
            sb.append("  ").append(f).append('\n');
        }
        sb.append('\n');

        line(sb, "Log, last " + (LOG_TAIL_BYTES / 1024) + " KB");
        sb.append(logTail());
        return sb.toString();
    }

    /**
     * The end of the log, or a line saying there is none.
     *
     * <b>The end, because a log that has rolled is a log whose beginning is old.</b> A problem
     * report is about what just happened, and the first 64 KB of a 2 MB file is the least likely
     * part of it to be relevant.
     */
    private static String logTail() {
        java.io.File f = ErrorLog.file();
        if (!f.isFile() || f.length() == 0) {
            return "  (nothing has been written down)\n";
        }
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            long from = Math.max(0, raf.length() - LOG_TAIL_BYTES);
            raf.seek(from);
            byte[] buf = new byte[(int) (raf.length() - from)];
            raf.readFully(buf);
            String text = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
            return (from > 0 ? "  (earlier entries omitted)\n" : "") + text;
        } catch (Throwable t) {
            return "  (the log could not be read: " + t + ")\n";
        }
    }

    private static void line(StringBuilder sb, String heading) {
        sb.append("== ").append(heading).append('\n');
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(String.format("%-10s %s%n", key + ":", value));
    }

    /**
     * Writes a report and answers where it went, or null if it could not be written.
     *
     * <b>Never throws, for {@link ErrorLog}'s reason.</b> Every caller is either already handling
     * a failure or is a button, and a report that fails by throwing turns a recoverable problem
     * into a second one.
     */
    static java.io.File write(String what, Throwable cause) {
        try {
            java.io.File f = file(java.time.LocalDateTime.now());
            java.nio.file.Files.write(f.toPath(),
                compose(what, cause).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return f;
        } catch (Throwable t) {
            ErrorLog.record("could not write a problem report", t);
            return null;
        }
    }
}
