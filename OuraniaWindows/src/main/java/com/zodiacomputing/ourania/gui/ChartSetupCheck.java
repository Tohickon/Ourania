package com.zodiacomputing.ourania.gui;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The chart setup drawer's state machine: subject, engine, and the three controls that have to
 * agree with them.
 *
 * <p><b>Why this suite exists.</b> The panel used to carry a flat five-entry {@code ChartMode}
 * combo. It is now a two-step chooser - subject, then engine - and the mode can be set from
 * three places: a step-2 button, a step-1 button picking a default, and {@code loadTransit}.
 * <b>Every one of those has to leave the transit fields, the transits checkbox, the
 * reference-place field and the second column's title describing the same chart the engine will
 * actually build.</b> Nothing about a mismatch looks wrong on screen - the panel simply says
 * one thing while {@code applyChartSettings} sends another, which is the Step-dropdown defect
 * this panel has shipped once already and the reason {@code setMode} is the only writer.
 *
 * <p>Part C is the one that would otherwise rot: the reference place applies to a midpoint
 * composite and nothing else, so a Davison must never offer it.
 */
public final class ChartSetupCheck {

    private static final List<String> failures = new ArrayList<String>();
    private static int checks = 0;

    private static ChartSetupPanel panel;
    private static Class<?> subjectType;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                panel = new ChartSetupPanel(null);
                subjectType = Class.forName(
                    "com.zodiacomputing.ourania.gui.ChartSetupPanel$Subject");
            } catch (Exception e) {
                failures.add("the panel would not construct: " + e);
            }
        });
        if (panel == null) {
            System.out.println("FAILURES (1 of 1 checks):");
            System.out.println("  " + failures.get(0));
            System.exit(1);
        }

        System.out.println("=== Part A: a subject only offers engines it can use ===");
        int before = failures.size();
        subjectsAndEngines();
        report("Part A", before);

        System.out.println("=== Part B: the transits box says what the mode means ===");
        before = failures.size();
        transitsBox();
        report("Part B", before);

        System.out.println("=== Part C: the reference place is a midpoint composite's alone ===");
        before = failures.size();
        referencePlace();
        report("Part C", before);

        System.out.println("=== Part D: the second column is named for what it holds ===");
        before = failures.size();
        columnTitle();
        report("Part D", before);

        System.out.println("=== Part E: every engine explains itself ===");
        before = failures.size();
        theHoverText();
        report("Part E", before);

        System.out.println();
        System.out.println("=== Part F: a saved person in Chart B is never a transit ===");
        before = failures.size();
        theSecondSlot();
        report("Part F", before);

        System.out.println();
        System.out.println("=== Part G: loading a chart fills the form, nothing more ===");
        before = failures.size();
        loadingFillsOnly();
        report("Part G", before);

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

        // The panel is Swing, so the AWT event thread is up and non-daemon: main returning
        // would not end the JVM, and handover-stamp.sh captures each suite with $(...), which
        // blocks until the process closes stdout. CompositeCheck hung that script for 67
        // minutes on 2026-08-25 for exactly this.
        System.exit(0);
    }

    /** Step 1 picks a branch; step 2 must never offer an engine from another one. */
    private static void subjectsAndEngines() throws Exception {
        setSubject("MYSELF");
        ok("Myself lands on a one-person mode, got " + mode(),
            mode() == ChartMode.SINGLE || mode() == ChartMode.TRANSIT);

        setSubject("PARTNERSHIP");
        ok("Us / Partnership lands on a two-person mode, got " + mode(),
            mode() == ChartMode.SYNASTRY || mode() == ChartMode.COMPOSITE_MIDPOINT
                || mode() == ChartMode.COMPOSITE_DAVISON);

        // <b>Birth data survives a visit to The Sky Now.</b> That subject fills Chart A with
        // the present moment, and Chart A is the one field in this panel nobody can retype
        // from memory - so it is stashed on the way in and restored on the way out. Caught by
        // rendering the panel rather than by reading it: the fields visibly said today.
        setSubject("MYSELF");
        ((JTextField) field("baseDateField")).setText("1982-08-10");
        ((JTextField) field("baseTimeField")).setText("19:01");
        ((JTextField) field("baseLocationField")).setText("Philadelphia, USA");

        setSubject("SKY_NOW");
        eq("The Sky Now is a single chart", ChartMode.SINGLE, mode());
        String today = java.time.ZonedDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        eq("The Sky Now filled Chart A with today", today, text("baseDateField"));
        eq("The Sky Now filled Chart A with the home location",
            field("homeLocation"), text("baseLocationField"));

        setSubject("MYSELF");
        eq("leaving The Sky Now gave the birth date back", "1982-08-10", text("baseDateField"));
        eq("leaving The Sky Now gave the birth time back", "19:01", text("baseTimeField"));
        eq("leaving The Sky Now gave the birthplace back", "Philadelphia, USA",
            text("baseLocationField"));

        // And the other half: a Sky Now chart must not be written to settings as the user's
        // natal chart, or the next launch opens on today.
        setSubject("SKY_NOW");
        ok("a Sky Now generate does not persist Chart A as natal data",
            !persistsNatal());
        setSubject("MYSELF");
        ok("an ordinary generate does persist Chart A as natal data", persistsNatal());

        // <b>Returning to a subject keeps the engine last used there.</b> Without this,
        // flipping to Myself to check a birth time and back would silently turn a Davison into
        // a synastry, and the only sign would be a different chart on Generate.
        setSubject("PARTNERSHIP");
        setMode(ChartMode.COMPOSITE_DAVISON);
        setSubject("MYSELF");
        setSubject("PARTNERSHIP");
        eq("coming back to Partnership kept Davison", ChartMode.COMPOSITE_DAVISON, mode());

        setSubject("MYSELF");
        setMode(ChartMode.TRANSIT);
        setSubject("PARTNERSHIP");
        setSubject("MYSELF");
        eq("coming back to Myself kept Transits", ChartMode.TRANSIT, mode());

        // loadTransit sets the mode from outside the chooser. It must go through the same
        // path, or step 1 keeps highlighting a subject that cannot reach the mode shown.
        setSubject("PARTNERSHIP");
        setMode(ChartMode.SYNASTRY);
        invoke("setSubject", subjectType, subjectConst("MYSELF"));
        setMode(ChartMode.TRANSIT);
        eq("a load into the transit wheel leaves Myself selected",
            "MYSELF", subject().toString());
    }

    /** The four contracts named in syncTransitsCheck's javadoc, asserted rather than described. */
    private static void transitsBox() throws Exception {
        JCheckBox box = (JCheckBox) field("transitsCheck");

        setMode(ChartMode.SINGLE);
        ok("Single: transits off", !box.isSelected());
        ok("Single: transits disabled - there is no outer wheel", !box.isEnabled());
        ok("Single: the transit date field is disabled",
            !((JTextField) field("transitDateField")).isEnabled());

        setMode(ChartMode.TRANSIT);
        ok("Transit: transits on - it is what the mode means", box.isSelected());
        ok("Transit: transits disabled - unticking would leave a single chart",
            !box.isEnabled());
        ok("Transit: the transit fields are enabled",
            ((JTextField) field("transitDateField")).isEnabled());

        for (ChartMode m : new ChartMode[] {ChartMode.COMPOSITE_MIDPOINT,
                                            ChartMode.COMPOSITE_DAVISON}) {
            setMode(m);
            ok(m + ": transits enabled - a composite takes a sky ring or stands alone",
                box.isEnabled());
        }

        setMode(ChartMode.SYNASTRY);
        ok("Synastry: transits enabled - the tri-wheel draws A, B and the sky",
            box.isEnabled());

        // The panel and the engine must agree about whether an outer wheel exists at all.
        for (ChartMode m : ChartMode.values()) {
            setMode(m);
            boolean panelSays = box.isSelected();
            ok(m + ": the panel and outerWheelShown agree about the outer wheel",
                SkymapPanel.outerWheelShown(m, panelSays)
                    == SkymapPanel.outerWheelShown(m, panelSays));
            ok(m + ": the transit fields are enabled exactly when the mode is not SINGLE",
                ((JTextField) field("transitDateField")).isEnabled()
                    == (m != ChartMode.SINGLE));
        }
    }

    /**
     * A Davison is cast for a real place and takes its houses from there, so the reference
     * field must be dead in that mode. Enabling it would be a control that changes nothing -
     * the defect syncTransitsCheck's own comment records this panel having shipped before.
     */
    private static void referencePlace() throws Exception {
        JTextField ref = (JTextField) field("compositeRefField");
        for (ChartMode m : ChartMode.values()) {
            setMode(m);
            boolean shouldApply = m == ChartMode.COMPOSITE_MIDPOINT;
            ok(m + ": the reference place is enabled only for a midpoint composite",
                ref.isEnabled() == shouldApply);
            if (!shouldApply) {
                ok(m + ": a disabled reference place says why", ref.getToolTipText() != null
                    && !ref.getToolTipText().trim().isEmpty());
            }
        }
        setMode(ChartMode.COMPOSITE_DAVISON);
        ok("the Davison tooltip explains that its houses come from the place itself",
            ref.getToolTipText() != null
                && ref.getToolTipText().toLowerCase().contains("davison"));
    }

    /**
     * The second column is Chart B in every mode, and the sky has a column of its own.
     *
     * <b>This used to pin the opposite, and the opposite was the defect.</b> The heading
     * renamed itself between "Chart B (Partner)" and "Chart B (Transit)" because one row of
     * fields carried two different things - the second person in a synastry, and this moment
     * everywhere else. The ring chips switched which of those the row meant without switching
     * the value in it, so pressing Sky after setting up a partner drew that partner's birth
     * chart where the current sky belonged, and pressing Partner after looking at the sky drew
     * the sky where the partner belonged. A heading that renames itself is a row admitting it
     * is two rows.
     *
     * The sky has its own column now, so this one is Chart B always - and the check that used
     * to guarantee the renaming now guarantees it cannot come back.
     */
    private static void columnTitle() throws Exception {
        JLabel title = (JLabel) field("transitTitle");
        for (ChartMode m : ChartMode.values()) {
            setMode(m);
            eq(m + ": the second column's name", "Chart B", title.getText());
        }
        ok("the sky has a date field of its own", field("skyDateField") != null);
        ok("the sky has a time field of its own", field("skyTimeField") != null);
        ok("the sky has a location field of its own", field("skyLocationField") != null);
        // Nothing the sky needs may be a Chart B field, which is the whole point.
        for (String sky : new String[] {"skyDateField", "skyTimeField", "skyLocationField"}) {
            for (String b : new String[] {"transitDateField", "transitTimeField",
                    "transitLocationField"}) {
                ok(sky + " is not " + b, field(sky) != field(b));
            }
        }
    }

    /**
     * Every engine button carries hover text, and the two composites carry instructions.
     *
     * <b>The composites are the pair a reader cannot choose between from the name.</b> Picking
     * the wrong one produces a chart that looks entirely reasonable and answers a different
     * question, so "has a tooltip" is not enough - it has to tell them what to do.
     */
    @SuppressWarnings("unchecked")
    private static void theHoverText() throws Exception {
        Map<ChartMode, JButton> engines = (Map<ChartMode, JButton>) field("engineButtons");
        eq("every mode has an engine button", Integer.valueOf(ChartMode.values().length),
            Integer.valueOf(engines.size()));

        for (ChartMode m : ChartMode.values()) {
            JButton b = engines.get(m);
            ok(m + ": has an engine button", b != null);
            if (b == null) {
                continue;
            }
            String tip = b.getToolTipText();
            ok(m + ": the button has hover text", tip != null && tip.length() > 40);
            ok(m + ": the hover text tells the reader what to do",
                tip != null && tip.contains("To use it"));
        }

        String mid = engines.get(ChartMode.COMPOSITE_MIDPOINT).getToolTipText();
        String dav = engines.get(ChartMode.COMPOSITE_DAVISON).getToolTipText();

        ok("the midpoint tooltip says it is not a real moment",
            mid.contains("not a real moment"));
        ok("the midpoint tooltip names the reference place",
            mid.contains("Composite houses for"));
        ok("the midpoint tooltip warns about the unstable midpoint",
            mid.contains("UNSTABLE"));
        ok("the Davison tooltip says it IS a real chart", dav.contains("real chart"));
        ok("the Davison tooltip says the reference place does not apply",
            dav.contains("greyed"));
        ok("the two tooltips are not the same text", !mid.equals(dav));

        // The two must not be described as variants of one another - they answer different
        // questions and ChartFrame's own note says they are never blended.
        ok("the Davison tooltip distinguishes it from the midpoint",
            dav.contains("midpoint composite averages"));
    }

    // ---- reflection helpers --------------------------------------------------------

    private static ChartMode mode() throws Exception {
        return (ChartMode) field("selectedMode");
    }

    private static Object subject() throws Exception {
        return field("subject");
    }

    private static void setMode(ChartMode m) throws Exception {
        invoke("setMode", ChartMode.class, m);
    }

    private static void setSubject(String name) throws Exception {
        invoke("setSubject", subjectType, subjectConst(name));
    }

    private static Object subjectConst(String name) {
        for (Object c : subjectType.getEnumConstants()) {
            if (c.toString().equals(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("no such subject: " + name);
    }

    private static void invoke(String method, Class<?> argType, Object arg) throws Exception {
        Method m = ChartSetupPanel.class.getDeclaredMethod(method, argType);
        m.setAccessible(true);
        m.invoke(panel, arg);
    }

    private static Object field(String name) throws Exception {
        Field f = ChartSetupPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(panel);
    }

    private static String text(String fieldName) throws Exception {
        return ((JTextField) field(fieldName)).getText();
    }

    /**
     * Asks the panel the same question {@code generateChart} asks before writing birth data.
     *
     * Reached through the named method rather than by re-stating the condition here, so that
     * changing the rule in one place and not the other is a failure rather than a silent
     * divergence - which is the whole reason it is a method and not an inline test.
     */
    private static boolean persistsNatal() throws Exception {
        Method m = ChartSetupPanel.class.getDeclaredMethod("savesNatalData");
        m.setAccessible(true);
        return ((Boolean) m.invoke(panel)).booleanValue();
    }

    /**
     * Loading a saved profile into Chart B means a relationship, from any starting mode.
     *
     * <b>It only switched from SINGLE.</b> Starting in Natal &amp; Transit and loading someone
     * into Chart B left the mode alone, so their birth data went into the transit fields and
     * the app drew their nativity as the sky over Chart A. That is a different claim from
     * synastry and it looked like a working chart, which is what made it worth pinning: the
     * failure had no symptom except being wrong.
     *
     * A composite is left alone - it is already a reading of two people.
     */
    /**
     * Choosing a saved chart fills the form. Drawing it is a separate act.
     *
     * <b>It used to generate immediately.</b> Picking a chart from the directory threw the
     * reader straight out of Chart Setup and onto the wheel, so loading Chart A and then
     * Chart B meant navigating back in between - and there was no moment in which to correct
     * a birth time, set a relocation, or mark the time unknown before the chart was drawn.
     *
     * Also pins that both doors into this form agree. The Load button restored the date, time
     * and place but not the rating, so the same chart opened as "no time" from the directory
     * and as an ordinary A-rated chart from Load, casting angles it never had. Two surfaces,
     * two answers, and here the answers are different charts rather than different labels.
     */
    private static void loadingFillsOnly() throws Exception {
        final boolean[] drew = {false};
        final String[] went = {null};
        class Recorder extends OuraniaWindow {
            @Override
            public void switchScreen(String screen) {
                went[0] = screen;
            }

            @Override
            public void applyChartSettings(String bDate, String bTime, String bLoc,
                                           ChartMode mode, String tDate, String tTime,
                                           String tLoc, boolean transits, boolean baseUnknown,
                                           String zoneOverride, String relocate) {
                drew[0] = true;
            }
        }
        final Recorder rec = new Recorder();
        final ChartSetupPanel[] cp = new ChartSetupPanel[1];
        SwingUtilities.invokeAndWait(() -> cp[0] = new ChartSetupPanel(rec));

        java.io.File live = new java.io.File("saved_charts.properties");
        java.io.File kept = null;
        if (live.exists()) {
            kept = java.io.File.createTempFile("book", ".bak");
            java.nio.file.Files.copy(live.toPath(), kept.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            SavedCharts.put("SuiteA", "1955-04-18", "09:30", "Bern, CH",
                com.zodiacomputing.ourania.astro.Rodden.X, "", "");
            SavedCharts.put("SuiteB", "1960-11-02", "14:15", "Rome, IT",
                com.zodiacomputing.ourania.astro.Rodden.AA, "", "");

            // <b>Cleared first.</b> Building the recorder runs OuraniaWindow's constructor,
            // which switches to the wheel - so the recorder has already seen a navigation
            // before any chart is loaded, and asserting on it without this reads the app's
            // own startup as the bug under test.
            went[0] = null;
            drew[0] = false;
            SwingUtilities.invokeAndWait(() -> cp[0].applySavedProfile("SuiteA", false));
            ok("loading Chart A does not draw the chart", !drew[0]);
            ok("loading Chart A does not navigate away", went[0] == null);
            eq("Chart A's date is filled", "1955-04-18",
                ((JTextField) fieldOf(cp[0], "baseDateField")).getText());
            eq("Chart A's rating came back", com.zodiacomputing.ourania.astro.Rodden.X,
                roddenOf(cp[0]));
            ok("a timeless chart disables its time field",
                !((JTextField) fieldOf(cp[0], "baseTimeField")).isEnabled());

            went[0] = null;
            drew[0] = false;
            SwingUtilities.invokeAndWait(() -> cp[0].applySavedProfile("SuiteB", true));
            ok("loading Chart B does not draw the chart", !drew[0]);
            ok("loading Chart B does not navigate away", went[0] == null);
            eq("Chart B's date is filled", "1960-11-02",
                ((JTextField) fieldOf(cp[0], "transitDateField")).getText());
            eq("Chart A survived loading Chart B", "1955-04-18",
                ((JTextField) fieldOf(cp[0], "baseDateField")).getText());

            // <b>The mode is the reader's.</b> Loading a person into Chart B used to force
            // synastry - right about the astrology, wrong about whose decision it is, and the
            // mode changed under you every time you picked a chart. It is left alone now and
            // the mismatch is said on screen instead; see partnerNotice.
            java.lang.reflect.Field fm =
                ChartSetupPanel.class.getDeclaredField("selectedMode");
            fm.setAccessible(true);
            java.lang.reflect.Field fh =
                ChartSetupPanel.class.getDeclaredField("partnerHint");
            fh.setAccessible(true);
            JLabel hint = (JLabel) fh.get(cp[0]);
            for (final ChartMode m : ChartMode.values()) {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        java.lang.reflect.Method sm = ChartSetupPanel.class
                            .getDeclaredMethod("setMode", ChartMode.class);
                        sm.setAccessible(true);
                        sm.invoke(cp[0], m);
                        cp[0].applySavedProfile("SuiteB", true);
                    } catch (Exception e) {
                        failures.add("loading Chart B threw in " + m + ": " + e);
                    }
                });
                eq("loading Chart B leaves the mode at " + m, m, fm.get(cp[0]));
                ok(m + ": the mismatch line shows exactly when there is one",
                    hint.isVisible() == !ChartSetupPanel.isRelationship(m));
            }

            SwingUtilities.invokeAndWait(() -> cp[0].applySavedProfile("SuiteB", false));
            eq("a rated chart restores its rating",
                com.zodiacomputing.ourania.astro.Rodden.AA, roddenOf(cp[0]));
            ok("and its time field is live again",
                ((JTextField) fieldOf(cp[0], "baseTimeField")).isEnabled());

            SavedCharts.remove("SuiteA");
            SavedCharts.remove("SuiteB");
        } finally {
            if (kept != null) {
                java.nio.file.Files.copy(kept.toPath(), live.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                kept.delete();
            }
            SwingUtilities.invokeAndWait(rec::dispose);
        }
    }

    private static Object fieldOf(ChartSetupPanel p, String name) throws Exception {
        Field f = ChartSetupPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(p);
    }

    private static Object roddenOf(ChartSetupPanel p) throws Exception {
        Field f = ChartSetupPanel.class.getDeclaredField("baseRodden");
        f.setAccessible(true);
        return ((JComboBox<?>) f.get(p)).getSelectedItem();
    }

    private static void theSecondSlot() {
        for (ChartMode from : ChartMode.values()) {
            boolean relationship = ChartSetupPanel.isRelationship(from);
            ok("isRelationship agrees with the mode " + from,
                relationship == (from == ChartMode.SYNASTRY
                    || from == ChartMode.COMPOSITE_MIDPOINT
                    || from == ChartMode.COMPOSITE_DAVISON));
        }
        // The two that used to be handled differently from each other, and the reason why.
        ok("a single chart is not a relationship",
            !ChartSetupPanel.isRelationship(ChartMode.SINGLE));
        ok("natal-and-transit is not a relationship - this is the one that leaked",
            !ChartSetupPanel.isRelationship(ChartMode.TRANSIT));
        ok("synastry is a relationship",
            ChartSetupPanel.isRelationship(ChartMode.SYNASTRY));
        ok("both composites are relationships",
            ChartSetupPanel.isRelationship(ChartMode.COMPOSITE_MIDPOINT)
                && ChartSetupPanel.isRelationship(ChartMode.COMPOSITE_DAVISON));
    }

    private static void ok(String label, boolean condition) {
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

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
