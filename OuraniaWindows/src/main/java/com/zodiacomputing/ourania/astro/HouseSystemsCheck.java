package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * The house systems, held against the ephemeris and against what the app used to do.
 *
 * <p><b>Part A is the one that makes this commit a refactor rather than a change.</b> The list a
 * reader is offered, and the character each name turns into, are asserted to be exactly what the
 * four hand-written copies produced before they were replaced - written out here by hand rather
 * than read from the registry, because a check that asks the registry what the registry says would
 * pass against any answer.
 *
 * <p><b>Part C asks the ephemeris.</b> A registry that names a system the engine cannot compute
 * would be a picker entry that draws a broken chart, and the only authority on that is
 * {@code swe_houses} itself: it is asked to cast every offered system and the cusps are checked
 * for being twelve real degrees in order.
 */
public final class HouseSystemsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /**
     * Every system offered, written out rather than read from the registry.
     *
     * <b>The first six are what the four hand-written copies produced</b>, and holding them in
     * this order is what made H2a provably a refactor. The eight after them were added
     * deliberately in H2b, which is why this list had to be edited on purpose for that commit to
     * go green - a check that asked the registry what the registry says would have waved it
     * through either way.
     */
    private static final String[][] AS_IT_WAS = {
        {"Placidus", "P"}, {"Koch", "K"}, {"Equal", "E"},
        {"Whole Sign", "W"}, {"Campanus", "C"}, {"Regiomontanus", "R"},
        {"Porphyry", "O"}, {"Alcabitius", "B"}, {"Topocentric", "T"}, {"Morinus", "M"},
        {"Meridian", "X"}, {"Vehlow", "V"}, {"Krusinski", "U"}, {"APC", "Y"},
    };

    /**
     * The systems that deliberately do not put the Ascendant on the first cusp.
     *
     * <b>Measured, not recalled.</b> Whole Sign starts the rising sign, Vehlow puts the Ascendant
     * in the middle of house one, and Morinus and Meridian ignore the horizon entirely.
     */
    private static final String ASC_ELSEWHERE = "WVMX";

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        part("A: the screen offers exactly what it offered before",
            HouseSystemsCheck::unchanged);
        part("B: names and codes agree in both directions", HouseSystemsCheck::roundTrip);
        part("C: the ephemeris can actually cast every one", () -> castable(sw));
        part("D: an unknown name is answered, not thrown", HouseSystemsCheck::unknown);
        part("E: what is deliberately absent", HouseSystemsCheck::absent);

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

    // ------------------------------------------------------------------ A

    private static void unchanged() {
        String[] offered = HouseSystems.names();
        ok("fourteen systems are offered (" + offered.length + ")",
            offered.length == AS_IT_WAS.length);
        for (int i = 0; i < Math.min(offered.length, AS_IT_WAS.length); i++) {
            ok("position " + i + " is still " + AS_IT_WAS[i][0],
                AS_IT_WAS[i][0].equals(offered[i]));
            ok(AS_IT_WAS[i][0] + " still casts as '" + AS_IT_WAS[i][1] + "'",
                HouseSystems.codeFor(AS_IT_WAS[i][0]) == AS_IT_WAS[i][1].charAt(0));
        }
        ok("and Placidus is still what a chart takes when nobody has chosen",
            "Placidus".equals(HouseSystems.DEFAULT_NAME)
                && HouseSystems.defaultCode() == 'P');
    }

    // ------------------------------------------------------------------ B

    private static void roundTrip() {
        for (HouseSystems.System s : HouseSystems.ALL) {
            ok(s.name + " survives name to code to name",
                s.name.equals(HouseSystems.nameFor(HouseSystems.codeFor(s.name))));
            ok(s.name + " has a code in the ASCII letters swe_houses takes",
                s.code >= 'A' && s.code <= 'Z');
            ok(s.name + " says what it is, for the hover",
                s.about != null && s.about.length() > 30);
            ok(s.name + " is found by code", HouseSystems.byCode(s.code) == s);
            ok(s.name + " is found by name", HouseSystems.byName(s.name) == s);
        }

        // No two systems may share a code or a name, or one of them is unreachable.
        for (int i = 0; i < HouseSystems.ALL.length; i++) {
            for (int j = i + 1; j < HouseSystems.ALL.length; j++) {
                ok("no two share a code: " + HouseSystems.ALL[i].name + " / "
                    + HouseSystems.ALL[j].name,
                    HouseSystems.ALL[i].code != HouseSystems.ALL[j].code);
                ok("no two share a name",
                    !HouseSystems.ALL[i].name.equals(HouseSystems.ALL[j].name));
            }
        }

        // <b>Precision must answer from the registry, not from a list of its own.</b> That copy
        // was the only one that knew Porphyry, which is how the polar-circle substitute became a
        // system a reader could not choose.
        for (HouseSystems.System s : HouseSystems.ALL) {
            ok("Precision names " + s.name + " the same way",
                s.name.equals(Precision.houseSystemName(s.code)));
        }
        ok("and still names Porphyry, which is not offered but is substituted",
            "Porphyry".equals(Precision.houseSystemName('O')));
    }

    // ------------------------------------------------------------------ C

    private static void castable(SwissEph sw) {
        // A moderate latitude, where every system has a solution. The polar cases are
        // Precision's business and are checked there.
        double jd = SweDate.getJulDay(1982, 8, 10, 19.0);
        for (HouseSystems.System s : HouseSystems.ALL) {
            double[] cusps = new double[13];
            double[] ascmc = new double[10];
            // <b>Caught, because a system that is not twelve houses throws rather than fails.</b>
            // Gauquelin's 'G' fills thirty-six sectors and runs off the end of a twelve-cusp
            // array - and an exception here would take the suite down and report nothing, which
            // is the outcome least likely to tell anyone what went wrong.
            int rc;
            try {
                rc = sw.swe_houses(jd, 0, 39.9526, -75.1652, s.code, cusps, ascmc);
            } catch (RuntimeException notTwelveHouses) {
                ok(s.name + " casts into a twelve-cusp array: " + notTwelveHouses, false);
                continue;
            }
            ok(s.name + " casts at Philadelphia", rc >= 0);
            if (rc < 0) {
                continue;
            }
            // Twelve real degrees, and the wheel assumes they run forwards round the circle.
            boolean sane = true;
            for (int h = 1; h <= 12; h++) {
                if (!(cusps[h] >= 0.0 && cusps[h] < 360.0)) {
                    sane = false;
                }
            }
            ok(s.name + " gives twelve degrees in range", sane);
            double total = 0.0;
            for (int h = 1; h <= 12; h++) {
                double next = h == 12 ? cusps[1] : cusps[h + 1];
                double span = ((next - cusps[h]) % 360.0 + 360.0) % 360.0;
                ok(s.name + " house " + h + " is a real span (" + String.format("%.2f", span)
                    + ")", span > 0.0 && span < 360.0);
                total += span;
            }
            ok(s.name + " twelve houses close the circle (" + String.format("%.4f", total) + ")",
                Math.abs(total - 360.0) < 0.0001);
            boolean onCusp = Math.abs(Aspects.separation(ascmc[0], cusps[1])) < 0.0001;
            boolean elsewhere = ASC_ELSEWHERE.indexOf(s.code) >= 0;
            ok(s.name + (elsewhere ? " deliberately puts the Ascendant off the first cusp"
                : " puts the Ascendant on the first cusp"), onCusp != elsewhere);
        }

        // The high-latitude claim each entry makes, against the engine rather than against
        // received wisdom: Placidus and Koch are the two that fail, and this says so.
        for (HouseSystems.System s : HouseSystems.ALL) {
            // Guarded for the same reason as above, and it found something: housesFellBack
            // allocates a twelve-cusp array of its own, so a thirty-six sector system throws
            // ArrayIndexOutOfBounds out of the ephemeris rather than returning an error. Latent
            // while nothing offers 'G', and a third reason it stays out.
            boolean fails;
            try {
                fails = Precision.housesFellBack(sw, jd, 69.65, 18.96, s.code);   // Tromso
            } catch (RuntimeException notTwelveHouses) {
                ok(s.name + " can be asked whether it has a solution: " + notTwelveHouses, false);
                continue;
            }
            ok(s.name + (s.failsNearThePoles ? " has no solution at 69.6N"
                : " still works at 69.6N"), fails == s.failsNearThePoles);
        }
    }

    // ------------------------------------------------------------------ D

    private static void unknown() {
        // <b>A name from the future, or from a hand-edited file.</b> Refusing to draw a chart
        // because a string was not recognised is a worse answer than drawing the usual one.
        // <b>Caught, because the failure mode being tested for IS a throw.</b> An unguarded
        // call here takes the whole suite down with it, and a crashed suite reports no failures
        // at all - so the one assertion that matters would be the one you never see.
        for (String odd : new String[] {"Gauquelin", "", "   ", "placidus", "Nonsense"}) {
            boolean fellBack;
            try {
                fellBack = HouseSystems.codeFor(odd) == HouseSystems.defaultCode();
            } catch (RuntimeException threw) {
                fellBack = false;
            }
            ok("'" + odd + "' falls back to the default rather than throwing", fellBack);
        }
        ok("a null name does not throw", HouseSystems.byName(null) == null);
        // But a code the engine can return still gets a name, because a notice has to say which
        // system it substituted.
        ok("an unoffered code is still named", "Horizontal".equals(HouseSystems.nameFor('H')));
        ok("and a meaningless one says so", "The selected".equals(HouseSystems.nameFor('%')));
    }

    // ------------------------------------------------------------------ E

    private static void absent() {
        // <b>Gauquelin is excluded permanently, not pending.</b> 'G' divides the chart into
        // thirty-six sectors rather than twelve houses, and every cusp array, wheel ring and
        // house-based reading in this app assumes twelve. Asserted so that adding it reads as
        // the deliberate act it would have to be.
        ok("Gauquelin's 36 sectors are not offered", HouseSystems.byCode('G') == null);
        for (HouseSystems.System s : HouseSystems.ALL) {
            ok(s.name + " is not the Gauquelin code", s.code != 'G');
        }
    }

    // ------------------------------------------------------------------

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
