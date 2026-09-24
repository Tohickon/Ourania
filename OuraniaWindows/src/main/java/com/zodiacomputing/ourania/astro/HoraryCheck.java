package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * K13, stage 2: significators, and whether the chart is fit to judge.
 *
 * <p><b>Both halves are decidable without an opinion, which is why they are built first.</b> Who
 * stands for whom is arithmetic on the cusps and the rulerships; whether the chart is radical is
 * arithmetic on the hour and the rising sign. Nothing here judges a question - that is stage 3 -
 * so nothing here needs a source beyond the decision of 19 September.
 *
 * <p>The significator assertions are made against a chart's own cusps rather than against
 * remembered values: the ruler of the house of the matter is recomputed from the sign on that
 * cusp, so the check fails if either the cusp or the rulership moves. The radicality assertions
 * are made by <b>constructing the hour</b> - finding a moment whose hour ruler is the Ascendant's
 * ruler, and another whose is not - because a fixture chosen for its date would pass or fail by
 * luck and teach nothing.
 */
public final class HoraryCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final double LAT = 39.95;
    private static final double LON = -75.17;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(2026, 9, 23, 17.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, LAT, LON, 'P', false, 0.0);

        // ---- who stands for whom
        for (Horary.Matter m : Horary.Matter.values()) {
            Horary.Significators s = Horary.significators(f, m);
            ok(m + " takes its significator from house " + m.house,
                s.quesitedSign == Zodiac.signIndex(f.cusps[m.house]));
            ok(m + "'s significator is that sign's ruler (" + s.quesited + ")",
                s.quesited.equals(Dignity.domicileRulerOf(Zodiac.signIndex(f.cusps[m.house]))));
            ok(m + " keeps the same querent", s.querent.equals(
                Dignity.domicileRulerOf(Zodiac.signIndex(f.asc))) && "Moon".equals(s.moon));
        }

        Horary.Significators any = Horary.significators(f, Horary.Matter.PARTNERS);
        ok("the querent is the ruler of the rising sign", any.querent.equals(
            Dignity.domicileRulerOf(Zodiac.signIndex(f.asc))));
        ok("the Moon always co-signifies the querent", "Moon".equals(any.moon));
        ok("the seventh is the house of partners", Horary.Matter.PARTNERS.house == 7);
        ok("the matters are the seven the decision names", Horary.Matter.values().length == 7);

        // <b>Shared significator, constructed rather than hoped for.</b> The Ascendant and the
        // house of the matter carry signs of one ruler often enough that a chart can be found
        // for it, and the flag has to be true exactly then.
        boolean sharedSeen = false;
        boolean sharedRight = true;
        for (int hour = 0; hour < 24 && !sharedSeen; hour++) {
            ChartFrame g = ChartFrame.compute(sw,
                new SweDate(2026, 9, 23, hour).getJulDay(), LAT, LON, 'P', false, 0.0);
            for (Horary.Matter m : Horary.Matter.values()) {
                Horary.Significators s = Horary.significators(g, m);
                boolean same = s.querent.equals(s.quesited);
                if (s.shared() != same) {
                    sharedRight = false;
                }
                if (same) {
                    sharedSeen = true;
                }
            }
        }
        ok("a chart where one planet stands for both sides is found in a day", sharedSeen);
        ok("and shared() agrees with it every time", sharedRight);

        // ---- radicality, with the hour constructed
        // Walk a day in ten-minute steps and take the first moment of each kind, so both
        // branches are exercised on real hours rather than on a fixture chosen for its date.
        Double radicalAt = null;
        Double notRadicalAt = null;
        for (int step = 0; step < 24 * 6 && (radicalAt == null || notRadicalAt == null); step++) {
            double t = new SweDate(2026, 9, 23, 0).getJulDay() + step / 144.0;
            ChartFrame g = ChartFrame.compute(sw, t, LAT, LON, 'P', false, 0.0);
            Horary.Radicality r = Horary.radicality(sw, g, t, LAT, LON);
            if (r.unknown) {
                continue;
            }
            if (r.radical() && radicalAt == null) {
                radicalAt = t;
            }
            if (!r.radical() && notRadicalAt == null) {
                notRadicalAt = t;
            }
        }
        ok("a radical moment exists in the day", radicalAt != null);
        ok("and a moment that is not radical", notRadicalAt != null);

        if (radicalAt != null) {
            ChartFrame g = ChartFrame.compute(sw, radicalAt, LAT, LON, 'P', false, 0.0);
            Horary.Radicality r = Horary.radicality(sw, g, radicalAt, LAT, LON);
            ok("a radical chart says which ground it passed on: " + r,
                r.sameRuler || r.sharesTriplicity);
            ok("the hour ruler it names is the hour ruler",
                r.hourRuler.equals(PlanetaryHours.rulerAt(sw, radicalAt, LAT, LON)));
            ok("the Ascendant ruler it names is the Ascendant's",
                r.ascendantRuler.equals(Dignity.domicileRulerOf(Zodiac.signIndex(g.asc))));
            if (r.sharesTriplicity && !r.sameRuler) {
                ChartFrame.Body sun = g.body("Sun");
                ok("and the triplicity it passed on is the one for this chart's sect",
                    r.triplicityRuler.equals(Dignity.triplicityRulerOf(
                        Zodiac.signIndex(g.asc), Sect.isDiurnal(sun.lon, g.asc))));
            }
        }
        if (notRadicalAt != null) {
            ChartFrame g = ChartFrame.compute(sw, notRadicalAt, LAT, LON, 'P', false, 0.0);
            Horary.Radicality r = Horary.radicality(sw, g, notRadicalAt, LAT, LON);
            ok("a chart that is not radical says so on both grounds: " + r,
                !r.sameRuler && !r.sharesTriplicity && !r.radical());
        }

        // <b>Sect is load-bearing here.</b> The same rising sign has a different triplicity
        // ruler by day and by night, so a radicality test that ignored sect would answer for
        // whichever half of the tradition it happened to be written from.
        boolean sectMatters = false;
        for (int sign = 0; sign < 12; sign++) {
            if (!Dignity.triplicityRulerOf(sign, true).equals(
                    Dignity.triplicityRulerOf(sign, false))) {
                sectMatters = true;
            }
        }
        ok("day and night triplicity rulers differ, so the sect in the test is load-bearing",
            sectMatters);

        // ---- where there are no hours, it refuses rather than guessing
        double polar = new SweDate(2026, 12, 21, 12.0).getJulDay();
        ChartFrame arctic = ChartFrame.compute(sw, polar, 78.2, 15.6, 'P', false, 0.0);
        Horary.Radicality none = Horary.radicality(sw, arctic, polar, 78.2, 15.6);
        ok("no hour ruler inside the polar night, so radicality is not judged", none.unknown);
        ok("and an unjudged chart is not called radical", !none.radical());

        // ---- the considerations before judgement: measured, reported, and not a refusal
        //
        // <b>They warn and do not refuse, which is the whole of what was decided.</b> Folded
        // into radical() they turn away 26.7% of all moments - six degrees of every thirty for
        // the Ascendant and about a house in twelve for Saturn - and David's call was that a
        // reader should be told what is doubtful rather than handed silence for one question in
        // four. So the assertion that matters here is the negative one: a chart carrying every
        // caution at once is still judged.
        //
        // Walked across a whole day rather than sampled, because the Ascendant moves through
        // every degree of every sign in one and the boundaries are what is being asserted.
        double dayStart = new SweDate(2026, 5, 14, 0.0).getJulDay();
        boolean earlyRight = true;
        boolean lateRight = true;
        boolean middleClear = true;
        boolean cautionNeverRefuses = true;
        boolean degreeRight = true;
        int earlySeen = 0;
        int lateSeen = 0;
        int saturnSeen = 0;
        for (int step = 0; step < 288; step++) {
            double when = dayStart + step * (1.0 / 288.0);
            ChartFrame g = ChartFrame.compute(sw, when, LAT, LON, 'P', false, 0.0);
            Horary.Radicality r = Horary.radicality(sw, g, when, LAT, LON);
            if (r.unknown) {
                continue;
            }
            double deg = ((g.asc % 30.0) + 30.0) % 30.0;
            degreeRight &= Math.abs(deg - r.ascendantDegree) < 1e-9;
            if (deg < Horary.CAVEAT_DEGREES) {
                earlyRight &= r.tooEarly;
                earlySeen++;
            } else if (deg > 30.0 - Horary.CAVEAT_DEGREES) {
                lateRight &= r.tooLate;
                lateSeen++;
            } else {
                middleClear &= !r.tooEarly && !r.tooLate;
            }
            if (r.saturnInSeventh) {
                saturnSeen++;
            }
            // The property David chose: a caution changes what is said, never what is judged.
            boolean byHour = r.sameRuler || r.sharesTriplicity;
            cautionNeverRefuses &= r.radical() == byHour;
        }
        ok("the Ascendant's degree is measured from its own sign", degreeRight);
        ok("an Ascendant inside the first three degrees is called too early (" + earlySeen + ")",
            earlyRight && earlySeen > 0);
        ok("inside the last three, too late (" + lateSeen + ")", lateRight && lateSeen > 0);
        ok("and anywhere between, neither", middleClear);
        ok("a caution never changes whether the chart is judged", cautionNeverRefuses);
        ok("Saturn in the seventh is seen at some point in the day (" + saturnSeen + ")",
            saturnSeen > 0);

        // <b>And each caution is said out loud.</b> A consideration nobody is told about is the
        // same as one that was never measured.
        Horary.Radicality spoken = new Horary.Radicality();
        ok("with nothing wrong there is nothing to say", spoken.cautions().isEmpty());
        spoken.ascendantDegree = 1.4;
        spoken.tooEarly = true;
        ok("too early is reported", spoken.cautions().size() == 1
            && spoken.cautions().get(0).contains("too early"));
        spoken.tooLate = true;
        spoken.saturnInSeventh = true;
        ok("and all three are, when all three apply", spoken.cautions().size() == 3);
        ok("the seventh-house one names the astrologer, not the question",
            spoken.cautions().get(2).contains("reader"));


        // =================================================== stage 3: the dynamics of light

        // <b>firstMeeting is the whole engine of stage 3</b>, so it is asserted against the
        // ephemeris rather than against itself: at the moment it returns, the two bodies really
        // are that aspect's angle apart, and there really is no earlier meeting in the window.
        double monthEnd = jd + 30.0;
        int meetingsProved = 0;
        for (String[] pair : new String[][] {
                {"Mercury", "Jupiter"}, {"Venus", "Mars"}, {"Sun", "Saturn"},
                {"Moon", "Mars"}, {"Mars", "Jupiter"}}) {
            Horary.Meeting m = Horary.firstMeeting(sw, pair[0], pair[1], jd, monthEnd);
            if (m == null) {
                continue;
            }
            meetingsProved++;
            double sep = Math.abs(Almanac.signedDelta(
                Almanac.bodyLongitude(sw, m.jd, pair[0]),
                Almanac.bodyLongitude(sw, m.jd, pair[1])));
            ok(pair[0] + " and " + pair[1] + " really are at "
                + m.type.name().toLowerCase() + " when the finder says so",
                Math.abs(sep - m.type.exactAngle) < 0.02);
            ok("and nothing earlier was missed for " + pair[0] + "/" + pair[1],
                Horary.firstMeeting(sw, pair[0], pair[1], jd, m.jd - 0.05) == null);
            Horary.Meeting swapped = Horary.firstMeeting(sw, pair[1], pair[0], jd, monthEnd);
            ok("and the pair reads the same in either order for " + pair[0] + "/" + pair[1],
                swapped != null && Math.abs(swapped.jd - m.jd) < 1e-6 && swapped.type == m.type);
        }
        ok("the meeting finder was actually exercised", meetingsProved >= 3);
        ok("a window that ends before it opens holds no meeting",
            Horary.firstMeeting(sw, "Mars", "Jupiter", jd, jd) == null);

        // <b>The difficulty rule, over all five aspects at once.</b> Asserting it only
        // through the perfections a sample happens to contain is how "a trine is an aspect of
        // difficulty" survived the mutation run on 21 September: the four days sampled held no
        // square and no trine between any pair of significators, so the rule was never
        // observed. Now the rule is named in the engine and read here directly.
        ok("a square completes a matter the hard way",
            Horary.throughDifficulty(Aspects.Type.SQUARE));
        ok("and so does an opposition",
            Horary.throughDifficulty(Aspects.Type.OPPOSITION));
        ok("a conjunction does not", !Horary.throughDifficulty(Aspects.Type.CONJUNCTION));
        ok("nor a sextile", !Horary.throughDifficulty(Aspects.Type.SEXTILE));
        ok("nor a trine", !Horary.throughDifficulty(Aspects.Type.TRINE));

        // ---- the judgement, over a run of real moments
        int judged = 0;
        int radical = 0;
        boolean everyChainSpeaks = true;
        boolean moonNeverProhibits = true;
        boolean windowIsCapped = true;
        boolean windowEndsAtASignChange = true;
        int windowsClosedBySign = 0;
        boolean voidAgrees = true;
        boolean hardIsSquareOrOpposition = true;
        boolean perfectionInsideWindow = true;
        boolean noPerfectionMeansNone = true;
        boolean sharedIsUndecided = true;
        boolean translatorIsFaster = true;
        boolean translatorSeparates = true;
        boolean prohibitionComesFirst = true;
        int directSeen = 0;
        int translationSeen = 0;
        int prohibitionSeen = 0;
        int sharedSeen3 = 0;

        // <b>Twelve days, not four, and the number was measured.</b> A square perfection
        // first appears on day 7, a trine on day 10, a prohibition blocking the quesited rather
        // than the querent on day 9, and the chart that proves the refranation guard is
        // load-bearing on day 9. At four days the suite read green while three mutations walked
        // through it.
        java.util.Set<Aspects.Type> directTypes = new java.util.LinkedHashSet<>();
        int blockedQuerent = 0;
        int blockedQuesited = 0;
        int notApplyingWithAStation = 0;
        boolean guardHolds = true;
        for (int day = 0; day < 12; day++) {
            for (double hour : new double[] {3.0, 9.0, 15.0, 21.0}) {
                double at = new SweDate(2026, 9, 23 + day, hour).getJulDay();
                ChartFrame chart = ChartFrame.compute(sw, at, LAT, LON, 'P', false, 0.0);
                for (Horary.Matter m : Horary.Matter.values()) {
                    Horary.Judgement j = Horary.judge(sw, chart, m, at, LAT, LON);
                    judged++;
                    everyChainSpeaks &= !j.chain.isEmpty() && j.verdict != null;

                    if (j.has(Horary.Kind.NOT_RADICAL)) {
                        continue;
                    }
                    radical++;

                    if (j.has(Horary.Kind.SHARED_RULER)) {
                        sharedSeen3++;
                        sharedIsUndecided &= j.verdict == Horary.Verdict.UNDECIDED
                            && j.chain.size() == 1;
                        continue;
                    }

                    windowIsCapped &= j.windowEnds - at <= 365.0 + 1e-9;

                    // <b>Asserted against the sky, not against the field.</b> Either the
                    // window ran to the cap, or a significator really does change sign at the
                    // moment it closes: the same sign an hour before, a different one an hour
                    // after. A signExit that simply returned its horizon would satisfy every
                    // other assertion here.
                    if (!"the year".equals(j.windowEndedBy)) {
                        windowsClosedBySign++;
                        String who = j.windowEndedBy;
                        int before = Zodiac.signIndex(
                            Almanac.bodyLongitude(sw, j.windowEnds - 0.05, who));
                        int after = Zodiac.signIndex(
                            Almanac.bodyLongitude(sw, j.windowEnds + 0.05, who));
                        int start = Zodiac.signIndex(Almanac.bodyLongitude(sw, at, who));
                        windowEndsAtASignChange &= before == start && after != start;
                    }
                    voidAgrees &= chart.moonVoidOfCourse == j.has(Horary.Kind.VOID_MOON);

                    Horary.Testimony p = j.first(Horary.Kind.PROHIBITION);
                    if (p != null) {
                        prohibitionSeen++;
                        if (p.b.equals(j.significators.querent)) {
                            blockedQuerent++;
                        } else {
                            blockedQuesited++;
                        }
                        moonNeverProhibits &= !"Moon".equals(p.a);
                        // A prohibition is only a prohibition if it beats a real perfection.
                        Horary.Meeting d = Horary.firstMeeting(sw, j.significators.querent,
                            j.significators.quesited, at, j.windowEnds);
                        prohibitionComesFirst &= d != null && p.jd < d.jd;
                    }

                    Horary.Testimony dt = j.first(Horary.Kind.DIRECT);
                    if (dt != null) {
                        directSeen++;
                        directTypes.add(dt.type);
                        perfectionInsideWindow &= dt.jd <= j.windowEnds + 1e-9 && dt.jd > at;
                        boolean hard = dt.type == Aspects.Type.SQUARE
                            || dt.type == Aspects.Type.OPPOSITION;
                        hardIsSquareOrOpposition &=
                            hard == (j.verdict == Horary.Verdict.YES_WITH_DIFFICULTY);
                    }

                    Horary.Testimony t = j.first(Horary.Kind.TRANSLATION);
                    if (t != null) {
                        translationSeen++;
                        translatorIsFaster &= motion(t.a) > motion(j.significators.querent)
                            && motion(t.a) > motion(j.significators.quesited);
                        // It must be leaving one of them, or it is carrying nothing.
                        boolean leaves = false;
                        for (Aspects.Hit h : Aspects.betweenBodies(chart)) {
                            boolean touches = (h.a.equals(t.a) || h.b.equals(t.a));
                            boolean other = h.a.equals(j.significators.querent)
                                || h.b.equals(j.significators.querent)
                                || h.a.equals(j.significators.quesited)
                                || h.b.equals(j.significators.quesited);
                            leaves |= touches && other && !h.applying;
                        }
                        translatorSeparates &= leaves;
                    }

                    if (j.has(Horary.Kind.NO_PERFECTION)) {
                        noPerfectionMeansNone &= Horary.firstMeeting(sw,
                            j.significators.querent, j.significators.quesited,
                            at, j.windowEnds) == null
                            && j.verdict == Horary.Verdict.NO;

                        // <b>The negative case the refranation guard exists for.</b> A station
                        // inside the window is not refranation on its own: the significators
                        // have to have been applying to something for one of them to withdraw
                        // from it. Without this, dropping the guard changed nothing observable
                        // and the mutation survived.
                        boolean applying = false;
                        for (Aspects.Hit h : Aspects.betweenBodies(chart)) {
                            boolean pair = (h.a.equals(j.significators.querent)
                                    && h.b.equals(j.significators.quesited))
                                || (h.a.equals(j.significators.quesited)
                                    && h.b.equals(j.significators.querent));
                            applying |= pair && h.applying;
                        }
                        if (!applying && !Almanac.stations(sw, at, j.windowEnds,
                                j.significators.querent, j.significators.quesited).isEmpty()) {
                            notApplyingWithAStation++;
                            guardHolds &= !j.has(Horary.Kind.REFRANATION);
                        }
                    }
                }
            }
        }

        ok("every chart judged returns a verdict and at least one reason", everyChainSpeaks);
        ok("the run was long enough to mean something: " + judged + " judged, "
            + radical + " radical", judged >= 100 && radical >= 10);
        ok("the window never runs past the year the cap names", windowIsCapped);
        ok("and the cap is the year it says it is", Horary.MAX_WINDOW_DAYS == 365.0);
        ok("the window closes exactly where a significator changes sign ("
            + windowsClosedBySign + ")",
            windowEndsAtASignChange && windowsClosedBySign > 0);
        ok("a void Moon is reported exactly when the chart has one", voidAgrees);
        ok("the Moon never prohibits - it is already the querent's co-significator ("
            + prohibitionSeen + ")", moonNeverProhibits && prohibitionSeen > 0);
        ok("a prohibition always beats a perfection that would otherwise have arrived",
            prohibitionComesFirst);
        ok("a perfection falls after the question and inside the window (" + directSeen + ")",
            perfectionInsideWindow && directSeen > 0);
        ok("only a square or an opposition answers yes with difficulty",
            hardIsSquareOrOpposition);
        ok("a translator is faster than both significators (" + translationSeen + ")",
            translatorIsFaster && translationSeen > 0);
        ok("and it is separating from one of them, or it carries nothing",
            translatorSeparates);
        ok("no perfection means the significators really never meet", noPerfectionMeansNone);
        ok("the sample really contains a hard perfection and an easy one: " + directTypes,
            directTypes.contains(Aspects.Type.SQUARE)
                && directTypes.contains(Aspects.Type.TRINE));
        ok("a prohibition is watched for on both sides, not just the querent's ("
            + blockedQuerent + " / " + blockedQuesited + ")",
            blockedQuerent > 0 && blockedQuesited > 0);
        ok("a station alone is not refranation - they must have been applying ("
            + notApplyingWithAStation + ")",
            guardHolds && notApplyingWithAStation > 0);
        ok("one planet for both sides is left undecided, alone in its chain ("
            + sharedSeen3 + ")", sharedIsUndecided && sharedSeen3 > 0);

        foundDynamics(sw);

        report();
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
        }
        System.exit(1);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }

    // <b>Both moments below were FOUND, not chosen.</b> Neither collection nor refranation
    // appeared once in 392 judgements across a fortnight, so a suite built on a convenient date
    // would have left two of the six dynamics entirely unproven and still read green.
    // DynamicsSearch walked the stations of Mercury, Venus and Mars over three years for the
    // first and 2026 for the second, testing radicality before the expensive scanning because
    // three charts in four are unfit to judge. These are its answers.

    /** 2026-06-22, where Mercury stations before completing its aspect to Jupiter. */
    private static final double REFRANATION_JD =
        new SweDate(2026, 6, 22, 23.59524536).getJulDay();
    private static final Horary.Matter REFRANATION_MATTER = Horary.Matter.HEALTH;

    /** 2026-01-07, where the Sun and Venus both apply to Mars. */
    private static final double COLLECTION_JD = new SweDate(2026, 1, 7, 0.0).getJulDay();
    private static final Horary.Matter COLLECTION_MATTER = Horary.Matter.COMMUNICATION;

    /**
     * Mean daily motion, as the judgement orders the bodies by.
     *
     * <b>A second copy of the engine's table, on purpose.</b> If {@code Horary.meanMotion} is
     * edited, this one does not follow, and the translation and collection assertions go red -
     * which is the point of asserting an ordering the engine also holds an opinion about.
     */
    private static double motion(String body) {
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

    /** The two dynamics no ordinary fixture shows. */
    private static void foundDynamics(SwissEph sw) {
        ChartFrame rf = ChartFrame.compute(sw, REFRANATION_JD, LAT, LON, 'P', false, 0.0);
        Horary.Judgement rj =
            Horary.judge(sw, rf, REFRANATION_MATTER, REFRANATION_JD, LAT, LON);
        Horary.Testimony r = rj.first(Horary.Kind.REFRANATION);
        ok("the found moment really refranates", r != null && rj.verdict == Horary.Verdict.NO);
        if (r != null) {
            ok("and the body that turns is one of the two significators",
                r.a.equals(rj.significators.querent) || r.a.equals(rj.significators.quesited));
            ok("and the ephemeris agrees there is a station inside the window",
                !Almanac.stations(sw, REFRANATION_JD, rj.windowEnds,
                    rj.significators.querent, rj.significators.quesited).isEmpty());
            // The correction of 21 Sep, encoded: refranation is the reason a promised meeting
            // never arrives, so there must be no meeting. Written the other way round - a
            // station before a perfection that does happen - it could never be true.
            ok("and the perfection it withdraws from never arrives",
                Horary.firstMeeting(sw, rj.significators.querent, rj.significators.quesited,
                    REFRANATION_JD, rj.windowEnds) == null);
            boolean applying = false;
            for (Aspects.Hit h : Aspects.betweenBodies(rf)) {
                boolean pair = (h.a.equals(rj.significators.querent)
                        && h.b.equals(rj.significators.quesited))
                    || (h.a.equals(rj.significators.quesited)
                        && h.b.equals(rj.significators.querent));
                applying |= pair && h.applying;
            }
            ok("and they were applying when the question was asked", applying);
        }

        ChartFrame cf = ChartFrame.compute(sw, COLLECTION_JD, LAT, LON, 'P', false, 0.0);
        Horary.Judgement cj =
            Horary.judge(sw, cf, COLLECTION_MATTER, COLLECTION_JD, LAT, LON);
        Horary.Testimony c = cj.first(Horary.Kind.COLLECTION);
        ok("the found moment really collects", c != null && cj.verdict == Horary.Verdict.YES);
        if (c != null) {
            ok("and the collector is slower than both significators",
                motion(c.a) < motion(cj.significators.querent)
                    && motion(c.a) < motion(cj.significators.quesited));
            ok("and both significators really do apply to it",
                Horary.firstMeeting(sw, cj.significators.querent, c.a,
                    COLLECTION_JD, cj.windowEnds) != null
                && Horary.firstMeeting(sw, cj.significators.quesited, c.a,
                    COLLECTION_JD, cj.windowEnds) != null);
            ok("and it was only reached because the two never meet each other",
                Horary.firstMeeting(sw, cj.significators.querent, cj.significators.quesited,
                    COLLECTION_JD, cj.windowEnds) == null);
        }
    }
}
