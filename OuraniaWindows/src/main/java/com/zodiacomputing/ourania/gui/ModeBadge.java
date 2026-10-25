package com.zodiacomputing.ourania.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The strip above the four tabs: which kind of reading this is, and of what.
 *
 * <b>Reading framework spec, 2026-10-24 (section 2.1):</b> the tab bar never changes, so
 * something else has to say what the four tabs are about. A pill names the mode - NATAL,
 * CURRENT SKY, TRANSITS, SYNASTRY, COMPOSITE, TRANSITING SYNASTRY or TRANSITING COMPOSITE - and
 * one line names the charts loaded.
 *
 * <b>The rules are static and take plain values,</b> so a suite can ask them without a window
 * and the window passes in only what it already knows.
 */
final class ModeBadge extends JPanel {

    static final String NATAL = "NATAL";
    static final String SKY = "CURRENT SKY";
    static final String TRANSITS = "TRANSITS";
    static final String SYNASTRY = "SYNASTRY";
    static final String COMPOSITE = "COMPOSITE";
    static final String TRANSITING_SYNASTRY = "TRANSITING SYNASTRY";
    static final String TRANSITING_COMPOSITE = "TRANSITING COMPOSITE";

    private final Pill pill = new Pill();
    private final JLabel context = new JLabel(" ");

    ModeBadge() {
        super(new BorderLayout(8, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(6, 8, 4, 8));
        context.setForeground(Theme.TEXT_DIM);
        context.setFont(Theme.BODY);
        add(pill, BorderLayout.WEST);
        add(context, BorderLayout.CENTER);
    }

    void show(String mode, String line) {
        pill.setText(mode);
        context.setText(line == null || line.isEmpty() ? " " : line);
        context.setToolTipText(line);
        revalidate();
        repaint();
    }

    String mode() {
        return pill.getText();
    }

    String line() {
        return context.getText();
    }

    /**
     * The mode a chart tab is in.
     *
     * <b>The two transiting modes are not tabs of their own.</b> A pair under the sky is the
     * Synastry tab with the sky switched on, or the Transits tab with both people chosen; a
     * composite under the sky is the Composite tab with it on. The sky does not make a tab
     * "transiting" unless there are people under it.
     */
    static String modeOf(ChartTab tab, boolean skyOn, boolean aIn, boolean bIn) {
        switch (tab) {
            case SKY:
                return SKY;
            case TRANSITS:
                return aIn && bIn ? TRANSITING_SYNASTRY : TRANSITS;
            case SYNASTRY:
                return skyOn ? TRANSITING_SYNASTRY : SYNASTRY;
            case COMPOSITE:
                return skyOn ? TRANSITING_COMPOSITE : COMPOSITE;
            default:
                return NATAL;
        }
    }

    /**
     * One line naming the charts loaded: who, and for a person where they were born.
     *
     * @param a      Chart A's name, already resolved to "Chart A" when it has none
     * @param aPlace Chart A's place, or null
     * @param skyAt  the sky's moment, or null
     */
    static String contextOf(ChartTab tab, String a, String aPlace, String b, String bPlace,
                            ChartMode mode, ZonedDateTime skyAt) {
        switch (tab) {
            case CHART_A:
                return named(a, aPlace);
            case CHART_B:
                return named(b, bPlace);
            case SKY:
                return skyAt == null ? "The sky"
                    : "The sky · " + DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm",
                        Locale.ENGLISH).format(skyAt);
            case TRANSITS:
                return "The current sky on " + a + (b == null || b.isEmpty() ? "" : " and " + b);
            case SYNASTRY:
                return a + " & " + b;
            case COMPOSITE:
                return a + " & " + b + " · "
                    + (mode == ChartMode.COMPOSITE_DAVISON ? "Davison" : "Midpoint");
            default:
                return "";
        }
    }

    private static String named(String who, String place) {
        return place == null || place.trim().isEmpty() ? who : who + " · " + place.trim();
    }

    /** The rounded label for the mode. */
    private static final class Pill extends JLabel {
        Pill() {
            super(" ");
            setFont(Theme.BODY.deriveFont(java.awt.Font.BOLD, 11f));
            setForeground(Theme.BG);
            setBorder(new EmptyBorder(2, 10, 2, 10));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, Math.max(d.height, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor((Color) Theme.ACCENT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
