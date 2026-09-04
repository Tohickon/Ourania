package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L8 support: an almanac - walks a date range and finds the moments when something happens.
 *
 * Every event this produces is the same problem - a quantity derived from the ephemeris
 * crosses a value - so there is one root finder and several functions handed to it:
 *
 *   ingress   longitude crosses a sign boundary
 *   station   speed in longitude crosses zero
 *   aspect    the angle between two bodies reaches an exact figure
 *   lunation  the Sun-Moon angle reaches 0 or 180, which is the aspect case
 *
 * Coarse scan to bracket a crossing, then bisect. The scan step has to be small enough
 * that a crossing cannot be bracketed twice inside one step; nothing in the solar system
 * reverses fast enough for half a day to be unsafe, the Moon included.
 *
 * Stateless and independent of ChartFrame: computing a whole frame per sample would be
 * about a hundred times the work for two numbers, and this is called thousands of times.
 * It does share ChartFrame's body tables, so a body added there is scannable here.
 */
public final class Almanac {

    /** Default coarse step, in days. */
    public static double scanStep = 0.5;
    /**
     * Coarse step for the mundane aspect scan, in days.
     *
     * Separate from scanStep because this one scan is about four fifths of the cost of a
     * year: five outer bodies is ten pairs, times five aspect types, times both sides of
     * the zodiac - eighty root scans over the whole range, where an ingress scan is one.
     *
     * The bound is the fastest relative motion in MUNDANE, which is Jupiter against
     * Saturn at roughly 0.37 deg/day. A step must not let that pair traverse a whole
     * aspect and back unseen; at 5 days it moves 1.85 deg, which is far inside the margin.
     * Held as a field rather than a constant so a check can sweep it and confirm the
     * event list does not change.
     */
    public static double mundaneStep = 5.0;
    /**
     * Bisection tolerance, in days. About a second of time.
     *
     * Set by the fastest thing being scanned rather than by taste. The Moon moves
     * 13.2 deg/day, so the angular error of a result is roughly its speed times this:
     * at 1e-4 days that is a thousandth of a degree, which is more slop than a lunation
     * time deserves. At 1e-5 it is a ten-thousandth, and bisection only needs three or
     * four more halvings to get there.
     */
    public static double precisionDays = 1.0e-5;
    /** Guard against a wrap being mistaken for a crossing. */
    private static final double WRAP_GUARD = 180.0;

    private static final int FLAGS = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED;

    public enum Kind {
        INGRESS, STATION_RETROGRADE, STATION_DIRECT, ASPECT, NEW_MOON, FULL_MOON,
        SOLAR_ECLIPSE, LUNAR_ECLIPSE
    }

    /**
     * Ephemeris flag for the eclipse searches.
     *
     * Without SEFLG_SPEED: the eclipse functions want a plain ephemeris selector, and the
     * speed bit is meaningless to them. Same SWIEPH request the rest of this class makes,
     * which on this machine falls back to Moshier - see the ephemeris-fallback note.
     */
    private static final int ECLIPSE_FLAGS = SweConst.SEFLG_SWIEPH;

    /**
     * How close an eclipse maximum must be to a lunation for them to be the same moment.
     *
     * A solar eclipse maximum IS the Sun-Moon conjunction and a lunar eclipse maximum IS the
     * opposition, so in practice these agree to minutes. Half a day is loose enough to be
     * safe and far tighter than the 29.5 days between successive lunations of one phase, so
     * it cannot match the wrong one.
     */
    private static final double ECLIPSE_LUNATION_DAYS = 0.5;

    public static final class Event implements Comparable<Event> {
        public double jd;
        public Kind kind;
        public String body;
        /** Second body, for aspects. */
        public String other;
        public Aspects.Type aspect;
        /** Sign entered, for an ingress. */
        public int sign = -1;
        /** Longitude of the body at the event. */
        public double longitude;
        /** True when an ingress is the body backing INTO a sign it had left. */
        public boolean retrograde;
        /** Eclipse magnitude class: "total", "annular", "hybrid", "partial", "penumbral". */
        public String detail = "";
        /**
         * A short badge naming why an entry is here when the rule alone would not put it here.
         * Empty for the great majority of events, which need no defence.
         */
        public String note = "";

        @Override
        public int compareTo(Event o) {
            return Double.compare(jd, o.jd);
        }

        private String eclipseLabel(String which) {
            return detail == null || detail.isEmpty()
                ? capitalise(which) + " eclipse"
                : capitalise(detail) + " " + which + " eclipse";
        }

        @Override
        public String toString() {
            switch (kind) {
                case INGRESS:
                    return String.format("%s enters %s%s", body,
                        capitalise(Zodiac.SIGNS[sign]), retrograde ? " (retrograde)" : "");
                case STATION_RETROGRADE:
                    return body + " stations retrograde at " + Zodiac.format(longitude);
                case STATION_DIRECT:
                    return body + " stations direct at " + Zodiac.format(longitude);
                case NEW_MOON:
                    return "New Moon at " + Zodiac.format(longitude);
                case FULL_MOON:
                    return "Full Moon at " + Zodiac.format(longitude);
                case SOLAR_ECLIPSE:
                    return eclipseLabel("solar") + " at " + Zodiac.format(longitude);
                case LUNAR_ECLIPSE:
                    return eclipseLabel("lunar") + " at " + Zodiac.format(longitude);
                default:
                    return String.format("%s %s %s exact", body, aspect.label.toLowerCase(), other);
            }
        }
    }

    private Almanac() { }

    // ---------------------------------------------------------------- the solver

    /** A scalar function of Julian day. */
    public interface OfTime {
        double at(double jd);
    }

    /**
     * Bisects a bracketed root. Callers must have established that f changes sign across
     * [lo, hi] - this does not search, it only refines.
     */
    public static double bisect(OfTime f, double lo, double hi) {
        double flo = f.at(lo);
        for (int i = 0; i < 200 && hi - lo > precisionDays; i++) {
            double mid = (lo + hi) / 2.0;
            double fmid = f.at(mid);
            if (fmid == 0.0) {
                return mid;
            }
            if ((flo < 0) == (fmid < 0)) {
                lo = mid;
                flo = fmid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    /**
     * Every root of f in the range, found by stepping and bisecting each sign change.
     *
     * Skips brackets where the function jumped by more than the wrap guard: an angle
     * running off 360 back to 0 changes sign without crossing anything, and taking that
     * as a root is how a scanner invents events that never happened.
     */
    public static List<Double> roots(OfTime f, double jd0, double jd1, double step) {
        List<Double> out = new ArrayList<>();
        double prev = f.at(jd0);
        for (double t = jd0 + step; t <= jd1; t += step) {
            double cur = f.at(t);
            if (prev != 0.0 && cur != 0.0 && (prev < 0) != (cur < 0)
                && Math.abs(cur - prev) < WRAP_GUARD) {
                out.add(bisect(f, t - step, t));
            }
            prev = cur;
        }
        return out;
    }

    // ---------------------------------------------------------------- ephemeris

    /** {longitude, speed} for a body, or null if it will not compute. */
    private static double[] pos(SwissEph sw, double jd, int ipl) {
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        if (sw.swe_calc_ut(jd, ipl, FLAGS, xx, err) == SweConst.ERR) {
            return null;
        }
        return new double[]{Zodiac.normalise(xx[0]), xx[3]};
    }

    private static double lon(SwissEph sw, double jd, int ipl) {
        double[] p = pos(sw, jd, ipl);
        return p == null ? Double.NaN : p[0];
    }

    /**
     * Swiss Ephemeris body number for a display name, or -1.
     *
     * Reads the Bodies registry. It used to read ChartFrame's own parallel BODY_NAMES and
     * getBodiesIpl() arrays, which were removed when ChartFrame moved onto the registry;
     * this went unnoticed because a stale Almanac.class shadowed the broken source, so the
     * build stayed green while every caller died with NoSuchMethodError at runtime.
     *
     * Derived points - the angles, Fortune, the Vertex - resolve to -1 rather than to their
     * placeholder ipl, because handing swe_calc_ut a -1 is exactly the mistake BodyCheck
     * Part A guards against. The node and Lilith variants come out settings-aware, because
     * Def.getIpl() reads Settings, which the old fixed array could not do.
     */
    public static int iplOf(String body) {
        Bodies.Def d = Bodies.byName(body);
        if (d == null || d.source != Bodies.Source.EPHEMERIS) {
            return -1;
        }
        return d.getIpl();
    }

    /**
     * Signed difference in (-180, 180].
     *
     * Public because L8 needs exactly this and a second copy of it elsewhere is how the
     * two-surfaces defect starts.
     */
    public static double signedDelta(double a, double b) {
        double d = Zodiac.normalise(a - b);
        return d > 180.0 ? d - 360.0 : d;
    }

    /**
     * One body's ecliptic longitude at a moment, by name.
     *
     * The by-name entry point for callers outside the scanner, so a solar return or a
     * transit lookup shares this body-table mapping rather than repeating it. Returns NaN
     * if the body is unknown or the ephemeris call fails - never a zero, which would read
     * as a real placement at 0 Aries.
     */
    public static double bodyLongitude(SwissEph sw, double jd, String body) {
        int ipl = iplOf(body);
        return ipl < 0 ? Double.NaN : lon(sw, jd, ipl);
    }

    // ---------------------------------------------------------------- detectors

    /**
     * Sign ingresses, including the extra crossings a retrograde body makes when it backs
     * over a boundary and returns. Those are real events and a scanner that reports one
     * ingress per sign silently drops two thirds of a retrograde passage.
     */
    public static List<Event> ingresses(SwissEph sw, double jd0, double jd1, String... bodies) {
        List<Event> out = new ArrayList<>();
        for (String body : bodies) {
            int ipl = iplOf(body);
            if (ipl < 0) {
                continue;
            }
            double prev = lon(sw, jd0, ipl);
            for (double t = jd0 + scanStep; t <= jd1; t += scanStep) {
                double cur = lon(sw, t, ipl);
                if (Double.isNaN(prev) || Double.isNaN(cur)) {
                    prev = cur;
                    continue;
                }
                int sPrev = Zodiac.signIndex(prev);
                int sCur = Zodiac.signIndex(cur);
                if (sPrev != sCur) {
                    // The boundary crossed is the start of whichever sign was entered,
                    // which for a retrograde crossing is the start of the sign LEFT.
                    boolean forward = signedDelta(cur, prev) > 0;
                    double boundary = (forward ? sCur : sPrev) * 30.0;
                    final int fipl = ipl;
                    final double b = boundary;
                    double jd = bisect(x -> signedDelta(lon(sw, x, fipl), b), t - scanStep, t);

                    Event e = new Event();
                    e.jd = jd;
                    e.kind = Kind.INGRESS;
                    e.body = body;
                    e.longitude = lon(sw, jd, ipl);
                    e.sign = sCur;
                    e.retrograde = !forward;
                    out.add(e);
                }
                prev = cur;
            }
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    /** Stations, direct and retrograde, from the sign change of speed. */
    public static List<Event> stations(SwissEph sw, double jd0, double jd1, String... bodies) {
        List<Event> out = new ArrayList<>();
        for (String body : bodies) {
            int ipl = iplOf(body);
            if (ipl < 0) {
                continue;
            }
            final int fipl = ipl;
            OfTime speed = x -> {
                double[] p = pos(sw, x, fipl);
                return p == null ? Double.NaN : p[1];
            };
            for (double jd : roots(speed, jd0, jd1, scanStep)) {
                Event e = new Event();
                e.jd = jd;
                e.body = body;
                e.longitude = lon(sw, jd, ipl);
                // Speed a little after the station tells which way it turned.
                e.kind = speed.at(jd + 0.5) < 0 ? Kind.STATION_RETROGRADE : Kind.STATION_DIRECT;
                out.add(e);
            }
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    /**
     * Exact aspects between two bodies - zero orb, the moment of perfection.
     *
     * Both the applying and separating side of the zodiac are searched, because an aspect
     * of 60 degrees perfects at +60 and at -60 and they are different events.
     */
    public static List<Event> exactAspects(SwissEph sw, double jd0, double jd1,
                                           String bodyA, String bodyB, Aspects.Type... types) {
        List<Event> out = new ArrayList<>();
        int ia = iplOf(bodyA);
        int ib = iplOf(bodyB);
        if (ia < 0 || ib < 0) {
            return out;
        }
        for (Aspects.Type type : types) {
            double[] targets = (type.exactAngle == 0.0 || type.exactAngle == 180.0)
                ? new double[]{type.exactAngle}
                : new double[]{type.exactAngle, -type.exactAngle};
            for (double target : targets) {
                OfTime f = x -> signedDelta(signedDelta(lon(sw, x, ia), lon(sw, x, ib)), target);
                for (double jd : roots(f, jd0, jd1, scanStep)) {
                    Event e = new Event();
                    e.jd = jd;
                    e.kind = Kind.ASPECT;
                    e.body = bodyA;
                    e.other = bodyB;
                    e.aspect = type;
                    e.longitude = lon(sw, jd, ia);
                    out.add(e);
                }
            }
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    /** New and full Moons - the Sun-Moon conjunction and opposition. */
    public static List<Event> lunations(SwissEph sw, double jd0, double jd1) {
        List<Event> out = new ArrayList<>();
        for (Event e : exactAspects(sw, jd0, jd1, "Moon", "Sun",
                Aspects.Type.CONJUNCTION, Aspects.Type.OPPOSITION)) {
            e.kind = e.aspect == Aspects.Type.CONJUNCTION ? Kind.NEW_MOON : Kind.FULL_MOON;
            e.aspect = null;
            e.other = null;
            e.body = "Moon";
            out.add(e);
        }
        return out;
    }

    // ---------------------------------------------------------------- eclipses

    /**
     * Eclipses in the range, solar and lunar, at the moment of maximum.
     *
     * These do <b>not</b> go through the root finder above. An eclipse is not "a quantity
     * crosses a value" - it is a conjunction or opposition that additionally happens near a
     * node, with the geometry deciding whether it is total, annular or partial. Swiss
     * Ephemeris already solves that, so re-deriving it from longitudes here would be both
     * slower and wrong at the edges. The scanner's job is to walk the range and collect.
     *
     * <b>Longitude convention:</b> a solar eclipse is recorded at the Sun's degree and a
     * lunar eclipse at the Moon's. For a solar eclipse the two coincide anyway. For a lunar
     * one they are opposite, and the Moon's degree is the one the tradition reckons the
     * eclipse to fall on - so a caller matching eclipses to natal points is matching against
     * the degree that is actually meant.
     */
    public static List<Event> eclipses(SwissEph sw, double jd0, double jd1) {
        List<Event> out = new ArrayList<>();
        out.addAll(eclipseSeries(sw, jd0, jd1, true));
        out.addAll(eclipseSeries(sw, jd0, jd1, false));
        out.sort(Comparator.naturalOrder());
        return out;
    }

    /**
     * Walks one eclipse family forwards from jd0.
     *
     * The loop is guarded twice, because the ephemeris call is a search and not a step: once
     * on a generous count for the range, and once on the returned time failing to advance.
     * Either failure mode would otherwise be an infinite loop inside a background thread,
     * which presents as the app hanging with no error.
     */
    private static List<Event> eclipseSeries(SwissEph sw, double jd0, double jd1, boolean solar) {
        List<Event> out = new ArrayList<>();
        StringBuffer err = new StringBuffer();
        double t = jd0;
        int limit = (int) ((jd1 - jd0) / 20.0) + 10;
        for (int guard = 0; guard < limit && t < jd1; guard++) {
            double[] tret = new double[10];
            int flags = solar
                ? sw.swe_sol_eclipse_when_glob(t, ECLIPSE_FLAGS, 0, tret, 0, err)
                : sw.swe_lun_eclipse_when(t, ECLIPSE_FLAGS, 0, tret, 0, err);
            if (flags == SweConst.ERR || Double.isNaN(tret[0]) || tret[0] <= t) {
                break;
            }
            double max = tret[0];
            if (max > jd1) {
                break;
            }
            Event e = new Event();
            e.jd = max;
            e.kind = solar ? Kind.SOLAR_ECLIPSE : Kind.LUNAR_ECLIPSE;
            e.body = solar ? "Sun" : "Moon";
            e.longitude = lon(sw, max, solar ? SweConst.SE_SUN : SweConst.SE_MOON);
            e.detail = solar ? solarClass(flags) : lunarClass(flags);
            out.add(e);
            // Step clear of the one just found. Two eclipses of the same family are never
            // less than a fortnight apart, so a day is ample and cannot skip one.
            t = max + 1.0;
        }
        return out;
    }

    /**
     * Swiss Ephemeris returns one magnitude class combined with a centrality bit, so these
     * are tested most-specific first rather than as a bit soup.
     */
    private static String solarClass(int flags) {
        if ((flags & SweConst.SE_ECL_ANNULAR_TOTAL) != 0) return "hybrid";
        if ((flags & SweConst.SE_ECL_TOTAL) != 0)         return "total";
        if ((flags & SweConst.SE_ECL_ANNULAR) != 0)       return "annular";
        if ((flags & SweConst.SE_ECL_PARTIAL) != 0)       return "partial";
        return "";
    }

    private static String lunarClass(int flags) {
        if ((flags & SweConst.SE_ECL_TOTAL) != 0)     return "total";
        if ((flags & SweConst.SE_ECL_PARTIAL) != 0)   return "partial";
        if ((flags & SweConst.SE_ECL_PENUMBRAL) != 0) return "penumbral";
        return "";
    }

    /**
     * The moment in a range when the Ascendant reaches a given degree, for a location.
     *
     * The same solver aimed at a different function - this is the "reverse search" for a
     * wanted rising degree, and it needs no new machinery.
     */
    public static List<Double> ascendantReaches(SwissEph sw, double jd0, double jd1,
                                                double lat, double lon, int hsys,
                                                double targetLongitude) {
        OfTime f = x -> {
            double[] cusps = new double[13];
            double[] ascmc = new double[10];
            sw.swe_houses(x, SweConst.SEFLG_SWIEPH, lat, lon, hsys, cusps, ascmc);
            return signedDelta(ascmc[0], targetLongitude);
        };
        // The Ascendant laps the zodiac daily, so the step must be well under a day.
        return roots(f, jd0, jd1, Math.min(scanStep, 0.02));
    }

    // ---------------------------------------------------------------- calendar

    /**
     * A safe scan step for a body, in days.
     *
     * A single step sized for the Moon is correct everywhere and wasteful almost
     * everywhere: Pluto moves under 0.03 deg/day, so sampling it four times a day asks
     * the ephemeris the same question sixty times over. The rule is that a step must not
     * let a body traverse a whole sign, or cross and re-cross a boundary, unseen - so it
     * scales with speed, with a wide margin.
     */
    private static double stepFor(String body) {
        switch (body) {
            case "Moon":                                   return 0.5;
            case "Sun": case "Mercury": case "Venus":       return 0.5;
            case "Mars":                                    return 1.0;
            case "Jupiter": case "Saturn":                  return 2.0;
            default:                                        return 3.0;
        }
    }

    /**
     * The bodies the annual calendar scans for ingresses.
     *
     * This list used to be ChartFrame.BODY_NAMES and is reproduced here exactly, verified
     * against out-selftest's pre-registry ChartFrame.class. It is deliberately NOT
     * "every ephemeris point in the registry": the registry now carries 22 of those,
     * including five asteroids and the Lilith/Fortune/Vertex points, and sweeping them all
     * would both slow the year scan and quietly add lines to a calendar nobody asked to
     * change. Which bodies deserve a dated entry is an editorial choice, and it belongs
     * here in the Almanac rather than in whatever list a chart happens to compute.
     *
     * Add to it deliberately. Eris moves about a degree and a half a century, so it would
     * cost a full scan to report nothing.
     */
    public static final String[] CALENDAR_BODIES = {
        "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter",
        "Saturn", "Uranus", "Neptune", "Pluto", "North Node", "Chiron"
    };

    /** Bodies that can station. The lights never retrograde, so never station. */
    private static final String[] RETROGRADERS = {
        "Mercury", "Venus", "Mars", "Jupiter", "Saturn",
        "Uranus", "Neptune", "Pluto", "Chiron"
    };

    /**
     * The stations a year's calendar shows unasked.
     *
     * <b>A station earns its line by being rare enough to date a year by.</b> Jupiter through
     * Pluto and Chiron station once a year each and the retrograde runs for months, so the
     * date names a season. Venus and Mars station rarely too but their loops are read as
     * personal weather rather than as landmarks, and they are off by default; ask for them
     * with the flag on {@link #annualCalendar}.
     *
     * <b>Mercury is the declared exception, and is badged as one.</b> By the rule it belongs
     * with Venus and Mars: it stations three times a year, which is the opposite of rare, and
     * an earlier audit flagged its presence beside the slow bodies as a contradiction. It is
     * here because a reader who opens a calendar and cannot find Mercury retrograde concludes
     * the calendar is broken. That is a cultural fact about the audience rather than an
     * astrological one about the body, so the entry carries a note saying so instead of
     * sitting silently among the landmarks pretending to be one.
     */
    private static final String[] STATION_DEFAULT = {
        "Mercury", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto", "Chiron"
    };

    /** Stations shown only when asked for: read as personal weather, not as landmarks. */
    private static final String[] STATION_ON_REQUEST = { "Venus", "Mars" };

    /** The badge {@link #STATION_DEFAULT} hangs on Mercury, and on nothing else. */
    public static final String MERCURY_STATION_NOTE =
        "shown by convention, not by the rule that admits the slow stations";

    /** The slow pairs whose exact aspects are the mundane events worth listing. */
    private static final String[] MUNDANE = {
        "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"
    };

    /**
     * Everything dateable in one year: ingresses, stations, lunations, and the exact
     * aspects between the slow bodies.
     *
     * Aspects are restricted to the outer pairs on purpose. Every pair of the twelve
     * bodies would be sixty-six pairs times five aspects times both sides of the zodiac,
     * which is both far slower and a worse calendar - the Moon alone perfects hundreds of
     * aspects a year and none of them is an event anyone marks.
     */
    public static List<Event> annualCalendar(SwissEph sw, double jd0, double jd1) {
        return annualCalendar(sw, jd0, jd1, false);
    }

    /**
     * @param includeMoonIngresses the Moon changes sign every two and a half days, so
     *     including it adds about 157 entries to a year and they crowd out everything
     *     else - a calendar where nine tenths of the lines are the Moon moving is not a
     *     calendar of events. Off by default; ask for it when the Moon IS the subject.
     */
    public static List<Event> annualCalendar(SwissEph sw, double jd0, double jd1,
                                             boolean includeMoonIngresses) {
        return annualCalendar(sw, jd0, jd1, includeMoonIngresses, false);
    }

    /**
     * @param includeFastStations adds the Venus and Mars stations, which are off by default.
     *     See {@link #STATION_DEFAULT} for what the default admits and why Mercury is in it.
     */
    public static List<Event> annualCalendar(SwissEph sw, double jd0, double jd1,
                                             boolean includeMoonIngresses,
                                             boolean includeFastStations) {
        List<Event> out = new ArrayList<>();
        double saved = scanStep;
        try {
            for (String body : CALENDAR_BODIES) {
                if (!includeMoonIngresses && "Moon".equals(body)) {
                    continue;
                }
                scanStep = stepFor(body);
                out.addAll(ingresses(sw, jd0, jd1, body));
            }
            for (String body : STATION_DEFAULT) {
                scanStep = stepFor(body);
                for (Event e : stations(sw, jd0, jd1, body)) {
                    if ("Mercury".equals(body)) {
                        e.note = MERCURY_STATION_NOTE;
                    }
                    out.add(e);
                }
            }
            if (includeFastStations) {
                for (String body : STATION_ON_REQUEST) {
                    scanStep = stepFor(body);
                    out.addAll(stations(sw, jd0, jd1, body));
                }
            }
            scanStep = 0.5;
            // An eclipse IS a new or full Moon. Emitting both would print one moment twice
            // under two names, so the eclipse - the more specific fact - wins and the
            // lunation it belongs to is dropped. Note this is a suppression, not an edit:
            // lunations() stays a pure Sun-Moon angle and is still correct on its own.
            List<Event> eclipses = eclipses(sw, jd0, jd1);
            for (Event lun : lunations(sw, jd0, jd1)) {
                if (!eclipsed(lun, eclipses)) {
                    out.add(lun);
                }
            }
            out.addAll(eclipses);

            scanStep = mundaneStep;
            for (int i = 0; i < MUNDANE.length; i++) {
                for (int j = i + 1; j < MUNDANE.length; j++) {
                    out.addAll(exactAspects(sw, jd0, jd1, MUNDANE[i], MUNDANE[j],
                        Aspects.Type.values()));
                }
            }
        } finally {
            scanStep = saved;
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    /** Whether this lunation is the same moment as an eclipse of the matching phase. */
    private static boolean eclipsed(Event lunation, List<Event> eclipses) {
        for (Event ecl : eclipses) {
            boolean samePhase =
                (lunation.kind == Kind.NEW_MOON && ecl.kind == Kind.SOLAR_ECLIPSE)
                || (lunation.kind == Kind.FULL_MOON && ecl.kind == Kind.LUNAR_ECLIPSE);
            if (samePhase && Math.abs(ecl.jd - lunation.jd) < ECLIPSE_LUNATION_DAYS) {
                return true;
            }
        }
        return false;
    }

    /**
     * The subset of the calendar that is a contact with a degree: eclipses and stations.
     *
     * Split out from annualCalendar because a caller matching events against natal degrees
     * wants exactly these two, and the mundane aspect scan - about four fifths of the
     * calendar's cost - produces nothing such a caller can use. Ingresses are excluded for
     * the same reason: a sign change is not a degree contact.
     */
    public static List<Event> datedMoments(SwissEph sw, double jd0, double jd1) {
        List<Event> out = new ArrayList<>();
        double saved = scanStep;
        try {
            for (String body : RETROGRADERS) {
                scanStep = stepFor(body);
                out.addAll(stations(sw, jd0, jd1, body));
            }
        } finally {
            scanStep = saved;
        }
        out.addAll(eclipses(sw, jd0, jd1));
        out.sort(Comparator.naturalOrder());
        return out;
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
