package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ArcMidpoints;

/**
 * What {@code SA Body = A/B} means, said in prose.
 *
 * <h3>Why this composes instead of looking one up</h3>
 *
 * <p>Thirteen dial points make 78 midpoints and 78 &times; 13 = <b>1,014 pictures</b>. A written
 * catalogue of them is what the tradition has - Ebertin's <i>Combination of Stellar
 * Influences</i> is exactly that book - and a catalogue written <i>here</i> would be 1,014
 * paragraphs of invention, because nobody has that doctrine to hand and a plausible paragraph
 * about SA Mercury = Jupiter/Neptune is indistinguishable from a sourced one once it is on the
 * screen. This project's own rule about that is in the work plan: prose nobody can source is
 * worse than an honest gap, because a reader believes it.
 *
 * <p>So a picture is read in the two halves it is actually made of:
 *
 * <ul>
 *   <li><b>The catalyst</b> - what the directed body does when it arrives. Thirteen sentences,
 *       one per dial point, in {@code arc_catalyst}.</li>
 *   <li><b>The matrix</b> - what the two natal bodies mean fused together. A midpoint IS their
 *       fusion, which is what a conjunction reading already describes, so this half asks
 *       {@link InterpretationService#getAspect} for the pair's conjunction. <b>Measured: all 78
 *       pairs of the thirteen dial points have one</b>, across {@code extra_bodies.json} (64) and
 *       {@code aspect_prose_v2.json} (14). Nothing is invented and nothing is missing.</li>
 * </ul>
 *
 * <p>And where a picture has a specific traditional reading, that is written out and takes
 * precedence - {@code arc_picture}, keyed {@code directed_a_b}. There are four to begin with, the
 * ones David named: SA Uranus = Sun/Moon, SA Pluto = Sun/Saturn, SA Jupiter = Sun/MC and SA
 * Uranus = Mars/Pluto. <b>The composed halves are still shown underneath an override</b>, because
 * the override is a claim about this particular picture and the halves are the reasoning a reader
 * can check it against.
 *
 * <h3>The natal anchor is not optional</h3>
 *
 * <p>Every reading ends with it, because L8's doctrine says a directed contact cannot produce
 * what the radix does not promise - "no promise made, no promise kept" - and a reading that
 * printed the picture's keywords without that sentence would be asserting an outcome the chart
 * may have no capacity for. It is the one line here that is about what the reading is <i>not</i>
 * entitled to say.
 */
public final class ArcMidpointReading {

    private ArcMidpointReading() { }

    /** The aspect whose prose describes two bodies fused, which is what a midpoint is. */
    private static final String FUSION = "Conjunction";

    /**
     * The reading for one picture, as HTML.
     *
     * @param c    the contact, which supplies the picture and its dates
     * @param nowJd the moment being read, for the strength line; NaN to omit it
     */
    public static String of(ArcMidpoints.Contact c, double nowJd, double arcNow) {
        if (c == null) {
            return "";
        }
        InterpretationService svc = InterpretationService.getInstance();
        StringBuilder h = new StringBuilder();

        h.append("<h3>").append(esc(c.picture())).append("</h3>");

        String strength = strengthLine(c, arcNow);
        if (!strength.isEmpty()) {
            h.append("<p>").append(strength).append("</p>");
        }

        String specific = svc.getArcPicture(c.directed, c.a, c.b);
        if (specific != null && !specific.isEmpty()) {
            h.append("<p>").append(specific).append("</p>");
        }

        String catalyst = svc.getArcCatalyst(c.directed);
        if (catalyst != null && !catalyst.isEmpty()) {
            h.append("<p>").append(catalyst).append("</p>");
        }

        // <b>The pair fused, which is the half this layer does not have to write.</b>
        String matrix = svc.getAspect(c.a, c.b, FUSION);
        if (matrix != null && !matrix.isEmpty()) {
            h.append("<p><b>").append(esc(c.a)).append("/").append(esc(c.b))
             .append("</b> &mdash; the natal pairing this arrives on:</p>")
             .append("<p>").append(matrix).append("</p>");
        }

        h.append(ANCHOR);
        return h.toString();
    }

    /**
     * How close the contact is now, in the units the reader has.
     *
     * <p>Degrees of arc and days, not a percentage. The weight is a number this layer uses to
     * rank; a reader wants to know whether the thing is arriving, exact, or leaving, and the
     * honest form of that is the offset and the date.
     */
    private static String strengthLine(ArcMidpoints.Contact c, double arcNow) {
        if (Double.isNaN(arcNow)) {
            return "";
        }
        double off = arcNow - c.arc;
        double w = ArcMidpoints.weight(off);
        String where = Math.abs(off) <= ArcMidpoints.PEAK
            ? "<b>at its peak</b>"
            : (off < 0 ? "applying" : "separating");
        if (w <= 0.0) {
            return "Outside its orb now &mdash; " + (off < 0 ? "not yet" : "past")
                + ", by " + String.format("%.2f", Math.abs(off)) + "&deg; of arc.";
        }
        return String.format(
            "%s, %.2f&deg; of arc from exact, at %.0f%% of full strength.",
            where, Math.abs(off), w * 100.0);
    }

    /**
     * The sentence every one of these readings ends with.
     *
     * <b>Stated once as a constant</b> rather than written into each entry in the catalogue,
     * which is how the four specific readings would have ended up carrying four slightly
     * different versions of the same caveat.
     */
    static final String ANCHOR =
        "<p><i><b>No promise made, no promise kept.</b> A directed contact sets off what the "
        + "birth chart already holds; it cannot produce a capacity the radix never promised. "
        + "Read this against the natal aspect between the same two bodies before believing it "
        + "&mdash; where that aspect is hard, this is an opportunity to master it rather than a "
        + "release from it.</i></p>";

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
