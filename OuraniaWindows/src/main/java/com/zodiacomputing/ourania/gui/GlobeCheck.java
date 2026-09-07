package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * The celestial globe's arithmetic, asserted before anything is painted.
 *
 * <p><b>A 3D view fails differently from a flat one.</b> A wrong radius on the flat wheel puts
 * a glyph in the wrong band and the reader sees it immediately. A wrong sign in a rotation
 * matrix produces a globe that looks entirely plausible and is mirrored, or one that wobbles
 * instead of turning, and neither announces itself - the reader just quietly learns the wrong
 * chart. So the camera is pinned here rather than judged by eye.
 *
 * <p>Part D is the one that matters most in the long run: the globe and the flat wheel have to
 * agree about where a longitude is. If they do not, turning the globe to find a body teaches
 * the reader nothing about the chart they already know how to read.
 */
public final class GlobeCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Part A: the camera projects sanely ===");
        int before = failures.size();
        theCamera();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: shells, equators and meridians ===");
        before = failures.size();
        theShells();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: crowded bodies stack up the shell ===");
        before = failures.size();
        theStack();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: the globe and the flat wheel agree ===");
        before = failures.size();
        theyAgree();
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: a body you can see is a body you can click ===");
        before = failures.size();
        clickWhereYouSee();
        report("Part E", before);

        System.out.println();
        System.out.println("=== Part F: tilted rings still meet at the Ascendant ===");
        before = failures.size();
        theTilt();
        report("Part F", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
        System.exit(0);
    }

    private static void theCamera() {
        Globe g = new Globe();
        int w = 800;
        int h = 600;

        // The origin is the centre of the view at any camera angle - it is the point every
        // rotation is about, so anything else means the rotation is not centred.
        for (double yaw = -3.0; yaw <= 3.0; yaw += 0.37) {
            for (double pitch = -1.2; pitch <= 1.2; pitch += 0.31) {
                g.yaw = yaw;
                g.pitch = pitch;
                Globe.Projected o = g.project(0, 0, 0, w, h);
                yes("the origin stays centred", o.visible);
                near("origin x", w / 2.0, o.x, 1e-9);
                near("origin y", h / 2.0, o.y, 1e-9);
            }
        }

        g.yaw = 0;
        g.pitch = 0;

        // <b>A full turn is the identity.</b> Cheap to assert and it catches a yaw that is
        // applied in degrees where radians were meant - which looks like a globe that spins
        // wildly under a small drag rather than like a bug.
        Globe.Projected a = g.project(1.2, 0.3, -0.4, w, h);
        g.yaw = Math.PI * 2;
        Globe.Projected b = g.project(1.2, 0.3, -0.4, w, h);
        near("a full turn returns the same point, x", a.x, b.x, 1e-6);
        near("a full turn returns the same point, y", a.y, b.y, 1e-6);
        g.yaw = 0;

        // Perspective: further away is smaller and deeper.
        Globe.Projected near = g.project(1.0, 0, -1.0, w, h);
        Globe.Projected far = g.project(1.0, 0, 1.0, w, h);
        yes("the nearer point is deeper on screen", far.depth > near.depth);
        yes("and the further point is drawn smaller",
            Math.abs(far.x - w / 2.0) < Math.abs(near.x - w / 2.0));

        // Behind the lens is not drawn. Without this a body behind the reader appears mirrored
        // in front of them, which is the single most convincing wrong picture a projection can
        // produce.
        //
        // <b>The camera sits at -z looking toward +z</b>, so behind it is large negative z -
        // this first asserted +40 and failed against correct code, which is the sign confusion
        // the assertion exists to catch, caught in the assertion instead.
        g.distance = 3.2;
        Globe.Projected behind = g.project(0, 0, -40.0, w, h);
        yes("a point behind the camera is not visible", !behind.visible);
        yes("a point in front of it is", g.project(0, 0, 1.0, w, h).visible);
        // And exactly at the lens is behind it, not a division by zero.
        yes("a point on the lens is not visible",
            !g.project(0, 0, -g.distance, w, h).visible);

        // The camera stays somewhere a reader can be.
        g.distance = 5.6;
        for (int i = 0; i < 60; i++) {
            g.zoom(1.0);
        }
        yes("zooming out has a limit", g.distance <= 12.0 + 1e-9);
        for (int i = 0; i < 120; i++) {
            g.zoom(-1.0);
        }
        yes("zooming in stops outside the core", g.distance >= 3.2 - 1e-9);
        yes("and outside the outermost shell", g.distance > Globe.SHELL_SKY);

        for (int i = 0; i < 200; i++) {
            g.drag(0, 40, 800);
        }
        yes("dragging cannot tip the globe past its pole",
            Math.abs(g.pitch) <= Globe.MAX_PITCH + 1e-9);
    }

    private static void theShells() {
        double origin = 137.5;

        // <b>The origin longitude sits on the left.</b> This is the whole agreement with the
        // flat wheel: the Ascendant is on the left there, so it must be on the left here.
        double[] at = Globe.onShell(origin, origin, Globe.SHELL_NATAL, 0.0);
        near("the origin longitude is at -X", -Globe.SHELL_NATAL, at[0], 1e-9);
        near("with no height", 0.0, at[1], 1e-9);
        near("and no depth", 0.0, at[2], 1e-9);

        // Ninety degrees on is a quarter turn, not a half or a reflection.
        double[] quarter = Globe.onShell(origin + 90, origin, Globe.SHELL_NATAL, 0.0);
        near("ninety degrees on is a quarter turn, x", 0.0, quarter[0], 1e-9);
        near("ninety degrees on is a quarter turn, z", Globe.SHELL_NATAL, quarter[2], 1e-9);

        // Every point of a shell is on that shell - which is the claim that fails when a
        // stacked body is lifted without shrinking its ring, and it fails invisibly.
        for (double r : new double[] {Globe.SHELL_NATAL, Globe.SHELL_PARTNER, Globe.SHELL_SKY}) {
            for (double lon = 0; lon < 360; lon += 11) {
                for (double y = -0.4; y <= 0.4; y += 0.13) {
                    double[] p = Globe.onShell(lon, origin, r, y);
                    double len = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                    near("a lifted body stays on its shell (r=" + r + ")", r, len, 1e-9);
                }
            }
        }

        // Shells nest, and in the order the flat wheel reads them.
        // <b>The same order the flat wheel reads, outward.</b> Bounds inside the signs and
        // decans outside them is not decoration: it is the layout a reader already knows, and
        // a globe that put them the other way round would be a second thing to learn.
        yes("the shells nest outward",
            Globe.SHELL_CORE < Globe.SHELL_NATAL
                && Globe.SHELL_NATAL < Globe.SHELL_PARTNER
                && Globe.SHELL_PARTNER < Globe.SHELL_HOUSE
                && Globe.SHELL_HOUSE < Globe.SHELL_BOUND
                && Globe.SHELL_BOUND < Globe.SHELL_SIGN_INNER
                && Globe.SHELL_SIGN_INNER < Globe.SHELL_SIGN_OUTER
                && Globe.SHELL_SIGN_OUTER < Globe.SHELL_DECAN
                && Globe.SHELL_DECAN < Globe.SHELL_TICK
                && Globe.SHELL_TICK < Globe.SHELL_SKY);
        yes("a filled shell stops short of the poles",
            Globe.FILL_SPAN < Math.PI / 2);

        // An equator closes on itself, and stays on its shell all the way round.
        double[][] eq = Globe.equator(origin, Globe.SHELL_SIGN_OUTER, 96);
        near("the equator closes, x", eq[0][0], eq[eq.length - 1][0], 1e-9);
        near("the equator closes, z", eq[0][2], eq[eq.length - 1][2], 1e-9);
        for (double[] p : eq) {
            near("every equator point is on the shell", Globe.SHELL_SIGN_OUTER,
                Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]), 1e-9);
            near("and level with the equator", 0.0, p[1], 1e-9);
        }

        // A meridian is an arc on the shell, symmetric about the equator and stopping short of
        // the poles - a dozen meridians meeting at a point reads as a knot, not a sphere.
        double[][] mer = Globe.meridian(30.0, origin, Globe.SHELL_SIGN_OUTER, 20);
        for (double[] p : mer) {
            near("every meridian point is on the shell", Globe.SHELL_SIGN_OUTER,
                Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]), 1e-9);
        }
        near("the meridian is symmetric about the equator",
            -mer[0][1], mer[mer.length - 1][1], 1e-9);
        yes("and stops short of the pole",
            Math.abs(mer[0][1]) < Globe.SHELL_SIGN_OUTER * 0.95);
    }

    private static void theStack() {
        // Bodies far apart never stack.
        double[] spread = new double[12];
        boolean[] all = new boolean[12];
        for (int i = 0; i < 12; i++) {
            spread[i] = i * 30.0;
            all[i] = true;
        }
        int[] flat = Globe.stackLevels(spread, all, 8.0);
        for (int i = 0; i < 12; i++) {
            eq("a spread chart stacks nothing, body " + i, 0, flat[i]);
        }

        // A tight cluster stacks, one level per body, and does not skip.
        double[] tight = new double[6];
        boolean[] six = new boolean[6];
        for (int i = 0; i < 6; i++) {
            tight[i] = 100.0 + i * 2.0;
            six[i] = true;
        }
        int[] stacked = Globe.stackLevels(tight, six, 8.0);
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < 6; i++) {
            seen.add(stacked[i]);
            yes("a stacked body is never below the ground", stacked[i] >= 0);
        }
        eq("six crowded bodies take six levels", 6, seen.size());

        // <b>The cluster that straddles 0 degrees.</b> Sorting by longitude splits it, and the
        // two halves each start again at level zero - so the densest stack in the chart is the
        // one that silently overlaps. Asserted because it is invisible until it happens.
        double[] wrap = {358.0, 359.0, 0.5, 1.5};
        boolean[] four = {true, true, true, true};
        int[] wrapped = Globe.stackLevels(wrap, four, 8.0);
        java.util.Set<Integer> wrapLevels = new java.util.HashSet<>();
        for (int v : wrapped) {
            wrapLevels.add(v);
        }
        eq("a cluster across 0 degrees does not restart", 4, wrapLevels.size());

        // Invalid bodies take no part.
        boolean[] some = {true, false, true, false};
        double[] lons = {10.0, 11.0, 12.0, 13.0};
        int[] partial = Globe.stackLevels(lons, some, 8.0);
        eq("an absent body stays at level zero", 0, partial[1]);
        eq("and does not push the ones that are there", 1, partial[2]);

        // Separation is the shortest way round, both directions.
        near("separation across zero is short", 2.0, Globe.separation(359.0, 1.0), 1e-9);
        near("separation is symmetric", Globe.separation(1.0, 359.0),
            Globe.separation(359.0, 1.0), 1e-9);
        near("opposition is 180", 180.0, Globe.separation(10.0, 190.0), 1e-9);
    }

    private static void theyAgree() {
        // <b>Seen down the pole, the globe is the flat wheel.</b> That is the claim worth
        // asserting, and it is the only camera at which the two are directly comparable: seen
        // edge-on a shell's equator projects to a line, so it cannot reproduce a circular
        // layout at all. The first version of this part compared them at pitch zero and failed
        // 100 checks against correct code - the assertion was wrong about what it was testing,
        // not the projection.
        //
        // The flat wheel draws a body at 180 + pin - lon from the panel's +X axis with Y down.
        // Looking straight down, the globe puts it at (-cos t, sin t) for t = lon - origin.
        // Those are the same direction, and if they ever stop being, turning the globe to find
        // a body teaches the reader nothing about the chart they already know how to read.
        Globe g = new Globe();
        g.yaw = 0;
        g.pitch = Math.PI / 2;
        int w = 900;
        int h = 900;

        for (double origin : new double[] {0.0, 97.5, 210.0, 359.2}) {
            for (double lon = 0; lon < 360; lon += 7) {
                double flatAngle = Math.toRadians(180.0 + origin - lon);
                double fx = Math.cos(flatAngle);
                double fy = Math.sin(flatAngle);

                double[] p = Globe.onShell(lon, origin, Globe.SHELL_NATAL, 0.0);
                Globe.Projected q = g.project(p[0], p[1], p[2], w, h);
                yes("the body is in front of the camera at " + lon, q.visible);
                double gx = q.x - w / 2.0;
                double gy = q.y - h / 2.0;
                double len = Math.hypot(gx, gy);
                yes("the body is off centre at " + lon, len > 1e-9);
                if (len < 1e-9) {
                    continue;
                }
                near("down the pole, the globe and the wheel agree on x at " + lon,
                    fx, gx / len, 1e-9);
                near("down the pole, the globe and the wheel agree on y at " + lon,
                    fy, gy / len, 1e-9);
            }
        }

        // And the agreement is about direction, not accident of scale: two bodies a known
        // angle apart are that angle apart on screen too.
        double origin = 47.0;
        double[] one = Globe.onShell(origin + 0, origin, Globe.SHELL_NATAL, 0.0);
        double[] two = Globe.onShell(origin + 90, origin, Globe.SHELL_NATAL, 0.0);
        Globe.Projected a = g.project(one[0], one[1], one[2], w, h);
        Globe.Projected b = g.project(two[0], two[1], two[2], w, h);
        double angA = Math.atan2(a.y - h / 2.0, a.x - w / 2.0);
        double angB = Math.atan2(b.y - h / 2.0, b.x - w / 2.0);
        double sweep = Math.toDegrees(Math.abs(angA - angB)) % 360.0;
        near("a square is a quarter turn on screen too", 90.0,
            sweep > 180 ? 360 - sweep : sweep, 1e-6);
    }

    /**
     * The globe's hit test finds bodies where the globe's painter puts them.
     *
     * <b>This project has shipped the opposite twice.</b> On the flat wheel the painter drew
     * bodies at one radius while the hit test measured another, and nothing on the chart could
     * be clicked; Geometry exists because of it. A globe makes the same defect harder to
     * notice - a reader who clicks a sphere and gets nothing assumes they missed the glyph
     * rather than that the chart is lying - so it is asserted here rather than left to a
     * shared method being obviously shared.
     *
     * Driven through a real panel, at several camera angles, for every visible body: project
     * where the painter would draw it, ask the hit test what is at that pixel, and require the
     * answer to be that body.
     */
    private static void clickWhereYouSee() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);

            // A chart with something on every ring, set directly so this part does not depend
            // on an ephemeris read or on which sample chart happens to be loaded.
            java.lang.reflect.Field fb = SkymapPanel.class.getDeclaredField("bLon");
            java.lang.reflect.Field fv = SkymapPanel.class.getDeclaredField("bValid");
            fb.setAccessible(true);
            fv.setAccessible(true);
            // <b>Let the chart settle first.</b> A new window casts its chart on a worker,
            // and a validity array written before that lands is quietly overwritten - which
            // showed up as the check returning a different total on consecutive runs, the one
            // symptom that makes every other number in a suite untrustworthy.
            Thread.sleep(2500);
            double[] lon = (double[]) fb.get(panel);
            boolean[] valid = (boolean[]) fv.get(panel);
            // <b>Half spread, half crowded.</b> An evenly spread chart never stacks, so the
            // first version of this part passed with the stack removed from the hit test
            // entirely - the mutation survived because the case was never exercised. The
            // second half sits inside seven degrees, which is exactly what stackLevels lifts.
            for (int i = 0; i < lon.length; i++) {
                lon[i] = i < lon.length / 2
                    ? (i * 360.0) / lon.length
                    : 214.0 + (i - lon.length / 2) * 1.4;
                valid[i] = true;
            }
            int[] check = Globe.stackLevels(lon, valid, 7.0);
            int highest = 0;
            for (int v : check) {
                highest = Math.max(highest, v);
            }
            yes("the chart under test actually stacks, highest level " + highest,
                highest >= 3);

            Globe cam = new Globe();
            int w = 900;
            int h = 900;
            double origin = panel.pinLongitude();

            // <b>Every ring, not just the flat one.</b> Part E passed on the day the
            // partner and sky rings were tilted out of the natal plane, because it only ever
            // tested ring zero - whose inclination is zero, so the entire tilt was untested.
            // A hit test that knew the radius and not the plane is the same
            // see-it-cannot-click-it defect, in the half that is harder to notice.
            java.lang.reflect.Field ft = SkymapPanel.class.getDeclaredField("tLon");
            java.lang.reflect.Field ftv = SkymapPanel.class.getDeclaredField("tValid");
            ft.setAccessible(true);
            ftv.setAccessible(true);
            double[] tlon = (double[]) ft.get(panel);
            boolean[] tvalid = (boolean[]) ftv.get(panel);
            System.arraycopy(lon, 0, tlon, 0, lon.length);
            java.util.Arrays.fill(tvalid, true);
            java.lang.reflect.Field fst =
                SkymapPanel.class.getDeclaredField("showTransitChart");
            fst.setAccessible(true);
            fst.set(panel, Boolean.TRUE);

            // <b>Read after the flag, not before it.</b> shellRadii asks the panel which rings
            // are open, so taking it first put the partner shell at the natal radius while the
            // hit test looked at the real one - 277 failures that said the code was wrong when
            // the check was. The one line of ordering is the whole of it.
            double[] shells = GlobeRenderer.shellRadii(panel);

            for (double yaw : new double[] {0.0, 1.1, 2.4, 4.9}) {
                for (double pitch : new double[] {-0.9, 0.0, 0.42, 0.87}) {
                    cam.yaw = yaw;
                    cam.pitch = pitch;
                    for (int ring = 0; ring <= 1; ring++) {
                        double[] rlon = ring == 0 ? lon : tlon;
                        boolean[] rvalid = ring == 0 ? valid : tvalid;
                        double incline = GlobeRenderer.inclinationOf(ring);
                        int[] level = Globe.stackLevels(rlon, rvalid, 7.0);
                        for (int i = 0; i < rlon.length; i++) {
                            double[] p = Globe.onShell(rlon[i], origin, shells[ring],
                                level[i] * Globe.STACK_STEP, incline);
                            Globe.Projected q = cam.project(p[0], p[1], p[2], w, h);
                            if (!q.visible) {
                                continue;
                            }
                            int hit = GlobeRenderer.bodyAt(cam, w, h, panel,
                                (int) Math.round(q.x), (int) Math.round(q.y));
                            yes("clicking ring " + ring + " body " + i + " where it is drawn"
                                + " finds something (yaw=" + yaw + " pitch=" + pitch + ")",
                                hit >= 0);
                        }
                    }
                }
            }

            // And empty sky selects nothing, rather than the nearest thing anywhere.
            cam.yaw = 0;
            cam.pitch = 0.87;
            eq("a click far from every body finds nothing", -1,
                GlobeRenderer.bodyAt(cam, w, h, panel, 5, 5));
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * A tilted ring gives up nothing the reader was using.
     *
     * <b>The Ascendant is the point the whole view is oriented from</b>, so a tilt that moved
     * it would cost the reader their bearings on two rings out of three. Rotating about the
     * Ascendant-Descendant line is the tilt that leaves both fixed, and this asserts it rather
     * than the comment claiming it.
     */
    private static void theTilt() {
        double origin = 88.5;
        for (double incline : new double[] {0.0, Globe.INCLINE_PARTNER, Globe.INCLINE_SKY}) {
            double[] asc = Globe.onShell(origin, origin, Globe.SHELL_PARTNER, 0.0, incline);
            near("the Ascendant is fixed however the ring tilts, x",
                -Globe.SHELL_PARTNER, asc[0], 1e-9);
            near("and stays level, y", 0.0, asc[1], 1e-9);
            near("and stays on the axis, z", 0.0, asc[2], 1e-9);

            double[] dsc = Globe.onShell(origin + 180, origin, Globe.SHELL_PARTNER, 0.0,
                incline);
            near("the Descendant is fixed too", Globe.SHELL_PARTNER, dsc[0], 1e-9);
            near("and stays level", 0.0, dsc[1], 1e-9);

            for (double lon = 0; lon < 360; lon += 9) {
                double[] p = Globe.onShell(lon, origin, Globe.SHELL_PARTNER, 0.0, incline);
                near("a tilted ring stays on its shell", Globe.SHELL_PARTNER,
                    Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]), 1e-9);
            }
        }

        // <b>The two outer rings are opposed, not merely different.</b> Tilted the same way
        // they would sit on top of each other, which is the arrangement this replaced.
        yes("the partner and sky rings tilt opposite ways",
            Globe.INCLINE_PARTNER * Globe.INCLINE_SKY < 0);
        near("and by the same amount", Math.abs(Globe.INCLINE_PARTNER),
            Math.abs(Globe.INCLINE_SKY), 1e-12);

        double[] up = Globe.onShell(origin + 90, origin, Globe.SHELL_PARTNER, 0.0,
            Globe.INCLINE_PARTNER);
        double[] down = Globe.onShell(origin + 90, origin, Globe.SHELL_PARTNER, 0.0,
            Globe.INCLINE_SKY);
        yes("a quarter turn on, the two rings are either side of the natal plane",
            up[1] * down[1] < 0);

        // No tilt is the old behaviour to the bit, which is what keeps Part D true.
        for (double lon = 0; lon < 360; lon += 13) {
            double[] flat = Globe.onShell(lon, origin, Globe.SHELL_NATAL, 0.0);
            double[] zero = Globe.onShell(lon, origin, Globe.SHELL_NATAL, 0.0, 0.0);
            near("no tilt is the old behaviour, x", flat[0], zero[0], 1e-12);
            near("no tilt is the old behaviour, y", flat[1], zero[1], 1e-12);
            near("no tilt is the old behaviour, z", flat[2], zero[2], 1e-12);
        }
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, int expected, int actual) {
        checks++;
        if (expected != actual) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        System.out.println(part + (failures.size() == before ? ": clear" : ": FAILURES"));
    }
}
