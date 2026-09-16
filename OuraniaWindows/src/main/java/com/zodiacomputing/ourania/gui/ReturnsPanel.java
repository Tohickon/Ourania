package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Precession;
import com.zodiacomputing.ourania.astro.Returns;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns as a chart you can look at: master list F5.
 *
 * <p>The engine has computed solar, lunar and planetary returns since 2026-08-10, and every one of
 * them went straight into {@link com.zodiacomputing.ourania.astro.Convergence} as a witness. A
 * return chart was therefore something the app <i>counted</i> and never <i>showed</i> - and what a
 * return adds over a transit list is its angles, which are precisely what a count throws away.
 *
 * <p>This screen adds the two things the checklist names as missing beside the mode itself:
 *
 * <ul>
 * <li><b>Relocated</b> - a return cast where the native actually is, rather than at the birthplace.
 * The engine always took a latitude and longitude; nothing had ever passed anything but the birth
 * ones. Only the angles and houses move, and they are the whole of the technique, so a relocated
 * return is a different reading rather than the same one from a different chair.</li>
 * <li><b>Precessed</b> - the return taken to the body's place against the stars rather than to its
 * tropical degree, about twenty minutes later per year of age. A toggle, tropical by default
 * (David, 2026-09-15), and the reading says which it used.</li>
 * </ul>
 *
 * <p><b>And it opens on the wheel.</b> A return is a chart for a moment at a place, which is exactly
 * what the wheel's sky ring already draws, so "Show on the wheel" hands the moment and the place to
 * that ring rather than teaching the wheel a new mode. The alternative - a RETURN chart mode - would
 * mean editing the decompiled wheel and the setup form, where most of this project's regressions
 * have come from, to reach a picture it can already draw.
 */
public final class ReturnsPanel extends JPanel {

    private final OuraniaWindow window;
    private final SwissEph sw;

    final JComboBox<String> kind = new JComboBox<>();
    final JSpinner fromYear = new JSpinner(new SpinnerNumberModel(java.time.Year.now().getValue(), 1800, 2399, 1));
    final JSpinner toYear = new JSpinner(new SpinnerNumberModel(java.time.Year.now().getValue() + 1, 1800, 2399, 1));
    final JComboBox<String> where = new JComboBox<>(new String[] {
        "Birthplace", "Where the sky is set", "Somewhere else"});
    final JTextField place = new JTextField(16);
    final JCheckBox precessed = new JCheckBox("Precessed");
    final JButton find = new JButton("Find");
    final JButton toWheel = new JButton("Show on the wheel");
    final JLabel status = new JLabel(" ");

    final DefaultListModel<Returns.Return> model = new DefaultListModel<>();
    final JList<Returns.Return> list = new JList<>(model);
    final JEditorPane detail = new JEditorPane();

    private ChartFrame chart;
    private double natalJd = Double.NaN;
    private SwingWorker<List<Returns.Return>, Void> running;

    public ReturnsPanel(OuraniaWindow window) {
        this.window = window;
        this.sw = new SwissEph(Ephemeris.PATH);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Returns");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("The chart for the moment a body comes back to where it was at birth.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);
        lede.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        kind.addItem("Solar");
        kind.addItem("Lunar");
        List<String> bodies = new ArrayList<>(Returns.returnableBodies());
        java.util.Collections.sort(bodies);
        for (String b : bodies) {
            if (!"Sun".equals(b) && !"Moon".equals(b)) {
                kind.addItem(b);
            }
        }
        // A year is not a quantity: without this the spinners read "2,026".
        fromYear.setEditor(new JSpinner.NumberEditor(fromYear, "#"));
        toYear.setEditor(new JSpinner.NumberEditor(toYear, "#"));
        Widgets.styleCombo(kind);
        Widgets.styleCombo(where);
        for (JButton b : new JButton[] {find, toWheel}) {
            Widgets.styleButton(b, Widgets.Role.TRANSPORT);
        }
        toWheel.setEnabled(false);
        toWheel.setToolTipText("Draw this return around the chart, on the wheel's outer ring");
        precessed.setForeground(Theme.TEXT_DIM);
        precessed.setFont(Theme.SMALL);
        precessed.setOpaque(false);
        precessed.setToolTipText("<html><b>Take the return to the body's place against the stars.</b>"
            + "<br>About 50 arcseconds a year of precession, which is roughly twenty minutes of "
            + "clock for each year of age.<br>Off is the ordinary tropical return.</html>");
        place.setToolTipText("Where to cast the return - only the angles and houses move");
        PlaceField.attach(place);
        place.setEnabled(false);
        where.addActionListener(e -> place.setEnabled(where.getSelectedIndex() == 2));
        kind.addActionListener(e -> syncYears());
        find.addActionListener(e -> search());
        toWheel.addActionListener(e -> sendToWheel());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetail(list.getSelectedValue());
            }
        });
        list.setBackground(Theme.SURFACE);
        list.setForeground(Theme.TEXT);
        list.setFont(Theme.SMALL);
        // A return reads as its date and its rising degree: the date is how it is chosen, and the
        // Ascendant is the part of it that is new information.
        list.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> l, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(l, value, index, selected, focus);
                if (value instanceof Returns.Return) {
                    Returns.Return r = (Returns.Return) value;
                    setText(moment(r.jd) + "  -  " + (r.chart == null ? "?"
                        : Zodiac.format(r.chart.asc) + " rising"));
                }
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return this;
            }
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(label("Return of"));
        row.add(kind);
        row.add(label("from"));
        row.add(fromYear);
        row.add(label("to"));
        row.add(toYear);
        row.add(label("cast at"));
        row.add(where);
        row.add(place);
        row.add(precessed);
        row.add(find);
        row.add(toWheel);

        JPanel head = new JPanel();
        head.setLayout(new javax.swing.BoxLayout(head, javax.swing.BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.setBorder(Theme.pad(Theme.GAP_L, Theme.GAP_L, Theme.GAP, Theme.GAP_L));
        title.setAlignmentX(LEFT_ALIGNMENT);
        lede.setAlignmentX(LEFT_ALIGNMENT);
        row.setAlignmentX(LEFT_ALIGNMENT);
        status.setForeground(Theme.TEXT_DIM);
        status.setFont(Theme.SMALL);
        status.setAlignmentX(LEFT_ALIGNMENT);
        head.add(title);
        head.add(lede);
        head.add(row);
        head.add(Box.createRigidArea(new Dimension(0, 4)));
        head.add(status);
        add(head, BorderLayout.NORTH);

        javax.swing.JScrollPane left = new javax.swing.JScrollPane(list);
        left.getViewport().setBackground(Theme.SURFACE);
        left.setPreferredSize(new Dimension(260, 400));
        left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.EDGE));
        add(left, BorderLayout.WEST);

        detail.setContentType("text/html");
        detail.setEditable(false);
        detail.setBackground(Theme.SURFACE);
        add(HtmlPanes.scroller(detail), BorderLayout.CENTER);

        syncYears();
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.TEXT_DIM);
        l.setFont(Theme.SMALL);
        // A label is laid out at exactly its own measured width, with nothing to spare, so a
        // context that renders the same string a hair wider loses its last letter - "Return of"
        // came out "Return o". A few pixels of room costs nothing.
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        return l;
    }

    /** A solar return is one chart a year, so its window is one year unless the reader widens it. */
    private void syncYears() {
        boolean solar = "Solar".equals(kind.getSelectedItem());
        toYear.setEnabled(!solar || true);
        if (solar && ((Number) toYear.getValue()).intValue() < ((Number) fromYear.getValue()).intValue()) {
            toYear.setValue(fromYear.getValue());
        }
    }

    /** Reads Chart A from the wheel again; called whenever the screen is opened. */
    void refreshChart() {
        ChartFrame c = window == null || !window.wheelHasChartA() ? null : window.radixChartForSearch();
        setChart(c);
    }

    void setChart(ChartFrame c) {
        this.chart = c;
        this.natalJd = c == null ? Double.NaN : c.julianDayUt;
        model.clear();
        toWheel.setEnabled(false);
        if (c == null) {
            status.setText(" ");
            detail.setText("<html><body style='font-family:Arial; color:#d8dae2; padding:10px;'><i>There is no "
                + "Chart A on the wheel. Cast one in Chart Setup to see its returns.</i></body></html>");
        } else {
            detail.setText(blank());
        }
    }

    private String blank() {
        return "<html><body style='font-family:Arial; color:#d8dae2; padding:10px;'><i>Press Find, "
            + "then choose a return to read it.</i></body></html>";
    }

    /** Where this return is being cast, as [lat, lon, name]. */
    Object[] placeFor() {
        if (chart == null) {
            return null;
        }
        int which = where.getSelectedIndex();
        if (which == 1 && window != null && window.skymapForViews() != null) {
            double[] m = window.skymapForViews().skyMoment(false);
            return new Object[] {m[1], m[2], "where the sky is set"};
        }
        if (which == 2) {
            Atlas.Place p = Atlas.resolve(place.getText());
            if (p != null) {
                return new Object[] {p.latitude, p.longitude, p.name};
            }
            return null;
        }
        return new Object[] {chart.geoLat, chart.geoLon, "the birthplace"};
    }

    /** One computation at a time, and never on the event thread. */
    void search() {
        if (chart == null) {
            return;
        }
        final Object[] at = placeFor();
        if (at == null) {
            status.setText("That place is not in the atlas. Try another spelling.");
            return;
        }
        if (running != null) {
            // Cancelled without interrupting: an interrupt during an ephemeris read closes the
            // file channel for every later call. Same rule as the Transit Calendar.
            running.cancel(false);
        }
        final String which = String.valueOf(kind.getSelectedItem());
        final int y0 = ((Number) fromYear.getValue()).intValue();
        final int y1 = Math.max(y0, ((Number) toYear.getValue()).intValue());
        final boolean pre = precessed.isSelected();
        final double lat = (Double) at[0];
        final double lon = (Double) at[1];
        final String placeName = String.valueOf(at[2]);
        status.setText("Working out " + which.toLowerCase() + " returns...");
        model.clear();
        toWheel.setEnabled(false);

        running = new SwingWorker<List<Returns.Return>, Void>() {
            @Override
            protected List<Returns.Return> doInBackground() {
                return compute(which, y0, y1, lat, lon, pre);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                List<Returns.Return> found;
                try {
                    found = get();
                } catch (Exception e) {
                    status.setText("Could not work out the returns: " + e.getMessage());
                    return;
                }
                for (Returns.Return r : found) {
                    model.addElement(r);
                }
                status.setText(found.size() + (found.size() == 1 ? " return" : " returns")
                    + ", cast at " + placeName
                    + (pre ? ", precessed" : ", tropical")
                    + (found.isEmpty() ? " - widen the years" : ""));
                if (!found.isEmpty()) {
                    list.setSelectedIndex(0);
                }
            }
        };
        running.execute();
    }

    /**
     * The returns themselves.
     *
     * <b>Synchronised on the ephemeris.</b> The Swiss Ephemeris port is not thread-safe even
     * across separate instances - SweDate holds a static one - so a worker here and a worker
     * anywhere else must not be inside it at once.
     */
    List<Returns.Return> compute(String which, int y0, int y1, double lat, double lon, boolean pre) {
        synchronized (sw) {
            List<Returns.Return> out = new ArrayList<>();
            if (chart == null) {
                return out;
            }
            double from = new SweDate(y0, 1, 1, 0.0).getJulDay();
            double to = new SweDate(y1 + 1, 1, 1, 0.0).getJulDay();
            if ("Solar".equals(which)) {
                ChartFrame.Body sun = chart.body("Sun");
                if (sun == null || !sun.ok) {
                    return out;
                }
                int birthYear = new SweDate(natalJd).getYear();
                for (int y = y0; y <= y1; y++) {
                    Returns.Return r = Returns.solar(sw, natalJd, sun.lon, y - birthYear,
                        lat, lon, chart.hsys, pre);
                    if (r != null && r.jd >= from - 400 && r.jd < to + 400) {
                        r.relocated = relocated(r);
                        out.add(r);
                    }
                }
                return out;
            }
            if ("Lunar".equals(which)) {
                ChartFrame.Body moon = chart.body("Moon");
                if (moon == null || !moon.ok) {
                    return out;
                }
                for (Returns.Return r : Returns.lunar(sw, natalJd, moon.lon, from, to, lat, lon,
                        chart.hsys, pre)) {
                    r.relocated = relocated(r);
                    out.add(r);
                }
                return out;
            }
            ChartFrame.Body b = chart.body(which);
            if (b == null || !b.ok) {
                return out;
            }
            for (Returns.Return r : Returns.planetary(sw, which, b.lon, from, to, lat, lon,
                    chart.hsys, pre, natalJd)) {
                r.relocated = relocated(r);
                out.add(r);
            }
            return out;
        }
    }

    /** A return is relocated when it was not cast within a kilometre or so of the birthplace. */
    private boolean relocated(Returns.Return r) {
        return chart != null
            && (Math.abs(r.lat - chart.geoLat) > 0.01 || Math.abs(r.lon - chart.geoLon) > 0.01);
    }

    void showDetail(Returns.Return r) {
        toWheel.setEnabled(r != null);
        if (r == null || r.chart == null || chart == null) {
            detail.setText(blank());
            return;
        }
        StringBuilder h = new StringBuilder("<html><body style='font-family:Arial; font-size:12px; "
            + "color:#d8dae2; padding:10px;'>");
        h.append("<h2 style='color:#E2B258; margin:0 0 4px 0;'>").append(cap(r.kind))
         .append(" return</h2>");
        h.append("<div style='color:#94A0B4;'>").append(moment(r.jd)).append(" UT, ")
         .append(String.format("%.2f, %.2f", r.lat, r.lon)).append("</div>");
        h.append("<p>").append(r.precessed
            ? "Precessed: taken to the " + r.body + "'s place against the stars, "
              + trim(r.precessionDegrees) + "&deg; on from its natal degree."
            : "Tropical: taken to the " + r.body + "'s natal degree.");
        if (r.relocated) {
            h.append(" Cast away from the birthplace, so the angles and houses are the relocated "
                + "ones and the planets are not.");
        }
        h.append("</p>");

        // The angles: the whole of what a return adds over a transit list.
        h.append("<h3 style='color:#7FB3FF;'>The angles</h3><table cellpadding='3'>");
        String[] names = {"Ascendant", "MC", "Descendant", "IC"};
        double[] lons = {r.chart.asc, r.chart.mc, r.chart.dsc, r.chart.ic};
        double[] natalLons = {chart.asc, chart.mc, chart.dsc, chart.ic};
        for (int i = 0; i < names.length; i++) {
            h.append("<tr><td><b>").append(names[i]).append("</b></td><td>")
             .append(Zodiac.format(lons[i])).append("</td><td style='color:#94A0B4;'>natal ")
             .append(Zodiac.format(natalLons[i])).append("</td></tr>");
        }
        h.append("</table>");

        // Where the return's own bodies stand.
        h.append("<h3 style='color:#7FB3FF;'>The return chart</h3><table cellpadding='3'>");
        for (ChartFrame.Body b : r.chart.bodies) {
            if (b == null || !b.ok) {
                continue;
            }
            h.append("<tr><td><b>").append(b.name).append("</b></td><td>")
             .append(Zodiac.format(b.lon)).append(b.retrograde ? " R" : "").append("</td></tr>");
        }
        h.append("</table>");

        // What it lands on, by the same natal-end filter every other technique uses.
        List<Returns.Contact> cs = Returns.contacts(r, chart, null, null);
        h.append("<h3 style='color:#7FB3FF;'>On the natal chart</h3>");
        if (cs.isEmpty()) {
            h.append("<p><i>Nothing of this return falls on a significant natal point.</i></p>");
        } else {
            h.append("<table cellpadding='3'>");
            for (Returns.Contact c : cs) {
                h.append("<tr><td><b>").append(c.returnPoint).append("</b></td><td>")
                 .append(label(c.type)).append("</td><td>natal ").append(c.natal)
                 .append("</td><td style='color:#94A0B4;'>").append(trim(c.offBy))
                 .append("&deg; off, ").append(c.why).append("</td></tr>");
            }
            h.append("</table>");
        }
        h.append("</body></html>");
        detail.setText(h.toString());
        detail.setCaretPosition(0);
    }

    /** Hands the moment and the place to the wheel's sky ring, which already draws exactly this. */
    void sendToWheel() {
        Returns.Return r = list.getSelectedValue();
        if (r == null || window == null) {
            return;
        }
        window.showReturnOnWheel(r);
    }

    private static String label(Aspects.Type t) {
        return t == null ? "&mdash;" : t.label;
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String trim(double d) {
        String s = String.format("%.2f", d);
        return s.endsWith(".00") ? s.substring(0, s.length() - 3) : s;
    }

    /** The moment of a Julian day, in UT, for a label. */
    static String moment(double jd) {
        ZonedDateTime when = Instant.ofEpochMilli(Math.round((jd - 2440587.5) * 86400000.0))
            .atZone(ZoneOffset.UTC);
        return when.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"));
    }
}
