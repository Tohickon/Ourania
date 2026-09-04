package com.zodiacomputing.ourania.gui;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

/**
 * A panel with drawers that slide over its content instead of squeezing it.
 *
 * <b>Why some drawers push and some overlap.</b> A drawer in a BorderLayout region takes its
 * space from its sibling, so opening one resizes the chart - and the wheel is drawn to fit
 * the smaller of its two dimensions, which means every open and close rescales the whole
 * chart and reflows the text on it. That is the right trade for the side rails, where the
 * panel and the wheel are read together and neither should sit under the other. It is the
 * wrong trade for a grid the size of a page and for the transport row, which are looked at
 * and then dismissed: there the chart should still be exactly where it was when the drawer
 * closes.
 *
 * The base fills the whole area and the drawers are laid on top of it at their own preferred
 * thickness, so nothing below them is resized - only hidden while they are open.
 */
public final class OverlayDock extends JLayeredPane {

    private final JComponent base;
    private final Drawer top;
    private final Drawer bottom;

    /**
     * @param base   the content the drawers lie over - it always gets the full area
     * @param top    a {@link Drawer.Side#TOP} drawer, or null
     * @param bottom a {@link Drawer.Side#BOTTOM} drawer, or null
     */
    public OverlayDock(JComponent base, Drawer top, Drawer bottom) {
        this.base = base;
        this.top = top;
        this.bottom = bottom;
        setLayout(null);
        setOpaque(true);
        setBackground(Theme.BG);
        add(base, JLayeredPane.DEFAULT_LAYER);
        if (top != null) {
            add(top, JLayeredPane.PALETTE_LAYER);
        }
        if (bottom != null) {
            add(bottom, JLayeredPane.PALETTE_LAYER);
        }
    }

    /**
     * Base to the full area; each drawer to its own thickness at its edge.
     *
     * <b>Laid out by hand because that is what overlapping requires.</b> Every layout manager
     * in Swing divides the space rather than sharing it - which is precisely the behaviour
     * this class exists to avoid - so the geometry is four lines of arithmetic instead.
     */
    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        // <b>The base stops short of the handles, not of the open drawers.</b> A handle is on
        // screen whether its drawer is open or shut, so anything drawn under one is simply
        // never seen - which is what clipped the top and bottom off the wheel once the chart
        // started receiving the dock's full height. The drawers themselves still overlap when
        // they open; it is only the permanent 26px strip that the base gives up.
        int topInset = top == null ? 0 : Drawer.HANDLE_WIDTH;
        int bottomInset = bottom == null ? 0 : Drawer.HANDLE_WIDTH;
        int baseH = Math.max(0, h - topInset - bottomInset);
        base.setBounds(0, topInset, w, baseH);
        if (top != null) {
            int t = top.getPreferredSize().height;
            top.setBounds(0, 0, w, Math.min(t, h));
        }
        if (bottom != null) {
            int b = bottom.getPreferredSize().height;
            bottom.setBounds(0, Math.max(0, h - b), w, Math.min(b, h));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return base.getPreferredSize();
    }
}
