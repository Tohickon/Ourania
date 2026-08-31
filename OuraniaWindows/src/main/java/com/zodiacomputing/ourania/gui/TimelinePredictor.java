package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Aspects;
import java.util.Arrays;
import java.util.List;

public class TimelinePredictor {

    public static String generate(ChartFrame f, ChartFrame tf) {
        StringBuilder sb = new StringBuilder();
        InterpretationService svc = InterpretationService.getInstance();
        
        ChartFrame chartToUse = (tf != null) ? tf : f;

        sb.append("<html><body style='font-family: sans-serif; font-size: 14px; margin: 15px; color: #E0E0E0; background-color: #000000; line-height: 1.6;'>");
        sb.append("<h1 style='color: #FFFFFF; border-bottom: 1px solid #444; padding-bottom: 5px;'>Timeline Synthesis</h1>");
        
        List<String> pastBodies = Arrays.asList("South Node", "Chiron", "Pluto", "Saturn", "Descendant", "IC");
        List<String> presentBodies = Arrays.asList("Sun", "Moon", "Mercury", "Venus", "Mars", "Ascendant", "Midheaven", "Lilith", "Ceres", "Pallas", "Juno", "Vesta");
        List<String> futureBodies = Arrays.asList("North Node", "Jupiter", "Uranus", "Neptune", "Part of Fortune", "Vertex");

        List<Aspects.Hit> aspects = Aspects.betweenBodies(chartToUse);
        aspects.addAll(Aspects.toAngles(chartToUse));

        // 1. Past
        sb.append("<h2 style='color: #FFD700;'>The Momentary Past</h2>");
        sb.append("<p>");
        sb.append("Drawing upon deep foundational roots and prior cycles, ");
        sb.append(buildParagraph(svc, chartToUse, pastBodies, aspects, pastBodies, presentBodies, futureBodies, false));
        sb.append("</p>");

        // 2. Present
        sb.append("<h2 style='color: #FFD700;'>The Present</h2>");
        sb.append("<p>");
        sb.append("Currently, the immediate active forces dictate that ");
        sb.append(buildParagraph(svc, chartToUse, presentBodies, aspects, pastBodies, presentBodies, futureBodies, true));
        sb.append("</p>");

        // 3. Future
        sb.append("<h2 style='color: #FFD700;'>The Future</h2>");
        sb.append("<p>");
        sb.append("Looking ahead, the trajectory set in motion suggests that ");
        sb.append(buildParagraph(svc, chartToUse, futureBodies, aspects, pastBodies, presentBodies, futureBodies, false));
        sb.append("</p>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String buildParagraph(InterpretationService svc, ChartFrame f, List<String> targetBodies, List<Aspects.Hit> aspects, List<String> past, List<String> present, List<String> future, boolean includeUnmapped) {
        StringBuilder pb = new StringBuilder();
        
        String[] transitions = new String[] {
            " Furthermore, ", " Meanwhile, ", " In conjunction with this, ", 
            " Simultaneously, ", " Adding to this dynamic, ", " At the same time, ",
            " Notably, ", " We also see that ", " Deepening the theme, "
        };
        int tIndex = 0;
        boolean first = true;

        List<String> allBodies = new java.util.ArrayList<>(Arrays.asList(
            "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto",
            "North Node", "South Node", "Chiron", "Lilith", "Part of Fortune", "Vertex", "Ceres", "Pallas", "Juno", "Vesta"
        ));
        
        for (String bodyName : allBodies) {
            boolean shouldInclude = targetBodies.contains(bodyName) || (includeUnmapped && !past.contains(bodyName) && !present.contains(bodyName) && !future.contains(bodyName));
            if (!shouldInclude) continue;
            
            ChartFrame.Body b = null;
            try {
                b = f.body(bodyName);
            } catch (Exception e) {
                continue;
            }
            if (b == null || !b.ok) continue;

            if (!first) {
                pb.append(transitions[tIndex % transitions.length]);
                tIndex++;
            }
            first = false;

            String sign = com.zodiacomputing.ourania.astro.Zodiac.signName(b.lon);
            int house = com.zodiacomputing.ourania.astro.Zodiac.houseOf(b.lon, f.cusps);

            // Prose.summary, not the raw value. Each interpretation is
            // "<b>summary sentence.</b><br><br>a long body", and this used to paste the
            // WHOLE thing through .toLowerCase().replace(".", "") - which lowercased every
            // proper noun ("saturn trine sun") and stripped every full stop rather than one
            // trailing one, welding the sentences together. Measured before the fix: one
            // period per 396 characters across 44,030 characters of "three paragraphs".
            String core = Prose.summary(svc.getBodyCore(bodyName));
            String signText = Prose.summary(svc.getPlanetInSign(bodyName, sign));
            String houseText = house > 0 ? Prose.summary(svc.getPlanetInHouse(bodyName, house)) : "";

            pb.append("<b>").append(bodyName).append("</b>");
            if (!core.isEmpty()) {
                pb.append(" (").append(lowerFirst(core)).append(")");
            }
            // Many summaries name the placement themselves - "In its ruling sign, Saturn in
            // Capricorn is the ultimate architect..." - so prefixing "Saturn in Capricorn -"
            // said it twice in one breath. Where the sentence already carries the name, let
            // it stand on its own and keep its capitalisation.
            if (mentions(signText, bodyName)) {
                pb.append(" - ").append(signText).append(". ");
            } else {
                pb.append(" in ").append(capitalise(sign));
                if (!signText.isEmpty()) {
                    pb.append(" - ").append(lowerFirst(signText));
                }
                pb.append(". ");
            }

            if (house > 0 && !houseText.isEmpty()) {
                pb.append("In the ").append(ordinal(house)).append(" house, ")
                  .append(lowerFirst(houseText)).append(". ");
            }

            // One aspect per body, not two: this is the summary tier, and a second clause
            // per body is what turned three paragraphs into forty thousand characters.
            for (Aspects.Hit h : aspects) {
                if (!h.a.equals(bodyName) && !h.b.equals(bodyName)) {
                    continue;
                }
                String other = h.a.equals(bodyName) ? h.b : h.a;
                String aspectText = Prose.summary(svc.getAspect(bodyName, other, h.type.label));
                if (!aspectText.isEmpty()) {
                    // Same doubling: "Its quincunx to Venus means Chiron quincunx Venus
                    // indicates..." - the aspect prose names both ends already.
                    if (mentions(aspectText, bodyName)) {
                        pb.append(capitalise(aspectText)).append(". ");
                    } else {
                        pb.append("Its ").append(h.type.label.toLowerCase()).append(" to <b>")
                          .append(other).append("</b> means ").append(lowerFirst(aspectText)).append(". ");
                    }
                    break;
                }
            }
        }
        
        if (includeUnmapped) {
            String[] angles = new String[]{"Ascendant", "Midheaven", "Descendant", "IC"};
            for (String angleName : angles) {
                double lon = 0;
                if (angleName.equals("Ascendant")) lon = f.asc;
                if (angleName.equals("Midheaven")) lon = f.mc;
                if (angleName.equals("Descendant")) lon = (f.asc + 180) % 360;
                if (angleName.equals("IC")) lon = (f.mc + 180) % 360;
                
                if (lon != 0.0) {
                    String sign = com.zodiacomputing.ourania.astro.Zodiac.signName(lon);
                    int deg = (int) Math.floor(lon % 30) + 1;
                    String sabian = svc.getSabianSymbol(sign, deg);
                    
                    if (!first) {
                        pb.append(transitions[tIndex % transitions.length]);
                        tIndex++;
                    }
                    first = false;
                    pb.append("the <b>").append(angleName).append("</b> acts as a structural pillar in ").append(sign).append(" at ").append(deg).append("&deg;, carrying the Sabian archetype: '<i>").append(sabian).append("</i>'. ");
                }
            }
        }
        
        return pb.toString();
    }

    /**
     * Whether a summary already names its subject, so a lead-in would say it twice.
     *
     * Word-boundary rather than plain contains: "Eros" must not match inside "Erosszsolt",
     * and "Sun" must not match inside "Sunday".
     */
    private static boolean mentions(String text, String name) {
        if (text == null || text.isEmpty() || name == null || name.isEmpty()) {
            return false;
        }
        int at = text.indexOf(name);
        while (at >= 0) {
            boolean leftOk = at == 0 || !Character.isLetter(text.charAt(at - 1));
            int end = at + name.length();
            boolean rightOk = end >= text.length() || !Character.isLetter(text.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            at = text.indexOf(name, at + 1);
        }
        return false;
    }

    /**
     * Lowercases a leading word unless it is one English capitalises anyway. Without this,
     * moving an interpretation into the middle of a clause produced "saturn descends to the
     * Imum Coeli" - the same defect PlainSnapshot hit, from the same data.
     */
    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int end = s.indexOf(' ');
        String head = end < 0 ? s : s.substring(0, end);
        for (String proper : PROPER) {
            if (head.equals(proper)) {
                return s;
            }
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static final String[] PROPER = {
        "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus",
        "Neptune", "Pluto", "Chiron", "Ceres", "Pallas", "Juno", "Vesta", "Eris",
        "Eros", "Hygiea", "Nessus", "Pholus", "Lilith"
    };

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String ordinal(int n) {
        switch (n) {
            case 1:  return "1st";
            case 2:  return "2nd";
            case 3:  return "3rd";
            case 21: return "21st";
            case 22: return "22nd";
            case 23: return "23rd";
            default: return n + "th";
        }
    }
}
