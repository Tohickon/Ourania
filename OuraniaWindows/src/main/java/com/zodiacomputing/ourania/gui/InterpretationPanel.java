package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.*;
import javax.swing.*;
import java.awt.*;
import java.time.ZonedDateTime;

public class InterpretationPanel extends JPanel {

    private SkymapPanel skymapPanel;
    private OuraniaWindow window;
    private JEditorPane editorPane;
    private JScrollPane scrollPane;
    private SwissEph sw;

    public JEditorPane getEditorPane() {
        return editorPane;
    }

    /** True while showing the report tier, whose &lt;pre&gt; must be allowed to overflow. */
    private boolean preformatted;

    /** Width for flowing interpretations, and the wider one the report tables need. */
    private static final int FLOW_WIDTH = 350;
    private static final int REPORT_WIDTH = 560;

    /**
     * Single render point, so the preformatted flag can never be left set from a previous
     * view. Ten call sites setting the text by hand is exactly how one of them ends up
     * showing a report layout to a Sabian symbol.
     */
    /**
     * Every reading passes through here, so the house style is applied here.
     *
     * <b>Twenty-eight callers, one seam.</b> Snapshot, Report, Synthesize, Predict and the
     * per-placement panels all end at setHtml, so type scale and term colouring go in once
     * rather than being threaded through five generators that would drift apart. That is the
     * same argument Prose makes at the top of its own file.
     *
     * <b>Preformatted output is left alone.</b> The report pane is monospaced, column-aligned
     * text; injecting a stylesheet or anchors into it would break the alignment it depends on.
     */
    /** Whatever reading is on screen, as its own markup, for export. */
    public String currentHtml() {
        return editorPane == null ? "" : editorPane.getText();
    }

    /**
     * Runs a show* method and returns the page it built, leaving this panel's own pane alone.
     *
     * <b>So a clicked reading can open on the Selection page without a second generator.</b>
     * Every detail view ends at setHtml; while a capture is open, setHtml keeps the styled page
     * for the caller instead of rendering it. Null when the method built no page. Nested
     * captures each keep their own.
     */
    String capture(Runnable show) {
        String[] slot = {null};
        String[] outer = capturing;
        capturing = slot;
        try {
            show.run();
        } finally {
            capturing = outer;
        }
        return slot[0];
    }

    /** The open capture's slot, or null when pages render here as usual. */
    private String[] capturing;

    private void setHtml(String html, boolean pre) {
        if (capturing != null) {
            capturing[0] = pre ? html : style(Prose.decorate(html));
            return;
        }
        preformatted = pre;
        setPreferredSize(new Dimension(pre ? REPORT_WIDTH : FLOW_WIDTH, 0));
        editorPane.setText(pre ? html : style(Prose.decorate(html)));
        editorPane.setCaretPosition(0);
        revalidate();
        if (getParent() != null) {
            getParent().revalidate();
        }
    }


    /**
     * The reading type scale, injected once per document.
     *
     * <b>Headings were barely distinguishable from body text.</b> "Sun in Virgo" rendered at
     * almost the same weight and size as the paragraph under it, so a 240,000-character
     * synthesis read as one undifferentiated wall. Body drops to 12px, headings rise and go
     * bold, and each level steps down clearly from the one above.
     *
     * Written as a style block rather than by editing five generators, because Swing HTML
     * applies a document stylesheet to the whole pane and the generators disagree about
     * inline colours - h2 is gold in one and blue in another. Element selectors here set the
     * scale; the generators keep their own colours where they set them.
     */
    private static String style(String html) {
        String css = "<style>"
            + "body { font-family: Arial, sans-serif; font-size: 12px; line-height: 1.55;"
            + "       color: #d8dae2; }"
            + "h1 { font-size: 22px; font-weight: bold; color: #ffffff;"
            + "     border-bottom: 1px solid #3a3f4c; padding-bottom: 4px; }"
            + "h2 { font-size: 17px; font-weight: bold; margin-top: 18px; }"
            + "h3 { font-size: 14px; font-weight: bold; margin-top: 14px; color: #e8eaf0; }"
            + "p  { font-size: 12px; margin: 6px 0; }"
            + "li { font-size: 12px; margin: 3px 0; }"
            + "b  { color: #f2f4f8; }"
            // Swing's HTMLEditorKit gives an inline anchor its own margin, which shows as a
            // gap between a linked word and the comma after it - "Mercury , Virgo". The
            // generated HTML is already tight (</a> immediately followed by the comma), so
            // this is the renderer, not the markup.
            + "a  { margin: 0; padding: 0; text-decoration: none; }"
            + "</style>";
        int head = html.indexOf("<body");
        if (head < 0) {
            return css + html;
        }
        return html.substring(0, head) + css + html.substring(head);
    }

    private static final int[] PLANETS = {
            SweConst.SE_SUN, SweConst.SE_MOON, SweConst.SE_MERCURY, SweConst.SE_VENUS,
            SweConst.SE_MARS, SweConst.SE_JUPITER, SweConst.SE_SATURN, SweConst.SE_URANUS,
            SweConst.SE_NEPTUNE, SweConst.SE_PLUTO
    };

    private static final String[] PLANET_NAMES = {
            "Sun", "Moon", "Mercury", "Venus",
            "Mars", "Jupiter", "Saturn", "Uranus",
            "Neptune", "Pluto"
    };
    
    private static final String[] SIGN_NAMES = {
            "Aries", "Taurus", "Gemini", "Cancer",
            "Leo", "Virgo", "Libra", "Scorpio",
            "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    };

    private static final String[] STA_KEYS = {
            "StaSunText", "StaMoonText", "StaMercText", "StaVenuText",
            "StaMarsText", "StaJupiText", "StaSatuText", "StaUranText",
            "StaNeptText", "StaPlutText"
    };

    public InterpretationPanel(SkymapPanel skymapPanel, OuraniaWindow window) {
        this.skymapPanel = skymapPanel;
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        
        // Add a header with a Close button
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerPanel.setBackground(Color.BLACK);
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(205, 92, 92));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(true);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> {
            if (this.window != null) {
                this.window.closeInterpretationPanel();
            }
        });
        headerPanel.add(closeBtn);
        add(headerPanel, BorderLayout.NORTH);

        try {
            sw = new SwissEph(com.zodiacomputing.ourania.astro.Ephemeris.PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Flowing HTML must track the viewport width so paragraphs wrap to the panel.
        // Preformatted report tables must NOT, or the lines are forced to the panel width
        // and simply clipped - a <pre> cannot wrap, so it loses the right-hand end of
        // every row with no scrollbar to reach it.
        editorPane = new JEditorPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return !preformatted;
            }
        };
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setBackground(Color.BLACK);

        // Links inside this panel did nothing until 2026-08-23: the pane had no listener at
        // all, so the aspect patterns and the lunar mansion were text you could read and not
        // reach. "A screen with no route to it is not a built feature" - the same finding as
        // the settings screen, one level down, at a paragraph instead of a window.
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                handleLink(e.getDescription());
            }
        });

        scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    /** Light these bodies on the wheel, or clear it. Routed through the window, not direct. */
    private void highlightOnWheel(java.util.List<String> bodies) {
        if (window != null) {
            window.highlightPattern(bodies);
        }
    }

    /**
     * The panel's link scheme: four builders and parsers, and no href formatted anywhere else.
     *
     * <b>Delimited by a pipe, not an underscore.</b> The aspect grid's href was underscore
     * delimited and its row labels contain underscores, so `split("_")` returned five parts
     * where the handler expected four and every transit cell was silently unclickable for
     * months. A pipe cannot occur in a body name, a pattern name or a number.
     *
     * <b>Built and parsed in one place each, for the same reason.</b> That defect was two
     * definitions of one string drifting apart. AspectGridCheck Part H round-trips every link
     * this panel can emit.
     */
    static String patternHref(String name, java.util.List<String> bodies) {
        return "pattern|" + name + "|" + String.join(",", bodies);
    }

    /** Builds the link for one lunar mansion. */
    static String mansionHref(int number) {
        return "mansion|" + number;
    }

    /** {name, bodiesCsv} for a pattern link, or null if this is not one. */
    static String[] parsePatternHref(String href) {
        if (href == null || !href.startsWith("pattern|")) {
            return null;
        }
        String[] parts = href.split("\\|");
        return parts.length == 3 ? new String[] {parts[1], parts[2]} : null;
    }

    /** The mansion number for a mansion link, or -1. */
    static int parseMansionHref(String href) {
        if (href == null || !href.startsWith("mansion|")) {
            return -1;
        }
        String[] parts = href.split("\\|");
        if (parts.length != 2) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void handleLink(String href) {
        followLink(href);
    }

    /**
     * Follows one of this panel's links, wherever the link was clicked. True if it was one.
     *
     * <b>The panel's prose also renders on the Selection page now</b>, where a click goes to
     * OuraniaWindow.handlePlacementClick rather than to this panel's listener. Its glossary
     * terms, the forecast link and "Turn it off" would all have been dead there. The window
     * asks here before reading a link as a body, so this scheme is still parsed in one place.
     */
    boolean followLink(String href) {
        if (href == null) {
            return false;
        }
        if (href.equals("back")) {
            // Clearing the wheel on the way back matters: a figure left lit under a different
            // view is the chart asserting something the panel is no longer saying.
            highlightOnWheel(null);
            updateInterpretations();
            return true;
        }
        if (href.equals("mansions")) {
            showAllMansions();
            return true;
        }
        if (href.equals("index") || href.startsWith("index|")) {
            showIndex(href.equals("index") ? "" : href.substring(6));
            return true;
        }
        if (href.startsWith("body|")) {
            openBody(parseInt(href.substring(5), -1));
            return true;
        }
        if (href.startsWith("house|")) {
            showHouseInterpretation(parseInt(href.substring(6), -1));
            return true;
        }
        if (href.startsWith("dignity|")) {
            showDignityDetail(href.substring(8));
            return true;
        }
        if (href.startsWith("aspecttype|")) {
            showAspectTypeDetail(href.substring(11));
            return true;
        }
        if (href.startsWith("sign|")) {
            showSignInterpretation(href.substring(5));
            return true;
        }
        if (href.startsWith("decan|")) {
            String[] bits = href.substring(6).split("\\|");
            if (bits.length == 2) {
                showDecanInterpretation(bits[0], parseInt(bits[1], 1));
            }
            return true;
        }
        if (href.startsWith("sabian|")) {
            String[] bits = href.substring(7).split("\\|");
            if (bits.length == 2) {
                showSabianInterpretation(bits[0], parseInt(bits[1], 1));
            }
            return true;
        }
        if (href.startsWith("forecast|")) {
            String[] bits = href.substring(9).split("\\|");
            if (bits.length == 2) {
                showPatternForecast(bits[0], bits[1]);
            }
            return true;
        }
        String[] pattern = parsePatternHref(href);
        if (pattern != null) {
            showPatternDetail(pattern[0], pattern[1]);
            return true;
        }
        if (href.equals("unlight")) {
            highlightOnWheel(null);
            return true;
        }
        int mansion = parseMansionHref(href);
        if (mansion >= 1) {
            showMansionDetail(mansion);
            return true;
        }
        // Anything else is a no-op rather than a stack trace in the UI.
        return false;
    }

    /**
     * <b>The longitude overload exists because a mansion cannot be recovered from a sign and
     * a degree.</b> A mansion is 12.857 degrees wide and does not align to sign boundaries,
     * and the degree these callers carry is already truncated to a whole number, so rebuilding
     * a position from it can land in the neighbouring station - the boundary bug
     * LunarMansionCheck caught on its first run. Callers that have the longitude pass it;
     * the old signature passes NaN and simply gets no mansion section.
     */
    public void showPlanetInterpretation(String planetName, String signName, int degree, int decanNum, int houseNum, java.util.List<String[]> activeAspects) {
        showPlanetInterpretation(planetName, signName, degree, decanNum, houseNum, activeAspects, Double.NaN);
    }

    public void showPlanetInterpretation(String planetName, String signName, int degree, int decanNum, int houseNum, java.util.List<String[]> activeAspects, double lon) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append(generatePlanetHtml(planetName, signName, degree, decanNum, houseNum, activeAspects, lon));
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    public String generatePlanetHtml(String planetName, String signName, int degree, int decanNum, int houseNum, java.util.List<String[]> activeAspects) {
        return generatePlanetHtml(planetName, signName, degree, decanNum, houseNum, activeAspects, Double.NaN);
    }

    public String generatePlanetHtml(String planetName, String signName, int degree, int decanNum, int houseNum, java.util.List<String[]> activeAspects, double lon) {
        StringBuilder html = new StringBuilder();
        appendRelationshipFrame(html, planetName, signName, houseNum);
        
        boolean isTransit = planetName.toLowerCase().startsWith("transit_");
        String displayPlanetName = isTransit ? planetName.substring(8) : planetName; // remove transit_
        displayPlanetName = displayPlanetName.substring(0, 1).toUpperCase() + displayPlanetName.substring(1);

        int pIndex = -1;
        for (int i = 0; i < PLANET_NAMES.length; i++) {
            if (PLANET_NAMES[i].equalsIgnoreCase(displayPlanetName)) {
                pIndex = i;
                break;
            }
        }
        String coreText = (pIndex != -1) ? InterpretationData.DATA.get(STA_KEYS[pIndex]) : null;

        // Three tiers of header, best first: the original dataset's core text for the ten
        // classical planets, then extra_bodies.json's core paragraph for the points added
        // later, then the registry's bare one-liner. The last of those is a placeholder and
        // says so below, so that a gap in the data reads as a gap rather than as a bug.
        InterpretationService svc = InterpretationService.getInstance();
        String header = coreText;
        if (header == null || header.isEmpty()) {
            header = svc.getBodyCore(displayPlanetName);
        }
        boolean placeholder = header == null || header.isEmpty();
        if (placeholder) {
            header = Bodies.meaningOf(displayPlanetName);
        }
        if (header != null && !header.isEmpty()) {
            html.append("<p style='font-size:14px; font-style:italic; color:#cccccc; border-bottom: 1px solid #444; padding-bottom: 10px;'>").append(header).append("</p>");
        }
        if (placeholder && !svc.hasSignProse(displayPlanetName)
                && Bodies.byName(displayPlanetName) != null) {
            html.append("<p style='font-size:11px; color:#9AA5B1;'>Full prose for this point "
                + "is not written yet; the sign and house sections below will read as "
                + "not-found. The degree, decan and Sabian material underneath them is "
                + "per-degree and applies to any point.</p>");
        }

        String transitContext = null;
        if (isTransit && pIndex != -1) {
            if (pIndex <= 4) {
                transitContext = "This transit reflects short-term focus, daily moods, or immediate personal interactions.";
            } else {
                transitContext = "This transit indicates major, long-term life trends and deep structural changes.";
            }
        }

        if (isTransit) {
            // <b>The outer ring is not always a transit.</b> handleChartClick prefixes every
            // outer-ring body with "transit_" whatever the mode, and in SYNASTRY that ring is
            // the second PERSON. Read as a transit, their Venus produced "Transiting Venus",
            // transit-in-sign and transit-in-house prose, and aspect rows headed "to Natal
            // Sun" - a passing event described where a standing relationship belongs.
            //
            // <b>The aspect grid already knew.</b> aspectSummary in SkymapPanel carries a
            // comment about this exact confusion and routes to getSynastryInteraspect; the
            // body click was never given the same treatment. One surface corrected, its
            // neighbour left behind - which is the third instance of that shape found on
            // 2026-08-31 alone.
            boolean partner = skymapPanel != null && skymapPanel.isSynastryChart();
            InterpretationService isvc = InterpretationService.getInstance();

            if (partner) {
                // Their placement is their NATAL placement - Venus in Scorpio really is their
                // natal Venus in Scorpio - so the sign prose is the natal entry, not a
                // transit one. What changes is the frame around it and the house, which is
                // yours rather than theirs.
                html.append("<h2 style='color:#E0E0E0;'><b>Their ").append(displayPlanetName)
                    .append(" in ").append(signName).append("</b></h2>");
                html.append("<p>").append(isvc.getPlanetInSign(displayPlanetName, signName))
                    .append("</p>");

                html.append("<h3 style='color:#E0E0E0;'><b>Landing in your House ")
                    .append(houseNum).append("</b></h3>");
                String overlay = isvc.getOverlayPlanetHouse(displayPlanetName, houseNum);
                if (overlay == null) {
                    overlay = isvc.getOverlayHouse(houseNum);
                }
                if (overlay != null) {
                    html.append("<p>").append(overlay).append("</p>");
                }

                // <b>An angle contact is additional, not instead.</b> A body landing on one of
                // your corners is still in a house, and both readings are true at once - so
                // this section appears beneath the overlay rather than replacing it, and only
                // when Synastry.angleContacts says there is a contact at all.
                String onAngle = skymapPanel.synastryAngleContact(displayPlanetName);
                if (onAngle != null) {
                    String angleProse = isvc.getOverlayPlanetAngle(displayPlanetName, onAngle);
                    if (angleProse == null) {
                        angleProse = isvc.getAngleContact(onAngle);
                    }
                    if (angleProse != null) {
                        String shown = onAngle.length() <= 2 ? onAngle.toUpperCase()
                            : Character.toUpperCase(onAngle.charAt(0)) + onAngle.substring(1);
                        html.append("<h3 style='color:#E0E0E0;'><b>On your ").append(shown)
                            .append("</b></h3>");
                        html.append("<p>").append(angleProse).append("</p>");
                    }
                }
            } else {
                // Transit Sign
                html.append("<h2 style='color:#E0E0E0;'><b>Transiting ").append(displayPlanetName).append(" in ").append(signName).append("</b></h2>");
                if (transitContext != null) {
                    html.append("<p style='font-weight:bold; color:#ffb366;'>").append(transitContext).append("</p>");
                }
                html.append("<p>").append(svc.getTransit(displayPlanetName, signName)).append("</p>");

                // Transit House
                html.append("<h3 style='color:#E0E0E0;'><b>In House ").append(houseNum).append("</b></h3>");
                html.append("<p>").append(svc.getTransitInHouse(displayPlanetName, houseNum)).append("</p>");
            }

            // Aspects
            if (activeAspects != null && !activeAspects.isEmpty()) {
                html.append("<h2 style='color:#E0E0E0; margin-top: 30px;'><b>")
                    .append(partner ? "Contacts to their chart" : "Active Aspects")
                    .append("</b></h2>");
                for (String[] aspectData : activeAspects) {
                    String otherPlanet = aspectData[0];
                    String aspectType = aspectData[1];
                    String applyState = (aspectData.length > 2) ? aspectData[2] : "";
                    String displayAspect = applyState.isEmpty() ? aspectType : applyState + " " + aspectType;
                    // Optional 4th element: whose chart the other body sits in. Absent means
                    // "Natal", which is what every caller before the tri-wheel meant, so those
                    // rows render exactly as they always did. The tri-wheel is the first case
                    // with TWO people in one panel, where "Natal" would name neither of them.
                    // Display only - aspectData[0] stays the bare body name because it is the
                    // key the prose is looked up by, and a decorated name misses.
                    String whoseChart = (aspectData.length > 3 && aspectData[3] != null
                        && !aspectData[3].isEmpty()) ? aspectData[3]
                        : (partner ? "your" : "Natal");
                    html.append("<h3 style='color:#E0E0E0;'><i>").append(displayAspect)
                        .append("</i> to ").append(whoseChart).append(" ").append(otherPlanet)
                        .append("</h3>");
                    // Between two people this is an interaspect, not a transit hit.
                    String prose = partner
                        ? svc.getSynastryInteraspect(displayPlanetName, otherPlanet, aspectType)
                        : null;
                    if (prose == null) {
                        prose = svc.getTransitAspect(displayPlanetName, otherPlanet, aspectType);
                    }
                    html.append("<p>").append(prose).append("</p>");
                }
            }
        } else {
            // Sign
            html.append("<h2 style='color:#E0E0E0;'><b>").append(displayPlanetName).append(" in ").append(signName).append("</b></h2>");
            html.append("<p>").append(InterpretationService.getInstance().getPlanetInSign(planetName, signName)).append("</p>");
            
            // Decan. Both rulers are named above the prose, each with the job it actually
            // does, so that the prose opening "Sub-ruled by Jupiter" and the tarot line
            // below reading "Chaldean face ruler Venus" arrive already reconciled.
            //
            // <b>This used to be a footnote apologising for the disagreement.</b> The decan
            // detail page was given the two-role table on 2026-08-23 and the reading was
            // not, so the surface a reader actually meets kept the grey italic note and
            // never got the face ruler as a first-class part of the reading at all.
            appendDecanSection(html, signName, decanNum, "h3");

            // <b>The decan ring has been drawn since August with nothing to say about the body</b>
            // standing in it. appendDecanSection above describes the decan - its two rulers and
            // what each does. This is the other half: what this particular body does from there.
            // Composite charts get their own entry rather than the natal one, because a decan
            // sub-rulership acting on a relationship is not the same statement as it acting on
            // a person.
            boolean relChart = skymapPanel != null && skymapPanel.isRelationshipChart();
            String decanProse = InterpretationService.getInstance()
                .getBodyDecan(planetName, signName, decanNum, relChart);
            if (decanProse != null) {
                html.append("<p>").append(decanProse).append("</p>");
            }
            
            // Sabian Symbol
            html.append("<h3 style='color:#E0E0E0;'><b>Sabian Symbol (").append(signName).append(" ").append(degree).append("&deg;)</b></h3>");
            html.append("<p><i>\"").append(InterpretationService.getInstance().getSabianSymbol(signName, degree)).append("\"</i></p>");
            html.append(modernSabianHtml(signName, degree));

            // Expanded Sabian detail: interpretation, shadow expression, keywords
            String sabianFullText = InterpretationService.getInstance().getSabianFullText(signName, degree);
            String sabianShadow = InterpretationService.getInstance().getSabianShadow(signName, degree);
            String sabianKeywords = InterpretationService.getInstance().getSabianKeywords(signName, degree);
            // <b>The mansion is resolved from the longitude, never rebuilt from sign+degree.</b>
            // 12.857 degrees wide and unaligned to the signs, so a truncated degree can name the
            // wrong station. Callers without a longitude pass NaN and this section is absent -
            // which is the honest outcome, rather than a confidently wrong mansion.
            if (!Double.isNaN(lon)) {
                com.zodiacomputing.ourania.astro.LunarMansions.Mansion man =
                    com.zodiacomputing.ourania.astro.LunarMansions.at(lon);
                if (man != null) {
                    String manProse = InterpretationService.getInstance()
                        .getBodyMansion(planetName, man.number, relChart);
                    if (manProse != null) {
                        html.append("<h3 style='color:#E0E0E0;'><b>Lunar Mansion ")
                            .append(man.number).append(" - <a href='")
                            .append(mansionHref(man.number)).append("'>").append(man.name)
                            .append("</a></b></h3>");
                        html.append("<p>").append(manProse).append("</p>");
                    }
                }
            }

            // <b>The body on the degree, not just the degree.</b> The three blocks below
            // describe the Sabian symbol itself and have since July; this says what this
            // particular body does standing on it, and reads the composite entry in a
            // relationship chart.
            // The technical note on the exact degree - critical degrees and their kin.
            // One entry per body-sign-degree, natal and composite alike, so no flag.
            String techDeg = InterpretationService.getInstance()
                .getBodyTechnicalDegree(planetName, signName, degree);
            if (techDeg != null) {
                html.append("<p>").append(techDeg).append("</p>");
            }

            String bodySabian = InterpretationService.getInstance()
                .getBodySabian(planetName, signName, degree, relChart);
            if (bodySabian != null) {
                html.append("<p>").append(bodySabian).append("</p>");
            }

            if (!sabianFullText.isEmpty()) {
                html.append("<p>").append(sabianFullText).append("</p>");
            }
            if (!sabianShadow.isEmpty()) {
                html.append("<p style='color:#D08A8A;'><b>Shadow:</b> ").append(sabianShadow).append("</p>");
            }
            if (!sabianKeywords.isEmpty()) {
                html.append("<p style='color:#9AA5B1;'><i>Keywords: ").append(sabianKeywords).append("</i></p>");
            }

            // 360 Degree Interpretation
            String degreeSummary = InterpretationService.getInstance().getDegreeSummary(signName, degree);
            String degreeFullText = InterpretationService.getInstance().getDegreeFullText(signName, degree);
            if (!degreeSummary.isEmpty() || !degreeFullText.isEmpty()) {
                html.append("<h3 style='color:#E0E0E0;'><b>Degree Interpretation (").append(signName).append(" ").append(degree).append("&deg;)</b></h3>");
                if (!degreeSummary.isEmpty()) {
                    html.append("<p><b>Summary:</b> ").append(degreeSummary).append("</p>");
                }
                if (!degreeFullText.isEmpty()) {
                    html.append("<p><b>Full Text:</b> ").append(degreeFullText).append("</p>");
                }
            }
            
            // Tarot Associations
            html.append("<h2 style='color:#E0E0E0; margin-top: 30px;'><b>Tarot Associations</b></h2>");
            // "&nbsp; " after the </i>, same reason as the decan heading: a plain space at
            // an inline tag boundary collapses, closing these up to read "Planet Card(Sun)".
            // <b>The classical ten first, then the bodies the Golden Dawn never covered.</b>
            // getTarotPlanetCard returns the string "Unknown" for anything outside its ten,
            // and that was being printed on screen for nineteen of the twenty-nine registry
            // points - "Planet Card (Chiron): Unknown". The card name for those comes from
            // the lead of the tarot_body entry, and the reasoning goes underneath it.
            InterpretationService tarotSvc = InterpretationService.getInstance();
            String classical = tarotSvc.getTarotPlanetCard(planetName);
            String bodyEntry = null;
            if (classical == null || "Unknown".equals(classical)) {
                com.zodiacomputing.ourania.astro.Bodies.Def def =
                    com.zodiacomputing.ourania.astro.Bodies.byName(displayPlanetName);
                bodyEntry = def == null ? null : tarotSvc.getTarotBody(def.id);
            }
            String card = bodyEntry != null ? SkymapPanel.boldLead(bodyEntry) : classical;
            // <b>An absent field is omitted, not printed as the word Unknown.</b>
            // getTarotPlanetCard returns the literal string "Unknown" outside its ten, and
            // nine registry points still have no card of any kind - both nodes, the four
            // angles, both Lots and Black Moon Lilith. Rendering "Planet Card (Black Moon
            // Lilith): Unknown" tells the reader nothing except that the app has a hole in it.
            String shownCard = card == null || "Unknown".equals(card) ? classical : card;
            boolean haveCard = shownCard != null && !"Unknown".equals(shownCard);
            if (haveCard) {
                html.append("<p><i>Planet Card</i>&nbsp; (").append(displayPlanetName)
                    .append("): ").append(shownCard).append("</p>");
            }
            if (bodyEntry != null) {
                int cut = bodyEntry.indexOf("</b>");
                html.append("<div style='color:#cccccc; font-size:12px; margin:-6px 0 12px 0;'>")
                    .append(cut >= 0 ? bodyEntry.substring(cut + 4) : bodyEntry).append("</div>");
            }
            html.append("<p><i>Sign Card</i>&nbsp; (").append(signName).append("): ").append(InterpretationService.getInstance().getTarotSignCard(signName)).append("</p>");
            // The Golden Dawn attributions are defined in Chaldean order, so the card is
            // named with the face ruler it actually encodes, not the triplicity sub-ruler.
            String faceRuler = Zodiac.chaldeanDecanRuler(signName, decanNum);
            html.append("<p><i>Decan Card</i>&nbsp; (").append(signName).append(" Decan ").append(decanNum);
            if (!faceRuler.isEmpty()) {
                html.append(", Chaldean face ruler ").append(faceRuler);
            }
            html.append("): ").append(InterpretationService.getInstance().getTarotDecanCard(signName, decanNum)).append("</p>");
            
            // House
            html.append("<h3 style='color:#E0E0E0;'><b>In House ").append(houseNum).append("</b></h3>");
            html.append("<p>").append(InterpretationService.getInstance().getPlanetInHouse(planetName, houseNum)).append("</p>");
            
            // Aspects
            if (activeAspects != null && !activeAspects.isEmpty()) {
                html.append("<h2 style='color:#E0E0E0; margin-top: 30px;'><b>Active Aspects</b></h2>");
                for (String[] aspectData : activeAspects) {
                    String otherPlanet = aspectData[0];
                    String aspectType = aspectData[1];
                    String applyState = (aspectData.length > 2) ? aspectData[2] : "";
                    String displayAspect = applyState.isEmpty() ? aspectType : applyState + " " + aspectType;
                    html.append("<h3 style='color:#E0E0E0;'><i>").append(displayAspect).append("</i> ").append(otherPlanet).append("</h3>");
                    html.append("<p>").append(InterpretationService.getInstance().getAspect(planetName, otherPlanet, aspectType)).append("</p>");
                }
            }
        }

        return html.toString();
    }

    public void showAngleInterpretation(String angleName, String signName, int degree, java.util.List<String[]> activeAspects) {
        showAngleInterpretation(angleName, signName, degree, activeAspects, "ANCHOR", -1);
    }

    /**
     * An angle's reading, headed by whose angle it is and what that makes the card.
     *
     * <b>K7's missing half.</b> The decision settled that clicking Chart A's angle reads
     * natally while Chart B's reads cross-chart, and closed the behaviour as correct - but
     * left the labelling unbuilt, so the two cards looked identical while making different
     * claims. A deliberate asymmetry that the reader cannot see is indistinguishable from an
     * inconsistency, which is the one way this design could fail.
     *
     * @param role       ANCHOR, BRIDGE or SKY - see {@code SkymapPanel.AngleRole}
     * @param hostHouse  for a BRIDGE, which of Chart A's houses the visiting angle falls in
     */
    public void showAngleInterpretation(String angleName, String signName, int degree,
                                        java.util.List<String[]> activeAspects,
                                        String role, int hostHouse) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append(angleRoleBanner(angleName, role, hostHouse));
        html.append(generateAngleHtml(angleName, signName, degree, activeAspects));
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /**
     * The banner that says which chart this angle belongs to and which way the reading runs.
     *
     * Silent for a single chart: with one chart on screen every angle is the anchor, and a
     * badge saying so on every card would be noise that teaches the reader to stop reading
     * badges - which would cost exactly the case it exists for.
     */
    private String angleRoleBanner(String angleName, String role, int hostHouse) {
        // <b>Whenever more than one chart's angles are on the wheel.</b> Guarded first on
        // isRelationshipChart, which was wrong: in this class that name means a COMPOSITE
        // specifically - synastry has its own predicate - so the banner was silent in exactly
        // the case K7 was written about. Caught by the probe rather than by reading, because
        // the method reads as though it means "a chart about a relationship".
        if (skymapPanel == null
                || !(skymapPanel.showTransitChart || skymapPanel.isSynastryChart())) {
            return "";
        }
        StringBuilder h = new StringBuilder();
        if ("BRIDGE".equals(role)) {
            h.append("<div style='border-left:3px solid #7FB3FF; padding-left:10px; ")
             .append("margin-bottom:14px;'>")
             .append("<div style='color:#7FB3FF; font-size:12px; font-weight:bold;'>")
             .append("Chart B &rarr; Chart A &middot; ").append(angleName.toUpperCase())
             .append(" OVERLAY</div>")
             .append("<div style='color:#9AA5B1; font-size:11px;'>")
             .append("Interpersonal bridge - the visitor's impact. Chart B has no houses of ")
             .append("its own here, so this angle is read by where it falls in Chart A");
            if (hostHouse >= 1 && hostHouse <= 12) {
                h.append(": <b>Chart A's ").append(Zodiac.ordinal(hostHouse))
                 .append(" house</b>");
            }
            h.append(".</div></div>");
            return h.toString();
        }
        if ("SKY".equals(role)) {
            h.append("<div style='border-left:3px solid #B9B2D6; padding-left:10px; ")
             .append("margin-bottom:14px;'>")
             .append("<div style='color:#B9B2D6; font-size:12px; font-weight:bold;'>")
             .append("SKY &middot; ").append(angleName.toUpperCase()).append("</div>")
             .append("<div style='color:#9AA5B1; font-size:11px;'>")
             .append("A moment passing over the chart, not a person's angle.</div></div>");
            return h.toString();
        }
        h.append("<div style='border-left:3px solid #FFD166; padding-left:10px; ")
         .append("margin-bottom:14px;'>")
         .append("<div style='color:#FFD166; font-size:12px; font-weight:bold;'>")
         .append("Chart A &middot; ").append(angleName.toUpperCase()).append("</div>")
         .append("<div style='color:#9AA5B1; font-size:11px;'>")
         .append("Natal anchor - the sovereign host. The twelve houses on screen are Chart ")
         .append("A's, so this angle is read as their own and frames everything else.")
         .append("</div></div>");
        return h.toString();
    }

    public String generateAngleHtml(String angleName, String signName, int degree, java.util.List<String[]> activeAspects) {
        StringBuilder html = new StringBuilder();
        
        // Main Angle Sign Interpretation. getAngleInterpretation has files for the Ascendant,
        // the MC and the IC only; the Descendant returns an empty string, which rendered as a
        // heading followed by nothing. Its meaning is stated instead, and the Sabian and
        // degree material below is per-degree and works for all four.
        html.append("<h2 style='color:#E0E0E0;'><b>").append(angleName).append(" in ").append(signName).append("</b></h2>");
        String angleText = InterpretationService.getInstance().getAngleInterpretation(angleName, signName);
        if (angleText == null || angleText.trim().isEmpty()) {
            angleText = Bodies.meaningOf(angleName);
        }
        html.append("<p>").append(angleText).append("</p>");

        // Sabian Symbol
        html.append("<h3 style='color:#E0E0E0;'><b>Sabian Symbol (").append(signName).append(" ").append(degree).append("&deg;)</b></h3>");
        html.append("<p><i>\"").append(InterpretationService.getInstance().getSabianSymbol(signName, degree)).append("\"</i></p>");
        
        // 360 Degree Interpretation
        String degreeSummary = InterpretationService.getInstance().getDegreeSummary(signName, degree);
        String degreeFullText = InterpretationService.getInstance().getDegreeFullText(signName, degree);
        if (!degreeSummary.isEmpty() || !degreeFullText.isEmpty()) {
            html.append("<h3 style='color:#E0E0E0;'><b>Degree Interpretation (").append(signName).append(" ").append(degree).append("&deg;)</b></h3>");
            if (!degreeSummary.isEmpty()) {
                html.append("<p><b>Summary:</b> ").append(degreeSummary).append("</p>");
            }
            if (!degreeFullText.isEmpty()) {
                html.append("<p><b>Full Text:</b> ").append(degreeFullText).append("</p>");
            }
        }
        
        // Active Aspects
        if (activeAspects != null && !activeAspects.isEmpty()) {
            html.append("<h2 style='color:#E0E0E0; margin-top: 30px;'><b>Active Aspects</b></h2>");
            for (String[] aspectData : activeAspects) {
                String otherPlanet = aspectData[0];
                String aspectType = aspectData[1];
                String applyState = (aspectData.length > 2) ? aspectData[2] : "";
                String displayAspect = applyState.isEmpty() ? aspectType : applyState + " " + aspectType;
                // Optional 4th element, same convention as generatePlanetHtml: whose chart the
                // other body is in. Absent means "say nothing", which is how every caller
                // before the tri-wheel behaved and how they all still render. Only a sky angle
                // in a tri-wheel sets it, because only there are two people under one angle.
                // Display only - aspectData[0] stays the bare body name, since that is the key
                // getAngleAspectInterpretation looks the prose up by.
                String whoseChart = (aspectData.length > 3 && aspectData[3] != null
                    && !aspectData[3].isEmpty()) ? aspectData[3] + " " : "";
                html.append("<h3 style='color:#E0E0E0;'><i>").append(displayAspect).append("</i> ").append(whoseChart).append(otherPlanet).append("</h3>");
                html.append("<p>").append(InterpretationService.getInstance().getAngleAspectInterpretation(otherPlanet, aspectType, angleName)).append("</p>");
            }
        }

        return html.toString();
    }

    /**
     * The L9 snapshot: one paragraph read off the whole chart rather than a single
     * placement. Plain text from the astro package, so it is escaped rather than
     * trusted as markup - nothing upstream promises HTML.
     */
    public void showSnapshot(String when, String where, String paragraph) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>Chart Snapshot</h2>");
        if ((when != null && !when.isEmpty()) || (where != null && !where.isEmpty())) {
            html.append("<p style='color:#9AA5B1; font-size:11px;'>");
            if (when != null && !when.isEmpty()) {
                html.append(escape(when));
            }
            if (where != null && !where.isEmpty()) {
                html.append("<br>").append(escape(where));
            }
            html.append("</p>");
        }
        html.append("<p style='font-size:14px;'>").append(escape(paragraph)).append("</p>");
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /**
     * The full report tier.
     *
     * Rendered in a &lt;pre&gt; because Snapshot.report lays its tables out with spaces -
     * the body/position/reasons columns only line up in a monospaced font that keeps the
     * runs of whitespace. Dropping it into a normal paragraph collapses those runs and
     * the columns collapse with them.
     */
    public void showReport(String when, String where, String report) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 14px;'>");
        html.append("<h2 style='color:#00BFFF;'>Chart Report</h2>");
        if ((when != null && !when.isEmpty()) || (where != null && !where.isEmpty())) {
            html.append("<p style='color:#9AA5B1; font-size:11px;'>");
            if (when != null && !when.isEmpty()) {
                html.append(escape(when));
            }
            if (where != null && !where.isEmpty()) {
                html.append("<br>").append(escape(where));
            }
            html.append("</p>");
        }
        html.append("<pre style='font-family:Consolas,monospace; font-size:10px; color:#E0E0E0;'>")
            .append(escape(report))
            .append("</pre>");
        html.append("</body></html>");
        setHtml(html.toString(), true);
    }

    /**
     * Shows a fully synthesized narrative HTML string generated by NarrativeSynthesizer.
     * Uses normal flow layout (preformatted=false) since it is regular markup, not a table.
     */
    public void showSynthesis(String html) {
        setHtml(html, false);
    }

    /**
     * The annual almanac. Preformatted for the same reason the report is: the day and
     * time columns only line up in a monospaced font that keeps its runs of spaces.
     */
    public void showCalendar(int year, String body) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 14px;'>");
        html.append("<h2 style='color:#00BFFF;'>Astro Calendar ").append(year).append("</h2>");
        html.append("<p style='color:#9AA5B1; font-size:11px;'>Ingresses, stations, "
            + "lunations and exact aspects between the slow bodies. Moon sign changes are "
            + "excluded &mdash; there are about 157 a year and they crowd out everything else.</p>");
        html.append("<pre style='font-family:Consolas,monospace; font-size:10px; color:#E0E0E0;'>")
            .append(escape(body))
            .append("</pre>");
        html.append("</body></html>");
        setHtml(html.toString(), true);
    }

    /** Minimal HTML escaping for text that did not come from the interpretation JSON. */
    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }


    /**
     * What stands in a slice of the zodiac, ready to append to an interpretation panel.
     *
     * Both wheels. The natal side comes from the computed frame; the transit side from
     * SkymapPanel.transitOccupants, because the transit wheel is held as parallel arrays
     * and is never assembled into a frame.
     *
     * Takes the sign by name and the offset within it rather than an absolute longitude,
     * so the one place that can fail - an unrecognised sign name - is checked once here.
     * Zodiac.signIndexOf returns -1 for a name it does not know, and -1 * 30 is a
     * perfectly plausible-looking longitude that would quietly report the wrong slice.
     *
     * @param signName the sign as the wheel spells it, e.g. "Capricorn"
     * @param offset   degrees into that sign where the span starts
     * @param span     30 for a sign, 10 for a decan, 1 for a single degree
     * @param what     the noun used in the sentence: "sign", "decan", "degree"
     */
    private String occupantsHtml(String signName, double offset, double span, String what) {
        int sign = Zodiac.signIndexOf(signName);
        if (skymapPanel == null || sign < 0) {
            return "";
        }
        double start = sign * 30.0 + offset;
        try {
            return Occupants.html(
                Occupants.inSpan(skymapPanel.getCurrentChart(), start, span),
                skymapPanel.transitOccupants(start, span),
                what);
        } catch (Exception e) {
            // A panel that cannot show its extras is worth far less than one that throws
            // out of a click handler and leaves the interpretation blank.
            return "";
        }
    }

    /**
     * One Egyptian bound: its stretch of the sign, its ruler, what a bound is, and who is in it.
     *
     * <b>The bounds ring was drawn and had no page.</b> A click on it named the sign, for want of
     * anything better, which is a click on one item opening another (David, 2026-09-14: "each
     * click should be the entire space of the item"). The description and the mechanic are the
     * dignity index's own text, so this page and the index say the same thing about bounds.
     */
    public void showBoundInterpretation(double longitude) {
        int sign = Zodiac.signIndex(longitude);
        String signName = Zodiac.SIGNS[sign].substring(0, 1).toUpperCase()
            + Zodiac.SIGNS[sign].substring(1);
        double inSign = Zodiac.degreeInSign(longitude);
        double[] edges = com.zodiacomputing.ourania.astro.Dignity.boundEdges(sign);
        double from = 0.0;
        double to = 30.0;
        for (int i = 0; i < edges.length - 1; i++) {
            if (inSign >= edges[i] && inSign < edges[i + 1]) {
                from = edges[i];
                to = edges[i + 1];
            }
        }
        String ruler = com.zodiacomputing.ourania.astro.Dignity.boundRulerOf(sign * 30.0 + from);
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>The bound of ").append(ruler).append("</h2>");
        html.append("<p>").append(signName).append(" ").append((int) from).append("&deg; to ")
            .append((int) to).append("&deg;, in the Egyptian bounds.</p>");
        html.append("<p>").append(dignityDefinition("bound")).append("</p>");
        html.append("<p style='color:#9AA5B1;'>").append(dignityMechanic("bound")).append("</p>");
        html.append(occupantsHtml(signName, from, to - from, "bound"));
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    public void showDecanInterpretation(String signName, int decanNum) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>").append(signName).append(" Decan ")
            .append(decanNum).append("</h2>");
        appendDecanSection(html, signName, decanNum, "h3");
        html.append(occupantsHtml(signName, (decanNum - 1) * 10.0, 10.0, "decan"));
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /**
     * The trailing part of a decan heading, naming the sub-ruler the prose below it uses.
     * Empty if the sign or ordinal is not recognised, so an odd caller degrades to the
     * plain heading rather than showing a broken label.
     */
    private static String decanSchemeHeading(String signName, int decanNum) {
        String triplicity = Zodiac.triplicityDecanRuler(signName, decanNum);
        if (triplicity.isEmpty()) {
            return "";
        }
        // "&nbsp; " - entity AND a space. Swing's HTML renderer collapses the gap here to
        // nothing if given either one alone, or two plain spaces, or a space moved inside
        // the span; only the pair survives, and without it the dash abuts "Decan 3".
        return "&nbsp; <span style='color:#9AA5B1; font-weight:normal;'>&mdash; triplicity sub-ruler "
             + triplicity + "</span>";
    }

    /**
     * The four facts about a decan, above the prose rather than footnoted below it.
     *
     * <b>The two rulership schemes are presented as two roles, not as a discrepancy.</b> This
     * app carries both - {@code triplicityDecanRuler}, the modern scheme the decan prose is
     * written to, and {@code chaldeanDecanRuler}, the older face system the Golden Dawn cards
     * and the Sabian decan ruler actually encode - and they disagree for <b>30 of the 36</b>.
     *
     * That was previously explained in a grey italic note under the text, which is the worst
     * of both worlds: it reads as an apology for the data rather than as information. The
     * disagreement is real and it is old, and the honest presentation is to name what each
     * ruler governs and let the reader hold both.
     *
     * <b>The note is gone entirely as of 2026-08-25, and this table is now the only place
     * either scheme is explained.</b> It was kept as a trailing footnote when this table was
     * written, on both this page and - it turned out - nowhere else: the reading in
     * {@code generatePlanetHtml} had the footnote and never had the table, so the surface a
     * reader actually meets carried the apology without the information. Both now call this.
     *
     * <b>Every field is derived.</b> The degrees from the decan number, both rulers from
     * {@link Zodiac}, the card from the tarot dataset. Nothing here is a per-decan literal, so
     * all 36 are covered by one method and none of them can drift from the engine.
     */
    private static String decanMetadata(String signName, int decanNum) {
        String triplicity = Zodiac.triplicityDecanRuler(signName, decanNum);
        String face = Zodiac.chaldeanDecanRuler(signName, decanNum);
        String card = InterpretationService.getInstance().getTarotDecanCard(signName, decanNum);

        int signIdx = Zodiac.signIndexOf(signName);
        String borrows = "";
        if (signIdx >= 0) {
            int from = Zodiac.triplicityDecanSignIndex(signIdx, decanNum);
            if (from >= 0 && from != signIdx) {
                // Zodiac.SIGNS is lower case - it is a lookup table, not display text.
                borrows = capitalise(Zodiac.SIGNS[from]);
            }
        }

        StringBuilder html = new StringBuilder();
        html.append("<table style='font-size:12px; margin-bottom:14px;'>");
        metaRow(html, "Coordinates", (decanNum - 1) * 10 + "&deg;00' &ndash; "
            + (decanNum * 10 - 1) + "&deg;59' " + signName, null);

        if (!triplicity.isEmpty()) {
            StringBuilder v = new StringBuilder();
            String g = glyphOf(triplicity);
            if (!g.isEmpty()) {
                glyph(v, g, 13, null);
                v.append(" ");
            }
            v.append(triplicity);
            String note = "The internal filter &mdash; how this decan's mind processes"
                + (borrows.isEmpty() ? ", drawn from the sign itself"
                                     : ", borrowed from " + borrows);
            metaRow(html, "Modern triplicity sub-ruler", v.toString(), note);
        }
        if (!face.isEmpty()) {
            StringBuilder v = new StringBuilder();
            String g = glyphOf(face);
            if (!g.isEmpty()) {
                glyph(v, g, 13, null);
                v.append(" ");
            }
            v.append(face);
            metaRow(html, "Traditional Chaldean face ruler", v.toString(),
                "The external path &mdash; how the energy manifests, and what the tarot card "
                + "and the Sabian decan ruler encode");
        }
        if (card != null && !card.isEmpty() && !card.equals("Unknown")) {
            metaRow(html, "Tarot correspondence", card, null);
        }
        html.append("</table>");
        return html.toString();
    }

    /** One label/value/note row of the decan metadata table. */
    private static void metaRow(StringBuilder html, String label, String value, String note) {
        html.append("<tr><td style='padding-right:14px; color:").append(CARD_KICKER)
            .append("; vertical-align:top;'>").append(label).append("</td><td>").append(value);
        if (note != null && !note.isEmpty()) {
            html.append("<br><span style='color:").append(CARD_META)
                .append("; font-size:11px;'>").append(note).append("</span>");
        }
        html.append("</td></tr>");
    }

    /**
     * The two registers a decan's rulers are read in, one row per planet.
     *
     * <b>Per planet, not per decan.</b> Thirty-six hand-written tension paragraphs is a content
     * project; ten descriptors compose into all thirty-six and cannot fall out of step with
     * {@link Zodiac}. The cost is that the sentence is assembled rather than authored, and it
     * reads like it - this is the honest floor, not the ceiling.
     *
     * Ten rows because the triplicity scheme uses <b>modern</b> rulers, so Uranus, Neptune and
     * Pluto appear on the internal side. The Chaldean cycle only ever names the seven classical
     * planets, so the external column is unused for the outer three - kept anyway so a caller
     * cannot index off the end.
     */
    private static final String[][] RULER_REGISTER = {
        //  planet     internal - how the mind runs        external - how the path drives
        {"Sun",     "bright, self-defining vitality",   "visible pride in work that is seen"},
        {"Moon",    "instinctive, tidal feeling",       "the pull toward shelter and belonging"},
        {"Mercury", "quick, hyper-analytical scrutiny", "restless naming, sorting and connecting"},
        {"Venus",   "harmonising, value-weighing taste", "the draw toward beauty and union"},
        {"Mars",    "sharp, adversarial decisiveness",  "the will to cut and to begin"},
        {"Jupiter", "expansive, meaning-seeking reach", "appetite for scale and significance"},
        {"Saturn",  "sober, limit-testing rigour",      "the long labour of building what lasts"},
        {"Uranus",  "disruptive, pattern-breaking insight", "the break toward the untried"},
        {"Neptune", "dissolving, boundaryless imagination", "surrender into something larger"},
        {"Pluto",   "compressive, unflinching depth",   "the descent that remakes"}
    };

    /** A ruler's register, column 1 internal or 2 external, or "" for a planet not listed. */
    private static String registerOf(String planet, int column) {
        for (String[] row : RULER_REGISTER) {
            if (row[0].equalsIgnoreCase(planet)) {
                return row[column];
            }
        }
        return "";
    }

    /**
     * The core tension: the two schemes read as two layers rather than as a contradiction.
     *
     * <b>Empty for the six decans where the schemes agree</b> - Aries I, Aries II, Taurus III,
     * Leo II, Leo III, Aquarius II. There is no tension to report when one planet does both
     * jobs, and inventing one would be the app asserting something the geometry does not.
     */
    private static String decanCoreTension(String signName, int decanNum) {
        String triplicity = Zodiac.triplicityDecanRuler(signName, decanNum);
        String face = Zodiac.chaldeanDecanRuler(signName, decanNum);
        if (triplicity.isEmpty() || face.isEmpty() || triplicity.equals(face)) {
            return "";
        }
        String inner = registerOf(triplicity, 1);
        String outer = registerOf(face, 2);
        if (inner.isEmpty() || outer.isEmpty()) {
            return "";
        }
        // Plain spaces around the inline tags. <b>Verified against the parsed document, not
        // against a screenshot:</b> feeding "<b>X</b> &mdash; text" to a JEditorPane and
        // reading getDocument().getText() back shows the space present, as it is for a space
        // before an <i> and for the &nbsp; forms. All six variants keep it.
        //
        // Worth the sentence because a bold run followed by an em-dash <i>looks</i> unspaced
        // at 13px and is not - it was "corrected" twice here on that misreading. The document
        // is the ground truth for whitespace; the render is not. decanSchemeHeading's
        // "&nbsp; " is a genuinely different case - a span butted against a heading - and is
        // left exactly as it is.
        return "Your cognitive style runs on <b>" + triplicity + "</b> &mdash; " + inner
             + ". Your outward path is driven by <b>" + face + "</b> &mdash; " + outer
             + ". The friction between the two is the signature of this decan: the mind that "
             + "processes the work and the drive that makes it visible are not the same planet.";
    }

    /**
     * Prose with the ruler restatement removed.
     *
     * <b>Stripped at render, not in the data.</b> Every decan entry opens by naming its own
     * ruler - "Ruled by Mercury, this decan..." - and eighteen of the thirty-six name it a
     * second time in the first body sentence, so a reader met the same planet up to four times
     * before the reading began. The metadata table above now states it once, with the job it
     * does, which makes every one of those a repetition.
     *
     * <b>The JSON is deliberately left alone.</b> {@code interpretations.json} is read-mostly
     * and shared, and {@code ZodiacSelfTest} Part C parses these exact lead sentences to assert
     * the prose is triplicity-based. Editing the file would break that check and remove the
     * data's own witness to which scheme it was written in. Stripping in the view keeps both.
     *
     * Measured against all 36 entries before this was written: 36 of 36 leads match, 18 of 18
     * body clauses match, and no entry is left empty or truncated mid-sentence.
     */
    private static String stripRulerRestatement(String prose) {
        if (prose == null || prose.isEmpty()) {
            return prose;
        }
        String out = prose.replaceFirst(
            "^<b>(?:[Ss]ub-)?[Rr]uled by (?:the )?[A-Z][a-z]+,\\s*", "<b>");
        int cut = out.indexOf("</b>");
        if (cut >= 0) {
            String lead = capitaliseFirst(out.substring(3, cut));
            String rest = out.substring(cut);
            // The body's own restatement: either a whole sentence of pure metadata, or a
            // participial clause in front of real content. Only the metadata goes.
            rest = rest.replaceFirst(
                "^</b><br><br>Covering the first \\w+ degrees of [A-Z][a-z]+, this decan is "
                + "(?:sub-)?ruled (?:purely )?by (?:the )?[A-Z][a-z]+\\.\\s*", "</b><br><br>");
            rest = rest.replaceFirst(
                "^</b><br><br>(?:Influenced by [A-Z][a-z]+ and )?(?:[Ss]ub-)?ruled by "
                + "(?:the )?[A-Z][a-z]+,\\s*", "</b><br><br>");
            // The twelve second decans share one template - "carries a secondary Gemini
            // influence, governed by Mercury" - and both halves of that clause are now rows
            // in the table above. Only the ruler clause goes; the mechanism sentence it hangs
            // off is real content and stays. Measured: 12 of 12 match, every one followed by
            // a full stop or a comma, so the remainder is always a whole sentence.
            rest = rest.replaceAll(",\\s*governed by (?:the )?[A-Z][a-z]+(?=[.,])", "");
            int b = rest.indexOf("<br><br>");
            if (b >= 0) {
                String body = capitaliseFirst(rest.substring(b + 8));
                rest = rest.substring(0, b + 8) + body;
            }
            out = "<b>" + lead + rest;
        }
        return out;
    }

    private static String capitaliseFirst(String s) {
        if (s == null || s.isEmpty() || !Character.isLetter(s.charAt(0))) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * The whole decan block, in the order a reader needs it: the facts, the tension the two
     * schemes create, the portrait, then the Golden Dawn card.
     *
     * One method so the reading and the decan page cannot present the same decan differently -
     * which is exactly what had happened, the page having gained the metadata table while the
     * reading kept only the footnote.
     */
    private void appendDecanSection(StringBuilder html, String signName, int decanNum,
                                    String headingTag) {
        html.append("<").append(headingTag).append(" style='color:#E0E0E0;'><b>Decan ")
            .append(decanNum).append("</b>").append(decanSchemeHeading(signName, decanNum))
            .append("</").append(headingTag).append(">");
        html.append(decanMetadata(signName, decanNum));

        String tension = decanCoreTension(signName, decanNum);
        if (!tension.isEmpty()) {
            html.append("<div style='color:").append(CARD_KICKER)
                .append("; font-size:10px; margin:16px 0 0 0;'><b>THE CORE TENSION</b></div>");
            html.append("<p style='margin-top:3px;'>").append(tension).append("</p>");
        }

        String prose = InterpretationService.getInstance().getDecan(signName, decanNum);
        if (prose != null && !prose.isEmpty()
                && !prose.startsWith("Interpretation not found")) {
            html.append("<div style='color:").append(CARD_KICKER)
                .append("; font-size:10px; margin:16px 0 0 0;'><b>THE PSYCHOLOGICAL PORTRAIT</b></div>");
            html.append("<p style='margin-top:3px;'>").append(stripRulerRestatement(prose))
                .append("</p>");
        }

        String card = InterpretationService.getInstance().getTarotDecanCard(signName, decanNum);
        String face = Zodiac.chaldeanDecanRuler(signName, decanNum);
        if (card != null && !card.isEmpty() && !card.equals("Unknown") && !face.isEmpty()) {
            html.append("<div style='color:").append(CARD_KICKER)
                .append("; font-size:10px; margin:16px 0 0 0;'><b>THE TAROT &amp; GOLDEN DAWN BLUEPRINT</b></div>");
            html.append("<p style='margin-top:3px;'>In the Golden Dawn system this decan is "
                + "<b>").append(card).append("</b> &mdash; ").append(face)
                .append(" in ").append(signName)
                .append(". The attributions are <i>defined</i> in Chaldean "
                + "order, which is why the card answers to the face ruler above and not to "
                + "the triplicity sub-ruler the portrait is written in.</p>");
        }
    }

    
    public void showSignInterpretation(String signName) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>").append(signName).append("</h2>");
        html.append("<p>").append(InterpretationService.getInstance().getSign(signName)).append("</p>");
        html.append(occupantsHtml(signName, 0.0, 30.0, "sign"));
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }
    
    /**
     * The modern Sabian tiers under a degree's classic symbol, or nothing when there are none.
     *
     * Each label is its own line and each tier its own paragraph: Swing's HTML swallows a space
     * at the edge of a styled run, so "Archetype:" in bold before plain text would run into it.
     *
     * Where the file's classic symbol differs from the one printed above, the file's is shown
     * too, because the modern image and meaning were written from it and a reader should be
     * able to see which picture they describe. See InterpretationService.loadModernSabians.
     */
    static String modernSabianHtml(String signName, int degree) {
        String[] t = InterpretationService.getInstance().getModernSabian(signName, degree);
        if (t == null || t[2] == null) {
            return "";
        }
        StringBuilder h = new StringBuilder();
        if (t[0] != null && !t[0].isEmpty()) {
            h.append("<p style='color:#FFD166;'>").append(escapeText(t[0])).append("</p>");
        }
        if ("true".equals(t[5]) && t[1] != null) {
            h.append("<p style='color:#9AA5B1; font-size:11px;'>The modern reading below was ")
             .append("written from a differently worded symbol: \"")
             .append(escapeText(t[1])).append("\"</p>");
        }
        h.append("<p style='color:#9AA5B1; font-size:11px;'>Modern image</p>")
         .append("<p><i>").append(escapeText(t[2])).append("</i></p>");
        if (t[3] != null) {
            h.append("<p style='color:#9AA5B1; font-size:11px;'>Core archetype</p>")
             .append("<p>").append(escapeText(t[3])).append("</p>");
        }
        if (t[4] != null) {
            h.append("<p style='color:#9AA5B1; font-size:11px;'>What it means now</p>")
             .append("<p>").append(escapeText(t[4])).append("</p>");
        }
        return h.toString();
    }

    private static String escapeText(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public void showSabianInterpretation(String signName, int degree) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>Sabian Symbol (").append(signName).append(" ").append(degree).append("&deg;)</h2>");
        html.append("<p style='font-size:16px; font-style:italic;'>\"").append(InterpretationService.getInstance().getSabianSymbol(signName, degree)).append("\"</p>");
        html.append(modernSabianHtml(signName, degree));

        String degreeSummary = InterpretationService.getInstance().getDegreeSummary(signName, degree);
        String degreeFullText = InterpretationService.getInstance().getDegreeFullText(signName, degree);
        if (!degreeSummary.isEmpty() || !degreeFullText.isEmpty()) {
            html.append("<h2 style='color:#00BFFF; margin-top:20px;'>Degree Interpretation</h2>");
            if (!degreeSummary.isEmpty()) {
                html.append("<p><b>Summary:</b> ").append(degreeSummary).append("</p>");
            }
            if (!degreeFullText.isEmpty()) {
                html.append("<p><b>Full Text:</b> ").append(degreeFullText).append("</p>");
            }
        }
        
        // The clicked degree is the Nth of the sign, so it spans [N-1, N) from its cusp.
        html.append(occupantsHtml(signName, degree - 1, 1.0, "degree"));
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }
    
    public void showHouseInterpretation(int houseNum) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>House ").append(houseNum).append("</h2>");
        if (skymapPanel != null && skymapPanel.isRelationshipChart()) {
            String frame = InterpretationService.getInstance().getCompositeHouse(houseNum);
            if (frame != null) {
                html.append("<div style='border-left:3px solid #FFD166; padding-left:10px; ")
                    .append("margin-bottom:12px;'>").append(frame).append("</div>");
            }
        }
        html.append("<p>").append(InterpretationService.getInstance().getHouse(houseNum)).append("</p>");
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }
    
    /** "fire 8.0, air 6.0, ..." - the weighted balance, largest first. */
    private static String weights(java.util.Map<String, Double> m) {
        java.util.List<java.util.Map.Entry<String, Double>> es = new java.util.ArrayList<>(m.entrySet());
        es.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Double> e : es) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(capitalise(e.getKey())).append(' ')
              .append(String.format("%.1f", e.getValue()));
        }
        return sb.toString();
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s
            : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The link back to the full interpretation, on every detail view this panel opens. */
    private static void appendBack(StringBuilder html) {
        // <b>The hub goes in the one place every detail page already routes through.</b>
        // Eight detail views call this; adding the link here reaches all of them without
        // eight edits that would then have to be kept in step.
        html.append("<p style='margin-top:24px;'><a href='back' style='color:#7FB3FF;'>"
            + "&larr; back to the full interpretation</a>"
            + " &nbsp;&middot;&nbsp; <a href='index' style='color:#7FB3FF;'>index</a></p>");
    }

    /**
     * One aspect pattern, opened by clicking it in the list.
     *
     * Rebuilt from the live chart rather than passed in as text, so a detail view can never
     * describe a figure the chart no longer has - the panel is reached by a link that may sit
     * on screen after the chart time has moved.
     */
    public void showPatternDetail(String name, String bodiesCsv) {
        setHtml(patternDetail(name, bodiesCsv, true), false);
    }

    /**
     * One aspect pattern in full, styled, for the Selection page.
     *
     * <b>A clicked figure is a selection, not the chart's interpretation.</b> David, 2026-09-14:
     * "interpretation was meant to be a tab to interpret the entire chart, selection was meant
     * to define and show what was selected" - so the banner's click opens this there, whole.
     * No back link: there is no full interpretation behind the Selection page to go back to.
     */
    String patternDetailHtml(String name, String bodiesCsv) {
        return style(Prose.decorate(patternDetail(name, bodiesCsv, false)));
    }

    private String patternDetail(String name, String bodiesCsv, boolean withBack) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");

        com.zodiacomputing.ourania.astro.ChartFrame frame =
            skymapPanel == null ? null : skymapPanel.getCurrentChart();
        com.zodiacomputing.ourania.astro.AspectPatterns.Pattern found = null;
        com.zodiacomputing.ourania.astro.Gestalt.Result g = null;
        if (frame != null) {
            g = com.zodiacomputing.ourania.astro.Gestalt.compute(frame);
            for (com.zodiacomputing.ourania.astro.AspectPatterns.Pattern p : g.aspectPatterns) {
                if (p.name.equals(name) && String.join(",", p.bodies).equals(bodiesCsv)) {
                    found = p;
                    break;
                }
            }
        }
        if (found == null) {
            highlightOnWheel(null);
            html.append("<h2 style='color:#FFD166;'>").append(name).append("</h2>");
            html.append("<p>This figure is not in the chart as currently cast.</p>");
            if (withBack) {
                appendBack(html);
            }
            return html.append("</body></html>").toString();
        }

        // Light it on the wheel as the detail view opens. This is the answer to "where is it
        // on the chart" - the figure's own legs at full alpha with a white halo, drawn last so
        // they sit on top of the weave rather than under it.
        highlightOnWheel(found.bodies);

        html.append("<h2 style='color:#FFD166;'>").append(found.name);
        if (found.modality != null
            && (found.name.equals("T-square") || found.name.equals("Grand cross"))) {
            html.append(" &middot; ").append(capitalise(found.modality));
        } else if (found.element != null
            && (found.name.equals("Grand trine") || found.name.equals("Kite"))) {
            html.append(" &middot; ").append(capitalise(found.element));
        }
        html.append("</h2>");

        // Where each member actually stands, which is the thing a reader wants next and the
        // thing the summary line cannot fit.
        html.append("<table style='color:#E0E0E0; font-size:12px;'>");
        for (String b : found.bodies) {
            com.zodiacomputing.ourania.astro.ChartFrame.Body body = frame.body(b);
            html.append("<tr><td style='padding-right:14px;'><b>").append(b);
            if (b.equals(found.apex)) {
                html.append(" <span style='color:#FFD166;'>(apex)</span>");
            }
            html.append("</b></td><td>");
            if (body != null && body.ok) {
                html.append(com.zodiacomputing.ourania.astro.Zodiac.format(body.lon))
                    .append(" &nbsp; house ")
                    .append(com.zodiacomputing.ourania.astro.Zodiac.houseOf(
                        body.lon, frame.cusps));
            } else {
                html.append("did not compute");
            }
            html.append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p style='color:#9AA5B1; font-size:12px;'>Widest leg ")
            .append(String.format("%.2f", found.widestOrb))
            .append("&deg; from exact, inside the ")
            .append(String.format("%.1f",
                com.zodiacomputing.ourania.astro.AspectPatterns.MAX_ORB))
            .append("&deg; a pattern is allowed.</p>");

        appendMacro(html, found.detailKey());
        appendMacro(html, found.variantKey());
        if (found.isDissociate()) {
            appendMacro(html, "pattern_dissociate");
        }
        String theirs = InterpretationService.getInstance().getMacroDynamic(found.name);
        if (!theirs.isEmpty()) {
            html.append("<p><i>").append(theirs).append("</i></p>");
        }
        for (com.zodiacomputing.ourania.astro.TensionRelease.Release rel : g.releases) {
            if (rel.source.equals(found.name) && rel.bodies.equals(found.bodies)) {
                html.append("<h3 style='color:#8FD694;'>Where it discharges</h3>");
                html.append("<p style='color:#8FD694;'>").append(rel.sentence).append("</p>");
                appendEmptyLeg(html, rel);
                appendOutlets(html, rel);
            }
        }
        html.append("<p style='margin-top:14px;'><a href='forecast|").append(found.name)
            .append("|").append(String.join(",", found.bodies))
            .append("' style='color:").append(CARD_LINK).append("; font-size:13px;'>")
            .append("What's coming for this figure &mdash; the next ").append(FORECAST_MONTHS)
            .append(" months &rarr;</a></p>");
        html.append("<p style='font-size:11px; color:#9AA5B1;'>This figure is lit on the wheel. "
            + "<a href='unlight' style='color:#7FB3FF;'>Turn it off</a>.</p>");
        if (withBack) {
            appendBack(html);
        }
        return html.append("</body></html>").toString();
    }

    // ------------------------------------------------------------------ the indexes

    /**
     * A browsable index of every reference set the app carries.
     *
     * <b>Modelled on the lunar mansion list, which was the only one that had this.</b> That
     * page worked because it did three things: listed the whole set, marked the member the
     * current chart is actually on, and linked each row to the reading that already existed.
     * Everything here follows that shape, and every row link goes to a detail view that was
     * already written - nothing below renders an interpretation of its own.
     *
     * <b>The category list is derived, not typed.</b> Bodies come from the registry grouped by
     * {@code Bodies.Group}, aspects from {@code com.zodiacomputing.ourania.astro.Aspects.Type}, signs and decans from
     * {@code Zodiac}. A twelfth aspect or a thirtieth body appears in these pages the day it
     * is registered, which is the only arrangement that stays true.
     */
    public void showIndex(String category) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        if (category == null || category.isEmpty()) {
            html.append("<h1 style='color:#FFFFFF;'>Index</h1>");
            html.append("<p style='color:#9AA5B1;'>Everything the app carries a reading for. "
                + "Where the current chart sits in a set, its row is marked.</p>");
            html.append("<table style='font-size:13px;'>");
            indexRow(html, "bodies", "Bodies", Bodies.count()
                + " points, grouped as the registry groups them");
            indexRow(html, "signs", "Signs", "the twelve, with their rulers and elements");
            indexRow(html, "houses", "Houses", "the twelve departments of a life");
            indexRow(html, "aspects", "Aspects", com.zodiacomputing.ourania.astro.Aspects.Type.values().length
                + " angles the engine can find, with what each one means");
            indexRow(html, "dignities", "Dignities", "what a planet's own degree does to it, "
                + "and the scoring behind it");
            indexRow(html, "decans", "Decans", "36 faces, ten degrees each");
            indexRow(html, "sabians", "Sabian symbols", "360 degree images, by sign");
            indexRow(html, "tarot", "Tarot", "the planetary, sign, decan and body attributions");
            html.append("<tr><td style='padding-right:14px;'>"
                + "<a href='mansions' style='color:#7FB3FF;'>Lunar mansions</a></td>"
                + "<td style='color:#9AA5B1;'>28 stations of the Moon</td></tr>");
            html.append("</table>");
        } else if (category.equals("bodies")) {
            indexBodies(html);
        } else if (category.equals("signs")) {
            indexSigns(html);
        } else if (category.equals("houses")) {
            indexHouses(html);
        } else if (category.equals("aspects")) {
            indexAspects(html);
        } else if (category.equals("dignities")) {
            indexDignities(html);
        } else if (category.equals("decans")) {
            indexDecans(html);
        } else if (category.startsWith("sabians")) {
            int bar = category.indexOf('|');
            indexSabians(html, bar < 0 ? null : category.substring(bar + 1));
        } else if (category.equals("tarot")) {
            indexTarot(html);
        } else {
            html.append("<h1 style='color:#FFFFFF;'>Index</h1><p>No such section.</p>");
        }
        appendBack(html);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    private static void indexRow(StringBuilder html, String key, String title, String blurb) {
        html.append("<tr><td style='padding-right:14px;'><a href='index|").append(key)
            .append("' style='color:#7FB3FF;'>").append(title).append("</a></td>")
            .append("<td style='color:#9AA5B1;'>").append(blurb).append("</td></tr>");
    }

    /** The registry, grouped as it groups itself, with the chart's own placements marked. */
    private void indexBodies(StringBuilder html) {
        html.append("<h1 style='color:#FFFFFF;'>Bodies</h1>");
        for (Bodies.Group g : Bodies.Group.values()) {
            html.append("<h3 style='color:#add8e6; margin-bottom:2px;'>").append(g.title)
                .append("</h3>");
            html.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:6px;'>")
                .append(g.blurb).append("</div>");
            html.append("<table style='font-size:12px; margin-bottom:14px;'>");
            for (int i = 0; i < Bodies.count(); i++) {
                Bodies.Def d = Bodies.at(i);
                if (d.group != g) {
                    continue;
                }
                boolean on = skymapPanel != null && i < skymapPanel.bValid.length
                    && skymapPanel.bValid[i];
                html.append("<tr><td style='padding-right:10px;'>").append(d.glyph)
                    .append("</td><td style='padding-right:14px;'><a href='body|").append(i)
                    .append("' style='color:").append(on ? "#7FB3FF" : "#6b7785").append(";'>")
                    .append(d.name).append("</a></td>");
                html.append("<td style='padding-right:14px; color:#9AA5B1;'>");
                if (on) {
                    html.append(com.zodiacomputing.ourania.astro.Zodiac
                        .format(skymapPanel.bLon[i]));
                } else {
                    html.append("<span style='color:#6b7785;'>not in this chart</span>");
                }
                html.append("</td><td style='color:#9AA5B1;'>").append(d.meaning)
                    .append("</td></tr>");
            }
            html.append("</table>");
        }
    }

    private void indexSigns(StringBuilder html) {
        html.append("<h1 style='color:#FFFFFF;'>Signs</h1>");
        html.append("<table style='font-size:12px;'>");
        for (int i = 0; i < 12; i++) {
            String name = SkymapPanel.SIGN_NAMES[i];
            html.append("<tr><td style='padding-right:14px;'><a href='sign|").append(name)
                .append("' style='color:#7FB3FF;'>").append(name).append("</a></td>")
                .append("<td style='padding-right:14px; color:#9AA5B1;'>")
                .append(com.zodiacomputing.ourania.astro.Zodiac.elementName(i)).append(", ")
                .append(com.zodiacomputing.ourania.astro.Zodiac.modalityName(i))
                .append("</td><td style='color:#9AA5B1;'>")
                .append(InterpretationService.getInstance().getTarotSignCard(name))
                .append("</td></tr>");
        }
        html.append("</table>");
    }

    private void indexHouses(StringBuilder html) {
        html.append("<h1 style='color:#FFFFFF;'>Houses</h1>");
        html.append("<table style='font-size:12px;'>");
        for (int h = 1; h <= 12; h++) {
            html.append("<tr><td style='padding-right:14px;'><a href='house|").append(h)
                .append("' style='color:#7FB3FF;'>House ").append(h).append("</a></td>")
                .append("<td style='color:#9AA5B1;'>")
                .append(firstSentence(InterpretationService.getInstance().getHouse(h)))
                .append("</td></tr>");
        }
        html.append("</table>");
    }

    /**
     * Every aspect the engine can emit, with its own general meaning.
     *
     * No row links anywhere: an aspect on its own has no detail view, because a reading needs
     * two bodies. The general prose IS the reading at this level, so it is shown inline
     * rather than hidden behind a link that would only repeat it.
     */
    private void indexAspects(StringBuilder html) {
        html.append("<h1 style='color:#FFFFFF;'>Aspects</h1>");
        html.append("<table style='font-size:12px;'>");
        for (com.zodiacomputing.ourania.astro.Aspects.Type t : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            html.append("<tr><td style='padding-right:10px; color:")
                .append(SkymapPanel.getAspectColorHex(t.label)).append(";'>")
                .append((int) t.exactAngle).append("&deg;</td>")
                .append("<td style='padding-right:14px;'><a href='aspecttype|")
                .append(t.label).append("' style='color:")
                .append(SkymapPanel.getAspectColorHex(t.label)).append(";'>")
                .append(t.label).append("</a></td>")
                .append("<td style='padding-right:14px; color:#9AA5B1;'>1/")
                .append(t.harmonic).append("</td>")
                .append("<td style='color:#9AA5B1;'>")
                .append(firstSentence(InterpretationService.getInstance()
                    .getAspectGeneral(t.label)))
                .append("</td></tr>");
        }
        html.append("</table>");
    }

    private void indexDecans(StringBuilder html) {
        html.append("<h1 style='color:#FFFFFF;'>Decans</h1>");
        html.append("<table style='font-size:12px;'>");
        for (int i = 0; i < 12; i++) {
            String sign = SkymapPanel.SIGN_NAMES[i];
            for (int d = 1; d <= 3; d++) {
                html.append("<tr><td style='padding-right:14px;'><a href='decan|").append(sign)
                    .append("|").append(d).append("' style='color:#7FB3FF;'>").append(sign)
                    .append(" ").append(d).append("</a></td>")
                    .append("<td style='padding-right:14px; color:#9AA5B1;'>")
                    .append((d - 1) * 10).append("&deg;-").append(d * 10).append("&deg;</td>")
                    .append("<td style='color:#9AA5B1;'>")
                    .append(com.zodiacomputing.ourania.astro.Zodiac
                        .triplicityDecanRuler(sign.toLowerCase(), d))
                    .append("</td></tr>");
            }
        }
        html.append("</table>");
    }

    /**
     * 360 Sabian degrees, one sign at a time.
     *
     * <b>Split by sign deliberately.</b> Three hundred and sixty rows on one page is a list
     * nobody reads; thirty is a page. The sign menu comes first and each sign is its own view.
     */
    private void indexSabians(StringBuilder html, String sign) {
        if (sign == null || sign.isEmpty()) {
            html.append("<h1 style='color:#FFFFFF;'>Sabian symbols</h1>");
            html.append("<p style='color:#9AA5B1;'>360 images, thirty to a sign.</p>");
            html.append("<table style='font-size:13px;'>");
            for (int i = 0; i < 12; i++) {
                html.append("<tr><td><a href='index|sabians|").append(SkymapPanel.SIGN_NAMES[i])
                    .append("' style='color:#7FB3FF;'>").append(SkymapPanel.SIGN_NAMES[i])
                    .append("</a></td></tr>");
            }
            html.append("</table>");
            return;
        }
        html.append("<h1 style='color:#FFFFFF;'>Sabian symbols &middot; ").append(sign)
            .append("</h1>");
        html.append("<p><a href='index|sabians' style='color:#7FB3FF;'>all twelve signs</a></p>");
        html.append("<table style='font-size:12px;'>");
        for (int deg = 1; deg <= 30; deg++) {
            html.append("<tr><td style='padding-right:14px;'><a href='sabian|").append(sign)
                .append("|").append(deg).append("' style='color:#7FB3FF;'>").append(deg)
                .append("&deg;</a></td><td style='color:#9AA5B1;'>")
                .append(InterpretationService.getInstance().getSabianSymbol(sign, deg))
                .append("</td></tr>");
        }
        html.append("</table>");
    }

    private void indexTarot(StringBuilder html) {
        InterpretationService svc = InterpretationService.getInstance();
        html.append("<h1 style='color:#FFFFFF;'>Tarot</h1>");
        html.append("<h3 style='color:#add8e6;'>Signs</h3><table style='font-size:12px;'>");
        for (int i = 0; i < 12; i++) {
            String name = SkymapPanel.SIGN_NAMES[i];
            html.append("<tr><td style='padding-right:14px;'><a href='sign|").append(name)
                .append("' style='color:#7FB3FF;'>").append(name).append("</a></td>")
                .append("<td style='color:#9AA5B1;'>").append(svc.getTarotSignCard(name))
                .append("</td></tr>");
        }
        html.append("</table>");
        html.append("<h3 style='color:#add8e6;'>Bodies</h3><table style='font-size:12px;'>");
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            String card = svc.getTarotPlanetCard(d.id);
            String entry = svc.getTarotBody(d.id);
            if ((card == null || card.equals("Unknown")) && entry == null) {
                continue;
            }
            String shown = entry != null ? SkymapPanel.boldLead(entry) : card;
            html.append("<tr><td style='padding-right:14px;'><a href='body|").append(i)
                .append("' style='color:#7FB3FF;'>").append(d.name).append("</a></td>")
                .append("<td style='color:#9AA5B1;'>").append(shown == null ? card : shown)
                .append("</td></tr>");
        }
        html.append("</table>");
    }

    /**
     * Open a body's reading from an index row.
     *
     * The detail view needs the sign, degree, decan, house and active aspects that the chart
     * supplies - an index link carries only the registry index, so the rest is rebuilt here
     * from the panel exactly as a click on the wheel would.
     */
    private void openBody(int index) {
        if (skymapPanel == null || index < 0 || index >= SkymapPanel.BODY_COUNT) {
            return;
        }
        if (!skymapPanel.bValid[index]) {
            StringBuilder html = new StringBuilder();
            html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding:20px;'>");
            html.append("<h2 style='color:#FFFFFF;'>").append(Bodies.at(index).name)
                .append("</h2><p style='color:#9AA5B1;'>")
                .append(Bodies.at(index).meaning)
                .append("</p><p style='color:#9AA5B1;'>This point is not switched on for the "
                    + "current chart, so it has no placement to read. Turn it on in the body "
                    + "settings to see it in context.</p>");
            appendBack(html);
            html.append("</body></html>");
            setHtml(html.toString(), false);
            return;
        }
        double lon = skymapPanel.bLon[index];
        int signIdx = com.zodiacomputing.ourania.astro.Zodiac.signIndex(lon);
        int degree = (int) (lon % 30.0) + 1;
        int decan = (int) (lon % 30.0 / 10.0) + 1;
        int house = com.zodiacomputing.ourania.astro.Zodiac.houseOf(lon, skymapPanel.activeCusps);
        showPlanetInterpretation(Bodies.at(index).name, SkymapPanel.SIGN_NAMES[signIdx],
            degree, decan, house, skymapPanel.getActiveAspectsFor(index, false));
    }

    /** The lead sentence of a prose entry, for an index row. Never the whole paragraph. */
    private static String firstSentence(String prose) {
        if (prose == null) {
            return "";
        }
        String lead = SkymapPanel.boldLead(prose);
        if (lead != null) {
            return lead;
        }
        int stop = prose.indexOf(". ");
        return stop > 0 ? prose.substring(0, stop + 1) : prose;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /**
     * One aspect in full: how it is measured, what defines it, what it generally means, and
     * every instance of it in the chart on screen.
     *
     * <b>The definition is read off {@link com.zodiacomputing.ourania.astro.Aspects.Type}
     * rather than written here.</b> The exact angle, the ceiling on its orb, its harmonic, its
     * whole-sign step and its planetary nature are all declared on the enum constant, so this
     * page cannot drift from the engine that finds the aspect - and a twelfth aspect gets a
     * complete page the day it is added.
     *
     * <b>The instances at the bottom are the mansion page's trick.</b> That list works because
     * it marks where the current chart actually sits; an aspect page that only defined the
     * angle would be a glossary. These rows are the real hits in the chart on screen, each
     * linking to the pair reading that already exists.
     */
    public void showAspectTypeDetail(String label) {
        com.zodiacomputing.ourania.astro.Aspects.Type type = null;
        for (com.zodiacomputing.ourania.astro.Aspects.Type t
                : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            if (t.label.equalsIgnoreCase(label)) {
                type = t;
            }
        }
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        if (type == null) {
            html.append("<h1 style='color:#FFFFFF;'>Aspects</h1><p>No such aspect.</p>");
            appendBack(html);
            html.append("</body></html>");
            setHtml(html.toString(), false);
            return;
        }
        String colour = SkymapPanel.getAspectColorHex(type.label);
        html.append("<h1 style='color:").append(colour).append(";'>")
            .append(SkymapPanel.getAspectSymbol(type.label)).append(" ").append(type.label)
            .append("</h1>");

        html.append("<h3 style='color:#add8e6;'>How it is measured</h3>");
        html.append("<p>Take both bodies' ecliptic longitudes and the shortest arc between "
            + "them, which is always 0&deg; to 180&deg;. This aspect is that arc at <b>")
            .append(trimAngle(type.exactAngle)).append("&deg;</b>");
        if (type == com.zodiacomputing.ourania.astro.Aspects.Type.CONJUNCTION) {
            html.append(" - the two bodies at the same degree");
        }
        html.append(".</p>");

        html.append("<h3 style='color:#add8e6;'>The orb it is allowed</h3>");
        html.append("<p>A pair is judged on the <b>larger</b> of the two bodies' orbs");
        if (type.maxOrb < 1000.0) {
            html.append(", capped for this aspect at <b>").append(trimAngle(type.maxOrb))
                .append("&deg;</b> - which is tighter than most bodies allow, so the cap is "
                    + "usually what decides it");
        } else {
            html.append(", with no ceiling of its own - the bodies decide it");
        }
        html.append(". Across two charts every width is halved, cap included.</p>");
        html.append("<p style='color:#9AA5B1; font-size:12px;'>Sun or Moon ")
            .append(trimAngle(com.zodiacomputing.ourania.astro.Aspects.effectiveOrb(
                "Sun", "Moon", type, false)))
            .append("&deg; natal, ")
            .append(trimAngle(com.zodiacomputing.ourania.astro.Aspects.effectiveOrb(
                "Sun", "Moon", type, true)))
            .append("&deg; in synastry &nbsp;&middot;&nbsp; two outer planets ")
            .append(trimAngle(com.zodiacomputing.ourania.astro.Aspects.effectiveOrb(
                "Pluto", "Neptune", type, false)))
            .append("&deg; natal.</p>");

        html.append("<h3 style='color:#add8e6;'>What defines it</h3><table style='font-size:12px;'>");
        row(html, "Division of the circle", "1/" + type.harmonic
            + " &nbsp;(360&deg; &divide; " + type.harmonic + ")");
        row(html, "Whole-sign form", type.signSteps < 0
            ? "none - this angle does not fall on a whole number of signs"
            : type.signSteps + " sign" + (type.signSteps == 1 ? "" : "s") + " apart");
        row(html, "Planetary nature", type.planetaryNature == null
            ? "none assigned - the minors postdate the doctrine"
            : type.planetaryNature);
        row(html, "Family", type.maxOrb >= 1000.0
            ? "Ptolemaic - one of the five the tradition is built on"
            : "minor - capped at a degree, and added to this engine in 2026");
        html.append("</table>");

        String general = InterpretationService.getInstance().getAspectGeneral(type.label);
        if (general != null) {
            html.append("<h3 style='color:#add8e6;'>What it generally means</h3>");
            html.append("<p>").append(general).append("</p>");
        }

        appendAspectInstances(html, type);
        html.append("<p><a href='index|aspects' style='color:#7FB3FF;'>all ")
            .append(com.zodiacomputing.ourania.astro.Aspects.Type.values().length)
            .append(" aspects</a></p>");
        appendBack(html);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /** Every instance of this aspect in the chart on screen, each linking to its reading. */
    private void appendAspectInstances(StringBuilder html,
                                       com.zodiacomputing.ourania.astro.Aspects.Type type) {
        if (skymapPanel == null) {
            return;
        }
        html.append("<h3 style='color:#add8e6;'>In this chart</h3>");
        int found = 0;
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < SkymapPanel.BODY_COUNT; i++) {
            if (!skymapPanel.bValid[i]) {
                continue;
            }
            for (int j = i + 1; j < SkymapPanel.BODY_COUNT; j++) {
                if (!skymapPanel.bValid[j] || Bodies.isOppositePair(i, j)) {
                    continue;
                }
                double sep = com.zodiacomputing.ourania.astro.Aspects.separation(
                    skymapPanel.bLon[i], skymapPanel.bLon[j]);
                if (com.zodiacomputing.ourania.astro.Aspects.typeOf(
                        sep, Bodies.at(i).name, Bodies.at(j).name) != type) {
                    continue;
                }
                found++;
                double off = type == com.zodiacomputing.ourania.astro.Aspects.Type.CONJUNCTION
                    ? sep : Math.abs(sep - type.exactAngle);
                rows.append("<tr><td style='padding-right:14px;'><a href='")
                    .append(SkymapPanel.aspectHref(Bodies.at(i).name, Bodies.at(j).name, type.label))
                    .append("' style='color:#7FB3FF;'>").append(Bodies.at(i).name)
                    .append(" &ndash; ").append(Bodies.at(j).name).append("</a></td>")
                    .append("<td style='color:#9AA5B1;'>orb ")
                    .append(String.format("%.2f", off)).append("&deg;</td></tr>");
            }
        }
        if (found == 0) {
            // Said out loud. A section that vanishes cannot be told from one that broke - the
            // same reasoning as the aspect-pattern box on the placements panel.
            html.append("<p style='color:#9AA5B1;'>No ").append(type.label.toLowerCase())
                .append(" between the bodies switched on in this chart.</p>");
        } else {
            html.append("<table style='font-size:12px;'>").append(rows).append("</table>");
        }
    }

    private static void row(StringBuilder html, String key, String value) {
        html.append("<tr><td style='padding-right:14px; color:#9AA5B1;'>").append(key)
            .append("</td><td>").append(value).append("</td></tr>");
    }

    /** 60 rather than 60.0, and 1.5 kept as 1.5. */
    private static String trimAngle(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /** The nine conditions, in descending strength, with the engine's own scores. */
    private static final String[][] DIGNITIES = {
        {"domicile",   "Domicile",   "Master of its own home"},
        {"exaltation", "Exaltation", "An honoured guest in a foreign court"},
        {"triplicity", "Triplicity", "Supported by close family"},
        {"bound",      "Bound",      "Rules its own small territory"},
        {"face",       "Face",       "An anxious actor hanging on"},
        {"peregrine",  "Peregrine",  "A wandering stranger"},
        {"fall",       "Fall",       "An unwelcome visitor, brought low"},
        {"detriment",  "Detriment",  "Stranded far from home"},
    };

    /**
     * Essential dignity: what a planet's own degree does to it.
     *
     * <b>Every number on these pages comes from {@code Dignity}, never from this file.</b>
     * The scoring constants, the rulership tables, the bounds and the faces are all declared
     * there and used by the engine to score real charts - so a page that typed its own copy of
     * the table would be the second statement of a rule this project has spent the day
     * removing elsewhere.
     */
    private void indexDignities(StringBuilder html) {
        html.append("<h1 style='color:#FFFFFF;'>Essential dignity</h1>");
        html.append("<p style='color:#9AA5B1;'>A planet's condition is decided by its exact "
            + "coordinate. These are the states it can be in, strongest first, with the points "
            + "the engine actually scores them at.</p>");
        html.append("<table style='font-size:12px;'>");
        for (String[] d : DIGNITIES) {
            html.append("<tr><td style='padding-right:14px;'><a href='dignity|").append(d[0])
                .append("' style='color:#7FB3FF;'>").append(d[1]).append("</a></td>")
                .append("<td style='padding-right:14px; color:")
                .append(dignityPoints(d[0]) >= 0 ? "#7CCB8F" : "#E08A8A").append(";'>")
                .append(dignityPoints(d[0]) > 0 ? "+" : "").append(dignityPoints(d[0]))
                .append("</td><td style='color:#9AA5B1;'>").append(d[2]).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p style='color:#9AA5B1; font-size:12px;'>Essential dignity applies to the "
            + "seven traditional bodies only - ").append(String.join(", ", com.zodiacomputing.ourania.astro.Dignity.TRADITIONAL))
            .append(". The outer planets and the asteroids have no rulerships in the tradition, "
                + "so the engine assigns them none rather than inventing any.</p>");
        appendDignityTable(html);
    }

    /** One condition in full, and which of the seven are in it right now. */
    public void showDignityDetail(String key) {
        String title = null;
        String metaphor = null;
        for (String[] d : DIGNITIES) {
            if (d[0].equals(key)) {
                title = d[1];
                metaphor = d[2];
            }
        }
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        if (title == null) {
            html.append("<h1 style='color:#FFFFFF;'>Dignity</h1><p>No such condition.</p>");
            appendBack(html);
            html.append("</body></html>");
            setHtml(html.toString(), false);
            return;
        }
        int pts = dignityPoints(key);
        html.append("<h1 style='color:#FFFFFF;'>").append(title).append("</h1>");
        html.append("<p style='color:").append(pts >= 0 ? "#7CCB8F" : "#E08A8A")
            .append("; font-size:15px;'>").append(pts > 0 ? "+" : "").append(pts)
            .append(" &nbsp;<span style='color:#9AA5B1;'>&middot; ").append(metaphor)
            .append("</span></p>");

        html.append("<h3 style='color:#add8e6;'>What it is</h3><p>")
            .append(dignityDefinition(key)).append("</p>");
        html.append("<h3 style='color:#add8e6;'>How it is decided</h3><p>")
            .append(dignityMechanic(key)).append("</p>");

        // Every sign this condition falls in, for the seven, straight from the tables.
        html.append("<h3 style='color:#add8e6;'>Where it falls</h3><table style='font-size:12px;'>");
        for (int i = 0; i < 12; i++) {
            String who = dignityHolder(key, i);
            if (who == null || who.isEmpty()) {
                continue;
            }
            html.append("<tr><td style='padding-right:14px;'><a href='sign|")
                .append(SkymapPanel.SIGN_NAMES[i]).append("' style='color:#7FB3FF;'>")
                .append(SkymapPanel.SIGN_NAMES[i]).append("</a></td>")
                .append("<td style='color:#9AA5B1;'>").append(who).append("</td></tr>");
        }
        html.append("</table>");

        appendDignityHolders(html, key);
        html.append("<p><a href='index|dignities' style='color:#7FB3FF;'>all "
            + "essential dignities</a></p>");
        appendBack(html);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    private static int dignityPoints(String key) {
        switch (key) {
            case "domicile":   return com.zodiacomputing.ourania.astro.Dignity.PTS_DOMICILE;
            case "exaltation": return com.zodiacomputing.ourania.astro.Dignity.PTS_EXALTATION;
            case "triplicity": return com.zodiacomputing.ourania.astro.Dignity.PTS_TRIPLICITY;
            case "bound":      return com.zodiacomputing.ourania.astro.Dignity.PTS_BOUND;
            case "face":       return com.zodiacomputing.ourania.astro.Dignity.PTS_FACE;
            case "detriment":  return com.zodiacomputing.ourania.astro.Dignity.PTS_DETRIMENT;
            case "fall":       return com.zodiacomputing.ourania.astro.Dignity.PTS_FALL;
            case "peregrine":  return com.zodiacomputing.ourania.astro.Dignity.peregrinePenalty;
            default:           return 0;
        }
    }

    private static String dignityDefinition(String key) {
        switch (key) {
            case "domicile": return "The planet is in the sign it rules. The strongest of all "
                + "the essential dignities - it is at home, lord of its own actions, and "
                + "expresses itself without having to answer to a host.";
            case "exaltation": return "The planet is in the sign where its qualities are raised "
                + "up and honoured. Powerful but not sovereign: an exalted planet is a guest "
                + "who is treated extremely well, not the owner of the house.";
            case "triplicity": return "The planet is in a sign of an element it governs for the "
                + "chart's sect - one set of rulers by day, another by night. Being among "
                + "family: comfortable, resourced, and quietly supported rather than commanding.";
            case "bound": return "The planet occupies a specific degree range within a sign that "
                + "it rules - the bounds, also called terms. A moderate, local authority: it "
                + "governs its own small field and can regulate itself there.";
            case "face": return "The planet rules the ten-degree decan it sits in. The weakest "
                + "dignity there is, and it confers very little: enough to keep a planet from "
                + "being peregrine, and not much more.";
            case "peregrine": return "The planet has no essential dignity of any kind where it "
                + "stands - not domicile, exaltation, triplicity, bound or face. A wanderer, "
                + "with no ties or resources in that part of the zodiac. It can still act "
                + "brilliantly, but takes a roundabout path to get there."
                + "<br><br><b>The sources disagree about what this is worth, and the app takes "
                + "a position.</b> Late-medieval scoring penalises it -5, which equals detriment "
                + "and is worse than fall; classical natal practice treats it as 0, neither "
                + "dignity nor debility. Measured over 300 charts, <b>34% of all traditional "
                + "planets are peregrine</b> and at -5 two charts in three come out net "
                + "negative - a figure that common is an offset, not a judgement. This engine "
                + "uses " + com.zodiacomputing.ourania.astro.Dignity.peregrinePenalty
                + ", which keeps the scale in order: a planet merely unsupported should not "
                + "score below one the tradition calls brought low.";
            case "fall": return "The planet is in the sign opposite its exaltation - its "
                + "depression, from the Greek tapeinoma, to be brought low. Where the exalted "
                + "planet is an honoured guest, this one is an unwelcome visitor.";
            case "detriment": return "The planet is in the sign opposite the one it rules, also "
                + "called exile. Far from home and made to live by rules that run against its "
                + "nature, so its gifts come out indirect, insecure, or distorted into their "
                + "own defects.";
            default: return "";
        }
    }

    private static String dignityMechanic(String key) {
        switch (key) {
            case "domicile": return "By sign alone. The whole sign belongs to its ruler.";
            case "exaltation": return "By sign, with one degree in each singled out as the exact "
                + "degree of exaltation. The engine records that degree separately when a "
                + "planet lands on it.";
            case "triplicity": return "By element AND by sect - so the same sign gives triplicity "
                + "to a different planet by day than by night. A third, participating ruler "
                + "exists in some schemes; this engine has it switchable and off by default, "
                + "because counting it changes scores.";
            case "bound": return "By degree. Each sign is divided into five unequal stretches "
                + "with a different ruler, so a planet's bound changes several times as it "
                + "crosses a single sign.";
            case "face": return "By decan - each sign split into three tens, walked in Chaldean "
                + "order. Note this is the FACE ruler, which is not the same scheme as the "
                + "triplicity decan ruler shown on the decans page; the two disagree on thirty "
                + "of the thirty-six.";
            case "peregrine": return "By exhaustion: it is what is left when every other test "
                + "fails. Worth knowing that a planet in its own bound or face is NOT peregrine, "
                + "which is easy to miss - Mercury can sit in a sign it has no relation to and "
                + "still escape being peregrine on the strength of two degrees of bound.";
            case "fall": return "By sign, as the exact opposite of the exaltation.";
            case "detriment": return "By sign, as the exact opposite of the domicile.";
            default: return "";
        }
    }

    private static String dignityHolder(String key, int signIndex) {
        switch (key) {
            case "domicile":   return com.zodiacomputing.ourania.astro.Dignity.domicileRulerOf(signIndex);
            case "detriment":  return com.zodiacomputing.ourania.astro.Dignity.detrimentBodyOf(signIndex);
            case "exaltation": return com.zodiacomputing.ourania.astro.Dignity.exaltedBodyOf(signIndex);
            case "fall":       return com.zodiacomputing.ourania.astro.Dignity.fallBodyOf(signIndex);
            case "triplicity": return com.zodiacomputing.ourania.astro.Dignity.triplicityRulerOf(signIndex, true)
                + " by day, " + com.zodiacomputing.ourania.astro.Dignity.triplicityRulerOf(signIndex, false) + " by night";
            default:           return null;
        }
    }

    /** The seven traditional bodies scored against the chart on screen. */
    private void appendDignityTable(StringBuilder html) {
        if (skymapPanel == null) {
            return;
        }
        com.zodiacomputing.ourania.astro.ChartFrame frame = skymapPanel.getCurrentChart();
        if (frame == null) {
            return;
        }
        html.append("<h3 style='color:#add8e6;'>This chart</h3><table style='font-size:12px;'>");
        for (String body : com.zodiacomputing.ourania.astro.Dignity.TRADITIONAL) {
            int idx = Bodies.indexOfName(body);
            if (idx < 0 || idx >= SkymapPanel.BODY_COUNT || !skymapPanel.bValid[idx]) {
                continue;
            }
            com.zodiacomputing.ourania.astro.Dignity.Result r = com.zodiacomputing.ourania.astro.Dignity.evaluate(
                body, skymapPanel.bLon[idx], frame.diurnal);
            html.append("<tr><td style='padding-right:14px;'><a href='body|").append(idx)
                .append("' style='color:#7FB3FF;'>").append(body).append("</a></td>")
                .append("<td style='padding-right:14px; color:#9AA5B1;'>").append(r.sign)
                .append("</td><td style='padding-right:14px; color:")
                .append(r.score >= 0 ? "#7CCB8F" : "#E08A8A").append(";'>")
                .append(r.score > 0 ? "+" : "").append(r.score)
                .append("</td><td style='color:#9AA5B1;'>")
                .append(String.join("; ", r.reasons)).append("</td></tr>");
        }
        html.append("</table>");
    }

    /** Which of the seven are in this particular condition right now. */
    private void appendDignityHolders(StringBuilder html, String key) {
        if (skymapPanel == null) {
            return;
        }
        com.zodiacomputing.ourania.astro.ChartFrame frame = skymapPanel.getCurrentChart();
        if (frame == null) {
            return;
        }
        html.append("<h3 style='color:#add8e6;'>In this chart</h3>");
        StringBuilder rows = new StringBuilder();
        for (String body : com.zodiacomputing.ourania.astro.Dignity.TRADITIONAL) {
            int idx = Bodies.indexOfName(body);
            if (idx < 0 || idx >= SkymapPanel.BODY_COUNT || !skymapPanel.bValid[idx]) {
                continue;
            }
            com.zodiacomputing.ourania.astro.Dignity.Result r = com.zodiacomputing.ourania.astro.Dignity.evaluate(
                body, skymapPanel.bLon[idx], frame.diurnal);
            boolean here;
            switch (key) {
                case "domicile":   here = r.domicile; break;
                case "exaltation": here = r.exaltation; break;
                case "triplicity": here = r.triplicity; break;
                case "bound":      here = r.bound; break;
                case "face":       here = r.face; break;
                case "peregrine":  here = r.peregrine; break;
                case "fall":       here = r.fall; break;
                case "detriment":  here = r.detriment; break;
                default:           here = false;
            }
            if (!here) {
                continue;
            }
            rows.append("<tr><td style='padding-right:14px;'><a href='body|").append(idx)
                .append("' style='color:#7FB3FF;'>").append(body).append("</a></td>")
                .append("<td style='color:#9AA5B1;'>").append(r.sign).append(", total ")
                .append(r.score > 0 ? "+" : "").append(r.score).append("</td></tr>");
        }
        if (rows.length() == 0) {
            html.append("<p style='color:#9AA5B1;'>None of the seven is in this condition "
                + "in the chart on screen.</p>");
        } else {
            html.append("<table style='font-size:12px;'>").append(rows).append("</table>");
        }
    }

    /** One lunar mansion in full, opened by clicking it. */
    public void showMansionDetail(int number) {
        com.zodiacomputing.ourania.astro.LunarMansions.Mansion m;
        try {
            m = com.zodiacomputing.ourania.astro.LunarMansions.byNumber(number);
        } catch (IllegalArgumentException e) {
            return;
        }
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#B5A0E3;'>Mansion ").append(m.number).append(": ")
            .append(m.name).append("</h2>");
        if (m.alsoKnownAs != null) {
            html.append("<p style='color:#9AA5B1; font-size:12px;'>also ")
                .append(m.alsoKnownAs).append("</p>");
        }
        html.append("<p><b>").append(m.translation).append("</b></p>");
        html.append("<p style='color:#9AA5B1; font-size:12px;'>")
            .append(com.zodiacomputing.ourania.astro.Zodiac.format(m.start))
            .append(" to ")
            .append(com.zodiacomputing.ourania.astro.Zodiac.format(
                m.end() >= 360.0 ? 0.0 : m.end()))
            .append(" &nbsp;|&nbsp; 12&deg;51'26\" wide</p>");
        if (!m.goodFor.isEmpty()) {
            html.append("<p><b>Elected for:</b> ").append(m.goodFor).append(".</p>");
        }
        if (!m.avoid.isEmpty()) {
            html.append("<p><b>Avoid:</b> ").append(m.avoid).append(".</p>");
        }

        com.zodiacomputing.ourania.astro.ChartFrame frame =
            skymapPanel == null ? null : skymapPanel.getCurrentChart();
        if (frame != null) {
            com.zodiacomputing.ourania.astro.LunarMansions.Mansion here =
                com.zodiacomputing.ourania.astro.LunarMansions.ofMoon(frame);
            if (here != null) {
                html.append("<p style='color:#9AA5B1; font-size:12px;'>")
                    .append(here.number == m.number
                        ? "The Moon is in this mansion in the current chart."
                        : "The Moon is in mansion " + here.number + " (" + here.name
                          + ") in the current chart.")
                    .append("</p>");
            }
        }
        html.append("<p><a href='mansions' style='color:#7FB3FF;'>all 28 mansions</a></p>");
        appendBack(html);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /** The whole table of 28, each row a link. */
    public void showAllMansions() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#B5A0E3;'>The 28 Lunar Mansions</h2>");
        html.append("<p style='font-size:11px; color:#9AA5B1;'>Equal tropical stations of "
            + "12&deg;51'26\" from 0&deg; Aries, after al-Biruni. Electional and horary "
            + "machinery: they choose a moment rather than describe a character.</p>");

        com.zodiacomputing.ourania.astro.ChartFrame frame =
            skymapPanel == null ? null : skymapPanel.getCurrentChart();
        com.zodiacomputing.ourania.astro.LunarMansions.Mansion here =
            frame == null ? null
                : com.zodiacomputing.ourania.astro.LunarMansions.ofMoon(frame);

        html.append("<table style='font-size:12px;'>");
        for (com.zodiacomputing.ourania.astro.LunarMansions.Mansion m
                : com.zodiacomputing.ourania.astro.LunarMansions.all()) {
            boolean current = here != null && here.number == m.number;
            html.append("<tr><td style='padding-right:10px; color:")
                .append(current ? "#B5A0E3" : "#9AA5B1").append(";'>")
                .append(m.number).append("</td>");
            html.append("<td style='padding-right:14px;'><a href='")
                .append(mansionHref(m.number))
                .append("' style='color:").append(current ? "#B5A0E3" : "#7FB3FF")
                .append(";'>").append(m.name).append("</a>");
            if (current) {
                html.append(" &nbsp;<span style='color:#B5A0E3;'>&larr; the Moon</span>");
            }
            html.append("</td>");
            html.append("<td style='padding-right:14px; color:#9AA5B1;'>")
                .append(com.zodiacomputing.ourania.astro.Zodiac.format(m.start))
                .append("</td>");
            html.append("<td>").append(m.translation).append("</td></tr>");
        }
        html.append("</table>");
        appendBack(html);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /**
     * A cross-chart aspect, read as a relationship rather than as an event.
     *
     * <b>The body prose comes from getAspect, not getTransitAspect.</b> A natal aspect entry
     * describes two energies in a fixed relation to one another, which is exactly what a
     * synastry contact is; a transit entry describes something arriving and leaving. The
     * frame above it - what a conjunction or a square MEANS between two people - is the new
     * synastry_aspect section, and it is the part the natal dataset could never supply.
     */
    public void showSynastryAspectInterpretation(String chartA, String chartB, String aspectType) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#FFD166;'>Chart A&#39;s ").append(chartA).append(" ")
            .append(aspectType).append(" Chart B&#39;s ").append(chartB).append("</h2>");
        String frame = InterpretationService.getInstance().getSynastryAspect(aspectType);
        if (frame != null) {
            html.append("<div style='border-left:3px solid #FFD166; padding-left:10px; ")
                .append("margin-bottom:12px;'>").append(frame).append("</div>");
        }
        // The pair reading if the interaspect dataset has this one, the natal aspect reading
        // if it does not. The fallback is not a consolation prize - a natal entry describes
        // the same two energies in the same fixed relation, which is what a synastry contact
        // is; what it cannot do is name which person is at which end, and the pair entries can.
        InterpretationService svc = InterpretationService.getInstance();
        String pair = svc.getSynastryInteraspect(chartA, chartB, aspectType);
        html.append("<p>").append(pair != null ? pair
            : svc.getAspect(chartA, chartB, aspectType)).append("</p>");
        appendQuincunxKind(html, chartA, chartB, aspectType);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /**
     * The relationship frame above a body reading, when the wheel is a relationship chart.
     *
     * Prepended rather than substituted. The sign and house prose beneath still describes the
     * character of the placement correctly - Venus in Scorpio is Venus in Scorpio - what it
     * cannot say is that this Venus belongs to a pairing rather than to a person, and that is
     * the whole of what this adds. Silent when the chart is a natal one or the key is absent.
     */
    private void appendRelationshipFrame(StringBuilder html, String planetName,
                                         String signName, int houseNum) {
        if (skymapPanel == null || !skymapPanel.isRelationshipChart() || planetName == null) {
            return;
        }
        com.zodiacomputing.ourania.astro.Bodies.Def def =
            com.zodiacomputing.ourania.astro.Bodies.byName(planetName);
        String frame = def == null ? null
            : InterpretationService.getInstance().getCompositeBody(def.id);
        if (frame == null) {
            return;
        }
        html.append("<div style='border-left:3px solid #FFD166; padding-left:10px; ")
            .append("margin-bottom:12px;'>").append(frame);

        // <b>The specific reading if there is one, the house framing if there is not.</b>
        // composite_planet_house covers the ten planets by house - the way the technique is
        // actually taught - and composite_house covers all twelve houses generically. Showing
        // both would say the same thing twice for a planet that has the specific entry, which
        // is how a reading gets long without getting better.
        InterpretationService svc = InterpretationService.getInstance();
        String specific = svc.getCompositePlanetHouse(planetName, houseNum);
        String house = specific != null ? specific : svc.getCompositeHouse(houseNum);
        if (house != null) {
            html.append("<br><br>").append(house);
        }

        // <b>Sign after house, and only if the dataset has it.</b> The house reading is
        // the one the technique leans on, so it leads; the sign says what character the
        // placement has, which is the half a composite reading could not say at all
        // until composite_signs.json landed on 2026-08-30.
        String sign = svc.getCompositePlanetSign(planetName, signName);
        if (sign != null) {
            html.append("<br><br>").append(sign);
        }
        html.append("</div>");
    }

    /**
     * Marks a contact between two minor bodies as the background alignment it is.
     *
     * <b>The decision this implements assumed these cells were empty, and they are not.</b>
     * K4 says to leave the sixty asteroid-to-asteroid cells unwritten and show a data badge in
     * their place. Measured 2026-09-03: all sixty are written, so the letter of that decision
     * would now mean deleting sixty entries somebody composed. Its reasoning is about weight
     * rather than existence - Burk's point is that the ten planets are the psychic actors and
     * a contact between two asteroids has no actor to express it, not that no text should
     * exist - so the prose stays and the weight is stated.
     *
     * Without this a Pallas square Juno arrives looking exactly like a Sun square Saturn:
     * same heading, same weight of paragraph, same place on the page. Cunningham's brain
     * clutter and Tompkins' violinist turning a page in the back row are both about a reader
     * being unable to tell which is which.
     */
    private void appendTertiaryNote(StringBuilder html, String planet1, String planet2) {
        // Mutual contact between two bodies of one chart; the shared rule decides.
        if (com.zodiacomputing.ourania.astro.Bodies.hasPrimaryActor(planet1, planet2, false)) {
            return;
        }
        html.append("<div style='margin-top:16px; border-left:3px solid #6B5FA8; ")
            .append("padding-left:10px; color:#B9B2D6; font-size:11px;'>")
            .append("<b>Subtle esoteric resonance.</b> Both ends of this contact are minor ")
            .append("bodies, so it has no primary actor to express it and works in the ")
            .append("background rather than as a feature of character. Read it as texture ")
            .append("beneath the planetary signatures, not beside them.</div>");
    }

    public void showAspectInterpretation(String planet1, String planet2, String aspectType) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>").append(planet1).append(" ").append(aspectType).append(" ").append(planet2).append("</h2>");
        // <b>The same method serves a natal chart and a composite one.</b> The aspect between
        // two composite bodies is a real aspect and the natal prose describes it correctly;
        // what the natal dataset cannot say is that both ends belong to a relationship rather
        // than to a person, which is what the frame adds. Silent in a natal chart.
        InterpretationService svc = InterpretationService.getInstance();
        if (skymapPanel != null && skymapPanel.isRelationshipChart()) {
            String frame = svc.getCompositeAspectFrame(aspectType);
            if (frame != null) {
                html.append("<div style='border-left:3px solid #FFD166; padding-left:10px; ")
                    .append("margin-bottom:12px;'>").append(frame).append("</div>");
            }
        }
        String pair = skymapPanel != null && skymapPanel.isRelationshipChart()
            ? svc.getCompositeAspect(planet1, planet2, aspectType) : null;
        html.append("<p>").append(pair != null ? pair
            : svc.getAspect(planet1, planet2, aspectType)).append("</p>");
        appendQuincunxKind(html, planet1, planet2, aspectType);
        appendTertiaryNote(html, planet1, planet2);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /**
     * Which kind of quincunx this is, when it is one.
     *
     * The 49 quincunx readings have always said the same thing about incongruity, because
     * that is all a generic paragraph can say. The signs carry more than that: whether they
     * share a ruler, mirror each other across a solstice or an equinox, or have nothing
     * whatever in common. That is a discrimination the prose cannot make and the geometry
     * can, so it is computed here and appended rather than written 49 times.
     */
    private void appendQuincunxKind(StringBuilder html, String planet1, String planet2,
                                    String aspectType) {
        if (!"Quincunx".equalsIgnoreCase(aspectType)) {
            return;
        }
        com.zodiacomputing.ourania.astro.ChartFrame frame =
            skymapPanel == null ? null : skymapPanel.getCurrentChart();
        if (frame == null) {
            return;
        }
        com.zodiacomputing.ourania.astro.ChartFrame.Body a = frame.body(planet1);
        com.zodiacomputing.ourania.astro.ChartFrame.Body b = frame.body(planet2);
        if (a == null || b == null || !a.ok || !b.ok) {
            return;
        }
        int signA = com.zodiacomputing.ourania.astro.Zodiac.signIndex(a.lon);
        int signB = com.zodiacomputing.ourania.astro.Zodiac.signIndex(b.lon);

        // A quincunx by degree can be four or six signs apart when it straddles a cusp. There
        // is then no quincunx sign-pair to classify, and saying so is more use than guessing.
        if (!com.zodiacomputing.ourania.astro.Quincunx.isQuincunxPair(signA, signB)) {
            html.append("<p style='color:#9AA5B1;'><b>Dissociate.</b> The degrees make a "
                + "quincunx but the signs do not - ")
                .append(capitalise(com.zodiacomputing.ourania.astro.Zodiac.signName(a.lon)))
                .append(" to ")
                .append(capitalise(com.zodiacomputing.ourania.astro.Zodiac.signName(b.lon)))
                .append(" is not a five-sign span. The sign-based reading below does not "
                + "apply.</p>");
            return;
        }
        com.zodiacomputing.ourania.astro.Quincunx.Relation rel =
            com.zodiacomputing.ourania.astro.Quincunx.classify(signA, signB);
        html.append("<h3 style='color:#00BFFF;'>This one is ").append(rel.describe())
            .append("</h3>");
        html.append("<p>").append(rel.kind.meaning).append("</p>");
        html.append("<p style='font-size:11px; color:#9AA5B1;'>")
            .append(capitalise(com.zodiacomputing.ourania.astro.Zodiac.signName(a.lon)))
            .append(" and ")
            .append(capitalise(com.zodiacomputing.ourania.astro.Zodiac.signName(b.lon)))
            .append(" share no element, modality or polarity - every quincunx pair is like "
                + "that. What separates them is whether any secondary link survives it.</p>");
    }

    /**
     * The transit-row version, reached by clicking a cell of the transit-to-natal grid.
     *
     * Separate from {@link #showAspectInterpretation} because the lookup is a different map:
     * handing "transit_sun" to getAspect as a body name returns "Interpretation not found",
     * which is what every transit cell would have shown had they ever been clickable.
     */
    public void showTransitAspectInterpretation(String transiting, String natal, String aspectType) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h2 style='color:#00BFFF;'>Transiting ").append(transiting).append(" ")
            .append(aspectType).append(" natal ").append(natal).append("</h2>");
        html.append("<p>").append(InterpretationService.getInstance()
            .getTransitAspect(transiting, natal, aspectType)).append("</p>");
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /** How far ahead a pattern forecast looks. Two years covers a slow body's whole passage. */
    private static final int FORECAST_MONTHS = 24;

    /** The bodies slow enough that a perfection dates a season rather than a week. */
    private static final String[] FORECAST_BODIES =
        {"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"};

    /**
     * When the sky next comes back to this figure, and what it will do when it does.
     *
     * <b>Its own page rather than part of the pattern detail, because it costs real time.</b>
     * Measured: 60 root-finding scans over 24 months is 2.0 seconds, 1.1 over 12, and
     * showPatternDetail runs on the event thread - inlining it would freeze the window on
     * every pattern click. Behind a link the reader has asked for it.
     *
     * <b>The empty leg is scanned for conjunctions only, and that is geometry, not a
     * shortcut.</b> The vacant point is opposite the apex, so a square to it falls on the same
     * two degrees as a square to the apex, and an opposition to it IS a conjunction to the
     * apex - the first run of this reported "Saturn conjunct Chiron" and "Saturn opposite the
     * empty leg" on the same date, which is one event printed twice. What a conjunction to the
     * empty leg uniquely means is that the transiting body occupies the vacancy and
     * <b>completes the T-square into a Grand cross</b> for as long as it sits there. That is
     * the one event on this page worth interrupting someone for, and it is called out.
     */
    public void showPatternForecast(String name, String bodiesCsv) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding:14px;'>");

        com.zodiacomputing.ourania.astro.ChartFrame frame =
            skymapPanel == null ? null : skymapPanel.getCurrentChart();
        if (frame == null || sw == null) {
            html.append("<h2 style='color:#FFD166;'>What's coming</h2>");
            html.append("<p>No chart is cast, so there is nothing to scan against.</p>");
            appendBack(html);
            html.append("</body></html>");
            setHtml(html.toString(), false);
            return;
        }

        com.zodiacomputing.ourania.astro.Gestalt.Result g =
            com.zodiacomputing.ourania.astro.Gestalt.compute(frame);
        com.zodiacomputing.ourania.astro.AspectPatterns.Pattern found = null;
        for (com.zodiacomputing.ourania.astro.AspectPatterns.Pattern p : g.aspectPatterns) {
            if (p.name.equals(name) && String.join(",", p.bodies).equals(bodiesCsv)) {
                found = p;
                break;
            }
        }
        if (found == null) {
            html.append("<h2 style='color:#FFD166;'>What's coming</h2>");
            html.append("<p>This figure is not in the chart as currently cast.</p>");
            appendBack(html);
            html.append("</body></html>");
            setHtml(html.toString(), false);
            return;
        }
        com.zodiacomputing.ourania.astro.TensionRelease.Release rel = null;
        for (com.zodiacomputing.ourania.astro.TensionRelease.Release r : g.releases) {
            if (r.source.equals(found.name) && r.bodies.equals(found.bodies)) {
                rel = r;
                break;
            }
        }

        boolean relationship = skymapPanel.isRelationshipChart();
        html.append("<h2 style='color:#FFD166;'>What's coming for the ")
            .append(found.name.toLowerCase()).append("</h2>");
        html.append("<p style='color:").append(CARD_META).append("; font-size:12px;'>")
            .append(String.join(" &middot; ", found.bodies));
        if (found.apex != null) {
            html.append(" &nbsp;|&nbsp; apex <b>").append(found.apex).append("</b>");
        }
        html.append("</p>");
        html.append("<p style='color:").append(CARD_META).append("; font-size:11px;'>")
            .append("The next ").append(FORECAST_MONTHS)
            .append(" months, from Jupiter outward. These are the bodies slow enough that a "
                + "perfection marks a season rather than an afternoon; the Moon and the "
                + "personal planets cross these degrees several times a year and cannot "
                + "single out one of them.")
            .append(relationship ? " The figure belongs to the pairing, so a transit here "
                + "reaches both people at once." : "")
            .append("</p>");

        java.util.List<Object[]> events = collectForecast(frame, found, rel);
        if (events.isEmpty()) {
            html.append("<p>Nothing from Jupiter outward perfects on this figure in the next ")
                .append(FORECAST_MONTHS).append(" months. That is a real answer rather than "
                + "an empty one: the configuration is not scheduled to be pressed from "
                + "outside during that window.</p>");
        } else {
            appendForecastRows(html, events, found);
        }
        html.append("<p style='margin-top:14px;'><a href='")
            .append(patternHref(found.name, found.bodies))
            .append("' style='color:").append(CARD_LINK).append(";'>")
            .append("&larr; back to the figure</a></p>");
        appendBack(html);
        html.append("</body></html>");
        setHtml(html.toString(), false);
    }

    /**
     * Every perfection on this figure in the window, as {jd, body, type, targetName, apexFlag}.
     *
     * Sorted by date, because a forecast a reader cannot read in order is a table, not a
     * forecast.
     */
    private java.util.List<Object[]> collectForecast(
            com.zodiacomputing.ourania.astro.ChartFrame frame,
            com.zodiacomputing.ourania.astro.AspectPatterns.Pattern found,
            com.zodiacomputing.ourania.astro.TensionRelease.Release rel) {

        java.util.LinkedHashMap<String, Double> targets = new java.util.LinkedHashMap<>();
        for (String b : found.bodies) {
            com.zodiacomputing.ourania.astro.ChartFrame.Body body = frame.body(b);
            if (body != null && body.ok) {
                targets.put(b, body.lon);
            }
        }

        double from = new de.thmac.swisseph.SweDate(
            java.time.ZonedDateTime.now().getYear(),
            java.time.ZonedDateTime.now().getMonthValue(),
            java.time.ZonedDateTime.now().getDayOfMonth(), 0.0).getJulDay();
        double to = from + FORECAST_MONTHS * 30.4;

        com.zodiacomputing.ourania.astro.Aspects.Type[] hard = {
            com.zodiacomputing.ourania.astro.Aspects.Type.CONJUNCTION,
            com.zodiacomputing.ourania.astro.Aspects.Type.SQUARE,
            com.zodiacomputing.ourania.astro.Aspects.Type.OPPOSITION
        };

        java.util.List<Object[]> out = new java.util.ArrayList<>();
        for (String body : FORECAST_BODIES) {
            for (java.util.Map.Entry<String, Double> t : targets.entrySet()) {
                for (com.zodiacomputing.ourania.astro.Aspects.Type type : hard) {
                    for (double jd : com.zodiacomputing.ourania.astro.Transits.exactDates(
                            sw, body, t.getValue(), type, from, to)) {
                        out.add(new Object[] {jd, body, type, t.getKey(), Boolean.FALSE});
                    }
                }
            }
            // The vacancy, conjunctions only - see the note on showPatternForecast.
            if (rel != null && rel.hasEmptyLeg) {
                for (double jd : com.zodiacomputing.ourania.astro.Transits.exactDates(
                        sw, body, rel.emptyLegLon,
                        com.zodiacomputing.ourania.astro.Aspects.Type.CONJUNCTION, from, to)) {
                    out.add(new Object[] {jd, body,
                        com.zodiacomputing.ourania.astro.Aspects.Type.CONJUNCTION,
                        "the empty leg", Boolean.TRUE});
                }
            }
        }
        out.sort(java.util.Comparator.comparingDouble(a -> (Double) a[0]));
        return out;
    }

    /** The forecast as dated rows, with the completing transit called out where it occurs. */
    private void appendForecastRows(StringBuilder html, java.util.List<Object[]> events,
                                    com.zodiacomputing.ourania.astro.AspectPatterns.Pattern found) {
        InterpretationService svc = InterpretationService.getInstance();
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                           "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (Object[] e : events) {
            de.thmac.swisseph.SweDate d = new de.thmac.swisseph.SweDate((Double) e[0]);
            String body = (String) e[1];
            com.zodiacomputing.ourania.astro.Aspects.Type type =
                (com.zodiacomputing.ourania.astro.Aspects.Type) e[2];
            String target = (String) e[3];
            boolean completes = Boolean.TRUE.equals(e[4]);

            cardOpen(html, completes ? ACCENT_PATTERN : ACCENT_BODY, null);
            cardTitle(html, null, d.getDay() + " " + months[d.getMonth()] + " " + d.getYear(),
                null);
            StringBuilder meta = new StringBuilder();
            meta.append("transiting <b>").append(body).append("</b> ")
                .append(type.label.toLowerCase()).append(" ");
            if (target.equals("the empty leg")) {
                meta.append("<b>the empty leg</b>");
            } else {
                meta.append("<b>").append(target).append("</b>");
                if (target.equals(found.apex)) {
                    meta.append(" <span style='color:").append(ACCENT_PATTERN)
                        .append(";'>the apex</span>");
                }
            }
            cardMeta(html, meta.toString());
            cardRule(html);

            if (completes) {
                cardBlock(html, ACCENT_PATTERN, "THIS ONE COMPLETES THE FIGURE",
                    "<b>" + body + "</b> occupies the vacant point opposite the apex, which "
                    + "turns this " + found.name.toLowerCase() + " into a grand cross for as "
                    + "long as it stays there. The outlet the figure has been discharging "
                    + "through is taken up by the transit, so the pressure has nowhere "
                    + "unoccupied to go and has to be handled deliberately.");
            }
            cardBlock(html, "#C9D1D9", "WHAT IT DOES",
                aspectMeaning(svc.getTransitAspect(body, target, type.label)));
            int targetIdx = Bodies.indexOfName(target);
            cardLinks(html, new String[][] {
                {"aspecttype|" + type.label, "The " + type.label.toLowerCase()},
                {targetIdx >= 0 ? "body|" + targetIdx : null, target}
            });
            cardClose(html);
        }
    }

    /**
     * The empty leg, as a place rather than as a number.
     *
     * <b>The engine has computed this since TensionRelease was written and nothing showed
     * it.</b> {@code Release} carries emptyLegLon, emptyLegSign, emptyLegHouse and any
     * occupant, and the only surface for it was one clause inside {@code rel.sentence}. For a
     * T-square the vacant point is the whole practical answer to "what do we do about it" -
     * the sign and house opposite the apex are where the pressure is worked off deliberately
     * rather than discharged at whoever is nearest - so it gets its own section, its sign
     * reading, its house reading, and a link to each.
     *
     * Silent for a Grand cross, which has four occupied angles and no vacancy, and for an
     * unpatterned opposition, which has no apex to be opposite.
     */
    private void appendEmptyLeg(StringBuilder html,
                                com.zodiacomputing.ourania.astro.TensionRelease.Release rel) {
        if (!rel.hasEmptyLeg) {
            return;
        }
        InterpretationService svc = InterpretationService.getInstance();
        boolean relationship = skymapPanel != null && skymapPanel.isRelationshipChart();

        html.append("<h3 style='color:#7FB3FF;'>The empty leg")
            .append(rel.emptyLegOccupant == null ? "" : " - and what stands on it")
            .append("</h3>");

        html.append("<p style='color:#9AA5B1; font-size:12px;'>")
            .append(com.zodiacomputing.ourania.astro.Zodiac.format(rel.emptyLegLon));
        if (rel.emptyLegHouse > 0) {
            html.append(" &nbsp;|&nbsp; house ").append(rel.emptyLegHouse);
        }
        html.append(" &nbsp;|&nbsp; opposite the apex")
            .append(rel.apex == null ? "" : " " + rel.apex).append("</p>");

        if (rel.emptyLegOccupant != null) {
            html.append("<p><b>").append(rel.emptyLegOccupant)
                .append("</b> stands there, so this figure's vacancy is not vacant. The point "
                    + "opposite the apex is occupied, which means the release is already "
                    + "wired in rather than waiting to be chosen.</p>");
        } else {
            html.append("<p>Nothing stands there. That is the point: the degree opposite the "
                + "apex is where this figure's pressure can be worked off <i>deliberately</i>, "
                + "and because no body occupies it, nothing does that automatically. It is the "
                + "part of the chart that has to be taken up on purpose.</p>");
        }

        // The sign and the house of the vacant point, each with its own reading. In a
        // relationship chart the composite house frame goes first, for the same reason it does
        // above a body: the house means something different for a pairing than for a person.
        if (rel.emptyLegSign != null && !rel.emptyLegSign.isEmpty()) {
            String sign = capitalise(rel.emptyLegSign);
            String prose = leadOrNull(svc.getSign(sign));
            if (prose != null) {
                html.append("<p><b>").append(sign).append("</b> &mdash; ").append(prose)
                    .append("</p>");
            }
            if (rel.emptyLegHouse > 0) {
                if (relationship) {
                    String comp = svc.getCompositeHouse(rel.emptyLegHouse);
                    if (comp != null) {
                        html.append("<div style='border-left:3px solid #FFD166; ")
                            .append("padding-left:10px; margin:10px 0;'>").append(comp)
                            .append("</div>");
                    }
                }
                String house = leadOrNull(svc.getHouse(rel.emptyLegHouse));
                if (house != null) {
                    html.append("<p><b>House ").append(rel.emptyLegHouse)
                        .append("</b> &mdash; ").append(house).append("</p>");
                }
            }
            cardLinks(html, new String[][] {
                {"sign|" + sign, sign + " in full"},
                {rel.emptyLegHouse > 0 ? "house|" + rel.emptyLegHouse : null,
                 "House " + rel.emptyLegHouse},
                {"sabian|" + sign + "|"
                    + ((int) com.zodiacomputing.ourania.astro.Zodiac
                        .degreeInSign(rel.emptyLegLon) + 1), "The degree itself"}
            });
        }
    }

    /**
     * What reaches into the configuration from outside it.
     *
     * The apex-touching outlet is the classical release of a T-square, and a mediator - one
     * body softening two members at once - is the strongest form of it. Both are already on
     * {@link com.zodiacomputing.ourania.astro.TensionRelease.Outlet} and neither was shown.
     */
    private void appendOutlets(StringBuilder html,
                               com.zodiacomputing.ourania.astro.TensionRelease.Release rel) {
        if (rel.outlets.isEmpty()) {
            return;
        }
        html.append("<h3 style='color:#8FD694;'>What softens it</h3>");
        html.append("<table style='color:#E0E0E0; font-size:12px;'>");
        for (com.zodiacomputing.ourania.astro.TensionRelease.Outlet o : rel.outlets) {
            html.append("<tr><td style='padding-right:14px; vertical-align:top;'><b>")
                .append(o.via).append("</b>");
            if (o.touchesApex) {
                html.append(" <span style='color:#8FD694;'>reaches the apex</span>");
            }
            if (o.isMediator()) {
                html.append(" <span style='color:#FFD166;'>mediator</span>");
            }
            // Built from the two parallel lists rather than from phrase(), which leads with
            // the body name that is already in the column to the left.
            html.append("</td><td>");
            for (int i = 0; i < o.to.size(); i++) {
                if (i > 0) {
                    html.append(i == o.to.size() - 1 ? " and " : ", ");
                }
                html.append(o.types.get(i).label.toLowerCase()).append(' ')
                    .append(o.to.get(i));
            }
            html.append(" <span style='color:#9AA5B1;'>(closest ")
                .append(orbText(o.bestOffBy)).append(")</span></td></tr>");
        }
        html.append("</table>");
    }

    /** Appends a macro-dynamics paragraph if the key is non-null and the data has it. */
    private static void appendMacro(StringBuilder html, String key) {
        if (key == null) {
            return;
        }
        String text = InterpretationService.getInstance().getMacroDynamic(key);
        if (!text.isEmpty()) {
            html.append("<p style='color:#C9D1D9;'>").append(text).append("</p>");
        }
    }

    // ---------------------------------------------------------------------- card chrome

    /*
     * The dashboard's card chrome, defined once.
     *
     * <b>Every value here was measured in a JEditorPane before it was written down.</b> That
     * renderer is HTML 3.2 with a CSS1 subset and it silently drops what it does not
     * understand, so a card built from browser habits looks correct in the source and renders
     * as flat text on screen. border-radius and box-shadow are both dropped; what survives is
     * background-color, a full border, a heavier border-left, padding and margin - which is
     * the whole of the card look below.
     *
     * <b>The rule under the rule the fill divider follows.</b> An empty div with only a
     * border-top collapses to nothing here, so the divider carries a non-breaking space at
     * font-size:1px. That is the reason for an otherwise inexplicable line.
     */
    private static final String CARD_BG = "#14181F";
    private static final String CARD_EDGE = "#2E3542";
    private static final String CARD_KICKER = "#7E8AA0";
    private static final String CARD_LINK = "#7FB3FF";
    private static final String CARD_META = "#9AA5B1";

    /** Accents, so a reader learns the sections by colour rather than by re-reading them. */
    private static final String ACCENT_PATTERN = "#FFD166";
    private static final String ACCENT_MANSION = "#B5A0E3";
    private static final String ACCENT_MACRO = "#00BFFF";
    private static final String ACCENT_BODY = "#8FA6C9";
    private static final String ACCENT_FRICTION = "#E08A8A";

    /**
     * How many minor aspects get a card before the rest are left to the grid.
     *
     * <b>Three, because the point of the section is that it is a shortlist.</b> A chart of 29
     * points routinely carries fifty or more minor hits; carding all of them would rebuild the
     * scrolling document this screen was rebuilt to stop being. The count in the section
     * heading tells the reader how many were not shown.
     */
    private static final int FRICTION_CARDS = 3;

    /**
     * Glyphs render, but only in a font that composites.
     *
     * <b>Arial reports canDisplay=false for every planetary and zodiacal glyph in the app.</b>
     * The HTML renderer finds a fallback face anyway, so they do appear - but the body font is
     * declared Arial and relying on an undeclared fallback is how a glyph turns into a hollow
     * box on a machine with a different font set. SansSerif is a logical font and composites by
     * contract, which is why the wheel's own HTML rows at SkymapPanel:3059 already use it.
     */
    private static void glyph(StringBuilder html, String g, int size, String colour) {
        if (g == null || g.isEmpty()) {
            return;
        }
        html.append("<span style='font-family:SansSerif; font-size:").append(size).append("px;");
        if (colour != null) {
            html.append(" color:").append(colour).append(";");
        }
        html.append("'>").append(g).append("</span>");
    }

    private static void cardOpen(StringBuilder html, String accent, String kicker) {
        html.append("<div style='background-color:").append(CARD_BG)
            .append("; border:1px solid ").append(CARD_EDGE)
            .append("; border-left:3px solid ").append(accent)
            .append("; padding:12px 14px; margin:0 0 14px 0;'>");
        if (kicker != null && !kicker.isEmpty()) {
            html.append("<div style='color:").append(CARD_KICKER)
                .append("; font-size:10px; margin:16px 0 0 0;'><b>").append(kicker).append("</b></div>");
        }
    }

    /** The card's headline: an optional glyph, the name, and a quieter qualifier. */
    private static void cardTitle(StringBuilder html, String g, String title, String sub) {
        html.append("<div style='font-size:15px; color:#FFFFFF; margin-top:3px;'>");
        if (g != null && !g.isEmpty()) {
            glyph(html, g, 16, null);
            html.append("&nbsp; ");
        }
        html.append("<b>").append(title).append("</b>");
        if (sub != null && !sub.isEmpty()) {
            html.append(" <span style='color:").append(CARD_META)
                .append("; font-size:12px; font-weight:normal;'>").append(sub).append("</span>");
        }
        html.append("</div>");
    }

    /** The metadata strip: coordinates, orbs, rulers - never the takeaway. */
    private static void cardMeta(StringBuilder html, String meta) {
        if (meta == null || meta.isEmpty()) {
            return;
        }
        html.append("<div style='color:").append(CARD_META)
            .append("; font-size:11px; margin-top:2px;'>").append(meta).append("</div>");
    }

    private static void cardRule(StringBuilder html) {
        html.append("<div style='border-top:1px solid ").append(CARD_EDGE)
            .append("; margin:9px 0 8px 0; font-size:1px;'>&nbsp;</div>");
    }

    /**
     * One labelled block of prose.
     *
     * <b>Silent when the text is absent or is the not-found sentinel.</b> That is the third
     * point of the critique this rebuild answers, applied to the field where it still bit:
     * getPlanetInSign returns the literal string "Interpretation not found for Vesta in Leo.
     * Please add to JSON." for a key nobody has written yet, and a card that prints it has
     * told the reader nothing except that the app has a hole in it.
     */
    private static void cardBlock(StringBuilder html, String labelColour, String label,
                                  String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        html.append("<div style='margin-bottom:7px;'>");
        if (label != null && !label.isEmpty()) {
            html.append("<span style='color:").append(labelColour)
                .append("; font-size:10px;'><b>").append(label).append("</b></span><br>");
        }
        html.append(text).append("</div>");
    }

    /** The card's way out: where the depth this card is not showing actually lives. */
    private static void cardLink(StringBuilder html, String href, String label) {
        html.append("<div style='margin-top:9px;'><a href='").append(href)
            .append("' style='color:").append(CARD_LINK).append("; font-size:11px;'>")
            .append(label).append(" &rarr;</a></div>");
    }

    /** Several ways out on one line, for a card that fans out to more than one reading. */
    private static void cardLinks(StringBuilder html, String[][] hrefLabels) {
        html.append("<div style='margin-top:9px; font-size:11px;'>");
        boolean first = true;
        for (String[] hl : hrefLabels) {
            if (hl[0] == null) {
                continue;
            }
            if (!first) {
                html.append(" <span style='color:").append(CARD_EDGE).append(";'>|</span> ");
            }
            html.append("<a href='").append(hl[0]).append("' style='color:").append(CARD_LINK)
                .append(";'>").append(hl[1]).append("</a>");
            first = false;
        }
        html.append("</div>");
    }

    private static void cardClose(StringBuilder html) {
        html.append("</div>");
    }

    /**
     * The rule above a run of cards, naming the group and counting it.
     *
     * The count is the part that earns its place: "3 closed circuits" tells a reader how much
     * screen is coming before they start scrolling through it, which a bare heading does not.
     */
    private static void sectionHeading(StringBuilder html, String accent, String title,
                                       String count) {
        html.append("<div style='margin:16px 0 8px 0;'>");
        html.append("<span style='color:").append(accent).append("; font-size:12px;'><b>")
            .append(title).append("</b></span>");
        if (count != null && !count.isEmpty()) {
            html.append(" <span style='color:").append(CARD_KICKER)
                .append("; font-size:11px;'>&middot; ").append(count).append("</span>");
        }
        html.append("</div>");
    }

    /** An orb as degrees and minutes - "0&deg;48'" - because 0.8&deg; is not how an orb reads. */
    private static String orbText(double degrees) {
        int d = (int) degrees;
        int m = (int) Math.round((degrees - d) * 60.0);
        if (m == 60) {
            d++;
            m = 0;
        }
        return d + "&deg;" + String.format("%02d", m) + "'";
    }

    /**
     * A body's glyph, but only when it really is a glyph.
     *
     * <b>Four of the twenty-nine registry points carry a text abbreviation in the glyph field
     * rather than a symbol</b> - Nessus "Ns", Pholus "Ph", Ascendant "ASC", Descendant "DSC" -
     * because no font reliably carries U+2BF1 and friends. Two more, MC and IC, are their own
     * name, which {@link SkymapPanel#namedGlyph} already drops. Counted, not guessed: 23
     * symbols, 4 abbreviations, 2 same-as-name.
     *
     * <b>That is correct on the wheel and wrong in a sentence.</b> SkymapPanel's own comment
     * decides that "ASC Ascendant" reads acceptably in its compact label rows, and for a label
     * row it does - so namedGlyph is left exactly as it is. But a card title that renders
     * "Moon semisquare Ns Nessus" beside another reading "Sun semisquare Venus" with real
     * symbols is mixing two registers in one sentence. Here the letters are dropped and the
     * name carries it alone.
     *
     * The test is the codepoint, not a list of the four. A twenty-fourth symbol added to the
     * registry tomorrow is treated correctly without anyone remembering this method exists.
     */
    private static String symbolGlyph(int index) {
        if (index < 0 || index >= SkymapPanel.BODY_COUNT) {
            return "";
        }
        String g = SkymapPanel.namedGlyph(index);
        for (int i = 0; i < g.length(); i++) {
            if (g.charAt(i) > 127) {
                return g;
            }
        }
        return "";
    }

    /** The same rule, by name, for the surfaces that have a name rather than an index. */
    private static String glyphOf(String bodyName) {
        return symbolGlyph(Bodies.indexOfName(bodyName));
    }

    /**
     * The minor aspects, shortlisted by tightness - the blueprint's "underlying friction".
     *
     * <b>Every number on these cards comes off {@link com.zodiacomputing.ourania.astro.Aspects.Hit},
     * not out of this method.</b> The orb, how far off exact it is, whether it is applying,
     * whether it is partile or dissociate are all computed by the engine that finds the aspect
     * in the first place. A panel that recomputed any of them would be the view-and-engine
     * disagreement that this file's own comments record twice - once for MacroAnalyzer, once
     * for tension and release - and it would disagree silently.
     *
     * <b>Minor aspects specifically, and that is the whole reason this section earns a place.</b>
     * The majors are already on the wheel, in the grid, and on every body card as a count. The
     * app carries 1,311 minor-aspect readings across three files that a reader could previously
     * reach only by opening a body and scrolling to its aspect list. This is the surface for
     * them.
     */
    private void appendFriction(StringBuilder html,
                                com.zodiacomputing.ourania.astro.ChartFrame frame) {
        java.util.List<com.zodiacomputing.ourania.astro.Aspects.Hit> minors =
            new java.util.ArrayList<>();
        for (com.zodiacomputing.ourania.astro.Aspects.Hit h
                : com.zodiacomputing.ourania.astro.Aspects.betweenBodies(frame)) {
            if (h.type != null && h.type.isMinor()) {
                minors.add(h);
            }
        }
        if (minors.isEmpty()) {
            return;
        }
        // Tightest first. tightness is 1.0 at exact and 0.0 at the edge of orb, so it already
        // normalises across aspects whose orbs differ - which offBy alone would not.
        minors.sort((x, y) -> Double.compare(y.tightness, x.tightness));

        int show = Math.min(minors.size(), FRICTION_CARDS);
        sectionHeading(html, ACCENT_FRICTION, "UNDERLYING FRICTION",
            minors.size() == show
                ? (minors.size() == 1 ? "1 minor aspect" : minors.size() + " minor aspects")
                : "tightest " + show + " of " + minors.size());

        InterpretationService svc = InterpretationService.getInstance();
        for (int i = 0; i < show; i++) {
            com.zodiacomputing.ourania.astro.Aspects.Hit h = minors.get(i);
            cardOpen(html, ACCENT_FRICTION, null);

            StringBuilder title = new StringBuilder();
            String ga = glyphOf(h.a);
            String gb = glyphOf(h.b);
            if (!ga.isEmpty() && !gb.isEmpty()) {
                glyph(title, ga, 14, null);
                title.append(" ").append(h.a).append(" ").append(h.type.label.toLowerCase())
                     .append(" ");
                glyph(title, gb, 14, null);
                title.append(" ").append(h.b);
            } else {
                title.append(h.a).append(" ").append(h.type.label.toLowerCase())
                     .append(" ").append(h.b);
            }
            cardTitle(html, null, title.toString(), h.applying ? "applying" : "separating");

            StringBuilder meta = new StringBuilder();
            meta.append(orbText(h.offBy)).append(" off exact &nbsp;|&nbsp; ")
                .append(trimAngle(h.type.exactAngle)).append("&deg; aspect");
            if (h.partile) {
                meta.append(" &nbsp;|&nbsp; <b>partile</b>");
            }
            if (h.dissociate) {
                meta.append(" &nbsp;|&nbsp; dissociate");
            }
            cardMeta(html, meta.toString());
            cardRule(html);

            cardBlock(html, "#C9D1D9", "THE RECURRING SNAG",
                leadOrNull(svc.getAspect(h.a, h.b, h.type.label)));
            cardLink(html, "aspecttype|" + h.type.label, "How the "
                + h.type.label.toLowerCase() + " is measured");
            cardClose(html);
        }

        if (minors.size() > show) {
            html.append("<div style='color:").append(CARD_META)
                .append("; font-size:11px; margin:-4px 0 12px 0;'>")
                .append(minors.size() - show)
                .append(" more minor aspects are in the aspect grid.</div>");
        }
    }

    /**
     * What an aspect or transit entry actually MEANS, rather than what it is called.
     *
     * <b>{@link #leadOrNull} is wrong for these and reads as an echo.</b> The data pipeline
     * gives aspect and transit values the shape
     * {@code <b>title</b><br><i>summary</i><br><br>text}, so the bold lead is a restatement of
     * the query - "Transiting Saturn Square Natal Saturn" - while the finding is the italic
     * line beneath it. A forecast card that prints the title under the heading "what it does"
     * has told the reader the name of the thing they just clicked.
     *
     * Preference: the italic summary, then the body text after the blank line, then whatever
     * is left that is not the title. Null only when there is genuinely nothing.
     */
    private static String aspectMeaning(String entry) {
        if (entry == null || entry.isEmpty()
                || entry.startsWith("Interpretation not found")
                || entry.startsWith("Transit aspect interpretation not found")) {
            return null;
        }
        int io = entry.indexOf("<i>");
        int ic = entry.indexOf("</i>", io + 3);
        if (io >= 0 && ic > io) {
            String summary = entry.substring(io + 3, ic).trim();
            // <b>A parenthetical italic is a label, not a finding.</b> Where getTransitAspect
            // has no specific entry it falls back to the natal aspect's prose behind
            // "<i>(Transit triggering natal aspect meaning)</i><br>", and that parenthetical
            // is what a reader would have been shown - the real text sits after it, behind a
            // SINGLE br, which is why the blank-line rule below does not reach it.
            //
            // Safe because it is measured: <b>0 of the 677 italic summaries in the transits
            // section are parenthetical</b>, so this cannot swallow a real one.
            if (!summary.isEmpty() && !summary.startsWith("(")) {
                return summary;
            }
            String after = entry.substring(ic + 4).replaceFirst("^(?:<br>)+", "").trim();
            if (!after.isEmpty()) {
                return after;
            }
        }
        int gap = entry.indexOf("<br><br>");
        if (gap >= 0) {
            String body = entry.substring(gap + 8).trim();
            if (!body.isEmpty()) {
                return body;
            }
        }
        // No summary and no body: this entry is a title and nothing else, and saying nothing
        // is better than saying the title back.
        return SkymapPanel.boldLead(entry) == null ? entry : null;
    }

    /** The lead sentence of a macro-dynamics entry, or null when the data has no such key. */
    private static String macroLead(String key) {
        if (key == null) {
            return null;
        }
        return leadOrNull(InterpretationService.getInstance().getMacroDynamic(key));
    }

    /**
     * The lead sentence of a reading, or null when there is nothing honest to show.
     *
     * Distinct from {@link #firstSentence} because that one faithfully returns whatever it was
     * given, sentinel included. A card needs the opposite: absent rather than apologetic.
     */
    private static String leadOrNull(String prose) {
        if (prose == null || prose.isEmpty()
                || prose.startsWith("Interpretation not found")
                || prose.startsWith("Sabian symbol not found")
                || prose.startsWith("Transit aspect interpretation not found")) {
            return null;
        }
        String lead = firstSentence(prose);
        return lead == null || lead.isEmpty() ? null : lead;
    }

    /**
     * One body, as a card: where it is, what it most nearly means, and the way in.
     *
     * <b>This does not replace {@link #generatePlanetHtml} - it summarises what that renders
     * in full.</b> The two are wired to the same body through the {@code body|index} link, so
     * the sign, decan, Sabian, degree, tarot, house and aspect material the long form carries
     * is one click away rather than deleted. Making the long form itself shorter would have
     * taken the depth out of the detail view as well, since both surfaces call it.
     */
    private String generateBodyCard(int index, double lon, double[] cusps, boolean isTransit) {
        Bodies.Def def = Bodies.at(index);
        int signIdx = com.zodiacomputing.ourania.astro.Zodiac.signIndex(lon);
        int degree = (int) (lon % 30.0) + 1;
        int decanNum = (int) (lon % 30.0 / 10.0) + 1;
        int houseNum = com.zodiacomputing.ourania.astro.Zodiac.houseOf(lon, cusps);
        String signName = SkymapPanel.SIGN_NAMES[signIdx];

        StringBuilder html = new StringBuilder();
        // No kicker: updateInterpretations prints the group as a heading above each run of
        // these, and repeating it inside every card is the metadata noise the critique
        // objected to rather than the hierarchy it asked for.
        cardOpen(html, ACCENT_BODY, null);

        StringBuilder title = new StringBuilder();
        title.append(def.name).append(" in ").append(signName);
        cardTitle(html, symbolGlyph(index), title.toString(),
            isTransit ? "transiting" : null);

        StringBuilder meta = new StringBuilder();
        meta.append(com.zodiacomputing.ourania.astro.Zodiac.format(lon)).append(" ");
        glyph(meta, SkymapPanel.ZODIAC_SYMBOLS[signIdx], 12, null);
        // Both decan rulers, named as roles rather than as a discrepancy - the same treatment
        // the reading and the decan page give them, so a reader meets one scheme story on all
        // three surfaces instead of a table on one and a footnote on another.
        meta.append(" &nbsp;|&nbsp; decan ").append(decanNum);
        String trip = com.zodiacomputing.ourania.astro.Zodiac
            .triplicityDecanRuler(signName, decanNum);
        String face = com.zodiacomputing.ourania.astro.Zodiac
            .chaldeanDecanRuler(signName, decanNum);
        if (!trip.isEmpty() && !face.isEmpty()) {
            meta.append(trip.equals(face)
                ? " <span style='color:" + CARD_KICKER + ";'>(" + trip + ")</span>"
                : " <span style='color:" + CARD_KICKER + ";'>(sub " + trip
                  + " &middot; face " + face + ")</span>");
        }
        if (!def.isAngle()) {
            meta.append(" &nbsp;|&nbsp; house ").append(houseNum);
        }
        cardMeta(html, meta.toString());

        // A composite point whose two sources are near opposition is a coin flip, and the
        // card says so rather than presenting it as settled. Silent for every other chart.
        com.zodiacomputing.ourania.astro.ChartFrame cf =
            skymapPanel == null ? null : skymapPanel.getCurrentChart();
        com.zodiacomputing.ourania.astro.ChartFrame.Body cb =
            cf == null ? null : cf.body(def.name);
        if (cb != null && cb.unstableMidpoint) {
            cardBlock(html, ACCENT_FRICTION, "UNSTABLE",
                "<span style='font-size:11px;'>The two charts put this point almost exactly "
                + "opposite each other, so the midpoint sits between two arcs of nearly equal "
                + "length and a few minutes of birth time flips it to the other side of the "
                + "chart. The degree above is the arithmetic, not a finding.</span>");
        }
        cardRule(html);

        InterpretationService svc = InterpretationService.getInstance();
        String lead = isTransit
            ? leadOrNull(svc.getTransit(def.name, signName))
            : leadOrNull(def.isAngle() ? svc.getAngleInterpretation(def.name, signName)
                                       : svc.getPlanetInSign(def.name, signName));
        if (lead == null) {
            // No written reading for this point yet. The registry's one-liner is what the
            // point IS, which is honest and is not the same claim as a reading of it.
            cardBlock(html, "#C9D1D9", null,
                "<span style='color:" + CARD_META + ";'><i>" + def.meaning + "</i></span>");
        } else {
            cardBlock(html, "#C9D1D9", "IN " + signName.toUpperCase(), lead);
        }

        String sabian = leadOrNull(svc.getSabianSymbol(signName, degree));
        if (sabian != null) {
            cardBlock(html, "#FFD166", "SABIAN " + signName.toUpperCase() + " " + degree,
                "<i>\"" + sabian + "\"</i>");
        }

        int aspectCount = 0;
        if (skymapPanel != null) {
            java.util.List<String[]> active = skymapPanel.getActiveAspectsFor(index, isTransit);
            aspectCount = active == null ? 0 : active.size();
        }
        if (aspectCount > 0) {
            cardMeta(html, aspectCount + (aspectCount == 1 ? " active aspect" : " active aspects"));
        }

        cardLinks(html, new String[][] {
            {"body|" + index, "Read in full"},
            {"sign|" + signName, signName},
            {"decan|" + signName + "|" + decanNum, "Decan " + decanNum},
            {"sabian|" + signName + "|" + degree, "Sabian"}
        });
        cardClose(html);
        return html.toString();
    }

    public void updateInterpretations() {
        if (skymapPanel == null) return;
        
        java.time.ZonedDateTime time = skymapPanel.getChartTime();
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 14px;'>");

        // The masthead. A ZonedDateTime.toString() - "2026-08-25T13:42:07.481-07:00[America/
        // Los_Angeles]" - is a machine's idea of a moment and was the first thing a reader
        // met on this screen. The same instant, formatted for a person, costs one formatter.
        html.append("<div style='color:#FFFFFF; font-size:17px;'><b>Full Interpretation</b></div>");
        html.append("<div style='color:").append(CARD_KICKER)
            .append("; font-size:11px;'>")
            .append(escape(time.format(java.time.format.DateTimeFormatter
                .ofPattern("d MMM yyyy, HH:mm"))))
            .append(" &middot; ")
            .append(escape(time.getZone().getId().replace('_', ' ')))
            .append("</div>");
        html.append("<div style='font-size:11px; margin:8px 0 14px 0;'>"
            + "<a href='index' style='color:" + CARD_LINK + ";'>"
            + "Browse everything the app knows &rarr;</a></div>");

        boolean isTransit = skymapPanel.showTransitChart;
        double[] lonArray = isTransit ? skymapPanel.tLon : skymapPanel.bLon;
        boolean[] validArray = isTransit ? skymapPanel.tValid : skymapPanel.bValid;
        double[] cusps = skymapPanel.activeCusps;

        // Macro dynamics come from Gestalt, and the geometries from AspectPatterns.
        //
        // <b>They used to come from MacroAnalyzer and PatternDetector, which computed the same
        // things a second time and got different answers.</b> Measured over 200 random charts
        // before this was consolidated on 2026-08-23: the signature sign disagreed on 74% of
        // them, the Jones shape on 46%, the dominant element on 34%. The cause was that the
        // second implementation counted bodies as unweighted integers while Gestalt uses the
        // documented 14-point table, so a chart showed one signature here and another in the
        // narrative. Gestalt is the version five check suites assert against.
        com.zodiacomputing.ourania.astro.ChartFrame frame =
            skymapPanel == null ? null : skymapPanel.getCurrentChart();
        if (!isTransit && frame != null) {
            com.zodiacomputing.ourania.astro.Gestalt.Result g =
                com.zodiacomputing.ourania.astro.Gestalt.compute(frame);

            // Aspect patterns first, because a closed circuit outranks anything else in the
            // chart: it is several placements behaving as one unit, and a transit to any
            // member fires all of them. Nothing here when the chart has none - which is now
            // most charts, since patterns were restricted to physical bodies at a tight orb.
            if (!g.aspectPatterns.isEmpty()) {
                sectionHeading(html, ACCENT_PATTERN, "ASPECT PATTERNS",
                    g.aspectPatterns.size() + (g.aspectPatterns.size() == 1
                        ? " closed circuit" : " closed circuits"));
                String mechanics = macroLead("pattern_mechanics");
                if (mechanics != null) {
                    html.append("<div style='color:").append(CARD_META)
                        .append("; font-size:11px; margin:0 0 10px 0;'>")
                        .append(mechanics).append("</div>");
                }
                for (com.zodiacomputing.ourania.astro.AspectPatterns.Pattern ap
                        : g.aspectPatterns) {
                    cardOpen(html, ACCENT_PATTERN, null);

                    String qualifier = null;
                    if (ap.modality != null
                        && (ap.name.equals("T-square") || ap.name.equals("Grand cross"))) {
                        qualifier = capitalise(ap.modality);
                    } else if (ap.element != null
                        && (ap.name.equals("Grand trine") || ap.name.equals("Kite"))) {
                        qualifier = capitalise(ap.element);
                    }
                    cardTitle(html, null, ap.name, qualifier);

                    StringBuilder meta = new StringBuilder();
                    meta.append(String.join(" &middot; ", ap.bodies));
                    if (ap.apex != null) {
                        meta.append(" &nbsp;|&nbsp; apex <b>").append(ap.apex).append("</b>");
                    }
                    meta.append(" &nbsp;|&nbsp; widest leg ")
                        .append(String.format("%.2f", ap.widestOrb)).append("&deg;");
                    cardMeta(html, meta.toString());
                    cardRule(html);

                    // <b>The lead only - the three paragraphs live on the figure's own page.</b>
                    // detailKey, variantKey and Antigravity's entry under the plain name were
                    // all printed here in full, one after another, which is up to four
                    // paragraphs per figure before the reader has reached a single planet.
                    // The preference order is unchanged; only how much of it lands here is.
                    String lead = macroLead(ap.detailKey());
                    if (lead == null) {
                        lead = macroLead(ap.variantKey());
                    }
                    if (lead == null) {
                        lead = macroLead(ap.name);
                    }
                    cardBlock(html, "#C9D1D9", "HOW IT BEHAVES", lead);
                    if (ap.isDissociate()) {
                        cardBlock(html, ACCENT_PATTERN, "DISSOCIATE",
                            macroLead("pattern_dissociate"));
                    }
                    for (com.zodiacomputing.ourania.astro.TensionRelease.Release rel
                            : g.releases) {
                        if (rel.source.equals(ap.name)
                            && rel.bodies.equals(ap.bodies)) {
                            cardBlock(html, "#8FD694", "RELEASE", rel.sentence);
                        }
                    }
                    cardLink(html, patternHref(ap.name, ap.bodies), "The figure in full");
                    cardClose(html);
                }
            }

            // Friction sits next to the patterns because both are what the geometry is doing,
            // and a reader who has just been told about a closed circuit is in the right frame
            // to be told what is quietly grinding underneath it.
            appendFriction(html, frame);

            // The Moon's lunar mansion. Its own block, and framed as electional rather than
            // natal, because that is what the Western tradition uses the 28 stations for -
            // choosing a moment, not describing a character. Printing it beside the natal
            // prose without that frame would be the app quietly making a claim the sources
            // do not.
            com.zodiacomputing.ourania.astro.LunarMansions.Mansion mansion =
                com.zodiacomputing.ourania.astro.LunarMansions.ofMoon(frame);
            if (mansion != null) {
                sectionHeading(html, ACCENT_MANSION, "THE MOON'S MANSION", null);
                cardOpen(html, ACCENT_MANSION, null);
                // indexOf returns -1 for an id the registry does not carry, and symbolGlyph
                // takes that as "no glyph" rather than indexing an array with it. The Moon is
                // not going anywhere, but a panel that throws is worse than a bare card.
                cardTitle(html, symbolGlyph(Bodies.indexOf("moon")),
                    mansion.number + ". " + mansion.name,
                    mansion.alsoKnownAs == null ? null : "(" + mansion.alsoKnownAs + ")");

                StringBuilder meta = new StringBuilder();
                meta.append(mansion.translation).append(" &nbsp;|&nbsp; ")
                    .append(com.zodiacomputing.ourania.astro.Zodiac.format(mansion.start))
                    .append(" to ")
                    .append(com.zodiacomputing.ourania.astro.Zodiac.format(
                        mansion.end() >= 360.0 ? 0.0 : mansion.end()));
                cardMeta(html, meta.toString());
                cardRule(html);

                if (!mansion.goodFor.isEmpty()) {
                    cardBlock(html, "#8FD694", "ELECTED FOR", mansion.goodFor + ".");
                }
                if (!mansion.avoid.isEmpty()) {
                    cardBlock(html, "#D08A8A", "AVOID", mansion.avoid + ".");
                }

                // The provenance stays on the card rather than moving behind the link. It is
                // not detail - it is the frame that stops the card being read as a natal
                // claim, and a frame is worthless on the page the reader did not open.
                cardBlock(html, CARD_KICKER, "THE TRADITION",
                    "<span style='color:" + CARD_META + "; font-size:11px;'>28 equal stations "
                    + "of 12&deg;51'26\" from 0&deg; Aries, after al-Biruni - the Moon's daily "
                    + "travel rather than the Sun's yearly. Read for electional and horary "
                    + "work, choosing a moment, and not for natal character.</span>");

                cardLinks(html, new String[][] {
                    {mansionHref(mansion.number), "This mansion in full"},
                    {"mansions", "All 28"}
                });
                cardClose(html);
            }

            sectionHeading(html, ACCENT_MACRO, "THE CHART AS A WHOLE", null);

            cardOpen(html, ACCENT_MACRO, "MACRO DYNAMICS");
            cardTitle(html, null, g.signatureSign == null
                ? (g.signatureTied ? "No single signature" : "Signature not determined")
                : capitalise(g.signatureSign) + " signature", null);
            cardMeta(html, g.signatureSign == null && g.signatureTied
                ? "the top element or modality is tied" : null);
            cardRule(html);
            cardBlock(html, "#C9D1D9", "ELEMENTS", weights(g.elements));
            cardBlock(html, "#C9D1D9", "MODALITIES", weights(g.modalities));

            StringBuilder emphasis = new StringBuilder();
            emphasis.append(g.hemisphereEmphasis == null
                ? "No decisive hemisphere emphasis."
                : "Weighted " + g.hemisphereEmphasis + ".").append("<br>");
            emphasis.append(g.quadrantEmphasis == null
                ? "No single quadrant dominates."
                : "Concentrated in the " + g.quadrantEmphasis + " quadrant.").append("<br>");
            emphasis.append(g.houseModeEmphasis == null
                ? "Angular, succedent and cadent houses are evenly held."
                : "Mostly " + g.houseModeEmphasis + " houses.");
            if (g.trinityEmphasis != null) {
                emphasis.append("<br>Weighted toward the houses of ")
                    .append(g.trinityEmphasis).append(".");
            }
            cardBlock(html, "#C9D1D9", "EMPHASIS", emphasis.toString());

            if (!g.singletons.isEmpty()) {
                cardBlock(html, "#FFD166", "SINGLETONS", String.join("<br>", g.singletons));
            }
            cardBlock(html, CARD_KICKER, null,
                "<span style='color:" + CARD_META + "; font-size:11px;'>Weighted on the "
                + "14-point table, not a headcount: the Sun, Moon and Ascendant carry 3 each "
                + "and the chart ruler takes a bonus, so these are not whole numbers.</span>");
            cardClose(html);

            // Global geometries. <b>The aspect-pattern list that used to sit in this block is
            // gone.</b> It walked the same g.aspectPatterns already rendered as cards at the
            // top of this screen and printed name, bodies, apex and macro text a second time -
            // so every figure in the chart appeared twice, a few hundred pixels apart, and the
            // second copy had no link on it. The cards are the surviving copy.
            boolean haveGeometry = g.shape != null || !g.stelliums.isEmpty()
                || !g.releases.isEmpty();
            if (haveGeometry) {
                cardOpen(html, ACCENT_MACRO, "GLOBAL GEOMETRIES");
                if (g.shape != null) {
                    String shapeName = capitalise(g.shape.name().toLowerCase());
                    cardTitle(html, null, shapeName, "Jones pattern");
                    if (g.shapeHandle != null) {
                        cardMeta(html, shapeName + " with " + g.shapeHandle + " as its "
                            + (g.shape == com.zodiacomputing.ourania.astro.Gestalt.Shape.BUCKET
                                ? "handle." : "leading planet."));
                    }
                    cardRule(html);
                    cardBlock(html, "#C9D1D9", null, macroLead(shapeName));
                }

                for (String s : g.stelliums) {
                    cardBlock(html, "#FFD166", "STELLIUM", s);
                }
                if (!g.stelliums.isEmpty()) {
                    String stelDesc = macroLead("Stellium");
                    if (stelDesc != null) {
                        cardBlock(html, CARD_KICKER, null,
                            "<span style='color:" + CARD_META + "; font-size:11px;'><i>"
                            + stelDesc + "</i></span>");
                    }
                }

                // Tension and release - framework item 6.3.
                //
                // <b>Driven from g.releases rather than re-derived here.</b> The panel and the
                // engine reading the same chart differently is the defect that removed
                // MacroAnalyzer, and a release clause computed in the view would be the same
                // mistake one level down.
                if (!g.releases.isEmpty()) {
                    StringBuilder rels = new StringBuilder();
                    for (com.zodiacomputing.ourania.astro.TensionRelease.Release rel
                            : g.releases) {
                        if (rels.length() > 0) {
                            rels.append("<br>");
                        }
                        rels.append(rel.sentence);
                        if (rel.definitionalSpine) {
                            rels.append(" <span style='color:").append(CARD_META)
                                .append(";'>(definitional axis)</span>");
                        }
                    }
                    cardBlock(html, "#8FD694", "TENSION AND RELEASE", rels.toString());
                    cardBlock(html, CARD_KICKER, null,
                        "<span style='color:" + CARD_META + "; font-size:11px;'>Release means "
                        + "a trine or a sextile reaching the configuration from outside it, at "
                        + "the orbs the rest of the chart uses; the empty leg is the degree "
                        + "opposite the apex. In a chart of 29 points rather than ten, that "
                        + "leg is occupied about half the time.</span>");
                }
                cardClose(html);
            }
        }

        // The placements, one card each, grouped the way the registry groups them.
        //
        // <b>This loop used to inline generatePlanetHtml for all twenty-nine points.</b> That
        // is the single scrolling document the critique was about: every body contributed its
        // core paragraph, sign prose, decan, Sabian symbol with shadow and keywords, the
        // 360-degree text, three tarot lines, house prose and one heading per active aspect,
        // separated by a horizontal rule. On a full registry that is several hundred
        // paragraphs, and the reader who wanted to know where the Moon was had to scroll past
        // all of it.
        //
        // <b>The long form is not deleted, it is one click away.</b> Each card links to
        // body|index, which reaches generatePlanetHtml through openBody with every one of
        // those sections intact - so the depth still exists, and only the default changed.
        //
        // The hand-rolled house search that used to sit here is gone too - it was a second
        // implementation of Zodiac.houseOf living in a view. <b>It was not producing wrong
        // houses.</b> Measured on 200,000 random longitudes against 200,000 random
        // well-formed quadrant cusp sets, the two agreed every single time, including across
        // the 12-to-1 wrap I expected to separate them. It is removed because one rule in two
        // places is how they drift, not because this pair had drifted yet.
        int shown = 0;
        com.zodiacomputing.ourania.astro.Bodies.Group group = null;
        for (int i = 0; i < SkymapPanel.BODY_COUNT; i++) {
            if (!validArray[i]) {
                continue;
            }
            com.zodiacomputing.ourania.astro.Bodies.Def def =
                com.zodiacomputing.ourania.astro.Bodies.at(i);
            if (def.group != group) {
                group = def.group;
                sectionHeading(html, ACCENT_BODY, group.name(), null);
            }
            html.append(generateBodyCard(i, lonArray[i], cusps, isTransit));
            shown++;
        }
        if (shown == 0) {
            html.append("<div style='color:").append(CARD_META)
                .append("; font-size:12px;'>No points are switched on for this chart. "
                    + "Turn some on in the body settings.</div>");
        }
        
        html.append("</body></html>");
        setHtml(html.toString(), false); // Scroll to top
    }
}
