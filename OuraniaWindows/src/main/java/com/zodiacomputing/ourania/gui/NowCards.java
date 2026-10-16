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
     * Converts a transit contact into a clean plain-English ReadingCard.
     */
    public static ReadingCard toCard(TransitHit hit) {
        InterpretationService svc = InterpretationService.getInstance();
        String prose = svc.getTransitAspect(hit.transiting, hit.natal, hit.aspect.label);

        String category = hit.isFast ? "THIS WEEK · CATALYST" : "THIS SEASON · THE LONG ARC";
        String label = "[ " + hit.timingBadge() + " ] · " + category;

        List<String> why = new ArrayList<>();
        why.add("Transiting " + hit.transiting + " in " + PlacementText.capitalise(Zodiac.signName(hit.transitLon))
            + (hit.transitHouse > 0 ? ", " + Zodiac.ordinal(hit.transitHouse) + " house" : ""));
        why.add(hit.aspect.label + " Natal " + hit.natal + " in " + PlacementText.capitalise(Zodiac.signName(hit.natalLon))
            + (hit.natalHouse > 0 ? ", " + Zodiac.ordinal(hit.natalHouse) + " house" : ""));
        why.add(String.format("Orb: %.2f° (%s)", hit.orb, hit.timingBadge().toLowerCase()));

        ReadingCard card = ReadingCard.fromLead(label, prose, why);
        if (card != null) {
            return card;
        }

        String headline;
        String bodyText;

        if (prose != null && !prose.trim().isEmpty() && !prose.startsWith("<i>(No transit reading")) {
            headline = hit.transiting + " " + hit.aspect.label.toLowerCase() + " natal " + hit.natal;
            bodyText = prose.replaceAll("(?i)<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        } else {
            headline = hit.transiting + " " + hit.aspect.label.toLowerCase() + " natal " + hit.natal;
            if (hit.isFast) {
                bodyText = "A fast-moving daily catalyst activating your natal " + hit.natal
                    + (hit.natalHouse > 0 ? " in the " + Zodiac.ordinal(hit.natalHouse) + " house" : "")
                    + ". Events, interactions, and immediate choices around this theme move quickly.";
            } else {
                bodyText = "An enduring developmental arc steadily reshaping, testing, and deepening your natal "
                    + hit.natal + (hit.natalHouse > 0 ? " in the " + Zodiac.ordinal(hit.natalHouse) + " house" : "")
                    + ". This theme is doing foundational work throughout this season.";
            }
        }

        return new ReadingCard(label, headline, bodyText, why);
    }

    /**
     * Builds the complete page HTML for the Now / Transits pane.
     */
    public static String html(WheelRing transitRing, WheelRing natalRing, double[] cusps,
                              boolean showWhy, String rawPlacementsHtml) {
        List<TransitHit> hits = findHits(transitRing, natalRing, cusps);

        StringBuilder sb = ReadingCard.page("Now \u00b7 Active Cycles",
            "The sky passing over your chart &mdash; immediate catalysts this week and major cycles this season.");

        List<ReadingCard> thisWeekCards = new ArrayList<>();
        List<ReadingCard> thisSeasonCards = new ArrayList<>();

        for (TransitHit hit : hits) {
            ReadingCard card = toCard(hit);
            if (hit.isFast) {
                if (thisWeekCards.size() < 6) {
                    thisWeekCards.add(card);
                }
            } else {
                if (thisSeasonCards.size() < 6) {
                    thisSeasonCards.add(card);
                }
            }
        }

        if (thisWeekCards.isEmpty() && thisSeasonCards.isEmpty()) {
            sb.append("<table width='100%' cellpadding='10' cellspacing='0' bgcolor='#1B2230' style='margin-bottom:12px;'><tr><td>")
              .append("<div style='color:#7FA7C9; font-size:10px; font-weight:bold;'>QUIET SKY</div>")
              .append("<div style='color:#FFFFFF; font-size:15px; font-weight:bold; margin-top:4px;'>A Quiet Stretch in the Sky</div>")
              .append("<p style='font-size:13px; margin-top:6px; color:#E0E0E0;'>No major transit aspects are in tight peak orb right now. Your chart is running on steady habit rather than external pressure.</p>")
              .append("</td></tr></table>");
        }

        if (!thisWeekCards.isEmpty()) {
            sb.append("<h3 style='color:#7FB3FF; margin-top:14px; margin-bottom:6px;'>This Week \u00b7 Catalysts</h3>");
            sb.append(ReadingCard.html(thisWeekCards, showWhy));
        }

        if (!thisSeasonCards.isEmpty()) {
            sb.append("<h3 style='color:#FFD166; margin-top:18px; margin-bottom:6px;'>This Season \u00b7 The Long Arc</h3>");
            sb.append(ReadingCard.html(thisSeasonCards, showWhy));
        }

        // Astrologer view drawer: append raw placement tables when enabled
        if (showWhy && rawPlacementsHtml != null && !rawPlacementsHtml.trim().isEmpty()) {
            sb.append("<div style='margin-top:24px; border-top:1px solid #333; padding-top:12px;'>");
            sb.append("<h3 style='color:#add8e6; margin-bottom:8px;'>Placements \u00b7 Raw Coordinates</h3>");
            sb.append(stripEnclosingHtml(rawPlacementsHtml));
            sb.append("</div>");
        }

        sb.append("</body></html>");
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
