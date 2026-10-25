package com.zodiacomputing.ourania.gui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The strip of pages inside a rail tab: what it shows, what it says, and what it keeps quiet
 * about.
 *
 * <p><b>Why this suite exists.</b> The left rail carried twelve tabs in one column and became
 * three tabs with a strip inside two of them. A second level is a second place for a page to
 * get lost, and every failure mode here is silent: a page that is present but clipped off the
 * bottom of the strip, a listener that fires when nothing was chosen, a tab that greys while
 * its page stays in front. None of them throws and none of them shows up in a count.
 *
 * <p><b>What would otherwise rot is Part B.</b> {@code DrawerRail} fires its listener on
 * {@code select} - a click - and stays silent on {@code reveal}, the programmatic path. The
 * first version of SubRail fired on both, on the reasonable-sounding ground that a reading
 * must regenerate whenever it is opened. It must; but the window routes revealPage through the
 * rail AND the strip, the rail's listener asks the strip to re-open its page, and the strip's
 * listener reaches code that calls revealPage again. Three reasonable steps and a
 * StackOverflowError. The two paths have to stay different, and nothing but this says so.
 *
 * <p><b>Part C is the one a screenshot would have caught and a count never would.</b> Eight
 * tabs do not fit across a 340px drawer, so the strip wraps - and FlowLayout, asked for a
 * preferred size, answers one row's height no matter how many rows it will use. Inside
 * BorderLayout.NORTH, which takes exactly that height, four of Blueprint's eight pages were
 * laid out, painted, and clipped off-screen. A navigation bar that hides half its destinations
 * is worse than the twelve-tab column it replaced, because at least that column could be read.
 */
public final class SubRailCheck {

    private SubRailCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        System.out.println("=== Part A: pages go in and come out ===");
        int before = failures.size();
        SwingUtilities.invokeAndWait(SubRailCheck::thePages);
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: a click speaks, a reveal does not ===");
        before = failures.size();
        SwingUtilities.invokeAndWait(SubRailCheck::theListener);
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: every tab is on screen, however many there are ===");
        before = failures.size();
        SwingUtilities.invokeAndWait(SubRailCheck::theWrapping);
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: greying moves the reader off the page ===");
        before = failures.size();
        SwingUtilities.invokeAndWait(SubRailCheck::theGreying);
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: groups - four tabs on top, views below ===");
        before = failures.size();
        SwingUtilities.invokeAndWait(SubRailCheck::theGroups);
        report("Part E", before);

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
     * The grouped strip (reading framework spec, 2026-10-24): the top row is the groups that
     * have a page to show, the second row is the front group's pages when it has more than one,
     * and a page in no group is refused.
     */
    private static void theGroups() {
        SubRail rail = strip("Glance", "Planet", "Life", "Shape", "Report", "Year");
        java.util.Map<String, List<String>> g = new java.util.LinkedHashMap<>();
        g.put("At a glance", List.of("Glance"));
        g.put("Placements", List.of("Planet", "Life"));
        g.put("Patterns", List.of("Shape"));
        g.put("Full report", List.of("Report", "Year"));
        rail.setGroups(g);

        eq("every group with a page is on the top row",
            List.of("At a glance", "Placements", "Patterns", "Full report"), rail.groupLabels());
        eq("the first page is in front, so its group is", "At a glance", rail.selectedGroup());
        eq("a group of one page draws no second row", List.of(), rail.drawnPages());

        rail.select("Planet");
        eq("choosing a page moves the group", "Placements", rail.selectedGroup());
        eq("the second row is the group's pages, in the group's order",
            List.of("Planet", "Life"), rail.drawnPages());

        rail.select("Life");
        rail.select("Shape");
        rail.select("Glance");
        // The group's own tab, as the mouse reaches it: coming back finds the view last used.
        callOpenGroup(rail, "Placements");
        eq("coming back to a group finds the view it had", "Life", rail.selected());

        rail.setPageShown("Year", false);
        rail.select("Report");
        eq("a group left with one view draws no second row", List.of(), rail.drawnPages());

        rail.setPageShown("Shape", false);
        eq("a group with nothing to show leaves the top row",
            List.of("At a glance", "Placements", "Full report"), rail.groupLabels());
        rail.setPageShown("Shape", true);

        rail.select("Planet");
        rail.setPageShown("Planet", false);
        eq("hiding the page in front lands in the same group", "Life", rail.selected());
        eq("and that group is still the one in front", "Placements", rail.selectedGroup());

        rail.setPageEnabled("Life", false);
        rail.select("Planet");
        ok("a hidden page cannot be selected", !"Planet".equals(rail.selected()));

        ok("the pages are still all on the card stack, hidden or not",
            rail.pages().containsAll(List.of("Glance", "Planet", "Life", "Shape", "Report", "Year")));

        SubRail missing = strip("One", "Two");
        boolean refused = false;
        try {
            missing.setGroups(java.util.Map.of("Only", List.of("One")));
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        ok("a page in no group is refused", refused);

        SubRail twice = strip("One", "Two");
        java.util.Map<String, List<String>> both = new java.util.LinkedHashMap<>();
        both.put("A", List.of("One", "Two"));
        both.put("B", List.of("Two"));
        refused = false;
        try {
            twice.setGroups(both);
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        ok("a page in two groups is refused", refused);
    }

    /** The top row's click, through the private method the tab calls. */
    private static void callOpenGroup(SubRail rail, String label) {
        try {
            java.lang.reflect.Method m = SubRail.class.getDeclaredMethod("openGroup", String.class);
            m.setAccessible(true);
            m.invoke(rail, label);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SubRail strip(String... names) {
        SubRail rail = new SubRail();
        for (String name : names) {
            JLabel body = new JLabel(name);
            body.setName(name);
            rail.addPage(name, body);
        }
        return rail;
    }

    /** The register: order kept, components findable, the first one showing. */
    private static void thePages() {
        SubRail rail = strip("One", "Two", "Three");
        eq("the pages come back in the order they went in",
            Arrays.asList("One", "Two", "Three"), rail.pages());
        eq("the first page added is the one showing", "One", rail.selected());

        for (String name : rail.pages()) {
            // <b>The second claim is guarded by the first.</b> Written as two plain
            // assertions these read fine and the pair is a trap: when a page is registered
            // on the strip but not in the card register, the first FAILS correctly and the
            // second dereferences the same null - and an exception out of a check takes the
            // whole suite's report with it. A mutation run proved it: the harness got no
            // verdict at all, which is not a catch and must never be mistaken for one.
            javax.swing.JComponent body = rail.page(name);
            ok(name + " can be asked for its component", body != null);
            if (body != null) {
                ok(name + "'s component is the one that was added",
                    name.equals(body.getName()));
            }
        }
        ok("a page nobody added has no component", rail.page("Nope") == null);
        ok("and is not enabled either", !rail.isPageEnabled("Nope"));

        // <b>Revealing a name the strip does not have must not move it.</b> DrawerRail takes
        // an unknown label without complaint - it sets selected to the name and shows nothing,
        // so the rail then claims a page it is not displaying. That cost a real failure during
        // the restructure, and it is not a behaviour worth copying.
        rail.reveal("Nope");
        eq("revealing a page that does not exist leaves the strip where it was",
            "One", rail.selected());
    }

    /**
     * <b>The two paths, and the difference between them.</b> This is the assertion that would
     * have caught the stack overflow: one of these fires and the other does not, and which is
     * which is load-bearing all the way up through OuraniaWindow's routing.
     */
    private static void theListener() {
        SubRail rail = strip("One", "Two", "Three");
        final List<String> heard = new ArrayList<>();
        rail.setOnSelect(heard::add);

        rail.reveal("Two");
        eq("reveal shows the page", "Two", rail.selected());
        eq("and says nothing", 0, heard.size());

        rail.select("Three");
        eq("select shows the page", "Three", rail.selected());
        eq("and reports it", Arrays.asList("Three"), heard);

        // Re-entering the strip from the tab above it: the page did not change, the reader
        // looking at it did. A reading has to regenerate, so this one has to speak.
        rail.refire();
        eq("refire reports the page already showing",
            Arrays.asList("Three", "Three"), heard);
        eq("and does not move", "Three", rail.selected());

        // prepare is for the opening state, before anyone is listening for a choice.
        heard.clear();
        rail.prepare("One");
        eq("prepare shows the page", "One", rail.selected());
        eq("and says nothing either", 0, heard.size());

        // A greyed page cannot be chosen, by either path.
        rail.setPageEnabled("Two", false);
        heard.clear();
        rail.select("Two");
        eq("a greyed page cannot be selected", "One", rail.selected());
        eq("and selecting it reports nothing", 0, heard.size());
    }

    /**
     * <b>Measured, not eyeballed.</b> The strip reports a height; the tabs have bounds. If the
     * tallest tab's bottom edge is below the height the strip asked for, BorderLayout.NORTH
     * will clip it and the reader never sees that page.
     */
    private static void theWrapping() {
        // The real case: Blueprint's eight pages in the drawer's own width.
        SubRail rail = strip("Chart", "Chart B", "Overview", "Interpretation", "Selection",
            "Snapshot", "Report", "Synthesize");
        JPanel host = new JPanel(new java.awt.BorderLayout());
        host.add(rail, java.awt.BorderLayout.CENTER);
        host.setSize(340, 600);
        host.doLayout();
        rail.doLayout();

        JPanel row = (JPanel) field(rail, "strip");
        row.doLayout();
        Dimension want = row.getPreferredSize();
        int lowest = 0;
        int placed = 0;
        for (java.awt.Component c : row.getComponents()) {
            if (c.getWidth() > 0 && c.getHeight() > 0) {
                placed++;
            }
            lowest = Math.max(lowest, c.getY() + c.getHeight());
        }
        eq("all eight tabs are laid out", 8, placed);
        ok("the strip asks for enough height to hold them (" + want.height
            + "px asked, " + lowest + "px used)", want.height >= lowest);

        // <b>It really does wrap</b> - if all eight fitted on one row this part would pass
        // without ever exercising the thing it is about.
        ok("and they do not all fit on one row, which is the case this is for",
            lowest > 30);

        // Three narrow tabs in a wide strip stay on one row: the wrapping has to be a
        // response to the width, not something it does always.
        SubRail few = strip("One", "Two", "Three");
        JPanel wide = new JPanel(new java.awt.BorderLayout());
        wide.add(few, java.awt.BorderLayout.CENTER);
        wide.setSize(900, 600);
        wide.doLayout();
        few.doLayout();
        JPanel fewStrip = (JPanel) field(few, "strip");
        fewStrip.doLayout();
        int rows = 0;
        for (java.awt.Component c : fewStrip.getComponents()) {
            rows = Math.max(rows, c.getY() + c.getHeight());
        }
        ok("three short tabs in a wide strip stay on one row (" + rows + "px)", rows <= 30);
    }

    /**
     * <b>Greying the page in front of the reader has to move them.</b> Leaving it there shows
     * a live-looking page that nothing will refresh - which is the state Selection was in
     * before it had a door at all.
     */
    private static void theGreying() {
        SubRail rail = strip("One", "Two", "Three");
        rail.select("Two");
        rail.setPageEnabled("Two", false);
        ok("the greyed page is no longer in front", !"Two".equals(rail.selected()));
        ok("and the reader is on a page that works", rail.isPageEnabled(rail.selected()));

        ok("not everything is greyed yet", !rail.allDisabled());
        rail.setPageEnabled("One", false);
        rail.setPageEnabled("Three", false);
        ok("with every page greyed the strip says so", rail.allDisabled());

        // An empty strip is not "all disabled" - there is nothing to disable, and a tab above
        // an empty strip is a different problem from a tab above a greyed one.
        ok("an empty strip is not all-disabled", !new SubRail().allDisabled());

        // Greying a page that is not in front leaves the reader alone.
        SubRail other = strip("One", "Two");
        other.setPageEnabled("Two", false);
        eq("greying a page nobody is looking at does not move them", "One", other.selected());

        // And a label is not a page: renaming changes what is shown and nothing else.
        other.setPageLabel("One", "Renamed");
        eq("the shown text changes", "Renamed", other.pageLabel("One"));
        ok("the page is still addressed by its own name", other.isPageEnabled("One"));
        ok("and its component is still there", other.page("One") != null);
        eq("and it is still the page in front", "One", other.selected());
    }

    private static Object field(Object target, String name) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
            System.out.println("  FAIL: " + label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            String msg = label + ": got " + actual + ", expected " + expected;
            failures.add(msg);
            System.out.println("  FAIL: " + msg);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
