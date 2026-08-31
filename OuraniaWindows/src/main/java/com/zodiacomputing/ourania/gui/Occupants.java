package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which chart points fall inside a span of the zodiac.
 *
 * Clicking a decan or a Sabian degree used to describe that slice of zodiac in the
 * abstract, with no way to tell whether anything of yours was actually standing in it -
 * which is the first thing a reader wants to know and the one thing the panel could not
 * say. This answers it, for the natal wheel and the transit wheel both.
 *
 * Covers every registered point including the angles: the Ascendant sitting in the degree
 * you just clicked is exactly the kind of thing worth being told about, and it is not a
 * "body" anywhere else in the app.
 */
public final class Occupants {

    private Occupants() { }

    /** One point standing in the span, with what it takes to render a line about it. */
    public static final class Hit {
        public final String name;
        public final String glyph;
        public final double longitude;
        public final boolean retrograde;

        Hit(String name, String glyph, double longitude, boolean retrograde) {
            this.name = name;
            this.glyph = glyph;
            this.longitude = longitude;
            this.retrograde = retrograde;
        }
    }

    /**
     * Every valid point of a computed chart whose longitude lies in [start, start + span).
     *
     * Half-open on purpose: a body exactly on a boundary belongs to the slice it is
     * entering, not the one it just left, so 10.000 degrees of a sign is the first degree
     * of the second decan and is reported once rather than twice. Verified by partition -
     * summing every sign, every decan and every degree each returns the whole chart.
     *
     * @param start span start in degrees of the zodiac, normalised internally
     * @param span  width in degrees - 30 for a sign, 10 for a decan, 1 for a degree
     */
    public static List<Hit> inSpan(ChartFrame f, double start, double span) {
        if (f == null || f.bodies == null) {
            return Collections.emptyList();
        }
        List<Hit> out = new ArrayList<>();
        double from = Zodiac.normalise(start);
        for (int i = 0; i < f.bodies.length && i < Bodies.count(); i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b == null || !b.ok || Double.isNaN(b.lon)) {
                continue;
            }
            if (Zodiac.normalise(b.lon - from) < span) {
                out.add(new Hit(b.name, Bodies.at(i).glyph, b.lon, b.retrograde));
            }
        }
        return sorted(out, from);
    }

    /**
     * The same question asked of the wheel's parallel arrays rather than a ChartFrame.
     *
     * The transit wheel is held as longitude/speed/validity arrays indexed by the registry
     * and never assembled into a frame, so this exists to avoid recomputing a whole chart
     * just to answer "what is standing here". Retrograde is taken from the sign of the
     * speed, which is what it means everywhere else in the app.
     *
     * @param speed may be null, in which case nothing is reported as retrograde
     */
    public static List<Hit> inSpan(double[] lon, boolean[] valid, double[] speed,
                                   double start, double span) {
        if (lon == null || valid == null) {
            return Collections.emptyList();
        }
        List<Hit> out = new ArrayList<>();
        double from = Zodiac.normalise(start);
        for (int i = 0; i < lon.length && i < valid.length && i < Bodies.count(); i++) {
            if (!valid[i] || Double.isNaN(lon[i])) {
                continue;
            }
            if (Zodiac.normalise(lon[i] - from) < span) {
                Bodies.Def d = Bodies.at(i);
                boolean retro = speed != null && i < speed.length && speed[i] < 0.0;
                out.add(new Hit(d.name, d.glyph, lon[i], retro));
            }
        }
        return sorted(out, from);
    }

    /** In the order a reader meets them going forward through the span. */
    private static List<Hit> sorted(List<Hit> hits, double from) {
        hits.sort((x, y) -> Double.compare(
            Zodiac.normalise(x.longitude - from), Zodiac.normalise(y.longitude - from)));
        return hits;
    }

    /**
     * An HTML block naming what stands in the span, or "" when nothing does.
     *
     * Returns empty rather than "nothing here" so a caller can simply append it: a slice
     * of zodiac with nothing in it is the ordinary case, and saying so every time would
     * add a line of noise to eleven panels out of twelve.
     *
     * The two wheels are listed separately and labelled, because "Saturn is here" means
     * something quite different depending on whether it has been there since birth or
     * arrived last week.
     *
     * @param transit may be empty - it always is when no transit wheel is displayed
     */
    public static String html(List<Hit> natal, List<Hit> transit, String what) {
        boolean hasNatal = natal != null && !natal.isEmpty();
        boolean hasTransit = transit != null && !transit.isEmpty();
        if (!hasNatal && !hasTransit) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<h2 style='color:#00BFFF; margin-top:20px;'>In this ").append(what)
          .append("</h2>");
        if (hasNatal) {
            section(sb, natal, "Natal", "#E0E0E0");
        }
        if (hasTransit) {
            section(sb, transit, "Transiting now", "#B9D4FF");
        }
        return sb.toString();
    }

    private static void section(StringBuilder sb, List<Hit> hits, String label, String colour) {
        sb.append("<p style='margin-top:6px; margin-bottom:2px; color:").append(colour)
          .append(";'><b>").append(label).append("</b></p><ul style='margin-top:2px;'>");
        for (Hit h : hits) {
            sb.append("<li style='margin-bottom:3px; color:").append(colour).append(";'>")
              .append("<span style='font-size:15px;'>").append(h.glyph).append("</span> <b>")
              .append(h.name).append("</b> at ").append(Zodiac.format(h.longitude));
            if (h.retrograde) {
                sb.append(" <span style='color:#E58A8A;'>R</span>");
            }
            sb.append("</li>");
        }
        sb.append("</ul>");
    }
}
