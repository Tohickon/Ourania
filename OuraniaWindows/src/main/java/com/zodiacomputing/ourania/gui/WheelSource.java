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
     * The directed chart, in a band of its own between the middle ring and the sky (G17).
     *
     * <p>Empty unless {@link #arcRingDrawn} - the panel clears it when the directed chart is
     * riding the middle ring instead, so a painter that forgot to ask would draw nothing
     * rather than a stale chart.
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

    boolean layerShown(SkymapPanel.Layer layer);

    boolean outerRingDrawn();

    boolean triRingDrawn();

    /** Whether the directed chart has a band of its own to be drawn in (G17). */
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

    Color ringInk(int body, SkymapPanel.AngleRole role);

    Color ringAngleInk(SkymapPanel.AngleRole role);

    SkymapPanel.AngleRole angleRoleFor(boolean isSky, boolean isTransit);

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
