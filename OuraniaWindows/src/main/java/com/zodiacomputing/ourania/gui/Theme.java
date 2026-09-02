package com.zodiacomputing.ourania.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * One palette, one type scale, one spacing grid - for the whole app.
 *
 * <p><b>This exists because the app had no visual system at all, and it was measurable.</b>
 * Counted on 2026-09-02: <b>59 hand-rolled {@code new Color(...)} values across ten panels</b>
 * and <b>twelve different font specifications</b> - Arial at 11, 12, 13, 14, 18, 20 and 24, in
 * plain, bold and italic, plus a stray Segoe UI Symbol. ChartSetupPanel alone carried nine
 * colours and drew white hairline borders; the drawers carried six each in a different family;
 * SkymapPanel's readings were purple, the sidebar's rows were slate, and the interpretation
 * HTML used a fourth set again in hex. Nothing was wrong with any single one of them, and
 * together they read as several different applications sharing a window.
 *
 * <h2>What this can and cannot do</h2>
 *
 * <b>It can do the part that actually makes an interface look designed</b>: one family of
 * surfaces, one accent, consistent radius, consistent padding, a type scale with three sizes
 * instead of seven, and controls that stop looking like stock Windows widgets.
 *
 * <b>It cannot do backdrop blur.</b> Swing has no compositor access - a panel cannot sample and
 * blur what is painted beneath it - so the frosted, semi-transparent glass in the reference
 * mockups is not reachable from here at any effort. What is reachable, and is what this uses,
 * is a translucent surface over a dark ground with a light hairline edge, which reads as glass
 * without being it. Said plainly rather than approximated badly and left to be noticed.
 */
final class Theme {

    private Theme() { }

    // ---- surfaces ---------------------------------------------------------------------
    // A navy-black ground rather than pure #000: black flattens everything on top of it and
    // is why the old panels read as cut-out rectangles rather than as layers.

    /** The window ground, behind everything. */
    static final Color BG = new Color(9, 12, 20);
    /** A panel sitting on the ground - drawers, sidebars. */
    static final Color SURFACE = new Color(16, 21, 33);
    /** A card sitting on a panel - accordion bodies, profile cards. */
    static final Color SURFACE_2 = new Color(22, 28, 43);
    /** A raised row - headers, buttons at rest. */
    static final Color SURFACE_3 = new Color(30, 38, 57);
    /** Hairline edges. Low contrast on purpose; a border should separate, not divide. */
    static final Color EDGE = new Color(48, 60, 84);
    /** The edge of something active. */
    static final Color EDGE_ACTIVE = new Color(56, 132, 190);

    // ---- accents ----------------------------------------------------------------------

    /** The primary action and the open state. */
    static final Color ACCENT = new Color(56, 160, 220);
    static final Color ACCENT_HOVER = new Color(74, 182, 242);
    /** Readings and interpretive surfaces. */
    static final Color VIOLET = new Color(139, 122, 214);
    static final Color VIOLET_HOVER = new Color(160, 144, 232);
    /** Warnings, patterns, anything the eye should find first. */
    static final Color GOLD = new Color(226, 178, 88);

    // ---- text -------------------------------------------------------------------------

    static final Color TEXT = new Color(226, 232, 240);
    static final Color TEXT_DIM = new Color(148, 160, 180);
    static final Color TEXT_ON_ACCENT = Color.WHITE;

    // ---- type -------------------------------------------------------------------------
    // Three sizes and two weights. Seven sizes is not a scale, it is an accident.

    private static final String FAMILY = "Segoe UI";

    /** Screen and panel titles. */
    static final Font TITLE = font(Font.BOLD, 17);
    /** Section headers, button labels, anything that names a thing. */
    static final Font HEADING = font(Font.BOLD, 12);
    /** Ordinary content. */
    static final Font BODY = font(Font.PLAIN, 12);
    /** Captions, field labels, secondary detail. */
    static final Font SMALL = font(Font.PLAIN, 11);

    private static Font font(int style, int size) {
        // Segoe UI is present on every supported Windows version and is what the rest of the
        // desktop is set in; Arial was inherited from the decompiled source. Font falls back
        // on its own if it is ever missing, so this needs no guard.
        return new Font(FAMILY, style, size);
    }

    // ---- spacing ----------------------------------------------------------------------
    // A 4px grid. Every gap in the app should be one of these, not a number someone typed.

    static final int GAP_S = 4;
    static final int GAP = 8;
    static final int GAP_L = 14;
    static final int RADIUS = 10;

    /** Padding inside a card or a panel. */
    static javax.swing.border.Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /**
     * A rounded, hairline-edged surface - the shape everything in this app should have.
     *
     * Square corners are most of why the old panels looked like a form from 2003; a 10px
     * radius costs nothing and changes the read of the whole window.
     */
    static javax.swing.border.Border card(Color edge, int padding) {
        return BorderFactory.createCompoundBorder(
            new RoundEdge(edge, RADIUS), pad(padding, padding, padding, padding));
    }

    /** Paints a rounded, filled surface. For components that draw their own background. */
    static void fillCard(Graphics g, JComponent c, Color fill, Color edge) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, RADIUS, RADIUS);
        if (edge != null) {
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, RADIUS, RADIUS);
        }
        g2.dispose();
    }

    /** A hairline border with rounded corners. */
    static final class RoundEdge extends AbstractBorder {

        private final Color colour;
        private final int radius;

        RoundEdge(Color colour, int radius) {
            this.colour = colour;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colour);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }
}
