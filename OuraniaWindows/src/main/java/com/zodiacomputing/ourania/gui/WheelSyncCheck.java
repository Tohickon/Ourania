package com.zodiacomputing.ourania.gui;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The drawer answers a click on the wheel (reading framework spec, section 4).
 *
 * <b>The rules that make it safe are the ones asked.</b> A planet brings up the Placements
 * tab, a house the parts of life, an aspect the Patterns - but only while the left rail is
 * already open, and never by moving a page that is already in front. The first would take width
 * from the wheel the reader is clicking; the second would scroll a list under the cursor that
 * just clicked it. Scrolling itself is asked only as far as a headless window can answer: that
 * the link a click scrolls to exists, and that finding text never throws.
 */
public final class WheelSyncCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            long end = System.nanoTime() + 20_000_000_000L;
            while (sky.radixChart() == null && System.nanoTime() < end) {
                Thread.sleep(50);
            }
            int casts = sky.castsApplied();
            SwingUtilities.invokeAndWait(() -> sky.applyChartSettings("1990-06-15", "12:00",
                "51.5074, -0.1278", ChartMode.SINGLE, "", "", "", false));
            long cast = System.nanoTime() + 20_000_000_000L;
            while ((sky.castsApplied() <= casts || !sky.innerIsBirthChart())
                    && System.nanoTime() < cast) {
                Thread.sleep(50);
            }
            yes("a birth chart is on the wheel", sky.innerIsBirthChart());
            SwingUtilities.invokeAndWait(() -> w.chartRail().choose(ChartTab.CHART_A.door));

            System.out.println("=== Part A: a planet, a house, an aspect ===");
            clicks(w);

            System.out.println("=== Part B: the entry a click scrolls to exists ===");
            anchors(w);

            System.out.println("=== Part C: finding text never throws ===");
            scrolling();
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }
        report();
    }

    private static void front(OuraniaWindow w, String page, boolean open) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            w.setPageEnabled(page, true);
            w.revealPage(page);
            w.chartRail().setOpen(open);
        });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void clicks(OuraniaWindow w) throws Exception {
        String away = "Report";

        // A planet, rail open: Placements.
        front(w, away, true);
        eq("the report is in front", away, w.currentPage());
        SwingUtilities.invokeAndWait(() -> w.showInterpretationForPlanet("Mars", "aries", 5, 1, 3,
            new ArrayList<String[]>()));
        eq("a planet click brings up Placements", OuraniaWindow.CHART_PAGE, w.currentPage());
        yes("and leaves the rail open", w.chartRail().isOpen());

        // Already there: left alone.
        SwingUtilities.invokeAndWait(() -> w.showInterpretationForPlanet("Venus", "taurus", 5, 1,
            3, new ArrayList<String[]>()));
        eq("a click when Placements is in front leaves it there", OuraniaWindow.CHART_PAGE,
            w.currentPage());

        // A planet, rail closed: nothing opens, nothing moves.
        front(w, away, false);
        yes("the rail is shut", !w.chartRail().isOpen());
        SwingUtilities.invokeAndWait(() -> w.showInterpretationForPlanet("Mars", "aries", 5, 1, 3,
            new ArrayList<String[]>()));
        eq("with the rail shut the page does not move", away, w.currentPage());
        yes("and the rail stays shut", !w.chartRail().isOpen());

        // A house, rail open: the parts of life.
        front(w, away, true);
        SwingUtilities.invokeAndWait(() -> w.showInterpretationForHouse(10));
        eq("a house click brings up the parts of life", OuraniaWindow.LIFE_AREAS_PAGE,
            w.currentPage());

        // An aspect, rail open: Patterns.
        front(w, away, true);
        SwingUtilities.invokeAndWait(() -> w.showInterpretationForAspect("Sun", "Moon", "Square"));
        eq("an aspect click brings up Patterns", OuraniaWindow.OVERVIEW_PAGE, w.currentPage());

        // Every click still opens the Selected drawer, as David asked on 2026-10-20.
        front(w, away, true);
        SwingUtilities.invokeAndWait(() -> w.showInterpretationForHouse(4));
        yes("a click still fills the Selected drawer", selectedHasText(w));
    }

    private static boolean selectedHasText(OuraniaWindow w) {
        try {
            JEditorPane pane = (JEditorPane) CheckReflect.get(w, "selectionPane");
            javax.swing.text.Document doc = pane.getDocument();
            return !doc.getText(0, doc.getLength()).trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static void anchors(OuraniaWindow w) throws Exception {
        int mars = Arrays.asList(SkymapPanel.BODY_NAMES).indexOf("Mars");
        yes("Mars is a body the wheel names", mars >= 0);
        JEditorPane placements = w.paneOf(OuraniaWindow.CHART_PAGE);
        yes("the Placements page has a pane", placements != null);
        if (placements == null) {
            return;
        }
        yes("Mars has an entry the wheel can scroll to", hasAnchor(placements, "base_" + mars));
        int shown = 0;
        int named = 0;
        HTMLDocument doc = (HTMLDocument) placements.getDocument();
        HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);
        while (it.isValid()) {
            Object href = it.getAttributes().getAttribute(HTML.Attribute.HREF);
            Object name = it.getAttributes().getAttribute(HTML.Attribute.NAME);
            if (href != null && String.valueOf(href).startsWith("base_")) {
                shown++;
                if (href.equals(name)) {
                    named++;
                }
            }
            it.next();
        }
        yes("every planet entry is named for the link it is (" + named + " of " + shown + ")",
            shown > 0 && named == shown);
    }

    private static boolean hasAnchor(JEditorPane pane, String name) {
        HTMLDocument doc = (HTMLDocument) pane.getDocument();
        HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);
        while (it.isValid()) {
            if (name.equals(it.getAttributes().getAttribute(HTML.Attribute.NAME))) {
                return true;
            }
            it.next();
        }
        return false;
    }

    private static void scrolling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JEditorPane pane = new JEditorPane();
            pane.setContentType("text/html");
            pane.setText("<html><body><h3>Alpha</h3><p>one</p><h3>Beta</h3></body></html>");
            pane.setSize(300, 200);
            boolean ok = true;
            try {
                OuraniaWindow.scrollToText(pane, "Beta");
                OuraniaWindow.scrollToText(pane, "not on the page");
                OuraniaWindow.scrollToText(pane, "");
            } catch (RuntimeException e) {
                ok = false;
            }
            yes("finding text, present or absent, never throws", ok);
        });
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (int i = 0; i < Math.min(40, failures.size()); i++) {
            System.out.println("  " + failures.get(i));
        }
        System.exit(1);
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }
}
