package com.zodiacomputing.ourania.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Several drawers sharing one edge: a column of tabs, and one panel that slides out behind
 * whichever tab is chosen.
 *
 * <b>Why this is not just two {@link Drawer}s stacked.</b> A Drawer owns its handle and its
 * body together, so stacking two down the left edge gives each one half the window's height
 * and lets them open to different widths - a ragged edge with two panels of different sizes
 * jutting into the chart. What the layout wants is one body region and a tab per thing that
 * can appear in it, which is this class.
 *
 * <b>Selecting the open tab closes the rail.</b> The tab is the whole control: one click to
 * show a panel, one more to put it away, with no separate close affordance to hunt for.
 * Choosing a different tab swaps the content without closing, because that is a change of
 * subject rather than a decision to stop looking.
 *
 * The rail pushes rather than covers - it is a sibling of the chart in a BorderLayout, so
 * opening it takes width from the wheel and closing gives it back. That is the behaviour the
 * right-hand Menu drawer already had, and the one this matches deliberately.
 */
public final class DrawerRail extends JPanel {

    /** Tab thickness, matching {@link Drawer#HANDLE_WIDTH} so both edges read as one idea. */
    public static final int RAIL_WIDTH = Drawer.HANDLE_WIDTH;

    private static final Color RAIL_BG = Theme.SURFACE;
    private static final Color TAB_HOVER = Theme.SURFACE_3;
    private static final Color TAB_ON = Theme.SURFACE_3;
    /** Dim enough to read as unavailable, light enough to still read as a word. */
    private static final Color TAB_DISABLED = Theme.TEXT_DIM;
    private static final int FRAME_MS = 12;
    private static final int STEP_PX = 38;

    private final Drawer.Side side;
    private final int openWidth;
    private final JPanel rail;
    private final JPanel body;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages;
    private final List<Tab> tabs = new ArrayList<>();
    private final java.util.Map<String, Integer> pageWidths = new java.util.HashMap<>();
    private final Timer animator;

    private String selected;
    private int currentWidth;
    private int targetWidth;
    /**
     * Told when a page is opened, so its content can be produced on demand.
     *
     * <b>A tab that only swaps cards shows an empty card.</b> The readings used to be buttons
     * that ran something; as tabs they revealed a pane nothing had ever filled, so five of
     * them opened onto blank panels. A rail whose pages are generated rather than stored has
     * to say when one is asked for.
     */
    private java.util.function.Consumer<String> onSelect;
    private final java.util.Set<String> disabled = new java.util.HashSet<>();

    public DrawerRail(Drawer.Side side, int openWidth) {
        this.side = side;
        this.openWidth = openWidth;
        setLayout(new BorderLayout());
        setBackground(RAIL_BG);
        setOpaque(true);

        rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(RAIL_BG);

        pages = new JPanel(cards);
        pages.setBackground(Theme.BG);
        body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createMatteBorder(
            0, side == Drawer.Side.LEFT ? 0 : 1, 0, side == Drawer.Side.LEFT ? 1 : 0,
            Theme.EDGE));
        body.add(pages, BorderLayout.CENTER);

        add(rail, side == Drawer.Side.LEFT ? BorderLayout.WEST : BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        animator = new Timer(FRAME_MS, e -> step());
        applyWidth(0);
    }

    /** Adds one tab and the panel it reveals, opening to the rail's own width. */
    public void addPage(String label, JComponent content) {
        addPage(label, content, openWidth);
    }

    /**
     * Adds a tab whose panel opens to a width of its own.
     *
     * <b>Because one width does not fit every panel.</b> The menu is a column of short rows
     * and wants to be narrow - given the grid's width it opens with half a panel of empty
     * space beside it, at the chart's expense. The aspect grid is a table twenty-nine columns
     * wide and wants every pixel there is. A single rail width has to be wrong for one of
     * them, so the page carries its own.
     */
    public void addPage(String label, JComponent content, int pageWidth) {
        pages.add(content, label);
        pageWidths.put(label, pageWidth);
        Tab tab = new Tab(label);
        tabs.add(tab);
        rail.add(tab);
        rail.revalidate();
    }

    /** How wide a page opens: its own width, or the rail's when it did not ask for one. */
    private int widthFor(String label) {
        Integer w = pageWidths.get(label);
        return w == null ? openWidth : w;
    }

    /**
     * Shows a page, or closes the rail when that page is already showing.
     *
     * Called by a tab click, and by the wheel when a click on a body needs its panel visible.
     */
    public void select(String label) {
        if (disabled.contains(label)) {
            return;
        }
        if (label.equals(selected) && isOpen()) {
            setOpen(false);
            return;
        }
        selected = label;
        cards.show(pages, label);
        setOpen(true);
        repaintTabs();
        if (onSelect != null) {
            onSelect.accept(label);
        }
    }

    /** Called with a page name each time that page is opened by a click. */
    public void setOnSelect(java.util.function.Consumer<String> listener) {
        this.onSelect = listener;
    }

    /**
     * Greys a tab out and stops it opening.
     *
     * <b>For a page that has nothing to say yet.</b> Selection before anything is clicked,
     * and Transits on a chart drawn without them, are both panels that would open onto an
     * empty pane - which reads as a broken tab rather than as an empty one. A tab that is
     * visibly unavailable says the same thing honestly and costs no click to find out.
     */
    public void setPageEnabled(String label, boolean enabled) {
        boolean changed = enabled ? disabled.remove(label) : disabled.add(label);
        if (!changed) {
            return;
        }
        // A page that goes unavailable while it is the one showing takes the rail with it,
        // rather than leaving a disabled tab lit and a stale panel open beside it.
        if (!enabled && label.equals(selected) && isOpen()) {
            setOpen(false);
        }
        repaintTabs();
    }

    /** Whether a page can currently be opened. */
    public boolean isPageEnabled(String label) {
        return !disabled.contains(label);
    }

    /** Shows a page without the toggle, for a caller that needs it visible either way. */
    public void reveal(String label) {
        selected = label;
        cards.show(pages, label);
        setOpen(true);
        repaintTabs();
    }

    public boolean isOpen() {
        return targetWidth > 0;
    }

    /** The page currently showing, or null when the rail has never been opened. */
    public String selected() {
        return selected;
    }

    /** The tab labels this rail carries, in the order they appear. */
    public List<String> pageNames() {
        List<String> out = new ArrayList<>();
        for (Tab t : tabs) {
            out.add(t.label);
        }
        return out;
    }

    public void setOpen(boolean shouldOpen) {
        // Widened or narrowed to suit the page being shown, so switching tabs animates the
        // panel to its own size rather than leaving it at the last one's.
        targetWidth = shouldOpen ? widthFor(selected) : 0;
        repaintTabs();
        if (!animator.isRunning()) {
            animator.start();
        }
    }

    /** Opens without animating, for the state the app starts in. */
    public void revealImmediately(String label) {
        selected = label;
        cards.show(pages, label);
        targetWidth = widthFor(label);
        applyWidth(targetWidth);
        repaintTabs();
    }

    private void repaintTabs() {
        for (Tab t : tabs) {
            t.repaint();
        }
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
        setPreferredSize(new Dimension(RAIL_WIDTH + width, 0));
        setMinimumSize(new Dimension(RAIL_WIDTH, 0));
        setMaximumSize(new Dimension(RAIL_WIDTH + width, Integer.MAX_VALUE));
        revalidate();
        Component parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    /** One tab: the page name turned on its side, sized to the words it holds. */
    private final class Tab extends JComponent {

        private final String label;
        private boolean hover;

        Tab(String label) {
            this.label = label;
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText(label + " - click to open or close this panel");
            setAlignmentY(0.0f);
            setAlignmentX(0.5f);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    select(Tab.this.label);
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

        /**
         * As tall as its own name needs, not a share of the window.
         *
         * Splitting the rail evenly would put the second tab at the vertical middle however
         * short its label, which reads as an empty gap rather than as two tabs.
         */
        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Theme.HEADING);
            return new Dimension(RAIL_WIDTH, fm.stringWidth(label) + 46);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean off = disabled.contains(label);
            boolean on = !off && label.equals(selected) && isOpen();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(on ? TAB_ON : (hover && !off ? TAB_HOVER : RAIL_BG));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(on ? Theme.ACCENT : Theme.EDGE);
            int thick = on ? 2 : 1;
            int edgeX = side == Drawer.Side.LEFT ? getWidth() - thick : 0;
            g2.fillRect(edgeX, 0, thick, getHeight());

            g2.setFont(Theme.HEADING);
            g2.setColor(off ? TAB_DISABLED : (on ? Color.WHITE : Theme.TEXT));
            FontMetrics fm = g2.getFontMetrics();
            // ASCII only: this font draws the triangle glyphs as empty boxes.
            String arrow = side == Drawer.Side.LEFT ? (on ? "<" : ">") : (on ? ">" : "<");
            String text = arrow + "  " + label;
            g2.rotate(-Math.PI / 2.0);
            int textX = -(getHeight() + fm.stringWidth(text)) / 2;
            int textY = (getWidth() + fm.getAscent()) / 2 - 2;
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }
}
