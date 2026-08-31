package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Fits the six invented prominence weights against a corpus of expert picks.
 *
 * What this does and does not do:
 *
 *   IT FITS the weights I made up - angularityOrb, the two angle weights, chart ruler,
 *       sect light, out-of-sect malefic. Those are placeholders and the sensitivity run
 *       showed they drive 40-53% of the ranking.
 *
 *   IT DOES NOT touch anything the tradition supplies - Lilly's dignity points, the
 *       Egyptian bounds, the Dorothean triplicities. Tuning those to make charts read
 *       better is fitting noise to a tradition.
 *
 *   IT DOES NOT test whether astrology works. The target is agreement with published
 *       professional practice, which is a fact about texts. Nothing here bears on
 *       whether a chart predicts anything.
 *
 * Method: hold out a third of the corpus, grid-search on the rest, report tuned against
 * default against three naive baselines on the untouched third. The train-test gap is
 * printed prominently because with six free parameters and a small corpus, overfitting
 * is the expected outcome rather than a risk.
 *
 *   java -cp "out;src\main\java" com.zodiacomputing.ourania.astro.FittingHarness [corpus.tsv]
 *
 * With no corpus file present it writes a template and stops.
 */
public final class FittingHarness {

    private static final String EPHE_PATH = Ephemeris.PATH;
    private static final String DEFAULT_CORPUS = "calibration-corpus.tsv";

    /** Charts whose top-3 shifts across this many minutes either way are excluded. */
    private static double stabilityMinutes = 5.0;
    /** Fraction of the corpus held out and never used for fitting. */
    private static final double HOLDOUT = 1.0 / 3.0;
    private static final long SPLIT_SEED = 20260801L;

    // Grid. angularityOrb in degrees actually used in the tradition; the rest as
    // multiples of their current placeholder value.
    private static final double[] ORB_GRID = {5.0, 8.0, 10.0, 12.0, 15.0};
    private static final double[] MULT_GRID = {0.5, 0.75, 1.0, 1.5, 2.0};

    // ------------------------------------------------------------------ corpus

    static final class Entry {
        String name;
        double julianDayUt;
        double lat;
        double lon;
        String rodden;
        String school;
        String source;
        final List<String> expertPicks = new ArrayList<>();

        ChartFrame frame;
        ChartFrame early;
        ChartFrame late;
        BodyScore.Precomputed pre;
        boolean stable;
        double ascDrift;
    }

    /** What a fitting run concluded, so it can be asserted on rather than only printed. */
    public static final class Outcome {
        public int loaded;
        public int usable;
        public int trainSize;
        public int testSize;
        public double defaultTrain;
        public double defaultTest;
        public double tunedTrain;
        public double tunedTest;
        public double bestBaselineTest;
        public double[] tuned;
    }

    public static void main(String[] args) throws IOException {
        String path = args.length > 0 ? args[0] : DEFAULT_CORPUS;
        if (args.length > 1) {
            // Tolerance is a judgement about the data, not a constant. Birth-certificate
            // times are usually recorded to the minute, so +/-5 may be over-strict.
            stabilityMinutes = Double.parseDouble(args[1]);
        }
        File file = new File(path);
        if (!file.exists()) {
            writeTemplate(file);
            System.out.println("No corpus found. Wrote a template to " + file.getAbsolutePath());
            System.out.println("Fill it in and run again.");
            return;
        }
        run(file);
    }

    /** The whole pipeline. Returns null when there is nothing usable to fit. */
    public static Outcome run(File file) throws IOException {
        Outcome o = new Outcome();
        List<Entry> corpus = parse(file);
        o.loaded = corpus.size();
        System.out.println("Loaded " + corpus.size() + " entries from " + file.getName());
        if (corpus.isEmpty()) {
            System.out.println("Nothing to fit.");
            return null;
        }

        SwissEph sw = new SwissEph(EPHE_PATH);
        List<Entry> usable = prepare(sw, corpus);
        o.usable = usable.size();
        if (usable.size() < 6) {
            System.out.println();
            System.out.println("Only " + usable.size() + " usable charts. Fitting six parameters");
            System.out.println("on this is meaningless. Collect more before drawing conclusions.");
            if (usable.isEmpty()) {
                return null;
            }
        }

        List<Entry> train = new ArrayList<>();
        List<Entry> test = new ArrayList<>();
        split(usable, train, test);
        o.trainSize = train.size();
        o.testSize = test.size();
        System.out.printf("%nSplit: %d train, %d held out (seed %d)%n",
            train.size(), test.size(), SPLIT_SEED);

        verifyFastPath(usable);
        long t0 = System.currentTimeMillis();
        double[] best = gridSearch(train);
        System.out.printf("  grid search took %.1fs%n", (System.currentTimeMillis() - t0) / 1000.0);
        report(train, test, best, o);
        return o;
    }

    // ------------------------------------------------------------------ preparation

    /**
     * Computes each chart once - the ephemeris is the expensive part, re-ranking is
     * cheap - and applies the birth-time stability filter.
     *
     * An AA Rodden rating certifies the SOURCE, not the number: delivery-room times are
     * routinely rounded. So AA is necessary and not sufficient. A chart survives only if
     * its top three are the same at minus and plus the tolerance, evaluated at default
     * weights. Charts that fail cannot test angle-sensitive parameters at all, because
     * their angles are the unreliable part.
     */
    private static List<Entry> prepare(SwissEph sw, List<Entry> corpus) {
        BodyScore.resetWeights();
        List<Entry> usable = new ArrayList<>();
        double delta = stabilityMinutes / (24.0 * 60.0);

        System.out.println();
        System.out.println("=== BIRTH-TIME STABILITY FILTER (+/- " + stabilityMinutes + " min) ===");
        for (Entry e : corpus) {
            e.frame = ChartFrame.compute(sw, e.julianDayUt, e.lat, e.lon, 'W', false, 0.0);
            e.early = ChartFrame.compute(sw, e.julianDayUt - delta, e.lat, e.lon, 'W', false, 0.0);
            e.late = ChartFrame.compute(sw, e.julianDayUt + delta, e.lat, e.lon, 'W', false, 0.0);
            e.ascDrift = ChartFrame.separation(e.early.asc, e.late.asc);

            Set<String> mid = topK(BodyScore.rank(e.frame), e.expertPicks.size());
            Set<String> lo = topK(BodyScore.rank(e.early), e.expertPicks.size());
            Set<String> hi = topK(BodyScore.rank(e.late), e.expertPicks.size());
            e.stable = mid.equals(lo) && mid.equals(hi);

            System.out.printf("  %-28s %-3s  asc drift %5.2f°  %s%n",
                truncate(e.name, 28), e.rodden, e.ascDrift,
                e.stable ? "stable" : "UNSTABLE - excluded");
            if (e.stable) {
                // Dignity and valence do not depend on any weight, so compute them once
                // here rather than 15,625 times inside the grid search.
                e.pre = BodyScore.precompute(e.frame);
                usable.add(e);
            }
        }
        System.out.printf("  %d of %d charts usable.%n", usable.size(), corpus.size());
        return usable;
    }

    private static void split(List<Entry> all, List<Entry> train, List<Entry> test) {
        List<Entry> shuffled = new ArrayList<>(all);
        Collections.shuffle(shuffled, new Random(SPLIT_SEED));
        int nTest = Math.max(1, (int) Math.round(shuffled.size() * HOLDOUT));
        test.addAll(shuffled.subList(0, nTest));
        train.addAll(shuffled.subList(nTest, shuffled.size()));
    }

    // ------------------------------------------------------------------ scoring

    /**
     * Which bodies may be picked, or null for every body the engine ranks.
     *
     * Null in normal use: fitting against the real expert corpus must see the same field of
     * candidates a reader saw. It exists for the synthetic self-test, which needs the
     * *problem* held still. That test generates a corpus from known weights and asks
     * whether the fitter can recover them, and the difficulty of recovering a top-3 depends
     * on how many bodies are competing for those three places. The registry went 23 -> 28
     * when five asteroids started computing and 28 -> 24 when the angles were dropped, so
     * the self-test's score moved twice without the fitter changing at all.
     *
     * Pinning the candidates makes that score a measurement rather than a moving target.
     * Mutable static because that is how this file already scopes BodyScore's weights, and
     * a caller that sets it must clear it in a finally.
     */
    public static Set<String> candidates = null;

    /** Whether a body is in the current candidate set. */
    private static boolean eligible(String body) {
        return candidates == null || candidates.contains(body);
    }

    /**
     * Overlap between the engine's top-k and the expert's picks, where k is however many
     * the expert named. Exact-set match is reported separately as a harsher secondary.
     */
    private static double score(Entry e, ChartFrame f) {
        int k = e.expertPicks.size();
        if (k == 0) {
            return 0.0;
        }
        Set<String> top = topK(BodyScore.rank(f), k);
        int hit = 0;
        for (String p : e.expertPicks) {
            if (top.contains(p)) {
                hit++;
            }
        }
        return (double) hit / k;
    }

    /** Fast path: same score, computed from the precomputed half. Used in the grid loop. */
    private static double scoreFast(Entry e) {
        int k = e.expertPicks.size();
        if (k == 0) {
            return 0.0;
        }
        String[] order = BodyScore.rankNames(e.pre);
        Set<String> top = new LinkedHashSet<>();
        for (String name : order) {
            if (top.size() >= k || name == null) {
                break;
            }
            // Must filter exactly as topK does. A check at the top of this file asserts
            // scoreFast equals score for every entry, so a divergence here fails loudly
            // rather than quietly fitting against a different question.
            if (!eligible(name)) {
                continue;
            }
            top.add(name);
        }
        int hit = 0;
        for (String p : e.expertPicks) {
            if (top.contains(p)) {
                hit++;
            }
        }
        return (double) hit / k;
    }

    private static double meanScore(List<Entry> set) {
        double s = 0.0;
        for (Entry e : set) {
            s += scoreFast(e);
        }
        return set.isEmpty() ? 0.0 : s / set.size();
    }

    /**
     * The fast path must agree with the full ranking exactly, or the grid search is
     * optimising something other than what the engine will actually do. Checked once per
     * run at the default weights rather than assumed.
     */
    private static void verifyFastPath(List<Entry> set) {
        BodyScore.resetWeights();
        int mismatch = 0;
        for (Entry e : set) {
            if (Math.abs(scoreFast(e) - score(e, e.frame)) > 1e-9) {
                mismatch++;
            }
        }
        if (mismatch > 0) {
            throw new IllegalStateException("fast path disagrees with full ranking on "
                + mismatch + " charts - grid search results would be meaningless");
        }
    }

    private static double exactMatchRate(List<Entry> set) {
        int n = 0;
        for (Entry e : set) {
            if (score(e, e.frame) >= 1.0 - 1e-9) {
                n++;
            }
        }
        return set.isEmpty() ? 0.0 : (double) n / set.size();
    }

    // ------------------------------------------------------------------ grid search

    private static double[] gridSearch(List<Entry> train) {
        System.out.println();
        System.out.println("=== GRID SEARCH ON THE TRAINING SPLIT ===");
        long combos = (long) ORB_GRID.length * MULT_GRID.length * MULT_GRID.length
            * MULT_GRID.length * MULT_GRID.length * MULT_GRID.length;
        System.out.println("  " + combos + " combinations x " + train.size() + " charts");

        double[] best = null;
        double bestScore = -1.0;
        int ties = 0;

        for (double orb : ORB_GRID) {
            for (double ascMc : MULT_GRID) {
                for (double dscIc : MULT_GRID) {
                    for (double ruler : MULT_GRID) {
                        for (double light : MULT_GRID) {
                            for (double malefic : MULT_GRID) {
                                BodyScore.angularityOrb = orb;
                                BodyScore.weightAscMc = 1.0 * ascMc;
                                BodyScore.weightDscIc = 0.8 * dscIc;
                                BodyScore.weightChartRuler = 0.35 * ruler;
                                BodyScore.weightSectLight = 0.25 * light;
                                BodyScore.weightOutOfSectMalefic = 0.30 * malefic;

                                double s = meanScore(train);
                                if (s > bestScore + 1e-12) {
                                    bestScore = s;
                                    best = new double[] {orb, BodyScore.weightAscMc,
                                        BodyScore.weightDscIc, BodyScore.weightChartRuler,
                                        BodyScore.weightSectLight,
                                        BodyScore.weightOutOfSectMalefic};
                                    ties = 1;
                                } else if (Math.abs(s - bestScore) < 1e-12) {
                                    ties++;
                                }
                            }
                        }
                    }
                }
            }
        }
        BodyScore.resetWeights();
        System.out.printf("  best training score %.3f, reached by %d combinations%n",
            bestScore, ties);
        if (ties > combos / 20) {
            System.out.println("  NOTE: a large share of the grid ties at the optimum, which");
            System.out.println("  means the corpus cannot distinguish these parameters.");
        }
        return best;
    }

    private static void apply(double[] w) {
        BodyScore.angularityOrb = w[0];
        BodyScore.weightAscMc = w[1];
        BodyScore.weightDscIc = w[2];
        BodyScore.weightChartRuler = w[3];
        BodyScore.weightSectLight = w[4];
        BodyScore.weightOutOfSectMalefic = w[5];
    }

    // ------------------------------------------------------------------ report

    private static void report(List<Entry> train, List<Entry> test, double[] best, Outcome o) {
        System.out.println();
        System.out.println("=== RESULTS ===");

        BodyScore.resetWeights();
        double defTrain = meanScore(train);
        double defTest = meanScore(test);
        double defExact = exactMatchRate(test);

        apply(best);
        double tunTrain = meanScore(train);
        double tunTest = meanScore(test);
        double tunExact = exactMatchRate(test);
        BodyScore.resetWeights();

        double baseAngular = baseline(test, Baseline.ANGULAR);
        double baseDignity = baseline(test, Baseline.DIGNITY);
        double baseFixed = baseline(test, Baseline.FIXED);
        double bestBase = Math.max(baseAngular, Math.max(baseDignity, baseFixed));

        o.defaultTrain = defTrain;
        o.defaultTest = defTest;
        o.tunedTrain = tunTrain;
        o.tunedTest = tunTest;
        o.bestBaselineTest = bestBase;
        o.tuned = best.clone();

        System.out.printf("  %-26s %8s %8s%n", "", "train", "HELD OUT");
        System.out.printf("  %-26s %8.3f %8.3f%n", "default weights", defTrain, defTest);
        System.out.printf("  %-26s %8.3f %8.3f%n", "tuned weights", tunTrain, tunTest);
        System.out.println();
        System.out.printf("  %-26s %17.3f%n", "baseline: most angular", baseAngular);
        System.out.printf("  %-26s %17.3f%n", "baseline: most dignified", baseDignity);
        System.out.printf("  %-26s %17.3f%n", "baseline: Sun/Moon/ruler", baseFixed);
        System.out.println();
        System.out.printf("  exact top-k match, held out: default %.1f%%, tuned %.1f%%%n",
            defExact * 100, tunExact * 100);

        System.out.println();
        System.out.println("  tuned parameters:");
        String[] names = {"angularityOrb", "weightAscMc", "weightDscIc",
                          "weightChartRuler", "weightSectLight", "weightOutOfSectMalefic"};
        double[] defs = {10.0, 1.0, 0.8, 0.35, 0.25, 0.30};
        for (int i = 0; i < names.length; i++) {
            System.out.printf("    %-24s %7.3f   (default %.3f)%s%n",
                names[i], best[i], defs[i],
                Math.abs(best[i] - defs[i]) < 1e-9 ? "" : "  <- moved");
        }

        System.out.println();
        System.out.println("  VERDICT");
        double gap = tunTrain - tunTest;
        if (tunTest <= bestBase + 1e-9) {
            System.out.println("    The tuned engine does not beat the best naive baseline on");
            System.out.println("    held-out data. Nothing has been learned. Do not ship these weights.");
        } else if (gap > 0.15) {
            System.out.printf("    Train-test gap is %.3f. That is overfitting, not learning.%n", gap);
            System.out.println("    Collect more charts before trusting the tuned values.");
        } else if (tunTest <= defTest + 1e-9) {
            System.out.println("    Tuning did not improve on the defaults out of sample.");
            System.out.println("    Keep the defaults - they are at least not fitted to noise.");
        } else {
            System.out.printf("    Tuned beats default by %.3f and the best baseline by %.3f%n",
                tunTest - defTest, tunTest - bestBase);
            System.out.printf("    on held-out data, with a train-test gap of %.3f.%n", gap);
        }

        // Per-chart detail at the tuned weights is the most diagnostically useful part:
        // it shows WHICH charts the engine reads differently from the expert.
        apply(best);
        System.out.println();
        System.out.println("=== PER-CHART, TUNED, HELD-OUT SPLIT ===");
        for (Entry e : test) {
            List<BodyScore.Vector> r = BodyScore.rank(e.frame);
            System.out.printf("  %-24s %.2f  expert %-28s engine %s%n",
                truncate(e.name, 24), score(e, e.frame),
                String.join(",", e.expertPicks),
                String.join(",", topK(r, e.expertPicks.size())));
        }
        BodyScore.resetWeights();
    }

    private enum Baseline { ANGULAR, DIGNITY, FIXED }

    private static double baseline(List<Entry> set, Baseline kind) {
        double total = 0.0;
        for (Entry e : set) {
            int k = e.expertPicks.size();
            List<BodyScore.Vector> r = BodyScore.rank(e.frame);
            Set<String> pick = new LinkedHashSet<>();
            switch (kind) {
                case ANGULAR:
                    r.sort((a, b) -> Double.compare(b.angularity, a.angularity));
                    for (BodyScore.Vector v : r) {
                        if (pick.size() >= k) {
                            break;
                        }
                        pick.add(v.body);
                    }
                    break;
                case DIGNITY:
                    r.sort((a, b) -> Integer.compare(b.dignity.score, a.dignity.score));
                    for (BodyScore.Vector v : r) {
                        if (pick.size() >= k) {
                            break;
                        }
                        pick.add(v.body);
                    }
                    break;
                default:
                    pick.add("Sun");
                    pick.add("Moon");
                    pick.add(Dignity.domicileRulerOf(Zodiac.signIndex(e.frame.asc)));
                    break;
            }
            int hit = 0;
            for (String p : e.expertPicks) {
                if (pick.contains(p)) {
                    hit++;
                }
            }
            total += k == 0 ? 0.0 : (double) hit / k;
        }
        return set.isEmpty() ? 0.0 : total / set.size();
    }

    // ------------------------------------------------------------------ io

    private static List<Entry> parse(File file) throws IOException {
        List<Entry> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = r.readLine()) != null) {
                lineNo++;
                // Strip a UTF-8 BOM. Editors on Windows add one silently, and without
                // this the first line stops looking like a comment and reports a
                // confusing field-count error.
                if (lineNo == 1 && !line.isEmpty() && line.charAt(0) == '﻿') {
                    line = line.substring(1);
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] p = line.split("\\s*\\|\\s*");
                if (p.length < 10) {
                    System.err.println("line " + lineNo + ": expected 10 fields, got " + p.length);
                    continue;
                }
                try {
                    Entry e = new Entry();
                    e.name = p[0];
                    String[] ymd = p[1].split("-");
                    String[] hm = p[2].split(":");
                    double localHour = Integer.parseInt(hm[0]) + Integer.parseInt(hm[1]) / 60.0;
                    double utcOffset = Double.parseDouble(p[3]);
                    // Build the Julian day from midnight and add hours, so an offset that
                    // pushes the moment into the previous or next day cannot roll over wrong.
                    SweDate midnight = new SweDate(Integer.parseInt(ymd[0]),
                        Integer.parseInt(ymd[1]), Integer.parseInt(ymd[2]), 0.0);
                    e.julianDayUt = midnight.getJulDay() + (localHour - utcOffset) / 24.0;
                    e.lat = Double.parseDouble(p[4]);
                    e.lon = Double.parseDouble(p[5]);
                    e.rodden = p[6];
                    e.school = p[7];
                    e.source = p[8];
                    for (String pick : p[9].split("\\s*,\\s*")) {
                        if (!pick.isEmpty()) {
                            e.expertPicks.add(pick);
                        }
                    }
                    if (!"AA".equalsIgnoreCase(e.rodden)) {
                        System.err.println("line " + lineNo + ": " + e.name
                            + " is rated " + e.rodden + ", not AA - skipped");
                        continue;
                    }
                    if (e.expertPicks.isEmpty()) {
                        System.err.println("line " + lineNo + ": " + e.name + " has no picks");
                        continue;
                    }
                    out.add(e);
                } catch (RuntimeException ex) {
                    System.err.println("line " + lineNo + ": " + ex);
                }
            }
        }
        return out;
    }

    private static void writeTemplate(File file) throws IOException {
        try (PrintWriter w = new PrintWriter(file, "UTF-8")) {
            w.println("# Calibration corpus for the L3 prominence weights.");
            w.println("#");
            w.println("# One chart per line, pipe-delimited, ten fields:");
            w.println("#");
            w.println("#   name | date | local time | utc offset | lat | lon | rodden | school | source | picks");
            w.println("#");
            w.println("#   date         YYYY-MM-DD");
            w.println("#   local time   HH:MM as recorded, 24 hour");
            w.println("#   utc offset   hours, e.g. -8 for PST, 5.5 for IST. Historical, not current.");
            w.println("#   lat / lon    signed decimal. NORTH positive, EAST positive.");
            w.println("#   rodden       must be AA. Anything else is skipped.");
            w.println("#   school       whose reading the picks come from: tyl, brennan, arroyo, ...");
            w.println("#                Keep one school per corpus file - they disagree about");
            w.println("#                what is prominent, and mixing them makes the target incoherent.");
            w.println("#   source       page or URL, so a pick can be checked later");
            w.println("#   picks        comma separated body names the astrologer foregrounds.");
            w.println("#                Use exactly: Sun Moon Mercury Venus Mars Jupiter Saturn");
            w.println("#                Uranus Neptune Pluto Chiron 'North Node'");
            w.println("#");
            w.println("# BEFORE YOU START, fix the coding protocol and write it here:");
            w.println("#   What counts as the astrologer 'foregrounding' a placement? First");
            w.println("#   mention? Named as the chart signature? Most words spent on it?");
            w.println("#   Decide once, write it down, and code every chart the same way.");
            w.println("#   Code BLIND - do not look at engine output while deciding picks, or");
            w.println("#   you will drift toward agreement without noticing.");
            w.println("#");
            w.println("# Protocol used: ................................................");
            w.println("#");
            w.println("# Format example with placeholder data - delete this line:");
            w.println("# Example Person | 1970-01-01 | 14:25 | -8 | 34.05 | -118.24 | AA | tyl | Tyl p.000 | Saturn,Venus,Moon");
            w.println();
        }
    }

    private static Set<String> topK(List<BodyScore.Vector> ranked, int k) {
        Set<String> s = new LinkedHashSet<>();
        for (BodyScore.Vector v : ranked) {
            if (s.size() >= k) {
                break;
            }
            // Ineligible bodies are skipped rather than breaking the loop: they are not
            // "the end of the ranking", they are simply not on the ballot.
            if (!eligible(v.body)) {
                continue;
            }
            if (v.prominence <= 0.0) {
                break;
            }
            s.add(v.body);
        }
        return s;
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "\u2026";
    }

    private FittingHarness() { }
}
