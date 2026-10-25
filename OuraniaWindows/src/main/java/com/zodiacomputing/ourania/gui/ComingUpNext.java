package com.zodiacomputing.ourania.gui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * "Coming up next": the soonest few contacts that turn exact in the next two weeks, each with
 * its date (reading framework spec, Mode 3, At a glance).
 *
 * <b>It is the Coming up page, cut short, not a second engine.</b> The contacts are
 * {@link NowTimeline}'s - transits to the chart at the moment each is exact - so this card and
 * that page cannot disagree about when anything happens. This keeps only those that are exact from
 * now to {@value #DAYS} days ahead, soonest first, at most {@value #LIMIT}. A contact already past
 * its peak is not "next". The card is the contact's own plain card, cut to its headline, so a
 * glance stays a glance.
 */
final class ComingUpNext {

    /** How far ahead "next" looks, in days. A contact exactly this far ahead counts. */
    static final double DAYS = 14.0;

    /** How many cards at most. The spec asks for one to three. */
    static final int LIMIT = 3;

    private ComingUpNext() {
    }

    /**
     * The cards for the soonest contacts that are exact between {@code nowJd} and
     * {@link #DAYS} days later, soonest first.
     *
     * @param nowJd the Julian day of the sky on the wheel
     */
    static List<ReadingCard> cards(List<NowTimeline.Entry> entries, double nowJd) {
        List<NowTimeline.Entry> next = new ArrayList<>();
        for (NowTimeline.Entry e : entries) {
            if (!Double.isNaN(e.jd()) && e.jd() >= nowJd && e.jd() - nowJd <= DAYS
                    && e.card() != null) {
                next.add(e);
            }
        }
        next.sort(Comparator.comparingDouble(NowTimeline.Entry::jd));
        List<ReadingCard> out = new ArrayList<>();
        for (NowTimeline.Entry e : next) {
            if (out.size() >= LIMIT) {
                break;
            }
            out.add(new ReadingCard("COMING UP NEXT · " + dateLabel(e.jd()),
                e.card().headline(), "", e.card().why()));
        }
        return out;
    }

    /** "Thu 29 Oct", in the reader's own time zone. */
    static String dateLabel(double jd) {
        long seconds = Math.round((jd - 2440587.5) * 86400.0);
        return DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
            .format(Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()));
    }

    /** The section on the Transits At a glance page: a heading, then the cards or the news that there are none. */
    static String html(String whose, List<ReadingCard> cards, boolean showWhy) {
        StringBuilder sb = new StringBuilder(SkyOnChart.sectionHeading("Coming up next on "
            + SkyOnChart.chartHeading(whose)));
        if (cards.isEmpty()) {
            return sb.append("<p style='color:#AAAAAA;'>Nothing turns exact on this chart in the "
                + "next two weeks.</p>").toString();
        }
        return sb.append(ReadingCard.html(cards, showWhy)).toString();
    }
}
