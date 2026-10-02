package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Rodden;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.SavedCharts;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * The chart book on the phone (M6): saving a birth, listing the book, opening a chart again.
 *
 * <p><b>The desktop's book, not a second one.</b> Every chart goes through {@link SavedCharts},
 * the same class and the same file format the desktop keeps ({@code saved_charts.properties},
 * in the app's private storage here), and export and import are its own
 * {@link SavedCharts#exportBook} and {@link SavedCharts#importBook} - so a book exported from
 * either app opens in the other (G2).
 *
 * <p><b>Fields are written the way the desktop's form writes them:</b> the date as
 * {@code yyyy-MM-dd}, the time as {@code HH:mm} (empty when unknown), the place as the
 * "lat, lon" the desktop's place box fills in when a suggestion is picked, and an unknown time
 * as Rodden X, which is what the desktop reads as "cast for noon, angles withheld". A place
 * the desktop saved as a typed name is found in the atlas as the desktop finds it.
 *
 * <p>No Android in here, like {@link PhoneChart}: {@code PhoneBookTest} runs it on the JVM.
 */
final class PhoneBook {

    private PhoneBook() { }

    /** A saved chart read back: the three answers the form needs, and its name. */
    static final class Birth {
        final String name;
        final LocalDate date;
        /** Null when the time is not known. */
        final LocalTime time;
        final Atlas.Place place;

        Birth(String name, LocalDate date, LocalTime time, Atlas.Place place) {
            this.name = name;
            this.date = date;
            this.time = time;
            this.place = place;
        }
    }

    /** Every saved name, sorted, as the desktop lists them. */
    static List<String> names() {
        return SavedCharts.names();
    }

    static boolean exists(String name) {
        return SavedCharts.get(name) != null;
    }

    /**
     * Saves a birth under a name, replacing one of the same name. Notes and tags already on it
     * - written on the desktop, perhaps - are kept; the rating becomes X for an unknown time,
     * and otherwise stays what it was (A for a new chart).
     */
    static boolean save(String name, LocalDate date, LocalTime time, Atlas.Place place) {
        SavedCharts.Entry prior = SavedCharts.get(name);
        Rodden rating = time == null ? Rodden.X
            : prior == null || prior.rodden.timeUnknown() ? Rodden.A : prior.rodden;
        return SavedCharts.put(name.trim(), date.toString(),
            time == null ? "" : String.format(Locale.ROOT, "%02d:%02d", time.getHour(),
                time.getMinute()),
            coordinates(place), rating,
            prior == null ? "" : prior.notes, prior == null ? "" : prior.tags);
    }

    /** The place as the desktop's place box writes a picked suggestion: "lat, lon". */
    static String coordinates(Atlas.Place p) {
        return String.format(Locale.ROOT, "%.4f, %.4f", p.latitude, p.longitude);
    }

    /**
     * A saved chart ready to cast, or null when it cannot be read: a date that does not parse,
     * or a place neither given as coordinates nor in the atlas. The desktop falls back to an
     * online lookup for those; the phone is offline, so it says so instead.
     */
    static Birth open(String name) {
        SavedCharts.Entry e = SavedCharts.get(name);
        if (e == null) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(e.date.trim());
            LocalTime time = null;
            String t = e.time == null ? "" : e.time.trim();
            if (!e.rodden.timeUnknown() && !t.isEmpty()) {
                String[] hm = t.split(":");
                time = LocalTime.of(Integer.parseInt(hm[0].trim()),
                    hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0);
            }
            Atlas.Place place = Atlas.fromCoordinates(e.location);
            if (place == null) {
                place = Atlas.resolve(e.location);
            }
            return place == null ? null : new Birth(name, date, time, place);
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    static boolean remove(String name) {
        return SavedCharts.remove(name);
    }

    /** The whole book to a file, in the desktop's export format. */
    static boolean export(File to) {
        return SavedCharts.exportBook(to);
    }

    /** Merges a book file in - never replacing a chart already here - and says how many came. */
    static int importFrom(File from) {
        return SavedCharts.importBook(from);
    }
}
