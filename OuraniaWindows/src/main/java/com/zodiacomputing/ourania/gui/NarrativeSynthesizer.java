package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AspectPatterns;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Convergence;
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Sect;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import com.zodiacomputing.ourania.astro.TransferOfLight;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.SkymapPanel.YearScan;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NarrativeSynthesizer {

    public static String generateReport(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked, Themes.Result t, ChartFrame tf, Profection prof, List<Transits.Hit> hits, YearScan scan, List<Convergence.Target> convergence, boolean withTime) {
        StringBuilder sb = new StringBuilder();
        InterpretationService svc = InterpretationService.getInstance();
        boolean isDiurnal = g.diurnal;

        sb.append("<html><body style='font-family: sans-serif; font-size: 14px; margin: 15px; color: #E0E0E0; background-color: #000000;'>");
        sb.append("<h1 style='color: #FFFFFF; border-bottom: 1px solid #444; padding-bottom: 5px;'>Chart Synthesis</h1>");
        
        sb.append("<p style='font-style: italic; color: #AAAAAA;'>Based on the principles and mechanics of scientific astrology, the data provided outlines a highly dynamic and structurally complex chart. Here is a mechanical and synthesized interpretation of the forces at work.</p>");

        // 1. Core Architecture
        sb.append("<h2 style='color: #FFD700;'>1. Core Architecture: The Chart's Signature</h2>");
        String sectName = isDiurnal ? "diurnal (day)" : "nocturnal (night)";
        sb.append("<p>This is a <b>").append(sectName).append("</b> chart, meaning the <b>").append(g.sectLight).append("</b> is the master of the sect, <b>").append(Sect.beneficOfSect(isDiurnal)).append("</b> is your greatest ally (the benefic of sect), and <b>").append(g.outOfSectMalefic).append("</b> is your primary friction point (the malefic contrary to sect).</p>");
        
        String leadBody = (g.shapeHandle != null && !g.shapeHandle.isEmpty()) ? g.shapeHandle : ranked.get(0).body;
        ChartFrame.Body leadPlanet = f.body(leadBody);
        String leadSign = leadPlanet != null ? Zodiac.signName(leadPlanet.lon) : "";
        sb.append("<p>The chart is shaped as a <b>").append(g.shape != null ? g.shape.name() : "Standard").append("</b>, led by <b>").append(leadBody).append(" in ").append(leadSign).append("</b>. This mechanical shape implies a personality that is highly driven and structured by this leading force.</p>");
        
        if (!g.dominantElements.isEmpty()) {
            sb.append("<p>The dominant element is <b>").append(String.join(", ", g.dominantElements)).append("</b>, emphasizing that baseline operating system.</p>");
        }

        // 2. Planetary Placements
        sb.append("<h2 style='color: #FFD700;'>2. Planetary Placements (Ranked by Prominence)</h2>");
        sb.append("<p>The following details the structural function and narrative interpretation of all placements in the chart, in order of their strength:</p>");
        for (BodyScore.Vector v : ranked) {
            ChartFrame.Body b = f.body(v.body);
            Dignity.Result dr = Dignity.evaluate(v.body, b.lon, isDiurnal);
            sb.append("<div style='margin-bottom: 25px; padding-bottom: 10px; border-bottom: 1px dotted #333;'>");
            sb.append("<h3 style='color: #FFFFFF; margin-bottom: 5px;'>").append(v.body).append(" (").append(String.format("%.1f%%", v.prominence * 100)).append(" Prominence)</h3>");
            
            sb.append("<p style='font-size: 13px; color: #BBBBBB;'>");
            if (dr.score >= Dignity.PTS_EXALTATION) sb.append("<b>Highly Dignified.</b> ");
            if (dr.domicile) sb.append("<b>In Domicile.</b> ");
            if (v.house > 0) sb.append("Residing in House ").append(v.house).append(". ");
            sb.append("</p>");
            
            String core = svc.getBodyCore(v.body);
            if (core != null && !core.isEmpty()) {
                sb.append("<p><b>Core function:</b> ").append(core).append("</p>");
            }
            
            String sign = Zodiac.signName(b.lon);
            String signText = svc.getPlanetInSign(v.body, sign);
            if (!signText.startsWith("Interpretation not found")) {
                sb.append("<p><b>In ").append(Character.toUpperCase(sign.charAt(0))).append(sign.substring(1)).append(":</b> ").append(signText).append("</p>");
            }
            
            if (v.house > 0) {
                String houseText = svc.getPlanetInHouse(v.body, v.house);
                if (!houseText.startsWith("Interpretation not found")) {
                    sb.append("<p><b>In House ").append(v.house).append(":</b> ").append(houseText).append("</p>");
                }
            }

            // Angles
            if (v.nearestAngle != null && v.angularity > 0.5) {
                String angleText = svc.getAngleInterpretation(v.nearestAngle, sign);
                if (angleText != null && !angleText.isEmpty() && !angleText.startsWith("Interpretation not found")) {
                    sb.append("<p><b>On the ").append(v.nearestAngle).append(":</b> ").append(angleText).append("</p>");
                }
            }
            
            // Major Aspects
            if (v.aspects != null && !v.aspects.isEmpty()) {
                sb.append("<p><b>Major Contacts:</b></p><ul>");
                for (int i = 0; i < Math.min(3, v.aspects.size()); i++) {
                    Aspects.Hit h = v.aspects.get(i);
                    String other = v.body.equals(h.a) ? h.b : h.a;
                    String aspectText = svc.getAspect(v.body, other, h.type.label);
                    if (!aspectText.startsWith("Interpretation not found") && !aspectText.startsWith("General ")) {
                        sb.append("<li><b>").append(h.type.label).append(" to ").append(other).append(":</b> ").append(aspectText).append("</li>");
                    } else {
                        sb.append("<li><b>").append(h.type.label).append(" to ").append(other).append("</b> (").append(String.format("%.1f&deg;", h.offBy)).append(").</li>");
                    }
                }
                sb.append("</ul>");
            }
            sb.append("</div>");
        }

        // 3. Structural Tensions
        sb.append("<h2 style='color: #FFD700;'>3. Structural Tensions (The Unresolved Vectors)</h2>");
        sb.append("<ul>");
        for (Themes.Contradiction contradiction : t.contradictions) {
            sb.append("<li><b>Clash:</b> ").append(contradiction.themeA).append(" vs ").append(contradiction.themeB).append("</li>");
        }
        sb.append("<li>Elemental Distribution: Fire ").append(String.format("%.1f", g.elements.getOrDefault("Fire", 0.0))).append(", Water ").append(String.format("%.1f", g.elements.getOrDefault("Water", 0.0))).append(", Air ").append(String.format("%.1f", g.elements.getOrDefault("Air", 0.0))).append(", Earth ").append(String.format("%.1f", g.elements.getOrDefault("Earth", 0.0))).append(".</li>");
        sb.append("</ul>");

        // 4. Current Chronometry
        sb.append("<h2 style='color: #FFD700;'>4. Current Chronometry</h2>");
        if (withTime && prof != null) {
            sb.append("<p>You are currently in a <b>House ").append(prof.house).append(" Profection (").append(prof.sign).append(")</b> at age ").append(String.format("%.0f", prof.age)).append(". ");
            sb.append("The structural focus of your year shifts to the ").append(prof.house).append("th House, ruled by <b>").append(prof.lord).append("</b>. ");
            sb.append("As the 'Lord of the Year', themes surrounding this planet are paramount.</p>");
        } else {
            sb.append("<p><i>Transit data not enabled to calculate current chronometry. Enable transits to see Time Layers.</i></p>");
        }

        // 5. Complex Geometric Circuitry
        sb.append("<h2 style='color: #FFD700;'>5. Complex Geometric Circuitry (Aspect Patterns)</h2>");
        List<Aspects.Hit> allAspects = new ArrayList<>();
        allAspects.addAll(Aspects.betweenBodies(f));
        allAspects.addAll(Aspects.toAngles(f));
        List<AspectPatterns.Pattern> patterns = AspectPatterns.findPatterns(allAspects);
        if (patterns.isEmpty()) {
            sb.append("<p>No major closed-circuit aspect patterns (like Grand Trines or T-Squares) found.</p>");
        } else {
            sb.append("<ul>");
            for (AspectPatterns.Pattern p : patterns) {
                sb.append("<li><b>").append(p.name).append("</b> involving ").append(String.join(", ", p.bodies));
                if (p.apex != null && !p.apex.isEmpty()) {
                    sb.append(" (Apex: <b>").append(p.apex).append("</b>)");
                }
                sb.append("</li>");
            }
            sb.append("</ul>");
        }

        // 6. The Lunar Engine
        sb.append("<h2 style='color: #FFD700;'>6. The Lunar Engine: Translation of Light</h2>");
        List<String> transfers = TransferOfLight.findTransfers(f, Aspects.betweenBodies(f));
        if (transfers.isEmpty()) {
            sb.append("<p>No active classical translations of light detected.</p>");
        } else {
            sb.append("<ul>");
            for (String tr : transfers) {
                sb.append("<li>").append(tr).append("</li>");
            }
            sb.append("</ul>");
        }

        // 7. House Mechanics
        sb.append("<h2 style='color: #FFD700;'>7. House Mechanics: Competence vs. Chaos</h2>");
        List<Topics.Topic> topicsList = Topics.analyse(f, ranked);
        List<Integer> strongHouses = new ArrayList<>();
        List<Integer> weakHouses = new ArrayList<>();
        for (Topics.Topic top : topicsList) {
            if (top.agreement == Topics.Agreement.ALL_STRONG || top.agreement == Topics.Agreement.QUIET_COMPETENCE) {
                strongHouses.add(top.house);
            } else if (top.agreement == Topics.Agreement.ALL_WEAK || top.agreement == Topics.Agreement.ACTIVITY_WITHOUT_FOLLOW_THROUGH) {
                weakHouses.add(top.house);
            }
        }
        sb.append("<p><b>The Zones of Quiet Competence:</b> Houses ").append(strongHouses.isEmpty() ? "None" : strongHouses.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(". These areas operate with strategic stability.</p>");
        sb.append("<p><b>The Zones of Friction:</b> Houses ").append(weakHouses.isEmpty() ? "None" : weakHouses.stream().map(String::valueOf).collect(Collectors.joining(", "))).append(". You may expend massive energy here, but results require significant effort to consolidate.</p>");

        // 8. Angles and Sabian Archetypes
        sb.append("<h2 style='color: #FFD700;'>8. The Angles and their Sabian Archetypes</h2>");
        sb.append("<ul>");
        if (f.asc != 0.0) {
            String ascSign = Zodiac.signName(f.asc);
            int ascDeg = (int) Math.floor(f.asc % 30) + 1;
            sb.append("<li><b>Ascendant (").append(ascDeg).append("° ").append(ascSign).append("):</b> ").append(svc.getSabianSymbol(ascSign, ascDeg)).append("</li>");
            
            double dsc = (f.asc + 180) % 360;
            String dscSign = Zodiac.signName(dsc);
            int dscDeg = (int) Math.floor(dsc % 30) + 1;
            sb.append("<li><b>Descendant (").append(dscDeg).append("° ").append(dscSign).append("):</b> ").append(svc.getSabianSymbol(dscSign, dscDeg)).append("</li>");
        }
        
        if (f.mc != 0.0) {
            String mcSign = Zodiac.signName(f.mc);
            int mcDeg = (int) Math.floor(f.mc % 30) + 1;
            sb.append("<li><b>Midheaven (").append(mcDeg).append("° ").append(mcSign).append("):</b> ").append(svc.getSabianSymbol(mcSign, mcDeg)).append("</li>");
            
            double ic = (f.mc + 180) % 360;
            String icSign = Zodiac.signName(ic);
            int icDeg = (int) Math.floor(ic % 30) + 1;
            sb.append("<li><b>IC (").append(icDeg).append("° ").append(icSign).append("):</b> ").append(svc.getSabianSymbol(icSign, icDeg)).append("</li>");
        }
        sb.append("</ul>");

        // 9. Specific Chronological Triggers
        sb.append("<h2 style='color: #FFD700;'>9. Specific Chronological Triggers</h2>");
        if (withTime && scan != null && convergence != null) {
            sb.append("<p>Upcoming significant convergence events across the current profection year:</p><ul>");
            for (Convergence.Target target : convergence) {
                if (target.score > 0.0) {
                    sb.append("<li><b>Converging on ").append(target.natal).append("</b>: ").append(String.format("%.1f", target.score * 100)).append("% intensity.<br>");
                    sb.append("<i>Triggers:</i> ");
                    for (Convergence.Witness w : target.witnesses) {
                        sb.append(w.family.name()).append(" (");
                        if (w.movingBody != null) sb.append(w.movingBody).append(" ");
                        if (w.aspect != null) sb.append(w.aspect.label).append(" ");
                        sb.append("), ");
                    }
                    sb.append("</li>");
                }
            }
            if (convergence.isEmpty()) {
                sb.append("<li>No major convergence spikes in this profection year.</li>");
            }
            sb.append("</ul>");
        } else {
            sb.append("<p><i>Transit data not enabled. Enable transits to scan upcoming timeline events.</i></p>");
        }

        // 10. Current Transits
        sb.append("<h2 style='color: #FFD700;'>10. Current Transits</h2>");
        if (withTime && hits != null && !hits.isEmpty()) {
            java.util.Map<String, List<Transits.Hit>> byTarget = new java.util.LinkedHashMap<>();
            for (Transits.Hit h : hits) {
                byTarget.computeIfAbsent(h.natal, k -> new java.util.ArrayList<>()).add(h);
            }
            for (java.util.Map.Entry<String, List<Transits.Hit>> e : byTarget.entrySet()) {
                sb.append("<h3 style='color: #FFFFFF;'>Transits to Natal ").append(e.getKey()).append(" (").append(e.getValue().get(0).why).append(")</h3>");
                sb.append("<ul>");
                for (Transits.Hit h : e.getValue()) {
                    sb.append("<li style='margin-bottom: 10px;'><b>").append(h.transiting).append(h.transitRetrograde ? " Rx" : "").append(" ")
                      .append(h.type.label).append(" Natal ").append(e.getKey()).append("</b>")
                      .append(" (").append(String.format("%.1f", h.offBy)).append("\u00B0 ").append(h.applying ? "applying" : "separating").append("):<br>");
                    sb.append(svc.getTransitAspect(h.transiting, e.getKey(), h.type.label));
                    sb.append("</li>");
                }
                sb.append("</ul>");
            }
        } else if (withTime) {
            sb.append("<p>No significant current transits activating major chart themes at this moment.</p>");
        } else {
            sb.append("<p><i>Transit data not enabled. Enable transits to view current transits.</i></p>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }
}