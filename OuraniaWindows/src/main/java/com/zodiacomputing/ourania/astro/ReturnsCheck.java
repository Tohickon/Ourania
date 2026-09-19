package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guards {@link Returns#planetary}: Mercury, Venus, Mars, Jupiter and Saturn coming back to
 * their natal degree.
 *
 * The feature shipped on 2026-08-21 with no suite behind it, which broke the invariant the
 * two-agent protocol leans on - every shipped feature has one. This is that suite.
 *
 * The central claim is small and checkable without any astrology: **at every moment this
 * reports, the body is on its natal longitude.** Everything else here is structure around
 * that - ordering, windowing, the definitional contact, and the one documented trap.
 *
 * Run:
 *   java -cp src\main\java com.zodiacomputing.ourania.astro.ReturnsCheck
 */
public final class ReturnsCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    private static final double LAT = 34.05;
    private static final double LON = -118.24;

    /** 1990-03-14, the chart the rest of the project measures against. */
    private static final double NATAL_JD = 2447964.60417;

    private static final double DAYS_PER_YEAR = 365.25;

    /**
     * Geocentric return periods in days - how long the body takes to come back to a fixed
     * ecliptic degree AS SEEN FROM EARTH.
     *
     * These are NOT the heliocentric sidereal periods, and the difference is the whole trap:
     * geocentric Mercury and Venus never leave the Sun's vicinity, so they cross a fixed
     * degree about once a year rather than every 88 or 225 days. Benchmarking against
     * heliocentric figures during development produced a confident "Mercury is 315% off"
     * against code that was entirely correct.
     */
    private static final Map<String, Double> GEOCENTRIC_PERIOD = new LinkedHashMap<>();
    static {
        GEOCENTRIC_PERIOD.put("Mercury", 365.25);
        GEOCENTRIC_PERIOD.put("Venus", 365.25);
        GEOCENTRIC_PERIOD.put("Mars", 686.98);
        GEOCENTRIC_PERIOD.put("Jupiter", 4332.59);
        GEOCENTRIC_PERIOD.put("Saturn", 10759.22);
    }

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private ReturnsCheck() { }

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame natal = ChartFrame.compute(sw, NATAL_JD, LAT, LON, 'P', false, 0.0);

        System.out.println("=== Part A: the returning body is on its natal degree ===");
        int before = failures.size();
        onNatalDegree(sw, natal);
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: ordering, windowing, and the cast chart ===");
        before = failures.size();
        structure(sw, natal);
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: cadence matches the body's geocentric period ===");
        before = failures.size();
        cadence(sw, natal);
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: the definitional contact is never reported ===");
        before = failures.size();
        definitional(sw, natal);
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: the birth-crossing trap stays documented ===");
        before = failures.size();
        birthCrossing(sw, natal);
        report("Part E", before);

        System.out.println();
        System.out.println("=== Part G: weight decides the order, not tightness ===");
        before = failures.size();
        ordering(sw, natal);
        report("Part G", before);

        System.out.println();
        System.out.println("=== Part F: each return is read to the depth its period earns ===");
        before = failures.size();
        scope(sw, natal);
        report("Part F", before);

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

    /**
     * The whole claim, asserted per moment: ask the ephemeris where the body actually was.
     *
     * Tolerance is a thousandth of a degree, which is far inside anything a reading would
     * notice and far outside the solver's precision - Almanac bisects to about a second of
     * time, so the residual measured here is around 1e-6 degrees.
     */
    private static void onNatalDegree(SwissEph sw, ChartFrame natal) {
        for (String body : Returns.returnableBodies()) {
            double natalLon = natal.body(body).lon;
            List<Returns.Return> rs = window(sw, natal, body, 40);
            yes("returns were found at all (" + body + ")", !rs.isEmpty());
            double worst = 0.0;
            for (Returns.Return r : rs) {
                double lon = Almanac.bodyLongitude(sw, r.jd, body);
                worst = Math.max(worst, Math.abs(Almanac.signedDelta(lon, natalLon)));
            }
            near("body is on its natal degree at every return (" + body + ")", 0.0, worst, 1e-3);
            System.out.printf("  %-8s %3d returns, worst error %.7f deg%n",
                body, rs.size(), worst);
        }
    }

    // ---------------------------------------------------------------- part B

    private static void structure(SwissEph sw, ChartFrame natal) {
        double from = NATAL_JD;
        double to = NATAL_JD + DAYS_PER_YEAR * 40;
        for (String body : Returns.returnableBodies()) {
            List<Returns.Return> rs = window(sw, natal, body, 40);
            double prev = Double.NEGATIVE_INFINITY;
            int ordinal = 0;
            for (Returns.Return r : rs) {
                yes("returns are chronological (" + body + ")", r.jd >= prev);
                prev = r.jd;
                yes("return is inside the window (" + body + ")",
                    r.jd >= from - 1e-6 && r.jd <= to + 1e-6);
                eq("kind names the body (" + body + ")", body.toLowerCase(), r.kind);
                eq("body field is set (" + body + ")", body, r.body);
                eq("ordinal counts up (" + body + ")", ordinal++, r.ordinal);
                yes("a chart was cast (" + body + ")", r.chart != null);
                if (r.chart != null) {
                    yes("the cast chart carries the body (" + body + ")",
                        r.chart.body(body) != null && r.chart.body(body).ok);
                }
            }
        }
        // A body with no return definition must come back empty rather than guess.
        yes("the Sun is not a planetary return", !Returns.canReturn("Sun"));
        yes("the Moon is not a planetary return", !Returns.canReturn("Moon"));
        yes("an unknown body is not returnable", !Returns.canReturn("Nessus"));
        eq("an unreturnable body yields no returns", 0,
            Returns.planetary(sw, "Sun", 0.0, NATAL_JD, NATAL_JD + 400, LAT, LON, 'P').size());
    }

    // ---------------------------------------------------------------- part C

    /**
     * Successive passages should fall about one geocentric period apart.
     *
     * Crossings are collapsed into passages first, because a retrograde loop puts the body
     * over the same degree three times within weeks and those are one arrival, not three.
     * The collapse window is a third of the body's period, which is far wider than any
     * retrograde loop and far narrower than the gap to the next passage.
     */
    private static void cadence(SwissEph sw, ChartFrame natal) {
        for (String body : Returns.returnableBodies()) {
            double period = GEOCENTRIC_PERIOD.get(body);
            int years = (int) Math.ceil(period * 4.0 / DAYS_PER_YEAR) + 2;
            List<Returns.Return> rs = window(sw, natal, body, years);

            List<Double> passes = new ArrayList<>();
            for (Returns.Return r : rs) {
                if (passes.isEmpty() || r.jd - passes.get(passes.size() - 1) > period / 3.0) {
                    passes.add(r.jd);
                }
            }
            yes("at least two passages to measure (" + body + ")", passes.size() >= 2);
            if (passes.size() < 2) {
                continue;
            }
            double mean = (passes.get(passes.size() - 1) - passes.get(0)) / (passes.size() - 1);
            double errPct = 100.0 * Math.abs(mean - period) / period;
            System.out.printf("  %-8s %2d passages, mean gap %8.1f d, expected %8.1f d (%.1f%%)%n",
                body, passes.size(), mean, period, errPct);
            yes("passage cadence matches the geocentric period (" + body + ")", errPct < 8.0);

            // Completeness, not just correctness. Part A asks whether the moments reported
            // are right; nothing there can notice a moment that was never reported at all.
            // A scan step too coarse for the body silently drops roots, and the signature
            // is a gap of two periods where there should be one. Found by mutation testing:
            // widening Saturn's step from 4 days to 400 left Parts A to D entirely clear.
            double widest = 0.0;
            for (int i = 1; i < passes.size(); i++) {
                widest = Math.max(widest, passes.get(i) - passes.get(i - 1));
            }
            yes("no passage was skipped (" + body + ")", widest < period * 1.5);
        }
    }

    // ---------------------------------------------------------------- part D

    /**
     * A Venus return has Venus on natal Venus by construction. Reporting it would put one
     * guaranteed, contentless line in every return chart - and the test that suppresses it
     * used to switch on `kind` and fall through to "Moon", so it would have let every
     * planetary return through.
     */
    private static void definitional(SwissEph sw, ChartFrame natal) {
        Gestalt.Result g = Gestalt.compute(natal);
        List<BodyScore.Vector> ranked = BodyScore.rank(natal, g);
        for (String body : Returns.returnableBodies()) {
            List<Returns.Return> rs = window(sw, natal, body, 3);
            int total = 0;
            int self = 0;
            for (Returns.Return r : rs) {
                for (Returns.Contact c : Returns.contacts(r, natal, ranked, null)) {
                    total++;
                    if (body.equals(c.returnPoint) && body.equals(c.natal)) {
                        self++;
                    }
                    yes("contact respects the orb ceiling (" + body + ")",
                        c.offBy <= Transits.eventMaxOrb + 1e-9);
                    eq("contact carries the return moment (" + body + ")", r.jd, c.jd);
                }
            }
            eq("the definitional contact is suppressed (" + body + ")", 0, self);
            System.out.printf("  %-8s %3d contacts, %d definitional%n", body, total, self);
        }
    }

    // ---------------------------------------------------------------- part E

    /**
     * Pins the trap the javadoc warns about, so it cannot change silently.
     *
     * A window starting at the natal moment reports the birth itself, because the body is on
     * its natal degree then by definition. On this chart Saturn also retrogrades back over
     * that degree twice within the first year. Those three crossings are correct and are not
     * returns, and any caller passing natalJd as the window start will meet them.
     */
    private static void birthCrossing(SwissEph sw, ChartFrame natal) {
        List<Returns.Return> sat = window(sw, natal, "Saturn", 40);
        yes("Saturn crossings were found", !sat.isEmpty());
        if (sat.isEmpty()) {
            return;
        }
        double firstYears = (sat.get(0).jd - NATAL_JD) / DAYS_PER_YEAR;
        yes("the first Saturn crossing is the birth passage, not the return",
            firstYears < 1.0);
        double lastYears = (sat.get(sat.size() - 1).jd - NATAL_JD) / DAYS_PER_YEAR;
        near("the real Saturn return lands near its period", 29.46, lastYears, 1.0);
        System.out.printf("  first crossing +%.2f yr (birth passage), last +%.2f yr (the return)%n",
            firstYears, lastYears);
    }

    // ---------------------------------------------------------------- helpers

    private static List<Returns.Return> window(SwissEph sw, ChartFrame natal,
                                               String body, int years) {
        return Returns.planetary(sw, body, natal.body(body).lon,
            NATAL_JD, NATAL_JD + DAYS_PER_YEAR * years, LAT, LON, 'P');
    }

    // ---------------------------------------------------------------- part G

    /**
     * Contacts come back ordered by what they are worth, not by how tight they are.
     *
     * <b>Sorting on orb alone let the minor bodies lead a return.</b> On the app's default
     * chart the solar return opened with Pholus exactly on the Sun, Chiron on the Ascendant
     * and Eris on the Moon - all at a tenth of a degree, all above Mars conjunct the Sun two
     * degrees off. Tightness is a measurement and the sort was reading it as importance.
     * These assertions hold the K8 hierarchy in place on this surface too, so the two cannot
     * be tuned apart.
     */
    private static void ordering(SwissEph sw, ChartFrame natal) {
        Gestalt.Result g = Gestalt.compute(natal);
        List<BodyScore.Vector> ranked = BodyScore.rank(natal, g);
        int compared = 0;
        int minorFirst = 0;
        int lists = 0;
        for (String body : Returns.returnableBodies()) {
            for (Returns.Return r : window(sw, natal, body, 3)) {
                List<Returns.Contact> cs = Returns.contacts(r, natal, ranked, null);
                if (cs.isEmpty()) {
                    continue;
                }
                lists++;
                for (Returns.Contact c : cs) {
                    yes("every contact carries a weight (" + body + ")",
                        c.weight > 0.0 && !Double.isNaN(c.weight));
                }
                for (int i = 1; i < cs.size(); i++) {
                    compared++;
                    yes("the list is ordered by weight (" + body + ")",
                        cs.get(i - 1).weight >= cs.get(i).weight - 1e-9);
                }
                // The leading contact must not be a minor body arriving while a planetary
                // one waits below it. This is the defect stated as an assertion.
                Returns.Contact top = cs.get(0);
                if (Bodies.isMinor(top.returnPoint)) {
                    boolean planetaryBelow = false;
                    for (Returns.Contact c : cs) {
                        if (!Bodies.isMinor(c.returnPoint)) {
                            planetaryBelow = true;
                            break;
                        }
                    }
                    if (planetaryBelow) {
                        minorFirst++;
                    }
                }
            }
        }
        eq("no return is led by a minor body while a planetary contact sits below it",
            0, minorFirst);
        yes("the ordering was actually exercised", compared > 0 && lists > 0);

        // <b>A conjunction's maxOrb is effectively unbounded</b>, so precision has to come
        // off the orb actually allowed. Dividing by maxOrb would make every conjunction score
        // as though it were exact and put this whole ordering back where it started. Asserted
        // by finding two conjunctions of the same pairing at different orbs and requiring the
        // tighter one to weigh more - a tautological check here would hide the exact bug the
        // rest of this part exists to catch.
        Returns.Contact loose = null;
        Returns.Contact tight = null;
        for (String body : Returns.returnableBodies()) {
            for (Returns.Return r : window(sw, natal, body, 3)) {
                for (Returns.Contact c : Returns.contacts(r, natal, ranked, null)) {
                    if (c.type != Aspects.Type.CONJUNCTION) {
                        continue;
                    }
                    if (tight == null || c.offBy < tight.offBy) {
                        tight = c;
                    }
                    if (loose == null || c.offBy > loose.offBy) {
                        loose = c;
                    }
                }
            }
        }
        if (tight != null && loose != null && loose.offBy - tight.offBy > 0.5
                && Convergence.bodyWeight(tight.returnPoint) == Convergence.bodyWeight(loose.returnPoint)
                && Convergence.bodyWeight(tight.natal) == Convergence.bodyWeight(loose.natal)) {
            yes("a tighter conjunction outweighs a looser one of equal standing",
                tight.weight > loose.weight);
        } else {
            yes("the orb term is live rather than constant across conjunctions",
                tight != null && loose != null && (loose.offBy - tight.offBy < 1e-9
                    || Math.abs(tight.weight - loose.weight) > 1e-9));
        }
        System.out.printf("  %d lists, %d adjacent pairs compared, %d led by a minor body%n",
            lists, compared, minorFirst);
    }

    // ---------------------------------------------------------------- part F

    /**
     * The K1 scope rule, pinned so it cannot be undone silently.
     *
     * <b>Without this the rule is invisible to the suite.</b> Narrowing Mercury, Venus and
     * Mars to their angles took Part D from 1,431 checks to 1,033 - the suite kept passing
     * and simply verified less, which is the shape of defect this project keeps finding:
     * a total that drifts with a decision rather than with the code's correctness. Reverting
     * scopeOf would restore the 398 checks and still be green. So the rule is asserted
     * directly: an angles-only return contributes angles and nothing else, and a full-wheel
     * return is not quietly reduced to the same thing.
     */
    private static void scope(SwissEph sw, ChartFrame natal) {
        Gestalt.Result g = Gestalt.compute(natal);
        List<BodyScore.Vector> ranked = BodyScore.rank(natal, g);

        eq("the Sun's return is a whole chart", Returns.Scope.FULL_WHEEL,
            Returns.scopeOf("Sun"));
        eq("the Moon's return is a whole chart", Returns.Scope.FULL_WHEEL,
            Returns.scopeOf("Moon"));
        for (String body : new String[]{"Mercury", "Venus", "Mars"}) {
            eq("the annual three are angles only (" + body + ")", Returns.Scope.ANGLES_ONLY,
                Returns.scopeOf(body));
        }
        for (String body : new String[]{"Jupiter", "Saturn"}) {
            eq("the slow returns keep their wheel (" + body + ")", Returns.Scope.FULL_WHEEL,
                Returns.scopeOf(body));
        }
        yes("an unknown body defaults to a whole chart rather than to silence",
            Returns.scopeOf("Nessus") == Returns.Scope.FULL_WHEEL);
        yes("a null body does not throw", Returns.scopeOf(null) != null);

        // The rule as the reader meets it, not just as the enum reports it.
        int anglesOnlyContacts = 0;
        int fullWheelNonAngle = 0;
        for (String body : Returns.returnableBodies()) {
            boolean anglesOnly = Returns.scopeOf(body) == Returns.Scope.ANGLES_ONLY;
            for (Returns.Return r : window(sw, natal, body, 3)) {
                for (Returns.Contact c : Returns.contacts(r, natal, ranked, null)) {
                    boolean isAngle = "Ascendant".equals(c.returnPoint)
                        || "MC".equals(c.returnPoint);
                    if (anglesOnly) {
                        anglesOnlyContacts++;
                        yes("an angles-only return reports only angles (" + body + " gave "
                            + c.returnPoint + ")", isAngle);
                    } else if (!isAngle) {
                        fullWheelNonAngle++;
                    }
                }
            }
        }
        yes("the angles-only rule was actually exercised", anglesOnlyContacts > 0);
        yes("a full-wheel return still reports its bodies", fullWheelNonAngle > 0);
        System.out.printf("  angles-only contacts %d, full-wheel non-angle contacts %d%n",
            anglesOnlyContacts, fullWheelNonAngle);
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null) {
            if (actual != null) {
                failures.add(label + ": got " + actual + ", expected null");
            }
        } else if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected + " +/- " + tol);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "clear" : added + " FAILURE(S)"));
    }
}
