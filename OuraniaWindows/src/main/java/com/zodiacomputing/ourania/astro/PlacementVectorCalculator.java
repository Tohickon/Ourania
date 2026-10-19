package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A placement vector model: how much weight the natal chart itself puts behind each of
 * {@link ThemeConvergence}'s four themes. It orders and grades; it never decides.
 *
 * <p><b>The Rule of Three stays where it is.</b> Whether a theme is a headline is
 * {@link ThemeConvergence.Result#headline()} - three independent technique families, echoes
 * already removed by {@link Convergence}. The first draft of this class gated on a number
 * (a summed score) and a separate witness count passed in by the caller; that is two rules
 * for one decision, and the summed-score half is the false precision the rule exists to stop.
 * Here the vector can only rank headlines among themselves and grade them. A theme with two
 * families is QUIET however heavy its placements are.
 *
 * <p><b>The space is small on purpose: theme x element, 4 x 4 = 16 doubles per body.</b>
 * Each body is (P (x) S) (.) H: P says which themes the body signifies (a theme point, or
 * ruler of a theme house), S is the sign's element, H is the house the body sits in. Lunar
 * mansion and Sabian degree are not further Kronecker factors - 28 x 360 more dimensions per
 * body, with nothing in the repo to fill them - but sparse scalar modifiers on the whole
 * vector, neutral (1.0) unless a table says otherwise.
 *
 * <p><b>The rest reuses the engine's own rules:</b> essential dignity from {@link Dignity},
 * sect membership from {@link Sect}, aspect angles from {@link Aspects.Type}, dispositors
 * from {@link Dignity#domicileRulerOf}. None of them is restated here.
 *
 * <p><b>Nothing here writes prose, and nothing here imports {@code gui}.</b> The plain card
 * is built by {@code ReadingCard.fromTheme}, which keeps the number in the "why" lines that
 * only Astrologer view shows.
 */
public final class PlacementVectorCalculator {

    private PlacementVectorCalculator() { }

    public static final int THEMES = ThemeConvergence.Theme.values().length;
    /** Fire, earth, air, water - {@code signIndex % 4}. */
    public static final int ELEMENTS = 4;
    public static final int DIM = THEMES * ELEMENTS;

    /** Gaussian orb decay width, in degrees. */
    public static final double ORB_SIGMA = 2.0;
    /** Past this orb an aspect adds nothing worth the arithmetic (exp(-8) at 8 degrees). */
    public static final double MAX_ORB = 8.0;
    /** Share of the dispositor's base vector a body inherits. */
    public static final double ALPHA_DISPOSITOR = 0.20;
    /** P and H: a full signification or house, and the floor for neither. */
    static final double SIGNIFIES = 1.0;
    static final double BACKGROUND = 0.5;

    /** The five Ptolemaic aspects; the minor ones carry no weight in this model. */
    static final Aspects.Type[] MAJORS = {
        Aspects.Type.CONJUNCTION, Aspects.Type.SEXTILE, Aspects.Type.SQUARE,
        Aspects.Type.TRINE, Aspects.Type.OPPOSITION
    };

    /** How a headline theme's weight compares with the chart's heaviest theme. */
    public enum IntensityBand {
        MAJOR("Major"),
        HIGH("High"),
        ACTIVE("Active"),
        QUIET("Quiet");

        public final String label;
        IntensityBand(String label) { this.label = label; }

        /** @param share the theme's weight over the heaviest theme's, 0..1 */
        public static IntensityBand fromShare(double share) {
            if (share >= 0.90) return MAJOR;
            if (share >= 0.70) return HIGH;
            if (share >= 0.40) return ACTIVE;
            return QUIET;
        }
    }

    // ------------------------------------------------------------------ scalars

    /** exp(-orb^2 / (2 sigma^2)). */
    public static double gaussianOrbDecay(double orb, double sigma) {
        return Math.exp(-(orb * orb) / (2.0 * sigma * sigma));
    }

    /** 1.2 of sect, 0.8 contrary to sect, 1.0 for a body sect does not classify. */
    public static double sectScalar(Sect.Membership m) {
        if (m == Sect.Membership.OF_SECT) return 1.2;
        if (m == Sect.Membership.CONTRARY_TO_SECT) return 0.8;
        return 1.0;
    }

    /** D = (1 + essential / 10) x sect x accidental, floored at 0.1 so no body vanishes. */
    public static double dignityScalar(int essential, Sect.Membership sect, double accidental) {
        return Math.max(0.1, (1.0 + essential / 10.0) * sectScalar(sect) * accidental);
    }

    /** W: luminaries and angles 1.0, personal 0.85, social 0.75, outer 0.65, minor bodies less. */
    public static double hierarchyWeight(String body) {
        Bodies.Def d = Bodies.byName(body);
        if (d == null) return 0.30;
        switch (d.kind) {
            case LUMINARY:
            case ANGLE:
                return 1.0;
            case PLANET:
                switch (body) {
                    case "Mercury": case "Venus": case "Mars": return 0.85;
                    case "Jupiter": case "Saturn": return 0.75;
                    default: return 0.65;
                }
            case ASTEROID:
                return 0.40;
            default:
                return 0.30;   // nodes, lots, calculated points
        }
    }

    /**
     * A sparse table of multipliers keyed by an int (a Sabian degree 0..359, a mansion 1..28).
     * Absent keys are 1.0, so an empty table is exactly "no modifier".
     */
    public static final class SparseModifier {
        public static final SparseModifier NEUTRAL = new SparseModifier(new int[0], new double[0]);

        private final int[] keys;
        private final double[] values;

        /** @param keys sorted ascending, one value each */
        public SparseModifier(int[] keys, double[] values) {
            if (keys.length != values.length) {
                throw new IllegalArgumentException("one value per key");
            }
            this.keys = keys.clone();
            this.values = values.clone();
        }

        public double at(int key) {
            int i = Arrays.binarySearch(keys, key);
            return i >= 0 ? values[i] : 1.0;
        }
    }

    // ------------------------------------------------------------------ in-place vector ops

    /** out = a (x) b; out.length must be a.length x b.length. */
    public static void kronecker(double[] a, double[] b, double[] out) {
        int k = 0;
        for (double va : a) {
            for (double vb : b) {
                out[k++] = va * vb;
            }
        }
    }

    /** a = a (.) mask, element by element. */
    public static void hadamardInPlace(double[] a, double[] mask) {
        for (int i = 0; i < a.length; i++) {
            a[i] *= mask[i];
        }
    }

    public static double magnitude(double[] v) {
        double s = 0.0;
        for (double x : v) s += x * x;
        return Math.sqrt(s);
    }

    // ------------------------------------------------------------------ the chart

    /** One body's inputs, as the engine's own rules read them. */
    public record Placement(String body, double longitude, int house, int essential,
                            Sect.Membership sect, double accidental, double weight) { }

    /**
     * The buffers one computation needs, allocated once per body count and reused. A caller
     * that recomputes - a time scrub, a sweep over many charts - passes the same workspace
     * and the inner loops allocate nothing.
     */
    public static final class Workspace {
        double[][] base = new double[0][];
        double[][] out = new double[0][];
        final double[] p = new double[THEMES];
        final double[] s = new double[ELEMENTS];
        final double[] h = new double[DIM];

        void ensure(int n) {
            if (base.length >= n) {
                for (int i = 0; i < n; i++) {
                    Arrays.fill(base[i], 0.0);
                    Arrays.fill(out[i], 0.0);
                }
                return;
            }
            base = new double[n][DIM];
            out = new double[n][DIM];
        }
    }

    /** The finished model: per-body vectors and the per-theme totals. */
    public static final class Result {
        public final List<Placement> placements;
        /** Indexed like {@link ThemeConvergence.Theme#values()}. */
        public final double[] themeWeight = new double[THEMES];
        final double[][] vectors;

        Result(List<Placement> placements, double[][] vectors) {
            this.placements = placements;
            this.vectors = vectors;
        }

        public double weightOf(ThemeConvergence.Theme t) {
            return themeWeight[t.ordinal()];
        }

        public double heaviest() {
            double m = 0.0;
            for (double w : themeWeight) m = Math.max(m, w);
            return m;
        }

        /** The body's final vector; a view into the workspace, valid until its next use. */
        public double[] vectorOf(String body) {
            for (int i = 0; i < placements.size(); i++) {
                if (placements.get(i).body().equals(body)) return vectors[i];
            }
            return null;
        }
    }

    /** Every body the chart computed, read through Dignity and Sect. Accidental is 1.0. */
    public static List<Placement> placementsOf(ChartFrame f) {
        List<Placement> out = new ArrayList<>();
        ChartFrame.Body sun = f.body("Sun");
        double sunLon = sun != null && sun.ok ? sun.lon : 0.0;
        for (int i = 0; i < Bodies.count(); i++) {
            String name = Bodies.at(i).name;
            ChartFrame.Body b = f.body(name);
            if (b == null || !b.ok) continue;
            int essential = Dignity.evaluate(name, b.lon, f.diurnal).score;
            Sect.Membership m = Sect.evaluate(name, f.diurnal, b.lon, sunLon).membership;
            out.add(new Placement(name, b.lon, Zodiac.houseOf(b.lon, f.cusps), essential, m,
                1.0, hierarchyWeight(name)));
        }
        return out;
    }

    public static Result compute(ChartFrame f) {
        return compute(f, placementsOf(f), new Workspace(),
            SparseModifier.NEUTRAL, SparseModifier.NEUTRAL);
    }

    /**
     * I(P) = W x D x mansion x sabian x [(P (x) S) (.) H] + aspects + alpha x base(dispositor).
     */
    public static Result compute(ChartFrame f, List<Placement> placements, Workspace ws,
                                 SparseModifier mansions, SparseModifier sabians) {
        int n = placements.size();
        ws.ensure(n);
        ThemeConvergence.Theme[] themes = ThemeConvergence.Theme.values();
        List<java.util.Set<String>> significators = new ArrayList<>();
        for (ThemeConvergence.Theme t : themes) {
            significators.add(significatorsOf(t, f));
        }

        // Base vectors.
        for (int i = 0; i < n; i++) {
            Placement pl = placements.get(i);
            for (int t = 0; t < THEMES; t++) {
                ws.p[t] = significators.get(t).contains(pl.body()) ? SIGNIFIES : BACKGROUND;
            }
            Arrays.fill(ws.s, 0.0);
            ws.s[Zodiac.signIndex(pl.longitude()) % ELEMENTS] = 1.0;
            for (int t = 0; t < THEMES; t++) {
                double hv = themes[t].hasHouse(pl.house()) ? SIGNIFIES : BACKGROUND;
                for (int e = 0; e < ELEMENTS; e++) ws.h[t * ELEMENTS + e] = hv;
            }
            double[] v = ws.base[i];
            kronecker(ws.p, ws.s, v);
            hadamardInPlace(v, ws.h);
            double scale = pl.weight() * dignityScalar(pl.essential(), pl.sect(), pl.accidental())
                * mansions.at(LunarMansions.at(pl.longitude()).number)
                * sabians.at((int) Math.floor(Zodiac.normalise(pl.longitude())));
            for (int k = 0; k < DIM; k++) v[k] *= scale;
        }

        // Aspects: each pair reinforces the dimensions both share, by orb.
        for (int i = 0; i < n; i++) {
            double[] oi = ws.out[i];
            for (int j = i + 1; j < n; j++) {
                double w = aspectWeight(placements.get(i).longitude(), placements.get(j).longitude());
                if (w == 0.0) continue;
                double[] bi = ws.base[i];
                double[] bj = ws.base[j];
                double[] oj = ws.out[j];
                for (int k = 0; k < DIM; k++) {
                    double x = bi[k] * bj[k] * w;
                    oi[k] += x;
                    oj[k] += x;
                }
            }
        }

        // Base, plus a share of the dispositor's base.
        Result r = new Result(placements, ws.out);
        for (int i = 0; i < n; i++) {
            Placement pl = placements.get(i);
            String disp = Dignity.domicileRulerOf(Zodiac.signIndex(pl.longitude()));
            int d = disp.equals(pl.body()) ? -1 : indexOf(placements, disp);
            double[] o = ws.out[i];
            double[] b = ws.base[i];
            for (int k = 0; k < DIM; k++) {
                o[k] += b[k] + (d >= 0 ? ALPHA_DISPOSITOR * ws.base[d][k] : 0.0);
                r.themeWeight[k / ELEMENTS] += o[k];
            }
        }
        return r;
    }

    /** The strongest major aspect between two longitudes, Gaussian by orb; 0 outside {@link #MAX_ORB}. */
    static double aspectWeight(double lonA, double lonB) {
        double sep = Math.abs(Zodiac.normalise(lonA - lonB));
        if (sep > 180.0) sep = 360.0 - sep;
        double best = 0.0;
        for (Aspects.Type a : MAJORS) {
            double orb = Math.abs(sep - a.exactAngle);
            if (orb <= MAX_ORB) best = Math.max(best, gaussianOrbDecay(orb, ORB_SIGMA));
        }
        return best;
    }

    /** What a theme signifies by nature: its points and the rulers of its houses. Not occupancy - H carries that. */
    static java.util.Set<String> significatorsOf(ThemeConvergence.Theme t, ChartFrame f) {
        java.util.Set<String> s = new java.util.LinkedHashSet<>(Arrays.asList(t.points));
        for (int h : t.rulerHouses) {
            s.add(Dignity.domicileRulerOf(Zodiac.signIndex(f.cusps[h])));
        }
        return s;
    }

    private static int indexOf(List<Placement> ps, String body) {
        for (int i = 0; i < ps.size(); i++) {
            if (ps.get(i).body().equals(body)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ themes

    /**
     * One theme as the reader meets it: the convergence verdict, and the chart's weight
     * behind it. {@link #headline()} is the convergence's, unchanged.
     */
    public record ThemeReading(ThemeConvergence.Result convergence, double weight, double share,
                               IntensityBand band) {
        public ThemeConvergence.Theme theme() { return convergence.theme; }
        public boolean headline() { return convergence.headline(); }
    }

    /**
     * The themes in reading order: headlines first, then more families, then the heavier chart
     * weight - which replaces the convergence score only as the last tie-break. The band is
     * QUIET for every theme that is not a headline.
     */
    public static List<ThemeReading> readings(List<ThemeConvergence.Result> themes, Result vectors) {
        double top = vectors.heaviest();
        List<ThemeReading> out = new ArrayList<>();
        for (ThemeConvergence.Result c : themes) {
            double w = vectors.weightOf(c.theme);
            double share = top > 0.0 ? w / top : 0.0;
            IntensityBand band = c.headline() ? IntensityBand.fromShare(share) : IntensityBand.QUIET;
            out.add(new ThemeReading(c, w, share, band));
        }
        out.sort(Comparator.comparing((ThemeReading t) -> !t.headline())
            .thenComparing(t -> -t.convergence().families.size())
            .thenComparing(t -> -t.weight()));
        return out;
    }
}
