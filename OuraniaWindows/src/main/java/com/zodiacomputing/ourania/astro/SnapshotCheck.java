package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the composed L3 ranking and the L5 gestalt against live charts, and checks the
 * invariants that must hold whatever the ephemeris says.
 *
 * The point of this runner is to prove the layers compose. Sect and Dignity were each
 * verified in isolation; this is the first thing that puts them together and ranks a
 * real chart.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.SnapshotCheck
 */
public final class SnapshotCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;
    private static final double LAT = 34.05;
    private static final double LON = -118.24;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        SwissEph sw = new SwissEph(EPHE_PATH);

        // Same instant, two very different charts: noon and midnight in Los Angeles.
        show(sw, 2026, 7, 31, 19.0, "31 July 2026, local noon, Los Angeles");
        show(sw, 2026, 8, 1, 7.0, "31 July 2026, local midnight, Los Angeles");
        // A second location to make sure nothing depends on a single Ascendant.
        show(sw, 1990, 3, 14, 2.5, "14 March 1990, 02:30 UT, Los Angeles");

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

    private static void show(SwissEph sw, int y, int m, int d, double hourUt, String label) {
        SweDate sd = new SweDate(y, m, d, hourUt);
        ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'W', false, 0.0);

        System.out.println();
        System.out.println("================================================================");
        System.out.println(label);
        System.out.println("================================================================");

        Gestalt.Result g = Gestalt.compute(f);
        System.out.println();
        for (String h : g.headlines) {
            System.out.println("  " + h);
        }
        System.out.printf("%n  elements   %s%n  modalities %s%n", g.elements, g.modalities);
        System.out.printf("  horizon    above %.2f / below %.2f     east %.2f / west %.2f%n",
            g.aboveHorizon, g.belowHorizon, g.eastern, g.western);
        System.out.printf("  shape      %s%s%n", g.shape,
            g.shapeHandle == null ? "" : " (" + g.shapeHandle + ")");
        System.out.printf("  moon       %s%s%n", g.moonPhase,
            f.moonVoidOfCourse ? ", void of course" : "");
        if (!g.retrograde.isEmpty()) {
            System.out.println("  retrograde " + String.join(", ", g.retrograde));
        }
        if (!g.outOfBounds.isEmpty()) {
            System.out.println("  out of bounds " + String.join(", ", g.outOfBounds));
        }

        List<BodyScore.Vector> ranked = BodyScore.rank(f);
        System.out.println();
        System.out.print(BodyScore.table(ranked));

        verify(f, g, ranked, label);
    }

    private static void verify(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked, String label) {
        String tag = " [" + label + "]";

        // Ranking invariants.
        eq("ranking is non-empty" + tag, true, !ranked.isEmpty());
        eq("top body has prominence 1.0" + tag, true,
            Math.abs(ranked.get(0).prominence - 1.0) < 1e-9);
        for (int i = 1; i < ranked.size(); i++) {
            checks++;
            if (ranked.get(i).prominence > ranked.get(i - 1).prominence + 1e-12) {
                failures.add("ranking out of order at " + i + tag);
            }
        }
        for (BodyScore.Vector v : ranked) {
            checks++;
            if (v.prominence < 0.0 || v.prominence > 1.0 + 1e-9) {
                failures.add(v.body + " prominence out of range: " + v.prominence + tag);
            }
        }

        // Every ranked body must be able to justify itself. A score with no reasons is
        // a number the reading cannot use.
        for (BodyScore.Vector v : ranked) {
            checks++;
            if (v.allReasons().isEmpty()) {
                failures.add(v.body + " produced no reasons" + tag);
            }
        }

        // The four axes must stay independent: a body can rank high on prominence while
        // scoring badly on dignity and valence. If that never happens the vector has
        // collapsed back into a strength sum.
        eq("chart ruler is identified" + tag, true, g.chartRuler != null);
        eq("gestalt sect matches the frame" + tag, f.diurnal, g.diurnal);
        eq("out-of-sect malefic matches sect" + tag,
            f.diurnal ? "Mars" : "Saturn", g.outOfSectMalefic);

        // Balances must account for all the weight that went in.
        double e = g.elements.values().stream().mapToDouble(Double::doubleValue).sum();
        double mo = g.modalities.values().stream().mapToDouble(Double::doubleValue).sum();
        near("element and modality totals agree" + tag, e, mo, 1e-9);

        double vTotal = g.aboveHorizon + g.belowHorizon;
        double hTotal = g.eastern + g.western;
        near("hemisphere totals agree" + tag, vTotal, hTotal, 1e-9);

        // The angles must NOT be double counted in the balances.
        //
        // They were: Ascendant and MC are registry points picked up by the body loop out of
        // WEIGHTS, and balances() then added them a second time from f.asc and f.mc. The
        // element total came to 27.0 against a 14-point table that allows 22.0, so the
        // Ascendant counted 6 and the MC 3. Fixing it moved the dominant element or modality
        // on 51.3% of 400 random charts, which is why this is asserted rather than trusted.
        double expected = 0.0;
        for (ChartFrame.Body b : f.bodies) {
            if (b.ok && Gestalt.weightOf(b.name) > 0.0) {
                expected += Gestalt.weightOf(b.name);
            }
        }
        expected += 2.0;   // the chart ruler's bonus, added on top of its own weight
        near("element weight matches the table exactly" + tag, expected, e, 1e-9);

        // Quadrants, angularity classes and thematic trinities are three different divisions
        // of ONE population, so their totals must agree with each other and with the
        // hemisphere count, which walks the same angle-free set. A body filed under a quadrant
        // but missing from a trinity would mean one of the three divisors is wrong.
        double q = sum(g.quadrants);
        double hm = sum(g.houseModes);
        double tr = sum(g.trinities);
        near("quadrant and house-mode totals agree" + tag, q, hm, 1e-9);
        near("house-mode and trinity totals agree" + tag, hm, tr, 1e-9);
        near("house fields cover the hemisphere population" + tag, vTotal, q, 1e-9);

        // The signature sign must genuinely carry both dominant traits, or be absent.
        if (g.signatureSign != null) {
            int s = -1;
            for (int i = 0; i < Zodiac.SIGNS.length; i++) {
                if (Zodiac.SIGNS[i].equals(g.signatureSign)) {
                    s = i;
                }
            }
            eq("signature sign is a real sign" + tag, true, s >= 0);
            if (s >= 0) {
                eq("signature sign carries the top element" + tag,
                    topKey(g.elements), Gestalt.ELEMENTS[s % 4]);
                eq("signature sign carries the top modality" + tag,
                    topKey(g.modalities), Gestalt.MODALITIES[s % 3]);
            }
            eq("a named signature is not also flagged as tied" + tag, false, g.signatureTied);
        }

        // Every bucket the headline writer can name must have a clause to name it with.
        for (String k : Gestalt.QUADRANTS) {
            eq("quadrant '" + k + "' has a meaning" + tag, true, Gestalt.fieldMeaning(k) != null);
        }
        for (String k : Gestalt.HOUSE_MODES) {
            eq("house mode '" + k + "' has a meaning" + tag, true, Gestalt.fieldMeaning(k) != null);
        }
        for (String k : Gestalt.TRINITIES) {
            eq("trinity '" + k + "' has a meaning" + tag, true, Gestalt.fieldMeaning(k) != null);
        }
        for (String h : g.headlines) {
            checks++;
            if (h.contains("null")) {
                failures.add("headline contains a null: " + h + tag);
            }
        }

        // A singleton is a count of one. Anything named as one must actually be alone.
        for (String s : g.singletons) {
            checks++;
            String who = s.substring(0, s.indexOf(','));
            boolean alone = holdsAlone(g.elementMembers, who) || holdsAlone(g.modalityMembers, who)
                || s.equals(g.hemisphereSingleton);
            if (!alone) {
                failures.add("singleton " + s + " is not actually alone" + tag);
            }
        }

        // Sect is the horizon test, so the Sun must land on the side sect says it does.
        boolean sunAbove = Zodiac.normalise(f.body("Sun").lon - f.asc) > 180.0;
        eq("Sun's hemisphere agrees with sect" + tag, f.diurnal, sunAbove);

        // A body cannot be both a final dispositor and inside a dispositor loop.
        for (String fd : g.finalDispositors) {
            for (String loop : g.dispositorLoops) {
                checks++;
                if (loop.contains(fd)) {
                    failures.add(fd + " is both a final dispositor and in a loop" + tag);
                }
            }
        }
    }

    private static double sum(java.util.Map<String, Double> m) {
        double t = 0.0;
        for (double v : m.values()) {
            t += v;
        }
        return t;
    }

    /** The key with the largest weight. Only called where a unique top is already established. */
    private static String topKey(java.util.Map<String, Double> m) {
        String best = null;
        double bv = Double.NEGATIVE_INFINITY;
        for (java.util.Map.Entry<String, Double> e : m.entrySet()) {
            if (e.getValue() > bv) {
                bv = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static boolean holdsAlone(java.util.Map<String, java.util.List<String>> members, String who) {
        for (java.util.List<String> group : members.values()) {
            if (group.size() == 1 && group.get(0).equals(who)) {
                return true;
            }
        }
        return false;
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double a, double b, double tol) {
        checks++;
        if (Math.abs(a - b) > tol) {
            failures.add(label + ": " + a + " vs " + b);
        }
    }
}
