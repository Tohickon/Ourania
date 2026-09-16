package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L8: solar and lunar returns.
 *
 * A return is the chart for the moment a body regains its natal longitude. The solar return
 * pairs naturally with the profection year - it <i>is</i> the boundary of that year, which is
 * why {@link Profection#solarReturnJd} already existed before this class and is reused here
 * rather than reimplemented.
 *
 * <h3>What a return actually adds</h3>
 *
 * Less than it first appears, and saying so is the point of this comment. A return chart's
 * planets are simply <b>where the planets were at that instant</b> - the same sky the transit
 * family already reports. What a return contributes that nothing else does is its
 * <b>angular structure</b>: the Ascendant and MC of the return depend on the location and on
 * the exact minute of the return, so "the solar return Ascendant falls on natal Mars" is a
 * genuinely new statement.
 *
 * <h3>Measured: the guard catches half, and the survivors are the fast bodies</h3>
 *
 * An earlier draft of this comment said to expect the echo guard in {@link Convergence} to
 * discount most planetary contacts. <b>It discounts about half</b> - 17 of 31 on the measured
 * chart - and the ones that survive are not a random half:
 *
 * <pre>
 *   independent return witnesses by point:  MC 4, Mercury 4, Mars 3, Moon 2, Pluto 1
 * </pre>
 *
 * Mercury, Mars and the Moon survive <i>because</i> they are absent from
 * {@link Transits#yearMarkers}: there is no transit perfection carrying that body, so there is
 * nothing for the guard to match them against. **The return family therefore readmits exactly
 * the annually-guaranteed fast-body noise that the transit family excludes on principle.**
 * "Solar return Mercury square natal Jupiter" is largely a statement about where Mercury
 * happened to be on a birthday.
 *
 * This is left as it stands rather than quietly filtered, because a return chart being read
 * whole is what the technique <i>is</i>, and narrowing it to the angles is an astrological
 * judgement rather than a bug fix. But it is the same inconsistency already flagged for
 * Mercury stations, and it is why the score histogram flattened when this family was added:
 * `{1:1, 2:4, 3:4, 4:1}` became `{1:1, 3:7, 4:1, 5:1}`. Angles-only was measured too and is
 * no better - `{1:1, 3:8, 4:1}` - because the MC alone contributes four witnesses.
 *
 * <h3>This is a sky-position family</h3>
 *
 * {@link SolarArc} and {@link Progressions} are symbolic - a directed or progressed Mars is
 * not Mars. <b>A return chart's Mars is Mars</b>, at a particular moment, so RETURN is
 * classified as a sky position in {@link Convergence} and can legitimately echo a transit.
 * Classifying it symbolic would let one slow transit vote twice.
 *
 * The return angles cannot echo anything, because the transit family has no "transiting
 * Ascendant" - so they survive the guard on their own, which is exactly the part worth
 * keeping.
 *
 * <h3>The definitional contact is excluded</h3>
 *
 * At a solar return the Sun is on its natal degree <i>by construction</i>, and at a lunar
 * return the Moon is. Reporting that as a contact would hand every chart a guaranteed witness
 * on its Sun every year, which is the "fires constantly" failure the whole layer is built to
 * avoid. It is a definition, not a finding.
 */
public final class Returns {

    /**
     * Lunar returns are off by default for the convergence family.
     *
     * The spec calls them "monthly. Noisy; optional", and thirteen returns a year against one
     * is exactly the volume asymmetry that would drown the solar return. Computed on request;
     * not fed to the ranking unless a caller asks.
     */
    public static boolean includeLunarInConvergence = false;

    /** A return moment and the chart cast for it. */
    public static final class Return {
        /** "solar", "lunar", or the lowercased body name for a planetary return. */
        public String kind;
        /**
         * The body that came back to its natal degree: "Sun", "Moon", "Venus", ...
         *
         * Held explicitly rather than inferred from `kind`, because the one contact a
         * return must NOT report is that body meeting itself - that is the definition of
         * the moment, not a finding about it - and deriving it from a string meant the
         * test read "is this the Moon" for every kind that was not solar.
         */
        public String body;
        public double jd;
        public ChartFrame chart;
        /**
         * Where the return was cast, so a relocated one can say so.
         *
         * A return chart's angles are the whole of its new information, and the angles are
         * the part that moves when the native does. Without this a return cast for the
         * natal place and one cast for wherever the native actually was are the same object
         * with different numbers in it, and nothing downstream can tell the reader which
         * claim is on the screen.
         */
        public double lat = Double.NaN;
        public double lon = Double.NaN;
        /** Age in completed years for a solar return; the index within the window for lunar. */
        public int ordinal;
        /**
         * True when the return was taken to the body's <i>sidereal</i> natal degree - the
         * precessed return - rather than to its tropical one. See {@link #precessionDegrees}.
         */
        public boolean precessed;
        /**
         * Degrees of precession carried into the target longitude; zero for a tropical return.
         *
         * Held rather than recomputed so a reading can say how much later this return is, and a
         * check can hold it to {@link Precession#between} without redoing the search.
         */
        public double precessionDegrees;
        /** True when the return was cast somewhere other than the birthplace. */
        public boolean relocated;

        @Override
        public String toString() {
            return String.format("%s return, %s rising",
                kind, chart == null ? "?" : Zodiac.format(chart.asc));
        }
    }

    /** A point of the return chart landing on a natal point. */
    public static final class Contact {
        /** The return-chart point: a body name, or "Ascendant" / "MC". */
        public String returnPoint;
        public String natal;
        public Aspects.Type type;
        /** The return moment - every contact of one return shares it. */
        public double jd;
        public double offBy;
        public double orbUsed;
        /**
         * What this contact is worth, on K8's hierarchy.
         *
         * <b>Sorting by orb alone made a return read as though its minor bodies ran it.</b>
         * Tightness is a measurement, not an importance: Pholus exactly on the Sun printed
         * above Mars conjunct the Sun two degrees off, and a return whose first five lines
         * were Pholus, Chiron, Eris and Nessus said, by placement, that those were the year.
         * Same weighting the convergence engine uses, so the two surfaces rank alike.
         */
        public double weight;
        public String kind;
        public String why;
        public int natalRank = -1;

        @Override
        public String toString() {
            return String.format("%s return %s %s natal %s, %.2f° off",
                kind, returnPoint, type.label.toLowerCase(), natal, offBy);
        }
    }

    private Returns() { }

    /**
     * The solar return for a given completed age.
     *
     * @param lat latitude and
     * @param lon longitude the return is cast for. The natal place by default in this app;
     *            relocated returns are a real technique but a different claim, and mixing the
     *            two silently would make the angles - the only part that is new information -
     *            mean two things at once.
     */
    public static Return solar(SwissEph sw, double natalJd, double natalSunLon, int forAge,
                               double lat, double lon, int hsys) {
        return solar(sw, natalJd, natalSunLon, forAge, lat, lon, hsys, false);
    }

    /**
     * The solar return, tropical or precessed - master list F5.
     *
     * <b>What precessed means, and why it is solved twice.</b> A tropical return is the moment the
     * Sun regains the longitude it held at birth. A precessed return is the moment it regains the
     * same place <i>against the stars</i>, so the target moves by the precession accumulated since
     * birth - about 50.3 arcseconds a year, which is roughly twenty minutes of clock per year of
     * age. The amount depends on when the return falls and the return depends on the amount, so
     * the tropical return is solved first and the target corrected from it; one further pass is
     * enough, because the correction moves the answer by hours and the precession across those
     * hours is measured in thousandths of an arcsecond.
     *
     * Both are in use and neither is the return, so the caller says which and the reading names it.
     * David's call, 2026-09-15: tropical by default.
     */
    public static Return solar(SwissEph sw, double natalJd, double natalSunLon, int forAge,
                               double lat, double lon, int hsys, boolean precessed) {
        double jd = Profection.solarReturnJd(sw, natalJd, natalSunLon, forAge);
        if (Double.isNaN(jd)) {
            return null;
        }
        double carried = 0.0;
        if (precessed) {
            for (int pass = 0; pass < 2; pass++) {
                carried = Precession.between(natalJd, jd);
                double target = Zodiac.normalise(natalSunLon + carried);
                double next = Profection.solarReturnJd(sw, natalJd, target, forAge);
                if (Double.isNaN(next)) {
                    return null;
                }
                jd = next;
            }
        }
        Return r = new Return();
        r.kind = "solar";
        r.lat = lat;
        r.lon = lon;
        r.body = "Sun";
        r.jd = jd;
        r.ordinal = forAge;
        r.precessed = precessed;
        r.precessionDegrees = carried;
        r.chart = ChartFrame.compute(sw, jd, lat, lon, hsys, false, 0.0);
        return r;
    }

    /**
     * Every lunar return inside the window: the Moon back on its natal degree, about every
     * 27.3 days.
     *
     * Uses the Almanac root finder rather than stepping by 27.3 days and hoping. A step of one
     * day is safe - the Moon covers about 13 degrees a day, so it cannot cross its natal
     * degree and return between samples.
     */
    public static List<Return> lunar(SwissEph sw, double natalJd, double natalMoonLon,
                                     double jdFrom, double jdTo,
                                     double lat, double lon, int hsys) {
        return lunar(sw, natalJd, natalMoonLon, jdFrom, jdTo, lat, lon, hsys, false);
    }

    /** As {@link #lunar}, tropical or precessed - the target degree moves with the equinox. */
    public static List<Return> lunar(SwissEph sw, double natalJd, double natalMoonLon,
                                     double jdFrom, double jdTo,
                                     double lat, double lon, int hsys, boolean precessed) {
        List<Return> out = new ArrayList<>();
        // The Moon covers thirteen degrees a day, so the correction cannot move a return past the
        // next one and the target is taken from the middle of the window rather than solved per
        // crossing: across a year the two differ by under a tenth of a second of clock.
        final double carried = precessed
            ? Precession.between(natalJd, (jdFrom + jdTo) / 2.0) : 0.0;
        final double target = Zodiac.normalise(natalMoonLon + carried);
        Almanac.OfTime f = jd -> Almanac.signedDelta(
            Almanac.bodyLongitude(sw, jd, "Moon"), target);
        int i = 0;
        for (double jd : Almanac.roots(f, jdFrom, jdTo, 1.0)) {
            Return r = new Return();
            r.kind = "lunar";
            r.lat = lat;
            r.lon = lon;
            r.body = "Moon";
            r.jd = jd;
            r.ordinal = i++;
            r.precessed = precessed;
            r.precessionDegrees = carried;
            r.chart = ChartFrame.compute(sw, jd, lat, lon, hsys, false, 0.0);
            out.add(r);
        }
        return out;
    }

    /**
     * Bodies that get their own return chart, and the scan step in days for finding one.
     *
     * The step has to be small enough that the body cannot leave its natal degree and come
     * back between two samples. The binding case is not the body's average motion but its
     * retrograde loop, where it crosses the same degree three times in a few weeks: Venus
     * turns around inside about 43 days, Mars about 72. These steps sample the tightest of
     * those loops a dozen times over.
     *
     * The Sun and Moon are deliberately absent. They have their own entry points above -
     * the solar return is indexed by age and the lunar by position in a window, and folding
     * them in here would mean pretending those two indices are the same thing.
     */
    private static final java.util.Map<String, Double> RETURN_SCAN_STEP =
        new java.util.LinkedHashMap<>();
    static {
        RETURN_SCAN_STEP.put("Mercury", 1.0);
        RETURN_SCAN_STEP.put("Venus", 2.0);
        RETURN_SCAN_STEP.put("Mars", 2.0);
        RETURN_SCAN_STEP.put("Jupiter", 4.0);
        RETURN_SCAN_STEP.put("Saturn", 4.0);
    }

    /** How much of a return chart is worth reading. */
    public enum Scope {
        /**
         * A chart in its own right: own house cusps, its own angles, relocatable to wherever
         * the native actually is when the return falls.
         */
        FULL_WHEEL,
        /**
         * The return's angles only, laid over the natal wheel as trigger nodes.
         *
         * The wheel is still computed - the angles have to come from somewhere - but nothing
         * asks the reader to treat it as a standalone chart.
         */
        ANGLES_ONLY
    }

    /**
     * How much of this body's return to read.
     *
     * <b>Selective hierarchy: a return is worth a whole chart when its period is long enough
     * for that chart to describe.</b> A solar return governs a year and a lunar return a
     * month, so each has room for twelve houses of its own. Mercury, Venus and Mars return
     * roughly annually but their returns are moments rather than chapters; casting twelve
     * houses over one gives it a structure it does not have, and three extra full wheels a
     * year is the crowding this app keeps deciding against. Their angles still matter, so
     * they arrive as trigger nodes on the natal wheel.
     *
     * <b>Jupiter and Saturn the decision does not name</b>, and they are returnable here.
     * They read as full wheels: a Saturn return is the canonical example of a return that IS
     * a chapter, and at one every twelve and twenty-nine years there is no crowding argument
     * against them. Flagged rather than assumed - if that is wrong it is a one-line change.
     */
    public static Scope scopeOf(String body) {
        if (body == null) {
            return Scope.FULL_WHEEL;
        }
        switch (body) {
            case "Mercury":
            case "Venus":
            case "Mars":
                return Scope.ANGLES_ONLY;
            default:
                return Scope.FULL_WHEEL;
        }
    }

    /** Whether a planetary return can be computed for this body. */
    public static boolean canReturn(String body) {
        return RETURN_SCAN_STEP.containsKey(body);
    }

    /** The bodies {@link #planetary} accepts, in order. */
    public static java.util.Set<String> returnableBodies() {
        return java.util.Collections.unmodifiableSet(RETURN_SCAN_STEP.keySet());
    }

    /**
     * Every return of one planet inside the window: the body back on its natal degree.
     *
     * The same root-find as the lunar return, which is the point - a return is one idea, not
     * five, and Venus coming home is the Moon coming home with a different period. Uses the
     * Almanac solver rather than stepping by the body's period, because the period is not
     * constant and the error compounds.
     *
     * **A retrograde loop over the natal degree yields three crossings, and all three are
     * returned.** They are real: the body genuinely arrives, backs off, and arrives again.
     * Which of the three an astrologer reads as "the" return is a judgement this does not
     * make - see the note in the work plan. Callers wanting one moment should take the
     * first; callers wanting the passage should take the span from first to last.
     *
     * **Do not start the window at the natal moment.** The body is on its natal degree at
     * birth by definition, so a retrograde loop in the months after it crosses that degree
     * again and those crossings are reported as returns. Measured on a 1990 chart: Saturn
     * gives crossings at +0.29 and +0.74 years - the tail of the birth crossing - before the
     * real return at +29.84 years, which is right against its 29.46-year period. Start the
     * window past the body's first retrograde loop, or drop crossings inside one period.
     *
     * @param body   one of {@link #returnableBodies()}
     * @param lat    latitude and
     * @param lon    longitude the return is cast for, on the same convention as the solar
     *               return: the natal place, not a relocation.
     * @return the returns in time order, empty if the body is not returnable
     */
    public static List<Return> planetary(SwissEph sw, String body, double natalLon,
                                         double jdFrom, double jdTo,
                                         double lat, double lon, int hsys) {
        return planetary(sw, body, natalLon, jdFrom, jdTo, lat, lon, hsys, false, Double.NaN);
    }

    /**
     * As {@link #planetary}, tropical or precessed.
     *
     * @param natalJd the birth moment, needed only to measure the precession from; pass NaN with
     *                {@code precessed} false when there is nothing to measure.
     */
    public static List<Return> planetary(SwissEph sw, String body, double natalLon,
                                         double jdFrom, double jdTo,
                                         double lat, double lon, int hsys,
                                         boolean precessed, double natalJd) {
        List<Return> out = new ArrayList<>();
        Double step = RETURN_SCAN_STEP.get(body);
        if (step == null) {
            return out;
        }
        final double carried = precessed && !Double.isNaN(natalJd)
            ? Precession.between(natalJd, (jdFrom + jdTo) / 2.0) : 0.0;
        final double target = Zodiac.normalise(natalLon + carried);
        Almanac.OfTime f = jd -> Almanac.signedDelta(
            Almanac.bodyLongitude(sw, jd, body), target);
        int i = 0;
        for (double jd : Almanac.roots(f, jdFrom, jdTo, step)) {
            Return r = new Return();
            r.kind = body.toLowerCase();
            r.lat = lat;
            r.lon = lon;
            r.body = body;
            r.jd = jd;
            r.ordinal = i++;
            r.precessed = precessed;
            r.precessionDegrees = carried;
            r.chart = ChartFrame.compute(sw, jd, lat, lon, hsys, false, 0.0);
            out.add(r);
        }
        return out;
    }

    /**
     * Return-chart points landing on significant natal points.
     *
     * Same natal-end filter as every other technique, and the same orb ceiling as eclipses
     * and stations ({@link Transits#eventMaxOrb}) - a return is the same shape of thing, a
     * dated moment whose contacts are read with a tight orb, so it takes the same rule rather
     * than a new number.
     */
    public static List<Contact> contacts(Return ret, ChartFrame natal,
                                         List<BodyScore.Vector> ranked, String lord, int topN) {
        List<Contact> out = new ArrayList<>();
        if (ret == null || ret.chart == null || natal == null) {
            return out;
        }
        List<Transits.NatalTarget> targets =
            Transits.significantTargets(natal, ranked, lord, topN);

        List<String> names = new ArrayList<>();
        List<Double> lons = new ArrayList<>();
        // An angles-only return contributes its two angles and nothing else - see scopeOf.
        // The bodies of a Venus return are just the sky on that day, which the transit engine
        // already reports; what the return adds that transits cannot is where its angles fall.
        boolean anglesOnly = scopeOf(ret.body) == Scope.ANGLES_ONLY;
        for (int bi = 0; !anglesOnly && bi < ret.chart.bodies.length && bi < Bodies.count(); bi++) {
            ChartFrame.Body b = ret.chart.bodies[bi];
            // Angles are skipped here and added explicitly below. Before the Bodies
            // registry, `bodies` held twelve planets and the angles lived in their own
            // fields; on 2026-08-19 it became all 28 points and this loop silently began
            // picking them up. The Ascendant and MC then arrived TWICE, and the Descendant
            // and IC arrived at all - which is the same axis reported a second time with
            // the aspect flipped, exactly what the comment below says must not happen.
            if (b != null && b.ok && !Bodies.at(bi).isAngle()) {
                names.add(b.name);
                lons.add(b.lon);
            }
        }
        // The two independent angles. Descendant and IC would repeat every contact with the
        // aspect flipped, the same reason SolarArc directs only these two.
        names.add("Ascendant");
        lons.add(ret.chart.asc);
        names.add("MC");
        lons.add(ret.chart.mc);

        // -------------------------------------------------------------
        // BOOLEAN GATES (Morin's restrictions)
        // 1. Return Angles
        // 2. Lord of the Year
        // 3. Replicated Natal Aspects
        // -------------------------------------------------------------
        java.util.Set<String> allowedPlanets = new java.util.HashSet<>();
        if (lord != null) {
            allowedPlanets.add(lord);
        }
        
        double retAsc = ret.chart.asc;
        double retMc = ret.chart.mc;
        double retDsc = (retAsc + 180.0) % 360.0;
        double retIc = (retMc + 180.0) % 360.0;
        
        for (ChartFrame.Body b : ret.chart.bodies) {
            if (b == null || !b.ok) continue;
            // Conjunct a return angle
            if (Aspects.separation(b.lon, retAsc) <= Aspects.ANGLE_ORB ||
                Aspects.separation(b.lon, retMc) <= Aspects.ANGLE_ORB ||
                Aspects.separation(b.lon, retDsc) <= Aspects.ANGLE_ORB ||
                Aspects.separation(b.lon, retIc) <= Aspects.ANGLE_ORB) {
                allowedPlanets.add(b.name);
            }
        }
        
        List<Aspects.Hit> returnHits = Aspects.betweenBodies(ret.chart);
        List<Aspects.Hit> natalHits = Aspects.betweenBodies(natal);
        for (Aspects.Hit rHit : returnHits) {
            for (Aspects.Hit nHit : natalHits) {
                if (rHit.a.equals(nHit.a) && rHit.b.equals(nHit.b) && rHit.type == nHit.type) {
                    allowedPlanets.add(rHit.a);
                    allowedPlanets.add(rHit.b);
                }
            }
        }

        for (int i = 0; i < names.size(); i++) {
            for (Transits.NatalTarget target : targets) {
                if (isDefinitional(ret.body, names.get(i), target.name)) {
                    continue;
                }
                double sep = Aspects.separation(lons.get(i), target.lon);
                Aspects.Type type = Aspects.typeOf(sep, names.get(i), target.name);
                if (type == null) {
                    continue;
                }
                double orb = Math.min(
                    Aspects.orbFor(names.get(i), target.name) * Transits.eventOrbScale,
                    Transits.eventMaxOrb);
                double off = Math.abs(sep - type.exactAngle);
                if (off > orb) {
                    continue;
                }
                
                // Final Boolean Gate Check (including Partile Conjunction rule)
                boolean isReturnAngle = "Ascendant".equals(names.get(i)) || "MC".equals(names.get(i));
                boolean isAllowed = isReturnAngle || allowedPlanets.contains(names.get(i));
                if (!isAllowed) {
                    if (type == Aspects.Type.CONJUNCTION && off <= Aspects.PARTILE_ORB) {
                        isAllowed = true;
                    }
                }
                if (!isAllowed) {
                    continue;
                }

                Contact c = new Contact();
                c.returnPoint = names.get(i);
                c.natal = target.name;
                c.type = type;
                c.jd = ret.jd;
                c.offBy = off;
                c.orbUsed = orb;
                c.kind = ret.kind;
                c.why = target.why;
                c.natalRank = target.rank;
                // Precision off orbUsed, not type.maxOrb: CONJUNCTION.maxOrb is
                // Double.MAX_VALUE, and dividing by it makes every conjunction look exact.
                double slack = orb > 0.0 ? 1.0 - (off / orb) : 1.0;
                slack = slack < 0.0 ? 0.0 : slack > 1.0 ? 1.0 : slack;
                c.weight = Convergence.bodyWeight(c.returnPoint)
                    * Convergence.bodyWeight(c.natal)
                    * Convergence.aspectWeight(type)
                    * (0.25 + 0.75 * slack);
                out.add(c);
            }
        }
        out.sort(Comparator.comparingDouble((Contact c) -> -c.weight)
            .thenComparingDouble(c -> c.offBy)
            .thenComparing(c -> c.natal));
        return out;
    }

    /** Convenience overload with the standard filter settings. */
    public static List<Contact> contacts(Return ret, ChartFrame natal,
                                         List<BodyScore.Vector> ranked, String lord) {
        return contacts(ret, natal, ranked, lord, Transits.defaultTopN);
    }

    /**
     * The contact a return has by definition rather than by circumstance.
     *
     * At a solar return the Sun is on its natal degree because that is what defines the
     * moment; likewise the Moon at a lunar return. Reporting it would give every chart a free
     * witness on its own light, every year.
     */
    /**
     * The contact that IS the return rather than a finding about it: the returning body
     * conjunct its own natal place. True by construction at every return, so reporting it
     * would put one guaranteed line in every chart.
     *
     * Reads the body directly. It used to switch on `kind` and fall through to "Moon" for
     * anything that was not "solar", which was correct while those were the only two kinds
     * and would have quietly let a Venus return report Venus-on-Venus as news.
     */
    private static boolean isDefinitional(String body, String returnPoint, String natalPoint) {
        return body != null && body.equals(returnPoint) && body.equals(natalPoint);
    }
}
