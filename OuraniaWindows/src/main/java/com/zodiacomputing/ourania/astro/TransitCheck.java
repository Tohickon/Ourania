package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification for L8: Profection and Transits.
 *
 * Every geometric claim is re-derived here from the frames, deliberately without calling
 * the code under test - a check that asks the implementation whether it agrees with itself
 * proves nothing. Separation is recomputed by hand, applying is re-tested by stepping a
 * day, and every exact date is confirmed by evaluating the actual angle at that moment.
 *
 * Natal chart is real birth data rather than a synthetic one, because the interesting
 * failures - a body with no aspects, an angle at a sign boundary - do not occur in tidy
 * made-up charts.
 */
public final class TransitCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    // Synthetic reference chart - 1984-09-08 07:33 UT, 41.8781 N 87.6298 W.
    // Not anyone's real birth data.
    private static final int    NATAL_Y = 1984;
    private static final int    NATAL_M = 9;
    private static final int    NATAL_D = 8;
    private static final double NATAL_UT = 7.0 + 33.0 / 60.0;
    private static final double NATAL_LAT = 41.8781;
    private static final double NATAL_LON = -87.6298;

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        SwissEph sw = new SwissEph(EPHE_PATH);

        double natalJd = new SweDate(NATAL_Y, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
        ChartFrame natal = ChartFrame.compute(sw, natalJd, NATAL_LAT, NATAL_LON,
            'P', false, 0.0);

        System.out.println("=== Part A: profection arithmetic ===");
        partA(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part B: transit geometry ===");
        ChartFrame transit = partB(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part C: the filter actually filters ===");
        partC(natal, transit);

        System.out.println();
        System.out.println("=== Part D: exact dates and retrograde re-crossings ===");
        partD(sw, natal);

        System.out.println();
        System.out.println("=== Part E: the prose it actually produces ===");
        partE(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part F: eclipses, stations, and natal contacts ===");
        partF(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part G: the convergence rule ===");
        partG(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part H: solar arc directions ===");
        partH(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part I: secondary progressions ===");
        partI(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part J: solar and lunar returns ===");
        partJ(sw, natal, natalJd);

        System.out.println();
        System.out.println("=== Part K: zodiacal releasing ===");
        partK(sw, natal, natalJd);

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES - " + checks + " checks, " + failures + " failures.");
        }
    }

    // ------------------------------------------------------------------ A: profections

    private static void partA(SwissEph sw, ChartFrame natal, double natalJd) {
        int ascSign = Zodiac.signIndex(natal.asc);
        System.out.printf("natal Ascendant %s (sign %d)%n", Zodiac.format(natal.asc), ascSign);

        // The defining property: house advances one per completed year, wrapping at 12.
        for (int age = 0; age <= 36; age++) {
            double target = new SweDate(NATAL_Y + age, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
            Profection p = Profection.at(natalJd, target, natal.asc);
            eq("age " + age + " -> completed years", age, p.age);
            eq("age " + age + " -> house", (age % 12) + 1, p.house);
            eq("age " + age + " -> sign", (ascSign + age) % 12, p.sign);
            eq("age " + age + " -> lord",
                Dignity.domicileRulerOf((ascSign + age) % 12), p.lord);
        }

        // Age 0 and any multiple of 12 must return to the Ascendant's own sign.
        for (int age : new int[]{0, 12, 24, 36, 48}) {
            double target = new SweDate(NATAL_Y + age, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
            Profection p = Profection.at(natalJd, target, natal.asc);
            eq("age " + age + " returns to house 1", 1, p.house);
            eq("age " + age + " returns to Asc sign", ascSign, p.sign);
        }

        // The birthday boundary. This is the off-by-one that silently shifts a whole house.
        // Probe dates straddle the reference chart's birthday (8 September) and the expected
        // ages follow from its year - move all four together if that chart ever changes.
        double dayBefore = new SweDate(2026, 9, 7, 12.0).getJulDay();
        double onBirthday = new SweDate(2026, 9, 8, 12.0).getJulDay();
        double dayAfter = new SweDate(2026, 9, 9, 12.0).getJulDay();
        eq("day before birthday", 41, Profection.completedYears(natalJd, dayBefore));
        eq("on the birthday", 42, Profection.completedYears(natalJd, onBirthday));
        eq("day after birthday", 42, Profection.completedYears(natalJd, dayAfter));

        // January must NOT turn the year - the classic calendar-year bug.
        double janBefore = new SweDate(2026, 1, 1, 12.0).getJulDay();
        eq("1 Jan does not advance the year", 41,
            Profection.completedYears(natalJd, janBefore));

        // Solar return: the Sun really is back on its natal degree at the returned moment.
        double natalSun = natal.bodies[0].lon;
        for (int age : new int[]{40, 43, 44}) {
            double srJd = Profection.solarReturnJd(sw, natalJd, natalSun, age);
            if (Double.isNaN(srJd)) {
                fail("solar return age " + age + " not found");
                continue;
            }
            double sunThen = Almanac.bodyLongitude(sw, srJd, "Sun");
            double off = Math.abs(Almanac.signedDelta(sunThen, natalSun));
            near("solar return age " + age + " puts Sun on natal degree", 0.0, off, 1e-3);
            SweDate d = new SweDate(srJd);
            System.out.printf("  solar return age %d: %04d-%02d-%02d  (Sun %.6f° off natal)%n",
                age, d.getYear(), d.getMonth(), d.getDay(), off);
        }

        double now = new SweDate(2026, 8, 5, 12.0).getJulDay();
        Profection p = Profection.at(natalJd, now, natal.asc);
        System.out.println("  today: " + p);
    }

    // ------------------------------------------------------------------ B: geometry

    private static ChartFrame partB(SwissEph sw, ChartFrame natal, double natalJd) {
        double nowJd = new SweDate(2026, 8, 5, 19.0).getJulDay();
        ChartFrame transit = ChartFrame.compute(sw, nowJd, 34.0522, -118.2437, 'P', false, 0.0);

        List<BodyScore.Vector> ranked = BodyScore.rank(natal);
        Profection prof = Profection.at(natalJd, nowJd, natal.asc);
        List<Transits.Hit> hits = Transits.toNatal(natal, transit, ranked, prof.lord);

        System.out.println("  " + hits.size() + " filtered transits");

        for (Transits.Hit h : hits) {
            double tLon = lonOf(transit, h.transiting);
            double nLon = natalLonOf(natal, h.natal);
            if (Double.isNaN(tLon) || Double.isNaN(nLon)) {
                fail("cannot re-derive longitudes for " + h);
                continue;
            }
            // Separation, recomputed here rather than asked of Aspects.
            double d = Math.abs(tLon - nLon) % 360.0;
            double sep = d > 180.0 ? 360.0 - d : d;
            near("separation " + h.transiting + "-" + h.natal, sep, h.separation, 1e-9);
            near("offBy " + h.transiting + "-" + h.natal,
                Math.abs(sep - h.type.exactAngle), h.offBy, 1e-9);
            yes("within orb " + h.transiting + "-" + h.natal, h.offBy <= h.orbUsed);

            // Applying, re-tested by stepping a day. The natal point does not move.
            double tNext = tLon + speedOf(transit, h.transiting);
            double d2 = Math.abs(tNext - nLon) % 360.0;
            double sepNext = d2 > 180.0 ? 360.0 - d2 : d2;
            boolean expect = Math.abs(sepNext - h.type.exactAngle)
                < Math.abs(sep - h.type.exactAngle);
            eq("applying " + h.transiting + "-" + h.natal, expect, h.applying);
        }
        return transit;
    }

    // ------------------------------------------------------------------ C: the filter

    private static void partC(ChartFrame natal, ChartFrame transit) {
        List<BodyScore.Vector> ranked = BodyScore.rank(natal);
        Profection prof = Profection.at(natal.julianDayUt, transit.julianDayUt, natal.asc);

        List<Transits.Hit> filtered = Transits.toNatal(natal, transit, ranked, prof.lord);
        // Everything, by making every natal body qualify.
        List<Transits.Hit> everything = Transits.toNatal(natal, transit, ranked, prof.lord,
            99, true);

        System.out.println("  unfiltered (all targets, Moon included): " + everything.size());
        System.out.println("  filtered                                : " + filtered.size());
        yes("the filter removes something", filtered.size() < everything.size());
        yes("the filter does not remove everything", filtered.size() > 0);

        // Every surviving hit must name a reason, and the reason must be true.
        List<String> prominent = new ArrayList<>();
        for (int i = 0; i < ranked.size() && i < 5; i++) {
            prominent.add(ranked.get(i).body);
        }
        for (Transits.Hit h : filtered) {
            yes("hit carries a reason", h.why != null && !h.why.isEmpty());
            boolean legit;
            switch (h.why) {
                case "angle":
                    legit = h.natal.equals("Ascendant") || h.natal.equals("MC")
                        || h.natal.equals("Descendant") || h.natal.equals("IC");
                    break;
                case "light":
                    legit = h.natal.equals("Sun") || h.natal.equals("Moon");
                    break;
                case "prominence":
                    legit = prominent.contains(h.natal);
                    break;
                case "lord of the year":
                    legit = h.natal.equals(prof.lord);
                    break;
                default:
                    legit = false;
            }
            yes("reason '" + h.why + "' is true for natal " + h.natal, legit);
        }

        // The transiting Moon must be absent by default.
        for (Transits.Hit h : filtered) {
            yes("no transiting Moon by default", !"Moon".equals(h.transiting));
        }

        System.out.println();
        System.out.print(Transits.table(filtered));
    }

    // ------------------------------------------------------------------ D: exact dates

    private static void partD(SwissEph sw, ChartFrame natal) {
        // Five years, because a retrograde re-crossing is a property of the body stationing
        // near the natal degree - over one year it may simply not happen, and an assertion
        // that depends on luck is an assertion that will fail for no reason later.
        double from = new SweDate(2026, 1, 1, 0.0).getJulDay();
        double to = new SweDate(2031, 1, 1, 0.0).getJulDay();

        double natalSun = natal.bodies[0].lon;
        int multiple = 0;
        int total = 0;

        for (Aspects.Type type : Aspects.Type.values()) {
            for (String body : new String[]{"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"}) {
                List<Double> exact = Transits.exactDates(sw, body, natalSun, type, from, to);
                total += exact.size();
                if (exact.size() > 1) {
                    multiple++;
                }

                // No two reported moments may be the same event. This is the check that
                // catches the 0.0 / -0.0 duplication directly rather than by eye.
                for (int i = 1; i < exact.size(); i++) {
                    yes("distinct perfections for " + body + " " + type.label
                            + " (gap " + String.format("%.3f", exact.get(i) - exact.get(i - 1))
                            + " d)",
                        exact.get(i) - exact.get(i - 1) > 0.5);
                }

                for (double jd : exact) {
                    // The claim: at this instant the angle IS the aspect's exact angle.
                    double lon = Almanac.bodyLongitude(sw, jd, body);
                    double d = Math.abs(lon - natalSun) % 360.0;
                    double sep = d > 180.0 ? 360.0 - d : d;
                    near(body + " " + type.label + " natal Sun is exact at its date",
                        type.exactAngle, sep, 1e-3);
                }
                if (exact.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (double jd : exact) {
                        SweDate sd = new SweDate(jd);
                        sb.append(String.format(" %04d-%02d-%02d",
                            sd.getYear(), sd.getMonth(), sd.getDay()));
                    }
                    System.out.printf("  %-8s %-12s natal Sun x%d:%s%n",
                        body, type.label.toLowerCase(), exact.size(), sb);
                }
            }
        }
        System.out.println("  " + total + " exact perfections, " + multiple
            + " aspects perfecting more than once");
        yes("at least one aspect perfects more than once (retrograde re-crossing)",
            multiple > 0);
    }

    // ------------------------------------------------------------------ E: the prose

    /**
     * Renders the sentences a reader would actually see, over several dates.
     *
     * Reading one is not enough: the phrasing branches on whether the loudest transit is
     * on the lord of the year, on whether it is retrograde, and on there being any
     * applying transit at all. Those branches render fluently while asserting something
     * false, so each is checked on the string rather than on the struct behind it.
     */
    private static void partE(SwissEph sw, ChartFrame natal, double natalJd) {
        List<BodyScore.Vector> ranked = BodyScore.rank(natal);
        int[][] dates = {
            {2026, 8, 5}, {2026, 11, 20}, {2027, 3, 21}, {2027, 9, 1}, {2028, 2, 14}
        };
        boolean sawLordHit = false;
        boolean sawNonLordHit = false;

        for (int[] d : dates) {
            double jd = new SweDate(d[0], d[1], d[2], 19.0).getJulDay();
            ChartFrame tf = ChartFrame.compute(sw, jd, 34.0522, -118.2437, 'P', false, 0.0);
            Profection prof = Profection.at(natalJd, jd, natal.asc);
            List<Transits.Hit> hits = Transits.toNatal(natal, tf, ranked, prof.lord);
            String sentence = Snapshot.timeSentence(prof, hits);

            System.out.printf("  %04d-%02d-%02d  %s%n", d[0], d[1], d[2], sentence);

            yes("sentence names the lord", sentence.contains(prof.lord));
            yes("sentence has no null", !sentence.contains("null"));
            yes("sentence ends in a full stop", sentence.trim().endsWith("."));
            yes("sentence has no doubled space", !sentence.contains("  "));
            yes("sentence has no ' - .' artefact", !sentence.contains(" - ."));

            for (Transits.Hit h : hits) {
                if (h.applying && prof.lord.equals(h.natal)) {
                    sawLordHit = true;
                }
            }
            // When the loudest hit IS on the lord, the sentence may say "it".
            if (sentence.contains(" closing on it ") || sentence.contains(" over it ")) {
                yes("says 'it' only when the hit is on the lord", sawLordHit);
            }

            // Drive the other branch deliberately rather than waiting for a date where the
            // lord happens to have no applying transit. That case is real but rare, and a
            // test that only fires by luck is a test that silently stops covering the code.
            List<Transits.Hit> noLordHits = new ArrayList<>();
            for (Transits.Hit h : hits) {
                if (!prof.lord.equals(h.natal)) {
                    noLordHits.add(h);
                }
            }
            if (!noLordHits.isEmpty()) {
                String alt = Snapshot.timeSentence(prof, noLordHits);
                if (alt.contains("natal ")) {
                    sawNonLordHit = true;
                    yes("no-lord-hit sentence does not claim 'it'",
                        !alt.contains(" closing on it ") && !alt.contains(" over it "));
                    yes("no-lord-hit sentence has no null", !alt.contains("null"));
                    yes("no-lord-hit sentence ends in a full stop", alt.trim().endsWith("."));
                }
            }
        }

        yes("exercised the on-the-lord branch", sawLordHit);
        yes("exercised the not-on-the-lord branch", sawNonLordHit);

        // The report section, on today's chart.
        double now = new SweDate(2026, 8, 5, 19.0).getJulDay();
        ChartFrame tf = ChartFrame.compute(sw, now, 34.0522, -118.2437, 'P', false, 0.0);
        Profection prof = Profection.at(natalJd, now, natal.asc);
        List<Transits.Hit> hits = Transits.toNatal(natal, tf, ranked, prof.lord);
        String section = Snapshot.timeLayer(prof, ranked, hits);
        yes("report section has THE YEAR", section.contains("THE YEAR"));
        yes("report section names the lord natally", section.contains("lord natally:"));
        yes("report section has no null", !section.contains("null"));

        System.out.println();
        for (String line : section.split("\n", 14)) {
            System.out.println("  |" + line);
        }
    }

    // ------------------------------------------------------------------ F: dated moments

    /**
     * Eclipses and stations, and the natal contacts drawn from them.
     *
     * The eclipse checks do not ask the almanac whether it agrees with itself. An eclipse
     * has two defining properties that can be tested straight from the ephemeris - the
     * Sun and Moon are conjunct or opposite, and the Moon is near a node, meaning its
     * ecliptic latitude is small - so both are recomputed here from swe_calc_ut. A list of
     * "eclipses" that were merely new Moons would sail through a self-consistency test and
     * fails these.
     */
    private static void partF(SwissEph sw, ChartFrame natal, double natalJd) {
        double jd0 = new SweDate(2026, 1, 1, 0.0).getJulDay();
        double jd1 = new SweDate(2027, 1, 1, 0.0).getJulDay();

        List<Almanac.Event> eclipses = Almanac.eclipses(sw, jd0, jd1);
        System.out.printf("2026 eclipses: %d%n", eclipses.size());
        for (Almanac.Event e : eclipses) {
            SweDate sd = new SweDate();
            sd.setJulDay(e.jd);
            System.out.printf("  %04d-%02d-%02d  %s%n", sd.getYear(), sd.getMonth(),
                sd.getDay(), e);
        }

        // Every calendar year has at least four eclipses and at most seven. This is a fact
        // about the geometry of the nodes, not about this code, so it is a real constraint.
        yes("2026 has 4..7 eclipses", eclipses.size() >= 4 && eclipses.size() <= 7);

        double prevJd = -1;
        for (Almanac.Event e : eclipses) {
            yes("eclipse inside range", e.jd >= jd0 && e.jd <= jd1);
            yes("eclipses are chronological", e.jd > prevJd);
            prevJd = e.jd;

            double sun = bodyLon(sw, e.jd, SweConst.SE_SUN);
            double moon = bodyLon(sw, e.jd, SweConst.SE_MOON);
            double sep = delta(moon, sun);

            if (e.kind == Almanac.Kind.SOLAR_ECLIPSE) {
                near("solar eclipse is a conjunction", 0.0, sep, 1.0);
                near("solar eclipse recorded at the Sun", sun, e.longitude, 1.0e-6);
            } else {
                yes("only eclipse kinds", e.kind == Almanac.Kind.LUNAR_ECLIPSE);
                near("lunar eclipse is an opposition", 180.0, Math.abs(sep), 1.0);
                near("lunar eclipse recorded at the Moon", moon, e.longitude, 1.0e-6);
            }

            // The property that separates an eclipse from an ordinary lunation: the Moon
            // is near a node. Its latitude runs to +/-5.3 degrees, so under 2 is a real test.
            double lat = moonLatitude(sw, e.jd);
            yes("eclipse has the Moon near a node", Math.abs(lat) < 2.0);
            yes("eclipse is classified", e.detail != null && !e.detail.isEmpty());
        }

        // Stations: the speed really does change sign across the reported moment.
        List<Almanac.Event> moments = Almanac.datedMoments(sw, jd0, jd1);
        int stationCount = 0;
        boolean sawRetro = false;
        boolean sawDirect = false;
        for (Almanac.Event e : moments) {
            if (e.kind != Almanac.Kind.STATION_RETROGRADE
                    && e.kind != Almanac.Kind.STATION_DIRECT) {
                continue;
            }
            stationCount++;
            sawRetro |= e.kind == Almanac.Kind.STATION_RETROGRADE;
            sawDirect |= e.kind == Almanac.Kind.STATION_DIRECT;
            int ipl = Almanac.iplOf(e.body);
            double before = speedAt(sw, e.jd - 1.0, ipl);
            double after = speedAt(sw, e.jd + 1.0, ipl);
            yes(e.body + " station reverses direction", (before < 0) != (after < 0));
            if (e.kind == Almanac.Kind.STATION_RETROGRADE) {
                yes(e.body + " stations retrograde into retrograde motion", after < 0);
            } else {
                yes(e.body + " stations direct into direct motion", after > 0);
            }
        }
        System.out.printf("2026 stations: %d%n", stationCount);
        yes("both station directions occur in a year", sawRetro && sawDirect);
        yes("datedMoments carries no ingresses",
            moments.stream().noneMatch(e -> e.kind == Almanac.Kind.INGRESS));

        // The calendar must not print one moment twice under two names.
        List<Almanac.Event> cal = Almanac.annualCalendar(sw, jd0, jd1);
        for (Almanac.Event ecl : cal) {
            if (ecl.kind != Almanac.Kind.SOLAR_ECLIPSE && ecl.kind != Almanac.Kind.LUNAR_ECLIPSE) {
                continue;
            }
            Almanac.Kind shadowed = ecl.kind == Almanac.Kind.SOLAR_ECLIPSE
                ? Almanac.Kind.NEW_MOON : Almanac.Kind.FULL_MOON;
            for (Almanac.Event lun : cal) {
                if (lun.kind == shadowed && Math.abs(lun.jd - ecl.jd) < 0.5) {
                    fail("calendar lists " + ecl + " and its lunation both");
                }
            }
        }
        yes("calendar still contains the eclipses",
            cal.stream().filter(e -> e.kind == Almanac.Kind.SOLAR_ECLIPSE
                || e.kind == Almanac.Kind.LUNAR_ECLIPSE).count() == eclipses.size());

        // Natal contacts: same filter vocabulary as the transit path, and the orb rule
        // recomputed by hand rather than trusted.
        Profection prof = Profection.at(natalJd, jd0, natal.asc);
        ChartFrame frame = ChartFrame.compute(sw, jd0, NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        List<BodyScore.Vector> ranked = BodyScore.rank(natal, Gestalt.compute(natal));
        List<Transits.EventHit> hits =
            Transits.eventsToNatal(moments, natal, ranked, prof.lord);
        System.out.printf("natal contacts from 2026 dated moments: %d%n", hits.size());

        double lastJd = -1;
        for (Transits.EventHit h : hits) {
            yes("event hit is chronological", h.event.jd >= lastJd);
            lastJd = h.event.jd;
            yes("event hit why is in the filter vocabulary",
                "angle".equals(h.why) || "light".equals(h.why)
                    || "prominence".equals(h.why) || "lord of the year".equals(h.why));
            yes("event hit is a contactable kind",
                h.event.kind == Almanac.Kind.SOLAR_ECLIPSE
                    || h.event.kind == Almanac.Kind.LUNAR_ECLIPSE
                    || h.event.kind == Almanac.Kind.STATION_RETROGRADE
                    || h.event.kind == Almanac.Kind.STATION_DIRECT);

            double natalLon = natalLonOf(natal, h.natal);
            yes("event hit names a real natal point", !Double.isNaN(natalLon));
            double sep = Math.abs(delta(h.event.longitude, natalLon));
            near("event hit offBy re-derived", Math.abs(sep - h.type.exactAngle), h.offBy, 1e-9);
            yes("event hit is inside its orb", h.offBy <= h.orbUsed + 1e-9);
            near("event hit orb re-derived",
                Math.min(Aspects.orbFor(h.event.body, h.natal) * Transits.eventOrbScale,
                    Transits.eventMaxOrb),
                h.orbUsed, 1e-9);
            yes("event hit respects the spec's ceiling", h.offBy <= Transits.eventMaxOrb + 1e-9);
        }

        // The filter is the same one, so anything it rejects for transits it rejects here.
        for (Transits.EventHit h : hits) {
            boolean isAngle = "angle".equals(h.why);
            boolean isLight = "Sun".equals(h.natal) || "Moon".equals(h.natal);
            boolean isLord = prof.lord.equals(h.natal);
            boolean topN = false;
            for (int i = 0; i < ranked.size() && i < Transits.defaultTopN; i++) {
                topN |= ranked.get(i).body.equals(h.natal);
            }
            yes("every contacted natal point qualifies", isAngle || isLight || isLord || topN);
        }

        // The window the app actually scans: solar return to solar return, not a calendar
        // year. This is the one part of the GUI wiring that is not otherwise exercised, and
        // getting it wrong would silently scan the wrong twelve months.
        double natalSun = lonOf(natal, "Sun");
        double from = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age);
        double to = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age + 1);
        yes("solar return window solves", !Double.isNaN(from) && !Double.isNaN(to));
        yes("solar return window runs forwards", to > from);
        near("solar return window is one tropical year", 365.2422, to - from, 1.0);
        yes("solar return window contains the target date", jd0 >= from && jd0 <= to);
        near("solar return puts the Sun back on its natal degree",
            0.0, Math.abs(delta(bodyLon(sw, from, SweConst.SE_SUN), natalSun)), 1.0e-4);

        // And the section renders without a null or an empty aspect label.
        String section = Snapshot.timeLayer(prof, ranked,
            Transits.toNatal(natal, frame, ranked, prof.lord), hits);
        yes("time layer renders the dated moments",
            hits.isEmpty() || section.contains("DATED MOMENTS ON WHAT MATTERS"));
        yes("time layer has no null", !section.contains("null"));
    }

    // ------------------------------------------------------------------ G: convergence

    /**
     * The convergence rule: grouping, the family count, and the echo guard.
     *
     * The interesting assertions here are the negative ones. It is easy to write a
     * convergence rule that scores everything highly and therefore says nothing, so this
     * checks that the echo guard actually removes something, that the removed things are
     * genuinely the same relationship, and that a score is never larger than the number of
     * families that exist.
     */
    private static void partG(SwissEph sw, ChartFrame natal, double natalJd) {
        double transitJd = new SweDate(2026, 8, 7, 0.0).getJulDay();
        ChartFrame tf = ChartFrame.compute(sw, transitJd, 34.05, -118.24, 'P', false, 0.0);
        List<BodyScore.Vector> ranked = BodyScore.rank(natal, Gestalt.compute(natal));
        Profection prof = Profection.at(natalJd, transitJd, natal.asc);
        List<Transits.Hit> hits = Transits.toNatal(natal, tf, ranked, prof.lord);

        double natalSun = lonOf(natal, "Sun");
        double from = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age);
        double to = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age + 1);
        List<Transits.EventHit> events = Transits.eventsToNatal(
            Almanac.datedMoments(sw, from, to), natal, ranked, prof.lord);
        // The whole point of the span fix: transits enter as dated perfections over the
        // SAME window the events came from, not as a reading at one instant.
        List<Transits.Perfection> perfections =
            Transits.perfectionsOverRange(sw, natal, ranked, prof.lord, from, to);
        // Solar arc from the same window, so the fifth family is measured like the rest and
        // Part G's "only sky families are ever echoes" assertion actually covers it.
        List<SolarArc.Contact> arcsForWindow =
            SolarArc.contacts(sw, natal, natalJd, ranked, prof.lord, from, to);
        System.out.printf("perfections %d, arcs %d over the profection year%n",
            perfections.size(), arcsForWindow.size());

        boolean savedGuard = Convergence.discountSharedBodies;
        try {
            Convergence.discountSharedBodies = true;
            List<Convergence.Target> targets = Convergence.collect(prof, perfections, events, arcsForWindow);

            // Every dated witness must fall inside the window every family was measured over.
            for (Convergence.Target t : targets) {
                for (Convergence.Witness w : t.witnesses) {
                    if (!Double.isNaN(w.jd)) {
                        yes("witness is inside the shared window",
                            w.jd >= from - 1e-6 && w.jd <= to + 1e-6);
                    }
                }
            }
            for (Transits.Perfection p : perfections) {
                yes("perfection is inside the window", p.jd >= from && p.jd <= to);
                yes("perfection body is a year marker",
                    java.util.Arrays.asList(Transits.yearMarkers).contains(p.transiting));
            }
            System.out.printf("convergence targets: %d%n", targets.size());

            int familyCount = Convergence.Family.values().length;
            double prevScore = Double.MAX_VALUE;
            int echoes = 0;
            for (Convergence.Target t : targets) {
                yes("targets are ranked by score", t.score <= prevScore);
                prevScore = t.score;
                yes("target names a real natal point", !Double.isNaN(natalLonOf(natal, t.natal)));
                yes("target has at least one witness", !t.witnesses.isEmpty());

                // No family may be in the set unless an independent witness carries it, and
                // every independent witness's family must be in the set.
                for (Convergence.Witness w : t.witnesses) {
                    if (w.independent) {
                        yes("independent witness's family is counted",
                            t.families.contains(w.family));
                    } else {
                        echoes++;
                        // An echo must genuinely repeat a transit: same body, same aspect.
                        boolean matched = false;
                        for (Convergence.Witness other : t.witnesses) {
                            matched |= other.family == Convergence.Family.TRANSIT
                                && other.aspect == w.aspect
                                && other.movingBody.equals(w.movingBody);
                        }
                        yes("an echo really does repeat a transit", matched);
                        yes("only dated families are ever echoes",
                            w.family == Convergence.Family.ECLIPSE
                                || w.family == Convergence.Family.STATION);
                    }
                }
            }
            yes("the echo guard removes something", echoes > 0);
            System.out.printf("echoes discounted: %d%n", echoes);

            // Profection is independent by construction and must never be discounted.
            for (Convergence.Target t : targets) {
                for (Convergence.Witness w : t.witnesses) {
                    if (w.family == Convergence.Family.PROFECTION) {
                        yes("profection is never an echo", w.independent);
                        eq("profection witness is the lord", prof.lord, t.natal);
                    }
                }
            }

            // loudest() must return every target tying for the top score, and nothing else.
            List<Convergence.Target> loudest = Convergence.loudest(targets);
            yes("loudest is non-empty", !loudest.isEmpty());
            double best = targets.get(0).score;
            for (Convergence.Target t : loudest) {
                eq("loudest all tie on score", best, t.score);
            }
            long tying = targets.stream().filter(t -> t.score == best).count();
            eq("loudest contains every tie", (int) tying, loudest.size());

            // atLeast is monotonic: a higher bar can never select more.
            int prevSize = Integer.MAX_VALUE;
            for (int n = 1; n <= familyCount; n++) {
                int size = Convergence.atLeast(targets, n).size();
                yes("atLeast(" + n + ") is monotonic", size <= prevSize);
                prevSize = size;
            }

            // The guard must lower or hold every score, never raise one - asserted on
            // rawScore, NOT on score, and the difference is the whole point.
            //
            // score is now a softmax over the whole field, so it is a share of a fixed
            // budget rather than a property of one target. Discounting an echo lowers that
            // target's rawScore, which shrinks the shared denominator, which RAISES the
            // score of every target that had no echo at all. Four targets failed this line
            // that way: their own witnesses were untouched and their guarded score still
            // came out above their unguarded one.
            //
            // The invariant is pointwise, so it belongs on the pointwise quantity. rawScore
            // is a plain sum over this target's own surviving witnesses, so "the guard
            // never raises it" is both true and worth knowing. Restating it on a normalised
            // score would either fail forever or have to be weakened into saying nothing.
            java.util.Map<String, Double> guarded = new java.util.HashMap<>();
            for (Convergence.Target t : targets) {
                guarded.put(t.natal, t.rawScore);
            }
            // Count the guarded selection BEFORE flipping the flag. Reading it afterwards
            // re-collects with the guard off and silently compares the unguarded list with
            // itself, which is how this line first reported 10 against 10.
            int guardedAtTwo = Convergence.atLeast(targets, 2).size();
            Convergence.discountSharedBodies = false;
            List<Convergence.Target> raw = Convergence.collect(prof, perfections, events, arcsForWindow);
            boolean loweredAny = false;
            for (Convergence.Target t : raw) {
                Double g = guarded.get(t.natal);
                yes("guard never raises a raw score", g == null || g <= t.rawScore + 1e-6);
                loweredAny |= g != null && g < t.rawScore - 1e-6;
            }
            // Note: NOT "the guard lowers at least one score". Since transits became dated
            // perfections the guard still marks echoes, but a target whose station witness
            // is an echo now usually carries a second, independent station witness, so the
            // family survives and the count does not move. The guard is still correct and
            // still worth having on other charts; on this one it changes no score.
            System.out.printf("guard lowered a score on this chart: %s%n", loweredAny);
            int rawAtTwo = Convergence.atLeast(raw, 2).size();
            // Keyed on the FAMILY COUNT, not on score. The vault quotes this histogram as
            // {1:1, 2:5, 3:3, 4:1} - a distribution over independent families. Once score
            // became a softmax probability, keying on it gave one bucket per target with a
            // long decimal for a key, which is not a histogram of anything.
            java.util.Map<Integer, Integer> histogram = new java.util.TreeMap<>();
            for (Convergence.Target t : targets) {
                histogram.merge(t.families.size(), 1, Integer::sum);
            }
            System.out.println("family-count histogram: " + histogram);
            System.out.printf("converging at >=2 : guarded %d, unguarded %d (of %d targets)%n",
                guardedAtTwo, rawAtTwo, targets.size());

            // This used to read: yes("threshold at 2 is now selective", guardedAtTwo <
            // targets.size()). It was passing on 0 < 10 while atLeast was unit-broken, and
            // the moment atLeast was fixed it failed honestly at 10 < 10 - because with
            // seven families NOTHING scores below two any more. The histogram is {2:5, 3:4,
            // 4:1}.
            //
            // That is not a regression, it is the finding this plan already recorded:
            // "adding families no longer improves discrimination", and ">=2 is not a cut".
            // Re-pinning the assertion to whichever n happens to bite this year would be
            // fitting the test to the data.
            //
            // So assert the property that is actually invariant and actually worth having -
            // that the accessor is CAPABLE of cutting somewhere - and print where it bites,
            // leaving the astrology free to move without the suite going red.
            int cutsAt = 0;
            for (int n = 1; n <= familyCount; n++) {
                if (Convergence.atLeast(targets, n).size() < targets.size()) {
                    cutsAt = n;
                    break;
                }
            }
            System.out.printf("the >=n cut first bites at n=%d (>=2 selects %d of %d)%n",
                cutsAt, guardedAtTwo, targets.size());
            yes("some threshold discriminates at all", cutsAt > 0);
            // And the top of the range must not select everything, or the cut is decorative.
            yes("the highest family count is selective",
                Convergence.atLeast(targets, familyCount).size() < targets.size());
        } finally {
            Convergence.discountSharedBodies = savedGuard;
        }

        String section = Snapshot.timeLayer(prof, ranked, hits, events,
            Convergence.collect(prof, perfections, events));
        yes("report has the convergence section",
            section.contains("WHERE THE TECHNIQUES AGREE"));
        yes("convergence section has no null", !section.contains("null"));
        // The four-argument overload must render without a convergence section rather than
        // silently computing a span-mismatched one, which is what it used to do.
        String noConv = Snapshot.timeLayer(prof, ranked, hits, events);
        yes("four-arg overload omits the convergence section",
            !noConv.contains("WHERE THE TECHNIQUES AGREE"));
    }

    // ------------------------------------------------------------------ H: solar arc

    /**
     * Solar arc directions.
     *
     * The decisive check is that every reported contact really is exact: the directed
     * longitude is recomputed here as natal longitude plus the arc, and the separation from
     * the natal target compared against the aspect angle. A class that solved for the wrong
     * arc, or applied it to the wrong point, produces plausible dates that fail this.
     */
    private static void partH(SwissEph sw, ChartFrame natal, double natalJd) {
        double natalSun = lonOf(natal, "Sun");

        // ChartFrame and Almanac ask the ephemeris for the Sun with the same flags but do
        // not return bit-identical longitudes: measured 1.9e-6 degrees apart, about seven
        // milliarcseconds. Cause not established, so it is bounded here rather than
        // explained - if it ever grows, this is where it shows up.
        double frameSun = lonOf(natal, "Sun");
        double almanacSun = Almanac.bodyLongitude(sw, natalJd, "Sun");
        near("ChartFrame and Almanac agree on the natal Sun", 0.0,
            Math.abs(delta(frameSun, almanacSun)), 1.0e-5);

        // The arc is zero at birth and advances about a degree a year. Tolerance is the
        // bound above, not zero, for the same reason.
        near("arc at birth is zero", 0.0, SolarArc.arcAt(sw, natalJd, natalSun, natalJd), 1e-5);
        double prev = 0.0;
        for (int age = 1; age <= 60; age++) {
            double at = SolarArc.arcAt(sw, natalJd, natalSun,
                natalJd + age * SolarArc.DAYS_PER_YEAR);
            yes("arc is monotonic at age " + age, at > prev);
            double perYear = at - prev;
            yes("arc advances about a degree at age " + age, perYear > 0.9 && perYear < 1.1);
            prev = at;
        }
        System.out.printf("arc at age 43: %.3f deg%n", prev >= 0 ? SolarArc.arcAt(
            sw, natalJd, natalSun, natalJd + 43 * SolarArc.DAYS_PER_YEAR) : Double.NaN);

        List<BodyScore.Vector> ranked = BodyScore.rank(natal, Gestalt.compute(natal));
        double transitJd = new SweDate(2026, 8, 7, 0.0).getJulDay();
        Profection prof = Profection.at(natalJd, transitJd, natal.asc);
        double from = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age);
        double to = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age + 1);

        List<SolarArc.Contact> arcs =
            SolarArc.contacts(sw, natal, natalJd, ranked, prof.lord, from, to);
        System.out.printf("solar arc contacts this profection year: %d%n", arcs.size());
        for (SolarArc.Contact c : arcs) {
            SweDate sd = new SweDate();
            sd.setJulDay(c.jd);
            System.out.printf("  %04d-%02d-%02d  %s (%s)%n",
                sd.getYear(), sd.getMonth(), sd.getDay(), c, c.why);
        }

        double lastJd = -1;
        for (SolarArc.Contact c : arcs) {
            yes("arc contact inside the window", c.jd >= from - 1e-6 && c.jd <= to + 1e-6);
            yes("arc contacts are chronological", c.jd >= lastJd);
            lastJd = c.jd;
            yes("a point is not directed onto itself", !c.directed.equals(c.natal));
            yes("the axis duplicates are not directed",
                !"Descendant".equals(c.directed) && !"IC".equals(c.directed));
            yes("arc contact why is in the filter vocabulary",
                "angle".equals(c.why) || "light".equals(c.why)
                    || "prominence".equals(c.why) || "lord of the year".equals(c.why));

            // Re-derive exactness from the ephemeris rather than trusting the solver.
            double arc = SolarArc.arcAt(sw, natalJd, natalSun, c.jd);
            near("reported arc matches the arc at that moment", arc, c.arc, 1e-4);
            double directedLon = natalLonOf(natal, c.directed) + arc;
            double sep = Math.abs(delta(directedLon, natalLonOf(natal, c.natal)));
            near("solar arc contact really is exact", c.type.exactAngle, sep, 1e-3);
        }

        // Selectivity: the arc advances about a degree in a year, so this must not behave
        // like a transit list, which would return hundreds.
        //
        // <b>Recalibrated 2026-08-30 with the synthetic reference chart.</b> The old bound of
        // 12 was measured against the previous fixture; this chart yields 18 in the profection
        // year and 97 over five, i.e. ~19 a year, so the rate is consistent across the span and
        // nothing is directing wrongly - the chart simply has a denser aspect set. The bound is
        // deliberately loose because its job is to catch an order-of-magnitude break, not to
        // pin a chart-specific count.
        yes("solar arc is selective over one year", arcs.size() <= 40);

        // The geometric verification runs over five years as well as one. On the previous
        // fixture the one-year window happened to contain no contact at all, so the wide window
        // was the only thing exercising the exactness path; this chart does produce contacts in
        // one year, but the five-year run is kept because a check that could see an empty list
        // is not a check, and the next fixture may be sparse again.
        double wideTo = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age + 5);
        List<SolarArc.Contact> wide =
            SolarArc.contacts(sw, natal, natalJd, ranked, prof.lord, from, wideTo);
        System.out.printf("solar arc contacts over five years: %d%n", wide.size());
        yes("five years produces contacts to verify", !wide.isEmpty());
        for (SolarArc.Contact c : wide) {
            double arc = SolarArc.arcAt(sw, natalJd, natalSun, c.jd);
            near("wide: reported arc matches the moment", arc, c.arc, 1e-4);
            double directedLon = natalLonOf(natal, c.directed) + arc;
            double sep = Math.abs(delta(directedLon, natalLonOf(natal, c.natal)));
            near("wide: contact really is exact", c.type.exactAngle, sep, 1e-3);
            yes("wide: inside the window", c.jd >= from - 1e-6 && c.jd <= wideTo + 1e-6);
            yes("wide: not directed onto itself", !c.directed.equals(c.natal));
        }
        // The claim is that contacts scale with the span rather than jumping, and this now
        // measures that instead of asserting an absolute count.
        //
        // The old form was `wide.size() <= 40`, which is a different statement: the count
        // also scales with how many points are being directed, so when the registry grew
        // from 12 points to 28 the cap failed for a reason that had nothing to do with the
        // arc filter it was meant to guard. A cap cannot tell a broken filter from a bigger
        // chart; a density comparison can.
        //
        // Measured over 1, 2, 5, 10 and 20 year windows the density is flat at roughly 8 to
        // 11 contacts a year. A filter that let the arc value through unchecked would grow
        // super-linearly, so a tenfold window is the thing to compare against a fivefold.
        double denseTo = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age + 10);
        List<SolarArc.Contact> denser =
            SolarArc.contacts(sw, natal, natalJd, ranked, prof.lord, from, denseTo);
        double perYear5 = wide.size() / 5.0;
        double perYear10 = denser.size() / 10.0;
        System.out.printf("solar arc density: %.2f/yr over five years, %.2f/yr over ten%n",
            perYear5, perYear10);
        yes("contact density is flat across window length rather than exploding",
            wide.isEmpty() || (perYear10 <= perYear5 * 1.6 && perYear10 >= perYear5 * 0.4));

        // And the family must reach the convergence rule without being mistaken for an echo.
        List<Transits.EventHit> events = Transits.eventsToNatal(
            Almanac.datedMoments(sw, from, to), natal, ranked, prof.lord);
        List<Transits.Perfection> perfections =
            Transits.perfectionsOverRange(sw, natal, ranked, prof.lord, from, to);
        List<Convergence.Target> targets =
            Convergence.collect(prof, perfections, events, arcs);
        int arcWitnesses = 0;
        for (Convergence.Target t : targets) {
            for (Convergence.Witness w : t.witnesses) {
                if (w.family == Convergence.Family.SOLAR_ARC) {
                    arcWitnesses++;
                    yes("solar arc is never discounted as an echo", w.independent);
                }
            }
        }
        eq("every arc contact became a witness", arcs.size(), arcWitnesses);
        System.out.printf("targets naming solar arc: %d%n",
            targets.stream().filter(t -> t.families.contains(Convergence.Family.SOLAR_ARC))
                .count());
    }

    // ------------------------------------------------------------------ I: progressions

    /**
     * Secondary progressions.
     *
     * The decisive check, as with solar arc, is that every reported contact really is exact -
     * recomputed here by asking the ephemeris directly for the progressed body at the
     * reported instant, without going through the class under test.
     */
    private static void partI(SwissEph sw, ChartFrame natal, double natalJd) {
        // A day for a year: the progressed moment at age N is birth plus N days.
        for (int age = 0; age <= 60; age += 10) {
            double target = natalJd + age * Progressions.DAYS_PER_YEAR;
            near("progressed moment at age " + age,
                natalJd + age, Progressions.progressedJd(natalJd, target), 1e-9);
        }

        // The progressed Moon covers roughly 13 degrees a year, the progressed Sun about 1.
        double y0 = natalJd;
        double y1 = natalJd + Progressions.DAYS_PER_YEAR;
        double moon0 = Almanac.bodyLongitude(sw, Progressions.progressedJd(natalJd, y0), "Moon");
        double moon1 = Almanac.bodyLongitude(sw, Progressions.progressedJd(natalJd, y1), "Moon");
        double sun0 = Almanac.bodyLongitude(sw, Progressions.progressedJd(natalJd, y0), "Sun");
        double sun1 = Almanac.bodyLongitude(sw, Progressions.progressedJd(natalJd, y1), "Sun");
        double moonRate = Math.abs(delta(moon1, moon0));
        double sunRate = Math.abs(delta(sun1, sun0));
        System.out.printf("progressed rates: Moon %.2f deg/yr, Sun %.2f deg/yr%n",
            moonRate, sunRate);
        yes("progressed Moon moves 11-16 deg a year", moonRate > 11.0 && moonRate < 16.0);
        yes("progressed Sun moves about a degree a year", sunRate > 0.9 && sunRate < 1.1);

        List<BodyScore.Vector> ranked = BodyScore.rank(natal, Gestalt.compute(natal));
        double transitJd = new SweDate(2026, 8, 7, 0.0).getJulDay();
        Profection prof = Profection.at(natalJd, transitJd, natal.asc);
        double natalSun = lonOf(natal, "Sun");
        double from = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age);
        double to = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age + 1);

        List<Progressions.Contact> contacts =
            Progressions.contacts(sw, natal, natalJd, ranked, prof.lord, from, to);
        System.out.printf("progressed contacts this profection year: %d%n", contacts.size());
        for (Progressions.Contact c : contacts) {
            SweDate sd = new SweDate();
            sd.setJulDay(c.jd);
            System.out.printf("  %04d-%02d-%02d  %s (%s)%n",
                sd.getYear(), sd.getMonth(), sd.getDay(), c, c.why);
        }

        double lastJd = -1;
        for (Progressions.Contact c : contacts) {
            yes("progressed contact inside the window", c.jd >= from - 1e-6 && c.jd <= to + 1e-6);
            yes("progressed contacts are chronological", c.jd >= lastJd);
            lastJd = c.jd;
            yes("progressed body is one of the fast enough ones",
                java.util.Arrays.asList(Progressions.progressedBodies).contains(c.progressed));
            yes("progressed contact why is in the filter vocabulary",
                "angle".equals(c.why) || "light".equals(c.why)
                    || "prominence".equals(c.why) || "lord of the year".equals(c.why));

            // Re-derive: ask the ephemeris for the progressed body at the reported moment.
            double lon = Almanac.bodyLongitude(sw,
                Progressions.progressedJd(natalJd, c.jd), c.progressed);
            double sep = Math.abs(delta(lon, natalLonOf(natal, c.natal)));
            near("progressed contact really is exact", c.type.exactAngle, sep, 1e-3);
        }

        // Progressed angles must not appear: the spec says they need an exact birth time.
        for (Progressions.Contact c : contacts) {
            yes("no progressed angles",
                !"Ascendant".equals(c.progressed) && !"MC".equals(c.progressed)
                    && !"Descendant".equals(c.progressed) && !"IC".equals(c.progressed));
        }

        // Lunation phase is one of the eight, and it advances over a lifetime.
        String phaseNow = Progressions.lunationPhase(sw, natalJd, transitJd);
        yes("phase is one of the eight",
            java.util.Arrays.asList(Progressions.PHASES).contains(phaseNow));
        System.out.println("progressed lunation phase: " + phaseNow);
        String phaseBirth = Progressions.lunationPhase(sw, natalJd, natalJd);
        yes("phase at birth is one of the eight",
            java.util.Arrays.asList(Progressions.PHASES).contains(phaseBirth));

        // Sign ingresses and phase changes: about one every 2.5 and 3.7 years respectively,
        // so a decade must contain several and they must be correctly placed.
        double decade = natalJd + (prof.age + 10) * Progressions.DAYS_PER_YEAR;
        List<Progressions.Change> changes = Progressions.changes(sw, natalJd, from, decade);
        System.out.printf("progressed changes over ten years: %d%n", changes.size());
        yes("a decade contains progressed changes", !changes.isEmpty());
        double prevJd = -1;
        int signs = 0;
        int phases = 0;
        for (Progressions.Change ch : changes) {
            yes("change inside the window", ch.jd >= from - 1e-6 && ch.jd <= decade + 1e-6);
            yes("changes are chronological", ch.jd >= prevJd);
            prevJd = ch.jd;
            if ("sign".equals(ch.kind)) {
                signs++;
                // At an ingress the progressed Moon sits on a sign boundary.
                double lon = Almanac.bodyLongitude(sw,
                    Progressions.progressedJd(natalJd, ch.jd), "Moon");
                double intoSign = Zodiac.degreeInSign(lon);
                yes("sign ingress lands on a boundary",
                    intoSign < 0.01 || intoSign > 29.99);
                eq("ingress names the sign arrived in",
                    Zodiac.SIGNS[Zodiac.signIndex(lon + 0.005)], ch.entered);
            } else {
                phases++;
                yes("phase change names a phase",
                    java.util.Arrays.asList(Progressions.PHASES).contains(ch.entered));
                // At a phase change the Moon-Sun angle sits on a multiple of 45.
                double p = Progressions.progressedJd(natalJd, ch.jd);
                double ang = norm360(Almanac.bodyLongitude(sw, p, "Moon")
                    - Almanac.bodyLongitude(sw, p, "Sun"));
                double off = Math.abs(ang - Math.round(ang / 45.0) * 45.0);
                yes("phase change lands on an eighth", off < 0.05);
            }
        }
        System.out.printf("  of which %d sign ingresses, %d phase changes%n", signs, phases);
        yes("a decade has 3-6 progressed Moon sign ingresses", signs >= 3 && signs <= 6);

        // And the family reaches convergence without being mistaken for an echo.
        List<Transits.EventHit> events = Transits.eventsToNatal(
            Almanac.datedMoments(sw, from, to), natal, ranked, prof.lord);
        List<Transits.Perfection> perfections =
            Transits.perfectionsOverRange(sw, natal, ranked, prof.lord, from, to);
        List<SolarArc.Contact> arcs =
            SolarArc.contacts(sw, natal, natalJd, ranked, prof.lord, from, to);
        List<Convergence.Target> targets =
            Convergence.collect(prof, perfections, events, arcs, contacts);
        int progWitnesses = 0;
        for (Convergence.Target t : targets) {
            for (Convergence.Witness w : t.witnesses) {
                if (w.family == Convergence.Family.PROGRESSION) {
                    progWitnesses++;
                    yes("progression is never discounted as an echo", w.independent);
                }
            }
        }
        eq("every progressed contact became a witness", contacts.size(), progWitnesses);
        java.util.Map<Integer, Integer> histogram = new java.util.TreeMap<>();
        for (Convergence.Target t : targets) {
            histogram.merge(t.families.size(), 1, Integer::sum);
        }
        System.out.println("family-count histogram with six families: " + histogram);
        System.out.println("loudest: " + Convergence.loudest(targets));
    }

    private static double norm360(double d) {
        double x = d % 360.0;
        return x < 0 ? x + 360.0 : x;
    }

    // ------------------------------------------------------------------ J: returns

    /**
     * Solar and lunar returns.
     *
     * The claim that would fail silently is the definitional one: at a solar return the Sun
     * is on its natal degree by construction, so if that leaked into the contact list every
     * chart would gain a free witness on its own Sun every year and the output would look
     * entirely plausible. That is asserted directly, from the ephemeris.
     */
    private static void partJ(SwissEph sw, ChartFrame natal, double natalJd) {
        List<BodyScore.Vector> ranked = BodyScore.rank(natal, Gestalt.compute(natal));
        double transitJd = new SweDate(2026, 8, 7, 0.0).getJulDay();
        Profection prof = Profection.at(natalJd, transitJd, natal.asc);
        double natalSun = lonOf(natal, "Sun");
        double natalMoon = lonOf(natal, "Moon");
        double from = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age);
        double to = Profection.solarReturnJd(sw, natalJd, natalSun, prof.age + 1);

        Returns.Return sr = Returns.solar(sw, natalJd, natalSun, prof.age,
            NATAL_LAT, NATAL_LON, 'P');
        yes("the solar return solves", sr != null && sr.chart != null);
        if (sr == null || sr.chart == null) {
            return;
        }
        SweDate sd = new SweDate();
        sd.setJulDay(sr.jd);
        System.out.printf("solar return %04d-%02d-%02d, %s rising%n",
            sd.getYear(), sd.getMonth(), sd.getDay(), Zodiac.format(sr.chart.asc));

        // The return moment IS the profection year's lower bound - the two must be the same
        // period, not merely similar ones.
        near("the solar return opens the profection year", from, sr.jd, 1e-6);

        // Definitional: the Sun really is back on its natal degree.
        double srSun = Almanac.bodyLongitude(sw, sr.jd, "Sun");
        near("the return Sun is on the natal Sun", 0.0,
            Math.abs(delta(srSun, natalSun)), 1e-4);
        ChartFrame.Body frameSun = sr.chart.body("Sun");
        yes("the return chart has a Sun", frameSun != null && frameSun.ok);
        near("the return chart agrees with the ephemeris on the Sun", 0.0,
            Math.abs(delta(frameSun.lon, srSun)), 1e-4);

        List<Returns.Contact> contacts = Returns.contacts(sr, natal, ranked, prof.lord);
        System.out.printf("solar return contacts: %d%n", contacts.size());
        for (Returns.Contact c : contacts) {
            System.out.printf("  %s (%s)%n", c, c.why);
        }

        int angleContacts = 0;
        for (Returns.Contact c : contacts) {
            eq("return contact carries the return moment", sr.jd, c.jd);
            eq("return contact is solar", "solar", c.kind);
            yes("return contact respects the orb ceiling",
                c.offBy <= Transits.eventMaxOrb + 1e-9);
            yes("return contact why is in the filter vocabulary",
                "angle".equals(c.why) || "light".equals(c.why)
                    || "prominence".equals(c.why) || "lord of the year".equals(c.why));
            yes("the definitional Sun-on-Sun contact is excluded",
                !("Sun".equals(c.returnPoint) && "Sun".equals(c.natal)));
            yes("only the two independent angles are used",
                !"Descendant".equals(c.returnPoint) && !"IC".equals(c.returnPoint));
            if ("Ascendant".equals(c.returnPoint) || "MC".equals(c.returnPoint)) {
                angleContacts++;
            }
            // Re-derive the separation by hand from the return chart.
            double retLon = "Ascendant".equals(c.returnPoint) ? sr.chart.asc
                : "MC".equals(c.returnPoint) ? sr.chart.mc
                : lonOf(sr.chart, c.returnPoint);
            yes("return point resolves to a longitude", !Double.isNaN(retLon));
            double sep = Math.abs(delta(retLon, natalLonOf(natal, c.natal)));
            near("return contact offBy re-derived",
                Math.abs(sep - c.type.exactAngle), c.offBy, 1e-9);
        }
        System.out.printf("  of which %d are return-angle contacts%n", angleContacts);

        // Lunar returns: about 13 in a year, and the Moon back on its natal degree at each.
        List<Returns.Return> lunars =
            Returns.lunar(sw, natalJd, natalMoon, from, to, NATAL_LAT, NATAL_LON, 'P');
        System.out.printf("lunar returns in the year: %d%n", lunars.size());
        yes("a year holds 12-14 lunar returns", lunars.size() >= 12 && lunars.size() <= 14);
        double prevJd = -1;
        for (Returns.Return lr : lunars) {
            yes("lunar return inside the window", lr.jd >= from && lr.jd <= to);
            yes("lunar returns are chronological", lr.jd > prevJd);
            prevJd = lr.jd;
            double m = Almanac.bodyLongitude(sw, lr.jd, "Moon");
            near("the return Moon is on the natal Moon", 0.0,
                Math.abs(delta(m, natalMoon)), 1e-3);
            eq("lunar return is labelled lunar", "lunar", lr.kind);
        }
        if (!lunars.isEmpty()) {
            List<Returns.Contact> lc =
                Returns.contacts(lunars.get(0), natal, ranked, prof.lord);
            for (Returns.Contact c : lc) {
                yes("the definitional Moon-on-Moon contact is excluded",
                    !("Moon".equals(c.returnPoint) && "Moon".equals(c.natal)));
            }
            System.out.printf("first lunar return contacts: %d%n", lc.size());
        }
        yes("lunar returns stay out of convergence by default",
            !Returns.includeLunarInConvergence);

        // Into the ranking: RETURN is a sky position, so its planetary contacts may be
        // discounted as echoes - but its ANGLES never can, having no body name to match.
        List<Transits.EventHit> events = Transits.eventsToNatal(
            Almanac.datedMoments(sw, from, to), natal, ranked, prof.lord);
        List<Transits.Perfection> perfections =
            Transits.perfectionsOverRange(sw, natal, ranked, prof.lord, from, to);
        List<SolarArc.Contact> arcs =
            SolarArc.contacts(sw, natal, natalJd, ranked, prof.lord, from, to);
        List<Progressions.Contact> progs =
            Progressions.contacts(sw, natal, natalJd, ranked, prof.lord, from, to);
        List<Convergence.Target> targets =
            Convergence.collect(prof, perfections, events, arcs, progs, contacts);

        int retWitnesses = 0;
        int retEchoes = 0;
        for (Convergence.Target t : targets) {
            for (Convergence.Witness w : t.witnesses) {
                if (w.family != Convergence.Family.RETURN) {
                    continue;
                }
                retWitnesses++;
                if (!w.independent) {
                    retEchoes++;
                } else if ("Ascendant".equals(w.movingBody) || "MC".equals(w.movingBody)) {
                    // fine either way; the assertion below covers the class
                }
                if ("Ascendant".equals(w.movingBody) || "MC".equals(w.movingBody)) {
                    yes("a return ANGLE is never an echo", w.independent);
                }
            }
        }
        eq("every return contact became a witness", contacts.size(), retWitnesses);
        System.out.printf("return witnesses %d, of which echoes %d%n", retWitnesses, retEchoes);
        java.util.Map<Integer, Integer> histogram = new java.util.TreeMap<>();
        for (Convergence.Target t : targets) {
            histogram.merge(t.families.size(), 1, Integer::sum);
        }
        System.out.println("family-count histogram with seven families: " + histogram);
        System.out.println("loudest: " + Convergence.loudest(targets));
    }

    // ------------------------------------------------------------------ K: releasing

    /**
     * Zodiacal releasing, as far as it is built.
     *
     * The first assertion is the important one: the loosing-of-the-bond rule is <b>not</b>
     * implemented, and this check exists so that fact cannot quietly stop being true. If
     * someone flips the flag without writing the rule, or writes the rule without flipping
     * the flag, this fails.
     */
    private static void partK(SwissEph sw, ChartFrame natal, double natalJd) {
        yes("the bond rule is implemented", ZodiacalReleasing.bondApplied);

        // The lesser years, against the spec's greater ones. Both facts asserted, because the
        // spec's figures are right for the bounds and wrong here, and the next reader should
        // be able to see the difference rather than take it on trust.
        int circuit = 0;
        for (int y : ZodiacalReleasing.PERIOD_YEARS) {
            circuit += y;
        }
        eq("a full releasing circuit is 211 years", 211, circuit);
        yes("no period is longer than a working lifetime",
            java.util.Arrays.stream(ZodiacalReleasing.PERIOD_YEARS).max().getAsInt() <= 30);
        // The spec's own figures, kept here as the counter-example.
        int greaterCircuit = 57 + 79 + 66 + 82 + 76;
        eq("the spec's figures are the bounds totals, summing to 360", 360, greaterCircuit);

        // Domicile rulers must agree with the period lengths: two signs sharing a ruler take
        // the same number, except Capricorn, which is the documented irregularity.
        for (int s = 0; s < 12; s++) {
            String ruler = Dignity.domicileRulerOf(s);
            for (int t = s + 1; t < 12; t++) {
                if (!ruler.equals(Dignity.domicileRulerOf(t))) {
                    continue;
                }
                boolean capricornPair = s == 9 || t == 9;
                if (capricornPair) {
                    yes("Capricorn is the irregular one",
                        ZodiacalReleasing.PERIOD_YEARS[9] == 27);
                } else {
                    eq("signs sharing " + ruler + " share a period length",
                        ZodiacalReleasing.PERIOD_YEARS[s], ZodiacalReleasing.PERIOD_YEARS[t]);
                }
            }
        }

        // Peaks: exactly four signs are angular to Spirit's, and they are the right four.
        int spiritSign = Zodiac.signIndex(natal.lotOfSpirit);
        int peaks = 0;
        for (int s = 0; s < 12; s++) {
            if (ZodiacalReleasing.isPeak(s, spiritSign)) {
                peaks++;
                int from = Math.floorMod(s - spiritSign, 12);
                yes("a peak sign is angular to Spirit",
                    from == 0 || from == 3 || from == 6 || from == 9);
            }
        }
        eq("exactly four peak signs", 4, peaks);

        // The L1 sequence itself.
        double until = natalJd + 90 * ZodiacalReleasing.DAYS_PER_YEAR;
        List<ZodiacalReleasing.Period> l1 = ZodiacalReleasing.l1(
            natalJd, natal.lotOfSpirit, natal.lotOfSpirit, until);
        System.out.printf("Lot of Spirit %s, releasing L1 over 90 years: %d periods%n",
            Zodiac.format(natal.lotOfSpirit), l1.size());
        for (ZodiacalReleasing.Period p : l1) {
            SweDate sd = new SweDate();
            sd.setJulDay(p.startJd);
            System.out.printf("  %04d-%02d-%02d  %s%n",
                sd.getYear(), sd.getMonth(), sd.getDay(), p);
        }

        yes("releasing produces periods", !l1.isEmpty());
        eq("the first period is the Lot's own sign", spiritSign, l1.get(0).sign);
        near("the first period starts at birth", natalJd, l1.get(0).startJd, 1e-9);
        double prevEnd = natalJd;
        for (ZodiacalReleasing.Period p : l1) {
            eq("L1 periods are level 1", 1, p.level);
            near("periods are contiguous", prevEnd, p.startJd, 1e-9);
            prevEnd = p.endJd;
            // The final period is cut off by the requested end date; only the others are
            // expected to run their sign's full length.
            if (p.truncated) {
                yes("only the last period is truncated", p == l1.get(l1.size() - 1));
            } else {
                near("period length matches its sign",
                    ZodiacalReleasing.PERIOD_YEARS[p.sign] * ZodiacalReleasing.DAYS_PER_YEAR,
                    p.endJd - p.startJd, 1e-6);
            }
            yes("no period is flagged as after a bond", !p.afterBond);
            eq("peak flag agrees with the angularity test",
                ZodiacalReleasing.isPeak(p.sign, spiritSign), p.peak);
        }
        // Signs advance one at a time, which is what "no bond applied" means in practice.
        for (int i = 1; i < l1.size(); i++) {
            eq("signs advance in plain zodiacal order",
                (l1.get(i - 1).sign + 1) % 12, l1.get(i).sign);
        }

        // at() finds the right period, including at the exact boundaries.
        ZodiacalReleasing.Period first = l1.get(0);
        eq("at() finds the birth period", first, ZodiacalReleasing.at(l1, natalJd));
        eq("a period is half-open at its end", l1.get(1),
            ZodiacalReleasing.at(l1, first.endJd));
        yes("at() returns null before birth",
            ZodiacalReleasing.at(l1, natalJd - 1) == null);

        // ---- loosing of the bond -------------------------------------------------
        //
        // The arithmetic that decided between two conflicting sources is pinned here. One
        // said the jump comes on reaching the opposite sign, roughly halfway round; the other
        // said after a full circuit, "about 17 and a half years in". A circuit is 211 months,
        // which is 17.58 years - the full-circuit reading, to within a month. If this figure
        // ever stops being 211 the reasoning in ZodiacalReleasing's comment is void.
        eq("a circuit is 211 sub-periods' worth of months", 211, circuit);
        double circuitYears = circuit / 12.0;
        yes("a circuit lands at about 17 and a half years",
            circuitYears > 17.4 && circuitYears < 17.7);
        // And no parent affords two circuits, which is why the bond fires at most once.
        int longestMonths = java.util.Arrays.stream(ZodiacalReleasing.PERIOD_YEARS)
            .max().getAsInt() * 12;
        yes("no period is long enough for two circuits", longestMonths < 2 * circuit);

        List<ZodiacalReleasing.Period> deep = ZodiacalReleasing.release(
            natalJd, natal.lotOfSpirit, natal.lotOfSpirit, until, 2);
        int bondsSeen = 0;
        for (ZodiacalReleasing.Period p : deep) {
            List<ZodiacalReleasing.Period> kids = p.children;
            yes("a general period has sub-periods", !kids.isEmpty());
            near("sub-periods start with the parent", p.startJd, kids.get(0).startJd, 1e-9);
            eq("sub-periods start on the parent's sign", p.sign, kids.get(0).sign);
            double prev = p.startJd;
            for (ZodiacalReleasing.Period k : kids) {
                eq("sub-periods are level 2", 2, k.level);
                near("sub-periods are contiguous", prev, k.startJd, 1e-9);
                prev = k.endJd;
                yes("sub-periods stay inside the parent", k.endJd <= p.endJd + 1e-9);
            }
            near("sub-periods fill the parent exactly", p.endJd, prev, 1e-9);
            yes("only the last sub-period may be cut off",
                kids.get(kids.size() - 1).truncated
                    || Math.abs(kids.get(kids.size() - 1).endJd - p.endJd) < 1e-9);

            // The bond: present only when the parent affords a whole circuit. Measured from
            // the period's ACTUAL span, not its nominal length - the last one is cut off by
            // the requested end date and so may not reach a circuit even though its sign is
            // long enough. Using the nominal figure here was wrong and this check caught it.
            double actualMonths = (p.endJd - p.startJd) / ZodiacalReleasing.unitDays(2);
            boolean longEnough = actualMonths > circuit;
            List<ZodiacalReleasing.Period> bonded = new java.util.ArrayList<>();
            for (ZodiacalReleasing.Period k : kids) {
                if (k.afterBond) {
                    bonded.add(k);
                }
            }
            if (!longEnough) {
                eq("a short period has no bond (" + Zodiac.SIGNS[p.sign] + ")",
                    0, bonded.size());
            } else {
                bondsSeen++;
                eq("a long period bonds exactly once (" + Zodiac.SIGNS[p.sign] + ")",
                    1, bonded.size());
                ZodiacalReleasing.Period b = bonded.get(0);
                eq("the bond jumps to the sign opposite the parent",
                    (p.sign + 6) % 12, b.sign);
                int idx = kids.indexOf(b);
                eq("the bond comes after a full circuit of twelve", 12, idx);
                eq("the sign before the bond is the twelfth from the start",
                    (p.sign + 11) % 12, kids.get(idx - 1).sign);
                double monthsToBond = (b.startJd - p.startJd)
                    / ZodiacalReleasing.unitDays(2);
                near("the bond lands 211 months in", 211.0, monthsToBond, 1e-6);
                // After the jump the sequence resumes in plain order.
                for (int i = idx + 1; i < kids.size(); i++) {
                    eq("order resumes after the bond",
                        (kids.get(i - 1).sign + 1) % 12, kids.get(i).sign);
                    yes("the bond fires only once", !kids.get(i).afterBond);
                }
            }
        }
        System.out.printf("general periods showing a loosing of the bond: %d%n", bondsSeen);
        yes("at least one bond occurs in ninety years", bondsSeen > 0);

        // Show one, so the dates are visible rather than merely asserted.
        for (ZodiacalReleasing.Period p : deep) {
            if (p.units * 12 <= circuit) {
                continue;
            }
            System.out.printf("  %s (%.0f yr) sub-periods around the bond:%n",
                capitalise(Zodiac.SIGNS[p.sign]), p.units);
            for (int i = 10; i < Math.min(p.children.size(), 15); i++) {
                ZodiacalReleasing.Period k = p.children.get(i);
                SweDate ks = new SweDate();
                ks.setJulDay(k.startJd);
                System.out.printf("    %02d %04d-%02d-%02d  %s%n", i,
                    ks.getYear(), ks.getMonth(), ks.getDay(), k);
            }
            break;
        }
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------ helpers

    /** Signed difference in (-180, 180], recomputed here rather than borrowed. */
    private static double delta(double a, double b) {
        double d = ((a - b) % 360.0 + 360.0) % 360.0;
        return d > 180.0 ? d - 360.0 : d;
    }

    private static double bodyLon(SwissEph sw, double jd, int ipl) {
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        if (sw.swe_calc_ut(jd, ipl, SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED, xx, err)
                == SweConst.ERR) {
            return Double.NaN;
        }
        return ((xx[0] % 360.0) + 360.0) % 360.0;
    }

    private static double moonLatitude(SwissEph sw, double jd) {
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        if (sw.swe_calc_ut(jd, SweConst.SE_MOON, SweConst.SEFLG_SWIEPH, xx, err)
                == SweConst.ERR) {
            return Double.NaN;
        }
        return xx[1];
    }

    private static double speedAt(SwissEph sw, double jd, int ipl) {
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        if (sw.swe_calc_ut(jd, ipl, SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED, xx, err)
                == SweConst.ERR) {
            return Double.NaN;
        }
        return xx[3];
    }

    private static double lonOf(ChartFrame f, String name) {
        for (ChartFrame.Body b : f.bodies) {
            if (b != null && b.ok && b.name.equals(name)) {
                return b.lon;
            }
        }
        return Double.NaN;
    }

    private static double speedOf(ChartFrame f, String name) {
        for (ChartFrame.Body b : f.bodies) {
            if (b != null && b.ok && b.name.equals(name)) {
                return b.lonSpeed;
            }
        }
        return Double.NaN;
    }

    private static double natalLonOf(ChartFrame f, String name) {
        switch (name) {
            case "Ascendant":  return f.asc;
            case "MC":         return f.mc;
            case "Descendant": return f.dsc;
            case "IC":         return f.ic;
            default:           return lonOf(f, name);
        }
    }

    private static void eq(String what, Object expect, Object got) {
        checks++;
        if (!expect.equals(got)) {
            failures++;
            System.out.println("  FAIL " + what + ": expected " + expect + ", got " + got);
        }
    }

    private static void near(String what, double expect, double got, double tol) {
        checks++;
        if (Double.isNaN(got) || Math.abs(expect - got) > tol) {
            failures++;
            System.out.printf("  FAIL %s: expected %.9f, got %.9f%n", what, expect, got);
        }
    }

    private static void yes(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL " + what);
        }
    }

    private static void fail(String what) {
        checks++;
        failures++;
        System.out.println("  FAIL " + what);
    }

    private TransitCheck() { }
}
