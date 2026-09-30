package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Zodiac;

/**
 * The words for one placement: the row in the placements list, the hover card, the decan line
 * under both, and the entry for an aspect pattern.
 *
 * <h3>Why this is its own class (J13, step 3)</h3>
 *
 * <p>These lived on {@link SkymapPanel}, which is the problem J13 exists for. The rings came
 * out first ({@code WheelRing}) and the zoom and pan second ({@code WheelView}); this is the
 * first of the text builders. It is also the safest one to move, because none of it needs
 * the panel for anything but a single number: which house cusps to measure a body against.
 *
 * <h3>The cusps arrive as an argument, and that is the whole point</h3>
 *
 * <p>Both builders used to read the panel's cusps themselves - the list its active cusps, the
 * hover card the outer ring's when the body was on it - which is why neither could be called
 * with no panel built. The caller chooses the cusps now, exactly as it chose them before, and
 * every method here is static: give it a body and a degree and it answers, with no window
 * open. Nothing here holds state, so nothing here can disagree with the wheel about it.
 *
 * <h3>What has not changed</h3>
 *
 * <p>Every string this builds is the string the panel built, character for character; the
 * move is a move. The one visible seam is {@link #hoverCard}'s ring word, which the panel now
 * looks up and passes in - and only for a ring that is not the natal one, which is when the
 * panel looked it up before, because {@code ringWord} is not free: it reassigns ring kinds.
 */
final class PlacementText {

    private PlacementText() {
    }

    static String hoverCard(int n, double lon, double speed, boolean transit, String ringWord,
            double[] cusps) {
        Bodies.Def def = Bodies.at(n);
        int signIdx = Zodiac.signIndex(lon);
        String sign = Zodiac.SIGNS[signIdx];
        int degInSign = (int)(lon % 30.0);
        int minInSign = (int)((lon % 30.0 - (double)degInSign) * 60.0);
        int decan = Zodiac.decan(lon);

        StringBuilder sb = new StringBuilder();
        // <b>Its own background and its own ink, because this card lands in two places.</b>
        // It is a tooltip over the wheel and it is also the top of the Selection drawer -
        // selectionHtml builds on it - and it set neither colour, so it inherited whatever it
        // landed on. Over a pale tooltip the default black read fine; in the dark drawer the
        // point's name was black on near-black, which is what David saw. A fragment reused on
        // two surfaces has to carry its own contrast rather than borrow one.
        sb.append("<html><body style='width:250px; font-family:SansSerif; font-size:11px;"
            + " background:#12151A; color:#E0E0E0;'>");
        sb.append("<div style='font-size:13px;'><b>").append(def.name).append("</b>");
        if (transit) {
            sb.append(" <span style='color:#5A7FBF;'>(")
                  .append(ringWord).append(")</span>");
        }
        if (!def.isAngle() && speed < 0.0) {
            sb.append(" <span style='color:#B03030;'><b>R</b></span>");
        }
        sb.append("</div>");

        sb.append("<div><b>").append(degInSign).append("&deg;")
          .append(minInSign < 10 ? "0" : "").append(minInSign).append("'</b> ")
          .append(capitalise(sign));
        int house = Zodiac.houseOf(lon, cusps);
        if (house > 0) {
            sb.append(" &nbsp;&middot;&nbsp; House ").append(house);
        }
        sb.append("</div>");

        // <b>Both decan schemes, each named.</b> This line used to read "sub-ruler Mercury"
        // without saying which of the two systems that was, which is the one thing a reader
        // cannot afford not to know here: they disagree for 30 of the 36 decans, and the app
        // uses both at once. The triplicity ruler is the one the decan prose is written to;
        // the Chaldean face is the one the Golden Dawn tarot cards and the Sabian decan_ruler
        // field encode. Naming them is what lets a practitioner reconcile the two surfaces
        // instead of reading the difference as a fault. Zodiac's header carries the full note.
        // <b>The degree's own condition, when it has one.</b> Open in the work plan since
        // 21 Aug and settled 2026-09-03: anaretic is exactly 29d00'00" to 29d59'59" with no
        // tolerance either side, and 0d is the opposite condition rather than the same one.
        // See DECISIONS.md, K3.
        Zodiac.DegreeStatus status = Zodiac.degreeStatus(lon);
        if (status == Zodiac.DegreeStatus.ANARETIC) {
            sb.append("<div style='color:#E8B24A;'><b>Anaretic</b> &middot; the 30th degree, ")
              .append("completing this sign</div>");
        } else if (status == Zodiac.DegreeStatus.INITIATION) {
            sb.append("<div style='color:#7FB3FF;'><b>First degree</b> &middot; the sign just ")
              .append("begun</div>");
        }
        String triplicity = Zodiac.triplicityDecanRuler(lon);
        String face = Zodiac.chaldeanDecanRuler(lon);
        int decanFrom = (decan - 1) * 10;
        sb.append("<div style='color:#9FB4C7;'>Decan ").append(decan)
          .append(" &middot; ").append(decanFrom).append("&deg;&ndash;").append(decanFrom + 10)
          .append("&deg;</div>");
        if ((triplicity != null && !triplicity.isEmpty()) || (face != null && !face.isEmpty())) {
            sb.append("<div style='color:#9FB4C7; font-size:10px;'>");
            if (triplicity != null && !triplicity.isEmpty()) {
                sb.append("Triplicity <b>").append(triplicity).append("</b>");
            }
            if (face != null && !face.isEmpty()) {
                if (triplicity != null && !triplicity.isEmpty()) {
                    sb.append(" &nbsp;&middot;&nbsp; ");
                }
                sb.append("Chaldean face <b>").append(face).append("</b>");
            }
            sb.append("</div>");
        }

        try {
            String sabian = InterpretationService.getInstance()
                .getSabianSymbol(capitalise(sign), degInSign + 1);
            if (sabian != null && !sabian.isEmpty() && !sabian.startsWith("Interpretation not found")) {
                sb.append("<div style='margin-top:4px; color:#C9BFA0; font-style:italic;'>")
                  .append(degInSign + 1).append("&deg; ").append(capitalise(sign))
                  .append(": &ldquo;").append(sabian).append("&rdquo;</div>");
            }
        } catch (Exception e) {
            // A tooltip is not worth throwing out of a mouse-moved handler for.
        }
        if (!def.meaning.isEmpty()) {
            sb.append("<div style='margin-top:4px; color:#8FA98F;'>").append(def.meaning).append("</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    static String capitalise(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    /**
     * One figure in the pattern banner, every word of it a link to the figure's reading.
     *
     * <b>Only the bold name was a link.</b> The quality after it and the line of planets under
     * it were plain text, so a reader clicking "Mercury, Pluto, Uranus" under a grand trine got
     * nothing. David, 2026-09-14: "Grand trine (air) Mercury, Pluto, Uranus ... not letting me
     * select them". Swing's HTML cannot wrap a block in one anchor, so each run carries the same
     * href in its own colour. NavigationCheck Part Q clicks every character of this markup.
     */
    static String patternEntryHtml(com.zodiacomputing.ourania.astro.AspectPatterns.Pattern p) {
        String href = InterpretationPanel.patternHref(p.name, p.bodies);
        StringBuilder sb = new StringBuilder("<div style='margin-bottom:3px;'>");
        sb.append("<a href='").append(href)
            .append("' style='color:#FFD166; text-decoration:none;'><b>").append(p.name).append("</b>");
        String quality = p.modality != null
            && (p.name.equals("T-square") || p.name.equals("Grand cross"))
                ? p.modality
                : p.element != null
                    && (p.name.equals("Grand trine") || p.name.equals("Kite"))
                        ? p.element : null;
        if (quality != null) {
            sb.append(" <span style='color:#dddddd;'>(").append(quality).append(")</span>");
        }
        sb.append("</a>");
        sb.append("<div style='font-size:11px;'><a href='").append(href)
            .append("' style='color:#dddddd; text-decoration:none;'>")
            .append(String.join(", ", p.bodies));
        if (p.apex != null) {
            sb.append(" &nbsp;|&nbsp; apex ").append(p.apex);
        }
        sb.append("</a></div></div>");
        return sb.toString();
    }
    static String placementRow(int n, double d, double d2, String string, double[] cusps) {
        int n2 = (int)(d / 30.0);
        int n3 = (int)(d % 30.0);
        int n4 = (int)((d - Math.floor(d)) * 60.0);
        int n5 = 1;
        for (int i = 1; i <= 12; ++i) {
            double d3;
            double d4;
            double d5 = cusps[i];
            double d6 = d4 = i == 12 ? cusps[1] : cusps[i + 1];
            if (d4 < d5) {
                d4 += 360.0;
            }
            if ((d3 = d) < d5 && d4 > 360.0) {
                d3 += 360.0;
            }
            if (!(d3 >= d5) || !(d3 < d4)) continue;
            n5 = i;
            break;
        }
        String string2 = showsDirection(n) ? (d2 < 0.0 ? " R" : " D") : "";
        String string3 = SkymapPanel.elementTextHex(Zodiac.elementIndex(n2));
        // <b>Both decan rulers, each named, under the placement.</b> The app runs two schemes
        // at once and they disagree for 30 of the 36 decans, so an unlabelled "sub-ruler" here
        // would be worse than none: the reader cannot tell whether it governs the prose or the
        // tarot. Triplicity rules the decan prose; the Chaldean face rules the Golden Dawn
        // cards and the Sabian decan_ruler field. Zodiac's header carries the full note.
        String decanLine = decanRulers(d);
        return String.format("<div style='margin-bottom:4px;'><a href='%s%d' style='color:#cccccc; text-decoration:none; font-family:SansSerif; font-size:14px;'><span style='font-size:16px;'>%s</span> %d&deg; %02d'%s <span style='color:%s; font-size:16px;'>%s</span> House %s</a>%s</div>", string, n, SkymapPanel.BODY_GLYPHS[n], n3, n4, string2, string3, SkymapPanel.ZODIAC_SYMBOLS[n2], romanNumeral(n5), decanLine);
    }

    /**
     * The decan and its two rulers, as one small line for a placement row.
     *
     * Static and shared so the placements list and the hover card cannot drift apart - they
     * are the two surfaces a reader compares, and a decan named differently on each would be
     * read as a bug in the chart rather than a difference between two traditions.
     */
    static String decanRulers(double lon) {
        int decan = Zodiac.decan(lon);
        String triplicity = Zodiac.triplicityDecanRuler(lon);
        String face = Zodiac.chaldeanDecanRuler(lon);
        if ((triplicity == null || triplicity.isEmpty()) && (face == null || face.isEmpty())) {
            return "";
        }
        StringBuilder sb = new StringBuilder(
            "<div style='color:#9FB4C7; font-size:10px; margin-left:20px;'>Decan ");
        sb.append(decan).append(" &middot; ");
        if (triplicity != null && !triplicity.isEmpty()) {
            sb.append("triplicity <b>").append(triplicity).append("</b>");
        }
        if (face != null && !face.isEmpty()) {
            if (triplicity != null && !triplicity.isEmpty()) {
                sb.append(" &middot; ");
            }
            sb.append("Chaldean <b>").append(face).append("</b>");
        }
        return sb.append("</div>").toString();
    }

    static boolean showsDirection(int n) {
        Bodies.Source source = Bodies.at((int)n).source;
        return source == Bodies.Source.EPHEMERIS || source == Bodies.Source.SOUTH_NODE;
    }

    static String romanNumeral(int n) {
        String[] stringArray = new String[]{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
        if (n >= 1 && n <= 12) {
            return stringArray[n];
        }
        return String.valueOf(n);
    }
}
