package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Rectify;
import de.thmac.swisseph.SwissEph;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Where a birth time is worked out from what happened afterwards (G8).
 *
 * <p><b>The engine's door.</b> {@link Rectify} scores candidate birth times against dated events,
 * and an engine with no way in is invisible in exactly the way a missing one is not - the suites
 * stay green, so nothing reports it. This project has found four of those this month.
 *
 * <p><b>Events are typed as text, one to a line.</b> A form with date pickers and a body dropdown
 * per row is a better screen and a much larger one; a practitioner rectifying a chart already has
 * the dates written down somewhere and can paste them. The format is stated on the screen and
 * anything unreadable is named rather than skipped.
 *
 * <p><b>It reports the ranking and says when it has not decided.</b> The one thing this screen must
 * never do is print a time to the minute from evidence that does not support one, because a reader
 * will believe it.
 */
final class RectifyPanel extends JPanel {

    private final OuraniaWindow window;
    private final JTextArea events = new JTextArea(8, 40);
    private final JEditorPane out = HtmlPanes.darkPane();
    private final JButton scan = new JButton("Work out the time");

    RectifyPanel(OuraniaWindow window) {
        this.window = window;
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBackground(Theme.BG);
        setBorder(Theme.pad(Theme.GAP, Theme.GAP, Theme.GAP, Theme.GAP));

        JPanel top = new JPanel(new BorderLayout(Theme.GAP_S, Theme.GAP_S));
        top.setBackground(Theme.BG);
        JEditorPane how = HtmlPanes.darkPane();
        HtmlPanes.setHtml(how, "<html><body><h2>Rectification</h2>"
            + "<p>The chart on the wheel supplies the <b>date and the place</b>, which "
            + "rectification takes as known. Type the things that happened, one to a line:</p>"
            + "<pre>1998-04-12  Saturn  left home\n"
            + "2004-09-01  Venus   married\n"
            + "2011-02-20          a move</pre>"
            + "<p>A date, then optionally the point it should have touched, then whatever you "
            + "want to call it. Three events is the fewest that can decide anything.</p>"
            + "<p><b>It rectifies against the angles</b>, because they are the only thing a birth "
            + "time moves much: four minutes of clock is about a degree of Midheaven, and a "
            + "degree of directed arc is about a year of life.</p></body></html>");
        how.setPreferredSize(new Dimension(520, 230));
        top.add(HtmlPanes.scroller(how), BorderLayout.NORTH);

        events.setBackground(Theme.SURFACE_3);
        events.setForeground(Theme.TEXT);
        events.setCaretColor(Theme.ACCENT);
        events.setFont(Theme.font("Monospaced", java.awt.Font.PLAIN, 13));
        Undo.install(events);
        Accessibility.name(events, "Events",
            "One life event per line: a date, optionally a chart point, then a description.");
        top.add(new javax.swing.JScrollPane(events), BorderLayout.CENTER);

        Widgets.styleButton(scan, Widgets.Role.PRIMARY);
        scan.setToolTipText("Score every birth time in a two-hour window around the one on the "
            + "wheel, four minutes apart.");
        scan.addActionListener(e -> run());
        JPanel buttons = new JPanel();
        buttons.setBackground(Theme.BG);
        buttons.add(scan);
        top.add(buttons, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(HtmlPanes.scroller(out), BorderLayout.CENTER);
        HtmlPanes.setHtml(out, "<html><body><p>Type some events and press the button.</p>"
            + "</body></html>");
    }

    /** Reads the box, scans, and renders. The scan casts charts, so it runs off the event thread. */
    private void run() {
        final String typed = events.getText();
        final com.zodiacomputing.ourania.astro.ChartFrame chart =
            window == null ? null : window.currentChartFrame();
        if (chart == null) {
            HtmlPanes.setHtml(out, "<html><body><p>There is no chart on the wheel to rectify. "
                + "Cast one first.</p></body></html>");
            return;
        }
        scan.setEnabled(false);
        new javax.swing.SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                List<String> bad = new ArrayList<>();
                List<Rectify.Event> list = parse(typed, bad);
                if (list.size() < Rectify.minimumEvents) {
                    return new Object[] {null, bad, list.size()};
                }
                de.thmac.swisseph.SweDate birth = new de.thmac.swisseph.SweDate(
                    chart.julianDayUt);
                double hour = birth.getHour();
                SwissEph sw = new SwissEph(Ephemeris.PATH);
                Rectify.Result r = Rectify.scan(sw, birth.getYear(), birth.getMonth(),
                    birth.getDay(), chart.geoLat, chart.geoLon,
                    Math.max(0.0, hour - 1.0), Math.min(23.9, hour + 1.0), 4, list);
                return new Object[] {r, bad, list.size()};
            }

            @Override
            protected void done() {
                scan.setEnabled(true);
                try {
                    Object[] got = get();
                    HtmlPanes.setHtml(out, render((Rectify.Result) got[0],
                        castList(got[1]), (Integer) got[2]));
                    out.setCaretPosition(0);
                } catch (Exception ex) {
                    HtmlPanes.setHtml(out, "<html><body><p>The scan could not finish: "
                        + escape(String.valueOf(ex.getMessage())) + "</p></body></html>");
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object o) {
        return (List<String>) o;
    }

    /**
     * One line per event: a date, an optional body, a description.
     *
     * <b>An unreadable line is named, not dropped.</b> A practitioner who mistypes one date of
     * twelve should be told which, rather than wondering why the answer moved.
     */
    static List<Rectify.Event> parse(String text, List<String> problems) {
        List<Rectify.Event> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        String[] lines = text.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+", 3);
            String date = ChartImport.normaliseDate(parts[0], 0);
            if (date == null) {
                problems.add("Line " + (i + 1) + ": \"" + parts[0] + "\" is not a date I can "
                    + "read without guessing. Write it as 1998-04-12.");
                continue;
            }
            String body = "";
            String what = parts.length > 1 ? line.substring(parts[0].length()).trim() : "";
            if (parts.length > 1
                    && com.zodiacomputing.ourania.astro.Bodies.byName(parts[1]) != null) {
                body = com.zodiacomputing.ourania.astro.Bodies.byName(parts[1]).name;
                what = parts.length > 2 ? parts[2].trim() : body;
            }
            if (what.isEmpty()) {
                what = "an event";
            }
            double jd = new de.thmac.swisseph.SweDate(
                Integer.parseInt(date.substring(0, 4)),
                Integer.parseInt(date.substring(5, 7)),
                Integer.parseInt(date.substring(8, 10)), 12.0).getJulDay();
            out.add(new Rectify.Event(what, jd, body));
        }
        return out;
    }

    /** What the scan found, said plainly. */
    static String render(Rectify.Result r, List<String> problems, int events) {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (String p : problems) {
            sb.append("<p><b>").append(escape(p)).append("</b></p>");
        }
        if (r == null || r.candidates.isEmpty()) {
            sb.append("<p>").append(events).append(" event")
                .append(events == 1 ? "" : "s").append(" read. ")
                .append("Rectification needs at least ").append(Rectify.minimumEvents)
                .append(", because fewer cannot tell one time from another.</p>");
            return sb.append("</body></html>").toString();
        }

        Rectify.Candidate best = r.best();
        sb.append("<h2>").append(clock(best.hour)).append("</h2>");
        if (r.confident) {
            sb.append("<p>The best fit of ").append(r.candidates.size())
                .append(" times tried, against ").append(r.events).append(" events.</p>");
        } else {
            // <b>The sentence this screen exists to be able to say.</b>
            sb.append("<p><b>This is a ranking, not an answer.</b> These events do not separate "
                + "the candidate times well enough to name one &mdash; the best scores close to "
                + "the middle of the field. Treat the list below as where to look, and find more "
                + "dated events.</p>");
        }

        sb.append("<h3>Why</h3><ul>");
        for (String reason : best.reasons) {
            sb.append("<li>").append(escape(reason)).append("</li>");
        }
        if (best.reasons.isEmpty()) {
            sb.append("<li>No directed contact inside ").append(Rectify.orb)
                .append("&deg; for any event.</li>");
        }
        sb.append("</ul>");

        sb.append("<h3>The field</h3><table cellpadding='3'><tr><th>Time</th><th>Score</th></tr>");
        for (int i = 0; i < Math.min(10, r.candidates.size()); i++) {
            Rectify.Candidate c = r.candidates.get(i);
            sb.append("<tr><td>").append(clock(c.hour)).append("</td><td>")
                .append(String.format("%.3f", c.score)).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p>Separation ").append(String.format("%.2f", r.separation))
            .append(" &mdash; how far the best stands above the middle of the field, where 0 is "
                + "no better and 1 is far better.</p>");
        return sb.append("</body></html>").toString();
    }

    private static String clock(double hour) {
        int h = (int) Math.floor(hour + 1e-9);
        int m = (int) Math.round((hour - h) * 60.0);
        if (m == 60) {
            h++;
            m = 0;
        }
        return String.format("%02d:%02d", h, m);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
