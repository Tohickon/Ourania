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
    /**
     * The obliquity of the ecliptic, in radians - and the reason the ribbons sit where they do.
     *
     * <b>A number with a source rather than a number that looked right.</b> The three chart
     * ribbons are circles of latitude on one sphere, and this is the latitude the outer two
     * ride: the tropics. It was arrived at twice over, which is why it is worth keeping. David
     * drew the three bands at +23.5, 0 and -23.5 in a design sketch; separately, measuring how
     * far apart the decks have to be before the sky ring visibly clears Chart A at the working
     * tilt gave <b>0.75</b>. A ribbon at the obliquity on a shell of 1.75 sits at
     * {@code 1.75 * sin(23.44) = 0.696}, and on the 1.92 house shell at 0.764. The tuned
     * number and the astronomical one are the same number, so the astronomical one is used.
     */
    static final double OBLIQUITY = Math.toRadians(23.4392911);

    /**
     * The one shell all three chart ribbons ride, just inside the houses.
     *
     * <b>One radius, not three, and that is the whole fix.</b> The rings used to nest - 1.20,
     * 1.50, 1.80 - and nested rings cannot read as a stack at any tilt, because the depth term
     * scales with the radius and a bigger ring always straddles a smaller one however far it is
     * lifted. Measured on 2026-09-21: at the default tilt Chart B rose above Chart A, and from
     * beneath the sky fell below it. On one shell the radius term is identical for all three
     * and the latitude alone decides, which is what makes a bloom a bloom.
     */
    static final double SHELL_CHART = 1.75;

    static final double SHELL_NATAL = SHELL_CHART;
    static final double SHELL_PARTNER = SHELL_CHART;
    /**
     * The sky ring, now inside the zodiac rather than outside it.
     *
     * <b>The zodiac has to enclose everything it measures.</b> The sky rode the outer skin,
     * beyond the sign plane, so a transiting body sat outside the band that says which sign it
     * is in - which is the one thing the zodiac is for. Every body ring is inside the plane
     * now: natal, partner and sky, in that order, with the zodiac wrapped around all three.
     */
    static final double SHELL_SKY = SHELL_CHART;
    static final double SHELL_HOUSE = 1.92;
    /** Egyptian bounds, just inside the signs - the flat wheel's order, kept. */
    static final double SHELL_BOUND = 1.99;
    static final double SHELL_SIGN_INNER = 2.08;
    static final double SHELL_SIGN_OUTER = 2.30;
    /** Decans, just outside the signs, as on the flat wheel. */
    static final double SHELL_DECAN = 2.38;
    static final double SHELL_TICK = 2.42;

    /**
     * How far a hovered degree tick stands out of the scale.
     *
     * <b>Named because the mansion band is placed against it.</b> It was 0.34, which put the
     * reach of the ticks at 2.67 and left no room outside them for another ring that the panel
     * could still hold. At 0.21 the lit tick is still the longest on the scale - a sign
     * boundary reaches 0.20 - and it stops a hair short of the band rather than crossing it.
     */
    static final double TICK_HOVER_REACH = 0.21;

    /**
     * The lunar mansion band, outside everything else.
     *
     * <b>Sized by what the panel can hold, measured rather than chosen.</b> The first attempt
     * put it at 2.74 to 2.92, which looked right at the default tilt and reached exactly 100%
     * of the panel's half-width when the globe is turned edge-on - the view David asked for.
     * A probe sweeping every pitch the reader can drag to puts 2.70 at 90% and 2.92 at 100%,
     * so this is the outermost the ring can sit and still be whole at every angle.
     *
     * The inner edge clears the degree scale: the ticks start at SHELL_SIGN_OUTER + 0.03 and
     * the longest of them reaches TICK_HOVER_REACH beyond that, which is 2.54.
     */
    static final double SHELL_MANSION_INNER = 2.55;

    /** The outside of that band. */
    static final double SHELL_MANSION_OUTER = 2.70;

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

    /**
     * How far apart the three decks sit, as a share of the ribbon's own radius.
     *
     * <b>Chosen for the eye, not taken from the sky.</b> This was {@code sin(OBLIQUITY)} -
     * 0.397, the true tilt between the ecliptic and the equator - which is the honest number
     * and reads as too little air. David compared it against his prototype on 2026-09-23,
     * where the three planes sit at 1.5 on a radius of 2.5, and chose that: 0.60, half again
     * as much separation, about eight times a body's bead rather than five.
     *
     * The obliquity has not gone anywhere - {@link #OBLIQUITY} still tilts the meridians and
     * still sets {@link #INCLINE_SKY}. What it stopped doing is deciding how far apart three
     * parallel rings are drawn, which was never a question about the sky: the rings are not
     * the ecliptic and the equator, they are three charts that have to be told apart.
     */
    static final double LIFT_FRACTION = 0.60;

    /**
     * How far the upper deck sits above the middle one, when the decks are stacked rather
     * than crossed. Chart A rides this, the sky holds the middle, and Chart B takes its
     * negative - see {@link SkymapPanel#ringDeck} for which chart lands where.
     *
     * <b>The other way of keeping three rings apart, and it keeps a different promise.</b>
     * Tilting separates the planes and costs the one thing a reader crosses rings to do:
     * a tilted ring turns longitude into something other than the angle you see, so a transit
     * conjunct a natal planet is only over it at the Ascendant and the Descendant, and drifts
     * away from it everywhere else. Lifted instead, each ring stays a circle of latitude on
     * its own shell, every degree keeps the direction it has on the natal ring, and a
     * conjunction across charts is a body directly above another body.
     *
     * What it gives up is the crossing: three parallel rings meet nowhere, so the Ascendant is
     * no longer a place where all three visibly agree. It does not need to be, because now
     * they agree everywhere.
     *
     * <b>A height rather than an angle.</b> Latitude would put the outer ring further up than
     * the inner one for the same tilt; a height puts all three at the same remove, which is
     * what "just above" and "just below" mean when you look at the globe edge-on. At 0.40 the
     * gap is about five times a body's bead, and the horizontal reach of the upper ring shrinks
     * by 0.045 of its radius to stay on its shell - the same arithmetic that keeps a stacked
     * body on the sphere.
     *
     * <b>Named for the deck, not for what is on it.</b> This was LIFT_SKY until 2026-09-23,
     * from when the sky rode above the chart. {@link SkymapPanel#ringDeck} reversed that on
     * 2026-09-21 - the sky took the middle and Chart A went up - and the constant kept the old
     * name, so for two days the upper deck was called the sky while carrying Chart A. On
     * 2026-09-23 that name was read back as the arrangement and reported to David as a
     * difference from his prototype that did not exist. A constant that names its contents is
     * wrong the moment the contents move; one that names its place cannot go stale.
     */
    static final double LIFT_UPPER = SHELL_CHART * Math.min(LIFT_FRACTION, 1.0);

    /** The lower deck, the same distance under the middle as the upper one is over it.
     *
     * <b>Not a compile-time constant any more, and that is deliberate.</b> A
     * {@code static final double} with a literal initialiser is inlined into every class that
     * mentions it, so changing one and rebuilding only this file leaves GlobeRenderer holding
     * the old value. That cost a measurement run on 2026-09-21: three deck lifts were rendered
     * and produced three identical sets of numbers. Derived from {@link #OBLIQUITY} through a
     * method call, it cannot be folded, so every reader sees the same number as this file. */
    static final double LIFT_LOWER = -LIFT_UPPER;

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

    /**
     * The far end of the tilt: overhead, looking straight down at the chart.
     *
     * <b>A quarter turn is the whole range, and the reason is a haircut.</b> This was 1.25, then
     * briefly a half turn, and the churn was all one mistake - treating the limit as a number to
     * tune rather than asking what the reader is looking at. David settled it with a picture:
     * "imagine the aspect arches are the hair on top of the head that is the wheel. I want to
     * see the haircut, all of it, from the top of the head and the sides. Chart A would be the
     * headband around the forehead, the chart and transit would be where the face is. The
     * haircut is funky but it's always on top of the head."
     *
     * A head is looked at from the top and from the sides - which is exactly edge-on to
     * overhead, and nothing past it. Beyond a quarter turn the camera is under the chin: the
     * projection inverts, every arch hangs below, and there is no hair down there to see.
     *
     * <b>Which is why two attempts to fix that view both failed.</b> Flipping the arches back at
     * the crossing made every one of them snap upside down in a single frame - the same
     * discontinuity as the 2026-09-19 horizontal mirror, and rejected for the same reason.
     * Leaving them alone made half the range a view of the underside. Neither was fixable
     * because the range itself was the error.
     *
     * Stopping here, the arch domes at every tilt the reader can reach, the houses read the
     * right way at every tilt, and nothing ever jumps.
     *
     * Written out rather than {@code Math.PI / 2 - MIN_PITCH}, which javac rejects as a forward
     * reference: MIN_PITCH is declared below, and reordering two constants to satisfy the
     * compiler would put them in an order that reads worse than it computes.
     */
    static final double MAX_PITCH = Math.PI / 2;

    /**
     * How near edge-on the camera may come, on either side of the chart's plane.
     *
     * <b>This was a floor, and the globe could only lean one way.</b> On 2026-09-18 the clamp
     * became {@code [MIN_PITCH, MAX_PITCH]} because David reported the houses running the wrong
     * way: at the default tilt of +0.32 the houses and signs run counterclockwise from an
     * Ascendant on the left, houses 1 to 6 below the horizon - the chart convention and the flat
     * wheel's - and at -0.32 both run clockwise with house 1 above the horizon, because the
     * camera is underneath the plane and sees the chart in a mirror. One upward drag took it
     * there, so the lower half was shut off.
     *
     * <b>Shutting it off cost the other thing.</b> David, 2026-09-19: "i used to be able to tilt
     * the chart more than one way". Both halves are back. Nothing is mirrored to make the
     * underside read like the top - see {@link #project} for why that was tried and taken out
     * again - so from below the chart reads as the back of a painted window, and the panel says
     * which side is being looked at.
     *
     * So this is now a band either side of edge-on that the camera passes through rather than
     * rests in: about three degrees, because edge-on the whole plane collapses to a line and the
     * direction of the houses is not visible at all.
     */
    static final double MIN_PITCH = 0.01;

    Globe() {
        this.yaw = 0.0;
        // <b>Nearly edge-on, at about eighteen degrees.</b> This was fifty for a while, on
        // the reasoning that a low angle made the shells project to ellipses flat enough that
        // the scene read as a squashed wheel rather than a sphere. That was true when the
        // shells were wireframes: a circle seen edge-on is a line, and a dozen of them are a
        // scribble. It stopped being true when the signs became coloured wedges over the
        // surface - a sphere with shading reads as a sphere from any angle, and the low camera
        // is what makes the equatorial ring a ring rather than a disc seen from above.
        // <b>Shallower since the ribbons became a bloom.</b> Three circles of latitude on one
        // sphere separate on screen by their height times the cosine of the tilt, while each
        // one's own ellipse opens as the sine - so the stack is plainest near edge-on and
        // collapses toward the overhead view, where cosine goes to nothing. Measured rather
        // than reasoned: at 0.32 the sky ribbon still overlapped Chart A by 87 pixels, at 0.18
        // it clears. See MAX_PITCH for the other end of the same arithmetic.
        // <b>Negative, which is now the view from above.</b> The sign of the pitch that
        // looks down changed with the handedness flip on 2026-09-23: the camera sits at world
        // y = -distance * sin(pitch), so a negative pitch puts it over the plane, looking down
        // at Chart A with the houses running the right way.
        this.pitch = -0.18;
    }

    /**
     * The share of a pending move applied each frame, and the share of the rest that survives.
     *
     * <b>0.05, which is three's dampingFactor.</b> A drag does not move the camera; it adds to
     * a pending delta, and each frame takes a twentieth of what is pending and lets the rest
     * decay. The total applied comes to exactly the raw delta - the sum of {@code d * 0.95^n}
     * is {@code d / (1 - 0.95)} times {@code 0.05}, which is 1 - so this changes when the
     * motion happens and never how far it goes. Half of it is spent in about thirteen frames
     * and it is quiet inside a second.
     *
     * That is the whole of the weight the reader feels: the globe eases under the hand rather
     * than tracking it exactly, and it coasts to rest when the hand lets go instead of stopping
     * dead. David, 2026-09-23, on the prototype: "i wanted it to move and tilt and spin and
     * bloom and zoom like this".
     */
    static final double DAMPING = 0.05;

    /** Below this a pending move is over, and is zeroed so it cannot drift. */
    private static final double SETTLED = 1e-5;

    /** Yaw still owed to the camera by drags already made. */
    private double pendingYaw;

    /** Pitch still owed, as above. */
    private double pendingPitch;

    /** World offset still owed to the target by pans already made. */
    private double pendingPanX;
    private double pendingPanY;
    private double pendingPanZ;

    /** The point the camera looks at and turns about. Panning moves this, not the camera. */
    double targetX;
    double targetY;
    double targetZ;

    /** How far the target may be dragged from the middle before it stops. */
    private static final double PAN_REACH = SHELL_MANSION_OUTER;

    /**
     * Applies a drag, in pixels. Nothing moves until {@link #settle} runs.
     *
     * <b>Both axes on the height, which is what the prototype does.</b> Sideways used to divide
     * by the panel's width and up-down by the same width, so on a panel wider than it is tall
     * the two axes had different gearing and a diagonal drag came out skewed. three's
     * OrbitControls puts both on {@code clientHeight} and says so in a comment - "yes, height" -
     * for exactly this reason: the hand should not learn a different sensitivity when the window
     * is resized.
     *
     * Sideways is the same on both sides of the plane, because nothing is mirrored - see
     * {@link #project}. Reversing it below would be the same discontinuity in the hand that the
     * flip was in the picture.
     */
    void drag(double dx, double dy, int panelHeight) {
        int h = Math.max(1, panelHeight);
        this.pendingYaw += (dx / h) * Math.PI * 2.0;
        this.pendingPitch += (dy / h) * Math.PI * 2.0;
    }

    /**
     * Applies a pan, in pixels, moving the point the camera orbits rather than the camera.
     *
     * <b>Screen-space, so the globe follows the cursor.</b> The target slides along the camera's
     * own right and up axes by the world distance that projects to the pixels asked for, which
     * is {@code distance / focal} per pixel. Checked numerically against {@link #project} over
     * four thousand random cameras before it was written: a point at the target's depth lands
     * exactly under the cursor, to within a ten-thousandth of a pixel. Points nearer or further
     * than the target slide by more or less, which is perspective doing its job and is what the
     * prototype does too.
     *
     * The axes come from inverting the yaw-then-pitch order in {@link #project}: camera right is
     * {@code (cos yaw, 0, sin yaw)} and camera up is
     * {@code (sin pitch sin yaw, cos pitch, -sin pitch cos yaw)}.
     */
    void pan(double dx, double dy, int panelWidth, int panelHeight) {
        double focal = Math.max(1.0, Math.min(panelWidth, panelHeight) * ZOOM);
        double k = this.distance / focal;
        double cy = Math.cos(this.yaw);
        double sy = Math.sin(this.yaw);
        double cp = Math.cos(this.pitch);
        double sp = Math.sin(this.pitch);
        // Dragging right carries the globe right, which is the target going left.
        this.pendingPanX += -cy * dx * k + sp * sy * dy * k;
        this.pendingPanY += cp * dy * k;
        this.pendingPanZ += -sy * dx * k - sp * cy * dy * k;
    }

    /**
     * Advances one frame of the glide. True while anything is still moving.
     *
     * The caller runs this on a timer and repaints while it answers true; see SkymapPanel.
     */
    boolean settle() {
        boolean moving = false;
        if (Math.abs(this.pendingYaw) > SETTLED) {
            this.yaw += this.pendingYaw * DAMPING;
            this.pendingYaw *= 1.0 - DAMPING;
            moving = true;
        } else {
            this.pendingYaw = 0.0;
        }
        if (Math.abs(this.pendingPitch) > SETTLED) {
            double step = this.pendingPitch * DAMPING;
            this.pitch = clampPitch(this.pitch + step, step);
            this.pendingPitch *= 1.0 - DAMPING;
            moving = true;
        } else {
            this.pendingPitch = 0.0;
        }
        if (Math.abs(this.pendingPanX) > SETTLED || Math.abs(this.pendingPanY) > SETTLED
            || Math.abs(this.pendingPanZ) > SETTLED) {
            this.targetX = clamp(this.targetX + this.pendingPanX * DAMPING, -PAN_REACH, PAN_REACH);
            this.targetY = clamp(this.targetY + this.pendingPanY * DAMPING, -PAN_REACH, PAN_REACH);
            this.targetZ = clamp(this.targetZ + this.pendingPanZ * DAMPING, -PAN_REACH, PAN_REACH);
            this.pendingPanX *= 1.0 - DAMPING;
            this.pendingPanY *= 1.0 - DAMPING;
            this.pendingPanZ *= 1.0 - DAMPING;
            moving = true;
        } else {
            this.pendingPanX = 0.0;
            this.pendingPanY = 0.0;
            this.pendingPanZ = 0.0;
        }
        return moving;
    }

    /** True while the glide is still running, which is what "the globe is moving" means. */
    boolean coasting() {
        return Math.abs(this.pendingYaw) > SETTLED || Math.abs(this.pendingPitch) > SETTLED
            || Math.abs(this.pendingPanX) > SETTLED || Math.abs(this.pendingPanY) > SETTLED
            || Math.abs(this.pendingPanZ) > SETTLED;
    }

    /** Runs the glide to a standstill. For measurement, where there are no frames. */
    void settleFully() {
        for (int i = 0; i < 10000 && settle(); i++) {
            continue;
        }
    }

    /**
     * The tilt, kept off the poles and out of the edge-on band.
     *
     * <b>Through the band, not stopped at its edge.</b> Snapping a tilt that lands inside the
     * band back to the near edge would make the plane a wall: every drag toward it would stop
     * dead and the reader could never reach the underside at all, which is the thing being
     * given back. So a tilt that lands inside comes out the far side, in the direction the hand
     * was already moving - one continuous motion from above the chart to below it, pausing
     * nowhere.
     */
    static double clampPitch(double pitch, double step) {
        // <b>Pole to pole, with nothing excluded in between.</b> David, pointing at his own
        // prototype: "i can go full tilt from south pole to north pole on that site." So the
        // camera walks the whole meridian - down on the north pole, through edge-on, up at the
        // south - and rests wherever it is let go, edge-on included.
        //
        // <b>This clamp has been rewritten four times in two days and the churn was one
        // mistake:</b> treating the range as a number to tune instead of asking what the reader
        // is looking at. It excluded a band either side of edge-on, then the whole lower half,
        // then everything past a quarter turn. Each was a guess at a question the prototype
        // answers directly.
        //
        // <b>What the lower half costs, and why it is affordable now.</b> Below the plane the
        // house sequence reads backwards - that is what shut it off on 2026-09-18. It is
        // affordable because the house numbers now ride near both poles rather than in the
        // plane, so whichever way the globe is turned, a set of them faces the reader and says
        // which house is which.
        return clamp(pitch, -MAX_PITCH, MAX_PITCH);
    }

    /**
     * True when the camera is under the chart's plane, looking up at it.
     *
     * <b>Positive pitch is underneath.</b> The camera sits at world y =
     * {@code -distance * sin(pitch)}, so the sign that puts it below the plane is the positive
     * one. This read {@code pitch < 0} until 2026-09-23, when the chart was turned over so it
     * reads correctly from above - see {@link #onShell}. The projection is unchanged; what
     * changed is which side the right-way-round view is on.
     */
    boolean fromBelow() {
        return this.pitch > 0.0;
    }

    /** How much of the distance one notch of the wheel takes away. three's zoomSpeed of 1. */
    private static final double ZOOM_STEP = 0.95;

    /**
     * Applies a scroll. Bounded so the reader cannot end up inside the core or in deep space.
     *
     * <b>A scale, not a step.</b> This added 0.35 per notch to a distance between 3.2 and 12,
     * which is a ninth of the way in when you are far out and a tenth of everything you have
     * left when you are close - so the same gesture lurched near the globe and did almost
     * nothing far from it. Multiplying takes the same share every time, which is what makes a
     * zoom feel like one thing rather than two, and it is what the prototype does:
     * {@code radius *= pow(0.95, ...)}.
     *
     * The wheel reports positive when it is rolled away, which should move the camera back, so
     * the exponent is negated - a notch out is 1/0.95, about five and a quarter percent.
     */
    void zoom(double ticks) {
        this.distance = clamp(this.distance * Math.pow(ZOOM_STEP, -ticks), 3.2, 12.0);
    }

    /**
     * World point to panel point.
     *
     * <b>Yaw then pitch then perspective, in that order.</b> Any other order tilts about an
     * axis that has already been turned, which reads as the globe wobbling rather than turning
     * - the classic gimbal complaint, and the reason this is one method rather than a matrix
     * assembled at each call site.
     */
    Projected project(double worldX, double worldY, double worldZ, int width, int height) {
        // <b>Everything is measured from the target, not from the middle of the world.</b>
        // Panning moves the target; the camera keeps looking at it and keeps turning about it,
        // so a globe dragged into the corner spins about the point under the cursor rather than
        // about a centre that is no longer on the screen. That is the prototype's behaviour and
        // it is the reason pan is a camera move rather than a screen offset applied at the end.
        double x = worldX - this.targetX;
        double y = worldY - this.targetY;
        double z = worldZ - this.targetZ;
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

        // <b>Nothing is flipped when the camera goes under the plane, and that is the point.</b>
        // It was, for one afternoon: below the plane the horizontal was mirrored so the houses
        // would keep running counterclockwise down there. It works, and it is wrong, because a
        // mirror applied on one side of a boundary is a discontinuity at it. David, 2026-09-20,
        // on seeing it: "when i tilt up the signs shouldnt move at all you have them on one side
        // and then when tilting over the horizon they appear to shoow up instantly on the other
        // side."
        //
        // He is right, and the reason is worth keeping. Every other camera move here is
        // continuous - a degree of drag moves the picture by a degree's worth - so the reader
        // learns that the globe is a solid object they are walking around. One instant
        // rearrangement at the horizon teaches the opposite, and it teaches it at exactly the
        // moment the reader is trying to work out what they are looking at.
        //
        // So the projection is honest on both sides and the chart simply turns away from the
        // viewer. Seen from below the houses do read clockwise - that is not a fault either,
        // it is what the back of a painted window looks like: "I just wanted to see the bottom
        // of the transluscent charts as if viewing painted glass from underneath and above."
        // GlobeRenderer says which side is being looked at, so a reader cannot mistake the back
        // for the front.
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
        // <b>Counterclockwise seen from above, where Chart A is.</b> This was +sin until
        // 2026-09-23, which wound longitude the other way: the houses read correctly only from
        // the south side, and Chart A rides LIFT_UPPER at +y, so the one view that read the
        // right way was the one looking at Chart A's underside. David: "make sure that chart a
        // is on top meaning if looking down at it the houses will be going the correct way."
        //
        // The Ascendant does not move. It sits at t = 0, where the point is (-ring, 0, 0) and
        // sin is zero, so flipping z leaves it exactly on the left where the flat wheel puts
        // it. The order turns over; the picture does not slide.
        double z = -ring * Math.sin(t);
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
     * How far an aspect line bows out of the straight chord, as a fraction of the geometric
     * answer.
     *
     * <b>One is not a taste, it is the arc that touches the shell.</b> Two bodies on a shell of
     * radius R separated by an angle are half a chord apart, and that half-chord is exactly the
     * height a bow has to reach for its apex to land back on R: an opposition rises a full
     * radius and passes over the pole, a trine rises to 0.87 of one, a conjunction barely
     * leaves the surface. So every arc in a ring, whatever its aspect, has its top on the same
     * sphere - which is the thing being drawn here, a globe of aspects inside the globe of
     * bodies. Lowering this flattens them back toward chords; raising it puts arcs through the
     * rings outside them.
     */
    static final double ARC_RISE = 1.0;

    /**
     * An aspect as an arc over the middle, rather than a chord through it.
     *
     * <b>The chord is honest and unreadable.</b> A line between two bodies on a sphere runs
     * through the interior, and a dozen of them are a ball of wool: an opposition is the worst
     * of them, a diameter that passes through the exact centre where every other line is
     * already crowding. Lifted into a bow, the same opposition goes up and over the middle and
     * a reader turning the globe sees it as one long span rather than as a line ending
     * somewhere behind the far bodies. It also puts the widest aspects highest, so the figure
     * a chart makes has a silhouette.
     *
     * <b>Vertical, and perpendicular to the chord.</b> The bow could lie in the plane
     * through both bodies and the centre, which is the plane a great circle would use - but for
     * two bodies on the ecliptic that plane is the ecliptic, so the arc would hug the ring it
     * came from and read as a wider ring rather than as a span. Rotating it to the vertical is
     * a presentational choice, made once here so that every arc in the scene bows the same way
     * and the eye reads them as a family. It is also the only choice that stays continuous at
     * an exact opposition, where the two bodies are antipodal and the plane through them and
     * the centre is undefined.
     *
     * <b>Up by default, down for a negative rise.</b> The sign flips the bow through the chord
     * and changes nothing else, so a downward arc is the mirror of the upward one with its
     * lowest point on the same shell - which is what lets one chart's network sit as a bowl
     * under the sign plane while another's is a dome over it. GlobeRenderer.riseFor says
     * which way each line goes.
     *
     * @param segments how many straight pieces stand in for the curve
     * @return segments + 1 world points, starting at a and ending at b
     */
    static double[][] arc(double[] a, double[] b, int segments) {
        return arc(a, b, segments, ARC_RISE);
    }

    /**
     * As above, over a chosen rise - zero for the straight chord.
     *
     * <b>One method for both, because the painter draws both.</b> The reader can switch the
     * bow off, and a straight line built by a second code path is a straight line that fades,
     * sorts and steps by different arithmetic than the curved one - which is this project's
     * most common defect wearing a new hat. At a rise of zero this returns the chord exactly.
     */
    static double[][] arc(double[] a, double[] b, int segments, double rise) {
        int n = Math.max(1, segments);
        double[] lift = arcLift(a, b, rise);
        double[][] pts = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            // A half sine: zero at both bodies, so the arc lands on the glyphs rather than
            // near them, and steepest where it leaves them.
            double bow = Math.sin(Math.PI * t);
            pts[i] = new double[] {
                a[0] + (b[0] - a[0]) * t + lift[0] * bow,
                a[1] + (b[1] - a[1]) * t + lift[1] * bow,
                a[2] + (b[2] - a[2]) * t + lift[2] * bow,
            };
        }
        return pts;
    }

    /**
     * The vector added at the top of an arc: which way it bows, times how far.
     *
     * Separate from {@link #arc} because the painter sizes its curve from the height and the
     * check suite asserts the height, and neither wants the whole polyline to get one number.
     */
    static double[] arcLift(double[] a, double[] b) {
        return arcLift(a, b, ARC_RISE);
    }

    /**
     * An aspect as hair lying over the crown: a path <b>on</b> a shell, not a bulge through the
     * air above one.
     *
     * <b>What was wrong with the bulge.</b> {@link #arc} takes the chord and pushes its middle
     * out along a straight perpendicular, so the path leaves the sphere, climbs through empty
     * space and comes back. Seen from the side that reads as a dome and looks right; seen from
     * anywhere else it reads as wrong, and no amount of tilting or flipping fixes it - which is
     * what a whole afternoon of trying either established. David: "it wouldn't matter to view it
     * upside down or underneath if you could get the placement of the aspect arches correct",
     * and the placement he means is a haircut: "the hair on top of the head that is the wheel...
     * the haircut is funky but it's always on top of the head."
     *
     * <b>So every point of the path sits at a real radius.</b> The line between the two bodies
     * is bowed toward the crown as before, but each point is then pushed back out to a radius
     * that runs from the first body's, through the shell at the middle of the span, to the
     * second body's. The ends therefore land exactly on their glyphs, and everything between
     * lies on a surface - hair on a scalp rather than a wire over it.
     *
     * @param shell how far out the middle of the span rides - the crown it combs over
     * @param sign  +1 to comb over the top, -1 under the chin, from the deck's own rule
     */
    static double[][] arcOverShell(double[] a, double[] b, int segments, double shell,
                                   double sign) {
        int n = Math.max(1, segments);
        double[] dir = arcLift(a, b, 1.0);
        double dl = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        double ra = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        double rb = Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]);
        if (dl < 1e-9 || ra < 1e-9 || rb < 1e-9) {
            return arc(a, b, n, 0.0);
        }
        double[][] pts = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double bow = Math.sin(Math.PI * t);
            double x = a[0] + (b[0] - a[0]) * t + sign * dir[0] * bow;
            double y = a[1] + (b[1] - a[1]) * t + sign * dir[1] * bow;
            double z = a[2] + (b[2] - a[2]) * t + sign * dir[2] * bow;
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len < 1e-9) {
                pts[i] = new double[] {x, y, z};
                continue;
            }
            // The radius this point should sit at: the bodies' own at the ends, the crown in
            // the middle. Without this the path would be a great circle and hug the ring it
            // came from, which is the objection arc()'s own javadoc raises against them.
            double want = (ra + (rb - ra) * t) * (1.0 - bow) + shell * bow;
            pts[i] = new double[] {x / len * want, y / len * want, z / len * want};
        }
        return pts;
    }

    /**
     * The rise that lands this arc's apex on a chosen shell - cracks on one egg.
     *
     * <b>Why a function and not the constant it replaces.</b> {@link #ARC_RISE} is 1.0 because
     * that is the rise whose apex lands back on the <i>bodies' own</i> shell, whatever the
     * aspect. That was the right rule while the aspects were the outermost thing drawn; it is
     * the wrong one now the houses sit outside the ribbons, because every arc then overshoots
     * the house sphere it is meant to be drawn on. Asking instead for a named shell gives every
     * arc in the scene one surface, and the widest and the tightest aspects land on it alike.
     *
     * <p>{@link #arcLift} sets the apex offset to {@code rise * chord / 2}, and the apex sits at
     * the chord's midpoint plus that offset square to the chord. So with {@code m} the
     * midpoint's distance from the centre, the apex lands at {@code sqrt(m^2 + h^2)} and the
     * rise wanted is {@code 2 * sqrt(shell^2 - m^2) / chord}. For bodies on one shell that is
     * exact - a constant 1.15 would put an opposition at 1.92 and a sextile at 2.04, which is
     * two eggs, not one.
     *
     * <p><b>Where it is only nearly exact:</b> an arc between two <i>different</i> decks has
     * endpoints at different heights, so the perpendicular the bow rides is not square to the
     * midpoint vector and the apex lands a little off the shell. The error is small at the
     * latitudes the ribbons use and the alternative - solving for a bow that is neither
     * vertical nor square to the chord - would give up the property that every arc in the scene
     * bows the same way. GlobeCheck asserts the exact case exactly and this one within a band.
     *
     * @return 0 when the shell is inside the chord's own midpoint, where no bow reaches it
     */
    static double riseToShell(double[] a, double[] b, double shell) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) {
            return 0.0;
        }
        double mx = (a[0] + b[0]) / 2.0;
        double my = (a[1] + b[1]) / 2.0;
        double mz = (a[2] + b[2]) / 2.0;
        double m2 = mx * mx + my * my + mz * mz;
        double h2 = shell * shell - m2;
        if (h2 <= 0.0) {
            return 0.0;
        }
        return 2.0 * Math.sqrt(h2) / len;
    }

    /** As above, over a chosen rise. */
    static double[] arcLift(double[] a, double[] b, double rise) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) {
            return new double[] {0.0, 0.0, 0.0};
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        // Up, with the part of it that runs along the chord taken out - so the bow is square
        // to the line it leaves, whatever plane that line lies in.
        double px = -uy * ux;
        double py = 1.0 - uy * uy;
        double pz = -uy * uz;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        if (pl < 1e-6) {
            // A chord that already points at the pole has no upward to bow into. Nothing in
            // this scene draws one - every shell shares a centre, so two bodies are never
            // stacked vertically - but a fallback that returns a flat chord is better than one
            // that returns a division by nothing.
            return new double[] {0.0, 0.0, 0.0};
        }
        double h = rise * len / 2.0;
        return new double[] {px / pl * h, py / pl * h, pz / pl * h};
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
