package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.TransitCalendar;
import de.thmac.swisseph.SwissEph;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * The transit calendar: a month of Chart A's transits, each day shaded by how much is happening.
 *
 * <p>Master list F2. The Calendar reading tab is the sky's year - ingresses, stations, lunations -
 * the same for everyone; this is one person's month. The engine is {@link TransitCalendar}, which
 * reads the transit search's passages a day at a time; this lays them out as a calendar and lists
 * a day's transits when it is clicked.
 */
public final class TransitCalendarPanel extends JPanel {

    private final OuraniaWindow window;
    private final SwissEph sw;
    final JLabel monthLabel = new JLabel(" ", JLabel.CENTER);
    final JLabel status = new JLabel(" ");
    final JButton prev = new JButton("◂");
    final JButton next = new JButton("▸");
    final JButton thisMonth = new JButton("This month");
    final JPanel grid = new JPanel(new GridLayout(7, 7, 4, 4));
    final JEditorPane detail = new JEditorPane();

    YearMonth shown = YearMonth.now();
    ZoneId zone = ZoneId.systemDefault();
    double orb = 1.0;
    volatile TransitCalendar.Month month;
    LocalDate selected;
    private ChartFrame chart;
    private SwingWorker<TransitCalendar.Month, Void> running;
    /** Day cells in grid order, 42 of them; null where the grid falls outside the month. */
    final DayCell[] cells = new DayCell[42];

    public TransitCalendarPanel(OuraniaWindow window) {
        this.window = window;
        this.sw = new SwissEph(Ephemeris.PATH);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JLabel title = new JLabel("Transit Calendar");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.TITLE);
        JLabel lede = new JLabel("Chart A's month: each day shaded by the transits in orb and those going exact.");
        lede.setForeground(Theme.TEXT_DIM);
        lede.setFont(Theme.SMALL);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        nav.setOpaque(false);
        for (JButton b : new JButton[] {prev, next, thisMonth}) {
            Widgets.styleButton(b, Widgets.Role.TRANSPORT);
        }
        prev.addActionListener(e -> showMonth(shown.minusMonths(1)));
        next.addActionListener(e -> showMonth(shown.plusMonths(1)));
        thisMonth.addActionListener(e -> showMonth(YearMonth.now()));
        monthLabel.setForeground(Theme.TEXT);
        monthLabel.setFont(Theme.font("Segoe UI", Font.BOLD, 16));
        monthLabel.setPreferredSize(new Dimension(170, 28));
        status.setForeground(Theme.TEXT_DIM);
        status.setFont(Theme.SMALL);
        nav.add(prev);
        nav.add(monthLabel);
        nav.add(next);
        nav.add(thisMonth);
        nav.add(legend("Intense", intenseColor()));
        nav.add(legend("Notable", notableColor()));
        nav.add(status);

        JPanel head = new JPanel();
        head.setLayout(new javax.swing.BoxLayout(head, javax.swing.BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.setBorder(Theme.pad(Theme.GAP_L, Theme.GAP_L, Theme.GAP, Theme.GAP_L));
        title.setAlignmentX(LEFT_ALIGNMENT);
        lede.setAlignmentX(LEFT_ALIGNMENT);
        nav.setAlignmentX(LEFT_ALIGNMENT);
        head.add(title);
        head.add(lede);
        head.add(nav);
        add(head, BorderLayout.NORTH);

        grid.setOpaque(false);
        grid.setBorder(Theme.pad(0, Theme.GAP_L, Theme.GAP_L, Theme.GAP));
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String n : names) {
            JLabel l = new JLabel(n, JLabel.CENTER);
            l.setForeground(Theme.TEXT_DIM);
            l.setFont(Theme.SMALL);
            grid.add(l);
        }
        for (int i = 0; i < 42; i++) {
            cells[i] = new DayCell();
            grid.add(cells[i]);
        }
        add(grid, BorderLayout.CENTER);

        detail.setContentType("text/html");
        detail.setEditable(false);
        detail.setBackground(Theme.SURFACE);
        javax.swing.JScrollPane side = HtmlPanes.scroller(detail);
        side.setPreferredSize(new Dimension(320, 400));
        side.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.EDGE));
        add(side, BorderLayout.EAST);
        layoutCells();
    }

    private static JLabel legend(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(c);
        l.setForeground(Theme.TEXT);
        l.setFont(Theme.SMALL);
        l.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return l;
    }

    static Color intenseColor() {
        return new Color(128, 44, 52);
    }

    static Color notableColor() {
        return new Color(104, 80, 32);
    }

    /** Reads Chart A from the wheel again; called whenever the screen is opened. */
    void refreshChart() {
        ChartFrame c = window == null || !window.wheelHasChartA() ? null : window.radixChartForSearch();
        setChart(c);
    }

    void setChart(ChartFrame c) {
        this.chart = c;
        showMonth(this.shown);
    }

    /** Which grid slot the first of a month falls in, Sunday first. */
    static int firstSlot(YearMonth ym) {
        return ym.atDay(1).getDayOfWeek().getValue() % 7;
    }

    void showMonth(YearMonth ym) {
        this.shown = ym;
        this.month = null;
        this.selected = null;
        monthLabel.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.getYear());
        layoutCells();
        if (this.chart == null) {
            status.setText(" ");
            detail.setText("<html><body style='font-family:Arial; color:#d8dae2; padding:10px;'><i>There is no Chart A on "
                + "the wheel. Cast one in Chart Setup to see its transits.</i></body></html>");
            return;
        }
        if (running != null) {
            // <b>Cancelled without interrupting.</b> Interrupting a thread while Swiss Ephemeris
            // reads its files closes the file channel under it for good (a Java NIO channel is
            // closed by an interrupt), and every later month on this panel then fails or stalls.
            // The stale worker finishes and its answer is dropped in done(), which checks shown.
            running.cancel(false);
        }
        status.setText("Working out " + monthLabel.getText() + "...");
        detail.setText("<html><body></body></html>");
        final ChartFrame natal = this.chart;
        final ZoneId z = this.zone;
        final double o = this.orb;
        running = new SwingWorker<TransitCalendar.Month, Void>() {
            @Override
            protected TransitCalendar.Month doInBackground() {
                return compute(natal, ym, z, o);
            }

            @Override
            protected void done() {
                if (isCancelled() || !ym.equals(shown)) {
                    return;
                }
                try {
                    month = get();
                } catch (Exception e) {
                    status.setText("Could not work out the month: " + e.getMessage());
                    return;
                }
                long intense = month.days.stream().filter(d -> d.level == TransitCalendar.Level.INTENSE).count();
                long notable = month.days.stream().filter(d -> d.level == TransitCalendar.Level.NOTABLE).count();
                status.setText(intense + " intense and " + notable + " notable days, orb " + trim(o) + "°, " + z.getId());
                layoutCells();
                LocalDate today = LocalDate.now(z);
                select(YearMonth.from(today).equals(ym) ? today : ym.atDay(1));
            }
        };
        running.execute();
    }

    /** Computes a month on the calling thread - for a check, which needs the answer before it goes on. */
    void showMonthNow(YearMonth ym) {
        this.shown = ym;
        this.selected = null;
        monthLabel.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.getYear());
        this.month = this.chart == null ? null : compute(this.chart, ym, this.zone, this.orb);
        layoutCells();
    }

    /**
     * One month at a time through this panel's ephemeris.
     *
     * <b>Swiss Ephemeris is not safe to use from two threads at once</b>, and a month is worked out
     * off the event thread: stepping to the next month while the last is still being worked out
     * started a second worker on the same instance, and a cancelled worker keeps running until it
     * next looks. Caught by TransitCalendarCheck crashing inside the ephemeris one run in three.
     *
     * <p><b>A separate SwissEph is not enough on its own</b>: the port's SweDate keeps one static
     * SwissEph for tidal acceleration that every instance calls through, so two instances on two
     * threads still collide (measured: a NullPointerException in SweDate.setGlobalTidalAcc). The
     * lock serialises this panel's months; the wider exposure is recorded in the handover.
     */
    TransitCalendar.Month compute(ChartFrame natal, YearMonth ym, ZoneId z, double o) {
        synchronized (sw) {
            return TransitCalendar.month(sw, natal, ym, z, o);
        }
    }

    private void layoutCells() {
        int first = firstSlot(shown);
        for (int i = 0; i < 42; i++) {
            int d = i - first + 1;
            cells[i].date = d >= 1 && d <= shown.lengthOfMonth() ? shown.atDay(d) : null;
            cells[i].day = null;
            if (cells[i].date != null && month != null && month.month.equals(shown)) {
                cells[i].day = month.days.get(d - 1);
            }
            cells[i].repaint();
        }
    }

    void select(LocalDate date) {
        this.selected = date;
        for (DayCell c : cells) {
            c.repaint();
        }
        detail.setText(detailHtml(date));
        detail.setCaretPosition(0);
    }

    String detailHtml(LocalDate date) {
        StringBuilder h = new StringBuilder("<html><body style='font-family:Arial; font-size:12px; color:#d8dae2; padding:10px;'>");
        h.append("<h3 style='color:#E2B258; margin:0 0 4px 0;'>")
            .append(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))).append("</h3>");
        TransitCalendar.Day day = month == null || !YearMonth.from(date).equals(month.month) ? null
            : month.days.get(date.getDayOfMonth() - 1);
        if (day == null) {
            return h.append("<p><i>Not worked out yet.</i></p></body></html>").toString();
        }
        h.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:8px;'>")
            .append(day.level == TransitCalendar.Level.INTENSE ? "An intense day" : day.level == TransitCalendar.Level.NOTABLE
                ? "A notable day" : "A quiet day")
            .append(String.format(", score %.1f", day.score)).append("</div>");
        if (day.contacts.isEmpty()) {
            return h.append("<p><i>No transits within orb.</i></p></body></html>").toString();
        }
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
        h.append("<table cellpadding='2'>");
        for (TransitCalendar.Contact c : day.contacts) {
            h.append("<tr><td>").append(c.passage.transiting).append(" ").append(c.passage.type.label.toLowerCase())
                .append(" natal ").append(c.passage.natal).append("</td><td style='color:#9AA5B1;'>");
            if (c.exactToday()) {
                long ms = Math.round((c.exactJd - 2440587.5) * 86400000.0);
                h.append("<b style='color:#E2B258;'>exact ").append(Instant.ofEpochMilli(ms).atZone(zone).format(time)).append("</b>");
            } else {
                h.append(String.format("%.2f&deg; off", c.offAtNoon));
            }
            h.append("</td></tr>");
        }
        return h.append("</table></body></html>").toString();
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    /** A short label for a contact, glyph to glyph: "♄ □ ☉". */
    static String shortLabel(TransitCalendar.Contact c) {
        return glyph(c.passage.transiting) + " " + aspectGlyph(c.passage.type) + " " + glyph(c.passage.natal);
    }

    static String glyph(String name) {
        Bodies.Def d = Bodies.byName(name);
        if ("Ascendant".equals(name)) {
            return "AC";
        }
        if ("MC".equals(name)) {
            return "MC";
        }
        return d == null || d.glyph == null || d.glyph.isEmpty() ? name.substring(0, 2) : d.glyph;
    }

    static String aspectGlyph(Aspects.Type t) {
        switch (t) {
            case CONJUNCTION: return "☌";
            case SEXTILE:     return "⚹";
            case SQUARE:      return "□";
            case TRINE:       return "△";
            case OPPOSITION:  return "☍";
            default:          return t.label.substring(0, 1);
        }
    }

    /** One day in the grid. */
    final class DayCell extends JComponent {
        LocalDate date;
        TransitCalendar.Day day;

        DayCell() {
            setPreferredSize(new Dimension(96, 70));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (date != null) {
                        select(date);
                    }
                }
            });
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (date == null) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color ground = Theme.SURFACE_2;
            if (day != null && day.level == TransitCalendar.Level.INTENSE) {
                ground = intenseColor();
            } else if (day != null && day.level == TransitCalendar.Level.NOTABLE) {
                ground = notableColor();
            }
            g.setColor(ground);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            boolean today = date.equals(LocalDate.now(zone));
            if (date.equals(selected) || today) {
                g.setColor(date.equals(selected) ? Theme.ACCENT : Theme.GOLD);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                g.setStroke(new BasicStroke(1f));
            }
            g.setColor(Theme.TEXT);
            g.setFont(Theme.font("Segoe UI", Font.BOLD, 12));
            g.drawString(String.valueOf(date.getDayOfMonth()), 7, 16);
            if (day != null) {
                g.setFont(Theme.font("Segoe UI", Font.PLAIN, 12));
                FontMetrics fm = g.getFontMetrics();
                int y = 34;
                for (int i = 0; i < Math.min(2, day.contacts.size()); i++) {
                    TransitCalendar.Contact c = day.contacts.get(i);
                    g.setColor(c.exactToday() ? Theme.GOLD : Theme.TEXT_DIM);
                    g.drawString(shortLabel(c), 7, y);
                    y += fm.getHeight();
                }
                if (day.contacts.size() > 2) {
                    g.setColor(Theme.TEXT_DIM);
                    String more = "+" + (day.contacts.size() - 2);
                    g.drawString(more, getWidth() - 8 - fm.stringWidth(more), 16);
                }
            }
            g.dispose();
        }
    }
}
