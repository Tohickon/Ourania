package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * L7 synthesis: where a chart's hard configurations discharge.
 *
 * Framework item 6.3, "dynamic tension vs resolution". {@link AspectPatterns} finds the
 * stressed geometries and records an apex; nothing until now said what relieves one. A
 * reading that lists a T-square and stops has named the pressure and withheld the answer.
 *
 * <b>This adds no numbers of its own.</b> Every aspect it reasons about is already in the
 * hit list {@link Aspects#betweenBodies} produced, at the orbs declared on
 * {@link Aspects.Type}; the only arithmetic here is the point opposite an apex, which needs
 * no orb. That is deliberate - a release rule with a threshold in it would be a reading
 * decision wearing a synthesis costume, and those are David's to take.
 *
 * Three channels, in the order the tradition ranks them:
 *
 * <ol>
 *   <li><b>An outlet</b> - a body outside the configuration trining or sextiling a member.
 *       An outlet touching the <i>apex</i> is the classical release of a T-square or a Yod:
 *       the focal planet gets somewhere to put the pressure.</li>
 *   <li><b>A mediator</b> - one outlet touching two or more members at once, so the same
 *       body softens both ends rather than one.</li>
 *   <li><b>The empty leg</b> - the degree opposite the apex, which a T-square leaves vacant
 *       by geometry. The tradition reads it as the house where the configuration asks to be
 *       worked out. A Yod's equivalent is its reaction point, and a body standing there
 *       makes the Yod a Boomerang.</li>
 * </ol>
 *
 * <b>Soft means trine or sextile, and only those.</b> The five minor aspects added on
 * 2026-08-23 are capped at 1.0 degrees and none of them is a release channel in any source
 * on the shelf; admitting them here would quietly widen what the engine calls a resolution.
 */
public final class TensionRelease {

    /** The configurations this reads as stressed, by {@link AspectPatterns.Pattern#name}. */
    private static final Set<String> STRESS_PATTERNS =
        new LinkedHashSet<>(List.of("T-square", "Grand cross", "Yod"));

    /** A soft aspect reaching into a stressed configuration from outside it. */
    public static final class Outlet {
        /** The body outside the configuration. */
        public final String via;
        /** The members it softens - more than one makes it a mediator. */
        public final List<String> to = new ArrayList<>();
        /** Trine or sextile, per member touched, parallel to {@link #to}. */
        public final List<Aspects.Type> types = new ArrayList<>();
        /** True when one of the members touched is the configuration's apex. */
        public boolean touchesApex;
        /** The tightest contact this body makes into the configuration, in degrees. */
        public double bestOffBy = Double.MAX_VALUE;

        Outlet(String via) {
            this.via = via;
        }

        public boolean isMediator() {
            return to.size() > 1;
        }

        /** "Jupiter, trine Mars" / "Jupiter, trine Mars and sextile Saturn". */
        public String phrase() {
            StringBuilder sb = new StringBuilder(via).append(", ");
            for (int i = 0; i < to.size(); i++) {
                if (i > 0) {
                    sb.append(i == to.size() - 1 ? " and " : ", ");
                }
                sb.append(types.get(i).label.toLowerCase()).append(' ').append(to.get(i));
            }
            return sb.toString();
        }
    }

    /** One stressed configuration and everything that discharges it. */
    public static final class Release {
        /** "T-square", "Grand cross", "Yod", or "Opposition" for an unpatterned one. */
        public String source;
        public final List<String> bodies = new ArrayList<>();
        /** The focal body, or null where the geometry has none. */
        public String apex;
        /** Outlets, apex first, then mediators, then tightest. */
        public final List<Outlet> outlets = new ArrayList<>();

        /** True where the geometry leaves a vacant point opposite the apex. */
        public boolean hasEmptyLeg;
        public double emptyLegLon;
        public String emptyLegSign;
        /** 1-12, or 0 where the house system degenerated at this latitude. */
        public int emptyLegHouse;
        /**
         * A body standing on the empty leg, or null.
         *
         * <b>A T-square's empty leg is nearly always empty and is not guaranteed to be.</b>
         * A fourth body opposite the apex normally squares both ends too, which makes the
         * figure a Grand cross rather than a T-square - but the orb is taken per pair, so a
         * wide body can oppose the apex while missing one of the squares. Measured rather
         * than assumed; the incidence is in TensionReleaseCheck Part D.
         */
        public String emptyLegOccupant;

        /** Members that also stand in a Grand trine or Kite - a release already built in. */
        public final List<String> softMembers = new ArrayList<>();

        /**
         * True when the configuration's opposition is a point and its own definition.
         *
         * The south node is the north node plus 180 always, which is why
         * {@link Bodies#oppositeOf} exists and why the aspect grid blanks the cell. A
         * T-square built on that spine is not three bodies in tension - it is one body square
         * the nodal axis, wearing a T-square's name because {@link AspectPatterns} takes its
         * oppositions from the hit list without asking where they came from.
         *
         * <b>Flagged rather than dropped.</b> The panel already lists these as aspect
         * patterns; suppressing the release line under a pattern the reader can see would
         * make the two surfaces disagree, and deciding they are not patterns at all is a
         * reading call. The incidence is in TensionReleaseCheck Part E.
         */
        public boolean definitionalSpine;

        /** The finished line. Never null, never contains a null. */
        public String sentence;

        /** True when nothing at all reaches this configuration. */
        public boolean unrelieved() {
            return outlets.isEmpty() && softMembers.isEmpty() && emptyLegOccupant == null;
        }
    }

    private TensionRelease() { }

    /**
     * Every stressed configuration in the chart with its release channels.
     *
     * Takes the patterns rather than finding them, so that this and {@link Gestalt} cannot
     * end up asking two different questions about the same chart. The project has already
     * paid for two implementations of one analysis once.
     */
    public static List<Release> find(ChartFrame f, List<Aspects.Hit> hits,
                                     List<AspectPatterns.Pattern> patterns) {
        List<Release> out = new ArrayList<>();

        for (AspectPatterns.Pattern p : patterns) {
            if (STRESS_PATTERNS.contains(p.name)) {
                out.add(build(f, hits, patterns, p.name, p.bodies, p.apex));
            }
        }
        for (Aspects.Hit opp : unpatternedOppositions(hits, patterns)) {
            out.add(build(f, hits, patterns, "Opposition", List.of(opp.a, opp.b), null));
        }

        // Deterministic order: the panel and the check must see the same list twice running,
        // and AspectPatterns returns its findings out of a HashSet.
        out.sort((x, y) -> {
            int c = rank(x.source) - rank(y.source);
            if (c != 0) {
                return c;
            }
            return String.join(",", x.bodies).compareTo(String.join(",", y.bodies));
        });
        return out;
    }

    private static int rank(String source) {
        switch (source) {
            case "Grand cross": return 0;
            case "T-square":    return 1;
            case "Yod":         return 2;
            default:            return 3;
        }
    }

    // ------------------------------------------------------------------ one configuration

    private static Release build(ChartFrame f, List<Aspects.Hit> hits,
                                 List<AspectPatterns.Pattern> patterns,
                                 String source, List<String> bodies, String apex) {
        Release r = new Release();
        r.source = source;
        r.bodies.addAll(bodies);
        Collections.sort(r.bodies);
        r.apex = apex;

        outlets(r, hits);
        emptyLeg(f, r, hits);
        softMembers(r, patterns);
        r.definitionalSpine = hasDefinitionalPair(r.bodies);
        r.sentence = sentence(r);
        return r;
    }

    /**
     * Two members that are one point and its own opposition.
     *
     * Asked of the registry rather than of a list of node names here, so that a point added
     * later with a definitional opposite is covered by the entry that declares it rather than
     * by a second list somebody has to remember to update.
     */
    private static boolean hasDefinitionalPair(List<String> bodies) {
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i + 1; j < bodies.size(); j++) {
                int a = Bodies.indexOfName(bodies.get(i));
                int b = Bodies.indexOfName(bodies.get(j));
                if (a >= 0 && b >= 0 && Bodies.isOppositePair(a, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The bodies and orbs this reading works in, taken from {@link AspectPatterns} rather
     * than restated.
     *
     * <b>One population for the whole tension reading.</b> When patterns were restricted to
     * physical bodies at a tight orb on 2026-08-23 and this file was not, the result was 21
     * pattern lines against 320 loose-opposition lines in the same corpus - the feature
     * half-converted, reporting a nodal-axis opposition as a stressed configuration on the
     * same screen as a pattern engine that had just excluded the nodes on principle. Pointing
     * at the constants rather than copying them means the next change to either lands here
     * too.
     */
    private static boolean eligible(Aspects.Hit h) {
        return AspectPatterns.PATTERN_BODIES.contains(h.a)
            && AspectPatterns.PATTERN_BODIES.contains(h.b)
            && h.offBy <= AspectPatterns.MAX_ORB;
    }

    /** Trines and sextiles reaching a member from a body that is not in the configuration. */
    private static void outlets(Release r, List<Aspects.Hit> hits) {
        List<Outlet> found = new ArrayList<>();
        for (Aspects.Hit h : hits) {
            if (h.type != Aspects.Type.TRINE && h.type != Aspects.Type.SEXTILE) {
                continue;
            }
            if (!eligible(h)) {
                continue;
            }
            String inside = null;
            String outside = null;
            if (r.bodies.contains(h.a) && !r.bodies.contains(h.b)) {
                inside = h.a;
                outside = h.b;
            } else if (r.bodies.contains(h.b) && !r.bodies.contains(h.a)) {
                inside = h.b;
                outside = h.a;
            }
            if (inside == null) {
                continue;
            }
            Outlet o = null;
            for (Outlet existing : found) {
                if (existing.via.equals(outside)) {
                    o = existing;
                    break;
                }
            }
            if (o == null) {
                o = new Outlet(outside);
                found.add(o);
            }
            o.to.add(inside);
            o.types.add(h.type);
            o.bestOffBy = Math.min(o.bestOffBy, h.offBy);
            if (inside.equals(r.apex)) {
                o.touchesApex = true;
            }
        }
        // Apex first, then mediators, then tightest - the order a reading would take them in.
        found.sort((a, b) -> {
            if (a.touchesApex != b.touchesApex) {
                return a.touchesApex ? -1 : 1;
            }
            if (a.isMediator() != b.isMediator()) {
                return a.isMediator() ? -1 : 1;
            }
            return Double.compare(a.bestOffBy, b.bestOffBy);
        });
        r.outlets.addAll(found);
    }

    /**
     * The point opposite the apex, where the geometry leaves one.
     *
     * A Grand cross has four occupied angles and no vacancy; an unpatterned opposition has
     * no apex to be opposite. Both correctly report no empty leg rather than a made-up one.
     */
    private static void emptyLeg(ChartFrame f, Release r, List<Aspects.Hit> hits) {
        if (r.apex == null) {
            return;
        }
        ChartFrame.Body a = f.body(r.apex);
        if (a == null || !a.ok) {
            return;
        }
        r.hasEmptyLeg = true;
        r.emptyLegLon = Zodiac.opposite(a.lon);
        r.emptyLegSign = Zodiac.signName(r.emptyLegLon);
        r.emptyLegHouse = Zodiac.houseOf(r.emptyLegLon, f.cusps);

        // The tightest opposition, not the first one the hit list happens to hold. Half of
        // these legs are occupied and a chart can offer more than one candidate; taking
        // whichever came first would let the order of Bodies.ALL name the occupant, which is
        // the same defect as the hit test taking the first glyph within reach rather than the
        // nearest one.
        double best = Double.MAX_VALUE;
        for (Aspects.Hit h : hits) {
            if (h.type != Aspects.Type.OPPOSITION || !eligible(h)) {
                continue;
            }
            String other = h.a.equals(r.apex) ? h.b : h.b.equals(r.apex) ? h.a : null;
            if (other != null && !r.bodies.contains(other) && h.offBy < best) {
                best = h.offBy;
                r.emptyLegOccupant = other;
            }
        }
    }

    /** Members that already stand in a Grand trine or Kite. */
    private static void softMembers(Release r, List<AspectPatterns.Pattern> patterns) {
        Set<String> soft = new LinkedHashSet<>();
        for (AspectPatterns.Pattern p : patterns) {
            if (!p.name.equals("Grand trine") && !p.name.equals("Kite")) {
                continue;
            }
            for (String b : p.bodies) {
                if (r.bodies.contains(b)) {
                    soft.add(b);
                }
            }
        }
        r.softMembers.addAll(soft);
    }

    /**
     * Oppositions that are not already an arm of a reported pattern.
     *
     * Without this filter the same opposition would be read twice - once as the spine of its
     * T-square and again on its own - and the second reading would say the tension is
     * unmediated while the first says where it discharges.
     */
    private static List<Aspects.Hit> unpatternedOppositions(List<Aspects.Hit> hits,
                                                            List<AspectPatterns.Pattern> patterns) {
        List<Aspects.Hit> out = new ArrayList<>();
        for (Aspects.Hit h : hits) {
            if (h.type != Aspects.Type.OPPOSITION || !eligible(h)) {
                continue;
            }
            boolean claimed = false;
            for (AspectPatterns.Pattern p : patterns) {
                if (p.bodies.contains(h.a) && p.bodies.contains(h.b)) {
                    claimed = true;
                    break;
                }
            }
            if (!claimed) {
                out.add(h);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ the sentence

    /**
     * One line per configuration, with a clause for every state it can be in.
     *
     * <b>Every branch here ends in a full sentence.</b> The quincunx shipped as a live
     * aspect with an empty glyph for months because the model knew about it and the view did
     * not; a configuration reaching an unwritten branch would read as "releases through
     * null". TensionReleaseCheck Part C asserts the completeness rather than trusting it.
     */
    private static String sentence(Release r) {
        StringBuilder sb = new StringBuilder();
        sb.append(subject(r));

        Outlet apexOutlet = null;
        for (Outlet o : r.outlets) {
            if (o.touchesApex) {
                apexOutlet = o;
                break;
            }
        }

        if (apexOutlet != null) {
            sb.append(" releases through ").append(apexOutlet.phrase())
              .append(" - the apex itself has somewhere to put the pressure");
        } else if (!r.outlets.isEmpty()) {
            Outlet best = r.outlets.get(0);
            sb.append(" releases at the base through ").append(best.phrase());
            if (r.apex != null) {
                sb.append(", but nothing softens ").append(r.apex).append(" directly");
            }
        } else if (!r.softMembers.isEmpty()) {
            sb.append(" has no soft aspect out of it, though ")
              .append(String.join(" and ", r.softMembers))
              .append(r.softMembers.size() > 1 ? " stand" : " stands")
              .append(" in a grand trine of its own");
        } else {
            sb.append(" has no soft aspect out of it at all");
        }

        if (r.hasEmptyLeg) {
            if (r.emptyLegOccupant != null) {
                sb.append(". Its empty leg is not empty - ").append(r.emptyLegOccupant)
                  .append(" stands opposite ").append(r.apex)
                  .append(", which is where the figure discharges");
            } else {
                sb.append(". The empty leg is ").append(capitalise(r.emptyLegSign));
                if (r.emptyLegHouse > 0) {
                    sb.append(", in the ").append(Zodiac.ordinal(r.emptyLegHouse)).append(" house");
                }
                sb.append(" - the ground the figure asks to be worked out on");
            }
        } else if (r.source.equals("Grand cross")) {
            sb.append(". A grand cross has no empty leg: all four angles are held");
        }

        if (r.definitionalSpine) {
            sb.append(". Its spine is a point and its own opposition, which is exactly "
                + "opposite in every chart ever cast");
            if (r.apex != null) {
                sb.append(" - read this as ").append(r.apex).append(" square that axis");
            }
        }

        return sb.append('.').toString();
    }

    private static String subject(Release r) {
        if (r.source.equals("Opposition")) {
            return "The " + String.join("-", r.bodies) + " opposition";
        }
        String s = "The " + r.source + " of " + String.join(", ", r.bodies);
        if (r.apex != null) {
            s += ", apex " + r.apex + ",";
        }
        return s;
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
