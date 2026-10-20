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
 * Selected, as a drawer under the wheel (2026-10-20).
 *
 * <b>The tab spec makes Selected an inspector, not a page.</b> A click on a body, a cusp or an
 * aspect line opens what was clicked right under the wheel, and the reader stays on whatever tab
 * they were reading. As a page of the left rail it took the rail over: the Reading or the year
 * the reader had open was swapped out for the body they had just clicked, and to get back they
 * had to find the tab again.
 *
 * <b>A split pane, so the reader sets the height.</b> The wheel takes what is left. When the
 * drawer is shut, the split pane shows the wheel alone, with no divider.
 */
final class SelectionDrawer extends JPanel {

    /** How tall the drawer opens the first time, before the reader has dragged it. */
    static final int OPEN_HEIGHT = 260;

    private final JSplitPane split;
    private boolean placed;

    /**
     * @param wheel what the drawer opens under
     * @param body  the selection's page, already in its scroller
     */
    SelectionDrawer(JComponent wheel, JComponent body) {
        super(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.EDGE));
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
        setMinimumSize(new Dimension(0, 80));
        setVisible(false);

        this.split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, wheel, this);
        this.split.setResizeWeight(1.0);
        this.split.setContinuousLayout(true);
        this.split.setBorder(null);
        this.split.setDividerSize(6);
    }

    /** The wheel with the drawer under it: what goes where the wheel went. */
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
        // The first opening sets the height; after that the divider stays where the reader left
        // it. A window too short to spare OPEN_HEIGHT gives the drawer half of it. Before the
        // window is laid out there is no height to divide, and the split pane's own sizes stand.
        int h = this.split.getHeight();
        if (!this.placed && h > 0) {
            this.split.setDividerLocation(Math.max(h / 2, h - OPEN_HEIGHT));
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
