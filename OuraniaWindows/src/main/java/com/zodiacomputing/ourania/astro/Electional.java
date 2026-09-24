package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

import de.thmac.swisseph.SwissEph;

/**
 * F10, the electional half: whether a moment is fit to begin something in.
 *
 * <h3>The difference from horary, which shares all of this apparatus</h3>
 *
 * <p>Horary asks what a chart <i>says</i>; election asks whether to act <i>now</i>. The same
 * significators, the same dignities, the same planetary hours - read for a different purpose. So
 * nothing here recomputes what {@link Horary} already names. The significators come from
 * {@link Horary#significators}, the hour and day rulers from {@link PlanetaryHours}, the
 * dignities from {@link Dignity}, and which aspects are soft from
 * {@link Horary#throughDifficulty}. What this class adds is the reading: which of those facts
 * raise a moment and which lower it.
 *
 * <h3>The two measures the decision names</h3>
 *
 * <p><b>The planetary day and hour, with what each favours.</b> {@code PlanetaryHours} has
 * returned both since K13 stage 1 and neither has ever carried a meaning. The ruler of the hour
 * is the traditional first question of an election - not because the hour decides the matter,
 * but because it says what kind of matter the moment is shaped for.
 *
 * <p><b>Angularity.</b> A planet on an angle acts; a planet in a cadent house does not. So the
 * benefics on angles raise a moment, and the malefics on the two angles that matter most for a
 * beginning lower it. The South Node on a significator lowers it too, which is not angularity
 * but belongs with it: all three are about what is placed where it will be felt.
 *
 * <h3>What "on an angle" is taken to mean</h3>
 *
 * <p><b>The angular houses, through {@link Zodiac#houseOf}, by the chart's own cusps.</b> Not by
 * counting signs from the Ascendant. This engine reads a quadrant house system, in which the
 * fourth house is not the fourth sign, and a house walk written here would be the second copy of
 * a rule - the defect this project logs more than any other. Where the planet is also within
 * {@link #ANGLE_ORB} of the angle itself, the sentence says so, because the tradition
 * holds that to be the strong form and the distinction is free once the number is in hand.
 *
 * <h3>Decided here, and worth overruling</h3>
 *
 * <p><b>A benefic raises the Moon only by conjunction, sextile or trine.</b> David's wording was
 * "aspecting the Moon", and a benefic square to the Moon is still a benefic - but it is not what
 * the tradition means by fortifying her, so it is not counted as raising, and neither is it
 * counted against. The test is written as the two rules that already exist,
 * {@code type.isPtolemaic()} and {@code !Horary.throughDifficulty(type)}, rather than as a third
 * list of five aspect names that could drift away from them.
 *
 * <p><b>The aspect is found at the reader's own orbs.</b> {@link Aspects#typeOf} is what answers,
 * so a reader who narrowed the trine in Settings &rsaquo; Natal Orbs has narrowed it here too.
 * That is the point of H1 and it should reach every surface, including this one.
 *
 * <p><b>{@link Standing} is an arithmetic this class invents.</b> The tradition weighs these
 * testimonies; it does not count them. More raising than lowering is reported as favoured, the
 * reverse as ill-favoured, an even split as mixed. The notes themselves are the real answer and
 * they are all reported, so a reader can disagree with the tally and still have the evidence.
 */
public final class Electional {

    private Electional() {
    }

    /**
     * How near an angle a planet must be to be called conjunct it, in degrees.
     *
     * <p>Five, which is the usual figure for an angle in electional work. A convention rather
     * than a measurement, so it is named once here rather than written into the test twice.
     */
    public static final double ANGLE_ORB = 5.0;

    /**
     * How near a significator the South Node must be to count as sitting on it, in degrees.
     *
     * <p>Three, and deliberately tighter than {@link #ANGLE_ORB}. The nodes are not bodies and
     * the tradition gives them narrow orbs; a node allowed five degrees either side would land
     * on a significator far more often than the testimony is meant to fire.
     */
    public static final double NODE_ORB = 3.0;

    /**
     * Does this aspect from a benefic fortify the Moon?
     *
     * <p>Conjunction, sextile and trine. A benefic square to the Moon is still a benefic, but it
     * is not what the tradition means by fortifying her, so it does not raise the moment - and it
     * is not counted against it either.
     *
     * <p><b>Why this is a named method and not a term inside {@code assess}.</b> Exactly the
     * reason {@link Horary#throughDifficulty} gives: inline, the rule could only be observed
     * through whichever aspects the sky happened to supply on the days a suite sampled, and a
     * mutation making the trine hard would survive any fortnight in which no benefic trined the
     * Moon. Named, it is asserted over all fifteen aspect types at once.
     *
     * <p>It is written as the two rules that already exist rather than as a third list of five
     * aspect names, so it cannot drift away from them.
     */
    public static boolean fortifies(Aspects.Type type) {
        return type != null && type.isPtolemaic() && !Horary.throughDifficulty(type);
    }

    /** The houses a planet acts from. */
    public static boolean angular(int house) {
        return house == 1 || house == 4 || house == 7 || house == 10;
    }

    /**
     * What the ruler of a planetary day or hour favours - one clause, in the reader's language.
     *
     * <p>Returns an empty string for anything that is not one of the seven, which is what
     * {@link PlanetaryHours} returns when it cannot name a ruler at all.
     */
    public static String favours(String ruler) {
        if (ruler == null) {
            return "";
        }
        switch (ruler) {
            case "Sun":
                return "matters of authority and honour, and dealings with those in office";
            case "Moon":
                return "journeys, matters that must move or change, and dealings with the public";
            case "Mars":
                return "surgery and the cutting of things, contests, and anything needing force";
            case "Mercury":
                return "writing, accounts, study, negotiation, and the sending of messages";
            case "Jupiter":
                return "contracts, law, money lent or borrowed, and the asking of a favour";
            case "Venus":
                return "marriage, friendship, art and adornment, and the making of peace";
            case "Saturn":
                return "endings, building, husbandry, and anything meant to last";
            default:
                return "";
        }
    }

    /** Which way a testimony pulls. */
    public enum Weight {
        RAISES,
        LOWERS
    }

    /** One testimony about the moment, carrying its own sentence. */
    public static final class Note {
        public Weight weight;
        /** The planet the testimony is about. */
        public String body;
        /** One sentence, in the reader's language rather than the engine's. */
        public String because;

        @Override
        public String toString() {
            return this.weight + ": " + this.because;
        }
    }

    /** The tally, which is a convention of this class - see the note on Electional. */
    public enum Standing {
        FAVOURED,
        MIXED,
        ILL_FAVOURED,
        NEUTRAL,
        /** The hours could not be computed - no sunrise, so no ruler and no reading. */
        UNKNOWN
    }

    /** A moment weighed: what it is shaped for, and what raises or lowers it. */
    public static final class Quality {
        public String dayRuler;
        public String hourRuler;
        /** What {@link #dayRuler} favours. Empty where there is no ruler. */
        public String dayFavours = "";
        /** What {@link #hourRuler} favours. Empty where there is no ruler. */
        public String hourFavours = "";
        /** True inside the polar day or night, where there is no sunrise to divide. */
        public boolean unknown;
        /** The testimonies, in the order the method found them. Never null. */
        public final List<Note> notes = new ArrayList<>();

        public int raises() {
            return count(Weight.RAISES);
        }

        public int lowers() {
            return count(Weight.LOWERS);
        }

        private int count(Weight w) {
            int n = 0;
            for (Note note : this.notes) {
                if (note.weight == w) {
                    n++;
                }
            }
            return n;
        }

        public Standing standing() {
            if (this.unknown) {
                return Standing.UNKNOWN;
            }
            int up = raises();
            int down = lowers();
            if (up == 0 && down == 0) {
                return Standing.NEUTRAL;
            }
            if (up > down) {
                return Standing.FAVOURED;
            }
            if (down > up) {
                return Standing.ILL_FAVOURED;
            }
            return Standing.MIXED;
        }

        @Override
        public String toString() {
            if (this.unknown) {
                return "the hours cannot be divided here, so the moment is not weighed";
            }
            return String.format("%s: %d raising, %d lowering; a %s hour on a %s day",
                standing(), raises(), lowers(), this.hourRuler, this.dayRuler);
        }
    }

    /**
     * Weigh a moment.
     *
     * @param matter the matter being elected for, or null. The South Node testimony needs
     *               significators and significators need a house of the matter; where there is
     *               no matter that one measure simply does not fire, rather than firing against
     *               a significator invented for the occasion.
     */
    public static Quality assess(SwissEph sw, ChartFrame f, double jd,
                                 double lat, double lon, Horary.Matter matter) {
        Quality q = new Quality();

        PlanetaryHours.Day day = PlanetaryHours.at(sw, jd, lat, lon);
        PlanetaryHours.Hour hour = day == null ? null : day.at(jd);
        if (day == null || hour == null) {
            q.unknown = true;
            return q;
        }
        q.dayRuler = day.ruler;
        q.hourRuler = hour.ruler;
        q.dayFavours = favours(q.dayRuler);
        q.hourFavours = favours(q.hourRuler);

        ChartFrame.Body sun = f.body("Sun");
        boolean diurnal = sun != null && sun.ok && Sect.isDiurnal(sun.lon, f.asc);

        // <b>The benefics.</b> On an angle they act, and aspecting the Moon they fortify the
        // significator every election carries whether or not it has a matter.
        ChartFrame.Body moon = f.body("Moon");
        for (String name : new String[] {"Jupiter", "Venus"}) {
            ChartFrame.Body b = f.body(name);
            if (b == null || !b.ok) {
                continue;
            }
            int house = Zodiac.houseOf(b.lon, f.cusps);
            if (angular(house)) {
                Note n = new Note();
                n.weight = Weight.RAISES;
                n.body = name;
                String angle = nearestAngle(f, b.lon);
                n.because = angle == null
                    ? String.format("%s is angular, in the %s house, where a planet acts",
                        name, ordinal(house))
                    : String.format("%s is angular and within %.0f degrees of the %s itself, "
                        + "which is the strong form", name, ANGLE_ORB, angle);
                q.notes.add(n);
            }
            if (moon != null && moon.ok && !name.equals("Moon")) {
                Aspects.Type t = Aspects.typeOf(Aspects.separation(b.lon, moon.lon), name, "Moon");
                if (fortifies(t)) {
                    Note n = new Note();
                    n.weight = Weight.RAISES;
                    n.body = name;
                    n.because = String.format("%s is in %s to the Moon, fortifying her",
                        name, t.label.toLowerCase());
                    q.notes.add(n);
                }
            }
        }

        // <b>The malefics, and only where they are both placed and weak.</b> Mars on the
        // Ascendant in its own sign is not the testimony this is about: the tradition's warning
        // is about a malefic that has no dignity to govern its own nature.
        for (String name : new String[] {"Mars", "Saturn"}) {
            ChartFrame.Body b = f.body(name);
            if (b == null || !b.ok) {
                continue;
            }
            String angle = null;
            if (Aspects.separation(b.lon, f.asc) <= ANGLE_ORB) {
                angle = "Ascendant";
            } else if (Aspects.separation(b.lon, f.mc) <= ANGLE_ORB) {
                angle = "Midheaven";
            }
            if (angle == null) {
                continue;
            }
            Dignity.Result d = Dignity.evaluate(name, b.lon, diurnal);
            if (d == null || d.score >= 0) {
                continue;
            }
            Note n = new Note();
            n.weight = Weight.LOWERS;
            n.body = name;
            n.because = String.format("%s is on the %s undignified, at %d for dignity, "
                + "so its nature governs the beginning", name, angle, d.score);
            q.notes.add(n);
        }

        // <b>The South Node on a significator.</b> Not angularity, but the same question: what
        // is placed where the matter will feel it.
        if (matter != null) {
            Horary.Significators sig = Horary.significators(f, matter);
            if (sig != null) {
                addNodeNote(q, f, sig.querent, "the querent's significator");
                addNodeNote(q, f, sig.quesited, "the significator of the matter");
                addNodeNote(q, f, sig.moon, "the Moon, co-significator of the querent");
            }
        }
        return q;
    }

    /** One South Node testimony, or none. Silent where the significator has no body. */
    private static void addNodeNote(Quality q, ChartFrame f, String name, String role) {
        if (name == null) {
            return;
        }
        ChartFrame.Body b = f.body(name);
        if (b == null || !b.ok) {
            return;
        }
        if (Aspects.separation(b.lon, f.southNode) > NODE_ORB) {
            return;
        }
        Note n = new Note();
        n.weight = Weight.LOWERS;
        n.body = name;
        n.because = String.format("the South Node sits on %s, %s, which drains what it touches",
            name, role);
        q.notes.add(n);
    }

    /** The angle this longitude is within {@link #ANGLE_ORB} of, or null. */
    private static String nearestAngle(ChartFrame f, double lon) {
        if (Aspects.separation(lon, f.asc) <= ANGLE_ORB) {
            return "Ascendant";
        }
        if (Aspects.separation(lon, f.mc) <= ANGLE_ORB) {
            return "Midheaven";
        }
        if (Aspects.separation(lon, f.dsc) <= ANGLE_ORB) {
            return "Descendant";
        }
        if (Aspects.separation(lon, f.ic) <= ANGLE_ORB) {
            return "Imum Coeli";
        }
        return null;
    }

    private static final String[] ORDINALS = {
        "", "first", "second", "third", "fourth", "fifth", "sixth",
        "seventh", "eighth", "ninth", "tenth", "eleventh", "twelfth"
    };

    /** The house number in words, because the sentence is prose and not a table. */
    public static String ordinal(int house) {
        return house >= 1 && house <= 12 ? ORDINALS[house] : String.valueOf(house);
    }
}
