package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * K12, stage 2: the progressed Moon as the mid-term clock.
 *
 * <p>Two things are proved here, and they are proved differently on purpose.
 *
 * <p><b>The clock itself</b> is measured against the ephemeris rather than against its own
 * output: every tenancy's house is recomputed from the progressed Moon's longitude at that
 * instant, every ingress is checked to fall on the cusp it claims, and a thirty-year sweep is
 * checked against the technique's own period - twelve houses to a cycle, a cycle in about
 * twenty-seven years - which no arithmetic inside {@link Progressions#clock} could fake.
 *
 * <p><b>The testimony</b> is built from hand-made tenancies on a real chart, the way
 * {@link ThemeConvergenceCheck} builds witnesses, so each rule is exercised exactly: the clock
 * in a theme's house testifies for it, in another house it does not, two of a theme's houses
 * count once, and the clock activates the bodies of the house it tenants so a transit there
 * stops being background. The three-argument {@code themes} is run beside the four-argument one
 * on the same targets, so what the clock added is visible rather than assumed.
 */
public final class ProgressedMoonCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, 41.8781, -87.6298, 'P', false, 0.0);

        // ---- the clock, measured against the ephemeris
        double from = jd + 365.25 * 30.0;
        double to = from + 365.25;
        List<Progressions.Tenancy> year = Progressions.clock(sw, jd, f.cusps, from, to);
        ok("a year of the clock yields at least one tenancy (" + year.size() + ")", !year.isEmpty());

        boolean housesRight = true;
        boolean signsRight = true;
        boolean phasesRight = true;
        for (Progressions.Tenancy t : year) {
            double lon = progressedMoon(sw, jd, t.jdFrom + 0.5);
            if (Zodiac.houseOf(lon, f.cusps) != t.house) {
                housesRight = false;
            }
            if (!Zodiac.signName(progressedMoon(sw, jd, t.jdFrom)).equals(t.sign)) {
                signsRight = false;
            }
            if (!Progressions.lunationPhase(sw, jd, t.jdFrom).equals(t.phase)) {
                phasesRight = false;
            }
        }
        ok("every tenancy's house is the house the progressed Moon is actually in", housesRight);
        ok("the sign recorded is the progressed Moon's own sign", signsRight);
        ok("the phase recorded is the progressed lunation phase", phasesRight);

        ok("the first tenancy opens with the window",
            Math.abs(year.get(0).jdFrom - from) < 1e-6);
        ok("the last closes with it",
            Math.abs(year.get(year.size() - 1).jdTo - to) < 1e-6);
        ok("the Moon is found already in the first house, not entering it", !year.get(0).entered);
        boolean contiguous = true;
        boolean laterEntered = true;
        for (int i = 1; i < year.size(); i++) {
            if (Math.abs(year.get(i).jdFrom - year.get(i - 1).jdTo) > 1e-6) {
                contiguous = false;
            }
            if (!year.get(i).entered) {
                laterEntered = false;
            }
        }
        ok("the tenancies are contiguous - no gap and no overlap", contiguous);
        ok("and every later one is an ingress", laterEntered);

        // ---- thirty years: the technique's own period, which the scan cannot invent
        List<Progressions.Tenancy> sweep =
            Progressions.clock(sw, jd, f.cusps, jd + 365.25, jd + 365.25 * 31.0);
        int ingresses = sweep.size() - 1;
        ok("thirty years hold a full cycle and a little more: " + ingresses
            + " ingresses, twelve to a cycle in about 27.3 years",
            ingresses >= 12 && ingresses <= 15);

        boolean forward = true;
        boolean onCusp = true;
        double worst = 0.0;
        for (int i = 1; i < sweep.size(); i++) {
            int prev = sweep.get(i - 1).house;
            int now = sweep.get(i).house;
            if (now != prev % 12 + 1) {
                forward = false;
            }
            double off = Math.abs(Almanac.signedDelta(
                progressedMoon(sw, jd, sweep.get(i).jdFrom), Zodiac.normalise(f.cusps[now])));
            worst = Math.max(worst, off);
            if (off > 0.05) {
                onCusp = false;
            }
        }
        ok("the houses are entered in order, never skipped and never backwards", forward);
        ok(String.format("every ingress lands on the cusp it names (worst %.4f°)", worst), onCusp);

        // First ingress to last, over the gaps between them - one fewer than the ingresses.
        double span = (sweep.get(sweep.size() - 1).jdFrom - sweep.get(0).jdTo)
            / 365.25 / (ingresses - 1);
        ok(String.format("a house takes about two and a quarter years - 27.3 over twelve (%.2f)",
            span), span > 1.8 && span < 2.8);

        // ---- the guards
        ok("no cusps, no clock", Progressions.clock(sw, jd, null, from, to).isEmpty());
        ok("short cusps, no clock",
            Progressions.clock(sw, jd, new double[] {0, 1, 2}, from, to).isEmpty());
        ok("an empty window yields nothing", Progressions.clock(sw, jd, f.cusps, to, from).isEmpty());

        // ---- the testimony
        List<Convergence.Target> two = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)));
        ok("without the clock, two families are a background trend", !career(f, two, null).headline());
        ok("the clock in the 10th is Career's third testimony",
            career(f, two, List.of(tenancy(10, from, to, false))).headline());
        ok("and it is named as its own family",
            career(f, two, List.of(tenancy(10, from, to, false)))
                .families.contains(Convergence.Family.PROGRESSED_MOON));
        ok("the clock in the 2nd says nothing about Career",
            !career(f, two, List.of(tenancy(2, from, to, false))).headline());

        ThemeConvergence.Result home = theme(ThemeConvergence.Theme.HOME, f, List.of(),
            List.of(tenancy(4, from, from + 100, false), tenancy(3, from + 100, to, true)));
        ok("two of a theme's houses in one window are one testimony",
            home.families.size() == 1);
        ok("but both tenancies are listed, the second marked",
            home.testimonies.size() == 2
                && home.testimonies.get(1).contains("counted once"));
        ok("an ingress reads as entering, a tenancy already under way as holding",
            home.testimonies.get(0).contains("holds")
                && home.testimonies.get(1).contains("enters"));

        // ---- the clock loads the gun for the house it tenants
        String tenanted = null;
        int tenantedHouse = 0;
        String elsewhere = null;
        for (int i = 0; i < Bodies.count() && (tenanted == null || elsewhere == null); i++) {
            String n = Bodies.at(i).name;
            ChartFrame.Body b = f.body(n);
            if (b == null || !b.ok) {
                continue;
            }
            int h = Zodiac.houseOf(b.lon, f.cusps);
            if (tenanted == null && h != 0) {
                tenanted = n;
                tenantedHouse = h;
            } else if (tenanted != null && h != tenantedHouse && elsewhere == null) {
                elsewhere = n;
            }
        }
        List<Progressions.Tenancy> onIt = List.of(tenancy(tenantedHouse, from, to, false));
        java.util.Set<String> cold = ThemeConvergence.activePoints(f, null, List.of(), null);
        java.util.Set<String> warm = ThemeConvergence.activePoints(f, null, List.of(), onIt);
        ok(tenanted + " is in house " + tenantedHouse + " and nothing else has woken it",
            !cold.contains(tenanted));
        ok("the clock over that house activates it", warm.contains(tenanted));
        ok(elsewhere + ", in another house, stays inactive", !warm.contains(elsewhere));

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

    /** The progressed Moon's longitude at a real instant, from the ephemeris directly. */
    private static double progressedMoon(SwissEph sw, double natalJd, double jd) {
        return Almanac.bodyLongitude(sw, Progressions.progressedJd(natalJd, jd), "Moon");
    }

    private static Progressions.Tenancy tenancy(int house, double from, double to, boolean entered) {
        Progressions.Tenancy t = new Progressions.Tenancy();
        t.house = house;
        t.jdFrom = from;
        t.jdTo = to;
        t.entered = entered;
        t.sign = "aries";
        t.phase = "new";
        return t;
    }

    private static ThemeConvergence.Result career(ChartFrame f, List<Convergence.Target> t,
                                                  List<Progressions.Tenancy> clock) {
        return theme(ThemeConvergence.Theme.CAREER, f, t, clock);
    }

    private static ThemeConvergence.Result theme(ThemeConvergence.Theme want, ChartFrame f,
                                                 List<Convergence.Target> t,
                                                 List<Progressions.Tenancy> clock) {
        List<ThemeConvergence.Result> all = clock == null
            ? ThemeConvergence.themes(f, null, t)
            : ThemeConvergence.themes(f, null, t, clock);
        for (ThemeConvergence.Result r : all) {
            if (r.theme == want) {
                return r;
            }
        }
        throw new IllegalStateException("no " + want + " theme");
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
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
