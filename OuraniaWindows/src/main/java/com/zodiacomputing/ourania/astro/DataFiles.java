package com.zodiacomputing.ourania.astro;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Where the engine's data files come from: the interpretation corpus, by name (M1).
 *
 * <p><b>A name, not a path, is what the loader asks for.</b> On the desktop the answer is a file
 * in {@link AppPaths#dataDir()}, exactly as it always was. On the phone the same 83 MB of prose
 * is packed inside the app, where there is no path to give: Android hands out a stream per asset
 * and nothing else. Copying it all out on first launch would work and would store the corpus
 * twice. So the loader asks this class to open "interpretations.json", and the phone installs a
 * {@link Source} that opens it from the package - one call at start-up, before the corpus is
 * first read.
 *
 * <p><b>Not the ephemeris.</b> The Swiss Ephemeris seeks about inside its files, which a packed
 * asset cannot do, so on the phone those few files are copied out once and
 * {@link Ephemeris#PROPERTY} is pointed at the copy. They are two megabytes; the prose is not.
 *
 * <p>{@code DataFilesCheck} holds the loader to this: with the data folder pointed at nowhere and
 * the corpus served only through a {@code Source}, the readings still come back.
 */
public final class DataFiles {

    private DataFiles() { }

    /** Somewhere named data files can be opened from. */
    public interface Source {
        /** True when a file of this name can be opened. */
        boolean exists(String name);

        /** The file's bytes. The caller closes the stream. */
        InputStream open(String name) throws IOException;

        /** Where this reads from, for a crash report or a notice. */
        String where();
    }

    /** Files in a folder - the desktop, and every run before M1. */
    public static final class Folder implements Source {
        private final String dir;

        /** @param dir the folder, with or without a trailing slash */
        public Folder(String dir) {
            this.dir = dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + "/";
        }

        @Override
        public boolean exists(String name) {
            return new File(this.dir + name).isFile();
        }

        @Override
        public InputStream open(String name) throws IOException {
            return new FileInputStream(this.dir + name);
        }

        @Override
        public String where() {
            return this.dir;
        }
    }

    private static volatile Source source;

    /** Where data files are opened from now: the one installed, or the data folder. */
    public static Source source() {
        Source s = source;
        if (s == null) {
            s = new Folder(AppPaths.dataDir());
            source = s;
        }
        return s;
    }

    /**
     * Opens data files from here from now on.
     *
     * Install it before the corpus is first read - {@code InterpretationService} loads once, so a
     * source installed afterwards is used by nothing that has already been loaded.
     */
    public static void use(Source s) {
        source = s;
    }

    /** A data file by name, opened through the current source. */
    public static Entry entry(String name) {
        return new Entry(name);
    }

    /**
     * One named data file. Shaped like the {@code java.io.File} the loader used to hold -
     * {@link #exists} and {@link #getName} - so the loader's own logic did not have to change,
     * only what it opens.
     */
    public static final class Entry {
        private final String name;

        Entry(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public boolean exists() {
            return source().exists(this.name);
        }

        public InputStream open() throws IOException {
            return source().open(this.name);
        }

        /**
         * The whole file. Read in a loop rather than with {@code readAllBytes}, which Android
         * only has from version 13.
         */
        public byte[] bytes() throws IOException {
            try (InputStream in = this.open()) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(1 << 16);
                byte[] buf = new byte[1 << 16];
                for (int n; (n = in.read(buf)) > 0; ) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        }

        @Override
        public String toString() {
            return source().where() + this.name;
        }
    }
}
