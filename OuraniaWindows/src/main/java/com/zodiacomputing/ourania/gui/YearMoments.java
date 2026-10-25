package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Progressions;
import com.zodiacomputing.ourania.astro.SolarArc;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.YearScan;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SweDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The year's biggest moments: when each one comes, what makes it, and what it means
 * (2026-10-22).
 *
 * <b>Why it exists.</b> David, 2026-10-22: Your year "should be only your year and how each
 * point effects natal points throughout the year (i.e. biggest moments during the year and when
 * they occur and by what and what it means". The page had the year's themes and then a long
 * list of natal placements, which say nothing about this year; the dated contacts were all in
 * the year scan and all folded under Mechanics.
 *
 * <b>One moment per contact, not per pass.</b> A slow planet can cross a point three times in
 * a year, going forward, back and forward again. That is one story with three dates, so the
 * passes are kept together and the card names each date.
 *
 * <b>Biggest is a ranking for a person to read, not a vote.</b> The weights here order a short
 * list; they do not feed Convergence, which has its own and decides the themes. The slower the
 * mover, the rarer the moment; a contact that hits head-on outranks an easy one; and the
 * lights, the Ascendant, the MC and the year's own ruler are the points a year turns on.
 *
 * <b>The words are the transit card's parts</b> ({@code transit_cards.json}), so a moment here
 * and the same contact on Coming up say one thing. A contact whose parts are not all written
 * gives no card, as on Coming up: a minor aspect, or a point with no written need.
 */
final class YearMoments {

    /** How many moments the page shows. Enough for a year, few enough to read. */
    static final int SHOWN = 8;

    /** The bodies whose slow inner movement can make a moment. */
    private static final java.util.Set<String> PLANETS = java.util.Set.of("Sun", "Moon",
        "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto");

    /** The line that starts a moment's direction, on its own paragraph. */
    static final String WHAT_TO_DO = "What to do: ";

    private YearMoments() {
    }

    /** What makes a moment, which decides the words of its card. */
    private enum Mover { SKY, STATION, ECLIPSE, INNER }

    /** One contact across the year, with every date it is exact. */
    private static final class Moment {
        final Mover mover;
        /** The moving body, or "solar" / "lunar" for an eclipse. */
        final String body;
        final String natal;
        final Aspects.Type type;
        /** The technique, for the why line: "transiting", "progressed", "directed", ... */
        final String technique;
        final List<Double> dates = new ArrayList<>();
        double weight;

        Moment(Mover mover, String body, String natal, Aspects.Type type, String technique) {
            this.mover = mover;
            this.body = body;
            this.natal = natal;
            this.type = type;
            this.technique = technique;
        }

        double first() {
            return dates.get(0);
        }
    }

    /**
     * The year's biggest moments as cards, in the order they come.
     *
     * @param lord the year's ruling planet, which lifts a moment that touches it; may be null
     */
    static List<ReadingCard> cards(ChartFrame f, YearScan scan, String lord) {
        List<ReadingCard> out = new ArrayList<>();
        if (f == null || scan == null) {
            return out;
        }
        Map<String, Moment> moments = new LinkedHashMap<>();
        for (Transits.Perfection p : scan.perfections) {
            add(moments, Mover.SKY, p.transiting, p.natal, p.type, "transiting", p.jd);
        }
        for (Transits.EventHit e : scan.events) {
            Almanac.Event ev = e.event;
            if (ev == null) {
                continue;
            }
            switch (ev.kind) {
                case SOLAR_ECLIPSE -> add(moments, Mover.ECLIPSE, "solar", e.natal, e.type,
                    "solar eclipse", ev.jd);
                case LUNAR_ECLIPSE -> add(moments, Mover.ECLIPSE, "lunar", e.natal, e.type,
                    "lunar eclipse", ev.jd);
                case STATION_RETROGRADE, STATION_DIRECT -> add(moments, Mover.STATION, ev.body,
                    e.natal, e.type, "stationing", ev.jd);
                default -> { }
            }
        }
        // A planet moving forward, not a point: the first render had a directed Pallas and a
        // directed Lot of Eros among the eight biggest moments of the year.
        for (Progressions.Contact c : scan.progressions) {
            if (PLANETS.contains(c.progressed)) {
                add(moments, Mover.INNER, c.progressed, c.natal, c.type, "progressed", c.jd);
            }
        }
        for (SolarArc.Contact c : scan.arcs) {
            if (PLANETS.contains(c.directed)) {
                add(moments, Mover.INNER, c.directed, c.natal, c.type, "directed", c.jd);
            }
        }

        List<Moment> ranked = new ArrayList<>();
        for (Moment m : moments.values()) {
            if (card(f, m) == null) {
                continue;
            }
            m.dates.sort(Double::compare);
            m.weight = moverWeight(m) * aspectWeight(m.type) * natalWeight(m.natal, lord);
            ranked.add(m);
        }
        ranked.sort(Comparator.comparingDouble((Moment m) -> -m.weight)
            .thenComparingDouble(Moment::first));
        List<Moment> shown = new ArrayList<>(ranked.subList(0, Math.min(SHOWN, ranked.size())));
        shown.sort(Comparator.comparingDouble(Moment::first));
        for (Moment m : shown) {
            out.add(card(f, m));
        }
        return out;
    }

    private static void add(Map<String, Moment> moments, Mover mover, String body, String natal,
            Aspects.Type type, String technique, double jd) {
        if (body == null || natal == null || type == null || Double.isNaN(jd)) {
            return;
        }
        String key = technique + "|" + body + "|" + type + "|" + natal;
        moments.computeIfAbsent(key, k -> new Moment(mover, body, natal, type, technique))
            .dates.add(jd);
    }

    /** The slower the mover, the rarer the moment. */
    private static double moverWeight(Moment m) {
        return switch (m.mover) {
            case INNER -> 5.0;
            case ECLIPSE -> 4.0;
            case STATION -> 3.0;
            case SKY -> switch (m.body) {
                case "Pluto" -> 6.0;
                case "Neptune", "Uranus" -> 5.0;
                case "Saturn" -> 4.0;
                default -> 3.0;
            };
        };
    }

    /** A contact that meets head-on outranks one that flows. */
    private static double aspectWeight(Aspects.Type type) {
        return switch (type) {
            case CONJUNCTION -> 1.0;
            case OPPOSITION, SQUARE -> 0.9;
            case TRINE -> 0.6;
            default -> 0.5;
        };
    }

    /** The lights and the two main angles carry a year; so does the year's own ruler. */
    private static double natalWeight(String natal, String lord) {
        double w = switch (natal) {
            case "Sun", "Moon", "Ascendant", "MC" -> 1.5;
            default -> 1.0;
        };
        return natal.equals(lord) ? w * 1.3 : w;
    }

    /** The card for one moment, or null when its parts are not all written. */
    private static ReadingCard card(ChartFrame f, Moment m) {
        InterpretationService svc = InterpretationService.getInstance();
        String aspect = m.type.label.toLowerCase(Locale.ROOT);
        String source = switch (m.mover) {
            case ECLIPSE -> "eclipse";
            case INNER -> "inner";
            default -> idOf(m.body);
        };
        String trigger = svc.getTransitCardPart("trigger." + source);
        String acts = svc.getTransitCardPart("acts." + source);
        String verb = svc.getTransitCardPart("aspect." + aspect);
        String feel = svc.getTransitCardPart("feel." + aspect);
        String need = svc.getTransitCardPart("need." + idOf(m.natal));
        String todo = svc.getTransitCardPart("todo." + source + "." + aspect);
        if (trigger == null || acts == null || verb == null || feel == null || need == null
                || todo == null) {
            return null;
        }
        String station = m.mover == Mover.STATION ? svc.getTransitCardPart("station") : null;
        if (m.mover == Mover.STATION && station == null) {
            return null;
        }

        String when = dayOf(m.first()).toUpperCase(Locale.ROOT);
        String area = LifeAreas.of(houseOf(f, m.natal));
        String label = area == null ? when : when + " · " + area.toUpperCase(Locale.ROOT);

        StringBuilder body = new StringBuilder();
        body.append(byWhat(m, verb)).append(' ').append(dates(m)).append("\n\n");
        if (station != null) {
            body.append(station).append(' ');
        }
        body.append(acts).append(' ').append(feel).append("\n\n").append(WHAT_TO_DO).append(todo);

        List<String> why = new ArrayList<>();
        // An eclipse's technique already names it: "solar eclipse", not "solar eclipse solar".
        String who = m.mover == Mover.ECLIPSE ? m.technique : m.technique + " " + m.body;
        for (double jd : m.dates) {
            why.add(who + " " + aspect + " natal " + m.natal + ", exact " + dayOf(jd));
        }
        return new ReadingCard(label, trigger + " " + verb + " " + need, body.toString(), why);
    }

    /** What makes the moment, in plain words: "Saturn in the sky clashes with your Sun." */
    private static String byWhat(Moment m, String verb) {
        String natal = "your " + m.natal;
        return switch (m.mover) {
            case SKY -> m.body + " in the sky " + verb + " " + natal + ".";
            case STATION -> m.body + " stands still and " + verb + " " + natal + ".";
            case ECLIPSE -> "A " + m.body + " eclipse " + verb + " " + natal + ".";
            case INNER -> "Your own " + m.body + ", moving slowly through your life, " + verb
                + " " + natal + ".";
        };
    }

    /** "It is exact on 14 March 2027." with any later passes named after it. */
    private static String dates(Moment m) {
        StringBuilder sb = new StringBuilder("It is exact on ").append(dayOf(m.dates.get(0)));
        for (int i = 1; i < m.dates.size(); i++) {
            sb.append(i == m.dates.size() - 1 ? ", and again on " : ", again on ")
                .append(dayOf(m.dates.get(i)));
        }
        return sb.append('.').toString();
    }

    /** The part of life a natal point lives in: its house, or the house an angle opens. */
    private static int houseOf(ChartFrame f, String natal) {
        switch (natal) {
            case "Ascendant": return 1;
            case "IC": return 4;
            case "Descendant": return 7;
            case "MC": return 10;
            default: break;
        }
        ChartFrame.Body b = f.body(natal);
        return b == null || !b.ok ? 0 : Zodiac.houseOf(b.lon, f.cusps);
    }

    private static String dayOf(double jd) {
        SweDate d = new SweDate(jd);
        return d.getDay() + " " + MONTHS[Math.max(1, Math.min(12, d.getMonth())) - 1] + " "
            + d.getYear();
    }

    private static final String[] MONTHS = {"January", "February", "March", "April", "May",
        "June", "July", "August", "September", "October", "November", "December"};

    private static String idOf(String name) {
        Bodies.Def d = Bodies.byName(name);
        return d == null ? "" : d.id;
    }
}
