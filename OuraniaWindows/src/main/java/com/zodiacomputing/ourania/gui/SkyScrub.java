package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.ZonedDateTime;

/**
 * Scrubbing: walking the sky through time by hand, to watch transits cross a chart.
 *
 * <h3>What a scrub moves</h3>
 *
 * <p><b>The sky, and only the sky.</b> David, 30 Sep: "time scrubbing is really only needed for
 * the transit chart not a b or another charts". Birth times are changed from Chart Setup, and
 * rectification has its own screen (G8). The transport buttons are not this - Play is
 * animation and keeps its own rule in {@code SkymapPanel.stepTime}.
 *
 * <p><b>A scrub is an offset from where it began, not a run of steps.</b> Every change puts the
 * sky back where the scrub started and moves it once by the whole offset, so dragging out and
 * back lands exactly where it began - a run of one-month steps from the 31st would not
 * (31 January, 28 February, 28 March).
 *
 * <p>Master list E8. Two ways in: Shift+drag on the wheel or the globe, which the panel's mouse
 * handler turns into {@link #to}, and the bar under the transport buttons, which is
 * {@link #slider}.
 *
 * <h3>Why this is its own class (J13, step 4)</h3>
 *
 * <p>It was a stretch of {@code SkymapPanel} that reached into the panel for the sky's time, the
 * Step setting, the Play flag and the recompute. It is handed exactly those, as a {@link Host},
 * and a {@link WheelRing} - the sky - rather than the panel. Its state is its own: whether a
 * scrub is running, where it began, how far it has gone.
 */
final class SkyScrub {

    /** What a scrub needs from the chart it is scrubbing. */
    interface Host {
        /** The ring whose moment a scrub moves. */
        WheelRing sky();

        /** The Step setting, as the transport shows it: "1 Hour", "1 Day", or "Real Time". */
        String stepSetting();

        /** Stops Play, so a running animation does not fight the hand. */
        void pause();

        /** Recomputes the chart for the ring's moment and redraws it. */
        void recompute();

        /** Redraws without recomputing, for the offset tag. */
        void repaint();
    }

    /** Screen pixels of drag per step. */
    static final int PX_PER_STEP = 10;

    /** How many steps the slider reaches each way from where the scrub began. */
    static final int SLIDER_REACH = 60;

    /** How often a held bar redraws while it runs on, in milliseconds; the distance is by the clock. */
    static final int SHUTTLE_TICK_MS = 50;
    /** Steps a second with the knob pulled all the way. */
    static final double SHUTTLE_MAX_RATE = 20.0;
    /** Knob positions either side of the middle that only offset, and do not run on. */
    static final int SHUTTLE_DEAD_ZONE = 10;

    private final Host host;
    private boolean scrubbing;
    private ZonedDateTime from;
    /** Steps from where the scrub began. */
    private int steps;
    private boolean refreshQueued;

    SkyScrub(Host host) {
        this.host = host;
    }

    boolean isScrubbing() {
        return this.scrubbing;
    }

    /** Steps from where the running scrub began; zero when none is running. */
    int steps() {
        return this.steps;
    }

    /** The unit a scrub moves in: the Step setting, or an hour when Step follows the clock. */
    String unit() {
        return "Real Time".equals(this.host.stepSetting()) ? "1 Hour" : this.host.stepSetting();
    }

    void begin() {
        if (this.scrubbing) {
            return;
        }
        this.host.pause();
        this.from = this.host.sky().time;
        this.steps = 0;
        this.scrubbing = true;
    }

    /** Moves the sky to this many steps from where the scrub began. */
    void to(int steps) {
        if (!this.scrubbing) {
            this.begin();
        }
        if (steps == this.steps) {
            return;
        }
        this.host.sky().time = SkymapPanel.stepBy(this.from, this.unit(), steps);
        this.steps = steps;
        this.requestRefresh();
    }

    /** Ends the scrub where it stands, and draws that moment at once. */
    void end() {
        if (!this.scrubbing) {
            return;
        }
        this.scrubbing = false;
        this.from = null;
        this.refreshQueued = false;
        this.host.recompute();
    }

    /**
     * One recompute for however many drag events arrived before it could run.
     *
     * A drag delivers an event per pixel; recomputing the chart for each would queue work
     * faster than it is done and the wheel would trail the mouse by seconds.
     */
    private void requestRefresh() {
        if (this.refreshQueued) {
            return;
        }
        this.refreshQueued = true;
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (!this.refreshQueued) {
                return;
            }
            this.refreshQueued = false;
            this.host.recompute();
        });
    }

    /**
     * A slider that springs back: drag the knob and the chart follows it, let go and the chart
     * stays where it was put while the knob returns to the middle, ready to go again.
     *
     * <b>Why it springs back.</b> A slider with a fixed range pinned to a date would need a
     * range, and any range is wrong - a day's worth for a transit, a century for a progression.
     * Springing back makes the range relative: sixty steps of the Step setting each way, from
     * wherever the chart is, as many times as the reader likes. An arrow key on the focused
     * slider is a single step.
     *
     * @param afterRelease run once the knob is let go and the scrub has ended
     */
    javax.swing.JSlider slider(Runnable afterRelease) {
        javax.swing.JSlider slider = new javax.swing.JSlider(-SLIDER_REACH, SLIDER_REACH, 0);
        slider.setOpaque(false);
        slider.setPreferredSize(new java.awt.Dimension(220, 26));
        slider.setToolTipText("Drag to move the sky through time by the Step setting. Hold it "
            + "pulled and time keeps going that way, faster the further you pull; it stays where "
            + "you let go.");
        final boolean[] resetting = {false};
        // Steps run up while the knob is held off the dead zone, on top of where the knob sits.
        final double[] travel = {0.0};
        // <b>By the clock, not by the tick.</b> Each tick recomputes the chart on the event thread,
        // and Swing coalesces ticks that pile up behind it: counted per tick, a bar held at 48 for
        // two seconds ran 6 steps where its rate said 23.
        final long[] lastTick = {0L};
        final javax.swing.Timer shuttle = new javax.swing.Timer(SHUTTLE_TICK_MS, null);
        shuttle.addActionListener(ev -> {
            if (!slider.getValueIsAdjusting() || !this.isScrubbing()) {
                shuttle.stop();
                return;
            }
            long now = System.nanoTime();
            travel[0] += shuttleRate(slider.getValue()) * (now - lastTick[0]) / 1.0e9;
            lastTick[0] = now;
            this.to(slider.getValue() + (int) travel[0]);
            this.host.repaint();
        });
        slider.addChangeListener(e -> {
            if (resetting[0]) {
                return;
            }
            this.begin();
            this.to(slider.getValue() + (int) travel[0]);
            this.host.repaint();
            if (slider.getValueIsAdjusting()) {
                if (!shuttle.isRunning()) {
                    lastTick[0] = System.nanoTime();
                    shuttle.start();
                }
                return;
            }
            shuttle.stop();
            travel[0] = 0.0;
            this.end();
            afterRelease.run();
            resetting[0] = true;
            try {
                slider.setValue(0);
            } finally {
                resetting[0] = false;
            }
        });
        return slider;
    }

    /**
     * Steps a second a bar runs at with its knob held here.
     *
     * David: "when you pull them forward or backward can you have the time keep going until
     * released". The knob still offsets the chart by where it sits, so a short flick is still a
     * few steps; held past the dead zone it also runs on, like a shuttle, and the rate climbs with
     * the square of the pull - a gentle pull creeps, a full one covers twenty steps a second.
     */
    static double shuttleRate(int value) {
        int pull = Math.abs(value) - SHUTTLE_DEAD_ZONE;
        if (pull <= 0) {
            return 0.0;
        }
        double share = (double) pull / (SLIDER_REACH - SHUTTLE_DEAD_ZONE);
        return Math.signum(value) * SHUTTLE_MAX_RATE * share * share;
    }

    /** "+3 days", "-1 month": the offset a scrub stands at, in words. */
    static String label(int steps, String unit) {
        String word = unit.replaceFirst("^1 ", "").toLowerCase();
        String sign = steps > 0 ? "+" : steps < 0 ? "−" : "±";
        int n = Math.abs(steps);
        return sign + n + " " + word + (n == 1 ? "" : "s");
    }

    /** The offset, over the top of the wheel while a scrub is running. */
    void paintTag(Graphics2D g2, int w) {
        if (!this.scrubbing) {
            return;
        }
        String text = "Scrubbing the sky " + label(this.steps, this.unit());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(Theme.font("Segoe UI", Font.BOLD, 13));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text) + 24;
        int x = (w - tw) / 2;
        g2.setColor(new Color(22, 28, 43, 230));
        g2.fillRoundRect(x, 10, tw, 26, 12, 12);
        g2.setColor(new Color(226, 178, 88));
        g2.drawRoundRect(x, 10, tw, 26, 12, 12);
        g2.drawString(text, x + 12, 10 + (26 + fm.getAscent() - fm.getDescent()) / 2);
    }
}
