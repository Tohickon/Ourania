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

    @Test
    public void planetsAreSizedInTheirTrueOrderAndNoneOutgrowsTheBand() {
        PhoneWheel w = wheel();
        String[] bigToSmall = {"Sun", "Jupiter", "Saturn", "Uranus", "Neptune", "Venus",
            "Mars", "Mercury", "Moon", "Pluto"};
        float last = Float.MAX_VALUE;
        for (String name : bigToSmall) {
            int i = com.zodiacomputing.ourania.astro.Bodies.indexOfName(name);
            float f = PhoneWheel.sizeFactor(i);
            assertTrue(name + " is smaller than the one before it", f < last);
            assertTrue(name + " is a slight variance, not a vanishing one", f >= 0.72f);
            assertTrue(name + " fits the band built round the Sun",
                w.planetRadius(i) <= PhoneWheel.maxPlanetRadius());
            last = f;
        }
        assertEquals("the Sun is the band's own disc", PhoneWheel.maxPlanetRadius(),
            w.planetRadius(com.zodiacomputing.ourania.astro.Bodies.indexOfName("Sun")));
    }

    @Test
    public void theMoonsLitAreaFollowsItsPhase() {
        float rad = 20f;
        double disc = Math.PI * rad * rad;
        assertEquals("new: nothing lit", 0.0, litArea(0) / disc, 0.01);
        assertEquals("first quarter: half", 0.5, litArea(90) / disc, 0.01);
        assertEquals("full: all of it", 1.0, litArea(180) / disc, 0.01);
        assertEquals("last quarter: half", 0.5, litArea(270) / disc, 0.01);
        assertEquals("crescent at 45 degrees: (1 - cos e) / 2",
            (1 - Math.cos(Math.toRadians(45))) / 2, litArea(45) / disc, 0.01);
        assertTrue("waxing is lit on the right", centroidX(90) > 0);
        assertTrue("waning is lit on the left", centroidX(270) < 0);
    }

    private static double litArea(double elongation) {
        float[] p = PhoneWheel.moonLitOutline(0, 0, 20f, elongation, 400);
        double a = 0;
        for (int k = 0; k < p.length; k += 2) {
            int n = (k + 2) % p.length;
            a += p[k] * p[n + 1] - p[n] * p[k + 1];
        }
        return Math.abs(a) / 2;
    }

    private static double centroidX(double elongation) {
        float[] p = PhoneWheel.moonLitOutline(0, 0, 20f, elongation, 400);
        double sum = 0;
        for (int k = 0; k < p.length; k += 2) {
            sum += p[k];
        }
        return sum / (p.length / 2);
    }
}
