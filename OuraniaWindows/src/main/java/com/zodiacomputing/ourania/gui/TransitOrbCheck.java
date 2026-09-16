package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.TransitSearch;
import com.zodiacomputing.ourania.astro.Transits;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.JSpinner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Master list F3: one transit orb, and every transit surface judging by it.
 *
 * <p>Before, the Report and Synthesis transit lists took the natal body table (ten degrees to the
 * Sun) while the Transit Search and Calendar took a flat degree - the same question answered two
 * ways. The width itself is a convention (David's call, 1 degree; see {@link Transits#orb}), so
 * nothing here can check that it is right. What is checked is that it is <b>one</b> rule:
 *
 * <ul>
 * <li>A - the transit list is exactly the contacts inside the flat orb, enumerated here by brute
 *     force over every aspect, at the default and at a wider setting.</li>
 * <li>B - the transit list at a moment and the Transit Search's passages at that moment name the
 *     same transits, so the Report and the Search cannot disagree about what is in orb.</li>
 * <li>C - the setting: stored, validated, put in force, and followed by the Settings control, the
 *     Search's orb box and the Calendar.</li>
 * <li>D - what the old rule got wrong no longer happens: a Pluto conjunction to the natal Sun is a
 *     season in the transit list, not a decade.</li>
 * </ul>
 */
public final class TransitOrbCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        Transits.orb = Settings.transitOrb();
        try {
            part("A: the transit list is the flat orb, by brute force", () -> brute(sw));
            part("B: the transit list and the Transit Search agree", () -> agree(sw));
            part("C: the setting", TransitOrbCheck::setting);
            part("D: a transit is a season, not a decade", () -> season(sw));
        } finally {
            Transits.orb = Transits.DEFAULT_ORB;
        }

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (int i = 0; i < Math.min(40, failures.size()); i++) {
            System.out.println("  " + failures.get(i));
        }
        System.exit(1);
    }

    private static ChartFrame cast(SwissEph sw, double jd, double lat, double lon) {
        return ChartFrame.compute(sw, jd, lat, lon, 'P', false, 0.0);
    }

    private static String key(String transiting, String natal, Aspects.Type t) {
        return transiting + "|" + natal + "|" + t;
    }

    // ------------------------------------------------------------------ A

    private static void brute(SwissEph sw) {
        ok("a fresh install's transit orb is 1 degree", Settings.transitOrb() == 1.0 && Transits.orb == 1.0);
        for (double orb : new double[]{1.0, 2.5}) {
            Transits.orb = orb;
            Random r = new Random(315L);
            int charts = 150;
            int mismatched = 0;
            int hits = 0;
            int wide = 0;
            int wrongOrbUsed = 0;
            String firstMismatch = null;
            for (int c = 0; c < charts; c++) {
                double jdN = 2415020.5 + r.nextDouble() * 36500;
                double lat = -55 + r.nextDouble() * 115;
                double lon = -180 + r.nextDouble() * 360;
                ChartFrame natal = cast(sw, jdN, lat, lon);
                ChartFrame tr = cast(sw, jdN + 7300 + r.nextDouble() * 18000, lat, lon);

                List<Transits.Hit> got = Transits.toNatal(natal, tr, null, null, 0, false);
                Set<String> have = new TreeSet<>();
                for (Transits.Hit h : got) {
                    have.add(key(h.transiting, h.natal, h.type));
                    hits++;
                    if (h.offBy > orb + 1e-9) {
                        wide++;
                    }
                    if (Math.abs(h.orbUsed - Math.min(orb, h.type.maxOrb)) > 1e-12) {
                        wrongOrbUsed++;
                    }
                }

                // Every transiting body against the lights and the four angles, every aspect,
                // measured here: in when within the flat orb and the aspect's own ceiling.
                Set<String> want = new TreeSet<>();
                String[] natalNames = {"Sun", "Moon", "Ascendant", "MC", "Descendant", "IC"};
                double[] natalLons = {natal.body("Sun").lon, natal.body("Moon").lon,
                    natal.asc, natal.mc, natal.dsc, natal.ic};
                for (ChartFrame.Body t : tr.bodies) {
                    if (t == null || !t.ok || "Moon".equals(t.name)) {
                        continue;
                    }
                    for (int i = 0; i < natalNames.length; i++) {
                        if (Aspects.bothCalculated(t.name, natalNames[i])) {
                            continue;
                        }
                        double d = Math.abs(t.lon - natalLons[i]) % 360.0;
                        double sep = d > 180.0 ? 360.0 - d : d;
                        for (Aspects.Type type : Aspects.Type.values()) {
                            if (Math.abs(sep - type.exactAngle) <= Math.min(orb, type.maxOrb)) {
                                want.add(key(t.name, natalNames[i], type));
                            }
                        }
                    }
                }
                if (!have.equals(want)) {
                    mismatched++;
                    if (firstMismatch == null) {
                        Set<String> extra = new TreeSet<>(have);
                        extra.removeAll(want);
                        Set<String> missing = new TreeSet<>(want);
                        missing.removeAll(have);
                        firstMismatch = "extra " + extra + " missing " + missing;
                    }
                }
            }
            String at = " at " + orb + " degrees";
            ok("the list is exactly the contacts in the flat orb, " + mismatched + " of " + charts
                + " charts differ" + at + (firstMismatch == null ? "" : ": " + firstMismatch), mismatched == 0);
            ok("no contact is wider than the orb, " + wide + at, wide == 0);
            ok("each contact reports the orb it was judged on, " + wrongOrbUsed + " wrong" + at, wrongOrbUsed == 0);
            ok("and there are contacts to judge, " + hits + at, hits > charts);
        }
        Transits.orb = Transits.DEFAULT_ORB;
    }

    // ------------------------------------------------------------------ B

    private static void agree(SwissEph sw) {
        Transits.orb = Transits.DEFAULT_ORB;
        Random r = new Random(1982L);
        int moments = 60;
        int differ = 0;
        int inOrb = 0;
        String first = null;
        List<String> lights = Arrays.asList("Sun", "Moon");
        Set<Aspects.Type> majors = new java.util.HashSet<>(Arrays.asList(TransitSearch.MAJOR));
        Set<String> bodies = new java.util.HashSet<>(Arrays.asList(TransitSearch.TRANSITING));
        for (int m = 0; m < moments; m++) {
            double jdN = 2415020.5 + r.nextDouble() * 36500;
            double lat = -50 + r.nextDouble() * 100;
            double lon = -180 + r.nextDouble() * 360;
            ChartFrame natal = cast(sw, jdN, lat, lon);
            double jd = jdN + 7300 + r.nextDouble() * 18000;
            ChartFrame tr = cast(sw, jd, lat, lon);

            Set<String> report = new TreeSet<>();
            for (Transits.Hit h : Transits.toNatal(natal, tr, null, null, 0, false)) {
                if (lights.contains(h.natal) && majors.contains(h.type) && bodies.contains(h.transiting)) {
                    report.add(key(h.transiting, h.natal, h.type));
                }
            }

            // The transit list follows the body selection (a point switched off in Settings is
            // not cast), and the Search offers every body; ask the Search about the same ones.
            List<String> shown = new ArrayList<>();
            for (String b : TransitSearch.TRANSITING) {
                ChartFrame.Body tb = tr.body(b);
                if (tb != null && tb.ok) {
                    shown.add(b);
                }
            }
            Set<String> search = new TreeSet<>();
            for (TransitSearch.Passage p : TransitSearch.search(sw, natal, shown,
                    lights, majors, Transits.orb, jd - 1.0, jd + 1.0)) {
                boolean covers = (Double.isNaN(p.enters) || p.enters <= jd) && (Double.isNaN(p.leaves) || p.leaves >= jd);
                // A retrograde season is one passage even where it steps outside the orb for a
                // while, so "covers the moment" is not yet "in orb at the moment".
                double lonNow = Almanac.bodyLongitude(sw, jd, p.transiting);
                double d = Math.abs(lonNow - p.natalLongitude) % 360.0;
                double sep = d > 180.0 ? 360.0 - d : d;
                if (covers && Math.abs(sep - p.type.exactAngle) <= Transits.orb) {
                    search.add(key(p.transiting, p.natal, p.type));
                }
            }
            inOrb += search.size();
            if (!report.equals(search)) {
                differ++;
                if (first == null) {
                    first = "report " + report + " search " + search;
                }
            }
        }
        ok("the transit list and the Search name the same transits to the lights, " + differ + " of "
            + moments + " moments differ" + (first == null ? "" : ": " + first), differ == 0);
        ok("over enough transits to mean it, " + inOrb, inOrb >= 30);
    }

    // ------------------------------------------------------------------ C

    private static void setting() throws Exception {
        Settings.set(Settings.TRANSIT_ORB_KEY, "not a number");
        ok("an unreadable stored orb is the default", Settings.transitOrb() == Transits.DEFAULT_ORB);
        Settings.set(Settings.TRANSIT_ORB_KEY, "40");
        ok("an out-of-range stored orb is the default", Settings.transitOrb() == Transits.DEFAULT_ORB);
        Settings.setTransitOrb(9.0);
        ok("setting a wider orb than allowed stops at the maximum", Settings.transitOrb() == Settings.TRANSIT_ORB_MAX);

        Settings.setTransitOrb(2.0);
        ok("the setting is saved", Settings.transitOrb() == 2.0);
        ok("and put in force", Transits.orb == 2.0);

        final SettingsPanel[] sp = new SettingsPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> sp[0] = new SettingsPanel(null));
        ok("the Settings control shows it", ((Number) sp[0].transitOrb.getValue()).doubleValue() == 2.0);
        javax.swing.SwingUtilities.invokeAndWait(() -> sp[0].transitOrb.setValue(1.5));
        ok("changing the control saves the orb", Settings.transitOrb() == 1.5);
        ok("and puts it in force", Transits.orb == 1.5);

        final TransitSearchPanel[] tp = new TransitSearchPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> tp[0] = new TransitSearchPanel(null));
        java.lang.reflect.Field f = TransitSearchPanel.class.getDeclaredField("orb");
        f.setAccessible(true);
        JSpinner box = (JSpinner) f.get(tp[0]);
        ok("the Search's orb box opens at the setting", ((Number) box.getValue()).doubleValue() == 1.5);
        Settings.setTransitOrb(2.0);
        javax.swing.SwingUtilities.invokeAndWait(() -> tp[0].refreshChart());
        ok("and follows it when the setting changes", ((Number) box.getValue()).doubleValue() == 2.0);
        javax.swing.SwingUtilities.invokeAndWait(() -> box.setValue(3.0));
        javax.swing.SwingUtilities.invokeAndWait(() -> tp[0].refreshChart());
        ok("but an orb typed for one search is left alone", ((Number) box.getValue()).doubleValue() == 3.0);
        ok("and typing it does not change the setting", Settings.transitOrb() == 2.0 && Transits.orb == 2.0);

        final TransitCalendarPanel[] cp = new TransitCalendarPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> cp[0] = new TransitCalendarPanel(null));
        ok("the Calendar judges by the setting", cp[0].orb() == 2.0);
        ChartFrame natal = ChartFrame.compute(new SwissEph(Ephemeris.PATH),
            SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0), 39.9526, -75.1652, 'P', false, 0.0);
        java.lang.reflect.Field c = TransitCalendarPanel.class.getDeclaredField("chart");
        c.setAccessible(true);
        c.set(cp[0], natal);
        javax.swing.SwingUtilities.invokeAndWait(() -> cp[0].showMonthNow(java.time.YearMonth.of(2026, 9)));
        ok("and the month it draws was worked out at it", cp[0].month != null && cp[0].month.orb == 2.0);

        Settings.setTransitOrb(Transits.DEFAULT_ORB);
    }

    // ------------------------------------------------------------------ D

    private static void season(SwissEph sw) {
        Transits.orb = Transits.DEFAULT_ORB;
        // David's chart. Transiting Pluto's conjunction with his natal Sun (18 Leo) is past, so
        // any Sun in Capricorn would do; the old rule's figure was an average of 14.6 years.
        // Find the longest unbroken run of months a Pluto contact to the Sun stays on the list.
        ChartFrame natal = cast(sw, SweDate.getJulDay(1970, 1, 10, 12.0), 39.9526, -75.1652);
        int longestRun = 0;
        int run = 0;
        int months = 0;
        for (double jd = SweDate.getJulDay(1995, 1, 1, 0.0); jd < SweDate.getJulDay(2035, 1, 1, 0.0); jd += 30.44) {
            ChartFrame tr = cast(sw, jd, 39.9526, -75.1652);
            boolean on = false;
            for (Transits.Hit h : Transits.toNatal(natal, tr, null, null, 0, false)) {
                if ("Pluto".equals(h.transiting) && "Sun".equals(h.natal)
                        && h.type == Aspects.Type.CONJUNCTION) {
                    on = true;
                }
            }
            run = on ? run + 1 : 0;
            months += on ? 1 : 0;
            longestRun = Math.max(longestRun, run);
        }
        ok("Pluto conjunct the natal Sun is on the list at all, " + months + " months", months > 0);
        ok("and for under four years in a row, not a decade: " + longestRun + " months", longestRun < 48);
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
}
