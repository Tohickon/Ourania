package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The L3 vector: four independent axes per body, never summed into a master number.
 *
 *   dignity     is it functioning in its own terms      -  Dignity
 *   condition   is it empowered to act                  -  PARTIAL, see below
 *   valence     does its action help or hurt            -  Sect
 *   prominence  how loud is it, regardless of quality   -  aspects and membership in
 *
 * Ranking is on PROMINENCE, not on dignity plus valence. That distinction is the whole
 * point of the vector: an angular out-of-sect malefic is the loudest thing in a chart
 * and scores badly on every other axis. Sorting by "strength" buries it.
 *
 * CONDITION now carries angularity, the solar conditions, retrogradation, stationarity,
 * swift/slow, the planetary joys and the solar phase. Only besiegement is still missing,
 * and unlike the rest it needs an input nothing computes yet: the geometry to both
 * malefics at once.
 *
 * Note condition feeds NO term of prominence. That is deliberate - loudness and
 * capability are different questions - but it means any sensitivity sweep that ranks on
 * prominence alone will score every condition weight at zero and call it dead. See
 * Calibration, which measures both axes for exactly that reason.
 */
public final class BodyScore {

    /**
     * Prominence weights. Every one of these is a placeholder chosen by eye, not
     * calibrated against anything. They are mutable so Calibration can perturb them and
     * measure which ones the output is actually sensitive to.
     */
    /**
     * Angularity: a wider orb at a lower weight than the original 10 degrees at 1.0.
     *
     * The old setting made angularity worth three times the largest standing term
     * (chart ruler, 0.35) and gave it a hard edge: full value at the angle, exactly zero
     * at 10 degrees, linear in between. Since the chart rotates about 15 degrees an hour,
     * a body crossed that whole window in forty minutes, and the lead body of a "now"
     * chart changed roughly every quarter hour as a result.
     *
     * Measured over two full days at 10-minute steps, two locations, counting how often
     * the top-ranked body changes against how often a body within 2 degrees of an angle
     * still reaches the top three:
     *
     *   orb 10, w 1.0 (original)   92 changes   95% signal
     *   orb 15, w 0.7 (this)       65 changes   84% signal
     *   orb 20, w 0.5              54 changes   60% signal
     *   w 0.0 (angularity off)     51 changes   21% signal
     *
     * The floor of 51 is the churn from everything else, so this removes about two
     * thirds of the angularity-driven instability for a tenth of the signal. Going wider
     * buys little more and starts costing signal fast.
     */
    public static double angularityOrb = 15.0;
    public static double weightAscMc = 0.7;
    public static double weightDscIc = 0.56;

    /**
     * Raised-cosine falloff instead of the straight line. Off, and measured, not assumed:
     * at identical orb and weight it scored 71 changes / 85% against the line's 65 / 84%.
     * Flattening the top puts more bodies near maximum at once, which makes MORE ties to
     * flip between, not fewer. Left in place so the next person does not re-run the idea.
     */
    public static boolean cosineFalloff = false;
    public static double weightChartRuler = 0.35;
    public static double weightSectLight = 0.25;
    public static double weightOutOfSectMalefic = 0.30;
    /** Near a station: apparent motion has fallen to a fraction of the body's median. */
    public static double weightStationary = 0.25;

    /** Per tight aspect to a luminary, scaled by how close to exact it is. */
    public static double weightAspectToLight = 0.30;
    /**
     * Per soft aspect to an angle. Only sextiles and trines count here: the four angles
     * are two axes 90 degrees apart, so a conjunction, square or opposition to any angle
     * is a conjunction to one of the four, which measureAngularity has already scored.
     * Counting those again would be paying twice for the same fact.
     */
    public static double weightAspectToAngle = 0.15;
    /** Breadth of connection, saturating - the sixth aspect says less than the second. */
    public static double weightAspectBreadth = 0.20;
    /** A body answering to nothing is conspicuous, not negligible. */
    public static double weightUnaspected = 0.25;

    /**
     * L4 rulership terms.
     *
     * The 1st is excluded from the angular term on purpose: its ruler is the chart ruler,
     * which weightChartRuler already scores, and crediting both would pay twice for one
     * fact. So this covers the 4th, 7th and 10th only.
     */
    public static double weightRulesAngularHouse = 0.20;
    /** Breadth of rulership, saturating - ruling five houses is not five times ruling one. */
    public static double weightHousesRuled = 0.15;
    /**
     * Reception by domicile or exaltation only. A body in its dispositor's own sign is
     * held up by it; bound and face receptions are near-universal and mean little, so
     * counting them would add noise at the strength of a real rescue.
     *
     * Scored on CONDITION, not prominence: reception does not make a body louder, it
     * makes it better supported.
     */
    public static double weightReception = 0.15;

    /**
     * L5: membership terms. A body's prominence is not only its own condition - being
     * part of a concentration, or being where every chain of rulership ends, makes it
     * louder than its individual facts suggest.
     *
     * The sole dispositor gets the largest of these because it is the strongest single
     * structural claim the engine can make about a chart: every other planet's chain of
     * rulership terminates there. Plain final dispositors are common by comparison.
     */
    public static double weightStellium = 0.20;
    public static double weightSoleDispositor = 0.35;
    public static double weightFinalDispositor = 0.15;
    public static double weightDispositorLoop = 0.10;

    /**
     * Condition weights: accidental dignity, how empowered a body is to act.
     *
     * Unlike prominence, condition starts from a NEUTRAL baseline and moves both ways.
     * Prominence is a sum from zero because a quiet planet really does contribute
     * nothing to loudness. Condition is not like that: a planet nowhere near an angle is
     * not thereby crippled, it is simply unremarkable. Scoring it from zero, as the
     * angularity-only version did, meant most of a chart read as maximally disabled.
     */
    public static double conditionBaseline = 0.5;
    public static double weightConditionAngular = 0.30;
    /** Cazimi is a dignity - the heart of the Sun protects rather than burns. */
    public static double weightCazimi = 0.25;
    public static double weightCombust = -0.30;
    public static double weightUnderBeams = -0.10;
    public static double weightBesieged = -0.30;
    public static double weightContainedByBenefics = 0.30;
    public static double weightConditionRetrograde = -0.15;
    /** A station is a stronger statement than swift or slow, and stops the body. */
    public static double weightConditionStationary = -0.05;
    public static double weightSwift = 0.10;
    public static double weightSlow = -0.10;
    /**
     * The planetary joy. See JOY_HOUSE for the table and for why it is read by whole sign.
     *
     * Sized against reception rather than against angularity. Both are the same kind of
     * claim - a body is helped by where it finds itself, not made louder by it - and a
     * one-way reception scores weightReception * 0.6 = 0.09. A joy is a slightly stronger
     * statement than that, being unconditional rather than dependent on another body's
     * position, so it sits just above it.
     *
     * There is no matching penalty for the opposite house, because the tradition does not
     * have one: joys are seven positive assignments, not a signed scale with a bad end.
     * Inventing a "sorrow" to balance the books would be a term this project made up.
     */
    public static double weightJoy = 0.12;

    /**
     * The solar phase, oriental or occidental, as accidental dignity.
     *
     * TWO-SIDED, unlike the joy, and for the same reason the joy is one-sided: the source
     * is. Lilly's accidental table carries both a dignity row and a debility row for this
     * - "Saturn, Jupiter or Mars orientall +2 / occidentall -2", "Venus or Mercury
     * occidentall +2 / orientall -2" - where the joys appear only as seven positive
     * assignments. Symmetry here and asymmetry there is not an inconsistency in this
     * class; it is the tradition being copied accurately in both places.
     *
     * SIZED FROM LILLY'S OWN SCALE rather than by eye. He gives this +/-2 against -5 for
     * combustion, and this engine has already priced combustion at -0.30, so 2/5 of that
     * is 0.12. That is the whole derivation - it inherits whatever judgement went into
     * weightCombust rather than adding a fresh guess on top of it.
     *
     * READ THIS BEFORE TRUSTING THE NUMBER. At 0.12 this is the single most influential
     * term on the whole condition axis. Turning each condition term off in turn and
     * counting L6 witness standings that move, over 324 charts, measured after the
     * visibility gate landed:
     *
     *   phase 1497 · reception 1298 · retrograde 887 · angularity 625 · slow 511
     *   combust 354 · swift 322 · joy 156 · under beams 140 · stationary 19 · cazimi 3
     *
     * It outranks angularity, which carries 2.5x the weight. The cause is structural
     * rather than a mistake: this is the only term that is ALWAYS ON, ALWAYS SIGNED, and
     * applies to five of the seven traditional bodies in every chart, so its 0.24 swing is
     * collected every time. Angularity's 0.30 is larger but is near zero for most bodies
     * most of the time, and combustion's 0.30 is rare.
     *
     * THESE ARE MARGINAL EFFECTS AGAINST A SHARED BASELINE, NOT A DECOMPOSITION. They do
     * not sum to anything, and the same table from a different build is not comparable
     * row by row: the pre-gate run put phase at 2027 and reception at 1514, and reception's
     * code had not changed at all - the gate moved the baseline, which moved which topics
     * sit near a standing boundary, which moved every row. Trust the ordering, which was
     * identical in both runs. If this is re-run, re-run the whole table.
     *
     * The sweep offers no help in choosing: standings move 721 / 1579 / 1835 / 2027 /
     * 2153 / 2229 / 2311 across 0.04 to 0.30, smooth and monotone with no plateau and no
     * cliff. So unlike weightJoy, THIS VALUE RESTS ENTIRELY ON THE SOURCE, and the
     * measurement neither confirms nor contradicts it. Lowering it to about 0.04 would put
     * it beside angularity if the always-on character is judged to warrant a discount;
     * that is a modelling decision, and it has not been taken.
     */
    public static double weightPhaseFavourable = 0.12;
    public static double weightPhaseContrary = -0.12;

    /**
     * Which solar phase each planet rejoices in.
     *
     * The superior planets - Saturn, Jupiter, Mars - want to be ORIENTAL, emerging from
     * the Sun's rays into the morning sky. The inferior ones - Venus, Mercury - want to be
     * OCCIDENTAL, the evening star. It reads as arbitrary until you notice both halves say
     * the same thing: the planet is favoured in the part of its own synodic cycle where it
     * is pulling clear of the Sun and gaining visibility, and that part falls on opposite
     * sides for planets inside and outside the Earth's orbit.
     *
     * Getting this backwards for one group is the obvious failure, and it would not look
     * like a bug - it would produce fluent, confident, wrong readings for two planets out
     * of five. PhaseCheck asserts the split rather than trusting the table was typed right.
     */
    private static Sect.Phase favouredPhase(String body) {
        switch (body) {
            case "Saturn":
            case "Jupiter":
            case "Mars":
                return Sect.Phase.ORIENTAL;
            case "Venus":
            case "Mercury":
                return Sect.Phase.OCCIDENTAL;
            default:
                return Sect.Phase.NOT_APPLICABLE;
        }
    }

    /** Exposed so a check can re-derive the split instead of reading it back out. */
    public static boolean rejoicesOriental(String body) {
        return favouredPhase(body) == Sect.Phase.ORIENTAL;
    }

    /**
     * The seven planetary joys, indexed by house 1..12, or null where no body rejoices.
     *
     * Mercury 1st, Moon 3rd, Venus 5th, Mars 6th, Sun 9th, Jupiter 11th, Saturn 12th.
     *
     * READ BY WHOLE SIGN, deliberately, and this is the first term in the class where the
     * two house forms give different answers often enough to matter. The joys are a
     * Hellenistic doctrine and their internal logic is whole-sign: the scheme is symmetric
     * about the horizon axis, with the benefics and luminaries taking the houses that
     * aspect the Ascendant and the malefics the ones that do not. Read by quadrant, a body
     * a degree the far side of a cusp changes joy while its relationship to the Ascendant
     * sign has not changed at all, which is the arrangement the doctrine is about.
     *
     * Vector carries both forms precisely so this layer can make that choice explicitly
     * rather than inheriting whichever one L0 happens to call canonical.
     */
    private static final String[] JOY_HOUSE = {
        null,       // index 0 unused - houses are 1-based
        "Mercury",  // 1st
        null,       // 2nd
        "Moon",     // 3rd
        null,       // 4th
        "Venus",    // 5th
        "Mars",     // 6th
        null,       // 7th
        null,       // 8th
        "Sun",      // 9th
        null,       // 10th
        "Jupiter",  // 11th
        "Saturn"    // 12th
    };

    /** The house this body rejoices in, or 0 for the bodies that have no joy. */
    public static int joyHouse(String body) {
        for (int h = 1; h <= 12; h++) {
            if (JOY_HOUSE[h] != null && JOY_HOUSE[h].equals(body)) {
                return h;
            }
        }
        return 0;
    }

    /** The body that rejoices in this house, or null. Exposed so a check can re-derive it. */
    public static String joyOfHouse(int house) {
        return house >= 1 && house <= 12 ? JOY_HOUSE[house] : null;
    }

    /*
     * underBeamsOrb used to live here, at 15.0, with a comment explaining that the orb was
     * kept out of Aspects.Solar because adding a third constant to that enum would make
     * SkymapPanel's two-way ternary label the new value "Combust".
     *
     * The constant has since moved to Aspects.UNDER_BEAMS_ORB and the enum did gain
     * UNDER_BEAMS - and the predicted mislabel duly happened, on the wheel, for months of
     * charts. The field was left behind: still public, still reset by resetWeights, still
     * read by nothing.
     *
     * That is worse than untidy, because Calibration finds tunables by reflection over
     * every public static double here. It swept this one on every run and reported 0%
     * churn - which reads as "this weight does not matter" when it actually meant "this
     * weight is not connected to anything". A dead knob does not go quiet; it reports a
     * confident zero.
     */

    /**
     * Slow and swift thresholds in degrees per day, as {p25, p75} of each body's own
     * DIRECT motion. Measured from this project's own ephemeris: 1900-2100 at 2-day
     * steps, retrograde samples excluded.
     *
     * The first version compared speed to net orbital mean and it did not work, in two
     * opposite ways at once. Net mean includes the retrograde arcs, so an outer planet
     * moving forward is always far above it - Pluto scored "swift" in 94% of its direct
     * samples, Neptune 89%, Uranus 85%. The term was not describing speed, it was
     * restating "direct", which the retrograde term already says. Meanwhile the Sun and
     * Moon never retrograde, so their net mean IS their typical speed and a plus or minus
     * 20% band sat outside their real range: the Moon runs 12.15 to 14.10 against a mean
     * of 13.18, so it registered as neither swift nor slow in 100% of samples - the one
     * body whose speed the tradition cares about most.
     *
     * Percentiles of the body's own direct motion fix both ends, and fire about a quarter
     * of the time each way by construction.
     */
    /**
     * Median direct motion, from the same measurement as speedBand. The stationarity
     * test scales against this rather than the p25/p75 edges, which are by construction
     * a quarter of the distribution away from typical.
     */
    private static double medianDirectMotion(String body) {
        switch (body) {
            case "Sun":        return 0.98487;
            case "Moon":       return 13.05884;
            case "Mercury":    return 1.50997;
            case "Venus":      return 1.20524;
            case "Mars":       return 0.64335;
            case "Jupiter":    return 0.17564;
            case "Saturn":     return 0.09087;
            case "Uranus":     return 0.04255;
            case "Neptune":    return 0.02662;
            case "Pluto":      return 0.02034;
            case "North Node": return 0.01160;
            case "Chiron":     return 0.05452;
            default:           return 0.0;
        }
    }

    private static double[] speedBand(String body) {
        switch (body) {
            case "Sun":        return new double[]{0.9623, 1.0089};
            case "Moon":       return new double[]{12.1542, 14.0998};
            case "Mercury":    return new double[]{1.0571, 1.7264};
            case "Venus":      return new double[]{1.0790, 1.2341};
            case "Mars":       return new double[]{0.5604, 0.7148};
            case "Jupiter":    return new double[]{0.1035, 0.2142};
            case "Saturn":     return new double[]{0.0507, 0.1153};
            case "Uranus":     return new double[]{0.0231, 0.0551};
            case "Neptune":    return new double[]{0.0144, 0.0347};
            case "Pluto":      return new double[]{0.0112, 0.0263};
            case "North Node": return new double[]{0.0048, 0.0195};
            case "Chiron":     return new double[]{0.0309, 0.0733};
            default:           return null;
        }
    }

    /** Restores every weight to its shipped default. Used between calibration runs. */
    public static void resetWeights() {
        angularityOrb = 15.0;
        weightAscMc = 0.7;
        weightDscIc = 0.56;
        cosineFalloff = false;
        weightChartRuler = 0.35;
        weightSectLight = 0.25;
        weightOutOfSectMalefic = 0.30;
        // weightStationary was missing here from the day it was added, and nothing
        // noticed until Calibration started perturbing it: the sweep restores state by
        // calling this, so a weight it does not reset stays perturbed for every row that
        // follows. Calibration no longer relies on this being complete, but callers do.
        weightStationary = 0.25;
        weightAspectToLight = 0.30;
        weightAspectToAngle = 0.15;
        weightAspectBreadth = 0.20;
        weightUnaspected = 0.25;
        conditionBaseline = 0.5;
        weightConditionAngular = 0.30;
        weightCazimi = 0.25;
        weightCombust = -0.30;
        weightUnderBeams = -0.10;
        weightBesieged = -0.30;
        weightContainedByBenefics = 0.30;
        weightConditionRetrograde = -0.15;
        weightConditionStationary = -0.05;
        stationaryFraction = 0.10;
        weightSwift = 0.10;
        weightSlow = -0.10;
        weightJoy = 0.12;
        weightPhaseFavourable = 0.12;
        weightPhaseContrary = -0.12;
        weightRulesAngularHouse = 0.20;
        weightHousesRuled = 0.15;
        weightReception = 0.15;
        weightStellium = 0.20;
        weightSoleDispositor = 0.35;
        weightFinalDispositor = 0.15;
        weightDispositorLoop = 0.10;
    }

    /**
     * A body is stationary when its apparent motion falls to this fraction of its own
     * MEDIAN DIRECT speed.
     *
     * The note that used to sit here said stationarity could not be done without sampling
     * the ephemeris either side of the moment, on the grounds that an absolute threshold
     * cannot span 13 deg/day for the Moon down to 0.004 for Pluto, and that scaling by
     * mean motion does not fix it. Both halves were right about the yardstick available
     * at the time: net orbital mean includes the retrograde arcs, so Pluto's is 0.004
     * against a direct median of 0.020 - a fraction of it is far too tight to ever fire.
     *
     * The swift/slow work replaced that yardstick with each body's measured direct-motion
     * distribution, and against the MEDIAN the same simple test behaves correctly at both
     * ends: the Moon's speed never falls below 11.8 so it can never be stationary, while
     * Pluto's threshold lands at 0.002 where it genuinely does station. No extra ephemeris
     * calls are needed, because a body's speed is near zero exactly when it is near a
     * station, and taking the magnitude handles the swing through either side.
     *
     * What this does NOT give is time-to-station, which would distinguish a body one day
     * off its station from one a week off. That still wants the L1 sampling the old note
     * described; the fraction is a proxy for it, not a substitute.
     */
    public static double stationaryFraction = 0.10;

    public static final class Vector {
        public String body;
        public double longitude;

        public Dignity.Result dignity;
        public Sect.Valence valence;

        public double angularity;        // 0..1
        public String nearestAngle;
        public double degreesToAngle;

        /**
         * 0..1, neutral at conditionBaseline. Still named "partial": it now carries
         * angularity, the solar conditions, retrogradation, speed, the planetary joy and
         * the solar phase. Besiegement is the last term outstanding.
         */
        public double conditionPartial;
        /** Signed daily motion in longitude, kept for the swift/slow term. */
        public double speed;
        /**
         * Cazimi or combust, or null. Typed rather than left for a caller to recognise
         * in conditionReasons - phrasing code that greps its own prose for facts breaks
         * the moment the prose is reworded.
         */
        public Aspects.Solar solar;
        /** Inside Aspects.UNDER_BEAMS_ORB but outside the combustion orb. */
        public boolean underBeams;
        public final List<String> conditionReasons = new ArrayList<>();
        public double prominenceRaw;
        public double prominence;        // normalised per chart, 0..1

        public boolean isChartRuler;
        public boolean isSectLight;
        public boolean isOutOfSectMalefic;
        public boolean isStationary;
        public boolean isRetrograde;
        public boolean isBesieged;
        public boolean isContainedByBenefics;
        /** In the whole-sign house this body rejoices in. Only seven bodies can be. */
        public boolean inJoy;
        /**
         * Which side of the Sun this body is on. NOT_APPLICABLE for the Sun, the Moon and
         * the modern bodies - never null, so no caller has to guess what absence means.
         */
        public Sect.Phase phase = Sect.Phase.NOT_APPLICABLE;
        /** Every aspect this body makes to another body, tightest first. */
        public final List<Aspects.Hit> aspects = new ArrayList<>();
        public int aspectCount;
        /** No Ptolemaic aspect to any of the traditional seven. */
        public boolean isUnaspected;
        /** Aspects to the four angles, kept separate from body-to-body. */
        public final List<Aspects.Hit> angleAspects = new ArrayList<>();

        // L2 placement. Both forms, because L0 owns which is canonical and L3's joys and
        // L6's topics do not always want the same one.
        /** Quadrant house 1..12, or 0 if the cusps were degenerate. */
        public int house;
        public int wholeSignHouse;

        // L4 rulership.
        /** Houses whose cusp sign this body rules, ascending. Often empty, sometimes 2+. */
        public final List<Integer> housesRuled = new ArrayList<>();
        /** Rules the 4th, 7th or 10th. The 1st is excluded - that is the chart ruler. */
        public boolean rulesAngularHouse;
        /**
         * Strongest reception in which this body is SUPPORTED - mutual, or sitting in
         * another's dignity. Receptions where this body is merely the host are not
         * recorded: they say something about the guest, not about this one.
         */
        public Rulership.Reception reception;

        // L5 membership.
        /** The sign of the stellium this body belongs to, or null. */
        public String stelliumSign;
        /** In its own sign, so a chain of rulership stops here. */
        public boolean isFinalDispositor;
        /** Every chain in the chart terminates at this body. Implies isFinalDispositor. */
        public boolean isSoleDispositor;
        /** Part of a closed rulership circuit that no final dispositor resolves. */
        public boolean inDispositorLoop;

        public final List<String> prominenceReasons = new ArrayList<>();

        /**
         * Everything the reading needs to justify itself, in one list, deduplicated.
         * The axes overlap by design - sect light is both a valence fact and a
         * prominence term - so the same sentence can arrive from two directions and
         * must not be said twice.
         */
        public List<String> allReasons() {
            List<String> out = new ArrayList<>();
            if (dignity != null) {
                out.addAll(dignity.reasons);
            }
            if (valence != null) {
                out.addAll(valence.reasons);
            }
            out.addAll(conditionReasons);
            out.addAll(prominenceReasons);
            List<String> deduped = new ArrayList<>();
            for (String s : out) {
                if (!deduped.contains(s)) {
                    deduped.add(s);
                }
            }
            return deduped;
        }
    }

    private BodyScore() { }

    /**
     * The weight-independent half of the vector, computed once per chart.
     *
     * Dignity and valence are pure functions of position and sect - no weight touches
     * them. Only angularity and the three prominence bonuses vary with the weights. So a
     * grid search should compute this once and re-run only the cheap half, which is the
     * difference between a fitting run taking minutes and taking seconds.
     */
    public static final class Precomputed {
        final ChartFrame frame;
        /** Fully built, with every weight-independent field already populated. */
        final List<Vector> vectors = new ArrayList<>();

        Precomputed(ChartFrame f) {
            this.frame = f;
        }
    }

    /**
     * Builds the chart's Vectors once, with every weight-INDEPENDENT field populated:
     * dignity, valence, aspects, houses ruled, stellium and dispositor membership. Only
     * angularity and the prominence total depend on the weights, and rankNames redoes
     * exactly those.
     *
     * This used to hold loose parallel lists and rankNames reimplemented the prominence
     * sum over them. That is a second copy of a formula, and it drifted: twelve terms
     * were added to computeProminence and none of them to the copy, so the grid search
     * silently optimised a four-term model of a sixteen-term engine. verifyFastPath
     * caught it. The fix is not to re-sync the copy but to delete it.
     */
    public static Precomputed precompute(ChartFrame f) {
        Precomputed p = new Precomputed(f);
        p.vectors.addAll(rank(f, Gestalt.compute(f)));
        return p;
    }

    /**
     * Batch fast path: body names ordered by prominence, no Vector or reason objects.
     * Allocates one array. Used by the grid search, where only the ordering matters.
     */
    public static String[] rankNames(Precomputed p) {
        List<Vector> vs = p.vectors;
        int n = vs.size();
        Integer[] idx = new Integer[n];
        double[] score = new double[n];

        for (int i = 0; i < n; i++) {
            Vector v = vs.get(i);
            idx[i] = i;
            // Angularity and the prominence total are the only weight-dependent parts,
            // so they are the only parts redone. Everything else was settled in
            // precompute and does not change as the grid moves.
            v.prominenceReasons.clear();
            measureAngularity(v, p.frame);
            v.conditionPartial = v.angularity;
            score[i] = computeProminence(v);
        }
        java.util.Arrays.sort(idx, (a, b) -> Double.compare(score[b], score[a]));
        String[] out = new String[n];
        for (int i = 0; i < n; i++) {
            out[i] = score[idx[i]] > 0.0 ? vs.get(idx[i]).body : null;
        }
        return out;
    }

    /**
     * How much of an angle's weight a body at this separation collects, 0..1.
     *
     * One definition, used by both angularityOf and measureAngularity. It was written
     * twice, identically, which is how two copies start to drift.
     *
     * Raised cosine rather than the original straight line. The line fell at a constant
     * rate and hit exactly zero at the orb, so a body crossed the whole window in the
     * forty minutes it takes the chart to rotate ten degrees, and its score marched
     * across that range at a constant clip. The cosine is flat at both ends: a body very
     * near an angle stays near-maximal for a while, and one near the edge of orb changes
     * slowly instead of sliding straight through. The midpoint is identical at 0.5, so
     * this is a change of slope, not of how generous the orb is.
     */
    public static double angularFalloff(double separation) {
        if (separation >= angularityOrb) {
            return 0.0;
        }
        if (!cosineFalloff) {
            return Math.max(0.0, 1.0 - separation / angularityOrb);
        }
        return 0.5 * (1.0 + Math.cos(Math.PI * separation / angularityOrb));
    }

    /** Shared angularity computation, weight-dependent. */
    private static double angularityOf(double lon, ChartFrame f) {
        double[] lons = {f.asc, f.mc, f.dsc, f.ic};
        double[] weights = {weightAscMc, weightAscMc, weightDscIc, weightDscIc};
        double best = 0.0;
        for (int i = 0; i < 4; i++) {
            double sep = ChartFrame.separation(lon, lons[i]);
            double strength = angularFalloff(sep) * weights[i];
            if (strength > best) {
                best = strength;
            }
        }
        return best;
    }

    /**
     * Scores every body in the frame and returns them ranked by prominence, loudest first.
     *
     * Computes the chart's gestalt to get the L5 membership terms. Callers that already
     * hold one should pass it to the overload rather than pay for a second - though at
     * well under a millisecond that is tidiness, not performance.
     */
    public static List<Vector> rank(ChartFrame f) {
        return rank(f, Gestalt.compute(f));
    }

    /**
     * Ranking with a gestalt already in hand.
     *
     * Gestalt does not depend on BodyScore, so taking it as a parameter here is safe -
     * the dependency runs one way and stays that way.
     */
    public static List<Vector> rank(ChartFrame f, Gestalt.Result g) {
        String chartRuler = Dignity.domicileRulerOf(Zodiac.signIndex(f.asc));
        String sectLight = Sect.sectLight(f.diurnal);
        String oosMalefic = Sect.outOfSectMalefic(f.diurnal);
        double sunLon = f.body("Sun").lon;

        // L4. Computed once for the whole chart, then split per body - the pairs are
        // symmetric, so building them per body would do the work twice and risk the two
        // halves disagreeing about an aspect at the edge of orb.
        List<Aspects.Hit> bodyAspects = Aspects.betweenBodies(f);
        List<Aspects.Hit> angleAspects = Aspects.toAngles(f);

        // L4. Twelve lookups and a pass over 21 pairs - cheap enough to do unconditionally.
        List<Rulership.Edge> web = Rulership.web(f);
        List<Rulership.Reception> receptions = Rulership.receptions(f);

        List<Vector> out = new ArrayList<>();
        for (int bi = 0; bi < f.bodies.length && bi < Bodies.count(); bi++) {
            ChartFrame.Body b = f.bodies[bi];
            if (b == null || !b.ok) {
                continue;
            }
            // The four angles are the frame bodies are measured against, not competitors
            // in the ranking. They arrived here by accident on 2026-08-19, when ChartFrame
            // moved onto the Bodies registry and `bodies` went from 12 planets to all 28
            // points; nothing here was changed to match.
            //
            // Scoring them is not merely noisy, it is circular. Prominence is mostly
            // angularity, and an angle is by definition 0 degrees from itself: the MC came
            // out "70% angular, 0.0 deg from MC" and ranked 7th, above Venus and the Sun,
            // for the achievement of being where it is. Its "17 soft aspects to an angle"
            // counted itself too. The other prominence terms - chart ruler, sect light,
            // out-of-sect malefic - cannot apply to an angle at all, so there is no reading
            // of this scale on which the four of them are being measured rather than
            // flattered.
            //
            // Transits and directions to the angles are unaffected: Transits.
            // significantTargets adds all four explicitly from natal.asc/mc/dsc/ic with
            // reason "angle", and never took them from this list.
            if (Bodies.at(bi).isAngle()) {
                continue;
            }
            Vector v = new Vector();
            v.body = b.name;
            v.longitude = b.lon;
            v.isRetrograde = b.retrograde;
            double medianDirect = medianDirectMotion(b.name);
            v.isStationary = medianDirect > 0.0
                && Math.abs(b.lonSpeed) <= stationaryFraction * medianDirect;

            v.dignity = Dignity.evaluate(b.name, b.lon, f.diurnal);
            v.valence = Sect.evaluate(b.name, f.diurnal, b.lon, sunLon);

            measureAngularity(v, f);
            v.speed = b.lonSpeed;

            v.isChartRuler = b.name.equals(chartRuler);
            v.isSectLight = b.name.equals(sectLight);
            v.isOutOfSectMalefic = b.name.equals(oosMalefic);

            v.house = Zodiac.houseOf(b.lon, f.cusps);
            v.wholeSignHouse = Zodiac.wholeSignHouse(b.lon, f.asc);

            collectAspects(v, b.name, bodyAspects, angleAspects);
            collectMembership(v, g);
            collectRulership(v, web, receptions);

            // AFTER collectRulership, not before. computeCondition reads v.reception, and
            // when this ran earlier that field was still null - the reception term never
            // fired once, silently, while the same condition phrased fine in the prose
            // because Snapshot reads the field later. Anything computeCondition consumes
            // has to be populated above this line.
            computeCondition(v, f);

            v.prominenceRaw = computeProminence(v);
            out.add(v);
        }

        // Normalise per chart. Every chart has a loudest body; the question is relative.
        double max = out.stream().mapToDouble(x -> x.prominenceRaw).max().orElse(1.0);
        if (max <= 0.0) {
            max = 1.0;
        }
        for (Vector v : out) {
            v.prominence = v.prominenceRaw / max;
        }

        out.sort(Comparator.comparingDouble((Vector v) -> v.prominence).reversed());
        return out;
    }

    /** Distance to the nearest of the four angles, decayed to a 0..1 strength. */
    private static void measureAngularity(Vector v, ChartFrame f) {
        String[] names = {"Ascendant", "MC", "Descendant", "IC"};
        double[] lons = {f.asc, f.mc, f.dsc, f.ic};
        double[] weights = {weightAscMc, weightAscMc, weightDscIc, weightDscIc};

        double best = 0.0;
        for (int i = 0; i < 4; i++) {
            double sep = ChartFrame.separation(v.longitude, lons[i]);
            double strength = angularFalloff(sep) * weights[i];
            if (strength > best || v.nearestAngle == null) {
                if (strength > best) {
                    best = strength;
                }
                if (v.nearestAngle == null || sep < v.degreesToAngle) {
                    v.nearestAngle = names[i];
                    v.degreesToAngle = sep;
                }
            }
        }
        v.angularity = best;
    }

    /**
     * Condition: how empowered a body is to act, as distinct from whether it acts well
     * (valence) or in its own terms (dignity).
     *
     * Moves both directions from a neutral baseline. The previous version assigned
     * angularity straight across, so every body away from an angle scored 0 - reported
     * in the same column as a genuinely afflicted planet, and indistinguishable from it.
     *
     * The solar conditions are the substantial addition. Combustion is one of the
     * heaviest accidental debilities in the tradition and Aspects.solarCondition had been
     * computing it since the L4 port with nothing in this package reading it.
     */
    private static void computeCondition(Vector v, ChartFrame f) {
        ChartFrame.Body sun = f.body("Sun");
        ChartFrame.Body b = f.body(v.body);
        double c = conditionBaseline;

        if (v.angularity > 0.0) {
            c += weightConditionAngular * v.angularity;
            v.conditionReasons.add(String.format("angular (%.2f)", v.angularity));
        }

        v.solar = Aspects.solarCondition(v.body, b.lon, b.lat, sun.lon, sun.lat);
        if (v.solar == Aspects.Solar.CAZIMI) {
            c += weightCazimi;
            v.conditionReasons.add("cazimi - in the heart of the Sun");
        } else if (v.solar == Aspects.Solar.COMBUST) {
            c += weightCombust;
            v.conditionReasons.add("combust");
        } else if (v.solar == Aspects.Solar.UNDER_BEAMS) {
            v.underBeams = true;
            c += weightUnderBeams;
            v.conditionReasons.add("under the beams");
        }

        // The solar phase. Sect measures which side of the Sun the body is on.
        v.phase = Sect.phaseOf(v.body, v.longitude, sun.lon);
        if (v.phase != Sect.Phase.NOT_APPLICABLE) {
            // Visibility Paradox Gate: if it's Combust or Under the Beams, it's invisible.
            if (v.solar != null && v.solar != Aspects.Solar.CAZIMI) {
                v.conditionReasons.add("invisible - phase score suppressed");
            } else {
                boolean favoured = v.phase == favouredPhase(v.body);
                String name = v.phase == Sect.Phase.ORIENTAL ? "oriental" : "occidental";
                c += favoured ? weightPhaseFavourable : weightPhaseContrary;
                v.conditionReasons.add(favoured
                    ? name + " - the phase it rejoices in"
                    : name + " - contrary to the phase it rejoices in");
            }
        }

        // Retrogradation is a condition term, not a dignity one: the body still functions
        // in its own terms, it is just not free to press outward.
        if (v.isRetrograde) {
            c += weightConditionRetrograde;
            v.conditionReasons.add("retrograde");
        }

        // A station is its own statement, and the strongest of the speed terms: a body
        // there has effectively stopped. It also has to pre-empt the swift/slow test,
        // which would otherwise label it "slow" - true in the arithmetic and misleading
        // as a reading, since slow means dragging and stationary means turning.
        if (v.isStationary) {
            c += weightConditionStationary;
            v.conditionReasons.add(String.format("stationary (%.4f°/day)", v.speed));
        }

        // Swift or slow against the body's own median direct motion. Skipped while
        // retrograde, where the sign of the motion already carries the statement and
        // "slow" would double-count what "retrograde" just said.
        double[] band = speedBand(v.body);
        if (band != null && !v.isRetrograde && !v.isStationary) {
            if (v.speed >= band[1]) {
                c += weightSwift;
                v.conditionReasons.add(String.format("swift (%.4f°/day)", v.speed));
            } else if (v.speed <= band[0]) {
                c += weightSlow;
                v.conditionReasons.add(String.format("slow (%.4f°/day)", v.speed));
            }
        }

        // The planetary joy, by whole sign - see JOY_HOUSE for why that form and not the
        // quadrant one.
        //
        // This does NOT double-count angularity, though Mercury's joy in the 1st looks
        // like it might. Angularity is measured as proximity to the Ascendant DEGREE and
        // decays to nothing at angularityOrb; occupying the 1st whole-sign house is a
        // different fact, and a body late in that sign can hold the joy while scoring zero
        // angularity. The reverse also happens, a body just above the Ascendant sitting in
        // the 12th sign. They agree often enough to look redundant and disagree often
        // enough to be two terms.
        v.inJoy = v.wholeSignHouse >= 1 && v.body.equals(JOY_HOUSE[v.wholeSignHouse]);
        if (v.inJoy) {
            c += weightJoy;
            v.conditionReasons.add("in its joy in the "
                + Zodiac.ordinal(v.wholeSignHouse) + " house");
        }

        // Reception belongs on condition, not prominence: a body in its dispositor's own
        // sign is not louder for it, it is better supported. Classically this is what
        // rescues an otherwise debilitated planet.
        if (v.reception != null && v.reception.strength >= Dignity.PTS_EXALTATION) {
            c += weightReception * (v.reception.mutual ? 1.0 : 0.6);
            v.conditionReasons.add(v.reception.mutual
                ? "in mutual reception by " + v.reception.dignity()
                : "received by " + v.reception.receiver() + "'s " + v.reception.dignity());
        }

        // Besiegement (between Mars and Saturn) and Containment (between Venus and Jupiter)
        // by bodily enclosure in longitude without intervening bodies.
        if (!"Mars".equals(v.body) && !"Saturn".equals(v.body)) {
            ChartFrame.Body mars = f.body("Mars");
            ChartFrame.Body saturn = f.body("Saturn");
            if (mars != null && mars.ok && saturn != null && saturn.ok) {
                double sepMalefics = ChartFrame.separation(mars.lon, saturn.lon);
                if (sepMalefics < 170.0) { // Avoid ambiguity at exact opposition
                    double sep1 = ChartFrame.separation(v.longitude, mars.lon);
                    double sep2 = ChartFrame.separation(v.longitude, saturn.lon);
                    // On the shorter arc between them?
                    if (Math.abs((sep1 + sep2) - sepMalefics) < 0.1) {
                        boolean beneficIntervenes = false;
                        String[] benefics = {"Venus", "Jupiter"};
                        for (String ben : benefics) {
                            ChartFrame.Body benefic = f.body(ben);
                            if (benefic != null && benefic.ok && !benefic.name.equals(v.body)) {
                                double bSep1 = ChartFrame.separation(benefic.lon, mars.lon);
                                double bSep2 = ChartFrame.separation(benefic.lon, saturn.lon);
                                if (Math.abs((bSep1 + bSep2) - sepMalefics) < 0.1) {
                                    beneficIntervenes = true;
                                    break;
                                }
                            }
                        }
                        if (!beneficIntervenes) {
                            v.isBesieged = true;
                            c += weightBesieged;
                            v.conditionReasons.add("besieged by Mars and Saturn");
                        }
                    }
                }
            }
        }

        if (!"Venus".equals(v.body) && !"Jupiter".equals(v.body)) {
            ChartFrame.Body venus = f.body("Venus");
            ChartFrame.Body jup = f.body("Jupiter");
            if (venus != null && venus.ok && jup != null && jup.ok) {
                double sepBenefics = ChartFrame.separation(venus.lon, jup.lon);
                if (sepBenefics < 170.0) {
                    double sep1 = ChartFrame.separation(v.longitude, venus.lon);
                    double sep2 = ChartFrame.separation(v.longitude, jup.lon);
                    if (Math.abs((sep1 + sep2) - sepBenefics) < 0.1) {
                        boolean maleficIntervenes = false;
                        String[] malefics = {"Mars", "Saturn"};
                        for (String m : malefics) {
                            ChartFrame.Body malefic = f.body(m);
                            if (malefic != null && malefic.ok && !malefic.name.equals(v.body)) {
                                double mSep1 = ChartFrame.separation(malefic.lon, venus.lon);
                                double mSep2 = ChartFrame.separation(malefic.lon, jup.lon);
                                if (Math.abs((mSep1 + mSep2) - sepBenefics) < 0.1) {
                                    maleficIntervenes = true;
                                    break;
                                }
                            }
                        }
                        if (!maleficIntervenes) {
                            v.isContainedByBenefics = true;
                            c += weightContainedByBenefics;
                            v.conditionReasons.add("contained by Venus and Jupiter");
                        }
                    }
                }
            }
        }

        v.conditionPartial = Math.max(0.0, Math.min(1.0, c));
    }

    /** Reads this body's L5 memberships off the chart-level gestalt. */
    private static void collectMembership(Vector v, Gestalt.Result g) {
        if (g == null) {
            return;
        }
        for (java.util.Map.Entry<String, List<String>> e : g.stelliumMembers.entrySet()) {
            if (e.getValue().contains(v.body)) {
                v.stelliumSign = e.getKey();
                break;
            }
        }
        v.isFinalDispositor = g.finalDispositors.contains(v.body);
        v.isSoleDispositor = v.body.equals(g.soleDispositor);
        for (List<String> loop : g.dispositorLoopMembers) {
            if (loop.contains(v.body)) {
                v.inDispositorLoop = true;
                break;
            }
        }
    }

    /**
     * Which houses this body rules, and the best reception it is party to.
     *
     * A body can rule two houses - Mercury holds Gemini and Virgo - and under quadrant
     * houses an intercepted sign can put the same sign on two cusps, so the count is not
     * capped at two.
     */
    private static void collectRulership(Vector v, List<Rulership.Edge> web,
                                         List<Rulership.Reception> receptions) {
        for (Rulership.Edge e : web) {
            if (v.body.equals(e.ruler)) {
                v.housesRuled.add(e.house);
                // 1st deliberately absent: that is weightChartRuler's territory.
                if (e.house == 4 || e.house == 7 || e.house == 10) {
                    v.rulesAngularHouse = true;
                }
            }
        }
        // The strongest reception in which this body is SUPPORTED - mutual, or the guest.
        //
        // Direction matters and getting it wrong produces nonsense: taking the strongest
        // reception the body merely takes part in meant a host was credited as though it
        // were the guest, and the reason text named the body as its own receiver -
        // "Jupiter received by Jupiter's exaltation". Hosting another planet is not a
        // support to you; sitting in another's dignity is.
        for (Rulership.Reception r : receptions) {     // already sorted strongest first
            if (!v.body.equals(r.a) && !v.body.equals(r.b)) {
                continue;
            }
            if (r.mutual || v.body.equals(r.received())) {
                v.reception = r;
                break;
            }
        }
    }

    /** The seven a classical chart is judged unaspected against. */
    private static final List<String> TRADITIONAL_SEVEN = java.util.Arrays.asList(
        "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn");

    /**
     * Splits the chart-wide aspect lists onto one body, tightest first.
     *
     * Only the body-to-body hits populate v.aspects; the angle hits feed prominence but
     * are not aspects "to" anything the reading would name as a partner.
     */
    private static void collectAspects(Vector v, String name,
                                       List<Aspects.Hit> bodyAspects,
                                       List<Aspects.Hit> angleAspects) {
        for (Aspects.Hit h : bodyAspects) {
            if (name.equals(h.a) || name.equals(h.b)) {
                v.aspects.add(h);
            }
        }
        v.aspects.sort((x, y) -> Double.compare(x.offBy, y.offBy));
        v.aspectCount = v.aspects.size();

        boolean touchesTraditional = false;
        for (Aspects.Hit h : v.aspects) {
            String other = name.equals(h.a) ? h.b : h.a;
            if (TRADITIONAL_SEVEN.contains(other)) {
                touchesTraditional = true;
                break;
            }
        }
        // Only meaningful for the traditional seven themselves; calling Chiron or the
        // Node "unaspected" is a category error, not an observation.
        v.isUnaspected = TRADITIONAL_SEVEN.contains(name) && !touchesTraditional;

        for (Aspects.Hit h : angleAspects) {
            if (name.equals(h.a) || name.equals(h.b)) {
                v.angleAspects.add(h);
            }
        }
    }

    /**
     * Prominence. Angularity, role, and now the L4 aspect terms.
     *
     * Still missing stellium and dispositor membership, which arrive with L5. Before the
     * aspect terms this was dominated by angularity, which is also the fastest-moving
     * input - the lead body of a "now" chart could change every twenty minutes purely
     * from the Ascendant advancing. Aspects are stable across hours, so they steady it.
     */
    private static double computeProminence(Vector v) {
        double p = 0.0;

        if (v.angularity > 0.0) {
            p += v.angularity;
            v.prominenceReasons.add(String.format("%.1f° from the %s",
                v.degreesToAngle, v.nearestAngle));
        }
        if (v.isChartRuler) {
            p += weightChartRuler;
            v.prominenceReasons.add("rules the Ascendant");
        }
        if (v.isSectLight) {
            p += weightSectLight;
            v.prominenceReasons.add("the sect light");
        }
        if (v.isOutOfSectMalefic) {
            p += weightOutOfSectMalefic;
            v.prominenceReasons.add("the out-of-sect malefic");
        }
        if (v.isStationary) {
            p += weightStationary;
            v.prominenceReasons.add("stationary");
        }

        // --- L4 aspect terms ---

        // Tight aspects to a luminary. Scaled by closeness to exact, so a partile square
        // to the Sun counts and one at the edge of a 10 degree orb barely does.
        double toLights = 0.0;
        Aspects.Hit tightestLight = null;
        for (Aspects.Hit h : v.aspects) {
            String other = v.body.equals(h.a) ? h.b : h.a;
            if ("Sun".equals(other) || "Moon".equals(other)) {
                toLights += h.tightness;
                if (tightestLight == null || h.offBy < tightestLight.offBy) {
                    tightestLight = h;
                }
            }
        }
        if (toLights > 0.0) {
            p += weightAspectToLight * toLights;
            String other = v.body.equals(tightestLight.a) ? tightestLight.b : tightestLight.a;
            v.prominenceReasons.add(String.format("%s %s, %.1f° off exact",
                tightestLight.type.label.toLowerCase(), other, tightestLight.offBy));
        }

        // Soft aspects to an angle only - see weightAspectToAngle for why the hard ones
        // are excluded rather than merely weighted down.
        double toAngles = 0.0;
        for (Aspects.Hit h : v.angleAspects) {
            if (h.type == Aspects.Type.SEXTILE || h.type == Aspects.Type.TRINE) {
                toAngles += h.tightness;
            }
        }
        if (toAngles > 0.0) {
            p += weightAspectToAngle * toAngles;
            v.prominenceReasons.add("in soft aspect to an angle");
        }

        // Breadth, saturating at five so a heavily aspected body does not run away with
        // the ranking on connection count alone.
        if (v.aspectCount > 0) {
            double breadth = Math.min(v.aspectCount, 5) / 5.0;
            p += weightAspectBreadth * breadth;
            v.prominenceReasons.add(v.aspectCount + (v.aspectCount == 1 ? " aspect" : " aspects"));
        }

        if (v.isUnaspected) {
            p += weightUnaspected;
            v.prominenceReasons.add("unaspected by the traditional seven");
        }

        // --- L4 rulership terms ---

        if (v.rulesAngularHouse) {
            p += weightRulesAngularHouse;
            v.prominenceReasons.add("rules an angular house");
        }
        if (!v.housesRuled.isEmpty()) {
            double breadth = Math.min(v.housesRuled.size(), 3) / 3.0;
            p += weightHousesRuled * breadth;
            v.prominenceReasons.add(v.housesRuled.size() == 1
                ? "rules house " + v.housesRuled.get(0)
                : "rules houses " + v.housesRuled);
        }

        // --- L5 membership terms ---

        if (v.stelliumSign != null) {
            p += weightStellium;
            v.prominenceReasons.add("in the " + v.stelliumSign + " stellium");
        }
        // Sole and plain final dispositor are exclusive: the sole one is a final one, and
        // scoring both would pay twice for a single fact.
        if (v.isSoleDispositor) {
            p += weightSoleDispositor;
            v.prominenceReasons.add("sole dispositor of the chart");
        } else if (v.isFinalDispositor) {
            p += weightFinalDispositor;
            v.prominenceReasons.add("final dispositor");
        }
        if (v.inDispositorLoop) {
            p += weightDispositorLoop;
            v.prominenceReasons.add("in a dispositor loop");
        }

        return p;
    }

    /** One line per body, four axes side by side, so the vector stays visible. */
    public static String table(List<Vector> ranked) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  %-11s %-16s %5s %5s %5s %5s  %s%n",
            "body", "position", "dig", "cond", "val", "prom", "why"));
        for (Vector v : ranked) {
            sb.append(String.format("  %-11s %-16s %+5d %5.2f %+5d %5.2f  %s%n",
                v.body, Zodiac.format(v.longitude),
                v.dignity.score, v.conditionPartial, v.valence.score, v.prominence,
                String.join("; ", v.allReasons())));
        }
        return sb.toString();
    }
}
