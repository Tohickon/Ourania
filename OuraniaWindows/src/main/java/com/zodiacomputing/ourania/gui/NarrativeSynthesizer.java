package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AspectPatterns;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Convergence;
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Progressions;
import com.zodiacomputing.ourania.astro.Sect;
import com.zodiacomputing.ourania.astro.ThemeConvergence;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import de.thmac.swisseph.SweDate;
import com.zodiacomputing.ourania.astro.TransferOfLight;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.astro.YearScan;

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
    /** A sign index as its name, capitalised. Out of range reads as unknown rather than throwing. */
    static String signName(int sign) {
        if (sign < 0 || sign >= Zodiac.SIGNS.length) {
            return "unknown";
        }
        String s = Zodiac.SIGNS[sign];
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** " until 3 October", or nothing at all when the period has no end recorded. */
    static String until(double jd) {
        if (Double.isNaN(jd)) {
            return "";
        }
        SweDate d = new SweDate(jd);
        return String.format(" until %d %s", d.getDay(), MONTHS[Math.max(1,
            Math.min(12, d.getMonth())) - 1]);
    }

    private static final String[] MONTHS = {"January", "February", "March", "April", "May",
        "June", "July", "August", "September", "October", "November", "December"};

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
        // The whole reading, which is what every caller before the mechanics toggle existed
        // got and must keep getting. Only the reader's own choice can withhold a tier, so only
        // the surface that has a reader passes it.
        return generateReport(f, g, ranked, t, tf, prof, hits, scan, convergence, withTime,
            relationship, true);
    }

    /**
     * The same, with tier 4 - the mechanics - made optional.
     *
     * <b>The four tiers are progressive disclosure, and a Swing HTML pane cannot disclose
     * progressively.</b> Its renderer is HTML 3.2: there is no {@code <details>} element to
     * collapse the audit trail behind and no script to collapse it with. So the tier is chosen
     * before the page is built rather than folded up after, and what would have been a
     * disclosure triangle is a checkbox on the Synthesize page.
     *
     * <b>Tiers 1 to 3 are unaffected by this flag.</b> Whichever way it is set, the summary,
     * the themes and the dates are computed from the same scan and say the same thing - the
     * reader is choosing whether to be handed the working, not which answer they get.
     *
     * @param mechanics whether to print tier 4; false stops after the dates and says where the
     *                  rest went
     */
    public static String generateReport(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked, Themes.Result t, ChartFrame tf, Profection prof, List<Transits.Hit> hits, YearScan scan, List<Convergence.Target> convergence, boolean withTime, boolean relationship, boolean mechanics) {
        // <b>Nothing selected is a state the reader can reach, and it has to be said, not thrown.</b>
        // Settings has a None for every section, and with every body off the ranking is empty:
        // the lead body below was ranked.get(0), so Synthesize threw IndexOutOfBounds, and
        // showReading only prints a stack trace - the button simply did nothing. Found 2026-09-19
        // when AspectGridCheck, isolated from David's settings by J14, left the selection empty.
        if (ranked == null || ranked.isEmpty()) {
            return "<html><body style='font-family: sans-serif; font-size: 14px; margin: 15px;"
                + " color: #E0E0E0; background-color: #000000;'>"
                + "<h1 style='color: #FFFFFF;'>Chart Synthesis</h1>"
                + "<p>There is nothing to synthesize: no planets or points are selected. Turn some"
                + " on under Settings, Chart Points, and synthesize again.</p>"
                + "</body></html>";
        }

        StringBuilder sb = new StringBuilder();
        InterpretationService svc = InterpretationService.getInstance();
        boolean isDiurnal = g.diurnal;

        sb.append("<html><body style='font-family: sans-serif; font-size: 14px; margin: 15px; color: #E0E0E0; background-color: #000000;'>");
        sb.append("<h1 style='color: #FFFFFF; border-bottom: 1px solid #444; padding-bottom: 5px;'>Chart Synthesis</h1>");
        
        // <b>The facts the summary and the mechanics both state, worked out once, here.</b>
        // Tier 1 below is a precis of things stated in full further down, and every line of it
        // reads the same variable its detailed section reads. That is what keeps it a second
        // VIEW of one fact rather than a second computation of it - one rule in two places,
        // drifting apart, is the defect this project logs more than any other.
        String sectName = isDiurnal ? "diurnal (day)" : "nocturnal (night)";
        // The enum constant is BOWL, and "shaped as a BOWL" reads as shouting. Named here once
        // so the summary and 4.1 cannot end up calling the same shape two different things.
        String shapeName = g.shape != null
            ? g.shape.name().charAt(0) + g.shape.name().substring(1).toLowerCase()
            : "Standard";
        String leadBody = (g.shapeHandle != null && !g.shapeHandle.isEmpty()) ? g.shapeHandle : ranked.get(0).body;
        ChartFrame.Body leadPlanet = f.body(leadBody);
        String leadSign = leadPlanet != null ? Zodiac.signName(leadPlanet.lon) : "";

        // The year's themes, read by tiers 1, 2 and 3. Computed here rather than where they
        // are printed because three tiers now show different faces of one list, and computing
        // it per tier would let the summary and the theme cards disagree about what reached
        // three testimonies. Conditions are the ones section 4.9 used to carry: convergence is
        // assembled from the profection year scan and the solar return, so a relationship
        // chart - which has no birthday - gets none of it.
        List<ThemeConvergence.Result> yearThemes = null;
        if (!relationship && withTime && scan != null && convergence != null) {
            yearThemes = ThemeConvergence.themes(f, prof, convergence, scan.moonClock,
                scan.catalysts);
        }
        // K12 stage 2. The mid-term clock reads the same whether or not any theme reaches
        // three, so it is stated whatever the themes do - and stated once, in the summary,
        // because it is a headline-level fact and not a piece of machinery.
        String clock = null;
        if (!relationship && scan != null && scan.moonClock != null && !scan.moonClock.isEmpty()) {
            StringBuilder cb = new StringBuilder();
            for (Progressions.Tenancy ten : scan.moonClock) {
                cb.append(cb.length() == 0 ? "" : "; ").append(ten);
                if (ten.phase != null && !ten.phase.isEmpty()) {
                    cb.append(", lunation ").append(ten.phase);
                }
            }
            clock = cb.toString();
        }

        // ------------------------------------------------------------------ Tier 1
        //
        // <b>The reading used to open on its own machinery and close on its findings.</b> The
        // sect was section 1, the profection year section 4, and the convergence rankings - the
        // one part that answers "what is this year about" - sat at the very bottom, behind two
        // hundred lines of individual triggers. Nothing was missing from the page; the order of
        // it was upside down, and a reader met the engine before they met the reading.
        sb.append(tier("1. At a glance"));
        sb.append("<ul>");
        sb.append("<li><b>The chart</b> &mdash; ").append(sectName).append(", shaped as a <b>")
            .append(shapeName).append("</b> led by <b>").append(leadBody);
        if (leadSign != null && !leadSign.isEmpty()) {
            sb.append(" in ").append(Character.toUpperCase(leadSign.charAt(0)))
                .append(leadSign.substring(1));
        }
        sb.append("</b>");
        if (!g.dominantElements.isEmpty()) {
            sb.append("; dominant element <b>").append(String.join(", ", g.dominantElements))
                .append("</b>");
        }
        sb.append(".</li>");
        if (!relationship && prof != null) {
            sb.append("<li><b>The year</b> &mdash; age ").append(prof.age)
                .append(", profected to the <b>").append(prof.house).append("th house</b> (")
                .append(signName(prof.sign)).append("), lord of the year <b>")
                .append(prof.lord).append("</b>.</li>");
        }
        if (clock != null) {
            sb.append("<li><b>The mid-term clock</b> &mdash; ").append(clock).append(".</li>");
        }
        // The most activated points, which is what the intensity rankings say when they are
        // read as an answer rather than as a list. Three, because a summary that names eight
        // points has not summarised anything; the whole ranking is still in 4.9.
        if (convergence != null && !convergence.isEmpty()) {
            List<Convergence.Target> top = new ArrayList<>(convergence);
            top.sort((x, y) -> Double.compare(y.score, x.score));
            StringBuilder points = new StringBuilder();
            int shown = 0;
            for (Convergence.Target target : top) {
                if (target.score <= 0.0 || shown >= SPARK.length) {
                    continue;
                }
                points.append(shown == 0 ? "" : ", ")
                    .append("<span style='color:").append(SPARK[shown]).append(";'>&#9679;</span> <b>")
                    .append(target.natal).append("</b> ")
                    .append(String.format("%.1f%%", target.score * 100));
                shown++;
            }
            if (shown > 0) {
                sb.append("<li><b>Most activated points</b> &mdash; ").append(points)
                    .append(".</li>");
            }
        }
        if (yearThemes != null) {
            StringBuilder heads = new StringBuilder();
            for (ThemeConvergence.Result th : yearThemes) {
                if (th.headline()) {
                    heads.append(heads.length() == 0 ? "" : ", ").append("<b>")
                        .append(th.theme.label).append("</b> (").append(th.families.size())
                        .append(")");
                }
            }
            sb.append("<li><b>The year's themes</b> &mdash; ")
                .append(heads.length() == 0
                    ? "none reaches three independent testimonies"
                    : heads.toString())
                .append(".</li>");
        }
        sb.append("</ul>");

        // ------------------------------------------------------------------ Tier 2
        sb.append(tier("2. The year by theme (the Rule of Three)"));
        if (relationship) {
            sb.append("<p><i>Convergence is scanned across a profection year, which a composite"
                + " does not have. See 4.4 below.</i></p>");
        } else if (yearThemes == null) {
            sb.append("<p><i>Transit data not enabled. Enable transits to scan the year by"
                + " theme.</i></p>");
        } else {
            sb.append("<p style='color:#AAAAAA;'>A theme is a headline only where three"
                + " <i>independent</i> techniques name it; the rest are background trends, and"
                + " are listed as such rather than written up. One line per technique, because"
                + " a technique agreeing with itself is one witness however many contacts it"
                + " makes &mdash; <i>+n more</i> is how many of those there were, and every one"
                + " of them is listed in full under 4.9.</p>");
            boolean anyHeadline = false;
            for (ThemeConvergence.Result th : yearThemes) {
                if (!th.headline()) {
                    continue;
                }
                anyHeadline = true;
                sb.append("<h3 style='color:#FFD166;'>").append(th.theme.label)
                    .append(" <span style='color:#888; font-weight:normal;'>&middot; ")
                    .append(th.families.size()).append(" independent testimonies</span></h3>");
                sb.append(testimonies(th));
            }
            if (!anyHeadline) {
                sb.append("<p>No theme reaches three independent testimonies this year.</p>");
            }
            StringBuilder trends = new StringBuilder();
            for (ThemeConvergence.Result th : yearThemes) {
                if (!th.headline() && !th.families.isEmpty()) {
                    trends.append(trends.length() == 0 ? "" : "; ").append(th.theme.label)
                        .append(" (").append(th.families.size()).append(")");
                }
            }
            if (trends.length() > 0) {
                sb.append("<p style='color:#AAAAAA;'><i>Background trends:</i> ").append(trends)
                    .append("</p>");
            }
        }

        // ------------------------------------------------------------------ Tier 3
        //
        // <b>One timeline, not one per theme.</b> The dates were printed inside each theme's
        // card, so a reader who wanted to know what was coming in October had to read three
        // lists and merge them in their head - and a fortnight where two themes peak together
        // looked like two unrelated entries. Merging them is also the honest shape: the Sun and
        // Mars do not visit one theme at a time.
        sb.append(tier("3. Dates to watch"));
        if (yearThemes == null) {
            sb.append("<p><i>").append(relationship
                    ? "Dating follows the year scan, which a composite does not have."
                    : "Transit data not enabled. Enable transits to date the year.")
                .append("</i></p>");
        } else {
            List<Window> windows = windows(yearThemes);
            if (windows.isEmpty()) {
                sb.append("<p>No headline theme has a gathering of catalysts worth dating this"
                    + " year.</p>");
            } else {
                sb.append("<p style='color:#AAAAAA;'>The year's fullest gatherings of Sun and"
                    + " Mars on a headline theme's own points. The Sun reaches every point"
                    + " monthly, so these are the strongest of a monthly beat rather than rare"
                    + " events: they mark the day without making the case.</p><ul>");
                for (Window w : windows) {
                    String span = dayOf(w.from).equals(dayOf(w.to))
                        ? dayOf(w.from)
                        : dayOf(w.from) + " to " + dayOf(w.to);
                    sb.append("<li><b>").append(span).append("</b> &mdash; <i>")
                        .append(String.join(", ", w.themes)).append("</i><br>")
                        .append("<span style='color:#AAAAAA;'>")
                        .append(String.join(", ", w.hits)).append("</span></li>");
                }
                sb.append("</ul>");
            }
        }

        // ------------------------------------------------------------------ Tier 4
        //
        // <b>Named even when it is withheld.</b> A reading that simply ended after the dates
        // would look like a reading that had run out, and a reader who had forgotten the
        // checkbox would have no way to tell the difference.
        if (!mechanics) {
            sb.append(tier("4. The mechanics"));
            sb.append("<p style='color:#AAAAAA;'><i>Not shown. The architecture, every"
                + " placement, the patterns, the houses, the full convergence rankings and the"
                + " current transits are what the three tiers above are drawn from &mdash; tick"
                + " <b>Show the mechanics</b> at the top of this page to read them.</i></p>");
            sb.append("</body></html>");
            return sb.toString();
        }
        sb.append(tier("4. The mechanics"));
        sb.append("<p style='font-style: italic; color: #AAAAAA;'>Everything the three tiers"
            + " above are drawn from, in the engine's own order: the chart's architecture, every"
            + " placement ranked, the patterns, the houses, the full convergence rankings and"
            + " the current transits. Nothing here is a summary &mdash; this is the audit"
            + " trail.</p>");

        // 4.1 Core Architecture
        sb.append("<h2 style='color: #FFD700;'>4.1 Core Architecture: The Chart's Signature</h2>");
        sb.append("<p>This is a <b>").append(sectName).append("</b> chart, meaning the <b>").append(g.sectLight).append("</b> is the master of the sect, <b>").append(Sect.beneficOfSect(isDiurnal)).append("</b> is " + (relationship ? "the relationship's" : "your") + " greatest ally (the benefic of sect), and <b>").append(g.outOfSectMalefic).append("</b> is " + (relationship ? "its" : "your") + " primary friction point (the malefic contrary to sect).</p>");

        sb.append("<p>The chart is shaped as a <b>").append(shapeName).append("</b>, led by <b>").append(leadBody).append(" in ").append(leadSign).append("</b>. This mechanical shape implies a personality that is highly driven and structured by this leading force.</p>");

        if (!g.dominantElements.isEmpty()) {
            sb.append("<p>The dominant element is <b>").append(String.join(", ", g.dominantElements)).append("</b>, emphasizing that baseline operating system.</p>");
        }

        // 4.2 Planetary Placements
        sb.append("<h2 style='color: #FFD700;'>4.2 Planetary Placements (Ranked by Prominence)</h2>");
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
                // <b>Both ends minor means there is no actor to express the contact</b>, so it
                // is listed as background rather than under a heading that calls it major. The
                // K4 decision asked for these sixty asteroid-to-asteroid cells to be left
                // unwritten; measured 2026-09-03 all sixty are written, so the prose stays and
                // the claim above it is the thing that changes. A Pallas square Juno that
                // arrives titled "Major Contacts" is the app asserting something false about
                // its own weight - which is precisely Tompkins' clutter and Cunningham's
                // alarm fatigue, reached through wording rather than through line-drawing.
                StringBuilder major = new StringBuilder();
                StringBuilder background = new StringBuilder();
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
                    // Mutual: both bodies belong to the same person, so either end can
                    // carry the contact. Same rule the returns table asks directionally.
                    StringBuilder into = Bodies.hasPrimaryActor(v.body, other, false)
                        ? major : background;
                    if (!aspectText.startsWith("Interpretation not found") && !aspectText.startsWith("General ")) {
                        into.append("<li><b>").append(h.type.label).append(" to ").append(other)
                            .append(":</b> ");
                        if (frame != null) {
                            into.append(frame).append("<br><br>");
                        }
                        into.append(aspectText).append("</li>");
                    } else {
                        into.append("<li><b>").append(h.type.label).append(" to ").append(other).append("</b> (").append(String.format("%.1f&deg;", h.offBy)).append(").</li>");
                    }
                }
                if (major.length() > 0) {
                    sb.append("<p><b>Major Contacts:</b></p><ul>").append(major).append("</ul>");
                }
                if (background.length() > 0) {
                    sb.append("<p><b>Background Resonance:</b> <span style='font-size:11px;")
                      .append(" color:#B9B2D6;'>minor bodies at both ends - texture beneath the")
                      .append(" planetary signatures, not beside them.</span></p><ul>")
                      .append(background).append("</ul>");
                }
            }
            sb.append("</div>");
        }

        // 4.3 Structural Tensions
        sb.append("<h2 style='color: #FFD700;'>4.3 Structural Tensions (The Unresolved Vectors)</h2>");
        sb.append("<ul>");
        for (Themes.Contradiction contradiction : t.contradictions) {
            sb.append("<li><b>Clash:</b> ").append(contradiction.themeA).append(" vs ").append(contradiction.themeB).append("</li>");
        }
        // <b>Read the keys the engine writes, not the ones that read well.</b> Zodiac.ELEMENTS
        // is lowercase - fire, earth, air, water - and this asked for "Fire", "Water", "Air",
        // "Earth". Four lookups, four misses, and getOrDefault turned every one into 0.0, so
        // the section reported a chart with no elements at all while the paragraph above it
        // correctly named air as dominant. A get() would have shown null and been noticed the
        // first time anyone read the page; the default is what made it survive.
        sb.append("<li>Elemental Distribution:");
        for (int i = 0; i < Gestalt.ELEMENTS.length; i++) {
            String key = Gestalt.ELEMENTS[i];
            sb.append(i == 0 ? " " : ", ")
              .append(Character.toUpperCase(key.charAt(0))).append(key.substring(1))
              .append(' ').append(String.format("%.1f", g.elements.getOrDefault(key, 0.0)));
        }
        sb.append(".</li>");
        sb.append("</ul>");

        // 4.4 Current Chronometry
        sb.append("<h2 style='color: #FFD700;'>4.4 Current Chronometry</h2>");
        if (relationship) {
            sb.append("<p><i>A profection year and a solar return are keyed to a birthday - an age in years, and the Sun's return to its natal degree. A composite has no birthday: its moment is the midpoint of two births, so an &quot;age&quot; here would be the average of the partners' ages rather than the age of the relationship. Transits to the composite are shown below instead, which is the standard timing technique for a relationship chart.</i></p>");
        } else if (prof != null) {
            // <b>The sign, not its index.</b> This printed prof.sign, which is an int, so the
            // line read "House 5 Profection (4)" - a number where a sign belongs. Profection's
            // own toString had always named it properly; this was the one place that did not.
            sb.append("<p>You are currently in a <b>House ").append(prof.house)
                .append(" Profection (").append(signName(prof.sign)).append(")</b> at age ")
                .append(prof.age).append(". ");
            sb.append("The structural focus of your year shifts to the ").append(prof.house)
                .append("th House, ruled by <b>").append(prof.lord).append("</b>. ");
            sb.append("As the 'Lord of the Year', themes surrounding this planet are paramount.</p>");

            // <b>Master list F6.</b> The engine has computed the monthly and daily profections
            // for as long as the annual one, and nothing has ever shown them - which is the
            // whole of what that item is. The year divides into twelve and each month again, so
            // these are the same technique at two shorter scales: the month names where the
            // year's theme is being worked out now, and the day is the finest the method goes.
            if (prof.monthlyLord != null && prof.dailyLord != null) {
                sb.append("<p>Within that year, the <b>month</b> profects to <b>")
                    .append(signName(prof.monthlySign)).append("</b>, ruled by <b>")
                    .append(prof.monthlyLord).append("</b>")
                    .append(until(prof.monthlyUntil))
                    .append("; and the <b>day</b> to <b>").append(signName(prof.dailySign))
                    .append("</b>, ruled by <b>").append(prof.dailyLord).append("</b>")
                    .append(until(prof.dailyUntil)).append(".</p>");
                sb.append("<p><i>The year divides into twelve and each month into twelve again, "
                    + "against this chart's own solar return year rather than a round number - "
                    + "so a profected month here is about ")
                    .append(String.format("%.1f", prof.monthLength))
                    .append(" days and a profected day about ")
                    .append(String.format("%.1f", prof.monthLength / 12.0))
                    .append(".</i></p>");
            }
        } else {
            // <b>Names what is actually absent.</b> This said "Transit data not enabled",
            // which was never the condition it tested and is not the condition now: a profection
            // wants a birth moment to count an age from, and transits have nothing to do with
            // it. The two sections further down do need transits and say so on their own.
            sb.append("<p><i>No birth moment to count a profection from, so there is no age and no Lord of the Year.</i></p>");
        }

        // 4.5 Complex Geometric Circuitry
        sb.append("<h2 style='color: #FFD700;'>4.5 Complex Geometric Circuitry (Aspect Patterns)</h2>");
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
        // Named for what it lists. Translation and collection of light are classical
        // techniques about any two planets and a third that carries between them - the Moon
        // has no special part in either, and the heading "The Lunar Engine" promised one that
        // never appeared: on a real chart this section listed the Sun, Venus, Mars, Jupiter,
        // Uranus, Neptune, Pluto and Saturn, and not the Moon once.
        sb.append("<h2 style='color: #FFD700;'>4.6 Translation and Collection of Light</h2>");
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

        // 4.7 House Mechanics
        sb.append("<h2 style='color: #FFD700;'>4.7 House Mechanics: Competence vs. Chaos</h2>");
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
        // <b>Say what happened to the rest.</b> A house whose witnesses neither agree nor
        // disagree lands in neither list above, so a reader saw nine houses named under a
        // heading promising a partition of twelve and could not tell an unremarkable house
        // from a house the code had lost.
        List<Integer> middling = new ArrayList<>();
        for (int h = 1; h <= 12; h++) {
            if (!strongHouses.contains(h) && !weakHouses.contains(h)) {
                middling.add(h);
            }
        }
        if (!middling.isEmpty()) {
            sb.append("<p><b>Unremarkable:</b> Houses ")
              .append(middling.stream().map(String::valueOf)
                  .collect(Collectors.joining(", ")))
              .append(". The witnesses here neither agree nor contradict, which is a finding "
                  + "rather than a gap - most charts have several.</p>");
        }

        // 4.8 Angles and Sabian Archetypes
        sb.append("<h2 style='color: #FFD700;'>4.8 The Angles and their Sabian Archetypes</h2>");
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

        // 4.9 Convergence Intensity Rankings
        //
        // <b>What this section is, once the themes have been lifted out of it.</b> It ranked
        // the natal points by how much of the year lands on them, and it answered a real
        // question - which of my points is busy - underneath the one a reader asks first, which
        // is what the year is about. That answer is tier 1 and tier 2 now; the ranking stays
        // here, whole, as the working it was read off.
        sb.append("<h2 style='color: #FFD700;'>4.9 Convergence Intensity Rankings</h2>");
        // Convergence is assembled from the profection year scan and the solar return, so it
        // inherits the birthday problem described on 4.4 and is suppressed with it.
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

        // 4.10 Current Transits
        sb.append("<h2 style='color: #FFD700;'>4.10 Current Transits</h2>");
        if (withTime && hits != null && !hits.isEmpty()) {
            // One contact per axis. A body conjunct the MC is opposite the IC at the same orb
            // in the same instant, and the page was reporting both as though they were two
            // things to think about.
            java.util.Map<String, List<Transits.Hit>> byTarget = new java.util.LinkedHashMap<>();
            for (Transits.Hit h : Transits.collapseAxisMirrors(hits)) {
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

    /**
     * A Julian day as a calendar date, for the dating line.
     *
     * The day and not the hour: the fast bodies date an event to about a day, and printing a
     * time would claim a precision the technique does not have - the perfection is exact to the
     * minute, the thing it is supposed to mark is not.
     */
    private static String dayOf(double jd) {
        de.thmac.swisseph.SweDate d = new de.thmac.swisseph.SweDate();
        d.setJulDay(jd);
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonth(), d.getDay());
    }

    /**
     * A tier heading.
     *
     * <b>Swing's HTML renderer styles h1, h2 and h3 and stops there</b>, and the mechanics'
     * own section headings already use h2 - so a tier cannot simply be an h4 and be seen. It
     * is an h2 with the weight put back by hand: white, larger, and underlined, against the
     * gold h2s of the sections beneath it. Written once here because four headings that must
     * look alike, spelled out at four call sites, is the shape of thing that drifts.
     */
    private static String tier(String title) {
        // <b>The class is for the phone, which cannot see the style.</b> Android's
        // Html.fromHtml takes no stylesheet and no class, and the phone's page follows the
        // device's light or dark setting - so PhoneReading.forPhone strips every colour, white
        // headings on a light page being invisible. That left the four tiers rendering
        // identically to the ten mechanics sections beneath them, which is the whole hierarchy
        // gone. The class is what lets the phone find a tier heading and promote it to an h1,
        // so the levels read apart by SIZE rather than by colour. Marked here rather than
        // matched by its text on the phone: a heading's wording is not an interface.
        return "<h2 class='tier' style='color:#FFFFFF; font-size:19px; margin-top:26px;"
            + " border-bottom:1px solid #444; padding-bottom:3px;'>" + title + "</h2>";
    }

    /** The colours of the top activated points, strongest first. */
    private static final String[] SPARK = {"#FF6B6B", "#FFA94D", "#FFD43B"};

    /**
     * A theme's testimonies, one line per technique that agreed.
     *
     * <p><b>The list was one line per witness, and a witness is not a technique.</b> Nine
     * transits to the Moon are one testimony by the Rule of Three's own definition - "two
     * transits to the same point are one witness, not two" - and all nine were printed, each
     * ending "(same technique, counted once)". A theme headlined as seven testimonies arrived
     * as thirty-one lines, twenty-four of which said they did not count. A reader cannot hold
     * that, and the shape of it argues against the very rule the section exists to apply.
     *
     * <p>So one line per family - the first, which is the one that counted - and the echoes
     * behind it as a number. <b>Nothing is dropped:</b> every contact is still listed verbatim
     * under 4.9, witness by witness, which is where somebody checking the count goes.
     */
    private static String testimonies(ThemeConvergence.Result th) {
        java.util.LinkedHashMap<String, List<String>> byFamily = new java.util.LinkedHashMap<>();
        int background = 0;
        for (String line : th.testimonies) {
            // The engine marks a contact on a point the year has not woken as background and
            // does not count it, so it is not a testimony and does not get a line of its own.
            if (line.contains("(to a point not active this year")) {
                background++;
                continue;
            }
            int colon = line.indexOf(": ");
            String head = colon > 0 ? line.substring(0, colon) : line;
            byFamily.computeIfAbsent(head, k -> new ArrayList<>()).add(line);
        }
        StringBuilder sb = new StringBuilder("<ul>");
        for (java.util.Map.Entry<String, List<String>> e : byFamily.entrySet()) {
            List<String> lines = e.getValue();
            sb.append("<li>").append(testimony(lines.get(0)));
            // The rule is stated once, above the themes. Repeating it on forty lines would be
            // the same crowding this method exists to undo.
            if (lines.size() > 1) {
                sb.append("<span style='color:#888;'> &middot; +").append(lines.size() - 1)
                    .append(" more</span>");
            }
            sb.append("</li>");
        }
        if (background > 0) {
            sb.append("<li style='color:#888;'>").append(background)
                .append(background == 1 ? " further contact lands" : " further contacts land")
                .append(" on a point the year has not woken, so ")
                .append(background == 1 ? "it is" : "they are")
                .append(" background and not counted.</li>");
        }
        return sb.append("</ul>").toString();
    }

    /**
     * A testimony line in the reading's voice.
     *
     * <p><b>{@link ThemeConvergence} writes its testimonies for the audit, and it is right
     * to.</b> The family is its enum constant in capitals, and a repeat carries "(same
     * technique, counted once)" so that the headline count can be checked against the list it
     * came from. Both of those are exactly what a check suite wants and neither is English, so
     * the wording is changed here, on the page, and the engine keeps its record unaltered -
     * which is also why this is a rewrite of the line rather than a second set of strings in
     * ThemeConvergence.
     */
    private static String testimony(String line) {
        String s = line;
        int colon = s.indexOf(": ");
        if (colon > 0) {
            String head = s.substring(0, colon);
            if (head.equals(head.toUpperCase()) && head.indexOf(' ') < 0) {
                s = "<b>" + familyLabel(head) + "</b> &mdash; " + s.substring(colon + 2);
            }
        }
        return s
            .replace(" (same technique, counted once)",
                "<span style='color:#888;'> (this technique again, counted once)</span>")
            .replace(" (to a point not active this year: background, not counted)",
                "<span style='color:#888;'> (background &mdash; the year has not woken this"
                + " point, so it does not count as a testimony)</span>");
    }

    /** A technique family as a reader's name for it rather than as its enum constant. */
    private static String familyLabel(String family) {
        switch (family) {
            case "PROFECTION": return "Profection";
            case "TRANSIT": return "Transit";
            case "ECLIPSE": return "Eclipse";
            case "STATION": return "Station";
            case "SOLAR_ARC": return "Solar arc";
            case "PROGRESSION": return "Progression";
            case "PROGRESSED_MOON": return "Progressed Moon";
            case "RETURN": return "Return";
            // A family added to Convergence.Family and not to this switch reads as its own
            // constant - odd-looking, and never wrong about which technique spoke.
            default: return family;
        }
    }

    /** One occasion on the timeline: the themes peaking in it, and the touches that make it up. */
    private static final class Window {
        double from;
        double to;
        final java.util.LinkedHashSet<String> themes = new java.util.LinkedHashSet<>();
        final List<String> hits = new ArrayList<>();
    }

    /**
     * Every headline theme's strongest peaks, merged into one timeline.
     *
     * <p><b>Peaks were printed inside the theme that owned them, and a year does not arrive
     * theme by theme.</b> A fortnight where career and health peak together is one occasion in
     * a reader's life and was two entries in two lists here. So the peaks are pooled, sorted by
     * date, and any two falling within {@link ThemeConvergence#PEAK_SPAN_DAYS} of each other
     * become one window naming both themes - the same span the engine uses to decide that two
     * catalyst touches are one occasion, applied once more, one level up.
     *
     * <p>Four per theme, as before: {@code strongest(4)} is where the choice of which peaks
     * are worth naming is made, and merging must not quietly widen it.
     */
    private static List<Window> windows(List<ThemeConvergence.Result> themes) {
        List<ThemeConvergence.Peak> peaks = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        for (ThemeConvergence.Result th : themes) {
            if (!th.headline()) {
                continue;
            }
            for (ThemeConvergence.Peak p : th.strongest(4)) {
                peaks.add(p);
                owners.add(th.theme.label);
            }
        }
        Integer[] order = new Integer[peaks.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order,
            java.util.Comparator.comparingDouble(i -> peaks.get(i).from()));

        List<Window> out = new ArrayList<>();
        for (int i : order) {
            ThemeConvergence.Peak p = peaks.get(i);
            Window last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last == null || p.from() > last.to + ThemeConvergence.PEAK_SPAN_DAYS) {
                last = new Window();
                last.from = p.from();
                last.to = p.to();
                out.add(last);
            } else {
                last.to = Math.max(last.to, p.to());
            }
            last.themes.add(owners.get(i));
            for (ThemeConvergence.Dated d : p.hits) {
                String s = d.toString();
                // The same catalyst touch belongs to every theme whose points it lands on, so
                // a merged window would otherwise print "Sun conjunct Moon" twice for a health
                // and a home peak that share the Moon.
                if (!last.hits.contains(s)) {
                    last.hits.add(s);
                }
            }
        }
        return out;
    }
}