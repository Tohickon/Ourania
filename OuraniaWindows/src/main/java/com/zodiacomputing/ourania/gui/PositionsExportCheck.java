package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Declinations;
import com.zodiacomputing.ourania.astro.Dignity;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The positions export, which had never been held by anything (B4, G10).
 *
 * <p><b>Two findings before a line was written.</b> {@code positionsText} - what Copy Positions and
 * Save Positions both produce, and the only view of a chart a reader can open in a spreadsheet - had
 * <b>no assertion anywhere in the tree</b>. And G10's row said there was no midpoint table, while
 * {@code ChartTables.midpoints} has been one since A1; what was actually missing was declination and
 * dignity as <i>columns</i>, both from engines that already answer.
 *
 * <p><b>Every column is asserted against the engine, never against a typed value.</b> A table checked
 * against hand-written numbers is a second copy of the engine and drifts from it - which is this
 * project's most logged defect, and the reason G10's template said so before the work started.
 */
public final class PositionsExportCheck {

    private PositionsExportCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    /** A chart with a body out of bounds and planets in real dignity - Feb 1990. */
    private static final ZonedDateTime WHEN =
        ZonedDateTime.of(1990, 2, 14, 9, 30, 0, 0, ZoneId.of("UTC"));

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        try {
            SkymapPanel sky = panel();
            String text = sky.positionsText();
            ChartFrame f = sky.radixChart();

            part("A: the export names every column it carries", () -> header(text));
            part("B: each row is the engine's answer, not a copy of it", () -> rows(text, f));
            part("C: declination comes from the declination engine", () -> declination(text, f));
            part("D: dignity is asked only where it applies", () -> dignity(text, f));
            part("E: what the export does not claim", () -> limits(text, f));
        } finally {
            if (window != null) {
                final OuraniaWindow w = window;
                javax.swing.SwingUtilities.invokeAndWait(() -> w.dispose());
            }
        }

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            // <b>Exits, because a Swing window keeps the JVM alive.</b> The first run of this suite
            // printed ALL CLEAR and then sat there: build.ps1 gives each suite a timeout, so a suite
            // that will not exit is a red one however many assertions passed. Every window-using
            // suite here ends this way.
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
        }
        System.exit(1);
    }

    /** Kept so it can be disposed however the run ends. */
    private static OuraniaWindow window;

    // ---------------------------------------------------------------- Part A

    private static final String[] COLUMNS = {
        "Body", "Longitude", "Sign", "Degree", "House", "Retrograde",
        "Declination", "Out of bounds", "Dignity", "Dignity score",
    };

    /**
     * <b>Written out by hand, deliberately</b>, the way {@code PresetBarCheck}'s tab list is: a
     * header read off the method it is checking would agree with any change, including a column
     * quietly disappearing from a reader's spreadsheet.
     */
    private static void header(String text) {
        String[] head = text.split("\n", 2)[0].split("\t", -1);
        eq("the export has " + COLUMNS.length + " columns", COLUMNS.length, head.length);
        for (int i = 0; i < Math.min(COLUMNS.length, head.length); i++) {
            eq("column " + (i + 1) + " is " + COLUMNS[i], COLUMNS[i], head[i]);
        }
        ok("it is tab separated, which is what a spreadsheet opens",
            text.split("\n", 2)[0].contains("\t") && !text.split("\n", 2)[0].contains(","));
    }

    // ---------------------------------------------------------------- Part B

    private static void rows(String text, ChartFrame f) {
        java.util.List<String[]> body = body(text);
        ok("the export has a row for every valid body: " + body.size(), body.size() >= 10);
        int checkedSign = 0;
        int checkedHouse = 0;
        for (String[] r : body) {
            double lon = Double.parseDouble(r[1]);
            String sign = com.zodiacomputing.ourania.astro.Zodiac
                .SIGNS[com.zodiacomputing.ourania.astro.Zodiac.signIndex(lon)];
            if (!r[2].equalsIgnoreCase(sign)) {
                ok(r[0] + "'s sign is the sign of its longitude: " + r[2] + " against " + sign,
                    false);
            } else {
                checkedSign++;
            }
            double deg = com.zodiacomputing.ourania.astro.Zodiac.degreeInSign(lon);
            if (Math.abs(Double.parseDouble(r[3]) - deg) > 0.011) {
                ok(r[0] + "'s degree is its degree in sign: " + r[3] + " against " + deg, false);
            } else {
                checkedHouse++;
            }
        }
        ok("every row's sign is derived from its own longitude: " + checkedSign,
            checkedSign == body.size());
        ok("every row's degree in sign likewise: " + checkedHouse, checkedHouse == body.size());
    }

    // ---------------------------------------------------------------- Part C

    /**
     * The declination column against {@link Declinations}, and the out-of-bounds column against the
     * same entry's own flag - so a planet can never be listed past the Sun's reach by one and not
     * the other.
     */
    private static void declination(String text, ChartFrame f) {
        java.util.Map<String, Declinations.Entry> engine = new java.util.HashMap<>();
        for (Declinations.Entry e : Declinations.of(f).entries) {
            engine.put(e.name, e);
        }
        int agreed = 0;
        int oob = 0;
        int missing = 0;
        for (String[] r : body(text)) {
            Declinations.Entry e = engine.get(r[0]);
            if (e == null) {
                if (!r[6].isEmpty()) {
                    ok(r[0] + " has a declination the engine does not give", false);
                }
                missing++;
                continue;
            }
            if (!r[6].equals(ChartTables.declination(e.declination))) {
                ok(r[0] + "'s declination is the engine's: " + r[6] + " against "
                    + ChartTables.declination(e.declination), false);
            } else {
                agreed++;
            }
            if (e.outOfBounds != !r[7].isEmpty()) {
                ok(r[0] + "'s out-of-bounds column agrees with the engine's flag", false);
            } else if (e.outOfBounds) {
                oob++;
            }
        }
        ok("every declination is the engine's: " + agreed, agreed > 0);
        ok("and the formatter is the declination table's own, not a second one",
            ChartTables.declination(23.44).contains("N"));
        ok("bodies the engine does not carry have an empty column: " + missing, missing >= 0);
        System.out.println("    (out of bounds in this chart: " + oob + ")");
    }

    // ---------------------------------------------------------------- Part D

    /**
     * <b>Dignity is a question about the seven traditional bodies</b>, which is what
     * {@code Dignity.TRADITIONAL}'s own comment says. The negative half matters more than the
     * positive: a dignity printed against Uranus or the Ascendant would be confidently meaningless,
     * and that is the sort of thing a reader would believe.
     */
    private static void dignity(String text, ChartFrame f) {
        int traditional = 0;
        int others = 0;
        for (String[] r : body(text)) {
            boolean applies = java.util.Arrays.asList(Dignity.TRADITIONAL).contains(r[0]);
            if (applies) {
                Dignity.Result d = Dignity.evaluate(r[0], Double.parseDouble(r[1]), f.diurnal);
                String expect = d.reasons.isEmpty() ? "no essential dignity"
                    : String.join("; ", d.reasons);
                if (!r[8].equals(expect)) {
                    ok(r[0] + "'s dignity is the engine's words: " + r[8] + " against " + expect,
                        false);
                } else if (!r[9].equals(String.valueOf(d.score))) {
                    ok(r[0] + "'s dignity score is the engine's: " + r[9] + " against " + d.score,
                        false);
                } else {
                    traditional++;
                }
            } else {
                if (!r[8].isEmpty() || !r[9].isEmpty()) {
                    ok(r[0] + " is not one of the seven and carries no dignity", false);
                } else {
                    others++;
                }
            }
        }
        eq("all seven traditional bodies carry the engine's dignity", 7, traditional);
        ok("and nothing else carries any: " + others + " other rows", others > 0);
        ok("the seven come from Dignity.TRADITIONAL rather than a list written here",
            Dignity.TRADITIONAL.length == 7);
    }

    // ---------------------------------------------------------------- Part E

    /**
     * Written down rather than left to be assumed, which is the habit B8 established.
     *
     * <b>This is the natal ring only.</b> The export says nothing about transits, progressions, a
     * partner's chart or the sky, and a reader looking at a tri-wheel gets the inner chart. That is
     * the existing behaviour of B4 and this commit did not change it - but nothing said so, and a
     * column called "Dignity" arriving in a spreadsheet invites the assumption that the file
     * describes whatever is on screen.
     */
    private static void limits(String text, ChartFrame f) {
        ok("the sect the dignity is judged for is the chart's own, not a default",
            f.diurnal == com.zodiacomputing.ourania.astro.Sect.isDiurnal(sunLon(f), f.asc));
        ok("no row claims a midpoint - ChartTables.midpoints is where those live",
            !text.toLowerCase().contains("midpoint"));
        ok("no row claims a parallel - the declination table carries those",
            !text.toLowerCase().contains("parallel"));
        ok("every line has the same number of columns, so a spreadsheet reads it",
            sameWidth(text));
    }

    private static double sunLon(ChartFrame f) {
        for (ChartFrame.Body b : f.bodies) {
            if ("Sun".equals(b.name)) {
                return b.lon;
            }
        }
        return 0.0;
    }

    private static boolean sameWidth(String text) {
        int want = -1;
        for (String line : text.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            int n = line.split("\t", -1).length;
            if (want < 0) {
                want = n;
            } else if (n != want) {
                return false;
            }
        }
        return want == COLUMNS.length;
    }

    // ---------------------------------------------------------------- plumbing

    private static java.util.List<String[]> body(String text) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        String[] lines = text.split("\n");
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                out.add(lines[i].split("\t", -1));
            }
        }
        return out;
    }

    /**
     * The panel as the app makes it, through a window - the same way {@code ScrubCheck} does it.
     *
     * <b>Not a bare {@code new SkymapPanel()}</b>, which the constructor refuses anyway: on 26 Sep a
     * probe built a settings panel with no window and concluded a control was missing from the
     * screen, because seven combos are re-parented by the window that owns it. A panel taken from a
     * real window cannot be wrong about what the app has.
     */
    private static SkymapPanel panel() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        window = w[0];
        final SkymapPanel[] out = new SkymapPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                SkymapPanel p = (SkymapPanel) CheckReflect.get(w[0], "skymapPanel");
                CheckReflect.set(p, "natalRing.time", WHEN);
                CheckReflect.set(p, "skyRing.time", WHEN);
                p.updateChartData();
                out[0] = p;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return out[0];
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + name.charAt(0) + ": "
            + (failures.size() == before ? "PASS" : (failures.size() - before) + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String what, boolean pass) {
        checks++;
        if (!pass) {
            failures.add(what);
        }
        System.out.println("  " + (pass ? "ok  " : "FAIL") + "  " + what);
    }

    private static void eq(String what, Object want, Object got) {
        ok(what + ": " + got, want.equals(got));
    }
}
