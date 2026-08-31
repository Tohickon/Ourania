package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Weight diagnostics that need no external data.
 *
 * Astrology has no outcome variable, so the weights in BodyScore, Gestalt and Themes
 * cannot be fitted in the statistical sense. But two useful questions are answerable
 * from the engine alone:
 *
 *   DISCRIMINATION - does the ranker actually separate? If one body wins most charts,
 *       or every body scores near the top, the ranking carries little information
 *       regardless of whether the weights are "right".
 *
 *   SENSITIVITY - which weights change the output at all? Perturb each by +/-50% and
 *       measure how often the top-3 set changes. A weight that changes nothing is
 *       over-specified and should be deleted rather than calibrated. A weight that
 *       reorders half the charts is where the effort belongs.
 *
 * Run this BEFORE trying to calibrate anything against expert practice: it tells you
 * which of the numbers are worth the trouble.
 */
public final class Calibration {

    private static final String EPHE_PATH = Ephemeris.PATH;
    private static final int SAMPLE = 400;
    private static final long SEED = 20260731L;

    public static void main(String[] args) {
        SwissEph sw = new SwissEph(EPHE_PATH);
        List<ChartFrame> charts = sample(sw);
        System.out.println("Sampled " + charts.size() + " charts, 1950-2030, latitudes -55 to 60.");

        discrimination(charts);
        headroom(charts);
        sensitivity(charts);
    }

    /**
     * How much work is the sophisticated part of the engine actually doing?
     *
     * If a naive ranker - "the three bodies nearest an angle" - agrees with the full
     * engine most of the time, then all the dignity, sect and rulership machinery is
     * only moving a small fraction of the decision. That fraction is the effect size any
     * calibration corpus has to be large enough to detect. It is the difference between
     * "40 charts will do" and "you need hundreds and will not get them".
     */
    private static void headroom(List<ChartFrame> charts) {
        System.out.println();
        System.out.println("=== HEADROOM: full engine vs naive baselines ===");

        int agreeAngular = 0;
        int agreeDignity = 0;
        int agreeFixed = 0;
        int agreeAngularDignity = 0;

        for (ChartFrame f : charts) {
            List<BodyScore.Vector> r = BodyScore.rank(f);
            Set<String> full = topN(r, 3);

            if (full.equals(byAngularity(r))) {
                agreeAngular++;
            }
            if (full.equals(byDignity(r))) {
                agreeDignity++;
            }
            if (full.equals(fixedPicks(f))) {
                agreeFixed++;
            }
            if (byAngularity(r).equals(byDignity(r))) {
                agreeAngularDignity++;
            }
        }
        int n = charts.size();
        System.out.printf("  full engine == three most angular      %5.1f%%%n",
            100.0 * agreeAngular / n);
        System.out.printf("  full engine == three most dignified    %5.1f%%%n",
            100.0 * agreeDignity / n);
        System.out.printf("  full engine == Sun/Moon/chart ruler    %5.1f%%%n",
            100.0 * agreeFixed / n);
        System.out.printf("  (angular baseline == dignity baseline  %5.1f%%)%n",
            100.0 * agreeAngularDignity / n);
        System.out.println("  The gap between the engine and the best baseline is the");
        System.out.println("  effect a calibration corpus must be powered to detect.");
    }

    private static Set<String> topN(List<BodyScore.Vector> r, int n) {
        Set<String> s = new LinkedHashSet<>();
        for (BodyScore.Vector v : r) {
            if (s.size() >= n || v.prominence <= 0.0) {
                break;
            }
            s.add(v.body);
        }
        return s;
    }

    private static Set<String> byAngularity(List<BodyScore.Vector> r) {
        List<BodyScore.Vector> c = new ArrayList<>(r);
        c.sort((a, b) -> Double.compare(b.angularity, a.angularity));
        Set<String> s = new LinkedHashSet<>();
        for (BodyScore.Vector v : c) {
            if (s.size() >= 3 || v.angularity <= 0.0) {
                break;
            }
            s.add(v.body);
        }
        return s;
    }

    private static Set<String> byDignity(List<BodyScore.Vector> r) {
        List<BodyScore.Vector> c = new ArrayList<>(r);
        c.sort((a, b) -> Integer.compare(b.dignity.score, a.dignity.score));
        Set<String> s = new LinkedHashSet<>();
        for (BodyScore.Vector v : c) {
            if (s.size() >= 3) {
                break;
            }
            s.add(v.body);
        }
        return s;
    }

    private static Set<String> fixedPicks(ChartFrame f) {
        Set<String> s = new LinkedHashSet<>();
        s.add("Sun");
        s.add("Moon");
        s.add(Dignity.domicileRulerOf(Zodiac.signIndex(f.asc)));
        return s;
    }

    /** Random moments and places, so results are not an artefact of one location. */
    private static List<ChartFrame> sample(SwissEph sw) {
        Random rnd = new Random(SEED);
        List<ChartFrame> out = new ArrayList<>();
        for (int i = 0; i < SAMPLE; i++) {
            int year = 1950 + rnd.nextInt(80);
            int month = 1 + rnd.nextInt(12);
            int day = 1 + rnd.nextInt(28);
            double hour = rnd.nextDouble() * 24.0;
            double lat = -55.0 + rnd.nextDouble() * 115.0;
            double lon = -180.0 + rnd.nextDouble() * 360.0;
            SweDate sd = new SweDate(year, month, day, hour);
            out.add(ChartFrame.compute(sw, sd.getJulDay(), lat, lon, 'W', false, 0.0));
        }
        return out;
    }

    // ------------------------------------------------------------------ discrimination

    private static void discrimination(List<ChartFrame> charts) {
        System.out.println();
        System.out.println("=== DISCRIMINATION ===");

        Map<String, Integer> winner = new LinkedHashMap<>();
        int ties = 0;
        int flatCharts = 0;
        double sumGap = 0.0;
        double sumSecond = 0.0;
        int nonEmpty = 0;

        for (ChartFrame f : charts) {
            List<BodyScore.Vector> r = BodyScore.rank(f);
            if (r.isEmpty()) {
                continue;
            }
            nonEmpty++;
            winner.merge(r.get(0).body, 1, Integer::sum);
            if (r.size() > 1) {
                double gap = r.get(0).prominence - r.get(1).prominence;
                sumGap += gap;
                sumSecond += r.get(1).prominence;
                if (gap < 1e-9) {
                    ties++;
                }
            }
            // How many bodies score zero: they are unranked in practice.
            long zero = r.stream().filter(v -> v.prominence <= 0.0).count();
            if (zero >= r.size() - 1) {
                flatCharts++;
            }
        }

        System.out.printf("  top-1 to top-2 gap, mean      %.3f%n", sumGap / nonEmpty);
        System.out.printf("  second place prominence, mean %.3f%n", sumSecond / nonEmpty);
        System.out.printf("  charts with a tie at the top  %d (%.1f%%)%n",
            ties, 100.0 * ties / nonEmpty);
        System.out.printf("  charts where only one body scores above zero  %d (%.1f%%)%n",
            flatCharts, 100.0 * flatCharts / nonEmpty);
        System.out.println("  which body ranks first:");
        final int total = nonEmpty;
        winner.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(e -> System.out.printf("    %-11s %4d  %5.1f%%%n",
                e.getKey(), e.getValue(), 100.0 * e.getValue() / total));
    }

    // ------------------------------------------------------------------ sensitivity

    private static void sensitivity(List<ChartFrame> charts) {
        System.out.println();
        System.out.println("=== SENSITIVITY: how often does +/-50% change the top 3? ===");
        System.out.println("  Two independent outputs, because they do not share weights:");
        System.out.println("  PROMINENCE is what the reading ranks on; CONDITION is a separate axis");
        System.out.println("  that feeds no prominence term. Measured against prominence alone, every");
        System.out.println("  condition weight scores 0% and reads as dead.");

        List<Set<String>> promBase = new ArrayList<>();
        List<Set<String>> condBase = new ArrayList<>();
        BodyScore.resetWeights();
        for (ChartFrame f : charts) {
            List<BodyScore.Vector> r = BodyScore.rank(f);
            promBase.add(topThreeBy(r, true));
            condBase.add(topThreeBy(r, false));
        }

        System.out.printf("  %-28s %-15s %-15s %s%n",
            "weight", "prominence", "condition", "verdict");
        System.out.println("  " + "-".repeat(76));
        for (java.lang.reflect.Field f : tunables()) {
            double[] lo = churn(charts, promBase, condBase, f, 0.5);
            double[] hi = churn(charts, promBase, condBase, f, 1.5);
            double prom = Math.max(lo[0], hi[0]);
            double cond = Math.max(lo[1], hi[1]);
            System.out.printf("  %-28s %5.1f%% / %5.1f%%  %5.1f%% / %5.1f%%  %s%n",
                f.getName(), lo[0] * 100, hi[0] * 100, lo[1] * 100, hi[1] * 100,
                verdict(f.getName(), Math.max(prom, cond)));
        }

        // Control AFTER the sweep, not before. Run first it proves only that the baseline
        // was clean to begin with, which is never the interesting question - the failure
        // this catches is a weight left perturbed by an earlier row, and that can only
        // show up once every row has run.
        double[] control = churn(charts, promBase, condBase, null, 1.0);
        System.out.printf("  %-28s %5.1f%% / %5.1f%%  %5.1f%% / %5.1f%%  %s%n",
            "(control: nothing changed)", control[0] * 100, control[0] * 100,
            control[1] * 100, control[1] * 100,
            control[0] + control[1] == 0.0
                ? "state restored cleanly"
                : "LEAK - a weight above was not restored; readings are contaminated");
        BodyScore.resetWeights();

        System.out.println();
        System.out.println("=== SENSITIVITY: Themes.witnessFloor ===");
        double[] floors = {0.0, 0.10, 0.25, 0.40, 0.60};
        for (double fl : floors) {
            Themes.witnessFloor = fl;
            int sigs = 0;
            int contras = 0;
            for (ChartFrame f : charts) {
                Gestalt.Result g = Gestalt.compute(f);
                List<BodyScore.Vector> r = BodyScore.rank(f);
                Themes.Result t = Themes.extract(f, g, r);
                sigs += Themes.signaturesAtThreshold(t).size();
                contras += t.contradictions.size();
            }
            System.out.printf("  floor %.2f   signatures/chart %.2f   contradictions/chart %.2f%n",
                fl, (double) sigs / charts.size(), (double) contras / charts.size());
        }
        Themes.witnessFloor = 0.25;
    }

    /**
     * Every tunable double on BodyScore, found by reflection rather than listed.
     *
     * This used to be a hand-written switch, and the switch was a trap: a new weight had
     * to be added in three places - the field, resetWeights, and here - and forgetting
     * the third left it silently untested. Eighteen weights were added for aspects,
     * condition and L5 membership and not one of them was being swept. Reflection means
     * a weight is covered the moment it exists.
     */
    private static List<java.lang.reflect.Field> tunables() {
        List<java.lang.reflect.Field> out = new ArrayList<>();
        for (java.lang.reflect.Field f : BodyScore.class.getDeclaredFields()) {
            int m = f.getModifiers();
            if (java.lang.reflect.Modifier.isPublic(m)
                && java.lang.reflect.Modifier.isStatic(m)
                && f.getType() == double.class) {
                out.add(f);
            }
        }
        return out;
    }

    private static void scale(java.lang.reflect.Field f, double factor) {
        try {
            f.setDouble(null, f.getDouble(null) * factor);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(f.getName(), e);
        }
    }

    /**
     * Churn on two independent outputs.
     *
     * The original metric was the top three by PROMINENCE, which cannot see a condition
     * weight at all: conditionPartial feeds no term of computeProminence, so perturbing
     * weightCombust by half moves nothing this metric looks at. Measuring both axes is
     * the difference between "this weight is inert" and "this weight governs an output
     * you were not measuring".
     */
    private static double[] churn(List<ChartFrame> charts,
                                  List<Set<String>> promBase,
                                  List<Set<String>> condBase,
                                  java.lang.reflect.Field f, double factor) {
        // Snapshot and restore EVERY tunable by reflection rather than trusting
        // resetWeights to mention them all. It did not: weightStationary was absent, so
        // once the sweep perturbed it, it stayed perturbed and every row after it carried
        // a constant phantom churn that looked like a real reading. Restoring exactly
        // what was saved cannot go stale when a weight is added.
        List<java.lang.reflect.Field> all = tunables();
        double[] saved = snapshot(all);
        if (f != null) {
            scale(f, factor);
        }
        int prom = 0;
        int cond = 0;
        for (int i = 0; i < charts.size(); i++) {
            List<BodyScore.Vector> ranked = BodyScore.rank(charts.get(i));
            if (!topThreeBy(ranked, true).equals(promBase.get(i))) {
                prom++;
            }
            if (!topThreeBy(ranked, false).equals(condBase.get(i))) {
                cond++;
            }
        }
        restore(all, saved);
        return new double[]{(double) prom / charts.size(), (double) cond / charts.size()};
    }

    private static double[] snapshot(List<java.lang.reflect.Field> fs) {
        double[] out = new double[fs.size()];
        for (int i = 0; i < fs.size(); i++) {
            try {
                out[i] = fs.get(i).getDouble(null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(fs.get(i).getName(), e);
            }
        }
        return out;
    }

    private static void restore(List<java.lang.reflect.Field> fs, double[] vals) {
        for (int i = 0; i < fs.size(); i++) {
            try {
                fs.get(i).setDouble(null, vals[i]);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(fs.get(i).getName(), e);
            }
        }
    }

    private static Set<String> topThree(ChartFrame f) {
        return topThreeBy(BodyScore.rank(f), true);
    }

    /** Top three by prominence, or by condition when byProminence is false. */
    private static Set<String> topThreeBy(List<BodyScore.Vector> ranked, boolean byProminence) {
        List<BodyScore.Vector> copy = new ArrayList<>(ranked);
        if (!byProminence) {
            copy.sort((a, b) -> Double.compare(b.conditionPartial, a.conditionPartial));
        }
        Set<String> s = new LinkedHashSet<>();
        for (BodyScore.Vector v : copy) {
            if (s.size() >= 3) {
                break;
            }
            if (byProminence && v.prominence <= 0.0) {
                break;
            }
            s.add(v.body);
        }
        return s;
    }

    /**
     * Why a weight can measure as zero without being dead.
     *
     * The original verdict said "inert - delete it" below 2% churn, and on the current
     * weight set that advice is wrong for every weight it fires on. Three different
     * reasons produce a zero, and none of them means the number is useless:
     *
     *   - the term is gated off, so the flag it multiplies is never set
     *   - it shifts every body equally, so a RANK-based metric cannot see it by
     *     construction, however much it moves the absolute score
     *   - it governs something genuinely rare, so few charts can possibly change
     *
     * A metric that cannot distinguish these will happily recommend deleting a correct
     * weight. Naming them is cheaper than teaching the metric to tell them apart.
     */
    private static String zeroNote(String weight) {
        switch (weight) {
            case "conditionBaseline":
                return "offset: moves every body equally, invisible to a rank metric";
            case "weightCazimi":
                return "rare: cazimi occurs in well under 1% of bodies";
            default:
                return null;
        }
    }

    private static String verdict(String weight, double worst) {
        if (worst < 0.02) {
            String why = zeroNote(weight);
            return why == null ? "no effect on either ranking - check whether it is reachable"
                               : "no rank effect (" + why + ")";
        }
        if (worst < 0.15) {
            return "minor";
        }
        if (worst < 0.40) {
            return "material";
        }
        return "dominant - calibrate this one";
    }
}
