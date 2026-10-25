package com.zodiacomputing.ourania.gui;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

/**
 * The Astrologer view switch in Settings, held against the page switches.
 *
 * <p>Two things must stay true. Settings has one entry for the view, and the search finds it.
 * And that entry, the box on every page and the saved setting never disagree, whichever one
 * the reader uses. The window is real, because the boxes live in it.
 */
public final class AstrologerSettingCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        Settings.setAstrologerView(false);

        final OuraniaWindow[] win = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> win[0] = new OuraniaWindow());
        try {
            SwingUtilities.invokeAndWait(() -> run(win[0]));
        } finally {
            SwingUtilities.invokeAndWait(() -> win[0].dispose());
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

    private static void run(OuraniaWindow w) {
        List<Component> all = new ArrayList<>();
        walk(w.getContentPane(), all);

        SettingsPanel panel = null;
        List<JCheckBox> pageBoxes = new ArrayList<>();
        List<JCheckBox> entries = new ArrayList<>();
        for (Component c : all) {
            if (c instanceof SettingsPanel) {
                panel = (SettingsPanel) c;
            }
            if (c instanceof JCheckBox) {
                String t = ((JCheckBox) c).getText();
                if ("Show the astrology".equals(t)) {
                    pageBoxes.add((JCheckBox) c);
                } else if (t != null && t.startsWith("Astrologer view (")) {
                    entries.add((JCheckBox) c);
                }
            }
        }

        check(panel != null, "the window holds a Settings panel");
        check(entries.size() == 1, "Settings has exactly one Astrologer view entry (found "
            + entries.size() + ")");
        check(!pageBoxes.isEmpty(), "pages have their own boxes (found " + pageBoxes.size() + ")");
        if (panel == null || entries.size() != 1 || pageBoxes.isEmpty()) {
            return;
        }
        JCheckBox entry = entries.get(0);

        // The search finds it by what a reader would type.
        for (String q : new String[] {"astrologer", "astrologer view", "astrology drawer"}) {
            boolean found = false;
            for (SettingsSearch.Hit h : SettingsSearch.find(panel.searchHits(), q)) {
                if (h.control == entry) {
                    found = true;
                }
            }
            check(found, "searching '" + q + "' finds the Astrologer view entry");
        }

        // Settings entry -> setting and every page box.
        entry.setSelected(true);
        check(Settings.astrologerView(), "ticking the entry saves the setting");
        check(allMatch(pageBoxes, true), "ticking the entry ticks every page box");
        entry.setSelected(false);
        check(!Settings.astrologerView(), "clearing the entry clears the setting");
        check(allMatch(pageBoxes, false), "clearing the entry clears every page box");

        // Page box -> setting and the Settings entry.
        pageBoxes.get(0).setSelected(true);
        check(Settings.astrologerView(), "ticking a page box saves the setting");
        check(entry.isSelected(), "ticking a page box ticks the Settings entry");
        check(allMatch(pageBoxes, true), "ticking a page box ticks the other page boxes");
        pageBoxes.get(pageBoxes.size() - 1).setSelected(false);
        check(!entry.isSelected(), "clearing a page box clears the Settings entry");
        check(!Settings.astrologerView(), "clearing a page box clears the setting");
    }

    private static boolean allMatch(List<JCheckBox> boxes, boolean on) {
        for (JCheckBox b : boxes) {
            if (b.isSelected() != on) {
                return false;
            }
        }
        return true;
    }

    private static void walk(Container c, List<Component> out) {
        for (Component k : c.getComponents()) {
            out.add(k);
            if (k instanceof Container) {
                walk((Container) k, out);
            }
        }
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures.add(what);
        }
    }
}
