package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Horary;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A question, judged: master list F10, K13's fourth stage.
 *
 * <p>Everything K13 has built so far is engine and suite. {@link Horary#judge} returns a verdict
 * with the chain of reasons that produced it, in order, and 79 checks hold it there - and nothing
 * in the interface could show a reader a single one of them. That is the whole of why F10 is not
 * closed, and it is what this screen is.
 *
 * <p><b>Cast for the moment the question is asked, not for the chart that happens to be open.</b>
 * That is the convention rather than a convenience: a horary chart is the chart of the moment the
 * astrologer understands the question, so a question asked now is a chart cast now. The reader's
 * home place is where it is cast from - {@code home.location}, the same setting the sky ring
 * reads, so the two agree about where "here" is.
 *
 * <p><b>Nothing is added to what the engine said.</b> Each testimony carries its own sentence in
 * the reader's language ({@code Testimony.because}), written where the rule was applied and by the
 * code that applied it. This screen orders and presents them and writes no astrology of its own -
 * the single line it composes is the verdict, and that is a translation of an enum. Where the
 * chart is not radical it says so and gives no answer, which is the method's answer rather than a
 * failure to produce one.
 */
public final class HoraryPanel extends JPanel {

    /**
     * Built on the first question, not on construction.
     *
     * <b>Because constructing this panel is not asking it anything.</b> Every screen is built
     * when the window opens, and an ephemeris handle costs a file open and a first calculation;
     * NavigationCheck builds a whole window per screen it sweeps, and it went from 192 to 702
     * seconds when this was eager. A reader who never opens Horary should not pay for it either.
     */
    private SwissEph sw;

    private final JComboBox<String> matter = new JComboBox<>();
    private final JTextField place = new JTextField(18);
    private final JButton ask = new JButton("Ask now");
    private final JEditorPane out;

    private SwingWorker<Object[], Void> running;

    public HoraryPanel(OuraniaWindow window) {
        this.out = HtmlPanes.chartPane(window);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Horary");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("A question answered by the chart of the moment it is asked.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);
        lede.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        for (Horary.Matter m : Horary.Matter.values()) {
            this.matter.addItem(m.house + " - " + m.about);
        }
        Widgets.styleCombo(this.matter);
        Widgets.styleButton(this.ask, Widgets.Role.PRIMARY);
        this.matter.setToolTipText("<html><b>Which house the matter belongs to.</b><br>"
            + "The quesited is the ruler of this house; the querent is the Ascendant's ruler."
            + "</html>");
        this.place.setToolTipText("Where the question is asked from - your home place by default");
        PlaceField.attach(this.place);
        this.place.setText(Settings.get("home.location",
            Settings.get("default.transit.location", "")).trim());

        this.ask.addActionListener(e -> this.askNow());

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        head.setOpaque(false);
        head.add(title);
        head.add(lede);
        head.add(this.matter);
        head.add(this.place);
        head.add(this.ask);

        add(head, BorderLayout.NORTH);
        add(HtmlPanes.scroller(this.out), BorderLayout.CENTER);
        HtmlPanes.setHtml(this.out, "<p>Choose what the question is about, then ask. "
            + "The chart is cast for this moment.</p>");
    }

    private void askNow() {
        if (this.running != null && !this.running.isDone()) {
            return;
        }
        final Horary.Matter m = Horary.Matter.values()[Math.max(0, this.matter.getSelectedIndex())];
        final String typed = this.place.getText().trim();
        if (typed.isEmpty()) {
            HtmlPanes.setHtml(this.out, "<p>I do not know where you are asking from. Type a "
                + "place, or set a home location in Settings.</p>");
            return;
        }
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime utc = now.withZoneSameInstant(ZoneId.of("UTC"));
        final double jd = new SweDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
            utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0).getJulDay();
        this.ask.setEnabled(false);
        HtmlPanes.setHtml(this.out, "<p>Casting the chart for this moment...</p>");

        this.running = new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                // <b>The lookup is here and not on the button.</b> Geocoder.lookup blocks and
                // says so in its own javadoc; called from the action listener it would freeze
                // the window for the length of a network round trip.
                Geocoder.Result there = Geocoder.lookup(typed);
                if (there == null) {
                    return new Object[] {null, null};
                }
                // On this thread, and only once: askNow refuses to start a second worker while
                // one is running, so there is never a race for it.
                if (HoraryPanel.this.sw == null) {
                    HoraryPanel.this.sw = new SwissEph(Ephemeris.PATH);
                }
                ChartFrame f = ChartFrame.compute(HoraryPanel.this.sw, jd, there.lat, there.lon,
                    'P', false, 0.0);
                return new Object[] {
                    Horary.judge(HoraryPanel.this.sw, f, m, jd, there.lat, there.lon), there};
            }

            @Override
            protected void done() {
                HoraryPanel.this.ask.setEnabled(true);
                try {
                    Object[] r = get();
                    if (r[0] == null) {
                        HtmlPanes.setHtml(HoraryPanel.this.out,
                            "<p>I could not find " + escape(typed) + ".</p>");
                        return;
                    }
                    HtmlPanes.setHtml(HoraryPanel.this.out, render(
                        (Horary.Judgement) r[0], m, now, (Geocoder.Result) r[1]));
                    HoraryPanel.this.out.setCaretPosition(0);
                } catch (Exception ex) {
                    HtmlPanes.setHtml(HoraryPanel.this.out, "<p>The chart could not be cast: "
                        + escape(String.valueOf(ex.getMessage())) + "</p>");
                }
            }
        };
        this.running.execute();
    }

    /**
     * The judgement as HTML.
     *
     * <b>Static and package-private so the suite can walk it with no window open.</b> It reads
     * no field of the panel - the same property that makes WheelView testable.
     */
    static String render(Horary.Judgement j, Horary.Matter m, ZonedDateTime when,
                         Geocoder.Result where) {
        StringBuilder b = new StringBuilder();
        b.append("<h2>").append(verdictLine(j)).append("</h2>");
        b.append("<p><i>").append(escape(m.house + "th house - " + m.about)).append(", asked ")
            .append(escape(when.format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm"))))
            .append(" at ").append(escape(where.name)).append("</i></p>");

        b.append("<p><b>Who stands for whom.</b> ")
            .append(escape(String.valueOf(j.significators))).append("</p>");
        b.append("<p><b>Whether the chart may be judged.</b> ")
            .append(escape(String.valueOf(j.radicality))).append("</p>");

        // <b>The considerations before judgement, beside the verdict rather than instead of
        // it.</b> They refuse 26.7% of all moments if taken as bars, and David's call was that a
        // reader should be told what is doubtful about the moment rather than handed silence for
        // one question in four. So they are printed and the answer still comes.
        if (j.radicality != null && !j.radicality.cautions().isEmpty()) {
            b.append("<p><b>Worth knowing before you weigh it.</b></p><ul>");
            for (String caution : j.radicality.cautions()) {
                b.append("<li>").append(escape(caution)).append("</li>");
            }
            b.append("</ul>");
        }

        if (j.chain.isEmpty()) {
            b.append("<p>No testimony was found inside the window.</p>");
        } else {
            b.append("<p><b>The reasons, in the order the method found them.</b></p><ol>");
            for (Horary.Testimony t : j.chain) {
                b.append("<li>").append(escape(t.because));
                if (!Double.isNaN(t.jd)) {
                    b.append(" <i>(").append(escape(dayOf(t.jd))).append(")</i>");
                }
                b.append("</li>");
            }
            b.append("</ol>");
        }

        if (!Double.isNaN(j.windowEnds)) {
            b.append("<p><b>The window closes</b> ").append(escape(dayOf(j.windowEnds)));
            if (j.windowEndedBy != null) {
                b.append(", when ").append(escape(j.windowEndedBy));
            }
            b.append(".</p>");
        }
        return b.toString();
    }

    /** The verdict as a sentence, which is the one line of prose this screen composes. */
    static String verdictLine(Horary.Judgement j) {
        switch (j.verdict) {
            case YES:
                return "Yes.";
            case YES_WITH_DIFFICULTY:
                return "Yes, through difficulty.";
            case NO:
                return "No.";
            case NOT_RADICAL:
                return "The chart is not fit to judge.";
            default:
                return "Undecided.";
        }
    }

    static String dayOf(double jd) {
        SweDate d = new SweDate(jd);
        return String.format("%d-%02d-%02d", d.getYear(), d.getMonth(), d.getDay());
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
