package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

/**
 * K13, stage 2: who the chart is about, and whether it is fit to judge.
 *
 * <h3>The two questions horary asks before it asks anything else</h3>
 *
 * <p><b>Who stands for whom.</b> A horary chart is read through significators rather than through
 * placements: the querent is the Ascendant, its domicile ruler and the Moon; the quesited is the
 * ruler of the house of the matter. Everything after this - perfection, translation, collection,
 * prohibition - is about what those two significators do to each other, so naming them wrongly
 * makes every later judgement wrong in a way that still reads as a judgement.
 *
 * <p><b>Whether to judge at all.</b> David's decision (19 September) names the planetary-hour
 * test: the hour ruler should share the Ascendant ruler's nature or triplicity. A chart that
 * fails it is held not radical - not fit to be read - and the tradition's point is that this is
 * decided <i>before</i> the chart is interpreted, not after an answer is found to be unwelcome.
 *
 * <h3>What "nature or triplicity" is taken to mean</h3>
 *
 * <p>Read as: the hour ruler <b>is</b> the Ascendant's ruler, or it is a triplicity ruler of the
 * Ascendant's sign by the chart's own sect. The first is the strong form - the same planet
 * governs the hour and the rising sign - and the second is the tradition's usual relaxation,
 * since triplicity is the dignity that speaks about sympathy of nature rather than about
 * ownership. Both are reported separately, so a reader can see which one a chart passed on.
 *
 * <h3>What this deliberately does not do</h3>
 *
 * <p>The other radicality caveats - an Ascendant in the first three or last three degrees, the
 * Moon void of course, Saturn in the seventh - are <b>not</b> here. The decision names the
 * planetary-hour test and this builds that; the rest are real and would each need their own
 * source and their own argument about severity. The void Moon already exists elsewhere in the
 * engine and belongs to stage 3, where it is about the question's outcome rather than its
 * fitness.
 */
public final class Horary {

    private Horary() { }

    /**
     * The houses a horary question can be asked of, from David's decision.
     *
     * <p>Seven rather than twelve, because these are the ones the decision names - and a list
     * that quietly grew to twelve would be claiming a coverage nobody has checked. The eighth,
     * ninth, eleventh and twelfth take questions too; they arrive when someone writes down what
     * they mean here.
     */
    public enum Matter {
        MONEY(2, "money, and lost things"),
        COMMUNICATION(3, "communications, and siblings"),
        HOME(4, "home, and property"),
        ROMANCE(5, "romance, children, speculation"),
        HEALTH(6, "health, work, service"),
        PARTNERS(7, "partners, relationships, lawsuits"),
        CAREER(10, "career, and status");

        public final int house;
        public final String about;

        Matter(int house, String about) {
            this.house = house;
            this.about = about;
        }
    }

    /** Who stands for whom in one chart, and why. */
    public static final class Significators {
        /** The rising sign, 0 to 11. */
        public int ascendantSign;
        /** The querent's own significator: the domicile ruler of the Ascendant. */
        public String querent;
        /** The querent's co-significator, always. */
        public String moon = "Moon";
        public Matter matter;
        /** The sign on the cusp of the house of the matter. */
        public int quesitedSign;
        /** The quesited's significator: the domicile ruler of that house. */
        public String quesited;

        /**
         * True when one planet stands for both sides.
         *
         * <b>Not an error, and worth saying out loud.</b> It happens whenever the two houses
         * carry signs of the same ruler - the second and the seventh both on Venus's signs, say
         * - and the tradition reads it as the two parties already being of one mind, which is a
         * finding rather than a failure of the method. It also means the perfection tests in
         * stage 3 have nothing to look for between them, so whatever reads this has to know.
         */
        public boolean shared() {
            return this.querent != null && this.querent.equals(this.quesited);
        }

        @Override
        public String toString() {
            return String.format("querent %s (%s rising) with the Moon; %s for %s (%s)",
                querent, Zodiac.SIGNS[ascendantSign], quesited, matter.about,
                Zodiac.SIGNS[quesitedSign]);
        }
    }

    /** Whether the chart is fit to judge, and on what grounds. */
    public static final class Radicality {
        public String hourRuler;
        public String ascendantRuler;
        public String triplicityRuler;
        /** The hour ruler is the Ascendant's own ruler - the strong form. */
        public boolean sameRuler;
        /** The hour ruler is a triplicity ruler of the rising sign - the usual relaxation. */
        public boolean sharesTriplicity;
        /** True when the hours could not be computed at all: no sunrise, so no hour ruler. */
        public boolean unknown;

        public boolean radical() {
            return this.sameRuler || this.sharesTriplicity;
        }

        @Override
        public String toString() {
            if (this.unknown) {
                return "not judged: the Sun neither rises nor sets here, so there is no hour ruler";
            }
            if (this.sameRuler) {
                return "radical: the hour and the Ascendant are both ruled by " + hourRuler;
            }
            if (this.sharesTriplicity) {
                return "radical: the hour ruler " + hourRuler
                    + " is a triplicity ruler of the rising sign";
            }
            return "not radical: the hour ruler " + hourRuler
                + " shares neither nature nor triplicity with " + ascendantRuler;
        }
    }

    /** Who stands for whom, given a chart and the house the question belongs to. */
    public static Significators significators(ChartFrame f, Matter matter) {
        Significators s = new Significators();
        s.matter = matter;
        s.ascendantSign = Zodiac.signIndex(f.asc);
        s.querent = Dignity.domicileRulerOf(s.ascendantSign);
        s.quesitedSign = Zodiac.signIndex(f.cusps[matter.house]);
        s.quesited = Dignity.domicileRulerOf(s.quesitedSign);
        return s;
    }

    /**
     * The planetary-hour test.
     *
     * <p><b>By the chart's own sect</b>, because triplicity rulership is. A day chart and a night
     * chart with the same rising sign do not have the same triplicity ruler, so asking the
     * question without the sect would answer it for whichever half of the tradition was written
     * down first.
     *
     * @param jd the moment the question was asked, Julian day UT
     */
    public static Radicality radicality(SwissEph sw, ChartFrame f, double jd,
                                        double lat, double lon) {
        Radicality r = new Radicality();
        int sign = Zodiac.signIndex(f.asc);
        r.ascendantRuler = Dignity.domicileRulerOf(sign);
        r.hourRuler = PlanetaryHours.rulerAt(sw, jd, lat, lon);
        if (r.hourRuler.isEmpty()) {
            r.unknown = true;
            return r;
        }
        ChartFrame.Body sun = f.body("Sun");
        if (sun == null || !sun.ok) {
            r.unknown = true;               // no sect, so no triplicity ruler to test against
            return r;
        }
        r.triplicityRuler = Dignity.triplicityRulerOf(sign, Sect.isDiurnal(sun.lon, f.asc));
        r.sameRuler = r.hourRuler.equals(r.ascendantRuler);
        r.sharesTriplicity = r.hourRuler.equals(r.triplicityRuler);
        return r;
    }
}
