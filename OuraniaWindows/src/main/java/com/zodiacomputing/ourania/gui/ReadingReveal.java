package com.zodiacomputing.ourania.gui;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

/**
 * Plays a {@link RevealPainter} sequence over the window, and lands the real reading at its end.
 *
 * <b>On the layered pane, because the flight crosses components.</b> The wheel and the reading
 * are different panels; a card painted inside either is clipped at its edge. The overlay is a
 * listener-free component above both, so every click still reaches what is under it.
 *
 * <b>The card that flies is a picture; the card that stays is the real one.</b> When the
 * flight ends, {@code onLand} shows the reading's own HTML and the overlay goes. If anything
 * interrupts - a click, a key, a new request, the chart changing - the reading lands at once
 * and the overlay folds back from wherever it was, through the same {@link Bloom} it played
 * on. No path leaves the reader without the reading they asked for.
 *
 * <b>Only for a reading the reader asked for</b> - see {@link #shouldPlay}. The transport
 * rebuilds the reading several times a second, and a reveal on each of those would be noise.
 */
final class ReadingReveal {

    /** How long an interrupted reveal takes to fold all the way back. */
    static final int FOLD_MS = 300;

    private final JLayeredPane layer;
    private final Overlay overlay = new Overlay();
    private Bloom bloom;
    private RevealPainter.Scene scene;
    private Runnable onLand;
    /** Where the real card is once the page has landed, or null while it cannot be seen. */
    private java.util.function.Supplier<java.awt.Rectangle> landing;
    /** The value at which the reveal was interrupted, which the fold fades against. */
    private double foldFrom = 1.0;
    private AWTEventListener interruptor;

    ReadingReveal(JLayeredPane layer) {
        this.layer = layer;
    }

    /** Whether motion is allowed at all: this setting, and the app's reduced-motion one. */
    static boolean allowed() {
        return Settings.animateReadings() && Settings.animateRings();
    }

    /**
     * Whether a reading should be revealed rather than simply shown.
     *
     * @param asked true only for the reader's own request - a button, the sidebar - and never
     *              for the rebuild that follows a chart change
     */
    static boolean shouldPlay(boolean asked, RevealPlan plan) {
        return asked && plan != null && allowed();
    }

    /** True from the start of a reveal until its overlay is gone. */
    boolean playing() {
        return overlay.getParent() != null;
    }

    /** What is being drawn now; its target moves to the real card once the page lands. */
    RevealPainter.Scene scene() {
        return scene;
    }

    /** How far through the sequence the reveal is, 0 to 1. */
    double progress() {
        return bloom == null ? 0.0 : bloom.value();
    }

    /**
     * Starts a reveal. Anything still playing lands first.
     *
     * @param onLand shows the real reading; run exactly once, at the end or on interruption
     */
    void play(RevealPainter.Scene s, Runnable onLand) {
        this.play(s, onLand, null);
    }

    /**
     * As above, landing on the real card the page shows.
     *
     * @param landing asked each frame once the reading has landed; where its card is, in the
     *                layered pane, or null to keep the scene's own target
     */
    void play(RevealPainter.Scene s, Runnable onLand,
            java.util.function.Supplier<java.awt.Rectangle> landing) {
        interrupt();
        remove();
        this.scene = s;
        this.onLand = onLand;
        this.landing = landing;
        this.foldFrom = 1.0;
        overlay.setBounds(0, 0, layer.getWidth(), layer.getHeight());
        layer.add(overlay, JLayeredPane.DRAG_LAYER);
        bloom = Bloom.timeline(RevealPainter.TOTAL_MS, FOLD_MS, this::frame);
        listen();
        bloom.set(true);
    }

    /** Lands the reading now and folds what is left of the reveal away. */
    void interrupt() {
        if (!playing() || bloom == null) {
            return;
        }
        land();
        // Past the landing, the card is only holding over a reading that is already there.
        // Folding would fly it back to the wheel, so it simply goes.
        if (bloom.opening() && bloom.value() >= RevealPainter.LANDS_AT) {
            bloom.setImmediately(false);
            remove();
            return;
        }
        if (bloom.opening()) {
            foldFrom = Math.max(1e-6, bloom.value());
            bloom.set(false);
        }
    }

    private void frame() {
        if (bloom == null) {
            return;
        }
        // The reading lands as soon as the card has formed, and the card then holds over it.
        if (bloom.opening() && bloom.value() >= RevealPainter.LANDS_AT) {
            land();
            // Measured every frame, not once: the page lays out, and scrolls to its top, a
            // turn or two after it is set.
            java.awt.Rectangle r = landing == null ? null : landing.get();
            if (r != null && scene != null && !r.equals(scene.target())) {
                scene = scene.landingOn(r);
            }
        }
        if (bloom.opening() && bloom.value() >= 1.0) {
            remove();
            return;
        }
        if (!bloom.opening() && bloom.value() <= 0.0) {
            remove();
            return;
        }
        overlay.repaint();
    }

    private void land() {
        Runnable r = onLand;
        onLand = null;
        if (r != null) {
            r.run();
        }
    }

    private void remove() {
        unlisten();
        if (overlay.getParent() != null) {
            layer.remove(overlay);
            layer.repaint(overlay.getBounds());
        }
    }

    /** Any click or key interrupts. Watched, not consumed: the event still reaches its target. */
    private void listen() {
        unlisten();
        interruptor = e -> {
            if (e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == KeyEvent.KEY_PRESSED) {
                interrupt();
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(interruptor,
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    private void unlisten() {
        if (interruptor != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(interruptor);
            interruptor = null;
        }
    }

    /**
     * The real card a reading opens on, in the layered pane's coordinates, or null.
     *
     * <b>Found on the page, not assumed.</b> A reading can land on any of several panes - the
     * tab that asked, the Selection page - and the one with the card is the one on screen whose
     * document holds {@link RevealPlan#CARD_ID} with this card's heading in it. The box is the
     * card's own table, from the pane's view tree, so the flying card lands on its edges.
     */
    static java.awt.Rectangle cardOnPage(JLayeredPane lp, String headline) {
        for (javax.swing.JEditorPane pane : panes(lp, new java.util.ArrayList<>())) {
            if (!pane.isShowing()
                    || !(pane.getDocument() instanceof javax.swing.text.html.HTMLDocument doc)) {
                continue;
            }
            javax.swing.text.Element div = doc.getElement(RevealPlan.CARD_ID);
            if (div == null) {
                continue;
            }
            try {
                int from = div.getStartOffset();
                String text = doc.getText(from, Math.max(0, div.getEndOffset() - from));
                if (headline != null && !text.contains(headline)) {
                    continue;
                }
            } catch (javax.swing.text.BadLocationException e) {
                continue;
            }
            // The cell, not the table: the table's box carries its bottom margin, and a card
            // landing on that would hang 12 px over the page below it.
            javax.swing.text.Element box = firstNamed(div, "td");
            if (box == null) {
                box = div;
            }
            java.awt.Rectangle r = boxOf(pane, box);
            if (r != null && r.width > 0 && r.height > 0 && pane.getVisibleRect().intersects(r)) {
                return javax.swing.SwingUtilities.convertRectangle(pane, r, lp);
            }
        }
        return null;
    }

    private static javax.swing.text.Element firstNamed(javax.swing.text.Element e, String name) {
        for (int i = 0; i < e.getElementCount(); i++) {
            javax.swing.text.Element k = e.getElement(i);
            if (k.getName().equalsIgnoreCase(name)) {
                return k;
            }
            javax.swing.text.Element deeper = firstNamed(k, name);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    private static java.util.List<javax.swing.JEditorPane> panes(java.awt.Container c,
            java.util.List<javax.swing.JEditorPane> out) {
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof javax.swing.JEditorPane p) {
                out.add(p);
            } else if (k instanceof java.awt.Container sub) {
                panes(sub, out);
            }
        }
        return out;
    }

    /** An element's box in its pane: the view tree walked down to it, allocation by allocation. */
    private static java.awt.Rectangle boxOf(javax.swing.JEditorPane pane,
            javax.swing.text.Element target) {
        javax.swing.text.View v = pane.getUI().getRootView(pane);
        java.awt.Insets in = pane.getInsets();
        java.awt.Shape a = new java.awt.Rectangle(in.left, in.top,
            pane.getWidth() - in.left - in.right, pane.getHeight() - in.top - in.bottom);
        int at = target.getStartOffset();
        for (int depth = 0; v != null && v.getElement() != target && depth < 64; depth++) {
            int i = v.getViewIndex(at, javax.swing.text.Position.Bias.Forward);
            if (i < 0 || i >= v.getViewCount()) {
                return null;
            }
            a = v.getChildAllocation(i, a);
            v = v.getView(i);
            if (a == null) {
                return null;
            }
        }
        return v == null || v.getElement() != target ? null : a.getBounds();
    }

    private final class Overlay extends JComponent {
        Overlay() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (scene == null || bloom == null) {
                return;
            }
            double v = bloom.value();
            double fade = bloom.opening() ? 1.0 : v / foldFrom;
            RevealPainter.paint((Graphics2D) g, scene, v, fade);
        }

        /** Never the target of a click: the reveal is something to watch, not to press. */
        @Override
        public boolean contains(int x, int y) {
            return false;
        }
    }
}
