package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;
import de.thmac.swisseph.SweDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sweeps Almanac.mundaneStep and reports what it costs and what it changes.
 *
 * The mundane aspect scan is about four fifths of the cost of an annual calendar, so it
 * is the only knob worth turning. But a coarser step is only a saving if it finds the
 * same events - a step that skips a perfection is not faster, it is wrong. So this times
 * each setting AND diffs its output against a fine reference scan.
 *
 * The reference is 1.0 days, not the old 2.0 default: comparing a candidate against the
 * value it is meant to replace would hide anything they both miss.
 */
public final class CalCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    /** Two events are the same event if same signature and within this many days. */
    private static final double MATCH_DAYS = 0.01;

    private static final double[] STEPS = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0 };

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(EPHE_PATH);

        if (args.length > 0 && "years".equals(args[0])) {
            int a = args.length > 2 ? Integer.parseInt(args[1]) : 2015;
            int b = args.length > 2 ? Integer.parseInt(args[2]) : 2032;
            validateAcrossYears(sw, a, b);
            return;
        }
        if (args.length > 2 && "cost".equals(args[0])) {
            costBreakdown(sw, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            return;
        }

        double jd0 = new SweDate(2026, 1, 1, 0.0).getJulDay();
        double jd1 = new SweDate(2027, 1, 1, 0.0).getJulDay();

        double saved = Almanac.mundaneStep;
        try {
            // Warm the JIT and the ephemeris file cache so the first row is not the outlier.
            Almanac.mundaneStep = 5.0;
            Almanac.annualCalendar(sw, jd0, jd1);

            Almanac.mundaneStep = STEPS[0];
            long t0 = System.nanoTime();
            List<Almanac.Event> reference = Almanac.annualCalendar(sw, jd0, jd1);
            long refMs = (System.nanoTime() - t0) / 1_000_000L;

            System.out.println("Annual calendar 2026, mundane aspect step swept");
            System.out.println("reference = " + STEPS[0] + " d, " + reference.size()
                + " events, " + refMs + " ms");
            System.out.println();
            System.out.printf("%8s %9s %9s %8s %8s %12s%n",
                "step(d)", "events", "time(ms)", "missing", "extra", "worst dt(s)");

            for (double step : STEPS) {
                Almanac.mundaneStep = step;
                long s0 = System.nanoTime();
                List<Almanac.Event> got = Almanac.annualCalendar(sw, jd0, jd1);
                long ms = (System.nanoTime() - s0) / 1_000_000L;

                Diff d = diff(reference, got);
                System.out.printf("%8.1f %9d %9d %8d %8d %12.3f%n",
                    step, got.size(), ms, d.missing, d.extra, d.worstDelta * 86400.0);
            }

            System.out.println();
            System.out.println("missing = in reference, absent here. A non-zero value means");
            System.out.println("this step skips real events and must not be used.");

            // Name any event the widest safe-looking step drops, so a failure is legible.
            Almanac.mundaneStep = 5.0;
            List<Almanac.Event> five = Almanac.annualCalendar(sw, jd0, jd1);
            List<Almanac.Event> lost = missingEvents(reference, five);
            if (lost.isEmpty()) {
                System.out.println();
                System.out.println("At 5.0 d: no events lost against the 1.0 d reference.");
            } else {
                System.out.println();
                System.out.println("At 5.0 d these events are LOST:");
                for (Almanac.Event e : lost) {
                    System.out.println("   " + stamp(e.jd) + "  " + e);
                }
            }
        } finally {
            Almanac.mundaneStep = saved;
        }
    }

    /**
     * One year proves nothing about a step size. The bound is the fastest relative motion
     * a pair reaches, and that peaks only when the faster body is direct at full speed
     * while the slower is retrograde - a configuration that recurs on the synodic cycle,
     * not annually. So the candidate step has to hold over a span long enough to contain
     * several of those, including a Jupiter-Saturn conjunction.
     */
    /**
     * Times each category of the annual calendar separately, so a slowdown can be
     * attributed instead of guessed at. Runs the same year twice and reports the second,
     * so a cold cache is not read as a cost.
     */
    private static void costBreakdown(SwissEph sw, int from, int to) {
        System.out.printf("Cost breakdown per category, %d-%d, mundaneStep=5.0%n", from, to);
        System.out.println();
        System.out.printf("%6s %10s %10s %10s %10s %10s%n",
            "year", "ingress", "chiron-in", "stations", "lunations", "aspects");
        double saved = Almanac.mundaneStep;
        try {
            Almanac.mundaneStep = 5.0;
            for (int y = from; y <= to; y++) {
                double a = new SweDate(y, 1, 1, 0.0).getJulDay();
                double b = new SweDate(y + 1, 1, 1, 0.0).getJulDay();

                long ing = 0;
                long chi = 0;
                for (int pass = 0; pass < 2; pass++) {
                    long t = System.nanoTime();
                    for (String body : Almanac.CALENDAR_BODIES) {
                        if ("Moon".equals(body)) {
                            continue;
                        }
                        Almanac.scanStep = 0.5;
                        Almanac.ingresses(sw, a, b, body);
                    }
                    ing = (System.nanoTime() - t) / 1_000_000L;

                    t = System.nanoTime();
                    Almanac.scanStep = 3.0;
                    Almanac.ingresses(sw, a, b, "Chiron");
                    chi = (System.nanoTime() - t) / 1_000_000L;
                }

                long t2 = System.nanoTime();
                Almanac.scanStep = 1.0;
                for (String body : new String[] { "Mercury", "Venus", "Mars", "Jupiter",
                        "Saturn", "Uranus", "Neptune", "Pluto", "Chiron" }) {
                    Almanac.stations(sw, a, b, body);
                }
                long sta = (System.nanoTime() - t2) / 1_000_000L;

                long t3 = System.nanoTime();
                Almanac.scanStep = 0.5;
                Almanac.lunations(sw, a, b);
                long lun = (System.nanoTime() - t3) / 1_000_000L;

                long t4 = System.nanoTime();
                Almanac.scanStep = 5.0;
                String[] mund = { "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto" };
                for (int i = 0; i < mund.length; i++) {
                    for (int j = i + 1; j < mund.length; j++) {
                        Almanac.exactAspects(sw, a, b, mund[i], mund[j],
                            Aspects.Type.values());
                    }
                }
                long asp = (System.nanoTime() - t4) / 1_000_000L;

                System.out.printf("%6d %10d %10d %10d %10d %10d%n",
                    y, ing, chi, sta, lun, asp);
            }
        } finally {
            Almanac.mundaneStep = saved;
            Almanac.scanStep = 0.5;
        }
    }

    private static void validateAcrossYears(SwissEph sw, int from, int to) {
        System.out.println("Validating mundaneStep 5.0 against a 1.0 reference, "
            + from + "-" + to);
        System.out.println();
        System.out.printf("%6s %8s %9s %8s %8s %12s%n",
            "year", "events", "ms(5d)", "missing", "extra", "worst dt(s)");

        int totMissing = 0;
        int totExtra = 0;
        double worst = 0.0;
        long totalMs = 0;
        double saved = Almanac.mundaneStep;
        try {
            for (int y = from; y <= to; y++) {
                double a = new SweDate(y, 1, 1, 0.0).getJulDay();
                double b = new SweDate(y + 1, 1, 1, 0.0).getJulDay();

                Almanac.mundaneStep = 1.0;
                List<Almanac.Event> ref = Almanac.annualCalendar(sw, a, b);

                Almanac.mundaneStep = 5.0;
                long t0 = System.nanoTime();
                List<Almanac.Event> got = Almanac.annualCalendar(sw, a, b);
                long ms = (System.nanoTime() - t0) / 1_000_000L;
                totalMs += ms;

                Diff d = diff(ref, got);
                totMissing += d.missing;
                totExtra += d.extra;
                worst = Math.max(worst, d.worstDelta);

                System.out.printf("%6d %8d %9d %8d %8d %12.3f%n",
                    y, got.size(), ms, d.missing, d.extra, d.worstDelta * 86400.0);

                for (Almanac.Event e : missingEvents(ref, got)) {
                    System.out.println("        LOST: " + stamp(e.jd) + "  " + e);
                }
            }
        } finally {
            Almanac.mundaneStep = saved;
        }

        System.out.println();
        System.out.println("years scanned    : " + (to - from + 1));
        System.out.println("events missing   : " + totMissing);
        System.out.println("events invented  : " + totExtra);
        System.out.printf("worst timing gap : %.3f s%n", worst * 86400.0);
        System.out.printf("mean per year    : %d ms%n", totalMs / (to - from + 1));
        System.out.println(totMissing == 0 && totExtra == 0
            ? "PASS - 5.0 d finds exactly what 1.0 d finds, every year."
            : "FAIL - 5.0 d is not safe.");
    }

    private static final class Diff {
        int missing;
        int extra;
        double worstDelta;
    }

    /** Signature that identifies an event independent of when it happened. */
    private static String sig(Almanac.Event e) {
        return e.kind + "|" + e.body + "|" + e.other + "|" + e.aspect + "|" + e.sign
            + "|" + e.retrograde;
    }

    private static Map<String, List<Almanac.Event>> bySig(List<Almanac.Event> in) {
        Map<String, List<Almanac.Event>> m = new HashMap<>();
        for (Almanac.Event e : in) {
            m.computeIfAbsent(sig(e), k -> new ArrayList<>()).add(e);
        }
        return m;
    }

    private static Diff diff(List<Almanac.Event> ref, List<Almanac.Event> got) {
        Diff d = new Diff();
        d.missing = missingEvents(ref, got).size();
        d.extra = missingEvents(got, ref).size();
        // Worst timing disagreement across events that do match.
        Map<String, List<Almanac.Event>> g = bySig(got);
        for (Almanac.Event r : ref) {
            List<Almanac.Event> cand = g.get(sig(r));
            if (cand == null) {
                continue;
            }
            double best = Double.MAX_VALUE;
            for (Almanac.Event c : cand) {
                best = Math.min(best, Math.abs(c.jd - r.jd));
            }
            if (best <= MATCH_DAYS && best > d.worstDelta) {
                d.worstDelta = best;
            }
        }
        return d;
    }

    /** Events present in a but with no counterpart in b. */
    private static List<Almanac.Event> missingEvents(List<Almanac.Event> a,
                                                     List<Almanac.Event> b) {
        Map<String, List<Almanac.Event>> m = bySig(b);
        List<Almanac.Event> out = new ArrayList<>();
        for (Almanac.Event e : a) {
            List<Almanac.Event> cand = m.get(sig(e));
            boolean found = false;
            if (cand != null) {
                for (Almanac.Event c : cand) {
                    if (Math.abs(c.jd - e.jd) <= MATCH_DAYS) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                out.add(e);
            }
        }
        return out;
    }

    private static String stamp(double jd) {
        SweDate sd = new SweDate(jd);
        double h = sd.getHour();
        return String.format("%04d-%02d-%02d %02d:%02d", sd.getYear(), sd.getMonth(),
            sd.getDay(), (int) h, (int) Math.round((h - (int) h) * 60.0));
    }

    private CalCheck() { }
}
