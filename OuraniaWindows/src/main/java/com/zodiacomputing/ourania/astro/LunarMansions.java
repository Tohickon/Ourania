package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * The 28 tropical lunar mansions, after al-Biruni.
 *
 * The solar zodiac divides the circle by the Sun's year; this divides it by the Moon's
 * sidereal month of 27.32166 days, into 28 stations of roughly one day's travel each. Where
 * the signs track the season, the mansions track the daily tide.
 *
 * <b>Tropical and equal, not sidereal.</b> The original mansions are star-based and therefore
 * unequal - Al-Thurayya at the Pleiades, Aldebaran at the Bull's eye, Al-Kalb at the heart of
 * the Scorpion. Traditional Western practice transposes them onto the tropical zodiac as 28
 * equal sectors from 0 Aries, each 360/28 = 12 degrees 51 minutes 25.7 seconds. That is what
 * this computes, and it is the version al-Biruni's cusp table gives.
 *
 * <b>The mansions are electional and horary machinery, not natal character.</b> Western
 * tradition does not read them for personality; it reads them to choose a moment. The "good
 * for" and "avoid" lines below are that tradition, verbatim in substance, and they are the
 * reason the feature exists at all.
 *
 * Source: David, 2026-08-23, from al-Biruni's tables. <b>Two versions of the table were
 * supplied and they disagreed on one row</b>: the first named mansion 13 (Alhaire) "the
 * covering" and an astronomical dead zone, which is mansion 15 (Agrapha) in both of its own
 * cusp tables. The second, fuller version is used here and the first is treated as a
 * conflation of the two rows. Flagged rather than silently resolved.
 */
public final class LunarMansions {

    /** 360 / 28 degrees: 12 degrees, 51 minutes, 25.714 seconds. */
    public static final double WIDTH = 360.0 / 28.0;

    public static final int COUNT = 28;

    /** One station of the Moon. */
    public static final class Mansion {
        /** 1 to 28. */
        public final int number;
        /** Al-Biruni's name, in the commonest Latin transliteration. */
        public final String name;
        /** The alternative transliteration the sources also carry, or null. */
        public final String alsoKnownAs;
        /** What the name means. */
        public final String translation;
        /** Where it starts, in tropical longitude. */
        public final double start;
        /** What the tradition elects this mansion for. Never null; may be empty. */
        public final String goodFor;
        /** What the tradition warns against here. Never null; may be empty. */
        public final String avoid;

        Mansion(int number, String name, String alsoKnownAs, String translation,
                String goodFor, String avoid) {
            this.number = number;
            this.name = name;
            this.alsoKnownAs = alsoKnownAs;
            this.translation = translation;
            this.start = (number - 1) * WIDTH;
            this.goodFor = goodFor;
            this.avoid = avoid;
        }

        /** Where it ends. Mansion 28 ends at 360, which is 0 Aries again. */
        public double end() {
            return start + WIDTH;
        }

        /** "12 Aries 51" - the cusp as the traditional tables print it. */
        public String cusp() {
            return Zodiac.format(start);
        }

        @Override
        public String toString() {
            return number + " " + name + " (" + translation + "), " + cusp();
        }
    }

    private static final Mansion[] ALL = {
        new Mansion(1, "Alnath", "Al Sharatan", "The Horns of the Ram",
            "raw beginnings - new journeys, independent ventures, swift protective action", ""),
        new Mansion(2, "Allothaim", "Albochan", "The Belly of the Ram",
            "planting, starting construction, securing material foundations; business, travel "
            + "and marriage", ""),
        new Mansion(3, "Achaomazon", "Althoray", "The Pleiades",
            "planting crops and sawing wood",
            "weddings, and long journeys by water"),
        new Mansion(4, "Aldebaram", null, "The Eye of the Bull",
            "commercial agreements, business, travel, and solemnising marriages", ""),
        new Mansion(5, "Alchatay", "Albachay", "A White Spot",
            "beginning necessary conflict or military action",
            "sawing wood, or any major constructive enterprise"),
        new Mansion(6, "Alhanna", "Alchaya", "A Brand or Mark",
            "ploughing and sawing lumber",
            "long travel"),
        new Mansion(7, "Aldimiach", "Alarzach", "The Forearm",
            "setting off on journeys, travel, and giving or taking medicine", ""),
        new Mansion(8, "Alnaza", "Anatchtraya", "The Gap or Crib, at the Nebula",
            "sea travel, navigation and maritime work", ""),
        new Mansion(9, "Archeam", "Arcaph", "The Glance of the Lion's Eye",
            "planting, building and marriage",
            "setting out on long travels"),
        new Mansion(10, "Algellocho", "Algbebh", "The Forehead of the Lion",
            "sawing wood, planting, and releasing prisoners",
            "giving or taking purgative remedies"),
        new Mansion(11, "Azobra", "Ardurf", "The Mane of the Lion",
            "planting and entering marriage",
            "setting sail, and navigating water"),
        new Mansion(12, "Alzarpha", "Azarpha", "The Changer",
            "travel, sea navigation, sawing, ploughing, marriage, and sending messengers", ""),
        new Mansion(13, "Alhaire", null, "The Barker, at the Dog Star",
            "sawing wood, planting, and preparing or taking medicine",
            "travel, and solemnising marriage"),
        new Mansion(14, "Achurethor", null, "The Unarmed, at the ear of corn (Spica)",
            "excavation, digging and agricultural preparation",
            "marriage, and setting off on journeys"),
        new Mansion(15, "Agrapha", "Algarpha", "The Covering",
            "",
            "everything - the tradition treats this station as a void and warns against "
            + "starting any major initiative while the Moon is in it"),
        new Mansion(16, "Azubone", "Ahubene", "The Claws of Scorpio",
            "acquiring, trading or buying cattle and livestock",
            "navigation and sea travel"),
        new Mansion(17, "Alchil", null, "The Crown of the Forehead",
            "building, sawing wood, planting, and sea navigation",
            "entering marriage contracts"),
        new Mansion(18, "Alchas", "Altob", "The Heart of Scorpio, at Antares",
            "planting, sawing wood, travelling, and beginning necessary conflict", ""),
        new Mansion(19, "Allatha", "Achala", "The Sting of Scorpio",
            "buying livestock and cattle, and hunting",
            "solemnising marriage"),
        new Mansion(20, "Abnahaya", null, "The Ostrich, at the Beam",
            "constructing buildings, laying foundations, and asking favours of authority",
            "marriage"),
        new Mansion(21, "Abeda", "Albeldach", "The Desert, or the City",
            "taking medicine, navigating water, and buying or putting on new clothing", ""),
        new Mansion(22, "Sadahacha", "Zodeboluch", "The Shepherd",
            "self-discipline, settling boundary disputes, and slow long-term work that needs "
            + "concentration", ""),
        new Mansion(23, "Zabadola", "Zobrach", "The Swallower",
            "cleansing and detoxification, ending unfavourable relationships, and dissolving "
            + "partnerships", ""),
        new Mansion(24, "Sadabath", "Chadozoad", "The Star of Fortune",
            "social gatherings, community reform, and cooperative or humanitarian projects", ""),
        new Mansion(25, "Sadalbracha", "Sadalachia", "The Butterfly, the Star of Hidden Things",
            "private research, keeping secrets, investigation, and work behind the scenes", ""),
        new Mansion(26, "Alpharg", "Phragol Mocaden", "The First Spout",
            "the free flow of ideas, poetry, and beginning irrigation or plumbing", ""),
        new Mansion(27, "Alcharya", "Alhalgalmoad", "The Second Spout",
            "spiritual practice, non-linear artistic work, and charitable or volunteer work", ""),
        new Mansion(28, "Albotham", "Alchalcy", "The Fishes, at the Belly of the Fish",
            "bringing long projects to a peaceful close, finalising contracts, and wrapping up "
            + "cycles", ""),
    };

    private LunarMansions() { }

    /**
     * The mansion holding this longitude. Never null: the 28 sectors partition the circle.
     *
     * <b>Dividing by WIDTH is not enough, and the check caught it.</b> 360/28 has no exact
     * binary representation, so for mansions 4, 7, 13 and 25 the cusp longitude divided by
     * the width came out a hair under the whole number - 2.9999999999999996 rather than 3 -
     * and a body standing exactly on the cusp was filed in the previous station. The division
     * gives the starting guess and the loops below settle it against the same cusp values the
     * mansions were built from, so the answer is exact by construction rather than to within
     * a rounding error.
     */
    public static Mansion at(double longitude) {
        double lon = Zodiac.normalise(longitude);
        int index = (int) Math.floor(lon / WIDTH);
        if (index < 0) {
            index = 0;
        }
        if (index >= COUNT) {
            index = COUNT - 1;
        }
        while (index + 1 < COUNT && lon >= ALL[index + 1].start) {
            index++;
        }
        while (index > 0 && lon < ALL[index].start) {
            index--;
        }
        return ALL[index];
    }

    /** The mansion with this number, 1 to 28. */
    public static Mansion byNumber(int number) {
        if (number < 1 || number > COUNT) {
            throw new IllegalArgumentException("mansion number out of range: " + number);
        }
        return ALL[number - 1];
    }

    /** All 28, in order. */
    public static List<Mansion> all() {
        return new ArrayList<>(java.util.Arrays.asList(ALL));
    }

    /**
     * The Moon's mansion in a chart - the one the tradition actually reads.
     *
     * Any body can be placed in a mansion and {@link #at} will do it, but the mansions are
     * the Moon's stations: they are where she is on a given night, and an electional reading
     * that placed Saturn in a mansion would be using the frame for something it does not
     * describe.
     */
    public static Mansion ofMoon(ChartFrame f) {
        ChartFrame.Body moon = f.body("Moon");
        return moon == null || !moon.ok ? null : at(moon.lon);
    }

    /**
     * A one-line electional summary for a mansion, complete for every station.
     *
     * Both clauses are optional in the sources and several mansions carry only one, so the
     * sentence is assembled rather than templated - a station with nothing to avoid must not
     * read "avoid ." LunarMansionCheck asserts that all 28 produce a whole sentence.
     */
    public static String electionalLine(Mansion m) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mansion ").append(m.number).append(", ").append(m.name)
          .append(" - ").append(m.translation).append('.');
        if (!m.goodFor.isEmpty()) {
            sb.append(" Elected for ").append(m.goodFor).append('.');
        }
        if (!m.avoid.isEmpty()) {
            sb.append(" Avoid ").append(m.avoid).append('.');
        }
        return sb.toString();
    }
}
