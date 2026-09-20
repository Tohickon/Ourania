package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * The sky's houses are cast where the reader is.
 *
 * <p><b>The defect this exists for.</b> Chart A and Chart B are rebuilt from the form on every
 * Generate; the sky subject was passed through untouched and nothing ever gave it a place, so it
 * kept the empty subject's 0,0 - the Atlantic off Africa - and every house it cast was cast
 * there. David, 2026-09-19: "when I have it just on current sky it shows the asc at my natal
 * ascendant in sagittarius... if I then take off chart A it reverts back to sky with the
 * incorrect houses." Measured on a fresh install: the drawn Ascendant was Gemini 11.52, which is
 * exactly the Ascendant at 0,0 for that moment, against Pisces 21 at Philadelphia.
 *
 * <p>So: with no Chart A the wheel's houses must be the sky's own, cast at the reader's home
 * location; the Sky row's location field must reach the wheel; and the sequence David walked -
 * sky alone, Chart A in, Chart A out - must end where it started rather than on stale cusps.
 */
public final class SkyHouseCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** Philadelphia, as the location boxes themselves write a chosen place. */
    private static final String HOME = "39.95, -75.17";
    private static final double HOME_LAT = 39.95;
    private static final double HOME_LON = -75.17;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        Settings.set("home.location", HOME);
        OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        Thread.sleep(2500);
        java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
        fs.setAccessible(true);
        SkymapPanel p = (SkymapPanel) fs.get(hold[0]);
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        // ---- the sky alone
        SwingUtilities.invokeAndWait(() -> {
            p.setComposition(false, false);
            p.updateChartData();
        });
        Thread.sleep(800);
        ok("the sky has the reader's home, not 0,0 (" + p.skyRing.latitude + ", "
            + p.skyRing.longitude + ")",
            Math.abs(p.skyRing.latitude - HOME_LAT) < 0.01
                && Math.abs(p.skyRing.longitude - HOME_LON) < 0.01);
        double home = ascAt(sw, p.skyRing.time, HOME_LAT, HOME_LON);
        double nowhere = ascAt(sw, p.skyRing.time, 0.0, 0.0);
        ok("with only the sky on the wheel the houses are cast at home: drawn "
            + fmt(p.activeAscendant) + ", home " + fmt(home),
            Math.abs(p.activeAscendant - home) < 0.5);
        ok("and not at 0,0: " + fmt(nowhere),
            Math.abs(home - nowhere) > 1.0 && Math.abs(p.activeAscendant - nowhere) > 1.0);

        // ---- David's sequence: sky alone, Chart A in, Chart A out again
        double skyOnly = p.activeAscendant;
        SwingUtilities.invokeAndWait(() -> {
            p.setComposition(true, false);
            p.updateChartData();
        });
        Thread.sleep(800);
        SwingUtilities.invokeAndWait(() -> {
            p.setComposition(false, false);
            p.updateChartData();
        });
        Thread.sleep(800);
        ok("taking Chart A out again returns to the sky's own houses, not stale ones: "
            + fmt(p.activeAscendant) + " against " + fmt(skyOnly),
            Math.abs(p.activeAscendant - skyOnly) < 2.0);

        // ---- the Sky row's location field reaches the wheel
        SwingUtilities.invokeAndWait(() -> p.setSkyPlace("51.5, -0.12"));
        Thread.sleep(800);
        ok("a place typed in the Sky row moves the sky (" + p.skyRing.latitude + ", "
            + p.skyRing.longitude + ")",
            Math.abs(p.skyRing.latitude - 51.5) < 0.01 && Math.abs(p.skyRing.longitude + 0.12) < 0.01);
        double london = ascAt(sw, p.skyRing.time, 51.5, -0.12);
        ok("and the houses follow it: drawn " + fmt(p.activeAscendant) + ", London " + fmt(london),
            Math.abs(p.activeAscendant - london) < 0.5);
        SwingUtilities.invokeAndWait(() -> p.setSkyPlace(""));
        Thread.sleep(300);
        ok("an empty place is ignored rather than moving the sky to 0,0",
            Math.abs(p.skyRing.latitude - 51.5) < 0.01);

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

    private static double ascAt(SwissEph sw, java.time.ZonedDateTime when, double lat, double lon) {
        java.time.ZonedDateTime utc = when.withZoneSameInstant(java.time.ZoneOffset.UTC);
        SweDate sd = new SweDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
            utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0);
        double[] cusps = new double[13];
        double[] ascmc = new double[10];
        sw.swe_houses(sd.getJulDay(), Ephemeris.flags(sw, 2), lat, lon, 'P', cusps, ascmc);
        return ascmc[0];
    }

    private static String fmt(double lon) {
        return String.format("%.2f %s", lon % 30.0, Zodiac.signName(lon));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
