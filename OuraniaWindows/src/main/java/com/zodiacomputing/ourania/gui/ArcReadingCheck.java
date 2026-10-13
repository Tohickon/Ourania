package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ArcMidpoints;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Dial;

import java.util.ArrayList;
import java.util.List;

/**
 * That a midpoint picture reads, and that the half it does not write is actually there.
 *
 * <h3>Part B is the one holding the design up</h3>
 *
 * <p>{@link ArcMidpointReading} does not store 1,014 readings. It composes each one from a
 * catalyst sentence and <b>the pair's own conjunction prose</b>, on the argument that a midpoint
 * is the two bodies fused and a conjunction reading is what describes that. The whole design
 * rests on a measurement: every one of the 78 pairs the thirteen dial points can make has such a
 * reading, in a data file somebody already wrote.
 *
 * <p>If that stops being true - a data file edited, a body renamed, a section dropped - the
 * composition does not break. {@code getAspect} falls back to the aspect's general meaning and
 * appends "<i>a reading specific to X and Y is not written yet</i>", which is honest and is
 * exactly the wrong thing to find in a reading that claims to be about Sun/Moon. <b>So Part B
 * asserts the absence of that fallback, pair by pair, all 78.</b> It is the assertion that
 * notices a corpus going quietly thinner.
 *
 * <h3>And Part C asserts what a reading may not omit</h3>
 *
 * <p>The natal anchor. L8's doctrine is that a directed contact cannot produce what the radix
 * does not promise, so a reading that printed the picture's keywords and stopped would be
 * asserting an outcome the chart may have no capacity for. It is a constant rather than a line
 * in each catalogue entry, and this is what keeps it from being edited out of one of them.
 */
public final class ArcReadingCheck {

    private ArcReadingCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** The wording {@code getAspect} appends when a pair has no reading of its own. */
    private static final String NOT_WRITTEN = "is not written yet";

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        InterpretationService svc = InterpretationService.getInstance();

        part("A: every dial point has a catalyst sentence", () -> catalysts(svc));
        part("B: and every pair it can make has prose for the fusion", () -> fusion(svc));
        part("C: a composed reading says the three things it must", () -> composed(svc));
        part("D: the named pictures are in the catalogue, both ways round",
            () -> pictures(svc));

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

    /** The thirteen points, by the names the dial uses. */
    private static final String[] POINTS = {
        "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn",
        "Uranus", "Neptune", "Pluto", "North Node", "Ascendant", "MC",
    };

    // ------------------------------------------------------------------ Part A

    private static void catalysts(InterpretationService svc) {
        for (String p : POINTS) {
            String s = svc.getArcCatalyst(p);
            ok(p + " has a catalyst sentence", s != null && !s.isEmpty());
            ok("and it is prose rather than a key", s != null && s.length() > 60);
        }
        // <b>Named against the registry, not against this list.</b> A catalyst keyed to a body
        // the registry does not have would never be found, and would never be reported either.
        for (String p : POINTS) {
            ok(p + " is a body this app actually has", Bodies.indexOfName(p) >= 0);
        }
        ok("a body outside the dial has no catalyst, so the file is not a free-for-all",
            svc.getArcCatalyst("Ceres").isEmpty());
        ok("and an unknown name gets nothing rather than something",
            svc.getArcCatalyst("Nibiru").isEmpty());
    }

    // ------------------------------------------------------------------ Part B

    /**
     * All 78 pairs, each asked for the fusion prose the composition relies on.
     *
     * <p>Asked through {@link InterpretationService#getAspect} - the same call the reading
     * makes - rather than by reading the JSON. A check that read the files directly would pass
     * while the service served something else, which is the failure mode of every test that
     * goes round its own subject.
     */
    private static void fusion(InterpretationService svc) {
        int covered = 0;
        List<String> gaps = new ArrayList<>();
        for (int i = 0; i < POINTS.length; i++) {
            for (int j = i + 1; j < POINTS.length; j++) {
                String text = svc.getAspect(POINTS[i], POINTS[j], "Conjunction");
                boolean real = text != null && !text.isEmpty()
                    && !text.contains(NOT_WRITTEN)
                    && !text.startsWith("Interpretation not found");
                if (real) {
                    covered++;
                } else {
                    gaps.add(POINTS[i] + "/" + POINTS[j]);
                }
            }
        }
        eq("the thirteen points make 78 pairs", 78, POINTS.length * (POINTS.length - 1) / 2);
        ok("every pair has prose for its fusion, so no composed reading falls back to "
            + "\"not written yet\" (" + covered + " of 78"
            + (gaps.isEmpty() ? "" : "; MISSING " + gaps) + ")", gaps.isEmpty());

        // <b>Both orders give real prose, which is what this layer needs.</b> Dial.midpoint is
        // symmetric, so which way round a pair arrives depends on the order Dial.midpoints
        // happened to emit, and a composition that fell back for one order would fall back for
        // half the charts.
        int bothReal = 0;
        int differ = 0;
        int templatedServed = 0;
        for (int i = 0; i < POINTS.length; i++) {
            for (int j = i + 1; j < POINTS.length; j++) {
                String ab = svc.getAspect(POINTS[i], POINTS[j], "Conjunction");
                String ba = svc.getAspect(POINTS[j], POINTS[i], "Conjunction");
                boolean realAb = ab != null && !ab.isEmpty() && !ab.contains(NOT_WRITTEN);
                boolean realBa = ba != null && !ba.isEmpty() && !ba.contains(NOT_WRITTEN);
                if (realAb && realBa) {
                    bothReal++;
                }
                if (realAb && realBa && !ab.equals(ba)) {
                    differ++;
                }
                if (templated(ab)) {
                    templatedServed++;
                }
            }
        }
        eq("both orders of every pair give real prose", 78, bothReal);

        // <b>The corpus serves the written reading, not the generated filler.</b> This began
        // as a reported defect and the defect was mine: a measurement over the raw JSON files
        // picked the winner by ALPHABETICAL file order, which puts extra_bodies.json ahead of
        // interpretations.json - and the app loads interpretations.json FIRST, so putIfAbsent
        // gives the written text. Asking the service instead of simulating it returns zero
        // templated entries for all 78 pairs.
        //
        // <b>Kept as an assertion, and what protects it is not what I first wrote.</b>
        // extra_bodies.json genuinely holds a generated "<b>Sun Conjunction Moon:</b> Your core
        // identity and vital energy merges powerfully with..." for most of these pairs. The
        // first version of this comment said load order was the only thing stopping a reader
        // seeing it. A mutation that loads the extras BEFORE the main corpus survived, which is
        // how that was found to be wrong: loadData uses plain `put` and says why - "this file
        // loads first and is authoritative, and the supplementary files defer to it" - so
        // interpretations.json wins whatever order it is read in. The fragile part is that
        // sentence being true. Turn that `put` into `putIfAbsent` and the filler sticks.
        //
        // <b>Asserted at zero, not "no worse than before".</b> The first version of this was
        // written `<= 42`, which passes on 0 and on 42 alike - an assertion that cannot
        // contradict the person who wrote it, which is how it came to protect a false claim.
        ok("no dial pair is served the generated template instead of its written reading ("
            + templatedServed + " of 78)", templatedServed == 0);
        ok("and the generated entries are still in the corpus, so this guard has something to "
            + "guard (" + differ + " pairs hold two texts, one per order)", differ > 0);
    }

    // ------------------------------------------------------------------ Part C

    /** One composed reading, checked for the three parts it is made of. */
    private static void composed(InterpretationService svc) {
        ArcMidpoints.Contact c = contact("Uranus", "Sun", "Moon", 12.0);
        String html = ArcMidpointReading.of(c, 0.0, 12.0);

        ok("the reading names the picture", html.contains("SA Uranus = Sun/Moon"));
        ok("it carries the catalyst for the directed body",
            html.contains("Directed Uranus arrives"));
        ok("it carries the pair's own fusion prose, introduced as the natal pairing",
            html.contains("the natal pairing this arrives on"));
        // <b>And it is the CONJUNCTION's prose, not just some aspect's.</b> A midpoint is two
        // bodies fused, which is what a conjunction describes; read as an opposition it would
        // be a polarity, which is the opposite claim. The first version of this part only
        // checked the matrix was PRESENT, so a mutation setting FUSION to "Opposition"
        // survived - an opposition reading is present too.
        String conj = svc.getAspect("Sun", "Moon", "Conjunction");
        ok("and the fusion is the pair's conjunction, not another aspect of it",
            conj != null && !conj.isEmpty() && html.contains(conj));
        ok("and demonstrably not its opposition",
            !html.contains(svc.getAspect("Sun", "Moon", "Opposition")));
        ok("and it ends with the natal anchor", html.contains("No promise made, no promise kept"));
        ok("which is one constant rather than a line per entry",
            ArcMidpointReading.ANCHOR.contains("No promise made"));

        // <b>A picture with no specific reading still reads.</b> This is the normal case - 1,010
        // of the 1,014 - and a composition that only worked for the catalogued four would have
        // looked fine in every test written about the four.
        ArcMidpoints.Contact plain = contact("Mercury", "Jupiter", "Neptune", 30.0);
        String bare = ArcMidpointReading.of(plain, 0.0, 30.0);
        ok("an uncatalogued picture composes anyway",
            bare.contains("SA Mercury = Jupiter/Neptune")
                && bare.contains("Directed Mercury arrives"));
        ok("and does not claim a reading it has not got",
            !bare.contains(NOT_WRITTEN));
        ok("and still carries the anchor", bare.contains("No promise made"));

        // The strength line, which is the only part that depends on when it is read.
        ok("at exactness it says so", ArcMidpointReading
            .of(c, 0.0, 12.0).contains("at its peak"));
        ok("before it, it is applying", ArcMidpointReading
            .of(c, 0.0, 12.0 - ArcMidpoints.ORB / 2).contains("applying"));
        ok("after it, separating", ArcMidpointReading
            .of(c, 0.0, 12.0 + ArcMidpoints.ORB / 2).contains("separating"));
        ok("and outside the orb it says that instead of a strength", ArcMidpointReading
            .of(c, 0.0, 12.0 + ArcMidpoints.ORB * 2).contains("Outside its orb"));
        ok("with no moment given there is no strength line at all",
            !ArcMidpointReading.of(c, 0.0, Double.NaN).contains("full strength"));
        ok("and a null contact reads as nothing rather than throwing",
            ArcMidpointReading.of(null, 0.0, 0.0).isEmpty());
    }

    // ------------------------------------------------------------------ Part D

    /** The four David named, and the symmetry the catalogue has to honour. */
    private static void pictures(InterpretationService svc) {
        String[][] named = {
            {"Uranus", "Sun", "Moon"},
            {"Pluto", "Sun", "Saturn"},
            {"Jupiter", "Sun", "MC"},
            {"Uranus", "Mars", "Pluto"},
        };
        for (String[] p : named) {
            String fwd = svc.getArcPicture(p[0], p[1], p[2]);
            String rev = svc.getArcPicture(p[0], p[2], p[1]);
            ok("SA " + p[0] + " = " + p[1] + "/" + p[2] + " has a specific reading",
                fwd != null && !fwd.isEmpty());
            ok("and is found with the pair the other way round too",
                rev != null && rev.equals(fwd));
            ok("and it reads as the classical reading rather than a keyword list",
                fwd != null && fwd.contains("classical reading"));
        }
        eq("the catalogue holds exactly the four written so far", 4, svc.arcPictureCount());
        ok("a picture nobody has written returns null, which means compose it",
            svc.getArcPicture("Saturn", "Mercury", "Venus") == null);
        ok("and so does a nonsense one",
            svc.getArcPicture("Nibiru", "Sun", "Moon") == null);

        // <b>Every catalogued picture is one the scanner can actually produce.</b> A reading
        // keyed to a pair outside the dial would sit in the file forever and never be reached.
        List<String> dial = new ArrayList<>();
        for (String p : POINTS) {
            dial.add(p);
        }
        for (String[] p : named) {
            ok("SA " + p[0] + " is a point the dial directs", dial.contains(p[0]));
            ok("and " + p[1] + "/" + p[2] + " is a midpoint it can make",
                dial.contains(p[1]) && dial.contains(p[2]));
        }
    }

    // ------------------------------------------------------------------ plumbing

    /** A contact built by hand, because the reading is a pure function of one. */
    private static ArcMidpoints.Contact contact(String directed, String a, String b, double arc) {
        ArcMidpoints.Contact c = new ArcMidpoints.Contact();
        c.directed = directed;
        c.a = a;
        c.b = b;
        c.arc = arc;
        c.midpoint = 0.0;
        c.jd = 0.0;
        return c;
    }

    /** The shape the generated conjunction entries announce themselves with. */
    private static boolean templated(String s) {
        return s != null && s.matches("(?s)^<b>[A-Za-z ]+ Conjunction [A-Za-z ]+:</b>.*");
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println();
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + name.charAt(0) + ": "
            + (failures.size() == before ? "PASS" : "FAIL"));
    }

    private static void ok(String what, boolean passed) {
        checks++;
        if (passed) {
            System.out.println("  ok   " + what);
        } else {
            System.out.println("  FAIL " + what);
            failures.add(what);
        }
    }

    private static void eq(String what, Object want, Object got) {
        ok(what + " (" + got + ")", want.equals(got));
    }
}
