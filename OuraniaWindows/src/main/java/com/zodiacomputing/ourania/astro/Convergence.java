package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L8: the convergence rule.
 *
 * The spec in one line: <i>confidence in a period should scale with the number of
 * independent techniques pointing at it.</i> Any single technique fires constantly - there
 * is always a transit within orb of something, always a lord of the year, and a reading
 * built on any one of them alone is the cookbook dump in temporal form. What is rare, and
 * therefore worth saying, is several unrelated methods naming the same natal point.
 *
 * So this collects every active indication, groups by the natal point it targets, and ranks
 * by how many distinct <b>technique families</b> agree. The spec is explicit that the unit
 * of counting is the family and not the indication: "two transits to the same point are one
 * witness, not two". Grouping by family gives that for free.
 *
 * <h3>Where "independent" needs more than family labels</h3>
 *
 * Counting families alone double-counts, and measurably so. A station is a transiting body
 * sitting at a degree; if that body is also within orb of the same natal point in the
 * transit list, the two are one relationship observed at two moments, not two votes. This is
 * not hypothetical - the first run of this code found transiting Pluto square natal Mars at
 * 0.17 degrees in the transit list, and Pluto stationing retrograde square natal Mars at
 * 1.27 degrees in the station list. One fact. Two families. Confidence would have been
 * inflated by exactly the sort of duplicate the work plan's recurring defect section is
 * about.
 *
 * The guard is deliberately narrow: a dated witness is discounted only when it shares
 * <b>both</b> the moving body and the aspect type with a transit to the same point. Same
 * body and same aspect is the same geometry seen twice. A different aspect from the same
 * body is a different relationship, and a solar eclipse is not the transiting Sun merely
 * because both involve the Sun.
 *
 * Discounted witnesses are kept and still printed - they carry the <i>when</i>, which the
 * transit alone does not - they just do not add to the score. Hiding them would trade one
 * kind of wrong answer for another.
 *
 * <h3>Every family is measured over the same span</h3>
 *
 * This takes {@link Transits.Perfection}, not {@link Transits.Hit}: transits enter as the
 * dated moments at which they perfect across the window, the same window the eclipses and
 * stations came from. An earlier version compared an instantaneous transit reading against a
 * year of dated events, which handed the dated families dozens of chances to agree where
 * transits got one, and a two-family score then selected eight natal points out of ten.
 *
 * Two consequences worth knowing:
 *
 * <ul>
 *   <li><b>A transit sitting within orb all year but never perfecting is not a witness.</b>
 *       That is deliberate. A slow body can be within orb of a natal point for years, and
 *       reporting it as evidence about <i>this</i> year is the failure the whole class
 *       guards against. The perfection is the event; the orb is the weather.</li>
 *   <li><b>Only bodies slower than about two years vote</b> - see
 *       {@link Transits#yearMarkers}. The Sun conjuncts every natal point once a year
 *       without fail, so as a witness it agrees with everything and distinguishes nothing.</li>
 * </ul>
 *
 * The report still shows the instantaneous transit list separately. It answers a different
 * and legitimate question - what is in orb right now - and is not what this ranks.
 */
public final class Convergence {

    /**
     * The technique families L8 currently has.
     *
     * Solar arc, progressions, returns and releasing will each add one when they exist. The
     * enum is the list of things that can vote, so adding a family here and populating it in
     * collect() is the whole integration.
     */
    public enum Family {
        PROFECTION, TRANSIT, ECLIPSE, STATION, SOLAR_ARC, PROGRESSION, RETURN
    }

    /**
     * Whether a family reports where a body physically is in the sky.
     *
     * This is what the echo guard compares against, and getting it wrong would be a quiet
     * disaster. A station and an eclipse are real positions of a real body, so they can and
     * do repeat what a transit already said. <b>A solar arc contact is symbolic</b> - a
     * directed Mars is not Mars, it is Mars's natal degree pushed forward by the Sun's
     * progressed travel - so "directed Mars conjunct natal Sun" and "transiting Mars
     * conjunct natal Sun" share a body name and an aspect while being entirely unrelated
     * events. Without this distinction the guard would have silently discounted solar arc,
     * which is the one family that fires rarely enough to be worth the most.
     *
     * Profection is symbolic for the same reason: it is arithmetic on a birthday. So is a
     * progression - a progressed Mars is Mars's position on a <i>different day</i>, read as
     * a symbol for this year, and it has no more to do with where Mars is tonight than a
     * directed Mars does.
     *
     * <b>Classify every new family here before adding it.</b> A family that is wrongly
     * called a sky position gets silently discounted whenever it shares a body name and an
     * aspect with a transit, and nothing about the output looks wrong.
     */
    private static boolean skyPosition(Family f) {
        return f == Family.TRANSIT || f == Family.ECLIPSE || f == Family.STATION
            || f == Family.RETURN;
    }

    /**
     * Set false to score by raw family count, ignoring the shared-body guard.
     *
     * Exists so the effect of the guard can be measured rather than argued about.
     */
    public static boolean discountSharedBodies = true;

    /** One technique's claim on one natal point. */
    public static final class Witness {
        public Family family;
        /** The moving body, or null for profection, which has none. */
        public String movingBody;
        /** The aspect to the natal point, or null for profection. */
        public Aspects.Type aspect;
        /** Julian day for the dated families; NaN for a condition rather than a moment. */
        public double jd = Double.NaN;
        public double offBy = Double.NaN;
        /** False when this repeats a relationship another family already reported. */
        public boolean independent = true;
        public double intensity = 1.0;
        /** What this witness is, ready to print. */
        public String detail = "";

        @Override
        public String toString() {
            return (independent ? "" : "(echo) ") + detail;
        }
    }

    /** A natal point and everything currently pointing at it. */
    public static final class Target {
        public String natal;
        /** Why the natal point matters at all, in Transits' vocabulary. */
        public String why = "";
        public final List<Witness> witnesses = new ArrayList<>();
        /** The independent families agreeing. */
        public Set<Family> families = EnumSet.noneOf(Family.class);
        public double rawScore;
        /** Softmax probability. */
        public double score;
        /** Independent witnesses, for ordering within a score. */
        public int independentWitnesses;

        @Override
        public String toString() {
            return String.format("%s: %.2f%% (%s)", natal, score * 100.0, families);
        }
    }

    private Convergence() { }

    /**
     * Every indication, grouped by natal point and ranked by independent agreement.
     *
     * @param prof         the profection, whose lord is one witness. Null to omit the family.
     * @param perfections  dated transit perfections over the window, already filtered by
     *                     natal significance. Not the instantaneous transit list - see the
     *                     class comment on why the spans have to match.
     * @param events       eclipse and station contacts over the same window.
     */
    public static List<Target> collect(Profection prof, List<Transits.Perfection> perfections,
                                       List<Transits.EventHit> events) {
        return collect(prof, perfections, events, java.util.Collections.emptyList());
    }

    /**
     * As above, with the solar arc family included.
     *
     * @param arcs directed contacts perfecting inside the same window.
     */
    public static List<Target> collect(Profection prof, List<Transits.Perfection> perfections,
                                       List<Transits.EventHit> events,
                                       List<SolarArc.Contact> arcs) {
        return collect(prof, perfections, events, arcs, java.util.Collections.emptyList());
    }

    /**
     * As above, with the secondary progression family included.
     *
     * @param progressions progressed contacts perfecting inside the same window.
     */
    public static List<Target> collect(Profection prof, List<Transits.Perfection> perfections,
                                       List<Transits.EventHit> events,
                                       List<SolarArc.Contact> arcs,
                                       List<Progressions.Contact> progressions) {
        return collect(prof, perfections, events, arcs, progressions,
            java.util.Collections.emptyList());
    }

    /**
     * As above, with the return family included.
     *
     * @param returns return-chart contacts. A return is a sky position, so most of its
     *                planetary contacts will be discounted as echoes of transits - the part
     *                that survives is the return's <i>angles</i>, which nothing else reports.
     */
    public static List<Target> collect(Profection prof, List<Transits.Perfection> perfections,
                                       List<Transits.EventHit> events,
                                       List<SolarArc.Contact> arcs,
                                       List<Progressions.Contact> progressions,
                                       List<Returns.Contact> returns) {
        return collect(prof, perfections, events, arcs, progressions, returns, null);
    }

    /**
     * As above, gated by zodiacal releasing.
     *
     * The gate arrives already read rather than being computed here, for the reason the
     * convergence ranking itself arrives already computed at the L9 renderer: this class has
     * no ephemeris and no business opening one, and a caller that already knows the chart's
     * lots can answer the question far more cheaply than a rebuild from scratch.
     *
     * A null gate means ungated, not dormant. Callers that have no lots - a composite, a chart
     * whose Sun failed to compute - get exactly the ranking they got before releasing existed
     * rather than a uniformly suppressed one.
     */
    public static List<Target> collect(Profection prof, List<Transits.Perfection> perfections,
                                       List<Transits.EventHit> events,
                                       List<SolarArc.Contact> arcs,
                                       List<Progressions.Contact> progressions,
                                       List<Returns.Contact> returns,
                                       Gate releasingGate) {
        Map<String, Target> byPoint = new LinkedHashMap<>();

        // Profection. The only family that is not a body arriving somewhere - it is
        // arithmetic on the birthday, so it is independent of the sky by construction.
        if (prof != null && prof.lord != null) {
            Witness w = new Witness();
            w.family = Family.PROFECTION;
            w.detail = String.format("lord of the %s-house profection year", ordinal(prof.house));
            target(byPoint, prof.lord, "lord of the year").witnesses.add(w);
        }

        if (perfections != null) {
            for (Transits.Perfection p : perfections) {
                Witness w = new Witness();
                w.family = Family.TRANSIT;
                w.movingBody = p.transiting;
                w.aspect = p.type;
                w.jd = p.jd;
                w.offBy = 0.0;                 // a perfection is exact by definition
                w.intensity = p.intensity;
                w.detail = String.format("transiting %s%s %s exact",
                    p.transiting, p.retrograde ? " Rx" : "", p.type.label.toLowerCase());
                target(byPoint, p.natal, p.why).witnesses.add(w);
            }
        }

        if (events != null) {
            for (Transits.EventHit h : events) {
                Family f = h.event.kind == Almanac.Kind.SOLAR_ECLIPSE
                    || h.event.kind == Almanac.Kind.LUNAR_ECLIPSE
                    ? Family.ECLIPSE : Family.STATION;
                Witness w = new Witness();
                w.family = f;
                w.movingBody = h.event.body;
                w.aspect = h.type;
                w.jd = h.event.jd;
                w.offBy = h.offBy;
                w.detail = String.format("%s — %s, %.2f° off",
                    h.event, h.type.label.toLowerCase(), h.offBy);
                target(byPoint, h.natal, h.why).witnesses.add(w);
            }
        }

        if (arcs != null) {
            for (SolarArc.Contact c : arcs) {
                Witness w = new Witness();
                w.family = Family.SOLAR_ARC;
                // The directed point's name, not a transiting body. Never compared against
                // a transit - see skyPosition - but recorded so the line reads properly.
                w.movingBody = c.directed;
                w.aspect = c.type;
                w.jd = c.jd;
                w.offBy = 0.0;
                w.detail = String.format("directed %s %s exact (arc %.2f°)",
                    c.directed, c.type.label.toLowerCase(), c.arc);
                target(byPoint, c.natal, c.why).witnesses.add(w);
            }
        }

        if (progressions != null) {
            for (Progressions.Contact c : progressions) {
                Witness w = new Witness();
                w.family = Family.PROGRESSION;
                // Symbolic, like solar arc - never compared against a transit. See
                // skyPosition; a progressed Mars is not Mars tonight.
                w.movingBody = c.progressed;
                w.aspect = c.type;
                w.jd = c.jd;
                w.offBy = 0.0;
                w.detail = String.format("progressed %s%s %s exact",
                    c.progressed, c.retrograde ? " Rx" : "", c.type.label.toLowerCase());
                target(byPoint, c.natal, c.why).witnesses.add(w);
            }
        }

        if (returns != null) {
            for (Returns.Contact c : returns) {
                Witness w = new Witness();
                w.family = Family.RETURN;
                // A return chart's Mars IS Mars, so this name is compared against transits
                // and will often be discounted. The angles carry no body name a transit can
                // match, which is why they survive - and they are the new information.
                w.movingBody = c.returnPoint;
                w.aspect = c.type;
                w.jd = c.jd;
                w.offBy = c.offBy;
                w.detail = String.format("%s return %s %s, %.2f° off",
                    c.kind, c.returnPoint, c.type.label.toLowerCase(), c.offBy);
                target(byPoint, c.natal, c.why).witnesses.add(w);
            }
        }

        List<Target> out = new ArrayList<>(byPoint.values());
        String lordOfTheYear = prof != null ? prof.lord : null;
        Gate gate = releasingGate == null ? Gate.open() : releasingGate;
        for (Target t : out) {
            score(t, lordOfTheYear, gate);
        }
        
        // <b>Share of the loudest, not share of the total.</b>
        //
        // This was a softmax, and a softmax answers a different question than the one the
        // report asks. It distributes one unit across the targets, so a target's number moves
        // when OTHER targets change: adding a twelfth target lowers everything else's
        // "intensity" although nothing about them got weaker. And because the exponent is of
        // an unbounded sum - rawScore adds a term per witness, with the lord of the year
        // multiplied - the spread scales with how many witnesses a chart happens to have.
        // Measured: on one chart the loudest read 50.5% and the quietest 0.6%; on another,
        // with more witnesses and a wider spread, the loudest read 100.0% and every one of
        // the other ten read 0.0% while listing twenty triggers each. exp(-10) is 4.5e-5, so
        // the display rounded a real, well-witnessed target to nothing.
        //
        // Dividing by the loudest says what a reader takes "intensity" to mean: how loud this
        // is against the loudest thing in the year. It is stable when the field changes, it
        // cannot round to zero unless the raw score is zero, and being a monotone transform of
        // rawScore it leaves the sort below and loudest() behaving exactly as before.
        if (!out.isEmpty()) {
            double maxRaw = 0.0;
            for (Target t : out) {
                maxRaw = Math.max(maxRaw, t.rawScore);
            }
            for (Target t : out) {
                t.score = maxRaw > 0.0 ? t.rawScore / maxRaw : 0.0;
            }
        }
        
        // Loudest first
        out.sort(Comparator.comparingDouble((Target t) -> -t.score)
            .thenComparingInt(t -> -t.independentWitnesses)
            .thenComparing(t -> t.natal));
        return out;
    }

    /**
     * Marks echoes and counts the independent families.
     *
     * A dated witness echoes a transit when it shares the moving body AND the aspect with a
     * transit to this same point. Note the transit family itself is never discounted: it is
     * the baseline the others are compared against, and discounting both halves of a pair
     * would remove the fact entirely.
     */
    private static void score(Target t, String lordOfTheYear, Gate gate) {
        if (discountSharedBodies) {
            for (Witness w : t.witnesses) {
                // Only a real sky position can echo a transit, and the transit family is
                // the baseline rather than a candidate for discounting.
                if (w.family == Family.TRANSIT || !skyPosition(w.family)) {
                    continue;
                }
                for (Witness other : t.witnesses) {
                    if (other.family == Family.TRANSIT
                            && other.aspect == w.aspect
                            && other.movingBody != null
                            && other.movingBody.equals(w.movingBody)) {
                        w.independent = false;
                        break;
                    }
                }
            }
        }
        t.families = EnumSet.noneOf(Family.class);
        t.independentWitnesses = 0;
        t.rawScore = 0.0;
        for (Witness w : t.witnesses) {
            if (!w.independent) {
                continue;
            }
            t.families.add(w.family);
            t.independentWitnesses++;
            
            // Weighted by what the witness actually is, not counted flat. See the helpers
            // below and DECISIONS.md, K8.
            double lordFactor = (lordOfTheYear != null && lordOfTheYear.equals(w.movingBody))
                ? 3.5 : 1.0;
            t.rawScore += w.intensity
                * bodyWeight(w.movingBody)
                * bodyWeight(t.natal)
                * aspectWeight(w.aspect)
                * precision(w)
                * lordFactor
                * gate.multiplierFor(w.movingBody);
        }
    }

    /**
     * Zodiacal releasing as a gate on the whole score, rather than one more voter.
     *
     * <b>This is the half of K8 that unblocks releasing itself.</b> Releasing was deliberately
     * kept out of the convergence ranking because adding it as another family made
     * discrimination worse - which is the same complaint that produced the weighting above.
     * As a multiplier it does not compete with the other families, it conditions them.
     *
     * The Hellenistic claim it encodes: a transit is dormant until its time-lord is awake. A
     * hard Saturn transit during a period Saturn does not rule passes with far less to show
     * for itself than the same transit while Saturn holds the year.
     *
     * <b>Peak and bond scale everything at that moment equally, and that is deliberate.</b>
     * Within one instant a uniform factor cannot change an ordering - it is there for
     * comparing one date against another along a timeline, which is what the calendar and the
     * predictor do. The part that discriminates inside a single reading is the lord test.
     */
    public static final class Gate {
        /** Domicile ruler of the active general period, or null when nothing is released. */
        public final String l1Lord;
        /** Domicile ruler of the active sub-period. */
        public final String l2Lord;
        /** The active chain reaches a peak period - angular to the Lot of Spirit. */
        public final boolean peak;
        /** The active chain includes a period that follows a loosing of the bond. */
        public final boolean afterBond;

        public Gate(String l1Lord, String l2Lord, boolean peak, boolean afterBond) {
            this.l1Lord = l1Lord;
            this.l2Lord = l2Lord;
            this.peak = peak;
            this.afterBond = afterBond;
        }

        /** Nothing released, so nothing is gated: every witness keeps its own weight. */
        public static Gate open() {
            return new Gate(null, null, false, false);
        }

        /**
         * Reads the gate off a chart at a moment.
         *
         * Fortune is the lot released from, because Fortune is the chain about circumstance
         * and body, which is what a transit lands on. Spirit supplies the angle the peak test
         * is measured against, which is why both longitudes go in.
         */
        public static Gate at(double natalJd, double lotLon, double spiritLon, double jd) {
            if (Double.isNaN(lotLon) || Double.isNaN(spiritLon) || Double.isNaN(jd)) {
                return open();
            }
            List<ZodiacalReleasing.Period> top;
            try {
                top = ZodiacalReleasing.release(natalJd, lotLon, spiritLon,
                    jd + ZodiacalReleasing.DAYS_PER_YEAR, 4);
            } catch (RuntimeException e) {
                return open();
            }
            List<ZodiacalReleasing.Period> chain = ZodiacalReleasing.activeChain(top, jd);
            if (chain.isEmpty()) {
                return open();
            }
            String l1 = Dignity.domicileRulerOf(chain.get(0).sign);
            String l2 = chain.size() > 1 ? Dignity.domicileRulerOf(chain.get(1).sign) : null;
            boolean peak = false;
            boolean bond = false;
            for (ZodiacalReleasing.Period p : chain) {
                peak |= p.peak;
                bond |= p.afterBond;
            }
            return new Gate(l1, l2, peak, bond);
        }

        /**
         * What this moment does to a witness moved by {@code body}.
         *
         * The lord tests are taken at their strongest rather than multiplied together: a
         * planet ruling both the general period and the sub-period is emphatically awake, not
         * six times awake, and stacking them would let one body swamp a whole reading.
         */
        public double multiplierFor(String body) {
            double m;
            if (body != null && body.equalsIgnoreCase(this.l1Lord)) {
                m = 3.0;
            } else if (body != null && body.equalsIgnoreCase(this.l2Lord)) {
                m = 2.0;
            } else if (this.l1Lord == null) {
                m = 1.0;        // nothing released: do not punish what was never gated
            } else {
                m = 0.3;        // dormant
            }
            if (this.afterBond) {
                m *= 4.0;
            } else if (this.peak) {
                m *= 2.5;
            }
            return m;
        }
    }

    // ------------------------------------------------- the hierarchy (DECISIONS.md, K8)

    /**
     * What a witness is worth before the year's lord and the orb are taken into account.
     *
     * <b>Flat counting was the defect.</b> Every witness used to contribute the same
     * {@code intensity x 3.5-or-0.1}, so transiting Ceres semisquare the South Node scored
     * exactly what transiting Saturn conjunct the Sun scored. With 29 bodies and fifteen
     * aspect types every point in the chart collects several such witnesses on any given day,
     * which is why adding voters made the ranking worse instead of better: the noise floor
     * rose faster than the signal.
     *
     * Weighting by body and by aspect fixes that arithmetically rather than by taste. A minor
     * aspect between two asteroids is worth 0.5 x 0.3 = 0.15 against a conjunction of a light
     * to an angle at 5.0 x 3.0 = 15.0, a hundredfold gap, so background contacts can pile up
     * indefinitely without ever summing to one real alignment.
     *
     * <b>Both ends are weighted, and the first attempt weighted only one.</b> Scoring the
     * moving body alone left the natal target unranked, and since rawScore sums over witnesses
     * a minor point simply aspected by many things climbed: on a test chart that put Chiron
     * second and Eris fourth, above the Ascendant. The decision's own worked example
     * multiplies both ends - transiting Mars at 3.0 onto a natal Ascendant at 5.0 - which is
     * what makes a busy asteroid stay quiet.
     */
    public static double bodyWeight(String body) {
        if (body == null) {
            return 1.0;
        }
        String b = body.toLowerCase();
        switch (b) {
            case "sun": case "moon":
            case "ascendant": case "descendant": case "mc": case "ic":
                return 5.0;
            case "mercury": case "venus": case "mars":
                return 3.0;
            case "jupiter": case "saturn":
                return 2.0;
            case "uranus": case "neptune": case "pluto":
                return 1.5;
            case "chiron": case "ceres": case "juno": case "pallas": case "vesta":
                return 0.5;
            default:
                // Lilith, the nodes, the lots, and the outer asteroids. Kept above zero so a
                // genuine pile-up still registers, low enough that it cannot lead.
                return 0.2;
        }
    }

    /** What the geometry is worth. Conjunction and opposition lead; the minors barely count. */
    public static double aspectWeight(Aspects.Type type) {
        if (type == null) {
            return 1.0;     // profection is a condition, not an aspect
        }
        switch (type) {
            case CONJUNCTION: case OPPOSITION:
                return 3.0;
            case SQUARE: case TRINE:
                return 2.0;
            case SEXTILE:
                return 1.0;
            default:
                return 0.3;
        }
    }

    /**
     * How much of the aspect's allowance is left, from 1.0 at exact to 0.0 at the edge.
     *
     * This is what stops a minor aspect sitting at the very edge of its 1.0 degree cap from
     * counting as much as one that is exact. A witness with no recorded orb - profection has
     * none - keeps its full value rather than being punished for a measurement that does not
     * apply to it.
     */
    private static double precision(Witness w) {
        if (w.aspect == null || Double.isNaN(w.offBy)) {
            return 1.0;
        }
        double max = w.aspect.maxOrb;
        if (max <= 0.0) {
            return 1.0;
        }
        double p = 1.0 - (Math.abs(w.offBy) / max);
        return p < 0.0 ? 0.0 : p > 1.0 ? 1.0 : p;
    }

    private static Target target(Map<String, Target> byPoint, String natal, String why) {
        Target t = byPoint.computeIfAbsent(natal, k -> {
            Target n = new Target();
            n.natal = k;
            return n;
        });
        // First non-empty reason wins, matching how Transits labels the most specific one.
        if (t.why.isEmpty() && why != null) {
            t.why = why;
        }
        return t;
    }

    /**
     * Targets with at least n independent families.
     *
     * Parameterised rather than fixed at two on purpose. <b>Two remains a poor threshold</b>
     * even after the span fix, and the measurement is worth recording because it contradicts
     * the prediction that motivated the fix: matching the spans took the >=2 selection from
     * eight targets out of ten to <i>nine</i> out of ten. It did not make a score of two
     * mean more.
     *
     * What matching the spans did do is grade the ranking. The score histogram went from
     * {1:2, 2:7, 4:1} - one standout above a flat mass - to {1:1, 2:5, 3:3, 4:1}, which has
     * a real middle tier. So the fix improved the <i>ordering</i>, which is what the spec
     * actually asks for ("rank by"), and left the threshold as unhelpful as it was.
     *
     * Prefer {@link #loudest}. Use this only when a cut is genuinely wanted.
     *
     * <b>THE PARAMETER IS AN int, AND THAT IS THE FIX.</b> This took a {@code double
     * minScore} and compared it against {@link Target#score}. That was fine while score was
     * an integer family count, and became a silent no-op the moment score turned into a
     * softmax probability in [0,1]: every call site passes a family count, so
     * {@code atLeast(targets, 2)} was asking which targets held 200% of the probability
     * mass. The answer is always none.
     *
     * Nothing failed. Two assertions in TransitCheck went on comparing empty lists to each
     * other and passing - "atLeast(n) is monotonic" comparing zeroes, and "threshold at 2
     * is selective" reading {@code 0 < 10}. Both had been real checks the day before.
     *
     * So this counts {@link Target#families}, which is what "n independent families" meant
     * all along, and takes an int so that handing it a probability is a compile error
     * rather than an empty list. <b>A unit mismatch that the type system can express is a
     * unit mismatch that will eventually happen.</b>
     *
     * A share-based cut is newly meaningful now that scores are a normalised distribution -
     * "the targets holding the top 80% of the mass" is a sensible question the integer form
     * cannot ask. That is a separate method with a separate name, and it is deliberately
     * not written until something needs it; overloading this one on parameter type is how
     * the confusion above would come straight back.
     */
    public static List<Target> atLeast(List<Target> all, int minFamilies) {
        List<Target> out = new ArrayList<>();
        for (Target t : all) {
            if (t.families.size() >= minFamilies) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * The targets tying for the highest agreement - what the rule actually points at.
     *
     * On the measured chart this is one point scoring four against a field of twos, which is
     * the discrimination the spec is after and the threshold form does not give.
     */
    public static List<Target> loudest(List<Target> all) {
        List<Target> out = new ArrayList<>();
        double best = 0.0;
        for (Target t : all) {
            best = Math.max(best, t.score);
        }
        for (Target t : all) {
            // within a small epsilon to account for floating point
            if (Math.abs(t.score - best) < 1e-6) {
                out.add(t);
            }
        }
        return out;
    }

    /** Delegates: one spelling of this, in Zodiac. */
    private static String ordinal(int n) {
        return Zodiac.ordinal(n);
    }
}
