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
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.LunarMansions;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.Ink;
import com.zodiacomputing.ourania.gui.Settings;
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

        // The zodiac and its subdivisions, rim to planets: the mansions when asked for, the rim
        // degree scale, decans, signs, bounds and the inner degree scale.
        drawZodiac(c, w);

        // Houses: cusp lines from the aspect circle up to the zodiac's inner edge, the angles
        // heavier. They used to run on to the signs, through nothing; with the bounds and the
        // degree scale drawn there they would cut every segment they crossed.
        ChartFrame f = w.frame;
        c.drawCircle(w.cx, w.cy, w.aspectDisc, this.line);
        if (!f.timeUnknown) {
            for (int h = 1; h <= 12; h++) {
                boolean angle = h == 1 || h == 4 || h == 7 || h == 10;
                this.line.setColor(angle ? Color.rgb(220, 220, 230) : Color.rgb(70, 76, 90));
                this.line.setStrokeWidth((angle ? 2.2f : 1f) * this.density);
                float[] a = w.point(f.cusps[h], w.aspectDisc);
                float[] b = w.point(f.cusps[h], r[WheelLayout.RING_DEGREE_INNER]);
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

        // The planets as themselves, each carrying its own glyph - as the desktop's beads do.
        //
        // <b>The glyph used to sit beside the planet, a radius and 9dp further in.</b> That put
        // ten labels in the open ring between the planets and the aspect circle, which is
        // where the house numbers live, and made every body two marks instead of one:
        // "cluttered with the glyphs and the spheres being separated", and exactly right. A
        // label that has to be matched to its planet by eye is worse than no label.
        double phase = PhoneWheel.moonElongation(f);
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            float[] p = w.body(i);
            if (p == null) {
                continue;
            }
            int rad = w.planetRadius(i);
            drawPlanet(c, i, p[0], p[1], rad, i == this.selected, phase);
            glyphOn(c, Bodies.at(i).glyph, p[0], p[1], rad, PhoneWheel.faceColour(i));
        }
        c.restore();
    }

    /**
     * The zodiac and its subdivisions, the desktop's rings in the desktop's order: the lunar
     * mansions at the rim when the reader has them on, the rim degree scale, the decans, the
     * signs, the Egyptian bounds and the inner degree scale the planets sit under.
     *
     * <p><b>Every ring reads the engine, not a table of its own.</b> The bounds come from
     * {@link Dignity#boundEdges} and {@link Dignity#boundRulerOf}, the decans from {@link Zodiac}
     * under the reader's chosen decan ring, the mansions from {@link LunarMansions} - the same
     * calls the desktop's WheelCanvas makes - so a planet the dignity score puts in Mars's
     * bound is standing in the segment this draws for Mars.
     *
     * <p><b>Colour first, glyphs when there is room for them.</b> The bands are the desktop's
     * depths in pixels, and at a phone's density the decan band is under 8dp deep: 96 glyphs
     * in that would be specks. So each segment is a wash in its ruler's colour, which reads at
     * a glance - a run of Saturn bounds closing every sign, an element's three decans - and the
     * glyphs and numbers fade in as the reader pinches in to where they can be read.
     */
    private void drawZodiac(Canvas c, PhoneWheel w) {
        int[] r = w.rings;
        int outer = r[WheelLayout.RING_OUTER];
        int rimTop = r[WheelLayout.RING_MANSION_INNER];
        int decanOuter = r[WheelLayout.RING_DECAN_OUTER];
        int signOuter = r[WheelLayout.RING_SIGN_OUTER];
        int signInner = r[WheelLayout.RING_SIGN_INNER];
        int termInner = r[WheelLayout.RING_TERM_INNER];
        int degreeInner = r[WheelLayout.RING_DEGREE_INNER];
        float glyphs = ringGlyphAlpha();

        if (w.mansions) {
            drawMansions(c, w, outer, rimTop, glyphs);
        }

        // The decans: thirty-six segments, the middle one of each sign a shade stronger so the
        // three read apart even when the triplicity ring gives all three one element's colour.
        boolean chaldean = Settings.DECAN_RING_CHALDEAN.equals(Settings.decanRing());
        this.text.setTextSize((decanOuter - signOuter) * 0.72f);
        for (int n = 0; n < 36; n++) {
            double from = n * 10.0;
            int[] rgb;
            String glyph;
            int fi = chaldean ? Bodies.indexOfName(Zodiac.chaldeanDecanRuler(from + 5.0)) : -1;
            if (fi >= 0) {
                rgb = rgb(PhonePalette.body(fi));
                glyph = Bodies.at(fi).glyph;
            } else {
                // The triplicity ring, and the fallback for a ruler that will not resolve - a
                // gap in the band would read as a rendering fault.
                int si = Zodiac.triplicityDecanSignIndex(n / 3, n % 3 + 1);
                rgb = elementRgb(Zodiac.elementIndex(si));
                glyph = Zodiac.SIGN_GLYPHS[si];
            }
            sector(c, w, from, from + 10.0, signOuter, decanOuter,
                Color.argb(n % 3 == 1 ? 78 : 48, rgb[0], rgb[1], rgb[2]));
            ringGlyph(c, w, glyph, from + 5.0, (decanOuter + signOuter) / 2f, rgb, glyphs);
        }

        // The Egyptian bounds: sixty segments in their rulers' colours.
        this.text.setTextSize((signInner - termInner) * 0.72f);
        for (int sign = 0; sign < 12; sign++) {
            double[] edges = Dignity.boundEdges(sign);
            for (int i = 0; i < edges.length - 1; i++) {
                double from = sign * 30.0 + edges[i];
                double to = sign * 30.0 + edges[i + 1];
                double mid = (from + to) / 2.0;
                int bi = Bodies.indexOfName(Dignity.boundRulerOf(mid % 360.0));
                if (bi < 0) {
                    continue;                           // unresolvable ruler: left blank
                }
                int[] rgb = rgb(PhonePalette.body(bi));
                sector(c, w, from, to, termInner, signInner,
                    Color.argb(i % 2 == 0 ? 62 : 40, rgb[0], rgb[1], rgb[2]));
                if (i > 0) {
                    spoke(c, w, from, termInner, signInner, Color.argb(120, 150, 150, 160), 0.8f);
                }
                ringGlyph(c, w, Bodies.at(bi).glyph, mid, (signInner + termInner) / 2f, rgb,
                    glyphs);
            }
        }

        // The edges of every band, then the decan divisions and the sign boundaries, which run
        // the whole depth of the zodiac so each sign reads as one wedge of all its rings.
        this.line.setColor(Color.rgb(90, 96, 112));
        this.line.setStrokeWidth(1.2f * this.density);
        for (int edge : new int[] {outer, decanOuter, signOuter, signInner, termInner, degreeInner}) {
            c.drawCircle(w.cx, w.cy, edge, this.line);
        }
        if (w.mansions) {
            c.drawCircle(w.cx, w.cy, rimTop, this.line);
        }
        for (int n = 0; n < 36; n++) {
            if (n % 3 != 0) {
                spoke(c, w, n * 10.0, signOuter, decanOuter, Color.argb(150, 120, 126, 140), 0.8f);
            }
        }
        for (int s = 0; s < 12; s++) {
            spoke(c, w, s * 30.0, degreeInner, rimTop, Color.rgb(110, 116, 132), 1.4f);
        }

        // The signs, each glyph in its element's colour.
        this.text.setTextSize((signOuter - signInner) * 0.62f);
        for (int s = 0; s < 12; s++) {
            float[] g = w.point(s * 30.0 + 15.0, (signOuter + signInner) / 2.0);
            this.text.setColor(elementColour(s % 4));
            c.drawText(Zodiac.SIGN_GLYPHS[s], g[0], g[1] + this.text.getTextSize() * 0.35f, this.text);
        }

        // Two degree scales framing the zodiac: one hanging from the rim, one standing on the
        // planets, each marking every degree, the fives longer and the tens longest.
        degreeTicks(c, w, rimTop, rimTop - decanOuter);
        degreeTicks(c, w, termInner, termInner - degreeInner);
        // The degree within its sign at every ten, on the inner scale, between the tens.
        if (glyphs > 0) {
            this.text.setTextSize((termInner - degreeInner) * 0.5f);
            this.text.setColor(Color.argb((int) (255 * glyphs), 150, 158, 170));
            for (int d = 0; d < 360; d += 10) {
                float[] g = w.point(d + 5.0, termInner - (termInner - degreeInner) * 0.68);
                c.drawText(String.valueOf(d % 30), g[0], g[1] + this.text.getTextSize() * 0.35f,
                    this.text);
            }
        }
    }

    /**
     * The 28 lunar mansions in their band at the rim: the Moon's own station washed in the
     * mansion colour, a division at each station's start, and the numbers when zoomed in.
     */
    private void drawMansions(Canvas c, PhoneWheel w, int outer, int inner, float glyphs) {
        int[] ink = rgb(PhonePalette.mansion());
        LunarMansions.Mansion moon = LunarMansions.ofMoon(w.frame);
        if (moon != null) {
            sector(c, w, moon.start, moon.start + LunarMansions.WIDTH, inner, outer,
                Color.argb(140, ink[0], ink[1], ink[2]));
        }
        this.text.setTextSize((outer - inner) * 0.85f);
        for (LunarMansions.Mansion m : LunarMansions.all()) {
            boolean current = moon != null && moon.number == m.number;
            spoke(c, w, m.start, inner, outer,
                Color.argb(current ? 255 : 150, ink[0], ink[1], ink[2]), current ? 1.4f : 0.8f);
            ringGlyph(c, w, String.valueOf(m.number), m.start + LunarMansions.WIDTH / 2.0,
                (outer + inner) / 2f, current ? new int[] {255, 255, 255} : ink, glyphs);
        }
    }

    /**
     * A degree scale: a tick at every degree, hanging {@code depth} from {@code top} toward the
     * centre - the tens the full depth, the fives half, the ones a quarter.
     */
    private void degreeTicks(Canvas c, PhoneWheel w, float top, float depth) {
        for (int d = 0; d < 360; d++) {
            float len = d % 10 == 0 ? depth * 0.9f : d % 5 == 0 ? depth * 0.5f : depth * 0.25f;
            spoke(c, w, d, top - len, top,
                d % 5 == 0 ? Color.rgb(120, 126, 140) : Color.argb(170, 110, 116, 130),
                d % 10 == 0 ? 1.0f : 0.6f);
        }
    }

    /** One radial line, at a longitude, between two radii. */
    private void spoke(Canvas c, PhoneWheel w, double lon, float from, float to, int colour,
            float widthDp) {
        float[] a = w.point(lon, from);
        float[] b = w.point(lon, to);
        this.line.setColor(colour);
        this.line.setStrokeWidth(widthDp * this.density);
        c.drawLine(a[0], a[1], b[0], b[1], this.line);
    }

    /** One segment of a ring - between two longitudes and two radii - filled with a colour. */
    private void sector(Canvas c, PhoneWheel w, double from, double to, float inner, float outer,
            int colour) {
        int steps = Math.max(2, (int) Math.ceil((to - from) / 2.0));
        Path p = new Path();
        for (int k = 0; k <= steps; k++) {
            float[] pt = w.point(from + (to - from) * k / steps, outer);
            if (k == 0) {
                p.moveTo(pt[0], pt[1]);
            } else {
                p.lineTo(pt[0], pt[1]);
            }
        }
        for (int k = steps; k >= 0; k--) {
            float[] pt = w.point(from + (to - from) * k / steps, inner);
            p.lineTo(pt[0], pt[1]);
        }
        p.close();
        this.fill.setShader(null);
        this.fill.setColor(colour);
        c.drawPath(p, this.fill);
    }

    /** A ring's glyph at a longitude and radius, at the strength the zoom allows. */
    private void ringGlyph(Canvas c, PhoneWheel w, String glyph, double lon, float radius,
            int[] rgb, float strength) {
        if (strength <= 0) {
            return;
        }
        float[] g = w.point(lon, radius);
        this.text.setColor(Color.argb((int) (255 * strength), lighten(rgb[0], 0.2),
            lighten(rgb[1], 0.2), lighten(rgb[2], 0.2)));
        c.drawText(glyph, g[0], g[1] + this.text.getTextSize() * 0.35f, this.text);
    }

    /**
     * How strongly the rings' glyphs are drawn: not at all at the wheel's own size, where they
     * would be specks, fading in from 1.6x and whole by 2.4x, where a decan glyph is about
     * 13dp - the size the house numbers are read at.
     */
    private float ringGlyphAlpha() {
        return Math.max(0f, Math.min(1f, (this.zoom - 1.6f) / 0.8f));
    }

    private static int[] rgb(Ink ink) {
        return new int[] {ink.getRed(), ink.getGreen(), ink.getBlue()};
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

    /**
     * A body's glyph, centred on its disc in whichever of black or white reads against it.
     *
     * <p><b>The contrast is computed, not chosen.</b> Ten bodies carry ten face colours, from
     * the Sun's near-white to Pluto's slate, and one fixed glyph colour would be invisible on
     * roughly half of them. Rec. 709 luminance decides: a pale planet takes a dark glyph, a
     * dark one takes a light glyph.
     *
     * <p>It is drawn twice - a stroke of the opposite colour, then the fill - because a disc
     * is shaded and the Moon is drawn in its phase, so one flat colour will always cross a
     * region it cannot be seen against. The outline costs a second draw and removes the whole
     * class of problem.
     *
     * <p>Sized at 1.45 of the radius rather than a fixed dp: the bodies now differ in size
     * (PhoneWheel.sizeFactor), and a fixed glyph would overflow Pluto while swimming in the
     * Sun. At 1.45 the glyph's ink is about the radius tall, which leaves it clear of the rim.
     * Centred on the metrics, not on a guessed offset, so it sits true in the disc.
     */
    private void glyphOn(Canvas c, String glyph, float x, float y, float rad, int[] face) {
        if (glyph == null || glyph.isEmpty() || rad <= 0) {
            return;
        }
        double lum = (0.2126 * face[0] + 0.7152 * face[1] + 0.0722 * face[2]) / 255.0;
        int ink = lum > 0.55 ? Color.rgb(16, 18, 24) : Color.rgb(246, 248, 252);
        int edge = lum > 0.55 ? Color.argb(170, 255, 255, 255) : Color.argb(170, 0, 0, 0);
        this.text.setTextSize(rad * 1.45f);
        Paint.FontMetrics fm = this.text.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) / 2f;
        this.text.setStyle(Paint.Style.STROKE);
        this.text.setStrokeWidth(Math.max(1f, rad * 0.14f));
        this.text.setColor(edge);
        c.drawText(glyph, x, baseline, this.text);
        this.text.setStyle(Paint.Style.FILL);
        this.text.setColor(ink);
        c.drawText(glyph, x, baseline, this.text);
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
