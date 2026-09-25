package com.zodiacomputing.ourania.astro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Turning a written date and time into a real instant, and saying so when that is not certain.
 *
 * <b>A local time is not always one moment.</b> Twice a year every zone that observes summer
 * time either skips an hour or repeats one. In the repeated hour a written time names two
 * instants an hour apart; in the skipped hour it names none. Java resolves both silently -
 * {@code ZonedDateTime.of} shifts a skipped time forward and picks the earlier offset for a
 * repeated one - and a chart is one of the few places where that silence is expensive: an hour
 * of real time is about fifteen degrees of Ascendant, which moves most of the houses.
 *
 * Measured for America/New_York: 01:30 on 1 Nov 2026 resolves to -04:00, and the other reading
 * at -05:00 is sixty minutes of real time away. 02:30 on 8 Mar 2026 does not exist and becomes
 * 03:30. Neither was reported to anyone.
 *
 * This class does not choose better than Java does - the same reading comes back by default,
 * because changing which instant an existing chart resolves to would silently redraw charts
 * people have already saved. What it adds is that the caller can now know, and say.
 */
public final class Moments {

    /** What kind of local time this was. */
    public enum Kind {
        /** One instant, as usual. */
        NORMAL,
        /** The clocks went forward: this local time never happened. */
        SKIPPED,
        /** The clocks went back: this local time happened twice. */
        REPEATED
    }

    /** A resolved instant, with what had to be assumed to get it. */
    public static final class Resolved {
        public final ZonedDateTime when;
        public final Kind kind;
        /** The instant not chosen, for a repeated hour. Null otherwise. */
        public final ZonedDateTime other;
        /** One sentence for a reader, or null when nothing had to be assumed. */
        public final String note;
        /**
         * Set when this Java runtime's zone database is known not to match the historical
         * record for this moment, with a sentence saying so. Null the overwhelming majority of
         * the time. See {@link ZoneCorpus}.
         *
         * <b>Separate from {@link #note}</b>, because they are different kinds of doubt: a note
         * says the clocks were ambiguous that morning, which is a fact about the world; this
         * says the software's own data is wrong, which is a fact about the software. A reader
         * can act on the first by checking a birth certificate and on the second only by not
         * trusting the chart.
         */
        public final String zoneDataDoubt;

        Resolved(ZonedDateTime when, Kind kind, ZonedDateTime other, String note) {
            this(when, kind, other, note, null);
        }

        Resolved(ZonedDateTime when, Kind kind, ZonedDateTime other, String note,
                 String zoneDataDoubt) {
            this.when = when;
            this.kind = kind;
            this.other = other;
            this.note = note;
            this.zoneDataDoubt = zoneDataDoubt;
        }

        /** True when the caller assumed something a person might want to correct. */
        public boolean uncertain() {
            return kind != Kind.NORMAL || this.zoneDataDoubt != null;
        }
    }

    private Moments() { }

    /**
     * Resolves a local date and time in a zone, reporting any daylight-saving ambiguity.
     *
     * @param date  the local date, and
     * @param time  the local time as written on a birth certificate
     * @param zone  the zone of the place it was written in
     */
    public static Resolved resolve(LocalDate date, LocalTime time, ZoneId zone) {
        LocalDateTime local = LocalDateTime.of(date, time);
        List<ZoneOffset> valid = zone.getRules().getValidOffsets(local);

        // <b>Whether the runtime's own zone data is trustworthy here.</b> Asked for every moment
        // and almost always null; when it is not, the chart is cast anyway and the reader is
        // told, because the alternative - quietly applying a correction - would move charts
        // people have already saved without saying so.
        ZoneCorpus.Doubt doubt = ZoneCorpus.doubtAbout(zone, local);
        String doubtNote = doubt == null ? null : doubt.note;

        if (valid.size() == 1) {
            ZonedDateTime when = ZonedDateTime.of(local, zone);
            if (doubt != null && doubt.correctionSeconds != 0) {
                when = corrected(local, when, doubt);
            }
            return new Resolved(when, Kind.NORMAL, null, null, doubtNote);
        }

        if (valid.isEmpty()) {
            // The clocks went forward over this time. Java moves it ahead by the size of the
            // gap, which is the only defensible guess - the hour genuinely did not exist - but
            // it means the chart is cast for a time nobody wrote down.
            ZonedDateTime shifted = ZonedDateTime.of(local, zone);
            String moved = shifted.toLocalTime().toString();
            return new Resolved(shifted, Kind.SKIPPED, null,
                "The clocks went forward that morning and " + time + " did not occur in "
                    + zone.getId() + ". The chart is cast for " + moved + " instead. If the "
                    + "birth time is right, the date or the place may not be.", doubtNote);
        }

        // Two valid offsets: the hour was repeated. Java takes the earlier, which is the one
        // still on summer time; the later reading is an hour after it in real time.
        ZonedDateTime earlier = ZonedDateTime.of(local, zone).withEarlierOffsetAtOverlap();
        ZonedDateTime later = earlier.withLaterOffsetAtOverlap();
        return new Resolved(earlier, Kind.REPEATED, later,
            "The clocks went back that night, so " + time + " happened twice in "
                + zone.getId() + ". This chart uses the first (" + earlier.getOffset()
                + "); the second (" + later.getOffset() + ") is an hour later in real time "
                + "and moves the Ascendant by roughly fifteen degrees.", doubtNote);
    }

    /**
     * The same local time, read at the offset history gives rather than the one this runtime has.
     *
     * <b>Applied, not merely reported - David, 25 Sep.</b> The earlier design followed this
     * class's own precedent for ambiguous hours and left the chart as Java cast it. That
     * precedent does not actually cover this case: an ambiguous hour is a fact about the world
     * that the software cannot resolve for the reader, while a wrong zone database is a fact
     * about the software, which it can.
     *
     * <b>The local time is preserved and the instant moves</b>, which is the right way round:
     * what the reader knows is the time on the clock in the room, and what was wrong is the
     * conversion from that to real time. A birth written 12:00 in Amsterdam in 1930 stays 12:00
     * and becomes an instant 19 minutes 32 seconds earlier in UT than this runtime believed.
     *
     * <p>The returned moment carries a fixed offset rather than the named zone, because the named
     * zone is precisely what is wrong here - handing it back would let any later re-resolution
     * undo the correction silently.
     */
    private static ZonedDateTime corrected(LocalDateTime local, ZonedDateTime asRuntimeHasIt,
                                           ZoneCorpus.Doubt doubt) {
        ZoneOffset trueOffset = ZoneOffset.ofTotalSeconds(
            asRuntimeHasIt.getOffset().getTotalSeconds() + doubt.correctionSeconds);
        return local.atOffset(trueOffset).toZonedDateTime();
    }

    /**
     * The other reading of a repeated hour, or the same instant when there is only one.
     *
     * Lets a caller offer the choice without re-deriving which two instants were candidates.
     */
    public static ZonedDateTime alternative(Resolved r) {
        return r != null && r.other != null ? r.other : (r == null ? null : r.when);
    }

    /**
     * Noon, for a chart whose birth time is not known.
     *
     * <b>Noon rather than midnight, and that is the convention for a reason.</b> An unknown
     * time is somewhere in a 24-hour day, so the cast moment should be the one that minimises
     * the worst-case error - the middle of the day, not an end of it. It also keeps the Moon,
     * which moves about half a degree an hour, within about six degrees of wherever it really
     * was, and keeps the date unambiguous: midnight sits on the boundary between two days and
     * invites the off-by-one that a birth certificate's date is meant to settle.
     */
    public static LocalTime unknownTime() {
        return LocalTime.NOON;
    }
}
