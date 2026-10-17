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
        testTransitCards();

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

        // The plain page is made of TransitCards: a part of life and a direction.
        yes("plain view names the part of life, " + plainHtml.length(),
            plainHtml.contains("FUN, ROMANCE, CREATIVITY AND CHILDREN"));
        yes("plain view says what the sky does to which need",
            plainHtml.contains("A push of energy fuses with your sense of who you are"));
        yes("plain view says what to do now", plainHtml.contains(TransitCard.WHAT_TO_DO));
        yes("plain view shows no corpus jargon", !plainHtml.contains("transiting")
            && !plainHtml.contains("Natal") && !plainHtml.contains("Orb:"));
        yes("astrologer view keeps the corpus card", astrologerHtml.contains("[ PEAK ]"));
    }

    private static void testTransitCards() {
        System.out.println("=== Part E: the plain transit card (2026-10-17) ===");
        InterpretationService svc = InterpretationService.getInstance();
        String[] sky = {"sun", "moon", "mercury", "venus", "mars", "jupiter", "saturn", "uranus",
            "neptune", "pluto", "chiron"};
        String[] aspects = {"conjunction", "sextile", "square", "trine", "opposition"};
        String[] natal = {"sun", "moon", "mercury", "venus", "mars", "jupiter", "saturn", "uranus",
            "neptune", "pluto", "chiron", "north_node", "south_node"};
        List<String> missing = new ArrayList<>();
        List<String> unfinished = new ArrayList<>();
        java.util.Set<String> todos = new java.util.HashSet<>();
        for (String s : sky) {
            for (String key : new String[] {"trigger." + s, "acts." + s}) {
                need(svc, key, missing);
            }
            ends(svc.getTransitCardPart("acts." + s), "acts." + s, unfinished);
            // The aspect verbs are singular ("clashes with"), so a trigger joined by "and" must
            // be one thing: "A rush of thoughts and messages", not "Busy thoughts and messages".
            String trigger = svc.getTransitCardPart("trigger." + s);
            if (trigger != null && trigger.contains(" and ") && !trigger.matches("^(A|An|The) .*")) {
                unfinished.add("trigger." + s + " reads as plural");
            }
            for (String a : aspects) {
                String todo = need(svc, "todo." + s + "." + a, missing);
                ends(todo, "todo." + s + "." + a, unfinished);
                if (todo != null) {
                    todos.add(todo);
                }
            }
        }
        for (String a : aspects) {
            need(svc, "aspect." + a, missing);
            ends(need(svc, "feel." + a, missing), "feel." + a, unfinished);
        }
        for (String n : natal) {
            String need = need(svc, "need." + n, missing);
            if (need != null && !Character.isLowerCase(need.charAt(0))) {
                unfinished.add("need." + n + " must read mid-sentence");
            }
        }
        eq("every part a card needs is written", "[]", missing.toString());
        eq("and each sentence ends as a sentence", "[]", unfinished.toString());
        eq("all 55 directions are different", 55, todos.size());
        for (Aspects.Type t : Aspects.Type.values()) {
            String verb = svc.getTransitCardPart("aspect." + t.label.toLowerCase(java.util.Locale.ROOT));
            boolean major = java.util.List.of("conjunction", "sextile", "square", "trine",
                "opposition").contains(t.label.toLowerCase(java.util.Locale.ROOT));
            if ((verb != null) != major) {
                missing.add("aspect." + t.label + (major ? " missing" : " must not be written"));
            }
        }
        eq("the file holds the five majors and no other aspect", "[]", missing.toString());

        NowCards.TransitHit pluto = new NowCards.TransitHit("Pluto", 304.0, 0.01, 2, "Mars", 214.8,
            11, Aspects.Type.SQUARE, 1.2, true, false);
        ReadingCard c = TransitCard.of(pluto);
        yes("a major transit to a written need makes a card", c != null);
        if (c != null) {
            eq("its label is the part of life, then the timing",
                "FRIENDS, GROUPS AND HOPES FOR THE FUTURE · BUILDING THIS SEASON", c.label());
            eq("its headline is trigger, contact and need",
                "Deep pressure to change clashes with how you stand up for yourself", c.headline());
            yes("its body says what the planet does and how it feels",
                c.body().startsWith(svc.getTransitCardPart("acts.pluto") + " "
                    + svc.getTransitCardPart("feel.square")));
            yes("and ends on what to do, in its own paragraph", c.body().endsWith(
                "\n\n" + TransitCard.WHAT_TO_DO + svc.getTransitCardPart("todo.pluto.square")));
            yes("the why box is the Now page's own", c.why().equals(NowCards.whyLines(pluto)));
            String html = ReadingCard.html(List.of(c), false);
            yes("the direction renders as its own paragraph",
                html.contains("<p style='font-size:13px; margin-top:6px;'>" + TransitCard.WHAT_TO_DO));
        }
        eq("a fast contact at its peak reads as this week", "at its peak this week",
            TransitCard.timing(new NowCards.TransitHit("Venus", 214.9, 1.2, 11, "Mars", 214.8, 11,
                Aspects.Type.CONJUNCTION, 0.1, true, true)));
        eq("a separating one is fading", "fading this season",
            TransitCard.timing(new NowCards.TransitHit("Neptune", 1, 0.01, 4, "Sun", 1, 9,
                Aspects.Type.TRINE, 0.9, false, false)));
        yes("a minor aspect makes no plain card", TransitCard.of(new NowCards.TransitHit("Sun",
            0, 1, 1, "Uranus", 36, 12, Aspects.Type.DECILE, 0.02, true, true)) == null);
        yes("a natal point with no written need makes none", TransitCard.of(
            new NowCards.TransitHit("Sun", 0, 1, 1, "Ceres", 0, 1, Aspects.Type.CONJUNCTION, 0.1,
                true, true)) == null);
        yes("nor does a sky point with no trigger", TransitCard.of(new NowCards.TransitHit(
            "North Node", 0, -0.05, 1, "Moon", 90, 5, Aspects.Type.SQUARE, 0.5, true, false)) == null);
    }

    private static String need(InterpretationService svc, String key, List<String> missing) {
        String v = svc.getTransitCardPart(key);
        if (v == null || v.isBlank()) {
            missing.add(key);
            return null;
        }
        return v;
    }

    private static void ends(String s, String key, List<String> unfinished) {
        if (s != null && !s.endsWith(".")) {
            unfinished.add(key);
        }
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
