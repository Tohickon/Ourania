package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SwissEph;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Coming up for the sky alone: what the sky itself does next (2026-10-20).
 *
 * <b>David, 2026-10-20: "basic coming up should apply to the sky chart because it is a current
 * transit chart despite it not transiting another chart".</b> With no birth chart on the wheel
 * the page was greyed, because every card on it was a contact to a chart. The sky has its own
 * events - a planet changing sign, turning back or forward, the new and full moons, eclipses,
 * and the slow planets meeting each other - and {@link Almanac} already finds them for the
 * Calendar tab. This says them plainly and puts them in the same time windows as the chart's
 * timeline, so the page reads the same with a chart or without one.
 *
 * <b>The words are this class's, the events are the Almanac's.</b> A card is built from a few
 * parts - what the planet is about, the tone of the sign, what the event does - so a year's
 * forty-odd events read as forty different sentences without forty hand-written entries.
 */
final class SkyTimeline {

    private SkyTimeline() { }

    /** How far ahead the page looks: the windows run to "later this year". */
    static final double AHEAD_DAYS = 365.0;

    /** How many cards a window shows before "And N more", as on the chart's timeline. */
    static final int PER_WINDOW = NowTimeline.PER_WINDOW;

    /** One event on the page: when, in which window, and its card. */
    record Entry(Almanac.Event event, NowTimeline.Window window, ReadingCard card) { }

    /** What each planet is about in daily life, as the subject of a sentence. */
    static final Map<String, String> AREA = Map.ofEntries(
        Map.entry("Sun", "where people put their attention"),
        Map.entry("Moon", "the mood around you"),
        Map.entry("Mercury", "talk, plans, travel and paperwork"),
        Map.entry("Venus", "love, money and what people enjoy"),
        Map.entry("Mars", "drive, effort and tempers"),
        Map.entry("Jupiter", "growth, luck and big plans"),
        Map.entry("Saturn", "duty, rules and hard work"),
        Map.entry("Uranus", "sudden change and new ideas"),
        Map.entry("Neptune", "dreams, ideals and confusion"),
        Map.entry("Pluto", "power and deep change"),
        Map.entry("North Node", "where things are heading"),
        Map.entry("Chiron", "old hurts and how people heal them"));

    /** The tone of each sign, as what follows "takes on". */
    static final String[] TONE = {
        "a bold, quick, start-it-now tone",
        "a slow, steady, practical tone",
        "a curious, talkative, restless tone",
        "a caring, protective, home-loving tone",
        "a warm, proud, playful tone",
        "a careful, tidy, useful tone",
        "a fair, sociable, people-pleasing tone",
        "an intense, private, all-or-nothing tone",
        "a hopeful, wide-ranging, honest tone",
        "a serious, ambitious, long-term tone",
        "an independent, friendly, forward-looking tone",
        "a gentle, dreamy, forgiving tone"
    };

    /** What each sign is about, for the new and full moons. */
    static final String[] ABOUT = {
        "starting things and standing up for yourself",
        "money, comfort and the body",
        "conversations, learning and the people nearby",
        "home, family and feeling safe",
        "fun, love and showing who you are",
        "work, health and daily routines",
        "partners, fairness and getting along",
        "trust, shared money and deep feelings",
        "travel, study and what you believe",
        "career, goals and responsibility",
        "friends, groups and the future",
        "rest, endings and letting go"
    };

    /** The events from {@code now} for a year, in the windows the chart's timeline uses. */
    static List<Entry> build(SwissEph sw, double now, ZoneId zone) {
        List<Entry> out = new ArrayList<>();
        if (sw == null || Double.isNaN(now)) {
            return out;
        }
        for (Almanac.Event e : Almanac.annualCalendar(sw, now, now + AHEAD_DAYS, false, true)) {
            NowTimeline.Window w = NowTimeline.windowOf(e.jd, now, zone);
            ReadingCard card = card(e, w, zone);
            if (w != null && card != null) {
                out.add(new Entry(e, w, card));
            }
        }
        return out;
    }

    /** The plain card for one event, or null for a kind this page does not say. */
    static ReadingCard card(Almanac.Event e, NowTimeline.Window w, ZoneId zone) {
        String when = w == null ? "" : NowTimeline.when(w, e.jd, zone)
            .replace("strongest ", "");
        String label = ("The sky · " + when).toUpperCase(Locale.ROOT);
        String why = e.toString();
        String area = AREA.getOrDefault(e.body, e.body);
        int sign = Zodiac.signIndex(e.longitude);
        switch (e.kind) {
            case INGRESS:
                if (e.sign < 0) {
                    return null;
                }
                return new ReadingCard(label,
                    e.body + " moves into " + signName(e.sign),
                    "Until " + e.body + " moves on, " + area + " takes on "
                        + TONE[e.sign] + "."
                        + (e.retrograde ? " It is backing into this sign, so expect a return"
                            + " to something you thought was finished." : ""),
                    List.of(why));
            case STATION_RETROGRADE:
                return new ReadingCard(label,
                    e.body + " slows and seems to move backward",
                    "For the next weeks, " + area + " asks for review rather than new starts."
                        + ("Mercury".equals(e.body)
                            ? " Check details, back up your files and confirm plans twice."
                            : " Go back over what slipped and finish old work."),
                    List.of(why));
            case STATION_DIRECT:
                return new ReadingCard(label,
                    e.body + " moves forward again",
                    "What was stuck around " + area + " starts to move. Put what you"
                        + " reviewed in the last weeks into action.",
                    List.of(why));
            case NEW_MOON:
                return new ReadingCard(label, "New Moon in " + signName(sign),
                    "A fresh start for " + ABOUT[sign] + ". Begin something small in this"
                        + " part of life in the days that follow.",
                    List.of(why));
            case FULL_MOON:
                return new ReadingCard(label, "Full Moon in " + signName(sign),
                    "Things come to a head around " + ABOUT[sign] + ". Finish what is ready,"
                        + " let go of what is not, and notice how you feel.",
                    List.of(why));
            case SOLAR_ECLIPSE:
                return new ReadingCard(label, "A solar eclipse in " + signName(sign),
                    "A strong new beginning for " + ABOUT[sign] + ". Changes that start now"
                        + " tend to unfold over the next months, so do not rush them.",
                    List.of(why));
            case LUNAR_ECLIPSE:
                return new ReadingCard(label, "A lunar eclipse in " + signName(sign),
                    "A strong ending around " + ABOUT[sign] + ". Something that has run its"
                        + " course comes into the open. Let it close.",
                    List.of(why));
            case ASPECT:
                return aspect(e, label, why);
            default:
                return null;
        }
    }

    /** Two slow planets meeting: the wider world's weather for some weeks. */
    private static ReadingCard aspect(Almanac.Event e, String label, String why) {
        if (e.aspect == null || e.other == null) {
            return null;
        }
        String verb = switch (e.aspect) {
            case CONJUNCTION -> "join forces";
            case OPPOSITION -> "pull against each other";
            case SQUARE -> "clash";
            case TRINE -> "work together easily";
            case SEXTILE -> "open a door for each other";
            default -> null;
        };
        if (verb == null) {
            return null;
        }
        String a = AREA.getOrDefault(e.body, e.body);
        String b = AREA.getOrDefault(e.other, e.other);
        return new ReadingCard(label, e.body + " and " + e.other + " " + verb,
            "In the world around you, " + a + " and " + b + " " + verb + ". This is"
                + " background weather that lasts for weeks, and it touches everyone.",
            List.of(why));
    }

    private static String signName(int sign) {
        String s = Zodiac.SIGNS[sign];
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The whole page. */
    static String html(List<Entry> entries, boolean showWhy) {
        StringBuilder sb = ReadingCard.page(NowCards.TITLE,
            "What the sky itself does next, for everyone. No birth chart is on the wheel:"
                + " turn one on to see what reaches it.");
        if (entries.isEmpty()) {
            sb.append("<p style='color:#9AA5B1; font-size:12px;'>Nothing to show yet.</p>");
        }
        for (NowTimeline.Window w : NowTimeline.Window.values()) {
            List<ReadingCard> cards = new ArrayList<>();
            int all = 0;
            for (Entry e : entries) {
                if (e.window() == w) {
                    all++;
                    if (cards.size() < PER_WINDOW) {
                        cards.add(e.card());
                    }
                }
            }
            if (all == 0) {
                continue;
            }
            sb.append("<h3 style='color:#7FB3FF; margin-top:14px; margin-bottom:6px;'>")
              .append(w.title).append("</h3>");
            sb.append(ReadingCard.html(cards, showWhy));
            if (all > cards.size()) {
                sb.append("<p style='color:#9AA5B1; font-size:12px; margin-top:-6px;'>And ")
                  .append(all - cards.size()).append(" more ")
                  .append(w.title.toLowerCase(Locale.ROOT)).append(".</p>");
            }
        }
        return sb.append("</body></html>").toString();
    }
}
