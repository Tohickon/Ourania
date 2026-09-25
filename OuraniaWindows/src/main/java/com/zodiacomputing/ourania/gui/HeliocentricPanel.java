package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Heliocentric;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The chart as seen from the Sun (D12).
 *
 * <p><b>There is no place field, and that is the screen's first statement.</b> Every other chart
 * in the app needs a place, because a place is where the horizon is and the horizon is what cuts
 * the ecliptic into houses. From the Sun there is no observer, so this chart is a function of the
 * moment alone - and asking for a place would imply an Ascendant that does not exist.
 *
 * <p><b>It says what is absent, with reasons.</b> A reader arriving from the chart wheel will look
 * for the Sun, the Moon, the Ascendant and the houses, and not find them. A list of absences with
 * no explanation reads as a defect; the same list with its reasons reads as the astronomy, which
 * is what it is. The reasons come from {@link Heliocentric#whyExcluded} rather than being written
 * here, because this project's recurring defect is one rule stated in two places.
 *
 * <p>The screen writes no astrology. It reports positions, distances and aspects - the same
 * discipline {@code HoraryPanel} and {@code ElectionalPanel} keep, and for the same reason.
 */
public final class HeliocentricPanel extends JPanel {

    /** A moment, typed and shown. No zone conversion: see {@link #castNow}. */
    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Built on the first cast, not on construction - as in {@link HoraryPanel}, where an eager
     * handle took NavigationCheck from 192 to 702 seconds.
     */
    private SwissEph sw;

    private final JTextField moment = new JTextField(15);
    private final JButton now = new JButton("Now");
    private final JButton cast = new JButton("Cast from the Sun");
    private final JEditorPane out;

    private SwingWorker<String, Void> running;

    public HeliocentricPanel(OuraniaWindow window) {
        this.out = HtmlPanes.chartPane(window);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Heliocentric");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("The solar system from its centre - no observer, no houses.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);
        lede.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        Widgets.styleButton(this.cast, Widgets.Role.PRIMARY);
        Widgets.styleButton(this.now, Widgets.Role.TRANSPORT);
        this.moment.setToolTipText("<html>The moment to cast, as <code>yyyy-MM-dd HH:mm</code> UT."
            + "<br><b>No place is asked for.</b> A heliocentric chart has no horizon, so it"
            + "<br>depends on the moment alone.</html>");
        this.moment.setText(ZonedDateTime.now(ZoneId.of("UTC")).format(STAMP));

        this.now.addActionListener(e ->
            this.moment.setText(ZonedDateTime.now(ZoneId.of("UTC")).format(STAMP)));
        this.cast.addActionListener(e -> this.castNow());

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        head.setOpaque(false);
        head.add(title);
        head.add(lede);
        head.add(new Caption("Moment (UT)"));
        head.add(this.moment);
        head.add(this.now);
        head.add(this.cast);

        add(head, BorderLayout.NORTH);
        add(HtmlPanes.scroller(this.out), BorderLayout.CENTER);
        HtmlPanes.setHtml(this.out, intro());
    }

    /** A dim caption beside a field, so the head row reads as a sentence. */
    private static final class Caption extends JLabel {
        Caption(String s) {
            super(s);
            setForeground(Theme.TEXT_DIM);
            setFont(Theme.SMALL);
        }
    }

    private static String intro() {
        return "<p>A heliocentric chart puts the Sun at the centre and the Earth among the "
            + "planets. Pick a moment and cast it.</p><p>Because there is no observer, there are "
            + "no houses, no angles and no lots - and no Sun or Moon as bodies. What the chart "
            + "gains instead is <b>distance</b>: every body's real remove from the centre, which "
            + "a geocentric chart cannot state.</p>";
    }

    /**
     * The moment is read as UT, not as local time.
     *
     * <b>Unlike every other screen in the app</b>, and for the same reason there is no place
     * field: local time is local to a place. Converting through a zone here would be inventing
     * an observer to convert for.
     */
    private void castNow() {
        if (this.running != null && !this.running.isDone()) {
            return;
        }
        final LocalDateTime when = parse(this.moment.getText());
        if (when == null) {
            HtmlPanes.setHtml(this.out, "<p>I could not read <b>" + escape(this.moment.getText())
                + "</b> as a moment. Write it as <code>yyyy-MM-dd HH:mm</code>, or press Now.</p>");
            return;
        }
        HtmlPanes.setHtml(this.out, "<p>Casting…</p>");
        this.running = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                if (HeliocentricPanel.this.sw == null) {
                    HeliocentricPanel.this.sw = new SwissEph(Ephemeris.PATH);
                }
                double jd = SweDate.getJulDay(when.getYear(), when.getMonthValue(),
                    when.getDayOfMonth(), when.getHour() + when.getMinute() / 60.0);
                Heliocentric.Frame f = Heliocentric.compute(HeliocentricPanel.this.sw, jd);
                return render(f, when);
            }

            @Override
            protected void done() {
                try {
                    HtmlPanes.setHtml(HeliocentricPanel.this.out, get());
                } catch (Exception e) {
                    HtmlPanes.setHtml(HeliocentricPanel.this.out,
                        "<p>The chart would not cast: " + escape(String.valueOf(e)) + "</p>");
                }
            }
        };
        this.running.execute();
    }

    /** Package-private so the suite can render without a window. */
    static String render(Heliocentric.Frame f, LocalDateTime when) {
        StringBuilder b = new StringBuilder();
        b.append("<h2>").append(when.format(STAMP)).append(" UT</h2>");
        b.append("<p class=\"dim\">Longitudes in the ")
            .append(escape(Ephemeris.zodiacLabel()))
            .append(" zodiac. Distance is from the Sun, in astronomical units.</p>");

        b.append("<table><tr><th>Body</th><th>Longitude</th><th>Latitude</th>")
            .append("<th>Speed / day</th><th>Distance (AU)</th></tr>");
        for (Heliocentric.Place p : f.places) {
            b.append("<tr><td>").append(escape(p.glyph)).append(' ')
                .append(escape(p.name)).append("</td>");
            if (!p.ok) {
                b.append("<td colspan=\"4\">").append(escape(p.error == null ? "no position"
                    : p.error)).append("</td></tr>");
                continue;
            }
            b.append("<td>").append(escape(Zodiac.format(p.longitude))).append("</td>");
            b.append("<td>").append(String.format("%+.2f°", p.latitude)).append("</td>");
            b.append("<td>").append(String.format("%.4f°", p.speed)).append("</td>");
            b.append("<td>").append(String.format("%.4f", p.distanceAu)).append("</td></tr>");
        }
        b.append("</table>");

        // <b>Stated, not left for the reader to notice.</b> Every speed above is positive, and a
        // reader used to a geocentric chart will wonder where the retrogrades went.
        b.append("<p>Every body moves forward. Retrogradation is the Earth overtaking a planet "
            + "or being overtaken by it, so from the centre there is none to see.</p>");

        List<Heliocentric.Contact> cs = Heliocentric.aspects(f);
        b.append("<h3>Aspects</h3>");
        if (cs.isEmpty()) {
            b.append("<p>No two bodies are within orb of an aspect at this moment.</p>");
        } else {
            b.append("<table><tr><th>Between</th><th>Aspect</th><th>Orb</th></tr>");
            for (Heliocentric.Contact c : cs) {
                b.append("<tr><td>").append(escape(c.a)).append(" &ndash; ")
                    .append(escape(c.b)).append("</td><td>")
                    .append(escape(c.type.label)).append("</td><td>")
                    .append(String.format("%.2f°", c.orb)).append("</td></tr>");
            }
            b.append("</table>");
            b.append("<p class=\"dim\">Judged at the widths of the Natal preset, because this is "
                + "a chart of a moment rather than one body's passage over another.</p>");
        }

        b.append(absent());

        if (!f.warnings.isEmpty()) {
            b.append("<h3>Notes</h3><ul>");
            for (String w : f.warnings) {
                b.append("<li>").append(escape(w)).append("</li>");
            }
            b.append("</ul>");
        }
        return b.toString();
    }

    /**
     * What a geocentric reader will look for and not find, with the reason for each.
     *
     * <b>The reasons are {@link Heliocentric#whyExcluded}'s</b>, not this screen's. A second
     * author of the same rule is how {@code isMinor} ended up in three places.
     */
    static String absent() {
        StringBuilder b = new StringBuilder();
        b.append("<h3>What is not here</h3><ul>");
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if (d.source != Bodies.Source.EPHEMERIS) {
                continue;
            }
            String why = Heliocentric.whyExcluded(d);
            if (why != null) {
                b.append("<li><b>").append(escape(d.name)).append("</b> — ")
                    .append(escape(why)).append("</li>");
            }
        }
        b.append("<li><b>The houses, the angles and the lots</b> — an Ascendant is where the "
            + "horizon of a place cuts the ecliptic, and there is no place here. The Part of "
            + "Fortune and the other lots are arcs measured from it, so they go with it.</li>");
        b.append("</ul>");
        return b.toString();
    }

    static LocalDateTime parse(String s) {
        try {
            return LocalDateTime.parse(s.trim(), STAMP);
        } catch (Exception e) {
            return null;
        }
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
