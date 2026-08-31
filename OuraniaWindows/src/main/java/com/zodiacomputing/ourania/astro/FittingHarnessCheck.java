package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

/**
 * Verifies the fitting harness against data whose correct answer is known.
 *
 * A fitting harness cannot be checked against real expert picks, because if it disagrees
 * you cannot tell whether the harness is broken or the engine is wrong. So: generate a
 * synthetic corpus whose "expert picks" were produced by a KNOWN weight set that is not
 * the default, then check the harness recovers weights that reproduce those picks on
 * data it never saw.
 *
 * Two properties are asserted:
 *
 *   RECOVERY - fitting on a corpus generated from non-default weights must beat the
 *       defaults on the held-out split. If it cannot recover a signal that is there by
 *       construction, it will certainly not find one that is not.
 *
 *   NO FALSE SIGNAL - fitting on a corpus of RANDOM picks must not beat the defaults
 *       out of sample by any meaningful margin. If it does, the harness manufactures
 *       results from noise and nothing it reports can be trusted.
 *
 * The second is the more important one.
 */
public final class FittingHarnessCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;
    private static final int CORPUS_SIZE = 60;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), "ourania-fitcheck");
        dir.mkdirs();
        SwissEph sw = new SwissEph(EPHE_PATH);

        // Ground truth: a weight set deliberately far from the defaults.
        double[] truth = {15.0, 1.0, 0.2, 1.20, 0.90, 0.05};

        System.out.println("=== Part A: template generation ===");
        File tpl = new File(dir, "template.tsv");
        if (tpl.exists()) {
            tpl.delete();
        }
        FittingHarness.main(new String[] {tpl.getAbsolutePath()});
        eq("template written", true, tpl.exists());
        eq("template parses to zero usable entries", null, FittingHarness.run(tpl));

        // Pin the candidate set for the whole synthetic run. The corpus is written from
        // it and the fitter must score over the same one, or the ground truth and the
        // question asked of it are two different questions - the fitter would be marked
        // wrong for naming a body the corpus was never allowed to pick.
        FittingHarness.candidates = HARNESS_CANDIDATES;
        try {

        System.out.println();
        System.out.println("=== Part B: recovery of a known signal ===");
        File signal = new File(dir, "signal.tsv");
        writeCorpus(sw, signal, truth, false);
        FittingHarness.Outcome sig = FittingHarness.run(signal);
        eq("signal corpus produced an outcome", true, sig != null);
        if (sig != null) {
            // NOT all charts survive, and that is the correct behaviour: a chart whose
            // top-3 flips across +/- 5 minutes cannot test angle-sensitive weights even
            // with a perfect birth time. The surviving fraction is a real constraint on
            // corpus size and is reported rather than asserted away.
            System.out.printf("  stability filter kept %d of %d (%.0f%%) - budget corpus size accordingly%n",
                sig.usable, CORPUS_SIZE, 100.0 * sig.usable / CORPUS_SIZE);
            checks++;
            if (sig.usable < CORPUS_SIZE / 4) {
                failures.add("stability filter rejected almost everything: " + sig.usable
                    + " of " + CORPUS_SIZE);
            }
            checks++;
            if (sig.tunedTest <= sig.defaultTest) {
                failures.add(String.format(
                    "recovery failed: tuned %.3f did not beat default %.3f on held-out data",
                    sig.tunedTest, sig.defaultTest));
            }
            checks++;
            if (sig.tunedTest < 0.75) {
                failures.add(String.format(
                    "recovery weak: held-out score %.3f on a corpus generated from fixed weights",
                    sig.tunedTest));
            }
            System.out.printf("  recovery: default %.3f -> tuned %.3f on held-out%n",
                sig.defaultTest, sig.tunedTest);
        }

        System.out.println();
        System.out.println("=== Part C: no false signal from noise ===");
        File noise = new File(dir, "noise.tsv");
        writeCorpus(sw, noise, truth, true);
        FittingHarness.Outcome noi = FittingHarness.run(noise);
        eq("noise corpus produced an outcome", true, noi != null);
        if (noi != null) {
            checks++;
            double outOfSampleGain = noi.tunedTest - noi.defaultTest;
            if (outOfSampleGain > 0.20) {
                failures.add(String.format(
                    "harness manufactured a %.3f out-of-sample gain from random picks",
                    outOfSampleGain));
            }
            // The train-test gap is reported, not asserted. On a small corpus it swings
            // either way from sampling variance alone, so a negative gap is noise rather
            // than a defect. The assertion that matters is the one above: no out-of-
            // sample gain from random picks.
            double gap = noi.tunedTrain - noi.tunedTest;
            System.out.printf("  noise: default %.3f -> tuned %.3f held-out "
                + "(train %.3f, train-test gap %+.3f)%n",
                noi.defaultTest, noi.tunedTest, noi.tunedTrain, gap);
            System.out.println("  No held-out gain from random picks is the result that matters:");
            System.out.println("  the harness does not manufacture signal where none exists.");
        }

        } finally {
            // Never leave it set: FittingHarness is also run directly against the real
            // expert corpus, and that must see every body.
            FittingHarness.candidates = null;
        }

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

    /**
     * Writes a synthetic corpus. Picks are either the top-3 under the ground-truth
     * weights, or three random bodies when scrambled is set.
     *
     * Times are chosen on the hour at temperate latitudes so the charts survive the
     * harness's own stability filter - the point here is to test the fitter, not the
     * filter, which Part A of SectCheck already covers.
     */
    /**
     * The candidate set this self-test measures over, pinned so the question stays the same
     * one from run to run.
     *
     * These are the twelve points the wheel carried before the Bodies registry existed. The
     * particular list matters far less than the fact that it does not move: the score below
     * is only meaningful compared with its own previous value, and it was silently comparing
     * across three different candidate counts in two days.
     *
     * Do not swap this for Bodies.count() or for a filtered view of the registry. That is
     * precisely the coupling being removed.
     */
    static final Set<String> HARNESS_CANDIDATES = new LinkedHashSet<>(Arrays.asList(
        "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter",
        "Saturn", "Uranus", "Neptune", "Pluto", "North Node", "Chiron"));

    private static void writeCorpus(SwissEph sw, File file, double[] truth, boolean scrambled)
            throws IOException {
        Random rnd = new Random(scrambled ? 99L : 7L);
        BodyScore.resetWeights();
        BodyScore.angularityOrb = truth[0];
        BodyScore.weightAscMc = truth[1];
        BodyScore.weightDscIc = truth[2];
        BodyScore.weightChartRuler = truth[3];
        BodyScore.weightSectLight = truth[4];
        BodyScore.weightOutOfSectMalefic = truth[5];

        try (PrintWriter w = new PrintWriter(file, "UTF-8")) {
            w.println("# synthetic corpus - " + (scrambled ? "RANDOM picks" : "picks from known weights"));
            int made = 0;
            int attempt = 0;
            while (made < CORPUS_SIZE && attempt < CORPUS_SIZE * 20) {
                attempt++;
                int year = 1940 + rnd.nextInt(70);
                int month = 1 + rnd.nextInt(12);
                int day = 1 + rnd.nextInt(28);
                int hour = rnd.nextInt(24);
                double lat = 25.0 + rnd.nextDouble() * 30.0;
                double lon = -120.0 + rnd.nextDouble() * 140.0;

                SweDate sd = new SweDate(year, month, day, hour);
                ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), lat, lon, 'W', false, 0.0);
                List<BodyScore.Vector> ranked = BodyScore.rank(f);

                // Restricted to the pinned set before anything is picked, so neither the
                // ground truth nor the random control depends on how many points the
                // registry happens to hold today. rnd.nextInt(eligible.size()) with a
                // moving size was what re-rolled the whole scrambled corpus.
                List<BodyScore.Vector> eligible = new ArrayList<>();
                for (BodyScore.Vector v : ranked) {
                    if (HARNESS_CANDIDATES.contains(v.body)) {
                        eligible.add(v);
                    }
                }
                List<String> picks = new ArrayList<>();
                if (scrambled) {
                    Set<String> chosen = new LinkedHashSet<>();
                    while (chosen.size() < 3 && eligible.size() >= 3) {
                        chosen.add(eligible.get(rnd.nextInt(eligible.size())).body);
                    }
                    picks.addAll(chosen);
                } else {
                    for (BodyScore.Vector v : eligible) {
                        if (picks.size() >= 3 || v.prominence <= 0.0) {
                            break;
                        }
                        picks.add(v.body);
                    }
                }
                if (picks.size() < 3) {
                    continue;
                }
                w.printf("Synthetic %03d | %04d-%02d-%02d | %02d:00 | 0 | %.4f | %.4f | AA | synthetic | generated | %s%n",
                    made, year, month, day, hour, lat, lon, String.join(",", picks));
                made++;
            }
        }
        BodyScore.resetWeights();
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private FittingHarnessCheck() { }
}
