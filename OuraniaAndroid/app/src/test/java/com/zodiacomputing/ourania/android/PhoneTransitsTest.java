package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Moments;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;
import com.zodiacomputing.ourania.gui.WheelLayout;

import de.thmac.swisseph.SwissEph;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** The sky over a birth chart on the phone (M7), on the JVM. */
public class PhoneTransitsTest {

    private static final InterpretationService SVC = InterpretationService.getInstance();

    private static SwissEph sw() {
        return new SwissEph(Ephemeris.PATH);
    }

    private static PhoneChart.Cast sydney(LocalTime time) {
        for (Atlas.Place p : Atlas.search("Sydney", 8)) {
            if (p.country != null && p.country.contains("Australia")) {
                return PhoneChart.cast(sw(), LocalDate.of(1975, 7, 4), time, p);
            }
        }
        throw new AssertionError("Sydney");
    }

    @Test
    public void theSkyIsTheEnginesSkyAtThatMoment() {
        PhoneChart.Cast natal = sydney(LocalTime.of(14, 15));
        ZonedDateTime when = ZonedDateTime.parse("2026-09-30T12:00:00+10:00[Australia/Sydney]");
        PhoneTransits.Sky sky = PhoneTransits.at(sw(), natal, when);
        ChartFrame direct = ChartFrame.compute(sw(), Moments.sweDate(when).getJulDay(),
            natal.place.latitude, natal.place.longitude, PhoneChart.HOUSE_SYSTEM, false, 0.0);
        for (int i = 0; i < PhoneChart.PLANETS; i++) {
            assertEquals(direct.bodies[i].name, direct.bodies[i].lon, sky.frame.bodies[i].lon,
                1e-9);
        }
    }

    @Test
    public void atTheBirthMomentEveryPlanetMeetsItself() {
        // The sky at the birth is the birth chart: each prominent planet is conjunct itself,
        // exactly - the one transit whose answer is known without an ephemeris.
        PhoneChart.Cast natal = sydney(LocalTime.of(14, 15));
        PhoneTransits.Sky sky = PhoneTransits.at(sw(), natal, natal.moment.when);
        int selfContacts = 0;
        for (Transits.Hit h : sky.hits) {
            if (h.transiting.equals(h.natal) && h.type.label.equals("Conjunction")) {
                selfContacts++;
                assertEquals(h.transiting, 0.0, h.offBy, 1e-6);
            }
        }
        assertTrue("several planets conjunct themselves: " + selfContacts, selfContacts >= 3);
        assertTrue(PhoneTransits.reading(natal, sky, SVC)
            .contains("Transiting Sun conjunction natal Sun"));
    }

    @Test
    public void theYearIsProfectedFromTheAscendant() {
        PhoneChart.Cast natal = sydney(LocalTime.of(14, 15));
        // Thirty years and a week on: age 30, and 30 mod 12 = 6 houses on from the first.
        PhoneTransits.Sky sky = PhoneTransits.at(sw(), natal,
            natal.moment.when.plusYears(30).plusWeeks(1));
        assertNotNull(sky.year);
        assertEquals(30, sky.year.age);
        assertEquals(7, sky.year.house);
        assertTrue(PhoneTransits.reading(natal, sky, SVC).contains("a house 7 year"));
    }

    @Test
    public void withNoBirthTimeThereIsNoYearAndNoAngles() {
        PhoneChart.Cast natal = sydney(null);
        PhoneTransits.Sky sky = PhoneTransits.at(sw(), natal, natal.moment.when.plusYears(20));
        assertNull(sky.year);
        for (Transits.Hit h : sky.hits) {
            assertFalse(h.natal + " is an angle", "angle".equals(h.why));
        }
    }

    @Test
    public void aTappedSkyPlanetIsReadInTheNatalHouses() {
        PhoneChart.Cast natal = sydney(LocalTime.of(14, 15));
        PhoneTransits.Sky sky = PhoneTransits.at(sw(), natal,
            ZonedDateTime.parse("2026-09-30T12:00:00+10:00[Australia/Sydney]"));
        String saturn = PhoneTransits.skyPlanet(natal, sky, 6, SVC);
        assertTrue(saturn, saturn.startsWith("<h2>Transiting Saturn in "));
        assertTrue("its natal house", saturn.contains(", your house "));
        assertFalse(saturn.contains("not found"));
    }

    @Test
    public void theSkyRidesItsOwnRingAndIsTappedThere() {
        PhoneChart.Cast natal = sydney(LocalTime.of(14, 15));
        PhoneTransits.Sky sky = PhoneTransits.at(sw(), natal,
            ZonedDateTime.parse("2026-09-30T12:00:00+10:00[Australia/Sydney]"));
        PhoneWheel w = PhoneWheel.of(natal.frame, sky.frame, 1080, 1080);
        assertTrue("the natal band moves in to make room",
            w.natalTop < PhoneWheel.of(natal.frame, 1080, 1080).natalTop);
        for (int i = 0; i < PhoneChart.PLANETS; i++) {
            float[] p = w.skyBody(i);
            assertNotNull(p);
            double r = Math.hypot(p[0] - w.cx, p[1] - w.cy);
            assertTrue(i + " between the natal planets and the signs",
                r > w.natalTop && r <= w.rings[WheelLayout.RING_TRANSIT]);
            int hit = w.bodyAt(p[0], p[1], 0f);
            assertEquals("sky " + i, PhoneWheel.SKY + i, hit);
        }
    }
}
