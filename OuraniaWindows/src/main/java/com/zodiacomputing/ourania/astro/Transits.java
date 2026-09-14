package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * L8: transits to the natal chart.
 *
 * The obvious timing technique and the least selective one. At any moment dozens of
 * transits are within orb, and everyone born the same year shares the outer-planet ones -
 * so an unfiltered transit list is the cookbook dump in temporal form, which is the exact
 * failure L7 exists to prevent.
 *
 * The spec's answer, and this class's whole design, is that <b>the filter is on the natal
 * end, not the transiting end</b>. A transit is worth reporting when the natal point it
 * lands on already matters:
 *
 *   - an angle          the chart's structural corners
 *   - a light           Sun or Moon
 *   - top-N prominence  the L3 ranking that drives every other reading
 *   - lord of the year  L8's own contribution, from Profection
 *
 * Every hit carries the reason it survived, so a reading can say why it is being told
 * about this and not the ninety others.
 *
 * Aspect geometry is not re-implemented here: orbs, type detection and applying all come
 * from Aspects, so a change to the orb table moves natal and transit readings together.
 */
public final class Transits {

    /** The four angles, named as Aspects.toAngles names them. */
    private static final String[] ANGLE_NAMES = {"Ascendant", "MC", "Descendant", "IC"};

    /**
     * Multiplier on the natal orb table for transit work.
     *
     * Deliberately 1.0. Transit practice usually wants tighter orbs than natal - often a
     * degree or two for the outers - but no value here has been calibrated against
     * anything, and picking one by eye is how weights got into the state the calibration
     * note describes. The seam exists so a measured answer has somewhere to land; until
     * then the orb table is the single source of truth and the filter does the selecting.
     */
    public static double orbScale = 1.0;

    /** How many natally prominent bodies count as significant targets. */
    public static int defaultTopN = 5;

    /**
     * Multiplier on the natal orb table for almanac events landing on natal points.
     *
     * Separate seam from orbScale, and 1.0 for the same reason: the L8 spec asks for eclipse
     * contacts "within a few degrees", which is tighter than the orb table gives, but no
     * number here has been measured against anything and picking one by eye is the failure
     * the calibration note records. Until it is calibrated the orb table stays the single
     * source of truth and the natal-end filter does the selecting - which is this class's
     * whole design anyway.
     */
    public static double eventOrbScale = 1.0;

    /**
     * Hard ceiling on the orb for an almanac event landing on a natal degree, in degrees.
     *
     * The orb table is built for aspects between two moving bodies and runs to about ten
     * degrees for the luminaries. Applying that to "did this eclipse fall on this degree" is
     * a category error, and the measurement says so: at the table's own orbs a single year
     * produces 92 contacts, 32 of them more than five degrees off, including a station
     * "square natal Sun" 9.65 degrees away. That is the wall of true, useless statements
     * this class exists to prevent.
     *
     * Three degrees is not a taste call - it is the L8 spec's own figure, which asks for
     * eclipse contacts "within a few degrees". It takes the same year from 92 contacts to
     * 36. Held as a field so a calibrated value has somewhere to land.
     */
    public static double eventMaxOrb = 3.0;

    /** Two exact moments closer than this are one perfection found twice, not two events. */
    private static final double SAME_EVENT_DAYS = 0.5;

    public static final class Hit {
        /** The moving body. */
        public String transiting;
        /** The natal point being hit: a body name or one of ANGLE_NAMES. */
        public String natal;
        public Aspects.Type type;
        public double separation;
        /** Degrees from exact. */
        public double offBy;
        public double orbUsed;
        /** 1.0 at exact, 0.0 at the edge of orb. */
        public double tightness;
        public boolean applying;
        public boolean partile;
        public boolean transitRetrograde;
        public double transitSpeed;
        /** Why the natal target qualified: "angle", "light", "prominence", "lord of the year". */
        public String why;
        /** Natal target's rank in the prominence ordering, or -1 for angles. */
        public int natalRank = -1;
        /** Weighted score based on orb tightness, phase (applying/separating), and station status. */
        public double intensity = 1.0;
        /**
         * What this contact is worth for ranking, on K8's hierarchy.
         *
         * <b>Deliberately separate from {@link #intensity}.</b> Convergence multiplies
         * intensity by its own body and aspect weights, so folding the hierarchy into
         * intensity would count it twice and quietly re-tune the whole predictive engine.
         * This field exists for ordering a list a person reads; intensity remains what it
         * always was.
         *
         * <b>What it fixes.</b> The sort ranked natal targets and then fell back to orb, so
         * the arriving body never entered it at all. On the default chart that put Eros
         * septile the Ascendant at position one and filled the entire angular block - the
         * most prominent group in the reading - with Eros, Pholus, Vesta, Chiron, Juno,
         * Eris, Ceres and Pallas before a single transiting planet appeared. Tightness was
         * standing in for importance, the same defect the return contacts had.
         */
        public double weight = 1.0;

        @Override
        public String toString() {
            return String.format("transiting %s%s %s natal %s (%.2f° off, %s) [%s]",
                transiting, transitRetrograde ? " Rx" : "", type.label.toLowerCase(),
                natal, offBy, applying ? "applying" : "separating", why);
        }
    }

    private Transits() { }

    /**
     * Transits from the transit frame to the natal frame, filtered to significant natal
     * targets.
     *
     * @param natal    the radix. Its positions are fixed points; they do not move.
     * @param transit  the moving sky.
     * @param ranked   natal prominence ordering from BodyScore.rank, or null to skip that
     *                 criterion.
     * @param lord     lord of the year from Profection, or null.
     * @param topN     how many of {@code ranked} count as significant.
     * @param includeTransitingMoon the Moon aspects everything within hours and clears
     *                 within a day. Off by default for the same reason Moon ingresses are
     *                 off in the annual calendar: it is volume, not signal - unless the
     *                 question is explicitly about the day.
     */
    public static List<Hit> toNatal(ChartFrame natal, ChartFrame transit,
                                    List<BodyScore.Vector> ranked, String lord, int topN,
                                    boolean includeTransitingMoon) {
        Set<String> prominent = prominentSet(ranked, topN);
        java.util.Map<String, Integer> rankOf = rankIndex(ranked);

        List<Hit> out = new ArrayList<>();
        for (ChartFrame.Body t : transit.bodies) {
            if (t == null || !t.ok) {
                continue;
            }
            if (!includeTransitingMoon && "Moon".equals(t.name)) {
                continue;
            }

            // Natal bodies.
            for (ChartFrame.Body n : natal.bodies) {
                if (n == null || !n.ok) {
                    continue;
                }
                String why = qualify(n.name, prominent, lord);
                if (why == null) {
                    continue;
                }
                Hit h = hit(t, n.name, n.lon, why);
                if (h != null) {
                    Integer r = rankOf.get(n.name);
                    h.natalRank = r == null ? -1 : r;
                    out.add(h);
                }
            }

            // Natal angles. Always significant - they are the chart's corners.
            double[] angleLons = {natal.asc, natal.mc, natal.dsc, natal.ic};
            for (int i = 0; i < ANGLE_NAMES.length; i++) {
                Hit h = hit(t, ANGLE_NAMES[i], angleLons[i], "angle");
                if (h != null) {
                    out.add(h);
                }
            }
        }

        // Most significant target first, then what is actually arriving at it, then orb.
        //
        // <b>The weight term is the fix; the grouping above it is deliberately kept.</b>
        // Angular contacts still lead and the natal targets still come in prominence order,
        // because a reading that groups every contact to one natal point together is easier
        // to read than one sorted purely by score. What changed is the tie-break inside each
        // group: it was orb, which let a septile from Eros outrank a square from Saturn
        // merely by being tighter. Ordering by weight first and orb second says that a
        // planet arriving matters more than a minor body arriving closer.
        out.sort(Comparator
            .comparingInt((Hit h) -> "angle".equals(h.why) ? 0 : 1)
            .thenComparingInt(h -> h.natalRank < 0 ? Integer.MAX_VALUE : h.natalRank)
            .thenComparingDouble(h -> -h.weight)
            .thenComparingDouble(h -> h.offBy));
        return out;
    }

    /** Convenience overload with the standard filter settings. */
    public static List<Hit> toNatal(ChartFrame natal, ChartFrame transit,
                                    List<BodyScore.Vector> ranked, String lord) {
        return toNatal(natal, transit, ranked, lord, defaultTopN, false);
    }

    /**
     * Why this natal body counts, or null if it does not.
     *
     * Order matters only for the label: a body can qualify several ways and the most
     * specific reason is the most informative one to show.
     */
    /** A natal point that survived the significance filter, with the reason it did. */
    public static final class NatalTarget {
        public final String name;
        public final double lon;
        public final String why;
        public final int rank;

        NatalTarget(String name, double lon, String why, int rank) {
            this.name = name;
            this.lon = lon;
            this.why = why;
            this.rank = rank;
        }
    }

    /**
     * The natal points worth reporting anything about: angles, lights, top-N prominence,
     * lord of the year.
     *
     * Public and shared because every L8 technique has to agree about what "matters" - a
     * point worth reporting a transit to is worth reporting an eclipse or a solar arc on.
     * This list was being rebuilt inline by each technique, which is the duplicated-rule
     * shape that keeps biting this project; there is now one copy.
     */
    public static List<NatalTarget> significantTargets(ChartFrame natal,
                                                       List<BodyScore.Vector> ranked,
                                                       String lord, int topN) {
        List<NatalTarget> out = new ArrayList<>();
        if (natal == null) {
            return out;
        }
        Set<String> prominent = prominentSet(ranked, topN);
        java.util.Map<String, Integer> rankOf = rankIndex(ranked);
        for (ChartFrame.Body n : natal.bodies) {
            if (n == null || !n.ok) {
                continue;
            }
            String why = qualify(n.name, prominent, lord);
            if (why == null) {
                continue;
            }
            Integer r = rankOf.get(n.name);
            out.add(new NatalTarget(n.name, n.lon, why, r == null ? -1 : r));
        }
        double[] angleLons = {natal.asc, natal.mc, natal.dsc, natal.ic};
        for (int i = 0; i < ANGLE_NAMES.length; i++) {
            out.add(new NatalTarget(ANGLE_NAMES[i], angleLons[i], "angle", -1));
        }
        return out;
    }

    /**
     * The two ends of an angular axis, keyed each way.
     *
     * The Ascendant and Descendant are one axis and so are the MC and IC: a body 180 degrees
     * from one is on the other, by construction and always.
     */
    private static String axisPartner(String angle) {
        if ("Ascendant".equals(angle)) {
            return "Descendant";
        }
        if ("Descendant".equals(angle)) {
            return "Ascendant";
        }
        if ("MC".equals(angle)) {
            return "IC";
        }
        if ("IC".equals(angle)) {
            return "MC";
        }
        return null;
    }

    /**
     * Drops the half of an axis contact that restates the other half.
     *
     * <b>Every transit to an angle was reported twice.</b> A body conjunct the MC is opposite
     * the IC at the same orb in the same instant, so significantTargets - which carries all
     * four angles, correctly, because they are four distinct points - produced two hits for
     * one event. A synthesis showed "Chiron Conjunction Natal MC" and "Chiron Opposition Natal
     * IC" a few lines apart, both true, both the same fact in different words, and the same
     * for Vesta, Pluto, Jupiter and Saturn down the page.
     *
     * The one kept is the one that names where the body actually is. A conjunction wins,
     * because "Chiron on your MC" says more than "Chiron opposite your IC" about the same
     * degree. When neither is a conjunction - a square hits both ends alike - the primary is
     * kept, which is arbitrary but has to be decided somewhere and is at least consistent.
     *
     * Only pairs at the same orb from the same body collapse. Two genuinely different
     * contacts to the two ends, at different orbs, are two events and both survive.
     */
    public static List<Hit> collapseAxisMirrors(List<Hit> hits) {
        if (hits == null || hits.isEmpty()) {
            return hits;
        }
        List<Hit> out = new ArrayList<>();
        for (Hit h : hits) {
            String partner = axisPartner(h.natal);
            boolean mirrored = false;
            if (partner != null) {
                for (Hit other : hits) {
                    if (other == h
                            || !partner.equals(other.natal)
                            || !other.transiting.equals(h.transiting)
                            || Math.abs(other.offBy - h.offBy) > 1.0e-6) {
                        continue;
                    }
                    boolean mineIsConjunction = h.type == Aspects.Type.CONJUNCTION;
                    boolean theirsIsConjunction = other.type == Aspects.Type.CONJUNCTION;
                    if (theirsIsConjunction && !mineIsConjunction) {
                        mirrored = true;
                    } else if (mineIsConjunction == theirsIsConjunction
                            && ("Descendant".equals(h.natal) || "IC".equals(h.natal))) {
                        mirrored = true;
                    }
                    if (mirrored) {
                        break;
                    }
                }
            }
            if (!mirrored) {
                out.add(h);
            }
        }
        return out;
    }

    /** The top-N prominence set, built once so both techniques select the same targets. */
    private static Set<String> prominentSet(List<BodyScore.Vector> ranked, int topN) {
        Set<String> prominent = new LinkedHashSet<>();
        if (ranked != null) {
            for (int i = 0; i < ranked.size() && i < topN; i++) {
                prominent.add(ranked.get(i).body);
            }
        }
        return prominent;
    }

    /** Body to its place in the prominence ordering. */
    private static java.util.Map<String, Integer> rankIndex(List<BodyScore.Vector> ranked) {
        java.util.Map<String, Integer> rankOf = new java.util.HashMap<>();
        if (ranked != null) {
            for (int i = 0; i < ranked.size(); i++) {
                rankOf.put(ranked.get(i).body, i);
            }
        }
        return rankOf;
    }

    private static String qualify(String natalBody, Set<String> prominent, String lord) {
        if (lord != null && lord.equals(natalBody)) {
            return "lord of the year";
        }
        if ("Sun".equals(natalBody) || "Moon".equals(natalBody)) {
            return "light";
        }
        if (prominent.contains(natalBody)) {
            return "prominence";
        }
        return null;
    }

    /**
     * The geometry half of a contact, with no opinion about what is moving.
     *
     * Shared by the transit path and the almanac-event path deliberately. Two copies of
     * "which aspect is this, and is it inside orb" is precisely the duplicated-rule defect
     * the work plan tracks, and it would show up as an eclipse being reported in orb while
     * the equivalent transit was not.
     *
     * Returns null when there is no aspect, or when there is one but it is outside orb.
     */
    private static final class Contact {
        Aspects.Type type;
        double separation;
        double offBy;
        double orbUsed;
        double tightness;
    }

    private static Contact contact(String movingName, double movingLon,
                                   String natalName, double natalLon,
                                   double scale, double maxOrb) {
        double sep = Aspects.separation(movingLon, natalLon);
        Aspects.Type type = Aspects.typeOf(sep, movingName, natalName);
        if (type == null) {
            return null;
        }
        double orb = Math.min(Aspects.orbFor(movingName, natalName) * scale, maxOrb);
        double off = Math.abs(sep - type.exactAngle);
        if (off > orb) {
            return null;                       // outside the scaled orb
        }
        Contact c = new Contact();
        c.type = type;
        c.separation = sep;
        c.offBy = off;
        c.orbUsed = orb;
        c.tightness = orb <= 0 ? 0.0 : Math.max(0.0, 1.0 - off / orb);
        return c;
    }

    private static Hit hit(ChartFrame.Body t, String natalName, double natalLon, String why) {
        // No ceiling on the transit path: the orb table is the rule there, unchanged.
        Contact c = contact(t.name, t.lon, natalName, natalLon,
            orbScale, Double.POSITIVE_INFINITY);
        if (c == null) {
            return null;
        }
        Hit h = new Hit();
        h.transiting = t.name;
        h.natal = natalName;
        h.type = c.type;
        h.separation = c.separation;
        h.orbUsed = c.orbUsed;
        h.offBy = c.offBy;
        h.tightness = c.tightness;
        // The natal point is a fixed degree - it has no speed. Only the transit moves.
        h.applying = Aspects.isApplying(t.lon, t.lonSpeed, natalLon, 0.0, c.type);
        h.partile = Aspects.isPartile(c.separation, c.type);
        h.transitRetrograde = t.retrograde;
        h.transitSpeed = t.lonSpeed;
        h.why = why;
        
        h.intensity = h.tightness * (h.applying ? 1.3 : 0.7);
        if (Math.abs(h.transitSpeed) <= 0.05) {
            h.intensity *= 5.0;
        }
        // <b>After the station boost, not before it.</b> A body that has stopped is the
        // strongest transit there is - it sits on the degree for weeks instead of crossing
        // it - and computing the ranking weight above this line silently threw that away.
        h.weight = h.intensity
            * Convergence.bodyWeight(h.transiting)
            * Convergence.bodyWeight(h.natal)
            * Convergence.aspectWeight(h.type);
        
        return h;
    }

    // ------------------------------------------------------------------ almanac events

    /**
     * An almanac event landing on a natal point that already matters.
     *
     * The spec lists eclipses and stations on natal degrees as high-signal and cheap, and
     * both are the same shape: a dated moment at a degree, asked whether that degree means
     * anything in this chart. So they share one type and one path rather than getting a
     * class each.
     */
    public static final class EventHit {
        public Almanac.Event event;
        /** The natal point being contacted: a body name or one of ANGLE_NAMES. */
        public String natal;
        public Aspects.Type type;
        /** Degrees from exact. */
        public double offBy;
        public double orbUsed;
        /** 1.0 at exact, 0.0 at the edge of orb. */
        public double tightness;
        /** Why the natal target qualified, same vocabulary as Hit.why. */
        public String why;
        public int natalRank = -1;

        @Override
        public String toString() {
            return String.format("%s — %s natal %s (%.2f° off) [%s]",
                event, type.label.toLowerCase(), natal, offBy, why);
        }
    }

    /**
     * Which almanac kinds are worth asking about a natal degree at all.
     *
     * Eclipses and stations. An ingress is a sign change, not a degree contact; a lunation
     * is already covered by the transiting Sun and Moon; a mundane aspect between two outer
     * planets is an event in the sky rather than one on this chart.
     *
     * <b>Direct stations are included, not only retrograde ones.</b> The spec's table names
     * the row "retrograde stations", but its own note says "a station on a natal point is a
     * long, loud transit" - and the reason it is loud is that the planet parks on that
     * degree for weeks, which is equally true at the direct station that ends the loop.
     * Reporting only half of each retrograde passage would describe the door closing and not
     * opening.
     */
    private static boolean contactable(Almanac.Kind kind) {
        return kind == Almanac.Kind.SOLAR_ECLIPSE
            || kind == Almanac.Kind.LUNAR_ECLIPSE
            || kind == Almanac.Kind.STATION_RETROGRADE
            || kind == Almanac.Kind.STATION_DIRECT;
    }

    /**
     * Almanac events filtered to the ones landing on natal points that already matter.
     *
     * Same filter as {@link #toNatal}, by construction rather than by resemblance: the
     * prominence set, the ranking and {@code qualify} are the same code. A natal point that
     * is worth reporting a transit to is worth reporting an eclipse on, and if those two
     * lists ever disagreed about what "matters" the reading would contradict itself.
     *
     * Sorted chronologically, unlike transits: these are dated moments, and a reader wants
     * to know what is coming when, not which is tightest.
     */
    public static List<EventHit> eventsToNatal(List<Almanac.Event> events, ChartFrame natal,
                                               List<BodyScore.Vector> ranked, String lord,
                                               int topN) {
        List<EventHit> out = new ArrayList<>();
        if (events == null || natal == null) {
            return out;
        }
        List<NatalTarget> targets = significantTargets(natal, ranked, lord, topN);
        for (Almanac.Event ev : events) {
            if (ev == null || !contactable(ev.kind)) {
                continue;
            }
            for (NatalTarget target : targets) {
                EventHit h = eventHit(ev, target.name, target.lon, target.why);
                if (h != null) {
                    h.natalRank = target.rank;
                    out.add(h);
                }
            }
        }
        out.sort(Comparator.comparingDouble((EventHit h) -> h.event.jd)
            .thenComparingDouble(h -> h.offBy));
        return out;
    }

    /** Convenience overload with the standard filter settings. */
    public static List<EventHit> eventsToNatal(List<Almanac.Event> events, ChartFrame natal,
                                               List<BodyScore.Vector> ranked, String lord) {
        return eventsToNatal(events, natal, ranked, lord, defaultTopN);
    }

    private static EventHit eventHit(Almanac.Event ev, String natalName, double natalLon,
                                     String why) {
        Contact c = contact(ev.body, ev.longitude, natalName, natalLon,
            eventOrbScale, eventMaxOrb);
        if (c == null) {
            return null;
        }
        EventHit h = new EventHit();
        h.event = ev;
        h.natal = natalName;
        h.type = c.type;
        h.offBy = c.offBy;
        h.orbUsed = c.orbUsed;
        h.tightness = c.tightness;
        h.why = why;
        return h;
    }

    // ------------------------------------------------------------------ perfections

    /**
     * A transit reaching exactness on a natal point, at a moment.
     *
     * The dated form of a transit, as against {@link Hit}, which is a condition holding at
     * the instant the chart is cast. Both are true and they answer different questions:
     * a Hit says "this is in orb now", a Perfection says "this became exact on this date".
     *
     * The convergence rule needs the dated form. Comparing an instantaneous condition
     * against eclipses and stations gathered over a year gave the dated families dozens of
     * chances to agree where transits got one, which inflated agreement to the point where a
     * two-family score selected eight targets out of ten.
     */
    public static final class Perfection {
        public String transiting;
        public String natal;
        public Aspects.Type type;
        /** Moment of exactness. */
        public double jd;
        public boolean retrograde;
        public String why;
        public int natalRank = -1;
        public double intensity;

        @Override
        public String toString() {
            return String.format("transiting %s%s %s natal %s exact",
                transiting, retrograde ? " Rx" : "", type.label.toLowerCase(), natal);
        }
    }

    /**
     * Every moment in the range at which a transit perfects on a significant natal point.
     *
     * Same natal-end filter as {@link #toNatal}, so the two agree about what matters, and
     * the same {@link #exactDates} root finding used for a single aspect - this is that
     * applied across every significant target rather than one.
     *
     * Note this deliberately reports <b>perfections</b> and not "in orb at some point during
     * the window". A slow body can sit within orb of a natal point for years without
     * perfecting, and reporting that as a witness for this particular year is the failure
     * this class exists to prevent. The perfection is the event; the orb is the weather.
     */
    public static List<Perfection> perfectionsOverRange(
            SwissEph sw, ChartFrame natal, List<BodyScore.Vector> ranked, String lord,
            double jdFrom, double jdTo, int topN, boolean includeTransitingMoon) {

        List<NatalTarget> targets = significantTargets(natal, ranked, lord, topN);

        List<Perfection> out = new ArrayList<>();
        for (String body : includeTransitingMoon ? withMoon() : yearMarkers) {
            // One longitude cache per body. The coarse scan asks for the same grid of times
            // once per natal target per aspect - 70 times over - and without this the scan
            // re-asks the ephemeris for every one of them. Measured at 35-40 seconds for a
            // year before caching, which is 25-44x the dated-moments scan beside it.
            LonCache cache = new LonCache(sw, body);
            double step = stepFor(body);
            for (NatalTarget target : targets) {
                for (Aspects.Type type : Aspects.Type.values()) {
                    for (double jd : perfections(cache, target.lon, type, jdFrom, jdTo, step)) {
                        Perfection p = new Perfection();
                        p.transiting = body;
                        p.natal = target.name;
                        p.type = type;
                        p.jd = jd;
                        p.why = target.why;
                        p.natalRank = target.rank;
                        double speed = speedOf(sw, jd, body);
                        p.retrograde = speed < 0.0;
                        p.intensity = 1.0;
                        if (Math.abs(speed) <= 0.05) {
                            p.intensity *= 5.0;
                        }

                        out.add(p);
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble((Perfection p) -> p.jd)
            .thenComparing(p -> p.natal));
        return out;
    }

    /** Convenience overload with the standard filter settings. */
    public static List<Perfection> perfectionsOverRange(
            SwissEph sw, ChartFrame natal, List<BodyScore.Vector> ranked, String lord,
            double jdFrom, double jdTo) {
        return perfectionsOverRange(sw, natal, ranked, lord, jdFrom, jdTo, defaultTopN, false);
    }

    /**
     * The transiting bodies whose perfections can mark a <i>particular</i> year.
     *
     * The Sun conjuncts every natal point once a year, without fail. Mercury and Venus do it
     * several times, Mars in most two-year spans. A transit guaranteed to recur annually
     * cannot distinguish this year from any other, which is the same objection the spec
     * raises against unfiltered transits - so as a witness for the convergence rule it is
     * worse than useless, because it votes for everything.
     *
     * Measured: with the fast bodies included, one profection year produced 371 perfections
     * on the significant natal points, and 331 of them were Sun, Mercury, Venus or Mars.
     * They would have made the transit family agree with every other family everywhere,
     * re-creating from the other direction exactly the inflation this widening set out to
     * remove.
     *
     * The boundary is "slower than about two years to return to a given aspect". Held as a
     * field so it can be argued with.
     */
    public static String[] yearMarkers = {
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron", "North Node"
    };

    private static String[] withMoon() {
        String[] out = new String[yearMarkers.length + 1];
        System.arraycopy(yearMarkers, 0, out, 0, yearMarkers.length);
        out[yearMarkers.length] = "Moon";
        return out;
    }

    /**
     * Memoised longitude of one body.
     *
     * {@link Almanac#roots} walks a fixed grid of times, and this scan runs it once per
     * natal target per aspect over the same grid, so the same instants are asked for dozens
     * of times. The coarse scan hits the cache; bisection asks for off-grid instants and
     * falls through to the ephemeris, which is what it should do.
     *
     * Keyed on the time rounded to a microsecond of a day, so accumulated floating-point
     * drift in the caller's stepping cannot turn a hit into a miss.
     */
    private static final class LonCache {
        private final SwissEph sw;
        private final String body;
        private final java.util.HashMap<Long, Double> memo = new java.util.HashMap<>();

        LonCache(SwissEph sw, String body) {
            this.sw = sw;
            this.body = body;
        }

        double at(double jd) {
            long key = Math.round(jd * 1.0e6);
            Double v = memo.get(key);
            if (v == null) {
                v = Almanac.bodyLongitude(sw, jd, body);
                memo.put(key, v);
            }
            return v;
        }
    }

    /**
     * Exact moments at which one body reaches one aspect to one fixed degree.
     *
     * The body of {@link #exactDates}, reading longitudes through a cache rather than
     * calling the ephemeris directly. Kept in step with it deliberately: same two targets
     * for the non-symmetric aspects, same collapse of near-coincident roots.
     */
    private static List<Double> perfections(LonCache cache, double natalLon,
                                            Aspects.Type type, double jdFrom, double jdTo,
                                            double step) {
        List<Double> targets = new ArrayList<>();
        targets.add(type.exactAngle);
        if (type.exactAngle != 0.0 && type.exactAngle != 180.0) {
            targets.add(-type.exactAngle);
        }
        List<Double> out = new ArrayList<>();
        for (double target : targets) {
            Almanac.OfTime f = jd -> Almanac.signedDelta(
                Almanac.signedDelta(cache.at(jd), natalLon), target);
            out.addAll(Almanac.roots(f, jdFrom, jdTo, step));
        }
        out.sort(Comparator.naturalOrder());

        List<Double> deduped = new ArrayList<>();
        for (double jd : out) {
            if (deduped.isEmpty() || jd - deduped.get(deduped.size() - 1) > SAME_EVENT_DAYS) {
                deduped.add(jd);
            }
        }
        return deduped;
    }

    /** Longitude speed of a body at a moment, for the retrograde flag. NaN if unknown. */
    private static double speedOf(SwissEph sw, double jd, String body) {
        int ipl = Almanac.iplOf(body);
        if (ipl < 0) {
            return Double.NaN;
        }
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        int flags = de.thmac.swisseph.SweConst.SEFLG_SWIEPH
            | de.thmac.swisseph.SweConst.SEFLG_SPEED;
        if (sw.swe_calc_ut(jd, ipl, Ephemeris.flags(sw, flags), xx, err) == de.thmac.swisseph.SweConst.ERR) {
            return Double.NaN;
        }
        return xx[3];
    }

    // ------------------------------------------------------------------ exact dates

    /**
     * Every moment in the range at which this transit is exact, including the extra
     * perfections a retrograde body makes when it backs over the same degree and returns.
     *
     * The spec asks for the date range of a hit including retrograde re-crossings, and a
     * slow outer planet routinely perfects the same aspect three times across several
     * months. Reporting only the first is how a transit reads as a day when it is a season.
     *
     * Reuses the Almanac root finder rather than bisecting again here.
     */
    public static List<Double> exactDates(SwissEph sw, String transitingBody,
                                          double natalLon, Aspects.Type type,
                                          double jdFrom, double jdTo) {
        // A square perfects at both +90 and -90 from the natal point, and likewise sextile
        // and trine. A conjunction and an opposition have ONE target each: 0 and -0 are the
        // same angle, as are 180 and -180.
        //
        // Note this cannot be a Set<Double>: Double.equals distinguishes 0.0 from -0.0, so
        // a set silently keeps both and every conjunction gets scanned - and reported -
        // twice. That is not a duplicate-looking bug, it is a duplicate.
        List<Double> targets = new ArrayList<>();
        targets.add(type.exactAngle);
        if (type.exactAngle != 0.0 && type.exactAngle != 180.0) {
            targets.add(-type.exactAngle);
        }

        List<Double> out = new ArrayList<>();
        for (double target : targets) {
            Almanac.OfTime f = jd -> Almanac.signedDelta(
                Almanac.signedDelta(Almanac.bodyLongitude(sw, jd, transitingBody), natalLon),
                target);
            out.addAll(Almanac.roots(f, jdFrom, jdTo, stepFor(transitingBody)));
        }
        out.sort(Comparator.naturalOrder());

        // Two targets can still bracket the same instant near a station; collapse those so
        // a caller counting perfections is counting events, not scans.
        List<Double> deduped = new ArrayList<>();
        for (double jd : out) {
            if (deduped.isEmpty() || jd - deduped.get(deduped.size() - 1) > SAME_EVENT_DAYS) {
                deduped.add(jd);
            }
        }
        return deduped;
    }

    /**
     * A safe coarse step for scanning one body against a FIXED point, in days.
     *
     * Wider than the Almanac's body-pair steps because only one end moves here: the natal
     * degree is stationary, so the relative motion is just the body's own.
     */
    private static double stepFor(String body) {
        switch (body) {
            case "Moon":                                    return 0.25;
            case "Sun": case "Mercury": case "Venus":        return 1.0;
            case "Mars":                                    return 2.0;
            case "Jupiter": case "Saturn":                  return 4.0;
            default:                                        return 6.0;
        }
    }

    /** Plain-text listing, tightest and most significant first. */
    public static String table(List<Hit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s %-12s %-11s %7s  %-10s %s%n",
            "transit", "aspect", "natal", "off", "motion", "why"));
        for (Hit h : hits) {
            sb.append(String.format("%-10s %-12s %-11s %6.2f°  %-10s %s%n",
                h.transiting + (h.transitRetrograde ? " Rx" : ""),
                h.type.label.toLowerCase(), h.natal, h.offBy,
                h.applying ? "applying" : "separating", h.why));
        }
        return sb.toString();
    }
}
