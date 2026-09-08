package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Rodden;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The chart book: what it stores about a chart, and what it must never lose.
 *
 * <p><b>This store holds the one thing in the app nobody can retype.</b> A reading can be
 * regenerated and a layout redrawn, but a client's birth time came from a document that may no
 * longer exist. Until now the book could only add and replace - there was no delete, no
 * rename, no backup, and no record of how well any time was known - so a rectified guess and a
 * birth certificate sat in the same list looking identical.
 *
 * <p>Every write here is read-modify-write. Part D is the one that would otherwise rot: this
 * project has already destroyed a shared data file once by writing it wholesale, and the
 * failure was silent.
 */
public final class ChartBookCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final String FILE = "saved_charts.properties";
    private static File backup;

    public static void main(String[] args) throws Exception {
        // Never the reader's own settings file: a suite that generates a chart persists it,
        // and one of these once overwrote a saved birth chart. See Settings.useScratchFile.
        Settings.useScratchFile();
        // The suite writes to the real store, so the real one is set aside first and put back
        // in a finally - a check that eats the user's charts would be worse than no check.
        File live = new File(FILE);
        if (live.exists()) {
            backup = File.createTempFile("chartbook", ".bak");
            java.nio.file.Files.copy(live.toPath(), backup.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            fresh();

            System.out.println("=== Part A: a chart remembers how good its time is ===");
            int before = failures.size();
            ratings();
            report("Part A", before);

            System.out.println();
            System.out.println("=== Part B: delete and rename ===");
            before = failures.size();
            deleteAndRename();
            report("Part B", before);

            System.out.println();
            System.out.println("=== Part C: search and grouping ===");
            before = failures.size();
            searching();
            report("Part C", before);

            System.out.println();
            System.out.println("=== Part D: a write never costs another chart ===");
            before = failures.size();
            neverLoses();
            report("Part D", before);

            System.out.println();
            System.out.println("=== Part E: backup and merge ===");
            before = failures.size();
            backupAndMerge();
            report("Part E", before);

            System.out.println();
            System.out.println("=== Part F: a deleted legacy chart stays deleted ===");
            before = failures.size();
            legacyDelete();
            report("Part F", before);
        } finally {
            restore();
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
        System.exit(0);
    }

    private static void ratings() {
        SavedCharts.put("Ada", "1815-12-10", "12:00", "London, UK",
            Rodden.AA, "from the register", "historical, research");
        SavedCharts.Entry e = SavedCharts.get("Ada");
        yes("a saved chart comes back", e != null);
        eq("the rating is kept", Rodden.AA, e.rodden);
        eq("the notes are kept", "from the register", e.notes);
        eq("the tags are kept", "historical, research", e.tags);
        eq("the tags split", 2, e.tagList().size());

        // <b>An unrated chart must not claim a document.</b> Defaulting the other way would
        // silently upgrade every chart already in the book to "birth certificate".
        eq("an unknown code defaults to A, not AA", Rodden.A, Rodden.of("ZZ"));
        eq("a null code defaults to A", Rodden.A, Rodden.of(null));
        eq("a lowercase code still reads", Rodden.DD, Rodden.of("dd"));

        // Only X changes the calculation; the rest are provenance the reader weighs.
        for (Rodden r : Rodden.values()) {
            eq(r.code + " changes the cast only when it is X",
                r == Rodden.X, r.timeUnknown());
            yes(r.code + " explains itself", r.meaning != null && r.meaning.length() > 20);
        }
        yes("rectified angles are flagged uncertain", Rodden.R.anglesUncertain());
        yes("conflicting angles are flagged uncertain", Rodden.DD.anglesUncertain());
        yes("a recorded time is not flagged uncertain", !Rodden.AA.anglesUncertain());

        // The old four-argument save must not wipe a rating already stored.
        SavedCharts.put("Ada", "1815-12-10", "13:00", "London, UK");
        eq("editing the time keeps the rating", Rodden.AA, SavedCharts.get("Ada").rodden);
        eq("editing the time keeps the notes",
            "from the register", SavedCharts.get("Ada").notes);
    }

    private static void deleteAndRename() {
        SavedCharts.put("Temp", "1990-01-01", "12:00", "Paris, FR",
            Rodden.C, "note", "tagged");
        yes("a chart can be deleted", SavedCharts.remove("Temp"));
        yes("a deleted chart is gone", SavedCharts.get("Temp") == null);
        yes("deleting what is not there reports so", !SavedCharts.remove("Temp"));

        SavedCharts.put("Before", "1970-05-05", "09:00", "Rome, IT",
            Rodden.B, "keep me", "x");
        yes("a chart can be renamed", SavedCharts.rename("Before", "After"));
        yes("the old name is gone", SavedCharts.get("Before") == null);
        SavedCharts.Entry moved = SavedCharts.get("After");
        yes("the renamed chart exists", moved != null);
        eq("the rename carried the date", "1970-05-05", moved.date);
        eq("the rename carried the rating", Rodden.B, moved.rodden);
        eq("the rename carried the notes", "keep me", moved.notes);

        // <b>A rename must never overwrite somebody else's birth data.</b>
        SavedCharts.put("Other", "1980-02-02", "10:00", "Oslo, NO");
        yes("renaming onto an existing name is refused",
            !SavedCharts.rename("After", "Other"));
        eq("the chart it would have overwritten is untouched",
            "1980-02-02", SavedCharts.get("Other").date);
        yes("renaming to blank is refused", !SavedCharts.rename("After", "   "));
        yes("renaming to the same name is refused", !SavedCharts.rename("After", "After"));
    }

    private static void searching() {
        fresh();
        SavedCharts.put("Ada Lovelace", "1815-12-10", "12:00", "London, UK",
            Rodden.AA, "mathematician", "historical, research");
        SavedCharts.put("Bob Client", "1975-03-04", "08:15", "Leeds, UK",
            Rodden.A, "first session May", "client");
        SavedCharts.put("Cara", "1999-09-09", "23:45", "Cork, IE", Rodden.X, "", "family");

        eq("an empty search returns everything", 3, SavedCharts.search("", "").size());
        eq("a name search finds one", 1, SavedCharts.search("ada", "").size());
        eq("search is case-insensitive", 1, SavedCharts.search("ADA", "").size());
        eq("a notes search finds one", 1, SavedCharts.search("session", "").size());
        eq("a place search finds two", 2, SavedCharts.search("uk", "").size());
        eq("a tag filter narrows", 1, SavedCharts.search("", "client").size());
        eq("tag and text together", 1, SavedCharts.search("ada", "research").size());
        eq("a tag that excludes returns none", 0, SavedCharts.search("ada", "client").size());

        List<String> tags = SavedCharts.tags();
        eq("every tag in use is listed", 4, tags.size());
        yes("tags are de-duplicated and sorted",
            tags.indexOf("client") < tags.indexOf("family"));
    }

    /**
     * The rule this project learned the hard way: a write touches one chart.
     *
     * A wholesale rewrite of a shared data file deleted 192 entries once, silently. Everything
     * here writes through read-modify-write, and this is what says so.
     */
    private static void neverLoses() {
        fresh();
        for (int i = 0; i < 12; i++) {
            SavedCharts.put("Chart" + i, "19" + (70 + i) + "-01-01", "12:00", "Place" + i);
        }
        eq("twelve charts are stored", 12, SavedCharts.names().size());

        SavedCharts.put("Chart5", "1975-06-06", "18:30", "Elsewhere", Rodden.DD, "n", "t");
        eq("editing one chart leaves eleven others", 12, SavedCharts.names().size());
        eq("the edited chart changed", "1975-06-06", SavedCharts.get("Chart5").date);
        eq("its neighbour did not", "1974-01-01", SavedCharts.get("Chart4").date);

        SavedCharts.remove("Chart5");
        eq("deleting one leaves eleven", 11, SavedCharts.names().size());
        eq("a neighbour survives the delete", "1976-01-01", SavedCharts.get("Chart6").date);

        // A field this version does not know about must survive a write to the same chart.
        Properties p = new Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(FILE)) {
            p.load(in);
        } catch (Exception ex) {
            failures.add("could not read the book back: " + ex);
        }
        p.setProperty("Chart3.futureField", "kept");
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            p.store(out, "test");
        } catch (Exception ex) {
            failures.add("could not seed a future field: " + ex);
        }
        SavedCharts.put("Chart3", "1973-01-01", "12:00", "Place3");
        Properties after = new Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(FILE)) {
            after.load(in);
        } catch (Exception ex) {
            failures.add("could not re-read the book: " + ex);
        }
        eq("a field this version does not know survives a write",
            "kept", after.getProperty("Chart3.futureField"));

        // And a delete removes that unknown field too, rather than orphaning it.
        SavedCharts.remove("Chart3");
        Properties gone = new Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(FILE)) {
            gone.load(in);
        } catch (Exception ex) {
            failures.add("could not re-read after delete: " + ex);
        }
        yes("deleting a chart takes its unknown fields with it",
            gone.getProperty("Chart3.futureField") == null);
    }

    private static void backupAndMerge() throws Exception {
        fresh();
        SavedCharts.put("Keep", "1960-01-01", "01:00", "Here", Rodden.AA, "mine", "own");
        File out = File.createTempFile("book", ".properties");
        yes("the book exports", SavedCharts.exportBook(out));
        yes("the export is not empty", out.length() > 0);

        // A colleague's book, one of whose charts shares a name with one of ours.
        Properties theirs = new Properties();
        theirs.setProperty("Keep.date", "1800-01-01");
        theirs.setProperty("Theirs.date", "1985-07-07");
        theirs.setProperty("Theirs.time", "07:07");
        theirs.setProperty("Theirs.location", "Away");
        theirs.setProperty("Theirs.rodden", "DD");
        File in = File.createTempFile("theirs", ".properties");
        try (FileOutputStream o = new FileOutputStream(in)) {
            theirs.store(o, "colleague");
        }

        eq("an import adds only what is new", 1, SavedCharts.importBook(in));
        eq("our chart of the same name is untouched",
            "1960-01-01", SavedCharts.get("Keep").date);
        SavedCharts.Entry got = SavedCharts.get("Theirs");
        yes("the imported chart arrived", got != null);
        eq("the imported chart kept its rating", Rodden.DD, got.rodden);
        eq("nothing else was lost", 2, SavedCharts.names().size());
        out.delete();
        in.delete();
    }

    /**
     * The legacy file, which {@code read()} merges in for any name the current file lacks.
     *
     * The suite has to set this aside too, or every count here is one high - and that is how
     * the delete bug below was found.
     */
    private static final String LEGACY = "saved_transits.properties";
    private static File legacyBackup;

    private static void fresh() {
        new File(FILE).delete();
        File l = new File(LEGACY);
        if (l.exists() && legacyBackup == null) {
            try {
                legacyBackup = File.createTempFile("legacy", ".bak");
                java.nio.file.Files.move(l.toPath(), legacyBackup.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                failures.add("could not set the legacy book aside: " + ex);
            }
        }
    }

    /**
     * A chart deleted out of the legacy file must stay deleted.
     *
     * <b>The delete used to report success and the chart came back.</b> {@code read()} merges
     * any legacy name the current file does not carry, so removing one wrote it out of the
     * current file and the very next read restored it from the old one. Nothing failed; the
     * user simply watched a chart they had deleted reappear, which is the worst shape a data
     * bug takes - it looks like the app is haunted rather than wrong.
     */
    private static void legacyDelete() throws Exception {
        fresh();
        Properties old = new Properties();
        old.setProperty("Ghost.date", "1900-01-01");
        old.setProperty("Ghost.time", "06:00");
        old.setProperty("Ghost.location", "Old Town");
        try (FileOutputStream o = new FileOutputStream(LEGACY)) {
            old.store(o, "legacy book");
        }
        SavedCharts.put("Living", "2000-01-01", "12:00", "New Town");

        yes("a legacy chart is visible", SavedCharts.get("Ghost") != null);
        yes("a legacy chart can be deleted", SavedCharts.remove("Ghost"));
        yes("and it stays deleted", SavedCharts.get("Ghost") == null);
        yes("it is still gone on a later read", !SavedCharts.names().contains("Ghost"));
        yes("the chart beside it survived", SavedCharts.get("Living") != null);

        // Saving the name again must bring it back - a delete is not a permanent ban.
        SavedCharts.put("Ghost", "1901-02-02", "07:00", "Elsewhere");
        SavedCharts.Entry back = SavedCharts.get("Ghost");
        yes("re-saving a deleted name works", back != null);
        eq("and it is the new data, not the legacy data", "1901-02-02", back.date);

        // The tombstone marker is bookkeeping, not a chart.
        yes("the deletion marker is not listed as a chart",
            !SavedCharts.names().contains("__deleted"));
        new File(LEGACY).delete();
    }

    private static void restore() {
        try {
            File live = new File(FILE);
            if (backup != null) {
                java.nio.file.Files.copy(backup.toPath(), live.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                backup.delete();
            } else {
                live.delete();
            }
            if (legacyBackup != null) {
                java.nio.file.Files.move(legacyBackup.toPath(), new File(LEGACY).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                legacyBackup = null;
            }
        } catch (Exception ex) {
            System.out.println("  WARNING: could not restore the real chart book: " + ex);
        }
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        System.out.println(part + (failures.size() == before ? ": clear" : ": FAILURES"));
    }
}
