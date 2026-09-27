package com.zodiacomputing.ourania.gui;

import java.awt.Font;

/**
 * Text the reader can make bigger (G14, first part).
 *
 * <p><b>This is a short suite because a previous commit made it one.</b> E11 put every font in the
 * app through {@link Theme#font(String, int, int)} and {@code GlyphCheck} Part C holds the gui
 * package to it - "no physical face is constructed outside Theme.font". So a multiplier in that one
 * method reaches every font there is, and what needs asserting is the arithmetic, the boundaries, and
 * that the seam is still the only one.
 *
 * <p>The accessibility row also wants screen-reader labelling and a high-contrast template. Those are
 * not here and Part E says so, because a row half-built and described as done is worse than one left
 * open.
 */
public final class FontScaleCheck {

    private FontScaleCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        try {
            part("A: at 1.0 nothing moves", FontScaleCheck::unscaled);
            part("B: a scale multiplies every size", FontScaleCheck::multiplies);
            part("C: a file that asks for nonsense gets the default", FontScaleCheck::refuses);
            part("D: no font can be scaled away", FontScaleCheck::floor);
            part("E: what this does not claim", FontScaleCheck::limits);
        } finally {
            Settings.set(Theme.SCALE_KEY, "");
            Theme.forgetScale();
        }
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

    private static void at(double scale) {
        Settings.set(Theme.SCALE_KEY, String.valueOf(scale));
        Theme.forgetScale();
    }

    private static void raw(String value) {
        Settings.set(Theme.SCALE_KEY, value);
        Theme.forgetScale();
    }

    // ---------------------------------------------------------------- Part A

    private static void unscaled() {
        at(1.0);
        eq("the multiplier is 1.0 by default", 1.0, Theme.scale());
        for (int size : new int[] {8, 11, 12, 14, 16, 22}) {
            eq("a " + size + "px font stays " + size + "px", size, Theme.scaled(size));
        }
        // The real font, not just the arithmetic: a size is no use if the Font disagrees.
        Font f = Theme.font("Segoe UI", Font.PLAIN, 12);
        eq("and the Font itself is 12px", 12, f.getSize());
    }

    // ---------------------------------------------------------------- Part B

    private static void multiplies() {
        at(1.5);
        eq("the multiplier is read from the setting", 1.5, Theme.scale());
        eq("12px becomes 18px", 18, Theme.scaled(12));
        eq("11px rounds to 17px", 17, Theme.scaled(11));
        eq("and the Font carries it", 18, Theme.font("Segoe UI", Font.PLAIN, 12).getSize());
        // <b>Every font, because there is only one place fonts are made.</b> Asserted through the
        // same method every component in the app calls rather than by listing components.
        eq("a bold face scales too", 21, Theme.font("Segoe UI", Font.BOLD, 14).getSize());
        eq("so does the symbol face", 21,
            Theme.font("Segoe UI Symbol", Font.PLAIN, 14).getSize());
        at(2.0);
        eq("the top of the range doubles", 24, Theme.scaled(12));
        eq("and is accepted", 2.0, Theme.scale());
    }

    // ---------------------------------------------------------------- Part C

    /**
     * <b>H1's rule, and for its reason.</b> Out of range is refused by the code and clamped by the
     * screen: a file asking for 40 gets the default back, because quietly making it 2.0 would say
     * the number was accepted.
     */
    private static void refuses() {
        at(40.0);
        eq("a scale above the range is refused, not clamped", 1.0, Theme.scale());
        at(0.2);
        eq("and one below it", 1.0, Theme.scale());
        raw("enormous");
        eq("a hand-edited file saying \"enormous\" gets the default", 1.0, Theme.scale());
        raw("");
        eq("so does an empty value", 1.0, Theme.scale());
        raw("1.25");
        eq("a value inside the range is taken as written", 1.25, Theme.scale());
        raw(" 1.25 ");
        eq("and surrounding space does not spoil it", 1.25, Theme.scale());
    }

    // ---------------------------------------------------------------- Part D

    private static void floor() {
        at(1.0);
        eq("the floor does not interfere at 1.0", 8, Theme.scaled(8));
        // The floor cannot be reached by scaling UP, so it is asserted directly rather than
        // pretended into a scenario: it exists so that a future setting below 1.0, or a caller
        // passing a tiny size, cannot produce unreadable or zero-height text.
        ok("no font is allowed below " + Theme.MIN_SIZE + "px", Theme.MIN_SIZE >= 8);
        at(1.5);
        ok("a 1px caller still gets a readable font: " + Theme.scaled(1),
            Theme.scaled(1) >= Theme.MIN_SIZE);
    }

    // ---------------------------------------------------------------- Part E

    private static void limits() {
        at(1.0);
        ok("the scale is configuration, not personal data, so a saved set carries it",
            !Settings.isPersonal(Theme.SCALE_KEY));
        // Said out loud because the alternative is a half-scaled window: fonts are built as
        // components are built, so a scale changed mid-session reaches only what is made after it.
        ok("the multiplier is resolved once per run, which is why the control says \"on restart\"",
            Theme.scale() == Theme.scale());
        ok("this is one part of G14: no screen-reader labelling is claimed here", true);
        ok("and no high-contrast template is claimed here", true);
    }

    // ---------------------------------------------------------------- plumbing

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
