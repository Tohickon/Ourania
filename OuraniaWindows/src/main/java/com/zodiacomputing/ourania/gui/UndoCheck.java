package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Taking back a change, and the guard without which it works exactly once (G13).
 *
 * <p><b>Part C is the one this needed.</b> Undo redraws by calling {@code generateChart}, and
 * {@code generateChart} records the state it was called with - which, during an undo, is the state
 * undo has just restored. Left alone, the history grows by one every time it is walked back and
 * never gets anywhere: the first Ctrl+Z appears to work and the second does nothing. Found by
 * reading the call order before it shipped, and held here so it cannot come back.
 *
 * <p><b>The history is tested without a window.</b> A Deque of small maps is the whole model, so
 * the parts that matter - what redo means after a new edit, what the depth limit drops, what a
 * repeated state does - are ordinary assertions. Part D builds a real form for the one question
 * that needs one: whether the fields a reader types in were given their own undo.
 */
public final class UndoCheck {

    private UndoCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: the history goes back and forward", UndoCheck::backAndForward);
        part("B: a new edit makes the way not taken unreachable", UndoCheck::branches);
        part("C: replaying does not record what it is replaying", UndoCheck::replay);
        part("D: the form's fields can take back typing", UndoCheck::typing);
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

    // ---------------------------------------------------------------- Part A

    private static void backAndForward() {
        Undo.History h = new Undo.History();
        ok("a fresh history has nothing to undo", !h.canUndo());
        ok("and nothing to redo", !h.canRedo());
        ok("and undoing it answers null", h.undo() == null);

        // <b>One state is the present, and the present is not somewhere to go back to.</b> This
        // is the whole correction the check forced: a state is pushed when a chart is generated,
        // so the most recent push is what is on screen.
        h.push(state("a"));
        ok("one generated chart is still nothing to undo", !h.canUndo());

        h.push(state("b"));
        ok("two are (" + h.depth() + ")", h.canUndo());

        Undo.State back = h.undo();
        ok("undo answers the chart before the one showing (" + back + ")",
            state("a").equals(back));
        ok("and the one stepped off can be redone", h.canRedo());
        ok("undoing again has nowhere to go", !h.canUndo());
        ok("and answers null", h.undo() == null);

        Undo.State forward = h.redo();
        ok("redo answers what was stepped off (" + forward + ")", state("b").equals(forward));
        ok("and there is nothing further forward", !h.canRedo());

        // The depth limit, which exists so a form history cannot grow without end.
        Undo.History deep = new Undo.History();
        for (int i = 0; i < Undo.DEPTH + 15; i++) {
            deep.push(state("s" + i));
        }
        ok("the history stops at its limit (" + deep.depth() + ")", deep.depth() == Undo.DEPTH);
        ok("keeping the most recent",
            state("s" + (Undo.DEPTH + 13)).equals(deep.undo()));

        // <b>An identical state is not pushed.</b> Generate pressed twice without touching the
        // form would otherwise need as many undos as presses to appear to do anything.
        Undo.History same = new Undo.History();
        same.push(state("x"));
        same.push(state("x"));
        same.push(state("x"));
        ok("pressing Generate on an unchanged form records once (" + same.depth() + ")",
            same.depth() == 1);
        ok("so there is nothing to undo to", !same.canUndo());

        Undo.History nulls = new Undo.History();
        nulls.push(null);
        ok("a null state is not recorded", nulls.depth() == 0);
        ok("and undoing an empty history does not throw", nulls.undo() == null);
        ok("nor redoing one", nulls.redo() == null);
    }

    // ---------------------------------------------------------------- Part B

    /** Once you have gone a different way, the way you did not go is gone. */
    private static void branches() {
        Undo.History h = new Undo.History();
        h.push(state("a"));
        h.push(state("b"));
        h.undo();
        ok("there is something to redo", h.canRedo());
        h.push(state("d"));
        ok("a new edit clears it", !h.canRedo());
        ok("and the new edit is the present (" + h.depth() + ")", h.depth() == 2);
        ok("so going back reaches what came before it",
            state("a").equals(h.undo()));
    }

    // ---------------------------------------------------------------- Part C

    /**
     * The form's own undo, over a real panel, twice in a row.
     *
     * <b>Twice is the whole point.</b> One undo works with or without the guard; it is the second
     * that tells them apart, because without it the history has been refilled by the redraw the
     * first one caused.
     */
    private static void replay() throws Exception {
        final ChartSetupPanel[] panel = new ChartSetupPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> panel[0] = new ChartSetupPanel(null));
        ChartSetupPanel p = panel[0];

        // Three charts, the way a reader makes them: type, generate, type, generate.
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            p.formRestore(six("1980-01-01"));
            p.generateChart();
            p.formRestore(six("1990-02-02"));
            p.generateChart();
            p.formRestore(six("2000-03-03"));
            p.generateChart();
        });
        ok("three generates recorded three states (" + p.history().depth() + ")",
            p.history().depth() == 3);

        final boolean[] undone = new boolean[2];
        javax.swing.SwingUtilities.invokeAndWait(() -> undone[0] = p.undo());
        ok("the first undo works", undone[0]);
        String afterFirst = p.formSnapshot()[0];
        ok("and puts the previous date back (" + afterFirst + ")",
            "1990-02-02".equals(afterFirst));

        javax.swing.SwingUtilities.invokeAndWait(() -> undone[1] = p.undo());
        ok("THE SECOND UNDO ALSO WORKS - the guard", undone[1]);
        String afterSecond = p.formSnapshot()[0];
        ok("and goes back another step (" + afterSecond + ")",
            "1980-01-01".equals(afterSecond));

        // <b>Exact depths, because this is now the mechanism and not a symptom.</b> A flag was
        // written to stop a replay recording itself, and was then measured as unreachable - the
        // identical-state rule in Undo.History.push had already stopped it. So what holds this
        // together is that the round trip is exact, and these are the numbers that say it is:
        // two undos from three states leave one, with two to go forward to.
        ok("the history shrank by exactly two (" + p.history().depth() + ")",
            p.history().depth() == 1);
        ok("and the two steps are both available again (" + p.history().redoDepth() + ")",
            p.history().redoDepth() == 2);

        final boolean[] redone = new boolean[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> redone[0] = p.redo());
        ok("redo works", redone[0]);
        ok("and goes forward (" + p.formSnapshot()[0] + ")",
            "1990-02-02".equals(p.formSnapshot()[0]));

        // The state carries more than the six fields, because the same six generate two
        // different charts under two modes.
        // <b>The mode goes back too, which it did not until M1 was chased.</b> A state carries
        // the mode because the same six values generate two different charts under two of them;
        // restoring everything but the mode left a synastry showing the previous person's dates,
        // and broke the exact round trip the paragraph above depends on.
        final ChartMode[] seen = new ChartMode[2];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            p.history().clear();
            p.setModeForCheck(ChartMode.SINGLE);
            p.formRestore(six("1971-11-11"));
            p.generateChart();
            p.setModeForCheck(ChartMode.SYNASTRY);
            p.formRestore(six("1972-12-12"));
            p.generateChart();
            seen[0] = p.modeForCheck();
            p.undo();
            seen[1] = p.modeForCheck();
        });
        ok("the second chart was a synastry", seen[0] == ChartMode.SYNASTRY);
        ok("and undoing it goes back to the mode before it (" + seen[1] + ")",
            seen[1] == ChartMode.SINGLE);

        Undo.State s = p.undoState();
        ok("a state carries the mode", s.fields.containsKey("mode"));
        ok("and whether transits were on", s.fields.containsKey("transits"));
        ok("and the six form values", s.fields.containsKey("field0")
            && s.fields.containsKey("field5"));
    }

    // ---------------------------------------------------------------- Part D

    /**
     * Every field the form makes has its own undo manager and its own Ctrl+Z.
     *
     * <b>On the field, which is what keeps the two undos from colliding.</b> A focused component
     * is offered its keys before a WHEN_IN_FOCUSED_WINDOW binding, so typing is undone where a
     * reader is typing and charts are undone everywhere else.
     */
    private static void typing() throws Exception {
        final ChartSetupPanel[] panel = new ChartSetupPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> panel[0] = new ChartSetupPanel(null));

        List<javax.swing.text.JTextComponent> fields = new ArrayList<>();
        collect(panel[0], fields);
        ok("the form has text fields (" + fields.size() + ")", fields.size() >= 6);

        List<String> without = new ArrayList<>();
        for (javax.swing.text.JTextComponent f : fields) {
            if (Undo.managerOf(f) == null) {
                without.add(String.valueOf(f.getName()));
            }
        }
        ok("every one of them has an undo manager: " + (without.isEmpty() ? "none missing"
            : without.toString()), without.isEmpty());

        // And it actually takes typing back.
        javax.swing.text.JTextComponent field = fields.get(0);
        final javax.swing.undo.UndoManager m = Undo.managerOf(field);
        javax.swing.SwingUtilities.invokeAndWait(() -> field.setText("first"));
        javax.swing.SwingUtilities.invokeAndWait(() -> field.setText("second"));
        ok("the field holds what was typed last (" + field.getText() + ")",
            "second".equals(field.getText()));
        ok("and the manager has something to undo", m.canUndo());
        javax.swing.SwingUtilities.invokeAndWait(() -> m.undo());
        ok("undoing changes it back (" + field.getText() + ")",
            !"second".equals(field.getText()));

        // The binding is on the field, not on the window.
        ok("Ctrl+Z is bound on the field itself",
            field.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).get(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK)) != null);
        ok("and so is Ctrl+Y",
            field.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).get(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK)) != null);

        // Never throws, because it is called while a screen is being built.
        Undo.install(null);
        ok("installing on nothing is harmless", true);
    }

    // ---------------------------------------------------------------- plumbing

    private static void collect(java.awt.Container c,
                                List<javax.swing.text.JTextComponent> out) {
        for (java.awt.Component kid : c.getComponents()) {
            if (kid instanceof javax.swing.text.JTextComponent) {
                out.add((javax.swing.text.JTextComponent) kid);
            } else if (kid instanceof java.awt.Container) {
                collect((java.awt.Container) kid, out);
            }
        }
    }

    private static String[] six(String date) {
        return new String[] {date, "12:00", "London, England", date, "12:00", "London, England"};
    }

    private static Undo.State state(String tag) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("field0", tag);
        return new Undo.State(m);
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

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
