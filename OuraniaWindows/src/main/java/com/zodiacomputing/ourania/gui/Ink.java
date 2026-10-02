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
