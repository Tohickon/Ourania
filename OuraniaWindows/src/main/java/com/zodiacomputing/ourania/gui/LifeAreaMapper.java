package com.zodiacomputing.ourania.gui;

/**
 * The twelve houses sorted into seven parts of life (reading framework spec, 2026-10-24).
 *
 * <b>Each house is in exactly one domain, and each domain has at least one house.</b> The spec
 * names seven - Love and Relationships, Career and Public Life, Home and Family, Money and
 * Resources, Mind and Communication, Health and Body, Growth and Spirituality - and says to map
 * "each placement's ruling house(s)" into them. A placement has one house, so one house gets
 * one domain; the spec gives no table, so this one is the conventional modern reading, and where
 * the spec's own words could take a house two ways the choice is written beside it:
 * <ul>
 * <li>the 6th (daily work and health) is under Health and Body, since Career has the 10th;</li>
 * <li>the 8th (shared money and intimacy) is under Money and Resources, since the 7th and 5th
 *     are already the relationships;</li>
 * <li>the 11th (friends, groups, hopes) is under Career and Public Life, as the public side of
 *     a life, and the 12th (rest, solitude) under Growth and Spirituality.</li>
 * </ul>
 * The wheel's house click and the Life areas page both ask this, and nothing else decides it.
 */
final class LifeAreaMapper {

    /** The domains, in the spec's order. */
    enum Domain {
        LOVE("Love & Relationships", 7, 5),
        CAREER("Career & Public Life", 10, 11),
        HOME("Home & Family", 4),
        MONEY("Money & Resources", 2, 8),
        MIND("Mind & Communication", 3),
        HEALTH("Health & Body", 1, 6),
        GROWTH("Growth & Spirituality", 9, 12);

        /** What the page says over the domain's cards. */
        final String label;
        private final int[] houses;

        Domain(String label, int... houses) {
            this.label = label;
            this.houses = houses;
        }

        /** The houses under this domain, in the order its cards are shown. */
        int[] houses() {
            return houses.clone();
        }
    }

    private LifeAreaMapper() {
    }

    /** The domain a house belongs to, or null outside 1 to 12. */
    static Domain of(int house) {
        for (Domain d : Domain.values()) {
            for (int h : d.houses) {
                if (h == house) {
                    return d;
                }
            }
        }
        return null;
    }
}
