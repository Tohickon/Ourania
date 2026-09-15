package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * The Uranian dial: the zodiac folded onto a modulus so that a whole aspect family becomes one
 * conjunction.
 *
 * <p>Master list E: "Modulo-22.5 dial (16th harmonic) for semi-square / sesquiquadrate family
 * contacts". On a 90&deg; dial every multiple of 90 - conjunction, square, opposition - lands on
 * the same spot. On a 45&deg; dial the semi-square and the sesquiquadrate join them, and on a
 * 22.5&deg; dial the 22.5 family too. Hamburg School and Ebertin work is done on these dials,
 * because a chart's hard contacts and midpoint pictures, scattered round a 360&deg; wheel, stand
 * in a line under one pointer.
 *
 * <p><b>A midpoint and its opposite are the same point on every one of these dials</b>, since 180
 * is a multiple of 90, 45 and 22.5. So the near-or-far question the wheel's midpoint tree has to
 * settle does not arise here, and each pair contributes one dial position.
 */
public final class Dial {

    private Dial() { }

    /** The dials the screen offers, in zodiac degrees per turn. */
    public static final double[] MODULI = {90.0, 45.0, 22.5};

    /** One thing on the dial: a planet or point, or the midpoint of two. */
    public static final class Point {
        public final String name;
        /** The first of a midpoint's two points; null for a single point. */
        public final String a;
        public final String b;
        /** Zodiac longitude, 0-360. For a midpoint, the nearer midpoint. */
        public final double lon;

        Point(String name, String a, String b, double lon) {
            this.name = name;
            this.a = a;
            this.b = b;
            this.lon = lon;
        }

        public boolean isMidpoint() {
            return this.a != null;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static Point point(String name, double lon) {
        return new Point(name, null, null, Zodiac.normalise(lon));
    }

    /** The midpoint of two points, named "A/B" the way a midpoint tree writes it. */
    public static Point midpoint(Point x, Point y) {
        double d = Zodiac.normalise(y.lon - x.lon);
        double near = d <= 180.0 ? x.lon + d / 2.0 : x.lon - (360.0 - d) / 2.0;
        return new Point(x.name + "/" + y.name, x.name, y.name, Zodiac.normalise(near));
    }

    /** Where a longitude sits on a dial, 0 up to the modulus. */
    public static double folded(double lon, double modulus) {
        double v = Zodiac.normalise(lon) % modulus;
        return v < 0 ? v + modulus : v;
    }

    /** The dial's own angle for a longitude, 0-360, so the dial can be drawn as a circle. */
    public static double angle(double lon, double modulus) {
        return folded(lon, modulus) / modulus * 360.0;
    }

    /**
     * How far apart two longitudes are on a dial, in zodiac degrees, 0 up to half the modulus.
     * Zero means the two are an exact member of the modulus's aspect family.
     */
    public static double separation(double lonA, double lonB, double modulus) {
        double d = folded(lonA - lonB, modulus);
        return Math.min(d, modulus - d);
    }

    /** The chart's planets and angles as dial points. */
    public static List<Point> points(ChartFrame f) {
        List<Point> out = new ArrayList<>();
        if (f == null) {
            return out;
        }
        boolean asc = false;
        boolean mc = false;
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b == null || !b.ok || Double.isNaN(b.lon)) {
                continue;
            }
            // The derived half of an axis sits on its partner on every dial: the Descendant is
            // the Ascendant, the IC the Midheaven, the South Node the North. Listing both would
            // put every picture on the dial twice.
            if (Bodies.oppositeOf(i) >= 0) {
                continue;
            }
            asc |= "Ascendant".equals(b.name);
            mc |= "MC".equals(b.name);
            out.add(point(b.name, b.lon));
        }
        if (!asc && !Double.isNaN(f.asc)) {
            out.add(point("Ascendant", f.asc));
        }
        if (!mc && !Double.isNaN(f.mc)) {
            out.add(point("MC", f.mc));
        }
        return out;
    }

    /** Every pair's midpoint, once each. */
    public static List<Point> midpoints(List<Point> points) {
        List<Point> out = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                out.add(midpoint(points.get(i), points.get(j)));
            }
        }
        return out;
    }

    /** What stands within orb of a place on the dial, nearest first. */
    public static List<Point> near(double lon, List<Point> candidates, double modulus, double orb) {
        List<Point> out = new ArrayList<>();
        for (Point p : candidates) {
            if (separation(p.lon, lon, modulus) <= orb) {
                out.add(p);
            }
        }
        out.sort((x, y) -> Double.compare(separation(x.lon, lon, modulus), separation(y.lon, lon, modulus)));
        return out;
    }

    /** One planetary picture: a point, and the points and midpoints standing with it on the dial. */
    public static final class Picture {
        public final Point focus;
        public final List<Point> members;

        Picture(Point focus, List<Point> members) {
            this.focus = focus;
            this.members = members;
        }
    }

    /**
     * Each point with everything that stands on it on this dial: its hard contacts with other
     * points, and the midpoints it occupies - "Sun = Mars/Saturn" - leaving out the midpoints the
     * point itself is half of, which stand on it trivially at half their span.
     */
    public static List<Picture> pictures(List<Point> points, List<Point> midpoints, double modulus, double orb) {
        List<Picture> out = new ArrayList<>();
        for (Point focus : points) {
            List<Point> members = new ArrayList<>();
            for (Point p : points) {
                if (p != focus && separation(p.lon, focus.lon, modulus) <= orb) {
                    members.add(p);
                }
            }
            for (Point m : midpoints) {
                if (focus.name.equals(m.a) || focus.name.equals(m.b)) {
                    continue;
                }
                if (separation(m.lon, focus.lon, modulus) <= orb) {
                    members.add(m);
                }
            }
            if (!members.isEmpty()) {
                members.sort((x, y) -> Double.compare(separation(x.lon, focus.lon, modulus),
                    separation(y.lon, focus.lon, modulus)));
                out.add(new Picture(focus, members));
            }
        }
        return out;
    }
}
