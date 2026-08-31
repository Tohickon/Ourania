package com.zodiacomputing.ourania.gui;

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

        Entry(String date, String time, String location) {
            this.date = date;
            this.time = time;
            this.location = location;
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
                for (String k : old.stringPropertyNames()) {
                    if (!p.containsKey(k)) {
                        p.setProperty(k, old.getProperty(k));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return p;
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
            p.getProperty(name + ".location", ""));
    }

    /** Adds or replaces a chart. Returns false if it could not be written. */
    public static boolean put(String name, String date, String time, String location) {
        Properties p = read();
        p.setProperty(name + ".date", date);
        p.setProperty(name + ".time", time);
        p.setProperty(name + ".location", location);
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            p.store(out, "Saved charts - natal and transit, shared");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
