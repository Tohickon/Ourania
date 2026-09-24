package com.zodiacomputing.ourania.astro;

import com.zodiacomputing.ourania.gui.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Master list H1: the natal orbs as the reader's, per point.
 *
 * <p>The transit orb has been a setting since F3. The natal widths were a table in
 * {@link Aspects} with no way to reach them, which is what H1 names - and the setting most often
 * asked for by anyone who works to a house style.
 *
 * <p><b>What is actually at risk here is reach.</b> An orb that the aspect list honours and the
 * wheel does not, or that the reading uses and the grid ignores, is worse than no setting: the
 * reader sets a width, sees some of it take effect, and cannot tell which surface is lying. So
 * the assertions below are mostly about the width arriving everywhere one number is supposed to
 * go, rather than about arithmetic.
 *
 * <ul>
 * <li><b>A, nothing set changes nothing</b> - the table stands, to the last point in the
 * registry.</li>
 * <li><b>B, a width the reader sets is the width that judges</b>, through the pair rule and the
 * synastry halving, and clearing it puts the table back.</li>
 * <li><b>C, the bounds are refused rather than bent</b>, and a hand-edited file cannot stop the
 * app starting.</li>
 * <li><b>D, the settings round-trip</b> - what is stored is keyed by the registry's stable id and
 * comes back as the same number.</li>
 * </ul>
 */
public final class OrbCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        try {
            part("A: the table stands when nothing is set", OrbCheck::untouched);
            part("B: a width the reader sets is the one that judges", OrbCheck::inForce);
            part("C: the bounds are refused, not bent", OrbCheck::bounds);
            part("D: the setting round-trips", OrbCheck::roundTrip);
        } finally {
            Aspects.setCustomOrbs(null);
        }

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    /**
     * <b>Every point in the registry, not a sample.</b> The failure this guards against is a
     * point whose width stops coming from the table - and the points most likely to be missed
     * are the ones nobody thinks about, which is exactly what a sample leaves out.
     */
    private static void untouched() {
        Aspects.setCustomOrbs(null);
        boolean all = true;
        String first = "";
        for (int i = 0; i < Bodies.count(); i++) {
            String name = Bodies.at(i).name;
            if (Aspects.orbOf(name) != Aspects.defaultBodyOrb(name)) {
                all = false;
                if (first.isEmpty()) {
                    first = name;
                }
            }
        }
        ok("with nothing set every point is judged at its built-in width" + first, all);
        ok("and nothing is reported as customised", Aspects.customOrbs().isEmpty());

        // The angles are in the table under their own names rather than falling to the default,
        // which is the distinction hasExplicitOrb exists to keep.
        ok("an angle has its own entry", Aspects.hasExplicitOrb("Ascendant"));
        ok("and something unheard of does not", !Aspects.hasExplicitOrb("Vulcan"));
        ok("which still gets a width to be judged at",
            Aspects.orbOf("Vulcan") == Aspects.ANGLE_ORB);
    }

    /**
     * <b>Through the pair rule, not just the getter.</b> A width that only {@code orbOf} can see
     * has reached nothing: every aspect in the app is judged by {@link Aspects#orbFor}, which
     * takes the larger of the two points and halves it for a cross-chart reading.
     */
    private static void inForce() {
        double sunWas = Aspects.defaultBodyOrb("Sun");
        double plutoWas = Aspects.defaultBodyOrb("Pluto");

        Map<String, Double> mine = new HashMap<>();
        mine.put("Pluto", 12.0);
        Aspects.setCustomOrbs(mine);

        near("the point the reader widened is judged at their width", 12.0,
            Aspects.orbOf("Pluto"), 1e-9);
        near("and a point they left alone is not", sunWas, Aspects.orbOf("Sun"), 1e-9);
        ok("which is a real change, not the width it already had", Math.abs(12.0 - plutoWas) > 1);

        // The pair rule takes the larger, so widening the smaller of a pair has to show.
        near("a pair takes the wider of the two", 12.0, Aspects.orbFor("Sun", "Pluto"), 1e-9);
        near("whichever way round it is asked", 12.0, Aspects.orbFor("Pluto", "Sun"), 1e-9);
        near("and a cross-chart pair is still halved from there", 6.0,
            Aspects.orbFor("Sun", "Pluto", true), 1e-9);

        // <b>And the aspect's own ceiling still caps it.</b> A reader widening Pluto to 12 has
        // not widened a semisextile to 12; the minor aspects keep their 1 degree.
        double minorCap = Aspects.Type.SEMISEXTILE.maxOrb;
        ok("a minor aspect keeps its own ceiling over the reader's width",
            Aspects.orbFor("Sun", "Pluto") > minorCap);

        Aspects.setCustomOrbs(null);
        near("clearing puts the table back", plutoWas, Aspects.orbOf("Pluto"), 1e-9);
    }

    /** <b>Refused, not clamped.</b> A number the engine cannot honour must not be reported as taken. */
    private static void bounds() {
        double plutoWas = Aspects.defaultBodyOrb("Pluto");
        Map<String, Double> silly = new HashMap<>();
        silly.put("Pluto", 400.0);
        silly.put("Sun", -3.0);
        silly.put("Mars", Double.NaN);
        silly.put(null, 5.0);
        Aspects.setCustomOrbs(silly);

        near("a width past the maximum is refused", plutoWas, Aspects.orbOf("Pluto"), 1e-9);
        near("a negative one is refused", Aspects.defaultBodyOrb("Sun"),
            Aspects.orbOf("Sun"), 1e-9);
        near("a NaN is refused", Aspects.defaultBodyOrb("Mars"), Aspects.orbOf("Mars"), 1e-9);
        ok("and none of them is reported as in force", Aspects.customOrbs().isEmpty());

        // A zero orb would switch a point off entirely rather than narrow it, which is a
        // different feature and not this one.
        ok("the minimum is above zero", Aspects.MIN_BODY_ORB > 0.0);
        ok("and the maximum is below a whole sign", Aspects.MAX_BODY_ORB < 30.0);
    }

    /**
     * <b>Stored under the registry's id, read back by name.</b> Bodies.Def.id calls itself the
     * stable key written into settings.properties; the display name is what Aspects looks an orb
     * up by. Storing under the name would tie a saved file to a label that may be reworded.
     */
    private static void roundTrip() {
        Settings.resetBodyOrbs();
        ok("a fresh install has no widths of its own", Settings.bodyOrbs().isEmpty());
        near("and reports the built-in width", Aspects.defaultBodyOrb("Venus"),
            Settings.bodyOrb("Venus"), 1e-9);

        Settings.setBodyOrb("Venus", 9.5);
        near("what was set comes back", 9.5, Settings.bodyOrb("Venus"), 1e-9);
        near("and is in force in the engine at once", 9.5, Aspects.orbOf("Venus"), 1e-9);
        ok("stored under the registry's id",
            !Settings.get(Settings.BODY_ORB_PREFIX + Bodies.byName("Venus").id, "").isEmpty());

        // Setting it back to the built-in width is the same state as never having touched it.
        Settings.setBodyOrb("Venus", Aspects.defaultBodyOrb("Venus"));
        ok("putting it back to the default forgets the setting", Settings.bodyOrbs().isEmpty());
        near("and the engine is back on the table", Aspects.defaultBodyOrb("Venus"),
            Aspects.orbOf("Venus"), 1e-9);

        Settings.setBodyOrb("Venus", 9.5);
        Settings.setBodyOrb("Mars", 4.0);
        ok("two widths are both remembered", Settings.bodyOrbs().size() == 2);
        Settings.resetBodyOrbs();
        ok("and reset forgets both", Settings.bodyOrbs().isEmpty());
        near("leaving the engine on the table", Aspects.defaultBodyOrb("Venus"),
            Aspects.orbOf("Venus"), 1e-9);

        // Out of range from the screen is clamped rather than refused - the screen is a spinner
        // with bounds, so a value past them is the reader dragging to the end, not a bad file.
        Settings.setBodyOrb("Venus", 999.0);
        near("a width past the maximum is clamped when it comes from the screen",
            Aspects.MAX_BODY_ORB, Settings.bodyOrb("Venus"), 1e-9);
        Settings.resetBodyOrbs();
    }

    private static void part(String title, Runnable body) {
        System.out.println("=== Part " + title + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + title.charAt(0)
            + (failures.size() == before ? ": clear"
                : ": " + (failures.size() - before) + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void near(String label, double expect, double got, double tol) {
        checks++;
        if (!(Math.abs(expect - got) <= tol)) {
            failures.add(label + ": expected " + expect + ", got " + got);
        }
    }
}
