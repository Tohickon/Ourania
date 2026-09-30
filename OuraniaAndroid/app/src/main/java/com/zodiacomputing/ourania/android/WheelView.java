package com.zodiacomputing.ourania.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Transits;
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
    private ChartFrame sky;
    private java.util.List<Transits.Hit> contacts = java.util.Collections.emptyList();
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
        show(f, null, null);
    }

    /**
     * The chart with the sky on a ring outside its planets, and a dashed line for each contact
     * the sky makes to a natal planet (M7). A null sky is the birth chart alone.
     */
    void show(ChartFrame f, ChartFrame sky, java.util.List<Transits.Hit> contacts) {
        this.frame = f;
        this.sky = sky;
        this.contacts = contacts == null ? java.util.Collections.emptyList() : contacts;
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
            this.wheel = PhoneWheel.of(this.frame, this.sky, getWidth(), getHeight());
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
            for (int h = 1; h <= 12; h++) {
                boolean angle = h == 1 || h == 4 || h == 7 || h == 10;
                this.line.setColor(angle ? Color.rgb(220, 220, 230) : Color.rgb(70, 76, 90));
                this.line.setStrokeWidth(angle ? 2.2f : 1f);
                float[] a = w.point(f.cusps[h], w.aspectDisc);
                float[] b = w.point(f.cusps[h], signInner);
                c.drawLine(a[0], a[1], b[0], b[1], this.line);
            }
            // The numbers in the open ring between the aspect circle and the planets, bold, in
            // the colour of their house's element - as the desktop draws them.
            this.text.setTextSize(w.houseTextSize(density));
            this.text.setFakeBoldText(true);
            for (int h = 1; h <= 12; h++) {
                float[] n = w.housePoint(h);
                int[] rgb = elementRgb((h - 1) % 4);
                this.text.setColor(Color.argb(200, rgb[0], rgb[1], rgb[2]));
                c.drawText(String.valueOf(h), n[0], n[1] + this.text.getTextSize() * 0.36f, this.text);
            }
            this.text.setFakeBoldText(false);
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

        // The sky's contacts to natal planets, dashed so they are not read as natal aspects.
        if (w.sky != null) {
            this.line.setPathEffect(new DashPathEffect(new float[] {6 * density, 4 * density}, 0));
            for (Transits.Hit h : this.contacts) {
                int t = index(w.sky, h.transiting);
                int n = index(f, h.natal);
                if (t < 0 || n < 0) {
                    continue;                           // a contact to an angle: no planet to join
                }
                boolean lit = this.selected < 0 || this.selected == PhoneWheel.SKY + t
                    || this.selected == n;
                this.line.setColor(aspectColour(h.type, lit ? 230 : 40));
                this.line.setStrokeWidth(lit && this.selected >= 0 ? 2.6f : 1.6f);
                float[] a = w.point(w.sky.bodies[t].lon, w.aspectDisc);
                float[] b = w.point(f.bodies[n].lon, w.aspectDisc);
                c.drawLine(a[0], a[1], b[0], b[1], this.line);
            }
            this.line.setPathEffect(null);
            // The sky's ring: the boundary it shares with the natal planets.
            this.line.setColor(Color.rgb(70, 90, 130));
            this.line.setStrokeWidth(1.2f);
            c.drawCircle(w.cx, w.cy, w.rings[WheelLayout.RING_BODY_TOP], this.line);
            for (int i = 0; i < PhoneWheel.PLANETS; i++) {
                float[] p = w.skyBody(i);
                if (p != null) {
                    drawPlanet(c, i, p[0], p[1], PhoneWheel.skyPlanetRadius(i),
                        this.selected == PhoneWheel.SKY + i);
                }
            }
        }

        // The planets as themselves - the globe's pictures of them - with each one's glyph
        // small just inside it, so Mercury and Pluto are told apart without a tap.
        this.text.setTextSize(12f * density);
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            float[] p = w.body(i);
            if (p == null) {
                continue;
            }
            int rad = w.planetRadius(i);
            drawPlanet(c, i, p[0], p[1], rad, i == this.selected);
            float[] g = w.point(f.bodies[i].lon, w.bodyRadius[i] - rad - 9 * density);
            int[] rgb = PhoneWheel.faceColour(i);
            this.text.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
            c.drawText(Bodies.at(i).glyph, g[0], g[1] + 4 * density, this.text);
        }
        c.restore();
    }

    /** Where a named planet is among the first ten of a frame, or -1. */
    private static int index(ChartFrame f, String name) {
        for (int i = 0; i < PhoneWheel.PLANETS && i < f.bodies.length; i++) {
            if (f.bodies[i] != null && name.equals(f.bodies[i].name)) {
                return i;
            }
        }
        return -1;
    }

    /** Fire, earth, air, water - the desktop's element hues, lightened for a dark ground. */
    private static int elementColour(int element) {
        int[] c = elementRgb(element);
        return Color.rgb(c[0], c[1], c[2]);
    }

    private static int[] elementRgb(int element) {
        switch (element) {
            case 0: return new int[] {255, 120, 90};
            case 1: return new int[] {120, 200, 110};
            case 2: return new int[] {240, 214, 90};
            default: return new int[] {110, 170, 255};
        }
    }

    /**
     * One planet drawn as itself - GlobeRenderer.drawPlanet's pictures, on Android's canvas: lit
     * from the upper left, the Sun glowing with a white core, Jupiter belted, Saturn ringed
     * (the back of the ring, the planet, then the front), and the Moon grey with its seas.
     */
    private void drawPlanet(Canvas c, int i, float x, float y, float rad, boolean lit) {
        int[] base = PhoneWheel.faceColour(i);
        PhoneWheel.Face face = PhoneWheel.face(i);
        this.fill.setShader(null);
        if (face == PhoneWheel.Face.SUN) {
            for (int k = 5; k >= 1; k--) {
                this.fill.setColor(Color.argb((int) (255 * 0.16 / k), 255, 186, 82));
                c.drawCircle(x, y, rad + k * rad * 0.28f, this.fill);
            }
            this.fill.setShader(new RadialGradient(x - rad * 0.22f, y - rad * 0.22f, rad * 1.5f,
                new int[] {Color.rgb(255, 255, 246), Color.rgb(255, 232, 158),
                    Color.rgb(255, 168, 56), Color.rgb(214, 96, 28)},
                new float[] {0f, 0.35f, 0.75f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, rad, this.fill);
            this.fill.setShader(null);
            ring(c, x, y, rad, lit, Color.rgb(255, 214, 130));
            return;
        }
        float rw = rad * 2.15f;
        float rh = Math.max(2f, rad * 0.52f);
        RectF ringBox = new RectF(x - rw, y - rh, x + rw, y + rh);
        if (face == PhoneWheel.Face.RINGED) {
            this.line.setColor(Color.argb(184, 206, 186, 142));
            this.line.setStrokeWidth(rad * 0.16f);
            c.drawArc(ringBox, 180, 180, false, this.line);     // the back of the ring
        }
        // A soft halo in the planet's own colour, so it reads as a light above the band.
        for (float[] pass : new float[][] {{0.7f, 0.06f}, {0.42f, 0.10f}, {0.2f, 0.16f}}) {
            this.fill.setColor(Color.argb((int) (255 * pass[1]), lighten(base[0], 0.35),
                lighten(base[1], 0.35), lighten(base[2], 0.35)));
            c.drawCircle(x, y, rad * (1 + pass[0]), this.fill);
        }
        this.fill.setShader(new RadialGradient(x - rad * 0.35f, y - rad * 0.35f, rad * 1.5f,
            Color.rgb(lighten(base[0], 0.45), lighten(base[1], 0.45), lighten(base[2], 0.45)),
            Color.rgb((int) (base[0] * 0.5), (int) (base[1] * 0.5), (int) (base[2] * 0.5)),
            Shader.TileMode.CLAMP));
        c.drawCircle(x, y, rad, this.fill);
        this.fill.setShader(null);
        if (face == PhoneWheel.Face.BANDED || face == PhoneWheel.Face.MOON) {
            c.save();
            Path clip = new Path();
            clip.addCircle(x, y, rad, Path.Direction.CW);
            c.clipPath(clip);
            if (face == PhoneWheel.Face.BANDED) {
                this.line.setStrokeWidth(rad * 0.14f);
                for (int k = -2; k <= 2; k++) {
                    if (k == 0) {
                        continue;
                    }
                    float by = y + k * rad * 0.34f;
                    this.line.setColor(k % 2 == 0
                        ? Color.argb(217, (int) (base[0] * 0.72), (int) (base[1] * 0.72),
                            (int) (base[2] * 0.72))
                        : Color.argb(217, lighten(base[0], 0.25), lighten(base[1], 0.25),
                            lighten(base[2], 0.25)));
                    c.drawLine(x - rad, by, x + rad, by, this.line);
                }
            } else {
                // The Moon's seas: a few darker patches, the same on every chart.
                this.fill.setColor(Color.argb(70, 60, 64, 76));
                c.drawCircle(x - rad * 0.25f, y - rad * 0.2f, rad * 0.3f, this.fill);
                c.drawCircle(x + rad * 0.2f, y - rad * 0.3f, rad * 0.2f, this.fill);
                c.drawCircle(x + rad * 0.05f, y + rad * 0.3f, rad * 0.24f, this.fill);
            }
            c.restore();
        }
        if (face == PhoneWheel.Face.RINGED) {
            this.line.setColor(Color.rgb(232, 214, 170));
            this.line.setStrokeWidth(rad * 0.18f);
            c.drawArc(ringBox, 0, 180, false, this.line);       // the front of the ring
        }
        ring(c, x, y, rad, lit,
            Color.rgb((int) (base[0] * 0.5), (int) (base[1] * 0.5), (int) (base[2] * 0.5)));
    }

    /** A planet's rim; a bright ring, and a second fainter one, around the tapped planet. */
    private void ring(Canvas c, float x, float y, float rad, boolean lit, int rim) {
        this.line.setColor(lit ? Color.rgb(255, 238, 170) : rim);
        this.line.setStrokeWidth(lit ? 3f : 1f);
        c.drawCircle(x, y, rad, this.line);
        if (lit) {
            this.line.setColor(Color.argb(110, 255, 238, 170));
            this.line.setStrokeWidth(2f);
            c.drawCircle(x, y, rad + 8, this.line);
        }
    }

    private static int lighten(int v, double t) {
        return (int) (v + (255 - v) * t);
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
