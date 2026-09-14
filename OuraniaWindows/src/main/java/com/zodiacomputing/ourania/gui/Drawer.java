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

    /**
     * Which edge the drawer lives on.
     *
     * <b>LEFT and RIGHT open along the width; TOP and BOTTOM along the height.</b> Everything
     * below that reads "width" predates the vertical pair and now means "extent along this
     * drawer's own axis" - renaming it throughout would have touched every line in the class
     * for no behavioural gain, so the axis is asked for explicitly where it matters instead.
     */
    public enum Side { LEFT, RIGHT, TOP, BOTTOM }

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
    /** Handle text, which the bottom drawer replaces as the clock moves. */
    private String label;

    public Drawer(Side side, String handleLabel, JComponent content, int openWidth) {
        this.side = side;
        this.openWidth = openWidth;
        this.label = handleLabel;
        setLayout(new BorderLayout());
        setBackground(HANDLE_BG);
        setOpaque(true);

        handle = new Handle();
        body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        // A hairline on the side the content faces, so an open drawer reads as a panel
        // against the chart rather than as the chart having changed colour.
        body.setBorder(BorderFactory.createMatteBorder(
            side == Side.BOTTOM ? 1 : 0,
            side == Side.RIGHT ? 1 : 0,
            side == Side.TOP ? 1 : 0,
            side == Side.LEFT ? 1 : 0, EDGE));
        body.add(content, BorderLayout.CENTER);

        add(handle, handlePosition());
        add(body, BorderLayout.CENTER);

        animator = new Timer(FRAME_MS, e -> step());
        applyWidth(0);
    }

    /** Which edge this drawer lives on, for a check that needs to pin the arrangement. */
    Side side() {
        return side;
    }

    /** Where this drawer's handle sits: always on the edge the drawer opens away from. */
    private String handlePosition() {
        switch (side) {
            case LEFT:   return BorderLayout.WEST;
            case RIGHT:  return BorderLayout.EAST;
            case TOP:    return BorderLayout.NORTH;
            default:     return BorderLayout.SOUTH;
        }
    }

    /** True for a drawer that opens along the window's height rather than its width. */
    private boolean vertical() {
        return side == Side.TOP || side == Side.BOTTOM;
    }

    /**
     * Replaces the handle's text.
     *
     * <b>For the bottom drawer, whose handle IS the clock.</b> The date, time and place have
     * to stay readable while the transport controls are put away, and a handle that is
     * already spanning the window is the one piece of chrome with room for them.
     */
    public void setLabel(String text) {
        this.label = text == null ? "" : text;
        handle.setToolTipText(this.label + " - click to open or close this panel");
        handle.repaint();
    }

    /** The handle's text, for checks. */
    String label() {
        return label;
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
        if (vertical()) {
            setPreferredSize(new Dimension(0, HANDLE_WIDTH + width));
            setMinimumSize(new Dimension(0, HANDLE_WIDTH));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, HANDLE_WIDTH + width));
        } else {
            setPreferredSize(new Dimension(HANDLE_WIDTH + width, 0));
            setMinimumSize(new Dimension(HANDLE_WIDTH, 0));
            setMaximumSize(new Dimension(HANDLE_WIDTH + width, Integer.MAX_VALUE));
        }
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

        private boolean hover;

        Handle() {
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
            return vertical()
                ? new Dimension(0, HANDLE_WIDTH)
                : new Dimension(HANDLE_WIDTH, 0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(open ? HANDLE_ON : (hover ? HANDLE_HOVER : HANDLE_BG));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(open ? Theme.ACCENT : EDGE);
            int thick = open ? 2 : 1;
            switch (side) {
                case LEFT:   g2.fillRect(getWidth() - thick, 0, thick, getHeight()); break;
                case RIGHT:  g2.fillRect(0, 0, thick, getHeight()); break;
                case TOP:    g2.fillRect(0, getHeight() - thick, getWidth(), thick); break;
                default:     g2.fillRect(0, 0, getWidth(), thick); break;
            }

            g2.setFont(Theme.HEADING);
            g2.setColor(open ? Color.WHITE : TEXT);
            FontMetrics fm = g2.getFontMetrics();
            // ASCII arrows: this font renders the triangle glyphs as empty boxes, which is
            // why the wheel's drawer labels use "..." and why "Swap Natal / Transit" still
            // shows a box where its arrow should be.
            String arrow;
            switch (side) {
                case LEFT:   arrow = open ? "<" : ">"; break;
                case RIGHT:  arrow = open ? ">" : "<"; break;
                case TOP:    arrow = open ? "^" : "v"; break;
                default:     arrow = open ? "v" : "^"; break;
            }
            String text = arrow + "  " + label;
            if (vertical()) {
                // <b>Horizontal, not rotated.</b> A top or bottom handle is as wide as the
                // window and as thin as the text is tall, so the rotation that makes a side
                // tab readable would stand the label on its end in a 26px-high strip.
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(text, Math.max(8, textX), textY);
            } else {
                // Bottom-to-top, the convention for a vertical tab on either edge.
                g2.rotate(-Math.PI / 2.0);
                int textX = -(getHeight() + fm.stringWidth(text)) / 2;
                int textY = (getWidth() + fm.getAscent()) / 2 - 2;
                g2.drawString(text, textX, textY);
            }
            g2.dispose();
        }
    }
}
