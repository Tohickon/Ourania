package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The chart as a stack of rings you open, rather than a mode you pick.
 *
 * <b>David's idea, and it is a better question than the one the app was asking.</b> "Is this a
 * synastry or a natal-and-transit chart?" is a question about software; "who else is in this
 * chart, and is the sky over it?" is a question about astrology. The rings are the second
 * question: natal is always there, a partner blooms around it, the sky blooms around both.
 *
 * <b>This is not a second place the mode is decided.</b> It reports which rings the reader
 * wants and asks the window to apply that; Chart Setup remains the one writer of
 * {@code ChartMode}. Two controls independently setting the same field is the defect this
 * project has found in this panel twice - the Step dropdown, and the transits checkbox - and
 * both times it presented as the chart quietly disagreeing with the control that drew it.
 *
 * The composition maps onto the modes the engine already has:
 * <ul>
 *   <li>natal alone - {@code SINGLE}</li>
 *   <li>natal + sky - {@code TRANSIT}</li>
 *   <li>natal + partner - {@code SYNASTRY}</li>
 *   <li>natal + partner + sky - {@code SYNASTRY} with transits, which is the tri-wheel</li>
 * </ul>
 */
public final class RingBar extends JPanel {

    private final OuraniaWindow window;
    private final Chip partner;
    private final Chip sky;
    private final Chip globe;

    private boolean partnerOpen;
    private boolean skyOpen;
    /** True when a second chart has been entered at all; without one the ring has nothing. */
    private boolean partnerAvailable = true;

    public RingBar(OuraniaWindow window) {
        this.window = window;
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));
        setOpaque(false);

        Chip natal = new Chip("Natal", null);
        natal.fixed = true;
        add(natal);

        partner = new Chip("Partner", () -> {
            partnerOpen = !partnerOpen;
            apply();
        });
        partner.setToolTipText("<html><b>Bloom a second person around the natal wheel.</b><br>"
            + "Chart B's placements arrive in their own ring; the aspects between the two "
            + "charts are what a synastry is read for.<br>"
            + "<i>Needs Chart B filled in on the setup screen.</i></html>");
        add(partner);

        sky = new Chip("Sky", () -> {
            skyOpen = !skyOpen;
            apply();
        });
        sky.setToolTipText("<html><b>Bloom the sky around whatever is already drawn.</b><br>"
            + "Over a natal chart that is the transit wheel; over a natal and a partner it is "
            + "the third ring - the sky above both of them at once.</html>");
        add(sky);

        // <b>A view, not a ring - which is why it is last and reads differently.</b> The three
        // chips before it say what is in the chart; this one says how the chart is drawn, and
        // sits on the same strip because that is where a reader looking at the wheel already
        // is. It carries no ring state and never calls setRings.
        globe = new Chip("Globe", () -> {
            if (window != null) {
                window.setGlobeView(!globeOpen);
            }
        });
        globe.setToolTipText("<html><b>The same chart as nested shells.</b><br>"
            + "Every ring becomes a sphere - natal inside, a partner around it, the sky on "
            + "the outer skin - with the zodiac as a band at the equator.<br>"
            + "<i>Drag to turn, scroll to zoom.</i></html>");
        add(globe);
    }

    /** True when the wheel is currently drawn as a globe. */
    private boolean globeOpen;

    /** Reflects the view actually being painted, the way syncFrom does for the rings. */
    public void syncView(boolean showingGlobe) {
        this.globeOpen = showingGlobe;
        this.globe.label = showingGlobe ? "Wheel" : "Globe";
        this.globe.repaint();
    }

    /** Reflects what is actually drawn, so the chips cannot drift from the wheel. */
    public void syncFrom(ChartMode mode, boolean transits, boolean hasPartnerData) {
        this.partnerOpen = mode == ChartMode.SYNASTRY;
        this.skyOpen = mode == ChartMode.TRANSIT
            || (transits && (mode == ChartMode.SYNASTRY
                || mode == ChartMode.COMPOSITE_MIDPOINT
                || mode == ChartMode.COMPOSITE_DAVISON));
        this.partnerAvailable = hasPartnerData;
        // A composite is one derived wheel rather than two people side by side, so the
        // partner ring is not a thing that can be opened or folded there.
        boolean composite = mode == ChartMode.COMPOSITE_MIDPOINT
            || mode == ChartMode.COMPOSITE_DAVISON;
        partner.enabled = !composite;
        partner.label = composite ? "In composite" : "Partner";
        repaintChips();
    }

    private void apply() {
        if (window != null) {
            window.setRings(partnerOpen, skyOpen);
        }
        repaintChips();
    }

    private void repaintChips() {
        partner.repaint();
        sky.repaint();
        globe.repaint();
    }

    /** One ring, as a chip that reads as open or folded. */
    private final class Chip extends JComponent {

        String label;
        boolean fixed;
        boolean enabled = true;
        private final Runnable onClick;
        private boolean hover;

        Chip(String label, Runnable onClick) {
            this.label = label;
            this.onClick = onClick;
            setFont(Theme.SMALL);
            if (onClick != null) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (enabled) {
                            onClick.run();
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }
        }

        private boolean open() {
            if (fixed) {
                return true;
            }
            if (this == partner) {
                return partnerOpen && partnerAvailable;
            }
            return this == globe ? globeOpen : skyOpen;
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(label) + 34, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            boolean on = open();
            boolean live = enabled && (fixed || partnerAvailable || this != partner);

            g2.setColor(on ? Theme.SURFACE_3 : Theme.SURFACE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(on ? Theme.ACCENT : Theme.EDGE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            // A filled dot for an open ring, a hollow one for a folded one: the chip says
            // which state it is IN, not which state pressing it would reach.
            int cy = getHeight() / 2;
            g2.setColor(!live ? Theme.TEXT_DIM : (on ? Theme.ACCENT : Theme.TEXT_DIM));
            if (on) {
                g2.fillOval(9, cy - 4, 8, 8);
            } else {
                g2.drawOval(9, cy - 4, 8, 8);
            }

            g2.setFont(getFont().deriveFont(on ? Font.BOLD : Font.PLAIN));
            g2.setColor(!live ? Theme.TEXT_DIM
                : (on ? Color.WHITE : (hover ? Theme.TEXT : Theme.TEXT_DIM)));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, 23, cy + fm.getAscent() / 2 - 1);
            g2.dispose();
        }
    }
}
