package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.Settings;

import de.thmac.swisseph.SwissEph;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Properties;

/**
 * The phone's settings (M9), on the JVM: each one read and written under the desktop's key, and
 * each one changing the chart the way it changes the desktop's. Every test starts from a fresh
 * settings file - the redirect the desktop's own check suites use - and leaves the defaults.
 */
public class PhoneSettingsTest {

    @Before
    public void aFreshInstall() {
        Settings.useScratchFile();
        Settings.applyToEngine();
    }

    @After
    public void backToTheDefaults() {
        PhoneSettings.reset();
    }

    private static PhoneChart.Cast sydney() {
        for (Atlas.Place p : Atlas.search("Sydney", 8)) {
            if (p.country != null && p.country.contains("Australia")) {
                return PhoneChart.cast(new SwissEph(Ephemeris.PATH), LocalDate.of(1975, 7, 4),
                    LocalTime.of(14, 15), p);
            }
        }
        throw new AssertionError("Sydney");
    }

    private static Properties file() throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(System.getProperty("ourania.settings"))) {
            p.load(in);
        }
        return p;
    }

    @Test
    public void aFreshInstallHasTheDesktopsDefaults() {
        PhoneSettings.Values v = PhoneSettings.read();
        assertEquals("Placidus", v.houseSystem);
        assertEquals("Tropical", v.zodiac);
        assertEquals(Transits.DEFAULT_ORB, v.transitOrb, 0.0);
        for (int i = 0; i < v.orbs.length; i++) {
            assertEquals(PhoneSettings.planet(i),
                Aspects.defaultBodyOrb(PhoneSettings.planet(i), Aspects.Profile.NATAL),
                v.orbs[i], 1e-9);
        }
        for (boolean on : v.aspects) {
            assertTrue("every aspect drawn", on);
        }
    }

    @Test
    public void wholeSignHousesStartEachHouseAtASign() throws Exception {
        PhoneSettings.Values v = PhoneSettings.read();
        v.houseSystem = "Whole Sign";
        PhoneSettings.save(v);
        assertEquals("under the desktop's key", "Whole Sign",
            file().getProperty(Settings.HOUSE_SYSTEM_KEY));
        assertEquals('W', PhoneChart.houseSystem());
        ChartFrame f = sydney().frame;
        for (int h = 1; h <= 12; h++) {
            assertEquals("cusp " + h + " on a sign boundary", 0.0, f.cusps[h] % 30.0, 1e-6);
        }
    }

    @Test
    public void aSiderealZodiacMovesEveryPlanetBackByTheAyanamsa() {
        double tropicalSun = sydney().frame.bodies[0].lon;
        PhoneSettings.Values v = PhoneSettings.read();
        v.zodiac = "Sidereal (Lahiri)";
        PhoneSettings.save(v);
        double siderealSun = sydney().frame.bodies[0].lon;
        // Lahiri's ayanamsa in 1975 was a little over 23.5 degrees.
        assertEquals(23.5, tropicalSun - siderealSun, 0.3);
    }

    @Test
    public void onlyAChangedOrbIsWritten() throws Exception {
        PhoneSettings.Values v = PhoneSettings.read();
        v.orbs[0] = 3.0;                                     // the Sun
        PhoneSettings.save(v);
        assertEquals(3.0, PhoneSettings.read().orbs[0], 1e-9);
        assertEquals(3.0, Aspects.orbFor("Sun", "Sun", Aspects.Profile.NATAL), 1e-9);
        Properties p = file();
        assertEquals("3.0", p.getProperty("orb.body.sun"));
        String moon = p.getProperty("orb.body.moon");
        assertTrue("the Moon's orb is left to the engine: " + moon,
            moon == null || moon.isEmpty());
    }

    @Test
    public void anAspectSwitchedOffIsNeitherDrawnNorRead() {
        PhoneChart.Cast c = sydney();
        boolean sawSextile = false;
        for (PhoneWheel.Line l : PhoneWheel.aspectsOf(c.frame, PhoneChart.PLANETS)) {
            sawSextile |= l.type.label.equals("Sextile");
        }
        assertTrue("the chart has a sextile to switch off", sawSextile);
        PhoneSettings.Values v = PhoneSettings.read();
        for (Aspects.Type t : Aspects.Type.values()) {
            if (t.label.equals("Sextile")) {
                v.aspects[t.ordinal()] = false;
            }
        }
        PhoneSettings.save(v);
        for (PhoneWheel.Line l : PhoneWheel.aspectsOf(c.frame, PhoneChart.PLANETS)) {
            assertFalse(l.type.label.equals("Sextile"));
        }
        assertFalse(PhoneReading.planet(c, 0, com.zodiacomputing.ourania.gui.InterpretationService
            .getInstance()).contains("Sextile Moon"));
    }

    @Test
    public void aWiderTransitOrbFindsMoreTransits() {
        PhoneChart.Cast c = sydney();
        ZonedDateTime when = ZonedDateTime.parse("2026-09-30T12:00:00+10:00[Australia/Sydney]");
        int tight = PhoneTransits.at(new SwissEph(Ephemeris.PATH), c, when).hits.size();
        PhoneSettings.Values v = PhoneSettings.read();
        v.transitOrb = 3.0;
        PhoneSettings.save(v);
        assertEquals(3.0, Transits.orb, 0.0);
        int wide = PhoneTransits.at(new SwissEph(Ephemeris.PATH), c, when).hits.size();
        assertTrue(tight + " then " + wide, wide > tight);
    }

    @Test
    public void resetPutsEverythingBack() {
        PhoneSettings.Values v = PhoneSettings.read();
        v.houseSystem = "Koch";
        v.zodiac = "Sidereal (Raman)";
        v.transitOrb = 2.0;
        v.orbs[1] = 4.0;
        java.util.Arrays.fill(v.aspects, false);
        PhoneSettings.save(v);
        PhoneSettings.reset();
        aFreshInstallHasTheDesktopsDefaults();
        assertEquals('P', PhoneChart.houseSystem());
    }

    @Test
    public void theMansionsAreOffUntilAskedForAndOpenAtTheRimNotInThePlanets() {
        assertFalse("off on a fresh install", PhoneSettings.read().wheelMansions);
        PhoneWheel shut = PhoneWheel.of(sydney().frame, 1080, 1080);
        assertEquals("shut, the band has no depth",
            shut.rings[com.zodiacomputing.ourania.gui.WheelLayout.RING_OUTER],
            shut.rings[com.zodiacomputing.ourania.gui.WheelLayout.RING_MANSION_INNER]);
        org.junit.Assert.assertArrayEquals("and the wheel is the layout it always was",
            com.zodiacomputing.ourania.gui.WheelLayout.ringRadii(1080, 1080, false),
            shut.rings);

        PhoneSettings.Values v = PhoneSettings.read();
        v.wheelMansions = true;
        PhoneSettings.save(v);
        assertTrue("saved", PhoneSettings.read().wheelMansions);
        PhoneWheel open = PhoneWheel.of(sydney().frame, 1080, 1080);
        int outer = open.rings[com.zodiacomputing.ourania.gui.WheelLayout.RING_OUTER];
        int band = open.rings[com.zodiacomputing.ourania.gui.WheelLayout.RING_MANSION_INNER];
        assertTrue("open, the band is carved out of the rim",
            band < outer && band > open.rings[com.zodiacomputing.ourania.gui.WheelLayout.RING_DECAN_OUTER]);
        assertEquals("and the planets do not move", shut.natalTop, open.natalTop);
        assertEquals(shut.natalFloor, open.natalFloor);

        PhoneSettings.reset();
        assertFalse("reset puts them away", PhoneSettings.read().wheelMansions);
    }
}
