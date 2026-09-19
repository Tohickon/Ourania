package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * K12, stage 4: the year's convergence gathered into life themes, and the Rule of Three.
 *
 * <p><b>Convergence already counts independent techniques, but per natal point.</b> That answers
 * "which of my points is busy this year" - not "what is this year about". A promotion does not
 * arrive through one point: it is the 10th house, the Midheaven, Saturn and the Sun, and the
 * ruler of the 10th, any of which can be the one a technique happens to touch. So the targets
 * {@link Convergence#collect} produces are pooled by theme, and the theme - not the point - is
 * what the Rule of Three is applied to.
 *
 * <p><b>The Rule of Three</b> (David, 2026-09-19): a major event is only foretold when at least
 * three <i>independent</i> techniques name the same theme in the same window. Independence is
 * counted exactly as Convergence counts it - by technique family, echoes excluded - because
 * "two transits to the same point are one witness, not two" holds just as much across a theme:
 * two transits to two career points are still one technique agreeing with itself.
 *
 * <p><b>The profection is stage 1's testimony.</b> A year whose profection falls on one of a
 * theme's houses gives that theme the profection family, whatever the lord happens to touch -
 * the year's topic is a statement about the theme by itself.
 *
 * <p>Nothing here writes prose. A theme is named, and its testimonies are the witnesses'
 * own details, listed; the headline is the theme's name and the count (the K12 note on why).
 */
public final class ThemeConvergence {

    private ThemeConvergence() { }

    /** How many independent technique families make a headline. */
    public static final int RULE_OF_THREE = 3;

    /** The four themes of K12, with the houses, points and rulers that stand for each. */
    public enum Theme {
        CAREER("Career and status", new int[] {10}, new int[] {10}, "MC", "Saturn", "Sun"),
        RELATIONSHIP("Relationship", new int[] {7}, new int[] {7}, "Descendant", "Venus", "Lot of Eros"),
        HEALTH("Health and vitality", new int[] {1, 6}, new int[] {1, 6}, "Ascendant", "Mars", "Sun", "Moon"),
        HOME("Home and relocation", new int[] {4, 3, 9}, new int[] {4}, "IC", "Moon", "Mercury");

        public final String label;
        /** The houses of the theme: their occupants belong to it, and a profection on one is a testimony. */
        final int[] houses;
        /** The houses whose domicile rulers stand for the theme. */
        final int[] rulerHouses;
        final String[] points;

        Theme(String label, int[] houses, int[] rulerHouses, String... points) {
            this.label = label;
            this.houses = houses;
            this.rulerHouses = rulerHouses;
            this.points = points;
        }

        boolean hasHouse(int h) {
            for (int x : houses) {
                if (x == h) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class Result {
        public Theme theme;
        /** The natal points that stand for the theme in this chart. */
        public final Set<String> members = new LinkedHashSet<>();
        /** The independent technique families agreeing on the theme. */
        public final Set<Convergence.Family> families = EnumSet.noneOf(Convergence.Family.class);
        /** One line per independent witness: the family, the point, and the witness's own detail. */
        public final List<String> testimonies = new ArrayList<>();
        /** The members' convergence scores summed, for ordering within a band. */
        public double score;

        /** The Rule of Three: at least three independent families name this theme. */
        public boolean headline() {
            return families.size() >= RULE_OF_THREE;
        }

        @Override
        public String toString() {
            return theme.label + ": " + families.size() + " families " + families
                + (headline() ? " - HEADLINE" : " - background");
        }
    }

    /**
     * The themes for a chart's year, headlines first, then by families, then by score.
     *
     * @param f        the natal chart - its cusps say which house holds what, and whose rulers
     * @param prof     the year's profection; null leaves out the profection testimony
     * @param targets  {@link Convergence#collect}'s output for the same year
     */
    public static List<Result> themes(ChartFrame f, Profection prof, List<Convergence.Target> targets) {
        List<Result> out = new ArrayList<>();
        Set<String> active = activePoints(f, prof, targets);
        for (Theme theme : Theme.values()) {
            Result r = new Result();
            r.theme = theme;
            r.members.addAll(membersOf(theme, f));
            if (prof != null && theme.hasHouse(prof.house)) {
                r.families.add(Convergence.Family.PROFECTION);
                r.testimonies.add("PROFECTION: the year falls on the " + ordinal(prof.house)
                    + " house (lord " + prof.lord + ")");
            }
            for (Convergence.Target t : targets) {
                if (!r.members.contains(t.natal)) {
                    continue;
                }
                r.score += t.score;
                for (Convergence.Witness w : t.witnesses) {
                    if (!w.independent) {
                        continue;
                    }
                    // K12 stages 1 and 3: an external catalyst only testifies where the year
                    // has already loaded the gun. To a point nothing else has woken it is
                    // listed, as background, and not counted.
                    if (isCatalyst(w.family) && !active.contains(t.natal)) {
                        r.testimonies.add(w.family.name() + ": " + t.natal + " - " + w.detail
                            + " (to a point not active this year: background, not counted)");
                        continue;
                    }
                    boolean fresh = r.families.add(w.family);
                    // Every independent witness is listed; a family repeated on another point
                    // is still shown, so the reader sees what agreed, but it counts once.
                    r.testimonies.add(w.family.name() + ": " + t.natal + " - " + w.detail
                        + (fresh ? "" : " (same technique, counted once)"));
                }
            }
            out.add(r);
        }
        out.sort((a, b) -> {
            if (a.headline() != b.headline()) {
                return a.headline() ? -1 : 1;
            }
            if (a.families.size() != b.families.size()) {
                return b.families.size() - a.families.size();
            }
            return Double.compare(b.score, a.score);
        });
        return out;
    }

    /** Transits, stations and eclipses: the world arriving, as against the chart's own clocks. */
    static boolean isCatalyst(Convergence.Family f) {
        return f == Convergence.Family.TRANSIT || f == Convergence.Family.STATION
            || f == Convergence.Family.ECLIPSE;
    }

    /**
     * K12 stages 1 and 2: the natal points the year has activated - the lord of the year, the
     * bodies in the profected sign, and every point a progression or solar arc reaches in the
     * window (the "loaded gun"). Only these can be triggered by a transit, station or eclipse.
     */
    static Set<String> activePoints(ChartFrame f, Profection prof, List<Convergence.Target> targets) {
        Set<String> a = new LinkedHashSet<>();
        if (prof != null) {
            if (prof.lord != null) {
                a.add(prof.lord);
            }
            for (int i = 0; i < Bodies.count(); i++) {
                String name = Bodies.at(i).name;
                ChartFrame.Body b = f.body(name);
                if (b != null && b.ok && Zodiac.signIndex(b.lon) == prof.sign) {
                    a.add(name);
                }
            }
        }
        for (Convergence.Target t : targets) {
            for (Convergence.Witness w : t.witnesses) {
                if (w.independent && (w.family == Convergence.Family.PROGRESSION
                        || w.family == Convergence.Family.SOLAR_ARC)) {
                    a.add(t.natal);
                }
            }
        }
        return a;
    }

    /** The theme's named points, the domicile rulers of its ruler houses, and its houses' occupants. */
    static Set<String> membersOf(Theme theme, ChartFrame f) {
        Set<String> m = new LinkedHashSet<>();
        for (String p : theme.points) {
            m.add(p);
        }
        for (int h : theme.rulerHouses) {
            m.add(Dignity.domicileRulerOf(Zodiac.signIndex(f.cusps[h])));
        }
        for (int i = 0; i < Bodies.count(); i++) {
            String name = Bodies.at(i).name;
            ChartFrame.Body b = f.body(name);
            if (b != null && b.ok && theme.hasHouse(Zodiac.houseOf(b.lon, f.cusps))) {
                m.add(name);
            }
        }
        return m;
    }

    private static String ordinal(int n) {
        int t = n % 100;
        if (t >= 11 && t <= 13) {
            return n + "th";
        }
        switch (n % 10) {
            case 1: return n + "st";
            case 2: return n + "nd";
            case 3: return n + "rd";
            default: return n + "th";
        }
    }
}
