package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * L8: zodiacal releasing.
 *
 * Hellenistic, from the Lot of Fortune (body, circumstance, livelihood) or the Lot of Spirit
 * (agency, career, action). Periods run in zodiacal order from the Lot's sign, each lasting
 * the number of years belonging to that sign's domicile ruler.
 *
 * <h3>The spec's period lengths are wrong, and demonstrably so</h3>
 *
 * [[L8-time-layer-spec]] says the periods last "that sign's ruler's planetary years - the
 * same figures that check the Egyptian bounds: Saturn 57, Jupiter 79, Mars 66, Venus 82,
 * Mercury 76". Those are the <b>greater</b> years. They are the right figures for the bounds
 * checksum - they sum to exactly 360, which is why {@link Dignity}'s BOUNDS table verifies
 * against them - and the wrong ones here.
 *
 * The demonstration is arithmetic, not opinion. At 82 years a single Venus-ruled period
 * outlasts most lives, so a chart would contain one or two periods. The spec's own comparison
 * table gives this technique a resolution of "months to decades" and calls its output the
 * "chapter structure of a life"; one chapter is not a structure. Releasing uses the
 * <b>lesser</b> years, which total 211 for a full circuit and give a life several chapters.
 *
 * <h3>Loosing of the bond, and how the sources were reconciled</h3>
 *
 * Sub-periods run in zodiacal order from their parent's sign. When they have gone all the way
 * round and would arrive back at the starting sign, they do <b>not</b> repeat it: the sequence
 * jumps to the sign <b>opposite the parent's sign</b> and continues in order from there. That
 * break is the loosing of the bond.
 *
 * Two sources disagreed about the trigger, and the difference moves every subsequent date:
 *
 * <ul>
 *   <li>The Astrology Podcast (Chris Brennan, whose <i>Hellenistic Astrology</i> this project
 *       already cites in {@code CorpusBuilder}) - the jump comes after a <b>full circuit</b>,
 *       "always about 17 and a half years into" the parent period.</li>
 *   <li>Kerykeion - the jump comes when a sub-period "reaches the sign opposite the starting
 *       sign", which would be roughly halfway round.</li>
 * </ul>
 *
 * <b>The arithmetic settles it.</b> The twelve sign figures total 211. Run as months that is
 * 211/12 = <b>17.58 years</b>, matching Brennan's "about 17 and a half" to within a month -
 * far too close to be coincidence, and impossible to reach at the halfway point. The
 * full-circuit reading is implemented; the halfway reading is not. {@code TransitCheck} Part K
 * pins the 211-month figure so the reasoning cannot quietly rot.
 *
 * <h3>Sub-period scaling: years become months</h3>
 *
 * The same figures, one unit down per level. A 20-year Gemini period contains a 20-<i>month</i>
 * Gemini sub-period, then 25 months of Cancer, and so on. Sub-periods therefore do not
 * partition their parent exactly - the last one is cut off when the parent ends, which is
 * correct and not a rounding bug.
 *
 * <h3>A bond can only fire in a long parent</h3>
 *
 * A circuit costs 211 months, so only parents longer than 17.58 years ever reach one: Gemini,
 * Cancer, Virgo, Capricorn, Aquarius and Leo. And no parent is long enough for <i>two</i>
 * circuits - the longest is Aquarius at 30 years, 360 months, against 422 for two - so the
 * bond fires at most once per sequence. Part K asserts that, because if a longer period ever
 * appeared the "at most once" simplification below would silently become wrong.
 *
 * <h3>Not wired into the convergence rule, deliberately</h3>
 *
 * Releasing names an activated <i>sign</i> and a chapter of life; {@link Convergence} ranks
 * <i>natal points</i> by how many techniques land on them. The natural bridge would be the
 * period's ruler, the way profection contributes its lord of the year - but the work plan
 * already records that adding families has stopped improving discrimination, and an eighth
 * voter on a ranking where nothing scores below 3 would make it worse rather than better.
 * Left out until that is addressed.
 */
public final class ZodiacalReleasing {

    /**
     * L1 period lengths in years, by sign index, from the domicile ruler's lesser years.
     *
     * Mars 15, Venus 8, Mercury 20, Moon 25, Sun 19, Jupiter 12, Saturn 30 - with Capricorn
     * taking 27 rather than Saturn's 30, which is the one irregularity in the table and is
     * traditional rather than derived. Total for a circuit: 211 years.
     */
    public static final int[] PERIOD_YEARS = {
        15,  // Aries       - Mars
        8,   // Taurus      - Venus
        20,  // Gemini      - Mercury
        25,  // Cancer      - Moon
        19,  // Leo         - Sun
        20,  // Virgo       - Mercury
        8,   // Libra       - Venus
        15,  // Scorpio     - Mars
        12,  // Sagittarius - Jupiter
        27,  // Capricorn   - Saturn, the irregular one
        30,  // Aquarius    - Saturn
        12   // Pisces      - Jupiter
    };

    /** Idealized 360-day year used for all releasing calculations (30-day months). */
    public static final double DAYS_PER_YEAR = 360.0;

    /** The bond rule is implemented; see the class comment for the sources and arithmetic. */
    public static final boolean bondApplied = true;

    /** Periods in a full circuit of the twelve signs. */
    public static final int SIGNS_IN_CIRCUIT = 12;

    /** One releasing period. */
    public static final class Period {
        /** 1 for the general periods, 2 for their sub-periods, and so on. */
        public int level;
        public int sign;
        public double startJd;
        public double endJd;
        /** Length in this level's own unit: years at L1, months at L2, and so on. */
        public double units;
        /** Angular to the Lot of Spirit's sign - the spec's "peak period". */
        public boolean peak;
        /** True when this period is the first after a loosing of the bond. */
        public boolean afterBond;
        /** True when the parent ended before this period could run its full length. */
        public boolean truncated;
        public final List<Period> children = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("L%d %s %.0f%s%s%s", level,
                capitalise(Zodiac.SIGNS[sign]), units, level == 1 ? " yr" : " mo",
                peak ? " (peak)" : "", afterBond ? " [BOND]" : "");
        }
    }

    private ZodiacalReleasing() { }

    /**
     * Whether a sign is angular to the Lot of Spirit's sign: the same sign, or the 4th, 7th
     * or 10th from it. These are the spec's peak periods.
     */
    public static boolean isPeak(int sign, int spiritSign) {
        if (sign < 0 || spiritSign < 0) {
            return false;
        }
        int from = Math.floorMod(sign - spiritSign, 12);
        return from == 0 || from == 3 || from == 6 || from == 9;
    }

    /** Days per unit at a level: years at L1, months at L2, and a twelfth again below that. */
    public static double unitDays(int level) {
        double d = DAYS_PER_YEAR;
        for (int i = 1; i < level; i++) {
            d /= 12.0;
        }
        return d;
    }

    /**
     * Releasing from a Lot, from birth until a date, to the requested depth.
     *
     * @param lotLon    the releasing Lot - Spirit for career and action, Fortune for body and
     *                  circumstance.
     * @param spiritLon the Lot of Spirit, which defines the peaks whichever Lot is released
     *                  from.
     * @param maxLevel  1 for general periods only, 2 to add sub-periods, and so on.
     */
    public static List<Period> release(double natalJd, double lotLon, double spiritLon,
                                       double untilJd, int maxLevel) {
        int spiritSign = Zodiac.signIndex(spiritLon);
        return sequence(1, maxLevel, Zodiac.signIndex(lotLon), natalJd, untilJd, spiritSign);
    }

    /** L1 only, for callers that just want the chapter list. */
    public static List<Period> l1(double natalJd, double lotLon, double spiritLon,
                                  double untilJd) {
        return release(natalJd, lotLon, spiritLon, untilJd, 1);
    }

    /**
     * One level's worth of periods, filling [startJd, endJd) from startSign.
     *
     * The bond lives here, and it is three lines: count the periods emitted, and when a full
     * circuit of twelve has gone by, take the next sign from opposite the sequence's start
     * rather than from the one just finished.
     *
     * The sequence is cut off by endJd rather than partitioning it, which is what makes the
     * last child {@code truncated} - correct behaviour, since a parent's length and its
     * children's lengths are independent quantities in this technique.
     */
    private static List<Period> sequence(int level, int maxLevel, int startSign,
                                         double startJd, double endJd, int spiritSign) {
        List<Period> out = new ArrayList<>();
        if (level > maxLevel || startJd >= endJd) {
            return out;
        }
        double unit = unitDays(level);
        int sign = startSign;
        int emitted = 0;
        boolean bonded = false;
        double t = startJd;
        // Generous stop: a circuit is twelve periods and no parent affords two of them.
        for (int guard = 0; guard < 64 && t < endJd - 1e-9; guard++) {
            Period p = new Period();
            p.level = level;
            p.sign = sign;
            p.units = PERIOD_YEARS[sign];
            p.startJd = t;
            double natural = t + p.units * unit;
            p.endJd = Math.min(natural, endJd);
            p.truncated = natural > endJd + 1e-9;
            p.peak = isPeak(sign, spiritSign);
            p.afterBond = bonded && emitted == 0;
            p.children.addAll(
                sequence(level + 1, maxLevel, sign, p.startJd, p.endJd, spiritSign));
            out.add(p);

            t = natural;
            emitted++;
            if (emitted == SIGNS_IN_CIRCUIT && !bonded) {
                // Loosing of the bond: jump to the sign opposite the sequence's start.
                sign = (startSign + 6) % 12;
                emitted = 0;
                bonded = true;
            } else {
                sign = (sign + 1) % 12;
            }
        }
        return out;
    }

    /** Every period at a given level, flattened, in time order. */
    public static List<Period> atLevel(List<Period> periods, int level) {
        List<Period> out = new ArrayList<>();
        for (Period p : periods) {
            if (p.level == level) {
                out.add(p);
            }
            out.addAll(atLevel(p.children, level));
        }
        return out;
    }

    /** The period containing a date, or null. */
    public static Period at(List<Period> periods, double jd) {
        for (Period p : periods) {
            if (jd >= p.startJd && jd < p.endJd) {
                return p;
            }
        }
        return null;
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
