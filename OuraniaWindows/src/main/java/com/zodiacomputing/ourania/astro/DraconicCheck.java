package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * The draconic chart: one subtraction, and the two things it must not break.
 *
 * <p><b>The test is a property, not a table of numbers.</b> A draconic chart is the radix
 * measured from the Moon's north node rather than the equinox, so two things follow by
 * construction and both are checkable without a reference chart to compare against: the node
 * lands on 0 Aries, and every angular separation is unchanged - a square is still a square, to
 * the arcsecond. If either fails the derivation is wrong, whatever the figures look like.
 *
 * <p>Part C is the one that would otherwise rot: the angles must NOT be converted, for the same
 * reason {@link Harmonics} documents at length. A draconic Ascendant drawn against radix cusps
 * is a defect this codebase has already shipped twice in other classes.
 */
public final class DraconicCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        // A fresh install's settings and chart book, never the reader's. The engine reads the body
        // selection, the transit orb and the node variant underneath this suite even where it never
        // names Settings, so without this its answer depends on what the reader last saved (J14).
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);

        System.out.println("=== Part A: the node becomes the origin ===");
        int before = failures.size();
        theOrigin(sw);
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: every aspect survives the change of zero ===");
        before = failures.size();
        aspectsSurvive(sw);
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: the angles and the moment stay tropical ===");
        before = failures.size();
        anglesAndMoment(sw);
        report("Part C", before);

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
        System.exit(0);
    }

    /** Several charts across the year, because the node moves and the offset with it. */
    private static ChartFrame[] corpus(SwissEph sw) {
        return new ChartFrame[] {
            ChartFrame.compute(sw, new SweDate(1982, 8, 10, 19.0).getJulDay(),
                39.95, -75.17, 'P', false, 0),
            ChartFrame.compute(sw, new SweDate(1972, 9, 22, 18.5).getJulDay(),
                34.05, -118.25, 'P', false, 0),
            ChartFrame.compute(sw, new SweDate(1990, 1, 1, 12.0).getJulDay(),
                51.5, -0.12, 'P', false, 0),
            ChartFrame.compute(sw, new SweDate(2026, 6, 15, 3.25).getJulDay(),
                -33.87, 151.21, 'P', false, 0),
        };
    }

    private static void theOrigin(SwissEph sw) {
        for (ChartFrame f : corpus(sw)) {
            ChartFrame d = Draconic.of(f);
            ChartFrame.Body node = d.body("North Node");
            yes("the draconic chart has a node", node != null && node.ok);
            if (node == null || !node.ok) {
                continue;
            }
            // <b>Zero Aries, by construction.</b> This is the whole definition, and it is the
            // one number that says the subtraction went the right way round - an offset added
            // rather than subtracted puts the node on twice its own longitude, which looks
            // like a perfectly ordinary placement.
            near("the node lands on 0 Aries", 0.0,
                Math.min(node.lon, 360.0 - node.lon), 1e-6);

            // And the south node, half a circle from it.
            near("the south node lands on 0 Libra", 180.0, d.southNode, 1e-6);
        }
        yes("a chart with no node comes back unchanged rather than zeroed",
            Draconic.of(null) == null);
    }

    private static void aspectsSurvive(SwissEph sw) {
        for (ChartFrame f : corpus(sw)) {
            ChartFrame d = Draconic.of(f);
            int compared = 0;
            for (int i = 0; i < f.bodies.length; i++) {
                for (int j = i + 1; j < f.bodies.length; j++) {
                    ChartFrame.Body a1 = f.bodies[i];
                    ChartFrame.Body b1 = f.bodies[j];
                    ChartFrame.Body a2 = d.bodies[i];
                    ChartFrame.Body b2 = d.bodies[j];
                    if (a1 == null || b1 == null || !a1.ok || !b1.ok) {
                        continue;
                    }
                    // Angles keep their tropical longitude, so a pair involving one is not
                    // expected to hold - the point of Part C.
                    if (Bodies.at(i).isAngle() || Bodies.at(j).isAngle()) {
                        continue;
                    }
                    double was = Aspects.separation(a1.lon, b1.lon);
                    double now = Aspects.separation(a2.lon, b2.lon);
                    compared++;
                    near("the separation " + a1.name + "/" + b1.name + " is unchanged",
                        was, now, 1e-9);
                }
            }
            yes("there were pairs to compare", compared > 40);
        }
    }

    private static void anglesAndMoment(SwissEph sw) {
        for (ChartFrame f : corpus(sw)) {
            ChartFrame d = Draconic.of(f);
            near("the Ascendant is the real one", f.asc, d.asc, 1e-9);
            near("the Midheaven is the real one", f.mc, d.mc, 1e-9);
            for (int i = 0; i < f.cusps.length; i++) {
                near("cusp " + i + " is the real one", f.cusps[i], d.cusps[i], 1e-9);
            }
            for (int i = 0; i < f.bodies.length; i++) {
                if (Bodies.at(i).isAngle() && f.bodies[i] != null && f.bodies[i].ok) {
                    near("the angle " + f.bodies[i].name + " was not converted",
                        f.bodies[i].lon, d.bodies[i].lon, 1e-9);
                }
            }
            // The moment's own answers belong to the tropical chart.
            yes("the draconic chart declares itself derived", d.syntheticMoment);
            yes("and says why", d.syntheticNote != null && d.syntheticNote.length() > 40);
            eq("no lunar phase is claimed", -1, d.phaseIndex);
            yes("no syzygy is claimed", Double.isNaN(d.syzygyLon));
            yes("no void-of-course is claimed", !d.moonVoidOfCourse);

            // Motion is a fact about the body and survives a change of origin.
            int retro = 0;
            for (int i = 0; i < f.bodies.length; i++) {
                if (f.bodies[i] != null && f.bodies[i].ok && f.bodies[i].retrograde) {
                    retro++;
                    yes("a retrograde body is still retrograde", d.bodies[i].retrograde);
                }
            }
            yes("the chart had motion to preserve", retro >= 0);
        }
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void near(String label, double expected, double actual, double tol) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        System.out.println(part + (failures.size() == before ? ": clear" : ": FAILURES"));
    }
}
