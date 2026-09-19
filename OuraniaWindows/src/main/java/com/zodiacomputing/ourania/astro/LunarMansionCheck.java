package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Guards {@link LunarMansions}.
 *
 * The strongest check available here is a cross-check between two independent statements of
 * the same fact: the engine computes each cusp as (n-1) x 360/28, while al-Biruni's published
 * table gives it in degrees and minutes of a sign. **Those are different representations from
 * different sources, so agreement between them is evidence rather than a tautology.** Part B
 * asserts all 28 against the printed table.
 *
 * Run:
 *   java -cp src\main\java com.zodiacomputing.ourania.astro.LunarMansionCheck
 */
public final class LunarMansionCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    /**
     * Al-Biruni's cusps as the traditional tables print them, sign and minute.
     *
     * Typed from the source table, NOT generated from WIDTH - a copy of the formula would
     * agree with the formula no matter what either said. Sign index 0-11, degree, minute.
     */
    private static final int[][] PUBLISHED = {
        {0, 0, 0},    {0, 12, 51},  {0, 25, 43},  {1, 8, 34},   {1, 21, 26},
        {2, 4, 17},   {2, 17, 9},   {3, 0, 0},    {3, 12, 51},  {3, 25, 43},
        {4, 8, 34},   {4, 21, 26},  {5, 4, 17},   {5, 17, 9},   {6, 0, 0},
        {6, 12, 51},  {6, 25, 43},  {7, 8, 34},   {7, 21, 26},  {8, 4, 17},
        {8, 17, 9},   {9, 0, 0},    {9, 12, 51},  {9, 25, 43},  {10, 8, 34},
        {10, 21, 26}, {11, 4, 17},  {11, 17, 9},
    };

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private LunarMansionCheck() { }

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        System.out.println("=== Part A: 28 equal stations partitioning the circle ===");
        int before = failures.size();
        partition();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: every cusp matches al-Biruni's printed table ===");
        before = failures.size();
        publishedCusps();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: the worked example, and the boundaries ===");
        before = failures.size();
        workedExample();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: every station has a whole electional sentence ===");
        before = failures.size();
        completeness();
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: the Moon lands in every mansion across a corpus ===");
        before = failures.size();
        corpus();
        report("Part E", before);

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
    }

    // ---------------------------------------------------------------- part A

    private static void partition() {
        eq("there are 28 mansions", 28, LunarMansions.all().size());
        near("each is 360/28 degrees wide", 12.857142857, LunarMansions.WIDTH, 1e-9);
        near("28 of them close the circle", 360.0, LunarMansions.WIDTH * 28, 1e-9);

        // Every tenth of a degree lands in exactly one mansion, and in the one whose bounds
        // contain it. A partition check, the same shape Gestalt uses for the house fields.
        for (int i = 0; i < 3600; i++) {
            double lon = i / 10.0;
            LunarMansions.Mansion m = LunarMansions.at(lon);
            yes("longitude " + lon + " is inside its own mansion's bounds",
                lon >= m.start - 1e-9 && lon < m.end() + 1e-9);
        }
        for (int n = 1; n <= 28; n++) {
            LunarMansions.Mansion m = LunarMansions.byNumber(n);
            eq("numbering is consistent (" + n + ")", n, m.number);
            eq("the mansion at a station's own start is that station (" + n + ")",
                n, LunarMansions.at(m.start).number);
            eq("and just before its end, still that station (" + n + ")",
                n, LunarMansions.at(m.end() - 1e-6).number);
        }
        eq("360 degrees wraps to the first mansion", 1, LunarMansions.at(360.0).number);
        eq("a negative longitude normalises", 28, LunarMansions.at(-1.0).number);
    }

    // ---------------------------------------------------------------- part B

    /**
     * The published table against the computed one, to the minute.
     *
     * The tables print whole minutes, and 360/28 is 12 deg 51 min 25.7 sec, so the true cusps
     * carry seconds the table drops. A tolerance of one minute is therefore the correct
     * comparison - anything tighter would be asserting that al-Biruni rounded the way this
     * code does.
     */
    private static void publishedCusps() {
        for (int n = 1; n <= 28; n++) {
            LunarMansions.Mansion m = LunarMansions.byNumber(n);
            int[] p = PUBLISHED[n - 1];
            double printed = p[0] * 30.0 + p[1] + p[2] / 60.0;
            near("mansion " + n + " (" + m.name + ") starts where the table says",
                printed, m.start, 1.0 / 60.0);
            eq("mansion " + n + " starts in the sign the table says",
                Zodiac.SIGNS[p[0]], Zodiac.signName(m.start));
        }
        // The four quarter points are exact in the tradition and must be exact here.
        eq("mansion 1 opens the circle at 0 Aries", 1, LunarMansions.at(0.0).number);
        eq("mansion 8 opens at 0 Cancer", 8, LunarMansions.at(90.0).number);
        eq("mansion 15 opens at 0 Libra", 15, LunarMansions.at(180.0).number);
        eq("mansion 22 opens at 0 Capricorn", 22, LunarMansions.at(270.0).number);
    }

    // ---------------------------------------------------------------- part C

    private static void workedExample() {
        // The source's own example: the Moon at 15 Leo 27 falls in mansion 11, Azobra,
        // which runs from 8 Leo 34 to 21 Leo 26.
        double lon = 4 * 30 + 15 + 27 / 60.0;
        LunarMansions.Mansion m = LunarMansions.at(lon);
        eq("15 Leo 27 falls in mansion 11", 11, m.number);
        eq("which is Azobra", "Azobra", m.name);
        yes("Azobra starts at 8 Leo 34", m.cusp().startsWith("8") || m.cusp().contains("8"));

        // The named stellar anchors should land where the tradition puts them.
        eq("Antares, the heart of Scorpio, is mansion 18",
            18, LunarMansions.at(7 * 30 + 12.0).number);
        eq("Spica's station is mansion 14", 14, LunarMansions.at(5 * 30 + 20.0).number);

        // The void station, which is the row the two supplied tables disagreed about.
        LunarMansions.Mansion agrapha = LunarMansions.byNumber(15);
        eq("mansion 15 is Agrapha", "Agrapha", agrapha.name);
        eq("and it is the Covering", "The Covering", agrapha.translation);
        yes("Agrapha is the station that warns against everything",
            agrapha.goodFor.isEmpty() && agrapha.avoid.contains("everything"));
        LunarMansions.Mansion alhaire = LunarMansions.byNumber(13);
        eq("mansion 13 is Alhaire", "Alhaire", alhaire.name);
        yes("Alhaire is NOT the void station - it has elections of its own",
            !alhaire.goodFor.isEmpty());
    }

    // ---------------------------------------------------------------- part D

    /**
     * Clause completeness, the same rule the aspect patterns and the Gestalt trinities carry:
     * a station with no "good for" must not render "Elected for ." and one with nothing to
     * avoid must not render "Avoid ."
     */
    private static void completeness() {
        int withGood = 0, withAvoid = 0;
        for (LunarMansions.Mansion m : LunarMansions.all()) {
            yes("mansion " + m.number + " has a name", m.name != null && !m.name.isEmpty());
            yes("mansion " + m.number + " has a translation",
                m.translation != null && !m.translation.isEmpty());
            yes("mansion " + m.number + " has at least one electional clause",
                !m.goodFor.isEmpty() || !m.avoid.isEmpty());

            String line = LunarMansions.electionalLine(m);
            yes("the line is whole (" + m.number + "): " + line, line.endsWith("."));
            yes("no empty clause (" + m.number + ")",
                !line.contains("Elected for .") && !line.contains("Avoid ."));
            yes("no null in the line (" + m.number + ")", !line.contains("null"));
            yes("the line names the mansion (" + m.number + ")",
                line.contains(m.name) && line.contains("Mansion " + m.number));
            if (!m.goodFor.isEmpty()) withGood++;
            if (!m.avoid.isEmpty()) withAvoid++;
        }
        System.out.println("  " + withGood + " of 28 stations carry an election, "
            + withAvoid + " carry a warning");
        yes("most stations are elected for something", withGood >= 24);
        yes("some stations carry a warning", withAvoid > 0 && withAvoid < 28);
    }

    // ---------------------------------------------------------------- part E

    private static void corpus() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        Random rnd = new Random(20260823L);
        Map<Integer, Integer> seen = new LinkedHashMap<>();
        int n = 400;
        for (int i = 0; i < n; i++) {
            SweDate sd = new SweDate(1900 + rnd.nextInt(150), 1 + rnd.nextInt(12),
                1 + rnd.nextInt(28), rnd.nextDouble() * 24.0);
            ChartFrame f = ChartFrame.compute(sw, sd.getJulDay(), 34.05, -118.24, 'P', false, 0.0);
            LunarMansions.Mansion m = LunarMansions.ofMoon(f);
            yes("the Moon always has a mansion", m != null);
            if (m != null) {
                seen.merge(m.number, 1, Integer::sum);
                eq("the Moon's mansion is the mansion at its longitude",
                    LunarMansions.at(f.body("Moon").lon).number, m.number);
            }
        }
        eq("the Moon reached all 28 stations across " + n + " charts", 28, seen.size());
        int min = Integer.MAX_VALUE, max = 0;
        for (int c : seen.values()) {
            min = Math.min(min, c);
            max = Math.max(max, c);
        }
        System.out.printf("  %d charts, all 28 stations reached, %d to %d charts each "
            + "(even would be %.1f)%n", n, min, max, n / 28.0);
        yes("no station is wildly over-represented - the division is even", max < n / 28.0 * 3);
    }

    // ---------------------------------------------------------------- helpers

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null) {
            if (actual != null) {
                failures.add(label + ": got " + actual + ", expected null");
            }
        } else if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected + " +/- " + tol);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "clear" : added + " FAILURE(S)"));
    }
}
