package com.zodiacomputing.ourania.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A report for every chart you choose, in one go (B8).
 *
 * <p><b>The master list called this "a loop and a progress dialog". It is not.</b> A report needs
 * a <i>drawn</i> wheel and a <i>generated</i> reading, and neither is a function that can be
 * called: casting a saved chart goes through the setup form, which may have to geocode a place
 * name, and the reading is produced by a {@code SwingWorker} inside {@link SkymapPanel} that
 * hands its result to the window when it finishes. So a batch drives the app - it loads each
 * chart the way a reader would, waits for the two asynchronous stages, exports, and puts the
 * reader's own chart back at the end.
 *
 * <p><b>Every wait has a deadline and none is a fixed sleep.</b> That is this project's rule since
 * {@code ec44b792}, when two suites flaked under regression load on slept guesses. A chart that
 * never finishes casting - an unreachable geocoder, a place that cannot be found - is recorded as
 * that chart's problem and the batch moves on, because one bad row in a list of forty must not
 * take the other thirty-nine with it.
 *
 * <p>The naming below is the part that can be held without any of that, and it carries more traps
 * than it looks: chart names are typed by a reader and file names are not.
 */
final class BatchReports {

    /** How long one chart may take to cast and read before the batch gives up on it. */
    static final long PER_CHART_MS = 120_000L;

    private BatchReports() { }

    // ------------------------------------------------------------------ naming

    /**
     * Characters Windows refuses in a file name, plus the ones that are merely a bad idea.
     *
     * <b>A chart is named by a reader and a file is not.</b> "Ada Lovelace 10/12/1815" is a
     * perfectly good chart name and three directories on disk.
     */
    private static final String ILLEGAL = "\\/:*?\"<>|";

    /**
     * Names Windows will not give a file whatever the extension.
     *
     * <b>Measured, not recalled:</b> {@code CON.pdf} and {@code NUL.pdf} cannot be created on
     * Windows at all, and a client called Prn or Aux is unlikely but a chart called "Aux" is not.
     * The open fails with a message about the file being in use by another process, which is the
     * least helpful thing the batch could report.
     */
    private static final Set<String> RESERVED = new LinkedHashSet<>(java.util.Arrays.asList(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"));

    /**
     * A file name for this chart that is safe, recognisable, and not already taken.
     *
     * <b>Recognisable matters as much as safe.</b> Numbering the files 1..n would be safe and
     * would leave a practitioner with forty PDFs they have to open to tell apart, which is most
     * of the value of the feature gone.
     *
     * @param taken names already used in this run, in lower case; this method adds to it
     */
    static String fileNameFor(String chartName, Set<String> taken) {
        StringBuilder sb = new StringBuilder();
        String from = chartName == null ? "" : chartName.trim();
        for (int i = 0; i < from.length(); i++) {
            char c = from.charAt(i);
            // Control characters would be accepted by some filesystems and are unreadable in all.
            if (c < ' ' || ILLEGAL.indexOf(c) >= 0) {
                sb.append('-');
            } else {
                sb.append(c);
            }
        }
        // A trailing dot or space is silently dropped by Windows, so "Ada." and "Ada" would
        // become the same file - a collision the loop below could not see coming.
        String base = sb.toString().trim();
        while (base.endsWith(".")) {
            base = base.substring(0, base.length() - 1).trim();
        }
        if (base.isEmpty()) {
            base = "chart";
        }
        if (RESERVED.contains(base.toUpperCase(Locale.ROOT))) {
            base = base + "-chart";
        }
        // Long names are the reader's business, but a path has a limit and the folder they chose
        // is already part of it.
        if (base.length() > 80) {
            base = base.substring(0, 80).trim();
        }

        String candidate = base;
        int n = 2;
        while (taken.contains(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + " (" + n + ")";
            n++;
        }
        taken.add(candidate.toLowerCase(Locale.ROOT));
        return candidate + ".pdf";
    }

    // ------------------------------------------------------------------ what happened

    /** What became of one chart. */
    static final class Outcome {
        final String chart;
        /** The file written, or null. */
        final File file;
        /** Why it was not written, or null. */
        final String problem;

        Outcome(String chart, File file, String problem) {
            this.chart = chart;
            this.file = file;
            this.problem = problem;
        }

        boolean worked() {
            return this.problem == null;
        }

        @Override
        public String toString() {
            return this.chart + (worked() ? " → " + this.file.getName()
                : " — " + this.problem);
        }
    }

    /**
     * What became of the whole run.
     *
     * <b>A run that was stopped still reports what it wrote.</b> Cancelling a batch after
     * thirty of forty reports is not a failure of thirty reports, and a dialog that said
     * "cancelled" and nothing else would send a practitioner looking in an empty folder that is
     * not empty.
     */
    static final class Run {
        final List<Outcome> outcomes = new ArrayList<>();
        boolean cancelled;

        int written() {
            int n = 0;
            for (Outcome o : this.outcomes) {
                if (o.worked()) {
                    n++;
                }
            }
            return n;
        }

        int failed() {
            return this.outcomes.size() - written();
        }

        /** One line a reader can act on. */
        String summary() {
            int ok = written();
            int bad = failed();
            StringBuilder sb = new StringBuilder();
            sb.append(ok).append(ok == 1 ? " report written" : " reports written");
            if (bad > 0) {
                sb.append(", ").append(bad).append(bad == 1 ? " could not be" : " could not be");
            }
            if (this.cancelled) {
                sb.append(" — stopped before the rest");
            }
            return sb.append('.').toString();
        }
    }

    // ------------------------------------------------------------------ driving it

    /** What the batch reports back to, and asks whether to stop. */
    interface Progress {
        /** True once the reader has asked it to stop; checked between charts, never mid-chart. */
        boolean cancelled();

        /** About to start this one. */
        void starting(int done, int total, String chart);

        /** This one is finished, well or badly. */
        void finished(Outcome outcome);
    }

    /**
     * Every chart in the list, into that folder.
     *
     * <b>Runs off the event thread and touches the screen only through invokeAndWait.</b> A batch
     * on the EDT would freeze the progress dialog it is trying to draw, and forty frozen seconds
     * is indistinguishable from a hang.
     *
     * <b>One bad chart does not take the rest.</b> A place the geocoder cannot find, a chart
     * deleted from the book since the list was drawn, a folder that turns read-only half way -
     * each is recorded against its own row and the batch goes on. A practitioner exporting forty
     * client reports wants thirty-nine and a list of what went wrong, not nothing and a dialog.
     */
    static Run run(OuraniaWindow window, List<String> names, File dir, Progress progress) {
        Run out = new Run();
        if (window == null || names == null || dir == null) {
            return out;
        }
        Set<String> taken = new LinkedHashSet<>();
        final String[] before = onScreen(window, () -> window.chartSetup().formSnapshot());

        try {
            for (int i = 0; i < names.size(); i++) {
                if (progress != null && progress.cancelled()) {
                    out.cancelled = true;
                    break;
                }
                String name = names.get(i);
                if (progress != null) {
                    progress.starting(i, names.size(), name);
                }
                Outcome outcome = one(window, name, dir, taken);
                out.outcomes.add(outcome);
                if (progress != null) {
                    progress.finished(outcome);
                }
            }
        } finally {
            // <b>Always, including after a cancel or a throw.</b> Leaving a reader on somebody
            // else's chart because a batch failed is the worst outcome available here: the wheel
            // would look perfectly normal and be the wrong person.
            onScreen(window, () -> {
                window.chartSetup().formRestore(before);
                window.chartSetup().generateChart();
                return null;
            });
        }
        return out;
    }

    private static Outcome one(OuraniaWindow window, String name, File dir, Set<String> taken) {
        SavedCharts.Entry entry = SavedCharts.get(name);
        if (entry == null) {
            return new Outcome(name, null, "no longer in the chart book");
        }
        long deadline = System.currentTimeMillis() + PER_CHART_MS;

        onScreen(window, () -> {
            // Cleared first, so "the reading is ready" cannot be answered by the previous
            // chart's reading still sitting in the pane - which would export forty copies of
            // the first one and look completely convincing.
            window.interpretation().getEditorPane().setText("");
            window.chartSetup().applySavedProfile(name, false);
            window.chartSetup().generateChart();
            return null;
        });

        String problem = waitFor("the wheel to show " + name,
            () -> showing(window, entry.date), left(deadline));
        if (problem != null) {
            return new Outcome(name, null, problem);
        }

        onScreen(window, () -> {
            window.skymap().showReading(SkymapPanel.ReadingTier.REPORT);
            return null;
        });
        problem = waitFor("the reading for " + name,
            () -> Boolean.TRUE.equals(onScreen(window,
                () -> ChartExporter.hasReading(window.interpretation().getEditorPane()))),
            left(deadline));
        if (problem != null) {
            return new Outcome(name, null, problem);
        }

        final File file = new File(dir, fileNameFor(name, taken));
        String failed = onScreen(window, () -> {
            try {
                com.zodiacomputing.ourania.astro.ChartSubject who = window.skymap().chartASubject();
                ChartExporter.writeReadingPdf(window.skymap().chartComponent(),
                    window.interpretation().getEditorPane(), file,
                    who == null ? name : who.label,
                    who == null ? "" : who.summary());
                return null;
            } catch (Exception | LinkageError e) {
                // Half a PDF under the name the reader will look for is worse than none.
                file.delete();
                return String.valueOf(e.getMessage());
            }
        });
        return failed == null ? new Outcome(name, file, null)
            : new Outcome(name, null, "could not write the PDF: " + failed);
    }

    /** True once the wheel is showing a chart cast for this date. */
    private static boolean showing(OuraniaWindow window, String isoDate) {
        Boolean yes = onScreen(window, () -> {
            com.zodiacomputing.ourania.astro.ChartSubject a = window.skymap().chartASubject();
            if (a == null || a.moment == null) {
                return Boolean.FALSE;
            }
            return a.moment.toLocalDate().toString().equals(isoDate);
        });
        return Boolean.TRUE.equals(yes);
    }

    private static long left(long deadline) {
        return Math.max(0L, deadline - System.currentTimeMillis());
    }

    /** Runs it on the event thread and waits, because everything above touches the screen. */
    private static <T> T onScreen(OuraniaWindow window, java.util.concurrent.Callable<T> job) {
        final Object[] held = new Object[1];
        try {
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                held[0] = job.call();
            } else {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    try {
                        held[0] = job.call();
                    } catch (Exception e) {
                        held[0] = null;
                    }
                });
            }
        } catch (Exception e) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T t = (T) held[0];
        return t;
    }

    /**
     * Waits for a condition, or gives up saying what it was waiting for.
     *
     * <b>Never a fixed sleep.</b> See the class comment: a slept guess is right on a fast machine
     * and wrong on a loaded one, and this machine has been measured varying by seven times.
     */
    static String waitFor(String what, java.util.function.BooleanSupplier done, long limitMs) {
        long deadline = System.currentTimeMillis() + limitMs;
        while (System.currentTimeMillis() < deadline) {
            if (done.getAsBoolean()) {
                return null;
            }
            try {
                Thread.sleep(40);
            } catch (InterruptedException stop) {
                Thread.currentThread().interrupt();
                return "interrupted while waiting for " + what;
            }
        }
        return "gave up waiting for " + what;
    }
}
