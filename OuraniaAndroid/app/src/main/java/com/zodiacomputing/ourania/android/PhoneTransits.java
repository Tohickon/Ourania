package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Moments;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.InterpretationService;

import de.thmac.swisseph.SwissEph;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The sky at a moment laid over a birth chart (M7): the transit ring's positions, the contacts
 * it makes to the chart, the profected year, and their readings.
 *
 * <p><b>The desktop's transits, not a second finder.</b> The contacts are
 * {@link Transits#toNatal} with the natal chart's own prominence ranking and the lord of the
 * year - the call the desktop's reading makes - so the phone lists the same transits, in the
 * same order, at the same one-degree orb. The words are the transit corpus's
 * ({@link InterpretationService#getTransitAspect} and its kin).
 *
 * <p>The sky is cast at the birth place: the planets' longitudes do not depend on where they
 * are seen from, and the houses they fall in are the natal chart's.
 *
 * <p>No Android in here, like {@link PhoneChart}: {@code PhoneTransitsTest} runs it on the JVM.
 */
final class PhoneTransits {

    /** The sky, its contacts to the chart, and the year - everything the screen shows. */
    static final class Sky {
        final ZonedDateTime when;
        final ChartFrame frame;
        final List<Transits.Hit> hits;
        /** The profected year, or null when the birth time is unknown (it needs the Ascendant). */
        final Profection year;

        Sky(ZonedDateTime when, ChartFrame frame, List<Transits.Hit> hits, Profection year) {
            this.when = when;
            this.frame = frame;
            this.hits = hits;
            this.year = year;
        }
    }

    private PhoneTransits() { }

    /** The sky at {@code when} over the chart {@code natal}. */
    static Sky at(SwissEph sw, PhoneChart.Cast natal, ZonedDateTime when) {
        double jd = Moments.sweDate(when).getJulDay();
        ChartFrame sky = ChartFrame.compute(sw, jd, natal.place.latitude, natal.place.longitude,
            PhoneChart.HOUSE_SYSTEM, false, 0.0);
        Profection year = null;
        if (!natal.timeUnknown) {
            double birthJd = Moments.sweDate(natal.moment.when).getJulDay();
            year = Profection.at(birthJd, jd, natal.frame.asc);
        }
        List<BodyScore.Vector> ranked = BodyScore.rank(natal.frame, Gestalt.compute(natal.frame));
        List<Transits.Hit> hits = Transits.toNatal(natal.frame, sky, ranked,
            year == null ? null : year.lord);
        if (natal.timeUnknown) {
            // No birth time, no angles: a contact to an Ascendant cast for noon is a contact
            // to nothing.
            hits.removeIf(h -> "angle".equals(h.why));
        }
        return new Sky(when, sky, hits, year);
    }

    /** "Transiting Saturn square natal Sun" - how a contact is named everywhere it is shown. */
    static String name(Transits.Hit h) {
        return "Transiting " + h.transiting + " " + h.type.label.toLowerCase() + " natal "
            + h.natal;
    }

    /** Every contact, the year, with their readings: the "Transits now" page. */
    static String reading(PhoneChart.Cast natal, Sky sky, InterpretationService svc) {
        StringBuilder h = new StringBuilder();
        h.append("<h2>Transits, ").append(sky.when.format(DateTimeFormatter.ofPattern(
            "d MMM yyyy, HH:mm"))).append("</h2>");
        if (sky.year != null) {
            h.append("<p><b>Your year:</b> age ").append(sky.year.age).append(", a house ")
                .append(sky.year.house).append(" year (")
                .append(capital(Zodiac.SIGNS[sky.year.sign])).append(" profected), ruled by <b>")
                .append(sky.year.lord).append("</b> - contacts from or to ")
                .append(sky.year.lord).append(" weigh more this year.</p>");
        }
        if (sky.hits.isEmpty()) {
            h.append("<p>Nothing in the sky is within a degree of an aspect to the important "
                + "points of this chart right now. Try another date, or tap a planet on the "
                + "outer ring to read where it is passing through.</p>");
            return h.toString();
        }
        for (Transits.Hit hit : sky.hits) {
            contact(h, hit, svc);
        }
        return h.toString();
    }

    private static void contact(StringBuilder h, Transits.Hit hit, InterpretationService svc) {
        h.append("<h4>").append(name(hit)).append(String.format(
            " <small>(%.1f&deg;, %s%s)</small></h4>", hit.offBy,
            hit.applying ? "applying" : "separating",
            hit.transitRetrograde ? ", retrograde" : ""));
        String text = svc.getTransitAspect(hit.transiting, hit.natal, hit.type.label);
        if (PhoneReading.usable(text)) {
            h.append("<p>").append(text).append("</p>");
        }
    }

    /** A planet on the outer ring: where it is passing through, and what it is touching. */
    static String skyPlanet(PhoneChart.Cast natal, Sky sky, int i, InterpretationService svc) {
        if (i < 0 || i >= PhoneChart.PLANETS || i >= sky.frame.bodies.length) {
            return "";
        }
        ChartFrame.Body b = sky.frame.bodies[i];
        if (b == null || !b.ok) {
            return "";
        }
        String sign = capital(Zodiac.signName(b.lon));
        int house = natal.timeUnknown ? 0 : Zodiac.houseOf(b.lon, natal.frame.cusps);
        StringBuilder h = new StringBuilder();
        h.append("<h2>Transiting ").append(b.name).append(" in ").append(sign);
        if (house > 0) {
            h.append(", your house ").append(house);
        }
        h.append("</h2><p><i>").append(Zodiac.format(b.lon));
        if (b.retrograde) {
            h.append(", retrograde");
        }
        h.append(" &middot; ").append(sky.when.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
            .append("</i></p>");
        String inSign = svc.getTransit(b.name, sign);
        if (PhoneReading.usable(inSign)) {
            h.append("<h3>").append(b.name).append(" through ").append(sign).append("</h3><p>")
                .append(inSign).append("</p>");
        }
        if (house > 0) {
            String inHouse = svc.getTransitInHouse(b.name, house);
            if (PhoneReading.usable(inHouse)) {
                h.append("<h3>Through your house ").append(house).append("</h3><p>")
                    .append(inHouse).append("</p>");
            }
        }
        List<Transits.Hit> mine = new ArrayList<>();
        for (Transits.Hit hit : sky.hits) {
            if (hit.transiting.equals(b.name)) {
                mine.add(hit);
            }
        }
        if (!mine.isEmpty()) {
            h.append("<h3>Touching your chart</h3>");
            for (Transits.Hit hit : mine) {
                contact(h, hit, svc);
            }
        }
        return h.toString();
    }

    /** One line for the wheel's caption: the tightest contact, or how many there are. */
    static String headline(Sky sky) {
        if (sky.hits.isEmpty()) {
            return "No close transits to this chart.";
        }
        Transits.Hit first = sky.hits.get(0);
        return name(first) + (sky.hits.size() > 1
            ? " and " + (sky.hits.size() - 1) + " more" : "");
    }

    /** The local time now at a place. */
    static ZonedDateTime now(String zoneId) {
        return ZonedDateTime.now(ZoneId.of(zoneId));
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
