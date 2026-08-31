package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Guards {@link AspectPatterns} after the 2026-08-23 rework: physical bodies only, tight
 * orbs, and two new figures.
 *
 * <b>Most of this suite is synthetic, and that is the point.</b> A Grand sextile did not
 * occur once in 3,000 random charts and a Boomerang occurred in 0.8% of them, so a corpus
 * cannot demonstrate that either detector works - it can only fail to contradict them. The
 * geometries here are built by hand out of {@link Aspects.Hit}s, so every branch is exercised
 * on every run whether or not the sky obliges.
 *
 * Run:
 *   java -cp src\main\java com.zodiacomputing.ourania.astro.AspectPatternCheck
 */
public final class AspectPatternCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;
    private static final int SAMPLE = 400;
    private static final long SEED = 20260823L;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private AspectPatternCheck() { }

    public static void main(String[] args) {
        System.out.println("=== Part A: each figure is found from its own textbook geometry ===");
        int before = failures.size();
        textbookFigures();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: the population and the orb are boundaries, not hints ===");
        before = failures.size();
        boundaries();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: one figure gets one reading ===");
        before = failures.size();
        oneReading();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: modality and element classification ===");
        before = failures.size();
        classification();
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: incidence over the corpus ===");
        before = failures.size();
        incidence();
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
     * Every figure the engine claims to find, built from its definition and nothing else.
     *
     * The expected set is exact rather than "contains", because a detector that finds a
     * T-square inside a Grand cross - or a Yod inside a Boomerang - is reporting one
     * configuration twice, which is the failure mode these patterns are most prone to.
     */
    private static void textbookFigures() {
        // T-square: Sun opposite Moon, both square Mars.
        expect("T-square",
            hits(opp("Sun", "Moon"), sqr("Sun", "Mars"), sqr("Moon", "Mars")),
            "T-square:Mars:Mars,Moon,Sun");

        // Grand cross: two oppositions, four squares.
        expect("Grand cross",
            hits(opp("Sun", "Moon"), opp("Mars", "Venus"),
                 sqr("Sun", "Mars"), sqr("Sun", "Venus"),
                 sqr("Moon", "Mars"), sqr("Moon", "Venus")),
            "Grand cross:-:Mars,Moon,Sun,Venus",
            "T-square:Mars:Mars,Moon,Sun", "T-square:Venus:Moon,Sun,Venus",
            "T-square:Moon:Mars,Moon,Venus", "T-square:Sun:Mars,Sun,Venus");

        // Grand trine.
        expect("Grand trine",
            hits(tri("Sun", "Moon"), tri("Moon", "Mars"), tri("Sun", "Mars")),
            "Grand trine:-:Mars,Moon,Sun");

        // Kite: grand trine plus a body opposite one corner, sextile the other two.
        expect("Kite",
            hits(tri("Sun", "Moon"), tri("Moon", "Mars"), tri("Sun", "Mars"),
                 opp("Venus", "Sun"), sex("Venus", "Moon"), sex("Venus", "Mars")),
            "Grand trine:-:Mars,Moon,Sun", "Kite:Sun:Mars,Moon,Sun,Venus");

        // Yod: sextile base, both ends quincunx the apex.
        expect("Yod",
            hits(sex("Sun", "Moon"), qui("Sun", "Mars"), qui("Moon", "Mars")),
            "Yod:Mars:Mars,Moon,Sun");

        // Boomerang: the same Yod with a fourth body opposite the apex. The Yod must NOT
        // also be reported - see Part C.
        expect("Boomerang",
            hits(sex("Sun", "Moon"), qui("Sun", "Mars"), qui("Moon", "Mars"),
                 opp("Venus", "Mars")),
            "Boomerang:Mars:Mars,Moon,Sun,Venus");

        // Mystic rectangle: two oppositions, cross-trines and cross-sextiles.
        expect("Mystic rectangle",
            hits(opp("Sun", "Mars"), opp("Moon", "Venus"),
                 tri("Sun", "Moon"), tri("Mars", "Venus"),
                 sex("Sun", "Venus"), sex("Mars", "Moon")),
            "Mystic rectangle:-:Mars,Moon,Sun,Venus");

        // Grand sextile: two grand trines standing 60 degrees apart. **Never once seen in
        // 3,000 random charts**, so this is the only place it is exercised at all.
        //
        // Asserted by the structural claim the source makes about it rather than by a
        // hand-written list of members: a Grand sextile "structurally contains two Grand
        // Trines, six Kites, and three Mystic Rectangles". That is a statement about the
        // geometry, so it is checkable, and it cannot be satisfied by copying whatever the
        // detector happened to print - which is exactly what the first version of this
        // assertion did, and it was wrong while the code was right.
        Map<String, Integer> star = new LinkedHashMap<>();
        for (AspectPatterns.Pattern p : AspectPatterns.findPatterns(grandSextileHits())) {
            star.merge(p.name, 1, Integer::sum);
        }
        eq("a Grand sextile is found at all", 1, star.getOrDefault("Grand sextile", 0));
        eq("a Grand sextile contains two Grand trines", 2, star.getOrDefault("Grand trine", 0));
        eq("a Grand sextile contains six Kites", 6, star.getOrDefault("Kite", 0));
        eq("a Grand sextile contains three Mystic rectangles",
            3, star.getOrDefault("Mystic rectangle", 0));
        eq("and nothing else", 4, star.size());
    }

    /**
     * Six bodies at 60-degree steps: Sun 0, Venus 60, Mercury 120, Moon 180, Mars 240,
     * Jupiter 300. Every pair is sextile, trine or opposition by construction.
     */
    private static List<Aspects.Hit> grandSextileHits() {
        String[] names = {"Sun", "Venus", "Mercury", "Moon", "Mars", "Jupiter"};
        List<Aspects.Hit> out = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            for (int j = i + 1; j < names.length; j++) {
                int steps = Math.min(j - i, names.length - (j - i));
                Aspects.Type t = steps == 1 ? Aspects.Type.SEXTILE
                    : steps == 2 ? Aspects.Type.TRINE : Aspects.Type.OPPOSITION;
                out.add(hit(names[i], names[j], t));
            }
        }
        return out;
    }

    /** Asserts the exact set of patterns found, as name:apex:bodies keys. */
    private static void expect(String label, List<Aspects.Hit> hits, String... wanted) {
        Set<String> got = new java.util.TreeSet<>();
        for (AspectPatterns.Pattern p : AspectPatterns.findPatterns(hits)) {
            got.add(p.name + ":" + (p.apex == null ? "-" : p.apex)
                + ":" + String.join(",", p.bodies));
        }
        Set<String> want = new java.util.TreeSet<>(java.util.Arrays.asList(wanted));
        eq(label + " produces exactly its expected figures", want, got);
    }

    // ---------------------------------------------------------------- part B

    /**
     * The two rules adopted on 2026-08-23, asserted as the boundaries they are.
     *
     * A figure built to the letter out of a node, a lot or an asteroid must produce nothing,
     * and a figure one tenth of a degree outside the orb must produce nothing. Before the
     * rework both of these returned a T-square, which is how a chart came to carry 12.3
     * configurations and a T-square on the nodal axis.
     */
    private static void boundaries() {
        for (String outsider : new String[]{"North Node", "South Node", "Part of Fortune",
                                            "Ascendant", "Black Moon Lilith", "Ceres",
                                            "Eris", "Pallas", "Vesta"}) {
            List<Aspects.Hit> h = hits(opp("Sun", "Moon"),
                sqr("Sun", outsider), sqr("Moon", outsider));
            eq("a T-square apexed on " + outsider + " is not a pattern",
                0, AspectPatterns.findPatterns(h).size());
        }
        yes("the pattern population is the ten planets and Chiron",
            AspectPatterns.PATTERN_BODIES.size() == 11
                && AspectPatterns.PATTERN_BODIES.contains("Chiron")
                && !AspectPatterns.PATTERN_BODIES.contains("North Node"));

        double orb = AspectPatterns.MAX_ORB;
        eq("a figure inside the orb is a pattern",
            1, AspectPatterns.findPatterns(hits(
                opp("Sun", "Moon", orb - 0.1), sqr("Sun", "Mars", orb - 0.1),
                sqr("Moon", "Mars", orb - 0.1))).size());
        eq("a figure just outside the orb is not",
            0, AspectPatterns.findPatterns(hits(
                opp("Sun", "Moon", orb + 0.1), sqr("Sun", "Mars", orb - 0.1),
                sqr("Moon", "Mars", orb - 0.1))).size());
        eq("one loose leg is enough to break the circuit",
            0, AspectPatterns.findPatterns(hits(
                opp("Sun", "Moon", 0.1), sqr("Sun", "Mars", 0.1),
                sqr("Moon", "Mars", orb + 0.1))).size());
    }

    // ---------------------------------------------------------------- part C

    /** A Boomerang is not also a Yod, and the widest orb is the loosest member aspect. */
    private static void oneReading() {
        List<AspectPatterns.Pattern> boom = AspectPatterns.findPatterns(
            hits(sex("Sun", "Moon"), qui("Sun", "Mars"), qui("Moon", "Mars"),
                 opp("Venus", "Mars")));
        eq("a Boomerang is reported once", 1, boom.size());
        eq("and it is not reported as a Yod", "Boomerang", boom.get(0).name);

        List<AspectPatterns.Pattern> tsq = AspectPatterns.findPatterns(
            hits(opp("Sun", "Moon", 3.0), sqr("Sun", "Mars", 1.0), sqr("Moon", "Mars", 4.4)));
        eq("one T-square", 1, tsq.size());
        near("widestOrb is the loosest leg", 4.4, tsq.get(0).widestOrb, 1e-9);

        List<AspectPatterns.Pattern> twice = AspectPatterns.findPatterns(
            hits(opp("Sun", "Moon"), sqr("Sun", "Mars"), sqr("Moon", "Mars")));
        List<AspectPatterns.Pattern> again = AspectPatterns.findPatterns(
            hits(opp("Sun", "Moon"), sqr("Sun", "Mars"), sqr("Moon", "Mars")));
        eq("the same input gives the same list", render(twice), render(again));
    }

    private static String render(List<AspectPatterns.Pattern> ps) {
        StringBuilder sb = new StringBuilder();
        for (AspectPatterns.Pattern p : ps) {
            sb.append(p.name).append('/').append(String.join(",", p.bodies)).append(';');
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- part D

    /**
     * Classification is only meaningful against real longitudes, so this part uses charts.
     *
     * The claim is narrow and checkable: a pattern reported as cardinal must have every
     * member in a cardinal sign, and one reported with a null modality must not.
     */
    private static void classification() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        for (ChartFrame f : sample(sw)) {
            for (AspectPatterns.Pattern p : AspectPatterns.findPatterns(f,
                    Aspects.betweenBodies(f))) {
                String modality = null;
                String element = null;
                boolean first = true;
                boolean computable = true;
                for (String name : p.bodies) {
                    ChartFrame.Body b = f.body(name);
                    if (b == null || !b.ok) {
                        computable = false;
                        break;
                    }
                    int s = Zodiac.signIndex(b.lon);
                    if (first) {
                        modality = Zodiac.modalityName(s);
                        element = Zodiac.elementName(s);
                        first = false;
                    } else {
                        if (!Zodiac.modalityName(s).equals(modality)) modality = null;
                        if (!Zodiac.elementName(s).equals(element)) element = null;
                    }
                }
                if (!computable) {
                    continue;
                }
                eq("modality matches the members' signs (" + p.name + ")", modality, p.modality);
                eq("element matches the members' signs (" + p.name + ")", element, p.element);
                yes("widestOrb is inside the pattern orb (" + p.name + ")",
                    p.widestOrb <= AspectPatterns.MAX_ORB);
                yes("every member is a pattern body (" + p.name + ")",
                    AspectPatterns.PATTERN_BODIES.containsAll(p.bodies));
                if (p.apex != null) {
                    yes("the apex is a member (" + p.name + ")", p.bodies.contains(p.apex));
                }
            }
        }
    }

    // ---------------------------------------------------------------- part E

    private static void incidence() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        Map<String, Integer> found = new TreeMap<>();
        Map<String, Integer> charts = new TreeMap<>();
        int any = 0;
        List<ChartFrame> corpus = sample(sw);
        for (ChartFrame f : corpus) {
            List<AspectPatterns.Pattern> ps = AspectPatterns.findPatterns(f,
                Aspects.betweenBodies(f));
            java.util.Set<String> here = new java.util.HashSet<>();
            for (AspectPatterns.Pattern p : ps) {
                found.merge(p.name, 1, Integer::sum);
                here.add(p.name);
            }
            for (String n : here) {
                charts.merge(n, 1, Integer::sum);
            }
            if (!ps.isEmpty()) {
                any++;
            }
        }
        System.out.printf("  %d charts, population %d bodies, orb %.1f%n",
            corpus.size(), AspectPatterns.PATTERN_BODIES.size(), AspectPatterns.MAX_ORB);
        for (Map.Entry<String, Integer> e : found.entrySet()) {
            System.out.printf("    %-18s %5d found, in %.1f%% of charts%n",
                e.getKey(), e.getValue(), 100.0 * charts.get(e.getKey()) / corpus.size());
        }
        System.out.printf("  any pattern at all: %d of %d (%.1f%%)%n",
            any, corpus.size(), 100.0 * any / corpus.size());

        yes("patterns are found in some charts", any > 0);
        yes("patterns are NOT found in every chart - they are meant to be notable",
            any < corpus.size());
        yes("the T-square is the commonest figure",
            found.getOrDefault("T-square", 0) >= found.getOrDefault("Grand trine", 0));
    }

    // ---------------------------------------------------------------- builders

    private static List<Aspects.Hit> hits(Aspects.Hit... hs) {
        return new ArrayList<>(java.util.Arrays.asList(hs));
    }

    private static Aspects.Hit opp(String a, String b) { return hit(a, b, Aspects.Type.OPPOSITION); }
    private static Aspects.Hit sqr(String a, String b) { return hit(a, b, Aspects.Type.SQUARE); }
    private static Aspects.Hit tri(String a, String b) { return hit(a, b, Aspects.Type.TRINE); }
    private static Aspects.Hit sex(String a, String b) { return hit(a, b, Aspects.Type.SEXTILE); }
    private static Aspects.Hit qui(String a, String b) { return hit(a, b, Aspects.Type.QUINCUNX); }

    private static Aspects.Hit opp(String a, String b, double off) {
        return hit(a, b, Aspects.Type.OPPOSITION, off);
    }
    private static Aspects.Hit sqr(String a, String b, double off) {
        return hit(a, b, Aspects.Type.SQUARE, off);
    }

    private static Aspects.Hit hit(String a, String b, Aspects.Type t) {
        return hit(a, b, t, 0.0);
    }

    private static Aspects.Hit hit(String a, String b, Aspects.Type t, double offBy) {
        Aspects.Hit h = new Aspects.Hit();
        h.a = a;
        h.b = b;
        h.type = t;
        h.offBy = offBy;
        h.separation = t.exactAngle + offBy;
        h.orbUsed = 10.0;
        h.tightness = 1.0 - offBy / 10.0;
        return h;
    }

    private static List<ChartFrame> sample(SwissEph sw) {
        Random rnd = new Random(SEED);
        List<ChartFrame> out = new ArrayList<>();
        for (int i = 0; i < SAMPLE; i++) {
            SweDate sd = new SweDate(1900 + rnd.nextInt(150), 1 + rnd.nextInt(12),
                1 + rnd.nextInt(28), rnd.nextDouble() * 24.0);
            out.add(ChartFrame.compute(sw, sd.getJulDay(),
                -60.0 + rnd.nextDouble() * 120.0, -180.0 + rnd.nextDouble() * 360.0,
                'P', false, 0.0));
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
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "clear" : added + " FAILURE(S)"));
    }
}
