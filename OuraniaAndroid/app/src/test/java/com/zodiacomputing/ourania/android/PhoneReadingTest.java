package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;
import com.zodiacomputing.ourania.gui.Settings;

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

    // ------------------------------------------------------------ the reading's four levels

    @Test
    public void theReadingKeepsItsFourLevelsOnThePhone() {
        // <b>The tiers and the mechanics sections are both h2 on the desktop</b>, told apart
        // there by colour and size - and the phone strips colour, because its page follows the
        // device's light or dark setting. So the levels have to be re-expressed as sizes, which
        // is the one structural lever Html.fromHtml offers.
        String r = PhoneReading.synthesis(sydney(LocalTime.of(14, 15)));

        assertTrue("the title is the top level", r.startsWith("<h1>Chart Synthesis</h1>"));
        assertEquals("exactly one h1 - the title", 1, count(r, "<h1>"));

        // the four tiers, one level under it
        for (String tier : new String[] {"1. At a glance", "2. The year by theme",
                                         "3. Dates to watch", "4. The mechanics"}) {
            assertTrue("tier is an h2: " + tier, r.contains("<h2>" + tier));
        }
        assertEquals("four tiers and no more", 4, count(r, "<h2>"));

        // the mechanics' sections, one level under the tiers
        assertTrue("a section is an h3", r.contains("<h3>4.1 Core Architecture"));
        assertTrue("and the last one too", r.contains("<h3>4.10 Current Transits"));
        assertFalse("no section is left at h2", r.contains("<h2>4.1"));
    }

    @Test
    public void aSectionsOwnSubHeadingsSitUnderIt() {
        // A body's name inside the placements section was an h3 on the desktop - the same level
        // the sections themselves now take on the phone. Pushed to h4 so it stays underneath.
        String r = PhoneReading.synthesis(sydney(LocalTime.of(14, 15)));
        int section = r.indexOf("<h3>4.2 Planetary Placements");
        assertTrue("the placements section is there", section > 0);
        int sub = r.indexOf("<h4>", section);
        assertTrue("and a body's name under it is an h4", sub > section);
        assertTrue("which is a planet: " + r.substring(sub, Math.min(sub + 40, r.length())),
            r.substring(sub, Math.min(sub + 40, r.length())).contains("Prominence")
                || r.substring(sub, Math.min(sub + 60, r.length())).contains("%"));
    }

    @Test
    public void everyHeadingIsPushedDownAndNothingIsPushedTwice() {
        // <b>The order of the rewrites is the whole trick.</b> Demote h2 before h3 and a
        // section lands at h4 with the sub-headings; tokenise the tiers after demoting and
        // they land at h3 with the sections. Either way the levels collapse and the page looks
        // plausible, which is why this asserts the counts rather than eyeballing one heading.
        String r = PhoneReading.synthesis(sydney(LocalTime.of(14, 15)));
        assertEquals("nothing is left at h5", 0, count(r, "<h5>"));
        assertTrue("there are sections", count(r, "<h3>") >= 8);
        assertTrue("and sub-headings beneath them", count(r, "<h4>") > 0);
        assertTrue("more sub-headings than sections, since each section has several",
            count(r, "<h4>") > count(r, "<h3>"));
    }

    @Test
    public void noClassSurvivesToConfuseTheRenderer() {
        // class='tier' is how the tiers are found; Html.fromHtml does nothing with it, so it
        // is gone by the time the phone sees the reading.
        String r = PhoneReading.synthesis(sydney(LocalTime.of(14, 15)));
        assertFalse("no class attribute", r.contains("class="));
        assertFalse("and no sentinel leaked", r.contains("TIER"));
    }

    // ------------------------------------------------------------ the mechanics tier

    @Test
    public void theMechanicsTierFollowsTheReadersSetting() {
        PhoneChart.Cast c = sydney(LocalTime.of(14, 15));
        boolean kept = Settings.readingMechanics();
        try {
            Settings.setReadingMechanics(true);
            String full = PhoneReading.synthesis(c);
            Settings.setReadingMechanics(false);
            String lean = PhoneReading.synthesis(c);

            assertTrue("the full reading carries the sections", full.contains("4.1 Core"));
            assertFalse("the lean one does not", lean.contains("4.1 Core"));
            assertTrue("and is much shorter: " + lean.length() + " vs " + full.length(),
                lean.length() * 4 < full.length());
            // It must still SAY the tier exists, or a reader who forgot the setting sees a
            // reading that looks like it ran out.
            assertTrue("the tier is still named", lean.contains("<h2>4. The mechanics"));
            assertTrue("and says how to get it back", lean.contains("Show the mechanics"));
            // The tiers above it are untouched either way - the reader chooses whether to be
            // handed the working, not which answer they get.
            int fullMech = full.indexOf("4. The mechanics");
            int leanMech = lean.indexOf("4. The mechanics");
            assertEquals("tiers 1 to 3 are identical", full.substring(0, fullMech),
                lean.substring(0, leanMech));
        } finally {
            Settings.setReadingMechanics(kept);
        }
    }

    @Test
    public void theSettingsScreenCarriesTheToggle() {
        boolean kept = Settings.readingMechanics();
        try {
            PhoneSettings.Values v = PhoneSettings.read();
            assertEquals("read from the shared setting", kept, v.readingMechanics);
            v.readingMechanics = !kept;
            PhoneSettings.save(v);
            assertEquals("and saved to it", !kept, Settings.readingMechanics());
            PhoneSettings.reset();
            assertTrue("reset gives the whole reading back", Settings.readingMechanics());
        } finally {
            Settings.setReadingMechanics(kept);
        }
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
