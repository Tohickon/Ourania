package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * No astrology jargon in what a plain card says (plain-English tabs, Phase 5 guard).
 *
 * <b>The list is David's</b>, from his source document for the plain tabs ("The structure
 * needed", the "Frontend Banned Words Filter"), and the plan of 2026-10-13 named this suite.
 * It was not built, and on 2026-10-17 David asked: "didnt i give you banned words?" The words
 * may stand in the "why" drawer, which is where the real terms belong; they may not stand in
 * a card's label, headline or body.
 *
 * <b>What it reads.</b> Part A: every value of the transit card parts. Part B: every card those
 * parts can build, for each sky planet, natal point and aspect. Part C: the Now page in plain
 * view, filled and empty. Part D: the words a pop-up paints, which are the card's own.
 */
public final class JargonCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** David's list. Whole words, any case; the patterns take the plural and other endings. */
    static final String[] BANNED = {
        // Astrological mechanics
        "profections?", "lords? of the year", "time-?lords?", "dispositors?", "sect", "diurnal",
        "houses?", "cusps?", "decans?", "chaldean faces?", "sabian symbols?", "sabian", "septiles?",
        // Aspect and transit words
        "conjunctions?", "conjunct", "squares?", "trines?", "sextiles?", "oppositions?",
        "inconjuncts?", "orbs?", "applying", "separating", "transit\\w*", "progress(ed|ion\\w*)",
        "solar arcs?", "solar returns?", "translation of light", "collection of light",
        // Mystical jargon
        "karmic", "alchemical", "sacred catalysts?", "vibrational frequenc(y|ies)", "cosmic web",
        "akashic", "destiny"
    };

    private static final Pattern WORDS = Pattern.compile(
        "(?i)\\b(" + String.join("|", BANNED) + ")\\b");

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("Part A: the transit card parts", JargonCheck::theParts);
        part("Part B: every card the parts can build", JargonCheck::everyCard);
        part("Part C: the Now page in plain view", JargonCheck::thePage);
        part("Part D: the words a pop-up paints", JargonCheck::thePopups);

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

    /** The banned words in a piece of plain text, each once, in the order they appear. */
    static Set<String> banned(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) {
            return out;
        }
        Matcher m = WORDS.matcher(text);
        while (m.find()) {
            out.add(m.group(1).toLowerCase(java.util.Locale.ROOT));
        }
        return out;
    }

    private static void clean(String where, String text) {
        Set<String> found = banned(text);
        checks++;
        if (!found.isEmpty()) {
            failures.add(where + " says " + found + ": " + text);
            System.out.println("  FAIL " + where + " says " + found);
        }
    }

    // ------------------------------------------------------------------ A

    private static void theParts() throws Exception {
        // The guard itself, so a broken pattern cannot pass everything.
        yes("the guard finds a banned word", banned("Saturn squares your Moon").contains("squares"));
        yes("and its plural and endings", banned("Transiting houses").size() == 2);
        yes("in any case", !banned("THE SEXTILE").isEmpty());
        yes("but not inside a longer word", banned("household rules and a squarely set orbit").isEmpty());

        Path file = Path.of(InterpretationService.DATA_DIR + "transit_cards.json");
        if (!Files.exists(file)) {
            file = Path.of("src/main/resources/data/transit_cards.json");
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Matcher entry = Pattern.compile("\"([a-z_.]+)\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        int values = 0;
        while (entry.find()) {
            values++;
            clean("transit_cards.json " + entry.group(1), entry.group(2));
        }
        yes("the parts were read, " + values, values >= 100);
    }

    // ------------------------------------------------------------------ B

    private static final String[] SKY = {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter",
        "Saturn", "Uranus", "Neptune", "Pluto", "Chiron"};
    private static final String[] NATAL = {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter",
        "Saturn", "Uranus", "Neptune", "Pluto", "Chiron", "North Node", "South Node"};
    private static final Aspects.Type[] MAJORS = {Aspects.Type.CONJUNCTION, Aspects.Type.SEXTILE,
        Aspects.Type.SQUARE, Aspects.Type.TRINE, Aspects.Type.OPPOSITION};

    private static void everyCard() {
        int built = 0;
        for (String sky : SKY) {
            for (String natal : NATAL) {
                for (Aspects.Type type : MAJORS) {
                    for (int house = 1; house <= 12; house += 11) {
                        for (boolean fast : new boolean[] {true, false}) {
                            ReadingCard c = TransitCard.of(new NowCards.TransitHit(sky, 10, 1,
                                house, natal, 10, house, type, 0.1, true, fast));
                            if (c == null) {
                                failures.add("no card for " + sky + " " + type + " " + natal);
                                checks++;
                                continue;
                            }
                            built++;
                            String where = sky + " " + type.label + " " + natal;
                            clean(where + " label", c.label());
                            clean(where + " headline", c.headline());
                            clean(where + " body", c.body());
                        }
                    }
                }
            }
        }
        yes("every sky planet, natal point and aspect has a card, " + built,
            built == SKY.length * NATAL.length * MAJORS.length * 2 * 2);
    }

    // ------------------------------------------------------------------ C

    private static void thePage() {
        WheelRing sky = WheelRing.of(WheelRing.Kind.SKY);
        WheelRing natal = WheelRing.of(WheelRing.Kind.CHART_A);
        double[] cusps = new double[13];
        for (int i = 1; i <= 12; i++) {
            cusps[i] = (i - 1) * 30.0;
        }
        String empty = NowCards.html(sky, natal, cusps, false, "<p>raw orb table</p>");
        clean("the empty Now page", text(empty));

        // Every body in the sky one degree past each natal point a quarter of the zodiac on:
        // every major aspect, and a few minor ones that a plain page must leave out.
        for (int i = 0; i < SkymapPanel.BODY_COUNT; i++) {
            natal.valid[i] = true;
            natal.lon[i] = (i * 37.0) % 360.0;
            sky.valid[i] = true;
            sky.lon[i] = (i * 37.0 + 90.5 + (i % 5) * 30.0) % 360.0;
            sky.speed[i] = 0.5;
        }
        String full = NowCards.html(sky, natal, cusps, false, "<p>raw orb table</p>");
        yes("the filled page has cards", full.contains(TransitCard.WHAT_TO_DO));
        clean("the filled Now page", text(full));
        String astro = NowCards.html(sky, natal, cusps, true, "<p>raw orb table</p>");
        yes("the drawer in Astrologer view still names the astrology",
            !banned(text(astro)).isEmpty());
    }

    // ------------------------------------------------------------------ D

    private static void thePopups() {
        for (String sky : SKY) {
            NowCards.TransitHit h = new NowCards.TransitHit(sky, 10, 1, 7, "Venus", 10, 7,
                Aspects.Type.OPPOSITION, 0.3, false, false);
            ReadingCard painted = RevealPlan.forTransit(h, TransitCard.of(h)).card();
            clean(sky + " pop-up", painted.label() + " " + painted.headline() + " " + painted.body());
        }
    }

    // ------------------------------------------------------------------ helpers

    /** A page's words as the reader sees them: tags out, entities read. */
    private static String text(String html) {
        return html.replaceAll("(?s)<[^>]+>", " ").replace("&mdash;", "-").replace("&amp;", "&")
            .replace("&middot;", "·").replaceAll("\\s+", " ");
    }

    private interface Part {
        void run() throws Exception;
    }

    private static void part(String name, Part p) throws Exception {
        System.out.println("=== " + name + " ===");
        int before = failures.size();
        int checksBefore = checks;
        p.run();
        System.out.println("  " + (checks - checksBefore) + " checks, "
            + (failures.size() - before) + " failures");
    }

    private static void yes(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(what);
            System.out.println("  FAIL " + what);
        }
    }
}
