package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Guards {@link Quincunx}: which of the twelve quincunx sign-pairs keeps a secondary
 * connection, and which are averse.
 *
 * <b>The classification is derived and the check is tabulated, deliberately.</b> `Quincunx`
 * works each pair out from {@link Dignity}'s rulership table and {@link Zodiac}'s reflection
 * arithmetic; the tables below are typed from the traditional lists. Two representations from
 * two sources, so agreement is evidence. A check that re-derived the answer the same way would
 * agree with the code no matter what either of them said.
 *
 * Run:
 *   java -cp src\main\java com.zodiacomputing.ourania.astro.QuincunxCheck
 */
public final class QuincunxCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    /** The six antiscia sign pairs, typed from the traditional list. */
    private static final String[][] ANTISCIA = {
        {"aries", "virgo"}, {"taurus", "leo"}, {"gemini", "cancer"},
        {"libra", "pisces"}, {"scorpio", "aquarius"}, {"sagittarius", "capricorn"},
    };

    /** The six contra-antiscia sign pairs, likewise. */
    private static final String[][] CONTRA = {
        {"aries", "pisces"}, {"taurus", "aquarius"}, {"gemini", "capricorn"},
        {"cancer", "sagittarius"}, {"leo", "scorpio"}, {"virgo", "libra"},
    };

    /** Every quincunx pair and the kind it should classify as, typed out. */
    private static final Object[][] EXPECTED = {
        {"aries", "virgo", Quincunx.Kind.ANTISCIA, null},
        {"aries", "scorpio", Quincunx.Kind.COMMON_RULERSHIP, "Mars"},
        {"taurus", "libra", Quincunx.Kind.COMMON_RULERSHIP, "Venus"},
        {"taurus", "sagittarius", Quincunx.Kind.AVERSE, null},
        {"gemini", "scorpio", Quincunx.Kind.AVERSE, null},
        {"gemini", "capricorn", Quincunx.Kind.CONTRA_ANTISCIA, null},
        {"cancer", "sagittarius", Quincunx.Kind.CONTRA_ANTISCIA, null},
        {"cancer", "aquarius", Quincunx.Kind.AVERSE, null},
        {"leo", "capricorn", Quincunx.Kind.AVERSE, null},
        {"leo", "pisces", Quincunx.Kind.AVERSE, null},
        {"virgo", "aquarius", Quincunx.Kind.AVERSE, null},
        {"libra", "pisces", Quincunx.Kind.ANTISCIA, null},
    };

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private QuincunxCheck() { }

    public static void main(String[] args) {
        System.out.println("=== Part A: which pairs are quincunx at all ===");
        int before = failures.size();
        pairing();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: the reflections land where the tradition says ===");
        before = failures.size();
        reflections();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: all twelve pairs, against the typed table ===");
        before = failures.size();
        classification();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: the averse set, measured ===");
        before = failures.size();
        averseSet();
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: every quincunx in a corpus classifies ===");
        before = failures.size();
        corpus();
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

    private static void pairing() {
        int pairs = 0;
        for (int a = 0; a < 12; a++) {
            for (int b = 0; b < 12; b++) {
                int gap = Math.floorMod(b - a, 12);
                boolean shouldBe = gap == 5 || gap == 7;
                eq("quincunx pairing at " + Zodiac.SIGNS[a] + "/" + Zodiac.SIGNS[b],
                    shouldBe, Quincunx.isQuincunxPair(a, b));
                if (shouldBe && a < b) {
                    pairs++;
                }
                if (!shouldBe) {
                    // A caller asking about a square has made a mistake; a plausible answer
                    // would hide it, so this must throw rather than return AVERSE.
                    boolean threw = false;
                    try {
                        Quincunx.classify(a, b);
                    } catch (IllegalArgumentException e) {
                        threw = true;
                    }
                    yes("classifying a non-quincunx throws (" + Zodiac.SIGNS[a] + "/"
                        + Zodiac.SIGNS[b] + ")", threw);
                }
            }
        }
        eq("there are twelve unordered quincunx pairs", 12, pairs);
    }

    // ---------------------------------------------------------------- part B

    /**
     * The arithmetic against the traditional lists.
     *
     * Checked at three degrees inside each sign rather than at the cusp: the reflection maps a
     * sign's start exactly onto a boundary, and which side of it you land on is a rounding
     * question rather than an astrological one.
     */
    private static void reflections() {
        for (String[] pair : ANTISCIA) {
            int a = Zodiac.signIndexOf(pair[0]);
            int b = Zodiac.signIndexOf(pair[1]);
            for (double d : new double[]{5.0, 15.0, 25.0}) {
                eq("antiscion of " + d + " " + pair[0] + " lands in " + pair[1],
                    b, Zodiac.signIndex(Zodiac.antiscion(a * 30.0 + d)));
                eq("and back again from " + pair[1],
                    a, Zodiac.signIndex(Zodiac.antiscion(b * 30.0 + d)));
            }
            // The reflection property itself, which is what "equal length of day" rests on:
            // a longitude and its antiscion always sum to 180 degrees around the circle.
            //
            // The first version of this asserted their midpoint was 90 - the Cancer end of the
            // solstitial axis - and failed on the three Capricorn-side pairs, whose midpoint is
            // 270. The axis has two ends; the assertion had only remembered one. The code was
            // right throughout.
            double lon = a * 30.0 + 15.0;
            near("a longitude and its antiscion sum to 180 (" + pair[0] + ")",
                180.0, Zodiac.normalise(lon + Zodiac.antiscion(lon)), 1e-9);
            double mid = (lon + Zodiac.antiscion(lon)) / 2.0;
            yes("and they straddle one end of the solstitial axis (" + pair[0] + ")",
                Math.abs(mid - 90.0) < 1e-9 || Math.abs(mid - 270.0) < 1e-9);
        }
        for (String[] pair : CONTRA) {
            int a = Zodiac.signIndexOf(pair[0]);
            int b = Zodiac.signIndexOf(pair[1]);
            for (double d : new double[]{5.0, 15.0, 25.0}) {
                eq("contra-antiscion of " + d + " " + pair[0] + " lands in " + pair[1],
                    b, Zodiac.signIndex(Zodiac.contraAntiscion(a * 30.0 + d)));
                eq("and back again from " + pair[1],
                    a, Zodiac.signIndex(Zodiac.contraAntiscion(b * 30.0 + d)));
            }
        }
        // The axes themselves are fixed points of their own reflection.
        near("0 Cancer is its own antiscion", 90.0, Zodiac.antiscion(90.0), 1e-9);
        near("0 Capricorn is its own antiscion", 270.0, Zodiac.antiscion(270.0), 1e-9);
        near("0 Aries is its own contra-antiscion", 0.0, Zodiac.contraAntiscion(0.0), 1e-9);
        near("0 Libra is its own contra-antiscion", 180.0, Zodiac.contraAntiscion(180.0), 1e-9);
    }

    // ---------------------------------------------------------------- part C

    private static void classification() {
        Map<Quincunx.Kind, Integer> tally = new LinkedHashMap<>();
        for (Object[] row : EXPECTED) {
            int a = Zodiac.signIndexOf((String) row[0]);
            int b = Zodiac.signIndexOf((String) row[1]);
            Quincunx.Relation r = Quincunx.classify(a, b);
            eq("kind of " + row[0] + "/" + row[1], row[2], r.kind);
            eq("shared ruler of " + row[0] + "/" + row[1], row[3], r.sharedRuler);

            // Order must not matter: the relationship is a property of the pair.
            Quincunx.Relation back = Quincunx.classify(b, a);
            eq("classification is symmetric (" + row[0] + "/" + row[1] + ")", r.kind, back.kind);
            eq("shared ruler is symmetric (" + row[0] + "/" + row[1] + ")",
                r.sharedRuler, back.sharedRuler);

            // A shared ruler must genuinely be the ruler of both signs, asked of Dignity.
            if (r.sharedRuler != null) {
                eq("the shared ruler rules " + row[0], r.sharedRuler, Dignity.domicileRulerOf(a));
                eq("the shared ruler rules " + row[1], r.sharedRuler, Dignity.domicileRulerOf(b));
            }
            yes("a kind always has a label and a meaning",
                !r.kind.label.isEmpty() && r.kind.meaning.length() > 60);
            yes("describe() names the ruler when there is one",
                r.sharedRuler == null || r.describe().contains(r.sharedRuler));
            tally.merge(r.kind, 1, Integer::sum);
        }
        eq("the twelve pairs are fully classified", 12,
            tally.values().stream().mapToInt(Integer::intValue).sum());
        System.out.println("  " + tally);
        eq("two pairs share a ruler", 2, (int) tally.getOrDefault(
            Quincunx.Kind.COMMON_RULERSHIP, 0));
        eq("two pairs are antiscia", 2, (int) tally.getOrDefault(Quincunx.Kind.ANTISCIA, 0));
        eq("two pairs are contra-antiscia", 2, (int) tally.getOrDefault(
            Quincunx.Kind.CONTRA_ANTISCIA, 0));
        eq("six pairs are averse", 6, (int) tally.getOrDefault(Quincunx.Kind.AVERSE, 0));
    }

    // ---------------------------------------------------------------- part D

    /**
     * The source's claim, and the half of it that does not hold.
     *
     * Burk and Cunningham give the Leo and Aquarius quincunxes as the truly averse ones,
     * "because these signs have absolutely no shared rulership, antiscia, or contra-antiscia
     * connections". <b>The first half checks out exactly and the implied second half does
     * not</b>: Taurus-Sagittarius and Gemini-Scorpio are equally unconnected. Asserted both
     * ways so that neither can drift, and so the finding survives being forgotten.
     */
    private static void averseSet() {
        for (int sign : new int[]{Zodiac.signIndexOf("leo"), Zodiac.signIndexOf("aquarius")}) {
            for (int gap : new int[]{5, 7}) {
                int other = (sign + gap) % 12;
                eq("every Leo/Aquarius quincunx is averse (" + Zodiac.SIGNS[sign] + "/"
                    + Zodiac.SIGNS[other] + ")",
                    Quincunx.Kind.AVERSE, Quincunx.classify(sign, other).kind);
            }
        }
        // ...and these two are averse without involving either sign.
        eq("Taurus-Sagittarius is averse too", Quincunx.Kind.AVERSE,
            Quincunx.classify(Zodiac.signIndexOf("taurus"),
                Zodiac.signIndexOf("sagittarius")).kind);
        eq("Gemini-Scorpio is averse too", Quincunx.Kind.AVERSE,
            Quincunx.classify(Zodiac.signIndexOf("gemini"),
                Zodiac.signIndexOf("scorpio")).kind);

        List<String> averseWithout = new ArrayList<>();
        for (int a = 0; a < 12; a++) {
            for (int gap : new int[]{5, 7}) {
                int b = (a + gap) % 12;
                if (a > b || a == 4 || b == 4 || a == 10 || b == 10) {
                    continue;
                }
                if (Quincunx.classify(a, b).kind == Quincunx.Kind.AVERSE) {
                    averseWithout.add(Zodiac.SIGNS[a] + "/" + Zodiac.SIGNS[b]);
                }
            }
        }
        System.out.println("  averse without Leo or Aquarius: " + averseWithout);
        eq("exactly two averse pairs involve neither Leo nor Aquarius", 2, averseWithout.size());
    }

    // ---------------------------------------------------------------- part E

    private static void corpus() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        Random rnd = new Random(20260823L);
        Map<String, Integer> tally = new LinkedHashMap<>();
        int seen = 0;
        for (int i = 0; i < 300; i++) {
            SweDate sd = new SweDate(1900 + rnd.nextInt(150), 1 + rnd.nextInt(12),
                1 + rnd.nextInt(28), rnd.nextDouble() * 24.0);
            ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(),
                -60.0 + rnd.nextDouble() * 120.0, -180.0 + rnd.nextDouble() * 360.0,
                'P', false, 0.0);
            for (Aspects.Hit h : Aspects.betweenBodies(f)) {
                if (h.type != Aspects.Type.QUINCUNX) {
                    continue;
                }
                ChartFrame.Body a = f.body(h.a);
                ChartFrame.Body b = f.body(h.b);
                if (a == null || b == null || !a.ok || !b.ok) {
                    continue;
                }
                int sa = Zodiac.signIndex(a.lon);
                int sb = Zodiac.signIndex(b.lon);
                // A quincunx by degree can be dissociate - five signs by orb, four or six by
                // sign - and then there is no quincunx sign-pair to classify at all. That is a
                // real state and it must not throw.
                if (!Quincunx.isQuincunxPair(sa, sb)) {
                    tally.merge("dissociate (no sign pair)", 1, Integer::sum);
                    seen++;
                    continue;
                }
                Quincunx.Relation r = Quincunx.classify(sa, sb);
                yes("every classified quincunx has a kind", r.kind != null);
                yes("and a describable one", !r.describe().isEmpty());
                tally.merge(r.kind.label, 1, Integer::sum);
                seen++;
            }
        }
        System.out.println("  " + seen + " quincunxes over 300 charts: " + tally);
        yes("the corpus produced quincunxes to classify", seen > 0);
        yes("more than one kind occurs", tally.size() > 1);
    }

    // ---------------------------------------------------------------- helpers

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
