package com.zodiacomputing.ourania.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

/**
 * Every keyboard shortcut in the app, declared once (G13).
 *
 * <p><b>A label that claims a key the binding does not have is this project's signature defect in its
 * smallest form.</b> Before this, F11 and Escape were bound inside {@code WindowPlacement.installKeys}
 * and known nowhere else, and the transport buttons had no keys at all. Anyone adding a shortcut had
 * two places to touch and nothing to stop the two disagreeing - which is how {@code ringWord} came to
 * be written six times and four lists of house systems drifted apart.
 *
 * <p>So the table is the declaration and the installer reads it. A row carries its own key, its own
 * label and its own description, and {@link #hint} is what a tooltip appends - so a button cannot
 * advertise a key that is not bound, because it does not know the key except through this table.
 *
 * <p><b>Every new key is modified.</b> Bare letters and bare arrows were both available and both
 * rejected after looking at what already listens: the scrub bars and the place list bind arrow keys on
 * their own components, and a bare letter is a character a reader is typing into a field. A focused
 * component consumes its keys before a {@code WHEN_IN_FOCUSED_WINDOW} binding is offered them, so a
 * bare binding would work until it silently did not - the worst kind. Control-modified strokes collide
 * with neither.
 */
final class Shortcuts {

    private Shortcuts() { }

    /** The one table. A row is a key, what it is called, and what it does. */
    enum Key {
        FULL_SCREEN("Full screen", KeyEvent.VK_F11, 0,
            "Fills the screen, and back again") {
            @Override
            void fire(Actions a) {
                a.fullScreen();
            }
        },
        LEAVE_FULL_SCREEN("Leave full screen", KeyEvent.VK_ESCAPE, 0,
            "Leaves full screen, when it is on") {
            @Override
            void fire(Actions a) {
                a.leaveFullScreen();
            }
        },
        PLAY_PAUSE("Play / Pause", KeyEvent.VK_SPACE, java.awt.event.InputEvent.CTRL_DOWN_MASK,
            "Starts and stops the chart moving through time") {
            @Override
            void fire(Actions a) {
                a.playPause();
            }
        },
        STEP_BACK("Step back", KeyEvent.VK_LEFT, java.awt.event.InputEvent.CTRL_DOWN_MASK,
            "Runs the chart backwards at the slow rate") {
            @Override
            void fire(Actions a) {
                a.run(-1);
            }
        },
        STEP_FORWARD("Step forward", KeyEvent.VK_RIGHT, java.awt.event.InputEvent.CTRL_DOWN_MASK,
            "Runs the chart forwards at the slow rate") {
            @Override
            void fire(Actions a) {
                a.run(1);
            }
        },
        NOW("Now", KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK,
            "Stops, and moves the chart to this moment") {
            @Override
            void fire(Actions a) {
                a.now();
            }
        },
        UNDO("Undo", KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK,
            "Takes back the last chart you generated. While you are typing in a field it "
            + "takes back what you typed instead") {
            @Override
            void fire(Actions a) {
                a.undo();
            }
        },
        REDO("Redo", KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_DOWN_MASK,
            "Puts back what Undo took away") {
            @Override
            void fire(Actions a) {
                a.redo();
            }
        };

        /** What a reader calls it - the same words as the button it mirrors, where there is one. */
        final String label;
        final int code;
        final int modifiers;
        /** One sentence, for a help page and for a tooltip. */
        final String about;

        /**
         * What this key does, on the row itself (G13).
         *
         * <b>It was a switch with a default, and that is where a key added later goes to die.</b>
         * The identical shape cost this project a whole morning on 27 September, when a seventh
         * ring kind would have been silently called "Chart" by a switch exactly like it - and the
         * fix was the same one: put the behaviour on the enum, so a row cannot be added without
         * saying what it is for. Two rows were added here in the same commit that removed it.
         */
        abstract void fire(Actions a);

        Key(String label, int code, int modifiers, String about) {
            this.label = label;
            this.code = code;
            this.modifiers = modifiers;
            this.about = about;
        }

        KeyStroke stroke() {
            return KeyStroke.getKeyStroke(this.code, this.modifiers);
        }

        /** The action name this row is bound under; distinct per row by construction. */
        String actionKey() {
            return "ourania.shortcut." + name();
        }

        /** How the key is written for a reader: "Ctrl+Space", "F11". */
        String written() {
            String mods = java.awt.event.InputEvent.getModifiersExText(this.modifiers);
            String key = KeyEvent.getKeyText(this.code);
            return mods.isEmpty() ? key : mods + "+" + key;
        }
    }

    /** What a tooltip appends, so a button cannot name a key it does not have. */
    static String hint(Key k) {
        return " (" + k.written() + ")";
    }

    /**
     * The window's own rows, with the transport ones doing nothing.
     *
     * <b>So that there is still one installer.</b> {@code WindowPlacement.installKeys} predates this
     * table and {@code WindowPlacementCheck} calls it directly to assert F11 on a bare frame that has
     * no chart in it. Rather than leave a second place that binds keys, that method delegates here
     * with this adapter - the full-screen rows behave exactly as they did, and a frame with no
     * transport simply has nothing for those rows to do.
     */
    static Actions windowOnly(final JFrame frame) {
        return new Actions() {
            @Override
            public void fullScreen() {
                WindowPlacement.toggleFullScreen(frame);
            }

            @Override
            public void leaveFullScreen() {
                if (WindowPlacement.isFullScreen(frame)) {
                    WindowPlacement.toggleFullScreen(frame);
                }
            }

            @Override
            public void playPause() {
                // No chart on a bare frame.
            }

            @Override
            public void run(int direction) {
                // As above.
            }

            @Override
            public void now() {
                // As above.
            }

            @Override
            public void undo() {
                // No chart form on a bare frame.
            }

            @Override
            public void redo() {
                // As above.
            }
        };
    }

    /** What each row does, supplied by whoever owns the behaviour. */
    interface Actions {
        void fullScreen();

        void leaveFullScreen();

        void playPause();

        void run(int direction);

        void now();

        /** Take back the last generated chart. G13. */
        void undo();

        /** Put back what undo took away. G13. */
        void redo();
    }

    /**
     * Binds every row on the window's root pane.
     *
     * <b>On the root pane with {@code WHEN_IN_FOCUSED_WINDOW}</b>, the way F11 already was, so any
     * focus hears them - and so that a focused text field still gets its own keys first.
     */
    static void install(JFrame frame, Actions actions) {
        javax.swing.InputMap in =
            frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap act = frame.getRootPane().getActionMap();
        for (final Key k : Key.values()) {
            in.put(k.stroke(), k.actionKey());
            act.put(k.actionKey(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    k.fire(actions);
                }
            });
        }
    }


}
