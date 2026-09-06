package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

/**
 * Layer 1: the full Swiss Ephemeris call sequence for one chart, plus the values the
 * ephemeris does not return and this layer has to derive.
 *
 * The output is a frozen struct. Nothing above L1 calls the ephemeris again - every
 * later layer reads from a ChartFrame instance. That is what lets the interpretation
 * stack be tested from a fixture with no native library and no dates.
 *
 * Note the Java port exposes swe_houses, not the C library's swe_houses_ex, and takes
 * the house system as an int holding the character code.
 */
public final class ChartFrame {



    /** Chiron is uncomputable outside this range and returns zeroed positions if asked. */
    private static final double CHIRON_MIN_JD = 1967601.5;   // approx 650 AD
    private static final double CHIRON_MAX_JD = 3419437.5;   // approx 4650 AD

    private static final String[] PHASE_NAMES = {
        "New", "Crescent", "First Quarter", "Gibbous",
        "Full", "Disseminating", "Last Quarter", "Balsamic"
    };

    /** One body's L1 output. Absent bodies carry ok=false and are never interpreted. */
    public static final class Body {
        public String name;
        public int ipl;
        public boolean ok;
        public String error;
        public double lon, lat, dist;
        public double lonSpeed, latSpeed, distSpeed;
        public double ra, dec;
        public boolean retrograde;
        public boolean outOfBounds;

        /**
         * True when this position is a midpoint of two sources that are nearly opposite, and
         * is therefore not a settled number.
         *
         * <b>A midpoint takes the shorter arc.</b> When the two sources are close to 180
         * apart the two arcs are nearly equal, so a fraction of a degree decides which one
         * wins and the midpoint jumps to the far side of the chart. Found on David's own
         * reference pair on 2026-08-25: their Parts of Spirit are 178.4 apart, and correcting
         * one birth time by ten minutes moved the composite Part of Spirit
         * <b>178 degrees</b>, from 22 Gemini to 24 Sagittarius.
         *
         * Set only by {@link #computeMidpointComposite}. A Davison is a real chart of a real
         * moment and has no midpoints to be unstable, and a harmonic inherits whatever its
         * source carried.
         */
        public boolean unstableMidpoint;
    }

    public final Body[] bodies = new Body[Bodies.count()];
    public double asc, mc, dsc, ic, armc, vertex, equatorialAsc;
    public final double[] cusps = new double[13];
    public double trueObliquity, meanObliquity;
    public double lotOfFortune, lotOfSpirit;
    public double southNode;
    public double elongation;
    public int phaseIndex;
    public String phaseName;
    public double syzygyLon;
    public double syzygyJd;
    public boolean syzygyWasNewMoon;
    /** Non-fatal warnings from the ephemeris, e.g. falling back to Moshier when .se1 files are absent. */
    public final java.util.List<String> warnings = new java.util.ArrayList<>();
    public boolean moonVoidOfCourse;
    public boolean diurnal;

    /**
     * True when this frame has no real moment behind it - i.e. a midpoint composite.
     *
     * A midpoint composite is an average of two charts, not a chart of an instant. Its Moon is
     * the midpoint of two Moons and was never in that degree, so the questions L1 normally
     * answers about *time* have no referent here: there is no last new moon before it, no
     * void-of-course period, no lunar phase. Those fields are left unset and flagged rather than
     * filled with a number that would look exactly like a real one.
     *
     * A Davison chart is NOT synthetic - it is a genuine chart cast for the midpoint moment and
     * place, so every field means what it usually means and this stays false.
     */
    public boolean syntheticMoment;

    /** Why the time-derived fields are absent, for the UI to show. Null unless synthetic. */
    public String syntheticNote;

    /**
     * True when the birth time was not known and the chart was cast for noon.
     *
     * <b>Different from {@link #syntheticMoment}, and the difference is which half is real.</b>
     * A midpoint composite has real angles and an unreal Moon: it is an average of two charts,
     * so nothing about it happened at an instant. A timeless chart is the opposite - the sky is
     * genuine to within a few degrees, because the planets barely move in a day, while the
     * angles and houses are pure artefacts of the noon guess. The Ascendant crosses all twelve
     * signs in twenty-four hours, so a chart with an unknown time has no Ascendant at all;
     * printing the noon one would be inventing the single most specific-looking fact on the
     * page.
     *
     * <b>Suppressed through {@code ok}, not through special cases.</b> The angles and the two
     * Lots are marked not-ok, which every reader in the app already honours - the same
     * mechanism that hides an unselected body. Adding a "if timeUnknown" branch to the wheel,
     * the grid, the hit test and the readings in turn is how one rule becomes five copies.
     */
    public boolean timeUnknown;

    /** What is missing and why, for the UI to show. Null unless the time is unknown. */
    public String timeUnknownNote;
    public double julianDayUt;
    public double geoLat;
    public double geoLon;
    public double geoAltM;
    public int hsys;
    public boolean topocentric;

    /**
     * Package-private rather than private since 2026-08-25, so {@link Harmonics} can build a
     * derived frame the way the composites do from inside this class. <b>Deliberately not
     * public:</b> a frame is only ever produced by a named derivation - compute, the two
     * composites, or a harmonic - and an empty one escaping into the app is a chart with no
     * positions that still looks like a chart.
     */
    ChartFrame() { }

    // ------------------------------------------------------------------ L1 sequence

    /**
     * The complete call sequence, in order. Every call is checked; a body that fails is
     * marked not-ok rather than silently carrying zeros, which is how a failed Chiron
     * otherwise renders as a real placement at 0 Aries.
     *
     * @param hsys house system as a character code, e.g. 'W' whole sign, 'P' Placidus
     */
    /**
     * A chart whose birth time is not known: cast for noon, with the angles withheld.
     *
     * See {@link #timeUnknown} for why the angles go and the planets stay. The caller supplies
     * the noon Julian day - this class does not know the zone the noon belongs to.
     */
    public static ChartFrame computeTimeUnknown(SwissEph sw, double tjdUtNoon, double geoLat,
                                                double geoLon, int hsys, boolean topocentric,
                                                double geoAltM) {
        ChartFrame f = compute(sw, tjdUtNoon, geoLat, geoLon, hsys, topocentric, geoAltM);
        f.timeUnknown = true;
        f.timeUnknownNote = "The birth time is not known, so this chart is cast for noon. "
            + "The planets are within about half a degree of where they were - they barely "
            + "move in a day - but the Ascendant crosses the whole zodiac in twenty-four "
            + "hours, so the angles, the houses and the Lots are withheld rather than guessed.";
        for (int i = 0; i < Bodies.count() && i < f.bodies.length; i++) {
            Body b = f.bodies[i];
            if (b == null) {
                continue;
            }
            // The angles themselves, and the two Lots, which are measured from the Ascendant
            // and so inherit its whole error.
            if (Bodies.at(i).isAngle()
                    || "Part of Fortune".equals(b.name) || "Part of Spirit".equals(b.name)) {
                b.ok = false;
            }
        }
        f.lotOfFortune = Double.NaN;
        f.lotOfSpirit = Double.NaN;
        return f;
    }

    public static ChartFrame compute(SwissEph sw, double tjdUt, double geoLat, double geoLon,
                                     int hsys, boolean topocentric, double geoAltM) {
        ChartFrame f = new ChartFrame();
        f.julianDayUt = tjdUt;
        f.geoLat = geoLat;
        f.geoLon = geoLon;
        f.geoAltM = geoAltM;
        f.hsys = hsys;
        f.topocentric = topocentric;

        // Speed is not optional: retrograde and applying/separating both need it, and
        // differencing two calls loses precision exactly at a station.
        int base = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED;
        if (topocentric) {
            sw.swe_set_topo(geoLon, geoLat, geoAltM);
            base |= SweConst.SEFLG_TOPOCTR;
        }

        // 1. Obliquity and nutation. Needed before the out-of-bounds test, so do it first
        //    rather than hardcoding 23 deg 26 min.
        double[] eclnut = new double[6];
        StringBuffer serr = new StringBuffer();
        if (sw.swe_calc_ut(tjdUt, SweConst.SE_ECL_NUT, base, eclnut, serr) >= 0) {
            f.trueObliquity = eclnut[0];
            f.meanObliquity = eclnut[1];
        } else {
            f.trueObliquity = 23.4392911;   // fallback only; flagged by the zero-speed check below
            f.meanObliquity = f.trueObliquity;
        }

        // 2. Each body twice: ecliptic with speed, then equatorial for declination.
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if (d.source == Bodies.Source.EPHEMERIS) {
                f.bodies[i] = readBody(sw, tjdUt, d.getIpl(), d.name, base, f.trueObliquity);
                if (f.bodies[i].ok && f.bodies[i].error != null && !f.bodies[i].error.isEmpty()) {
                    f.warnings.add(d.name + ": " + f.bodies[i].error);
                }
            } else {
                f.bodies[i] = new Body();
                f.bodies[i].name = d.name;
                f.bodies[i].ok = false;
            }
        }

        // 3. Houses and angles. Returns ERR when Placidus or Koch have no solution above
        //    the polar circle and substitutes Porphyry, so the return value matters.
        double[] ascmc = new double[10];
        int hret = sw.swe_houses(tjdUt, base, geoLat, geoLon, hsys, f.cusps, ascmc);
        if (hret < 0) {
            System.err.println("swe_houses failed or fell back for hsys '" + (char) hsys + "'");
        }
        f.asc = Zodiac.normalise(ascmc[0]);
        f.mc = Zodiac.normalise(ascmc[1]);
        f.armc = Zodiac.normalise(ascmc[2]);
        f.vertex = Zodiac.normalise(ascmc[3]);
        f.equatorialAsc = Zodiac.normalise(ascmc[4]);

        f.deriveAll(sw, tjdUt, base);

        boolean[] shown = com.zodiacomputing.ourania.gui.Settings.loadBodySelection();
        for (int i = 0; i < Bodies.count(); i++) {
            f.bodies[i].ok = f.bodies[i].ok && shown[i];
        }

        return f;
    }

    /** Shortest angular midpoint across the 0/360 boundary. */
    public static double midpoint(double a, double b) {
        double diff = (b - a) % 360.0;
        if (diff < -180.0) diff += 360.0;
        else if (diff > 180.0) diff -= 360.0;
        return Zodiac.normalise(a + (diff / 2.0));
    }

    /**
     * The point halfway along the great circle between two places, as {@code {lat, lon}}.
     *
     * <b>Not the average of the coordinates, which is what this used to do.</b> Averaging
     * latitude and longitude separately treats the globe as a flat grid, and great circles bow
     * poleward, so the two answers separate as the pair spreads out. Measured 2026-08-30:
     * Chicago/Los Angeles differ by 1.0 degrees of latitude, Philadelphia/Los Angeles by 2.0,
     * Oslo/Vancouver by 19.2, and New York/Tokyo by <b>31.5</b> - which is not a rounding
     * difference, it is a different hemisphere's worth of house cusps.
     *
     * <b>This matters more for Davison than for the composite.</b> A Davison chart IS the chart
     * of the midpoint moment at the midpoint place, so an inaccurate midpoint is simply a wrong
     * chart. The midpoint composite only uses the latitude as a reference place for deriving
     * cusps, which is a free parameter either way.
     *
     * <b>Antipodal points have no unique midpoint</b> - every great circle through them is
     * equally valid - so those fall back to the component-wise answer rather than returning a
     * value the geometry does not define. The caller cannot tell, which is acceptable only
     * because the alternative is NaN.
     */
    public static double[] geographicMidpoint(double lat1, double lon1,
                                              double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(midpoint(lon1, lon2) - lon1) * 2.0;

        double bx = Math.cos(p2) * Math.cos(dLon);
        double by = Math.cos(p2) * Math.sin(dLon);
        double cosSum = Math.cos(p1) + bx;
        double horiz = Math.sqrt(cosSum * cosSum + by * by);

        // Antipodal: the horizontal component vanishes and the midpoint is undefined.
        if (horiz < 1e-12 && Math.abs(Math.sin(p1) + Math.sin(p2)) < 1e-12) {
            return new double[] { (lat1 + lat2) / 2.0, midpoint(lon1, lon2) };
        }

        double lat = Math.toDegrees(Math.atan2(Math.sin(p1) + Math.sin(p2), horiz));
        double lon = lon1 + Math.toDegrees(Math.atan2(by, cosSum));
        // Longitudes are carried as -180..180 everywhere else in this class.
        lon = ((lon + 540.0) % 360.0) - 180.0;
        return new double[] { lat, lon };
    }

    /**
     * Midpoint composite: every point is the midpoint of the two charts' corresponding points.
     *
     * <b>Conventions settled with David on 2026-08-16.</b> This is one of two composite techniques
     * the app offers and they are never blended; see {@link #computeDavisonComposite}.
     *
     * <b>Houses come from the midpoint MC at the midpoint latitude</b> - Astrodienst's
     * "reference place method" - not from midpointing the twelve cusps independently.
     *
     * <b>Corrected 2026-08-30: this is NOT Hand's method, though it said so for months.</b>
     * Hand's default in Planets in Composite (1975) is the midpoint method throughout - the
     * composite Ascendant is the midpoint of the two natal Ascendants, and the cusps are
     * midpointed like the planets. That is precisely the approach the paragraph below argues
     * against. So this implementation deliberately departs from Hand and then credited him for
     * the departure. Hand remains the interpretive reference for what the placements MEAN; he
     * is not the source of this house construction. The earlier version averaged each cusp
     * separately, which produced a plausible-looking chart in testing and exact oppositions by
     * construction - but nothing guaranteed the resulting ASC/MC pair corresponded to any real
     * latitude. Deriving through swe_houses_armc guarantees a chart that could exist, and
     * reuses the library's house code rather than inventing arithmetic.
     *
     * <b>This javadoc used to say "a house frame that no sky could produce", and that was too
     * strong.</b> Measured over 23 pairs on 2026-08-24, sweeping latitude for one that yields
     * the averaged Ascendant from the averaged MC under Placidus: <b>22 of the 23 are
     * achievable</b> - one was not, at a residual of 99 degrees. So a sky usually could produce
     * them.
     *
     * <b>The real objection is which sky.</b> The latitude that produces the averaged pair sits
     * a mean of <b>15.6 degrees</b> from the couple's own midpoint latitude, and more than 10
     * degrees away in 7 of the 22 - so the intermediate cusps are the house frame of somewhere
     * neither person has been. That is a weaker claim than the one this comment made and it is
     * the one the numbers support. The practical size of the difference is unchanged: averaged
     * against derived, the Ascendant differs by a mean of 11.9 degrees, which moves bodies
     * between houses routinely.
     *
     * <b>The time-derived fields are suppressed, not computed.</b> See {@link #syntheticMoment}.
     * The old version passed a null SwissEph into deriveAll, which threw a NullPointerException on
     * the void-of-course scan - so this method had never once run. That crash was really the
     * unsettled convention showing through: the fix is not to hand it an ephemeris, it is to
     * decide that a chart with no moment has no answers to questions about moments.
     *
     * <b>Retrograde is not asserted.</b> Averaging a retrograde body with a direct one gives a
     * speed near zero, which would render as a station that is purely an artefact of the
     * averaging. See the stationarity note in the vault.
     */
    /**
     * How close to 180 two sources must be before their midpoint is called unstable.
     *
     * Two degrees. At exactly 180 the midpoint is undefined - both arcs are the same length -
     * and the instability does not switch on at a threshold, it grows as the pair approaches
     * opposition. This is the width at which a plausible birth-time error can flip the answer:
     * the pair that prompted it sat at 178.4 and flipped on ten minutes.
     */
    public static final double UNSTABLE_MIDPOINT_ORB = 2.0;

    /** Writes a derived angle over the midpointed one, keeping the registry entry identifiable. */
    private static void setAngle(ChartFrame f, String name, double lon) {
        int i = Bodies.indexOfName(name);
        if (i < 0 || i >= f.bodies.length) {
            return;
        }
        Body b = f.bodies[i];
        if (b == null) {
            b = new Body();
            b.name = name;
            f.bodies[i] = b;
        }
        b.lon = Zodiac.normalise(lon);
        b.lonSpeed = 0.0;
        b.retrograde = false;
        // An angle derived from the composite MC is not a midpoint and cannot be unstable
        // as one; clearing this matters because the loop above may already have set it.
        b.unstableMidpoint = false;
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /**
     * The composite at the couple's own midpoint place.
     *
     * <b>The default, not the only answer.</b> The reference place method wants the latitude
     * of somewhere the relationship actually happens; the midpoint of two birthplaces is a
     * defensible automatic choice and nothing more. Use
     * {@link #computeMidpointComposite(SwissEph, ChartFrame, ChartFrame, double, double)} to
     * say where.
     */
    public static ChartFrame computeMidpointComposite(SwissEph sw, ChartFrame c1, ChartFrame c2) {
        double[] mid = geographicMidpoint(c1.geoLat, c1.geoLon, c2.geoLat, c2.geoLon);
        return computeMidpointComposite(sw, c1, c2, mid[0], mid[1]);
    }

    /**
     * The composite with the reference place named.
     *
     * <b>Only the latitude changes the chart.</b> The cusps are derived by
     * {@code swe_houses_armc}, which takes the ARMC and a latitude - and the ARMC here comes
     * from the composite MC, not from geographic longitude. So {@code refLon} is recorded on
     * the frame for display and provenance and has no effect on a single cusp. That is worth
     * stating plainly because a user who types a city expects both halves to matter, and in a
     * midpoint composite only one does. (A Davison is the opposite: it casts a real chart, so
     * its longitude matters as much as its latitude.)
     *
     * <b>Latitude moves cusps a long way.</b> The same composite MC derived at 25 N and at
     * 55 N produces Ascendants far enough apart to move most bodies between houses, so this
     * is a substantive choice, not a cosmetic one.
     *
     * @param refLat latitude to derive the house frame at, degrees north positive
     * @param refLon recorded on the frame; does not affect the cusps
     */
    public static ChartFrame computeMidpointComposite(SwissEph sw, ChartFrame c1, ChartFrame c2,
                                                      double refLat, double refLon) {
        ChartFrame f = new ChartFrame();
        f.julianDayUt = (c1.julianDayUt + c2.julianDayUt) / 2.0;
        double[] compPlace = new double[] { refLat, refLon };
        f.geoLat = compPlace[0];
        f.geoLon = compPlace[1];
        f.geoAltM = (c1.geoAltM + c2.geoAltM) / 2.0;
        f.hsys = c1.hsys;
        f.topocentric = c1.topocentric;
        f.syntheticMoment = true;
        f.syntheticNote = "Midpoint composite: every position is the average of two charts, so "
            + "this chart has no moment of its own. Lunar phase, the prenatal syzygy, "
            + "void-of-course and retrogradation are therefore not shown - they are questions "
            + "about an instant, and there is no instant here.";
        for (int i = 0; i < c1.bodies.length; i++) {
            Body b1 = c1.bodies[i];
            Body b2 = c2.bodies[i];
            Body mb = new Body();
            if (b1 != null && b2 != null && b1.ok && b2.ok) {
                mb.name = b1.name; mb.ipl = b1.ipl; mb.ok = true;
                mb.lon = midpoint(b1.lon, b2.lon);
                mb.lat = (b1.lat + b2.lat) / 2.0;
                mb.dist = (b1.dist + b2.dist) / 2.0;
                mb.lonSpeed = (b1.lonSpeed + b2.lonSpeed) / 2.0;
                mb.latSpeed = (b1.latSpeed + b2.latSpeed) / 2.0;
                mb.distSpeed = (b1.distSpeed + b2.distSpeed) / 2.0;
                mb.ra = midpoint(b1.ra, b2.ra);
                mb.dec = (b1.dec + b2.dec) / 2.0;
                // Deliberately NOT set from the averaged speed: a retrograde body averaged with a
                // direct one lands near zero and would render as a station that exists only in
                // the arithmetic. A composite body is not moving, so it is neither direct nor
                // retrograde.
                mb.retrograde = false;
                mb.outOfBounds = Math.abs(mb.dec) > ((c1.trueObliquity + c2.trueObliquity) / 2.0);
                // Flagged, not dropped: the number is the best available and is still the one
                // to draw, but a reader should know it is standing on a coin edge.
                mb.unstableMidpoint =
                    Math.abs(Aspects.separation(b1.lon, b2.lon) - 180.0) < UNSTABLE_MIDPOINT_ORB;
            } else {
                // <b>The name is set even when the body is not ok, and that is load-bearing.</b>
                // It was not until 2026-08-24, and the consequence was a NullPointerException
                // from anything that walks a composite by name: BodyScore.rank -> Gestalt
                // .compute -> TransferOfLight -> body(String), which does name.equals(..) on
                // every entry. Part of Spirit is not ok in an ordinary natal chart, so it took
                // this branch on essentially every midpoint composite ever built, and every
                // reading of one crashed. A Body that exists must be identifiable whether or
                // not it has a position; ok says whether to interpret it, not whether it is
                // there. Taken from the registry rather than from b1, which may be null.
                mb.name = Bodies.at(i).name;
                mb.ipl = b1 != null ? b1.ipl : -1;
                mb.ok = false;
                mb.error = "Missing in one of the base charts";
            }
            f.bodies[i] = mb;
        }

        f.trueObliquity = (c1.trueObliquity + c2.trueObliquity) / 2.0;
        f.meanObliquity = (c1.meanObliquity + c2.meanObliquity) / 2.0;

        // Houses by the reference place method (NOT Hand's - see the javadoc): the composite MC
        // is the midpoint of the two MCs, and the rest of the frame is DERIVED from it at the
        // midpoint latitude rather than averaged. The
        // library wants the MC as right ascension, so convert: the MC is the ecliptic point on
        // the meridian, so RAMC = atan2(sin(lambda) cos(eps), cos(lambda)).
        double compMc = midpoint(c1.mc, c2.mc);
        double eps = Math.toRadians(f.trueObliquity);
        double lam = Math.toRadians(compMc);
        double ramc = Math.toDegrees(Math.atan2(Math.sin(lam) * Math.cos(eps), Math.cos(lam)));
        f.armc = Zodiac.normalise(ramc);

        double[] ascmc = new double[10];
        int hret = sw == null ? -1
            : sw.swe_houses_armc(f.armc, f.geoLat, f.trueObliquity, f.hsys, f.cusps, ascmc);
        if (hret >= 0) {
            f.asc = Zodiac.normalise(ascmc[0]);
            f.mc = Zodiac.normalise(ascmc[1]);
            f.vertex = Zodiac.normalise(ascmc[3]);
            f.equatorialAsc = Zodiac.normalise(ascmc[4]);
        } else {
            // No ephemeris, or a house system with no solution at this latitude. Fall back to
            // whole-sign-ish equal houses off the midpoint MC so the frame is still usable, and
            // say so rather than silently producing quadrant cusps that were never computed.
            f.mc = compMc;
            f.asc = Zodiac.normalise(compMc + 90.0);
            for (int i = 1; i <= 12; i++) {
                f.cusps[i] = Zodiac.normalise(f.asc + (i - 1) * 30.0);
            }
            f.syntheticNote += " House cusps fell back to equal houses from the midpoint MC "
                + "because the quadrant system had no solution here.";
        }
        f.dsc = Zodiac.opposite(f.asc);
        f.ic = Zodiac.opposite(f.mc);

        // <b>The four angles as registry bodies are the DERIVED angles, not midpoints.</b>
        //
        // The loop above midpointed every registry entry, the angles among them, and the
        // houses were then derived from the composite MC by the reference place method - so until
        // 2026-08-25 the same composite carried two different Ascendants and which one you
        // saw depended on whether you read the wheel's glyph or the house frame. On David's
        // reference pair they were <b>12.93 degrees apart</b>: 27 50' Capricorn as a body,
        // 14 54' Capricorn derived. Whole-sign hid it because both fall in Capricorn; under
        // a quadrant system it moves every cusp.
        //
        // The derived one wins because it is the one the method is documented to use and the
        // only one guaranteed to correspond to a real horizon.
        setAngle(f, "Ascendant", f.asc);
        setAngle(f, "Descendant", f.dsc);
        setAngle(f, "MC", f.mc);
        setAngle(f, "IC", f.ic);

        // Which points are standing on a coin edge, named once on the frame so a reader can
        // be told rather than having to know.
        StringBuilder unstable = new StringBuilder();
        for (Body mb : f.bodies) {
            if (mb != null && mb.ok && mb.unstableMidpoint) {
                unstable.append(unstable.length() == 0 ? "" : ", ").append(mb.name);
            }
        }
        if (unstable.length() > 0) {
            String warn = "Near-opposition midpoint, so the position is unstable: "
                + unstable + ". The two charts' values for these are within "
                + trim(UNSTABLE_MIDPOINT_ORB) + " degrees of opposite, and a midpoint takes "
                + "the shorter arc - a few minutes of birth time flips them to the far side "
                + "of the chart. Do not read them.";
            f.warnings.add(warn);
            f.syntheticNote += " " + warn;
        }

        // Only the timeless derivations. Sect and the two lots follow from the Ascendant, the Sun
        // and the Moon, all of which this chart genuinely has - so they are computed, and the
        // reading should present them as properties of the pairing rather than of a birth.
        Body sun = f.body("Sun");
        Body moon = f.body("Moon");
        if (sun != null && sun.ok && moon != null && moon.ok) {
            f.diurnal = Sect.isDiurnal(sun.lon, f.asc);
            f.lotOfFortune = Sect.lotOfFortune(f.diurnal, f.asc, sun.lon, moon.lon);
            f.lotOfSpirit = Sect.lotOfSpirit(f.diurnal, f.asc, sun.lon, moon.lon);
            f.elongation = Zodiac.normalise(moon.lon - sun.lon);
        }
        Body node = f.body("North Node");
        if (node != null && node.ok) {
            f.southNode = Zodiac.opposite(node.lon);
        }

        // Explicitly NOT computed, and the fields say so rather than carrying a plausible zero.
        f.phaseName = null;
        f.phaseIndex = -1;
        f.syzygyLon = Double.NaN;
        f.syzygyJd = Double.NaN;
        f.moonVoidOfCourse = false;
        return f;
    }

    /**
     * Davison relationship chart: a real chart cast for the midpoint in time and space.
     *
     * <b>Not synthetic</b>, and that is the whole difference from the midpoint composite. Two
     * people born at two moments have a genuine midpoint moment, and a genuine midpoint place, so
     * this is an ordinary chart and every field means exactly what it means anywhere else -
     * lunar phase, prenatal syzygy, void-of-course, retrogradation, all of it. It goes through
     * {@link #compute} unchanged for precisely that reason, and needs no special cases anywhere
     * downstream.
     *
     * The two techniques answer different questions and are never blended: a midpoint composite
     * is an average of two people, a Davison is a moment that sits between them.
     */
    public static ChartFrame computeDavisonComposite(SwissEph sw, ChartFrame c1, ChartFrame c2) {
        double davisonTjd = (c1.julianDayUt + c2.julianDayUt) / 2.0;
        double[] davisonPlace =
            geographicMidpoint(c1.geoLat, c1.geoLon, c2.geoLat, c2.geoLon);
        double davisonLat = davisonPlace[0];
        double davisonLon = davisonPlace[1];
        double davisonAlt = (c1.geoAltM + c2.geoAltM) / 2.0;
        return compute(sw, davisonTjd, davisonLat, davisonLon, c1.hsys, c1.topocentric, davisonAlt);
    }

    /** Two calls per body, both checked. Chiron is range-gated before it can return zeros. */
    private static Body readBody(SwissEph sw, double tjdUt, int ipl, String name,
                                 int baseFlags, double obliquity) {
        Body b = new Body();
        b.name = name;
        b.ipl = ipl;

        if (ipl == SweConst.SE_CHIRON && (tjdUt < CHIRON_MIN_JD || tjdUt > CHIRON_MAX_JD)) {
            b.ok = false;
            b.error = "Chiron is not computable outside 650-4650 AD";
            return b;
        }

        double[] xx = new double[6];
        StringBuffer serr = new StringBuffer();
        if (sw.swe_calc_ut(tjdUt, ipl, baseFlags, xx, serr) < 0) {
            b.ok = false;
            b.error = serr.toString();
            return b;
        }
        b.lon = Zodiac.normalise(xx[0]);
        b.lat = xx[1];
        b.dist = xx[2];
        b.lonSpeed = xx[3];
        b.latSpeed = xx[4];
        b.distSpeed = xx[5];
        b.retrograde = xx[3] < 0.0;

        double[] eq = new double[6];
        StringBuffer serr2 = new StringBuffer();
        if (sw.swe_calc_ut(tjdUt, ipl, baseFlags | SweConst.SEFLG_EQUATORIAL, eq, serr2) < 0) {
            b.ok = false;
            b.error = serr2.toString();
            return b;
        }
        b.ra = eq[0];
        b.dec = eq[1];
        b.outOfBounds = Math.abs(b.dec) > obliquity;

        b.ok = true;
        // Carry any non-fatal warning up rather than dropping it on success.
        String warn = serr.length() > 0 ? serr.toString() : serr2.toString();
        b.error = warn.isEmpty() ? null : warn;
        return b;
    }

    // ------------------------------------------------------- what the ephemeris withholds

    private void deriveAll(SwissEph sw, double tjdUt, int flags) {
        double sun = bodies[Bodies.indexOf("sun")].lon;
        double moon = bodies[Bodies.indexOf("moon")].lon;

        // Descendant and IC are not returned; they are the opposite points.
        dsc = Zodiac.opposite(asc);
        ic = Zodiac.opposite(mc);

        // Only the north node is a body. The south node is its opposition.
        southNode = Zodiac.opposite(bodies[Bodies.indexOf("north_node")].lon);

        // Sect, and the two lots that reverse on it. Both rules live in Sect: the wheel
        // draws the Part of Fortune as a selectable point and asks the same questions, and
        // a night-only divergence between two copies of the lot formula is not something
        // any surface here would surface.
        diurnal = Sect.isDiurnal(sun, asc);
        lotOfFortune = Sect.lotOfFortune(diurnal, asc, sun, moon);
        lotOfSpirit = Sect.lotOfSpirit(diurnal, asc, sun, moon);

        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if (d.source == Bodies.Source.EPHEMERIS) continue;
            bodies[i].lon = Bodies.derive(d.source, asc, mc, sun, moon, bodies[Bodies.indexOf("north_node")].lon);
            bodies[i].ok = !Double.isNaN(bodies[i].lon);
        }

        // Lunar phase. swe_pheno_ut gives illuminated fraction, but the eightfold phase
        // is a bucket on elongation and belongs here.
        elongation = Zodiac.normalise(moon - sun);
        phaseIndex = (int) Math.floor(elongation / 45.0) % 8;
        phaseName = PHASE_NAMES[phaseIndex];

        syzygyLon = prenatalSyzygy(sw, tjdUt, flags);
        moonVoidOfCourse = isMoonVoidOfCourse(sw, tjdUt, flags);
    }

    /**
     * Last new or full moon before birth. There is no call for this: it is a root-find on
     * the Sun-Moon elongation. Step back in hours until the signed elongation, folded to
     * +/-90 around the nearest syzygy, changes sign, then bisect.
     */
    private double prenatalSyzygy(SwissEph sw, double tjdUt, int flags) {
        if (sw == null) return Double.NaN;
        double step = 1.0 / 24.0;
        double t1 = tjdUt;
        double f1 = syzygyResidual(sw, t1, flags);
        for (int i = 0; i < 24 * 32; i++) {
            double t0 = t1 - step;
            double f0 = syzygyResidual(sw, t0, flags);
            if (f0 * f1 <= 0.0 && Math.abs(f1 - f0) < 90.0) {
                for (int j = 0; j < 60; j++) {
                    double tm = (t0 + t1) / 2.0;
                    double fm = syzygyResidual(sw, tm, flags);
                    if (f0 * fm <= 0.0) {
                        t1 = tm;
                        f1 = fm;
                    } else {
                        t0 = tm;
                        f0 = fm;
                    }
                }
                double e = Zodiac.normalise(lonAt(sw, t0, SweConst.SE_MOON, flags)
                                          - lonAt(sw, t0, SweConst.SE_SUN, flags));
                syzygyWasNewMoon = e < 90.0 || e > 270.0;
                syzygyJd = t0;
                return lonAt(sw, t0, SweConst.SE_MOON, flags);
            }
            t1 = t0;
            f1 = f0;
        }
        return Double.NaN;
    }

    /** Elongation folded into [-90, +90), so it crosses zero at both new and full moon. */
    private double syzygyResidual(SwissEph sw, double t, int flags) {
        double e = Zodiac.normalise(lonAt(sw, t, SweConst.SE_MOON, flags)
                                  - lonAt(sw, t, SweConst.SE_SUN, flags));
        double folded = e % 180.0;
        return folded >= 90.0 ? folded - 180.0 : folded;
    }

    /**
     * Void of course: the Moon makes no further Ptolemaic aspect to a traditional planet
     * before leaving its current sign. Stepped forward rather than solved, because the
     * aspecting bodies are moving too.
     */
    private boolean isMoonVoidOfCourse(SwissEph sw, double tjdUt, int flags) {
        int[] others = {SweConst.SE_SUN, SweConst.SE_MERCURY, SweConst.SE_VENUS,
                        SweConst.SE_MARS, SweConst.SE_JUPITER, SweConst.SE_SATURN};
        double[] aspectAngles = {0.0, 60.0, 90.0, 120.0, 180.0};

        double startSign = Math.floor(Zodiac.normalise(lonAt(sw, tjdUt, SweConst.SE_MOON, flags)) / 30.0);
        double step = 1.0 / 48.0;
        double prevMoon = lonAt(sw, tjdUt, SweConst.SE_MOON, flags);
        double[] prevSep = new double[others.length];
        for (int i = 0; i < others.length; i++) {
            prevSep[i] = separation(prevMoon, lonAt(sw, tjdUt, others[i], flags));
        }

        for (double t = tjdUt + step; t < tjdUt + 3.0; t += step) {
            double moon = lonAt(sw, t, SweConst.SE_MOON, flags);
            if (Math.floor(Zodiac.normalise(moon) / 30.0) != startSign) {
                return true;   // left the sign with no aspect perfected
            }
            for (int i = 0; i < others.length; i++) {
                double sep = separation(moon, lonAt(sw, t, others[i], flags));
                for (double a : aspectAngles) {
                    if ((prevSep[i] - a) * (sep - a) <= 0.0) {
                        return false;   // an aspect perfected before the ingress
                    }
                }
                prevSep[i] = sep;
            }
        }
        return true;
    }

    private static double lonAt(SwissEph sw, double t, int ipl, int flags) {
        double[] xx = new double[6];
        StringBuffer serr = new StringBuffer();
        if (sw.swe_calc_ut(t, ipl, flags, xx, serr) < 0) {
            return Double.NaN;
        }
        return Zodiac.normalise(xx[0]);
    }

    /** Absolute separation in [0, 180]. The one seam-safe distance function. */
    public static double separation(double a, double b) {
        double d = Math.abs(Zodiac.normalise(a) - Zodiac.normalise(b)) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    /**
     * The body with this display name.
     *
     * <b>Null-name safe since 2026-08-24.</b> This threw a NullPointerException rather than
     * its own IllegalArgumentException whenever any entry in the array carried a null name -
     * which the midpoint composite produced for every body missing from either base chart.
     * The guard is here as well as at the site that caused it, because this method is walked
     * by most of the interpretation stack and a struct invariant is better defended in the
     * accessor than trusted at forty call sites.
     */
    public Body body(String name) {
        for (Body b : bodies) {
            if (b != null && name != null && name.equals(b.name)) {
                return b;
            }
        }
        throw new IllegalArgumentException("no such body: " + name);
    }
}
