package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * Declination: the sky measured north and south of the equator, where the zodiac measures
 * along the ecliptic.
 *
 * <p><b>Computed since the start and never shown.</b> {@link ChartFrame} has asked Swiss
 * Ephemeris for every body's declination and set {@code outOfBounds} on it from the first
 * version, and {@link Gestalt} collects the out-of-bounds names into a list - which nothing
 * reads. Master list D4 on 2026-09-02 found no declination table, no parallel aspects and no
 * out-of-bounds report anywhere in the application. This is the engine for all three.
 *
 * <p><b>Out of bounds</b> is a body further from the equator than the Sun can ever go - past
 * the obliquity, about 23&deg;26'. It is read as a planet acting outside the usual rules of
 * the chart. <b>The Sun is never reported</b>, because it defines the bound: at a solstice its
 * declination equals the obliquity, and its few arcseconds of ecliptic latitude would
 * otherwise flag it out of bounds for a day or two each year, which means nothing.
 *
 * <p><b>Parallel</b> is two bodies at the same declination on the same side of the equator,
 * read like a conjunction; <b>contraparallel</b> is the same declination on opposite sides,
 * read like an opposition.
 *
 * <p><b>The set is the Sun, the Moon and Mercury to Pluto.</b> The asteroids are left out on
 * purpose: Pallas is inclined 35&deg; to the ecliptic and routinely sits far out of bounds, so
 * including them would fill the report with the one body the concept was never about.
 */
public final class Declinations {

    private Declinations() { }

    /**
     * How close two declinations must be to count as parallel, in degrees.
     *
     * One degree is the orb in general use for parallels, and it has not been calibrated
     * against anything here - the same standing as every other orb in this project. Held as a
     * field so a measured value has somewhere to land.
     */
    public static double parallelOrb = 1.0;

    /** One body's place north or south of the equator. */
    public static final class Entry {
        public final String name;
        /** Degrees, north positive. */
        public final double declination;
        public final boolean outOfBounds;
        /** How far past the obliquity, in degrees; zero when in bounds. */
        public final double beyond;

        Entry(String name, double declination, boolean outOfBounds, double beyond) {
            this.name = name;
            this.declination = declination;
            this.outOfBounds = outOfBounds;
            this.beyond = beyond;
        }
    }

    /** Two bodies at the same declination, on the same side or on opposite sides. */
    public static final class Contact {
        public final String a;
        public final String b;
        /** False for parallel, true for contraparallel. */
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

    /** A chart's declinations, out-of-bounds bodies, and parallels. */
    public static final class Result {
        public final double obliquity;
        public final List<Entry> entries = new ArrayList<>();
        public final List<Contact> contacts = new ArrayList<>();

        Result(double obliquity) {
            this.obliquity = obliquity;
        }

        public List<Entry> outOfBounds() {
            List<Entry> out = new ArrayList<>();
            for (Entry e : entries) {
                if (e.outOfBounds) {
                    out.add(e);
                }
            }
            return out;
        }
    }

    /** True for the bodies this report covers: the lights and the planets. */
    static boolean covered(Bodies.Def d) {
        return d.kind == Bodies.Kind.LUMINARY || d.kind == Bodies.Kind.PLANET;
    }

    public static Result of(ChartFrame f) {
        Result r = new Result(f.trueObliquity);
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b == null || !b.ok || !covered(Bodies.at(i)) || Double.isNaN(b.dec)) {
                continue;
            }
            boolean sun = "Sun".equals(b.name);
            double past = Math.abs(b.dec) - f.trueObliquity;
            boolean oob = !sun && past > 0.0;
            r.entries.add(new Entry(b.name, b.dec, oob, oob ? past : 0.0));
        }
        for (int i = 0; i < r.entries.size(); i++) {
            for (int j = i + 1; j < r.entries.size(); j++) {
                Contact c = contact(r.entries.get(i), r.entries.get(j));
                if (c != null) {
                    r.contacts.add(c);
                }
            }
        }
        r.contacts.sort((x, y) -> Double.compare(x.off, y.off));
        return r;
    }

    /** The contact between two bodies, or null when neither is within orb. */
    static Contact contact(Entry x, Entry y) {
        double parallel = Math.abs(x.declination - y.declination);
        double contra = Math.abs(x.declination + y.declination);
        // Near the equator two bodies can be within orb both ways - 0.2 N and 0.3 S are 0.5
        // apart as a parallel and 0.1 as a contraparallel. The tighter one is reported, and a tie
        // goes to parallel, which is the reading that does not invent an opposition.
        if (parallel <= parallelOrb && parallel <= contra) {
            return new Contact(x.name, y.name, false, parallel);
        }
        if (contra <= parallelOrb) {
            return new Contact(x.name, y.name, true, contra);
        }
        return null;
    }
}
