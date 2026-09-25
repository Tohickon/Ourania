package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * What a cast chart could not do, said in a sentence a reader can act on.
 *
 * <p><b>Two silent substitutions, and only two of the several the ephemeris makes.</b> Swiss
 * Ephemeris degrades quietly in more ways than this reports, and most of them do not matter
 * to anyone reading a chart. Measured on 2026-09-13 with David's ephemeris directory, which
 * lacks {@code semo_18.se1}: every planet's position still agrees with the full ephemeris to
 * about half an arcsecond, and the Moon falls back to Moshier, which is good to a few
 * arcseconds. A strip on every chart announcing that would be a strip nobody reads. So this
 * reports the substitutions that change what is on the wheel:
 *
 * <ul>
 *   <li><b>A body that cannot be placed at all.</b> Moshier has no asteroids. With the
 *       directory wrong or {@code seas_18.se1} gone, Chiron, Ceres, Pallas, Juno, Vesta,
 *       Pholus and the numbered asteroids return an error and simply are not drawn - measured
 *       by pointing the same code at a directory that does not exist. The reader sees a
 *       wheel with fewer bodies than they selected and no reason why.</li>
 *   <li><b>A house system with no solution at the birth latitude.</b> Placidus and Koch
 *       divide the time a degree takes to rise, and inside the polar circles some degrees
 *       never rise. {@code swe_houses} returns -1 and hands back Porphyry cusps instead -
 *       measured to agree with a Porphyry cast to 0.000000 degrees at 66.56, 67, 70, -67 and
 *       89.9, and to return 0 with genuinely different cusps at 66.0 and 66.5. The boundary
 *       is 90 degrees less the obliquity, so it moves slightly with the date.</li>
 * </ul>
 *
 * <p>Pure: no Swing, no settings. The caller says which bodies were asked for and which house
 * system, because whether a missing body matters depends on whether anyone wanted it.
 */
public final class Precision {

    private Precision() { }

    /**
     * True when the house system has no solution here and Swiss Ephemeris substituted
     * Porphyry.
     */
    public static boolean housesFellBack(SwissEph sw, double jdUt, double lat, double lon,
                                         int hsys) {
        double[] cusps = new double[13];
        double[] ascmc = new double[10];
        return sw.swe_houses(jdUt, 0, lat, lon, hsys, cusps, ascmc) < 0;
    }

    /**
     * The name the settings screen uses for a house system code.
     *
     * <b>Kept as the one line it is now.</b> This was a switch of its own, and it was the only
     * one of the four copies that knew Porphyry - which is how the system this class substitutes
     * above the polar circle became one a reader could not choose. See {@link HouseSystems}.
     */
    public static String houseSystemName(int hsys) {
        return HouseSystems.nameFor(hsys);
    }

    /** The sentence for a house system that fell back, naming whose chart and where. */
    public static String housesNote(String label, double lat, int hsys) {
        String hemi = lat >= 0 ? "N" : "S";
        return label + ": " + houseSystemName(hsys) + " houses have no solution at "
            + String.format("%.1f", Math.abs(lat)) + "°" + hemi
            + ", where some degrees never rise. The houses shown are Porphyry.";
    }

    /** A body that was asked for and could not be placed, and why. */
    public static final class Missing {
        public final String name;
        /** True when the reason is a data file the ephemeris directory does not hold. */
        public final boolean fileMissing;

        Missing(String name, boolean fileMissing) {
            this.name = name;
            this.fileMissing = fileMissing;
        }
    }

    /**
     * The selected ephemeris bodies Swiss Ephemeris cannot place at this moment.
     *
     * @param selected one flag per {@link Bodies} index; a body nobody asked for is not
     *                 reported, because its absence is what the reader chose
     */
    public static List<Missing> unplaceable(SwissEph sw, double jdUt, boolean[] selected) {
        List<Missing> out = new ArrayList<>();
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SPEED;
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if (d.source != Bodies.Source.EPHEMERIS) {
                continue;
            }
            if (selected != null && (i >= selected.length || !selected[i])) {
                continue;
            }
            double[] xx = new double[6];
            StringBuffer serr = new StringBuffer();
            if (sw.swe_calc_ut(jdUt, d.getIpl(), flags, xx, serr) < 0) {
                out.add(new Missing(d.name, serr.indexOf("not found") >= 0));
            }
        }
        return out;
    }

    /** The sentence for bodies that could not be placed, or null when every one was. */
    public static String bodiesNote(List<Missing> missing) {
        if (missing == null || missing.isEmpty()) {
            return null;
        }
        List<String> noFile = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (Missing m : missing) {
            (m.fileMissing ? noFile : other).add(m.name);
        }
        StringBuilder sb = new StringBuilder();
        if (!noFile.isEmpty()) {
            sb.append(join(noFile)).append(noFile.size() == 1 ? " is" : " are")
              .append(" not on the wheel: the ephemeris files for ")
              .append(noFile.size() == 1 ? "it" : "them")
              .append(Ephemeris.present() ? " are not in " : " would be in ")
              .append(Ephemeris.PATH)
              .append(Ephemeris.present() ? "." : ", which does not exist.");
        }
        if (!other.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(join(other)).append(other.size() == 1 ? " is" : " are")
              .append(" not on the wheel: the ephemeris cannot compute ")
              .append(other.size() == 1 ? "it" : "them").append(" for this date.");
        }
        return sb.toString();
    }

    private static String join(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1))
            + " and " + names.get(names.size() - 1);
    }
}
