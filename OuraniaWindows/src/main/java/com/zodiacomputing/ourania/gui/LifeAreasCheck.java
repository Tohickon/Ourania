package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * The Life areas page (2026-10-13): reachable, faithful to the chart, and plain.
 *
 * <b>Faithful is asked of the engine, not of the page's own loop.</b> Every body the wheel shows
 * is looked up in the chart and must appear in exactly one card, the card for the house
 * Zodiac.houseOf gives it - so a page that dropped a body, doubled one, or filed it under the
 * wrong part of life fails here even though every card on it would read well.
 */
public final class LifeAreasCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w, "skymapPanel");
            long end = System.nanoTime() + 20_000_000_000L;
            while (sky.radixChart() == null && System.nanoTime() < end) {
                Thread.sleep(50);
            }
            LifeAreasPanel panel = (LifeAreasPanel) CheckReflect.get(w, "lifeAreasPanel");
            yes("the window has a Life areas page", panel != null);
            yes("it is one of the pages under the chart tabs: " + w.pagesRail().pages(),
                w.pagesRail().pages().contains(OuraniaWindow.LIFE_AREAS_PAGE));
            // <b>A birth chart, cast on purpose (2026-10-22).</b> A fresh window holds the sky
            // alone, whose Life areas are the place's collective life (ReadingVoice.SKY); this
            // suite pins a person's, so it casts one, and asks the page once so it takes the
            // person's voice before its cards are read.
            int casts = sky.castsApplied();
            SwingUtilities.invokeAndWait(() -> sky.applyChartSettings("1990-06-15", "12:00",
                "51.5074, -0.1278", ChartMode.SINGLE, "", "", "", false));
            long cast = System.nanoTime() + 20_000_000_000L;
            while ((sky.castsApplied() <= casts || !sky.innerIsBirthChart())
                    && System.nanoTime() < cast) {
                Thread.sleep(50);
            }
            yes("a birth chart is on the wheel", sky.innerIsBirthChart());
            if (panel != null) {
                SwingUtilities.invokeAndWait(panel::html);
            }
            ChartFrame f = sky.getCurrentChart();
            yes("a chart was cast to read", f != null);
            if (panel == null || f == null) {
                report();
                return;
            }

            System.out.println("=== Part A: every shown body, once, in its own house ===");
            List<ReadingCard> cards = panel.cards(f);
            int expected = 0;
            for (int i = 0; i < f.bodies.length; i++) {
                ChartFrame.Body b = f.bodies[i];
                Bodies.Def def = Bodies.at(i);
                if (b == null || !b.ok || def.isAngle() || !sky.natalRing.valid[i]) {
                    continue;
                }
                expected++;
                int house = Zodiac.houseOf(b.lon, f.cusps);
                String want = def.name + " in ";
                String suffix = ", " + house + (house % 10 == 1 && house != 11 ? "st"
                    : house % 10 == 2 && house != 12 ? "nd" : house % 10 == 3 && house != 13
                    ? "rd" : "th") + " house";
                int found = 0;
                boolean right = false;
                for (ReadingCard c : cards) {
                    for (String why : c.why()) {
                        if (why.startsWith(want)) {
                            found++;
                            right = why.endsWith(suffix) && c.headline().equalsIgnoreCase(
                                LifeAreas.of(house));
                        }
                    }
                }
                yes(def.name + " appears exactly once (" + found + ")", found == 1);
                yes(def.name + " is filed under house " + house + ", "
                    + LifeAreas.of(house), right);
            }
            int listed = 0;
            for (ReadingCard c : cards) {
                listed += c.why().size();
            }
            yes("no card lists a body the chart does not show (" + listed + " of " + expected
                + ")", listed == expected);

            System.out.println();
            System.out.println("=== Part B: plain on the surface ===");
            for (ReadingCard c : cards) {
                yes("headline is a part of life, not a number: " + c.headline(),
                    !c.headline().matches(".*\\d.*") && !c.headline().contains("house"));
                yes("body has something to say for " + c.headline(),
                    c.body() != null && c.body().length() > 10);
                yes("body carries no degree sign for " + c.headline(), !c.body().contains("°"));
            }

            System.out.println();
            System.out.println("=== Part C: the why box is Astrologer view's alone ===");
            boolean was = Settings.astrologerView();
            try {
                Settings.setAstrologerView(false);
                yes("plain view hides the astrology", !panel.html().contains("Why &mdash;"));
                Settings.setAstrologerView(true);
                yes("Astrologer view shows it", cards.isEmpty()
                    || panel.html().contains("Why &mdash;"));
            } finally {
                Settings.setAstrologerView(was);
            }
            yes("a new install starts plain", !was);

            System.out.println();
            System.out.println("=== Part D: seven parts of life ===");
            // The reading framework spec's seven (2026-10-24), asked by name and in order.
            String[] spec = {"Love & Relationships", "Career & Public Life", "Home & Family",
                "Money & Resources", "Mind & Communication", "Health & Body",
                "Growth & Spirituality"};
            LifeAreaMapper.Domain[] domains = LifeAreaMapper.Domain.values();
            yes("seven domains", domains.length == 7);
            java.util.Set<Integer> placed = new java.util.HashSet<>();
            for (int i = 0; i < Math.min(spec.length, domains.length); i++) {
                yes("domain " + (i + 1) + " is '" + spec[i] + "'", spec[i].equals(domains[i].label));
                yes(spec[i] + " has a house", domains[i].houses().length >= 1);
                for (int h : domains[i].houses()) {
                    yes("house " + h + " is in one domain only", placed.add(h));
                    yes("the mapper agrees house " + h + " is " + spec[i],
                        LifeAreaMapper.of(h) == domains[i]);
                }
            }
            yes("all twelve houses are placed", placed.size() == 12);
            yes("outside 1 to 12 there is none",
                LifeAreaMapper.of(0) == null && LifeAreaMapper.of(13) == null);
            yes("a person reads under seven headings, the sky alone under its four",
                LifeAreasPanel.groupsFor(ReadingVoice.PERSON).size() == 7
                    && LifeAreasPanel.groupsFor(ReadingVoice.SKY).size() == 4);

            // The page: a domain heads it exactly when a card sits in one of its houses, and
            // the headings keep the spec's order.
            String page = panel.html();
            int last = -1;
            for (LifeAreaMapper.Domain d : domains) {
                boolean occupied = false;
                for (int h : d.houses()) {
                    for (ReadingCard c : cards) {
                        occupied |= c.headline().equalsIgnoreCase(LifeAreas.of(h));
                    }
                }
                int at = page.indexOf(">" + d.label.replace("&", "&amp;") + "</h3>");
                yes(d.label + " heads the page exactly when something sits in it",
                    (at >= 0) == occupied);
                if (at >= 0) {
                    yes(d.label + " comes after the heading before it", at > last);
                    last = at;
                }
            }
            yes("the four old headings are gone from a person's page",
                !page.contains("Career and work") && !page.contains("Body, mind and soul"));
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }
        report();
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
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
