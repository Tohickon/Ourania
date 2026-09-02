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

        // Everything is in one drawer now, so every section must be in this one.
        for (String s : new String[] {SidePanel.MENU, SidePanel.PROFILES, SidePanel.NATAL,
                                      SidePanel.TRANSITS, SidePanel.GRIDS,
                                      SidePanel.READINGS}) {
            ok("the drawer holds " + s, sectionTitles(side).contains(s));
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

    /** The Menu section's rows - the ten screens, without the directory's Chart A/B buttons. */
    private static List<JButton> menuRows() {
        List<JButton> out = new ArrayList<>();
        Accordion acc = side.accordion();
        Accordion.Section menu = acc == null ? null : acc.section(SidePanel.MENU);
        if (menu != null) {
            collectButtons(menu, out);
        }
        return out;
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
     * DecanSystem is exempt by name, because that one is a judgement: four labels and a getter
     * with no logic behind them and no reference anywhere in the tree. It wants implementing
     * or deleting, not a button, and a door onto it would open onto nothing.
     *
     * The first version of this part named Calibration and DecanSystem and immediately found
     * FittingHarness and CorpusBuilder, which the audit that prompted it had missed. Adding a
     * name here is allowed. Adding one silently, to make this part go green, is the thing it
     * exists to prevent.
     */
    private static void everyEngineHasADoor() {
        java.util.Set<String> exempt = new java.util.HashSet<>(
                java.util.Collections.singletonList("DecanSystem"));

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
