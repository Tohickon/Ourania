package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.TransitCalendar;
import com.zodiacomputing.ourania.astro.TransitSearch;
import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Master list F2: the personal transit calendar.
 *
 * <p>The calendar is held to the transit search it reads and to positions taken from the ephemeris
 * here: every exact moment on the day it happens in the reader's zone, no transit in orb missing
 * from a day, each day's score exactly its stated definition, and the levels by their rule.
 */
public final class TransitCalendarCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        ChartFrame natal = ChartFrame.compute(sw, SweDate.getJulDay(1982, 8, 10, 19.0 + 1.0 / 60.0),
            39.9526, -75.1652, 'P', false, 0.0);
        part("A: a month, against the search and the ephemeris", () -> month(sw, natal));
        part("B: the reader's zone decides the day", () -> zones(sw, natal));
        part("C: a slow planet's exact day is not a quiet one", () -> slow(sw, natal));
        part("D: the screen", () -> screen(natal));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    private static void month(SwissEph sw, ChartFrame natal) {
        ZoneId ny = ZoneId.of("America/New_York");
        double orb = 1.0;
        int exactsSeen = 0;
        int contacts = 0;
        int wrongExact = 0;
        int missingExact = 0;
        int missingContact = 0;
        int wrongOff = 0;
        int wrongScore = 0;
        int ruleBroken = 0;
        int intenseTotal = 0;
        for (YearMonth ym : new YearMonth[] {YearMonth.of(2026, 9), YearMonth.of(2027, 3), YearMonth.of(2028, 2)}) {
            TransitCalendar.Month m = TransitCalendar.month(sw, natal, ym, ny, orb);
            ok(ym + " has a day for every date, " + m.days.size(), m.days.size() == ym.lengthOfMonth());
            boolean contiguous = true;
            for (int i = 0; i < m.days.size(); i++) {
                TransitCalendar.Day d = m.days.get(i);
                contiguous &= d.date.equals(ym.atDay(i + 1))
                    && Math.abs(d.startJd - TransitCalendar.jdOf(d.date, ny)) < 1e-9
                    && (i == 0 || Math.abs(d.startJd - m.days.get(i - 1).endJd) < 1e-9);
            }
            ok(ym + ": each day runs from its local midnight to the next, with no gaps", contiguous);

            for (TransitCalendar.Day d : m.days) {
                double noon = (d.startJd + d.endJd) / 2.0;
                double sum = 0;
                for (TransitCalendar.Contact c : d.contacts) {
                    contacts++;
                    sum += c.score;
                    TransitSearch.Passage p = c.passage;
                    // The planet's place at noon, straight from the ephemeris.
                    double[] xx = new double[6];
                    sw.swe_calc_ut(noon, Almanac.iplOf(p.transiting),
                        Ephemeris.flags(sw, SweConst.SEFLG_SWIEPH), xx, new StringBuffer());
                    double off = Math.abs(Aspects.separation(xx[0], p.natalLongitude) - p.type.exactAngle);
                    if (Math.abs(off - c.offAtNoon) > 0.01) {
                        wrongOff++;
                    }
                    double w = TransitCalendar.bodyWeight(p.transiting) * TransitCalendar.aspectWeight(p.type)
                        * TransitCalendar.pointWeight(p.natal);
                    double want = w * Math.max(0, 1 - c.offAtNoon / orb) + (c.exactToday() ? w : 0);
                    if (Math.abs(want - c.score) > 1e-9) {
                        wrongScore++;
                    }
                    if (c.exactToday() && (c.exactJd < d.startJd || c.exactJd >= d.endJd)) {
                        wrongExact++;
                    }
                }
                if (Math.abs(sum - d.score) > 1e-9) {
                    wrongScore++;
                }
                // Nothing in orb is left off the day.
                for (TransitSearch.Passage p : m.passages) {
                    boolean exactToday = false;
                    for (TransitSearch.Exact e : p.exacts) {
                        exactToday |= e.jd >= d.startJd && e.jd < d.endJd;
                    }
                    double off = TransitCalendar.offExact(sw, p, noon);
                    boolean overlaps = (Double.isNaN(p.enters) || p.enters < d.endJd) && (Double.isNaN(p.leaves) || p.leaves >= d.startJd);
                    boolean counts = overlaps && (exactToday || off < orb);
                    boolean listed = d.contacts.stream().anyMatch(c -> c.passage == p);
                    if (counts && !listed) {
                        missingContact++;
                    }
                }
            }
            // Every exact moment the search found inside the month is on its day.
            for (TransitSearch.Passage p : m.passages) {
                for (TransitSearch.Exact e : p.exacts) {
                    if (e.jd < m.days.get(0).startJd || e.jd >= m.days.get(m.days.size() - 1).endJd) {
                        continue;
                    }
                    exactsSeen++;
                    boolean found = false;
                    for (TransitCalendar.Day d : m.days) {
                        for (TransitCalendar.Contact c : d.contacts) {
                            found |= c.passage == p && c.exactJd == e.jd;
                        }
                    }
                    if (!found) {
                        missingExact++;
                    }
                }
            }
            // The levels, by their rule.
            List<Double> scoring = new ArrayList<>();
            for (TransitCalendar.Day d : m.days) {
                if (d.score > 0) {
                    scoring.add(d.score);
                }
            }
            int intense = 0;
            double lowestIntense = Double.POSITIVE_INFINITY;
            double highestOther = 0;
            for (TransitCalendar.Day d : m.days) {
                switch (d.level) {
                    case INTENSE:
                        intense++;
                        lowestIntense = Math.min(lowestIntense, d.score);
                        ruleBroken += d.score >= TransitCalendar.INTENSE_FLOOR ? 0 : 1;
                        break;
                    case NOTABLE:
                        highestOther = Math.max(highestOther, d.score);
                        ruleBroken += d.score >= TransitCalendar.NOTABLE_FLOOR ? 0 : 1;
                        break;
                    default:
                        highestOther = Math.max(highestOther, d.score);
                        // A quiet day is one under the notable floor; nothing at or above it is quiet.
                        ruleBroken += d.score < TransitCalendar.NOTABLE_FLOOR ? 0 : 1;
                }
            }
            intenseTotal += intense;
            ok(ym + ": no more intense days than a fifth of the scoring days, " + intense + " of " + scoring.size(),
                intense <= Math.ceil(scoring.size() * TransitCalendar.INTENSE_SHARE));
            ok(ym + ": every intense day outscores every day that is not", intense == 0 || lowestIntense >= highestOther);
        }
        ok("the three months have exact transits to test, " + exactsSeen, exactsSeen >= 20);
        ok("and days in orb, " + contacts + " contacts", contacts >= 100);
        ok("every exact moment is on its day, " + missingExact + " missing", missingExact == 0);
        ok("and on no other day, " + wrongExact + " misplaced", wrongExact == 0);
        ok("no transit in orb is missing from a day, " + missingContact + " missing", missingContact == 0);
        ok("each distance from exact at noon agrees with the ephemeris, " + wrongOff + " off", wrongOff == 0);
        ok("each score is its definition, and each day the sum, " + wrongScore + " wrong", wrongScore == 0);
        ok("each level is by its floor, " + ruleBroken + " broken", ruleBroken == 0);
        ok("the months have intense days at all, " + intenseTotal, intenseTotal > 0);
        eq("February 2028 has 29 days", 29, TransitCalendar.month(sw, natal, YearMonth.of(2028, 2), ny, orb).days.size());
    }

    private static void zones(SwissEph sw, ChartFrame natal) {
        YearMonth ym = YearMonth.of(2026, 11);
        int checked = 0;
        int wrong = 0;
        int moved = 0;
        for (ZoneId z : new ZoneId[] {ZoneId.of("UTC"), ZoneId.of("Pacific/Kiritimati")}) {
            TransitCalendar.Month m = TransitCalendar.month(sw, natal, ym, z, 1.0);
            for (TransitCalendar.Day d : m.days) {
                for (TransitCalendar.Contact c : d.contacts) {
                    if (!c.exactToday()) {
                        continue;
                    }
                    checked++;
                    LocalDate local = Instant.ofEpochMilli(Math.round((c.exactJd - 2440587.5) * 86400000.0)).atZone(z).toLocalDate();
                    LocalDate utc = Instant.ofEpochMilli(Math.round((c.exactJd - 2440587.5) * 86400000.0)).atZone(ZoneId.of("UTC")).toLocalDate();
                    if (!local.equals(d.date)) {
                        wrong++;
                    }
                    if (!local.equals(utc)) {
                        moved++;
                    }
                }
            }
        }
        ok("exact moments were checked in two zones fourteen hours apart, " + checked, checked >= 10);
        ok("each is on its date in the reader's zone, " + wrong + " wrong", wrong == 0);
        ok("and some change date between the two, as they must, " + moved, moved > 0);
    }

    private static void slow(SwissEph sw, ChartFrame natal) {
        ZoneId z = ZoneId.of("UTC");
        List<TransitSearch.Passage> year = TransitSearch.search(sw, natal, java.util.Arrays.asList("Saturn", "Jupiter"),
            TransitCalendar.NATAL, java.util.Arrays.asList(TransitSearch.MAJOR), 1.0,
            TransitCalendar.jdOf(LocalDate.of(2026, 1, 1), z), TransitCalendar.jdOf(LocalDate.of(2027, 1, 1), z));
        int tested = 0;
        int quiet = 0;
        for (TransitSearch.Passage p : year) {
            for (TransitSearch.Exact e : p.exacts) {
                LocalDate date = Instant.ofEpochMilli(Math.round((e.jd - 2440587.5) * 86400000.0)).atZone(z).toLocalDate();
                if (date.getYear() != 2026 || tested >= 4) {
                    continue;
                }
                tested++;
                TransitCalendar.Month m = TransitCalendar.month(sw, natal, YearMonth.from(date), z, 1.0);
                TransitCalendar.Day d = m.days.get(date.getDayOfMonth() - 1);
                if (d.level == TransitCalendar.Level.QUIET) {
                    quiet++;
                }
                ok(p.transiting + " " + p.type.label.toLowerCase() + " natal " + p.natal + " on " + date
                    + " is listed exact that day", d.contacts.stream().anyMatch(c -> c.passage.transiting.equals(p.transiting)
                        && c.passage.natal.equals(p.natal) && c.exactToday()));
            }
        }
        ok("a year of Saturn and Jupiter gives exact days to test, " + tested, tested >= 2);
        ok("none of them is a quiet day, " + quiet + " quiet", quiet == 0);
    }

    private static void screen(ChartFrame natal) throws Exception {
        eq("September 2026 begins on a Tuesday, the third slot", 2, TransitCalendarPanel.firstSlot(YearMonth.of(2026, 9)));
        eq("February 2026 begins on a Sunday, the first", 0, TransitCalendarPanel.firstSlot(YearMonth.of(2026, 2)));
        final TransitCalendarPanel[] v = new TransitCalendarPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            v[0] = new TransitCalendarPanel(null);
            v[0].zone = ZoneId.of("America/New_York");
            v[0].setChart(null);
        });
        TransitCalendarPanel p = v[0];
        ok("with no Chart A the screen says so", p.detail.getText().contains("no Chart A"));
        SwingUtilities.invokeAndWait(() -> {
            p.setChart(natal);
            p.showMonthNow(YearMonth.of(2026, 9));
            p.setSize(1000, 700);
            p.doLayout();
            p.grid.doLayout();
        });
        ok("the month is worked out", p.month != null && p.month.days.size() == 30);
        ok("the slots before the first are empty", p.cells[0].date == null && p.cells[1].date == null);
        eq("the first is in the third slot", LocalDate.of(2026, 9, 1), p.cells[2].date);
        eq("the thirtieth in the thirty-second", LocalDate.of(2026, 9, 30), p.cells[31].date);
        ok("and nothing after", p.cells[32].date == null);
        boolean matched = true;
        for (int i = 2; i <= 31; i++) {
            matched &= p.cells[i].day == p.month.days.get(i - 2);
        }
        ok("each cell holds its own day", matched);
        eq("the heading names the month", "September 2026", p.monthLabel.getText());

        // Click the busiest day.
        int best = 0;
        for (int i = 1; i < p.month.days.size(); i++) {
            if (p.month.days.get(i).score > p.month.days.get(best).score) {
                best = i;
            }
        }
        final TransitCalendar.Day busiest = p.month.days.get(best);
        final TransitCalendarPanel.DayCell cell = p.cells[best + 2];
        SwingUtilities.invokeAndWait(() -> cell.dispatchEvent(new MouseEvent(cell, MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1)));
        eq("clicking a day selects it", busiest.date, p.selected);
        String html = p.detail.getText();
        int named = 0;
        for (TransitCalendar.Contact c : busiest.contacts) {
            if (html.contains(c.passage.transiting) && html.contains(c.passage.natal)) {
                named++;
            }
        }
        ok("and lists every transit on it, " + named + " of " + busiest.contacts.size(),
            named == busiest.contacts.size() && !busiest.contacts.isEmpty());
        ok("with the exact ones timed", busiest.contacts.stream().noneMatch(TransitCalendar.Contact::exactToday) || html.contains("exact"));
        eq("a short label is glyph, aspect, glyph", "♄ □ ☉", TransitCalendarPanel.glyph("Saturn") + " "
            + TransitCalendarPanel.aspectGlyph(Aspects.Type.SQUARE) + " " + TransitCalendarPanel.glyph("Sun"));

        SwingUtilities.invokeAndWait(() -> p.prev.doClick());
        eq("the back arrow goes to the month before", YearMonth.of(2026, 8), p.shown);
        SwingUtilities.invokeAndWait(() -> p.next.doClick());
        SwingUtilities.invokeAndWait(() -> p.next.doClick());
        eq("and the forward arrow to the month after", YearMonth.of(2026, 10), p.shown);
        // <b>Months stepped through faster than they are worked out.</b> Each arrow cancels the
        // month before; a cancellation that interrupted the worker closed the ephemeris's file
        // channel under it, and the month finally asked for never arrived. So: step through four
        // months without waiting, then wait for the last, and hold it to a month worked out fresh.
        SwingUtilities.invokeAndWait(() -> {
            p.next.doClick();
            p.next.doClick();
            p.prev.doClick();
            p.next.doClick();
        });
        YearMonth last = YearMonth.of(2026, 12);
        eq("four quick steps land on December", last, p.shown);
        long deadline = System.currentTimeMillis() + 180_000;
        while (System.currentTimeMillis() < deadline) {
            TransitCalendar.Month got = p.month;
            if (got != null && got.month.equals(last)) {
                break;
            }
            Thread.sleep(250);
        }
        TransitCalendar.Month arrived = p.month;
        ok("after months are cancelled mid-way, the month asked for still arrives", arrived != null && arrived.month.equals(last));
        if (arrived != null && arrived.month.equals(last)) {
            // Through the panel's own lock: Swiss Ephemeris shares static state between instances,
            // so a fresh instance on this thread would race any stale worker still finishing.
            TransitCalendar.Month fresh = p.compute(natal, last, p.zone, p.orb());
            boolean same = fresh.days.size() == arrived.days.size();
            for (int i = 0; same && i < fresh.days.size(); i++) {
                same = Math.abs(fresh.days.get(i).score - arrived.days.get(i).score) < 1e-9
                    && fresh.days.get(i).level == arrived.days.get(i).level;
            }
            ok("and is the same month a fresh ephemeris works out, day by day", same);
        }

        ok("the menu offers the Transit Calendar",
            java.util.Arrays.stream(SidePanel.SCREENS).anyMatch(s -> s[1].equals("TRANSIT_CALENDAR")));
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SwingUtilities.invokeAndWait(() -> w[0].switchScreen("TRANSIT_CALENDAR"));
            java.lang.reflect.Field f = OuraniaWindow.class.getDeclaredField("transitCalendarPanel");
            f.setAccessible(true);
            TransitCalendarPanel panel = (TransitCalendarPanel) f.get(w[0]);
            ok("opening it shows the screen", panel != null && panel.isVisible());
            ok("and follows whether the wheel holds a Chart A",
                w[0].wheelHasChartA() ? !panel.detail.getText().contains("no Chart A") : panel.detail.getText().contains("no Chart A"));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void eq(String label, Object want, Object got) {
        ok(label + ": got " + got + ", expected " + want, want == null ? got == null : want.equals(got));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
