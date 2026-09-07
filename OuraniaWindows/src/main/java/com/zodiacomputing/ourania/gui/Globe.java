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
    static final double SHELL_NATAL = 1.26;
    static final double SHELL_PARTNER = 1.56;
    static final double SHELL_HOUSE = 1.72;
    /** Egyptian bounds, just inside the signs - the flat wheel's order, kept. */
    static final double SHELL_BOUND = 1.86;
    static final double SHELL_SIGN_INNER = 1.94;
    static final double SHELL_SIGN_OUTER = 2.16;
    /** Decans, just outside the signs, as on the flat wheel. */
    static final double SHELL_DECAN = 2.24;
    static final double SHELL_TICK = 2.28;
    static final double SHELL_SKY = 2.32;

    /**
     * How far above and below the equator a filled shell reaches, in radians of latitude.
     *
     * <b>Short of the poles, like the meridians.</b> A house or a sign is a slice of the sky
     * that does converge at the poles, but filling all the way there stacks twelve translucent
     * wedges on one point and the result is a black cap - which says nothing and hides the
     * bodies behind it.
     */
    static final double FILL_SPAN = 1.02;

    /** How far up the shell one collision level lifts a body. */
    static final double STACK_STEP = 0.085;

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
     * <b>Set from the outermost shell, not chosen.</b> At 1.45 the sky shell projected to 660
     * pixels of a 550-pixel half-frame and the globe ran off every edge - which looks like a
     * broken camera rather than like a zoom. The largest thing in the scene is SHELL_SKY, so
     * this is the value that puts it comfortably inside the frame at the default distance,
     * and the reader scrolls from there.
     */
    private static final double ZOOM = 0.95;

    /** The tilt beyond which the poles cross the view and the scene reads as inverted. */
    static final double MAX_PITCH = 1.25;

    Globe() {
        this.yaw = 0.0;
        // <b>Looking down at about fifty degrees.</b> At twenty-four the shells project to
        // ellipses so flat that the scene reads as a squashed wheel rather than as a sphere -
        // which loses the whole argument for the view. The prototype's camera sits at
        // [0, 4.2, 3.5], which is this angle; matching it means the two look like the same
        // idea rather than like two attempts at one.
        this.pitch = 0.87;
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
        double t = Math.toRadians(lon - origin);
        // The equatorial radius shrinks as a body rides up the shell, so a stacked body stays
        // on the sphere instead of floating off it.
        double lift = Math.max(-radius, Math.min(radius, y));
        double ring = Math.sqrt(Math.max(0.0, radius * radius - lift * lift));
        return new double[] {-ring * Math.cos(t), lift, ring * Math.sin(t)};
    }

    /** The full circle of a shell's equator, as world points. */
    static double[][] equator(double origin, double radius, int segments) {
        int n = Math.max(3, segments);
        double[][] pts = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            pts[i] = onShell(origin + (360.0 * i) / n, origin, radius, 0.0);
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
        int n = Math.max(2, segments);
        double t = Math.toRadians(lon - origin);
        double[][] pts = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double phi = -MERIDIAN_SPAN + (2.0 * MERIDIAN_SPAN * i) / n;
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
