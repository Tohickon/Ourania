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
        // desktop is set in; Arial was inherited from the decompiled source.
        return font(FAMILY, style, size);
    }

    /**
     * A named face that still draws the symbols it does not have. Every font a component is set
     * in comes from here; GlyphCheck holds the gui package to that.
     *
     * <b>new Font("Segoe UI", ...) draws every symbol in the app as an empty box.</b> A physical
     * font has no fallback: rendered, U+2648 leaves exactly the ink of a missing glyph, and so
     * did the drawer triangles, the swap arrow, and the releasing panel's star and link. The
     * glyphs had been swapped for "+", "&lt;" and "v" one at a time, each comment blaming "this
     * font". HTML panes never showed it, because the HTML renderer falls back on its own; nor
     * did SansSerif, which is a logical font and composites by contract.
     *
     * <p>StyleContext.getFont is Swing's public way to the same composite for a physical face:
     * Latin text keeps that face's exact metrics (measured: "Aries W" is 68px either way) and
     * anything missing is drawn from the platform's fallback fonts. deriveFont keeps the
     * fallback and drops the UIResource marker, so a look-and-feel refresh cannot swap the font
     * back out; new Font(font.getAttributes()) would lose the fallback, measured.
     */
    static Font font(String family, int style, int size) {
        int scaled = scaled(size);
        return javax.swing.text.StyleContext.getDefaultStyleContext()
            .getFont(family, style, scaled).deriveFont(style, (float) scaled);
    }

    // ---- type scale (G14) --------------------------------------------------------------

    /** The setting a reader turns up; outside {@code isPersonal}, so it travels in a saved set. */
    static final String SCALE_KEY = "ui.font.scale";

    /** Smallest and largest multiplier the app will act on. */
    static final double SCALE_MIN = 1.0;
    static final double SCALE_MAX = 2.0;

    /** No font is allowed below this, whatever arithmetic says. */
    static final int MIN_SIZE = 8;

    /**
     * Every font in the app, multiplied - which is possible because there is only one place.
     *
     * <p><b>One multiplier, because E11 already did the hard part.</b> Every font a component is set
     * in comes from {@link #font(String, int, int)}, and {@code GlyphCheck} Part C holds the gui
     * package to that: "no physical face is constructed outside Theme.font". So scaling here provably
     * reaches every font in the app, and G14's first commit is arithmetic rather than a sweep through
     * a hundred call sites. The accessibility row is expensive; this part of it is not, and that is
     * only true because a previous commit refused to let fonts be made in two places.
     *
     * <p><b>Out of range is refused, not clamped</b> - H1's rule for orbs, for the same reason: a
     * settings file asking for 40 gets the default back, because quietly making it 2.0 would say the
     * number was accepted. The screen clamps what a reader can choose; the file does not get to lie.
     *
     * <p><b>Resolved once per run.</b> Fonts are built as components are built, so a scale changed
     * mid-session would apply to whatever is constructed afterwards and not to what is already on
     * screen - a half-scaled window. The setting takes effect on restart and the control says so.
     */
    static int scaled(int size) {
        double s = scale();
        return s == 1.0 ? size : Math.max(MIN_SIZE, (int) Math.round(size * s));
    }

    /** The multiplier in force, or 1.0 when the setting is absent, unreadable or out of range. */
    static double scale() {
        Double cached = scale;
        if (cached != null) {
            return cached;
        }
        double s = 1.0;
        try {
            double asked = Double.parseDouble(Settings.get(SCALE_KEY, "1.0").trim());
            if (asked >= SCALE_MIN && asked <= SCALE_MAX) {
                s = asked;
            }
        } catch (RuntimeException notANumber) {
            // A hand-edited file saying "big" gets the default, like any other unreadable value.
        }
        return scale = s;
    }

    /** Asked for on every font, so worked out once per run - and forgotten when a suite sets it. */
    private static volatile Double scale;

    /** Drops the cached multiplier. For the suite, and for a settings reload. */
    static void forgetScale() {
        scale = null;
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
