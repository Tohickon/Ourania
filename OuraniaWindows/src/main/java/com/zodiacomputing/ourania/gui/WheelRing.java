package com.zodiacomputing.ourania.gui;

import de.thmac.swisseph.SweDate;
import java.time.ZonedDateTime;

/**
 * One ring of the wheel: every body's position on it, and the moment and place it was cast for.
 *
 * <p><b>J13, step 1 (2026-09-19).</b> These were thirty loose fields on SkymapPanel, ten per ring,
 * named three different ways - {@code bLon} / {@code baseChartTime}, {@code tLon} /
 * {@code transitChartTime}, {@code cLon} / {@code triCusps} / {@code skyChartTime} - and declared
 * across a thousand lines. Every part of the panel read them directly: the painter, the hover
 * text, the placements table, the click handling, the scrub bars. That shared, scattered state is
 * what made the file impossible to split; a piece moved out would have had to reach back into
 * the panel for every number it drew. Gathered here, a ring is one thing that can be handed over.
 *
 * <p>The fields stay public and the arrays stay replaceable, exactly as the loose fields were:
 * this step changes where the data lives, not who may touch it. Narrowing that comes later,
 * once the pieces that read it have moved out.
 */
public final class WheelRing {

    /** Each body's ecliptic longitude on this ring, by registry index. */
    public double[] lon = new double[SkymapPanel.BODY_COUNT];
    /** Each body's speed in longitude, degrees per day; negative is retrograde. */
    public double[] speed = new double[SkymapPanel.BODY_COUNT];
    /** Whether the body is on this ring: computed, and switched on. */
    public boolean[] valid = new boolean[SkymapPanel.BODY_COUNT];
    /** Whether the ephemeris could place the body at all, whatever the selection. */
    public boolean[] ok = new boolean[SkymapPanel.BODY_COUNT];
    /** House cusps 1-12 (index 0 unused), as this ring was cast. */
    public double[] cusps = new double[13];
    /** The Ascendant this ring was cast with. */
    public double ascendant;
    /** The moment this ring shows. */
    public ZonedDateTime time;
    /** The same moment, as the ephemeris takes it. */
    public SweDate sd;
    /** Where this ring was cast for. Greenwich until something says otherwise. */
    public double latitude = 51.4779;
    public double longitude = 0.0;
}
