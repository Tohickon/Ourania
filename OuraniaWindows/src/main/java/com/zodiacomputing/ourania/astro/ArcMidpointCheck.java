package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * L8: that solar arc midpoint contacts are the contacts they claim to be.
 *
 * <p><b>Part A exists because the spec was wrong and the error was silent.</b> The spec says to
 * reduce modulo 90, on the grounds that all five kinetic aspects then coincide. They do not: 0,
 * 90 and 180 fold to 0 while 45 and 135 fold to 45, so a scan stepping the arc by 90 finds every
 * conjunction, square and opposition and misses every semisquare and sesquiquadrate. Nothing
 * would have reported it, because everything it did find was real. The first draft of
 * {@link ArcMidpoints} had exactly that bug and Part A is what would have caught it.
 *
 * <p><b>Part F is the one that matters most: it looks for contacts the scanner should have
 * found.</b> Every other part checks that what came back is correct, which a scanner that returns
 * half the answer passes easily. So Part F walks the window day by day with no knowledge of the
 * congruence at all - just "how far apart are these two on the dial today" - and asserts the set
 * it finds is the set the scanner returned. That is the test the mod-90 bug fails.
 *
 * <p>The checks that can be stated without the ephemeris are stated without it: the fold
 * arithmetic, the decay weight and the point set are pure, and a suite that needed a chart cast
 * to assert that 135 degrees folds onto 45 would be measuring the wrong thing.
 */
public final class ArcMidpointCheck {

    private ArcMidpointCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** A birth chart to direct. David's own, which the rest of the suites use. */
    private static final int Y = 1982;
    private static final int MO = 8;
    private static final int D = 10;
    private static final double HOUR = 7.5;
    private static final double LAT = 39.95;
    private static final double LON = -75.16;

    public static void main(String[] args) throws Exception {
        // <b>SettingsIsolationCheck caught the absence of this line.</b> Without it a suite
        // reads and writes the reader's own settings file, so running the checks would change
        // the app's state - and there is a check whose whole job is to assert every suite
        // calls it. It failed on this file by name on the first full run.
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        part("A: the eighth harmonic, which is 45 and not 90", ArcMidpointCheck::harmonic);
        part("B: the decay weight, and what the orb is in days", ArcMidpointCheck::decay);
        part("C: the dial's points are Ebertin's thirteen", ArcMidpointCheck::pointSet);

        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double natalJd = new SweDate(Y, MO, D, HOUR).getJulDay();
        ChartFrame natal = ChartFrame.compute(sw, natalJd, LAT, LON, (char) 80, false, 0.0);
        double from = new SweDate(2026, 1, 1, 0.0).getJulDay();
        double to = new SweDate(2028, 1, 1, 0.0).getJulDay();
        List<ArcMidpoints.Contact> found = ArcMidpoints.contacts(sw, natal, natalJd, from, to);

        part("D: every contact returned is exact on the dial",
            () -> exactness(sw, natal, natalJd, found));
        part("E: each one carries the window it is active in",
            () -> windows(sw, natalJd, natal, found));
        part("F: and no kinetic contact in the window was missed",
            () -> completeness(sw, natal, natalJd, from, to, found));

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

    // ------------------------------------------------------------------ Part A

    /**
     * Where each aspect lands on each dial.
     *
     * <p>Stated as the fold of the aspect angle itself rather than of two longitudes, because
     * that is the claim: the kinetic family is the multiples of 45, and 45 is the modulus in
     * which they are one contact. The 90 dial is kept for reading - a practitioner watches its
     * two marks - and this says which mark each aspect stands on.
     */
    private static void harmonic() {
        double[] kinetic = {0.0, 45.0, 90.0, 135.0, 180.0};
        double[] onNinety = {0.0, 45.0, 0.0, 45.0, 0.0};
        for (int i = 0; i < kinetic.length; i++) {
            near("on a 90 dial " + fmt(kinetic[i]) + " stands at " + fmt(onNinety[i]),
                onNinety[i], Dial.separation(kinetic[i], 0.0, ArcMidpoints.DIAL), 1e-9);
        }
        // <b>Two marks, not one.</b> This is the sentence the spec got wrong.
        ok("so the kinetic family occupies TWO places on a 90 dial, not one",
            Dial.separation(45.0, 0.0, ArcMidpoints.DIAL)
                != Dial.separation(90.0, 0.0, ArcMidpoints.DIAL));

        for (double a : kinetic) {
            near("in the eighth harmonic " + fmt(a) + " folds onto the one contact",
                0.0, Dial.separation(a, 0.0, ArcMidpoints.HARMONIC), 1e-9);
        }
        ok("and the eighth harmonic is 360/8", Math.abs(360.0 / 8.0 - ArcMidpoints.HARMONIC) < 1e-9);

        // The soft aspects need no filter because they are nowhere near either mark.
        for (double soft : new double[]{30.0, 60.0, 120.0, 150.0}) {
            double h = Dial.separation(soft, 0.0, ArcMidpoints.HARMONIC);
            ok("a " + fmt(soft) + " aspect is not a contact in the eighth harmonic ("
                + String.format("%.1f", h) + " off)", h > 1.0);
        }
    }

    // ------------------------------------------------------------------ Part B

    /** The weight curve, and the spec's "six weeks" against the arc's real rate. */
    private static void decay() {
        near("exact is full strength", 1.0, ArcMidpoints.weight(0.0), 1e-12);
        near("the orb is zero strength", 0.0, ArcMidpoints.weight(ArcMidpoints.ORB), 1e-12);
        near("and beyond it stays zero", 0.0, ArcMidpoints.weight(ArcMidpoints.ORB * 3), 1e-12);
        near("the curve is symmetric", ArcMidpoints.weight(0.1),
            ArcMidpoints.weight(-0.1), 1e-12);
        near("the peak zone is near full strength", 0.96,
            ArcMidpoints.weight(ArcMidpoints.PEAK), 1e-9);

        // Quadratic, not linear: at half the orb a linear ramp would give 0.5.
        double half = ArcMidpoints.weight(ArcMidpoints.ORB / 2.0);
        near("half way out is 0.75, so the fall-off is quadratic", 0.75, half, 1e-12);

        boolean falls = true;
        double last = Double.MAX_VALUE;
        for (double d = 0.0; d <= ArcMidpoints.ORB; d += ArcMidpoints.ORB / 50.0) {
            double w = ArcMidpoints.weight(d);
            falls &= w <= last + 1e-12;
            last = w;
        }
        ok("and never rises as the offset grows", falls);

        // <b>The spec's duration, checked against the arc rate rather than restated.</b> The
        // arc advances at the progressed Sun's rate, so a span of arc is a span of years.
        double yearsPerDegree = 1.0 / 0.9856;         // the Sun's mean daily motion
        double orbDays = 2 * ArcMidpoints.ORB * yearsPerDegree * SolarArc.DAYS_PER_YEAR;
        double peakDays = 2 * ArcMidpoints.PEAK * yearsPerDegree * SolarArc.DAYS_PER_YEAR;
        ok("the peak zone is about six weeks (" + Math.round(peakDays) + " days)",
            peakDays > 30 && peakDays < 45);
        ok("the orb is about six MONTHS (" + Math.round(orbDays) + " days), not six weeks",
            orbDays > 170 && orbDays < 200);
        ok("so the spec's \"six weeks\" names the peak zone and not the orb",
            orbDays / peakDays > 4.0);
    }

    // ------------------------------------------------------------------ Part C

    /**
     * The point set, and the arithmetic that chose it.
     *
     * <p>Every point added grows the midpoint matrix quadratically, so this is pinned rather
     * than left to whoever next adds a body to the registry: a new asteroid must not silently
     * double this layer's contact rate.
     */
    private static void pointSet() throws Exception {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double natalJd = new SweDate(Y, MO, D, HOUR).getJulDay();
        ChartFrame natal = ChartFrame.compute(sw, natalJd, LAT, LON, (char) 80, false, 0.0);

        List<Dial.Point> pts = ArcMidpoints.dialPoints(natal);
        Set<String> names = new LinkedHashSet<>();
        for (Dial.Point p : pts) {
            names.add(p.name);
        }
        eq("thirteen points: two luminaries, eight planets, the Node and two angles", 13,
            pts.size());
        for (String want : new String[]{"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter",
            "Saturn", "Uranus", "Neptune", "Pluto", "North Node", "Ascendant", "MC"}) {
            ok(want + " is on the dial", names.contains(want));
        }
        // The derived half of each axis sits on its partner on every one of these dials.
        for (String no : new String[]{"Descendant", "IC", "South Node"}) {
            ok(no + " is not, because it stands where its partner stands", !names.contains(no));
        }
        // And the bodies Ebertin's dial leaves out.
        for (String no : new String[]{"Ceres", "Chiron", "Vesta", "Part of Fortune", "Vertex"}) {
            ok(no + " is not on this layer's dial", !names.contains(no));
        }
        eq("so the midpoint matrix is 13 choose 2", 78, Dial.midpoints(pts).size());
    }

    // ------------------------------------------------------------------ Part D

    /**
     * Each contact, recomputed from its own date.
     *
     * <p>Asked of {@link Dial#separation}, which this layer did not write, rather than of the
     * congruence that produced the date - a scanner checked against its own arithmetic proves
     * only that it is consistent.
     */
    private static void exactness(SwissEph sw, ChartFrame natal, double natalJd,
                                  List<ArcMidpoints.Contact> found) {
        ok("the window has contacts to check at all (" + found.size() + ")", !found.isEmpty());
        double natalSunLon = natal.body("Sun").lon;
        Map<String, Double> lons = natalLons(natal);
        double worstFold = 0.0;
        double worstKinetic = 0.0;
        double worstMid = 0.0;
        int checked = 0;
        for (ArcMidpoints.Contact c : found) {
            Double natalLon = lons.get(c.directed);
            Double la = lons.get(c.a);
            Double lb = lons.get(c.b);
            if (natalLon == null || la == null || lb == null) {
                continue;
            }
            checked++;
            // <b>The body's own natal longitude, looked up by name.</b> The first cut of this
            // recovered it as (midpoint - arc), which is where the scanner PUT it: the arc was
            // chosen to make that difference the midpoint, so the separation came out zero by
            // construction and the assertion could not fail. Part F's aspect tally is what
            // showed it - 38 contacts and not one semisquare, which cannot happen if the
            // family is evenly spread modulo 45.
            double arc = SolarArc.arcAt(sw, natalJd, natalSunLon, c.jd);
            double directed = Zodiac.normalise(natalLon + arc);
            worstFold = Math.max(worstFold,
                Dial.separation(directed, c.midpoint, ArcMidpoints.HARMONIC));

            // And the plain angular separation is a multiple of 45, said without the dial.
            double sep = Zodiac.normalise(directed - c.midpoint);
            double off = Math.abs(sep / 45.0 - Math.round(sep / 45.0)) * 45.0;
            worstKinetic = Math.max(worstKinetic, off);

            // The midpoint it reports is the midpoint of the two it names, recomputed by
            // Dial rather than taken on trust.
            double mid = Dial.midpoint(Dial.point(c.a, la), Dial.point(c.b, lb)).lon;
            double midOff = Zodiac.normalise(mid - c.midpoint);
            worstMid = Math.max(worstMid, Math.min(midOff, 360.0 - midOff));
        }
        eq("every contact's bodies were found in the natal chart", found.size(), checked);
        ok("and the midpoint it reports is the midpoint of the pair it names (worst "
            + String.format("%.9f", worstMid) + ")", worstMid < 1e-6);
        ok("every contact is exact on the dial (worst " + String.format("%.6f", worstFold)
            + " degrees)", worstFold < 1e-4);
        ok("and every one is a multiple of 45 degrees from its midpoint (worst "
            + String.format("%.6f", worstKinetic) + ")", worstKinetic < 1e-4);
        boolean dated = true;
        for (ArcMidpoints.Contact c : found) {
            dated &= c.a != null && c.b != null && !c.a.isEmpty() && !c.b.isEmpty();
        }
        ok("and names both halves of the midpoint it reached", dated);
        ok("and writes the classical notation",
            found.isEmpty() || found.get(0).picture().startsWith("SA "));
    }

    // ------------------------------------------------------------------ Part E

    /** The active window around each perfection. */
    private static void windows(SwissEph sw, double natalJd, ChartFrame natal,
                                List<ArcMidpoints.Contact> found) {
        double natalSunLon = natal.body("Sun").lon;
        boolean ordered = true;
        boolean bracketed = true;
        double worstEdge = 0.0;
        int edged = 0;
        for (int i = 1; i < found.size(); i++) {
            ordered &= found.get(i - 1).jd <= found.get(i).jd;
        }
        for (ArcMidpoints.Contact c : found) {
            if (!Double.isNaN(c.jdFrom) && !Double.isNaN(c.jdTo)) {
                edged++;
                bracketed &= c.jdFrom < c.jd && c.jd < c.jdTo;
                double aFrom = SolarArc.arcAt(sw, natalJd, natalSunLon, c.jdFrom);
                double aTo = SolarArc.arcAt(sw, natalJd, natalSunLon, c.jdTo);
                worstEdge = Math.max(worstEdge,
                    Math.max(Math.abs((c.arc - aFrom) - ArcMidpoints.ORB),
                             Math.abs((aTo - c.arc) - ArcMidpoints.ORB)));
            }
        }
        ok("contacts come back in date order", ordered);
        ok("most carry both edges of the orb (" + edged + " of " + found.size() + ")",
            edged > found.size() / 2);
        ok("and exactness falls inside its own window", bracketed);
        ok("each edge is one orb of arc from exactness (worst "
            + String.format("%.6f", worstEdge) + " degrees)", worstEdge < 1e-3);
    }

    // ------------------------------------------------------------------ Part F

    /**
     * The completeness check: contacts found by walking, not by solving.
     *
     * <p><b>This is the part the mod-90 bug fails.</b> It knows nothing about the congruence. It
     * steps the window a day at a time, computes every directed position and every natal
     * midpoint, and records any pair whose dial separation comes within a whisker of zero. The
     * arc moves about 0.0027 degrees a day, so the nearest day to a perfection is within 0.0014
     * degrees of it and the threshold below cannot miss one.
     *
     * <p>Then it compares sets, not counts: a scanner returning the right number of the wrong
     * contacts would pass on counts.
     */
    private static void completeness(SwissEph sw, ChartFrame natal, double natalJd,
                                     double from, double to, List<ArcMidpoints.Contact> found) {
        List<Dial.Point> pts = ArcMidpoints.dialPoints(natal);
        List<Dial.Point> mids = Dial.midpoints(pts);
        double natalSunLon = natal.body("Sun").lon;

        Set<String> walked = new LinkedHashSet<>();
        for (double jd = from; jd <= to; jd += 1.0) {
            double arc = SolarArc.arcAt(sw, natalJd, natalSunLon, jd);
            if (Double.isNaN(arc)) {
                continue;
            }
            for (Dial.Point d : pts) {
                double directed = Zodiac.normalise(d.lon + arc);
                for (Dial.Point m : mids) {
                    if (Dial.separation(directed, m.lon, ArcMidpoints.HARMONIC) < 0.002) {
                        walked.add(d.name + "=" + m.a + "/" + m.b);
                    }
                }
            }
        }

        Set<String> solved = new LinkedHashSet<>();
        for (ArcMidpoints.Contact c : found) {
            solved.add(c.directed + "=" + c.a + "/" + c.b);
        }

        ok("walking the window finds contacts at all (" + walked.size() + ")", !walked.isEmpty());

        Set<String> missed = new LinkedHashSet<>(walked);
        missed.removeAll(solved);
        Set<String> spurious = new LinkedHashSet<>(solved);
        spurious.removeAll(walked);

        ok("the scanner missed none of them" + (missed.isEmpty() ? ""
            : " - MISSED " + missed.size() + ": " + first(missed)), missed.isEmpty());
        ok("and invented none" + (spurious.isEmpty() ? ""
            : " - EXTRA " + spurious.size() + ": " + first(spurious)), spurious.isEmpty());

        // <b>And the family is represented.</b> A scan that found only conjunctions and squares
        // would agree with a walk that also only looked for them, so the aspect each contact
        // turned out to be is counted here and the 45s must be among them.
        int[] byMultiple = new int[8];
        Map<String, Double> lons = natalLons(natal);
        for (ArcMidpoints.Contact c : found) {
            Double natalLon = lons.get(c.directed);
            if (natalLon == null) {
                continue;
            }
            double arcAt = SolarArc.arcAt(sw, natalJd, natalSunLon, c.jd);
            double sep = Zodiac.normalise(Zodiac.normalise(natalLon + arcAt) - c.midpoint);
            byMultiple[((int) Math.round(sep / 45.0)) % 8]++;
        }
        int odd = byMultiple[1] + byMultiple[3] + byMultiple[5] + byMultiple[7];
        int even = byMultiple[0] + byMultiple[2] + byMultiple[4] + byMultiple[6];
        ok("conjunctions, squares and oppositions are found (" + even + ")", even > 0);
        ok("and so are semisquares and sesquiquadrates (" + odd
            + ") - the half a 90 degree fold would have lost", odd > 0);
    }

    // ------------------------------------------------------------------ plumbing

    /** Every dial point's natal longitude, by name. */
    private static Map<String, Double> natalLons(ChartFrame natal) {
        Map<String, Double> out = new HashMap<>();
        for (Dial.Point p : ArcMidpoints.dialPoints(natal)) {
            out.put(p.name, p.lon);
        }
        return out;
    }

    private static String first(Set<String> s) {
        for (String x : s) {
            return x;
        }
        return "";
    }

    private static String fmt(double d) {
        return String.format("%.0f°", d);
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println();
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + name.charAt(0) + ": "
            + (failures.size() == before ? "PASS" : "FAIL"));
    }

    private static void ok(String what, boolean passed) {
        checks++;
        if (passed) {
            System.out.println("  ok   " + what);
        } else {
            System.out.println("  FAIL " + what);
            failures.add(what);
        }
    }

    private static void eq(String what, Object want, Object got) {
        ok(what + " (" + got + ")", want.equals(got));
    }

    private static void near(String what, double want, double got, double tol) {
        ok(what + " (" + String.format("%.6f", got) + ")", Math.abs(want - got) <= tol);
    }
}
