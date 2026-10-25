package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Gestalt;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Export on the Full report (reading framework spec, section 7): the Markdown converter, and the
 * rule that export reads the page in front.
 *
 * <b>Parity is asked of the window.</b> Export used to read the Reading panel's pane whatever was
 * in front, so a page other than the Reading could not be exported. Here each page of the
 * Full report group is brought to the front and the pane export would read is compared with the
 * pane that page owns - and with the Reading panel's, to show the two are no longer confused.
 */
public final class ExportBarCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        System.out.println("=== Part A: Markdown ===");
        markdown();

        System.out.println("=== Part B: the bar, and the page in front ===");
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            bar(w);
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }
        report();
    }

    // ------------------------------------------------------------------ Part A

    private static void markdown() throws Exception {
        eq("nothing is nothing", "", ChartExporter.toMarkdown(null));
        eq("a heading", "## Title", ChartExporter.toMarkdown("<h2>Title</h2>"));
        eq("a smaller heading", "### Sub", ChartExporter.toMarkdown("<h3 style='x'>Sub</h3>"));
        eq("bold and italic", "Hello **bold** and _it_.",
            ChartExporter.toMarkdown("<p>Hello <b>bold</b> and <i>it</i>.</p>"));
        eq("strong and em are the same", "**a** _b_",
            ChartExporter.toMarkdown("<strong>a</strong> <em>b</em>"));
        eq("a list", "- One\n- Two", ChartExporter.toMarkdown("<ul><li>One</li><li>Two</li></ul>"));
        eq("a line break", "a\nb", ChartExporter.toMarkdown("a<br>b"));
        eq("entities", "A & B <c> — 5°",
            ChartExporter.toMarkdown("A &amp; B &lt;c&gt; &mdash; 5&deg;"));
        eq("a table row is a line", "Fire | 40%",
            ChartExporter.toMarkdown("<table><tr><td>Fire</td><td>40%</td></tr></table>"));
        eq("the head and its style are dropped", "x",
            ChartExporter.toMarkdown("<html><head><style>p{color:red}</style></head>"
                + "<body>x</body></html>"));
        eq("an empty bold leaves nothing", "ab", ChartExporter.toMarkdown("a<b> </b>b").replace(" ", ""));
        yes("blank lines never run past one", !ChartExporter.toMarkdown("<p>a</p><p></p><p></p><p>b</p>")
            .contains("\n\n\n"));

        // A real page: the bars of the At a glance page, which are tables.
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        ChartFrame f = ChartFrame.compute(sw, new SweDate(1984, 5, 17, 6).getJulDay(), 40.71,
            -74.01, 'P', false, 0.0);
        String md = ChartExporter.toMarkdown(GlanceBlocks.prefix(f, Gestalt.compute(f), false, true));
        yes("a real page has no tag left", !md.matches("(?s).*</?[a-zA-Z][^>]*>.*"));
        yes("or an escape left", !md.contains("&amp;") && !md.contains("&nbsp;")
            && !md.contains("&mdash;"));
        yes("it keeps the words", md.contains("Fire") && md.contains("Strongest"));
        yes("and the percentages", md.matches("(?s).*\\d+%.*"));
        yes("and bold where the page had bold", md.contains("**"));
    }

    // ------------------------------------------------------------------ Part B

    private static void bar(OuraniaWindow w) throws Exception {
        ExportBar bar = w.exportBar();
        yes("the window has an export bar", bar != null);
        if (bar == null) {
            return;
        }
        eq("three buttons, in the spec's order", List.of("Copy Markdown", "Export HTML",
            "Print / PDF"), bar.labels());

        // The Sky tab is in front at start. These are its Full report views.
        List<String> views = new ArrayList<>();
        for (String page : OuraniaWindow.READING_GROUPS.get(OuraniaWindow.REPORT_TAB)) {
            if (OuraniaWindow.CHART_TAB_PAGES.get(ChartTab.SKY).contains(page)) {
                views.add(page);
            }
        }
        yes("the Sky has several Full report views: " + views, views.size() >= 3);

        for (String page : views) {
            SwingUtilities.invokeAndWait(() -> {
                w.setPageEnabled(page, true);
                w.revealPage(page);
            });
            SwingUtilities.invokeAndWait(() -> { });
            eq(page + " is in front", page, w.currentPage());
            yes(page + ": the bar shows under Full report", bar.isVisible());
            JEditorPane own = w.paneOf(page);
            yes(page + " owns a text pane", own != null);
            if (page.equals("Calendar")) {
                // The month grid is in front first, and a grid is a picture.
                yes("Calendar: the month grid in front has no text pane", w.frontPane() == null);
            } else {
                yes(page + ": export reads this page's pane", w.frontPane() == own);
                if (!page.equals(OuraniaWindow.READING_PAGE)) {
                    yes(page + ": and not the Reading panel's",
                        w.frontPane() != w.interpretation().getEditorPane());
                }
            }
        }

        // Any other group hides it.
        for (String page : List.of(OuraniaWindow.CHART_PAGE, OuraniaWindow.OVERVIEW_PAGE,
                "Snapshot")) {
            SwingUtilities.invokeAndWait(() -> {
                w.setPageEnabled(page, true);
                w.revealPage(page);
            });
            SwingUtilities.invokeAndWait(() -> { });
            eq(page + " is in front", page, w.currentPage());
            yes(page + ": the bar is hidden outside Full report", !bar.isVisible());
        }
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
