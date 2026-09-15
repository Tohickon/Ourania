package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Dial;
import com.zodiacomputing.ourania.astro.Zodiac;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.HyperlinkEvent;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The dial screen: the chart folded onto a 90&deg;, 45&deg; or 22.5&deg; dial with a pointer.
 *
 * <p>Master list E: "Modulo-22.5 dial (16th harmonic) for semi-square / sesquiquadrate family
 * contacts." The harmonic selector can already draw H4 or H16, which puts the planets where a
 * dial would; what it cannot do is what a dial is for - a pointer laid across the chart, and the
 * list of every planet and midpoint standing under it. The arithmetic is {@link Dial}; this class
 * draws it and answers the pointer.
 *
 * <p><b>Click or drag anywhere on the dial to turn the pointer.</b> A click close to a planet sets
 * the pointer exactly on that planet, since "what stands on my Sun" is the question almost every
 * time. The list beside it says what is on the pointer, and every planetary picture the dial
 * holds; a picture's link turns the pointer to it.
 */
public final class DialPanel extends JPanel {

    static final String[] DIAL_NAMES = {"90° dial", "45° dial", "22.5° dial"};

    private final OuraniaWindow window;
    final JComboBox<String> dialChoice = new JComboBox<>(DIAL_NAMES);
    final JSpinner orb = new JSpinner(new SpinnerNumberModel(1.0, 0.25, 3.0, 0.25));
    final JCheckBox showMidpoints = new JCheckBox("Midpoints", true);
    final Canvas canvas = new Canvas();
    final JEditorPane list = new JEditorPane();

    private ChartFrame chart;
    List<Dial.Point> points = new ArrayList<>();
    List<Dial.Point> midpoints = new ArrayList<>();
    /** The pointer, as a place on the dial in zodiac degrees, 0 up to the modulus. */
    double pointer;

    public DialPanel(OuraniaWindow window) {
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Dial");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("Hard aspects and midpoint pictures under one pointer. "
            + "Drag to turn it; click a planet to set it there.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        controls.setOpaque(false);
        Widgets.styleCombo((JComboBox<String>) dialChoice);
        dialChoice.addActionListener(e -> refresh());
        JLabel orbLabel = new JLabel("Orb");
        orbLabel.setForeground(Theme.TEXT);
        orbLabel.setFont(Theme.BODY);
        orb.addChangeListener(e -> refresh());
        showMidpoints.setOpaque(false);
        showMidpoints.setForeground(Theme.TEXT);
        showMidpoints.setFont(Theme.BODY);
        showMidpoints.addActionListener(e -> refresh());
        controls.add(dialChoice);
        controls.add(orbLabel);
        controls.add(orb);
        controls.add(showMidpoints);

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

        list.setContentType("text/html");
        list.setEditable(false);
        list.setBackground(Theme.SURFACE);
        list.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getDescription() != null
                    && e.getDescription().startsWith("dial|")) {
                pointTo(e.getDescription().substring(5));
            }
        });
        javax.swing.JScrollPane side = HtmlPanes.scroller(list);
        side.setPreferredSize(new Dimension(360, 400));
        side.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.EDGE));

        canvas.setPreferredSize(new Dimension(520, 520));
        add(canvas, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
    }

    /** The dial's modulus in zodiac degrees. */
    double modulus() {
        return Dial.MODULI[Math.max(0, dialChoice.getSelectedIndex())];
    }

    double orbDegrees() {
        return ((Number) orb.getValue()).doubleValue();
    }

    /** Reads the chart on the wheel again; called whenever the screen is opened. */
    void refreshChart() {
        setChart(window == null ? null : window.radixChartForSearch());
    }

    void setChart(ChartFrame f) {
        this.chart = f;
        this.points = Dial.points(f);
        this.midpoints = Dial.midpoints(this.points);
        refresh();
    }

    ChartFrame chart() {
        return this.chart;
    }

    /** Turns the pointer to a named point. */
    void pointTo(String name) {
        for (Dial.Point p : this.points) {
            if (p.name.equals(name)) {
                setPointer(Dial.folded(p.lon, modulus()));
                return;
            }
        }
    }

    void setPointer(double folded) {
        this.pointer = Dial.folded(folded, modulus());
        refresh();
    }

    /** What stands under the pointer: points first, then midpoints if they are shown. */
    List<Dial.Point> onPointer() {
        List<Dial.Point> all = new ArrayList<>(this.points);
        if (showMidpoints.isSelected()) {
            all.addAll(this.midpoints);
        }
        return Dial.near(this.pointer, all, modulus(), orbDegrees());
    }

    void refresh() {
        this.pointer = Dial.folded(this.pointer, modulus());
        list.setText(listHtml());
        list.setCaretPosition(0);
        canvas.repaint();
    }

    String listHtml() {
        double m = modulus();
        StringBuilder h = new StringBuilder("<html><body style='font-family:Arial; font-size:12px; color:#d8dae2; padding:10px;'>");
        if (this.chart == null || this.points.isEmpty()) {
            h.append("<p><i>There is no chart on the wheel to lay on the dial.</i></p></body></html>");
            return h.toString();
        }
        h.append("<h3 style='color:#E2B258; margin:0 0 4px 0;'>On the pointer</h3>");
        h.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:6px;'>")
            .append(degrees(this.pointer)).append(" on the ").append(DIAL_NAMES[dialChoice.getSelectedIndex()])
            .append(", within ").append(trim(orbDegrees())).append("&deg;</div>");
        List<Dial.Point> under = onPointer();
        if (under.isEmpty()) {
            h.append("<p><i>Nothing stands on the pointer.</i></p>");
        } else {
            h.append("<table cellpadding='2'>");
            for (Dial.Point p : under) {
                h.append("<tr><td>").append(p.isMidpoint() ? "<i>" + p.name + "</i>" : "<b>" + p.name + "</b>")
                    .append("</td><td style='color:#9AA5B1;'>").append(Zodiac.format(p.lon).replace("'", "&#39;"))
                    .append("</td><td style='color:#9AA5B1;'>")
                    .append(String.format("%.2f&deg;", Dial.separation(p.lon, this.pointer, m)))
                    .append("</td></tr>");
            }
            h.append("</table>");
        }
        h.append("<h3 style='color:#7FB3FF; margin:14px 0 4px 0;'>Planetary pictures</h3>");
        h.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:6px;'>Each point with what stands on it "
            + "on this dial. Click one to turn the pointer to it.</div>");
        List<Dial.Picture> pictures = Dial.pictures(this.points,
            showMidpoints.isSelected() ? this.midpoints : new ArrayList<>(), m, orbDegrees());
        if (pictures.isEmpty()) {
            h.append("<p><i>No pictures within this orb.</i></p>");
        }
        for (Dial.Picture pic : pictures) {
            h.append("<div style='margin-bottom:4px;'><a href='dial|").append(pic.focus.name)
                .append("' style='color:#E2B258; text-decoration:none;'><b>").append(pic.focus.name).append("</b></a> = ");
            for (int i = 0; i < pic.members.size(); i++) {
                Dial.Point p = pic.members.get(i);
                if (i > 0) {
                    h.append(" &middot; ");
                }
                h.append(p.name).append(" <span style='color:#9AA5B1; font-size:10px;'>")
                    .append(String.format("%.1f", Dial.separation(p.lon, pic.focus.lon, m))).append("</span>");
            }
            h.append("</div>");
        }
        return h.append("</body></html>").toString();
    }

    private static String degrees(double d) {
        int deg = (int) Math.floor(d);
        int min = (int) Math.round((d - deg) * 60.0);
        if (min == 60) {
            deg++;
            min = 0;
        }
        return String.format("%d&deg;%02d&#39;", deg, min);
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    /** The dial, drawn. Layout functions are package-visible so the check reads the same geometry. */
    final class Canvas extends JComponent {

        Canvas() {
            MouseAdapter turn = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    turnTo(e.getX(), e.getY(), true);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    turnTo(e.getX(), e.getY(), false);
                }
            };
            addMouseListener(turn);
            addMouseMotionListener(turn);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        int cx() {
            return getWidth() / 2;
        }

        int cy() {
            return getHeight() / 2;
        }

        int radius() {
            return Math.max(40, Math.min(getWidth(), getHeight()) / 2 - 34);
        }

        /** Screen position of a dial angle (0 at the top, clockwise) at a radius. */
        java.awt.Point at(double dialAngle, double r) {
            double a = Math.toRadians(dialAngle);
            return new java.awt.Point((int) Math.round(cx() + r * Math.sin(a)), (int) Math.round(cy() - r * Math.cos(a)));
        }

        /** The dial angle under a screen point. */
        double angleAt(int x, int y) {
            double a = Math.toDegrees(Math.atan2(x - cx(), cy() - y));
            return a < 0 ? a + 360.0 : a;
        }

        /** Where each point's glyph sits: stacked inwards when two would collide. */
        int[] glyphRadii() {
            double m = modulus();
            int n = points.size();
            int[] radii = new int[n];
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }
            java.util.Arrays.sort(order, (i, j) -> Double.compare(Dial.angle(points.get(i).lon, m),
                Dial.angle(points.get(j).lon, m)));
            int base = radius() - 26;
            double lastAngle = -999;
            int depth = 0;
            for (int k = 0; k < n; k++) {
                int i = order[k];
                double a = Dial.angle(points.get(i).lon, m);
                double arcPx = Math.toRadians(a - lastAngle) * base;
                depth = arcPx < 22 ? depth + 1 : 0;
                radii[i] = base - Math.min(depth, 5) * 22;
                lastAngle = a;
            }
            return radii;
        }

        void turnTo(int x, int y, boolean snap) {
            double m = modulus();
            if (snap) {
                int[] radii = glyphRadii();
                for (int i = 0; i < points.size(); i++) {
                    java.awt.Point g = at(Dial.angle(points.get(i).lon, m), radii[i]);
                    if (g.distance(x, y) <= 13) {
                        setPointer(Dial.folded(points.get(i).lon, m));
                        return;
                    }
                }
            }
            setPointer(angleAt(x, y) / 360.0 * m);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Theme.BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            double m = modulus();
            int r = radius();
            int cx = cx();
            int cy = cy();
            g.setColor(new Color(20, 24, 31));
            g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            g.setColor(new Color(90, 100, 120));
            g.setStroke(new BasicStroke(1.4f));
            g.drawOval(cx - r, cy - r, 2 * r, 2 * r);

            // Ninety ticks a turn whatever the dial: a degree on the 90, half a degree on the 45,
            // a quarter on the 22.5. Every eighth of a turn is labelled with its dial degree.
            g.setFont(Theme.font("Segoe UI", Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            for (int t = 0; t < 90; t++) {
                double a = t * 4.0;
                boolean major = t % 5 == 0;
                java.awt.Point o = at(a, r);
                java.awt.Point i = at(a, r - (major ? 12 : 6));
                g.setColor(major ? new Color(170, 180, 200) : new Color(90, 100, 120));
                g.drawLine(o.x, o.y, i.x, i.y);
            }
            for (int e = 0; e < 8; e++) {
                double a = e * 45.0;
                String label = trim(e * m / 8.0) + "°";
                java.awt.Point p = at(a, r + 16);
                g.setColor(new Color(200, 205, 215));
                g.drawString(label, p.x - fm.stringWidth(label) / 2, p.y + (fm.getAscent() - fm.getDescent()) / 2);
            }

            List<Dial.Point> under = onPointer();
            // Midpoints as small marks on an inner ring, lit when they are on the pointer.
            if (showMidpoints.isSelected()) {
                int mr = r - 150;
                for (Dial.Point p : midpoints) {
                    java.awt.Point q = at(Dial.angle(p.lon, m), Math.max(20, mr));
                    g.setColor(under.contains(p) ? new Color(226, 178, 88) : new Color(110, 120, 145));
                    int s = under.contains(p) ? 5 : 3;
                    g.fillOval(q.x - s / 2, q.y - s / 2, s, s);
                }
            }

            // The pointer: a solid arm, and a dashed one opposite it at half a turn.
            double pa = Dial.angle(pointer, m);
            java.awt.Point tip = at(pa, r - 2);
            java.awt.Point back = at(pa + 180.0, r - 2);
            g.setColor(new Color(226, 178, 88));
            g.setStroke(new BasicStroke(2.2f));
            g.drawLine(cx, cy, tip.x, tip.y);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {6f, 5f}, 0f));
            g.drawLine(cx, cy, back.x, back.y);
            g.setStroke(new BasicStroke(1f));
            g.fillOval(cx - 4, cy - 4, 8, 8);

            int[] radii = glyphRadii();
            Font glyphFont = new Font("SansSerif", Font.PLAIN, 15);
            for (int i = 0; i < points.size(); i++) {
                Dial.Point p = points.get(i);
                java.awt.Point q = at(Dial.angle(p.lon, m), radii[i]);
                boolean lit = under.contains(p);
                g.setColor(lit ? new Color(90, 70, 30) : new Color(30, 38, 57));
                g.fillOval(q.x - 11, q.y - 11, 22, 22);
                g.setColor(lit ? new Color(226, 178, 88) : new Color(90, 110, 140));
                g.setStroke(new BasicStroke(lit ? 2f : 1f));
                g.drawOval(q.x - 11, q.y - 11, 22, 22);
                g.setStroke(new BasicStroke(1f));
                Bodies.Def d = Bodies.byName(p.name);
                String glyph = d == null || d.glyph == null || d.glyph.isEmpty()
                    ? p.name.substring(0, Math.min(2, p.name.length())) : d.glyph;
                if ("Ascendant".equals(p.name)) {
                    glyph = "AC";
                } else if ("MC".equals(p.name)) {
                    glyph = "MC";
                }
                g.setFont(glyph.codePointCount(0, glyph.length()) > 1 ? Theme.font("Segoe UI", Font.BOLD, 9) : glyphFont);
                FontMetrics gm = g.getFontMetrics();
                g.setColor(Color.WHITE);
                g.drawString(glyph, q.x - gm.stringWidth(glyph) / 2, q.y + (gm.getAscent() - gm.getDescent()) / 2);
            }
            g.dispose();
        }
    }
}
