package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L8: solar arc directions onto natal midpoints - the cosmobiology layer.
 *
 * <p><b>David, 2026-10-10, specifying this layer:</b> a directed planet aspecting a natal
 * <i>planet</i> describes a two-year chapter; a directed planet contacting a natal
 * <i>midpoint</i> compresses it into a short, dated event. The notation is Ebertin's and is
 * written {@code SA Body = A/B}: the directed planet is the active catalyst, the natal midpoint
 * the fused matrix of those two natal bodies. SA Uranus = Sun/Moon is a re-orientation; SA Pluto
 * = Sun/Saturn is structural pressure.
 *
 * <h3>Why there is no aspect loop in this file</h3>
 *
 * <p>The kinetic family - conjunction, semisquare, square, sesquiquadrate, opposition - is every
 * multiple of 45 degrees, which is the eighth harmonic of the circle. Folded modulo
 * {@link #HARMONIC} all five become <b>one equality</b>. So where {@link SolarArc} asks five
 * aspect types in two directions per pair, this asks one question per pair: what arc puts the
 * directed body on this midpoint, in eighth-harmonic space.
 *
 * <p>And that question is a subtraction. The arc is one monotonic function of time shared by every
 * point in the chart, so for a directed body at natal longitude {@code d} and a midpoint at
 * {@code m} the contact perfects at every arc congruent to {@code m - d} modulo 45. A one-year
 * window advances the arc by about one degree, so of the 45 degrees between consecutive
 * perfections almost none qualify - and the handful that do take one bisection each. This is the
 * same trick {@link SolarArc} uses, for the same reason: the thousand-scan version is what made
 * the transit perfection scan take 35 seconds before it was cached.
 *
 * <p><b>45 and not 90, which the spec got wrong and which would have failed silently.</b> The
 * spec says to reduce modulo 90, on the grounds that all five aspects then coincide. They do not:
 * 0, 90 and 180 fold to 0, but 45 and 135 fold to 45. A scan stepping the arc by 90 therefore
 * finds every conjunction, square and opposition and <b>misses every semisquare and
 * sesquiquadrate</b> - half the kinetic family, with nothing to report it, because the contacts it
 * does find are all real. The 90 degree <i>dial</i> is still the right instrument to read on, and
 * a practitioner watches both of its marks; the congruence is modulo 45. {@link ArcMidpointCheck}
 * Part A pins the fold of all five aspects rather than trusting this paragraph.
 *
 * <p>The soft aspects need no filter: 60 and 120 both fold to 30 in 90-degree space and to 15 in
 * 45-degree space, so they are nowhere near either mark and never arise.
 *
 * <h3>The orb, and a correction to the spec's duration</h3>
 *
 * <p>The spec gives a cap of <b>&plusmn;0.25 degrees</b> of arc with the weight
 * {@code W = max(0, 1 - (|dTheta| / 0.25)^2)}, a peak zone of &plusmn;0.05, and an active
 * duration of "about six weeks". <b>The six weeks is the peak zone, not the cap.</b> The arc
 * advances at the progressed Sun's rate, near enough one degree a year, so:
 *
 * <ul>
 *   <li>&plusmn;0.05 degrees spans 0.1 degrees of arc: 37 days, which is the six weeks.</li>
 *   <li>&plusmn;0.25 degrees spans 0.5 degrees of arc: 185 days, about six months.</li>
 * </ul>
 *
 * <p>Both are kept because they say different things - {@link #ORB} is where a contact begins to
 * count and {@link #PEAK} is its climax - but this class reports dates it computes rather than a
 * duration asserted in advance, because the rate is not uniform: the Sun moves 0.953 to 1.019
 * degrees a day through the year, so one orb is a different number of weeks depending on the
 * birthday.
 *
 * <h3>What this does not do</h3>
 *
 * <p>No prose. {@link Contact#picture()} writes the classical notation and nothing interprets it
 * yet; the catalogue of {@code SA Body = A/B} readings does not exist. And no convergence: these
 * are witnesses, and weighing them against transits and profections is {@link Convergence}'s job.
 */
public final class ArcMidpoints {

    private ArcMidpoints() { }

    /**
     * The dial these contacts are conventionally read on, in degrees per turn.
     *
     * Not used to find them - see {@link #HARMONIC} - but it is the number a reader recognises,
     * and {@link Dial#MODULI} offers it as the first choice.
     */
    public static final double DIAL = 90.0;

    /** The eighth harmonic: the modulus in which all five kinetic aspects are one contact. */
    public static final double HARMONIC = 45.0;

    /** Where a contact begins to count, in degrees of arc. The spec's cap. */
    public static final double ORB = 0.25;

    /** The climax zone, in degrees of arc: inside this the contact is at full strength. */
    public static final double PEAK = 0.05;

    /** How far either side of the window to look when dating the orb's edges, in days. */
    private static final double EDGE_PAD_DAYS = 400.0;

    /** A directed body reaching a natal midpoint in eighth-harmonic space. */
    public static final class Contact {

        /** The natal point being directed forward by the arc. */
        public String directed;

        /** The first half of the natal midpoint. */
        public String a;

        /** The second half. */
        public String b;

        /** The natal midpoint's longitude, as a midpoint tree would give it. */
        public double midpoint;

        /** When the contact is exact, Julian day UT. */
        public double jd;

        /** When it enters {@link #ORB}, or NaN when that falls outside the searched span. */
        public double jdFrom = Double.NaN;

        /** When it leaves, or NaN likewise. */
        public double jdTo = Double.NaN;

        /** The arc at exactness, in degrees. */
        public double arc;

        /** The classical notation: {@code SA Uranus = Sun/Moon}. */
        public String picture() {
            return "SA " + this.directed + " = " + this.a + "/" + this.b;
        }

        @Override
        public String toString() {
            return String.format("%s exact (arc %.2f°)", this.picture(), this.arc);
        }
    }

    /**
     * The spec's decay weight: 1.0 at exactness, 0.0 at the orb, quadratic between.
     *
     * <p>Quadratic and not linear, so the weight falls away slowly near exactness and sharply at
     * the edge - which is the shape a reader of these contacts describes, and the reason
     * {@link #PEAK} is worth naming rather than being a second threshold.
     *
     * @param offset degrees of arc from exactness, either sign
     */
    public static double weight(double offset) {
        double t = Math.abs(offset) / ORB;
        return t >= 1.0 ? 0.0 : 1.0 - t * t;
    }

    /**
     * Whether a point belongs in this layer's dial.
     *
     * <p><b>Ebertin's set, which is narrower than this app's body list, and the reason is
     * arithmetic.</b> Every point added grows the midpoint matrix quadratically and the contact
     * rate with it. The thirteen points below - the two luminaries, the eight planets, the North
     * Node, the Ascendant and the MC - give 78 midpoints and about 22 contacts a year. Adding the
     * five asteroids takes it to 18 points, 153 midpoints and about 43 a year, and the nine
     * calculated lots would roughly double it again. At that density a "rare, dated and specific"
     * witness is neither rare nor specific, and the convergence rule it feeds would be counting
     * noise.
     *
     * <p>The derived half of each axis is already gone before this is asked: {@link Dial#points}
     * drops the Descendant, the IC and the South Node because each sits on its partner on every
     * one of these dials.
     */
    private static boolean inDial(String name) {
        int n = Bodies.indexOfName(name);
        if (n < 0) {
            return false;
        }
        Bodies.Kind kind = Bodies.at(n).kind;
        return kind == Bodies.Kind.LUMINARY || kind == Bodies.Kind.PLANET
            || kind == Bodies.Kind.NODE || kind == Bodies.Kind.ANGLE;
    }

    /** The natal points this layer directs and builds midpoints from. */
    public static List<Dial.Point> dialPoints(ChartFrame natal) {
        List<Dial.Point> out = new ArrayList<>();
        for (Dial.Point p : Dial.points(natal)) {
            if (inDial(p.name)) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Directed bodies contacting natal midpoints, perfecting inside the window.
     *
     * <p><b>A directed body may contact a midpoint it is itself half of.</b> SA Sun = Sun/Moon is
     * a real picture and perfects at a real arc - half the natal Sun-Moon distance - so it is not
     * excluded. The only thing skipped is a body onto its own midpoint with itself, which cannot
     * arise because {@link Dial#midpoints} pairs distinct points.
     *
     * @param natal    the birth chart, stationary
     * @param natalJd  birth moment, Julian day UT
     * @param jdFrom   window start
     * @param jdTo     window end
     * @return contacts in date order; empty when the chart or its Sun is unavailable
     */
    public static List<Contact> contacts(SwissEph sw, ChartFrame natal, double natalJd,
                                         double jdFrom, double jdTo) {
        List<Contact> out = new ArrayList<>();
        if (sw == null || natal == null || jdTo <= jdFrom) {
            return out;
        }
        ChartFrame.Body sun = natal.body("Sun");
        if (sun == null || !sun.ok || Double.isNaN(sun.lon)) {
            return out;                          // no Sun, no arc
        }
        double natalSunLon = sun.lon;

        double arcFrom = SolarArc.arcAt(sw, natalJd, natalSunLon, jdFrom);
        double arcTo = SolarArc.arcAt(sw, natalJd, natalSunLon, jdTo);
        if (Double.isNaN(arcFrom) || Double.isNaN(arcTo) || arcTo <= arcFrom) {
            return out;
        }

        List<Dial.Point> points = dialPoints(natal);
        List<Dial.Point> mids = Dial.midpoints(points);

        for (Dial.Point directed : points) {
            for (Dial.Point mid : mids) {
                for (double required : requiredArcs(directed.lon, mid.lon, arcFrom, arcTo)) {
                    double jd = SolarArc.solveArc(sw, natalJd, natalSunLon, required,
                        jdFrom, jdTo);
                    if (Double.isNaN(jd)) {
                        continue;
                    }
                    Contact c = new Contact();
                    c.directed = directed.name;
                    c.a = mid.a;
                    c.b = mid.b;
                    c.midpoint = mid.lon;
                    c.jd = jd;
                    c.arc = required;
                    c.jdFrom = SolarArc.solveArc(sw, natalJd, natalSunLon, required - ORB,
                        jdFrom - EDGE_PAD_DAYS, jdTo + EDGE_PAD_DAYS);
                    c.jdTo = SolarArc.solveArc(sw, natalJd, natalSunLon, required + ORB,
                        jdFrom - EDGE_PAD_DAYS, jdTo + EDGE_PAD_DAYS);
                    out.add(c);
                }
            }
        }
        out.sort(Comparator.comparingDouble(c -> c.jd));
        return out;
    }

    /**
     * The arc values inside the window that put a directed point on a midpoint.
     *
     * <p><b>One congruence, not ten aspect tests.</b> The directed longitude is {@code d + arc},
     * so the contact is exact when {@code d + arc} and {@code m} agree modulo 45 - that is, at
     * every arc congruent to {@code m - d}. The first such arc at or above the window's start is
     * found directly and the rest follow every 45 degrees, so the loop body runs at most once for
     * any window shorter than forty-five years of life.
     *
     * <p>The aspect family never appears here because asking "was that a square or a
     * sesquiquadrate" is asking which multiple of 45 the arc passed, which is a label on the
     * answer rather than part of finding it.
     */
    private static double[] requiredArcs(double directedNatalLon, double midpointLon,
                                         double arcFrom, double arcTo) {
        double base = Dial.folded(midpointLon - directedNatalLon, HARMONIC);
        double k = Math.ceil((arcFrom - base) / HARMONIC);
        List<Double> hits = new ArrayList<>(1);
        for (double arc = base + k * HARMONIC; arc <= arcTo; arc += HARMONIC) {
            if (arc >= arcFrom) {
                hits.add(arc);
            }
        }
        double[] a = new double[hits.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = hits.get(i);
        }
        return a;
    }
}
