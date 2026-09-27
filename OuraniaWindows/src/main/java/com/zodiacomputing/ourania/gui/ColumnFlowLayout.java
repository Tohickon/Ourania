package com.zodiacomputing.ourania.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * As many columns as the width will take, each its own height (H4a, second pass).
 *
 * <p><b>The Bodies &amp; Points tab used 752 pixels of 1460 and scrolled.</b> Closing the void
 * <i>inside</i> the boxes on 26 Sep left the space <i>beside</i> them: two fixed columns, 45% of
 * the width black, and the last group cut off at the bottom of a scroll. David, 27 Sep: "the gap
 * of space in the settings bodies and points".
 *
 * <p><b>Two columns was as arbitrary as one.</b> A fixed count encodes a window size, the same
 * mistake as the pixel threshold in {@code RimAndFillsCheck} the same morning - a fact about one
 * machine written into the app. The count is computed from the width actually available, so the
 * tab fills a wide window and still reads on a narrow one.
 *
 * <p><b>Columns share the leftover rather than leaving it at the edge.</b> The count is the most
 * that fit at the children's natural width, so the remainder is always less than one more column
 * and spreading it grows each by at most about a third. The alternative - natural widths and a
 * ragged black margin - is the gap again, smaller.
 *
 * <p><b>Filled column by column to an even height, the way a newspaper sets columns.</b> Dealing
 * them round-robin was tried first and measured: with boxes of 247, 260, 157, 397 and 530 pixels
 * it puts 658 in the first column, 804 in the second and 157 in the third, so the third is a
 * short box over a large hole - the gap again, moved. Filling each column until the next box
 * would take it past its share gives 521, 568 and 530, and it keeps the order the groups are
 * read in, because a column is read down before the eye moves across.
 */
public final class ColumnFlowLayout implements LayoutManager {

    private final int hgap;
    private final int vgap;

    /**
     * The width assumed before the container has one.
     *
     * <b>Swing asks for a preferred size before it has laid anything out</b>, and a preferred
     * size computed against a width of zero would be one column and enormously tall - which is
     * what the scroll pane would then believe. Two columns is what this tab had before and is a
     * sane first answer; the first real layout replaces it.
     */
    private static final int ASSUMED_COLUMNS = 2;

    public ColumnFlowLayout(int hgap, int vgap) {
        this.hgap = Math.max(0, hgap);
        this.vgap = Math.max(0, vgap);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // Nothing to record: position is decided entirely by order and width.
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // As above.
    }

    /** The widest any child wants to be; every column is at least this. */
    private int naturalWidth(Container parent) {
        int w = 0;
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) {
                w = Math.max(w, c.getPreferredSize().width);
            }
        }
        return w;
    }

    /** How many columns fit, given a width to fit them into. */
    int columnsFor(Container parent, int width) {
        int natural = naturalWidth(parent);
        int visible = visibleCount(parent);
        if (natural <= 0 || visible == 0) {
            return 1;
        }
        Insets in = parent.getInsets();
        int available = width - in.left - in.right;
        if (available <= 0) {
            return Math.min(ASSUMED_COLUMNS, visible);
        }
        int fit = (available + this.hgap) / (natural + this.hgap);
        return Math.max(1, Math.min(fit, visible));
    }

    private static int visibleCount(Container parent) {
        int n = 0;
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) {
                n++;
            }
        }
        return n;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = parent.getWidth();
            int columns = width > 0 ? columnsFor(parent, width)
                : Math.max(1, Math.min(ASSUMED_COLUMNS, visibleCount(parent)));
            int natural = naturalWidth(parent);

            int[] heights = share(parent, columns);
            int tallest = 0;
            for (int h : heights) {
                tallest = Math.max(tallest, h);
            }
            int wide = width > 0 ? width - in.left - in.right
                : columns * natural + (columns - 1) * this.hgap;
            return new Dimension(wide + in.left + in.right, tallest + in.top + in.bottom);
        }
    }

    /**
     * The height each column is aiming for: an even share of the whole stack.
     *
     * <b>A target, not a limit.</b> A column takes one more box than its share whenever stopping
     * short would leave the box for a column that has no room either - the last column always
     * takes whatever is left, so nothing is ever dropped.
     */
    private int targetHeight(Container parent, int columns) {
        int total = 0;
        int n = 0;
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) {
                if (n > 0) {
                    total += this.vgap;
                }
                total += c.getPreferredSize().height;
                n++;
            }
        }
        return columns <= 0 ? total : (total + columns - 1) / columns;
    }

    /**
     * Which column each visible child goes in.
     *
     * <b>A column breaks only when it is closer to its share WITHOUT the next box than with
     * it.</b> Stopping the moment a box would take a column past its share was tried and
     * measured: with boxes of 247, 260, 157, 397 and 530 it refused a box that overshot by 19
     * pixels, left that column holding 157, and pushed the rest onto the last one - 521, 157,
     * 941, which is worse than dealing them round-robin. Overshooting a little beats undershooting
     * a lot, and saying so gives 521, 568 and 530.
     *
     * <b>One assignment, used both to measure and to place</b>, so the height reported and the
     * layout drawn cannot disagree - which is this project's most logged defect in miniature.
     */
    private int[] assign(Container parent, int columns) {
        int visible = visibleCount(parent);
        int[] out = new int[visible];
        if (visible == 0) {
            return out;
        }
        int target = targetHeight(parent, columns);
        int column = 0;
        int height = 0;
        int at = 0;
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }
            int h = c.getPreferredSize().height;
            if (column < columns - 1 && height > 0) {
                int with = Math.abs(height + this.vgap + h - target);
                int without = Math.abs(target - height);
                if (with > without) {
                    column++;
                    height = 0;
                }
            }
            out[at++] = column;
            height += height > 0 ? this.vgap + h : h;
        }
        return out;
    }

    /** What each column ends up holding, by that same assignment. */
    private int[] share(Container parent, int columns) {
        int[] heights = new int[Math.max(1, columns)];
        int[] where = assign(parent, columns);
        int at = 0;
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }
            int column = where[at++];
            if (heights[column] > 0) {
                heights[column] += this.vgap;
            }
            heights[column] += c.getPreferredSize().height;
        }
        return heights;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            // One column of the widest child: below this the boxes would have to clip.
            return new Dimension(naturalWidth(parent) + in.left + in.right, 0);
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int available = parent.getWidth() - in.left - in.right;
            if (available <= 0) {
                return;
            }
            int columns = columnsFor(parent, parent.getWidth());
            int columnWidth = (available - (columns - 1) * this.hgap) / columns;

            int[] where = assign(parent, columns);
            int[] y = new int[columns];
            for (int i = 0; i < columns; i++) {
                y[i] = in.top;
            }
            int at = 0;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                int column = where[at++];
                if (y[column] > in.top) {
                    y[column] += this.vgap;
                }
                int h = c.getPreferredSize().height;
                c.setBounds(in.left + column * (columnWidth + this.hgap), y[column],
                    columnWidth, h);
                y[column] += h;
            }
        }
    }
}
