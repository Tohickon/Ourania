package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Chronometry;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Moments;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.astro.ZodiacalReleasing;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import com.zodiacomputing.ourania.gui.InterpretationService;
import com.zodiacomputing.ourania.gui.NarrativeSynthesizer;

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
        /** The engine's whole reckoning of the moment; null unless the year was scanned. */
        final Chronometry time;

        Sky(ZonedDateTime when, ChartFrame frame, List<Transits.Hit> hits, Profection year,
                Chronometry time) {
            this.when = when;
            this.frame = frame;
            this.hits = hits;
            this.year = year;
            this.time = time;
        }
    }

    private PhoneTransits() { }

    /** The sky at {@code when} over the chart {@code natal}, without the year scan. */
    static Sky at(SwissEph sw, PhoneChart.Cast natal, ZonedDateTime when) {
        return at(sw, natal, when, false);
    }

    /**
     * The sky at {@code when} over the chart {@code natal}.
     *
     * <p>With a birth time this is {@link Chronometry#at} - the call the desktop's reading
     * worker makes - so the year, its months and days, the transit contacts and, when
     * {@code scanYear}, the year scan and its convergence are the desktop's. Without one there
     * is no Ascendant to profect from, so there is no year and no lord to weigh the contacts,
     * and the contacts to the angles - angles of a chart cast for noon - are left out.
     */
    static Sky at(SwissEph sw, PhoneChart.Cast natal, ZonedDateTime when, boolean scanYear) {
        double jd = Moments.sweDate(when).getJulDay();
        List<BodyScore.Vector> ranked = BodyScore.rank(natal.frame, Gestalt.compute(natal.frame));
        double lat = natal.place.latitude;
        double lon = natal.place.longitude;
        if (!natal.timeUnknown) {
            double birthJd = Moments.sweDate(natal.moment.when).getJulDay();
            Chronometry time = Chronometry.at(sw, natal.frame, birthJd, jd, ranked, jd, lat, lon,
                lat, lon, PhoneChart.HOUSE_SYSTEM, scanYear);
            return new Sky(when, time.transit, time.hits, time.profection,
                scanYear ? time : null);
        }
        ChartFrame sky = ChartFrame.compute(sw, jd, lat, lon, PhoneChart.HOUSE_SYSTEM, false,
            0.0);
        List<Transits.Hit> hits = Transits.toNatal(natal.frame, sky, ranked, null);
        hits.removeIf(h -> "angle".equals(h.why));
        return new Sky(when, sky, hits, null, null);
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
                .append(sky.year.lord).append(" weigh more this year.");
            if (sky.year.monthlyLord != null) {
                h.append(" This month is ruled by <b>").append(sky.year.monthlyLord)
                    .append("</b>");
                if (sky.year.dailyLord != null) {
                    h.append(", today by <b>").append(sky.year.dailyLord).append("</b>");
                }
                h.append('.');
            }
            h.append("</p>");
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

    /** One dated line of the calendar. */
    private static final class Dated {
        final double jd;
        final String text;

        Dated(double jd, String text) {
            this.jd = jd;
            this.text = text;
        }
    }

    /**
     * The rest of the profection year, month by month: each moment a transit becomes exact on
     * this chart, and each eclipse, lunation and station that lands on it - the year scan's
     * perfections and events, from {@code sky.when} to the next birthday's solar return.
     * Needs a sky cast with the year scanned; "" without one.
     */
    static String calendar(Sky sky) {
        if (sky.time == null || sky.time.scan == null) {
            return "";
        }
        double from = Moments.sweDate(sky.when).getJulDay();
        List<Dated> all = new ArrayList<>();
        for (Transits.Perfection p : sky.time.scan.perfections) {
            if (p.jd >= from) {
                all.add(new Dated(p.jd, "Transiting " + p.transiting
                    + (p.retrograde ? " (retrograde)" : "") + " " + p.type.label.toLowerCase()
                    + " natal " + p.natal + " - exact"));
            }
        }
        for (Transits.EventHit e : sky.time.scan.events) {
            if (e.event.jd >= from) {
                all.add(new Dated(e.event.jd, e.event + " - " + e.type.label.toLowerCase()
                    + " natal " + e.natal));
            }
        }
        all.sort((a, b) -> Double.compare(a.jd, b.jd));
        StringBuilder h = new StringBuilder("<h2>The year ahead</h2>");
        if (sky.year != null) {
            h.append("<p>To your next birthday: a house ").append(sky.year.house)
                .append(" year ruled by <b>").append(sky.year.lord).append("</b>.</p>");
        }
        if (all.isEmpty()) {
            return h.append("<p>Nothing else perfects on this chart before the next birthday."
                + "</p>").toString();
        }
        DateTimeFormatter month = DateTimeFormatter.ofPattern("MMMM yyyy");
        DateTimeFormatter day = DateTimeFormatter.ofPattern("d MMM");
        String shownMonth = "";
        for (Dated d : all) {
            ZonedDateTime at = fromJd(d.jd, sky.when.getZone());
            String m = at.format(month);
            if (!m.equals(shownMonth)) {
                if (!shownMonth.isEmpty()) {
                    h.append("</p>");
                }
                h.append("<h3>").append(m).append("</h3><p>");
                shownMonth = m;
            }
            h.append("<b>").append(at.format(day)).append("</b> &nbsp;").append(d.text)
                .append("<br>");
        }
        return h.append("</p>").toString();
    }

    /**
     * Zodiacal releasing at a moment, from both Lots: the chain of periods active now,
     * level by level, the next few sub-periods, and the life's chapters - the desktop's
     * Releasing tree ({@code ReleasingPanel}), from the same {@link ZodiacalReleasing#release}
     * to the same four levels. Needs a birth time, since the Lots are measured from the
     * Ascendant; "" without one.
     */
    static String releasing(PhoneChart.Cast natal, ZonedDateTime when) {
        if (natal.timeUnknown) {
            return "";
        }
        ChartFrame f = natal.frame;
        double now = Moments.sweDate(when).getJulDay();
        ZoneId zone = when.getZone();
        StringBuilder h = new StringBuilder("<h2>Zodiacal releasing</h2>");
        h.append("<p><i>From the Lot of Spirit for career and action, from the Lot of Fortune "
            + "for body and circumstance. A peak is a period in an angle from the Lot of "
            + "Spirit; the bond is loosed when a period jumps to the opposite sign.</i></p>");
        String[] names = {"Spirit", "Fortune"};
        double[] lots = {f.lotOfSpirit, f.lotOfFortune};
        for (int k = 0; k < 2; k++) {
            List<ZodiacalReleasing.Period> periods = ZodiacalReleasing.release(
                f.julianDayUt, lots[k], f.lotOfSpirit, f.julianDayUt + 100 * 365.2422, 4);
            h.append("<h3>From the Lot of ").append(names[k]).append(" (")
                .append(capital(Zodiac.signName(lots[k]))).append(")</h3>");
            List<ZodiacalReleasing.Period> chain = ZodiacalReleasing.activeChain(periods, now);
            if (chain.isEmpty()) {
                h.append("<p>No period is active at this date.</p>");
                continue;
            }
            h.append("<p><b>Now:</b><br>");
            for (ZodiacalReleasing.Period p : chain) {
                h.append("&nbsp;&nbsp;").append(period(p, zone)).append("<br>");
            }
            h.append("</p>");
            if (chain.size() >= 2) {
                // What the current chapter holds next, at the second level.
                ZodiacalReleasing.Period chapter = chain.get(0);
                StringBuilder next = new StringBuilder();
                int shown = 0;
                for (ZodiacalReleasing.Period p : chapter.children) {
                    if (p.startJd > now && shown < 4) {
                        next.append("&nbsp;&nbsp;").append(period(p, zone)).append("<br>");
                        shown++;
                    }
                }
                if (shown > 0) {
                    h.append("<p><b>Next in this chapter:</b><br>").append(next).append("</p>");
                }
            }
            h.append("<p><b>The chapters of the life:</b><br>");
            for (ZodiacalReleasing.Period p : periods) {
                h.append("&nbsp;&nbsp;").append(p == chain.get(0) ? "<b>" : "")
                    .append(period(p, zone)).append(p == chain.get(0) ? " &larr; now</b>" : "")
                    .append("<br>");
            }
            h.append("</p>");
        }
        return h.toString();
    }

    /** "L2 Leo, 3 Mar 2026 - 1 Oct 2026, peak" - one period as the desktop's tree names it. */
    static String period(ZodiacalReleasing.Period p, ZoneId zone) {
        DateTimeFormatter d = DateTimeFormatter.ofPattern("d MMM yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append('L').append(p.level).append(' ').append(capital(Zodiac.SIGNS[p.sign]))
            .append(", ").append(fromJd(p.startJd, zone).format(d)).append(" - ")
            .append(fromJd(p.endJd, zone).format(d));
        if (p.peak) {
            sb.append(", <b>peak</b>");
        }
        if (p.afterBond) {
            sb.append(", <b>bond loosed</b>");
        }
        return sb.toString();
    }

    /** A Julian day (UT) as a local date and time in a zone. */
    static ZonedDateTime fromJd(double jd, ZoneId zone) {
        de.thmac.swisseph.SweDate d = new de.thmac.swisseph.SweDate(jd);
        double hour = d.getHour();
        int h = (int) hour;
        int min = (int) Math.round((hour - h) * 60.0);
        return ZonedDateTime.of(d.getYear(), d.getMonth(), d.getDay(), 0, 0, 0, 0,
                ZoneId.of("UTC")).plusHours(h).plusMinutes(min).withZoneSameInstant(zone);
    }

    /**
     * The whole-chart reading at the sky's moment: the desktop's Synthesize with transits on,
     * its timing sections - the year, the transits, the year ahead - written from the same
     * {@link Chronometry}. Needs a sky cast with the year scanned.
     */
    static String synthesis(PhoneChart.Cast natal, Sky sky) {
        ChartFrame f = natal.frame;
        Gestalt.Result g = Gestalt.compute(f);
        List<BodyScore.Vector> ranked = BodyScore.rank(f, g);
        Themes.Result themes = Themes.extract(f, g, ranked, Topics.analyse(f, ranked));
        Chronometry t = sky.time;
        return PhoneReading.forPhone(NarrativeSynthesizer.generateReport(f, g, ranked, themes,
            t.transit, t.profection, t.hits, t.scan, t.convergence, true, false));
    }

    /** The local time now at a place. */
    static ZonedDateTime now(String zoneId) {
        return ZonedDateTime.now(ZoneId.of(zoneId));
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
