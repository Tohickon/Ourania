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
    /** What {@link #bodyAt} adds to a planet's index when it is on the sky's ring (M7). */
    static final int SKY = 100;

    /** {@link WheelLayout#ringRadii} at this size, with the sky's band open when it is shown. */
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
    /** The sky on the ring inside the signs, or null for the birth chart alone (M7). */
    final ChartFrame sky;
    /** Each sky planet's radius, 0 for one not drawn; empty without a sky. */
    final int[] skyRadius;

    private PhoneWheel(ChartFrame frame, ChartFrame sky, int width, int height) {
        this.frame = frame;
        this.sky = sky;
        this.width = width;
        this.height = height;
        this.cx = width / 2;
        this.cy = height / 2;
        // The desktop's transit band: with it open, the natal band moves in to make room, and
        // the sky's planets ride between the signs and the natal planets, as on the desktop.
        this.rings = WheelLayout.ringRadii(width, height, sky != null);
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
        this.skyRadius = new int[sky == null ? 0 : Bodies.count()];
        if (sky != null) {
            double[] skyLon = new double[Bodies.count()];
            boolean[] skyDrawn = new boolean[Bodies.count()];
            for (int i = 0; i < PLANETS && i < sky.bodies.length; i++) {
                ChartFrame.Body b = sky.bodies[i];
                if (b != null && b.ok) {
                    skyLon[i] = b.lon;
                    skyDrawn[i] = true;
                }
            }
            int[] sr = WheelLayout.bandRadii(skyLon, skyDrawn,
                this.rings[WheelLayout.RING_TRANSIT], this.rings[WheelLayout.RING_BODY_TOP]);
            for (int i = 0; i < sr.length && i < this.skyRadius.length; i++) {
                this.skyRadius[i] = skyDrawn[i] ? sr[i] : 0;
            }
        }
    }

    static PhoneWheel of(ChartFrame frame, int width, int height) {
        return new PhoneWheel(frame, null, width, height);
    }

    /** The birth chart with the sky on the ring outside its planets (M7). */
    static PhoneWheel of(ChartFrame frame, ChartFrame sky, int width, int height) {
        return new PhoneWheel(frame, sky, width, height);
    }

    /** A sky planet's centre, or null when there is no sky or it is not drawn. */
    float[] skyBody(int i) {
        if (i < 0 || i >= this.skyRadius.length || this.skyRadius[i] <= 0) {
            return null;
        }
        return this.point(this.sky.bodies[i].lon, this.skyRadius[i]);
    }

    /** How big a sky planet is drawn: the desktop's transit glyph, a third larger. */
    static int skyPlanetRadius(int i) {
        return Math.round(WheelLayout.transitSize(i).radius * 1.35f);
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
     * How big a planet is drawn on the phone: the desktop's glyph radius, seven tenths larger. The
     * desktop's beads carry a glyph; the phone's are pictures of the planets, which need the
     * room to read as Saturn's rings or Jupiter's belts (reported 30 Sep: "basic glyphs in
     * circles"). The band spreads crowded planets by the same spacing either way.
     */
    int planetRadius(int i) {
        return Math.round(glyphRadius(i) * 1.7f);
    }

    /** How a planet is pictured: the globe's faces (GlobeRenderer.drawPlanet), and the Moon's. */
    enum Face { SUN, MOON, BANDED, RINGED, PLAIN }

    static Face face(int i) {
        switch (Bodies.at(i).name) {
            case "Sun": return Face.SUN;
            case "Moon": return Face.MOON;
            case "Jupiter": return Face.BANDED;
            case "Saturn": return Face.RINGED;
            default: return Face.PLAIN;
        }
    }

    /** A planet's colour as {r, g, b}: the globe's, warm to cold, and a pale grey Moon. */
    static int[] faceColour(int i) {
        switch (Bodies.at(i).name) {
            case "Sun": return new int[] {255, 196, 84};
            case "Moon": return new int[] {214, 216, 222};
            case "Mercury": return new int[] {178, 172, 160};
            case "Venus": return new int[] {226, 200, 148};
            case "Mars": return new int[] {198, 96, 66};
            case "Jupiter": return new int[] {206, 176, 138};
            case "Saturn": return new int[] {214, 194, 146};
            case "Uranus": return new int[] {150, 206, 208};
            case "Neptune": return new int[] {104, 138, 214};
            case "Pluto": return new int[] {164, 146, 132};
            default: return new int[] {190, 190, 196};
        }
    }

    /**
     * Where a house's number goes: halfway through its house, in the open ring between the
     * aspect circle and the planets. It sat just inside the aspect circle among the lines,
     * which on a phone read as cramped (reported 30 Sep); the desktop puts its numbers in this
     * same open ring, just inside the natal band.
     */
    float[] housePoint(int h) {
        double from = this.frame.cusps[h];
        double to = this.frame.cusps[h == 12 ? 1 : h + 1];
        double mid = from + (((to - from) % 360.0 + 360.0) % 360.0) / 2.0;
        return this.point(mid, this.houseRadius());
    }

    /** The radius the house numbers sit on: two thirds of the way out to the planets. */
    double houseRadius() {
        return this.aspectDisc + (this.natalFloor - this.aspectDisc) * 0.62;
    }

    /** The house numbers' text size in pixels: 15dp, and never wider than their ring. */
    float houseTextSize(float density) {
        return Math.min(15f * density, (this.natalFloor - this.aspectDisc) * 0.4f);
    }

    /**
     * The planet under a tap, nearest first, within {@code grab} pixels of its edge; or -1.
     * A planet on the sky's ring answers {@link #SKY} plus its index.
     * Nearest wins so two planets close together are each still reachable.
     */
    int bodyAt(float x, float y, float grab) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < PLANETS; i++) {
            float[] p = this.body(i);
            if (p != null) {
                double d = Math.hypot(x - p[0], y - p[1]);
                if (d <= this.planetRadius(i) + grab && d < bestDistance) {
                    best = i;
                    bestDistance = d;
                }
            }
            float[] q = this.skyBody(i);
            if (q != null) {
                double d = Math.hypot(x - q[0], y - q[1]);
                if (d <= skyPlanetRadius(i) + grab && d < bestDistance) {
                    best = SKY + i;
                    bestDistance = d;
                }
            }
        }
        return best;
    }

    /** A contact between a planet on the outer ring and one of the chart's own, by index. */
    static final class Cross {
        final int outer;
        final int inner;
        final Aspects.Type type;

        Cross(int outer, int inner, Aspects.Type type) {
            this.outer = outer;
            this.inner = inner;
            this.type = type;
        }
    }

    /**
     * The sky's contacts as lines: each transit between two of the ten planets. A contact to
     * an angle has no planet to join and is left to the reading.
     */
    static List<Cross> crosses(List<com.zodiacomputing.ourania.astro.Transits.Hit> hits,
            ChartFrame sky, ChartFrame natal) {
        List<Cross> out = new ArrayList<>();
        for (com.zodiacomputing.ourania.astro.Transits.Hit h : hits) {
            int t = index(sky, h.transiting);
            int n = index(natal, h.natal);
            if (t >= 0 && n >= 0) {
                out.add(new Cross(t, n, h.type));
            }
        }
        return out;
    }

    /** Where a named planet is among the first ten of a frame, or -1. */
    static int index(ChartFrame f, String name) {
        for (int i = 0; i < PLANETS && i < f.bodies.length; i++) {
            if (f.bodies[i] != null && name.equals(f.bodies[i].name)) {
                return i;
            }
        }
        return -1;
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
     * The natal aspects between the drawn planets, at the desktop's natal orbs - {@link Aspects}
     * decides, the same call the desktop wheel and grid make.
     */
    List<Line> aspects() {
        List<Line> out = new ArrayList<>();
        for (Line l : aspectsOf(this.frame, PLANETS)) {
            if (this.bodyRadius[l.a] > 0 && this.bodyRadius[l.b] > 0) {
                out.add(l);
            }
        }
        return out;
    }

    /**
     * The natal aspects among a chart's first {@code count} bodies - the wheel's and the
     * reading's - leaving out the aspect types the reader has switched off (M9), which is the
     * same setting, {@link Settings#loadAspectSelection}, the desktop's wheel and grid obey.
     */
    static List<Line> aspectsOf(ChartFrame frame, int count) {
        List<Line> out = new ArrayList<>();
        boolean[] shown = Settings.loadAspectSelection();
        int n = Math.min(count, frame.bodies.length);
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                ChartFrame.Body x = frame.bodies[a];
                ChartFrame.Body y = frame.bodies[b];
                if (x == null || y == null || !x.ok || !y.ok) {
                    continue;
                }
                double sep = Math.abs(x.lon - y.lon);
                if (sep > 180.0) {
                    sep = 360.0 - sep;
                }
                Aspects.Type t = Aspects.typeOf(sep, x.name, y.name, Aspects.Profile.NATAL);
                if (t != null && (t.ordinal() >= shown.length || shown[t.ordinal()])) {
                    out.add(new Line(a, b, t));
                }
            }
        }
        return out;
    }
}
