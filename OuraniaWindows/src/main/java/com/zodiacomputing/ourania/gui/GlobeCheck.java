package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;

import com.zodiacomputing.ourania.astro.Aspects;

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
        // Never the reader's own settings file: a suite that generates a chart persists it,
        // and one of these once overwrote a saved birth chart. See Settings.useScratchFile.
        Settings.useScratchFile();
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
        System.out.println("=== Part K: the globe lights the ring the reader is pointing at ===");
        before = failures.size();
        theGlobeLightsOneRing();
        report("Part K", before);

        System.out.println();
        System.out.println("=== Part L: the mansion band fits, and names what it points at ===");
        before = failures.size();
        theMansionBand();
        report("Part L", before);

        System.out.println();
        System.out.println("=== Part M: a chart's band survives being turned ===");
        before = failures.size();
        theBandWhileTurning();
        report("Part M", before);

        System.out.println();
        System.out.println("=== Part Q: taking a chart out does not move the others ===");
        before = failures.size();
        theDecksHoldStill();
        report("Part Q", before);

        System.out.println();
        System.out.println("=== Part P: stacked rings line their degrees up ===");
        before = failures.size();
        theStackedRings();
        report("Part P", before);

        System.out.println();
        System.out.println("=== Part O: an aspect goes over the middle, not through it ===");
        before = failures.size();
        theArcGoesOver();
        report("Part O", before);

        System.out.println();
        System.out.println("=== Part N: a chord through the far side is dimmer than one in front ===");
        before = failures.size();
        theChordsFadeWithDepth();
        report("Part N", before);

        System.out.println();
        System.out.println("=== Part J: a band is cut wherever it would lie across itself ===");
        before = failures.size();
        bandsDoNotCrossThemselves();
        report("Part J", before);

        System.out.println();
        System.out.println("=== Part R: the houses are painted on the sphere ===");
        before = failures.size();
        theHousesTakeTheSphere();
        report("Part R", before);

        System.out.println();
        System.out.println("=== Part S: three ribbons on one sphere, and one egg of arcs ===");
        before = failures.size();
        theRibbonsBloom();
        report("Part S", before);

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

        // <b>And the other way, which now reaches the underside.</b> David, 2026-09-18: "the
        // chart when in globe mode has the houses going the wrong direction" shut the lower half
        // off; David, 2026-09-19: "i used to be able to tilt the chart more than one way" asked
        // for it back. Both halves are reachable, the mirror is handled in the projection, and
        // the camera still never rests edge-on, where the plane collapses to a line.
        for (int i = 0; i < 200; i++) {
            g.drag(0, -40, 800);
        }
        yes("dragging the other way reaches under the chart's plane: pitch " + g.pitch,
            g.pitch <= -Globe.MIN_PITCH + 1e-9 && g.pitch >= -Globe.MAX_PITCH - 1e-9);

        // <b>Which way round the hand works, pinned in the reader's words.</b> David,
        // 2026-09-20: "when the mouse pulls upward we are shown the bottom on view and pulled
        // down the top". Swing's dy is negative when the pointer moves up the panel, so that is
        // the direction that takes the camera under the chart - and this says so out loud,
        // because a sign flipped in drag() would still leave every other tilt assertion green.
        Globe hand = new Globe();
        for (int i = 0; i < 40; i++) {
            hand.drag(0, -20, 800);                       // pointer pulled UP the panel
        }
        yes("pulling the mouse up shows the underside: pitch " + hand.pitch,
            hand.pitch < 0 && hand.fromBelow());
        for (int i = 0; i < 40; i++) {
            hand.drag(0, 20, 800);                        // pointer pushed DOWN the panel
        }
        yes("pulling it down shows the top again: pitch " + hand.pitch,
            hand.pitch > 0 && !hand.fromBelow());

        // <b>Through the band in one motion, never resting in it.</b> A drag from well above
        // the plane to well below it is walked in single steps, and no step may leave the
        // camera edge-on: that is the one tilt where the houses cannot be read at all.
        Globe cross = new Globe();
        cross.pitch = 0.4;
        boolean rested = false;
        boolean reached = false;
        for (int i = 0; i < 60; i++) {
            cross.drag(0, -6, 800);
            if (Math.abs(cross.pitch) < Globe.MIN_PITCH - 1e-9) {
                rested = true;
            }
            if (cross.pitch < 0) {
                reached = true;
            }
        }
        yes("a steady drag crosses the plane rather than stopping at it", reached);
        yes("and never rests edge-on on the way through", !rested);
        for (int i = 0; i < 60; i++) {
            cross.drag(0, 6, 800);
        }
        yes("and comes back up again", cross.pitch > 0);
        try {
            housesTurnTheRightWay();
        } catch (Exception e) {
            yes("the house labels can be placed to be measured: " + e, false);
        }
    }

    /**
     * At every tilt a drag can reach, the houses run counterclockwise as the reader sees them.
     *
     * <b>Measured on the screen, through the real label placement.</b> Each house number is put
     * where GlobeRenderer puts it and projected through the camera; the signed turn from house 1
     * round to house 12 is summed about the projected centre. Screen y runs down, so a chart that
     * reads the right way - counterclockwise, as on the flat wheel - totals -360 degrees, and a
     * mirrored one +360. Equal houses from an Ascendant at 0 degrees Aries, pinned as the flat
     * wheel pins it; the question is the camera, not the house system.
     */
    private static void housesTurnTheRightWay() throws Exception {
        java.lang.reflect.Method label = GlobeRenderer.class.getDeclaredMethod(
            "houseLabelAt", double[].class, int.class, double.class);
        label.setAccessible(true);
        double[] cusps = new double[13];
        for (int i = 1; i <= 12; i++) {
            cusps[i] = (i - 1) * 30.0;
        }
        double origin = 0.0;
        int w = 900;
        int h = 900;
        int wrong = 0;
        double worstTilt = Double.NaN;
        for (double tilt = Globe.MIN_PITCH; tilt <= Globe.MAX_PITCH + 1e-9; tilt += 0.05) {
          for (double side : new double[] {1.0}) {
            double pitch = tilt * side;
            for (double yaw = 0.0; yaw < 2 * Math.PI; yaw += Math.PI / 3) {
                Globe cam = new Globe();
                cam.pitch = pitch;
                cam.yaw = yaw;
                Globe.Projected c = cam.project(0, 0, 0, w, h);
                double total = 0.0;
                double[] prev = null;
                for (int k = 0; k <= 12; k++) {
                    int house = k == 12 ? 1 : k + 1;
                    double[] p = (double[]) label.invoke(null, cusps, house, origin);
                    Globe.Projected q = cam.project(p[0], p[1], p[2], w, h);
                    double[] here = {Math.atan2(q.y - c.y, q.x - c.x)};
                    if (prev != null) {
                        double d = here[0] - prev[0];
                        while (d > Math.PI) {
                            d -= 2 * Math.PI;
                        }
                        while (d <= -Math.PI) {
                            d += 2 * Math.PI;
                        }
                        total += d;
                    }
                    prev = here;
                }
                if (Math.toDegrees(total) > 0) {
                    wrong++;
                    worstTilt = pitch;
                }
            }
          }
        }
        yes("the houses run counterclockwise at every tilt above the chart's plane and every "
            + "turn; wrong at " + wrong + " views, e.g. pitch " + worstTilt, wrong == 0);

        // <b>Nothing jumps when the camera crosses the plane.</b> This is the assertion David's
        // complaint earned. The underside was briefly rendered mirrored, so that the houses
        // would keep running counterclockwise down there - and mirroring one side of a boundary
        // is a discontinuity at it: "when i tilt up the signs shouldnt move at all you have them
        // on one side and then when tilting over the horizon they appear to shoow up instantly
        // on the other side."
        //
        // Measured as the reader would see it: the same twelve house numbers projected either
        // side of the crossing, at the two tilts a drag actually visits. Edge-on the whole plane
        // is nearly a line, so both frames are nearly the same picture and every point should
        // barely have moved. A mirror puts each of them a full diameter away, which no tolerance
        // hides.
        Globe justAbove = new Globe();
        justAbove.pitch = Globe.MIN_PITCH;
        Globe justBelow = new Globe();
        justBelow.pitch = -Globe.MIN_PITCH;
        double worstJump = 0.0;
        for (int i = 1; i <= 12; i++) {
            double[] pt = (double[]) label.invoke(null, cusps, i, origin);
            Globe.Projected up = justAbove.project(pt[0], pt[1], pt[2], w, h);
            Globe.Projected down = justBelow.project(pt[0], pt[1], pt[2], w, h);
            worstJump = Math.max(worstJump, Math.hypot(up.x - down.x, up.y - down.y));
        }
        yes(String.format("crossing the plane moves nothing on the panel: worst house number "
            + "moves %.1f px of %d", worstJump, w), worstJump < w / 20.0);

        // And the view itself, stated: the back of the glass reads in reverse, which is what it
        // is. The Ascendant stays on the left on both sides - it is the order that turns over,
        // not the picture that jumps across.
        Globe below = new Globe();
        below.pitch = -0.32;
        Globe.Projected mid = below.project(0, 0, 0, w, h);
        double[] asc = (double[]) label.invoke(null, cusps, 1, origin);
        Globe.Projected qa = below.project(asc[0], asc[1], asc[2], w, h);
        Globe above = new Globe();
        above.pitch = 0.32;
        Globe.Projected midAbove = above.project(0, 0, 0, w, h);
        Globe.Projected qaAbove = above.project(asc[0], asc[1], asc[2], w, h);
        yes("the first house stays on the left above the plane: " + (int) qaAbove.x
            + " against " + (int) midAbove.x, qaAbove.x < midAbove.x);
        yes("and on the left below it too - nothing swaps sides: " + (int) qa.x
            + " against " + (int) mid.x, qa.x < mid.x);
        yes("the camera knows which side it is on",
            below.fromBelow() && !above.fromBelow());

        // The underside reads clockwise, and that is the view rather than a fault: it is what
        // the back of a painted window shows. Stated here so that a later change which quietly
        // mirrors it again has to come through this line.
        Globe under = new Globe();
        under.pitch = -0.32;
        Globe.Projected c = under.project(0, 0, 0, w, h);
        double[] p1 = (double[]) label.invoke(null, cusps, 1, origin);
        double[] p4 = (double[]) label.invoke(null, cusps, 4, origin);
        Globe.Projected q1 = under.project(p1[0], p1[1], p1[2], w, h);
        Globe.Projected q4 = under.project(p4[0], p4[1], p4[2], w, h);
        double turn = Math.atan2(q4.y - c.y, q4.x - c.x) - Math.atan2(q1.y - c.y, q1.x - c.x);
        while (turn > Math.PI) {
            turn -= 2 * Math.PI;
        }
        while (turn <= -Math.PI) {
            turn += 2 * Math.PI;
        }
        yes("from below the chart reads in reverse, as the back of the glass does", turn > 0);
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
        // <b>The three chart ribbons no longer nest - they share one shell.</b> Until
        // 2026-09-21 they were 1.20, 1.50 and 1.80, and that nesting is exactly what stopped
        // them reading as a stack: the depth term scales with the radius, so the widest ring
        // straddles the narrower ones at any tilt however far it is lifted. They now ride one
        // shell at three latitudes, which is asserted in Part S; what still has to nest is
        // everything outside them.
        yes("the shells nest outward",
            Globe.SHELL_CORE < Globe.SHELL_CHART
                && Globe.SHELL_CHART < Globe.SHELL_HOUSE
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
            // <b>Let the chart settle first.</b> A new window casts its chart on a worker,
            // and a validity array written before that lands is quietly overwritten - which
            // showed up as the check returning a different total on consecutive runs, the one
            // symptom that makes every other number in a suite untrustworthy.
            Thread.sleep(2500);
            double[] lon = panel.natalRing.lon;
            boolean[] valid = panel.natalRing.valid;
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
            double[] tlon = panel.outerRing.lon;
            boolean[] tvalid = panel.outerRing.valid;
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

            // <b>Both ring layouts, because either is a plane the hit test has to know.</b>
            // The rings can be crossed - the partner tipped one way and the sky the other - or
            // stacked, one just above the natal plane and one just below. A body drawn at a
            // height and clicked at zero is the same see-it-cannot-click-it defect as a body
            // drawn on a tilt and clicked flat, and the layout is a setting, so the sweep runs
            // under both rather than under whichever one happens to be saved.
            boolean layoutWas = Settings.globeStackedRings();
            for (boolean stacked : new boolean[] {false, true}) {
                Settings.setGlobeStackedRings(stacked);
                String plan = stacked ? "stacked" : "crossed";
                for (double yaw : new double[] {0.0, 1.1, 2.4, 4.9}) {
                    for (double pitch : new double[] {-0.9, 0.0, 0.42, 0.87}) {
                        cam.yaw = yaw;
                        cam.pitch = pitch;
                        for (int ring = 0; ring <= 1; ring++) {
                            double[] rlon = ring == 0 ? lon : tlon;
                            boolean[] rvalid = ring == 0 ? valid : tvalid;
                            // The deck the painter would put this wheel on, not the wheel
                            // index - the two part company the moment a chart is taken out.
                            int deck = panel.ringDeck(ring);
                            double incline = GlobeRenderer.inclinationOf(deck, stacked);
                            double lift = GlobeRenderer.liftOf(deck, stacked);
                            int[] level = Globe.stackLevels(rlon, rvalid, 7.0);
                            for (int i = 0; i < rlon.length; i++) {
                                double[] p = Globe.onShell(rlon[i], origin, shells[ring],
                                    lift + level[i] * Globe.STACK_STEP, incline);
                                Globe.Projected q = cam.project(p[0], p[1], p[2], w, h);
                                if (!q.visible) {
                                    continue;
                                }
                                int hit = GlobeRenderer.bodyAt(cam, w, h, panel,
                                    (int) Math.round(q.x), (int) Math.round(q.y));
                                yes("clicking " + plan + " ring " + ring + " body " + i
                                    + " where it is drawn finds something (yaw=" + yaw
                                    + " pitch=" + pitch + ")", hit >= 0);
                            }
                        }
                    }
                }
            }
            Settings.setGlobeStackedRings(layoutWas);

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
    /**
     * The twelve house faces are actually painted on the sphere.
     *
     * <b>A mutation walked straight past every other assertion here.</b> Removing the call that
     * fills the house lunes left all 11,029 checks green: the winding is measured through the
     * label placement, the tilt through the camera, and none of it ever asks whether the
     * surface a reader is looking at exists. So this paints two frames that differ in one
     * setting and measures the difference where only the house faces live - the sphere above
     * the chart's plane, outside the rings and inside the silhouette.
     *
     * Measured as a count of pixels that changed rather than as a mean, because a mean over a
     * mostly-black frame is dominated by the black and a very translucent wedge - which these
     * deliberately are - moves it by almost nothing.
     */
    private static void theHousesTakeTheSphere() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);

            boolean was = Settings.globeHouseFill();
            try {
                Settings.setGlobeHouseFill(true);
                java.awt.image.BufferedImage on = paintGlobe(panel, 0.32);
                Settings.setGlobeHouseFill(false);
                java.awt.image.BufferedImage off = paintGlobe(panel, 0.32);

                int changed = inkBetween(on, off, 0.0, 1.0);
                yes("switching the house fill on changes the picture: " + changed + " pixels",
                    changed > 500);

                // And the change is on the sphere, not only in the plane: the lower cap is
                // below every ring, so nothing but the house faces can have painted there.
                int lowerCap = inkBetween(on, off, 0.62, 0.95);
                yes("and reaches the sphere below the rings: " + lowerCap + " pixels",
                    lowerCap > 100);
            } finally {
                Settings.setGlobeHouseFill(was);
            }
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    private static java.awt.image.BufferedImage paintGlobe(SkymapPanel panel, double pitch)
            throws Exception {
        final int side = 600;
        final java.awt.image.BufferedImage[] out = new java.awt.image.BufferedImage[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            Globe cam = new Globe();
            cam.pitch = pitch;
            java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(
                side, side, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = im.createGraphics();
            g.setColor(new java.awt.Color(10, 12, 16));
            g.fillRect(0, 0, side, side);
            GlobeRenderer.paint(g, cam, side, side, panel, false);
            g.dispose();
            out[0] = im;
        });
        Thread.sleep(120);
        return out[0];
    }

    /** Pixels that visibly differ, between two fractions of the frame's height. */
    private static int inkBetween(java.awt.image.BufferedImage a,
                                  java.awt.image.BufferedImage b,
                                  double fromY, double toY) {
        int n = 0;
        int y0 = (int) (a.getHeight() * fromY);
        int y1 = (int) (a.getHeight() * toY);
        for (int y = y0; y < y1; y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                int d = Math.abs(((p >> 16) & 255) - ((q >> 16) & 255))
                    + Math.abs(((p >> 8) & 255) - ((q >> 8) & 255))
                    + Math.abs((p & 255) - (q & 255));
                if (d > 6) {
                    n++;
                }
            }
        }
        return n;
    }

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

    /**
     * Hovering an aspect lights it on the globe, on its own ring, at the flat wheel's orbs.
     *
     * <b>Two defects, one root.</b> The globe drew every chord alike: pointing at a cell of
     * the aspect grid lit the flat wheel and did nothing at all here, so a reader who had
     * switched views lost the one gesture that says which two points a line joins. And the
     * globe judged every cross-chart chord as a synastry pair, so in a synastry chart the sky
     * ring's orbs were halved on the globe and full on the wheel - the two views disagreeing
     * about what is in aspect, which is the disagreement Part D exists to prevent for
     * positions.
     *
     * The orb assertion names both candidate answers rather than recomputing the chosen one:
     * the sky ring's chord count has to match what natal orbs give and differ from what
     * synastry orbs give, so flipping the flag back fails rather than passes quietly.
     */
    private static void theGlobeLightsOneRing() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);
            set(panel, "chartMode", ChartMode.SYNASTRY);
            set(panel, "showTransitChart", Boolean.TRUE);
            set(panel, "showTriWheel", Boolean.TRUE);
            set(panel, "aspectFilter", "Both");
            panel.updateChartData();
            Thread.sleep(1500);

            yes("the chart under test is a synastry tri-wheel", panel.isSynastryChart());

            // <b>The lit figures are not the hover.</b> A grand trine lights its own legs
            // whether or not anything is being pointed at, which is a separate feature with
            // its own checks - left in, it would answer "lit" here for reasons that have
            // nothing to do with the ring under test.
            set(panel, "autoPatterns", new int[0][]);
            set(panel, "highlightPattern", new int[0]);

            // 1. The hover reaches the globe, and reaches one ring.
            for (int a = 0; a < 12; a += 3) {
                for (int b = 1; b < 12; b += 4) {
                    for (int wheel : new int[] {SkymapPanel.WHEEL_NATAL,
                            SkymapPanel.WHEEL_OUTER, SkymapPanel.WHEEL_SKY}) {
                        String row = wheel == SkymapPanel.WHEEL_NATAL
                            ? com.zodiacomputing.ourania.astro.Bodies.at(a).name : "transit_" + com.zodiacomputing.ourania.astro.Bodies.at(a).name.toLowerCase();
                        panel.setHighlightedAspect(
                            SkymapPanel.aspectHref(row, com.zodiacomputing.ourania.astro.Bodies.at(b).name, "Trine", wheel));
                        for (int other : new int[] {SkymapPanel.WHEEL_NATAL,
                                SkymapPanel.WHEEL_OUTER, SkymapPanel.WHEEL_SKY}) {
                            yes("globe chord " + a + "-" + b + " on ring " + other
                                + (other == wheel ? " lights" : " stays dark")
                                + " when ring " + wheel + " is hovered",
                                panel.lightsChord(a, b, other) == (other == wheel));
                        }
                    }
                }
            }
            panel.setHighlightedAspect(null);

            // 2. The sky ring is judged at natal orbs, as the flat wheel judges it.
            int atNatal = 0;
            int atSynastry = 0;
            int built = 0;
            for (int a = 0; a < SkymapPanel.BODY_COUNT; a++) {
                if (!SkymapPanel.aspecting(a, panel.skyRing.valid)) {
                    continue;
                }
                for (int b = 0; b < SkymapPanel.BODY_COUNT; b++) {
                    if (!SkymapPanel.aspecting(b, panel.natalRing.valid)) {
                        continue;
                    }
                    if (panel.aspectInkFor(panel.skyRing.lon[a], panel.natalRing.lon[b], a, b, false) != null) {
                        atNatal++;
                    }
                    if (panel.aspectInkFor(panel.skyRing.lon[a], panel.natalRing.lon[b], a, b, true) != null) {
                        atSynastry++;
                    }
                }
            }
            // The cache fills when the globe is painted, so paint one frame into an image.
            // Reading the cache is the point: this has to be the array the renderer draws
            // from, not a second computation of it that could agree while the first is wrong.
            java.awt.image.BufferedImage frame = globeFrame(panel);
            for (int[] c : panel.globeChords(() -> new int[0][])) {
                if (c[0] == SkymapPanel.WHEEL_SKY) {
                    built++;
                }
            }
            System.out.println("  sky chords built " + built + "; natal orbs give " + atNatal
                + ", synastry orbs give " + atSynastry);
            yes("the two orb widths disagree, so the choice is a real one",
                atNatal != atSynastry);
            eq("the globe builds the sky ring at the flat wheel's orbs", atNatal, built);

            // 3. And the highlight reaches the paint, not just the predicate. lightsChord is
            // a door the renderer has to walk through; asking the door alone would leave a
            // renderer that never opens it green.
            int a0 = -1;
            int b0 = -1;
            for (int[] c : panel.globeChords(() -> new int[0][])) {
                if (c[0] == SkymapPanel.WHEEL_SKY) {
                    a0 = c[1];
                    b0 = c[2];
                    break;
                }
            }
            yes("there is a sky chord to hover", a0 >= 0);
            if (a0 >= 0) {
                panel.setHighlightedAspect(null);
                java.awt.image.BufferedImage dark = globeFrame(panel);
                panel.setHighlightedAspect(SkymapPanel.aspectHref(
                    "transit_" + com.zodiacomputing.ourania.astro.Bodies.at(a0).name
                        .toLowerCase(),
                    com.zodiacomputing.ourania.astro.Bodies.at(b0).name, "Trine",
                    SkymapPanel.WHEEL_SKY));
                java.awt.image.BufferedImage bright = globeFrame(panel);
                panel.setHighlightedAspect(null);
                System.out.println("  hovering a sky chord changes "
                    + differingPixels(dark, bright) + " pixels of the globe");
                yes("hovering a sky chord changes what the globe paints",
                    differingPixels(dark, bright) > 40);
            }
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * A chart's band costs less while the reader is dragging, and still reads as a band.
     *
     * <b>Cheaper is only half the rule.</b> Three filled bands are two hundred and
     * eighty-eight quads a frame, and measured, they took a drag at 1100 pixels from 24ms to
     * 40ms - so the fill is left out while turning, the way the sign shells already are. But
     * "leave it out" and "leave it visible" are two halves of one rule: pin only the cheapness
     * and someone can delete the rims and still pass, at which point a chart's plane vanishes
     * exactly while the reader is turning the globe to look for it.
     *
     * <b>The bodies are switched off, not a layer folded.</b> The first version of this counted
     * gold pixels of the whole scene, and passed under both mutations - the gold it was
     * counting was glyphs and sign shells, and folding Layer.NATAL takes the natal bodies away
     * with the band because they share a guard. Emptying the validity arrays leaves the rings
     * drawn and nothing riding on them, so what changes between two frames is the band and
     * only the band.
     */
    private static void theBandWhileTurning() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);
            set(panel, "chartMode", ChartMode.SYNASTRY);
            set(panel, "showTransitChart", Boolean.TRUE);
            set(panel, "showTriWheel", Boolean.TRUE);
            panel.updateChartData();
            Thread.sleep(1500);

            // Everything that is not a band, out of the frame.
            for (SkymapPanel.Layer layer : SkymapPanel.Layer.values()) {
                if (layer != SkymapPanel.Layer.NATAL) {
                    panel.setLayer(layer, false);
                }
            }
            Thread.sleep(900);
            for (String field : new String[] {"natalRing.valid", "outerRing.valid", "skyRing.valid"}) {
                set(panel, field, new boolean[SkymapPanel.BODY_COUNT]);
            }

            java.awt.image.BufferedImage rest = bandFrame(panel, false);
            java.awt.image.BufferedImage turning = bandFrame(panel, true);
            int fill = differingPixels(rest, turning);
            System.out.println("  the fill the bands drop while turning: " + fill + " px");
            yes("a band's fill is left out while the globe is turning", fill > 3000);

            // What is left when the bands are gone entirely - so what the turning frame still
            // paints over it is the rims, with nothing else able to account for it.
            panel.setLayer(SkymapPanel.Layer.NATAL, false);
            set(panel, "showTransitChart", Boolean.FALSE);
            set(panel, "showTriWheel", Boolean.FALSE);
            Thread.sleep(900);
            java.awt.image.BufferedImage bare = bandFrame(panel, true);
            int rims = differingPixels(turning, bare);
            System.out.println("  what a turning band still paints: " + rims + " px");
            yes("a turning band still paints its rims", rims > 600);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /** One frame on a flat ground, so a band's own ink is what differs between two of them. */
    private static java.awt.image.BufferedImage bandFrame(SkymapPanel panel, boolean turning) {
        java.awt.image.BufferedImage frame = new java.awt.image.BufferedImage(
            700, 700, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D gg = frame.createGraphics();
        gg.setColor(new java.awt.Color(10, 12, 16));
        gg.fillRect(0, 0, 700, 700);
        GlobeRenderer.paint(gg, new Globe(), 700, 700, panel, turning);
        gg.dispose();
        return frame;
    }

    /**
     * Taking one chart out does not move the others between decks.
     *
     * <b>This is the complaint, stated as an assertion.</b> David: "something confusing is
     * happening when I select or deselect chart a chart b and sky - I don't know if it's that
     * when one chart is deselected it changes levels, or that the same chart keeps being
     * deselected even though a different button is pressed." It was the first: the engine's
     * three slots are roles, and which chart sits in which slot moves with the selection, so
     * the globe - which drew straight from the slot - sent the remaining charts climbing
     * between rings whenever a chip was pressed. A reader cannot tell that apart from having
     * pressed the wrong chip.
     *
     * So the deck is a fact about the chart and not about the slot, and this walks every
     * combination of the two chips asking the only questions that matter: Chart A in the
     * middle whenever it is drawn, Chart B below whenever it is drawn, the sky above whenever
     * it is transits rather than the whole chart - and never two wheels on one deck.
     */
    private static void theDecksHoldStill() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);
            com.zodiacomputing.ourania.astro.ChartSubject a =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart A",
                    java.time.ZonedDateTime.of(1982, 8, 10, 15, 1, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Philadelphia", 39.95, -75.16, "America/New_York", false);
            com.zodiacomputing.ourania.astro.ChartSubject b =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart B",
                    java.time.ZonedDateTime.of(1979, 3, 22, 8, 40, 0, 0,
                        java.time.ZoneId.of("Europe/London")),
                    "London", 51.51, -0.13, "Europe/London", false);
            com.zodiacomputing.ourania.astro.ChartSubject sky =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Sky",
                    java.time.ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Philadelphia", 39.95, -75.16, "America/New_York", false);
            java.lang.reflect.Method install = SkymapPanel.class.getDeclaredMethod(
                "installSubjects", com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class);
            install.setAccessible(true);
            install.invoke(panel, a, b, sky);
            java.lang.reflect.Method compose = SkymapPanel.class.getDeclaredMethod(
                "setComposition", boolean.class, boolean.class);
            compose.setAccessible(true);

            for (boolean chartA : new boolean[] {true, false}) {
                for (boolean chartB : new boolean[] {true, false}) {
                    compose.invoke(panel, chartA, chartB);
                    // The mode follows the composition in the app; here it is set beside it,
                    // because this is asking about the decks and not about the routing.
                    set(panel, "chartMode", chartA && chartB
                        ? ChartMode.SYNASTRY : ChartMode.TRANSIT);
                    set(panel, "showTransitChart", Boolean.TRUE);
                    set(panel, "showTriWheel", Boolean.valueOf(chartA && chartB));
                    panel.updateChartData();
                    String state = "A " + (chartA ? "in" : "out")
                        + ", B " + (chartB ? "in" : "out");

                    int inner = panel.ringDeck(SkymapPanel.WHEEL_NATAL);
                    int outer = panel.ringDeck(SkymapPanel.WHEEL_OUTER);
                    int tri = panel.ringDeck(SkymapPanel.WHEEL_SKY);

                    // <b>The sky holds the middle, and the people float either side of it.</b>
                    // Reversed on 2026-09-21, David's call: the sky is the one ring that is
                    // nobody's chart - it is where everything actually is - so it is the frame
                    // and the two charts are what sit above and below it. Before that Chart A
                    // held the middle and the sky rode above, which crowded a synastry's two
                    // people onto one side of the sky.
                    if (chartA || chartB) {
                        eq("with " + state + " the sky ring takes the middle deck",
                            SkymapPanel.DECK_MIDDLE, chartA && chartB ? tri : outer);
                    } else {
                        // <b>Both people out, so there are two skies.</b> The sky is the chart
                        // and holds the middle on the inner wheel; this ring is the sky at the
                        // scrubbed moment and has to go somewhere else, or two rings draw at
                        // one radius in one plane as a single ring holding both.
                        eq("with " + state + " the transiting sky rides above the sky chart",
                            SkymapPanel.DECK_UPPER, outer);
                    }
                    if (chartA) {
                        eq("with " + state + " Chart A rides above the sky",
                            SkymapPanel.DECK_UPPER, inner);
                    } else if (chartB) {
                        eq("with " + state + " Chart B keeps the lower deck",
                            SkymapPanel.DECK_LOWER, inner);
                    } else {
                        eq("with " + state + " the sky is the chart and still takes the middle",
                            SkymapPanel.DECK_MIDDLE, inner);
                    }
                    if (chartA && chartB) {
                        eq("with " + state + " Chart B is on the lower deck",
                            SkymapPanel.DECK_LOWER, outer);
                    }

                    // <b>And never two wheels on one deck.</b> Two rings at one radius in one
                    // plane is one ring holding two charts, which is worse than the shuffling
                    // this replaced.
                    java.util.List<Integer> drawn = new java.util.ArrayList<>();
                    drawn.add(inner);
                    if (panel.outerRingDrawn()) {
                        drawn.add(outer);
                    }
                    if (panel.triRingDrawn()) {
                        drawn.add(tri);
                    }
                    eq("with " + state + " no two drawn rings share a deck",
                        drawn.size(), new java.util.HashSet<Integer>(drawn).size());

                    // <b>And Chart B's lines bow toward its deck.</b> Down wherever Chart B is
                    // the chart on a wheel, up for everything else - the sky's transits to a
                    // promoted Chart B included, because those are the sky's lines.
                    for (int wheel = 0; wheel < 3; wheel++) {
                        boolean partner = panel.ringDeck(wheel) == SkymapPanel.DECK_LOWER;
                        double rise = GlobeRenderer.riseFor(panel, wheel, true);
                        yes("with " + state + " wheel " + wheel + " bows "
                            + (partner ? "down, toward Chart B" : "up"),
                            partner ? rise < 0.0 : rise > 0.0);
                    }
                    near("with " + state + " no line bows at all with arcs off", 0.0,
                        GlobeRenderer.riseFor(panel, 1, false), 0.0);

                    // The radii follow the decks, so a ring that keeps its deck keeps its
                    // place - which is the whole of what was asked for.
                    double[] shells = GlobeRenderer.shellRadii(panel);
                    near("with " + state + " the inner wheel sits at its deck's radius",
                        inner == SkymapPanel.DECK_MIDDLE ? Globe.SHELL_NATAL
                            : (inner == SkymapPanel.DECK_LOWER ? Globe.SHELL_PARTNER
                                : Globe.SHELL_SKY), shells[0], 1e-12);
                }
            }
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * Stacked rings keep the promise the crossed ones cannot.
     *
     * <b>What a reader crosses rings to do is compare a degree.</b> Tilting the partner one way
     * and the sky the other separates the three planes and makes them meet at the Ascendant,
     * which is handsome - and it costs the comparison, because a tilted ring turns longitude
     * into something other than the angle you see. A transit at 15 Leo and a natal planet at 15
     * Leo are over each other at the Ascendant and the Descendant and nowhere else. In between
     * the sky ring swings through eight tenths of a world unit of height, and its degrees skew
     * three and a half degrees round the wheel. Those are the two numbers this measures rather
     * than describes.
     *
     * Stacked, the two outer rings are circles of latitude on their own shells, so every degree
     * keeps the direction it has on the natal ring and a conjunction across charts is one body
     * directly above another. What it gives up is the crossing, which is also asserted: three
     * parallel rings meet nowhere.
     */
    private static void theStackedRings() {
        boolean was = Settings.globeStackedRings();
        try {
            double origin = 0.0;
            double inner = Globe.SHELL_NATAL;
            double outer = Globe.SHELL_SKY;

            // <b>The drift, measured on the layout that has it.</b>
            Settings.setGlobeStackedRings(false);
            double worstCrossed = 0.0;
            for (double lon = 0.0; lon < 360.0; lon += 1.0) {
                worstCrossed = Math.max(worstCrossed,
                    apart(lon, origin, inner, outer, GlobeRenderer.inclinationOf(2, Settings.globeStackedRings()),
                        GlobeRenderer.liftOf(2, Settings.globeStackedRings())));
            }
            System.out.printf("  crossed: a degree on the sky ring is up to %.1f degrees round "
                + "from the same degree on the natal ring%n", worstCrossed);
            yes("crossed rings do not line their degrees up", worstCrossed > 3.0);

            // <b>And its absence on the layout that fixes it.</b>
            Settings.setGlobeStackedRings(true);
            double worstStacked = 0.0;
            for (double lon = 0.0; lon < 360.0; lon += 1.0) {
                worstStacked = Math.max(worstStacked,
                    apart(lon, origin, inner, outer, GlobeRenderer.inclinationOf(2, Settings.globeStackedRings()),
                        GlobeRenderer.liftOf(2, Settings.globeStackedRings())));
            }
            System.out.printf("  stacked: the worst that gap gets is %.2e degrees%n",
                worstStacked);
            yes("stacked rings put every degree over the same degree", worstStacked < 1e-12);

            // <b>The height swing, which is the bigger half of the complaint.</b> The skew
            // above is three degrees and a reader might live with it; a tilted ring also
            // carries the same longitude up and down through most of a world unit as it goes
            // round, so a transit conjunct a natal planet sits well above it in one quarter of
            // the wheel and well below it in the next. Stacked, that distance is one number
            // everywhere.
            Settings.setGlobeStackedRings(false);
            double high = -Double.MAX_VALUE;
            double low = Double.MAX_VALUE;
            for (double lon = 0.0; lon < 360.0; lon += 1.0) {
                double[] sky = Globe.onShell(lon, origin, outer, GlobeRenderer.liftOf(2, Settings.globeStackedRings()),
                    GlobeRenderer.inclinationOf(2, Settings.globeStackedRings()));
                high = Math.max(high, sky[1]);
                low = Math.min(low, sky[1]);
            }
            System.out.printf("  crossed: the sky ring swings through %.2f world units of "
                + "height; stacked it holds %.2f%n", high - low, Globe.LIFT_SKY);
            yes("a crossed ring changes height as it goes round", high - low > 1.0);
            Settings.setGlobeStackedRings(true);

            // Which way round: the sky above this chart, the partner below it. Named rather
            // than implied, because the two constants are a sign apart and nothing else would
            // catch them being swapped.
            yes("the sky rides above the natal plane", GlobeRenderer.liftOf(2, Settings.globeStackedRings()) > 0.0);
            yes("and the partner below it", GlobeRenderer.liftOf(1, Settings.globeStackedRings()) < 0.0);
            near("by the same distance", Math.abs(GlobeRenderer.liftOf(2, Settings.globeStackedRings())),
                Math.abs(GlobeRenderer.liftOf(1, Settings.globeStackedRings())), 1e-12);
            near("with this chart on the plane itself", 0.0, GlobeRenderer.liftOf(0, Settings.globeStackedRings()), 1e-12);
            for (int ring = 0; ring < 3; ring++) {
                near("stacked ring " + ring + " has no tilt left in it", 0.0,
                    GlobeRenderer.inclinationOf(ring, Settings.globeStackedRings()), 1e-12);
            }

            // <b>Parallel means they meet nowhere, which is the trade.</b> Every point of the
            // sky ring stands the same height above every point of the natal one, so there is
            // no Ascendant where all three visibly agree any more - they agree everywhere
            // instead.
            double lowest = Double.MAX_VALUE;
            for (double lon = 0.0; lon < 360.0; lon += 1.0) {
                double[] sky = Globe.onShell(lon, origin, outer, GlobeRenderer.liftOf(2, Settings.globeStackedRings()),
                    GlobeRenderer.inclinationOf(2, Settings.globeStackedRings()));
                double[] natal = Globe.onShell(lon, origin, inner, GlobeRenderer.liftOf(0, Settings.globeStackedRings()),
                    GlobeRenderer.inclinationOf(0, Settings.globeStackedRings()));
                lowest = Math.min(lowest, sky[1] - natal[1]);
                near("the sky ring is level at " + (int) lon + " degrees",
                    Globe.LIFT_SKY, sky[1], 1e-12);
            }
            yes("and always above the natal ring", lowest > 0.0);

            // A lifted ring is still on its shell: the horizontal reach shrinks to pay for the
            // height, the same way a stacked body stays on the sphere rather than floating off
            // it. Without this the ring would stand outside the band drawn under it.
            double[] lifted = Globe.onShell(90.0, origin, outer, Globe.LIFT_SKY, 0.0);
            near("a lifted ring stays on the shell it belongs to", outer,
                Math.sqrt(lifted[0] * lifted[0] + lifted[1] * lifted[1]
                    + lifted[2] * lifted[2]), 1e-12);
        } finally {
            Settings.setGlobeStackedRings(was);
        }
    }

    /**
     * How far round the globe a longitude on an outer ring lands from the same longitude on the
     * natal one, in degrees of apparent angle.
     *
     * Measured as the angle between the two points seen from the axis, which is what a reader
     * looking down at the globe sees as "over it" or "off to one side of it". The heights are
     * thrown away deliberately: the question is whether the two line up around the wheel, not
     * whether one is higher.
     */
    private static double apart(double lon, double origin, double inner, double outer,
            double incline, double lift) {
        double[] a = Globe.onShell(lon, origin, inner, 0.0, 0.0);
        double[] b = Globe.onShell(lon, origin, outer, lift, incline);
        // <b>atan2 of the cross and the dot, not acos of the dot.</b> acos near one is where
        // rounding turns into angle: two points that agree to the last bit came back 1.2e-6
        // degrees apart, which is nothing and is also not zero, and would have needed a
        // threshold chosen to hide it. This form is exact at zero.
        double dot = a[0] * b[0] + a[2] * b[2];
        double cross = a[0] * b[2] - a[2] * b[0];
        return Math.abs(Math.toDegrees(Math.atan2(cross, dot)));
    }

    /**
     * An aspect goes over the middle rather than through it.
     *
     * <b>The arc is a presentational lie and has to be a disciplined one.</b> The true line
     * between two bodies is the chord; the bow is there because the chord hides in the
     * interior, and an opposition - the aspect a reader most wants to see - is the diameter
     * that runs through the exact centre where every other line already is. What keeps the lie
     * honest is that it changes nothing a reader could measure: the arc starts and ends on the
     * two bodies to the last decimal, and its height is not a taste but the one number that
     * puts every apex, whatever the aspect, on the same sphere.
     *
     * <b>The assertion that earned its place is the one about 179 degrees.</b> The obvious way
     * to arc a line on a sphere is a great circle, in the plane through the two bodies and the
     * centre - and that plane does not exist when the bodies are opposed, because three points
     * on a line define no plane. An implementation that used it would be stable at 170, stable
     * at 179, and would flip its arc through ninety degrees somewhere in the last fraction of
     * a degree before opposition, which is exactly the orb where a reader is watching. Bowing
     * upward instead is continuous everywhere, and this walks up to the singularity that is not
     * there to prove it.
     */
    private static void theArcGoesOver() {
        double r = Globe.SHELL_NATAL;
        double origin = 0.0;

        // An opposition: the chord is the diameter, so its middle is the centre of the globe.
        double[] a = Globe.onShell(0.0, origin, r, 0.0);
        double[] b = Globe.onShell(180.0, origin, r, 0.0);
        near("an opposition's chord passes through the centre",
            0.0, length(mid(a, b)), 1e-9);

        double[][] arc = Globe.arc(a, b, 64);
        near("the arc starts on the first body", 0.0, distance(arc[0], a), 1e-12);
        near("and ends on the second", 0.0, distance(arc[arc.length - 1], b), 1e-12);
        near("and its top stands a full radius above the centre",
            r, length(arc[32]), 1e-9);
        yes("above the equator rather than below it", arc[32][1] > 0.0);

        // <b>Every aspect's top on the same sphere.</b> This is what makes the network read as
        // a globe inside the globe rather than as a heap of unrelated bows: the rise is half
        // the chord, and half a chord is exactly the height that puts the apex back on the
        // shell the two bodies sit on.
        double worst = 0.0;
        double over = 0.0;
        for (double sep : new double[] {30, 45, 60, 72, 90, 120, 135, 150, 180}) {
            double[] p = Globe.onShell(0.0, origin, r, 0.0);
            double[] q = Globe.onShell(sep, origin, r, 0.0);
            double[][] path = Globe.arc(p, q, 96);
            worst = Math.max(worst, Math.abs(length(path[48]) - r));
            for (double[] point : path) {
                over = Math.max(over, length(point) - r);
            }
        }
        System.out.printf("  the furthest any apex sits from the shell is %.2e world units%n",
            worst);
        yes("every aspect's arc tops out on the shell its bodies ride",  worst < 1e-9);
        yes("and no part of one leaves that shell", over < 1e-9);

        // <b>Continuous at the opposition, which a great-circle arc would not be.</b>
        double[] near179 = Globe.arc(Globe.onShell(0.0, origin, r, 0.0),
            Globe.onShell(179.99, origin, r, 0.0), 64)[32];
        double[] at180 = Globe.arc(Globe.onShell(0.0, origin, r, 0.0),
            Globe.onShell(180.0, origin, r, 0.0), 64)[32];
        System.out.printf("  the top of a 179.99 arc sits %.4f from the top of a 180 arc%n",
            distance(near179, at180));
        yes("an arb a hundredth of a degree off opposition tops out beside the opposition's",
            distance(near179, at180) < 0.01);

        // Rise zero is the chord, exactly - which is what the reader gets with the arc
        // switched off, and it has to be the old picture rather than a nearly flat curve.
        double[][] flat = Globe.arc(a, b, 8, 0.0);
        double straightest = 0.0;
        for (int i = 0; i <= 8; i++) {
            double t = i / 8.0;
            straightest = Math.max(straightest, distance(flat[i], new double[] {
                a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t}));
        }
        yes("with the arc switched off the line is the chord again", straightest < 1e-12);

        // A conjunction has almost no chord to bow out of, so it stays on the surface.
        double[][] tight = Globe.arc(Globe.onShell(0.0, origin, r, 0.0),
            Globe.onShell(2.0, origin, r, 0.0), 32);
        // <b>Measured against the shell, not against a remembered number.</b> This was a flat
        // 0.03, which was two percent of the 1.20 shell the ribbons used to ride. They ride
        // 1.75 now, so the same two-degree chord is half again as long and its bow cleared the
        // old threshold by a whisker - a red that was the constant's age, not the code's.
        yes("a conjunction barely leaves the ring", tight[16][1] < 0.02 * r);

        // <b>Across two charts, where the bodies are on different shells and different
        // planes.</b> The synastry chord is the one this view exists for, and it is the one
        // where a bow could quietly detach from a glyph - the tilt means the two ends are not
        // symmetrical about anything.
        double[] partner = Globe.onShell(40.0, origin, Globe.SHELL_PARTNER,
            Globe.STACK_STEP, Globe.INCLINE_PARTNER);
        double[] natal = Globe.onShell(220.0, origin, r, 0.0, 0.0);
        double[][] cross = Globe.arc(partner, natal, 48);
        near("a cross-chart arc starts on the partner's body",
            0.0, distance(cross[0], partner), 1e-12);
        near("and ends on this chart's", 0.0, distance(cross[48], natal), 1e-12);
        yes("and rises above both of them",
            cross[24][1] > Math.max(partner[1], natal[1]));

        // <b>A downward arc is the upward one reflected, and nothing else.</b> Chart B's lines
        // bow down so its network sits as a bowl under the sign plane. That only reads as the
        // same family of lines if the bowl is the dome's mirror - same ends, same depth, lowest
        // point on the same shell - rather than some other curve that happens to go down.
        double[][] up = Globe.arc(a, b, 64, Globe.ARC_RISE);
        double[][] down = Globe.arc(a, b, 64, -Globe.ARC_RISE);
        double mirrored = 0.0;
        for (int i = 0; i <= 64; i++) {
            mirrored = Math.max(mirrored, distance(down[i],
                new double[] {up[i][0], -up[i][1], up[i][2]}));
        }
        yes("a downward opposition is the upward one reflected through the plane",
            mirrored < 1e-12);
        yes("its lowest point is below the equator", down[32][1] < 0.0);
        near("and on the same shell as the dome's highest", r, length(down[32]), 1e-9);

        // <b>How far the painted line strays from the arc it stands for.</b> The bow was first
        // drawn as one straight segment per coloured piece, and a polyline is smooth where a
        // curve is lazy and hinged where it is tight - which on an arc is the apex, the part
        // the whole idea is about. This walks the shape the painter actually strokes and
        // measures the worst gap in pixels, rather than trusting that more points are
        // smoother.
        Globe cam = new Globe();
        int size = 900;
        for (double sep : new double[] {60, 120, 180}) {
            double[] e0 = Globe.onShell(0.0, origin, r, 0.0);
            double[] e1 = Globe.onShell(sep, origin, r, 0.0);
            Globe.Projected q0 = cam.project(e0[0], e0[1], e0[2], size, size);
            Globe.Projected q1 = cam.project(e1[0], e1[1], e1[2], size, size);
            int atRest = GlobeRenderer.stepsFor(q0, q1, true, false);
            int dragging = GlobeRenderer.stepsFor(q0, q1, true, true);
            int fine = GlobeRenderer.subdivisions(q0, q1, atRest, false);
            int coarse = GlobeRenderer.subdivisions(q0, q1, dragging, true);
            double still = strayed(cam, e0, e1,
                painted(cam, e0, e1, atRest, fine, size), size);
            double moving = strayed(cam, e0, e1,
                painted(cam, e0, e1, dragging, coarse, size), size);
            // One straight segment per piece is the mutant: it is what the bow was drawn as
            // first, and it is what put visible corners at the apex. Without this the check
            // would pass on any subdivision at all, including none.
            double hinged = strayed(cam, e0, e1, painted(cam, e0, e1, atRest, 1, size), size);
            System.out.printf("  %3.0f degrees: %d pieces of %d stray %.2fpx, %d of %d while "
                + "turning %.2fpx, unsubdivided %.2fpx%n",
                sep, atRest, fine, still, dragging, coarse, moving, hinged);
            yes("at " + (int) sep + " degrees the painted line holds the arc to half a pixel",
                still < 0.5);
            yes("and to a pixel while the globe is being turned", moving < 1.0);
            yes("and the points inside a piece are doing that, not the piece count",
                hinged > 1.0);
        }
    }

    /**
     * The path one aspect line is stroked as, cut up the way GlobeRenderer cuts it.
     *
     * Both numbers come from the painter - how many pieces, and how many points inside one - so
     * the suite measures the shape that ships rather than a second copy of it that could be
     * smooth while the shipped one hinges.
     */
    private static java.awt.geom.Path2D.Double painted(Globe cam, double[] a, double[] b,
            int pieces, int sub, int size) {
        double[][] path = Globe.arc(a, b, pieces * sub);
        java.awt.geom.Path2D.Double whole = new java.awt.geom.Path2D.Double();
        for (int i = 0; i < path.length; i++) {
            Globe.Projected q = cam.project(path[i][0], path[i][1], path[i][2], size, size);
            if (i == 0) {
                whole.moveTo(q.x, q.y);
            } else {
                whole.lineTo(q.x, q.y);
            }
        }
        return whole;
    }

    /**
     * The furthest that path lies from the arc it stands for, in pixels.
     *
     * The path is flattened to a hundredth of a pixel and every point of a finely walked arc
     * is measured against it. Measuring the two by their own parameters would compare points
     * that are not opposite each other and would report a gap where there is none.
     */
    private static double strayed(Globe cam, double[] a, double[] b,
            java.awt.geom.Path2D.Double painted, int size) {
        java.util.List<double[]> flat = new java.util.ArrayList<>();
        double[] seg = new double[6];
        double[] last = null;
        java.awt.geom.PathIterator it = painted.getPathIterator(null, 0.01);
        while (!it.isDone()) {
            int kind = it.currentSegment(seg);
            if (kind == java.awt.geom.PathIterator.SEG_LINETO && last != null) {
                flat.add(new double[] {last[0], last[1], seg[0], seg[1]});
            }
            last = new double[] {seg[0], seg[1]};
            it.next();
        }
        double worst = 0.0;
        for (double[] w : Globe.arc(a, b, 512)) {
            Globe.Projected q = cam.project(w[0], w[1], w[2], size, size);
            double best = Double.MAX_VALUE;
            for (double[] s : flat) {
                best = Math.min(best,
                    java.awt.geom.Line2D.ptSegDist(s[0], s[1], s[2], s[3], q.x, q.y));
            }
            worst = Math.max(worst, best);
        }
        return worst;
    }

    private static double[] mid(double[] a, double[] b) {
        return new double[] {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
    }

    private static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static double distance(double[] a, double[] b) {
        return length(new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]});
    }

    /**
     * An aspect chord dims with how far through the globe it runs.
     *
     * <b>The bodies have dimmed round the back for a long time; the lines between them had
     * not.</b> So a dense chart painted its whole interior at one strength, and the figure in
     * front of the reader had to be picked out of everything behind it - the one complaint a
     * three-shell globe earns that a flat wheel does not.
     *
     * <b>Measured off the pixels the painter actually laid down.</b> This walks the chords the
     * renderer builds - its own {@code buildChords}, not a second copy of the aspect rule -
     * projects each midpoint the way the painter projects it, and samples that pixel. Then it
     * compares the deepest quarter against the shallowest. Reading the fade function itself
     * would only prove arithmetic; the thing worth pinning is that the ramp reaches the screen.
     *
     * Everything but the chords is folded away, so a midpoint pixel is a chord or it is the
     * ground. Aggregates rather than single lines: chords cross, and a pair overlapping at a
     * midpoint would make one far line read bright on its own.
     */
    private static void theChordsFadeWithDepth() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);
            // <b>Three fixed moments, because the sky moves.</b> The first version of this let
            // the sky cast itself at the instant of the run, so the chord set - and with it
            // every number below - was different every time. The measured ratio it was
            // thresholding against wandered between 0.60 and 0.78 on identical code, which is
            // wide enough to swallow the effect being checked. A check whose sample the check
            // does not control is measuring the clock.
            com.zodiacomputing.ourania.astro.ChartSubject a =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart A",
                    java.time.ZonedDateTime.of(1982, 8, 10, 15, 1, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Philadelphia", 39.95, -75.16, "America/New_York", false);
            com.zodiacomputing.ourania.astro.ChartSubject b =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart B",
                    java.time.ZonedDateTime.of(1979, 3, 22, 8, 40, 0, 0,
                        java.time.ZoneId.of("Europe/London")),
                    "London", 51.51, -0.13, "Europe/London", false);
            com.zodiacomputing.ourania.astro.ChartSubject skySubject =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Sky",
                    java.time.ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Philadelphia", 39.95, -75.16, "America/New_York", false);
            java.lang.reflect.Method install = SkymapPanel.class.getDeclaredMethod(
                "installSubjects", com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class);
            install.setAccessible(true);
            install.invoke(panel, a, b, skySubject);

            set(panel, "chartMode", ChartMode.SYNASTRY);
            set(panel, "showTransitChart", Boolean.TRUE);
            set(panel, "showTriWheel", Boolean.TRUE);
            // Both families drawn, so the sample reaches the cross-chart lines as well - the
            // ones that leave a tilted plane, which are the arcs with the least symmetry and
            // the most to go wrong.
            set(panel, "aspectFilter", "Both");
            panel.updateChartData();
            Thread.sleep(1500);

            for (SkymapPanel.Layer layer : SkymapPanel.Layer.values()) {
                if (layer != SkymapPanel.Layer.ASPECTS && layer != SkymapPanel.Layer.NATAL) {
                    panel.setLayer(layer, false);
                }
            }
            // Nothing lit: the hovered chord is deliberately exempt from the fade, and one of
            // those in the sample would be a bright line at whatever depth it happened to lie.
            set(panel, "autoPatterns", new int[0][]);
            set(panel, "highlightPattern", new int[0]);
            panel.setHighlightedAspect(null);
            Thread.sleep(900);

            final int size = 900;
            Globe cam = new Globe();

            // The painter's own chord list, and the painter's own geometry for its ends.
            java.lang.reflect.Method build = GlobeRenderer.class.getDeclaredMethod(
                "buildChords", SkymapPanel.class);
            build.setAccessible(true);
            java.lang.reflect.Method radii = GlobeRenderer.class.getDeclaredMethod(
                "shellRadii", SkymapPanel.class);
            radii.setAccessible(true);
            int[][] chords = (int[][]) build.invoke(null, panel);
            double[] shells = (double[]) radii.invoke(null, panel);
            double origin = panel.pinLongitude();
            double[][] lons = {panel.natalRing.lon, panel.outerRing.lon, panel.skyRing.lon};
            int[][] levels = {
                Globe.stackLevels(panel.natalRing.lon, panel.natalRing.valid, 7.0),
                Globe.stackLevels(panel.outerRing.lon, panel.outerRing.valid, 7.0),
                Globe.stackLevels(panel.skyRing.lon, panel.skyRing.valid, 7.0),
            };

            // <b>One chord on the screen at a time.</b> Two earlier versions of this sampled a
            // full chart and could not see the fade at all: the interior is a mesh, every
            // sample point has another chord within a pixel of it, and the measurement
            // saturated on whichever line was brightest nearby. With the fade removed the
            // median came back 0.823 against 0.814 with it - the same number, which is a check
            // that cannot fail. So each chord is measured alone, with every other body's
            // validity switched off, and nothing can cross it.
            boolean[] b0 = panel.natalRing.valid.clone();
            boolean[] t0 = panel.outerRing.valid.clone();
            boolean[] c0 = panel.skyRing.valid.clone();
            java.util.List<double[]> sample = new java.util.ArrayList<>();
            int tried = 0;
            for (int[] c : chords) {
                int ring = c[0];
                // <b>Only the lines the painter would draw.</b> The reader's filter decides
                // which families reach the screen, and it is applied where they are drawn
                // rather than where they are built - so a suite that walks the built list
                // and not the filter spends most of its twelve tries sampling background
                // where a cross-chart line was never painted, and finds too few to judge.
                if (ring == 0 && !panel.drawsNatalAspects()) {
                    continue;
                }
                if (ring > 0 && !panel.drawsCrossAspects()) {
                    continue;
                }
                if (ring == 1 && !panel.outerRingDrawn()) {
                    continue;
                }
                if (ring == 2 && !panel.triRingDrawn()) {
                    continue;
                }
                // The painter's own plane for the ring, height and tilt both - a sample that
                // knew the tilt and not the height would look for the ink where the ring
                // sits under the other layout, and read the background.
                boolean stacked = Settings.globeStackedRings();
                int deck = panel.ringDeck(ring);
                int innerDeck = panel.ringDeck(0);
                double[] from = Globe.onShell(lons[ring][c[1]], origin, shells[ring],
                    GlobeRenderer.liftOf(deck, stacked)
                        + levels[ring][c[1]] * Globe.STACK_STEP,
                    GlobeRenderer.inclinationOf(deck, stacked));
                double[] to = Globe.onShell(panel.natalRing.lon[c[2]], origin, shells[0],
                    GlobeRenderer.liftOf(innerDeck, stacked)
                        + levels[0][c[2]] * Globe.STACK_STEP,
                    GlobeRenderer.inclinationOf(innerDeck, stacked));
                Globe.Projected pa = cam.project(from[0], from[1], from[2], size, size);
                Globe.Projected pb = cam.project(to[0], to[1], to[2], size, size);
                if (!pa.visible || !pb.visible) {
                    continue;
                }
                Globe.Projected nearEnd = pa.depth <= pb.depth ? pa : pb;
                Globe.Projected farEnd = pa.depth <= pb.depth ? pb : pa;
                // A line running across the camera rather than away from it has no depth to
                // fade over, and one drawn nearly end-on has no length to sample along.
                // Deep enough that the fade has somewhere to go: below this the near and far
                // halves of a line sit at nearly one depth and "dimmer at its far end" is not
                // the rule. The threshold predates the arc, where it also picked out the lines
                // the painter drew in more than one step; the arc gives every line at least
                // three, and this now only says which lines have a fade worth measuring.
                if (farEnd.depth - nearEnd.depth < 1.2) {
                    continue;
                }
                // Long enough that a sample a quarter in from an end clears the body drawn
                // there. The bodies are planets now, with a ring of their chart's ink around
                // the outer ones, so the disc at a chord's end is wider than it was when this
                // margin was first set - and one chord in seven was reading its far sample off
                // the glyph rather than off the line.
                if (Math.hypot(pa.x - pb.x, pa.y - pb.y) < 120.0) {
                    continue;
                }
                if (tried >= 12) {
                    break;
                }
                tried++;

                // Only this chord's two bodies remain in the chart.
                boolean[] onlyB = new boolean[SkymapPanel.BODY_COUNT];
                boolean[] onlyT = new boolean[SkymapPanel.BODY_COUNT];
                boolean[] onlyC = new boolean[SkymapPanel.BODY_COUNT];
                onlyB[c[2]] = true;
                if (ring == 0) {
                    onlyB[c[1]] = true;
                } else if (ring == 1) {
                    onlyT[c[1]] = true;
                } else {
                    onlyC[c[1]] = true;
                }
                set(panel, "natalRing.valid", onlyB);
                set(panel, "outerRing.valid", onlyT);
                set(panel, "skyRing.valid", onlyC);
                panel.invalidateGlobeChords();

                java.awt.image.BufferedImage frame = new java.awt.image.BufferedImage(
                    size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D gg = frame.createGraphics();
                gg.setColor(new java.awt.Color(10, 12, 16));
                gg.fillRect(0, 0, size, size);
                GlobeRenderer.paint(gg, cam, size, size, panel, false);
                gg.dispose();

                // <b>Sampled along the arc the painter drew, not along the straight line
                // between its ends.</b> An aspect bows over the middle now, so a point a
                // quarter of the way along the chord is nowhere near the ink - which is not a
                // fade failing, it is a check looking in the wrong place. The painter strokes
                // curves that follow the arc to a tenth of a pixel, which Part O asserts, so
                // walking the arc finely lands on ink the painter laid down.
                // <b>Walked with the painter's own rule, not a copy of it.</b> riseFor gives
                // the direction and reachOf turns it into the distance the painter used. This
                // read riseFor alone until 2026-09-21, when riseFor became a direction: the
                // sampler then walked a curve nobody drew and measured 0 chords of 81.
                double[][] path = Globe.arc(from, to, 128, GlobeRenderer.reachOf(
                    GlobeRenderer.riseFor(panel, ring, Settings.globeAspectArcs()), from, to));
                boolean backwards = pb.depth < pa.depth;
                double nearInk = along(frame, cam, path, backwards, 0.25, size);
                double farInk = along(frame, cam, path, backwards, 0.75, size);
                if (nearInk < 8.0 || farInk < 4.0) {
                    continue;           // a sample missed the line
                }
                sample.add(new double[] {nearInk, farInk});
            }
            set(panel, "natalRing.valid", b0);
            set(panel, "outerRing.valid", t0);
            set(panel, "skyRing.valid", c0);
            panel.invalidateGlobeChords();

            System.out.println("  " + sample.size() + " chords measured alone, of "
                + chords.length + " in the chart");
            yes("there are enough chords to measure", sample.size() >= 5);
            if (sample.size() < 5) {
                return;
            }

            double[] ratios = new double[sample.size()];
            int dimmerAtItsFarEnd = 0;
            for (int i = 0; i < sample.size(); i++) {
                ratios[i] = sample.get(i)[1] / sample.get(i)[0];
                if (sample.get(i)[1] < sample.get(i)[0]) {
                    dimmerAtItsFarEnd++;
                }
            }
            java.util.Arrays.sort(ratios);
            double median = ratios[ratios.length / 2];
            System.out.printf("  a chord keeps %.0f%% of its ink three quarters of the way "
                + "along; %d of %d dim toward their far end%n",
                median * 100, dimmerAtItsFarEnd, sample.size());

            // <b>What this can and cannot prove.</b> The sphere's own translucent wash dims
            // whatever lies behind it, so a line's far end is darker than its near end with no
            // fade at all. That confound used to be large - measured at 0.55 of the near end
            // when the lines were chords lying in the equatorial clutter - and it shrank when
            // they became arcs, because a bow spends its middle above the band rather than
            // behind it.
            //
            // What this is is a regression pin between two measured states of this fixed
            // chart: 0.80 with the ramp, 1.01 without it, on a sample that now includes the
            // cross-chart lines as well. If the ramp stops reaching the screen this number
            // walks back up past 1.0 and the check fails. That is worth having and it is not
            // the same claim as "lines fade with depth" - see the note in
            // GlobeRenderer.chordDepthFade for the rule itself.
            yes("a line is dimmer at its far end than its near end", median < 1.0);
            // <b>Measured both ways on this fixed chart, not guessed.</b> 0.80 with the ramp,
            // 1.01 without it, and this sits between them. The gap is narrower than the old
            // one because the arcs carry less of the sphere's wash, and the count below is
            // what makes up the difference: without the ramp only five of eleven dim at all.
            yes("and dimmer by more than the sphere's wash alone accounts for", median < 0.90);
            // <b>All but one, and the exception is honest rather than slack.</b> A chord is
            // measured alone, so nothing crosses it - but the frame still holds the three
            // ribbons, the shells and the sphere's wash, and a sample point that lands where a
            // band crosses the line reads the band's ink instead of the chord's. Asserting
            // every single one made this fail on a chord whose far sample sat on a ribbon,
            // which is not the rule being checked breaking.
            yes("and it holds chord by chord, not just on average",
                dimmerAtItsFarEnd >= sample.size() - 1);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * The ink on an aspect line a given fraction of the way from its near end to its far end.
     *
     * <b>On the polyline, between two of its vertices.</b> The line is bowed, so a fraction of
     * the way along it is not a fraction of the way along the straight line between its ends.
     * The point is found on the segment the painter actually stroked - interpolated between
     * two projected vertices rather than on the ideal curve, which passes up to a pixel away
     * from the chord that stands in for it.
     */
    private static double along(java.awt.image.BufferedImage img, Globe cam, double[][] path,
            boolean backwards, double t, int size) {
        double u = (backwards ? 1.0 - t : t) * (path.length - 1);
        int i = Math.max(0, Math.min(path.length - 2, (int) Math.floor(u)));
        double f = u - i;
        Globe.Projected q0 = cam.project(path[i][0], path[i][1], path[i][2], size, size);
        Globe.Projected q1 = cam.project(
            path[i + 1][0], path[i + 1][1], path[i + 1][2], size, size);
        if (!q0.visible || !q1.visible) {
            return 0.0;
        }
        int x = (int) Math.round(q0.x + (q1.x - q0.x) * f);
        int y = (int) Math.round(q0.y + (q1.y - q0.y) * f);
        if (x < 1 || y < 1 || x >= size - 1 || y >= size - 1) {
            return 0.0;
        }
        return brightest(img, x, y);
    }

    private static double brightest(java.awt.image.BufferedImage img, int x, int y) {
        double best = 0.0;
        int[][] around = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : around) {
            int rgb = img.getRGB(x + d[0], y + d[1]);
            double v = Math.abs(((rgb >> 16) & 0xFF) - 10)
                + Math.abs(((rgb >> 8) & 0xFF) - 12)
                + Math.abs((rgb & 0xFF) - 16);
            best = Math.max(best, v);
        }
        return best;
    }

    /** One globe frame, painted offscreen. */
    private static java.awt.image.BufferedImage globeFrame(SkymapPanel panel) {
        java.awt.image.BufferedImage frame = new java.awt.image.BufferedImage(
            600, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D gg = frame.createGraphics();
        GlobeRenderer.paint(gg, new Globe(), 600, 600, panel, false);
        gg.dispose();
        return frame;
    }

    private static int differingPixels(java.awt.image.BufferedImage a,
            java.awt.image.BufferedImage b) {
        int n = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static void set(SkymapPanel panel, String name, Object value) throws Exception {
        CheckReflect.set(panel, name, value);
    }

    /**
     * The lunar mansion band is whole at every angle, and answers with the right station.
     *
     * <b>Three things, and the first is the one that bit.</b> The band was first placed at
     * 2.74 to 2.92, which looks right at the default tilt and reaches exactly a hundred per
     * cent of the panel's half-width when the globe is turned edge-on - the view David
     * specifically asked for. A ring that fits at one camera angle is not a ring that fits;
     * this walks every pitch the reader can drag to and asks whether it is still on the panel.
     *
     * Second, the hit test has to agree with the engine: a point on the band at a longitude
     * must resolve to the station LunarMansions puts that longitude in. The globe and the
     * tables are two surfaces onto one fact, and this project's defects live in those seams.
     *
     * Third, the band has to clear the degree scale beneath it, including the one tick that
     * grows when the reader points at it - the case that only appears while a cursor is
     * somewhere, which is exactly the case a still frame never shows.
     */
    private static void theMansionBand() throws Exception {
        int w = 1000;
        int h = 1000;
        double half = w / 2.0;

        // 1. Whole at every angle. The threshold is typed out rather than derived from the
        // shell, so moving the shell outward fails here instead of moving the goalposts.
        double worst = 0;
        double worstPitch = 0;
        for (double pitch = 0.0; pitch <= Globe.MAX_PITCH + 1e-9; pitch += 0.02) {
            Globe cam = new Globe();
            cam.pitch = pitch;
            // One assertion per angle rather than per sample: the sweep is here to find the
            // worst reach, and forty-five thousand identical "it projects" checks would say
            // nothing the count of failures could not, while making the suite too slow to run.
            boolean whole = true;
            for (double lon = 0; lon < 360; lon += 0.5) {
                double[] pt = Globe.onShell(lon, 0.0, Globe.SHELL_MANSION_OUTER, 0.0);
                Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
                whole &= q.visible;
                double reach = Math.max(Math.abs(q.x - half), Math.abs(q.y - half));
                if (reach > worst) {
                    worst = reach;
                    worstPitch = pitch;
                }
            }
            yes("the whole mansion band projects at pitch " + pitch, whole);
        }
        System.out.printf("  the band reaches %.0f px of %.0f at pitch %.2f (%.0f%%)%n",
            worst, half, worstPitch, 100.0 * worst / half);
        yes("the mansion band is whole at every angle the reader can drag to", worst < half);
        yes("and keeps a margin, so a panel that is not square still holds it",
            worst < half * 0.94);

        // 2. It clears the degree scale, including the tick that grows under the cursor.
        double tickInner = Globe.SHELL_SIGN_OUTER + 0.03;
        yes("the longest degree tick stops short of the band",
            tickInner + Globe.TICK_HOVER_REACH < Globe.SHELL_MANSION_INNER);
        yes("a hovered tick is still the longest on the scale",
            Globe.TICK_HOVER_REACH > 0.20);
        yes("the band has depth to read as a band",
            Globe.SHELL_MANSION_OUTER - Globe.SHELL_MANSION_INNER > 0.10);

        // 3. What the cursor finds is the station the engine says it is standing in.
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);

            // <b>Not while the ring is edge-on, and that is a fact about the view rather
            // than a gap in the check.</b> A ring in the ecliptic plane seen from within that
            // plane projects to a line: every longitude lands on the same few pixels, the far
            // half lies on the near half, and the round trip below is asking the projection
            // to be reversible when it is provably not. Part D learned this once already,
            // comparing the two views edge-on and producing a hundred failures against code
            // that was right; asserting it here would be the same mistake with a new name.
            // What the reader sees at pitch 0 is a line of overlapping numbers - nothing
            // legible to point at - so any station is as good an answer as any other.
            Globe cam = new Globe();
            double origin = panel.pinLongitude();
            double mid = (Globe.SHELL_MANSION_INNER + Globe.SHELL_MANSION_OUTER) / 2.0;
            int tested = 0;
            for (double yaw : new double[] {0.0, 1.6, 3.3, 5.0}) {
                cam.yaw = yaw;
                for (double pitch : new double[] {0.32, 0.7, 1.1}) {
                    cam.pitch = pitch;
                    // Half a degree inside each station's edges as well as its middle, so a
                    // boundary is tested from both sides rather than only the easy centre.
                    for (int m = 1; m <= com.zodiacomputing.ourania.astro
                            .LunarMansions.COUNT; m++) {
                        double start = (m - 1) * com.zodiacomputing.ourania.astro
                            .LunarMansions.WIDTH;
                        double width = com.zodiacomputing.ourania.astro.LunarMansions.WIDTH;
                        for (double at : new double[] {start + 0.4, start + width / 2.0,
                                start + width - 0.4}) {
                            double[] pt = Globe.onShell(at, origin, mid, 0.0);
                            Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
                            if (!q.visible) {
                                continue;
                            }
                            int got = GlobeRenderer.mansionAt(cam, w, h, panel,
                                (int) Math.round(q.x), (int) Math.round(q.y));
                            int want = com.zodiacomputing.ourania.astro.LunarMansions
                                .at(at).number;
                            eq("a point at " + String.format("%.1f", at) + " degrees (yaw "
                                + yaw + ", pitch " + pitch + ") is in its own station",
                                want, got);
                            tested++;
                        }
                    }
                }
            }
            System.out.println("  " + tested + " points on the band resolved to a station");
            yes("the band was actually walked", tested > 800);

            // Edge-on, the band must still answer with a real station rather than with
            // nothing or with nonsense - it is ambiguous there, not broken.
            cam.pitch = 0.0;
            cam.yaw = 0.0;
            int answered = 0;
            for (double at = 0.5; at < 360; at += 3.0) {
                double[] pt = Globe.onShell(at, origin, mid, 0.0);
                Globe.Projected q = cam.project(pt[0], pt[1], pt[2], w, h);
                if (!q.visible) {
                    continue;
                }
                int got = GlobeRenderer.mansionAt(cam, w, h, panel,
                    (int) Math.round(q.x), (int) Math.round(q.y));
                yes("edge-on, a point on the band still names some station",
                    got >= 1 && got <= com.zodiacomputing.ourania.astro.LunarMansions.COUNT);
                answered++;
            }
            yes("the edge-on sweep reached the band", answered > 100);

            // And the middle of the globe is not on the band, at any angle.
            cam.pitch = 0.32;
            yes("the centre of the view is not a station",
                GlobeRenderer.mansionAt(cam, w, h, panel, w / 2, h / 2) < 0);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * A filled band never lies across itself.
     *
     * <b>The defect this exists to catch.</b> A sign wedge on the sphere is a lune from pole
     * to pole, and its projection folds over twice: once at the limb, where the far half lands
     * on the near half, and again near the crown, where each meridian climbs to an apex short
     * of the pole and comes back down. Java's fillPolygon uses the even-odd rule, so either
     * fold came out as a hole rather than as paint - first the notches bitten out around the
     * pole, then, once the limb was handled, a slit a band wide down the middle of the crown.
     *
     * Globe.bandRuns is the cut. Three properties make the polygons it describes simple, and
     * all three are checked here on the depths and screen positions a real trace produces
     * rather than on made-up numbers: the runs tile the trace exactly once and share their
     * boundaries, so no gap opens between them; no run holds segments from both faces; and no
     * run has an edge that reverses direction in screen y, which is what lets a scan line
     * cross each side of the strip exactly once.
     *
     * The last two are the ones that bite, and they bite separately. Dropping the fold cut
     * leaves the face rule true and fails the monotone one; collapsing bandRuns to a single
     * run - the shape the painter had at first - fails both.
     */
    private static void bandsDoNotCrossThemselves() {
        Globe cam = new Globe();
        int w = 1000;
        int h = 1000;
        final int steps = 36;
        int crossed = 0;
        int folded = 0;

        for (double yaw : new double[] {0.0, 0.9, 2.4, 4.1, 5.6}) {
            cam.yaw = yaw;
            for (double pitch : new double[] {0.0, 0.32, 0.9}) {
                cam.pitch = pitch;
                for (double lon = 0; lon < 360; lon += 15) {
                    double radius = Globe.SHELL_SIGN_INNER;
                    double[] mid = new double[steps];
                    double[] ay = new double[steps + 1];
                    double[] by = new double[steps + 1];
                    double[] ad = new double[steps + 1];
                    double[] bd = new double[steps + 1];
                    for (int i = 0; i <= steps; i++) {
                        Globe.Projected qa = trace(cam, lon, radius, i, steps, w, h);
                        Globe.Projected qb = trace(cam, lon + 5, radius, i, steps, w, h);
                        ay[i] = qa.y;
                        by[i] = qb.y;
                        ad[i] = qa.depth;
                        bd[i] = qb.depth;
                    }
                    for (int j = 0; j < steps; j++) {
                        mid[j] = (ad[j] + ad[j + 1] + bd[j] + bd[j + 1]) / 4.0;
                    }
                    int[] runs = Globe.bandRuns(mid, cam.distance, ay, by);
                    String at = "yaw " + yaw + " pitch " + pitch + " lon " + lon;

                    eq(at + ": runs start at the first vertex", 0, runs[0]);
                    eq(at + ": runs end at the last vertex", steps, runs[runs.length - 1]);
                    yes(at + ": runs advance", advancing(runs));

                    for (int r = 0; r + 1 < runs.length; r++) {
                        // One face per run, or the far half of the strip lies on the near.
                        boolean near = mid[runs[r]] < cam.distance;
                        boolean mixed = false;
                        for (int j = runs[r]; j < runs[r + 1]; j++) {
                            if ((mid[j] < cam.distance) != near) {
                                mixed = true;
                            }
                        }
                        yes(at + ": run " + r + " stays on one face", !mixed);

                        // One direction per run, or the strip folds back over its own apex.
                        yes(at + ": run " + r + " edge A does not turn back",
                            monotone(ay, runs[r], runs[r + 1]));
                        yes(at + ": run " + r + " edge B does not turn back",
                            monotone(by, runs[r], runs[r + 1]));
                    }

                    // And these bands really do fold, both ways - otherwise the rules above
                    // would be passing on traces that never had the problem.
                    boolean crosses = false;
                    for (int j = 1; j < steps; j++) {
                        if ((mid[j] < cam.distance) != (mid[j - 1] < cam.distance)) {
                            crosses = true;
                        }
                    }
                    if (crosses) {
                        crossed++;
                    }
                    if (!monotone(ay, 0, steps)) {
                        folded++;
                    }
                }
            }
        }
        System.out.println("  " + crossed + " bands cross the limb, "
            + folded + " fold over an apex");
        yes("bands cross the limb, so the face cut is exercised", crossed > 200);
        yes("bands fold over an apex, so the turn cut is exercised", folded > 100);
    }

    /** One vertex of a pole-to-pole trace, as the painter computes it. */
    private static Globe.Projected trace(Globe cam, double lon, double radius,
            int i, int steps, int w, int h) {
        double phi = -Globe.FILL_SPAN + (2 * Globe.FILL_SPAN * i) / steps;
        double[] pt = Globe.onShell(lon, 0.0, radius, radius * Math.sin(phi));
        return cam.project(pt[0], pt[1], pt[2], w, h);
    }

    /** Whether a stretch of an edge only ever moves one way. */
    private static boolean monotone(double[] v, int from, int to) {
        int sign = 0;
        for (int i = from + 1; i <= to; i++) {
            double step = v[i] - v[i - 1];
            if (step == 0) {
                continue;
            }
            int now = step > 0 ? 1 : -1;
            if (sign != 0 && now != sign) {
                return false;
            }
            sign = now;
        }
        return true;
    }

    private static boolean advancing(int[] runs) {
        for (int i = 1; i < runs.length; i++) {
            if (runs[i] <= runs[i - 1]) {
                return false;
            }
        }
        return true;
    }

    /**
     * The three properties that kept going wrong, asserted where they can be measured.
     *
     * <p>All three come out of one week of getting this wrong repeatedly, and each is written
     * against the geometry rather than against a remembered number.
     *
     * <p><b>One shell.</b> The ribbons used to nest - 1.20, 1.50, 1.80 - and nested rings cannot
     * read as a stack at any tilt, because the depth term scales with the radius so a bigger
     * ring straddles a smaller one however far it is lifted. Measured on 2026-09-21: Chart B
     * rose above Chart A at a steep tilt and the sky fell below it from beneath. Asserting the
     * three radii are equal is what stops that returning.
     *
     * <p><b>The order, at every tilt.</b> The sky highest on screen, then Chart A, then Chart B,
     * across the whole allowed range and on both sides of the plane. It does <i>not</i> invert
     * under the plane: the lift reaches the screen through cos(pitch), which is positive
     * throughout, so a ribbon above the plane stays above even when the camera is beneath
     * looking up. What mirrors down there is the house sequence, not the stack - a probe that
     * assumed otherwise called twenty-five good tilts wrong.
     *
     * <p><b>One egg.</b> Every arc's apex lands on the house shell, whatever its aspect. The old
     * rule - a fixed rise of 1.0 - landed each apex back on the bodies' own shell, which was
     * right while the aspects were the outermost thing drawn and wrong once the houses moved
     * outside them. A constant cannot do it: at these radii a single rise that puts an
     * opposition on the shell puts a sextile well past it.
     */
    private static void theRibbonsBloom() {
        // ---- one shell
        near("the natal and partner ribbons share a shell",
            Globe.SHELL_NATAL, Globe.SHELL_PARTNER, 1e-9);
        near("and the sky rides it too", Globe.SHELL_NATAL, Globe.SHELL_SKY, 1e-9);
        yes("the ribbons sit inside the houses", Globe.SHELL_CHART < Globe.SHELL_HOUSE);
        yes("and the houses inside the bounds", Globe.SHELL_HOUSE < Globe.SHELL_BOUND);

        // ---- the latitude is the obliquity, not a tuned number
        near("the outer ribbons ride the obliquity",
            Globe.SHELL_CHART * Math.sin(Globe.OBLIQUITY), Globe.LIFT_SKY, 1e-9);
        near("and the partner rides it the other way", -Globe.LIFT_SKY, Globe.LIFT_PARTNER, 1e-9);
        yes("which is a real latitude, not a height off the sphere",
            Math.abs(Globe.LIFT_SKY) < Globe.SHELL_CHART);

        // ---- the order, swept across the whole allowed range and both sides of the plane
        int tilts = 0;
        boolean ordered = true;
        for (int i = -125; i <= 125; i += 5) {
            double pitch = i / 100.0;
            if (Math.abs(pitch) < Globe.MIN_PITCH) {
                continue;
            }
            Globe cam = new Globe();
            cam.pitch = pitch;
            cam.yaw = 0.0;
            double upper = ribbonMiddle(cam, GlobeRenderer.liftOf(SkymapPanel.DECK_UPPER, true));
            double middle = ribbonMiddle(cam, GlobeRenderer.liftOf(SkymapPanel.DECK_MIDDLE, true));
            double lower = ribbonMiddle(cam, GlobeRenderer.liftOf(SkymapPanel.DECK_LOWER, true));
            ordered &= upper < middle && middle < lower;
            tilts++;
        }
        // <b>Asserted by deck, not by chart.</b> Which chart rides which deck is a decision and
        // it changed on 2026-09-21 - the sky moved to the middle with Chart A above it. Written
        // as "the sky is on top" this assertion would have had to be edited to follow, which is
        // a check that agrees with whatever the code says. The decks' own order is the geometry
        // and does not move.
        yes("the upper deck rides above the middle and the middle above the lower, at every "
            + "tilt (" + tilts + ")", ordered && tilts > 40);

        // ---- the ribbons are lit at the edge and translucent through the body
        //
        // <b>What makes a stack of strokes read as light.</b> Each pass narrower than the last
        // and brighter than the last: a bloom that got wider as it got brighter would be a
        // halo round a hollow line, and one drawn at a single width is just a thick line. The
        // painter's own table is read here rather than restated - a suite that keeps its own
        // copy of a rule can only prove the copy agrees with itself.
        float[][] passes = GlobeRenderer.rimPasses();
        yes("a lit rim is drawn in more than one pass", passes.length >= 3);
        boolean narrowing = true;
        boolean brightening = true;
        for (int i = 1; i < passes.length; i++) {
            narrowing &= passes[i][0] < passes[i - 1][0];
            brightening &= passes[i][1] > passes[i - 1][1];
        }
        yes("each pass is narrower than the one before it", narrowing);
        yes("and brighter than the one before it", brightening);
        yes("the faintest pass really is faint", passes[0][1] < 1.0);
        yes("and the core really is the bright one",
            passes[passes.length - 1][1] > 1.0
                && passes[passes.length - 1][0] < 2.0);
        // The body stays translucent: a band whose fill matched its lit edge would be a solid
        // wall and the ribbons behind it would be gone.
        yes("the core outshines the band's own body by a wide margin",
            passes[passes.length - 1][1] >= 3.0);

        // ---- one egg: every apex on the shell its width asks for, over every aspect
        //
        // <b>Not one shell for all of them, and Part O is why.</b> The first version of this
        // sent every apex to the house shell; a conjunction's chord is nearly zero, so that
        // meant rising vertically out of two touching bodies and Part O's "a conjunction barely
        // leaves the ring" went red. The target is interpolated between the bodies' shell and
        // the houses' by how wide the aspect is, so the tight ones stay flat and the wide ones
        // sweep - which is what an egg's cracks do.
        boolean onTheShell = true;
        double worst = 0.0;
        for (Aspects.Type type : new Aspects.Type[] {Aspects.Type.CONJUNCTION,
                Aspects.Type.SEXTILE, Aspects.Type.SQUARE, Aspects.Type.TRINE,
                Aspects.Type.OPPOSITION}) {
            if (type.exactAngle < 1.0) {
                continue;                       // a conjunction has no chord to bow across
            }
            double[] a = Globe.onShell(0.0, 0.0, Globe.SHELL_CHART, 0.0);
            double[] b = Globe.onShell(type.exactAngle, 0.0, Globe.SHELL_CHART, 0.0);
            double target = targetShell(a, b);
            double[][] path = Globe.arc(a, b, 64, Globe.riseToShell(a, b, target));
            double apex = 0.0;
            for (double[] q : path) {
                apex = Math.max(apex, Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2]));
            }
            worst = Math.max(worst, Math.abs(apex - target));
            onTheShell &= Math.abs(apex - target) < 0.01;
        }
        yes(String.format("every aspect tops out on the shell its width asks for (worst %.4f)",
            worst), onTheShell);

        double[] a = Globe.onShell(0.0, 0.0, Globe.SHELL_CHART, 0.0);
        double[] opp = Globe.onShell(180.0, 0.0, Globe.SHELL_CHART, 0.0);
        double[] sex = Globe.onShell(60.0, 0.0, Globe.SHELL_CHART, 0.0);
        double[] tight = Globe.onShell(2.0, 0.0, Globe.SHELL_CHART, 0.0);

        // <b>The widest aspect rides highest and the tightest stays home.</b> This is the
        // property that makes them cracks rather than spikes, and it is the one a fixed rise
        // cannot have.
        yes("an opposition sweeps out to the houses",
            Math.abs(targetShell(a, opp) - Globe.SHELL_HOUSE) < 0.01);
        yes("and a near conjunction stays on the ribbon",
            Math.abs(targetShell(a, tight) - Globe.SHELL_CHART) < 0.05);
        yes("with a sextile somewhere between the two",
            targetShell(a, sex) > Globe.SHELL_CHART && targetShell(a, sex) < Globe.SHELL_HOUSE);

        // <b>And the same three, through the painter's own rule rather than this file's copy.</b>
        // The copy above exists so that editing the painter turns Part S red; but a copy alone
        // asserts nothing about what is drawn. Mutation-tested on 2026-09-21: sending every apex
        // back to the house shell in GlobeRenderer <b>survived</b> the whole suite, because
        // nothing called reachOf. These three do.
        yes("the painter sweeps an opposition out to the houses",
            Math.abs(paintedApex(a, opp) - Globe.SHELL_HOUSE) < 0.02);
        yes("and the painter keeps a near conjunction on the ribbon",
            Math.abs(paintedApex(a, tight) - Globe.SHELL_CHART) < 0.05);
        yes("and puts a sextile between them",
            paintedApex(a, sex) > Globe.SHELL_CHART + 0.02
                && paintedApex(a, sex) < Globe.SHELL_HOUSE - 0.02);

        // <b>A shell inside the chord's own midpoint cannot be reached by any bow.</b> The case
        // has to be a tight aspect: an opposition's midpoint is the centre of the sphere, so
        // every shell is reachable from it and the first version of this assertion picked an
        // example that proved nothing.
        near("a shell no bow can reach asks for no rise", 0.0,
            Globe.riseToShell(a, sex, 0.5), 1e-9);
    }

    /**
     * The shell an arc of this width should top out on.
     *
     * <b>A second copy of the renderer's rule, deliberately.</b> If the painter's interpolation
     * is edited this one does not follow, and Part S goes red - which is the point of asserting
     * a rule the painter also holds an opinion about.
     */
    private static double targetShell(double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double wide = Math.min(1.0, len / (2.0 * Globe.SHELL_CHART));
        return Globe.SHELL_CHART + (Globe.SHELL_HOUSE - Globe.SHELL_CHART) * wide;
    }

    /**
     * How far from the centre the painter's own arc actually reaches.
     *
     * Built through {@link GlobeRenderer#reachOf}, so this measures the curve that gets drawn
     * rather than a restatement of the rule that produces it.
     */
    private static double paintedApex(double[] a, double[] b) {
        double[][] path = Globe.arc(a, b, 96, GlobeRenderer.reachOf(1.0, a, b));
        double apex = 0.0;
        for (double[] q : path) {
            apex = Math.max(apex, Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2]));
        }
        return apex;
    }

    /** Where a ribbon's centre lands on screen, walked round the ring. */
    private static double ribbonMiddle(Globe cam, double lift) {
        Globe.Projected centre = cam.project(0, 0, 0, 900, 900);
        double sum = 0.0;
        for (int d = 0; d < 360; d += 2) {
            double[] pt = Globe.onShell(d, 0.0, Globe.SHELL_CHART, lift);
            sum += cam.project(pt[0], pt[1], pt[2], 900, 900).y - centre.y;
        }
        return sum / 180.0;
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
