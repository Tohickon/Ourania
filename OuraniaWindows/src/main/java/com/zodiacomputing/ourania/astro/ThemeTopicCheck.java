package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * L7 reads L6, and L6 can ask L7 what it is a witness to (F14).
 *
 * <p><b>The row said there was no cross-reference either way, and that was exactly true.</b>
 * {@code Themes} gathered from L3 bodies and L5 gestalt and never from L6; {@code Topics} never
 * mentioned {@code Themes}. Measured on 28 September: neither class named the other anywhere.
 *
 * <p><b>Part C is the part this needed before the wiring could be trusted.</b> L7's whole method is
 * that a theme counts witnesses only when their provenance sets are disjoint - one loud placement
 * generating a dozen statements is one witness, not twelve. A topic's standing is made FROM bodies
 * that already produced candidates, so the obvious wiring inflates every count it touches. Two
 * topics sharing a ruler must not be two independent witnesses that the chart consolidates.
 *
 * <p><b>Part D is a sweep over the vocabulary itself.</b> A contradiction pair whose words nothing
 * ever emits is a rule no failure can reach, which this project has logged four times under other
 * names. The pair F14 adds is checked the same way as the six that were already there.
 */
public final class ThemeTopicCheck {

    private ThemeTopicCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** Three charts, so a vocabulary sweep is not answering for one person's placements. */
    private static final double[][] CHARTS = {
        {1984, 9, 8, 7.55, 41.8781, -87.6298},
        {1975, 7, 4, 4.25, -33.8688, 151.2093},
        {1950, 1, 15, 18.0, 51.4779, -0.0015},
    };

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        part("A: with no topics, nothing changes", ThemeTopicCheck::unchanged);
        part("B: a reported topic becomes a witness", ThemeTopicCheck::arrives);
        part("C: a topic does not vote twice for its own bodies",
            ThemeTopicCheck::independence);
        part("D: every declared theme word is one something emits",
            ThemeTopicCheck::vocabulary);
        part("E: a topic can say which signatures it helped make", ThemeTopicCheck::back);
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

    // ---------------------------------------------------------------- Part A

    /**
     * The three-argument call the six existing callers use answers as it did.
     *
     * <b>Asserted against the source word, not against a remembered count.</b> Every candidate a
     * topic contributes is marked L6, so "no L6 candidates" is the whole claim, and it cannot be
     * satisfied by a count that happens to match.
     */
    private static void unchanged() {
        Fixture fx = fixture(0);
        Themes.Result without = Themes.extract(fx.frame, fx.gestalt, fx.ranked);
        Themes.Result explicitNull = Themes.extract(fx.frame, fx.gestalt, fx.ranked, null);

        ok("no candidate comes from L6 when topics were not passed",
            countFrom(without, "L6") == 0);
        ok("and the three-argument form is the same as passing null (" + without.candidates.size()
            + " vs " + explicitNull.candidates.size() + ")",
            without.candidates.size() == explicitNull.candidates.size());
        ok("an empty topic list is also harmless",
            countFrom(Themes.extract(fx.frame, fx.gestalt, fx.ranked,
                new ArrayList<>()), "L6") == 0);
        ok("there were candidates to begin with (" + without.candidates.size() + ")",
            without.candidates.size() > 10);
    }

    // ---------------------------------------------------------------- Part B

    /** With topics passed, the reported ones arrive carrying their house's theme. */
    private static void arrives() {
        Fixture fx = fixture(0);
        Themes.Result with = Themes.extract(fx.frame, fx.gestalt, fx.ranked, fx.topics);

        int reported = 0;
        for (Topics.Topic t : fx.topics) {
            if (t.worthReporting) {
                reported++;
            }
        }
        ok("the chart has topics worth reporting (" + reported + ")", reported > 0);
        ok("and each one is a candidate (" + countFrom(with, "L6") + ")",
            countFrom(with, "L6") == reported);

        Set<String> emitted = new LinkedHashSet<>();
        for (Themes.Candidate c : with.candidates) {
            if ("L6".equals(c.source)) {
                emitted.addAll(c.themes);
            }
        }
        boolean anyHouseTheme = false;
        for (Topics.Topic t : fx.topics) {
            if (t.worthReporting) {
                anyHouseTheme |= emitted.contains(Themes.TOPIC_THEME[t.house - 1]);
            }
        }
        ok("carrying the theme of the house it is about", anyHouseTheme);

        // Every L6 candidate names the bodies its standing rests on, or the house when it has
        // none - an empty provenance set is disjoint from every other one, which would make
        // every bodiless topic independent of everything.
        boolean allNamed = true;
        for (Themes.Candidate c : with.candidates) {
            if ("L6".equals(c.source) && c.provenance.isEmpty()) {
                allNamed = false;
            }
        }
        ok("and naming what it rests on", allNamed);

        ok("the twelve house themes are all different",
            new LinkedHashSet<>(java.util.Arrays.asList(Themes.TOPIC_THEME)).size() == 12);

        // <b>A topic with no bodies at all, built because no real chart here has one.</b> An empty
        // provenance set is disjoint from every other one, so without a fallback every bodiless
        // topic would count as independent of everything - including of other bodiless topics.
        // Strength matters: a candidate under Themes.witnessFloor is present in the chart and
        // too quiet to be a witness, and the first version of this fixture left it at zero - so
        // both topics were correctly dropped and the assertion below failed against behaviour
        // that was right. The floor applies to a topic exactly as it does to a placement.
        Topics.Topic bare = new Topics.Topic();
        bare.house = 8;
        bare.worthReporting = true;
        bare.strength = 1.0;
        bare.agreement = Topics.Agreement.ALL_WEAK;
        Topics.Topic alsoBare = new Topics.Topic();
        alsoBare.house = 12;
        alsoBare.worthReporting = true;
        alsoBare.strength = 1.0;
        alsoBare.agreement = Topics.Agreement.ALL_WEAK;

        Themes.Result bareResult = Themes.extract(fx.frame, fx.gestalt, new ArrayList<>(),
            java.util.Arrays.asList(bare, alsoBare));
        boolean named = true;
        for (Themes.Candidate c : bareResult.candidates) {
            if ("L6".equals(c.source) && c.provenance.isEmpty()) {
                named = false;
            }
        }
        ok("a topic with no bodies still names where it stands", named);

        // <b>The floor applies to a topic as it does to a placement.</b> Without this nothing
        // asserted that a topic's own strength reaches the count at all - a mutation pinning
        // every topic's weight at 1.0 passed every other assertion here. Three barely-standing
        // houses out-voting one emphatic placement is the naive-counting failure L7 was built
        // to avoid, wearing L6's clothes.
        Topics.Topic quiet = new Topics.Topic();
        quiet.house = 3;
        quiet.worthReporting = true;
        quiet.strength = Themes.witnessFloor / 2.0;
        quiet.agreement = Topics.Agreement.ALL_WEAK;
        Topics.Topic loud = new Topics.Topic();
        loud.house = 9;
        loud.worthReporting = true;
        loud.strength = 1.0;
        loud.agreement = Topics.Agreement.ALL_WEAK;

        Themes.Result mixed = Themes.extract(fx.frame, fx.gestalt, new ArrayList<>(),
            java.util.Arrays.asList(quiet, loud));
        ok("a topic below the witness floor is still a candidate",
            countFrom(mixed, "L6") == 2);
        ok("but only the one above it is a witness ("
            + componentsFor(mixed, "unconsolidated") + ")",
            componentsFor(mixed, "unconsolidated") == 1);
        ok("and the quiet one's own house theme is not a witness either ("
            + componentsFor(mixed, Themes.TOPIC_THEME[2]) + ")",
            componentsFor(mixed, Themes.TOPIC_THEME[2]) == 0);
        ok("and two of them are two separate witnesses, since they share nothing",
            componentsFor(bareResult, "unconsolidated") == 2);
    }

    // ---------------------------------------------------------------- Part C

    /**
     * Two topics resting on the same body are one witness, not two.
     *
     * <b>The assertion the whole design turns on.</b> A topic's standing is built from bodies that
     * have already spoken, so wiring L6 in naively adds a second vote for every placement it
     * touches - and the signature threshold is three, so inflating counts is the difference
     * between a reading that names a chart's spine and one that names everything.
     *
     * Checked two ways: directly, that no signature holds two components sharing a body, and
     * specifically, that two topics which share a ruler land in one component.
     */
    private static void independence() {
        for (int i = 0; i < CHARTS.length; i++) {
            Fixture fx = fixture(i);
            Themes.Result with = Themes.extract(fx.frame, fx.gestalt, fx.ranked, fx.topics);

            // <b>Components are disjoint by construction, so asserting it proves nothing.</b>
            // The first version of this part checked exactly that and a mutation that stripped a
            // topic's provenance walked straight through it. What CAN fail is the count: a theme
            // may not gain a witness from a topic that rests on bodies already speaking for it.
            java.util.Map<String, Integer> before = witnessCounts(
                Themes.extract(fx.frame, fx.gestalt, fx.ranked));
            for (Themes.Signature s : with.signatures) {
                Integer was = before.get(s.theme);
                if (was == null) {
                    continue;
                }
                int gained = s.witnessCount - was;
                int disjointTopics = 0;
                for (List<Themes.Candidate> component : s.components) {
                    boolean allL6 = true;
                    for (Themes.Candidate c : component) {
                        allL6 &= "L6".equals(c.source);
                    }
                    if (allL6) {
                        disjointTopics++;
                    }
                }
                ok("chart " + i + ": \"" + s.theme + "\" gained " + gained
                    + " witness(es), and " + disjointTopics
                    + " topic component(s) stand alone", gained <= disjointTopics);
            }
        }

        // And the specific case, named rather than left to chance.
        Fixture fx = fixture(0);
        Topics.Topic a = null;
        Topics.Topic b = null;
        for (Topics.Topic t : fx.topics) {
            if (!t.worthReporting || t.ruler == null) {
                continue;
            }
            for (Topics.Topic u : fx.topics) {
                if (u != t && u.worthReporting && u.ruler != null
                        && u.ruler.body.equals(t.ruler.body)) {
                    a = t;
                    b = u;
                    break;
                }
            }
            if (a != null) {
                break;
            }
        }
        ok("the chart has two reported topics sharing a ruler"
            + (a == null ? "" : " (" + a.house + " and " + b.house + ", "
                + a.ruler.body + ")"), a != null);
        if (a == null) {
            return;
        }
        Themes.Result with = Themes.extract(fx.frame, fx.gestalt, fx.ranked, fx.topics);
        Themes.Candidate ca = candidateFor(with, a);
        Themes.Candidate cb = candidateFor(with, b);
        ok("both are candidates", ca != null && cb != null);
        if (ca == null || cb == null) {
            return;
        }
        Set<String> shared = new LinkedHashSet<>(ca.provenance);
        shared.retainAll(cb.provenance);
        ok("and their provenance overlaps, so the rule can see them as one: " + shared,
            !shared.isEmpty());

        constructed();
    }

    /**
     * Two topics resting on one body, built rather than found.
     *
     * <b>The decisive case, and no real chart is obliged to contain it.</b> Part C above reports
     * what three charts happen to do; this states the rule. Both topics carry the same agreement,
     * so both emit "consolidated", and they share their only body - so the theme must have ONE
     * witness. Strip the provenance and it has two, which at a threshold of three is the
     * difference between a reading that names a chart's spine and one that names everything.
     */
    private static void constructed() {
        Fixture fx = fixture(0);
        BodyScore.Vector shared = fx.ranked.get(0);

        Topics.Topic one = new Topics.Topic();
        one.house = 2;
        one.worthReporting = true;
        one.agreement = Topics.Agreement.QUIET_COMPETENCE;
        one.strength = 1.0;
        one.ruler = shared;
        one.rulerName = shared.body;

        Topics.Topic two = new Topics.Topic();
        two.house = 11;
        two.worthReporting = true;
        two.agreement = Topics.Agreement.QUIET_COMPETENCE;
        two.strength = 1.0;
        two.ruler = shared;
        two.rulerName = shared.body;

        Themes.Result r = Themes.extract(fx.frame, fx.gestalt, new ArrayList<>(),
            java.util.Arrays.asList(one, two));
        int witnesses = 0;
        for (Themes.Signature s : r.signatures) {
            if ("consolidated".equals(s.theme)) {
                witnesses = s.witnessCount;
            }
        }
        int candidates = countFrom(r, "L6");
        ok("two topics on one body are two candidates (" + candidates + ")", candidates == 2);
        ok("both say consolidated",
            themesOfCandidates(r).contains("consolidated"));
        ok("and they are not two witnesses (" + witnesses + ")", witnesses < 2);

        // And two topics on DIFFERENT bodies are two, or the rule above would be satisfied by a
        // method that simply never counts a topic at all.
        Topics.Topic other = new Topics.Topic();
        other.house = 11;
        other.worthReporting = true;
        other.agreement = Topics.Agreement.QUIET_COMPETENCE;
        other.strength = 1.0;
        other.ruler = fx.ranked.get(1);
        other.rulerName = fx.ranked.get(1).body;

        Themes.Result apart = Themes.extract(fx.frame, fx.gestalt, new ArrayList<>(),
            java.util.Arrays.asList(one, other));
        int split = 0;
        for (Themes.Signature s : apart.signatures) {
            if ("consolidated".equals(s.theme)) {
                split = s.witnessCount;
            }
        }
        java.util.Map<String, Integer> counts = witnessCounts(apart);
        ok("two topics on different bodies are counted apart ("
            + counts.getOrDefault("consolidated", 0) + " components)",
            componentsFor(apart, "consolidated") == 2);
        ok("where the shared pair had one (" + componentsFor(r, "consolidated") + ")",
            componentsFor(r, "consolidated") == 1);
        ok("neither reaches the signature threshold of " + Themes.signatureThreshold
            + " on two topics alone (" + split + ")", split < Themes.signatureThreshold);
    }

    // ---------------------------------------------------------------- Part D

    /**
     * Every word in a contradiction pair is emitted somewhere.
     *
     * <b>Over three charts, because one chart cannot exercise a vocabulary.</b> A word that never
     * appears makes its pair a contradiction that can never fire - and the app has shipped four
     * rules like that this month under other names.
     */
    private static void vocabulary() {
        Set<String> emitted = new LinkedHashSet<>();
        for (int i = 0; i < CHARTS.length; i++) {
            Fixture fx = fixture(i);
            Themes.Result r = Themes.extract(fx.frame, fx.gestalt, fx.ranked, fx.topics);
            for (Themes.Candidate c : r.candidates) {
                emitted.addAll(c.themes);
            }
        }
        ok("the three charts between them emit a vocabulary (" + emitted.size() + " words)",
            emitted.size() > 12);
        for (String[] pair : Themes.OPPOSITES) {
            for (String word : pair) {
                ok("\"" + word + "\" is emitted by something", emitted.contains(word));
            }
        }
        // The pair F14 adds, named separately so a failure says which row it belongs to.
        ok("consolidated and unconsolidated are a declared pair", declared("consolidated",
            "unconsolidated"));
    }

    private static boolean declared(String a, String b) {
        for (String[] pair : Themes.OPPOSITES) {
            if ((pair[0].equals(a) && pair[1].equals(b))
                || (pair[0].equals(b) && pair[1].equals(a))) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- Part E

    /** The other direction: a topic naming the signatures its bodies helped make. */
    private static void back() {
        Fixture fx = fixture(0);
        Themes.Result with = Themes.extract(fx.frame, fx.gestalt, fx.ranked, fx.topics);

        ok("null inputs answer with an empty list, not null",
            Topics.themesOf(null, with) != null && Topics.themesOf(null, with).isEmpty());
        ok("and so does a null result",
            Topics.themesOf(fx.topics.get(0), null).isEmpty());

        Set<String> signatureNames = new LinkedHashSet<>();
        for (Themes.Signature s : with.signatures) {
            signatureNames.add(s.theme);
        }
        ok("the chart has signatures to be a witness to (" + signatureNames.size() + ")",
            !signatureNames.isEmpty());

        int answered = 0;
        boolean allReal = true;
        for (Topics.Topic t : fx.topics) {
            List<String> mine = Topics.themesOf(t, with);
            if (!mine.isEmpty()) {
                answered++;
            }
            for (String theme : mine) {
                allReal &= signatureNames.contains(theme);
            }
            ok("house " + t.house + " names "
                + mine.size() + " signature(s) and no duplicates",
                new LinkedHashSet<>(mine).size() == mine.size());
        }
        ok("at least one topic is a witness to something (" + answered + ")", answered > 0);
        ok("and every theme it names is a signature the chart actually has", allReal);

        // A topic standing on no bodies at all cannot be a witness to anything - the assertion
        // that stops this method answering "yes" for every house.
        Topics.Topic empty = new Topics.Topic();
        empty.house = 6;
        ok("a topic with no bodies is a witness to nothing",
            Topics.themesOf(empty, with).isEmpty());
    }

    // ---------------------------------------------------------------- plumbing

    /** The L6 candidate for a topic, found by the house theme it carries. */
    private static Themes.Candidate candidateFor(Themes.Result r, Topics.Topic t) {
        for (Themes.Candidate c : r.candidates) {
            if ("L6".equals(c.source) && c.themes.contains(Themes.TOPIC_THEME[t.house - 1])) {
                return c;
            }
        }
        return null;
    }

    /** Each theme's witness count, for comparing one extraction against another. */
    private static java.util.Map<String, Integer> witnessCounts(Themes.Result r) {
        java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
        for (Themes.Signature s : r.signatures) {
            out.put(s.theme, s.witnessCount);
        }
        return out;
    }

    /** How many independent components a theme was grouped into, or 0 if it is no signature. */
    private static int componentsFor(Themes.Result r, String theme) {
        for (Themes.Signature s : r.signatures) {
            if (theme.equals(s.theme)) {
                return s.components.size();
            }
        }
        return 0;
    }

    private static Set<String> themesOfCandidates(Themes.Result r) {
        Set<String> out = new LinkedHashSet<>();
        for (Themes.Candidate c : r.candidates) {
            out.addAll(c.themes);
        }
        return out;
    }

    private static int countFrom(Themes.Result r, String source) {
        int n = 0;
        for (Themes.Candidate c : r.candidates) {
            if (source.equals(c.source)) {
                n++;
            }
        }
        return n;
    }

    private static final class Fixture {
        ChartFrame frame;
        Gestalt.Result gestalt;
        List<BodyScore.Vector> ranked;
        List<Topics.Topic> topics;
    }

    private static Fixture fixture(int i) {
        double[] c = CHARTS[i];
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate((int) c[0], (int) c[1], (int) c[2], c[3]).getJulDay();
        Fixture fx = new Fixture();
        fx.frame = ChartFrame.compute(sw, jd, c[4], c[5], 'P', false, 0.0);
        fx.gestalt = Gestalt.compute(fx.frame);
        fx.ranked = BodyScore.rank(fx.frame, fx.gestalt);
        fx.topics = Topics.analyse(fx.frame, fx.ranked);
        return fx;
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
