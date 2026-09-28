package com.zodiacomputing.ourania.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Taking back a change to the chart form, and taking back typing (G13).
 *
 * <p><b>Swept for on 28 September: no {@code UndoManager}, no {@code undo()}, nothing.</b> The row
 * called this "a model change and the larger half of this row", and the model change is the part
 * below - the form had no notion of a previous state, only of its current one.
 *
 * <p><b>Two undos, and they do not collide.</b> Typing in a field is undone by that field; a chart
 * you have already generated is undone by the window. They share a keystroke and it works, for a
 * reason this project measured when the shortcut table was built: <i>a focused component consumes
 * its keys before a {@code WHEN_IN_FOCUSED_WINDOW} binding is offered them</i>. So Ctrl+Z inside a
 * date field steps back through what you typed, and Ctrl+Z anywhere else steps back through the
 * charts you generated. That is the behaviour a reader expects from every other application, and
 * it falls out of the binding order rather than being arranged.
 *
 * <p><b>A snapshot is the form's own words, not a chart.</b> Storing computed charts would make
 * undo depend on the ephemeris and would restore positions rather than the text somebody typed -
 * so a birth time that was wrong by an hour would come back as a chart and not as a field they
 * could correct. The panel that owns the fields is the one that reads and writes them.
 *
 * <p><b>Bounded, and the bound is small on purpose.</b> Thirty states of a dozen short strings is
 * nothing to hold, and an unbounded history of a form is a slow leak nobody ever notices.
 */
final class Undo {

    private Undo() { }

    /** How many form states are kept. Beyond this the oldest is dropped. */
    static final int DEPTH = 30;

    /**
     * One state of the chart form: every field by name, as text.
     *
     * <b>Text, including the booleans.</b> A snapshot that carried typed values would need to know
     * what each field means, which is the panel's business; as strings it is a record of what was
     * on screen and nothing else has to agree with it.
     */
    static final class State {
        final Map<String, String> fields;

        State(Map<String, String> fields) {
            this.fields = new LinkedHashMap<>(fields);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof State && fields.equals(((State) o).fields);
        }

        @Override
        public int hashCode() {
            return fields.hashCode();
        }

        @Override
        public String toString() {
            return fields.toString();
        }
    }

    /**
     * The form's history: what was there before, and what was taken back.
     *
     * <b>Two stacks, which is what makes redo honest.</b> A single cursor into one list is the
     * other common shape and it leaves "redo" meaning something after a new edit; here a new push
     * clears the redo stack, because once you have gone a different way the way you did not go is
     * no longer reachable.
     */
    static final class History {
        private final Deque<State> past = new ArrayDeque<>();
        private final Deque<State> future = new ArrayDeque<>();

        /**
         * Records a state as the one to come back to.
         *
         * <b>A state identical to the last is not pushed</b>, and that one rule does two jobs.
         * Pressing Generate twice without touching the form would otherwise fill the history with
         * one repeated state, and undo would appear to do nothing for as many presses as had been
         * made. And undo redraws by generating, so the generate an undo causes would record the
         * state the undo had just restored - the history would grow by one every time it was
         * walked back and never get anywhere.
         *
         * <b>A flag was written for that second job and then measured as unreachable.</b>
         * {@code ChartSetupPanel} carried a {@code replaying} field, and removing it changed
         * nothing that any assertion could see, because this comparison had already stopped the
         * push. A guard no failure can reach is not a guard, so it is gone and this is the
         * mechanism - which means the round trip has to be exact, and UndoCheck Part C asserts
         * the depths rather than trusting that it is.
         *
         * The early return is deliberately before {@link #future} is cleared: a redo restores a
         * state, generates, and arrives here, and clearing the future there would throw away the
         * rest of the redo stack on the first step forward.
         */
        void push(State s) {
            if (s == null) {
                return;
            }
            if (!past.isEmpty() && past.peek().equals(s)) {
                return;
            }
            past.push(s);
            while (past.size() > DEPTH) {
                past.removeLast();
            }
            future.clear();
        }

        /**
         * True when there is somewhere to go back TO, which needs two states and not one.
         *
         * <b>The top of the stack is the present, not the past</b> - a state is pushed when a
         * chart is generated, so the most recent push is the chart on screen. The first version
         * treated the top as somewhere to go and undo returned the state it was already showing:
         * the fields were rewritten with their own values and nothing moved. Measured, not
         * reasoned about - the check asked for 1990 and got 2000.
         */
        boolean canUndo() {
            return past.size() >= 2;
        }

        boolean canRedo() {
            return !future.isEmpty();
        }

        /** The chart generated before the one showing, or null when there is not one. */
        State undo() {
            if (!canUndo()) {
                return null;
            }
            future.push(past.pop());
            return past.peek();
        }

        /** The chart undo stepped off, or null. */
        State redo() {
            if (future.isEmpty()) {
                return null;
            }
            State next = future.pop();
            past.push(next);
            return next;
        }

        int depth() {
            return past.size();
        }

        int redoDepth() {
            return future.size();
        }

        void clear() {
            past.clear();
            future.clear();
        }
    }

    // ------------------------------------------------------------------ typing

    /**
     * Gives one text field its own undo, bound on the field itself.
     *
     * <b>On the field, deliberately, and that is what keeps the two undos apart.</b> A binding on
     * the component wins over the window's while the component has focus, so this is offered the
     * keystroke first exactly when a reader is typing - which is exactly when they mean it.
     *
     * Never throws: it is called while a screen is being built.
     */
    static void install(javax.swing.text.JTextComponent field) {
        if (field == null) {
            return;
        }
        try {
            final javax.swing.undo.UndoManager manager = new javax.swing.undo.UndoManager();
            manager.setLimit(DEPTH * 10);
            field.getDocument().addUndoableEditListener(manager);
            field.putClientProperty(MANAGER, manager);

            field.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK), "ourania.undo.text");
            field.getActionMap().put("ourania.undo.text", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (manager.canUndo()) {
                        manager.undo();
                    }
                }
            });
            field.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK), "ourania.redo.text");
            field.getActionMap().put("ourania.redo.text", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (manager.canRedo()) {
                        manager.redo();
                    }
                }
            });
        } catch (Throwable ignored) {
            // A field without undo is the behaviour there was before this class; a screen that
            // will not build because of it is worse than that.
        }
    }

    /**
     * Gives every text field under a container its own undo, once each.
     *
     * <b>A sweep rather than a call beside each field, because three of them are made
     * elsewhere.</b> The first version installed inside {@code createField}, which makes eight of
     * the eleven; the sky date, time and location are built directly and had none - found by the
     * check listing the fields that answered null. A sweep over the finished screen is the same
     * shape the settings search and the accessibility naming use, and for the same reason: it
     * cannot miss one somebody adds later.
     */
    static int installAll(java.awt.Container root) {
        int installed = 0;
        if (root == null) {
            return 0;
        }
        for (java.awt.Component kid : root.getComponents()) {
            if (kid instanceof javax.swing.text.JTextComponent) {
                javax.swing.text.JTextComponent f = (javax.swing.text.JTextComponent) kid;
                if (managerOf(f) == null) {
                    install(f);
                    installed++;
                }
            } else if (kid instanceof java.awt.Container) {
                installed += installAll((java.awt.Container) kid);
            }
        }
        return installed;
    }

    /** Where a field's manager is kept, so a check can find it without a second registry. */
    static final String MANAGER = "ourania.undo.manager";

    /** A field's undo manager, or null if it was never given one. */
    static javax.swing.undo.UndoManager managerOf(javax.swing.text.JTextComponent field) {
        Object o = field == null ? null : field.getClientProperty(MANAGER);
        return o instanceof javax.swing.undo.UndoManager ? (javax.swing.undo.UndoManager) o : null;
    }
}
