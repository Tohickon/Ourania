package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

/**
 * The ephemeris regression corpus, and the invariants that say whether it is right (J15).
 *
 * <p><b>Two halves, because one of them cannot do the job alone.</b> Part A re-computes every
 * chart in {@code ephemeris-corpus.tsv} and reports any that moved - which catches a change and
 * says nothing about correctness, because the expectations came from this build. Parts B onwards
 * assert things that are true of the <i>sky</i>: the length of the tropical year, the synodic
 * month, the sidereal periods of the planets, the bounds on ecliptic latitude, and the fact that
 * a body's longitude does not depend on where the observer is standing. Those are the ones a
 * genuinely wrong ephemeris fails.
 *
 * <p><b>The constants below are not taken from this program.</b> They are the textbook figures -
 * 365.2422 days, 29.5306 days, 4332.59 days - so an implementation that agreed with itself and
 * with nothing else would fail here. That is the whole point of the section, and it is the thing
 * the row meant by "known".
 *
 * <p><b>Part F is about the one place latitude enters a chart.</b> A planet's longitude is the
 * same from Greenwich and from Svalbard; only the angles move. The corpus holds five places at
 * every instant precisely so that this can be asserted rather than assumed - and it is worth
 * asserting, because "the sky was cast at the wrong place" was a live defect in this app twice
 * this month.
 */
public final class EphemerisCorpusCheck {

    private EphemerisCorpusCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        part("A: nothing in the corpus has moved", EphemerisCorpusCheck::drift);
        part("B: the corpus covers the eras and latitudes it claims to",
            EphemerisCorpusCheck::coverage);
        part("C: the tropical year, from equinox to equinox", EphemerisCorpusCheck::year);
        part("D: the synodic month, from new moon to new moon", EphemerisCorpusCheck::month);
        part("E: each planet returns to its own longitude in its own period",
            EphemerisCorpusCheck::periods);
        part("F: a longitude does not depend on where you are standing",
            EphemerisCorpusCheck::place);
        part("G: ecliptic latitude stays inside the bounds each body has",
            EphemerisCorpusCheck::latitudes);
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Part A

    /**
     * Every stored row, recomputed.
     *
     * <b>The longitude and the source, and the source is the half that would go unnoticed.</b> A
     * body that stops being read from the files and starts being approximated moves by well under
     * an arcsecond - far too little to fail a tolerance, and a different computation entirely.
     */
    private static void drift() throws Exception {
        java.io.File file = new java.io.File(EphemerisCorpus.FILE);
        ok("the corpus is there (" + file.getAbsolutePath() + ")", file.isFile());
        if (!file.isFile()) {
            ok("run EphemerisCorpus with " + EphemerisCorpus.ACKNOWLEDGE + " to make it", false);
            return;
        }
        java.util.Map<String, EphemerisCorpus.Row> stored = EphemerisCorpus.read(file);
        java.util.List<EphemerisCorpus.Row> now = EphemerisCorpus.measure();
        ok("it has rows (" + stored.size() + ")", stored.size() > 500);
        ok("and the same number are computed now (" + now.size() + ")",
            now.size() == stored.size());

        java.util.List<String> moved = new java.util.ArrayList<>();
        java.util.List<String> changedSource = new java.util.ArrayList<>();
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (EphemerisCorpus.Row r : now) {
            EphemerisCorpus.Row was = stored.get(r.key());
            if (was == null) {
                missing.add(r.key().replace('\t', '/'));
                continue;
            }
            if (!was.source.equals(r.source)) {
                changedSource.add(r.key().replace('\t', '/') + ": " + was.source + " -> "
                    + r.source);
            }
            boolean bothNaN = Double.isNaN(was.lon) && Double.isNaN(r.lon);
            if (!bothNaN && Math.abs(was.lon - r.lon) > EphemerisCorpus.TOLERANCE) {
                moved.add(r.key().replace('\t', '/') + ": " + was.lon + " -> " + r.lon);
            }
        }
        ok("every stored row is still computed: " + head(missing), missing.isEmpty());
        ok("no longitude has moved: " + head(moved), moved.isEmpty());
        ok("and nothing has started answering from somewhere else: " + head(changedSource),
            changedSource.isEmpty());
    }

    // ---------------------------------------------------------------- Part B

    /**
     * That the corpus is the shape it says it is.
     *
     * <b>Part A passes against a corpus of one row.</b> A drift check over a file that covers
     * nothing is a check that cannot fail, and this is what stops the file being quietly narrowed.
     */
    private static void coverage() throws Exception {
        java.util.Map<String, EphemerisCorpus.Row> stored =
            EphemerisCorpus.read(new java.io.File(EphemerisCorpus.FILE));
        java.util.Set<String> eras = new java.util.TreeSet<>();
        java.util.Set<String> places = new java.util.TreeSet<>();
        java.util.Set<String> sources = new java.util.TreeSet<>();
        for (EphemerisCorpus.Row r : stored.values()) {
            eras.add(r.era);
            places.add(r.place);
            sources.add(r.source);
        }
        ok("it spans " + EphemerisCorpus.ERAS.length + " eras (" + eras.size() + ")",
            eras.size() == EphemerisCorpus.ERAS.length);
        ok("from before the files begin to after they end",
            eras.contains("1500") && eras.contains("2450"));
        ok("at " + EphemerisCorpus.PLACES.length + " places (" + places.size() + ")",
            places.size() == EphemerisCorpus.PLACES.length);
        ok("including one above the polar circle and one below",
            places.contains("longyearbyen") && places.contains("ushuaia"));
        ok("and one on the equator", places.contains("equator"));

        // <b>All three outcomes are in the file.</b> If they were not, two thirds of what the
        // source column is for would never be exercised by the drift check.
        ok("the file records answers from the ephemeris files", sources.contains("files"));
        ok("answers from the approximation", sources.contains("approximation"));
        ok("and points that cannot be computed at all",
            sources.contains("error") || sources.contains("none"));

        java.util.List<String> angles = new java.util.ArrayList<>();
        for (EphemerisCorpus.Row r : stored.values()) {
            if (r.point.equals("Ascendant") && r.era.equals("2000")) {
                angles.add(r.place);
            }
        }
        ok("every place has its own Ascendant (" + angles.size() + ")",
            angles.size() == EphemerisCorpus.PLACES.length);
    }

    // ---------------------------------------------------------------- Part C

    /** The mean tropical year, in days. Not from this program. */
    static final double TROPICAL_YEAR = 365.2422;

    /**
     * Successive March equinoxes are a tropical year apart.
     *
     * <b>The equinox is a definition, not an observation.</b> The tropical zodiac begins where the
     * Sun's longitude is zero, so "when is the equinox" and "when is the Sun at 0 Aries" are the
     * same question - which is what makes this checkable without an almanac. What is <i>not</i>
     * definitional is how long the gap is, and 365.2422 is the figure every textbook carries.
     */
    private static void year() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double[] found = new double[6];
        int n = 0;
        for (int y = 1900; y <= 2150; y += 50) {
            found[n++] = crossing(sw, SweConst.SE_SUN, 0.0,
                new SweDate(y, 3, 15, 0.0).getJulDay(), 20.0);
        }
        for (int i = 1; i < n; i++) {
            double gap = (found[i] - found[i - 1]) / 50.0;
            ok(String.format("%d to %d averages %.5f days a year", 1900 + 50 * (i - 1),
                1900 + 50 * i, gap), Math.abs(gap - TROPICAL_YEAR) < 0.002);
        }
        // <b>And the quarters, which is where a sign error would hide.</b> A zodiac off by a
        // quarter turn keeps the year exactly right.
        double eq = found[2];
        double solstice = crossing(sw, SweConst.SE_SUN, 90.0, eq, 120.0);
        ok(String.format("the June solstice is %.2f days after the March equinox",
            solstice - eq), Math.abs((solstice - eq) - 92.8) < 0.5);
        double autumn = crossing(sw, SweConst.SE_SUN, 180.0, eq, 220.0);
        ok(String.format("and the September equinox %.2f days after it", autumn - eq),
            Math.abs((autumn - eq) - 186.4) < 0.5);
    }

    // ---------------------------------------------------------------- Part D

    /** The mean synodic month, in days. Not from this program. */
    static final double SYNODIC_MONTH = 29.530588;

    /**
     * New moons are a synodic month apart.
     *
     * <b>The one invariant here that exercises the Moon</b>, which on this machine is computed
     * without its own data file. A conjunction of Sun and Moon is a definition too - the elongation
     * passing through zero - so no almanac is needed for the instants, only for the interval.
     */
    private static void month() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double from = new SweDate(2000, 1, 1, 0.0).getJulDay();
        double first = newMoon(sw, from);
        double last = first;
        int count = 0;
        // A hundred lunations is long enough that a single bad conjunction cannot average out and
        // short enough to run in a second.
        for (int i = 0; i < 100; i++) {
            last = newMoon(sw, last + 25.0);
            count++;
        }
        double mean = (last - first) / count;
        ok(String.format("%d lunations average %.6f days", count, mean),
            Math.abs(mean - SYNODIC_MONTH) < 0.01);
        ok("and none of them was missed", count == 100 && last - first > 2900);

        // At a new moon the two are in the same degree; at the full moon, opposite. A Moon read
        // from the wrong slot - which this app has seen - fails both.
        double sunLon = lonAt(sw, SweConst.SE_SUN, last);
        double moonLon = lonAt(sw, SweConst.SE_MOON, last);
        ok(String.format("at the conjunction they share a longitude (%.4f vs %.4f)",
            sunLon, moonLon), separation(sunLon, moonLon) < 0.05);
    }

    // ---------------------------------------------------------------- Part E

    /**
     * Sidereal periods, in days. Textbook figures, and the tolerance is what an eccentric orbit
     * makes of a single revolution rather than a mean one.
     */
    static final double[][] PERIODS = {
        {SweConst.SE_JUPITER, 4332.59, 40.0},
        {SweConst.SE_SATURN, 10759.22, 120.0},
        {SweConst.SE_MARS, 686.98, 40.0},
        {SweConst.SE_URANUS, 30685.4, 400.0},
    };

    /**
     * Each planet comes back to the same longitude after its own period.
     *
     * <b>Measured heliocentrically.</b> A geocentric longitude is the planet's position seen past
     * a moving Earth, so it does not repeat on the planet's own period at all - the first draft
     * asserted it did and was wrong by months. The sidereal period is a property of the orbit, so
     * the observation has to be taken from the body the orbit is around.
     */
    private static void periods() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double from = new SweDate(2000, 1, 1, 0.0).getJulDay();
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_HELCTR;
        for (double[] row : PERIODS) {
            int ipl = (int) row[0];
            double start = lonAt(sw, ipl, from, flags);
            double after = lonAt(sw, ipl, from + row[1], flags);
            double moved = separation(start, after);
            ok(String.format("%s is back within %.2f degrees after %.0f days (%.3f)",
                nameOf(ipl), row[2] / 30.0, row[1], moved), moved < row[2] / 30.0);

            // <b>And is NOT back half a period earlier</b>, without which the assertion above
            // passes for a body that never moves.
            double half = separation(start, lonAt(sw, ipl, from + row[1] / 2.0, flags));
            ok(String.format("and is on the far side of its orbit halfway through (%.1f)", half),
                half > 150.0);
        }
    }

    // ---------------------------------------------------------------- Part F

    /**
     * The same instant at five places: the bodies agree, the angles do not.
     *
     * <b>Both halves, and the second is the one that matters here.</b> "Every place gives the same
     * longitudes" is also true of a program that ignores the place entirely - which is what the
     * sky ring did until 20 September, casting every chart in the Atlantic off Africa. The
     * assertion that the Ascendants differ is what separates the two.
     */
    private static void place() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(2000, 1, 1, 12.0).getJulDay();
        ChartFrame[] frames = new ChartFrame[EphemerisCorpus.PLACES.length];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ChartFrame.compute(sw, jd,
                Double.parseDouble(EphemerisCorpus.PLACES[i][1]),
                Double.parseDouble(EphemerisCorpus.PLACES[i][2]),
                EphemerisCorpus.HOUSES, false, 0.0);
        }
        double worst = 0.0;
        String where = "";
        for (int b = 0; b < Bodies.count(); b++) {
            // <b>Only the bodies that are looked up.</b> The first draft walked the whole
            // registry and failed at 153 degrees on the MC, which is correct behaviour reported
            // as a defect: the angles and the lots are DERIVED from the place, so of course they
            // move. What is place-independent is a body the ephemeris answers for.
            if (Bodies.at(b).source != Bodies.Source.EPHEMERIS) {
                continue;
            }
            ChartFrame.Body first = frames[0].bodies[b];
            if (first == null || !first.ok) {
                continue;
            }
            for (int i = 1; i < frames.length; i++) {
                ChartFrame.Body other = frames[i].bodies[b];
                if (other == null || !other.ok) {
                    continue;
                }
                double d = separation(first.lon, other.lon);
                if (d > worst) {
                    worst = d;
                    where = Bodies.at(b).name + " at " + EphemerisCorpus.PLACES[i][0];
                }
            }
        }
        // Not zero: a topocentric correction is not in force here, but the library still works in
        // doubles and the frames are built independently.
        ok(String.format("every body agrees across five places to %.2e degrees (%s)",
            worst, where.isEmpty() ? "no disagreement at all" : "worst: " + where),
            worst < 1e-9);

        java.util.Set<Long> ascendants = new java.util.TreeSet<>();
        java.util.Set<Long> midheavens = new java.util.TreeSet<>();
        for (ChartFrame f : frames) {
            ascendants.add(Math.round(f.asc * 1000.0));
            midheavens.add(Math.round(f.mc * 1000.0));
        }
        ok("and five places give five Ascendants (" + ascendants.size() + ")",
            ascendants.size() == frames.length);

        // <b>The Midheaven is the exception, and it is a definition rather than a coincidence.</b>
        // The MC is the ecliptic's crossing of the meridian, which is fixed by the local sidereal
        // time and the obliquity alone - so two places on the same meridian share it whatever
        // their latitude. None of the five here share a meridian, so all five differ; what is
        // asserted is the rule itself.
        ChartFrame sameMeridian = ChartFrame.compute(sw, jd, 12.0,
            Double.parseDouble(EphemerisCorpus.PLACES[0][2]), EphemerisCorpus.HOUSES, false, 0.0);
        ok(String.format("the Midheaven is the same at another latitude on one meridian "
            + "(%.6f vs %.6f)", frames[0].mc, sameMeridian.mc),
            separation(frames[0].mc, sameMeridian.mc) < 1e-9);
        ok("while the Ascendant there is not",
            separation(frames[0].asc, sameMeridian.asc) > 1.0);
        ok("and five places give five Midheavens too (" + midheavens.size() + ")",
            midheavens.size() == frames.length);
    }

    // ---------------------------------------------------------------- Part G

    /**
     * Each planet's orbital inclination in degrees and its semi-major axis in AU. Textbook
     * figures for the orbits, not for what is seen from here.
     */
    static final double[][] ORBITS = {
        {SweConst.SE_MERCURY, 7.005, 0.3871},
        {SweConst.SE_VENUS, 3.395, 0.7233},
        {SweConst.SE_MARS, 1.850, 1.5237},
        {SweConst.SE_JUPITER, 1.303, 5.2026},
        {SweConst.SE_SATURN, 2.485, 9.5549},
    };

    /**
     * Headroom on the geometric bound, for the eccentricity it leaves out.
     *
     * <b>Measured, after the first version was written from the inclinations alone and failed on
     * three planets.</b> Venus reached 8.81 degrees against the 3.40 of its orbit, Jupiter 1.64
     * against 1.30, Saturn 2.79 against 2.49 - all three correct, and the assertion wrong. The
     * bound below is circular-orbit geometry; Mars fits it worst because its orbit is the least
     * circular of the five, and 1.3 is what covers it.
     */
    static final double ECCENTRICITY_HEADROOM = 1.3;

    /** The Moon's, which no planetary geometry describes: 5.145 of orbit plus perturbation. */
    static final double MOON_BOUND = 5.4;

    /**
     * How far from the ecliptic a planet can appear from here.
     *
     * <b>Seen from the Earth, not from the Sun - and that distinction is the assertion.</b> A
     * planet's orbit is inclined by a degree or two; what an observer on a different orbit sees
     * is that tilt magnified by how close the two come, which for Venus turns 3.4 degrees into
     * nearly 9. The factor is a/|a-1|, the orbit's radius over the least distance between the
     * two, and it is why a bound copied from a table of inclinations is wrong for every planet.
     */
    static double latitudeBound(double inclination, double au) {
        return inclination * (au / Math.abs(au - 1.0)) * ECCENTRICITY_HEADROOM;
    }

    /**
     * Ecliptic latitude stays inside the bound its orbit implies, and goes most of the way to it.
     *
     * <b>A longitude can be wrong and stay in range; a latitude cannot.</b> The Moon never leaves
     * about 5.3 degrees of the ecliptic, so a Moon read out of another body's buffer - the failure
     * this project has an open row about - shows up here even when its longitude looks plausible.
     */
    private static void latitudes() {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double from = new SweDate(1900, 1, 1, 0.0).getJulDay();

        for (double[] row : ORBITS) {
            int ipl = (int) row[0];
            double inclination = row[1];
            double bound = latitudeBound(inclination, row[2]);
            double worst = extremeLatitude(sw, ipl, from);
            ok(String.format("%s stays within %.2f degrees of the ecliptic (%.4f)",
                nameOf(ipl), bound, worst), worst < bound);
            // <b>And gets most of the way to that bound.</b> Without a floor the part passes
            // for a planet reported flat on the ecliptic, and it is the half that asserts the
            // geometry rather than an upper limit.
            //
            // <b>Not "further than its own orbit is tilted", which was the first floor and was
            // wrong for Mercury.</b> The factor a/|a-1| is 0.63 for Mercury and 2.6 for Venus, so
            // the same geometry that lifts Venus from 3.4 degrees to nearly 9 holds Mercury BELOW
            // its own 7 - it is never seen far enough from the Sun to show its tilt. Measured
            // 4.98, and the assertion said it should exceed 7.005.
            double geometric = bound / ECCENTRICITY_HEADROOM;
            ok(String.format("and reaches most of what the geometry predicts, %.3f of %.3f",
                worst, geometric), worst > geometric * 0.6);
        }

        double moon = extremeLatitude(sw, SweConst.SE_MOON, from);
        ok(String.format("the Moon stays within %.2f degrees of the ecliptic (%.4f)",
            MOON_BOUND, moon), moon < MOON_BOUND);
        ok("and reaches most of that", moon > MOON_BOUND * 0.9);

        // The Sun's ecliptic latitude is zero by definition - the ecliptic IS its path - so
        // anything but zero here is the frame itself being wrong.
        double sun = extremeLatitude(sw, SweConst.SE_SUN, from);
        ok(String.format("the Sun never leaves the ecliptic (%.6f)", sun), sun < 0.001);
    }

    /**
     * The furthest a body gets from the ecliptic over two centuries.
     *
     * Every eleven days: dense enough to catch a body near its standstill rather than only near
     * its nodes, and coarse enough to finish in a second.
     */
    private static double extremeLatitude(SwissEph sw, int ipl, double from) {
        double worst = 0.0;
        for (double jd = from; jd < from + 73000.0; jd += 11.0) {
            double[] xx = new double[6];
            if (sw.swe_calc_ut(jd, ipl, SweConst.SEFLG_SWIEPH, xx, new StringBuffer()) < 0) {
                continue;
            }
            worst = Math.max(worst, Math.abs(xx[1]));
        }
        return worst;
    }

    // ---------------------------------------------------------------- plumbing

    /** The registry's name for an ephemeris number, so a failure names a body and not a code. */
    private static String nameOf(int ipl) {
        for (int i = 0; i < Bodies.count(); i++) {
            if (Bodies.at(i).getIpl() == ipl) {
                return Bodies.at(i).name;
            }
        }
        return "ipl " + ipl;
    }

    private static double lonAt(SwissEph sw, int ipl, double jd) {
        return lonAt(sw, ipl, jd, SweConst.SEFLG_SWIEPH);
    }

    private static double lonAt(SwissEph sw, int ipl, double jd, int flags) {
        double[] xx = new double[6];
        sw.swe_calc_ut(jd, ipl, flags, xx, new StringBuffer());
        return Zodiac.normalise(xx[0]);
    }

    /** The shorter arc between two longitudes, 0 to 180. */
    private static double separation(double a, double b) {
        double d = Math.abs(Zodiac.normalise(a) - Zodiac.normalise(b)) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    /**
     * When a body's longitude next passes a target, by bisection over a bracketing window.
     *
     * Signed distance to the target rather than the absolute value, so the sign change the search
     * needs actually exists; wrapped into -180..180 so a crossing of 0 Aries is not a crossing of
     * 360 degrees of nothing.
     */
    private static double crossing(SwissEph sw, int ipl, double target, double from,
            double window) {
        double lo = from;
        double hi = from + window;
        double flo = signed(lonAt(sw, ipl, lo) - target);
        for (double t = from; t <= from + window; t += 0.25) {
            double f = signed(lonAt(sw, ipl, t) - target);
            if (flo <= 0 && f >= 0) {
                lo = t - 0.25;
                hi = t;
                break;
            }
            flo = f;
        }
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2.0;
            if (signed(lonAt(sw, ipl, mid) - target) < 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    /** When the Moon next catches the Sun, at or after a Julian day. */
    private static double newMoon(SwissEph sw, double from) {
        double lo = from;
        double hi = from + 40.0;
        double prev = signed(lonAt(sw, SweConst.SE_MOON, lo) - lonAt(sw, SweConst.SE_SUN, lo));
        for (double t = from; t <= from + 40.0; t += 0.25) {
            double d = signed(lonAt(sw, SweConst.SE_MOON, t) - lonAt(sw, SweConst.SE_SUN, t));
            if (prev < 0 && d >= 0) {
                lo = t - 0.25;
                hi = t;
                break;
            }
            prev = d;
        }
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2.0;
            double d = signed(lonAt(sw, SweConst.SE_MOON, mid) - lonAt(sw, SweConst.SE_SUN, mid));
            if (d < 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    /** An angular difference folded into -180..180. */
    private static double signed(double d) {
        double x = ((d % 360.0) + 360.0) % 360.0;
        return x > 180.0 ? x - 360.0 : x;
    }

    private static String head(java.util.List<String> list) {
        if (list.isEmpty()) {
            return "none";
        }
        return list.size() + ", first: " + list.subList(0, Math.min(3, list.size()));
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
