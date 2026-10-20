package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;

import java.util.ArrayList;
import java.util.List;

/**
 * Coming up together (PairTimeline, 2026-10-20): which contacts count as reaching both people.
 *
 * <b>The pairing is the only new rule on the page</b>, so it is what this pins: the same planet,
 * within {@link PairTimeline#SHARED_DAYS} days, each contact used once, and never the Moon. The
 * two timelines it pairs are NowTimeline's, which NowCardsCheck already holds.
 */
public final class PairTimelineCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        Settings.useScratchFile();
        System.out.println("Running com.zodiacomputing.ourania.gui.PairTimelineCheck");

        NowTimeline.Entry satA = at("Saturn", "Sun", 100.0);
        NowTimeline.Entry satB = at("Saturn", "Moon", 104.0);
        NowTimeline.Entry satLate = at("Saturn", "Venus", 120.0);
        NowTimeline.Entry moonA = at("Moon", "Sun", 100.0);
        NowTimeline.Entry moonB = at("Moon", "Mars", 100.0);
        NowTimeline.Entry marsA1 = at("Mars", "Sun", 200.0);
        NowTimeline.Entry marsA2 = at("Mars", "Venus", 202.0);
        NowTimeline.Entry marsB = at("Mars", "Moon", 201.0);
        NowTimeline.Entry nowA = at("Jupiter", "Sun", Double.NaN);
        NowTimeline.Entry nowB = at("Jupiter", "Moon", Double.NaN);

        eq("the same planet four days apart reaches both", 1,
            PairTimeline.shared(List.of(satA), List.of(satB)).size());
        eq("twenty days apart it does not", 0,
            PairTimeline.shared(List.of(satA), List.of(satLate)).size());
        eq("the Moon reaching both is not news", 0,
            PairTimeline.shared(List.of(moonA), List.of(moonB)).size());
        eq("one contact pairs once, however often the other chart is reached", 1,
            PairTimeline.shared(List.of(marsA1, marsA2), List.of(marsB)).size());
        eq("right now for both is shared", 1,
            PairTimeline.shared(List.of(nowA), List.of(nowB)).size());
        eq("right now and a date are not", 0,
            PairTimeline.shared(List.of(nowA), List.of(satB)).size());

        String page = PairTimeline.html(List.of(satA, moonA), "Ana", List.of(satB, moonB),
            "Ben", false);
        yes("the page names the shared planet", page.contains("Saturn reaches both of you"));
        yes("and says what it means for each", page.contains("For Ana: ")
            && page.contains("For Ben: "));
        yes("then each person's own timeline", page.indexOf(">Ana</h2>") > 0
            && page.indexOf(">Ben</h2>") > page.indexOf(">Ana</h2>"));
        yes("with nothing shared the page says so", PairTimeline.html(List.of(moonA), "Ana",
            List.of(moonB), "Ben", false).contains("Nothing in the coming year"));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    private static NowTimeline.Entry at(String sky, String natal, double jd) {
        NowCards.TransitHit h = new NowCards.TransitHit(sky, 0.0, 1.0, 1, natal, 0.0, 1,
            Aspects.Type.CONJUNCTION, 0.5, true, false);
        NowTimeline.Window w = Double.isNaN(jd) ? NowTimeline.Window.RIGHT_NOW
            : NowTimeline.Window.LATER;
        return new NowTimeline.Entry(h, jd, w, new ReadingCard("LABEL",
            sky + " meets your " + natal, "Body.", List.of(sky + " on " + natal)));
    }

    private static void yes(String claim, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(claim);
            System.out.println("  FAIL: " + claim);
        } else {
            System.out.println("  ok: " + claim);
        }
    }

    private static void eq(String claim, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(claim + " (expected <" + expected + ">, got <" + actual + ">)");
            System.out.println("  FAIL: " + claim + " (expected <" + expected + ">, got <"
                + actual + ">)");
        } else {
            System.out.println("  ok: " + claim);
        }
    }
}
