package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The current sky on a person's chart, for At a glance (2026-10-22).
 *
 * <b>Why it exists.</b> David, 2026-10-22: At a glance on Chart A said "where the weight sits
 * now" about a birth chart, and on the Sky tab "its as if... its not taking into effect the
 * transit to the other chart. is there a way to bridge the 2 charts". At a glance read the
 * chart on the inner wheel and nothing else. This is the bridge: what today's sky is touching
 * in one person's chart, under a heading that names both, because "there needs to be
 * organization in the readings as to what chart is effecting what chart".
 *
 * <b>The words are the transit card's</b> ({@link TransitCard}), so a contact here reads as it
 * does on Coming up. Only contacts with a plain card take part: the five major aspects, and a
 * natal point with a written need.
 *
 * <b>Strongest first, and only a few.</b> The slower the planet, the longer the contact lasts
 * and the more it shapes a season; the Moon is left out, because it is gone in hours. Within a
 * planet, the tighter contact leads. This orders a short list a person reads, as
 * {@link YearMoments} does; it scores nothing.
 */
final class SkyOnChart {

    /** How many contacts the section shows. */
    static final int SHOWN = 3;

    private SkyOnChart() {
    }

    /** The heading that says which chart is acting on which: the sky on this person's chart. */
    static String heading(String whose) {
        return "The current sky on " + chartHeading(whose);
    }

    /**
     * A chart by its owner: "David's chart" once it has a name, and "Chart A" before - the
     * slot's own name already says it is a chart, and "Chart A's chart" says it twice.
     */
    static String chartHeading(String whose) {
        String w = whose == null || whose.isBlank() ? "Chart A" : whose.trim();
        return w.matches("Chart [AB]") ? w : possessive(w) + " chart";
    }

    /**
     * The section: its heading, a line saying what it is, and the strongest contacts as cards.
     *
     * @param whose the person's name, or "Chart A" / "Chart B" before the chart has one
     * @param hits  the sky's contacts to that chart, from {@link Transits#toNatal}
     */
    static String html(String whose, ChartFrame natal, ChartFrame sky, List<Transits.Hit> hits,
            boolean showWhy) {
        StringBuilder sb = new StringBuilder(sectionHeading(heading(whose)));
        List<ReadingCard> cards = cards(natal, sky, hits);
        if (cards.isEmpty()) {
            sb.append("<p style='color:#AAAAAA;'>Today's sky makes no strong contact with this"
                + " chart. It is a quiet time for it.</p>");
        } else {
            sb.append("<p style='color:#AAAAAA;'>What today's sky is touching in this chart,"
                + " strongest first.</p>");
            sb.append(ReadingCard.html(cards, showWhy));
        }
        return sb.toString();
    }

    /**
     * A heading between parts of a reading. Its own size and rule, apart from a card's label,
     * because it says whose chart the cards under it are about.
     */
    static String sectionHeading(String title) {
        return "<div style='color:#FFFFFF; font-size:16px; font-weight:bold; margin-top:14px;"
            + " margin-bottom:6px; border-bottom:1px solid #444; padding-bottom:3px;'>"
            + escape(title) + "</div>";
    }

    /** The strongest contacts, as plain cards. */
    static List<ReadingCard> cards(ChartFrame natal, ChartFrame sky, List<Transits.Hit> hits) {
        List<ReadingCard> out = new ArrayList<>();
        if (natal == null || sky == null || hits == null) {
            return out;
        }
        List<Transits.Hit> ranked = new ArrayList<>();
        for (Transits.Hit h : hits) {
            if (h != null && h.type != null && !"Moon".equals(h.transiting)) {
                ranked.add(h);
            }
        }
        ranked.sort(Comparator.comparingDouble((Transits.Hit h) -> -slowness(h.transiting))
            .thenComparingDouble(h -> h.offBy));
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Transits.Hit h : ranked) {
            if (out.size() >= SHOWN) {
                break;
            }
            // One card per planet: the first render had Pluto on all three, each true and
            // together a page about one planet. Its strongest contact stands for it.
            if (seen.contains(h.transiting)) {
                continue;
            }
            NowCards.TransitHit hit = toCardHit(natal, sky, h);
            ReadingCard card = hit == null ? null : TransitCard.of(hit);
            if (card != null) {
                out.add(card);
                seen.add(h.transiting);
            }
        }
        return out;
    }

    /** The slower the planet, the longer its contact lasts. */
    private static double slowness(String body) {
        return switch (body) {
            case "Pluto" -> 9;
            case "Neptune" -> 8;
            case "Uranus" -> 7;
            case "Saturn" -> 6;
            case "Chiron" -> 5;
            case "North Node", "South Node" -> 4.5;
            case "Jupiter" -> 4;
            case "Mars" -> 3;
            case "Sun", "Venus", "Mercury" -> 2;
            default -> 1;
        };
    }

    /** The contact in the shape the transit card reads. */
    private static NowCards.TransitHit toCardHit(ChartFrame natal, ChartFrame sky,
            Transits.Hit h) {
        ChartFrame.Body t = sky.body(h.transiting);
        if (t == null || !t.ok) {
            return null;
        }
        double nLon = natalLon(natal, h.natal);
        if (Double.isNaN(nLon)) {
            return null;
        }
        int tHouse = Zodiac.houseOf(t.lon, natal.cusps);
        return new NowCards.TransitHit(h.transiting, t.lon, h.transitSpeed, tHouse, h.natal,
            nLon, natalHouse(natal, h.natal, nLon), h.type, h.offBy, h.applying,
            NowCards.FAST_BODIES.contains(h.transiting));
    }

    private static double natalLon(ChartFrame f, String name) {
        switch (name) {
            case "Ascendant": return f.asc;
            case "MC": return f.mc;
            case "Descendant": return f.dsc;
            case "IC": return f.ic;
            default: break;
        }
        ChartFrame.Body b = f.body(name);
        return b == null || !b.ok ? Double.NaN : b.lon;
    }

    /** The part of life a point lives in: its house, or the house an angle opens. */
    private static int natalHouse(ChartFrame f, String name, double lon) {
        return switch (name) {
            case "Ascendant" -> 1;
            case "IC" -> 4;
            case "Descendant" -> 7;
            case "MC" -> 10;
            default -> Zodiac.houseOf(lon, f.cusps);
        };
    }

    /** "David's"; a name already ending in s takes the apostrophe alone. */
    static String possessive(String w) {
        return w.endsWith("s") ? w + "'" : w + "'s";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
