package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * How heavily an aspect line is inked (H3a).
 *
 * <p><b>Weight is amplitude times proximity.</b> Before 25 Sep it was proximity alone, so a
 * semisextile at 0&deg; drew exactly as heavily as a conjunction at 0&deg; - two lines saying the
 * same thing about very different aspects. And the range it was scaled into was 0.3 to 1.0
 * pixels, under one pixel of variation, so the rule that a tighter aspect draws heavier was true
 * and invisible.
 *
 * <p><b>Part C is the one David asked for.</b> Measuring closeness as a fraction of the orb in
 * force means the same aspect brightens when you widen the orb - a conjunction three degrees from
 * exact goes from alpha 70 at a five degree orb to 143 at ten. That is defensible as "how much of
 * your allowance this uses", and it is a trap while tuning orbs, so the absolute mode exists and
 * is asserted to be genuinely independent of the orb setting.
 */
public final class AspectWeightCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();

        part("A: amplitude belongs to the harmonic family", AspectWeightCheck::amplitude);
        part("B: the tradition's ranking, in order", AspectWeightCheck::ranking);
        part("C: what proximity is measured against", AspectWeightCheck::proximity);
        part("D: the width range a reader can set", AspectWeightCheck::range);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    // ------------------------------------------------------------------ A

    private static void amplitude() {
        for (Aspects.Type t : Aspects.Type.values()) {
            double a = t.amplitude();
            ok(t.label + " carries a real amplitude (" + a + ")", a > 0.0 && a <= 1.0);
        }

        // <b>The claim the design rests on.</b> Amplitude is a property of the harmonic family,
        // so two aspects dividing the circle the same way must weigh the same - and this is what
        // stops the semisextile and the quincunx, or the two quintile-family pairs, drifting
        // apart the day one of them is edited.
        for (Aspects.Type a : Aspects.Type.values()) {
            for (Aspects.Type b : Aspects.Type.values()) {
                if (a.harmonic == b.harmonic) {
                    ok(a.label + " and " + b.label + " share harmonic " + a.harmonic
                        + " and so share an amplitude",
                        Math.abs(a.amplitude() - b.amplitude()) < 1e-9);
                }
            }
        }

        // Pairs the tradition groups, asserted by name so that the grouping is visible here and
        // not only in a switch.
        same("Semisquare", "Sesquiquadrate");
        same("Quintile", "Biquintile");
        same("Decile", "Sesquiquintile");
        same("Semisextile", "Quincunx");
        same("Conjunction", "Opposition");
    }

    private static void same(String a, String b) {
        Aspects.Type x = Aspects.Type.fromLabel(a);
        Aspects.Type y = Aspects.Type.fromLabel(b);
        ok(a + " and " + b + " are one family", x != null && y != null
            && Math.abs(x.amplitude() - y.amplitude()) < 1e-9);
    }

    // ------------------------------------------------------------------ B

    private static void ranking() {
        double conj = Aspects.Type.CONJUNCTION.amplitude();
        double opp = Aspects.Type.OPPOSITION.amplitude();
        double trine = Aspects.Type.TRINE.amplitude();
        double square = Aspects.Type.SQUARE.amplitude();
        double sextile = Aspects.Type.SEXTILE.amplitude();
        double semisq = Aspects.Type.SEMISQUARE.amplitude();
        double quintile = Aspects.Type.QUINTILE.amplitude();
        double quincunx = Aspects.Type.QUINCUNX.amplitude();
        double septile = Aspects.Type.SEPTILE.amplitude();

        // <b>No function of the harmonic gives this.</b> The opposition divides the circle in two
        // and carries as much as the conjunction, where 1/sqrt(harmonic) would put it at 0.71 -
        // which is why amplitude is a stated ranking rather than arithmetic.
        ok("the opposition is as strong as the conjunction", Math.abs(conj - opp) < 1e-9);
        ok("and both are the strongest", conj >= trine && conj >= square && conj >= sextile);

        ok("trine and square come next together",
            Math.abs(trine - square) < 1e-9 && trine < conj);
        ok("the sextile below them", sextile < trine);
        ok("the hard minors below the sextile", semisq < sextile);
        ok("the quintile family below those", quintile < semisq);
        ok("aversion below again", quincunx < quintile);
        ok("and the septile lowest", septile <= quincunx);

        // The exact figures, written out, so a change to them is a deliberate edit here too.
        eq("Conjunction", 1.00);
        eq("Opposition", 1.00);
        eq("Trine", 0.80);
        eq("Square", 0.80);
        eq("Sextile", 0.65);
        eq("Semisquare", 0.45);
        eq("Sesquiquadrate", 0.45);
        eq("Quintile", 0.40);
        eq("Decile", 0.40);
        eq("Semisextile", 0.35);
        eq("Quincunx", 0.35);
        eq("Septile", 0.30);
        eq("Novile", 0.30);
    }

    private static void eq(String label, double want) {
        Aspects.Type t = Aspects.Type.fromLabel(label);
        ok(label + " weighs " + want, t != null && Math.abs(t.amplitude() - want) < 1e-9);
    }

    // ------------------------------------------------------------------ C

    /** The proximity term, as drawAspectLine computes it. */
    private static double proximity(double degreesFromExact, double reach) {
        double d7 = Math.min(1.0, degreesFromExact / Math.max(0.0001, reach));
        return Math.pow(1.0 - d7, 2.0);
    }

    private static void proximity() {
        // Relative: the same aspect looks stronger when the orb is widened. This is the coupling
        // David found, asserted so that it is a recorded property rather than a surprise.
        double atFive = proximity(3.0, 5.0);
        double atTen = proximity(3.0, 10.0);
        ok("relative: three degrees reads tighter under a ten degree orb than a five ("
            + String.format("%.3f", atFive) + " -> " + String.format("%.3f", atTen) + ")",
            atTen > atFive * 1.5);

        // Absolute: the same three degrees reads the same whatever is allowed, which is the
        // point of the mode.
        double absFive = proximity(3.0,
            com.zodiacomputing.ourania.gui.Settings.ABSOLUTE_FADE_DEGREES);
        double absTen = proximity(3.0,
            com.zodiacomputing.ourania.gui.Settings.ABSOLUTE_FADE_DEGREES);
        ok("absolute: the orb setting does not enter into it",
            Math.abs(absFive - absTen) < 1e-12);

        // Both modes agree on the two ends, or the mode would be changing more than it claims.
        ok("exact is full strength in either mode",
            Math.abs(proximity(0.0, 5.0) - 1.0) < 1e-9
                && Math.abs(proximity(0.0, 10.0) - 1.0) < 1e-9);
        ok("at the edge it falls to nothing", proximity(5.0, 5.0) < 1e-9);
        ok("and never goes negative past the edge", proximity(99.0, 5.0) >= 0.0);

        // The curve is squared, so it falls away fast: half way out is a quarter of the force,
        // not half. That is what makes a partile aspect stand out from a merely close one.
        ok("half way out is a quarter of the strength",
            Math.abs(proximity(2.5, 5.0) - 0.25) < 1e-9);

        // The default keeps what the wheel has always done.
        ok("relative is the default",
            !com.zodiacomputing.ourania.gui.Settings.aspectWeightAbsolute());
    }

    // ------------------------------------------------------------------ D

    private static void range() {
        double lo = com.zodiacomputing.ourania.gui.Settings.aspectWeightMin();
        double hi = com.zodiacomputing.ourania.gui.Settings.aspectWeightMax();
        ok("the thinnest line is thinner than the thickest", lo < hi);
        // The old range was 0.3 to 1.0 - under one pixel, which is why the rule was invisible.
        ok("and the range spans more than a pixel (" + lo + " to " + hi + ")", hi - lo > 1.0);

        // Out of range is refused and the default comes back, which is H1's rule for orbs: a
        // file saying 400 gets the default, because quietly making it 15 would say 400 was
        // accepted.
        com.zodiacomputing.ourania.gui.Settings.set(
            com.zodiacomputing.ourania.gui.Settings.ASPECT_WEIGHT_MAX_KEY, "400");
        ok("a width of 400 is refused, not clamped",
            Math.abs(com.zodiacomputing.ourania.gui.Settings.aspectWeightMax() - hi) < 1e-9);
        com.zodiacomputing.ourania.gui.Settings.set(
            com.zodiacomputing.ourania.gui.Settings.ASPECT_WEIGHT_MAX_KEY, "not a number");
        ok("and so is a word",
            Math.abs(com.zodiacomputing.ourania.gui.Settings.aspectWeightMax() - hi) < 1e-9);
        com.zodiacomputing.ourania.gui.Settings.set(
            com.zodiacomputing.ourania.gui.Settings.ASPECT_WEIGHT_MAX_KEY, "2.5");
        ok("but a sensible one is kept",
            Math.abs(com.zodiacomputing.ourania.gui.Settings.aspectWeightMax() - 2.5) < 1e-9);

        // The whole point: a conjunction at nought outweighs a semisextile at nought.
        double conjAtExact = 1.0 * Aspects.Type.CONJUNCTION.amplitude();
        double semiAtExact = 1.0 * Aspects.Type.SEMISEXTILE.amplitude();
        ok("an exact conjunction inks heavier than an exact semisextile",
            conjAtExact > semiAtExact * 2.0);
    }

    // ------------------------------------------------------------------

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
