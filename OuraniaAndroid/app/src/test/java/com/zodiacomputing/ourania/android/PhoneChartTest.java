package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * The phone's whole path from a birth certificate to a list of placements, on the JVM (M3).
 *
 * <p>There is no emulator on the build machines, so this is where the phone's chart is proved:
 * the place comes out of the real atlas search the place box uses, the chart is cast by
 * {@link PhoneChart} exactly as the screen casts it, and the answers are held against the
 * desktop engine and against the sky. The data folder and the ephemeris come from the build's
 * test settings (app/build.gradle).
 */
public class PhoneChartTest {

    /** Sydney, 4 July 1975, 14:15 local - the chart the desktop's EphemerisFailureCheck uses. */
    private static final LocalDate SYDNEY_DAY = LocalDate.of(1975, 7, 4);
    private static final LocalTime SYDNEY_TIME = LocalTime.of(14, 15);

    private static Atlas.Place first(String query, String country) {
        List<Atlas.Place> found = Atlas.search(query, 8);
        assertNull("the atlas loaded: " + Atlas.failure(), Atlas.failure());
        for (Atlas.Place p : found) {
            if (p.country != null && p.country.contains(country)) {
                return p;
            }
        }
        throw new AssertionError(query + " in " + country + " not among " + found.size()
            + " suggestions");
    }

    private static void assertNull(String message, Object value) {
        org.junit.Assert.assertNull(message, value);
    }

    private static SwissEph ephemeris() {
        return new SwissEph(Ephemeris.PATH);
    }

    @Test
    public void thePlaceBoxFindsPlacesAsTheyAreTyped() {
        Atlas.Place london = first("Lond", "United Kingdom");
        assertTrue("London is suggested for \"Lond\": " + PhoneChart.label(london),
            PhoneChart.label(london).startsWith("London"));
        Atlas.Place sydney = first("Sydn", "Australia");
        assertEquals("Australia/Sydney", sydney.zoneId);
    }

    @Test
    public void aChartCastOnThePhoneIsTheDesktopEnginesChart() {
        Atlas.Place sydney = first("Sydney", "Australia");
        PhoneChart.Cast c = PhoneChart.cast(ephemeris(), SYDNEY_DAY, SYDNEY_TIME, sydney);

        // The same moment and place, cast directly - no Moments, no atlas, no PhoneChart.
        ZonedDateTime ut = ZonedDateTime.of(SYDNEY_DAY, SYDNEY_TIME, ZoneId.of("Australia/Sydney"))
            .withZoneSameInstant(ZoneOffset.UTC);
        double jd = new SweDate(ut.getYear(), ut.getMonthValue(), ut.getDayOfMonth(),
            ut.getHour() + ut.getMinute() / 60.0).getJulDay();
        ChartFrame direct = ChartFrame.compute(ephemeris(), jd, sydney.latitude, sydney.longitude,
            PhoneChart.houseSystem(), false, 0.0);
        for (int i = 0; i < PhoneChart.PLANETS; i++) {
            assertEquals(direct.bodies[i].name, direct.bodies[i].lon, c.frame.bodies[i].lon, 1e-9);
        }
        assertEquals("the Ascendant", direct.asc, c.frame.asc, 1e-9);

        // And the sky: the Sun at 12 Cancer, 101.593 with the Swiss Ephemeris files. Loose
        // enough for Moshier when the build has none.
        assertEquals("the Sun on 4 July 1975", 101.593, c.frame.bodies[0].lon, 0.01);
    }

    @Test
    public void theChartIsWrittenOutWithHousesAndReadings() {
        PhoneChart.Cast c = PhoneChart.cast(ephemeris(), SYDNEY_DAY, SYDNEY_TIME,
            first("Sydney", "Australia"));
        String text = PhoneChart.describe(c, InterpretationService.getInstance());
        for (String planet : new String[] {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter",
                "Saturn", "Uranus", "Neptune", "Pluto"}) {
            assertTrue(planet + " is listed", text.contains("\n" + planet + "  "));
        }
        assertTrue("with houses", text.contains("  house "));
        assertTrue("and the angles", text.contains("Ascendant  ") && text.contains("Midheaven  "));
        assertTrue("and each planet's reading under it - the Sun in Cancer",
            text.contains("Cancer") && text.split("\n   ").length > 5);
        assertTrue("where and when it was cast", text.startsWith("Sydney"));
    }

    @Test
    public void anUnknownTimeWithholdsTheAnglesAndHouses() {
        PhoneChart.Cast c = PhoneChart.cast(ephemeris(), SYDNEY_DAY, null,
            first("Sydney", "Australia"));
        assertTrue(c.frame.timeUnknown);
        String text = PhoneChart.describe(c, InterpretationService.getInstance());
        assertTrue("it says so", text.contains("time unknown"));
        assertNotNull(c.frame.timeUnknownNote);
        assertTrue("and why", text.contains(c.frame.timeUnknownNote));
        assertFalse("no houses", text.contains("  house "));
        assertFalse("no Ascendant", text.contains("Ascendant  "));
        assertTrue("the planets are still there", text.contains("\nSun  "));
    }

    @Test
    public void aTimeTheClocksSkippedIsNamed() {
        // 02:30 on 8 March 2026 did not happen in New York: the clocks went from 02:00 to 03:00.
        PhoneChart.Cast c = PhoneChart.cast(ephemeris(), LocalDate.of(2026, 3, 8),
            LocalTime.of(2, 30), first("New York", "United States"));
        String text = PhoneChart.describe(c, InterpretationService.getInstance());
        assertNotNull("the engine noticed", c.moment.note);
        assertTrue("and the reader is told, before the placements: " + c.moment.note,
            text.indexOf(c.moment.note) >= 0 && text.indexOf(c.moment.note) < text.indexOf("\nSun  "));
    }
}
