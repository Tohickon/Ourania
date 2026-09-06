package com.zodiacomputing.ourania.gui;

import javax.swing.Timer;

/**
 * A ring opening or folding away: one interruptible number from 0 to 1.
 *
 * <b>From David's prototype, ported.</b> The idea there is that a chart is not a mode you pick
 * but a stack of rings you open - natal alone, then a partner blooming around it, then the sky
 * around both - and that each ring should unfurl rather than appear. This is the primitive
 * that makes that possible: everything the wheel draws for a ring is scaled by its bloom, so
 * one number decides whether the ring is absent, arriving, or fully there.
 *
 * <b>Interruptible, and that is the whole difficulty.</b> A reader who opens a ring and
 * immediately closes it must not wait for the opening to finish first, and must not see the
 * ring jump. So a reversal starts from wherever the value currently is rather than from the
 * end it was heading to - which is why the current value is kept rather than recomputed from
 * a start time and a direction.
 *
 * <b>Sine in-out, not linear.</b> A linear bloom reads as a mechanism sliding; the eased one
 * reads as something opening. The midpoint matters here more than usual, because a
 * half-bloomed ring is a state the reader will actually see and should look deliberate.
 */
public final class Bloom {

    /** How long a ring takes to open or fold, in milliseconds. */
    public static final int DEFAULT_MS = 900;

    private static final int FRAME_MS = 16;

    private final Timer timer;
    private final int durationMs;
    private final Runnable onFrame;

    private double value;
    private double from;
    private double target;
    private long startedAt;

    /**
     * @param onFrame called on every step, for the caller to repaint. Never null in practice -
     *     a bloom nothing repaints is a number that changes invisibly.
     */
    public Bloom(boolean openAtStart, Runnable onFrame) {
        this(openAtStart, DEFAULT_MS, onFrame);
    }

    public Bloom(boolean openAtStart, int durationMs, Runnable onFrame) {
        this.durationMs = Math.max(1, durationMs);
        this.onFrame = onFrame;
        this.value = openAtStart ? 1.0 : 0.0;
        this.from = this.value;
        this.target = this.value;
        this.timer = new Timer(FRAME_MS, e -> step());
        this.timer.setCoalesce(true);
    }

    /** Where this ring is between folded (0) and open (1). */
    public double value() {
        return value;
    }

    /** True once the ring is drawing at all - anything above nothing. */
    public boolean showing() {
        return value > 0.001;
    }

    /** True when the ring is on its way to being open, whatever it is doing now. */
    public boolean opening() {
        return target > 0.5;
    }

    /** Opens or folds the ring, from wherever it currently is. */
    public void set(boolean open) {
        double to = open ? 1.0 : 0.0;
        if (Math.abs(target - to) < 1e-9 && !timer.isRunning()) {
            return;
        }
        this.target = to;
        this.from = this.value;
        if (Math.abs(from - to) < 0.002 || reducedMotion()) {
            finish();
            return;
        }
        this.startedAt = System.currentTimeMillis();
        timer.restart();
    }

    /** Opens or folds with no animation, for the state the app starts in. */
    public void setImmediately(boolean open) {
        timer.stop();
        this.value = open ? 1.0 : 0.0;
        this.from = this.value;
        this.target = this.value;
    }

    public void toggle() {
        set(!opening());
    }

    private void step() {
        double p = Math.min(1.0, (System.currentTimeMillis() - startedAt)
            / (double) durationMs);
        value = from + (target - from) * ease(p);
        if (p >= 1.0) {
            finish();
            return;
        }
        if (onFrame != null) {
            onFrame.run();
        }
    }

    private void finish() {
        timer.stop();
        value = target;
        from = target;
        if (onFrame != null) {
            onFrame.run();
        }
    }

    /** Sine in-out: still at both ends, quickest through the middle. */
    private static double ease(double p) {
        return 0.5 - 0.5 * Math.cos(Math.PI * Math.max(0.0, Math.min(1.0, p)));
    }

    /**
     * Whether to skip the animation entirely.
     *
     * <b>A setting rather than an OS query, because Swing does not expose one.</b> The web
     * prototype could ask {@code prefers-reduced-motion}; there is no equivalent here, so the
     * reader is asked once and it is honoured everywhere a bloom runs. Motion that cannot be
     * turned off is the accessibility failure this guards against.
     */
    static boolean reducedMotion() {
        return !Settings.animateRings();
    }

    /**
     * One item's share of a bloom, so a ring unfurls instead of appearing at once.
     *
     * Item {@code index} of {@code count} starts later than the one before it and finishes
     * with the rest. {@code spread} is how much of the bloom is given over to that offset:
     * at 0 every item moves together, at 0.5 the last one starts halfway through.
     */
    public static double stagger(double t, int index, int count, double spread) {
        double offset = (index / (double) Math.max(1, count)) * spread;
        double span = 1.0 - spread;
        double local = span <= 0 ? 1.0 : (t - offset) / span;
        return Math.min(1.0, Math.max(0.0, local));
    }

    /** Smooth 0..1 ramp between two edges, for fading a thing in over part of a bloom. */
    public static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.min(1.0, Math.max(0.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }
}
