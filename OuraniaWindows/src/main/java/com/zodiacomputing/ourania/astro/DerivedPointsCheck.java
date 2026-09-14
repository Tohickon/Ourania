package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Master list D10: the Vertex, the East Point and the five Hermetic lots.
 *
 * <p>Parts A and C lean on facts that do not come from this code. The general lot formula has to
 * reproduce Fortune and Spirit, which the app has computed and checked for a month; and at the
 * equator the East Point - the equatorial Ascendant - is the Ascendant, because there the
 * horizon and the equator's prime vertical coincide in the east.
 */
public final class DerivedPointsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    static final String[] NEW_IDS = {"vertex", "east_point", "lot_eros", "lot_necessity",
        "lot_courage", "lot_victory", "lot_nemesis"};

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        // A frame marks unselected points not ok, so every point is switched on - in the scratch
        // file - the way a reader who wants these points would.
        boolean[] all = new boolean[Bodies.count()];
        java.util.Arrays.fill(all, true);
        com.zodiacomputing.ourania.gui.Settings.saveBodySelection(all);
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double birth = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);

        part("A: one lot formula, and it is Fortune's", DerivedPointsCheck::formula);
        part("B: the registry", DerivedPointsCheck::registry);
        part("C: the Vertex and East Point are the sky's", () -> angles(sw, birth));
        part("D: each lot is its formula on a real chart, day and night", () -> lots(sw, birth));
        part("E: no birth time, none of them", () -> noTime(sw, birth));
        part("F: a composite takes them from its own house frame", () -> composite(sw, birth));

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

    private static void formula() {
        double worstF = 0.0;
        double worstS = 0.0;
        double worstSum = 0.0;
        java.util.Random rnd = new java.util.Random(11);
        for (int i = 0; i < 2000; i++) {
            double asc = rnd.nextDouble() * 360, sun = rnd.nextDouble() * 360, moon = rnd.nextDouble() * 360;
            double a = rnd.nextDouble() * 360, b = rnd.nextDouble() * 360;
            for (boolean day : new boolean[]{true, false}) {
                worstF = Math.max(worstF, Aspects.separation(Sect.hermeticLot(day, asc, sun, moon),
                    Sect.lotOfFortune(day, asc, sun, moon)));
                worstS = Math.max(worstS, Aspects.separation(Sect.hermeticLot(day, asc, moon, sun),
                    Sect.lotOfSpirit(day, asc, sun, moon)));
            }
            // Day and night reverse the arc, so the two always sum to twice the Ascendant.
            worstSum = Math.max(worstSum, Aspects.separation(
                Sect.hermeticLot(true, asc, a, b) + Sect.hermeticLot(false, asc, a, b), 2 * asc));
        }
        ok("from the Sun to the Moon is the Lot of Fortune, day and night, worst " + worstF, worstF < 1e-9);
        ok("from the Moon to the Sun is the Lot of Spirit, worst " + worstS, worstS < 1e-9);
        ok("a lot's day and night forms sum to twice the Ascendant, worst " + worstSum, worstSum < 1e-9);
    }

    private static void registry() {
        int lilith = Bodies.indexOf("lilith");
        for (int k = 0; k < NEW_IDS.length; k++) {
            int i = Bodies.indexOf(NEW_IDS[k]);
            ok(NEW_IDS[k] + " is registered", i >= 0);
            if (i < 0) {
                continue;
            }
            ok(NEW_IDS[k] + " comes after every point that existed before it", i > lilith);
            ok(NEW_IDS[k] + " is off by default", !Bodies.at(i).defaultOn);
            ok(NEW_IDS[k] + " needs a birth time", Bodies.needsBirthTime(Bodies.at(i).source));
            ok(NEW_IDS[k] + " has its own orb", Aspects.hasExplicitOrb(Bodies.at(i).name));
        }
        ok("the south node needs no birth time",
            !Bodies.needsBirthTime(Bodies.at(Bodies.indexOf("south_node")).source));
        boolean[] on = new boolean[Bodies.count()];
        on[Bodies.indexOf("vertex")] = true;
        on[Bodies.indexOf("lot_nemesis")] = true;
        boolean[] back = Bodies.parse(Bodies.format(on));
        ok("a selection of the new points round-trips through settings",
            back[Bodies.indexOf("vertex")] && back[Bodies.indexOf("lot_nemesis")]
                && !back[Bodies.indexOf("lot_eros")]);
        ok("the five-number derive leaves the new points to the full one",
            Double.isNaN(Bodies.derive(Bodies.Source.VERTEX, 1, 2, 3, 4, 5))
                && Double.isNaN(Bodies.derive(Bodies.Source.LOT_EROS, 1, 2, 3, 4, 5)));
    }

    private static ChartFrame frame(SwissEph sw, double jd, double lat, double lon) {
        return ChartFrame.compute(sw, jd, lat, lon, 'P', false, 0.0);
    }

    private static void angles(SwissEph sw, double birth) {
        ChartFrame f = frame(sw, birth, 39.9526, -75.1652);
        double[] cusps = new double[13];
        double[] ascmc = new double[10];
        sw.swe_houses(birth, Ephemeris.flags(sw, de.thmac.swisseph.SweConst.SEFLG_SWIEPH),
            39.9526, -75.1652, 'P', cusps, ascmc);
        near("the Vertex body is swe_houses' Vertex", f.body("Vertex").lon, ascmc[3]);
        near("the East Point body is swe_houses' East Point", f.body("East Point").lon, ascmc[4]);
        ok("both are placed", f.body("Vertex").ok && f.body("East Point").ok);

        double worst = 0.0;
        for (int h = 0; h < 24; h++) {
            ChartFrame eq = frame(sw, birth + h / 24.0, 0.0, 30.0);
            worst = Math.max(worst, Aspects.separation(eq.body("East Point").lon, eq.asc));
        }
        ok("at the equator the East Point is the Ascendant, every hour of a day, worst " + worst,
            worst < 0.01);
        ok("away from the equator it is not", Aspects.separation(f.body("East Point").lon, f.asc) > 1.0);
        // The Vertex is in the west: for a mid-latitude birth it lies within a quadrant of the
        // Descendant.
        ok("the Vertex lies on the western side, " + Math.round(Aspects.separation(f.body("Vertex").lon, f.dsc))
            + " degrees from the Descendant", Aspects.separation(f.body("Vertex").lon, f.dsc) < 90.0);
    }

    private static void lots(SwissEph sw, double birth) {
        for (double[] when : new double[][]{{birth}, {birth + 0.5}}) {
            ChartFrame f = frame(sw, when[0], 39.9526, -75.1652);
            double sun = f.body("Sun").lon, moon = f.body("Moon").lon, asc = f.asc;
            boolean day = Sect.isDiurnal(sun, asc);
            String dn = day ? "day" : "night";
            double fortune = Sect.lotOfFortune(day, asc, sun, moon);
            double spirit = Sect.lotOfSpirit(day, asc, sun, moon);
            // Written out here the long way, so a sign slip in either direction fails.
            double eros = day ? asc + f.body("Venus").lon - spirit : asc + spirit - f.body("Venus").lon;
            double necessity = day ? asc + fortune - f.body("Mercury").lon : asc + f.body("Mercury").lon - fortune;
            double courage = day ? asc + fortune - f.body("Mars").lon : asc + f.body("Mars").lon - fortune;
            double victory = day ? asc + f.body("Jupiter").lon - spirit : asc + spirit - f.body("Jupiter").lon;
            double nemesis = day ? asc + fortune - f.body("Saturn").lon : asc + f.body("Saturn").lon - fortune;
            near(dn + ": Lot of Eros", f.body("Lot of Eros").lon, eros);
            near(dn + ": Lot of Necessity", f.body("Lot of Necessity").lon, necessity);
            near(dn + ": Lot of Courage", f.body("Lot of Courage").lon, courage);
            near(dn + ": Lot of Victory", f.body("Lot of Victory").lon, victory);
            near(dn + ": Lot of Nemesis", f.body("Lot of Nemesis").lon, nemesis);
            ok(dn + " chart is the sect it should be (15:01 is day, 03:01 night)",
                day == (when[0] == birth));
        }
    }

    private static void noTime(SwissEph sw, double birth) {
        ChartFrame f = ChartFrame.computeTimeUnknown(sw, birth, 39.9526, -75.1652, 'P', false, 0.0);
        for (String id : NEW_IDS) {
            ok(id + " is withheld without a birth time", !f.bodies[Bodies.indexOf(id)].ok);
        }
        ok("and so are Fortune and the Ascendant, as before",
            !f.body("Part of Fortune").ok && !f.body("Ascendant").ok);
        ok("but the south node is kept", f.body("South Node").ok);
    }

    private static void composite(SwissEph sw, double birth) {
        ChartFrame a = frame(sw, birth, 39.9526, -75.1652);
        ChartFrame b = frame(sw, SweDate.getJulDay(1985, 3, 2, 14.5), 34.05, -118.24);
        ChartFrame c = ChartFrame.computeMidpointComposite(sw, a, b);
        near("the composite Vertex is its house frame's", c.body("Vertex").lon, c.vertex);
        near("the composite East Point is its house frame's", c.body("East Point").lon, c.equatorialAsc);
    }

    private static void near(String label, double got, double want) {
        ok(label + String.format(", off by %.6f", Aspects.separation(got, want)),
            Aspects.separation(got, Zodiac.normalise(want)) < 1e-6);
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
