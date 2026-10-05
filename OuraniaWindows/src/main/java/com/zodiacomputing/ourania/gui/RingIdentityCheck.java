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
