package com.zodiacomputing.ourania.gui;

/**
 * The celestial globe: the wheel's rings as concentric shells, and the camera that looks at
 * them.
 *
 * <b>From David's Nodus prototype, which is a Three.js app and could not be ported.</b> What
 * ports is the idea, and the idea is a good one: a chart is not flat, and the thing the flat
 * wheel is a projection of is a set of nested spheres with the reader inside them. Natal on the
 * inner shell, a partner outside it, the sky on the outer skin, the zodiac as a band around the
 * equator. Every ring the wheel opens becomes a shell, and the same Bloom that widens a band
 * moves a shell outward.
 *
 * <b>Hand-rolled rather than a 3D library.</b> This app has one dependency, Swiss Ephemeris,
 * and adding a native GL binding to draw wireframe spheres and line segments would be a large
 * change to how it builds and runs for something Java2D does adequately. The scene has no
 * textures, no lights and no meshes to speak of - it is points on great circles, lines between
 * them, and glyphs that always face the reader. A projection matrix and a depth sort is the
 * whole of it.
 *
 * <b>Everything here is pure.</b> No Swing, no state beyond the camera, so the arithmetic can
 * be asserted without painting anything - which is the only way the painter and the hit test
 * will be able to agree about where a body is, the same way Geometry does for the flat wheel.
 */
final class Globe {

    /**
     * Shell radii, in world units, innermost first.
     *
     * <b>Named for what they carry, not for their size</b>, so the layout can be read against
     * the flat wheel's ring chain: natal inside, partner around it, the sky outermost, with the
     * zodiac band between the bodies and the sky. The proportions are from the prototype, where
     * they were arrived at by looking; keeping them means the two views agree about what is
     * near and what is far.
     */
    static final double SHELL_CORE = 0.90;
    static final double SHELL_NATAL = 1.20;
    static final double SHELL_PARTNER = 1.50;
    /**
     * The sky ring, now inside the zodiac rather than outside it.
     *
     * <b>The zodiac has to enclose everything it measures.</b> The sky rode the outer skin,
     * beyond the sign plane, so a transiting body sat outside the band that says which sign it
     * is in - which is the one thing the zodiac is for. Every body ring is inside the plane
     * now: natal, partner and sky, in that order, with the zodiac wrapped around all three.
     */
    static final double SHELL_SKY = 1.80;
    static final double SHELL_HOUSE = 1.92;
    /** Egyptian bounds, just inside the signs - the flat wheel's order, kept. */
    static final double SHELL_BOUND = 1.99;
    static final double SHELL_SIGN_INNER = 2.08;
    static final double SHELL_SIGN_OUTER = 2.30;
    /** Decans, just outside the signs, as on the flat wheel. */
    static final double SHELL_DECAN = 2.38;
    static final double SHELL_TICK = 2.42;

    /**
     * How far above and below the equator a filled shell reaches, in radians of latitude.
     *
     * <b>All the way to the poles, so the sphere is closed.</b> This stopped at 1.02 radians
     * on the reasoning that twelve translucent wedges meeting at one point would stack into a
     * black cap - which is true of wedges drawn as quads that all cover the pole, and not true
     * of wedges that taper. A slice of a sphere converges to nothing at the pole, so the top
     * row of cells is triangles that tile the cap exactly and overlap nowhere. The old value
     * left the globe with a hole at each end, which read as unfinished rather than as
     * restraint.
     */
    static final double FILL_SPAN = Math.PI / 2;

    /** How far up the shell one collision level lifts a body. */
    static final double STACK_STEP = 0.085;

    /**
     * How far the partner and sky rings are tilted out of the natal plane, in radians.
     *
     * <b>Tilted about the Ascendant axis, so all three rings still cross there.</b> Every ring
     * is on the ecliptic in fact - this is presentation, not astronomy - so the tilt has to be
     * the kind that gives up nothing: rotating about the line through the Ascendant and the
     * Descendant leaves both of those fixed on every ring, and the three great circles meet at
     * the two points a reader uses to orient themselves. Opposed, so the partner rides above
     * the horizon where the sky rides below it and neither hides the other.
     *
     * At zero this is the old behaviour exactly, which is what lets the flat wheel and the
     * natal ring go on agreeing.
     */
    static final double INCLINE_PARTNER = 0.48;
    static final double INCLINE_SKY = -0.48;

    /** Half-height of a meridian arc, in radians of latitude. Matches the prototype's 0.92. */
    static final double MERIDIAN_SPAN = 0.92;

    /** A point projected to the panel, with the depth that decides what covers what. */
    static final class Projected {
        final double x;
        final double y;
        /** Distance from the camera. Larger is further away, so paint larger first. */
        final double depth;
        /** False when the point is behind the camera and must not be drawn at all. */
        final boolean visible;

        Projected(double x, double y, double depth, boolean visible) {
            this.x = x;
            this.y = y;
            this.depth = depth;
            this.visible = visible;
        }
    }

    /** Rotation about the vertical axis, radians. The reader drags this. */
    double yaw;
    /** Tilt toward the reader, radians. Clamped so the globe never turns inside out. */
    double pitch;
    /** Camera distance in world units. The reader scrolls this. */
    double distance = 5.6;

    /**
     * Focal length as a multiple of the panel's smaller side.
     *
     * <b>Set from the outermost thing drawn, not chosen.</b> At 1.45 the outermost shell
     * projected to 660 pixels of a 550-pixel half-frame and the globe ran off every edge -
     * which looks like a broken camera rather than like a zoom. The far edge is now the degree
     * scale outside the decans, further out than the old sky ring, so this came down with it.
     */
    private static final double ZOOM = 0.82;

    /** The tilt beyond which the poles cross the view and the scene reads as inverted. */
    static final double MAX_PITCH = 1.25;

    Globe() {
        this.yaw = 0.0;
        // <b>Nearly edge-on, at about eighteen degrees.</b> This was fifty for a while, on
        // the reasoning that a low angle made the shells project to ellipses flat enough that
        // the scene read as a squashed wheel rather than a sphere. That was true when the
        // shells were wireframes: a circle seen edge-on is a line, and a dozen of them are a
        // scribble. It stopped being true when the signs became coloured wedges over the
        // surface - a sphere with shading reads as a sphere from any angle, and the low camera
        // is what makes the equatorial ring a ring rather than a disc seen from above.
        this.pitch = 0.32;
    }

    /** Applies a drag, in pixels, and keeps the camera somewhere a reader can understand. */
    void drag(double dx, double dy, int panelWidth) {
        int w = Math.max(1, panelWidth);
        this.yaw += (dx / w) * Math.PI * 2.0;
        this.pitch = clamp(this.pitch + (dy / w) * Math.PI * 2.0, -MAX_PITCH, MAX_PITCH);
    }

    /** Applies a scroll. Bounded so the reader cannot end up inside the core or in deep space. */
    void zoom(double ticks) {
        this.distance = clamp(this.distance + ticks * 0.35, 3.2, 12.0);
    }

    /**
     * World point to panel point.
     *
     * <b>Yaw then pitch then perspective, in that order.</b> Any other order tilts about an
     * axis that has already been turned, which reads as the globe wobbling rather than turning
     * - the classic gimbal complaint, and the reason this is one method rather than a matrix
     * assembled at each call site.
     */
    Projected project(double x, double y, double z, int width, int height) {
        double cy = Math.cos(this.yaw);
        double sy = Math.sin(this.yaw);
        double xr = x * cy + z * sy;
        double zr = -x * sy + z * cy;

        double cp = Math.cos(this.pitch);
        double sp = Math.sin(this.pitch);
        double yr = y * cp - zr * sp;
        double zc = y * sp + zr * cp + this.distance;

        // Anything at or behind the lens has no honest position on the panel.
        if (zc <= 0.08) {
            return new Projected(0, 0, Double.MAX_VALUE, false);
        }
        double focal = Math.min(width, height) * ZOOM;
        double s = focal / zc;
        return new Projected(width / 2.0 + xr * s, height / 2.0 - yr * s, zc, true);
    }

    /**
     * Ecliptic longitude to a point on a shell.
     *
     * <b>The origin longitude sits on the left, as the Ascendant does on the flat wheel.</b>
     * The two views have to agree about where a degree is or turning the globe to find a body
     * would teach the reader nothing about the chart they already know.
     *
     * @param lon    ecliptic longitude, degrees
     * @param origin the longitude pinned to the left of the view, normally the Ascendant
     * @param radius the shell
     * @param y      height above the shell's equator, for stacked bodies
     */
    static double[] onShell(double lon, double origin, double radius, double y) {
        return onShell(lon, origin, radius, y, 0.0);
    }

    /**
     * As above, on a ring tilted out of the horizontal.
     *
     * <b>Rotated about the x axis, which is the Ascendant-Descendant line.</b> Those two
     * points are at -x and +x, so they are exactly the points an x rotation leaves alone: a
     * tilted ring still crosses the horizontal one where the reader is looking to orient
     * themselves, and only the quarters in between rise and fall.
     */
    static double[] onShell(double lon, double origin, double radius, double y,
                            double inclination) {
        double t = Math.toRadians(lon - origin);
        // The equatorial radius shrinks as a body rides up the shell, so a stacked body stays
        // on the sphere instead of floating off it.
        double lift = Math.max(-radius, Math.min(radius, y));
        double ring = Math.sqrt(Math.max(0.0, radius * radius - lift * lift));
        double x = -ring * Math.cos(t);
        double yy = lift;
        double z = ring * Math.sin(t);
        if (inclination == 0.0) {
            return new double[] {x, yy, z};
        }
        double c = Math.cos(inclination);
        double sn = Math.sin(inclination);
        return new double[] {x, yy * c - z * sn, yy * sn + z * c};
    }

    /** The full circle of a shell's equator, as world points. */
    static double[][] equator(double origin, double radius, int segments) {
        return equator(origin, radius, segments, 0.0);
    }

    /** As above, on a ring tilted out of the horizontal. */
    static double[][] equator(double origin, double radius, int segments, double inclination) {
        int n = Math.max(3, segments);
        double[][] pts = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            pts[i] = onShell(origin + (360.0 * i) / n, origin, radius, 0.0, inclination);
        }
        return pts;
    }

    /**
     * A meridian arc through one longitude, from below the equator to above it.
     *
     * Stops short of the poles: a full meridian converges with every other one at two points,
     * and a dozen of them meeting reads as a knot rather than as a sphere.
     */
    static double[][] meridian(double lon, double origin, double radius, int segments) {
        return meridian(lon, origin, radius, segments, MERIDIAN_SPAN);
    }

    /**
     * As above, over a chosen half-height.
     *
     * <b>Pole to pole for a boundary, short of it for decoration.</b> A house cusp and a sign
     * boundary are real divisions of the whole sky and stop nowhere, so they are drawn at
     * PI/2 and meet at the poles the way they actually do. A wireframe meridian is there to
     * suggest a surface, and a dozen of those converging is a knot.
     */
    static double[][] meridian(double lon, double origin, double radius, int segments,
                               double span) {
        int n = Math.max(2, segments);
        double t = Math.toRadians(lon - origin);
        double[][] pts = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double phi = -span + (2.0 * span * i) / n;
            double c = Math.cos(phi);
            pts[i] = new double[] {
                -radius * Math.cos(t) * c, radius * Math.sin(phi), radius * Math.sin(t) * c};
        }
        return pts;
    }

    /**
     * Which level each body sits at when longitudes crowd, by index.
     *
     * <b>The same rule as the flat wheel's radialLevels, on a different axis.</b> There a
     * crowded body steps inward; here it steps up the shell. Written separately because the
     * flat version resolves in pixels against a radius that changes per ring, and folding both
     * into one method would mean a signature that serves neither - but the behaviour is
     * deliberately identical, and asserted to be.
     *
     * @param minDeg how close two longitudes must be before the later one steps up
     */
    static int[] stackLevels(double[] lon, boolean[] valid, double minDeg) {
        int n = lon.length;
        int[] level = new int[n];
        Integer[] order = new Integer[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (valid[i]) {
                order[count++] = i;
            }
        }
        if (count == 0) {
            return level;
        }
        Integer[] live = new Integer[count];
        System.arraycopy(order, 0, live, 0, count);
        java.util.Arrays.sort(live, (a, b) -> Double.compare(norm(lon[a]), norm(lon[b])));

        int cluster = 0;
        for (int i = 1; i < count; i++) {
            double gap = separation(lon[live[i - 1]], lon[live[i]]);
            cluster = gap < minDeg ? cluster + 1 : 0;
            level[live[i]] = cluster;
        }
        // The list wraps: the first and last longitudes are neighbours across 0 degrees, and
        // without this the densest cluster in the chart silently splits at the origin.
        if (count > 2 && separation(lon[live[0]], lon[live[count - 1]]) < minDeg) {
            level[live[0]] = level[live[count - 1]] + 1;
        }
        return level;
    }

    /**
     * Where a band traced from pole to pole has to be cut so that no piece overlaps itself.
     *
     * <b>A lune is not one shape on screen.</b> A band between two longitudes runs from the
     * south pole to the north, which means half of it is on the near face and half on the far
     * one, and the two halves land on top of each other in the projection. Filled as a single
     * polygon that is a shape crossing itself, and an even-odd fill - which is what Java's
     * fillPolygon does - subtracts the overlap instead of adding it. The sphere came out with
     * black notches bitten out of it around the pole, where the far half of a band lay across
     * the near half of the same band.
     *
     * So the band is cut where it crosses the limb and each side is filled separately. That
     * also mends the shading and the depth sort, which had been averaging a near half and a
     * far half into one number that described neither.
     *
     * @param mid   the mean depth of each segment of the trace, in order
     * @param limit the depth of the sphere's centre; a segment nearer than this faces the
     *              reader, one beyond it is on the far face
     * @param edgeA the screen y of one edge of the band, one value per vertex
     * @param edgeB the screen y of the other
     * @return vertex indices bounding each run: always starts at 0 and ends at mid.length, so
     *         neighbouring runs share their boundary vertex and leave no gap between them
     */
    static int[] bandRuns(double[] mid, double limit, double[] edgeA, double[] edgeB) {
        int n = mid.length;
        if (n == 0) {
            return new int[] {0};
        }
        boolean[] cut = new boolean[n + 1];
        cut[0] = true;
        cut[n] = true;
        for (int j = 1; j < n; j++) {
            if ((mid[j] < limit) != (mid[j - 1] < limit)) {
                cut[j] = true;
            }
        }
        // <b>And wherever an edge turns back on itself.</b> Cutting at the limb alone was not
        // enough, because a meridian's projection is not monotone even within one face: it
        // climbs to an apex short of the pole and comes back down, so the strip folds across
        // itself near the top of the sphere and the fold cancelled in exactly the same way.
        // That was the slit left standing after the notches were gone - a hole the width of a
        // band, running down the middle of the crown, that got thinner as the bands got
        // narrower and never went away. A run whose edges only ever move one way in screen y
        // meets every scan line once, which is what makes the polygon simple.
        for (double[] edge : new double[][] {edgeA, edgeB}) {
            for (int i = 1; i < n; i++) {
                if ((edge[i] - edge[i - 1]) * (edge[i + 1] - edge[i]) < 0) {
                    cut[i] = true;
                }
            }
        }
        int count = 0;
        for (boolean b : cut) {
            if (b) {
                count++;
            }
        }
        int[] out = new int[count];
        int k = 0;
        for (int i = 0; i <= n; i++) {
            if (cut[i]) {
                out[k++] = i;
            }
        }
        return out;
    }

    /** Shortest angular distance between two longitudes, 0 to 180. */
    static double separation(double a, double b) {
        double d = Math.abs(norm(a) - norm(b)) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    private static double norm(double d) {
        double v = d % 360.0;
        return v < 0 ? v + 360.0 : v;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
