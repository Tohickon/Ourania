package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A date, a time and a place name, turned into a cast chart (G7).
 *
 * <p><b>Written because the third copy was about to be.</b> The chart form does this, and
 * {@code ElectionalPanel} does it again a hundred lines of its own, and a placement search over the
 * chart book needs it a third time. Three copies of "what instant is this, and where" in a program
 * whose most logged defect is one rule in two places would have been asking for it.
 *
 * <p><b>The instant is local to the PLACE, not to this computer.</b> A chart saved for a birth in
 * Sydney is read on a Sydney clock; resolving it in the system zone moves it by hours and the
 * Ascendant by a whole sign. {@code ElectionalPanel} had that right for its own screen and its
 * reasoning is kept here verbatim, because it is the thing this class exists to stop being
 * re-derived.
 *
 * <p><b>Cast charts are cached by what they were cast from.</b> A placement search walks the whole
 * book on every keystroke that parses, and the book is allowed to be large; casting twenty charts
 * per keystroke is the difference between a search box and a hang. The key is the three strings, so
 * a chart whose data is edited is cast again.
 */
final class Moments {

    private Moments() { }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * The zone a place keeps, or this computer's when the atlas has no opinion.
     *
     * <b>Moved here from {@code ElectionalPanel} on 28 September</b>, which now asks this, so the
     * election screen and the book search cannot disagree about what hour a moment was.
     */
    static ZoneId zoneOf(Geocoder.Result where) {
        if (where != null && where.tzId != null && !where.tzId.trim().isEmpty()) {
            try {
                return ZoneId.of(where.tzId.trim());
            } catch (Exception ignored) {
                // An unknown zone id is not worth refusing a chart over.
            }
        }
        return ZoneId.systemDefault();
    }

    /** The Julian day of a local moment at a place, or NaN when it cannot be read. */
    static double julianDay(String date, String time, Geocoder.Result where) {
        try {
            String t = time == null || time.trim().isEmpty() ? "12:00" : time.trim();
            String[] hm = t.split(":");
            LocalDateTime local = LocalDateTime.of(
                java.time.LocalDate.parse(date.trim(), DATE),
                java.time.LocalTime.of(Integer.parseInt(hm[0].trim()),
                    hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0));
            ZonedDateTime utc = local.atZone(zoneOf(where))
                .withZoneSameInstant(ZoneId.of("UTC"));
            return new SweDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
                utc.getHour() + utc.getMinute() / 60.0).getJulDay();
        } catch (Exception cannotRead) {
            return Double.NaN;
        }
    }

    /** The cast charts, by the three strings they came from. */
    private static final java.util.Map<String, ChartFrame> CAST =
        java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, ChartFrame>() {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, ChartFrame> e) {
                // Enough for a large book, bounded so a long session cannot grow without end.
                return size() > 500;
            }
        });

    /** One SwissEph for every cast here, as the app uses one: the library caches per instance. */
    private static volatile SwissEph sw;

    /**
     * The chart a saved entry stands for, or null when it cannot be cast.
     *
     * <b>Null rather than an exception, because the caller is a search box.</b> A place the atlas
     * has never heard of and a date somebody typed wrong are both ordinary, and a book search that
     * stopped at the first of them would be useless on exactly the books that need it.
     *
     * <b>Blocking.</b> The atlas answers from disk for a known town, and the network only for what
     * it does not have - so this must not run on the event thread.
     */
    static ChartFrame frameOf(SavedCharts.Entry e) {
        if (e == null) {
            return null;
        }
        String key = e.date + "\u0000" + e.time + "\u0000" + e.location;
        ChartFrame cached = CAST.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Geocoder.Result where = Geocoder.lookup(e.location);
            if (where == null) {
                return null;
            }
            double jd = julianDay(e.date, e.time, where);
            if (Double.isNaN(jd)) {
                return null;
            }
            if (sw == null) {
                sw = new SwissEph(Ephemeris.PATH);
            }
            // Placidus and no relocation: the book stores a birth, and a search is about where
            // the bodies were, not about a house system the reader may have changed since.
            ChartFrame f = ChartFrame.compute(sw, jd, where.lat, where.lon, 'P', false, 0.0);
            CAST.put(key, f);
            return f;
        } catch (Exception cannotCast) {
            return null;
        }
    }

    /** Drops the cast charts. For the check, and for a book that has been edited wholesale. */
    static void forget() {
        CAST.clear();
    }

    /**
     * The saved charts answering a placement question, in the book's own order.
     *
     * <b>Charts that cannot be cast are left out and counted, not hidden.</b> The caller says how
     * many were unreadable, because "no results" and "none of your charts could be cast" are
     * different answers and a search box that gives the first for the second is lying.
     */
    static java.util.List<String> matching(PlacementQuery q, int[] unreadable) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (unreadable != null && unreadable.length > 0) {
            unreadable[0] = 0;
        }
        if (q == null) {
            return out;
        }
        for (String name : SavedCharts.names()) {
            SavedCharts.Entry e = SavedCharts.get(name);
            if (e == null) {
                continue;
            }
            ChartFrame f = frameOf(e);
            if (f == null) {
                if (unreadable != null && unreadable.length > 0) {
                    unreadable[0]++;
                }
                continue;
            }
            if (q.matches(f)) {
                out.add(name);
            }
        }
        return out;
    }
}
