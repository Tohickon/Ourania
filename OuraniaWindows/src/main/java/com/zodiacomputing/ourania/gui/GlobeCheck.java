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
        System.out.println("=== Part G: the cached aspects belong to the chart on screen ===");
        before = failures.size();
        theCacheClears();
        report("Part G", before);

        System.out.println();
        System.out.println("=== Part H: folding a layer hides it and changes nothing else ===");
        before = failures.size();
        theLayers();
        report("Part H", before);

        System.out.println();
        System.out.println("=== Part I: a degree tick can be pointed at ===");
        before = failures.size();
        theDegreeTicks();
        report("Part I", before);

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
        // <b>Every body ring inside the zodiac, and the zodiac inside its own scale.</b>
        // The sky used to ride outside the sign plane, which put a transiting body outside the
        // band that says which sign it is in.
        yes("the shells nest outward",
            Globe.SHELL_CORE < Globe.SHELL_NATAL
                && Globe.SHELL_NATAL < Globe.SHELL_PARTNER
                && Globe.SHELL_PARTNER < Globe.SHELL_SKY
                && Globe.SHELL_SKY < Globe.SHELL_HOUSE
                && Globe.SHELL_HOUSE < Globe.SHELL_BOUND
                && Globe.SHELL_BOUND < Globe.SHELL_SIGN_INNER
                && Globe.SHELL_SIGN_INNER < Globe.SHELL_SIGN_OUTER
                && Globe.SHELL_SIGN_OUTER < Globe.SHELL_DECAN
                && Globe.SHELL_DECAN < Globe.SHELL_TICK);
        yes("every body ring is inside the zodiac plane",
            Globe.SHELL_SKY < Globe.SHELL_SIGN_INNER
                && Globe.SHELL_PARTNER < Globe.SHELL_SIGN_INNER
                && Globe.SHELL_NATAL < Globe.SHELL_SIGN_INNER);
        // <b>A filled shell closes the sphere.</b> It used to stop short, which left a
        // hole at each end; the cells at the pole degenerate to triangles and tile it.
        near("a filled shell reaches the pole", Math.PI / 2, Globe.FILL_SPAN, 1e-12);
        double[] north = Globe.onShell(0, 0, Globe.SHELL_HOUSE,
            Globe.SHELL_HOUSE * Math.sin(Globe.FILL_SPAN));
        double[] alsoNorth = Globe.onShell(217.0, 0, Globe.SHELL_HOUSE,
            Globe.SHELL_HOUSE * Math.sin(Globe.FILL_SPAN));
        // Every longitude arrives at the same point there, which is what makes the polar
        // cells triangles rather than overlapping quads.
        near("every longitude meets at the pole, x", north[0], alsoNorth[0], 1e-9);
        near("every longitude meets at the pole, y", north[1], alsoNorth[1], 1e-9);
        near("every longitude meets at the pole, z", north[2], alsoNorth[2], 1e-9);
        near("and the pole is on the shell", Globe.SHELL_HOUSE, north[1], 1e-9);

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

        // <b>A boundary meridian does reach the pole, and that is a different call.</b> House
        // cusps and sign divisions cut the whole sky, so they are drawn at PI/2 while the
        // wireframe ones stop short - one method, two spans, and nothing was asserting that
        // the span argument was read at all.
        double[][] full = Globe.meridian(30.0, origin, Globe.SHELL_HOUSE, 26, Math.PI / 2);
        near("a boundary meridian reaches the north pole",
            Globe.SHELL_HOUSE, full[full.length - 1][1], 1e-9);
        near("and the south pole", -Globe.SHELL_HOUSE, full[0][1], 1e-9);
        for (double[] q : full) {
            near("every point of it is still on the shell", Globe.SHELL_HOUSE,
                Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2]), 1e-9);
        }
        // And the default overload is the short one, which is what the wireframe wants.
        near("the default span is the wireframe span", Globe.MERIDIAN_SPAN,
            Math.asin(mer[mer.length - 1][1] / Globe.SHELL_SIGN_OUTER), 1e-9);
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

    /**
     * A cache that outlives its chart draws the previous reading over the current one.
     *
     * <b>The worst kind of wrong.</b> The aspects are computed once and held between frames -
     * without that, dragging the globe recomputed two and a half thousand pairs sixty times a
     * second - but a cache is only ever as good as the thing that empties it. A stale one here
     * does not crash or look broken: it draws a confident, plausible network of aspects
     * belonging to a chart the reader has already moved on from.
     *
     * Removing the invalidation from updateChartData survived every other check in this suite.
     * That is the definition of an untested rule, so it is tested by the shape of the thing
     * rather than by its effect: the field is watched directly, because "the chart changed" is
     * exactly the moment there is nothing on screen to compare against.
     */
    private static void theCacheClears() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);

            java.lang.reflect.Field fc = SkymapPanel.class.getDeclaredField("globeChords");
            fc.setAccessible(true);

            // It computes once and then holds - which is the point of it existing.
            final int[] calls = new int[1];
            int[][] first = panel.globeChords(() -> {
                calls[0]++;
                return new int[][] {{0, 1, 2, 0xFF00FF00}};
            });
            int[][] second = panel.globeChords(() -> {
                calls[0]++;
                return new int[][] {{0, 3, 4, 0xFF0000FF}};
            });
            eq("the aspects are built once, not once a frame", 1, calls[0]);
            yes("and the held list is handed back unchanged", first == second);

            // Every door that changes what an aspect is has to empty it.
            fc.set(panel, new int[][] {{0, 1, 2, 0}});
            panel.invalidateGlobeChords();
            yes("invalidating empties the cache", fc.get(panel) == null);

            fc.set(panel, new int[][] {{0, 1, 2, 0}});
            javax.swing.SwingUtilities.invokeAndWait(() -> panel.updateChartData());
            yes("recasting the chart empties the cache", fc.get(panel) == null);

            fc.set(panel, new int[][] {{0, 1, 2, 0}});
            javax.swing.SwingUtilities.invokeAndWait(() -> panel.reloadAspectSelection());
            yes("changing which aspects are shown empties the cache", fc.get(panel) == null);

            // <b>And the filter must NOT empty it.</b> It decides which families are drawn,
            // not which pairs are in aspect, so it is applied when drawing - which is what
            // makes flipping it a repaint rather than a recompute of every pair.
            java.lang.reflect.Field ff = SkymapPanel.class.getDeclaredField("aspectFilter");
            ff.setAccessible(true);
            String wasFilter = (String) ff.get(panel);
            try {
                ff.set(panel, "Natal-Natal");
                yes("Natal-Natal draws the natal aspects", panel.drawsNatalAspects());
                yes("and not the cross-chart ones", !panel.drawsCrossAspects());
                ff.set(panel, "Transit-Natal");
                yes("Transit-Natal drops the natal aspects", !panel.drawsNatalAspects());
                ff.set(panel, "Both");
                yes("Both draws the natal aspects", panel.drawsNatalAspects());
            } finally {
                ff.set(panel, wasFilter);
            }

            // And having been emptied, it really does build again rather than hand back null.
            calls[0] = 0;
            int[][] rebuilt = panel.globeChords(() -> {
                calls[0]++;
                return new int[][] {{0, 5, 6, 0xFFFF0000}};
            });
            eq("an emptied cache builds again", 1, calls[0]);
            eq("and hands back what was built", 5, rebuilt[0][1]);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * A folded layer stops being drawn, and the chart underneath it is untouched.
     *
     * <b>The whole point of the distinction.</b> Partner and Sky change what the chart is -
     * opening the partner ring makes it a synastry and the engine has to be told - so they go
     * through ChartMode. These layers change only what is drawn of it. If folding one moved
     * the mode there would be a second writer on that field, which is the defect this panel
     * has shipped twice: a control that quietly disagrees with the chart it drew.
     *
     * Also asserted: a layer folds gradually rather than blinking out, because that is what
     * makes it read as folding away rather than as something failing to draw.
     */
    private static void theLayers() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);

            java.lang.reflect.Field fm = SkymapPanel.class.getDeclaredField("chartMode");
            fm.setAccessible(true);

            // Everything starts open; a reader who has folded nothing sees the whole chart.
            for (SkymapPanel.Layer layer : SkymapPanel.Layer.values()) {
                yes(layer + " starts open", panel.layerWanted(layer));
                yes(layer + " starts drawn", panel.layerShown(layer));
                near(layer + " starts fully open", 1.0, panel.layerOpen(layer), 1e-9);
            }

            Settings.setAnimateRings(false);
            try {
                for (SkymapPanel.Layer layer : SkymapPanel.Layer.values()) {
                    Object modeBefore = fm.get(panel);

                    panel.setLayer(layer, false);
                    yes(layer + " reports folded the moment it is asked",
                        !panel.layerWanted(layer));
                    yes(layer + " is not drawn once folded", !panel.layerShown(layer));
                    near(layer + " is fully folded", 0.0, panel.layerOpen(layer), 1e-9);

                    // <b>And nothing else moved.</b> A layer is a drawing decision.
                    yes("folding " + layer + " leaves the chart mode alone",
                        fm.get(panel) == modeBefore);

                    // Its neighbours are untouched, so one chip cannot fold two things.
                    for (SkymapPanel.Layer other : SkymapPanel.Layer.values()) {
                        if (other != layer) {
                            yes("folding " + layer + " leaves " + other + " open",
                                panel.layerWanted(other));
                        }
                    }

                    panel.setLayer(layer, true);
                    yes(layer + " comes back", panel.layerWanted(layer)
                        && panel.layerShown(layer));
                }
            } finally {
                Settings.setAnimateRings(true);
            }

            // <b>It folds rather than blinking out.</b> With motion on, a layer caught
            // mid-fold is partly there - which is what tells the reader it is leaving rather
            // than that something failed to draw.
            panel.setLayer(SkymapPanel.Layer.DECANS, false);
            double partway = -1;
            long deadline = System.currentTimeMillis() + 1500;
            while (System.currentTimeMillis() < deadline) {
                double v = panel.layerOpen(SkymapPanel.Layer.DECANS);
                if (v > 0.05 && v < 0.95) {
                    partway = v;
                    break;
                }
                Thread.sleep(8);
            }
            yes("a folding layer passes through the middle, saw " + partway, partway > 0);
            panel.setLayer(SkymapPanel.Layer.DECANS, true);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * Every degree on the scale is findable at the pixel its tick is drawn at.
     *
     * <b>Three hundred and sixty targets on one ring, so this is where a hit test drifts.</b>
     * The tick and the test derive their position the same way and from the same radius; if
     * they ever stop doing that the scale becomes decorative, and a reader pointing at a
     * degree gets its neighbour without anything looking wrong.
     *
     * Also: the scale answers nothing when its layer is folded. A tick that is not drawn but
     * is still clickable is a target the reader cannot see.
     */
    private static void theDegreeTicks() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);

            Globe cam = new Globe();
            int w = 1000;
            int h = 1000;
            double origin = panel.pinLongitude();
            double inner = Globe.SHELL_SIGN_OUTER + 0.03;

            int tested = 0;
            for (double yaw : new double[] {0.0, 1.7, 3.9}) {
                cam.yaw = yaw;
                for (int d = 0; d < 360; d += 7) {
                    double[] pt = Globe.onShell(d + 0.5, origin, inner + 0.09, 0.0);
                    Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
                    if (!q.visible) {
                        continue;
                    }
                    tested++;
                    int got = GlobeRenderer.degreeAt(cam, w, h, panel,
                        (int) Math.round(q.x), (int) Math.round(q.y));
                    eq("pointing at degree " + d + " finds it (yaw=" + yaw + ")", d, got);
                }
            }
            yes("the sweep reached some ticks: " + tested, tested > 100);

            // Well inside the globe there is no scale, so nothing should answer.
            cam.yaw = 0;
            eq("the middle of the globe is not a degree", -1,
                GlobeRenderer.degreeAt(cam, w, h, panel, w / 2, h / 2));

            // <b>House numbers are targets too, and by the same rule.</b> Pointing at one
            // carves that house through the sphere, so a label that cannot be pointed at is a
            // feature with no door - and the reader will try, because it looks like a button.
            double[] cusps = panel.activeCusps;
            java.lang.reflect.Method labelAt = GlobeRenderer.class.getDeclaredMethod(
                "houseLabelAt", double[].class, int.class, double.class);
            labelAt.setAccessible(true);
            int found = 0;
            for (double yaw : new double[] {0.0, 2.2, 4.4}) {
                cam.yaw = yaw;
                for (int house = 1; house <= 12; house++) {
                    double[] at = (double[]) labelAt.invoke(null, cusps, house, origin);
                    Globe.Projected q = cam.project(at[0], at[1], at[2], w, h);
                    if (!q.visible) {
                        continue;
                    }
                    found++;
                    eq("pointing at house " + house + " finds it (yaw=" + yaw + ")", house,
                        GlobeRenderer.houseNumberAt(cam, w, h, panel,
                            (int) Math.round(q.x), (int) Math.round(q.y)));
                }
            }
            yes("the house sweep reached some labels: " + found, found > 20);

            // <b>And the label is where it ought to be, stated twice.</b> The sweep above
            // proves the painter and the hit test agree, and it cannot prove more than that:
            // both of them, and the check, ask houseLabelAt, so moving that method moves all
            // three together and the sweep stays green. Mutating the label onto the cusp
            // instead of the middle of the house survived it. So the position is written out
            // here independently - the same trick Part J uses on the ring chain, and the only
            // way a shared accessor can be pinned rather than merely agreed with.
            for (int house = 1; house <= 12; house++) {
                double from = cusps[house];
                double to = cusps[house == 12 ? 1 : house + 1];
                double span = ((to - from) % 360.0 + 360.0) % 360.0;
                double expect = from + span / 2.0;
                double[] at = (double[]) labelAt.invoke(null, cusps, house, origin);
                double[] want = Globe.onShell(expect, origin, Globe.SHELL_HOUSE - 0.10, 0.0);
                near("house " + house + " is labelled at its midpoint, x", want[0], at[0], 1e-9);
                near("house " + house + " is labelled at its midpoint, y", want[1], at[1], 1e-9);
                near("house " + house + " is labelled at its midpoint, z", want[2], at[2], 1e-9);
                // Halfway means halfway: as far from one cusp as from the other.
                near("house " + house + " label sits between its cusps",
                    Globe.separation(expect, from), Globe.separation(expect, to), 1e-9);
            }

            cam.yaw = 0;
            panel.setLayer(SkymapPanel.Layer.HOUSES, false);
            Thread.sleep(1200);
            double[] one = (double[]) labelAt.invoke(null, cusps, 1, origin);
            Globe.Projected qh = cam.project(one[0], one[1], one[2], w, h);
            eq("folded houses answer nothing", -1, GlobeRenderer.houseNumberAt(cam, w, h,
                panel, (int) Math.round(qh.x), (int) Math.round(qh.y)));
            panel.setLayer(SkymapPanel.Layer.HOUSES, true);

            // <b>Folded means unclickable.</b> A target the reader cannot see is worse than
            // no target: it answers when they meant to click through it.
            panel.setLayer(SkymapPanel.Layer.DEGREES, false);
            Thread.sleep(1200);
            double[] pt = Globe.onShell(90.5, origin, inner + 0.09, 0.0);
            Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
            eq("a folded scale answers nothing", -1, GlobeRenderer.degreeAt(cam, w, h, panel,
                (int) Math.round(q.x), (int) Math.round(q.y)));
            panel.setLayer(SkymapPanel.Layer.DEGREES, true);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
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
