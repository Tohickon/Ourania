package com.zodiacomputing.ourania.gui;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * A report for every chart you choose (B8).
 *
 * <p><b>Part A is most of this suite, and that is the right shape.</b> The orchestration is
 * waiting on two asynchronous stages and can only really be held end to end, slowly, once - which
 * Part D does. The naming rule runs on every chart in every batch, is pure, and carries more
 * traps than it looks: a chart is named by a reader and a file is not.
 *
 * <p><b>Three of Part A's cases are about Windows specifically</b> and none of them would fail on
 * a developer's Linux box: {@code CON.pdf} cannot be created at all, a trailing dot is silently
 * dropped so "Ada." and "Ada" are the same file, and a colon in "Ada 12:30" is a stream
 * separator. This app runs on Windows.
 *
 * <p><b>What this suite does NOT claim, said here rather than discovered later:</b> that the
 * reading inside each report belongs to the chart on its cover. The batch clears the
 * interpretation pane before each chart precisely so that "the reading is ready" cannot be
 * answered by the previous one still sitting there - but this app draws PDF text as outlines to
 * keep the astrological glyphs, so there is no text in the file to read back and compare. Part D
 * gets as close as it can: two charts fifteen years apart must not produce identical documents.
 * A stale reading under a fresh wheel would slip past that, and it is the defect in this feature
 * most worth being afraid of.
 */
public final class BatchReportsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        part("A: a file name from a chart name", BatchReportsCheck::naming);
        part("B: a run says what it wrote", BatchReportsCheck::bookkeeping);
        part("C: a wait gives up rather than hanging", BatchReportsCheck::waiting);
        part("D: a real batch writes real reports", BatchReportsCheck::endToEnd);

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

    private static void naming() {
        Set<String> taken = new LinkedHashSet<>();
        eq("an ordinary name is left alone", "Ada Lovelace.pdf",
            BatchReports.fileNameFor("Ada Lovelace", taken));

        // <b>A date in a chart name is three directories.</b> The most likely thing a reader
        // types after a name, and the one that would fail silently as a path.
        taken.clear();
        eq("slashes cannot make directories", "Ada 10-12-1815.pdf",
            BatchReports.fileNameFor("Ada 10/12/1815", taken));
        taken.clear();
        eq("a colon is not a stream separator", "Ada 12-30.pdf",
            BatchReports.fileNameFor("Ada 12:30", taken));
        taken.clear();
        eq("every illegal character goes", "a-b-c-d-e-f-g-h-i.pdf",
            BatchReports.fileNameFor("a\\b/c:d*e?f\"g<h>i", taken));

        // <b>Windows drops a trailing dot silently</b>, so without this "Ada." and "Ada" are one
        // file and the second chart quietly overwrites the first.
        taken.clear();
        eq("a trailing dot goes", "Ada.pdf", BatchReports.fileNameFor("Ada.", taken));
        eq("and the next one is not the same file", "Ada (2).pdf",
            BatchReports.fileNameFor("Ada", taken));

        // Device names cannot be files whatever the extension.
        taken.clear();
        eq("CON is not a file name", "CON-chart.pdf", BatchReports.fileNameFor("CON", taken));
        taken.clear();
        eq("nor is nul, in any case", "nul-chart.pdf", BatchReports.fileNameFor("nul", taken));
        taken.clear();
        eq("nor COM1", "COM1-chart.pdf", BatchReports.fileNameFor("COM1", taken));
        taken.clear();
        eq("but Connie is fine", "Connie.pdf", BatchReports.fileNameFor("Connie", taken));

        taken.clear();
        eq("a nameless chart still gets a file", "chart.pdf",
            BatchReports.fileNameFor("   ", taken));
        eq("and the next one", "chart (2).pdf", BatchReports.fileNameFor(null, taken));

        // Two different chart names can sanitise to the same thing.
        taken.clear();
        eq("first", "Ada-Lovelace.pdf", BatchReports.fileNameFor("Ada/Lovelace", taken));
        eq("a different name that sanitises the same does not overwrite it",
            "Ada-Lovelace (2).pdf", BatchReports.fileNameFor("Ada:Lovelace", taken));
        eq("nor does a third", "Ada-Lovelace (3).pdf",
            BatchReports.fileNameFor("Ada*Lovelace", taken));

        taken.clear();
        String long1 = BatchReports.fileNameFor(repeat("A very long chart name ", 20), taken);
        ok("a very long name is cut to something a path can hold (" + long1.length() + ")",
            long1.length() <= 88);
        ok("and still ends in .pdf", long1.endsWith(".pdf"));

        // Collision matching ignores case, because the filesystem does.
        taken.clear();
        BatchReports.fileNameFor("Ada", taken);
        eq("case does not make a different file", "ADA (2).pdf",
            BatchReports.fileNameFor("ADA", taken));
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ B

    private static void bookkeeping() {
        BatchReports.Run run = new BatchReports.Run();
        run.outcomes.add(new BatchReports.Outcome("Ada", new File("Ada.pdf"), null));
        run.outcomes.add(new BatchReports.Outcome("Bob", null, "no longer in the chart book"));
        run.outcomes.add(new BatchReports.Outcome("Cy", new File("Cy.pdf"), null));

        ok("two were written (" + run.written() + ")", run.written() == 2);
        ok("one was not (" + run.failed() + ")", run.failed() == 1);
        ok("an outcome with a file worked", run.outcomes.get(0).worked());
        ok("an outcome with a problem did not", !run.outcomes.get(1).worked());
        ok("and says which chart and why", run.outcomes.get(1).toString().contains("Bob")
            && run.outcomes.get(1).toString().contains("chart book"));

        // <b>A cancelled run still reports what it wrote.</b> Stopping after thirty of forty is
        // not a failure of thirty, and a dialog saying only "cancelled" sends a practitioner
        // looking in a folder that is not empty.
        ok("a finished run does not claim to have stopped", !run.summary().contains("stopped"));
        run.cancelled = true;
        ok("a stopped run still counts what it wrote", run.summary().startsWith("2 reports"));
        ok("and says it stopped", run.summary().contains("stopped"));
    }

    // ------------------------------------------------------------------ C

    private static void waiting() {
        // <b>A deadline, not a sleep.</b> ec44b792's rule, after two suites flaked under
        // regression load on slept guesses.
        long began = System.currentTimeMillis();
        String gaveUp = BatchReports.waitFor("something that never happens", () -> false, 300);
        long took = System.currentTimeMillis() - began;
        ok("it gives up (" + gaveUp + ")", gaveUp != null);
        ok("and says what it was waiting for",
            gaveUp != null && gaveUp.contains("something that never happens"));
        ok("and takes about as long as it was given (" + took + "ms)", took >= 250 && took < 4000);

        began = System.currentTimeMillis();
        String fine = BatchReports.waitFor("something already true", () -> true, 30000);
        took = System.currentTimeMillis() - began;
        ok("a condition already true returns at once (" + took + "ms)", fine == null && took < 500);

        // Half way: true only after a moment, and well inside the limit.
        final long flips = System.currentTimeMillis() + 200;
        String soon = BatchReports.waitFor("something that becomes true",
            () -> System.currentTimeMillis() > flips, 8000);
        ok("and one that becomes true is waited for", soon == null);
    }

    // ------------------------------------------------------------------ D

    /**
     * One real batch, end to end.
     *
     * <b>Two charts, not one.</b> A batch of one would pass with the naming, the collision
     * handling and the loop all broken; the second chart is what proves it moved on.
     */
    private static void endToEnd() {
        File dir;
        try {
            dir = Files.createTempDirectory("ourania-b8").toFile();
        } catch (Exception e) {
            fail("could not make a folder to write into: " + e);
            return;
        }

        SavedCharts.put("Batch One", "1990-01-15", "09:30", "London, UK");
        SavedCharts.put("Batch Two", "1975-06-02", "14:45", "London, UK");
        ok("the two charts are in the book", SavedCharts.get("Batch One") != null
            && SavedCharts.get("Batch Two") != null);

        final OuraniaWindow[] win = new OuraniaWindow[1];
        try {
            SwingUtilities.invokeAndWait(() -> win[0] = new OuraniaWindow());
        } catch (Exception e) {
            fail("could not build a window: " + e);
            return;
        }

        try {
            BatchReports.Run run = BatchReports.run(win[0],
                java.util.Arrays.asList("Batch One", "Batch Two"), dir, null);

            ok("both charts were attempted (" + run.outcomes.size() + ")",
                run.outcomes.size() == 2);
            for (BatchReports.Outcome o : run.outcomes) {
                ok("wrote " + o.chart + " (" + (o.worked() ? o.file.getName() : o.problem) + ")",
                    o.worked());
                if (o.worked()) {
                    ok(o.chart + "'s file is on disk and is not empty ("
                        + o.file.length() + " bytes)", o.file.isFile() && o.file.length() > 1000);
                }
            }
            ok("the run was not cancelled", !run.cancelled);
            ok("and the two files have different names",
                run.outcomes.size() == 2 && run.outcomes.get(0).worked()
                    && run.outcomes.get(1).worked()
                    && !run.outcomes.get(0).file.getName()
                        .equals(run.outcomes.get(1).file.getName()));

            // <b>And different contents.</b> Two people born fifteen years apart do not produce
            // the same document; identical files would mean the batch exported the first chart
            // twice under two names, which is the failure that looks most like success.
            if (run.outcomes.size() == 2 && run.outcomes.get(0).worked()
                && run.outcomes.get(1).worked()) {
                try {
                    byte[] one = Files.readAllBytes(run.outcomes.get(0).file.toPath());
                    byte[] two = Files.readAllBytes(run.outcomes.get(1).file.toPath());
                    ok("the two reports are not the same document ("
                        + one.length + " and " + two.length + " bytes)",
                        !java.util.Arrays.equals(one, two));
                } catch (Exception e) {
                    fail("could not read the two reports back: " + e);
                }
            }
        } finally {
            SavedCharts.remove("Batch One");
            SavedCharts.remove("Batch Two");
            try {
                SwingUtilities.invokeAndWait(() -> win[0].dispose());
            } catch (Exception ignored) {
                // Nothing useful to do about a window that will not close in a check.
            }
        }
    }

    // ------------------------------------------------------------------

    private static void eq(String label, String want, String got) {
        checks++;
        if (!want.equals(got)) {
            failures.add(label + " - wanted \"" + want + "\", got \"" + got + "\"");
        }
    }

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
        System.out.println();
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        try {
            body.run();
        } catch (RuntimeException e) {
            fail(name + " threw " + e);
        }
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

    private static void fail(String label) {
        checks++;
        failures.add(label);
    }
}
