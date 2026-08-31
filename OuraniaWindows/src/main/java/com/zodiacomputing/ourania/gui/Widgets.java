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

    public static final Color COMBO_BG = ACCENT;

    private static final Font COMBO_FONT = new Font("Arial", Font.BOLD, 12);

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

    /** Paints a button by role, with a hover state and no focus ring. */
    public static void styleButton(JButton b, Role role) {
        final Color base;
        final Color hover;
        final Color text;
        switch (role) {
            case PRIMARY:
                base = ACCENT; hover = ACCENT_HOVER; text = Color.WHITE; break;
            case READING:
                base = new Color(92, 74, 148); hover = new Color(112, 92, 174);
                text = Color.WHITE; break;
            default:
                base = new Color(34, 37, 46); hover = new Color(50, 55, 68);
                text = new Color(206, 210, 220); break;
        }
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setForeground(text);
        b.setBackground(base);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        for (java.awt.event.MouseListener old : b.getMouseListeners()) {
            if (old instanceof Hover) {
                b.removeMouseListener(old);
            }
        }
        b.addMouseListener(new Hover(b, base, hover));
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
    public static void styleCombo(JComboBox<String> combo, Font font, boolean sizeToContent) {
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
                String item = combo.getItemAt(i);
                if (item != null) {
                    widest = Math.max(widest, fm.stringWidth(item));
                }
            }
            int arrowAndPadding = 40;
            combo.setPreferredSize(new Dimension(widest + arrowAndPadding,
                                                 combo.getPreferredSize().height));
        }
    }

    /** The wheel's control-strip dropdowns: bold 12, sized to their widest item. */
    public static void styleCombo(JComboBox<String> combo) {
        styleCombo(combo, COMBO_FONT, true);
    }
}
