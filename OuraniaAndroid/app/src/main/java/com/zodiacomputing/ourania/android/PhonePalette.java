package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.gui.AngleRole;
import com.zodiacomputing.ourania.gui.ChartPalette;
import com.zodiacomputing.ourania.gui.Ink;

/**
 * The phone's colours: the reader's own palette, the same one the desktop paints with (M11).
 *
 * <h3>What changed, and why it is this file's whole purpose</h3>
 *
 * <p>The phone used to carry four element colours and ten planet colours hard-coded into its
 * own source, so a reader who chose the Cosmic template on the desktop, or set Mars to their
 * own red, saw none of it on the phone. That was not a decision anyone took - it was what
 * {@code ChartPalette} being desktop-only left behind.
 *
 * <p>It is shared now. Forty of its methods always returned hex strings, which any device can
 * read; the two that returned AWT {@code Color}s are {@code AwtPen}'s, and the decode is
 * {@link Ink#of}. So <b>every colour here is the reader's</b>, read from the same
 * {@code Settings} the phone already shares, and the phone's wheel and globe look like the
 * desktop's because they are reading the desktop's answer rather than imitating it.
 *
 * <p>The fallbacks are the desktop's own fallbacks, not the phone's old values: where the
 * reader has chosen nothing, both devices land on the same colour.
 */
final class PhonePalette {

    private PhonePalette() { }

    /**
     * Fire, earth, air, water by index - the reader's, or the desktop's default.
     *
     * <p>The defaults are {@code SkymapPanel}'s four constants, which is what the desktop draws
     * with no template chosen. The phone's old values - {@code (255,120,90)} and the rest -
     * are gone: they were a second palette, and the reader never asked for them.
     */
    static Ink element(int element) {
        return Ink.of(ChartPalette.elementHex(element, ELEMENT_DEFAULTS[clamp(element)]),
            Ink.of(ELEMENT_DEFAULTS[clamp(element)]));
    }

    /** The desktop's element colours: fire, earth, air, water. */
    private static final String[] ELEMENT_DEFAULTS =
        {"#FF4500", "#32CD32", "#FFD700", "#1E90FF"};

    private static int clamp(int element) {
        return element < 0 || element >= ELEMENT_DEFAULTS.length ? 0 : element;
    }

    /**
     * A body's colour: the reader's override, else its element's colour.
     *
     * <p>The desktop's order exactly - a body given a colour of its own wins, everything else
     * takes its element's. A point with no element at all (the south node, the lots, the
     * asteroids beyond the four) falls to white, as the desktop's own no-element fallback does
     * on a dark ground.
     *
     * <p><b>One method, where there were briefly two.</b> The desktop used to resolve this two
     * different ways - its wheel took the override and its globe's bound and decan rings did
     * not - so the phone mirrored both rather than pick a side and look like neither surface.
     * {@code SkymapPanel.bodyInkFor} takes the override now, so there is one rule on both
     * devices and this is it.
     */
    static Ink body(int index) {
        Bodies.Def d = index >= 0 && index < Bodies.count() ? Bodies.at(index) : null;
        if (d == null) {
            return Ink.WHITE;
        }
        Ink own = Ink.of(ChartPalette.bodyHex(d.id), null);
        if (own != null) {
            return own;
        }
        return d.element >= 0 ? element(d.element) : Ink.WHITE;
    }

    /**
     * The fill of a body's bead on a ring of this role.
     *
     * <p>The desktop's three constants. No template reaches them there either, so these are
     * the same colours rather than the phone's own - which is why they were already right.
     */
    static Ink bead(AngleRole role) {
        switch (role) {
            case BRIDGE: return new Ink(62, 54, 38);
            case SKY: return new Ink(24, 48, 74);
            default: return new Ink(62, 66, 76);
        }
    }

    /** A glyph's colour on a ring of this role: the body's own, quieter on a visiting ring. */
    static Ink ring(int index, AngleRole role) {
        Ink base = body(index);
        if (role == AngleRole.ANCHOR) {
            return base;
        }
        return new Ink(base.getRed(), base.getGreen(), base.getBlue(), 200);
    }

    /** The lunar mansions' ring - the reader's, else the desktop's lavender. */
    static Ink mansion() {
        return Ink.of(ChartPalette.mansionHex(null), new Ink(181, 160, 227));
    }

    /** The leader lines from a body down to the sign ring. */
    static Ink leader() {
        return Ink.of(ChartPalette.leaderHex(null), Ink.WHITE);
    }

    /**
     * An aspect chord's colour: the reader's, per aspect, at the alpha the globe draws with.
     *
     * <p><b>This is the one that was a single flat ink until the palette was shared.</b> All
     * fifteen aspects came out the same colour, because the alternative was copying fifteen hex
     * defaults into a second home. Now it reads them, so a reader who has made their squares
     * red sees red squares on the phone.
     *
     * <p>Alpha 150 is the desktop's, applied in the same place for the same reason: a chord is
     * a line across a sphere and an opaque one hides the far side.
     */
    static Ink aspect(String aspectLabel) {
        Ink base = Ink.of(ChartPalette.aspectHex(aspectLabel), Ink.WHITE);
        return new Ink(base.getRed(), base.getGreen(), base.getBlue(), 150);
    }

    /** The wheel's own ground, for a surface that paints a background before anything else. */
    static Ink background() {
        return Ink.of(ChartPalette.backgroundHex(null), new Ink(12, 14, 20));
    }
}
