package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Rodden;

import java.util.ArrayList;
import java.util.List;

/**
 * Bringing a book of charts in from another program (G1).
 *
 * <p><b>Part B is the one this row exists for.</b> {@code 03/04/1985} is the third of April in
 * Britain and the fourth of March in America. A guess splits a practitioner's book silently down
 * the middle, and nothing ever tells them - the charts all cast, they are just other people's. So
 * the assertions that matter here are the refusals.
 *
 * <p><b>Every fixture is written to a temporary file and read back.</b> Asserting the parser
 * against strings it was handed would leave the byte-order mark, the separator and the line
 * endings untested, and all three are things exporters actually emit.
 */
public final class ChartImportCheck {

    private ChartImportCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: a file other software could have written", ChartImportCheck::ordinary);
        part("B: a date that could be two dates is refused", ChartImportCheck::ambiguous);
        part("C: times, quotes and separators", ChartImportCheck::shapes);
        part("D: nothing in the book is overwritten", ChartImportCheck::noOverwrite);
        part("E: a file that is not one says so", ChartImportCheck::rubbish);
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

    private static void ordinary() throws Exception {
        ChartImport.Result r = read(
            "Name,Birth Date,Birth Time,Birth Place,RR,Notes,Tags",
            "Ada Lovelace,1815-12-10,18:00,\"London, England\",AA,a mathematician,research",
            "Alan Turing,1912-06-23,02:15,\"London, England\",A,,research;maths");

        ok("both charts read (" + r.charts.size() + ")", r.charts.size() == 2);
        ok("and nothing was reported as a problem: " + r.problems, r.problems.isEmpty());
        if (r.charts.size() < 2) {
            return;
        }
        ChartImport.Candidate a = r.charts.get(0);
        eq("the name", "Ada Lovelace", a.name);
        eq("the date", "1815-12-10", a.date);
        eq("the time", "18:00", a.time);
        eq("the place, comma and all", "London, England", a.place);
        eq("the Rodden rating", Rodden.AA, a.rodden);
        eq("the notes", "a mathematician", a.notes);
        eq("the tags", "research", a.tags);
        eq("and the second chart's rating", Rodden.A, r.charts.get(1).rodden);

        // <b>Column order is the exporter's business.</b> The same two charts, columns shuffled
        // and named differently, have to come out the same.
        ChartImport.Result other = read(
            "dob,full_name,city,time_of_birth",
            "1815-12-10,Ada Lovelace,\"London, England\",18:00");
        ok("a differently-ordered, differently-named header reads the same",
            other.charts.size() == 1
            && "Ada Lovelace".equals(other.charts.get(0).name)
            && "1815-12-10".equals(other.charts.get(0).date)
            && "18:00".equals(other.charts.get(0).time));

        // A byte-order mark is not a column name.
        ChartImport.Result bom = read("﻿Name,Date", "Ada,1815-12-10");
        ok("a byte-order mark does not hide the first column", bom.charts.size() == 1);
    }

    // ---------------------------------------------------------------- Part B

    /**
     * The refusals.
     *
     * <b>An import that quietly reads half a book as American and half as British is the failure
     * this row could produce and never be blamed for</b>, because every chart still casts.
     */
    private static void ambiguous() throws Exception {
        ChartImport.Result mixed = read("Name,Date", "Ada,03/04/1985");
        ok("a slash date with both parts under thirteen is refused", mixed.charts.isEmpty());
        ok("and the line is named: " + mixed.problems,
            mixed.problems.size() == 1 && mixed.problems.get(0).contains("Line 2"));
        ok("and the reason says it was not guessed",
            mixed.problems.get(0).toLowerCase().contains("guess"));

        // Unambiguous on its own: 25 cannot be a month.
        ChartImport.Result clear = read("Name,Date", "Ada,25/04/1985");
        ok("a day above twelve is unambiguous", clear.charts.size() == 1);
        eq("and reads day first", "1985-04-25", clear.charts.get(0).date);

        ChartImport.Result us = read("Name,Date", "Ada,04/25/1985");
        eq("the other way round too", "1985-04-25", us.charts.get(0).date);

        // <b>The file teaches the parser its own order.</b> One unambiguous row makes the rest
        // readable, which is what turns a refusal into an import for a real export.
        ChartImport.Result taught = read("Name,Date",
            "Ada,25/04/1985",
            "Bee,03/04/1985");
        ok("one unambiguous row settles the file (" + taught.dateOrder + ")",
            taught.charts.size() == 2);
        eq("so the ambiguous one is read the same way", "1985-04-03",
            taught.charts.get(1).date);

        ChartImport.Result taughtUs = read("Name,Date",
            "Ada,04/25/1985",
            "Bee,03/04/1985");
        eq("and the other order is learned as readily", "1985-03-04",
            taughtUs.charts.get(1).date);

        // <b>A file that contradicts itself teaches nothing.</b> Taking either reading would be
        // wrong for half of it.
        ChartImport.Result contradictory = read("Name,Date",
            "Ada,25/04/1985",
            "Bee,04/25/1985",
            "Cal,03/04/1985");
        ok("a file with both orders in it still refuses the ambiguous row ("
            + contradictory.charts.size() + " of 3)", contradictory.charts.size() == 2);

        // A two-digit year is its own guess and is refused.
        ok("a two-digit year is refused", read("Name,Date", "Ada,25/04/85").charts.isEmpty());
        ok("an impossible month is refused",
            read("Name,Date", "Ada,1985-13-01").charts.isEmpty());
        ok("and an empty date", read("Name,Date", "Ada,").charts.isEmpty());
    }

    // ---------------------------------------------------------------- Part C

    private static void shapes() throws Exception {
        eq("a 24-hour time", "18:05", ChartImport.normaliseTime("18:05"));
        eq("an afternoon time written with pm", "18:05", ChartImport.normaliseTime("6:05 pm"));
        eq("midnight written as 12am", "00:30", ChartImport.normaliseTime("12:30 AM"));
        eq("noon written as 12pm", "12:30", ChartImport.normaliseTime("12:30 pm"));
        eq("a time with a dot", "18:05", ChartImport.normaliseTime("18.05"));
        eq("an hour alone", "18:00", ChartImport.normaliseTime("18"));
        // <b>An unreadable time becomes noon rather than refusing the chart</b>, because a chart
        // with no time is a real thing the Rodden rating already describes.
        eq("no time at all", "12:00", ChartImport.normaliseTime(""));
        eq("nonsense", "12:00", ChartImport.normaliseTime("about teatime"));
        eq("and an impossible hour", "12:00", ChartImport.normaliseTime("99:99"));

        ok("a semicolon file is read", read("Name;Date", "Ada;1815-12-10").charts.size() == 1);
        ok("a tab file is read",
            read("Name\tDate", "Ada\t1815-12-10").charts.size() == 1);
        eq("the separator is taken from the header", ';',
            ChartImport.separatorOf("Name;Date;Place"));

        // The quoting, which is the thing that silently shifts every later column.
        List<String> cells = ChartImport.split("Ada,\"London, England\",AA", ',');
        ok("a quoted comma stays inside its cell (" + cells + ")",
            cells.size() == 3 && "London, England".equals(cells.get(1)));
        ok("a doubled quote is one quote",
            "she said \"yes\"".equals(ChartImport.split("\"she said \"\"yes\"\"\"", ',').get(0)));

        // A missing place is imported and said out loud, because it is recoverable by hand.
        ChartImport.Result noPlace = read("Name,Date,Place", "Ada,1815-12-10,");
        ok("a chart with no place still imports", noPlace.charts.size() == 1);
        ok("and is reported: " + noPlace.problems,
            noPlace.problems.size() == 1
            && noPlace.problems.get(0).contains("no birth place"));
    }

    // ---------------------------------------------------------------- Part D

    /**
     * An import never destroys what is already in the book.
     *
     * <b>The version a practitioner has corrected by hand is the one worth keeping</b>, and an
     * overwrite would take it without saying anything.
     */
    private static void noOverwrite() throws Exception {
        SavedCharts.put("Ada Lovelace", "1815-12-10", "18:00", "London, England");
        ChartImport.Result r = read("Name,Date,Time,Place",
            "Ada Lovelace,1900-01-01,06:00,Elsewhere");
        List<String> renamed = new ArrayList<>();
        int added = ChartImport.add(r, renamed);

        ok("the incoming chart was added (" + added + ")", added == 1);
        eq("the one already there is untouched", "1815-12-10",
            SavedCharts.get("Ada Lovelace").date);
        ok("the newcomer came in beside it: " + renamed, renamed.size() == 1);
        ok("under a numbered name", SavedCharts.get("Ada Lovelace (2)") != null);
        eq("carrying its own data", "1900-01-01",
            SavedCharts.get("Ada Lovelace (2)").date);

        SavedCharts.remove("Ada Lovelace");
        SavedCharts.remove("Ada Lovelace (2)");
    }

    // ---------------------------------------------------------------- Part E

    private static void rubbish() throws Exception {
        ChartImport.Result empty = read();
        ok("an empty file says so", empty.charts.isEmpty() && !empty.problems.isEmpty());

        ChartImport.Result noHeader = read("1815-12-10,Ada,London");
        ok("a file with no column names is refused", noHeader.charts.isEmpty());
        ok("and says what it needs: " + noHeader.problems,
            !noHeader.problems.isEmpty()
            && noHeader.problems.get(0).contains("name the columns"));

        ChartImport.Result noName = read("Date,Place", "1815-12-10,London");
        ok("a file with no name column is refused", noName.charts.isEmpty());

        ChartImport.Result blankName = read("Name,Date", ",1815-12-10");
        ok("a row with no name is left out", blankName.charts.isEmpty());
        ok("and named", blankName.problems.get(0).contains("Line 2"));

        ChartImport.Result missing = ChartImport.read(new java.io.File("no-such-file.csv"));
        ok("a file that is not there does not throw",
            missing.charts.isEmpty() && !missing.problems.isEmpty());

        // Blank lines between records are ordinary and must not become problems.
        // The place column is here on purpose: without it both rows draw the "no birth place"
        // notice, which is correct behaviour and not what this assertion is about. The first
        // version of this fixture left it out and failed against the right answer.
        ChartImport.Result gaps = read("Name,Date,Place",
            "Ada,1815-12-10,London", "", "Bee,1912-06-23,London", "");
        ok("blank lines are skipped, not reported (" + gaps.problems + ")",
            gaps.charts.size() == 2 && gaps.problems.isEmpty());
    }

    // ---------------------------------------------------------------- plumbing

    /** Writes the lines to a real file and reads them back, so the file half is tested too. */
    private static ChartImport.Result read(String... lines) throws Exception {
        java.io.File f = java.io.File.createTempFile("ourania-import-", ".csv");
        f.deleteOnExit();
        java.nio.file.Files.write(f.toPath(), String.join("\n", lines)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return ChartImport.read(f);
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
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void eq(String what, Object want, Object got) {
        ok(what + " (" + got + ")", want == null ? got == null : want.equals(got));
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
