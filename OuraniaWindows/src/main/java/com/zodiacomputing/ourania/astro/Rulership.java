package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * L4: the house rulership web, and reception between bodies.
 *
 * The web is twelve edges - for each house, the ruler of its cusp sign, and where that
 * ruler actually sits:
 *
 *   house H  ->  ruler R  ->  R is in house H'
 *
 * The L4 spec calls this the highest-value structure in the layer and the one most
 * commonly skipped, and the reason is that it is what lets topics talk to each other.
 * Ruler of the 10th in the 11th says career arrives through networks; ruler of the 4th
 * in the 12th says home life is entangled with what is hidden. Without the web, house
 * interpretation is twelve disconnected paragraphs.
 *
 * Deliberately free of BodyScore. The spec asks for each ruler's full L3 condition, but
 * having L4 reach into L3 to fetch it would couple the layers in the direction they are
 * not supposed to run - BodyScore already consumes Gestalt, and a return edge invites a
 * cycle. Edges carry the ruler's NAME; a caller holding a ranked list joins on it, which
 * is a one-line lookup and keeps the dependency pointing one way.
 */
public final class Rulership {

    private Rulership() { }

    /** One edge of the web: a house, its ruler, and where that ruler is. */
    public static final class Edge {
        /** 1..12. */
        public int house;
        /** Sign on the cusp of that house. */
        public int cuspSign;
        /** Domicile ruler of the cusp sign. Never null for a well-formed chart. */
        public String ruler;
        /** Where the ruler sits, 1..12, or 0 if the ruler failed to compute. */
        public int rulerHouse;
        /** Whole-sign house of the ruler, which often differs from rulerHouse. */
        public int rulerWholeSignHouse;
        /** Sign the ruler is in, or -1. */
        public int rulerSign;
        public double rulerLongitude;
        public boolean rulerRetrograde;
        /** True when the ruler sits in the house it rules - the topic answers to itself. */
        public boolean rulerInOwnHouse;

        @Override
        public String toString() {
            return String.format("house %d (%s) -> %s in house %d (%s)%s",
                house, capitalise(Zodiac.SIGNS[cuspSign]), ruler, rulerHouse,
                rulerSign < 0 ? "?" : capitalise(Zodiac.SIGNS[rulerSign]),
                rulerInOwnHouse ? " [rules its own house]" : "");
        }
    }

    /**
     * One body sitting in another's dignity. Mutual when it runs both ways.
     *
     * The dignity matters as much as the fact: reception by domicile is a strong claim,
     * reception by face is nearly nothing, and reporting them alike would flatten a real
     * distinction the tradition draws.
     */
    public static final class Reception {
        public String a;
        public String b;
        /** The dignity of A's that B is sitting in - "domicile", "exaltation", ... */
        public String bInDignityOfA;
        /** The dignity of B's that A is sitting in, or null when it is one-way. */
        public String aInDignityOfB;
        public boolean mutual;
        /** Strength of the weaker leg, so a mutual reception is judged by its floor. */
        public int strength;

        @Override
        public String toString() {
            if (mutual) {
                return String.format("%s and %s in mutual reception (%s / %s)",
                    a, b, aInDignityOfB, bInDignityOfA);
            }
            // A one-way reception runs in whichever direction is non-null. Assuming it is
            // always A-receives-B printed "Moon receives Mars by null" - the wrong
            // direction AND a null - for every reception that happened to run B to A.
            return bInDignityOfA != null
                ? String.format("%s receives %s by %s", a, b, bInDignityOfA)
                : String.format("%s receives %s by %s", b, a, aInDignityOfB);
        }

        /** The one that does the receiving, for a one-way reception. */
        public String receiver() {
            return bInDignityOfA != null ? a : b;
        }

        /** The one being received. */
        public String received() {
            return bInDignityOfA != null ? b : a;
        }

        /** The dignity the reception runs by; for a mutual one, the weaker leg. */
        public String dignity() {
            if (!mutual) {
                return bInDignityOfA != null ? bInDignityOfA : aInDignityOfB;
            }
            return rank(bInDignityOfA) <= rank(aInDignityOfB) ? bInDignityOfA : aInDignityOfB;
        }
    }

    /**
     * The twelve edges, in house order.
     *
     * Uses the frame's quadrant cusps for the house of each ruler, and records the
     * whole-sign house alongside rather than choosing between them - L0 owns that choice
     * and L3 and L6 each sometimes want the other one.
     */
    public static List<Edge> web(ChartFrame f) {
        List<Edge> out = new ArrayList<>();
        for (int h = 1; h <= 12; h++) {
            Edge e = new Edge();
            e.house = h;
            e.cuspSign = Zodiac.signIndex(f.cusps[h]);
            e.ruler = Dignity.domicileRulerOf(e.cuspSign);
            e.rulerSign = -1;
            e.rulerHouse = 0;

            ChartFrame.Body r = f.body(e.ruler);
            if (r != null && r.ok) {
                e.rulerLongitude = r.lon;
                e.rulerSign = Zodiac.signIndex(r.lon);
                e.rulerHouse = Zodiac.houseOf(r.lon, f.cusps);
                e.rulerWholeSignHouse = Zodiac.wholeSignHouse(r.lon, f.asc);
                e.rulerRetrograde = r.retrograde;
                e.rulerInOwnHouse = e.rulerHouse == h;
            }
            out.add(e);
        }
        return out;
    }

    /**
     * Every reception among the traditional seven, mutual ones flagged, strongest first.
     *
     * Only the traditional seven participate: reception is a rulership relation and the
     * outer planets rule nothing in the scheme this engine uses, so including them would
     * invent relationships the tradition does not have.
     *
     * Expect roughly fifteen per chart, because every body is always inside SOME lord's
     * bound and face - those two account for most of the volume and carry least of the
     * meaning. Consumers should filter on {@code strength}: reception by domicile is a
     * real statement, reception by face is close to noise. The list is returned whole
     * rather than pre-filtered because where that line sits is a reading decision, not
     * a structural one.
     */
    public static List<Reception> receptions(ChartFrame f) {
        List<Reception> out = new ArrayList<>();
        List<String> bodies = new ArrayList<>();
        for (String n : Dignity.TRADITIONAL) {
            ChartFrame.Body b = f.body(n);
            if (b != null && b.ok) {
                bodies.add(n);
            }
        }

        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i + 1; j < bodies.size(); j++) {
                String a = bodies.get(i);
                String b = bodies.get(j);
                // "B is in a dignity of A" means A rules the place B occupies.
                String bInA = dignityOfOver(a, f.body(b).lon, f.diurnal);
                String aInB = dignityOfOver(b, f.body(a).lon, f.diurnal);
                if (bInA == null && aInB == null) {
                    continue;
                }
                Reception rec = new Reception();
                rec.a = a;
                rec.b = b;
                rec.bInDignityOfA = bInA;
                rec.aInDignityOfB = aInB;
                rec.mutual = bInA != null && aInB != null;
                rec.strength = rec.mutual
                    ? Math.min(rank(bInA), rank(aInB))
                    : rank(bInA != null ? bInA : aInB);
                out.add(rec);
            }
        }
        out.sort((x, y) -> Integer.compare(y.strength, x.strength));
        return out;
    }

    /**
     * Which dignity of {@code lord}, if any, covers the given longitude - strongest
     * first, so a body in both a lord's bound and its face reports the bound.
     */
    private static String dignityOfOver(String lord, double longitude, boolean diurnal) {
        int sign = Zodiac.signIndex(longitude);
        if (lord.equals(Dignity.domicileRulerOf(sign))) {
            return "domicile";
        }
        if (lord.equals(Dignity.exaltedBodyOf(sign))) {
            return "exaltation";
        }
        if (lord.equals(Dignity.triplicityRulerOf(sign, diurnal))) {
            return "triplicity";
        }
        if (lord.equals(Dignity.boundRulerOf(longitude))) {
            return "bound";
        }
        if (lord.equals(Dignity.faceRulerOf(longitude))) {
            return "face";
        }
        return null;
    }

    /** Lilly's common weighting, so receptions can be compared. */
    private static int rank(String dignity) {
        if (dignity == null) {
            return 0;
        }
        switch (dignity) {
            case "domicile":   return Dignity.PTS_DOMICILE;
            case "exaltation": return Dignity.PTS_EXALTATION;
            case "triplicity": return Dignity.PTS_TRIPLICITY;
            case "bound":      return Dignity.PTS_BOUND;
            case "face":       return Dignity.PTS_FACE;
            default:           return 0;
        }
    }

    /** Human-readable dump of the web, for the report tier and for eyeballing. */
    public static String table(List<Edge> web) {
        StringBuilder sb = new StringBuilder();
        for (Edge e : web) {
            sb.append("  ").append(e).append('\n');
        }
        return sb.toString();
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
