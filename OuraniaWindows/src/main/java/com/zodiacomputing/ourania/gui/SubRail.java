package com.zodiacomputing.ourania.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A horizontal strip of tabs over a card stack: the second level inside one rail page.
 *
 * <p><b>Why this exists.</b> The left rail carried twelve tabs in one column - four charts,
 * three views of them and five generated documents - and David's reading of it was "a bit
 * bloated and confusing". An earlier attempt grouped those twelve with rules between them
 * ({@code caa68f9c}), which was the right fix at the wrong granularity: twelve items in three
 * groups is still twelve items, and the rail still asks the reader to choose between all of
 * them at once. Three doors with a handful of rooms behind each is a different shape of
 * question.
 *
 * <p><b>Horizontal, because the rail is vertical.</b> A second column of vertical tabs beside
 * the first would read as one rail of twice the width, and {@code DrawerRail}'s tab text is
 * rotated - two rotated columns is not a hierarchy, it is a wall. Running this strip across
 * the top of the page says "these are inside the tab you just opened" by its direction alone.
 *
 * <p><b>It owns no content and no refresh.</b> A card is handed in whole and the card's own
 * owner keeps filling it; this only decides which one is visible. {@link #setOnSelect} reports
 * the name so {@code OuraniaWindow} can go on doing what it already did when a rail tab opened
 * - a card swap is not a door, and the thing behind the door still has to be made.
 */
final class SubRail extends JPanel {

    private static final Color STRIP_BG = Theme.SURFACE;
    private static final Color TAB_ON = Theme.SURFACE_3;
    private static final Color TAB_HOVER = Theme.SURFACE_2;

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(this.cards);
    private final JPanel strip = new JPanel(new WrapRow());
    private final Map<String, Tab> tabs = new LinkedHashMap<>();
    private final Map<String, JComponent> bodies = new LinkedHashMap<>();
    private final List<String> order = new ArrayList<>();
    private String selected;
    private Consumer<String> onSelect;

    SubRail() {
        super(new BorderLayout());
        this.setBackground(Theme.BG);
        this.strip.setBackground(STRIP_BG);
        this.body.setBackground(Theme.BG);
        this.add(this.strip, BorderLayout.NORTH);
        this.add(this.body, BorderLayout.CENTER);
    }

    /** Add a page. The first one added is the one shown. */
    void addPage(String name, JComponent page) {
        Tab tab = new Tab(name);
        this.tabs.put(name, tab);
        this.order.add(name);
        this.strip.add(tab);
        this.body.add(page, name);
        this.bodies.put(name, page);
        if (this.selected == null) {
            this.selected = name;
        }
    }

    /**
     * Show a page WITHOUT telling the listener - the programmatic path.
     *
     * <b>These are DrawerRail's semantics, deliberately, and getting them wrong cost a stack
     * overflow.</b> There, {@code reveal} shows a page silently and only {@code select} - a
     * click - fires the listener. The first version of this class fired on reveal too, on the
     * reasoning that a reading must regenerate whenever it is opened. It does; but the window
     * routes {@code revealPage} through the rail AND the strip, the rail's own listener asks
     * the strip to re-open its page, and the strip's listener reaches code that calls
     * revealPage again. Three reasonable steps and a cycle.
     *
     * A listener that fires on every programmatic show is also a listener nobody can reason
     * about: the caller asking for a page to be visible is not the same event as a reader
     * choosing it.
     */
    void reveal(String name) {
        if (!this.tabs.containsKey(name) || !this.tabs.get(name).enabled) {
            return;
        }
        this.selected = name;
        this.cards.show(this.body, name);
        this.strip.repaint();
    }

    /** A reader chose this tab: show it, and tell the listener. */
    void select(String name) {
        if (!this.tabs.containsKey(name) || !this.tabs.get(name).enabled) {
            return;
        }
        this.reveal(name);
        if (this.onSelect != null) {
            this.onSelect.accept(name);
        }
    }

    /**
     * Tell the listener about the page already showing.
     *
     * <b>For re-entering a strip from the rail tab above it.</b> Opening Chronometry puts
     * whichever page was last chosen back in front, and if that is Predict it has to be
     * rebuilt - a reading is a snapshot of a moment and the transport buttons keep moving the
     * moment. The page does not change, so this is not a reveal; what changes is that the
     * reader is looking at it again.
     */
    void refire() {
        if (this.selected != null && this.onSelect != null
                && this.isPageEnabled(this.selected)) {
            this.onSelect.accept(this.selected);
        }
    }

    /** Show a page without telling the listener - for setting the opening state. */
    void prepare(String name) {
        if (!this.tabs.containsKey(name)) {
            return;
        }
        this.selected = name;
        this.cards.show(this.body, name);
        this.strip.repaint();
    }

    void setOnSelect(Consumer<String> listener) {
        this.onSelect = listener;
    }

    /**
     * Grey a tab out, as the rail does for Selection before anything is clicked.
     *
     * <b>A disabled tab that is currently showing steps aside.</b> Greying the page in front of
     * the reader and leaving it there would show a live-looking page that nothing can refresh -
     * which is the state Selection was in before it had a door at all.
     */
    void setPageEnabled(String name, boolean on) {
        Tab tab = this.tabs.get(name);
        if (tab == null || tab.enabled == on) {
            return;
        }
        tab.enabled = on;
        tab.repaint();
        if (!on && name.equals(this.selected)) {
            for (String other : this.order) {
                if (this.tabs.get(other).enabled) {
                    // Silently: the reader did not choose this page, the app moved them off a
                    // page that stopped being available. Firing here would regenerate a
                    // reading nobody asked for, on every chart change that greys a tab.
                    this.reveal(other);
                    return;
                }
            }
        }
    }

    boolean isPageEnabled(String name) {
        Tab tab = this.tabs.get(name);
        return tab != null && tab.enabled;
    }

    /** True when every page here is greyed out, which is what greys the rail tab above. */
    boolean allDisabled() {
        for (Tab tab : this.tabs.values()) {
            if (tab.enabled) {
                return false;
            }
        }
        return !this.tabs.isEmpty();
    }

    /**
     * Rename a tab without renaming its page.
     *
     * <b>The shown text and the card key are different things, and conflating them is how the
     * rail nearly lost its pages.</b> The chart tab is renamed as the wheel changes - "Chart A"
     * in a synastry, the ring's own word otherwise - and if the rename reached the key, every
     * caller holding CHART_PAGE would be addressing a card that no longer exists. DrawerRail
     * learned this first; see its own setPageLabel.
     */
    void setPageLabel(String name, String shown) {
        Tab tab = this.tabs.get(name);
        if (tab == null || tab.shown.equals(shown)) {
            return;
        }
        tab.shown = shown;
        tab.resize();
        this.strip.revalidate();
        this.strip.repaint();
    }

    String pageLabel(String name) {
        Tab tab = this.tabs.get(name);
        return tab == null ? null : tab.shown;
    }

    String selected() {
        return this.selected;
    }

    /**
     * What was added under a name.
     *
     * A CardLayout keeps the name as a layout constraint rather than on the component, so the
     * container cannot be asked which card is which afterwards. Recorded on the way in, as
     * DrawerRail does for the same reason.
     */
    JComponent page(String name) {
        return this.bodies.get(name);
    }

    /** The card stack, for a suite counting what is visible in it. */
    JPanel cards() {
        return this.body;
    }

    /** The names in the order they were added, for the suites to pin. */
    List<String> pages() {
        return java.util.Collections.unmodifiableList(this.order);
    }

    /**
     * A row of tabs that wraps, and <b>reports the height it actually wrapped to</b>.
     *
     * FlowLayout does the wrapping and lies about the height: asked for a preferred size it
     * answers one row's worth regardless of how many rows it will need, and inside
     * BorderLayout.NORTH - which takes exactly the preferred height - that silently clips
     * every row but the first. Blueprint has eight pages in a 340px drawer, so four of them
     * were simply not on screen: present in the strip, laid out, painted into a region two
     * dozen pixels tall. A navigation bar that hides half the destinations is worse than the
     * twelve-tab column it replaced, because at least that column could be read.
     */
    private static final class WrapRow implements LayoutManager {
        private static final int GAP = 2;
        private static final int VGAP = 3;

        @Override
        public void addLayoutComponent(String name, java.awt.Component comp) { }

        @Override
        public void removeLayoutComponent(java.awt.Component comp) { }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return this.measure(parent, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return this.measure(parent, false);
        }

        @Override
        public void layoutContainer(Container parent) {
            this.measure(parent, true);
        }

        /**
         * One pass that both measures and places, so the two cannot disagree.
         *
         * <b>The width to wrap against is the parent's, and it may be zero.</b> Before the
         * first layout a container has no width, and wrapping against zero puts every tab on
         * its own row - which is a tall strip that then never shrinks, because the layout it
         * reported is the height it was given. Falling back to the preferred width keeps the
         * first measurement honest.
         */
        private Dimension measure(Container parent, boolean place) {
            Insets in = parent.getInsets();
            int avail = parent.getWidth() - in.left - in.right;
            if (avail <= 0) {
                avail = Integer.MAX_VALUE;
            }
            int x = in.left;
            int y = in.top;
            int rowHeight = 0;
            int widest = 0;
            for (java.awt.Component c : parent.getComponents()) {
                Dimension d = c.getPreferredSize();
                if (x > in.left && x - in.left + d.width > avail) {
                    x = in.left;
                    y += rowHeight + VGAP;
                    rowHeight = 0;
                }
                if (place) {
                    c.setBounds(x, y, d.width, d.height);
                }
                x += d.width + GAP;
                widest = Math.max(widest, x - in.left);
                rowHeight = Math.max(rowHeight, d.height);
            }
            return new Dimension(Math.min(widest, avail == Integer.MAX_VALUE ? widest : avail),
                y + rowHeight + VGAP + in.bottom);
        }
    }

    /** One tab in the strip. Painted rather than a JButton, to match DrawerRail's tabs. */
    private final class Tab extends JComponent {
        /** The card key. Never changes - callers hold it. */
        private final String name;
        /** What the reader sees. Renamed as the chart changes. */
        private String shown;
        private boolean enabled = true;
        private boolean hover;

        Tab(String name) {
            this.name = name;
            this.shown = name;
            this.setFont(Theme.BODY);
            this.resize();
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    Tab.this.hover = true;
                    Tab.this.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Tab.this.hover = false;
                    Tab.this.repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (Tab.this.enabled) {
                        SubRail.this.select(Tab.this.name);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            boolean on = this.name.equals(SubRail.this.selected);
            g2.setColor(on ? TAB_ON : (this.hover && this.enabled ? TAB_HOVER : STRIP_BG));
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 7, 7);
            if (on) {
                // The accent runs along the bottom, where the page is, rather than round the
                // whole tab - it has to read as the tab joining its page, not as a button.
                g2.setColor(Theme.ACCENT);
                g2.fillRect(3, this.getHeight() - 2, this.getWidth() - 6, 2);
            }
            g2.setColor(!this.enabled ? Theme.TEXT_DIM : (on ? Color.WHITE : Theme.TEXT));
            g2.setFont(Theme.BODY);
            int tw = g2.getFontMetrics().stringWidth(this.shown);
            g2.drawString(this.shown, (this.getWidth() - tw) / 2,
                this.getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
            g2.dispose();
        }

        /** Width follows the shown text, so a renamed tab does not clip or leave a gap. */
        void resize() {
            int w = this.getFontMetrics(Theme.BODY).stringWidth(this.shown) + 22;
            this.setPreferredSize(new Dimension(w, 24));
        }
    }
}
