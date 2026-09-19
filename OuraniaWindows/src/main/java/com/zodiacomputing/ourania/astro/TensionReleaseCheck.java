package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Guards {@link TensionRelease}: framework item 6.3, where a chart's hard configurations
 * discharge.
 *
 * The feature computes nothing new about the sky - every aspect it reads was already in the
 * hit list - so the claims worth asserting are all structural: that an outlet really is a
 * soft aspect from outside the configuration to a member of it, that the empty leg really is
 * the degree opposite the apex, that nothing is read twice, and that every state the model
 * can be in has a sentence for it.
 *
 * <b>The last of those is why this suite exists at all.</b> The quincunx was a live aspect
 * in the model with an empty glyph in the view for months, and a Global Chart Dynamics
 * trinity could have shipped reading "weighted toward the houses of x - null". A synthesis
 * pass is a branch table; an unwritten branch is a sentence with a hole in it.
 *
 * Run:
 *   java -cp src\main\java com.zodiacomputing.ourania.astro.TensionReleaseCheck
 */
public final class TensionReleaseCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    /** 1990-03-14 Los Angeles, the chart the rest of the project measures against. */
    private static final double NATAL_JD = 2447964.60417;
    private static final double LAT = 34.05;
    private static final double LON = -118.24;

    /**
     * 300 charts, and the size has been both up and down for measured reasons.
     *
     * Every assertion here is structural, and originally a chart carried 11.8 configurations
     * with several outlets each, so the count ran away with the corpus: 300 charts asserted
     * 247,241 times and 100 asserted 83,602, half of every check in the tree. It was cut to
     * 40 for that reason - the handover's total is the one number two agents who never talk
     * use to notice each other's damage, and a suite that dominates it dilutes the signal.
     *
     * <b>Then the population changed and the same 40 charts yielded 1,658 checks.</b>
     * Patterns moved to physical bodies at a tight orb, configurations fell from 12.3 per
     * chart to 1.49, and the corpus that had been too large became too small to meet a
     * Boomerang or a Grand sextile at all. Raised to 300 for coverage, not for the number:
     * it now costs about a tenth of what it did at that size.
     *
     * <b>The lesson is that a corpus size is not a constant.</b> It is a function of how much
     * the code under it produces per chart, and that changed by an order of magnitude in one
     * afternoon.
     */
    private static final int SAMPLE = 300;
    private static final long SEED = 20260823L;

    /**
     * The population Gestalt's CLUSTER_BODIES uses, restated here for one purpose only:
     * counting how much of the tension the engine reports is between planets and how much
     * involves an asteroid, a node or a lot. That number is the input to a question that is
     * David's - whether a tension reading should be taken over all 29 registry points - so it
     * is measured here and decided nowhere.
     */
    private static final List<String> CLASSICAL = List.of(
        "Sun", "Moon", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron");

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private TensionReleaseCheck() { }

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(EPHE_PATH);
        List<ChartFrame> corpus = sample(sw);
        corpus.add(0, ChartFrame.compute(sw, NATAL_JD, LAT, LON, 'P', false, 0.0));

        System.out.println("=== Part A: an outlet is a soft aspect from outside to a member ===");
        int before = failures.size();
        outletsAreReal(corpus);
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: the empty leg is the degree opposite the apex ===");
        before = failures.size();
        emptyLegGeometry(corpus);
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: every state the model can reach has a sentence ===");
        before = failures.size();
        clauseCompleteness(corpus);
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: nothing is read twice, and the list is stable ===");
        before = failures.size();
        noDoubleReading(corpus);
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: incidence over the corpus ===");
        before = failures.size();
        incidence(corpus);
        report("Part E", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- part A

    /**
     * The central claim, asserted against the hit list rather than against the model's own
     * bookkeeping: for every outlet the model reports, the aspect it names must exist, must
     * be a trine or a sextile, and must run from a body outside the configuration to one
     * inside it. An outlet that names a member on both ends is an internal aspect being sold
     * as a way out.
     */
    private static void outletsAreReal(List<ChartFrame> corpus) {
        for (ChartFrame f : corpus) {
            List<Aspects.Hit> hits = Aspects.betweenBodies(f);
            for (TensionRelease.Release r : releases(f, hits)) {
                for (TensionRelease.Outlet o : r.outlets) {
                    yes("outlet comes from outside the configuration (" + o.via + ")",
                        !r.bodies.contains(o.via));
                    yes("outlet touches at least one member (" + o.via + ")", !o.to.isEmpty());
                    eq("outlet records one type per member touched (" + o.via + ")",
                        o.to.size(), o.types.size());
                    for (int i = 0; i < o.to.size(); i++) {
                        String member = o.to.get(i);
                        Aspects.Type t = o.types.get(i);
                        yes("outlet target is a member (" + o.via + " -> " + member + ")",
                            r.bodies.contains(member));
                        yes("outlet is a trine or a sextile (" + t + ")",
                            t == Aspects.Type.TRINE || t == Aspects.Type.SEXTILE);
                        yes("the aspect the outlet names exists in the hit list ("
                            + o.via + " " + t + " " + member + ")",
                            hasAspect(hits, o.via, member, t));
                    }
                    eq("touchesApex agrees with the members touched (" + o.via + ")",
                        r.apex != null && o.to.contains(r.apex), o.touchesApex);
                    eq("isMediator agrees with the members touched (" + o.via + ")",
                        o.to.size() > 1, o.isMediator());
                    yes("the apex outlet sorts first (" + r.source + ")",
                        !o.touchesApex || r.outlets.get(0).touchesApex);
                }
                eq("the definitional-spine flag agrees with the registry ("
                    + String.join(",", r.bodies) + ")",
                    registrySaysDefinitional(r.bodies), r.definitionalSpine);
            }
        }
    }

    /**
     * The same question asked of {@link Bodies} directly.
     *
     * Deliberately not a copy of the model's rule: it reaches the registry by a different
     * route, so a change to what counts as a definitional pair has to be made in the registry
     * to satisfy both. A check that restated the rule would agree with itself while both
     * drifted, which is how the element balance came to be 27.0 against a table saying 22.0.
     */
    private static boolean registrySaysDefinitional(List<String> bodies) {
        for (String a : bodies) {
            for (String b : bodies) {
                if (a.equals(b)) {
                    continue;
                }
                int ia = Bodies.indexOfName(a);
                int ib = Bodies.indexOfName(b);
                if (ia >= 0 && ib >= 0 && Bodies.oppositeOf(ia) == ib) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- part B

    /**
     * The one piece of arithmetic in the feature. 180 degrees from the apex, in the sign and
     * house that lands in - and no empty leg at all where the geometry does not leave one.
     */
    private static void emptyLegGeometry(List<ChartFrame> corpus) {
        for (ChartFrame f : corpus) {
            List<Aspects.Hit> hits = Aspects.betweenBodies(f);
            for (TensionRelease.Release r : releases(f, hits)) {
                if (r.apex == null) {
                    yes("a configuration with no apex has no empty leg (" + r.source + ")",
                        !r.hasEmptyLeg);
                    eq("no apex means no empty-leg sign (" + r.source + ")",
                        null, r.emptyLegSign);
                    continue;
                }
                ChartFrame.Body apex = f.body(r.apex);
                if (apex == null || !apex.ok) {
                    continue;
                }
                yes("an apex-bearing configuration has an empty leg (" + r.source + ")",
                    r.hasEmptyLeg);
                near("the empty leg is 180 degrees from the apex (" + r.apex + ")",
                    180.0, ChartFrame.separation(r.emptyLegLon, apex.lon), 1e-9);
                eq("the empty-leg sign is the sign of that degree (" + r.apex + ")",
                    Zodiac.signName(r.emptyLegLon), r.emptyLegSign);
                yes("the empty-leg house is a house or the degenerate 0 (" + r.apex + ")",
                    r.emptyLegHouse >= 0 && r.emptyLegHouse <= 12);
                if (r.emptyLegOccupant != null) {
                    yes("an occupant of the empty leg opposes the apex ("
                        + r.emptyLegOccupant + ")",
                        hasAspect(hits, r.emptyLegOccupant, r.apex, Aspects.Type.OPPOSITION));
                    yes("an occupant of the empty leg is not itself a member ("
                        + r.emptyLegOccupant + ")",
                        !r.bodies.contains(r.emptyLegOccupant));
                }
            }
        }
    }

    // ---------------------------------------------------------------- part C

    /**
     * Clause completeness. Not "does it read well" - that is not checkable - but "did every
     * branch produce a whole sentence", which is exactly the failure the quincunx glyph was.
     */
    private static void clauseCompleteness(List<ChartFrame> corpus) {
        for (ChartFrame f : corpus) {
            List<Aspects.Hit> hits = Aspects.betweenBodies(f);
            for (TensionRelease.Release r : releases(f, hits)) {
                String s = r.sentence;
                yes("the release has a sentence (" + r.source + ")", s != null && !s.isEmpty());
                if (s == null) {
                    continue;
                }
                yes("the sentence contains no null (" + r.source + "): " + s,
                    !s.contains("null"));
                yes("the sentence ends in a full stop (" + r.source + ")", s.endsWith("."));
                yes("the sentence has no doubled space (" + r.source + ")", !s.contains("  "));
                yes("the sentence names the configuration (" + r.source + ")",
                    s.toLowerCase().contains(r.source.toLowerCase()));
                if (r.apex != null) {
                    yes("an apex-bearing sentence names its apex (" + r.apex + ")",
                        s.contains(r.apex));
                }
                if (!r.outlets.isEmpty()) {
                    yes("a relieved configuration names the body relieving it (" + r.source + ")",
                        s.contains(r.outlets.get(0).via)
                            || s.contains(firstApexOutlet(r)));
                }
                if (r.unrelieved() && r.outlets.isEmpty()) {
                    yes("an unrelieved configuration says so (" + r.source + ")",
                        s.contains("no soft aspect out of it"));
                }
            }
        }
    }

    private static String firstApexOutlet(TensionRelease.Release r) {
        for (TensionRelease.Outlet o : r.outlets) {
            if (o.touchesApex) {
                return o.via;
            }
        }
        return r.outlets.isEmpty() ? "" : r.outlets.get(0).via;
    }

    // ---------------------------------------------------------------- part D

    /**
     * An opposition that is the spine of a T-square must not also be reported on its own.
     *
     * Without that filter the same two bodies get two readings that contradict each other -
     * one saying where the figure discharges, the other saying the tension is unmediated -
     * which is the two-surfaces defect this project logs more than any other, arriving inside
     * a single method.
     *
     * The stability assertion is here for the same reason AspectGridCheck stopped asserting
     * per grid cell: {@link AspectPatterns} returns its findings out of a HashSet, so a list
     * built from it is only reproducible if something sorts it.
     */
    private static void noDoubleReading(List<ChartFrame> corpus) {
        for (ChartFrame f : corpus) {
            List<Aspects.Hit> hits = Aspects.betweenBodies(f);
            List<AspectPatterns.Pattern> patterns = AspectPatterns.findPatterns(hits);
            List<TensionRelease.Release> rs = TensionRelease.find(f, hits, patterns);

            List<String> keys = new ArrayList<>();
            for (TensionRelease.Release r : rs) {
                keys.add(r.source + ":" + String.join(",", r.bodies));
            }
            eq("no configuration is reported twice", keys.size(),
                new java.util.LinkedHashSet<>(keys).size());

            for (TensionRelease.Release r : rs) {
                if (!r.source.equals("Opposition")) {
                    continue;
                }
                for (AspectPatterns.Pattern p : patterns) {
                    yes("an opposition inside a pattern is not read on its own ("
                        + String.join(",", r.bodies) + " vs " + p.name + ")",
                        !(p.bodies.contains(r.bodies.get(0)) && p.bodies.contains(r.bodies.get(1))));
                }
            }

            // The partition: every stressed configuration is either a named pattern or a
            // loose opposition, and none is both. This replaced an assertion that said a
            // stress pattern should be absent from SOME chart in the corpus - which is
            // false, and was an expectation about ten-planet charts asserted over a
            // twenty-nine point one. See the incidence in Part E.
            int namedStress = 0;
            for (AspectPatterns.Pattern p : patterns) {
                if (p.name.equals("T-square") || p.name.equals("Grand cross")
                    || p.name.equals("Yod")) {
                    namedStress++;
                }
            }
            int loose = 0;
            for (Aspects.Hit h : hits) {
                if (h.type != Aspects.Type.OPPOSITION) {
                    continue;
                }
                // Same population and orb as the pattern engine, read from its constants
                // rather than restated. When patterns moved to physical bodies at a tight
                // orb and this line did not, the partition failed on 20 charts of 41 - the
                // check correctly reporting that the two halves of one feature had come
                // apart.
                if (!AspectPatterns.PATTERN_BODIES.contains(h.a)
                    || !AspectPatterns.PATTERN_BODIES.contains(h.b)
                    || h.offBy > AspectPatterns.MAX_ORB) {
                    continue;
                }
                boolean claimed = false;
                for (AspectPatterns.Pattern p : patterns) {
                    if (p.bodies.contains(h.a) && p.bodies.contains(h.b)) {
                        claimed = true;
                        break;
                    }
                }
                if (!claimed) {
                    loose++;
                }
            }
            eq("the release list is exactly the named stress patterns plus the loose "
                + "oppositions", namedStress + loose, rs.size());

            List<TensionRelease.Release> again = TensionRelease.find(f, hits, patterns);
            List<String> keys2 = new ArrayList<>();
            for (TensionRelease.Release r : again) {
                keys2.add(r.source + ":" + String.join(",", r.bodies));
            }
            eq("the same chart gives the same list in the same order", keys, keys2);
        }
    }

    // ---------------------------------------------------------------- part E

    /**
     * What the feature actually finds, so the handover carries a measured number rather than
     * a plausible one - and so the next person to widen the stress set can see what it cost.
     *
     * The empty-leg occupancy line is the one to watch. A T-square's fourth point is vacant
     * by construction in the textbook, but the engine takes its orb per pair, so a body can
     * oppose the apex while missing one of the squares. Whether that is rare or common is a
     * measurement, and this is where it is taken.
     */
    private static void incidence(List<ChartFrame> corpus) {
        Map<String, Integer> bySource = new LinkedHashMap<>();
        int chartsWithPattern = 0;
        int apexBearing = 0;
        int apexRelieved = 0;
        int unrelieved = 0;
        int mediated = 0;
        int occupiedLegs = 0;
        int emptyLegs = 0;
        int degenerateHouses = 0;
        int definitional = 0;
        int classical = 0;
        int total = 0;

        for (ChartFrame f : corpus) {
            List<Aspects.Hit> hits = Aspects.betweenBodies(f);
            List<TensionRelease.Release> rs = releases(f, hits);
            boolean patterned = false;
            for (TensionRelease.Release r : rs) {
                total++;
                bySource.merge(r.source, 1, Integer::sum);
                if (!r.source.equals("Opposition")) {
                    patterned = true;
                }
                if (r.apex != null) {
                    apexBearing++;
                    for (TensionRelease.Outlet o : r.outlets) {
                        if (o.touchesApex) {
                            apexRelieved++;
                            break;
                        }
                    }
                }
                for (TensionRelease.Outlet o : r.outlets) {
                    if (o.isMediator()) {
                        mediated++;
                        break;
                    }
                }
                if (r.unrelieved()) {
                    unrelieved++;
                }
                if (r.definitionalSpine) {
                    definitional++;
                }
                if (CLASSICAL.containsAll(r.bodies)) {
                    classical++;
                }
                if (r.hasEmptyLeg) {
                    emptyLegs++;
                    if (r.emptyLegOccupant != null) {
                        occupiedLegs++;
                    }
                    if (r.emptyLegHouse == 0) {
                        degenerateHouses++;
                    }
                }
            }
            if (patterned) {
                chartsWithPattern++;
            }
        }

        System.out.printf("  %d charts, %d configurations, %.2f per chart%n",
            corpus.size(), total, total / (double) corpus.size());
        System.out.println("  by source: " + bySource);
        System.out.printf("  charts carrying a named stress pattern: %d of %d (%.1f%%)%n",
            chartsWithPattern, corpus.size(), 100.0 * chartsWithPattern / corpus.size());
        System.out.printf("  apex-bearing configurations relieved at the apex: %d of %d (%.1f%%)%n",
            apexRelieved, apexBearing, apexBearing == 0 ? 0.0 : 100.0 * apexRelieved / apexBearing);
        System.out.printf("  configurations with a mediator: %d of %d (%.1f%%)%n",
            mediated, total, total == 0 ? 0.0 : 100.0 * mediated / total);
        System.out.printf("  configurations with nothing reaching them: %d of %d (%.1f%%)%n",
            unrelieved, total, total == 0 ? 0.0 : 100.0 * unrelieved / total);
        System.out.printf("  empty legs occupied: %d of %d (%.1f%%)%n",
            occupiedLegs, emptyLegs, emptyLegs == 0 ? 0.0 : 100.0 * occupiedLegs / emptyLegs);
        System.out.printf("  empty legs in a degenerate house frame: %d%n", degenerateHouses);
        System.out.printf("  configurations whose spine is definitional: %d of %d (%.1f%%)%n",
            definitional, total, total == 0 ? 0.0 : 100.0 * definitional / total);
        System.out.printf("  configurations made only of the ten planets and Chiron: "
            + "%d of %d (%.1f%%), %.2f per chart%n",
            classical, total, total == 0 ? 0.0 : 100.0 * classical / total,
            classical / (double) corpus.size());

        yes("the corpus produced configurations to measure", total > 0);

        System.out.println();
        System.out.println("  the reference chart reads:");
        ChartFrame ref = corpus.get(0);
        for (TensionRelease.Release r : releases(ref, Aspects.betweenBodies(ref))) {
            System.out.println("    " + r.sentence);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static List<TensionRelease.Release> releases(ChartFrame f, List<Aspects.Hit> hits) {
        return TensionRelease.find(f, hits, AspectPatterns.findPatterns(hits));
    }

    private static boolean hasAspect(List<Aspects.Hit> hits, String a, String b,
                                     Aspects.Type type) {
        for (Aspects.Hit h : hits) {
            if (h.type != type) {
                continue;
            }
            if ((h.a.equals(a) && h.b.equals(b)) || (h.a.equals(b) && h.b.equals(a))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Placidus, inside the latitude band where quadrant cusps exist, the same corpus shape
     * JoyCheck uses. The empty leg is reported by house, so a frame whose houses degenerate
     * would make Part B measure the house system rather than this code.
     */
    private static List<ChartFrame> sample(SwissEph sw) {
        Random rnd = new Random(SEED);
        List<ChartFrame> out = new ArrayList<>();
        for (int i = 0; i < SAMPLE; i++) {
            int year = 1930 + rnd.nextInt(90);
            int month = 1 + rnd.nextInt(12);
            int day = 1 + rnd.nextInt(28);
            double hour = rnd.nextDouble() * 24.0;
            double lat = -60.0 + rnd.nextDouble() * 120.0;
            double lon = -180.0 + rnd.nextDouble() * 360.0;
            SweDate sd = new SweDate(year, month, day, hour);
            out.add(ChartFrame.compute(sw, sd.getJulDay(), lat, lon, 'P', false, 0.0));
        }
        return out;
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null) {
            if (actual != null) {
                failures.add(label + ": got " + actual + ", expected null");
            }
        } else if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected + " +/- " + tol);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "clear" : added + " FAILURE(S)"));
    }
}
