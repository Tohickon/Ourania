package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;

import de.thmac.swisseph.SwissEph;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

/** A tapped planet's full reading (M5), on the JVM, against the desktop's corpus. */
public class PhoneReadingTest {

    private static final InterpretationService SVC = InterpretationService.getInstance();

    private static PhoneChart.Cast sydney(LocalTime time) {
        Atlas.Place place = null;
        for (Atlas.Place p : Atlas.search("Sydney", 8)) {
            if (p.country != null && p.country.contains("Australia")) {
                place = p;
                break;
            }
        }
        assertNotNull(place);
        return PhoneChart.cast(new SwissEph(Ephemeris.PATH), LocalDate.of(1975, 7, 4), time, place);
    }

    @Test
    public void everyPlanetHasItsSignHouseAndAspectReadings() {
        PhoneChart.Cast c = sydney(LocalTime.of(14, 15));
        for (int i = 0; i < PhoneChart.PLANETS; i++) {
            String name = c.frame.bodies[i].name;
            String r = PhoneReading.planet(c, i, SVC);
            assertTrue(name + " is headed", r.startsWith("<h2>" + name + " in "));
            // The desktop's own paragraph for the sign, word for word.
            String sign = com.zodiacomputing.ourania.astro.Zodiac.signName(c.frame.bodies[i].lon);
            assertTrue(name + " sign text", r.contains(SVC.getPlanetInSign(name, sign)));
            assertTrue(name + " house", r.contains("<h3>In house "));
            assertFalse(name + ": no placeholder reaches the reader", r.contains("not found")
                || r.contains("Add to JSON"));
        }
    }

    @Test
    public void theSunsAspectsAreTheWheelsAspects() {
        PhoneChart.Cast c = sydney(LocalTime.of(14, 15));
        String r = PhoneReading.planet(c, 0, SVC);
        int expected = 0;
        for (PhoneWheel.Line l : PhoneWheel.aspectsOf(c.frame, PhoneChart.PLANETS)) {
            if (l.a == 0 || l.b == 0) {
                expected++;
                String other = c.frame.bodies[l.a == 0 ? l.b : l.a].name;
                assertTrue("the Sun's " + l.type.label + " to " + other + " is read",
                    r.contains(l.type.label + " " + other));
            }
        }
        assertEquals("one heading per aspect", expected, r.split("<h4>", -1).length - 1);
    }

    @Test
    public void withNoBirthTimeTheHouseIsLeftOutAndSaidWhy() {
        String r = PhoneReading.planet(sydney(null), 0, SVC);
        assertFalse(r.contains("<h3>In house "));
        assertTrue(r.contains("depends on the birth time"));
    }

    @Test
    public void applyingAndSeparatingFollowTheSpeeds() {
        com.zodiacomputing.ourania.astro.ChartFrame.Body fast =
            new com.zodiacomputing.ourania.astro.ChartFrame.Body();
        com.zodiacomputing.ourania.astro.ChartFrame.Body still =
            new com.zodiacomputing.ourania.astro.ChartFrame.Body();
        com.zodiacomputing.ourania.astro.Aspects.Type square = null;
        for (com.zodiacomputing.ourania.astro.Aspects.Type t
                : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            if (t.label.equals("Square")) {
                square = t;
            }
        }
        still.lon = 100.0;
        fast.lon = 188.0;                         // 88 degrees on: two short of the square
        fast.lonSpeed = 13.0;                     // moving away from the other, toward 90
        assertEquals("Applying ", PhoneReading.state(fast, still, square));
        assertEquals(2.0, PhoneReading.orb(fast, still, square), 1e-9);
        fast.lonSpeed = -13.0;
        assertEquals("Separating ", PhoneReading.state(fast, still, square));
    }

    @Test
    public void nothingForNoPlanet() {
        assertEquals("", PhoneReading.planet(sydney(LocalTime.of(14, 15)), -1, SVC));
    }
}
