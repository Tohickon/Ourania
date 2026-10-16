package com.zodiacomputing.ourania.gui;

/**
 * The twelve houses as the parts of life a reader would name them by (2026-10-13).
 *
 * <b>A reader who is not an astrologer does not know what "the 10th house" is,</b> but knows
 * what a career is. The plain view says where a placement shows up in a life; the house number
 * stays in Astrologer view, where it is evidence rather than the message. The phrases follow
 * the conventional modern house meanings, the same ones the house readings are written to.
 */
final class LifeAreas {

    private static final String[] AREAS = {
        "how you come across, your body and first impressions",
        "money, belongings and what you value",
        "everyday talk, siblings, neighbours and short trips",
        "home, family and roots",
        "fun, romance, creativity and children",
        "daily work, routines and health",
        "partners and one-to-one relationships",
        "shared money, intimacy and deep change",
        "travel, study and beliefs",
        "career, reputation and public standing",
        "friends, groups and hopes for the future",
        "rest, solitude and what goes on behind the scenes",
    };

    private LifeAreas() {
    }

    /** The part of life a house stands for, lower case, or null outside 1 to 12. */
    static String of(int house) {
        return house >= 1 && house <= 12 ? AREAS[house - 1] : null;
    }

    /**
     * The same part of life in another person's chart: "how they come across", not "how you".
     *
     * <b>Derived from {@link #of}, not a second table,</b> so the two can never name a house
     * differently. The Together page needs both voices - their planets in your life, and yours
     * in theirs - and only the pronouns change between them.
     */
    static String ofTheirs(int house) {
        String s = of(house);
        // Whole words only: "young" or "yourself" in a later phrase must not become "theyng".
        return s == null ? null
            : s.replaceAll("\\byour\\b", "their").replaceAll("\\byou\\b", "they");
    }
}
