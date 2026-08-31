package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A snapshot a reader can actually read: three titled paragraphs of plain prose.
 *
 * The scoring engine is machinery for deciding WHAT TO SAY. It is not the thing to say.
 * Both previous attempts had that backwards - Snapshot.paragraph printed the astrologer's
 * worksheet ("participating triplicity ruler (no points under current setting)"), and the
 * 2026-08-19 wiring appended raw getBodyCore HTML into plain text, so a reader got literal
 * b-tags and unconverted escapes mid-sentence plus ninety words of encyclopedia per body.
 *
 * So prominence is used ONLY as a running order - which few placements are loud enough to
 * be worth a sentence - and the words come from the one plain-English line the data already
 * carries. Nothing was generating that line: everything was either quoting the whole entry
 * or describing the geometry instead of the meaning.
 *
 * Placements that share an element are merged into one claim before they are listed
 * separately, which is the difference between a reading and a list. That merge is a
 * template and will always be one: naming what "South Node in Leo" and "Chiron in Cancer"
 * genuinely have in common needs a reader, not a lookup. What this can honestly do is say
 * they share an element and what that costs, which is true and is not nothing.
 *
 * No jargon reaches the page. Sect, angularity, dignity and prominence all decide things
 * here and none of them are named. If a term would send a reader to a glossary it gets
 * translated at the boundary or dropped.
 */
public final class PlainSnapshot {

    private PlainSnapshot() { }

    /** Where the chart is coming from. */
    private static final List<String> PAST =
        Arrays.asList("South Node", "Saturn", "Chiron");
    /** Where it is going. */
    private static final List<String> FUTURE =
        Arrays.asList("North Node", "Part of Fortune", "Jupiter");
    /** Never given a sentence of their own: they are the frame, not an actor in it. */
    private static final Set<String> ANGLES = new LinkedHashSet<>(
        Arrays.asList("Ascendant", "Descendant", "MC", "IC", "Midheaven"));

    /** How many placements a paragraph will name at most. */
    public static int bodiesPerParagraph = 3;

    public static String generate(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked) {
        InterpretationService svc = InterpretationService.getInstance();
        StringBuilder out = new StringBuilder();
        Set<String> used = new LinkedHashSet<>();

        List<String> past = pickNamed(f, PAST, used);
        List<String> now = pickRanked(f, ranked, used);
        List<String> ahead = pickNamed(f, FUTURE, used);

        out.append(block(svc, f, "WHAT YOU ARE WORKING FROM", past, openerPast(g), null));
        out.append("\n\n");
        out.append(block(svc, f, "WHERE THE WEIGHT SITS NOW", now, "", tail(g)));
        out.append("\n\n");
        out.append(block(svc, f, "WHERE IT IS HEADING", ahead, "", null));

        return out.toString();
    }

    // ------------------------------------------------------------------ selection

    /** Placements wanted by name, in the order given, skipping any that will not resolve. */
    private static List<String> pickNamed(ChartFrame f, List<String> wanted, Set<String> used) {
        List<String> out = new ArrayList<>();
        for (String name : wanted) {
            if (out.size() >= bodiesPerParagraph || used.contains(name) || !resolves(f, name)) {
                continue;
            }
            out.add(name);
            used.add(name);
        }
        return out;
    }

    /**
     * Placements taken off the top of the ranking.
     *
     * This is the only use prominence gets, and the number never reaches the page: a reader
     * has no use for "0.569" and no way to calibrate it against anything.
     */
    private static List<String> pickRanked(ChartFrame f, List<BodyScore.Vector> ranked,
                                           Set<String> used) {
        List<String> out = new ArrayList<>();
        if (ranked == null) {
            return out;
        }
        for (BodyScore.Vector v : ranked) {
            if (out.size() >= bodiesPerParagraph) {
                break;
            }
            if (v == null || v.body == null || ANGLES.contains(v.body)
                || used.contains(v.body) || !resolves(f, v.body)) {
                continue;
            }
            out.add(v.body);
            used.add(v.body);
        }
        return out;
    }

    private static boolean resolves(ChartFrame f, String name) {
        ChartFrame.Body b = bodyOf(f, name);
        return b != null && b.ok;
    }

    private static ChartFrame.Body bodyOf(ChartFrame f, String name) {
        try {
            return f.body(name);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ rendering

    /**
     * One titled paragraph: heading, optional opener, the shared-element claim, then the
     * placements themselves.
     *
     * @param opener leading clause for the prose, or "" - kept separate from the title so
     *               the title can be built from the placements the opener knows nothing of
     * @param tail   a closing observation about the chart as a whole, or null
     */
    private static String block(InterpretationService svc, ChartFrame f, String heading,
                                List<String> names, String opener, String tail) {
        StringBuilder sb = new StringBuilder();
        sb.append(heading);

        String theme = themeOf(f, names);
        if (theme != null) {
            sb.append(": ").append(theme);
        }
        sb.append("\n\n");

        if (names.isEmpty()) {
            sb.append(opener.isEmpty() ? "" : opener)
              .append("the chart says nothing specific enough to be worth reporting.");
            return sb.toString();
        }

        List<String> prose = new ArrayList<>();
        String shared = sharedElement(f, names);
        if (shared != null) {
            prose.add(shared);
        }
        for (int i = 0; i < names.size(); i++) {
            // Only the paragraph's lead placement earns its house clause. Three sentences
            // each carrying "and in the Nth house that means ..." is the template showing
            // through the prose, which is what made the first cut read like a mail merge.
            String line = sentenceFor(svc, f, names.get(i), i == 0);
            if (line != null) {
                prose.add(line);
            }
        }

        // The opener is prepended verbatim and the body is NOT lowercased. Lowercasing it
        // was how "South Node" became "south Node" and "Part of Fortune" became "part of
        // Fortune": the body's first word is often a placement name, and no list of proper
        // nouns catches the multi-word ones. So every opener ends its own sentence.
        sb.append(opener).append(String.join(" ", prose));
        if (tail != null) {
            sb.append(" ").append(tail);
        }
        return sb.toString();
    }

    /**
     * The paragraph's title, taken from the lead placement's sign.
     *
     * Deliberately the lead placement and not a blend: a title averaged over three signs
     * describes no chart in particular, and a reader cannot check it against anything.
     */
    private static String themeOf(ChartFrame f, List<String> names) {
        if (names.isEmpty()) {
            return null;
        }
        ChartFrame.Body b = bodyOf(f, names.get(0));
        if (b == null || !b.ok) {
            return null;
        }
        return SIGN_KEYNOTE[Zodiac.signIndex(b.lon)];
    }

    /**
     * One claim covering the placements that share an element, or null if none do.
     *
     * Two of three in one element is worth saying; one each is just the zodiac being
     * ordinary, and reporting it would be noise dressed as a finding.
     */
    private static String sharedElement(ChartFrame f, List<String> names) {
        Map<String, List<String>> byElement = new LinkedHashMap<>();
        for (String name : names) {
            ChartFrame.Body b = bodyOf(f, name);
            if (b == null || !b.ok) {
                continue;
            }
            String el = Zodiac.elementName(Zodiac.signIndex(b.lon));
            byElement.computeIfAbsent(el, k -> new ArrayList<>()).add(name);
        }
        for (Map.Entry<String, List<String>> e : byElement.entrySet()) {
            if (e.getValue().size() >= 2) {
                return list(e.getValue())
                    + (e.getValue().size() == 2 ? " both sit in " : " all sit in ")
                    + e.getKey() + " signs, " + ELEMENT_TOGETHER.get(e.getKey());
            }
        }
        return null;
    }

    /**
     * Opens on the day/night split, because it is the one fact that changes who helps.
     * Stated as consequence rather than as terminology: a reader does not need the word
     * "sect" to understand that a night chart gives Venus the better hand.
     */
    private static String openerPast(Gestalt.Result g) {
        String helper = g.diurnal ? "Jupiter" : "Venus";
        StringBuilder sb = new StringBuilder();
        sb.append("You were born ").append(g.diurnal ? "in daylight" : "at night")
          .append(", which sets the terms for everything below: ").append(helper)
          .append(" is the planet best placed to help you");
        if (g.outOfSectMalefic != null && !g.outOfSectMalefic.isEmpty()) {
            sb.append(", and ").append(g.outOfSectMalefic)
              .append(" the one most likely to make things hard");
        }
        sb.append(". ");
        return sb.toString();
    }

    /** A closing observation for the middle paragraph: what the chart has to do without. */
    private static String tail(Gestalt.Result g) {
        if (g == null || g.missingElements == null || g.missingElements.isEmpty()) {
            return null;
        }
        String e = g.missingElements.get(0);
        String gloss = "fire".equals(e) ? "drive and appetite for risk"
            : "earth".equals(e) ? "patience and practical follow-through"
            : "air".equals(e) ? "detachment and talking things out"
            : "feeling and instinct";
        return "There is almost no " + e + " here, so " + gloss
            + " is something you have to supply on purpose rather than fall back on.";
    }

    // ------------------------------------------------------------------ sentences

    /**
     * One placement as one sentence.
     *
     * Returns null rather than a placeholder when the data has nothing: a paragraph with a
     * gap in it is better than one carrying "Interpretation not found" as though that were
     * a finding.
     *
     * @param withHouse whether to add the house clause - the paragraph's lead only
     */
    private static String sentenceFor(InterpretationService svc, ChartFrame f,
                                      String name, boolean withHouse) {
        ChartFrame.Body b = bodyOf(f, name);
        if (b == null || !b.ok) {
            return null;
        }
        String sign = Zodiac.signName(b.lon);
        Line signLine = plain(svc.getPlanetInSign(name, sign));
        if (signLine == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (signLine.whole) {
            // The value already names the body - "Chiron in Cancer carries a devastating
            // wound..." - so a lead-in would double it.
            sb.append(signLine.text);
        } else {
            sb.append(name).append(" in ").append(capitalise(sign))
              .append(" means ").append(signLine.text);
        }

        if (withHouse) {
            int house = Zodiac.houseOf(b.lon, f.cusps);
            Line houseLine = house > 0 ? plain(svc.getPlanetInHouse(name, house)) : null;
            if (houseLine != null) {
                sb.append(", and in the ").append(ordinal(house)).append(" house that means ")
                  .append(houseLine.whole ? lowerFirst(houseLine.text) : houseLine.text);
            }
        }
        sb.append(".");
        return sb.toString();
    }

    // ------------------------------------------------------------------ vocabulary

    /**
     * One short phrase per sign, used only as a paragraph title.
     *
     * Kept deliberately plain and slightly two-sided - what the sign wants, and what it
     * costs - because a title that only flatters is one a reader stops trusting by the
     * third paragraph. Indexed by Zodiac.signIndex, so the order is Aries first.
     */
    private static final String[] SIGN_KEYNOTE = {
        "starting things, and the appetite for a fight",
        "holding steady, and what you are unwilling to give up",
        "gathering it all in, and talking it through",
        "safety, and who you have decided to look after",
        "being seen, and what recognition turns out to cost",
        "getting it right, and the price of that",
        "keeping the peace, and what you trade away for it",
        "going deep, and what you refuse to let go of",
        "reaching further, and what you have decided to believe",
        "building something that lasts, and who is in charge of it",
        "standing apart, and what you still owe the group",
        "letting the edges go, and what that asks you to give up"
    };

    /** What it means when several placements land in one element. */
    private static final Map<String, String> ELEMENT_TOGETHER = new LinkedHashMap<>();
    static {
        ELEMENT_TOGETHER.put("fire",
            "so this part of your life runs hot and tends to move before it checks.");
        ELEMENT_TOGETHER.put("earth",
            "so this part of your life is slow, practical, and genuinely hard to shift.");
        ELEMENT_TOGETHER.put("air",
            "so this part of your life gets thought about and talked through"
            + " long before it gets felt.");
        ELEMENT_TOGETHER.put("water",
            "so this part of your life is run by feeling and memory"
            + " rather than by argument.");
    }

    // ------------------------------------------------------------------ text helpers

    /**
     * The one plain line out of an interpretation value.
     *
     * Takes what is inside the bold and discards the body, strips the markup, and converts
     * the escapes the hand-rolled loader leaves behind into real characters.
     *
     * Two shapes live in this data and they need opposite handling:
     *
     *   "Ceres in Aries: Nurturing is expressed through..."   label, then a fragment
     *   "Chiron in Cancer carries a devastating wound..."     already a whole sentence
     *
     * Lowercasing the first character suits the first and wrecks the second - that is what
     * produced "means saturn descends to the Imum Coeli" and the doubled "Chiron in Cancer
     * means chiron in Cancer carries...". The colon decides which shape it is, and the
     * caller is told rather than left to guess.
     */
    static Line plain(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty() || s.startsWith("Interpretation not found") || s.startsWith("General ")) {
            return null;
        }
        int br = s.indexOf("<br>");
        if (br >= 0) {
            s = s.substring(0, br);
        }
        s = s.replace("<b>", "").replace("</b>", "")
             .replace("<i>", "").replace("</i>", "");
        s = Prose.unescape(s).trim();

        int colon = s.indexOf(':');
        if (colon > 0 && colon < 40) {
            s = trimStops(s.substring(colon + 1).trim());
            return s.isEmpty() ? null : new Line(lowerFirst(s), false);
        }
        s = trimStops(s);
        return s.isEmpty() ? null : new Line(s, true);
    }

    /**
     * A plain line, plus whether it already stands as a sentence.
     *
     * A type rather than a naming convention, because the two shapes need opposite
     * treatment and getting it wrong fails silently - it reads as a typo, not as a crash.
     */
    static final class Line {
        final String text;
        /** True when the value already names the body and needs no lead-in. */
        final boolean whole;

        Line(String text, boolean whole) {
            this.text = text;
            this.whole = whole;
        }
    }

    private static String trimStops(String s) {
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    /**
     * Lowercases the first letter unless the word is one the language capitalises anyway.
     * "The Moon in Libra" must survive being moved mid-clause; an ordinary sentence should
     * not keep its capital there.
     */
    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int end = s.indexOf(' ');
        String first = end < 0 ? s : s.substring(0, end);
        for (String proper : PROPER) {
            if (first.equals(proper)) {
                return s;
            }
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Words that keep their capital at the start of a clause. The bodies are here because
     * an interpretation line often opens with the body's own name, and lowercasing it is
     * the difference between "the Moon in Libra seeks" and "the moon in Libra seeks".
     */
    private static final String[] PROPER = {
        "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus",
        "Neptune", "Pluto", "Chiron", "Ceres", "Pallas", "Juno", "Vesta", "Eris",
        "Eros", "Hygiea", "Nessus", "Pholus", "Lilith"
    };

    private static String list(List<String> names) {
        List<String> n = new ArrayList<>();
        for (String name : names) {
            // "Ceres, Mars and Moon" is the one place the bare names read as a database
            // column rather than as English.
            n.add("Moon".equals(name) || "Sun".equals(name) ? "the " + name : name);
        }
        if (n.size() == 1) {
            return n.get(0);
        }
        return String.join(", ", n.subList(0, n.size() - 1)) + " and " + n.get(n.size() - 1);
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String ordinal(int n) {
        switch (n) {
            case 1:  return "1st";
            case 2:  return "2nd";
            case 3:  return "3rd";
            case 21: return "21st";
            case 22: return "22nd";
            case 23: return "23rd";
            default: return n + "th";
        }
    }
}
