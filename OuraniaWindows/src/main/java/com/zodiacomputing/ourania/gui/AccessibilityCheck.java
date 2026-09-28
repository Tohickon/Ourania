package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * What a screen reader is told, and whether the High Contrast template is one (G14).
 *
 * <p><b>Both halves of this row were measured as absent on 28 September</b>, not thin: no
 * {@code getAccessibleContext}, no {@code setAccessibleName}, no accessible description and no
 * high-contrast template anywhere in the tree.
 *
 * <p><b>Part B is the assertion that keeps the first half closed.</b> Naming 221 controls once is
 * a commit; naming the 222nd is the problem, and the only mechanism that survives is deriving the
 * name from the built screen. Part B compares the number of controls the search can see against
 * the number that announce themselves, so a control added later is either named by the same code
 * or is a failure here.
 *
 * <p><b>Part C computes contrast rather than asserting it.</b> "High contrast" is a claim, and a
 * palette is 21 colours somebody chose; the WCAG ratio is arithmetic, so it is done. The two mono
 * templates were the closest thing the app had and Part D measures exactly how far short they
 * fall, because the reason for a new template should be a number and not an opinion.
 */
public final class AccessibilityCheck {

    private AccessibilityCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: the contrast arithmetic is WCAG's", AccessibilityCheck::formula);
        part("B: every control on the settings screen announces itself",
            AccessibilityCheck::named);
        part("C: High Contrast clears AAA against its own ground",
            AccessibilityCheck::highContrast);
        part("D: and it is not a template the app already had",
            AccessibilityCheck::notAlreadyThere);
        part("E: the wheel is named, and naming never makes a screen worse",
            AccessibilityCheck::wheelAndRules);
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

    // ---------------------------------------------------------------- Part A

    /**
     * The three ratios everybody knows, so a wrong formula cannot pass the parts below.
     *
     * <b>A brightness average would pass two of these and fail the third.</b> Mean-of-channels
     * gets black on white right and calls pure blue as bright as pure red, which is the mistake
     * that puts unreadable blue on a dark ground while believing it measured something.
     */
    private static void formula() {
        near("black on white is 21 to 1",
            Accessibility.contrast(Color.BLACK, Color.WHITE), 21.0, 0.01);
        near("a colour against itself is 1 to 1",
            Accessibility.contrast(Color.RED, Color.RED), 1.0, 0.001);
        near("pure red on black is 5.25 to 1",
            Accessibility.contrast(Color.RED, Color.BLACK), 5.25, 0.02);
        near("pure blue on black is 2.44 to 1",
            Accessibility.contrast(Color.BLUE, Color.BLACK), 2.44, 0.02);
        ok("red and blue are not equally bright, which an average would say they are",
            Math.abs(Accessibility.contrast(Color.RED, Color.BLACK)
                - Accessibility.contrast(Color.BLUE, Color.BLACK)) > 2.0);
        ok("the ratio does not care which way round it is asked",
            Math.abs(Accessibility.contrast(Color.BLACK, Color.WHITE)
                - Accessibility.contrast(Color.WHITE, Color.BLACK)) < 1e-9);
    }

    // ---------------------------------------------------------------- Part B

    /**
     * Every control the settings search can find announces a name.
     *
     * <b>Counted against the index rather than against a number.</b> 221 was true on 26 September
     * and is a fact about a screen that grows; what must hold is that the two counts are equal.
     */
    private static void named() throws Exception {
        final SettingsPanel[] panel = new SettingsPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> panel[0] = new SettingsPanel(null));
        javax.swing.JTabbedPane tabs =
            (javax.swing.JTabbedPane) CheckReflect.get(panel[0], "tabs");
        ok("the screen has tabs", tabs != null);
        if (tabs == null) {
            return;
        }

        List<SettingsSearch.Hit> index = SettingsSearch.index(tabs);
        ok("the search can see the controls (" + index.size() + ")", index.size() > 200);

        List<String> anonymous = new ArrayList<>();
        for (SettingsSearch.Hit hit : index) {
            if (Accessibility.nameOf(hit.control).isEmpty()) {
                anonymous.add(hit.name + " on " + hit.tabTitle);
            }
        }
        ok("and every one of them announces a name: " + head(anonymous), anonymous.isEmpty());

        // The description has to say WHERE, because a name alone is ambiguous on a screen with
        // four tabs carrying a control called Sun.
        int described = 0;
        int placed = 0;
        for (SettingsSearch.Hit hit : index) {
            String d = Accessibility.descriptionOf(hit.control);
            if (!d.isEmpty()) {
                described++;
                if (hit.tabTitle != null && d.contains(hit.tabTitle)) {
                    placed++;
                }
            }
        }
        ok("every control carries a description too (" + described + " of " + index.size() + ")",
            described == index.size());
        ok("and each one says which tab it is on (" + placed + ")", placed == index.size());

        // <b>A name Swing already had is not replaced.</b> A button reading Reset announces
        // Reset, and deriving a name over the top would make the screen worse.
        int fromOwnText = 0;
        for (SettingsSearch.Hit hit : index) {
            if (hit.control instanceof javax.swing.AbstractButton) {
                String own = ((javax.swing.AbstractButton) hit.control).getText();
                if (own != null && !own.trim().isEmpty()
                        && own.trim().equals(Accessibility.nameOf(hit.control))) {
                    fromOwnText++;
                }
            }
        }
        ok("buttons that already said their own name kept it (" + fromOwnText + ")",
            fromOwnText > 0);
    }

    // ---------------------------------------------------------------- Part C

    /** Every colour in the template, against the ground the template draws on. */
    private static void highContrast() {
        String ground = ChartPalette.groundOf(ChartPalette.HIGH_CONTRAST);
        ok("the template exists and has a ground (" + ground + ")",
            ground != null && !ground.isEmpty());
        Color bg = Color.decode(ground);

        java.util.Map<String, String> colours =
            ChartPalette.everyColourOf(ChartPalette.HIGH_CONTRAST);
        ok("it defines the whole palette (" + colours.size() + ")", colours.size() >= 20);

        double worst = 99.0;
        String worstName = "";
        for (java.util.Map.Entry<String, String> e : colours.entrySet()) {
            if ("wheel".equals(e.getKey())) {
                continue;               // the ground is what the rest is measured against
            }
            double r = Accessibility.contrast(Color.decode(e.getValue()), bg);
            if (r < worst) {
                worst = r;
                worstName = e.getKey();
            }
        }
        ok(String.format("every colour clears AAA against the ground (worst: %s at %.2f to 1)",
            worstName, worst), worst >= Accessibility.AAA);

        // <b>The five Ptolemaic aspects have to be tellable apart from each other</b>, or a
        // palette of fifteen near-white lines would pass the ratio above and be unreadable. The
        // minors deliberately reuse these hues; the dash pattern separates them.
        String[] major = {"Conjunction", "Sextile", "Square", "Trine", "Opposition"};
        double closest = 999.0;
        String pair = "";
        for (int i = 0; i < major.length; i++) {
            for (int j = i + 1; j < major.length; j++) {
                double d = distance(Color.decode(colours.get(major[i])),
                    Color.decode(colours.get(major[j])));
                if (d < closest) {
                    closest = d;
                    pair = major[i] + "/" + major[j];
                }
            }
        }
        ok(String.format("the five Ptolemaic aspects are distinguishable (closest: %s at %.0f)",
            pair, closest), closest > 120.0);

        // And the four elements, which sit beside each other on the same wheel.
        double elementClosest = 999.0;
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                elementClosest = Math.min(elementClosest,
                    distance(Color.decode(colours.get("element " + i)),
                        Color.decode(colours.get("element " + j))));
            }
        }
        ok(String.format("and so are the four elements (%.0f)", elementClosest),
            elementClosest > 120.0);

        ok("the template is on the chooser's list",
            java.util.Arrays.asList(ChartPalette.TEMPLATES)
                .contains(ChartPalette.HIGH_CONTRAST));
    }

    // ---------------------------------------------------------------- Part D

    /**
     * The two mono templates were the nearest thing and neither qualifies.
     *
     * <b>The negative half of the row, and it is the half that says why the work was needed.</b>
     * Without it "High Contrast clears AAA" is satisfied by renaming an existing template.
     */
    private static void notAlreadyThere() {
        for (String template : ChartPalette.TEMPLATES) {
            if (ChartPalette.HIGH_CONTRAST.equals(template)) {
                continue;
            }
            Color bg = Color.decode(ChartPalette.groundOf(template));
            double worst = 99.0;
            String worstName = "";
            for (java.util.Map.Entry<String, String> e
                    : ChartPalette.everyColourOf(template).entrySet()) {
                if ("wheel".equals(e.getKey())) {
                    continue;
                }
                double r = Accessibility.contrast(Color.decode(e.getValue()), bg);
                if (r < worst) {
                    worst = r;
                    worstName = e.getKey();
                }
            }
            ok(String.format("%s does not clear AAA (%s at %.2f to 1)", template, worstName,
                worst), worst < Accessibility.AAA);
        }
    }

    // ---------------------------------------------------------------- Part E

    /**
     * The chart wheel is named, and {@link Accessibility#name} cannot damage a component.
     *
     * <b>The wheel is asserted by a source sweep rather than by building one.</b> A SkymapPanel
     * costs an ephemeris and a window, four suites already pay for that, and what is being checked
     * here is that the call exists at the one place that starts the app - which is a fact about
     * the source. The rules below are checked against a real component, because those are facts
     * about behaviour.
     */
    private static void wheelAndRules() throws Exception {
        String code = JavaSource.withoutComments(new String(java.nio.file.Files.readAllBytes(
            new java.io.File("src/main/java/com/zodiacomputing/ourania/gui/OuraniaWindow.java")
                .toPath()), java.nio.charset.StandardCharsets.UTF_8));
        ok("the window names the chart wheel",
            code.contains("Accessibility.name(skymapPanel"));
        ok("and calls it something a reader would recognise", code.contains("\"Chart wheel\""));

        // <b>A name a component already had is kept.</b> This is the rule that stops an
        // accessibility pass making a screen worse than it was, and it is asserted rather than
        // trusted because the obvious implementation overwrites.
        javax.swing.JButton already = new javax.swing.JButton("Reset");
        Accessibility.name(already, "Something else", "a description");
        ok("a button that already announces itself keeps its own name ("
            + Accessibility.nameOf(already) + ")",
            "Reset".equals(Accessibility.nameOf(already)));

        javax.swing.JSpinner anonymous = new javax.swing.JSpinner();
        ok("a spinner announces nothing to begin with",
            Accessibility.nameOf(anonymous).isEmpty());
        Accessibility.name(anonymous, "Sun", "Sun, orb width, Bodies & Points");
        ok("and takes the name it is given", "Sun".equals(Accessibility.nameOf(anonymous)));
        ok("with the description", Accessibility.descriptionOf(anonymous).contains("Bodies"));

        // Never throws, for ErrorLog's reason: every caller is building a screen.
        Accessibility.name(null, "x", "y");
        Accessibility.name(anonymous, null, null);
        Accessibility.name(anonymous, "   ", "");
        ok("naming survives null and blank input", true);
        ok("and a blank name does not replace a real one",
            "Sun".equals(Accessibility.nameOf(anonymous)));
    }

    // ---------------------------------------------------------------- plumbing

    /** Straight-line distance in RGB. Crude, and enough to catch two near-identical colours. */
    private static double distance(Color a, Color b) {
        double dr = a.getRed() - b.getRed();
        double dg = a.getGreen() - b.getGreen();
        double db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private static String head(List<String> list) {
        return list.isEmpty() ? "none"
            : list.size() + ", first: " + list.subList(0, Math.min(5, list.size()));
    }

    private static void near(String what, double got, double want, double slack) {
        ok(String.format("%s (%.3f)", what, got), Math.abs(got - want) <= slack);
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
