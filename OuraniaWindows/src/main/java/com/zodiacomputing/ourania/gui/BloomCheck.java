package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * The ring animator: opens, folds, and survives being interrupted halfway.
 *
 * <p><b>Interruption is the whole difficulty.</b> A reader who opens a ring and changes their
 * mind a third of the way through must not wait for the opening to finish, and must not see
 * the ring jump to full and then close. So a reversal has to start from wherever the value
 * actually is - which is the one thing an animator written from a start time and a direction
 * gets wrong, and it gets it wrong in a way that looks like a rendering glitch rather than
 * like a bug in arithmetic.
 *
 * <p>Part C is the one that would otherwise rot: with animation turned off every bloom must
 * still reach its end state. A ring that never opens because motion was disabled is a feature
 * silently switched off by an accessibility preference.
 */
public final class BloomCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Part A: the shape of the curve ===");
        int before = failures.size();
        theCurve();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: staggering unfurls a ring ===");
        before = failures.size();
        theStagger();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: an interrupted bloom, and no motion at all ===");
        before = failures.size();
        interruption();
        report("Part C", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
        System.exit(0);
    }

    private static void theCurve() {
        // Sine in-out: still at both ends, quickest through the middle. Asserted through
        // smoothstep and stagger, which are the two shapes the painter actually consumes.
        near("smoothstep is 0 below its first edge", 0.0, Bloom.smoothstep(0.4, 0.9, 0.1), 1e-9);
        near("smoothstep is 1 above its last edge", 1.0, Bloom.smoothstep(0.4, 0.9, 1.0), 1e-9);
        near("smoothstep is a half in the middle", 0.5,
            Bloom.smoothstep(0.0, 1.0, 0.5), 1e-9);
        yes("smoothstep never leaves 0..1", inRange(Bloom.smoothstep(0.2, 0.8, -3.0))
            && inRange(Bloom.smoothstep(0.2, 0.8, 7.0)));

        // Monotonic: a ring that is opening must never go backwards mid-bloom.
        double last = -1;
        for (double x = 0; x <= 1.0001; x += 0.01) {
            double v = Bloom.smoothstep(0.0, 1.0, x);
            yes("smoothstep never decreases", v >= last - 1e-12);
            last = v;
        }
    }

    private static void theStagger() {
        int count = 12;
        // At the very start nothing has arrived; at the end everything has.
        for (int i = 0; i < count; i++) {
            near("item " + i + " has not started at t=0", 0.0,
                Bloom.stagger(0.0, i, count, 0.44), 1e-9);
            near("item " + i + " has arrived at t=1", 1.0,
                Bloom.stagger(1.0, i, count, 0.44), 1e-9);
        }
        // <b>Earlier items lead.</b> This is what makes a ring unfurl rather than appear -
        // if the order inverted, the ring would fill backwards, which reads as a glitch.
        double mid = 0.5;
        double first = Bloom.stagger(mid, 0, count, 0.44);
        double last = Bloom.stagger(mid, count - 1, count, 0.44);
        yes("the first item leads the last", first > last);
        yes("every item stays within 0..1",
            inRange(first) && inRange(last)
                && inRange(Bloom.stagger(0.3, 5, count, 0.44)));
        // No spread means everything moves together, which is the reduced-motion shape.
        near("with no spread the items move as one",
            Bloom.stagger(0.5, 0, count, 0.0), Bloom.stagger(0.5, count - 1, count, 0.0), 1e-9);
    }

    private static void interruption() throws Exception {
        Bloom b = new Bloom(false, 400, null);
        eq("a folded ring starts at nothing", 0.0, b.value());
        yes("and is not showing", !b.showing());

        b.setImmediately(true);
        eq("opened without animating, it is fully there", 1.0, b.value());
        yes("and it is showing", b.showing());

        // Reversing mid-bloom must continue from where it is, not from the far end.
        //
        // <b>Observed rather than timed.</b> This first slept a fixed 160ms against a 400ms
        // bloom and asserted the value was mid-range - which held on an idle machine and
        // failed on a busy one, because the sleep overshot the whole animation. A check that
        // passes four times in five is worse than no check: it teaches you to re-run rather
        // than to look. So the bloom is given a long duration and polled until a mid value is
        // actually seen, with a deadline; the assertions then run from a state that was
        // observed rather than assumed.
        Bloom slow = new Bloom(false, 3000, null);
        Settings.setAnimateRings(true);
        slow.set(true);
        double partway = 0.0;
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            double v = slow.value();
            if (v > 0.05 && v < 0.85) {
                partway = v;
                break;
            }
            Thread.sleep(10);
        }
        yes("a bloom passes through the middle, saw " + partway, partway > 0.0);
        if (partway > 0.0) {
            slow.set(false);
            double afterReversal = slow.value();
            yes("reversing does not jump the ring to full",
                afterReversal <= partway + 0.02);
            yes("and it is still on screen while folding", afterReversal > 0.0);
            // It must actually arrive, not stall wherever the reversal caught it.
            long fold = System.currentTimeMillis() + 5000;
            while (slow.value() > 1e-9 && System.currentTimeMillis() < fold) {
                Thread.sleep(20);
            }
            near("the fold completes", 0.0, slow.value(), 1e-9);
        }

        // <b>With motion off, every bloom still reaches its end.</b> A ring that never opens
        // because someone turned animation off would be a feature disabled by a preference
        // about movement.
        Settings.setAnimateRings(false);
        Bloom still = new Bloom(false, 400, null);
        still.set(true);
        eq("with motion off, opening is immediate", 1.0, still.value());
        yes("and the ring is showing", still.showing());
        still.set(false);
        eq("with motion off, folding is immediate", 0.0, still.value());
        Settings.setAnimateRings(true);

        // toggle() follows intent, not the current frame.
        Bloom t = new Bloom(false, 400, null);
        t.toggle();
        yes("toggling a folded ring opens it", t.opening());
        t.toggle();
        yes("toggling it again folds it", !t.opening());
    }

    private static boolean inRange(double v) {
        return v >= -1e-12 && v <= 1.0 + 1e-12;
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, double expected, double actual) {
        near(label, expected, actual, 1e-9);
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        System.out.println(part + (failures.size() == before ? ": clear" : ": FAILURES"));
    }
}
