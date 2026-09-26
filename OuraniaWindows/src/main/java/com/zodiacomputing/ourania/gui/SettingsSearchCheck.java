package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The settings search (H4b), held against the screen it searches.
 *
 * <p><b>Every naming rule tried here produced names that were wrong rather than missing</b>, and
 * that is what this suite is mostly for. A control with no name is obviously broken; a control
 * named after the control next to it looks perfect in a result list and takes the reader
 * confidently to the wrong place. Both defects below shipped in a draft and were caught by
 * measuring, not by reading the code:
 *
 * <ul>
 * <li><b>"the nearest preceding label"</b> named the Semisextile colour swatch <i>Conjunction</i>,
 *     because the orb grid runs swatch, checkbox, spinner and scanning backwards walks into the
 *     row above. Part C is that defect, stated generally.</li>
 * <li><b>"the nearest either way, follower wins"</b> fixed the grid and broke Engine Rules, where
 *     the row is label, combo, label, combo and the follower is the <i>next</i> setting's label.
 *     Part B's Engine Rules cases are that one.</li>
 * </ul>
 *
 * <p><b>Part B is the suite.</b> The rest describe properties; B asks what a reader would type
 * and asserts what they get, from a hand-written table. It is hand-written for the reason
 * {@code HouseSystemsCheck} Part A is: a check that asked the index what the index contains would
 * pass against any indexing rule at all, including the two wrong ones above.
 *
 * <p><b>It builds a real window</b>, which is slow, and it has to. Seven combos were re-parented
 * onto the Engine Rules tab from the chart's own control strip, and {@code new SettingsPanel(null)}
 * has none of them - which is how a first probe concluded the house system picker was not on the
 * settings screen at all, and nearly had me "fixing" a tab tooltip that was telling the truth.
 */
public final class SettingsSearchCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static List<SettingsSearch.Hit> index;
    private static JTabbedPane tabs;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        final OuraniaWindow[] win = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> win[0] = new OuraniaWindow());
        final SettingsPanel[] panel = new SettingsPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new SettingsPanel(win[0]));
        SwingUtilities.invokeAndWait(() -> {
            tabs = findTabs(panel[0]);
            // <b>The list the search box itself queries</b>, not one built here. A suite that
            // re-indexed would be testing this file's copy of the rule and not the screen's.
            index = panel[0].searchHits();
        });

        try {
            part("A: every control on the screen is in the index",
                SettingsSearchCheck::coverage);
            part("B: what a reader would type finds the right control",
                SettingsSearchCheck::queries);
            part("C: no control is named after a different control",
                SettingsSearchCheck::neighbours);
            part("D: a control is filed under the heading above it",
                SettingsSearchCheck::sections);
            part("E: every word has to match", SettingsSearchCheck::everyWord);
            part("F: nothing a reader sees is markup", SettingsSearchCheck::words);
            part("G: choosing a result actually goes there",
                () -> reveals(panel[0]));
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

    // ------------------------------------------------------------------ A

    /**
     * Walked again here, by a list of control classes written out in this file.
     *
     * <b>Deliberately a second opinion and not the same call.</b> "What counts as a control" is
     * itself one of the rules under test, so asking {@code SettingsSearch} which components it
     * considers controls and then checking it found those would pass against any answer.
     */
    private static void coverage() {
        List<Component> mine = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component page = tabs.getComponentAt(i);
            if (page instanceof Container) {
                walk((Container) page, mine);
            }
        }
        ok("the screen has more than two hundred controls (" + mine.size() + ")",
            mine.size() > 200);
        ok("the index holds exactly as many (" + index.size() + ")",
            index.size() == mine.size());

        for (Component c : mine) {
            boolean found = false;
            for (SettingsSearch.Hit h : index) {
                if (h.control == c) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ok("indexed: " + c.getClass().getSimpleName() + " \"" + describe(c) + "\"", false);
            }
        }
        checks++;   // the sweep above only speaks when it fails

        // <b>Every one of them has words.</b> A hit with no name cannot be recognised in a
        // result list even when the search finds it, and eleven controls on this screen carry no
        // text of their own at all.
        int unnamed = 0;
        for (SettingsSearch.Hit h : index) {
            if (h.name.isEmpty()) {
                unnamed++;
            }
        }
        ok("every control has a name (" + unnamed + " without)", unnamed == 0);

        // Four scroll panes contribute two arrow buttons each, and they are JButtons.
        for (SettingsSearch.Hit h : index) {
            if (up(h.control, JScrollBar.class)) {
                ok("a scrollbar arrow is not a setting: " + h, false);
            }
        }
        checks++;
    }

    // ------------------------------------------------------------------ B

    /**
     * A reader's words, and the control they should land on.
     *
     * Each row is query, the name expected at the top, and the tab it must be on.
     */
    private static final String[][] WANTED = {
        // The one that was missing entirely: a combo's options were not indexed, so the name of
        // the house system a reader wants was in no field anywhere.
        {"placidus", "Houses", "Engine Rules"},
        {"whole sign", "Houses", "Engine Rules"},
        {"koch", "Houses", "Engine Rules"},
        // Engine Rules: the tab where "nearest label, follower wins" took each combo's name from
        // the NEXT setting's label.
        {"lunar node", "Lunar Node", "Engine Rules"},
        {"black moon lilith", "Black Moon Lilith", "Engine Rules"},
        {"zodiac", "Zodiac", "Engine Rules"},
        {"sidereal", "Zodiac", "Engine Rules"},
        {"harmonic", "H", "Engine Rules"},
        // The orb grid and the body groups.
        {"quincunx", "Quincunx", "Aspects & Orbs"},
        {"thinnest line", "Thinnest line", "Globe & Display"},
        {"thickest line", "Thickest line", "Globe & Display"},
        {"restore defaults", "Restore defaults", "Bodies & Points"},
    };

    private static void queries() {
        for (String[] want : WANTED) {
            List<SettingsSearch.Hit> hits = SettingsSearch.find(index, want[0]);
            if (hits.isEmpty()) {
                ok("\"" + want[0] + "\" finds something", false);
                continue;
            }
            SettingsSearch.Hit top = hits.get(0);
            ok("\"" + want[0] + "\" finds " + want[1] + " (found " + top.name + ")",
                want[1].equals(top.name));
            ok("\"" + want[0] + "\" points at " + want[2] + " (points at " + top.tabTitle + ")",
                want[2].equals(top.tabTitle));
        }

        // Every body and every aspect is findable by its own name, which is the bulk of the
        // screen and far too many to write out one at a time.
        int missingBodies = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            String name = Bodies.at(i).name;
            if (SettingsSearch.find(index, name).isEmpty()) {
                missingBodies++;
                ok("\"" + name + "\" finds its own controls", false);
            }
        }
        ok("all " + Bodies.count() + " points are findable by name", missingBodies == 0);

        int missingAspects = 0;
        for (Aspects.Type t : Aspects.Type.values()) {
            if (SettingsSearch.find(index, t.label).isEmpty()) {
                missingAspects++;
                ok("\"" + t.label + "\" finds its own controls", false);
            }
        }
        ok("all " + Aspects.Type.values().length + " aspects are findable by name",
            missingAspects == 0);

        // A word that is on no control returns nothing rather than everything.
        ok("a word that is nowhere on the screen finds nothing",
            SettingsSearch.find(index, "zzzz no such setting").isEmpty());
        ok("an empty query finds nothing", SettingsSearch.find(index, "   ").isEmpty());
    }

    // ------------------------------------------------------------------ C

    /**
     * The defect that a result list cannot show you.
     *
     * <b>Each aspect has exactly three controls</b> - a colour swatch, a tick and a ceiling - and
     * each point on Bodies &amp; Points the same. If the naming rule slips by one row, one name
     * gets two and its neighbour gets four, and the totals say so without anyone having to know
     * which row moved.
     */
    private static void neighbours() {
        for (Aspects.Type t : Aspects.Type.values()) {
            int n = named(t.label, "Aspects & Orbs");
            ok(t.label + " has its three controls, colour, tick and ceiling (" + n + ")", n == 3);
        }
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            // <b>One space, where the screen writes three.</b> The checkbox is built as
            // glyph + "   " + name so the glyph and the word do not crowd each other, and
            // SettingsSearch.plain collapses runs of whitespace - so the indexed name has a
            // single space. Written out here rather than run through plain(), which is code
            // under test.
            String label = d.glyph.equals(d.name) ? d.name : d.glyph + " " + d.name;
            int n = named(label, "Bodies & Points");
            ok(d.name + " has its three controls, colour, tick and width (" + n + ")", n == 3);

            // <b>And a reader can tell them apart in the list.</b> Searching "lilith" returned
            // four rows of which three read identically - same name, same tab, same section -
            // because a point's colour, its tick and its width are all called after the point.
            java.util.List<String> kinds = new ArrayList<>();
            for (SettingsSearch.Hit h : index) {
                if (label.equals(h.name) && "Bodies & Points".equals(h.tabTitle)) {
                    kinds.add(h.kind());
                }
            }
            ok(d.name + "'s three controls read differently from one another " + kinds,
                kinds.size() == 3 && !kinds.get(0).equals(kinds.get(1))
                    && !kinds.get(1).equals(kinds.get(2))
                    && !kinds.get(0).equals(kinds.get(2)));
        }

        // <b>The column header is not a control's name.</b> When the rule scanned backwards, the
        // first swatch in the grid took the heading of the column above it.
        for (SettingsSearch.Hit h : index) {
            String n = h.name;
            ok("no control is called \"" + n + "\"",
                !"Ceiling°".equals(n) && !"Orb°".equals(n)
                    && !"Colour".equals(n) && !"Point".equals(n));
        }
    }

    private static int named(String name, String tab) {
        int n = 0;
        for (SettingsSearch.Hit h : index) {
            if (name.equals(h.name) && tab.equals(h.tabTitle)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ D

    private static void sections() {
        // <b>Filed under the nearest heading, not the first one on the tab.</b> Returning on the
        // first match put every control on Globe & Display under "Colour Template", the heading
        // that opens it, including the ones nine sections below.
        SettingsSearch.Hit fill = first("fill the lunar mansion");
        ok("the globe's mansion fill is on Globe & Display",
            fill != null && "Globe & Display".equals(fill.tabTitle));
        ok("and not filed under the heading that opens the tab ("
            + (fill == null ? "?" : fill.section) + ")",
            fill != null && !"Colour Template".equals(fill.section));

        // <b>A heading is marked, not recognised by its font.</b> Swing's default JLabel font is
        // bold, so taking any bold label for a heading made every row label on Engine Rules into
        // a section title - and each combo was filed under the setting above it.
        SettingsSearch.Hit zodiac = first("sidereal");
        ok("the zodiac combo sits under Calculation Variants ("
            + (zodiac == null ? "?" : zodiac.section) + ")",
            zodiac != null && "Calculation Variants".equals(zodiac.section));
        for (SettingsSearch.Hit h : index) {
            ok("\"" + h.section + "\" is a heading and not a row label",
                !h.section.endsWith(":"));
        }

        // Every section named by the index is a heading that is actually on that tab.
        for (SettingsSearch.Hit h : index) {
            if (h.section.isEmpty()) {
                continue;
            }
            ok("\"" + h.section + "\" is a real heading on " + h.tabTitle,
                headings(h.tab).contains(h.section));
        }
    }

    /** The marked headings on one tab, asked of the components rather than of the index. */
    private static List<String> headings(int tab) {
        List<String> out = new ArrayList<>();
        Component page = tabs.getComponentAt(tab);
        if (page instanceof Container) {
            headings((Container) page, out);
        }
        return out;
    }

    private static void headings(Container c, List<String> out) {
        for (Component kid : c.getComponents()) {
            if (kid instanceof javax.swing.JComponent
                && Boolean.TRUE.equals(
                    ((javax.swing.JComponent) kid).getClientProperty(SettingsSearch.HEADING))
                && kid instanceof javax.swing.JLabel) {
                out.add(SettingsSearch.plain(((javax.swing.JLabel) kid).getText()));
            }
            if (kid instanceof Container) {
                headings((Container) kid, out);
            }
        }
    }

    // ------------------------------------------------------------------ E

    private static void everyWord() {
        int orb = SettingsSearch.find(index, "orb").size();
        int sunOrb = SettingsSearch.find(index, "sun orb").size();
        ok("\"orb\" is all over this screen (" + orb + ")", orb > 20);
        ok("\"sun orb\" narrows it rather than widening it (" + sunOrb + ")",
            sunOrb > 0 && sunOrb < orb);

        // Any-word matching would make a second word unable to narrow anything, which is the
        // mutation this catches: it would return at least as many as one word alone.
        int sun = SettingsSearch.find(index, "sun").size();
        ok("\"sun orb\" is narrower than \"sun\" too (" + sunOrb + " against " + sun + ")",
            sunOrb <= sun);

        // Case is not something a reader should have to get right.
        ok("case does not matter",
            SettingsSearch.find(index, "PLACIDUS").size()
                == SettingsSearch.find(index, "placidus").size());
        ok("and neither does the space around it",
            SettingsSearch.find(index, "  placidus  ").size()
                == SettingsSearch.find(index, "placidus").size());
    }

    // ------------------------------------------------------------------ F

    private static void words() {
        // Nearly every hover on this screen is HTML. If the tags survived into the index, a
        // query of "b" or "br" would return the whole screen, and a result line would read as
        // markup.
        for (SettingsSearch.Hit h : index) {
            ok("the name of " + h.name + " is words, not markup",
                h.name.indexOf('<') < 0 && !h.name.contains("&nbsp"));
            ok("what " + h.name + " is about is words, not markup",
                h.about.indexOf('<') < 0 && !h.about.contains("&nbsp"));
        }
        int tagged = SettingsSearch.find(index, "br").size();
        ok("searching for a tag name does not return the screen (" + tagged + ")", tagged < 20);

        // An abbreviation gets its hover's first clause, because "H" on its own says nothing in
        // a list of results.
        SettingsSearch.Hit h = first("harmonic");
        ok("the harmonic control is labelled beyond its one letter ("
            + (h == null ? "?" : h.label()) + ")",
            h != null && h.label().length() > h.name.length() && h.label().startsWith(h.name));
    }

    // ------------------------------------------------------------------ G

    /**
     * The half an index cannot prove.
     *
     * <b>A search that finds the right control and does nothing with it is this project's most
     * logged defect wearing a new hat</b> - built, correct, and not wired to anything. Parts A to
     * F would all pass against a result list whose rows did nothing when clicked.
     */
    private static void reveals(SettingsPanel panel) {
        SettingsSearch.Hit onPoints = null;
        SettingsSearch.Hit onEngine = null;
        for (SettingsSearch.Hit h : index) {
            if (onPoints == null && "Bodies & Points".equals(h.tabTitle)) {
                onPoints = h;
            }
            if (onEngine == null && "Engine Rules".equals(h.tabTitle)) {
                onEngine = h;
            }
        }
        ok("there is something to reveal on both tabs", onPoints != null && onEngine != null);
        if (onPoints == null || onEngine == null) {
            return;
        }

        press(panel, onEngine);
        ok("revealing " + onEngine.name + " opens Engine Rules (tab "
            + tabs.getSelectedIndex() + ")", tabs.getSelectedIndex() == onEngine.tab);
        press(panel, onPoints);
        ok("revealing " + onPoints.name + " opens Bodies & Points (tab "
            + tabs.getSelectedIndex() + ")", tabs.getSelectedIndex() == onPoints.tab);

        // <b>The ring comes off again.</b> It is compounded outside whatever border the control
        // already had, so a flash that never ended would leave the control two pixels bigger and
        // permanently circled - and the next search would compound a second ring onto the first.
        javax.swing.JComponent target = (javax.swing.JComponent) onPoints.control;
        javax.swing.border.Border ringed = target.getBorder();
        // Waited on with a deadline rather than slept through: ec44b792's rule, after two suites
        // flaked under regression load on a fixed sleep.
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline && target.getBorder() == ringed) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException stop) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        ok("the ring comes off " + onPoints.name + " again", target.getBorder() != ringed);

        // The "nothing matches" row is a String, not a Hit, and pressing it must do nothing at
        // all rather than throw on the way.
        int before = tabs.getSelectedIndex();
        press(panel, "Nothing on this screen matches “xyzzy”");
        ok("pressing the no-results line does nothing", tabs.getSelectedIndex() == before);
    }

    private static void press(SettingsPanel panel, Object value) {
        try {
            SwingUtilities.invokeAndWait(() -> panel.revealSetting(value));
            // revealSetting scrolls and rings on a later pass, once the tab has been laid out.
            SwingUtilities.invokeAndWait(() -> { });
        } catch (Exception e) {
            fail("revealing " + value + " threw " + e);
        }
    }

    // ------------------------------------------------------------------

    private static SettingsSearch.Hit first(String query) {
        List<SettingsSearch.Hit> hits = SettingsSearch.find(index, query);
        return hits.isEmpty() ? null : hits.get(0);
    }

    private static boolean up(Component c, Class<?> type) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (type.isInstance(p)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Component c) {
        if (c instanceof AbstractButton) {
            return String.valueOf(((AbstractButton) c).getText());
        }
        return String.valueOf(c.getName());
    }

    /** This file's own idea of what a control is - see {@link #coverage()}. */
    private static void walk(Container c, List<Component> out) {
        if (c instanceof JScrollBar) {
            return;
        }
        for (Component kid : c.getComponents()) {
            if (kid instanceof AbstractButton || kid instanceof JComboBox
                || kid instanceof JSpinner || kid instanceof JTextField) {
                out.add(kid);
                continue;
            }
            if (kid instanceof Container) {
                walk((Container) kid, out);
            }
        }
    }

    private static JTabbedPane findTabs(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JTabbedPane) {
                return (JTabbedPane) k;
            }
            if (k instanceof Container) {
                JTabbedPane t = findTabs((Container) k);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
        System.out.println();
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        try {
            body.run();
        } catch (RuntimeException e) {
            fail(name + " threw " + e);
        }
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void fail(String label) {
        checks++;
        failures.add(label);
    }

    static {
        // Keeps the locale out of the comparisons above.
        Locale.setDefault(Locale.ROOT);
    }
}
