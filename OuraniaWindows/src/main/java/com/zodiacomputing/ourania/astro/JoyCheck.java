package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Verification and measurement for the L3 planetary-joy term.
 *
 *  Part A - the table itself, checked by a property rather than by retyping it. The joys
 *           are not seven arbitrary assignments: the diurnal planets rejoice above the
 *           horizon and the nocturnal ones below, with Mercury on the hinge. A single
 *           mistyped house breaks that, which no amount of re-reading the table would.
 *  Part B - the term in isolation on a live chart: flagged bodies really are in their joy
 *           house, and the condition delta is exactly weightJoy and nothing else.
 *  Part C - corpus measurement. Incidence, the whole-sign against quadrant disagreement
 *           that decided which house form this reads, and the overlap with angularity.
 *  Part D - what the term actually changes downstream, which is L6 topic standings. This
 *           is the number that says whether the term earns its place; the check count does
 *           not, and neither does the incidence.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.JoyCheck
 */
public final class JoyCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    // Synthetic reference chart - 1984-09-08 07:33 UT, 41.8781 N 87.6298 W. Not anyone's
    // real birth data. Same chart as TopicCheck uses - Part D's topic numbers are only
    // comparable against that suite if both read the same chart, so change them together.
    private static final int    NATAL_Y = 1984;
    private static final int    NATAL_M = 9;
    private static final int    NATAL_D = 8;
    private static final double NATAL_UT = 7.0 + 33.0 / 60.0;
    private static final double NATAL_LAT = 41.8781;
    private static final double NATAL_LON = -87.6298;

    /** Corpus size and seed. Fixed so two runs of this suite are comparable. */
    private static final int SAMPLE = 324;
    private static final long SEED = 20260810L;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(EPHE_PATH);

        System.out.println("=== Part A: the joy table ===");
        table();

        System.out.println();
        System.out.println("=== Part B: the term in isolation ===");
        double natalJd = new SweDate(NATAL_Y, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
        ChartFrame natal =
            ChartFrame.compute(sw, natalJd, NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        isolation(natal);

        System.out.println();
        System.out.println("=== Part C: corpus measurement ===");
        List<ChartFrame> corpus = sample(sw);
        corpusMeasurement(corpus);

        System.out.println();
        System.out.println("=== Part D: what it changes downstream ===");
        downstream(corpus);

        System.out.println();
        System.out.println("=== Part E: is 0.12 on a cliff? ===");
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
     * The sect property, which is the joy scheme's actual structure.
     *
     * Diurnal planets - Sun, Jupiter, Saturn - rejoice in houses 7 to 12, above the
     * horizon where the Sun is in a day chart. Nocturnal ones - Moon, Venus, Mars -
     * rejoice in 1 to 6, below it. Mercury takes the 1st, on the horizon itself, which is
     * the same neither-side placement it holds in Sect.
     *
     * This is the joy table's equivalent of the Egyptian bounds' planetary-years checksum:
     * a property the table must satisfy, so a transcription slip fails a check instead of
     * quietly rescoring every chart that has that body in that house.
     */
    private static void table() {
        String[] diurnal = {"Sun", "Jupiter", "Saturn"};
        String[] nocturnal = {"Moon", "Venus", "Mars"};

        for (String b : diurnal) {
            int h = BodyScore.joyHouse(b);
            yes(b + " rejoices above the horizon (h" + h + ")", h >= 7 && h <= 12);
        }
        for (String b : nocturnal) {
            int h = BodyScore.joyHouse(b);
            yes(b + " rejoices below the horizon (h" + h + ")", h >= 1 && h <= 6);
        }
        eq("Mercury rejoices on the hinge", 1, BodyScore.joyHouse("Mercury"));

        // Exactly the traditional seven have a joy, and no more.
        int withJoy = 0;
        for (String b : Dignity.TRADITIONAL) {
            if (BodyScore.joyHouse(b) != 0) {
                withJoy++;
            }
        }
        eq("all seven traditional bodies have a joy", 7, withJoy);
        for (String b : new String[]{"Uranus", "Neptune", "Pluto", "Chiron", "North Node"}) {
            eq(b + " has no joy", 0, BodyScore.joyHouse(b));
        }

        // The two directions of the table must invert each other, and no house may hold
        // two joys.
        int housesWithJoy = 0;
        for (int h = 1; h <= 12; h++) {
            String b = BodyScore.joyOfHouse(h);
            if (b != null) {
                housesWithJoy++;
                eq("joyHouse inverts joyOfHouse at h" + h, h, BodyScore.joyHouse(b));
            }
        }
        eq("seven distinct houses hold a joy", 7, housesWithJoy);
        eq("no joy outside 1..12 (h0)", null, BodyScore.joyOfHouse(0));
        eq("no joy outside 1..12 (h13)", null, BodyScore.joyOfHouse(13));

        StringBuilder sb = new StringBuilder("  joys: ");
        for (int h = 1; h <= 12; h++) {
            if (BodyScore.joyOfHouse(h) != null) {
                sb.append(BodyScore.joyOfHouse(h)).append(" ").append(Zodiac.ordinal(h)).append("  ");
            }
        }
        System.out.println(sb.toString().trim());
    }

    // ---------------------------------------------------------------- part B

    /**
     * The flag agrees with the table, and the score moves by exactly weightJoy.
     *
     * The delta is measured by re-ranking with weightJoy at zero rather than by
     * recomputing the expected condition here. Recomputing it would be a second copy of
     * computeCondition, which is the duplication this codebase keeps getting caught by -
     * and a copy in a test is worse, because it passes whatever the real one does.
     */
    private static void isolation(ChartFrame f) {
        List<BodyScore.Vector> withJoy = BodyScore.rank(f);

        BodyScore.weightJoy = 0.0;
        List<BodyScore.Vector> without = BodyScore.rank(f);
        BodyScore.resetWeights();

        Map<String, BodyScore.Vector> base = new LinkedHashMap<>();
        for (BodyScore.Vector v : without) {
            base.put(v.body, v);
        }

        for (BodyScore.Vector v : withJoy) {
            BodyScore.Vector b = base.get(v.body);
            if (v.inJoy) {
                eq(v.body + " flagged in joy really is in its joy house",
                    BodyScore.joyHouse(v.body), v.wholeSignHouse);
                yes(v.body + " gives a joy reason",
                    v.conditionReasons.stream().anyMatch(s -> s.startsWith("in its joy")));
                // Clamping at 1.0 can eat part of the delta, so this asserts the weaker
                // true statement: the term moved the score up, by no more than its weight.
                double delta = v.conditionPartial - b.conditionPartial;
                yes(v.body + " condition rose by at most weightJoy (" + delta + ")",
                    delta > 0.0 && delta <= BodyScore.weightJoy + 1e-9);
            } else {
                yes(v.body + " not in joy leaves condition untouched",
                    Math.abs(v.conditionPartial - b.conditionPartial) < 1e-12);
                yes(v.body + " not in joy gives no joy reason",
                    v.conditionReasons.stream().noneMatch(s -> s.startsWith("in its joy")));
            }
        }

        // Condition feeds no term of prominence, so the ranking must be untouched. If this
        // ever fails, the two axes have been wired together and the vector has collapsed.
        for (int i = 0; i < withJoy.size(); i++) {
            eq("ranking unchanged at " + i, without.get(i).body, withJoy.get(i).body);
        }

        List<String> inJoy = new ArrayList<>();
        for (BodyScore.Vector v : withJoy) {
            if (v.inJoy) {
                inJoy.add(v.body + " h" + v.wholeSignHouse);
            }
        }
        System.out.println("  natal chart: " + (inJoy.isEmpty() ? "no body in its joy" : inJoy));
    }

    // ---------------------------------------------------------------- part C

    /**
     * Placidus frames, deliberately. A whole-sign frame makes houseOf and wholeSignHouse
     * return the same number, so a corpus built the way Calibration builds one could not
     * see the disagreement this part exists to measure.
     */
    private static List<ChartFrame> sample(SwissEph sw) {
        Random rnd = new Random(SEED);
        List<ChartFrame> out = new ArrayList<>();
        for (int i = 0; i < SAMPLE; i++) {
            int year = 1950 + rnd.nextInt(80);
            int month = 1 + rnd.nextInt(12);
            int day = 1 + rnd.nextInt(28);
            double hour = rnd.nextDouble() * 24.0;
            // Placidus degenerates at high latitude, so this stays inside the band where
            // quadrant cusps are defined at all - otherwise the disagreement measured
            // below would be partly an artefact of the house system failing.
            double lat = -60.0 + rnd.nextDouble() * 120.0;
            double lon = -180.0 + rnd.nextDouble() * 360.0;
            SweDate sd = new SweDate(year, month, day, hour);
            out.add(ChartFrame.compute(sw, sd.getJulDay(), lat, lon, 'P', false, 0.0));
        }
        return out;
    }

    private static void corpusMeasurement(List<ChartFrame> corpus) {
        Map<String, Integer> incidence = new LinkedHashMap<>();
        for (String b : Dignity.TRADITIONAL) {
            incidence.put(b, 0);
        }
        int joysWhole = 0;
        int joysQuadrant = 0;
        int both = 0;
        int wholeOnly = 0;
        int quadrantOnly = 0;
        int joyAndAngular = 0;
        int chartsWithAny = 0;

        for (ChartFrame f : corpus) {
            boolean any = false;
            for (BodyScore.Vector v : BodyScore.rank(f)) {
                int jh = BodyScore.joyHouse(v.body);
                if (jh == 0) {
                    continue;
                }
                boolean whole = v.wholeSignHouse == jh;
                boolean quad = v.house == jh;
                if (whole) {
                    joysWhole++;
                    incidence.merge(v.body, 1, Integer::sum);
                    any = true;
                    if (v.angularity > 0.0) {
                        joyAndAngular++;
                    }
                }
                if (quad) {
                    joysQuadrant++;
                }
                if (whole && quad) {
                    both++;
                } else if (whole) {
                    wholeOnly++;
                } else if (quad) {
                    quadrantOnly++;
                }
                // The flag must agree with the whole-sign reading on every body of every
                // chart, not only on the one natal chart Part B looked at.
                checks++;
                if (v.inJoy != whole) {
                    failures.add(v.body + " inJoy disagrees with the whole-sign house");
                }
            }
            if (any) {
                chartsWithAny = chartsWithAny + 1;
            }
        }

        int opportunities = corpus.size() * 7;
        System.out.printf("  %d charts, %d body-slots that could hold a joy%n",
            corpus.size(), opportunities);
        System.out.printf("  joys by whole sign     %4d  (%.1f%%, chance is 8.3%%)%n",
            joysWhole, 100.0 * joysWhole / opportunities);
        System.out.printf("  joys by quadrant       %4d  (%.1f%%)%n",
            joysQuadrant, 100.0 * joysQuadrant / opportunities);
        // Stated three ways on purpose. "The forms disagree on N" is ambiguous about its
        // denominator, and the work plan's rule about totals applies to ratios too: say
        // what a number is over, or it means nothing.
        System.out.printf("  both forms agree it is a joy   %4d%n", both);
        System.out.printf("  whole sign only (quadrant says no) %3d  (%.1f%% of whole-sign joys)%n",
            wholeOnly, joysWhole == 0 ? 0.0 : 100.0 * wholeOnly / joysWhole);
        System.out.printf("  quadrant only (whole sign says no) %3d  (%.1f%% of quadrant joys)%n",
            quadrantOnly, joysQuadrant == 0 ? 0.0 : 100.0 * quadrantOnly / joysQuadrant);
        System.out.printf("  charts with at least one joy  %d of %d (%.1f%%)%n",
            chartsWithAny, corpus.size(), 100.0 * chartsWithAny / corpus.size());
        System.out.printf("  in joy AND scoring angularity %d of %d (%.1f%%) - the double-count risk%n",
            joyAndAngular, joysWhole, joysWhole == 0 ? 0.0 : 100.0 * joyAndAngular / joysWhole);
        System.out.println("  incidence per body: " + incidence);

        // Roughly one slot in twelve, for seven bodies over a corpus this size. A wild
        // departure means the house lookup is wrong, not that the sky is unusual.
        yes("whole-sign joy rate is near chance",
            joysWhole > opportunities / 20 && joysWhole < opportunities / 6);
    }

    // ---------------------------------------------------------------- part D

    /**
     * The only downstream consumer of conditionPartial is L6, which reads it as a witness
     * strength and buckets it into a Standing. So the honest question about this term is
     * how many standings it moves - not how many bodies it flags.
     */
    private static void downstream(List<ChartFrame> corpus) {
        BodyScore.weightJoy = 0.0;
        List<List<Topics.Topic>> before = analyseAll(corpus);
        BodyScore.resetWeights();
        List<List<Topics.Topic>> after = analyseAll(corpus);

        int[] d = diff(before, after);
        int topics = corpus.size() * 12;

        System.out.printf("  %d topics across %d charts%n", topics, corpus.size());
        System.out.printf("  topic strength moved   %4d  (%.1f%%)%n",
            d[1], 100.0 * d[1] / topics);
        System.out.printf("  witness standing flipped %2d  (%.1f%%)%n",
            d[0], 100.0 * d[0] / topics);
        System.out.printf("  loudest topic changed    %2d charts of %d (%.1f%%)%n",
            d[2], corpus.size(), 100.0 * d[2] / corpus.size());
        yes("the joy term reaches L6 at all", d[0] > 0);
    }

    /** Ranked topics per chart, at whatever the weights currently are. */
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
            eq("topic count is stable", was.get(i).size(), now.get(i).size());
            // analyse returns RANKED order, so the per-topic comparison is done in HOUSE
            // order. Comparing position against position would silently treat a
            // reordering as no change on both sides at once.
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

    // ---------------------------------------------------------------- part E

    /**
     * The weight sweep the work plan's threshold rule asks for: a value that changes the
     * answer when nudged is on a cliff and is wrong however reasonable it sounded.
     *
     * Everything is measured against the SAME weightJoy = 0 baseline rather than against
     * the neighbouring row, so each line reads as "what this weight does to a chart that
     * has no joy term", which is the comparison that decides whether 0.12 sits somewhere
     * flat.
     */
    private static void sweep(List<ChartFrame> corpus) {
        BodyScore.weightJoy = 0.0;
        List<List<Topics.Topic>> base = analyseAll(corpus);

        double[] candidates = {0.04, 0.08, 0.10, 0.12, 0.15, 0.20, 0.30};
        System.out.println("  weight   standings  strengths  loudest-topic changes");
        for (double w : candidates) {
            BodyScore.weightJoy = w;
            int[] d = diff(base, analyseAll(corpus));
            System.out.printf("  %5.2f %s   %6d %10d %13d%n",
                w, w == 0.12 ? "*" : " ", d[0], d[1], d[2]);
        }
        BodyScore.resetWeights();

        // The sweep is a measurement, not a threshold test - there is no single right
        // answer to assert. What must hold is that the term is monotone in its weight:
        // a bigger joy bonus cannot disturb FEWER standings than a smaller one.
        BodyScore.weightJoy = 0.04;
        int small = diff(base, analyseAll(corpus))[0];
        BodyScore.weightJoy = 0.30;
        int large = diff(base, analyseAll(corpus))[0];
        BodyScore.resetWeights();
        yes("more weight disturbs at least as many standings (" + small + " -> " + large + ")",
            large >= small);
    }

    // ---------------------------------------------------------------- harness

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
