package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.List;
import java.util.Random;

/**
 * {@link HarmonicResonance}: the primitive filter, the ranking, and the two artefacts that
 * would otherwise sit at the top of every chart ever cast.
 *
 * <p><b>Part D is the reason this suite exists.</b> The filter and the sort are ordinary code
 * and a reviewer can read them. What no reviewer would catch by reading is that North Node
 * against South Node is a 0.0000 degree orb in H2 - genuinely new at H2 by the gcd rule, since
 * k = 1 - so leaving it in puts the same pair at rank 1 of every chart, and the list still
 * looks entirely plausible. Part D asserts across a hundred charts that it never appears.
 *
 * <p><b>The check count here does not move with `settings.properties`.</b> Every part that
 * walks a list of contacts counts one check per property rather than one per contact, through
 * {@link Rule} - because {@code ChartFrame.compute} applies the user's body selection, so the
 * length of any such list is a setting. That is the stamp defect HANDOVER has been warning
 * about; this suite is written not to add to it.
 */
public final class HarmonicResonanceCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    private static final java.util.List<String> failures = new java.util.ArrayList<String>();
    private static int checks = 0;

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        System.out.println("=== Part A: what is new at a harmonic ===");
        int before = failures.size();
        theFilter();
        report("Part A", before);

        System.out.println("=== Part B: the shape of a ranking ===");
        before = failures.size();
        theRanking();
        report("Part B", before);

        System.out.println("=== Part C: the five are the top of the whole list ===");
        before = failures.size();
        thePrefix();
        report("Part C", before);

        System.out.println("=== Part D: the artefacts that would own rank 1 ===");
        before = failures.size();
        theArtefacts();
        report("Part D", before);

        System.out.println("=== Part E: five means five ===");
        before = failures.size();
        fiveMeansFive();
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
        System.exit(0);
    }

    /**
     * The gcd rule, on a pair placed at an exact angle and nowhere else.
     *
     * Every case here is the same question asked twice: does the contact appear at the
     * harmonic it belongs to, and is it <b>absent</b> from the multiples of that harmonic
     * which also contain it geometrically. The second half is the whole filter - without it a
     * trine is reported as a finding in H3, H6 and H9 alike.
     */
    private static void theFilter() {
        ok("gcd(0, n) is n, so a radix conjunction is new nowhere",
            HarmonicResonance.gcd(0, 5) == 5 && HarmonicResonance.gcd(0, 16) == 16);
        ok("gcd is symmetric and absolute",
            HarmonicResonance.gcd(8, 16) == 8 && HarmonicResonance.gcd(16, 8) == 8);
        ok("coprime pairs give 1", HarmonicResonance.gcd(3, 8) == 1);

        // A conjunction is a conjunction in every harmonic and a finding in none of them.
        eqInts("a radix conjunction reports nothing", new int[] {}, harmonicsFor(0.0));

        // Each of these is new at exactly one harmonic on the dial, and the multiples of that
        // harmonic which also contain it must not report it a second time.
        eqInts("an opposition is new at H2 only", new int[] {2}, harmonicsFor(180.0));
        eqInts("a trine is new at H3 only (not H6, not H9)",
            new int[] {3}, harmonicsFor(120.0));
        eqInts("a square is new at H4 only (not H8, not H16)",
            new int[] {4}, harmonicsFor(90.0));
        eqInts("a quintile is new at H5 only", new int[] {5}, harmonicsFor(72.0));
        eqInts("a sextile is new at H6 only", new int[] {6}, harmonicsFor(60.0));
        eqInts("a septile is new at H7 only", new int[] {7}, harmonicsFor(360.0 / 7.0));
        eqInts("a semisquare is new at H8 only (not H16)",
            new int[] {8}, harmonicsFor(45.0));
        eqInts("a sesquiquadrate is new at H8 too - k = 3 is coprime to 8",
            new int[] {8}, harmonicsFor(135.0));
        eqInts("a novile is new at H9 only", new int[] {9}, harmonicsFor(40.0));
        eqInts("22.5 degrees is new at H16 - the modulo-16 dial's whole point",
            new int[] {16}, harmonicsFor(22.5));
        eqInts("a biquintile is new at H5 - k = 2 is coprime to 5",
            new int[] {5}, harmonicsFor(144.0));

        // The orb is measured in the harmonic chart, so a radix miss is multiplied by N.
        eqInts("0.2 deg off a quintile is inside the 1.5 deg orb (1.0 in H5)",
            new int[] {5}, harmonicsFor(72.2));
        eqInts("0.7 deg off a quintile is outside it (3.5 in H5)",
            new int[] {}, harmonicsFor(72.7));

        List<HarmonicResonance.Contact> near = scanPair(72.2);
        checks++;
        if (near.size() != 1) {
            failures.add("expected one contact 0.2 off a quintile, got " + near.size());
        } else {
            HarmonicResonance.Contact c = near.get(0);
            near("the orb is the separation in H5", 1.0, c.orb, 1e-9);
            near("the exact angle it misses is 72", 72.0, c.exactAngle(), 1e-9);
            near("the radix separation is carried unchanged", 72.2, c.radixSeparation, 1e-9);
            eq("the multiple is 1", Integer.valueOf(1), Integer.valueOf(c.multiple));
        }

        // H1 is the radix: every pair is trivially a conjunction in it, so a dial entry of 1
        // must be ignored rather than reporting all 300 pairs.
        checks++;
        if (!HarmonicResonance.all(pair(0.0, 72.0), new int[] {1}, 1.5, null).isEmpty()) {
            failures.add("H1 on the dial reported contacts; it is the radix");
        }
        checks++;
        if (!HarmonicResonance.all(pair(0.0, 72.0), new int[] {0, -3}, 1.5, null).isEmpty()) {
            failures.add("a harmonic below 1 on the dial reported contacts");
        }
        checks++;
        if (!HarmonicResonance.all(null, HarmonicResonance.DIAL, 1.5, null).isEmpty()) {
            failures.add("a null frame did not return an empty list");
        }
    }

    /**
     * What a caller is promised about the list it gets back.
     *
     * <b>One check per property, not one per contact - and that is deliberate.</b>
     * {@code ChartFrame.compute} reads the user's {@code settings.properties} body selection,
     * so a loop counting a check per returned contact would move this suite's total whenever
     * David turns a body off. That is the settings-dependent stamp HANDOVER has been carrying
     * a warning about since 2026-08-25, and the fix it names is for a suite to stop letting the
     * selection reach its count. Violations are still reported individually; only the counting
     * is pinned.
     */
    private static void theRanking() {
        ChartFrame r = chart(1982, 8, 10, 19 + 1.0 / 60.0);
        List<HarmonicResonance.Contact> five = HarmonicResonance.scan(r);

        eq("K is five", Integer.valueOf(5), Integer.valueOf(HarmonicResonance.K));
        ok("the scan returns at most K", five.size() <= HarmonicResonance.K);

        Rule sorted = new Rule("the list is not sorted by orb");
        Rule ceiling = new Rule("a contact is outside the orb ceiling");
        Rule dial = new Rule("a contact names a harmonic that is not on the dial");
        Rule fresh = new Rule("a contact is not new at its own harmonic");
        Rule notConjunct = new Rule("a contact has k = 0, which is a radix conjunction");
        Rule ordered = new Rule("a pair is not stored with indexA < indexB");
        Rule noAngles = new Rule("a contact involves an angle");
        Rule names = new Rule("a contact's names do not match its indices");
        Rule orbTrue = new Rule("an orb is not the separation in its own harmonic");
        Rule sepTrue = new Rule("a radix separation is not reproducible from the frame");

        double previous = -1.0;
        for (HarmonicResonance.Contact c : five) {
            sorted.test(c.orb >= previous, c.orb + " after " + previous);
            previous = c.orb;

            ceiling.test(c.orb <= HarmonicResonance.MAX_ORB, c.toString());
            dial.test(onDial(c.harmonic), c.toString());
            fresh.test(HarmonicResonance.gcd(c.multiple, c.harmonic) == 1, c.toString());
            notConjunct.test(c.multiple != 0, c.toString());
            ordered.test(c.indexA < c.indexB, c.toString());
            noAngles.test(!Bodies.at(c.indexA).isAngle() && !Bodies.at(c.indexB).isAngle(),
                c.toString());
            names.test(Bodies.at(c.indexA).name.equals(c.nameA)
                && Bodies.at(c.indexB).name.equals(c.nameB), c.toString());

            // The orb has to be the separation in that harmonic and not something adjacent to
            // it - this is the one number the whole feature is ranked on.
            double a = r.bodies[c.indexA].lon;
            double b = r.bodies[c.indexB].lon;
            orbTrue.test(Math.abs(Aspects.separation(Harmonics.map(a, c.harmonic),
                Harmonics.map(b, c.harmonic)) - c.orb) <= 1e-9, c.toString());
            sepTrue.test(Math.abs(Aspects.separation(a, b) - c.radixSeparation) <= 1e-9,
                c.toString());
        }
        assertAll(sorted, ceiling, dial, fresh, notConjunct, ordered, noAngles, names,
            orbTrue, sepTrue);

        // Two scans of one frame must agree entry for entry. The comparator is fully ordered
        // for this reason: exactly-equal orbs are common and a tie would let iteration order
        // decide which five a reader sees.
        List<HarmonicResonance.Contact> again = HarmonicResonance.scan(r);
        checks++;
        if (!sameList(five, again)) {
            failures.add("two scans of the same frame returned different lists");
        }
    }

    /** The ranking is the head of the full list, not a separate calculation. */
    private static void thePrefix() {
        ChartFrame r = chart(1982, 8, 10, 19 + 1.0 / 60.0);
        // The same body set scan(frame) uses, or this compares two different scans and the
        // prefix property would fail for a reason that is not a defect.
        List<HarmonicResonance.Contact> all = HarmonicResonance.all(
            r, HarmonicResonance.DIAL, HarmonicResonance.MAX_ORB,
            HarmonicResonance.classical());
        List<HarmonicResonance.Contact> five = HarmonicResonance.scan(r);

        ok("the full list is at least as long as the ranking", all.size() >= five.size());
        checks++;
        if (!sameList(five, all.subList(0, five.size()))) {
            failures.add("the five are not the head of the full list");
        }
        System.out.println("  the sample chart holds " + all.size()
            + " new contacts inside " + HarmonicResonance.MAX_ORB + " deg; showing "
            + five.size());
        for (HarmonicResonance.Contact c : five) {
            System.out.println("    " + c);
        }

        // Narrowing below the default narrows the list without letting anything else in.
        boolean[] personal = new boolean[Bodies.count()];
        for (String id : new String[] {"sun", "moon", "mercury", "venus", "mars"}) {
            personal[Bodies.indexOf(id)] = true;
        }
        List<HarmonicResonance.Contact> narrowed = HarmonicResonance.all(
            r, HarmonicResonance.DIAL, HarmonicResonance.MAX_ORB, personal);
        ok("narrowing the body set does not lengthen the list", narrowed.size() <= all.size());
        Rule respected = new Rule("a narrowed contact used a body that was not asked for");
        for (HarmonicResonance.Contact c : narrowed) {
            respected.test(personal[c.indexA] && personal[c.indexB], c.toString());
        }
        assertAll(respected);

        // classical() must hand out a fresh array, or one caller's narrowing narrows it for
        // everyone for the rest of the JVM's life.
        boolean[] mine = HarmonicResonance.classical();
        java.util.Arrays.fill(mine, false);
        int stillOn = 0;
        for (boolean on : HarmonicResonance.classical()) {
            if (on) {
                stillOn++;
            }
        }
        eq("classical() is a fresh array each call", Integer.valueOf(12),
            Integer.valueOf(stillOn));
        ok("the classical set excludes the angles and the asteroids",
            !HarmonicResonance.classical()[Bodies.indexOf("ascendant")]
                && !HarmonicResonance.classical()[Bodies.indexOf("vesta")]);
    }

    /**
     * The two pairs whose harmonic contacts are definitions rather than findings.
     *
     * The node pair is excluded outright. Fortune and Spirit are not - their relationship is
     * real - but it is worth pinning what it is, because a reader seeing both a Sun/Moon and a
     * Fortune/Spirit entry in one list of five is seeing one fact twice.
     */
    private static void theArtefacts() {
        Random rnd = new Random(20260901L);
        int north = Bodies.indexOf("north_node");
        int south = Bodies.indexOf("south_node");
        int nodeHits = 0;
        int angleHits = 0;
        int charts = 0;

        for (int i = 0; i < 100; i++) {
            ChartFrame r = randomChart(rnd);
            if (r == null) {
                continue;
            }
            charts++;
            for (HarmonicResonance.Contact c : HarmonicResonance.all(
                    r, HarmonicResonance.DIAL_WITH_H360, 180.0, null)) {
                if ((c.indexA == north && c.indexB == south)
                        || (c.indexA == south && c.indexB == north)) {
                    nodeHits++;
                }
                if (Bodies.at(c.indexA).isAngle() || Bodies.at(c.indexB).isAngle()) {
                    angleHits++;
                }
            }
        }
        checks++;
        if (nodeHits != 0) {
            failures.add("North Node against South Node was reported " + nodeHits
                + " times over " + charts + " charts; it is exactly opposite by definition");
        }
        checks++;
        if (angleHits != 0) {
            failures.add("an angle appeared in " + angleHits + " contacts; angles are not "
                + "mapped by Harmonics.of, so an orb against one mixes H1 with HN");
        }
        System.out.println("  " + charts + " charts scanned at an unbounded orb: "
            + nodeHits + " node-pair contacts, " + angleHits + " angle contacts");

        // Spirit is Fortune with the sect reversed, so Spirit - Fortune = 2 * (Sun - Moon)
        // exactly. A Fortune/Spirit contact in HN is therefore the Sun/Moon relationship in
        // H2N restated - not an artefact to filter, but not an independent witness either.
        ChartFrame r = chart(1982, 8, 10, 19 + 1.0 / 60.0);
        double fortune = r.body("Part of Fortune").lon;
        double spirit = r.body("Part of Spirit").lon;
        double sun = r.body("Sun").lon;
        double moon = r.body("Moon").lon;
        for (int n : HarmonicResonance.DIAL) {
            near("H" + n + " Fortune/Spirit is H" + (2 * n) + " Sun/Moon",
                Aspects.separation(Harmonics.map(fortune, n), Harmonics.map(spirit, n)),
                Aspects.separation(Harmonics.map(sun, 2 * n), Harmonics.map(moon, 2 * n)),
                1e-6);
        }
    }

    /**
     * The fixed count is the promise the whole design rests on, so it is measured.
     *
     * <b>Two assertions, because the promise is only unconditional in one of them.</b> That
     * the scan returns min(K, what exists) is an invariant of the code and holds at any body
     * selection - it is what catches a padding bug, where a fifth slot gets filled with
     * something outside the ceiling to keep the list long.
     *
     * That all five slots actually fill is <b>not</b> unconditional, and finding that out is
     * what this part is worth. Measured by running the suite from a directory whose
     * `settings.properties` enables nine bodies: seven of them non-angle is 21 pairs, which
     * yields <b>0.9</b> new contacts per chart against 13.0 at the full registry's 300 pairs,
     * and 1 chart in 100 fills all five slots rather than 100. A narrow selection is a
     * legitimate setting, so asserting the fill rate against it would turn this suite red on a
     * user's preference. The guard therefore runs against the full registry and says out loud
     * when it did not - and the number it protects is the dial and the ceiling, not the user.
     */
    private static void fiveMeansFive() {
        Random rnd = new Random(19820810L);
        int charts = 0;
        int full = 0;
        int bodies = 0;
        long contacts = 0;
        Rule cast = new Rule("a sample chart failed to compute");
        Rule exactlyMin = new Rule("a scan did not return min(K, contacts inside the ceiling)");
        for (int i = 0; i < 100; i++) {
            ChartFrame r = randomChart(rnd);
            cast.test(r != null, "chart " + i);
            if (r == null) {
                continue;
            }
            charts++;
            bodies = countable(r);
            List<HarmonicResonance.Contact> five = HarmonicResonance.scan(r);
            int available = HarmonicResonance.all(r, HarmonicResonance.DIAL,
                HarmonicResonance.MAX_ORB, HarmonicResonance.classical()).size();
            contacts += available;
            if (five.size() == HarmonicResonance.K) {
                full++;
            }
            exactlyMin.test(
                five.size() == Math.min(HarmonicResonance.K, available), "chart " + i);
        }
        assertAll(cast, exactlyMin);

        double perChart = contacts / (double) Math.max(charts, 1);
        System.out.println("  " + bodies + " of the 12 classical bodies present: " + full
            + " of " + charts + " charts filled all " + HarmonicResonance.K + " slots, "
            + String.format("%.1f", Double.valueOf(perChart))
            + " new contacts per chart inside " + HarmonicResonance.MAX_ORB + " deg");

        // <b>Keyed on the scanned set, not the frame's.</b> scan(frame) uses classical(), so
        // the number that decides whether five slots fill is how many of those twelve the
        // user left enabled - not how many bodies the registry holds. Getting this wrong is
        // what made the guard fire on a nine-body selection while measuring nothing.
        checks++;
        if (bodies < HarmonicResonance.classicalCount()) {
            System.out.println("  the fill-rate guard did not run: it needs all "
                + HarmonicResonance.classicalCount() + " classical bodies and this run has "
                + bodies);
        } else if (full < charts * 95 / 100) {
            failures.add("only " + full + " of " + charts + " charts filled all five slots on "
                + "the classical set - the ceiling is binding, so the list length has become "
                + "a finding");
        }
    }

    /** How many of the scanned default set the frame actually supplies. */
    private static int countable(ChartFrame f) {
        boolean[] wanted = HarmonicResonance.classical();
        int n = 0;
        for (int i = 0; i < f.bodies.length && i < Bodies.count(); i++) {
            if (wanted[i] && f.bodies[i] != null && f.bodies[i].ok && !Bodies.at(i).isAngle()) {
                n++;
            }
        }
        return n;
    }

    // ---- helpers -------------------------------------------------------------------

    /** The harmonics on the default dial at which a pair this far apart is a new contact. */
    private static int[] harmonicsFor(double separation) {
        List<HarmonicResonance.Contact> found = scanPair(separation);
        int[] out = new int[found.size()];
        for (int i = 0; i < found.size(); i++) {
            out[i] = found.get(i).harmonic;
        }
        java.util.Arrays.sort(out);
        return out;
    }

    /**
     * Part A's orb, <b>fixed here rather than taken from {@code MAX_ORB}</b>. The cases below
     * are statements about the filter at a stated width - "0.7 degrees off a quintile is
     * outside a 1.5 degree orb" is only a test while 1.5 is the number. Reading the constant
     * would have quietly turned that assertion into its opposite when the ceiling moved to 6.
     */
    private static final double FILTER_ORB = 1.5;

    private static List<HarmonicResonance.Contact> scanPair(double separation) {
        return HarmonicResonance.all(pair(11.7, 11.7 + separation),
            HarmonicResonance.DIAL, FILTER_ORB, null);
    }

    /** A frame holding two bodies and nothing else, for asking one question at a time. */
    private static ChartFrame pair(double sunLon, double moonLon) {
        ChartFrame f = new ChartFrame();
        f.bodies[Bodies.indexOf("sun")] = body("Sun", sunLon);
        f.bodies[Bodies.indexOf("moon")] = body("Moon", moonLon);
        return f;
    }

    private static ChartFrame.Body body(String name, double lon) {
        ChartFrame.Body b = new ChartFrame.Body();
        b.name = name;
        b.ok = true;
        b.lon = Zodiac.normalise(lon);
        return b;
    }

    private static ChartFrame chart(int year, int month, int day, double hour) {
        SwissEph sw = new SwissEph(EPHE_PATH);
        return ChartFrame.compute(sw, new SweDate(year, month, day, hour).getJulDay(),
            40.45, -75.3333, 'P', false, 0.0);
    }

    private static ChartFrame randomChart(Random rnd) {
        return chart(1940 + rnd.nextInt(70), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28),
            rnd.nextDouble() * 24.0);
    }

    private static boolean onDial(int h) {
        for (int d : HarmonicResonance.DIAL) {
            if (d == h) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameList(List<HarmonicResonance.Contact> a,
                                    List<HarmonicResonance.Contact> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            HarmonicResonance.Contact x = a.get(i);
            HarmonicResonance.Contact y = b.get(i);
            if (x.harmonic != y.harmonic || x.indexA != y.indexA || x.indexB != y.indexB
                    || Math.abs(x.orb - y.orb) > 1e-12) {
                return false;
            }
        }
        return true;
    }

    /**
     * One named property, tested over as many contacts as a chart happens to yield but
     * counted exactly once.
     *
     * <b>This exists so the suite's check total cannot depend on `settings.properties`.</b>
     * {@code ChartFrame.compute} applies the user's body selection, so how many contacts come
     * back is a user setting; counting a check per contact would make the whole-suite number
     * in HANDOVER move when nobody changed any code. The first offender is kept so a failure
     * still names something concrete rather than only a count.
     */
    private static final class Rule {
        private final String label;
        private String offender;

        Rule(String label) {
            this.label = label;
        }

        void test(boolean holds, String subject) {
            if (!holds && offender == null) {
                offender = subject;
            }
        }
    }

    private static void assertAll(Rule... rules) {
        for (Rule rule : rules) {
            checks++;
            if (rule.offender != null) {
                failures.add(rule.label + " - first offender: " + rule.offender);
            }
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
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void eqInts(String label, int[] expected, int[] actual) {
        checks++;
        if (!java.util.Arrays.equals(expected, actual)) {
            failures.add(label + ": got " + java.util.Arrays.toString(actual)
                + ", expected " + java.util.Arrays.toString(expected));
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected
                + " (" + Math.abs(expected - actual) + " off, tolerance " + tol + ")");
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
