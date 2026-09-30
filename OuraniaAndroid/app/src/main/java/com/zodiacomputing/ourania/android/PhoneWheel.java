package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.gui.Settings;
import com.zodiacomputing.ourania.gui.WheelLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything goes on the phone's wheel, in pixels (M4).
 *
 * <p><b>The desktop's layout, not a second one.</b> The ring radii, the body band, how crowded
 * bodies spread across it and the circle the aspect lines are drawn on all come from
 * {@link WheelLayout} - the same arithmetic the desktop wheel is painted and clicked from - at
 * the phone's size. The phone paints with Android's canvas rather than AWT, so the painting is
 * {@code WheelView}'s own; where things are is not.
 *
 * <p>No Android in here, like {@link PhoneChart}: {@code PhoneWheelTest} checks the geometry
 * and the tap target on the JVM.
 *
 * <p>The desktop's convention for angle: longitude L is drawn at {@code 180 + pin - L} degrees,
 * screen y down, with the Ascendant pinned to the left - so houses run counterclockwise from
 * the Ascendant and the IC is at the bottom, as a chart is read.
 */
final class PhoneWheel {

    /** The planets the phone draws: the ten, as PhoneChart lists them. */
    static final int PLANETS = PhoneChart.PLANETS;

    final int width;
    final int height;
    final int cx;
    final int cy;
    /** {@link WheelLayout#ringRadii}, one outer ring (none open), at this size. */
    final int[] rings;
    /** The top and floor of the natal band. */
    final int natalTop;
    final int natalFloor;
    /** The circle natal aspect lines are drawn on. */
    final int aspectDisc;
    /** The longitude drawn at the left: the Ascendant, or 0 Aries without a birth time. */
    final double pin;
    /** Each planet's radius, 0 for one not drawn. */
    final int[] bodyRadius;
    final ChartFrame frame;

    private PhoneWheel(ChartFrame frame, int width, int height) {
        this.frame = frame;
        this.width = width;
        this.height = height;
        this.cx = width / 2;
        this.cy = height / 2;
        this.rings = WheelLayout.ringRadii(width, height, false);
        this.natalTop = WheelLayout.bodyBase(this.rings, Settings.bodyRing());
        this.natalFloor = this.natalTop - WheelLayout.natalBandDepth(Math.max(1, this.natalTop));
        this.aspectDisc = WheelLayout.aspectDiscs(this.natalFloor)[0];
        this.pin = frame.timeUnknown ? 0.0 : frame.asc;
        double[] lon = new double[Bodies.count()];
        boolean[] drawn = new boolean[Bodies.count()];
        for (int i = 0; i < PLANETS && i < frame.bodies.length; i++) {
            ChartFrame.Body b = frame.bodies[i];
            if (b != null && b.ok) {
                lon[i] = b.lon;
                drawn[i] = true;
            }
        }
        int[] radii = WheelLayout.bandRadii(lon, drawn, this.natalTop, this.natalFloor,
            WheelLayout.NATAL_EDGE, WheelLayout.NATAL_SPACING);
        this.bodyRadius = new int[Bodies.count()];
        for (int i = 0; i < radii.length && i < this.bodyRadius.length; i++) {
            this.bodyRadius[i] = drawn[i] ? radii[i] : 0;
        }
    }

    static PhoneWheel of(ChartFrame frame, int width, int height) {
        return new PhoneWheel(frame, width, height);
    }

    /** The screen angle of a longitude, in radians. */
    double angle(double lon) {
        return Math.toRadians(180.0 + this.pin - lon);
    }

    /** The point at a longitude and radius. */
    float[] point(double lon, double radius) {
        double a = this.angle(lon);
        return new float[] {(float) (this.cx + radius * Math.cos(a)),
            (float) (this.cy + radius * Math.sin(a))};
    }

    /** A planet's centre, or null when it is not drawn. */
    float[] body(int i) {
        if (i < 0 || i >= this.bodyRadius.length || this.bodyRadius[i] <= 0) {
            return null;
        }
        return this.point(this.frame.bodies[i].lon, this.bodyRadius[i]);
    }

    /** How big a planet's disc is drawn, from the desktop's glyph table. */
    static int glyphRadius(int i) {
        return WheelLayout.natalSize(i).radius;
    }

    /**
     * The planet under a tap, nearest first, within {@code grab} pixels of its edge; or -1.
     * Nearest wins so two planets close together are each still reachable.
     */
    int bodyAt(float x, float y, float grab) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < PLANETS; i++) {
            float[] p = this.body(i);
            if (p == null) {
                continue;
            }
            double d = Math.hypot(x - p[0], y - p[1]);
            if (d <= glyphRadius(i) + grab && d < bestDistance) {
                best = i;
                bestDistance = d;
            }
        }
        return best;
    }

    /** One aspect line: the two planets and the aspect between them. */
    static final class Line {
        final int a;
        final int b;
        final Aspects.Type type;

        Line(int a, int b, Aspects.Type type) {
            this.a = a;
            this.b = b;
            this.type = type;
        }
    }

    /**
     * The natal aspects between the planets, at the desktop's natal orbs - {@link Aspects}
     * decides, the same call the desktop wheel and grid make.
     */
    List<Line> aspects() {
        List<Line> out = new ArrayList<>();
        for (int a = 0; a < PLANETS; a++) {
            for (int b = a + 1; b < PLANETS; b++) {
                if (this.bodyRadius[a] <= 0 || this.bodyRadius[b] <= 0) {
                    continue;
                }
                ChartFrame.Body x = this.frame.bodies[a];
                ChartFrame.Body y = this.frame.bodies[b];
                double sep = Math.abs(x.lon - y.lon);
                if (sep > 180.0) {
                    sep = 360.0 - sep;
                }
                Aspects.Type t = Aspects.typeOf(sep, x.name, y.name, Aspects.Profile.NATAL);
                if (t != null) {
                    out.add(new Line(a, b, t));
                }
            }
        }
        return out;
    }
}
