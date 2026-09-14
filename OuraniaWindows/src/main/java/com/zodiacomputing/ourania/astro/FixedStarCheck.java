package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed stars: the catalogue places them where the almanacs do, and the contacts are complete.
 *
 * <p>Part A holds the stars to their published tropical longitudes for J2000 - the values every
 * fixed-star table prints - so a catalogue read in the wrong frame, or a proper motion applied
 * twice, fails on a number that does not come from this code. Part B holds precession to a
 * historical event: Regulus left Leo for Virgo in late 2011.
 */
public final class FixedStarCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double birth = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);
        try {
            part("A: the published J2000 positions", () -> published(sw));
            part("B: Regulus crossed into Virgo in 2011", () -> regulus(sw));
            part("C: every listed star is in the catalogue", () -> catalogue(sw, birth));
            part("D: a real chart, complete", () -> contacts(sw, birth));
            part("E: in a sidereal chart the stars move with the zodiac", () -> sidereal(sw, birth));
            part("F: no birth time, no angles; the table says it", () -> tableAndTime(sw, birth));
        } finally {
            Ephemeris.setZodiac("Tropical");
        }
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

    private static String[] def(String name) {
        for (String[] d : FixedStars.STARS) {
            if (d[0].equals(name)) {
                return d;
            }
        }
        return null;
    }

    private static void published(SwissEph sw) {
        double j2000 = SweDate.getJulDay(2000, 1, 1, 12.0);
        // Tropical longitude at J2000, degrees and minutes, as fixed-star tables print them.
        Object[][] known = {
            {"Regulus", 149, 50}, {"Aldebaran", 69, 47}, {"Antares", 249, 46},
            {"Fomalhaut", 333, 52}, {"Spica", 203, 50}, {"Algol", 56, 10},
            {"Sirius", 104, 5}, {"Alcyone", 60, 0},
        };
        for (Object[] k : known) {
            FixedStars.Star s = FixedStars.at(sw, def((String) k[0]), j2000);
            double want = (Integer) k[1] + (Integer) k[2] / 60.0;
            ok(k[0] + " at J2000 is " + k[1] + " deg " + k[2] + " min, got "
                + (s == null ? null : String.format("%.3f", s.longitude)),
                s != null && Math.abs(Almanac.signedDelta(s.longitude, want)) < 2.0 / 60.0);
        }
    }

    private static void regulus(SwissEph sw) {
        FixedStars.Star y2011 = FixedStars.at(sw, def("Regulus"), SweDate.getJulDay(2011, 1, 1, 0.0));
        FixedStars.Star y2012 = FixedStars.at(sw, def("Regulus"), SweDate.getJulDay(2012, 1, 1, 0.0));
        ok("Regulus was still in Leo at the start of 2011", y2011 != null && y2011.longitude < 150.0);
        ok("and in Virgo at the start of 2012", y2012 != null && y2012.longitude >= 150.0);
        if (y2011 != null && y2012 != null) {
            double arcsec = (y2012.longitude - y2011.longitude) * 3600.0;
            ok("moving about 50 arcseconds a year, got " + Math.round(arcsec), arcsec > 45 && arcsec < 55);
        }
    }

    private static void catalogue(SwissEph sw, double jd) {
        FixedStars.Result r = FixedStars.of(sw, ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0));
        ok("all " + FixedStars.STARS.length + " stars placed, missing " + r.missing, r.missing.isEmpty()
            && r.stars.size() == FixedStars.STARS.length);
        boolean onEcliptic = true;
        for (FixedStars.Star s : r.stars) {
            onEcliptic &= s.longitude >= 0 && s.longitude < 360 && Math.abs(s.latitude) <= 90;
        }
        ok("every longitude and latitude in range", onEcliptic);
        // Found by designation: the catalogue has two "Menkar" lines, so names are not trusted.
        SwissEph fresh = new SwissEph(Ephemeris.PATH);
        FixedStars.Star byName = FixedStars.at(fresh, new String[]{"Rigel", "beOri", ""}, jd);
        ok("a star is found by its Bayer designation", byName != null);
    }

    private static void contacts(SwissEph sw, double jd) {
        ChartFrame f = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        FixedStars.Result r = FixedStars.of(sw, f);
        int expected = 0;
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b == null || !b.ok || !FixedStars.covered(Bodies.at(i))) {
                continue;
            }
            for (FixedStars.Star s : r.stars) {
                expected += Aspects.separation(b.lon, s.longitude) <= FixedStars.orb ? 1 : 0;
            }
        }
        ok("every point on a star, once: " + r.contacts.size() + " of " + expected,
            r.contacts.size() == expected);
        boolean sorted = true;
        for (int i = 1; i < r.contacts.size(); i++) {
            sorted &= r.contacts.get(i - 1).off <= r.contacts.get(i).off;
        }
        ok("tightest first", sorted);
        StringBuilder sb = new StringBuilder();
        for (FixedStars.Contact c : r.contacts) {
            sb.append(c.point).append(" on ").append(c.star.name)
              .append(String.format(" %.2f; ", c.off));
        }
        System.out.println("  David's chart: " + (sb.length() == 0 ? "none" : sb));
    }

    private static void sidereal(SwissEph sw, double jd) {
        Ephemeris.setZodiac("Tropical");
        ChartFrame tf = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        FixedStars.Result t = FixedStars.of(sw, tf);
        Ephemeris.setZodiac("Sidereal (Lahiri)");
        ChartFrame sf = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        FixedStars.Result s = FixedStars.of(sw, sf);
        double worst = 0.0;
        for (int i = 0; i < Math.min(t.stars.size(), s.stars.size()); i++) {
            FixedStars.Star a = t.stars.get(i);
            FixedStars.Star b = null;
            for (FixedStars.Star x : s.stars) {
                if (x.name.equals(a.name)) {
                    b = x;
                }
            }
            if (b != null) {
                worst = Math.max(worst, Math.abs(Almanac.signedDelta(a.longitude - sf.ayanamsa, b.longitude)));
            }
        }
        ok("every star moves by the ayanamsa, worst " + worst, worst < 0.001);
        ok("so the same points stand on the same stars: " + t.contacts.size() + " and " + s.contacts.size(),
            t.contacts.size() == s.contacts.size());
    }

    private static void tableAndTime(SwissEph sw, double jd) {
        Ephemeris.setZodiac("Tropical");
        ChartFrame unknown = ChartFrame.computeTimeUnknown(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        boolean angles = FixedStars.of(sw, unknown).contacts.stream()
            .anyMatch(c -> c.point.equals("Ascendant") || c.point.equals("MC"));
        ok("a chart with no birth time puts no angle on a star", !angles);
        ChartFrame f = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        String html = com.zodiacomputing.ourania.gui.ChartTables.fixedStars(f, sw);
        FixedStars.Result r = FixedStars.of(sw, f);
        ok("the table lists every star", r.stars.stream().allMatch(s -> html.contains(">" + s.name + "<")));
        ok("and every contact", r.contacts.stream().allMatch(c -> html.contains(">" + c.point + "<")));
        SwissEph lost = new SwissEph("C:/no-such-ephemeris");
        String none = com.zodiacomputing.ourania.gui.ChartTables.fixedStars(f, lost);
        ok("without the catalogue the table says where it looked", none.contains("sefstars.txt"));
    }

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
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
