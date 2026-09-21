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

        /**
         * The days the fast catalysts touch this theme, if it is a headline. Never a testimony.
         *
         * <p>K12 stage 3 keeps Mars and the Sun out of the voting because they agree with
         * everything - and uses them for the one thing that makes them good: saying when. A
         * theme that three independent techniques have already agreed on gets its dates here;
         * a background trend gets none, because dating something that has not been established
         * is the false precision the whole Rule of Three exists to prevent.
         */
        public final List<Dated> dates = new ArrayList<>();

        /**
         * The days worth naming: where the catalysts pile up rather than merely pass through.
         *
         * <p><b>A year's worth of Sun and Mars touches is data, not an answer.</b> The Sun
         * reaches every one of a theme's points five times a year by construction, so a
         * headline theme collects dozens of hits and a reader handed the list learns nothing -
         * the same objection the Rule of Three answers for witnesses, arriving again in the
         * timing.
         *
         * <p>So the same shape of answer: a **peak** is where two or more catalyst touches on
         * the theme's own points fall within {@link #PEAK_SPAN_DAYS} of each other. One body
         * passing one point is the background hum of a year; two landing together is a date.
         */
        public final List<Peak> peaks = new ArrayList<>();

        /**
         * The n fullest peaks, back in date order.
         *
         * <b>Because the peaks themselves come out roughly monthly, and that is the Sun's
         * doing.</b> It reaches every point of a theme once a month by construction, so a
         * year's peaks are a monthly drumbeat and handing a reader twelve of them says little
         * more than handing them the calendar. What actually differs between them is how much
         * lands together: a fortnight where the Sun crosses the Midheaven while Mars squares
         * Saturn is not the same occasion as one where the Sun alone makes two aspects in
         * passing.
         *
         * So the ranking is by weight and the presentation is by date - the reader wants to
         * know which are the strong ones, and then when they are.
         */
        public List<Peak> strongest(int n) {
            List<Peak> byWeight = new ArrayList<>(peaks);
            byWeight.sort((a, b) -> b.hits.size() - a.hits.size());
            List<Peak> top = new ArrayList<>(
                byWeight.subList(0, Math.min(n, byWeight.size())));
            top.sort(java.util.Comparator.comparingDouble(Peak::from));
            return top;
        }

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

    /** How close two catalyst touches must fall to count as one date. */
    public static final double PEAK_SPAN_DAYS = 3.0;

    /** Two or more catalyst touches on a theme's points, close enough to be one occasion. */
    public static final class Peak {
        /** The touches, in order. */
        public final List<Dated> hits = new ArrayList<>();

        public double from() {
            return hits.get(0).jd;
        }

        public double to() {
            return hits.get(hits.size() - 1).jd;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Dated d : hits) {
                sb.append(sb.length() == 0 ? "" : ", ").append(d);
            }
            return sb.toString();
        }
    }

    /** One day a fast catalyst touches a headline theme. */
    public static final class Dated {
        public double jd;
        public String body;
        public String natal;
        public Aspects.Type type;

        @Override
        public String toString() {
            return String.format("%s %s %s", body, type.label.toLowerCase(), natal);
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
        return themes(f, prof, targets, null);
    }

    /**
     * The same, with K12's stage 2: the progressed Moon's own clock.
     *
     * @param clock {@link Progressions#clock}'s tenancies for the same window; null leaves the
     *              clock's testimony out, which is what the three-argument form does
     */
    public static List<Result> themes(ChartFrame f, Profection prof, List<Convergence.Target> targets,
                                      List<Progressions.Tenancy> clock) {
        return themes(f, prof, targets, clock, null);
    }

    /**
     * The same, with K12's stage 3 dating.
     *
     * <p>The catalyst hits are distributed to the themes they touch <b>after</b> every theme has
     * been judged, so that no ordering, no count and no headline can depend on them. That is the
     * whole design: the Sun and Mars agree with everything, so they are allowed to say when and
     * never whether.
     *
     * @param catalysts {@link Transits#datingHits}'s output for the same window; null leaves the
     *                  dating out, which is what the four-argument form does
     */
    public static List<Result> themes(ChartFrame f, Profection prof, List<Convergence.Target> targets,
                                      List<Progressions.Tenancy> clock,
                                      List<Transits.Perfection> catalysts) {
        List<Result> out = new ArrayList<>();
        Set<String> active = activePoints(f, prof, targets, clock);
        for (Theme theme : Theme.values()) {
            Result r = new Result();
            r.theme = theme;
            r.members.addAll(membersOf(theme, f));
            if (prof != null && theme.hasHouse(prof.house)) {
                r.families.add(Convergence.Family.PROFECTION);
                r.testimonies.add("PROFECTION: the year falls on the " + ordinal(prof.house)
                    + " house (lord " + prof.lord + ")");
            }
            // K12 stage 2. The progressed Moon tenanting one of the theme's houses is that
            // theme's own mid-term clock speaking, and it says so about the area of life
            // rather than about any one point - so it is a testimony here, next to the
            // profection, and not a witness on a natal point. One family however many of the
            // theme's houses it passes through: it is one Moon, and it agrees with itself.
            if (clock != null) {
                for (Progressions.Tenancy t : clock) {
                    if (!theme.hasHouse(t.house)) {
                        continue;
                    }
                    boolean fresh = r.families.add(Convergence.Family.PROGRESSED_MOON);
                    r.testimonies.add("PROGRESSED_MOON: " + t
                        + (t.phase == null || t.phase.isEmpty() ? "" : ", lunation " + t.phase)
                        + (fresh ? "" : " (same technique, counted once)"));
                }
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
        dateHeadlines(out, catalysts);
        return out;
    }

    /**
     * Hands each headline the days its own points are touched by a fast catalyst.
     *
     * <b>After the sort, and reading nothing it could change.</b> It runs on the finished list,
     * adds to one field, and touches neither {@code families} nor {@code score} - so a reader of
     * this method can see in one screen that dating cannot promote a trend into a headline,
     * which is the one way this stage could quietly undo stage 4.
     *
     * <b>Only headlines.</b> Naming the days something might happen, when nothing has agreed
     * that it will, is the false precision the Rule of Three exists to prevent - and it would be
     * the most convincing-looking output the app produces, which makes it the worst place to
     * let one through.
     */
    private static void dateHeadlines(List<Result> themes, List<Transits.Perfection> catalysts) {
        if (catalysts == null || catalysts.isEmpty()) {
            return;
        }
        for (Result r : themes) {
            if (!r.headline()) {
                continue;
            }
            for (Transits.Perfection p : catalysts) {
                if (!r.members.contains(p.natal)) {
                    continue;
                }
                Dated d = new Dated();
                d.jd = p.jd;
                d.body = p.transiting;
                d.natal = p.natal;
                d.type = p.type;
                r.dates.add(d);
            }
            r.dates.sort(java.util.Comparator.comparingDouble(d -> d.jd));
            r.peaks.addAll(peaksIn(r.dates));
        }
    }

    /**
     * Runs of touches close enough together to be one occasion, two or more.
     *
     * <b>Greedy from the left, each gap measured against its predecessor.</b> A run therefore
     * grows as long as the touches keep coming within the span, which is right: four touches
     * three days apart are one nine-day occasion, not two overlapping ones. A lone touch is
     * dropped, because one fast body passing one point is what a year is made of.
     */
    static List<Peak> peaksIn(List<Dated> dates) {
        List<Peak> out = new ArrayList<>();
        Peak run = null;
        for (Dated d : dates) {
            if (run != null && d.jd - run.to() <= PEAK_SPAN_DAYS) {
                run.hits.add(d);
                continue;
            }
            if (run != null && run.hits.size() >= 2) {
                out.add(run);
            }
            run = new Peak();
            run.hits.add(d);
        }
        if (run != null && run.hits.size() >= 2) {
            out.add(run);
        }
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
        return activePoints(f, prof, targets, null);
    }

    /**
     * The same, with the progressed Moon's clock included.
     *
     * <p>K12 stage 2 calls an active progression "the loaded gun", and the progressed Moon is a
     * progression: the natal bodies in the house it tenants are woken by it, exactly as the
     * bodies in the profected sign are woken by stage 1. Without this the clock could name a
     * theme and still leave every transit into that area demoted to background, which is the
     * opposite of what a mid-term clock is for.
     */
    static Set<String> activePoints(ChartFrame f, Profection prof, List<Convergence.Target> targets,
                                    List<Progressions.Tenancy> clock) {
        Set<String> a = new LinkedHashSet<>();
        if (clock != null && f != null) {
            for (Progressions.Tenancy t : clock) {
                for (int i = 0; i < Bodies.count(); i++) {
                    String name = Bodies.at(i).name;
                    ChartFrame.Body b = f.body(name);
                    if (b != null && b.ok && Zodiac.houseOf(b.lon, f.cusps) == t.house) {
                        a.add(name);
                    }
                }
            }
        }
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
    /**
     * Every point any theme stands on, pooled - what a dating scan needs to watch.
     *
     * <b>One scan for four themes.</b> Which theme a catalyst hit belongs to is decided here,
     * in {@link #dateHeadlines}, so scanning per theme would ask the ephemeris the same
     * questions several times over for the points the themes share - the Sun and the Moon are
     * each in two of them, and a ruler can be in any.
     */
    public static Set<String> allThemePoints(ChartFrame f) {
        Set<String> all = new LinkedHashSet<>();
        for (Theme theme : Theme.values()) {
            all.addAll(membersOf(theme, f));
        }
        return all;
    }

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
