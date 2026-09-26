package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;

import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Presets stage 2b: the bar, and the four things it could quietly get wrong.
 *
 * <p><b>This suite exists because the rule and its only consumer would otherwise be checked
 * apart.</b> {@code OrbCheck} proves the engine keeps four independent tables and that synastry
 * derives from the natal width in force. None of that says the panel reads or writes the right
 * one, and every defect below is invisible from the engine's side:
 *
 * <ul>
 * <li><b>A</b> - the switch shows what is stored, and going back shows what was there before.</li>
 * <li><b>B</b> - <b>switching leaves every stored table exactly as it was.</b> Setting a spinner
 *     fires its change listener, which writes under {@code shownProfile}. This is asserted by
 *     reading the stored keys rather than the spinners, because the spinners would show the right
 *     numbers either way - the defect is what gets written behind them. <b>What it catches is the
 *     ordering:</b> assigning {@code shownProfile} after the reseed loop instead of before sends
 *     every write to the profile being switched away from, and the reader's natal table is
 *     overwritten with synastry widths. That mutation takes 14 assertions here red. Removing the
 *     {@code seeding} guard takes none - it saves 51 chart rebuilds per switch, not the data.</li>
 * <li><b>C</b> - bold means "you changed it" measured against the shown profile's default. On
 *     synastry that default is half the natal width, so comparing with the natal figure would
 *     bold every untouched point the moment the reader switched.</li>
 * <li><b>D</b> - a ceiling's spinner bound moves with the profile, or a reader can type a number
 *     {@code Settings.setAspectCap} then silently clamps: the screen and the file disagreeing.</li>
 * <li><b>E</b> - Reset touches only what is on screen. The no-argument {@code resetBodyOrbs}
 *     clears all four profiles, so the button must not call it.</li>
 * <li><b>G</b> - <b>geometry, which is the half of this screen no other suite looks at.</b>
 *     Every assertion above would pass against a tab where the points had been pushed 300 pixels
 *     down inside their own box, because a spinner reads and writes the same value wherever it
 *     is drawn. This part measures the boxes after a real layout pass.</li>
 * </ul>
 */
public final class PresetBarCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** A point nobody is likely to have opinions about, and one with a wide natal width. */
    private static final String NARROW = "Moon";
    private static final String WIDE = "Sun";

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        Settings.resetBodyOrbs();
        Settings.resetAspectCaps();

        part("A: the switch shows what is stored", PresetBarCheck::shows);
        part("B: switching writes nothing", PresetBarCheck::writesNothing);
        part("C: bold is measured against the shown profile", PresetBarCheck::bold);
        part("D: a ceiling's bound moves with the profile", PresetBarCheck::bound);
        part("E: Reset touches only what is on screen", PresetBarCheck::reset);
        part("F: the four tabs, and what is on each", PresetBarCheck::tabs);
        part("G: no group box is bigger than what is in it", PresetBarCheck::noVoid);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + "):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    // ------------------------------------------------------------------ A

    private static void shows() throws Exception {
        Settings.resetBodyOrbs();
        Settings.setBodyOrb(NARROW, 4.5);
        Settings.setBodyOrb(NARROW, 2.0, Aspects.Profile.SYNASTRY);

        SettingsPanel p = panel();
        JSpinner s = orbSpinner(p, NARROW);
        near("it opens on natal", 4.5, value(s));

        click(p, Aspects.Profile.SYNASTRY);
        near("Synastry shows the synastry width", 2.0, value(s));

        click(p, Aspects.Profile.COMPOSITE);
        near("Composite follows natal, which is 4.5", 4.5, value(s));

        click(p, Aspects.Profile.TRANSIT);
        near("Transits is flat, whatever natal does",
            com.zodiacomputing.ourania.astro.Transits.DEFAULT_ORB, value(s));

        click(p, Aspects.Profile.NATAL);
        near("and going back shows what was there before", 4.5, value(s));

        // A point nobody has set in synastry follows the natal width IN FORCE, which is the
        // derivation OrbCheck's Part G asserts - here it is asserted through the screen.
        Settings.setBodyOrb(WIDE, 12.0);
        SettingsPanel q = panel();
        click(q, Aspects.Profile.SYNASTRY);
        near("an unset synastry point shows half the natal width in force, not half the table",
            6.0, value(orbSpinner(q, WIDE)));
    }

    // ------------------------------------------------------------------ B

    private static void writesNothing() throws Exception {
        Settings.resetBodyOrbs();
        Settings.resetAspectCaps();
        Settings.setBodyOrb(NARROW, 4.5);

        SettingsPanel p = panel();
        // Walk every profile, twice round, the way a reader comparing them would.
        for (int lap = 0; lap < 2; lap++) {
            for (Aspects.Profile profile : Aspects.Profile.values()) {
                click(p, profile);
            }
        }

        // <b>Read the stored keys, not the spinners.</b> The spinners would show the right
        // numbers either way; the defect is what got written behind them. Shown able to fail:
        // moving shownProfile's assignment below the reseed loop takes fourteen of these red.
        ok("a point nobody set in synastry is still unset after visiting it",
            stored(Aspects.Profile.SYNASTRY, WIDE).isEmpty());
        ok("and in composite", stored(Aspects.Profile.COMPOSITE, WIDE).isEmpty());
        ok("and in transits", stored(Aspects.Profile.TRANSIT, WIDE).isEmpty());
        ok("the point that WAS set in natal is still set, and only in natal",
            !stored(Aspects.Profile.NATAL, NARROW).isEmpty()
                && stored(Aspects.Profile.SYNASTRY, NARROW).isEmpty());
        near("and it still reads what the reader chose", 4.5, Settings.bodyOrb(NARROW));

        for (Aspects.Type t : Aspects.Type.values()) {
            ok("no ceiling was written into synastry by looking at it: " + t.name(),
                Settings.get("orb.synastry.aspect." + t.name(), "").trim().isEmpty());
        }
    }

    // ------------------------------------------------------------------ C

    private static void bold() throws Exception {
        Settings.resetBodyOrbs();
        Settings.setBodyOrb(WIDE, 12.0);

        SettingsPanel p = panel();
        JSpinner wide = orbSpinner(p, WIDE);
        JSpinner narrow = orbSpinner(p, NARROW);
        ok("a natal point the reader widened is bold", isBold(wide));
        ok("and one they left alone is not", !isBold(narrow));

        click(p, Aspects.Profile.SYNASTRY);
        // Both points are untouched in synastry. The wide one is only "changed" if the marker
        // measures it against the NATAL default, which is the bug.
        ok("on Synastry, a point nobody has set there is not bold", !isBold(wide));
        ok("nor is the other", !isBold(narrow));

        Settings.setBodyOrb(WIDE, 3.0, Aspects.Profile.SYNASTRY);
        click(p, Aspects.Profile.NATAL);
        click(p, Aspects.Profile.SYNASTRY);
        ok("but a point set IN synastry is bold there", isBold(wide));
        click(p, Aspects.Profile.NATAL);
        ok("and that does not make it bold in natal - it was already", isBold(wide));
        Settings.resetBodyOrbs();
    }

    // ------------------------------------------------------------------ D

    private static void bound() throws Exception {
        Settings.resetAspectCaps();
        SettingsPanel p = panel();
        Aspects.Type t = Aspects.Type.values()[0];
        JSpinner s = capSpinner(p, t);

        near("the natal bound is the declared ceiling", Aspects.defaultCapOf(t), maximum(s));
        click(p, Aspects.Profile.SYNASTRY);
        near("the synastry bound is half of it", Aspects.defaultCapOf(t) / 2.0, maximum(s));
        near("and it is what the engine would give",
            Aspects.defaultCapOf(t, Aspects.Profile.SYNASTRY), maximum(s));
        click(p, Aspects.Profile.NATAL);
        near("and it goes back", Aspects.defaultCapOf(t), maximum(s));
    }

    // ------------------------------------------------------------------ E

    private static void reset() throws Exception {
        Settings.resetBodyOrbs();
        Settings.setBodyOrb(NARROW, 4.5);
        Settings.setBodyOrb(NARROW, 2.0, Aspects.Profile.SYNASTRY);

        SettingsPanel p = panel();
        click(p, Aspects.Profile.SYNASTRY);
        press(p);

        near("the shown profile is back to its default", 2.25,
            Settings.bodyOrb(NARROW, Aspects.Profile.SYNASTRY));
        // 2.25 is half of 4.5 - half of the natal width IN FORCE, not half the built-in table.
        near("and natal is untouched", 4.5, Settings.bodyOrb(NARROW));
        ok("which is the whole point: the button said Synastry",
            !stored(Aspects.Profile.NATAL, NARROW).isEmpty());
        Settings.resetBodyOrbs();
    }

    // ------------------------------------------------------------------ F

    /** The four tabs David named, in his order. Spelled here so a rename has to be deliberate. */
    private static final String[] TABS = {
        "Globe & Display", "Aspects & Orbs", "Bodies & Points", "Engine Rules",
    };

    /**
     * Where each control actually landed.
     *
     * <b>Existing is not the same as being in the right place.</b> Every other suite here builds
     * the panel and reaches its controls by field, so all of them would go on passing if a section
     * boundary were off by one and a whole block of the screen moved to the wrong tab. This asks
     * which tab a control is under.
     */
    private static void tabs() throws Exception {
        SettingsPanel p = panel();
        javax.swing.JTabbedPane pane =
            (javax.swing.JTabbedPane) CheckReflect.get(p, "tabs");
        ok("the screen has tabs", pane != null);
        if (pane == null) {
            return;
        }
        ok("four of them", pane.getTabCount() == TABS.length);
        for (int i = 0; i < Math.min(TABS.length, pane.getTabCount()); i++) {
            ok("tab " + i + " is " + TABS[i], TABS[i].equals(pane.getTitleAt(i)));
            ok("and says what it holds", pane.getToolTipTextAt(i) != null
                && pane.getToolTipTextAt(i).length() > 30);
        }

        // A point's orb is with the point, and an aspect's ceiling with the aspect - which is
        // what David asked for on 24 Sep and what the tabs must not undo.
        ok("a point's orb spinner is on Bodies & Points",
            tabOf(pane, orbSpinner(p, NARROW)) == 2);
        ok("an aspect's ceiling is on Aspects & Orbs",
            tabOf(pane, capSpinner(p, Aspects.Type.values()[0])) == 1);
        ok("the flat transit orb is on Engine Rules", tabOf(pane, p.transitOrb) == 3);

        // <b>The presets sit on the tabs they govern.</b> They were above the strip, hidden on
        // the two tabs they govern nothing on - which made the strip move as you changed tab.
        java.util.List<javax.swing.JToggleButton> bars = toggles(p);
        ok("there are preset toggles", !bars.isEmpty());
        java.util.Set<Integer> onTabs = new java.util.TreeSet<>();
        for (javax.swing.JToggleButton b : bars) {
            onTabs.add(Integer.valueOf(tabOf(pane, b)));
        }
        ok("every preset toggle is on Aspects & Orbs or Bodies & Points",
            onTabs.equals(new java.util.TreeSet<>(
                java.util.Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)))));

        java.util.List<javax.swing.JButton> resets = resetButtons(p);
        ok("there is a Reset beside each bar", resets.size() == 2);
        for (javax.swing.JButton b : resets) {
            int where = tabOf(pane, b);
            ok("Reset is on a tab with orb controls", where == 1 || where == 2);
        }

        // <b>Two bars, one answer.</b> A component cannot be in two containers, so there are two
        // of these; two controls over one setting is the defect this project logs most often.
        click(p, Aspects.Profile.SYNASTRY);
        int lit = 0;
        for (javax.swing.JToggleButton b : bars) {
            if (b.isSelected()) {
                lit++;
                ok("every lit toggle says Synastry",
                    Aspects.Profile.SYNASTRY.name().equals(b.getActionCommand()));
            }
        }
        ok("one toggle is lit on each bar", lit == 2);
        for (javax.swing.JButton b : resets) {
            ok("and every Reset names Synastry", b.getText().contains("Synastry"));
        }
        click(p, Aspects.Profile.NATAL);
        ok("and back again", countLit(bars, Aspects.Profile.NATAL) == 2);
    }

    private static int countLit(java.util.List<javax.swing.JToggleButton> bars,
                                Aspects.Profile want) {
        int n = 0;
        for (javax.swing.JToggleButton b : bars) {
            if (b.isSelected() && want.name().equals(b.getActionCommand())) {
                n++;
            }
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<javax.swing.JButton> resetButtons(SettingsPanel p)
            throws Exception {
        return (java.util.List<javax.swing.JButton>) CheckReflect.get(p, "orbResetButtons");
    }

    /** Which tab a component sits under, or -1 when it is not under any of them. */
    private static int tabOf(javax.swing.JTabbedPane pane, Component c) {
        if (c == null) {
            return -1;
        }
        for (int i = 0; i < pane.getTabCount(); i++) {
            if (javax.swing.SwingUtilities.isDescendingFrom(c, pane.getComponentAt(i))) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ G

    /**
     * The five body group boxes, each the height of its own contents.
     *
     * <b>Measured after a layout pass, because a preferred size is not what a reader sees.</b>
     * Until 26 Sep the boxes sat in a {@code GridLayout}, which gives every cell the height of
     * the tallest: Calculated Points has fourteen rows and The Lunar Nodes has two, so all five
     * were 530 pixels tall against preferred heights of 247, 260, 157, 397 and 530 - and
     * BoxLayout handed the 283 spare pixels in the first box to the one child that would take
     * them, the All/None row, which then floated in the middle of the hole with the points
     * beneath it. David, 26 Sep: "a huge upper margin gap that should be closed."
     *
     * <b>Five claims, because each catches a different way back.</b> A box no taller than its
     * contents catches the grid; a box that will not accept more height than it needs catches
     * the cap on the All/None row coming off; nothing inside a box stretched past its own
     * preferred height catches the hole moving to some other child; a column starting at the top
     * of its half catches the hole reopening one level up, above the first box instead of inside
     * it; and the whole block being shorter than the boxes laid end to end catches a single
     * column, which would have no void in it and still be twice as long as the screen.
     */
    private static void noVoid() throws Exception {
        SettingsPanel p = laidOut();
        javax.swing.JCheckBox[] all = (javax.swing.JCheckBox[]) CheckReflect.get(p, "boxes");
        int tallest = 0;
        int wanted = 0;
        int width = -1;
        Container block = null;
        java.util.List<Container> columns = new ArrayList<>();
        for (Bodies.Group group : Bodies.Group.values()) {
            Container box = groupBox(all, group);
            if (box == null) {
                fail("no box on the screen is headed " + group.title);
                continue;
            }
            int want = box.getPreferredSize().height;
            wanted += want;
            tallest = Math.max(tallest, box.getHeight());
            ok(group.title + " is the height of what is in it (" + box.getHeight()
                + " drawn, " + want + " wanted)", box.getHeight() <= want + 1);

            for (Component kid : box.getComponents()) {
                ok(group.title + ": " + describe(kid) + " is not stretched to fill a hole ("
                    + kid.getHeight() + " drawn, " + kid.getPreferredSize().height + " wanted)",
                    kid.getHeight() <= kid.getPreferredSize().height + 1);
            }

            // <b>And it will not accept more, wherever it is put next.</b> The box's own
            // maximum was unbounded because the All/None row's was, which is what let BoxLayout
            // put 283 pixels through the middle of it. Asserted rather than left to the parent,
            // because the parent is what changed last time.
            ok(group.title + " will not take more height than it needs ("
                + box.getMaximumSize().height + " allowed, " + want + " wanted)",
                box.getMaximumSize().height <= want + 1);

            if (width < 0) {
                width = box.getWidth();
            }
            ok(group.title + " is the same width as the others (" + box.getWidth()
                + " against " + width + ")", Math.abs(box.getWidth() - width) <= 1);
            block = box.getParent() == null ? null : box.getParent().getParent();
            if (box.getParent() != null && !columns.contains(box.getParent())) {
                columns.add(box.getParent());
            }
        }

        // <b>And the boxes start at the top of the column.</b> Without this the hole comes
        // back one level up and every assertion above still passes: a stack of correctly sized
        // boxes handed a column taller than itself is centred in it by BoxLayout, which puts
        // half the slack above the first box - the reported defect exactly, with the boxes now
        // innocent. It is the NORTH in bodyGroupColumns that stops it.
        ok("the boxes sit in " + columns.size() + " columns", columns.size() == 2);
        for (int i = 0; i < columns.size(); i++) {
            Container column = columns.get(i);
            ok("column " + (i + 1) + " starts at the top of its half (" + column.getY() + ")",
                column.getY() == 0);
            ok("column " + (i + 1) + "'s first box starts at the top of it ("
                + (column.getComponentCount() == 0 ? -1 : column.getComponent(0).getY()) + ")",
                column.getComponentCount() > 0 && column.getComponent(0).getY() == 0);
        }

        // Two columns, not one: the block has to be shorter than the boxes end to end, or the
        // whole tab is a single strip twice the height of the screen.
        ok("the five boxes share two columns (" + (block == null ? -1 : block.getHeight())
            + " tall, " + wanted + " laid end to end)",
            block != null && block.getHeight() < wanted && block.getHeight() >= tallest);
    }

    /** What a child of a group box is, for the failure line. */
    private static String describe(Component c) {
        if (c instanceof javax.swing.JLabel) {
            String t = ((javax.swing.JLabel) c).getText();
            return "the label " + (t.length() > 30 ? t.substring(0, 30) : t);
        }
        return c.getClass().getSimpleName();
    }

    /**
     * The box headed with this group's title.
     *
     * <b>Found by walking up from one of the group's own checkboxes</b> until an ancestor is
     * holding a heading that reads the group's name, rather than by counting containers down
     * from the tab. The tab's nesting is exactly what changed to close the gap, so a suite that
     * knew the nesting would have needed editing to keep passing - and an assertion you have to
     * edit to keep green is not one.
     */
    private static Container groupBox(javax.swing.JCheckBox[] all, Bodies.Group group) {
        javax.swing.JCheckBox mine = null;
        for (int i = 0; i < Bodies.count() && mine == null; i++) {
            if (Bodies.at(i).group == group) {
                mine = all[i];
            }
        }
        for (Container c = mine; c != null; c = c.getParent()) {
            for (Component kid : c.getComponents()) {
                if (kid instanceof javax.swing.JLabel
                    && group.title.equals(((javax.swing.JLabel) kid).getText())) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * A settings panel that has actually been laid out, on the Bodies and Points tab.
     *
     * <b>A panel nobody has sized has every component at 0x0</b>, so the measurements in Part G
     * would all pass against anything. The window size is a wide one on purpose: a hole that only
     * appears when there is spare height is not going to appear on a cramped screen.
     */
    private static SettingsPanel laidOut() throws Exception {
        final SettingsPanel p = panel();
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame();
            frame.setContentPane(p);
            frame.setSize(1500, 950);
            javax.swing.JTabbedPane pane = null;
            try {
                pane = (javax.swing.JTabbedPane) CheckReflect.get(p, "tabs");
            } catch (Exception noTabs) {
                fail("the settings panel has no tabs: " + noTabs);
            }
            if (pane != null) {
                for (int i = 0; i < pane.getTabCount(); i++) {
                    if (pane.getTitleAt(i).startsWith("Bodies")) {
                        pane.setSelectedIndex(i);
                    }
                }
            }
            frame.validate();
            p.setSize(1460, 900);
            relayout(p);
            frame.dispose();
        });
        return p;
    }

    /** Swing lays out lazily; this forces the whole tree so the bounds are the real ones. */
    private static void relayout(Container c) {
        c.doLayout();
        for (Component kid : c.getComponents()) {
            if (kid instanceof Container) {
                relayout((Container) kid);
            }
        }
    }

    // ------------------------------------------------------------------ driving the panel

    private static SettingsPanel panel() throws Exception {
        final SettingsPanel[] out = new SettingsPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> out[0] = new SettingsPanel(null));
        return out[0];
    }

    /**
     * Press the bar's own button, so the wiring is under test and not only the method.
     *
     * <b>The label is asked of the panel, never spelled here.</b> A copy of the four names in
     * this file would pass while the screen said something else - which is the defect this
     * project keeps producing, and a suite that carries its own copy of the rule cannot catch it.
     */
    private static void click(SettingsPanel p, Aspects.Profile profile) throws Exception {
        java.lang.reflect.Method m =
            SettingsPanel.class.getDeclaredMethod("profileLabel", Aspects.Profile.class);
        m.setAccessible(true);
        String label = (String) m.invoke(null, profile);
        for (final JToggleButton b : toggles(p)) {
            if (label.equals(b.getText())) {
                javax.swing.SwingUtilities.invokeAndWait(() -> b.doClick());
                return;
            }
        }
        throw new IllegalStateException("no button on the bar reads " + label);
    }

    private static JSpinner orbSpinner(SettingsPanel p, String name) throws Exception {
        JSpinner[] all = (JSpinner[]) CheckReflect.get(p, "orbSpinners");
        for (int i = 0; i < Bodies.count(); i++) {
            if (Bodies.at(i).name.equals(name)) {
                return all[i];
            }
        }
        throw new IllegalStateException("no spinner for " + name);
    }

    private static JSpinner capSpinner(SettingsPanel p, Aspects.Type t) throws Exception {
        JSpinner[] all = (JSpinner[]) CheckReflect.get(p, "aspectCapSpinners");
        return all[t.ordinal()];
    }

    private static void press(SettingsPanel p) throws Exception {
        // Either Reset acts on the shown preset; the first is as good as the second, and
        // Part F is what asserts the two agree.
        final javax.swing.JButton b = resetButtons(p).get(0);
        javax.swing.SwingUtilities.invokeAndWait(() -> b.doClick());
    }

    private static double value(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    private static double maximum(JSpinner s) {
        return ((Number) ((javax.swing.SpinnerNumberModel) s.getModel()).getMaximum())
            .doubleValue();
    }

    private static boolean isBold(JSpinner s) {
        Component editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            return ((JSpinner.DefaultEditor) editor).getTextField().getFont().isBold();
        }
        return false;
    }

    private static String stored(Aspects.Profile profile, String name) {
        String id = null;
        for (int i = 0; i < Bodies.count(); i++) {
            if (Bodies.at(i).name.equals(name)) {
                id = Bodies.at(i).id;
            }
        }
        String key = profile == Aspects.Profile.NATAL
            ? "orb.body." + id
            : "orb." + profile.key() + ".body." + id;
        return Settings.get(key, "").trim();
    }

    /**
     * The preset toggles, as the panel itself knows them.
     *
     * <b>Not a walk of the component tree.</b> {@code JCheckBox} extends {@code JToggleButton},
     * so collecting toggles from the tree collects every checkbox on the screen - every aspect,
     * every point, every globe option - and Part F then reported a "preset toggle" on the Globe &
     * Display tab and a lit one that did not say Synastry. Both were true of a checkbox and
     * neither was about a preset. Asking the panel for its own list means the suite and the screen
     * cannot disagree about what a preset toggle is.
     */
    @SuppressWarnings("unchecked")
    private static List<JToggleButton> toggles(SettingsPanel p) throws Exception {
        return (List<JToggleButton>) CheckReflect.get(p, "orbToggles");
    }

    // ------------------------------------------------------------------ harness

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        int before = failures.size();
        try {
            body.run();
        } catch (Exception e) {
            fail(title + " threw " + e);
        }
        System.out.println(title.substring(0, 1) + ": "
            + (failures.size() == before ? "clear" : "RED"));
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  ok   " + label);
        } else {
            fail(label);
        }
    }

    private static void near(String label, double expect, double got) {
        checks++;
        if (Math.abs(expect - got) < 1e-9) {
            System.out.println("  ok   " + label);
        } else {
            fail(label + " (expected " + expect + ", got " + got + ")");
        }
    }

    private static void fail(String label) {
        failures.add(label);
        System.out.println("  FAIL " + label);
    }

    private PresetBarCheck() {
    }
}
