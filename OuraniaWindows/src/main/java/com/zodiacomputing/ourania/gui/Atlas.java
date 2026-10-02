package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.DataFiles;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Every place a chart can be cast for, on disk, with no network.
 *
 * <b>The app could not cast a chart without the internet.</b> A birth place went to Nominatim
 * for coordinates and to a second service for its time zone, so a reader on a train, behind a
 * firewall, or simply rate-limited had a form that would not resolve - and a birth place is not
 * an optional field. It is also two round trips for a fact that has not changed since the town
 * was founded.
 *
 * 168,549 places of a thousand people or more, from GeoNames cities1000, trimmed by
 * {@code tools/build_atlas.py} to the eight columns a chart needs. Source and licence are in
 * the file's own header: <a href="https://download.geonames.org/export/dump/">GeoNames</a>,
 * CC BY 4.0.
 *
 * <b>Sorted on disk, so loading is a parse and nothing else.</b> The rows are in order of the
 * folded name, which means a prefix is a contiguous run and a lookup is a binary search. No
 * index is built at load; that decision is what keeps this affordable to load lazily, and it is
 * the same bargain the four large prose sets already make.
 *
 * @see Geocoder which asks this first and only then goes out to the network
 */
public final class Atlas {

    /** One place: what to call it, where it is, and what time it keeps. */
    public static final class Place {
        public final String name;
        public final String region;
        public final String country;
        public final double latitude;
        public final double longitude;
        public final String zoneId;
        public final int population;

        Place(String name, String region, String country, double latitude, double longitude,
                String zoneId, int population) {
            this.name = name;
            this.region = region;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
            this.zoneId = zoneId;
            this.population = population;
        }

        /**
         * How the place reads in a list: "Philadelphia, Pennsylvania, United States".
         *
         * The region is what separates the two Londons and the two Philadelphias, so it is
         * never dropped when there is one - a list of bare city names is a list a reader
         * cannot choose from.
         */
        public String label() {
            StringBuilder sb = new StringBuilder(this.name);
            if (!this.region.isEmpty()) {
                sb.append(", ").append(this.region);
            }
            if (!this.country.isEmpty()) {
                sb.append(", ").append(this.country);
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return label();
        }
    }

    private Atlas() { }

    /**
     * The atlas's own filename, so start-up can look for it without writing it down twice (J9).
     *
     * The gazetteer is not part of the interpretation corpus and is not in
     * {@link InterpretationService#everyFileName}, so without this the one file whose absence
     * makes every place search silently return nothing was the one file nothing checked for.
     */
    static final String FILE_NAME = "atlas.tsv.gz";

    // Parallel arrays rather than 168,549 objects: a Place is built only for the handful a
    // search actually returns. The strings that repeat - zones, countries, regions - are shared
    // rather than copied, which is most of the file's bulk.
    private static String[] folded;
    private static String[] names;
    private static String[] regions;
    private static String[] countries;
    private static String[] zones;
    private static double[] lats;
    private static double[] lons;
    private static int[] pops;
    private static boolean loaded;
    private static String failure;

    /** How many places the atlas holds, loading it if it has not been read yet. */
    public static synchronized int size() {
        load();
        return folded == null ? 0 : folded.length;
    }

    /** Why the atlas is empty, or null when it loaded. For a check to report, not to guess. */
    public static synchronized String failure() {
        load();
        return failure;
    }

    /**
     * The form a typed query is matched in: unaccented, lowercase, single-spaced.
     *
     * <b>The same fold the file was built with</b> - {@code build_atlas.py} writes column one
     * this way. If the two ever disagree, every accented place stops being findable and nothing
     * else breaks, which is the kind of silence this project has learned to distrust: AtlasCheck
     * asserts the round trip on rows taken from the file itself.
     */
    public static String fold(String s) {
        if (s == null) {
            return "";
        }
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKD);
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase(java.util.Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /**
     * The gazetteer's text, whether it arrives gzipped or not.
     *
     * <b>Because Android unpacks it.</b> The build that packs the phone's assets stores a
     * {@code .gz} file decompressed and without the suffix - measured in the first APK, 30 Sep
     * (M2): {@code atlas.tsv.gz} went in as a 14 MB {@code atlas.tsv}. The phone's data source
     * finds it under either name; this reads it either way, by its first two bytes rather than
     * by its name, since the name is exactly what the packager changed.
     */
    static java.io.InputStream plainOrGzipped(java.io.InputStream raw) throws java.io.IOException {
        java.io.BufferedInputStream in = new java.io.BufferedInputStream(raw);
        in.mark(2);
        int b1 = in.read();
        int b2 = in.read();
        in.reset();
        return b1 == 0x1f && b2 == 0x8b ? new GZIPInputStream(in) : in;
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        List<String[]> rows = new ArrayList<>(180000);
        Map<String, String> shared = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                plainOrGzipped(DataFiles.entry(FILE_NAME).open()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] col = line.split("\t", -1);
                if (col.length < 9) {
                    continue;
                }
                rows.add(col);
            }
        } catch (Exception e) {
            failure = e.getClass().getSimpleName() + ": " + e.getMessage();
            return;
        }
        int n = rows.size();
        folded = new String[n];
        names = new String[n];
        regions = new String[n];
        countries = new String[n];
        zones = new String[n];
        lats = new double[n];
        lons = new double[n];
        pops = new int[n];
        for (int i = 0; i < n; i++) {
            String[] col = rows.get(i);
            folded[i] = col[0];
            names[i] = col[1];
            try {
                lats[i] = Double.parseDouble(col[2]);
                lons[i] = Double.parseDouble(col[3]);
                pops[i] = Integer.parseInt(col[7]);
            } catch (NumberFormatException e) {
                lats[i] = 0.0;
                lons[i] = 0.0;
                pops[i] = 0;
            }
            countries[i] = share(shared, col[5]);
            regions[i] = share(shared, col[6]);
            zones[i] = share(shared, col[8]);
        }
    }

    private static String share(Map<String, String> pool, String s) {
        String had = pool.get(s);
        if (had != null) {
            return had;
        }
        pool.put(s, s);
        return s;
    }

    private static Place at(int i) {
        return new Place(names[i], regions[i], countries[i], lats[i], lons[i], zones[i],
            pops[i]);
    }

    /**
     * Places whose name begins with what has been typed, biggest first.
     *
     * <b>A qualifier after a comma narrows rather than searches.</b> "London, Ontario" is not a
     * place called "London, Ontario" - it is the London in Ontario, and a reader who types it
     * has already told us which of the two they mean. So the part before the comma is matched
     * as a name and the part after it filters the region and the country.
     *
     * Ranked by population because that is what a bare name means: someone typing "Paris" with
     * nothing after it means the French one, and the eleven other Parises are still reachable
     * by saying which.
     */
    public static synchronized List<Place> search(String query, int limit) {
        load();
        List<Place> out = new ArrayList<>();
        if (folded == null || query == null) {
            return out;
        }
        String q = query.trim();
        String qualifier = "";
        int comma = q.indexOf(',');
        if (comma >= 0) {
            qualifier = fold(q.substring(comma + 1));
            q = q.substring(0, comma);
        }
        String prefix = fold(q);
        if (prefix.isEmpty()) {
            return out;
        }
        int from = lowerBound(prefix);
        List<Integer> hits = new ArrayList<>();
        for (int i = from; i < folded.length && folded[i].startsWith(prefix); i++) {
            if (!qualifier.isEmpty()
                && !fold(regions[i]).startsWith(qualifier)
                && !fold(countries[i]).startsWith(qualifier)) {
                continue;
            }
            hits.add(i);
        }
        hits.sort((a, b) -> {
            // An exact name beats a longer one that merely starts the same way: someone typing
            // "York" means York, not Yorktown, however much bigger Yorktown might be.
            boolean ea = folded[a].equals(prefix);
            boolean eb = folded[b].equals(prefix);
            if (ea != eb) {
                return ea ? -1 : 1;
            }
            return Integer.compare(pops[b], pops[a]);
        });
        for (int i = 0; i < hits.size() && out.size() < limit; i++) {
            out.add(at(hits.get(i)));
        }
        return out;
    }

    /**
     * The known place closest to a coordinate pair, or null when the atlas is empty.
     *
     * <b>What a pair of coordinates is for: its time zone.</b> Latitude and longitude are
     * enough to cast houses and nothing else - the moment still has to be read in some zone,
     * and a bare pair carries none. The nearest town's zone is the answer, and it is right
     * everywhere except within a few kilometres of a zone boundary, where nothing short of a
     * boundary polygon would be.
     *
     * A linear scan of 168,549 rows, which is a few milliseconds and happens once per chart.
     * An index would be faster and would be one more thing that can disagree with the data.
     */
    public static synchronized Place nearest(double lat, double lon) {
        load();
        if (folded == null || folded.length == 0) {
            return null;
        }
        int best = -1;
        double bestD = Double.MAX_VALUE;
        // Longitude degrees are narrower away from the equator; without this the nearest place
        // to somewhere in Scandinavia is chosen as though a degree of longitude were 111km.
        double squash = Math.cos(Math.toRadians(lat));
        for (int i = 0; i < folded.length; i++) {
            double dy = lats[i] - lat;
            double dx = (lons[i] - lon) * squash;
            double d = dy * dy + dx * dx;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best < 0 ? null : at(best);
    }

    /**
     * Reads "39.95, -75.16" as a place, taking its zone from the nearest town the atlas knows.
     *
     * Returns null for anything that is not a pair of numbers, which is how a place name falls
     * through to the lookups below. The bounds are checked rather than assumed: "12, 2000" is
     * two numbers and is not a coordinate, and a chart cast at longitude 2000 fails somewhere
     * far less legible than here.
     *
     * <p>Moved here from {@code Geocoder} on 30 Sep so the phone's chart book reads a saved
     * place exactly as the desktop does (M6); Geocoder asks this first, as before.
     */
    public static Place fromCoordinates(String query) {
        String[] parts = query.split(",");
        if (parts.length != 2) {
            return null;
        }
        double lat;
        double lon;
        try {
            lat = Double.parseDouble(parts[0].trim());
            lon = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            return null;
        }
        Place near = nearest(lat, lon);
        String zone = near == null ? "UTC" : near.zoneId;
        String name = near == null ? String.format(java.util.Locale.ROOT, "%.4f, %.4f", lat, lon)
            : near.label();
        // The coordinates the reader gave, not the town's - a birth place is not its nearest
        // city centre, and the houses are cast from these.
        return new Place(name, "", "", lat, lon, zone, 0);
    }

    /** The one place a typed name most likely means, or null when the atlas has none. */
    public static Place resolve(String query) {
        List<Place> hits = search(query, 1);
        return hits.isEmpty() ? null : hits.get(0);
    }

    /** First index whose folded name is not less than the prefix. */
    private static int lowerBound(String prefix) {
        int lo = 0;
        int hi = folded.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (folded[mid].compareTo(prefix) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }
}
