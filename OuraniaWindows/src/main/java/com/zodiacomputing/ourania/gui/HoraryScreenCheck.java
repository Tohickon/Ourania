package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Horary;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Master list F10, K13 stage 4: the screen that presents a judgement.
 *
 * <p>The engine has judged since K13's third stage and nothing in the interface could show a
 * reader any of it - which is the whole reason F10 is not closed. What is held here is the
 * surface: that it presents what the engine said, in the engine's order, and that it adds
 * nothing of its own.
 *
 * <ul>
 * <li><b>A, the verdict</b> - every {@link Horary.Verdict} gets its own sentence, and none falls
 * through to another's.</li>
 * <li><b>B, the chain</b> - every testimony's own sentence appears, in the order the method found
 * them, which is the order horary is judged in.</li>
 * <li><b>C, a chart that may not be judged</b> - says so, and gives no answer.</li>
 * <li><b>D, the screen is reachable</b> - registered as a card and named in the rail, which is
 * what {@link NavigationCheck} cross-checks.</li>
 * </ul>
 */
public final class HoraryScreenCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** David's chart, the one the other suites use. */
    private static final double LAT = 39.9526;
    private static final double LON = -75.1652;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        part("A: every verdict has its own sentence", HoraryScreenCheck::verdicts);
        part("B: the chain, in the method's order", () -> chain(sw));
        part("C: a chart that may not be judged", () -> notRadical(sw));
        part("D: the screen is reachable", HoraryScreenCheck::reachable);

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

    /**
     * <b>Every verdict says something, and no two say the same thing.</b> The switch has a
     * default, so a verdict added later and not handled would silently read "Undecided" - an
     * answer, and the wrong one. Sweeping the enum is what notices.
     */
    private static void verdicts() {
        List<String> said = new ArrayList<>();
        for (Horary.Verdict v : Horary.Verdict.values()) {
            Horary.Judgement j = new Horary.Judgement();
            j.verdict = v;
            String line = HoraryPanel.verdictLine(j);
            ok(v + " has a sentence", line != null && !line.trim().isEmpty());
            ok(v + "'s sentence is its own, not another's: " + line, !said.contains(line));
            said.add(line);
        }
    }

    /**
     * <b>The reasons appear in the order the method found them.</b> Horary answers on the first
     * decisive testimony, so the order is the argument - a prohibition before a perfection means
     * something a set of the same sentences shuffled does not.
     */
    private static void chain(SwissEph sw) {
        double jd = SweDate.getJulDay(2026, 3, 14, 15.0);
        ChartFrame f = ChartFrame.compute(sw, jd, LAT, LON, 'P', false, 0.0);
        Horary.Judgement j = Horary.judge(sw, f, Horary.Matter.PARTNERS, jd, LAT, LON);
        String html = HoraryPanel.render(j, Horary.Matter.PARTNERS,
            ZonedDateTime.now(ZoneId.of("UTC")), placeNamed("Philadelphia"));

        ok("the rendering names the house of the matter",
            html.contains(String.valueOf(Horary.Matter.PARTNERS.house)));
        ok("and what that house is about", html.contains(Horary.Matter.PARTNERS.about));

        // <b>The chart has to have said something, or the sweep below proves nothing.</b>
        // "every reason was printed" and "in the order it found them" are both vacuously true
        // of an empty chain, so a judge that stopped producing testimony would leave this part
        // green with nothing in it. Asserted rather than assumed: judge always records why,
        // even when the why is that nothing perfects.
        ok("the chart gave at least one reason to print (" + j.chain.size() + ")",
            !j.chain.isEmpty());

        int at = -1;
        boolean inOrder = true;
        int found = 0;
        for (Horary.Testimony t : j.chain) {
            if (t.because == null || t.because.isEmpty()) {
                continue;
            }
            int here = html.indexOf(escaped(t.because));
            ok("the rendering carries: " + t.kind, here >= 0);
            if (here < 0) {
                continue;
            }
            found++;
            inOrder &= here > at;
            at = here;
        }
        ok("every reason the method gave was printed (" + found + " of " + j.chain.size() + ")",
            found == j.chain.size());
        ok("and they are printed in the order it found them", inOrder);
    }

    /**
     * <b>Not fit to judge is an answer, and it is not a yes or a no.</b> The method declines on a
     * chart whose hour ruler does not agree with the Ascendant's, and the screen has to decline
     * with it rather than print the nearest verdict it has.
     */
    private static void notRadical(SwissEph sw) {
        Horary.Judgement j = new Horary.Judgement();
        j.verdict = Horary.Verdict.NOT_RADICAL;
        String line = HoraryPanel.verdictLine(j);
        ok("a chart that may not be judged says so: " + line,
            line.toLowerCase().contains("not fit"));
        ok("and does not answer yes", !line.toLowerCase().startsWith("yes"));
        ok("nor no", !line.toLowerCase().startsWith("no."));
    }

    /**
     * <b>A screen nobody can open is the defect F10 names.</b> The rail's list and the cards the
     * window registers have to agree, which NavigationCheck asserts across every screen; this
     * asserts the horary one is in that list at all.
     */
    private static void reachable() {
        boolean listed = false;
        for (String[] row : SidePanel.SCREENS) {
            if ("HORARY".equals(row[1])) {
                listed = true;
                ok("the rail names it: " + row[0], row[0] != null && !row[0].isEmpty());
                ok("and says what it is", row[2] != null && row[2].length() > 20);
            }
        }
        ok("the horary screen is in the rail's list", listed);
    }

    /** What the panel's own escaping does to a sentence before it reaches the HTML. */
    private static String escaped(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Geocoder.Result placeNamed(String name) {
        Geocoder.Result r = new Geocoder.Result();
        r.name = name;
        r.lat = LAT;
        r.lon = LON;
        return r;
    }

    private static void part(String title, Runnable body) {
        System.out.println("=== Part " + title + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + title.charAt(0)
            + (failures.size() == before ? ": clear" : ": " + (failures.size() - before)
                + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
