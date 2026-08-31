package com.zodiacomputing.ourania.gui;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;

public class MainMenuPanel extends JPanel {

    private OuraniaWindow parentWindow;
    private JEditorPane placementsPane;

    public MainMenuPanel(OuraniaWindow parentWindow) {
        this.parentWindow = parentWindow;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK); // Dark grey background
        setPreferredSize(new Dimension(280, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // App Title Label
        JLabel titleLabel = new JLabel("Ourania+");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 15)));

        // Navigation Dropdown
        String[] menuItems = {
            "Navigate to...",
            "Grid Skymap",
            "Name List",
            "Interpretation",
            "Zodiacal Releasing",
            "Share Skymap",
            "Sync",
            "Connect",
            "Settings",
            "Help"
        };
        JComboBox<String> navCombo = new JComboBox<>(menuItems);
        navCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        // Through Widgets, not by hand. Setting the colours without swapping the UI delegate
        // is what this line used to do, and under the Windows look-and-feel that paints white
        // text into the LAF's own white value area: the menu was a blank box, so every entry
        // in it - Settings included - was invisible and effectively unreachable. sizeToContent
        // is false because the BoxLayout stretches this combo to the sidebar width anyway.
        Widgets.styleCombo(navCombo, new Font("Arial", Font.BOLD, 14), false);
        navCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        navCombo.addActionListener(e -> {
            String sel = (String) navCombo.getSelectedItem();
            if (sel != null) {
                switch (sel) {
                    case "Name List": parentWindow.switchScreen("NAME_LIST"); break;
                    case "Grid Skymap": parentWindow.switchScreen("SKYMAP"); break;
                    case "Interpretation": parentWindow.switchScreen("INTERPRETATION"); break;
                    case "Zodiacal Releasing": parentWindow.switchScreen("RELEASING"); break;
                    case "Share Skymap": parentWindow.switchScreen("SHARE"); break;
                    case "Sync": parentWindow.switchScreen("SYNC"); break;
                    case "Connect": parentWindow.switchScreen("CONNECT"); break;
                    case "Settings": parentWindow.switchScreen("SETTINGS"); break;
                    case "Help": parentWindow.switchScreen("HELP"); break;
                }
                navCombo.setSelectedIndex(0); // Reset after selection
            }
        });
        add(navCombo);
        add(Box.createRigidArea(new Dimension(0, 10)));

        addMenuButton("\uD83D\uDD0D Search Transit", "SEARCH");
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Placements text pane
        // The pane resolves its own tooltips from the href under the cursor. JEditorPane has
        // no notion of a per-element tooltip - the HTML `title` attribute is ignored by the
        // Swing renderer - so the card has to be looked up from the document model here.
        placementsPane = new JEditorPane() {
            @Override
            public String getToolTipText(MouseEvent event) {
                String href = hrefAt(this, event.getPoint());
                String card = href == null ? null : parentWindow.aspectHoverHtml(href);
                return card != null ? card : super.getToolTipText(event);
            }

            /**
             * Wrap to the panel instead of scrolling sideways.
             *
             * <b>The placements HTML sets no body width</b> - the hover cards next to it in
             * SkymapPanel both pin 250px, this one does not - so the pane laid itself out at
             * its natural width, overflowed the viewport and grew a horizontal scrollbar
             * across the bottom of the sidebar. Tracking the viewport is the fix rather than
             * hardcoding a width in the HTML, because the panel can be resized and a pinned
             * pixel width would clip or gap the moment it is.
             */
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        placementsPane.setContentType("text/html");
        placementsPane.setEditable(false);
        placementsPane.setBackground(new Color(20, 20, 20));
        placementsPane.setForeground(Color.WHITE);
        placementsPane.setOpaque(true);
        placementsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                // E.g. href="base_0" for base sun, "transit_1" for transit moon
                String command = e.getDescription();
                parentWindow.handlePlacementClick(command);
            }
        });

        ToolTipManager.sharedInstance().registerComponent(placementsPane);

        // Hovering a grid cell lights the matching line on the wheel. setHighlightedAspect
        // returns false when nothing changed, so a mouse dragged across one cell repaints the
        // chart once rather than on every pixel of travel.
        placementsPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                parentWindow.highlightAspect(hrefAt(placementsPane, e.getPoint()));
            }
        });
        placementsPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                parentWindow.highlightAspect(null);
            }
        });

        JScrollPane scrollPane = new JScrollPane(placementsPane);
        // One call rather than a hand-rolled border here: the pale stock scrollbars were
        // the only light thing in this panel, and styling them per-screen is the defect
        // this class documents at the top of Widgets.
        Widgets.styleScrollPane(scrollPane);
        // Belt and braces with getScrollableTracksViewportWidth above: nothing here should
        // ever need to scroll sideways, so the bar is not merely dark, it is absent.
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(scrollPane);
    }

    /**
     * The href of the link under a point in an HTML pane, or null.
     *
     * <b>viewToModel2D returns the nearest insertion point, not a hit test.</b> A click in the
     * margin, in an empty grid cell, or below the table still resolves to whichever character
     * is closest, so without a bounds check the tooltip would follow the cursor around the
     * whole pane reporting a link it is nowhere near.
     *
     * <b>And modelToView2D returns a zero-width caret rectangle</b>, so testing that rectangle
     * for containment rejects everything - the first version of this did exactly that and no
     * tooltip could ever appear. The character box has to be built from two carets: the one at
     * the position and the one after it.
     */
    private static String hrefAt(JEditorPane pane, Point pt) {
        if (!(pane.getDocument() instanceof HTMLDocument)) {
            return null;
        }
        try {
            int pos = pane.viewToModel2D(pt);
            if (pos < 0) {
                return null;
            }
            // viewToModel2D returns the nearest INSERTION POINT, so a point in the right half
            // of a glyph resolves to the offset after it - which belongs to the next cell, or
            // to no anchor at all. Both the character at pos and the one before it have to be
            // considered, or the right half of every cell is dead.
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            for (int p : new int[]{pos, pos - 1}) {
                if (p < 0 || p >= doc.getLength()) {
                    continue;
                }
                if (!overCharacter(pane, p, pt)) {
                    continue;
                }
                Object anchor = doc.getCharacterElement(p).getAttributes().getAttribute(HTML.Tag.A);
                if (anchor instanceof AttributeSet) {
                    Object href = ((AttributeSet) anchor).getAttribute(HTML.Attribute.HREF);
                    if (href != null) {
                        return href.toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            // Runs from mouseMoved and from getToolTipText; neither is worth throwing out of.
            return null;
        }
    }

    /**
     * True when pt lies inside the glyph box of the character at pos.
     *
     * <b>modelToView2D returns a zero-width caret</b>, not a character box, so the width has to
     * come from the caret after it. The first version of this tested {@code rect.contains(pt)}
     * on the caret itself, which is false everywhere, and no tooltip could appear at all.
     *
     * The two fallbacks below are defensive rather than demonstrated: measured across a full
     * 229-cell transit grid, no anchor's following caret wrapped to another line, so neither
     * branch was reached. They are kept because a cell alone on a wrapped line is cheap to
     * guard against and expensive to debug, but <b>do not read them as documenting a bug that
     * was seen</b> - the bug that was seen is the one in hrefAt above.
     */
    private static boolean overCharacter(JEditorPane pane, int pos, Point pt) throws Exception {
        Rectangle2D here = pane.modelToView2D(pos);
        if (here == null) {
            return false;
        }
        if (pt.getY() < here.getY() || pt.getY() > here.getY() + here.getHeight()) {
            return false;
        }
        double left = here.getX();
        double width = 0.0;
        if (pos + 1 <= pane.getDocument().getLength()) {
            Rectangle2D next = pane.modelToView2D(pos + 1);
            if (onSameLine(next, here) && next.getX() > left) {
                width = next.getX() - left;
            }
        }
        if (width <= 0.0 && pos > 0) {
            Rectangle2D prev = pane.modelToView2D(pos - 1);
            if (onSameLine(prev, here) && prev.getX() < left) {
                width = left - prev.getX();
            }
        }
        if (width <= 0.0) {
            // Nothing either side to measure against - a cell alone on its line. Fall back to
            // the line box, which is the only dimension still known to be real.
            width = here.getHeight();
        }
        return pt.getX() >= left && pt.getX() <= left + width;
    }

    private static boolean onSameLine(Rectangle2D other, Rectangle2D here) {
        return other != null && Math.abs(other.getY() - here.getY()) < 1.0;
    }

    public void updatePlanetPlacements(String html) {
        // Save current scroll position
        Container parent = placementsPane.getParent();
        if (parent instanceof JViewport) {
            JScrollPane scrollPane = (JScrollPane) parent.getParent();
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
            int scrollValue = verticalBar.getValue();

            placementsPane.setText(html);

            // Restore scroll position after layout update. The vertical position is worth
            // keeping - it is where you were reading - but the horizontal one is pinned back
            // to the left instead of preserved. The aspect grid is wider than this pane once
            // enough points are switched on, and a grid wide enough to need the scrollbar was
            // leaving the pane parked at the right-hand end, so the placements list came up
            // showing a column of "House VII" with every glyph and degree off-screen.
            SwingUtilities.invokeLater(() -> {
                verticalBar.setValue(scrollValue);
                horizontalBar.setValue(0);
            });
        } else {
            placementsPane.setText(html);
            placementsPane.setCaretPosition(0);
        }
    }

    private void addMenuButton(String text, String commandName) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setBackground(new Color(60, 120, 200));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        button.setHorizontalAlignment(SwingConstants.CENTER);

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(80, 140, 220));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(60, 120, 200));
            }
        });

        // Add action listener to switch screens
        button.addActionListener(e -> parentWindow.switchScreen(commandName));
        add(button);
    }
}
