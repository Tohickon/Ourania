package com.zodiacomputing.ourania.astro;

/**
 * Not all quincunxes are the same, and the difference is computable.
 *
 * A quincunx joins signs that share no element, no modality and no polarity - which is the
 * whole of its difficulty, and also the reason one generic paragraph has served all 49 pairs
 * until now. But the tradition (Burk, Cunningham) separates them by whether any *secondary*
 * relationship survives that incompatibility, and every input for that already exists in this
 * engine: {@link Dignity} holds the traditional rulers and {@link Zodiac} now does the
 * solstice- and equinox-axis reflections.
 *
 * <b>Three connections, then aversion:</b>
 *
 * <ul>
 *   <li><b>Common rulership</b> - both signs answer to the same planet, so there is an
 *       arbiter. The easiest kind to resolve.</li>
 *   <li><b>Antiscia</b> - mirrored across the solstitial axis, equal length of day. A softer,
 *       cooperative link.</li>
 *   <li><b>Contra-antiscia</b> - mirrored across the equinoctial axis, equal but opposite
 *       declination. A link, and a more oppositional one.</li>
 *   <li><b>Averse</b> - none of the three. The signs genuinely cannot see each other, and this
 *       is the quincunx at full strength.</li>
 * </ul>
 *
 * <b>The classification is derived, not tabulated.</b> There is no list of twelve pairs in this
 * file; each is worked out from the rulership table and the reflection arithmetic, so a change
 * to either shows up here rather than drifting away from it. QuincunxCheck asserts the result
 * against a hand-typed table of the traditional pairs, which is the independent second opinion.
 */
public final class Quincunx {

    /** Which secondary relationship survives the incongruity, if any. */
    public enum Kind {
        COMMON_RULERSHIP("common rulership",
            "Both signs answer to the same planet, so the dispute has an arbiter. The two "
            + "sides want different things and can still be referred to one authority, which "
            + "makes this the most tractable quincunx there is."),
        ANTISCIA("antiscia",
            "The signs mirror each other across the solstice axis and hold equal length of "
            + "day. Nothing about the incompatibility changes, but there is a hidden sympathy "
            + "underneath it and the adjustment tends to be cooperative rather than grinding."),
        CONTRA_ANTISCIA("contra-antiscia",
            "The signs mirror each other across the equinox axis, at equal but opposite "
            + "declination. A real connection and an oppositional one: the two sides recognise "
            + "each other the way adversaries do, which is still more than most quincunxes "
            + "manage."),
        AVERSE("averse",
            "No shared ruler, no antiscion, no contra-antiscion. The two signs have no ground "
            + "whatever on which to meet, and this is the quincunx at full strength - the "
            + "purest form of the incongruity, and the one that stays a stone in the shoe.");

        /** How this reads on screen. */
        public final String label;
        /** What it means for the reading. */
        public final String meaning;

        Kind(String label, String meaning) {
            this.label = label;
            this.meaning = meaning;
        }
    }

    /** A classified quincunx: its kind, and the shared ruler where there is one. */
    public static final class Relation {
        public final Kind kind;
        /** The planet both signs answer to, or null unless the kind is COMMON_RULERSHIP. */
        public final String sharedRuler;

        Relation(Kind kind, String sharedRuler) {
            this.kind = kind;
            this.sharedRuler = sharedRuler;
        }

        /** "averse" / "common rulership under Mars". */
        public String describe() {
            return sharedRuler == null ? kind.label : kind.label + " under " + sharedRuler;
        }
    }

    private Quincunx() { }

    /** True when two sign indices stand five signs apart, either way round. */
    public static boolean isQuincunxPair(int signA, int signB) {
        int d = Math.floorMod(signB - signA, 12);
        return d == 5 || d == 7;
    }

    /**
     * Classify a quincunx between two signs.
     *
     * Order-independent, because the relationship is a property of the pair. Throws on a pair
     * that is not a quincunx at all rather than quietly returning AVERSE - a caller asking
     * about a square has made a mistake, and a plausible answer would hide it.
     */
    public static Relation classify(int signA, int signB) {
        int a = Math.floorMod(signA, 12);
        int b = Math.floorMod(signB, 12);
        if (!isQuincunxPair(a, b)) {
            throw new IllegalArgumentException(
                "not a quincunx pair: " + Zodiac.SIGNS[a] + " and " + Zodiac.SIGNS[b]);
        }

        String rulerA = Dignity.domicileRulerOf(a);
        String rulerB = Dignity.domicileRulerOf(b);
        if (rulerA.equals(rulerB)) {
            return new Relation(Kind.COMMON_RULERSHIP, rulerA);
        }
        if (signOfAntiscion(a) == b) {
            return new Relation(Kind.ANTISCIA, null);
        }
        if (signOfContraAntiscion(a) == b) {
            return new Relation(Kind.CONTRA_ANTISCIA, null);
        }
        return new Relation(Kind.AVERSE, null);
    }

    /** Classify the quincunx between two longitudes. */
    public static Relation classify(double lonA, double lonB) {
        return classify(Zodiac.signIndex(lonA), Zodiac.signIndex(lonB));
    }

    /**
     * The sign a whole sign reflects into across the solstitial axis.
     *
     * Taken from the middle of the sign rather than its cusp, because the reflection maps a
     * sign's start to the next sign's end - reflecting 0 Leo gives exactly 0 Gemini, the
     * boundary, and which side of it you land on is a rounding question rather than an
     * astrological one. The midpoint has no such ambiguity.
     */
    private static int signOfAntiscion(int sign) {
        return Zodiac.signIndex(Zodiac.antiscion(sign * 30.0 + 15.0));
    }

    private static int signOfContraAntiscion(int sign) {
        return Zodiac.signIndex(Zodiac.contraAntiscion(sign * 30.0 + 15.0));
    }
}
