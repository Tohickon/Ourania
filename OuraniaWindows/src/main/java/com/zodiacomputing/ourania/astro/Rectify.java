package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finding a birth time from what happened afterwards (G8).
 *
 * <p><b>The raw material was all here and there was no method.</b> The scrub bar already moves a
 * birth time against a live wheel and the Rodden rating already says how well the time is known;
 * what was missing is the thing that makes rectification a technique rather than a fiddle - dated
 * events, and a score.
 *
 * <p><b>It rectifies against the ANGLES, because they are the only thing a birth time moves
 * much.</b> Four minutes of clock is about one degree of Midheaven, and one degree of directed arc
 * is about a year of life - so an hour of uncertainty is fifteen degrees of angle and fifteen years
 * of misdated events, while the Sun has moved two and a half minutes of arc and every planet less.
 * A method scoring planetary positions would be scoring noise.
 *
 * <p><b>It reports a ranking and a spread, never an answer.</b> The honest output of a
 * rectification is "these times fit, these do not, and here is how much better the best one is" -
 * and when the events do not discriminate, the spread says so. A program that named a birth time
 * to the minute from three vague memories would be lying, and a practitioner would believe it
 * because it came from a computer. {@link Result#confident} is the guard on that.
 *
 * <p><b>What this is not:</b> it does not weigh the events against each other, it does not use
 * transits or profections, and it does not consider converse directions. Those are all real parts
 * of the technique and all of them are judgement; this is the arithmetic underneath, which is the
 * part a program can do without pretending.
 */
public final class Rectify {

    private Rectify() { }

    /** The aspects a direction is read on. Traditional, and the ones that time an event. */
    static final double[] ANGLES_OF_ASPECT = {0.0, 60.0, 90.0, 120.0, 180.0};

    /** How far from exact a directed contact may be and still count, in degrees. */
    public static double orb = 1.0;

    /** One dated thing that happened, and what in the chart it should have touched. */
    public static final class Event {
        public final String what;
        public final double jd;
        /** A registry body name the event should have contacted, or "" for any of them. */
        public final String body;

        public Event(String what, double jd, String body) {
            this.what = what;
            this.jd = jd;
            this.body = body == null ? "" : body;
        }
    }

    /** One birth time considered, with what it would make of the events. */
    public static final class Candidate implements Comparable<Candidate> {
        public double jd;
        /** Hours after midnight, local to the place, as the time was offered. */
        public double hour;
        public double score;
        public final List<String> reasons = new ArrayList<>();

        @Override
        public int compareTo(Candidate o) {
            return Double.compare(o.score, this.score);
        }
    }

    /** The whole scan: every time tried, best first, and whether it decided anything. */
    public static final class Result {
        public final List<Candidate> candidates = new ArrayList<>();
        /** The best score less the median, over the spread - how much the best one stands out. */
        public double separation;
        /** How many events were weighed. Below {@link #minimumEvents} nothing is confident. */
        public int events;
        /**
         * Whether the events discriminate at all.
         *
         * <b>False is a real answer and the common one.</b> Three events that every candidate
         * time fits equally well have told you nothing, and saying so is the whole difference
         * between a method and a number.
         */
        public boolean confident;

        public Candidate best() {
            return candidates.isEmpty() ? null : candidates.get(0);
        }
    }

    /** Below this, the best time is not meaningfully better than the middle of the field. */
    public static double confidenceFloor = 0.25;

    /**
     * How many dated events it takes before a scan may call itself confident.
     *
     * <b>A separation test alone is not enough, which the check proved.</b> One vague event over a
     * whole day measured a separation of 0.50 - twice the floor - because most candidate times
     * score nothing at all and a handful score well, so the best stands far above the median. The
     * spread was real and the conclusion was worthless: one contact fits one time no better than
     * coincidence fits anything. Three independent events is what the technique has always asked
     * for, and it is a rule about evidence rather than about arithmetic, so no amount of tuning
     * the floor would have produced it.
     */
    public static int minimumEvents = 3;

    /**
     * Every birth time in a window, scored against the events.
     *
     * @param sw      the ephemeris
     * @param year    the birth date, which rectification takes as known
     * @param lat     where, which rectification also takes as known - the angles depend on it
     * @param fromHour first birth time to try, in hours after midnight UT
     * @param toHour   last
     * @param stepMinutes how finely to step. Four minutes is one degree of Midheaven.
     */
    public static Result scan(SwissEph sw, int year, int month, int day,
                              double lat, double lon,
                              double fromHour, double toHour, int stepMinutes,
                              List<Event> events) {
        Result r = new Result();
        if (events == null || events.isEmpty() || stepMinutes <= 0 || toHour < fromHour) {
            return r;
        }
        for (double h = fromHour; h <= toHour + 1e-9; h += stepMinutes / 60.0) {
            double jd = new de.thmac.swisseph.SweDate(year, month, day, h).getJulDay();
            ChartFrame f = ChartFrame.compute(sw, jd, lat, lon, 'P', false, 0.0);
            Candidate c = new Candidate();
            c.jd = jd;
            c.hour = h;
            c.score = scoreOf(sw, f, jd, events, c.reasons);
            r.candidates.add(c);
        }
        Collections.sort(r.candidates);

        // <b>How far the best stands above the middle, scaled by the whole spread.</b> A raw best
        // score says nothing: every candidate scores well when the orb is generous, and the
        // question is whether this time is better than that one.
        if (r.candidates.size() >= 3) {
            double best = r.candidates.get(0).score;
            double worst = r.candidates.get(r.candidates.size() - 1).score;
            double median = r.candidates.get(r.candidates.size() / 2).score;
            double spread = best - worst;
            r.separation = spread <= 1e-9 ? 0.0 : (best - median) / spread;
        }
        r.events = events.size();
        // Both, and the second is not negotiable by the first: a huge separation from one event
        // is one coincidence, not a rectification.
        r.confident = r.events >= minimumEvents && r.separation >= confidenceFloor;
        return r;
    }

    /**
     * What one birth time makes of the events.
     *
     * <b>Each event scores its single closest contact, not all of them.</b> Summing every contact
     * within orb rewards a time whose angles happen to sit in a crowded part of the chart, which
     * is a property of the chart and not evidence about the birth time.
     */
    private static double scoreOf(SwissEph sw, ChartFrame f, double natalJd,
                                  List<Event> events, List<String> reasons) {
        double total = 0.0;
        double natalSun = sunOf(f);
        if (Double.isNaN(natalSun)) {
            return 0.0;
        }
        for (Event e : events) {
            double arc = SolarArc.arcAt(sw, natalJd, natalSun, e.jd);
            if (Double.isNaN(arc)) {
                continue;
            }
            double best = 0.0;
            String why = "";
            for (String angle : new String[] {"Ascendant", "Midheaven"}) {
                double directed = Zodiac.normalise(
                    ("Ascendant".equals(angle) ? f.asc : f.mc) + arc);
                for (int i = 0; i < Bodies.count(); i++) {
                    Bodies.Def d = Bodies.at(i);
                    if (!e.body.isEmpty() && !e.body.equalsIgnoreCase(d.name)) {
                        continue;
                    }
                    ChartFrame.Body b = f.bodies[i];
                    if (b == null || !b.ok) {
                        continue;
                    }
                    for (double aspect : ANGLES_OF_ASPECT) {
                        double off = Math.abs(separation(directed, b.lon) - aspect);
                        if (off <= orb) {
                            double hit = 1.0 - off / orb;
                            if (hit > best) {
                                best = hit;
                                why = e.what + ": directed " + angle + " "
                                    + name(aspect) + " " + d.name
                                    + String.format(" (%.2f°)", off);
                            }
                        }
                    }
                }
            }
            total += best;
            if (!why.isEmpty() && reasons != null) {
                reasons.add(why);
            }
        }
        return total;
    }

    /** The natal Sun's longitude, which the arc is measured from. */
    private static double sunOf(ChartFrame f) {
        int i = Bodies.indexOf("sun");
        if (i < 0 || f.bodies[i] == null || !f.bodies[i].ok) {
            return Double.NaN;
        }
        return f.bodies[i].lon;
    }

    private static double separation(double a, double b) {
        double d = Math.abs(Zodiac.normalise(a) - Zodiac.normalise(b)) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    private static String name(double aspect) {
        if (aspect == 0.0) {
            return "conjunct";
        }
        if (aspect == 60.0) {
            return "sextile";
        }
        if (aspect == 90.0) {
            return "square";
        }
        if (aspect == 120.0) {
            return "trine";
        }
        return "opposite";
    }
}
