package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.LunarMansions;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Everything the wheel's painter reads, and nothing else.
 *
 * <h3>Why this exists (J13, step 6)</h3>
 *
 * <p>{@link WheelCanvas} left {@code SkymapPanel} in step 5b still holding the whole panel,
 * so what it depended on was sixty-odd members found by searching for "panel.". This is that
 * list written down as a contract. The canvas holds a {@code WheelSource} and can read only
 * what is here; the panel implements it. A second screen that wants to draw the wheel - the
 * phone's (M4) - implements this rather than being a {@code SkymapPanel}.
 *
 * <p><b>Read-only, measured before it was written.</b> The painter assigns to none of the
 * panel's state, so every field it used to read directly is an accessor here. The one call
 * that changes anything, {@link #clampView}, is the view keeping its pan inside the wheel at
 * the size being painted, and it is in the desktop group below.
 *
 * <p>Grouped by what a second implementation would have to supply. The first four groups
 * are the chart; the last is the desktop view around it, which another screen would answer
 * in its own way or not at all.
 */
interface WheelSource {

    // ------------------------------------------------------------ the chart

    WheelRing natalRing();

    WheelRing outerRing();

    WheelRing skyRing();

    /**
     * The directed chart's own band, between the sky and the middle ring (G17 step 3).
     *
     * <p><b>Draw-only, and deliberately so.</b> The three wheel INDICES - natal, outer, sky -
     * are what the aspect lines, the hover card, the hit test and the packed-hit encoding are
     * written against, and a fourth would have to be threaded through all of them. A directed
     * chart is a comparison a reader makes by eye against the ring beside it, so this band is
     * painted and not yet clickable. An implementation with no directed chart returns a ring
     * whose points are all invalid, which draws nothing.
     */
    WheelRing arcRing();

    /** The ring a wheel index names. */
    WheelRing ringAt(int wheel);

    ChartMode chartMode();

    /** Twelve house cusps, 1-based, the frame the wheel is drawn against. */
    double[] activeCusps();

    /** Which chart's houses frame the wheel: "Chart A", "Chart B", "Sky" or "Both". */
    String houseAlignment();

    boolean showTransitChart();

    /** The longitude drawn at the left of the wheel. */
    double getPinLongitude();

    LunarMansions.Mansion moonMansion();

    /** True when there is a cast chart to draw: an ephemeris, and a moment for Chart A. */
    boolean chartReady();

    // ------------------------------------------------------------ what is shown

    boolean layerShown(Layer layer);

    /** The name of each figure in autoPatterns, in the same order - for its glass plane. */
    String[] patternNames();

    /** Whether a line is one of the focused body's own, which glows gold (the focus rays). */
    boolean focusTouches(int a, int b, int wheel);

    /** How strongly a body on a ring is drawn under the focus or a lit aspect, 1 when neither. */
    double glyphWeight(int body, int ring);

    /** How strongly a line on a ring is drawn under the focus, 1 when none. */
    double focusWeight(int a, int b, int wheel);

    /** How far a hover pulse's sparks have run, 0 to 1, or -1 when none are running. */
    double pulse();

    /** How much a body swells under the hover, 1 when not at all. */
    double swell(int body, int ring);

    /** True when the focused body is the first end of this line. */
    boolean focusAtFirstEnd(int a, int wheel);

    /** Figures that run between charts, each member packed with its ring (WheelLayout.packHit). */
    int[][] crossFigures();

    /** Their names, in the same order. */
    String[] crossFigureNames();

    /** The focused body's longitude, or NaN - the wheel lights its sign, as the globe does. */
    double focusedLongitude();

    /** How far a layer is open, 0 folded to 1 shown - for a wash that fades with its chip. */
    double layerOpen(Layer layer);

    boolean outerRingDrawn();

    boolean triRingDrawn();

    /** Whether the directed band is open far enough to draw. See {@link #arcRing}. */
    boolean arcRingDrawn();

    double outerOpenFraction();

    double triOpenFraction();

    double arcOpenFraction();

    /** The radius chain and body placement for a panel this size. */
    SkymapPanel.Geometry geometry(int w, int h);

    // ------------------------------------------------------------ aspects

    Aspects.Type visibleAspect(double sep, int a, int b, Aspects.Profile profile);

    double getOrbFor(int a, int b, Aspects.Profile profile);

    Aspects.Profile profileForPair(boolean crossChart);

    boolean drawsPair(int a, int b);

    boolean drawsNatalAspects();

    boolean drawsCrossAspects();

    double[] wheelLon(int wheel);

    boolean[] wheelValid(int wheel);

    int endpointRadius(int wheel, int bodyIndex, int discRadius);

    // ------------------------------------------------------------ highlight, focus, hover

    int highlightA();

    int highlightB();

    int highlightWheel();

    int[] highlightPattern();

    int[][] autoPatterns();

    boolean isHighlighted(int a, int b, int wheel);

    boolean onHighlightedLine(int body, int wheel);

    int focusBody();

    boolean focusTransit();

    double focusWeight(int a, int b, boolean transitPair);

    boolean noFocus();

    int hoverBody();

    double glyphWeight(int body, boolean transit);

    // ------------------------------------------------------------ colour

    Color bodyColor(int bodyIndex);

    Color getElementColor(int element);

    Color ringInk(int body, AngleRole role);

    Color ringAngleInk(AngleRole role);

    AngleRole angleRoleFor(boolean isSky, boolean isTransit);

    // ------------------------------------------------------------ the desktop view

    boolean globeMode();

    /** Draws the globe instead of the wheel, filling a panel this size. */
    void paintGlobe(Graphics2D g, int w, int h);

    java.awt.geom.AffineTransform viewTransform(int w, int h);

    java.awt.Point toWheel(int x, int y);

    /** Keeps the pan inside the wheel at the size being painted. */
    void clampView();

    void paintHover(Graphics2D g2, SkymapPanel.Geometry g);

    void paintFitChip(Graphics2D g2);

    void paintScrubTag(Graphics2D g2, int w);

    String hoverTextAt(int x, int y);
}
