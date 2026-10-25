package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a birth chart's At a glance page opens with (reading framework spec, 2026-10-24): the
 * three cards that say who this is, the day-or-night line, and the mix of elements and ways of
 * acting as bars.
 *
 * <b>Nothing here is written for the reader.</b> The sentences are the interpretation corpus's
 * ({@link PlainSnapshot#sentenceFor}); the line about day and night is the Chart shape card's own
 * ({@link OverviewPanel#sectSentence}); the bars are Gestalt's weights, and the captions only name
 * what Gestalt already calls dominant or missing. The labels are the interface's.
 *
 * <b>The page's four spec parts, and the one left out.</b> The spec also asks for "one
 * high-impact synthesis sentence". A line like that would have to be composed here from
 * placements, which is the thing this project does not do; the three cards that follow these
 * blocks already open on the chart's own themes.
 */
final class GlanceBlocks {

    /** Registry name, the plain role for a person, the plain role for a couple. */
    private static final String[][] BIG3 = {
        {"Sun", "YOUR CORE DRIVE", "WHAT THE RELATIONSHIP IS FOR"},
        {"Moon", "YOUR EMOTIONAL NEEDS", "THE EMOTIONAL CLIMATE"},
        {"Ascendant", "HOW OTHERS MEET YOU", "HOW THE WORLD SEES THE COUPLE"},
    };

    private GlanceBlocks() {
    }

    /** The whole opening: the three cards, then (for a person) day or night, then the bars. */
    static String prefix(ChartFrame f, Gestalt.Result g, boolean composite, boolean showWhy) {
        StringBuilder sb = new StringBuilder();
        sb.append(ReadingCard.html(big3(f, composite), showWhy));
        if (!composite && g != null) {
            sb.append(sectHtml(g));
        }
        if (g != null) {
            sb.append(balanceHtml(g));
        }
        return sb.toString();
    }

    /**
     * The Sun, the Moon and the Ascendant as cards. A body with no written reading gets no card:
     * nothing is made up to fill it.
     */
    static List<ReadingCard> big3(ChartFrame f, boolean composite) {
        InterpretationService svc = InterpretationService.getInstance();
        List<ReadingCard> out = new ArrayList<>();
        for (String[] row : BIG3) {
            ChartFrame.Body b = bodyOf(f, row[0]);
            if (b == null || !b.ok) {
                continue;
            }
            String sentence = PlainSnapshot.sentenceFor(svc, f, row[0], false);
            if (sentence == null) {
                continue;
            }
            String sign = capitalise(Zodiac.signName(b.lon));
            int index = Bodies.indexOf(row[0].toLowerCase(Locale.ROOT));
            boolean angle = index >= 0 && Bodies.at(index).isAngle();
            int house = angle ? 0 : Zodiac.houseOf(b.lon, f.cusps);
            out.add(new ReadingCard(composite ? row[2] : row[1], sign, sentence,
                List.of(row[0] + " in " + sign + (house > 0 ? ", " + ordinal(house) + " house"
                    : ""))));
        }
        return out;
    }

    /** The day-or-night line, for a person's chart only. */
    static String sectHtml(Gestalt.Result g) {
        return "<div style='font-size:13px; margin:2px 0 12px 0;'><b>"
            + esc(OverviewPanel.subtitleOfSect(g.diurnal)) + ".</b> "
            + esc(OverviewPanel.sectSentence(g, ReadingVoice.PERSON)) + "</div>";
    }

    /** The elements and the ways of acting, as two sets of bars with a caption under each. */
    static String balanceHtml(Gestalt.Result g) {
        String elements = bars("The mix in your chart", g.elements, true, g.dominantElements,
            g.missingElements);
        String modes = bars("How you tend to act", g.modalities, false, g.dominantModalities,
            g.missingModalities);
        if (elements.isEmpty() && modes.isEmpty()) {
            return "";
        }
        return elements + modes
            + (modes.isEmpty() ? "" : "<div style='color:#9AA5B1; font-size:11px; margin:2px 0 "
                + "12px 0;'>Cardinal starts things. Fixed keeps them going. Mutable adapts.</div>");
    }

    /**
     * Whole percentages that add to exactly 100, by largest remainder, so that the bars never
     * add to 99 or 101 on the page. All zero when there is nothing to divide.
     */
    static int[] percents(double[] weights) {
        double sum = 0;
        for (double w : weights) {
            sum += Math.max(0, w);
        }
        int[] out = new int[weights.length];
        if (sum <= 0) {
            return out;
        }
        double[] rest = new double[weights.length];
        int used = 0;
        for (int i = 0; i < weights.length; i++) {
            double share = 100.0 * Math.max(0, weights[i]) / sum;
            out[i] = (int) Math.floor(share);
            rest[i] = share - out[i];
            used += out[i];
        }
        while (used < 100) {
            int best = 0;
            for (int i = 1; i < weights.length; i++) {
                if (rest[i] > rest[best]) {
                    best = i;
                }
            }
            out[best]++;
            rest[best] = -1;
            used++;
        }
        return out;
    }

    private static String bars(String title, Map<String, Double> weights, boolean coloured,
                               List<String> dominant, List<String> missing) {
        if (weights == null || weights.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>(weights.keySet());
        double[] w = new double[names.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = weights.get(names.get(i));
        }
        int[] pct = percents(w);
        int total = 0;
        for (int p : pct) {
            total += p;
        }
        if (total == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<h3 style='color:#FFD166; margin-top:14px; "
            + "margin-bottom:4px;'>").append(esc(title)).append("</h3>");
        sb.append("<table width='100%' cellspacing='0' cellpadding='2'>");
        for (int i = 0; i < names.size(); i++) {
            sb.append("<tr><td width='24%'>").append(esc(capitalise(names.get(i))))
              .append("</td><td><table width='100%' cellspacing='0' cellpadding='0'><tr>");
            if (pct[i] > 0) {
                sb.append("<td width='").append(pct[i]).append("%' height='9' bgcolor='")
                  .append(colour(names.get(i), coloured)).append("'></td>");
            }
            if (pct[i] < 100) {
                sb.append("<td></td>");
            }
            sb.append("</tr></table></td><td width='12%' align='right'>").append(pct[i])
              .append("%</td></tr>");
        }
        sb.append("</table>");
        StringBuilder cap = new StringBuilder();
        if (!dominant.isEmpty()) {
            cap.append("Strongest: ").append(TogetherCards.joinAnd(capitalised(dominant)))
               .append(". ");
        }
        if (!missing.isEmpty()) {
            cap.append("Light or missing: ").append(TogetherCards.joinAnd(capitalised(missing)))
               .append(".");
        }
        if (cap.length() > 0) {
            sb.append("<div style='color:#9AA5B1; font-size:12px; margin-top:3px;'>")
              .append(esc(cap.toString().trim())).append("</div>");
        }
        return sb.toString();
    }

    private static String colour(String name, boolean coloured) {
        if (!coloured) {
            return "#7FA7C9";
        }
        switch (name.toLowerCase(Locale.ROOT)) {
            case "fire":
                return "#E8743B";
            case "earth":
                return "#7A9E5B";
            case "air":
                return "#7FC8E8";
            default:
                return "#4B7BE5";
        }
    }

    private static List<String> capitalised(List<String> names) {
        List<String> out = new ArrayList<>();
        for (String n : names) {
            out.add(capitalise(n));
        }
        return out;
    }

    private static ChartFrame.Body bodyOf(ChartFrame f, String name) {
        try {
            return f.body(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String ordinal(int n) {
        int t = n % 100;
        String suffix = t >= 11 && t <= 13 ? "th"
            : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suffix;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
