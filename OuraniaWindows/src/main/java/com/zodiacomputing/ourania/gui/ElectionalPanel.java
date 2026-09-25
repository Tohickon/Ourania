package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Electional;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Horary;
import com.zodiacomputing.ourania.astro.PlanetaryHours;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

/**
 * Electional: a moment weighed, and the planetary hours of its day.
 *
 * <p><b>The last of F10.</b> {@code Electional.assess} and {@code PlanetaryHours} were both built
 * and held by suites, and neither had a way in - {@code NavigationCheck} had reported
 * {@code PlanetaryHours} unreachable since the day it was written. This is that door, and it is
 * the same door for both, because the day and hour rulers <i>are</i> the first two measures an
 * election is made on.
 *
 * <p><b>This screen writes no astrology of its own.</b> Every testimony arrives carrying its own
 * sentence - {@code Note.because} says in its javadoc that it is "in the reader's language rather
 * than the engine's" - so what happens here is ordering and presentation. The same discipline
 * {@code HoraryPanel} follows, and for the same reason: a second author of the same judgement is
 * this project's recurring defect.
 *
 * <p>The hours table is the reader's actual working tool. "Is this a good moment" is answered by
 * the weighing; "when today would be better" is answered by looking down the column for a Jupiter
 * or Venus hour, which is why the whole day is shown rather than only the hour in force.
 */
public final class ElectionalPanel extends JPanel {

    /** How a moment is typed and shown. Local to the place, as an electional reader thinks. */
    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Built on the first weighing, not on construction - as in {@link HoraryPanel}, where an
     * eager handle took NavigationCheck from 192 to 702 seconds.
     */
    private SwissEph sw;

    private final JComboBox<String> matter = new JComboBox<>();
    private final JTextField place = new JTextField(16);
    private final JTextField moment = new JTextField(15);
    private final JButton now = new JButton("Now");
    private final JButton weigh = new JButton("Weigh this moment");
    private final JEditorPane out;

    private SwingWorker<Object[], Void> running;

    public ElectionalPanel(OuraniaWindow window) {
        this.out = HtmlPanes.chartPane(window);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Electional");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("A moment weighed for what it is shaped to carry.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);
        lede.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        // <b>The matter is optional here, unlike Horary.</b> Electional.assess says so in its
        // javadoc: the South Node measure needs significators and significators need a house of
        // the matter, so with no matter that one testimony simply does not fire rather than
        // firing against a significator invented for the occasion.
        this.matter.addItem("no particular matter");
        for (Horary.Matter m : Horary.Matter.values()) {
            this.matter.addItem(m.house + " - " + m.about);
        }
        Widgets.styleCombo(this.matter);
        Widgets.styleButton(this.weigh, Widgets.Role.PRIMARY);
        Widgets.styleButton(this.now, Widgets.Role.TRANSPORT);

        this.matter.setToolTipText("<html><b>What the moment is being elected for.</b><br>"
            + "Left at <i>no particular matter</i>, the Moon-and-South-Node measure does not"
            + "<br>fire, because it needs a significator and a significator needs a house."
            + "</html>");
        this.place.setToolTipText("Where the thing will happen - your home place by default");
        this.moment.setToolTipText("<html>The moment to weigh, in <b>local time at that place</b>,"
            + "<br>as <code>yyyy-MM-dd HH:mm</code>. Press Now for this minute.</html>");
        PlaceField.attach(this.place);
        this.place.setText(Settings.get("home.location",
            Settings.get("default.transit.location", "")).trim());
        this.moment.setText(ZonedDateTime.now(ZoneId.systemDefault()).format(STAMP));

        this.now.addActionListener(e ->
            this.moment.setText(ZonedDateTime.now(ZoneId.systemDefault()).format(STAMP)));
        this.weigh.addActionListener(e -> this.weighNow());

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        head.setOpaque(false);
        head.add(title);
        head.add(lede);
        head.add(this.matter);
        head.add(this.place);
        head.add(this.moment);
        head.add(this.now);
        head.add(this.weigh);

        add(head, BorderLayout.NORTH);
        add(HtmlPanes.scroller(this.out), BorderLayout.CENTER);
        HtmlPanes.setHtml(this.out, "<p>Choose a place and a moment, then weigh it. The day and "
            + "hour rulers say what the moment is shaped for; the testimonies say what raises "
            + "and lowers it.</p>");
    }

    private void weighNow() {
        if (this.running != null && !this.running.isDone()) {
            return;
        }
        int pick = this.matter.getSelectedIndex();
        final Horary.Matter m = pick <= 0 ? null : Horary.Matter.values()[pick - 1];
        final String typed = this.place.getText().trim();
        if (typed.isEmpty()) {
            HtmlPanes.setHtml(this.out, "<p>I do not know where this would happen. Type a place, "
                + "or set a home location in Settings.</p>");
            return;
        }
        final LocalDateTime local = parse(this.moment.getText());
        if (local == null) {
            HtmlPanes.setHtml(this.out, "<p>I could not read <b>" + escape(this.moment.getText())
                + "</b> as a moment. Write it as <code>yyyy-MM-dd HH:mm</code>, or press Now.</p>");
            return;
        }
        this.weigh.setEnabled(false);
        HtmlPanes.setHtml(this.out, "<p>Weighing that moment...</p>");

        this.running = new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                // The lookup blocks and says so in its own javadoc, so it happens here rather
                // than on the button, where it would freeze the window for a round trip.
                Geocoder.Result there = Geocoder.lookup(typed);
                if (there == null) {
                    return new Object[] {null, null, null, null};
                }
                if (ElectionalPanel.this.sw == null) {
                    ElectionalPanel.this.sw = new SwissEph(Ephemeris.PATH);
                }
                // <b>The typed moment is local to the PLACE, not to this computer.</b> An
                // election made for a venue three time zones away is made in that venue's
                // clock; reading it in the system zone would weigh a different hour, and the
                // planetary hour is the first thing this screen reports.
                ZonedDateTime at = local.atZone(zoneOf(there));
                ZonedDateTime utc = at.withZoneSameInstant(ZoneId.of("UTC"));
                double jd = new SweDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
                    utc.getHour() + utc.getMinute() / 60.0).getJulDay();
                ChartFrame f = ChartFrame.compute(ElectionalPanel.this.sw, jd,
                    there.lat, there.lon, 'P', false, 0.0);
                Electional.Quality q = Electional.assess(ElectionalPanel.this.sw, f, jd,
                    there.lat, there.lon, m);
                PlanetaryHours.Day day = PlanetaryHours.at(ElectionalPanel.this.sw, jd,
                    there.lat, there.lon);
                return new Object[] {q, day, there, at};
            }

            @Override
            protected void done() {
                ElectionalPanel.this.weigh.setEnabled(true);
                try {
                    Object[] r = get();
                    if (r[0] == null) {
                        HtmlPanes.setHtml(ElectionalPanel.this.out,
                            "<p>I could not find " + escape(typed) + ".</p>");
                        return;
                    }
                    HtmlPanes.setHtml(ElectionalPanel.this.out, render(
                        (Electional.Quality) r[0], (PlanetaryHours.Day) r[1], m,
                        (ZonedDateTime) r[3], (Geocoder.Result) r[2]));
                    ElectionalPanel.this.out.setCaretPosition(0);
                } catch (Exception ex) {
                    HtmlPanes.setHtml(ElectionalPanel.this.out, "<p>The moment could not be "
                        + "weighed: " + escape(String.valueOf(ex.getMessage())) + "</p>");
                }
            }
        };
        this.running.execute();
    }

    /**
     * The place's own zone where the geocoder gave one, else this computer's.
     *
     * <b>Package-private so the suite can assert it rather than the principle.</b> A test that
     * only shows two zones give two instants would pass whatever this method returned.
     */
    static ZoneId zoneOf(Geocoder.Result where) {
        if (where != null && where.tzId != null && !where.tzId.trim().isEmpty()) {
            try {
                return ZoneId.of(where.tzId.trim());
            } catch (Exception ignored) {
                // An unknown zone id is not worth refusing the election over.
            }
        }
        return ZoneId.systemDefault();
    }

    /** A typed moment, or null. Lenient about the minute so "2026-09-25 14" is not a refusal. */
    static LocalDateTime parse(String typed) {
        if (typed == null) {
            return null;
        }
        String s = typed.trim().replace('T', ' ');
        if (s.isEmpty()) {
            return null;
        }
        String[] forms = {"yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm", "yyyy-MM-dd HH", "yyyy-MM-dd"};
        for (String form : forms) {
            try {
                DateTimeFormatter f = DateTimeFormatter.ofPattern(form);
                return form.contains("H")
                    ? LocalDateTime.parse(s, f)
                    : java.time.LocalDate.parse(s, f).atStartOfDay();
            } catch (Exception ignored) {
                // try the next shape
            }
        }
        return null;
    }

    /**
     * The weighing as HTML.
     *
     * <b>Static and package-private so the suite can walk it with no window open</b>, which is
     * the property that makes {@code WheelView} and {@code HoraryPanel.render} testable. It reads
     * no field of the panel.
     */
    static String render(Electional.Quality q, PlanetaryHours.Day day, Horary.Matter m,
                         ZonedDateTime when, Geocoder.Result where) {
        StringBuilder b = new StringBuilder();
        b.append("<html><body style='color:#E0E0E0; font-family:Arial; padding:16px;'>");

        b.append("<p style='color:#9AA5B1; font-size:11px;'>")
            .append(escape(when == null ? "" : when.format(STAMP)))
            .append(where == null || where.name == null ? "" : " &middot; " + escape(where.name))
            .append(m == null ? " &middot; no particular matter"
                : " &middot; " + escape(m.house + " - " + m.about))
            .append("</p>");

        if (q.unknown) {
            // The polar case. PlanetaryHours refuses to invent hours where the Sun neither rises
            // nor sets, and this says the same thing rather than showing an empty verdict.
            b.append("<h2 style='color:#B5A0E3;'>The hours cannot be divided here</h2>")
                .append("<p>The Sun neither rises nor sets at this place on this date, so there ")
                .append("is no sunrise to divide into unequal hours - and with no day or hour ")
                .append("ruler there is nothing for an election to be shaped by. The rest of the ")
                .append("chart can still be read; this measure simply does not apply.</p>")
                .append("</body></html>");
            return b.toString();
        }

        b.append("<h2 style='color:#B5A0E3;'>").append(standingLine(q)).append("</h2>");
        b.append("<p>").append(shapedFor(q)).append("</p>");

        appendNotes(b, q, Electional.Weight.RAISES, "What raises it", "#7FD1A0");
        appendNotes(b, q, Electional.Weight.LOWERS, "What lowers it", "#E68370");

        if (q.notes.isEmpty()) {
            b.append("<p style='color:#9AA5B1;'>Nothing in this chart raises or lowers the "
                + "moment: no benefic or malefic on an angle, and the Moon clear of the nodes. "
                + "The day and hour rulers above are the whole of what it is shaped for.</p>");
        }

        appendHours(b, day);
        b.append("</body></html>");
        return b.toString();
    }

    /** The verdict, as a sentence rather than an enum. */
    static String standingLine(Electional.Quality q) {
        int up = q.raises();
        int down = q.lowers();
        switch (q.standing()) {
            case FAVOURED:
                return "A favoured moment";
            case ILL_FAVOURED:
                return "An ill-favoured moment";
            case MIXED:
                return "A mixed moment - " + up + " raising it and " + down + " lowering it";
            case NEUTRAL:
                return "A neutral moment";
            default:
                return "The moment is not weighed";
        }
    }

    /** The day and hour rulers, and what each favours, in the reader's language. */
    static String shapedFor(Electional.Quality q) {
        StringBuilder s = new StringBuilder();
        s.append("A <b>").append(escape(q.hourRuler)).append("</b> hour on a <b>")
            .append(escape(q.dayRuler)).append("</b> day.");
        if (!q.dayFavours.isEmpty()) {
            s.append(" The day favours ").append(escape(q.dayFavours)).append('.');
        }
        if (!q.hourFavours.isEmpty()) {
            s.append(" The hour favours ").append(escape(q.hourFavours)).append('.');
        }
        // <b>The hour is the nearer of the two.</b> Said once, here, rather than left for the
        // reader to work out from two clauses that look equally weighted.
        s.append(" Where the two disagree, the hour is the closer of the two to the moment.");
        return s.toString();
    }

    private static void appendNotes(StringBuilder b, Electional.Quality q,
                                    Electional.Weight weight, String heading, String colour) {
        StringBuilder rows = new StringBuilder();
        for (Electional.Note n : q.notes) {
            if (n.weight == weight) {
                // The engine's own sentence, not a second telling of it.
                rows.append("<li>").append(escape(n.because)).append("</li>");
            }
        }
        if (rows.length() == 0) {
            return;
        }
        b.append("<h3 style='color:").append(colour).append("; margin-bottom:4px;'>")
            .append(heading).append("</h3><ul style='margin-top:4px;'>")
            .append(rows).append("</ul>");
    }

    /** The whole planetary day, so a reader can find a better hour rather than only judge this one. */
    private static void appendHours(StringBuilder b, PlanetaryHours.Day day) {
        if (day == null || day.hours.isEmpty()) {
            return;
        }
        b.append("<h3 style='color:#B5A0E3; margin-bottom:4px;'>The hours of this ")
            .append(escape(day.ruler)).append(" day</h3>")
            .append("<p style='color:#9AA5B1; font-size:11px; margin-top:0;'>Unequal hours, "
                + "sunrise to sunrise, in Chaldean order. They are sixty minutes long only at an "
                + "equinox.</p>")
            .append("<table cellspacing='0' cellpadding='4' style='font-size:12px;'>");
        for (PlanetaryHours.Hour h : day.hours) {
            String favours = Electional.favours(h.ruler);
            b.append("<tr><td style='color:#9AA5B1;'>").append(h.index).append("</td>")
                .append("<td style='color:#9AA5B1;'>").append(h.daytime ? "day" : "night")
                .append("</td><td><b>").append(escape(h.ruler)).append("</b></td>")
                .append("<td style='color:#9AA5B1;'>").append(Math.round(h.minutes()))
                .append(" min</td><td style='color:#9AA5B1;'>")
                .append(favours == null ? "" : escape(favours)).append("</td></tr>");
        }
        b.append("</table>");
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
