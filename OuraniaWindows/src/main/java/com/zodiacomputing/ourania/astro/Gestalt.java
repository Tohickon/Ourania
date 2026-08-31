package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L5: the chart as a shape, before any single placement is read.
 *
 * Everything here is counting and bucketing over data L1 through L3 already produced.
 * The discipline of this layer is cutting, not counting - the counting is trivial and
 * the temptation is to report all of it. Hence the thresholds: a hemisphere split is
 * only reported past 70/30, because reporting a 55/45 split teaches the reader to
 * distrust the whole section.
 */
public final class Gestalt {

    /**
     * The elements and modalities, pointed at {@link Zodiac} rather than declared again.
     *
     * These were two literal arrays here and one more pair in Zodiac, which is the shape
     * every drift in this project has started from. `AspectPatterns` needs the modality of a
     * sign to say whether a T-square is cardinal, and a third copy for it would have been
     * the point where they began to disagree.
     */
    public static final String[] ELEMENTS = Zodiac.ELEMENTS;
    public static final String[] MODALITIES = Zodiac.MODALITIES;

    /** Houses 1-3, 4-6, 7-9, 10-12. Quadrant = (house - 1) / 3. */
    public static final String[] QUADRANTS = {"first", "second", "third", "fourth"};

    /**
     * One clause per field reading, kept beside the definition rather than in the headline
     * writer. A distribution and the sentence describing it drifting apart is the same defect
     * the aspect grid had when its glyph table and its aspect table disagreed.
     */
    private static final Map<String, String> QUADRANT_MEANING = new LinkedHashMap<>();
    private static final Map<String, String> HOUSE_MODE_MEANING = new LinkedHashMap<>();
    private static final Map<String, String> TRINITY_MEANING = new LinkedHashMap<>();
    static {
        QUADRANT_MEANING.put("first", "self-definition, before the world has had much say");
        QUADRANT_MEANING.put("second", "the immediate surroundings, daily work and the body");
        QUADRANT_MEANING.put("third", "other people, and what becomes visible only through them");
        QUADRANT_MEANING.put("fourth", "the collective, the public, and what outlasts you");

        HOUSE_MODE_MEANING.put("angular", "action and visible consequence, life lived at the front");
        HOUSE_MODE_MEANING.put("succedent", "consolidation, holding and building on what is already there");
        HOUSE_MODE_MEANING.put("cadent", "adjustment, learning and preparation rather than the event itself");

        TRINITY_MEANING.put("life", "vitality, creative expression and belief");
        TRINITY_MEANING.put("substance", "money, labour, the body and the work of making things hold");
        TRINITY_MEANING.put("association", "conversation, partnership and the networks you belong to");
        TRINITY_MEANING.put("endings", "depth, shared resources, and what has to be let go of");
    }
    /** Angular 1/4/7/10, succedent 2/5/8/11, cadent 3/6/9/12. Mode = (house - 1) % 3. */
    public static final String[] HOUSE_MODES = {"angular", "succedent", "cadent"};
    /**
     * The four thematic triplicities of houses, sometimes called the Triangles of Life.
     * Trinity = (house - 1) % 4, which lands 1/5/9 on life, 2/6/10 on substance,
     * 3/7/11 on association and 4/8/12 on endings.
     */
    public static final String[] TRINITIES = {"life", "substance", "association", "endings"};

    /**
     * Weights for element and modality counting. The outers are downweighted because
     * they are generational - everyone born in a several-year window shares them, so
     * they carry almost no individuating information about elemental balance
     * specifically. They matter elsewhere: by house, by aspect, by angularity.
     * These are a choice and belong in the settings block.
     */
    /**
     * The background-wash scoring table: David's 14-point system, adopted 2026-08-21 from
     * [[master-chart-synthesis-checklist]] Phase 3.
     *
     * Fourteen points, not fourteen bodies' worth of weight: Sun, Moon and Ascendant carry 3
     * each, and MC, both nodes, the three personal planets and the five social/outer planets
     * carry 1 each.
     *
     * The change from the previous table is not cosmetic. **The Ascendant now counts at all**
     * - it was absent, which meant the rising sign contributed nothing to a chart's element
     * or modality balance, and the Ascendant is a third of the Big Three. Mercury, Venus and
     * Mars came down from 1.5 to 1.0 and the outer three came up from 0.5 to 1.0, so the
     * outer planets now speak as loudly as the personal ones. Chiron is dropped: it is not in
     * the 14-point system, and leaving it at a quarter point was a weight nobody had chosen.
     *
     * **This table is for elements, modalities and hemispheres only.** It used to serve the
     * shape pass and the stellium pass as well, through a `>= 0.5` threshold and a bare
     * `containsKey`, which meant a weighting decision silently redefined what a Jones pattern
     * and a stellium were made of. Those two now have their own membership lists below.
     */
    private static final Map<String, Double> WEIGHTS = new LinkedHashMap<>();
    static {
        WEIGHTS.put("Sun", 3.0);
        WEIGHTS.put("Moon", 3.0);
        WEIGHTS.put("Ascendant", 3.0);
        WEIGHTS.put("MC", 1.0);
        WEIGHTS.put("North Node", 1.0);
        WEIGHTS.put("South Node", 1.0);
        WEIGHTS.put("Mercury", 1.0);
        WEIGHTS.put("Venus", 1.0);
        WEIGHTS.put("Mars", 1.0);
        WEIGHTS.put("Jupiter", 1.0);
        WEIGHTS.put("Saturn", 1.0);
        WEIGHTS.put("Uranus", 1.0);
        WEIGHTS.put("Neptune", 1.0);
        WEIGHTS.put("Pluto", 1.0);
    }
    /**
     * The chart ruler's bonus, on top of whatever WEIGHTS gives its body.
     *
     * <b>There were once WEIGHT_ASC and WEIGHT_MC constants beside this, and they were a
     * double count.</b> The Ascendant and the MC are registry points, so the body loop in
     * balances() already picked them up out of WEIGHTS - and then balances() added them a
     * second time from f.asc and f.mc. Measured on a 1990 chart: the element weights totalled
     * 27.0 where the 14-point table plus this bonus allows 22.0, the excess being exactly the
     * Ascendant's extra 3.0 and the MC's extra 2.0. The Ascendant counted 6 and the MC 3.
     *
     * The comment above WEIGHTS says the Ascendant "now counts at all - it was absent", which
     * is how it happened: it was added to the table on 2026-08-21 without noticing the
     * standalone add() that had been covering it. <b>WEIGHTS is now the only source.</b>
     */
    private static final double WEIGHT_ASC_RULER = 2.0;

    /** Report a hemisphere split only past this share. Below it, the split is noise. */
    public static double hemisphereThreshold = 0.70;
    /** An element or modality holding this share of the weight counts as dominant. */
    public static double dominanceThreshold = 0.40;
    /** Below this weight an element counts as missing. */
    public static double missingThreshold = 1.0;

    public enum Shape { BUNDLE, BOWL, BUCKET, LOCOMOTIVE, SEESAW, SPLASH, SPLAY }

    public static final class Result {
        public boolean diurnal;
        public String chartRuler;
        public String sectLight;
        public String outOfSectMalefic;

        public final Map<String, Double> elements = new LinkedHashMap<>();
        public final Map<String, Double> modalities = new LinkedHashMap<>();
        public final List<String> missingElements = new ArrayList<>();
        public final List<String> dominantElements = new ArrayList<>();
        public final List<String> missingModalities = new ArrayList<>();
        public final List<String> dominantModalities = new ArrayList<>();

        /**
         * Which bodies sit in each element and modality, as membership rather than weight.
         * A singleton is a COUNT of one, not a small share of the weight - the Sun alone in
         * fire is a singleton at weight 3.0, and two outer planets in fire are not at 2.0.
         */
        public final Map<String, List<String>> elementMembers = new LinkedHashMap<>();
        public final Map<String, List<String>> modalityMembers = new LinkedHashMap<>();

        /**
         * The sign carrying the chart's dominant element AND dominant modality.
         * Null when either is tied, because a signature is a single claim or it is nothing.
         */
        public String signatureSign;
        /** Set when the signature could not be named because the top element or modality tied. */
        public boolean signatureTied;

        /** "Mars, the only body in fire" and the like. Element, modality and hemisphere. */
        public final List<String> singletons = new ArrayList<>();

        public double aboveHorizon, belowHorizon, eastern, western;
        public String hemisphereEmphasis;      // null when below threshold
        /** Set when exactly one body holds a hemisphere on its own. */
        public String hemisphereSingleton;

        /** House-field distributions, all weighted like the balances and all angle-free. */
        public final Map<String, Double> quadrants = new LinkedHashMap<>();
        public final Map<String, Double> houseModes = new LinkedHashMap<>();
        public final Map<String, Double> trinities = new LinkedHashMap<>();
        public String quadrantEmphasis;        // null when below threshold
        public String houseModeEmphasis;
        public String trinityEmphasis;

        public Shape shape;
        public String shapeHandle;             // bucket handle or bowl/locomotive leader

        public final List<String> stelliums = new ArrayList<>();
        /**
         * The same stelliums as membership rather than display text, sign to bodies.
         * BodyScore needs to ask "is this body in one", and picking that out of
         * "aries: Moon, Saturn, Neptune" would couple the scoring to the wording.
         */
        public final Map<String, List<String>> stelliumMembers = new LinkedHashMap<>();
        public final List<String> outOfBounds = new ArrayList<>();
        public final List<String> retrograde = new ArrayList<>();
        public final List<String> finalDispositors = new ArrayList<>();
        public final List<String> dispositorLoops = new ArrayList<>();
        /**
         * The same loops as membership, in TRAVERSAL order - each member is disposed by
         * the next, and the last by the first. Only the dedupe key is sorted; see the
         * note in dispositors(). Consumers may rely on the order being real.
         */
        public final List<List<String>> dispositorLoopMembers = new ArrayList<>();
        /** Set only when one final dispositor terminates every chain in the chart. */
        public String soleDispositor;

        public String moonPhase;

        public final List<AspectPatterns.Pattern> aspectPatterns = new ArrayList<>();
        public final List<String> lightTransfers = new ArrayList<>();

        /**
         * Where each of those patterns discharges - framework item 6.3.
         *
         * Computed from the same hit list and the same pattern list, in {@link TensionRelease},
         * so the release cannot describe a configuration the panel above it is not showing.
         *
         * <b>Deliberately not added to {@link #headlines}.</b> A chart carries 11.8 of these
         * on average and the headlines are what PlainSnapshot prints; twelve release lines
         * would swamp a three-paragraph snapshot. The full interpretation panel, which
         * already lists every pattern, is where they surface.
         */
        public final List<TensionRelease.Release> releases = new ArrayList<>();

        /** The opening lines of a reading, already cut and ordered. */
        public final List<String> headlines = new ArrayList<>();
    }

    /**
     * This body's weight in the 14-point table, or 0.0 if it is not in it.
     *
     * Exposed so a check can reconstruct the expected balance total from the same table the
     * balance is built from, rather than restating the numbers - a copy of the table in a
     * check would agree with itself while both drifted from whatever the app actually does.
     */
    public static double weightOf(String bodyName) {
        return WEIGHTS.getOrDefault(bodyName, 0.0);
    }

    /**
     * The explanatory clause for a quadrant, house mode or trinity, or null.
     *
     * Exposed so a check can assert that every bucket the code can report has something to
     * say about it. The quincunx shipped for months as a live aspect with an empty glyph
     * because the model knew about it and the view did not; a fifth trinity added here
     * without a clause would read as "Weighted toward the houses of x - null."
     */
    public static String fieldMeaning(String key) {
        if (QUADRANT_MEANING.containsKey(key)) {
            return QUADRANT_MEANING.get(key);
        }
        if (HOUSE_MODE_MEANING.containsKey(key)) {
            return HOUSE_MODE_MEANING.get(key);
        }
        return TRINITY_MEANING.get(key);
    }

    private Gestalt() { }

    public static Result compute(ChartFrame f) {
        Result r = new Result();
        r.diurnal = f.diurnal;
        r.chartRuler = Dignity.domicileRulerOf(Zodiac.signIndex(f.asc));
        r.sectLight = Sect.sectLight(f.diurnal);
        r.outOfSectMalefic = Sect.outOfSectMalefic(f.diurnal);
        r.moonPhase = f.phaseName;

        balances(f, r);
        signature(r);
        hemispheres(f, r);
        houseFields(f, r);
        singletons(r);
        shape(f, r);
        clusters(f, r);
        dispositors(f, r);
        
        List<Aspects.Hit> hits = Aspects.betweenBodies(f);
        // The frame overload, so patterns arrive classified by modality and element.
        r.aspectPatterns.addAll(AspectPatterns.findPatterns(f, hits));
        r.lightTransfers.addAll(TransferOfLight.findTransfers(f, hits));
        r.releases.addAll(TensionRelease.find(f, hits, r.aspectPatterns));

        headlines(r);
        return r;
    }

    // ------------------------------------------------------------------ balances

    private static void balances(ChartFrame f, Result r) {
        for (String e : ELEMENTS) {
            r.elements.put(e, 0.0);
        }
        for (String m : MODALITIES) {
            r.modalities.put(m, 0.0);
        }

        for (String e : ELEMENTS) {
            r.elementMembers.put(e, new ArrayList<>());
        }
        for (String m : MODALITIES) {
            r.modalityMembers.put(m, new ArrayList<>());
        }

        for (ChartFrame.Body b : f.bodies) {
            if (b.ok && WEIGHTS.containsKey(b.name)) {
                double w = WEIGHTS.get(b.name);
                if (b.name.equals(r.chartRuler)) {
                    w += WEIGHT_ASC_RULER;
                }
                add(r, b.lon, w);
                int s = Zodiac.signIndex(b.lon);
                r.elementMembers.get(ELEMENTS[s % 4]).add(b.name);
                r.modalityMembers.get(MODALITIES[s % 3]).add(b.name);
            }
        }
        // No add() for f.asc / f.mc here. Both are registry points and were already counted
        // by the loop above; adding them again was the double count described on
        // WEIGHT_ASC_RULER.

        double totalE = r.elements.values().stream().mapToDouble(Double::doubleValue).sum();
        for (String e : ELEMENTS) {
            double v = r.elements.get(e);
            if (v < missingThreshold) {
                r.missingElements.add(e);
            } else if (v / totalE >= dominanceThreshold) {
                r.dominantElements.add(e);
            }
        }
        double totalM = r.modalities.values().stream().mapToDouble(Double::doubleValue).sum();
        for (String m : MODALITIES) {
            double v = r.modalities.get(m);
            if (v < missingThreshold) {
                r.missingModalities.add(m);
            } else if (v / totalM >= dominanceThreshold) {
                r.dominantModalities.add(m);
            }
        }
    }

    private static void add(Result r, double lon, double w) {
        int s = Zodiac.signIndex(lon);
        r.elements.merge(ELEMENTS[s % 4], w, Double::sum);
        r.modalities.merge(MODALITIES[s % 3], w, Double::sum);
    }

    // ------------------------------------------------------------------ signature sign

    /**
     * The sign holding both the dominant element and the dominant modality.
     *
     * Element and modality repeat around the zodiac with periods 4 and 3, which are coprime,
     * so every one of the twelve (element, modality) pairs names exactly one sign and the
     * lookup below cannot fail to find one. Fire + cardinal is Aries and nothing else.
     *
     * <b>A tie leaves this null rather than picking.</b> The signature is a single summarising
     * claim about a chart; "fire or air, cardinal" is not one, and choosing between equal
     * weights by map order would make the answer depend on the order ELEMENTS happens to be
     * declared in.
     */
    private static void signature(Result r) {
        int e = uniqueTopIndex(r.elements, ELEMENTS);
        int m = uniqueTopIndex(r.modalities, MODALITIES);
        if (e < 0 || m < 0) {
            r.signatureTied = true;
            r.signatureSign = null;
            return;
        }
        for (int s = 0; s < 12; s++) {
            if (s % 4 == e && s % 3 == m) {
                r.signatureSign = Zodiac.SIGNS[s];
                return;
            }
        }
    }

    /** Index of the strictly largest entry, or -1 when the top is shared. */
    private static int uniqueTopIndex(Map<String, Double> weights, String[] keys) {
        int best = -1;
        double bestVal = Double.NEGATIVE_INFINITY;
        boolean tied = false;
        for (int i = 0; i < keys.length; i++) {
            double v = weights.getOrDefault(keys[i], 0.0);
            if (v > bestVal) {
                bestVal = v;
                best = i;
                tied = false;
            } else if (v == bestVal) {
                tied = true;
            }
        }
        return tied ? -1 : best;
    }

    // ------------------------------------------------------------------ singletons

    /**
     * A body that is the ONLY one of its element, modality or hemisphere.
     *
     * Counted, never weighted. The whole point of a singleton is that one body is carrying a
     * whole category alone, and that is a fact about population rather than about how heavily
     * the table happens to score it.
     *
     * Hemisphere singletons are counted from the same angle-free population the hemisphere
     * split uses, for the reason given in hemispheres(): asking which side of the horizon the
     * Ascendant falls on is asking a question the chart answered before it was cast.
     */
    private static void singletons(Result r) {
        for (String e : ELEMENTS) {
            List<String> in = r.elementMembers.get(e);
            if (in != null && in.size() == 1) {
                r.singletons.add(in.get(0) + ", the only body in " + e);
            }
        }
        for (String m : MODALITIES) {
            List<String> in = r.modalityMembers.get(m);
            if (in != null && in.size() == 1) {
                r.singletons.add(in.get(0) + ", the only " + m + " body");
            }
        }
        if (r.hemisphereSingleton != null) {
            r.singletons.add(r.hemisphereSingleton);
        }
    }

    // ------------------------------------------------------------------ hemispheres

    /**
     * Houses 7 to 12 are above the horizon and occupy the arc more than 180 degrees
     * counterclockwise from the Ascendant - the same test sect uses. Houses 10 to 3 are
     * eastern and occupy the 180 degrees counterclockwise from the MC.
     */
    /** The four angles, by the names the registry uses. */
    private static boolean isAngleName(String name) {
        return "Ascendant".equals(name) || "Descendant".equals(name)
            || "MC".equals(name) || "IC".equals(name) || "Midheaven".equals(name);
    }

    private static void hemispheres(ChartFrame f, Result r) {
        List<String> above = new ArrayList<>();
        List<String> below = new ArrayList<>();
        List<String> east = new ArrayList<>();
        List<String> west = new ArrayList<>();
        for (ChartFrame.Body b : f.bodies) {
            // Weighted as elsewhere, but the ANGLES are excluded here, and only here.
            //
            // The Ascendant is the eastern horizon and the MC is the top of the chart, so
            // asking which hemisphere they fall in is asking a question whose answer was
            // fixed before the chart was cast. Measured over 400 charts: the Ascendant came
            // out "below horizon + eastern" 400 times out of 400, and the MC "above horizon
            // + eastern" 400 out of 400. Counting them adds the same constant to every chart
            // - +3 below and +3 eastern from the Ascendant, +1 above and +1 eastern from the
            // MC - which does not distinguish one chart from another.
            //
            // This is the same shape as the MC ranking defect fixed on 2026-08-21: an angle
            // scored against the frame it defines. They still carry their full weight in the
            // element and modality balances, where the rising sign has a real element and the
            // question is not circular.
            if (!b.ok || !WEIGHTS.containsKey(b.name) || isAngleName(b.name)) {
                continue;
            }
            double w = WEIGHTS.get(b.name);
            if (Zodiac.normalise(b.lon - f.asc) > 180.0) {
                r.aboveHorizon += w;
                above.add(b.name);
            } else {
                r.belowHorizon += w;
                below.add(b.name);
            }
            if (Zodiac.normalise(b.lon - f.mc) < 180.0) {
                r.eastern += w;
                east.add(b.name);
            } else {
                r.western += w;
                west.add(b.name);
            }
        }

        r.hemisphereSingleton = loneHolder(above, "above the horizon");
        if (r.hemisphereSingleton == null) {
            r.hemisphereSingleton = loneHolder(below, "below the horizon");
        }
        if (r.hemisphereSingleton == null) {
            r.hemisphereSingleton = loneHolder(east, "in the eastern half");
        }
        if (r.hemisphereSingleton == null) {
            r.hemisphereSingleton = loneHolder(west, "in the western half");
        }

        double vTotal = r.aboveHorizon + r.belowHorizon;
        double hTotal = r.eastern + r.western;
        List<String> parts = new ArrayList<>();
        if (r.aboveHorizon / vTotal >= hemisphereThreshold) {
            parts.add("strongly above the horizon");
        } else if (r.belowHorizon / vTotal >= hemisphereThreshold) {
            parts.add("strongly below the horizon");
        }
        if (r.eastern / hTotal >= hemisphereThreshold) {
            parts.add("strongly eastern");
        } else if (r.western / hTotal >= hemisphereThreshold) {
            parts.add("strongly western");
        }
        r.hemisphereEmphasis = parts.isEmpty() ? null : String.join(", ", parts);
    }

    /** Names the sole occupant of a group, or null when the group does not have exactly one. */
    private static String loneHolder(List<String> group, String where) {
        return group.size() == 1 ? group.get(0) + ", the only body " + where : null;
    }

    // ------------------------------------------------------------------ house fields

    /**
     * Three distributions over the houses: quadrant, angularity class, and thematic trinity.
     *
     * All three are the same walk with different divisors, so they share one pass. Weighted
     * from the same table as the balances, and <b>angle-free for the same reason the
     * hemisphere split is</b>: the Ascendant is the first house cusp and the MC the tenth by
     * construction, so counting them tells you about the house system rather than the chart.
     *
     * A degenerate house system returns house 0 from Zodiac.houseOf, and those bodies are
     * skipped rather than filed under a quadrant that does not exist - ChartFrame already
     * refuses to invent cusps in that case and this must not undo it.
     */
    private static void houseFields(ChartFrame f, Result r) {
        for (String q : QUADRANTS) {
            r.quadrants.put(q, 0.0);
        }
        for (String m : HOUSE_MODES) {
            r.houseModes.put(m, 0.0);
        }
        for (String t : TRINITIES) {
            r.trinities.put(t, 0.0);
        }

        for (ChartFrame.Body b : f.bodies) {
            if (!b.ok || !WEIGHTS.containsKey(b.name) || isAngleName(b.name)) {
                continue;
            }
            int house = Zodiac.houseOf(b.lon, f.cusps);
            if (house < 1 || house > 12) {
                continue;
            }
            double w = WEIGHTS.get(b.name);
            r.quadrants.merge(QUADRANTS[(house - 1) / 3], w, Double::sum);
            r.houseModes.merge(HOUSE_MODES[(house - 1) % 3], w, Double::sum);
            r.trinities.merge(TRINITIES[(house - 1) % 4], w, Double::sum);
        }

        r.quadrantEmphasis = emphasis(r.quadrants, QUADRANTS);
        r.houseModeEmphasis = emphasis(r.houseModes, HOUSE_MODES);
        r.trinityEmphasis = emphasis(r.trinities, TRINITIES);
    }

    /**
     * The one key holding at least dominanceThreshold of the total, or null.
     *
     * Reuses dominanceThreshold rather than inventing a fourth knob. It is the same question
     * the element and modality balances ask - "does one bucket hold enough of this to be worth
     * saying out loud" - and modalities are already a three-way split judged at 0.40, so a
     * three-way house-mode split judged the same way is consistent rather than coincidental.
     */
    private static String emphasis(Map<String, Double> weights, String[] keys) {
        double total = 0.0;
        for (double v : weights.values()) {
            total += v;
        }
        if (total <= 0.0) {
            return null;
        }
        int top = uniqueTopIndex(weights, keys);
        if (top < 0) {
            return null;
        }
        return weights.get(keys[top]) / total >= dominanceThreshold ? keys[top] : null;
    }

    // ------------------------------------------------------------------ shape

    /**
     * Every Jones pattern is decided by the sorted gaps between consecutive bodies around
     * the circle, so compute those once and classify. Uses the ten Sun-to-Pluto bodies.
     */
    /**
     * Smallest empty arc that still leaves a BOWL for a bucket's handle to hang off.
     *
     * A bucket is a bowl plus a handle, so the tenanted part has to be bowl-sized. Wanda
     * Sellar, Chart Shapes: the bucket has "nine planets in one hemisphere or in a 180 deg
     * of arc though this can be stretched to 190", with the handle around 90 deg from the
     * boundary planets. 190 degrees tenanted is 170 empty, hence this figure.
     *
     * It was 120.0, which is the LOCOMOTIVE threshold - so any chart spread over as much as
     * 240 degrees was called a bucket the moment one body happened to sit alone. Measured
     * over 600 charts, tightening it to 170 reclassifies 27 of them (4.5%), every one
     * BUCKET -> LOCOMOTIVE, which is what they were.
     *
     * The other two rules Sellar states - internal gaps under 60, and a see-saw needing
     * three planets a side - were measured at 21.0% and 21.5% and deliberately NOT taken:
     * she words those as ideals ("ideally", "the most extreme being"), and hard-coding an
     * ideal is what makes a typology written for the eye behave badly as a classifier.
     * [[chart-shape-rules]]
     */
    private static final double BUCKET_MIN_EMPTY_ARC = 170.0;

    /**
     * The bodies a Jones pattern is drawn from: the ten classical planets, and nothing else.
     *
     * Explicit rather than "everything WEIGHTS scores at 0.5 or better", which is how it used
     * to be selected. Under that rule, adopting a new weighting table would have quietly put
     * the Ascendant, the MC and both nodes into every chart's shape - and a Bowl computed over
     * fourteen points is not the Bowl Marc Edmund Jones described or the one
     * [[chart-shape-rules]] measures against.
     */
    private static final Set<String> SHAPE_BODIES = new LinkedHashSet<>(Arrays.asList(
        "Sun", "Moon", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"));

    /**
     * The bodies that can form a stellium.
     *
     * The ten planets plus Chiron, with the nodes and angles excluded - the checklist's own
     * Phase 2 says "3+ planets (excluding the Lunar Nodes)", and a cluster owes its meaning to
     * bodies being there, which an angle is not.
     */
    private static final Set<String> CLUSTER_BODIES = new LinkedHashSet<>(Arrays.asList(
        "Sun", "Moon", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron"));

    private static void shape(ChartFrame f, Result r) {
        List<double[]> pts = new ArrayList<>();   // {longitude, index into names}
        List<String> names = new ArrayList<>();
        for (ChartFrame.Body b : f.bodies) {
            if (b.ok && SHAPE_BODIES.contains(b.name)) {
                pts.add(new double[] {Zodiac.normalise(b.lon), names.size()});
                names.add(b.name);
            }
        }
        if (pts.size() < 3) {
            r.shape = Shape.SPLAY;
            return;
        }
        pts.sort((a, b) -> Double.compare(a[0], b[0]));

        int n = pts.size();
        double[] gapAfter = new double[n];
        for (int i = 0; i < n; i++) {
            double next = pts.get((i + 1) % n)[0];
            gapAfter[i] = Zodiac.normalise(next - pts.get(i)[0]);
        }
        double[] sorted = gapAfter.clone();
        Arrays.sort(sorted);
        double g1 = sorted[n - 1];
        double g2 = sorted[n - 2];

        // A handle is a body with a large empty space on both sides.
        int handle = -1;
        for (int i = 0; i < n; i++) {
            double before = gapAfter[(i - 1 + n) % n];
            if (before >= 90.0 && gapAfter[i] >= 90.0) {
                handle = i;
                break;
            }
        }

        if (g1 >= 240.0) {
            r.shape = Shape.BUNDLE;
        } else if (handle >= 0 && g1 >= BUCKET_MIN_EMPTY_ARC) {
            r.shape = Shape.BUCKET;
            r.shapeHandle = names.get((int) pts.get(handle)[1]);
        } else if (g1 >= 180.0) {
            r.shape = Shape.BOWL;
            r.shapeHandle = leaderAfter(pts, names, gapAfter, n);
        } else if (g1 >= 120.0) {
            r.shape = Shape.LOCOMOTIVE;
            r.shapeHandle = leaderAfter(pts, names, gapAfter, n);
        } else if (g1 >= 60.0 && g2 >= 60.0) {
            r.shape = Shape.SEESAW;
        } else if (g1 < 60.0) {
            r.shape = Shape.SPLASH;
        } else {
            r.shape = Shape.SPLAY;
        }
    }

    /** The body immediately after the largest gap, in zodiacal order: the leading planet. */
    private static String leaderAfter(List<double[]> pts, List<String> names, double[] gapAfter, int n) {
        int widest = 0;
        for (int i = 1; i < n; i++) {
            if (gapAfter[i] > gapAfter[widest]) {
                widest = i;
            }
        }
        return names.get((int) pts.get((widest + 1) % n)[1]);
    }

    // ------------------------------------------------------------------ clusters

    private static void clusters(ChartFrame f, Result r) {
        Map<String, List<String>> bySign = new LinkedHashMap<>();
        for (ChartFrame.Body b : f.bodies) {
            if (!b.ok || !CLUSTER_BODIES.contains(b.name)) {
                continue;
            }
            bySign.computeIfAbsent(Zodiac.signName(b.lon), k -> new ArrayList<>()).add(b.name);
            if (b.outOfBounds) {
                r.outOfBounds.add(b.name);
            }
            if (b.retrograde) {
                r.retrograde.add(b.name);
            }
        }
        for (Map.Entry<String, List<String>> e : bySign.entrySet()) {
            if (e.getValue().size() >= 3) {
                r.stelliums.add(e.getKey() + ": " + String.join(", ", e.getValue()));
                r.stelliumMembers.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
    }

    // ------------------------------------------------------------------ dispositors

    /**
     * Follow each traditional planet to the domicile ruler of its sign, and repeat. Chains
     * terminate either in a planet in its own sign - a final dispositor - or in a loop.
     * Both are chart signatures and both feed prominence.
     */
    private static void dispositors(ChartFrame f, Result r) {
        Map<String, String> ruledBy = new LinkedHashMap<>();
        for (String p : Dignity.TRADITIONAL) {
            ChartFrame.Body b = findBody(f, p);
            if (b != null && b.ok) {
                ruledBy.put(p, Dignity.domicileRulerOf(Zodiac.signIndex(b.lon)));
            }
        }
        for (Map.Entry<String, String> e : ruledBy.entrySet()) {
            if (e.getKey().equals(e.getValue())) {
                r.finalDispositors.add(e.getKey());
            }
        }
        // "Disposes the whole chart" is a much stronger claim than "is in its own sign",
        // and only holds when every other planet's chain actually terminates there.
        for (String fd : r.finalDispositors) {
            boolean all = true;
            for (String p : ruledBy.keySet()) {
                String cur = p;
                List<String> seen = new ArrayList<>();
                while (cur != null && !seen.contains(cur) && !cur.equals(fd)) {
                    seen.add(cur);
                    cur = ruledBy.get(cur);
                }
                if (!fd.equals(cur)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                r.soleDispositor = fd;
                break;
            }
        }

        // Any cycle of length two or more that contains no final dispositor.
        java.util.Set<String> seenLoops = new java.util.LinkedHashSet<>();
        for (String start : ruledBy.keySet()) {
            List<String> path = new ArrayList<>();
            String cur = start;
            while (cur != null && !path.contains(cur)) {
                path.add(cur);
                cur = ruledBy.get(cur);
            }
            if (cur != null && !cur.equals(path.get(path.size() - 1))) {
                int at = path.indexOf(cur);
                List<String> loop = new ArrayList<>(path.subList(at, path.size()));
                if (loop.size() >= 2) {
                    // The sort here used to be applied to the loop ITSELF, which threw
                    // away the traversal order and left the display string joined with
                    // arrows that no longer meant anything - "Mars -> Mercury -> Moon"
                    // read as a chain while being merely alphabetical. Sort only the
                    // dedupe key: walking the same cycle from a different planet yields a
                    // rotation of one list, and sorting is what makes those compare equal.
                    List<String> canonical = new ArrayList<>(loop);
                    java.util.Collections.sort(canonical);
                    String key = String.join("|", canonical);
                    if (!seenLoops.contains(key)) {
                        seenLoops.add(key);
                        // Ring closed back to the start, so the arrows are unambiguous:
                        // this is a cycle, not a chain that happens to end somewhere.
                        r.dispositorLoops.add(String.join(" -> ", loop)
                            + " -> " + loop.get(0));
                        r.dispositorLoopMembers.add(new ArrayList<>(loop));
                    }
                }
            }
        }
    }

    private static ChartFrame.Body findBody(ChartFrame f, String name) {
        for (ChartFrame.Body b : f.bodies) {
            if (b.name.equals(name)) {
                return b;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ headlines

    /**
     * The opening of a reading, in the order L9 wants it: sect, then what is missing or
     * dominant, then the shape's handle, then hemisphere if past threshold, then the
     * chart ruler. Anything past this belongs to a later layer.
     */
    private static void headlines(Result r) {
        r.headlines.add(r.diurnal
            ? "A day chart: the Sun rules its sect, Jupiter does the most good, Mars the most harm."
            : "A night chart: the Moon rules its sect, Venus does the most good, Saturn the most harm.");

        if (!r.missingElements.isEmpty()) {
            r.headlines.add("No " + String.join(" and no ", r.missingElements)
                + " to speak of - a lack reads louder than an abundance.");
        }
        for (String e : r.dominantElements) {
            r.headlines.add("Heavily " + e + ".");
        }
        for (String m : r.dominantModalities) {
            r.headlines.add("Predominantly " + m + ".");
        }
        if (r.shapeHandle != null) {
            r.headlines.add("A " + r.shape.name().toLowerCase() + " chart with "
                + r.shapeHandle + " as its "
                + (r.shape == Shape.BUCKET ? "handle" : "leading planet") + ".");
        }
        if (r.signatureSign != null) {
            r.headlines.add("The chart's signature is "
                + Character.toUpperCase(r.signatureSign.charAt(0)) + r.signatureSign.substring(1)
                + " - its dominant element and modality meeting in one sign.");
        }
        if (r.hemisphereEmphasis != null) {
            r.headlines.add("Weighted " + r.hemisphereEmphasis + ".");
        }
        if (r.quadrantEmphasis != null) {
            r.headlines.add("Concentrated in the " + r.quadrantEmphasis + " quadrant - "
                + QUADRANT_MEANING.get(r.quadrantEmphasis) + ".");
        }
        if (r.houseModeEmphasis != null) {
            r.headlines.add("Mostly " + r.houseModeEmphasis + " houses - "
                + HOUSE_MODE_MEANING.get(r.houseModeEmphasis) + ".");
        }
        if (r.trinityEmphasis != null) {
            r.headlines.add("Weighted toward the houses of " + r.trinityEmphasis + " - "
                + TRINITY_MEANING.get(r.trinityEmphasis) + ".");
        }
        for (String s : r.singletons) {
            r.headlines.add(s + " - a whole category resting on one body.");
        }
        r.headlines.add(r.chartRuler + " rules the Ascendant.");
        for (String s : r.stelliums) {
            r.headlines.add("A stellium in " + s + ".");
        }
        for (String d : r.finalDispositors) {
            r.headlines.add(d.equals(r.soleDispositor)
                ? d + " disposes the whole chart - every chain of rulership ends there."
                : d + " is a final dispositor, in its own sign.");
        }
        for (String l : r.dispositorLoops) {
            r.headlines.add("Dispositor loop: " + l + " - no single planet resolves the chain.");
        }
        for (AspectPatterns.Pattern p : r.aspectPatterns) {
            String h = "Aspect pattern: " + p.name + " involving " + String.join(", ", p.bodies);
            if (p.apex != null) {
                h += " with apex " + p.apex;
            }
            h += ".";
            r.headlines.add(h);
        }
        r.headlines.addAll(r.lightTransfers);
    }
}
