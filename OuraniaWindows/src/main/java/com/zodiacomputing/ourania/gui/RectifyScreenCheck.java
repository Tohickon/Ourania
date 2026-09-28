package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Rectify;

import java.util.ArrayList;
import java.util.List;

/**
 * What the rectification screen makes of what is typed into it (G8).
 *
 * <p><b>The engine is checked in {@code RectifyCheck}; this is the reading of the box.</b> The two
 * questions that matter here are whether an ambiguous date gets through - the screen reuses the
 * import's rule, so it must refuse the same things - and whether the screen can say "I have not
 * decided", which is the one sentence it exists to be able to say.
 */
public final class RectifyScreenCheck {

    private RectifyScreenCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        Settings.useScratchFile();
        part("A: the events box", RectifyScreenCheck::parsing);
        part("B: what it says when it has not decided", RectifyScreenCheck::saying);
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

    private static void parsing() {
        List<String> bad = new ArrayList<>();
        List<Rectify.Event> got = RectifyPanel.parse(
            "1998-04-12  Saturn  left home\n"
            + "2004-09-01  Venus   married\n"
            + "\n"
            + "# a comment\n"
            + "2011-02-20  a move\n", bad);
        ok("three events read (" + got.size() + ")", got.size() == 3);
        ok("blank lines and comments are not events: " + bad, bad.isEmpty());
        ok("a named point is taken", "Saturn".equals(got.get(0).body));
        ok("and the rest of the line is what it is called",
            "left home".equals(got.get(0).what));
        ok("a line with no point names none", got.get(2).body.isEmpty());
        ok("and keeps its description", "a move".equals(got.get(2).what));

        // <b>The same refusal the import makes, because it is the same rule.</b> A birth time
        // rectified against dates read the wrong way round is worse than no rectification.
        List<String> problems = new ArrayList<>();
        List<Rectify.Event> ambiguous = RectifyPanel.parse("03/04/1985 married", problems);
        ok("an ambiguous date is refused here too", ambiguous.isEmpty());
        ok("and the line is named: " + problems,
            problems.size() == 1 && problems.get(0).contains("Line 1"));
        ok("with what to write instead", problems.get(0).contains("1998-04-12"));

        ok("an unambiguous slash date is taken",
            RectifyPanel.parse("25/04/1985 married", new ArrayList<>()).size() == 1);
        ok("nothing typed is no events",
            RectifyPanel.parse("", new ArrayList<>()).isEmpty());
        ok("null is no events", RectifyPanel.parse(null, new ArrayList<>()).isEmpty());
    }

    private static void saying() {
        // Too few events: the screen must say how many it needs rather than rank two things.
        String few = RectifyPanel.render(null, new ArrayList<>(), 2);
        ok("with too few events it says so", few.contains("at least"));
        ok("and gives the number", few.contains(String.valueOf(Rectify.minimumEvents)));
        ok("and does not print a time", !few.contains("<h2>"));

        // A result that is not confident must say the ranking is not an answer.
        Rectify.Result unsure = new Rectify.Result();
        Rectify.Candidate c = new Rectify.Candidate();
        c.hour = 7.55;
        c.score = 1.0;
        c.reasons.add("something: directed Midheaven conjunct Sun (0.10°)");
        unsure.candidates.add(c);
        unsure.events = 3;
        unsure.separation = 0.05;
        unsure.confident = false;
        String hedged = RectifyPanel.render(unsure, new ArrayList<>(), 3);
        ok("an unconfident result prints the best time", hedged.contains("07:33"));
        ok("and says plainly that it is a ranking, not an answer",
            hedged.contains("not an answer"));
        ok("and tells the reader what to do", hedged.contains("more dated events"));
        ok("and still shows the reasoning", hedged.contains("Midheaven conjunct Sun"));

        unsure.confident = true;
        unsure.separation = 0.9;
        String sure = RectifyPanel.render(unsure, new ArrayList<>(), 3);
        ok("a confident result does not carry the caveat", !sure.contains("not an answer"));
        ok("and says what it weighed", sure.contains("3 events"));
        ok("both report the separation as a number", hedged.contains("Separation")
            && sure.contains("Separation"));

        ok("a problem typed into the box reaches the screen",
            RectifyPanel.render(null, java.util.Arrays.asList("Line 4: not a date"), 0)
                .contains("Line 4"));
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
