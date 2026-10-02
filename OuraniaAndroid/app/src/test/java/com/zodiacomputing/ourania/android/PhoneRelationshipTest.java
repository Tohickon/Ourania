package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;

import de.thmac.swisseph.SwissEph;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Two charts on the phone (M10), on the JVM: synastry and the midpoint composite. */
public class PhoneRelationshipTest {

    private static final InterpretationService SVC = InterpretationService.getInstance();

    private static Atlas.Place place(String name, String country) {
        for (Atlas.Place p : Atlas.search(name, 8)) {
            if (p.country != null && p.country.contains(country)) {
                return p;
            }
        }
        throw new AssertionError(name);
    }

    private static ChartFrame jane() {
        return PhoneChart.cast(new SwissEph(Ephemeris.PATH), LocalDate.of(1975, 7, 4),
            LocalTime.of(14, 15), place("Sydney", "Australia")).frame;
    }

    private static ChartFrame john(LocalTime time) {
        return PhoneChart.cast(new SwissEph(Ephemeris.PATH), LocalDate.of(1980, 1, 1), time,
            place("London", "United Kingdom")).frame;
    }

    @Test
    public void contactsAreJudgedAtTheSynastryOrbsEitherWayRound() {
        ChartFrame a = jane();
        ChartFrame b = john(LocalTime.NOON);
        List<PhoneRelationship.Contact> ab = PhoneRelationship.contacts(a, b);
        List<PhoneRelationship.Contact> ba = PhoneRelationship.contacts(b, a);
        assertTrue("two charts have contacts", ab.size() > 5);
        assertEquals("the same contacts from either side", ab.size(), ba.size());
        double last = 0.0;
        for (PhoneRelationship.Contact c : ab) {
            double allowed = Aspects.orbFor(a.bodies[c.a].name, b.bodies[c.b].name,
                Aspects.Profile.SYNASTRY);
            assertTrue(a.bodies[c.a].name + "-" + b.bodies[c.b].name + " " + c.orb,
                c.orb <= allowed + 1e-9);
            assertTrue("tightest first", c.orb >= last);
            last = c.orb;
        }
    }

    @Test
    public void theSynastryPageReadsEveryContactAndBothWaysOfHouses() {
        ChartFrame a = jane();
        ChartFrame b = john(LocalTime.NOON);
        String r = PhoneRelationship.synastry("Jane", a, "John", b, SVC);
        assertTrue(r.startsWith("<h2>Jane and John</h2>"));
        for (PhoneRelationship.Contact c : PhoneRelationship.contacts(a, b)) {
            assertTrue(r.contains("Jane's " + a.bodies[c.a].name + " "
                + c.type.label.toLowerCase() + " John's " + b.bodies[c.b].name));
        }
        int sunInJohns = Zodiac.houseOf(a.bodies[0].lon, b.cusps);
        assertTrue("Jane's Sun in John's house " + sunInJohns,
            r.contains("<h3>Jane's planets in John's houses</h3>")
                && r.contains("<h4>Sun in house " + sunInJohns + "</h4>"));
        assertTrue(r.contains("<h3>John's planets in Jane's houses</h3>"));
        assertFalse(r.contains("not found"));
    }

    @Test
    public void withoutTheirBirthTimeNothingIsPlacedInTheirHouses() {
        String r = PhoneRelationship.synastry("Jane", jane(), "John", john(null), SVC);
        assertFalse(r.contains("in John's houses"));
        assertFalse(r.contains("on John's angles"));
        assertTrue("theirs still fall in hers", r.contains("John's planets in Jane's houses"));
    }

    @Test
    public void aPartnersPlanetIsReadInTheOtherChart() {
        ChartFrame a = jane();
        ChartFrame b = john(LocalTime.NOON);
        String venus = PhoneRelationship.partnerPlanet("Jane", a, "John", b, 3, SVC);
        int house = Zodiac.houseOf(b.bodies[3].lon, a.cusps);
        assertTrue(venus, venus.startsWith("<h2>John's Venus in ")
            && venus.contains("Jane's house " + house));
    }

    @Test
    public void theCompositeSunIsTheMidpointOfTheTwoSuns() {
        ChartFrame a = jane();
        ChartFrame b = john(LocalTime.NOON);
        ChartFrame c = PhoneRelationship.composite(new SwissEph(Ephemeris.PATH), a, b);
        double x = a.bodies[0].lon;
        double y = b.bodies[0].lon;
        double d = ((y - x) % 360.0 + 540.0) % 360.0 - 180.0;     // shorter way from x to y
        double mid = ((x + d / 2.0) % 360.0 + 360.0) % 360.0;
        assertEquals(mid, c.bodies[0].lon, 1e-6);
        String sun = PhoneRelationship.compositePlanet(c, 0, SVC);
        assertTrue(sun, sun.startsWith("<h2>The relationship's Sun in "));
        assertFalse(sun.contains("not found"));
    }

    @Test
    public void theCompositeIsReadAsARelationship() {
        ChartFrame c = PhoneRelationship.composite(new SwissEph(Ephemeris.PATH), jane(),
            john(LocalTime.NOON));
        String r = PhoneRelationship.compositeSynthesis(c);
        assertTrue(r.startsWith("<h1>Chart Synthesis</h1>"));
        assertTrue("in the relationship's voice", r.contains("the relationship's"));
        assertFalse(r.contains("style="));
    }

    @Test
    public void thePartnersPlanetsRideTheOuterRingAndAreTappedThere() {
        ChartFrame a = jane();
        ChartFrame b = john(LocalTime.NOON);
        PhoneWheel w = PhoneWheel.of(a, b, 1080, 1080);
        for (int i = 0; i < PhoneChart.PLANETS; i++) {
            float[] p = w.skyBody(i);
            assertEquals(b.bodies[i].name, PhoneWheel.SKY + i, w.bodyAt(p[0], p[1], 0f));
        }
        assertEquals(PhoneRelationship.contacts(a, b).size(),
            PhoneRelationship.crosses(PhoneRelationship.contacts(a, b)).size());
    }

    @Test
    public void theDavisonIsARealChartHalfwayBetweenTheBirths() {
        ChartFrame a = jane();
        ChartFrame b = john(LocalTime.NOON);
        ChartFrame d = PhoneRelationship.davison(new SwissEph(Ephemeris.PATH), a, b);
        assertEquals("halfway in time", (a.julianDayUt + b.julianDayUt) / 2.0, d.julianDayUt,
            1e-9);
        // A real moment, so its Sun is where the Sun was then - not the midpoint of two Suns.
        ChartFrame then = ChartFrame.compute(new SwissEph(Ephemeris.PATH), d.julianDayUt,
            d.geoLat, d.geoLon, PhoneChart.houseSystem(), false, 0.0);
        assertEquals(then.bodies[0].lon, d.bodies[0].lon, 1e-9);
        assertTrue("the lunar phase is real", d.phaseName != null);
    }

    @Test
    public void whereTheRelationshipLivesMovesTheHousesAndNotThePlanets() {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        try {
            ChartFrame a = jane();
            ChartFrame b = john(LocalTime.NOON);
            ChartFrame mid = PhoneRelationship.composite(new SwissEph(Ephemeris.PATH), a, b);
            assertEquals("the midpoint of the two birthplaces",
                PhoneRelationship.compositePlace());
            Atlas.Place oslo = place("Oslo", "Norway");
            com.zodiacomputing.ourania.gui.Settings.setCompositeReference(oslo.latitude,
                oslo.longitude, "Oslo, Norway");
            ChartFrame there = PhoneRelationship.composite(new SwissEph(Ephemeris.PATH), a, b);
            assertEquals("Oslo, Norway", PhoneRelationship.compositePlace());
            assertEquals("the planets stay", mid.bodies[0].lon, there.bodies[0].lon, 1e-9);
            assertTrue("the Ascendant moves: " + mid.asc + " vs " + there.asc,
                Math.abs(mid.asc - there.asc) > 1.0);
            com.zodiacomputing.ourania.gui.Settings.setCompositeReference(Double.NaN,
                Double.NaN, null);
            assertEquals(mid.asc, PhoneRelationship.composite(new SwissEph(Ephemeris.PATH), a,
                b).asc, 1e-9);
        } finally {
            com.zodiacomputing.ourania.gui.Settings.setCompositeReference(Double.NaN,
                Double.NaN, null);
        }
    }
}
