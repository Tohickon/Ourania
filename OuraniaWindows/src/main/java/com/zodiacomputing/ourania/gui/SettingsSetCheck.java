package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Saved settings sets: what a set carries, and what it must never carry.
 *
 * <p><b>The assertions that matter here are the negative ones.</b> A set that round-trips the
 * configuration is the easy half and would pass on a naive implementation that copied the whole
 * settings file. The half that needs holding is that it does <i>not</i> copy the reader: the real
 * settings.properties holds {@code natal.date}, {@code natal.time}, {@code natal.location},
 * {@code home.location}, two default locations and {@code window.bounds}, and a button called
 * "save my settings" that swept those up would, on a restore months later, put an old birth time
 * back without a word. The chart would change and the settings screen would be the last place
 * anyone looked.
 *
 * <p>So the exclusion is asserted from both ends - nothing personal is written into a set, and
 * nothing personal is touched by restoring one - and once more against a set that has a personal
 * key in it already, because a file written by an older build or edited by hand must not be able
 * to reintroduce one through the back door.
 */
public final class SettingsSetCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** Keys a reader would be dismayed to have quietly restored. */
    private static final String[] PERSONAL = {
        "natal.date", "natal.time", "natal.location",
        "home.location", "default.base.location", "default.transit.location",
        "window.bounds",
    };

    /** Keys that are configuration, and belong in a set. */
    private static final String[] CONFIGURATION = {
        "aspects.enabled", "bodies.enabled", "chart.zodiac", "chart.palette",
        "node.variant", "lilith.variant", "transit.orb",
        "orb.body.moon", "orb.aspect.SEMISEXTILE", "default.house.system",
    };

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        partA();
        partB();
        partC();
        partD();

        report();
    }

    // ------------------------------------------------- A: what is personal, over the real keys

    private static void partA() {
        System.out.println("Part A - the rule about what belongs to the reader");

        for (String key : PERSONAL) {
            ok(key + " belongs to the reader", Settings.isPersonal(key));
        }
        for (String key : CONFIGURATION) {
            ok(key + " is configuration", !Settings.isPersonal(key));
        }
        ok("no key at all is not personal", !Settings.isPersonal(null));

        // <b>A name cannot walk out of the sets folder.</b> The reader types this.
        ok("a name with a path separator is made safe",
            !Settings.setFileName("../../evil").contains(".."));
        ok("and cannot carry a slash", !Settings.setFileName("a/b").contains("/"));
        ok("and cannot carry a backslash", !Settings.setFileName("a\\b").contains("\\"));
        ok("an empty name is refused", Settings.setFileName("   ").isEmpty());
        ok("and so is no name at all", Settings.setFileName(null).isEmpty());
    }

    // ------------------------------------------------------------- B: the configuration returns

    private static void partB() {
        System.out.println();
        System.out.println("Part B - a set puts the configuration back");

        Settings.set("chart.zodiac", "Tropical");
        Settings.setBodyOrb("Moon", 4.25);
        ok("the starting state is what was set",
            "Tropical".equals(Settings.get("chart.zodiac", ""))
                && Math.abs(Settings.bodyOrb("Moon") - 4.25) < 1e-9);

        ok("the set saved", Settings.saveSet("tight"));
        ok("and it is listed", Settings.savedSets().contains("tight"));

        Settings.set("chart.zodiac", "Sidereal");
        Settings.setBodyOrb("Moon", 12.0);
        ok("the configuration then moved",
            "Sidereal".equals(Settings.get("chart.zodiac", ""))
                && Math.abs(Settings.bodyOrb("Moon") - 12.0) < 1e-9);

        ok("the set restored", Settings.restoreSet("tight"));
        ok("a plain setting came back", "Tropical".equals(Settings.get("chart.zodiac", "")));
        ok("and an orb came back", Math.abs(Settings.bodyOrb("Moon") - 4.25) < 1e-9);

        // <b>And it reached the engine, not only the file.</b> Settings pushes into Aspects; a
        // restore that wrote the file and left the engine holding the old width would be read
        // as working right up until someone cast a chart.
        ok("and the engine is holding it, not just the file",
            Math.abs(com.zodiacomputing.ourania.astro.Aspects.orbFor("Moon", "Moon")
                - 4.25) < 1e-9);

        // <b>Replace, not merge.</b> A key set after the save must not survive the restore.
        Settings.set("chart.decanRing", "true");
        Settings.restoreSet("tight");
        ok("a setting made after the save does not survive the restore",
            Settings.get("chart.decanRing", "ABSENT").equals("ABSENT"));
    }

    // ---------------------------------------------------- C: the reader's own data is untouched

    private static void partC() {
        System.out.println();
        System.out.println("Part C - a set never carries the reader");

        Settings.set("natal.date", "1979-03-11");
        Settings.set("natal.time", "04:35");
        Settings.set("natal.location", "Philadelphia, PA");
        Settings.set("home.location", "Doylestown, PA");
        Settings.set("window.bounds", "10,10,1200,800");
        Settings.set("chart.zodiac", "Tropical");

        ok("saving with a chart in the file works", Settings.saveSet("mine"));

        // Read the file the save actually wrote, rather than asking the code that wrote it.
        Properties onDisk = new Properties();
        java.io.File f = new java.io.File(Settings.setsDir(), Settings.setFileName("mine"));
        ok("the set is a file on disk", f.isFile());
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            onDisk.load(fis);
        } catch (java.io.IOException ex) {
            fail("the set could not be read back: " + ex);
            return;
        }

        int personalFound = 0;
        for (String key : onDisk.stringPropertyNames()) {
            if (Settings.isPersonal(key)) {
                personalFound++;
                fail("the set carries a personal key: " + key);
            }
        }
        ok("the saved set carries nothing of the reader's", personalFound == 0);
        ok("and it does carry the configuration",
            "Tropical".equals(onDisk.getProperty("chart.zodiac")));

        // <b>Now the restore end of it.</b> The chart moves, a set is restored, and the chart
        // must be the one the reader last entered - not the one that was current when the set
        // was saved.
        Settings.set("natal.time", "17:02");
        Settings.set("natal.location", "Berlin, Germany");
        Settings.restoreSet("mine");
        ok("restoring leaves the birth time alone",
            "17:02".equals(Settings.get("natal.time", "")));
        ok("and the birth place alone",
            "Berlin, Germany".equals(Settings.get("natal.location", "")));
        ok("and the window where it is",
            "10,10,1200,800".equals(Settings.get("window.bounds", "")));

        // <b>A hand-edited set does not get in either.</b> This is the case the code guards
        // twice, so it is asserted against a file written deliberately badly.
        onDisk.setProperty("natal.time", "23:59");
        onDisk.setProperty("home.location", "Nowhere");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(f)) {
            onDisk.store(fos, "hand-edited, with a chart smuggled in");
        } catch (java.io.IOException ex) {
            fail("could not write the hand-edited set: " + ex);
            return;
        }
        Settings.restoreSet("mine");
        ok("a set with a birth time written into it cannot put one back",
            "17:02".equals(Settings.get("natal.time", "")));
        ok("nor a home place", !"Nowhere".equals(Settings.get("home.location", "")));
    }

    // ------------------------------------------------------------------- D: the ordinary edges

    private static void partD() {
        System.out.println();
        System.out.println("Part D - names, listing and forgetting");

        ok("a set that was never saved cannot be restored",
            !Settings.restoreSet("no such set"));
        ok("an empty name saves nothing", !Settings.saveSet("  "));
        ok("and restores nothing", !Settings.restoreSet(""));

        Settings.saveSet("alpha");
        Settings.saveSet("beta");
        List<String> all = Settings.savedSets();
        ok("both sets are listed", all.contains("alpha") && all.contains("beta"));
        ok("and the list is sorted", isSorted(all));

        ok("a set can be forgotten", Settings.deleteSet("alpha"));
        ok("and is gone from the list", !Settings.savedSets().contains("alpha"));
        ok("forgetting it twice says so", !Settings.deleteSet("alpha"));
        ok("but the others are still there", Settings.savedSets().contains("beta"));
    }

    private static boolean isSorted(List<String> names) {
        for (int i = 1; i < names.size(); i++) {
            if (String.CASE_INSENSITIVE_ORDER.compare(names.get(i - 1), names.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------------------- the harness

    private static void fail(String label) {
        checks++;
        failures.add(label);
        System.out.println("  FAIL " + label);
    }

    private static void report() {
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String s : failures) {
            System.out.println("  " + s);
        }
        System.exit(1);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + label);
    }
}
