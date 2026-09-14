package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TransitSearch: the passages it reports are real, whole, and all of them.
 *
 * <p>The search works from a coarse sample of each body and brackets everything from it, which
 * is what makes it fast and is also exactly how a search quietly misses things. So the parts
 * below check it against slower routes that cannot miss: the existing exact-date solver, and a
 * scan fine enough that no passage fits between two of its samples.
 */
public final class TransitSearchCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        // David's chart: 10 Aug 1982, 15:01 EDT, Philadelphia.
        double birth = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);
        ChartFrame natal = ChartFrame.compute(sw, birth, 39.9526, -75.1652, 'P', false, 0.0);
        double from = SweDate.getJulDay(2026, 1, 1, 0.0);
        double to = SweDate.getJulDay(2030, 1, 1, 0.0);

        part("A: a retrograde season is one passage", () -> season(sw, natal, from, to));
        part("B: the exact dates are the solver's exact dates", () -> agreement(sw, natal, from, to));
        part("C: nothing a fine scan finds is missing", () -> completeness(sw, natal, from, to));
        part("D: exact is exact, and the orb edges are at the orb", () -> edges(sw, natal, from, to));
        part("E: a year apart is not one season", () -> notOverMerged(sw, natal, from, to));
        part("F: a chart with no birth time offers no angles", () -> noTime(sw, birth, from, to));
        part("G: only what overlaps the window, in order", () -> window(sw, natal, from, to));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    /**
     * Saturn conjunct natal Moon, 2027-28, on David's chart.
     *
     * Measured when this was written: exact 2 Jul 2027, retrograde 17 Sep 2027, and 16 Mar 2028,
     * with Saturn stationing retrograde on 9 Aug 2027 and direct on 24 Dec 2027. At a 1-degree
     * orb Saturn's loop carries it out of orb between each, which is why the first version gave
     * three rows.
     */
    private static void season(SwissEph sw, ChartFrame natal, double from, double to) {
        List<TransitSearch.Passage> ps = TransitSearch.search(sw, natal,
            List.of("Saturn"), List.of("Moon"), List.of(Aspects.Type.CONJUNCTION), 1.0, from, to);
        ok("one passage, got " + ps, ps.size() == 1);
        if (ps.size() != 1) {
            return;
        }
        TransitSearch.Passage p = ps.get(0);
        ok("three exact moments, got " + p.exacts.size(), p.exacts.size() == 3);
        ok("two stations, got " + p.stations.size(), p.stations.size() == 2);
        if (p.exacts.size() == 3) {
            ok("the first is direct", !p.exacts.get(0).retrograde);
            ok("the second retrograde", p.exacts.get(1).retrograde);
            ok("the third direct again", !p.exacts.get(2).retrograde);
            ok("first exact on 2 Jul 2027, got " + date(p.exacts.get(0).jd),
                date(p.exacts.get(0).jd).equals("2027-07-02"));
            ok("last exact on 16 Mar 2028, got " + date(p.exacts.get(2).jd),
                date(p.exacts.get(2).jd).equals("2028-03-16"));
        }
        if (p.stations.size() == 2) {
            ok("retrograde station first", p.stations.get(0).retrograde);
            ok("then direct", !p.stations.get(1).retrograde);
            ok("each station between the exacts around it", p.exacts.size() == 3
                && p.stations.get(0).jd > p.exacts.get(0).jd
                && p.stations.get(0).jd < p.exacts.get(1).jd
                && p.stations.get(1).jd > p.exacts.get(1).jd
                && p.stations.get(1).jd < p.exacts.get(2).jd);
        }
        ok("it enters before the first exact and leaves after the last",
            p.enters < p.firstMoment() && p.leaves > p.exacts.get(p.exacts.size() - 1).jd);
    }

    /**
     * Every exact moment Transits.exactDates finds in the window, the search finds, and the
     * other way round - to within about ten seconds.
     */
    private static void agreement(SwissEph sw, ChartFrame natal, double from, double to) {
        String[] bodies = {"Mercury", "Mars", "Jupiter", "Saturn", "Pluto"};
        // The MC is not among the scratch settings' default bodies, so it is not placed here.
        String[] points = {"Sun", "Moon", "Venus", "Ascendant", "Saturn"};
        for (String body : bodies) {
            for (String point : points) {
                ChartFrame.Body target = natal.body(point);
                if (target == null || !target.ok) {
                    ok(point + " is placed on the fixture chart", false);
                    continue;
                }
                List<TransitSearch.Passage> ps = TransitSearch.search(sw, natal, List.of(body),
                    List.of(point), Arrays.asList(TransitSearch.MAJOR), 1.0, from, to);
                for (Aspects.Type type : TransitSearch.MAJOR) {
                    List<Double> solver = Transits.exactDates(sw, body, target.lon, type, from, to);
                    List<Double> found = new ArrayList<>();
                    for (TransitSearch.Passage p : ps) {
                        if (p.type != type) {
                            continue;
                        }
                        for (TransitSearch.Exact e : p.exacts) {
                            if (e.jd >= from && e.jd <= to) {
                                found.add(e.jd);
                            }
                        }
                    }
                    Collections.sort(found);
                    String what = body + " " + type.label + " " + point;
                    ok(what + ": " + solver.size() + " exact by the solver, "
                        + found.size() + " by the search", solver.size() == found.size());
                    for (int i = 0; i < Math.min(solver.size(), found.size()); i++) {
                        double gap = Math.abs(solver.get(i) - found.get(i)) * 86400.0;
                        ok(what + " exact " + date(solver.get(i)) + " agrees, off by "
                            + Math.round(gap) + " s", gap < 15.0);
                    }
                }
            }
        }
    }

    /**
     * A scan at a tenth of a degree of motion per sample finds every stretch in orb, and each
     * one lies inside a passage the search returned.
     */
    private static void completeness(SwissEph sw, ChartFrame natal, double from, double to) {
        String[][] cases = {{"Mercury", "Sun"}, {"Venus", "Moon"}, {"Mars", "Ascendant"},
            {"Sun", "Saturn"}};
        double orb = 1.0;
        for (String[] c : cases) {
            String body = c[0];
            ChartFrame.Body target = natal.body(c[1]);
            List<TransitSearch.Passage> ps = TransitSearch.search(sw, natal, List.of(body),
                List.of(c[1]), Arrays.asList(TransitSearch.MAJOR), orb, from, to);
            double step = 0.1 / TransitSearch.maxSpeed(body);
            for (Aspects.Type type : TransitSearch.MAJOR) {
                int stretches = 0;
                int covered = 0;
                boolean wasIn = false;
                double inAt = 0.0;
                for (double t = from; t <= to; t += step) {
                    double off = TransitSearch.offBy(Almanac.bodyLongitude(sw, t, body),
                        target.lon, type);
                    boolean in = off < orb;
                    if (in && !wasIn) {
                        inAt = t;
                    }
                    if (!in && wasIn) {
                        stretches++;
                        if (coveredBy(ps, type, inAt, t - step)) {
                            covered++;
                        }
                    }
                    wasIn = in;
                }
                ok(body + " " + type.label + " " + c[1] + ": " + covered + " of " + stretches
                    + " fine-scan stretches inside a passage", covered == stretches);
            }
        }
    }

    private static boolean coveredBy(List<TransitSearch.Passage> ps, Aspects.Type type,
                                     double a, double b) {
        for (TransitSearch.Passage p : ps) {
            if (p.type != type) {
                continue;
            }
            double start = Double.isNaN(p.enters) ? Double.NEGATIVE_INFINITY : p.enters - 0.01;
            double end = Double.isNaN(p.leaves) ? Double.POSITIVE_INFINITY : p.leaves + 0.01;
            if (a >= start && b <= end) {
                return true;
            }
        }
        return false;
    }

    /** At every exact moment the body is on the aspect; at every entry and exit, on the orb. */
    private static void edges(SwissEph sw, ChartFrame natal, double from, double to) {
        List<TransitSearch.Passage> ps = TransitSearch.search(sw, natal,
            Arrays.asList(TransitSearch.TRANSITING), null, Arrays.asList(TransitSearch.MAJOR),
            1.5, from, SweDate.getJulDay(2027, 1, 1, 0.0));
        ok("a year of every body against every point finds something, got " + ps.size(),
            ps.size() > 100);
        double worstExact = 0.0;
        double worstEdge = 0.0;
        int exacts = 0;
        for (TransitSearch.Passage p : ps) {
            for (TransitSearch.Exact e : p.exacts) {
                exacts++;
                worstExact = Math.max(worstExact, TransitSearch.offBy(
                    Almanac.bodyLongitude(sw, e.jd, p.transiting), p.natalLongitude, p.type));
            }
            for (double edge : new double[]{p.enters, p.leaves}) {
                if (!Double.isNaN(edge)) {
                    worstEdge = Math.max(worstEdge, Math.abs(TransitSearch.offBy(
                        Almanac.bodyLongitude(sw, edge, p.transiting), p.natalLongitude, p.type)
                        - 1.5));
                }
            }
        }
        ok("every exact moment within 0.001 degree of exact over " + exacts + ", worst "
            + worstExact, worstExact < 0.001);
        ok("every orb edge within 0.01 degree of the orb, worst " + worstEdge, worstEdge < 0.01);
        boolean allHaveExact = true;
        for (TransitSearch.Passage p : ps) {
            allHaveExact &= !p.exacts.isEmpty() || Double.isNaN(p.enters) || Double.isNaN(p.leaves)
                || turnsInside(sw, p);
        }
        ok("a whole passage without an exact moment only happens at a station", allHaveExact);
    }

    /** A body can enter orb, station short of exact and leave; that passage has no exact. */
    private static boolean turnsInside(SwissEph sw, TransitSearch.Passage p) {
        return !Almanac.stations(sw, p.enters, p.leaves, p.transiting).isEmpty();
    }

    /**
     * Mercury turns three times a year, so a turn lies between almost any two of its passages.
     * Merging on the turn alone collapsed a four-year search from 672 stretches to 98.
     */
    private static void notOverMerged(SwissEph sw, ChartFrame natal, double from, double to) {
        for (String body : new String[]{"Mercury", "Venus", "Mars"}) {
            List<TransitSearch.Passage> ps = TransitSearch.search(sw, natal, List.of(body),
                List.of("Sun"), List.of(Aspects.Type.CONJUNCTION), 1.0, from, to);
            double longest = 0.0;
            for (TransitSearch.Passage p : ps) {
                if (!Double.isNaN(p.enters) && !Double.isNaN(p.leaves)) {
                    longest = Math.max(longest, p.leaves - p.enters);
                }
            }
            ok(body + " conjunct natal Sun: no passage longer than a retrograde season, longest "
                + Math.round(longest) + " days", longest < 200.0);
            // Mercury and Venus meet the Sun's degree every year; Mars about every two.
            int least = body.equals("Mars") ? 2 : 4;
            ok(body + " conjunct natal Sun happens at least " + least + " times in four years, got "
                + ps.size(), ps.size() >= least);
        }
    }

    private static void noTime(SwissEph sw, double birth, double from, double to) {
        ChartFrame unknown = ChartFrame.computeTimeUnknown(sw, birth, 39.9526, -75.1652, 'P',
            false, 0.0);
        List<TransitSearch.Passage> ps = TransitSearch.search(sw, unknown,
            List.of("Mars"), null, Arrays.asList(TransitSearch.MAJOR), 1.0, from, to);
        ok("still finds passages to the planets, got " + ps.size(), !ps.isEmpty());
        boolean angles = false;
        for (TransitSearch.Passage p : ps) {
            angles |= p.natal.equals("Ascendant") || p.natal.equals("MC")
                || p.natal.equals("Descendant") || p.natal.equals("IC");
        }
        ok("but none to an angle nobody knows", !angles);
    }

    private static void window(SwissEph sw, ChartFrame natal, double from, double to) {
        double a = SweDate.getJulDay(2028, 1, 1, 0.0);
        double b = SweDate.getJulDay(2028, 7, 1, 0.0);
        List<TransitSearch.Passage> ps = TransitSearch.search(sw, natal,
            List.of("Mars", "Jupiter"), null, Arrays.asList(TransitSearch.MAJOR), 1.0, a, b);
        boolean overlaps = true;
        boolean ordered = true;
        double last = Double.NEGATIVE_INFINITY;
        for (TransitSearch.Passage p : ps) {
            double start = Double.isNaN(p.enters) ? Double.NEGATIVE_INFINITY : p.enters;
            double end = Double.isNaN(p.leaves) ? Double.POSITIVE_INFINITY : p.leaves;
            overlaps &= end >= a && start <= b;
            ordered &= p.firstMoment() >= last;
            last = p.firstMoment();
        }
        ok("half a year finds passages, got " + ps.size(), !ps.isEmpty());
        ok("every passage overlaps the half year", overlaps);
        ok("in order of first moment", ordered);
        boolean threw = false;
        try {
            TransitSearch.search(sw, natal, List.of("Mars"), null, List.of(Aspects.Type.SQUARE),
                0.0, a, b);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        ok("an orb of zero is refused rather than returning nothing", threw);

        // What a reader is shown: a passage that went exact only before the window is gone, one
        // that goes exact inside it stays, and one that never goes exact stays if it overlaps.
        List<TransitSearch.Passage> shown = TransitSearch.perfectingIn(ps, a, b);
        boolean everyShownPerfectsInside = true;
        int dropped = 0;
        for (TransitSearch.Passage p : ps) {
            boolean inside = p.exacts.isEmpty();
            for (TransitSearch.Exact e : p.exacts) {
                inside |= e.jd >= a && e.jd < b;
            }
            if (!inside) {
                dropped++;
            }
        }
        for (TransitSearch.Passage p : shown) {
            boolean inside = p.exacts.isEmpty();
            for (TransitSearch.Exact e : p.exacts) {
                inside |= e.jd >= a && e.jd < b;
            }
            everyShownPerfectsInside &= inside;
        }
        ok("every passage shown perfects inside the half year, or never perfects",
            everyShownPerfectsInside);
        ok("and the ones that perfect only outside it are left out: " + dropped + " dropped of "
            + ps.size(), shown.size() == ps.size() - dropped && dropped > 0);
    }

    private static String date(double jd) {
        SweDate d = new SweDate(jd);
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonth(), d.getDay());
    }

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
