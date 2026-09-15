package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A month of one person's transits, day by day: master list F2, "Personal transit calendar - a
 * month view flagging high-intensity days for the loaded chart, not the sky in general."
 *
 * <p><b>Built on {@link TransitSearch}, not beside it.</b> The search already finds every passage of
 * a transiting planet through orb of a natal point, its exact moments and its stations; this reads
 * those passages a day at a time. A second transit scanner here would be the defect the project
 * logs more than any other.
 *
 * <p><b>A day's score</b> is the sum over every transit in orb that day of
 * <i>weight</i> &times; <i>closeness</i>, plus <i>weight</i> again for each one that goes exact that
 * day. Closeness is 1 at exact and 0 at the edge of the orb, measured at local noon. Weight is the
 * transiting planet's (the slow ones carry more: a Pluto square is years of a life, a Venus trine an
 * afternoon) times the aspect's (conjunction, square and opposition a whole; trine and sextile
 * 0.6) times the natal point's (the Sun, Moon, Ascendant and MC half again).
 *
 * <p><b>Those weights are a judgement and have not been calibrated</b> - the same standing as the
 * orbs everywhere else in this project. They are named constants so a measured scheme has somewhere
 * to land. The levels are relative to the month: a day is <i>intense</i> when it is among the
 * month's top fifth and scores at least {@link #INTENSE_FLOOR}, and <i>notable</i> above
 * {@link #NOTABLE_FLOOR}; a quiet month has no intense days rather than a manufactured few.
 */
public final class TransitCalendar {

    private TransitCalendar() { }

    /** The planets a calendar follows: the transit search's, without the Moon, which is volume. */
    public static final List<String> TRANSITING = Arrays.asList(TransitSearch.TRANSITING);

    /** The natal points a calendar watches: the planets and the two angles. */
    public static final List<String> NATAL = Arrays.asList("Sun", "Moon", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Ascendant", "MC");

    public static final double NOTABLE_FLOOR = 1.5;
    public static final double INTENSE_FLOOR = 3.0;
    /** The share of a month's scoring days that may be called intense. */
    public static final double INTENSE_SHARE = 0.2;

    public enum Level { QUIET, NOTABLE, INTENSE }

    /** How much a transiting planet carries. */
    public static double bodyWeight(String body) {
        switch (body) {
            case "Pluto": case "Neptune": case "Uranus": return 5.0;
            case "Saturn":                               return 4.0;
            case "Jupiter": case "Chiron":               return 3.0;
            case "North Node": case "Mars":              return 2.0;
            default:                                     return 1.0;
        }
    }

    public static double aspectWeight(Aspects.Type t) {
        return t == Aspects.Type.TRINE || t == Aspects.Type.SEXTILE ? 0.6 : 1.0;
    }

    public static double pointWeight(String natal) {
        switch (natal) {
            case "Sun": case "Moon": case "Ascendant": case "MC": return 1.5;
            default:                                              return 1.0;
        }
    }

    public static double weight(TransitSearch.Passage p) {
        return bodyWeight(p.transiting) * aspectWeight(p.type) * pointWeight(p.natal);
    }

    /** One transit on one day. */
    public static final class Contact {
        public final TransitSearch.Passage passage;
        /** Degrees from exact at the day's local noon. */
        public final double offAtNoon;
        /** The exact moment that falls on this day, or NaN. */
        public final double exactJd;
        public final double score;

        Contact(TransitSearch.Passage passage, double offAtNoon, double exactJd, double score) {
            this.passage = passage;
            this.offAtNoon = offAtNoon;
            this.exactJd = exactJd;
            this.score = score;
        }

        public boolean exactToday() {
            return !Double.isNaN(this.exactJd);
        }
    }

    public static final class Day {
        public final LocalDate date;
        public final double startJd;
        public final double endJd;
        public final List<Contact> contacts = new ArrayList<>();
        public double score;
        public Level level = Level.QUIET;

        Day(LocalDate date, double startJd, double endJd) {
            this.date = date;
            this.startJd = startJd;
            this.endJd = endJd;
        }
    }

    public static final class Month {
        public final YearMonth month;
        public final ZoneId zone;
        public final double orb;
        public final List<Day> days = new ArrayList<>();
        /** Every passage the month touches, as the search returned them. */
        public final List<TransitSearch.Passage> passages = new ArrayList<>();

        Month(YearMonth month, ZoneId zone, double orb) {
            this.month = month;
            this.zone = zone;
            this.orb = orb;
        }
    }

    /** The Julian day (UT) of a local midnight. */
    public static double jdOf(LocalDate date, ZoneId zone) {
        long epochSecond = date.atStartOfDay(zone).toEpochSecond();
        return 2440587.5 + epochSecond / 86400.0;
    }

    /** How far a passage's transiting planet is from exact at a moment, in degrees. */
    public static double offExact(SwissEph sw, TransitSearch.Passage p, double jd) {
        double lon = Almanac.bodyLongitude(sw, jd, p.transiting);
        double sep = Aspects.separation(lon, p.natalLongitude);
        return Math.abs(sep - p.type.exactAngle);
    }

    public static Month month(SwissEph sw, ChartFrame natal, YearMonth ym, ZoneId zone, double orb) {
        Month m = new Month(ym, zone, orb);
        double from = jdOf(ym.atDay(1), zone);
        double to = jdOf(ym.plusMonths(1).atDay(1), zone);
        m.passages.addAll(TransitSearch.search(sw, natal, TRANSITING, NATAL,
            Arrays.asList(TransitSearch.MAJOR), orb, from, to));
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            Day day = new Day(date, jdOf(date, zone), jdOf(date.plusDays(1), zone));
            double noon = (day.startJd + day.endJd) / 2.0;
            for (TransitSearch.Passage p : m.passages) {
                double enters = Double.isNaN(p.enters) ? Double.NEGATIVE_INFINITY : p.enters;
                double leaves = Double.isNaN(p.leaves) ? Double.POSITIVE_INFINITY : p.leaves;
                if (enters >= day.endJd || leaves < day.startJd) {
                    continue;
                }
                double exact = Double.NaN;
                for (TransitSearch.Exact e : p.exacts) {
                    if (e.jd >= day.startJd && e.jd < day.endJd) {
                        exact = e.jd;
                        break;
                    }
                }
                double off = offExact(sw, p, noon);
                double closeness = Math.max(0.0, 1.0 - off / orb);
                double score = weight(p) * closeness + (Double.isNaN(exact) ? 0.0 : weight(p));
                if (score <= 0.0) {
                    continue;
                }
                day.contacts.add(new Contact(p, off, exact, score));
                day.score += score;
            }
            day.contacts.sort((a, b) -> Double.compare(b.score, a.score));
            m.days.add(day);
        }
        levels(m.days);
        return m;
    }

    /** Sets each day's level from its score and the month's spread. */
    static void levels(List<Day> days) {
        List<Double> scoring = new ArrayList<>();
        for (Day d : days) {
            if (d.score > 0) {
                scoring.add(d.score);
            }
        }
        scoring.sort(null);
        int topCount = (int) Math.ceil(scoring.size() * INTENSE_SHARE);
        double cut = topCount == 0 ? Double.POSITIVE_INFINITY : scoring.get(scoring.size() - topCount);
        for (Day d : days) {
            if (d.score >= INTENSE_FLOOR && d.score >= cut) {
                d.level = Level.INTENSE;
            } else if (d.score >= NOTABLE_FLOOR) {
                d.level = Level.NOTABLE;
            } else {
                d.level = Level.QUIET;
            }
        }
    }
}
