package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * L4: aspects between bodies, and between bodies and the angles.
 *
 * This logic was written first inside SkymapPanel, where it drew the wheel's aspect
 * lines and grid. It lives here now because BodyScore needs the same answers to rank a
 * chart - prominence without aspects is dominated by angularity, which is the one input
 * that changes minute to minute - and because two definitions of "what a square is"
 * disagreeing silently is the kind of defect nothing would ever surface.
 *
 * Keyed by body NAME, not index. The two layers disagree about index 10: the GUI's
 * tenth body is Chiron, ChartFrame's is the North Node. An index-keyed port would have
 * handed Chiron's 3-degree orb to the Node and nobody would have noticed.
 *
 * Behaviour is deliberately identical to the GUI original, including the parts that are
 * conventions rather than truths: the pair takes the LARGER of the two orbs, applying is
 * judged by stepping both bodies one day forward, and a conjunction is tested against
 * the orb before any other aspect is considered.
 */
public final class Aspects {

    /**
     * The aspects the engine recognises, in the order the GUI tests them.
     *
     * <b>Each carries its own maximum orb, and that is load-bearing.</b> {@link #orbFor} takes
     * the larger of the two bodies' orbs, which reaches 10 degrees off the Sun - fine for a
     * conjunction, meaningless for a semisextile. The cap is declared here rather than applied
     * as a special case inside {@link #typeOf} so that there is exactly one definition per
     * aspect; the last time this project had one rule in two places it drifted to a 74%
     * disagreement.
     *
     * <b>The minor caps were chosen, not measured.</b> A sweep over 300 charts on 2026-08-23
     * found the count of detected aspects perfectly linear in the cap - about 7.8 more per
     * chart for every extra half degree, no plateau and no cliff - so the corpus cannot pick a
     * number and any value taken from it would be taste in the costume of measurement. 1.0 is
     * David's decision, inside conventional practice for minor aspects. See WORK-PLAN.
     *
     * <b>Four more were added on 2026-08-31, also David's call.</b> Septile (360/7), novile
     * (360/9), decile (360/10) and biquintile (2 x 72), all capped at the same 1.0 degrees.
     * They arrived because the prose existed: a supplied minor-aspect set carried ten aspect
     * types and four of them were not on this enum, so 9,976 readings had nowhere to go.
     * <b>Prose is not a reason to add an aspect</b> - the reason is that he wanted them - but
     * it is why the question came up, and it is recorded here so nobody later reads the enum
     * and assumes the set was chosen on doctrine alone.
     *
     * They sit in angle order like the rest, and none of them collides: at 1.0 degrees the
     * nearest neighbours are decile to semisextile (6 apart) and novile to semisquare (5),
     * so typeOf cannot match two. Harmonics reuse existing families - decile joins
     * sesquiquintile at 10, biquintile joins quintile at 5 - which is correct rather than
     * convenient: they are the same division of the circle.
     *
     * <b>The quincunx was capped at the same time, and that changed a shipped feature.</b> It
     * had been running uncapped at up to 10 degrees and firing 15.4 times per chart out of
     * 83.6; at 1.0 it fires 2.9. Charts lose quincunx readings they used to show. That is the
     * trade that lets five new aspects be added for a 4% increase in total lines rather than
     * 37%.
     */
    public enum Type {
        CONJUNCTION("Conjunction", 0.0, Double.MAX_VALUE, 1, "Mercury", 0),
        SEMISEXTILE("Semisextile", 30.0, 1.0, 12, null, 1),
        DECILE("Decile", 36.0, 1.0, 10, null, -1),
        NOVILE("Novile", 40.0, 1.0, 9, null, -1),
        SEMISQUARE("Semisquare", 45.0, 1.0, 8, null, -1),
        SEPTILE("Septile", 360.0 / 7.0, 1.0, 7, null, -1),
        SEXTILE("Sextile", 60.0, Double.MAX_VALUE, 6, "Venus", 2),
        QUINTILE("Quintile", 72.0, 1.0, 5, null, -1),
        SQUARE("Square", 90.0, Double.MAX_VALUE, 4, "Mars", 3),
        SESQUIQUINTILE("Sesquiquintile", 108.0, 1.0, 10, null, -1),
        TRINE("Trine", 120.0, Double.MAX_VALUE, 3, "Jupiter", 4),
        SESQUIQUADRATE("Sesquiquadrate", 135.0, 1.0, 8, null, -1),
        BIQUINTILE("Biquintile", 144.0, 1.0, 5, null, -1),
        QUINCUNX("Quincunx", 150.0, 1.0, 12, null, 5),
        OPPOSITION("Opposition", 180.0, Double.MAX_VALUE, 2, "Saturn", 6);

        public final String label;
        public final double exactAngle;
        /** Hard ceiling on this aspect's orb. MAX_VALUE means "whatever the bodies allow". */
        public final double maxOrb;

        /**
         * Which division of the circle this aspect is: 360 / harmonic.
         *
         * The families the tradition actually groups by. 1/2/3/4/6 are the Ptolemaic
         * skeleton; 8 is the material-challenge family (semisquare and sesquiquadrate);
         * 12 is aversion (semisextile and quincunx - signs that share no element, mode or
         * polarity and traditionally cannot see each other); 5 and 10 are Kepler's
         * creative pair.
         */
        public final int harmonic;

        /**
         * The planet whose nature the tradition assigns to this aspect, or null.
         *
         * Only the five Ptolemaic aspects carry one: conjunction Mercury (neutral, takes
         * its character from the planets involved), sextile Venus, square Mars, trine
         * Jupiter, opposition Saturn. The minors postdate the doctrine and are left null
         * rather than given a plausible-looking assignment.
         */
        public final String planetaryNature;

        /**
         * Whole signs between the two positions when this aspect is in sign, or -1.
         *
         * <b>-1 means the aspect has no whole-sign form at all</b> - 45, 72, 108 and 135
         * degrees do not land a whole number of signs away. That is not a technicality:
         * Tompkins gives it as the reason the sesquiquadrate is hard to spot on a chart and
         * therefore ends up suppressed. Only an aspect with a whole-sign form can be
         * dissociate, which is what {@link Hit#dissociate} depends on.
         */
        public final int signSteps;

        Type(String label, double exactAngle, double maxOrb,
             int harmonic, String planetaryNature, int signSteps) {
            this.label = label;
            this.exactAngle = exactAngle;
            this.maxOrb = maxOrb;
            this.harmonic = harmonic;
            this.planetaryNature = planetaryNature;
            this.signSteps = signSteps;
        }

        /** True for the five added 2026-08-23 plus the quincunx: the capped, subtler set. */
        public boolean isMinor() {
            return maxOrb != Double.MAX_VALUE;
        }

        /** True for the five Ptolemaic aspects, which are the ones with a planetary nature. */
        public boolean isPtolemaic() {
            return planetaryNature != null;
        }

        /** True when this aspect has a whole-sign form and so can be in or out of sign. */
        public boolean isWholeSign() {
            return signSteps >= 0;
        }

        /** The Type for a display label, or null. Lets the GUI keep its string API. */
        public static Type fromLabel(String label) {
            for (Type t : values()) {
                if (t.label.equals(label)) {
                    return t;
                }
            }
            return null;
        }
    }

    /**
     * The three degrees of solar proximity, closest first. Cazimi is a dignity and
     * outranks combustion, which is a debility; under the beams is the mild outer band.
     *
     * EACH CONSTANT CARRIES ITS OWN DISPLAY LABEL, and that is not decoration. This enum
     * held two constants and SkymapPanel rendered it with a two-way ternary -
     * {@code s == CAZIMI ? "Cazimi" : "Combust"} - so the moment UNDER_BEAMS was added,
     * every planet 8 to 15 degrees from the Sun began printing as "Combust" on the wheel.
     * Measured over 324 charts that was 263 mislabels against 246 correct ones: more than
     * half of what the wheel called combust was not, and nothing failed a check.
     *
     * A label on the constant makes that unrepeatable - a fourth value cannot be added
     * without supplying one, and there is no else-branch left to fall into.
     */
    public enum Solar {
        CAZIMI("Cazimi"),
        COMBUST("Combust"),
        UNDER_BEAMS("Under the beams");

        /** How this reads on the wheel. One spelling, here, not at each call site. */
        public final String label;

        Solar(String label) {
            this.label = label;
        }
    }

    /**
     * Maximum orb per body. Luminaries get the widest berth because they are largest and
     * brightest; minor points the tightest. A pair uses the LARGER of the two, so
     * Sun-Chiron is judged on the Sun's 10, not Chiron's 3.
     *
     * <b>Every point in the Bodies registry must have a case here.</b> The default is the
     * angle orb, which is 8 degrees - wider than Pluto's - so a body that falls through
     * does not fail, it quietly becomes one of the loudest things on the chart. That is
     * how the asteroids would have arrived: Vesta aspecting at 8 degrees while Neptune
     * aspects at 5. BodyCheck asserts the registry against this switch so a new entry
     * cannot reach the default by accident.
     */
    private static final java.util.Map<String, Double> ORBS = buildOrbs();

    private static java.util.Map<String, Double> buildOrbs() {
        java.util.Map<String, Double> m = new java.util.HashMap<>();
        for (String n : new String[]{"Sun", "Moon"}) {
            m.put(n, 10.0);
        }
        for (String n : new String[]{"Mercury", "Venus", "Mars"}) {
            m.put(n, 7.0);
        }
        for (String n : new String[]{"Jupiter", "Saturn"}) {
            m.put(n, 6.0);
        }
        for (String n : new String[]{"Uranus", "Neptune", "Pluto"}) {
            m.put(n, 5.0);
        }
        for (String n : new String[]{"Chiron", "North Node", "South Node"}) {
            m.put(n, 3.0);
        }
        // The four asteroids and the two calculated points are small or notional, and
        // there are six of them: at a planetary orb they would treble the aspect lines on
        // the wheel without adding a comparable amount of meaning.
        for (String n : new String[]{"Ceres", "Pallas", "Juno", "Vesta",
                                     "Eris", "Eros", "Hygiea", "Nessus", "Pholus",
                                     "Part of Fortune", "Part of Spirit", "Black Moon Lilith"}) {
            m.put(n, 2.0);
        }
        // D10's points take the calculated points' width for the same reason, and not the
        // angles' 8: the Vertex and East Point are sensitive points rather than angles of the
        // chart, and five more lots at an angle's orb would swamp the wheel.
        for (String n : new String[]{"Vertex", "East Point", "Lot of Eros", "Lot of Necessity",
                                     "Lot of Courage", "Lot of Victory", "Lot of Nemesis"}) {
            m.put(n, 2.0);
        }
        // Entered explicitly so that "is an angle" and "is unrecognised" stop being the
        // same branch. Descendant and IC are new here only in the sense of being named:
        // toAngles has always passed all four and taken this width from the default.
        for (String n : new String[]{"Ascendant", "Descendant", "MC", "IC", "Midheaven"}) {
            m.put(n, ANGLE_ORB);
        }
        return java.util.Collections.unmodifiableMap(m);
    }

    private static double bodyOrb(String name) {
        return name == null ? ANGLE_ORB : ORBS.getOrDefault(name, ANGLE_ORB);
    }

    /**
     * The orb for a single named point, exposed so a check can prove the registry and this
     * table agree. Not for use in aspect logic - a pair is always judged by
     * {@link #orbFor}, which takes the larger of the two.
     */
    public static double orbOf(String name) {
        return bodyOrb(name);
    }

    /**
     * True when the name has its own entry rather than falling to the default.
     *
     * Reads the same map {@link #bodyOrb} reads, deliberately. An earlier draft of this
     * asked the question with a second switch listing the same two dozen names, which is
     * the one thing this class is not allowed to contain: a check that passes because both
     * copies of a list were edited, and stops meaning anything the day only one of them is.
     */
    public static boolean hasExplicitOrb(String name) {
        return name != null && ORBS.containsKey(name);
    }

    /** ASC / MC / DSC / IC, and anything unrecognised. */
    public static final double ANGLE_ORB = 8.0;
    /** Within 8 degrees of the Sun: the body's own light is lost in it. */
    public static final double COMBUST_ORB = 8.0;
    /** 17 arcminutes - the heart of the Sun. */
    public static final double CAZIMI_ORB = 17.0 / 60.0;
    /**
     * Outside combustion but still inside the Sun's glare, so not visible.
     *
     * The single source for this orb. BodyScore carried a second copy as
     * {@code underBeamsOrb} while this one did the work; it was deleted rather than kept
     * in step, because a duplicate nobody reads is the dangerous kind.
     */
    public static final double UNDER_BEAMS_ORB = 15.0;
    /** Within a degree of exact. */
    public static final double PARTILE_ORB = 1.0;

    private Aspects() { }

    /** Shortest angular distance between two longitudes, 0..180. */
    public static double separation(double lonA, double lonB) {
        double d = Math.abs(lonA - lonB) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    /** The orb a pair is judged on: the larger of the two bodies'. */
    public static double orbFor(String nameA, String nameB) {
        return Math.max(bodyOrb(nameA), bodyOrb(nameB));
    }

    public static double orbFor(String nameA, String nameB, boolean isSynastry) {
        double o = orbFor(nameA, nameB);
        return isSynastry ? o * 0.5 : o;
    }

    /**
     * The width one pair is judged on for one aspect: the body orb, capped by that aspect's
     * own ceiling, <b>and for a cross-chart reading both of them halved.</b>
     *
     * <b>Halving the body orb alone is not a tightening, and that is measured rather than
     * argued.</b> {@link Type#maxOrb} caps the six minor aspects at 1.0 degree, which already
     * sits at or below half of even the smallest body orb - so before 2026-08-24 the halving
     * reached every Ptolemaic aspect and <b>not one minor aspect</b>: {@code Math.min} was
     * already binding on all six. Over 300 ordered pairs that took the minors' share of a
     * synastry from a natal chart's 16% up to 28%. The reading got shorter and simultaneously
     * more minor-weighted, which is the opposite of what tightening it is for.
     *
     * <b>Ten schemes were measured before this one was chosen</b>, including the per-aspect
     * table the source literature recommends - Cunningham's 3 to 5 degrees. That came out
     * worst of the ten at 35% minor, for the same reason in a different costume: it cuts
     * squares and trines to 3 while the minors keep their 1.0. The variable was never
     * per-aspect versus per-body, it is whether the minors are tightened in step.
     *
     * Halving the cap alongside the body orb keeps <b>49-52% of every one of the eleven
     * aspect types</b> - conjunction 51, square 51, trine 50, quincunx 51 - which is what a
     * scaling that is genuinely uniform looks like, and none of the other nine produce it.
     * Cross aspects per pair go 114.3 to 57.6, minor share stays at 16%. See WORK-PLAN.
     *
     * <b>One definition, used by both branches of {@link #typeOf}.</b> The two {@code
     * Math.min} calls this replaced were the same rule written twice, and this class's own
     * header is about what happens when that drifts.
     *
     * Note the four Ptolemaic majors and the conjunction declare {@code Double.MAX_VALUE},
     * so halving their cap is a no-op by construction: they are governed by the body orb and
     * always were.
     */
    public static double effectiveOrb(String nameA, String nameB, Type t, boolean isSynastry) {
        double cap = isSynastry ? t.maxOrb / 2.0 : t.maxOrb;
        return Math.min(orbFor(nameA, nameB, isSynastry), cap);
    }

    /**
     * The aspect a separation makes, or null. Tested in Ptolemaic order, conjunction
     * first, exactly as the wheel has always tested it.
     */
    public static Type typeOf(double separation, String nameA, String nameB, boolean isSynastry) {
        // <b>A calculated point can receive an aspect and cannot cast one.</b> Burk: the
        // angles, the nodes and the Arabic lots are not bodies, emit and reflect no light, and
        // so carry no moiety of their own - "these points... do not make aspects, they can
        // only receive aspects". Two of them together therefore have nothing between them to
        // measure, and the North Node square the Ascendant, the Part of Fortune square the
        // Part of Spirit and the South Node conjunct a lot were all being drawn and counted as
        // though they did. Settled 2026-09-03; see DECISIONS.md, K5.
        //
        // Placed here rather than at the call sites because this is the one decision the wheel,
        // the grid and every reading already share - a filter in the painter alone would leave
        // the grid still listing what the wheel had stopped drawing.
        if (bothCalculated(nameA, nameB)) {
            return null;
        }
        if (separation <= effectiveOrb(nameA, nameB, Type.CONJUNCTION, isSynastry)) {
            return Type.CONJUNCTION;
        }
        // Every declared aspect, each against its own ceiling. Iterating values() rather than
        // a hand-written array is what stops a new constant being added to the enum and
        // silently never matching - which is the shape of defect the quincunx already had
        // twice over, once with no glyph and once with no line on the wheel.
        for (Type t : Type.values()) {
            if (t == Type.CONJUNCTION) {
                continue;
            }
            if (Math.abs(separation - t.exactAngle) <= effectiveOrb(nameA, nameB, t, isSynastry)) {
                return t;
            }
        }
        return null;
    }

    /** True when neither point is a physical body, so neither can cast an aspect. */
    public static boolean bothCalculated(String nameA, String nameB) {
        return isCalculatedPoint(nameA) && isCalculatedPoint(nameB);
    }

    /** Nodes, angles and lots: real positions, no light of their own. */
    public static boolean isCalculatedPoint(String name) {
        Bodies.Def d = Bodies.byName(name);
        if (d == null) {
            return false;
        }
        return d.kind == Bodies.Kind.NODE
            || d.kind == Bodies.Kind.ANGLE
            || d.kind == Bodies.Kind.POINT;
    }

    /**
     * The aspect within a single flat orb, for transits - or null.
     *
     * <b>Why transits do not go through {@link #typeOf}.</b> That judges a pair on the natal
     * body table, ten degrees for the Sun, and a transit judged there stayed "in orb" for years:
     * measured 2026-09-15, transiting Pluto to the natal Sun averaged 5,345 days in orb, and
     * a chart carried 21.6 transits at any moment, a third of them over five degrees off. Transit
     * orbs are one flat width, {@link Transits#orb}, capped by each aspect's own ceiling so a
     * minor aspect never gets wider than it is natally.
     *
     * The calculated-point rule is kept - it is about what can cast an aspect, not about width -
     * and the nearest exact angle wins, so a wide setting cannot name the wrong aspect.
     */
    public static Type typeWithin(double separation, String nameA, String nameB, double orb) {
        if (bothCalculated(nameA, nameB)) {
            return null;
        }
        Type best = null;
        double bestOff = Double.MAX_VALUE;
        for (Type t : Type.values()) {
            double off = Math.abs(separation - t.exactAngle);
            if (off <= Math.min(orb, t.maxOrb) && off < bestOff) {
                best = t;
                bestOff = off;
            }
        }
        return best;
    }

    public static Type typeOf(double separation, String nameA, String nameB) {
        return typeOf(separation, nameA, nameB, false);
    }

    /**
     * Whether the aspect is closing rather than opening, by stepping both bodies one day
     * forward and asking whether that lands nearer exactitude. Speed-aware, so it stays
     * correct for a retrograde body and at a station.
     */
    public static boolean isApplying(double lonA, double speedA,
                                     double lonB, double speedB, Type t) {
        double now = separation(lonA, lonB);
        double next = separation(lonA + speedA, lonB + speedB);
        return Math.abs(next - t.exactAngle) < Math.abs(now - t.exactAngle);
    }

    /** Within a degree of exact - the tightest reading of an aspect. */
    public static boolean isPartile(double separation, Type t) {
        return Math.abs(separation - t.exactAngle) <= PARTILE_ORB;
    }

    /**
     * Cazimi or combustion for a body conjunct the Sun, or null.
     *
     * Strict test requires latitude to be within 17'.
     */
    public static Solar solarCondition(String name, double bodyLon, double bodyLat, double sunLon, double sunLat) {
        if (name == null || "Sun".equals(name)) {
            return null;   // the Sun is not combust by itself
        }
        double dLon = separation(bodyLon, sunLon);
        double dLat = Math.abs(bodyLat - sunLat);
        if (dLon <= CAZIMI_ORB && dLat <= CAZIMI_ORB) {
            return Solar.CAZIMI;
        }
        if (dLon <= COMBUST_ORB) return Solar.COMBUST;
        if (dLon <= UNDER_BEAMS_ORB) return Solar.UNDER_BEAMS;
        return null;
    }

    /**
     * Fallback for contexts where latitude is unavailable.
     */
    public static Solar solarCondition(String name, double bodyLon, double sunLon) {
        if (name == null || "Sun".equals(name)) {
            return null;   // the Sun is not combust by itself
        }
        double d = separation(bodyLon, sunLon);
        if (d <= CAZIMI_ORB) {
            return Solar.CAZIMI;
        }
        if (d <= COMBUST_ORB) return Solar.COMBUST;
        if (d <= UNDER_BEAMS_ORB) return Solar.UNDER_BEAMS;
        return null;
    }

    /** One aspect between two named points. */
    public static final class Hit {
        public String a;
        public String b;
        public Type type;
        public double separation;
        public double orbUsed;
        /** How far from exact, 0 at partile. */
        public double offBy;
        /** 1.0 at exact, 0.0 at the edge of orb. */
        public double tightness;
        public boolean applying;
        public boolean partile;

        /** Whole signs between the two positions, 0-6, however the degrees came out. */
        public int signDistance;

        /**
         * True when the degrees make this aspect but the signs do not - an out-of-sign,
         * or dissociate, aspect.
         *
         * A square is three signs apart in the textbook. The Sun at 29 Aries and the Moon
         * at 1 Leo are 92 degrees apart, which is a square by degree, while Aries to Leo is
         * four signs and a trine by sign. The tradition reads that as a weaker, stranger
         * square, and until now the app could not tell the two cases apart at all - it
         * measured degrees and never asked what the signs said.
         *
         * Only aspects with a whole-sign form can be dissociate; 45, 72, 108 and 135 degrees
         * have none, so this is always false for them. See {@link Type#signSteps}.
         */
        public boolean dissociate;

        @Override
        public String toString() {
            return String.format("%s %s %s (%.2f° off, %s%s)",
                a, type.label, b, offBy, applying ? "applying" : "separating",
                dissociate ? ", dissociate" : "");
        }
    }

    /**
     * Whole signs between two longitudes, counted the way an aspect counts them: 0 to 6,
     * taking the shorter way round, so a trine is 4 whether measured forwards or back.
     */
    public static int signDistance(double lonA, double lonB) {
        int d = Math.floorMod(Zodiac.signIndex(lonB) - Zodiac.signIndex(lonA), 12);
        return Math.min(d, 12 - d);
    }

    /**
     * Every aspect between two computed bodies in the frame. Bodies that failed to
     * compute are skipped rather than aspected at a fictitious 0 Aries.
     */
    public static List<Hit> betweenBodies(ChartFrame f) {
        List<Hit> out = new ArrayList<>();
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body a = f.bodies[i];
            if (a == null || !a.ok) {
                continue;
            }
            if (Bodies.byName(a.name) != null && Bodies.byName(a.name).isAngle()) continue;
            for (int j = i + 1; j < f.bodies.length; j++) {
                ChartFrame.Body b = f.bodies[j];
                if (b == null || !b.ok) {
                    continue;
                }
                if (Bodies.byName(b.name) != null && Bodies.byName(b.name).isAngle()) continue;
                Hit h = hit(a.name, a.lon, a.lonSpeed, b.name, b.lon, b.lonSpeed);
                if (h != null) {
                    out.add(h);
                }
            }
        }
        return out;
    }

    /**
     * Aspects from each body to the four angles. The angles do not move at a planet's
     * rate, so applying is meaningless here and is left false.
     */
    public static List<Hit> toAngles(ChartFrame f) {
        String[] names = {"Ascendant", "MC", "Descendant", "IC"};
        double[] lons = {f.asc, f.mc, f.dsc, f.ic};
        List<Hit> out = new ArrayList<>();
        for (ChartFrame.Body b : f.bodies) {
            if (b == null || !b.ok) {
                continue;
            }
            for (int i = 0; i < names.length; i++) {
                Hit h = hit(b.name, b.lon, b.lonSpeed, names[i], lons[i], 0.0);
                if (h != null) {
                    out.add(h);
                }
            }
        }
        return out;
    }

    private static Hit hit(String nameA, double lonA, double speedA,
                           String nameB, double lonB, double speedB) {
        double sep = separation(lonA, lonB);
        Type t = typeOf(sep, nameA, nameB);
        if (t == null) {
            return null;
        }
        Hit h = new Hit();
        h.a = nameA;
        h.b = nameB;
        h.type = t;
        h.separation = sep;
        h.orbUsed = orbFor(nameA, nameB);
        h.offBy = Math.abs(sep - t.exactAngle);
        h.tightness = Math.max(0.0, 1.0 - h.offBy / h.orbUsed);
        h.applying = isApplying(lonA, speedA, lonB, speedB, t);
        h.partile = isPartile(sep, t);
        h.signDistance = signDistance(lonA, lonB);
        h.dissociate = t.isWholeSign() && h.signDistance != t.signSteps;
        return h;
    }
}
