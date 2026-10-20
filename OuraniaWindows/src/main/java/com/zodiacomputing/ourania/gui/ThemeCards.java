package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Themes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Synthesize page: the chart's own themes, each one said by at least three independent
 * parts of the chart (2026-10-20).
 *
 * <b>Why this page exists apart from Your year.</b> The tab spec David adopted on 2026-10-20 puts
 * Synthesize under Interpretation - "only major themes supported by at least three independent
 * chart configurations" - and Your year under Prediction. The app had one page doing the year's
 * job under the Synthesize key. The natal half already existed: {@link Themes} (L7) counts
 * witnesses with an independence rule and has a threshold of three, but its signatures reached
 * a reader only as one sentence in At a glance and a block of the text report.
 *
 * <b>The theme words are the engine's, the sentences are this class's.</b> L7 names a theme with
 * an adjective ("fiery", "saturnian", "vocational"), which is a label for counting and not a
 * thing to say. Each word gets a plain headline and body here; the witnesses themselves, which
 * are the engine's own technical lines, go in the "why" box that Astrologer view opens.
 */
final class ThemeCards {

    private ThemeCards() { }

    /** A theme word, said plainly: what to call it, and what it is like to live with. */
    record Plain(String headline, String body) { }

    static final Map<String, Plain> PLAIN = Map.ofEntries(
        // What a part of life is about (L6's topic words).
        Map.entry("self-defining", new Plain("Becoming yourself is the work",
            "Who you are, and how you come across, is a thing you build on purpose.")),
        Map.entry("resourcing", new Plain("What you have and what you are worth",
            "Money, things and your own sense of value take a lot of your attention.")),
        Map.entry("communicative", new Plain("Words are your tool",
            "Talking, writing, learning and the people near you shape your days.")),
        Map.entry("rooted", new Plain("Home and family hold you",
            "Where you come from and where you live matter more than most people see.")),
        Map.entry("creative", new Plain("You need to make things",
            "Play, art, romance and children are where you come alive.")),
        Map.entry("labouring", new Plain("Daily work and health",
            "Routine, service and the care of your body are a large part of your life.")),
        Map.entry("relational", new Plain("Partners shape you",
            "Close one-to-one bonds, in love and in work, are where you learn most.")),
        Map.entry("shared", new Plain("What you share with others",
            "Trust, intimacy and shared money are strong subjects for you.")),
        Map.entry("believing", new Plain("You look for meaning",
            "Travel, study and what you believe pull you beyond the familiar.")),
        Map.entry("vocational", new Plain("Your work in the world",
            "Career and reputation are a central thread of your life.")),
        Map.entry("communal", new Plain("Friends and groups",
            "Your circle, your causes and your hopes for the future carry weight.")),
        Map.entry("withdrawn", new Plain("A strong inner life",
            "You need time alone, and much of your life happens out of sight.")),
        // Whether a part of life holds together.
        Map.entry("consolidated", new Plain("What you build tends to last",
            "Where you put effort, the parts of your life support each other.")),
        Map.entry("unconsolidated", new Plain("Effort that does not always add up",
            "You start things with energy, and finishing them takes a plan.")),
        // How well the chart's parts work.
        Map.entry("resourced", new Plain("You have strong tools",
            "Several parts of your chart work at their best. Use them on purpose.")),
        Map.entry("strained", new Plain("Some things come the hard way",
            "Several parts of your chart work under pressure. Patience pays off here.")),
        Map.entry("expansive", new Plain("Room to grow",
            "Your chart leans toward openness, luck and more of everything.")),
        Map.entry("constrained", new Plain("Limits that shape you",
            "Your chart leans toward caution and limits, which also build strength.")),
        // Where the chart faces.
        Map.entry("public", new Plain("A life lived in view",
            "Much of your chart faces the world, your work and your reputation.")),
        Map.entry("private", new Plain("A life lived close to home",
            "Much of your chart faces inward, toward family and private life.")),
        Map.entry("self-directed", new Plain("You steer your own course",
            "You act first and on your own terms more than most people.")),
        Map.entry("other-directed", new Plain("You find yourself through others",
            "Other people, and how you meet them, steer a lot of your life.")),
        // Elements, present and missing.
        Map.entry("fiery", new Plain("You run on enthusiasm",
            "Energy, courage and a need to act come up again and again.")),
        Map.entry("earthy", new Plain("You are practical",
            "Results you can touch, steady work and the body come up again and again.")),
        Map.entry("airy", new Plain("You live in ideas",
            "Thinking, talking and people come up again and again.")),
        Map.entry("watery", new Plain("You feel things deeply",
            "Feelings, intuition and closeness come up again and again.")),
        Map.entry("absence-of-fire", new Plain("Drive does not come easily",
            "Get started with small steps. Do not wait to feel ready.")),
        Map.entry("absence-of-earth", new Plain("Practical matters need effort",
            "Routines and plans do not come by themselves, so build them on purpose.")),
        Map.entry("absence-of-air", new Plain("Stepping back is hard",
            "Talking a thing through with someone helps you see it clearly.")),
        Map.entry("absence-of-water", new Plain("Feelings can be hard to read",
            "Give your feelings, and other people's, time before you act.")),
        // Modes.
        Map.entry("initiating", new Plain("You are a starter",
            "You like to begin things and to set them moving.")),
        Map.entry("consolidating", new Plain("You are a keeper",
            "You hold on, see things through and resist change.")),
        Map.entry("adapting", new Plain("You go with the flow",
            "You adjust easily, and you need a clear goal to keep your focus.")),
        // The tone of the planet that leads the chart.
        Map.entry("solar", new Plain("You need to shine",
            "Purpose, pride and being seen are strong in your life.")),
        Map.entry("lunar", new Plain("Feelings lead",
            "Moods, needs and care for others are strong in your life.")),
        Map.entry("mercurial", new Plain("A busy mind",
            "Questions, news and making connections are strong in your life.")),
        Map.entry("venusian", new Plain("Love and beauty matter",
            "Harmony, pleasure and good relations are strong in your life.")),
        Map.entry("martial", new Plain("You are a fighter",
            "Drive, competition and courage are strong in your life.")),
        Map.entry("jovial", new Plain("A generous outlook",
            "Hope, growth and a wide view of life are strong in your life.")),
        Map.entry("saturnian", new Plain("Discipline is your strength",
            "Duty, patience and slow, real achievement are strong in your life.")),
        Map.entry("disruptive", new Plain("You need freedom",
            "Change, surprise and doing it your own way are strong in your life.")),
        Map.entry("dissolving", new Plain("A dreamer",
            "Imagination, ideals and blurred edges are strong in your life.")),
        Map.entry("transformative", new Plain("Deep change runs through your life",
            "Power, crisis and starting again are strong in your life.")));

    /** The plain words for a theme; an engine word with none gets a careful default. */
    static Plain plain(String theme) {
        Plain p = PLAIN.get(theme);
        return p != null ? p : new Plain("A repeated note in your chart",
            "Several separate parts of your chart say the same thing here.");
    }

    /** The cards: each theme at the threshold, strongest first, then each tension. */
    static List<ReadingCard> cards(Themes.Result t) {
        List<ReadingCard> out = new ArrayList<>();
        if (t == null) {
            return out;
        }
        for (Themes.Signature s : Themes.signaturesAtThreshold(t)) {
            Plain p = plain(s.theme);
            List<String> why = new ArrayList<>();
            why.add(s.witnessCount + " independent witnesses for \"" + s.theme + "\"");
            for (List<Themes.Candidate> comp : s.components) {
                why.add(comp.get(0).text);
            }
            out.add(new ReadingCard(s.witnessCount + " PARTS OF YOUR CHART AGREE",
                p.headline(), p.body(), why));
        }
        for (Themes.Contradiction c : t.contradictions) {
            Plain a = plain(c.themeA);
            Plain b = plain(c.themeB);
            out.add(new ReadingCard("A PULL IN TWO DIRECTIONS",
                a.headline() + ", and also: " + lower(b.headline()),
                "Both are strong in your chart. This tension is part of who you are,"
                    + " not a problem to solve. Make room for each side.",
                List.of(c.themeA + " (" + c.witnessesA + ") against " + c.themeB
                    + " (" + c.witnessesB + ")", c.strongestA.text, c.strongestB.text)));
        }
        return out;
    }

    /** The whole page. */
    static String html(Themes.Result t, boolean showWhy) {
        List<ReadingCard> cards = cards(t);
        StringBuilder sb = ReadingCard.page("Synthesize",
            "Only what at least " + Themes.signatureThreshold + " separate parts of your chart"
                + " agree on. One placement alone is a detail; this is the pattern.");
        if (cards.isEmpty()) {
            sb.append(ReadingCard.html(List.of(new ReadingCard("NO STRONG PATTERN",
                "No theme repeats " + Themes.signatureThreshold + " times",
                "Your chart spreads its weight. At a glance and Reading describe the parts.",
                List.of())), false));
        } else {
            sb.append(ReadingCard.html(cards, showWhy));
        }
        return sb.append("</body></html>").toString();
    }

    private static String lower(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
