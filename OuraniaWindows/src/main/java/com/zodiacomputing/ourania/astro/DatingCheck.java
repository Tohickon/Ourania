package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * K12, stage 3: Mars and the Sun date a converged theme, and never vote for one.
 *
 * <p><b>The one thing that must not be true.</b> The Sun conjuncts every natal point once a year
 * and Mars does it in most two-year spans - measured, on this engine: with the fast bodies let
 * into the convergence, 331 of one year's 371 perfections were theirs. That is why
 * {@link Transits#yearMarkers} excludes them, and it is exactly what makes them good clocks. So
 * the assertions here are in two halves: the scan finds real touches at real moments, and
 * <b>nothing it finds can change a single family, score or headline</b>. The second half is the
 * one worth having - a dating stage that quietly votes would re-create the inflation the Rule of
 * Three was built to remove, and it would look like a richer reading while doing it.
 */
public final class DatingCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, 41.8781, -87.6298, 'P', false, 0.0);
        double from = new SweDate(2026, 9, 20, 12.0).getJulDay();
        double to = from + 365.25;

        java.util.Set<String> points = ThemeConvergence.allThemePoints(f);
        List<Transits.Perfection> hits = Transits.datingHits(sw, f, points, from, to);

        // ---- the scan
        ok("a year of catalyst touches is found (" + hits.size() + ")",
            hits.size() > 40 && hits.size() < 400);
        boolean onlyFast = true;
        boolean onlyPtolemaic = true;
        boolean inWindow = true;
        boolean onMembers = true;
        boolean ordered = true;
        for (int i = 0; i < hits.size(); i++) {
            Transits.Perfection p = hits.get(i);
            if (!"Sun".equals(p.transiting) && !"Mars".equals(p.transiting)) {
                onlyFast = false;
            }
            if (p.type.maxOrb != Double.MAX_VALUE) {
                onlyPtolemaic = false;
            }
            if (p.jd < from || p.jd > to) {
                inWindow = false;
            }
            if (!points.contains(p.natal)) {
                onMembers = false;
            }
            if (i > 0 && p.jd < hits.get(i - 1).jd) {
                ordered = false;
            }
        }
        ok("only the Sun and Mars - the year markers stay out", onlyFast);
        ok("only the five Ptolemaic aspects: a septile dates nothing a reader could recognise",
            onlyPtolemaic);
        ok("every touch is inside the window it was asked about", inWindow);
        ok("and on a point some theme actually stands on", onMembers);
        ok("in date order", ordered);
        ok("both bodies are represented", named(hits, "Sun") && named(hits, "Mars"));

        // <b>Exact when it says it is</b>, recomputed from the ephemeris rather than trusted.
        double worst = 0.0;
        for (Transits.Perfection p : hits) {
            double lon = Almanac.bodyLongitude(sw, p.jd, p.transiting);
            double target = targetLon(f, p.natal);
            double sep = Math.abs(Almanac.signedDelta(lon, target));
            worst = Math.max(worst, Math.abs(sep - p.type.exactAngle));
        }
        ok(String.format("every touch is exact at the moment given (worst %.4f°)", worst),
            worst < 0.01);

        // <b>Once each.</b> The angles are in the body list as well as in the angle list.
        java.util.Set<String> keys = new java.util.HashSet<>();
        boolean noDoubles = true;
        for (Transits.Perfection p : hits) {
            if (!keys.add(p.transiting + "|" + p.natal + "|" + p.type + "|"
                    + Math.round(p.jd * 1000.0))) {
                noDoubles = false;
            }
        }
        ok("no touch is reported twice", noDoubles);

        // ---- the guards
        ok("no members, no dating",
            Transits.datingHits(sw, f, java.util.Set.of(), from, to).isEmpty());
        ok("a member nothing in the chart answers to yields nothing",
            Transits.datingHits(sw, f, java.util.Set.of("Nobody"), from, to).isEmpty());
        ok("a reversed window yields nothing",
            Transits.datingHits(sw, f, points, to, from).isEmpty());

        // ---- dating attaches to headlines, and only to headlines
        List<Convergence.Target> two = List.of(
            target("MC", w(Convergence.Family.TRANSIT), w(Convergence.Family.SOLAR_ARC)));
        List<Convergence.Target> three = List.of(
            target("MC", w(Convergence.Family.TRANSIT), w(Convergence.Family.SOLAR_ARC)),
            target("Saturn", w(Convergence.Family.PROGRESSION)));

        ThemeConvergence.Result trend = career(f, two, hits);
        ok("a background trend is given no dates at all", !trend.headline()
            && trend.dates.isEmpty() && trend.peaks.isEmpty());

        ThemeConvergence.Result head = career(f, three, hits);
        ok("a headline is dated (" + head.dates.size() + " touches)",
            head.headline() && !head.dates.isEmpty());
        boolean mine = true;
        for (ThemeConvergence.Dated d : head.dates) {
            if (!head.members.contains(d.natal)) {
                mine = false;
            }
        }
        ok("and only on its own points", mine);
        boolean datesOrdered = true;
        for (int i = 1; i < head.dates.size(); i++) {
            if (head.dates.get(i).jd < head.dates.get(i - 1).jd) {
                datesOrdered = false;
            }
        }
        ok("its dates are in order", datesOrdered);

        // ---- and the thing that must not happen
        ThemeConvergence.Result without = career(f, three, null);
        ok("the dating changes no family: " + head.families + " against " + without.families,
            head.families.equals(without.families));
        ok("and no score", Math.abs(head.score - without.score) < 1e-9);
        ok("and no testimony", head.testimonies.equals(without.testimonies));
        ok("a trend with a year of catalysts on it is still a trend",
            !career(f, two, hits).headline());
        List<ThemeConvergence.Result> withHits = ThemeConvergence.themes(f, null, three, null, hits);
        List<ThemeConvergence.Result> noHits = ThemeConvergence.themes(f, null, three, null, null);
        boolean sameOrder = true;
        for (int i = 0; i < withHits.size(); i++) {
            if (withHits.get(i).theme != noHits.get(i).theme) {
                sameOrder = false;
            }
        }
        ok("and the themes come out in the same order either way", sameOrder);

        // <b>A headline built without a single transit family, so a leaked vote has nowhere to
        // hide.</b> The first cut of this compared the family sets before and after dating,
        // which looks conclusive and is not: the fixture already carried TRANSIT, so a dating
        // stage that quietly added TRANSIT changed nothing a Set could show. The mutation
        // "a catalyst is allowed to vote" walked straight through it. Three families that the
        // Sun and Mars could never supply, and then the absence asserted by name.
        List<Convergence.Target> quiet = List.of(
            target("MC", w(Convergence.Family.SOLAR_ARC), w(Convergence.Family.PROGRESSION)),
            target("Saturn", w(Convergence.Family.RETURN)));
        ThemeConvergence.Result clean = career(f, quiet, hits);
        ok("a headline can be made without any transit family", clean.headline()
            && clean.families.size() == 3);
        ok("and a year of Sun and Mars touches on it adds no transit family: " + clean.families,
            !clean.families.contains(Convergence.Family.TRANSIT));
        ok("nor any other family - exactly three, the three it was given",
            clean.families.size() == 3);
        ok("while it is still dated (" + clean.dates.size() + " touches)",
            !clean.dates.isEmpty());

        // ---- peaks
        ok("the year's touches gather into peaks (" + head.peaks.size() + ")",
            !head.peaks.isEmpty() && head.peaks.size() < head.dates.size());
        boolean twoUp = true;
        boolean tight = true;
        boolean inside = true;
        for (ThemeConvergence.Peak p : head.peaks) {
            if (p.hits.size() < 2) {
                twoUp = false;
            }
            for (int i = 1; i < p.hits.size(); i++) {
                if (p.hits.get(i).jd - p.hits.get(i - 1).jd > ThemeConvergence.PEAK_SPAN_DAYS) {
                    tight = false;
                }
            }
            if (!head.dates.containsAll(p.hits)) {
                inside = false;
            }
        }
        ok("a peak is never one lone touch - that is what a year is made of", twoUp);
        ok("every step inside a peak is within the span", tight);
        ok("and a peak invents no touch of its own", inside);

        List<ThemeConvergence.Peak> top = head.strongest(4);
        ok("the strongest few are offered for reading", top.size() == Math.min(4, head.peaks.size()));
        boolean byDate = true;
        for (int i = 1; i < top.size(); i++) {
            if (top.get(i).from() < top.get(i - 1).from()) {
                byDate = false;
            }
        }
        ok("ranked by weight but handed back in date order", byDate);
        int smallestKept = Integer.MAX_VALUE;
        for (ThemeConvergence.Peak p : top) {
            smallestKept = Math.min(smallestKept, p.hits.size());
        }
        int biggestDropped = 0;
        for (ThemeConvergence.Peak p : head.peaks) {
            if (!top.contains(p)) {
                biggestDropped = Math.max(biggestDropped, p.hits.size());
            }
        }
        ok("nothing fuller was left out: kept down to " + smallestKept
            + ", dropped up to " + biggestDropped, biggestDropped <= smallestKept);

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

    private static double targetLon(ChartFrame f, String name) {
        if ("Ascendant".equals(name)) {
            return f.asc;
        }
        if ("MC".equals(name)) {
            return f.mc;
        }
        if ("Descendant".equals(name)) {
            return f.dsc;
        }
        if ("IC".equals(name)) {
            return f.ic;
        }
        ChartFrame.Body b = f.body(name);
        return b == null ? Double.NaN : b.lon;
    }

    private static boolean named(List<Transits.Perfection> hits, String body) {
        for (Transits.Perfection p : hits) {
            if (body.equals(p.transiting)) {
                return true;
            }
        }
        return false;
    }

    private static ThemeConvergence.Result career(ChartFrame f, List<Convergence.Target> t,
                                                  List<Transits.Perfection> hits) {
        for (ThemeConvergence.Result r : ThemeConvergence.themes(f, null, t, null, hits)) {
            if (r.theme == ThemeConvergence.Theme.CAREER) {
                return r;
            }
        }
        throw new IllegalStateException("no Career theme");
    }

    private static Convergence.Target target(String natal, Convergence.Witness... ws) {
        Convergence.Target t = new Convergence.Target();
        t.natal = natal;
        for (Convergence.Witness w : ws) {
            t.witnesses.add(w);
            t.families.add(w.family);
        }
        t.score = 0.1 * ws.length;
        return t;
    }

    private static Convergence.Witness w(Convergence.Family family) {
        Convergence.Witness w = new Convergence.Witness();
        w.family = family;
        w.independent = true;
        w.detail = family.name().toLowerCase() + " witness";
        return w;
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
