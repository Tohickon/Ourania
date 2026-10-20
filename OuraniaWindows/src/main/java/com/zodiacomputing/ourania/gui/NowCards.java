package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Plain-English cards for active transits (Plain-Tabs Phase 3).
 *
 * <p><b>The reader meets the meaning before the machinery.</b>
 * David, 2026-10-13: the tabs "only show the user the back end math side".
 * This transforms the Now / Transits page from a raw coordinate table into:
 * <ul>
 *   <li><b>This Week (Catalysts)</b>: fast-moving personal bodies (Moon, Sun, Mercury, Venus, Mars)</li>
 *   <li><b>This Season (The Long Arc)</b>: slow-moving outer bodies (Jupiter, Saturn, Uranus, Neptune, Pluto, Chiron)</li>
 *   <li><b>Dynamic Timing Badges</b>:
 *     <ul>
 *       <li><b>PEAK</b>: tight orb &le; 0.25&deg;</li>
 *       <li><b>BUILDING</b>: applying aspect closing toward exact</li>
 *       <li><b>WINDING DOWN</b>: separating aspect opening past exact</li>
 *     </ul>
 *   </li>
 *   <li><b>Why &mdash; the astrology</b>: technical degrees and aspect geometry shown only in
 *       Astrologer view ({@link Settings#astrologerView()}).</li>
 * </ul>
 */
public final class NowCards {

    private NowCards() { }

    public static final Set<String> FAST_BODIES = Set.of(
        "Moon", "Sun", "Mercury", "Venus", "Mars"
    );

    public record TransitHit(
        String transiting,
        double transitLon,
        double transitSpeed,
        int transitHouse,
        String natal,
        double natalLon,
        int natalHouse,
        Aspects.Type aspect,
        double orb,
        boolean applying,
        boolean isFast
    ) {
        public String timingBadge() {
            if (orb <= 0.25) {
                return "PEAK";
            }
            return applying ? "BUILDING" : "WINDING DOWN";
        }
    }

    /**
     * Finds active transits between the transiting ring and the natal/radix ring.
     */
    public static List<TransitHit> findHits(WheelRing transitRing, WheelRing natalRing, double[] cusps) {
        List<TransitHit> hits = new ArrayList<>();
        if (transitRing == null || natalRing == null
                || transitRing.lon == null || natalRing.lon == null
                || transitRing.valid == null || natalRing.valid == null) {
            return hits;
        }

        // Allow up to 2.5 degrees so readers see building and separating phases around the peak
        double orbCap = Math.max(Transits.orb, 2.5);

        for (int t = 0; t < transitRing.lon.length && t < Bodies.count(); t++) {
            if (t >= transitRing.valid.length || !WheelLayout.aspecting(t, transitRing.valid)
                    || Bodies.at(t).isAngle()) {
                continue;
            }
            String transitingName = Bodies.at(t).name;
            double tLon = transitRing.lon[t];
            double tSpeed = transitRing.speed != null && t < transitRing.speed.length
                ? transitRing.speed[t] : 0.0;
            int tHouse = cusps != null ? Zodiac.houseOf(tLon, cusps) : 0;
            boolean fast = FAST_BODIES.contains(transitingName);

            for (int n = 0; n < natalRing.lon.length && n < Bodies.count(); n++) {
                if (n >= natalRing.valid.length || !WheelLayout.aspecting(n, natalRing.valid)
                        || Bodies.isOppositePair(t, n)) {
                    continue;
                }
                String natalName = Bodies.at(n).name;
                double nLon = natalRing.lon[n];
                int nHouse = cusps != null ? Zodiac.houseOf(nLon, cusps) : 0;

                double sep = Aspects.separation(tLon, nLon);
                Aspects.Type aspectType = Aspects.typeWithin(
                    sep, transitingName, natalName, orbCap, Aspects.Profile.TRANSIT);
                if (aspectType == null) {
                    continue;
                }

                double orb = Math.abs(sep - aspectType.exactAngle);
                boolean applying = Aspects.isApplying(tLon, tSpeed, nLon, 0.0, aspectType);

                hits.add(new TransitHit(
                    transitingName, tLon, tSpeed, tHouse,
                    natalName, nLon, nHouse,
                    aspectType, orb, applying, fast
                ));
            }
        }

        // Sort by orb: tightest aspects (peaks) first
        hits.sort(Comparator.comparingDouble(TransitHit::orb));
        return hits;
    }

    /**
     * The astrology behind a transit card, one line each: the "why" box of every
     * {@link TransitCard}, shown in Astrologer view.
     */
    static List<String> whyLines(TransitHit hit) {
        List<String> why = new ArrayList<>();
        why.add("Transiting " + hit.transiting + " in " + PlacementText.capitalise(Zodiac.signName(hit.transitLon))
            + (hit.transitHouse > 0 ? ", " + Zodiac.ordinal(hit.transitHouse) + " house" : ""));
        why.add(hit.aspect.label + " Natal " + hit.natal + " in " + PlacementText.capitalise(Zodiac.signName(hit.natalLon))
            + (hit.natalHouse > 0 ? ", " + Zodiac.ordinal(hit.natalHouse) + " house" : ""));
        why.add(String.format("Orb: %.2f° (%s)", hit.orb, hit.timingBadge().toLowerCase()));

        return why;
    }

    /**
     * Builds the complete page HTML for the Now / Transits pane, with no ephemeris: in plain
     * view, the timeline's "right now" alone.
     */
    public static String html(WheelRing transitRing, WheelRing natalRing, double[] cusps,
                              boolean showWhy, String rawPlacementsHtml) {
        return html(transitRing, natalRing, cusps, showWhy, rawPlacementsHtml, null);
    }

    /** The page's title and the line under it, in both views. */
    static final String TITLE = "Coming up";
    static final String LEDE = "What the sky does to your chart, from right now to a year ahead.";

    /**
     * Builds the complete page HTML for the Coming up pane.
     *
     * <b>One timeline in both views (2026-10-20).</b> The timeline ({@link NowTimeline}) runs
     * right now, today, tonight ... later this year, in the order a reader asks. With {@code sw}
     * null - while time is running - only "right now" is worked out, and the page says the rest
     * follows. Astrologer view adds, and only adds: under each card the placements it was read
     * from, and at the foot the sky's positions.
     *
     * <b>Astrologer view used to be a second page.</b> It swapped the timeline for two lists,
     * "This Week · Catalysts" and "This Season · The Long Arc", of corpus cards: every minor
     * aspect and every minor point, each written to one template - "In the transiting
     * structure ... This geometry specifically bridges the temporary cosmic current to ..." -
     * and, where the corpus had no line, "General Semisextile meaning" and "(Specific text ...
     * not found)". David, 2026-10-20: the titles "arent plain english user friendly and most of
     * the readings say the same thing". The plain cards are built from parts per planet,
     * aspect and need, so ticking a box to see the astrology now shows the astrology of the
     * same reading rather than a different reading.
     */
    public static String html(WheelRing transitRing, WheelRing natalRing, double[] cusps,
                              boolean showWhy, String rawPlacementsHtml,
                              de.thmac.swisseph.SwissEph sw) {
        List<NowTimeline.Entry> entries = NowTimeline.build(sw, transitRing, natalRing, cusps);
        StringBuilder sb = ReadingCard.page(TITLE, LEDE);
        sb.append(windowsHtml(entries, visible(entries), showWhy));
        if (sw == null) {
            sb.append(STOP_THE_CLOCK);
        }
        if (showWhy && rawPlacementsHtml != null && !rawPlacementsHtml.trim().isEmpty()) {
            sb.append("<div style='margin-top:24px; border-top:1px solid #333; padding-top:12px;'>");
            sb.append("<h3 style='color:#add8e6; margin-bottom:8px;'>Where the planets are now</h3>");
            sb.append(stripEnclosingHtml(rawPlacementsHtml));
            sb.append("</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static final String STOP_THE_CLOCK = "<p style='color:#9AA5B1; font-size:12px;'>"
        + "Stop the clock to see what is coming later.</p>";

    /** The card a page shows when nothing in the sky is touching the chart. */
    private static final String QUIET_SKY =
        "<table width='100%' cellpadding='10' cellspacing='0' bgcolor='#1B2230' style='margin-bottom:12px;'><tr><td>"
        + "<div style='color:#7FA7C9; font-size:10px; font-weight:bold;'>QUIET SKY</div>"
        + "<div style='color:#FFFFFF; font-size:15px; font-weight:bold; margin-top:4px;'>A Quiet Stretch in the Sky</div>"
        + "<p style='font-size:13px; margin-top:6px; color:#E0E0E0;'>Nothing in the sky is pressing on your chart right now. Life runs on steady habit for the moment, not on outside pressure.</p>"
        + "</td></tr></table>";

    /**
     * The timeline's entries the page shows, in its order: up to {@link NowTimeline#PER_WINDOW}
     * in each window. One rule for the page and for the pop-ups that land on it.
     */
    static List<NowTimeline.Entry> visible(List<NowTimeline.Entry> entries) {
        List<NowTimeline.Entry> out = new ArrayList<>();
        java.util.Map<NowTimeline.Window, Integer> count = new java.util.EnumMap<>(
            NowTimeline.Window.class);
        for (NowTimeline.Window w : NowTimeline.Window.values()) {
            for (NowTimeline.Entry e : entries) {
                if (e.window() == w && count.merge(w, 1, Integer::sum) <= NowTimeline.PER_WINDOW) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    /** The plain page: the timeline's windows in order, each with its cards. */
    static String timelineHtml(List<NowTimeline.Entry> entries, boolean complete) {
        return timelineHtml(TITLE, LEDE, entries, visible(entries), complete);
    }

    /** How far ahead Coming up looks: the near term, which is what that page is for. */
    static final double COMING_UP_DAYS = 90.0;

    /**
     * The Coming up page: the same timeline as Now, cut to the next {@link #COMING_UP_DAYS}
     * days, with every contact in that stretch shown rather than three a window.
     *
     * <b>David, 2026-10-20.</b> Coming up was written by TimelinePredictor, which read the
     * transit sky <i>in place of</i> the chart and listed the aspects the sky's planets made to
     * each other - so nothing on it was about the reader, and its Past, Present and Future were
     * fixed lists of bodies rather than dates. The Now timeline was already the right engine:
     * transits to the chart, at the moments they are exact. One engine, two lengths of view.
     *
     * @param now the moment the timeline was built from, as a Julian day
     */
    static String comingUpHtml(List<NowTimeline.Entry> entries, double now) {
        List<NowTimeline.Entry> near = new ArrayList<>();
        for (NowTimeline.Entry e : entries) {
            // Right now has no date of its own: it is in orb at this moment.
            if (Double.isNaN(e.jd()) || e.jd() <= now + COMING_UP_DAYS) {
                near.add(e);
            }
        }
        return timelineHtml("Coming up",
            "What the sky does to your chart in the next " + (int) COMING_UP_DAYS
                + " days, in date order.",
            near, near, true);
    }

    private static String timelineHtml(String title, String lede,
            List<NowTimeline.Entry> entries, List<NowTimeline.Entry> shown, boolean complete) {
        StringBuilder sb = ReadingCard.page(title, lede);
        sb.append(windowsHtml(entries, shown, false));
        if (!complete) {
            sb.append(STOP_THE_CLOCK);
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * The timeline's windows in order, each headed and with its cards - the body of a timeline
     * page, without the page around it. Coming up together draws one of these per person.
     */
    static String windowsHtml(List<NowTimeline.Entry> entries, List<NowTimeline.Entry> shown,
            boolean showWhy) {
        StringBuilder sb = new StringBuilder();
        if (entries.isEmpty()) {
            sb.append(QUIET_SKY);
        }
        for (NowTimeline.Window w : NowTimeline.Window.values()) {
            List<ReadingCard> cards = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            int all = 0;
            for (NowTimeline.Entry e : entries) {
                if (e.window() == w) {
                    all++;
                    if (shown.contains(e)) {
                        cards.add(e.card());
                        ids.add(TransitCard.id(e.hit()));
                    }
                }
            }
            if (all == 0) {
                continue;
            }
            sb.append("<h3 style='color:#7FB3FF; margin-top:14px; margin-bottom:6px;'>")
              .append(w.title).append("</h3>");
            sb.append(cardsHtml(cards, ids, showWhy));
            if (all > cards.size()) {
                sb.append("<p style='color:#9AA5B1; font-size:12px; margin-top:-6px;'>And ")
                  .append(all - cards.size()).append(" more ")
                  .append(w.title.toLowerCase(java.util.Locale.ROOT)).append(".</p>");
            }
        }
        return sb.toString();
    }

    /** How many cards each section of the page holds. */
    static final int PER_SECTION = 6;

    /**
     * The contacts the page makes a card for, in its order: up to {@link #PER_SECTION} this
     * week and as many this season, and in plain view only those with a plain card.
     *
     * <b>The page and its pop-ups ask this one rule.</b> The pop-ups first chose from every
     * contact, so a card could form, fly, and find no card of its own on the page to land on.
     */
    static List<TransitHit> shown(List<TransitHit> hits, boolean showWhy) {
        List<TransitHit> out = new ArrayList<>();
        int fast = 0;
        int slow = 0;
        for (TransitHit hit : hits) {
            if (!showWhy && TransitCard.of(hit) == null) {
                continue;
            }
            if (hit.isFast ? fast++ < PER_SECTION : slow++ < PER_SECTION) {
                out.add(hit);
            }
        }
        return out;
    }

    /**
     * The cards, each in a div named for its contact ({@link TransitCard#id}), so a card that
     * pops up when time stops can find its own card on this page and land on it.
     */
    private static String cardsHtml(List<ReadingCard> cards, List<String> ids, boolean showWhy) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            sb.append("<div id='").append(ids.get(i)).append("'>")
              .append(ReadingCard.html(List.of(cards.get(i)), showWhy)).append("</div>");
        }
        return sb.toString();
    }

    private static String stripEnclosingHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceFirst("(?i)<!DOCTYPE[^>]*>", "")
                   .replaceFirst("(?i)<html[^>]*>", "")
                   .replaceFirst("(?i)<body[^>]*>", "")
                   .replaceFirst("(?i)</body>\\s*</html>\\s*$", "")
                   .trim();
    }
}
