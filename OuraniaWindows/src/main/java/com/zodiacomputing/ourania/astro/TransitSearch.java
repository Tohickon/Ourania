package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * "When does Saturn square my Moon between 2026 and 2030?" - answered as passages.
 *
 * <p><b>A passage, not a date.</b> A slow planet does not aspect a natal point once. It comes
 * into orb, perfects, often stations and backs over the degree, perfects again, stations
 * direct and perfects a third time before it leaves. Three exact dates reported as three rows
 * read as three events, and the months between them - when the transit is most felt, by any
 * school that reads stations - vanish. So the unit here is the stretch of time the transit
 * spends inside the orb, carrying every exact moment and every station that falls in it.
 *
 * <p><b>The orb is the caller's, and it says so.</b> The screens pass {@link Transits#orb}, the
 * one transit orb the Report and Synthesis also use, and the reader can change it for a search.
 * It only decides where a passage begins and ends; the exact dates do not depend on it at all.
 *
 * <p><b>The same moments the rest of the app finds, found more cheaply.</b> The exact dates are
 * roots of the same function {@link Transits#exactDates} solves and the stations come from
 * {@link Almanac#stations}; TransitSearchCheck holds the exact dates to within a few seconds of
 * that method's. What differs is the route: one shared sample of each body's longitude, with
 * every crossing bracketed from it - see {@link #passages} for why, and what it cost before.
 */
public final class TransitSearch {

    private TransitSearch() { }

    /** Bodies a search offers, slowest last. The Moon is left out: it is volume, not an answer. */
    public static final String[] TRANSITING = {
        "Sun", "Mercury", "Venus", "Mars", "Jupiter", "Saturn",
        "Uranus", "Neptune", "Pluto", "Chiron", "North Node"
    };

    /** The five aspects a transit search offers by default. */
    public static final Aspects.Type[] MAJOR = {
        Aspects.Type.CONJUNCTION, Aspects.Type.SEXTILE, Aspects.Type.SQUARE,
        Aspects.Type.TRINE, Aspects.Type.OPPOSITION
    };

    /** A moment of exactness inside a passage. */
    public static final class Exact {
        public final double jd;
        public final boolean retrograde;

        Exact(double jd, boolean retrograde) {
            this.jd = jd;
            this.retrograde = retrograde;
        }
    }

    /** A station of the transiting body while it is in orb. */
    public static final class Station {
        public final double jd;
        /** True for turning retrograde, false for turning direct. */
        public final boolean retrograde;
        public final double longitude;

        Station(double jd, boolean retrograde, double longitude) {
            this.jd = jd;
            this.retrograde = retrograde;
            this.longitude = longitude;
        }
    }

    /** One stretch of time a transit spends within orb of a natal point. */
    public static final class Passage {
        public String transiting;
        public String natal;
        public double natalLongitude;
        public Aspects.Type type;
        /** When it came within orb; NaN when it was already in orb before the search could see. */
        public double enters;
        /** When it left orb; NaN when it was still in orb after the search could see. */
        public double leaves;
        public final List<Exact> exacts = new ArrayList<>();
        public final List<Station> stations = new ArrayList<>();

        /** The first exact moment, or the entry when the passage never perfects in view. */
        public double firstMoment() {
            if (!exacts.isEmpty()) {
                return exacts.get(0).jd;
            }
            return Double.isNaN(enters) ? leaves : enters;
        }

        @Override
        public String toString() {
            return transiting + " " + type.label.toLowerCase() + " natal " + natal
                + " (" + exacts.size() + " exact, " + stations.size() + " station"
                + (stations.size() == 1 ? "" : "s") + ")";
        }
    }

    /**
     * The passages a reader asking about these years means: those that go exact inside them,
     * and those that come within orb inside them and never go exact at all.
     *
     * <b>Not the same as overlapping the window, which {@link #search} returns.</b> Rendered for
     * 2026-2030 on David's chart, the first seven rows were passages that perfected only in 2025
     * and were still in orb in January - true, and not an answer to the question asked. The
     * search keeps them so a check can prove nothing is missed; this is what a person reads.
     */
    public static List<Passage> perfectingIn(List<Passage> passages, double jdFrom, double jdTo) {
        List<Passage> out = new ArrayList<>();
        for (Passage p : passages) {
            boolean inside = p.exacts.isEmpty();
            for (Exact e : p.exacts) {
                inside |= e.jd >= jdFrom && e.jd < jdTo;
            }
            if (inside) {
                out.add(p);
            }
        }
        return out;
    }

    /** The largest daily motion each body reaches, so a step can never jump a whole passage. */
    static double maxSpeed(String body) {
        switch (body) {
            case "Mercury":    return 2.3;
            case "Venus":      return 1.3;
            case "Sun":        return 1.02;
            case "Mars":       return 0.8;
            case "Jupiter":    return 0.25;
            case "North Node": return 0.25;
            case "Chiron":     return 0.16;
            case "Saturn":     return 0.14;
            case "Uranus":     return 0.07;
            default:           return 0.05;
        }
    }

    /**
     * How far a passage can reach outside the window, in days.
     *
     * Wide enough for the longest retrograde passage of the body, so a passage the window cuts
     * through is found whole rather than as a fragment - with its entry and first exact
     * reported even though they fall before the window opens.
     */
    static double reach(String body) {
        switch (body) {
            case "Sun": case "Mercury": case "Venus": return 120.0;
            case "Mars":                              return 240.0;
            default:                                  return 400.0;
        }
    }

    /**
     * Every passage in the window.
     *
     * @param natal       the chart whose points are the targets; only points it can place are
     *                    searched, so a chart with no birth time offers no angles
     * @param transiting  body names from {@link #TRANSITING}
     * @param natalPoints natal point names, or null for every point the chart places
     * @param types       aspects to search
     * @param orb         degrees either side of exact that count as in orb; must be positive
     * @param jdFrom      window start
     * @param jdTo        window end
     * @return passages that overlap the window, in order of their first exact moment
     */
    public static List<Passage> search(SwissEph sw, ChartFrame natal,
                                       Collection<String> transiting,
                                       Collection<String> natalPoints,
                                       Collection<Aspects.Type> types,
                                       double orb, double jdFrom, double jdTo) {
        if (orb <= 0.0 || jdTo <= jdFrom) {
            throw new IllegalArgumentException("need a positive orb and a window that runs forward");
        }
        List<Passage> out = new ArrayList<>();
        for (String body : transiting) {
            if (Almanac.iplOf(body) < 0) {
                continue;
            }
            double lo = jdFrom - reach(body);
            double hi = jdTo + reach(body);
            // Under half the time the fastest motion takes to cross the whole orb, capped: at
            // least two samples always land inside any passage, so none can open and close
            // between them unseen.
            double step = Math.min(4.0, 0.9 * orb / maxSpeed(body));
            // Longitudes along the scan are shared by every target and aspect for this body.
            double[] grid = sampleGrid(sw, body, lo, hi, step);
            List<Station> stations = stationsOnGrid(sw, body, grid, lo, step);

            for (ChartFrame.Body target : natal.bodies) {
                if (target == null || !target.ok) {
                    continue;
                }
                if (natalPoints != null && !natalPoints.contains(target.name)) {
                    continue;
                }
                // A body aspecting its own natal place is kept: Saturn conjunct natal Saturn is
                // the Saturn return, which is among the first things anyone searches for.
                for (Aspects.Type type : types) {
                    out.addAll(passages(sw, body, target.name, target.lon, type, orb,
                        lo, hi, step, grid, stations, jdFrom, jdTo));
                }
            }
        }
        out.sort(Comparator.comparingDouble(Passage::firstMoment)
            .thenComparing(p -> p.transiting).thenComparing(p -> p.natal));
        return out;
    }

    private static double[] sampleGrid(SwissEph sw, String body, double lo, double hi,
                                       double step) {
        int n = (int) Math.floor((hi - lo) / step) + 1;
        double[] lon = new double[n];
        for (int i = 0; i < n; i++) {
            lon[i] = Almanac.bodyLongitude(sw, lo + i * step, body);
        }
        return lon;
    }

    /** Degrees from exact, over both sides of the zodiac for the aspects that have two. */
    static double offBy(double transitLon, double natalLon, Aspects.Type type) {
        double sep = Almanac.signedDelta(transitLon, natalLon);
        double off = Math.abs(Almanac.signedDelta(sep, type.exactAngle));
        if (type.exactAngle != 0.0 && type.exactAngle != 180.0) {
            off = Math.min(off, Math.abs(Almanac.signedDelta(sep, -type.exactAngle)));
        }
        return off;
    }

    /** Signed degrees from one side of an aspect: the side is +angle or -angle. */
    static double signedOff(double transitLon, double natalLon, double side) {
        return Almanac.signedDelta(Almanac.signedDelta(transitLon, natalLon), side);
    }

    /**
     * The passages for one body, one natal point and one aspect.
     *
     * <b>Worked from the shared samples; only the refinements go back to the ephemeris.</b>
     * One call costs about 0.3 ms with this machine's files - measured 298 microseconds, and 56
     * on Moshier - and the first version asked for every orb crossing and exact date to a
     * millionth of a day, which put a four-year Mercury search against every natal point at
     * 28 seconds. A passage always holds at least two samples, so the orb crossings and exact
     * moments are bracketed by samples already taken and refined with a handful of calls.
     *
     * <b>And a retrograde season is one passage.</b> A 1-degree orb is narrower than Saturn's
     * loop, so the three perfections of one season came out as three stretches in orb with
     * months outside between them. Two stretches with a station between them are one season
     * and are merged: on David's chart, Saturn conjunct natal Moon was three rows - July 2027,
     * September 2027, March 2028 - and is now one, carrying both stations.
     */
    private static List<Passage> passages(SwissEph sw, String body, String natalName,
                                          double natalLon, Aspects.Type type, double orb,
                                          double lo, double hi, double step, double[] grid,
                                          List<Station> stations, double jdFrom, double jdTo) {
        int n = grid.length;
        // Stretches of samples in orb, as [first, last] index pairs.
        List<int[]> spans = new ArrayList<>();
        int open = -1;
        for (int i = 0; i < n; i++) {
            boolean in = !Double.isNaN(grid[i]) && offBy(grid[i], natalLon, type) < orb;
            if (in && open < 0) {
                open = i;
            } else if (!in && open >= 0) {
                spans.add(new int[]{open, i - 1});
                open = -1;
            }
        }
        if (open >= 0) {
            spans.add(new int[]{open, n - 1});
        }

        // The node's true motion reverses every few weeks; those are not the stations anyone
        // means, so it neither merges seasons nor lists stations.
        boolean seasons = !body.equals("North Node");
        List<int[]> merged = new ArrayList<>();
        for (int[] span : spans) {
            if (seasons && !merged.isEmpty()) {
                int[] last = merged.get(merged.size() - 1);
                if (turnsBetween(grid, last[1], span[0])
                        && staysNear(grid, last[1], span[0], natalLon, type)) {
                    last[1] = span[1];
                    continue;
                }
            }
            merged.add(span);
        }

        List<Passage> out = new ArrayList<>();
        Almanac.OfTime outside = t -> offBy(Almanac.bodyLongitude(sw, t, body), natalLon, type) - orb;
        for (int[] span : merged) {
            int a = span[0];
            int b = span[1];
            double enters = a == 0 ? Double.NaN
                : refine(outside, lo + (a - 1) * step, lo + a * step, 1.0e-3);
            double leaves = b == n - 1 ? Double.NaN
                : refine(outside, lo + b * step, lo + (b + 1) * step, 1.0e-3);
            double start = Double.isNaN(enters) ? lo : enters;
            double end = Double.isNaN(leaves) ? hi : leaves;
            if (end < jdFrom || start > jdTo) {
                continue;
            }
            Passage p = new Passage();
            p.transiting = body;
            p.natal = natalName;
            p.natalLongitude = natalLon;
            p.type = type;
            p.enters = enters;
            p.leaves = leaves;

            // Exact moments: sign changes of the signed offset from each side of the aspect,
            // over the samples just outside the stretch as well as inside it.
            int from = Math.max(0, a - 1);
            int to = Math.min(n - 1, b + 1);
            double[] sides = type.exactAngle == 0.0 || type.exactAngle == 180.0
                ? new double[]{type.exactAngle} : new double[]{type.exactAngle, -type.exactAngle};
            for (double side : sides) {
                for (int k = from; k < to; k++) {
                    if (Double.isNaN(grid[k]) || Double.isNaN(grid[k + 1])) {
                        continue;
                    }
                    double s0 = signedOff(grid[k], natalLon, side);
                    double s1 = signedOff(grid[k + 1], natalLon, side);
                    // A sign change far from zero is the offset wrapping at 180, not a crossing.
                    if ((s0 < 0) == (s1 < 0) || Math.abs(s0 - s1) > 90.0) {
                        continue;
                    }
                    final double fs = side;
                    double jd = refine(
                        t -> signedOff(Almanac.bodyLongitude(sw, t, body), natalLon, fs),
                        lo + k * step, lo + (k + 1) * step, 1.0e-4);
                    boolean rx = Almanac.signedDelta(grid[k + 1], grid[k]) < 0.0;
                    p.exacts.add(new Exact(jd, rx));
                }
            }
            p.exacts.sort(Comparator.comparingDouble(e -> e.jd));

            if (seasons) {
                for (Station st : stations) {
                    if (st.jd >= start && st.jd <= end) {
                        p.stations.add(st);
                    }
                }
            }
            out.add(p);
        }
        return out;
    }

    /**
     * How far from exact a body may wander between two stretches that are still one season.
     *
     * A turn between two stretches is not enough on its own: Mercury turns three times a year,
     * so a turn sits between almost any two of its passages, and without this a four-year
     * search merged its 672 stretches into 98. A retrograde loop keeps the body within its own
     * width of the degree - under 20 degrees for Mercury and Mars, a few for the outers - while
     * the next pass a year later has been all the way round. Thirty separates the two with room
     * on both sides.
     */
    static final double SEASON_REACH = 30.0;

    private static boolean staysNear(double[] grid, int i, int j, double natalLon,
                                     Aspects.Type type) {
        for (int k = i; k <= j && k < grid.length; k++) {
            if (!Double.isNaN(grid[k]) && offBy(grid[k], natalLon, type) > SEASON_REACH) {
                return false;
            }
        }
        return true;
    }

    /** Every station of the body over the scan, found once and shared by every target. */
    private static List<Station> stationsOnGrid(SwissEph sw, String body, double[] grid,
                                                double lo, double step) {
        List<Station> out = new ArrayList<>();
        if (body.equals("North Node")) {
            return out;
        }
        for (int k = 0; k + 2 < grid.length; k++) {
            if (!turnsBetween(grid, k, k + 2)) {
                continue;
            }
            for (Almanac.Event e : Almanac.stations(sw, lo + k * step, lo + (k + 2) * step, body)) {
                boolean seen = false;
                for (Station st : out) {
                    seen |= Math.abs(st.jd - e.jd) < 1.0;
                }
                if (!seen) {
                    out.add(new Station(e.jd, e.kind == Almanac.Kind.STATION_RETROGRADE,
                        e.longitude));
                }
            }
        }
        out.sort(Comparator.comparingDouble(st -> st.jd));
        return out;
    }

    /** True when the body's direction of motion reverses between two sample indices. */
    private static boolean turnsBetween(double[] grid, int i, int j) {
        int dir = 0;
        for (int k = i; k < j && k + 1 < grid.length; k++) {
            if (Double.isNaN(grid[k]) || Double.isNaN(grid[k + 1])) {
                continue;
            }
            double d = Almanac.signedDelta(grid[k + 1], grid[k]);
            int now = d > 0 ? 1 : (d < 0 ? -1 : 0);
            if (now != 0 && dir != 0 && now != dir) {
                return true;
            }
            if (now != 0) {
                dir = now;
            }
        }
        return false;
    }

    /**
     * A bracketed root by regula falsi, Illinois variant: a few calls on the smooth functions
     * here, where bisection to the same width takes seventeen or more.
     */
    static double refine(Almanac.OfTime f, double a, double b, double tolDays) {
        double fa = f.at(a);
        double fb = f.at(b);
        if (fa == 0.0) {
            return a;
        }
        if (fb == 0.0) {
            return b;
        }
        if ((fa < 0) == (fb < 0)) {
            return (a + b) / 2.0;
        }
        int kept = 0;
        double c = a;
        for (int i = 0; i < 60; i++) {
            c = (a * fb - b * fa) / (fb - fa);
            double fc = f.at(c);
            if (fc == 0.0) {
                return c;
            }
            if ((fc < 0) == (fb < 0)) {
                b = c;
                fb = fc;
                if (kept == -1) {
                    fa /= 2.0;
                }
                kept = -1;
            } else {
                a = c;
                fa = fc;
                if (kept == 1) {
                    fb /= 2.0;
                }
                kept = 1;
            }
            if (Math.abs(b - a) <= tolDays) {
                return (a + b) / 2.0;
            }
        }
        return c;
    }
}
