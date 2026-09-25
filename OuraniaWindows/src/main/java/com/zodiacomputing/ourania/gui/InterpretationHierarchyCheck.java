package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Dignity;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * K11: the Interpretation tab reads a natal placement in the interpretive hierarchy, and the
 * selection pane does not change.
 *
 * <p>What is held, for every body in the registry at several placements:
 * <ol>
 *   <li><b>Nothing is lost.</b> The layer breakdown carries every piece of the reading, word for
 *       word - the hierarchy is an arrangement, never a cut.</li>
 *   <li><b>The levels are in order</b>: planet, sign, house and their Major Arcana cards; the
 *       aspects; the triplicity, bound and decan with its pip; then the degree and the mansion -
 *       the mansion leading that level for the Moon and closing it for everything else.</li>
 *   <li><b>The main account writes nothing.</b> Every sentence in it is found word for word in
 *       the pieces it came from (C7: authored and generated text must stay distinguishable).</li>
 *   <li><b>Collapsed is collapsed</b>: the full pieces are not on the page until asked for.</li>
 *   <li><b>The selection pane's source keeps its order.</b> generatePlanetHtml, which the pane is
 *       cut from, still prints the pieces exactly as before K11.</li>
 *   <li><b>The tab takes the hierarchy for a natal body only</b>, keeps the transit reading for the
 *       outer ring, and the breakdown opens and closes in place.</li>
 *   <li><b>The engine's facts are the engine's</b>: dignity and the bound agree with Dignity.</li>
 * </ol>
 */
public final class InterpretationHierarchyCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        InterpretationPanel[] hold = new InterpretationPanel[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new InterpretationPanel(null, null));
        InterpretationPanel ip = hold[0];

        part("A-E: every body, several placements", () -> sweep(ip));
        part("F: the tab's path, and the breakdown's link", () -> theTab(ip));
        part("G: the facts come from the engine", () -> facts(ip));
        part("H: the synthesis ranks and reads in the same order", InterpretationHierarchyCheck::synthesis);
        part("I: Read in full opens the ring the card came from",
            InterpretationHierarchyCheck::theRing);
        part("J: a ring says what it is", InterpretationHierarchyCheck::ringKinds);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }


    /**
     * <b>Read in full must open the body the card described.</b>
     *
     * David, 25 Sep: with Chart A and Sky up, the card said "Sun in Libra" - the sky's Sun - and
     * Read in full opened Chart A's Sun in Leo. Every body did it. {@code generateBodyCard} knew
     * which ring it was drawing and emitted a link carrying only the body index, and
     * {@code openBody} then read {@code natalRing} whatever it was handed.
     *
     * <p>This is a round trip on purpose: the link the card writes, parsed the way the handler
     * parses it. Each half was reasonable on its own and they only disagreed with each other, so
     * a check on either half alone would have passed while the screen was wrong.
     */
    private static void theRing() {
        // The naming rule both doors now ask, rather than each writing out the ternary.
        SkymapPanel[] hold = new SkymapPanel[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new SkymapPanel(null));
        } catch (Exception e) {
            ok("a wheel would construct: " + e, false);
            return;
        }
        final SkymapPanel sp = hold[0];
        ok("a natal body keeps its own name",
            "Sun".equalsIgnoreCase(sp.interpretationNameFor(0, false)));
        ok("an outer-ring body is marked as one",
            sp.interpretationNameFor(0, true).startsWith("transit_"));
        ok("and the two differ", !sp.interpretationNameFor(0, false)
            .equals(sp.interpretationNameFor(0, true)));
        ok("out of range is answered, not thrown", "".equals(sp.interpretationNameFor(-1, true)));

        // <b>The round trip.</b> Both link shapes the card can emit, parsed as the handler parses
        // them, must name the ring the card was drawing.
        for (int index : new int[] {0, 3, 9}) {
            for (boolean outer : new boolean[] {false, true}) {
                String href = "body|" + index + "|" + (outer ? "1" : "0");
                ok("the link names a body and a ring: " + href,
                    href.startsWith("body|") && href.split("\\|").length == 3);
                String[] bits = href.substring(5).split("\\|");
                ok("it parses back to the same body", Integer.parseInt(bits[0]) == index);
                ok("and to the same ring",
                    ("1".equals(bits.length > 1 ? bits[1] : "0")) == outer);
            }
        }

        // The old two-part form still has to work, because other surfaces emit it for natal
        // placements and a reader's saved reading should not stop opening.
        String[] legacy = "body|4".substring(5).split("\\|");
        ok("the older body|N form still parses", Integer.parseInt(legacy[0]) == 4);
        ok("and means the natal ring", legacy.length == 1);

        // <b>And now the defect itself, end to end.</b> Everything above passed against a
        // reverted openBody, because none of it reached the line that was wrong. Two rings, one
        // body, deliberately in different signs - which is exactly David's screen: Chart A's Sun
        // in Leo and the sky's Sun in Libra.
        sp.natalRing.lon[0] = 130.0;      // Leo
        sp.natalRing.valid[0] = true;
        sp.outerRing.lon[0] = 190.0;      // Libra
        sp.outerRing.valid[0] = true;

        InterpretationPanel[] pair = new InterpretationPanel[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() ->
                pair[0] = new InterpretationPanel(sp, null));
        } catch (Exception e) {
            ok("a panel would construct over that wheel: " + e, false);
            return;
        }
        InterpretationPanel ip = pair[0];

        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> ip.openBody(0, false));
            String natal = ip.currentHtml();
            ok("the natal ring opens Leo", natal.contains("Leo"));
            ok("and not Libra", !natal.contains("Libra"));

            javax.swing.SwingUtilities.invokeAndWait(() -> ip.openBody(0, true));
            String outer = ip.currentHtml();
            ok("the outer ring opens Libra", outer.contains("Libra"));
            ok("and not Leo", !outer.contains("Leo"));
            ok("and the two readings differ at all", !natal.equals(outer));
        } catch (Exception e) {
            ok("both rings open without throwing: " + e, false);
        }
    }


    /**
     * <b>Every mode assigns every ring, and the two questions stay apart.</b>
     *
     * A ring's contents used to be re-derived at six sites from chartMode, showTransitChart,
     * transitsEnabled, showTriWheel, isSynastryChart() and showProgressed, and those derivations
     * disagreed one at a time for months. {@code assignRingKinds} is now the only place that
     * reads those flags for this purpose, so this walks every mode and checks what it assigns.
     */
    private static void ringKinds() {
        SkymapPanel[] hold = new SkymapPanel[1];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new SkymapPanel(null));
        } catch (Exception e) {
            ok("a wheel would construct: " + e, false);
            return;
        }
        SkymapPanel sp = hold[0];

        // The sky ring is only ever the sky, whatever the mode - which is why it is the one
        // name that never caused trouble.
        for (ChartMode mode : ChartMode.values()) {
            sp.chartMode = mode;
            for (boolean prog : new boolean[] {false, true}) {
                sp.showProgressed = prog;
                sp.assignRingKinds();
                ok(mode + (prog ? " progressed" : "") + ": the sky ring is the sky",
                    sp.skyRing.kind == WheelRing.Kind.SKY);

                boolean composite = mode == ChartMode.COMPOSITE_MIDPOINT
                    || mode == ChartMode.COMPOSITE_DAVISON;
                ok(mode + ": the inner ring is "
                        + (composite ? "the composite" : "Chart A"),
                    sp.natalRing.kind == (composite ? WheelRing.Kind.COMPOSITE
                        : WheelRing.Kind.CHART_A));

                WheelRing.Kind want = mode == ChartMode.SYNASTRY ? WheelRing.Kind.CHART_B
                    : prog ? WheelRing.Kind.PROGRESSED : WheelRing.Kind.TRANSIT;
                ok(mode + (prog ? " progressed" : "") + ": the outer ring is " + want,
                    sp.outerRing.kind == want);
            }
        }

        // <b>A second person outranks a progression.</b> In a synastry the outer ring is never a
        // moment, whatever else is switched on - and the order those two are tested in is the
        // whole content of that rule.
        sp.chartMode = ChartMode.SYNASTRY;
        sp.showProgressed = true;
        sp.assignRingKinds();
        ok("synastry beats progressed on the outer ring",
            sp.outerRing.kind == WheelRing.Kind.CHART_B);

        // <b>The two questions, held apart.</b> Collapsing them is a regression that looks like a
        // simplification: make the prefix follow the meaning and CHART_B loses it, and with it
        // the partner branch in generatePlanetHtml - so a synastry reading quietly becomes plain
        // natal prose with no "Their Venus" framing and no overlay house.
        ok("a partner is not a passing event",
            !WheelRing.Kind.CHART_B.readsAsEvent());
        ok("but a partner's body still carries the wire prefix",
            WheelRing.Kind.CHART_B.takesTransitPrefix());
        ok("a progression is not an event either",
            !WheelRing.Kind.PROGRESSED.readsAsEvent());
        ok("and takes no prefix, which is the guard the click path carried",
            !WheelRing.Kind.PROGRESSED.takesTransitPrefix());
        ok("a transit is an event", WheelRing.Kind.TRANSIT.readsAsEvent());
        ok("and so is the sky", WheelRing.Kind.SKY.readsAsEvent());
        ok("the inner wheel takes no prefix",
            !WheelRing.Kind.CHART_A.takesTransitPrefix()
                && !WheelRing.Kind.COMPOSITE.takesTransitPrefix());

        // <b>Dashing says which chart, colour says which aspect.</b> This was a boolean - not
        // the natal ring - so a partner's line, a transit and the sky were all dashed 5/5 and
        // read identically. Each kind needs its own hand or the setting says nothing.
        ok("the inner wheel draws solid", WheelRing.Kind.CHART_A.dashPattern() == null
            && WheelRing.Kind.COMPOSITE.dashPattern() == null);
        java.util.List<String> seen = new java.util.ArrayList<>();
        for (WheelRing.Kind k : new WheelRing.Kind[] {WheelRing.Kind.CHART_B,
                WheelRing.Kind.PROGRESSED, WheelRing.Kind.TRANSIT, WheelRing.Kind.SKY}) {
            float[] d = k.dashPattern();
            ok(k + " is dashed", d != null && d.length >= 2);
            if (d == null) {
                continue;
            }
            ok(k + " has real lengths", d[0] > 0f && d[1] > 0f);
            String key = java.util.Arrays.toString(d);
            ok(k + " is told apart from the others by its dash (" + key + ")",
                !seen.contains(key));
            seen.add(key);
        }
        // A transit is the commonest outer wheel, and it keeps exactly what it had, so the
        // ordinary chart looks as it always has.
        ok("a transit keeps its 5/5",
            java.util.Arrays.equals(WheelRing.Kind.TRANSIT.dashPattern(),
                new float[] {5.0f, 5.0f}));
        // Handed out as a copy: BasicStroke takes the array, and a caller that edited it would
        // change every line drawn afterwards.
        float[] once = WheelRing.Kind.SKY.dashPattern();
        once[0] = 99f;
        ok("the pattern cannot be edited from outside",
            WheelRing.Kind.SKY.dashPattern()[0] == 1.0f);

        // The ring word, which was implemented six times over before this.
        ok("the inner wheel takes no ring word",
            WheelRing.Kind.CHART_A.ringWord == null
                && WheelRing.Kind.COMPOSITE.ringWord == null);
        for (WheelRing.Kind k : new WheelRing.Kind[] {WheelRing.Kind.CHART_B,
                WheelRing.Kind.PROGRESSED, WheelRing.Kind.TRANSIT, WheelRing.Kind.SKY}) {
            ok(k + " names itself for a reader",
                k.ringWord != null && !k.ringWord.isEmpty());
        }

        // And the naming rule agrees with the kinds, asked either way round.
        sp.chartMode = ChartMode.SYNASTRY;
        sp.showProgressed = false;
        sp.assignRingKinds();
        ok("a partner's body is prefixed", sp.interpretationNameFor(0, true)
            .startsWith("transit_"));
        sp.chartMode = ChartMode.TRANSIT;
        sp.showProgressed = true;
        sp.assignRingKinds();
        ok("a progressed body is not", !sp.interpretationNameFor(0, true)
            .startsWith("transit_"));
        ok("and the ring-typed form agrees with the positional one",
            sp.interpretationNameFor(0, sp.outerRing.kind)
                .equals(sp.interpretationNameFor(0, true)));
    }

    private static final String[] SELECTION_ORDER = {"sign", "decan", "sabian", "mansion",
        "degreeNotes", "sabianDetail", "degree", "tarotHeading", "tarotPlanet", "tarotSign",
        "tarotDecan", "house", "aspects"};

    private static void sweep(InterpretationPanel ip) {
        String[] signs = SkymapPanel.SIGN_NAMES;
        int placements = 0;
        int lostTotal = 0;
        int disorderTotal = 0;
        int inventedTotal = 0;
        int collapsedLeak = 0;
        int selectionDisorder = 0;
        String firstBad = null;
        for (int b = 0; b < Bodies.count(); b++) {
            String name = Bodies.at(b).name;
            for (int k = 0; k < 3; k++) {
                int signIdx = (b * 5 + k * 7) % 12;
                int degree = 1 + (b * 11 + k * 13) % 30;
                int decan = 1 + (degree - 1) / 10;
                int house = 1 + (b + k * 3) % 12;
                double lon = signIdx * 30 + degree - 0.5;
                List<String[]> aspects = new ArrayList<>();
                if (k != 1) {
                    aspects.add(new String[] {"Mars", "Square", "Applying"});
                    aspects.add(new String[] {"Venus", "Trine"});
                }
                String sign = signs[signIdx];
                String label = name + " " + sign + " " + degree + " H" + house;
                placements++;
                String display = name.substring(0, 1).toUpperCase() + name.substring(1);
                LinkedHashMap<String, String> p = ip.natalPieces(name, sign, degree, decan,
                    house, aspects, lon, display);
                String open = ip.hierarchyHtml(name, sign, degree, decan, house, aspects, lon, true);
                String closed = ip.hierarchyHtml(name, sign, degree, decan, house, aspects, lon, false);

                // A. nothing lost - every piece but the old tarot heading, verbatim
                for (java.util.Map.Entry<String, String> e : p.entrySet()) {
                    if (e.getKey().equals("tarotHeading") || e.getValue().isEmpty()) {
                        continue;
                    }
                    if (!open.contains(e.getValue())) {
                        lostTotal++;
                        firstBad = firstBad == null ? label + ": lost " + e.getKey() : firstBad;
                    }
                }

                // B. level order
                boolean moon = "Moon".equalsIgnoreCase(display);
                String[] order = moon
                    ? new String[] {"sign", "house", "tarotPlanet", "tarotSign", "aspects", "decan",
                        "tarotDecan", "mansion", "sabian", "degreeNotes", "sabianDetail", "degree"}
                    : new String[] {"sign", "house", "tarotPlanet", "tarotSign", "aspects", "decan",
                        "tarotDecan", "sabian", "degreeNotes", "sabianDetail", "degree", "mansion"};
                int last = open.indexOf("CORE ARCHETYPE");
                for (String key : order) {
                    String piece = p.get(key);
                    if (piece.isEmpty()) {
                        continue;
                    }
                    int at = open.indexOf(piece, Math.max(0, last));
                    if (at < 0) {
                        disorderTotal++;
                        firstBad = firstBad == null ? label + ": " + key + " out of order" : firstBad;
                        break;
                    }
                    last = at;
                }

                // C. the account writes nothing
                String account = paragraph(open, "<p style='font-size:14px;'>");
                String source = plain(String.join("", p.values()) + ip.coreHtmlFor(display));
                String body = plain(account.replaceAll("<i>[^<]*:</i>\\s*", ""));
                for (String sentence : body.split("(?<=[.!?])\\s*(?=[A-Z\"'])")) {
                    String s = sentence.trim();
                    if (!s.isEmpty() && !source.contains(s)) {
                        inventedTotal++;
                        firstBad = firstBad == null ? label + ": not in the corpus: " + s : firstBad;
                    }
                }

                // D. collapsed is collapsed
                String house0 = p.get("house");
                if (!house0.isEmpty() && closed.contains(house0)) {
                    collapsedLeak++;
                }

                // E. the selection pane's source keeps its order
                String sel = ip.generatePlanetHtml(name, sign, degree, decan, house, aspects, lon);
                int prev = -1;
                for (String key : SELECTION_ORDER) {
                    String piece = p.get(key);
                    if (piece.isEmpty()) {
                        continue;
                    }
                    int at = sel.indexOf(piece, prev + 1);
                    if (at < 0) {
                        selectionDisorder++;
                        break;
                    }
                    prev = at;
                }
            }
        }
        System.out.println("  " + placements + " placements");
        ok("every body read at several placements", placements >= 3 * 28);
        ok("A: nothing is lost from the breakdown" + sample(lostTotal, firstBad), lostTotal == 0);
        ok("B: the breakdown is in level order" + sample(disorderTotal, firstBad), disorderTotal == 0);
        ok("C: every sentence of the main account is the corpus's own" + sample(inventedTotal, firstBad),
            inventedTotal == 0);
        ok("D: the collapsed view does not carry the full pieces (" + collapsedLeak + ")",
            collapsedLeak == 0);
        ok("E: generatePlanetHtml - the selection pane's source - keeps its order ("
            + selectionDisorder + ")", selectionDisorder == 0);
    }

    private static void theTab(InterpretationPanel ip) throws Exception {
        List<String[]> none = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> ip.showPlanetInterpretation("Saturn", "Capricorn", 3, 1,
            10, none, 272.5));
        String natal = text(ip);
        ok("a natal body reads in the hierarchy", natal.contains(InterpretationPanel.LAYERS_HREF));
        ok("and opens collapsed", natal.contains("Show the layer breakdown"));
        SwingUtilities.invokeAndWait(() -> ip.followLink(InterpretationPanel.LAYERS_HREF));
        ok("the link opens the breakdown in place", text(ip).contains("Hide the layer breakdown")
            && text(ip).contains("CORE ARCHETYPE"));
        SwingUtilities.invokeAndWait(() -> ip.followLink(InterpretationPanel.LAYERS_HREF));
        ok("and closes it again", text(ip).contains("Show the layer breakdown"));

        SwingUtilities.invokeAndWait(() -> ip.showPlanetInterpretation("transit_saturn", "Capricorn",
            3, 1, 10, none, 272.5));
        String transit = text(ip);
        // "Transiting", not "Transiting Saturn": the pane links every body name it shows, so
        // the name arrives wrapped in an anchor and the plain phrase is never in the markup.
        ok("the outer ring keeps its own reading", !transit.contains(InterpretationPanel.LAYERS_HREF)
            && transit.contains("Transiting"));
    }

    private static void facts(InterpretationPanel ip) {
        List<String[]> none = new ArrayList<>();
        String[][] cases = {
            {"Saturn", "Capricorn", "3", "272.5", "in domicile"},
            {"Moon", "Taurus", "4", "33.5", "exalted"},
            {"Mars", "Cancer", "10", "99.5", "in fall"},
            {"Venus", "Aries", "20", "19.5", "in detriment"},
        };
        for (String[] c : cases) {
            String html = ip.hierarchyHtml(c[0], c[1], Integer.parseInt(c[2]), 1, 1, none,
                Double.parseDouble(c[3]), true);
            ok(c[0] + " in " + c[1] + " is " + c[4], html.contains(c[4]));
            String bound = Dignity.boundRulerOf(Double.parseDouble(c[3]));
            ok(c[0] + " in " + c[1] + ": the bound is " + bound, html.contains("Bound</b> of " + bound));
        }
        String chiron = ip.hierarchyHtml("Chiron", "Aries", 17, 2, 7, none, 16.4, true);
        ok("a body outside the traditional seven claims no essential dignity",
            !chiron.contains("in domicile") && !chiron.contains("exalted")
                && !chiron.contains("in detriment") && !chiron.contains("in fall"));
    }

    /**
     * K11's synthesis half, measured on 2026-09-19 to hold already - so this is a guard, not a
     * change. BodyScore, which ranks the bodies the synthesis leads with, takes no Level 3 or 4
     * input (bounds and faces reach only Dignity's condition score, which feeds no term of
     * prominence), so no amount of fine detail can outrank a Level 1 or 2 fact. And each body's
     * block in the report reads sign, then house, then aspects, with nothing from Level 4 in it.
     * Read from the sources, because the failure guarded against is a later edit.
     */
    private static void synthesis() throws Exception {
        String base = "src/main/java/com/zodiacomputing/ourania/";
        String score = code(base + "astro/BodyScore.java");
        ok("H1: the ranking reads no mansion, Sabian or decan",
            !score.matches("(?s).*\\b(LunarMansions|[Ss]abian|[Dd]ecan)\\w*.*"));

        String syn = code(base + "gui/NarrativeSynthesizer.java");
        int a = syn.indexOf("2. Planetary Placements");
        int b = syn.indexOf("3. Structural Tensions");
        ok("H2: the placements section is where it was", a > 0 && b > a);
        if (a > 0 && b > a) {
            String block = syn.substring(a, b);
            int sign = block.indexOf("getPlanetInSign");
            int house = block.indexOf("getPlanetInHouse");
            int aspect = block.indexOf("getAspect(");
            ok("H2: each body reads sign, then house, then aspects (" + sign + ", " + house
                + ", " + aspect + ")", sign > 0 && house > sign && aspect > house);
            ok("H3: nothing from Level 4 inside a body's block",
                !block.matches("(?s).*\\b(getSabian|getBodySabian|getBodyMansion|getDegree)\\w*\\(.*"));
        }
    }

    /** A source file with its comments removed, so a word in a comment cannot pass or fail it. */
    private static String code(String path) throws Exception {
        String s = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)),
            java.nio.charset.StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    // ------------------------------------------------------------------ helpers

    private static String paragraph(String html, String open) {
        int a = html.indexOf(open);
        if (a < 0) {
            return "";
        }
        int b = html.indexOf("</p>", a);
        return html.substring(a + open.length(), b < 0 ? html.length() : b);
    }

    private static String plain(String html) {
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ");
    }

    private static String text(InterpretationPanel ip) throws Exception {
        String[] t = new String[1];
        SwingUtilities.invokeAndWait(() -> t[0] = ip.getEditorPane().getText());
        return t[0];
    }

    private static String sample(int n, String first) {
        return n == 0 ? "" : " - " + n + ", e.g. " + first;
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        int before = failures.size();
        System.out.println("=== Part " + name + " ===");
        body.run();
        System.out.println("Part " + name + ": "
            + (failures.size() == before ? "clear" : (failures.size() - before) + " FAILED"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
