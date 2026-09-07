package com.zodiacomputing.ourania.gui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

/**
 * The two drawers: what they hold, and that nothing became unreachable on the way in.
 *
 * <p><b>This suite exists because of what a UI reshuffle cannot fail at.</b> Navigation was a
 * {@code JComboBox} whose listener was a nine-case switch on the selected string. It became ten
 * rows in a popup menu, then ten rows in an {@link Accordion} inside a {@link Drawer}, while the
 * chart's own sections moved to a second drawer on the other side of the window. At every one of
 * those steps, dropping a destination or pointing two rows at the same screen fails at nothing:
 * the screen still exists, still gets built at startup, and simply has no way in. Exactly the
 * defect this project logged for the browsable index - built, correct, unreachable.
 *
 * <p>Part C is the one that would otherwise rot, because it is the only thing here that knows
 * what the full set of screens is supposed to be.
 */
public final class NavigationCheck {

    /**
     * Every screen {@code OuraniaWindow.initScreens} registers with the CardLayout, plus
     * INTERPRETATION, which is not a card - it is the east panel, and {@code switchScreen}
     * toggles it before the CardLayout is ever asked.
     *
     * <b>Written out rather than read from the window</b>, because a check that derives its
     * expectation from the thing under test asserts nothing. If a screen is added, this list is
     * the place that has to change, and the failure says so.
     */
    private static final String[] SCREENS = {
        "SEARCH", "SKYMAP", "NAME_LIST", "INTERPRETATION", "RELEASING", "SETTINGS",
    };

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static SidePanel side;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                side = new SidePanel(null);
            } catch (Exception e) {
                failures.add("a sidebar would not construct: " + e);
            }
        });
        if (side == null) {
            System.out.println("FAILURES (1 of 1 checks):");
            System.out.println("  " + (failures.isEmpty() ? "null sidebar" : failures.get(0)));
            System.exit(1);
        }

        System.out.println("=== Part A: the accordion ===");
        int before = failures.size();
        theAccordion();
        report("Part A", before);

        System.out.println("=== Part B: the drawer holds what it should, and no dropdown ===");
        before = failures.size();
        theSidebars();
        report("Part B", before);

        System.out.println("=== Part C: every screen is still reachable ===");
        before = failures.size();
        everyScreenReachable();
        report("Part C", before);

        System.out.println("=== Part D: every engine has a door ===");
        before = failures.size();
        everyEngineHasADoor();
        report("Part D", before);

        System.out.println("=== Part E: an angle card says whose angle it is ===");
        before = failures.size();
        angleRoles();
        report("Part E", before);

        System.out.println();
        System.out.println("=== Part F: a hovered aspect line lights both its ends ===");
        before = failures.size();
        hoveredLineEnds();
        report("Part F", before);

        System.out.println();
        System.out.println("=== Part G: each ring's glyphs say which ring they are on ===");
        before = failures.size();
        ringColours();
        report("Part G", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }

        // Swing is up and the AWT thread is non-daemon; handover-stamp.sh captures this with
        // $(...) and would block on a JVM that never exits. See CompositeCheck, 2026-08-25.
        System.exit(0);
    }

    /**
     * Sections open and shut independently, and measure themselves.
     *
     * <b>Independence is the property worth pinning</b>, not a detail: these are the sections a
     * reader compares against each other - placements against transits against the grid - and
     * a one-at-a-time accordion would make comparing any two impossible. It would also be an
     * easy thing to "tidy" into later.
     */
    private static void theAccordion() {
        Accordion acc = new Accordion();
        Accordion.Section a = acc.addSection("Alpha", new javax.swing.JLabel("a"));
        Accordion.Section b = acc.addSection("Beta", new javax.swing.JLabel("b"));

        eq("sections are kept in order", "Alpha", acc.sections().get(0).title());
        eq("a section is addressable by title", b, acc.section("Beta"));
        ok("an unknown title yields null rather than throwing", acc.section("Nope") == null);

        ok("sections start shut", !a.isOpen() && !b.isOpen());
        a.setOpen(true);
        ok("opening one opens it", a.isOpen());
        ok("opening one does NOT shut the others", !b.isOpen());
        b.setOpen(true);
        ok("both can be open at once", a.isOpen() && b.isOpen());
        a.toggle();
        ok("toggle shuts an open section", !a.isOpen());
        ok("and leaves the other alone", b.isOpen());

        // Opened before the window is shown - the state the app starts in - a section must
        // take its height immediately rather than waiting on a Timer that is not running.
        Accordion.Section c = acc.addSection("Gamma", new javax.swing.JLabel("g"));
        c.setOpen(true);
        ok("a section opened while hidden has a height straight away",
            c.getPreferredSize().height > 20);
    }

    /** What the one drawer holds, and that the dropdown is gone. */
    private static void theSidebars() {
        List<JComboBox<?>> combos = new ArrayList<>();
        findCombos(side, combos);
        checks++;
        if (!combos.isEmpty()) {
            failures.add("a sidebar still holds " + combos.size() + " dropdown(s); the "
                + "navigation combo was replaced by a drawer");
        }

        // <b>The right drawer holds what the app can DO; the chart's own data moved left.</b>
        // Natal and Transits are pages on the left rail and the grid is the other tab of the
        // rail this panel sits in, so asserting they are still here would pin the layout that
        // was replaced.
        for (String s : new String[] {SidePanel.TABLES, SidePanel.EXPORT}) {
            ok("the drawer holds " + s, sectionTitles(side).contains(s));
        }
        // <b>Everything about the chart itself moved to the left rail.</b> The right drawer
        // is now what the app can do; the left is what you are looking at. Asserting these
        // are absent is what stops one drifting back into the other.
        for (String s : new String[] {"Natal Chart", "Transits", "Aspect Grids", "Selection",
                                      "Interpretation", "Readings", "Saved Charts"}) {
            ok("the drawer no longer holds " + s, !sectionTitles(side).contains(s));
        }
        // <b>Every reading kept a door when its section was dissolved.</b> Deleting a wrapper
        // is only safe if what it held survives, and five readings that became five tabs are
        // exactly the kind of thing that goes missing in a move with nothing noticing. Asked
        // of a real window, because the rail is the thing that would be wrong - checking the
        // READINGS array against itself would pass however few pages were actually built.
        WindowFacts facts = windowFacts();
        java.util.List<String> pages = facts.railPages;
        for (String[] r : OuraniaWindow.READINGS) {
            ok("the rail has a page for the reading " + r[0], pages.contains(r[0]));
        }
        for (String p : new String[] {OuraniaWindow.CHART_PAGE, OuraniaWindow.TRANSITS_PAGE,
                                      OuraniaWindow.SELECTION_PAGE,
                                      OuraniaWindow.READING_PAGE}) {
            ok("the rail has a page for " + p, pages.contains(p));
        }

        // <b>The menu and the grid are two tabs on one strip.</b> The grid spent a version
        // across the top, where its handle was a horizontal bar between two vertical strips
        // and read as a title rather than as a drawer, and a version as a second drawer on
        // this edge, which put one strip in front of the other instead of beside it.
        //
        // All three halves are pinned, because each was a different way of getting it wrong:
        // both tabs exist, the east edge is ONE component rather than a container holding two
        // drawers, and nothing is on the top edge. The middle one is the load-bearing check -
        // BorderLayout replaces the occupant of a region in silence, so a grid added straight
        // to EAST would take the menu off the screen with nothing failing.
        ok("the right rail has a " + OuraniaWindow.MENU_PAGE + " tab",
            facts.menuRailPages.contains(OuraniaWindow.MENU_PAGE));
        ok("the right rail has an " + OuraniaWindow.GRID_PAGE + " tab",
            facts.menuRailPages.contains(OuraniaWindow.GRID_PAGE));
        ok("the east edge is one rail, not two strips stacked",
            facts.eastIsOneRail);
        ok("nothing is left on the top edge for a handle to lie across",
            facts.nothingOnTop);

        // <b>The Menu wrapper is gone and its six destinations sit at the drawer's level.</b>
        // A section whose entire content was a list of links cost a click on the way to every
        // screen behind it. This pins both halves: the wrapper is absent, and nothing it held
        // was lost with it.
        ok("there is no Menu section left to nest the screens in",
            !sectionTitles(side).contains("Menu"));
        List<String> labels = new ArrayList<>();
        for (JButton row : rows(side)) {
            labels.add(row.getText());
        }
        for (String[] screen : SidePanel.SCREENS) {
            ok("the drawer offers " + screen[0] + " directly", labels.contains(screen[0]));
        }
        // Tools duplicated three rows that Menu already carried; with one drawer there is
        // nowhere for that duplication to hide, so it was removed rather than moved.
        ok("there is no leftover Tools section", !sectionTitles(side).contains("Tools"));

        // "Search Transit" opened the SEARCH card, which is ChartSetupPanel - the screen
        // titled "Chart Setup". The label had been stale since the two-step chooser replaced
        // the mode combo; this pins that it does not come back.
        for (JButton row : rows(side)) {
            ok("no row calls the setup screen a transit search: " + row.getText(),
                !row.getText().toLowerCase().contains("transit"));
        }
    }

    /**
     * Every screen the window builds has exactly one row in the Menu that reaches it.
     *
     * <b>Counted per screen, not totalled</b>, so two rows pointing at one destination fails as
     * loudly as a missing one - that pair is how a converted switch loses a case while the row
     * count still looks right.
     */
    private static void everyScreenReachable() {
        for (String screen : SCREENS) {
            int hits = 0;
            for (JButton row : menuRows()) {
                if (screen.equals(screenOf(row.getText()))) {
                    hits++;
                }
            }
            checks++;
            if (hits != 1) {
                failures.add("screen " + screen + " is reachable from " + hits
                    + " Menu rows; it must be exactly one");
            }
        }

        List<JButton> menu = menuRows();
        eq("the Menu offers one row per screen", Integer.valueOf(SCREENS.length),
            Integer.valueOf(menu.size()));
        // And the menu's own table is the same length, so the two lists cannot drift.
        eq("SidePanel.SCREENS matches", Integer.valueOf(SCREENS.length),
            Integer.valueOf(SidePanel.SCREENS.length));
        for (JButton row : menu) {
            ok("the row \"" + row.getText() + "\" explains itself on hover",
                row.getToolTipText() != null && !row.getToolTipText().trim().isEmpty());
        }
    }

    /**
     * Which screen a Menu row switches to, by firing it against a recording window.
     *
     * The listener closes over a string this suite cannot read, so the row is asked rather than
     * inspected - which also proves the listener is wired at all, where reading a field would
     * only prove it was stored.
     */
    private static String screenOf(String label) {
        Recorder recorder = new Recorder();
        SidePanel probe = new SidePanel(recorder);
        for (JButton row : rows(probe)) {
            if (row.getText().equals(label)) {
                row.doClick();
                return recorder.lastScreen;
            }
        }
        return null;
    }

    /** An OuraniaWindow that shows nothing and remembers what it was asked for. */
    private static final class Recorder extends OuraniaWindow {
        private String lastScreen;

        @Override
        public void switchScreen(String screenName) {
            this.lastScreen = screenName;
        }
    }

    /**
     * The screen destinations, without the directory's Chart A/B buttons.
     *
     * These used to live inside a Menu section; they now sit at the drawer's own level, so
     * this reads the screens column directly rather than looking for a wrapper that no
     * longer exists.
     */
    private static List<JButton> menuRows() {
        List<JButton> out = new ArrayList<>();
        java.awt.Container screens = side.screensColumn();
        if (screens != null) {
            collectButtons(screens, out);
        }
        return out;
    }

    /** What only a whole window can answer, gathered in one construction. */
    private static final class WindowFacts {
        final List<String> railPages = new ArrayList<>();
        final List<String> menuRailPages = new ArrayList<>();
        boolean eastIsOneRail;
        boolean nothingOnTop;
    }

    /**
     * The arrangement, read off a real window.
     *
     * Building a whole OuraniaWindow is heavier than the rest of this suite needs, so it is
     * done once and only for the questions that cannot be answered without it - which is why
     * everything one window can settle is collected here rather than asked for separately.
     */
    private static WindowFacts windowFacts() {
        final WindowFacts f = new WindowFacts();
        try {
            SwingUtilities.invokeAndWait(() -> {
                OuraniaWindow w = new OuraniaWindow();
                DrawerRail rail = w.chartRail();
                if (rail != null) {
                    f.railPages.addAll(rail.pageNames());
                }
                DrawerRail menu = w.menuRail();
                if (menu != null) {
                    f.menuRailPages.addAll(menu.pageNames());
                }
                java.awt.LayoutManager lm = w.getContentPane().getLayout();
                if (lm instanceof java.awt.BorderLayout) {
                    java.awt.BorderLayout bl = (java.awt.BorderLayout) lm;
                    f.nothingOnTop =
                        bl.getLayoutComponent(java.awt.BorderLayout.NORTH) == null;
                    // One rail, not a container holding a menu drawer and a grid drawer. Asked
                    // of the region rather than of the rail, because the rail could be correct
                    // and still be sharing the edge with something else.
                    f.eastIsOneRail =
                        bl.getLayoutComponent(java.awt.BorderLayout.EAST) instanceof DrawerRail;
                }
                w.dispose();
            });
        } catch (Exception e) {
            failures.add("a window would not construct for the layout check: " + e);
        }
        return f;
    }

    private static List<String> sectionTitles(Container panel) {
        List<String> titles = new ArrayList<>();
        Accordion acc = accordionOf(panel);
        if (acc != null) {
            for (Accordion.Section s : acc.sections()) {
                titles.add(s.title());
            }
        }
        return titles;
    }

    private static Accordion accordionOf(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof Accordion) {
                return (Accordion) k;
            }
            if (k instanceof Container) {
                Accordion found = accordionOf((Container) k);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<JButton> rows(Container panel) {
        List<JButton> out = new ArrayList<>();
        collectButtons(panel, out);
        return out;
    }

    /**
     * The buttons a reader can actually press, which is not every JButton in the tree.
     *
     * <b>A scrollbar is made of buttons.</b> {@code Widgets.styleScrollBar} cannot remove the
     * increment and decrement buttons - BasicScrollBarUI dereferences them during layout - so
     * it makes them zero-sized instead, and they are still JButtons sitting inside every
     * scrolled section. Walking the tree naively counted four of them as menu rows: the Menu
     * came back with fourteen entries, four of them nameless and none of them clickable.
     */
    /**
     * No engine may be computed, checked, and unreachable.
     *
     * <b>This project's signature defect, caught four times before it was ever measured.</b>
     * The browsable index whose only two entry points sat inside a panel that had to be open
     * first. SavedCharts, a working profile store the name-list screen never read. The harmonic
     * dial the work plan still listed as unimplemented. And on 2026-09-01, five astro classes
     * with zero references from this package at once.
     *
     * Every one of those passed every suite. A capability with no door is invisible in exactly
     * the way a missing one is not: nothing is broken, nothing is red, and no count moves. So
     * the reference count itself has to be the assertion.
     *
     * Two exemptions, and the first is a rule rather than a list.
     *
     * A class declaring its own {@code main} is a command-line tool: it is invoked directly,
     * so the interface is not the way in and its absence from this package means nothing.
     * Calibration, FittingHarness and CorpusBuilder are all of that kind - weight diagnostics
     * and fitting for a domain whose own header says it has no outcome variable. Writing the
     * rule that way means a future harness exempts itself correctly instead of arriving as a
     * failure someone silences by hand.
     *
     * The exempt-by-name list is empty, and that is the point of keeping it. DecanSystem was
     * its only entry on 2026-09-02 and was deleted the same day rather than exempted: Zodiac's
     * own header records that the app runs two decan schemes deliberately, Chaldean bound to
     * the Sabian decan_ruler field and the Golden Dawn cards, triplicity bound to the decan
     * prose and the wheel's ring, agreeing on 6 of 36 and each surface saying which it shows.
     * An enum offering a choice between them was not an unfinished feature; acting on it would
     * have printed a ruler that contradicted the prose beside it.
     *
     * The first version of this part named Calibration and DecanSystem and immediately found
     * FittingHarness and CorpusBuilder, which the audit that prompted it had missed. Adding a
     * name here is allowed. Adding one silently, to make this part go green, is the thing it
     * exists to prevent.
     */
    private static void everyEngineHasADoor() {
        java.util.Set<String> exempt = new java.util.HashSet<>();

        java.io.File astro = new java.io.File("src/main/java/com/zodiacomputing/ourania/astro");
        java.io.File guiDir = new java.io.File("src/main/java/com/zodiacomputing/ourania/gui");
        java.io.File[] engines = astro.listFiles((d, n) -> n.endsWith(".java"));
        java.io.File[] screens = guiDir.listFiles((d, n) -> n.endsWith(".java"));

        ok("the astro package is readable from the working directory", engines != null);
        ok("the gui package is readable from the working directory", screens != null);
        if (engines == null || screens == null) {
            return;
        }

        // One pass over the gui sources; grepping per engine would be 52 reads of every file.
        StringBuilder all = new StringBuilder();
        for (java.io.File f : screens) {
            if (f.getName().endsWith("Check.java")) {
                continue;   // a suite referencing an engine is not a door for a reader
            }
            try {
                all.append(new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8"));
            } catch (Exception e) {
                failures.add("could not read " + f.getName() + ": " + e);
            }
        }
        String gui = all.toString();

        for (java.io.File f : engines) {
            String name = f.getName().substring(0, f.getName().length() - ".java".length());
            if (name.endsWith("Check") || exempt.contains(name)) {
                continue;
            }
            try {
                String src = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
                if (src.contains("public static void main")) {
                    continue;   // a command-line tool; the interface is not its way in
                }
            } catch (Exception e) {
                failures.add("could not read " + f.getName() + ": " + e);
                continue;
            }
            ok(name + " is reachable from the interface", gui.contains(name));
        }
    }

    private static void collectButtons(Container c, List<JButton> out) {
        if (c instanceof javax.swing.JScrollBar) {
            return;
        }
        for (Component k : c.getComponents()) {
            if (k instanceof JButton) {
                JButton b = (JButton) k;
                if (b.getText() != null && !b.getText().trim().isEmpty()) {
                    out.add(b);
                }
            } else if (k instanceof Container) {
                collectButtons((Container) k, out);
            }
        }
    }

    private static void findCombos(Container c, List<JComboBox<?>> out) {
        for (Component k : c.getComponents()) {
            if (k instanceof JComboBox) {
                out.add((JComboBox<?>) k);
            } else if (k instanceof Container) {
                findCombos((Container) k, out);
            }
        }
    }

    /**
     * K7's labelling: the same click means different things and the card has to say which.
     *
     * <b>The decision closed the behaviour as correct and left this unbuilt.</b> Chart A's
     * angle reads natally because Chart A owns the houses on screen; Chart B's is visiting
     * and reads cross-chart. That asymmetry is the design - but the two cards were
     * identical in appearance, and an asymmetry a reader cannot see is indistinguishable
     * from an inconsistency, which is the one way it could fail.
     *
     * <b>Also guards the silence.</b> A single chart has only one set of angles, so there is
     * no question to answer and no badge; a badge on every card would train the reader to
     * ignore badges, which costs exactly the case this exists for.
     */
    private static void angleRoles() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fp =
                OuraniaWindow.class.getDeclaredField("interpretationPanel");
            fp.setAccessible(true);
            Object ip = fp.get(w[0]);
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            java.lang.reflect.Method banner = ip.getClass().getDeclaredMethod(
                "angleRoleBanner", String.class, String.class, int.class);
            banner.setAccessible(true);
            java.lang.reflect.Field mode = SkymapPanel.class.getDeclaredField("chartMode");
            mode.setAccessible(true);
            java.lang.reflect.Method roleFor = SkymapPanel.class.getDeclaredMethod(
                "angleRoleFor", boolean.class, boolean.class);
            roleFor.setAccessible(true);

            mode.set(sky, ChartMode.SINGLE);
            sky.showTransitChart = false;
            ok("a single chart shows no role banner",
                ((String) banner.invoke(ip, "Ascendant", "ANCHOR", -1)).isEmpty());

            mode.set(sky, ChartMode.SYNASTRY);
            String anchor = (String) banner.invoke(ip, "Ascendant", "ANCHOR", -1);
            String bridge = (String) banner.invoke(ip, "Ascendant", "BRIDGE", 7);
            String skyCard = (String) banner.invoke(ip, "Ascendant", "SKY", -1);

            ok("the anchor card names Chart A", anchor.contains("Chart A"));
            ok("the anchor card calls it the sovereign host",
                anchor.toLowerCase().contains("sovereign host"));
            ok("the bridge card shows the B-to-A direction", bridge.contains("&rarr;"));
            ok("the bridge card calls it the interpersonal bridge",
                bridge.toLowerCase().contains("interpersonal bridge"));
            ok("the bridge card names which of A's houses it lands in",
                bridge.contains("7th"));
            ok("the sky card claims neither role",
                skyCard.contains("SKY") && !skyCard.toLowerCase().contains("sovereign"));
            ok("the three cards are distinguishable",
                !anchor.equals(bridge) && !bridge.equals(skyCard)
                    && !anchor.equals(skyCard));

            // One rule, asked by the wheel and by the placements list alike.
            mode.set(sky, ChartMode.SYNASTRY);
            ok("a synastry outer angle is a bridge",
                "BRIDGE".equals(roleFor.invoke(sky, false, true).toString()));
            ok("a synastry inner angle is the anchor",
                "ANCHOR".equals(roleFor.invoke(sky, false, false).toString()));
            ok("the sky ring is always the sky",
                "SKY".equals(roleFor.invoke(sky, true, false).toString()));
            mode.set(sky, ChartMode.TRANSIT);
            ok("a transit outer angle is a moment, not a person",
                "SKY".equals(roleFor.invoke(sky, false, true).toString()));
        } finally {
            SwingUtilities.invokeAndWait(w[0]::dispose);
        }
    }

    /**
     * Hovering an aspect line has to say which two bodies it joins.
     *
     * <b>The lines stopped touching their bodies on 2026-09-06.</b> Moving them onto three
     * nested discs is what stopped the wheel reading as a tangle, but it also cut the visible
     * link between a line and its ends - a reader could light a chord and still have to work
     * out from its angle which two points it belonged to. The halo puts that back, and this is
     * the rule it draws from.
     *
     * <b>The two ends are read from opposite ends of the pair</b>, which is the part worth
     * asserting: for a cross-chart line A is the outer body and B the natal one, so the natal
     * loop must not light A and the outer loop must not light B. Get that backwards and the
     * halo appears on two bodies that are not in aspect at all - which looks exactly like a
     * working feature.
     */
    private static void hoveredLineEnds() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            java.lang.reflect.Method lit = SkymapPanel.class.getDeclaredMethod(
                "onHighlightedLine", int.class, boolean.class);
            lit.setAccessible(true);

            // Nothing hovered: nothing lights, on either ring.
            set(sky, "highlightA", -1);
            set(sky, "highlightB", -1);
            set(sky, "highlightTransit", Boolean.FALSE);
            for (int i = 0; i < 6; i++) {
                ok("with no line hovered, body " + i + " stays dark",
                    !(Boolean) lit.invoke(sky, i, false)
                        && !(Boolean) lit.invoke(sky, i, true));
            }

            // A natal-to-natal line: both ends are on the natal wheel, neither on the outer.
            set(sky, "highlightA", 2);
            set(sky, "highlightB", 5);
            set(sky, "highlightTransit", Boolean.FALSE);
            ok("a natal line lights its first end", (Boolean) lit.invoke(sky, 2, false));
            ok("a natal line lights its second end", (Boolean) lit.invoke(sky, 5, false));
            ok("and lights nothing on the outer ring",
                !(Boolean) lit.invoke(sky, 2, true) && !(Boolean) lit.invoke(sky, 5, true));
            ok("and nothing that is not an end", !(Boolean) lit.invoke(sky, 3, false));

            // A cross-chart line: A is the outer body, B the natal one, and not the reverse.
            set(sky, "highlightA", 2);
            set(sky, "highlightB", 5);
            set(sky, "highlightTransit", Boolean.TRUE);
            ok("a cross-chart line lights its outer end on the outer ring",
                (Boolean) lit.invoke(sky, 2, true));
            ok("and its natal end on the natal wheel",
                (Boolean) lit.invoke(sky, 5, false));
            ok("the outer end does not also light on the natal wheel",
                !(Boolean) lit.invoke(sky, 2, false));
            ok("the natal end does not also light on the outer ring",
                !(Boolean) lit.invoke(sky, 5, true));
            ok("and nothing that is not an end", !(Boolean) lit.invoke(sky, 3, true));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    /**
     * A glyph has to say which chart it belongs to without being clicked.
     *
     * <b>Lightness is not a distinction.</b> Until 2026-09-06 all three rings drew the same
     * element palette, differing only by how much it had been lightened - a Venus was a Venus
     * and nothing on it said whose. Only the four angles per ring carried gold or blue.
     *
     * <b>The first attempt blended and this check is why it did not ship.</b> Mixing the
     * element colour halfway to the ring's hue put the partner and sky rings 67 apart in RGB:
     * different, and useless - a cyan Moon blended to gold comes out pale green, a red Sun
     * blended to blue comes out grey, so neither ring read as its own colour either. Flat
     * hues are 174 apart. The threshold below is set where the blended version fails.
     */
    private static void ringColours() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            java.lang.reflect.Method ink = SkymapPanel.class.getDeclaredMethod(
                "ringInk", int.class, SkymapPanel.AngleRole.class);
            ink.setAccessible(true);
            java.lang.reflect.Method bead = SkymapPanel.class.getDeclaredMethod(
                "ringBead", SkymapPanel.AngleRole.class);
            bead.setAccessible(true);
            java.lang.reflect.Method bodyColor = SkymapPanel.class.getDeclaredMethod(
                "bodyColor", int.class);
            bodyColor.setAccessible(true);

            for (int i = 0; i < com.zodiacomputing.ourania.astro.Bodies.count(); i++) {
                java.awt.Color anchor =
                    (java.awt.Color) ink.invoke(sky, i, SkymapPanel.AngleRole.ANCHOR);
                java.awt.Color bridge =
                    (java.awt.Color) ink.invoke(sky, i, SkymapPanel.AngleRole.BRIDGE);
                java.awt.Color skyInk =
                    (java.awt.Color) ink.invoke(sky, i, SkymapPanel.AngleRole.SKY);
                String name = com.zodiacomputing.ourania.astro.Bodies.at(i).name;

                // The natal wheel is where element colour earns its place - untouched.
                ok("the natal wheel keeps " + name + "'s element colour",
                    anchor.equals((java.awt.Color) bodyColor.invoke(sky, i)));
                ok("the partner ring tells " + name + " apart from the sky ring: "
                    + rgbGap(bridge, skyInk),
                    rgbGap(bridge, skyInk) > 120);
                ok("the partner ring tells " + name + " apart from the natal wheel",
                    rgbGap(bridge, anchor) > 40);
                ok("the sky ring tells " + name + " apart from the natal wheel",
                    rgbGap(skyInk, anchor) > 40);
            }

            // The beads under the glyphs have to agree with them, or a gold glyph on the
            // sky ring's bead is two claims about the same point.
            java.awt.Color bBridge = (java.awt.Color) bead.invoke(null, SkymapPanel.AngleRole.BRIDGE);
            java.awt.Color bSky = (java.awt.Color) bead.invoke(null, SkymapPanel.AngleRole.SKY);
            ok("the beads differ between the two outer rings", rgbGap(bBridge, bSky) > 20);
            ok("the partner bead is the warmer of the two",
                bBridge.getRed() - bBridge.getBlue() > bSky.getRed() - bSky.getBlue());
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    /** Crude RGB distance - enough to say two colours are not the same colour. */
    private static int rgbGap(java.awt.Color a, java.awt.Color b) {
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return (int) Math.round(Math.sqrt(dr * dr + dg * dg + db * db));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
