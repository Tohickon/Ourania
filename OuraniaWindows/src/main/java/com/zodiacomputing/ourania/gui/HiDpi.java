package com.zodiacomputing.ourania.gui;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;

/**
 * How many real pixels the screen has for each of Swing's.
 *
 * <p>Master list E: "Anti-aliased high-DPI rendering - RenderingHints present; HiDPI scaling on 4K
 * displays unverified". Measured on David's machine on 2026-09-15: a 3840 by 2400 panel at 300%
 * Windows scaling, which Java 21 reports as 1280 by 800 with a 3x default transform. Swing draws
 * every line, oval and glyph through that transform, so the wheel, the globe and the dial are sharp
 * at 3x with nothing done here. <b>What is not sharp is anything drawn into an image first</b>: an
 * image made at Swing's size is stretched three times over and comes out soft - which is what the
 * declination graph did, and what "1x (screen)" and Copy Chart handed the reader, a picture a third
 * the size of the one on screen.
 */
final class HiDpi {

    private HiDpi() { }

    /**
     * The scale of the screen this component is on, or of the main screen when it is on none.
     * Never below 1, and rounded to a quarter so a 1.2499 transform draws a whole-pixel image.
     */
    static double scale(Component c) {
        GraphicsConfiguration gc = c == null ? null : c.getGraphicsConfiguration();
        if (gc == null) {
            if (GraphicsEnvironment.isHeadless()) {
                return 1.0;
            }
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        return round(gc.getDefaultTransform().getScaleX());
    }

    static double round(double s) {
        if (Double.isNaN(s) || s < 1.0) {
            return 1.0;
        }
        return Math.round(s * 4.0) / 4.0;
    }

    /** "3x", "1.5x". */
    static String label(double s) {
        return (s == Math.rint(s) ? String.valueOf((int) s) : String.valueOf(s)) + "x";
    }
}
