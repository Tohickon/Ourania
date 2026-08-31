package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Closed geometric circuits: three or more bodies in mutual aspect, acting as one unit.
 *
 * The point of a pattern is that triggering any member triggers all of them, which is why
 * the two boundary conditions below are not fussiness - they are what makes the circuit a
 * circuit rather than a coincidence of angles.
 *
 * <b>Both were adopted on 2026-08-23 at David's instruction, and both changed what the app
 * reports.</b> Before that, patterns were built from every non-angle registry point at each
 * body's full orb, which produced 12.3 configurations per chart, a T-square in 100% of charts,
 * and only 10.5% of them made of planets. The measurement that prompted the change is in
 * TensionReleaseCheck Part E and the before/after is in the session log.
 */
public final class AspectPatterns {

    /**
     * The bodies that can form a pattern: the ten planets and Chiron.
     *
     * <b>Physical bodies only.</b> The Ascendant, MC, nodes, Vertex and the lots are
     * mathematical points - planets can aspect them, and they act as outlets or sensitive
     * degrees, but they do not cast aspects and cannot close a harmonic loop. A T-square
     * whose spine was the nodal axis was the clearest symptom: the nodes are exactly opposite
     * in every chart ever cast, so that figure was the definition restated, not a finding.
     *
     * <b>The asteroids are excluded pending a decision, not by argument.</b> Eris, Eros,
     * Hygiea, Nessus, Pholus, Ceres, Pallas, Juno and Vesta are physical bodies and would
     * qualify on the stated rule; the source names "the planets and, optionally, Chiron", so
     * that is what this list holds. Adding them is one line here - measure it first, because
     * they were the bulk of what the old population reported.
     */
    public static final Set<String> PATTERN_BODIES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList(
            "Sun", "Moon", "Mercury", "Venus", "Mars",
            "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron")));

    /**
     * How far from exact a contact may be and still hold a pattern together, in degrees.
     *
     * A pattern works by harmonic resonance - the tuned-string argument - so a loose contact
     * does not weaken the figure so much as stop it being one. The source gives "ideally
     * under 4 to 5 degrees"; 5.0 is the permissive end of that, chosen so the change from the
     * old behaviour is the population rather than the orb wherever possible.
     *
     * <b>This is a threshold and thresholds move readings.</b> It is David's, not the
     * engine's, and it should not be tuned to make a particular chart come out.
     */
    public static final double MAX_ORB = 5.0;

    /** One closed circuit. */
    public static final class Pattern {
        public String name;
        public List<String> bodies;
        /** The focal point, if applicable (e.g. T-square apex). */
        public String apex;

        /**
         * The modality every member shares, or null when they do not all share one.
         *
         * A T-square is textbook cardinal, fixed or mutable. Null here means the figure is
         * dissociate - held by degree while the signs disagree - which is a real and
         * reportable condition rather than a gap in the data.
         */
        public String modality;

        /** The element every member shares, or null. The grand trine's defining quality. */
        public String element;

        /** The loosest aspect among the members: how near collapse the figure is. */
        public double widestOrb;

        public Pattern(String name, List<String> bodies, String apex) {
            this.name = name;
            this.bodies = new ArrayList<>(bodies);
            this.apex = apex;
            Collections.sort(this.bodies);
        }

        /**
         * The prose key for this figure's mechanics, derived from the name.
         *
         * <b>Derived here, not spelled out in the view.</b> The quincunx spent months as a
         * live aspect with an empty glyph because the model knew a name the view had no case
         * for; a switch in the panel mapping nine pattern names to nine key strings is the
         * same arrangement waiting to happen. AspectPatternCheck asserts that every name the
         * engine can emit resolves to an entry that exists.
         */
        public String detailKey() {
            return "pattern_" + name.toLowerCase().replace("-", "").replace(" ", "");
        }

        /**
         * The key for the modality or element specialisation, or null when the figure has
         * none - either because its type does not take one, or because its members do not
         * share a sign quality and it is dissociate.
         */
        public String variantKey() {
            if (modality != null && (name.equals("T-square") || name.equals("Grand cross"))) {
                return name.toLowerCase().replace("-", "").replace(" ", "") + "_" + modality;
            }
            if (element != null && name.equals("Grand trine")) {
                return "grandtrine_" + element;
            }
            return null;
        }

        /**
         * True when the figure holds by degree while its members' signs disagree - a
         * dissociate pattern, with no clean modality (or element, for a grand trine).
         */
        public boolean isDissociate() {
            if (name.equals("Grand trine") || name.equals("Kite")) {
                return element == null;
            }
            if (name.equals("T-square") || name.equals("Grand cross")) {
                return modality == null;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pattern pattern = (Pattern) o;
            return name.equals(pattern.name) && bodies.equals(pattern.bodies);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + bodies.hashCode();
            return result;
        }
    }

    private AspectPatterns() { }

    /**
     * Patterns without the sign classification, for callers that have no frame to hand.
     * The geometry is identical; only {@link Pattern#modality} and {@link Pattern#element}
     * are left null.
     */
    public static List<Pattern> findPatterns(List<Aspects.Hit> hits) {
        return findPatterns(null, hits);
    }

    /** Patterns, classified by modality and element when a frame is supplied. */
    public static List<Pattern> findPatterns(ChartFrame f, List<Aspects.Hit> hits) {
        List<Aspects.Hit> usable = eligible(hits);
        Set<Pattern> found = new HashSet<>();

        List<Aspects.Hit> oppositions = ofType(usable, Aspects.Type.OPPOSITION);
        List<Aspects.Hit> squares = ofType(usable, Aspects.Type.SQUARE);
        List<Aspects.Hit> trines = ofType(usable, Aspects.Type.TRINE);
        List<Aspects.Hit> sextiles = ofType(usable, Aspects.Type.SEXTILE);
        List<Aspects.Hit> quincunxes = ofType(usable, Aspects.Type.QUINCUNX);
        Set<String> bodies = allBodies(usable);

        // T-Square: an opposition where both ends square a third body (the apex)
        for (Aspects.Hit opp : oppositions) {
            for (String b : bodies) {
                if (!b.equals(opp.a) && !b.equals(opp.b)
                    && hasAspect(squares, opp.a, b) && hasAspect(squares, opp.b, b)) {
                    found.add(new Pattern("T-square", List.of(opp.a, opp.b, b), b));
                }
            }
        }

        // Grand Cross: two oppositions that square each other
        for (int i = 0; i < oppositions.size(); i++) {
            for (int j = i + 1; j < oppositions.size(); j++) {
                Aspects.Hit o1 = oppositions.get(i);
                Aspects.Hit o2 = oppositions.get(j);
                if (distinct(o1.a, o1.b, o2.a, o2.b)
                    && hasAspect(squares, o1.a, o2.a) && hasAspect(squares, o1.a, o2.b)
                    && hasAspect(squares, o1.b, o2.a) && hasAspect(squares, o1.b, o2.b)) {
                    found.add(new Pattern("Grand cross",
                        List.of(o1.a, o1.b, o2.a, o2.b), null));
                }
            }
        }

        // Grand Trine: three bodies all trine each other
        for (int i = 0; i < trines.size(); i++) {
            for (int j = i + 1; j < trines.size(); j++) {
                String common = commonBody(trines.get(i), trines.get(j));
                if (common == null) {
                    continue;
                }
                String other1 = other(trines.get(i), common);
                String other2 = other(trines.get(j), common);
                if (hasAspect(trines, other1, other2)) {
                    found.add(new Pattern("Grand trine", List.of(common, other1, other2), null));
                }
            }
        }

        List<Pattern> grandTrines = found.stream()
            .filter(p -> p.name.equals("Grand trine")).collect(Collectors.toList());

        // Kite: a grand trine plus a fourth body opposite one corner and sextile the other two
        for (Pattern gt : grandTrines) {
            String b1 = gt.bodies.get(0), b2 = gt.bodies.get(1), b3 = gt.bodies.get(2);
            for (String apex : bodies) {
                if (gt.bodies.contains(apex)) {
                    continue;
                }
                if (hasAspect(oppositions, apex, b1)
                    && hasAspect(sextiles, apex, b2) && hasAspect(sextiles, apex, b3)) {
                    found.add(new Pattern("Kite", List.of(b1, b2, b3, apex), b1));
                }
                if (hasAspect(oppositions, apex, b2)
                    && hasAspect(sextiles, apex, b1) && hasAspect(sextiles, apex, b3)) {
                    found.add(new Pattern("Kite", List.of(b1, b2, b3, apex), b2));
                }
                if (hasAspect(oppositions, apex, b3)
                    && hasAspect(sextiles, apex, b1) && hasAspect(sextiles, apex, b2)) {
                    found.add(new Pattern("Kite", List.of(b1, b2, b3, apex), b3));
                }
            }
        }

        // Grand Sextile: two grand trines standing 60 degrees apart.
        //
        // Tested through the cross-aspects rather than by measuring gaps: every body of one
        // trine must be sextile to two members of the other and opposite the third. That is
        // the six-pointed star stated as a property, and it cannot be satisfied by six bodies
        // that merely look evenly spread.
        for (int i = 0; i < grandTrines.size(); i++) {
            for (int j = i + 1; j < grandTrines.size(); j++) {
                Pattern g1 = grandTrines.get(i);
                Pattern g2 = grandTrines.get(j);
                if (g1.bodies.stream().anyMatch(g2.bodies::contains)) {
                    continue;
                }
                boolean star = true;
                for (String a : g1.bodies) {
                    int sex = 0, opp = 0;
                    for (String b : g2.bodies) {
                        if (hasAspect(sextiles, a, b)) sex++;
                        else if (hasAspect(oppositions, a, b)) opp++;
                    }
                    if (sex != 2 || opp != 1) {
                        star = false;
                        break;
                    }
                }
                if (star) {
                    List<String> six = new ArrayList<>(g1.bodies);
                    six.addAll(g2.bodies);
                    found.add(new Pattern("Grand sextile", six, null));
                }
            }
        }

        // Yod: a sextile whose ends both quincunx a third body (the apex).
        // Boomerang: the same figure with a fourth body opposite that apex.
        List<Pattern> yods = new ArrayList<>();
        for (Aspects.Hit sex : sextiles) {
            for (String b : bodies) {
                if (!b.equals(sex.a) && !b.equals(sex.b)
                    && hasAspect(quincunxes, sex.a, b) && hasAspect(quincunxes, sex.b, b)) {
                    yods.add(new Pattern("Yod", List.of(sex.a, sex.b, b), b));
                }
            }
        }
        for (Pattern yod : yods) {
            String release = null;
            for (String b : bodies) {
                if (!yod.bodies.contains(b) && hasAspect(oppositions, b, yod.apex)) {
                    release = b;
                    break;
                }
            }
            if (release == null) {
                found.add(yod);
            } else {
                // Reported as a Boomerang and NOT also as the Yod inside it. One figure gets
                // one reading; a Yod that says "no point of resolution" printed beside the
                // Boomerang that supplies one is two surfaces contradicting each other.
                List<String> four = new ArrayList<>(yod.bodies);
                four.add(release);
                found.add(new Pattern("Boomerang", four, yod.apex));
            }
        }

        // Mystic Rectangle: two oppositions whose endpoints are trine and sextile across
        for (int i = 0; i < oppositions.size(); i++) {
            for (int j = i + 1; j < oppositions.size(); j++) {
                Aspects.Hit o1 = oppositions.get(i);
                Aspects.Hit o2 = oppositions.get(j);
                if (distinct(o1.a, o1.b, o2.a, o2.b)
                    && ((hasAspect(trines, o1.a, o2.a) && hasAspect(trines, o1.b, o2.b)
                         && hasAspect(sextiles, o1.a, o2.b) && hasAspect(sextiles, o1.b, o2.a))
                        || (hasAspect(trines, o1.a, o2.b) && hasAspect(trines, o1.b, o2.a)
                            && hasAspect(sextiles, o1.a, o2.a)
                            && hasAspect(sextiles, o1.b, o2.b)))) {
                    found.add(new Pattern("Mystic rectangle",
                        List.of(o1.a, o1.b, o2.a, o2.b), null));
                }
            }
        }

        // Cradle: three linked sextiles closing a trine between the endpoints
        for (Aspects.Hit t : trines) {
            for (String b2 : bodies) {
                if (b2.equals(t.a) || b2.equals(t.b)) {
                    continue;
                }
                for (String b3 : bodies) {
                    if (b3.equals(t.a) || b3.equals(t.b) || b3.equals(b2)) {
                        continue;
                    }
                    if (hasAspect(sextiles, t.a, b2) && hasAspect(sextiles, b2, b3)
                        && hasAspect(sextiles, b3, t.b)) {
                        found.add(new Pattern("Cradle", List.of(t.a, b2, b3, t.b), null));
                    }
                }
            }
        }

        List<Pattern> out = new ArrayList<>(found);
        for (Pattern p : out) {
            classify(f, p, usable);
        }
        // Stable order, because this was built out of a HashSet and two runs of the same
        // chart must produce the same list for anything downstream to be checkable.
        out.sort((x, y) -> {
            int c = x.name.compareTo(y.name);
            return c != 0 ? c : String.join(",", x.bodies).compareTo(String.join(",", y.bodies));
        });
        return out;
    }

    /**
     * The hits a pattern may be built from: both ends in {@link #PATTERN_BODIES}, and inside
     * {@link #MAX_ORB} of exact.
     */
    private static List<Aspects.Hit> eligible(List<Aspects.Hit> hits) {
        List<Aspects.Hit> out = new ArrayList<>();
        for (Aspects.Hit h : hits) {
            if (PATTERN_BODIES.contains(h.a) && PATTERN_BODIES.contains(h.b)
                && h.offBy <= MAX_ORB) {
                out.add(h);
            }
        }
        return out;
    }

    /** Shared modality and element, and the loosest contact holding the figure together. */
    private static void classify(ChartFrame f, Pattern p, List<Aspects.Hit> usable) {
        for (Aspects.Hit h : usable) {
            if (p.bodies.contains(h.a) && p.bodies.contains(h.b)) {
                p.widestOrb = Math.max(p.widestOrb, h.offBy);
            }
        }
        if (f == null) {
            return;
        }
        String modality = null;
        String element = null;
        boolean first = true;
        for (String name : p.bodies) {
            ChartFrame.Body b = f.body(name);
            if (b == null || !b.ok) {
                return;                      // cannot classify what did not compute
            }
            int sign = Zodiac.signIndex(b.lon);
            String m = Zodiac.modalityName(sign);
            String e = Zodiac.elementName(sign);
            if (first) {
                modality = m;
                element = e;
                first = false;
            } else {
                if (!m.equals(modality)) modality = null;
                if (!e.equals(element)) element = null;
            }
        }
        p.modality = modality;
        p.element = element;
    }

    private static List<Aspects.Hit> ofType(List<Aspects.Hit> hits, Aspects.Type t) {
        List<Aspects.Hit> out = new ArrayList<>();
        for (Aspects.Hit h : hits) {
            if (h.type == t) {
                out.add(h);
            }
        }
        return out;
    }

    private static boolean hasAspect(List<Aspects.Hit> aspects, String a, String b) {
        for (Aspects.Hit h : aspects) {
            if ((h.a.equals(a) && h.b.equals(b)) || (h.a.equals(b) && h.b.equals(a))) {
                return true;
            }
        }
        return false;
    }

    private static String commonBody(Aspects.Hit h1, Aspects.Hit h2) {
        if (h1.a.equals(h2.a) || h1.a.equals(h2.b)) return h1.a;
        if (h1.b.equals(h2.a) || h1.b.equals(h2.b)) return h1.b;
        return null;
    }

    private static String other(Aspects.Hit h, String one) {
        return h.a.equals(one) ? h.b : h.a;
    }

    private static boolean distinct(String... bodies) {
        Set<String> s = new HashSet<>();
        for (String b : bodies) {
            if (!s.add(b)) return false;
        }
        return true;
    }

    private static Set<String> allBodies(List<Aspects.Hit> hits) {
        Set<String> bodies = new LinkedHashSet<>();
        for (Aspects.Hit h : hits) {
            bodies.add(h.a);
            bodies.add(h.b);
        }
        return bodies;
    }
}
