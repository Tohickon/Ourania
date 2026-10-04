package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import org.junit.Test;

/**
 * The drill-down: {@link PhoneFocus}, and the hit tests on {@link PhoneWheel} that feed it.
 *
 * <p><b>All of it runs here because none of it is Android.</b> The stack, the house a tap
 * falls in, the degree it stands on and the reading each level resolves to are plain Java, and
 * keeping them that way is what makes the drill-down testable at all - the alternative was
 * this logic inside a View, where it could only be checked by looking at a phone.
 */
public class PhoneFocusTest {

    private static final int SIZE = 1000;

    private static ChartFrame chart() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1975, 7, 4, 14.5).getJulDay();
        return ChartFrame.compute(sw, jd, -33.8688, 151.2093, 'P', false, 0.0);
    }

    // ------------------------------------------------------------------ the hit tests

    @Test
    public void aPointOnTheWheelReportsTheLongitudeItStandsOn() {
        PhoneWheel w = PhoneWheel.of(chart(), SIZE, SIZE);
        // The inverse has to agree with the forward map at every bearing, or a tap lands on a
        // different degree from the one drawn under the reader's finger.
        for (double lon = 0; lon < 360; lon += 7) {
            float[] p = w.point(lon, w.rings[com.zodiacomputing.ourania.gui.WheelLayout
                .RING_SIGN_INNER] - 10);
            double back = w.longitudeAt(p[0], p[1]);
            double off = Math.abs(((back - lon + 540) % 360) - 180);
            assertTrue("round trip at " + lon + " came back " + back, off < 0.6);
        }
    }

    @Test
    public void aTapBeyondTheWheelStandsNowhere() {
        PhoneWheel w = PhoneWheel.of(chart(), SIZE, SIZE);
        assertTrue("a corner is outside the rings",
            Double.isNaN(w.longitudeAt(2f, 2f)));
        assertEquals(-1, w.houseAt(2f, 2f));
        assertEquals(-1, w.degreeAt(2f, 2f));
    }

    @Test
    public void everyHouseIsReachableAndTheAscendantLandsInTheFirst() {
        PhoneWheel w = PhoneWheel.of(chart(), SIZE, SIZE);
        ChartFrame f = w.frame;
        // A point a degree inside each cusp must fall in that cusp's own house. A degree in
        // rather than on it, because a boundary belongs to one side and which one is not the
        // claim being made.
        for (int h = 1; h <= 12; h++) {
            float[] p = w.point(f.cusps[h] + 1.0, w.aspectDisc + 20);
            assertEquals("a point just inside cusp " + h + " is in house " + h,
                h, w.houseAt(p[0], p[1]));
        }
    }

    @Test
    public void theDegreeUnderAPointIsTheDegreeDrawnThere() {
        PhoneWheel w = PhoneWheel.of(chart(), SIZE, SIZE);
        float[] p = w.point(43.5, w.aspectDisc + 30);        // 13 Taurus and a half
        assertEquals(1, w.signAt(p[0], p[1]));                // Taurus
        assertEquals(14, w.degreeAt(p[0], p[1]));             // the 14th degree
    }

    // ------------------------------------------------------------------ the stack

    @Test
    public void goingInAndBackOutLandsWhereItStarted() {
        PhoneFocus focus = new PhoneFocus(chart());
        assertEquals(PhoneFocus.Level.CHART, focus.level());
        assertFalse("the chart is the floor", focus.canAscend());

        focus.toHouse(1);
        assertEquals(PhoneFocus.Level.HOUSE, focus.level());
        focus.toBody(0);
        assertEquals(PhoneFocus.Level.PLANET, focus.level());
        focus.toDegree(4, 12);
        assertEquals(PhoneFocus.Level.DEGREE, focus.level());

        focus.ascend();
        assertEquals(PhoneFocus.Level.PLANET, focus.level());
        focus.ascend();
        assertEquals(PhoneFocus.Level.HOUSE, focus.level());
        focus.ascend();
        assertEquals(PhoneFocus.Level.CHART, focus.level());
        assertFalse(focus.canAscend());
        focus.ascend();
        assertEquals("ascending from the floor stays on the floor",
            PhoneFocus.Level.CHART, focus.level());
    }

    @Test
    public void aPlanetBringsItsOwnHouseWhicheverWayItIsReached() {
        ChartFrame f = chart();
        PhoneFocus viaChart = new PhoneFocus(f);
        viaChart.toBody(0);                      // straight from the whole chart
        PhoneFocus viaHouse = new PhoneFocus(f);
        viaHouse.toHouse(7);                     // from some other house entirely
        viaHouse.toBody(0);
        assertEquals("a planet's house is a fact about the planet, not about the route taken",
            viaChart.house(), viaHouse.house());
        assertEquals(PhoneFocus.houseOf(f, f.bodies[0].lon), viaChart.house());
    }

    @Test
    public void aBodyTheChartCouldNotComputeIsNotFocusable() {
        ChartFrame f = chart();
        PhoneFocus focus = new PhoneFocus(f);
        int missing = -1;
        for (int i = 0; i < f.bodies.length; i++) {
            if (f.bodies[i] == null || !f.bodies[i].ok) {
                missing = i;
                break;
            }
        }
        if (missing < 0) {
            return;                              // every body computed; nothing to prove here
        }
        focus.toBody(missing);
        assertEquals("focusing a body with no position leaves the focus alone",
            PhoneFocus.Level.CHART, focus.level());
    }

    @Test
    public void theTrailNamesEveryStepInOrder() {
        PhoneFocus focus = new PhoneFocus(chart());
        assertEquals("Chart", focus.trail());
        focus.toHouse(3);
        assertTrue(focus.trail().contains("House 3"));
        focus.toBody(4);
        assertTrue(focus.trail().contains(Bodies.at(4).name));
        focus.toDegree(0, 7);
        assertTrue(focus.trail().contains("7 Aries"));
        assertTrue("the trail keeps its parents", focus.trail().startsWith("Chart"));
    }

    @Test
    public void nonsenseIsRefusedRatherThanStored() {
        PhoneFocus focus = new PhoneFocus(chart());
        focus.toHouse(0);
        focus.toHouse(13);
        assertEquals(PhoneFocus.Level.CHART, focus.level());
        focus.toDegree(-1, 5);
        focus.toDegree(3, 31);
        assertEquals(PhoneFocus.Level.CHART, focus.level());
    }

    @Test
    public void resetGoesAllTheWayOutFromAnyDepth() {
        PhoneFocus focus = new PhoneFocus(chart());
        focus.toHouse(9);
        focus.toBody(2);
        focus.toDegree(8, 21);
        focus.reset();
        assertEquals(PhoneFocus.Level.CHART, focus.level());
        assertEquals(-1, focus.house());
        assertEquals(-1, focus.body());
        assertEquals(-1, focus.degree());
        assertEquals("Chart", focus.trail());
    }

    @Test
    public void everyLevelAsksTheEngineForSomethingDifferent() {
        com.zodiacomputing.ourania.gui.InterpretationService svc =
            com.zodiacomputing.ourania.gui.InterpretationService.getInstance();
        ChartFrame f = chart();
        PhoneFocus focus = new PhoneFocus(f);
        assertEquals("the whole chart has its own reading elsewhere", "", focus.reading(svc));

        focus.toHouse(1);
        String house = focus.reading(svc);
        focus.toBody(0);
        String planet = focus.reading(svc);
        focus.toDegree(0, 1);
        String degree = focus.reading(svc);

        assertFalse("a house says something", house.isEmpty());
        assertFalse("a planet says something", planet.isEmpty());
        assertNotEquals("and they are not the same text", house, planet);
        assertNotEquals(planet, degree);
    }

    @Test
    public void aHouseWithNoPlanetsSaysSoRatherThanShowingNothing() {
        ChartFrame f = chart();
        com.zodiacomputing.ourania.gui.InterpretationService svc =
            com.zodiacomputing.ourania.gui.InterpretationService.getInstance();
        boolean[] occupied = new boolean[13];
        for (int i = 0; i < PhoneWheel.PLANETS && i < f.bodies.length; i++) {
            if (f.bodies[i] != null && f.bodies[i].ok) {
                int h = PhoneFocus.houseOf(f, f.bodies[i].lon);
                if (h > 0) {
                    occupied[h] = true;
                }
            }
        }
        for (int h = 1; h <= 12; h++) {
            if (occupied[h]) {
                continue;
            }
            PhoneFocus focus = new PhoneFocus(f);
            focus.toHouse(h);
            assertTrue("an empty house says it is empty",
                focus.reading(svc).contains("No planet stands in this house"));
            return;
        }
    }
}
