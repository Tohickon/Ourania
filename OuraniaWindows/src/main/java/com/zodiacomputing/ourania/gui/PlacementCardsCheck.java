package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Zodiac;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The natal Placements cards (reading framework spec, Phase 2A): faithful to the chart, grouped
 * as the spec says, plain by default, and richer in Astrologer view.
 *
 * <b>Every fact is asked of the chart again.</b> Sign, degree, house and the dignities are read
 * from the frame and from the dignity tables directly - not from {@code Dignity.evaluate}, which
 * the page itself calls - so a card with the wrong sign, a Ruler badge on a body that does not
 * rule its sign, or a house on an angle fails by name.
 */
public final class PlacementCardsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    private static final int[][] DATES = {
        {1984, 5, 17, 6}, {1961, 11, 2, 21}, {2003, 2, 9, 14}, {1976, 8, 30, 3},
    };

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        System.out.println("=== Part A: every body once, its own sign, degree and group ===");
        for (int[] d : DATES) {
            cards(ChartFrame.compute(sw, new SweDate(d[0], d[1], d[2], d[3]).getJulDay(), 40.71,
                -74.01, 'P', false, 0.0), d);
        }

        System.out.println("=== Part B: the window follows Astrologer view ===");
        window();

        System.out.println("=== Part C: the sentence on a plain card is plain ===");
        sentences();
        report();
    }

    // ------------------------------------------------------------------ Part C

    /**
     * Every sentence a plain card can show - each body in each sign - against David's list. The
     * card's own words (the label, the part of life) are the interface's and are asked in Part A;
     * this asks the corpus's, which are written in the astrologer's register in places.
     */
    private static void sentences() {
        InterpretationService svc = InterpretationService.getInstance();
        int total = 0;
        int withheld = 0;
        int withBannedWord = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def def = Bodies.at(i);
            for (String sign : Zodiac.SIGNS) {
                String lower = sign.toLowerCase(Locale.ROOT);
                String whole = PlainSnapshot.signSentence(svc, def.name, lower);
                if (whole == null) {
                    yes(def.name + " in " + sign + ": no line, no sentence either way",
                        PlacementCards.sentenceFor(svc, def.name, lower, false) == null
                            && PlacementCards.sentenceFor(svc, def.name, lower, true) == null);
                    continue;
                }
                total++;
                boolean banned = !JargonCheck.banned(whole).isEmpty();
                String plain = PlacementCards.sentenceFor(svc, def.name, lower, false);
                String astro = PlacementCards.sentenceFor(svc, def.name, lower, true);
                yes(def.name + " in " + sign + ": Astrologer view shows the whole sentence",
                    whole.equals(astro));
                yes(def.name + " in " + sign + ": plain view shows it exactly when it is plain",
                    banned ? plain == null : whole.equals(plain));
                if (banned) {
                    withBannedWord++;
                    withheld += plain == null ? 1 : 0;
                }
            }
        }
        System.out.println("sentences: " + total + ", with a banned word: " + withBannedWord
            + ", withheld from plain view: " + withheld);
        yes("some sentences exist to be asked (" + total + ")", total > 100);
        yes("every sentence with a banned word is withheld from plain view",
            withheld == withBannedWord);
    }

    // ------------------------------------------------------------------ Part A

    private static void cards(ChartFrame f, int[] d) {
        String at = " (" + d[0] + ")";
        int n = f.bodies.length;
        double[] lon = new double[n];
        double[] speed = new double[n];
        boolean[] valid = new boolean[n];
        for (int i = 0; i < n; i++) {
            ChartFrame.Body b = f.bodies[i];
            valid[i] = b != null && b.ok;
            lon[i] = valid[i] ? b.lon : 0;
            speed[i] = valid[i] ? b.lonSpeed : 0;
        }
        String plain = PlacementCards.html(lon, speed, valid, f.cusps, "base_", false);
        String astro = PlacementCards.html(lon, speed, valid, f.cusps, "base_", true);
        yes("the page has cards" + at, plain.contains("<table") && astro.contains("<table"));

        int shown = 0;
        int lastHeading = -1;
        for (PlacementCards.Group g : PlacementCards.Group.values()) {
            int at2 = plain.indexOf(">" + g.label + "</h3>");
            if (at2 >= 0) {
                yes(g.label + " comes after the group before it" + at, at2 > lastHeading);
                lastHeading = at2;
            }
        }
        for (int i = 0; i < n; i++) {
            Bodies.Def def = Bodies.at(i);
            String marker = "name='base_" + i + "' href='base_" + i + "'";
            if (!valid[i]) {
                yes(def.name + " is not shown when the wheel does not show it" + at,
                    !plain.contains("name='base_" + i + "'"));
                continue;
            }
            shown++;
            yes(def.name + " appears exactly once and is a link" + at,
                count(plain, marker) == 1 && count(astro, marker) == 1);
            String card = segment(plain, "name='base_" + i + "'");
            String cardAstro = segment(astro, "name='base_" + i + "'");
            String sign = cap(Zodiac.signName(lon[i]));
            Matcher m = Pattern.compile("name='base_" + i + "'[^>]*>([^<]*)</a>").matcher(plain);
            yes(def.name + " is in " + sign + at, m.find() && m.group(1).trim().endsWith(sign));
            int deg = (int) Math.floor(Zodiac.degreeInSign(lon[i]));
            yes(def.name + " is at " + deg + "°" + at, card.contains(">" + deg + "°"));
            yes(def.name + " is under " + PlacementCards.groupOf(i).label + at,
                underOwnHeading(plain, i));

            // Plain: no house, no badge, no minutes, no symbol.
            yes(def.name + ": plain view has no house or dignity word" + at,
                !card.toLowerCase().contains("house") && !has(card, "Ruler", "Exaltation",
                    "Detriment", "Fall") && !card.contains("Sabian") && !card.matches("(?s).*\\d°\\d\\d'.*"));
            // Astrologer: minutes, and a house for everything but an angle.
            yes(def.name + ": Astrologer view has the minutes" + at,
                cardAstro.matches("(?s).*>" + deg + "°\\d\\d'.*"));
            if (def.isAngle()) {
                yes(def.name + " has no house" + at, !cardAstro.toLowerCase().contains(" house"));
            } else {
                int house = Zodiac.houseOf(lon[i], f.cusps);
                yes(def.name + " is in the " + Zodiac.ordinal(house) + " house" + at,
                    cardAstro.contains(Zodiac.ordinal(house) + " house"));
                yes(def.name + " names the part of life" + at,
                    card.contains("Shows up in: " + LifeAreas.of(house).replace("&", "&amp;")));
            }

            // The dignities, from the tables.
            int s = Zodiac.signIndex(lon[i]);
            boolean ruler = def.name.equals(Dignity.domicileRulerOf(s));
            boolean exalt = def.name.equals(Dignity.exaltedBodyOf(s));
            boolean detri = def.name.equals(Dignity.detrimentBodyOf(s));
            boolean fall = def.name.equals(Dignity.fallBodyOf(s));
            yes(def.name + ": Ruler badge exactly when it rules " + sign + at,
                cardAstro.contains(">Ruler<") == ruler);
            yes(def.name + ": Exaltation badge exactly when exalted" + at,
                cardAstro.contains(">Exaltation<") == exalt);
            yes(def.name + ": Detriment badge exactly when in detriment" + at,
                cardAstro.contains(">Detriment<") == detri);
            yes(def.name + ": Fall badge exactly when in fall" + at,
                cardAstro.contains(">Fall<") == fall);

            boolean retro = speed[i] < 0 && PlacementText.showsDirection(i);
            yes(def.name + ": retrograde shown exactly when it is" + at,
                card.contains("retrograde") == retro);
        }
        yes("no valid body was dropped (" + shown + ")" + at, shown > 10);
    }

    // ------------------------------------------------------------------ Part B

    private static void window() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        boolean was = Settings.astrologerView();
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            long end = System.nanoTime() + 20_000_000_000L;
            while (sky.radixChart() == null && System.nanoTime() < end) {
                Thread.sleep(50);
            }
            int casts = sky.castsApplied();
            SwingUtilities.invokeAndWait(() -> sky.applyChartSettings("1990-06-15", "12:00",
                "51.5074, -0.1278", ChartMode.SINGLE, "", "", "", false));
            long cast = System.nanoTime() + 20_000_000_000L;
            while ((sky.castsApplied() <= casts || !sky.innerIsBirthChart())
                    && System.nanoTime() < cast) {
                Thread.sleep(50);
            }
            yes("a birth chart is on the wheel", sky.innerIsBirthChart());
            JEditorPane pane = w.paneOf(OuraniaWindow.CHART_PAGE);
            yes("the Placements page has a pane", pane != null);
            if (pane == null) {
                return;
            }
            Settings.setAstrologerView(false);
            SwingUtilities.invokeAndWait(w::refreshPlainPages);
            String plain = text(pane);
            yes("a birth chart's Placements are cards in three groups",
                plain.contains("Personal drivers") && plain.contains("Social and evolutionary")
                    && plain.contains("Angles"));
            yes("plain view has no Essential dignity line", !plain.contains("Essential dignity"));

            Settings.setAstrologerView(true);
            SwingUtilities.invokeAndWait(w::refreshPlainPages);
            String astro = text(pane);
            yes("switching Astrologer view on rewrites the page", astro.contains("Sabian symbol"));
            yes("and shows a house", astro.contains(" house"));
        } finally {
            Settings.setAstrologerView(was);
            SwingUtilities.invokeAndWait(w::dispose);
        }
    }

    // ------------------------------------------------------------------ Helpers

    private static String text(JEditorPane pane) throws Exception {
        final String[] out = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                javax.swing.text.Document doc = pane.getDocument();
                out[0] = doc.getText(0, doc.getLength());
            } catch (javax.swing.text.BadLocationException e) {
                out[0] = "";
            }
        });
        return out[0];
    }

    /** From a marker to the end of its card. */
    private static String segment(String html, String marker) {
        int at = html.indexOf(marker);
        if (at < 0) {
            return "";
        }
        int end = html.indexOf("</table>", html.indexOf("</td></tr>", at) < 0 ? at : at);
        // A card holds a small table for its badges, so the card ends at the LAST table close
        // before the next card opens.
        int next = html.indexOf("<table width='100%'", at);
        return html.substring(at, next < 0 ? html.length() : next);
    }

    /** Whether a body's card sits after its own group's heading and before any later one. */
    private static boolean underOwnHeading(String html, int index) {
        PlacementCards.Group own = PlacementCards.groupOf(index);
        int card = html.indexOf("name='base_" + index + "'");
        int heading = html.indexOf(">" + own.label + "</h3>");
        if (card < 0 || heading < 0 || card < heading) {
            return false;
        }
        for (PlacementCards.Group g : PlacementCards.Group.values()) {
            if (g.ordinal() > own.ordinal()) {
                int later = html.indexOf(">" + g.label + "</h3>");
                if (later >= 0 && later < card) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean has(String s, String... words) {
        for (String w : words) {
            if (s.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static int count(String s, String sub) {
        int n = 0;
        for (int at = s.indexOf(sub); at >= 0; at = s.indexOf(sub, at + sub.length())) {
            n++;
        }
        return n;
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (int i = 0; i < Math.min(40, failures.size()); i++) {
            System.out.println("  " + failures.get(i));
        }
        System.exit(1);
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }
}
