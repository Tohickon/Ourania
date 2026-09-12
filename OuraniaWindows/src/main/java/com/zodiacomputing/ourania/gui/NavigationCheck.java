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
        // Never the reader's own settings file: a suite that generates a chart persists it,
        // and one of these once overwrote a saved birth chart. See Settings.useScratchFile.
        Settings.useScratchFile();
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

        before = failures.size();
        everyRingBandAnswers();
        report("Part I - every drawn ring answers a click, and every chord a hover", before);


        before = failures.size();

        oneChipFoldsBothViews();

        report("Part J - one chip folds the mansions in both views", before);



        before = failures.size();


        theWheelAgreesWithTheForm();


        report("Part K - a cold open draws the chart the form holds, and the chips name it",


            before);




        before = failures.size();



        eachWheelReadsItsOwnSubject();



        report("Part L - each wheel reads its own subject, and a chip moves no data", before);





        before = failures.size();




        theSelectionCardIsReadable();




        report("Part M - the selection card carries its own contrast", before);






        before = failures.size();





        anEmptyChartCastsNothing();





        report("Part N - an empty Chart A behaves exactly like an empty Chart B", before);

        System.out.println();
        System.out.println("=== Part O: deselecting a chart promotes the next one inward ===");
        before = failures.size();
        theCompositionFollowsTheChips();
        report("Part O - the chart is whoever is still selected", before);

        System.out.println();
        System.out.println("=== Part P: natal inside, progressed in the middle, the sky outside ===");
        before = failures.size();
        theProgressedTriWheel();
        report("Part P - a progressed chart and the sky can be drawn at once", before);

        System.out.println();
        System.out.println("=== Part G: each ring's glyphs say which ring they are on ===");
        before = failures.size();
        ringColours();
        report("Part G", before);

        System.out.println();
        System.out.println("=== Part H: the app opens onto the chart ===");
        before = failures.size();
        opensOntoTheChart();
        report("Part H", before);

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
    /**
     * Every drawn ring answers a click, and every drawn chord answers a hover.
     *
     * <b>Two defects with one shape: a surface the reader can see and cannot reach.</b>
     *
     * The click handler's ring bands were written for the chain as it stood before the rings
     * were reordered outward and were never re-derived. The sign test ran from the body base
     * out to the sign ring's outer edge - one band swallowing the decans, the bounds and the
     * whole inner degree scale - while the decan test read from the sign ring outward to the
     * body top, which is inside it, so it covered nothing and the decan ring answered with
     * silence. Every radius from 226 to 500 pixels opened "Virgo"; 500 to 520 opened nothing.
     *
     * And the wheel had no aspect hover at all: mouseMoved asked only which body was under the
     * cursor, so the chords - the thing a reader is looking at - lit nothing on any ring. The
     * only place an aspect could be hovered was the grid in the drawer.
     *
     * The first half is checked against radii typed out from the ring chain rather than
     * recomputed from it, so moving a bound fails rather than moving the expectation with it.
     * The second walks a real tri-wheel and asks the panel what is under a point on each ring's
     * chords - which is the assertion the sky ring could not have passed.
     */
    /**
     * The mansion chip folds the ring in the flat wheel and on the globe, and nothing else.
     *
     * <b>One rule, two painters, two hit tests - which is four places to forget.</b> The
     * mansion ring was drawn unconditionally in both views, matching each other and matching
     * nothing else: every ring beside it could be put away and this one could not. Wiring a
     * chip to it means the flat wheel's painter, the globe's painter, the flat wheel's click
     * band and the globe's hit test all have to read the same layer, and a chip that folds
     * three of the four is worse than no chip at all - the reader puts the ring away and it
     * still answers a click, or it vanishes from one view and stays in the other.
     *
     * The flat half is checked by painting the wheel twice and comparing: folding has to
     * change the outer rim and leave the rest of the chart alone, which is the "hides it and
     * changes nothing else" contract stated as pixels rather than as intent.
     */
    /**
     * On a cold open the wheel draws the chart the setup form holds, and the chips say so.
     *
     * <b>The defect this exists to catch.</b> The saved Chart A was restored into the setup
     * form at startup and never handed to the wheel: nothing called generateChart, so
     * applyChartSettings never ran, and the wheel kept the moment its own constructor had
     * defaulted to - now. David opened the app to his natal wheel showing today's sky with the
     * setup screen beside it reading 1982-08-10 the whole time. **The form was right and the
     * wheel was wrong, and neither piece of code was: they had never been introduced.**
     *
     * The invariant asserted here is the one that catches that without depending on what
     * happens to be in this machine's settings.properties - which would be a check measuring
     * its environment rather than the code. **Whatever the form holds, the wheel must agree
     * with it.** Empty and empty is as good a pass as full and full; disagreeing is the
     * failure, and disagreeing is exactly what the defect was.
     *
     * Also pinned: the three chips carry the three names the setup form and the bottom readout
     * use, and a chip whose chart does not exist is not live. A control that looks pressable
     * and changes nothing is the Step-dropdown defect this project keeps logging.
     */
    /**
     * Every per-wheel field comes from the subject that wheel belongs to, and nothing else.
     *
     * <b>This is the assertion the old code could not make.</b> The wheel's three charts were
     * nine loose fields filled by eleven positional strings, and which string meant what
     * depended on the chart mode - so the sky was cast for the second person's birthplace, the
     * partner ring drew this moment, and pressing Play walked a birth time forward. Four
     * faults, one cause: nothing anywhere said that "Chart B" and "the sky" were different
     * things, so writing one into the other was ordinary code.
     *
     * The check is blunt on purpose. Three subjects are built with values that could not be
     * confused - different years, different hemispheres, different zones - installed, and then
     * every field is read back and matched against the subject it names. Swap any two lines of
     * installSubjects and this fails; that is the whole point of it.
     *
     * It also pins the two properties that follow from the type: a subject nobody entered
     * leaves its wheel's moment null rather than defaulting to now, and moving the sky moves
     * only the sky.
     */
    /**
     * The card that opens when a point is clicked is readable where it is shown.
     *
     * <b>Measured off pixels, because a colour in a stylesheet is an intention.</b> The card
     * is built by hoverHtml and set neither a background nor an ink colour, so it inherited
     * whatever it landed on. As a tooltip over the wheel the default black was fine; the
     * Selection drawer builds on the same fragment and is dark, so the point's name - "Sun",
     * "Saturn" - came out black on near-black. David could not read it.
     *
     * One fragment on two surfaces has to carry its own contrast rather than borrow one.
     * Removing either declaration puts the ground back to white and the contrast to nothing,
     * which is what this measures rather than asserts.
     */
    /**
     * A chart with nobody in it is not drawn, and is not called somebody's.
     *
     * <b>David's rule, and it is a symmetry.</b> "If no chart B is seen no chart B should be
     * formed - such should be the same for chart A as well. If no persons data is selected to
     * go into a or b then neither would be selectable nor cast a chart, only thing that could
     * is the sky because it defaults to current astrological chart."
     *
     * Chart B already behaved: no data, no ring. Chart A did not, and the ways it did not were
     * all namings rather than drawings - the wheel drew the sky when Chart A was empty, which
     * is right, and then the placements drawer headed it "Natal Chart" and the grid called
     * itself the "Natal Aspects Grid". A reader would have believed the sky was their birth
     * chart, which is the same mislabelling that started this week, from the other end.
     *
     * Driven by installing subjects rather than by typing in the form, so it needs no
     * geocoder and says the same thing on any machine.
     */
    private static void anEmptyChartCastsNothing() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            java.lang.reflect.Field fr = SkymapPanel.class.getDeclaredField("ringBar");
            fr.setAccessible(true);
            RingBar bar = (RingBar) fr.get(sky);

            java.lang.reflect.Method install = SkymapPanel.class.getDeclaredMethod(
                "installSubjects", com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class);
            install.setAccessible(true);

            com.zodiacomputing.ourania.astro.ChartSubject skyNow =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Sky",
                    java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Los_Angeles")),
                    "Los Angeles", 34.05, -118.24, "America/Los_Angeles", false);
            com.zodiacomputing.ourania.astro.ChartSubject person =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart A",
                    java.time.ZonedDateTime.of(1982, 8, 10, 15, 1, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Philadelphia", 39.95, -75.16, "America/New_York", false);

            Class<?> partType = Class.forName(
                "com.zodiacomputing.ourania.gui.SkymapPanel$PlacementPart");
            Object whole = Enum.valueOf(partType.asSubclass(Enum.class), "ALL");
            java.lang.reflect.Method drawer = SkymapPanel.class.getDeclaredMethod(
                "generatePlanetPlacementsHtml", partType);
            drawer.setAccessible(true);

            // ---- nobody in either chart ----
            install.invoke(sky,
                com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart A"),
                com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart B"), skyNow);
            SwingUtilities.invokeAndWait(() -> sky.updateChartData());
            Thread.sleep(1500);
            SwingUtilities.invokeAndWait(() -> w[0].syncRingBar(bar));

            ok("with nobody entered, Chart A is not selectable",
                !chipAvailable(bar, "Chart A"));
            ok("with nobody entered, Chart B is not selectable",
                !chipAvailable(bar, "Chart B"));
            ok("the sky is always selectable, because it defaults to now",
                chipAvailable(bar, "Sky"));
            ok("the wheel does not claim to hold a Chart A", !sky.hasChartA());
            ok("Chart B forms no outer ring", live(sky.tValid) == 0);

            String empty = (String) drawer.invoke(sky, whole);
            ok("nothing calls the wheel a natal chart", !empty.contains("Natal Chart"));
            ok("nor its grid a natal grid", !empty.contains("Natal Aspects Grid"));
            ok("the drawer names it as the sky", empty.contains("The Sky Now"));
            ok("and its grid as the sky's", empty.contains("Sky Aspects Grid"));
            ok("the wheel is drawing the sky's own moment",
                skyNow.moment.equals(get(sky, "baseChartTime")));

            // ---- and with somebody in Chart A, the naming comes back ----
            install.invoke(sky, person,
                com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart B"), skyNow);
            SwingUtilities.invokeAndWait(() -> sky.updateChartData());
            Thread.sleep(1500);
            SwingUtilities.invokeAndWait(() -> w[0].syncRingBar(bar));

            ok("with a Chart A entered it is selectable", chipAvailable(bar, "Chart A"));
            ok("and Chart B is still not", !chipAvailable(bar, "Chart B"));
            String filled = (String) drawer.invoke(sky, whole);
            ok("and now it is a natal chart", filled.contains("Natal Chart"));
            ok("with a natal grid", filled.contains("Natal Aspects Grid"));
            ok("drawing the person's moment",
                person.moment.equals(get(sky, "baseChartTime")));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static void theSelectionCardIsReadable() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            awaitChart(sky, false);

            java.lang.reflect.Method card = SkymapPanel.class.getDeclaredMethod(
                "selectionHtml", int.class, double.class, double.class, boolean.class);
            card.setAccessible(true);
            String html = (String) card.invoke(sky, 0, 165.0, 0.98, false);
            ok("the selection card has content", html != null && html.length() > 200);

            final javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
            final String doc = html;
            SwingUtilities.invokeAndWait(() -> {
                pane.setContentType("text/html");
                pane.setText(doc);
                pane.setSize(300, 900);
                pane.doLayout();
            });
            Thread.sleep(500);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                300, 300, java.awt.image.BufferedImage.TYPE_INT_RGB);
            SwingUtilities.invokeAndWait(() -> {
                java.awt.Graphics2D g = img.createGraphics();
                pane.paint(g);
                g.dispose();
            });

            // The strip where the point's name sits: its commonest luminance is the ground,
            // and the lightest is the ink.
            java.util.Map<Integer, Integer> tally = new java.util.HashMap<>();
            int lightest = 0;
            for (int y = 0; y < 40; y++) {
                for (int x = 0; x < 300; x++) {
                    int rgb = img.getRGB(x, y);
                    int lum = (((rgb >> 16) & 255) * 30 + ((rgb >> 8) & 255) * 59
                        + (rgb & 255) * 11) / 100;
                    lightest = Math.max(lightest, lum);
                    tally.merge(lum, 1, Integer::sum);
                }
            }
            int ground = 0;
            int most = 0;
            for (java.util.Map.Entry<Integer, Integer> e : tally.entrySet()) {
                if (e.getValue() > most) {
                    most = e.getValue();
                    ground = e.getKey();
                }
            }
            int contrast = Math.abs(lightest - ground);
            System.out.println("  the point's name: ground " + ground + ", ink up to "
                + lightest + ", contrast " + contrast);
            ok("the card paints its own dark ground rather than a borrowed white one",
                ground < 80);
            ok("and its name stands off that ground", contrast > 90);

            // <b>And its headings open, after the cursor has left the wheel.</b> David: "there
            // are these expanders but none of them open." They opened from the hover focus,
            // and the hover focus is not the card - it follows the cursor, and the cursor has
            // to cross the wheel to reach the drawer the card is in. The move put the focus on
            // nothing, the redraw bailed out on that, and every heading on the card went dead
            // without any of them looking dead. So the card is rebuilt here from a focus that
            // has already gone, which is the state the reader is always in by the time they
            // click one.
            java.lang.reflect.Method setFocus = SkymapPanel.class.getDeclaredMethod(
                "setFocus", int.class);
            setFocus.setAccessible(true);
            java.lang.reflect.Field fb = SkymapPanel.class.getDeclaredField("focusBody");
            fb.setAccessible(true);
            java.lang.reflect.Field sp = OuraniaWindow.class.getDeclaredField("selectionPane");
            sp.setAccessible(true);
            javax.swing.JEditorPane live = (javax.swing.JEditorPane) sp.get(w[0]);

            SwingUtilities.invokeAndWait(() -> w[0].showSelection(doc));
            setFocus.invoke(sky, Integer.valueOf(-1));
            same("the cursor has left every body", Integer.valueOf(-1), fb.get(sky));
            final String[] seen = new String[2];
            SwingUtilities.invokeAndWait(() -> seen[0] = live.getText());
            SwingUtilities.invokeAndWait(() ->
                w[0].handlePlacementClick(SkymapPanel.SELECT_EXPAND + "core"));
            SwingUtilities.invokeAndWait(() -> seen[1] = live.getText());
            ok("a heading opens even though nothing is under the cursor",
                seen[0] != null && !seen[0].equals(seen[1]));
            // And closes again, so the same click is a toggle rather than a one-way door.
            SwingUtilities.invokeAndWait(() ->
                w[0].handlePlacementClick(SkymapPanel.SELECT_EXPAND + "core"));
            final String[] shut = new String[1];
            SwingUtilities.invokeAndWait(() -> shut[0] = live.getText());
            ok("and closes again on a second click", shut[0].equals(seen[0]));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static void eachWheelReadsItsOwnSubject() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);

            com.zodiacomputing.ourania.astro.ChartSubject a =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart A",
                    java.time.ZonedDateTime.of(1911, 1, 11, 1, 11, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Aaa", 11.0, -11.0, "America/New_York", true);
            com.zodiacomputing.ourania.astro.ChartSubject b =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart B",
                    java.time.ZonedDateTime.of(1922, 2, 22, 2, 22, 0, 0,
                        java.time.ZoneId.of("Europe/Paris")),
                    "Bbb", 22.0, -22.0, "Europe/Paris", false);
            com.zodiacomputing.ourania.astro.ChartSubject s =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Sky",
                    java.time.ZonedDateTime.of(1933, 3, 3, 3, 33, 0, 0,
                        java.time.ZoneId.of("Asia/Tokyo")),
                    "Sss", 33.0, -33.0, "Asia/Tokyo", false);

            java.lang.reflect.Method install = SkymapPanel.class.getDeclaredMethod(
                "installSubjects", com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class);
            install.setAccessible(true);
            install.invoke(sky, a, b, s);

            // Chart A's fields hold Chart A, and Chart A is the only place they could
            // have come from - 11 is not 22 and is not 33.
            same("Chart A's moment", a.moment, get(sky, "baseChartTime"));
            same("Chart A's latitude", a.latitude, get(sky, "baseLatitude"));
            same("Chart A's longitude", a.longitude, get(sky, "baseLongitude"));
            same("Chart A's place", a.placeName, get(sky, "baseLocationName"));
            same("Chart A's zone", a.zoneId, get(sky, "baseTimeZoneId"));
            same("Chart A's time-unknown flag", a.timeUnknown, get(sky, "baseTimeUnknown"));

            same("Chart B's moment", b.moment, get(sky, "transitChartTime"));
            same("Chart B's latitude", b.latitude, get(sky, "transitLatitude"));
            same("Chart B's longitude", b.longitude, get(sky, "transitLongitude"));
            same("Chart B's place", b.placeName, get(sky, "transitLocationName"));
            same("Chart B's zone", b.zoneId, get(sky, "transitTimeZoneId"));

            same("the sky's moment", s.moment, get(sky, "skyChartTime"));
            same("the sky's latitude", s.latitude, get(sky, "skyLatitude"));
            same("the sky's longitude", s.longitude, get(sky, "skyLongitude"));
            same("the sky's zone", s.zoneId, get(sky, "skyTimeZoneId"));

            // And the subjects themselves come back as they went in.
            ok("Chart A reads back", a.equals(sky.chartASubject()));
            ok("Chart B reads back", b.equals(sky.chartBSubject()));
            ok("the sky reads back", s.equals(sky.skySubject()));

            // <b>A chart nobody entered is not a chart, and what is left moves inward.</b>
            // This used to pin the base slot as null with Chart B left on its own outer ring,
            // which was right while Chart A was the only chart that could be the natal one.
            // Chart B is promoted now - the same rule the chips follow, and the reason Part O
            // exists - so an absent Chart A reads as a Chart B natal chart rather than as a
            // hole with a partner ring around it.
            install.invoke(sky,
                com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart A"), b, s);
            ok("an unentered Chart A promotes Chart B onto the inner wheel",
                b.moment.equals(get(sky, "baseChartTime")));
            ok("and the panel still says there is no Chart A", !sky.hasChartA());
            ok("but the inner wheel is a birth chart", sky.innerIsBirthChart());
            ok("with nothing left on the outer ring", get(sky, "transitChartTime") == null);
            ok("or the sky", s.moment.equals(get(sky, "skyChartTime")));

            // Moving the sky moves the sky.
            install.invoke(sky, a, b, s);
            java.lang.reflect.Method move = SkymapPanel.class.getDeclaredMethod(
                "moveSky", java.time.ZonedDateTime.class);
            move.setAccessible(true);
            java.time.ZonedDateTime later = s.moment.plusDays(400);
            move.invoke(sky, later);
            same("the sky moved", later, get(sky, "skyChartTime"));
            same("Chart A did not", a.moment, get(sky, "baseChartTime"));
            same("Chart B did not", b.moment, get(sky, "transitChartTime"));
            same("and the sky kept its place", s.latitude, get(sky, "skyLatitude"));

            // A chip changes which wheels are drawn and touches nothing on them.
            //
            // Animation off: outerRingDrawn reads a bloom fraction, so a ring that has been
            // told to fold is still partly open for as long as the fold takes. This part is
            // about what the flag says, not how long the animation runs.
            Settings.setAnimateRings(false);
            install.invoke(sky, a, b, s);
            SwingUtilities.invokeAndWait(
                () -> sky.applyChartMode(ChartMode.SYNASTRY, true));
            Thread.sleep(400);
            ok("switching mode draws the outer wheel", sky.outerRingDrawn());
            ok("switching mode draws the sky ring", sky.triRingDrawn());
            ok("and Chart A is untouched", a.equals(sky.chartASubject()));
            ok("and Chart B is untouched", b.equals(sky.chartBSubject()));
            ok("and the sky is untouched", s.equals(sky.skySubject()));
            SwingUtilities.invokeAndWait(
                () -> sky.applyChartMode(ChartMode.SINGLE, false));
            Thread.sleep(400);
            ok("switching back folds the outer wheel", !sky.outerRingDrawn());
            ok("and still nothing moved", a.equals(sky.chartASubject())
                && b.equals(sky.chartBSubject()) && s.equals(sky.skySubject()));
            Settings.setAnimateRings(true);
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static Object get(SkymapPanel sky, String name) throws Exception {
        java.lang.reflect.Field f = SkymapPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(sky);
    }

    /** Object equality with the value printed, since eq here is for ints. */
    private static void same(String label, Object expected, Object actual) {
        ok(label + ": expected " + expected + ", got " + actual,
            expected == null ? actual == null : expected.equals(actual));
    }

    private static void theWheelAgreesWithTheForm() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            java.lang.reflect.Field fc =
                OuraniaWindow.class.getDeclaredField("chartSetupPanel");
            fc.setAccessible(true);
            Object setup = fc.get(w[0]);

            // <b>Through the same door the application uses.</b> main calls drawSavedChart
            // after making the window visible; constructing a window does not draw anything,
            // which is what keeps four other suites out of the ephemeris. Calling it here is
            // this check standing where the reader stands.
            SwingUtilities.invokeAndWait(() -> w[0].drawSavedChart());
            awaitChart(sky, false);

            java.lang.reflect.Method formHasA = setup.getClass()
                .getDeclaredMethod("hasChartA");
            formHasA.setAccessible(true);
            boolean formA = (Boolean) formHasA.invoke(setup);
            System.out.println("  cold open: the form holds a Chart A = " + formA
                + ", the wheel holds one = " + sky.hasChartA());
            ok("the wheel and the form agree about whether there is a Chart A",
                formA == sky.hasChartA());

            // And when there is one, the wheel is drawing it rather than this moment.
            if (formA) {
                java.lang.reflect.Field fd = setup.getClass()
                    .getDeclaredField("baseDateField");
                fd.setAccessible(true);
                String formDate =
                    ((javax.swing.text.JTextComponent) fd.get(setup)).getText().trim();
                java.lang.reflect.Field bt =
                    SkymapPanel.class.getDeclaredField("baseChartTime");
                bt.setAccessible(true);
                Object drawn = bt.get(sky);
                ok("the wheel has a moment to draw", drawn != null);
                String drawnDate = drawn == null ? "" : drawn.toString().substring(0, 10);
                System.out.println("  the form says " + formDate
                    + ", the wheel is drawing " + drawnDate);
                eq("the wheel draws the date the form holds", formDate, drawnDate);
                ok("and it is not simply today",
                    !drawnDate.equals(java.time.LocalDate.now().toString()));
            }

            // The chips carry the names the form and the readout use.
            java.lang.reflect.Field fr = SkymapPanel.class.getDeclaredField("ringBar");
            fr.setAccessible(true);
            RingBar bar = (RingBar) fr.get(sky);
            ok("the wheel has a ring bar", bar != null);
            java.util.List<String> labels = chipLabels(bar);
            System.out.println("  chips: " + labels);
            for (String want : new String[] {"Chart A", "Chart B", "Sky"}) {
                ok("a chip is named " + want, labels.contains(want));
            }
            for (String gone : new String[] {"Natal", "Partner"}) {
                ok("no chip is still named " + gone, !labels.contains(gone));
            }

            // A chip for a chart that does not exist is not live.
            java.lang.reflect.Method formHasB = setup.getClass()
                .getDeclaredMethod("hasPartnerData");
            formHasB.setAccessible(true);
            ok("the Chart A chip is available exactly when a Chart A is",
                chipAvailable(bar, "Chart A") == formA);

            // <b>And each chip names the chart it would draw.</b> They described what pressing
            // them did and never which chart it was, so the only way to learn what sat on a
            // wheel was to open the setup screen - in an app whose defining bug was a
            // confusion about exactly that. The summary is the subject's own, so a tooltip
            // that drifts from the wheel fails here.
            SwingUtilities.invokeAndWait(() -> w[0].drawSavedChart());
            awaitChart(sky, false);
            for (Object[] pair : new Object[][] {
                    {"Chart A", sky.chartASubject()},
                    {"Chart B", sky.chartBSubject()},
                    {"Sky", sky.skySubject()}}) {
                String name = (String) pair[0];
                com.zodiacomputing.ourania.astro.ChartSubject subject =
                    (com.zodiacomputing.ourania.astro.ChartSubject) pair[1];
                String tip = chipTip(bar, name);
                ok(name + "'s chip has hover text", tip != null && !tip.isEmpty());
                String plain = tip == null ? "" : tip.replaceAll("<[^>]*>", " ");
                ok(name + "'s chip names itself", plain.contains(name));
                if (subject.entered()) {
                    ok(name + "'s chip names the chart it would draw: " + subject.summary(),
                        plain.contains(subject.summary()));
                } else {
                    ok(name + "'s chip says there is no chart yet",
                        plain.contains("not entered"));
                }
            }
            ok("the Chart B chip is available exactly when a Chart B is",
                chipAvailable(bar, "Chart B") == (Boolean) formHasB.invoke(setup));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    /** Every chip's label, in the order the bar holds them. */
    /**
     * Taking a chart out of the selection changes what kind of chart it is.
     *
     * <b>Chart A's chip used to hide its glyphs and leave it in the chart.</b> So a synastry
     * with Chart A deselected was still a synastry: still cast from two people, still drawing
     * cross-aspects to a wheel that was not on screen, still calling itself one in the readout.
     * David walked the sequence he expected instead - "I deselect Chart A, now I am no longer
     * seeing a synastry, I am seeing a natal-transit chart... I then decide to deselect Chart
     * B, I am seeing a current time and place natal chart... now I select back on Chart A and
     * the chart now becomes a natal transit chart again" - and this is that walk, step for
     * step.
     *
     * The chips are checked at each step in the same breath as the mode, because "the chart
     * changed" and "the control says so" are the two halves that this panel has repeatedly had
     * come apart, always in the direction of a lit chip over a wheel that does not have it.
     */
    private static void theCompositionFollowsTheChips() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            java.lang.reflect.Field fc = OuraniaWindow.class.getDeclaredField("chartSetupPanel");
            fc.setAccessible(true);
            ChartSetupPanel setup = (ChartSetupPanel) fc.get(hold[0]);
            Thread.sleep(2500);

            // Coordinates rather than place names: a check that geocodes is a check that fails
            // on a train. Two real charts, so both chips have something behind them.
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                textField(setup, "baseDateField").setText("1982-08-10");
                textField(setup, "baseTimeField").setText("15:01");
                textField(setup, "baseLocationField").setText("39.95, -75.16");
                textField(setup, "transitDateField").setText("1979-03-22");
                textField(setup, "transitTimeField").setText("08:40");
                textField(setup, "transitLocationField").setText("51.51, -0.13");
            });
            Class<?> subj = Class.forName(
                "com.zodiacomputing.ourania.gui.ChartSetupPanel$Subject");
            java.lang.reflect.Method setSubject =
                ChartSetupPanel.class.getDeclaredMethod("setSubject", subj);
            setSubject.setAccessible(true);
            Object partnership = null;
            for (Object o : subj.getEnumConstants()) {
                if (((Enum<?>) o).name().equals("PARTNERSHIP")) {
                    partnership = o;
                }
            }
            final Object ps = partnership;
            java.lang.reflect.Method gen =
                ChartSetupPanel.class.getDeclaredMethod("generateChart");
            gen.setAccessible(true);
            final Exception[] blew = new Exception[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    setSubject.invoke(setup, ps);
                    gen.invoke(setup);
                } catch (Exception e) {
                    blew[0] = e;
                }
            });
            ok("the two charts generate", blew[0] == null);
            Thread.sleep(3000);
            ok("the wheel is holding a Chart A", hold[0].wheelHasChartA());
            ok("the wheel is holding a Chart B", hold[0].wheelHasChartB());

            java.lang.reflect.Field fr = SkymapPanel.class.getDeclaredField("ringBar");
            fr.setAccessible(true);
            RingBar bar = (RingBar) fr.get(panel);

            step(hold[0], panel, bar, true, true, true, ChartMode.SYNASTRY, "Chart A", true);
            ok("all three rings are drawn in a synastry with the sky", panel.triRingDrawn());

            step(hold[0], panel, bar, false, true, true, ChartMode.TRANSIT, "Chart B", true);
            ok("Chart B's transit chart has no third ring", !panel.triRingDrawn());

            step(hold[0], panel, bar, false, false, true, ChartMode.SINGLE, null, true);
            ok("the sky alone has no outer ring", !panel.outerRingDrawn());

            step(hold[0], panel, bar, true, false, true, ChartMode.TRANSIT, "Chart A", true);
            step(hold[0], panel, bar, true, false, false, ChartMode.SINGLE, "Chart A", false);
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * One press of the chips, and everything that should follow from it.
     *
     * @param anchorLabel whose chart is on the inner wheel, or null when it is the sky
     */
    private static void step(OuraniaWindow w, SkymapPanel panel, RingBar bar, boolean a,
            boolean b, boolean sky, ChartMode mode, String anchorLabel, boolean skyExpected)
            throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> w.setRings(a, b, sky));
        Thread.sleep(1600);
        String what = (a ? "A" : "-") + (b ? "B" : "-") + (sky ? "S" : "-");

        java.lang.reflect.Field fm = SkymapPanel.class.getDeclaredField("chartMode");
        fm.setAccessible(true);
        java.lang.reflect.Field fa = SkymapPanel.class.getDeclaredField("anchorSubject");
        fa.setAccessible(true);
        Object anchor = fa.get(panel);
        String on = anchor == null ? null
            : ((com.zodiacomputing.ourania.astro.ChartSubject) anchor).label;
        System.out.println("  " + what + " -> " + fm.get(panel) + ", inner wheel "
            + (on == null ? "the sky" : on));

        ok(what + " casts a " + mode, fm.get(panel) == mode);
        ok(what + " puts " + (anchorLabel == null ? "the sky" : anchorLabel)
            + " on the inner wheel", java.util.Objects.equals(on, anchorLabel));

        // And the chips agree with it. A lit chip over a chart the wheel does not have is the
        // defect this whole part exists to catch, so it is checked in both directions.
        ok(what + ": Chart A's chip is " + (a ? "lit" : "dark"), chipLit(bar, "Chart A") == a);
        ok(what + ": Chart B's chip is " + (b ? "lit" : "dark"), chipLit(bar, "Chart B") == b);
        ok(what + ": the Sky chip is " + (skyExpected ? "lit" : "dark"),
            chipLit(bar, "Sky") == skyExpected);
    }

    /** Whether a chip is drawn as open - the same call its own painter makes. */
    private static boolean chipLit(RingBar bar, String label) throws Exception {
        for (java.awt.Component c : bar.getComponents()) {
            if (label.equals(labelOf(c))) {
                java.lang.reflect.Method m = c.getClass().getDeclaredMethod("open");
                m.setAccessible(true);
                return (Boolean) m.invoke(c);
            }
        }
        return false;
    }

    private static javax.swing.JTextField textField(ChartSetupPanel p, String name) {
        try {
            java.lang.reflect.Field f = ChartSetupPanel.class.getDeclaredField(name);
            f.setAccessible(true);
            return (javax.swing.JTextField) f.get(p);
        } catch (Exception e) {
            throw new RuntimeException(name, e);
        }
    }

    /**
     * A progressed chart and the sky, on the same wheel.
     *
     * <b>They used to compete for one ring.</b> The middle ring carried transits or
     * progressions - a Settings choice between them - so natal, progressed and transiting could
     * not be read together, which is the standard way the technique is read. Everything needed
     * already existed: Progressions.progressedJd casts the ring, the third ring exists for
     * synastry, and the reason the third ring was refused was that its rule named the synastry
     * case rather than the reason for it.
     *
     * The engine half is asserted through {@code Progressions.progressedJd} itself rather than
     * against numbers written here - a day of ephemeris for a year of life is the definition,
     * and a check that restates the arithmetic is a check that passes when the arithmetic and
     * the restatement are wrong together.
     */
    private static void theProgressedTriWheel() throws Exception {
        // The rule, first, so a failure below reads as the wiring rather than the rule.
        ok("a synastry with transits is still a tri-wheel",
            SkymapPanel.triWheelShown(ChartMode.SYNASTRY, true, false));
        ok("a progressed transit chart is now one too",
            SkymapPanel.triWheelShown(ChartMode.TRANSIT, true, true));
        ok("but a plain transit chart is not",
            !SkymapPanel.triWheelShown(ChartMode.TRANSIT, true, false));
        ok("and neither is a progressed chart with no sky asked for",
            !SkymapPanel.triWheelShown(ChartMode.TRANSIT, false, true));
        ok("a composite is left alone",
            !SkymapPanel.triWheelShown(ChartMode.COMPOSITE_MIDPOINT, true, true));

        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);

            com.zodiacomputing.ourania.astro.ChartSubject a =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Chart A",
                    java.time.ZonedDateTime.of(1982, 8, 10, 15, 1, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Philadelphia", 39.95, -75.16, "America/New_York", false);
            com.zodiacomputing.ourania.astro.ChartSubject sky =
                com.zodiacomputing.ourania.astro.ChartSubject.of("Sky",
                    java.time.ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0,
                        java.time.ZoneId.of("America/New_York")),
                    "Philadelphia", 39.95, -75.16, "America/New_York", false);
            java.lang.reflect.Method install = SkymapPanel.class.getDeclaredMethod(
                "installSubjects", com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class,
                com.zodiacomputing.ourania.astro.ChartSubject.class);
            install.setAccessible(true);
            install.invoke(panel, a,
                com.zodiacomputing.ourania.astro.ChartSubject.empty("Chart B"), sky);

            // Progressions on, the way the Settings screen turns them on.
            Settings.setOuterWheel(Settings.OUTER_PROGRESSED);
            panel.reloadAspectSelection();
            set(panel, "chartMode", ChartMode.TRANSIT);
            set(panel, "transitsEnabled", Boolean.TRUE);
            java.lang.reflect.Method flags =
                SkymapPanel.class.getDeclaredMethod("applyRingFlags");
            flags.setAccessible(true);
            flags.invoke(panel);
            panel.updateChartData();
            Thread.sleep(1500);

            ok("the middle ring is drawn", panel.outerRingDrawn());
            ok("and the sky has a ring of its own", panel.triRingDrawn());

            // <b>Three rings with three different sets of positions.</b> The rings could all be
            // drawn and two of them be the same chart - which is what would happen if the
            // progressed ring were cast at the transit date after all.
            int sameAsNatal = 0;
            int sameAsSky = 0;
            int compared = 0;
            for (int i = 0; i < SkymapPanel.BODY_COUNT; i++) {
                if (!panel.bValid[i] || !panel.tValid[i] || !panel.cValid[i]) {
                    continue;
                }
                // <b>The angles are supposed to match.</b> A progressed bi-wheel is progressed
                // bodies around the NATAL frame - the cusps are deliberately copied rather than
                // recast, which is a documented choice in updateChartData - so the Ascendant
                // and Midheaven on the middle ring are the natal ones and always will be.
                // Counting them as "the natal chart again" asserted against the technique.
                if (com.zodiacomputing.ourania.astro.Bodies.at(i).isAngle()) {
                    continue;
                }
                compared++;
                if (Math.abs(panel.tLon[i] - panel.bLon[i]) < 0.001) {
                    sameAsNatal++;
                }
                if (Math.abs(panel.tLon[i] - panel.cLon[i]) < 0.001) {
                    sameAsSky++;
                }
            }
            System.out.println("  " + compared + " bodies on all three rings; middle matches "
                + "natal for " + sameAsNatal + ", matches the sky for " + sameAsSky);
            ok("there are bodies on all three rings", compared > 5);
            ok("the middle ring is not the natal chart again", sameAsNatal == 0);
            ok("nor the sky again", sameAsSky == 0);

            // <b>And it is progressed, not merely different.</b> A day for a year: the Sun moves
            // about a degree a progressed year, so forty-three years of life is a Sun somewhere
            // between thirty and fifty degrees on from birth - and nothing else the middle ring
            // could be carrying puts it there.
            double sunMoved = ((panel.tLon[0] - panel.bLon[0]) % 360.0 + 360.0) % 360.0;
            System.out.printf("  the progressed Sun has moved %.1f degrees from natal%n",
                sunMoved);
            ok("the progressed Sun has moved about a degree per year of life",
                sunMoved > 30.0 && sunMoved < 55.0);

            // The readouts name the ring rather than calling it a transit chart.
            java.lang.reflect.Method word = SkymapPanel.class.getDeclaredMethod(
                "ringWord", int.class);
            word.setAccessible(true);
            ok("the middle ring is called progressed",
                "progressed".equals(word.invoke(panel, SkymapPanel.WHEEL_OUTER)));
            ok("and the outer one is still the sky",
                "sky".equals(word.invoke(panel, SkymapPanel.WHEEL_SKY)));

            // <b>And back.</b> Turning progressions off has to give the ring back to the sky,
            // or the reader is left with a third ring and nothing on it.
            Settings.setOuterWheel(Settings.OUTER_TRANSITS);
            panel.reloadAspectSelection();
            // <b>Wait for the ring to finish closing, not for a number of milliseconds.</b>
            // triRingDrawn asks whether the ring is on SCREEN, and a ring that has been told to
            // close is still on screen while it folds - so a fixed sleep here is a race between
            // this check and an animation, which is the same mistake the clock notice check
            // made. Poll, with a timeout, so a ring that never closes still fails.
            long deadline = System.currentTimeMillis() + 8000;
            while (panel.triRingDrawn() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }
            ok("turning progressions off closes the third ring", !panel.triRingDrawn());
            ok("and the sky is back on the middle one", panel.outerRingDrawn());
        } finally {
            Settings.setOuterWheel(Settings.OUTER_TRANSITS);
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    private static java.util.List<String> chipLabels(RingBar bar) throws Exception {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (java.awt.Component c : bar.getComponents()) {
            String label = labelOf(c);
            if (label != null) {
                out.add(label);
            }
        }
        return out;
    }

    /** One chip's hover text, or null. */
    private static String chipTip(RingBar bar, String label) throws Exception {
        for (java.awt.Component c : bar.getComponents()) {
            if (label.equals(labelOf(c))) {
                return ((javax.swing.JComponent) c).getToolTipText();
            }
        }
        return null;
    }

    private static boolean chipAvailable(RingBar bar, String label) throws Exception {
        for (java.awt.Component c : bar.getComponents()) {
            if (label.equals(labelOf(c))) {
                java.lang.reflect.Field f = c.getClass().getDeclaredField("available");
                f.setAccessible(true);
                return (Boolean) f.get(c);
            }
        }
        return false;
    }

    private static String labelOf(java.awt.Component c) throws Exception {
        try {
            java.lang.reflect.Field f = c.getClass().getDeclaredField("label");
            f.setAccessible(true);
            return (String) f.get(c);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static void oneChipFoldsBothViews() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            boolean[] every = new boolean[com.zodiacomputing.ourania.astro.Bodies.ALL.length];
            java.util.Arrays.fill(every, true);
            set(sky, "shown", every);
            buildChart(sky, ChartMode.TRANSIT, false);
            Settings.setAnimateRings(false);
            try {
                java.awt.Component wheel = sky.chartComponent();
                final int size = 1100;
                SwingUtilities.invokeAndWait(() -> {
                    wheel.setSize(size, size);
                    wheel.doLayout();
                });

                ok("the mansion layer starts open",
                    sky.layerWanted(SkymapPanel.Layer.MANSIONS)
                        && sky.layerShown(SkymapPanel.Layer.MANSIONS));

                java.awt.image.BufferedImage open = flatFrame(wheel, size);
                SwingUtilities.invokeAndWait(
                    () -> sky.setLayer(SkymapPanel.Layer.MANSIONS, false));
                ok("the chip folds it", !sky.layerShown(SkymapPanel.Layer.MANSIONS));
                java.awt.image.BufferedImage folded = flatFrame(wheel, size);

                // Where the two frames differ, and how far out those pixels are.
                //
                // The window is the band plus a few pixels inward, because the station numbers
                // are centred in a band nine pixels deep and drawn at nine points - a glyph
                // taller than the band it sits in, overhanging its inner edge by two. Measured
                // rather than assumed: the first window stopped at the band and this reported
                // forty-three strays at radius 527 to 529, which is the numbers and nothing
                // else. Widening it to hide a real leak would be the wrong move; widening it
                // to describe the shape that is actually drawn is the right one.
                int[] rings = SkymapPanel.ringRadii(size, size, 1.0, 1.0);
                int outer = rings[SkymapPanel.RING_OUTER];
                int changed = 0;
                int strayed = 0;
                double strayLo = Double.MAX_VALUE;
                double strayHi = 0;
                double half = size / 2.0;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        if (open.getRGB(x, y) == folded.getRGB(x, y)) {
                            continue;
                        }
                        changed++;
                        // A pixel of the band, allowing a pixel of antialiasing either side.
                        double r = Math.hypot(x - half, y - half);
                        if (r < outer - 15 || r > outer + 2) {
                            strayed++;
                            strayLo = Math.min(strayLo, r);
                            strayHi = Math.max(strayHi, r);
                        }
                    }
                }
                System.out.println("  folding the mansions changed " + changed
                    + " pixels of the flat wheel, " + strayed + " of them outside the band"
                    + (strayed == 0 ? "" : String.format(" (radius %.0f to %.0f, band is %d to %d)",
                        strayLo, strayHi, outer - 15, outer + 2)));
                ok("folding the mansions changes the flat wheel", changed > 500);
                ok("and changes nothing outside the band it draws", strayed == 0);

                // The flat wheel's click band goes with it.
                java.lang.reflect.Method inBand = SkymapPanel.class.getDeclaredMethod(
                    "inMansionBand", double.class, int.class);
                inBand.setAccessible(true);
                ok("the band the click reads is the band that was drawn",
                    (Boolean) inBand.invoke(null, (double) (outer - 4), outer));

                // And the globe's hit test, which is the half that could silently outlive it.
                Globe cam = new Globe();
                double origin = sky.pinLongitude();
                double mid = (Globe.SHELL_MANSION_INNER + Globe.SHELL_MANSION_OUTER) / 2.0;
                double[] pt = Globe.onShell(40.0, origin, mid, 0.0);
                Globe.Projected q = cam.project(pt[0], pt[1], pt[2], size, size);
                ok("the sample point is on screen", q.visible);
                int px = (int) Math.round(q.x);
                int py = (int) Math.round(q.y);
                ok("a folded band answers no hover on the globe",
                    GlobeRenderer.mansionAt(cam, size, size, sky, px, py) < 0);

                SwingUtilities.invokeAndWait(
                    () -> sky.setLayer(SkymapPanel.Layer.MANSIONS, true));
                ok("the chip brings it back", sky.layerShown(SkymapPanel.Layer.MANSIONS));
                ok("and the globe answers again",
                    GlobeRenderer.mansionAt(cam, size, size, sky, px, py) >= 1);

                java.awt.image.BufferedImage back = flatFrame(wheel, size);
                int stillDifferent = 0;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        if (open.getRGB(x, y) != back.getRGB(x, y)) {
                            stillDifferent++;
                        }
                    }
                }
                ok("unfolding puts the wheel back exactly as it was", stillDifferent == 0);
            } finally {
                Settings.setAnimateRings(true);
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    /**
     * Builds a chart by writing the panel's own fields, without a geocoder.
     *
     * <b>These parts used to go through applyChartSettings, which geocodes.</b> That reaches
     * nominatim.openstreetmap.org, and a suite that needs the internet is a suite measuring
     * the machine it runs on - the same defect as a suite reading the user's settings, which
     * this project has logged twice. When the network was up Part I found 417 chords; when it
     * was down it found nought and reported three failures against code that was fine.
     *
     * Coordinates are typed in, so the chart is the same chart on any machine, on any network.
     */
    private static void buildChart(SkymapPanel sky, ChartMode mode, boolean sky3)
            throws Exception {
        set(sky, "chartMode", mode);
        set(sky, "showTransitChart", SkymapPanel.outerWheelShown(mode, true));
        set(sky, "showTriWheel", SkymapPanel.triWheelShown(mode, sky3));
        set(sky, "innerIsBirthChart", Boolean.TRUE);
        set(sky, "baseChartTime", java.time.ZonedDateTime.of(
            1972, 9, 22, 18, 38, 0, 0, java.time.ZoneId.of("America/Los_Angeles")));
        set(sky, "baseLatitude", 34.0522);
        set(sky, "baseLongitude", -118.2437);
        set(sky, "baseTimeZoneId", "America/Los_Angeles");
        set(sky, "transitChartTime", java.time.ZonedDateTime.of(
            1975, 3, 14, 9, 20, 0, 0, java.time.ZoneId.of("America/New_York")));
        set(sky, "transitLatitude", 39.9526);
        set(sky, "transitLongitude", -75.1652);
        set(sky, "transitTimeZoneId", "America/New_York");
        set(sky, "skyChartTime", java.time.ZonedDateTime.now(
            java.time.ZoneId.of("America/Los_Angeles")));
        set(sky, "skyLatitude", 34.0522);
        set(sky, "skyLongitude", -118.2437);
        set(sky, "skyTimeZoneId", "America/Los_Angeles");
        set(sky, "aspectFilter", "Both");
        // <b>And the aspects themselves, for the same reason as the coordinates.</b> Which
        // aspects are drawn is a reader's preference; a suite that inherits it is measuring
        // the machine, and AspectGridCheck pins this already for exactly that reason.
        boolean[] allAspects =
            new boolean[com.zodiacomputing.ourania.astro.Aspects.Type.values().length];
        java.util.Arrays.fill(allAspects, true);
        set(sky, "aspectShown", allAspects);
        SwingUtilities.invokeAndWait(sky::updateChartData);
        Thread.sleep(1200);
    }

    /**
     * Waits for the chart worker to finish, rather than sleeping and hoping.
     *
     * <b>A check whose totals move is a check that is measuring the machine.</b> These parts
     * slept a fixed six or seven seconds after asking for a chart, which is long enough on an
     * idle machine and not long enough while a compile is running: the same suite reported 417
     * chords under the cursor on one run and 87 on the next, and passed both times because its
     * assertions are per-ring rather than per-count. The assertions were fine; the sample was
     * not, and a sample that shrinks by four fifths is not testing what it claims to.
     *
     * GlobeCheck Part E had this exact defect and was fixed the same way. Waiting on the
     * arrays being populated, then letting the blooms settle, makes the count repeatable.
     */
    private static void awaitChart(SkymapPanel sky, boolean needSky) throws Exception {
        // <b>Populated is not the same as finished.</b> Waiting for the arrays to be non-empty
        // still caught the worker mid-flight - the sample came back 417, 417, then 407 - so
        // this waits for the counts to stop moving, which is the only signal from outside that
        // says the work is done rather than merely started.
        long deadline = System.currentTimeMillis() + 40000;
        String last = "";
        int steady = 0;
        while (System.currentTimeMillis() < deadline) {
            String now = live(sky.bValid) + "/" + live(sky.tValid) + "/" + live(sky.cValid);
            boolean enough = live(sky.bValid) > 0 && live(sky.tValid) > 0
                && (!needSky || live(sky.cValid) > 0);
            steady = now.equals(last) ? steady + 1 : 0;
            last = now;
            if (enough && steady >= 5) {
                break;
            }
            Thread.sleep(100);
        }
        // The rings and layers animate open; a still taken mid-bloom is a still of a chart
        // half drawn, and triRingDrawn is a bloom fraction rather than a flag.
        Thread.sleep(1500);
        // <b>Only the wheels this chart actually has.</b> Demanding an outer wheel of every
        // chart fails a single chart for not being two - which is what it reported on the
        // cold-open part, where SINGLE is the whole point.
        ok("the chart under test finished computing: natal " + live(sky.bValid)
            + ", outer " + live(sky.tValid) + ", sky " + live(sky.cValid),
            live(sky.bValid) > 0
                && (!sky.outerRingDrawn() || live(sky.tValid) > 0)
                && (!needSky || live(sky.cValid) > 0));
    }

    private static int live(boolean[] valid) {
        int n = 0;
        for (boolean b : valid) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    /** One frame of the flat wheel, painted offscreen. */
    private static java.awt.image.BufferedImage flatFrame(java.awt.Component wheel, int size)
            throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = img.createGraphics();
            wheel.paint(g);
            g.dispose();
        });
        return img;
    }

    private static void everyRingBandAnswers() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);

            // ---- half one: the bands, on the chain as the painter lays it out ----
            int[] rings = SkymapPanel.ringRadii(1100, 1100, 1.0, 1.0);
            int decanOuter = rings[SkymapPanel.RING_DECAN_OUTER];
            int signOuter = rings[SkymapPanel.RING_SIGN_OUTER];
            int signInner = rings[SkymapPanel.RING_SIGN_INNER];
            int termInner = rings[SkymapPanel.RING_TERM_INNER];
            int degreeInner = rings[SkymapPanel.RING_DEGREE_INNER];
            int bodyBase = 226;

            // The chain has to be in this order, or the bands below describe nothing.
            ok("the ring chain runs outward", degreeInner < termInner
                && termInner <= signInner && signInner < signOuter && signOuter < decanOuter);
            ok("the open middle starts inside the degree scale", bodyBase < degreeInner);

            eq("the decan ring reads a decan", SkymapPanel.BAND_DECAN,
                SkymapPanel.bandAt((signOuter + decanOuter) / 2.0, rings, bodyBase));
            eq("the sign ring reads a sign", SkymapPanel.BAND_SIGN,
                SkymapPanel.bandAt((signInner + signOuter) / 2.0, rings, bodyBase));
            eq("the inner degree scale reads a degree", SkymapPanel.BAND_DEGREE,
                SkymapPanel.bandAt((degreeInner + termInner) / 2.0, rings, bodyBase));
            eq("the bounds ring names its sign", SkymapPanel.BAND_OPEN,
                SkymapPanel.bandAt((termInner + signInner) / 2.0, rings, bodyBase));
            eq("the open middle names its sign", SkymapPanel.BAND_OPEN,
                SkymapPanel.bandAt((bodyBase + degreeInner) / 2.0, rings, bodyBase));
            eq("outside the decan ring is nothing", SkymapPanel.BAND_NONE,
                SkymapPanel.bandAt(decanOuter + 2.0, rings, bodyBase));
            eq("inside the natal band is nothing", SkymapPanel.BAND_NONE,
                SkymapPanel.bandAt(bodyBase - 2.0, rings, bodyBase));

            // <b>No band may swallow another.</b> This is the defect itself: one test wide
            // enough to answer for four rings. Walking every pixel is what catches it, where
            // five sample points in the middle of each band would not.
            java.util.Map<Integer, Integer> width = new java.util.TreeMap<>();
            for (int r = 0; r <= decanOuter + 20; r++) {
                width.merge(SkymapPanel.bandAt(r, rings, bodyBase), 1, Integer::sum);
            }
            ok("the decan ring is as deep as it is drawn",
                width.getOrDefault(SkymapPanel.BAND_DECAN, 0) == decanOuter - signOuter);
            ok("the sign ring is as deep as it is drawn",
                width.getOrDefault(SkymapPanel.BAND_SIGN, 0) == signOuter - signInner);
            ok("the degree scale is as deep as it is drawn",
                width.getOrDefault(SkymapPanel.BAND_DEGREE, 0) == termInner - degreeInner);
            ok("the open middle is what is left",
                width.getOrDefault(SkymapPanel.BAND_OPEN, 0)
                    == (signInner - bodyBase) - (termInner - degreeInner));

            // ---- half two: the chords, on a real tri-wheel ----
            boolean[] every = new boolean[com.zodiacomputing.ourania.astro.Bodies.ALL.length];
            java.util.Arrays.fill(every, true);
            set(sky, "shown", every);
            buildChart(sky, ChartMode.SYNASTRY, true);
            java.awt.Component wheel = sky.chartComponent();
            SwingUtilities.invokeAndWait(() -> {
                wheel.setSize(1100, 1100);
                wheel.doLayout();
            });
            java.awt.image.BufferedImage frame = new java.awt.image.BufferedImage(
                1100, 1100, java.awt.image.BufferedImage.TYPE_INT_RGB);
            SwingUtilities.invokeAndWait(() -> {
                java.awt.Graphics2D g2 = frame.createGraphics();
                wheel.paint(g2);
                g2.dispose();
            });

            ok("the chart under test draws all three rings",
                sky.outerRingDrawn() && sky.triRingDrawn()
                    && sky.drawsNatalAspects() && sky.drawsCrossAspects());

            java.lang.reflect.Method geom = SkymapPanel.class.getDeclaredMethod("geometry");
            geom.setAccessible(true);
            Object g = geom.invoke(sky);
            int cx = (Integer) fieldOf(g, "cx");
            int cy = (Integer) fieldOf(g, "cy");
            double pin = (Double) fieldOf(g, "pin");
            java.lang.reflect.Method disc =
                g.getClass().getDeclaredMethod("aspectDisc", int.class);
            disc.setAccessible(true);
            java.lang.reflect.Method chordAt = SkymapPanel.class.getDeclaredMethod(
                "chordAt", int.class, int.class);
            chordAt.setAccessible(true);
            java.lang.reflect.Method inkFor = SkymapPanel.class.getDeclaredMethod(
                "aspectInkFor", double.class, double.class, int.class, int.class,
                boolean.class);
            inkFor.setAccessible(true);

            double[][] lons = {sky.bLon, sky.tLon, sky.cLon};
            boolean[][] valids = {sky.bValid, sky.tValid, sky.cValid};
            String[] named = {"natal", "partner", "sky"};
            int[] reached = new int[3];
            int consistent = 0;
            int found = 0;

            for (int ring = 0; ring < 3; ring++) {
                int d = (Integer) disc.invoke(g, ring);
                for (int a = 0; a < SkymapPanel.BODY_COUNT; a++) {
                    if (!SkymapPanel.aspecting(a, valids[ring])) {
                        continue;
                    }
                    for (int b = ring == 0 ? a + 1 : 0; b < SkymapPanel.BODY_COUNT; b++) {
                        if (!SkymapPanel.aspecting(b, sky.bValid)
                            || (ring == 0 && com.zodiacomputing.ourania.astro.Bodies.isOppositePair(a, b))
                            || inkFor.invoke(sky, lons[ring][a], sky.bLon[b], a, b,
                                ring == 1) == null) {
                            continue;
                        }
                        double ra = Math.toRadians(180.0 + pin - lons[ring][a]);
                        double rb = Math.toRadians(180.0 + pin - sky.bLon[b]);
                        double ax = cx + d * Math.cos(ra);
                        double ay = cy + d * Math.sin(ra);
                        double bx = cx + d * Math.cos(rb);
                        double by = cy + d * Math.sin(rb);
                        // A quarter along, which keeps the sample off the middle where every
                        // near-opposition crosses every other.
                        int px = (int) Math.round(ax + 0.25 * (bx - ax));
                        int py = (int) Math.round(ay + 0.25 * (by - ay));
                        int[] hit = (int[]) chordAt.invoke(sky, px, py);
                        if (hit == null) {
                            continue;
                        }
                        found++;
                        if (hit[0] == ring) {
                            reached[ring]++;
                        }
                        // Whatever it names has to be a line that is really drawn there.
                        consistent++;
                        ok("what the cursor found on the " + named[ring]
                            + " field is a line that is drawn",
                            inkFor.invoke(sky, lons[hit[0]][hit[1]], sky.bLon[hit[2]],
                                hit[1], hit[2], hit[0] == 1) != null);
                    }
                }
            }
            System.out.println("  chords under the cursor: " + found + " found, "
                + consistent + " drawn where they were named; reached natal "
                + reached[0] + ", partner " + reached[1] + ", sky " + reached[2]);
            for (int ring = 0; ring < 3; ring++) {
                ok("the " + named[ring] + " ring's chords can be hovered", reached[ring] > 0);
            }
            ok("nothing is hovered well outside the fields",
                chordAt.invoke(sky, cx, cy - 540) == null);
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static Object fieldOf(Object o, String name) throws Exception {
        java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void hoveredLineEnds() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel sky = (SkymapPanel) fs.get(w[0]);
            java.lang.reflect.Method lit = SkymapPanel.class.getDeclaredMethod(
                "onHighlightedLine", int.class, int.class);
            lit.setAccessible(true);
            final int natal = SkymapPanel.WHEEL_NATAL;
            final int outer = SkymapPanel.WHEEL_OUTER;
            final int sky3 = SkymapPanel.WHEEL_SKY;

            // Nothing hovered: nothing lights, on any ring.
            set(sky, "highlightA", -1);
            set(sky, "highlightB", -1);
            set(sky, "highlightWheel", natal);
            for (int i = 0; i < 6; i++) {
                ok("with no line hovered, body " + i + " stays dark",
                    !(Boolean) lit.invoke(sky, i, natal)
                        && !(Boolean) lit.invoke(sky, i, outer)
                        && !(Boolean) lit.invoke(sky, i, sky3));
            }

            // A natal-to-natal line: both ends are on the natal wheel, neither elsewhere.
            set(sky, "highlightA", 2);
            set(sky, "highlightB", 5);
            set(sky, "highlightWheel", natal);
            ok("a natal line lights its first end", (Boolean) lit.invoke(sky, 2, natal));
            ok("a natal line lights its second end", (Boolean) lit.invoke(sky, 5, natal));
            ok("and lights nothing on the outer ring",
                !(Boolean) lit.invoke(sky, 2, outer) && !(Boolean) lit.invoke(sky, 5, outer));
            ok("and nothing on the sky ring",
                !(Boolean) lit.invoke(sky, 2, sky3) && !(Boolean) lit.invoke(sky, 5, sky3));
            ok("and nothing that is not an end", !(Boolean) lit.invoke(sky, 3, natal));

            // <b>A cross-chart line names its ring, not merely "not natal".</b> Both cross
            // rings shared one boolean until 2026-09-07, so a partner line lit the sky glyph
            // of the same body and a sky line lit the partner one - two haloes on two rings
            // for a line that touches one. Each ring is asked separately below.
            for (int hovered : new int[] {outer, sky3}) {
                int otherRing = hovered == outer ? sky3 : outer;
                String named = hovered == outer ? "partner" : "sky";
                set(sky, "highlightA", 2);
                set(sky, "highlightB", 5);
                set(sky, "highlightWheel", hovered);
                ok("a " + named + " line lights its outer end on its own ring",
                    (Boolean) lit.invoke(sky, 2, hovered));
                ok("a " + named + " line lights its natal end on the natal wheel",
                    (Boolean) lit.invoke(sky, 5, natal));
                ok("a " + named + " line's outer end does not light on the natal wheel",
                    !(Boolean) lit.invoke(sky, 2, natal));
                ok("a " + named + " line's natal end does not light on its own ring",
                    !(Boolean) lit.invoke(sky, 5, hovered));
                ok("a " + named + " line lights nothing on the other cross ring",
                    !(Boolean) lit.invoke(sky, 2, otherRing)
                        && !(Boolean) lit.invoke(sky, 5, otherRing));
                ok("a " + named + " line lights nothing that is not an end",
                    !(Boolean) lit.invoke(sky, 3, hovered));
            }
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

    /**
     * Nothing is open when the window appears except the chart.
     *
     * <b>Both rails were revealed at startup.</b> The first thing a reader saw was a chart
     * with nearly six hundred pixels of panel taken off it - the app opening onto its own
     * furniture. The rails still know which page they hold, so a first press of a tab opens
     * onto the right one; they simply start closed.
     *
     * Asserted because it is the kind of thing a later convenience quietly undoes: one call
     * to reveal a panel "so the reader can find it" and the chart is back behind the
     * furniture, with nothing to say so.
     */
    private static void opensOntoTheChart() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            Thread.sleep(2500);
            final java.util.List<DrawerRail> rails = new java.util.ArrayList<>();
            final java.util.List<Drawer> drawers = new java.util.ArrayList<>();
            SwingUtilities.invokeAndWait(() -> collectPanels(w[0].getContentPane(),
                rails, drawers));

            ok("the window has rails to be closed", !rails.isEmpty());
            for (DrawerRail rail : rails) {
                ok("a rail starts closed", !rail.isOpen());
                // Closed but not empty: it knows the page it will open onto.
                ok("and still knows which page it holds", rail.selected() != null);
            }
            for (Drawer d : drawers) {
                ok("the " + d.side() + " drawer starts closed", !d.isOpen());
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static void collectPanels(Container c, java.util.List<DrawerRail> rails,
                                      java.util.List<Drawer> drawers) {
        for (Component child : c.getComponents()) {
            if (child instanceof DrawerRail) {
                rails.add((DrawerRail) child);
            }
            if (child instanceof Drawer) {
                drawers.add((Drawer) child);
            }
            if (child instanceof Container) {
                collectPanels((Container) child, rails, drawers);
            }
        }
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
