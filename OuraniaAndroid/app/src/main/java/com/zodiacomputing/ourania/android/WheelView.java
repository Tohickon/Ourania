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
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.Ink;
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
    private java.util.List<PhoneWheel.Cross> contacts = java.util.Collections.emptyList();
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

    /**
     * This screen's pixels per dp, held as a field rather than fetched where it is needed.
     *
     * <p><b>Every stroke width in this file was a bare number</b> - 1f for a planet's rim, 1.2f
     * for the sky's boundary, 1f for an unlit aspect line - and a bare number is a PHYSICAL
     * pixel. These are the desktop's widths, chosen against a window at about 96 dpi; this
     * phone has about 500, so a "1 pixel" rim came out at a fifth of a dp and the circle it
     * was meant to draw around a planet was invisible. That is most of why the planets read as
     * blobs: the gradient fill and the halo were all there was, with no edge anywhere.
     *
     * <p>Positions and radii are NOT scaled here and must not be. They come from WheelLayout
     * against the real pixel width, and the wheel needs those pixels: the sign, decan, degree
     * and mansion bands have fixed depths, so laying the wheel out in dp collapses the natal
     * band to its 22px minimum. Measured, not assumed - at 308dp wide the natal band is 22
     * deep where at 1080 it is 78. So the geometry stays in pixels and the INK scales.
     */
    private final float density;

    WheelView(Context context) {
        super(context);
        this.density = context.getResources().getDisplayMetrics().density;
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
     * The chart with a second one on the ring outside its planets - the sky (M7) or a partner
     * (M10) - and a dashed line for each contact between the two. A null outer chart is the
     * chart alone.
     */
    void show(ChartFrame f, ChartFrame sky, java.util.List<PhoneWheel.Cross> contacts) {
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
        // The reader's chart ground, not a literal. This was Color.rgb(12, 14, 20) - the same
        // three numbers as PhonePalette.background()'s fallback, written out a second time -
        // so PhonePalette.background() existed and nothing called it, and a reader who chose
        // their own chart background on the desktop got it everywhere except here.
        Ink ground = PhonePalette.background();
        c.drawColor(Color.rgb(ground.getRed(), ground.getGreen(), ground.getBlue()));
        PhoneWheel w = wheel();
        if (w == null) {
            return;
        }
        c.save();
        c.concat(this.view);
        int[] r = w.rings;
        int signOuter = r[WheelLayout.RING_SIGN_OUTER];
        int signInner = r[WheelLayout.RING_SIGN_INNER];

        // The sign band: twelve sectors, each glyph in its element's colour.
        this.line.setColor(Color.rgb(90, 96, 112));
        this.line.setStrokeWidth(1.2f * this.density);
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
                this.line.setStrokeWidth((angle ? 2.2f : 1f) * this.density);
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
            this.line.setStrokeWidth((lit && this.selected >= 0 ? 2.4f : 1.3f) * this.density);
            float[] a = w.point(f.bodies[l.a].lon, w.aspectDisc);
            float[] b = w.point(f.bodies[l.b].lon, w.aspectDisc);
            c.drawLine(a[0], a[1], b[0], b[1], this.line);
        }

        // The sky's contacts to natal planets, dashed so they are not read as natal aspects.
        if (w.sky != null) {
            this.line.setPathEffect(new DashPathEffect(new float[] {6 * density, 4 * density}, 0));
            for (PhoneWheel.Cross h : this.contacts) {
                int t = h.outer;
                int n = h.inner;
                boolean lit = this.selected < 0 || this.selected == PhoneWheel.SKY + t
                    || this.selected == n;
                this.line.setColor(aspectColour(h.type, lit ? 230 : 40));
                this.line.setStrokeWidth((lit && this.selected >= 0 ? 2.6f : 1.6f) * this.density);
                float[] a = w.point(w.sky.bodies[t].lon, w.aspectDisc);
                float[] b = w.point(f.bodies[n].lon, w.aspectDisc);
                c.drawLine(a[0], a[1], b[0], b[1], this.line);
            }
            this.line.setPathEffect(null);
            // The sky's ring: the boundary it shares with the natal planets.
            this.line.setColor(Color.rgb(70, 90, 130));
            this.line.setStrokeWidth(1.2f * this.density);
            c.drawCircle(w.cx, w.cy, w.rings[WheelLayout.RING_BODY_TOP], this.line);
            double skyPhase = PhoneWheel.moonElongation(w.sky);
            for (int i = 0; i < PhoneWheel.PLANETS; i++) {
                float[] p = w.skyBody(i);
                if (p != null) {
                    drawPlanet(c, i, p[0], p[1], PhoneWheel.skyPlanetRadius(i),
                        this.selected == PhoneWheel.SKY + i, skyPhase);
                }
            }
        }

        // The planets as themselves - the globe's pictures of them - with each one's glyph
        // small just inside it, so Mercury and Pluto are told apart without a tap.
        this.text.setTextSize(12f * density);
        double phase = PhoneWheel.moonElongation(f);
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            float[] p = w.body(i);
            if (p == null) {
                continue;
            }
            int rad = w.planetRadius(i);
            drawPlanet(c, i, p[0], p[1], rad, i == this.selected, phase);
            float[] g = w.point(f.bodies[i].lon, w.bodyRadius[i] - rad - 9 * density);
            int[] rgb = PhoneWheel.faceColour(i);
            this.text.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
            c.drawText(Bodies.at(i).glyph, g[0], g[1] + 4 * density, this.text);
        }
        c.restore();
    }

    /** Fire, earth, air, water - the desktop's element hues, lightened for a dark ground. */
    private static int elementColour(int element) {
        int[] c = elementRgb(element);
        return Color.rgb(c[0], c[1], c[2]);
    }

    /**
     * The phone's element colours, from {@link PhonePalette} since the globe needed them too.
     *
     * <p>They were four hard-coded arrays here. The globe is asked for the same four, and a
     * second copy for it would have made three tables on one device - so the values moved to
     * PhonePalette unchanged and this reads them.
     */
    private static int[] elementRgb(int element) {
        Ink ink = PhonePalette.element(element);
        return new int[] {ink.getRed(), ink.getGreen(), ink.getBlue()};
    }

    /**
     * One planet drawn as itself - GlobeRenderer.drawPlanet's pictures, on Android's canvas: lit
     * from the upper left, the Sun glowing, Jupiter belted, Saturn ringed (the back of the ring,
     * the planet, then the front), and the Moon in the phase it was in at the chart's moment.
     *
     * <p><b>Crisp means the disc ends where it ends.</b> The planets carried a three-pass halo
     * in their own colour a quarter past the rim, and a halo is a soft edge by construction -
     * it was the last of the fuzz. It is gone; a planet is its shaded disc and a hairline rim,
     * and the only thing on the wheel that glows is the one thing that does.
     *
     * @param phase the Moon's elongation from the Sun, 0 to 360, for this ring's frame
     */
    private void drawPlanet(Canvas c, int i, float x, float y, float rad, boolean lit,
            double phase) {
        int[] base = PhoneWheel.faceColour(i);
        PhoneWheel.Face face = PhoneWheel.face(i);
        this.fill.setShader(null);
        if (face == PhoneWheel.Face.SUN) {
            drawSun(c, x, y, rad, lit);
            return;
        }
        if (face == PhoneWheel.Face.MOON) {
            drawMoon(c, x, y, rad, lit, base, phase);
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
        // Lit from the upper left, and THE DARK SIDE IS ACTUALLY DARK. The last version ran
        // from a pale highlight to half the planet's colour, so most of the disc sat in the
        // dull middle - Venus came out olive and every planet looked like a felt button. Now
        // the sweep runs highlight, the planet's own colour across the lit face, then down to
        // a third of it on the far side: a sphere has a terminator, and that is what reads as
        // round at this size. The sweep still finishes past the far rim (0.42r + r against
        // 1.3r), so the shadow lands on the disc rather than off it.
        shade(new RadialGradient(x - rad * 0.3f, y - rad * 0.3f, rad * 1.3f,
            new int[] {
                Color.rgb(lighten(base[0], 0.6), lighten(base[1], 0.6), lighten(base[2], 0.6)),
                Color.rgb(base[0], base[1], base[2]),
                Color.rgb((int) (base[0] * 0.32), (int) (base[1] * 0.32), (int) (base[2] * 0.32))},
            new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, rad, this.fill);
        this.fill.setShader(null);
        if (face == PhoneWheel.Face.BANDED) {
            c.save();
            Path clip = new Path();
            clip.addCircle(x, y, rad, Path.Direction.CW);
            c.clipPath(clip);
            this.line.setStrokeWidth(rad * 0.14f);
            for (int k = -2; k <= 2; k++) {
                if (k == 0) {
                    continue;
                }
                float by = y + k * rad * 0.34f;
                this.line.setColor(k % 2 == 0
                    ? Color.argb(150, (int) (base[0] * 0.6), (int) (base[1] * 0.6),
                        (int) (base[2] * 0.6))
                    : Color.argb(150, lighten(base[0], 0.3), lighten(base[1], 0.3),
                        lighten(base[2], 0.3)));
                c.drawLine(x - rad, by, x + rad, by, this.line);
            }
            c.restore();
        }
        if (face == PhoneWheel.Face.RINGED) {
            this.line.setColor(Color.rgb(232, 214, 170));
            this.line.setStrokeWidth(rad * 0.18f);
            c.drawArc(ringBox, 0, 180, false, this.line);       // the front of the ring
        }
        ring(c, x, y, rad, lit,
            Color.rgb((int) (base[0] * 0.7), (int) (base[1] * 0.7), (int) (base[2] * 0.7)));
    }

    /**
     * The Sun: a glow, then a bright disc with a hard edge on it.
     *
     * <p><b>The glow is a gradient, not rings.</b> The corona was five stacked translucent
     * circles at 0.16/k alpha, which summed to a faint orange smear and gave no light at all;
     * shrinking them (the previous fix) only made the smear smaller. A radial gradient from
     * strong at the limb to nothing at twice the radius reads as light leaving the disc, and
     * because the disc is drawn on top with its own rim the edge stays sharp inside it - glow
     * and crisp are not in conflict when the glow is OUTSIDE the edge.
     *
     * <p>The disc is centred, not lit from a side: the Sun is the light. It darkens only toward
     * the limb, as the real one does.
     */
    private void drawSun(Canvas c, float x, float y, float rad, boolean lit) {
        float reach = rad * 2.1f;
        shade(new RadialGradient(x, y, reach,
            new int[] {Color.argb(210, 255, 210, 110), Color.argb(210, 255, 210, 110),
                Color.argb(90, 255, 170, 60), Color.argb(0, 255, 150, 40)},
            new float[] {0f, rad / reach, (rad * 1.4f) / reach, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, reach, this.fill);
        shade(new RadialGradient(x, y, rad,
            new int[] {Color.rgb(255, 255, 242), Color.rgb(255, 240, 170),
                Color.rgb(255, 205, 92), Color.rgb(246, 156, 48)},
            new float[] {0f, 0.5f, 0.82f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, rad, this.fill);
        this.fill.setShader(null);
        ring(c, x, y, rad, lit, Color.rgb(255, 226, 150));
    }

    /**
     * The Moon in its phase: the whole disc in earthshine, the lit part over it, the seas on
     * the lit part only, and the rim round all of it - so a new Moon is still a Moon you can
     * find and tap, not a hole in the band.
     */
    private void drawMoon(Canvas c, float x, float y, float rad, boolean lit, int[] base,
            double phase) {
        this.fill.setColor(Color.rgb(34, 36, 44));
        c.drawCircle(x, y, rad, this.fill);
        float[] pts = PhoneWheel.moonLitOutline(x, y, rad, phase, 24);
        Path litPart = new Path();
        litPart.moveTo(pts[0], pts[1]);
        for (int k = 2; k < pts.length; k += 2) {
            litPart.lineTo(pts[k], pts[k + 1]);
        }
        litPart.close();
        shade(new RadialGradient(x - rad * 0.2f, y - rad * 0.25f, rad * 1.25f,
            Color.rgb(250, 250, 252),
            Color.rgb((int) (base[0] * 0.62), (int) (base[1] * 0.62), (int) (base[2] * 0.62)),
            Shader.TileMode.CLAMP));
        c.drawPath(litPart, this.fill);
        this.fill.setShader(null);
        // The seas, the same on every chart, and only where the Sun is on them.
        c.save();
        c.clipPath(litPart);
        this.fill.setColor(Color.argb(70, 60, 64, 76));
        c.drawCircle(x - rad * 0.25f, y - rad * 0.2f, rad * 0.3f, this.fill);
        c.drawCircle(x + rad * 0.2f, y - rad * 0.3f, rad * 0.2f, this.fill);
        c.drawCircle(x + rad * 0.05f, y + rad * 0.3f, rad * 0.24f, this.fill);
        c.restore();
        ring(c, x, y, rad, lit, Color.rgb(120, 122, 132));
    }

    /**
     * Gives {@code fill} a gradient to paint with, at full strength.
     *
     * <p><b>A Paint's alpha still applies under a shader</b> - the gradient is drawn at the
     * alpha of whatever colour was set last. {@code fill} is shared, and the last colour before
     * a disc was always a translucent one: the halo passes at 7 to 17 per cent, the Moon's seas
     * at 70 of 255. So every shaded disc on the wheel was painted mostly transparent over a
     * black ground, which is the dim, muddy, edgeless look that two commits tried to fix by
     * resizing halos and gradients. Every shaded fill goes through here so it cannot recur.
     */
    private void shade(Shader shader) {
        this.fill.setColor(Color.BLACK);          // opaque; the shader supplies the colour
        this.fill.setShader(shader);
    }

    /**
     * A planet's rim, and the lit halo around a selected one.
     *
     * <p><b>This is the edge that was missing.</b> The unlit rim was {@code 1f} - one physical
     * pixel, a fifth of a dp on this screen - so every planet was a soft gradient with a
     * halo and no circumference at all. It is a dp and a bit now, which is what a hairline
     * means, and it is the line that makes a small disc read as a disc.
     */
    private void ring(Canvas c, float x, float y, float rad, boolean lit, int rim) {
        this.line.setColor(lit ? Color.rgb(255, 238, 170) : rim);
        this.line.setStrokeWidth((lit ? 2.4f : 1.2f) * this.density);
        c.drawCircle(x, y, rad, this.line);
        if (lit) {
            this.line.setColor(Color.argb(110, 255, 238, 170));
            this.line.setStrokeWidth(1.6f * this.density);
            c.drawCircle(x, y, rad + 5 * this.density, this.line);
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
