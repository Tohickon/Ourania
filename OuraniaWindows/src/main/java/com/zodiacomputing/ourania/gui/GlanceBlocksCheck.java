package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Zodiac;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The opening of a birth chart's At a glance page (reading framework spec, 2026-10-24): three
 * cards, day or night, and the bars.
 *
 * <b>Faithful is asked of the chart.</b> Each card's sign and house are looked up again here from
 * the frame, and each bar's percentage is recomputed from Gestalt's weights, so a card filed under
 * the wrong sign or a bar drawn for the wrong element fails by name. <b>Plain is asked of
 * JargonCheck's own list</b>, so there is one list of banned words and not two.
 */
public final class GlanceBlocksCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** Fixed moments, not the clock: NavigationCheck taught this suite's neighbours why. */
    private static final int[][] DATES = {
        {1984, 5, 17, 6}, {1961, 11, 2, 21}, {2003, 2, 9, 14}, {1976, 8, 30, 3},
    };

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        System.out.println("=== Part A: the three cards, filed under the right sign and house ===");
        for (int[] d : DATES) {
            ChartFrame f = cast(sw, d);
            threeCards(f, d);
        }

        System.out.println("=== Part B: the bars add to 100 and follow the weights ===");
        for (int[] d : DATES) {
            ChartFrame f = cast(sw, d);
            bars(Gestalt.compute(f), d);
        }
        percents();

        System.out.println("=== Part C: plain on the surface ===");
        for (int[] d : DATES) {
            ChartFrame f = cast(sw, d);
            plain(f, Gestalt.compute(f), d);
        }
        report();
    }

    // ------------------------------------------------------------------ Part A

    private static void threeCards(ChartFrame f, int[] d) {
        String at = " (" + d[0] + ")";
        List<ReadingCard> person = GlanceBlocks.big3(f, false);
        List<ReadingCard> couple = GlanceBlocks.big3(f, true);
        yes("three cards for a person" + at + ": " + person.size(), person.size() == 3);
        yes("and the same three for a couple" + at, couple.size() == person.size());
        String[] names = {"Sun", "Moon", "Ascendant"};
        for (int i = 0; i < Math.min(names.length, person.size()); i++) {
            ChartFrame.Body b = f.body(names[i]);
            String sign = cap(Zodiac.signName(b.lon));
            ReadingCard c = person.get(i);
            yes(names[i] + " card headline is its sign, " + sign + at, sign.equals(c.headline()));
            boolean angle = names[i].equals("Ascendant");
            String why = c.why().isEmpty() ? "" : c.why().get(0);
            yes(names[i] + " says where it is" + at + ": " + why,
                why.startsWith(names[i] + " in " + sign));
            if (angle) {
                yes("the Ascendant has no house" + at, !why.contains("house"));
            } else {
                int house = Zodiac.houseOf(b.lon, f.cusps);
                yes(names[i] + " is in house " + house + at, why.endsWith(house + "th house")
                    || why.endsWith(house + "st house") || why.endsWith(house + "nd house")
                    || why.endsWith(house + "rd house"));
            }
            yes(names[i] + " has a sentence to read" + at,
                c.body() != null && c.body().length() > 20);
            yes("a couple's " + names[i] + " says something else from a person's" + at,
                !c.label().equals(couple.get(i).label()));
            yes("but is the same reading of the same placement" + at,
                c.body().equals(couple.get(i).body()) && c.headline().equals(couple.get(i).headline()));
        }
    }

    // ------------------------------------------------------------------ Part B

    private static void bars(Gestalt.Result g, int[] d) {
        String at = " (" + d[0] + ")";
        String html = GlanceBlocks.balanceHtml(g);
        yes("the bars draw" + at, !html.isEmpty());
        checkMap("element", g.elements, html, at);
        checkMap("mode", g.modalities, html, at);
        for (String dom : g.dominantElements) {
            yes(cap(dom) + " is named as strongest" + at, html.contains(cap(dom)));
        }
        yes("the colours are the elements' own" + at,
            html.contains("#E8743B") || html.contains("#7A9E5B") || html.contains("#7FC8E8")
                || html.contains("#4B7BE5"));
    }

    private static void checkMap(String kind, Map<String, Double> weights, String html, String at) {
        double sum = 0;
        for (double w : weights.values()) {
            sum += w;
        }
        int shownSum = 0;
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            Matcher m = Pattern.compile(">" + cap(e.getKey()) + "</td>.*?align='right'>(\\d+)%</td>",
                Pattern.DOTALL).matcher(html);
            yes(kind + " " + e.getKey() + " has a row" + at, m.find());
            if (m.find(0)) {
                int shown = Integer.parseInt(m.group(1));
                shownSum += shown;
                double exact = sum <= 0 ? 0 : 100.0 * e.getValue() / sum;
                yes(kind + " " + e.getKey() + " is " + shown + "% for " + String.format("%.1f", exact)
                    + at, Math.abs(shown - exact) < 1.0 + 1e-9);
            }
        }
        yes(kind + " rows add to 100 (" + shownSum + ")" + at, sum <= 0 || shownSum == 100);
    }

    private static void percents() {
        yes("nothing to divide gives zeros", sum(GlanceBlocks.percents(new double[] {0, 0, 0})) == 0);
        yes("an empty set is empty", GlanceBlocks.percents(new double[0]).length == 0);
        yes("one weight is all of it", GlanceBlocks.percents(new double[] {3.7})[0] == 100);
        int[] third = GlanceBlocks.percents(new double[] {1, 1, 1});
        yes("thirds add to 100, not 99", sum(third) == 100);
        Random r = new Random(24);
        for (int round = 0; round < 500; round++) {
            double[] w = new double[1 + r.nextInt(6)];
            for (int i = 0; i < w.length; i++) {
                w[i] = r.nextInt(4) == 0 ? 0 : r.nextDouble() * 10;
            }
            int[] p = GlanceBlocks.percents(w);
            double total = 0;
            for (double x : w) {
                total += x;
            }
            if (total > 0) {
                yes("random set " + round + " adds to 100", sum(p) == 100);
                for (int i = 0; i < w.length; i++) {
                    for (int j = 0; j < w.length; j++) {
                        if (w[i] > w[j]) {
                            yes("a bigger weight never gets a smaller share", p[i] >= p[j]);
                        }
                    }
                    yes("a zero weight gets nothing", w[i] > 0 || p[i] == 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ Part C

    private static void plain(ChartFrame f, Gestalt.Result g, int[] d) {
        String at = " (" + d[0] + ")";
        for (ReadingCard c : GlanceBlocks.big3(f, false)) {
            yes("no banned word in the label or headline: " + c.label() + " / " + c.headline(),
                JargonCheck.banned(c.label() + " " + c.headline()).isEmpty());
        }
        for (ReadingCard c : GlanceBlocks.big3(f, true)) {
            yes("nor in a couple's: " + c.label(), JargonCheck.banned(c.label()).isEmpty());
        }
        String sect = text(GlanceBlocks.sectHtml(g));
        yes("the day-or-night line has no banned word" + at + ": " + JargonCheck.banned(sect),
            JargonCheck.banned(sect).isEmpty());
        String balance = text(GlanceBlocks.balanceHtml(g));
        yes("nor the bars and their captions" + at + ": " + JargonCheck.banned(balance),
            JargonCheck.banned(balance).isEmpty());

        String plain = GlanceBlocks.prefix(f, g, false, false);
        String astro = GlanceBlocks.prefix(f, g, false, true);
        yes("plain view has no why lines" + at, !plain.contains("Why &mdash;"));
        yes("Astrologer view has them" + at, astro.contains("Why &mdash;"));
        yes("plain view has no degree sign" + at, !plain.contains("°"));
        yes("a person's page says day or night" + at,
            plain.contains("Born in daylight") || plain.contains("Born at night"));
        String composite = GlanceBlocks.prefix(f, g, true, false);
        yes("a composite does not claim anyone was born" + at,
            !composite.contains("Born in daylight") && !composite.contains("Born at night"));
        yes("but still shows the bars" + at, composite.contains("The mix in your chart"));
    }

    // ------------------------------------------------------------------ Helpers

    private static String text(String html) {
        return html.replaceAll("<[^>]+>", " ").replace("&amp;", "&").replace("&mdash;", "-");
    }

    private static int sum(int[] a) {
        int s = 0;
        for (int x : a) {
            s += x;
        }
        return s;
    }

    private static String cap(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static ChartFrame cast(SwissEph sw, int[] d) {
        SweDate sd = new SweDate(d[0], d[1], d[2], d[3]);
        return ChartFrame.compute(sw, sd.getJulDay(), 40.71, -74.01, 'P', false, 0.0);
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
