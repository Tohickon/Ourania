package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * Where the flat wheel is being looked at from: how far in, and how far across.
 *
 * <h3>Why this is its own object (J13, step 2)</h3>
 *
 * <p>It was four fields and seven methods on {@link SkymapPanel}, which is the problem J13
 * exists for rather than an example of it - the file passed nine thousand lines because every
 * feature the wheel grew went into it. The ring fields came out first, in {@code WheelRing},
 * because everything else read them. This is the next seam, and it is a good one for the same
 * reason the rings were: the state is small, it is touched by exactly one suite, and nothing
 * outside the panel had ever reached into it.
 *
 * <h3>The panel's size arrives as an argument, and that is the whole point</h3>
 *
 * <p>Every one of these calculations needs the panel's width and height, and the versions on
 * the panel read them off the component - which is why each began by asking whether the
 * component existed yet and returning early if it did not. Those guards were not describing
 * anything about zooming; they were describing the fact that the code lived on a Swing
 * component. Taking the size as an argument removes them, and leaves a class that can be
 * asked what it would do at any size without a window ever being opened.
 *
 * <h3>What has not changed</h3>
 *
 * <p>The design this holds is untouched, and it is worth restating because it is easy to undo:
 *
 * <ul>
 *   <li><b>One view transform, applied at the two ends and nowhere in between.</b> The painter
 *       draws the wheel through it and every mouse position comes back through its inverse
 *       before a hit test sees it, so {@code Geometry}, {@code bodyAt}, {@code chordAt} and the
 *       rest still work in the wheel's own unzoomed coordinates. Threading a zoom factor into
 *       the radius chain instead is the four-copies defect {@code Geometry} was built to end,
 *       and a hit test that missed the factor would put clicks beside what is drawn.</li>
 *   <li><b>Screen only.</b> A saved PNG or a print is the whole chart whatever the screen is
 *       zoomed to - the exporter marks the panel while it paints and the transform is skipped.</li>
 *   <li><b>Zoom about the cursor, not the centre</b>, so what the reader was pointing at stays
 *       where it is. Zooming about the centre would need a pan after every notch.</li>
 *   <li><b>Fit is zoom 1 with no pan</b>, because the wheel already sizes itself to the panel.</li>
 * </ul>
 */
final class WheelView {

    /** As far in as the reader may go. */
    static final double MAX_ZOOM = 8.0;

    /** Each wheel notch zooms by this much, so four notches is about double. */
    static final double STEP = 1.19;

    private double zoom = 1.0;
    private double panX;
    private double panY;
    private boolean panned;

    double zoom() {
        return this.zoom;
    }

    double panX() {
        return this.panX;
    }

    double panY() {
        return this.panY;
    }

    /** Set while a drag has been panning, so its closing click does not also select. */
    boolean panned() {
        return this.panned;
    }

    void panned(boolean value) {
        this.panned = value;
    }

    boolean isFit() {
        return this.zoom == 1.0 && this.panX == 0.0 && this.panY == 0.0;
    }

    /** Screen from wheel: scale about the panel's centre, then shift by the pan. */
    AffineTransform transform(int w, int h) {
        AffineTransform t = new AffineTransform();
        t.translate(w / 2.0 + this.panX, h / 2.0 + this.panY);
        t.scale(this.zoom, this.zoom);
        t.translate(-w / 2.0, -h / 2.0);
        return t;
    }

    /** A screen point in the wheel's own coordinates - what every hit test is asked about. */
    Point toWheel(int x, int y, int w, int h) {
        if (this.isFit()) {
            return new Point(x, y);
        }
        double[] p = unproject(x, y, w, h);
        return new Point((int) Math.round(p[0]), (int) Math.round(p[1]));
    }

    /** Zooms by a number of wheel notches, keeping the point under the cursor where it is. */
    void zoomAt(int x, int y, double notches, int w, int h) {
        double z = Math.max(1.0, Math.min(MAX_ZOOM, this.zoom * Math.pow(STEP, -notches)));
        // The wheel point under the cursor, before the change, in the wheel's coordinates.
        double[] p = unproject(x, y, w, h);
        this.zoom = z;
        this.panX = x - w / 2.0 - z * (p[0] - w / 2.0);
        this.panY = y - h / 2.0 - z * (p[1] - h / 2.0);
        this.clamp(w, h);
    }

    /** Moves the view by a screen distance. */
    void panBy(double dx, double dy, int w, int h) {
        this.panX += dx;
        this.panY += dy;
        this.clamp(w, h);
    }

    /**
     * Re-clamps against a size, without moving the view otherwise.
     *
     * <b>The painter calls this at the size it is painting.</b> A window made smaller while
     * zoomed would otherwise keep a pan measured against the old width, and that pan now runs
     * past the wheel's edge - so the reader sees the background where the chart should be. It
     * is the one place the view has to be corrected by something other than a gesture, which is
     * why it is a method and not a private detail of the two that move things.
     */
    void reclamp(int w, int h) {
        this.clamp(w, h);
    }

    /** Back to the whole wheel, fitted to the panel. */
    void fit() {
        this.zoom = 1.0;
        this.panX = 0.0;
        this.panY = 0.0;
    }

    /**
     * The wheel coordinates under a screen point, unclamped and unrounded.
     *
     * <b>One statement of the inverse.</b> It was written out twice - once in {@code toWheel}
     * and once inside {@code zoomAt}, which needs the point under the cursor before it moves
     * anything. Two copies of a transform's inverse is the shape of defect this panel has been
     * bitten by before: they do not disagree the day they are written, they disagree the day
     * one is edited, and the symptom is clicks landing beside what is drawn.
     */
    private double[] unproject(int x, int y, int w, int h) {
        return new double[] {
            (x - w / 2.0 - this.panX) / this.zoom + w / 2.0,
            (y - h / 2.0 - this.panY) / this.zoom + h / 2.0,
        };
    }

    /**
     * Keeps the zoomed wheel covering the panel.
     *
     * The scaled canvas is zoom times the panel, so it can slide (zoom - 1) half-panels each
     * way before an edge comes into view. At zoom 1 that is nothing: fit cannot be panned off
     * centre, and zooming all the way out always lands back on it.
     */
    private void clamp(int w, int h) {
        double limX = (this.zoom - 1.0) * w / 2.0;
        double limY = (this.zoom - 1.0) * h / 2.0;
        this.panX = Math.max(-limX, Math.min(limX, this.panX));
        this.panY = Math.max(-limY, Math.min(limY, this.panY));
        // <b>Belt and braces, and known to be.</b> At zoom 1 both limits above are zero, so the
        // pan has already been forced to the centre and this changes nothing - a mutation that
        // deletes it survives the suite, which is the honest reason to say so here rather than
        // leave a later reader to wonder whether it is load-bearing. It stays because it is the
        // one line that states "not zoomed means centred" outright, and because nothing but
        // zoomAt's own floor keeps the zoom from going below 1 in the first place.
        if (this.zoom <= 1.0) {
            this.fit();
        }
    }

    /** Where the "Fit" chip sits while zoomed: the top-left corner, clear of the wheel's rim. */
    static Rectangle fitChipBounds() {
        return new Rectangle(10, 10, 104, 24);
    }

    /** The chip that says how far in the view is and puts it back, drawn in screen space. */
    void paintFitChip(Graphics2D g2) {
        if (this.isFit()) {
            return;
        }
        Rectangle r = fitChipBounds();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(22, 28, 43, 225));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setColor(new Color(56, 132, 190));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setFont(Theme.font("Segoe UI", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        String label = Math.round(this.zoom * 100) + "%  ·  Fit";
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, r.x + (r.width - fm.stringWidth(label)) / 2,
            r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
    }
}
