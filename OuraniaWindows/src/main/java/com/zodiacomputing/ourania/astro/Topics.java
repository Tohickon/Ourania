package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * L6: topic and house analysis by the three-witness method.
 *
 * The problem this layer exists for: twelve houses, ten bodies, so five to seven houses are
 * <b>empty</b>. A reading that describes only occupants says nothing about half a person's
 * life - usually the half they asked about. The traditional answer is that a house is read
 * through three witnesses, and occupancy is only one of them:
 *
 * <ol>
 *   <li><b>The house itself</b> - its sign, its occupants with their full L3 vectors, its
 *       angularity. Present in roughly half of houses; <i>absent is not a failure, it is this
 *       witness abstaining</i>.</li>
 *   <li><b>The house ruler</b> - the ruler of the cusp sign, wherever it is, with its complete
 *       L3 condition. Always present. This is the primary witness for an empty house and the
 *       one most software omits.</li>
 *   <li><b>The natural significator</b> - the body that signifies the topic universally,
 *       with its condition.</li>
 * </ol>
 *
 * <h3>The disagreement is the reading</h3>
 *
 * The spec is emphatic and this class obeys it: <b>do not average the three into a topic score
 * and print the number.</b> A strength is computed, but only for ranking, and
 * {@link #describe} generates text from the <i>pattern</i> of agreement, never from the mean.
 * Occupancy strong with a weak ruler says something specific - activity without follow-through
 * - that an average would erase.
 *
 * <h3>Three bands, not two</h3>
 *
 * A witness is strong, middling or weak. The middling band is required rather than tidy: the
 * spec asks that a house whose witnesses are all middling and agree gets one sentence or
 * nothing, and a two-way split would make every such house either strong or weak. The
 * boundary is {@link BodyScore#conditionBaseline} - the existing documented neutral point,
 * not a new number - widened by {@link #strongMargin} so that "strong" means meaningfully
 * above neutral rather than a hair above it.
 */
public final class Topics {

    /**
     * How far from {@link BodyScore#conditionBaseline} a witness must sit to count as strong
     * or weak rather than middling.
     *
     * <b>Chosen from a plateau, not by taste.</b> The first value tried was 0.10, and it was
     * demonstrably wrong: `conditionPartial` clusters, with three bodies sitting at exactly
     * 0.590 on the test chart, so a strong threshold of 0.60 cut through the middle of a
     * cluster and flipped Jupiter, Saturn and the Moon to middling on a hair. Nine topics out
     * of twelve then came out ALL_MIDDLING and the layer said almost nothing.
     *
     * Sweeping the margin shows where the answer is stable:
     *
     * <pre>
     *   margin  bodies outside the middling band (of 12)   ALL_MIDDLING topics (of 12)
     *   0.02    7                                          0
     *   0.04    7                                          1
     *   0.05    7                                          2
     *   0.06    7                                          2
     *   0.08    7                                          2
     *   0.10    4                                          9    &lt;- cliff
     *   0.15    2                                          11
     * </pre>
     *
     * The answer is identical across 0.02-0.08, so 0.06 looked like the middle of a plateau.
     *
     * <h3>The corpus refuted that (2026-08-10)</h3>
     *
     * Swept across the same 324-chart corpus {@code JoyCheck} uses, <b>there is no plateau</b>.
     * The strong+weak count falls smoothly and monotonically from margin 0.01 to 0.20; the
     * per-step changes are noise around a steady slope, not a flat region. The one-chart
     * plateau was an artefact of twelve quantised values, exactly the risk the old caveat
     * named.
     *
     * <pre>
     *   margin   strong  middling    weak   ALL_MIDDLING topics (of 3888)
     *   0.06       4958      2469    2701    176   (4.5%)
     *   0.10       4091      3870    2167    442   (11.4%)
     *   0.15       3095      5646    1387   1033   (26.6%)
     *   0.20       2224      7189     715   1722   (44.3%)
     * </pre>
     *
     * <h3>The judgement was made on 2026-09-03 (DECISIONS.md, K6)</h3>
     *
     * The old note ended by saying that what fraction of topics <i>should</i> read as
     * unremarkable was a product judgement nobody had made. It has been made: <b>30 to 35
     * per cent</b>, and the margin follows from it rather than the other way round.
     *
     * The reasoning is about alarm fatigue. At 4.5% almost every area of a life is flagged as
     * a crisis or a threshold, and when everything is a red light none of them reads as one.
     * At the other end, 44.3% leaves so much of the chart quiet that the reading feels
     * unfinished. A third of topics resting is what lets the other two thirds be heard.
     *
     * Re-swept over the same 324-chart corpus to place the value:
     *
     * <pre>
     *   margin   ALL_MIDDLING topics (of 3888)   share
     *   0.06        128                           3.3%
     *   0.10        363                           9.3%
     *   0.15        916                          23.6%
     *   0.17       1212                          31.2%   &lt;- chosen
     *   0.18       1287                          33.1%
     *   0.20       1629                          41.9%
     * </pre>
     *
     * 0.17 is taken rather than 0.18 because it sits mid-band rather than near the top of it,
     * and the run above reads about a point low against the 2026-08-10 sweep in this same
     * comment - 3.3% where that recorded 4.5% at the same margin - so the true figure is
     * nearer 32%. Either reading is inside the target; 0.18 would not be under both.
     *
     * <b>The state is still a finding, and now says so.</b> It used to render as "X is
     * unremarkable", which reads as the app having nothing to say. Integrated ground is a
     * condition: three witnesses that neither push nor drag, an area asking to be neither
     * defended nor repaired. That is worth a sentence, and at a third of topics it is worth
     * writing properly.
     *
     * <h3>The finding that matters more: the asymmetry is structural</h3>
     *
     * Topic standings run about 2:1 strong-to-weak at every margin, and <b>no margin fixes
     * that</b>, because it is not a centring error. Measured over the corpus:
     *
     * <pre>
     *   all bodies        median 0.500   53.2% above baseline   (baseline is correctly centred)
     *   ruler witnesses   median 0.569   61.6% above baseline
     * </pre>
     *
     * The cause is witness selection. {@link Dignity} draws domicile rulers from the
     * traditional seven only, so Uranus, Neptune, Pluto, Chiron and the Node can occupy a
     * house but can never rule one - and having no essential dignity they score low. They drag
     * the body median down while never appearing as a ruler witness.
     *
     * The real fix, if this matters, is to centre each witness kind's band on that kind's own
     * median, so "strong" means strong <i>for a ruler</i> rather than strong for a body.
     * Deliberately not done here: it changes what every standing means and is a design
     * decision, not a tuning.
     */
    public static double strongMargin = 0.17;

    /** How many of the prominence ranking count as "a body that matters" for reporting. */
    public static int defaultTopN = 5;

    /** Occupants needed before a house counts as holding a stellium. */
    public static int stelliumSize = 3;

    /**
     * Natural significators by house, 1..12.
     *
     * The spec's own warning, restated because it matters: these vary between authors far
     * more than the dignity tables do. This is a choice, not a canon, and it belongs in the
     * settings block rather than being treated as fact.
     */
    private static final String[][] SIGNIFICATORS = {
        {},                                          // index 0 unused
        {"Sun", "Moon"},                             // 1  - plus the Ascendant ruler, added below
        {"Jupiter", "Venus"},                        // 2
        {"Mercury", "Moon"},                         // 3
        {"Moon", "Saturn"},                          // 4
        {"Venus", "Jupiter"},                        // 5
        {"Mars", "Saturn", "Mercury"},               // 6
        {"Venus", "Moon"},                           // 7
        {"Saturn", "Mars"},                          // 8
        {"Jupiter", "Sun"},                          // 9
        {"Sun", "Saturn", "Mercury"},                // 10
        {"Jupiter", "Venus"},                        // 11
        {"Saturn"}                                   // 12
    };

    /** Where a witness stands. ABSENT is the house witness abstaining, not a weak reading. */
    public enum Standing { STRONG, MIDDLING, WEAK, ABSENT }

    /**
     * The patterns the spec names. These drive the prose; the numeric strength does not.
     */
    public enum Agreement {
        ALL_STRONG,
        ALL_WEAK,
        ALL_MIDDLING,
        /** Occupancy strong, ruler weak - much happens here, little consolidates. */
        ACTIVITY_WITHOUT_FOLLOW_THROUGH,
        /** Ruler strong, house empty - nothing dramatic, and it works. */
        QUIET_COMPETENCE,
        /** The person's version of the topic differs from the conventional one. */
        RULER_AND_SIGNIFICATOR_DISAGREE,
        /** Two witnesses agree and one dissents. */
        MAJORITY_WITH_DISSENT,
        MIXED
    }

    public static final class Witness {
        /** "house", "ruler" or "significator". */
        public String kind;
        /** The body speaking, or null when the house witness abstains. */
        public String body;
        public Standing standing = Standing.ABSENT;
        /** L3 condition, or NaN when absent. Never printed as a topic score. */
        public double strength = Double.NaN;
        public String detail = "";

        @Override
        public String toString() {
            return standing == Standing.ABSENT
                ? kind + ": abstains"
                : String.format("%s: %s %s", kind, body, standing.toString().toLowerCase());
        }
    }

    public static final class Topic {
        public int house;
        public int cuspSign;
        public int wholeSignSign;
        /** The cusp sign and the whole-sign sign differ - the topic boundary is contested. */
        public boolean signsDisagree;
        public boolean angular;

        public final List<BodyScore.Vector> occupants = new ArrayList<>();
        public String rulerName = "";
        public BodyScore.Vector ruler;
        /** The L4 edge: this house -> its ruler -> the ruler's house. */
        public int rulerHouse;
        public final List<BodyScore.Vector> significators = new ArrayList<>();
        /** "Fortune" and/or "Spirit" when a Lot falls in this house. */
        public final List<String> lots = new ArrayList<>();

        public Witness houseWitness = new Witness();
        public Witness rulerWitness = new Witness();
        public Witness significatorWitness = new Witness();

        public Agreement agreement = Agreement.MIXED;
        /** Ranking only. Deliberately never rendered as a number - see the class comment. */
        public double strength;
        public final List<String> tensions = new ArrayList<>();

        public boolean worthReporting;
        public final List<String> reportReasons = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("house %d (%s): %s", house,
                capitalise(Zodiac.SIGNS[cuspSign]), agreement);
        }
    }

    private Topics() { }

    /**
     * All twelve topics, ranked, with the reporting decision already made.
     *
     * @param whole read topics by whole-sign house rather than by quadrant. L0 owns which is
     *              canonical; both are computed by L2 and this layer is the one where the
     *              choice changes the answer most - a body in the 9th by whole sign and the
     *              10th by Placidus moves from belief to career, which are different topics.
     */
    public static List<Topic> analyse(ChartFrame f, List<BodyScore.Vector> ranked,
                                      boolean whole, int topN) {
        List<Topic> out = new ArrayList<>();
        if (f == null || ranked == null) {
            return out;
        }
        Map<String, BodyScore.Vector> byBody = new HashMap<>();
        for (BodyScore.Vector v : ranked) {
            byBody.put(v.body, v);
        }
        java.util.Set<String> prominent = new java.util.LinkedHashSet<>();
        for (int i = 0; i < ranked.size() && i < topN; i++) {
            prominent.add(ranked.get(i).body);
        }

        int ascSign = Zodiac.signIndex(f.asc);
        String ascRuler = Dignity.domicileRulerOf(ascSign);

        for (int h = 1; h <= 12; h++) {
            Topic t = new Topic();
            t.house = h;
            t.cuspSign = Zodiac.signIndex(f.cusps[h]);
            t.wholeSignSign = Math.floorMod(ascSign + h - 1, 12);
            t.signsDisagree = t.cuspSign != t.wholeSignSign;
            t.angular = h == 1 || h == 4 || h == 7 || h == 10;

            int readingSign = whole ? t.wholeSignSign : t.cuspSign;

            // Witness 1: the house and whoever is in it.
            for (BodyScore.Vector v : ranked) {
                int vh = whole ? v.wholeSignHouse : v.house;
                if (vh == h) {
                    t.occupants.add(v);
                }
            }
            t.houseWitness.kind = "house";
            if (t.occupants.isEmpty()) {
                t.houseWitness.standing = Standing.ABSENT;
                t.houseWitness.detail = "no bodies here - this witness abstains";
            } else {
                BodyScore.Vector loudest = t.occupants.get(0);
                for (BodyScore.Vector v : t.occupants) {
                    if (v.prominence > loudest.prominence) {
                        loudest = v;
                    }
                }
                t.houseWitness.body = loudest.body;
                t.houseWitness.strength = loudest.conditionPartial;
                t.houseWitness.standing = standingOf(loudest.conditionPartial);
                t.houseWitness.detail = t.occupants.size() == 1
                    ? loudest.body + " here"
                    : t.occupants.size() + " bodies here, loudest " + loudest.body;
            }

            // Witness 2: the ruler. Always speaks.
            t.rulerName = Dignity.domicileRulerOf(readingSign);
            t.ruler = byBody.get(t.rulerName);
            t.rulerWitness.kind = "ruler";
            t.rulerWitness.body = t.rulerName;
            if (t.ruler != null) {
                t.rulerHouse = whole ? t.ruler.wholeSignHouse : t.ruler.house;
                t.rulerWitness.strength = t.ruler.conditionPartial;
                t.rulerWitness.standing = standingOf(t.ruler.conditionPartial);
                t.rulerWitness.detail = String.format("%s in %s, house %d",
                    t.rulerName, capitalise(Zodiac.SIGNS[Zodiac.signIndex(t.ruler.longitude)]),
                    t.rulerHouse);
            } else {
                t.rulerWitness.detail = t.rulerName + " not in this chart's bodies";
            }

            // Witness 3: the natural significators, plus the Ascendant ruler for the 1st.
            List<String> sigNames = new ArrayList<>(java.util.Arrays.asList(SIGNIFICATORS[h]));
            if (h == 1 && !sigNames.contains(ascRuler)) {
                sigNames.add(ascRuler);
            }
            double sigSum = 0;
            int sigCount = 0;
            BodyScore.Vector bestSig = null;
            for (String name : sigNames) {
                BodyScore.Vector v = byBody.get(name);
                if (v == null) {
                    continue;
                }
                t.significators.add(v);
                sigSum += v.conditionPartial;
                sigCount++;
                if (bestSig == null || v.conditionPartial > bestSig.conditionPartial) {
                    bestSig = v;
                }
            }
            t.significatorWitness.kind = "significator";
            if (sigCount > 0) {
                // Averaged across the significators only - they are one witness speaking with
                // several voices, unlike the three witnesses, which are never averaged.
                double mean = sigSum / sigCount;
                t.significatorWitness.body = bestSig.body;
                t.significatorWitness.strength = mean;
                t.significatorWitness.standing = standingOf(mean);
                t.significatorWitness.detail = String.join(", ", sigNames);
            } else {
                t.significatorWitness.detail = "no significator in this chart";
            }

            // The Lots, where they fall.
            if (houseOfLon(f, f.lotOfFortune, whole, ascSign) == h) {
                t.lots.add("Fortune");
            }
            if (houseOfLon(f, f.lotOfSpirit, whole, ascSign) == h) {
                t.lots.add("Spirit");
            }

            t.agreement = agree(t);
            t.strength = rankingStrength(t);
            addTensions(t);
            decideReporting(t, prominent, topN);
            out.add(t);
        }

        // Loudest first for ranking purposes; the house number is the tiebreak so the order
        // is stable rather than incidental.
        out.sort(Comparator.comparingDouble((Topic t) -> -t.strength)
            .thenComparingInt(t -> t.house));
        return out;
    }

    /** Convenience overload: quadrant houses, standard top-N. */
    public static List<Topic> analyse(ChartFrame f, List<BodyScore.Vector> ranked) {
        return analyse(f, ranked, false, defaultTopN);
    }

    /** Topics in house order, for callers that want to walk 1..12. */
    public static List<Topic> inHouseOrder(List<Topic> topics) {
        List<Topic> out = new ArrayList<>(topics);
        out.sort(Comparator.comparingInt(t -> t.house));
        return out;
    }

    private static Standing standingOf(double condition) {
        if (Double.isNaN(condition)) {
            return Standing.ABSENT;
        }
        if (condition >= BodyScore.conditionBaseline + strongMargin) {
            return Standing.STRONG;
        }
        if (condition <= BodyScore.conditionBaseline - strongMargin) {
            return Standing.WEAK;
        }
        return Standing.MIDDLING;
    }

    /**
     * Which of the spec's patterns this topic matches.
     *
     * Order matters: the specific patterns are tested before the general ones, because
     * "occupancy strong, ruler weak" is also a majority-with-dissent and the specific reading
     * is the informative one.
     */
    private static Agreement agree(Topic t) {
        Standing house = t.houseWitness.standing;
        Standing ruler = t.rulerWitness.standing;
        Standing sig = t.significatorWitness.standing;

        if (house == Standing.STRONG && ruler == Standing.WEAK) {
            return Agreement.ACTIVITY_WITHOUT_FOLLOW_THROUGH;
        }
        if (house == Standing.ABSENT && ruler == Standing.STRONG) {
            return Agreement.QUIET_COMPETENCE;
        }
        if ((ruler == Standing.STRONG && sig == Standing.WEAK)
                || (ruler == Standing.WEAK && sig == Standing.STRONG)) {
            return Agreement.RULER_AND_SIGNIFICATOR_DISAGREE;
        }

        List<Standing> present = new ArrayList<>();
        for (Standing s : new Standing[]{house, ruler, sig}) {
            if (s != Standing.ABSENT) {
                present.add(s);
            }
        }
        if (present.isEmpty()) {
            return Agreement.MIXED;
        }
        boolean allStrong = true;
        boolean allWeak = true;
        boolean allMid = true;
        for (Standing s : present) {
            allStrong &= s == Standing.STRONG;
            allWeak &= s == Standing.WEAK;
            allMid &= s == Standing.MIDDLING;
        }
        if (allStrong) {
            return Agreement.ALL_STRONG;
        }
        if (allWeak) {
            return Agreement.ALL_WEAK;
        }
        if (allMid) {
            return Agreement.ALL_MIDDLING;
        }
        if (present.size() == 3) {
            for (Standing s : Standing.values()) {
                int n = 0;
                for (Standing p : present) {
                    if (p == s) {
                        n++;
                    }
                }
                if (n == 2) {
                    return Agreement.MAJORITY_WITH_DISSENT;
                }
            }
        }
        return Agreement.MIXED;
    }

    /**
     * A number for ordering topics, and for nothing else.
     *
     * The spec forbids printing this or generating prose from it. It exists so that twelve
     * topics can be cut down to the few worth reporting.
     */
    private static double rankingStrength(Topic t) {
        double sum = 0;
        int n = 0;
        for (Witness w : new Witness[]{t.houseWitness, t.rulerWitness, t.significatorWitness}) {
            if (!Double.isNaN(w.strength)) {
                sum += w.strength;
                n++;
            }
        }
        double base = n == 0 ? BodyScore.conditionBaseline : sum / n;
        // Angular houses and stelliums are louder regardless of condition; this is ordering,
        // not judgement.
        if (t.angular) {
            base += 0.05;
        }
        if (t.occupants.size() >= stelliumSize) {
            base += 0.05;
        }
        return base;
    }

    private static void addTensions(Topic t) {
        if (t.agreement == Agreement.ACTIVITY_WITHOUT_FOLLOW_THROUGH) {
            t.tensions.add("occupied but its ruler is poorly placed - much happens, "
                + "little consolidates");
        }
        if (t.agreement == Agreement.RULER_AND_SIGNIFICATOR_DISAGREE) {
            t.tensions.add("the ruler and the natural significator disagree - this person's "
                + "version of the topic is not the conventional one");
        }
        if (t.signsDisagree) {
            t.tensions.add("cusp sign and whole-sign sign differ, so the topic's boundary "
                + "depends on the house system");
        }
        if (t.ruler != null && t.rulerHouse == 12 && t.house != 12) {
            t.tensions.add("its ruler is hidden in the 12th");
        }
    }

    private static void decideReporting(Topic t, java.util.Set<String> prominent, int topN) {
        for (BodyScore.Vector v : t.occupants) {
            if (prominent.contains(v.body)) {
                t.reportReasons.add("holds " + v.body + ", top-" + topN + " by prominence");
                break;
            }
        }
        if (prominent.contains(t.rulerName)) {
            t.reportReasons.add("its ruler " + t.rulerName + " is top-" + topN);
        }
        if (t.agreement == Agreement.ACTIVITY_WITHOUT_FOLLOW_THROUGH
                || t.agreement == Agreement.RULER_AND_SIGNIFICATOR_DISAGREE
                || t.agreement == Agreement.QUIET_COMPETENCE) {
            t.reportReasons.add("the witnesses disagree: " + t.agreement);
        }
        if (t.angular) {
            t.reportReasons.add("angular");
        }
        if (t.occupants.size() >= stelliumSize) {
            t.reportReasons.add("stellium of " + t.occupants.size());
        }
        if (!t.lots.isEmpty()) {
            t.reportReasons.add("holds the Lot of " + String.join(" and ", t.lots));
        }
        t.worthReporting = !t.reportReasons.isEmpty();
    }

    /**
     * One line of prose per topic, generated from the agreement pattern.
     *
     * Never from {@link Topic#strength} - that is the whole point of the layer. A house whose
     * witnesses all sit middling and agree gets the short sentence the spec asks for, which is
     * the reading correctly saying this area is unremarkable rather than a gap in it.
     */
    public static String describe(Topic t) {
        String h = "House " + t.house;
        switch (t.agreement) {
            case ALL_STRONG:
                return h + " is well supported on every count - say so briefly and move on.";
            case ALL_WEAK:
                return h + " is genuinely difficult: its occupants, its ruler "
                    + t.rulerName + " and its natural significator are all poorly placed.";
            case ALL_MIDDLING:
                return h + " is integrated ground: none of its three witnesses is notably "
                    + "strong or weak, so nothing here is asking to be defended or repaired.";
            case ACTIVITY_WITHOUT_FOLLOW_THROUGH:
                return h + " is busy but its ruler " + t.rulerName
                    + " is weak - much happens here and little of it consolidates.";
            case QUIET_COMPETENCE:
                return h + " is empty, but its ruler " + t.rulerName
                    + " is well placed: quiet competence rather than drama.";
            case RULER_AND_SIGNIFICATOR_DISAGREE:
                return h + "'s ruler " + t.rulerName + " and its natural significator "
                    + "disagree - this person's version of the topic is not the usual one.";
            case MAJORITY_WITH_DISSENT:
                return h + ": two witnesses agree, one dissents.";
            default:
                return h + " gives a mixed account.";
        }
    }

    /**
     * The derived house: the nth house counted from house {@code from}.
     *
     * The 10th from the 7th is the partner's career. Cheap, and the natural way to answer
     * "what about my partner's job" - but <b>only surface it on request</b>. Unprompted
     * derived-house statements read as noise, which is why nothing in {@link #analyse} calls
     * this.
     */
    public static int derived(int nth, int from) {
        return Zodiac.derivedHouse(nth, from);
    }

    private static int houseOfLon(ChartFrame f, double lon, boolean whole, int ascSign) {
        return whole
            ? Math.floorMod(Zodiac.signIndex(lon) - ascSign, 12) + 1
            : Zodiac.houseOf(lon, f.cusps);
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
