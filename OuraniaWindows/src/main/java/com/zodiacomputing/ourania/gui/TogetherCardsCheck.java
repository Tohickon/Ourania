package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Synastry;
import com.zodiacomputing.ourania.astro.Zodiac;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Together door (Plain-Tabs Phase 4, 2026-10-16): faithful to the pair, plain on the
 * surface, and reachable.
 *
 * <b>Faithful is asked of {@link Synastry}, not of the page's own loops.</b> Every overlay of a
 * listed body must appear in exactly one room card, under the right house and the right
 * direction; every inter-aspect must be the aspect {@link Aspects#typeOf} names at the synastry
 * profile. A page that dropped a body, filed it under the wrong person, or invented a contact
 * fails here even though every card on it would read well.
 *
 * <b>The pairs are fixed dates, not the clock.</b> NavigationCheck spent a month red some
 * evenings and green others because its chart was cast for the current moment; these are not.
 */
public final class TogetherCardsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    /** Ten fixed pairs, spread across a century so slow and fast bodies both vary. */
    private static final int[][] PAIRS = {
        {1950, 3, 14, 1952, 11, 2}, {1961, 7, 4, 1964, 1, 20}, {1972, 9, 9, 1975, 5, 28},
        {1980, 12, 25, 1983, 6, 6}, {1988, 2, 29, 1990, 10, 31}, {1993, 4, 17, 1995, 8, 8},
        {1999, 8, 11, 2001, 3, 3}, {2004, 6, 21, 2006, 12, 12}, {1969, 7, 20, 1979, 7, 20},
        {1985, 1, 1, 1985, 7, 1},
    };

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        List<TogetherCards.Pair> pairs = new ArrayList<>();
        for (int[] p : PAIRS) {
            pairs.add(new TogetherCards.Pair(cast(sw, p[0], p[1], p[2], 6.5),
                cast(sw, p[3], p[4], p[5], 18.25), null, null));
        }

        System.out.println("=== Part A: the engine's inter-aspects ===");
        for (TogetherCards.Pair p : pairs) {
            engine(p);
        }

        System.out.println("=== Part B: every overlay in one room, once ===");
        for (TogetherCards.Pair p : pairs) {
            rooms(p);
        }
        yes("each house is in exactly one room", everyHouseOnce());

        System.out.println("=== Part C: the strongest pulls ===");
        for (TogetherCards.Pair p : pairs) {
            pulls(p);
        }

        System.out.println("=== Part D: chemistry is the readings, tightest first ===");
        int anyChemistry = 0;
        for (TogetherCards.Pair p : pairs) {
            anyChemistry += chemistry(p);
        }
        yes("the corpus produced chemistry cards at all (" + anyChemistry + ")", anyChemistry > 0);
        for (TogetherCards.Pair p : pairs) {
            switches(p);
        }

        System.out.println("=== Part E: plain on the surface, astrology behind the box ===");
        for (TogetherCards.Pair p : pairs) {
            surface(p);
        }
        smallPieces();

        System.out.println("=== Part F: the door ===");
        door();
        report();
    }

    // ------------------------------------------------------------------ Part A

    private static void engine(TogetherCards.Pair p) {
        List<Synastry.InterAspect> list = Synastry.interAspects(p.them(), p.you());
        double last = -1;
        boolean sorted = true;
        boolean faithful = true;
        boolean noAngles = true;
        for (Synastry.InterAspect a : list) {
            double sep = Aspects.separation(a.visitorBodyLon, a.hostBodyLon);
            faithful &= Aspects.typeOf(sep, a.visitorBody, a.hostBody, Aspects.Profile.SYNASTRY) == a.type
                && Math.abs(Math.abs(sep - a.type.exactAngle) - a.orb) < 1e-9;
            noAngles &= !Bodies.at(a.visitorBodyIndex).isAngle() && !Bodies.at(a.hostBodyIndex).isAngle()
                && Bodies.oppositeOf(a.visitorBodyIndex) < 0 && Bodies.oppositeOf(a.hostBodyIndex) < 0;
            sorted &= a.orb >= last;
            last = a.orb;
        }
        // Independently counted: every pair of aspectable bodies the halved orb accepts.
        int expected = 0;
        for (int v = 0; v < p.them().bodies.length; v++) {
            for (int h = 0; h < p.you().bodies.length; h++) {
                ChartFrame.Body a = p.them().bodies[v];
                ChartFrame.Body b = p.you().bodies[h];
                if (a == null || !a.ok || b == null || !b.ok || Bodies.at(v).isAngle()
                        || Bodies.at(h).isAngle() || Bodies.oppositeOf(v) >= 0
                        || Bodies.oppositeOf(h) >= 0) {
                    continue;
                }
                if (Aspects.typeOf(Aspects.separation(a.lon, b.lon), a.name, b.name,
                        Aspects.Profile.SYNASTRY) != null) {
                    expected++;
                }
            }
        }
        yes("every inter-aspect is the aspect typeOf names, at its own orb", faithful);
        yes("no angle or definitional point is listed", noAngles);
        yes("tightest first", sorted);
        yes("none missing and none extra (" + list.size() + " of " + expected + ")",
            list.size() == expected);
        yes("the count is the same read from either side",
            list.size() == Synastry.interAspects(p.you(), p.them()).size());
    }

    // ------------------------------------------------------------------ Part B

    private static void rooms(TogetherCards.Pair p) {
        List<ReadingCard> all = new ArrayList<>();
        for (TogetherCards.Room room : TogetherCards.Room.values()) {
            for (ReadingCard c : TogetherCards.roomCards(p, room)) {
                yes("a room card is labelled with its room: " + c.label(),
                    c.label().equals(room.label.toUpperCase(Locale.ROOT)));
                all.add(c);
            }
        }
        for (TogetherCards.Direction d : TogetherCards.Direction.values()) {
            for (Synastry.Overlay o : Synastry.houseOverlays(d.visitor(p), d.host(p))) {
                if (Bodies.at(o.bodyIndex).isAngle() || o.house < 1) {
                    continue;
                }
                String want = d.visitorWord + " " + o.body + " in " + d.hostWord + " "
                    + Zodiac.ordinal(o.house) + " house";
                TogetherCards.Room room = TogetherCards.Room.of(o.house);
                // Named here, not asked of d.area(): a check that asks the page which words
                // to expect cannot catch the page using the wrong person's words. Measured:
                // with area() answering "your" both ways, this suite stayed green until this
                // line stopped asking it.
                String area = d == TogetherCards.Direction.THEIRS_IN_YOURS
                    ? LifeAreas.of(o.house) : LifeAreas.ofTheirs(o.house);
                int found = 0;
                boolean right = false;
                for (ReadingCard c : all) {
                    for (String why : c.why()) {
                        if (why.equals(want)) {
                            found++;
                            right = c.label().equals(room.label.toUpperCase(Locale.ROOT))
                                && c.headline().equals(d.headline)
                                && c.body().contains(PlacementText.capitalise(area) + ":");
                        }
                    }
                }
                yes(want + " appears once (" + found + ")", found == 1);
                yes(want + " is in the " + room.label + " card, under its part of life", right);
            }
        }
    }

    private static boolean everyHouseOnce() {
        int[] seen = new int[13];
        for (TogetherCards.Room r : TogetherCards.Room.values()) {
            for (int h : r.houses()) {
                seen[h]++;
            }
        }
        for (int h = 1; h <= 12; h++) {
            if (seen[h] != 1 || TogetherCards.Room.of(h) == null) {
                return false;
            }
        }
        return TogetherCards.Room.of(0) == null && TogetherCards.Room.of(13) == null;
    }

    // ------------------------------------------------------------------ Part C

    private static void pulls(TogetherCards.Pair p) {
        int total = 0;
        for (TogetherCards.Direction d : TogetherCards.Direction.values()) {
            for (Synastry.AngleContact c : Synastry.angleContacts(d.visitor(p), d.host(p))) {
                if (!Bodies.at(c.bodyIndex).isAngle()) {
                    total++;
                }
            }
        }
        List<ReadingCard> cards = TogetherCards.pullCards(p);
        yes("pulls: " + cards.size() + " of " + total + ", capped at "
            + TogetherCards.PULL_LIMIT, cards.size() == Math.min(total, TogetherCards.PULL_LIMIT));
        double last = -1;
        for (ReadingCard c : cards) {
            double orb = orbOf(c);
            yes("a pull names whose body it is: " + c.headline(),
                c.headline().startsWith("Their ") || c.headline().startsWith("Your "));
            yes("pulls are tightest first", orb >= last);
            last = orb;
        }
    }

    // ------------------------------------------------------------------ Part D

    private static int chemistry(TogetherCards.Pair p) {
        InterpretationService svc = InterpretationService.getInstance();
        List<ReadingCard> cards = TogetherCards.chemistryCards(p, TogetherCards.CHEMISTRY_LIMIT);
        yes("at most " + TogetherCards.CHEMISTRY_LIMIT + " chemistry cards",
            cards.size() <= TogetherCards.CHEMISTRY_LIMIT);
        // The cards must be exactly the first N aspects that have a reading, in order.
        List<Synastry.InterAspect> withReading = new ArrayList<>();
        for (Synastry.InterAspect a : Synastry.interAspects(p.them(), p.you())) {
            if (a.type.isPtolemaic()
                    && svc.getSynastryInteraspect(a.visitorBody, a.hostBody, a.type.label) != null) {
                withReading.add(a);
            }
        }
        yes("the page shows as many as it can (" + cards.size() + " of " + withReading.size()
            + " with a reading)",
            cards.size() == Math.min(TogetherCards.CHEMISTRY_LIMIT, withReading.size()));
        for (int i = 0; i < cards.size() && i < withReading.size(); i++) {
            Synastry.InterAspect a = withReading.get(i);
            ReadingCard c = cards.get(i);
            String prose = svc.getSynastryInteraspect(a.visitorBody, a.hostBody, a.type.label);
            yes("card " + i + " is " + a.visitorBody + " " + a.type.label + " " + a.hostBody,
                c.label().equals(TogetherCards.tone(a.type) + " · THEIR "
                    + a.visitorBody.toUpperCase(Locale.ROOT) + ", YOUR "
                    + a.hostBody.toUpperCase(Locale.ROOT)));
            String plain = prose.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
            yes("its headline is quoted from the reading, word for word: " + c.headline(),
                plain.contains(c.headline()));
            yes("its headline is not the template lead",
                !c.headline().equals(SkymapPanel.boldLead(prose)));
            yes("the catalogue tag is not shown to a reader", !c.body().contains("Dynamic:"));
            yes("the body carries no markup", !c.body().contains("<"));
            // The minor-aspect template opens "their Sun in Sesquiquadrate (135°) to your
            // Chiron"; a headline like that is why the page shows the Ptolemaic five only.
            yes("the headline carries no degree: " + c.headline(),
                !c.headline().contains("°") && !c.headline().matches(".*\\(\\d+.*"));
        }
        // The reason the lead was dropped: a page whose cards open on one sentence says
        // nothing about the pair. Asserted, so a later change back to the lead fails here.
        java.util.Set<String> headlines = new java.util.HashSet<>();
        for (ReadingCard c : cards) {
            headlines.add(c.headline());
        }
        yes("no two chemistry cards on one page share a headline",
            headlines.size() == cards.size());
        return cards.size();
    }

    /**
     * The reader's aspect switches reach the page, as they reach the grid (review OO-001).
     * Before this, a quincunx switched off was gone from the grid and still a card here.
     */
    private static void switches(TogetherCards.Pair p) {
        boolean[] none = new boolean[Aspects.Type.values().length];
        TogetherCards.Pair off = new TogetherCards.Pair(p.you(), p.them(), null, none);
        yes("every aspect switched off leaves no chemistry card",
            TogetherCards.chemistryCards(off, TogetherCards.CHEMISTRY_LIMIT).isEmpty());
        boolean[] conjunctionOnly = new boolean[Aspects.Type.values().length];
        conjunctionOnly[Aspects.Type.CONJUNCTION.ordinal()] = true;
        TogetherCards.Pair conj = new TogetherCards.Pair(p.you(), p.them(), null, conjunctionOnly);
        for (ReadingCard c : TogetherCards.chemistryCards(conj, TogetherCards.CHEMISTRY_LIMIT)) {
            yes("with only conjunctions on, every card is a conjunction: " + c.label(),
                c.label().startsWith("BLENDED"));
        }
        none[0] = false;
        yes("the Pair keeps its own copy of the switches", off.aspectShown() != none);
    }

    // ------------------------------------------------------------------ Part E

    private static void surface(TogetherCards.Pair p) {
        List<ReadingCard> cards = new ArrayList<>(TogetherCards.pullCards(p));
        for (TogetherCards.Room r : TogetherCards.Room.values()) {
            cards.addAll(TogetherCards.roomCards(p, r));
        }
        for (ReadingCard c : cards) {
            yes("headline has no number or house: " + c.headline(),
                !c.headline().matches(".*\\d.*") && !c.headline().toLowerCase().contains("house"));
            yes("body has no degree sign or house number",
                !c.body().contains("°") && !c.body().matches(".*\\d+(st|nd|rd|th) house.*"));
        }
        String plain = TogetherCards.meetHtml(p, false);
        String astro = TogetherCards.meetHtml(p, true);
        yes("plain view hides the why lines", !plain.contains("Why &mdash;"));
        yes("plain view hides the old cross-contact tables", !plain.contains("House overlays"));
        yes("Astrologer view shows the why lines", astro.contains("Why &mdash;"));
        yes("Astrologer view keeps the old tables, so nothing was lost",
            astro.contains("House overlays") && astro.contains("Angle contacts"));
        yes("chemistry in plain view hides the orbs",
            !TogetherCards.chemistryHtml(p, false).contains("orb "));
    }

    private static void smallPieces() {
        for (int h = 1; h <= 12; h++) {
            String theirs = LifeAreas.ofTheirs(h);
            yes("their house " + h + " says nothing about you: " + theirs,
                theirs != null && !theirs.matches(".*\\byou(r)?\\b.*"));
        }
        yes("their 1st house is how THEY come across",
            LifeAreas.ofTheirs(1).startsWith("how they come across"));
        yes("joinAnd of three", TogetherCards.joinAnd(List.of("Sun", "Moon", "Venus"))
            .equals("Sun, Moon and Venus"));
        yes("joinAnd of one", TogetherCards.joinAnd(List.of("Sun")).equals("Sun"));
        yes("a trine flows", TogetherCards.tone(Aspects.Type.TRINE).equals("EASY FLOW"));
        yes("a square has friction", TogetherCards.tone(Aspects.Type.SQUARE)
            .equals("SPARK AND FRICTION"));
        yes("a sesquiquadrate is the square's family",
            TogetherCards.tone(Aspects.Type.SESQUIQUADRATE).equals("SPARK AND FRICTION"));
        yes("the IC opens the 4th", Synastry.angleHouse("IC") == 4);
        yes("the MC opens the 10th", Synastry.angleHouse("MC") == 10);
    }

    // ------------------------------------------------------------------ Part F

    private static void door() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        OuraniaWindow w = hold[0];
        try {
            // The pair's pages are on the one strip, shown under the Synastry tab (2026-10-20).
            SubRail together = w.pagesRail();
            yes("the chart tabs have a strip", together != null);
            if (together == null) {
                return;
            }
            List<String> strip = together.pages();
            int meet = strip.indexOf(OuraniaWindow.SYNASTRY_PAGE);
            yes("Where you meet is followed by Chemistry: " + strip,
                meet >= 0 && meet + 1 < strip.size()
                    && OuraniaWindow.CHEMISTRY_PAGE.equals(strip.get(meet + 1)));
            yes("and both are pages of the Synastry tab", OuraniaWindow.CHART_TAB_PAGES
                .get(ChartTab.SYNASTRY).containsAll(List.of(OuraniaWindow.SYNASTRY_PAGE,
                    OuraniaWindow.CHEMISTRY_PAGE)));
            yes("the first page reads Where you meet",
                OuraniaWindow.MEET_LABEL.equals(together.pageLabel(OuraniaWindow.SYNASTRY_PAGE)));
            yes("the Synastry tab reads Synastry", "Synastry".equals(
                w.chartRail().pageLabel(ChartTab.SYNASTRY.door)));

            String blank = "<html><body></body></html>";
            String body = "<html><body>Angle contacts</body></html>";
            SwingUtilities.invokeAndWait(() -> w.updateChartSections(blank, blank, blank, blank, blank));
            yes("with one person both pages are greyed",
                !w.isPageEnabled(OuraniaWindow.SYNASTRY_PAGE)
                    && !w.isPageEnabled(OuraniaWindow.CHEMISTRY_PAGE));
            SwingUtilities.invokeAndWait(() -> w.updateChartSections(blank, blank, blank, body, blank));
            yes("with two both pages light",
                w.isPageEnabled(OuraniaWindow.SYNASTRY_PAGE)
                    && w.isPageEnabled(OuraniaWindow.CHEMISTRY_PAGE));
            // Chemistry is a Synastry page, and naming a page never changes the chart, so the
            // Synastry tab is put in front first (2026-10-20).
            SwingUtilities.invokeAndWait(() -> w.revealPage(OuraniaWindow.CHEMISTRY_PAGE));
            yes("from the Sky tab, naming Chemistry leaves the Sky in front",
                ChartTab.SKY.door.equals(w.chartRail().selected()));
            SwingUtilities.invokeAndWait(() -> {
                w.chartRail().choose(ChartTab.SYNASTRY.door);
                w.revealPage(OuraniaWindow.CHEMISTRY_PAGE);
            });
            yes("revealPage lands on Chemistry itself, not just its door",
                OuraniaWindow.CHEMISTRY_PAGE.equals(w.currentPage()));
            yes("allPages lists both Together pages",
                w.allPages().contains(OuraniaWindow.SYNASTRY_PAGE)
                    && w.allPages().contains(OuraniaWindow.CHEMISTRY_PAGE));

            // One setting, every box: ticking Astrologer view on one page shows on the others.
            @SuppressWarnings("unchecked")
            List<javax.swing.JCheckBox> boxes =
                (List<javax.swing.JCheckBox>) CheckReflect.get(w, "astrologerChecks");
            // Coming up, Where you meet, Chemistry, Coming up together, and since 2026-10-20 the
            // boxes on At a glance and Synthesize, which had been left out of the list; since
            // 2026-10-22 the Transits tab's three pages.
            yes("every page with the astrology box is in the one list (" + boxes.size() + ")",
                boxes.size() == 9);
            boolean was = Settings.astrologerView();
            try {
                SwingUtilities.invokeAndWait(() -> boxes.get(0).setSelected(!was));
                yes("ticking one sets the setting", Settings.astrologerView() == !was);
                boolean all = true;
                for (javax.swing.JCheckBox b : boxes) {
                    all &= b.isSelected() == !was;
                }
                yes("and every other box follows", all);
            } finally {
                Settings.setAstrologerView(was);
            }
        } finally {
            SwingUtilities.invokeAndWait(w::dispose);
        }
    }

    // ------------------------------------------------------------------ plumbing

    private static ChartFrame cast(SwissEph sw, int y, int m, int d, double hourUt) {
        SweDate sd = new SweDate(y, m, d, hourUt);
        return ChartFrame.compute(sw, sd.getJulDay(), 40.71, -74.01, 'P', false, 0.0);
    }

    /** The orb in a card's why lines, "orb 1.25°". */
    private static double orbOf(ReadingCard c) {
        for (String why : c.why()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("orb (\\d+\\.\\d+)")
                .matcher(why);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        }
        return Double.NaN;
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
