package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * The offline atlas: that it is there, that it is right, and that nothing needs the network.
 *
 * <b>A data file the app cannot cast a chart without deserves its own suite.</b> Every other
 * check here can pass with the atlas missing entirely - the geocoder falls through to the
 * network and the chart still comes out - so its absence would show up as a slow form on a good
 * connection and a broken one on a bad one, which is the kind of failure this project has
 * learned only shows up on somebody else's machine.
 *
 * Nothing here touches the network. That is the point of the atlas and it is also the rule for
 * suites: one that needs the internet is a suite measuring the internet.
 */
public final class AtlasCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();

        System.out.println("=== Part A: the atlas is on disk and loads ===");
        int before = failures.size();
        itLoads();
        report("Part A", before);

        System.out.println();
        System.out.println("=== Part B: known places resolve to the right coordinates ===");
        before = failures.size();
        knownPlaces();
        report("Part B", before);

        System.out.println();
        System.out.println("=== Part C: a typed name folds to the form on disk ===");
        before = failures.size();
        theFold();
        report("Part C", before);

        System.out.println();
        System.out.println("=== Part D: the rows are sorted, which is what makes lookup work ===");
        before = failures.size();
        theOrder();
        report("Part D", before);

        System.out.println();
        System.out.println("=== Part E: a name that means two places says which ===");
        before = failures.size();
        theAmbiguous();
        report("Part E", before);

        System.out.println();
        System.out.println("=== Part F: every zone is one Java can actually use ===");
        before = failures.size();
        theZones();
        report("Part F", before);

        System.out.println();
        System.out.println("=== Part G: the geocoder answers without the network ===");
        before = failures.size();
        offline();
        report("Part G", before);

        System.out.println();
        System.out.println("=== Part I: a coordinate pair is already an answer ===");
        before = failures.size();
        theCoordinates();
        report("Part I", before);

        System.out.println();
        System.out.println("=== Part H: the form's location boxes offer the atlas ===");
        before = failures.size();
        theForm();
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

    /** The file is present, it parses, and it holds roughly what it should. */
    private static void itLoads() {
        long t0 = System.currentTimeMillis();
        int n = Atlas.size();
        long ms = System.currentTimeMillis() - t0;
        System.out.println("  " + n + " places, loaded in " + ms + " ms");
        ok("the atlas file loads without error", Atlas.failure() == null);
        if (Atlas.failure() != null) {
            System.out.println("  " + Atlas.failure());
        }
        // A floor rather than an exact count: the file is rebuilt from GeoNames, which grows
        // and prunes. A count that has to be edited every rebuild is a count nobody maintains.
        ok("it holds a substantial atlas rather than a stub", n > 150000);
    }

    /**
     * Places whose coordinates can be checked against something other than this file.
     *
     * <b>Loose tolerances on purpose.</b> A city is not a point, and GeoNames puts its marker
     * where its own sources do; asserting five decimal places would be asserting that GeoNames
     * never revises, which it does. A tenth of a degree is about eleven kilometres - tight
     * enough that the wrong city fails, loose enough that a revised marker does not.
     */
    private static void knownPlaces() {
        place("Philadelphia", 39.95, -75.16, "America/New_York");
        place("London", 51.51, -0.13, "Europe/London");
        place("Paris", 48.86, 2.35, "Europe/Paris");
        place("Tokyo", 35.69, 139.69, "Asia/Tokyo");
        place("Sydney", -33.87, 151.21, "Australia/Sydney");
        place("Reykjavik", 64.14, -21.90, "Atlantic/Reykjavik");
        place("Nairobi", -1.28, 36.82, "Africa/Nairobi");
        place("Kolkata", 22.57, 88.36, "Asia/Kolkata");
    }

    private static void place(String query, double lat, double lon, String zone) {
        Atlas.Place p = Atlas.resolve(query);
        ok(query + " is in the atlas", p != null);
        if (p == null) {
            return;
        }
        ok(query + " is at the right latitude", Math.abs(p.latitude - lat) < 0.1);
        ok(query + " is at the right longitude", Math.abs(p.longitude - lon) < 0.1);
        ok(query + " keeps " + zone, zone.equals(p.zoneId));
    }

    /**
     * The fold in the app matches the fold the file was built with.
     *
     * <b>One rule, two implementations, in two languages.</b> Column one of the atlas is
     * written by tools/build_atlas.py and searched by Atlas.fold, and if those ever disagree
     * about an accent then every accented place quietly stops being findable - the search
     * returns nothing and looks like a place that is not in the atlas. So the assertion is made
     * against the file's own rows rather than against a list written here: fold the name in
     * column two and it has to come back as column one.
     */
    private static void theFold() throws Exception {
        int rows = 0;
        int mismatched = 0;
        String worst = null;
        try (java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(
                    new java.io.FileInputStream(InterpretationService.DATA_DIR + "atlas.tsv.gz")),
                java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] col = line.split("\t", -1);
                if (col.length < 9) {
                    continue;
                }
                rows++;
                if (!Atlas.fold(col[1]).equals(col[0])) {
                    mismatched++;
                    if (worst == null) {
                        worst = col[1] + " folds to '" + Atlas.fold(col[1])
                            + "' but the file says '" + col[0] + "'";
                    }
                }
            }
        }
        System.out.println("  " + rows + " rows read; " + mismatched
            + " fold differently in Java than in the builder");
        if (worst != null) {
            System.out.println("  first: " + worst);
        }
        ok("the file was actually read", rows > 150000);
        ok("every name folds the same way in both languages", mismatched == 0);

        // And the fold does what it is for, which is to let a reader leave the accents off.
        ok("an unaccented query finds an accented place",
            Atlas.resolve("Zurich") != null
                && "Europe/Zurich".equals(Atlas.resolve("Zurich").zoneId));
        ok("Sao Paulo finds Sao Paulo",
            Atlas.resolve("Sao Paulo") != null
                && "America/Sao_Paulo".equals(Atlas.resolve("Sao Paulo").zoneId));
    }

    /**
     * The rows are in the order the binary search assumes.
     *
     * Sorting is done once by the builder and never at load, so if the file were out of order
     * the search would not fail loudly - it would return nothing for some prefixes and the
     * right answer for others, depending where the binary search happened to land.
     */
    private static void theOrder() throws Exception {
        String previous = "";
        int outOfOrder = 0;
        int rows = 0;
        try (java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(
                    new java.io.FileInputStream(InterpretationService.DATA_DIR + "atlas.tsv.gz")),
                java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                int tab = line.indexOf('\t');
                if (tab < 0) {
                    continue;
                }
                String folded = line.substring(0, tab);
                rows++;
                if (folded.compareTo(previous) < 0) {
                    outOfOrder++;
                }
                previous = folded;
            }
        }
        System.out.println("  " + rows + " rows, " + outOfOrder + " out of order");
        ok("the atlas is sorted by its folded name", outOfOrder == 0);
    }

    /** A name that means several places offers them, and offers the one meant first. */
    private static void theAmbiguous() {
        List<Atlas.Place> londons = Atlas.search("London", 5);
        ok("London finds more than one place", londons.size() > 1);
        ok("and offers the English one first",
            !londons.isEmpty() && "Europe/London".equals(londons.get(0).zoneId));
        ok("and each one says where it is",
            londons.stream().allMatch(p -> p.label().contains(",")));

        // The reader saying which is what the qualifier is for.
        Atlas.Place ontario = Atlas.resolve("London, Ontario");
        ok("London, Ontario finds the Canadian one",
            ontario != null && "America/Toronto".equals(ontario.zoneId));
        ok("and not the English one",
            ontario != null && Math.abs(ontario.longitude + 81.23) < 0.2);

        // <b>An exact name beats a bigger place that merely starts the same way.</b> Ranking on
        // population alone would answer "Yorktown" or "York Region" to someone typing York.
        Atlas.Place york = Atlas.resolve("York");
        ok("York is York", york != null && "York".equals(york.name));

        // A place small enough to be missing from a city-sized atlas, which is the reason
        // cities1000 was chosen over the larger thresholds.
        ok("a town of a few thousand is in the atlas",
            Atlas.resolve("Wagga Wagga") != null);
    }

    /**
     * Every zone in the file is one this JVM can load.
     *
     * A zone id that Java cannot parse would not fail here - it would fail later, when a chart
     * was cast for that town, as a stack trace with a birth time in it.
     */
    private static void theZones() {
        java.util.Set<String> zones = new java.util.HashSet<>();
        for (String query : new String[] {"a", "b", "c", "d", "e", "k", "m", "n", "p", "s",
                "t", "w", "y", "z"}) {
            for (Atlas.Place p : Atlas.search(query, 4000)) {
                zones.add(p.zoneId);
            }
        }
        int bad = 0;
        String first = null;
        for (String z : zones) {
            try {
                java.time.ZoneId.of(z);
            } catch (Exception e) {
                bad++;
                if (first == null) {
                    first = z;
                }
            }
        }
        System.out.println("  " + zones.size() + " distinct zones sampled, " + bad + " unusable");
        ok("the sample covers a real spread of zones", zones.size() > 100);
        ok("every zone in it is one Java can load", bad == 0);
        if (first != null) {
            System.out.println("  first bad zone: " + first);
        }
    }

    /**
     * The geocoder answers from the atlas, with nothing reachable.
     *
     * <b>Proved by taking the network away, not by trusting the order of two branches.</b> The
     * whole claim is "this works offline", and the only way to check that claim is to make the
     * network fail - so a SecurityManager-free way to do it: point the JVM's proxy at a dead
     * address, so any socket the fallback tries cannot connect. If the atlas branch is ever
     * removed or reordered below the network call, this stops answering.
     */
    private static void offline() {
        String host = System.getProperty("http.proxyHost");
        String port = System.getProperty("http.proxyPort");
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "9");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "9");
        try {
            long t0 = System.currentTimeMillis();
            Geocoder.Result r = Geocoder.lookup("Philadelphia");
            long ms = System.currentTimeMillis() - t0;
            System.out.println("  a lookup with the network dead took " + ms + " ms");
            ok("the geocoder still answers with no network", r != null);
            if (r != null) {
                ok("with the right coordinates", Math.abs(r.lat - 39.95) < 0.1
                    && Math.abs(r.lon + 75.16) < 0.1);
                ok("and the right zone", "America/New_York".equals(r.tzId));
                ok("and it answered from disk rather than waiting on a socket", ms < 1000);
            }
        } finally {
            restore("http.proxyHost", host);
            restore("http.proxyPort", port);
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    /**
     * The suggestions reach the three boxes a reader actually types into.
     *
     * <b>An engine with no door is the defect this project logs most.</b> Atlas.search can be
     * perfect and the form still be plain text, and every check above would pass - so this one
     * goes and finds the boxes on the built panel and asserts each has the listener attached.
     * Names rather than positions: a field moved is a field this still finds.
     */
    private static void theForm() throws Exception {
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fc = OuraniaWindow.class.getDeclaredField("chartSetupPanel");
            fc.setAccessible(true);
            ChartSetupPanel setup = (ChartSetupPanel) fc.get(hold[0]);
            Thread.sleep(1500);
            for (String name : new String[] {"baseLocationField", "transitLocationField",
                    "skyLocationField"}) {
                java.lang.reflect.Field f = ChartSetupPanel.class.getDeclaredField(name);
                f.setAccessible(true);
                javax.swing.JTextField box = (javax.swing.JTextField) f.get(setup);
                ok(name + " exists", box != null);
                if (box == null) {
                    continue;
                }
                // The typeahead listens to the document and to the keyboard; both have to be
                // there or the list either never opens or cannot be chosen from.
                int docListeners = ((javax.swing.text.AbstractDocument) box.getDocument())
                    .getDocumentListeners().length;
                ok(name + " offers suggestions as it is typed into", docListeners > 0);
                ok(name + " can be chosen from with the keyboard",
                    box.getKeyListeners().length > 0);
            }
            // And what it would offer is what the atlas holds.
            java.util.List<Atlas.Place> hits = PlaceField.suggestionsFor("Philadel");
            ok("typing a partial name offers places", !hits.isEmpty());
            ok("and the first is the one meant",
                !hits.isEmpty() && "America/New_York".equals(hits.get(0).zoneId));
            ok("and every offer names its region so two can be told apart",
                hits.stream().allMatch(pl -> pl.label().contains(",")));
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
    }

    /**
     * "39.95, -75.16" resolves without asking anyone.
     *
     * <b>This was the common path and it was going out to the network.</b> The typeahead writes
     * coordinates into the box when a reader picks a place - deliberately, so the name is not
     * geocoded a second time and cannot come back a different Springfield - and those
     * coordinates then missed the atlas by name and fell through to Nominatim, to be told what
     * they had just said. A chart could not be cast offline for exactly the places the atlas
     * knows best.
     *
     * The zone is the one thing a bare pair does not carry, and the nearest known town supplies
     * it. Checked here at three latitudes because the nearest-place search has to squash
     * longitude by the cosine of latitude - without that, the nearest town to somewhere far
     * north is chosen as though a degree of longitude were as wide as a degree of latitude.
     */
    private static void theCoordinates() {
        pair("39.95, -75.16", 39.95, -75.16, "America/New_York");
        pair("51.51, -0.13", 51.51, -0.13, "Europe/London");
        pair("-33.87, 151.21", -33.87, 151.21, "Australia/Sydney");
        pair("64.14, -21.90", 64.14, -21.90, "Atlantic/Reykjavik");
        pair("35.69,139.69", 35.69, 139.69, "Asia/Tokyo");

        // <b>The coordinates the reader gave, not the town's.</b> A birth place is not its
        // nearest city centre, and the houses are cast from these numbers.
        Geocoder.Result r = Geocoder.lookup("39.95, -75.16");
        ok("a pair keeps the exact coordinates it was given",
            r != null && r.lat == 39.95 && r.lon == -75.16);

        // Two numbers are not a coordinate pair. This has to fall through rather than cast a
        // chart at a longitude that does not exist.
        ok("a longitude out of range is not read as a coordinate",
            Geocoder.fromCoordinates("12, 2000") == null);
        ok("nor a latitude out of range",
            Geocoder.fromCoordinates("100, 20") == null);
        ok("and a place name is still a place name",
            Geocoder.fromCoordinates("Philadelphia") == null);
    }

    private static void pair(String typed, double lat, double lon, String zone) {
        long t0 = System.currentTimeMillis();
        Geocoder.Result r = Geocoder.lookup(typed);
        long ms = System.currentTimeMillis() - t0;
        ok(typed + " resolves", r != null);
        if (r == null) {
            return;
        }
        ok(typed + " keeps its latitude", Math.abs(r.lat - lat) < 0.0001);
        ok(typed + " keeps its longitude", Math.abs(r.lon - lon) < 0.0001);
        ok(typed + " takes the zone of the nearest town (" + zone + ")",
            zone.equals(r.tzId));
        // A network round trip cannot happen in this long; the atlas load is the only thing
        // here that costs anything, and it happens once.
        ok(typed + " answered from disk", ms < 2000);
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void report(String part, int before) {
        int n = failures.size() - before;
        System.out.println(part + (n == 0 ? ": clear" : ": " + n + " FAILURE(S)"));
    }
}
