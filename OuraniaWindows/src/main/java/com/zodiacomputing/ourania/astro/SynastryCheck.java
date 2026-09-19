package com.zodiacomputing.ourania.astro;

import com.zodiacomputing.ourania.gui.ChartMode;
import com.zodiacomputing.ourania.gui.InterpretationService;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies the synastry path: aspects read between two charts rather than within one.
 *
 * <b>Written because synastry was the last shipped feature with no suite behind it.</b>
 * The composites got CompositeCheck; synastry has been wired into the UI through
 * ChartMode since 2026-08-16 and guarded by nothing. Everything below is the shared
 * arithmetic the wheel, the grid and the hit-detection all call - Aspects and Bodies -
 * so a change to any of them that breaks the two-chart reading fails here rather than on
 * screen.
 *
 * <b>What this suite deliberately does not do.</b> It does not construct SkymapPanel.
 * The grid and the aspect lines are private instance methods on a Swing component, and a
 * check that reimplemented their loops here would be a second copy of the rule - the
 * defect this project logs more than any other. It asserts the rules those loops call,
 * and the facts that decide whether the loops are right. Two places where the loops
 * currently disagree with each other are recorded in HANDOVER.md as findings, not
 * encoded here as expectations: they change what a user reads, which is David's call.
 */
public final class SynastryCheck {

    private static final String EPHE_PATH = Ephemeris.PATH;

    // Los Angeles, as in CompositeCheck - same two people, so the two suites can be read
    // against each other.
    private static final double LAT = 34.05;
    private static final double LON = -118.24;

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        int before;

        System.out.println("=== Part A: Cross-chart geometry ===");
        before = failures.size();
        crossChartGeometry();
        report("Part A", before);

        System.out.println("=== Part B: The synastry orb contract ===");
        before = failures.size();
        synastryOrbs();
        report("Part B", before);

        System.out.println("=== Part C: What a cross-chart contact means ===");
        before = failures.size();
        crossChartContacts();
        report("Part C", before);

        System.out.println("=== Part D: Mode wiring ===");
        before = failures.size();
        modeWiring();
        report("Part D", before);

        System.out.println("=== Part E: House overlays ===");
        before = failures.size();
        houseOverlays();
        report("Part E", before);

        System.out.println("=== Part F: Angle contacts ===");
        before = failures.size();
        angleContacts();
        report("Part F", before);

        System.out.println("=== Part G: Relationship prose ===");
        before = failures.size();
        relationshipProse();
        report("Part G", before);

        System.out.println("=== Part H: The 0/360 seam ===");
        before = failures.size();
        seam();
        report("Part H", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ Part A

    /**
     * The two-chart matrix, against two real charts.
     *
     * A natal grid is triangular and its diagonal is meaningless: a body does not aspect
     * itself. A synastry grid is neither. Every one of the ordered cells carries a
     * different reading, the diagonal included - A's Sun on B's Sun is the contact people
     * come for. These checks state that difference in a form that fails if anything
     * starts treating the cross matrix like the natal one.
     */
    private static void crossChartGeometry() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame a = run(sw, 1980, 5, 15, 12.0);
        ChartFrame b = run(sw, 1990, 8, 20, 8.0);

        int asymmetric = 0;
        int diagonalHits = 0;

        for (ChartFrame.Body ba : a.bodies) {
            if (!ba.ok) continue;
            for (ChartFrame.Body bb : b.bodies) {
                if (!bb.ok) continue;

                // Separation does not care which chart is the inner wheel.
                double sepAB = Aspects.separation(ba.lon, bb.lon);
                double sepBA = Aspects.separation(bb.lon, ba.lon);
                near("separation symmetric " + ba.name + "/" + bb.name, sepAB, sepBA, 1e-12);

                // The type agrees with the orb it was judged on. This is the invariant the
                // grid depends on: a symbol in a cell means the separation really is inside
                // that aspect's orb, and an empty cell means no aspect's is.
                double orb = Aspects.orbFor(ba.name, bb.name);
                Aspects.Type t = Aspects.typeOf(sepAB, ba.name, bb.name);
                checks++;
                double effOrb = t == null ? orb : Math.min(orb, t.maxOrb);
                if (t != null && Math.abs(sepAB - t.exactAngle) > effOrb) {
                    failures.add(ba.name + "/" + bb.name + " typed " + t.label
                        + " at " + sepAB + " outside its orb of " + effOrb);
                } else if (t == null && anyTypeWithin(sepAB, orb)
                        && !Aspects.bothCalculated(ba.name, bb.name)) {
                    failures.add(ba.name + "/" + bb.name + " typed null at " + sepAB
                        + " while inside an orb of " + orb);
                }

                if (ba.name.equals(bb.name) && t != null) {
                    diagonalHits++;
                }
            }
        }

        // Swapping which person is the inner wheel is not a relabelling: A-Sun to B-Moon
        // and B-Sun to A-Moon are different contacts. If this ever came back zero, the
        // matrix would have collapsed into a natal-shaped one.
        for (int i = 0; i < a.bodies.length; i++) {
            for (int j = 0; j < b.bodies.length; j++) {
                if (i == j) continue;
                if (!a.bodies[i].ok || !b.bodies[j].ok
                    || !b.bodies[i].ok || !a.bodies[j].ok) {
                    continue;
                }
                Aspects.Type ab = Aspects.typeOf(
                    Aspects.separation(a.bodies[i].lon, b.bodies[j].lon),
                    a.bodies[i].name, b.bodies[j].name);
                Aspects.Type ba2 = Aspects.typeOf(
                    Aspects.separation(b.bodies[i].lon, a.bodies[j].lon),
                    b.bodies[i].name, a.bodies[j].name);
                if (ab != ba2) {
                    asymmetric++;
                }
            }
        }
        checks++;
        if (asymmetric == 0) {
            failures.add("The cross-chart matrix is symmetric under swapping the charts - "
                + "it should not be; each direction is its own reading");
        }
        System.out.println("  ordered cross pairs that change under the swap: " + asymmetric);
        System.out.println("  same-body cross contacts in aspect: " + diagonalHits);

        // The diagonal, stated without depending on where these two charts happen to fall.
        // A body against ITSELF in one chart is skipped; against its counterpart in the
        // other chart it is the headline contact.
        eq("Same-name cross pair is a real conjunction",
            Aspects.Type.CONJUNCTION, Aspects.typeOf(0.0, "Sun", "Sun"));
        eq("Same-name cross pair at 180 is a real opposition",
            Aspects.Type.OPPOSITION, Aspects.typeOf(180.0, "Moon", "Moon"));

        // Applying is judged by stepping both forward a day, so it reads the same whichever
        // chart is named first. In synastry one chart is not "the moving one" - both bodies
        // carry their own speed and both are used.
        for (ChartFrame.Body ba : a.bodies) {
            if (!ba.ok) continue;
            for (ChartFrame.Body bb : b.bodies) {
                if (!bb.ok) continue;
                Aspects.Type t = Aspects.typeOf(Aspects.separation(ba.lon, bb.lon), ba.name, bb.name);
                if (t == null) continue;
                boolean fwd = Aspects.isApplying(ba.lon, ba.lonSpeed, bb.lon, bb.lonSpeed, t);
                boolean rev = Aspects.isApplying(bb.lon, bb.lonSpeed, ba.lon, ba.lonSpeed, t);
                eq("applying reads the same either way round " + ba.name + "/" + bb.name, fwd, rev);
            }
        }
    }

    /**
     * True when some aspect really should have matched at this separation.
     *
     * <b>Each aspect is judged against its OWN ceiling, not against the body orb.</b> Until
     * 2026-08-23 every aspect used the bodies' orb, so testing them all against one number was
     * correct; then five minor aspects arrived with a 1-degree cap and the quincunx was capped
     * with them. This method kept asking the old question and reported 243 failures on the
     * first run - every one of them a separation inside the 10-degree solar orb of some minor
     * aspect but well outside that aspect's actual 1 degree.
     *
     * <b>Those failures were correct behaviour described by a stale assertion.</b> The fix is
     * to make the check agree with the model, not to widen it.
     *
     * <b>Be clear about what this half can and cannot catch.</b> It now applies the same
     * Math.min that typeOf applies, so if that formula were wrong both would be wrong together
     * and this would stay silent - it is a consistency check, not an independent one. The
     * assertion that does bite is the other branch at the call site: a type that comes back
     * must sit inside *its own declared maxOrb*, which tests the result against the enum
     * rather than against a recomputation.
     */
    private static boolean anyTypeWithin(double sep, double orb) {
        for (Aspects.Type t : Aspects.Type.values()) {
            double effective = Math.min(orb, t.maxOrb);
            if (t == Aspects.Type.CONJUNCTION ? sep <= effective
                                              : Math.abs(sep - t.exactAngle) <= effective) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ Part B

    /**
     * The halved synastry orb.
     *
     * <b>Read this before changing anything here.</b> {@code Aspects.orbFor(a, b, true)}
     * and {@code Aspects.typeOf(.., true)} halve the orb for a cross-chart pair. When this
     * suite was written NOTHING CALLED THEM; as of 2026-08-24 exactly one thing does -
     * {@code Synastry.angleContacts} - and <b>the wheel, the grid and the hit test still
     * do not</b>, so the synastry grid on screen is still read at full natal orbs.
     *
     * <b>The halving got a second half on 2026-08-24, and the reason is in
     * {@link Aspects#effectiveOrb}.</b> Halving the body orb alone reached every Ptolemaic
     * aspect and not one minor aspect, because {@link Aspects.Type#maxOrb} already capped
     * the six minors at 1.0 - tighter than half of the smallest body orb - so
     * {@code Math.min} was binding on all of them before the halving arrived. Measured over
     * 300 ordered pairs that pushed the minors' share of a synastry from a natal chart's 16%
     * to 28%. The cap is now halved alongside the orb, which keeps 49-52% of every one of
     * the eleven aspect types instead of 100% of six of them.
     *
     * <b>Part B is where that contract lives.</b> The checks below guard it so that the day
     * the grid IS wired in, it is wired to something proven: exactly half, symmetric, capped
     * in step, and never widening what the natal orb already found.
     */
    private static void synastryOrbs() {
        int n = Bodies.count();
        for (int i = 0; i < n; i++) {
            String a = Bodies.at(i).name;
            for (int j = 0; j < n; j++) {
                String b = Bodies.at(j).name;
                double wide = Aspects.orbFor(a, b);
                double tight = Aspects.orbFor(a, b, true);
                near("synastry orb is half the natal orb " + a + "/" + b, wide / 2.0, tight, 1e-12);
                near("orb is symmetric " + a + "/" + b, wide, Aspects.orbFor(b, a), 1e-12);
            }
        }

        // A narrower orb can only ever find fewer aspects, never a different one. Swept
        // rather than sampled: the conjunction is tested before the others and against the
        // orb directly, so it is the one place a crossover could hide.
        String[] probes = {"Sun", "Moon", "Mercury", "Saturn", "Chiron", "Vesta", "Ascendant"};
        for (String a : probes) {
            for (String b : probes) {
                for (double sep = 0.0; sep <= 180.0; sep += 0.25) {
                    Aspects.Type tight = Aspects.typeOf(sep, a, b, true);
                    if (tight == null) {
                        continue;
                    }
                    Aspects.Type wide = Aspects.typeOf(sep, a, b, false);
                    eq("halved orb never finds what the full orb misses "
                        + a + "/" + b + " at " + sep, tight, wide);
                }
            }
        }

        // The boundary, on a pair whose orb is known from the table: Sun and Moon are 10,
        // so synastry is 5.
        near("Sun/Moon natal orb", 10.0, Aspects.orbFor("Sun", "Moon"), 1e-12);
        near("Sun/Moon synastry orb", 5.0, Aspects.orbFor("Sun", "Moon", true), 1e-12);
        eq("conjunction holds at the halved orb",
            Aspects.Type.CONJUNCTION, Aspects.typeOf(5.0, "Sun", "Moon", true));
        eq("conjunction is gone just outside it",
            null, Aspects.typeOf(5.5, "Sun", "Moon", true));
        eq("but the full orb still has it",
            Aspects.Type.CONJUNCTION, Aspects.typeOf(5.5, "Sun", "Moon", false));

        // The two-argument form is the full orb, not the halved one. Stated because the
        // whole grid depends on it and it is one keystroke from being otherwise.
        eq("the two-argument typeOf is the natal reading",
            Aspects.typeOf(5.5, "Sun", "Moon", false), Aspects.typeOf(5.5, "Sun", "Moon"));

        // ---- the cap halves in step with the orb (2026-08-24) ----
        //
        // This is the half that was missing. Every assertion above would have passed
        // unchanged while the six minor aspects went untouched by the halving, which is
        // exactly what happened for six days.
        for (int i = 0; i < n; i++) {
            String a = Bodies.at(i).name;
            for (int j = 0; j < n; j++) {
                String b = Bodies.at(j).name;
                for (Aspects.Type t : Aspects.Type.values()) {
                    double wide = Aspects.effectiveOrb(a, b, t, false);
                    double tight = Aspects.effectiveOrb(a, b, t, true);

                    // The whole point: exactly half, for EVERY aspect, not just the ones
                    // whose cap happens not to bind.
                    near("effective orb halves for " + a + "/" + b + " " + t.label,
                        wide / 2.0, tight, 1e-12);

                    // And it is still the smaller of the two ceilings, so a body orb wider
                    // than the aspect allows cannot leak through the halving.
                    checks++;
                    if (tight > t.maxOrb / 2.0 + 1e-12) {
                        failures.add(a + "/" + b + " " + t.label + " halved to " + tight
                            + ", above the halved cap of " + (t.maxOrb / 2.0));
                    }
                }
            }
        }

        // The minor aspects, named. Their cap is 1.0 and it binds for every pair in the
        // registry, so before the fix these were the six that never moved.
        for (Aspects.Type t : Aspects.Type.values()) {
            if (t.maxOrb > 1.0) {
                continue;
            }
            near(t.label + " is 1 degree natally", 1.0,
                Aspects.effectiveOrb("Sun", "Moon", t, false), 1e-12);
            near(t.label + " is half a degree in synastry", 0.5,
                Aspects.effectiveOrb("Sun", "Moon", t, true), 1e-12);
        }

        // A boundary that would have been silent before, on the aspect this project has
        // already had two defects in. 150.75 is three quarters of a degree off exact:
        // inside the quincunx's 1.0 natal cap, outside its 0.5 synastry one.
        eq("a quincunx at 0.75 degrees is a natal aspect",
            Aspects.Type.QUINCUNX, Aspects.typeOf(150.75, "Sun", "Moon", false));
        eq("and is not a synastry aspect",
            null, Aspects.typeOf(150.75, "Sun", "Moon", true));
        eq("at 0.4 it is both",
            Aspects.Type.QUINCUNX, Aspects.typeOf(150.4, "Sun", "Moon", true));

        // typeOf and effectiveOrb are the same rule. They have to be - typeOf is written in
        // terms of it now, and this fails if anyone reintroduces a second Math.min.
        for (Aspects.Type t : Aspects.Type.values()) {
            for (boolean syn : new boolean[] {false, true}) {
                double orb = Aspects.effectiveOrb("Mars", "Vesta", t, syn);
                double at = t == Aspects.Type.CONJUNCTION ? orb : t.exactAngle + orb;
                eq("typeOf agrees with effectiveOrb at the edge, " + t.label
                        + (syn ? " synastry" : " natal"),
                    t, Aspects.typeOf(at, "Mars", "Vesta", syn));
            }
        }
    }

    // ------------------------------------------------------------------ Part C

    /**
     * Why a cross-chart node or angle contact is a finding rather than a definition.
     *
     * Within one chart, North Node opposite South Node is arithmetic - the wheel suppresses
     * that line and that grid cell because printing it states the definition. Across two
     * charts nothing forces it: A's North Node against B's South Node is a real contact
     * that happens to land where it lands. These checks prove the difference is real for
     * two actual charts, which is the fact that decides how the cross-chart loops should
     * treat those pairs.
     */
    private static void crossChartContacts() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        ChartFrame a = run(sw, 1980, 5, 15, 12.0);
        ChartFrame b = run(sw, 1990, 8, 20, 8.0);

        // Within-chart: the registry knows these three pairs are definitional.
        checks++;
        if (!Bodies.isOppositePair(Bodies.indexOf("north_node"), Bodies.indexOf("south_node"))) {
            failures.add("North Node / South Node is not registered as a definitional pair");
        }
        checks++;
        if (!Bodies.isOppositePair(Bodies.indexOf("ascendant"), Bodies.indexOf("descendant"))) {
            failures.add("Ascendant / Descendant is not registered as a definitional pair");
        }
        checks++;
        if (!Bodies.isOppositePair(Bodies.indexOf("mc"), Bodies.indexOf("ic"))) {
            failures.add("MC / IC is not registered as a definitional pair");
        }

        // Within-chart, the opposition is exact to the last decimal - that is what makes it
        // uninformative.
        near("A's nodes are exactly opposed", 180.0,
            Aspects.separation(a.body("North Node").lon, a.southNode), 1e-9);
        near("B's nodes are exactly opposed", 180.0,
            Aspects.separation(b.body("North Node").lon, b.southNode), 1e-9);
        near("A's asc and dsc are exactly opposed", 180.0, Aspects.separation(a.asc, a.dsc), 1e-9);

        // Across charts it is contingent. If these ever came out exact the two charts would
        // be the same chart, and the check has stopped testing anything.
        awayFrom("A's North Node against B's South Node is not definitional", 180.0,
            Aspects.separation(a.body("North Node").lon, b.southNode), 1e-6);
        awayFrom("B's North Node against A's South Node is not definitional", 180.0,
            Aspects.separation(b.body("North Node").lon, a.southNode), 1e-6);
        awayFrom("A's Ascendant against B's Descendant is not definitional", 180.0,
            Aspects.separation(a.asc, b.dsc), 1e-6);
        awayFrom("A's MC against B's IC is not definitional", 180.0,
            Aspects.separation(a.mc, b.ic), 1e-6);

        // And the same point in the two charts is not the same degree, which is the reason
        // the diagonal of the cross matrix carries a reading at all.
        int identical = 0;
        for (int i = 0; i < a.bodies.length; i++) {
            if (!a.bodies[i].ok || !b.bodies[i].ok) {
                continue;
            }
            if (Aspects.separation(a.bodies[i].lon, b.bodies[i].lon) < 1e-9) {
                identical++;
            }
        }
        checks++;
        if (identical > 0) {
            failures.add(identical + " bodies sit at an identical degree in both charts - "
                + "these are not two distinct charts");
        }
    }

    // ------------------------------------------------------------------ Part D

    /**
     * The mode the panel keys off.
     *
     * SkymapPanel decides whether to draw a second wheel with
     * {@code mode == TRANSIT || mode == SYNASTRY}, and titles the wheels and the grid off
     * SYNASTRY specifically. A rename or a reorder here is silent at compile time in the
     * places that compare labels, so the labels are asserted.
     */
    private static void modeWiring() {
        eq("five chart modes", 5, ChartMode.values().length);
        eq("SYNASTRY label", "Synastry", ChartMode.SYNASTRY.label);
        eq("TRANSIT label", "Natal & Transit", ChartMode.TRANSIT.label);
        eq("SINGLE label", "Single Chart", ChartMode.SINGLE.label);
        eq("midpoint composite label", "Composite (Midpoint)", ChartMode.COMPOSITE_MIDPOINT.label);
        eq("davison composite label", "Composite (Davison)", ChartMode.COMPOSITE_DAVISON.label);
        checks++;
        if (ChartMode.SYNASTRY == ChartMode.TRANSIT) {
            failures.add("SYNASTRY and TRANSIT are the same constant");
        }
        // The panel renders a mode through toString(), so it must be the label and not the
        // constant name - "COMPOSITE_MIDPOINT" in a dropdown is a defect.
        eq("a mode renders as its label", "Synastry", ChartMode.SYNASTRY.toString());
    }

    // ------------------------------------------------------------------ Part E

    /**
     * A's bodies in B's houses.
     *
     * <b>The containment test here does not call Zodiac.houseOf.</b> It walks the host's
     * cusps with explicit arithmetic and asserts the longitude really is inside the arc the
     * returned house names. That is the difference between a consistency check and a real
     * one: if houseOf were wrong, a check that asked houseOf what the answer should be
     * would agree with it happily. Compare Part A's anyTypeWithin, which is honest about
     * being the consistency kind.
     *
     * <b>Both house systems, because an overlay inherits the host's.</b> Whole sign can
     * never produce an unequal house and would hide an arc-walking error that quadrant
     * cusps expose; Placidus at 34 degrees north is the app default and has houses of
     * genuinely different widths.
     */
    private static void houseOverlays() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        char[] systems = {'W', 'P'};
        int placed = 0;
        int angular = 0;
        int homeless = 0;

        for (char hsys : systems) {
            List<ChartFrame> corpus = corpus(sw, hsys);

            for (int c = 0; c + 1 < corpus.size(); c++) {
                ChartFrame a = corpus.get(c);
                ChartFrame b = corpus.get(c + 1);
                List<Synastry.Overlay> ab = Synastry.houseOverlays(a, b);

                // Completeness. Every body the frame computed is placed - a body that fell
                // out of this list would look on screen exactly like a body the ephemeris
                // failed to return, and the two must never be confusable.
                int ok = 0;
                for (ChartFrame.Body body : a.bodies) {
                    if (body != null && body.ok) ok++;
                }
                eq("every computed body is placed (" + hsys + ")", ok, ab.size());

                for (Synastry.Overlay o : ab) {
                    // The index and the name are the same registry entry.
                    eq("overlay index agrees with its name " + o.body,
                        Bodies.at(o.bodyIndex).name, o.body);

                    // The visitor is read, not moved.
                    near("overlay carries the visitor's own degree " + o.body,
                        a.bodies[o.bodyIndex].lon, o.lon, 0.0);

                    checks++;
                    if (o.house < 0 || o.house > 12) {
                        failures.add("overlay house out of range for " + o.body + ": " + o.house);
                    }

                    // The independent statement: the degree is inside the arc.
                    if (o.house >= 1 && o.house <= 12) {
                        placed++;
                        double start = Zodiac.normalise(b.cusps[o.house]);
                        double end = Zodiac.normalise(b.cusps[o.house == 12 ? 1 : o.house + 1]);
                        double span = Zodiac.normalise(end - start);
                        double into = Zodiac.normalise(o.lon - start);
                        checks++;
                        if (span <= 0.0 || into >= span) {
                            failures.add(o.body + " placed in house " + o.house + " of the host"
                                + " but sits " + into + " degrees into an arc of " + span);
                        }
                    } else {
                        homeless++;
                    }

                    eq("angular flag agrees with the house number " + o.body,
                        o.house == 1 || o.house == 4 || o.house == 7 || o.house == 10,
                        o.angularHouse);
                    if (o.angularHouse) {
                        angular++;
                    }
                }

                // Direction. A into B and B into A are different readings; if the house
                // numbers ever matched all the way down, the overlay would be reading one
                // chart twice.
                List<Synastry.Overlay> ba = Synastry.houseOverlays(b, a);
                int differing = 0;
                for (int i = 0; i < Math.min(ab.size(), ba.size()); i++) {
                    if (ab.get(i).house != ba.get(i).house) {
                        differing++;
                    }
                }
                checks++;
                if (differing == 0) {
                    failures.add("A-into-B and B-into-A gave identical houses for every body - "
                        + "the overlay is not direction-sensitive");
                }
            }

            // A chart laid over itself is its own natal house frame. This is the invariant
            // that ties the overlay to the reading the rest of the app already gives: if
            // these ever parted, the panel would say A's Sun is in B's 7th while the wheel
            // drew the same degree in the 6th for B's own chart.
            ChartFrame self = corpus.get(0);
            for (Synastry.Overlay o : Synastry.houseOverlays(self, self)) {
                eq("a chart over itself gives its own houses " + o.body + " (" + hsys + ")",
                    Zodiac.houseOf(o.lon, self.cusps), o.house);
            }
        }

        // Null in, empty out - the panel calls this before a second chart exists.
        eq("no visitor gives no overlays", 0, Synastry.houseOverlays(null, null).size());

        System.out.println("  bodies placed in a house: " + placed);
        System.out.println("  of those, in an angular house: " + angular
            + " (" + Math.round(1000.0 * angular / Math.max(1, placed)) / 10.0 + "%)");
        System.out.println("  bodies with no house (degenerate cusps): " + homeless);
    }

    // ------------------------------------------------------------------ Part F

    /**
     * A's bodies on B's angles.
     *
     * <b>Completeness is the check that matters.</b> Part A proves the same thing for the
     * grid: a contact reported must be inside orb, and a contact NOT reported must be
     * outside it. A feature that flags the loudest event in a synastry is worth nothing if
     * it can quietly miss one, and missing one is invisible on screen - the section simply
     * looks like a pairing that has no angle contacts.
     *
     * <b>These read at the halved synastry orb, which the grid on the same panel does
     * not.</b> Asserted here rather than assumed, because it is the first live use of the
     * halved contract Part B has been guarding since this suite was written.
     */
    private static void angleContacts() {
        SwissEph sw = new SwissEph(EPHE_PATH);
        List<ChartFrame> corpus = corpus(sw, 'P');
        int found = 0;
        int pairs = 0;
        int barren = 0;

        for (int c = 0; c + 1 < corpus.size(); c++) {
            ChartFrame a = corpus.get(c);
            ChartFrame b = corpus.get(c + 1);
            List<Synastry.AngleContact> hits = Synastry.angleContacts(a, b);
            pairs++;
            found += hits.size();
            if (hits.isEmpty()) {
                barren++;
            }

            double previous = -1.0;
            for (Synastry.AngleContact h : hits) {
                eq("contact index agrees with its name " + h.body,
                    Bodies.at(h.bodyIndex).name, h.body);

                // The orb reported is the separation, not something recomputed loosely.
                near("orb is the separation " + h.body + "/" + h.angle,
                    Aspects.separation(h.bodyLon, h.angleLon), h.orb, 1e-12);
                checks++;
                if (h.orb > h.maxOrb) {
                    failures.add(h.body + " on " + h.angle + " reported at " + h.orb
                        + ", outside the " + h.maxOrb + " it was judged on");
                }

                // The width really is the halved one.
                near("the contact orb is the halved synastry orb " + h.body + "/" + h.angle,
                    Aspects.orbFor(h.body, h.angle) / 2.0, h.maxOrb, 1e-12);

                // The angle degree is the host's, and it is the degree the wheel draws.
                near("the angle degree is the host's " + h.angle,
                    Synastry.angleLon(b, h.angle), h.angleLon, 0.0);

                // Sorted tightest first.
                checks++;
                if (h.orb < previous - 1e-12) {
                    failures.add("angle contacts are out of order: " + h.orb
                        + " came after " + previous);
                }
                previous = h.orb;

                // The suppressed restatements never appear.
                checks++;
                if (Bodies.oppositeOf(h.bodyIndex) >= 0) {
                    failures.add(h.body + " was offered as a visiting point; it is defined as "
                        + Bodies.at(Bodies.oppositeOf(h.bodyIndex)).name + "\'s opposition, "
                        + "which already states the same contact");
                }
            }

            // <b>The suppression is information-preserving, and this is the check that
            // says so.</b> Skipping the South Node, Descendant and IC on the visiting side
            // is only safe if their counterpart really does state the same contact against
            // the opposite host angle at the same orb. Asserted as an identity between two
            // separations, so it holds for every chart rather than for the ones that happen
            // to be in orb - and it would fail loudly if a "definitional" pair were ever
            // registered that is not actually 180 degrees apart.
            for (int i = 0; i < a.bodies.length; i++) {
                ChartFrame.Body body = a.bodies[i];
                int counterpart = Bodies.oppositeOf(i);
                if (body == null || !body.ok || counterpart < 0) continue;
                ChartFrame.Body other = a.bodies[counterpart];
                if (other == null || !other.ok) continue;
                for (String angle : Synastry.ANGLES) {
                    near("suppressing " + body.name + " on " + angle + " loses nothing - "
                            + other.name + " on " + oppositeAngle(angle) + " says it",
                        Aspects.separation(body.lon, Synastry.angleLon(b, angle)),
                        Aspects.separation(other.lon, Synastry.angleLon(b, oppositeAngle(angle))),
                        1e-9);
                }
            }

            // Completeness, both ways round: inside orb implies reported, reported implies
            // inside orb. Walked over every body and all four angles rather than sampled.
            for (int i = 0; i < a.bodies.length; i++) {
                ChartFrame.Body body = a.bodies[i];
                if (body == null || !body.ok) continue;
                if (Bodies.oppositeOf(i) >= 0) continue;
                for (String angle : Synastry.ANGLES) {
                    double sep = Aspects.separation(body.lon, Synastry.angleLon(b, angle));
                    // <b>An angle is a calculated point, so a calculated body cannot reach
                    // it.</b> Burk: these points have no moieties of their own and so receive
                    // aspects without casting them. Being inside the orb is therefore not
                    // enough on its own to expect a contact - K5 made that the engine's rule
                    // and this is the suite catching up to it, one commit late.
                    boolean inside = sep <= Aspects.orbFor(body.name, angle, true)
                        && !Aspects.bothCalculated(body.name, angle);
                    boolean reported = false;
                    for (Synastry.AngleContact h : hits) {
                        if (h.body.equals(body.name) && h.angle.equals(angle)) {
                            reported = true;
                            break;
                        }
                    }
                    eq("reported exactly when inside orb: " + body.name + "/" + angle
                        + " at " + sep, inside, reported);
                }
            }
        }

        // The four angles resolve; anything else is NaN rather than a plausible zero.
        ChartFrame f = corpus.get(0);
        near("Ascendant resolves", f.asc, Synastry.angleLon(f, "Ascendant"), 0.0);
        near("Descendant resolves", f.dsc, Synastry.angleLon(f, "Descendant"), 0.0);
        near("MC resolves", f.mc, Synastry.angleLon(f, "MC"), 0.0);
        near("IC resolves", f.ic, Synastry.angleLon(f, "IC"), 0.0);
        checks++;
        if (!Double.isNaN(Synastry.angleLon(f, "Vertex"))) {
            failures.add("angleLon answered for a point that is not one of the four angles");
        }
        eq("no host gives no contacts", 0, Synastry.angleContacts(f, null).size());

        // Measured, not asserted: the rate is what decides whether this section earns its
        // place on the panel or is empty for everyone.
        System.out.println("  angle contacts over " + pairs + " ordered pairs: " + found
            + " (" + Math.round(100.0 * found / Math.max(1, pairs)) / 100.0 + " per pair)");
        System.out.println("  ordered pairs with no angle contact at all: " + barren
            + " of " + pairs);
    }

    /** The far end of the axis an angle names. */
    private static String oppositeAngle(String angle) {
        switch (angle) {
            case "Ascendant":  return "Descendant";
            case "Descendant": return "Ascendant";
            case "MC":         return "IC";
            case "IC":         return "MC";
            default:           return angle;
        }
    }

    /**
     * Twenty charts at one place, spread across seventy years.
     *
     * One place on purpose: two people who met usually share a rough latitude, and holding
     * it fixed means a difference between two frames is a difference in the sky rather than
     * in the geometry of the house system.
     */
    private static List<ChartFrame> corpus(SwissEph sw, char hsys) {
        List<ChartFrame> out = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int year = 1940 + i * 3;
            int month = 1 + (i * 7) % 12;
            int day = 1 + (i * 13) % 27;
            double hour = (i * 5) % 24 + 0.25;
            SweDate sd = new SweDate(year, month, day, hour);
            out.add(ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, hsys, false, 0.0));
        }
        return out;
    }

    // ------------------------------------------------------------------ Part G

    /**
     * The relationship voice: one entry for every factor a relationship chart can show.
     *
     * <b>A missing key here is silent and the failure mode is the whole reason this file
     * exists.</b> Every getter returns null when it has nothing, and the panel then renders
     * the natal prose alone - which is exactly the defect being fixed, reappearing for one
     * body instead of all of them. There is no visible difference between "this body has no
     * composite framing" and "composite framing is switched off", so the coverage has to be
     * asserted rather than eyeballed.
     *
     * <b>Driven from the registry and the aspect enum, never from a list written here.</b>
     * A new body or a twelfth aspect fails this the moment it is added, which is the only
     * arrangement that keeps a dataset in step with the code that indexes it.
     */
    private static void relationshipProse() {
        InterpretationService svc = InterpretationService.getInstance();

        int missing = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            String id = Bodies.at(i).id;
            String prose = svc.getCompositeBody(id);
            checks++;
            if (prose == null) {
                failures.add("no composite framing for " + id
                    + " - its reading will silently speak in the natal voice");
                missing++;
            } else {
                ok("composite framing for " + id + " opens with a bold lead",
                    prose.startsWith("<b>") && prose.contains("</b>"));
                ok("composite framing for " + id + " carries no raw quote",
                    !prose.contains("\""));
            }
        }

        for (int h = 1; h <= 12; h++) {
            ok("composite house " + h, svc.getCompositeHouse(h) != null);
            ok("overlay house " + h, svc.getOverlayHouse(h) != null);
        }
        for (String angle : Synastry.ANGLES) {
            ok("angle contact prose for " + angle,
                svc.getAngleContact(angle.toLowerCase()) != null);
        }
        for (Aspects.Type t : Aspects.Type.values()) {
            ok("synastry framing for " + t.label, svc.getSynastryAspect(t.label) != null);
        }

        // <b>Null, not a not-found string.</b> The callers test for null to decide whether to
        // render the frame at all; a getter that returned "Interpretation not found" would put
        // that sentence on screen inside a highlighted box.
        ok("an unknown body gives null", svc.getCompositeBody("not_a_body") == null);
        ok("a null body id gives null", svc.getCompositeBody(null) == null);
        ok("house 0 gives null", svc.getCompositeHouse(0) == null);
        ok("house 13 gives null", svc.getCompositeHouse(13) == null);
        ok("overlay house 0 gives null", svc.getOverlayHouse(0) == null);
        ok("an unknown angle gives null", svc.getAngleContact("vertex") == null);
        ok("an unknown aspect gives null", svc.getSynastryAspect("Quindecile") == null);

        // Case does not decide whether a reading appears. The panel hands these getters a
        // display name ("MC", "Square"), the file is keyed in lower case, and the two must
        // not have to agree by hand.
        ok("angle lookup is case-insensitive", svc.getAngleContact("MC") != null);
        ok("aspect lookup is case-insensitive", svc.getSynastryAspect("SQUARE") != null);

        // ---- the per-pair interaspect dataset (2026-08-24) ----
        //
        // <b>Partial by design, so coverage is reported and not asserted at 100%.</b> It
        // carries 15 bodies and 6 aspects; the asteroids, the five minor aspects, the
        // Descendant and the IC are not in it and fall back to the natal aspect reading,
        // which is a real reading of the same two energies. Demanding completeness here
        // would fail a dataset that is behaving exactly as intended.
        //
        // What IS asserted is the part that can break silently: that the section is still
        // wired at all. Drop the file from EXTRA_FILES, rename the section, or change the
        // key format, and every cross-chart cell quietly reverts to the natal voice with
        // nothing on screen looking wrong.
        ok("the interaspect section is loaded",
            svc.getSynastryInteraspect("Sun", "Moon", "Conjunction") != null);

        // <b>Order must not decide whether a reading EXISTS - but it must decide what the
        // reading SAYS.</b> This used to require the two directions to be equal, on the note
        // that "the dataset stores one entry per unordered pair". That was true of the 756
        // entries it was written against and stopped being true on 2026-08-31, when a batch
        // arrived carrying both directions written separately: all 8,120 mirrored pairs in it
        // differ, none is a copy of its opposite.
        //
        // Synastry is transactional - their Sun quincunx your Moon is a different statement
        // from their Moon quincunx your Sun, because one names the sender and the other names
        // the receiver. Requiring the two to match asserted that synastry is a reworded
        // composite. So coverage is still checked in both directions; equality is not, and the
        // count of pairs that genuinely differ is printed rather than judged.
        int asymmetric = 0;
        int covered = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            for (int j = 0; j < Bodies.count(); j++) {
                String ab = svc.getSynastryInteraspect(Bodies.at(i).name, Bodies.at(j).name, "Square");
                String ba = svc.getSynastryInteraspect(Bodies.at(j).name, Bodies.at(i).name, "Square");
                checks++;
                if (ab != null && ba == null) {
                    failures.add("a reading exists one way round and not the other for "
                        + Bodies.at(i).name + "/" + Bodies.at(j).name);
                }
                if (ab != null && ba != null && !ab.equals(ba)) {
                    asymmetric++;
                }
                if (ab != null) covered++;
            }
        }

        // The registry spells it MC; the dataset spells it Midheaven. indexOfName carries
        // that alias, and this is the only thing standing between the alias and 90 silently
        // unreachable entries.
        ok("the Midheaven alias reaches the dataset",
            svc.getSynastryInteraspect("MC", "Venus", "Trine") != null);

        // Null, so the caller can fall back. A not-found string would be printed as prose.
        ok("an uncovered pair gives null",
            svc.getSynastryInteraspect("Pholus", "Hygiea", "Trine") == null);
        // <b>This named a gap, not a rule.</b> Quintile prose was simply unwritten when
        // this was added; it arrived 2026-08-31 and the check went red on new coverage.
        // Quindecile is not on Aspects.Type at all, so no dataset can ever turn it green.
        ok("an aspect the engine does not have gives null",
            svc.getSynastryInteraspect("Sun", "Moon", "Quindecile") == null);
        ok("a covered minor aspect now resolves",
            svc.getSynastryInteraspect("Sun", "Moon", "Quintile") != null);
        ok("a null argument gives null", svc.getSynastryInteraspect(null, "Moon", "Square") == null);

        // ---- composite planet in composite house (2026-08-24) ----
        //
        // <b>Ten planets by twelve houses, and completeness IS asserted here</b> - unlike the
        // interaspect dataset, this one was written to be complete over a stated scope, so a
        // hole in it is a mistake rather than a design. The nineteen registry points outside
        // that scope fall back to the composite body framing, which covers all 29.
        String[] planets = {"Sun", "Moon", "Mercury", "Venus", "Mars",
                            "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto",
                            "Chiron", "North Node"};
        for (String planet : planets) {
            for (int h = 1; h <= 12; h++) {
                String prose = svc.getCompositePlanetHouse(planet, h);
                checks++;
                if (prose == null) {
                    failures.add("no composite reading for " + planet + " in house " + h);
                    continue;
                }
                ok("composite " + planet + "_" + h + " opens with a bold lead",
                    prose.startsWith("<b>") && prose.contains("</b>"));
            }
        }

        // The house number is an index into a twelve-house frame and nothing here should
        // accept anything else. Zodiac.houseOf returns 0 for a degenerate cusp set, which is
        // a real value this getter can be handed.
        ok("composite house 0 gives null", svc.getCompositePlanetHouse("Sun", 0) == null);
        ok("composite house 13 gives null", svc.getCompositePlanetHouse("Sun", 13) == null);
        ok("a null body gives null", svc.getCompositePlanetHouse(null, 5) == null);

        // Out of scope means null, so the panel falls back to the body framing rather than
        // printing a not-found string inside the highlighted box.
        //
        // <b>Re-pointed 2026-08-30, when the coverage boundary moved.</b> These probes used
        // Vesta and the South Node, which had no planet-in-house prose at the time. They have
        // it now - composite_placements_b.json filled in all thirteen remaining non-angle
        // points - so those probes were asserting a gap that had been closed, and failed. The
        // assertion being made has not changed: absent prose must return null so the fallback
        // runs. Only the vehicle changed, to something still genuinely absent.
        //
        // The four ANGLES are that vehicle now. They carry body framing but have no
        // planet-in-house reading, which is correct - "Ascendant in the 5th" is not a thing
        // the technique says, because the Ascendant IS the first house cusp.
        ok("an out-of-scope body gives null",
            svc.getCompositePlanetHouse("Ascendant", 5) == null);
        ok("an out-of-scope body still has framing to fall back to",
            svc.getCompositePlanetHouse("Descendant", 5) == null
                && svc.getCompositeBody("descendant") != null);
        // The North/South Node asymmetry this used to pin is closed: both now have house
        // prose. Asserted so that a data loss on one side is a failure and not a silence.
        ok("both nodes now have composite house prose",
            svc.getCompositePlanetHouse("North Node", 5) != null
                && svc.getCompositePlanetHouse("South Node", 5) != null);
        int fallback = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            if (svc.getCompositePlanetHouse(Bodies.at(i).name, 5) == null) {
                fallback++;
                // and every one of those MUST have the framing, or it gets the natal voice
                ok(Bodies.at(i).name + " has body framing to fall back to",
                    svc.getCompositeBody(Bodies.at(i).id) != null);
            }
        }
        // <b>The size of the fallback set, pinned.</b> It is the four angles and nothing else
        // since 2026-08-30. Stating the number means a data drop that covers the angles, or a
        // regression that loses prose for a body that has it, both show up here instead of
        // quietly changing how much of a reading is specific and how much is generic.
        eq("the fallback set is exactly the four angles", 4, fallback);

        // ---- composite aspect frames (2026-08-24) ----
        for (Aspects.Type t : Aspects.Type.values()) {
            ok("composite aspect frame for " + t.label,
                svc.getCompositeAspectFrame(t.label) != null);
        }

        // <b>composite_aspect is a prefix of composite_aspect_frame, and the loader matches
        // section headers with startsWith.</b> It survives only because sectionFor includes
        // the closing quote in the literal, so the character after the prefix is a quote in
        // one and an underscore in the other. Drop that quote - an easy tidy-up - and all
        // eleven frames land in the pair map, where nothing reads them and nothing complains.
        ok("the frame section did not leak into the pair section",
            svc.getCompositeAspect("Conjunction", "Conjunction", "Conjunction") == null);

        // <b>This assertion used to read "the composite pair section is still empty", and it
        // did its job.</b> The file landed hours later and the suite went red immediately,
        // which is the point of asserting an empty state rather than assuming one: a section
        // cannot quietly acquire data, and a half-loaded file cannot sit here looking like
        // the empty case. What replaces it is completeness over the scope the file states.
        //
        // 14 bodies, every unordered pair, the five Ptolemaic aspects: 91 x 5 = 455.
        String[] cov = {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn",
                        "Uranus", "Neptune", "Pluto", "Chiron", "North Node",
                        "Ascendant", "MC"};
        String[] ptol = {"Conjunction", "Sextile", "Square", "Trine", "Opposition"};
        int pairs = 0;
        for (int i = 0; i < cov.length; i++) {
            for (int j = i + 1; j < cov.length; j++) {
                for (String t : ptol) {
                    String fwd = svc.getCompositeAspect(cov[i], cov[j], t);
                    checks++;
                    if (fwd == null) {
                        failures.add("no composite aspect for " + cov[i] + " " + t + " " + cov[j]);
                        continue;
                    }
                    pairs++;
                    // Order must not decide whether a reading exists. Unlike synastry there
                    // is no second person for the order to mean anything about, so the two
                    // must be the same entry rather than merely both present.
                    eq("composite aspect is order-independent " + cov[i] + "/" + cov[j] + " " + t,
                        fwd, svc.getCompositeAspect(cov[j], cov[i], t));
                    ok("composite aspect opens with a bold lead " + cov[i] + "/" + cov[j],
                        fwd.startsWith("<b>") && fwd.contains("</b>"));
                }
            }
        }

        // The Midheaven alias again - the file spells it Midheaven, the registry spells it MC,
        // and 65 entries reach the app only because indexOfName carries the alias.
        ok("the Midheaven alias reaches the composite aspects",
            svc.getCompositeAspect("MC", "Venus", "Trine") != null);

        // Out of scope means null so the natal aspect reading is used instead.
        //
        // <b>Re-pointed 2026-08-30.</b> This probed Sun/Moon quintile, which had no composite
        // prose until composite_minor_aspects.json added all six minors across the fourteen
        // covered bodies. An asteroid at a minor aspect is still absent on both axes, so it
        // tests the same fallback without asserting a gap that has been filled.
        // Same correction as above: unwritten is not out of scope.
        ok("an aspect the engine does not have gives no composite reading",
            svc.getCompositeAspect("Vesta", "Sun", "Quindecile") == null);
        // And the newly covered half, so the boundary is pinned from both sides.
        ok("a minor aspect between covered bodies now resolves",
            svc.getCompositeAspect("Sun", "Moon", "Quintile") != null);
        // <b>This asserted a gap in the data as if it were a rule about scope.</b> It read
        // getCompositeAspect("Vesta","Sun","Trine") == null and called that "an asteroid is
        // out of scope" - but Vesta was never out of scope, it was merely unwritten. David
        // supplied composite asteroid prose on 2026-08-31 and this went red, correctly
        // describing new coverage as a failure. The same shape as the 2026-08-23 red run:
        // the assertion was the thing that was wrong, not the engine.
        //
        // Now it tests the actual contract - <b>a body the registry does not know returns
        // null</b> - which no amount of new prose can turn green, because the lookup can
        // never resolve a name that is not a chart point.
        ok("an unknown body gives no composite aspect",
            svc.getCompositeAspect("Nibiru", "Sun", "Trine") == null);
        ok("a known body with prose now answers",
            svc.getCompositeAspect("Vesta", "Sun", "Trine") != null);
        ok("an unknown composite aspect gives null",
            svc.getCompositeAspectFrame("Quindecile") == null);
        // ---- the body click reads person B as a person (2026-08-31) ----
        //
        // <b>This suite ran 41,196 checks and none of them covered what a click produces.</b>
        // handleChartClick prefixes every outer-ring body with "transit_" whatever the mode,
        // and in SYNASTRY that ring is the second PERSON - so their Venus was read as a
        // transit: "Transiting Venus", transit-in-sign and transit-in-house prose, aspect rows
        // headed "to Natal Sun". The aspect GRID knew the difference and routed to
        // getSynastryInteraspect; the body click never did.
        //
        // The datasets that make the correct reading possible are asserted here, exhaustively,
        // because a missing overlay key is invisible at runtime - the getter returns null, the
        // panel falls back to the generic house paragraph, and the reading is merely blander.
        int overlayMissing = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            String body = Bodies.at(i).id;
            for (int h = 1; h <= 12; h++) {
                String prose = svc.getOverlayPlanetHouse(body, h);
                ok("overlay prose for " + body + " in house " + h, prose != null);
                if (prose == null) {
                    overlayMissing++;
                } else {
                    ok("overlay " + body + "_" + h + " opens with a bold lead",
                        prose.startsWith("<b>") && prose.contains("</b>"));
                    ok("overlay " + body + "_" + h + " is HTML, not markdown",
                        !prose.contains("**"));
                }
            }
        }
        int angleMissing = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            String body = Bodies.at(i).id;
            for (String ang : Synastry.ANGLES) {
                String prose = svc.getOverlayPlanetAngle(body, ang);
                ok("angle contact prose for " + body + " on " + ang, prose != null);
                if (prose == null) {
                    angleMissing++;
                } else {
                    ok("angle contact " + body + "_" + ang + " is HTML, not markdown",
                        !prose.contains("**"));
                }
            }
        }
        System.out.printf("  overlays missing %d, angle contacts missing %d%n",
            overlayMissing, angleMissing);

        // The specific reading must differ from the generic one, or the new dataset is
        // loading and changing nothing - which a null check alone would not catch.
        // <b>Null-safe on purpose.</b> Written first as a bare .equals() on the getter, which
        // throws when the entry is absent - so the mutation that made the getter return null
        // crashed this suite instead of failing it, and a crash reports nothing useful about
        // which assertion caught it. A check has to survive the defect it is testing for.
        String mars7 = svc.getOverlayPlanetHouse("mars", 7);
        String nep7 = svc.getOverlayPlanetHouse("neptune", 7);
        String generic7 = svc.getOverlayHouse(7);
        ok("a per-body overlay differs from the generic house paragraph",
            mars7 != null && generic7 != null && !mars7.equals(generic7));
        ok("two different bodies in the same house read differently",
            mars7 != null && nep7 != null && !mars7.equals(nep7));

        ok("composite frames are case-insensitive",
            svc.getCompositeAspectFrame("SQUARE") != null);

        // ---- tarot for the bodies the Golden Dawn never covered (2026-08-24) ----
        //
        // <b>Completeness over ASTEROID kind, asserted from the registry.</b> Every asteroid
        // and centaur must have a card from one section or the other, because the panel prints
        // the literal string Unknown when it has neither - nineteen of the twenty-nine
        // registry points were rendering that. Driven off Bodies.Kind rather than a list here,
        // so a new asteroid fails this the day it is registered.
        int tarotClassical = 0, tarotBody = 0, tarotNone = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def def = Bodies.at(i);
            String card = svc.getTarotPlanetCard(def.id);
            boolean hasClassical = card != null && !card.equals("Unknown");
            String entry = svc.getTarotBody(def.id);
            if (hasClassical) tarotClassical++;
            else if (entry != null) tarotBody++;
            else tarotNone++;

            if (def.kind == Bodies.Kind.ASTEROID) {
                checks++;
                if (!hasClassical && entry == null) {
                    failures.add("no tarot card for " + def.name
                        + " - the panel will print Unknown on screen");
                }
            }
            if (entry != null) {
                ok("tarot entry for " + def.id + " opens with the card name in bold",
                    entry.startsWith("<b>") && entry.contains("</b>"));
                // <b>Every one of these says how far to trust it.</b> The trailing italic is
                // the only thing separating an attribution named in a deck from one reasoned
                // out here, and this file is the reason that distinction exists at all.
                ok("tarot entry for " + def.id + " states its provenance",
                    entry.contains("Attested") || entry.contains("Inferred"));
            }
        }

        // The classical ten are not touched by any of this.
        eq("the Golden Dawn ten are intact", 10, tarotClassical);
        // No ok(..., true) here. The real assertion is the per-asteroid one inside the loop;
        // a summary line that cannot fail is worse than no line, because it reads like cover.

        System.out.println("  tarot: " + tarotClassical + " classical, " + tarotBody
            + " body cards, " + tarotNone + " still uncovered (nodes, angles, lots, Lilith)");
        System.out.println("  composite aspect pairs: " + pairs + " of 455");
        System.out.println("  composite planet-in-house: " + (planets.length * 12)
            + " of " + (planets.length * 12) + "; " + fallback
            + " registry points fall back to the body framing");
        System.out.println("  bodies with composite framing: "
            + (Bodies.count() - missing) + " of " + Bodies.count());
        System.out.println("  interaspect squares covered: " + covered
            + " ordered body pairs, " + asymmetric + " order-dependent");
    }

    // ------------------------------------------------------------------ Part H

    /**
     * The place where the zodiac joins itself.
     *
     * <b>Longitude is a circle stored as a number.</b> 359.5 and 1.0 are half a degree
     * apart in the sky and 358.5 apart in arithmetic, so every angular measurement in
     * this program has to fold the difference back onto the circle. {@link
     * Aspects#separation} does that, and it is the single implementation - Part A leans
     * on it, Part F leans on it, the grid and the wheel and the hit test all lean on it.
     *
     * <b>A second implementation was proposed on 2026-08-31</b> as a standalone test
     * class, along with the reasonable worry that a body at 29 Pisces and an angle at 1
     * Aries would be missed. The worry was worth having; the answer is that the seam
     * already holds. What was missing was not the code but the proof, and a proof kept in
     * a class outside the sweep would never have run. So it lives here.
     *
     * <b>Two things are asserted, and they are different things.</b> The first is that
     * the arithmetic is right, checked against a trigonometric oracle that shares no code
     * with it - {@code atan2(sin d, cos d)} folds the circle by a completely different
     * route, so the two agreeing is evidence rather than a tautology. The second, and the
     * one that would actually have caught a bug users could see, is that the <i>verdict</i>
     * does not change across the seam: the same separation must produce the same aspect
     * type whether it sits in the middle of the circle or straddles zero. That is the
     * property the panel depends on, and it is not implied by the arithmetic alone -
     * anything downstream that re-derived the difference itself would break it while
     * {@code separation} stayed correct.
     */
    private static void seam() {
        // The cases named in the 2026-08-31 proposal, kept verbatim so the record of what
        // was doubted survives alongside the answer.
        near("29 Aries to 1 Taurus is 2 degrees", 2.0, Aspects.separation(29.0, 31.0), 1e-12);
        near("29.5 Pisces to 1 Aries is 1.5 degrees", 1.5, Aspects.separation(359.5, 1.0), 1e-12);
        near("the same pair read backwards", 1.5, Aspects.separation(1.0, 359.5), 1e-12);
        near("a fifth of a degree across zero", 0.2, Aspects.separation(0.1, 359.9), 1e-12);
        near("a body against itself is zero", 0.0, Aspects.separation(12.3, 12.3), 1e-12);
        near("half the circle is 180", 180.0, Aspects.separation(0.0, 180.0), 1e-12);

        // Sweep the whole circle rather than the handful of places a hand-written case
        // would land on. The offsets bracket the seam, the two right angles and both sides
        // of the fold, because 180 is the other point where a folding bug hides.
        //
        // <b>179.5, 179.9, 180.1 and 180.5 are here because they were missing.</b> The
        // first draft of this sweep jumped from 179.0 straight to 180.0, and a mutant
        // that folded at 179 instead of 180 walked through Part H untouched - it was
        // caught two sections away, by luck rather than by design. A band a degree wide
        // is still a band, and this is the section that claims to own it.
        double[] offsets = {0.5, 2.0, 5.0, 89.5, 90.0, 179.0, 179.5, 179.9, 180.0,
                            180.1, 180.5, 181.0, 270.0, 359.5};
        for (int x = 0; x < 360; x++) {
            for (double off : offsets) {
                double a = x;
                double b = (x + off) % 360.0;
                double sep = Aspects.separation(a, b);

                // Independent oracle: same question, no shared arithmetic.
                double d = Math.toRadians(a - b);
                double oracle = Math.toDegrees(Math.abs(Math.atan2(Math.sin(d), Math.cos(d))));
                near("separation matches the trigonometric oracle at " + x + "+" + off,
                    oracle, sep, 1e-8);

                near("separation is symmetric at " + x + "+" + off,
                    sep, Aspects.separation(b, a), 1e-12);

                checks++;
                if (sep < 0.0 || sep > 180.0) {
                    failures.add("separation left the circle at " + x + "+" + off + ": " + sep);
                }
            }
        }

        // <b>The verdict must not move.</b> Same gap, once in open zodiac and once
        // straddling zero; the aspect engine has to call them the same thing. This is the
        // assertion that stands between a user and an angle contact that silently vanishes
        // because their partner's Venus sits at 29 Pisces.
        String[] bodies = {"Sun", "Moon", "Venus", "Mars", "Saturn"};
        String[] angles = {"Ascendant", "Descendant", "MC", "IC"};
        double[] gaps = {0.0, 0.1, 1.0, 2.0, 3.5, 5.0, 7.5, 12.0, 60.0, 90.0, 120.0, 180.0};
        for (String body : bodies) {
            for (String angle : angles) {
                for (double gap : gaps) {
                    // Middle of the circle, nowhere near the join.
                    Aspects.Type open = Aspects.typeOf(
                        Aspects.separation(100.0, 100.0 + gap), body, angle, true);
                    // The same gap laid across zero.
                    Aspects.Type across = Aspects.typeOf(
                        Aspects.separation(359.95, (359.95 + gap) % 360.0), body, angle, true);
                    eq("the seam does not change the verdict for " + body + "/" + angle
                        + " at " + gap + " degrees", open, across);

                    // And it does not change when the pair is read the other way round
                    // either, which is the order the two charts arrive in for the second
                    // person rather than the first.
                    Aspects.Type reversed = Aspects.typeOf(
                        Aspects.separation((359.95 + gap) % 360.0, 359.95), body, angle, true);
                    eq("argument order does not change the verdict for " + body + "/" + angle
                        + " at " + gap + " degrees", across, reversed);
                }

                // The halved synastry orb is Part B's contract; here it is only used to
                // place a probe safely inside and safely outside, so this stays a statement
                // about the seam rather than a second copy of the orb rule.
                double orb = Aspects.orbFor(body, angle, true);
                eq("well inside the orb is a conjunction across the seam for "
                    + body + "/" + angle,
                    Aspects.Type.CONJUNCTION,
                    Aspects.typeOf(Aspects.separation(359.9, (359.9 + orb * 0.5) % 360.0),
                        body, angle, true));
                checks++;
                if (Aspects.typeOf(Aspects.separation(359.9, (359.9 + orb + 5.0) % 360.0),
                        body, angle, true) == Aspects.Type.CONJUNCTION) {
                    failures.add("a conjunction was found " + (orb + 5.0)
                        + " degrees out across the seam for " + body + "/" + angle);
                }
            }
        }
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    // ------------------------------------------------------------------ plumbing

    private static ChartFrame run(SwissEph sw, int y, int m, int d, double hourUt) {
        SweDate sd = new SweDate(y, m, d, hourUt);
        return ChartFrame.compute(sw, sd.getJulDay(), LAT, LON, 'W', false, 0.0);
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null) {
            if (actual != null) {
                failures.add(label + ": got " + actual + ", expected null");
            }
        } else if (!expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected + " +/- " + tol);
        }
    }

    /** The opposite of {@link #near}: fails when a value IS the one named. */
    private static void awayFrom(String label, double forbidden, double actual, double tol) {
        checks++;
        if (Math.abs(forbidden - actual) <= tol) {
            failures.add(label + ": got " + actual + ", which is " + forbidden + " to within " + tol);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
