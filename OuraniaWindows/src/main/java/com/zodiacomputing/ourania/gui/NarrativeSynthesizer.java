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

    /**
     * The Sabian symbol for a degree, in the voice the chart calls for.
     *
     * One helper rather than the same ternary at four call sites - the angles section reads
     * four degrees and the fork has to be identical at each, which is exactly the shape of
     * thing that drifts when it is written out four times.
     */
    private static String sabian(InterpretationService svc, String sign, int deg, boolean rel) {
        String s = rel ? svc.getCompositeSabian(sign, deg) : null;
        return s != null ? s : svc.getSabianSymbol(sign, deg);
    }

    /** Natal voice. Kept so every pre-2026-08-31 caller compiles unchanged. */
    public static String generateReport(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked, Themes.Result t, ChartFrame tf, Profection prof, List<Transits.Hit> hits, YearScan scan, List<Convergence.Target> convergence, boolean withTime) {
        return generateReport(f, g, ranked, t, tf, prof, hits, scan, convergence, withTime, false);
    }

    /**
     * The narrative reading.
     *
     * <b>{@code relationship} changes which dataset is read and which sections run</b>, because
     * a composite is a chart of a pairing and the natal prose speaks to a person. Before
     * 2026-08-31 this method fetched getPlanetInSign / getPlanetInHouse / getAspect regardless
     * of mode, so a composite reading described the couple's positions in the second person -
     * "your identity", "your greatest ally" - while the composite datasets sat unread. It never
     * called a single getComposite* method.
     *
     * <b>The age-keyed sections are suppressed for a relationship, not reworded.</b> Profection
     * and the solar return are annual techniques keyed to a birthday: an age in years and a
     * return of the Sun to its natal degree. A composite has no birthday - its Julian day is
     * the midpoint of two BIRTHS - so "age" came out as the mean of the partners' ages and was
     * printed as the age of the relationship. On the 1984/1967 fixture that is 41 and 59
     * reported as 50. Transits are kept: they need only positions, and transits to the
     * composite are the standard timing technique for a relationship chart.
     */
    public static String generateReport(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked, Themes.Result t, ChartFrame tf, Profection prof, List<Transits.Hit> hits, YearScan scan, List<Convergence.Target> convergence, boolean withTime, boolean relationship) {
        StringBuilder sb = new StringBuilder();
        InterpretationService svc = InterpretationService.getInstance();
        boolean isDiurnal = g.diurnal;

        sb.append("<html><body style='font-family: sans-serif; font-size: 14px; margin: 15px; color: #E0E0E0; background-color: #000000;'>");
        sb.append("<h1 style='color: #FFFFFF; border-bottom: 1px solid #444; padding-bottom: 5px;'>Chart Synthesis</h1>");
        
        sb.append("<p style='font-style: italic; color: #AAAAAA;'>Based on the principles and mechanics of scientific astrology, the data provided outlines a highly dynamic and structurally complex chart. Here is a mechanical and synthesized interpretation of the forces at work.</p>");

        // 1. Core Architecture
        sb.append("<h2 style='color: #FFD700;'>1. Core Architecture: The Chart's Signature</h2>");
        String sectName = isDiurnal ? "diurnal (day)" : "nocturnal (night)";
        sb.append("<p>This is a <b>").append(sectName).append("</b> chart, meaning the <b>").append(g.sectLight).append("</b> is the master of the sect, <b>").append(Sect.beneficOfSect(isDiurnal)).append("</b> is " + (relationship ? "the relationship's" : "your") + " greatest ally (the benefic of sect), and <b>").append(g.outOfSectMalefic).append("</b> is " + (relationship ? "its" : "your") + " primary friction point (the malefic contrary to sect).</p>");
        
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
            // Composite prose for a composite chart. The natal entry describes a person; the
            // composite entry describes what the pairing does with that placement.
            String signText = relationship ? svc.getCompositePlanetSign(v.body, sign) : null;
            if (signText == null) {
                signText = svc.getPlanetInSign(v.body, sign);
            }
            if (!signText.startsWith("Interpretation not found")) {
                sb.append("<p><b>In ").append(Character.toUpperCase(sign.charAt(0))).append(sign.substring(1)).append(":</b> ").append(signText).append("</p>");
            }
            
            if (v.house > 0) {
                String houseText = relationship
                    ? svc.getCompositePlanetHouse(v.body, v.house) : null;
                if (houseText == null) {
                    houseText = svc.getPlanetInHouse(v.body, v.house);
                }
                if (!houseText.startsWith("Interpretation not found")) {
                    sb.append("<p><b>In House ").append(v.house).append(":</b> ").append(houseText).append("</p>");
                }
            }

            // Angles
            if (v.nearestAngle != null && v.angularity > 0.5) {
                String angleText = relationship
                    ? svc.getCompositeAngle(v.nearestAngle, sign) : null;
                if (angleText == null) {
                    angleText = svc.getAngleInterpretation(v.nearestAngle, sign);
                }
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
                    String aspectText = relationship
                        ? svc.getCompositeAspect(v.body, other, h.type.label) : null;
                    // <b>With no composite reading for the pair this borrows the NATAL one</b>
                    // - and the natal corpus describes two drives inside one person. The
                    // geometry is right and the voice is wrong: nothing in that sentence knows
                    // both ends belong to a relationship. As of 2026-08-31 that is 1,404 of
                    // 3,300 cells, every one of them a minor aspect.
                    //
                    // The frame is the one sentence the natal dataset cannot say, and it has
                    // sat in composite_aspects.json since that file was written - eleven
                    // entries, one per aspect type. <b>InterpretationPanel has shown it all
                    // along; this surface never called it.</b> Same data, same getter, one
                    // reader taught and its neighbour left behind.
                    //
                    // <b>Deliberately narrower here than on the panel</b>, which frames every
                    // relationship aspect it renders. That page carries one aspect; this report
                    // carries three per body across every body, so an unconditional frame would
                    // repeat one of eleven sentences dozens of times. It appears only where it
                    // does work - in front of borrowed natal wording. A difference driven by
                    // the surface, not by drift.
                    String frame = null;
                    if (aspectText == null) {
                        aspectText = svc.getAspect(v.body, other, h.type.label);
                        if (relationship) {
                            frame = svc.getCompositeAspectFrame(h.type.label);
                            // <b>When the natal fallback is ITSELF only the aspect general</b>,
                            // the frame already says that in the relationship's voice, so it
                            // replaces the text rather than sitting on top of it. Printed as a
                            // pair they said the same thing twice - "half a square and it
                            // behaves like one" in both paragraphs - and the second one closed
                            // by apologising for prose that is not written, which is a note to
                            // the authors, not a reading for the couple.
                            //
                            // A natal reading written for the actual PAIR is different and is
                            // kept: the geometry it describes is real, and the frame in front
                            // of it supplies the one thing it cannot know.
                            if (frame != null && aspectText != null
                                    && aspectText.contains("is not written yet.")) {
                                aspectText = frame;
                                frame = null;
                            }
                        }
                    }
                    if (!aspectText.startsWith("Interpretation not found") && !aspectText.startsWith("General ")) {
                        sb.append("<li><b>").append(h.type.label).append(" to ").append(other)
                            .append(":</b> ");
                        if (frame != null) {
                            sb.append(frame).append("<br><br>");
                        }
                        sb.append(aspectText).append("</li>");
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
        if (relationship) {
            sb.append("<p><i>A profection year and a solar return are keyed to a birthday - an age in years, and the Sun's return to its natal degree. A composite has no birthday: its moment is the midpoint of two births, so an &quot;age&quot; here would be the average of the partners' ages rather than the age of the relationship. Transits to the composite are shown below instead, which is the standard timing technique for a relationship chart.</i></p>");
        } else if (withTime && prof != null) {
            sb.append("<p>You are currently in a <b>House ").append(prof.house).append(" Profection (").append(prof.sign).append(")</b> at age ").append(prof.age).append(". ");
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
            // <b>A named pattern with no reading is not a mention.</b> Until 2026-08-31
            // this printed one bare line per pattern - "T-square involving Chiron, Uranus,
            // Venus (Apex: Venus)" - and stopped. pattern_detail.json has carried prose for
            // all nine patterns the engine can emit, plus MODALITY-specific entries
            // (tsquare_fixed, grandtrine_water, grandcross_cardinal), and the synthesis
            // fetched none of it. A T-square is the loudest thing in a chart and it was
            // reading as absent.
            for (AspectPatterns.Pattern p : patterns) {
                sb.append("<h3 style='color:#FFD166;'>").append(p.name);
                if (p.apex != null && !p.apex.isEmpty()) {
                    sb.append(" &mdash; apex <b>").append(p.apex).append("</b>");
                }
                sb.append("</h3>");
                sb.append("<p><b>Involving:</b> ").append(String.join(", ", p.bodies)).append("</p>");

                String slug = p.name.toLowerCase().replace("-", "").replace(" ", "");
                String general = svc.getMacroDynamic("pattern_" + slug);
                if (general != null && !general.isEmpty()) {
                    sb.append("<p>").append(general).append("</p>");
                }
                // The modality entry is the specific one - a fixed T-square is a different
                // animal from a cardinal one - so it follows the general reading.
                if (p.modality != null && !p.modality.isEmpty()) {
                    String byMode = svc.getMacroDynamic(slug + "_" + p.modality.toLowerCase());
                    if (byMode != null && !byMode.isEmpty()) {
                        sb.append("<p><b>As a ").append(p.modality).append(" ").append(p.name)
                          .append(":</b> ").append(byMode).append("</p>");
                    }
                }
            }
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
            sb.append("<li><b>Ascendant (").append(ascDeg).append("° ").append(ascSign).append("):</b> ").append(sabian(svc, ascSign, ascDeg, relationship)).append("</li>");
            
            double dsc = (f.asc + 180) % 360;
            String dscSign = Zodiac.signName(dsc);
            int dscDeg = (int) Math.floor(dsc % 30) + 1;
            sb.append("<li><b>Descendant (").append(dscDeg).append("° ").append(dscSign).append("):</b> ").append(sabian(svc, dscSign, dscDeg, relationship)).append("</li>");
        }
        
        if (f.mc != 0.0) {
            String mcSign = Zodiac.signName(f.mc);
            int mcDeg = (int) Math.floor(f.mc % 30) + 1;
            sb.append("<li><b>Midheaven (").append(mcDeg).append("° ").append(mcSign).append("):</b> ").append(sabian(svc, mcSign, mcDeg, relationship)).append("</li>");
            
            double ic = (f.mc + 180) % 360;
            String icSign = Zodiac.signName(ic);
            int icDeg = (int) Math.floor(ic % 30) + 1;
            sb.append("<li><b>IC (").append(icDeg).append("° ").append(icSign).append("):</b> ").append(sabian(svc, icSign, icDeg, relationship)).append("</li>");
        }
        sb.append("</ul>");

        // 9. Specific Chronological Triggers
        sb.append("<h2 style='color: #FFD700;'>9. Specific Chronological Triggers</h2>");
        // Convergence is assembled from the profection year scan and the solar return, so it
        // inherits the birthday problem described on section 4 and is suppressed with it.
        if (relationship) {
            sb.append("<p><i>Convergence is scanned across a profection year, which a composite does not have. See the note above.</i></p>");
        } else if (withTime && scan != null && convergence != null) {
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
                    String tAsp = relationship
                        ? svc.getCompositeTransitAspect(h.transiting, e.getKey(), h.type.label)
                        : null;
                    sb.append(tAsp != null ? tAsp
                        : svc.getTransitAspect(h.transiting, e.getKey(), h.type.label));
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