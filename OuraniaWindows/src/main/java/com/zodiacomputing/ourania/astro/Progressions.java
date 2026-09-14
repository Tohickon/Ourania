package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * L8: secondary progressions.
 *
 * One day after birth equals one year of life. The progressed chart for a date is the real
 * chart for {@code birthJD + ageInYears}, read as if it were now.
 *
 * What the spec says actually moves usefully, and what this therefore reports:
 *
 * <ul>
 *   <li><b>The progressed Moon</b> - about 2.5 years per sign, a full cycle in 27-28 years.
 *       The workhorse, and the reason progressions are worth having at all.</li>
 *   <li><b>The progressed Sun</b> - about a degree a year, so a sign change roughly every
 *       thirty years. Rare and therefore significant when it happens.</li>
 *   <li><b>The progressed lunation phase</b> - a 29.5-year cycle through eight phases.</li>
 * </ul>
 *
 * <h3>Progressed angles are deliberately absent</h3>
 *
 * The spec: progressed angles "need an exact birth time and degrade fast without one".
 *
 * <b>Corrected 2026-08-08.</b> This exclusion was first justified by saying the app has no
 * exact birth times, citing {@link Profection}. That was wrong, or at least too broad. A user
 * may well have an exact time, and the check suites pin one - the synthetic reference chart
 * they share. What has no real time is the app's <i>default placeholder chart</i>, which is a
 * data-entry gap, not a limit on what can be computed.
 *
 * So the honest position: progressed angles are legitimate for any chart with a known birth
 * time, and are excluded here only because nothing yet distinguishes a chart with a real time
 * from one carrying the placeholder. Add that distinction and this exclusion should be lifted
 * rather than defended. Natal angles remain legitimate <i>targets</i> either way, on the same
 * footing as for every other technique.
 *
 * <h3>The solar arc inversion does not transfer, and that is the interesting part</h3>
 *
 * {@link SolarArc} needs no scanning because one arc moves the whole chart, so each pair can
 * be asked "what arc value would make you exact" and answered with a subtraction. The work
 * plan predicted the same trick would apply here. <b>It does not.</b> Every progressed body
 * moves at its own rate, so there is no single shared quantity to invert - the question
 * "what value would make you exact" has a different answer per body and no common variable
 * to solve for.
 *
 * What does transfer is the cheaper half of the lesson. A one-year window is one <i>day</i>
 * of progressed ephemeris time, so the whole search space is tiny: sample each body once
 * across that day, cache it, and the root finder never asks the ephemeris twice for the same
 * instant. That is the same fix that took the transit perfection scan from 35 seconds to 300
 * milliseconds, and it is what keeps this cheap.
 */
public final class Progressions {

    /** Shared with {@link SolarArc} rather than restated, so the rate cannot drift. */
    public static final double DAYS_PER_YEAR = SolarArc.DAYS_PER_YEAR;

    /**
     * The bodies whose progressed motion is fast enough for a date to mean something.
     *
     * Beyond Mars, progressed motion falls under a tenth of a degree a year: progressed
     * Jupiter takes a decade to cross one degree. Such a contact is exact for years, so
     * naming the day it perfected is spurious precision - the technique's own weakness is
     * that it is "good at when and bad at for how long", and past Mars it stops being good
     * at when either.
     */
    public static String[] progressedBodies = {
        "Sun", "Moon", "Mercury", "Venus", "Mars"
    };

    /** The eight phases of the lunation cycle, in order from the conjunction. */
    public static final String[] PHASES = {
        "new", "crescent", "first quarter", "gibbous",
        "full", "disseminating", "last quarter", "balsamic"
    };

    /** A progressed body reaching exactness on a natal point. */
    public static final class Contact {
        public String progressed;
        public String natal;
        public Aspects.Type type;
        public double jd;
        public boolean retrograde;
        public String why;
        public int natalRank = -1;

        @Override
        public String toString() {
            return String.format("progressed %s%s %s natal %s exact",
                progressed, retrograde ? " Rx" : "", type.label.toLowerCase(), natal);
        }
    }

    /** The progressed Moon entering a sign, or the progressed lunation changing phase. */
    public static final class Change {
        public double jd;
        /** "sign" or "phase". */
        public String kind;
        /** The sign or phase entered. */
        public String entered;
        public String body;

        @Override
        public String toString() {
            return "sign".equals(kind)
                ? String.format("progressed %s enters %s", body, capitalise(entered))
                : String.format("progressed lunation turns %s", entered);
        }
    }

    private Progressions() { }

    /** The progressed moment for a target date: a day for a year. */
    public static double progressedJd(double natalJd, double targetJd) {
        return natalJd + (targetJd - natalJd) / DAYS_PER_YEAR;
    }

    /**
     * Memoised progressed longitude of one body, as a function of real time.
     *
     * Keyed on the real instant rounded to a microsecond of a day, so the coarse scan hits
     * the cache and only bisection falls through to the ephemeris.
     */
    private static final class ProgressedLon {
        private final SwissEph sw;
        private final String body;
        private final double natalJd;
        private final java.util.HashMap<Long, Double> memo = new java.util.HashMap<>();

        ProgressedLon(SwissEph sw, String body, double natalJd) {
            this.sw = sw;
            this.body = body;
            this.natalJd = natalJd;
        }

        double at(double jd) {
            long key = Math.round(jd * 1.0e6);
            Double v = memo.get(key);
            if (v == null) {
                v = Almanac.bodyLongitude(sw, progressedJd(natalJd, jd), body);
                memo.put(key, v);
            }
            return v;
        }
    }

    /**
     * Coarse step, in real days, for scanning a progressed body against a fixed degree.
     *
     * Set by the fastest progressed motion. The progressed Moon covers about 13 degrees a
     * year, which is 0.036 degrees per real day, so a ten-day step moves it about a third of
     * a degree - far too little to cross an aspect and come back unseen. Everything else is
     * slower still, so one step is safe for all of them.
     */
    private static final double SCAN_STEP_DAYS = 10.0;

    /**
     * Progressed contacts to significant natal points perfecting inside the window.
     *
     * @param natalJd birth moment, Julian day UT.
     * @param jdFrom  window start, and
     * @param jdTo    window end - the same window the other L8 techniques use, which is what
     *                makes the convergence count meaningful.
     */
    public static List<Contact> contacts(SwissEph sw, ChartFrame natal, double natalJd,
                                         List<BodyScore.Vector> ranked, String lord,
                                         double jdFrom, double jdTo, int topN) {
        List<Contact> out = new ArrayList<>();
        if (natal == null) {
            return out;
        }
        List<Transits.NatalTarget> targets =
            Transits.significantTargets(natal, ranked, lord, topN);

        for (String body : progressedBodies) {
            ProgressedLon cache = new ProgressedLon(sw, body, natalJd);
            for (Transits.NatalTarget target : targets) {
                for (Aspects.Type type : Aspects.Type.values()) {
                    for (double jd : perfections(cache, target.lon, type, jdFrom, jdTo)) {
                        Contact c = new Contact();
                        c.progressed = body;
                        c.natal = target.name;
                        c.type = type;
                        c.jd = jd;
                        c.why = target.why;
                        c.natalRank = target.rank;
                        c.retrograde = progressedSpeed(sw, natalJd, jd, body) < 0.0;
                        out.add(c);
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble((Contact c) -> c.jd).thenComparing(c -> c.natal));
        return out;
    }

    /** Convenience overload with the standard filter settings. */
    public static List<Contact> contacts(SwissEph sw, ChartFrame natal, double natalJd,
                                         List<BodyScore.Vector> ranked, String lord,
                                         double jdFrom, double jdTo) {
        return contacts(sw, natal, natalJd, ranked, lord, jdFrom, jdTo,
            Transits.defaultTopN);
    }

    /** Exact moments at which one progressed body reaches one aspect to a fixed degree. */
    private static List<Double> perfections(ProgressedLon cache, double natalLon,
                                            Aspects.Type type, double jdFrom, double jdTo) {
        List<Double> targets = new ArrayList<>();
        targets.add(type.exactAngle);
        if (type.exactAngle != 0.0 && type.exactAngle != 180.0) {
            targets.add(-type.exactAngle);
        }
        List<Double> out = new ArrayList<>();
        for (double target : targets) {
            Almanac.OfTime f = jd -> Almanac.signedDelta(
                Almanac.signedDelta(cache.at(jd), natalLon), target);
            out.addAll(Almanac.roots(f, jdFrom, jdTo, SCAN_STEP_DAYS));
        }
        out.sort(Comparator.naturalOrder());

        List<Double> deduped = new ArrayList<>();
        for (double jd : out) {
            // A progressed body covers at most 0.04 deg/day, so two roots within a fortnight
            // are one perfection found twice near a progressed station.
            if (deduped.isEmpty() || jd - deduped.get(deduped.size() - 1) > 14.0) {
                deduped.add(jd);
            }
        }
        return deduped;
    }

    /**
     * The progressed lunation phase at a date: the progressed Moon's angle ahead of the
     * progressed Sun, in eighths.
     *
     * Measured Moon minus Sun rather than the reverse, because the cycle runs from the
     * conjunction forwards through waxing to the opposition and back - the Moon is the one
     * that pulls ahead.
     */
    public static String lunationPhase(SwissEph sw, double natalJd, double targetJd) {
        double p = progressedJd(natalJd, targetJd);
        double moon = Almanac.bodyLongitude(sw, p, "Moon");
        double sun = Almanac.bodyLongitude(sw, p, "Sun");
        if (Double.isNaN(moon) || Double.isNaN(sun)) {
            return "";
        }
        double angle = Zodiac.normalise(moon - sun);
        return PHASES[(int) Math.floor(angle / 45.0) % 8];
    }

    /**
     * Progressed Moon sign ingresses and progressed lunation phase changes inside the window.
     *
     * These are the two dated things the progressed Moon produces on its own, without
     * reference to a natal point. Roughly one sign ingress every 2.5 years and one phase
     * change every 3.7 years, so most windows contain neither and some contain one.
     */
    public static List<Change> changes(SwissEph sw, double natalJd,
                                       double jdFrom, double jdTo) {
        List<Change> out = new ArrayList<>();
        ProgressedLon moon = new ProgressedLon(sw, "Moon", natalJd);
        ProgressedLon sun = new ProgressedLon(sw, "Sun", natalJd);

        // Sign ingresses: the boundary crossed is the start of the sign arrived in.
        double prev = moon.at(jdFrom);
        for (double t = jdFrom + SCAN_STEP_DAYS; t <= jdTo; t += SCAN_STEP_DAYS) {
            double cur = moon.at(t);
            if (Double.isNaN(prev) || Double.isNaN(cur)) {
                prev = cur;
                continue;
            }
            int sPrev = Zodiac.signIndex(prev);
            int sCur = Zodiac.signIndex(cur);
            if (sPrev != sCur) {
                boolean forward = Almanac.signedDelta(cur, prev) > 0;
                final double boundary = (forward ? sCur : sPrev) * 30.0;
                double jd = Almanac.bisect(
                    x -> Almanac.signedDelta(moon.at(x), boundary), t - SCAN_STEP_DAYS, t);
                Change c = new Change();
                c.jd = jd;
                c.kind = "sign";
                c.body = "Moon";
                c.entered = Zodiac.SIGNS[sCur];
                out.add(c);
            }
            prev = cur;
        }

        // Phase changes: the Moon-Sun angle crossing a multiple of 45 degrees.
        for (int eighth = 0; eighth < 8; eighth++) {
            final double boundary = eighth * 45.0;
            Almanac.OfTime f = x -> Almanac.signedDelta(
                Almanac.signedDelta(moon.at(x), sun.at(x)), boundary);
            for (double jd : Almanac.roots(f, jdFrom, jdTo, SCAN_STEP_DAYS)) {
                Change c = new Change();
                c.jd = jd;
                c.kind = "phase";
                c.body = "Moon";
                c.entered = PHASES[eighth];
                out.add(c);
            }
        }
        out.sort(Comparator.comparingDouble(c -> c.jd));
        return out;
    }

    /** Longitude speed of a progressed body, for the retrograde flag. NaN if unknown. */
    private static double progressedSpeed(SwissEph sw, double natalJd, double jd, String body) {
        int ipl = Almanac.iplOf(body);
        if (ipl < 0) {
            return Double.NaN;
        }
        double[] xx = new double[6];
        StringBuffer err = new StringBuffer();
        int flags = de.thmac.swisseph.SweConst.SEFLG_SWIEPH
            | de.thmac.swisseph.SweConst.SEFLG_SPEED;
        if (sw.swe_calc_ut(progressedJd(natalJd, jd), ipl, Ephemeris.flags(sw, flags), xx, err)
                == de.thmac.swisseph.SweConst.ERR) {
            return Double.NaN;
        }
        return xx[3];
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
