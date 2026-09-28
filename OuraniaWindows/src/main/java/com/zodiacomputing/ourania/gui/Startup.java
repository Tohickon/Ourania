package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Ephemeris;

/**
 * What is checked before the window opens, and what the reader is told about it (J9).
 *
 * <p><b>The row said "nothing verifies the ephemeris directory, the data files, or the settings
 * file before the window opens", and the first thing this class measured proved why that
 * matters:</b> {@code semo_18.se1} - the Moon's file - has never been in this machine's ephemeris
 * directory. Every {@code swe_calc_ut} in the app carries "SwissEph file 'semo_18.se1' not found"
 * in its error buffer and returns a success code anyway, and the North Node is quietly computed by
 * the built-in approximation rather than from the files, on every chart, since the project began.
 * Nothing said so. {@link Ephemeris#present} existed to let something say it and had no caller -
 * the thing exists and has no door, which is this project's most logged finding.
 *
 * <p><b>It asks the library rather than reading the directory.</b> A list of expected filenames is
 * a second copy of Swiss Ephemeris's own naming scheme, it goes stale when the date moves out of
 * one file's era, and it cannot see the case that actually bit: a body whose file is present and
 * which falls back anyway. So {@link #inspect} computes every body in the registry at the moment
 * the app is starting and reports what each one's return code says about where the answer came
 * from. That is a measurement of this launch, and it is worth exactly what it measures - which is
 * why the finding names the instant it was taken at.
 *
 * <p><b>A return code is not proof the file was read.</b> Measured 28 Sep with the Moon's file
 * absent: the Moon itself reports {@code SEFLG_SWIEPH}, at 1500 and at 2450 as well, where every
 * planet beside it has fallen back to Moshier. So this class reports a fallback it can see and
 * does not claim the rest were read from disk. The same fact is what killed both candidate guards
 * for the silent wrong Sun; see {@code known-red.txt}.
 *
 * <p><b>Nothing here stops the app.</b> Every one of these conditions has a working fallback -
 * that is precisely the problem, and why they went unnoticed. The findings are shown, not obeyed.
 */
final class Startup {

    private Startup() { }

    /** How much a finding costs the reader. Nothing here prevents the app from opening. */
    enum Level {
        /** The app will run and answer differently from how it is meant to. */
        DEGRADED,
        /** Worth knowing, costs nothing. */
        NOTE
    }

    /** One thing found, in the reader's words rather than the code's. */
    static final class Finding {
        final Level level;
        final String what;
        final String detail;

        Finding(Level level, String what, String detail) {
            this.level = level;
            this.what = what;
            this.detail = detail == null ? "" : detail;
        }

        @Override
        public String toString() {
            return level + ": " + what + (detail.isEmpty() ? "" : " - " + detail);
        }
    }

    /**
     * A moment to measure the ephemeris at, or null for now.
     *
     * <b>A seam the suite needs and nothing else uses.</b> "Now" makes the one assertion that
     * matters here untestable: a check that asserts an era boundary has to choose the era. Reading
     * it from a field rather than passing it through {@link #inspect} keeps the caller in
     * {@code main} a single argument-free call, which is what makes it hard to get wrong.
     */
    static Double jdOverride;

    /** A data directory for the suite to point at instead of the reader's, or null for theirs. */
    static String dataDirOverride;

    /**
     * Everything worth saying about how this launch is equipped, in the order a reader cares.
     *
     * Pure: it opens files and computes positions and changes nothing. That is what lets the check
     * suite run it against a directory of its own, and what lets the Diagnostics tab run it again
     * whenever it is opened rather than showing a snapshot from start-up.
     */
    static java.util.List<Finding> inspect() {
        java.util.List<Finding> out = new java.util.ArrayList<>();
        ephemeris(out);
        data(out);
        settings(out);
        log(out);
        return out;
    }

    // ------------------------------------------------------------------ the ephemeris

    /** The bodies whose answers did not come from the ephemeris files, at the moment measured. */
    private static void ephemeris(java.util.List<Finding> out) {
        java.io.File dir = new java.io.File(Ephemeris.PATH);
        if (!Ephemeris.present()) {
            out.add(new Finding(Level.DEGRADED, "The ephemeris folder is not there",
                Ephemeris.PATH + " - every position will come from the built-in approximation, "
                + "which is close but is not what this app is meant to be answering from. Set "
                + Ephemeris.PROPERTY + " or " + Ephemeris.ENV + " to where the files are."));
            return;
        }

        double jd = jdOverride != null ? jdOverride
            : new de.thmac.swisseph.SweDate(java.time.LocalDate.now().getYear(),
                java.time.LocalDate.now().getMonthValue(),
                java.time.LocalDate.now().getDayOfMonth(), 12.0).getJulDay();
        de.thmac.swisseph.SwissEph sw = new de.thmac.swisseph.SwissEph(Ephemeris.PATH);

        java.util.List<String> fellBack = new java.util.ArrayList<>();
        java.util.List<String> failed = new java.util.ArrayList<>();
        java.util.Set<String> missing = new java.util.TreeSet<>();
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def def = Bodies.at(i);
            if (def.getIpl() < 0) {
                continue;
            }
            double[] xx = new double[6];
            StringBuffer serr = new StringBuffer();
            int rc = sw.swe_calc_ut(jd, def.getIpl(),
                de.thmac.swisseph.SweConst.SEFLG_SWIEPH, xx, serr);
            if (rc < 0) {
                failed.add(def.name);
            } else if ((rc & de.thmac.swisseph.SweConst.SEFLG_MOSEPH) != 0) {
                fellBack.add(def.name);
            }
            missing.addAll(namedFiles(serr.toString()));
        }

        // <b>The missing files come first even when nothing fell back</b>, because that is the
        // case this class was written for: the Moon's file has been absent all along, everything
        // reported success, and the one body it actually cost was two rows further down a list
        // nobody was printing.
        if (!missing.isEmpty()) {
            out.add(new Finding(Level.DEGRADED,
                missing.size() == 1 ? "The ephemeris is missing one of its files"
                    : "The ephemeris is missing " + missing.size() + " of its files",
                String.join(", ", missing) + " - named by Swiss Ephemeris itself while computing "
                + "this moment, in " + dir.getPath() + ". They can be downloaded from the Swiss "
                + "Ephemeris distribution and dropped into that folder; nothing else has to "
                + "change."));
        }
        if (!fellBack.isEmpty()) {
            out.add(new Finding(Level.DEGRADED,
                fellBack.size() + (fellBack.size() == 1 ? " body is" : " bodies are")
                + " computed by approximation rather than from the files",
                String.join(", ", fellBack) + " - measured at " + when(jd) + ". The answer is "
                + "close, within an arcsecond or so, but it is arrived at by a different method "
                + "from every other point in the same chart."));
        }
        if (!failed.isEmpty()) {
            out.add(new Finding(Level.DEGRADED,
                failed.size() + (failed.size() == 1 ? " body cannot be computed"
                    : " bodies cannot be computed") + " at all",
                String.join(", ", failed) + " - measured at " + when(jd) + ". These points will "
                + "be absent from the chart rather than wrong in it."));
        }
        if (missing.isEmpty() && fellBack.isEmpty() && failed.isEmpty()) {
            out.add(new Finding(Level.NOTE, "Every body answered from the ephemeris files",
                "Measured at " + when(jd) + " in " + dir.getPath() + "."));
        }
    }

    /**
     * The {@code .se1} files Swiss Ephemeris says it looked for and did not find.
     *
     * <b>Read out of the error buffer rather than guessed from the date.</b> Which file covers
     * which years is the library's rule, and restating it here would be a second copy of it that
     * is wrong the first time the scheme changes. The buffer names the file in quotes, so that is
     * what is taken.
     */
    static java.util.List<String> namedFiles(String serr) {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (serr == null) {
            return names;
        }
        int at = 0;
        while (true) {
            int open = serr.indexOf('\'', at);
            if (open < 0) {
                return names;
            }
            int close = serr.indexOf('\'', open + 1);
            if (close < 0) {
                return names;
            }
            String quoted = serr.substring(open + 1, close);
            // <b>"not found" follows the name, and the first draft looked for it in front.</b>
            // The message is "SwissEph file 'semo_18.se1' not found in the paths of: '...'", so a
            // backwards search from the closing quote found nothing and this method reported no
            // missing files on a machine that is missing one. It also reads the trailing path,
            // which is quoted too - that is why the suffix is tested rather than the quoting.
            if (quoted.toLowerCase().endsWith(".se1")
                    && serr.startsWith(" not found", close + 1)) {
                names.add(quoted);
            }
            at = close + 1;
        }
    }

    /** A Julian day as a date a reader can place, which is the only reason it is printed. */
    private static String when(double jd) {
        de.thmac.swisseph.SweDate d = new de.thmac.swisseph.SweDate(jd);
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonth(), d.getDay());
    }

    // ------------------------------------------------------------------ the prose corpus

    /** Which of the files the interpretation corpus will ask for are not on disk. */
    private static void data(java.util.List<Finding> out) {
        String dir = dataDirOverride != null ? dataDirOverride : InterpretationService.DATA_DIR;
        java.io.File d = new java.io.File(dir);
        if (!d.isDirectory()) {
            out.add(new Finding(Level.DEGRADED, "The data folder is not there",
                d.getAbsolutePath() + " - the app will open with no interpretations at all."));
            return;
        }
        java.util.List<String> wanted = new java.util.ArrayList<>(java.util.Arrays.asList(
            InterpretationService.everyFileName()));
        // The gazetteer belongs to Atlas, not to the corpus, and its absence is invisible in a
        // different way: the place search returns nothing and looks like a place nobody has.
        wanted.add(Atlas.FILE_NAME);
        java.util.List<String> absent = new java.util.ArrayList<>();
        for (String name : wanted) {
            if (!new java.io.File(dir + name).isFile()) {
                absent.add(name);
            }
        }
        if (absent.isEmpty()) {
            out.add(new Finding(Level.NOTE, "The interpretation corpus is complete",
                wanted.size() + " files in " + d.getAbsolutePath() + "."));
        } else {
            out.add(new Finding(Level.DEGRADED,
                absent.size() + " interpretation " + (absent.size() == 1 ? "file is" : "files are")
                + " missing", String.join(", ", absent) + " - in " + d.getAbsolutePath()
                + ". What those files carry will read as silence rather than as an error."));
        }
    }

    // ------------------------------------------------------------------ the reader's own files

    /**
     * Whether the settings file was read, and whether an earlier one was set aside without
     * anybody being told.
     *
     * <b>The second half is the reason this is here.</b> J11 made a settings file that will not
     * parse get renamed rather than overwritten, which saved the evidence - and then printed one
     * line to a console a packaged app does not have. A reader whose preferences silently reverted
     * has been given the explanation and cannot see it.
     */
    private static void settings(java.util.List<Finding> out) {
        if (Settings.loadFailed()) {
            out.add(new Finding(Level.DEGRADED, "The settings file could not be read",
                Settings.file() + " - it will be kept with a .corrupt suffix rather than "
                + "overwritten, and this session starts from the defaults."));
        }
        java.io.File f = new java.io.File(Settings.file());
        java.io.File dir = f.getAbsoluteFile().getParentFile();
        String[] names = dir == null ? null : dir.list();
        java.util.List<String> aside = new java.util.ArrayList<>();
        if (names != null) {
            for (String n : names) {
                if (n.startsWith(f.getName() + ".corrupt")) {
                    aside.add(n);
                }
            }
        }
        if (!aside.isEmpty()) {
            java.util.Collections.sort(aside);
            out.add(new Finding(Level.NOTE,
                aside.size() + " settings " + (aside.size() == 1 ? "file was" : "files were")
                + " set aside as unreadable",
                String.join(", ", aside) + " in " + (dir == null ? "." : dir.getPath())
                + ". Each one is a session that started from the defaults."));
        }
    }

    /**
     * Whether a problem has been written down, which nothing told the reader before (J8).
     *
     * <b>The row J8 could not close without.</b> The log became one honest place on 27 Sep and
     * stayed a place nobody is ever sent to. Size and time rather than contents: what is in it is
     * the report's business, and reading a 2 MB file to start a window is not.
     */
    private static void log(java.util.List<Finding> out) {
        java.io.File f = ErrorLog.file();
        if (!f.isFile() || f.length() == 0) {
            return;
        }
        out.add(new Finding(Level.NOTE, "Problems have been written down",
            f.getPath() + " - last written " + java.time.Instant.ofEpochMilli(f.lastModified())
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            + ", " + f.length() + " bytes. Diagnostics can save it as a report."));
    }

    // ------------------------------------------------------------------ what main does

    /** Settings key holding the findings a reader has already been shown. */
    static final String SEEN_KEY = "diagnostics.seen";

    /**
     * A short stable name for a set of findings, so the same ones are announced once.
     *
     * <b>The degraded ones only, and their headlines rather than their detail.</b> The detail
     * carries the date it was measured at, so including it would make every launch a new set and
     * turn a notice into a nag - which is how a reader learns to dismiss the one that matters.
     * A finding appearing or disappearing changes this; the same finding tomorrow does not.
     */
    static String signature(java.util.List<Finding> findings) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (Finding f : findings) {
            if (f.level == Level.DEGRADED) {
                parts.add(f.what);
            }
        }
        java.util.Collections.sort(parts);
        return parts.isEmpty() ? "" : Integer.toHexString(String.join("|", parts).hashCode());
    }

    /** True when there is something costly to say that this reader has not been told. */
    static boolean unannounced(java.util.List<Finding> findings) {
        String now = signature(findings);
        return !now.isEmpty() && !now.equals(Settings.get(SEEN_KEY, ""));
    }

    /** Remembers that these findings have been shown, so the next launch is quiet. */
    static void announced(java.util.List<Finding> findings) {
        Settings.set(SEEN_KEY, signature(findings));
    }

    /** True when anything found costs the reader an answer. */
    static boolean anyDegraded(java.util.List<Finding> findings) {
        for (Finding f : findings) {
            if (f.level == Level.DEGRADED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inspects, and writes what was found where a problem gets written down.
     *
     * <b>Called from {@code main} before the window is built, and it never throws.</b> A check
     * that can stop the app from opening is worse than the conditions it checks for, every one of
     * which the app already survives. The findings are returned as well as logged so the window
     * can show them.
     */
    static java.util.List<Finding> runAndRecord() {
        java.util.List<Finding> findings;
        try {
            findings = inspect();
        } catch (Throwable t) {
            ErrorLog.record("start-up inspection failed", t);
            return java.util.Collections.emptyList();
        }
        if (anyDegraded(findings)) {
            StringBuilder sb = new StringBuilder("start-up inspection, "
                + Version.display() + "\n");
            for (Finding f : findings) {
                sb.append("  ").append(f).append('\n');
            }
            ErrorLog.record(sb.toString(), null);
        }
        return findings;
    }
}
