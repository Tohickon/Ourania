package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * Which chart a thing belongs to, said once and said everywhere (2026-10-05).
 *
 * <p>David: <i>"can we add some better separation of chart b and chart a and transit chart when
 * it comes to the interpretations"</i>. The separation existed on the wheel and in one card -
 * the angle banner K7 built - and nowhere else, and where it did exist the two surfaces
 * disagreed about the colours.
 *
 * <pre>
 *  Part A - the enum can name every ring, and no two rings answer to the same name.
 *  Part B - the hues are the wheel's OWN previous literals, to the byte. The point of moving
 *           them into the enum was to end a second copy, not to restyle the wheel, and the
 *           globe is proved pixel-identical by a harness that would report any drift as mine.
 *  Part C - a group is labelled by its ring. The regression pin is NEGATIVE: a Chart B group
 *           must not render the words "Transiting now" anywhere, which is what it did.
 *  Part D - the panel's own answer. Which rings are listed follows the same gates the painter
 *           uses, so the list cannot claim a ring the wheel is not drawing.
 * </pre>
 *
 * <p><b>Why a negative assertion carries this suite.</b> "The Chart B group says Chart B" passes
 * on a method that labels every group "Chart B". The defect was a <i>wrong</i> label, not a
 * missing one, so what has to be asserted is that the wrong one is gone - and the wrong one was
 * plausible enough to survive a year of reading the code.
 */
public final class RingIdentityCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        part("A: every ring can be named", RingIdentityCheck::naming);
        part("B: the hues did not move", RingIdentityCheck::hues);
        part("C: a group is labelled by its ring", RingIdentityCheck::grouping);
        part("D: the panel lists the rings it is drawing", RingIdentityCheck::panel);
        part("E: a person and a moment are on different pages", RingIdentityCheck::pages);
        part("F: a page with nothing on it is greyed out", RingIdentityCheck::greying);
        part("G: an aspect list is asked of a ring, not of a boolean",
            RingIdentityCheck::ringAspects);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    // ---- Part A

    private static void naming() {
        TreeSet<String> headings = new TreeSet<>();
        for (WheelRing.Kind k : WheelRing.Kind.values()) {
            ok(k + " has a heading", k.heading != null && !k.heading.trim().isEmpty());
            ok(k + " has a hue", k.hueHex != null && k.hueHex.matches("#[0-9A-Fa-f]{6}"));
            ok(k + "'s hue decodes", AwtPen.colorOr(k.hueHex, null) != null);
            headings.add(k.heading);
        }
        // <b>Distinct headings, because the heading IS the separation.</b> Two rings sharing one
        // heading puts a reader back where this started: two lists under one name. The hues may
        // repeat - the wheel does not distinguish the outer moments - and that is deliberate and
        // asserted below, which is why the two are different assertions.
        ok("no two rings answer to the same heading ("
            + WheelRing.Kind.values().length + " rings, " + headings.size() + " headings)",
            headings.size() == WheelRing.Kind.values().length);
    }

    // ---- Part B

    private static void hues() {
        // The literals SkymapPanel held before 2026-10-05, written out here so that a change to
        // the enum has to be a deliberate change to the wheel as well.
        same("Chart B keeps the wheel's gold", WheelRing.Kind.CHART_B, new Color(255, 210, 122));
        same("the sky keeps the wheel's blue", WheelRing.Kind.SKY, new Color(143, 208, 255));
        same("Chart A keeps ringAngleInk's anchor", WheelRing.Kind.CHART_A,
            new Color(255, 228, 160));

        // <b>The outer moments share one colour ON PURPOSE.</b> ringInk paints every non-Chart-B
        // outer ring alike, so giving progressed, transiting, directed and sky separate hues here
        // would invent a distinction the drawing does not make - a list that is more precise than
        // the picture it describes is its own kind of lie. They are separated by heading instead.
        ok("transiting, progressed, directed and sky share the wheel's one outer hue",
            WheelRing.Kind.TRANSIT.hueHex.equals(WheelRing.Kind.SKY.hueHex)
                && WheelRing.Kind.PROGRESSED.hueHex.equals(WheelRing.Kind.SKY.hueHex)
                && WheelRing.Kind.SOLAR_ARC.hueHex.equals(WheelRing.Kind.SKY.hueHex));
        ok("and a second person does not share it",
            !WheelRing.Kind.CHART_B.hueHex.equals(WheelRing.Kind.SKY.hueHex));
    }

    private static void same(String what, WheelRing.Kind k, Color was) {
        Color now = AwtPen.colorOr(k.hueHex, null);
        ok(what + " (" + k.hueHex + " -> " + describe(now) + ", was " + describe(was) + ")",
            now != null && now.getRed() == was.getRed() && now.getGreen() == was.getGreen()
                && now.getBlue() == was.getBlue());
    }

    private static String describe(Color c) {
        return c == null ? "null" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    // ---- Part C

    private static Occupants.Group group(WheelRing.Kind kind, String body) {
        List<Occupants.Hit> hits = new ArrayList<>();
        hits.add(new Occupants.Hit(body, "X", 15.0, false));
        return new Occupants.Group(kind, hits);
    }

    private static void grouping() {
        String b = Occupants.html(
            Collections.singletonList(group(WheelRing.Kind.CHART_B, "Venus")), "house");
        ok("a Chart B group is headed Chart B", b.contains("Chart B"));
        // THE PIN. outerWheelShown returns true in a synastry, so the old two-list form put a
        // second person's natal placements under this heading - the same defect the body-click
        // path carries a comment about, on a surface that feeds six detail views.
        ok("and a Chart B group says nothing about transiting",
            !b.contains("Transiting now") && !b.toLowerCase().contains("transiting"));
        ok("and carries Chart B's own hue", b.contains(WheelRing.Kind.CHART_B.hueHex));
        ok("and names the body in it", b.contains("Venus"));

        String t = Occupants.html(
            Collections.singletonList(group(WheelRing.Kind.TRANSIT, "Saturn")), "sign");
        ok("a transit group is headed Transits", t.contains("Transits"));
        ok("and a transit group is not called Chart B", !t.contains("Chart B"));

        // Every ring the wheel can carry at once, since G17: the chart, a partner, the directed
        // band and the sky. The old form could hold two and silently dropped the rest.
        List<Occupants.Group> four = Arrays.asList(
            group(WheelRing.Kind.CHART_A, "Sun"),
            group(WheelRing.Kind.CHART_B, "Moon"),
            group(WheelRing.Kind.SOLAR_ARC, "Mars"),
            group(WheelRing.Kind.SKY, "Pluto"));
        String all = Occupants.html(four, "degree");
        for (Occupants.Group g : four) {
            ok(g.kind + " survives a four-ring list", all.contains(g.kind.heading));
        }
        ok("four rings, four bodies, none dropped",
            all.contains("Sun") && all.contains("Moon") && all.contains("Mars")
                && all.contains("Pluto"));
        // Order matters: the reader meets the rings going outward, as on the wheel.
        ok("and they are listed inner wheel first",
            all.indexOf(WheelRing.Kind.CHART_A.heading)
                < all.indexOf(WheelRing.Kind.CHART_B.heading)
            && all.indexOf(WheelRing.Kind.CHART_B.heading)
                < all.indexOf(WheelRing.Kind.SKY.heading));

        // Empty and absent rings leave no trace - a heading with nothing under it reads as a
        // bug, which is the reason html() returned "" rather than "nothing here" to begin with.
        List<Occupants.Group> sparse = Arrays.asList(
            new Occupants.Group(WheelRing.Kind.CHART_A, Collections.emptyList()),
            group(WheelRing.Kind.TRANSIT, "Mercury"),
            null);
        String one = Occupants.html(sparse, "sign");
        ok("an empty ring is not given a heading", !one.contains(WheelRing.Kind.CHART_A.heading));
        ok("a null group does not break the list", one.contains("Transits"));
        ok("nothing anywhere renders nothing at all",
            Occupants.html(Arrays.asList(
                new Occupants.Group(WheelRing.Kind.CHART_A, Collections.emptyList()),
                new Occupants.Group(WheelRing.Kind.SKY, null)), "house").isEmpty());
        ok("and so does no list at all", Occupants.html(null, "house").isEmpty());
    }

    // ---- Part D

    private static void panel() throws Exception {
        SkymapPanel[] hold = new SkymapPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new SkymapPanel(null));
        SkymapPanel sp = hold[0];

        // One body on the outer ring at 15 degrees of Aries, and a span that contains it. The
        // ring arrays are written directly rather than cast, so the question asked is which ring
        // the group is attributed to and nothing else.
        Arrays.fill(sp.outerRing.valid, false);
        sp.outerRing.lon[0] = 15.0;
        sp.outerRing.valid[0] = true;
        sp.showTransitChart = true;
        sp.showTriWheel = false;

        sp.chartMode = ChartMode.SYNASTRY;
        List<Occupants.Group> syn = sp.occupantGroups(0.0, 30.0);
        ok("a synastry's outer group is Chart B", kinds(syn).contains(WheelRing.Kind.CHART_B));
        ok("and a synastry's outer group is NOT a transit",
            !kinds(syn).contains(WheelRing.Kind.TRANSIT));
        String synHtml = Occupants.html(syn, "sign");
        ok("so the reader is told Chart B", synHtml.contains("Chart B"));
        ok("and is not told 'Transiting now'", !synHtml.contains("Transiting now"));
        // <b>Order is asserted on the panel's own answer, not only on a hand-built list.</b>
        // Part C proves html() preserves the order it is given; without this, a panel that
        // returned the rings outside-in would still pass every assertion above.
        ok("the inner wheel is the first group the panel returns",
            !syn.isEmpty() && (syn.get(0).kind == WheelRing.Kind.CHART_A
                || syn.get(0).kind == WheelRing.Kind.COMPOSITE));

        sp.chartMode = ChartMode.TRANSIT;
        sp.showProgressed = false;
        sp.showSolarArc = false;
        ok("a transit chart's outer group is a transit",
            kinds(sp.occupantGroups(0.0, 30.0)).contains(WheelRing.Kind.TRANSIT));

        sp.showProgressed = true;
        ok("a progressed chart's outer group is progressed",
            kinds(sp.occupantGroups(0.0, 30.0)).contains(WheelRing.Kind.PROGRESSED));
        sp.showProgressed = false;

        // The sky ring is gated on showTriWheel, which is what the painter asks. A list that
        // named a ring the wheel is not drawing would be worse than one that named none.
        ok("the sky is not listed when it is not drawn",
            !kinds(sp.occupantGroups(0.0, 30.0)).contains(WheelRing.Kind.SKY));
        Arrays.fill(sp.skyRing.valid, false);
        sp.skyRing.lon[1] = 20.0;
        sp.skyRing.valid[1] = true;
        sp.showTriWheel = true;
        List<Occupants.Group> withSky = sp.occupantGroups(0.0, 30.0);
        ok("and is listed when it is", kinds(withSky).contains(WheelRing.Kind.SKY));
        ok("and the sky is listed outermost, as it is drawn",
            withSky.get(withSky.size() - 1).kind == WheelRing.Kind.SKY);

        // An outer wheel that is switched off contributes nothing, whatever is in its arrays -
        // the arrays keep their last contents, so this is a real way to list a stale ring.
        sp.showTransitChart = false;
        sp.showTriWheel = false;
        List<WheelRing.Kind> off = kinds(sp.occupantGroups(0.0, 30.0));
        ok("no outer wheel, no outer groups, stale arrays or not",
            !off.contains(WheelRing.Kind.CHART_B) && !off.contains(WheelRing.Kind.TRANSIT)
                && !off.contains(WheelRing.Kind.SKY));
    }

    // ---- Part E

    /**
     * Chart B's page, the Transits page, and the houses each is read in.
     *
     * <p>Reflection into the real {@code generatePlanetPlacementsHtml}, which is how
     * {@code AspectGridCheck} and {@code NavigationCheck} already reach it. The rings are
     * written directly rather than cast, and the two sets of cusps are given deliberately
     * incompatible origins - Chart A's houses starting at 0 degrees and Chart B's at 180 - so
     * that one body at 15 degrees lands in house I under one and house VII under the other.
     * That is the only way to tell, from the markup, WHOSE houses a page is reading.
     */
    private static void pages() throws Exception {
        SkymapPanel[] hold = new SkymapPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new SkymapPanel(null));
        SkymapPanel sp = hold[0];

        Class<?> partType = Class.forName(
            "com.zodiacomputing.ourania.gui.SkymapPanel$PlacementPart");
        java.lang.reflect.Method gen = SkymapPanel.class.getDeclaredMethod(
            "generatePlanetPlacementsHtml", partType);
        gen.setAccessible(true);
        Object chartB = Enum.valueOf(partType.asSubclass(Enum.class), "CHART_B");
        Object transits = Enum.valueOf(partType.asSubclass(Enum.class), "TRANSITS");

        Arrays.fill(sp.outerRing.valid, false);
        sp.outerRing.lon[0] = 15.0;
        sp.outerRing.valid[0] = true;
        sp.showTransitChart = true;
        sp.showTriWheel = false;
        for (int i = 1; i <= 12; i++) {
            sp.activeCusps[i] = (i - 1) * 30.0;
            sp.outerRing.cusps[i] = ((i - 1) * 30.0 + 180.0) % 360.0;
        }

        sp.chartMode = ChartMode.SYNASTRY;
        String bPage = (String) gen.invoke(sp, chartB);
        String tPage = (String) gen.invoke(sp, transits);

        ok("a synastry's Chart B page carries their placements",
            bPage.contains("Placements"));
        ok("headed by the ring's own name", bPage.contains(WheelRing.Kind.CHART_B.heading));
        // THE SPLIT. Their placements were emitted onto the Transits page whatever the outer
        // ring was carrying, and the tab was renamed to cover for it.
        ok("and the Transits page does not carry them at all",
            !tPage.contains("Placements"));
        ok("so the Transits page is empty enough to be greyed out",
            !OuraniaWindow.hasContent(tPage));

        // Whose houses. "House VII" is Chart B's own cusps; "House I" would be Chart A's.
        ok("Chart B is read in Chart B's houses (House VII)",
            bPage.contains("House VII</a>"));
        ok("and not in Chart A's (House I)", !bPage.contains("House I</a>"));

        // A moment goes the other way round: on Transits, and in the houses on screen.
        sp.chartMode = ChartMode.TRANSIT;
        sp.showProgressed = false;
        sp.showSolarArc = false;
        String bEmpty = (String) gen.invoke(sp, chartB);
        String tFull = (String) gen.invoke(sp, transits);
        ok("a transit chart puts nothing on the Chart B page",
            !bEmpty.contains("Placements"));
        ok("so that page is greyed out instead", !OuraniaWindow.hasContent(bEmpty));
        ok("and the Transits page carries the moment", tFull.contains("Placements"));
        ok("headed Transits", tFull.contains(WheelRing.Kind.TRANSIT.heading));
        ok("read in the houses on screen (House I)", tFull.contains("House I</a>"));

        // And the tab stops having to stand in for a page that now exists. This is the 27 Sep
        // complaint - "where did the transit tab go in a synastry transits chart" - which was
        // one tab doing two jobs.
        ok("the second tab is never called Chart B now",
            !OuraniaWindow.secondPageTitle(WheelRing.Kind.CHART_B, false,
                WheelRing.Kind.SKY, true).contains("Chart B"));
        ok("and still names the sky when the sky is what it holds",
            OuraniaWindow.secondPageTitle(WheelRing.Kind.CHART_B, false,
                WheelRing.Kind.SKY, true).contains(WheelRing.Kind.SKY.heading));
        ok("and still names a transit ring when that is what it holds",
            OuraniaWindow.secondPageTitle(WheelRing.Kind.TRANSIT, true,
                WheelRing.Kind.SKY, false).contains(WheelRing.Kind.TRANSIT.heading));

        // <b>The rule that decides which rings reach that page, asserted on its own.</b> It was
        // one condition inside the caller, where nothing could reach it: every assertion above
        // hands secondPageTitle its arguments directly, so the caller could have gone on
        // sending a person to the second page and all of them would still pass.
        ok("a drawn second person does not reach the second page",
            !OuraniaWindow.outerReachesSecondPage(WheelRing.Kind.CHART_B, true));
        ok("a drawn transit ring does",
            OuraniaWindow.outerReachesSecondPage(WheelRing.Kind.TRANSIT, true));
        ok("and so does a drawn progressed ring, which is a moment of the same person",
            OuraniaWindow.outerReachesSecondPage(WheelRing.Kind.PROGRESSED, true));
        ok("and a directed ring", OuraniaWindow.outerReachesSecondPage(
            WheelRing.Kind.SOLAR_ARC, true));
        ok("an undrawn ring reaches nothing, whatever it is",
            !OuraniaWindow.outerReachesSecondPage(WheelRing.Kind.TRANSIT, false)
                && !OuraniaWindow.outerReachesSecondPage(WheelRing.Kind.CHART_B, false));
        ok("and a ring with no kind at all does not crash the naming",
            !OuraniaWindow.outerReachesSecondPage(null, true));
    }

    // ---- Part F

    /**
     * The tab is actually told, and not merely told something true.
     *
     * <p><b>This part exists because a mutation survived.</b> Part E asserts that the markup
     * for an absent Chart B is empty, and {@code hasContent} agrees it is empty - but nothing
     * joined those two facts to the rail, so {@code setPageEnabled(CHART_B_PAGE, true)}
     * unconditionally passed every assertion in this suite. A tab that opens onto a blank
     * panel reads as broken rather than as empty, which is the whole reason the greying is
     * there.
     *
     * <p>It builds the window, which the rest of this suite avoids, because the wiring is the
     * claim: the emptiness test and the rail call are two lines apart and either one can be
     * right while the pair is wrong. The Synastry page is asserted alongside it - its greying
     * had nothing holding it either, which is how this gap came to be inherited rather than
     * introduced.
     */
    private static void greying() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            // No "the page exists" assertion here: it would be a tautology, and this project
            // has found a guard no failure can reach seven times in one month. The page's
            // existence is proved below by its lighting UP - setPageEnabled on a label the
            // rail does not have leaves isPageEnabled false, so that assertion fails if the
            // page was never added.
            final DrawerRail rail = w[0].chartRail();

            String body = "<html><body>Chart B<br>Placements</body></html>";
            String blank = "<html><body></body></html>";

            javax.swing.SwingUtilities.invokeAndWait(
                () -> w[0].updateChartSections(blank, blank, blank, blank, blank));
            ok("with no second person the Chart B tab is greyed out",
                !rail.isPageEnabled(OuraniaWindow.CHART_B_PAGE));
            ok("and so is Synastry", !rail.isPageEnabled(OuraniaWindow.SYNASTRY_PAGE));
            ok("and so is Transits", !rail.isPageEnabled(OuraniaWindow.TRANSITS_PAGE));

            javax.swing.SwingUtilities.invokeAndWait(
                () -> w[0].updateChartSections(blank, blank, blank, blank, body));
            ok("with a second person the Chart B tab lights up",
                rail.isPageEnabled(OuraniaWindow.CHART_B_PAGE));

            // <b>And the page behind it actually holds the section.</b> Also found by a
            // surviving mutation: dropping the setHtml call left the tab lighting up over an
            // empty pane, and every assertion about the greying still passed. A lit tab and a
            // blank page is a worse failure than a greyed tab, because it looks like the app
            // has nothing to say about the second person rather than like a bug.
            final javax.swing.JEditorPane pane =
                (javax.swing.JEditorPane) CheckReflect.get(w[0], "chartBPane");
            final String[] shown = new String[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> shown[0] = pane.getText());
            ok("and the Chart B page holds Chart B's section",
                shown[0] != null && shown[0].contains("Placements"));
            ok("and Synastry stays greyed until it has its own content",
                !rail.isPageEnabled(OuraniaWindow.SYNASTRY_PAGE));

            javax.swing.SwingUtilities.invokeAndWait(
                () -> w[0].updateChartSections(blank, body, blank, body, body));
            ok("and each page lights from its OWN section, not from any of them",
                rail.isPageEnabled(OuraniaWindow.CHART_B_PAGE)
                    && rail.isPageEnabled(OuraniaWindow.SYNASTRY_PAGE)
                    && rail.isPageEnabled(OuraniaWindow.TRANSITS_PAGE));

            // Back to empty, because a tab that lights up and never goes dark again is the
            // same defect with a longer fuse - the rail would stay lit from a chart ago.
            javax.swing.SwingUtilities.invokeAndWait(
                () -> w[0].updateChartSections(blank, blank, blank, blank, blank));
            ok("and goes dark again when the second person is removed",
                !rail.isPageEnabled(OuraniaWindow.CHART_B_PAGE));
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    // ---- Part G

    /**
     * The aspect list, asked of a ring.
     *
     * <p><b>The assertion that carries this part is that a question can now be ASKED.</b> The
     * old signature took a boolean - outer ring or inner ring - so there was no way to ask
     * what the directed band aspects, and the answer to a question nobody could put is not
     * wrong, it is absent. G17 drew that band and it has made no lines and appeared in no
     * aspect list since.
     *
     * <p>The two equivalence assertions matter as much: the boolean form still has four
     * callers, and if it stopped agreeing with the ring form there would be two definitions of
     * what an aspect row is - which is what extracting this method was for.
     */
    private static void ringAspects() throws Exception {
        SkymapPanel[] hold = new SkymapPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new SkymapPanel(null));
        SkymapPanel sp = hold[0];
        sp.chartMode = ChartMode.TRANSIT;

        // <b>Far from zero on purpose, and a mutation is why.</b> The first version of this
        // fixture put everything at 10 degrees, where an unset longitude of 0.0 is still
        // within a luminary's conjunction orb - so a mutation that ignored the ring argument
        // entirely and read natalRing's positions SURVIVED, finding the "same" aspect by
        // accident. At 100 degrees an unset 0.0 is no aspect at all, so reading the wrong
        // ring's array can only produce an empty list.
        Arrays.fill(sp.natalRing.valid, false);
        Arrays.fill(sp.outerRing.valid, false);
        Arrays.fill(sp.arcRing.valid, false);
        Arrays.fill(sp.natalRing.lon, 0.0);
        Arrays.fill(sp.outerRing.lon, 0.0);
        Arrays.fill(sp.arcRing.lon, 0.0);
        sp.natalRing.lon[0] = 100.0;
        sp.natalRing.speed[0] = 1.0;
        sp.natalRing.valid[0] = true;
        sp.outerRing.lon[1] = 100.0;
        sp.outerRing.speed[1] = 1.0;
        sp.outerRing.valid[1] = true;
        sp.arcRing.lon[1] = 100.0;
        sp.arcRing.valid[1] = true;

        List<String[]> byRing = sp.getActiveAspectsFor(0, sp.natalRing);
        List<String[]> byFlag = sp.getActiveAspectsFor(0, false);
        ok("the inner wheel answers the same either way (" + byRing.size() + " vs "
            + byFlag.size() + ")", same(byRing, byFlag));

        List<String[]> outRing = sp.getActiveAspectsFor(1, sp.outerRing);
        List<String[]> outFlag = sp.getActiveAspectsFor(1, true);
        ok("and so does the outer ring", same(outRing, outFlag));
        ok("the outer ring finds its conjunction to the natal Sun", outFlag.size() == 1);
        // <b>WHICH aspect, not how many.</b> Counting rows let a mutation that ignored the
        // ring argument survive: it read natalRing's positions instead, found 100 degrees
        // against a natal body at 100 - no, against an unset 0.0 - and landed within orb of a
        // SQUARE to 90. One row either way, so the count agreed while the fact did not.
        ok("and it is a conjunction, which only the right ring's longitude can give",
            outFlag.size() == 1 && "Conjunction".equals(outFlag.get(0)[1]));

        // THE POINT OF THE CHANGE. There was no argument that named this ring.
        List<String[]> arc = sp.getActiveAspectsFor(1, sp.arcRing);
        ok("the DIRECTED band can be asked at all, and finds its contact ("
            + arc.size() + " row" + (arc.size() == 1 ? "" : "s") + ")", arc.size() == 1);
        ok("the contact it finds is to the natal Sun",
            arc.size() == 1 && "Sun".equals(arc.get(0)[0]));
        ok("and it is the conjunction, read from the directed ring's own longitude",
            arc.size() == 1 && "Conjunction".equals(arc.get(0)[1]));

        // The far side is always the natal chart: transit-to-transit is mundane weather, which
        // the sources confirmed on 2026-10-05 and the app has never drawn.
        Arrays.fill(sp.natalRing.valid, false);
        ok("with nothing in the natal chart, an outer body aspects nothing",
            sp.getActiveAspectsFor(1, sp.outerRing).isEmpty());
        ok("and neither does a directed one",
            sp.getActiveAspectsFor(1, sp.arcRing).isEmpty());
        sp.natalRing.valid[0] = true;

        // Robustness: a ring that is not there, and a body that is not on it.
        ok("a null ring answers empty rather than throwing",
            sp.getActiveAspectsFor(0, (WheelRing) null).isEmpty());
        ok("a body the ring does not carry answers empty",
            sp.getActiveAspectsFor(2, sp.arcRing).isEmpty());
        ok("and so does an index off the end",
            sp.getActiveAspectsFor(9999, sp.arcRing).isEmpty());

        // directedAspects gates on the band being drawn; the ring form deliberately does not,
        // so a caller that wants the contacts without the drawing can still have them.
        ok("directedAspects says nothing while the band is not drawn",
            !sp.arcRingDrawn() && sp.directedAspects(1).isEmpty());
    }

    /** Two aspect lists holding the same rows, compared cell by cell. */
    private static boolean same(List<String[]> a, List<String[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!Arrays.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** The kinds of the groups that actually have somebody in them. */
    private static List<WheelRing.Kind> kinds(List<Occupants.Group> groups) {
        List<WheelRing.Kind> out = new ArrayList<>();
        for (Occupants.Group g : groups) {
            if (g != null && !g.hits.isEmpty()) {
                out.add(g.kind);
            }
        }
        return out;
    }

    // ---- harness

    private interface Body {
        void run() throws Exception;
    }

    /**
     * One part, with its throw caught and reported as a failure.
     *
     * <b>A crashed suite reports no failures at all</b> - no ALL CLEAR line and no FAILURES
     * line - and this project has twice scored such a crash as a pass, once against its own
     * mutation harness. So every part is guarded and a throw is a named failure.
     */
    private static void part(String name, Body body) {
        System.out.println("=== Part " + name + " ===");
        try {
            body.run();
        } catch (Throwable t) {
            checks++;
            failures.add("Part " + name + " threw " + t);
            System.out.println("  THREW: " + t);
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        if (!pass) {
            failures.add(what);
            System.out.println("  FAIL: " + what);
        }
    }
}
