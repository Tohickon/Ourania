package com.zodiacomputing.ourania.gui;

/**
 * Everything the globe draws with (M11, stage 3).
 *
 * <p><b>Why this exists.</b> {@link GlobeRenderer} drew straight onto a {@code Graphics2D},
 * so it could only draw where AWT is. Measured, it used about fifteen calls - a colour, a line
 * width, a font size, and a handful of shapes and text - so this is those calls and no more.
 * The desktop's {@link AwtPen} hands each straight to the {@code Graphics2D} it wraps; the
 * phone's draws them on Android's canvas.
 *
 * <p><b>State, as Graphics2D has it.</b> The colour, the line width and the font size stay set
 * until changed, and every shape uses the current ones, exactly as the renderer was written
 * against. Coordinates are pixels, integer where AWT's were.
 */
public interface Pen {

    /** The colour every following shape and text uses. */
    void color(Ink ink);

    /** The line width every following outline uses: a plain stroke, square caps, mitred joins. */
    void stroke(float width);

    /** The plain sans-serif font, at this many points, for the following text. */
    void font(int points);

    /** How wide {@code text} is in the plain font at {@code points}, in pixels. */
    int textWidth(String text, int points);

    void drawString(String text, int x, int y);

    void drawLine(int x1, int y1, int x2, int y2);

    void drawPolyline(int[] xs, int[] ys, int count);

    void fillPolygon(int[] xs, int[] ys, int count);

    void fillRect(int x, int y, int w, int h);

    void fillOval(int x, int y, int w, int h);

    void drawOval(int x, int y, int w, int h);

    /** An arc of the oval in the box, from {@code start} degrees, {@code extent} degrees, counter-clockwise. */
    void drawArc(int x, int y, int w, int h, int start, int extent);

    /**
     * An oval filled with a radial gradient centred at (cx, cy) - the colours at the fractions
     * of the radius - then the paint back to the plain colour.
     */
    void fillOvalRadial(int x, int y, int w, int h, float cx, float cy, float radius,
        float[] fractions, Ink[] colors);

    /** Clips everything after it to the oval, until {@link #unclip}. */
    void clipOval(float x, float y, float w, float h);

    /** Back to the clip before the last {@link #clipOval}. */
    void unclip();
}
