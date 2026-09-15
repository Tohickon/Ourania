package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Horizon;
import de.thmac.swisseph.SwissEph;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The Sky View: the real sky over the chart's place, as a dome overhead or a horizon panorama.
 *
 * <p>Master list E: "'Grid Skymap' menu entry shows the 2D wheel - a real horizon / celestial-sphere
 * view does not exist." The menu row that said Grid Skymap opened the chart wheel, so it is called
 * Chart Wheel now, and this is the view the old name promised. The arithmetic is {@link Horizon}.
 *
 * <ul>
 * <li><b>Dome</b>: the whole sky above the horizon as a circle - the zenith in the middle, the
 * horizon at the rim, north at the top and east on the left, the way a star map held overhead
 * reads.</li>
 * <li><b>Horizon</b>: all 360 degrees unrolled, north at both ends, east, south and west between,
 * with the ground shaded below the horizon so a planet that has set is still shown, dimmed.</li>
 * </ul>
 *
 * <p>The ecliptic is drawn across both, with the chart's Ascendant, MC, Descendant and IC marked
 * on it: the Ascendant is where the ecliptic meets the eastern horizon, which is the relationship
 * between the chart and the sky that the wheel cannot show. The moment is the sky's, or Chart A's
 * birth sky; a slider moves it up to twelve hours either way to watch the sky turn.
 */
public final class SkyViewPanel extends JPanel {

    static final String DOME = "Dome";
    static final String HORIZON = "Horizon";
    static final String SKY = "The sky";
    static final String CHART_A = "Chart A's birth sky";

    private final OuraniaWindow window;
    final JComboBox<String> view = new JComboBox<>(new String[] {DOME, HORIZON});
    final JComboBox<String> source = new JComboBox<>(new String[] {SKY, CHART_A});
    final JSlider offset = new JSlider(-720, 720, 0);
    final JLabel when = new JLabel(" ");
    final Canvas canvas = new Canvas();
    final JEditorPane table = new JEditorPane();
    private final SwissEph sw;

    /** The moment and place drawn, {jdUt, lat, lon}, before the slider's offset. */
    double[] moment = {new de.thmac.swisseph.SweDate().getJulDay(), 51.4779, 0.0};
    List<Horizon.Place> bodies = new ArrayList<>();
    List<Horizon.Place> angles = new ArrayList<>();
    List<double[]> ecliptic = new ArrayList<>();

    public SkyViewPanel(OuraniaWindow window) {
        this.window = window;
        this.sw = new SwissEph(Ephemeris.PATH);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Sky View");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("The real sky over the chart's place: each planet's height and direction.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        controls.setOpaque(false);
        Widgets.styleCombo(view);
        Widgets.styleCombo(source);
        view.addActionListener(e -> canvas.repaint());
        source.addActionListener(e -> refreshChart());
        offset.setOpaque(false);
        offset.setPreferredSize(new Dimension(240, 26));
        offset.setToolTipText("Move the sky up to twelve hours either way");
        offset.addChangeListener(e -> recompute());
        JButton reset = new JButton("Reset time");
        Widgets.styleButton(reset, Widgets.Role.TRANSPORT);
        reset.addActionListener(e -> offset.setValue(0));
        when.setForeground(Theme.TEXT);
        when.setFont(Theme.BODY);
        controls.add(view);
        controls.add(source);
        JLabel hours = new JLabel("±12 h");
        hours.setForeground(Theme.TEXT_DIM);
        hours.setFont(Theme.SMALL);
        controls.add(hours);
        controls.add(offset);
        controls.add(reset);
        controls.add(when);

        JPanel head = new JPanel();
        head.setLayout(new javax.swing.BoxLayout(head, javax.swing.BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.setBorder(Theme.pad(Theme.GAP_L, Theme.GAP_L, Theme.GAP, Theme.GAP_L));
        title.setAlignmentX(LEFT_ALIGNMENT);
        lede.setAlignmentX(LEFT_ALIGNMENT);
        controls.setAlignmentX(LEFT_ALIGNMENT);
        head.add(title);
        head.add(lede);
        head.add(controls);
        add(head, BorderLayout.NORTH);

        table.setContentType("text/html");
        table.setEditable(false);
        table.setBackground(Theme.SURFACE);
        javax.swing.JScrollPane side = HtmlPanes.scroller(table);
        side.setPreferredSize(new Dimension(300, 400));
        side.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.EDGE));
        add(canvas, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(canvas);
    }

    /** Reads the moment and place from the wheel again; called whenever the screen is opened. */
    void refreshChart() {
        SkymapPanel sky = window == null ? null : window.skymapForViews();
        if (sky != null) {
            boolean a = CHART_A.equals(source.getSelectedItem()) && sky.innerIsBirthChart();
            setMoment(sky.skyMoment(a));
        } else {
            recompute();
        }
    }

    void setMoment(double[] m) {
        this.moment = m.clone();
        recompute();
    }

    /** The moment drawn, with the slider's offset. */
    double jd() {
        return this.moment[0] + offset.getValue() / 1440.0;
    }

    void recompute() {
        double jd = jd();
        double lat = this.moment[1];
        double lon = this.moment[2];
        this.bodies = Horizon.bodies(sw, jd, lat, lon);
        this.angles = Horizon.angleplaces(sw, jd, lat, lon);
        this.ecliptic = Horizon.ecliptic(sw, jd, lat, lon, 2.0);
        long millis = Math.round((jd - 2440587.5) * 86400000.0);
        int off = offset.getValue();
        when.setText(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm 'UT'").format(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC))
            + String.format("   %.2f, %.2f", lat, lon)
            + (off == 0 ? "" : String.format("   (%s%dh %02dm)", off < 0 ? "−" : "+", Math.abs(off) / 60, Math.abs(off) % 60)));
        table.setText(tableHtml());
        table.setCaretPosition(0);
        canvas.repaint();
    }

    String tableHtml() {
        StringBuilder h = new StringBuilder("<html><body style='font-family:Arial; font-size:12px; color:#d8dae2; padding:10px;'>");
        h.append("<h3 style='color:#7FB3FF; margin:0 0 6px 0;'>Above the horizon</h3><table cellpadding='2'>");
        appendRows(h, true);
        h.append("</table><h3 style='color:#9AA5B1; margin:12px 0 6px 0;'>Below it</h3><table cellpadding='2'>");
        appendRows(h, false);
        h.append("</table><div style='color:#9AA5B1; font-size:11px; margin-top:10px;'>Altitude as seen, with "
            + "refraction; direction is a compass bearing. The Ascendant is where the ecliptic crosses the "
            + "eastern horizon.</div></body></html>");
        return h.toString();
    }

    private void appendRows(StringBuilder h, boolean up) {
        List<Horizon.Place> all = new ArrayList<>(this.bodies);
        all.sort((a, b) -> Double.compare(b.apparent, a.apparent));
        for (Horizon.Place p : all) {
            if (p.visible() != up) {
                continue;
            }
            h.append("<tr><td><b>").append(p.name).append("</b></td><td>")
                .append(String.format("%.1f&deg;", p.apparent)).append("</td><td>")
                .append(Horizon.wind(p.azimuth)).append(String.format(" %.0f&deg;", p.azimuth))
                .append("</td><td style='color:#9AA5B1;'>").append(p.climbing ? "rising" : "sinking").append("</td></tr>");
        }
    }

    /** The sky, drawn. Its projections are package-visible so the check reads the same geometry. */
    final class Canvas extends JComponent {

        Canvas() {
            setPreferredSize(new Dimension(620, 520));
        }

        boolean dome() {
            return DOME.equals(view.getSelectedItem());
        }

        int cx() {
            return getWidth() / 2;
        }

        int cy() {
            return getHeight() / 2;
        }

        int radius() {
            return Math.max(40, Math.min(getWidth(), getHeight()) / 2 - 30);
        }

        /**
         * Screen position on the dome: the zenith at the centre, the horizon at the rim, north up
         * and east on the left - a sky seen from underneath. Azimuthal equidistant, so altitude is
         * even from rim to centre.
         */
        Point2D.Double onDome(double azimuth, double altitude) {
            double r = radius() * (90.0 - altitude) / 90.0;
            double a = Math.toRadians(azimuth);
            return new Point2D.Double(cx() - r * Math.sin(a), cy() - r * Math.cos(a));
        }

        /** The horizon panorama's top and bottom altitudes. */
        static final double TOP_ALT = 90.0;
        static final double BOTTOM_ALT = -30.0;

        /** Screen position on the panorama: north at the left edge, bearing increasing to the right. */
        Point2D.Double onStrip(double azimuth, double altitude) {
            int left = 30;
            int right = getWidth() - 20;
            int top = 30;
            int bottom = getHeight() - 30;
            double x = left + (right - left) * (azimuth / 360.0);
            double y = top + (bottom - top) * (TOP_ALT - altitude) / (TOP_ALT - BOTTOM_ALT);
            return new Point2D.Double(x, y);
        }

        Point2D.Double at(double azimuth, double altitude) {
            return dome() ? onDome(azimuth, altitude) : onStrip(azimuth, altitude);
        }

        /** Whether a place is drawn in this view: the dome shows only what is above the horizon. */
        boolean drawn(Horizon.Place p) {
            return !dome() || p.apparent >= 0.0;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            Horizon.Place p = placeAt(e.getX(), e.getY());
            if (p == null) {
                return null;
            }
            return String.format("<html><b>%s</b><br>%.1f&deg; high, %s %.0f&deg;%s</html>", p.name, p.apparent,
                Horizon.wind(p.azimuth), p.azimuth, bodies.contains(p) ? (p.climbing ? ", rising" : ", sinking") : "");
        }

        /** The body or angle drawn under a point, or null. */
        Horizon.Place placeAt(int x, int y) {
            Horizon.Place best = null;
            double bestD = 13.0;
            List<Horizon.Place> all = new ArrayList<>(bodies);
            all.addAll(angles);
            for (Horizon.Place p : all) {
                if (!drawn(p)) {
                    continue;
                }
                double d = at(p.azimuth, p.apparent).distance(x, y);
                if (d <= bestD) {
                    bestD = d;
                    best = p;
                }
            }
            return best;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Theme.BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (dome()) {
                paintDome(g);
            } else {
                paintStrip(g);
            }
            paintEcliptic(g);
            paintAngles(g);
            paintBodies(g);
            g.dispose();
        }

        private void paintDome(Graphics2D g) {
            int r = radius();
            g.setColor(new Color(14, 22, 40));
            g.fillOval(cx() - r, cy() - r, 2 * r, 2 * r);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(60, 72, 96));
            for (int alt : new int[] {30, 60}) {
                int rr = (int) Math.round(r * (90.0 - alt) / 90.0);
                g.drawOval(cx() - rr, cy() - rr, 2 * rr, 2 * rr);
            }
            for (int az = 0; az < 360; az += 45) {
                Point2D.Double p = onDome(az, 0);
                g.drawLine(cx(), cy(), (int) p.x, (int) p.y);
            }
            g.setColor(new Color(150, 165, 190));
            g.setStroke(new BasicStroke(1.6f));
            g.drawOval(cx() - r, cy() - r, 2 * r, 2 * r);
            g.setFont(Theme.font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            String[] names = {"N", "E", "S", "W"};
            for (int k = 0; k < 4; k++) {
                Point2D.Double p = onDome(k * 90.0, -8.0);
                g.drawString(names[k], (int) p.x - fm.stringWidth(names[k]) / 2, (int) p.y + (fm.getAscent() - fm.getDescent()) / 2);
            }
            g.setFont(Theme.font("Segoe UI", Font.PLAIN, 10));
            g.setColor(new Color(110, 125, 150));
            g.drawString("60°", cx() + 3, (int) onDome(180, 60).y - 3);
            g.drawString("30°", cx() + 3, (int) onDome(180, 30).y - 3);
        }

        private void paintStrip(Graphics2D g) {
            Point2D.Double topLeft = onStrip(0, TOP_ALT);
            Point2D.Double horizonLeft = onStrip(0, 0);
            Point2D.Double bottomRight = onStrip(360, BOTTOM_ALT);
            int x0 = (int) topLeft.x;
            int x1 = (int) bottomRight.x;
            g.setColor(new Color(14, 22, 40));
            g.fillRect(x0, (int) topLeft.y, x1 - x0, (int) (horizonLeft.y - topLeft.y));
            g.setColor(new Color(34, 30, 26));
            g.fillRect(x0, (int) horizonLeft.y, x1 - x0, (int) (bottomRight.y - horizonLeft.y));
            g.setColor(new Color(60, 72, 96));
            g.setFont(Theme.font("Segoe UI", Font.PLAIN, 10));
            for (int alt : new int[] {30, 60}) {
                int y = (int) onStrip(0, alt).y;
                g.drawLine(x0, y, x1, y);
                g.drawString(alt + "°", 4, y + 4);
            }
            for (int az = 0; az <= 360; az += 45) {
                int x = (int) onStrip(az, 0).x;
                g.drawLine(x, (int) topLeft.y, x, (int) bottomRight.y);
            }
            g.setColor(new Color(170, 150, 110));
            g.setStroke(new BasicStroke(1.8f));
            g.drawLine(x0, (int) horizonLeft.y, x1, (int) horizonLeft.y);
            g.setStroke(new BasicStroke(1f));
            g.setFont(Theme.font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
            g.setColor(new Color(200, 205, 215));
            for (int k = 0; k < names.length; k++) {
                int x = (int) onStrip(k * 45.0, 0).x;
                g.drawString(names[k], x - fm.stringWidth(names[k]) / 2, (int) bottomRight.y + 18);
            }
        }

        private void paintEcliptic(Graphics2D g) {
            g.setColor(new Color(226, 178, 88, 170));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {7f, 5f}, 0f));
            for (int i = 0; i < ecliptic.size(); i++) {
                double[] a = ecliptic.get(i);
                double[] b = ecliptic.get((i + 1) % ecliptic.size());
                if (dome() && (a[1] < 0 || b[1] < 0)) {
                    continue;
                }
                Point2D.Double p = at(a[0], a[1]);
                Point2D.Double q = at(b[0], b[1]);
                // On the panorama, a segment that crosses north jumps from one edge to the other.
                if (!dome() && Math.abs(p.x - q.x) > getWidth() / 2.0) {
                    continue;
                }
                if (!dome() && (a[1] < Canvas.BOTTOM_ALT || b[1] < Canvas.BOTTOM_ALT)) {
                    continue;
                }
                g.drawLine((int) p.x, (int) p.y, (int) q.x, (int) q.y);
            }
            g.setStroke(new BasicStroke(1f));
        }

        private void paintAngles(Graphics2D g) {
            g.setFont(Theme.font("Segoe UI", Font.BOLD, 10));
            for (Horizon.Place p : angles) {
                if (!drawn(p) || (!dome() && p.apparent < BOTTOM_ALT)) {
                    continue;
                }
                Point2D.Double q = at(p.azimuth, p.apparent);
                String label = "Ascendant".equals(p.name) ? "ASC" : "Descendant".equals(p.name) ? "DSC" : p.name;
                g.setColor(new Color(226, 178, 88));
                g.fillRect((int) q.x - 3, (int) q.y - 3, 7, 7);
                g.drawString(label, (int) q.x + 6, (int) q.y - 5);
            }
        }

        private void paintBodies(Graphics2D g) {
            Font glyphFont = new Font("SansSerif", Font.PLAIN, 15);
            for (Horizon.Place p : bodies) {
                if (!drawn(p) || (!dome() && p.apparent < BOTTOM_ALT)) {
                    continue;
                }
                Point2D.Double q = at(p.azimuth, p.apparent);
                boolean down = p.apparent < 0.0;
                boolean light = "Sun".equals(p.name) || "Moon".equals(p.name);
                int s = light ? 13 : 10;
                g.setColor(down ? new Color(40, 40, 46) : "Sun".equals(p.name) ? new Color(120, 90, 20) : new Color(30, 38, 57));
                g.fillOval((int) q.x - s, (int) q.y - s, 2 * s, 2 * s);
                g.setColor(down ? new Color(90, 90, 100) : light ? new Color(240, 210, 120) : new Color(127, 179, 255));
                g.setStroke(new BasicStroke(1.4f));
                g.drawOval((int) q.x - s, (int) q.y - s, 2 * s, 2 * s);
                g.setStroke(new BasicStroke(1f));
                Bodies.Def d = Bodies.byName(p.name);
                String glyph = d == null ? p.name.substring(0, 1) : d.glyph;
                g.setFont(glyphFont);
                FontMetrics fm = g.getFontMetrics();
                g.setColor(down ? new Color(140, 140, 150) : Color.WHITE);
                g.drawString(glyph, (int) q.x - fm.stringWidth(glyph) / 2, (int) q.y + (fm.getAscent() - fm.getDescent()) / 2);
            }
        }
    }
}
