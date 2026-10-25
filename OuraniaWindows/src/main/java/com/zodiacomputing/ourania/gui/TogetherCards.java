package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Synastry;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

/**
 * Plain-English cards for two people's charts (Plain-Tabs Phase 4, the Together door).
 *
 * <p><b>The reader meets the meaning before the machinery.</b> David, 2026-10-13: the tabs
 * "only show the user the back end math side". The Synastry page listed angle contacts and
 * house overlays by Roman numeral; this class says the same facts as two pages:
 * <ul>
 *   <li><b>Where you meet</b>: the strongest pulls (a body on the other person's angle), then
 *       each person's planets in the other's life, grouped into three rooms - Foundation
 *       (houses 1, 4, 7, 10), Feel-good (3, 5, 9, 11) and Growth (2, 6, 8, 12).</li>
 *   <li><b>Day-to-day chemistry</b>: the tightest aspects between the two charts, each as the
 *       synastry reading already written for that pair.</li>
 * </ul>
 *
 * <p><b>Every fact comes from {@link Synastry}, and every filter from the one the grid uses.</b>
 * This class groups, names and ranks; it does not decide what is a contact. The bodies it
 * lists are the ones the wheel shows ({@link WheelRing#shows}) and the aspects the ones the
 * reader has switched on ({@link AspectGate#shown}) - the same two tests the grid applies -
 * so the page and the grid agree about what two people share. The numbers - houses, signs,
 * orbs - go in each card's why lines, shown only in Astrologer view
 * ({@link Settings#astrologerView()}).
 */
final class TogetherCards {

    /** How many chemistry cards the page shows. The rest are on the aspect grid. */
    static final int CHEMISTRY_LIMIT = 8;

    /** How many angle contacts lead the Where you meet page. */
    static final int PULL_LIMIT = 4;

    private TogetherCards() {
    }

    /**
     * The two charts being read, and what the reader has switched on.
     *
     * @param you          Chart A, the base wheel
     * @param them         Chart B, the partner
     * @param shown        whether a registry index is on the wheel; null shows every body
     * @param aspectShown  the reader's aspect switches, by {@link Aspects.Type} ordinal, as
     *                     {@link AspectGate#shown} takes them; null shows every aspect
     */
    record Pair(ChartFrame you, ChartFrame them, IntPredicate shown, boolean[] aspectShown) {
        Pair {
            shown = shown == null ? i -> true : shown;
            aspectShown = aspectShown == null ? null : aspectShown.clone();
        }
    }

    /** A group of four houses that answer one question about a relationship. */
    enum Room {
        FOUNDATION("Foundation", "What the relationship stands on",
            "Who you each are, home, partnership and your place in the world. Contact here is "
                + "felt as weight and commitment.", 1, 4, 7, 10),
        FEEL_GOOD("Feel-good", "Where it is easy to enjoy each other",
            "Talk, fun, shared ideas and friends. Contact here makes time together feel light.",
            3, 5, 9, 11),
        GROWTH("Growth", "Where you change each other",
            "Money and values, daily habits, intimacy and the private side of life. Contact "
                + "here asks more of both people, and changes them.", 2, 6, 8, 12);

        /** The room's display name, "Feel-good". Not {@code name()}, which is "FEEL_GOOD". */
        final String label;
        final String question;
        final String meaning;
        private final int[] houses;

        Room(String label, String question, String meaning, int... houses) {
            this.label = label;
            this.question = question;
            this.meaning = meaning;
            this.houses = houses;
        }

        /** The room a house belongs to, or null outside 1 to 12. */
        static Room of(int house) {
            for (Room r : values()) {
                for (int h : r.houses) {
                    if (h == house) {
                        return r;
                    }
                }
            }
            return null;
        }

        /** The room's houses, in the order the page names them. */
        int[] houses() {
            return houses.clone();
        }
    }

    /** Which way a contact runs: whose planets, in whose life. */
    enum Direction {
        THEIRS_IN_YOURS("What they bring to your life", "Their", "your",
            Pair::them, Pair::you, LifeAreas::of),
        YOURS_IN_THEIRS("What you bring to their life", "Your", "their",
            Pair::you, Pair::them, LifeAreas::ofTheirs);

        final String headline;
        /** The visiting body's owner, capitalised: "Their Sun". */
        final String visitorWord;
        /** The host chart's owner: "your 7th house". */
        final String hostWord;
        private final Function<Pair, ChartFrame> visitor;
        private final Function<Pair, ChartFrame> host;
        private final IntFunction<String> area;

        Direction(String headline, String visitorWord, String hostWord,
                  Function<Pair, ChartFrame> visitor, Function<Pair, ChartFrame> host,
                  IntFunction<String> area) {
            this.headline = headline;
            this.visitorWord = visitorWord;
            this.hostWord = hostWord;
            this.visitor = visitor;
            this.host = host;
            this.area = area;
        }

        ChartFrame visitor(Pair p) {
            return visitor.apply(p);
        }

        ChartFrame host(Pair p) {
            return host.apply(p);
        }

        /** The host's part of life for a house, in the host's voice. */
        String area(int house) {
            return area.apply(house);
        }
    }

    // ------------------------------------------------------------------ Where you meet

    /**
     * One card per room and direction that has anything in it, rooms in order.
     *
     * <b>Grouped by house inside the card</b>, for the reason the old overlay list grouped by
     * house: three of their bodies in your 7th is one fact about your partnerships, not three.
     */
    static List<ReadingCard> roomCards(Pair p, Room room) {
        List<ReadingCard> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            List<Synastry.Overlay> overlays = Synastry.houseOverlays(d.visitor(p), d.host(p));
            List<String> lines = new ArrayList<>();
            List<String> why = new ArrayList<>();
            for (int house : room.houses()) {
                List<String> names = new ArrayList<>();
                for (Synastry.Overlay o : overlays) {
                    if (o.house != house || !listed(p, o.bodyIndex)) {
                        continue;
                    }
                    names.add(o.body);
                    why.add(d.visitorWord + " " + o.body + " in " + d.hostWord + " "
                        + Zodiac.ordinal(house) + " house");
                }
                if (!names.isEmpty()) {
                    lines.add(PlacementText.capitalise(d.area(house)) + ": "
                        + d.visitorWord.toLowerCase(Locale.ROOT) + " " + joinAnd(names) + ".");
                }
            }
            if (!lines.isEmpty()) {
                out.add(new ReadingCard(room.label.toUpperCase(Locale.ROOT), d.headline,
                    String.join(" ", lines), why));
            }
        }
        return out;
    }

    /**
     * A body of one chart sitting on an angle of the other, both directions, tightest first.
     *
     * <b>These lead the page because they outrank any house.</b> A body on the other person's
     * Ascendant is noticed at the first meeting; the same body in their 3rd house is a
     * background fact. The old page put them first for the same reason.
     */
    static List<ReadingCard> pullCards(Pair p) {
        List<Pull> pulls = new ArrayList<>();
        for (Direction d : Direction.values()) {
            for (Synastry.AngleContact c : Synastry.angleContacts(d.visitor(p), d.host(p))) {
                if (listed(p, c.bodyIndex)) {
                    pulls.add(new Pull(d, c));
                }
            }
        }
        pulls.sort((x, y) -> Double.compare(x.contact.orb, y.contact.orb));
        List<ReadingCard> out = new ArrayList<>();
        for (Pull pull : pulls.subList(0, Math.min(PULL_LIMIT, pulls.size()))) {
            Direction d = pull.direction;
            Synastry.AngleContact c = pull.contact;
            out.add(new ReadingCard("THE STRONGEST PULL",
                d.visitorWord + " " + c.body + " is felt at once",
                "It sits right on a turning point of " + d.hostWord + " chart, the one about "
                    + d.area(Synastry.angleHouse(c.angle)) + ". A contact like this is usually "
                    + "noticed early, and it stays noticeable.",
                List.of(d.visitorWord + " " + c.body + " in " + sign(c.bodyLon) + " on "
                    + d.hostWord + " " + c.angle + " in " + sign(c.angleLon),
                    orb(c.orb, c.maxOrb))));
        }
        return out;
    }

    /** An angle contact and the direction it runs in, for sorting both directions together. */
    private record Pull(Direction direction, Synastry.AngleContact contact) {
    }


    // ------------------------------------------------------------------ Chemistry

    /**
     * The tightest aspects between the two charts that have a reading, at most {@code limit}.
     *
     * <b>Their body first, yours second</b>, which is the order
     * {@link InterpretationService#getSynastryInteraspect} files them in ("their Sun in
     * quincunx to your Moon").
     *
     * <b>The five Ptolemaic aspects only.</b> Measured 2026-10-16: at the halved synastry orb
     * the tightest contacts are often minor - a sesquiquadrate - and the minor readings are
     * one template whose first sentence reads "their Sun in Sesquiquadrate (135&deg;) to your
     * Chiron (the sacred vulnerability ...)". That is the machinery the plain page exists to
     * keep out of a headline. The minors stay on the grid, which shows every aspect.
     *
     * An aspect type the reader has switched off is skipped, as the
     * grid skips it. An aspect with no reading is skipped rather than given words
     * written here: the card shows only text the readings already contain.
     */
    static List<ReadingCard> chemistryCards(Pair p, int limit) {
        InterpretationService svc = InterpretationService.getInstance();
        List<ReadingCard> out = new ArrayList<>();
        for (Synastry.InterAspect a : contacts(p)) {
            if (out.size() >= limit) {
                break;
            }
            String prose = svc.getSynastryInteraspect(a.visitorBody, a.hostBody, a.type.label);
            if (prose == null) {
                continue;
            }
            // The closing "Dynamic: ..." tag is a catalogue label, not a sentence for a reader.
            prose = prose.replaceAll("(?i)<i>\\s*Dynamic:[^<]*</i>", "");
            // The first sentence after the lead, not the lead: see fromFirstSentence for the
            // measurement - five template leads head nearly every major reading.
            ReadingCard card = ReadingCard.fromFirstSentence(
                tone(a.type) + " · THEIR " + a.visitorBody.toUpperCase(Locale.ROOT)
                    + ", YOUR " + a.hostBody.toUpperCase(Locale.ROOT),
                prose,
                List.of("Their " + a.visitorBody + " in " + sign(a.visitorBodyLon) + " "
                    + a.type.label.toLowerCase(Locale.ROOT) + " your " + a.hostBody + " in "
                    + sign(a.hostBodyLon), orb(a.orb, a.maxOrb)));
            if (card != null) {
                out.add(card);
            }
        }
        return out;
    }

    /**
     * What an aspect feels like, in a word a reader knows, by its division of the circle.
     *
     * By harmonic rather than by name so the six minor aspects need no entry each: the
     * semisquare and sesquiquadrate are the square's family, the quincunx the semisextile's.
     */
    static String tone(Aspects.Type t) {
        switch (t.harmonic) {
            case 1:  return "BLENDED";
            case 3:
            case 6:  return "EASY FLOW";
            case 2:
            case 4:
            case 8:  return "SPARK AND FRICTION";
            case 12: return "TAKES ADJUSTING";
            default: return "A SUBTLE THREAD";
        }
    }

    // ------------------------------------------------------------------ At a glance

    /**
     * The contacts both pair pages draw from: Ptolemaic, between bodies the wheel shows, and
     * switched on in the aspect settings. Tightest first.
     *
     * <b>One filter for the Chemistry page and the glance (2026-10-24).</b> The glance counts
     * what the Chemistry page lists; a second copy of this test would let the count say eight
     * links while the page below it listed five.
     */
    static List<Synastry.InterAspect> contacts(Pair p) {
        List<Synastry.InterAspect> out = new ArrayList<>();
        for (Synastry.InterAspect a : Synastry.interAspects(p.them(), p.you())) {
            if (a.type.isPtolemaic() && listed(p, a.visitorBodyIndex)
                    && listed(p, a.hostBodyIndex) && AspectGate.shown(a.type, p.aspectShown())) {
                out.add(a);
            }
        }
        return out;
    }

    /**
     * How the contacts divide, by the tone words {@link #tone} already gives them:
     * {easy flow, spark and friction, blended, the rest}.
     */
    static int[] balance(List<Synastry.InterAspect> contacts) {
        int[] n = new int[4];
        for (Synastry.InterAspect a : contacts) {
            String t = tone(a.type);
            if (t.equals("EASY FLOW")) {
                n[0]++;
            } else if (t.equals("SPARK AND FRICTION")) {
                n[1]++;
            } else if (t.equals("BLENDED")) {
                n[2]++;
            } else {
                n[3]++;
            }
        }
        return n;
    }

    /** The one sentence over the bar. Counts only: it claims nothing the counts do not. */
    static String balanceSentence(int[] n) {
        int total = n[0] + n[1] + n[2] + n[3];
        if (total == 0) {
            return "No close links between the two charts.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(n[0]).append(n[0] == 1 ? " link flows easily, " : " links flow easily, ")
          .append(n[1]).append(n[1] == 1 ? " brings friction that takes work, "
              : " bring friction that takes work, ")
          .append(n[2]).append(n[2] == 1 ? " blends the two charts together"
              : " blend the two charts together");
        if (n[3] > 0) {
            sb.append(", and ").append(n[3]).append(n[3] == 1 ? " is a quieter thread"
                : " are quieter threads");
        }
        return sb.append(".").toString();
    }

    /** The At a glance page for a pair, a whole document (reading framework spec). */
    static String glanceHtml(Pair p, boolean showWhy) {
        StringBuilder sb = ReadingCard.page("Your chemistry at a glance",
            "How the two charts meet, before any detail: the core links, and how much comes "
                + "easily against how much takes work.");
        List<Synastry.InterAspect> all = contacts(p);
        int[] n = balance(all);
        sb.append("<h3 style='color:#FFD166; margin-top:14px; margin-bottom:6px;'>"
            + "Easy and hard</h3>");
        sb.append("<div style='font-size:13px; margin-bottom:6px;'>")
          .append(balanceSentence(n)).append("</div>");
        int ends = n[0] + n[1];
        if (ends > 0) {
            int easyPct = Math.max(1, Math.min(99, Math.round(100f * n[0] / ends)));
            sb.append("<table width='100%' cellspacing='0' cellpadding='0'><tr>");
            if (n[0] > 0) {
                sb.append("<td width='").append(n[1] == 0 ? 100 : easyPct)
                  .append("%' height='10' bgcolor='#3FA66A'></td>");
            }
            if (n[1] > 0) {
                sb.append("<td width='").append(n[0] == 0 ? 100 : 100 - easyPct)
                  .append("%' height='10' bgcolor='#D9534F'></td>");
            }
            sb.append("</tr></table>");
            sb.append("<div style='color:#9AA5B1; font-size:11px; margin-top:3px;'>"
                + "Green: what flows easily. Red: what takes work.</div>");
        }

        sb.append("<h3 style='color:#7FB3FF; margin-top:18px; margin-bottom:6px;'>"
            + "The core links</h3>");
        InterpretationService svc = InterpretationService.getInstance();
        List<String> unlinked = new ArrayList<>();
        for (String body : new String[] {"Sun", "Moon"}) {
            Synastry.InterAspect found = null;
            for (Synastry.InterAspect a : all) {
                if (a.visitorBody.equals(body) && a.hostBody.equals(body)) {
                    found = a;
                    break;
                }
            }
            ReadingCard card = null;
            if (found != null) {
                String prose = svc.getSynastryInteraspect(body, body, found.type.label);
                if (prose != null) {
                    prose = prose.replaceAll("(?i)<i>\\s*Dynamic:[^<]*</i>", "");
                    card = ReadingCard.fromFirstSentence(
                        tone(found.type) + " · " + body.toUpperCase(Locale.ROOT) + " AND "
                            + body.toUpperCase(Locale.ROOT),
                        prose,
                        List.of("Their " + body + " in " + sign(found.visitorBodyLon) + " "
                            + found.type.label.toLowerCase(Locale.ROOT) + " your " + body
                            + " in " + sign(found.hostBodyLon), orb(found.orb, found.maxOrb)));
                }
            }
            if (card == null) {
                unlinked.add(body);
            } else {
                sb.append(ReadingCard.html(List.of(card), showWhy));
            }
        }
        List<ReadingCard> pulls = pullCards(p);
        if (!pulls.isEmpty()) {
            sb.append(ReadingCard.html(List.of(pulls.get(0)), showWhy));
        }
        if (!unlinked.isEmpty()) {
            sb.append("<div style='color:#9AA5B1; font-size:12px; margin-top:6px;'>"
                + "Not closely linked: ").append(joinAnd(unlinked)).append(".</div>");
        }
        return sb.append("</body></html>").toString();
    }

    // ------------------------------------------------------------------ Pages

    /** The Where you meet page, a whole document. */
    static String meetHtml(Pair p, boolean showWhy) {
        StringBuilder sb = ReadingCard.page("Where you meet",
            "Where each person's planets land in the other's life. The closer a planet sits to a "
                + "turning point of the other chart, the more it is felt.");
        List<ReadingCard> pulls = pullCards(p);
        if (!pulls.isEmpty()) {
            sb.append("<h3 style='color:#FFD166; margin-top:14px; margin-bottom:6px;'>"
                + "The strongest pulls</h3>");
            sb.append(ReadingCard.html(pulls, showWhy));
        }
        for (Room room : Room.values()) {
            sb.append("<h3 style='color:#7FB3FF; margin-top:18px; margin-bottom:2px;'>")
              .append(room.label).append(" &middot; ").append(room.question).append("</h3>");
            sb.append("<div style='color:#9AA5B1; font-size:11px; margin-bottom:6px;'>")
              .append(room.meaning).append("</div>");
            List<ReadingCard> cards = roomCards(p, room);
            if (cards.isEmpty()) {
                // Said rather than left blank: a room that vanishes cannot be told from one
                // that broke.
                sb.append("<div style='color:#9AA5B1; font-size:12px; margin-bottom:10px;'>"
                    + "Neither chart has a planet here.</div>");
            } else {
                sb.append(ReadingCard.html(cards, showWhy));
            }
        }
        if (showWhy) {
            sb.append("<div style='margin-top:24px; border-top:1px solid #333; padding-top:12px;'>")
              .append("<h3 style='color:#add8e6; margin-bottom:8px;'>Cross-chart placements "
                  + "&middot; the astrology</h3>")
              .append(SynastryPage.crossContacts(p.you(), p.them()))
              .append("</div>");
        }
        return sb.append("</body></html>").toString();
    }

    /** The Day-to-day chemistry page, a whole document. */
    static String chemistryHtml(Pair p, boolean showWhy) {
        StringBuilder sb = ReadingCard.page("Day-to-day chemistry",
            "The closest links between your two charts, tightest first: what comes easily "
                + "between you, and what takes work.");
        List<ReadingCard> cards = chemistryCards(p, CHEMISTRY_LIMIT);
        if (cards.isEmpty()) {
            sb.append(ReadingCard.html(List.of(new ReadingCard("QUIET CHEMISTRY",
                "No close links between the two charts",
                "None of the planets shown make a tight aspect across the two charts. The "
                    + "relationship is described by where you meet rather than by single links.",
                List.of())), showWhy));
        } else {
            sb.append(ReadingCard.html(cards, showWhy));
        }
        return sb.append("</body></html>").toString();
    }


    // ------------------------------------------------------------------ Helpers

    /** Shown on the wheel and not an angle - the angles are the pulls, not planets in rooms. */
    private static boolean listed(Pair p, int index) {
        return index >= 0 && index < Bodies.count() && !Bodies.at(index).isAngle()
            && p.shown().test(index);
    }

    /** "Sun", "Sun and Moon", "Sun, Moon and Venus". */
    static String joinAnd(List<String> names) {
        int n = names.size();
        if (n <= 1) {
            return n == 0 ? "" : names.get(0);
        }
        return String.join(", ", names.subList(0, n - 1)) + " and " + names.get(n - 1);
    }

    private static String sign(double lon) {
        return PlacementText.capitalise(Zodiac.signName(lon));
    }

    /** "orb 0.42° (allowed 4.00°)": how close, and against what. */
    private static String orb(double orb, double allowed) {
        return String.format(Locale.ROOT, "orb %.2f° (allowed %.2f°)", orb, allowed);
    }
}
