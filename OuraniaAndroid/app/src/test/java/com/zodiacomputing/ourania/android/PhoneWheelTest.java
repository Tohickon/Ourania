package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;
import com.zodiacomputing.ourania.gui.WheelLayout;

import de.thmac.swisseph.SwissEph;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The phone's wheel geometry and tap targets, on the JVM (M4): the desktop's rings at the
 * phone's size, the chart the right way round, and every planet tappable where it is drawn.
 */
public class PhoneWheelTest {

    private static final int SIZE = 1080;     // a Galaxy S23's width in pixels

    private static PhoneChart.Cast sydney(LocalTime time) {
        Atlas.Place place = null;
        for (Atlas.Place p : Atlas.search("Sydney", 8)) {
            if (p.country != null && p.country.contains("Australia")) {
                place = p;
                break;
            }
        }
        assertNotNull("Sydney is in the atlas", place);
        return PhoneChart.cast(new SwissEph(Ephemeris.PATH), LocalDate.of(1975, 7, 4), time, place);
    }

    private static PhoneWheel wheel() {
        return PhoneWheel.of(sydney(LocalTime.of(14, 15)).frame, SIZE, SIZE);
    }

    @Test
    public void theRingsAreTheDesktopsRingsAtThePhonesSize() {
        PhoneWheel w = wheel();
        assertArrayEquals(WheelLayout.ringRadii(SIZE, SIZE, false), w.rings);
        assertTrue("the planets sit inside the signs",
            w.natalTop <= w.rings[WheelLayout.RING_SIGN_INNER]);
        assertTrue("and the aspect circle inside the planets", w.aspectDisc < w.natalFloor);
    }

    @Test
    public void theAscendantIsOnTheLeftAndTheIcAtTheBottom() {
        PhoneWheel w = wheel();
        float[] asc = w.point(w.frame.asc, 100);
        assertEquals(w.cx - 100, asc[0], 0.01);
        assertEquals(w.cy, asc[1], 0.01);
        // Ninety degrees on from the Ascendant is the bottom of the wheel (y grows downward).
        float[] ic = w.point(w.frame.asc + 90, 100);
        assertEquals(w.cx, ic[0], 0.01);
        assertEquals(w.cy + 100, ic[1], 0.01);
    }

    @Test
    public void withNoBirthTimeZeroAriesIsOnTheLeft() {
        PhoneWheel w = PhoneWheel.of(sydney(null).frame, SIZE, SIZE);
        assertEquals(0.0, w.pin, 0.0);
    }

    @Test
    public void everyPlanetIsTappedWhereItIsDrawn() {
        PhoneWheel w = wheel();
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            float[] p = w.body(i);
            assertNotNull(w.frame.bodies[i].name + " is drawn", p);
            assertEquals(w.frame.bodies[i].name, i, w.bodyAt(p[0], p[1], 20f));
        }
        assertEquals("the centre is no planet", -1, w.bodyAt(w.cx, w.cy, 20f));
    }

    @Test
    public void aspectLinesAreTheEnginesAndEndOnTheirPlanets() {
        PhoneWheel w = wheel();
        List<PhoneWheel.Line> lines = w.aspects();
        assertFalse("a chart has aspects", lines.isEmpty());
        for (PhoneWheel.Line l : lines) {
            assertTrue(l.a < l.b && l.b < PhoneWheel.PLANETS);
            float[] end = w.point(w.frame.bodies[l.a].lon, w.aspectDisc);
            assertEquals(w.aspectDisc, Math.hypot(end[0] - w.cx, end[1] - w.cy), 0.5);
        }
    }

    @Test
    public void aTappedPlanetShowsItsOwnLines() {
        PhoneChart.Cast c = sydney(LocalTime.of(14, 15));
        String sun = PhoneChart.planet(c, 0, InterpretationService.getInstance());
        assertTrue(sun, sun.startsWith("Sun  ") && sun.contains("Cancer") && sun.contains("house"));
        assertTrue("and it is the list's own entry",
            PhoneChart.describe(c, InterpretationService.getInstance()).contains(sun));
        assertEquals("nothing for no planet", "",
            PhoneChart.planet(c, -1, InterpretationService.getInstance()));
    }

    @Test
    public void houseNumbersSitInTheOpenRingClearOfTheLinesAndPlanets() {
        PhoneWheel w = wheel();
        float size = w.houseTextSize(3f);                 // an S23's density
        for (int h = 1; h <= 12; h++) {
            float[] n = w.housePoint(h);
            double r = Math.hypot(n[0] - w.cx, n[1] - w.cy);
            assertTrue("house " + h + " clear of the aspect lines",
                r - size / 2 > w.aspectDisc);
            assertTrue("house " + h + " clear of the planets", r + size / 2 < w.natalFloor);
        }
    }
}
