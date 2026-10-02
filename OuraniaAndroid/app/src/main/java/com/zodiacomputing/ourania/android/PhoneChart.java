package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Moments;
import com.zodiacomputing.ourania.astro.PlainText;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;
import com.zodiacomputing.ourania.gui.Settings;

import de.thmac.swisseph.SwissEph;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * A chart cast on the phone from a birth date, time and place, and the placements written out
 * (M3).
 *
 * <p><b>No Android in here, on purpose.</b> Everything the phone does with the engine goes
 * through this class, and it imports nothing from {@code android.*} - so the whole path from a
 * birth certificate to a list of placements runs, and is tested, on an ordinary JVM with no
 * phone or emulator: {@code PhoneChartTest}. The screen only gathers the three answers and shows
 * the text.
 *
 * <p><b>The engine decides, not this class.</b> A written local time becomes an instant through
 * {@link Moments#resolve} - the desktop's own path, with the skipped and repeated hours named
 * and the historical zone corrections applied (D5) - and the chart is {@link ChartFrame}'s. A
 * birth with no known time is cast for noon with the angles and houses withheld, as the desktop
 * does it.
 */
final class PhoneChart {

    private PhoneChart() { }

    /**
     * The reader's house system, from the settings file both apps read (M9) - Placidus, the
     * desktop's default, until one is chosen.
     */
    static char houseSystem() {
        return Settings.houseSystem();
    }

    /** How many of the registry's points the list shows: the ten planets. */
    static final int PLANETS = 10;

    /** A cast chart and what had to be assumed to cast it. */
    static final class Cast {
        final ChartFrame frame;
        final Moments.Resolved moment;
        final Atlas.Place place;
        final boolean timeUnknown;

        Cast(ChartFrame frame, Moments.Resolved moment, Atlas.Place place, boolean timeUnknown) {
            this.frame = frame;
            this.moment = moment;
            this.place = place;
            this.timeUnknown = timeUnknown;
        }
    }

    /**
     * Casts a birth chart.
     *
     * @param time the time on the birth certificate, or null when it is not known
     */
    static Cast cast(SwissEph sw, LocalDate date, LocalTime time, Atlas.Place place) {
        boolean unknown = time == null;
        LocalTime t = unknown ? Moments.unknownTime() : time;
        Moments.Resolved r = Moments.resolve(date, t, ZoneId.of(place.zoneId), place.longitude);
        double jd = Moments.sweDate(r.when).getJulDay();
        ChartFrame f = unknown
            ? ChartFrame.computeTimeUnknown(sw, jd, place.latitude, place.longitude,
                houseSystem(), false, 0.0)
            : ChartFrame.compute(sw, jd, place.latitude, place.longitude,
                houseSystem(), false, 0.0);
        return new Cast(f, r, place, unknown);
    }

    /** "Sydney, New South Wales, Australia" - what the place box shows for a place. */
    static String label(Atlas.Place p) {
        StringBuilder sb = new StringBuilder(p.name);
        if (p.region != null && !p.region.isEmpty() && !p.region.equals(p.name)) {
            sb.append(", ").append(p.region);
        }
        if (p.country != null && !p.country.isEmpty()) {
            sb.append(", ").append(p.country);
        }
        return sb.toString();
    }

    /**
     * One planet as text: its place, retrograde, house, and its reading on the next line - what
     * the list shows for it and what tapping it on the wheel shows. Empty for one not cast.
     */
    static String planet(Cast c, int i, InterpretationService svc) {
        if (i < 0 || i >= PLANETS || i >= Bodies.count() || i >= c.frame.bodies.length) {
            return "";
        }
        ChartFrame.Body b = c.frame.bodies[i];
        if (b == null || !b.ok) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(b.name).append("  ").append(Zodiac.format(b.lon));
        if (b.retrograde) {
            sb.append("  R");
        }
        int house = c.timeUnknown ? 0 : Zodiac.houseOf(b.lon, c.frame.cusps);
        if (house > 0) {
            sb.append("  house ").append(house);
        }
        sb.append('\n');
        String reading = PlainText.summary(svc.getPlanetInSign(b.name, Zodiac.signName(b.lon)));
        if (!reading.isEmpty()) {
            sb.append("   ").append(reading).append('\n');
        }
        return sb.toString();
    }

    /** The chart as text: when and where, anything assumed, then each planet with its reading. */
    static String describe(Cast c, InterpretationService svc) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter when = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
        sb.append(label(c.place)).append('\n');
        if (c.timeUnknown) {
            sb.append(c.moment.when.toLocalDate()).append(", time unknown\n");
        } else {
            sb.append(c.moment.when.format(when)).append(" local (")
              .append(c.moment.when.getOffset()).append("), ")
              .append(c.moment.when.withZoneSameInstant(ZoneOffset.UTC).format(when))
              .append(" UT\n");
        }
        // Said before the placements, because each changes what they mean.
        for (String note : new String[] {c.moment.note, c.moment.zoneDataDoubt,
                c.timeUnknown ? c.frame.timeUnknownNote : null}) {
            if (note != null && !note.isEmpty()) {
                sb.append('\n').append(note).append('\n');
            }
        }
        sb.append('\n');
        for (int i = 0; i < PLANETS; i++) {
            String p = planet(c, i, svc);
            if (!p.isEmpty()) {
                sb.append(p).append('\n');
            }
        }
        if (!c.timeUnknown) {
            sb.append("Ascendant  ").append(Zodiac.format(c.frame.asc)).append('\n');
            sb.append("Midheaven  ").append(Zodiac.format(c.frame.mc)).append('\n');
        }
        return sb.toString();
    }
}
