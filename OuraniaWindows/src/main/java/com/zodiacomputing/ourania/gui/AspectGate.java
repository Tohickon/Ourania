package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;

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
 *
 * <p><b>Shared with the phone, and {@code drawsPair} joined it, for M11.</b> The phone's globe
 * has to draw the chords the desktop's draws - the same aspects, within the same orbs, under
 * the same two settings - and the only way that is true rather than coincidental is for both
 * to ask one gate. {@code SkymapPanel.drawsPair} was the second half of the question and was
 * private to the panel, so it is {@link #draws} here now and the panel asks it. Reading
 * {@code Bodies} rather than the panel's name cache is what lets this file cross the line at
 * all; that cache is filled from {@code Bodies} in a static initialiser and never written
 * again, so it is the same answer from one place instead of two.
 */
public final class AspectGate {

    private AspectGate() {
    }

    /**
     * The aspect a separation makes between two registry points, or null when it makes none
     * or the reader has switched that aspect off.
     *
     * @param shown one flag per {@link Aspects.Type}, by ordinal; null, or too short to cover a
     *              type, shows it - a missing setting is not a reason to hide an aspect
     */
    public static Aspects.Type visible(double sep, int a, int b, Aspects.Profile profile,
            boolean[] shown) {
        Aspects.Type type = Aspects.typeOf(sep, nameOf(a), nameOf(b), profile);
        if (type == null) {
            return null;
        }
        return shown == null || type.ordinal() >= shown.length
            || shown[type.ordinal()] ? type : null;
    }

    /** A registry point's name, or null out of range - the contract Aspects.typeOf expects. */
    private static String nameOf(int index) {
        Bodies.Def d = index >= 0 && index < Bodies.count() ? Bodies.at(index) : null;
        return d == null ? null : d.name;
    }

    /**
     * Whether this pair earns a line across the wheel, under the reader's chosen mode.
     *
     * <p><b>The aspect still exists either way.</b> It is computed, it is in the grid, it is in
     * the placements and the reading; the mode decides only what becomes a line through the
     * middle. Tompkins' rule, which is about legibility rather than significance: note the
     * aspects to the minor bodies, do not draw them, so the essentials can be found quickly.
     *
     * <p>Deliberately separate from {@link #visible}, which is the gate the grid shares: the
     * first attempt put the mode inside it and emptied the grid of every asteroid and point as
     * well, 286 failures in AspectGridCheck.
     *
     * @param mode {@code Settings.aspectMode()}
     */
    public static boolean draws(int a, int b, String mode) {
        if (Settings.ASPECTS_ESOTERIC.equals(mode)) {
            return true;
        }
        boolean angles = Settings.ASPECTS_MANIFESTATION.equals(mode);
        return drawable(a, angles) && drawable(b, angles);
    }

    /** A classical planet always; an angle too once Manifestation is chosen. */
    private static boolean drawable(int index, boolean anglesCount) {
        Bodies.Def d = index >= 0 && index < Bodies.count() ? Bodies.at(index) : null;
        if (d == null) {
            return false;
        }
        if (d.kind == Bodies.Kind.LUMINARY || d.kind == Bodies.Kind.PLANET) {
            return true;
        }
        return anglesCount && d.kind == Bodies.Kind.ANGLE;
    }

    /** The same, as the label the grid and the links carry. */
    static String label(double sep, int a, int b, Aspects.Profile profile, boolean[] shown) {
        Aspects.Type type = visible(sep, a, b, profile, shown);
        return type == null ? null : type.label;
    }
}
