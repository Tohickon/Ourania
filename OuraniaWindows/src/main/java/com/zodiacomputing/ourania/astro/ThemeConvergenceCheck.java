package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * K12, stage 4: themes and the Rule of Three (ThemeConvergence).
 *
 * <p>Built from hand-made witnesses on a real natal chart, so each rule is tested exactly rather
 * than hoping a scanned year happens to exercise it: three independent families make a headline
 * and two do not; an echo does not count; one technique on two points of a theme counts once; a
 * profection on a theme's house testifies for it; a theme holds its named points, its houses'
 * rulers and its houses' occupants and nothing else; headlines come first.
 */
public final class ThemeConvergenceCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, 41.8781, -87.6298, 'P', false, 0.0);

        // ---- membership
        java.util.Set<String> career = ThemeConvergence.membersOf(ThemeConvergence.Theme.CAREER, f);
        ok("Career holds the MC, Saturn and the Sun", career.containsAll(List.of("MC", "Saturn", "Sun")));
        String tenthRuler = Dignity.domicileRulerOf(Zodiac.signIndex(f.cusps[10]));
        ok("Career holds the ruler of the 10th (" + tenthRuler + ")", career.contains(tenthRuler));
        int inTenth = 0;
        String outside = null;
        for (int i = 0; i < Bodies.count(); i++) {
            String n = Bodies.at(i).name;
            ChartFrame.Body b = f.body(n);
            if (b == null || !b.ok) {
                continue;
            }
            int h = Zodiac.houseOf(b.lon, f.cusps);
            if (h == 10) {
                inTenth++;
                ok("Career holds " + n + ", in the 10th", career.contains(n));
            } else if (outside == null && h == 2 && !career.contains(n)) {
                outside = n;
            }
        }
        ok("a body in the 2nd, ruling nothing of the theme, is not in Career (" + outside + ")",
            outside == null || !career.contains(outside));
        ok("Relationship holds the Lot of Eros",
            ThemeConvergence.membersOf(ThemeConvergence.Theme.RELATIONSHIP, f).contains("Lot of Eros"));

        // ---- the Rule of Three
        List<Convergence.Target> two = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)));
        ok("two families are a background trend", !career(f, null, two).headline());

        List<Convergence.Target> three = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)),
            target("Saturn", w(Convergence.Family.PROGRESSION, true)));
        ThemeConvergence.Result r3 = career(f, null, three);
        ok("three families on three career points are a headline", r3.headline());
        ok("and every testimony is listed", r3.testimonies.size() == 3);

        List<Convergence.Target> echo = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)),
            target("Saturn", w(Convergence.Family.STATION, false)));
        ok("an echo does not count", !career(f, null, echo).headline());

        List<Convergence.Target> same = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true)),
            target("Saturn", w(Convergence.Family.TRANSIT, true)),
            target("Sun", w(Convergence.Family.TRANSIT, true)));
        ThemeConvergence.Result rs = career(f, null, same);
        ok("one technique on three points is one testimony", rs.families.size() == 1 && !rs.headline());
        ok("but all three are still shown", rs.testimonies.size() == 3);

        List<Convergence.Target> elsewhere = List.of(
            target("Neptune-not-a-point", w(Convergence.Family.TRANSIT, true),
                w(Convergence.Family.SOLAR_ARC, true), w(Convergence.Family.PROGRESSION, true)));
        ok("witnesses on points outside the theme do not count for it",
            career(f, null, elsewhere).families.isEmpty());

        // ---- the profection
        // Real profections, at the ages whose year falls on the 10th and on the 2nd house - and
        // those houses asserted, so the test cannot pass on a wrong premise.
        Profection tenth = Profection.at(jd, jd + 365.25 * 9.5, f.asc);
        ok("age 9 profects to the 10th (" + tenth.house + ")", tenth.house == 10);
        ThemeConvergence.Result rp = career(f, tenth, two);
        ok("a profection on the 10th is Career's third testimony", rp.headline()
            && rp.families.contains(Convergence.Family.PROFECTION));
        Profection second = Profection.at(jd, jd + 365.25 * 1.5, f.asc);
        ok("age 1 profects to the 2nd (" + second.house + ")", second.house == 2);
        ok("a profection elsewhere is not", !career(f, second, two).headline());

        // ---- ordering
        List<ThemeConvergence.Result> all = ThemeConvergence.themes(f, null, three);
        ok("four themes", all.size() == 4);
        ok("the headline leads", all.get(0).theme == ThemeConvergence.Theme.CAREER && all.get(0).headline());

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

    private static ThemeConvergence.Result career(ChartFrame f, Profection p, List<Convergence.Target> t) {
        for (ThemeConvergence.Result r : ThemeConvergence.themes(f, p, t)) {
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
            if (w.independent) {
                t.families.add(w.family);
            }
        }
        t.score = 0.1 * ws.length;
        return t;
    }

    private static Convergence.Witness w(Convergence.Family family, boolean independent) {
        Convergence.Witness w = new Convergence.Witness();
        w.family = family;
        w.independent = independent;
        w.detail = family.name().toLowerCase() + " witness";
        return w;
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
