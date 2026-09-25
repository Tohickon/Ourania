package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.TransitSearch;

import de.thmac.swisseph.SwissEph;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transit search: "when does Saturn square my Moon between 2026 and 2030?"
 *
 * <p><b>Master list F1, and the one feature a practitioner opens other software to use.</b>
 * The engine has computed exact transit dates since August - for the year scan, the readings
 * and the convergence ranking - and nothing let a reader ask a question of it. This screen is
 * that door. The search itself is {@link TransitSearch}; this class only collects the question
 * and lays out the answer.
 *
 * <p><b>The chart searched is the one on the wheel</b>, read when Search is pressed rather than
 * when the screen opens, so changing Chart A and coming back cannot search the old one.
 */
public final class TransitSearchPanel extends JPanel {

    static final String SLOW = "Slow planets: Jupiter to Pluto, and Chiron";
    static final String ALL = "Every planet, Sun to Pluto, Chiron and the Node";
    static final String EVERY_POINT = "Every point in the chart";

    private final OuraniaWindow window;
    private final JComboBox<String> transiting = new JComboBox<>();
    private final JComboBox<String> natalPoint = new JComboBox<>();
    private final JCheckBox[] aspects = new JCheckBox[TransitSearch.MAJOR.length];
    private final JSpinner fromYear;
    private final JSpinner toYear;
    private final JSpinner orb;
    /** The Settings transit orb as of the last time the spinner was set from it. */
    private double orbSetting;
    private final JButton searchButton = new JButton("Search");
    private final JLabel status = new JLabel(" ");
    private final JEditorPane results;
    private SwingWorker<String, String> running;

    /** How many passages the last search found, for checks; -1 before any search finishes. */
    private volatile int lastCount = -1;

    public TransitSearchPanel(OuraniaWindow window) {
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Transit Search");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("When a transiting planet aspects a point in Chart A: every "
            + "exact date, the stations between them, and the stretch it stays in orb.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);

        transiting.addItem(SLOW);
        transiting.addItem(ALL);
        for (String body : TransitSearch.TRANSITING) {
            transiting.addItem(body);
        }
        transiting.setSelectedItem(SLOW);
        Widgets.styleCombo(transiting);
        natalPoint.addItem(EVERY_POINT);
        Widgets.styleCombo(natalPoint);

        int year = LocalDate.now().getYear();
        fromYear = new JSpinner(new SpinnerNumberModel(year, 1800, 2399, 1));
        toYear = new JSpinner(new SpinnerNumberModel(year + 4, 1800, 2399, 1));
        fromYear.setEditor(new JSpinner.NumberEditor(fromYear, "#"));
        toYear.setEditor(new JSpinner.NumberEditor(toYear, "#"));
        orbSetting = com.zodiacomputing.ourania.astro.Transits.orb;
        orb = new JSpinner(new SpinnerNumberModel(orbSetting, 0.1, 5.0, 0.5));
        orb.setToolTipText("<html>How close counts as in effect, in degrees either side of exact."
            + "<br>It sets where each passage starts and ends; the exact dates do not depend on it."
            + "<br>Opens at the transit orb in Settings, which the readings and the calendar use too;"
            + "<br>a change here is for this search only.</html>");

        JPanel row1 = row();
        row1.add(label("Transiting"));
        row1.add(transiting);
        row1.add(label("to natal"));
        row1.add(natalPoint);

        JPanel row2 = row();
        for (int i = 0; i < aspects.length; i++) {
            aspects[i] = new JCheckBox(TransitSearch.MAJOR[i].label, true);
            aspects[i].setOpaque(false);
            aspects[i].setForeground(Theme.TEXT);
            aspects[i].setFont(Theme.BODY);
            row2.add(aspects[i]);
        }

        JPanel row3 = row();
        row3.add(label("From"));
        row3.add(fromYear);
        row3.add(label("through"));
        row3.add(toYear);
        row3.add(label("  Orb"));
        row3.add(orb);
        row3.add(label("°"));
        Widgets.styleButton(searchButton, Widgets.Role.PRIMARY);
        searchButton.addActionListener(e -> search());
        row3.add(searchButton);

        status.setForeground(Theme.TEXT_DIM);
        status.setFont(Theme.SMALL);
        // The whole width, not a flow row: a flow row sized the label to its first text and
        // cut the finished summary off after "orb".
        JPanel row4 = new JPanel(new BorderLayout());
        row4.setOpaque(false);
        row4.setBorder(Theme.pad(4, 8, 0, 8));
        row4.add(status, BorderLayout.CENTER);

        JPanel head = new JPanel();
        head.setLayout(new javax.swing.BoxLayout(head, javax.swing.BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.setBorder(Theme.pad(16, 20, 8, 20));
        JPanel titles = new JPanel(new BorderLayout(0, 4));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(lede, BorderLayout.CENTER);
        titles.setAlignmentX(LEFT_ALIGNMENT);
        head.add(titles);
        for (JPanel r : new JPanel[]{row1, row2, row3, row4}) {
            r.setAlignmentX(LEFT_ALIGNMENT);
            head.add(r);
        }
        add(head, BorderLayout.NORTH);

        results = HtmlPanes.chartPane(window);
        HtmlPanes.setHtml(results, page("<p style='color:#94A0B4'>Choose what to look for and "
            + "press Search.</p>"));
        javax.swing.JScrollPane scroll = HtmlPanes.scroller(results);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        add(scroll, BorderLayout.CENTER);
    }

    /** Fills the natal point list from the chart on the wheel; called when the screen opens. */
    void refreshChart() {
        // The Settings orb moved since this screen last looked: follow it. An orb typed here for
        // one search is left alone otherwise.
        if (com.zodiacomputing.ourania.astro.Transits.orb != orbSetting) {
            orbSetting = com.zodiacomputing.ourania.astro.Transits.orb;
            orb.setValue(orbSetting);
        }
        ChartFrame natal = chart();
        Object keep = natalPoint.getSelectedItem();
        natalPoint.removeAllItems();
        natalPoint.addItem(EVERY_POINT);
        if (natal != null) {
            for (ChartFrame.Body b : natal.bodies) {
                if (b != null && b.ok) {
                    natalPoint.addItem(b.name);
                }
            }
        }
        natalPoint.setSelectedItem(keep != null ? keep : EVERY_POINT);
        if (natalPoint.getSelectedIndex() < 0) {
            natalPoint.setSelectedItem(EVERY_POINT);
        }
    }

    private ChartFrame chart() {
        if (window == null || !window.wheelHasChartA()) {
            return null;
        }
        return window.radixChartForSearch();
    }

    /** Runs the search on a worker; a second press replaces the first rather than queueing. */
    void search() {
        final ChartFrame natal = chart();
        if (natal == null) {
            showMessage("There is no Chart A on the wheel to search. Cast one in Chart Setup first.");
            return;
        }
        final List<String> bodies = bodiesFor((String) transiting.getSelectedItem());
        final List<String> points = EVERY_POINT.equals(natalPoint.getSelectedItem())
            ? null : Arrays.asList((String) natalPoint.getSelectedItem());
        final List<Aspects.Type> types = new ArrayList<>();
        for (int i = 0; i < aspects.length; i++) {
            if (aspects[i].isSelected()) {
                types.add(TransitSearch.MAJOR[i]);
            }
        }
        if (types.isEmpty()) {
            showMessage("Tick at least one aspect.");
            return;
        }
        int y0 = (Integer) fromYear.getValue();
        int y1 = (Integer) toYear.getValue();
        if (y1 < y0) {
            showMessage("The last year is before the first.");
            return;
        }
        final double jdFrom = jd(LocalDate.of(y0, 1, 1));
        final double jdTo = jd(LocalDate.of(y1 + 1, 1, 1));
        final double orbDeg = ((Number) orb.getValue()).doubleValue();
        final String span = y0 == y1 ? String.valueOf(y0) : y0 + " through " + y1;

        if (running != null) {
            running.cancel(true);
        }
        lastCount = -1;
        searchButton.setEnabled(false);
        status.setText("Searching " + span + "...");
        running = new SwingWorker<String, String>() {
            private int found;

            @Override
            protected String doInBackground() {
                // Its own ephemeris: the wheel's is used by the animation on the event thread,
                // and a Swiss Ephemeris instance holds state between calls.
                SwissEph sw = new SwissEph(Ephemeris.PATH);
                List<TransitSearch.Passage> all = new ArrayList<>();
                for (int i = 0; i < bodies.size() && !isCancelled(); i++) {
                    publish("Searching " + bodies.get(i) + " (" + (i + 1) + " of "
                        + bodies.size() + "), " + span + "...");
                    // <b>An orb typed for one search wins; an untouched box follows the
                    // Transits preset.</b> The box opens at the setting, so equality with it is
                    // exactly "the reader has not overridden this", and the per-point widths they
                    // set under Orbs reach the Search as they reach the reading lists.
                    boolean typed = Math.abs(orbDeg
                        - com.zodiacomputing.ourania.astro.Transits.orb) > 1e-9;
                    all.addAll(typed
                        ? TransitSearch.search(sw, natal, List.of(bodies.get(i)), points,
                            types, orbDeg, jdFrom, jdTo)
                        : TransitSearch.searchAtReaderWidths(sw, natal, List.of(bodies.get(i)),
                            points, types, jdFrom, jdTo));
                }
                all = TransitSearch.perfectingIn(all, jdFrom, jdTo);
                all.sort(java.util.Comparator.comparingDouble(TransitSearch.Passage::firstMoment));
                found = all.size();
                return table(all, span, orbDeg);
            }

            @Override
            protected void process(List<String> chunks) {
                status.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                searchButton.setEnabled(true);
                try {
                    HtmlPanes.setHtml(results, get());
                    lastCount = found;
                    status.setText(found + (found == 1 ? " passage" : " passages") + ", " + span
                        + ", orb " + trim(orbDeg) + "°. Dates and times are "
                        + ZoneId.systemDefault().getId() + ".");
                } catch (Exception ex) {
                    status.setText("The search failed: " + ex.getMessage());
                }
            }
        };
        running.execute();
    }

    static List<String> bodiesFor(String choice) {
        if (SLOW.equals(choice)) {
            return Arrays.asList("Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron");
        }
        if (ALL.equals(choice)) {
            return Arrays.asList(TransitSearch.TRANSITING);
        }
        return Arrays.asList(choice);
    }

    /** The answer as a table, one row per passage. */
    static String table(List<TransitSearch.Passage> passages, String span, double orbDeg) {
        StringBuilder h = new StringBuilder();
        if (passages.isEmpty()) {
            h.append("<p style='color:#94A0B4'>Nothing perfects in ").append(span)
             .append(" for what you asked. A wider span or another aspect may find something.</p>");
            return page(h.toString());
        }
        h.append("<table cellpadding='5' cellspacing='0' width='100%'>");
        h.append("<tr style='color:#94A0B4'><td><b>Transit</b></td><td><b>Exact</b></td>"
            + "<td><b>Stations</b></td><td><b>In orb</b></td></tr>");
        for (TransitSearch.Passage p : passages) {
            // One run of text, no bold or colour inside it. Swing's HTML swallows a single space
            // at the edge of a styled run - rendered, "Neptunetrine" and "natalJupiter" - and
            // that held for bold, <font>, <span>, and &nbsp; on either side of the boundary;
            // only a doubled space survived, which would be leaning on the bug.
            h.append("<tr valign='top'><td style='border-top:1px solid #303C54'>")
             .append(p.transiting).append(' ').append(p.type.label.toLowerCase())
             .append(" natal ").append(p.natal)
             .append("</td><td style='border-top:1px solid #303C54'>");
            if (p.exacts.isEmpty()) {
                h.append("<span style='color:#94A0B4'>comes within orb, never exact</span>");
            }
            for (int i = 0; i < p.exacts.size(); i++) {
                TransitSearch.Exact e = p.exacts.get(i);
                if (i > 0) {
                    h.append("<br>");
                }
                // The retrograde exact gets its own colour for the whole line, for the same
                // reason: a coloured word beside the date would lose the space before it.
                h.append(e.retrograde
                    ? "<font color='#E2B258'>" + when(e.jd, true) + " retrograde</font>"
                    : when(e.jd, true));
            }
            h.append("</td><td style='border-top:1px solid #303C54'>");
            for (int i = 0; i < p.stations.size(); i++) {
                TransitSearch.Station s = p.stations.get(i);
                if (i > 0) {
                    h.append("<br>");
                }
                h.append(s.retrograde ? "turns retrograde " : "turns direct ")
                 .append(when(s.jd, false)).append(" at ")
                 .append(com.zodiacomputing.ourania.astro.Zodiac.format(s.longitude));
            }
            h.append("</td><td style='border-top:1px solid #303C54; color:#94A0B4'>")
             .append(Double.isNaN(p.enters) ? "already in orb" : when(p.enters, false))
             .append(" to ")
             .append(Double.isNaN(p.leaves) ? "beyond the search" : when(p.leaves, false))
             .append("</td></tr>");
        }
        h.append("</table>");
        return page(h.toString());
    }

    private static String page(String body) {
        return "<html><body style='font-family:sans-serif; font-size:11px; color:#E2E8F0; "
            + "background-color:#090C14'>" + body + "</body></html>";
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    static String when(double jd, boolean withTime) {
        long ms = Math.round((jd - 2440587.5) * 86400000.0);
        ZonedDateTime t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault());
        return (withTime ? MINUTE : DAY).format(t);
    }

    private static double jd(LocalDate day) {
        long ms = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return ms / 86400000.0 + 2440587.5;
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private void showMessage(String text) {
        HtmlPanes.setHtml(results, page("<p style='color:#E2B258'>" + text + "</p>"));
        status.setText(" ");
    }

    private static JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setOpaque(false);
        return p;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.TEXT);
        l.setFont(Theme.BODY);
        return l;
    }

    // ---- for checks

    int lastCount() {
        return lastCount;
    }

    String resultsHtml() {
        return results.getText();
    }

    void choose(String body, String point, int from, int to, double orbDeg) {
        transiting.setSelectedItem(body);
        natalPoint.setSelectedItem(point);
        fromYear.setValue(from);
        toYear.setValue(to);
        orb.setValue(orbDeg);
    }
}
