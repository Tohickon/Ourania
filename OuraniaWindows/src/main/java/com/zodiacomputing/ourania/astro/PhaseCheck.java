package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Verification and measurement for the L3 solar-phase term, oriental and occidental.
 *
 *  Part A - the measurement, against constructed longitudes where the answer is known
 *           from the geometry rather than from the ephemeris.
 *  Part B - the superior/inferior split, checked against the ASTRONOMY rather than
 *           against a retyped copy of the table. This is the part that matters: getting
 *           the split backwards for one group crashes nothing and reads perfectly, it just
 *           makes two planets out of five wrong in every chart forever.
 *  Part C - the term in isolation on the natal chart, and the Mercury two-axis case.
 *  Part D - corpus measurement, including the overlap with combustion, which is the
 *           question of whether the term is claiming visibility a body does not have.
 *  Part E - the weight sweep.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.PhaseCheck
 */
public final class PhaseCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    // Synthetic reference chart - 1984-09-08 07:33 UT, 41.8781 N 87.6298 W.
    // Not anyone's real birth data; an exact time is all these checks need.
    private static final int    NATAL_Y = 1984;
    private static final int    NATAL_M = 9;
    private static final int    NATAL_D = 8;
    private static final double NATAL_UT = 7.0 + 33.0 / 60.0;
    private static final double NATAL_LAT = 41.8781;
    private static final double NATAL_LON = -87.6298;

    private static final int SAMPLE = 324;
    private static final long SEED = 20260810L;

    /** The five bodies the phase applies to. */
    private static final String[] PHASED = {"Saturn", "Jupiter", "Mars", "Venus", "Mercury"};

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        SwissEph sw = new SwissEph(EPHE_PATH);

        System.out.println("=== Part A: the measurement ===");
        measurement();

        System.out.println();
        System.out.println("=== Part B: the superior/inferior split, from the astronomy ===");
        split(sw);

        System.out.println();
        System.out.println("=== Part C: the term in isolation ===");
        double jd = new SweDate(NATAL_Y, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
        ChartFrame natal = ChartFrame.compute(sw, jd, NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        isolation(natal);

        System.out.println();
        System.out.println("=== Part D: corpus measurement ===");
        List<ChartFrame> corpus = sample(sw);
        corpusMeasurement(corpus);

        System.out.println();
        System.out.println("=== Part E: the weight sweep ===");
        sweep(corpus);

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
     * Oriental means the body rises BEFORE the Sun, which means it precedes the Sun in
     * zodiacal order, which means a smaller longitude. Built from constructed positions so
     * the expected answer comes from that sentence and not from running the code.
     */
    private static void measurement() {
        double sun = 100.0;
        for (String b : PHASED) {
            eq(b + " 10 deg behind the Sun is oriental", Sect.Phase.ORIENTAL,
                Sect.phaseOf(b, sun - 10.0, sun));
            eq(b + " 10 deg ahead of the Sun is occidental", Sect.Phase.OCCIDENTAL,
                Sect.phaseOf(b, sun + 10.0, sun));
        }

        // Across 0 degrees, where a naive subtraction without normalisation flips.
        eq("oriental still reads oriental across 0 Aries", Sect.Phase.ORIENTAL,
            Sect.phaseOf("Mars", 355.0, 5.0));
        eq("occidental still reads occidental across 0 Aries", Sect.Phase.OCCIDENTAL,
            Sect.phaseOf("Mars", 5.0, 355.0));

        // The bodies the question does not apply to. NOT_APPLICABLE rather than a
        // defaulted OCCIDENTAL, so nothing can read a positive claim out of an absence.
        for (String b : new String[]{"Sun", "Moon", "Uranus", "Neptune", "Pluto", "Chiron",
                                     "North Node"}) {
            eq(b + " has no solar phase", Sect.Phase.NOT_APPLICABLE,
                Sect.phaseOf(b, 50.0, 100.0));
        }

        // One definition of which side is which. If phaseOf ever stops agreeing with
        // isOriental, Mercury's SECT and Mercury's CONDITION are reading the same fact two
        // ways, which is this project's most-repeated defect.
        for (double d = 0.5; d < 360.0; d += 7.0) {
            eq("phaseOf agrees with isOriental at " + d,
                Sect.isOriental(d, 100.0) ? Sect.Phase.ORIENTAL : Sect.Phase.OCCIDENTAL,
                Sect.phaseOf("Venus", d, 100.0));
        }
    }

    // ---------------------------------------------------------------- part B

    /**
     * The split is superior planets oriental, inferior planets occidental. Rather than
     * assert that against a retyped list - which would only prove the table equals itself -
     * this derives SUPERIOR and INFERIOR from the ephemeris, by the one property that
     * distinguishes them: an inferior planet never gets far from the Sun.
     *
     * Venus tops out near 47 degrees of elongation and Mercury near 28. The superiors reach
     * opposition, 180. So a year of sampling separates the two groups with an enormous gap
     * and no table to mistype.
     */
    private static void split(SwissEph sw) {
        Map<String, Double> maxElongation = new LinkedHashMap<>();
        for (String b : PHASED) {
            maxElongation.put(b, 0.0);
        }
        // Two years at 5-day steps: long enough for the superiors to reach opposition.
        double start = new SweDate(2024, 1, 1, 0.0).getJulDay();
        for (int i = 0; i < 146; i++) {
            ChartFrame f = ChartFrame.compute(sw, start + i * 5.0, 0.0, 0.0, 'W', false, 0.0);
            double sun = f.body("Sun").lon;
            for (String b : PHASED) {
                ChartFrame.Body body = f.body(b);
                if (body == null || !body.ok) {
                    continue;
                }
                double sep = ChartFrame.separation(body.lon, sun);
                if (sep > maxElongation.get(b)) {
                    maxElongation.put(b, sep);
                }
            }
        }
        System.out.println("  greatest elongation observed: " + round(maxElongation));

        for (String b : PHASED) {
            boolean inferior = maxElongation.get(b) < 90.0;
            // The claim: a planet that can never be far from the Sun rejoices occidental,
            // and one that can reach opposition rejoices oriental.
            yes(b + (inferior ? " is inferior and must rejoice occidental"
                              : " is superior and must rejoice oriental"),
                BodyScore.rejoicesOriental(b) == !inferior);
        }

        // And the groups really did separate, so the test above was not vacuous - if every
        // body scored the same way this would pass while proving nothing.
        yes("Mercury and Venus stayed inside 50 degrees",
            maxElongation.get("Mercury") < 50.0 && maxElongation.get("Venus") < 50.0);
        yes("Saturn, Jupiter and Mars all reached past 120 degrees",
            maxElongation.get("Saturn") > 120.0 && maxElongation.get("Jupiter") > 120.0
                && maxElongation.get("Mars") > 120.0);
    }

    // ---------------------------------------------------------------- part C

    private static void isolation(ChartFrame f) {
        List<BodyScore.Vector> with = BodyScore.rank(f);

        BodyScore.weightPhaseFavourable = 0.0;
        BodyScore.weightPhaseContrary = 0.0;
        List<BodyScore.Vector> without = BodyScore.rank(f);
        BodyScore.resetWeights();

        Map<String, BodyScore.Vector> base = new LinkedHashMap<>();
        for (BodyScore.Vector v : without) {
            base.put(v.body, v);
        }

        for (BodyScore.Vector v : with) {
            BodyScore.Vector b = base.get(v.body);
            boolean applies = v.phase != Sect.Phase.NOT_APPLICABLE;
            eq(v.body + " phase applies iff it is one of the five", applies,
                java.util.Arrays.asList(PHASED).contains(v.body));
            if (applies) {
                yes(v.body + " gives a phase reason",
                    v.conditionReasons.stream().anyMatch(s -> s.contains("the phase it rejoices in")));
                double delta = v.conditionPartial - b.conditionPartial;
                boolean favoured = BodyScore.rejoicesOriental(v.body)
                    == (v.phase == Sect.Phase.ORIENTAL);
                // Clamping to 0..1 can eat part of the move, so this asserts direction and
                // a bound rather than an exact figure.
                yes(v.body + " moves the right way (" + delta + ")",
                    favoured ? delta > 0.0 : delta < 0.0);
                yes(v.body + " moves by no more than the weight",
                    Math.abs(delta) <= BodyScore.weightPhaseFavourable + 1e-9);
            } else {
                yes(v.body + " untouched", Math.abs(v.conditionPartial - b.conditionPartial) < 1e-12);
            }
        }

        // Condition feeds no term of prominence.
        for (int i = 0; i < with.size(); i++) {
            eq("ranking unchanged at " + i, without.get(i).body, with.get(i).body);
        }

        // The Mercury case, stated as a check so it cannot be mistaken for a defect later:
        // its phase decides its SECT in Sect and its CONDITION here, and the two verdicts
        // are allowed to point opposite ways.
        BodyScore.Vector merc = null;
        for (BodyScore.Vector v : with) {
            if (v.body.equals("Mercury")) {
                merc = v;
            }
        }
        if (merc != null) {
            boolean oriental = merc.phase == Sect.Phase.ORIENTAL;
            eq("Mercury's valence read the same phase as its condition",
                oriental, Sect.isOriental(merc.longitude, f.body("Sun").lon));
            eq("Mercury is in sect exactly when its phase matches the chart's",
                f.diurnal == oriental,
                merc.valence.membership == Sect.Membership.OF_SECT);
            System.out.printf("  Mercury %s in a %s chart: %s by sect, %s by phase%n",
                oriental ? "oriental" : "occidental", f.diurnal ? "day" : "night",
                merc.valence.membership == Sect.Membership.OF_SECT ? "OF SECT" : "CONTRARY",
                BodyScore.rejoicesOriental("Mercury") == oriental ? "FAVOURED" : "CONTRARY");
        }

        System.out.print("  natal phases: ");
        for (BodyScore.Vector v : with) {
            if (v.phase != Sect.Phase.NOT_APPLICABLE) {
                System.out.printf("%s %s  ", v.body,
                    v.phase == Sect.Phase.ORIENTAL ? "orient" : "occid");
            }
        }
        System.out.println();
    }

    // ---------------------------------------------------------------- part D

    private static List<ChartFrame> sample(SwissEph sw) {
        Random rnd = new Random(SEED);
        List<ChartFrame> out = new ArrayList<>();
        for (int i = 0; i < SAMPLE; i++) {
            int year = 1950 + rnd.nextInt(80);
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

    private static void corpusMeasurement(List<ChartFrame> corpus) {
        Map<String, Integer> favoured = new LinkedHashMap<>();
        Map<String, Integer> total = new LinkedHashMap<>();
        for (String b : PHASED) {
            favoured.put(b, 0);
            total.put(b, 0);
        }
        int invisible = 0;
        int scored = 0;

        for (ChartFrame f : corpus) {
            for (BodyScore.Vector v : BodyScore.rank(f)) {
                if (v.phase == Sect.Phase.NOT_APPLICABLE) {
                    continue;
                }
                scored++;
                total.merge(v.body, 1, Integer::sum);
                if (BodyScore.rejoicesOriental(v.body) == (v.phase == Sect.Phase.ORIENTAL)) {
                    favoured.merge(v.body, 1, Integer::sum);
                }
                // Combust or under the beams: the body is not visible at all, so calling
                // it a morning or evening star is a statement about geometry the sky does
                // not show. Lilly scores the two rows independently and so does this, but
                // the size of the overlap decides whether that is defensible.
                if (v.solar == Aspects.Solar.COMBUST || v.solar == Aspects.Solar.CAZIMI
                    || v.solar == Aspects.Solar.UNDER_BEAMS || v.underBeams) {
                    invisible++;
                }
            }
        }

        System.out.printf("  %d charts, %d phase-bearing bodies%n", corpus.size(), scored);
        for (String b : PHASED) {
            int t = total.get(b);
            System.out.printf("  %-8s favoured %3d of %3d  (%.1f%%)  rejoices %s%n",
                b, favoured.get(b), t, t == 0 ? 0.0 : 100.0 * favoured.get(b) / t,
                BodyScore.rejoicesOriental(b) ? "oriental" : "occidental");
        }
        System.out.printf("  invisible while scored (combust, cazimi or under beams) %d of %d (%.1f%%)%n",
            invisible, scored, 100.0 * invisible / scored);

        // A body is on one side of the Sun or the other, so over a large corpus the split
        // must land near even. A lopsided result would mean phaseOf has a boundary bug,
        // not that the solar system is lopsided.
        int allFavoured = favoured.values().stream().mapToInt(Integer::intValue).sum();
        double pct = 100.0 * allFavoured / scored;
        yes(String.format("the favoured/contrary split is near even (%.1f%%)", pct),
            pct > 40.0 && pct < 60.0);
    }

    // ---------------------------------------------------------------- part E

    private static void sweep(List<ChartFrame> corpus) {
        BodyScore.weightPhaseFavourable = 0.0;
        BodyScore.weightPhaseContrary = 0.0;
        List<List<Topics.Topic>> base = analyseAll(corpus);

        double[] candidates = {0.04, 0.08, 0.10, 0.12, 0.15, 0.20, 0.30};
        System.out.println("  weight   standings  strengths  loudest-topic changes");
        for (double w : candidates) {
            BodyScore.weightPhaseFavourable = w;
            BodyScore.weightPhaseContrary = -w;
            int[] d = diff(base, analyseAll(corpus));
            System.out.printf("  %5.2f %s   %6d %10d %13d%n",
                w, w == 0.12 ? "*" : " ", d[0], d[1], d[2]);
        }
        BodyScore.resetWeights();

        int[] shipped = diff(base, analyseAll(corpus));
        System.out.printf("  at the shipped weight: %d standings, %d strengths, %d loudest-topic changes%n",
            shipped[0], shipped[1], shipped[2]);
        yes("the phase term reaches L6 at all", shipped[0] > 0);
    }

    private static List<List<Topics.Topic>> analyseAll(List<ChartFrame> corpus) {
        List<List<Topics.Topic>> out = new ArrayList<>();
        for (ChartFrame f : corpus) {
            out.add(Topics.analyse(f, BodyScore.rank(f)));
        }
        return out;
    }

    /** {standing flips, strengths moved, charts whose loudest topic changed}. */
    private static int[] diff(List<List<Topics.Topic>> was, List<List<Topics.Topic>> now) {
        int standingFlips = 0;
        int strengthMoved = 0;
        int loudestChanged = 0;
        for (int i = 0; i < now.size(); i++) {
            // Ranked order out, house order for the comparison - see JoyCheck.diff.
            List<Topics.Topic> a = Topics.inHouseOrder(was.get(i));
            List<Topics.Topic> b = Topics.inHouseOrder(now.get(i));
            for (int h = 0; h < b.size(); h++) {
                if (a.get(h).houseWitness.standing != b.get(h).houseWitness.standing
                    || a.get(h).rulerWitness.standing != b.get(h).rulerWitness.standing) {
                    standingFlips++;
                }
                if (Math.abs(a.get(h).strength - b.get(h).strength) > 1e-12) {
                    strengthMoved++;
                }
            }
            if (was.get(i).get(0).house != now.get(i).get(0).house) {
                loudestChanged++;
            }
        }
        return new int[]{standingFlips, strengthMoved, loudestChanged};
    }

    // ---------------------------------------------------------------- harness

    private static Map<String, String> round(Map<String, Double> m) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : m.entrySet()) {
            out.put(e.getKey(), String.format("%.0f", e.getValue()));
        }
        return out;
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }
}
