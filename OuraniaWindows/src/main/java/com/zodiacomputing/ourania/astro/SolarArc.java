package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L8: solar arc directions.
 *
 * Every point in the chart advances by one arc - the distance the secondary-progressed Sun
 * has travelled from its natal place, near enough a degree a year. A body at 10 Taurus is at
 * 11 Taurus at age one. Because <i>everything</i> moves at the same rate, solar arc brings
 * pairs into exact contact that progressions never do, which is what makes it complementary
 * rather than redundant.
 *
 * The spec's warning is worth restating: contacts stay within orb for roughly two years, so
 * this technique is good at answering <b>when</b> and bad at answering <b>for how long</b>.
 * That is why what this class reports is the dated moment of exactness and not an orb.
 *
 * <h3>Why this does not scan</h3>
 *
 * The obvious implementation loops every directed body against every natal point against
 * every aspect and hands each combination to a root finder. That is about a thousand scans
 * of the window, and it is the exact shape that made the transit-perfection scan take
 * 35 seconds before it was cached.
 *
 * It is also unnecessary. The arc is a single monotonic function of time, shared by every
 * point in the chart, so instead of asking "when does this pair come together" a thousand
 * times, each pair is asked "what arc value would make you exact" - one subtraction - and
 * only the values falling inside the window's arc span are solved for. A one-year window
 * advances the arc about one degree, so almost nothing qualifies, and the handful that do
 * take one bisection each.
 *
 * That selectivity is a feature. A solar arc contact is rare, dated and specific, which is
 * exactly the kind of witness the convergence rule is short of.
 */
public final class SolarArc {

    /**
     * Days per year for the secondary progression rate.
     *
     * The tropical year, because the arc is the progressed <i>Sun's</i> travel and the Sun's
     * return to its own longitude is what defines that year. Using 365.25 here would drift
     * the arc by about a tenth of a degree over a lifetime, which is inside the orb but not
     * inside the precision this technique is prized for.
     */
    public static final double DAYS_PER_YEAR = 365.2422;

    /** A directed point reaching exactness on a natal point. */
    public static final class Contact {
        /** The natal point being directed forward by the arc. */
        public String directed;
        /** The natal point it contacts. */
        public String natal;
        public Aspects.Type type;
        /** Moment of exactness. */
        public double jd;
        /** The arc, in degrees, at that moment. */
        public double arc;
        /** Why the natal target qualified, in Transits' vocabulary. */
        public String why;
        public int natalRank = -1;

        @Override
        public String toString() {
            return String.format("directed %s %s natal %s exact (arc %.2f°)",
                directed, type.label.toLowerCase(), natal, arc);
        }
    }

    private SolarArc() { }

    /**
     * The arc at a moment: how far the secondary-progressed Sun has moved from its natal
     * longitude.
     *
     * Secondary progression is a day for a year, so the progressed moment for a target date
     * is the birth moment plus one day per elapsed year. Returns NaN if the ephemeris will
     * not answer, never zero - a zero would read as "no movement", which is a real value.
     */
    public static double arcAt(SwissEph sw, double natalJd, double natalSunLon,
                               double targetJd) {
        double years = (targetJd - natalJd) / DAYS_PER_YEAR;
        double progressedJd = natalJd + years;
        double sun = Almanac.bodyLongitude(sw, progressedJd, "Sun");
        if (Double.isNaN(sun)) {
            return Double.NaN;
        }
        return Zodiac.normalise(sun - natalSunLon);
    }

    /**
     * Directed contacts to significant natal points that perfect inside the window.
     *
     * Directs every natal body plus the Ascendant and MC. The Descendant and IC are
     * deliberately not directed: they are the same axes, so directing them would report
     * every contact a second time with the aspect flipped - "directed Descendant conjunct
     * natal X" is the same fact as "directed Ascendant opposition natal X".
     *
     * @param natalJd  birth moment, Julian day UT.
     * @param jdFrom   window start, and
     * @param jdTo     window end - the same window the other L8 techniques are measured
     *                 over, which is what makes the convergence count meaningful.
     */
    public static List<Contact> contacts(SwissEph sw, ChartFrame natal, double natalJd,
                                         List<BodyScore.Vector> ranked, String lord,
                                         double jdFrom, double jdTo, int topN) {
        List<Contact> out = new ArrayList<>();
        if (natal == null) {
            return out;
        }
        ChartFrame.Body sunBody = natal.body("Sun");
        if (sunBody == null || !sunBody.ok) {
            return out;                          // no Sun, no arc
        }
        final double natalSunLon = sunBody.lon;

        double arcFrom = arcAt(sw, natalJd, natalSunLon, jdFrom);
        double arcTo = arcAt(sw, natalJd, natalSunLon, jdTo);
        if (Double.isNaN(arcFrom) || Double.isNaN(arcTo) || arcTo <= arcFrom) {
            return out;
        }

        List<Transits.NatalTarget> targets =
            Transits.significantTargets(natal, ranked, lord, topN);

        // The points that get directed. Bodies plus the two independent angles.
        List<String> directedNames = new ArrayList<>();
        List<Double> directedLons = new ArrayList<>();
        for (int bi = 0; bi < natal.bodies.length && bi < Bodies.count(); bi++) {
            ChartFrame.Body b = natal.bodies[bi];
            // Angles are skipped here and added explicitly below. Before the Bodies
            // registry, `bodies` held twelve planets and the angles lived in their own
            // fields; on 2026-08-19 it became all 28 points and this loop silently began
            // picking them up. The Ascendant and MC then arrived TWICE, and the Descendant
            // and IC arrived at all - which is the same axis reported a second time with
            // the aspect flipped, exactly what the comment below says must not happen.
            if (b != null && b.ok && !Bodies.at(bi).isAngle()) {
                directedNames.add(b.name);
                directedLons.add(b.lon);
            }
        }
        directedNames.add("Ascendant");
        directedLons.add(natal.asc);
        directedNames.add("MC");
        directedLons.add(natal.mc);

        for (int d = 0; d < directedNames.size(); d++) {
            double lb = directedLons.get(d);
            for (Transits.NatalTarget target : targets) {
                // Directing a point onto itself is not an event, it is the natal chart.
                if (directedNames.get(d).equals(target.name)) {
                    continue;
                }
                for (Aspects.Type type : Aspects.Type.values()) {
                    for (double required : requiredArcs(lb, target.lon, type)) {
                        if (required < arcFrom || required > arcTo) {
                            continue;            // the arc does not reach it this window
                        }
                        double jd = solveArc(sw, natalJd, natalSunLon, required,
                            jdFrom, jdTo);
                        if (Double.isNaN(jd)) {
                            continue;
                        }
                        Contact c = new Contact();
                        c.directed = directedNames.get(d);
                        c.natal = target.name;
                        c.type = type;
                        c.jd = jd;
                        c.arc = required;
                        c.why = target.why;
                        c.natalRank = target.rank;
                        out.add(c);
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble((Contact c) -> c.jd).thenComparing(c -> c.natal));
        return out;
    }

    /** Convenience overload with the standard filter settings. */
    public static List<Contact> contacts(SwissEph sw, ChartFrame natal, double natalJd,
                                         List<BodyScore.Vector> ranked, String lord,
                                         double jdFrom, double jdTo) {
        return contacts(sw, natal, natalJd, ranked, lord, jdFrom, jdTo,
            Transits.defaultTopN);
    }

    /**
     * The arc values that would put a directed point exactly on an aspect to a natal point.
     *
     * A square perfects at +90 and at -90 and they are different arcs reached years apart,
     * so both are returned. A conjunction and an opposition have one each: 0 and -0 are the
     * same angle, as are 180 and -180, and returning both would report every one of them
     * twice.
     */
    private static double[] requiredArcs(double directedNatalLon, double targetLon,
                                         Aspects.Type type) {
        double a = Zodiac.normalise(targetLon + type.exactAngle - directedNatalLon);
        if (type.exactAngle == 0.0 || type.exactAngle == 180.0) {
            return new double[]{a};
        }
        double b = Zodiac.normalise(targetLon - type.exactAngle - directedNatalLon);
        return new double[]{a, b};
    }

    /**
     * The moment in the window at which the arc reaches a given value.
     *
     * The arc is monotonic - the Sun never turns back - so this is a plain bisection with no
     * scanning. Reuses the Almanac bisector rather than writing a second one.
     */
    private static double solveArc(SwissEph sw, double natalJd, double natalSunLon,
                                   double wanted, double jdFrom, double jdTo) {
        Almanac.OfTime f = jd -> arcAt(sw, natalJd, natalSunLon, jd) - wanted;
        double lo = f.at(jdFrom);
        double hi = f.at(jdTo);
        if (Double.isNaN(lo) || Double.isNaN(hi) || (lo < 0) == (hi < 0)) {
            return Double.NaN;                   // not bracketed, nothing to refine
        }
        return Almanac.bisect(f, jdFrom, jdTo);
    }
}
