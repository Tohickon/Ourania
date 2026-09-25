package com.zodiacomputing.ourania.astro;

/**
 * The house systems this app offers, in one place.
 *
 * <p><b>There were four.</b> The name a reader picks was turned into the character the ephemeris
 * wants by two separate {@code switch} blocks in {@code SkymapPanel} - one when the setting is
 * loaded and one when the combo changes - the list offered was a fourth literal array beside them,
 * and {@code Precision.houseSystemName} turned the character back into a name from a fifth list of
 * its own.
 *
 * <p><b>They had already drifted.</b> {@code Precision} knew Porphyry; neither switch did, and the
 * picker did not offer it - so the system this app falls back to above the polar circle was one a
 * reader could not choose. And the second switch's Regiomontanus case had no {@code break}, which
 * was harmless only because it happened to be written last.
 *
 * <p>That is this project's most logged defect - one rule implemented several times, the copies
 * diverging the day one of them is edited - and a list of house systems is exactly the shape that
 * attracts it, because adding one means finding every copy.
 *
 * <p><b>The ephemeris supports more than this offers.</b> {@code SweHouse} documents seventeen
 * codes. Gauquelin's 'G' is deliberately absent and always will be: it divides the chart into
 * thirty-six sectors rather than twelve houses, and every cusp array, wheel ring and house-based
 * reading in this app assumes twelve.
 */
public final class HouseSystems {

    private HouseSystems() {
    }

    /** One house system: what the ephemeris calls it, what a reader calls it, and what it is. */
    public static final class System {
        /** The character {@code swe_houses} takes. */
        public final char code;
        /** The name on the settings screen and in the saved settings file. */
        public final String name;
        /** One line, for the picker's hover. In a reader's language, not a textbook's. */
        public final String about;
        /**
         * True when this system has no solution at high latitudes and the engine substitutes
         * Porphyry - see {@link Precision#housesNote}. A fact about the geometry, not a defect.
         */
        public final boolean failsNearThePoles;

        System(char code, String name, String about, boolean failsNearThePoles) {
            this.code = code;
            this.name = name;
            this.about = about;
            this.failsNearThePoles = failsNearThePoles;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Every system offered, in the order the picker shows them.
     *
     * <b>The order is the one the picker already had</b>, so that this change moves no reader's
     * eye: the four systems in common use first, then the rest.
     */
    public static final System[] ALL = {
        new System('P', "Placidus",
            "Divides the time it takes each degree to rise. The commonest system in modern "
                + "practice, and the one that fails in the far north.", true),
        new System('K', "Koch",
            "Like Placidus, timed from the birth latitude rather than the equator. Also has no "
                + "solution above the polar circle.", true),
        new System('E', "Equal",
            "Twelve equal thirty-degree houses from the Ascendant. Works at any latitude.", false),
        new System('W', "Whole Sign",
            "The rising sign is the whole first house, and each sign after it a house. The "
                + "oldest system, and the one Hellenistic practice assumes.", false),
        new System('C', "Campanus",
            "Divides the prime vertical - the circle through east, west and the point overhead - "
                + "into equal arcs.", false),
        new System('R', "Regiomontanus",
            "Divides the celestial equator into equal arcs and projects them onto the ecliptic. "
                + "The medieval standard, and what traditional horary expects.", false),
    };

    /** What a chart uses when nobody has chosen. */
    public static final String DEFAULT_NAME = "Placidus";

    /** The default's code, so a caller needing one does not repeat the name. */
    public static char defaultCode() {
        return codeFor(DEFAULT_NAME);
    }

    /** The system of that name, or null. */
    public static System byName(String name) {
        for (System s : ALL) {
            if (s.name.equals(name)) {
                return s;
            }
        }
        return null;
    }

    /** The system with that code, or null. */
    public static System byCode(int code) {
        for (System s : ALL) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /**
     * The ephemeris character for a reader's name, falling back to the default.
     *
     * <b>An unknown name gets the default rather than an exception</b>, because the name can come
     * from a settings file a reader has edited, or one written by a later version of this app.
     * Refusing to draw a chart because a string was not recognised would be a worse answer than
     * drawing the usual one.
     */
    public static char codeFor(String name) {
        System s = byName(name);
        return s != null ? s.code : byName(DEFAULT_NAME).code;
    }

    /**
     * The reader's name for an ephemeris character.
     *
     * <b>Answers for systems that are not offered</b>, because the engine can hand back a code
     * this app does not put in the picker - Porphyry is the substitute above the polar circle, and
     * a notice naming it has to be able to say so.
     */
    public static String nameFor(int code) {
        System s = byCode(code);
        if (s != null) {
            return s.name;
        }
        switch (code) {
            case 'O': return "Porphyry";
            case 'B': return "Alcabitius";
            case 'T': return "Topocentric";
            case 'M': return "Morinus";
            case 'X': return "Meridian";
            case 'V': return "Vehlow";
            case 'U': return "Krusinski";
            case 'Y': return "APC";
            case 'A': return "Equal";
            case 'H': return "Horizontal";
            default:  return "The selected";
        }
    }

    /** The names, in picker order. */
    public static String[] names() {
        String[] out = new String[ALL.length];
        for (int i = 0; i < ALL.length; i++) {
            out[i] = ALL[i].name;
        }
        return out;
    }
}
