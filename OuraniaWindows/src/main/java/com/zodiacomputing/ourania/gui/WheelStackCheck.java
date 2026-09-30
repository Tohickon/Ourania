package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The wheel carrying more than three rings (G17).
 *
 * <pre>
 *  Part A - the nesting order, and that a new ring kind cannot dodge it.
 *  Part B - composition: what is asked for, sorted into the order it is drawn in.
 *  Part C - the geometry generalises and still produces the wheel it always did. This is the
 *           part that matters: the body bands were a two-link chain written out as two
 *           statements, and rolling them into a loop must not move a single pixel.
 *  Part D - the band depth at two bands is the historical figure exactly, for every size.
 *  Part E - a stack that does not fit is refused, and the natal wheel keeps its floor. The
 *           claim is measured off the real radii rather than restated as arithmetic.
 *  Part F - the two orders are opposite, which is the seam a ring gets lost through.
 *
 *   java -cp "src\main\java;lib\*" com.zodiacomputing.ourania.gui.WheelStackCheck
 * </pre>
 */
public final class WheelStackCheck {

    private WheelStackCheck() { }

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) {
        Settings.useScratchFile();
        part("A: the nesting order", WheelStackCheck::order);
        part("B: what the wheel is carrying", WheelStackCheck::composition);
        part("C: the band chain, rolled up", WheelStackCheck::chain);
        part("D: the depth at two bands is the old depth", WheelStackCheck::depth);
        part("E: what does not fit is refused, not drawn", WheelStackCheck::fitting);
        part("F: inner-first and outer-first", WheelStackCheck::seam);
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

    /**
     * <b>Every kind must be placed, which is the array answering the switch-with-a-default
     * problem.</b> A seventh {@code WheelRing.Kind} was added on 27 September and the headings
     * lived in a switch with a default, so a new kind would have been silently called "Chart".
     * The same shape is available here: a kind left out of {@code ORDER} would simply never be
     * drawn, and nothing would say so. So the partition is asserted - every kind is either an
     * inner wheel or has a place in the order, and none is both.
     */
    private static void order() {
        Set<WheelRing.Kind> inOrder = EnumSet.noneOf(WheelRing.Kind.class);
        for (WheelRing.Kind k : WheelStack.ORDER) {
            ok("no kind appears twice in the order: " + k, inOrder.add(k));
        }
        Set<WheelRing.Kind> inner = EnumSet.of(WheelRing.Kind.CHART_A, WheelRing.Kind.COMPOSITE);
        for (WheelRing.Kind k : WheelRing.Kind.values()) {
            ok(k + " is either an inner wheel or a band, and says which",
                inner.contains(k) != inOrder.contains(k));
        }
        ok("the order fills the bands it offers (" + WheelStack.ORDER.length + " of "
            + WheelStack.MOST_BANDS + ")", WheelStack.ORDER.length == WheelStack.MOST_BANDS);

        // The nesting claim itself, and it is astrology rather than layout: the person's own
        // derived charts sit inside anyone else's chart, and the sky is outside everything.
        List<WheelRing.Kind> o = Arrays.asList(WheelStack.ORDER);
        ok("progressed nests inside the partner",
            o.indexOf(WheelRing.Kind.PROGRESSED) < o.indexOf(WheelRing.Kind.CHART_B));
        ok("directed nests inside the partner",
            o.indexOf(WheelRing.Kind.SOLAR_ARC) < o.indexOf(WheelRing.Kind.CHART_B));
        ok("the sky is outermost",
            o.indexOf(WheelRing.Kind.SKY) == WheelStack.ORDER.length - 1);
        ok("transits are outside the partner and inside the sky",
            o.indexOf(WheelRing.Kind.CHART_B) < o.indexOf(WheelRing.Kind.TRANSIT)
                && o.indexOf(WheelRing.Kind.TRANSIT) < o.indexOf(WheelRing.Kind.SKY));

        // Every band kind must carry a ring word, because a body from it is named in prose.
        // The inner wheel is the one that may answer null - it takes no qualifier.
        for (WheelRing.Kind k : WheelStack.ORDER) {
            ok(k + " has a word for prose", k.ringWord != null && !k.ringWord.isEmpty());
        }
    }

    private static void composition() {
        // <b>The order asked in is thrown away.</b> A checkbox list hands its kinds over in
        // whatever order they were clicked, and letting that decide the nesting would put the
        // sky under the partner on a Tuesday.
        WheelStack backwards = WheelStack.of(WheelRing.Kind.CHART_A, Arrays.asList(
            WheelRing.Kind.SKY, WheelRing.Kind.PROGRESSED, WheelRing.Kind.TRANSIT));
        ok("asked backwards, nested forwards: " + backwards.bands(),
            backwards.bands().equals(Arrays.asList(WheelRing.Kind.PROGRESSED,
                WheelRing.Kind.TRANSIT, WheelRing.Kind.SKY)));

        // The four-wheel overlay this row exists for: natal, progressed, directed, transits.
        WheelStack four = WheelStack.of(WheelRing.Kind.CHART_A, Arrays.asList(
            WheelRing.Kind.TRANSIT, WheelRing.Kind.SOLAR_ARC, WheelRing.Kind.PROGRESSED));
        ok("progressions and solar arc are on the wheel at the same time",
            four.has(WheelRing.Kind.PROGRESSED) && four.has(WheelRing.Kind.SOLAR_ARC));
        ok("and so are transits, which is four wheels", four.bandCount() == 3
            && four.kinds().size() == 4);
        ok("directed is drawn outside progressed",
            four.indexOf(WheelRing.Kind.SOLAR_ARC) > four.indexOf(WheelRing.Kind.PROGRESSED));
        ok("the inner wheel is index 0", four.indexOf(WheelRing.Kind.CHART_A) == 0);
        ok("a kind that is not up is not on the wheel",
            four.indexOf(WheelRing.Kind.CHART_B) == -1 && !four.has(WheelRing.Kind.CHART_B));

        WheelStack twice = WheelStack.of(WheelRing.Kind.CHART_A,
            Arrays.asList(WheelRing.Kind.SKY, WheelRing.Kind.SKY, null));
        ok("a band asked for twice is one band", twice.bandCount() == 1);
        ok("a null in the list is not a band", twice.bands().contains(WheelRing.Kind.SKY));

        // An inner kind handed in as a band would be the chart drawn around itself.
        WheelStack selfWrapped = WheelStack.of(WheelRing.Kind.CHART_A,
            Arrays.asList(WheelRing.Kind.CHART_A, WheelRing.Kind.COMPOSITE));
        ok("the chart is not wrapped around itself", selfWrapped.bandCount() == 0);

        // A composite is an inner wheel that belongs to nobody, and keeps its seat.
        ok("a composite may sit at the centre",
            WheelStack.single(WheelRing.Kind.COMPOSITE).inner() == WheelRing.Kind.COMPOSITE);
        // A band kind asked for as the centre is a programming error, and falls back rather
        // than throwing out of a paint - a wheel that draws is worth more than a stack trace.
        ok("a band kind cannot take the centre",
            WheelStack.single(WheelRing.Kind.SKY).inner() == WheelRing.Kind.CHART_A);

        WheelStack alone = WheelStack.single(WheelRing.Kind.CHART_A);
        ok("a single wheel has no bands", alone.bandCount() == 0);
        ok("and is still one wheel", alone.kinds().size() == 1);
        ok("and has dropped nothing", alone.dropped().isEmpty());

        // The list is handed out, so it must not be editable from outside.
        try {
            four.bands().add(WheelRing.Kind.SKY);
            ok("the bands cannot be edited by a caller", false);
        } catch (UnsupportedOperationException expected) {
            ok("the bands cannot be edited by a caller", true);
        }
    }

    /**
     * <b>The historical formula is held here as a frozen copy, which is the one place that is
     * the right thing to do.</b> Restating a rule in its own check proves only that two copies
     * agree - but this copy is not of the rule, it is of the wheel as it stood before the
     * change, and the whole claim of the refactor is that the new code reproduces it. That is
     * what {@code AspectGridCheck} Part J does for the same arithmetic, and it is what caught
     * an earlier attempt at this that "looked equivalent and was not".
     */
    private static void chain() {
        int[] sizes = {200, 320, 480, 512, 700, 900, 1080, 1441};
        double[][] opens = {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {0.37, 0.62}, {0.5, 0.5}};
        for (int size : sizes) {
            for (double[] op : opens) {
                double t = op[0];
                double o = op[1];
                int outer = Math.min(size, size) / 2 - 10;
                int depth = WheelLayout.outerBandDepth(outer);
                int[] radii = WheelLayout.ringRadii(size, size, o, t);
                // The formula exactly as it stood before bodyBands existed.
                int wasTri = radii[WheelLayout.RING_DEGREE_INNER];
                int wasTransit = (int) Math.round((double) wasTri - (double) depth * t);
                int wasBodyTop = (int) Math.round((double) wasTransit - (double) depth * o);
                ok("tri unchanged at " + size + " " + Arrays.toString(op),
                    radii[WheelLayout.RING_TRI] == wasTri);
                ok("transit unchanged at " + size + " " + Arrays.toString(op),
                    radii[WheelLayout.RING_TRANSIT] == wasTransit);
                ok("body top unchanged at " + size + " " + Arrays.toString(op),
                    radii[WheelLayout.RING_BODY_TOP] == wasBodyTop);

                // And the panel actually goes through the generalisation rather than keeping
                // its own copy beside it - which is the failure this refactor could have.
                int[] bands = WheelLayout.bodyBands(wasTri, depth, new double[] {t, o});
                ok("the wheel's radii come from bodyBands at " + size,
                    bands[0] == radii[WheelLayout.RING_TRI]
                        && bands[1] == radii[WheelLayout.RING_TRANSIT]
                        && bands[2] == radii[WheelLayout.RING_BODY_TOP]);
            }
        }

        // <b>Rounding compounds from the rounded value, not the exact one.</b> Measuring every
        // band off an unrounded running total is the more obvious code and is a different
        // wheel: at depth 37 and three half-open bands it parts company by a pixel.
        int[] stepped = WheelLayout.bodyBands(500, 37, new double[] {0.5, 0.5, 0.5});
        ok("each edge is measured from the rounded one before it: "
            + Arrays.toString(stepped),
            stepped[1] == 482 && stepped[2] == 464 && stepped[3] == 446);

        // A band that is shut takes no room at all, which is what lets one wheel lay out as it
        // always has however many bands are declared.
        int[] shut = WheelLayout.bodyBands(400, 40, new double[] {0, 0, 0, 0, 0});
        for (int i = 0; i < shut.length; i++) {
            ok("a shut band takes nothing (edge " + i + ")", shut[i] == 400);
        }
        ok("one more edge than bands", shut.length == 6);

        int[] none = WheelLayout.bodyBands(400, 40, new double[0]);
        ok("no bands is the ceiling alone", none.length == 1 && none[0] == 400);
        ok("null bands is the ceiling alone",
            WheelLayout.bodyBands(400, 40, null).length == 1);

        // Out-of-range fractions are clamped rather than allowed to invert the chain.
        int[] wild = WheelLayout.bodyBands(400, 40, new double[] {-3.0, 9.0});
        ok("a negative fraction does not push a band outward", wild[1] == 400);
        ok("a fraction over one does not take two bands' worth", wild[2] == 360);

        // Five open bands is the point of the exercise: strictly decreasing, nothing inverted.
        int[] five = WheelLayout.bodyBands(500, 30, new double[] {1, 1, 1, 1, 1});
        for (int i = 1; i < five.length; i++) {
            ok("band " + i + " sits inside band " + (i - 1), five[i] < five[i - 1]);
        }
        ok("five open bands take five depths", five[5] == 500 - 5 * 30);
    }

    /**
     * <b>{@code (outer / 3) / 2} and {@code outer / 6} are the same integer.</b> That is a true
     * identity, and believing algebra about integer division is how a wheel moves by a pixel at
     * one window size in fifty. Measured instead, across every width the wheel can have.
     */
    private static void depth() {
        int drift = 0;
        for (int outer = 40; outer <= 1200; outer++) {
            // Frozen: the formula as it read before it took a band count, with its own
            // literals rather than the constants it derived them from.
            int historical = Math.max(WheelLayout.MIN_BAND_DEPTH,
                Math.min(2 * 20 + 2 * WheelLayout.BAND_EDGE, outer / 6));
            if (WheelLayout.outerBandDepth(outer, 2) != historical
                || WheelLayout.outerBandDepth(outer) != historical) {
                drift++;
            }
        }
        ok("the two-band depth is the historical depth at every width 40-1200 ("
            + drift + " disagreed)", drift == 0);
        ok("one band is not handed the whole budget",
            WheelLayout.outerBandDepth(600, 1) == WheelLayout.outerBandDepth(600, 2));

        // <b>Sharing only bites where the share is the binding constraint.</b> The first draft
        // of this asserted that four bands are always shallower than two and was wrong: at 900
        // both hit the ideal depth of 66, because there is room for four bands at full depth
        // and shrinking them would buy nothing. The budget is a cap, not a quota.
        ok("with room to spare, more bands does not mean thinner bands",
            WheelLayout.outerBandDepth(900, 4) == WheelLayout.outerBandDepth(900, 2));
        ok("where the budget binds, more bands do share it",
            WheelLayout.outerBandDepth(300, 5) < WheelLayout.outerBandDepth(300, 2));
        ok("a band never goes below the floor a glyph needs",
            WheelLayout.outerBandDepth(120, 5) >= WheelLayout.MIN_BAND_DEPTH);
        ok("more bands never means a deeper band",
            WheelLayout.outerBandDepth(900, 5) <= WheelLayout.outerBandDepth(900, 3));
    }

    /**
     * <b>The claim is measured off the radii, not restated.</b> "The natal wheel keeps its
     * floor" is only worth asserting if it is asked of the wheel that would actually be drawn -
     * an assertion that recomputes the budget and compares it to itself would pass however
     * wrong the layout was.
     */
    private static void fitting() {
        int smallest = 0;
        for (int size = 300; size <= 1600; size += 20) {
            int outer = size / 2 - 10;
            int most = WheelLayout.maxBodyBands(outer);
            ok("never more bands than the order offers at " + size,
                most <= WheelStack.MOST_BANDS);
            if (most == 0) {
                continue;
            }
            if (smallest == 0) {
                smallest = size;
            }

            // The whole point: draw that many bands and the natal wheel still has its floor.
            double[] all = new double[most];
            Arrays.fill(all, 1.0);
            int[] radii = WheelLayout.ringRadii(size, size, 1.0, 1.0);
            int[] bands = WheelLayout.bodyBands(radii[WheelLayout.RING_DEGREE_INNER],
                WheelLayout.outerBandDepth(outer, most), all);
            ok("the natal wheel keeps its floor at " + size + " with " + most + " bands ("
                + bands[most] + " >= " + WheelLayout.minNatalRadius() + ")",
                bands[most] >= WheelLayout.minNatalRadius());
        }
        ok("a window too small for any band is reported rather than drawn on (first that fits: "
            + smallest + ")", smallest > 300);

        // <b>And the defect the floor assertion found, pinned so it cannot come back.</b> The
        // wheel has always laid an outer band out at these sizes regardless, which leaves the
        // natal wheel at a NEGATIVE radius - drawn every frame, reported by nothing. This is
        // older than the stack and is why maxBodyBands is allowed to answer zero.
        int[] tiny = WheelLayout.ringRadii(300, 300, 1.0, 1.0);
        ok("the unguarded layout really does go negative at 300 ("
            + tiny[WheelLayout.RING_BODY_TOP] + ")",
            tiny[WheelLayout.RING_BODY_TOP] < 0);
        ok("and the fit rule refuses that window",
            WheelLayout.maxBodyBands(300 / 2 - 10) == 0);

        // A bigger window never carries fewer rings.
        int worse = 0;
        for (int outer = 100; outer < 1000; outer++) {
            if (WheelLayout.maxBodyBands(outer + 1) < WheelLayout.maxBodyBands(outer)) {
                worse++;
            }
        }
        ok("a wider wheel never carries fewer bands (" + worse + " did)", worse == 0);
        ok("a big window carries the whole order",
            WheelLayout.maxBodyBands(1000) == WheelStack.MOST_BANDS);

        WheelStack six = WheelStack.of(WheelRing.Kind.CHART_A,
            Arrays.asList(WheelStack.ORDER));
        ok("everything at once is six wheels", six.kinds().size() == 6);

        WheelStack cut = six.trimmedTo(2);
        ok("trimmed to what fits", cut.bandCount() == 2);
        ok("and the bands kept are the innermost",
            cut.bands().equals(Arrays.asList(WheelRing.Kind.PROGRESSED,
                WheelRing.Kind.SOLAR_ARC)));
        ok("what was dropped is named rather than forgotten", cut.dropped().size() == 3);
        ok("outermost first, because that is the one to complain about",
            cut.dropped().get(0) == WheelRing.Kind.SKY);
        ok("and the sentence names it", cut.toString().contains("no room for"));
        ok("the chart itself is never trimmed away",
            six.trimmedTo(0).inner() == WheelRing.Kind.CHART_A);
        ok("trimming to more than there are drops nothing",
            six.trimmedTo(99).dropped().isEmpty());
        ok("and returns the same stack", six.trimmedTo(99) == six);
        ok("a negative trim is read as none", six.trimmedTo(-4).bandCount() == 0);
    }

    private static void seam() {
        WheelStack s = WheelStack.of(WheelRing.Kind.CHART_A, Arrays.asList(
            WheelRing.Kind.PROGRESSED, WheelRing.Kind.TRANSIT, WheelRing.Kind.SKY));
        // bands() counts outward from the chart; opens() counts inward from the zodiac. A ring
        // half-open inside must arrive at the geometry as the LAST entry, not the first.
        double[] open = {0.25, 0.5, 0.75};
        double[] outward = s.opens(open);
        ok("one fraction per band", outward.length == 3);
        ok("the innermost band's fraction arrives last: " + Arrays.toString(outward),
            Math.abs(outward[2] - 0.25) < 1e-9);
        ok("and the outermost band's arrives first",
            Math.abs(outward[0] - 0.75) < 1e-9);
        ok("the middle is unmoved", Math.abs(outward[1] - 0.5) < 1e-9);

        double[] wideOpen = s.opens(null);
        ok("no fractions means fully open", wideOpen.length == 3
            && wideOpen[0] == 1.0 && wideOpen[1] == 1.0 && wideOpen[2] == 1.0);
        ok("a short list opens the rest of the way",
            s.opens(new double[] {0.5})[2] == 0.5 && s.opens(new double[] {0.5})[0] == 1.0);
        ok("no bands is no fractions",
            WheelStack.single(WheelRing.Kind.CHART_A).opens(null).length == 0);

        // The heading is what a tab and a scrub bar call the ring, and two rings sharing one
        // would be two controls a reader cannot tell apart.
        List<String> headings = new ArrayList<>();
        for (WheelRing.Kind k : s.kinds()) {
            ok("every ring on the wheel has a heading: " + k,
                k.heading != null && !k.heading.isEmpty());
            ok("and no two share it: " + k.heading, !headings.contains(k.heading));
            headings.add(k.heading);
        }
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void ok(String what, boolean pass) {
        checks++;
        if (!pass) {
            System.out.println("  FAIL " + what);
            failures.add(what);
        }
    }
}
