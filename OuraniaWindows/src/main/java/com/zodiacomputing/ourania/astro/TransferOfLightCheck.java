package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verification and measurement for L4's transfer of light.
 *
 *  Part A - the sentence is built from the finding. Every wording this class emits has to be
 *           reachable from the parts it kept, because the whole point of keeping records was
 *           that a card and the report describe one transfer the same way.
 *  Part B - findTransfers is exactly the records' sentences, deduplicated. This is the rule
 *           that stops the two drifting: the report's list is not computed a second time.
 *  Part C - the physics. A translator is faster than both ends; a collector is slower than
 *           both. That is what makes the figure the figure rather than three bodies in aspect.
 *  Part D - each finding carries the two aspects that make it, and each names its own ends.
 *  Part E - measurement: how many transfers a real chart yields, and how many of the
 *           translations are strict - the two ends unable to reach each other without the
 *           mediator. The engine has always surfaced both; until now nobody could count them.
 *
 * <b>This engine had no suite at all.</b> It feeds section 6 of the synthesis report, and
 * before today the only thing asserting anything about it was CompositeCheck, in passing.
 *
 *   java -cp "out-selftest;src\main\java" com.zodiacomputing.ourania.astro.TransferOfLightCheck
 */
public final class TransferOfLightCheck {

    // The synthetic reference chart TopicCheck and JoyCheck read - 1984-09-08 07:33 UT,
    // 41.8781 N 87.6298 W. Not anyone's real birth data. Shared so the numbers below can be
    // compared with those suites' measurements rather than standing alone.
    private static final int NATAL_Y = 1984;
    private static final int NATAL_M = 9;
    private static final int NATAL_D = 8;
    private static final double NATAL_UT = 7.0 + 33.0 / 60.0;
    private static final double NATAL_LAT = 41.8781;
    private static final double NATAL_LON = -87.6298;

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(NATAL_Y, NATAL_M, NATAL_D, NATAL_UT).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, NATAL_LAT, NATAL_LON, 'P', false, 0.0);
        List<Aspects.Hit> hits = Aspects.betweenBodies(f);
        List<TransferOfLight.Transfer> found = TransferOfLight.find(f, hits);

        System.out.println("=== Part A: the sentence is built from the finding ===");
        theSentenceIsTheParts(found);

        System.out.println();
        System.out.println("=== Part B: the report's list is the records' sentences ===");
        oneListNotTwo(f, hits, found);

        System.out.println();
        System.out.println("=== Part C: a translator is faster, a collector is slower ===");
        thePhysics(f, found);

        System.out.println();
        System.out.println("=== Part D: each finding carries the aspects that make it ===");
        itCarriesItsAspects(found);

        System.out.println();
        System.out.println("=== Part E: what a real chart yields ===");
        measurement(found);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String s : failures) {
                System.out.println("  " + s);
            }
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- part A

    private static void theSentenceIsTheParts(List<TransferOfLight.Transfer> found) {
        yes("the chart has transfers to check at all", !found.isEmpty());
        for (TransferOfLight.Transfer t : found) {
            String expected = t.kind == TransferOfLight.Kind.TRANSLATION
                ? t.mediator + " translates light from " + t.from + " to " + t.to
                : t.mediator + " collects light from " + t.from + " and " + t.to;
            eq("the wording is the parts", expected, t.sentence());
            yes("every part of " + t.sentence() + " is named",
                t.mediator != null && !t.mediator.isEmpty()
                    && t.from != null && !t.from.isEmpty()
                    && t.to != null && !t.to.isEmpty());
            // The direction of a translation is the figure; the two ends are not the same claim.
            yes("its ends are two different bodies: " + t.sentence(), !t.from.equals(t.to));
            yes("and neither end is the mediator: " + t.sentence(),
                !t.mediator.equals(t.from) && !t.mediator.equals(t.to));
        }
    }

    // ---------------------------------------------------------------- part B

    /**
     * The report's sentences and the records cannot disagree, because there is one wording.
     *
     * Asserted rather than assumed: findTransfers is a separate public method, and a caller
     * "helpfully" reformatting there is exactly how the card and the report would come to
     * describe one transfer two ways.
     */
    private static void oneListNotTwo(ChartFrame f, List<Aspects.Hit> hits,
                                      List<TransferOfLight.Transfer> found) {
        List<String> sentences = TransferOfLight.findTransfers(f, hits);
        List<String> fromRecords = new ArrayList<>();
        for (TransferOfLight.Transfer t : found) {
            if (!fromRecords.contains(t.sentence())) {
                fromRecords.add(t.sentence());
            }
        }
        eq("the report's list is the records' list", fromRecords, sentences);
        eq("and in the same order", fromRecords.toString(), sentences.toString());

        // <b>Deduplication, made reachable.</b> On this chart no two pairings of hits produce
        // the same finding, so removing the dedup changes nothing and a mutation of it
        // SURVIVED - measured 2026-09-27. An assertion that cannot be made to fail is not
        // holding anything. Handing find() the same aspects twice is the condition the dedup
        // exists for, and it is the condition a caller creates by accident: the transit and
        // natal hit lists overlap wherever the same pair is in aspect in both.
        List<Aspects.Hit> doubled = new ArrayList<>(hits);
        doubled.addAll(hits);
        eq("the same aspect listed twice is still one finding",
            found.size(), TransferOfLight.find(f, doubled).size());

        Set<String> ids = new HashSet<>();
        for (TransferOfLight.Transfer t : found) {
            yes("no finding is reported twice: " + t.sentence(),
                ids.add(t.kind + "|" + t.mediator + "|" + t.from + "|" + t.to));
        }
    }

    // ---------------------------------------------------------------- part C

    private static void thePhysics(ChartFrame f, List<TransferOfLight.Transfer> found) {
        for (TransferOfLight.Transfer t : found) {
            double mediator = speed(f, t.mediator);
            double from = speed(f, t.from);
            double to = speed(f, t.to);
            if (t.kind == TransferOfLight.Kind.TRANSLATION) {
                yes("the translator is the faster body: " + t.sentence()
                    + String.format(" (%.4f vs %.4f, %.4f)", mediator, from, to),
                    mediator > from && mediator > to);
            } else {
                yes("the collector is the slower body: " + t.sentence()
                    + String.format(" (%.4f vs %.4f, %.4f)", mediator, from, to),
                    mediator < from && mediator < to);
            }
        }
    }

    /** The engine compares absolute speed, so a retrograde body is judged on its rate. */
    private static double speed(ChartFrame f, String name) {
        ChartFrame.Body b = f.body(name);
        return b == null ? Double.NaN : Math.abs(b.lonSpeed);
    }

    // ---------------------------------------------------------------- part D

    private static void itCarriesItsAspects(List<TransferOfLight.Transfer> found) {
        for (TransferOfLight.Transfer t : found) {
            yes("it carries both its aspects: " + t.sentence(),
                t.fromHit != null && t.toHit != null);
            if (t.fromHit == null || t.toHit == null) {
                continue;
            }
            yes("the from end names the mediator and the from body: " + t.sentence(),
                names(t.fromHit, t.mediator, t.from));
            yes("the to end names the mediator and the to body: " + t.sentence(),
                names(t.toHit, t.mediator, t.to));
            // A translation LEAVES one end and REACHES the other; a collection is applied to
            // from both sides. That direction is the difference between the two figures.
            if (t.kind == TransferOfLight.Kind.TRANSLATION) {
                yes("it is separating from the from end: " + t.sentence(), !t.fromHit.applying);
            } else {
                yes("both ends are applying: " + t.sentence(), t.fromHit.applying);
            }
            yes("it is applying to the to end: " + t.sentence(), t.toHit.applying);
        }
    }

    private static boolean names(Aspects.Hit h, String one, String other) {
        return (h.a.equals(one) && h.b.equals(other)) || (h.a.equals(other) && h.b.equals(one));
    }

    // ---------------------------------------------------------------- part E

    /**
     * A count, not a threshold.
     *
     * The strict figure is the tradition's: the two ends cannot reach each other, so the light
     * only crosses through the mediator. This engine surfaces the loose form too and always
     * has. Printing both is what lets that decision be made on a number.
     */
    private static void measurement(List<TransferOfLight.Transfer> found) {
        int translations = 0;
        int collections = 0;
        int strictTranslations = 0;
        for (TransferOfLight.Transfer t : found) {
            if (t.kind == TransferOfLight.Kind.TRANSLATION) {
                translations++;
                if (!t.endsAspectEachOther) {
                    strictTranslations++;
                }
            } else {
                collections++;
            }
        }
        System.out.println("  translations: " + translations + ", of which strict (the ends "
            + "cannot reach each other): " + strictTranslations);
        System.out.println("  collections:  " + collections);
        yes("the two kinds account for every finding",
            translations + collections == found.size());
        yes("no more strict translations than translations", strictTranslations <= translations);
    }

    // ---------------------------------------------------------------- harness

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }
}
