package com.zodiacomputing.ourania.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A drawer: the space that slides in and out from the edge of the window.
 *
 * <p><b>The drawer is the space; an {@link Accordion} is how content is organised inside it.</b>
 * Keeping those separate is the whole point of this class existing - the first attempt made one
 * thing of both and it read as neither, a rail of tabs too menu-like to be a drawer and too flat
 * to organise anything.
 *
 * <h2>What it is not</h2>
 *
 * <b>Not a {@link javax.swing.JPopupMenu}.</b> The version before this used popups: they grouped
 * the controls, but a popup floats over the chart, shuts on the next click, and can only hold
 * rows of buttons. A drawer stays open while it is being read and holds whole panels.
 *
 * <h2>It takes space rather than overlaying</h2>
 *
 * The drawer is a BorderLayout region, so opening it narrows the chart rather than covering it.
 * Overlaying would mean the frame's layered pane and hand-positioning on every resize; this way
 * the wheel keeps using its own resize path, which matters because the wheel is decompiler
 * output whose ring geometry is derived at four separate call sites.
 *
 * <h2>The handle is always there</h2>
 *
 * A shut drawer still shows its handle down the outer edge, so it costs the chart
 * {@link #HANDLE_WIDTH} pixels and nothing more. A drawer with no visible handle is a feature
 * with no door - this project has shipped one of those already, in the browsable index whose
 * only two entry points were both inside a panel that had to be open first.
 */
public final class Drawer extends JPanel {

    /** Which edge the drawer lives on. */
    public enum Side { LEFT, RIGHT }

    /** Handle thickness: enough for a rotated label and a comfortable click target. */
    public static final int HANDLE_WIDTH = 26;

    private static final Color HANDLE_BG = Theme.SURFACE;
    private static final Color HANDLE_HOVER = Theme.SURFACE_3;
    /**
     * The open state is a quiet surface with an accent EDGE, not an accent fill.
     *
     * Filled, the two handles became bright vertical bars down both sides of the window -
     * the loudest thing on screen, competing with the chart they exist to reveal. A handle
     * should say which side it belongs to, not win the page.
     */
    private static final Color HANDLE_ON = Theme.SURFACE_3;
    private static final Color EDGE = Theme.EDGE;
    private static final Color TEXT = Theme.TEXT;

    private static final int FRAME_MS = 12;
    private static final int STEP_PX = 38;

    private final Side side;
    private final int openWidth;
    private final Handle handle;
    private final JPanel body;
    private final Timer animator;

    private boolean open;
    private int currentWidth;
    private int targetWidth;

    public Drawer(Side side, String handleLabel, JComponent content, int openWidth) {
        this.side = side;
        this.openWidth = openWidth;
        setLayout(new BorderLayout());
        setBackground(HANDLE_BG);
        setOpaque(true);

        handle = new Handle(handleLabel);
        body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createMatteBorder(
            0, side == Side.LEFT ? 0 : 1, 0, side == Side.LEFT ? 1 : 0, EDGE));
        body.add(content, BorderLayout.CENTER);

        add(handle, side == Side.LEFT ? BorderLayout.WEST : BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        animator = new Timer(FRAME_MS, e -> step());
        applyWidth(0);
    }

    public boolean isOpen() {
        return open;
    }

    public void toggle() {
        setOpen(!open);
    }

    public void setOpen(boolean shouldOpen) {
        this.open = shouldOpen;
        targetWidth = shouldOpen ? openWidth : 0;
        handle.repaint();
        if (!animator.isRunning()) {
            animator.start();
        }
    }

    /** Opens the drawer without animating, for the state the app starts in. */
    public void openImmediately() {
        this.open = true;
        this.targetWidth = openWidth;
        applyWidth(openWidth);
        handle.repaint();
    }

    private void step() {
        if (currentWidth == targetWidth) {
            animator.stop();
            return;
        }
        int delta = targetWidth - currentWidth;
        int move = Math.abs(delta) <= STEP_PX ? delta : (delta > 0 ? STEP_PX : -STEP_PX);
        applyWidth(currentWidth + move);
    }

    private void applyWidth(int width) {
        currentWidth = width;
        body.setVisible(width > 0);
        Dimension d = new Dimension(HANDLE_WIDTH + width, 0);
        setPreferredSize(d);
        setMinimumSize(new Dimension(HANDLE_WIDTH, 0));
        setMaximumSize(new Dimension(HANDLE_WIDTH + width, Integer.MAX_VALUE));
        revalidate();
        Component parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    /**
     * The handle: the drawer's name turned on its side down the edge.
     *
     * Rotated rather than abbreviated, because the handle is 26px wide and the name is a word.
     * Horizontally it would have to be shortened to a letter or two, which is how a control
     * ends up needing a tooltip to say what it is.
     */
    private final class Handle extends JComponent {

        private final String label;
        private boolean hover;

        Handle(String label) {
            this.label = label;
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText(label + " - click to open or close this panel");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    toggle();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(HANDLE_WIDTH, 0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(open ? HANDLE_ON : (hover ? HANDLE_HOVER : HANDLE_BG));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(open ? Theme.ACCENT : EDGE);
            int edgeX = side == Side.LEFT ? getWidth() - 2 : 0;
            g2.fillRect(edgeX, 0, open ? 2 : 1, getHeight());

            Font f = Theme.HEADING;
            g2.setFont(f);
            g2.setColor(open ? Color.WHITE : TEXT);
            FontMetrics fm = g2.getFontMetrics();
            // ASCII arrows: this font renders the triangle glyphs as empty boxes, which is
            // why the wheel's drawer labels use "..." and why "Swap Natal / Transit" still
            // shows a box where its arrow should be.
            String openArrow = side == Side.LEFT ? "<" : ">";
            String shutArrow = side == Side.LEFT ? ">" : "<";
            String text = (open ? openArrow : shutArrow) + "  " + label;
            // Bottom-to-top, the convention for a vertical tab on either edge.
            g2.rotate(-Math.PI / 2.0);
            int textX = -(getHeight() + fm.stringWidth(text)) / 2;
            int textY = (getWidth() + fm.getAscent()) / 2 - 2;
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }
}
