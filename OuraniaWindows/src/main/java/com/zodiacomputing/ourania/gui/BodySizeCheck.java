package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import java.util.ArrayList;
import java.util.List;

/**
 * How big each body is drawn on the wheel, and that the order of the four sizes is the order
 * of the actual objects.
 *
 * <p><b>Why this suite exists.</b> The glyph size table had no assertion of any kind, and it
 * had drifted twice without anything noticing. It claimed four tiers while
 * {@code TRANSIT_SIZES} drew three - tiers 1 and 2 were both radius 11, so the giants and the
 * inner planets wore the same bead - and its order was inverted against the sky, because the
 * tier came from {@code Bodies.Group}: Mercury was drawn larger than Jupiter. Neither defect
 * throws, neither shows up in a screenshot you are not comparing against anything, and both
 * are the kind of thing a reader experiences as "the wheel is a bit muddy" rather than as a
 * bug to report.
 *
 * <p><b>What would otherwise rot is Part D.</b> Physical size and astrological meaning are
 * different axes, and the tempting simplification is always to derive one from the other -
 * {@code Group} is right there, it is already per-body, and for most of the registry it even
 * agrees. It disagrees on exactly two points: Pluto is an outer planet 1,188km across, and
 * Ceres is an asteroid and the largest one there is. Part D pins those two negatively, so
 * collapsing the table back into a switch on {@code Group} fails here instead of quietly
 * restoring the inverted wheel.
 *
 * <p><b>Part C is the one that bounds the whole table.</b> Beads cannot grow past half the
 * collision spacing their ring is laid out with, or two neighbours both claim the same pixel
 * and a click answers for the wrong body. That rule was written in a comment on
 * {@code SkymapPanel.hitRadius} and enforced nowhere; the top tier sits exactly on the limit,
 * so it is an assertion that genuinely fires rather than one with slack in it.
 *
 * <p><b>Deliberately not asserted:</b> the {@code IllegalStateException} in
 * {@code WheelLayout.tier} for an id the registry does not have. It is reachable - by renaming
 * a body's id - but only from outside the test, because a miss fails class initialization and
 * the whole suite dies before it can report. That is the right failure (loud, at startup, with
 * the id in the message) and the wrong thing to build reflection scaffolding for.
 */
public final class BodySizeCheck {

    private static final List<String> failures = new ArrayList<String>();
    private static int checks = 0;

    /** The hierarchy David asked for on 2026-10-06, written out so it can be asserted. */
    private static final String[] LIGHTS = {"sun", "moon"};
    private static final String[] GIANTS = {"jupiter", "saturn", "uranus", "neptune"};
    private static final String[] ROCKY = {"mercury", "venus", "mars", "ceres", "pluto"};
    /** A sample of the smallest tier - not the whole of it, which is everything else. */
    private static final String[] SMALL = {"vesta", "pallas", "juno", "chiron", "eris",
                                           "north_node", "lilith", "fortune"};

    public static void main(String[] args) {
        // Never the reader's own settings file: a suite that generates a chart persists it,
        // and one of these once overwrote a saved birth chart. See Settings.useScratchFile.
        Settings.useScratchFile();

        System.out.println("=== Part A: every body is in the tier it belongs to ===");
        int before = failures.size();
        theTiers();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: the four sizes are four sizes ===");
        before = failures.size();
        theSpread();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: a bead cannot outgrow its ring ===");
        before = failures.size();
        theBounds();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: size is not meaning ===");
        before = failures.size();
        theAxes();
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: an angle is not a planet ===");
        before = failures.size();
        theAngles();
        report("Part E", before);

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

    /**
     * The mapping itself, body by body. Each group is asserted by name rather than by count,
     * so moving Saturn out of the giants fails with Saturn's name in the message.
     */
    private static void theTiers() {
        tierIs(LIGHTS, WheelLayout.TIER_LIGHT, "a light");
        tierIs(GIANTS, WheelLayout.TIER_GIANT, "a giant");
        tierIs(ROCKY, WheelLayout.TIER_ROCKY, "rocky");
        tierIs(SMALL, WheelLayout.TIER_SMALL, "small");

        // Nothing in the registry may land outside the four tiers. An index that answered 4
        // would be an ArrayIndexOutOfBounds the first time that body was drawn, on a chart
        // that happened to have it switched on - which is a crash in the painter, at a
        // user's elbow, and not here.
        for (int n = 0; n < Bodies.ALL.length; n++) {
            int t = WheelLayout.tierOf(n);
            ok(Bodies.at(n).name + " is drawn at one of the four tiers, or is an angle",
                t == -1 || (t >= WheelLayout.TIER_LIGHT && t <= WheelLayout.TIER_SMALL));
        }
    }

    /**
     * <b>Strictly decreasing, and by enough to see.</b> Strict decrease alone is what the
     * transit table failed: it had two tiers at radius 11, which is a hierarchy that exists
     * in the constant names and not on the screen. The ratio check is the testable form of
     * "clearly" - a 14-13-12-11 table passes strict decrease and reads as one size.
     */
    private static void theSpread() {
        decreasing("natal", WheelLayout.NATAL_SIZES);
        decreasing("transit", WheelLayout.TRANSIT_SIZES);

        // The natal wheel is the chart being read rather than context around it, so its
        // steps are the ones that have to survive a small window.
        for (int i = 1; i < WheelLayout.NATAL_SIZES.length; i++) {
            int step = WheelLayout.NATAL_SIZES[i - 1].radius - WheelLayout.NATAL_SIZES[i].radius;
            ok("natal tier " + i + " is at least 2px smaller than tier " + (i - 1)
                + " (got " + step + ")", step >= 2);
        }

        ratio("natal", WheelLayout.NATAL_SIZES);
        ratio("transit", WheelLayout.TRANSIT_SIZES);
    }

    private static void decreasing(String table, WheelLayout.GlyphSize[] sizes) {
        for (int i = 1; i < sizes.length; i++) {
            ok(table + " radius falls from tier " + (i - 1) + " to tier " + i
                + " (" + sizes[i - 1].radius + " then " + sizes[i].radius + ")",
                sizes[i].radius < sizes[i - 1].radius);
            ok(table + " glyph point size falls from tier " + (i - 1) + " to tier " + i
                + " (" + sizes[i - 1].fontPoints + "pt then " + sizes[i].fontPoints + "pt)",
                sizes[i].fontPoints < sizes[i - 1].fontPoints);
        }
    }

    /** Largest tier against smallest - the spread a reader actually perceives. */
    private static void ratio(String table, WheelLayout.GlyphSize[] sizes) {
        double top = sizes[0].radius;
        double bottom = sizes[sizes.length - 1].radius;
        double r = top / bottom;
        ok("the " + table + " lights are at least 1.5x an asteroid's bead"
            + " (got " + String.format("%.2f", r) + "x from " + (int) top + " and "
            + (int) bottom + ")", r >= 1.5);
    }

    /**
     * <b>The ceiling on the whole table.</b> SkymapPanel.hitRadius is the drawn radius plus
     * two, capped at half the collision spacing its ring was laid out with. Half the spacing
     * is the real bound: beyond it two adjacent bodies' targets overlap and a click is
     * answered by whichever the loop reaches, which is the defect nearestPoint was written to
     * remove. The top tiers sit exactly on the limit, so raising either one fails here.
     */
    private static void theBounds() {
        bound("natal", WheelLayout.NATAL_SIZES, WheelLayout.NATAL_SPACING);
        // The bands are laid out at bandRadii's own default spacing rather than the natal
        // wheel's wider one; see the second bandRadii overload.
        bound("transit", WheelLayout.TRANSIT_SIZES, 28.0);

        // And the clearance kept at a band edge has to clear the largest bead drawn in it,
        // which is what stops a glyph on the outermost sub-ring overhanging the zodiac.
        for (WheelLayout.GlyphSize s : WheelLayout.NATAL_SIZES) {
            ok("a natal bead of " + s.radius + "px fits inside NATAL_EDGE ("
                + WheelLayout.NATAL_EDGE + ")", s.radius < WheelLayout.NATAL_EDGE);
        }
        for (WheelLayout.GlyphSize s : WheelLayout.TRANSIT_SIZES) {
            ok("a transit bead of " + s.radius + "px fits inside BAND_EDGE ("
                + WheelLayout.BAND_EDGE + ")", s.radius < WheelLayout.BAND_EDGE);
        }
    }

    private static void bound(String table, WheelLayout.GlyphSize[] sizes, double spacing) {
        for (WheelLayout.GlyphSize s : sizes) {
            int hit = s.radius + 2;
            ok("a " + table + " bead of " + s.radius + "px has a hit target of " + hit
                + "px, within half the " + (int) spacing + "px it is spaced at",
                hit <= spacing / 2.0);
        }
    }

    /**
     * <b>Negative pins, and the point of the whole suite.</b> Both of these passed under the
     * old switch on Bodies.Group and both were wrong. Asserting that Pluto is rocky and Ceres
     * is not the smallest tier would not be enough on its own - the interesting statement is
     * that neither can be recovered from its group, so deriving the table from Group again
     * cannot pass.
     */
    private static void theAxes() {
        int pluto = Bodies.indexOf("pluto");
        int ceres = Bodies.indexOf("ceres");
        int jupiter = Bodies.indexOf("jupiter");
        int vesta = Bodies.indexOf("vesta");

        ok("pluto and jupiter are both Group.SOCIAL",
            Bodies.at(pluto).group == Bodies.at(jupiter).group);
        ok("...and are NOT drawn at the same size",
            WheelLayout.tierOf(pluto) != WheelLayout.tierOf(jupiter));

        ok("ceres and vesta are both Group.ASTEROIDS",
            Bodies.at(ceres).group == Bodies.at(vesta).group);
        ok("...and are NOT drawn at the same size",
            WheelLayout.tierOf(ceres) != WheelLayout.tierOf(vesta));

        // The inversion itself, stated as the thing a reader would notice: the largest planet
        // in the solar system is not drawn smaller than the smallest one.
        int mercury = Bodies.indexOf("mercury");
        ok("Jupiter is drawn larger than Mercury",
            WheelLayout.natalSize(jupiter).radius > WheelLayout.natalSize(mercury).radius);
        ok("Jupiter is drawn larger than Mercury in a transit ring too",
            WheelLayout.transitSize(jupiter).radius
                > WheelLayout.transitSize(mercury).radius);

        // Both lights, which is the one tier grouped by apparent rather than physical size.
        int moon = Bodies.indexOf("moon");
        ok("the Moon is drawn with the Sun despite being smaller than Mercury",
            WheelLayout.tierOf(moon) == WheelLayout.tierOf(Bodies.indexOf("sun")));
        ok("...and larger than Mercury, which is physically bigger",
            WheelLayout.natalSize(moon).radius > WheelLayout.natalSize(mercury).radius);
    }

    /**
     * Angles are outside the hierarchy: every painter tests isAngle() and draws a fixed
     * labelled cube. The tier answers -1 so that a caller which forgets to is not handed a
     * planet's weight for the Ascendant.
     */
    private static void theAngles() {
        int angles = 0;
        for (int n = 0; n < Bodies.ALL.length; n++) {
            if (!Bodies.at(n).isAngle()) {
                continue;
            }
            angles++;
            ok(Bodies.at(n).name + " is outside the size hierarchy",
                WheelLayout.tierOf(n) == -1);
            // The documented fallback. It is the smallest tier rather than a planet's, so a
            // caller that does reach it draws the not-a-body as the lightest thing on the
            // wheel instead of as an inner planet.
            ok(Bodies.at(n).name + " falls back to the smallest bead, not a planet's",
                WheelLayout.natalSize(n).radius
                    == WheelLayout.NATAL_SIZES[WheelLayout.TIER_SMALL].radius);
        }
        eq("all four angles were found", 4, angles);
    }

    private static void tierIs(String[] ids, int tier, String label) {
        for (String id : ids) {
            int n = Bodies.indexOf(id);
            ok(id + " is in the registry", n >= 0);
            if (n < 0) {
                continue;
            }
            eq(Bodies.at(n).name + " is drawn as " + label, tier, WheelLayout.tierOf(n));
        }
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
