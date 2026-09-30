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

    /** London, noon 1 Jan 1980: two grand trines and a T-square (Mercury, Moon, Saturn). */
    private static PhoneChart.Cast london1980() {
        Atlas.Place place = null;
        for (Atlas.Place p : Atlas.search("London", 8)) {
            if (p.country != null && p.country.contains("United Kingdom")) {
                place = p;
                break;
            }
        }
        assertNotNull(place);
        return PhoneChart.cast(new SwissEph(Ephemeris.PATH), LocalDate.of(1980, 1, 1),
            LocalTime.NOON, place);
    }

    @Test
    public void everyPatternTheEngineFindsIsReadWithItsMembers() {
        PhoneChart.Cast c = london1980();
        java.util.List<com.zodiacomputing.ourania.astro.AspectPatterns.Pattern> found =
            PhoneReading.patternsOf(c.frame);
        assertTrue("the chart has patterns", found.size() >= 2);
        String r = PhoneReading.patterns(c);
        assertEquals("one heading per pattern", found.size(), r.split("<h4>", -1).length - 1);
        for (com.zodiacomputing.ourania.astro.AspectPatterns.Pattern p : found) {
            assertTrue(p.name, r.contains("<h4>" + p.name));
            for (String b : p.bodies) {
                assertTrue(p.name + " names " + b, r.contains("<b>" + b + "</b>"));
            }
            String mechanics = SVC.getMacroDynamic(p.detailKey());
            assertTrue(p.name + " carries the desktop's reading of it",
                mechanics.isEmpty() || r.contains(mechanics));
        }
        assertFalse(r.contains("not found"));
    }

    @Test
    public void aPlanetInAPatternSaysSo() {
        PhoneChart.Cast c = london1980();
        int saturn = -1;
        for (int i = 0; i < PhoneChart.PLANETS; i++) {
            if (c.frame.bodies[i].name.equals("Saturn")) {
                saturn = i;
            }
        }
        String r = PhoneReading.planet(c, saturn, SVC);
        assertTrue(r.contains("<h3>In a pattern</h3>") && r.contains("<h4>T-square"));
        assertFalse("and Sydney's Sun, in none, does not",
            PhoneReading.planet(sydney(LocalTime.of(14, 15)), 0, SVC).contains("In a pattern"));
    }

    @Test
    public void aChartWithNoPatternsSaysNothing() {
        assertEquals("", PhoneReading.patterns(sydney(LocalTime.of(14, 15))));
    }

    @Test
    public void theWholeChartReadingIsTheDesktopsSynthesisWithoutItsColours() {
        PhoneChart.Cast c = sydney(LocalTime.of(14, 15));
        String r = PhoneReading.synthesis(c);
        assertTrue(r, r.startsWith("<h1>Chart Synthesis</h1>"));
        assertTrue("it ranks the planets", r.contains("Prominence"));
        assertFalse("no colours written for a black pane", r.contains("style=")
            || r.contains("<body") || r.contains("<html"));
        assertTrue("and is long", r.length() > 10000);
    }
}
