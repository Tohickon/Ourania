package com.zodiacomputing.ourania.gui;

/**
 * A colour with no toolkit behind it (M11, stage 3): red, green, blue and alpha, 0..255.
 *
 * <p><b>Why not {@code java.awt.Color}.</b> The globe's renderer is to be drawn on the phone,
 * which has no AWT. It used Color only as four numbers - built from them, read back from them,
 * once packed into an int - so this carries the same four numbers with the same method names
 * and the same rules, and each surface's {@link Pen} turns it into its own colour at the last
 * moment.
 *
 * <p><b>The same rules, deliberately.</b> A component outside 0..255 is refused with the
 * exception AWT throws, rather than clamped: the renderer was written against Color's
 * behaviour, and a colour that quietly clamped where Color threw would be a different program.
 * {@link #getRGB} packs as Color does, alpha in the top byte.
 */
public final class Ink {

    public static final Ink WHITE = new Ink(255, 255, 255);

    /**
     * A {@code #rrggbb} or {@code 0xrrggbb} hex string, or the fallback when it will not parse.
     *
     * <p><b>Here because the palette had to stop needing AWT (M11).</b> Every colour the reader
     * chooses is stored as a hex string, and {@code ChartPalette} is forty methods returning
     * those strings plus two that called {@code Color.decode} - and those two were the whole
     * reason the phone could not read the reader's palette. Decoding belongs with the colour
     * type rather than with the window toolkit, and this is the colour type both devices have.
     *
     * <p>Accepts the two forms {@code Color.decode} accepts, so a stored value written by the
     * desktop before this existed still reads. A null, a blank or a malformed string is not an
     * error - a missing setting is not a reason to have no colour - so it gives the fallback.
     */
    public static Ink of(String hex, Ink fallback) {
        if (hex == null) {
            return fallback;
        }
        String t = hex.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        } else if (t.startsWith("0x") || t.startsWith("0X")) {
            t = t.substring(2);
        }
        if (t.length() != 6) {
            return fallback;
        }
        try {
            int v = Integer.parseInt(t, 16);
            return new Ink((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** The same, giving white where nothing parses - the palette's own fallback of last resort. */
    public static Ink of(String hex) {
        return of(hex, WHITE);
    }

    private final int argb;

    public Ink(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Ink(int r, int g, int b, int a) {
        check(r, g, b, a);
        this.argb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** From packed bits, as {@code new Color(argb, true)} - or opaque when {@code hasAlpha} is false. */
    public Ink(int argb, boolean hasAlpha) {
        this.argb = hasAlpha ? argb : (0xFF000000 | argb);
    }

    private static void check(int r, int g, int b, int a) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0 || a > 255) {
            // Color's own message, so a stack trace reads the same as it did.
            throw new IllegalArgumentException("Color parameter outside of expected range:"
                + (a < 0 || a > 255 ? " Alpha" : "") + (r < 0 || r > 255 ? " Red" : "")
                + (g < 0 || g > 255 ? " Green" : "") + (b < 0 || b > 255 ? " Blue" : ""));
        }
    }

    public int getRed() {
        return (this.argb >> 16) & 0xFF;
    }

    public int getGreen() {
        return (this.argb >> 8) & 0xFF;
    }

    public int getBlue() {
        return this.argb & 0xFF;
    }

    public int getAlpha() {
        return (this.argb >>> 24) & 0xFF;
    }

    /** Alpha, red, green, blue packed into one int, as Color packs them. */
    public int getRGB() {
        return this.argb;
    }

    /**
     * The same colour at a given alpha, clamped to 0..255.
     *
     * <b>These three were private helpers inside GlobeRenderer</b>, which was the only place
     * that drew anything lit. They moved here when the wheel started drawing the planets too:
     * shading and tinting a colour is the colour's own business, and a second copy next to the
     * second caller is the defect this project spends most of its time removing. GlobeRenderer
     * keeps its one-line {@code shade}/{@code lighten}/{@code darken} wrappers so its fifty-odd
     * call sites did not all have to change to say the same thing.
     */
    public Ink withAlpha(int alpha) {
        return new Ink(this.getRed(), this.getGreen(), this.getBlue(),
            Math.max(0, Math.min(255, alpha)));
    }

    /** This colour mixed {@code t} of the way toward white; alpha is not carried. */
    public Ink lighter(double t) {
        return new Ink((int) (this.getRed() + (255 - this.getRed()) * t),
            (int) (this.getGreen() + (255 - this.getGreen()) * t),
            (int) (this.getBlue() + (255 - this.getBlue()) * t));
    }

    /**
     * This colour scaled toward black by {@code t}; alpha is not carried.
     *
     * <b>{@code t} is the fraction KEPT, not the fraction removed</b> - {@code darker(0.5)} is
     * half as bright. That is the sense GlobeRenderer's private version had and every call
     * site was written against, so inverting it here to read more naturally would have
     * silently changed every lit body on the globe.
     */
    public Ink darker(double t) {
        return new Ink((int) (this.getRed() * t), (int) (this.getGreen() * t),
            (int) (this.getBlue() * t));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ink && ((Ink) o).argb == this.argb;
    }

    @Override
    public int hashCode() {
        return this.argb;
    }

    @Override
    public String toString() {
        return "Ink[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + ",a=" + getAlpha()
            + "]";
    }
}
