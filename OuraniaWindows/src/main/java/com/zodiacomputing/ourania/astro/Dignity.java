package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * L3 axis 1: essential dignity.
 *
 * Pure table lookup against a longitude and the chart's sect. No ephemeris, no I/O.
 *
 * Two things here that simpler implementations get wrong:
 *
 *  - Peregrination is tested against ALL five dignities, not just domicile and
 *    exaltation. A planet sitting in its own bound is not peregrine. Mercury at 17 Pisces
 *    is in detriment AND fall AND its own Egyptian bound, which is exactly the case that
 *    breaks a naive first-match-wins evaluator.
 *  - Detriment and fall can co-occur, so they are separate booleans rather than branches
 *    of one enum.
 *
 * Detriment and fall are derived from the domicile and exaltation tables rather than
 * typed out again, so they cannot disagree with them.
 */
public final class Dignity {

    // Lilly's point values.
    public static final int PTS_DOMICILE   =  5;
    public static final int PTS_EXALTATION =  4;
    public static final int PTS_TRIPLICITY =  3;
    public static final int PTS_BOUND      =  2;
    public static final int PTS_FACE       =  1;
    public static final int PTS_DETRIMENT  = -5;
    public static final int PTS_FALL       = -4;
    /**
     * Peregrine, in the three values the sources actually use. Set {@link #peregrinePenalty}.
     *
     * <b>This was a flat -5 until 2026-08-24, and the measurement is why it is not any more.</b>
     * Over 300 charts and the seven traditional bodies:
     *
     * <ul>
     *   <li><b>34% of all planets are peregrine</b> - 715 of 2,100. It is the single most
     *       common condition there is, not an edge case. Only 19 charts in 300 had none, and
     *       the median chart has two or three.</li>
     *   <li>At -5 the mean seven-planet total is <b>-6.12</b> and <b>205 charts in 300 score
     *       net negative</b>. A number two thirds of charts share is a constant offset wearing
     *       the costume of a judgement - it discriminates nothing.</li>
     *   <li>At -2 the mean total is <b>+1.03</b>, so the scale is centred and the sign of a
     *       score means something again. At 0 it is +5.80 and 84 in 300 are negative.</li>
     * </ul>
     *
     * <b>And -5 breaks the ordering the scale exists to express.</b> It puts peregrine level
     * with detriment and BELOW fall, so a planet that is merely unsupported scores worse than
     * one the tradition calls actively brought low. The graduated value keeps the sequence
     * monotone: +5, +4, +3, +2, +1, -2, -4, -5.
     */
    public static final int PTS_PEREGRINE_CLASSICAL  = -5;
    public static final int PTS_PEREGRINE_GRADUATED  = -2;
    public static final int PTS_PEREGRINE_MODERN     =  0;

    /**
     * The peregrine penalty in force. Default {@link #PTS_PEREGRINE_GRADUATED}.
     *
     * Settable rather than final for the same reason {@link #countParticipatingTriplicity} is:
     * the sources genuinely differ and the choice changes every score, so it belongs where a
     * setting can reach it. <b>Use CLASSICAL for horary and medieval work</b>, where a
     * peregrine significator really is considered helpless and -5 is the received value.
     */
    public static int peregrinePenalty = PTS_PEREGRINE_GRADUATED;

    /** The seven traditional bodies, the only ones essential dignity applies to. */
    public static final String[] TRADITIONAL =
        {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn"};

    /** Domicile ruler of each sign, indexed as Zodiac.SIGNS. */
    private static final String[] DOMICILE = {
        "Mars",    // Aries
        "Venus",   // Taurus
        "Mercury", // Gemini
        "Moon",    // Cancer
        "Sun",     // Leo
        "Mercury", // Virgo
        "Venus",   // Libra
        "Mars",    // Scorpio
        "Jupiter", // Sagittarius
        "Saturn",  // Capricorn
        "Saturn",  // Aquarius
        "Jupiter"  // Pisces
    };

    /** Exaltation sign index and exact degree, per body. */
    private static final String[] EXALT_BODY = {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn"};
    private static final int[]    EXALT_SIGN = {0,     1,      5,         11,      9,      3,         6};
    private static final int[]    EXALT_DEG  = {19,    3,      15,        27,      28,     15,        21};

    /**
     * Dorothean triplicity rulers by element: day, night, participating.
     * Element = signIndex % 4, so 0 fire, 1 earth, 2 air, 3 water.
     */
    private static final String[][] TRIPLICITY = {
        {"Sun",   "Jupiter", "Saturn"},  // fire
        {"Venus", "Moon",    "Mars"},    // earth
        {"Saturn","Mercury", "Jupiter"}, // air
        {"Venus", "Mars",    "Moon"}     // water
    };

    /**
     * Egyptian bounds. Five segments per sign as {ruler, startDegree, endDegree},
     * inclusive start, exclusive end. Verified by BOUNDS checksum in DignityCheck:
     * contiguous, five distinct non-luminary rulers per sign, and each planet's total
     * across the zodiac equal to its planetary years.
     */
    private static final Object[][][] BOUNDS = {
        {{"Jupiter",0,6},{"Venus",6,12},{"Mercury",12,20},{"Mars",20,25},{"Saturn",25,30}},      // Aries
        {{"Venus",0,8},{"Mercury",8,14},{"Jupiter",14,22},{"Saturn",22,27},{"Mars",27,30}},      // Taurus
        {{"Mercury",0,6},{"Jupiter",6,12},{"Venus",12,17},{"Mars",17,24},{"Saturn",24,30}},      // Gemini
        {{"Mars",0,7},{"Venus",7,13},{"Mercury",13,19},{"Jupiter",19,26},{"Saturn",26,30}},      // Cancer
        {{"Jupiter",0,6},{"Venus",6,11},{"Saturn",11,18},{"Mercury",18,24},{"Mars",24,30}},      // Leo
        {{"Mercury",0,7},{"Venus",7,17},{"Jupiter",17,21},{"Mars",21,28},{"Saturn",28,30}},      // Virgo
        {{"Saturn",0,6},{"Mercury",6,14},{"Jupiter",14,21},{"Venus",21,28},{"Mars",28,30}},      // Libra
        {{"Mars",0,7},{"Venus",7,11},{"Mercury",11,19},{"Jupiter",19,24},{"Saturn",24,30}},      // Scorpio
        {{"Jupiter",0,12},{"Venus",12,17},{"Mercury",17,21},{"Saturn",21,26},{"Mars",26,30}},    // Sagittarius
        {{"Mercury",0,7},{"Jupiter",7,14},{"Venus",14,22},{"Saturn",22,26},{"Mars",26,30}},      // Capricorn
        {{"Mercury",0,7},{"Venus",7,13},{"Jupiter",13,20},{"Mars",20,25},{"Saturn",25,30}},      // Aquarius
        {{"Venus",0,12},{"Jupiter",12,16},{"Mercury",16,19},{"Mars",19,28},{"Saturn",28,30}}     // Pisces
    };

    /**
     * Whether the participating triplicity ruler earns the +3 alongside the sect-matching
     * ruler. Sources differ. Default false: Lilly's single +3 goes to the ruler whose
     * sect matches the chart, and the participating ruler is recorded as a flag only.
     * Flipping this changes dignity scores, so it belongs in the settings block.
     */
    public static boolean countParticipatingTriplicity = false;

    public static final class Result {
        public String body;
        public String sign;
        public double degreeInSign;

        public boolean domicile;
        public boolean exaltation;
        public boolean exactExaltationDegree;
        public boolean triplicity;
        public boolean triplicityParticipating;
        public boolean bound;
        public boolean face;
        public boolean detriment;
        public boolean fall;
        public boolean peregrine;

        public String boundRuler;
        public String faceRuler;
        public int score;
        public final List<String> reasons = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("%-9s %-18s %+3d  %s", body, Zodiac.format(
                Zodiac.longitudeOf(indexOfSign(sign), (int) degreeInSign,
                    (int) ((degreeInSign % 1) * 60), 0)), score,
                reasons.isEmpty() ? "no essential dignity" : String.join("; ", reasons));
        }
    }

    private Dignity() { }

    // ------------------------------------------------------------------ lookups

    public static String domicileRulerOf(int signIndex) {
        return DOMICILE[Math.floorMod(signIndex, 12)];
    }

    /** The body in detriment in this sign: the ruler of the opposite sign. */
    public static String detrimentBodyOf(int signIndex) {
        return DOMICILE[Math.floorMod(signIndex + 6, 12)];
    }

    /** The body exalted in this sign, or null. */
    public static String exaltedBodyOf(int signIndex) {
        for (int i = 0; i < EXALT_BODY.length; i++) {
            if (EXALT_SIGN[i] == Math.floorMod(signIndex, 12)) {
                return EXALT_BODY[i];
            }
        }
        return null;
    }

    /** The body in fall in this sign: the one exalted in the opposite sign. */
    public static String fallBodyOf(int signIndex) {
        return exaltedBodyOf(signIndex + 6);
    }

    /** Sect-appropriate triplicity ruler: day ruler by day, night ruler by night. */
    public static String triplicityRulerOf(int signIndex, boolean diurnal) {
        return TRIPLICITY[Math.floorMod(signIndex, 12) % 4][diurnal ? 0 : 1];
    }

    public static String participatingTriplicityRulerOf(int signIndex) {
        return TRIPLICITY[Math.floorMod(signIndex, 12) % 4][2];
    }

    /**
     * Where one sign's Egyptian bounds begin, in degrees into the sign.
     *
     * <b>The edges only; the ruler still comes from {@link #boundRulerOf}.</b> The wheel
     * needs both to draw a bound ring - where to put the divisions and what to write in each
     * segment - and handing out the table itself would let a caller build a second copy of a
     * rule this class exists to be the only statement of. Six values: five starts and the
     * closing 30, so a caller can take pairs without special-casing the last one.
     *
     * <b>The bounds table had no reader outside this package.</b> Dignity has scored bound
     * placements since it was written and the wheel drew nothing for them, which is why David
     * could look at a chart, see a terms ring in another program, and find ours had none - the
     * engine was there the whole time and had no door.
     */
    public static double[] boundEdges(int signIndex) {
        Object[][] rows = BOUNDS[((signIndex % 12) + 12) % 12];
        double[] edges = new double[rows.length + 1];
        for (int i = 0; i < rows.length; i++) {
            edges[i] = ((Integer) rows[i][1]).doubleValue();
        }
        edges[rows.length] = 30.0;
        return edges;
    }

    /** Egyptian bound ruler for a longitude. Inclusive start, exclusive end. */
    public static String boundRulerOf(double longitude) {
        int s = Zodiac.signIndex(longitude);
        double d = Zodiac.degreeInSign(longitude);
        for (Object[] seg : BOUNDS[s]) {
            double start = ((Integer) seg[1]).doubleValue();
            double end = ((Integer) seg[2]).doubleValue();
            if (d >= start && d < end) {
                return (String) seg[0];
            }
        }
        // Only reachable if a bounds row is malformed; the checksum test guards this.
        throw new IllegalStateException("no bound covers " + Zodiac.format(longitude));
    }

    /** Face ruler is the Chaldean decan ruler. Same computation, traditional name. */
    public static String faceRulerOf(double longitude) {
        return Zodiac.chaldeanDecanRuler(longitude);
    }

    // ------------------------------------------------------------------ evaluation

    public static Result evaluate(String body, double longitude, boolean diurnal) {
        Result r = new Result();
        r.body = body;
        int s = Zodiac.signIndex(longitude);
        r.sign = Zodiac.SIGNS[s];
        r.degreeInSign = Zodiac.degreeInSign(longitude);

        if (!isTraditional(body)) {
            r.reasons.add("outside the traditional seven - essential dignity does not apply");
            return r;
        }

        // Positive dignities.
        if (domicileRulerOf(s).equals(body)) {
            r.domicile = true;
            r.score += PTS_DOMICILE;
            r.reasons.add("in domicile (+" + PTS_DOMICILE + ")");
        }
        if (body.equals(exaltedBodyOf(s))) {
            r.exaltation = true;
            r.score += PTS_EXALTATION;
            int exactDeg = exactExaltationDegree(body);
            r.exactExaltationDegree = ((int) Math.floor(r.degreeInSign)) + 1 == exactDeg;
            r.reasons.add("exalted (+" + PTS_EXALTATION + ")"
                + (r.exactExaltationDegree ? ", on the exact degree of exaltation" : ""));
        }
        if (triplicityRulerOf(s, diurnal).equals(body)) {
            r.triplicity = true;
            r.score += PTS_TRIPLICITY;
            r.reasons.add((diurnal ? "day" : "night") + " triplicity ruler (+" + PTS_TRIPLICITY + ")");
        } else if (participatingTriplicityRulerOf(s).equals(body)) {
            r.triplicityParticipating = true;
            if (countParticipatingTriplicity) {
                r.triplicity = true;
                r.score += PTS_TRIPLICITY;
                r.reasons.add("participating triplicity ruler (+" + PTS_TRIPLICITY + ")");
            } else {
                r.reasons.add("participating triplicity ruler (no points under current setting)");
            }
        }
        r.boundRuler = boundRulerOf(longitude);
        if (r.boundRuler.equals(body)) {
            r.bound = true;
            r.score += PTS_BOUND;
            r.reasons.add("in its own bound (+" + PTS_BOUND + ")");
        }
        r.faceRuler = faceRulerOf(longitude);
        if (r.faceRuler.equals(body)) {
            r.face = true;
            r.score += PTS_FACE;
            r.reasons.add("in its own face (+" + PTS_FACE + ")");
        }

        // Debilities. These stack: Mercury in Pisces is in both detriment and fall.
        if (detrimentBodyOf(s).equals(body)) {
            r.detriment = true;
            r.score += PTS_DETRIMENT;
            r.reasons.add("in detriment (" + PTS_DETRIMENT + ")");
        }
        if (body.equals(fallBodyOf(s))) {
            r.fall = true;
            r.score += PTS_FALL;
            r.reasons.add("in fall (" + PTS_FALL + ")");
        }

        // Peregrine means no dignity of any of the five kinds. Tested last, and after
        // bound and face specifically - face exists to stop total peregrination.
        // Not applied on top of detriment or fall: those are already scored debilities,
        // and double-counting would penalise the same condition twice.
        // Participating triplicity rulership counts as dignity for peregrination even
        // when it scores nothing, because peregrine means "no dignity anywhere" and a
        // participating ruler plainly has some. Otherwise the reasons contradict:
        // "participating triplicity ruler; peregrine - no essential dignity anywhere".
        boolean anyDignity = r.domicile || r.exaltation || r.triplicity
            || r.bound || r.face || r.triplicityParticipating;
        if (!anyDignity) {
            r.peregrine = true;
            if (!r.detriment && !r.fall) {
                r.score += peregrinePenalty;
                r.reasons.add("peregrine - no essential dignity anywhere ("
                    + peregrinePenalty + ")");
            } else {
                r.reasons.add("also peregrine, but not scored again on top of detriment or fall");
            }
        }

        return r;
    }

    /**
     * Almuten of a degree: the traditional body with the highest dignity score there.
     * Used for chart-ruler determination and lot rulership. Ties return the first found,
     * which is stable because TRADITIONAL is a fixed order.
     */
    public static String almutenOf(double longitude, boolean diurnal) {
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        for (String b : TRADITIONAL) {
            int sc = evaluate(b, longitude, diurnal).score;
            if (sc > bestScore) {
                bestScore = sc;
                best = b;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ helpers

    public static boolean isTraditional(String body) {
        for (String b : TRADITIONAL) {
            if (b.equals(body)) {
                return true;
            }
        }
        return false;
    }

    public static int exactExaltationDegree(String body) {
        for (int i = 0; i < EXALT_BODY.length; i++) {
            if (EXALT_BODY[i].equals(body)) {
                return EXALT_DEG[i];
            }
        }
        return -1;
    }

    static int indexOfSign(String sign) {
        for (int i = 0; i < Zodiac.SIGNS.length; i++) {
            if (Zodiac.SIGNS[i].equals(sign)) {
                return i;
            }
        }
        return 0;
    }

    /** Exposed for the checksum test so the table cannot drift unnoticed. */
    static Object[][][] boundsTable() {
        return BOUNDS;
    }
}
