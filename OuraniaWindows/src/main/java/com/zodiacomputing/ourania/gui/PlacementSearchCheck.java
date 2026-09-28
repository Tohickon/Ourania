package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.ArrayList;
import java.util.List;

/**
 * Asking the chart book for a placement (G7).
 *
 * <p><b>The row's own example was the thing that could not be asked</b> - "every chart with Moon in
 * Scorpio". The book searched names, notes, places and tags, all of them things somebody typed, and
 * nothing had ever computed a chart for an entry sitting in it.
 *
 * <p><b>Part A is mostly about what does NOT parse.</b> A parser that guessed would turn a search
 * for a client called Mars into a sweep of the ephemeris on every keystroke, so the assertions that
 * matter are the ones saying "this is a name, leave it alone".
 *
 * <p><b>Part C searches a real book of real charts.</b> The expectation is derived from the chart
 * each entry casts to, not written down: a fixture saying "Ada is Moon in Scorpio" asserts that I
 * typed the fixture correctly, which is not the question.
 */
public final class PlacementSearchCheck {

    private PlacementSearchCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        try {
            part("A: a placement question is told from a name", PlacementSearchCheck::parsing);
            part("B: one rule for what instant a place keeps", PlacementSearchCheck::moments);
            part("C: the book answers a placement", PlacementSearchCheck::book);
            part("D: and the search box is the door", PlacementSearchCheck::door);
        } finally {
            Moments.forget();
        }
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
        System.exit(0);
    }

    // ---------------------------------------------------------------- Part A

    private static void parsing() {
        // The three forms, in the words people use.
        parses("moon in scorpio", "Moon in Scorpio");
        parses("Moon Scorpio", "Moon in Scorpio");
        parses("  MOON   IN   SCORPIO  ", "Moon in Scorpio");
        parses("mars in 7", "Mars in house 7");
        parses("mars in the 7th", "Mars in house 7");
        parses("mars 7th house", "Mars in house 7");
        parses("mercury retrograde", "Mercury retrograde");
        parses("mercury rx", "Mercury retrograde");
        parses("north node in aries", "North Node in Aries");
        parses("north_node in aries", "North Node in Aries");

        // <b>What must NOT parse, which is the half that keeps the search box quick.</b>
        notAPlacement("");
        notAPlacement("   ");
        notAPlacement(null);
        notAPlacement("moon");                 // a name search for a client called Moon
        notAPlacement("mars");
        notAPlacement("Martha");
        notAPlacement("client");
        notAPlacement("London");
        notAPlacement("moon in nowhere");
        notAPlacement("moon in 13");           // there is no thirteenth house
        notAPlacement("moon in 0");
        notAPlacement("1984-09-08");

        PlacementQuery node = PlacementQuery.parse("north node in aries");
        ok("a two-word body name is taken whole ("
            + (node == null ? "null" : node.body.name) + ")",
            node != null && node.body.name.equals("North Node"));

        // <b>Why the longest-first walk is not asserted directly, and what is asserted
        // instead.</b> The parser prefers the longest matching name so one body's name cannot be
        // swallowed inside another's - and measured, NO registry name is a prefix of any other,
        // so the ordering changes no answer today and a mutation reversing it passes everything.
        // The rule is kept for the body that gets added next; this is what says when it starts
        // to matter.
        List<String> spellings = new ArrayList<>();
        for (int i = 0; i < com.zodiacomputing.ourania.astro.Bodies.count(); i++) {
            com.zodiacomputing.ourania.astro.Bodies.Def d =
                com.zodiacomputing.ourania.astro.Bodies.at(i);
            spellings.add(d.name.toLowerCase());
            spellings.add(d.id.toLowerCase());
            spellings.add(d.id.toLowerCase().replace('_', ' '));
        }
        List<String> prefixes = new ArrayList<>();
        for (String x : spellings) {
            for (String y : spellings) {
                if (!x.equals(y) && !x.isEmpty() && y.startsWith(x)) {
                    prefixes.add(x + " inside " + y);
                }
            }
        }
        ok("no body name is a prefix of another (" + spellings.size() + " spellings): "
            + (prefixes.isEmpty() ? "none"
                : prefixes + " - bodyIn's longest-first rule now matters and wants a case here"),
            prefixes.isEmpty());
    }

    // ---------------------------------------------------------------- Part B

    /**
     * A moment is local to the place, and both screens now say so through one method.
     *
     * <b>Two zones give two instants, and the difference is the assertion.</b> A check that only
     * showed the method returned something would pass whatever it returned.
     */
    private static void moments() {
        Geocoder.Result sydney = new Geocoder.Result();
        sydney.lat = -33.8688;
        sydney.lon = 151.2093;
        sydney.tzId = "Australia/Sydney";

        Geocoder.Result london = new Geocoder.Result();
        london.lat = 51.5;
        london.lon = -0.12;
        london.tzId = "Europe/London";

        double inSydney = Moments.julianDay("1975-07-04", "14:00", sydney);
        double inLondon = Moments.julianDay("1975-07-04", "14:00", london);
        ok("the same clock time at two places is two instants",
            Math.abs(inSydney - inLondon) > 0.2);
        ok("and Sydney's is the earlier of the two", inSydney < inLondon);

        ok("the election screen and this ask the same thing",
            ElectionalPanel.zoneOf(sydney).equals(Moments.zoneOf(sydney)));
        ok("a place with no zone falls back to this computer's",
            Moments.zoneOf(new Geocoder.Result()).equals(java.time.ZoneId.systemDefault()));
        ok("and so does a nonsense one", Moments.zoneOf(zoned("Mars/Olympus"))
            .equals(java.time.ZoneId.systemDefault()));

        ok("a date that will not read answers NaN",
            Double.isNaN(Moments.julianDay("the fourth of july", "14:00", sydney)));
        ok("a missing time is taken as noon rather than refused",
            !Double.isNaN(Moments.julianDay("1975-07-04", "", sydney)));
    }

    private static Geocoder.Result zoned(String id) {
        Geocoder.Result r = new Geocoder.Result();
        r.tzId = id;
        return r;
    }

    // ---------------------------------------------------------------- Part C

    /**
     * A real book, searched.
     *
     * <b>The expected answer is computed from the charts, not typed.</b> Writing "Ada is the one
     * with Moon in Scorpio" asserts that the fixture was typed correctly; deriving it asserts that
     * the search agrees with the ephemeris.
     */
    private static void book() {
        String[][] people = {
            {"Ada", "1815-12-10", "12:00", "London, England"},
            {"Bee", "1975-07-04", "04:15", "Sydney, Australia"},
            {"Cal", "1984-09-08", "07:33", "Chicago, Illinois"},
            {"Dee", "2000-01-01", "00:00", "London, England"},
        };
        for (String[] p : people) {
            SavedCharts.put(p[0], p[1], p[2], p[3]);
        }
        ok("the book has the fixture charts (" + SavedCharts.names().size() + ")",
            SavedCharts.names().size() >= people.length);

        // Every chart, cast the way the search will cast it - and if none of them casts, the
        // assertions below would all pass vacuously, so that is checked first.
        java.util.Map<String, ChartFrame> frames = new java.util.LinkedHashMap<>();
        for (String[] p : people) {
            ChartFrame f = Moments.frameOf(SavedCharts.get(p[0]));
            if (f != null) {
                frames.put(p[0], f);
            }
        }
        ok("every fixture chart casts (" + frames.size() + " of " + people.length + ")",
            frames.size() == people.length);
        if (frames.isEmpty()) {
            return;
        }

        // For each sign the Moon actually occupies, the search must return exactly the charts
        // whose Moon is there.
        int moon = com.zodiacomputing.ourania.astro.Bodies.indexOf("moon");
        java.util.Map<Integer, List<String>> expected = new java.util.TreeMap<>();
        for (java.util.Map.Entry<String, ChartFrame> e : frames.entrySet()) {
            ChartFrame.Body b = e.getValue().bodies[moon];
            if (b != null && b.ok) {
                expected.computeIfAbsent(Zodiac.signIndex(b.lon), k -> new ArrayList<>())
                    .add(e.getKey());
            }
        }
        ok("the fixture Moons are spread over more than one sign (" + expected.size() + ")",
            expected.size() > 1);
        for (java.util.Map.Entry<Integer, List<String>> e : expected.entrySet()) {
            String sign = Zodiac.SIGNS[e.getKey()];
            List<String> got = Moments.matching(
                PlacementQuery.parse("moon in " + sign), new int[1]);
            java.util.Collections.sort(got);
            List<String> want = new ArrayList<>(e.getValue());
            java.util.Collections.sort(want);
            ok("\"moon in " + sign + "\" answers " + want + " (" + got + ")", got.equals(want));
        }

        // A sign no fixture Moon is in answers nothing - the assertion that stops the search
        // returning the whole book.
        java.util.Set<Integer> empty = new java.util.TreeSet<>();
        for (int i = 0; i < 12; i++) {
            empty.add(i);
        }
        empty.removeAll(expected.keySet());
        int none = empty.iterator().next();
        ok("\"moon in " + Zodiac.SIGNS[none] + "\" answers nothing",
            Moments.matching(PlacementQuery.parse("moon in " + Zodiac.SIGNS[none]),
                new int[1]).isEmpty());

        // A house search, derived the same way.
        int sun = com.zodiacomputing.ourania.astro.Bodies.indexOf("sun");
        ChartFrame first = frames.values().iterator().next();
        java.util.Map<Integer, List<String>> byHouse = new java.util.TreeMap<>();
        for (java.util.Map.Entry<String, ChartFrame> e : frames.entrySet()) {
            ChartFrame.Body b = e.getValue().bodies[sun];
            if (b != null && b.ok) {
                byHouse.computeIfAbsent(Zodiac.houseOf(b.lon, e.getValue().cusps),
                    k -> new ArrayList<>()).add(e.getKey());
            }
        }
        ok("the fixture Suns are in more than one house (" + byHouse.size() + ")",
            byHouse.size() > 1);
        // <b>The exact set, not containment.</b> Asserting only that the chart it was derived
        // from comes back passed against a match answering by SIGN instead of by house: for one
        // chart the two agree often enough by coincidence.
        for (java.util.Map.Entry<Integer, List<String>> e : byHouse.entrySet()) {
            List<String> got = Moments.matching(
                PlacementQuery.parse("sun in " + e.getKey()), new int[1]);
            java.util.Collections.sort(got);
            List<String> want = new ArrayList<>(e.getValue());
            java.util.Collections.sort(want);
            ok("\"sun in " + e.getKey() + "\" answers " + want + " (" + got + ")",
                got.equals(want));
        }

        // <b>Retrograde, derived the same way.</b> Nothing asserted this at first and a match
        // that answered "yes" for every body passed the whole suite - the third form of the
        // query, and the one with no sign or house to make it obviously wrong.
        // <b>A body that IS retrograde in some of these charts and not others</b>, found rather
        // than named: asserting Mercury gave an empty expected set on this fixture, so the match
        // was only ever being asked to say no and a version answering "yes" to everything passed.
        String splitBody = null;
        List<String> retro = new ArrayList<>();
        for (int i = 0; i < com.zodiacomputing.ourania.astro.Bodies.count() && splitBody == null;
                i++) {
            com.zodiacomputing.ourania.astro.Bodies.Def d =
                com.zodiacomputing.ourania.astro.Bodies.at(i);
            List<String> yes = new ArrayList<>();
            int asked = 0;
            for (java.util.Map.Entry<String, ChartFrame> e : frames.entrySet()) {
                ChartFrame.Body b = e.getValue().bodies[i];
                if (b != null && b.ok) {
                    asked++;
                    if (b.retrograde) {
                        yes.add(e.getKey());
                    }
                }
            }
            if (!yes.isEmpty() && yes.size() < asked) {
                splitBody = d.name;
                retro = yes;
            }
        }
        ok("some body is retrograde in some fixture charts and not others ("
            + splitBody + ")", splitBody != null);
        if (splitBody != null) {
            List<String> gotRetro = Moments.matching(
                PlacementQuery.parse(splitBody + " retrograde"), new int[1]);
            java.util.Collections.sort(gotRetro);
            java.util.Collections.sort(retro);
            ok("\"" + splitBody + " retrograde\" answers " + retro + " (" + gotRetro + ")",
                gotRetro.equals(retro));
            ok("which is neither none nor all of them (" + retro.size() + " of "
                + frames.size() + ")", !retro.isEmpty() && retro.size() < frames.size());
        }

        // A chart whose place the atlas cannot find is counted, not silently dropped.
        SavedCharts.put("Nowhere", "1990-01-01", "12:00", "Xyzzy Under Qwerty");
        int[] unreadable = new int[1];
        Moments.matching(PlacementQuery.parse("moon in scorpio"), unreadable);
        ok("a chart that cannot be cast is counted (" + unreadable[0] + ")", unreadable[0] >= 1);
        SavedCharts.remove("Nowhere");

        for (String[] p : people) {
            SavedCharts.remove(p[0]);
        }
    }

    // ---------------------------------------------------------------- Part D

    /**
     * The search box reaches both searches.
     *
     * <b>And the text half had no door at all.</b> {@code SavedCharts.search} matches the name,
     * the notes, the place and the tags; the panel matched the name, so three quarters of it was
     * unreachable.
     */
    private static void door() throws Exception {
        SavedCharts.put("Ada", "1815-12-10", "12:00", "London, England",
            com.zodiacomputing.ourania.astro.Rodden.AA, "a mathematician", "research");
        SavedCharts.put("Bee", "1975-07-04", "04:15", "Sydney, Australia",
            com.zodiacomputing.ourania.astro.Rodden.A, "", "client");

        final ProfileListPanel[] panel = new ProfileListPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> panel[0] = new ProfileListPanel(null));
        ProfileListPanel p = panel[0];

        ok("an empty box shows the whole book",
            p.matching("").size() == SavedCharts.names().size());
        ok("a name still matches", p.matching("Ada").contains("Ada"));
        ok("a note matches now", p.matching("mathematician").contains("Ada"));
        ok("a place matches now", p.matching("Sydney").contains("Bee"));
        ok("a tag matches now", p.matching("client").contains("Bee"));
        ok("and a note does not match the wrong chart",
            !p.matching("mathematician").contains("Bee"));

        List<String> placement = p.matching("moon in scorpio");
        ok("a placement query goes to the placement search",
            placement.size() < SavedCharts.names().size());
        ok("and a word that is not one goes to the text search",
            p.matching("Xyzzy").isEmpty());

        SavedCharts.remove("Ada");
        SavedCharts.remove("Bee");
    }

    // ---------------------------------------------------------------- plumbing

    private static void parses(String text, String reads) {
        PlacementQuery q = PlacementQuery.parse(text);
        ok("\"" + text.trim() + "\" reads as " + reads
            + (q == null ? " (did not parse)" : " (" + q.describe() + ")"),
            q != null && reads.equals(q.describe()));
    }

    private static void notAPlacement(String text) {
        ok("\"" + text + "\" is not a placement question", PlacementQuery.parse(text) == null);
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
