package com.zodiacomputing.ourania.gui;

import javax.swing.SwingUtilities;

/**
 * Every shortcut is declared once, bound, and unique (G13).
 *
 * <p><b>Before this the app had two keys and no list.</b> F11 and Escape were bound inside
 * {@code WindowPlacement.installKeys} and known nowhere else; the six transport buttons had no keys at
 * all, and their behaviour existed only inside anonymous listeners - so a shortcut for Play/Pause could
 * only have been a second copy of the same two lines.
 *
 * <p><b>Part C is the assertion that earns its keep as the table grows:</b> no two rows claim the same
 * stroke. That is the failure a person cannot see by reading, and the one that gets worse with every
 * row added.
 */
public final class ShortcutCheck {

    private ShortcutCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: the table says what it should say", ShortcutCheck::table);
        part("B: every row is bound on the window", ShortcutCheck::bound);
        part("C: no two rows claim the same stroke", ShortcutCheck::unique);
        part("D: a key does what its button does", ShortcutCheck::behaviour);
        part("E: a label cannot name a key it does not have", ShortcutCheck::labels);
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

    // ---------------------------------------------------------------- Part A

    /**
     * The rows, written out by hand.
     *
     * <b>Deliberately, the way {@code PresetBarCheck}'s tab list is.</b> A list read off
     * {@code Key.values()} would agree with any change, including a shortcut quietly disappearing or
     * changing key under a reader who had learned it. Adding a shortcut should be a visible diff in
     * two places.
     */
    private static final String[][] ROWS = {
        {"FULL_SCREEN", "Full screen", "F11"},
        {"LEAVE_FULL_SCREEN", "Leave full screen", "Escape"},
        {"PLAY_PAUSE", "Play / Pause", "Ctrl+Space"},
        {"STEP_BACK", "Step back", "Ctrl+Left"},
        {"STEP_FORWARD", "Step forward", "Ctrl+Right"},
        {"NOW", "Now", "Ctrl+T"},
    };

    private static void table() {
        eq("the table has " + ROWS.length + " rows", ROWS.length, Shortcuts.Key.values().length);
        for (String[] row : ROWS) {
            Shortcuts.Key k = Shortcuts.Key.valueOf(row[0]);
            eq(row[0] + " is called \"" + row[1] + "\"", row[1], k.label);
            // getKeyText and getModifiersExText are the platform's own words, so this asserts what a
            // reader is actually shown rather than what the code meant.
            eq(row[0] + " is written as " + row[2], row[2], k.written());
            ok(row[0] + " says what it does", k.about != null && k.about.length() > 12);
        }
        // Every new key is modified, which was a decision: a bare letter is a character somebody is
        // typing, and bare arrows are already bound by the scrub bars and the place list on their own
        // components - so a bare binding would work until it silently did not.
        int bare = 0;
        for (Shortcuts.Key k : Shortcuts.Key.values()) {
            if (k.modifiers == 0 && k.code >= java.awt.event.KeyEvent.VK_A
                && k.code <= java.awt.event.KeyEvent.VK_Z) {
                bare++;
            }
        }
        eq("no row claims a bare letter", 0, bare);
    }

    // ---------------------------------------------------------------- Part B

    private static void bound() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            javax.swing.InputMap in = w[0].getRootPane()
                .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
            javax.swing.ActionMap act = w[0].getRootPane().getActionMap();
            for (Shortcuts.Key k : Shortcuts.Key.values()) {
                Object name = in.get(k.stroke());
                ok(k.label + " is bound to " + k.written() + ": " + name,
                    k.actionKey().equals(name));
                ok("and " + k.written() + " has an action behind it", act.get(k.actionKey()) != null);
            }
        } finally {
            final OuraniaWindow win = w[0];
            SwingUtilities.invokeAndWait(() -> win.dispose());
        }
    }

    // ---------------------------------------------------------------- Part C

    /**
     * <b>The one that gets more valuable with every row.</b> Two rows on one stroke is invisible to a
     * reader of the table and silent at runtime - the second binding simply replaces the first, so one
     * shortcut stops working and nothing says why.
     */
    private static void unique() {
        java.util.Map<javax.swing.KeyStroke, String> seen = new java.util.HashMap<>();
        java.util.List<String> clashes = new java.util.ArrayList<>();
        for (Shortcuts.Key k : Shortcuts.Key.values()) {
            String already = seen.put(k.stroke(), k.name());
            if (already != null) {
                clashes.add(already + " and " + k.name() + " both claim " + k.written());
            }
        }
        ok("no two rows claim one stroke: " + clashes, clashes.isEmpty());
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Shortcuts.Key k : Shortcuts.Key.values()) {
            names.add(k.actionKey());
        }
        eq("and every row binds under its own action name", Shortcuts.Key.values().length,
            names.size());
    }

    // ---------------------------------------------------------------- Part D

    /**
     * <b>Fired through the binding, not by calling the method.</b> Calling {@code playPause} would
     * pass whether or not the key was wired to it, which is the whole claim - the same reason
     * {@code ScrubCheck} throws on a real thread and H4b's Part G presses the search result.
     */
    private static void behaviour() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w[0], "skymapPanel");
            boolean before = sky.isPlaying();
            if (fire(w[0], Shortcuts.Key.PLAY_PAUSE)) {
                ok("Ctrl+Space starts time when it is stopped: " + sky.isPlaying(),
                    sky.isPlaying() != before);
                fire(w[0], Shortcuts.Key.PLAY_PAUSE);
                eq("and stops it again", before, sky.isPlaying());
            }

            if (fire(w[0], Shortcuts.Key.STEP_FORWARD)) {
                ok("Ctrl+Right runs time forwards", sky.isPlaying()
                    && ((Integer) CheckReflect.get(sky, "animationDirection")) > 0);
            }
            if (fire(w[0], Shortcuts.Key.STEP_BACK)) {
                ok("Ctrl+Left runs it backwards",
                    ((Integer) CheckReflect.get(sky, "animationDirection")) < 0);
                eq("at the slow rate, which is the table's choice", SkymapPanel.SLOW_DELAY_MS,
                    ((javax.swing.Timer) CheckReflect.get(sky, "animationTimer")).getDelay());
            }

            if (fire(w[0], Shortcuts.Key.NOW)) {
                ok("Ctrl+T stops time", !sky.isPlaying());
                java.time.ZonedDateTime natal =
                    (java.time.ZonedDateTime) CheckReflect.get(sky, "natalRing.time");
                long off = Math.abs(java.time.Duration.between(natal,
                    java.time.ZonedDateTime.now(natal.getZone())).toMinutes());
                ok("and moves the chart to this moment: " + off + " minutes away", off <= 2);
            }
        } finally {
            final OuraniaWindow win = w[0];
            SwingUtilities.invokeAndWait(() -> win.dispose());
        }
    }

    /**
     * Presses a shortcut through its binding.
     *
     * <b>Guarded, because the first version of this threw.</b> Mutating the installer to skip a row
     * made {@code fire} hit a null action and the whole suite died with a stack trace part way through
     * Part D - and <i>a crashed suite reports no failures at all</i>, which is the lesson
     * {@code HouseSystemsCheck} recorded on 26 Sep when two of its mutations needed guards before they
     * could be caught. A missing binding is now a named failure, and the parts after it still run.
     */
    private static boolean fire(OuraniaWindow w, Shortcuts.Key k) throws Exception {
        javax.swing.Action a = w.getRootPane().getActionMap().get(k.actionKey());
        if (a == null) {
            ok(k.label + " is bound, so its key can be pressed", false);
            return false;
        }
        SwingUtilities.invokeAndWait(() ->
            a.actionPerformed(new java.awt.event.ActionEvent(w, 0, k.actionKey())));
        SwingUtilities.invokeAndWait(() -> { });
        return true;
    }

    // ---------------------------------------------------------------- Part E

    /**
     * The tooltips take their key text from the table, so they cannot advertise a key that is not
     * bound - which is the drift this table exists to prevent, and is exactly how a menu label comes
     * to claim a shortcut somebody removed.
     */
    private static void labels() throws Exception {
        String code = JavaSource.codeOnly(read("SkymapPanel.java"));
        ok("the transport tooltips ask Shortcuts for the key text",
            code.contains("Shortcuts.hint"));
        for (String lit : JavaSource.literals(read("SkymapPanel.java"))) {
            if (lit.contains("Ctrl+") || lit.equals("F11")) {
                ok("no key is typed into a string in SkymapPanel: " + lit, false);
            }
        }
        checks++;
        System.out.println("  ok    no key is typed into a string in SkymapPanel");
        // And the hint is the same words the table gives, so the two cannot say different things.
        eq("the hint is the table's own text", " (Ctrl+Space)",
            Shortcuts.hint(Shortcuts.Key.PLAY_PAUSE));
        ok("WindowPlacement no longer binds keys of its own",
            !JavaSource.codeOnly(read("WindowPlacement.java")).contains("KeyStroke.getKeyStroke"));
    }

    // ---------------------------------------------------------------- plumbing

    private static String read(String name) throws Exception {
        java.io.File f = new java.io.File(
            "src/main/java/com/zodiacomputing/ourania/gui/" + name);
        return new String(java.nio.file.Files.readAllBytes(f.toPath()),
            java.nio.charset.StandardCharsets.UTF_8);
    }

    private interface Body {
        void run() throws Exception;
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

    private static void eq(String what, Object want, Object got) {
        ok(what + ": " + got, want.equals(got));
    }
}
