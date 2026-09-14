package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed stars: where the traditional stars stand, and which chart points sit on them.
 *
 * <p><b>Master list D9: "No star catalogue, no conjunctions to stars, no parans. Swiss
 * Ephemeris supplies the data already."</b> It does, in {@code sefstars.txt}, which was never in
 * the ephemeris directory. It was copied there on 2026-09-14 from the Swiss Ephemeris
 * distribution already on this machine ({@code Downloads\Apps & Tools\sweph}); like the other
 * ephemeris files it is not in the repository.
 *
 * <p><b>The stars are the fifteen Behenian stars of the medieval tradition, the four royal
 * stars, and the other bright stars modern practice reads most</b> - thirty in all. The
 * catalogue holds over a thousand; a table of all of them would bury the few that carry a
 * tradition under hundreds that do not.
 *
 * <p><b>Each is asked for by its Bayer designation</b>, not its name: the catalogue lists two
 * different stars as "Menkar", and a name lookup takes whichever comes first.
 *
 * <p><b>Conjunction by longitude only.</b> Parans - a star rising or culminating as a planet
 * does - are the other half of fixed-star work and are not computed here.
 */
public final class FixedStars {

    private FixedStars() { }

    /**
     * How close a chart point must be to a star's longitude, in degrees. One degree, the figure
     * in common use for stars; uncalibrated, held as a field like every orb here.
     */
    public static double orb = 1.0;

    /** Display name, Bayer designation as the catalogue writes it, and why it is on the list. */
    public static final String[][] STARS = {
        {"Algol", "bePer", "Behenian"},
        {"Alcyone", "etTau", "Behenian (the Pleiades)"},
        {"Aldebaran", "alTau", "Behenian, royal"},
        {"Capella", "alAur", "Behenian"},
        {"Sirius", "alCMa", "Behenian"},
        {"Procyon", "alCMi", "Behenian"},
        {"Regulus", "alLeo", "Behenian, royal"},
        {"Alkaid", "etUMa", "Behenian"},
        {"Algorab", "deCrv", "Behenian"},
        {"Spica", "alVir", "Behenian"},
        {"Arcturus", "alBoo", "Behenian"},
        {"Alphecca", "alCrB", "Behenian"},
        {"Antares", "alSco", "Behenian, royal"},
        {"Vega", "alLyr", "Behenian"},
        {"Deneb Algedi", "deCap", "Behenian"},
        {"Fomalhaut", "alPsA", "royal"},
        {"Betelgeuse", "alOri", "bright"},
        {"Rigel", "beOri", "bright"},
        {"Castor", "alGem", "bright"},
        {"Pollux", "beGem", "bright"},
        {"Canopus", "alCar", "bright"},
        {"Achernar", "alEri", "bright"},
        {"Denebola", "beLeo", "bright"},
        {"Vindemiatrix", "epVir", "bright"},
        {"Zuben Elgenubi", "al-2Lib", "bright"},
        {"Zuben Eschamali", "beLib", "bright"},
        {"Altair", "alAql", "bright"},
        {"Deneb", "alCyg", "bright"},
        {"Scheat", "bePeg", "bright"},
        {"Markab", "alPeg", "bright"},
    };

    /** A star's place at the chart's moment, in the chart's zodiac. */
    public static final class Star {
        public final String name;
        public final String tradition;
        public final double longitude;
        public final double latitude;

        Star(String name, String tradition, double longitude, double latitude) {
            this.name = name;
            this.tradition = tradition;
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

    /** A chart point on a star. */
    public static final class Contact {
        public final String point;
        public final Star star;
        public final double off;

        Contact(String point, Star star, double off) {
            this.point = point;
            this.star = star;
            this.off = off;
        }
    }

    public static final class Result {
        public final List<Star> stars = new ArrayList<>();
        public final List<Contact> contacts = new ArrayList<>();
        /** Stars the catalogue could not place - all of them when the file is missing. */
        public final List<String> missing = new ArrayList<>();
    }

    /** One star at a moment, or null when the catalogue cannot place it. */
    public static Star at(SwissEph sw, String[] def, double jdUt) {
        double[] xx = new double[6];
        StringBuffer star = new StringBuffer("," + def[1]);
        StringBuffer err = new StringBuffer();
        int r = sw.swe_fixstar_ut(star, jdUt, Ephemeris.flags(sw, SweConst.SEFLG_SWIEPH), xx, err);
        if (r < 0) {
            return null;
        }
        return new Star(def[0], def[2], Zodiac.normalise(xx[0]), xx[1]);
    }

    /** The points a star contact is read for: the same set {@link Antiscia} mirrors. */
    static boolean covered(Bodies.Def d) {
        return Antiscia.covered(d);
    }

    public static Result of(SwissEph sw, ChartFrame f) {
        Result r = new Result();
        for (String[] def : STARS) {
            Star s = at(sw, def, f.julianDayUt);
            if (s == null) {
                r.missing.add(def[0]);
            } else {
                r.stars.add(s);
            }
        }
        r.stars.sort((a, b) -> Double.compare(a.longitude, b.longitude));
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b == null || !b.ok || !covered(Bodies.at(i))) {
                continue;
            }
            for (Star s : r.stars) {
                double off = Aspects.separation(b.lon, s.longitude);
                if (off <= orb) {
                    r.contacts.add(new Contact(b.name, s, off));
                }
            }
        }
        r.contacts.sort((a, b) -> Double.compare(a.off, b.off));
        return r;
    }
}
