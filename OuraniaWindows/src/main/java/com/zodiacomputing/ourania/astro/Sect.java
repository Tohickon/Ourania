package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * L3 axis 3: sect and valence.
 *
 * Sect is one boolean - is the Sun above the horizon - and it reassigns which benefic
 * does the most good, which malefic does the least harm, which triplicity ruler applies,
 * and the formulas for both Lots. Nothing else in the model propagates as far from as
 * little input, which is why this is built first.
 *
 * Body names match the strings ChartFrame produces.
 */
public final class Sect {

    /** Whether a body belongs to the chart's own sect. */
    public enum Membership { OF_SECT, CONTRARY_TO_SECT, NEUTRAL }

    /** What the body is doing for this chart, given its nature and its sect membership. */
    public enum Role {
        SECT_LIGHT,        // Sun by day, Moon by night
        CONTRARY_LIGHT,    // Sun by night, Moon by day
        BENEFIC_OF_SECT,   // Jupiter by day, Venus by night - does the most good
        BENEFIC_CONTRARY,  // Venus by day, Jupiter by night - still a benefic
        MALEFIC_OF_SECT,   // Saturn by day, Mars by night - does the least harm
        MALEFIC_CONTRARY,  // Mars by day, Saturn by night - the loudest difficulty
        NEUTRAL            // Mercury
    }

    /**
     * Valence is deliberately narrow: -2 to +2. It answers "does this body's action tend
     * to help or hurt", and nothing else. How dignified it is and how loud it is are
     * separate axes and must not be folded in here.
     */
    public static final class Valence {
        public final String body;
        public final Membership membership;
        public final Role role;
        public final int score;
        public final List<String> reasons;

        Valence(String body, Membership membership, Role role, int score, List<String> reasons) {
            this.body = body;
            this.membership = membership;
            this.role = role;
            this.score = score;
            this.reasons = reasons;
        }

        @Override
        public String toString() {
            return String.format("%-11s %-17s %-17s %+d  %s",
                body, role, membership, score, String.join("; ", reasons));
        }
    }

    private Sect() { }

    // ------------------------------------------------------------------ determination

    /**
     * True when the Sun is above the horizon.
     *
     * Houses 7 through 12 are above the horizon, and they occupy the arc more than 180
     * degrees counterclockwise from the Ascendant. A Sun just past sunrise sits in the
     * 12th, at a longitude just short of the Ascendant, so its arc from the Ascendant is
     * just under 360 - comfortably over 180. A Sun at the IC, i.e. local midnight, is 90
     * degrees past the Ascendant and correctly reads as under 180.
     */
    public static boolean isDiurnal(double sunLon, double ascLon) {
        return Zodiac.normalise(sunLon - ascLon) > 180.0;
    }

    /**
     * True when the Sun is close enough to either horizon that the competing sect rules
     * would disagree. Warn rather than silently picking one: flipping sect rewrites every
     * condition score in the chart, so this is a case the user should be told about.
     */
    public static boolean isBorderline(double sunLon, double ascLon, double warnDegrees) {
        double arc = Zodiac.normalise(sunLon - ascLon);
        double toAsc = Math.min(arc, 360.0 - arc);
        double toDsc = Math.abs(arc - 180.0);
        return Math.min(toAsc, toDsc) <= warnDegrees;
    }

    // -------------------------------------------------------------------- the two lots
    //
    // Both lots reverse by sect, and Spirit is Fortune's mirror across the Ascendant. The
    // rule lived inline in ChartFrame.deriveAll, which was fine while L1 was the only
    // caller; the wheel now draws the Part of Fortune as a selectable point and needs the
    // same answer. Two copies of a formula that reverses on a condition is the shape of
    // defect where the copies agree for every day chart and differ for every night one.

    /**
     * The Lot of Fortune: the arc from Sun to Moon, projected from the Ascendant, and
     * reversed at night.
     */
    public static double lotOfFortune(boolean diurnal, double ascLon,
                                     double sunLon, double moonLon) {
        return diurnal
            ? Zodiac.normalise(ascLon + moonLon - sunLon)
            : Zodiac.normalise(ascLon + sunLon - moonLon);
    }

    /** The Lot of Spirit: Fortune with the luminaries exchanged. */
    public static double lotOfSpirit(boolean diurnal, double ascLon,
                                     double sunLon, double moonLon) {
        return lotOfFortune(!diurnal, ascLon, sunLon, moonLon);
    }

    /**
     * Mercury takes its sect from its solar phase rather than its nature. It is oriental
     * - a morning star, rising before the Sun - when it precedes the Sun in zodiacal
     * order, and occidental when it follows. Oriental belongs to the day, occidental to
     * the night.
     */
    public static boolean isOriental(double bodyLon, double sunLon) {
        return Zodiac.normalise(bodyLon - sunLon) > 180.0;
    }

    /** Which side of the Sun a body sits on, or that the question does not apply. */
    public enum Phase { ORIENTAL, OCCIDENTAL, NOT_APPLICABLE }

    /**
     * The solar phase as a typed value, for the five bodies it means anything for.
     *
     * This is the MEASUREMENT only - which side of the Sun the body is on. Whether that
     * side is the fortunate one is a condition judgement and depends on the planet, so it
     * lives in BodyScore with the other condition rules. The same split as angularity:
     * the distance is measured here, what it is worth is decided there.
     *
     * NOT_APPLICABLE rather than a default of OCCIDENTAL, so a caller cannot read "not
     * oriental" as a positive claim about the Sun. The Sun has no phase relative to
     * itself; the Moon's classical analogue is increasing or decreasing light, which is a
     * different measurement and is deliberately not squeezed under this name; and the
     * modern bodies are outside the tradition this rule comes from.
     *
     * Delegates to isOriental rather than restating the comparison. Mercury's sect in
     * evaluate() below goes through the same function, so there is one definition of
     * which side of the Sun is which, not two that can drift apart.
     */
    public static Phase phaseOf(String body, double bodyLon, double sunLon) {
        switch (body) {
            case "Saturn":
            case "Jupiter":
            case "Mars":
            case "Venus":
            case "Mercury":
                return isOriental(bodyLon, sunLon) ? Phase.ORIENTAL : Phase.OCCIDENTAL;
            default:
                return Phase.NOT_APPLICABLE;
        }
    }

    // ------------------------------------------------------------------ valence

    /**
     * @param sunLon needed only for Mercury, whose sect depends on its solar phase
     */
    public static Valence evaluate(String body, boolean diurnal, double bodyLon, double sunLon) {
        List<String> reasons = new ArrayList<>();
        reasons.add(diurnal ? "day chart" : "night chart");

        Membership membership;
        Role role;
        int score;

        switch (body) {
            case "Sun":
                membership = diurnal ? Membership.OF_SECT : Membership.CONTRARY_TO_SECT;
                role = diurnal ? Role.SECT_LIGHT : Role.CONTRARY_LIGHT;
                score = 0;
                reasons.add(diurnal ? "the sect light" : "the light out of sect");
                break;

            case "Moon":
                membership = diurnal ? Membership.CONTRARY_TO_SECT : Membership.OF_SECT;
                role = diurnal ? Role.CONTRARY_LIGHT : Role.SECT_LIGHT;
                score = 0;
                reasons.add(diurnal ? "the light out of sect" : "the sect light");
                break;

            case "Jupiter":
                membership = diurnal ? Membership.OF_SECT : Membership.CONTRARY_TO_SECT;
                role = diurnal ? Role.BENEFIC_OF_SECT : Role.BENEFIC_CONTRARY;
                score = diurnal ? 2 : 1;
                reasons.add(diurnal
                    ? "benefic of sect - does the most good in this chart"
                    : "benefic contrary to sect - still helpful, less effective");
                break;

            case "Venus":
                membership = diurnal ? Membership.CONTRARY_TO_SECT : Membership.OF_SECT;
                role = diurnal ? Role.BENEFIC_CONTRARY : Role.BENEFIC_OF_SECT;
                score = diurnal ? 1 : 2;
                reasons.add(diurnal
                    ? "benefic contrary to sect - still helpful, less effective"
                    : "benefic of sect - does the most good in this chart");
                break;

            case "Saturn":
                membership = diurnal ? Membership.OF_SECT : Membership.CONTRARY_TO_SECT;
                role = diurnal ? Role.MALEFIC_OF_SECT : Role.MALEFIC_CONTRARY;
                score = diurnal ? -1 : -2;
                reasons.add(diurnal
                    ? "malefic of sect - does the least harm it can"
                    : "malefic contrary to sect - the loudest difficulty in this chart");
                break;

            case "Mars":
                membership = diurnal ? Membership.CONTRARY_TO_SECT : Membership.OF_SECT;
                role = diurnal ? Role.MALEFIC_CONTRARY : Role.MALEFIC_OF_SECT;
                score = diurnal ? -2 : -1;
                reasons.add(diurnal
                    ? "malefic contrary to sect - the loudest difficulty in this chart"
                    : "malefic of sect - does the least harm it can");
                break;

            case "Mercury": {
                boolean oriental = isOriental(bodyLon, sunLon);
                boolean ofSect = (diurnal == oriental);
                membership = ofSect ? Membership.OF_SECT : Membership.CONTRARY_TO_SECT;
                role = Role.NEUTRAL;
                score = 0;
                reasons.add(oriental ? "oriental - a morning star" : "occidental - an evening star");
                reasons.add(ofSect ? "which puts it in sect" : "which puts it out of sect");
                break;
            }

            default:
                // Bodies outside the traditional seven have no sect. Saying so is more
                // useful than inventing a value for them.
                return new Valence(body, Membership.NEUTRAL, Role.NEUTRAL, 0,
                    List.of("outside the traditional seven - sect does not apply"));
        }

        return new Valence(body, membership, role, score, reasons);
    }

    /** Convenience: the body that is the chart's loudest structural difficulty. */
    public static String outOfSectMalefic(boolean diurnal) {
        return diurnal ? "Mars" : "Saturn";
    }

    /** Convenience: the body that helps most. */
    public static String beneficOfSect(boolean diurnal) {
        return diurnal ? "Jupiter" : "Venus";
    }

    /** Convenience: the luminary that rules the chart's sect. */
    public static String sectLight(boolean diurnal) {
        return diurnal ? "Sun" : "Moon";
    }
}
