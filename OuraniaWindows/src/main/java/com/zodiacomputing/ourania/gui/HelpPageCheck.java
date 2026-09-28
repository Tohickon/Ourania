package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * The help page describes the app this build actually is (G11).
 *
 * <p><b>Nothing fails when a help page goes stale</b>, which is why it is the copy least likely to
 * be corrected and why this project's rule about one description in one place applies to it more
 * than to code. So the page is generated from {@link SidePanel}'s arrays and {@link Shortcuts.Key},
 * and Part A is the assertion that keeps it that way: every row of every menu list, and every
 * keystroke, has to appear. A screen added to the menu without help fails here.
 *
 * <p><b>Part B asserts the absences.</b> A page that simply printed every string in the program
 * would pass Part A, so what must NOT be on it is checked too - no birth data, and nothing about
 * the three placeholder screens that were removed.
 *
 * <p><b>One mutation here cannot be caught, and that is the design working.</b> Adding a menu row
 * with no help written for it passes everything below, because the page is generated FROM the menu
 * - the row describes itself the moment it exists. What that mutation really probes is a menu row
 * with no screen behind it, which is what was removed on 2 September, and {@code NavigationCheck}
 * owns it: its own list of card names is asserted against {@code SidePanel.SCREENS}, so adding one
 * without the other takes it red on "SidePanel.SCREENS matches". Verified on 28 September by doing
 * exactly that - and the first probe reported the guard missing because it searched the output for
 * the screen's name, which is not what the failure is called.
 */
public final class HelpPageCheck {

    private HelpPageCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: every menu row and every key is on the page", HelpPageCheck::covers);
        part("B: and what is not on it", HelpPageCheck::absences);
        part("C: the glossary is a glossary", HelpPageCheck::glossary);
        part("D: Help has a door, and something behind it", HelpPageCheck::door);
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
        System.exit(0);
    }

    // ---------------------------------------------------------------- Part A

    private static void covers() {
        String html = Help.html();
        ok("the page is built (" + html.length() + " characters)", html.length() > 2000);

        List<String> missing = new ArrayList<>();
        for (String[][] table : new String[][][] {
                SidePanel.SCREENS, SidePanel.TABLE_ROWS, SidePanel.EXPORT_ROWS}) {
            for (String[] row : table) {
                if (!html.contains(row[0])) {
                    missing.add(row[0]);
                }
                // The description too: a page listing names without saying what they do is a
                // menu, and the reader already has one of those.
                if (!html.contains(escape(row[2]))) {
                    missing.add(row[0] + " (its description)");
                }
            }
        }
        int rows = SidePanel.SCREENS.length + SidePanel.TABLE_ROWS.length
            + SidePanel.EXPORT_ROWS.length;
        ok("all " + rows + " menu rows are described: " + head(missing), missing.isEmpty());

        List<String> noKey = new ArrayList<>();
        for (Shortcuts.Key k : Shortcuts.Key.values()) {
            if (!html.contains(k.written()) || !html.contains(escape(k.about))) {
                noKey.add(k.name());
            }
        }
        ok("every keystroke is listed with what it does: " + head(noKey), noKey.isEmpty());

        ok("it says which build it is", html.contains(Version.display()));
        ok("and points at Diagnostics for where the files are", html.contains("Diagnostics"));
    }

    // ---------------------------------------------------------------- Part B

    /**
     * What the page must not say.
     *
     * <b>Part A passes against a page that printed every string in the program.</b> These are the
     * assertions that make "generated from the menu" mean something narrower than "generated".
     */
    private static void absences() {
        Settings.set("natal.date", "1982-08-10");
        Settings.set("natal.location", "a place the reader typed");
        try {
            String html = Help.html();
            ok("no birth date", !html.contains("1982-08-10"));
            ok("no birth place", !html.contains("a place the reader typed"));
            for (String gone : new String[] {"Under Construction", "Share", "Sync", "Connect"}) {
                ok("nothing about " + gone + ", which is not a screen", !html.contains(gone));
            }
            ok("and it is closed HTML",
                html.startsWith("<html>") && html.endsWith("</html>"));
        } finally {
            Settings.useScratchFile();
        }
    }

    // ---------------------------------------------------------------- Part C

    private static void glossary() {
        java.util.Set<String> terms = new java.util.LinkedHashSet<>();
        List<String> thin = new ArrayList<>();
        for (String[] entry : Help.GLOSSARY) {
            ok("\"" + entry[0] + "\" is defined once", terms.add(entry[0]));
            if (entry[1] == null || entry[1].length() < 40) {
                thin.add(entry[0]);
            }
        }
        ok("every term has a definition worth reading: " + head(thin), thin.isEmpty());
        ok("there are enough of them to be a glossary (" + terms.size() + ")", terms.size() >= 8);

        String html = Help.html();
        List<String> absent = new ArrayList<>();
        for (String[] entry : Help.GLOSSARY) {
            if (!html.contains(entry[0]) || !html.contains(escape(entry[1]))) {
                absent.add(entry[0]);
            }
        }
        ok("and all of them reach the page: " + head(absent), absent.isEmpty());
    }

    // ---------------------------------------------------------------- Part D

    /**
     * Help is on the menu and has a card, which is the pair the old placeholder broke.
     *
     * <b>Asserted against the window, not against the list.</b> A row in SCREENS with no card
     * behind it is exactly what was removed on 2 September, and reading the list twice would not
     * notice it.
     */
    private static void door() throws Exception {
        boolean listed = false;
        for (String[] row : SidePanel.SCREENS) {
            if ("HELP".equals(row[1])) {
                listed = true;
                ok("the menu calls it \"" + row[0] + "\"", "Help".equals(row[0]));
                ok("and says what it is", row[2] != null && row[2].length() > 30);
            }
        }
        ok("Help is on the menu", listed);

        final OuraniaWindow[] w = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        java.awt.Container content =
            (java.awt.Container) CheckReflect.get(w[0], "contentPanel");
        boolean carded = false;
        for (java.awt.Component c : content.getComponents()) {
            java.awt.LayoutManager lm = content.getLayout();
            if (lm instanceof java.awt.CardLayout && c.getName() != null) {
                carded |= "HELP".equals(c.getName());
            }
        }
        // CardLayout does not expose its names through the components, so ask it to show the
        // card: an unknown name is a silent no-op, and a known one changes which child is
        // visible. That difference is the assertion.
        java.awt.CardLayout cards = (java.awt.CardLayout) content.getLayout();
        final java.awt.Component[] before = new java.awt.Component[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            cards.show(content, "SKYMAP");
            before[0] = visible(content);
            cards.show(content, "HELP");
        });
        final java.awt.Component[] after = new java.awt.Component[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> after[0] = visible(content));
        ok("showing HELP changes which card is up", before[0] != after[0] && after[0] != null);
        ok("and the card is a scroll pane holding the page" + (carded ? "" : ""),
            after[0] instanceof javax.swing.JScrollPane);

        javax.swing.SwingUtilities.invokeAndWait(() -> w[0].dispose());
    }

    private static java.awt.Component visible(java.awt.Container content) {
        for (java.awt.Component c : content.getComponents()) {
            if (c.isVisible()) {
                return c;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- plumbing

    /** The page escapes its inputs, so an expected string has to be escaped to be found. */
    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String head(List<String> list) {
        return list.isEmpty() ? "none"
            : list.size() + ", first: " + list.subList(0, Math.min(5, list.size()));
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
