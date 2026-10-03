package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.HouseSystems;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.gui.Settings;

/**
 * The phone's settings (M9): house system, zodiac, the transit orb, each planet's natal orb and
 * which aspects are drawn.
 *
 * <p><b>The desktop's settings, not a second set.</b> Every value is read and written through
 * {@link Settings}, under the desktop's own keys, in the same {@code settings.properties} (in
 * the app's private storage here), and put into force with {@link Settings#applyToEngine} - the
 * list the desktop's window runs at startup. So a choice means on the phone exactly what it
 * means on the desktop, and a settings file carried across would carry the choices with it.
 *
 * <p><b>An orb left at its default is not written.</b> {@link Settings} keeps "absent means the
 * engine's own width" so a later change to the built-in table still reaches the reader; saving
 * every field of this screen would freeze today's widths into the file. Only a changed orb is
 * stored.
 *
 * <p>No Android in here, like {@link PhoneChart}: {@code PhoneSettingsTest} runs it on the JVM.
 */
final class PhoneSettings {

    private PhoneSettings() { }

    /** Everything the screen shows, read at once and saved at once. */
    static final class Values {
        String houseSystem;
        String zodiac;
        double transitOrb;
        /** The natal orb of each of the ten planets, in degrees. */
        final double[] orbs = new double[PhoneChart.PLANETS];
        /** Whether each {@link Aspects.Type}, by ordinal, is drawn. */
        boolean[] aspects;
        /**
         * Whether the synthesized reading carries its mechanics tier.
         *
         * <p>The same setting the desktop's Synthesize checkbox writes, so a reader who turned
         * the audit trail off there finds it off here. It matters more on a phone than on the
         * desktop: the tier is most of the reading's length, and the phone builds the whole
         * thing into one TextView in one narrow column.
         */
        boolean readingMechanics;
    }

    /** The house systems offered, by name - the desktop picker's list. */
    static String[] houseSystems() {
        return HouseSystems.names();
    }

    /** The zodiacs offered: tropical, then the sidereal ayanamsas. */
    static String[] zodiacs() {
        String[] out = new String[Ephemeris.ZODIACS.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = Ephemeris.ZODIACS[i][0];
        }
        return out;
    }

    /** The planet a row of orbs is for. */
    static String planet(int i) {
        return Bodies.at(i).name;
    }

    static Values read() {
        Values v = new Values();
        v.houseSystem = HouseSystems.nameFor(Settings.houseSystem());
        v.zodiac = Settings.zodiac();
        v.transitOrb = Settings.transitOrb();
        for (int i = 0; i < v.orbs.length; i++) {
            v.orbs[i] = Settings.bodyOrb(planet(i));
        }
        v.aspects = Settings.loadAspectSelection();
        v.readingMechanics = Settings.readingMechanics();
        return v;
    }

    /** Saves what changed and puts it in force; the chart on screen is cast again after. */
    static void save(Values v) {
        Settings.setHouseSystem(v.houseSystem);
        Settings.setZodiac(v.zodiac);
        Settings.setTransitOrb(v.transitOrb);
        for (int i = 0; i < v.orbs.length; i++) {
            double was = Settings.bodyOrb(planet(i));
            if (Math.abs(was - v.orbs[i]) > 1e-9) {
                Settings.setBodyOrb(planet(i), v.orbs[i]);
            }
        }
        Settings.saveAspectSelection(v.aspects);
        Settings.setReadingMechanics(v.readingMechanics);
        Settings.applyToEngine();
    }

    /**
     * Back to the defaults for everything on this screen: Placidus, tropical, the one-degree
     * transit orb, the engine's own natal orbs, every aspect drawn, the whole reading. Nothing else in the file -
     * the transit, synastry and composite orbs are the desktop's to set,
     * the chart book lives elsewhere, and the desktop's other choices are left alone.
     */
    static void reset() {
        Settings.setHouseSystem(HouseSystems.DEFAULT_NAME);
        Settings.setZodiac(Ephemeris.ZODIACS[0][0]);
        Settings.setTransitOrb(Transits.DEFAULT_ORB);
        Settings.resetBodyOrbs(Aspects.Profile.NATAL);          // the natal orbs this screen shows
        boolean[] all = new boolean[Aspects.Type.values().length];
        java.util.Arrays.fill(all, true);
        Settings.saveAspectSelection(all);
        // The whole reading, which is the desktop's default too - turning a section off unasked
        // is the app deciding what a reader may see.
        Settings.setReadingMechanics(true);
        Settings.applyToEngine();
    }
}
