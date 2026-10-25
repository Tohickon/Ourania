package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Synastry;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.List;

/**
 * What two charts do to each other, for the Synastry page of the side panel: each chart's
 * bodies on the other's angles, and each chart's placements in the other's houses.
 *
 * <h3>Why this is its own class (J13, step 3c)</h3>
 *
 * <p>It is the one part of the placements document that belongs to <i>neither</i> chart, which
 * is why it got its own page on 26 Sep - and it was already written as if it were its own
 * class: every builder took the two {@link ChartFrame}s and nothing else from the panel. What
 * stays in {@code SkymapPanel.generateSynastryCrossHtml} is finding those two frames, which is
 * a question about the panel's charts. Everything here is static.
 *
 * <p>Every string is the string the panel built, character for character.
 */
final class SynastryPage {

    /** The href prefix of a Show details link; the rest is the section key. */
    static final String DETAIL_LINK = "syndet|";

    /**
     * Which sections have their body paragraphs open. Static because the page is rebuilt from
     * scratch on every redraw and the reader's choice has to survive that; closed by default,
     * since open it is about 6,000 words.
     */
    private static final java.util.Set<String> OPEN = new java.util.HashSet<>();

    static void toggleDetail(String key) {
        if (key != null && !OPEN.remove(key)) {
            OPEN.add(key);
        }
    }

    private SynastryPage() {
    }

    /**
     * Both directions of angle contacts, then both directions of house overlays.
     *
     * @param a Chart A, as cast for the base wheel
     * @param b Chart B, as cast for the synastry
     */
    static String crossContacts(ChartFrame a, ChartFrame b) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<br><hr style='border-color:#444;'><br>");
        stringBuilder.append(angleContactsHtml("Chart A on Chart B's angles", a, b));
        stringBuilder.append(angleContactsHtml("Chart B on Chart A's angles", b, a));
        stringBuilder.append(houseOverlayHtml("Chart A's placements in Chart B's houses", a, b));
        stringBuilder.append(houseOverlayHtml("Chart B's placements in Chart A's houses", b, a));
        return stringBuilder.toString();
    }

    /** One direction of angle contacts, tightest first. */
    private static String angleContactsHtml(String string, ChartFrame chartFrame, ChartFrame chartFrame2) {
        List<Synastry.AngleContact> list = Synastry.angleContacts(chartFrame, chartFrame2);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<h3 style='color:#FFD166; margin:0 0 4px 0;'>Angle contacts &middot; ")
            .append(string).append("</h3>");
        if (list.isEmpty()) {
            // Said out loud rather than left blank, on the same reasoning as the aspect
            // pattern box: a section that vanishes cannot be told from a section that broke.
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:10px;'>")
                .append("Nothing sits on the Ascendant, Descendant, MC or IC within orb.</div>");
            return stringBuilder.toString();
        }
        stringBuilder.append("<div style='border:1px solid #FFD166; padding:6px; margin-bottom:10px;'>");
        for (Synastry.AngleContact angleContact : list) {
            stringBuilder.append("<div style='margin-bottom:3px; color:#cccccc; font-size:12px;'>")
                .append("<span style='font-size:15px;'>").append(SkymapPanel.namedGlyph(angleContact.bodyIndex))
                .append("</span> ").append(angleContact.body).append(" ")
                .append(Zodiac.format(angleContact.bodyLon))
                .append(" <span style='color:#FFD166;'>on</span> ").append(angleContact.angle)
                .append(" ").append(Zodiac.format(angleContact.angleLon))
                .append(" <span style='color:#9AA5B1;'>&mdash; orb ")
                .append(formatOrb(angleContact.orb)).append(" of ")
                .append(formatOrb(angleContact.maxOrb)).append("</span></div>");
        }
        List<String> angleNames = new java.util.ArrayList<>();
        for (Synastry.AngleContact angleContact : list) {
            angleNames.add(angleContact.body);
        }
        appendDetails(stringBuilder, "angle|" + string, angleNames);
        // What landing on that particular angle means, once per angle actually involved.
        // Once, not once per row: two bodies on the same Ascendant is one fact about the
        // Ascendant repeated, and printing the paragraph twice would say so twice.
        java.util.Set<String> said = new java.util.LinkedHashSet<>();
        for (Synastry.AngleContact angleContact : list) {
            if (!said.add(angleContact.angle)) {
                continue;
            }
            String prose = InterpretationService.getInstance()
                .getAngleContact(angleContact.angle.toLowerCase());
            if (prose != null) {
                stringBuilder.append("<div style='color:#dddddd; font-size:11px; ")
                    .append("margin-top:6px;'>").append(prose).append("</div>");
            }
        }
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }

    /**
     * One direction of house overlays, grouped by the host's house.
     *
     * Grouped rather than listed body by body because the reading is about the house: six
     * of A's bodies in B's 12th is one fact, not six. The houses nothing lands in are named
     * on their own line for the same reason the empty case above is - so that an absent
     * house reads as empty rather than as missing.
     */
    private static String houseOverlayHtml(String string, ChartFrame chartFrame, ChartFrame chartFrame2) {
        List<Synastry.Overlay> list = Synastry.houseOverlays(chartFrame, chartFrame2);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<h3 style='color:#add8e6; margin:0 0 4px 0;'>House overlays &middot; ")
            .append(string).append("</h3>");
        if (list.isEmpty()) {
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:10px;'>")
                .append("The visiting chart computed no bodies.</div>");
            return stringBuilder.toString();
        }
        StringBuilder[] stringBuilderArray = new StringBuilder[13];
        List<List<String>> namesByHouse = new java.util.ArrayList<>();
        for (int i = 0; i <= 12; ++i) {
            namesByHouse.add(new java.util.ArrayList<>());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        for (Synastry.Overlay overlay : list) {
            StringBuilder stringBuilder3;
            if (overlay.house < 1 || overlay.house > 12) {
                stringBuilder3 = stringBuilder2;
            } else {
                if (stringBuilderArray[overlay.house] == null) {
                    stringBuilderArray[overlay.house] = new StringBuilder();
                }
                stringBuilder3 = stringBuilderArray[overlay.house];
                namesByHouse.get(overlay.house).add(overlay.body);
            }
            if (stringBuilder3.length() > 0) {
                stringBuilder3.append(", ");
            }
            stringBuilder3.append("<span style='font-size:15px;'>")
                .append(SkymapPanel.namedGlyph(overlay.bodyIndex)).append("</span> ").append(overlay.body);
        }
        stringBuilder.append("<div style='margin-bottom:10px;'>");
        StringBuilder stringBuilder4 = new StringBuilder();
        for (int i = 1; i <= 12; ++i) {
            if (stringBuilderArray[i] == null) {
                if (stringBuilder4.length() > 0) {
                    stringBuilder4.append(", ");
                }
                stringBuilder4.append(PlacementText.romanNumeral(i));
                continue;
            }
            boolean bl = i == 1 || i == 4 || i == 7 || i == 10;
            stringBuilder.append("<div style='margin-bottom:3px; color:#cccccc; font-size:12px;'>")
                .append("<span style='color:").append(bl ? "#FFD166" : "#add8e6").append(";'>House ")
                .append(PlacementText.romanNumeral(i)).append(bl ? " (angular)" : "").append("</span> &nbsp;")
                .append(stringBuilderArray[i]);
            // The whole paragraph, not just the lead. The lead alone said the same vague line
            // for every body in the house; the paragraph names the everyday situations the
            // house covers, which is what the reader came for.
            String prose = InterpretationService.getInstance().getOverlayHouse(i);
            if (prose != null) {
                stringBuilder.append("<div style='color:#cccccc; font-size:11px; ")
                    .append("margin:3px 0 8px 14px;'>").append(prose).append("</div>");
            }
            appendDetails(stringBuilder, "house" + i + "|" + string, namesByHouse.get(i));
            stringBuilder.append("</div>");
        }
        if (stringBuilder4.length() > 0) {
            stringBuilder.append("<div style='color:#9AA5B1; font-size:11px;'>Nothing of theirs in: ")
                .append(stringBuilder4).append(".</div>");
        }
        if (stringBuilder2.length() > 0) {
            // House 0 from Zodiac.houseOf means the receiving chart's cusp set is degenerate,
            // which is a fact about that chart and not about these bodies.
            stringBuilder.append("<div style='color:#ff8080; font-size:11px;'>No house could be "
                + "determined for: ").append(stringBuilder2)
                .append(" &mdash; the receiving chart's cusps are degenerate at its latitude.</div>");
        }
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }

    /**
     * A "Show details" link, and under it the paragraph for each body while it is open. Nothing
     * at all when none of the bodies has a paragraph, so the link never opens onto an empty box.
     */
    private static void appendDetails(StringBuilder out, String key, List<String> names) {
        InterpretationService svc = InterpretationService.getInstance();
        boolean any = false;
        for (String name : names) {
            any |= svc.getOverlayBody(name) != null;
        }
        if (!any) {
            return;
        }
        // Letters, digits and bars only: the key rides in an href, and "Chart A's" is not safe there.
        key = key.replaceAll("[^A-Za-z0-9|]", "");
        boolean open = OPEN.contains(key);
        out.append("<div style='margin:2px 0 4px 14px; font-size:11px;'><a href='")
            .append(DETAIL_LINK).append(key)
            .append("' style='color:#7FB3FF; text-decoration:none;'>")
            .append(open ? "&#9662; Hide details" : "&#9656; Show details").append("</a></div>");
        if (open) {
            for (String name : names) {
                appendBodyProse(out, name);
            }
        }
    }

    /**
     * What one body does to a household, once per body it is listed for. Silent when the body has
     * no entry (Vertex, East Point and the lots other than Fortune and Spirit), because a line
     * saying "no text" would be noise.
     */
    private static void appendBodyProse(StringBuilder out, String bodyName) {
        String prose = InterpretationService.getInstance().getOverlayBody(bodyName);
        if (prose != null) {
            out.append("<div style='color:#aab4c0; font-size:11px; margin:2px 0 6px 14px;'>")
                .append(prose).append("</div>");
        }
    }

    /** Degrees and arcminutes, for an orb. Truncated to the minute, as Zodiac.format is. */
    private static String formatOrb(double d) {
        int n = (int)Math.floor(d);
        int n2 = (int)Math.floor((d - (double)n) * 60.0);
        return n + "&deg;" + String.format("%02d", n2) + "'";
    }
}
