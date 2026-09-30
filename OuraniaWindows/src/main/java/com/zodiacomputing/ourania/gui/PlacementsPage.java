package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AspectPatterns;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The placements page in the side panel: a chart's heading, its aspect patterns, and a
 * section of placements for each ring that is drawn.
 *
 * <h3>Why this is its own class (J13, step 3b)</h3>
 *
 * <p>Step 3a moved the words for one placement into {@link PlacementText}. This is the page
 * those rows are laid out on. It came out of {@code SkymapPanel.generatePlanetPlacementsHtml},
 * which still decides everything that is a question about the panel - which rings are drawn,
 * what the chart is called, whose it is - and now hands each section the ring it is about
 * rather than the section reaching into the panel for it. Every method here is static and
 * stateless, so a section can be built for any ring with no window open.
 *
 * <h3>One section builder for the outer ring and the sky</h3>
 *
 * <p>The outer ring and the sky ring were two copies of the same eleven lines differing only
 * in a title, a colour and a prefix - and they had already drifted once: the sky section
 * printed Chart B's coordinates until the sky got a row of its own. One builder that takes
 * the ring cannot print another ring's coordinates, because it has no other ring to print.
 *
 * <h3>What has not changed</h3>
 *
 * <p>Every string is the string the panel built, character for character. The aspect grid
 * and the synastry cross-contacts are still built by the panel; they read the aspect rules
 * and orb profiles, which is a larger surface than a ring.
 */
final class PlacementsPage {

    private PlacementsPage() {
    }

    /**
     * The base chart's section: its heading, when and where it was cast, the index link, the
     * aspect patterns, and its placements.
     *
     * @param title    what the panel calls this chart - whose it is, or that it is the sky
     * @param patterns the patterns already found for this chart; this does not look for them
     */
    static String natalSection(String title, WheelRing ring, DateTimeFormatter when,
            List<AspectPatterns.Pattern> patterns, double[] cusps) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<h2 style='color:#ffa500; margin-bottom: 2px;'>").append(title).append("</h2>");
        String string2 = ring.time != null ? ring.time.format(when) : "";
        String string3 = String.format("%.2f, %.2f", ring.latitude, ring.longitude);
        stringBuilder.append("<div style='color:#dddddd; font-size:11px; margin-bottom: 10px;'>").append(string2).append("<br>").append(string3).append("</div>");
        // <b>The one link that is always on screen.</b> Everything else in the index is
        // reached from a page you have to open first; this panel is up from the moment the
        // app starts, which makes it the only place a door actually works.
        stringBuilder.append("<div style='margin-bottom:10px;'><a href='index' "
            + "style='color:#7FB3FF; font-size:11px; text-decoration:none;'>"
            + "&#9776; Index &middot; browse bodies, signs, houses, aspects, dignities, "
            + "decans, Sabians, tarot and mansions</a></div>");

        // Aspect patterns, at the top of the aspect chart and without being asked for.
        //
        // These are closed circuits - several placements behaving as one unit - so they
        // outrank everything below them, and a chart that has one should say so before it
        // lists a single degree.
        if (patterns.isEmpty()) {
            // Said out loud rather than left blank. Patterns are held to physical bodies at a
            // tight orb, so about six charts in ten have none - and a section that simply
            // vanishes is indistinguishable from a section that is broken.
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px; "
                + "margin-bottom:10px;'>No aspect patterns in this chart - no T-square, "
                + "grand cross, grand trine, kite, yod or boomerang among the planets "
                + "within 5&deg;.</div>");
        } else {
            stringBuilder.append("<div style='border:1px solid #FFD166; padding:6px; "
                + "margin-bottom:10px;'>");
            stringBuilder.append("<h3 style='color:#FFD166; margin:0 0 4px 0;'>Aspect "
                + "Pattern").append(patterns.size() > 1 ? "s" : "")
                .append(" &middot; lit on the wheel</h3>");
            for (AspectPatterns.Pattern p : patterns) {
                stringBuilder.append(PlacementText.patternEntryHtml(p));
            }
            stringBuilder.append("<div style='color:#9AA5B1; font-size:10px;'>Click a figure "
                + "for the full reading.</div>");
            stringBuilder.append("</div>");
        }

        stringBuilder.append("<h3 style='color:#add8e6;'>Placements</h3>");
        stringBuilder.append(rows(ring, SkymapPanel.BASE_PREFIX, cusps));
        return stringBuilder.toString();
    }

    /**
     * A section for a ring drawn around the base chart - the outer ring or the sky.
     *
     * <b>Its own coordinates, always.</b> The sky section once printed Chart B's, which is the
     * same borrowing the houses were doing until the sky got a row of its own - right whenever
     * the two happened to be the same city and quietly wrong otherwise.
     *
     * @param when the moment as the panel words it; for the outer ring that can be a cast label
     */
    static String ringSection(String title, String colour, String when, WheelRing ring,
            String prefix, double[] cusps) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<br><h2 style='color:").append(colour)
            .append("; margin-bottom: 2px;'>").append(title).append("</h2>");
        String where = String.format("%.2f, %.2f", ring.latitude, ring.longitude);
        stringBuilder.append("<div style='color:#dddddd; font-size:11px; margin-bottom: 10px;'>").append(when).append("<br>").append(where).append("</div>");
        stringBuilder.append("<h3 style='color:").append(colour).append(";'>Placements</h3>");
        stringBuilder.append(rows(ring, prefix, cusps));
        return stringBuilder.toString();
    }

    /** One row per body this ring carries a position for, in registry order. */
    private static String rows(WheelRing ring, String prefix, double[] cusps) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < SkymapPanel.BODY_COUNT; ++n) {
            if (!ring.valid[n]) continue;
            sb.append(PlacementText.placementRow(n, ring.lon[n], ring.speed[n], prefix, cusps));
        }
        return sb.toString();
    }
}
