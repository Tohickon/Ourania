package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.AspectPatterns;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.TensionRelease;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.InterpretationService;
import com.zodiacomputing.ourania.gui.NarrativeSynthesizer;

import java.util.ArrayList;
import java.util.List;

/**
 * A planet's full reading on the phone (M5): its sign, its house, each aspect it makes, and
 * the finer detail - its decan, the Sabian symbol and the degree - as simple HTML the phone
 * renders with {@code Html.fromHtml}.
 *
 * <p><b>The desktop's words, the phone's order.</b> Every paragraph is the corpus entry the
 * desktop's reading shows for the same placement ({@code InterpretationPanel.natalPieces}),
 * fetched through the same {@link InterpretationService} calls. Only the order differs: on a
 * phone the three things a reader reads first - sign, house, aspects - come before the
 * reference detail rather than after the tarot, the way the Interpretation tab ranks them.
 *
 * <p>Also the chart's aspect patterns and its synthesized whole-chart reading - the rest of
 * M5 - through the same engine calls the desktop's pattern page and Synthesize button make.
 *
 * <p>No Android in here, like {@link PhoneChart}: {@code PhoneReadingTest} runs it on the JVM.
 */
final class PhoneReading {

    private PhoneReading() { }

    /** The full reading for planet {@code i}, or "" for one not cast. */
    static String planet(PhoneChart.Cast c, int i, InterpretationService svc) {
        ChartFrame f = c.frame;
        if (i < 0 || i >= PhoneChart.PLANETS || i >= f.bodies.length
                || f.bodies[i] == null || !f.bodies[i].ok) {
            return "";
        }
        ChartFrame.Body b = f.bodies[i];
        String sign = capital(Zodiac.signName(b.lon));
        int degree = (int) (b.lon % 30.0) + 1;          // Sabian degrees run 1-30, as the desktop's
        int decan = Zodiac.decan(b.lon);
        int house = c.timeUnknown ? 0 : Zodiac.houseOf(b.lon, f.cusps);

        StringBuilder h = new StringBuilder();
        h.append("<h2>").append(b.name).append(" in ").append(sign);
        if (house > 0) {
            h.append(", house ").append(house);
        }
        h.append("</h2>");
        h.append("<p><i>").append(Zodiac.format(b.lon));
        if (b.retrograde) {
            h.append(", retrograde");
        }
        h.append("</i></p>");

        section(h, b.name + " in " + sign, svc.getPlanetInSign(b.name, sign));
        if (house > 0) {
            section(h, "In house " + house, svc.getPlanetInHouse(b.name, house));
        } else if (c.timeUnknown) {
            h.append("<p><i>The house is not shown: it depends on the birth time.</i></p>");
        }

        StringBuilder aspects = new StringBuilder();
        for (PhoneWheel.Line l : PhoneWheel.aspectsOf(f, PhoneChart.PLANETS)) {
            if (l.a != i && l.b != i) {
                continue;
            }
            ChartFrame.Body other = f.bodies[l.a == i ? l.b : l.a];
            String label = l.type.label;
            aspects.append("<h4>").append(state(b, other, l.type)).append(label).append(' ')
                .append(other.name).append(String.format(" <small>(orb %.1f&deg;)</small>",
                    orb(b, other, l.type))).append("</h4>");
            aspects.append("<p>").append(svc.getAspect(b.name, other.name, label)).append("</p>");
        }
        if (aspects.length() > 0) {
            h.append("<h3>Aspects</h3>").append(aspects);
        }

        List<AspectPatterns.Pattern> mine = new ArrayList<>();
        for (AspectPatterns.Pattern p : patternsOf(f)) {
            if (p.bodies.contains(b.name)) {
                mine.add(p);
            }
        }
        if (!mine.isEmpty()) {
            h.append("<h3>In a pattern</h3>");
            for (AspectPatterns.Pattern p : mine) {
                pattern(h, p, f, null);
            }
        }

        section(h, "Decan " + decan + " of " + sign, svc.getDecan(sign, decan));
        String decanBody = svc.getBodyDecan(b.name, sign, decan, false);
        if (usable(decanBody)) {
            h.append("<p>").append(decanBody).append("</p>");
        }

        String symbol = svc.getSabianSymbol(sign, degree);
        if (usable(symbol)) {
            h.append("<h3>Sabian symbol, ").append(sign).append(' ').append(degree)
                .append("&deg;</h3>");
            h.append("<p><i>“").append(symbol).append("”</i></p>");
            String full = svc.getSabianFullText(sign, degree);
            if (usable(full)) {
                h.append("<p>").append(full).append("</p>");
            }
            String keywords = svc.getSabianKeywords(sign, degree);
            if (usable(keywords)) {
                h.append("<p><i>Keywords: ").append(keywords).append("</i></p>");
            }
        }
        String bodySabian = svc.getBodySabian(b.name, sign, degree, false);
        if (usable(bodySabian)) {
            h.append("<p>").append(bodySabian).append("</p>");
        }
        section(h, "The degree, " + sign + ' ' + degree + "&deg;",
            svc.getDegreeSummary(sign, degree));
        return h.toString();
    }

    /** The chart's aspect patterns, found as the desktop finds them for its pattern page. */
    static List<AspectPatterns.Pattern> patternsOf(ChartFrame f) {
        return Gestalt.compute(f).aspectPatterns;
    }

    /**
     * Every aspect pattern in the chart, each with its members, how tight it is and its
     * reading - the desktop's pattern page ({@code InterpretationPanel.patternDetail}) for
     * each. "" when the chart has none.
     */
    static String patterns(PhoneChart.Cast c) {
        return patterns(c.frame);
    }

    /** The same for any chart - a composite's patterns are the relationship's (M10). */
    static String patterns(ChartFrame f) {
        Gestalt.Result g = Gestalt.compute(f);
        StringBuilder h = new StringBuilder();
        for (AspectPatterns.Pattern p : g.aspectPatterns) {
            pattern(h, p, f, g);
        }
        return h.toString();
    }

    /** One pattern; its discharge too when {@code g} is given (the planet view leaves it out). */
    private static void pattern(StringBuilder h, AspectPatterns.Pattern p, ChartFrame f,
            Gestalt.Result g) {
        InterpretationService svc = InterpretationService.getInstance();
        h.append("<h4>").append(p.name);
        if (p.modality != null && (p.name.equals("T-square") || p.name.equals("Grand cross"))) {
            h.append(" &middot; ").append(capital(p.modality));
        } else if (p.element != null
                && (p.name.equals("Grand trine") || p.name.equals("Kite"))) {
            h.append(" &middot; ").append(capital(p.element));
        }
        h.append("</h4><p>");
        for (String name : p.bodies) {
            ChartFrame.Body b = f.body(name);
            h.append("<b>").append(name).append("</b>");
            if (name.equals(p.apex)) {
                h.append(" (apex)");
            }
            if (b != null && b.ok) {
                h.append(" &nbsp;").append(Zodiac.format(b.lon));
                if (!f.timeUnknown) {
                    h.append(", house ").append(Zodiac.houseOf(b.lon, f.cusps));
                }
            }
            h.append("<br>");
        }
        h.append(String.format("<small>Widest leg %.1f&deg; from exact.</small></p>",
            p.widestOrb));
        macro(h, svc, p.detailKey());
        macro(h, svc, p.variantKey());
        if (p.isDissociate()) {
            macro(h, svc, "pattern_dissociate");
        }
        if (g != null) {
            for (TensionRelease.Release rel : g.releases) {
                if (rel.source.equals(p.name) && rel.bodies.equals(p.bodies)
                        && usable(rel.sentence)) {
                    h.append("<p><b>Where it discharges:</b> ").append(rel.sentence).append("</p>");
                }
            }
        }
    }

    private static void macro(StringBuilder h, InterpretationService svc, String key) {
        if (key == null) {
            return;
        }
        String text = svc.getMacroDynamic(key);
        if (usable(text)) {
            h.append("<p>").append(text).append("</p>");
        }
    }

    /**
     * The synthesized whole-chart reading: the desktop's Synthesize, for the natal chart alone.
     * Its timing sections (profection, transits, the year ahead) wait for M7, so they are
     * passed as absent, which the synthesizer already handles for a chart with transits off.
     *
     * <p>The desktop's inline colours are stripped: they are written for its black pane, and
     * white headings on the phone's page would vanish.
     */
    static String synthesis(PhoneChart.Cast c) {
        ChartFrame f = c.frame;
        Gestalt.Result g = Gestalt.compute(f);
        List<BodyScore.Vector> ranked = BodyScore.rank(f, g);
        Themes.Result themes = Themes.extract(f, g, ranked, Topics.analyse(f, ranked));
        String html = NarrativeSynthesizer.generateReport(f, g, ranked, themes, null, null, null,
            null, null, false, false);
        return forPhone(html);
    }

    /** A desktop reading's HTML without its page wrapper and its colours for a black pane. */
    static String forPhone(String html) {
        return html.replaceAll("(?i)</?(html|body)[^>]*>", "")
            .replaceAll("(?i)\\s(style|color|bgcolor)\\s*=\\s*('[^']*'|\"[^\"]*\")", "");
    }

    private static void section(StringBuilder h, String heading, String prose) {
        if (usable(prose)) {
            h.append("<h3>").append(heading).append("</h3><p>").append(prose).append("</p>");
        }
    }

    /**
     * Whether an entry has something to say. The service answers a missing entry with a
     * placeholder ("Interpretation not found...", "(Add to JSON)") meant for the corpus's
     * authors; a reader on a phone is shown nothing instead.
     */
    static boolean usable(String s) {
        return s != null && !s.trim().isEmpty() && !s.contains("not found")
            && !s.contains("Add to JSON");
    }

    /** How far the aspect is from exact, in degrees. */
    static double orb(ChartFrame.Body a, ChartFrame.Body b, Aspects.Type t) {
        return Math.abs(separation(a.lon, b.lon) - t.exactAngle);
    }

    /**
     * "Applying " when the two are moving toward exact, "Separating " when away - from where
     * each will be an hour on, at its present speed.
     */
    static String state(ChartFrame.Body a, ChartFrame.Body b, Aspects.Type t) {
        double hour = 1.0 / 24.0;
        double later = Math.abs(separation(a.lon + a.lonSpeed * hour, b.lon + b.lonSpeed * hour)
            - t.exactAngle);
        double now = orb(a, b, t);
        if (Math.abs(later - now) < 1e-9) {
            return "";
        }
        return later < now ? "Applying " : "Separating ";
    }

    private static double separation(double x, double y) {
        double d = Math.abs(x - y) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
