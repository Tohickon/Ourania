package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies the essential dignity layer. No ephemeris; everything here is table lookup.
 *
 *  Part A - the Egyptian bounds checksum, run against the live table so it can never
 *           drift. Contiguity, five distinct non-luminary rulers per sign, and each
 *           planet's total degrees across the zodiac equal to its planetary years.
 *  Part B - the derived detriment and fall tables against their known values.
 *  Part C - worked placements, including the cases that break naive evaluators.
 *  Part D - peregrination, almuten, and boundary behaviour.
 *
 *   javac -encoding UTF-8 -cp src\main\java -d out-selftest src\main\java\com\zodiacomputing\ourania\astro\*.java
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.DignityCheck
 */
public final class DignityCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        section("Part A: Egyptian bounds checksum", DignityCheck::boundsChecksum);
        section("Part B: derived detriment and fall", DignityCheck::derivedTables);
        section("Part C: worked placements", DignityCheck::workedPlacements);
        section("Part D: peregrination, almuten, boundaries", DignityCheck::peregrineAndAlmuten);
        section("Part E: the scoring scale is ordered", DignityCheck::scaleIsOrdered);
        section("Part F: the wheel's bound edges are the scoring table's",
            DignityCheck::boundEdgesAgree);

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

    // ---------------------------------------------------------------- part A

    /**
     * The planetary-years property: across all twelve signs, each planet's total bound
     * degrees equals its traditional years. Moving any single boundary changes two
     * planets' totals at once, so this catches essentially any transcription slip.
     */
    /**
     * The edges the wheel draws and the ruler the score uses must be one table.
     *
     * <b>boundEdges exists because the bound ring had no way to know where a segment
     * starts.</b> Two accessors onto one table is fine; two tables would not be, and the way
     * that happens is someone corrects one of them. So this walks the zodiac degree by degree
     * and asserts the ruler changes exactly at an edge and nowhere else - which is the same
     * claim as "the ring's divisions land where the score changes", stated without either side
     * being asked to trust the other.
     *
     * <b>The band was drawn for the first time on 2026-09-06.</b> Dignity had scored bound
     * placements since it was written, and the wheel drew nothing for them - an engine with no
     * door, found by David looking at another program's chart and noticing ours had no terms.
     */
    private static void boundEdgesAgree() {
        for (int sign = 0; sign < 12; sign++) {
            double[] edges = Dignity.boundEdges(sign);
            eq("sign " + sign + " has five bounds", 6, edges.length);
            eq("sign " + sign + " starts at 0", 0.0, edges[0]);
            eq("sign " + sign + " closes at 30", 30.0, edges[edges.length - 1]);
            for (int i = 1; i < edges.length; i++) {
                yes("sign " + sign + " edge " + i + " moves outward",
                    edges[i] > edges[i - 1]);
            }

            // Tenth-degree steps: fine enough to land either side of every edge, since the
            // Egyptian bounds are whole degrees.
            String previous = null;
            for (int step = 0; step < 300; step++) {
                double deg = step / 10.0;
                double lon = sign * 30.0 + deg;
                String ruler = Dignity.boundRulerOf(lon);
                boolean atEdge = false;
                for (int i = 1; i < edges.length - 1; i++) {
                    if (Math.abs(deg - edges[i]) < 1e-9) {
                        atEdge = true;
                    }
                }
                if (previous != null) {
                    if (atEdge) {
                        yes("sign " + sign + ": the ruler changes at edge " + deg,
                            !ruler.equals(previous));
                    } else {
                        yes("sign " + sign + ": the ruler holds at " + deg,
                            ruler.equals(previous));
                    }
                }
                previous = ruler;
            }
        }
    }

    private static void boundsChecksum() {
        Map<String, Integer> totals = new HashMap<>();
        Object[][][] table = Dignity.boundsTable();

        for (int s = 0; s < 12; s++) {
            int prev = 0;
            List<String> rulers = new ArrayList<>();
            for (Object[] seg : table[s]) {
                String ruler = (String) seg[0];
                int start = (Integer) seg[1];
                int end = (Integer) seg[2];
                eq(Zodiac.SIGNS[s] + " bound " + ruler + " starts where the last ended", prev, start);
                prev = end;
                totals.merge(ruler, end - start, Integer::sum);
                rulers.add(ruler);
                checks++;
                if (ruler.equals("Sun") || ruler.equals("Moon")) {
                    failures.add(Zodiac.SIGNS[s] + ": a luminary rules a bound");
                }
            }
            eq(Zodiac.SIGNS[s] + " bounds end at 30", 30, prev);
            eq(Zodiac.SIGNS[s] + " has 5 distinct bound rulers", 5,
                (int) rulers.stream().distinct().count());
        }

        eq("Saturn total bound degrees", 57, totals.get("Saturn"));
        eq("Jupiter total bound degrees", 79, totals.get("Jupiter"));
        eq("Mars total bound degrees", 66, totals.get("Mars"));
        eq("Venus total bound degrees", 82, totals.get("Venus"));
        eq("Mercury total bound degrees", 76, totals.get("Mercury"));
        eq("all bounds sum to 360", 360, totals.values().stream().mapToInt(Integer::intValue).sum());

        System.out.println("  planetary years: Saturn " + totals.get("Saturn")
            + ", Jupiter " + totals.get("Jupiter") + ", Mars " + totals.get("Mars")
            + ", Venus " + totals.get("Venus") + ", Mercury " + totals.get("Mercury"));
    }

    // ---------------------------------------------------------------- part B

    /** Detriment and fall are derived, so check them against the values they must produce. */
    private static void derivedTables() {
        String[][] detriment = {
            {"aries", "Venus"}, {"taurus", "Mars"}, {"gemini", "Jupiter"}, {"cancer", "Saturn"},
            {"leo", "Saturn"}, {"virgo", "Jupiter"}, {"libra", "Mars"}, {"scorpio", "Venus"},
            {"sagittarius", "Mercury"}, {"capricorn", "Moon"}, {"aquarius", "Sun"}, {"pisces", "Mercury"}
        };
        for (String[] row : detriment) {
            eq("detriment in " + row[0], row[1], Dignity.detrimentBodyOf(Dignity.indexOfSign(row[0])));
        }

        String[][] fall = {
            {"aries", "Saturn"}, {"cancer", "Mars"}, {"libra", "Sun"}, {"capricorn", "Jupiter"},
            {"scorpio", "Moon"}, {"virgo", "Venus"}, {"pisces", "Mercury"}
        };
        for (String[] row : fall) {
            eq("fall in " + row[0], row[1], Dignity.fallBodyOf(Dignity.indexOfSign(row[0])));
        }

        // Signs with no exaltation must report none in fall either.
        eq("nothing falls in Taurus", null, Dignity.fallBodyOf(Dignity.indexOfSign("taurus")));
        eq("nothing is exalted in Sagittarius", null,
            Dignity.exaltedBodyOf(Dignity.indexOfSign("sagittarius")));
    }

    // ---------------------------------------------------------------- part C

    private static void workedPlacements() {
        // The case that breaks first-match-wins evaluators: Mercury at 17 Pisces is in
        // detriment AND fall AND its own Egyptian bound (Mercury 16-19 Pisces).
        Dignity.Result m17 = show(Dignity.evaluate("Mercury", lon("pisces", 17, 0), true));
        eq("Mercury 17 Pisces detriment", true, m17.detriment);
        eq("Mercury 17 Pisces fall", true, m17.fall);
        eq("Mercury 17 Pisces in own bound", true, m17.bound);
        eq("Mercury 17 Pisces not peregrine", false, m17.peregrine);
        eq("Mercury 17 Pisces score", -5 - 4 + 2, m17.score);

        // Five degrees earlier, same sign, and it is peregrine with no bound to save it.
        Dignity.Result m5 = show(Dignity.evaluate("Mercury", lon("pisces", 5, 0), true));
        eq("Mercury 5 Pisces peregrine", true, m5.peregrine);
        eq("Mercury 5 Pisces score", -5 - 4, m5.score);

        // Sun at its exact exaltation degree, day chart: exalted, day triplicity ruler of
        // fire, and the face of Aries II is the Sun.
        Dignity.Result sun19 = show(Dignity.evaluate("Sun", lon("aries", 18, 30), true));
        eq("Sun 19 Aries exalted", true, sun19.exaltation);
        eq("Sun 19 Aries on the exact degree", true, sun19.exactExaltationDegree);
        eq("Sun 19 Aries day triplicity", true, sun19.triplicity);
        eq("Sun 19 Aries in own face", true, sun19.face);
        eq("Sun 19 Aries score", 4 + 3 + 1, sun19.score);

        // Same Sun in a night chart loses the triplicity: Jupiter rules fire by night.
        eq("Sun 19 Aries by night loses triplicity", false,
            Dignity.evaluate("Sun", lon("aries", 18, 30), false).triplicity);

        // Saturn in its own sign and its own bound.
        Dignity.Result sat = show(Dignity.evaluate("Saturn", lon("capricorn", 23, 0), true));
        eq("Saturn 23 Capricorn domicile", true, sat.domicile);
        eq("Saturn 23 Capricorn in own bound", true, sat.bound);
        eq("Saturn 23 Capricorn score", 5 + 2, sat.score);

        // From the live chart run: Saturn at 14 43 Aries in a day chart. Fall, and the
        // participating triplicity ruler of fire, which scores nothing by default.
        Dignity.Result satAries = show(Dignity.evaluate("Saturn", lon("aries", 14, 43), true));
        eq("Saturn 14 Aries fall", true, satAries.fall);
        eq("Saturn 14 Aries participating triplicity", true, satAries.triplicityParticipating);
        eq("Saturn 14 Aries triplicity not scored", false, satAries.triplicity);
        eq("Saturn 14 Aries score", -4, satAries.score);
        // Participating rulership is still dignity, so this is not peregrine even though
        // the participating ruler scores nothing under the default setting.
        eq("Saturn 14 Aries not peregrine", false, satAries.peregrine);

        // Flipping the setting changes the score, which is why it belongs in settings.
        Dignity.countParticipatingTriplicity = true;
        eq("Saturn 14 Aries with participating counted", -4 + 3,
            Dignity.evaluate("Saturn", lon("aries", 14, 43), true).score);
        Dignity.countParticipatingTriplicity = false;
    }

    // ---------------------------------------------------------------- part D

    /**
     * The scale reads in order, whatever the peregrine penalty is set to.
     *
     * <b>This is the invariant the flat -5 broke.</b> At -5 a peregrine planet scored level
     * with detriment and BELOW fall, so "no support" ranked worse than "actively brought low"
     * - which is not a claim any of the sources actually make, it is an artefact of choosing a
     * round number to widen a gap. The scale has to descend: domicile, exaltation, triplicity,
     * bound, face, then peregrine, then fall, then detriment.
     *
     * <b>Asserted against the setting rather than against a literal</b>, so switching the
     * engine into classical mode for horary fails this loudly instead of silently inverting
     * the order behind a reader's back.
     */
    private static void scaleIsOrdered() {
        eq("domicile outranks exaltation", true, Dignity.PTS_DOMICILE > Dignity.PTS_EXALTATION);
        eq("exaltation outranks triplicity", true, Dignity.PTS_EXALTATION > Dignity.PTS_TRIPLICITY);
        eq("triplicity outranks bound", true, Dignity.PTS_TRIPLICITY > Dignity.PTS_BOUND);
        eq("bound outranks face", true, Dignity.PTS_BOUND > Dignity.PTS_FACE);
        eq("face outranks peregrine", true, Dignity.PTS_FACE > Dignity.peregrinePenalty);
        eq("detriment is the worst place to be", true,
            Dignity.PTS_DETRIMENT < Dignity.PTS_FALL);

        // The three named values, so a fourth cannot be introduced without a decision.
        eq("classical peregrine is Lilly's -5", -5, Dignity.PTS_PEREGRINE_CLASSICAL);
        eq("graduated peregrine is -2", -2, Dignity.PTS_PEREGRINE_GRADUATED);
        eq("modern peregrine is neutral", 0, Dignity.PTS_PEREGRINE_MODERN);
        eq("the default is the graduated one",
            Dignity.PTS_PEREGRINE_GRADUATED, Dignity.peregrinePenalty);

        // <b>In the default mode a wanderer must outrank a casualty.</b> The classical value
        // deliberately does not satisfy this, which is exactly why it is not the default.
        eq("peregrine is not worse than fall", true,
            Dignity.peregrinePenalty > Dignity.PTS_FALL);
        eq("and the classical value would fail that", true,
            Dignity.PTS_PEREGRINE_CLASSICAL < Dignity.PTS_FALL);
    }

    private static void peregrineAndAlmuten() {
        // Every degree of the zodiac must be covered by exactly one bound and one face,
        // and evaluate() must never throw.
        for (int s = 0; s < 12; s++) {
            for (int d = 0; d < 30; d++) {
                double l = lon(Zodiac.SIGNS[s], d, 30);
                checks++;
                try {
                    Dignity.boundRulerOf(l);
                    Dignity.faceRulerOf(l);
                    for (String b : Dignity.TRADITIONAL) {
                        Dignity.evaluate(b, l, true);
                        Dignity.evaluate(b, l, false);
                    }
                } catch (RuntimeException e) {
                    failures.add("evaluate threw at " + Zodiac.format(l) + ": " + e);
                }
            }
        }

        // Bound boundaries are inclusive start, exclusive end.
        eq("5.99 Aries is Jupiter's bound", "Jupiter", Dignity.boundRulerOf(lon("aries", 5, 59)));
        eq("exactly 6 Aries is Venus's bound", "Venus", Dignity.boundRulerOf(6.0));
        eq("29.99 Pisces is Saturn's bound", "Saturn", Dignity.boundRulerOf(lon("pisces", 29, 59)));

        // Every degree must have an almuten, and a planet in its own domicile plus bound
        // should win its own degree.
        for (int s = 0; s < 12; s++) {
            checks++;
            if (Dignity.almutenOf(lon(Zodiac.SIGNS[s], 15, 0), true) == null) {
                failures.add("no almuten at 15 " + Zodiac.SIGNS[s]);
            }
        }
        eq("almuten of 23 Capricorn is Saturn", "Saturn",
            Dignity.almutenOf(lon("capricorn", 23, 0), true));

        // Non-traditional bodies get no dignity rather than a fabricated zero.
        Dignity.Result pluto = Dignity.evaluate("Pluto", 100.0, true);
        eq("Pluto has no dignity score", 0, pluto.score);
        eq("Pluto is not marked peregrine", false, pluto.peregrine);
    }

    // ---------------------------------------------------------------- helpers

    private static double lon(String sign, int deg, int min) {
        return Zodiac.longitudeOf(Dignity.indexOfSign(sign), deg, min, 0);
    }

    private static Dignity.Result show(Dignity.Result r) {
        System.out.printf("  %-8s %-16s %+3d  %s%n", r.body,
            String.format("%.0f %s", Math.floor(r.degreeInSign) + 1, r.sign),
            r.score, String.join("; ", r.reasons));
        return r;
    }

    private static void section(String title, Runnable body) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println(title.split(":")[0] + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    /** For claims that are not an equality - ordering, membership, a boundary landing. */
    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
