package com.zodiacomputing.ourania.gui;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finding one control on a screen that has 216 of them (H4b).
 *
 * <p><b>Nothing here is a second list of the settings.</b> The index is read off the built screen,
 * so a control added later is searchable the day it is added and nobody has to remember to come
 * back. That is the same decision {@code SettingsPanel} already made about the checkboxes - it
 * generates them from {@code Bodies.ALL} rather than listing them - and for the same reason: a
 * hand-kept copy is the one that goes stale.
 *
 * <p><b>The naming rule was measured into shape, not reasoned into it.</b> Most controls here
 * carry no words of their own: a colour swatch is a coloured square, a spinner is a number, a
 * combo is a list. Their name lives in a neighbouring label, and two obvious rules both produced
 * names that were <i>wrong</i> rather than missing:
 *
 * <ul>
 * <li><b>"the nearest preceding label"</b> named the Semisextile colour swatch <i>Conjunction</i>.
 *     In the orb grid the order is swatch, checkbox, spinner, so scanning backwards from a swatch
 *     walks past its own row into the row above. <b>A control named after a different control is
 *     worse than one with no name</b>, because the search takes you confidently to the wrong
 *     place.</li>
 * <li><b>"the nearest either way, the follower winning a tie"</b> fixed the grid and broke the
 *     Engine Rules tab, where the row is label, combo, label, combo - both neighbours are one
 *     step away and the follower is the <i>next</i> setting's label.</li>
 * </ul>
 *
 * <p>What separates them is a convention the screen already keeps: <b>a label ending in a colon
 * is a prompt for what comes after it.</b> "Lunar Node:" labels the combo to its right; the
 * checkbox reading "&#9737; Sun" does not label anything. So a preceding label with a colon wins
 * outright, and only when there is none does nearest-wins apply.
 *
 * <p><b>Headings say so themselves</b> rather than being recognised by their font. The first
 * attempt took any bold label for a heading, and Swing's default {@code JLabel} font is bold, so
 * every row label on the Engine Rules tab became a section title and each control was filed under
 * the setting above it. {@code SettingsPanel.heading} marks what it makes, which means a heading
 * added later is marked by construction.
 */
public final class SettingsSearch {

    /**
     * Marks a label as a section heading, not a caption for one control.
     *
     * <b>Set by the two methods that make headings</b> - {@code SettingsPanel.heading} and the
     * group-box title in {@code groupPanel} - so that this file does not have to guess from a
     * font, and so that a heading written next year arrives already marked.
     */
    public static final String HEADING = "settings.heading";

    /** One control a reader could be looking for. */
    public static final class Hit {

        /** The control itself, so selecting a result can reveal the thing rather than describe it. */
        public final Component control;
        /** What it is called: its own words, or the label that prompts it. */
        public final String name;
        /** The heading it sits under, or "" at the top of a tab. */
        public final String section;
        /** Which tab holds it. */
        public final int tab;
        /** That tab's title, for the result line. */
        public final String tabTitle;
        /** Its hover text, flattened out of HTML. */
        public final String about;

        private final String haystack;

        Hit(Component control, String name, String section, int tab, String tabTitle,
            String about) {
            this.control = control;
            this.name = name;
            this.section = section;
            this.tab = tab;
            this.tabTitle = tabTitle;
            this.about = about;
            this.haystack = (name + " " + section + " " + tabTitle + " " + about
                + " " + options(control)).toLowerCase(Locale.ROOT);
        }

        /** Where to find it, as a reader would say it. */
        public String where() {
            return section.isEmpty() ? tabTitle : tabTitle + " › " + section;
        }

        /**
         * What to put on the result line.
         *
         * <b>Five controls on this screen are labelled with an abbreviation</b> - H, Anim, Pin -
         * because they were re-parented from the chart's own control strip, where space is
         * scarce and the wheel beside them supplies the context. In a list of search results
         * that context is gone, and a row reading "H" tells a reader nothing. Where the name is
         * too short to mean anything on its own, the first clause of its hover goes with it.
         * The name itself is left alone, because it is what the screen says and what an exact
         * match is measured against.
         */
        public String label() {
            if (name.length() > 3 || about.isEmpty()) {
                return name;
            }
            String first = about;
            int stop = first.indexOf(". ");
            if (stop > 0) {
                first = first.substring(0, stop);
            }
            if (first.length() > 46) {
                first = first.substring(0, 45) + "…";
            }
            return name + " — " + first;
        }

        /**
         * Which of a point's three controls this is.
         *
         * <b>Measured on a photograph of the thing.</b> Searching "lilith" returned four rows,
         * three of them reading "Black Moon Lilith - Bodies &amp; Points - Calculated Points &amp;
         * Angles" and indistinguishable from one another: they are its colour, its tick and its
         * width. A list whose rows a reader cannot tell apart has not answered the question.
         *
         * <b>A colour swatch says so in its hover, so that is where the word comes from</b>
         * rather than from "a button with no text on it", which would be an inference about a
         * button rather than a fact about this one.
         */
        public String kind() {
            if (control instanceof javax.swing.JCheckBox) {
                return "tick";
            }
            if (control instanceof JComboBox) {
                return "choice";
            }
            if (control instanceof JSpinner) {
                return "number";
            }
            if (control instanceof JTextField) {
                return "text";
            }
            if (control instanceof AbstractButton) {
                return about.startsWith("The colour") ? "colour" : "button";
            }
            return "";
        }

        @Override
        public String toString() {
            return label() + " [" + kind() + "] (" + where() + ")";
        }
    }

    /**
     * Everything a combo offers, so the search can be asked for the answer rather than the
     * question.
     *
     * <b>Measured: "placidus" found nothing.</b> The house system combo is called "Houses" on the
     * chart strip it was re-parented from and carries no hover at all, so the one word a reader
     * would actually type was in no index. The same was true of "whole sign", "koch", "mean
     * node" and every ayanamsa.
     *
     * <b>The options, not the selection.</b> Which item is selected changes under the reader, so
     * a search keyed on it would find a control on Monday and not on Tuesday; the list of what a
     * control <i>can</i> be set to is fixed for the life of the screen.
     */
    private static String options(Component c) {
        if (!(c instanceof JComboBox)) {
            return "";
        }
        JComboBox<?> combo = (JComboBox<?>) c;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item != null) {
                out.append(' ').append(plain(String.valueOf(item)));
            }
        }
        return out.toString();
    }

    private SettingsSearch() { }

    // ------------------------------------------------------------------ indexing

    /** Every control under every tab, in the order a reader would meet them. */
    public static List<Hit> index(JTabbedPane tabs) {
        List<Hit> out = new ArrayList<>();
        if (tabs == null) {
            return out;
        }
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component page = tabs.getComponentAt(i);
            if (page instanceof Container) {
                collect((Container) page, i, tabs.getTitleAt(i), out);
            }
        }
        return out;
    }

    private static void collect(Container c, int tab, String tabTitle, List<Hit> out) {
        // <b>A scrollbar's arrows are JButtons and are not settings.</b> Four of them were in the
        // first count, one pair per scroll pane, with no words at all - which is how they were
        // noticed.
        if (c instanceof JScrollBar) {
            return;
        }
        for (Component kid : c.getComponents()) {
            if (isControl(kid)) {
                out.add(new Hit(kid, name(kid), section(kid), tab, tabTitle,
                    plain(tip(kid))));
                continue;
            }
            if (kid instanceof Container) {
                collect((Container) kid, tab, tabTitle, out);
            }
        }
    }

    /**
     * What counts as a setting a reader could be hunting for.
     *
     * <b>Every button, including the ones that act rather than store.</b> Select all, Restore
     * defaults and Reset are exactly the things somebody opens the screen to find, and a search
     * that only knew about stored values would miss all of them.
     */
    static boolean isControl(Component c) {
        return c instanceof AbstractButton || c instanceof JComboBox || c instanceof JSpinner
            || c instanceof JTextField;
    }

    /**
     * Its own words; the label that prompts it; the nearest label either way; or its hover.
     *
     * See the class comment for why the order is this and not something simpler.
     */
    static String name(Component c) {
        String own = text(c);
        if (!own.isEmpty()) {
            return own;
        }
        Container parent = c.getParent();
        if (parent != null) {
            Component[] kids = parent.getComponents();
            int at = indexOf(kids, c);

            // A label ending in a colon is a prompt for what follows it, wherever the nearest
            // other words happen to be.
            for (int i = at - 1; i >= 0; i--) {
                String before = text(kids[i]);
                if (!before.isEmpty()) {
                    if (before.endsWith(":")) {
                        return before.substring(0, before.length() - 1).trim();
                    }
                    break;
                }
            }

            for (int step = 1; step < kids.length; step++) {
                if (at + step < kids.length) {
                    String after = text(kids[at + step]);
                    if (!after.isEmpty()) {
                        return after;
                    }
                }
                if (at - step >= 0) {
                    String before = text(kids[at - step]);
                    if (!before.isEmpty()) {
                        return before;
                    }
                }
            }
        }
        // Several colour buttons carry no words anywhere near them and say what they are only in
        // the hover - "Fire - this one moves a lot at once...". The first clause is the name.
        String tip = plain(tip(c));
        if (!tip.isEmpty()) {
            int dash = tip.indexOf(" - ");
            if (dash > 0 && dash < 30) {
                return tip.substring(0, dash);
            }
            int stop = tip.indexOf(". ");
            if (stop > 0 && stop < 40) {
                return tip.substring(0, stop);
            }
            return tip.length() > 40 ? tip.substring(0, 39) + "…" : tip;
        }
        return "";
    }

    /**
     * The heading it sits under.
     *
     * <b>The nearest heading passed on the way down, not the first.</b> Returning on the first
     * match files every control on a tab under whatever heading opens it, however many sections
     * below it the control actually sits - which is what the first version did, putting the
     * globe's fills under "Colour Template" nine sections above them.
     */
    static String section(Component c) {
        Component below = c;
        for (Container up = c.getParent(); up != null; below = up, up = up.getParent()) {
            String nearest = "";
            for (Component kid : up.getComponents()) {
                if (kid == below) {
                    break;
                }
                if (kid instanceof JComponent
                    && Boolean.TRUE.equals(((JComponent) kid).getClientProperty(HEADING))
                    && kid instanceof JLabel) {
                    nearest = plain(((JLabel) kid).getText());
                }
            }
            if (!nearest.isEmpty()) {
                return nearest;
            }
        }
        return "";
    }

    // ------------------------------------------------------------------ searching

    /**
     * The controls matching every word typed, best first.
     *
     * <b>Every word, not any.</b> "sun orb" should find the Sun's width and not every control
     * whose hover happens to say "orb" - which on this screen is most of two tabs.
     */
    public static List<Hit> find(List<Hit> all, String query) {
        List<Hit> out = new ArrayList<>();
        if (all == null || query == null) {
            return out;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return out;
        }
        String[] words = q.split("\\s+");
        List<Hit> byName = new ArrayList<>();
        List<Hit> byStart = new ArrayList<>();
        List<Hit> byWhere = new ArrayList<>();
        for (Hit h : all) {
            boolean everyWord = true;
            for (String w : words) {
                if (!h.haystack.contains(w)) {
                    everyWord = false;
                    break;
                }
            }
            if (!everyWord) {
                continue;
            }
            String lower = h.name.toLowerCase(Locale.ROOT);
            if (lower.equals(q)) {
                byName.add(h);
            } else if (lower.startsWith(q) || lower.contains(q)) {
                byStart.add(h);
            } else {
                byWhere.add(h);
            }
        }
        out.addAll(byName);
        out.addAll(byStart);
        out.addAll(byWhere);
        return out;
    }

    // ------------------------------------------------------------------ words

    /**
     * HTML hover text as the words in it.
     *
     * <b>Searching the markup would match on tag names.</b> Nearly every tooltip on this screen
     * is HTML, so a query of "b" or "br" would otherwise return the whole screen.
     */
    public static String plain(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String out = s.replaceAll("<[^>]*>", " ");
        out = out.replace("&deg;", "°").replace("&rarr;", "→")
            .replace("&amp;", "&").replace("&nbsp;", " ").replace("&rsaquo;", "›");
        out = out.replaceAll("&[a-zA-Z]+;", " ");
        return out.replaceAll("\\s+", " ").trim();
    }

    private static String text(Component c) {
        // A combo's "text" is whichever item is selected, which changes under the reader and is
        // not what the control is called.
        if (c instanceof JComboBox || c instanceof JSpinner) {
            return "";
        }
        if (c instanceof AbstractButton) {
            return plain(((AbstractButton) c).getText());
        }
        if (c instanceof JLabel) {
            return plain(((JLabel) c).getText());
        }
        return "";
    }

    private static String tip(Component c) {
        if (!(c instanceof JComponent)) {
            return "";
        }
        String t = ((JComponent) c).getToolTipText();
        return t == null ? "" : t;
    }

    private static int indexOf(Component[] kids, Component c) {
        for (int i = 0; i < kids.length; i++) {
            if (kids[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
