package com.zodiacomputing.ourania.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * An accordion: titled sections that expand and retract, stacked in one column.
 *
 * <p><b>The accordion organises content; the {@link Drawer} is the space it sits in.</b> Those
 * are two different jobs and were one class until 2026-09-01, which is why the first version
 * read as neither - a rail of tabs that was too menu-like to be a drawer and too flat to
 * organise anything. The drawer slides the whole sidebar in and out; the accordion decides what
 * you see once it is open.
 *
 * <h2>Sections are independent, not mutually exclusive</h2>
 *
 * Opening one does not shut the others. The sidebar's sections are things a reader compares -
 * the natal placements against the transits against the aspect grid - and a one-at-a-time
 * accordion would make comparing any two of them impossible. Everything open at once scrolls,
 * which the drawer already handles.
 */
public final class Accordion extends JPanel {

    private final List<Section> sections = new ArrayList<>();

    public Accordion() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** Adds a titled section and returns it, so a caller can refresh or open it later. */
    public Section addSection(String title, JComponent content) {
        Section section = new Section(title, content);
        sections.add(section);
        add(section);
        add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));
        return section;
    }

    /** The section with this title, or null. */
    public Section section(String title) {
        for (Section s : sections) {
            if (s.title().equals(title)) {
                return s;
            }
        }
        return null;
    }

    public List<Section> sections() {
        return new ArrayList<>(sections);
    }

    /**
     * One section: a header you press, and a panel that slides open under it.
     *
     * <h2>Height is measured, never fixed</h2>
     *
     * The open height is the content's own preferred height, capped by
     * {@link #setMaxOpenHeight} so one long section cannot push the ones below it off the
     * bottom of the window; content past the cap scrolls inside its own section. A fixed
     * height would be wrong in both directions at once - far too tall for "Menu", far too
     * short for a full aspect grid.
     */
    public static final class Section extends JPanel {

        private static final Color HEADER_BG = Theme.SURFACE_3;
        private static final Color HEADER_HOVER = Theme.SURFACE_3;
        private static final Color HEADER_OPEN = Theme.EDGE_ACTIVE;
        private static final Color EDGE = Theme.EDGE;
        private static final Color TITLE = Theme.TEXT;

        /** Frame interval and travel per frame. About a dozen frames end to end. */
        private static final int FRAME_MS = 12;
        private static final int STEP_PX = 34;

        private final String title;
        private final JPanel header;
        private final JLabel chevron;
        static final String CLOSED = "\u25B8";
        static final String OPEN = "\u25BE";
        private final JPanel holder;
        private final JScrollPane scroller;
        private JScrollPane scrollerRef;
        private final Timer animator;

        private boolean open;
        private int currentHeight;
        private int targetHeight;
        private int maxOpenHeight = 420;

        Section(String title, JComponent content) {
            this.title = title;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);

            // The triangle it was always meant to be. It was "+" while Theme's fonts were
            // physical and drew U+25B8 as an empty box; see Theme.font.
            chevron = new JLabel(CLOSED);
            chevron.setForeground(TITLE);
            chevron.setFont(Theme.HEADING);
            chevron.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

            JLabel name = new JLabel(title);
            name.setForeground(TITLE);
            name.setFont(Theme.HEADING);

            header = new JPanel(new BorderLayout());
            header.setBackground(HEADER_BG);
            header.setBorder(Theme.card(EDGE, Theme.GAP));
            header.add(chevron, BorderLayout.WEST);
            header.add(name, BorderLayout.CENTER);
            header.setCursor(new Cursor(Cursor.HAND_CURSOR));
            header.setToolTipText(title);
            header.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    toggle();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!open) {
                        header.setBackground(HEADER_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    header.setBackground(open ? HEADER_OPEN : HEADER_BG);
                }
            });

            scroller = new JScrollPane(content);
            scroller.setBorder(BorderFactory.createEmptyBorder());
            scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            this.scrollerRef = scroller;
            Widgets.styleScrollPane(scroller);

            // The holder is what animates. The scroll pane inside keeps its full size, so
            // sliding never reflows the content - only how much of it is showing.
            holder = new JPanel(new BorderLayout());
            holder.setBackground(Theme.SURFACE);
            holder.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, EDGE));
            holder.add(scroller, BorderLayout.CENTER);

            add(header);
            add(holder);

            animator = new Timer(FRAME_MS, e -> step());
            applyHeight(0);
        }

        /**
         * Lets this section scroll sideways.
         *
         * <b>Off everywhere except the aspect grid</b>, which is the only content here with a
         * width of its own: a horizontal scrollbar under a column of text is clutter, but a
         * table wider than the drawer cannot be read without one.
         */
        public void setHorizontalScrollAllowed(boolean allowed) {
            scrollerRef.setHorizontalScrollBarPolicy(allowed
                ? JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            // The bar takes room from the content, so the section needs to be that much
            // taller or it clips the table's last row to make space for the bar.
            if (allowed) {
                setMaxOpenHeight(maxOpenHeight + 16);
            }
        }

        /** How tall this section may open before its content starts scrolling instead. */
        public void setMaxOpenHeight(int px) {
            this.maxOpenHeight = Math.max(80, px);
        }

        public String title() {
            return title;
        }

        public boolean isOpen() {
            return open;
        }

        public void toggle() {
            setOpen(!open);
        }

        public void setOpen(boolean shouldOpen) {
            this.open = shouldOpen;
            chevron.setText(shouldOpen ? OPEN : CLOSED);
            header.setBackground(shouldOpen ? HEADER_OPEN : HEADER_BG);
            targetHeight = shouldOpen ? openHeight() : 0;
            // <b>No animation when nothing can see it.</b> A section opened before the window
            // is on screen - the state the app starts in, and every offscreen render - would
            // otherwise sit at zero height until a Timer that is not running yet advanced it,
            // which reads as a section that opened onto nothing. Animate only what is visible.
            if (!isShowing()) {
                animator.stop();
                applyHeight(targetHeight);
                return;
            }
            if (!animator.isRunning()) {
                animator.start();
            }
        }

        /** Replaces what the section holds, leaving it open or shut as it was. */
        public void setContent(JComponent content) {
            scroller.setViewportView(content);
            if (open) {
                // Re-measured rather than left at the old height: a chart change can turn
                // three placements into thirty, and a section that kept the previous height
                // would clip the difference with no scrollbar to say that it had.
                targetHeight = openHeight();
                applyHeight(targetHeight);
            }
        }

        private int openHeight() {
            Component view = scroller.getViewport().getView();
            int wanted = view == null ? 0 : view.getPreferredSize().height;
            return Math.min(Math.max(wanted + 6, 60), maxOpenHeight);
        }

        private void step() {
            if (currentHeight == targetHeight) {
                animator.stop();
                return;
            }
            int delta = targetHeight - currentHeight;
            int move = Math.abs(delta) <= STEP_PX ? delta : (delta > 0 ? STEP_PX : -STEP_PX);
            applyHeight(currentHeight + move);
        }

        private void applyHeight(int height) {
            currentHeight = height;
            holder.setVisible(height > 0);
            holder.setPreferredSize(new Dimension(0, height));
            holder.setMinimumSize(new Dimension(0, height));
            // <b>The maximum is the one that actually binds.</b> BoxLayout sizes its children
            // along its own axis from the MAXIMUM size, not the preferred - so setting only
            // the preferred height animates nothing: the section jumps fully open on the first
            // frame and stays there. The wheel's control strip had this same defect in the
            // other axis, where it clipped the House System dropdown instead.
            holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            revalidate();
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
