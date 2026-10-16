package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;

import java.util.ArrayList;
import java.util.List;

/**
 * Check suite for NowCards (Plain-Tabs Phase 3).
 *
 * Verifies:
 * 1. Fast bodies (Moon, Sun, Mercury, Venus, Mars) are categorized as "This Week" (Catalysts).
 * 2. Slow outer bodies (Jupiter, Saturn, Uranus, Neptune, Pluto, Chiron) are categorized as "This Season" (The Long Arc).
 * 3. Timing badges are correctly calculated:
 *    - PEAK for orb <= 0.25 deg
 *    - BUILDING for applying aspects
 *    - WINDING DOWN for separating aspects
 * 4. Cards produce plain headlines and informative prose.
 * 5. Astrologer view gates technical degrees and placement coordinates.
 */
public final class NowCardsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        Settings.useScratchFile();
        System.out.println("Running com.zodiacomputing.ourania.gui.NowCardsCheck");

        testFastSlowClassification();
        testTimingBadges();
        testCardGeneration();
        testHtmlRendering();

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

    private static void testFastSlowClassification() {
        System.out.println("=== Part A: fast vs slow body classification ===");
        yes("Moon is fast catalyst", NowCards.FAST_BODIES.contains("Moon"));
        yes("Sun is fast catalyst", NowCards.FAST_BODIES.contains("Sun"));
        yes("Mercury is fast catalyst", NowCards.FAST_BODIES.contains("Mercury"));
        yes("Venus is fast catalyst", NowCards.FAST_BODIES.contains("Venus"));
        yes("Mars is fast catalyst", NowCards.FAST_BODIES.contains("Mars"));

        yes("Jupiter is slow long-arc", !NowCards.FAST_BODIES.contains("Jupiter"));
        yes("Saturn is slow long-arc", !NowCards.FAST_BODIES.contains("Saturn"));
        yes("Uranus is slow long-arc", !NowCards.FAST_BODIES.contains("Uranus"));
        yes("Neptune is slow long-arc", !NowCards.FAST_BODIES.contains("Neptune"));
        yes("Pluto is slow long-arc", !NowCards.FAST_BODIES.contains("Pluto"));
        yes("Chiron is slow long-arc", !NowCards.FAST_BODIES.contains("Chiron"));
    }

    private static void testTimingBadges() {
        System.out.println("=== Part B: dynamic timing badges ===");
        // Peak: orb <= 0.25 deg
        NowCards.TransitHit peakHit = new NowCards.TransitHit(
            "Mars", 45.1, 0.6, 1, "Sun", 45.0, 1,
            Aspects.Type.CONJUNCTION, 0.10, true, true
        );
        eq("orb <= 0.25 reports PEAK", "PEAK", peakHit.timingBadge());

        // Building: applying and orb > 0.25
        NowCards.TransitHit buildingHit = new NowCards.TransitHit(
            "Mars", 44.0, 0.6, 1, "Sun", 45.0, 1,
            Aspects.Type.CONJUNCTION, 1.0, true, true
        );
        eq("applying with orb > 0.25 reports BUILDING", "BUILDING", buildingHit.timingBadge());

        // Winding down: separating and orb > 0.25
        NowCards.TransitHit separatingHit = new NowCards.TransitHit(
            "Mars", 46.2, 0.6, 1, "Sun", 45.0, 1,
            Aspects.Type.CONJUNCTION, 1.2, false, true
        );
        eq("separating with orb > 0.25 reports WINDING DOWN", "WINDING DOWN", separatingHit.timingBadge());
    }

    private static void testCardGeneration() {
        System.out.println("=== Part C: card generation ===");
        NowCards.TransitHit hit = new NowCards.TransitHit(
            "Jupiter", 120.05, 0.08, 9, "Sun", 120.0, 5,
            Aspects.Type.CONJUNCTION, 0.05, false, false
        );
        ReadingCard card = NowCards.toCard(hit);
        yes("card label contains timing badge", card.label().contains("PEAK"));
        yes("card label contains season category", card.label().contains("THIS SEASON"));
        yes("card headline is populated", card.headline() != null && !card.headline().isEmpty());
        yes("card body is populated", card.body() != null && !card.body().isEmpty());
        yes("card why box contains technical details", !card.why().isEmpty());
        yes("why box mentions exact aspect", card.why().stream().anyMatch(w -> w.contains("Conjunction")));
    }

    private static void testHtmlRendering() {
        System.out.println("=== Part D: html rendering ===");
        int count = Bodies.count();
        WheelRing transitRing = WheelRing.of(WheelRing.Kind.SKY);
        WheelRing natalRing = WheelRing.of(WheelRing.Kind.CHART_A);

        int sunIdx = Bodies.indexOfName("Sun");
        int marsIdx = Bodies.indexOfName("Mars");
        int saturnIdx = Bodies.indexOfName("Saturn");

        natalRing.valid[sunIdx] = true;
        natalRing.lon[sunIdx] = 120.0; // Leo 0°

        transitRing.valid[marsIdx] = true;
        transitRing.lon[marsIdx] = 120.15; // Mars conjunct Sun, orb 0.15 (Peak)
        transitRing.speed[marsIdx] = 0.6;

        transitRing.valid[saturnIdx] = true;
        transitRing.lon[saturnIdx] = 239.2; // Saturn trine Sun, orb 0.8, applying
        transitRing.speed[saturnIdx] = 0.05;

        double[] cusps = new double[13];
        for (int i = 1; i <= 12; i++) {
            cusps[i] = (i - 1) * 30.0;
        }

        String plainHtml = NowCards.html(transitRing, natalRing, cusps, false, "<p>raw</p>");
        yes("plain view does not show raw coordinates", !plainHtml.contains("Placements \u00b7 Raw Coordinates"));
        yes("plain view has catalysts header", plainHtml.contains("This Week"));
        yes("plain view has PEAK badge", plainHtml.contains("PEAK"));

        String astrologerHtml = NowCards.html(transitRing, natalRing, cusps, true, "<p>raw coordinates table</p>");
        yes("astrologer view includes raw coordinates drawer", astrologerHtml.contains("Placements \u00b7 Raw Coordinates"));
        yes("astrologer view includes raw table content", astrologerHtml.contains("raw coordinates table"));
        yes("astrologer view includes why box", astrologerHtml.contains("Why &mdash; the astrology:"));
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
            System.out.println("  FAIL: " + claim + " (expected <" + expected + ">, got <" + actual + ">)");
        } else {
            System.out.println("  ok: " + claim);
        }
    }
}
