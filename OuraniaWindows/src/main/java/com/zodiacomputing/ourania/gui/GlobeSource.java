package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.LunarMansions;

import java.awt.Color;

/**
 * Everything the globe asks of the chart it draws (M11, stage 2).
 *
 * <p><b>Why this exists.</b> {@link GlobeRenderer} read {@code SkymapPanel} directly, in about
 * eighty places - its rings' fields, its focus, its layer switches - so the globe could only
 * ever be drawn by the desktop window. The phone has a chart and a camera but no window. This
 * is the list of questions the renderer actually asks; the desktop panel answers them from its
 * own state, exactly as the renderer used to read it, and the phone will answer them from its
 * own. It is {@link WheelSource}'s counterpart for the globe.
 *
 * <p><b>Rings are numbered as the renderer always numbered them:</b> 0 the inner chart, 1 the
 * outer ring (a partner or transits), 2 the sky.
 *
 * <p>Colours, the layer enum and the angle roles are still the desktop's types; stage 3 takes
 * the renderer off AWT and they move with it.
 */
interface GlobeSource {

    // ------------------------------------------------------------ the chart

    /** The longitude drawn at the reference meridian: the Ascendant, usually. */
    double pinLongitude();

    /** The house cusps the globe divides the sphere by, 1..12. */
    double[] activeCusps();

    /** A ring's longitudes, by body index - the array itself, not a copy. */
    double[] ringLon(int ring);

    /** Which of a ring's bodies computed, by body index - the array itself. */
    boolean[] ringValid(int ring);

    /** Whether the outer ring (1) and the sky (2) are drawn at all. */
    boolean outerRingDrawn();

    boolean triRingDrawn();

    /** Which deck - upper, middle, lower - a ring rides on. */
    int ringDeck(int ring);

    /** The radius the natal bodies start from, for the wheel layout the top view matches. */
    int bodyBaseRadius(int[] rings);

    LunarMansions.Mansion moonMansion();

    // ------------------------------------------------------------ what is shown

    boolean layerShown(SkymapPanel.Layer layer);

    /** How far a layer has opened, 0..1, while it blooms. */
    double layerOpen(SkymapPanel.Layer layer);

    SkymapPanel.AngleRole angleRoleFor(boolean isSky, boolean isTransit);

    // ------------------------------------------------------------ focus

    int focusedBody();

    double focusedLongitude();

    int focusedHouse();

    int focusedMansion();

    int focusedDegree();

    boolean onGlobeFocus(int body, boolean outer);

    // ------------------------------------------------------------ aspects

    /** The chords, cached by the source and rebuilt with {@code build} when stale. */
    int[][] globeChords(java.util.function.Supplier<int[][]> build);

    boolean drawsNatalAspects();

    boolean drawsCrossAspects();

    boolean lightsChord(int a, int b, int ring);

    Color aspectInkFor(double lonA, double lonB, int a, int b, boolean cross);

    Color ringInk(int body, SkymapPanel.AngleRole role);
}
