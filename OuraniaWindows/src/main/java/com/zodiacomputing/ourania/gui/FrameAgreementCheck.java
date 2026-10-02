package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * The wheel and the reading frame describe the same chart.
 *
 * <p><b>Nothing asserted this, which is why it was wrong for weeks.</b> The wheel draws from
 * {@code natalRing.cusps}; every reading, dignity, lot and house-of-a-placement comes from the
 * frame {@code radixChart} returns. Two objects, one chart, and no check that they agreed.
 *
 * <p>David found it on 2026-09-27 by reading the running app - a house-occupant list put the
 * Descendant inside house 1 - and it took until 2026-09-28 to name, through six ruled-out causes.
 * The cause: with no Chart A the inner wheel is the sky, {@code updateChartData} copied
 * skyRing's MOMENT into natalRing and cast the wheel's houses at skyRing's PLACE, while
 * {@code radixChart} went on handing natalRing's place to the frame. Measured at open before the
 * fix: the wheel at 51.478, 0.000 and the frame at 34.052, -118.244, <b>94 degrees of Ascendant
 * apart</b> - with every planet agreeing exactly, because planets do not depend on place.
 *
 *  Part A - with no Chart A, where the inner wheel is the sky. This is the state that was wrong.
 *  Part B - with a chart cast, where it was always right. Kept so a fix that traded one state
 *           for the other cannot pass.
 *  Part C - the inputs, not just the outputs: both paths are handed the same place and the same
 *           instant. An agreement reached from different inputs would be luck.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.gui.FrameAgreementCheck
 */
public final class FrameAgreementCheck {

    /** Degrees. Both sides call swe_houses with the same arguments, so this is float noise. */
    private static final double TOLERANCE = 1e-6;

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            waitFor(() -> sky.radixChart() != null, 25000);

            System.out.println("=== Part A: no Chart A - the inner wheel is the sky ===");
            compare(sky, "sky only");

            // <b>Waits on the cast finishing, not on a field the cast writes.</b> This waited for
            // natalRing.time to read 1990, which the worker wrote BEFORE the wheel was recast -
            // so about one run in three compared a wheel still holding the sky's cusps against
            // a frame cast from today's instant at London, and failed 19 checks 110-170 degrees
            // apart. The count moves last in the cast's done(), on the event thread, and is read
            // there too, so seeing it move means the whole chart is in place.
            final int[] before = new int[1];
            SwingUtilities.invokeAndWait(() -> before[0] = sky.castsApplied());
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ChartSetupPanel form = w.chartSetup();
                    String[] v = form.formSnapshot();
                    v[0] = "1990-02-14";
                    v[1] = "09:30";
                    v[2] = "London, England, United Kingdom";
                    form.formRestore(v);
                    form.generateChart();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            // 90s bounds a hang, not a reasonable time - the same budget ec44b792 gave a cast
            // under full-regression load. Reaching it is a failure, named, not a fall-through
            // into nineteen comparisons against whatever happened to be there.
            boolean cast = waitFor(() -> sky.castsApplied() > before[0], 90000);
            yes("London 1990: the cast finished within 90s", cast);
            if (cast) {
                final boolean[] is1990 = new boolean[1];
                SwingUtilities.invokeAndWait(() ->
                    is1990[0] = sky.natalRing.time != null && sky.natalRing.time.getYear() == 1990);
                yes("London 1990: the finished cast is the 1990 chart", is1990[0]);
            }

            System.out.println();
            System.out.println("=== Part B: a chart cast - the inner wheel is Chart A ===");
            compare(sky, "London 1990");
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String s : failures) {
                System.out.println("  " + s);
            }
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Every reading taken inside ONE event-thread block.
     *
     * <b>Not a style preference.</b> The probe that found this defect reported a second one that
     * did not exist - a frame cast at today while the ring said 1990 - because it read the ring
     * on the main thread and called radixChart beside it, across a mutation. A torn read looks
     * exactly like a defect and wastes the same amount of time.
     */
    private static void compare(SkymapPanel sky, String state) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ChartFrame f = sky.radixChart();
            yes(state + ": there is a frame to compare against", f != null);
            if (f == null) {
                return;
            }

            // Part C's inputs, checked first: agreement reached from different inputs is luck.
            near(state + ": both cast at the same latitude", sky.innerLat(), f.geoLat, 1e-9);
            near(state + ": both cast at the same longitude", sky.innerLon(), f.geoLon, 1e-9);
            if (sky.natalRing.sd != null) {
                near(state + ": both cast at the same instant",
                    sky.natalRing.sd.getJulDay(), f.julianDayUt, 1e-9);
            }

            // <b>The angles, which is what a reader sees and what was wrong.</b>
            near(state + ": the Ascendant the wheel draws is the one the readings use",
                sky.natalRing.ascendant, f.asc, TOLERANCE);
            near(state + ": and the active Ascendant too", sky.activeCusps[1], f.asc, TOLERANCE);
            near(state + ": the Midheaven agrees", sky.natalRing.cusps[10], f.cusps[10], TOLERANCE);

            // Every cusp, not only the angles: a house a placement is reported in comes from
            // these, and a reader is told "your Mars is in the seventh" on their authority.
            for (int h = 1; h <= 12; h++) {
                near(state + ": cusp " + h + " agrees",
                    sky.natalRing.cusps[h], f.cusps[h], TOLERANCE);
            }

            // The planets were never the problem - they do not depend on place - so they are
            // asserted as the control. If these ever disagree it is a different defect.
            int compared = 0;
            for (int i = 0; i < sky.natalRing.lon.length && i < f.bodies.length; i++) {
                if (sky.natalRing.valid[i] && f.bodies[i] != null && f.bodies[i].ok) {
                    compared++;
                    near(state + ": " + f.bodies[i].name + " agrees",
                        sky.natalRing.lon[i], f.bodies[i].lon, 1e-4);
                }
            }
            yes(state + ": bodies were compared as a control: " + compared, compared > 5);
        });
    }

    // ---------------------------------------------------------------- harness

    /** True once {@code until} holds, false if the deadline passed first. */
    private static boolean waitFor(java.util.function.BooleanSupplier until, long limitMs)
            throws Exception {
        long end = System.nanoTime() + limitMs * 1_000_000L;
        while (System.nanoTime() < end) {
            if (until.getAsBoolean()) {
                SwingUtilities.invokeAndWait(() -> { });
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }

    private static void near(String label, double expected, double actual, double tolerance) {
        checks++;
        double apart = Math.abs(((expected - actual) % 360.0 + 540.0) % 360.0 - 180.0);
        if (Double.isNaN(actual) || apart > tolerance) {
            failures.add(label + String.format(": wheel %.6f, frame %.6f, %.6f apart",
                expected, actual, apart));
        }
    }
}
