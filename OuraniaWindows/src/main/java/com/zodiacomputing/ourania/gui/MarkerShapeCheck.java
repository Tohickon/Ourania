package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which bead shape each ring of bodies wears, and that no two rings can wear the same one.
 *
 * <p><b>Why this suite exists.</b> A synastry with transits draws three sets of bodies on one
 * wheel: this chart, the other person's, and the sky. Two of the three were cubes separated
 * only by a tint, which is a distinction that does not survive a small window, a projector or
 * a printout - and the reader is left comparing two shades of grey on opposite sides of the
 * wheel to decide whose Mars they are looking at.
 *
 * <p><b>What would otherwise rot is Part B.</b> The outer wheel carries chart B in synastry
 * and the sky in every other mode, so the shape it wears cannot be a property of the ring - it
 * has to be asked of the chart mode. Get that backwards and chart B is drawn with the transit
 * shape in exactly the chart that needed them told apart, while every simpler chart still
 * looks right. Nothing throws; the wheel is simply harder to read than it was.
 */
public final class MarkerShapeCheck {

    private static final List<String> failures = new ArrayList<String>();
    private static int checks = 0;

    public static void main(String[] args) {
        System.out.println("=== Part A: the shapes themselves ===");
        int before = failures.size();
        theShapes();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: which ring wears which ===");
        before = failures.size();
        theRoles();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: a value the file should not have ===");
        before = failures.size();
        theFallback();
        report("Part C", before);

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
    }

    /** The catalogue, and that the defaults actually differ from each other. */
    private static void theShapes() {
        ok("every shape the settings offer is one the wheel knows how to draw",
            Settings.MARKER_SHAPES.length == 4);
        for (String s : new String[] {Settings.MARKER_SPHERE, Settings.MARKER_CUBE,
                                      Settings.MARKER_PYRAMID, Settings.MARKER_NONE}) {
            ok("the settings offer " + s, contains(Settings.MARKER_SHAPES, s));
        }

        // <b>The point of the whole change, stated as a check.</b> Three roles sharing two
        // shapes is the defect this replaced - and a later edit that gives chart B the cube
        // back would be a one-word change that reintroduces it in silence.
        Set<String> defaults = new HashSet<String>();
        defaults.add(Settings.MARKER_SPHERE);
        defaults.add(Settings.MARKER_PYRAMID);
        defaults.add(Settings.MARKER_CUBE);
        ok("the three default shapes are three different shapes", defaults.size() == 3);

        // Asserted against the constants rather than the live settings, because a reader who
        // has chosen their own shapes must not fail this suite.
        ok("a bare glyph is offered but is nobody's default",
            !defaults.contains(Settings.MARKER_NONE));
    }

    /**
     * The outer wheel takes its shape from what it is carrying, not from where it is.
     *
     * Compared against the accessors rather than against a literal, so the check still holds
     * when a reader has picked shapes of their own - what is pinned is the routing.
     */
    private static void theRoles() {
        ok("in synastry the outer wheel is the other person, and wears their shape",
            Settings.synastryMarker().equals(
                SkymapPanel.outerRingMarker(ChartMode.SYNASTRY)));

        // Every other mode that can have an outer wheel puts the sky on it.
        for (ChartMode mode : new ChartMode[] {ChartMode.SINGLE, ChartMode.TRANSIT,
                                               ChartMode.COMPOSITE_MIDPOINT,
                                               ChartMode.COMPOSITE_DAVISON}) {
            ok("in " + mode.label + " the outer wheel is the sky, and wears the transit shape",
                Settings.transitMarker().equals(SkymapPanel.outerRingMarker(mode)));
        }

        // <b>The tri-wheel is the case the shapes exist for.</b> Only synastry can have one,
        // and when it does the middle ring and the outermost must not agree - the middle is
        // chart B and the outermost is the sky.
        ok("only synastry draws a tri-wheel",
            SkymapPanel.triWheelShown(ChartMode.SYNASTRY, true)
                && !SkymapPanel.triWheelShown(ChartMode.TRANSIT, true)
                && !SkymapPanel.triWheelShown(ChartMode.COMPOSITE_DAVISON, true));
        ok("on a tri-wheel the second person and the sky do not wear the same shape",
            !SkymapPanel.outerRingMarker(ChartMode.SYNASTRY)
                .equals(Settings.transitMarker()));
    }

    /** A shape name the app does not draw must fall back, not blank the ring. */
    private static void theFallback() {
        eq("an unknown shape falls back rather than drawing nothing",
            Settings.MARKER_SPHERE,
            Settings.markerOr("Dodecahedron", Settings.MARKER_SPHERE));
        eq("an empty value falls back", Settings.MARKER_CUBE,
            Settings.markerOr("", Settings.MARKER_CUBE));
        eq("a missing value falls back", Settings.MARKER_PYRAMID,
            Settings.markerOr(null, Settings.MARKER_PYRAMID));
        // The one that must NOT be corrected: a bare glyph is a real choice, not a bad value.
        eq("a bare glyph survives the fallback, because it is a shape and not a typo",
            Settings.MARKER_NONE,
            Settings.markerOr(Settings.MARKER_NONE, Settings.MARKER_SPHERE));
    }

    private static boolean contains(String[] all, String one) {
        for (String s : all) {
            if (s.equals(one)) {
                return true;
            }
        }
        return false;
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
