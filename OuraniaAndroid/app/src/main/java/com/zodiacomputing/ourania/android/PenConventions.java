package com.zodiacomputing.ourania.android;

/**
 * Where AWT and Android disagree about how to draw the same thing (M11, stage 4).
 *
 * <p><b>Why this is a class and not three lines inside {@link AndroidPen}.</b>
 * {@link com.zodiacomputing.ourania.gui.GlobeRenderer} was written against
 * {@code Graphics2D} and its {@link com.zodiacomputing.ourania.gui.Pen} keeps AWT's
 * conventions, because changing them would have changed the drawing rather than moved it.
 * Android's canvas does two of those things differently, and a difference that is wrong is
 * invisible: an arc swept the wrong way is still an arc, and a mitre clipped early still looks
 * like a join. Nothing on the phone reports either.
 *
 * <p>So the differences live here, each with the two conventions named, and the phone's unit
 * tests can hold them to it. {@code AndroidPen} itself cannot be unit-tested off a device -
 * every call on it goes to {@code Canvas}, which throws on the JVM - so what is testable is
 * pulled out rather than left untested inside it.
 *
 * <p>No Android imports, deliberately. A class that mentions {@code android.graphics} cannot be
 * relied on to load in a plain JVM test.
 */
final class PenConventions {

    private PenConventions() { }

    /**
     * The miter limit {@code BasicStroke(width)} uses, which Android's {@code Paint} does not.
     *
     * <p><b>AWT's default is 10 and Android's is 4.</b> Below the limit a mitred join is cut
     * off square instead, so on the globe's chunked polylines - which is where the renderer's
     * joins are - a sharp turn would be bevelled on the phone and pointed on the desktop. The
     * two surfaces are meant to draw the same globe, so the phone is told AWT's number.
     */
    static final float MITER_LIMIT = 10.0f;

    /**
     * Where an arc starts, in Android's terms, given AWT's.
     *
     * <p><b>Both measure from three o'clock and they run opposite ways.</b>
     * {@code Graphics2D.drawArc} counts degrees counter-clockwise; {@code Canvas.drawArc}
     * counts them clockwise. So the angle is negated, and the start is unchanged only at zero
     * and at 180.
     */
    static float arcStart(int awtStartDegrees) {
        return -awtStartDegrees;
    }

    /**
     * How far an arc sweeps, in Android's terms, given AWT's.
     *
     * <p>Negated for the reason {@link #arcStart} gives. A positive AWT extent opens
     * counter-clockwise from the start; the same arc on Android is a negative sweep from the
     * same, negated, start.
     */
    static float arcSweep(int awtExtentDegrees) {
        return -awtExtentDegrees;
    }

    /**
     * The text size for a {@code Pen.font(points)}.
     *
     * <p>The same number, and that is the point of writing it down. The renderer works in
     * pixels throughout - {@link com.zodiacomputing.ourania.gui.Globe} projects to pixels and
     * the pen's coordinates are pixels - and AWT's {@code Font} size at the default 72 dpi
     * transform is pixels too, so Android's {@code setTextSize}, which is pixels, takes the
     * figure unchanged. <b>It must not be converted through density.</b> Scaling text by the
     * screen's density while every coordinate around it stays in pixels is how a glyph ends up
     * three times the size of the ring it sits on.
     */
    static float textSize(int points) {
        return points;
    }
}
