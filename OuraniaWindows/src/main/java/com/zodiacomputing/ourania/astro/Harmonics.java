package com.zodiacomputing.ourania.astro;

/**
 * Harmonic charts: every longitude multiplied by N and wrapped back into the circle.
 *
 * <p>The technique is John Addey's. Multiplying by N turns the Nth-harmonic aspect into a
 * conjunction, which is the whole point of looking at one: a quintile (72&deg;) between two
 * bodies in the radix is a conjunction in H5, where the eye finds it immediately and an orb
 * of a few degrees means what it usually means. H7 shows septiles the same way, H9 noviles.
 * <b>H1 is the radix</b> - the map is the identity at N=1, which is why the dropdown starts
 * there rather than at 2.
 *
 * <h2>What is transformed and what is not</h2>
 *
 * <b>Bodies are transformed. House cusps and the angles are not.</b> That is a choice, it is
 * contested, and it is written here rather than buried:
 *
 * <ul>
 *   <li>A harmonic longitude is an <i>angular relationship</i> restated, not a position in the
 *       sky. Multiplying the Ascendant by five does not name a horizon anything ever crossed,
 *       so deriving a house frame from it would invent a mundane structure the sky never had.
 *   <li>Keeping the radix cusps means a harmonic position still lands in a real house, which
 *       is what most software does and is the least-invented option available.
 *   <li>The cost is real and worth stating: a body's harmonic house placement mixes a
 *       transformed longitude with an untransformed frame. Read the aspects, not the houses.
 * </ul>
 *
 * If David decides harmonic angles are wanted instead, change it <b>here</b> - every surface
 * reads its harmonic positions through this class, so there is one place to change and no
 * chance of the wheel and the grid disagreeing about what H5 means.
 *
 * <h2>Composites</h2>
 *
 * The harmonic is applied to the chart on screen, so a harmonic of a composite is
 * <b>H(midpoint(a, b))</b>, not midpoint(H(a), H(b)). Those are different charts and the
 * difference is not subtle: the midpoint takes the shorter arc, and scaling by N does not
 * commute with that choice. Whichever is wanted, only one of them can be "the harmonic of the
 * chart you are looking at", and this is it.
 */
public final class Harmonics {

    private Harmonics() { }

    /** The lowest harmonic offered. H1 is the radix itself. */
    public static final int MIN = 1;

    /**
     * The highest harmonic offered.
     *
     * 360 because David asked for the full range. Note the arithmetic degenerates at the top
     * end rather than failing: at N=360 every whole degree maps to 0, so a chart of bodies at
     * exact degrees collapses to a stack at 0&deg; Aries. That is the transform being honest
     * about what it does, not a bug - the fractional part is all that survives.
     */
    public static final int MAX = 360;

    /**
     * One longitude in the Nth harmonic.
     *
     * <b>The only statement of the rule in the codebase.</b> The panel maps its position
     * arrays with it and {@link #of} maps a whole frame with it, so the wheel, the aspect
     * grid, the hit test and the interpretation cannot disagree about where a harmonic body
     * is - the defect this project has logged more than any other.
     */
    public static double map(double longitude, int n) {
        if (n <= 1) {
            return Zodiac.normalise(longitude);
        }
        return Zodiac.normalise(Zodiac.normalise(longitude) * n);
    }

    /** True when this harmonic changes anything. N=1 is the radix and N&lt;1 is not offered. */
    public static boolean isActive(int n) {
        return n > 1 && n <= MAX;
    }

    /** Clamps a requested harmonic into the offered range, so a bad setting cannot poison a chart. */
    public static int clamp(int n) {
        return n < MIN ? MIN : (n > MAX ? MAX : n);
    }

    /**
     * A whole frame in the Nth harmonic: bodies moved, house frame left alone.
     *
     * Returns the SAME frame object when the harmonic is inactive, so a caller can use this
     * unconditionally without paying for a copy on every ordinary chart.
     *
     * <b>Time-derived fields are cleared the way a midpoint composite clears them.</b> A
     * harmonic chart has no moment of its own any more than a composite does - the lunar
     * phase of H5 is not a thing the Moon ever did - so the phase, the syzygy and
     * void-of-course are dropped rather than carried over looking authoritative. Sect and the
     * lots are also dropped: they are computed from the Sun, Moon and Ascendant, and the
     * Ascendant here is deliberately un-transformed, so the three no longer belong to one
     * frame and any answer would be a mixture.
     */
    public static ChartFrame of(ChartFrame f, int n) {
        if (f == null || !isActive(n)) {
            return f;
        }
        ChartFrame h = new ChartFrame();
        h.julianDayUt = f.julianDayUt;
        h.geoLat = f.geoLat;
        h.geoLon = f.geoLon;
        h.geoAltM = f.geoAltM;
        h.hsys = f.hsys;
        h.topocentric = f.topocentric;
        h.trueObliquity = f.trueObliquity;
        h.meanObliquity = f.meanObliquity;

        // The house frame, carried across untouched - see the class note.
        System.arraycopy(f.cusps, 0, h.cusps, 0, Math.min(f.cusps.length, h.cusps.length));
        h.asc = f.asc;
        h.mc = f.mc;
        h.dsc = f.dsc;
        h.ic = f.ic;
        h.armc = f.armc;
        h.vertex = f.vertex;
        h.equatorialAsc = f.equatorialAsc;

        for (int i = 0; i < f.bodies.length && i < h.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            ChartFrame.Body hb = new ChartFrame.Body();
            if (b == null) {
                hb.name = Bodies.at(i).name;
                hb.ok = false;
                hb.error = "Missing in the source chart";
                h.bodies[i] = hb;
                continue;
            }
            // The name is set whether or not the body is ok, for the reason the midpoint
            // composite records: anything walking a frame by name calls name.equals on every
            // entry, and a null name there is a NullPointerException in BodyScore.rank.
            hb.name = b.name != null ? b.name : Bodies.at(i).name;
            hb.ipl = b.ipl;
            hb.ok = b.ok;
            hb.error = b.error;
            hb.lat = b.lat;
            hb.dist = b.dist;
            hb.latSpeed = b.latSpeed;
            hb.distSpeed = b.distSpeed;
            hb.dec = b.dec;
            hb.outOfBounds = b.outOfBounds;
            // <b>The four angles are NOT mapped, and must not be.</b> They are registry
            // bodies like any other, so the obvious loop maps them - and then the frame
            // carries a harmonic Ascendant glyph against radix house cusps, which is the
            // two-Ascendants defect the midpoint composite had until 2026-08-25, reproduced
            // one class over. It was invisible until the wheel was rendered: at H5 the ASC
            // marker had walked to the top of the chart while the ASC/DSC axis stayed put.
            //
            // Consistent with the class note: a harmonic longitude restates an angular
            // relationship and does not name a place in the sky, so the horizon and meridian
            // stay where the sky actually put them.
            boolean angle = Bodies.at(i).isAngle();
            hb.lon = angle ? Zodiac.normalise(b.lon) : map(b.lon, n);
            hb.ra = angle ? Zodiac.normalise(b.ra) : map(b.ra, n);
            // Speed scales with the map: d/dt (n * lon) = n * d/dt lon. Retrogradation is a
            // real property of the underlying body and survives, unlike in a composite where
            // averaging a retrograde with a direct body invents a station. An unmapped angle
            // keeps its own speed for the same reason it keeps its longitude.
            hb.lonSpeed = angle ? b.lonSpeed : b.lonSpeed * n;
            hb.retrograde = b.retrograde;
            h.bodies[i] = hb;
        }

        h.southNode = map(f.southNode, n);

        // Deliberately not carried: see the note above.
        h.diurnal = f.diurnal;
        h.phaseName = null;
        h.phaseIndex = -1;
        h.syzygyLon = Double.NaN;
        h.syzygyJd = Double.NaN;
        h.moonVoidOfCourse = false;
        h.lotOfFortune = Double.NaN;
        h.lotOfSpirit = Double.NaN;
        h.elongation = Double.NaN;

        h.syntheticMoment = true;
        h.syntheticNote = "Harmonic " + n + ": every position is its radix longitude multiplied "
            + "by " + n + ", so this chart has no moment of its own. Lunar phase, the prenatal "
            + "syzygy, void-of-course and the lots are therefore not shown. The house cusps and "
            + "the angles are the radix ones, untransformed - a harmonic longitude restates an "
            + "angular relationship and does not name a place in the sky, so read the aspects "
            + "rather than the house placements.";
        return h;
    }
}
