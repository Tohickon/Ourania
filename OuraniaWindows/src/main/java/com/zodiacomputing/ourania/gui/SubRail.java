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

    /**
     * Group label to its pages, in the order the group lists them; null for a flat strip.
     *
     * <b>Added 2026-10-24 for the reading framework spec: four fixed tabs, whatever the chart.</b>
     * The pages stay what they were - same keys, same cards, same shown and enabled flags - and
     * the strip draws two rows instead of one: the groups on top, and below them the pages of
     * the group in front, when it has more than one to show. Nothing that addresses a page by
     * its key can tell the difference, which is why the suites that pin pages did not change.
     */
    private Map<String, List<String>> groups;
    private final Map<String, GroupTab> groupTabs = new LinkedHashMap<>();
    private final JPanel groupRow = new JPanel(new WrapRow());
    private final JPanel pageRow = new JPanel(new WrapRow());
    /** The page last in front in each group, so coming back to a group finds the same view. */
    private final Map<String, String> lastInGroup = new java.util.HashMap<>();

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
        this.addPageBefore(name, page, null);
    }

    /**
     * Add a page in front of another one, or at the end when {@code before} is not here.
     *
     * <b>The strip's order is the reading order, and the pages are not all built at once.</b>
     * The pair's pages exist before the wheel does, because the wheel's constructor already
     * writes to them; the readings that come first on the same strip are built after it. So
     * order cannot be the order of construction (2026-10-20, the Interpretation strip).
     */
    void addPageBefore(String name, JComponent page, String before) {
        Tab tab = new Tab(name);
        this.tabs.put(name, tab);
        int at = before == null ? -1 : this.order.indexOf(before);
        if (at < 0) {
            at = this.order.size();
        }
        this.order.add(at, name);
        if (this.groups == null) {
            this.strip.add(tab, at);
        }
        // The card at the same index as its tab: a suite reads the stack in strip order.
        this.body.add(page, name, at);
        this.bodies.put(name, page);
        this.refreshStrip();
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
        if (!this.tabs.containsKey(name) || !this.tabs.get(name).enabled
                || !this.tabs.get(name).isVisible()) {
            return;
        }
        this.selected = name;
        this.cards.show(this.body, name);
        this.rememberGroup(name);
        this.refreshStrip();
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
        this.rememberGroup(name);
        this.refreshStrip();
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
        this.refreshStrip();
        if (!on && name.equals(this.selected)) {
            for (String other : this.fallbackOrder(name)) {
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

    /**
     * Show or hide a tab: for a page the chart in front has no use for (2026-10-20). Your year
     * has no birthday to read on the sky; a pair's pages have no pair on Chart A. A hidden tab
     * that is showing steps aside to the first page still shown, as a greyed one does.
     */
    void setPageShown(String name, boolean on) {
        Tab tab = this.tabs.get(name);
        if (tab == null || tab.isVisible() == on) {
            return;
        }
        tab.setVisible(on);
        this.refreshStrip();
        this.strip.revalidate();
        this.strip.repaint();
        if (!on && name.equals(this.selected)) {
            for (String other : this.fallbackOrder(name)) {
                Tab t = this.tabs.get(other);
                if (t.enabled && t.isVisible()) {
                    this.reveal(other);
                    return;
                }
            }
        }
    }

    boolean isPageShown(String name) {
        Tab tab = this.tabs.get(name);
        return tab != null && tab.isVisible();
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
        this.refreshStrip();
        this.strip.revalidate();
        this.strip.repaint();
    }

    String pageLabel(String name) {
        Tab tab = this.tabs.get(name);
        return tab == null ? null : tab.shown;
    }

    /**
     * Group the pages into the strip's top row. Every page must be in exactly one group: a
     * page in none would be on the card stack and nowhere on the strip, which is a page nobody
     * can open, and failing here at start-up is better than finding it by eye.
     */
    void setGroups(Map<String, List<String>> grouped) {
        List<String> seen = new ArrayList<>();
        for (List<String> members : grouped.values()) {
            for (String page : members) {
                if (!this.tabs.containsKey(page) || seen.contains(page)) {
                    throw new IllegalArgumentException("page not added, or in two groups: " + page);
                }
                seen.add(page);
            }
        }
        List<String> orphans = new ArrayList<>(this.order);
        orphans.removeAll(seen);
        if (!orphans.isEmpty()) {
            throw new IllegalArgumentException("pages in no group: " + orphans);
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        grouped.forEach((label, members) -> copy.put(label, List.copyOf(members)));
        this.groups = copy;
        this.strip.removeAll();
        this.strip.setLayout(new BorderLayout());
        this.groupRow.setBackground(STRIP_BG);
        this.pageRow.setBackground(STRIP_BG);
        this.strip.add(this.groupRow, BorderLayout.NORTH);
        this.strip.add(this.pageRow, BorderLayout.CENTER);
        for (String label : copy.keySet()) {
            this.groupTabs.put(label, new GroupTab(label));
        }
        this.refreshStrip();
    }

    /** The group a page belongs to, or null on a flat strip. */
    String groupOf(String page) {
        if (this.groups == null || page == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> g : this.groups.entrySet()) {
            if (g.getValue().contains(page)) {
                return g.getKey();
            }
        }
        return null;
    }

    /** The group in front, or null. */
    String selectedGroup() {
        return this.groupOf(this.selected);
    }

    /** The groups the top row draws now, in order: a group with no page to show is left out. */
    List<String> groupLabels() {
        List<String> out = new ArrayList<>();
        if (this.groups != null) {
            for (Map.Entry<String, List<String>> g : this.groups.entrySet()) {
                if (!this.shownIn(g.getValue()).isEmpty()) {
                    out.add(g.getKey());
                }
            }
        }
        return out;
    }

    /** The pages the second row draws now: the front group's, when it has more than one. */
    List<String> drawnPages() {
        List<String> out = new ArrayList<>();
        String front = this.selectedGroup();
        if (front != null) {
            List<String> shown = this.shownIn(this.groups.get(front));
            if (shown.size() > 1) {
                out.addAll(shown);
            }
        }
        return out;
    }

    /** The pages of a group that are shown for this chart, in the group's order. */
    private List<String> shownIn(List<String> members) {
        List<String> out = new ArrayList<>();
        for (String page : members) {
            Tab tab = this.tabs.get(page);
            if (tab != null && tab.isVisible()) {
                out.add(page);
            }
        }
        return out;
    }

    private void rememberGroup(String page) {
        String g = this.groupOf(page);
        if (g != null) {
            this.lastInGroup.put(g, page);
        }
    }

    /** Open a group: the view it had in front last, or else its first page that can open. */
    private void openGroup(String label) {
        List<String> members = this.groups.get(label);
        String last = this.lastInGroup.get(label);
        if (last != null && this.usable(last)) {
            this.select(last);
            return;
        }
        for (String page : members) {
            if (this.usable(page)) {
                this.select(page);
                return;
            }
        }
    }

    private boolean usable(String page) {
        Tab tab = this.tabs.get(page);
        return tab != null && tab.enabled && tab.isVisible();
    }

    /**
     * Where to land when the page in front goes away: the same group first, so a reader who
     * changes chart tab stays on the same kind of page, then anywhere.
     */
    private List<String> fallbackOrder(String name) {
        List<String> out = new ArrayList<>();
        String g = this.groupOf(name);
        if (g != null) {
            out.addAll(this.groups.get(g));
        }
        for (String page : this.order) {
            if (!out.contains(page)) {
                out.add(page);
            }
        }
        return out;
    }

    /** Redraws both rows from the pages' flags. A no-op on a flat strip. */
    private void refreshStrip() {
        if (this.groups == null) {
            return;
        }
        this.groupRow.removeAll();
        for (String label : this.groupLabels()) {
            this.groupRow.add(this.groupTabs.get(label));
        }
        this.pageRow.removeAll();
        for (String page : this.drawnPages()) {
            this.pageRow.add(this.tabs.get(page));
        }
        this.pageRow.setVisible(this.pageRow.getComponentCount() > 0);
        this.strip.revalidate();
        this.strip.repaint();
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
                // A hidden tab takes no room: the chart in front has no use for that page.
                if (!c.isVisible()) {
                    continue;
                }
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

    /** A tab in the top row: a group of pages, lit while one of its pages is in front. */
    private final class GroupTab extends JComponent {
        private final String label;
        private boolean hover;

        GroupTab(String label) {
            this.label = label;
            this.setFont(Theme.BODY);
            int w = this.getFontMetrics(Theme.BODY.deriveFont(java.awt.Font.BOLD))
                .stringWidth(label) + 26;
            this.setPreferredSize(new Dimension(w, 26));
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    GroupTab.this.hover = true;
                    GroupTab.this.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    GroupTab.this.hover = false;
                    GroupTab.this.repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    SubRail.this.openGroup(GroupTab.this.label);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            boolean on = this.label.equals(SubRail.this.selectedGroup());
            g2.setColor(on ? TAB_ON : (this.hover ? TAB_HOVER : STRIP_BG));
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 7, 7);
            if (on) {
                g2.setColor(Theme.ACCENT);
                g2.fillRect(3, this.getHeight() - 2, this.getWidth() - 6, 2);
            }
            g2.setColor(on ? Color.WHITE : Theme.TEXT);
            g2.setFont(Theme.BODY.deriveFont(java.awt.Font.BOLD));
            int tw = g2.getFontMetrics().stringWidth(this.label);
            g2.drawString(this.label, (this.getWidth() - tw) / 2,
                this.getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
            g2.dispose();
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
