package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * What the app does when an ephemeris file will not read.
 *
 *  Part A - a file that opens and then fails a read does not take the application down. This
 *           is the defect itself: the library's own tidal-acceleration probe calls
 *           swe_calc_ut with a null error buffer on every single call, and the segment
 *           reader's error path appended to that buffer without asking whether there was
 *           one. The NullPointerException came out of a method documented to RETURN an error
 *           code, so it sailed past the twenty catch (SwissephException) handlers between
 *           there and the caller - and out of the SwissEph constructor, before any chart had
 *           been asked for. David hit it casting Sydney, 4 July 1975.
 *  Part B - and the reason a file would not read survives to be read. The old error path
 *           printed the stack trace into a StringWriter it then dropped, and appended the
 *           PrintWriter's own toString in its place, so every file error this library has
 *           ever reported ended in something like java.io.PrintWriter@1f2a3b.
 *  Part C - a chart still casts, and every body that reports a position reports the right
 *           one. THIS PART IS RED, and the failure is a second defect found while proving
 *           the first: with the crash guarded, the Sun comes back marked ok carrying the
 *           Moon's longitude. See known-red.txt.
 *  Part D - none of this touched the reader's ephemeris (J14).
 *
 * <b>The damage is made, not waited for.</b> On this machine the real files fail a read only
 * intermittently, which is no basis for a check. A copy with its content destroyed and its
 * length left alone fails deterministically, and reproduces the reported stack frame for
 * frame - truncating it does not, because a length check catches that before any read.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.EphemerisFailureCheck
 */
public final class EphemerisFailureCheck {

    /** The file the Moon's position is computed through, and so the one every calc touches. */
    private static final String FILE = "sepl_18.se1";

    /** Sydney, 4 July 1975, 14:15 local - the chart David lost the app on. */
    private static final double JD = new SweDate(1975, 7, 4, 4.25).getJulDay();
    private static final double LAT = -33.8688;
    private static final double LON = 151.2093;

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();

        Path real = new File(Ephemeris.PATH, FILE).toPath();
        if (!Files.isReadable(real)) {
            // Not a failure: CI has no ephemeris at all, and a check that cannot be set up
            // must say so rather than pass quietly and look like evidence.
            System.out.println("NO VERDICT - " + real + " is not readable, so there is no file "
                + "to damage. This suite measures what happens when one will not read.");
            return;
        }
        long lengthBefore = Files.size(real);
        Path damaged = damagedCopy(real);
        System.out.println("damaged copy: " + damaged);

        System.out.println();
        System.out.println("=== Part A: a file that will not read does not take the app down ===");
        doesNotThrow(damaged);

        System.out.println();
        System.out.println("=== Part B: and the reason survives ===");
        theReasonSurvives(damaged);

        System.out.println();
        System.out.println("=== Part C: a body that says it computed must be where it says ===");
        stillCasts(damaged);

        System.out.println();
        System.out.println("=== Part D: the reader's own ephemeris was not touched ===");
        untouched(real, lengthBefore, damaged);

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

    /**
     * A copy that opens, passes the header's own damage check, and cannot serve a segment.
     *
     * The length is kept because the library compares it before reading: a truncated file is
     * rejected as "damaged" up front and never reaches the error path this suite is about.
     */
    private static Path damagedCopy(Path real) throws Exception {
        Path dir = Files.createTempDirectory("ourania-ephemeris-failure-check");
        dir.toFile().deleteOnExit();
        Path copy = dir.resolve(FILE);
        Files.copy(real, copy, StandardCopyOption.REPLACE_EXISTING);
        copy.toFile().deleteOnExit();
        long length = Files.size(copy);
        byte[] rubbish = new byte[(int) (length - 8192L)];
        java.util.Arrays.fill(rubbish, (byte) 0xFF);
        try (RandomAccessFile f = new RandomAccessFile(copy.toFile(), "rw")) {
            f.seek(8192L);
            f.write(rubbish);
        }
        return copy;
    }

    // ---------------------------------------------------------------- part A

    /**
     * Nothing throws, with or without an error buffer to complain into.
     *
     * <b>The null buffer is the case that crashed</b>, and it is not an exotic one: it is what
     * SweDate.setGlobalTidalAcc passes on every swe_calc_ut, because it only wants to know
     * whether the Moon's file opens and handles the answer itself.
     */
    private static void doesNotThrow(Path damaged) {
        String path = damaged.getParent().toString();

        // The constructor calls swe_set_ephe_path, which probes the Moon. This alone was the
        // reported crash, before a single chart had been asked for.
        yes("a SwissEph can be built on an ephemeris that will not read", built(path));

        int[] bodies = {SweConst.SE_SUN, SweConst.SE_MOON, SweConst.SE_MERCURY,
                        SweConst.SE_MARS, SweConst.SE_TRUE_NODE};
        for (int ipl : bodies) {
            for (boolean withBuffer : new boolean[] {true, false}) {
                String what = "body " + ipl + (withBuffer ? " with an error buffer" : " with none");
                double[] xx = new double[6];
                try {
                    SwissEph sw = new SwissEph(path);
                    int rc = sw.swe_calc_ut(JD, ipl, SweConst.SEFLG_SWIEPH, xx,
                        withBuffer ? new StringBuffer() : null);
                    // The return code is the library's business - an error code IS the
                    // contract being asserted. What must not happen is a throw.
                    yes(what + " returns rather than throwing (rc " + rc + ")", true);
                } catch (Throwable t) {
                    yes(what + " returns rather than throwing, threw "
                        + t.getClass().getName() + ": " + t.getMessage(), false);
                }
            }
        }
    }

    private static boolean built(String path) {
        try {
            return new SwissEph(path) != null;
        } catch (Throwable t) {
            failures.add("building a SwissEph threw " + t.getClass().getName()
                + ": " + t.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------- part B

    private static void theReasonSurvives(Path damaged) {
        StringBuffer serr = new StringBuffer();
        SwissEph sw = new SwissEph(damaged.getParent().toString());
        sw.swe_calc_ut(JD, SweConst.SE_MOON, SweConst.SEFLG_SWIEPH, new double[6], serr);
        String said = serr.toString();
        System.out.println("  serr: " + (said.length() > 120 ? said.substring(0, 120) + "..." : said));

        yes("a file that will not read says so", said.contains("file error in swisseph.FileData"));
        // The exact defect: a PrintWriter's identity hash where the stack trace should be.
        yes("the message is not a writer's identity hash", !said.contains("java.io.PrintWriter@"));
        yes("the message carries the underlying failure", said.contains("Exception"));
        yes("the message carries a stack frame", said.contains("at de.thmac.swisseph."));
    }

    // ---------------------------------------------------------------- part C

    /**
     * A body that says it computed must have computed the right thing.
     *
     * <b>Not "did it return a number".</b> With the crash guarded, a read failure turns into an
     * error code - and on this chart one body comes back MARKED OK CARRYING ANOTHER BODY'S
     * LONGITUDE. The Sun reads 39.447 where it belongs at 101.593, which is the Moon's degree:
     * 9 Taurus instead of 12 Cancer, stated with no warning anywhere on the chart. That is a
     * confident wrong answer, which this project holds to be worse than a silent failure and
     * far worse than a crash - a crash at least tells the reader not to trust what they saw.
     *
     * <p>The rule is stated against a chart cast from the same moment on an ephemeris that
     * reads, because that is what "right" means here, and it is independent of how the wrong
     * value gets in. A body may fall back and agree, or it may say it failed. It may not say
     * it succeeded and be wrong.
     */
    private static void stillCasts(Path damaged) {
        ChartFrame bad;
        try {
            bad = ChartFrame.compute(new SwissEph(damaged.getParent().toString()),
                JD, LAT, LON, 'P', false, 0.0);
        } catch (Throwable t) {
            yes("a chart casts on an ephemeris that will not read, threw "
                + t.getClass().getName(), false);
            return;
        }
        yes("a chart casts on an ephemeris that will not read", bad != null);
        if (bad == null) {
            return;
        }
        yes("its ascendant is a real angle (" + String.format("%.3f", bad.asc) + ")",
            bad.asc >= 0.0 && bad.asc < 360.0);

        ChartFrame good = ChartFrame.compute(new SwissEph(Ephemeris.PATH),
            JD, LAT, LON, 'P', false, 0.0);
        yes("the houses are unaffected - they do not come from a body file",
            Math.abs(bad.asc - good.asc) < 1e-6);

        int computed = 0;
        int refused = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            if (!bad.bodies[i].ok) {
                refused++;
                continue;
            }
            computed++;
            double apart = Math.abs(((bad.bodies[i].lon - good.bodies[i].lon) % 360.0 + 540.0)
                % 360.0 - 180.0);
            yes(Bodies.at(i).name + " says it computed, so it must be where it is: "
                + String.format("%.3f", bad.bodies[i].lon) + " against "
                + String.format("%.3f", good.bodies[i].lon), apart < 0.001);
        }
        System.out.println("  " + computed + " bodies reported a position, " + refused
            + " said they could not");
    }

    // ---------------------------------------------------------------- part D

    private static void untouched(Path real, long lengthBefore, Path damaged) throws Exception {
        yes("the damage was done outside the reader's ephemeris directory",
            !damaged.getParent().toAbsolutePath().equals(
                new File(Ephemeris.PATH).toPath().toAbsolutePath()));
        yes("the reader's " + FILE + " is the length it was", Files.size(real) == lengthBefore);
        yes("the reader's " + FILE + " is still readable", Files.isReadable(real));
    }

    // ---------------------------------------------------------------- harness

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }
}
