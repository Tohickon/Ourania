package com.zodiacomputing.ourania.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Selected, as a panel that opens on the left of the wheel (2026-10-22).
 *
 * <b>From the left, not from the bottom (David, 2026-10-22).</b> "i dont like how the selected
 * drawer pops up from the bottom lets open it up from the left again". It opened under the wheel
 * from 2026-10-20; it now opens between the left rail and the wheel, which is where Selected
 * sat when it was a rail page, and the rail still keeps the page the reader had.
 *
 * <b>The tab spec makes Selected an inspector, not a page.</b> A click on a body, a cusp or an
 * aspect line opens what was clicked right beside the wheel, and the reader stays on whatever tab
 * they were reading. As a page of the left rail it took the rail over: the Reading or the year
 * the reader had open was swapped out for the body they had just clicked, and to get back they
 * had to find the tab again.
 *
 * <b>A split pane, so the reader sets the width.</b> The wheel takes what is left. When the
 * drawer is shut, the split pane shows the wheel alone, with no divider.
 */
final class SelectionDrawer extends JPanel {

    /** How wide the drawer opens the first time, before the reader has dragged it. */
    static final int OPEN_WIDTH = 360;

    private final JSplitPane split;
    private boolean placed;

    /**
     * @param wheel what the drawer opens beside
     * @param body  the selection's page, already in its scroller
     */
    SelectionDrawer(JComponent wheel, JComponent body) {
        super(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.EDGE));
        JLabel title = new JLabel("Selected");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.BODY);
        JButton close = new JButton("Close");
        Widgets.styleButton(close, Widgets.Role.TRANSPORT);
        close.setToolTipText("Put the selection away. Click the wheel again to open it.");
        close.addActionListener(e -> close());
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(Theme.pad(2, Theme.GAP, 2, 2));
        head.add(title, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.add(close);
        head.add(right, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        setMinimumSize(new Dimension(160, 0));
        setVisible(false);

        this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this, wheel);
        // The wheel takes any change in the window's width; the drawer keeps the reader's.
        this.split.setResizeWeight(0.0);
        this.split.setContinuousLayout(true);
        this.split.setBorder(null);
        this.split.setDividerSize(6);
    }

    /** The wheel with the drawer beside it: what goes where the wheel went. */
    JSplitPane withWheel() {
        return this.split;
    }

    /** Open, at the reader's last height or the first one. */
    void open() {
        if (isVisible()) {
            return;
        }
        setVisible(true);
        this.split.revalidate();
        // The first opening sets the width; after that the divider stays where the reader left
        // it. A window too narrow to spare OPEN_WIDTH gives the drawer half of it. Before the
        // window is laid out there is no width to divide, and the split pane's own sizes stand.
        int w = this.split.getWidth();
        if (!this.placed && w > 0) {
            this.split.setDividerLocation(Math.min(w / 2, OPEN_WIDTH));
            this.placed = true;
        }
    }

    void close() {
        setVisible(false);
        this.split.revalidate();
    }

    boolean isOpen() {
        return isVisible();
    }
}
