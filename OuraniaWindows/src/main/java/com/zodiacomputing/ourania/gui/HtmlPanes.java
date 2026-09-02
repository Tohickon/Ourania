package com.zodiacomputing.ourania.gui;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;

/**
 * The clickable, hoverable HTML panes the chart data is rendered into.
 *
 * <p><b>Extracted when the sidebar became two sidebars.</b> The placements, transits and aspect
 * grid moved to a right-hand drawer while the navigation stayed on the left, and both sides
 * needed the same pane: same hit test, same hover card, same scroll handling. A second copy of
 * {@link #hrefAt} in particular would have been the defect this project logs more than any
 * other - one rule implemented twice, and only one of the copies corrected. The bug history
 * below is exactly what a second copy would have re-introduced.
 */
final class HtmlPanes {

    private HtmlPanes() { }

    /**
     * A chart pane: renders the wheel's HTML, resolves its own tooltips, and lights the wheel
     * on hover.
     *
     * <b>Each section gets its own pane rather than sharing one.</b> A component can only be in
     * one place at a time, so a shared pane would blank whichever section was not currently
     * showing it - and a blank section looks exactly like a chart with nothing to report.
     */
    static JEditorPane chartPane(final OuraniaWindow window) {
        return chartPane(window, true);
    }

    /**
     * A pane for content that must keep its own width - the aspect grid.
     *
     * <b>Wrapping is right for the placements and wrong for the grid.</b> The placements are a
     * column of short lines and should reflow into whatever width the drawer has. The grid is a
     * table whose column count is the number of points switched on: with the asteroids and
     * angles enabled it is far wider than any sidebar, and forcing it to the viewport width
     * squeezed the columns until the glyphs collided and the table could not be read at all.
     * It needs its natural width and a scrollbar under it.
     */
    static JEditorPane wideChartPane(final OuraniaWindow window) {
        return chartPane(window, false);
    }

    private static JEditorPane chartPane(final OuraniaWindow window, final boolean wrap) {
        final JEditorPane pane = new JEditorPane() {
            @Override
            public String getToolTipText(MouseEvent event) {
                String href = hrefAt(this, event.getPoint());
                String card = href == null || window == null
                    ? null : window.aspectHoverHtml(href);
                return card != null ? card : super.getToolTipText(event);
            }

            /**
             * Wrap to the panel instead of scrolling sideways.
             *
             * <b>The placements HTML sets no body width</b>, so the pane laid itself out at its
             * natural width, overflowed the viewport and grew a horizontal scrollbar across the
             * sidebar. Tracking the viewport is the fix rather than hardcoding a width in the
             * HTML, because a drawer can be opened and shut and a pinned pixel width would clip
             * or gap the moment it was.
             */
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return wrap;
            }
        };
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(Theme.SURFACE);
        pane.setForeground(Color.WHITE);
        pane.setOpaque(true);
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && window != null) {
                // E.g. href="base_0" for base sun, "transit_1" for transit moon
                window.handlePlacementClick(e.getDescription());
            }
        });
        ToolTipManager.sharedInstance().registerComponent(pane);
        // Hovering a grid cell lights the matching line on the wheel. setHighlightedAspect
        // returns false when nothing changed, so a mouse dragged across one cell repaints the
        // chart once rather than on every pixel of travel.
        pane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (window != null) {
                    window.highlightAspect(hrefAt(pane, e.getPoint()));
                }
            }
        });
        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (window != null) {
                    window.highlightAspect(null);
                }
            }
        });
        return pane;
    }

    /**
     * Sets a pane's HTML, keeping where the reader was.
     *
     * <b>The scroll position matters more than it looks.</b> The chart updates on every
     * animation frame while the wheel is playing, and a pane that jumped back to the top on
     * each frame could not be read at all.
     */
    static void setHtml(JEditorPane pane, String html) {
        if (pane == null) {
            return;
        }
        Container parent = pane.getParent();
        if (parent instanceof JViewport) {
            JScrollPane scrollPane = (JScrollPane) parent.getParent();
            final JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            final JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
            final int scrollValue = verticalBar.getValue();

            pane.setText(html);

            // The vertical position is worth keeping - it is where you were reading - but the
            // horizontal one is pinned back to the left rather than preserved. The aspect grid
            // is wider than this column once enough points are switched on, and a pane left
            // parked at the right-hand end came up showing a column of "House VII" with every
            // glyph and degree off screen.
            SwingUtilities.invokeLater(() -> {
                verticalBar.setValue(scrollValue);
                horizontalBar.setValue(0);
            });
        } else {
            pane.setText(html);
            pane.setCaretPosition(0);
        }
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
    static String hrefAt(JEditorPane pane, Point pt) {
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
                Object anchor = doc.getCharacterElement(p).getAttributes()
                    .getAttribute(HTML.Tag.A);
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
}
