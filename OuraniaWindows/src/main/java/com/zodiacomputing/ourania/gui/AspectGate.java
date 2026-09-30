package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;

/**
 * Which aspect two points make, and whether the reader has that aspect switched on.
 *
 * <p><b>The one gate the wheel and the grid share.</b> There are two draw paths - the grid
 * asks for a label, the wheel's lines ask for a type - and a visibility filter added to only
 * one of them would produce a chart whose grid and whose lines disagree about which aspects
 * exist. That is this project's most-logged defect and it would be invisible: both surfaces
 * look entirely plausible on their own.
 *
 * <p>It lived on {@link SkymapPanel} as {@code visibleAspect}, reading the panel's
 * {@code aspectShown}. It is static here and takes that array, so the aspect grid could leave
 * the panel (J13, step 3c) without taking a second copy of the rule with it. The panel's
 * {@code visibleAspect} still exists and asks this.
 */
final class AspectGate {

    private AspectGate() {
    }

    /**
     * The aspect a separation makes between two registry points, or null when it makes none
     * or the reader has switched that aspect off.
     *
     * @param shown one flag per {@link Aspects.Type}, by ordinal; null, or too short to cover a
     *              type, shows it - a missing setting is not a reason to hide an aspect
     */
    static Aspects.Type visible(double sep, int a, int b, Aspects.Profile profile,
            boolean[] shown) {
        Aspects.Type type = Aspects.typeOf(sep, SkymapPanel.planetName(a),
            SkymapPanel.planetName(b), profile);
        if (type == null) {
            return null;
        }
        return shown == null || type.ordinal() >= shown.length
            || shown[type.ordinal()] ? type : null;
    }

    /** The same, as the label the grid and the links carry. */
    static String label(double sep, int a, int b, Aspects.Profile profile, boolean[] shown) {
        Aspects.Type type = visible(sep, a, b, profile, shown);
        return type == null ? null : type.label;
    }
}
