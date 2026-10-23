package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.Zodiac;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Arabic Lots in transit, read by David's model (2026-10-22).
 *
 * <b>The model is his.</b> A Lot is a calculated point that ties the Sun, the Moon and the
 * Ascendant together, not a body, so a transit reading of one joins its fixed natal capacity to
 * what moves now. He named three contacts and three layers:
 * <ul>
 *   <li><b>A. A transiting planet reaching a natal Lot</b> - the planet is the weather, forcing or
 *       easing what the Lot is for, in its sign and house.</li>
 *   <li><b>B. The current sky's Lot reaching a natal planet or angle</b> - a short window that
 *       touches what that planet does.</li>
 *   <li><b>C. A transit to the ruler of the natal Lot's sign</b> - it reaches the Lot through its
 *       ruler, "the plumbing".</li>
 * </ul>
 * Every card says the root (the natal Lot in its sign and part of life), the weather (what is
 * moving, the aspect, how close) and the domain (where it shows, and what to do).
 *
 * <b>The words are parts, as the transit card's are:</b> what each Lot is for is
 * {@code lot.<id>} in transit_cards.json; the planet, the aspect and what to do are the transit
 * card's own lines; the Lot in its sign is the natal corpus's summary line.
 */
final class LotTransits {

    private LotTransits() {
    }

    /** The Lots the app casts. */
    static final List<String> LOTS = List.of("Part of Fortune", "Part of Spirit",
        "Lot of Eros", "Lot of Necessity", "Lot of Courage", "Lot of Victory", "Lot of Nemesis");

    /** The sky's movers for A and C: the planets and the slow points, not the Moon (hours). */
    static final List<String> MOVERS = List.of("Sun", "Mercury", "Venus", "Mars", "Jupiter",
        "Saturn", "Uranus", "Neptune", "Pluto", "Chiron", "North Node");

    /** The natal targets for B: the planets and the two main angles. */
    static final List<String> TARGETS = List.of("Sun", "Moon", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Ascendant", "MC");

    /** The modern co-ruler, beside the traditional one (David's example: Scorpio, Mars and Pluto). */
    static final Map<Integer, String> MODERN = Map.of(7, "Pluto", 10, "Uranus", 11, "Neptune");

    /** How close a planet must be to a Lot, and a Lot to a planet: a point takes a tight orb. */
    static final double ORB = 3.0;
    /** The sky's Lots turn with the Ascendant, about a degree every four minutes. */
    static final double LOT_ORB = 2.0;

    /**
     * How many cards each contact shows. A share each, because the three are different
     * questions: with one cap of eight the planets on the Lots filled seven of them, and the
     * rulers and the sky's own Lots - two of David's three mechanisms - all but vanished.
     */
    static final Map<Kind, Integer> SHARE = Map.of(Kind.TO_LOT, 4, Kind.RULER, 3,
        Kind.LOT_TO, 2);

    private enum Kind { TO_LOT, LOT_TO, RULER }

    private record Contact(Kind kind, String mover, String lot, String target,
            Aspects.Type type, double off, boolean building) {
    }

    /** The five majors, nearest first: the aspect two longitudes make, or null. */
    private static Aspects.Type major(double a, double b, double orb) {
        double sep = Aspects.separation(a, b);
        Aspects.Type best = null;
        double bestOff = orb;
        for (Aspects.Type t : new Aspects.Type[] {Aspects.Type.CONJUNCTION,
                Aspects.Type.SEXTILE, Aspects.Type.SQUARE, Aspects.Type.TRINE,
                Aspects.Type.OPPOSITION}) {
            double off = Math.abs(sep - t.exactAngle);
            if (off <= bestOff) {
                best = t;
                bestOff = off;
            }
        }
        return best;
    }

    private static double off(double a, double b, Aspects.Type t) {
        return Math.abs(Aspects.separation(a, b) - t.exactAngle);
    }

    /** A point's longitude in a chart, the angles included; NaN when it is not cast. */
    private static double lon(ChartFrame f, String name) {
        switch (name) {
            case "Ascendant": return f.asc;
            case "MC": return f.mc;
            default: break;
        }
        ChartFrame.Body b = f.body(name);
        return b == null || !b.ok ? Double.NaN : b.lon;
    }

    private static double speed(ChartFrame f, String name) {
        ChartFrame.Body b = f.body(name);
        return b == null ? 0.0 : b.lonSpeed;
    }

    /** The Lot contacts now, strongest first: A, then C, then B, each slowest and tightest first. */
    static List<ReadingCard> cards(ChartFrame natal, ChartFrame sky) {
        List<ReadingCard> out = new ArrayList<>();
        if (natal == null || sky == null) {
            return out;
        }
        List<Contact> found = new ArrayList<>();
        for (String lot : LOTS) {
            double l = lon(natal, lot);
            if (Double.isNaN(l)) {
                continue;
            }
            // A. A planet now on the natal Lot.
            for (String p : MOVERS) {
                double m = lon(sky, p);
                Aspects.Type t = Double.isNaN(m) ? null : major(m, l, ORB);
                if (t != null) {
                    found.add(new Contact(Kind.TO_LOT, p, lot, lot, t, off(m, l, t),
                        Aspects.isApplying(m, speed(sky, p), l, 0.0, t)));
                }
            }
            // C. A planet now on the ruler of the Lot's sign.
            int sign = Zodiac.signIndex(l);
            List<String> rulers = new ArrayList<>();
            rulers.add(Dignity.domicileRulerOf(sign));
            if (MODERN.containsKey(sign)) {
                rulers.add(MODERN.get(sign));
            }
            for (String r : rulers) {
                double rl = lon(natal, r);
                if (Double.isNaN(rl)) {
                    continue;
                }
                for (String p : MOVERS) {
                    double m = lon(sky, p);
                    Aspects.Type t = Double.isNaN(m) ? null : major(m, rl, ORB);
                    if (t != null) {
                        found.add(new Contact(Kind.RULER, p, lot, r, t, off(m, rl, t),
                            Aspects.isApplying(m, speed(sky, p), rl, 0.0, t)));
                    }
                }
            }
            // B. The sky's own Lot on a natal planet or angle.
            double sl = lon(sky, lot);
            if (!Double.isNaN(sl)) {
                for (String target : TARGETS) {
                    double tl = lon(natal, target);
                    Aspects.Type t = Double.isNaN(tl) ? null : major(sl, tl, LOT_ORB);
                    if (t != null) {
                        found.add(new Contact(Kind.LOT_TO, lot, lot, target, t, off(sl, tl, t),
                            false));
                    }
                }
            }
        }
        found.sort(Comparator.comparingInt((Contact c) -> c.kind() == Kind.TO_LOT ? 0
                : c.kind() == Kind.RULER ? 1 : 2)
            .thenComparingInt(c -> -slowness(c.mover()))
            .thenComparingDouble(Contact::off));
        Map<Kind, Integer> used = new java.util.EnumMap<>(Kind.class);
        for (Contact c : found) {
            if (used.getOrDefault(c.kind(), 0) >= SHARE.get(c.kind())) {
                continue;
            }
            ReadingCard card = card(natal, sky, c);
            if (card != null) {
                out.add(card);
                used.merge(c.kind(), 1, Integer::sum);
            }
        }
        return out;
    }

    private static int slowness(String body) {
        int i = List.of("Sun", "Mercury", "Venus", "Mars", "Jupiter", "North Node", "Chiron",
            "Saturn", "Uranus", "Neptune", "Pluto").indexOf(body);
        return i < 0 ? -1 : i;
    }

    /** One contact, said in three layers: root, weather, domain. */
    private static ReadingCard card(ChartFrame natal, ChartFrame sky, Contact c) {
        InterpretationService svc = InterpretationService.getInstance();
        String aspect = c.type().label.toLowerCase(Locale.ROOT);
        String verb = svc.getTransitCardPart("aspect." + aspect);
        String feel = svc.getTransitCardPart("feel." + aspect);
        String purpose = svc.getTransitCardPart("lot." + id(c.lot()));
        if (verb == null || feel == null || purpose == null) {
            return null;
        }
        double lotLon = lon(natal, c.lot());
        String lotSign = Zodiac.signName(lotLon);
        int lotHouse = Zodiac.houseOf(lotLon, natal.cusps);
        String lotArea = LifeAreas.of(lotHouse);
        // The root: the natal Lot in its sign, and the part of life it sits in.
        StringBuilder root = new StringBuilder("Your " + c.lot() + " is in "
            + capitalise(lotSign) + (lotArea == null ? "" : ", in " + lotArea) + ".");
        PlainSnapshot.Line line = PlainSnapshot.plain(svc.getPlanetInSign(c.lot(), lotSign));
        if (line != null) {
            root.append(' ').append(line.whole ? capitalise(line.text) : c.lot() + ": "
                + line.text).append(line.text.endsWith(".") ? "" : ".");
        }
        String closeness = String.format(Locale.ROOT, "%.1f", c.off()) + " degrees from exact";
        List<String> why = new ArrayList<>();
        switch (c.kind()) {
            case TO_LOT -> {
                String trigger = svc.getTransitCardPart("trigger." + id(c.mover()));
                String acts = svc.getTransitCardPart("acts." + id(c.mover()));
                String todo = svc.getTransitCardPart("todo." + id(c.mover()) + "." + aspect);
                if (trigger == null || acts == null || todo == null) {
                    return null;
                }
                why.add("transiting " + c.mover() + " " + aspect + " natal " + c.lot() + " in "
                    + capitalise(lotSign) + ", " + Zodiac.ordinal(lotHouse) + " house, "
                    + closeness + (c.building() ? ", applying" : ", separating"));
                return new ReadingCard(area(lotArea, "YOUR " + c.lot()),
                    trigger + " " + verb + " " + purpose,
                    root + "\n\n" + acts + " " + feel + " It is "
                        + (c.building() ? "building" : "fading") + ", " + closeness + "."
                        + "\n\n" + TransitCard.WHAT_TO_DO + todo, why);
            }
            case RULER -> {
                String trigger = svc.getTransitCardPart("trigger." + id(c.mover()));
                String acts = svc.getTransitCardPart("acts." + id(c.mover()));
                String todo = svc.getTransitCardPart("todo." + id(c.mover()) + "." + aspect);
                if (trigger == null || acts == null || todo == null) {
                    return null;
                }
                why.add("transiting " + c.mover() + " " + aspect + " natal " + c.target()
                    + ", ruler of " + capitalise(lotSign) + " where the natal " + c.lot()
                    + " is, " + closeness);
                return new ReadingCard(area(lotArea, "YOUR " + c.lot()),
                    trigger + " " + verb + " your " + c.target() + ", which runs your "
                        + c.lot(),
                    root + " Its sign is ruled by " + c.target() + ", so what reaches "
                        + c.target() + " reaches the " + c.lot() + " too.\n\n" + acts + " "
                        + feel + "\n\n" + TransitCard.WHAT_TO_DO + todo, why);
            }
            default -> {
                String need = svc.getTransitCardPart("need." + id(c.target()));
                String window = svc.getTransitCardPart("lot.window");
                if (need == null || window == null) {
                    return null;
                }
                int house = Zodiac.houseOf(lon(natal, c.target()), natal.cusps);
                String area = "Ascendant".equals(c.target()) ? LifeAreas.of(1)
                    : "MC".equals(c.target()) ? LifeAreas.of(10) : LifeAreas.of(house);
                double skyLot = lon(sky, c.lot());
                why.add("transiting " + c.lot() + " in " + capitalise(Zodiac.signName(skyLot))
                    + " " + aspect + " natal " + c.target() + ", " + closeness);
                return new ReadingCard(area(area, "THE SKY'S " + c.lot()),
                    "The sky's " + c.lot() + " " + verb + " " + need,
                    window + " The sky's " + c.lot() + " is about "
                        + purpose.replaceFirst("^your ", "") + ", and it now meets " + need
                        + ". " + feel, why);
            }
        }
    }

    private static String area(String area, String fallback) {
        return (area == null ? fallback : area).toUpperCase(Locale.ROOT);
    }

    private static String id(String name) {
        Bodies.Def d = Bodies.byName(name);
        return d == null ? "" : d.id;
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
