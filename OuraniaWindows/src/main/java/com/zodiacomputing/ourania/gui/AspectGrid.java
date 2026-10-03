package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;

import java.util.List;

/**
 * The aspect grid in the side panel: a heading, a legend, and a table whose columns are the
 * base chart's points and whose rows come in bands, one per ring.
 *
 * <h3>Why this is its own class (J13, step 3c)</h3>
 *
 * <p>It was the second half of {@code SkymapPanel.generatePlanetPlacementsHtml}, and the reason
 * it stayed behind when the placements left in step 3b is that it asks a question the
 * placements do not: which aspect do these two points make, and is it switched on. That
 * question is now {@link AspectGate}, shared with the wheel, so the grid can take the answer
 * with it rather than a copy of the rule.
 *
 * <p>The panel still decides everything that is about the panel: the heading, which rings get
 * rows, what each is called and which orb profile it is judged at. It hands those over as
 * {@link Band}s. Everything here is static.
 *
 * <h3>What has not changed</h3>
 *
 * <p>Every string is the string the panel built, character for character.
 */
final class AspectGrid {

    /** The width the aspect grid has to fit into: the drawer, less padding and scrollbar. */
    private static final int GRID_FIT_WIDTH = 276;

    /** Below this the glyphs stop being distinguishable, so the grid scrolls rather than lies. */
    private static final int GRID_MIN_CELL = 11;

    private AspectGrid() {
    }

    /**
     * One ring's rows.
     *
     * @param word    what the ring is called, for the band over its rows; null for the base
     *                chart, whose rows need no qualifier
     * @param wheel   which wheel the rows are on, carried into each cell's link
     * @param profile the orbs its pairs are judged at
     */
    record Band(String word, WheelRing ring, int wheel, Aspects.Profile profile) {
    }

    /**
     * The heading, the legend and the table.
     *
     * @param columns the base chart - every aspect on this grid is something aspecting it
     * @param shown   the reader's aspect switches, as {@link AspectGate} takes them
     */
    static String table(String heading, WheelRing columns, List<Band> bands, boolean[] shown) {
        StringBuilder stringBuilder = new StringBuilder();
        int n;
        stringBuilder.append("<h3 style='color:white; margin-bottom: 4px;'>").append(heading)
            .append("</h3>");
        stringBuilder.append("<div style='font-size:10px; margin-bottom:10px;'>");
        // Quincunx belongs here: the grid has always emitted quincunx cells, so leaving it out
        // of the legend left a symbol on the table that nothing explained.
        // Driven from Aspects.Type rather than a hand-written list, so an aspect the engine
        // can emit cannot be missing from the legend. That is exactly how the quincunx came to
        // be drawn on the grid with nothing explaining it.
        for (Aspects.Type aspectType : Aspects.Type.values()) {
            String string5 = aspectType.label;
            stringBuilder.append("<span style='color:").append(SkymapPanel.getAspectColorHex(string5)).append("; font-size:14px;'>").append(SkymapPanel.getAspectSymbol(string5)).append("</span> <span style='color:#ccc;'>").append(string5).append("</span> &nbsp; ");
        }
        stringBuilder.append("</div>");
        // <b>The grid sizes itself to the points that are switched on.</b> It was a fixed
        // 22px cell at 14px type, which is a table as wide as the number of bodies enabled -
        // with the asteroids and angles on that is far wider than any drawer, and the columns
        // ran off the edge where they could not be read at all. Scaled to fit instead of
        // scrolled: a triangular grid is read by scanning across a row, and a table you have
        // to drag sideways to finish one row is worse than a small one you can take in whole.
        int gridCols = 0;
        for (n = 0; n < SkymapPanel.BODY_COUNT; ++n) {
            if (WheelLayout.aspecting(n, columns.valid)) {
                gridCols++;
            }
        }
        // The drawer's content width, less its padding and the vertical scrollbar.
        int cell = gridCols > 0 ? (GRID_FIT_WIDTH / (gridCols + 1)) : 22;
        cell = Math.max(GRID_MIN_CELL, Math.min(22, cell));
        int glyph = Math.max(8, cell - 3);
        stringBuilder.append("<table border='1' cellspacing='0' cellpadding='0' style='border-collapse: collapse; border-color: #555; text-align:center;'>");
        stringBuilder.append("<tr><td style='width:").append(cell).append("px;'></td>");
        for (n = 0; n < SkymapPanel.BODY_COUNT; ++n) {
            if (!WheelLayout.aspecting(n, columns.valid)) continue;
            stringBuilder.append("<td style='color:").append(SkymapPanel.bodyColorHex(n)).append("; font-size:").append(glyph).append("px; width:").append(cell).append("px;'>").append(SkymapPanel.BODY_GLYPHS[n]).append("</td>");
        }
        stringBuilder.append("</tr>");
        for (Band band : bands) {
            appendBand(stringBuilder, band.word(), gridCols);
            appendRows(stringBuilder, band, columns, cell, glyph, shown);
        }
        stringBuilder.append("</table>");
        return stringBuilder.toString();
    }

    /**
     * A band across the grid naming the ring whose rows follow it.
     *
     * <b>Through ringWord, not a literal.</b> The sky band used to be the string "sky" written
     * here, which is a second copy of a rule that already had a home - and the copies on this
     * particular question have drifted apart five times now, most recently leaving Chart B
     * labelled "(transiting)" on the aspect card. The panel asks ringWord and passes the answer,
     * so the grid cannot disagree with the card about what a ring is called.
     *
     * <b>Silent for the inner wheel.</b> ringWord returns null there: rows that are simply the
     * chart being read need no qualifier, and a band saying "natal" over a natal-only grid
     * would be noise.
     */
    private static void appendBand(StringBuilder sb, String word, int gridCols) {
        if (word == null) {
            return;
        }
        sb.append("<tr><td colspan='").append(gridCols + 1)
          .append("' style='color:#8FD0FF; font-size:10px; text-align:left;"
              + " padding:3px 0 1px 2px; background-color:#111;'>")
          .append(word).append("</td></tr>");
    }

    /**
     * One wheel's worth of rows in the aspect grid.
     *
     * <b>Written once because there are three wheels now.</b> The rows used to be built inline
     * with the wheel chosen by a boolean in three separate expressions, which is the shape the
     * sky ring could not be added to without a fourth copy - and a fourth copy of a cell test
     * is a fourth chance for the grid to disagree with the wheel about what is in aspect.
     *
     * Columns are always the natal points: every aspect on this grid is something aspecting
     * the chart. The natal rows are the triangular half, since a natal pair appears once.
     */
    private static void appendRows(StringBuilder out, Band band, WheelRing columns, int cell,
            int glyph, boolean[] shown) {
        int wheel = band.wheel();
        double[] lon = band.ring().lon;
        boolean[] valid = band.ring().valid;
        for (int n = 0; n < SkymapPanel.BODY_COUNT; ++n) {
            if (!WheelLayout.aspecting(n, valid)) continue;
            out.append("<tr>");
            out.append("<td style='color:").append(SkymapPanel.bodyColorHex(n))
               .append("; font-size:").append(glyph).append("px; width:").append(cell)
               .append("px;'>").append(SkymapPanel.BODY_GLYPHS[n]).append("</td>");
            for (int i = 0; i < SkymapPanel.BODY_COUNT; ++i) {
                if (!WheelLayout.aspecting(i, columns.valid)) continue;
                if ((wheel == WheelLayout.WHEEL_NATAL && i >= n) || Bodies.isOppositePair(n, i)) {
                    out.append("<td style='background-color:#111;'></td>");
                    continue;
                }
                double sep = Math.abs(lon[n] - columns.lon[i]);
                if (sep > 180.0) {
                    sep = 360.0 - sep;
                }
                String type = AspectGate.label(sep, n, i, band.profile(), shown);
                if (type == null) {
                    out.append("<td style='background-color:#222;'></td>");
                    continue;
                }
                String row = wheel == WheelLayout.WHEEL_NATAL ? SkymapPanel.BODY_NAMES[n]
                    : SkymapPanel.TRANSIT_PREFIX + SkymapPanel.BODY_NAMES[n].toLowerCase();
                // Through aspectHref, never formatted inline: the parser is the only other
                // place that knows this format and the two must not be able to drift.
                String href = SkymapPanel.aspectHref(row, SkymapPanel.BODY_NAMES[i], type, wheel);
                out.append("<td style='background-color:#222;'><a href='").append(href)
                   .append("' style='text-decoration:none;'>").append("<span style='color:")
                   .append(SkymapPanel.getAspectColorHex(type)).append("; font-size:").append(glyph)
                   .append("px;'>").append(SkymapPanel.getAspectSymbol(type))
                   .append("</span></a></td>");
            }
            out.append("</tr>");
        }
    }
}
