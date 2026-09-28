package com.zodiacomputing.ourania.gui;

import java.awt.Component;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * What a screen reader is told about a control, decided in one place (G14).
 *
 * <p><b>Swept for on 28 September and there was nothing:</b> no {@code getAccessibleContext}, no
 * {@code setAccessibleName}, no accessible description anywhere in the tree. Swing gives a control
 * a default accessible name from its own text, which is why nobody noticed - a button reading
 * "Reset" announces "Reset". But most of this app's settings screen is spinners, combo boxes and
 * colour swatches whose words live in a <i>separate</i> label beside them, and those announce
 * nothing at all. A reader using a screen reader met 221 controls of which the majority were
 * anonymous.
 *
 * <p><b>The names are not written again here.</b> {@link SettingsSearch} already solves the harder
 * half of this problem - given a control, what is it called and what section is it in - and solves
 * it by reading the built screen rather than from a list, which is why H4b's search picked up a
 * control added a commit later without being told. Accessibility asks the same question, so it
 * asks the same code. A second naming rule would be a second rule to drift, and this project has
 * watched that happen to house systems, ring words and the isMinor test.
 *
 * <p><b>The description carries the section and the hover text.</b> "Sun" is not enough on a screen
 * where four tabs each have a Sun control; "Sun, orb width, Bodies &amp; Points" is what tells a
 * reader which one they are on.
 */
final class Accessibility {

    private Accessibility() { }

    /**
     * Names every control on the settings screen from the search index.
     *
     * <b>Called once the screen is built, not as each control is made.</b> A control's name comes
     * from the label beside it, so it is not knowable until the layout exists - which is the same
     * reason the search index is built from the finished tree.
     *
     * @return how many controls were given a name, for the check to compare against the index
     */
    static int nameSettings(JTabbedPane tabs) {
        int named = 0;
        for (SettingsSearch.Hit hit : SettingsSearch.index(tabs)) {
            if (!(hit.control instanceof JComponent)) {
                continue;
            }
            String name = hit.name == null ? "" : hit.name.trim();
            if (name.isEmpty()) {
                continue;
            }
            StringBuilder where = new StringBuilder(name);
            if (hit.section != null && !hit.section.isEmpty()) {
                where.append(", ").append(hit.section);
            }
            if (hit.tabTitle != null && !hit.tabTitle.isEmpty()) {
                where.append(", ").append(hit.tabTitle);
            }
            String about = hit.about == null ? "" : hit.about.trim();
            name(hit.control, name, about.isEmpty() ? where.toString()
                : where + ". " + about);
            named++;
        }
        return named;
    }

    /**
     * Gives one component a name and a description.
     *
     * <b>Never throws and never overwrites a name a component already carries.</b> A JButton
     * reading "Reset" already announces itself correctly; replacing that with a derived name would
     * be this class making a screen worse in the name of improving it.
     */
    static void name(Component c, String name, String description) {
        if (!(c instanceof JComponent) || name == null || name.trim().isEmpty()) {
            return;
        }
        AccessibleContext ctx = c.getAccessibleContext();
        if (ctx == null) {
            return;
        }
        if (ctx.getAccessibleName() == null || ctx.getAccessibleName().trim().isEmpty()) {
            ctx.setAccessibleName(name.trim());
        }
        // <b>The description is replaced when it is Swing's own, and only then.</b> A JComponent
        // with a tooltip already answers getAccessibleDescription with that tooltip, so the
        // never-overwrite rule above left 221 of 228 controls announcing their hover text and not
        // where they are - which measured as 7 of 228 carrying a tab name. What is written here
        // is a superset: the control, its section, its tab, and then the same tooltip. Anything
        // somebody set deliberately is still left alone.
        String existing = ctx.getAccessibleDescription();
        String tip = c instanceof JComponent ? ((JComponent) c).getToolTipText() : null;
        boolean swingsOwn = existing == null || existing.trim().isEmpty()
            || (tip != null && existing.equals(tip));
        if (description != null && !description.trim().isEmpty() && swingsOwn) {
            ctx.setAccessibleDescription(description.trim());
        }
    }

    /**
     * What a component announces, or "" - the question a check asks and a reader's software asks.
     */
    static String nameOf(Component c) {
        if (c == null) {
            return "";
        }
        AccessibleContext ctx = c.getAccessibleContext();
        String n = ctx == null ? null : ctx.getAccessibleName();
        return n == null ? "" : n.trim();
    }

    /** The same for the description. */
    static String descriptionOf(Component c) {
        if (c == null) {
            return "";
        }
        AccessibleContext ctx = c.getAccessibleContext();
        String d = ctx == null ? null : ctx.getAccessibleDescription();
        return d == null ? "" : d.trim();
    }

    // ------------------------------------------------------------------ contrast

    /**
     * WCAG relative luminance of an sRGB colour.
     *
     * <b>The published formula, not a brightness average.</b> A mean of the three channels calls
     * pure blue as bright as pure red, and a palette chosen that way puts unreadable blue text on
     * black while believing it passed.
     */
    static double luminance(java.awt.Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen())
            + 0.0722 * channel(c.getBlue());
    }

    private static double channel(int eightBit) {
        double s = eightBit / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    /** The WCAG contrast ratio between two colours, 1.0 (identical) to 21.0 (black on white). */
    static double contrast(java.awt.Color a, java.awt.Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        double hi = Math.max(la, lb);
        double lo = Math.min(la, lb);
        return (hi + 0.05) / (lo + 0.05);
    }

    /** WCAG AAA for normal text, and what the High Contrast template is held to. */
    static final double AAA = 7.0;
}
