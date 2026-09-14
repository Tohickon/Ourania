package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The sidereal zodiac: every position moves by the ayanamsa and nothing else does.
 *
 * <p>The switch is one static read at about twenty ephemeris calls, so the danger is not the
 * arithmetic but a call that was missed - a wheel in one zodiac and its transits in the other.
 * Most parts here compare a sidereal cast with a tropical cast of the same moment and require
 * the difference to be exactly the ayanamsa where it should be, and exactly zero where it
 * should not: declination, obliquity, and the solstice-defined antiscia.
 */
public final class SiderealCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double birth = SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0);
        try {
            part("A: the published ayanamsas", () -> published(sw));
            part("B: a chart moves by the ayanamsa, its declinations do not", () -> chart(sw, birth));
            part("C: the almanac is in the same zodiac as the chart", () -> almanac(sw));
            part("D: a composite's houses move with it", () -> composite(sw, birth));
            part("E: the antiscia do not move", () -> antiscia(sw, birth));
            part("F: transits are read in one zodiac", () -> transits(sw, birth));
            part("G: the switch", SiderealCheck::theSwitch);
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

    /**
     * Lahiri at J2000 is 23 deg 51 min 11 s and Fagan-Bradley 24 deg 44 min 12 s (Swiss Ephemeris
     * documentation, section 2.7); Krishnamurti and Raman are a few minutes and about 1.4 degrees
     * less than Lahiri. Tolerances allow for UT against TT and nutation.
     */
    private static void published(SwissEph sw) {
        double j2000 = 2451545.0;
        ayan(sw, "Sidereal (Lahiri)", j2000, 23.853, 0.02);
        ayan(sw, "Sidereal (Fagan-Bradley)", j2000, 24.737, 0.02);
        ayan(sw, "Sidereal (Krishnamurti)", j2000, 23.76, 0.05);
        ayan(sw, "Sidereal (Raman)", j2000, 22.41, 0.05);
        Ephemeris.setZodiac("Tropical");
        ok("tropical has no ayanamsa", Ephemeris.ayanamsa(sw, j2000) == 0.0);
    }

    private static void ayan(SwissEph sw, String label, double jd, double want, double tol) {
        Ephemeris.setZodiac(label);
        double got = Ephemeris.ayanamsa(sw, jd);
        ok(label + " at J2000 is " + want + ", got " + String.format("%.4f", got),
            Math.abs(got - want) < tol);
    }

    private static void chart(SwissEph sw, double jd) {
        Ephemeris.setZodiac("Tropical");
        ChartFrame trop = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        Ephemeris.setZodiac("Sidereal (Lahiri)");
        ChartFrame sid = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        double ay = sid.ayanamsa;
        ok("the frame records its ayanamsa, got " + ay, ay > 23.5 && ay < 24.0);
        ok("and a tropical frame records none", trop.ayanamsa == 0.0);

        double worstLon = 0.0;
        double worstDec = 0.0;
        int compared = 0;
        for (int i = 0; i < trop.bodies.length; i++) {
            ChartFrame.Body t = trop.bodies[i];
            ChartFrame.Body s = sid.bodies[i];
            if (t == null || s == null || !t.ok || !s.ok) {
                continue;
            }
            compared++;
            worstLon = Math.max(worstLon,
                Math.abs(Almanac.signedDelta(t.lon - ay, s.lon)));
            if (Bodies.at(i).source == Bodies.Source.EPHEMERIS) {
                worstDec = Math.max(worstDec, Math.abs(t.dec - s.dec));
            }
        }
        ok("every placed point is the tropical one less the ayanamsa, over " + compared
            + ", worst " + worstLon, compared > 10 && worstLon < 0.001);
        ok("no declination moves, worst " + worstDec, worstDec < 1e-9);
        ok("nor the obliquity", trop.trueObliquity == sid.trueObliquity);
        double worstCusp = 0.0;
        for (int h = 1; h <= 12; h++) {
            worstCusp = Math.max(worstCusp,
                Math.abs(Almanac.signedDelta(trop.cusps[h] - ay, sid.cusps[h])));
        }
        ok("every cusp moves by the ayanamsa, worst " + worstCusp, worstCusp < 0.001);
        ok("and the Ascendant", Math.abs(Almanac.signedDelta(trop.asc - ay, sid.asc)) < 0.001);
        ok("and the MC", Math.abs(Almanac.signedDelta(trop.mc - ay, sid.mc)) < 0.001);
        ok("the Sun changes sign: Leo tropical, Cancer sidereal",
            Zodiac.signName(trop.body("Sun").lon).equals("leo")
                && Zodiac.signName(sid.body("Sun").lon).equals("cancer"));
    }

    private static void almanac(SwissEph sw) {
        double jd = SweDate.getJulDay(2027, 7, 2, 0.0);
        Ephemeris.setZodiac("Tropical");
        double t = Almanac.bodyLongitude(sw, jd, "Saturn");
        Ephemeris.setZodiac("Sidereal (Fagan-Bradley)");
        double s = Almanac.bodyLongitude(sw, jd, "Saturn");
        double ay = Ephemeris.ayanamsa(sw, jd);
        ok("the almanac's Saturn moves by the ayanamsa, off by "
            + Math.abs(Almanac.signedDelta(t - ay, s)), Math.abs(Almanac.signedDelta(t - ay, s)) < 0.001);
        // A second instance that has never been told the mode must still get it.
        SwissEph fresh = new SwissEph(Ephemeris.PATH);
        double f = Almanac.bodyLongitude(fresh, jd, "Saturn");
        ok("and so does an ephemeris instance made after the switch", Math.abs(f - s) < 1e-9);
        // And one told Lahiri earlier is told again when the mode changes.
        Ephemeris.setZodiac("Sidereal (Lahiri)");
        double l = Almanac.bodyLongitude(fresh, jd, "Saturn");
        ok("an instance is re-told when the ayanamsa changes, moved " + Math.abs(l - f),
            Math.abs(Almanac.signedDelta(l, f) - (Ephemeris.ayanamsa(fresh, jd) - ay)) > -1e-9
                && Math.abs(l - f) > 0.5);
    }

    private static void composite(SwissEph sw, double birth) {
        double other = SweDate.getJulDay(1985, 3, 2, 14.5);
        Ephemeris.setZodiac("Tropical");
        ChartFrame ta = ChartFrame.compute(sw, birth, 39.9526, -75.1652, 'P', false, 0.0);
        ChartFrame tb = ChartFrame.compute(sw, other, 34.05, -118.24, 'P', false, 0.0);
        ChartFrame tc = ChartFrame.computeMidpointComposite(sw, ta, tb);
        Ephemeris.setZodiac("Sidereal (Lahiri)");
        ChartFrame sa = ChartFrame.compute(sw, birth, 39.9526, -75.1652, 'P', false, 0.0);
        ChartFrame sb = ChartFrame.compute(sw, other, 34.05, -118.24, 'P', false, 0.0);
        ChartFrame sc = ChartFrame.computeMidpointComposite(sw, sa, sb);
        double ay = sc.ayanamsa;
        ok("the composite carries the mean ayanamsa", Math.abs(ay - (sa.ayanamsa + sb.ayanamsa) / 2) < 1e-12);
        double worst = 0.0;
        for (int h = 1; h <= 12; h++) {
            worst = Math.max(worst, Math.abs(Almanac.signedDelta(tc.cusps[h] - ay, sc.cusps[h])));
        }
        worst = Math.max(worst, Math.abs(Almanac.signedDelta(tc.asc - ay, sc.asc)));
        // The two natal ayanamsas differ by a few minutes, so the midpoint MC is not exactly the
        // tropical one less the mean; a hundredth of a degree allows for that and nothing more.
        ok("the composite's cusps and Ascendant move by the ayanamsa, worst " + worst, worst < 0.01);
    }

    private static void antiscia(SwissEph sw, double jd) {
        Ephemeris.setZodiac("Tropical");
        Antiscia.Result t = Antiscia.of(ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0));
        Ephemeris.setZodiac("Sidereal (Lahiri)");
        ChartFrame sf = ChartFrame.compute(sw, jd, 39.9526, -75.1652, 'P', false, 0.0);
        Antiscia.Result s = Antiscia.of(sf);
        ok("the same contacts in either zodiac: " + t.contacts.size() + " and " + s.contacts.size(),
            t.contacts.size() == s.contacts.size());
        boolean same = t.contacts.size() == s.contacts.size();
        for (int i = 0; same && i < t.contacts.size(); i++) {
            same = t.contacts.get(i).a.equals(s.contacts.get(i).a)
                && t.contacts.get(i).b.equals(s.contacts.get(i).b)
                && Math.abs(t.contacts.get(i).off - s.contacts.get(i).off) < 0.001;
        }
        ok("pair for pair, orb for orb", same);
        // Mirrored about sidereal 0 Cancer instead, the Moon-Mercury antiscion would move by
        // twice the ayanamsa and be lost.
        boolean moonMercury = s.contacts.stream().anyMatch(c ->
            (c.a + c.b).contains("Moon") && (c.a + c.b).contains("Mercury"));
        ok("the Moon is still on Mercury's antiscion", moonMercury);
    }

    private static void transits(SwissEph sw, double birth) {
        double from = SweDate.getJulDay(2027, 1, 1, 0.0);
        double to = SweDate.getJulDay(2029, 1, 1, 0.0);
        Ephemeris.setZodiac("Tropical");
        ChartFrame tn = ChartFrame.compute(sw, birth, 39.9526, -75.1652, 'P', false, 0.0);
        List<TransitSearch.Passage> tp = TransitSearch.search(sw, tn, List.of("Saturn"),
            List.of("Moon"), List.of(Aspects.Type.CONJUNCTION), 1.0, from, to);
        Ephemeris.setZodiac("Sidereal (Lahiri)");
        ChartFrame sn = ChartFrame.compute(sw, birth, 39.9526, -75.1652, 'P', false, 0.0);
        List<TransitSearch.Passage> sp = TransitSearch.search(sw, sn, List.of("Saturn"),
            List.of("Moon"), List.of(Aspects.Type.CONJUNCTION), 1.0, from, to);
        ok("both zodiacs find Saturn's season on the Moon", tp.size() == 1 && sp.size() == 1);
        if (tp.size() != 1 || sp.size() != 1 || sp.get(0).exacts.isEmpty()) {
            return;
        }
        double worst = 0.0;
        for (TransitSearch.Exact e : sp.get(0).exacts) {
            worst = Math.max(worst, TransitSearch.offBy(Almanac.bodyLongitude(sw, e.jd, "Saturn"),
                sn.body("Moon").lon, Aspects.Type.CONJUNCTION));
        }
        ok("every sidereal exact is exact in the sidereal frame, worst " + worst, worst < 0.001);
        // The ayanamsa grew by about 0.63 degree between 1982 and 2027. A sidereal natal Moon
        // read against tropical transits would be out by that much; read in one zodiac, the
        // season arrives later, by the time Saturn takes to cover it.
        double shift = sp.get(0).exacts.get(0).jd - tp.get(0).exacts.get(0).jd;
        ok("and the season is moved by the precession since birth, " + Math.round(shift) + " days",
            Math.abs(shift) > 1.0);
    }

    private static void theSwitch() {
        Ephemeris.setZodiac("Sidereal (Raman)");
        ok("a label goes in and comes back out", Ephemeris.zodiacLabel().equals("Sidereal (Raman)"));
        ok("sidereal says so", Ephemeris.sidereal());
        Ephemeris.setZodiac("no such zodiac");
        ok("an unknown label is tropical", !Ephemeris.sidereal()
            && Ephemeris.zodiacLabel().equals("Tropical"));
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        ok("tropical flags are the flags given", Ephemeris.flags(sw, 258) == 258);
        Ephemeris.setZodiac("Sidereal (Lahiri)");
        ok("sidereal adds the sidereal bit",
            (Ephemeris.flags(sw, 258) & de.thmac.swisseph.SweConst.SEFLG_SIDEREAL) != 0);
        com.zodiacomputing.ourania.gui.Settings.setZodiac("Sidereal (Krishnamurti)");
        ok("the setting puts its zodiac in force", Ephemeris.zodiacLabel().equals("Sidereal (Krishnamurti)"));
        ok("and saves it", com.zodiacomputing.ourania.gui.Settings.zodiac().equals("Sidereal (Krishnamurti)"));
        com.zodiacomputing.ourania.gui.Settings.setZodiac("Tropical");
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
