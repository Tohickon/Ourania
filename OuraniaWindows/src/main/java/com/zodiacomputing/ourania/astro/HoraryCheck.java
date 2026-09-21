package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * K13, stage 2: significators, and whether the chart is fit to judge.
 *
 * <p><b>Both halves are decidable without an opinion, which is why they are built first.</b> Who
 * stands for whom is arithmetic on the cusps and the rulerships; whether the chart is radical is
 * arithmetic on the hour and the rising sign. Nothing here judges a question - that is stage 3 -
 * so nothing here needs a source beyond the decision of 19 September.
 *
 * <p>The significator assertions are made against a chart's own cusps rather than against
 * remembered values: the ruler of the house of the matter is recomputed from the sign on that
 * cusp, so the check fails if either the cusp or the rulership moves. The radicality assertions
 * are made by <b>constructing the hour</b> - finding a moment whose hour ruler is the Ascendant's
 * ruler, and another whose is not - because a fixture chosen for its date would pass or fail by
 * luck and teach nothing.
 */
public final class HoraryCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final double LAT = 39.95;
    private static final double LON = -75.17;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(2026, 9, 23, 17.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, LAT, LON, 'P', false, 0.0);

        // ---- who stands for whom
        for (Horary.Matter m : Horary.Matter.values()) {
            Horary.Significators s = Horary.significators(f, m);
            ok(m + " takes its significator from house " + m.house,
                s.quesitedSign == Zodiac.signIndex(f.cusps[m.house]));
            ok(m + "'s significator is that sign's ruler (" + s.quesited + ")",
                s.quesited.equals(Dignity.domicileRulerOf(Zodiac.signIndex(f.cusps[m.house]))));
            ok(m + " keeps the same querent", s.querent.equals(
                Dignity.domicileRulerOf(Zodiac.signIndex(f.asc))) && "Moon".equals(s.moon));
        }

        Horary.Significators any = Horary.significators(f, Horary.Matter.PARTNERS);
        ok("the querent is the ruler of the rising sign", any.querent.equals(
            Dignity.domicileRulerOf(Zodiac.signIndex(f.asc))));
        ok("the Moon always co-signifies the querent", "Moon".equals(any.moon));
        ok("the seventh is the house of partners", Horary.Matter.PARTNERS.house == 7);
        ok("the matters are the seven the decision names", Horary.Matter.values().length == 7);

        // <b>Shared significator, constructed rather than hoped for.</b> The Ascendant and the
        // house of the matter carry signs of one ruler often enough that a chart can be found
        // for it, and the flag has to be true exactly then.
        boolean sharedSeen = false;
        boolean sharedRight = true;
        for (int hour = 0; hour < 24 && !sharedSeen; hour++) {
            ChartFrame g = ChartFrame.compute(sw,
                new SweDate(2026, 9, 23, hour).getJulDay(), LAT, LON, 'P', false, 0.0);
            for (Horary.Matter m : Horary.Matter.values()) {
                Horary.Significators s = Horary.significators(g, m);
                boolean same = s.querent.equals(s.quesited);
                if (s.shared() != same) {
                    sharedRight = false;
                }
                if (same) {
                    sharedSeen = true;
                }
            }
        }
        ok("a chart where one planet stands for both sides is found in a day", sharedSeen);
        ok("and shared() agrees with it every time", sharedRight);

        // ---- radicality, with the hour constructed
        // Walk a day in ten-minute steps and take the first moment of each kind, so both
        // branches are exercised on real hours rather than on a fixture chosen for its date.
        Double radicalAt = null;
        Double notRadicalAt = null;
        for (int step = 0; step < 24 * 6 && (radicalAt == null || notRadicalAt == null); step++) {
            double t = new SweDate(2026, 9, 23, 0).getJulDay() + step / 144.0;
            ChartFrame g = ChartFrame.compute(sw, t, LAT, LON, 'P', false, 0.0);
            Horary.Radicality r = Horary.radicality(sw, g, t, LAT, LON);
            if (r.unknown) {
                continue;
            }
            if (r.radical() && radicalAt == null) {
                radicalAt = t;
            }
            if (!r.radical() && notRadicalAt == null) {
                notRadicalAt = t;
            }
        }
        ok("a radical moment exists in the day", radicalAt != null);
        ok("and a moment that is not radical", notRadicalAt != null);

        if (radicalAt != null) {
            ChartFrame g = ChartFrame.compute(sw, radicalAt, LAT, LON, 'P', false, 0.0);
            Horary.Radicality r = Horary.radicality(sw, g, radicalAt, LAT, LON);
            ok("a radical chart says which ground it passed on: " + r,
                r.sameRuler || r.sharesTriplicity);
            ok("the hour ruler it names is the hour ruler",
                r.hourRuler.equals(PlanetaryHours.rulerAt(sw, radicalAt, LAT, LON)));
            ok("the Ascendant ruler it names is the Ascendant's",
                r.ascendantRuler.equals(Dignity.domicileRulerOf(Zodiac.signIndex(g.asc))));
            if (r.sharesTriplicity && !r.sameRuler) {
                ChartFrame.Body sun = g.body("Sun");
                ok("and the triplicity it passed on is the one for this chart's sect",
                    r.triplicityRuler.equals(Dignity.triplicityRulerOf(
                        Zodiac.signIndex(g.asc), Sect.isDiurnal(sun.lon, g.asc))));
            }
        }
        if (notRadicalAt != null) {
            ChartFrame g = ChartFrame.compute(sw, notRadicalAt, LAT, LON, 'P', false, 0.0);
            Horary.Radicality r = Horary.radicality(sw, g, notRadicalAt, LAT, LON);
            ok("a chart that is not radical says so on both grounds: " + r,
                !r.sameRuler && !r.sharesTriplicity && !r.radical());
        }

        // <b>Sect is load-bearing here.</b> The same rising sign has a different triplicity
        // ruler by day and by night, so a radicality test that ignored sect would answer for
        // whichever half of the tradition it happened to be written from.
        boolean sectMatters = false;
        for (int sign = 0; sign < 12; sign++) {
            if (!Dignity.triplicityRulerOf(sign, true).equals(
                    Dignity.triplicityRulerOf(sign, false))) {
                sectMatters = true;
            }
        }
        ok("day and night triplicity rulers differ, so the sect in the test is load-bearing",
            sectMatters);

        // ---- where there are no hours, it refuses rather than guessing
        double polar = new SweDate(2026, 12, 21, 12.0).getJulDay();
        ChartFrame arctic = ChartFrame.compute(sw, polar, 78.2, 15.6, 'P', false, 0.0);
        Horary.Radicality none = Horary.radicality(sw, arctic, polar, 78.2, 15.6);
        ok("no hour ruler inside the polar night, so radicality is not judged", none.unknown);
        ok("and an unjudged chart is not called radical", !none.radical());

        report();
    }

    private static void report() {
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

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
