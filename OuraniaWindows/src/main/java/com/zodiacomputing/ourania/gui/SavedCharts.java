package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Rodden;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * The saved chart book: named date/time/place triples, shared by both wheels.
 *
 * One store rather than one per side, deliberately. A saved chart is a <em>person</em> or a
 * moment, not a role - the same birth data is the natal wheel when you read that person and
 * the outer wheel when you compare them to someone else. Synastry and composite work is
 * exactly "load chart A into one side and chart B into the other", which a per-side store
 * makes awkward for no reason.
 *
 * Reads legacy {@code saved_transits.properties} once and carries its entries forward, so
 * charts saved before this existed are not stranded.
 */
public final class SavedCharts {

    private static final String FILE = "saved_charts.properties";
    private static final String LEGACY = "saved_transits.properties";

    /** One saved chart. Fields are raw form text, validated where they are used. */
    public static final class Entry {
        public final String date;
        public final String time;
        public final String location;
        /**
         * How well the birth time is known. Never null - see {@link Rodden#of}.
         *
         * <b>Stored with the chart, not with the session.</b> A rating that lived only in the
         * form would be lost the moment the chart was saved, which is precisely when it starts
         * to matter: an unsourced time and a birth certificate look identical once they are
         * both rows in a list.
         */
        public final Rodden rodden;
        /** Whatever the practitioner needs to remember about this chart. May be empty. */
        public final String notes;
        /** Comma-separated labels: "client", "family", "research". May be empty. */
        public final String tags;

        Entry(String date, String time, String location) {
            this(date, time, location, Rodden.A, "", "");
        }

        Entry(String date, String time, String location, Rodden rodden,
              String notes, String tags) {
            this.date = date;
            this.time = time;
            this.location = location;
            this.rodden = rodden == null ? Rodden.A : rodden;
            this.notes = notes == null ? "" : notes;
            this.tags = tags == null ? "" : tags;
        }

        /** The tags as a trimmed list, empty when there are none. */
        public java.util.List<String> tagList() {
            java.util.List<String> out = new ArrayList<>();
            for (String t : tags.split(",")) {
                String s = t.trim();
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
            return out;
        }
    }

    private SavedCharts() { }

    private static Properties read() {
        Properties p = new Properties();
        File f = new File(FILE);
        File legacy = new File(LEGACY);
        // Prefer the current file; fall back to the old one so nothing saved before the
        // rename disappears from the list.
        File source = f.exists() ? f : legacy;
        if (source.exists()) {
            try (FileInputStream in = new FileInputStream(source)) {
                p.load(in);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // Merge any legacy entries the current file does not already carry.
        if (f.exists() && legacy.exists()) {
            Properties old = new Properties();
            try (FileInputStream in = new FileInputStream(legacy)) {
                old.load(in);
                java.util.Set<String> buried = tombstones(p);
                for (String k : old.stringPropertyNames()) {
                    // <b>A deleted legacy chart must stay deleted.</b> Without this the merge
                    // undoes every delete of a chart that also exists in the old file: remove
                    // writes it out of the current one, and the very next read puts it back
                    // from the legacy one. The delete reports success and the chart returns,
                    // which is the worst shape of failure - the user believes it is gone.
                    int dot = k.lastIndexOf('.');
                    String owner = dot < 0 ? k : k.substring(0, dot);
                    if (!p.containsKey(k) && !buried.contains(owner)) {
                        p.setProperty(k, old.getProperty(k));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return p;
    }

    /** Key holding the names deleted out of the legacy file, so they stay deleted. */
    private static final String TOMBSTONES = "__deleted";

    /**
     * What separates buried names.
     *
     * A tab, not a newline or a comma: a Properties value is one line, and a chart may
     * legitimately be named "Smith, John". A tab cannot occur in a name typed into the save
     * dialog and survives the store/load round trip escaped.
     */
    private static final String SEP = "\t";

    private static java.util.Set<String> tombstones(Properties p) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (String s : p.getProperty(TOMBSTONES, "").split(SEP)) {
            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static void bury(Properties p, String name) {
        java.util.Set<String> t = tombstones(p);
        t.add(name);
        p.setProperty(TOMBSTONES, String.join(SEP, t));
    }

    /** Saved chart names, sorted. Never null. */
    public static List<String> names() {
        Properties p = read();
        List<String> out = new ArrayList<>();
        for (String k : p.stringPropertyNames()) {
            if (k.endsWith(".date")) {
                out.add(k.substring(0, k.length() - ".date".length()));
            }
        }
        Collections.sort(out);
        return out;
    }

    /** One chart by name, or null if absent. */
    public static Entry get(String name) {
        Properties p = read();
        String d = p.getProperty(name + ".date");
        if (d == null) {
            return null;
        }
        return new Entry(d, p.getProperty(name + ".time", ""),
            p.getProperty(name + ".location", ""),
            Rodden.of(p.getProperty(name + ".rodden")),
            p.getProperty(name + ".notes", ""),
            p.getProperty(name + ".tags", ""));
    }

    /** Adds or replaces a chart, keeping any rating and notes it already carried. */
    public static boolean put(String name, String date, String time, String location) {
        Entry existing = get(name);
        return put(name, date, time, location,
            existing == null ? Rodden.A : existing.rodden,
            existing == null ? "" : existing.notes,
            existing == null ? "" : existing.tags);
    }

    /**
     * Adds or replaces a chart with its full record.
     *
     * <b>Read-modify-write, never a rewrite.</b> The file is loaded, the keys for this one
     * chart are set, and everything else is stored back untouched - so a key this version does
     * not know about survives, and so does every other chart. This project has destroyed a
     * shared data file once by writing it wholesale.
     */
    public static boolean put(String name, String date, String time, String location,
                              Rodden rodden, String notes, String tags) {
        Properties p = read();
        p.setProperty(name + ".date", date);
        p.setProperty(name + ".time", time);
        p.setProperty(name + ".location", location);
        p.setProperty(name + ".rodden", (rodden == null ? Rodden.A : rodden).code);
        p.setProperty(name + ".notes", notes == null ? "" : notes);
        p.setProperty(name + ".tags", tags == null ? "" : tags);
        // Saving a name back un-buries it: a deliberate re-save outranks an old delete.
        java.util.Set<String> t = tombstones(p);
        if (t.remove(name)) {
            p.setProperty(TOMBSTONES, String.join(SEP, t));
        }
        return write(p);
    }

    /**
     * Removes one chart and everything filed under its name.
     *
     * <b>Every key with this prefix, not the five this version knows.</b> A chart saved by a
     * later version with a field this one has never heard of would otherwise leave that field
     * behind, and the orphan would reappear the day a chart of the same name was created.
     */
    public static boolean remove(String name) {
        Properties p = read();
        String prefix = name + ".";
        boolean found = false;
        for (String k : new ArrayList<>(p.stringPropertyNames())) {
            if (k.startsWith(prefix)) {
                p.remove(k);
                found = true;
            }
        }
        if (found) {
            bury(p, name);
        }
        return found && write(p);
    }

    /**
     * Renames a chart, carrying every field with it.
     *
     * Refuses to overwrite an existing name: a rename that silently replaced somebody else's
     * chart would destroy birth data, which is the one thing in this store nobody can retype.
     */
    public static boolean rename(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        String target = to.trim();
        if (target.isEmpty() || target.equals(from) || target.contains("=")) {
            return false;
        }
        Properties p = read();
        if (p.getProperty(target + ".date") != null) {
            return false;
        }
        String prefix = from + ".";
        boolean found = false;
        for (String k : new ArrayList<>(p.stringPropertyNames())) {
            if (k.startsWith(prefix)) {
                p.setProperty(target + k.substring(from.length()), p.getProperty(k));
                p.remove(k);
                found = true;
            }
        }
        return found && write(p);
    }

    /**
     * Names matching a search, and optionally carrying a tag.
     *
     * Case-insensitive, and matches the notes as well as the name - a practitioner searching
     * "rectified" or a city is looking for charts, not for a field.
     */
    public static List<String> search(String query, String tag) {
        String q = query == null ? "" : query.trim().toLowerCase();
        String t = tag == null ? "" : tag.trim().toLowerCase();
        List<String> out = new ArrayList<>();
        for (String name : names()) {
            Entry e = get(name);
            if (e == null) {
                continue;
            }
            boolean hitsQuery = q.isEmpty()
                || name.toLowerCase().contains(q)
                || e.notes.toLowerCase().contains(q)
                || e.location.toLowerCase().contains(q)
                || e.tags.toLowerCase().contains(q);
            boolean hitsTag = t.isEmpty();
            if (!hitsTag) {
                for (String each : e.tagList()) {
                    if (each.toLowerCase().equals(t)) {
                        hitsTag = true;
                        break;
                    }
                }
            }
            if (hitsQuery && hitsTag) {
                out.add(name);
            }
        }
        return out;
    }

    /** Every tag in use, sorted and de-duplicated - the grouping the reader actually has. */
    public static List<String> tags() {
        java.util.TreeSet<String> set = new java.util.TreeSet<>();
        for (String name : names()) {
            Entry e = get(name);
            if (e != null) {
                set.addAll(e.tagList());
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * Copies the whole chart book to a file the user chooses.
     *
     * <b>The backup this store has never had.</b> Client birth data lived in one properties
     * file beside the executable with no copy anywhere - a deleted folder or a bad write and
     * it is gone, and birth data is the one thing here nobody can reconstruct.
     */
    public static boolean exportBook(File target) {
        Properties p = read();
        try (FileOutputStream out = new FileOutputStream(target)) {
            p.store(out, "Ourania chart book export");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Merges a book file in, and says how many charts arrived.
     *
     * <b>Merge, never replace.</b> Importing a colleague's five charts must not take the
     * hundred already here with it. A name already present is skipped rather than overwritten,
     * so an import can add but can never destroy; the count returned is what actually landed.
     */
    public static int importBook(File source) {
        Properties incoming = new Properties();
        try (FileInputStream in = new FileInputStream(source)) {
            incoming.load(in);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
        Properties p = read();
        int added = 0;
        for (String k : incoming.stringPropertyNames()) {
            if (!k.endsWith(".date")) {
                continue;
            }
            String name = k.substring(0, k.length() - ".date".length());
            if (p.getProperty(k) != null) {
                continue;
            }
            for (String field : incoming.stringPropertyNames()) {
                if (field.startsWith(name + ".")) {
                    p.setProperty(field, incoming.getProperty(field));
                }
            }
            added++;
        }
        return added > 0 && write(p) ? added : 0;
    }

    private static boolean write(Properties p) {
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            p.store(out, "Saved charts - natal and transit, shared");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
