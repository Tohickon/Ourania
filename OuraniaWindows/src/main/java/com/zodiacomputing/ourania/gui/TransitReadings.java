package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The Transits tab's own pages: the current sky read against each chart that is selected
 * (2026-10-22).
 *
 * <b>Why a tab of its own.</b> David, 2026-10-22: "we should add a separate tab ... that takes
 * the sky chart and its transit effect on chart a or chart b or both whichever is selected on
 * in the chart and then has the same tab structure as the rest but only reads each tab as
 * interpreting current transits to the selected charts". The other tabs read one chart; these
 * pages read one chart acting on another, so every section says which: "The current sky on
 * Chart A" (SkyOnChart.heading), one section per selected person.
 *
 * <b>Cast from the people, not from the wheel's rings.</b> With one person the sky is the
 * wheel's outer ring and with two it is the third, and which ring holds what is the wheel's
 * business. Each person's chart and the sky are cast here from their subjects, so a page reads
 * the same whichever rings the wheel happens to draw.
 */
final class TransitReadings {

    /** One selected chart: whose it is, for the headings, and the chart itself. */
    record Person(String whose, ChartFrame frame) {
    }

    /** The sky's bodies a person's chart is read against: the planets and the slow points. */
    private static final List<String> SKY_BODIES = List.of("Sun", "Moon", "Mercury", "Venus",
        "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron", "North Node");

    /** The slower the planet, the longer its contact lasts; the order every list here takes. */
    private static final List<String> SLOW_FIRST = List.of("Pluto", "Neptune", "Uranus",
        "Saturn", "Chiron", "North Node", "Jupiter", "Mars", "Sun", "Venus", "Mercury", "Moon");

    private TransitReadings() {
    }

    /** What the page says when no chart is selected to read the sky against. */
    static String nobody() {
        return "<p style='color:#AAAAAA;'>Select Chart A, Chart B or both with the chips under"
            + " the wheel, and this page reads the current sky on them.</p>";
    }

    // ------------------------------------------------------------------ At a glance

    /** The strongest contacts now, as plain cards, for each selected chart. */
    static String glance(List<Person> people, ChartFrame sky, boolean showWhy) {
        if (people.isEmpty() || sky == null) {
            return nobody();
        }
        StringBuilder sb = new StringBuilder();
        for (Person p : people) {
            sb.append(SkyOnChart.html(p.whose(), p.frame(), sky, hits(p.frame(), sky), showWhy));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ Interpretation

    /** Every contact in effect now, slowest planet first, each with its written reading. */
    static String interpretation(List<Person> people, ChartFrame sky, boolean showWhy) {
        if (people.isEmpty() || sky == null) {
            return nobody();
        }
        InterpretationService svc = InterpretationService.getInstance();
        StringBuilder sb = new StringBuilder();
        for (Person p : people) {
            sb.append(SkyOnChart.sectionHeading(SkyOnChart.heading(p.whose())));
            List<Transits.Hit> hits = shown(hits(p.frame(), sky), showWhy);
            if (hits.isEmpty()) {
                sb.append("<p style='color:#AAAAAA;'>Today's sky makes no strong contact with"
                    + " this chart.</p>");
                continue;
            }
            for (Transits.Hit h : hits) {
                String aspect = h.type.label.toLowerCase();
                sb.append("<h3 style='color:#FFD166; margin-bottom:2px;'>").append(h.transiting)
                    .append(' ').append(aspect).append(" your ").append(h.natal).append("</h3>");
                sb.append("<p style='color:#AAAAAA; margin-top:0;'>")
                    .append(h.applying ? "Building" : "Fading")
                    .append(String.format(", %.1f&deg; from exact.", h.offBy)).append("</p>");
                String text = svc.getTransitAspect(h.transiting, h.natal, h.type.label);
                if (text != null && !text.startsWith("Interpretation not found")) {
                    sb.append("<p>").append(text).append("</p>");
                }
                ChartFrame.Body b = sky.body(h.transiting);
                int house = b == null || !b.ok ? 0 : Zodiac.houseOf(b.lon, p.frame().cusps);
                if (showWhy && house > 0) {
                    String housed = svc.getTransitInHouse(h.transiting, house);
                    if (housed != null && !housed.startsWith("Interpretation not found")) {
                        sb.append("<p style='color:#BBBBBB;'><b>").append(h.transiting)
                            .append(" in your ").append(ordinal(house)).append(" house:</b> ")
                            .append(housed).append("</p>");
                    }
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ shared

    /**
     * The five major aspects in plain view; every aspect in Astrologer view. The first render
     * listed a biquintile to the Part of Spirit and a decile to the Lot of Nemesis beside
     * Pluto on the Midheaven, each true and none of them a reading for a person.
     */
    private static List<Transits.Hit> shown(List<Transits.Hit> hits, boolean showWhy) {
        if (showWhy) {
            return hits;
        }
        List<Transits.Hit> out = new ArrayList<>();
        for (Transits.Hit h : hits) {
            if (!h.type.isMinor()) {
                out.add(h);
            }
        }
        return out;
    }

    /**
     * The sky's contacts to one chart, slowest planet first and tightest within it.
     *
     * The engine's own rule for which natal points matter ({@link Transits#toNatal}), with the
     * chart's own ranking, so these pages and the readings on a person's tab agree about it.
     */
    static List<Transits.Hit> hits(ChartFrame natal, ChartFrame sky) {
        List<BodyScore.Vector> ranked = BodyScore.rank(natal, Gestalt.compute(natal));
        List<Transits.Hit> out = new ArrayList<>();
        Set<String> wanted = Set.copyOf(SKY_BODIES);
        for (Transits.Hit h : Transits.toNatal(natal, sky, ranked, null)) {
            if (h != null && h.type != null && wanted.contains(h.transiting)) {
                out.add(h);
            }
        }
        out.sort(Comparator.comparingInt((Transits.Hit h) -> SLOW_FIRST.indexOf(h.transiting))
            .thenComparingDouble(h -> h.offBy));
        return out;
    }

    private static String ordinal(int n) {
        int mod100 = n % 100;
        String suffix = mod100 >= 11 && mod100 <= 13 ? "th"
            : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suffix;
    }
}
