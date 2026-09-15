package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.HarmonicResonance;
import com.zodiacomputing.ourania.astro.Midpoints;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Progressions;
import com.zodiacomputing.ourania.astro.Returns;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Draconic;
import com.zodiacomputing.ourania.astro.Rulership;
import com.zodiacomputing.ourania.astro.SolarArc;

import de.thmac.swisseph.SweDate;
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
        h.append(treeSection(f));
        return h.toString();
    }

    /**
     * The midpoint tree: every pair a body sits on, grouped under that body.
     *
     * <b>This is the reading the technique is named for, and it is a different question from
     * the table above.</b> That one asks which bodies occupy the axes of the lights and the
     * angles - six axes. This asks it of every pair in the chart: 29 points make 406 axes, and
     * a body can be found on a dozen of them. Written under the body that occupies them, it
     * reads the way Ebertin wrote it - Venus = Sun/Moon, = Mars/Saturn - which is why the
     * grouping is by the occupying body rather than by the pair.
     *
     * One degree, the same orb the table above uses. Measured on a real chart the tree holds
     * 69 contacts at half a degree, 131 at one, and 250 at two - so a loose orb here does not
     * find more, it finds nearly everything, which tells a reader nothing.
     */
    private static String treeSection(ChartFrame f) {
        List<Midpoints.Hit> hits = Midpoints.tree(f, MIDPOINT_ORB);
        StringBuilder h = new StringBuilder();
        h.append("<h2>Midpoint tree</h2>");
        if (hits.isEmpty()) {
            h.append("<p><i>No body sits on any pair's midpoint within ")
             .append(trim(MIDPOINT_ORB)).append("&deg;.</i></p>");
            return h.toString();
        }

        // Grouped by the occupying body, tightest axis first under each.
        java.util.Map<String, java.util.List<Midpoints.Hit>> byBody =
            new java.util.LinkedHashMap<String, java.util.List<Midpoints.Hit>>();
        for (Midpoints.Hit hit : hits) {
            java.util.List<Midpoints.Hit> row = byBody.get(hit.activatingBody);
            if (row == null) {
                row = new java.util.ArrayList<Midpoints.Hit>();
                byBody.put(hit.activatingBody, row);
            }
            row.add(hit);
        }
        for (java.util.List<Midpoints.Hit> row : byBody.values()) {
            java.util.Collections.sort(row, new java.util.Comparator<Midpoints.Hit>() {
                @Override
                public int compare(Midpoints.Hit x, Midpoints.Hit y) {
                    return Double.compare(x.separation, y.separation);
                }
            });
        }

        h.append("<p>Every pair whose midpoint a body occupies, within ")
         .append(trim(MIDPOINT_ORB)).append("&deg;. ").append(hits.size())
         .append(" in this chart.</p>");
        h.append("<table cellpadding=\"4\">");
        h.append(row2("th", "Body", "Sits on the midpoint of"));
        for (java.util.Map.Entry<String, java.util.List<Midpoints.Hit>> e : byBody.entrySet()) {
            StringBuilder axes = new StringBuilder();
            for (Midpoints.Hit hit : e.getValue()) {
                if (axes.length() > 0) {
                    axes.append("<br>");
                }
                axes.append(hit.p1).append('/').append(hit.p2)
                    .append(" <span style='color:#9FB4C7;'>")
                    .append(trim(hit.separation)).append("&deg;")
                    .append(hit.isOpposition ? ", opposite" : "")
                    .append("</span>");
            }
            h.append(row2("td", e.getKey(), axes.toString()));
        }
        h.append("</table>");
        h.append("<p><i>An axis is read as one statement: the body takes its meaning from ")
         .append("both ends at once. A body on many axes is doing a great deal of work in ")
         .append("the chart, which is the tree's whole point.</i></p>");
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
        h.append(contactSection(natal, sw, natalJd, nowJd, false));
        return h.toString();
    }

    /**
     * Solar arc directions: the whole chart moved forward by the Sun's own progressed travel.
     *
     * Like Progressions, SolarArc had exactly one reference in this package and it was inside
     * the year scan. The arc itself - one number that moves every point in the chart - was
     * never shown anywhere.
     */
    /**
     * The year's returns, each shown to the depth its period earns.
     *
     * Solar and lunar returns are charts in their own right and print their angles and cusps;
     * Mercury, Venus and Mars print two angles and stop. That split is {@link Returns#scopeOf}
     * and the reasoning lives there - this method only renders what the policy decides, so
     * the two cannot drift apart.
     */
    public static String returns(ChartFrame natal, SwissEph sw, double natalJd,
                                 double fromJd, double toJd, double lat, double lon, int hsys) {
        StringBuilder h = new StringBuilder();
        h.append("<h1>Returns</h1>");

        List<BodyScore.Vector> ranked = BodyScore.rank(natal);

        // --- Solar: the year's chart.
        ChartFrame.Body sun = natal.body("Sun");
        if (sun != null && sun.ok) {
            int age = (int) Math.floor((fromJd - natalJd) / 365.2422);
            Returns.Return sr = Returns.solar(sw, natalJd, sun.lon, age, lat, lon, hsys);
            appendReturn(h, "Solar return", sr, natal, ranked);
        }

        // --- Lunar: this month's chart. One is a reading; thirteen is a list, so the window
        // is the caller's and only the first is shown in full.
        ChartFrame.Body moon = natal.body("Moon");
        if (moon != null && moon.ok) {
            List<Returns.Return> lrs =
                Returns.lunar(sw, natalJd, moon.lon, fromJd, toJd, lat, lon, hsys);
            h.append("<p><b>Lunar returns in this window:</b> ").append(lrs.size()).append("</p>");
            if (!lrs.isEmpty()) {
                appendReturn(h, "Lunar return", lrs.get(0), natal, ranked);
            }
        }

        // --- The angles-only three.
        h.append("<h2>Trigger nodes</h2>");
        h.append("<p>Mercury, Venus and Mars return roughly once a year, and their returns are ")
         .append("moments rather than chapters. No house structure is cast for them; their ")
         .append("angles are laid over the natal wheel as triggers.</p>");
        h.append("<table cellpadding=\"4\">");
        h.append(row4("th", "Return", "Date", "Return angle", "On natal"));
        boolean any = false;
        for (String body : new String[]{"Mercury", "Venus", "Mars"}) {
            ChartFrame.Body nb = natal.body(body);
            if (nb == null || !nb.ok) {
                continue;
            }
            for (Returns.Return r : Returns.planetary(sw, body, nb.lon,
                                                      fromJd, toJd, lat, lon, hsys)) {
                List<Returns.Contact> cs = Returns.contacts(r, natal, ranked, null);
                if (cs.isEmpty()) {
                    h.append(row4("td", body, dateOf(r.jd), "&mdash;", "no contact"));
                    any = true;
                    continue;
                }
                for (Returns.Contact c : cs) {
                    h.append(row4("td", body, dateOf(r.jd), c.returnPoint,
                        c.type.label + " " + c.natal
                            + String.format(" (%.1f&deg;)", c.offBy)));
                    any = true;
                }
            }
        }
        if (!any) {
            h.append(row4("td", "&mdash;", "", "", "none in this window"));
        }
        h.append("</table>");
        return h.toString();
    }

    /** One full-wheel return: when it falls, where it was cast, and what it lands on. */
    private static void appendReturn(StringBuilder h, String title, Returns.Return r,
                                     ChartFrame natal, List<BodyScore.Vector> ranked) {
        h.append("<h2>").append(title).append("</h2>");
        if (r == null || r.chart == null) {
            h.append("<p>Not available for this window.</p>");
            return;
        }
        // <b>The place is printed rather than judged.</b> A return's angles are its new
        // information and they move with the native, so whether this is a relocated return
        // is the reader's first question - but ChartFrame does not record a chart's
        // geographic place, and there is no relocation field in the UI yet, so nothing here
        // can honestly label it. Returns.Return now carries the coordinates it was cast for;
        // stating them says exactly as much as is known.
        h.append("<p><b>Exact:</b> ").append(dateOf(r.jd))
         .append(String.format(" &middot; cast for %.2f, %.2f", r.lat, r.lon)).append("</p>");

        h.append("<table cellpadding=\"4\">");
        h.append(row2("th", "Return angle", "Position"));
        h.append(row2("td", "Ascendant", Zodiac.format(r.chart.asc)));
        h.append(row2("td", "MC", Zodiac.format(r.chart.mc)));
        h.append("</table>");

        // <b>Principal and background, not one undifferentiated list.</b> The contacts arrive
        // ordered by weight now, but a reader scanning sixteen rows of identical typography
        // still has to work out which ones have a planet behind them. Splitting them says it
        // once.
        //
        // The shared rule, asked with this surface's direction: a return arrives from outside
        // the chart it lands on, so the return end is the one making the statement. See
        // Bodies.hasPrimaryActor - the reading's lists ask the same question mutually.
        List<Returns.Contact> cs = Returns.contacts(r, natal, ranked, null);
        StringBuilder principal = new StringBuilder();
        StringBuilder background = new StringBuilder();
        for (Returns.Contact c : cs) {
            String cell = row3("td", c.returnPoint, c.type.label,
                c.natal + String.format(" (%.1f&deg;)", c.offBy));
            if (!Bodies.hasPrimaryActor(c.returnPoint, c.natal, true)) {
                background.append(cell);
            } else {
                principal.append(cell);
            }
        }
        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Return point", "Aspect", "Natal point"));
        if (principal.length() == 0) {
            h.append(row3("td", "&mdash;", "", "nothing inside orb"));
        }
        h.append(principal);
        h.append("</table>");
        if (background.length() > 0) {
            h.append("<p><b>Background resonance:</b> <span style='font-size:11px;")
             .append(" color:#B9B2D6;'>a minor body is the one arriving, whatever it lands")
             .append(" on.</span></p>");
            h.append("<table cellpadding=\"4\">");
            h.append(row3("th", "Return point", "Aspect", "Natal point"));
            h.append(background);
            h.append("</table>");
        }
    }

    /**
     * The draconic chart beside the tropical one, which is how it is read.
     *
     * <b>Never alone.</b> A draconic chart on its own is the radix with every longitude
     * shifted by one constant - the same picture, relabelled - so a page of draconic degrees
     * says nothing a tropical chart did not already say. What the technique is for is the
     * comparison: a draconic Sun sitting on a tropical Ascendant is a contact between the two
     * frames, and the pair of columns is the only way to see one.
     */
    public static String draconic(ChartFrame natal) {
        StringBuilder h = new StringBuilder();
        h.append("<h1>Draconic chart</h1>");

        double origin = Draconic.origin(natal);
        if (Double.isNaN(origin)) {
            h.append("<p>This chart has no north node, so there is no draconic origin to ")
             .append("measure from.</p>");
            return h.toString();
        }
        ChartFrame drac = Draconic.of(natal);
        h.append("<p>Measured from the Moon's north node at <b>").append(Zodiac.format(origin))
         .append("</b> rather than from the equinox. Every aspect is identical to the radix - ")
         .append("only the zero has moved - so the node itself sits on 0&deg; Aries.</p>");
        h.append("<p style='color:#9AA5B1; font-size:11px;'>The angles and houses are the ")
         .append("tropical ones. A draconic longitude restates a relationship; it does not ")
         .append("name a place in the sky, and the horizon is a place.</p>");

        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Body", "Draconic", "Tropical"));
        for (int i = 0; i < natal.bodies.length; i++) {
            ChartFrame.Body t = natal.bodies[i];
            ChartFrame.Body d = drac.bodies[i];
            if (t == null || d == null || !t.ok || Bodies.at(i).isAngle()) {
                continue;
            }
            h.append(row3("td", t.name, Zodiac.format(d.lon), Zodiac.format(t.lon)));
        }
        h.append("</table>");

        // <b>The contacts are the reading.</b> A draconic body conjunct a tropical one is the
        // technique's whole claim, so they are found here rather than left to the reader to
        // spot by comparing two columns of degrees.
        h.append("<h2>Draconic on tropical</h2>");
        h.append("<p>Draconic placements sitting on the tropical chart, within 2&deg;.</p>");
        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Draconic", "On tropical", "Orb"));
        int hits = 0;
        for (int i = 0; i < drac.bodies.length; i++) {
            ChartFrame.Body d = drac.bodies[i];
            if (d == null || !d.ok || Bodies.at(i).isAngle()) {
                continue;
            }
            for (int j = 0; j < natal.bodies.length; j++) {
                ChartFrame.Body t = natal.bodies[j];
                if (t == null || !t.ok) {
                    continue;
                }
                double sep = Aspects.separation(d.lon, t.lon);
                if (sep <= 2.0) {
                    h.append(row3("td", d.name, t.name,
                        String.format("%.2f&deg;", sep)));
                    hits++;
                }
            }
        }
        if (hits == 0) {
            h.append(row3("td", "&mdash;", "nothing within 2&deg;", ""));
        }
        h.append("</table>");
        return h.toString();
    }

    /**
     * Declinations, the planets out of bounds, and the parallels. Master list D4.
     *
     * Facts, with no prose beside them: the corpus holds nothing on out-of-bounds planets or
     * parallels, and a sentence invented here to fill the column would be the kind of text
     * the provenance note warns about.
     */
    public static String declinations(ChartFrame natal) {
        com.zodiacomputing.ourania.astro.Declinations.Result r =
            com.zodiacomputing.ourania.astro.Declinations.of(natal);
        StringBuilder h = new StringBuilder();
        h.append("<h1>Declinations</h1>");
        h.append("<p>How far north or south of the equator each planet stands. A planet past ")
         .append("the Sun's furthest reach - ").append(declination(r.obliquity).replace("N", ""))
         .append(" at this chart's date - is out of bounds.</p>");
        // No bold or colour mid-sentence in these panes: Swing's HTML swallows the space at the
        // edge of a styled run, and "is <b>out of bounds</b>" rendered as "iout of bounds".

        // The same facts drawn, before they are listed: a parallel is two planets level with
        // each other, which is seen at a glance and not in a table of pairs. See DeclinationGraph.
        h.append("<p>").append(DeclinationGraph.imgTag(r)).append("</p>");

        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Body", "Declination", ""));
        for (com.zodiacomputing.ourania.astro.Declinations.Entry e : r.entries) {
            h.append(row3("td", e.name, declination(e.declination),
                e.outOfBounds
                    ? "<font color='#E2B258'>out of bounds, "
                        + String.format("%.2f&deg;", e.beyond) + " past</font>"
                    : ""));
        }
        h.append("</table>");

        List<com.zodiacomputing.ourania.astro.Declinations.Entry> oob = r.outOfBounds();
        if (oob.isEmpty()) {
            h.append("<p><i>Nothing in this chart is out of bounds.</i></p>");
        }

        h.append("<h2>Parallels and contraparallels</h2>");
        h.append("<p>Two planets at the same declination: on the same side of the equator a ")
         .append("parallel, read like a conjunction; on opposite sides a contraparallel, read ")
         .append("like an opposition. Within ")
         .append(trim(com.zodiacomputing.ourania.astro.Declinations.parallelOrb))
         .append("&deg;.</p>");
        if (r.contacts.isEmpty()) {
            h.append("<p><i>No parallels or contraparallels within orb.</i></p>");
            return h.toString();
        }
        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Pair", "Contact", "Orb"));
        for (com.zodiacomputing.ourania.astro.Declinations.Contact c : r.contacts) {
            h.append(row3("td", c.a + " &ndash; " + c.b,
                c.contra ? "contraparallel" : "parallel",
                String.format("%.2f&deg;", c.off)));
        }
        h.append("</table>");
        return h.toString();
    }

    /**
     * Antiscia and contra-antiscia: each point's two mirror degrees, and who stands on them.
     * Master list D8. Facts only, for the reason {@link #declinations} gives.
     */
    public static String antiscia(ChartFrame natal) {
        com.zodiacomputing.ourania.astro.Antiscia.Result r =
            com.zodiacomputing.ourania.astro.Antiscia.of(natal);
        StringBuilder h = new StringBuilder();
        h.append("<h1>Antiscia</h1>");
        h.append("<p>Each degree mirrored across the solstice axis, 0&deg; Cancer to ")
         .append("0&deg; Capricorn, where two points share the same length of day - its ")
         .append("antiscion - and across the equinox axis, 0&deg; Aries to 0&deg; Libra - its ")
         .append("contra-antiscion.</p>");

        h.append("<table cellpadding=\"4\">");
        h.append(row4("th", "Point", "Position", "Antiscion", "Contra-antiscion"));
        for (com.zodiacomputing.ourania.astro.Antiscia.Point p : r.points) {
            h.append(row4("td", p.name, position(p.longitude), position(p.antiscion),
                position(p.contraAntiscion)));
        }
        h.append("</table>");

        h.append("<h2>Points on another's mirror</h2>");
        h.append("<p>A point on another's antiscion is read like a conjunction; on its ")
         .append("contra-antiscion, like an opposition. Within ")
         .append(trim(com.zodiacomputing.ourania.astro.Antiscia.orb)).append("&deg;.</p>");
        if (r.contacts.isEmpty()) {
            h.append("<p><i>No point stands on another's antiscion or contra-antiscion ")
             .append("within orb.</i></p>");
            return h.toString();
        }
        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Pair", "Contact", "Orb"));
        for (com.zodiacomputing.ourania.astro.Antiscia.Contact c : r.contacts) {
            h.append(row3("td", c.a + " &ndash; " + c.b,
                c.contra ? "contra-antiscion" : "antiscion",
                String.format("%.2f&deg;", c.off)));
        }
        h.append("</table>");
        return h.toString();
    }

    /**
     * Fixed stars: the chart points standing on a star, then where every listed star is.
     * Master list D9. Facts only - the corpus has no star prose.
     */
    public static String fixedStars(ChartFrame natal, SwissEph sw) {
        com.zodiacomputing.ourania.astro.FixedStars.Result r =
            com.zodiacomputing.ourania.astro.FixedStars.of(sw, natal);
        StringBuilder h = new StringBuilder();
        h.append("<h1>Fixed stars</h1>");
        if (r.stars.isEmpty()) {
            h.append("<p>No star could be placed. The star catalogue, sefstars.txt, is not in ")
             .append(com.zodiacomputing.ourania.astro.Ephemeris.PATH).append(".</p>");
            return h.toString();
        }
        h.append("<p>The fifteen Behenian stars, the four royal stars and the bright stars read ")
         .append("most often, at this chart's moment. A chart point within ")
         .append(trim(com.zodiacomputing.ourania.astro.FixedStars.orb))
         .append("&deg; of a star's longitude stands on it.</p>");

        h.append("<h2>On a star</h2>");
        if (r.contacts.isEmpty()) {
            h.append("<p><i>No chart point stands on a listed star within orb.</i></p>");
        } else {
            h.append("<table cellpadding=\"4\">");
            h.append(row3("th", "Point", "Star", "Orb"));
            for (com.zodiacomputing.ourania.astro.FixedStars.Contact c : r.contacts) {
                h.append(row3("td", c.point, c.star.name, String.format("%.2f&deg;", c.off)));
            }
            h.append("</table>");
        }

        h.append("<h2>The stars</h2>");
        h.append("<table cellpadding=\"4\">");
        h.append(row3("th", "Star", "Position", "Tradition"));
        for (com.zodiacomputing.ourania.astro.FixedStars.Star s : r.stars) {
            h.append(row3("td", s.name, position(s.longitude), s.tradition));
        }
        h.append("</table>");
        if (!r.missing.isEmpty()) {
            h.append("<p><i>Not in the catalogue: ").append(String.join(", ", r.missing))
             .append(".</i></p>");
        }
        h.append("<p style='color:#9AA5B1; font-size:11px;'>Conjunction by longitude only. ")
         .append("Parans, a star rising or culminating with a planet, are not computed.</p>");
        return h.toString();
    }

    /** A declination the way almanacs print it: degrees and minutes, N or S. */
    static String declination(double dec) {
        double a = Math.abs(dec);
        int deg = (int) a;
        int min = (int) Math.round((a - deg) * 60.0);
        if (min == 60) {
            deg++;
            min = 0;
        }
        return String.format("%d&deg;%02d' %s", deg, min, dec < 0 ? "S" : "N");
    }

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
        h.append(contactSection(natal, sw, natalJd, nowJd, true));
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

    /**
     * The window both contact tables scan: six months either side of the moment.
     *
     * The year scan uses the solar return year, which is right for a profection reading
     * because that is the unit profection counts in. These tables are not a profection
     * reading - they answer "what is my progressed chart doing", and a symmetric window
     * around now is the honest answer to that. Named rather than inlined so the two tables
     * cannot drift apart.
     */
    private static final double CONTACT_HALF_WINDOW = 182.6;

    /** Ranked bodies and the year's lord, which both contact scans need to filter targets. */
    private static String contactSection(ChartFrame natal, SwissEph sw, double natalJd,
                                         double nowJd, boolean arc) {
        StringBuilder h = new StringBuilder();
        List<BodyScore.Vector> ranked;
        String lord;
        try {
            ranked = BodyScore.rank(natal);
            lord = Profection.at(natalJd, nowJd, natal.asc).lord;
        } catch (RuntimeException e) {
            return "";
        }
        double from = nowJd - CONTACT_HALF_WINDOW;
        double to = nowJd + CONTACT_HALF_WINDOW;

        h.append("<h2>Perfecting within the year</h2>");
        h.append("<p>Contacts to natal points that reach exactness between ")
         .append(dateOf(from)).append(" and ").append(dateOf(to))
         .append(". Filtered to points that carry weight in the chart, with the lord of the ")
         .append("year (").append(lord == null ? "none" : lord).append(") given priority - ")
         .append("the same filter the Predict reading uses, so the two agree.</p>");

        h.append("<table cellpadding=\"4\">");
        h.append(row4("th", "Date", arc ? "Directed" : "Progressed", "Aspect", "Natal"));
        int rows = 0;
        if (arc) {
            for (SolarArc.Contact c : SolarArc.contacts(sw, natal, natalJd, ranked, lord, from, to)) {
                h.append(row4("td", dateOf(c.jd), c.directed, label(c.type), c.natal));
                rows++;
            }
        } else {
            for (Progressions.Contact c
                    : Progressions.contacts(sw, natal, natalJd, ranked, lord, from, to)) {
                h.append(row4("td", dateOf(c.jd), c.progressed + (c.retrograde ? " R" : ""),
                        label(c.type), c.natal));
                rows++;
            }
        }
        h.append("</table>");
        if (rows == 0) {
            return "<h2>Perfecting within the year</h2><p><i>Nothing perfects on a "
                 + "significant natal point inside this window. For solar arc that is common - "
                 + "the arc moves about a degree a year, so a point either arrives or it does "
                 + "not.</i></p>";
        }
        return h.toString();
    }

    private static String label(Aspects.Type t) {
        return t == null ? "&mdash;" : t.label;
    }

    /** Calendar date of a Julian day, in UT - the same form the report prints. */
    private static String dateOf(double jd) {
        de.thmac.swisseph.SweDate sd = new de.thmac.swisseph.SweDate();
        sd.setJulDay(jd);
        return String.format("%04d-%02d-%02d", sd.getYear(), sd.getMonth(), sd.getDay());
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

    private static String row2(String cell, String a, String b) {
        return "<tr><" + cell + " valign=\"top\">" + a + "</" + cell + "><" + cell + ">"
             + b + "</" + cell + "></tr>";
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
