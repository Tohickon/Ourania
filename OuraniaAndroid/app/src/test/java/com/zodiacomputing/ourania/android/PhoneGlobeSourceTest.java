package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.AngleRole;
import com.zodiacomputing.ourania.gui.AspectGate;
import com.zodiacomputing.ourania.gui.Globe;
import com.zodiacomputing.ourania.gui.GlobeRenderer;
import com.zodiacomputing.ourania.gui.Ink;
import com.zodiacomputing.ourania.gui.Layer;
import com.zodiacomputing.ourania.gui.Pen;
import com.zodiacomputing.ourania.gui.Settings;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import org.junit.Test;


/**
 * The phone's answers to the globe's questions (M11): {@link PhoneGlobeSource}.
 *
 * <p><b>What this can reach, and it is more than a getter sweep.</b> {@link GlobeRenderer} is
 * the desktop's, it is in the phone's build since stage 4, and it draws through a {@link Pen} -
 * which is an interface with no Android in it. So the real renderer can be driven here, on the
 * JVM, with a pen that counts what it is asked to draw. That proves the contract end to end:
 * not that the thirty-one methods return something, but that the renderer can actually paint a
 * globe through them without reaching for anything the phone has not got.
 *
 * <p>The rest is the invariants a wrong answer would break silently - two rings on one deck, a
 * sky planet lighting with its natal twin, the chord cache rebuilt per frame, and the aspect
 * gate being the shared one rather than a second rule.
 */
public class PhoneGlobeSourceTest {

    private static final int SIZE = 600;

    /** A pen that draws nothing and remembers being asked. */
    private static final class Tally implements Pen {
        int calls;
        int strings;
        int inks;

        @Override public void color(Ink ink) {
            this.inks++;
            assertNotNull("a null ink would paint nothing and say nothing", ink);
        }

        @Override public void stroke(float width) { }

        @Override public void font(int points) { }

        @Override public int textWidth(String text, int points) {
            return text == null ? 0 : text.length() * points / 2;
        }

        @Override public void drawString(String text, int x, int y) {
            this.strings++;
            this.calls++;
        }

        @Override public void drawLine(int x1, int y1, int x2, int y2) { this.calls++; }

        @Override public void drawPolyline(int[] xs, int[] ys, int count) { this.calls++; }

        @Override public void fillPolygon(int[] xs, int[] ys, int count) { this.calls++; }

        @Override public void fillRect(int x, int y, int w, int h) { this.calls++; }

        @Override public void fillOval(int x, int y, int w, int h) { this.calls++; }

        @Override public void drawOval(int x, int y, int w, int h) { this.calls++; }

        @Override public void drawArc(int x, int y, int w, int h, int start, int extent) {
            this.calls++;
        }

        @Override public void fillOvalRadial(int x, int y, int w, int h, float cx, float cy,
                float radius, float[] stops, Ink[] inks) {
            this.calls++;
        }

        @Override public void clipOval(float x, float y, float w, float h) { }

        @Override public void unclip() { }
    }

    /**
     * A real cast chart.
     *
     * <p>Cast straight through {@link ChartFrame} rather than through the atlas and
     * {@code PhoneChart}, which is how {@code PhoneWheelTest} does it. That test is partly
     * about place handling, so the gazetteer is the point there; nothing here is, and a test
     * that needs a data file on disk to answer a question about deck numbering fails for
     * reasons that have nothing to do with the thing it guards.
     */
    private static ChartFrame chart() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1975, 7, 4, 14.5).getJulDay();
        return ChartFrame.compute(sw, jd, -33.8688, 151.2093, 'P', false, 0.0);
    }

    // ------------------------------------------------------------ the contract, end to end

    @Test
    public void theDesktopsRendererPaintsAGlobeThroughThePhonesAnswers() {
        PhoneGlobeSource source = PhoneGlobeSource.of(chart());
        Tally pen = new Tally();
        GlobeRenderer.paint(pen, new Globe(), SIZE, SIZE, source, false);
        // A globe is rings, houses, a sign band and a planet per body: thousands of calls. The
        // number is a floor, not a measurement - what it rules out is a renderer that threw
        // early or drew nothing, which is what every missing answer would look like.
        assertTrue("the renderer drew something: " + pen.calls, pen.calls > 500);
        assertTrue("and some of it was glyphs: " + pen.strings, pen.strings > 0);
        assertTrue("and it asked for colours: " + pen.inks, pen.inks > 100);
    }

    @Test
    public void itPaintsWithASkyOnTheOuterRingToo() {
        ChartFrame natal = chart();
        PhoneGlobeSource source = PhoneGlobeSource.of(natal, natal);
        Tally pen = new Tally();
        GlobeRenderer.paint(pen, new Globe(), SIZE, SIZE, source, false);
        assertTrue("two rings drew: " + pen.calls, pen.calls > 500);
        assertTrue(source.outerRingDrawn());
    }

    @Test
    public void itPaintsWhileTurning() {
        // The turning pass takes different branches - fewer labels, no bowed chords - so a
        // missing answer can hide in it.
        PhoneGlobeSource source = PhoneGlobeSource.of(chart());
        Tally pen = new Tally();
        GlobeRenderer.paint(pen, new Globe(), SIZE, SIZE, source, true);
        assertTrue("the turning globe drew: " + pen.calls, pen.calls > 200);
    }

    // ------------------------------------------------------------ the decks

    @Test
    public void twoRingsNeverLandOnOneDeck() {
        // The desktop's rule, and the reason it is a rule: two wheels on one deck are drawn at
        // one radius in one plane, and the reader sees a single ring holding two charts.
        ChartFrame natal = chart();
        for (boolean withSky : new boolean[] {false, true}) {
            PhoneGlobeSource s = withSky
                ? PhoneGlobeSource.of(natal, natal) : PhoneGlobeSource.of(natal);
            int chartDeck = s.ringDeck(0);
            if (!s.outerRingDrawn()) {
                continue;
            }
            assertFalse("the chart and the sky share deck " + chartDeck,
                chartDeck == s.ringDeck(1));
        }
    }

    @Test
    public void aLoneChartTakesTheMiddleRatherThanFloatingAboveNothing() {
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        assertEquals(GlobeRenderer.DECK_MIDDLE, s.ringDeck(0));
    }

    @Test
    public void withASkyShownTheSkyOwnsTheMiddleAndTheChartRidesAbove() {
        ChartFrame natal = chart();
        PhoneGlobeSource s = PhoneGlobeSource.of(natal, natal);
        assertEquals(GlobeRenderer.DECK_MIDDLE, s.ringDeck(1));
        assertEquals(GlobeRenderer.DECK_UPPER, s.ringDeck(0));
    }

    // ------------------------------------------------------------ the rings

    @Test
    public void theChartIsRingZeroAndTheSkyIsRingOne() {
        ChartFrame natal = chart();
        PhoneGlobeSource s = PhoneGlobeSource.of(natal, natal);
        assertEquals(Bodies.count(), s.ringLon(0).length);
        assertEquals(Bodies.count(), s.ringValid(0).length);
        assertEquals(Bodies.count(), s.ringLon(1).length);
        int sun = Bodies.indexOfName("Sun");
        assertTrue("the Sun computed", s.ringValid(0)[sun]);
        assertEquals(natal.bodies[sun].lon, s.ringLon(0)[sun], 1e-9);
    }

    @Test
    public void aRingThatIsNotThereIsEmptyRatherThanNull() {
        // The renderer reads a ring's arrays once per frame and guards on their length. Null
        // would be a crash on the frame a reader switches the sky off; a length-zero array is
        // the honest "there is nothing here".
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        assertNotNull(s.ringLon(1));
        assertNotNull(s.ringValid(1));
        assertEquals(0, s.ringLon(1).length);
        assertEquals(0, s.ringValid(2).length);
        assertFalse(s.outerRingDrawn());
        assertFalse(s.triRingDrawn());
    }

    @Test
    public void thePinIsTheAscendant() {
        ChartFrame natal = chart();
        PhoneGlobeSource s = PhoneGlobeSource.of(natal);
        assertEquals(natal.asc, s.pinLongitude(), 1e-9);
        assertEquals(natal.cusps, s.activeCusps());
    }

    // ------------------------------------------------------------ focus

    @Test
    public void anUntouchedGlobeLightsNothing() {
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        assertEquals(-1, s.focusedBody());
        assertEquals(-1, s.focusedHouse());
        assertEquals(-1, s.focusedMansion());
        assertEquals(-1, s.focusedDegree());
        assertTrue("no focused longitude", Double.isNaN(s.focusedLongitude()));
        assertFalse(s.onGlobeFocus(Bodies.indexOfName("Sun"), false));
    }

    @Test
    public void aTappedBodyLightsItselfAndNotItsSkyTwin() {
        // The same planet on the sky's ring is a different thing - a moment, not the birth -
        // and lighting both would tell the reader they are one.
        ChartFrame natal = chart();
        PhoneGlobeSource s = PhoneGlobeSource.of(natal, natal);
        int sun = Bodies.indexOfName("Sun");
        assertTrue("the first tap is a change", s.focusBody(sun));
        assertFalse("tapping the same body twice is not", s.focusBody(sun));
        assertTrue(s.onGlobeFocus(sun, false));
        assertFalse("the sky's Sun must not light with the natal one",
            s.onGlobeFocus(sun, true));
        assertEquals(natal.bodies[sun].lon, s.focusedLongitude(), 1e-9);
    }

    @Test
    public void aTappedBodyLightsItsOwnChords() {
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        int sun = Bodies.indexOfName("Sun");
        int moon = Bodies.indexOfName("Moon");
        int mars = Bodies.indexOfName("Mars");
        assertFalse("nothing lit before a tap", s.lightsChord(sun, moon, 0));
        s.focusBody(sun);
        assertTrue(s.lightsChord(sun, moon, 0));
        assertTrue("either end", s.lightsChord(moon, sun, 0));
        assertFalse("and not a chord it is not on", s.lightsChord(moon, mars, 0));
    }

    // ------------------------------------------------------------ the chord cache

    @Test
    public void theChordsAreBuiltOnceAndKept() {
        // "Computed once per chart rather than once per frame" - on a phone that is the
        // difference between a globe that turns and one that stutters.
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        int[] built = new int[1];
        int[][] first = s.globeChords(() -> {
            built[0]++;
            return new int[][] {{1, 2, 3}};
        });
        int[][] again = s.globeChords(() -> {
            built[0]++;
            return new int[][] {{9, 9, 9}};
        });
        assertEquals("built once", 1, built[0]);
        assertEquals("and the same array came back", first, again);

        s.chartChanged();
        s.globeChords(() -> {
            built[0]++;
            return new int[][] {{4, 5, 6}};
        });
        assertEquals("and again after the chart moved", 2, built[0]);
    }

    // ------------------------------------------------------------ the aspect gate

    @Test
    public void aPairOutOfAspectGetsNoChord() {
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        int sun = Bodies.indexOfName("Sun");
        int moon = Bodies.indexOfName("Moon");
        // <b>The gap is FOUND, not assumed, and the first version of this assumed wrong.</b>
        // It used 100 degrees as "no aspect", which is 2.86 off the biseptile at 360/3.5 - well
        // inside the Sun and Moon's orb for it - so the phone was right and the test was not.
        // Fifteen aspects including septiles and noviles leave narrow gaps, and where they fall
        // is the engine's business rather than something to hardcode here.
        boolean[] shown = Settings.loadAspectSelection();
        double gap = Double.NaN;
        for (double sep = 0.0; sep <= 180.0; sep += 0.1) {
            if (AspectGate.visible(sep, sun, moon, Aspects.Profile.NATAL, shown) == null) {
                gap = sep;
                break;
            }
        }
        assertFalse("there is some separation the Sun and Moon make no aspect at",
            Double.isNaN(gap));
        assertNull("no aspect at " + gap + " degrees, so no chord",
            s.globeAspectInk(0.0, gap, sun, moon, false));
    }

    @Test
    public void aConjunctionGetsOne() {
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        int sun = Bodies.indexOfName("Sun");
        int moon = Bodies.indexOfName("Moon");
        assertNotNull(s.globeAspectInk(10.0, 10.2, sun, moon, false));
    }

    @Test
    public void thePhoneUsesTheSharedGateRatherThanARuleOfItsOwn() {
        // <b>This is the assertion that matters.</b> Whether a chord is drawn must be the
        // desktop's answer, not a second one that happens to agree today. Asked by putting the
        // two points in a real aspect and checking the phone agrees with AspectGate directly -
        // including the aspect-mode half, which decides whether an asteroid pair draws at all.
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        boolean[] shown = Settings.loadAspectSelection();
        String mode = Settings.aspectMode();
        int sun = Bodies.indexOfName("Sun");
        int moon = Bodies.indexOfName("Moon");
        int pallas = Bodies.indexOfName("Pallas");
        int juno = Bodies.indexOfName("Juno");
        int checked = 0;
        for (int[] pair : new int[][] {{sun, moon}, {pallas, juno}, {sun, pallas}}) {
            if (pair[0] < 0 || pair[1] < 0) {
                continue;
            }
            for (double sep : new double[] {0.5, 60.0, 90.0, 100.0, 120.0, 180.0}) {
                Aspects.Type type = AspectGate.visible(sep, pair[0], pair[1],
                    Aspects.Profile.NATAL, shown);
                boolean gateDraws = AspectGate.draws(pair[0], pair[1], mode)&& type != null;
                Ink ink = s.globeAspectInk(0.0, sep, pair[0], pair[1], false);
                assertEquals("pair " + pair[0] + "/" + pair[1] + " at " + sep + " degrees",
                    gateDraws, ink != null);
                checked++;
            }
        }
        assertTrue("enough pairs to mean anything: " + checked, checked >= 12);
    }

    // ------------------------------------------------------------ layers and colour

    @Test
    public void everyLayerAnswersAndTheAnswersAgreeWithEachOther() {
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        for (Layer layer : Layer.values()) {
            double open = s.layerOpen(layer);
            assertTrue(layer + " opens between 0 and 1: " + open, open >= 0.0 && open <= 1.0);
            // layerShown is defined in terms of layerOpen, and the renderer reads both - one
            // to skip a layer, the other to fade it. Disagreeing would draw a layer at zero
            // alpha, which costs a frame's work to paint nothing.
            assertEquals(layer + " shown and open agree", open > 0.004, s.layerShown(layer));
        }
    }

    @Test
    public void everyColourQuestionAnswersWithAnInkRatherThanNull() {
        // The renderer hands these straight to the pen. A null would paint nothing and say
        // nothing, which is the failure mode a glance at the globe would not catch.
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        for (AngleRole role : AngleRole.values()) {
            assertNotNull("bead " + role, s.beadInk(role));
            assertNotNull("ring ink " + role, s.globeRingInk(Bodies.indexOfName("Sun"), role));
        }
        for (int e = 0; e < 4; e++) {
            assertNotNull("element " + e, s.elementInk(e));
        }
        for (int i = 0; i < Bodies.count(); i++) {
            assertNotNull("body " + i, s.bodyInk(i));
        }
        assertNotNull(s.mansionInk());
        assertNotNull(s.leaderInk());
    }

    @Test
    public void nothingThePhoneDrawsIsABridge() {
        // A BRIDGE is Chart B's angle visiting Chart A's houses, and the phone has no Chart B.
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        assertEquals(AngleRole.ANCHOR, s.angleRoleFor(false, false));
        assertEquals(AngleRole.SKY, s.angleRoleFor(true, false));
        assertEquals(AngleRole.SKY, s.angleRoleFor(false, true));
    }

    @Test
    public void theMoonGetsAMansion() {
        assertNotNull(PhoneGlobeSource.of(chart()).moonMansion());
    }

    // ------------------------------------------------------- the tap, which GlobeView relies on

    /**
     * A tap on the globe finds planets, and every hit decodes to a real one.
     *
     * <p><b>This is the path {@code GlobeView} is made of</b>, and all of it except the
     * MotionEvent runs here: {@code GlobeRenderer.bodyAt} against the same camera and source
     * the painter used, then {@code WheelLayout.hitBody} to unpack what it returns. Both are
     * the desktop's, which is the point - the alternative was the phone writing a second hit
     * test, and {@code bodyAt}'s own comment says why that would be worse here than anywhere
     * else: on a globe, a reader who taps and gets the wrong planet assumes they missed.
     *
     * <p>It sweeps a grid rather than aiming at a known pixel, because where a planet lands
     * depends on the camera, the shells and the chart, and a test that hard-coded a coordinate
     * would be asserting today's arithmetic rather than the contract.
     */
    @Test
    public void aTapOnTheGlobeSelectsThePlanetUnderIt() {
        PhoneGlobeSource source = PhoneGlobeSource.of(chart());
        Globe cam = new Globe();
        java.util.Set<Integer> found = new java.util.TreeSet<>();
        for (int px = 0; px < SIZE; px += 4) {
            for (int py = 0; py < SIZE; py += 4) {
                int hit = GlobeRenderer.bodyAt(cam, SIZE, SIZE, source, px, py);
                if (hit < 0) {
                    continue;
                }
                int body = com.zodiacomputing.ourania.gui.WheelLayout.hitBody(hit);
                assertTrue("a hit decoded to body " + body + ", which is not a body",
                    body >= 0 && body < Bodies.count());
                found.add(body);
            }
        }
        // Ten planets are drawn and the camera starts nearly edge-on, so the far side of the
        // globe hides some of them. Several is the claim; all ten would be asserting the
        // camera's opening angle, which is Globe's business and changes when it is retuned.
        assertTrue("a swept tap found only " + found.size() + " bodies: " + found,
            found.size() >= 3);
    }

    @Test
    public void focusingABodyChangesWhatTheRendererIsTold() {
        PhoneGlobeSource s = PhoneGlobeSource.of(chart());
        assertEquals("nothing is focused until a tap lands", -1, s.focusedBody());
        assertTrue("focusing a new body asks for a redraw", s.focusBody(3));
        assertFalse("focusing the same body again does not", s.focusBody(3));
        assertEquals(3, s.focusedBody());
        assertTrue("and the renderer lights it on the chart's own ring", s.onGlobeFocus(3, false));
        assertFalse("but not the sky's copy of it", s.onGlobeFocus(3, true));
        assertTrue("tapping empty sky clears it", s.focusBody(-1));
        assertEquals(-1, s.focusedBody());
    }
}
