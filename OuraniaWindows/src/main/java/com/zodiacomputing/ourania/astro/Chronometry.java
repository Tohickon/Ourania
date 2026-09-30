package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.List;

/**
 * Where a chart stands in time at one moment: the profected year with its months and days,
 * the transit frame and its contacts, the year scan, and the convergence those feed - what
 * the synthesized reading's timing sections are written from.
 *
 * <p><b>Moved out of {@code SkymapPanel}'s reading worker on 30 Sep (M7)</b>, with the year
 * scan that was its private helper, because the phone needs exactly this sequence and a second
 * copy of it is how two readings of the same chart would come to disagree. The desktop worker
 * and the phone both call {@link #at}; nothing here was changed on the way, which a before and
 * after comparison of the full synthesis for three charts confirmed character for character.
 */
public final class Chronometry {

    /** The profected year, with its months and days; null without a moment to read at. */
    public Profection profection;
    /** The sky at the transit moment; null when transits are off. */
    public ChartFrame transit;
    /** The transit frame's contacts to the chart; null when transits are off. */
    public List<Transits.Hit> hits;
    /** The profection year, scanned; null unless asked for and transits are on. */
    public YearScan scan;
    /** What converges in that year; null with the scan. */
    public List<Convergence.Target> convergence;

    private Chronometry() { }

    /**
     * @param sw          the reader's ephemeris, for the profection's solar returns
     * @param natal       the chart
     * @param birthJd     its moment
     * @param nowJd       the moment the reading is made at, NaN for none
     * @param ranked      the chart's bodies by prominence, from {@link BodyScore#rank}
     * @param transitJd   the transit moment, NaN when transits are off
     * @param transitLat  where the transit frame is cast
     * @param transitLon
     * @param natalLat    where the solar return is cast
     * @param natalLon
     * @param houseSystem the reader's house system
     * @param scanYear    whether to scan the year - the deeper readings do, the snapshot not
     */
    public static Chronometry at(SwissEph sw, ChartFrame natal, double birthJd, double nowJd,
            List<BodyScore.Vector> ranked, double transitJd, double transitLat,
            double transitLon, double natalLat, double natalLon, char houseSystem,
            boolean scanYear) {
        Chronometry out = new Chronometry();
        if (!Double.isNaN(nowJd)) {
            out.profection = Profection.at(birthJd, nowJd, natal.asc);
            ChartFrame.Body theSun = natal.body("Sun");
            if (theSun != null && theSun.ok) {
                double from = Profection.solarReturnJd(sw, birthJd, theSun.lon,
                    out.profection.age);
                // The year this chart actually has, not a mean one - see computeSubPeriods for
                // the thirteenth month that came of a round number. One more root-find, once
                // per reading.
                double until = Profection.solarReturnJd(sw, birthJd, theSun.lon,
                    out.profection.age + 1);
                out.profection.computeSubPeriods(nowJd, from, until);
            }
        }
        if (!Double.isNaN(transitJd)) {
            out.transit = ChartFrame.compute(sw, transitJd, transitLat, transitLon, houseSystem,
                false, 0.0);
            out.hits = Transits.toNatal(natal, out.transit, ranked, out.profection.lord);
            if (scanYear) {
                out.scan = scanYear(natal, birthJd, transitJd, out.profection, ranked,
                    natalLat, natalLon, houseSystem);
                // Releasing gates the ranking rather than voting in it, which is what finally
                // puts it inside a reading. The chart carries the lots already, so the gate
                // costs one release walk and no ephemeris.
                Convergence.Gate gate = Convergence.Gate.at(
                    birthJd, natal.lotOfFortune, natal.lotOfSpirit, transitJd);
                out.convergence = Convergence.collect(out.profection, out.scan.perfections,
                    out.scan.events, out.scan.arcs, out.scan.progressions, out.scan.returns,
                    gate, out.scan.mutuals);
            }
        }
        return out;
    }

    /**
     * The profection year from solar return to solar return, scanned: the dated events, the
     * perfections, solar arcs, progressions, the progressed Moon's clock, mutual progressions,
     * the dating hits and the solar return's contacts. {@code SkymapPanel.scanProfectionYear}
     * until 30 Sep, unchanged.
     */
    public static YearScan scanYear(ChartFrame chartFrame, double d, double d2,
            Profection profection, List<BodyScore.Vector> list, double d3, double d4, int n) {
        Returns.Return return_;
        SwissEph swissEph = new SwissEph(Ephemeris.PATH);
        double d5 = Double.NaN;
        double d6 = Double.NaN;
        ChartFrame.Body body = chartFrame.body("Sun");
        if (body != null && body.ok) {
            d5 = Profection.solarReturnJd(swissEph, d, body.lon, profection.age);
            d6 = Profection.solarReturnJd(swissEph, d, body.lon, profection.age + 1);
        }
        if (Double.isNaN(d5) || Double.isNaN(d6) || d6 <= d5) {
            d5 = d2 - 182.6;
            d6 = d2 + 182.6;
        }
        YearScan yearScan = new YearScan();
        yearScan.events = Transits.eventsToNatal(Almanac.datedMoments(swissEph, d5, d6), chartFrame, list, profection.lord);
        yearScan.perfections = Transits.perfectionsOverRange(swissEph, chartFrame, list, profection.lord, d5, d6);
        yearScan.arcs = SolarArc.contacts(swissEph, chartFrame, d, list, profection.lord, d5, d6);
        yearScan.progressions = Progressions.contacts(swissEph, chartFrame, d, list, profection.lord, d5, d6);
        yearScan.moonClock = Progressions.clock(swissEph, d, chartFrame.cusps, d5, d6);
        yearScan.mutuals = Progressions.mutual(swissEph, d, d5, d6);
        // K12 stage 3: the days, for whichever themes turn out to be headlines.
        yearScan.catalysts = Transits.datingHits(swissEph, chartFrame,
            ThemeConvergence.allThemePoints(chartFrame), d5, d6);
        ChartFrame.Body body2 = chartFrame.body("Sun");
        if (body2 != null && body2.ok && (return_ = Returns.solar(swissEph, d, body2.lon, profection.age, d3, d4, n)) != null) {
            yearScan.returns = Returns.contacts(return_, chartFrame, list, profection.lord);
        }
        return yearScan;
    }
}
