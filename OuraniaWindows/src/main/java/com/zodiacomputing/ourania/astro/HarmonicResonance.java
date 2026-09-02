package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The tightest harmonic contacts in a chart: <b>a ranking, deliberately not an alert.</b>
 *
 * <p>{@link Harmonics} answers "where does this body sit in H5". This answers the question a
 * reader actually has, which is "which harmonic is worth looking at at all". It returns the
 * {@link #K} tightest contacts across a declared dial, always the same number of them, sorted
 * by orb.
 *
 * <h2>Why a ranking and not an alert</h2>
 *
 * The obvious design is a badge that lights when two bodies fall within a degree of an exact
 * harmonic contact. <b>Measured before it was rejected</b>, over 200 charts and 300 pairs each
 * (25 bodies, angles excluded):
 *
 * <pre>
 *   orb 1.0 deg, harmonics 2..360 : 780 contacts per chart, 363 of them new at that harmonic
 *   orb 1.5 deg, the dial below   :  31 contacts per chart,  15 of them new
 *   orb 1.5 deg, H5 alone         : 2.58 per chart,        2.04 new
 * </pre>
 *
 * The rate is not a property of the sample. For N &gt;= 2 the harmonic separation of a pair is
 * uniform on the circle whatever the radix separation was, so <b>any</b> pair lands within orb
 * <i>o</i> of a conjunction with probability 2<i>o</i>/360, in <b>every</b> harmonic equally.
 * 300 pairs times 359 harmonics is a certainty, not a rarity. A badge on that footing lights on
 * every chart, hundreds of times, and says nothing - coverage without discrimination.
 *
 * A ranking is honest at any hit rate: "the five tightest, and here is each orb" stays true
 * whether the chart holds three near-exact contacts or eighty loose ones. The reader can see
 * from the orbs themselves whether the top of the list is remarkable.
 *
 * <h2>The dial is declared, and H360 is not on it</h2>
 *
 * {@link #DIAL} is an explicit list rather than 2..360, because the uniformity above means a
 * ranking over all 359 harmonics fills up with large arbitrary N - there are 350 of them above
 * H9 and each hits at the same rate - and an entry reading "H293" carries no reading anyone can
 * give. The dial holds the harmonics the app can actually say something about.
 *
 * <b>H360 is off the default dial for a measurable reason, not a stylistic one.</b> A contact
 * within orb <i>o</i> at HN means the radix separation sits within <i>o</i>/N of exact, so an
 * H360 contact at a 1 degree orb means the two radix positions agree to <b>10 arcseconds</b>.
 * The Moon covers 10 arcseconds in about 18 seconds of clock time and the Ascendant in about
 * 2.4 seconds. Birth times are recorded to the minute. An H360 contact involving the Moon or
 * anything angle-derived is therefore birth-time rounding, and it would take a tenth of the
 * five slots on merit it does not have. {@link #DIAL_WITH_H360} is provided for a surface that
 * has asked for exactly that and can tell the reader what it costs.
 *
 * <h2>What counts as a contact</h2>
 *
 * <b>Only contacts that are new at that harmonic.</b> HN conjunctions contain every contact
 * from every divisor of N: an opposition is a conjunction in H2, H4, H6 and H16 alike. Without
 * the filter, two thirds of what H4 reports is the reader's ordinary squares and oppositions
 * relabelled as something hidden - measured at 1.5 degrees, H4 gives 3.57 contacts per chart of
 * which only 1.27 are new, and H6 gives 3.67 of which 0.95 are new.
 *
 * The filter is exact rather than a second pass. A contact at HN means the radix separation is
 * near 360k/N for some whole k; that same pair is a conjunction in a divisor d of N exactly
 * when (N/d) divides k, so the lowest harmonic it appears in is N/gcd(k, N) and <b>the contact
 * is new at N precisely when gcd(k, N) = 1</b>. k = 0 is a radix conjunction, gcd(0, N) = N,
 * and it is correctly new nowhere. k = N/2 is an opposition and is new at H2 only.
 *
 * <h2>Ranked by orb and by nothing else</h2>
 *
 * Not by body weight, and that is a choice with a scar behind it. {@code Gestalt.WEIGHTS} was
 * once serving three jobs at once, so a weighting decision silently redefined what a stellium
 * was. A rank mixing orb with body importance would be one number meaning two things, and
 * neither could be adjusted without moving the other.
 *
 * <b>Importance is expressed as a body set instead, and {@link #classical} is the one that
 * ships</b> - {@link #scan(ChartFrame)} uses it. That keeps the two adjustable separately: the
 * rank answers "how exact is this", the set answers "whose contacts are worth ranking", and
 * moving one does not move the other. {@link #scan(ChartFrame, int[], double, int, boolean[])}
 * takes any other set, including null for every body the frame has.
 *
 * <h2>Pass the radix</h2>
 *
 * <b>This takes the chart as cast, never a frame that has already been through
 * {@link Harmonics#of}.</b> Handing it an H5 frame reports the harmonics of a harmonic, and
 * nothing about the output looks wrong - it is the same shape as the H5-renders-as-H25 defect
 * {@code HarmonicCheck} Part C exists to pin. In the panel that means {@code radixChart()}.
 */
public final class HarmonicResonance {

    private HarmonicResonance() { }

    /**
     * How many contacts are returned. Five, as David asked.
     *
     * A fixed count is the whole point: the list does not grow on a busy chart or vanish on a
     * quiet one, so its length is never mistaken for a finding.
     */
    public static final int K = 5;

    /**
     * The harmonics scanned by default - see the class note on why this is a list and not a
     * range. H2 through H9 are the classical divisions; H16 is the modulo-16 dial the Master
     * Chart Synthesis Checklist asks for, which exposes the semi-square and sesquiquadrate
     * family that no Ptolemaic aspect reports.
     */
    public static final int[] DIAL = {2, 3, 4, 5, 6, 7, 8, 9, 16};

    /** The dial with H360 added, for a surface that has asked for it. See the class note. */
    public static final int[] DIAL_WITH_H360 = {2, 3, 4, 5, 6, 7, 8, 9, 16, 360};

    /**
     * The widest orb a contact may have and still be reported, measured in the harmonic chart.
     *
     * <b>A truncation guard, not a statement about tightness - and the difference is the whole
     * reason this number is 6 and not 1.5.</b> The rank is what delivers tight contacts; the
     * ceiling only decides when the list is allowed to come up short.
     *
     * It was 1.5 while the scan ran over the whole 25-body registry, where 300 pairs yield 13
     * new contacts per chart and five slots always fill. <b>On {@link #classical} - twelve
     * bodies, 66 pairs - 1.5 degrees fills five slots on 21 charts in 150, and the fixed count
     * K stops being a promise.</b> Swept over 150 charts to find what does:
     *
     * <pre>
     *   ceiling   contacts/chart   filled 5 of 5   median 5th orb
     *      1.5             2.85        21 / 150             1.25
     *      3.0             5.27        97 / 150             1.97
     *      4.5             7.85       134 / 150             2.39
     *      6.0            10.31       146 / 150             2.46
     * </pre>
     *
     * <b>The last column is why widening this costs nothing.</b> At a 6 degree ceiling the
     * fifth entry a reader actually sees is typically 2.46 degrees, not 6 - the ceiling is off
     * the end of the distribution almost always, and raising it does not loosen the list, it
     * stops the list truncating. On the full registry it binds even less than before.
     */
    public static final double MAX_ORB = 6.0;

    /**
     * The default body set: the ten classical bodies and both nodes.
     *
     * <b>Because ranking by orb alone over all 25 registry bodies puts minor points and
     * generational pairs in the five.</b> On the 1982-08-10 sample the unrestricted five were
     * North Node/Vesta, Pluto/Juno, South Node/Part of Spirit, Neptune/Pluto and Uranus/Eris -
     * and Neptune/Pluto is a generational sextile true of everyone born for decades. Body
     * importance stays out of the <i>rank</i> for the reason argued in the class note; this is
     * the separate filter that note points at.
     *
     * A fresh array each call, so a caller narrowing it further cannot narrow it for everyone.
     */
    public static boolean[] classical() {
        boolean[] on = new boolean[Bodies.count()];
        for (String id : CLASSICAL_IDS) {
            int i = Bodies.indexOf(id);
            if (i >= 0) {
                on[i] = true;
            }
        }
        return on;
    }

    /** How many bodies {@link #classical} names, for a caller reporting on its own coverage. */
    public static int classicalCount() {
        return CLASSICAL_IDS.length;
    }

    private static final String[] CLASSICAL_IDS = {
        "sun", "moon", "mercury", "venus", "mars", "jupiter", "saturn",
        "uranus", "neptune", "pluto", "north_node", "south_node"};

    /** One harmonic contact between two bodies. Immutable; ordered by {@link #BY_ORB}. */
    public static final class Contact {

        /** The harmonic it is a conjunction in, and the lowest one - see the gcd note. */
        public final int harmonic;

        /** Registry indices, always with {@code indexA < indexB} so a pair reads one way. */
        public final int indexA;
        public final int indexB;

        /** Registry names, carried so a caller need not walk {@link Bodies} again. */
        public final String nameA;
        public final String nameB;

        /** The separation in the harmonic chart: how far off exact the contact is. */
        public final double orb;

        /** The separation in the chart as cast, 0 to 180 degrees. */
        public final double radixSeparation;

        /** Which multiple of 360/N the radix separation sits on. Coprime to {@link #harmonic}. */
        public final int multiple;

        Contact(int harmonic, int indexA, int indexB, double orb,
                double radixSeparation, int multiple) {
            this.harmonic = harmonic;
            this.indexA = indexA;
            this.indexB = indexB;
            this.nameA = Bodies.at(indexA).name;
            this.nameB = Bodies.at(indexB).name;
            this.orb = orb;
            this.radixSeparation = radixSeparation;
            this.multiple = multiple;
        }

        /** The exact radix angle this contact is a near miss of: 360 * k / N. */
        public double exactAngle() {
            return 360.0 * multiple / harmonic;
        }

        @Override
        public String toString() {
            return "H" + harmonic + " " + nameA + "-" + nameB
                + " orb " + String.format("%.4f", Double.valueOf(orb))
                + " (radix " + String.format("%.4f", Double.valueOf(radixSeparation))
                + " vs exact " + String.format("%.4f", Double.valueOf(exactAngle())) + ")";
        }
    }

    /**
     * Tightest first, then lowest harmonic, then registry order.
     *
     * <b>Fully ordered on purpose.</b> Two contacts can share an orb to the last bit - a pair
     * exactly opposite is 0.0 in every even harmonic - and a comparator that left those tied
     * would let the five returned depend on the iteration order of the scan, which is the sort
     * of instability a check suite cannot pin.
     */
    public static final Comparator<Contact> BY_ORB = new Comparator<Contact>() {
        @Override
        public int compare(Contact a, Contact b) {
            int c = Double.compare(a.orb, b.orb);
            if (c != 0) {
                return c;
            }
            c = Integer.compare(a.harmonic, b.harmonic);
            if (c != 0) {
                return c;
            }
            c = Integer.compare(a.indexA, b.indexA);
            return c != 0 ? c : Integer.compare(a.indexB, b.indexB);
        }
    };

    /** The five tightest new harmonic contacts among the classical bodies, on the default dial. */
    public static List<Contact> scan(ChartFrame radix) {
        return scan(radix, DIAL, MAX_ORB, K, classical());
    }

    /**
     * The tightest {@code k} new contacts, or fewer when the chart has fewer inside
     * {@code maxOrb}.
     *
     * @param radix   the chart <b>as cast</b> - see the class note
     * @param dial    harmonics to scan; entries below 2 are ignored, since H1 is the radix and
     *                every pair is trivially a contact in it
     * @param maxOrb  the widest orb reported, measured in the harmonic chart
     * @param k       how many to return
     * @param include one flag per registry index, or null for every body the frame has; this
     *                is where a caller narrows the body set, not the ranking
     */
    public static List<Contact> scan(ChartFrame radix, int[] dial, double maxOrb, int k,
                                     boolean[] include) {
        List<Contact> all = all(radix, dial, maxOrb, include);
        return all.size() <= k ? all : new ArrayList<Contact>(all.subList(0, k));
    }

    /**
     * Every new contact inside {@code maxOrb}, sorted. The ranking is a {@code subList} of
     * this, so anything wanting the whole distribution - a measurement, a check suite - takes
     * it from here rather than re-deriving the scan with a larger k.
     */
    public static List<Contact> all(ChartFrame radix, int[] dial, double maxOrb,
                                    boolean[] include) {
        List<Contact> out = new ArrayList<Contact>();
        if (radix == null || dial == null || maxOrb <= 0.0) {
            return out;
        }

        // The bodies worth pairing, gathered once rather than re-tested inside three loops.
        int[] idx = new int[radix.bodies.length];
        double[] lon = new double[radix.bodies.length];
        boolean[] present = new boolean[Bodies.count()];
        int n = 0;
        for (int i = 0; i < radix.bodies.length && i < Bodies.count(); i++) {
            ChartFrame.Body b = radix.bodies[i];
            if (b == null || !b.ok) {
                continue;
            }
            if (include != null && (i >= include.length || !include[i])) {
                continue;
            }
            // <b>The angles are excluded, and this is not a preference.</b> Harmonics.of
            // leaves the angles at their radix longitudes, on the argument that a harmonic
            // longitude restates an angular relationship and does not name a place in the
            // sky. Pairing a mapped body against an unmapped angle would compare a position
            // in H5 against a position in H1 and call the difference an orb.
            if (Bodies.at(i).isAngle()) {
                continue;
            }
            idx[n] = i;
            lon[n] = Zodiac.normalise(b.lon);
            present[i] = true;
            n++;
        }

        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                // <b>North Node against South Node, permanently exactly opposite.</b> Left in,
                // it is a 0.0000 orb in H2 - new at H2 by the gcd rule, since k = 1 - so it
                // would hold rank 1 in every chart ever cast while being the definition of the
                // south node restated rather than a fact about anybody.
                if (Bodies.isOppositePair(idx[a], idx[b])) {
                    continue;
                }
                double sep = Aspects.separation(lon[a], lon[b]);
                boolean mirrored = mirrors(idx[a], present) || mirrors(idx[b], present);
                for (int d = 0; d < dial.length; d++) {
                    int h = dial[d];
                    if (h < 2 || h > Harmonics.MAX) {
                        continue;
                    }
                    // <b>At an even harmonic the South Node IS the North Node.</b> The south
                    // node is derived as north + 180, and map(lon + 180, N) = map(lon, N)
                    // whenever N is even, since 180N is then a whole number of circles. So
                    // every even-harmonic contact the south node makes carries the identical
                    // orb to the north node's own, against the same third body - measured, it
                    // spent a slot restating a contact already on screen in 34 of 300 charts.
                    // Skipped only when the point it mirrors is in the scanned set too;
                    // scanning the south node alone must still report it.
                    if (mirrored && h % 2 == 0) {
                        continue;
                    }
                    double orb = Aspects.separation(
                        Harmonics.map(lon[a], h), Harmonics.map(lon[b], h));
                    if (orb > maxOrb) {
                        continue;
                    }
                    int k = (int) Math.round(sep * h / 360.0);
                    if (gcd(k, h) != 1) {
                        continue;
                    }
                    out.add(new Contact(h, idx[a], idx[b], orb, sep, k));
                }
            }
        }
        Collections.sort(out, BY_ORB);
        return out;
    }

    /**
     * True when this point is the exact opposite of another point that is also being scanned,
     * so at even harmonics it occupies that point's position exactly rather than one of its
     * own. {@link Bodies#oppositeOf} already names the registry's mirrored points - the south
     * node, and the two angles, which never reach here.
     */
    private static boolean mirrors(int index, boolean[] present) {
        int partner = Bodies.oppositeOf(index);
        return partner >= 0 && partner < present.length && present[partner];
    }

    /** Euclid, with gcd(0, n) = n - which is what makes a radix conjunction new nowhere. */
    static int gcd(int a, int b) {
        int x = Math.abs(a);
        int y = Math.abs(b);
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return x;
    }
}
