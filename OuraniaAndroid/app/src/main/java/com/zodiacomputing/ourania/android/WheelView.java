package com.zodiacomputing.ourania.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.WheelLayout;

/**
 * The chart wheel on the phone (M4): the signs, the houses, the aspect lines and the ten
 * planets, which a reader can pinch to zoom, drag, and tap.
 *
 * <p>Where everything goes is {@link PhoneWheel}'s, which takes it from the desktop's
 * {@link WheelLayout}. This class only paints it with Android's canvas and turns touches into
 * wheel coordinates - the one inverse transform, applied before {@link PhoneWheel#bodyAt}, so
 * a planet is tapped where it is drawn at every zoom.
 */
final class WheelView extends View {

    /** Told which planet was tapped, or -1 for empty wheel. */
    interface OnBody {
        void tapped(int body);
    }

    private static final float MAX_ZOOM = 4f;

    private PhoneWheel wheel;
    private ChartFrame frame;
    private int selected = -1;
    private OnBody listener;

    private final Matrix view = new Matrix();
    private final Matrix inverse = new Matrix();
    private float zoom = 1f;

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ScaleGestureDetector pinch;
    private final GestureDetector gestures;

    WheelView(Context context) {
        super(context);
        this.line.setStyle(Paint.Style.STROKE);
        this.text.setTextAlign(Paint.Align.CENTER);
        this.pinch = new ScaleGestureDetector(context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector d) {
                    zoomBy(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                    return true;
                }
            });
        this.gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                if (zoom > 1f) {
                    view.postTranslate(-dx, -dy);
                    clampPan();
                    invalidate();
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                tap(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                zoom = 1f;
                view.reset();
                invalidate();
                return true;
            }
        });
    }

    void setOnBody(OnBody l) {
        this.listener = l;
    }

    void show(ChartFrame f) {
        this.frame = f;
        this.wheel = null;
        this.selected = -1;
        this.zoom = 1f;
        this.view.reset();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        setMeasuredDimension(width, width);         // square: the wheel is a circle
    }

    private PhoneWheel wheel() {
        if (this.frame == null || getWidth() == 0) {
            return null;
        }
        if (this.wheel == null || this.wheel.width != getWidth()) {
            this.wheel = PhoneWheel.of(this.frame, getWidth(), getHeight());
        }
        return this.wheel;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // The page scrolls; a pinch, or a drag on a zoomed wheel, is the wheel's and not the page's.
        if (getParent() != null && (e.getPointerCount() > 1 || this.zoom > 1f)) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        this.pinch.onTouchEvent(e);
        this.gestures.onTouchEvent(e);
        return true;
    }

    private void zoomBy(float factor, float fx, float fy) {
        float to = Math.max(1f, Math.min(MAX_ZOOM, this.zoom * factor));
        float applied = to / this.zoom;
        this.zoom = to;
        this.view.postScale(applied, applied, fx, fy);
        if (this.zoom == 1f) {
            this.view.reset();
        }
        clampPan();
        invalidate();
    }

    /** Keeps the zoomed wheel covering the view, so it cannot be dragged off-screen. */
    private void clampPan() {
        float[] v = new float[9];
        this.view.getValues(v);
        float w = getWidth();
        float h = getHeight();
        float minX = w - w * this.zoom;
        float minY = h - h * this.zoom;
        v[Matrix.MTRANS_X] = Math.max(minX, Math.min(0f, v[Matrix.MTRANS_X]));
        v[Matrix.MTRANS_Y] = Math.max(minY, Math.min(0f, v[Matrix.MTRANS_Y]));
        this.view.setValues(v);
    }

    private void tap(float x, float y) {
        PhoneWheel w = wheel();
        if (w == null) {
            return;
        }
        this.view.invert(this.inverse);
        float[] p = {x, y};
        this.inverse.mapPoints(p);
        // A fingertip, not a cursor: about four millimetres of grace around a planet's disc.
        float grab = 10f * getResources().getDisplayMetrics().density / this.zoom;
        this.selected = w.bodyAt(p[0], p[1], grab);
        invalidate();
        if (this.listener != null) {
            this.listener.tapped(this.selected);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(Color.rgb(12, 14, 20));
        PhoneWheel w = wheel();
        if (w == null) {
            return;
        }
        c.save();
        c.concat(this.view);
        float density = getResources().getDisplayMetrics().density;
        int[] r = w.rings;
        int signOuter = r[WheelLayout.RING_SIGN_OUTER];
        int signInner = r[WheelLayout.RING_SIGN_INNER];

        // The sign band: twelve sectors, each glyph in its element's colour.
        this.line.setColor(Color.rgb(90, 96, 112));
        this.line.setStrokeWidth(1.2f);
        c.drawCircle(w.cx, w.cy, signOuter, this.line);
        c.drawCircle(w.cx, w.cy, signInner, this.line);
        c.drawCircle(w.cx, w.cy, r[WheelLayout.RING_OUTER], this.line);
        this.text.setTextSize((signOuter - signInner) * 0.62f);
        for (int s = 0; s < 12; s++) {
            float[] a = w.point(s * 30.0, signInner);
            float[] b = w.point(s * 30.0, r[WheelLayout.RING_OUTER]);
            c.drawLine(a[0], a[1], b[0], b[1], this.line);
            float[] g = w.point(s * 30.0 + 15.0, (signOuter + signInner) / 2.0);
            this.text.setColor(elementColour(s % 4));
            c.drawText(Zodiac.SIGN_GLYPHS[s], g[0], g[1] + this.text.getTextSize() * 0.35f, this.text);
        }
        // Degree ticks every five degrees just outside the signs.
        for (int d = 0; d < 360; d += 5) {
            float[] a = w.point(d, signOuter);
            float[] b = w.point(d, signOuter + (d % 10 == 0 ? 6 : 3) * density);
            c.drawLine(a[0], a[1], b[0], b[1], this.line);
        }

        // Houses: cusp lines from the aspect circle to the signs, the angles heavier.
        ChartFrame f = w.frame;
        c.drawCircle(w.cx, w.cy, w.aspectDisc, this.line);
        if (!f.timeUnknown) {
            this.text.setTextSize(11f * density);
            this.text.setColor(Color.rgb(150, 160, 180));
            for (int h = 1; h <= 12; h++) {
                boolean angle = h == 1 || h == 4 || h == 7 || h == 10;
                this.line.setColor(angle ? Color.rgb(220, 220, 230) : Color.rgb(70, 76, 90));
                this.line.setStrokeWidth(angle ? 2.2f : 1f);
                float[] a = w.point(f.cusps[h], w.aspectDisc);
                float[] b = w.point(f.cusps[h], signInner);
                c.drawLine(a[0], a[1], b[0], b[1], this.line);
                double next = f.cusps[h == 12 ? 1 : h + 1];
                double mid = f.cusps[h] + (((next - f.cusps[h]) % 360.0 + 360.0) % 360.0) / 2.0;
                float[] n = w.point(mid, w.aspectDisc - 12 * density);
                c.drawText(String.valueOf(h), n[0], n[1] + 4 * density, this.text);
            }
        }

        // Aspect lines, inside the aspect circle, each end at its planet's longitude.
        for (PhoneWheel.Line l : w.aspects()) {
            boolean lit = this.selected < 0 || l.a == this.selected || l.b == this.selected;
            this.line.setColor(aspectColour(l.type, lit ? 210 : 45));
            this.line.setStrokeWidth(lit && this.selected >= 0 ? 2.4f : 1.3f);
            float[] a = w.point(f.bodies[l.a].lon, w.aspectDisc);
            float[] b = w.point(f.bodies[l.b].lon, w.aspectDisc);
            c.drawLine(a[0], a[1], b[0], b[1], this.line);
        }

        // The planets: a disc with its glyph, the selected one ringed.
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            float[] p = w.body(i);
            if (p == null) {
                continue;
            }
            float rad = PhoneWheel.glyphRadius(i);
            this.fill.setColor(i == this.selected ? Color.rgb(255, 214, 102) : Color.rgb(196, 200, 210));
            c.drawCircle(p[0], p[1], rad, this.fill);
            if (i == this.selected) {
                this.line.setColor(Color.rgb(255, 238, 170));
                this.line.setStrokeWidth(2f);
                c.drawCircle(p[0], p[1], rad + 5, this.line);
            }
            this.text.setColor(Color.rgb(20, 22, 30));
            this.text.setTextSize(rad * 1.3f);
            c.drawText(Bodies.at(i).glyph, p[0], p[1] + rad * 0.45f, this.text);
        }
        c.restore();
    }

    /** Fire, earth, air, water - the desktop's element hues, lightened for a dark ground. */
    private static int elementColour(int element) {
        switch (element) {
            case 0: return Color.rgb(255, 120, 90);
            case 1: return Color.rgb(120, 200, 110);
            case 2: return Color.rgb(240, 214, 90);
            default: return Color.rgb(110, 170, 255);
        }
    }

    /** Hard aspects red, soft blue, the conjunction gold, the rest grey. */
    private static int aspectColour(Aspects.Type t, int alpha) {
        String l = t.label;
        if (l.equals("Conjunction")) {
            return Color.argb(alpha, 255, 209, 102);
        }
        if (l.equals("Square") || l.equals("Opposition")) {
            return Color.argb(alpha, 230, 90, 80);
        }
        if (l.equals("Trine") || l.equals("Sextile")) {
            return Color.argb(alpha, 100, 160, 255);
        }
        return Color.argb(alpha, 160, 160, 170);
    }
}
