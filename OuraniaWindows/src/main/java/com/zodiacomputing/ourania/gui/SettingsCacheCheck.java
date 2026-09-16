package com.zodiacomputing.ourania.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The settings file is parsed once per change, not once per question.
 *
 * <p><b>Why this suite exists.</b> David, 2026-09-15: "I could instantly tell it was making my
 * computer work hard just to use it." Measured: one repaint of the wheel asked for a setting
 * <b>694 times</b> - the palette is consulted per body, per aspect, per ring - and each question
 * opened and parsed the file again, at about 139 microseconds a time. That was 96 of the 109
 * milliseconds a frame took. Caching it took the median repaint from <b>81.9 ms to 11.4 ms</b>.
 *
 * <p>A cache is only safe while it is honest about staleness, so what is held here is both halves:
 * that reading is cheap, and that a change still gets through - our own writes at once, and an edit
 * made outside the app within {@link Settings#STAT_INTERVAL_MS}. The second matters more than it
 * looks: this tree is worked by two agents, and the other one edits settings too.
 */
public final class SettingsCacheCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        part("A: a question is cheap", SettingsCacheCheck::cheap);
        part("B: our own writes are seen at once", SettingsCacheCheck::ownWrites);
        part("C: an edit from outside gets through", SettingsCacheCheck::outsideEdit);
        part("D: the cache follows the file it is told to use", SettingsCacheCheck::scratchFile);

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
     * <b>A time bound, and a generous one on purpose.</b> This machine varies about fourfold on
     * identical work, so the threshold is set where only the defect can cross it: 50,000 reads
     * take about 0.13 seconds cached and about 7 seconds if every one of them parses the file.
     * Three seconds separates those two answers by a mile in both directions.
     */
    private static void cheap() {
        Settings.set("cache.probe", "value");
        // Warm: the first call is allowed to read the file, which is the whole point.
        Settings.get("cache.probe", "");
        long t0 = System.nanoTime();
        int n = 50000;
        String last = null;
        for (int i = 0; i < n; i++) {
            last = Settings.get("cache.probe", "");
        }
        double seconds = (System.nanoTime() - t0) / 1.0e9;
        eq("and the answer is the stored one", "value", last);
        ok(n + " reads took " + String.format("%.2f", seconds)
            + " seconds, which is parsing once rather than " + n + " times", seconds < 3.0);
        ok("so a read is worth under a tenth of a millisecond, "
            + String.format("%.1f", seconds / n * 1.0e6) + " microseconds",
            seconds / n * 1.0e6 < 100.0);
    }

    // ------------------------------------------------------------------ B

    private static void ownWrites() {
        Settings.set("cache.write", "first");
        eq("what was just written is what is read", "first", Settings.get("cache.write", ""));
        Settings.set("cache.write", "second");
        eq("and again, with no wait in between", "second", Settings.get("cache.write", ""));
        Settings.update(p -> p.setProperty("cache.write", "third"));
        eq("an update is seen too", "third", Settings.get("cache.write", ""));

        // Two keys in one file: a write must not drop what it did not touch.
        Settings.set("cache.other", "kept");
        Settings.set("cache.write", "fourth");
        eq("a write leaves the other keys alone", "kept", Settings.get("cache.other", ""));
        eq("and lands its own", "fourth", Settings.get("cache.write", ""));
    }

    // ------------------------------------------------------------------ C

    private static void outsideEdit() throws Exception {
        Settings.set("cache.outside", "before");
        eq("read once so it is cached", "before", Settings.get("cache.outside", ""));

        // Somebody else writes the file - the other agent, or a hand edit.
        File f = new File(System.getProperty(Settings.FILE_PROPERTY));
        Properties p = new Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            p.load(in);
        }
        p.setProperty("cache.outside", "after");
        p.setProperty("cache.padding", "so the file length changes as well as its stamp");
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "written from outside");
        }

        Thread.sleep(Settings.STAT_INTERVAL_MS + 250);
        eq("an edit from outside is picked up, within half a second", "after",
            Settings.get("cache.outside", ""));
        eq("and the key it added with it", "so the file length changes as well as its stamp",
            Settings.get("cache.padding", ""));
    }

    // ------------------------------------------------------------------ D

    private static void scratchFile() {
        Settings.set("cache.file", "in the first file");
        eq("cached from the first file", "in the first file", Settings.get("cache.file", ""));
        // useScratchFile points at a different file; the parsed copy of the old one is now wrong.
        Settings.useScratchFile();
        Settings.set("cache.file", "in the second file");
        eq("switching files does not serve the old one", "in the second file",
            Settings.get("cache.file", ""));
        ok("and forget() sends the next read back to disk", forgetWorks());
    }

    private static boolean forgetWorks() {
        Settings.set("cache.forget", "stored");
        Settings.get("cache.forget", "");
        Settings.forget();
        return "stored".equals(Settings.get("cache.forget", ""));
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
}
