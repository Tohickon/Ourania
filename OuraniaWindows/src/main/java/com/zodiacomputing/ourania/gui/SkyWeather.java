package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Zodiac;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The Sky tab read as a weather report (2026-10-22).
 *
 * <b>Why it exists.</b> David, 2026-10-22: "Charts A and B should always be read from a personal
 * standpoint while sky chart should be read as more like a weather report". The Sky tab's pages
 * read the sky as if it were a birth chart ("You were born in daylight..."). These say what the
 * sky is doing for everyone, at one place.
 *
 * <b>The sky keeps its houses, read as the place's collective life.</b> David corrected the
 * first plan, which dropped them: cast for a place, the sky's houses are mundane domains - the
 * economy, the media, the government - of that place. The sky is cast for the Sky row's place
 * (SkyHouseCheck; the angles were checked against an independent sidereal-time formula on
 * 2026-10-22 for Los Angeles and London, within 0.01 degrees), so the domains move with it.
 *
 * <b>Built from parts, as the sky's Coming up is.</b> What a planet is about and the tone of a
 * sign are {@link SkyTimeline}'s, so a planet reads the same here as in the sky's timeline; the
 * mundane domains are {@link #DOMAIN}, from David's list. The house number is technical and goes
 * in the "why" box, as a natal page's does; the domain is the plain word for it.
 */
final class SkyWeather {

    private SkyWeather() {
    }

    /**
     * The mundane houses: what each covers in the life of a place (David, 2026-10-22). The 9th
     * was not in his list; it is the standard mundane 9th and is flagged for him to confirm.
     */
    static final String[] DOMAIN = {
        "the public mood and health of the place",
        "the economy, markets and national resources",
        "media, transport, news and public talk",
        "land, property, farming and local weather",
        "leisure, entertainment, children and social trends",
        "public health, workers and daily routines",
        "foreign relations, public partnerships and open conflicts",
        "debt, taxes, banks, insurance and loss",
        "law courts, religion, universities and foreign trade",
        "leaders, government and authority",
        "parliaments, councils, alliances and social movements",
        "institutions, hidden matters and work behind the scenes"
    };

    /** The sky's planets, in the order a report reads them. */
    static final List<String> BODIES = List.of("Sun", "Moon", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron", "North Node");

    /** The slow planets: the ones whose meetings are the season's story. */
    private static final List<String> SLOW = List.of("Jupiter", "Saturn", "Uranus", "Neptune",
        "Pluto", "Chiron", "North Node");

    /** How close two of the sky's planets must be to count as meeting. */
    static final double ORB = 4.0;

    /** The domain a house covers, or null for no house. */
    static String domain(int house) {
        return house >= 1 && house <= 12 ? DOMAIN[house - 1] : null;
    }

    /** "The sky over Los Angeles", or "The sky" when the place has no name. */
    static String title(String place) {
        return place == null || place.isBlank() ? "The sky" : "The sky over " + place.trim();
    }

    // ------------------------------------------------------------------ one planet

    /** One of the sky's planets, read: where it is and what it is doing there. */
    record Planet(String name, double lon, boolean retrograde, int sign, int house) {

        /** "Mars in Leo". */
        String where() {
            return name + " in " + signName(sign);
        }

        /** What this planet is about, in daily life; the Moon as the public mood, with no "you". */
        String area() {
            return "Moon".equals(name) ? "the public mood"
                : SkyTimeline.AREA.getOrDefault(name, name);
        }

        /** The planet as the subject of a sentence: "The Sun", "Mars", "The North Node". */
        String subject() {
            return ("Sun".equals(name) || "Moon".equals(name) || "North Node".equals(name)
                ? "The " : "") + name;
        }

        /**
         * "Mars sets a warm, proud, playful tone for drive, effort and tempers". Said this way
         * round because the areas are lists ("drive, effort and tempers") and "takes on" read as
         * a verb for one thing: the first render said "drive, effort and tempers takes on".
         */
        String tone() {
            return subject() + " sets " + SkyTimeline.TONE[sign] + " for " + area();
        }

        /** The weather sentence: what the planet is about, its sign's tone, and where it shows. */
        String weather() {
            String d = domain(house);
            String s = tone() + (d == null ? "" : ", mostly in " + d) + ".";
            if (retrograde) {
                s += " It is moving backward for now, so this is a time to review rather than"
                    + " to start.";
            }
            return s;
        }

        /** The technical line for the "why" box. */
        String why() {
            return name + " in " + signName(sign) + String.format(Locale.ROOT, " %.1f°",
                lon % 30.0) + (retrograde ? " Rx" : "")
                + (house > 0 ? ", " + ordinal(house) + " house" : "");
        }
    }

    /** The sky's planets as cast, each with its house at the sky's place. */
    static List<Planet> planets(ChartFrame f) {
        List<Planet> out = new ArrayList<>();
        if (f == null) {
            return out;
        }
        for (String name : BODIES) {
            ChartFrame.Body b = f.body(name);
            if (b == null || !b.ok) {
                continue;
            }
            out.add(new Planet(name, b.lon, b.retrograde, Zodiac.signIndex(b.lon),
                f.cusps == null ? 0 : Zodiac.houseOf(b.lon, f.cusps)));
        }
        return out;
    }

    // ------------------------------------------------------------------ two planets meeting

    /** Two of the sky's planets in a major aspect. */
    record Meeting(Planet a, Planet b, Aspects.Type type, double off, boolean building) {

        /** "Saturn and Neptune join forces". */
        String headline() {
            return a.name() + " and " + b.name() + " " + verb(type);
        }

        /** What it means for everyone, and where it shows at this place. */
        String weather() {
            String da = domain(a.house());
            String db = domain(b.house());
            String where = da == null ? "" : da.equals(db) ? " It shows in " + da + "."
                : " It shows in " + da + (db == null ? "" : ", and in " + db) + ".";
            return a.subject() + " (" + a.area() + ") and " + b.name() + " (" + b.area() + ") "
                + verb(type) + "." + where
                + (building ? " It is building." : " It is fading.");
        }

        String why() {
            return a.name() + " " + type.label.toLowerCase(Locale.ROOT) + " " + b.name()
                + String.format(Locale.ROOT, ", %.1f° from exact, %s", off,
                    building ? "applying" : "separating");
        }
    }

    /** The major aspects between the sky's planets, slow pairs first and then the tightest. */
    static List<Meeting> meetings(ChartFrame f, List<Planet> ps, boolean withMoon) {
        List<Meeting> out = new ArrayList<>();
        for (int i = 0; i < ps.size(); i++) {
            for (int j = i + 1; j < ps.size(); j++) {
                Planet a = ps.get(i);
                Planet b = ps.get(j);
                if (!withMoon && ("Moon".equals(a.name()) || "Moon".equals(b.name()))) {
                    continue;
                }
                if ("North Node".equals(a.name()) || "North Node".equals(b.name())) {
                    continue;
                }
                double sep = Aspects.separation(a.lon(), b.lon());
                Aspects.Type t = Aspects.typeWithin(sep, a.name(), b.name(), ORB);
                if (t == null || verb(t) == null) {
                    continue;
                }
                double off = Math.abs(sep - t.exactAngle);
                out.add(new Meeting(a, b, t, off, Aspects.isApplying(a.lon(), speed(f, a.name()),
                    b.lon(), speed(f, b.name()), t)));
            }
        }
        out.sort(Comparator.comparingInt((Meeting m) -> -slowCount(m))
            .thenComparingDouble(Meeting::off));
        return out;
    }

    private static int slowCount(Meeting m) {
        return (SLOW.contains(m.a().name()) ? 1 : 0) + (SLOW.contains(m.b().name()) ? 1 : 0);
    }

    private static double speed(ChartFrame f, String name) {
        ChartFrame.Body b = f.body(name);
        return b == null ? 0.0 : b.lonSpeed;
    }

    /** The aspect said as the sky's Coming up says it; null for a minor one. */
    static String verb(Aspects.Type t) {
        return switch (t) {
            case CONJUNCTION -> "join forces";
            case OPPOSITION -> "pull against each other";
            case SQUARE -> "clash";
            case TRINE -> "work together easily";
            case SEXTILE -> "open a door for each other";
            default -> null;
        };
    }

    // ------------------------------------------------------------------ the Moon's phase

    /** The Moon's phase and what it is good for, from the Sun-Moon angle. */
    static String[] phase(ChartFrame f) {
        ChartFrame.Body sun = f.body("Sun");
        ChartFrame.Body moon = f.body("Moon");
        if (sun == null || moon == null || !sun.ok || !moon.ok) {
            return null;
        }
        double e = ((moon.lon - sun.lon) % 360 + 360) % 360;
        String[][] phases = {
            {"a New Moon", "a time to start things quietly"},
            {"a waxing crescent Moon", "plans take their first steps"},
            {"a first quarter Moon", "a push to act on what has started"},
            {"a waxing gibbous Moon", "a time to adjust and improve"},
            {"a Full Moon", "things come to a head and feelings run high"},
            {"a waning gibbous Moon", "a time to share what has been learned"},
            {"a last quarter Moon", "a time to clear out what is not working"},
            {"a waning crescent Moon", "a time to rest before the next start"}};
        return phases[(int) (((e + 22.5) % 360) / 45.0)];
    }

    // ------------------------------------------------------------------ the pages

    /** The three cards of At a glance on every tab, in their order (2026-10-22). */
    static final String FROM = "WHAT THE SKY IS WORKING FROM";
    static final String NOW = "WHERE THE WEIGHT SITS NOW";
    static final String HEADING = "WHERE IT IS HEADING";

    /**
     * At a glance: the chart's three cards, said as weather (David, 2026-10-22: the same
     * structure on every tab). What the sky is working from is the slow background - the slow
     * planets meeting and the ones moving backward; where the weight sits now is today's
     * conditions; where it is heading is the fast contacts that are building.
     */
    static List<ReadingCard> glance(ChartFrame f, String place) {
        List<ReadingCard> parts = glanceParts(f, place);
        ReadingCard from = null;
        ReadingCard now = null;
        ReadingCard heading = null;
        for (ReadingCard c : parts) {
            switch (c.label()) {
                case "THE BIG STORY" -> from = relabel(c, FROM);
                case "CONDITIONS" -> now = relabel(c, NOW);
                case "WATCH FOR" -> heading = relabel(c, HEADING);
                default -> { }
            }
        }
        if (heading == null) {
            heading = new ReadingCard(HEADING, "Nothing new is building",
                "No contact between the faster planets is building right now. The weather"
                    + " holds as it is for the next few days.", List.of());
        }
        List<ReadingCard> out = new ArrayList<>();
        for (ReadingCard c : new ReadingCard[] {from, now, heading}) {
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    private static ReadingCard relabel(ReadingCard c, String label) {
        return new ReadingCard(label, c.headline(), c.body(), c.why());
    }

    /** The conditions, the big story, and what to watch for. */
    private static List<ReadingCard> glanceParts(ChartFrame f, String place) {
        List<ReadingCard> out = new ArrayList<>();
        List<Planet> ps = planets(f);
        Planet sun = find(ps, "Sun");
        Planet moon = find(ps, "Moon");
        String[] ph = phase(f);
        if (sun != null && moon != null) {
            StringBuilder b = new StringBuilder();
            b.append(sun.tone()).append(". ").append(moon.tone()).append('.');
            if (ph != null) {
                b.append(" It is ").append(ph[0]).append(": ").append(ph[1]).append('.');
            }
            int asc = f.cusps == null ? -1 : Zodiac.signIndex(f.asc);
            if (asc >= 0) {
                b.append("\n\nIn ").append(place == null || place.isBlank() ? "this place"
                    : place.trim()).append(", the rising sign sets ")
                    .append(SkyTimeline.TONE[asc]).append(" for the day.");
                List<String> rising = new ArrayList<>();
                for (Planet p : ps) {
                    if (p.house() == 1) {
                        rising.add(p.name());
                    }
                }
                if (!rising.isEmpty()) {
                    b.append(' ').append(list(rising))
                        .append(rising.size() == 1 ? " sits" : " sit")
                        .append(" close to the horizon there, so ")
                        .append(rising.size() == 1 ? "it colours" : "they colour")
                        .append(" the mood of the day.");
                }
            }
            out.add(new ReadingCard("CONDITIONS", sun.where() + ", " + moon.where(),
                b.toString(), List.of(sun.why(), moon.why(),
                    "Ascendant " + Zodiac.format(f.asc))));
        }

        List<Meeting> all = meetings(f, ps, false);
        List<Meeting> slow = new ArrayList<>();
        for (Meeting m : all) {
            if (slowCount(m) == 2) {
                slow.add(m);
            }
        }
        List<String> backward = new ArrayList<>();
        for (Planet p : ps) {
            if (p.retrograde() && !"Moon".equals(p.name()) && !"Sun".equals(p.name())
                    && !"North Node".equals(p.name())) {
                backward.add(p.name());
            }
        }
        if (!slow.isEmpty() || !backward.isEmpty()) {
            StringBuilder b = new StringBuilder();
            List<String> why = new ArrayList<>();
            String headline;
            if (!slow.isEmpty()) {
                Meeting m = slow.get(0);
                headline = m.headline();
                b.append(m.weather()).append(" This is background weather that lasts for"
                    + " weeks or months, and it touches everyone.");
                why.add(m.why());
            } else {
                headline = "A season of looking back";
            }
            if (!backward.isEmpty()) {
                b.append(b.length() == 0 ? "" : "\n\n").append(list(backward))
                    .append(backward.size() == 1 ? " is" : " are")
                    .append(" moving backward. Old business comes back to be finished.");
                why.add(String.join(", ", backward) + " retrograde");
            }
            out.add(new ReadingCard("THE BIG STORY", headline, b.toString(), why));
        } else {
            out.add(new ReadingCard("THE BIG STORY", "A steady background",
                "No slow planets meet right now, and none is moving backward. The wider"
                    + " weather is calm, so the faster planets set the pace.", List.of()));
        }

        List<Meeting> fast = new ArrayList<>();
        for (Meeting m : all) {
            if (slowCount(m) < 2 && m.building()) {
                fast.add(m);
            }
        }
        fast.sort(Comparator.comparingDouble(Meeting::off));
        if (!fast.isEmpty()) {
            StringBuilder b = new StringBuilder();
            List<String> why = new ArrayList<>();
            for (Meeting m : fast.subList(0, Math.min(3, fast.size()))) {
                b.append(b.length() == 0 ? "" : "\n\n").append(m.weather());
                why.add(m.why());
            }
            out.add(new ReadingCard("WATCH FOR", "In the next few days", b.toString(), why));
        }
        return out;
    }

    /**
     * One planet's weather sentence, for the Reading page's card (2026-10-22): the Sky tab's
     * Reading keeps the chart's structure and says each planet as weather. Null for a point
     * this report does not read.
     */
    static String weatherFor(ChartFrame f, String name) {
        for (Planet p : planets(f)) {
            if (p.name().equals(name)) {
                return p.weather();
            }
        }
        return null;
    }

    /**
     * Synthesize: only what at least three separate parts of the current sky agree on (David,
     * 2026-10-22). The same count the chart's Synthesize uses ({@link Themes}), with the
     * sky's own words for each theme ({@code sky.<theme>} in synthesize_cards.json).
     */
    static String synthesize(Themes.Result t, String place, boolean showWhy) {
        StringBuilder sb = ReadingCard.page("Synthesize", "Only what at least "
            + Themes.signatureThreshold + " separate parts of the current sky agree on. One"
            + " placement alone is a detail; this is the pattern.");
        List<ReadingCard> cards = new ArrayList<>();
        if (t != null) {
            for (Themes.Signature s : Themes.signaturesAtThreshold(t)) {
                String[] w = words(s.theme);
                List<String> why = new ArrayList<>();
                why.add(s.witnessCount + " independent witnesses for \"" + s.theme + "\"");
                for (List<Themes.Candidate> comp : s.components) {
                    why.add(comp.get(0).text);
                }
                cards.add(new ReadingCard("[ " + s.witnessCount + " PARTS AGREE ]", w[0], w[1],
                    why));
            }
            for (Themes.Contradiction c : t.contradictions) {
                String[] w = words(c.themeA + "_vs_" + c.themeB);
                if (w == MISSING) {
                    w = words(c.themeB + "_vs_" + c.themeA);
                }
                cards.add(new ReadingCard("[ A PULL IN TWO DIRECTIONS ]", w[0], w[1],
                    List.of(c.themeA + " (" + c.witnessesA + ") against " + c.themeB + " ("
                        + c.witnessesB + ")", c.strongestA.text, c.strongestB.text)));
            }
        }
        if (cards.isEmpty()) {
            cards.add(new ReadingCard("NO STRONG PATTERN", "No theme repeats "
                + Themes.signatureThreshold + " times",
                "The sky spreads its weight right now. At a glance and Interpretation describe"
                    + " the parts.", List.of()));
        }
        sb.append(ReadingCard.html(cards, showWhy));
        return sb.append("</body></html>").toString();
    }

    private static final String[] MISSING = {"A repeated note in the sky",
        "Several separate parts of the sky say the same thing right now. The parts are listed"
            + " under the astrology."};

    /** The sky's headline and body for a theme, or the honest fallback. */
    private static String[] words(String key) {
        InterpretationService svc = InterpretationService.getInstance();
        String h = svc.getSynthesizePart("sky." + key + ".headline");
        String b = svc.getSynthesizePart("sky." + key + ".body");
        return h == null || b == null ? MISSING : new String[] {h, b};
    }

    // ------------------------------------------------------------------ small helpers

    /** "Saturn", "Saturn and Neptune", "Saturn, Neptune and Chiron". */
    static String list(List<String> names) {
        if (names.size() <= 1) {
            return names.isEmpty() ? "" : names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and "
            + names.get(names.size() - 1);
    }

    private static Planet find(List<Planet> ps, String name) {
        for (Planet p : ps) {
            if (p.name().equals(name)) {
                return p;
            }
        }
        return null;
    }

    static String signName(int sign) {
        String s = Zodiac.SIGNS[sign];
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String ordinal(int n) {
        int mod100 = n % 100;
        String suffix = mod100 >= 11 && mod100 <= 13 ? "th"
            : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suffix;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
