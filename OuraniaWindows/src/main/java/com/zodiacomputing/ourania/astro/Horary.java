package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

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

        /**
         * How far into its sign the Ascendant is, in degrees. NaN before it is measured.
         *
         * <b>Carried rather than recomputed.</b> Two of the three caveats below are read off
         * this one number, and the sentence the reader is given quotes it.
         */
        public double ascendantDegree = Double.NaN;

        /**
         * The Ascendant is in the first three degrees of its sign - the matter is not ripe.
         *
         * <b>"Too early to judge."</b> Bonatti's first consideration: the question has been put
         * before the situation has taken shape, so the chart describes something that has not
         * happened yet. The degree is a convention and this project states its conventions -
         * three degrees is the usual figure and the one Lilly uses.
         */
        public boolean tooEarly;

        /**
         * The Ascendant is in the last three degrees - the matter is already settled.
         *
         * The mirror of the above, and the more commonly cited of the two: the case is past
         * deciding, so a judgement would be describing an outcome rather than predicting it.
         */
        public boolean tooLate;

        /**
         * Saturn in the seventh house, which is the astrologer's own.
         *
         * <b>The one caveat that is about the reader rather than the question.</b> The seventh
         * is the house of the astrologer in a horary chart, and Saturn there is the traditional
         * sign that the judgement will go wrong - not that the answer is unknowable, but that
         * this reader will misread it. Kept because leaving it out would be choosing which of
         * the considerations to honour, and the decision named it.
         */
        public boolean saturnInSeventh;

        /** True when any consideration says this chart should not be judged. */
        public boolean cautioned() {
            return this.tooEarly || this.tooLate || this.saturnInSeventh;
        }

        /**
         * Whether the chart may be judged at all: the hour test, and only that.
         *
         * <b>The considerations warn, they do not refuse.</b> They were folded in here for one
         * measurement and taken back out: too early, too late or Saturn in the seventh refuses
         * <b>26.7% of all moments</b> - six degrees of every thirty for the Ascendant, about a
         * house in twelve for Saturn - and David's call was that a reader who asks a question
         * should be told what is doubtful about the moment rather than handed silence for one
         * question in four. K13's decision words radicality as the planetary-hour test, and this
         * is that.
         *
         * What the considerations do instead is {@link #cautioned}, reported beside the verdict.
         */
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

        /**
         * The considerations that apply, in the reader's language, or empty.
         *
         * <b>Beside the verdict rather than instead of it.</b> These say the moment is doubtful,
         * not that it cannot be read - see {@link #radical}. A reader who is told "too late to
         * judge" and given no answer cannot tell whether the method had nothing to say or was
         * never asked; told "the answer is no, and note that the Ascendant is at 27.8 degrees",
         * they can weigh it themselves, which is what the considerations are for.
         */
        public java.util.List<String> cautions() {
            java.util.List<String> out = new java.util.ArrayList<>();
            if (this.tooEarly) {
                out.add(String.format("The Ascendant is at %.1f degrees of its sign, inside the "
                    + "first three: traditionally too early to judge, the matter not yet having "
                    + "taken shape.", this.ascendantDegree));
            }
            if (this.tooLate) {
                out.add(String.format("The Ascendant is at %.1f degrees of its sign, inside the "
                    + "last three: traditionally too late, the matter already decided.",
                    this.ascendantDegree));
            }
            if (this.saturnInSeventh) {
                out.add("Saturn is in the seventh, the astrologer's own house: the traditional "
                    + "sign that the reader, rather than the chart, will be at fault.");
            }
            return out;
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

        // <b>The considerations before judgement.</b> Read off the chart rather than the hour,
        // because they are about the moment being readable at all - see Radicality.radical.
        r.ascendantDegree = ((f.asc % 30.0) + 30.0) % 30.0;
        r.tooEarly = r.ascendantDegree < CAVEAT_DEGREES;
        r.tooLate = r.ascendantDegree > 30.0 - CAVEAT_DEGREES;

        ChartFrame.Body saturn = f.body("Saturn");
        if (saturn != null && saturn.ok) {
            // <b>By the chart's own cusps, through Zodiac.houseOf.</b> Not by counting signs
            // from the Ascendant - the rest of this engine reads a quadrant system, where the
            // seventh house is not the seventh sign. And not a house walk written here: that
            // rule already exists and a second copy of it is the defect this project logs most.
            r.saturnInSeventh = Zodiac.houseOf(saturn.lon, f.cusps) == 7;
        }
        return r;
    }

    // ======================================================= stage 3: the dynamics of light

    /**
     * The five aspects a perfection can be made by.
     *
     * <p>The Ptolemaic set, and only it. A horary judgement turns on whether two bodies
     * <i>meet</i>, and the minor aspects were never part of that question - admitting them
     * would let a septile decide a case, which no source this project holds asks for.
     */
    private static final Aspects.Type[] PTOLEMAIC = {
        Aspects.Type.CONJUNCTION, Aspects.Type.SEXTILE, Aspects.Type.SQUARE,
        Aspects.Type.TRINE, Aspects.Type.OPPOSITION
    };

    /** The seven planets a traditional judgement is made from. */
    private static final String[] TRADITIONAL =
        {"Moon", "Mercury", "Venus", "Sun", "Mars", "Jupiter", "Saturn"};

    /**
     * How far ahead a perfection is still an answer to the question, in days.
     *
     * <p><b>Why there is a cap at all.</b> The rule is "before either significator changes
     * sign", and two slow significators can sit in one sign for years - Saturn takes about two
     * and a half. Scanning that is both unbounded work and a false answer: a meeting thirty
     * months after the question was asked is not what was asked about. A year is a convention
     * rather than a measurement, so it is named here rather than buried in a loop bound.
     */
    public static final double MAX_WINDOW_DAYS = 365.0;

    /**
     * How near either end of a sign the Ascendant may not be, in degrees.
     *
     * Three, which is Lilly's figure and the usual one. A convention rather than a measurement,
     * so it is named here rather than written into the test twice.
     */
    public static final double CAVEAT_DEGREES = 3.0;

    /**
     * Mean daily motion in degrees, for deciding which of two bodies is the faster.
     *
     * <p><b>Mean, not instantaneous.</b> A planet's actual speed passes through zero at a
     * station, so an instantaneous comparison would call Saturn faster than Mars for the days
     * either side of a turn, and translation - which is defined by a faster body carrying light
     * to a slower - would flip with it. The ordering the tradition uses is a property of the
     * bodies, not of the moment.
     */
    private static double meanMotion(String body) {
        switch (body) {
            case "Moon":    return 13.176;
            case "Mercury": return 1.383;
            case "Venus":   return 1.602;
            case "Sun":     return 0.986;
            case "Mars":    return 0.524;
            case "Jupiter": return 0.083;
            case "Saturn":  return 0.034;
            default:        return 0.0;
        }
    }

    /**
     * A safe coarse scanning step for a body, in days.
     *
     * <p>Small enough that two successive crossings of the same aspect cannot both fall inside
     * one step, which is the only property the root finder needs of it.
     */
    private static double stepFor(String body) {
        switch (body) {
            case "Moon":                                 return 0.125;
            case "Sun": case "Mercury": case "Venus":    return 0.5;
            case "Mars":                                 return 1.0;
            default:                                     return 2.0;
        }
    }

    /** One meeting of two bodies: when, and by which aspect. */
    public static final class Meeting {
        public double jd;
        public Aspects.Type type;
        public String a;
        public String b;
    }

    /** What kind of testimony a line of the chain is. */
    public enum Kind {
        /** The two significators perfect an aspect with each other. */
        DIRECT,
        /** A faster body separates from one significator and applies to the other. */
        TRANSLATION,
        /** Both significators apply to a slower third body. */
        COLLECTION,
        /** A third body perfects with a significator first - prohibition, or frustration. */
        PROHIBITION,
        /** A significator stations before the perfection it was applying to. */
        REFRANATION,
        /** The Moon completes no further aspect in its sign. */
        VOID_MOON,
        /** One planet rules both sides of the question. */
        SHARED_RULER,
        /** The chart is not fit to judge. */
        NOT_RADICAL,
        /** Nothing perfects inside the window. */
        NO_PERFECTION
    }

    /** The answer a chart gives. */
    public enum Verdict {
        YES,
        /** It perfects, but by square or opposition - the matter completes through difficulty. */
        YES_WITH_DIFFICULTY,
        NO,
        /** Not fit to judge, so no answer is given. */
        NOT_RADICAL,
        /** The method has no answer here, and saying so is the answer. */
        UNDECIDED
    }

    /** One reason, in the order the method found it. */
    public static final class Testimony {
        public Kind kind;
        public String a;
        public String b;
        /** The aspect, where there is one. Null otherwise. */
        public Aspects.Type type;
        /** When it perfects, Julian day UT. NaN where the testimony is not an event. */
        public double jd = Double.NaN;
        /** One sentence, in the reader's language rather than the engine's. */
        public String because;

        @Override
        public String toString() {
            return kind + ": " + because;
        }
    }

    /** A judged chart: who stands for whom, whether it may be read, and why the answer. */
    public static final class Judgement {
        public Significators significators;
        public Radicality radicality;
        /** The reasons, in the order the method found them. Never null. */
        public final List<Testimony> chain = new ArrayList<>();
        public Verdict verdict = Verdict.UNDECIDED;
        /** The moment the first significator leaves its sign - the end of the window. */
        public double windowEnds = Double.NaN;
        /** Which significator ends the window, or "the year" when the cap did. */
        public String windowEndedBy;

        /** The first testimony of a kind, or null. */
        public Testimony first(Kind kind) {
            for (Testimony t : this.chain) {
                if (t.kind == kind) {
                    return t;
                }
            }
            return null;
        }

        public boolean has(Kind kind) {
            return first(kind) != null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(String.valueOf(verdict));
            for (Testimony t : this.chain) {
                sb.append("\n  ").append(t);
            }
            return sb.toString();
        }
    }

    private static Testimony say(Kind kind, String a, String b, Aspects.Type type,
                                 double jd, String because) {
        Testimony t = new Testimony();
        t.kind = kind;
        t.a = a;
        t.b = b;
        t.type = type;
        t.jd = jd;
        t.because = because;
        return t;
    }

    /**
     * When this body leaves the sign it is in now, or {@code horizon} if it does not.
     *
     * <p>Leaving covers backing out of a sign as well as entering the next one: a retrograde
     * significator that returns to the sign behind it has changed sign, and the rule is about
     * the sign the significator held when the question was asked.
     *
     * <p><b>One case this cannot see:</b> a body that leaves a sign and returns inside a single
     * scanning step, which needs a station within a fraction of a degree of a cusp. The step is
     * an eighth of a day for the Moon and two days for the slow planets, so the body would have
     * to cross, turn and cross back inside that - and in that case the significator is in its
     * own sign at both ends of the step, which is what this returns.
     */
    private static double signExit(SwissEph sw, String body, double jd, double horizon) {
        int start = Zodiac.signIndex(Almanac.bodyLongitude(sw, jd, body));
        double step = stepFor(body);
        for (double t = jd + step; t <= horizon; t += step) {
            if (Zodiac.signIndex(Almanac.bodyLongitude(sw, t, body)) == start) {
                continue;
            }
            double lo = t - step;
            double hi = t;
            for (int i = 0; i < 40; i++) {
                double mid = (lo + hi) / 2.0;
                if (Zodiac.signIndex(Almanac.bodyLongitude(sw, mid, body)) == start) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            return hi;
        }
        return horizon;
    }

    /**
     * The first moment in the window at which these two bodies perfect any Ptolemaic aspect,
     * or null when they do not meet inside it.
     *
     * <p><b>Both ends move</b>, which is what separates this from a transit to a fixed natal
     * degree: the scan is over the two bodies' separation, not over one body's longitude.
     */
    public static Meeting firstMeeting(SwissEph sw, String a, String b,
                                       double from, double to) {
        if (!(to > from)) {
            return null;
        }
        double step = Math.min(stepFor(a), stepFor(b));
        Meeting best = null;
        for (Aspects.Type type : PTOLEMAIC) {
            List<Double> targets = new ArrayList<>();
            targets.add(type.exactAngle);
            if (type.exactAngle != 0.0 && type.exactAngle != 180.0) {
                targets.add(-type.exactAngle);
            }
            for (double target : targets) {
                Almanac.OfTime g = t -> Almanac.signedDelta(
                    Almanac.signedDelta(Almanac.bodyLongitude(sw, t, a),
                                        Almanac.bodyLongitude(sw, t, b)),
                    target);
                for (double root : Almanac.roots(g, from, to, step)) {
                    if (best == null || root < best.jd) {
                        best = new Meeting();
                        best.jd = root;
                        best.type = type;
                        best.a = a;
                        best.b = b;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Judge a question: who stands for whom, whether the chart may be read, and what the light
     * between the significators does before either of them changes sign.
     *
     * <h3>Why a chain of reasons and not a score</h3>
     *
     * <p>David's decision is explicit about this, and it is the one place the spec's own
     * arithmetic was set aside. A confidence sum lets a void Moon and a translation cancel out,
     * which is not how the judgement works: horary answers on the <b>first decisive
     * testimony</b>, and a prohibition overrides a perfection that has not happened yet rather
     * than being averaged against it. So the verdict arrives with the chain that produced it,
     * in the order the method found it, and a reader can disagree with a step instead of with a
     * number.
     *
     * <h3>The order the testimonies are asked in</h3>
     *
     * <p>Radicality first, because an unfit chart is not judged at all. Then a shared ruler,
     * which removes the two significators the rest of the method is about. Then the window -
     * until the first significator changes sign, and never more than a year. Inside it: a direct
     * perfection, and if there is one, whether anything gets in front of it (prohibition) or
     * withdraws from it (refranation). Only where the significators never meet does the method
     * look for light carried by a third body - translation, then collection.
     *
     * @param jd the moment the question was asked, Julian day UT
     */
    public static Judgement judge(SwissEph sw, ChartFrame f, Matter matter,
                                  double jd, double lat, double lon) {
        Judgement j = new Judgement();
        j.significators = significators(f, matter);
        j.radicality = radicality(sw, f, jd, lat, lon);

        if (j.radicality.unknown || !j.radicality.radical()) {
            j.verdict = j.radicality.unknown ? Verdict.UNDECIDED : Verdict.NOT_RADICAL;
            j.chain.add(say(Kind.NOT_RADICAL, j.radicality.hourRuler,
                j.radicality.ascendantRuler, null, jd, j.radicality.toString()));
            return j;
        }

        String querent = j.significators.querent;
        String quesited = j.significators.quesited;

        if (j.significators.shared()) {
            j.chain.add(say(Kind.SHARED_RULER, querent, quesited, null, Double.NaN,
                querent + " rules both the Ascendant and the house of the matter, so there are "
                + "not two significators to perfect with each other. The tradition generally "
                + "reads one planet for both sides favourably - the parties are already of one "
                + "interest - but that is a reading of a structural fact rather than of a "
                + "motion, and this engine does not turn it into a yes without a source that "
                + "says to"));
            j.verdict = Verdict.UNDECIDED;
            return j;
        }

        // ---- the window: before either significator changes sign, and inside a year ----
        double cap = jd + MAX_WINDOW_DAYS;
        double outQuerent = signExit(sw, querent, jd, cap);
        double outQuesited = signExit(sw, quesited, jd, cap);
        j.windowEnds = Math.min(outQuerent, outQuesited);
        if (j.windowEnds >= cap) {
            j.windowEndedBy = "the year";
        } else {
            j.windowEndedBy = outQuerent <= outQuesited ? querent : quesited;
        }

        // The void Moon colours the whole judgement rather than deciding it, so it is entered
        // wherever it is true and the motion testimonies are still asked for.
        if (f.moonVoidOfCourse) {
            j.chain.add(say(Kind.VOID_MOON, "Moon", null, null, Double.NaN,
                "the Moon is void of course - it completes no further aspect in the sign it "
                + "holds - and the tradition reads that as nothing coming of the matter"));
        }

        List<Aspects.Hit> now = Aspects.betweenBodies(f);
        Meeting direct = firstMeeting(sw, querent, quesited, jd, j.windowEnds);

        // Prohibition and refranation are only asked about where there is a perfection for them
        // to prevent. A third body perfecting with a significator in a chart where the
        // significators never meet is not a prohibition - it is an unrelated aspect.
        if (direct != null) {
            double horizon = direct.jd;

            Meeting blocker = null;
            String blockerBody = null;
            String blocked = null;
            for (String third : TRADITIONAL) {
                if (third.equals(querent) || third.equals(quesited) || cannotProhibit(third)) {
                    continue;
                }
                for (int k = 0; k < 2; k++) {
                    String side = k == 0 ? querent : quesited;
                    Meeting m = firstMeeting(sw, third, side, jd, horizon);
                    if (m != null && (blocker == null || m.jd < blocker.jd)) {
                        blocker = m;
                        blockerBody = third;
                        blocked = side;
                    }
                }
            }
            if (blocker != null) {
                j.chain.add(say(Kind.PROHIBITION, blockerBody, blocked, blocker.type, blocker.jd,
                    blockerBody + " perfects a " + blocker.type.name().toLowerCase()
                    + " with " + blocked + " before " + querent + " and " + quesited
                    + " reach each other, so the light between them is taken first"));
                j.verdict = Verdict.NO;
                return j;
            }

            // <b>No refranation test here, and that is the correction.</b> This branch has a
            // perfection that the ephemeris says happens, and {@link #firstMeeting} finds it by
            // walking the real separation of two real bodies - so it already accounts for every
            // station between now and then. A planet that turns and later completes the aspect
            // anyway has not refranated; it has taken a longer road to the same meeting.
            // Refranation belongs where the meeting never arrives, below.

            boolean hard = throughDifficulty(direct.type);
            j.chain.add(say(Kind.DIRECT, querent, quesited, direct.type, direct.jd,
                querent + " and " + quesited + " perfect a "
                + direct.type.name().toLowerCase() + " before either changes sign"
                + (hard ? ", by an aspect of difficulty rather than of ease" : "")));
            j.verdict = hard ? Verdict.YES_WITH_DIFFICULTY : Verdict.YES;
            return j;
        }

        // ---- translation: a faster body carries the light from one significator to the other --
        for (String third : TRADITIONAL) {
            if (third.equals(querent) || third.equals(quesited)) {
                continue;
            }
            if (meanMotion(third) <= meanMotion(querent)
                || meanMotion(third) <= meanMotion(quesited)) {
                continue;                       // only a faster body can carry light
            }
            for (int k = 0; k < 2; k++) {
                String from = k == 0 ? querent : quesited;
                String to = k == 0 ? quesited : querent;
                if (!separating(now, third, from)) {
                    continue;
                }
                Meeting m = firstMeeting(sw, third, to, jd, j.windowEnds);
                if (m == null) {
                    continue;
                }
                j.chain.add(say(Kind.TRANSLATION, third, to, m.type, m.jd,
                    third + " separates from " + from + " and applies to " + to
                    + ", carrying the light between them"));
                j.verdict = Verdict.YES;
                return j;
            }
        }

        // ---- collection: both significators apply to a slower third ----
        for (String third : TRADITIONAL) {
            if (third.equals(querent) || third.equals(quesited)) {
                continue;
            }
            if (meanMotion(third) >= meanMotion(querent)
                || meanMotion(third) >= meanMotion(quesited)) {
                continue;                       // only a slower body can collect
            }
            Meeting one = firstMeeting(sw, querent, third, jd, j.windowEnds);
            Meeting two = firstMeeting(sw, quesited, third, jd, j.windowEnds);
            if (one == null || two == null) {
                continue;
            }
            j.chain.add(say(Kind.COLLECTION, third, null, null, Math.max(one.jd, two.jd),
                "both " + querent + " and " + quesited + " apply to " + third
                + ", which is slower than either and collects their light"));
            j.verdict = Verdict.YES;
            return j;
        }

        // ---- refranation: they were coming to it, and one of them turned back ----
        //
        // <b>This is the only place refranation can live.</b> It is not a rival to a perfection
        // - a perfection the ephemeris reports is a perfection, whatever the bodies did on the
        // way - it is the reason a perfection the chart was promising never arrives. So the
        // three conditions are read together: the significators are in an applying aspect at
        // the moment of the question, no perfection is found before the window closes, and one
        // of the two stations inside it. Written the other way round, as a station before a
        // perfection that does happen, the test could never be true, and it never was: it
        // fired zero times in 392 judgements, which is what sent me back to it.
        if (applyingNow(now, querent, quesited)) {
            List<Almanac.Event> turns =
                Almanac.stations(sw, jd, j.windowEnds, querent, quesited);
            if (!turns.isEmpty()) {
                Almanac.Event turn = turns.get(0);
                j.chain.add(say(Kind.REFRANATION, turn.body, null, null, turn.jd,
                    querent + " and " + quesited + " are applying, but " + turn.body
                    + " stations before the aspect completes and withdraws from it"));
                j.verdict = Verdict.NO;
                return j;
            }
        }

        j.chain.add(say(Kind.NO_PERFECTION, querent, quesited, null, Double.NaN,
            querent + " and " + quesited + " neither meet nor have their light carried before "
            + ("the year".equals(j.windowEndedBy) ? "a year is out"
               : j.windowEndedBy + " changes sign")));
        j.verdict = Verdict.NO;
        return j;
    }

    /** Are these two in an aspect they are closing on, at the moment of the question? */
    private static boolean applyingNow(List<Aspects.Hit> hits, String a, String b) {
        for (Aspects.Hit h : hits) {
            boolean pair = (h.a.equals(a) && h.b.equals(b))
                || (h.a.equals(b) && h.b.equals(a));
            if (pair && h.applying) {
                return true;
            }
        }
        return false;
    }


    /**
     * Bodies that cannot prohibit, however early they perfect.
     *
     * <p><b>The Moon, and the reason is structural rather than a preference.</b> This engine
     * makes the Moon the querent's co-significator in every chart ({@link Significators#moon}),
     * so it is already a party to the question - and a party cannot also be the third body that
     * cuts in between the two sides. Letting it do both is how the same planet ends up
     * testifying twice, which is the defect the convergence work spent K8 removing.
     *
     * <p><b>It was measured before it was decided.</b> With the Moon admitted, prohibition fired
     * <b>28 times against 4 surviving direct perfections</b> over 392 judgements - because the
     * Moon perfects with something every couple of days, so any window wider than that contains
     * a Moon aspect to a significator and the answer is NO almost everywhere. An engine that
     * answers NO almost always is not a judgement, and the number is what showed it.
     *
     * <p><b>This narrows the decision's words</b> - "a third body perfecting with one first" -
     * rather than following them exactly, and that was put to David on 21 September with the
     * measurement, and taken. The Moon still <i>translates</i> light, which is its classical
     * office here and the commonest yes this engine gives.
     */
    private static boolean cannotProhibit(String body) {
        return "Moon".equals(body);
    }


    /**
     * Does this aspect complete a matter only through difficulty?
     *
     * <p>The square and the opposition. The decision calls conjunction, trine and sextile a
     * plain yes, and the two hard aspects still perfect - the matter completes - but the
     * tradition is unanimous that it completes the hard way, which is a different answer from
     * either yes or no.
     *
     * <p><b>Why this is a named method and not two terms inside {@code judge}.</b> Inline, the
     * rule could only be observed through whichever aspects the sky supplied: a mutation making
     * the trine an aspect of difficulty <b>survived</b> the suite on 21 September, because in
     * the fortnight it sampled no pair of significators ever perfected a square or a trine.
     * Named, it can be asserted over all five aspects at once, and a suite that covers the
     * weather instead of the rule is exactly the kind of green this project does not want.
     */
    public static boolean throughDifficulty(Aspects.Type type) {
        return type == Aspects.Type.SQUARE || type == Aspects.Type.OPPOSITION;
    }

    /**
     * Is this body separating from that one - past the exact aspect rather than approaching it?
     *
     * <p>Read off the chart's own aspects rather than recomputed here, so translation uses the
     * same orbs and the same applying flag as every other surface in the app.
     */
    private static boolean separating(List<Aspects.Hit> hits, String a, String b) {
        for (Aspects.Hit h : hits) {
            boolean pair = (h.a.equals(a) && h.b.equals(b))
                || (h.a.equals(b) && h.b.equals(a));
            if (pair && !h.applying) {
                return true;
            }
        }
        return false;
    }
}
