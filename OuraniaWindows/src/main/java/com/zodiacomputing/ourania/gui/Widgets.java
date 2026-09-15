package com.zodiacomputing.ourania.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Shared Swing styling, for the rules that have to hold on every screen.
 *
 * There is one rule in here so far and it earned its own file the hard way. The dropdown
 * fix below was written and documented inside SkymapPanel, which fixed that panel's six
 * combos; MainMenuPanel styled its navigation combo by hand, the same wrong way the
 * original had, and so stayed invisible. The whole navigation menu - including the only
 * route to the Settings screen - read as an empty white box for as long as the fix lived
 * somewhere the menu could not reach it.
 *
 * That is the project's recurring defect exactly: one rule, two implementations, one of
 * them corrected. Anything styled on more than one screen belongs here.
 */
public final class Widgets {

    // ---------------------------------------------------------------- palette
    //
    // <b>One dark system, and the glyphs keep their element colours.</b> The wheel and the
    // placements list colour bodies by element and that is the most informative thing on
    // screen - nothing here touches ELEMENT_COLORS. What this governs is the chrome AROUND
    // the chart, which until 2026-08-31 was whatever the Windows look-and-feel happened to
    // paint: pale scrollbars and grey arrow buttons sitting inside black panels.
    //
    // Neutrals carry a slight blue bias toward the accent rather than being pure grey, so
    // the chrome reads as chosen rather than as the toolkit default it used to be.

    /** The ground the wheel is drawn on. Unchanged - the chart has always been black. */
    public static final Color BG        = Color.BLACK;
    /** Panel fill, one step off the ground so an edge is visible without a border. */
    public static final Color PANEL     = new Color(18, 19, 24);
    /** Hairlines and inactive edges. */
    public static final Color RULE      = new Color(44, 47, 58);
    /** Body text. */
    public static final Color TEXT      = new Color(228, 229, 234);
    /** Secondary text: timestamps, coordinates, the "no patterns" note. */
    public static final Color TEXT_DIM  = new Color(150, 154, 166);
    /** Disabled foreground. Dim, but of this palette rather than the LAF grey. */
    public static final Color TEXT_OFF  = new Color(96, 100, 112);

    /** The interactive accent. Kept from the original so nothing shifts hue. */
    public static final Color ACCENT       = new Color(60, 120, 200);
    public static final Color ACCENT_HOVER = new Color(80, 140, 220);
    /** Disabled accent - same hue, drained, so "off" still reads as the same control. */
    public static final Color ACCENT_OFF   = new Color(38, 52, 72);

    /**
     * Dropdown fill.
     *
     * <b>Was the accent itself</b>, which put seven solid blue blocks along the control strip
     * and two more in the sidebar - on a dark ground they read as the most important things in
     * the window, which they are not. A dropdown is a surface you read a value off; the accent
     * belongs on the one control you are meant to press.
     */
    public static final Color COMBO_BG = Theme.SURFACE_3;

    private static final Font COMBO_FONT = Theme.font("Arial", Font.BOLD, 12);

    private Widgets() { }

    /**
     * Dark scrollbars, because the stock ones were the only pale thing in a black panel.
     *
     * <b>The most visible defect in the window.</b> The placements list sits on black with
     * element-coloured glyphs, and the look-and-feel painted its track and thumb in light
     * grey - so the eye went to the scrollbar rather than the chart.
     *
     * <b>No arrow buttons.</b> BasicScrollBarUI insists on creating them, so they are made
     * zero-sized rather than removed; returning null is dereferenced during layout.
     */
    public static void styleScrollPane(JScrollPane pane) {
        pane.setBorder(BorderFactory.createLineBorder(RULE));
        pane.setBackground(PANEL);
        pane.getViewport().setBackground(PANEL);
        styleScrollBar(pane.getVerticalScrollBar());
        styleScrollBar(pane.getHorizontalScrollBar());
    }

    /**
     * What a button is for, which is what decides its colour.
     *
     * <b>Before 2026-08-31 the wheel had eleven buttons in eleven colours and none of them
     * meant anything.</b> Two blues and a green across the five transport controls; four
     * READINGS in four separate purples - 120/90/180, 95/70/150, 115/60/130, 105/50/110 -
     * which are peers and looked like four different kinds of thing; and "Now" in red, which
     * reads as destructive when jumping to the present is the most harmless control there.
     *
     * Three roles, three treatments. Colour now says what a button does.
     */
    public enum Role {
        /** Step, play, pause, jump to now. Quiet: they are used constantly and mean little. */
        TRANSPORT,
        /** The one control the transport row is built around. */
        PRIMARY,
        /** Produces a reading: Snapshot, Report, Synthesize, Predict, Calendar. */
        READING
    }

    /**
     * Paints a button by role: rounded, themed, hover state, no focus ring.
     *
     * <b>Rounded and painted rather than filled.</b> A JButton with {@code setOpaque(true)} and
     * a background colour is a square block, and square blocks in a dark panel are most of why
     * this app read as a form rather than as a designed surface. The rounding costs one
     * override and changes the whole window; see {@link Theme}.
     */
    public static void styleButton(JButton b, Role role) {
        final Color base;
        final Color hover;
        final Color text;
        switch (role) {
            case PRIMARY:
                base = Theme.ACCENT; hover = Theme.ACCENT_HOVER;
                text = Theme.TEXT_ON_ACCENT; break;
            case READING:
                base = Theme.VIOLET; hover = Theme.VIOLET_HOVER;
                text = Theme.TEXT_ON_ACCENT; break;
            default:
                base = Theme.SURFACE_3; hover = new Color(44, 55, 80);
                text = Theme.TEXT; break;
        }
        b.setFont(Theme.HEADING);
        b.setForeground(text);
        b.setBackground(base);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        // <b>Not opaque.</b> An opaque button fills its own square before the rounded shape is
        // painted, leaving hard corners behind the curve - the artefact that makes a rounded
        // button look like a bug rather than a style.
        b.setOpaque(false);
        b.setBorder(Theme.pad(6, 12, 6, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setUI(new RoundButtonUI());
        for (java.awt.event.MouseListener old : b.getMouseListeners()) {
            if (old instanceof Hover) {
                b.removeMouseListener(old);
            }
        }
        b.addMouseListener(new Hover(b, base, hover));
    }

    /**
     * Draws the button as a rounded surface and lets the label paint on top.
     *
     * Extends BasicButtonUI rather than replacing it, so focus traversal, mnemonics, the
     * pressed state and the disabled colour all keep working - a fully hand-painted button
     * loses those quietly, and the loss only shows up in keyboard use.
     */
    private static final class RoundButtonUI extends javax.swing.plaf.basic.BasicButtonUI {
        @Override
        public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            JButton b = (JButton) c;
            Color fill = b.getBackground();
            if (!b.isEnabled()) {
                fill = Theme.SURFACE_2;
            } else if (b.getModel().isPressed()) {
                fill = fill.darker();
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), Theme.RADIUS, Theme.RADIUS);
            g2.dispose();
            super.paint(g, c);
        }
    }

    /** Named rather than anonymous so styleButton can replace one it already installed. */
    private static final class Hover extends java.awt.event.MouseAdapter {
        private final JButton b;
        private final Color base;
        private final Color hover;
        Hover(JButton b, Color base, Color hover) {
            this.b = b; this.base = base; this.hover = hover;
        }
        @Override public void mouseEntered(java.awt.event.MouseEvent e) {
            if (b.isEnabled()) { b.setBackground(hover); }
        }
        @Override public void mouseExited(java.awt.event.MouseEvent e) {
            b.setBackground(base);
        }
    }

    /** The same treatment for a bar that is not inside a scroll pane. */
    public static void styleScrollBar(JScrollBar bar) {
        if (bar == null) {
            return;
        }
        bar.setUnitIncrement(16);
        bar.setBackground(PANEL);
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = new Color(58, 62, 76);
                this.thumbDarkShadowColor = PANEL;
                this.thumbHighlightColor = new Color(74, 79, 96);
                this.thumbLightShadowColor = PANEL;
                this.trackColor = PANEL;
                this.trackHighlightColor = PANEL;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                b.setFocusable(false);
                return b;
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(PANEL);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? new Color(78, 84, 102) : new Color(58, 62, 76));
                g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
                g2.dispose();
            }
        });
    }

    /**
     * Makes a dropdown actually show what is selected in it.
     *
     * The Windows look-and-feel paints a non-editable combo's value area itself: it ignores
     * setBackground but honours setForeground. Blue-background/white-text styling therefore
     * produces white text on the LAF's white box, and the dropdown reads as blank - you
     * could not see whether Animate said Base or Both, which pin was active, or that the
     * navigation menu had a Settings entry at all. Swapping in the basic UI delegate puts
     * both colours back under our control.
     *
     * <b>Deliberately installs no custom cell renderer.</b> BasicComboBoxUI paints the
     * closed value area through the renderer as well as the popup rows, so a renderer that
     * hardcodes the blue and the white also paints a DISABLED combo as though it were
     * enabled - and "Align Houses" is disabled whenever the bi-wheel is off, where losing
     * the greyed-out look would leave a control that reads as available and ignores clicks.
     * The default renderer already handles both the popup rows and the disabled state.
     *
     * @param combo         the dropdown to style
     * @param font          the font to draw it in
     * @param sizeToContent true to set a preferred width from the widest item. Pass false
     *                      for a combo that its layout stretches anyway; a fixed preferred
     *                      size on one of those fights the layout instead of helping it.
     */
    // Wildcard rather than String: the styling is paint and metrics, and cares nothing for
    // what the model holds. Typed to String it turned away the one combo in the app whose
    // items are an enum, which would have meant either a second copy of this or one
    // control styled differently from its neighbours.
    public static void styleCombo(JComboBox<?> combo, Font font, boolean sizeToContent) {
        combo.setOpaque(true);
        combo.setBackground(COMBO_BG);
        combo.setForeground(Color.WHITE);
        combo.setFont(font);

        // setUI last. BasicComboBoxUI.installDefaults() re-reads the colours off the
        // component, so installing the delegate first and colouring afterwards leaves the
        // value area filled from the LAF's white "ComboBox.background" instead - which is
        // how the text became invisible in the first place.
        // The arrow button is the other half of the combo and was never styled - the value
        // area went blue while the drop-arrow stayed a grey 3D LAF button bolted to its
        // right edge. Painted here rather than left to the toolkit.
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(comboBox.isEnabled() ? ACCENT : ACCENT_OFF);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        g2.setColor(comboBox.isEnabled() ? Color.WHITE : TEXT_OFF);
                        g2.fillPolygon(new int[] {cx - 4, cx + 4, cx},
                                       new int[] {cy - 2, cy - 2, cy + 3}, 3);
                        g2.dispose();
                    }
                };
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setContentAreaFilled(false);
                b.setFocusable(false);
                return b;
            }
        });

        // Show every item rather than Swing's default eight.
        //
        // The navigation menu has ten entries and Settings is the ninth, so it opened below
        // the fold behind a thin scrollbar - on a menu whose text was invisible anyway. The
        // screen was reachable in principle and undiscoverable in practice. Capped so that a
        // future long list still scrolls instead of running off the bottom of the display.
        combo.setMaximumRowCount(Math.min(Math.max(combo.getItemCount(), 8), 16));

        if (sizeToContent) {
            // Width from real font metrics rather than character counts: the arrow button
            // overlaps the value area, and padding by spaces still clipped "Transit".
            FontMetrics fm = combo.getFontMetrics(combo.getFont());
            int widest = 0;
            for (int i = 0; i < combo.getItemCount(); i++) {
                // Measured through toString, which is what the renderer draws - so a combo of
                // enums is sized by the text the reader actually sees rather than skipped.
                Object item = combo.getItemAt(i);
                if (item != null) {
                    widest = Math.max(widest, fm.stringWidth(String.valueOf(item)));
                }
            }
            int arrowAndPadding = 40;
            combo.setPreferredSize(new Dimension(widest + arrowAndPadding,
                                                 combo.getPreferredSize().height));
        }
    }

    /** The wheel's control-strip dropdowns: bold 12, sized to their widest item. */
    public static void styleCombo(JComboBox<?> combo) {
        styleCombo(combo, COMBO_FONT, true);
    }

    /**
     * A colour swatch: a small button that IS its colour.
     *
     * <b>Setting the background is not enough, and that is a look-and-feel trap.</b> The app
     * installs the system look-and-feel, and the Windows ButtonUI paints its own gradient and
     * ignores {@code setBackground} entirely - so every colour chip in Settings rendered as a
     * blank white button on the machine that matters, while looking perfectly correct under
     * Metal, which is what an offscreen test harness gets by default. The chip has to paint
     * itself.
     */
    public static void styleSwatch(final JButton b, java.awt.Color colour) {
        b.setBackground(colour);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, JComponent c) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 4, 4);
                // A hairline so a swatch the same colour as the panel behind it is still a
                // control rather than a hole.
                g2.setColor(Theme.EDGE);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 4, 4);
                g2.dispose();
            }
        });
    }

    /** Plain 11 rather than bold 12 - see {@link #styleCompactCombo}. */
    private static final Font COMPACT_COMBO_FONT = Theme.font("Arial", Font.PLAIN, 11);

    /**
     * A control-strip dropdown that has to share its row with six others.
     *
     * The wheel's strip carries Step, Harmonic, Animate, Filter, Align Houses, Pin and House
     * System. At bold 12 with their labels they do not fit a 1024-wide window, and House
     * System - the last one added - was the one off the end. Narrower type buys the room back;
     * <b>{@link WrapLayout} is what makes the row safe at any width</b>, and the two are meant
     * to be used together.
     */
    public static void styleCompactCombo(JComboBox<String> combo) {
        styleCombo(combo, COMPACT_COMBO_FONT, true);
    }

    /**
     * A {@link FlowLayout} that reports the height it will actually occupy.
     *
     * <p><b>This is the reason the House System dropdown was cut off, and nothing about it is
     * visible in the code that placed the control.</b> FlowLayout wraps its children onto as
     * many rows as it needs, but {@code preferredLayoutSize} always answers for a <i>single</i>
     * row. Inside a BoxLayout - which is exactly how the wheel's control strip is built - the
     * parent therefore reserves one row's height, the second row is laid out below the panel's
     * own bounds, and the last controls are simply not drawn. Nothing throws, nothing logs, and
     * widening the window makes it come back, which is what makes it read as a sizing quirk
     * rather than a bug.
     *
     * <p>The fix is to measure the wrap: walk the children against the target's real width,
     * break rows the way FlowLayout will, and sum the row heights. Laying out is left to
     * FlowLayout - it already does that part correctly.
     *
     * <p><b>Use this anywhere a FlowLayout row can outgrow its container.</b> Setting a smaller
     * font on the controls only postpones the clip to a narrower window; this removes it.
     */
    public static class WrapLayout extends FlowLayout {

        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = layoutSize(target, false);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                // Before the first layout the target has no width. Answering with one long
                // row there is right: it becomes the preferred width, and the real wrap is
                // measured on the next pass once a width exists.
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                Insets insets = target.getInsets();
                int horizontal = insets.left + insets.right + getHgap() * 2;
                int maxWidth = targetWidth - horizontal;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) {
                        continue;
                    }
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) {
                        rowWidth += getHgap();
                    }
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontal;
                dim.height += insets.top + insets.bottom + getVgap() * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) {
                dim.height += getVgap();
            }
            dim.height += rowHeight;
        }
    }
}
