package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * K12, stage 2: aspects between two progressed bodies ({@link Progressions#mutual}).
 *
 * <p>The scan is measured against the ephemeris, not against itself: at every contact's instant
 * the two progressed longitudes are fetched again and their separation compared with the
 * aspect's exact angle. A root finder that drifted, or a cache keyed wrongly, or an aspect
 * matched to the wrong target angle would all show up there and nowhere else.
 *
 * <p>The lunation guard gets the strong form. It is not enough to observe that no Sun-Moon pair
 * came back - that would also be true if the pair simply never perfected. So the same window is
 * put through {@link Progressions#changes}, which must report the progressed New Moon as a phase
 * change: the event exists, it is reported once, and the second report is the one that was
 * deliberately left out.
 */
public final class MutualProgressionCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, 41.8781, -87.6298, 'P', false, 0.0);

        double from = new SweDate(2026, 9, 20, 12.0).getJulDay();
        List<Progressions.Mutual> year = Progressions.mutual(sw, jd, from, from + 365.25);
        List<Progressions.Mutual> twenty = Progressions.mutual(sw, jd, from, from + 365.25 * 20);
        ok("a year holds a handful of mutual contacts (" + year.size() + ")",
            year.size() >= 1 && year.size() <= 12);
        ok("twenty years hold proportionally more (" + twenty.size() + ")",
            twenty.size() > year.size() * 5);

        // ---- exactness, recomputed from the ephemeris
        double worst = 0.0;
        for (Progressions.Mutual m : twenty) {
            double p = Progressions.progressedJd(jd, m.jd);
            double sep = Math.abs(Almanac.signedDelta(
                Almanac.bodyLongitude(sw, p, m.a), Almanac.bodyLongitude(sw, p, m.b)));
            worst = Math.max(worst, Math.abs(sep - m.type.exactAngle));
        }
        ok(String.format("every contact is exact when it says it is (worst %.6f°)", worst),
            worst < 1.0e-3);

        boolean ordered = true;
        boolean named = true;
        boolean oneWay = true;
        boolean distinct = true;
        for (int i = 0; i < twenty.size(); i++) {
            Progressions.Mutual m = twenty.get(i);
            if (i > 0 && m.jd < twenty.get(i - 1).jd) {
                ordered = false;
            }
            int ia = indexOf(m.a);
            int ib = indexOf(m.b);
            if (ia < 0 || ib < 0) {
                named = false;
            } else if (ia >= ib) {
                oneWay = false;
            }
            if (m.a.equals(m.b)) {
                distinct = false;
            }
        }
        ok("the contacts come back in date order", ordered);
        ok("both ends are progressed bodies", named);
        ok("a pair is reported one way round, not twice", oneWay);
        ok("no body is aspected to itself", distinct);

        // ---- the lunation guard, in its strong form
        double wide = 365.25 * 40.0;
        int lights = 0;
        for (Progressions.Mutual m : Progressions.mutual(sw, jd, from, from + wide)) {
            if (pair(m, "Sun", "Moon")) {
                lights++;
            }
        }
        int newMoons = 0;
        for (Progressions.Change c : Progressions.changes(sw, jd, from, from + wide)) {
            if ("phase".equals(c.kind) && "new".equals(c.entered)) {
                newMoons++;
            }
        }
        ok("forty years hold progressed New Moons (" + newMoons + ")", newMoons >= 1);
        ok("and not one of them is reported again as a Sun-Moon conjunction", lights == 0);
        boolean otherPairs = false;
        for (Progressions.Mutual m : twenty) {
            if (pair(m, "Sun", "Venus") || pair(m, "Sun", "Mars") || pair(m, "Sun", "Mercury")) {
                otherPairs = true;
            }
        }
        ok("the Sun is still scanned against everything else", otherPairs);

        int withMoon = 0;
        for (Progressions.Mutual m : twenty) {
            if ("Moon".equals(m.a) || "Moon".equals(m.b)) {
                withMoon++;
            }
        }
        ok("the Moon, being the fast one, is in most of them (" + withMoon + " of "
            + twenty.size() + ")", withMoon * 2 > twenty.size());

        // ---- the window
        ok("a reversed window yields nothing",
            Progressions.mutual(sw, jd, from + 100, from).isEmpty());
        ok("an empty window yields nothing", Progressions.mutual(sw, jd, from, from).isEmpty());
        boolean nested = true;
        for (Progressions.Mutual m : year) {
            boolean found = false;
            for (Progressions.Mutual w : twenty) {
                if (Math.abs(w.jd - m.jd) < 1.0e-6 && w.a.equals(m.a) && w.b.equals(m.b)) {
                    found = true;
                }
            }
            if (!found) {
                nested = false;
            }
        }
        ok("a wider window holds everything the narrower one found", nested);
        boolean inside = true;
        for (Progressions.Mutual m : twenty) {
            if (m.jd < from || m.jd > from + 365.25 * 20) {
                inside = false;
            }
        }
        ok("and nothing outside the window it was asked about", inside);

        // ---- into the convergence
        Progressions.Mutual one = year.get(0);
        List<Convergence.Target> with = Convergence.collect(null, List.of(), List.of(), List.of(),
            List.of(), List.of(), null, List.of(one));
        Convergence.Target ta = find(with, one.a);
        Convergence.Target tb = find(with, one.b);
        ok("a mutual aspect is a claim on " + one.a + ", one of its ends", ta != null);
        ok("and on " + one.b + ", the other", tb != null);
        ok("it is the progression family", ta != null
            && ta.witnesses.get(0).family == Convergence.Family.PROGRESSION);
        ok("and the line names both ends", ta != null
            && ta.witnesses.get(0).detail.contains(one.a)
            && ta.witnesses.get(0).detail.contains(one.b));
        ok("nothing else is claimed", with.size() == 2);
        ok("without the mutuals neither end is claimed",
            Convergence.collect(null, List.of(), List.of(), List.of(), List.of(), List.of(), null)
                .isEmpty());

        java.util.Set<String> woken = ThemeConvergence.activePoints(f, null, with);
        ok(one.a + " is woken by it", woken.contains(one.a));
        ok(one.b + " is woken by it", woken.contains(one.b));

        // Two mutuals touching the same body are still one technique agreeing with itself.
        List<Convergence.Target> twice = Convergence.collect(null, List.of(), List.of(), List.of(),
            List.of(), List.of(), null, List.of(one, year.size() > 1 ? year.get(1) : one));
        Convergence.Target again = find(twice, one.a);
        ok("two mutuals on one point are still one family", again != null
            && again.families.size() == 1
            && again.families.contains(Convergence.Family.PROGRESSION));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
        }
        System.exit(1);
    }

    private static boolean pair(Progressions.Mutual m, String x, String y) {
        return (m.a.equals(x) && m.b.equals(y)) || (m.a.equals(y) && m.b.equals(x));
    }

    private static int indexOf(String body) {
        for (int i = 0; i < Progressions.progressedBodies.length; i++) {
            if (Progressions.progressedBodies[i].equals(body)) {
                return i;
            }
        }
        return -1;
    }

    private static Convergence.Target find(List<Convergence.Target> ts, String natal) {
        for (Convergence.Target t : ts) {
            if (t.natal.equals(natal)) {
                return t;
            }
        }
        return null;
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
