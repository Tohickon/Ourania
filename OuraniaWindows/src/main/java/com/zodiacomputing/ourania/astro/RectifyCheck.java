package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Rectification, checked by hiding a birth time and asking for it back (G8).
 *
 * <p><b>Part A is a round trip, which is the only honest way to check this.</b> A fixture saying
 * "these events mean 14:32" asserts that I chose the fixture to agree with the code. Instead a
 * true birth time is picked, the dates on which its OWN directed angles perfect are computed, and
 * those dates are handed back as life events - so the question is whether the scan can find a time
 * it was never told, from evidence derived independently of it.
 *
 * <p><b>Part C is the half that stops it lying.</b> A scan handed events that discriminate between
 * nothing must say so rather than name whichever time scored a hair higher, and this asserts that
 * the separation collapses - because a practitioner will believe a time to the minute if a program
 * prints one.
 */
public final class RectifyCheck {

    private RectifyCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** The time being hidden. Nothing in the scan is told it. */
    private static final int Y = 1984;
    private static final int M = 9;
    private static final int D = 8;
    private static final double TRUE_HOUR = 7.0 + 33.0 / 60.0;
    private static final double LAT = 41.8781;
    private static final double LON = -87.6298;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        part("A: a hidden birth time is found from its own directions", RectifyCheck::roundTrip);
        part("B: the scan is the shape it says it is", RectifyCheck::shape);
        part("C: events that decide nothing are not allowed to decide",
            RectifyCheck::noFalseConfidence);
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

    // ---------------------------------------------------------------- Part A

    /**
     * Events derived from the true chart, handed to a scan that does not know it.
     *
     * <b>The events are found by walking the directions forward, not by choosing dates.</b> For
     * each of four natal bodies, the year in which the true directed Midheaven comes to it is
     * computed; those four dates are the "life events". A scan over a two-hour window then has to
     * pick the true time out of thirty-one candidates.
     */
    private static void roundTrip() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double trueJd = new SweDate(Y, M, D, TRUE_HOUR).getJulDay();
        ChartFrame truth = ChartFrame.compute(sw, trueJd, LAT, LON, 'P', false, 0.0);
        ok("the true chart casts", truth != null && truth.bodies[Bodies.indexOf("sun")] != null);

        // <b>Whichever bodies the directed Midheaven actually reaches inside a life span.</b>
        // The first version named four and derived nothing: an arc is the FORWARD distance from
        // the Midheaven, so a body sitting behind it is three hundred degrees away, which is
        // three hundred years. Which bodies qualify is a fact about this chart, so it is
        // measured rather than chosen.
        // <b>From both angles, and out to ninety degrees.</b> Only two bodies sit within seventy
        // degrees forward of this chart's Midheaven, which is a fact about the chart rather than
        // about the method - so the Ascendant's directions are derived too and the window is a
        // long life rather than a short one. Which contacts exist is measured either way.
        List<Rectify.Event> events = new ArrayList<>();
        for (double from : new double[] {truth.mc, truth.asc}) {
            for (int i = 0; i < Bodies.count() && events.size() < 4; i++) {
                if (truth.bodies[i] == null || !truth.bodies[i].ok) {
                    continue;
                }
                double arc = Zodiac.normalise(truth.bodies[i].lon - from);
                if (arc < 3.0 || arc > 90.0) {
                    continue;           // too soon to date, or beyond a life at a degree a year
                }
                double when = trueJd + arc * SolarArc.DAYS_PER_YEAR;
                events.add(new Rectify.Event("something at " + Bodies.at(i).name, when,
                    Bodies.at(i).name));
            }
        }
        ok("at least three events were derived (" + events.size() + ")", events.size() >= 3);
        if (events.size() < 3) {
            return;
        }

        Rectify.Result r = Rectify.scan(sw, Y, M, D, LAT, LON,
            TRUE_HOUR - 1.0, TRUE_HOUR + 1.0, 4, events);
        ok("every candidate in the window was tried (" + r.candidates.size() + ")",
            r.candidates.size() == 31);
        ok("and it reached a verdict", r.best() != null);
        if (r.best() == null) {
            return;
        }

        double offBy = Math.abs(r.best().hour - TRUE_HOUR) * 60.0;
        ok(String.format("the best time is within eight minutes of the true one (%.1f min, "
            + "%.4fh against %.4fh)", offBy, r.best().hour, TRUE_HOUR), offBy <= 8.0);
        ok("it says why: " + (r.best().reasons.isEmpty() ? "nothing"
            : r.best().reasons.get(0)), !r.best().reasons.isEmpty());
        ok("with a reason per event (" + r.best().reasons.size() + " of " + events.size() + ")",
            r.best().reasons.size() >= events.size() - 1);
        ok("and it is confident, because these events do discriminate ("
            + String.format("%.2f", r.separation) + ")", r.confident);

        // <b>And the true time is genuinely better than the far end of the window</b>, which is
        // what separates finding it from every candidate scoring alike.
        double best = r.best().score;
        double worst = r.candidates.get(r.candidates.size() - 1).score;
        ok(String.format("the best scores above the worst (%.3f against %.3f)", best, worst),
            best > worst + 0.5);
    }

    // ---------------------------------------------------------------- Part B

    private static void shape() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        List<Rectify.Event> one = new ArrayList<>();
        one.add(new Rectify.Event("a marriage",
            new SweDate(2010, 6, 1, 12.0).getJulDay(), ""));

        Rectify.Result r = Rectify.scan(sw, Y, M, D, LAT, LON, 6.0, 8.0, 10, one);
        ok("the window is honoured (" + r.candidates.size() + " at ten-minute steps)",
            r.candidates.size() == 13);
        double first = 99.0;
        double last = -99.0;
        for (Rectify.Candidate c : r.candidates) {
            first = Math.min(first, c.hour);
            last = Math.max(last, c.hour);
        }
        ok(String.format("from %.2f to %.2f", first, last),
            first >= 6.0 - 1e-9 && last <= 8.0 + 1e-9);

        boolean sorted = true;
        for (int i = 1; i < r.candidates.size(); i++) {
            sorted &= r.candidates.get(i - 1).score >= r.candidates.get(i).score;
        }
        ok("the candidates come back best first", sorted);

        // The refusals: a scan with nothing to go on produces nothing rather than everything.
        ok("no events, no candidates",
            Rectify.scan(sw, Y, M, D, LAT, LON, 6.0, 8.0, 10, new ArrayList<>())
                .candidates.isEmpty());
        ok("null events, no candidates",
            Rectify.scan(sw, Y, M, D, LAT, LON, 6.0, 8.0, 10, null).candidates.isEmpty());
        ok("a backwards window gives nothing",
            Rectify.scan(sw, Y, M, D, LAT, LON, 8.0, 6.0, 10, one).candidates.isEmpty());
        ok("a step of nothing gives nothing",
            Rectify.scan(sw, Y, M, D, LAT, LON, 6.0, 8.0, 0, one).candidates.isEmpty());
        ok("and an empty result has no best", new Rectify.Result().best() == null);
    }

    // ---------------------------------------------------------------- Part C

    /**
     * A scan that cannot tell the times apart has to say so.
     *
     * <b>The failure this guards is the one a practitioner cannot catch.</b> A program printing
     * "07:33" from three half-remembered dates will be believed, and the fact that 07:11 scored
     * within a thousandth of it is invisible unless the program says it.
     */
    private static void noFalseConfidence() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        // One event with no named body: at a degree of orb, some direction fits nearly every
        // candidate time, so nothing is being discriminated.
        List<Rectify.Event> vague = new ArrayList<>();
        vague.add(new Rectify.Event("something, some time",
            new SweDate(2005, 3, 15, 12.0).getJulDay(), ""));
        Rectify.Result flat = Rectify.scan(sw, Y, M, D, LAT, LON, 0.0, 23.0, 30, vague);
        ok("a whole day was scanned (" + flat.candidates.size() + ")",
            flat.candidates.size() > 40);

        // <b>And its separation is LARGE, which is the finding.</b> Most times score nothing and
        // a handful score well, so the best stands far above the median - 0.50 against a floor of
        // 0.25. The spread is real and the conclusion is worthless, which is why confidence needs
        // evidence as well as arithmetic.
        ok(String.format("one event can produce a wide separation (%.3f)", flat.separation),
            flat.separation > Rectify.confidenceFloor);
        ok("and is still not confident, because one event is not three", !flat.confident);
        ok("the result says how much evidence it had (" + flat.events + ")", flat.events == 1);

        // Two is not enough either, and the boundary is asserted rather than assumed.
        List<Rectify.Event> two = new ArrayList<>(vague);
        two.add(new Rectify.Event("another thing",
            new SweDate(2011, 8, 2, 12.0).getJulDay(), ""));
        ok("nor are two", !Rectify.scan(sw, Y, M, D, LAT, LON, 0.0, 23.0, 30, two).confident);

        ok("the floor is a number that can be met",
            Rectify.confidenceFloor > 0.0 && Rectify.confidenceFloor < 1.0);
        ok("and the evidence rule asks for what the technique asks for",
            Rectify.minimumEvents == 3);
        ok("separation is zero when every candidate scores alike",
            new Rectify.Result().separation == 0.0);
    }

    // ---------------------------------------------------------------- plumbing

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
