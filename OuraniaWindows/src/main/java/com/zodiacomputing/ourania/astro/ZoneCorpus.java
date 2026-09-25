package com.zodiacomputing.ourania.astro;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Known historic time zone offsets, from the historical record rather than from the runtime.
 *
 * <p><b>Why this exists (D5).</b> {@link Moments} turns a written birth time into an instant
 * through {@code ZoneId.getRules()}, which is whatever time zone database the Java runtime
 * happens to ship. That database is not part of this app, changes between releases, and - as the
 * entries below record - <b>is not always right</b>. A wrong offset is the most expensive kind of
 * defect this app can have, because nothing downstream can detect it: the chart is cast, drawn,
 * interpreted and saved, and every figure in it is confidently wrong.
 *
 * <p>An hour of real time is about fifteen degrees of Ascendant. Twenty minutes is five, which is
 * enough to change the rising sign for a birth near a cusp and to move house cusps under planets.
 *
 * <p><b>The expected offsets here are the history, not Java's opinion of it.</b> That is the whole
 * point: a corpus copied out of the runtime would agree with the runtime by construction and prove
 * nothing. Each case carries its source.
 *
 * <p><b>On the entries marked {@link Case#runtimeDisagrees}:</b> these are cases where this
 * project has measured the runtime to be wrong. They are recorded rather than silently tolerated,
 * and {@code ZoneCorpusCheck} asserts that they are <i>still</i> wrong in exactly the way recorded
 * - so that the suite goes red when the ground moves, whether the runtime is fixed or breaks
 * differently. A suite that only asserted the good cases would notice neither.
 */
public final class ZoneCorpus {

    private ZoneCorpus() {
    }

    /** What kind of local time this is, for cases that are deliberately ambiguous. */
    public enum Shape {
        /** One valid offset, the ordinary case. */
        SINGLE,
        /** The clocks went back: two valid offsets. */
        REPEATED,
        /** The clocks went forward: no valid offset. */
        SKIPPED
    }

    /** One historically attested moment and the offset that was in force. */
    public static final class Case {
        public final String place;
        public final String zoneId;
        public final LocalDateTime local;
        /** The offset the historical record gives, as {@link ZoneOffset#getId}. */
        public final String expected;
        public final Shape shape;
        /** Where the figure comes from, and why the case is interesting. */
        public final String source;
        /**
         * Set when this runtime's zone database is known to disagree, with what it says instead.
         * Null when the runtime is right.
         */
        public final String runtimeDisagrees;

        Case(String place, String zoneId, LocalDateTime local, String expected, Shape shape,
             String source, String runtimeDisagrees) {
            this.place = place;
            this.zoneId = zoneId;
            this.local = local;
            this.expected = expected;
            this.shape = shape;
            this.source = source;
            this.runtimeDisagrees = runtimeDisagrees;
        }

        /** The offset this runtime actually produces for the moment. */
        public ZoneOffset actual() {
            return ZonedDateTime.of(this.local, ZoneId.of(this.zoneId)).getOffset();
        }

        /** How many valid offsets this runtime finds - 0 skipped, 1 ordinary, 2 repeated. */
        public int validOffsets() {
            List<ZoneOffset> v = ZoneId.of(this.zoneId).getRules().getValidOffsets(this.local);
            return v.size();
        }

        @Override
        public String toString() {
            return this.place + " " + this.local;
        }
    }

    private static Case c(String place, String zone, int y, int mo, int d, int h, int mi,
                          String expected, Shape shape, String source) {
        return new Case(place, zone, LocalDateTime.of(y, mo, d, h, mi), expected, shape,
            source, null);
    }

    private static Case bad(String place, String zone, int y, int mo, int d, int h, int mi,
                            String expected, String source, String runtimeSays) {
        return new Case(place, zone, LocalDateTime.of(y, mo, d, h, mi), expected, Shape.SINGLE,
            source, runtimeSays);
    }

    /**
     * The corpus.
     *
     * <p>Chosen for the places a birth time actually goes wrong: the war years, the years before
     * a country adopted a standard zone at all, the zones that are not whole hours, and the two
     * mornings a year when a written time names two instants or none.
     */
    public static final Case[] CASES = {

        // ---- British Double Summer Time. Britain kept summer time all year from 1940 and put
        // the clocks forward a second hour each summer from 1941 to 1945, and again in 1947.
        // A birth in a British July of those years is TWO hours from GMT, not one.
        c("London, high summer of the war", "Europe/London", 1944, 7, 1, 12, 0,
            "+02:00", Shape.SINGLE,
            "Summer Time Act 1941: double summer time, mid-Feb to mid-Aug 1941-45"),
        c("London, winter of the war", "Europe/London", 1944, 1, 15, 12, 0,
            "+01:00", Shape.SINGLE,
            "Summer time was kept through the winter from 1940 to 1945"),
        c("London, the 1947 fuel crisis", "Europe/London", 1947, 7, 1, 12, 0,
            "+02:00", Shape.SINGLE,
            "Double summer time was brought back for 1947 alone"),

        // ---- United States war time: year-round daylight time, Feb 1942 to Sep 1945.
        c("New York, war time", "America/New_York", 1943, 1, 15, 12, 0,
            "-04:00", Shape.SINGLE,
            "War Time Act 1942: year-round daylight time, 9 Feb 1942 to 30 Sep 1945"),
        c("New York, the winter before", "America/New_York", 1941, 1, 15, 12, 0,
            "-05:00", Shape.SINGLE,
            "The control: the same date a year earlier is standard time"),

        // ---- Local mean time, before a country adopted a standard zone. These are not whole
        // minutes, and a chart that rounds them is a chart cast for the wrong moment.
        c("London, before the railways standardised time", "Europe/London", 1840, 6, 1, 12, 0,
            "-00:01:15", Shape.SINGLE,
            "London mean time at the Greenwich meridian's own longitude"),
        c("Paris, before it took GMT", "Europe/Paris", 1890, 6, 1, 12, 0,
            "+00:09:21", Shape.SINGLE,
            "Paris mean time, in force until 1911"),
        c("Dublin, before Dublin mean time ended", "Europe/Dublin", 1890, 6, 1, 12, 0,
            "-00:25:21", Shape.SINGLE,
            "Dublin mean time, abolished by the Time (Ireland) Act 1916"),

        // ---- Zones that are not whole hours at all.
        c("Kolkata", "Asia/Kolkata", 1975, 6, 1, 12, 0,
            "+05:30", Shape.SINGLE, "Indian Standard Time is a half hour zone"),
        c("Kathmandu", "Asia/Kathmandu", 1990, 6, 1, 12, 0,
            "+05:45", Shape.SINGLE, "Nepal is a quarter hour off the half hour"),
        c("Kolkata, wartime", "Asia/Kolkata", 1942, 10, 1, 12, 0,
            "+06:30", Shape.SINGLE,
            "India kept an hour of war time from Sep 1942 to Oct 1945"),

        // ---- A zone whose standard offset moved under the people living in it.
        c("Moscow, before the decree hour", "Europe/Moscow", 1929, 6, 1, 12, 0,
            "+02:00", Shape.SINGLE, "Moscow was two hours ahead until the 1930 decree"),
        c("Moscow, after it", "Europe/Moscow", 1935, 6, 1, 12, 0,
            "+03:00", Shape.SINGLE, "The 1930 decree added an hour and kept it for sixty years"),

        // ---- The two mornings a year a written time is not one moment. Moments reports these;
        // these cases are what prove it is being handed real ones.
        c("New York, the repeated hour", "America/New_York", 2026, 11, 1, 1, 30,
            "-04:00", Shape.REPEATED,
            "01:30 happens twice; the earlier reading is still on daylight time"),
        c("New York, the hour that did not happen", "America/New_York", 2026, 3, 8, 2, 30,
            "-04:00", Shape.SKIPPED,
            "02:30 does not exist; the clocks went straight from 02:00 to 03:00"),
        c("London, the end of double summer time", "Europe/London", 1947, 8, 10, 2, 30,
            "+02:00", Shape.REPEATED,
            "Britain went from double summer time to single on 10 Aug 1947, so 02:30 "
                + "happened twice - a historic repeated hour, not a modern one"),

        // ---- MEASURED WRONG IN THIS RUNTIME. See the class note.
        bad("Amsterdam, before the war", "Europe/Amsterdam", 1930, 1, 15, 12, 0,
            "+00:19:32",
            "The Netherlands kept Amsterdam mean time, +00:19:32, from 1909 until 1937",
            "Z - this runtime's Europe/Amsterdam rules are identical to Europe/Brussels, and "
                + "its earliest transition is Belgium's (+00:17:30 to GMT on 1 May 1892) rather "
                + "than the Netherlands'"),
        bad("Amsterdam, between the reforms", "Europe/Amsterdam", 1938, 1, 15, 12, 0,
            "+00:20",
            "The Netherlands moved to a round +00:20 on 1 July 1937, until the occupation "
                + "imposed central European time in May 1940",
            "Z - the same aliasing to Europe/Brussels"),
    };

    /**
     * A span in which this runtime's zone data is known not to match the historical record.
     *
     * <b>The point cases above are symptoms; this is the illness.</b> Two dated Amsterdam entries
     * prove the aliasing exists, but a reader's birth is not going to fall on 15 January 1930 -
     * it will fall somewhere in the forty-eight years the defect covers. So the span is declared
     * once, and {@code ZoneCorpusCheck} asserts that every recorded point case falls inside one,
     * which is what stops the two descriptions drifting apart.
     */
    public static final class Doubt {
        public final String zoneId;
        public final LocalDateTime from;
        public final LocalDateTime to;
        /** How far out the chart is, in minutes, at worst. */
        public final double minutesOut;
        /** One sentence for the reader. */
        public final String note;
        /**
         * Seconds to add to the offset this runtime reports, to get the offset history gives.
         *
         * <b>A constant, which is why the correction can be exact.</b> The runtime has the wrong
         * BASE offset for the zone and models summer time on top of it; adding the difference
         * fixes the systematic error and leaves the summer-time structure alone. Zero means the
         * disagreement is known but not correctable this way, and the chart is left as cast.
         */
        public final int correctionSeconds;

        Doubt(String zoneId, LocalDateTime from, LocalDateTime to, double minutesOut,
              int correctionSeconds, String note) {
            this.zoneId = zoneId;
            this.from = from;
            this.to = to;
            this.minutesOut = minutesOut;
            this.correctionSeconds = correctionSeconds;
            this.note = note;
        }

        boolean covers(String zone, LocalDateTime when) {
            return this.zoneId.equals(zone) && !when.isBefore(this.from) && when.isBefore(this.to);
        }
    }

    /** Every span this project has measured this runtime to get wrong. */
    /** Amsterdam mean time, the Dutch legal offset from 1909 until 1 July 1937. */
    public static final int AMSTERDAM_MEAN_TIME = 19 * 60 + 32;

    /** The round figure that replaced it, until the occupation imposed central European time. */
    public static final int DUTCH_TWENTY_MINUTES = 20 * 60;

    /**
     * <b>Two spans, because the true offset changed inside the runtime's one wrong answer.</b>
     * The Netherlands rounded Amsterdam mean time to a flat twenty minutes on 1 July 1937, so a
     * single correction across the whole period would be right for forty-five years and eight
     * seconds wrong for three.
     */
    public static final Doubt[] DOUBTS = {
        new Doubt("Europe/Amsterdam",
            LocalDateTime.of(1892, 5, 1, 0, 0), LocalDateTime.of(1937, 7, 1, 0, 0),
            19.0 + 32.0 / 60.0, AMSTERDAM_MEAN_TIME,
            "This Java runtime's zone data for the Netherlands is Belgium's - Europe/Amsterdam "
                + "and Europe/Brussels have identical rules here, and the earliest transition is "
                + "Belgium's move to GMT in 1892. The Netherlands kept Amsterdam mean time, "
                + "+00:19:32, until July 1937. <b>This chart has been corrected by that 19 "
                + "minutes 32 seconds</b>, which is about five degrees of Ascendant. Summer-time "
                + "dates in this period are still Belgium's, so if the birth is within a few "
                + "days of a spring or autumn changeover it is worth checking against a Dutch "
                + "source."),
        new Doubt("Europe/Amsterdam",
            LocalDateTime.of(1937, 7, 1, 0, 0), LocalDateTime.of(1940, 5, 16, 0, 0),
            20.0, DUTCH_TWENTY_MINUTES,
            "This Java runtime's zone data for the Netherlands is Belgium's. From July 1937 the "
                + "Dutch legal offset was a flat +00:20, until the occupation imposed central "
                + "European time in May 1940. <b>This chart has been corrected by those 20 "
                + "minutes</b>, which is about five degrees of Ascendant. Summer-time dates in "
                + "this period are still Belgium's."),
    };

    /**
     * Whether this runtime's zone data is known to be wrong for a moment, and what to say.
     *
     * <p>Returns null when there is nothing to report, which is the overwhelmingly common case.
     *
     * <p><b>This reports rather than corrects</b>, which is the same choice {@link Moments} makes
     * about ambiguous hours and for the same reason its javadoc gives: silently changing which
     * instant a chart resolves to would redraw charts people have already saved, without telling
     * them. A reader who is told can fix their own data; a reader whose chart quietly moved
     * cannot even tell that it did.
     */
    public static Doubt doubtAbout(ZoneId zone, LocalDateTime local) {
        if (zone == null || local == null) {
            return null;
        }
        for (Doubt d : DOUBTS) {
            if (d.covers(zone.getId(), local)) {
                return d;
            }
        }
        return null;
    }

    /** The cases this runtime is expected to get right. */
    public static List<Case> sound() {
        List<Case> out = new java.util.ArrayList<>();
        for (Case c : CASES) {
            if (c.runtimeDisagrees == null) {
                out.add(c);
            }
        }
        return out;
    }

    /** The cases this runtime is known to get wrong. */
    public static List<Case> known() {
        List<Case> out = new java.util.ArrayList<>();
        for (Case c : CASES) {
            if (c.runtimeDisagrees != null) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * How far out a chart is, in degrees of Ascendant, for a given error in minutes.
     *
     * <b>The reason any of this matters, in one line.</b> The Ascendant moves through the whole
     * zodiac in a day, so a minute of real time is a quarter of a degree of it - more at high
     * latitudes, where the ecliptic rises obliquely, and this is the gentle figure.
     */
    public static double ascendantDegreesFor(double minutesOut) {
        return minutesOut * 360.0 / (24.0 * 60.0);
    }
}
