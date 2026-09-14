package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * Antiscia and contra-antiscia: which bodies stand on another's mirror point.
 *
 * <p><b>Half-built for a month.</b> {@link Zodiac#antiscion} and {@link Zodiac#contraAntiscion}
 * have existed since the quincunx work, and {@link Quincunx} uses them at the level of whole
 * signs - but no degree was ever mirrored for a reader, and no contact was ever found. Master
 * list D8: "computed for quincunx classification; never shown as a ring or list."
 *
 * <p><b>A contact is a body on another body's antiscion</b> - its degree mirrored across 0
 * Cancer / 0 Capricorn, where the two share a declination and a length of day - or on its
 * contra-antiscion, mirrored across 0 Aries / 0 Libra. Traditional practice reads the first
 * like a conjunction and the second like an opposition. The relation is symmetric: A on B's
 * antiscion is B on A's, so each pair is reported once.
 *
 * <p><b>The points are the lights, the planets and, when the birth time is known, the
 * Ascendant and Midheaven</b> - the set the traditional authors mirror. The asteroids and
 * calculated points are left out for the same reason {@link Declinations} leaves them out.
 */
public final class Antiscia {

    private Antiscia() { }

    /**
     * How close a body must be to the mirror point, in degrees.
     *
     * One degree, uncalibrated, like every orb in this project - held as a field so a sourced
     * value has somewhere to land. Lilly allowed wider, by the moieties of the planets'
     * orbs; the tighter figure is the one modern traditional practice mostly uses.
     */
    public static double orb = 1.0;

    /** One point with its two mirror degrees. */
    public static final class Point {
        public final String name;
        public final double longitude;
        public final double antiscion;
        public final double contraAntiscion;

        Point(String name, double longitude) {
            this(name, longitude, 0.0);
        }

        /**
         * <b>The mirrors are tropical whatever zodiac the chart is in.</b> An antiscion is
         * defined by the solstices - equal declination either side of one - and the solstices
         * are at 0 Cancer and 0 Capricorn tropical. In a sidereal chart the degree is taken
         * back to tropical, mirrored, and brought forward again, so the pair still shares a
         * declination; mirroring about sidereal 0 Cancer would not.
         */
        Point(String name, double longitude, double ayanamsa) {
            this.name = name;
            this.longitude = longitude;
            this.antiscion = Zodiac.normalise(Zodiac.antiscion(longitude + ayanamsa) - ayanamsa);
            this.contraAntiscion = Zodiac.normalise(
                Zodiac.contraAntiscion(longitude + ayanamsa) - ayanamsa);
        }
    }

    /** A body on another's antiscion, or on its contra-antiscion. */
    public static final class Contact {
        public final String a;
        public final String b;
        /** False for antiscion, true for contra-antiscion. */
        public final boolean contra;
        /** Degrees from exact. */
        public final double off;

        Contact(String a, String b, boolean contra, double off) {
            this.a = a;
            this.b = b;
            this.contra = contra;
            this.off = off;
        }
    }

    public static final class Result {
        public final List<Point> points = new ArrayList<>();
        public final List<Contact> contacts = new ArrayList<>();
    }

    static boolean covered(Bodies.Def d) {
        return d.kind == Bodies.Kind.LUMINARY || d.kind == Bodies.Kind.PLANET
            || "Ascendant".equals(d.name) || "MC".equals(d.name);
    }

    public static Result of(ChartFrame f) {
        Result r = new Result();
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b == null || !b.ok || !covered(Bodies.at(i))) {
                continue;
            }
            r.points.add(new Point(b.name, b.lon, f.ayanamsa));
        }
        for (int i = 0; i < r.points.size(); i++) {
            for (int j = i + 1; j < r.points.size(); j++) {
                Contact c = contact(r.points.get(i), r.points.get(j));
                if (c != null) {
                    r.contacts.add(c);
                }
            }
        }
        r.contacts.sort((x, y) -> Double.compare(x.off, y.off));
        return r;
    }

    /**
     * The mirror contact between two points, or null.
     *
     * Unlike a declination parallel, a pair can never qualify both ways: the antiscion and
     * contra-antiscion of a degree are always exactly opposite, so their distances to any other
     * degree add to 180. The first draft carried a tie-break for the case anyway, and mutation
     * testing showed it was unreachable.
     */
    static Contact contact(Point x, Point y) {
        double anti = Aspects.separation(x.antiscion, y.longitude);
        double contra = Aspects.separation(x.contraAntiscion, y.longitude);
        if (anti <= orb) {
            return new Contact(x.name, y.name, false, anti);
        }
        if (contra <= orb) {
            return new Contact(x.name, y.name, true, contra);
        }
        return null;
    }
}
