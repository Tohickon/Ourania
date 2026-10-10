package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.TransferOfLight;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification for the Overview page.
 *
 *  Part A - it is on the rail, and opening it produces the page rather than revealing an empty
 *           card. The rail's own comment records what happens otherwise: five reading tabs
 *           once opened onto panes nothing had written to.
 *  Part B - it opens at rest. Every card is collapsed, and no card's detail is on the page
 *           until a reader asks for it - which is the whole claim of progressive disclosure
 *           and the easiest thing to get silently wrong.
 *  Part C - a tap opens one card and only that card, and a second tap closes it again.
 *  Part D - the cards agree with the engines they came from. This is the one that matters:
 *           a summary that disagrees with the reading is two confident pages about one chart.
 *  Part E - no card is a caption with nothing behind it. Every card carries detail, and the
 *           plain sentence names the bodies the detail names.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.gui.OverviewPageCheck
 */
public final class OverviewPageCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            // The wheel computes on a worker; the page reads whatever the wheel holds, so this
            // waits for a chart to exist rather than for a fixed number of milliseconds.
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            waitFor(() -> sky.radixChart() != null, 20000);

            OverviewPanel panel = w.overviewPanel();
            yes("the window has an Overview page", panel != null);
            if (panel == null) {
                report();
                return;
            }

            System.out.println("=== Part A: it is on the rail, and opening it makes the page ===");
            onTheRail(w, panel);

            System.out.println();
            System.out.println("=== Part B: it opens at rest, every card collapsed ===");
            atRest(panel);

            System.out.println();
            System.out.println("=== Part C: a tap opens one card, and closes it again ===");
            oneTapOneCard(panel);

            System.out.println();
            System.out.println("=== Part D: the cards agree with the engines ===");
            agreesWithTheEngines(panel, sky);

            System.out.println();
            System.out.println("=== Part E: no card is a caption with nothing behind it ===");
            nothingIsACaption(panel);

            System.out.println();
            System.out.println("=== Part F: there is no 30th degree of any sign ===");
            noThirtiethDegree();
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }
        report();
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String s : failures) {
                System.out.println("  " + s);
            }
            // A Swing window keeps the JVM alive, so a suite that has built one has to say
            // when it is done - otherwise it prints its verdict and never exits, and the
            // per-suite timeout turns a pass into a red.
            System.exit(1);
        }
        System.exit(0);
    }

    // ---------------------------------------------------------------- part A

    private static void onTheRail(OuraniaWindow w, OverviewPanel panel) throws Exception {
        DrawerRail rail = w.chartRail();
        yes("the rail exists", rail != null);
        if (rail == null) {
            return;
        }
        // <b>Overview is behind Blueprint, not beside it.</b> It was one of twelve rail
        // tabs; it is one of Blueprint's eight pages. Both halves are asserted, because
        // "reachable" needs the tab AND the page - a page on a strip nobody can open is as
        // unreachable as no page at all.
        List<String> pages = pagesOf(rail);
        yes("Blueprint is one of the rail's three tabs: " + pages,
            pages.contains(OuraniaWindow.BLUEPRINT_PAGE));
        yes("Overview is one of Blueprint's pages: " + w.blueprintRail().pages(),
            w.blueprintRail().pages().contains(OuraniaWindow.OVERVIEW_PAGE));

        SwingUtilities.invokeAndWait(() -> rail.select(OuraniaWindow.OVERVIEW_PAGE));
        SwingUtilities.invokeAndWait(() -> { });
        String html = panel.html();
        yes("opening it produces a page", html != null && html.length() > 200);
        yes("and the page is the overview", html.contains("Overview"));
        yes("it is not the empty-chart message",
            !html.contains("No chart is cast yet"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> pagesOf(DrawerRail rail) throws Exception {
        List<String> out = new ArrayList<>();
        java.lang.reflect.Field f = DrawerRail.class.getDeclaredField("tabs");
        f.setAccessible(true);
        for (Object tab : (List<Object>) f.get(rail)) {
            java.lang.reflect.Field label = tab.getClass().getDeclaredField("label");
            label.setAccessible(true);
            out.add((String) label.get(tab));
        }
        return out;
    }

    // ---------------------------------------------------------------- part B

    private static void atRest(OverviewPanel panel) {
        List<OverviewPanel.Card> cards = panel.cards();
        yes("the chart produces cards at all: " + cards.size(), !cards.isEmpty());
        String html = panel.html();
        for (OverviewPanel.Card c : cards) {
            yes("the " + c.id + " card is closed to begin with", !panel.isExpanded(c.id));
            yes("and offers to open: " + c.id, html.contains("toggle:" + c.id));
            for (String line : c.detail) {
                // The detail is the point of the tap. If it is already on the page, the card
                // is not disclosing anything, it is just drawing a link.
                yes("no detail of " + c.id + " is on the page yet: " + shorten(line),
                    !html.contains(escaped(line)));
            }
        }
    }

    // ---------------------------------------------------------------- part C

    private static void oneTapOneCard(OverviewPanel panel) {
        List<OverviewPanel.Card> cards = panel.cards();
        if (cards.isEmpty()) {
            return;
        }
        OverviewPanel.Card first = cards.get(0);
        panel.follow("toggle:" + first.id);
        yes("the tapped card is open", panel.isExpanded(first.id));
        String html = panel.html();
        yes("its detail is on the page now",
            first.detail.isEmpty() || html.contains(escaped(first.detail.get(0))));
        yes("and it offers to close", html.contains("Hide the detail"));
        for (OverviewPanel.Card other : cards) {
            if (!other.id.equals(first.id)) {
                yes("the " + other.id + " card stayed closed", !panel.isExpanded(other.id));
            }
        }
        panel.follow("toggle:" + first.id);
        yes("a second tap closes it", !panel.isExpanded(first.id));
        yes("and its detail leaves the page again",
            first.detail.isEmpty() || !panel.html().contains(escaped(first.detail.get(0))));
    }

    // ---------------------------------------------------------------- part D

    /**
     * The summary and the reading read the same chart through the same engines.
     *
     * Asserted against the engine calls rather than against a remembered string, because the
     * failure being guarded is not "the wording changed" - it is the page quietly computing
     * something of its own and telling a reader a different thing from the reading does.
     */
    private static void agreesWithTheEngines(OverviewPanel panel, SkymapPanel sky) {
        ChartFrame f = sky.radixChart();
        yes("the wheel has a chart to compare against", f != null);
        if (f == null) {
            return;
        }
        Gestalt.Result g = Gestalt.compute(f);
        List<OverviewPanel.Card> cards = panel.cards();

        OverviewPanel.Card sect = card(cards, "sect");
        yes("there is a sect card", sect != null);
        if (sect != null) {
            eq("the sect card says what Gestalt says", g.diurnal ? "Day chart" : "Night chart",
                sect.subtitle);
            yes("and names the light of the sect: " + g.sectLight,
                String.join(" ", sect.detail).contains(g.sectLight));
        }

        List<TransferOfLight.Transfer> all =
            TransferOfLight.find(f, Aspects.betweenBodies(f));
        int translations = 0;
        int collections = 0;
        for (TransferOfLight.Transfer t : all) {
            if (t.kind == TransferOfLight.Kind.TRANSLATION) {
                translations++;
            } else {
                collections++;
            }
        }
        countAgrees(cards, "translation", translations, "translations of light");
        countAgrees(cards, "collection", collections, "collections of light");
    }

    private static void countAgrees(List<OverviewPanel.Card> cards, String id, int engine,
                                    String words) {
        OverviewPanel.Card c = card(cards, id);
        if (engine == 0) {
            yes("no " + id + " card when the engine found none", c == null);
            return;
        }
        yes("there is a " + id + " card, because the engine found " + engine, c != null);
        if (c != null) {
            eq("the " + id + " card counts what the engine found", engine + " " + words,
                c.subtitle);
        }
    }

    // ---------------------------------------------------------------- part E

    private static void nothingIsACaption(OverviewPanel panel) {
        for (OverviewPanel.Card c : panel.cards()) {
            yes("the " + c.id + " card has something behind it", !c.detail.isEmpty());
            yes("the " + c.id + " card says something in plain words",
                c.plain != null && c.plain.length() > 30);
            yes("the " + c.id + " card names itself", c.title != null && !c.title.isEmpty());
            // A classical name on the expanded side, so a reader who knows the term can find
            // it and a reader who does not is never shown it first.
            yes("the " + c.id + " card names its classical figure when it has one",
                !c.id.equals("translation") && !c.id.equals("collection")
                    || String.join(" ", c.detail).contains("Classical figure"));
        }
    }

    // ---------------------------------------------------------------- part F

    /**
     * A body within half a minute of a boundary is in the NEXT sign.
     *
     * <b>This page printed "30°00' cancer" the day it was written</b>, and so had
     * ChartTables, for as long as it has had a position formatter: both rounded the minutes,
     * carried 60 of them into the degree, and then asked for the sign name of the UNrounded
     * longitude. One formatter in Zodiac now, and this sweeps it - the assertion lives here
     * because this is where the fault was seen, and both callers are pages.
     */
    private static void noThirtiethDegree() {
        for (int sign = 0; sign < 12; sign++) {
            double boundary = sign * 30.0;
            com.zodiacomputing.ourania.astro.Zodiac.Position just =
                com.zodiacomputing.ourania.astro.Zodiac.position(boundary - 0.0001);
            eq("a whisker before " + boundary + " degrees is the 0th degree of the next sign",
                0, just.degree);
            eq("and its minute is 0", 0, just.minute);
            eq("and it is named for the sign it has reached",
                com.zodiacomputing.ourania.astro.Zodiac.signName(boundary), just.sign);
        }
        int bad = 0;
        for (double lon = 0.0; lon < 360.0; lon += 0.017) {
            com.zodiacomputing.ourania.astro.Zodiac.Position p =
                com.zodiacomputing.ourania.astro.Zodiac.position(lon);
            if (p.degree < 0 || p.degree > 29 || p.minute < 0 || p.minute > 59) {
                bad++;
            }
        }
        eq("no longitude in a full circle lands outside 0-29 degrees and 0-59 minutes", 0, bad);
    }

    // ---------------------------------------------------------------- harness

    private static OverviewPanel.Card card(List<OverviewPanel.Card> cards, String id) {
        for (OverviewPanel.Card c : cards) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** What the page does to a line before it renders it, so the two are compared alike. */
    private static String escaped(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String shorten(String s) {
        return s.length() <= 40 ? s : s.substring(0, 40) + "...";
    }

    private static void waitFor(java.util.function.BooleanSupplier until, long limitMs)
            throws Exception {
        long end = System.nanoTime() + limitMs * 1_000_000L;
        while (System.nanoTime() < end) {
            if (until.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }
}
