package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;

import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Presets stage 2b: the bar, and the four things it could quietly get wrong.
 *
 * <p><b>This suite exists because the rule and its only consumer would otherwise be checked
 * apart.</b> {@code OrbCheck} proves the engine keeps four independent tables and that synastry
 * derives from the natal width in force. None of that says the panel reads or writes the right
 * one, and every defect below is invisible from the engine's side:
 *
 * <ul>
 * <li><b>A</b> - the switch shows what is stored, and going back shows what was there before.</li>
 * <li><b>B</b> - <b>switching leaves every stored table exactly as it was.</b> Setting a spinner
 *     fires its change listener, which writes under {@code shownProfile}. This is asserted by
 *     reading the stored keys rather than the spinners, because the spinners would show the right
 *     numbers either way - the defect is what gets written behind them. <b>What it catches is the
 *     ordering:</b> assigning {@code shownProfile} after the reseed loop instead of before sends
 *     every write to the profile being switched away from, and the reader's natal table is
 *     overwritten with synastry widths. That mutation takes 14 assertions here red. Removing the
 *     {@code seeding} guard takes none - it saves 51 chart rebuilds per switch, not the data.</li>
 * <li><b>C</b> - bold means "you changed it" measured against the shown profile's default. On
 *     synastry that default is half the natal width, so comparing with the natal figure would
 *     bold every untouched point the moment the reader switched.</li>
 * <li><b>D</b> - a ceiling's spinner bound moves with the profile, or a reader can type a number
 *     {@code Settings.setAspectCap} then silently clamps: the screen and the file disagreeing.</li>
 * <li><b>E</b> - Reset touches only what is on screen. The no-argument {@code resetBodyOrbs}
 *     clears all four profiles, so the button must not call it.</li>
 * </ul>
 */
public final class PresetBarCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** A point nobody is likely to have opinions about, and one with a wide natal width. */
    private static final String NARROW = "Moon";
    private static final String WIDE = "Sun";

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        Settings.resetBodyOrbs();
        Settings.resetAspectCaps();

        part("A: the switch shows what is stored", PresetBarCheck::shows);
        part("B: switching writes nothing", PresetBarCheck::writesNothing);
        part("C: bold is measured against the shown profile", PresetBarCheck::bold);
        part("D: a ceiling's bound moves with the profile", PresetBarCheck::bound);
        part("E: Reset touches only what is on screen", PresetBarCheck::reset);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + "):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    // ------------------------------------------------------------------ A

    private static void shows() throws Exception {
        Settings.resetBodyOrbs();
        Settings.setBodyOrb(NARROW, 4.5);
        Settings.setBodyOrb(NARROW, 2.0, Aspects.Profile.SYNASTRY);

        SettingsPanel p = panel();
        JSpinner s = orbSpinner(p, NARROW);
        near("it opens on natal", 4.5, value(s));

        click(p, Aspects.Profile.SYNASTRY);
        near("Synastry shows the synastry width", 2.0, value(s));

        click(p, Aspects.Profile.COMPOSITE);
        near("Composite follows natal, which is 4.5", 4.5, value(s));

        click(p, Aspects.Profile.TRANSIT);
        near("Transits is flat, whatever natal does",
            com.zodiacomputing.ourania.astro.Transits.DEFAULT_ORB, value(s));

        click(p, Aspects.Profile.NATAL);
        near("and going back shows what was there before", 4.5, value(s));

        // A point nobody has set in synastry follows the natal width IN FORCE, which is the
        // derivation OrbCheck's Part G asserts - here it is asserted through the screen.
        Settings.setBodyOrb(WIDE, 12.0);
        SettingsPanel q = panel();
        click(q, Aspects.Profile.SYNASTRY);
        near("an unset synastry point shows half the natal width in force, not half the table",
            6.0, value(orbSpinner(q, WIDE)));
    }

    // ------------------------------------------------------------------ B

    private static void writesNothing() throws Exception {
        Settings.resetBodyOrbs();
        Settings.resetAspectCaps();
        Settings.setBodyOrb(NARROW, 4.5);

        SettingsPanel p = panel();
        // Walk every profile, twice round, the way a reader comparing them would.
        for (int lap = 0; lap < 2; lap++) {
            for (Aspects.Profile profile : Aspects.Profile.values()) {
                click(p, profile);
            }
        }

        // <b>Read the stored keys, not the spinners.</b> The spinners would show the right
        // numbers either way; the defect is what got written behind them. Shown able to fail:
        // moving shownProfile's assignment below the reseed loop takes fourteen of these red.
        ok("a point nobody set in synastry is still unset after visiting it",
            stored(Aspects.Profile.SYNASTRY, WIDE).isEmpty());
        ok("and in composite", stored(Aspects.Profile.COMPOSITE, WIDE).isEmpty());
        ok("and in transits", stored(Aspects.Profile.TRANSIT, WIDE).isEmpty());
        ok("the point that WAS set in natal is still set, and only in natal",
            !stored(Aspects.Profile.NATAL, NARROW).isEmpty()
                && stored(Aspects.Profile.SYNASTRY, NARROW).isEmpty());
        near("and it still reads what the reader chose", 4.5, Settings.bodyOrb(NARROW));

        for (Aspects.Type t : Aspects.Type.values()) {
            ok("no ceiling was written into synastry by looking at it: " + t.name(),
                Settings.get("orb.synastry.aspect." + t.name(), "").trim().isEmpty());
        }
    }

    // ------------------------------------------------------------------ C

    private static void bold() throws Exception {
        Settings.resetBodyOrbs();
        Settings.setBodyOrb(WIDE, 12.0);

        SettingsPanel p = panel();
        JSpinner wide = orbSpinner(p, WIDE);
        JSpinner narrow = orbSpinner(p, NARROW);
        ok("a natal point the reader widened is bold", isBold(wide));
        ok("and one they left alone is not", !isBold(narrow));

        click(p, Aspects.Profile.SYNASTRY);
        // Both points are untouched in synastry. The wide one is only "changed" if the marker
        // measures it against the NATAL default, which is the bug.
        ok("on Synastry, a point nobody has set there is not bold", !isBold(wide));
        ok("nor is the other", !isBold(narrow));

        Settings.setBodyOrb(WIDE, 3.0, Aspects.Profile.SYNASTRY);
        click(p, Aspects.Profile.NATAL);
        click(p, Aspects.Profile.SYNASTRY);
        ok("but a point set IN synastry is bold there", isBold(wide));
        click(p, Aspects.Profile.NATAL);
        ok("and that does not make it bold in natal - it was already", isBold(wide));
        Settings.resetBodyOrbs();
    }

    // ------------------------------------------------------------------ D

    private static void bound() throws Exception {
        Settings.resetAspectCaps();
        SettingsPanel p = panel();
        Aspects.Type t = Aspects.Type.values()[0];
        JSpinner s = capSpinner(p, t);

        near("the natal bound is the declared ceiling", Aspects.defaultCapOf(t), maximum(s));
        click(p, Aspects.Profile.SYNASTRY);
        near("the synastry bound is half of it", Aspects.defaultCapOf(t) / 2.0, maximum(s));
        near("and it is what the engine would give",
            Aspects.defaultCapOf(t, Aspects.Profile.SYNASTRY), maximum(s));
        click(p, Aspects.Profile.NATAL);
        near("and it goes back", Aspects.defaultCapOf(t), maximum(s));
    }

    // ------------------------------------------------------------------ E

    private static void reset() throws Exception {
        Settings.resetBodyOrbs();
        Settings.setBodyOrb(NARROW, 4.5);
        Settings.setBodyOrb(NARROW, 2.0, Aspects.Profile.SYNASTRY);

        SettingsPanel p = panel();
        click(p, Aspects.Profile.SYNASTRY);
        press(p);

        near("the shown profile is back to its default", 2.25,
            Settings.bodyOrb(NARROW, Aspects.Profile.SYNASTRY));
        // 2.25 is half of 4.5 - half of the natal width IN FORCE, not half the built-in table.
        near("and natal is untouched", 4.5, Settings.bodyOrb(NARROW));
        ok("which is the whole point: the button said Synastry",
            !stored(Aspects.Profile.NATAL, NARROW).isEmpty());
        Settings.resetBodyOrbs();
    }

    // ------------------------------------------------------------------ driving the panel

    private static SettingsPanel panel() throws Exception {
        final SettingsPanel[] out = new SettingsPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> out[0] = new SettingsPanel(null));
        return out[0];
    }

    /**
     * Press the bar's own button, so the wiring is under test and not only the method.
     *
     * <b>The label is asked of the panel, never spelled here.</b> A copy of the four names in
     * this file would pass while the screen said something else - which is the defect this
     * project keeps producing, and a suite that carries its own copy of the rule cannot catch it.
     */
    private static void click(SettingsPanel p, Aspects.Profile profile) throws Exception {
        java.lang.reflect.Method m =
            SettingsPanel.class.getDeclaredMethod("profileLabel", Aspects.Profile.class);
        m.setAccessible(true);
        String label = (String) m.invoke(null, profile);
        for (final JToggleButton b : toggles(p)) {
            if (label.equals(b.getText())) {
                javax.swing.SwingUtilities.invokeAndWait(() -> b.doClick());
                return;
            }
        }
        throw new IllegalStateException("no button on the bar reads " + label);
    }

    private static JSpinner orbSpinner(SettingsPanel p, String name) throws Exception {
        JSpinner[] all = (JSpinner[]) CheckReflect.get(p, "orbSpinners");
        for (int i = 0; i < Bodies.count(); i++) {
            if (Bodies.at(i).name.equals(name)) {
                return all[i];
            }
        }
        throw new IllegalStateException("no spinner for " + name);
    }

    private static JSpinner capSpinner(SettingsPanel p, Aspects.Type t) throws Exception {
        JSpinner[] all = (JSpinner[]) CheckReflect.get(p, "aspectCapSpinners");
        return all[t.ordinal()];
    }

    private static void press(SettingsPanel p) throws Exception {
        final javax.swing.JButton b = (javax.swing.JButton) CheckReflect.get(p, "orbResetButton");
        javax.swing.SwingUtilities.invokeAndWait(() -> b.doClick());
    }

    private static double value(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    private static double maximum(JSpinner s) {
        return ((Number) ((javax.swing.SpinnerNumberModel) s.getModel()).getMaximum())
            .doubleValue();
    }

    private static boolean isBold(JSpinner s) {
        Component editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            return ((JSpinner.DefaultEditor) editor).getTextField().getFont().isBold();
        }
        return false;
    }

    private static String stored(Aspects.Profile profile, String name) {
        String id = null;
        for (int i = 0; i < Bodies.count(); i++) {
            if (Bodies.at(i).name.equals(name)) {
                id = Bodies.at(i).id;
            }
        }
        String key = profile == Aspects.Profile.NATAL
            ? "orb.body." + id
            : "orb." + profile.key() + ".body." + id;
        return Settings.get(key, "").trim();
    }

    /** Every JToggleButton under a container, in the order they were added. */
    private static List<JToggleButton> toggles(Container c) {
        List<JToggleButton> out = new ArrayList<>();
        for (Component child : c.getComponents()) {
            if (child instanceof JToggleButton) {
                out.add((JToggleButton) child);
            } else if (child instanceof Container) {
                out.addAll(toggles((Container) child));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ harness

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        int before = failures.size();
        try {
            body.run();
        } catch (Exception e) {
            fail(title + " threw " + e);
        }
        System.out.println(title.substring(0, 1) + ": "
            + (failures.size() == before ? "clear" : "RED"));
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  ok   " + label);
        } else {
            fail(label);
        }
    }

    private static void near(String label, double expect, double got) {
        checks++;
        if (Math.abs(expect - got) < 1e-9) {
            System.out.println("  ok   " + label);
        } else {
            fail(label + " (expected " + expect + ", got " + got + ")");
        }
    }

    private static void fail(String label) {
        failures.add(label);
        System.out.println("  FAIL " + label);
    }

    private PresetBarCheck() {
    }
}
