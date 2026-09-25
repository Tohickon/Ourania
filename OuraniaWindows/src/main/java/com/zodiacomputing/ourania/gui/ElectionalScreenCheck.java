package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Electional;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Horary;
import com.zodiacomputing.ourania.astro.PlanetaryHours;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Master list F10, the last piece: the screen that weighs a moment.
 *
 * <p><b>{@code PlanetaryHours} had no door at all</b> - {@code NavigationCheck} reported it
 * unreachable from the day it was written - and {@code Electional} had none either while
 * <i>appearing</i> to, because the reachability sweep matched the word "Electional" inside a
 * sentence about al-Biruni in {@code InterpretationPanel}. Both are answered by one screen,
 * because the day and hour rulers are the first two measures an election is made on.
 *
 * <ul>
 * <li><b>A, every standing has its own sentence</b>, and none falls through to another's.</li>
 * <li><b>B, the screen adds no astrology</b> - every testimony the engine produced appears,
 * carrying the engine's own words and not a second telling of them.</li>
 * <li><b>C, the polar case</b> - where the Sun neither rises nor sets there are no hours to
 * divide, and the screen says so instead of showing an empty verdict.</li>
 * <li><b>D, the hours are shown</b> - the whole day, which is how a reader finds a <i>better</i>
 * moment rather than only judging the one they typed.</li>
 * <li><b>E, a typed moment is read in the PLACE's zone</b>, not this computer's.</li>
 * <li><b>F, the screen is reachable</b>, which is what {@link NavigationCheck} cross-checks.</li>
 * </ul>
 */
public final class ElectionalScreenCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** David's chart, the one the other suites use. */
    private static final double LAT = 39.9526;
    private static final double LON = -75.1652;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        part("A: every standing has its own sentence", ElectionalScreenCheck::standings);
        part("B: the engine's words, not a second telling", () -> notes(sw));
        part("C: the polar case says so", ElectionalScreenCheck::polar);
        part("D: the whole day's hours are shown", () -> hours(sw));
        part("E: a moment is read in the place's own zone", ElectionalScreenCheck::zones);
        part("F: the screen is reachable", ElectionalScreenCheck::reachable);
        part("G: a word in a sentence is not a door", ElectionalScreenCheck::codeOnly);

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

    /**
     * Each standing gets a distinct sentence.
     *
     * <b>Distinctness is the assertion, not the wording.</b> A switch whose cases fall through
     * would still produce a sentence for every standing; what it would not produce is a
     * <i>different</i> one, and a reader told "a mixed moment" about a favoured one has been
     * misinformed by the screen rather than by the engine.
     */
    private static void standings() {
        java.util.Set<String> seen = new java.util.TreeSet<>();
        for (Electional.Standing want : Electional.Standing.values()) {
            Electional.Quality q = qualityWith(want);
            String line = ElectionalPanel.standingLine(q);
            ok(want + " has a sentence", line != null && !line.trim().isEmpty());
            ok(want + "'s sentence is not another's", seen.add(line.replaceAll("[0-9]+", "#")));
        }
        // UNKNOWN is the one that must never read as a judgement, because it is the absence of one.
        Electional.Quality u = qualityWith(Electional.Standing.UNKNOWN);
        String unknown = ElectionalPanel.standingLine(u).toLowerCase(java.util.Locale.ROOT);
        ok("the unweighed moment is not called favoured or ill-favoured",
            !unknown.contains("favoured"));
    }

    // ------------------------------------------------------------------ B

    private static void notes(SwissEph sw) {
        Electional.Quality q = assessAt(sw, 2026, 1, 7, 12.0, Horary.Matter.values()[0]);
        String html = ElectionalPanel.render(q, null, Horary.Matter.values()[0],
            ZonedDateTime.now(ZoneId.of("UTC")), null);

        ok("the weighing produced testimonies to show", !q.notes.isEmpty());
        int shown = 0;
        for (Electional.Note n : q.notes) {
            // <b>The engine's own sentence, escaped as the screen escapes it.</b> Comparing
            // against a reworded version would be checking that the screen agrees with this
            // suite, which is a second author of the same text - the defect being guarded.
            if (html.contains(ElectionalPanel.escape(n.because))) {
                shown++;
            } else {
                fail("a testimony the engine produced is not on the screen: " + n.because);
            }
        }
        ok("every testimony reaches the reader", shown == q.notes.size());
        ok("the rulers are named", html.contains(ElectionalPanel.escape(q.hourRuler))
            && html.contains(ElectionalPanel.escape(q.dayRuler)));
        ok("and the standing is stated", html.contains(ElectionalPanel.standingLine(q)));
    }

    // ------------------------------------------------------------------ C

    private static void polar() {
        Electional.Quality q = new Electional.Quality();
        q.unknown = true;
        String html = ElectionalPanel.render(q, null, null,
            ZonedDateTime.now(ZoneId.of("UTC")), null);
        String flat = html.toLowerCase(java.util.Locale.ROOT);
        ok("it says the hours cannot be divided", flat.contains("cannot be divided"));
        ok("and does not offer a verdict anyway", !flat.contains("a favoured moment")
            && !flat.contains("an ill-favoured moment") && !flat.contains("a neutral moment"));
        ok("and does not print an empty ruler", !html.contains("<b></b>"));
    }

    // ------------------------------------------------------------------ D

    private static void hours(SwissEph sw) {
        double jd = new SweDate(2026, 1, 7, 12.0).getJulDay();
        PlanetaryHours.Day day = PlanetaryHours.at(sw, jd, LAT, LON);
        ok("the day has its hours", day != null && day.hours.size() == 24);
        if (day == null) {
            return;
        }
        Electional.Quality q = assessAt(sw, 2026, 1, 7, 12.0, null);
        String html = ElectionalPanel.render(q, day, null,
            ZonedDateTime.now(ZoneId.of("UTC")), null);

        // <b>All twenty-four, not the one in force.</b> The reason this table exists is to let a
        // reader find a better hour; showing only the current one answers a different question.
        int named = 0;
        for (PlanetaryHours.Hour h : day.hours) {
            if (html.contains(">" + h.index + "</td>")) {
                named++;
            }
        }
        ok("every one of the day's hours is listed", named == 24);
        ok("the day's own ruler is named", html.contains(ElectionalPanel.escape(day.ruler)));
        ok("and what an hour favours is offered with it",
            html.contains(ElectionalPanel.escape(Electional.favours(day.hours.get(0).ruler))));
    }

    // ------------------------------------------------------------------ E

    /**
     * A typed moment belongs to the place, not to this computer.
     *
     * <b>The planetary hour is the first thing this screen reports</b>, so reading a moment in
     * the wrong zone reports a different hour with no sign that anything is wrong - an election
     * made for a venue three zones away would be weighed for the wrong part of its day.
     */
    private static void zones() {
        LocalDateTime m = ElectionalPanel.parse("2026-01-07 14:30");
        ok("a moment is read from the reader's own format", m != null);
        if (m == null) {
            return;
        }
        ok("with the hour it was given", m.getHour() == 14 && m.getMinute() == 30);

        double here = jdOf(m.atZone(ZoneId.of("America/New_York")));
        double there = jdOf(m.atZone(ZoneId.of("Asia/Tokyo")));
        // Not "they differ" - by how much, because a wrong sign would also differ.
        ok("the same clock time in two places is not the same instant", here > there);
        ok("and differs by those places' offsets", Math.abs((here - there) * 24.0 - 14.0) < 0.01);

        // <b>And the panel actually uses the place's zone.</b> Everything above is true of
        // java.time whatever this screen does; without this the rule and its only consumer are
        // checked apart, which is the trap AspectGridCheck's own comment names.
        Geocoder.Result tokyo = new Geocoder.Result();
        tokyo.tzId = "Asia/Tokyo";
        ok("the panel reads a moment in the place's zone",
            ZoneId.of("Asia/Tokyo").equals(ElectionalPanel.zoneOf(tokyo)));
        Geocoder.Result nowhere = new Geocoder.Result();
        ok("and falls back to this computer's when the place has none",
            ZoneId.systemDefault().equals(ElectionalPanel.zoneOf(nowhere)));
        Geocoder.Result nonsense = new Geocoder.Result();
        nonsense.tzId = "Middle/Earth";
        ok("an unknown zone is not worth refusing the election over",
            ZoneId.systemDefault().equals(ElectionalPanel.zoneOf(nonsense)));

        ok("a shape without minutes is still read", ElectionalPanel.parse("2026-01-07") != null);
        ok("and nonsense is refused rather than guessed",
            ElectionalPanel.parse("next tuesday") == null);
        ok("as is an empty box", ElectionalPanel.parse("  ") == null);
    }

    // ------------------------------------------------------------------ F

    private static void reachable() {
        boolean inRail = false;
        for (String[] row : SidePanel.SCREENS) {
            if ("ELECTIONAL".equals(row[1])) {
                inRail = true;
                ok("the rail entry has a name", row[0] != null && !row[0].trim().isEmpty());
                ok("and says what the screen does", row[2] != null && row[2].length() > 30);
            }
        }
        ok("the rail offers Electional", inRail);

        // The two engines this screen exists to give a door to, referenced as code rather than
        // named in a sentence - which is the distinction NavigationCheck could not make until
        // it started stripping string literals.
        String src = read("ElectionalPanel.java");
        ok("the screen calls Electional.assess", src.contains("Electional.assess("));
        ok("and PlanetaryHours.at", src.contains("PlanetaryHours.at("));
        ok("NavigationCheck expects the screen too",
            java.util.Arrays.asList(read("NavigationCheck.java").split("\"")).contains("ELECTIONAL"));
    }

    // ------------------------------------------------------------------ G

    /**
     * {@link NavigationCheck#codeOnly} really removes what it claims to.
     *
     * <b>The fix that found this screen's defect was itself unheld.</b> Reverting it would take
     * no suite red today, because Electional now has a real door - so the only thing standing
     * between the sweep and a second false pass is a hand-written scanner nobody was testing.
     *
     * <p>The escaped quote is the one that matters. Get it wrong and the scanner runs off the end
     * of a literal and eats the code after it, which does not fail loudly: it makes every class
     * used below that point look unreachable, or - worse, and in the same direction as the
     * original defect - swallows a whole file into one apparent string.
     */
    private static void codeOnly() {
        ok("a name inside a string literal is not a door",
            !NavigationCheck.codeOnly("String s = \"Electional and horary\";")
                .contains("Electional"));
        ok("but a call to it is",
            NavigationCheck.codeOnly("Electional.assess(sw, f);").contains("Electional"));
        ok("a line comment is not a door",
            !NavigationCheck.codeOnly("// PlanetaryHours would go here\n").contains(
                "PlanetaryHours"));
        ok("nor a block comment",
            !NavigationCheck.codeOnly("/* see PlanetaryHours */").contains("PlanetaryHours"));

        // <b>The escaped quote.</b> A scanner that treats \" as the end of the literal carries on
        // consuming as if it were code - or, having lost its place, swallows the real code after
        // it. Both are silent.
        String tricky = "String q = \"he said \\\"no\\\"\"; Electional.assess();";
        String kept = NavigationCheck.codeOnly(tricky);
        ok("an escaped quote does not end the literal", !kept.contains("he said"));
        ok("and the code after it survives", kept.contains("Electional.assess"));

        // A character literal holding a quote is the same trap in miniature.
        ok("a character literal is skipped too",
            NavigationCheck.codeOnly("char c = '\\''; PlanetaryHours.at(sw);")
                .contains("PlanetaryHours.at"));

        // And the sweep's real input: the sentence that fooled it, in its actual file.
        String real = read("InterpretationPanel.java");
        ok("the blurb that caused this names Electional", real.contains("Electional and horary"));
        ok("and is gone once the literals are",
            !NavigationCheck.codeOnly(real).contains("Electional and horary"));

        // <b>And the sweep itself uses it.</b> Everything above holds codeOnly; none of it held
        // the SWEEP, and a mutation reverting that to raw text survived the lot. This reads back
        // what the sweep actually searches.
        java.io.File[] sources = NavigationCheck.guiSources();
        ok("the gui sources are readable from here", sources != null);
        if (sources == null) {
            return;
        }
        String swept = NavigationCheck.guiCode(sources);
        ok("the text the sweep searches has its literals stripped",
            !swept.contains("Electional and horary"));
        ok("but still holds the call that is a real door",
            swept.contains("Electional.assess("));
        ok("and PlanetaryHours' door with it", swept.contains("PlanetaryHours.at("));
    }

    private static String read(String name) {
        try {
            java.io.File f = new java.io.File(
                "src/main/java/com/zodiacomputing/ourania/gui/" + name);
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
        } catch (Exception e) {
            fail("could not read " + name + ": " + e);
            return "";
        }
    }

    // ------------------------------------------------------------------ helpers

    private static double jdOf(ZonedDateTime at) {
        ZonedDateTime utc = at.withZoneSameInstant(ZoneId.of("UTC"));
        return new SweDate(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
            utc.getHour() + utc.getMinute() / 60.0).getJulDay();
    }

    private static Electional.Quality assessAt(SwissEph sw, int y, int mo, int d, double hour,
                                               Horary.Matter matter) {
        double jd = new SweDate(y, mo, d, hour).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, LAT, LON, 'P', false, 0.0);
        return Electional.assess(sw, f, jd, LAT, LON, matter);
    }

    /** A Quality standing in the named state, built from notes rather than asserted into it. */
    private static Electional.Quality qualityWith(Electional.Standing want) {
        Electional.Quality q = new Electional.Quality();
        q.dayRuler = "Jupiter";
        q.hourRuler = "Venus";
        if (want == Electional.Standing.UNKNOWN) {
            q.unknown = true;
            return q;
        }
        int up = want == Electional.Standing.FAVOURED ? 2
            : want == Electional.Standing.MIXED ? 1 : 0;
        int down = want == Electional.Standing.ILL_FAVOURED ? 2
            : want == Electional.Standing.MIXED ? 1 : 0;
        for (int i = 0; i < up; i++) {
            q.notes.add(note(Electional.Weight.RAISES, "a benefic is angular"));
        }
        for (int i = 0; i < down; i++) {
            q.notes.add(note(Electional.Weight.LOWERS, "a malefic is angular and debilitated"));
        }
        // Built, then checked against the engine's own tally rather than trusted.
        ok("the fixture really stands " + want, q.standing() == want);
        return q;
    }

    private static Electional.Note note(Electional.Weight w, String because) {
        Electional.Note n = new Electional.Note();
        n.weight = w;
        n.body = "Jupiter";
        n.because = because;
        return n;
    }

    // ------------------------------------------------------------------ harness

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("=== Part " + title + " ===");
        int before = failures.size();
        try {
            body.run();
        } catch (Exception e) {
            fail(title + " threw " + e);
        }
        System.out.println("Part " + title.substring(0, 1) + ": "
            + (failures.size() == before ? "PASS" : (failures.size() - before) + " FAILURE(S)"));
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            fail(label);
        }
    }

    private static void fail(String label) {
        failures.add(label);
    }

    private ElectionalScreenCheck() {
    }
}
