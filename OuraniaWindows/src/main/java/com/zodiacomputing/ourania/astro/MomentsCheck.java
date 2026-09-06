package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The two things a chart has to assume about a written time, and say that it assumed.
 *
 * <p><b>Both were silent before this suite existed.</b> A local time inside a repeated
 * daylight-saving hour names two instants an hour apart, and Java picks one; a time inside a
 * skipped hour names none, and Java shifts it. An hour of real time is about fifteen degrees
 * of Ascendant, so those are chart-changing choices that were being made on the reader's
 * behalf with nothing on screen to say so. Separately, a chart with no known birth time was
 * simply not expressible - the field demanded one, and a guess typed into it produced angles
 * indistinguishable from real ones.
 *
 * <p>Part C is the one that would otherwise rot: the suppression works through {@code ok},
 * which every reader in the app already honours, and the temptation in a hurry is to add an
 * {@code if (timeUnknown)} branch to whichever surface is being fixed that day.
 */
public final class MomentsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** New York: has DST, and has the historical oddities worth testing. */
    private static final ZoneId NY = ZoneId.of("America/New_York");

    public static void main(String[] args) {
        System.out.println("=== Part A: an ordinary time is resolved and not remarked on ===");
        int before = failures.size();
        ordinary();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: the hour that happened twice, and the one that did not ===");
        before = failures.size();
        daylightSaving();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: a chart with no birth time has no angles ===");
        before = failures.size();
        timeless();
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

    private static void ordinary() {
        Moments.Resolved r = Moments.resolve(
            LocalDate.of(1990, 1, 1), LocalTime.of(12, 0), NY);
        eq("an ordinary noon is NORMAL", Moments.Kind.NORMAL, r.kind);
        yes("an ordinary time is not flagged uncertain", !r.uncertain());
        yes("an ordinary time carries no note", r.note == null);
        yes("an ordinary time has no alternative reading", r.other == null);
        eq("an ordinary time keeps the time it was given",
            LocalTime.of(12, 0), r.when.toLocalTime());

        // <b>Historical daylight saving is real and the tz database has it.</b> The United
        // States ran DST all year in 1943 (war time) and again in 1974 (the energy crisis).
        // Software that applies today's rules to an old birth date is wrong by an hour for
        // millions of people, which is the same fifteen degrees of Ascendant as the ambiguous
        // hour below - just harder to notice.
        eq("January 1943 is on war time", "-04:00",
            Moments.resolve(LocalDate.of(1943, 1, 15), LocalTime.NOON, NY)
                .when.getOffset().getId());
        eq("January 1974 is on year-round DST", "-04:00",
            Moments.resolve(LocalDate.of(1974, 1, 15), LocalTime.NOON, NY)
                .when.getOffset().getId());
        eq("January 2007 is on standard time", "-05:00",
            Moments.resolve(LocalDate.of(2007, 1, 15), LocalTime.NOON, NY)
                .when.getOffset().getId());
    }

    private static void daylightSaving() {
        // The clocks go back on 1 Nov 2026: 01:30 happens twice.
        Moments.Resolved dup = Moments.resolve(
            LocalDate.of(2026, 11, 1), LocalTime.of(1, 30), NY);
        eq("a repeated hour is REPEATED", Moments.Kind.REPEATED, dup.kind);
        yes("a repeated hour is flagged uncertain", dup.uncertain());
        yes("a repeated hour explains itself", dup.note != null && dup.note.length() > 40);
        yes("a repeated hour offers the reading not taken", dup.other != null);
        long apart = Duration.between(dup.when.toInstant(), dup.other.toInstant()).toMinutes();
        eq("the two readings are an hour of real time apart", 60L, apart);
        yes("the two readings show the same clock time",
            dup.when.toLocalTime().equals(dup.other.toLocalTime()));
        yes("the two readings differ in offset",
            !dup.when.getOffset().equals(dup.other.getOffset()));

        // <b>The default must not move.</b> This class reports the ambiguity; it does not
        // resolve it differently from before. Changing which instant an existing chart lands
        // on would silently redraw every saved chart cast in a repeated hour.
        eq("the reading taken is still the earlier offset", "-04:00",
            dup.when.getOffset().getId());

        // The clocks go forward on 8 Mar 2026: 02:30 never happens.
        Moments.Resolved gap = Moments.resolve(
            LocalDate.of(2026, 3, 8), LocalTime.of(2, 30), NY);
        eq("a skipped hour is SKIPPED", Moments.Kind.SKIPPED, gap.kind);
        yes("a skipped hour is flagged uncertain", gap.uncertain());
        yes("a skipped hour explains itself", gap.note != null && gap.note.length() > 40);
        yes("a skipped hour has no alternative - the hour simply is not there",
            gap.other == null);
        eq("a skipped hour is moved past the gap", LocalTime.of(3, 30),
            gap.when.toLocalTime());

        // A zone with no daylight saving can never produce either.
        ZoneId utc = ZoneId.of("UTC");
        for (int month = 1; month <= 12; month++) {
            Moments.Resolved r = Moments.resolve(
                LocalDate.of(2026, month, 1), LocalTime.of(1, 30), utc);
            yes("UTC has no ambiguous hours (month " + month + ")",
                r.kind == Moments.Kind.NORMAL);
        }
    }

    private static void timeless() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double noon = new SweDate(1990, 1, 1, 12.0).getJulDay();

        ChartFrame t = ChartFrame.computeTimeUnknown(sw, noon, 40.71, -74.01, 'P', false, 0.0);
        ChartFrame n = ChartFrame.compute(sw, noon, 40.71, -74.01, 'P', false, 0.0);

        yes("a timeless chart says so", t.timeUnknown);
        yes("a timeless chart explains what is missing",
            t.timeUnknownNote != null && t.timeUnknownNote.length() > 40);
        yes("an ordinary chart does not claim to be timeless", !n.timeUnknown);

        int planets = 0;
        int angles = 0;
        int lots = 0;
        for (int i = 0; i < Bodies.count() && i < t.bodies.length; i++) {
            ChartFrame.Body b = t.bodies[i];
            if (b == null) {
                continue;
            }
            if (Bodies.at(i).isAngle()) {
                if (b.ok) {
                    angles++;
                }
            } else if (b.name.startsWith("Part of")) {
                if (b.ok) {
                    lots++;
                }
            } else if (b.ok) {
                planets++;
            }
        }
        eq("no angle is offered without a birth time", 0, angles);
        eq("neither Lot is offered without a birth time", 0, lots);
        yes("the planets are still there - they barely move in a day", planets >= 10);
        yes("the Lots are absent rather than zero",
            Double.isNaN(t.lotOfFortune) && Double.isNaN(t.lotOfSpirit));

        // <b>The same chart WITH a time must be untouched.</b> The suppression is opt-in, and
        // a defect that switched it on everywhere would be invisible in the timeless case.
        int normalAngles = 0;
        for (int i = 0; i < Bodies.count() && i < n.bodies.length; i++) {
            ChartFrame.Body b = n.bodies[i];
            if (b != null && b.ok && Bodies.at(i).isAngle()) {
                normalAngles++;
            }
        }
        yes("an ordinary chart still has its angles", normalAngles > 0);
        yes("an ordinary chart still has its Lots",
            !Double.isNaN(n.lotOfFortune) && !Double.isNaN(n.lotOfSpirit));

        // The planets themselves must be identical - the noon cast is the same instant, and
        // only the angle-derived points were withheld.
        for (int i = 0; i < Bodies.count() && i < t.bodies.length; i++) {
            ChartFrame.Body a = t.bodies[i];
            ChartFrame.Body b = n.bodies[i];
            if (a == null || b == null || !a.ok || Bodies.at(i).isAngle()) {
                continue;
            }
            near("the same body is in the same degree with or without a time (" + a.name + ")",
                b.lon, a.lon, 1e-9);
        }
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
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
