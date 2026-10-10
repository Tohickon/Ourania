package com.zodiacomputing.ourania.gui;

import javax.swing.SwingUtilities;

/**
 * The Index has a door, and there is still only one index (G11).
 *
 * <p><b>What this row turned out to be.</b> The list said "the placeholder Help card was removed and
 * nothing replaced it". But nine sections of reference material already existed in
 * {@code InterpretationPanel.showIndex} - every registered point grouped as the registry groups them,
 * the signs, the houses, every aspect the engine can find, the dignities and their scoring, the
 * decans, 360 Sabian symbols, the tarot attributions and the 28 mansions. <b>What was missing was the
 * door:</b> the only way in was a link inside a reading a reader was already looking at, which is the
 * shape {@code ChartTables}' header names as this project's signature defect. The index was the last
 * thing still in it.
 *
 * <p>Part B is the part that matters in a year: <b>one builder, two surfaces</b>. A second index page
 * written for the tab would have been the defect this project has paid for most.
 */
public final class IndexPageCheck {

    private IndexPageCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    /** Every section the index offers, written out by hand so adding one is a visible diff. */
    private static final String[] SECTIONS = {
        "bodies", "signs", "houses", "aspects", "dignities", "decans", "sabians", "tarot",
    };

    private static OuraniaWindow window;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        try {
            final OuraniaWindow[] w = new OuraniaWindow[1];
            SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
            window = w[0];
            part("A: the index is a tab on the right rail", () -> door(w[0]));
            part("B: one index, two surfaces", () -> oneBuilder(w[0]));
            part("C: every section says something", () -> sections(w[0]));
            part("D: a link goes where that kind of link goes", () -> links(w[0]));
            part("E: what the tab does not do", () -> limits());
        } finally {
            if (window != null) {
                final OuraniaWindow w = window;
                SwingUtilities.invokeAndWait(() -> w.dispose());
            }
        }
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

    // ---------------------------------------------------------------- Part A

    private static void door(OuraniaWindow w) throws Exception {
        DrawerRail rail = (DrawerRail) CheckReflect.get(w, "menuRail");
        java.util.List<String> pages = pagesOf(rail);
        ok("the right rail carries an " + OuraniaWindow.INDEX_PAGE + " tab: " + pages,
            pages.contains(OuraniaWindow.INDEX_PAGE));
        ok("beside the two that were already there",
            pages.contains(OuraniaWindow.MENU_PAGE) && pages.contains(OuraniaWindow.GRID_PAGE));
        ok("the tab is enabled, so it can be pressed",
            rail.isPageEnabled(OuraniaWindow.INDEX_PAGE));
        // <b>Pressed, not merely present.</b> An index wired to nothing is this row's own defect in
        // a new hat, which is why H4b's Part G pressed its search results too.
        SwingUtilities.invokeAndWait(() -> rail.revealImmediately(OuraniaWindow.INDEX_PAGE));
        SwingUtilities.invokeAndWait(() -> { });
        eq("choosing it selects it", OuraniaWindow.INDEX_PAGE, rail.selected());
        ok("and opens the rail", rail.isOpen());
        ok("the tab and the page heading say the same word",
            plain(w.indexPanel.pane()).contains("Index"));
    }

    // ---------------------------------------------------------------- Part B

    /**
     * The tab shows the interpretation panel's index, not one of its own.
     *
     * Asserted two ways: the words on screen are the words that builder produced, and
     * {@code IndexPanel} contains no index-building code at all - swept with the scanner J8
     * extracted, over code with its literals and comments removed.
     */
    private static void oneBuilder(OuraniaWindow w) throws Exception {
        InterpretationPanel source = (InterpretationPanel) CheckReflect.get(w, "interpretationPanel");
        for (String cat : join("", SECTIONS)) {
            final String c = cat;
            // <b>Compared pane to pane, not pane to string.</b> The first version stripped tags from
            // the HTML and compared that with what the pane displays; the two differed by ten
            // characters on every page, because Swing's HTML reader resolves entities and collapses
            // space its own way. Rendering the same html into BOTH panes makes the comparison exact
            // and keeps it about the thing being asserted rather than about the renderer.
            SwingUtilities.invokeAndWait(() -> {
                w.indexPanel.show(c);
                source.showIndex(c);
            });
            String onTheTab = plain(w.indexPanel.pane());
            String onTheReadingPage = plain(source.getEditorPane());
            eq("the " + label(c) + " page is the same on both surfaces: "
                + onTheTab.length() + " characters", onTheReadingPage, onTheTab);
        }
        String code = JavaSource.codeOnly(read("IndexPanel.java"));
        ok("IndexPanel asks for the html rather than building it",
            code.contains("indexHtml"));
        ok("and contains no html of its own",
            !code.contains("append") && !code.contains("StringBuilder"));
    }

    // ---------------------------------------------------------------- Part C

    private static void sections(OuraniaWindow w) throws Exception {
        SwingUtilities.invokeAndWait(() -> w.indexPanel.show(""));
        String top = plain(w.indexPanel.pane());
        // Derived, not typed: the count comes from the registry, so a body registered tomorrow is
        // listed tomorrow.
        ok("the top page counts the registry's points: "
            + com.zodiacomputing.ourania.astro.Bodies.count(),
            top.contains(String.valueOf(com.zodiacomputing.ourania.astro.Bodies.count())));
        ok("and the engine's aspect types: "
            + com.zodiacomputing.ourania.astro.Aspects.Type.values().length,
            top.contains(String.valueOf(
                com.zodiacomputing.ourania.astro.Aspects.Type.values().length)));
        for (String s : SECTIONS) {
            final String c = s;
            SwingUtilities.invokeAndWait(() -> w.indexPanel.show(c));
            String text = plain(w.indexPanel.pane());
            ok("the " + s + " page is not empty: " + text.length() + " characters",
                text.length() > 60);
            ok("and it is not the not-found page", !text.contains("No such section"));
        }
        SwingUtilities.invokeAndWait(() -> w.indexPanel.show("nonsense"));
        ok("a section that does not exist says so rather than showing nothing",
            plain(w.indexPanel.pane()).contains("No such section"));
    }

    // ---------------------------------------------------------------- Part D

    /**
     * <b>Section links stay here; reading links go where readings live.</b> The negative assertion is
     * the one that keeps the right rail from becoming a second reading surface, which is the mistake
     * the side panel made one level up on 26 Sep.
     */
    private static void links(OuraniaWindow w) throws Exception {
        SwingUtilities.invokeAndWait(() -> w.indexPanel.follow("index|houses"));
        eq("a section link moves within the tab", "houses", w.indexPanel.category());
        SwingUtilities.invokeAndWait(() -> w.indexPanel.follow("index"));
        eq("and the index link goes back to the list of sections", "", w.indexPanel.category());
        SwingUtilities.invokeAndWait(() -> w.indexPanel.follow("index|signs"));
        SwingUtilities.invokeAndWait(() -> w.indexPanel.follow("back"));
        eq("Back stays inside the index rather than leaving for the chart",
            "", w.indexPanel.category());

        DrawerRail left = (DrawerRail) CheckReflect.get(w, "chartRail");
        SwingUtilities.invokeAndWait(() -> w.indexPanel.show("bodies"));
        SwingUtilities.invokeAndWait(() -> w.indexPanel.follow("body|0"));
        SwingUtilities.invokeAndWait(() -> { });
        // <b>Selection, not Interpretation - and my first version of this assertion was wrong.</b>
        // A reading of ONE thing is a selection, and the rail has had a page for exactly that since
        // 72d37af1; Interpretation is the whole chart, which is where "back" goes. So a body opened
        // from the index lands where a body opened from the wheel lands, which is the claim worth
        // making.
        // <b>The window is asked, not the rail.</b> The rail answers "Blueprint" now -
        // truthfully, and uselessly: the claim is about which PAGE the link opened, and the
        // page is a level down. currentPage reports the deepest selection, which is what a
        // reader is actually looking at.
        eq("a reading link opens where a single reading belongs",
            OuraniaWindow.SELECTION_PAGE, w.currentPage());
        eq("and the index tab is still showing the section it was on",
            "bodies", w.indexPanel.category());
    }

    // ---------------------------------------------------------------- Part E

    private static void limits() throws Exception {
        String code = JavaSource.codeOnly(read("IndexPanel.java"));
        ok("the tab renders no interpretation of its own",
            !code.contains("InterpretationService") && !code.contains("showReading"));
        ok("it does not reach into the chart", !code.contains("SkymapPanel"));
        ok("G11 is not closed by this: no help page and no onboarding are claimed here", true);
    }

    // ---------------------------------------------------------------- plumbing

    private static java.util.List<String> join(String first, String[] rest) {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add(first);
        out.addAll(java.util.Arrays.asList(rest));
        return out;
    }

    private static String label(String cat) {
        return cat.isEmpty() ? "top" : cat;
    }

    /** The rail's tab labels, read off the tabs it actually built rather than off a list of names. */
    private static java.util.List<String> pagesOf(DrawerRail rail) throws Exception {
        java.util.List<String> out = new java.util.ArrayList<>();
        Object tabs = CheckReflect.get(rail, "tabs");
        for (Object tab : (java.util.List<?>) tabs) {
            out.add(String.valueOf(CheckReflect.get(tab, "label")));
        }
        return out;
    }

    private static String plain(javax.swing.JEditorPane pane) throws Exception {
        javax.swing.text.Document d = pane.getDocument();
        return d.getText(0, d.getLength()).replaceAll("\\s+", " ").trim();
    }

    private static String stripTags(String html) {
        return html.replaceAll("(?s)<[^>]*>", " ").replaceAll("&nbsp;", " ")
            .replaceAll("&deg;", "").replaceAll("\\s+", " ").trim();
    }

    /**
     * The same words, allowing for what Swing's HTML reader does to whitespace and entities.
     *
     * A byte comparison would fail on the renderer rather than on a difference that matters, so this
     * compares the words - which is what a reader sees, and enough to catch a second index.
     */
    private static boolean sameWords(String a, String b) {
        return words(a).equals(words(b));
    }

    private static java.util.List<String> words(String s) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String w : s.split("[^\\p{L}\\p{N}]+")) {
            if (!w.isEmpty()) {
                out.add(w.toLowerCase());
            }
        }
        return out;
    }

    private static String read(String name) throws Exception {
        java.io.File f = new java.io.File(
            "src/main/java/com/zodiacomputing/ourania/gui/" + name);
        return new String(java.nio.file.Files.readAllBytes(f.toPath()),
            java.nio.charset.StandardCharsets.UTF_8);
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + name.charAt(0) + ": "
            + (failures.size() == before ? "PASS" : (failures.size() - before) + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String what, boolean pass) {
        checks++;
        if (!pass) {
            failures.add(what);
        }
        System.out.println("  " + (pass ? "ok  " : "FAIL") + "  " + what);
    }

    private static void eq(String what, Object want, Object got) {
        ok(what + ": " + got, want.equals(got));
    }
}
