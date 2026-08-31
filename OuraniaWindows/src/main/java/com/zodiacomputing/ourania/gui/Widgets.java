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

    public static final Color COMBO_BG = new Color(60, 120, 200);

    private static final Font COMBO_FONT = new Font("Arial", Font.BOLD, 12);

    private Widgets() { }

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
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI());

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
