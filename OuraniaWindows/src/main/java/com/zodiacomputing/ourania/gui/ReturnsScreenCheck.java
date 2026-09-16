package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Precession;
import com.zodiacomputing.ourania.astro.Returns;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * Master list F5: returns as a chart - relocated, precessed, and drawn on the wheel.
 *
 * <p>The engine could always cast a return at any place; nothing ever passed it anything but the
 * birthplace, and nothing showed the chart. So what is held here is the two techniques that were
 * missing and the surface that was missing:
 *
 * <ul>
 * <li><b>A, precession</b> - the model against Swiss Ephemeris's own ayanamsa movement, which is an
 * independent measurement of the same thing.</li>
 * <li><b>B, the precessed return</b> - later than the tropical one by the precession the Sun has to
 * cover, and landing on the sidereal degree rather than the tropical one.</li>
 * <li><b>C, relocation</b> - the same instant, so the same planets, and angles that move with the
 * place.</li>
 * <li><b>D, the screen</b> - what it finds, what it says, and that it hands the return to the
 * wheel's outer ring.</li>
 * </ul>
 */
public final class ReturnsScreenCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** David's chart, the one the other suites use. */
    private static final double LAT = 39.9526;
    private static final double LON = -75.1652;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double natalJd = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);
        ChartFrame natal = ChartFrame.compute(sw, natalJd, LAT, LON, 'P', false, 0.0);

        part("A: the precession model", () -> precession(sw));
        part("B: the precessed return", () -> precessed(sw, natal, natalJd));
        part("C: a relocated return", () -> relocated(sw, natal, natalJd));
        part("D: the screen and the wheel", () -> screen(sw, natal, natalJd));

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

    // ------------------------------------------------------------------ A

    private static void precession(SwissEph sw) {
        eq("no precession from J2000 to itself", 0.0, Precession.sinceJ2000(Precession.J2000));
        ok("about 50.3 arcseconds a year, " + String.format("%.2f", Precession.rateArcsecPerYear(Precession.J2000)),
            Math.abs(Precession.rateArcsecPerYear(Precession.J2000) - 50.29) < 0.05);

        // A century of it is about 1.396 degrees, and it runs forward.
        double century = Precession.between(Precession.J2000, Precession.J2000 + 36525.0);
        ok("a century moves the equinox about 1.4 degrees, " + String.format("%.4f", century),
            century > 1.39 && century < 1.40);
        boolean forward = true;
        for (double jd = Precession.J2000 - 40000; jd < Precession.J2000 + 40000; jd += 3652.5) {
            forward &= Precession.between(jd, jd + 3652.5) > 0;
        }
        ok("and always forward", forward);

        // <b>The quadratic term, held where it shows.</b> Over the two centuries compared below it
        // amounts to about an arcsecond and hides inside the tolerance - a mutant that deleted it
        // survived on 2026-09-15. What it does is make the rate quicken, by twice its coefficient
        // per century, so that is what is asserted.
        double quickening = Precession.rateArcsecPerYear(Precession.J2000 + 36525.0)
            - Precession.rateArcsecPerYear(Precession.J2000);
        near("the rate quickens by 0.0221 arcseconds a year every century",
            2.0 * Precession.P2 / 100.0, quickening, 1.0e-9);
        ok("which is a quickening, not a constant", quickening > 0.0);
        // And a millennium either side of J2000, where the term is worth half an arcminute.
        double longRun = Precession.between(Precession.J2000, Precession.J2000 + 365250.0);
        ok("a millennium is about 14 degrees, " + String.format("%.3f", longRun),
            longRun > 13.9 && longRun < 14.0);

        // <b>Against Swiss Ephemeris, which knows nothing of this class.</b> An ayanamsa is the
        // same precession with an epoch constant, so the constant cancels over an interval and the
        // two must agree. Lahiri is used because it is the one this app already offers.
        String keep = Ephemeris.zodiacLabel();
        try {
            Ephemeris.setZodiac("Sidereal (Lahiri)");
            double worst = 0.0;
            // <b>Long spans, and asymmetric ones.</b> The quadratic term contributes T squared, so
            // over an interval that straddles J2000 evenly it cancels and a model without it agrees
            // exactly - which is how a mutant that deleted the term survived on 2026-09-15. Running
            // from 1900 puts the interval on one side, where 500 years of it is worth 17
            // arcseconds. Measured against Swiss, the real model agrees to 0.01 arcseconds even
            // over a millennium.
            for (double years : new double[] {10, 44, 100, 200, 500, 1000}) {
                double from = SweDate.getJulDay(1900, 1, 1, 0.0);
                double to = from + years * 365.2422;
                // <b>The mean ayanamsa, not Ephemeris.ayanamsa.</b> That one adds the nutation in
                // longitude, because a conversion between the zodiacs needs it - but nutation is a
                // wobble of up to 17 arcseconds either way and is not precession, so comparing two
                // dates through it disagreed by 32 arcseconds, which is two nutations rather than a
                // fault in the model. Measured 2026-09-15.
                double theirs = sw.swe_get_ayanamsa_ut(to) - sw.swe_get_ayanamsa_ut(from);
                double mine = Precession.between(from, to);
                worst = Math.max(worst, Math.abs(theirs - mine) * 3600.0);
            }
            ok("the model matches Swiss Ephemeris's own ayanamsa movement, worst "
                + String.format("%.2f", worst) + " arcseconds over a millennium", worst < 2.0);
        } finally {
            Ephemeris.setZodiac(keep);
        }
    }

    // ------------------------------------------------------------------ B

    private static void precessed(SwissEph sw, ChartFrame natal, double natalJd) {
        double sunLon = natal.body("Sun").lon;
        for (int age : new int[] {1, 20, 44, 70}) {
            Returns.Return tropical = Returns.solar(sw, natalJd, sunLon, age, LAT, LON, 'P', false);
            Returns.Return moved = Returns.solar(sw, natalJd, sunLon, age, LAT, LON, 'P', true);
            ok("both returns exist at age " + age, tropical != null && moved != null);
            if (tropical == null || moved == null) {
                continue;
            }

            // The tropical return puts the Sun back on its natal degree, to the second.
            double tropSun = Almanac.bodyLongitude(sw, tropical.jd, "Sun");
            near("the tropical return has the Sun on its natal degree at " + age,
                0.0, Almanac.signedDelta(tropSun, sunLon), 1.0e-4);
            ok("and carries no precession", tropical.precessionDegrees == 0.0 && !tropical.precessed);

            // The precessed one puts it that far on, which is where the natal degree has gone.
            double carried = Precession.between(natalJd, moved.jd);
            double preSun = Almanac.bodyLongitude(sw, moved.jd, "Sun");
            near("the precessed return has the Sun on the moved degree at " + age,
                0.0, Almanac.signedDelta(preSun, Zodiac.normalise(sunLon + carried)), 1.0e-3);
            near("and records the precession it carried at " + age,
                carried, moved.precessionDegrees, 1.0e-6);

            // About twenty minutes of clock per year of age, later rather than earlier.
            double minutes = (moved.jd - tropical.jd) * 1440.0;
            ok("the precessed return is later by about 20 minutes a year at " + age + ", "
                    + String.format("%.0f", minutes) + " minutes for " + age + " years",
                minutes > 17.0 * age && minutes < 23.0 * age);

            // And it is a different chart: the angles have moved with the clock.
            ok("so its Ascendant is not the tropical one at " + age,
                Math.abs(Almanac.signedDelta(moved.chart.asc, tropical.chart.asc)) > 0.01 || age == 0);
        }

        // The Moon, whose returns are thirteen a year: the same correction, the same direction.
        double moonLon = natal.body("Moon").lon;
        double from = SweDate.getJulDay(2026, 1, 1, 0.0);
        double to = SweDate.getJulDay(2026, 4, 1, 0.0);
        List<Returns.Return> plain = Returns.lunar(sw, natalJd, moonLon, from, to, LAT, LON, 'P', false);
        List<Returns.Return> moved = Returns.lunar(sw, natalJd, moonLon, from, to, LAT, LON, 'P', true);
        eq("the same number of lunar returns either way", plain.size(), moved.size());
        ok("lunar returns are found at all, " + plain.size(), plain.size() >= 3);
        boolean laterEach = plain.size() == moved.size();
        for (int i = 0; i < plain.size() && i < moved.size(); i++) {
            laterEach &= moved.get(i).jd > plain.get(i).jd;
        }
        ok("and each precessed one is later than its tropical twin", laterEach);
    }

    // ------------------------------------------------------------------ C

    private static void relocated(SwissEph sw, ChartFrame natal, double natalJd) {
        double sunLon = natal.body("Sun").lon;
        Returns.Return home = Returns.solar(sw, natalJd, sunLon, 44, LAT, LON, 'P', false);
        Returns.Return away = Returns.solar(sw, natalJd, sunLon, 44, 35.6762, 139.6503, 'P', false);
        ok("both returns exist", home != null && away != null);
        if (home == null || away == null) {
            return;
        }
        eq("a relocated return is the same instant", home.jd, away.jd);

        // The planets are where they were: relocation moves the chair, not the sky. The derived
        // points are the exception and not an exception at all - the angles, the Vertex, the East
        // Point and the Lots are built from the horizon, so they move with it by construction.
        int movedBodies = 0;
        int movedDerived = 0;
        for (int i = 0; i < com.zodiacomputing.ourania.astro.Bodies.count(); i++) {
            com.zodiacomputing.ourania.astro.Bodies.Def d = com.zodiacomputing.ourania.astro.Bodies.at(i);
            ChartFrame.Body b = home.chart.body(d.name);
            ChartFrame.Body o = away.chart.body(d.name);
            if (b == null || o == null || !b.ok || !o.ok) {
                continue;
            }
            boolean shifted = Math.abs(Almanac.signedDelta(b.lon, o.lon)) > 1.0e-9;
            if (d.source == com.zodiacomputing.ourania.astro.Bodies.Source.EPHEMERIS) {
                movedBodies += shifted ? 1 : 0;
            } else {
                movedDerived += shifted ? 1 : 0;
            }
        }
        eq("so every body the ephemeris places stands where it stood", 0, movedBodies);
        ok("while the points built from the horizon move with it, " + movedDerived,
            movedDerived >= 4);

        // The angles and houses are the whole of what moved.
        ok("the Ascendant moves with the place, "
                + String.format("%.0f", Math.abs(Almanac.signedDelta(home.chart.asc, away.chart.asc)))
                + " degrees",
            Math.abs(Almanac.signedDelta(home.chart.asc, away.chart.asc)) > 30.0);
        ok("and the MC with it",
            Math.abs(Almanac.signedDelta(home.chart.mc, away.chart.mc)) > 30.0);
        ok("the return records where it was cast",
            Math.abs(away.lat - 35.6762) < 1e-9 && Math.abs(away.lon - 139.6503) < 1e-9);
    }

    // ------------------------------------------------------------------ D

    private static void screen(SwissEph sw, ChartFrame natal, double natalJd) throws Exception {
        final ReturnsPanel[] p = new ReturnsPanel[1];
        SwingUtilities.invokeAndWait(() -> p[0] = new ReturnsPanel(null));
        SwingUtilities.invokeAndWait(() -> p[0].setChart(natal));

        List<Returns.Return> solar = p[0].compute("Solar", 2026, 2028, LAT, LON, false);
        eq("three years asked for, three solar returns", 3, solar.size());
        ok("each one is a chart", solar.stream().allMatch(r -> r.chart != null));
        ok("and they run in order",
            solar.get(0).jd < solar.get(1).jd && solar.get(1).jd < solar.get(2).jd);

        List<Returns.Return> lunar = p[0].compute("Lunar", 2026, 2026, LAT, LON, false);
        ok("a year of lunar returns is about thirteen, " + lunar.size(),
            lunar.size() >= 12 && lunar.size() <= 14);
        List<Returns.Return> saturn = p[0].compute("Saturn", 2010, 2026, LAT, LON, false);
        ok("Saturn returns once in about thirty years, " + saturn.size() + " in seventeen",
            saturn.size() <= 1);

        // The detail says what it is, including which convention produced it.
        SwingUtilities.invokeAndWait(() -> {
            p[0].model.clear();
            for (Returns.Return r : solar) {
                p[0].model.addElement(r);
            }
            p[0].list.setSelectedIndex(0);
        });
        String html = p[0].detail.getText();
        ok("the reading names the return", html.contains("Solar return"));
        ok("shows its angles against the natal ones", html.contains("Ascendant") && html.contains("natal"));
        ok("and says it is tropical", html.contains("Tropical"));

        List<Returns.Return> pre = p[0].compute("Solar", 2026, 2026, LAT, LON, true);
        SwingUtilities.invokeAndWait(() -> {
            p[0].model.clear();
            p[0].model.addElement(pre.get(0));
            p[0].list.setSelectedIndex(0);
        });
        ok("a precessed return says so, with the degrees it carried",
            p[0].detail.getText().contains("Precessed"));

        // A chart cast elsewhere is labelled as relocated rather than silently different.
        List<Returns.Return> away = p[0].compute("Solar", 2026, 2026, 35.6762, 139.6503, false);
        ok("a return cast away from the birthplace knows it", away.get(0).relocated);
        ok("and one cast at home does not", !solar.get(0).relocated);

        // Where the return is cast comes from the selector, and the selector was never held: the
        // computations above pass coordinates directly, so a screen that ignored the control would
        // have passed every one of them.
        SwingUtilities.invokeAndWait(() -> p[0].where.setSelectedIndex(0));
        Object[] home = p[0].placeFor();
        ok("Birthplace means the chart's own place",
            home != null && Math.abs((Double) home[0] - natal.geoLat) < 1e-9
                && Math.abs((Double) home[1] - natal.geoLon) < 1e-9);
        SwingUtilities.invokeAndWait(() -> {
            p[0].where.setSelectedIndex(2);
            p[0].place.setText("Tokyo");
        });
        Object[] elsewhere = p[0].placeFor();
        ok("and a place typed in is looked up in the atlas, "
                + (elsewhere == null ? "not found" : elsewhere[2]),
            elsewhere != null && Math.abs((Double) elsewhere[0] - 35.69) < 0.5
                && Math.abs((Double) elsewhere[1] - 139.69) < 0.5);
        SwingUtilities.invokeAndWait(() -> p[0].place.setText("nowhere at all, really"));
        ok("a place the atlas does not have is refused rather than guessed", p[0].placeFor() == null);
        SwingUtilities.invokeAndWait(() -> p[0].where.setSelectedIndex(0));

        // The door, and the handoff to the wheel.
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            boolean row = false;
            for (String[] s : SidePanel.SCREENS) {
                row |= "RETURNS".equals(s[1]);
            }
            ok("the menu offers Returns", row);
            SwingUtilities.invokeAndWait(() -> w[0].switchScreen("RETURNS"));
            ok("and the screen opens", true);

            java.lang.reflect.Field f = OuraniaWindow.class.getDeclaredField("skymapPanel");
            f.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) f.get(w[0]);
            Returns.Return r = solar.get(0);
            SwingUtilities.invokeAndWait(() -> w[0].showReturnOnWheel(r));
            com.zodiacomputing.ourania.astro.ChartSubject onRing = sky.skySubject();
            ok("the wheel's outer ring is now the return", onRing != null
                && onRing.label.contains("Solar return"));
            near("at the return's own moment",
                r.jd, new SweDate(onRing.moment.getYear(), onRing.moment.getMonthValue(),
                    onRing.moment.getDayOfMonth(),
                    onRing.moment.getHour() + onRing.moment.getMinute() / 60.0
                        + onRing.moment.getSecond() / 3600.0).getJulDay(), 1.0 / 1440.0);
            ok("and at the place it was cast for",
                Math.abs(onRing.latitude - r.lat) < 1e-9 && Math.abs(onRing.longitude - r.lon) < 1e-9);
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    // ------------------------------------------------------------------ harness

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
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

    private static void eq(String label, Object expect, Object got) {
        ok(label + ": expected " + expect + ", got " + got,
            expect == null ? got == null : expect.equals(got));
    }

    private static void near(String label, double expect, double got, double tol) {
        checks++;
        if (!(Math.abs(expect - got) <= tol)) {
            failures.add(label + ": expected " + expect + ", got " + got);
        }
    }
}
