package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.HarmonicResonance;
import com.zodiacomputing.ourania.astro.Midpoints;
import com.zodiacomputing.ourania.astro.Progressions;
import com.zodiacomputing.ourania.astro.Rulership;
import com.zodiacomputing.ourania.astro.SolarArc;

import de.thmac.swisseph.SwissEph;

import java.util.List;

/**
 * Doors onto engines that were computed, checked, and unreachable.
 *
 * Counting references from this package to each astro class on 2026-09-01 found five with
 * zero: Midpoints, Rulership, DecanSystem, HarmonicResonance and Calibration. All five were
 * implemented, all five were covered by suites, and nothing in the application could show any
 * of them. That is this project's signature defect - the browsable index whose only entry
 * points sat inside a panel that had to be open first, and SavedCharts, a working store the
 * name-list screen never read, were the same shape.
 *
 * Three of those five get a door here. The other two turned out not to want one, which a
 * reference count cannot distinguish from the rest:
 *
 *   DecanSystem is four labels and a getter with no logic behind it, and no reference anywhere
 *       in the tree rather than merely none in this package. A menu entry would open onto
 *       nothing, so it wants implementing or deleting, not a button.
 *   Calibration is weight diagnostics for a domain its own header says has no outcome
 *       variable. It is a developer harness, and a reader is not its audience.
 *
 * The tables render as HTML into the interpretation panel, the way every other reading does.
 */
public final class ChartTables {

    /** The Sun, Moon and angles at one degree is the set Midpoints itself documents. */
    private static final double MIDPOINT_ORB = 1.0;

    private ChartTables() { }

    /**
     * Direct midpoints: where a third body sits on the halfway point between two others.
     *
     * Ebertin's midpoint work is a whole school of practice and it has been sitting behind no
     * button. The engine finds conjunctions and oppositions to the midpoints of the Sun, Moon,
     * Ascendant and Midheaven.
     */
    public static String midpoints(ChartFrame f) {
        StringBuilder h = new StringBuilder();
        h.append("<h1>Midpoints</h1>");
        h.append("<p>A body on the midpoint of two others takes its meaning from both. ")
         .append("These are the direct midpoints of the Sun, Moon, Ascendant and Midheaven, ")
         .append("within ").append(trim(MIDPOINT_ORB)).append("&deg;.</p>");

        List<Midpoints.Hit> hits = Midpoints.findDirectMidpoints(f, MIDPOINT_ORB);
        if (hits.isEmpty()) {
            h.append("<p><i>No body sits on one of those midpoints in this chart. ")
             .append("That is a finding rather than a gap - most charts have few.</i></p>");
            return h.toString();
        }

        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Body", "Midpoint", "Orb"));
        for (Midpoints.Hit hit : hits) {
            String axis = hit.p1 + "/" + hit.p2 + (hit.isOpposition ? " (opposite)" : "");
            h.append(row3("td", hit.activatingBody, axis, trim(hit.separation) + "&deg;"));
        }
        h.append("</table>");
        h.append("<p><i>An opposition to a midpoint activates the same axis, which is why ")
         .append("both are listed.</i></p>");
        return h.toString();
    }

    /**
     * The rulership web, and the receptions inside it.
     *
     * Rulership already builds a printable table and nothing ever called it. Receptions are the
     * more interesting half: two bodies each sitting in the other's dignity is a relationship
     * no single placement shows.
     */
    public static String dispositors(ChartFrame f) {
        StringBuilder h = new StringBuilder();
        h.append("<h1>Dispositors and receptions</h1>");
        h.append(moonCourseNote(f));
        h.append("<p>Every house is ruled by the planet ruling the sign on its cusp. Where that ")
         .append("ruler sits is where the affairs of the house are actually carried out.</p>");

        List<Rulership.Edge> web = Rulership.web(f);
        h.append("<table cellpadding=\"4\">");
        h.append(row4("th", "House", "Cusp sign", "Ruler", "Ruler in house"));
        for (Rulership.Edge e : web) {
            h.append(row4("td",
                    String.valueOf(e.house),
                    signName(e.cuspSign),
                    e.ruler + (e.rulerRetrograde ? " R" : ""),
                    e.rulerHouse == 0 ? "&mdash;" : String.valueOf(e.rulerHouse)));
        }
        h.append("</table>");

        h.append("<h2>Receptions</h2>");
        List<Rulership.Reception> rec = Rulership.receptions(f);
        if (rec.isEmpty()) {
            h.append("<p><i>No receptions in this chart.</i></p>");
            return h.toString();
        }
        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Bodies", "Kind", "Strength"));
        for (Rulership.Reception r : rec) {
            String kind = r.mutual
                    ? "mutual (" + nz(r.aInDignityOfB) + " / " + nz(r.bInDignityOfA) + ")"
                    : "one-way (" + nz(r.bInDignityOfA) + ")";
            h.append(row3("td", r.a + " and " + r.b, kind, String.valueOf(r.strength)));
        }
        h.append("</table>");
        h.append("<p><i>A mutual reception is judged by its weaker leg, so the strength shown ")
         .append("is the floor rather than the better of the two.</i></p>");
        return h.toString();
    }

    /**
     * The five tightest harmonic contacts the classical aspects do not already show.
     *
     * Built 2026-09-01 with 58 checks and no way to reach it. The ranking deliberately does not
     * alert: a chart carries around 780 harmonic contacts, so a notification per contact was
     * measured and abandoned before this class was written.
     */
    public static String resonance(ChartFrame f) {
        StringBuilder h = new StringBuilder();
        h.append("<h1>Harmonic resonance</h1>");
        h.append("<p>Contacts that are exact in a harmonic chart but invisible to the classical ")
         .append("aspects. Ranked by how close to exact they are, tightest first.</p>");

        List<HarmonicResonance.Contact> hits = HarmonicResonance.scan(f);
        if (hits.isEmpty()) {
            h.append("<p><i>No harmonic contact in this chart is tight enough to rank.</i></p>");
            return h.toString();
        }

        h.append("<table cellpadding=\"4\">");
        h.append(row4("th", "Harmonic", "Pair", "Orb", "Separation"));
        for (HarmonicResonance.Contact c : hits) {
            h.append(row4("td",
                    "H" + c.harmonic,
                    c.nameA + " &ndash; " + c.nameB,
                    trim(c.orb) + "&deg;",
                    trim(c.radixSeparation) + "&deg;"));
        }
        h.append("</table>");
        h.append("<p><i>The harmonic shown is the lowest one in which the pair is a conjunction, ")
         .append("so a contact never appears again under a multiple of itself. Separation is ")
         .append("the angle in the chart as cast.</i></p>");
        return h.toString();
    }

    /**
     * The void-of-course Moon, as one sentence.
     *
     * ChartFrame computes this for every chart and nothing in this package read the field.
     * It heads the dispositor page rather than the placements panel, and the reason is cost:
     * generatePlanetPlacementsHtml runs on every chart update and holds no ChartFrame, so
     * surfacing it there would cast a frame per repaint to print one sentence. The dispositor
     * page already has a frame in hand, and a void Moon is a condition of the same kind as the
     * receptions under it - what a traditional reader checks before judging a question.
     */
    public static String moonCourseNote(ChartFrame f) {
        if (f == null || !f.moonVoidOfCourse) {
            return "";
        }
        return "<p><b>The Moon is void of course.</b> It completes no further Ptolemaic aspect "
             + "before leaving the sign it is in. Traditionally that is read as a period in "
             + "which matters do not come to the conclusion they are aimed at.</p>";
    }

    /**
     * The secondary progressed chart, as positions rather than as a list of hits.
     *
     * Progressions was referenced once in this package, inside the year scan that feeds
     * Predict, so its output reached a reader only after being folded into a convergence
     * count. Nothing could show where the progressed bodies actually are - which is what a
     * progressed chart is. Cast at the progressed moment: a day of ephemeris time for a year
     * of life.
     */
    public static String progressed(ChartFrame natal, SwissEph sw, double natalJd, double nowJd) {
        StringBuilder h = new StringBuilder();
        h.append("<h1>Progressed chart</h1>");

        double pjd = Progressions.progressedJd(natalJd, nowJd);
        double years = (nowJd - natalJd) / 365.2422;
        h.append("<p>Secondary progressions advance the chart one day for each year of life. ")
         .append("At ").append(String.format("%.1f", years)).append(" years, the progressed ")
         .append("moment is ").append(String.format("%.1f", pjd - natalJd))
         .append(" days after birth.</p>");

        String phase = Progressions.lunationPhase(sw, natalJd, nowJd);
        if (phase != null && !phase.isEmpty()) {
            h.append("<p><b>Progressed lunation phase:</b> ").append(phase).append("</p>");
        }

        ChartFrame p = ChartFrame.compute(sw, pjd, 0.0, 0.0, (char) 80, false, 0.0);
        h.append("<table cellpadding=\"4\">");
        h.append(row4("th", "Body", "Progressed", "Natal", "Moved"));
        for (ChartFrame.Body pb : p.bodies) {
            if (pb == null || !pb.ok) {
                continue;
            }
            ChartFrame.Body nb = natal.body(pb.name);
            if (nb == null || !nb.ok) {
                continue;
            }
            double moved = com.zodiacomputing.ourania.astro.Zodiac.normalise(pb.lon - nb.lon);
            h.append(row4("td",
                    pb.name + (pb.retrograde ? " R" : ""),
                    position(pb.lon),
                    position(nb.lon),
                    trim(moved > 180.0 ? moved - 360.0 : moved) + "&deg;"));
        }
        h.append("</table>");
        h.append("<p><i>Houses are not shown: progressed angles need the birth time to be a ")
         .append("real time rather than a placeholder, which the chart record does not yet ")
         .append("distinguish. The bodies are unaffected by that.</i></p>");
        return h.toString();
    }

    /**
     * Solar arc directions: the whole chart moved forward by the Sun's own progressed travel.
     *
     * Like Progressions, SolarArc had exactly one reference in this package and it was inside
     * the year scan. The arc itself - one number that moves every point in the chart - was
     * never shown anywhere.
     */
    public static String solarArc(ChartFrame natal, SwissEph sw, double natalJd, double nowJd) {
        StringBuilder h = new StringBuilder();
        h.append("<h1>Solar arc directions</h1>");

        ChartFrame.Body sun = natal.body("Sun");
        if (sun == null || !sun.ok) {
            h.append("<p><i>The natal Sun did not compute, so there is no arc.</i></p>");
            return h.toString();
        }
        double arc = SolarArc.arcAt(sw, natalJd, sun.lon, nowJd);
        if (Double.isNaN(arc)) {
            h.append("<p><i>The ephemeris would not answer for the progressed Sun.</i></p>");
            return h.toString();
        }

        h.append("<p>Every point in the chart is moved forward by the same arc - the distance ")
         .append("the progressed Sun has travelled. The arc today is <b>")
         .append(trim(arc)).append("&deg;</b>.</p>");

        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Point", "Natal", "Directed"));
        for (ChartFrame.Body b : natal.bodies) {
            if (b == null || !b.ok) {
                continue;
            }
            h.append(row3("td", b.name, position(b.lon),
                    position(com.zodiacomputing.ourania.astro.Zodiac.normalise(b.lon + arc))));
        }
        h.append("</table>");
        h.append("<p><i>A directed point is read where it lands, against the natal chart. ")
         .append("The arc is the same for every point, so what varies is what each one ")
         .append("arrives at.</i></p>");
        return h.toString();
    }

    /** Sign and degree, the way the rest of the app writes a position. */
    private static String position(double lon) {
        double d = com.zodiacomputing.ourania.astro.Zodiac.degreeInSign(lon);
        int deg = (int) d;
        int min = (int) Math.round((d - deg) * 60.0);
        if (min == 60) {
            deg++;
            min = 0;
        }
        return String.format("%d&deg;%02d' %s", deg, min,
                com.zodiacomputing.ourania.astro.Zodiac.signName(lon));
    }

    // -------------------------------------------------------------------- shared

    private static final String[] SIGNS = {
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    };

    private static String signName(int index) {
        return index >= 0 && index < SIGNS.length ? SIGNS[index] : "&mdash;";
    }

    private static String nz(String s) {
        return s == null ? "&mdash;" : s;
    }

    private static String trim(double d) {
        return String.format("%.2f", d);
    }

    private static String row3(String cell, String a, String b, String c) {
        return "<tr><" + cell + ">" + a + "</" + cell + "><" + cell + ">" + b + "</" + cell
             + "><" + cell + ">" + c + "</" + cell + "></tr>";
    }

    private static String row4(String cell, String a, String b, String c, String d) {
        return "<tr><" + cell + ">" + a + "</" + cell + "><" + cell + ">" + b + "</" + cell
             + "><" + cell + ">" + c + "</" + cell + "><" + cell + ">" + d + "</" + cell + "></tr>";
    }
}
