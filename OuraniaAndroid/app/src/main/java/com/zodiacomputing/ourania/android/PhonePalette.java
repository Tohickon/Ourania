package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.gui.AngleRole;
import com.zodiacomputing.ourania.gui.Ink;

/**
 * The phone's colours, in one place, as {@link Ink}s (M11).
 *
 * <h3>Why this exists, and what it is not</h3>
 *
 * <p>The phone's wheel has always carried its own colours, hard-coded: four element colours in
 * {@code WheelView} and ten planet colours in {@link PhoneWheel#faceColour}. The globe needs
 * the same answers - {@link PhoneGlobeSource} is asked for seven different inks - and writing
 * a second table for it would have made three. So the element table moved here out of
 * {@code WheelView}, the bodies are read from {@code PhoneWheel}, and both phone surfaces now
 * colour themselves from one file.
 *
 * <p><b>This is NOT the desktop's palette, and that is a known gap rather than a decision.</b>
 * The desktop's colours come from {@code ChartPalette} - the reader's chosen template, every
 * element and aspect overridable - and the phone cannot read it: {@code ChartPalette} is not in
 * {@code EngineIsolationCheck.SHARED} because two of its methods return AWT {@code Color}s.
 * Its other forty return hex strings and would cross the line untouched. Until that is done the
 * phone ignores the reader's palette, exactly as its flat wheel already did, and the two
 * surfaces at least agree with each other.
 *
 * <p>Consolidating rather than duplicating is the point: when the palette does become shared,
 * this file is the one place that changes.
 */
final class PhonePalette {

    private PhonePalette() { }

    /**
     * Fire, earth, air, water by index.
     *
     * <p><b>Moved here from {@code WheelView.elementRgb}, values unchanged.</b> They are the
     * phone's own, and deliberately not the desktop's - which are
     * {@code (255,69,0) (50,205,50) (255,215,0) (30,144,255)}. Whether the phone should look
     * like the desktop is the question the shared-palette item settles; this file only stops
     * the phone disagreeing with itself.
     */
    static Ink element(int element) {
        switch (element) {
            case 0: return new Ink(255, 120, 90);
            case 1: return new Ink(120, 200, 110);
            case 2: return new Ink(240, 214, 90);
            default: return new Ink(110, 170, 255);
        }
    }

    /** A body's own colour, from the table the phone's wheel already draws its planets with. */
    static Ink body(int index) {
        int[] rgb = PhoneWheel.faceColour(index);
        return new Ink(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * The fill of a body's bead on a ring of this role.
     *
     * <p>The desktop's three constants, which are constants there too - no palette reaches
     * them, so these are the same colours rather than the phone's own.
     */
    static Ink bead(AngleRole role) {
        switch (role) {
            case BRIDGE: return new Ink(62, 54, 38);
            case SKY: return new Ink(24, 48, 74);
            default: return new Ink(62, 66, 76);
        }
    }

    /** A glyph's colour on a ring of this role: the body's own, dimmed for a visiting ring. */
    static Ink ring(int index, AngleRole role) {
        Ink base = body(index);
        if (role == AngleRole.ANCHOR) {
            return base;
        }
        // A ring that is not the reader's own chart reads quieter, which is the distinction the
        // desktop draws with its own ring inks.
        return new Ink(base.getRed(), base.getGreen(), base.getBlue(), 200);
    }

    /** The lunar mansions' ring. The desktop's default, which no template overrides. */
    static Ink mansion() {
        return new Ink(181, 160, 227);
    }

    /** The leader lines from a body down to the sign ring. */
    static Ink leader() {
        return Ink.WHITE;
    }

    /**
     * An aspect chord's colour.
     *
     * <p><b>The one question the phone cannot answer properly yet, and it is colour only.</b>
     * WHETHER a chord is drawn is decided by {@code Aspects} and {@code Settings}, both shared,
     * so the phone's globe draws exactly the chords the desktop's would. What it cannot do is
     * colour them per aspect: those fifteen colours live in {@code ChartPalette.aspectHex} and
     * nowhere else.
     *
     * <p>So one ink for every chord, at the alpha the desktop uses, rather than a copy of
     * fifteen hex defaults that would then be the second place they live. The label is taken
     * even though it is unused, so that the day the palette is shared this method's body
     * changes and no caller does.
     */
    static Ink aspect(String aspectLabel) {
        return new Ink(150, 160, 190, 150);
    }
}
