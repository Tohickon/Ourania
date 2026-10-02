package com.zodiacomputing.ourania.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * The desktop's {@link Pen}: each call handed straight to a {@code Graphics2D}, as the globe
 * made it before stage 3 (M11). The stroke and font caches moved here from the renderer with
 * their reasons; antialiasing is switched on here, as the renderer's paint did first thing.
 */
final class AwtPen implements Pen {

    private final Graphics2D g;
    private final java.util.ArrayDeque<Shape> clips = new java.util.ArrayDeque<>();

    AwtPen(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** An AWT colour as an {@link Ink}, for the desktop's answers to {@link GlobeSource}. */
    static Ink ink(Color c) {
        return c == null ? null : new Ink(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    private static Color awt(Ink i) {
        return new Color(i.getRed(), i.getGreen(), i.getBlue(), i.getAlpha());
    }

    /**
     * Strokes, cached by width.
     *
     * A new BasicStroke per segment was allocating thousands of identical objects a frame.
     * There are four widths in this scene.
     */
    private static final java.util.Map<Float, Stroke> STROKES =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Fonts, cached by point size, for the same reason as the strokes.
     *
     * A frame writes about two hundred glyphs - bodies, bounds, decans, signs - and each one
     * was allocating a Font and asking for fresh FontMetrics. There are three sizes.
     */
    private static final java.util.Map<Integer, Font> FONTS =
        new java.util.concurrent.ConcurrentHashMap<>();

    private static Font fontOf(int points) {
        return FONTS.computeIfAbsent(points, p -> new Font("SansSerif", 0, p));
    }

    @Override
    public void color(Ink ink) {
        this.g.setColor(awt(ink));
    }

    @Override
    public void stroke(float width) {
        this.g.setStroke(STROKES.computeIfAbsent(width, BasicStroke::new));
    }

    @Override
    public void font(int points) {
        this.g.setFont(fontOf(points));
    }

    @Override
    public int textWidth(String text, int points) {
        return this.g.getFontMetrics(fontOf(points)).stringWidth(text);
    }

    @Override
    public void drawString(String text, int x, int y) {
        this.g.drawString(text, x, y);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        this.g.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawPolyline(int[] xs, int[] ys, int count) {
        this.g.drawPolyline(xs, ys, count);
    }

    @Override
    public void fillPolygon(int[] xs, int[] ys, int count) {
        this.g.fillPolygon(xs, ys, count);
    }

    @Override
    public void fillRect(int x, int y, int w, int h) {
        this.g.fillRect(x, y, w, h);
    }

    @Override
    public void fillOval(int x, int y, int w, int h) {
        this.g.fillOval(x, y, w, h);
    }

    @Override
    public void drawOval(int x, int y, int w, int h) {
        this.g.drawOval(x, y, w, h);
    }

    @Override
    public void drawArc(int x, int y, int w, int h, int start, int extent) {
        this.g.drawArc(x, y, w, h, start, extent);
    }

    @Override
    public void fillOvalRadial(int x, int y, int w, int h, float cx, float cy, float radius,
            float[] fractions, Ink[] colors) {
        Color[] cs = new Color[colors.length];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = awt(colors[i]);
        }
        this.g.setPaint(new java.awt.RadialGradientPaint(
            new java.awt.geom.Point2D.Float(cx, cy), radius, fractions, cs));
        this.g.fillOval(x, y, w, h);
        this.g.setPaint(null);
    }

    @Override
    public void clipOval(float x, float y, float w, float h) {
        this.clips.push(this.g.getClip() == null ? NO_CLIP : this.g.getClip());
        this.g.setClip(new java.awt.geom.Ellipse2D.Float(x, y, w, h));
    }

    @Override
    public void unclip() {
        Shape was = this.clips.pop();
        this.g.setClip(was == NO_CLIP ? null : was);
    }

    /** A stand-in for "no clip", since the stack cannot hold null. */
    private static final Shape NO_CLIP = new java.awt.Rectangle();
}
