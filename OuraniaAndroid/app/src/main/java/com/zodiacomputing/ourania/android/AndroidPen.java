package com.zodiacomputing.ourania.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.zodiacomputing.ourania.gui.Ink;
import com.zodiacomputing.ourania.gui.Pen;

/**
 * The phone's {@link Pen}: the globe's sixteen drawing calls, on Android's canvas (M11, stage 4).
 *
 * <p><b>The desktop's {@code AwtPen} is the reference, not a starting point.</b> Stage 3 reduced
 * {@link com.zodiacomputing.ourania.gui.GlobeRenderer} to these calls precisely so that a second
 * surface could answer them, and the two surfaces are meant to draw the same globe - so each
 * method here is the matching Android call, and where Android's convention differs the
 * difference is named in {@link PenConventions} rather than absorbed quietly here.
 *
 * <p><b>One Paint, with state, because that is what the renderer was written against.</b>
 * {@code Pen}'s colour, line width and font stay set until changed, the way a
 * {@code Graphics2D} holds them. So this keeps a single {@code Paint} and flips only its style
 * between filling and stroking - a Paint per call would allocate thousands a frame, which is the
 * allocation the desktop's stroke and font caches exist to avoid.
 *
 * <p><b>Measuring text does not disturb the pen.</b> {@code textWidth} is asked about a size
 * that may not be the one set, exactly as {@code AwtPen} asks {@code getFontMetrics(font)}
 * without touching the graphics, so it measures with a second Paint of its own.
 */
public final class AndroidPen implements Pen {

    private final Canvas canvas;
    private final Paint paint = new Paint();
    private final Paint measure = new Paint();
    private final RectF box = new RectF();
    private final Path path = new Path();

    /**
     * How deep the clip stack is, so {@link #unclip} restores the right one.
     *
     * <p>Android has {@code save}/{@code restore} where AWT has to be handed its previous clip
     * back, so the stack is the canvas's own and this only counts. The count is what stops an
     * unbalanced {@code unclip} restoring past the frame's own save and leaving the next frame
     * drawing through someone else's clip.
     */
    private int clipDepth;

    public AndroidPen(Canvas canvas) {
        this.canvas = canvas;
        // As AwtPen's constructor does, and for the same reason: the renderer's paint switched
        // antialiasing on first thing, so it is a property of the surface and not of a call.
        this.paint.setAntiAlias(true);
        this.measure.setAntiAlias(true);
        this.paint.setTypeface(Typeface.SANS_SERIF);
        this.measure.setTypeface(Typeface.SANS_SERIF);
        // BasicStroke(width)'s defaults, which Android does not share. See PenConventions.
        this.paint.setStrokeCap(Paint.Cap.SQUARE);
        this.paint.setStrokeJoin(Paint.Join.MITER);
        this.paint.setStrokeMiter(PenConventions.MITER_LIMIT);
    }

    @Override
    public void color(Ink ink) {
        // <b>Ink packs alpha-red-green-blue exactly as an Android colour int does</b>, which is
        // why there is no arithmetic here: Ink was written to pack as java.awt.Color packs, and
        // Android's Color uses that same layout. AndroidPenTest pins it rather than trusting it.
        this.paint.setColor(ink.getRGB());
    }

    @Override
    public void stroke(float width) {
        this.paint.setStrokeWidth(width);
    }

    @Override
    public void font(int points) {
        this.paint.setTextSize(PenConventions.textSize(points));
    }

    @Override
    public int textWidth(String text, int points) {
        this.measure.setTextSize(PenConventions.textSize(points));
        // AWT's stringWidth is an int; rounding rather than truncating, because the renderer
        // centres text by halving this and a half-pixel bias is visible on a glyph.
        return Math.round(this.measure.measureText(text));
    }

    @Override
    public void drawString(String text, int x, int y) {
        // FILL, or Android outlines the glyph with the current stroke instead of drawing it.
        // (x, y) is the left end of the baseline on both surfaces.
        this.paint.setStyle(Paint.Style.FILL);
        this.canvas.drawText(text, x, y, this.paint);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        this.paint.setStyle(Paint.Style.STROKE);
        this.canvas.drawLine(x1, y1, x2, y2, this.paint);
    }

    @Override
    public void drawPolyline(int[] xs, int[] ys, int count) {
        if (count < 2) {
            return;
        }
        this.paint.setStyle(Paint.Style.STROKE);
        this.canvas.drawPath(this.trace(xs, ys, count, false), this.paint);
    }

    @Override
    public void fillPolygon(int[] xs, int[] ys, int count) {
        if (count < 3) {
            return;
        }
        this.paint.setStyle(Paint.Style.FILL);
        this.canvas.drawPath(this.trace(xs, ys, count, true), this.paint);
    }

    /**
     * The first {@code count} points as a path, reusing one {@link Path}.
     *
     * <p>Reset rather than replaced for the reason the desktop caches its strokes: the globe
     * draws its rings as chunked polylines and a frame traces hundreds of them.
     */
    private Path trace(int[] xs, int[] ys, int count, boolean close) {
        this.path.rewind();
        this.path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < count; i++) {
            this.path.lineTo(xs[i], ys[i]);
        }
        if (close) {
            this.path.close();
        }
        return this.path;
    }

    @Override
    public void fillRect(int x, int y, int w, int h) {
        this.paint.setStyle(Paint.Style.FILL);
        this.canvas.drawRect(x, y, x + w, y + h, this.paint);
    }

    @Override
    public void fillOval(int x, int y, int w, int h) {
        this.paint.setStyle(Paint.Style.FILL);
        this.box.set(x, y, x + w, y + h);
        this.canvas.drawOval(this.box, this.paint);
    }

    @Override
    public void drawOval(int x, int y, int w, int h) {
        this.paint.setStyle(Paint.Style.STROKE);
        this.box.set(x, y, x + w, y + h);
        this.canvas.drawOval(this.box, this.paint);
    }

    @Override
    public void drawArc(int x, int y, int w, int h, int start, int extent) {
        this.paint.setStyle(Paint.Style.STROKE);
        this.box.set(x, y, x + w, y + h);
        // useCenter false: an arc, not a pie slice. Graphics2D.drawArc draws the curve alone.
        this.canvas.drawArc(this.box, PenConventions.arcStart(start),
            PenConventions.arcSweep(extent), false, this.paint);
    }

    @Override
    public void fillOvalRadial(int x, int y, int w, int h, float cx, float cy, float radius,
            float[] fractions, Ink[] colors) {
        int[] argb = new int[colors.length];
        for (int i = 0; i < argb.length; i++) {
            argb[i] = colors[i].getRGB();
        }
        // CLAMP matches RadialGradientPaint's default NO_CYCLE: past the last fraction the last
        // colour continues rather than repeating or reflecting.
        this.paint.setShader(new RadialGradient(cx, cy, radius, argb, fractions,
            Shader.TileMode.CLAMP));
        this.paint.setStyle(Paint.Style.FILL);
        this.box.set(x, y, x + w, y + h);
        this.canvas.drawOval(this.box, this.paint);
        // Put back, as AwtPen sets its paint back to null: a shader left on would tint every
        // shape after it, and the renderer draws the beads before most of the scene.
        this.paint.setShader(null);
    }

    @Override
    public void clipOval(float x, float y, float w, float h) {
        this.canvas.save();
        this.clipDepth++;
        this.path.rewind();
        this.box.set(x, y, x + w, y + h);
        this.path.addOval(this.box, Path.Direction.CW);
        this.canvas.clipPath(this.path);
    }

    @Override
    public void unclip() {
        // <b>An unclip with nothing clipped is ignored, not passed on.</b> Canvas.restore past
        // the matching save pops a state this pen never pushed - the View's own, in practice -
        // and the damage shows up somewhere else entirely, which is the hardest kind to find.
        if (this.clipDepth <= 0) {
            return;
        }
        this.clipDepth--;
        this.canvas.restore();
    }
}
