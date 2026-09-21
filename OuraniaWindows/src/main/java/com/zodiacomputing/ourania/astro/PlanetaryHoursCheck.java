package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * K13's planetary days and hours.
 *
 * <p><b>This one can be checked against something outside the program.</b> Most of the engine has
 * to be proved by recomputing it a second way; here the answer is written on every calendar in
 * the world. Twenty-four hours stepped through a seven-planet cycle advances by three, and
 * stepping three through Saturn-Jupiter-Mars-Sun-Venus-Mercury-Moon gives Saturn, Sun, Moon,
 * Mars, Mercury, Jupiter, Venus - which is the order of the days of the week. So the strongest
 * assertions here do not check the class against a fixture: they walk seven consecutive days and
 * require the ruler of each day's first hour to spell out Saturday, Sunday, Monday and the rest.
 * A bug in the Chaldean walk, the day boundary or the hour division disagrees with the calendar.
 *
 * <p>The rest is the shape of the thing: twenty-four hours with no gap and no overlap between one
 * sunrise and the next, twelve of each kind, unequal except at an equinox - and a refusal rather
 * than an invention where the Sun does not rise at all.
 */
public final class PlanetaryHoursCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final double PHILLY_LAT = 39.95;
    private static final double PHILLY_LON = -75.17;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        // ---- the week, derived rather than looked up
        double start = new SweDate(2026, 9, 23, 17.0).getJulDay();
        String[] expected = {"Mercury", "Jupiter", "Venus", "Saturn", "Sun", "Moon", "Mars"};
        String[] weekday = {"Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
            "Monday", "Tuesday"};
        List<PlanetaryHours.Day> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            PlanetaryHours.Day d = PlanetaryHours.at(sw, start + i, PHILLY_LAT, PHILLY_LON);
            ok("day " + i + " is found", d != null);
            if (d == null) {
                report();
                return;
            }
            week.add(d);
            ok(weekday[i] + " is the " + expected[i] + " day (" + d.ruler + ")",
                expected[i].equals(d.ruler));
            ok("and its first hour is ruled by " + expected[i],
                expected[i].equals(d.hours.get(0).ruler));
        }

        // <b>The fossil, stated directly.</b> Twenty-four hours on from a day's first, the
        // cycle has advanced three places - and that is the next day's ruler. This is the rule
        // the week's order IS; if it holds for seven days running, the walk is right.
        boolean fossil = true;
        for (int i = 0; i + 1 < week.size(); i++) {
            String twentyFifth = PlanetaryHours.CHALDEAN[
                (indexOf(week.get(i).ruler) + 24) % PlanetaryHours.CHALDEAN.length];
            if (!twentyFifth.equals(week.get(i + 1).ruler)) {
                fossil = false;
            }
        }
        ok("the twenty-fifth hour begins the next day, which is why the week runs as it does",
            fossil);

        // ---- the shape of a day
        PlanetaryHours.Day d = week.get(0);
        ok("twenty-four hours", d.hours.size() == 24);
        ok("twelve of them are daytime",
            d.hours.stream().filter(h -> h.daytime).count() == 12);
        ok("the first begins at sunrise", Math.abs(d.hours.get(0).from - d.sunrise) < 1e-9);
        ok("the twelfth ends at sunset", Math.abs(d.hours.get(11).to - d.sunset) < 1e-9);
        ok("the thirteenth begins at sunset", Math.abs(d.hours.get(12).from - d.sunset) < 1e-9);
        ok("the last ends at the next sunrise",
            Math.abs(d.hours.get(23).to - d.nextSunrise) < 1e-9);

        boolean contiguous = true;
        boolean numbered = true;
        boolean named = true;
        for (int i = 0; i < d.hours.size(); i++) {
            PlanetaryHours.Hour h = d.hours.get(i);
            if (h.index != i + 1) {
                numbered = false;
            }
            if (indexOf(h.ruler) < 0) {
                named = false;
            }
            if (i > 0 && Math.abs(h.from - d.hours.get(i - 1).to) > 1e-9) {
                contiguous = false;
            }
        }
        ok("no gap and no overlap between one hour and the next", contiguous);
        ok("numbered one to twenty-four", numbered);
        ok("every ruler is one of the seven", named);

        boolean walks = true;
        for (int i = 1; i < d.hours.size(); i++) {
            int prev = indexOf(d.hours.get(i - 1).ruler);
            if (indexOf(d.hours.get(i).ruler) != (prev + 1) % PlanetaryHours.CHALDEAN.length) {
                walks = false;
            }
        }
        ok("the hours walk the Chaldean order, one step each", walks);

        // <b>Unequal, and equal only at an equinox.</b> The 23rd of September is three days
        // past one, so the two kinds of hour are within a couple of minutes; at the solstice
        // they are not, and a class that divided the day into twenty-four equal hours would
        // pass the first of these and fail the second.
        double dayMin = d.hours.get(0).minutes();
        double nightMin = d.hours.get(12).minutes();
        ok(String.format("near the equinox the two kinds of hour are nearly equal (%.1f / %.1f)",
            dayMin, nightMin), Math.abs(dayMin - nightMin) < 4.0);

        PlanetaryHours.Day solstice =
            PlanetaryHours.at(sw, new SweDate(2026, 6, 21, 17.0).getJulDay(),
                PHILLY_LAT, PHILLY_LON);
        ok("a solstice day is found", solstice != null);
        double sDay = solstice.hours.get(0).minutes();
        double sNight = solstice.hours.get(12).minutes();
        ok(String.format("at the solstice they are not (%.1f / %.1f)", sDay, sNight),
            sDay - sNight > 20.0);
        ok("and the two kinds still fill the day between them",
            Math.abs(sDay * 12 + sNight * 12
                - (solstice.nextSunrise - solstice.sunrise) * 24 * 60) < 0.01);

        // ---- the day begins at sunrise, which is the trap
        double beforeDawn = new SweDate(2026, 9, 24, 5.5).getJulDay();
        PlanetaryHours.Day night = PlanetaryHours.at(sw, beforeDawn, PHILLY_LAT, PHILLY_LON);
        ok("an instant before dawn is found in a day", night != null);
        ok("and it is still the previous planetary day - Mercury's, not Jupiter's ("
            + night.ruler + ")", "Mercury".equals(night.ruler));
        ok("in one of its night hours", night.at(beforeDawn) != null
            && !night.at(beforeDawn).daytime);
        ok("the calendar date there is already the 24th, which is the point",
            dayOfMonth(beforeDawn) == 24);

        // ---- the instant is inside the day it is handed
        boolean holds = true;
        for (int i = 0; i < 7; i++) {
            double t = start + i;
            PlanetaryHours.Day day = PlanetaryHours.at(sw, t, PHILLY_LAT, PHILLY_LON);
            if (day == null || t < day.sunrise || t >= day.nextSunrise || day.at(t) == null) {
                holds = false;
            }
        }
        ok("every instant lands inside the day it is given, and inside one of its hours", holds);

        ok("rulerAt agrees with the day it comes from",
            PlanetaryHours.rulerAt(sw, start, PHILLY_LAT, PHILLY_LON)
                .equals(d.at(start).ruler));

        // ---- where the Sun does not rise, it says so
        double polarWinter = new SweDate(2026, 12, 21, 12.0).getJulDay();
        ok("no hours inside the arctic winter, rather than invented ones",
            PlanetaryHours.at(sw, polarWinter, 78.2, 15.6) == null);
        ok("and none in the midnight sun either",
            PlanetaryHours.at(sw, new SweDate(2026, 6, 21, 12.0).getJulDay(), 78.2, 15.6) == null);
        ok("rulerAt says nothing there rather than guessing",
            PlanetaryHours.rulerAt(sw, polarWinter, 78.2, 15.6).isEmpty());

        // <b>And the harder polar case: the Sun rises and then does not set.</b> The winter
        // fixtures above are answered by the first guard - no sunrise at all - so on their own
        // they say nothing about the transition into the midnight sun, where there IS a sunrise
        // and the day never closes. Two found by searching 66.6 to 71 north across the fortnight
        // either side.
        ok("the day the midnight sun begins has no hours at 69.65 north",
            PlanetaryHours.at(sw, new SweDate(2026, 5, 18, 12.0).getJulDay(), 69.65, 18.96) == null);
        ok("nor at 67.5 north, where it begins eleven days later",
            PlanetaryHours.at(sw, new SweDate(2026, 5, 29, 12.0).getJulDay(), 67.5, 18.96) == null);
        ok("and the same place has hours again once the Sun sets there",
            PlanetaryHours.at(sw, new SweDate(2026, 9, 23, 12.0).getJulDay(), 69.65, 18.96) != null);

        report();
    }

    private static int dayOfMonth(double jd) {
        SweDate d = new SweDate();
        d.setJulDay(jd);
        return d.getDay();
    }

    private static int indexOf(String planet) {
        for (int i = 0; i < PlanetaryHours.CHALDEAN.length; i++) {
            if (PlanetaryHours.CHALDEAN[i].equals(planet)) {
                return i;
            }
        }
        return -1;
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
        }
        System.exit(1);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
