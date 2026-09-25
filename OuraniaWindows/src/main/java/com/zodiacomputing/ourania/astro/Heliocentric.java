package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * The chart as seen from the Sun.
 *
 * <p><b>This is not the geocentric chart with a flag set</b>, which is why D12 sat at "partly
 * built" with topocentric done and this not started. {@code SEFLG_HELCTR} changes what a body
 * call returns, and in doing so it removes more of a chart than it keeps. Every statement below
 * was measured against the ephemeris before this class was written, not recalled:
 *
 * <ul>
 *   <li><b>The Sun is the origin, not a body.</b> Asked for its own heliocentric position it
 *       answers longitude 0.000000 at distance 0.000000 - a well-formed answer meaning "here",
 *       which would plot at 0&deg; Aries if anything drew it.</li>
 *   <li><b>Earth takes its place</b>, at exactly 180.000000&deg; from the geocentric Sun. The two
 *       are the same measurement read from opposite ends.</li>
 *   <li><b>The Moon is 0.13&deg; from the Earth</b> and follows it around, because it orbits the
 *       Earth rather than the Sun. Drawn, it would be a second marker on top of the first.</li>
 *   <li><b>The lunar nodes answer 0.000000</b>, like the Sun. They are where the Moon's orbit
 *       crosses the ecliptic, and the ecliptic is the Earth's orbital plane seen from the Earth.</li>
 *   <li><b>Nothing is ever retrograde.</b> Over ten years of ten-day steps, all eight planets gave
 *       zero retrograde samples heliocentrically against 26 to 157 geocentrically. Retrogradation
 *       is parallax: it is the Earth overtaking, and from the Sun there is nothing to overtake
 *       from.</li>
 *   <li><b>There are no houses and no angles.</b> {@code swe_houses} takes a geographic latitude
 *       and longitude; Ascendant, MC, Vertex and East Point are where the horizon of a place cuts
 *       the ecliptic. There is no observer on the Sun, so there is no horizon, and with the
 *       Ascendant go the Part of Fortune, the Part of Spirit and all five lots, which are arcs
 *       measured from it.</li>
 * </ul>
 *
 * <p>What survives is the planets, the centaurs and the asteroids - real bodies in real orbits
 * round the real centre - plus the Earth among them, and the aspects between them. That is a
 * smaller chart and an honest one, and it is why this has its own screen rather than a switch on
 * the existing one: half the geocentric chart would have had to learn to say "undefined", and a
 * screen that renders mostly blanks teaches a reader nothing.
 *
 * <p><b>One rule, in one place.</b> {@link #hasHeliocentricPosition} is the only thing that
 * decides which bodies appear, and it carries the reason for each exclusion in
 * {@link #whyExcluded}. This project's recurring defect is one rule implemented twice and the
 * copies drifting apart - {@code isMinor} was found in three places and {@code ringWord} in six -
 * so the screen, the suite and the engine all ask this method rather than keeping their own list.
 */
public final class Heliocentric {

    private Heliocentric() {
    }

    /** Swiss Ephemeris body number for the Earth, which has no entry in {@link Bodies}. */
    public static final int SE_EARTH = SweConst.SE_EARTH;

    /**
     * The name the Earth is shown under.
     *
     * <b>Deliberately not added to {@link Bodies}.</b> That registry's order is the index
     * everything in the app uses - the settings checkboxes, the saved selection, the prose keys -
     * and inserting a body into it would renumber every one of them for a point that exists in
     * one screen. It lives here instead.
     */
    public static final String EARTH = "Earth";

    /** The Earth's glyph, and its fallback for a font that lacks it. */
    public static final String EARTH_GLYPH = "⊕";
    public static final String EARTH_FALLBACK = "Ea";

    /**
     * One body's place in the heliocentric chart.
     *
     * <p>No house, because there are none; no retrograde flag, because nothing is. The speed is
     * kept anyway - it is what a reader uses to see which body is moving fastest through the
     * chart, and it is what proves the no-retrograde claim rather than asserting it.
     */
    public static final class Place {
        public String name;
        public String glyph;
        public String fallback;
        /** Ecliptic longitude in the zodiac in force, 0-360. */
        public double longitude;
        /** Ecliptic latitude. Larger here than geocentrically for the inner planets. */
        public double latitude;
        /** Degrees per day. Always positive; see the class note. */
        public double speed;
        /** Distance from the Sun in AU, which is the thing a heliocentric chart can say. */
        public double distanceAu;
        public boolean ok;
        public String error;
    }

    /** A heliocentric chart: the bodies that exist from the Sun, and nothing that does not. */
    public static final class Frame {
        public double julianDayUt;
        public final List<Place> places = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();

        /** The place for a name, or null. */
        public Place byName(String name) {
            for (Place p : places) {
                if (p.name.equals(name)) {
                    return p;
                }
            }
            return null;
        }
    }

    /**
     * Whether a body has a heliocentric position worth drawing.
     *
     * <b>The single rule.</b> Everything that lists the heliocentric bodies asks this.
     *
     * <p>Note what it is not: it is not "is this a planet". Chiron, Ceres and the rest orbit the
     * Sun and answer perfectly well. The excluded set is small and each member is excluded for its
     * own reason, which {@link #whyExcluded} states.
     */
    public static boolean hasHeliocentricPosition(Bodies.Def d) {
        return d.source == Bodies.Source.EPHEMERIS && whyExcluded(d) == null;
    }

    /**
     * Why a body is left out of the heliocentric chart, or null when it is in.
     *
     * <p>In the reader's language, because it is shown on the screen. A list of absences with no
     * reasons reads as a defect; the same list with its reasons reads as the astronomy.
     */
    public static String whyExcluded(Bodies.Def d) {
        if (d.source != Bodies.Source.EPHEMERIS) {
            return "derived from the Ascendant or the houses, which need an observer on the Earth";
        }
        if ("sun".equals(d.id)) {
            return "the centre of this chart, not a body in it";
        }
        if ("moon".equals(d.id)) {
            return "orbits the Earth, not the Sun - it sits within 0.13° of the Earth here";
        }
        if ("north_node".equals(d.id)) {
            return "where the Moon's orbit crosses the ecliptic, and the ecliptic is the Earth's "
                + "own orbital plane";
        }
        if ("lilith".equals(d.id)) {
            return "a focus of the Moon's orbit around the Earth";
        }
        return null;
    }

    /** The bodies this chart draws, in {@link Bodies} order, with the Earth in the Sun's place. */
    public static List<String> shownBodies() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            // The Earth enters where the Sun leaves, so the eye finds it where it expects a
            // luminary rather than appended after Pluto.
            if ("sun".equals(d.id)) {
                names.add(EARTH);
            }
            if (hasHeliocentricPosition(d)) {
                names.add(d.name);
            }
        }
        return names;
    }

    /**
     * Compute the heliocentric chart for a moment.
     *
     * <p>The zodiac in force applies: a sidereal reader gets sidereal heliocentric longitudes,
     * because the zodiac is a frame for measuring longitude and is independent of where the
     * measurement is taken from. That is the same reason {@link Ephemeris#flags} exists.
     */
    public static Frame compute(SwissEph sw, double tjdUt) {
        Frame f = new Frame();
        f.julianDayUt = tjdUt;

        // SEFLG_SPEED for the same reason ChartFrame gives: differencing two calls loses
        // precision, and here the speed is also the evidence for the no-retrograde claim.
        int base = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED | SweConst.SEFLG_HELCTR;
        base = Ephemeris.flags(sw, base);

        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if ("sun".equals(d.id)) {
                f.places.add(read(sw, tjdUt, SE_EARTH, EARTH, EARTH_GLYPH, EARTH_FALLBACK, base, f));
            }
            if (hasHeliocentricPosition(d)) {
                f.places.add(read(sw, tjdUt, d.getIpl(), d.name, d.glyph, d.fallback, base, f));
            }
        }
        return f;
    }

    /**
     * One body, read and guarded.
     *
     * <b>Package-private for the suite, deliberately.</b> The origin guard below cannot be
     * reached through {@link #compute} - no registered body answers the origin, which is the
     * whole point of the exclusion rule - so a mutation that deleted the guard survived the
     * suite untouched. A guard nothing can exercise is a comment. This is the seam that lets
     * {@code HeliocentricCheck} hand it the Sun and watch it refuse.
     */
    static Place read(SwissEph sw, double tjdUt, int ipl, String name, String glyph,
                      String fallback, int flags, Frame f) {
        Place p = new Place();
        p.name = name;
        p.glyph = glyph;
        p.fallback = fallback;
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        int rc = sw.swe_calc_ut(tjdUt, ipl, flags, xx, err);
        if (rc < 0) {
            p.ok = false;
            p.error = err.toString();
            f.warnings.add(name + ": " + p.error);
            return p;
        }
        p.longitude = Zodiac.normalise(xx[0]);
        p.latitude = xx[1];
        p.distanceAu = xx[2];
        p.speed = xx[3];
        p.ok = true;
        // A body that answers zero distance is answering "here" - the Sun's own case. Nothing
        // registered should reach this, and if one does it must not be silently plotted at Aries.
        if (p.distanceAu == 0.0 && p.longitude == 0.0) {
            p.ok = false;
            p.error = "no heliocentric position: the ephemeris answered the origin";
            f.warnings.add(name + ": " + p.error);
        }
        return p;
    }

    /** One aspect between two heliocentric bodies. */
    public static final class Contact {
        public String a;
        public String b;
        public Aspects.Type type;
        /** Degrees from exact. */
        public double orb;
        /** The width this pair was judged at. */
        public double orbUsed;
    }

    /**
     * Aspects between the bodies of a heliocentric chart.
     *
     * <p><b>Judged at the natal profile's widths</b>, not the transit profile's. This is a chart
     * of a moment in its own right, the way a natal chart is, rather than one body's passage over
     * another - so it takes the widths a chart is read at. A reader who widens Mars natally widens
     * it here.
     *
     * <p>No applying or separating: that asks which of two bodies is catching the other, and
     * heliocentrically both always move forward at their own steady rate, so the answer would be
     * arithmetic on the speeds rather than the thing an astrologer means by it.
     */
    public static List<Contact> aspects(Frame f) {
        List<Contact> out = new ArrayList<>();
        for (int i = 0; i < f.places.size(); i++) {
            for (int j = i + 1; j < f.places.size(); j++) {
                Place a = f.places.get(i);
                Place b = f.places.get(j);
                if (!a.ok || !b.ok) {
                    continue;
                }
                double sep = Aspects.separation(a.longitude, b.longitude);
                double width = Aspects.orbFor(a.name, b.name, Aspects.Profile.NATAL);
                Aspects.Type t = Aspects.typeWithin(sep, a.name, b.name, width,
                    Aspects.Profile.NATAL);
                if (t == null) {
                    continue;
                }
                Contact c = new Contact();
                c.a = a.name;
                c.b = b.name;
                c.type = t;
                c.orb = Math.abs(sep - t.exactAngle);
                c.orbUsed = Math.min(width, Aspects.capOf(t, Aspects.Profile.NATAL));
                out.add(c);
            }
        }
        return out;
    }
}
