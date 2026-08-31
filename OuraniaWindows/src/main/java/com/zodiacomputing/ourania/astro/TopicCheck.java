package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.List;

/**
 * Verification for L6: topics by the three-witness method.
 *
 * Every structural claim is re-derived from the frame rather than read back out of the class
 * under test - the ruler is recomputed from the cusp sign, the standings from
 * conditionBaseline, the Lot placements from the longitudes.
 *
 * The assertion this suite exists for is the last one: <b>the prose must not contain the
 * topic score</b>. The spec is explicit that the disagreement between witnesses carries more
 * information than their mean, and that averaging them into a printed number destroys it. A
 * strength is computed for ranking, so nothing but a check stops it leaking into the text.
 */
public final class TopicCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    // Synthetic reference chart - 1984-09-08 07:33 UT, 41.8781 N 87.6298 W.
    // Not anyone's real birth data. Shared with JoyCheck; change them together.
    private static final int    NATAL_Y = 1984;
    private static final int    NATAL_M = 9;
    private static final int    NATAL_D = 8;
    private static final double NATAL_UT = 7.0 + 33.0 / 60.0;
    private static final double NATAL_LAT = 41.8781;
    private static final double NATAL_LON = -87.6298;

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        SwissEph sw = new SwissEph(EPHE_PATH);
        double natalJd = new SweDate(NATAL_Y, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, natalJd, NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        List<BodyScore.Vector> ranked = BodyScore.rank(f, Gestalt.compute(f));

        System.out.println("=== Part A: structure ===");
        List<Topics.Topic> topics = partA(f, ranked);

        System.out.println();
        System.out.println("=== Part B: the three witnesses ===");
        partB(f, ranked, topics);

        System.out.println();
        System.out.println("=== Part C: agreement patterns ===");
        partC(topics);

        System.out.println();
        System.out.println("=== Part D: the prose carries no score ===");
        partD(topics);

        System.out.println();
        System.out.println("=== Part E: whole sign against quadrant ===");
        partE(f, ranked);

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES - " + checks + " checks, " + failures + " failures.");
        }
    }

    // ------------------------------------------------------------------ A: structure

    private static List<Topics.Topic> partA(ChartFrame f, List<BodyScore.Vector> ranked) {
        List<Topics.Topic> topics = Topics.analyse(f, ranked);
        eq("twelve topics", 12, topics.size());

        boolean[] seen = new boolean[13];
        for (Topics.Topic t : topics) {
            yes("house in range", t.house >= 1 && t.house <= 12);
            yes("each house appears once", !seen[t.house]);
            seen[t.house] = true;
            eq("angularity is the four angles", t.house == 1 || t.house == 4
                || t.house == 7 || t.house == 10, t.angular);
        }

        // Every ok body lands in exactly one topic.
        int placed = 0;
        for (Topics.Topic t : topics) {
            placed += t.occupants.size();
        }
        // Counted over the population Topics actually places, which is BodyScore.rank -
        // and that stopped including the four angles on 2026-08-21. Comparing against every
        // ok entry in f.bodies made this read 28 against 24 and fail for a reason that had
        // nothing to do with the partition.
        //
        // Excluding them is right rather than merely convenient. Topics names the loudest
        // occupant as each house's witness, so while the angles were ranked the Ascendant
        // spoke for the 1st house and the MC for the 10th in every chart ever cast - not
        // because anything was there, but because an angle is angular by definition.
        //
        // The claim being checked is unchanged and still bites: nothing placed twice,
        // nothing dropped.
        int okBodies = 0;
        for (int bi = 0; bi < f.bodies.length && bi < Bodies.count(); bi++) {
            ChartFrame.Body b = f.bodies[bi];
            if (b != null && b.ok && !Bodies.at(bi).isAngle()) {
                okBodies++;
            }
        }
        eq("every body occupies exactly one house", okBodies, placed);

        // Ranking is descending, and stable on house number.
        double prev = Double.MAX_VALUE;
        for (Topics.Topic t : topics) {
            yes("topics are ranked", t.strength <= prev + 1e-9);
            prev = t.strength;
        }
        eq("inHouseOrder really is house order", 1,
            Topics.inHouseOrder(topics).get(0).house);

        // The spec's own worked example of a derived house.
        eq("the 10th from the 7th is the 4th", 4, Topics.derived(10, 7));
        eq("the 1st from the 1st is the 1st", 1, Topics.derived(1, 1));

        System.out.printf("topics worth reporting: %d of 12%n",
            topics.stream().filter(t -> t.worthReporting).count());
        for (Topics.Topic t : Topics.inHouseOrder(topics)) {
            System.out.printf("  h%-2d %-11s ruler %-8s %-32s %s%n", t.house,
                Zodiac.SIGNS[t.cuspSign], t.rulerName, t.agreement,
                t.worthReporting ? String.join("; ", t.reportReasons) : "-");
        }
        return topics;
    }

    // ------------------------------------------------------------------ B: witnesses

    private static void partB(ChartFrame f, List<BodyScore.Vector> ranked,
                              List<Topics.Topic> topics) {
        int ascSign = Zodiac.signIndex(f.asc);
        for (Topics.Topic t : topics) {
            // Cusp and whole-sign signs, re-derived.
            eq("cusp sign re-derived h" + t.house,
                Zodiac.signIndex(f.cusps[t.house]), t.cuspSign);
            eq("whole-sign sign re-derived h" + t.house,
                Math.floorMod(ascSign + t.house - 1, 12), t.wholeSignSign);
            eq("signsDisagree agrees with the two signs",
                t.cuspSign != t.wholeSignSign, t.signsDisagree);

            // Witness 2 always speaks - the point of the layer.
            eq("the ruler is the domicile ruler of the cusp sign h" + t.house,
                Dignity.domicileRulerOf(t.cuspSign), t.rulerName);
            yes("the ruler witness never abstains h" + t.house,
                t.rulerWitness.standing != Topics.Standing.ABSENT);
            yes("the ruler witness names the ruler",
                t.rulerName.equals(t.rulerWitness.body));

            // Witness 1 abstains exactly when the house is empty.
            eq("the house witness abstains iff empty h" + t.house,
                t.occupants.isEmpty(),
                t.houseWitness.standing == Topics.Standing.ABSENT);

            // Standings re-derived from the documented baseline.
            for (Topics.Witness w : new Topics.Witness[]{
                    t.houseWitness, t.rulerWitness, t.significatorWitness}) {
                if (w.standing == Topics.Standing.ABSENT) {
                    yes("an absent witness has no strength", Double.isNaN(w.strength));
                    continue;
                }
                Topics.Standing expect;
                if (w.strength >= BodyScore.conditionBaseline + Topics.strongMargin) {
                    expect = Topics.Standing.STRONG;
                } else if (w.strength <= BodyScore.conditionBaseline - Topics.strongMargin) {
                    expect = Topics.Standing.WEAK;
                } else {
                    expect = Topics.Standing.MIDDLING;
                }
                eq("standing re-derived (" + w.kind + " h" + t.house + ")",
                    expect, w.standing);
            }

            // Occupants really are in this house.
            for (BodyScore.Vector v : t.occupants) {
                eq("an occupant's house matches the topic", t.house, v.house);
            }

            // The ruler edge points at the ruler's actual house.
            if (t.ruler != null) {
                eq("the ruler edge lands where the ruler is", t.ruler.house, t.rulerHouse);
            }
        }

        // Lots land where the longitudes say.
        int fortuneHouse = Zodiac.houseOf(f.lotOfFortune, f.cusps);
        int spiritHouse = Zodiac.houseOf(f.lotOfSpirit, f.cusps);
        for (Topics.Topic t : topics) {
            eq("Fortune recorded in the right house (h" + t.house + ")",
                t.house == fortuneHouse, t.lots.contains("Fortune"));
            eq("Spirit recorded in the right house (h" + t.house + ")",
                t.house == spiritHouse, t.lots.contains("Spirit"));
        }
        System.out.printf("Fortune in house %d, Spirit in house %d%n",
            fortuneHouse, spiritHouse);
    }

    // ------------------------------------------------------------------ C: patterns

    private static void partC(List<Topics.Topic> topics) {
        for (Topics.Topic t : topics) {
            switch (t.agreement) {
                case QUIET_COMPETENCE:
                    yes("quiet competence means an empty house", t.occupants.isEmpty());
                    eq("quiet competence means a strong ruler",
                        Topics.Standing.STRONG, t.rulerWitness.standing);
                    break;
                case ACTIVITY_WITHOUT_FOLLOW_THROUGH:
                    yes("activity without follow-through means occupants",
                        !t.occupants.isEmpty());
                    eq("...and a strong house witness",
                        Topics.Standing.STRONG, t.houseWitness.standing);
                    eq("...and a weak ruler",
                        Topics.Standing.WEAK, t.rulerWitness.standing);
                    break;
                case RULER_AND_SIGNIFICATOR_DISAGREE:
                    yes("a disagreement needs both to speak",
                        t.rulerWitness.standing != Topics.Standing.ABSENT
                            && t.significatorWitness.standing != Topics.Standing.ABSENT);
                    yes("a disagreement is strong against weak",
                        t.rulerWitness.standing != t.significatorWitness.standing);
                    break;
                case ALL_STRONG:
                    for (Topics.Witness w : present(t)) {
                        eq("all strong means every present witness is strong",
                            Topics.Standing.STRONG, w.standing);
                    }
                    break;
                case ALL_WEAK:
                    for (Topics.Witness w : present(t)) {
                        eq("all weak means every present witness is weak",
                            Topics.Standing.WEAK, w.standing);
                    }
                    break;
                case ALL_MIDDLING:
                    for (Topics.Witness w : present(t)) {
                        eq("all middling means every present witness is middling",
                            Topics.Standing.MIDDLING, w.standing);
                    }
                    break;
                default:
                    break;
            }
            // A tension implies something to be tense about.
            if (!t.tensions.isEmpty()) {
                yes("tensions come with a reason",
                    t.signsDisagree
                        || t.agreement == Topics.Agreement.ACTIVITY_WITHOUT_FOLLOW_THROUGH
                        || t.agreement == Topics.Agreement.RULER_AND_SIGNIFICATOR_DISAGREE
                        || (t.ruler != null && t.rulerHouse == 12));
            }
            // Reporting is never asserted without a stated reason.
            eq("worthReporting iff a reason was given",
                !t.reportReasons.isEmpty(), t.worthReporting);
            if (t.angular) {
                yes("angular houses are always reported", t.worthReporting);
            }
        }
    }

    private static List<Topics.Witness> present(Topics.Topic t) {
        List<Topics.Witness> out = new java.util.ArrayList<>();
        for (Topics.Witness w : new Topics.Witness[]{
                t.houseWitness, t.rulerWitness, t.significatorWitness}) {
            if (w.standing != Topics.Standing.ABSENT) {
                out.add(w);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ D: no score in prose

    /**
     * The spec's central prohibition, enforced.
     *
     * "Do not average the three into a topic score and print the number." A strength is
     * computed for ranking, so only this check stands between it and the prose.
     */
    private static void partD(List<Topics.Topic> topics) {
        java.util.regex.Pattern anyNumber = java.util.regex.Pattern.compile("\\d*\\.\\d+");
        for (Topics.Topic t : topics) {
            String s = Topics.describe(t);
            yes("the description is not empty", s != null && !s.isEmpty());
            yes("no decimal number in the prose (h" + t.house + "): " + s,
                !anyNumber.matcher(s).find());
            yes("the raw strength does not appear (h" + t.house + ")",
                !s.contains(String.valueOf(t.strength)));
            yes("no null leaked into the prose", !s.contains("null"));
            yes("the description names its house", s.contains("House " + t.house));
        }
        System.out.println("  sample descriptions:");
        for (Topics.Topic t : Topics.inHouseOrder(topics)) {
            if (t.worthReporting) {
                System.out.println("    " + Topics.describe(t));
            }
        }
    }

    // ------------------------------------------------------------------ E: house systems

    private static void partE(ChartFrame f, List<BodyScore.Vector> ranked) {
        List<Topics.Topic> quadrant = Topics.analyse(f, ranked, false, Topics.defaultTopN);
        List<Topics.Topic> whole = Topics.analyse(f, ranked, true, Topics.defaultTopN);
        eq("both systems give twelve topics", 12, whole.size());

        // The spec's structural warning: a body can be in the 9th by whole sign and the 10th
        // by quadrant, which is a different topic. Count how far the two readings diverge.
        int moved = 0;
        for (BodyScore.Vector v : ranked) {
            if (v.house != v.wholeSignHouse) {
                moved++;
            }
        }
        System.out.printf("bodies landing in a different house under whole sign: %d of %d%n",
            moved, ranked.size());

        // Whole-sign topics take their ruler from the whole-sign sign, so the two agree only
        // where the signs do.
        for (Topics.Topic wt : whole) {
            eq("whole-sign ruler comes from the whole-sign sign (h" + wt.house + ")",
                Dignity.domicileRulerOf(wt.wholeSignSign), wt.rulerName);
        }
        for (Topics.Topic qt : quadrant) {
            eq("quadrant ruler comes from the cusp sign (h" + qt.house + ")",
                Dignity.domicileRulerOf(qt.cuspSign), qt.rulerName);
        }
        // Under whole sign every body's house equals its whole-sign house, by construction.
        for (Topics.Topic wt : whole) {
            for (BodyScore.Vector v : wt.occupants) {
                eq("whole-sign occupancy uses the whole-sign house", wt.house,
                    v.wholeSignHouse);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void eq(String what, Object expect, Object got) {
        checks++;
        if (!expect.equals(got)) {
            failures++;
            System.out.println("  FAIL " + what + ": expected " + expect + ", got " + got);
        }
    }

    private static void yes(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL " + what);
        }
    }

    private TopicCheck() { }
}
