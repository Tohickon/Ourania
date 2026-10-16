package com.zodiacomputing.ourania.astro;

import com.zodiacomputing.ourania.gui.ReadingCard;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PlacementVectorCalculator: the scalars, the in-place vector ops, the buffers' reuse, and -
 * the part that matters - that the chart weight can grade a theme but never make one.
 *
 * <p>The chart is ThemeConvergenceCheck's (Chicago, 1984-09-08 07:33), so the theme members
 * these checks lean on are the ones that suite already pins.
 */
public final class PlacementVectorCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, 41.8781, -87.6298, 'P', false, 0.0);

        scalars();
        vectorOps();
        chartModel(f);
        ruleOfThree(f);
        cards(f);
        layering();

        if (failures.isEmpty()) {
            System.out.println("PlacementVectorCheck: ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("PlacementVectorCheck: FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String s : failures) {
                System.out.println("  " + s);
            }
            System.exit(1);
        }
    }

    // ---- scalars

    private static void scalars() {
        near("orb 0 decays to 1", 1.0, PlacementVectorCalculator.gaussianOrbDecay(0.0, 2.0));
        near("orb of one sigma decays to exp(-1/2)", Math.exp(-0.5),
            PlacementVectorCalculator.gaussianOrbDecay(2.0, 2.0));
        ok("decay falls with orb", PlacementVectorCalculator.gaussianOrbDecay(3.0, 2.0)
            < PlacementVectorCalculator.gaussianOrbDecay(1.0, 2.0));

        near("an exact trine weighs 1", 1.0, PlacementVectorCalculator.aspectWeight(10.0, 130.0));
        near("a conjunction across 0 Aries is measured the short way", Math.exp(-0.5),
            PlacementVectorCalculator.aspectWeight(359.0, 1.0));
        eq("129 degrees is 9 from a trine and nothing else: 0", 0.0,
            PlacementVectorCalculator.aspectWeight(0.0, 129.0));

        eq("of sect is 1.2", 1.2, PlacementVectorCalculator.sectScalar(Sect.Membership.OF_SECT));
        eq("contrary to sect is 0.8", 0.8,
            PlacementVectorCalculator.sectScalar(Sect.Membership.CONTRARY_TO_SECT));
        eq("neutral is 1.0", 1.0, PlacementVectorCalculator.sectScalar(Sect.Membership.NEUTRAL));
        near("domicile of sect: (1 + 5/10) x 1.2 = 1.8", 1.8,
            PlacementVectorCalculator.dignityScalar(Dignity.PTS_DOMICILE, Sect.Membership.OF_SECT, 1.0));
        near("no body falls below 0.1", 0.1,
            PlacementVectorCalculator.dignityScalar(-15, Sect.Membership.CONTRARY_TO_SECT, 1.0));

        eq("the Sun weighs 1.0", 1.0, PlacementVectorCalculator.hierarchyWeight("Sun"));
        eq("Venus weighs 0.85", 0.85, PlacementVectorCalculator.hierarchyWeight("Venus"));
        eq("Pluto weighs 0.65", 0.65, PlacementVectorCalculator.hierarchyWeight("Pluto"));
        ok("Chiron weighs less than Pluto", PlacementVectorCalculator.hierarchyWeight("Chiron")
            < PlacementVectorCalculator.hierarchyWeight("Pluto"));
    }

    // ---- vector ops

    private static void vectorOps() {
        double[] out = new double[4];
        PlacementVectorCalculator.kronecker(new double[] {1, 2}, new double[] {3, 4}, out);
        ok("kronecker {1,2} x {3,4} = {3,4,6,8}",
            java.util.Arrays.equals(out, new double[] {3, 4, 6, 8}));
        PlacementVectorCalculator.hadamardInPlace(out, new double[] {1, 0.5, 2, 0.25});
        ok("hadamard in place = {3,2,12,2}", java.util.Arrays.equals(out, new double[] {3, 2, 12, 2}));

        PlacementVectorCalculator.SparseModifier m = new PlacementVectorCalculator.SparseModifier(
            new int[] {7, 200}, new double[] {1.5, 0.5});
        eq("a listed key gives its value", 1.5, m.at(7));
        eq("an absent key is neutral", 1.0, m.at(8));
        eq("the empty table is neutral everywhere", 1.0,
            PlacementVectorCalculator.SparseModifier.NEUTRAL.at(123));
    }

    // ---- the chart model

    private static void chartModel(ChartFrame f) {
        List<PlacementVectorCalculator.Placement> ps = PlacementVectorCalculator.placementsOf(f);
        ok("every computed body is placed", ps.size() >= 10);
        for (PlacementVectorCalculator.Placement p : ps) {
            if (!Dignity.isTraditional(p.body())) continue;
            eq(p.body() + "'s essential score is Dignity's",
                Dignity.evaluate(p.body(), p.longitude(), f.diurnal).score, p.essential());
        }

        PlacementVectorCalculator.Workspace ws = new PlacementVectorCalculator.Workspace();
        PlacementVectorCalculator.Result a = PlacementVectorCalculator.compute(f, ps, ws,
            PlacementVectorCalculator.SparseModifier.NEUTRAL, PlacementVectorCalculator.SparseModifier.NEUTRAL);
        double[] first = a.themeWeight.clone();
        double[] sunVector = a.vectorOf("Sun");
        PlacementVectorCalculator.Result b = PlacementVectorCalculator.compute(f, ps, ws,
            PlacementVectorCalculator.SparseModifier.NEUTRAL, PlacementVectorCalculator.SparseModifier.NEUTRAL);
        ok("a reused workspace gives the same weights, nothing left over from the last run",
            java.util.Arrays.equals(first, b.themeWeight));
        ok("and reuses the same buffers rather than allocating", sunVector == b.vectorOf("Sun"));
        for (ThemeConvergence.Theme t : ThemeConvergence.Theme.values()) {
            ok(t + " has positive weight", a.weightOf(t) > 0.0);
        }

        // A Sabian modifier on the Sun's degree changes the chart by the Sun's share and no more.
        double sunLon = f.body("Sun").lon;
        int deg = (int) Math.floor(Zodiac.normalise(sunLon));
        PlacementVectorCalculator.Result base = PlacementVectorCalculator.compute(f, ps,
            new PlacementVectorCalculator.Workspace(),
            PlacementVectorCalculator.SparseModifier.NEUTRAL, PlacementVectorCalculator.SparseModifier.NEUTRAL);
        PlacementVectorCalculator.Result boosted = PlacementVectorCalculator.compute(f, ps,
            new PlacementVectorCalculator.Workspace(), PlacementVectorCalculator.SparseModifier.NEUTRAL,
            new PlacementVectorCalculator.SparseModifier(new int[] {deg}, new double[] {2.0}));
        ok("a Sabian modifier on the Sun's degree raises the chart's weight",
            sum(boosted.themeWeight) > sum(base.themeWeight));
        PlacementVectorCalculator.Result elsewhere = PlacementVectorCalculator.compute(f, ps,
            new PlacementVectorCalculator.Workspace(), PlacementVectorCalculator.SparseModifier.NEUTRAL,
            new PlacementVectorCalculator.SparseModifier(new int[] {emptyDegree(ps)}, new double[] {2.0}));
        ok("one on a degree nothing occupies changes nothing",
            java.util.Arrays.equals(base.themeWeight, elsewhere.themeWeight));

        // The house mask: a body in the 10th that signifies Relationship no more than Career
        // leans to Career in its own vector. Read from the base vector, before aspects mix it.
        java.util.Set<String> relSig = PlacementVectorCalculator.significatorsOf(
            ThemeConvergence.Theme.RELATIONSHIP, f);
        java.util.Set<String> carSig = PlacementVectorCalculator.significatorsOf(
            ThemeConvergence.Theme.CAREER, f);
        PlacementVectorCalculator.Workspace bare = new PlacementVectorCalculator.Workspace();
        PlacementVectorCalculator.compute(f, ps, bare,
            PlacementVectorCalculator.SparseModifier.NEUTRAL, PlacementVectorCalculator.SparseModifier.NEUTRAL);
        boolean tested = false;
        for (int i = 0; i < ps.size(); i++) {
            PlacementVectorCalculator.Placement p = ps.get(i);
            if (p.house() != 10 || (relSig.contains(p.body()) && !carSig.contains(p.body()))) continue;
            double career = block(bare.base[i], ThemeConvergence.Theme.CAREER);
            double rel = block(bare.base[i], ThemeConvergence.Theme.RELATIONSHIP);
            ok(p.body() + " in the 10th leans to Career over Relationship (" + career + " > " + rel + ")",
                career > rel);
            tested = true;
        }
        ok("the chart has a body in the 10th to test the house mask on", tested);
    }

    private static double block(double[] v, ThemeConvergence.Theme t) {
        double s = 0;
        for (int e = 0; e < PlacementVectorCalculator.ELEMENTS; e++) {
            s += v[t.ordinal() * PlacementVectorCalculator.ELEMENTS + e];
        }
        return s;
    }

    // ---- the Rule of Three

    private static void ruleOfThree(ChartFrame f) {
        PlacementVectorCalculator.Result vec = PlacementVectorCalculator.compute(f);

        List<Convergence.Target> two = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)));
        PlacementVectorCalculator.ThemeReading career2 = career(f, two, vec);
        ok("two families are not a headline, whatever the chart weight", !career2.headline());
        eq("and grade QUIET", PlacementVectorCalculator.IntensityBand.QUIET, career2.band());

        // Make Career the heaviest theme outright, and it is still not a headline on two families.
        PlacementVectorCalculator.Result heavy = PlacementVectorCalculator.compute(f);
        heavy.themeWeight[ThemeConvergence.Theme.CAREER.ordinal()] = 1000.0;
        PlacementVectorCalculator.ThemeReading loaded = career(f, two, heavy);
        eq("the heaviest theme on two families is still QUIET",
            PlacementVectorCalculator.IntensityBand.QUIET, loaded.band());
        ok("and still not a headline", !loaded.headline());

        List<Convergence.Target> three = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)),
            target("Saturn", w(Convergence.Family.PROGRESSION, true)));
        PlacementVectorCalculator.ThemeReading career3 = career(f, three, vec);
        ok("three families are a headline", career3.headline());
        eq("graded by its share of the heaviest theme",
            PlacementVectorCalculator.IntensityBand.fromShare(career3.share()), career3.band());

        // A station that echoes a transit is marked not independent by Convergence; it stays out.
        List<Convergence.Target> echo = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)),
            target("Saturn", w(Convergence.Family.STATION, false)));
        ok("an echo does not make the third family", !career(f, echo, heavy).headline());

        // Ordering: a headline comes before a heavier background theme.
        PlacementVectorCalculator.Result lightCareer = PlacementVectorCalculator.compute(f);
        lightCareer.themeWeight[ThemeConvergence.Theme.CAREER.ordinal()] = 0.01;
        List<PlacementVectorCalculator.ThemeReading> rs = PlacementVectorCalculator.readings(
            ThemeConvergence.themes(f, null, three), lightCareer);
        eq("the lightest headline still reads first", ThemeConvergence.Theme.CAREER, rs.get(0).theme());
        ok("and the families are ThemeConvergence's own",
            rs.get(0).convergence().families.size() == 3);
    }

    // ---- the cards

    private static void cards(ChartFrame f) {
        PlacementVectorCalculator.Result vec = PlacementVectorCalculator.compute(f);
        List<Convergence.Target> three = List.of(
            target("MC", w(Convergence.Family.TRANSIT, true), w(Convergence.Family.SOLAR_ARC, true)),
            target("Saturn", w(Convergence.Family.PROGRESSION, true)));
        PlacementVectorCalculator.ThemeReading r = career(f, three, vec);
        ReadingCard c = ReadingCard.fromTheme(r, "Work asks more of you this year.",
            "People notice what you do.", "Say yes to the visible job.");

        eq("the label is the theme's plain name", ThemeConvergence.Theme.CAREER.label, c.label());
        ok("with no band in it", !c.label().contains(r.band().label.toUpperCase())
            && !c.label().contains("["));
        ok("the body ends with the takeaway", c.body().endsWith("How to handle it: Say yes to the visible job."));
        ok("the why lines carry the chart weight", String.join(";", c.why()).contains("Chart weight"));
        ok("and the Rule of Three verdict", c.why().get(0).contains("Rule of Three met"));

        String plain = ReadingCard.html(List.of(c), false);
        String astrologer = ReadingCard.html(List.of(c), true);
        ok("plain view shows no chart weight", !plain.contains("Chart weight"));
        ok("plain view shows no decimal", !plain.matches("(?s).*\\d\\.\\d.*"));
        ok("Astrologer view shows it", astrologer.contains("Chart weight"));

        ReadingCard fallback = ReadingCard.fromTheme(r, null, null, null);
        eq("no headline falls back to the theme's name", ThemeConvergence.Theme.CAREER.label, fallback.headline());
    }

    // ---- layering: astro does not reach into gui

    private static void layering() throws Exception {
        Path src = Path.of("src/main/java/com/zodiacomputing/ourania/astro/PlacementVectorCalculator.java");
        if (!Files.exists(src)) {
            src = Path.of("OuraniaWindows").resolve(src);
        }
        ok("the calculator's source is found (" + src + ")", Files.exists(src));
        if (Files.exists(src)) {
            String text = Files.readString(src);
            ok("the calculator imports nothing from gui",
                !text.contains("import com.zodiacomputing.ourania.gui"));
        }
    }

    // ---- helpers

    private static PlacementVectorCalculator.ThemeReading career(ChartFrame f, List<Convergence.Target> t,
                                                                 PlacementVectorCalculator.Result vec) {
        for (PlacementVectorCalculator.ThemeReading r
                : PlacementVectorCalculator.readings(ThemeConvergence.themes(f, null, t), vec)) {
            if (r.theme() == ThemeConvergence.Theme.CAREER) {
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

    private static int emptyDegree(List<PlacementVectorCalculator.Placement> ps) {
        for (int d = 0; d < 360; d++) {
            boolean taken = false;
            for (PlacementVectorCalculator.Placement p : ps) {
                if ((int) Math.floor(Zodiac.normalise(p.longitude())) == d) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return d;
            }
        }
        throw new IllegalStateException("every degree occupied");
    }

    private static double sum(double[] v) {
        double s = 0;
        for (double x : v) s += x;
        return s;
    }

    private static void near(String label, double expected, double actual) {
        checks++;
        if (Math.abs(expected - actual) > 1e-9) {
            failures.add(label + ": expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": expected <" + expected + "> but got <" + actual + ">");
        }
    }
}
