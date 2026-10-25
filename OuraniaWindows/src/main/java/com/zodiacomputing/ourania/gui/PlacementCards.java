package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The natal chart's Placements page as cards (reading framework spec, Phase 2A): one card for
 * each body, in three groups - Personal drivers, Social and evolutionary forces, Angles - and a
 * fourth, Other points, so that nothing the wheel shows is left off the page.
 *
 * <b>What a card says.</b> Plain view: the body, its sign, its degree, the part of life it shows
 * up in, and the corpus's own sentence for that body in that sign. Astrologer view adds the house
 * number, the dignity badges (Ruler, Exaltation, Detriment, Fall) and the Sabian symbol - David,
 * 2026-10-25: "Astrologer view only". The house and the dignity words are on his banned list, so
 * they cannot stand on a plain card.
 *
 * <b>The card is a link, as the old row was.</b> Its name is its link's target ({@code base_N}),
 * which is how a click opens the Selected drawer and how a click on the wheel scrolls here.
 * Built from a ring's bare numbers, not a window, so a suite can build any chart's page.
 */
final class PlacementCards {

    /** The groups, in the order the page shows them. */
    enum Group {
        PERSONAL("Personal drivers"),
        SOCIAL("Social and evolutionary forces"),
        ANGLES("Angles"),
        OTHER("Other points");

        final String label;

        Group(String label) {
            this.label = label;
        }
    }

    private static final List<String> PERSONAL =
        Arrays.asList("Sun", "Moon", "Mercury", "Venus", "Mars");
    private static final List<String> SOCIAL =
        Arrays.asList("Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron");

    static final String RULER = "#3FA66A";
    static final String EXALTATION = "#E0A93B";
    static final String DETRIMENT = "#D9803B";
    static final String FALL = "#D9534F";

    private PlacementCards() {
    }

    /** The group a body is shown under. Any node, true or mean, is a social and evolutionary force. */
    static Group groupOf(int index) {
        Bodies.Def def = Bodies.at(index);
        if (def.isAngle()) {
            return Group.ANGLES;
        }
        if (PERSONAL.contains(def.name)) {
            return Group.PERSONAL;
        }
        if (SOCIAL.contains(def.name) || def.name.contains("Node")) {
            return Group.SOCIAL;
        }
        return Group.OTHER;
    }

    /**
     * The dignities a body holds in the sign it is in, as the badge words, in the order the spec
     * lists them. Empty outside the traditional seven (Dignity says so itself), and for a body
     * holding none.
     */
    static List<String> badges(String name, double longitude) {
        Dignity.Result r = Dignity.evaluate(name, longitude, true);
        List<String> out = new ArrayList<>();
        if (r.domicile) {
            out.add("Ruler");
        }
        if (r.exaltation) {
            out.add("Exaltation");
        }
        if (r.detriment) {
            out.add("Detriment");
        }
        if (r.fall) {
            out.add("Fall");
        }
        return out;
    }

    /**
     * The page body: every valid body as a card under its group's heading.
     *
     * @param prefix the link prefix of the ring ({@code base_} for the inner wheel)
     */
    static String html(double[] lon, double[] speed, boolean[] valid, double[] cusps,
                       String prefix, boolean showWhy) {
        StringBuilder sb = new StringBuilder();
        InterpretationService svc = InterpretationService.getInstance();
        for (Group g : Group.values()) {
            StringBuilder cards = new StringBuilder();
            for (int n = 0; n < valid.length && n < lon.length; n++) {
                if (valid[n] && groupOf(n) == g) {
                    cards.append(card(svc, n, lon[n], speed, cusps, prefix, showWhy));
                }
            }
            if (cards.length() > 0) {
                sb.append("<h3 style='color:#7FB3FF; margin-top:14px; margin-bottom:6px;'>")
                  .append(esc(g.label)).append("</h3>").append(cards);
            }
        }
        return sb.toString();
    }

    private static String card(InterpretationService svc, int n, double lon, double[] speed,
                               double[] cusps, String prefix, boolean showWhy) {
        Bodies.Def def = Bodies.at(n);
        String sign = Zodiac.signName(lon);
        double inSign = Zodiac.degreeInSign(lon);
        int deg = (int) Math.floor(inSign);
        int min = (int) Math.floor((inSign - deg) * 60.0);
        boolean angle = def.isAngle();
        int house = angle || cusps == null ? 0 : Zodiac.houseOf(lon, cusps);
        String area = house > 0 ? LifeAreas.of(house) : null;
        boolean retrograde = speed != null && n < speed.length && speed[n] < 0.0
            && PlacementText.showsDirection(n);
        String sentence = sentenceFor(svc, def.name, sign, showWhy);

        StringBuilder sb = new StringBuilder();
        sb.append("<table width='100%' cellpadding='8' cellspacing='0' bgcolor='#1B2230' "
            + "style='margin-bottom:8px;'><tr><td>");
        sb.append("<div style='color:#7FA7C9; font-size:10px; font-weight:bold;'>")
          .append(esc(def.name.toUpperCase(Locale.ROOT))).append("</div>");
        sb.append("<a name='").append(prefix).append(n).append("' href='").append(prefix).append(n)
          .append("' style='color:#FFFFFF; text-decoration:none; font-size:15px; "
              + "font-weight:bold;'>")
          .append(SkymapPanel.BODY_GLYPHS[n]).append(' ').append(esc(cap(sign))).append("</a>");
        if (retrograde) {
            sb.append(" <font color='#E8B04B' size='2'>retrograde</font>");
        }
        if (showWhy) {
            List<String> badges = badges(def.name, lon);
            if (!badges.isEmpty()) {
                sb.append("<table cellspacing='4' cellpadding='2'><tr>");
                for (String b : badges) {
                    sb.append("<td bgcolor='").append(badgeColour(b))
                      .append("'><font size='1' color='#10151F'><b>").append(b)
                      .append("</b></font></td>");
                }
                sb.append("</tr></table>");
            }
        }
        sb.append("<div style='color:#9AA5B1; font-size:11px; margin-top:3px;'>");
        sb.append(showWhy ? String.format(Locale.ROOT, "%d°%02d'", deg, min) : deg + "°");
        if (showWhy && house > 0) {
            sb.append(" · ").append(Zodiac.ordinal(house)).append(" house");
        }
        if (area != null) {
            sb.append(" · Shows up in: ").append(esc(area));
        }
        sb.append("</div>");
        if (sentence != null) {
            sb.append("<p style='font-size:13px; margin-top:6px;'>").append(esc(sentence))
              .append("</p>");
        }
        if (showWhy) {
            String sabian = svc.getSabianSymbol(sign, deg + 1);
            if (sabian != null && !sabian.isEmpty() && !sabian.startsWith("Sabian symbol not found")) {
                sb.append("<div style='color:#9AA5B1; font-size:11px; margin-top:4px;'>"
                    + "<b>Sabian symbol:</b> <i>")
                  .append(esc(sabian.replaceAll("(?i)<[^>]+>", " ").replaceAll("\\s+", " ").trim()))
                  .append("</i></div>");
            }
            Dignity.Result r = Dignity.evaluate(def.name, lon, true);
            if (!r.reasons.isEmpty() && (r.domicile || r.exaltation || r.detriment || r.fall
                    || r.triplicity || r.bound || r.face || r.peregrine)) {
                sb.append("<div style='color:#9AA5B1; font-size:11px; margin-top:3px;'>"
                    + "<b>Essential dignity:</b> ")
                  .append(esc(String.join("; ", r.reasons))).append("</div>");
            }
        }
        return sb.append("</td></tr></table>").toString();
    }

    /**
     * The corpus's sentence for a body in a sign, as the card may show it.
     *
     * <b>Chosen or left out, never reworded.</b> Measured 2026-10-25: of 432 body-in-sign
     * sentences, 5 carry a word from David's banned list ("applying", "destiny" three times,
     * "karmic"). A plain card for one of those shows no sentence - it still has its sign, degree
     * and part of life - and Astrologer view, where the real terms belong, shows it whole.
     */
    static String sentenceFor(InterpretationService svc, String name, String sign,
                              boolean astrologer) {
        String s = PlainSnapshot.signSentence(svc, name, sign);
        return s != null && (astrologer || PlainWords.isPlain(s)) ? s : null;
    }

    static String badgeColour(String badge) {
        switch (badge) {
            case "Ruler":
                return RULER;
            case "Exaltation":
                return EXALTATION;
            case "Detriment":
                return DETRIMENT;
            default:
                return FALL;
        }
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
