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
