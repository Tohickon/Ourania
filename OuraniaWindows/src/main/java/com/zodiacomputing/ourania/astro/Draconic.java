package com.zodiacomputing.ourania.astro;

/**
 * The draconic chart: the same sky measured from the Moon's north node instead of the equinox.
 *
 * <b>One subtraction, and a different zero.</b> Tropical longitude is measured from the vernal
 * point - where the Sun crosses the equator in March. Draconic longitude is measured from the
 * north node, the point where the Moon's path crosses the Sun's. So every body keeps its exact
 * angular relationship to every other - a square stays a square, to the arcsecond - and the
 * whole pattern is re-read against a different origin. The node itself lands on 0 Aries by
 * construction, which is what tells you the chart was built correctly.
 *
 * <b>What it is for.</b> The node is where the lunar and solar cycles intersect, and the
 * tradition reads the draconic chart as the pattern underneath the biographical one - the same
 * geometry, referred to an axis that has nothing to do with the seasons. It is conventionally
 * read against the tropical chart rather than alone: a draconic Sun on a tropical Ascendant is
 * the sort of contact the technique exists to find.
 *
 * <b>The angles are not converted, and that is the same rule the harmonics follow.</b> A
 * draconic longitude restates a relationship; it does not name a place in the sky. The horizon
 * and meridian are places - they are where the Earth actually was - so shifting them would put
 * a draconic Ascendant glyph against radix house cusps, which is the two-Ascendants defect
 * both the midpoint composite and {@link Harmonics} have already had once each.
 */
public final class Draconic {

    private Draconic() { }

    /**
     * The draconic frame for a chart, or the chart itself when it has no node to measure from.
     *
     * <b>Returns the source unchanged rather than a chart of zeros</b> when the node is
     * missing - a frame whose bodies are all silently offset by nothing looks exactly like a
     * working draconic chart, and the reader would have no way to tell.
     */
    public static ChartFrame of(ChartFrame f) {
        if (f == null) {
            return null;
        }
        ChartFrame.Body node = f.body("North Node");
        if (node == null || !node.ok) {
            return f;
        }
        double origin = Zodiac.normalise(node.lon);

        ChartFrame d = new ChartFrame();
        d.julianDayUt = f.julianDayUt;
        d.geoLat = f.geoLat;
        d.geoLon = f.geoLon;
        d.geoAltM = f.geoAltM;
        d.hsys = f.hsys;
        d.topocentric = f.topocentric;
        d.trueObliquity = f.trueObliquity;
        d.meanObliquity = f.meanObliquity;

        // The house frame is the real sky's and is carried across untouched.
        System.arraycopy(f.cusps, 0, d.cusps, 0, Math.min(f.cusps.length, d.cusps.length));
        d.asc = f.asc;
        d.mc = f.mc;
        d.dsc = f.dsc;
        d.ic = f.ic;
        d.armc = f.armc;
        d.vertex = f.vertex;
        d.equatorialAsc = f.equatorialAsc;
        d.diurnal = f.diurnal;

        for (int i = 0; i < f.bodies.length && i < d.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            ChartFrame.Body db = new ChartFrame.Body();
            if (b == null) {
                db.name = Bodies.at(i).name;
                db.ok = false;
                db.error = "Missing in the source chart";
                d.bodies[i] = db;
                continue;
            }
            // Named whether or not it is ok: anything walking a frame calls equals on every
            // name, and a null there is a NullPointerException in BodyScore.rank.
            db.name = b.name != null ? b.name : Bodies.at(i).name;
            db.ipl = b.ipl;
            db.ok = b.ok;
            db.error = b.error;
            db.lat = b.lat;
            db.dist = b.dist;
            db.latSpeed = b.latSpeed;
            db.distSpeed = b.distSpeed;
            db.dec = b.dec;
            db.outOfBounds = b.outOfBounds;
            // Speed and direction are facts about the body's motion and survive a change of
            // origin: subtracting a constant does not turn a retrograde planet direct.
            db.lonSpeed = b.lonSpeed;
            db.retrograde = b.retrograde;
            db.unstableMidpoint = b.unstableMidpoint;

            boolean angle = Bodies.at(i).isAngle();
            db.lon = angle ? Zodiac.normalise(b.lon) : Zodiac.normalise(b.lon - origin);
            d.bodies[i] = db;
        }

        // The Lots are measured from the Ascendant, which did not move, so they do not either.
        d.lotOfFortune = f.lotOfFortune;
        d.lotOfSpirit = f.lotOfSpirit;
        d.southNode = Zodiac.normalise(f.southNode - origin);

        // <b>Time-derived answers are suppressed, as they are for a midpoint composite.</b>
        // The lunar phase, the prenatal syzygy and the void-of-course period are all statements
        // about where the Sun and Moon were in the tropical zodiac at a moment. Recomputing
        // them from shifted longitudes would produce numbers that look exactly like real ones
        // and refer to nothing.
        d.syntheticMoment = true;
        d.syntheticNote = "A draconic chart re-measures the same sky from the Moon's north "
            + "node instead of the equinox. Every aspect is identical to the radix - only the "
            + "origin has moved - so the lunar phase, the prenatal syzygy and the "
            + "void-of-course period are left to the tropical chart they belong to.";
        d.phaseIndex = -1;
        d.phaseName = null;
        d.syzygyLon = Double.NaN;
        d.syzygyJd = Double.NaN;
        d.moonVoidOfCourse = false;
        return d;
    }

    /**
     * The origin a draconic chart was measured from, for a caller that wants to state it.
     *
     * NaN when the chart has no usable node.
     */
    public static double origin(ChartFrame f) {
        ChartFrame.Body node = f == null ? null : f.body("North Node");
        return node == null || !node.ok ? Double.NaN : Zodiac.normalise(node.lon);
    }
}
