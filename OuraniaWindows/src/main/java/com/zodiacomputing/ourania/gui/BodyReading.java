package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "The body": what astrological tradition links to the body, read from the chart (2026-10-22).
 *
 * <b>David's reference, and his shape for it.</b> He pasted the correspondences - each sign's
 * part of the body, each planet's functions, the health houses (1st, 6th, 8th, 12th) and the
 * aspect signatures for particular conditions - and asked that a reading be "interpret[ed]
 * through the planets placement so if the math lines up it should read like" his example:
 * "Mars in the 1st House (Physical Constitution &amp; Head/Face)", then the rulership, the
 * health expression, and the triggers. So a card appears only when the chart has the placement
 * or the aspect, and it says what is ruled, how it tends to show, and what helps.
 *
 * <b>Tradition, not diagnosis.</b> The section opens by saying so, and the words name areas
 * to look after and tendencies under stress; cancer, tumours and mortality are not named.
 *
 * <b>One section, three voices</b> (ReadingVoice): a person's chart; the sky at its place,
 * read for public health there; and on Transits, the sky's planets in the person's health
 * houses and in hard aspect to the person's planets now.
 */
final class BodyReading {

    private BodyReading() {
    }

    /** The planets the section reads, with the North Node for the breath. */
    static final List<String> PLANETS = List.of("Sun", "Moon", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "North Node");

    /** The health houses, in the order the reference gives them. */
    static final int[] HEALTH_HOUSES = {1, 6, 8, 12};

    /** The hard aspects, as the reference names them: the majors' hard three and two minors. */
    static final Aspects.Type[] HARD = {Aspects.Type.CONJUNCTION, Aspects.Type.SQUARE,
        Aspects.Type.OPPOSITION, Aspects.Type.SEMISQUARE, Aspects.Type.SESQUIQUADRATE};

    /** Orbs: the majors wider, the two minors tight. A transit takes a tighter one. */
    private static double orb(Aspects.Type t, boolean transit) {
        boolean minor = t == Aspects.Type.SEMISQUARE || t == Aspects.Type.SESQUIQUADRATE;
        return transit ? (minor ? 1.0 : 2.0) : (minor ? 2.0 : 6.0);
    }

    /**
     * The section.
     *
     * @param chart the chart whose planets are read: the person's, or the sky's
     * @param houses the chart whose houses they fall in: the same, or the person's on Transits
     * @param natal on Transits, the person's chart the sky's planets aspect; else null
     */
    static String html(ReadingVoice voice, ChartFrame chart, ChartFrame houses, ChartFrame natal,
            boolean showWhy) {
        if (chart == null || houses == null) {
            return "";
        }
        InterpretationService svc = InterpretationService.getInstance();
        StringBuilder sb = new StringBuilder(SkyOnChart.sectionHeading(switch (voice) {
            case PERSON -> "The body";
            case SKY -> "The body: public health";
            case TRANSIT -> "The body: what the sky stresses now";
        }));
        String intro = svc.getBodyPart("intro");
        if (intro != null) {
            sb.append("<p style='color:#AAAAAA; font-size:12px;'><i>").append(esc(intro))
                .append("</i></p>");
        }
        boolean transit = voice == ReadingVoice.TRANSIT;

        // The health houses: a planet in the 1st, 6th, 8th or 12th.
        List<ReadingCard> houseCards = new ArrayList<>();
        for (int h : HEALTH_HOUSES) {
            for (String p : PLANETS) {
                double lon = lon(chart, p);
                if (Double.isNaN(lon) || Zodiac.houseOf(lon, houses.cusps) != h) {
                    continue;
                }
                ReadingCard c = houseCard(svc, p, h, lon, transit, voice);
                if (c != null) {
                    houseCards.add(c);
                }
            }
        }

        // The aspect signatures: a hard aspect between a pair the reference names.
        List<ReadingCard> sigCards = new ArrayList<>();
        ChartFrame other = transit ? natal : chart;
        if (other != null) {
            for (int i = 0; i < PLANETS.size(); i++) {
                for (int j = transit ? 0 : i + 1; j < PLANETS.size(); j++) {
                    String a = PLANETS.get(i);
                    String b = PLANETS.get(j);
                    if (a.equals(b) && !transit) {
                        continue;
                    }
                    String key = key(a, b);
                    if (key == null || svc.getBodyPart("sig." + key + ".outcome") == null) {
                        continue;
                    }
                    double la = lon(chart, a);
                    double lb = lon(other, b);
                    Aspects.Type t = hard(la, lb, transit);
                    if (t == null) {
                        continue;
                    }
                    ReadingCard c = sigCard(svc, a, b, key, t, Math.abs(Aspects.separation(la, lb)
                        - t.exactAngle), transit);
                    if (c != null) {
                        sigCards.add(c);
                    }
                }
            }
        }

        // The body by sign: where each planet sits in the body.
        StringBuilder bySign = new StringBuilder();
        List<String> signWhy = new ArrayList<>();
        for (String p : PLANETS) {
            double lon = lon(chart, p);
            if (Double.isNaN(lon) || "North Node".equals(p)) {
                continue;
            }
            String sign = Zodiac.signName(lon);
            String areas = svc.getBodyPart("sign." + sign);
            if (areas == null) {
                continue;
            }
            bySign.append(bySign.length() == 0 ? "" : "\n\n").append(p).append(" in ")
                .append(capitalise(sign)).append(": ").append(areas).append('.');
            signWhy.add(p + " in " + capitalise(sign));
        }

        if (!houseCards.isEmpty()) {
            sb.append(sub("The health houses")).append(ReadingCard.html(houseCards, showWhy));
        }
        if (!sigCards.isEmpty()) {
            sb.append(sub("Aspect signatures")).append(ReadingCard.html(sigCards, showWhy));
        }
        if (houseCards.isEmpty() && sigCards.isEmpty()) {
            sb.append("<p style='color:#AAAAAA;'>No planet is in a health house and no health"
                + " signature is in force" + (transit ? " right now" : "") + ".</p>");
        }
        if (bySign.length() > 0) {
            sb.append(sub("The body by sign")).append(ReadingCard.html(List.of(new ReadingCard(
                "WHERE EACH PLANET SITS IN THE BODY", null, bySign.toString(), signWhy)),
                showWhy));
        }
        return sb.toString();
    }

    /** "Mars in the 1st House (Physical Constitution & Head/Face)", in three parts. */
    private static ReadingCard houseCard(InterpretationService svc, String p, int h, double lon,
            boolean transit, ReadingVoice voice) {
        String id = id(p);
        String title = svc.getBodyPart("house." + h + ".title");
        String rules = svc.getBodyPart("house." + h + ".rules");
        String how = svc.getBodyPart("house." + h + (transit ? ".transit" : ".natal"));
        String prules = svc.getBodyPart("planet." + id + ".rules");
        String stress = svc.getBodyPart("planet." + id + ".stress");
        String care = svc.getBodyPart("planet." + id + ".care");
        if (title == null || rules == null || how == null || prules == null || stress == null
                || care == null) {
            return null;
        }
        String who = voice == ReadingVoice.SKY ? "public health here" : "the body";
        return new ReadingCard(transit ? "THE SKY IN A HEALTH HOUSE" : "A HEALTH HOUSE",
            p + " in the " + Zodiac.ordinal(h) + " House (" + title + ")",
            "Body and rulership: " + rules + " " + subject(p) + " rules " + prules + ".\n\n"
                + "Health expression: " + String.format(how, article(p)) + " Under stress or hard"
                + " aspects, it tends to show in " + who + " as " + stress + ".\n\n"
                + "Triggers and care: " + care,
            List.of((transit ? "transiting " : "") + p + " in " + capitalise(Zodiac.signName(lon))
                + ", " + Zodiac.ordinal(h) + " house"));
    }

    /** "Hard aspect between Mars and the Sun (Vital Core & Overexertion)", in three parts. */
    private static ReadingCard sigCard(InterpretationService svc, String a, String b, String key,
            Aspects.Type t, double off, boolean transit) {
        String title = svc.getBodyPart("sig." + key + ".title");
        String outcome = svc.getBodyPart("sig." + key + ".outcome");
        String ra = svc.getBodyPart("planet." + id(a) + ".rules");
        String rb = svc.getBodyPart("planet." + id(b) + ".rules");
        if (title == null || outcome == null || ra == null || rb == null) {
            return null;
        }
        String aspect = t.label.toLowerCase(Locale.ROOT);
        String pair = transit ? "transiting " + a + " and your " + b
            : article(a) + " and " + article(b);
        return new ReadingCard(transit ? "A HEALTH SIGNATURE NOW" : "A HEALTH SIGNATURE",
            "Hard aspect between " + pair + " (" + title + ")",
            "Core interaction: " + subject(a) + " rules " + ra + "; " + article(b) + " rules "
                + rb + ".\n\n"
                + "Stressful configuration: a " + aspect + ", " + String.format(Locale.ROOT,
                    "%.1f", off) + " degrees from exact" + (transit ? ", in force now" : "")
                + ".\n\nHealth outcome: " + outcome,
            List.of((transit ? "transiting " : "") + a + " " + aspect + " " + (transit
                ? "natal " : "") + b + String.format(Locale.ROOT, ", %.1f° from exact", off)));
    }

    /** The hard aspect two longitudes make, nearest first, or null. */
    private static Aspects.Type hard(double a, double b, boolean transit) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return null;
        }
        double sep = Aspects.separation(a, b);
        Aspects.Type best = null;
        double bestOff = Double.MAX_VALUE;
        for (Aspects.Type t : HARD) {
            double off = Math.abs(sep - t.exactAngle);
            if (off <= orb(t, transit) && off < bestOff) {
                best = t;
                bestOff = off;
            }
        }
        return best;
    }

    /** The signature's key: the two ids in alphabetical order, "mars_sun". */
    private static String key(String a, String b) {
        String x = id(a);
        String y = id(b);
        if (x.isEmpty() || y.isEmpty() || x.equals(y)) {
            return null;
        }
        return x.compareTo(y) < 0 ? x + "_" + y : y + "_" + x;
    }

    private static double lon(ChartFrame f, String name) {
        ChartFrame.Body b = f.body(name);
        return b == null || !b.ok ? Double.NaN : b.lon;
    }

    private static String sub(String title) {
        return "<h3 style='color:#7FB3FF; margin-top:14px; margin-bottom:6px;'>" + esc(title)
            + "</h3>";
    }

    /** "the Sun", "the Moon", "the North Node", "Mars". */
    private static String article(String p) {
        return "Sun".equals(p) || "Moon".equals(p) || "North Node".equals(p) ? "the " + p : p;
    }

    /** As above, starting a sentence: "The Sun". */
    private static String subject(String p) {
        return capitalise(article(p));
    }

    private static String id(String name) {
        Bodies.Def d = Bodies.byName(name);
        return d == null ? "" : d.id;
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
