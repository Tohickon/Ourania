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
    private final java.util.List<Chip> layerChips = new java.util.ArrayList<>();
    private final java.util.Map<Chip, SkymapPanel.Layer> layerOf =
        new java.util.HashMap<>();

    private boolean partnerOpen;
    private boolean skyOpen;
    /** True when a second chart has been entered at all; without one the ring has nothing. */
    private Chip chartA;

    public RingBar(OuraniaWindow window) {
        this.window = window;
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));
        setOpaque(false);

        // <b>Natal folds now too.</b> It was fixed on the reasoning that a chart without it
        // is not a chart - true of the data and not of the drawing, and a reader comparing two
        // partners over one sky has good reason to take the anchor out of the picture for a
        // moment. It folds what is drawn; the chart stays a natal chart.
        // <b>Named for the rows they draw.</b> "Natal", "Partner" and "Sky" were three
        // different vocabularies for the same three charts that the setup form calls Chart A,
        // Chart B and Sky, and the bottom readout called something else again. One name each,
        // everywhere, so a reader never has to work out which of three words means the wheel
        // in front of them.
        chartA = layerChip("Chart A", SkymapPanel.Layer.NATAL);
        chartA.setToolTipText("<html><b>Fold Chart A's wheel away.</b><br>"
            + "The chart is still cast from it - this hides its glyphs, so Chart B or the "
            + "sky can be read on its own for a moment.</html>");
        add(chartA);

        partner = new Chip("Chart B", () -> {
            partnerOpen = !partnerOpen;
            apply();
        });
        partner.setToolTipText("<html><b>Bloom a second person around Chart A.</b><br>"
            + "Chart B's placements arrive in their own ring; the aspects between the two "
            + "charts are what a synastry is read for.<br>"
            + "<i>Needs Chart B filled in on the setup screen.</i></html>");
        add(partner);

        sky = new Chip("Sky", () -> {
            skyOpen = !skyOpen;
            apply();
        });
        sky.setToolTipText("<html><b>Bloom the sky around whatever is already drawn.</b><br>"
            + "Over Chart A that is the transit wheel; over Chart A and Chart B it is the "
            + "third ring - the sky above both of them at once.<br>"
            + "<i>Always this moment, at the sky's own location - it has its own row on the "
            + "setup screen and borrows nobody's birth data.</i></html>");
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
            + "Every ring becomes a sphere - natal inside, a partner around it, the sky "
            + "around both - with the zodiac wrapped around all three.<br>"
            + "<i>Drag to turn, scroll to zoom.</i></html>");
        add(globe);

        // The scaffolding layers. These change nothing about the chart, only what is drawn of
        // it, so they never touch the mode - see the note on SkymapPanel.Layer.
        addLayer("Degrees", SkymapPanel.Layer.DEGREES,
            "The 360-degree scale around the outside.");
        addLayer("Signs", SkymapPanel.Layer.SIGNS,
            "The zodiac: its colour, its glyphs and its twelve boundaries.");
        addLayer("Decans", SkymapPanel.Layer.DECANS,
            "The thirty-six decans, in whichever scheme Settings has chosen.");
        addLayer("Bounds", SkymapPanel.Layer.BOUNDS,
            "The Egyptian terms - five rulers to a sign.");
        addLayer("Mansions", SkymapPanel.Layer.MANSIONS,
            "The 28 lunar mansions - the Moon's nightly stations.");
        addLayer("Houses", SkymapPanel.Layer.HOUSES,
            "The twelve cusps and their numbers.");
        addLayer("Aspects", SkymapPanel.Layer.ASPECTS,
            "Every line between bodies in aspect.");
    }

    private void addLayer(String label, SkymapPanel.Layer layer, String what) {
        Chip c = layerChip(label, layer);
        c.setToolTipText("<html><b>" + label + "</b><br>" + what
            + "<br><i>Folds away without changing the chart.</i></html>");
        add(c);
    }

    /** A chip that folds one drawn layer. Its state lives on the panel, not here. */
    private Chip layerChip(String label, SkymapPanel.Layer layer) {
        Chip c = new Chip(label, null);
        c.layer = layer;
        c.onLayerClick = () -> {
            if (window != null) {
                window.toggleLayer(layer);
            }
            repaintChips();
        };
        layerChips.add(c);
        layerOf.put(c, layer);
        return c;
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
    public void syncFrom(ChartMode mode, boolean transits, boolean hasPartnerData,
            boolean hasChartA) {
        this.partnerOpen = mode == ChartMode.SYNASTRY;
        this.skyOpen = mode == ChartMode.TRANSIT
            || (transits && (mode == ChartMode.SYNASTRY
                || mode == ChartMode.COMPOSITE_MIDPOINT
                || mode == ChartMode.COMPOSITE_DAVISON));
        partner.available = hasPartnerData;
        // <b>A chip for a chart nobody has entered does nothing, so it says so.</b> David:
        // "If Chart A has no info loaded into it from Chart setup then the button shouldn't
        // work, Chart b should be chart b same principle." A control that looks pressable and
        // changes nothing is the Step-dropdown defect this project keeps logging.
        chartA.available = hasChartA;
        chartA.setToolTipText(hasChartA
            ? "<html><b>Fold Chart A's wheel away.</b><br>"
                + "The chart is still cast from it - this hides its glyphs, so Chart B or the "
                + "sky can be read on its own for a moment.</html>"
            : "<html><b>There is no Chart A yet.</b><br>"
                + "Enter a birth date, time and place on the Chart Setup screen and press "
                + "Generate. Until then the wheel shows the sky.</html>");
        // A composite is one derived wheel rather than two people side by side, so the
        // partner ring is not a thing that can be opened or folded there.
        boolean composite = mode == ChartMode.COMPOSITE_MIDPOINT
            || mode == ChartMode.COMPOSITE_DAVISON;
        partner.enabled = !composite;
        partner.label = composite ? "In composite" : "Chart B";
        repaintChips();
    }

    private void apply() {
        if (window != null) {
            window.setRings(partnerOpen, skyOpen);
        }
        repaintChips();
    }

    private void repaintChips() {
        chartA.repaint();
        partner.repaint();
        sky.repaint();
        globe.repaint();
        for (Chip c : layerChips) {
            c.repaint();
        }
    }

    /** One ring, as a chip that reads as open or folded. */
    private final class Chip extends JComponent {

        String label;
        boolean fixed;
        boolean enabled = true;

        /**
         * Whether the chart this chip names exists at all.
         *
         * <b>Separate from {@code enabled}, which is about the mode.</b> The partner chip is
         * disabled in a composite because a composite has no second wheel to fold; it is
         * unavailable when nobody has entered a Chart B. Those are different sentences and a
         * reader deserves the right one - "not here" rather than "not now".
         */
        boolean available = true;
        /** Set on a chip that folds a drawn layer rather than opening a ring. */
        SkymapPanel.Layer layer;
        Runnable onLayerClick;
        private final Runnable onClick;
        private boolean hover;

        Chip(String label, Runnable onClick) {
            this.label = label;
            this.onClick = onClick;
            setFont(Theme.SMALL);
            if (onClick == null) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (onLayerClick != null) {
                            onLayerClick.run();
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
            if (layer != null) {
                return window == null || window.isLayerOpen(layer);
            }
            if (fixed) {
                return true;
            }
            if (this == partner) {
                return partnerOpen && partner.available;
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
            boolean live = enabled && available;

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
