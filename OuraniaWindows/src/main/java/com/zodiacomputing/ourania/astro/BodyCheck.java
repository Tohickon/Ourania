package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks for the {@link Bodies} registry, the selection it persists, and the derived
 * points the wheel draws from it.
 *
 * Three of these parts exist because the registry's whole reason for being is that a body
 * is described in exactly one place, and a registry can only keep that promise if something
 * verifies the promise:
 *
 *   Part B asserts every registered name has its own entry in the orb table. Without it a
 *   new body reaches {@code Aspects.bodyOrb}'s default, which is the ANGLE orb of 8 degrees
 *   - wider than Pluto's - so the failure mode of forgetting is not a missing aspect but a
 *   minor asteroid aspecting more loudly than the outer planets.
 *
 *   Part D asserts the derived points agree with ChartFrame's L1 sequence to the last
 *   decimal. The Descendant, the IC, the south node and the Lot of Fortune are each
 *   computed for L1 and drawn on the wheel, and the lot in particular reverses on sect - so
 *   two copies of it would agree on every day chart and differ on every night chart, which
 *   is the least likely divergence to be noticed by looking.
 *
 * Run:
 *   javac -encoding UTF-8 -cp src\main\java -d out-selftest src\main\java\com\zodiacomputing\ourania\astro\*.java
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.BodyCheck
 */
public final class BodyCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    private static final double LAT = 34.05;
    private static final double LON = -118.24;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private BodyCheck() { }

    public static void main(String[] args) {
        System.out.println("=== Part A: registry integrity ===");
        int before = failures.size();
        registryIntegrity();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part A2: every ephemeris point actually resolves ===");
        before = failures.size();
        everyPointComputes();
        report("Part A2", before);

        System.out.println();
        System.out.println("=== Part B: every registered point has its own orb ===");
        before = failures.size();
        orbCoverage();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: selection round-trips through settings ===");
        before = failures.size();
        selectionRoundTrip();
        oppositePairs();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: derived points, and agreement with L1 ===");
        before = failures.size();
        derivedGeometry();
        agreementWithChartFrame();
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: one rule decides what counts as background ===");
        before = failures.size();
        primaryActor();
        report("Part E", before);

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

    // ---------------------------------------------------------------- part A

    private static void registryIntegrity() {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            String at = " (" + d.id + ")";

            ok("id is not blank" + at, d.id != null && !d.id.trim().isEmpty());
            ok("id is unique" + at, ids.add(d.id));
            ok("name is unique" + at, names.add(d.name));
            ok("glyph is not blank" + at, d.glyph != null && !d.glyph.isEmpty());
            ok("fallback is not blank" + at, d.fallback != null && !d.fallback.isEmpty());
            ok("meaning is not blank" + at, d.meaning != null && !d.meaning.trim().isEmpty());
            ok("has a group" + at, d.group != null);
            ok("has a kind" + at, d.kind != null);

            // An ipl on a derived point, or its absence on an ephemeris one, is the mistake
            // that would send swe_calc_ut a body number of -1 or silently leave a point at 0.
            if (d.source == Bodies.Source.EPHEMERIS) {
                ok("ephemeris point has a body number" + at, d.getIpl() >= 0);
                // Two legal ranges, not one. Up to SE_INTP_APOG are the built-in bodies
                // (SE_NPLANETS is package-private in the Java port, so the bound is the
                // highest number this app uses). At or above SE_AST_OFFSET are numbered
                // minor planets, which read a per-asteroid file.
                //
                // This used to assert the first range only, which was right while nothing
                // in the registry was a minor planet and wrong the moment five were added
                // on 2026-08-18. Widening it on its own would have weakened the suite into
                // silence, because what it was really standing in for is "will this point
                // actually produce a longitude" - so that is now asserted directly, in
                // Part A2 below. Pholus, incidentally, belongs in the FIRST range:
                // SweConst.SE_PHOLUS is 16.
                ok("body number is a main body or a numbered minor planet" + at,
                    d.getIpl() <= SweConst.SE_INTP_APOG || d.getIpl() >= SweConst.SE_AST_OFFSET);
            } else {
                ok("derived point carries no body number" + at, d.getIpl() == -1);
            }

            ok("element is -1 or 0..3" + at,
                d.element == Bodies.NO_ELEMENT || (d.element >= 0 && d.element <= 3));

            // The four angles are the objects drawn with a text label rather than a glyph,
            // and isAngle is what the wheel dispatches on.
            ok("isAngle agrees with kind" + at,
                d.isAngle() == (d.kind == Bodies.Kind.ANGLE));

            eq("indexOf finds itself" + at, i, Bodies.indexOf(d.id));
            eq("indexOfName finds itself" + at, i, Bodies.indexOfName(d.name));
            eq("indexOfName is case-insensitive" + at, i,
                Bodies.indexOfName(d.name.toUpperCase()));
        }

        eq("unknown id is -1", -1, Bodies.indexOf("no_such_body"));
        eq("null id is -1", -1, Bodies.indexOf(null));
        eq("unknown name is -1", -1, Bodies.indexOfName("Nibiru"));
        // InterpretationService's angle branch and the interpretation data both spell the
        // MC "Midheaven", so the registry has to answer to that name too.
        eq("Midheaven resolves to the MC", Bodies.indexOf("mc"),
            Bodies.indexOfName("Midheaven"));

        // The four angles, both nodes and the four asteroids are the point of the exercise;
        // asserting they are present stops a merge quietly dropping one.
        for (String id : new String[]{"sun", "moon", "mercury", "venus", "mars", "jupiter",
                                      "saturn", "uranus", "neptune", "pluto",
                                      "north_node", "south_node", "chiron",
                                      "ceres", "pallas", "juno", "vesta",
                                      "ascendant", "descendant", "mc", "ic",
                                      "fortune", "lilith"}) {
            ok("registry contains " + id, Bodies.indexOf(id) >= 0);
        }
    }

    /**
     * Every EPHEMERIS point in the registry returns a real longitude.
     *
     * This is the assertion Part A's body-number range check had been standing in for, and
     * standing in badly: a number inside the legal range says nothing about whether the
     * data file behind it is on disk. Five asteroids were added to the registry on
     * 2026-08-18 with correct numbers and no ephemeris files, so they were selectable in
     * Settings and returned "SwissEph file ... not found" - a range check cannot see that,
     * and a user would only find out by picking one and getting nothing.
     *
     * So this asks the ephemeris. It is the one check here that needs files on disk, which
     * is the point: it fails loudly when a shipped point has no data behind it.
     */
    private static void everyPointComputes() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        // Spread over the range a user will actually ask for. A per-asteroid file covers
        // 600 years, so a point can resolve today and fall off the end of its file in 1900.
        int[][] when = {
            {1900, 1, 1}, {1950, 6, 15}, {1990, 1, 1}, {2026, 8, 12}, {2050, 12, 31}
        };
        for (int[] date : when) {
            SweDate sd = new SweDate(date[0], date[1], date[2], 12.0);
            ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'P', false, 0.0);
            for (int i = 0; i < Bodies.count(); i++) {
                Bodies.Def d = Bodies.at(i);
                if (d.source != Bodies.Source.EPHEMERIS) {
                    continue;
                }
                String at = " (" + d.id + " at " + date[0] + ")";
                ChartFrame.Body b = f.body(d.name);
                ok("registry point is present in the frame" + at, b != null);
                if (b == null) {
                    continue;
                }
                ok("registry point computes" + at + (b.ok ? "" : ": " + b.error), b.ok);
                if (b.ok) {
                    ok("longitude is a real number in [0, 360)" + at,
                        !Double.isNaN(b.lon) && b.lon >= 0.0 && b.lon < 360.0);
                }
            }
        }
    }

    // ---------------------------------------------------------------- part B

    private static void orbCoverage() {
        for (int i = 0; i < Bodies.count(); i++) {
            String name = Bodies.at(i).name;
            ok("orb table has an entry for " + name, Aspects.hasExplicitOrb(name));
            ok("orb for " + name + " is positive", Aspects.orbOf(name) > 0.0);
            ok("orb for " + name + " is not wider than a luminary's",
                Aspects.orbOf(name) <= Aspects.orbOf("Sun"));
        }

        // Ordering, so a future edit cannot make an asteroid louder than a planet.
        ok("luminaries are the widest", Aspects.orbOf("Sun") > Aspects.orbOf("Mercury"));
        ok("outer planets are tighter than the social ones",
            Aspects.orbOf("Pluto") < Aspects.orbOf("Saturn"));
        ok("the nodes are tighter than the outer planets",
            Aspects.orbOf("North Node") < Aspects.orbOf("Pluto"));
        ok("the asteroids are tighter than the nodes",
            Aspects.orbOf("Ceres") < Aspects.orbOf("North Node"));
        ok("both nodes take the same orb",
            Aspects.orbOf("North Node") == Aspects.orbOf("South Node"));
        for (String a : new String[]{"Ceres", "Pallas", "Juno", "Vesta",
                                     "Part of Fortune", "Black Moon Lilith"}) {
            ok(a + " is tighter than any planet", Aspects.orbOf(a) < Aspects.orbOf("Pluto"));
        }

        // A pair takes the larger of the two, which is the rule the wheel draws lines by.
        eq("Sun-Vesta is judged on the Sun's orb",
            Aspects.orbOf("Sun"), Aspects.orbFor("Sun", "Vesta"), 1e-12);
        eq("an unrecognised name still gets the angle orb",
            Aspects.ANGLE_ORB, Aspects.orbOf("Nibiru"), 1e-12);
    }

    // ---------------------------------------------------------------- part C

    private static void selectionRoundTrip() {
        boolean[] defaults = Bodies.defaults();
        eq("defaults are one flag per point", Bodies.count(), defaults.length);
        ok("some points are on by default", anyTrue(defaults));
        ok("not every point is on by default", !allTrue(defaults));

        // An unwritten key means "never configured" and must not mean "nothing selected" -
        // that difference is a blank wheel on first launch.
        sameSelection("null parses as the defaults", defaults, Bodies.parse(null));
        sameSelection("defaults round-trip", defaults,
            Bodies.parse(Bodies.format(defaults)));

        boolean[] all = new boolean[Bodies.count()];
        java.util.Arrays.fill(all, true);
        sameSelection("all-on round-trips", all, Bodies.parse(Bodies.format(all)));

        // Explicitly nothing is a legitimate selection - it is what Select none asks for -
        // and it has to survive the round trip rather than reading back as the defaults.
        // Conflating the two is what made Select none revert at the next launch.
        boolean[] none = new boolean[Bodies.count()];
        eq("all-off formats as an empty string", "", Bodies.format(none));
        sameSelection("an empty list means nothing selected", none,
            Bodies.parse(Bodies.format(none)));
        sameSelection("an empty list is not the defaults", none, Bodies.parse(""));
        ok("empty and absent parse differently",
            !java.util.Arrays.equals(Bodies.parse(""), Bodies.parse(null)));

        // One point on, to prove format is not just emitting everything.
        boolean[] justMars = new boolean[Bodies.count()];
        justMars[Bodies.indexOf("mars")] = true;
        eq("a single point formats as its id", "mars", Bodies.format(justMars));
        sameSelection("a single point round-trips", justMars, Bodies.parse("mars"));

        // Hand-edited or rolled-back files must not throw or reject.
        sameSelection("unknown ids are ignored", justMars, Bodies.parse("mars,nibiru,planet_x"));
        sameSelection("whitespace is tolerated", justMars, Bodies.parse("  mars  ,  "));
        sameSelection("a whitespace-only list selects nothing", new boolean[Bodies.count()],
            Bodies.parse("   "));

        boolean[] pair = new boolean[Bodies.count()];
        pair[Bodies.indexOf("ceres")] = true;
        pair[Bodies.indexOf("vesta")] = true;
        sameSelection("order in the file does not matter", pair, Bodies.parse("vesta,ceres"));
    }

    private static void oppositePairs() {
        eq("the south node is the north node's opposition",
            Bodies.indexOf("north_node"), Bodies.oppositeOf(Bodies.indexOf("south_node")));
        eq("the Descendant is the Ascendant's opposition",
            Bodies.indexOf("ascendant"), Bodies.oppositeOf(Bodies.indexOf("descendant")));
        eq("the IC is the MC's opposition",
            Bodies.indexOf("mc"), Bodies.oppositeOf(Bodies.indexOf("ic")));
        eq("a body is nobody's opposition", -1, Bodies.oppositeOf(Bodies.indexOf("mars")));
        eq("out of range is -1", -1, Bodies.oppositeOf(-1));
        eq("out of range is -1", -1, Bodies.oppositeOf(Bodies.count()));

        // Symmetric in both argument orders, since the aspect loops hit it both ways.
        String[][] pairs = {{"north_node", "south_node"}, {"ascendant", "descendant"},
                            {"mc", "ic"}};
        for (String[] p : pairs) {
            int a = Bodies.indexOf(p[0]);
            int b = Bodies.indexOf(p[1]);
            ok(p[0] + "/" + p[1] + " is a pair", Bodies.isOppositePair(a, b));
            ok(p[1] + "/" + p[0] + " is a pair", Bodies.isOppositePair(b, a));
        }
        ok("Mars and Venus are not a pair",
            !Bodies.isOppositePair(Bodies.indexOf("mars"), Bodies.indexOf("venus")));
        ok("the Ascendant and the IC are not a pair",
            !Bodies.isOppositePair(Bodies.indexOf("ascendant"), Bodies.indexOf("ic")));
    }

    // ---------------------------------------------------------------- part D

    /**
     * The derived-point arithmetic from first principles, at several Ascendants so a
     * formula that accidentally works only for asc == 0 cannot pass, and across the 0/360
     * seam, which is where a missing wrap hides.
     */
    private static void derivedGeometry() {
        for (double asc : new double[]{0.0, 95.0, 179.9, 200.0, 359.5}) {
            for (double mc : new double[]{5.0, 271.0, 359.9}) {
                String at = String.format(" (asc %.1f, mc %.1f)", asc, mc);

                eq("ASC derives as itself" + at, Zodiac.normalise(asc),
                    Bodies.derive(Bodies.Source.ASC, asc, mc, 0, 0, 0), 1e-9);
                eq("MC derives as itself" + at, Zodiac.normalise(mc),
                    Bodies.derive(Bodies.Source.MC, asc, mc, 0, 0, 0), 1e-9);

                double dsc = Bodies.derive(Bodies.Source.DSC, asc, mc, 0, 0, 0);
                double ic = Bodies.derive(Bodies.Source.IC, asc, mc, 0, 0, 0);
                ok("DSC is in range" + at, dsc >= 0.0 && dsc < 360.0);
                ok("IC is in range" + at, ic >= 0.0 && ic < 360.0);
                eq("DSC is 180 from the ASC" + at, 180.0,
                    ChartFrame.separation(asc, dsc), 1e-9);
                eq("IC is 180 from the MC" + at, 180.0,
                    ChartFrame.separation(mc, ic), 1e-9);
                eq("opposing twice returns the original" + at, Zodiac.normalise(asc),
                    Zodiac.opposite(dsc), 1e-9);
            }
        }

        for (double node : new double[]{0.0, 45.0, 181.0, 359.99}) {
            double south = Bodies.derive(Bodies.Source.SOUTH_NODE, 0, 0, 0, 0, node);
            ok("south node is in range", south >= 0.0 && south < 360.0);
            eq("south node is 180 from the north", 180.0,
                ChartFrame.separation(node, south), 1e-9);
        }

        // The lot reverses on sect. Sun at the MC is a day chart, Sun at the IC a night one,
        // and the same three longitudes must therefore give two different answers.
        double asc = 100.0;
        double moon = 40.0;
        double daySun = asc - 90.0;      // at the MC
        double nightSun = asc + 90.0;    // at the IC
        ok("sun at the MC is a day chart", Sect.isDiurnal(daySun, asc));
        ok("sun at the IC is a night chart", !Sect.isDiurnal(nightSun, asc));

        double dayLot = Bodies.derive(Bodies.Source.FORTUNE, asc, 0.0, daySun, moon, 0.0);
        double nightLot = Bodies.derive(Bodies.Source.FORTUNE, asc, 0.0, nightSun, moon, 0.0);
        eq("the day lot is asc + moon - sun", Zodiac.normalise(asc + moon - daySun),
            dayLot, 1e-9);
        eq("the night lot is asc + sun - moon", Zodiac.normalise(asc + nightSun - moon),
            nightLot, 1e-9);
        ok("day and night lots differ", ChartFrame.separation(dayLot, nightLot) > 1.0);

        // Spirit is Fortune reflected in the Ascendant, in either sect.
        for (boolean diurnal : new boolean[]{true, false}) {
            double f = Sect.lotOfFortune(diurnal, asc, daySun, moon);
            double s = Sect.lotOfSpirit(diurnal, asc, daySun, moon);
            eq("Spirit mirrors Fortune across the asc (diurnal=" + diurnal + ")", 0.0,
                ChartFrame.separation(Zodiac.normalise(2 * asc - f), s), 1e-9);
        }

        eq("an ephemeris source does not derive", true,
            Double.isNaN(Bodies.derive(Bodies.Source.EPHEMERIS, 1, 2, 3, 4, 5)));
    }

    /**
     * The same four points, computed by L1 and by the registry, over charts spread across
     * the year and both sects. Exact agreement, not approximate: both call the same helpers
     * now, so any difference at all means a copy has come back.
     */
    private static void agreementWithChartFrame() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        int[][] when = {
            {2026, 8, 12}, {1990, 1, 1}, {1972, 3, 21}, {2001, 11, 7}, {2015, 6, 30}
        };
        // Both sects: 12:00 UT over Los Angeles is a day chart, 00:00 UT a night one.
        double[] hours = {19.0, 7.0};

        for (int[] date : when) {
            for (double hour : hours) {
                SweDate sd = new SweDate(date[0], date[1], date[2], hour);
                ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'P', false, 0.0);
                String at = String.format(" (%04d-%02d-%02d %04.1fh, %s)",
                    date[0], date[1], date[2], hour, f.diurnal ? "day" : "night");

                double asc = f.cusps[1];
                double mc = f.cusps[10];
                double sun = f.body("Sun").lon;
                double moon = f.body("Moon").lon;
                double node = f.body("North Node").lon;

                eq("cusp 1 is the Ascendant" + at, f.asc, Zodiac.normalise(asc), 1e-9);
                eq("cusp 10 is the MC" + at, f.mc, Zodiac.normalise(mc), 1e-9);

                eq("Descendant matches L1" + at, f.dsc,
                    Bodies.derive(Bodies.Source.DSC, asc, mc, sun, moon, node), 1e-12);
                eq("IC matches L1" + at, f.ic,
                    Bodies.derive(Bodies.Source.IC, asc, mc, sun, moon, node), 1e-12);
                eq("south node matches L1" + at, f.southNode,
                    Bodies.derive(Bodies.Source.SOUTH_NODE, asc, mc, sun, moon, node), 1e-12);
                eq("Part of Fortune matches L1" + at, f.lotOfFortune,
                    Bodies.derive(Bodies.Source.FORTUNE, asc, mc, sun, moon, node), 1e-12);
                // Spirit has TWO sources now: the frame field it has always had, and the
                // registry point added 2026-08-22 to give it a glyph, aspects and prose.
                // Two definitions of one value is exactly what made the aspect grid's href
                // rot, so they are asserted equal here and on the body itself below.
                eq("Part of Spirit matches L1" + at, f.lotOfSpirit,
                    Bodies.derive(Bodies.Source.SPIRIT, asc, mc, sun, moon, node), 1e-12);
                eq("the Part of Spirit body is the frame's lot" + at, f.lotOfSpirit,
                    f.body("Part of Spirit").lon, 1e-12);

                // Sect itself, since the lot's agreement is only meaningful if both sides
                // agree about which sect the chart is.
                eq("sect matches L1" + at, f.diurnal, Sect.isDiurnal(sun, asc));
            }
        }

        // And that both sects were actually exercised, rather than the loop having quietly
        // produced ten day charts.
        boolean sawDay = false;
        boolean sawNight = false;
        for (int[] date : when) {
            for (double hour : hours) {
                SweDate sd = new SweDate(date[0], date[1], date[2], hour);
                ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'P', false, 0.0);
                sawDay |= f.diurnal;
                sawNight |= !f.diurnal;
            }
        }
        ok("the corpus contains a day chart", sawDay);
        ok("the corpus contains a night chart", sawNight);
    }

    // ---------------------------------------------------------------- plumbing

    private static boolean anyTrue(boolean[] a) {
        for (boolean b : a) {
            if (b) {
                return true;
            }
        }
        return false;
    }

    private static boolean allTrue(boolean[] a) {
        for (boolean b : a) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    private static void sameSelection(String label, boolean[] expected, boolean[] actual) {
        checks++;
        if (expected.length != actual.length) {
            failures.add(label + ": length " + actual.length + ", expected " + expected.length);
            return;
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                failures.add(label + ": " + Bodies.at(i).id + " is " + actual[i]
                    + ", expected " + expected[i]);
                return;
            }
        }
    }

    /**
     * {@link Bodies#hasPrimaryActor} - the one rule the reading, the aspect card and the
     * returns table all ask.
     *
     * <b>They used to decide separately and had already drifted apart</b>: the reading asked
     * whether both ends were minor, the returns table whether the arriving end was. Both
     * answers were defensible and the pair of them was not, because nothing said which
     * question the app was asking. These assertions hold the two branches and, more
     * importantly, the reason they differ - direction is a property of the contact, not a
     * per-surface preference.
     */
    private static void primaryActor() {
        // Mutual: either end can carry it, so background needs both ends minor.
        ok("two minor bodies have no actor between them",
            !Bodies.hasPrimaryActor("Vesta", "Ceres", false));
        ok("a lot and a node have no actor between them",
            !Bodies.hasPrimaryActor("North Node", "Part of Fortune", false));
        ok("a planet at either end supplies one",
            Bodies.hasPrimaryActor("Sun", "Ceres", false)
                && Bodies.hasPrimaryActor("Ceres", "Sun", false));
        ok("an angle supplies one", Bodies.hasPrimaryActor("Ascendant", "Vesta", false));
        ok("two planets plainly supply one",
            Bodies.hasPrimaryActor("Sun", "Saturn", false));

        // Directional: only the arriving end is making the statement.
        ok("an arriving minor body has no actor however big the target",
            !Bodies.hasPrimaryActor("Pholus", "Sun", true));
        ok("an arriving planet has one however small the target",
            Bodies.hasPrimaryActor("Mars", "Vesta", true));
        ok("an arriving angle has one", Bodies.hasPrimaryActor("Ascendant", "Moon", true));

        // The shape of each branch, which is what stops them being quietly swapped.
        ok("the mutual branch is symmetric",
            Bodies.hasPrimaryActor("Sun", "Vesta", false)
                == Bodies.hasPrimaryActor("Vesta", "Sun", false));
        ok("the directional branch is not symmetric",
            Bodies.hasPrimaryActor("Sun", "Vesta", true)
                != Bodies.hasPrimaryActor("Vesta", "Sun", true));
        ok("directional is never more permissive than mutual",
            !Bodies.hasPrimaryActor("Pholus", "Sun", true)
                && Bodies.hasPrimaryActor("Pholus", "Sun", false));

        // Every registered body agrees with isMinor on both branches, so the two cannot be
        // tuned apart for some bodies and not others.
        int disagreed = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            String a = Bodies.at(i).name;
            if (Bodies.hasPrimaryActor(a, "Sun", true) != !Bodies.isMinor(a)) {
                disagreed++;
            }
            if (Bodies.hasPrimaryActor(a, "Vesta", false) != !Bodies.isMinor(a)) {
                disagreed++;
            }
        }
        eq("both branches agree with isMinor across the whole registry", 0, disagreed);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void eq(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(String.format("%s: got %.12f, expected %.12f", label, actual, expected));
        }
    }

    private static void report(String part, int before) {
        int failed = failures.size() - before;
        System.out.println(failed == 0 ? part + ": clear" : part + ": " + failed + " failed");
    }
}
