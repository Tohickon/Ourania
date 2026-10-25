package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Ephemeris;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Coming up for the sky alone (SkyTimeline, 2026-10-20).
 *
 * <b>What is pinned:</b> a real year of the sky gives cards of every kind the Almanac finds, each
 * in the window of its own date and in date order, the plain words carry none of JargonCheck's
 * banned words, and the page caps a window the way the chart's timeline does.
 */
public final class SkyTimelineCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        Settings.useScratchFile();
        System.out.println("Running com.zodiacomputing.ourania.gui.SkyTimelineCheck");

        SwissEph sw = new SwissEph(Ephemeris.PATH);
        ZoneId ny = ZoneId.of("America/New_York");
        double now = SweDate.getJulDay(2026, 10, 20, 16.0);
        long t0 = System.currentTimeMillis();
        List<SkyTimeline.Entry> year = SkyTimeline.build(sw, now, ny);
        long ms = System.currentTimeMillis() - t0;
        System.out.println("  a year of the sky in " + ms + " ms");

        yes("a year of the sky has events, " + year.size(), year.size() >= 20);
        Set<Almanac.Kind> kinds = EnumSet.noneOf(Almanac.Kind.class);
        double last = Double.NEGATIVE_INFINITY;
        boolean inOrder = true;
        List<String> misplaced = new ArrayList<>();
        List<String> jargon = new ArrayList<>();
        Pattern banned = Pattern.compile("\b(" + String.join("|", JargonCheck.BANNED) + ")\b",
            Pattern.CASE_INSENSITIVE);
        for (SkyTimeline.Entry e : year) {
            kinds.add(e.event().kind);
            inOrder &= e.event().jd >= last;
            last = e.event().jd;
            if (NowTimeline.windowOf(e.event().jd, now, ny) != e.window()) {
                misplaced.add(e.event() + " in " + e.window());
            }
            String plain = e.card().headline() + " " + e.card().body();
            if (banned.matcher(plain).find()) {
                jargon.add(plain);
            }
        }
        for (Almanac.Kind k : new Almanac.Kind[] {Almanac.Kind.INGRESS,
                Almanac.Kind.STATION_RETROGRADE, Almanac.Kind.STATION_DIRECT,
                Almanac.Kind.NEW_MOON, Almanac.Kind.FULL_MOON}) {
            yes("the year has a " + k + " card", kinds.contains(k));
        }
        yes("in date order", inOrder);
        eq("each in the window of its own date", "[]", misplaced.toString());
        eq("and no plain card uses a banned word", "[]", jargon.toString());

        String page = SkyTimeline.html(year, false);
        yes("the page is Coming up", page.contains(">Coming up</h2>"));
        yes("and says no birth chart is on the wheel", page.contains("No birth chart"));
        yes("a full window is capped, with the rest counted", page.contains(" more "));
        yes("plain view hides the astrology", !page.contains("Why &mdash;"));
        yes("astrologer view shows it", SkyTimeline.html(year, true).contains("Why &mdash;"));

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

    private static void yes(String claim, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(claim);
            System.out.println("  FAIL: " + claim);
        } else {
            System.out.println("  ok: " + claim);
        }
    }

    private static void eq(String claim, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(claim + " (expected <" + expected + ">, got <" + actual + ">)");
            System.out.println("  FAIL: " + claim + " (expected <" + expected + ">, got <"
                + actual + ">)");
        } else {
            System.out.println("  ok: " + claim);
        }
    }
}
