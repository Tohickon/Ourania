package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L7 reduced: repetition counting with the independence rule, plus contradiction
 * detection and a deduplicated ranking.
 *
 * The traditional test of significance is repetition - a theme that shows up three or
 * more times matters. Counted naively that is worthless, because one loud placement
 * generates a dozen statements that all say the same thing. So:
 *
 *   Two candidates count as independent witnesses only if their provenance sets
 *   are disjoint.
 *
 * Candidates are grouped per theme into connected components by provenance overlap, and
 * the theme's witness count is the number of components, not the number of candidates.
 */
public final class Themes {

    /** A theme needs this many independent witnesses to count as a chart signature. */
    public static int signatureThreshold = 3;
    /** Both sides of a contradiction need this many. Lower: contradictions are rarer. */
    public static int contradictionThreshold = 2;

    /**
     * A candidate below this weight does not count as a witness.
     *
     * Independence alone is not enough. Without a floor, three structurally independent
     * but near-invisible placements - Pluto at 0.01 prominence, Chiron at 0.18 - out-vote
     * one loud one, which is the naive-counting failure wearing a different hat. Such
     * candidates still appear in the ranked list; they just do not carry a theme.
     */
    public static double witnessFloor = 0.25;

    /**
     * Opposing theme pairs. A chart holding both at strength is not "balanced" - it is
     * divided, and saying so is the single biggest quality difference in a generated
     * reading. These are never averaged.
     */
    private static final String[][] OPPOSITES = {
        {"fiery", "watery"},
        {"airy", "earthy"},
        {"initiating", "consolidating"},
        // Two separate pairs, deliberately. Dignity answers "is it functioning well",
        // valence answers "does it help or hurt", and they are independent - a superbly
        // dignified out-of-sect Saturn is resourced AND constrained. Folding them into
        // one pair makes a body contradict itself and manufactures a false tension.
        {"resourced", "strained"},
        {"constrained", "expansive"},
        {"public", "private"},
        {"self-directed", "other-directed"}
    };

    private static final String[] ELEMENT_THEME = {"fiery", "earthy", "airy", "watery"};
    private static final String[] MODALITY_THEME = {"initiating", "consolidating", "adapting"};

    private static final Map<String, String> TONE = new LinkedHashMap<>();
    static {
        TONE.put("Sun", "solar");
        TONE.put("Moon", "lunar");
        TONE.put("Mercury", "mercurial");
        TONE.put("Venus", "venusian");
        TONE.put("Mars", "martial");
        TONE.put("Jupiter", "jovial");
        TONE.put("Saturn", "saturnian");
        TONE.put("Uranus", "disruptive");
        TONE.put("Neptune", "dissolving");
        TONE.put("Pluto", "transformative");
    }

    public static final class Candidate {
        public final String text;
        public final Set<String> themes = new LinkedHashSet<>();
        public final Set<String> provenance = new LinkedHashSet<>();
        public final double weight;
        public final String source;

        Candidate(String text, double weight, String source) {
            this.text = text;
            this.weight = weight;
            this.source = source;
        }

        Candidate theme(String... t) {
            for (String x : t) {
                if (x != null) {
                    themes.add(x);
                }
            }
            return this;
        }

        Candidate from(String... p) {
            for (String x : p) {
                if (x != null) {
                    provenance.add(x);
                }
            }
            return this;
        }

        boolean overlaps(Candidate o) {
            for (String p : provenance) {
                if (o.provenance.contains(p)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class Signature {
        public String theme;
        public int witnessCount;
        public int candidateCount;
        public final List<List<Candidate>> components = new ArrayList<>();
    }

    public static final class Contradiction {
        public String themeA;
        public String themeB;
        public int witnessesA;
        public int witnessesB;
        public Candidate strongestA;
        public Candidate strongestB;
    }

    public static final class Result {
        public final List<Candidate> candidates = new ArrayList<>();
        public final List<Signature> signatures = new ArrayList<>();
        public final List<Contradiction> contradictions = new ArrayList<>();
        public final List<Candidate> ranked = new ArrayList<>();
    }

    private Themes() { }

    // ------------------------------------------------------------------ extraction

    public static Result extract(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked) {
        Result r = new Result();
        gatherFromBodies(r, ranked);
        gatherFromGestalt(r, g);
        countSignatures(r);
        findContradictions(r);
        rankAndDeduplicate(r);
        return r;
    }

    private static void gatherFromBodies(Result r, List<BodyScore.Vector> ranked) {
        for (BodyScore.Vector v : ranked) {
            if (v.prominence <= 0.0) {
                continue;
            }
            int s = Zodiac.signIndex(v.longitude);
            Candidate c = new Candidate(describe(v), v.prominence, "L3");
            c.from(v.body);
            c.theme(TONE.get(v.body), ELEMENT_THEME[s % 4], MODALITY_THEME[s % 3]);

            if (v.dignity != null && v.dignity.score >= 3) {
                c.theme("resourced");
            } else if (v.dignity != null && v.dignity.score <= -3) {
                c.theme("strained");
            }
            if (v.valence != null && v.valence.score >= 2) {
                c.theme("expansive");
            } else if (v.valence != null && v.valence.score <= -2) {
                c.theme("constrained");
            }
            if (v.angularity > 0.0) {
                c.from(v.nearestAngle);
                if ("MC".equals(v.nearestAngle)) {
                    c.theme("public");
                } else if ("IC".equals(v.nearestAngle)) {
                    c.theme("private");
                } else if ("Ascendant".equals(v.nearestAngle)) {
                    c.theme("self-directed");
                } else if ("Descendant".equals(v.nearestAngle)) {
                    c.theme("other-directed");
                }
            }
            r.candidates.add(c);
        }
    }

    private static void gatherFromGestalt(Result r, Gestalt.Result g) {
        for (int i = 0; i < Gestalt.ELEMENTS.length; i++) {
            String e = Gestalt.ELEMENTS[i];
            if (g.dominantElements.contains(e)) {
                r.candidates.add(new Candidate(
                    "The chart leans heavily " + ELEMENT_THEME[i] + ".", 0.8, "L5")
                    .theme(ELEMENT_THEME[i]).from("element-balance"));
            }
            if (g.missingElements.contains(e)) {
                r.candidates.add(new Candidate(
                    "There is essentially no " + e + " here, and a lack reads louder "
                        + "than an abundance.", 0.9, "L5")
                    .theme("absence-of-" + e).from("element-balance"));
            }
        }
        for (int i = 0; i < Gestalt.MODALITIES.length; i++) {
            if (g.dominantModalities.contains(Gestalt.MODALITIES[i])) {
                r.candidates.add(new Candidate(
                    "Predominantly " + Gestalt.MODALITIES[i] + ".", 0.7, "L5")
                    .theme(MODALITY_THEME[i]).from("modality-balance"));
            }
        }

        if (g.hemisphereEmphasis != null) {
            Candidate c = new Candidate("Weighted " + g.hemisphereEmphasis + ".", 0.7, "L5")
                .from("hemispheres");
            if (g.hemisphereEmphasis.contains("above")) {
                c.theme("public");
            }
            if (g.hemisphereEmphasis.contains("below")) {
                c.theme("private");
            }
            if (g.hemisphereEmphasis.contains("eastern")) {
                c.theme("self-directed");
            }
            if (g.hemisphereEmphasis.contains("western")) {
                c.theme("other-directed");
            }
            r.candidates.add(c);
        }

        for (String st : g.stelliums) {
            String sign = st.substring(0, st.indexOf(':'));
            int s = Dignity.indexOfSign(sign);
            Candidate c = new Candidate("A concentration in " + sign + " - "
                + st.substring(st.indexOf(':') + 2) + ".", 0.85, "L5")
                .theme(ELEMENT_THEME[s % 4], MODALITY_THEME[s % 3],
                       TONE.get(Dignity.domicileRulerOf(s)));
            for (String b : st.substring(st.indexOf(':') + 2).split(", ")) {
                c.from(b);
            }
            r.candidates.add(c);
        }

        if (g.soleDispositor != null) {
            r.candidates.add(new Candidate(
                "Every chain of rulership ends at " + g.soleDispositor + ".", 0.85, "L5")
                .theme(TONE.get(g.soleDispositor)).from("dispositor-chain"));
        } else {
            for (String d : g.finalDispositors) {
                r.candidates.add(new Candidate(d + " sits in its own sign.", 0.6, "L5")
                    .theme(TONE.get(d)).from("dispositor-chain"));
            }
        }

        if (g.shapeHandle != null) {
            r.candidates.add(new Candidate("A " + g.shape.name().toLowerCase()
                + " shape, with " + g.shapeHandle + " carrying it.", 0.6, "L5")
                .theme(TONE.get(g.shapeHandle)).from("chart-shape"));
        }
    }

    // ------------------------------------------------------------------ counting

    /**
     * Group each theme's candidates into connected components by provenance overlap.
     * The witness count is the number of components. Two statements about the same body
     * are one witness however differently they are phrased.
     */
    private static void countSignatures(Result r) {
        Map<String, List<Candidate>> byTheme = new LinkedHashMap<>();
        for (Candidate c : r.candidates) {
            if (c.weight < witnessFloor) {
                continue;   // present in the chart, but too quiet to be a witness
            }
            for (String t : c.themes) {
                byTheme.computeIfAbsent(t, k -> new ArrayList<>()).add(c);
            }
        }
        for (Map.Entry<String, List<Candidate>> e : byTheme.entrySet()) {
            Signature sig = new Signature();
            sig.theme = e.getKey();
            sig.candidateCount = e.getValue().size();
            sig.components.addAll(components(e.getValue()));
            sig.witnessCount = sig.components.size();
            r.signatures.add(sig);
        }
        r.signatures.sort(Comparator.comparingInt((Signature s) -> s.witnessCount).reversed());
    }

    /** Union-find by provenance overlap, done directly - the lists are tiny. */
    private static List<List<Candidate>> components(List<Candidate> items) {
        List<List<Candidate>> comps = new ArrayList<>();
        for (Candidate c : items) {
            List<List<Candidate>> hits = new ArrayList<>();
            for (List<Candidate> comp : comps) {
                for (Candidate other : comp) {
                    if (c.overlaps(other)) {
                        hits.add(comp);
                        break;
                    }
                }
            }
            if (hits.isEmpty()) {
                List<Candidate> fresh = new ArrayList<>();
                fresh.add(c);
                comps.add(fresh);
            } else {
                // Merge every component this candidate bridges.
                List<Candidate> merged = hits.get(0);
                merged.add(c);
                for (int i = 1; i < hits.size(); i++) {
                    merged.addAll(hits.get(i));
                    comps.remove(hits.get(i));
                }
            }
        }
        return comps;
    }

    public static int witnessesFor(Result r, String theme) {
        for (Signature s : r.signatures) {
            if (s.theme.equals(theme)) {
                return s.witnessCount;
            }
        }
        return 0;
    }

    public static List<Signature> signaturesAtThreshold(Result r) {
        List<Signature> out = new ArrayList<>();
        for (Signature s : r.signatures) {
            if (s.witnessCount >= signatureThreshold) {
                out.add(s);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ contradiction

    private static void findContradictions(Result r) {
        for (String[] pair : OPPOSITES) {
            int a = witnessesFor(r, pair[0]);
            int b = witnessesFor(r, pair[1]);
            if (a >= contradictionThreshold && b >= contradictionThreshold) {
                Contradiction c = new Contradiction();
                c.themeA = pair[0];
                c.themeB = pair[1];
                c.witnessesA = a;
                c.witnessesB = b;
                c.strongestA = strongest(r, pair[0]);
                c.strongestB = strongest(r, pair[1]);
                r.contradictions.add(c);
            }
        }
        r.contradictions.sort(Comparator.comparingInt(
            (Contradiction c) -> Math.min(c.witnessesA, c.witnessesB)).reversed());
    }

    private static Candidate strongest(Result r, String theme) {
        Candidate best = null;
        for (Candidate c : r.candidates) {
            if (c.weight >= witnessFloor && c.themes.contains(theme)
                && (best == null || c.weight > best.weight)) {
                best = c;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ ranking

    /**
     * Two candidates with the same provenance and overlapping themes are the same
     * statement twice. Without this the top of the list fills with restatements of the
     * loudest placement - the exact failure the independence rule fixes for counting,
     * reappearing at the output stage.
     */
    private static void rankAndDeduplicate(Result r) {
        List<Candidate> sorted = new ArrayList<>(r.candidates);
        sorted.sort(Comparator.comparingDouble((Candidate c) -> c.weight).reversed());
        for (Candidate c : sorted) {
            boolean dup = false;
            for (Candidate kept : r.ranked) {
                if (kept.provenance.equals(c.provenance)) {
                    boolean shared = false;
                    for (String t : c.themes) {
                        if (kept.themes.contains(t)) {
                            shared = true;
                            break;
                        }
                    }
                    if (shared) {
                        dup = true;
                        break;
                    }
                }
            }
            if (!dup) {
                r.ranked.add(c);
            }
        }
    }

    // ------------------------------------------------------------------ test hooks

    /**
     * Builds a candidate directly, so the independence rule can be exercised against a
     * hand-built case rather than only against whatever a real chart happens to produce.
     */
    static Candidate testCandidate(String text, double weight, String theme, String... prov) {
        Candidate c = new Candidate(text, weight, "test");
        c.theme(theme);
        c.from(prov);
        return c;
    }

    /** Runs the counting passes over a hand-assembled candidate list. */
    static void recountForTest(Result r) {
        r.signatures.clear();
        r.contradictions.clear();
        r.ranked.clear();
        countSignatures(r);
        findContradictions(r);
        rankAndDeduplicate(r);
    }

    // ------------------------------------------------------------------ text

    /** Structural description of a body, built from the reasons L3 already produced. */
    private static String describe(BodyScore.Vector v) {
        StringBuilder sb = new StringBuilder();
        sb.append(v.body).append(" at ").append(Zodiac.format(v.longitude));
        List<String> why = v.allReasons();
        if (!why.isEmpty()) {
            sb.append(" - ").append(String.join("; ", why));
        }
        return sb.toString();
    }
}
