package com.zodiacomputing.ourania.astro;

/**
 * How well a birth time is known - the Rodden rating, as astrologers actually cite it.
 *
 * <b>A chart is only as good as its time, and nothing in this app said how good that was.</b>
 * A time typed from a birth certificate and a time somebody half-remembers produce charts that
 * look exactly alike: same Ascendant to the arcminute, same house cusps, same confident
 * placements. The difference is entirely in what the reader is entitled to conclude, and it
 * was not recorded anywhere - so a rectified guess, once saved, became indistinguishable from
 * a document.
 *
 * The scale is Lois Rodden's, used by AstroDatabank and cited in the literature by these
 * letters. Keeping her letters rather than inventing "high/medium/low" means a rating carried
 * in from another source, or quoted out to one, means the same thing at both ends.
 *
 * <b>{@link #X} is the one with teeth.</b> The others are provenance the reader weighs; X says
 * there is no time at all, and the chart is cast for noon with its angles withheld - see
 * {@code ChartFrame.computeTimeUnknown}. That is the difference between a rating that is a
 * label and one that changes the chart.
 */
public enum Rodden {

    /** Birth certificate, hospital record, or the family bible: a document. */
    AA("AA", "Recorded", "From a birth certificate, hospital record or other document."),

    /** From the person, a family member, or a news report made at the time. */
    A("A", "From memory", "Given by the person or their family, or reported at the time."),

    /** From a biography or autobiography - one remove from the source. */
    B("B", "Biography", "From a biography or autobiography rather than a record."),

    /** In circulation with no stated source. Read the angles with that in mind. */
    C("C", "Uncertain", "In circulation with no source given. Treat the angles as approximate."),

    /** Two or more sources that disagree. The chart is one of several candidates. */
    DD("DD", "Conflicting", "Sources disagree. This is one candidate chart among several."),

    /** Rectified: a time worked backwards from events rather than observed. */
    R("R", "Rectified", "Worked backwards from events rather than observed. The angles are "
        + "an argument, not a measurement."),

    /**
     * No birth time at all.
     *
     * <b>The only rating that changes the calculation.</b> The chart is cast for noon and its
     * angles, houses and Lots are withheld rather than guessed - the planets barely move in a
     * day, but the Ascendant crosses the whole zodiac.
     */
    X("X", "No time", "No birth time known. Cast for noon; angles and houses withheld.");

    /** The letter as it is cited - "AA", "DD", "X". */
    public final String code;
    /** A short label for a control, in plain words. */
    public final String label;
    /** One sentence a reader can act on. */
    public final String meaning;

    Rodden(String code, String label, String meaning) {
        this.code = code;
        this.label = label;
        this.meaning = meaning;
    }

    /** True when this rating means there is no time to cast from. */
    public boolean timeUnknown() {
        return this == X;
    }

    /**
     * True when the angles should be read with reservation even though a time exists.
     *
     * Rectified and conflicting times both produce a definite Ascendant that is not evidence
     * of anything; C is in circulation with nothing behind it. The chart is still cast - these
     * are readings a practitioner legitimately gives - but the card says so.
     */
    public boolean anglesUncertain() {
        return this == C || this == DD || this == R;
    }

    /**
     * The rating for a stored code, defaulting to {@link #A}.
     *
     * <b>Defaults to A rather than AA.</b> An unrated chart is one somebody typed in, and
     * claiming a document nobody cited is the one direction this must not round.
     */
    public static Rodden of(String code) {
        if (code != null) {
            String c = code.trim().toUpperCase();
            for (Rodden r : values()) {
                if (r.code.equals(c)) {
                    return r;
                }
            }
        }
        return A;
    }

    @Override
    public String toString() {
        return code + " · " + label;
    }
}
