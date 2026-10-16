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
        interrupt();
        remove();
        this.scene = s;
        this.onLand = onLand;
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
        if (bloom.opening()) {
            foldFrom = Math.max(1e-6, bloom.value());
            bloom.set(false);
        }
    }

    private void frame() {
        if (bloom == null) {
            return;
        }
        if (bloom.opening() && bloom.value() >= 1.0) {
            land();
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
