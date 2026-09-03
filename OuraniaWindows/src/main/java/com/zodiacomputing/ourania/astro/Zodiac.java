package com.zodiacomputing.ourania.astro;

/**
 * Layer 2 positional derivation: turns an ecliptic longitude into the categorical
 * buckets the interpretation data is keyed by.
 *
 * Every method here is a pure function of a longitude. No ephemeris, no dates, no
 * I/O, no Swing. That is deliberate: it lets ZodiacSelfTest exercise the whole layer
 * without starting the app or touching the bundled Swiss Ephemeris sources.
 *
 * Key format matches InterpretationService: lowercase sign name, underscore, ordinal.
 */
public final class Zodiac {

    /** Sign names in zodiacal order, lowercase to match the JSON key prefixes. */
    public static final String[] SIGNS = {
        "aries", "taurus", "gemini", "cancer", "leo", "virgo",
        "libra", "scorpio", "sagittarius", "capricorn", "aquarius", "pisces"
    };

    /**
     * The Chaldean order of the planets, rotated to start at Mars because Mars rules
     * the first decan of Aries. Walking this cycle across all 36 decans in zodiacal
     * order gives the traditional decan (face) rulers.
     *
     * This is the scheme the Golden Dawn tarot decan cards in interpretations.json
     * assume, and the one Sabian_interpretations.json records in its decan_ruler field.
     */
    private static final String[] CHALDEAN_CYCLE = {
        "Mars", "Sun", "Venus", "Mercury", "Moon", "Saturn", "Jupiter"
    };

    /**
     * Modern rulers, indexed by sign. Used by the triplicity scheme only, which is why
     * the three outer planets appear here and never in CHALDEAN_CYCLE.
     */
    private static final String[] MODERN_SIGN_RULERS = {
        "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
        "Venus", "Pluto", "Jupiter", "Saturn", "Uranus", "Neptune"
    };

    /*
     * Two decan rulership schemes coexist in this app, deliberately, and each surface
     * states which one it is showing. They are not reconcilable and neither is wrong:
     *
     *   Chaldean (face)  - CHALDEAN_CYCLE above. Sabian_interpretations.json decan_ruler,
     *                      and the Golden Dawn tarot decan cards, which are defined in
     *                      Chaldean order and cannot be remapped without breaking the
     *                      card attributions.
     *   Triplicity       - triplicityDecanRuler below. The "decans" prose section of
     *                      interpretations.json, and the decan ring drawn on the wheel.
     *                      A 20th century scheme (Alan Leo), and this project's data
     *                      takes it with modern rulers.
     *
     * They agree on only 6 of the 36 decans: aries I, aries II, taurus III, leo II,
     * leo III and aquarius II. An earlier note here said 27 disagreed, which is the
     * count you get for triplicity with traditional rulers; the prose uses modern
     * rulers, so every outer-planet decan mismatches too and the real count is 30.
     * ZodiacSelfTest asserts both the 6 and the two datasets that carry these schemes.
     */

    private Zodiac() { }

    /**
     * Wraps any longitude into [0, 360), handling negatives and values at or above 360.
     * Callers taking values straight out of Swiss Ephemeris should pass them through
     * here once, at the L1/L2 boundary, and never again.
     */
    public static double normalise(double longitude) {
        double d = longitude % 360.0;
        return d < 0.0 ? d + 360.0 : d;
    }

    /**
     * The point directly across the wheel.
     *
     * A one-liner with four call sites before this existed: ChartFrame derived the
     * Descendant, the IC and the south node this way, and the wheel needed the same rule
     * again for the points it draws. Four spellings of "+180, wrapped" is how three of them
     * stay right and the fourth quietly loses the wrap.
     */
    public static double opposite(double longitude) {
        return normalise(longitude + 180.0);
    }

    /**
     * The antiscion: this degree mirrored across the solstitial axis, 0 Cancer to 0 Capricorn.
     *
     * Two degrees in antiscion stand at equal declination on either side of a solstice, so they
     * receive the same length of day - the traditional argument for reading them as a hidden
     * sympathy. The reflection is about 90 degrees, which makes the arithmetic 180 - longitude,
     * and it pairs the signs Aries-Virgo, Taurus-Leo, Gemini-Cancer, Libra-Pisces,
     * Scorpio-Aquarius and Sagittarius-Capricorn.
     */
    public static double antiscion(double longitude) {
        return normalise(180.0 - longitude);
    }

    /**
     * The contra-antiscion: mirrored across the equinoctial axis, 0 Aries to 0 Libra.
     *
     * The same construction about the other axis, giving equal but opposite declination. Pairs
     * Aries-Pisces, Taurus-Aquarius, Gemini-Capricorn, Cancer-Sagittarius, Leo-Scorpio and
     * Virgo-Libra.
     */
    public static double contraAntiscion(double longitude) {
        return normalise(360.0 - longitude);
    }

    /** 0 = Aries through 11 = Pisces. */
    public static int signIndex(double longitude) {
        return (int) Math.floor(normalise(longitude) / 30.0);
    }

    /** Lowercase sign name, matching the JSON key prefix. */
    public static String signName(double longitude) {
        return SIGNS[signIndex(longitude)];
    }

    /** Position within the sign, in [0, 30). */
    public static double degreeInSign(double longitude) {
        return normalise(longitude) % 30.0;
    }

    /**
     * Where a degree sits in its sign's arc of development.
     *
     * Two conditions, and they are opposite ones rather than two names for a boundary. Settled
     * 2026-09-03; see DECISIONS.md, K3.
     */
    public enum DegreeStatus {
        /** 0°00'00" to 0°59'59". A phase beginning: raw, undifferentiated, impressionable. */
        INITIATION,
        /** 29°00'00" to 29°59'59". A phase ending, under pressure to complete. */
        ANARETIC,
        /** Everything between. */
        STANDARD
    }

    /**
     * The developmental status of a longitude's degree within its sign.
     *
     * <b>Zero tolerance on either side, deliberately.</b> 28°59'59" is not anaretic - it is
     * still the 29th degree, and the 30th is what the condition is about. 0°00'00" of the next
     * sign is not anaretic either; it is the opposite condition. March and McEvers put both
     * under "critical degrees" while naming them as opposite directions - "just beginning" or
     * "nearly ending some phase" - so they get separate values here rather than one flag with
     * two meanings, which is how they would end up merged by whoever reads this next.
     *
     * The 0-to-29 convention is why the last degree is written 29 and keyed 30: a sign holds
     * 30 degrees numbered from zero, so the 30th degree is 29°. The corpus already stores
     * these under the _30 keys, which is why nothing about the data needs to move.
     */
    public static DegreeStatus degreeStatus(double longitude) {
        double d = degreeInSign(longitude);
        if (d >= 29.0 && d < 30.0) {
            return DegreeStatus.ANARETIC;
        }
        if (d >= 0.0 && d < 1.0) {
            return DegreeStatus.INITIATION;
        }
        return DegreeStatus.STANDARD;
    }

    /** True only inside 29°00'00" to 29°59'59". See {@link #degreeStatus}. */
    public static boolean isAnaretic(double longitude) {
        return degreeStatus(longitude) == DegreeStatus.ANARETIC;
    }

    /**
     * The display degree, 1 to 30, which is what the technical-degree corpus is keyed on.
     *
     * A body at 29°30' is in the 30th degree. Callers that pass {@code (int) degreeInSign}
     * are one short of the key they want, which is the arithmetic this exists to stop being
     * rewritten at each call site.
     */
    public static int ordinalDegree(double longitude) {
        int n = (int) Math.floor(degreeInSign(longitude)) + 1;
        return n < 1 ? 1 : n > 30 ? 30 : n;
    }

    /**
     * Sabian degree ordinal within the sign, 1 to 30.
     *
     * The nth Sabian degree covers longitudes (n-1) up to but not including n, which
     * is exactly what the dataset's degree_range field records: aries_1 is "0-1 Aries".
     * So a planet at 10 deg 15 min takes the 11th symbol, and so does a planet at
     * exactly 10 deg 00 min 00 sec, because 10 degrees is where the eleventh begins.
     */
    public static int sabianDegree(double longitude) {
        return (int) Math.floor(degreeInSign(longitude)) + 1;
    }

    /** Absolute Sabian ordinal, 1 to 360, matching the dataset's absolute_degree field. */
    public static int sabianAbsolute(double longitude) {
        return signIndex(longitude) * 30 + sabianDegree(longitude);
    }

    /** The InterpretationService lookup key for this longitude, e.g. "aries_26". */
    public static String sabianKey(double longitude) {
        return signName(longitude) + "_" + sabianDegree(longitude);
    }

    /** Decan number within the sign, 1 to 3. */
    public static int decan(double longitude) {
        return (int) Math.floor(degreeInSign(longitude) / 10.0) + 1;
    }

    /** Decan ordinal across the whole zodiac, 0 to 35, counting from the first decan of Aries. */
    public static int decanIndex(double longitude) {
        return signIndex(longitude) * 3 + decan(longitude) - 1;
    }

    /** Traditional (Chaldean order) decan ruler, also called the face ruler. */
    public static String chaldeanDecanRuler(double longitude) {
        return CHALDEAN_CYCLE[decanIndex(longitude) % CHALDEAN_CYCLE.length];
    }

    /**
     * Sign index for a lowercase or capitalised sign name, or -1 if unrecognised.
     *
     * Returns -1 rather than throwing because the GUI reaches these lookups from inside
     * render paths, where an exception would blank the panel. Callers label conditionally.
     */
    public static int signIndexOf(String signName) {
        if (signName == null) {
            return -1;
        }
        String want = signName.toLowerCase();
        for (int i = 0; i < SIGNS.length; i++) {
            if (SIGNS[i].equals(want)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Chaldean face ruler for a decan named the way the interpretation data keys it,
     * e.g. ("Aries", 3) gives Venus. Empty string if the sign or ordinal is unrecognised.
     */
    public static String chaldeanDecanRuler(String signName, int decanNum) {
        int s = signIndexOf(signName);
        if (s < 0 || decanNum < 1 || decanNum > 3) {
            return "";
        }
        return CHALDEAN_CYCLE[(s * 3 + decanNum - 1) % CHALDEAN_CYCLE.length];
    }

    /**
     * The sign a decan borrows from under the triplicity scheme: the decan'th sign of
     * the same element, counting from the sign itself. Aries III borrows Sagittarius.
     */
    public static int triplicityDecanSignIndex(int signIndex, int decanNum) {
        return (signIndex + 4 * (decanNum - 1)) % 12;
    }

    /**
     * The four classical elements, in the order the zodiac cycles them. Aries is fire,
     * Taurus earth, Gemini air, Cancer water, and every fourth sign from there repeats.
     */
    public static final String[] ELEMENTS = { "fire", "earth", "air", "water" };

    /**
     * Element 0..3 for a sign index, or -1 if the index is out of range.
     *
     * The rule is just signIndex % 4, and it is here rather than inline at each call site
     * because it had been written out twice in the GUI and the copies had drifted: one
     * said air was gold and water blue, the other said air was blue and water purple, so
     * the same sign was coloured two different ways in one window.
     *
     * The same fourfold cycle governs house triplicity, so a house index 0..11 is a
     * legitimate argument too - houses 1, 5 and 9 are the fiery ones.
     *
     * Returns -1 rather than throwing, matching signIndexOf, because the GUI reaches this
     * from render paths where an exception would blank the panel.
     */
    public static int elementIndex(int signIndex) {
        if (signIndex < 0 || signIndex > 11) {
            return -1;
        }
        return signIndex % 4;
    }

    /** Element name for a sign index, or "" if the index is out of range. */
    public static String elementName(int signIndex) {
        int e = elementIndex(signIndex);
        return e < 0 ? "" : ELEMENTS[e];
    }

    /**
     * Cardinal, fixed, mutable, repeating every three signs from Aries.
     *
     * Lives here next to the elements because it is the same kind of fact about a sign, and
     * because {@link Gestalt} now points its own MODALITIES at this array rather than
     * declaring a second one. Two copies of a twelve-element cycle is how a chart ends up
     * with one modality in the balance and another in the aspect pattern.
     */
    public static final String[] MODALITIES = { "cardinal", "fixed", "mutable" };

    /** 0 = cardinal, 1 = fixed, 2 = mutable, or -1 if the sign index is out of range. */
    public static int modalityIndex(int signIndex) {
        if (signIndex < 0 || signIndex > 11) {
            return -1;
        }
        return signIndex % 3;
    }

    /** Modality name for a sign index, or "" if the index is out of range. */
    public static String modalityName(int signIndex) {
        int m = modalityIndex(signIndex);
        return m < 0 ? "" : MODALITIES[m];
    }

    /**
     * Triplicity sub-ruler, taken with modern rulers to match the decan prose in
     * interpretations.json. Empty string if the sign or ordinal is unrecognised.
     */
    public static String triplicityDecanRuler(String signName, int decanNum) {
        int s = signIndexOf(signName);
        if (s < 0 || decanNum < 1 || decanNum > 3) {
            return "";
        }
        return MODERN_SIGN_RULERS[triplicityDecanSignIndex(s, decanNum)];
    }

    /** Triplicity sub-ruler for a longitude. */
    public static String triplicityDecanRuler(double longitude) {
        return MODERN_SIGN_RULERS[triplicityDecanSignIndex(signIndex(longitude), decan(longitude))];
    }

    /**
     * Quadrant house 1..12 for a longitude, from a cusp array indexed 1..12.
     *
     * Pure: cusps in, category out, no ephemeris. That is why it lives here rather than
     * in L4 with the rulership web that needs it - it is the same shape as decan() and
     * sabianDegree(), and it belongs where the self-test can reach it.
     *
     * Inclusive start, exclusive end, matching every other boundary in this class: a body
     * exactly on a cusp is in the house that cusp opens, not the one it closes.
     *
     * Works by walking each house as an arc rather than comparing raw longitudes, so the
     * house containing 0 Aries needs no special case - the only place a naive
     * start <= lon < end test goes wrong, and it goes wrong silently.
     */
    public static int houseOf(double longitude, double[] cusps) {
        if (cusps == null || cusps.length < 13) {
            return 0;
        }
        double lon = normalise(longitude);
        for (int h = 1; h <= 12; h++) {
            double start = normalise(cusps[h]);
            double span = normalise(normalise(cusps[h == 12 ? 1 : h + 1]) - start);
            if (span <= 0.0) {
                continue;               // degenerate cusp pair; skip rather than divide by it
            }
            if (normalise(lon - start) < span) {
                return h;
            }
        }
        return 0;                       // unreachable for a well-formed cusp set
    }

    /**
     * Whole-sign house 1..12: the sign holding the Ascendant is the 1st, whole signs
     * thereafter. Independent of the quadrant cusps, and frequently different from them.
     *
     * L2's spec asks for both to be stored, because L3 and L6 sometimes want the other.
     */
    public static int wholeSignHouse(double longitude, double ascendant) {
        return ((signIndex(longitude) - signIndex(ascendant) + 12) % 12) + 1;
    }

    /**
     * House N counted from house M, as L6's relational topics need: the 4th of the 7th
     * is the partner's home. Both arguments 1..12.
     */
    public static int derivedHouse(int n, int m) {
        return ((n + m - 2) % 12) + 1;
    }

    /**
     * "1st", "2nd", "12th" - for prose that names a house or a year.
     *
     * Lives here because it was written three times otherwise. Snapshot and Convergence
     * each carried a private, byte-for-byte identical copy, and BodyScore's joy reason
     * wanted a third. Nothing had drifted yet, but a fact with three spellings is the
     * shape every defect in the work plan's recurring-defect section starts as.
     */
    public static String ordinal(int n) {
        switch (n) {
            case 1:  return "1st";
            case 2:  return "2nd";
            case 3:  return "3rd";
            case 21: return "21st";
            case 22: return "22nd";
            case 23: return "23rd";
            default: return n + "th";
        }
    }

    /**
     * Builds a longitude from a sign index and degrees, minutes and seconds within
     * that sign. Test helper, and the natural shape for parsing user-entered positions.
     */
    public static double longitudeOf(int signIndex, int deg, int min, double sec) {
        return signIndex * 30.0 + deg + min / 60.0 + sec / 3600.0;
    }

    /**
     * Formats for display, e.g. 25 deg 40 min Aries.
     *
     * Note this truncates rather than rounds, deliberately. Rounding 29 deg 59 min 59.6 sec
     * up to "30 deg 00 min" would display a position in a sign it is not actually in, and
     * would disagree with the Sabian degree shown beside it.
     *
     * This file is UTF-8 without a BOM and contains a literal degree sign below, so it
     * must be compiled with -encoding UTF-8. See ourania-build-and-run.
     */
    public static String format(double longitude) {
        double d = degreeInSign(longitude);
        int deg = (int) Math.floor(d);
        int min = (int) Math.floor((d - deg) * 60.0);
        String name = signName(longitude);
        String display = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return String.format("%d°%02d' %s", deg, min, display);
    }
}
