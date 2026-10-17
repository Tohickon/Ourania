package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SwissEph;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Now page as one timeline, in the order a reader asks: what is happening right now,
 * today, tonight, tomorrow, this week, next week, this month, next month, later this year.
 *
 * <b>David, 2026-10-17:</b> "its an interpretation app so it should be ordered in what the user
 * wants to know right now, today, tonight tomorrow this week next week this month next month
 * this year etc and easy to read and understand." The page had two lists, This Week and This
 * Season, of what was already in orb; nothing after this week could appear on it at all.
 *
 * <b>The dates are the moments each contact is exact</b>, from {@link Transits#exactDates}
 * over one walk per planet, with a look-ahead that suits the planet's speed. Each contact is
 * placed once, at its first exact moment from now; a contact already at its peak, or past it
 * and fading, is "right now". The cards are {@link TransitCard}s, with the moment as their
 * label.
 */
final class NowTimeline {

    /** The windows, in the order the page shows them. */
    enum Window {
        RIGHT_NOW("Right now"), TODAY("Today"), TONIGHT("Tonight"), TOMORROW("Tomorrow"),
        THIS_WEEK("This week"), NEXT_WEEK("Next week"), THIS_MONTH("This month"),
        NEXT_MONTH("Next month"), LATER("Later this year");

        final String title;

        Window(String title) {
            this.title = title;
        }
    }

    /** One contact on the timeline: when it is strongest, in which window, and its card. */
    record Entry(NowCards.TransitHit hit, double jd, Window window, ReadingCard card) {
    }

    /** How many cards a window shows before "And N more". David chose three. */
    static final int PER_WINDOW = 3;

    /** The planets in the sky the timeline follows: every one with a plain card. */
    static final String[] SKY = {"Moon", "Sun", "Mercury", "Venus", "Mars", "Jupiter", "Saturn",
        "Uranus", "Neptune", "Pluto", "Chiron"};

    static final List<Aspects.Type> MAJORS = List.of(Aspects.Type.CONJUNCTION,
        Aspects.Type.SEXTILE, Aspects.Type.SQUARE, Aspects.Type.TRINE, Aspects.Type.OPPOSITION);

    private NowTimeline() {
    }

    /**
     * The timeline from the moment {@code sky} shows. With {@code sw} null it is "right now"
     * alone, which is all the page can afford while time is running.
     */
    static List<Entry> build(SwissEph sw, WheelRing sky, WheelRing natal, double[] cusps) {
        List<Entry> out = new ArrayList<>();
        if (sky == null || natal == null) {
            return out;
        }

        // Right now: in orb, and at its peak or past it.
        Set<String> current = new HashSet<>();
        for (NowCards.TransitHit h : NowCards.findHits(sky, natal, cusps)) {
            ReadingCard card = TransitCard.of(h);
            if (card == null) {
                continue;
            }
            if ("PEAK".equals(h.timingBadge()) || !h.applying()) {
                out.add(new Entry(h, Double.NaN, Window.RIGHT_NOW, card));
                current.add(TransitCard.id(h));
            }
        }
        if (sw == null || sky.sd == null) {
            return out;
        }
        double now = sky.sd.getJulDay();
        // The reader's own evening: the ring's zone when it has one, else this computer's.
        // A transit ring carries its moment but not always a zone.
        ZoneId zone = sky.time != null ? sky.time.getZone() : ZoneId.systemDefault();

        // What is coming: each contact once, at its next exact moment.
        List<Integer> points = new ArrayList<>();
        for (int n = 0; n < natal.lon.length && n < Bodies.count(); n++) {
            if (natal.valid[n] && !Bodies.at(n).isAngle()
                    && TransitCard.hasNeed(Bodies.at(n).name)) {
                points.add(n);
            }
        }
        double[] targets = new double[points.size()];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = natal.lon[points.get(i)];
        }
        Set<String> placed = new HashSet<>(current);
        List<Entry> coming = new ArrayList<>();
        for (String body : SKY) {
            int t = Bodies.indexOfName(body);
            if (t < 0) {
                continue;
            }
            for (Transits.Exact e : Transits.exactDates(sw, body, targets, MAJORS, now,
                    now + aheadDays(body))) {
                int n = points.get(e.target());
                String natalName = Bodies.at(n).name;
                if (Bodies.isOppositePair(t, n)) {
                    continue;
                }
                NowCards.TransitHit h = hitAt(sw, body, natal, n, e, cusps, now);
                String id = TransitCard.id(h);
                if (!placed.add(id)) {
                    continue;
                }
                Window w = windowOf(e.jd(), now, zone);
                ReadingCard card = TransitCard.of(h, when(w, e.jd(), zone));
                if (card != null && w != null) {
                    coming.add(new Entry(h, e.jd(), w, card));
                }
            }
        }
        coming.sort(java.util.Comparator.comparingDouble(Entry::jd));
        out.addAll(coming);
        return out;
    }

    /** How far ahead to look for a planet: far enough for its next pass, no further. */
    static double aheadDays(String body) {
        return switch (body) {
            case "Moon" -> 3.0;
            case "Sun", "Mercury", "Venus", "Mars" -> 62.0;
            default -> 365.0;
        };
    }

    /** A contact as the Now page's cards take it, at the moment it is exact. */
    private static NowCards.TransitHit hitAt(SwissEph sw, String body, WheelRing natal, int n,
            Transits.Exact e, double[] cusps, double now) {
        double lonThen = com.zodiacomputing.ourania.astro.Almanac.bodyLongitude(sw, e.jd(), body);
        double nLon = natal.lon[n];
        int tHouse = cusps != null ? Zodiac.houseOf(lonThen, cusps) : 0;
        int nHouse = cusps != null ? Zodiac.houseOf(nLon, cusps) : 0;
        return new NowCards.TransitHit(body, lonThen, 0.0, tHouse, Bodies.at(n).name, nLon,
            nHouse, e.type(), 0.0, true, NowCards.FAST_BODIES.contains(body));
    }

    /**
     * Which window a moment falls in, seen from {@code now} in the reader's own time zone.
     *
     * The day runs from 6 am; tonight is 6 pm to 6 am; a week ends on Sunday; a month on its
     * last day. Each window starts where the one before it ends, so a Sunday's "this week" is
     * empty rather than overlapping "tomorrow". Null beyond a year.
     */
    static Window windowOf(double jd, double now, ZoneId zone) {
        if (jd < now) {
            return null;
        }
        ZonedDateTime n = at(now, zone);
        ZonedDateTime t = at(jd, zone);
        ZonedDateTime dayStart = n.truncatedTo(ChronoUnit.DAYS).plusHours(6);
        if (n.isBefore(dayStart)) {
            dayStart = dayStart.minusDays(1);
        }
        ZonedDateTime tonight = dayStart.plusHours(12);
        ZonedDateTime tomorrow = dayStart.plusDays(1);
        ZonedDateTime afterTomorrow = dayStart.plusDays(2);
        ZonedDateTime nextWeek = n.truncatedTo(ChronoUnit.DAYS)
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ZonedDateTime weekAfter = nextWeek.plusWeeks(1);
        ZonedDateTime nextMonth = n.truncatedTo(ChronoUnit.DAYS)
            .with(TemporalAdjusters.firstDayOfNextMonth());
        ZonedDateTime monthAfter = nextMonth.plusMonths(1);
        ZonedDateTime yearOn = n.plusDays(365);

        Window[] order = {Window.TODAY, Window.TONIGHT, Window.TOMORROW, Window.THIS_WEEK,
            Window.NEXT_WEEK, Window.THIS_MONTH, Window.NEXT_MONTH, Window.LATER};
        ZonedDateTime[] ends = {tonight, tomorrow, afterTomorrow, nextWeek, weekAfter, nextMonth,
            monthAfter, yearOn};
        ZonedDateTime end = n;
        for (int i = 0; i < order.length; i++) {
            if (ends[i].isAfter(end)) {
                end = ends[i];
            }
            if (t.isBefore(end)) {
                return order[i];
            }
        }
        return null;
    }

    /** When a contact is strongest, in the words its window calls for. */
    static String when(Window w, double jd, ZoneId zone) {
        if (w == null) {
            return "";
        }
        ZonedDateTime t = at(jd, zone);
        return switch (w) {
            case RIGHT_NOW -> "right now";
            case TODAY, TONIGHT, TOMORROW -> "strongest at "
                + t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
            case THIS_WEEK, NEXT_WEEK -> "strongest on "
                + t.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH));
            case THIS_MONTH, NEXT_MONTH -> "strongest on "
                + t.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH));
            case LATER -> "strongest on "
                + t.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
        };
    }

    /** A Julian day, UT, as a moment in a time zone. */
    static ZonedDateTime at(double jd, ZoneId zone) {
        long ms = Math.round((jd - 2440587.5) * 86_400_000.0);
        return Instant.ofEpochMilli(ms).atZone(zone);
    }
}
