package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.tools.GenerateExtraBodies;
import com.zodiacomputing.ourania.tools.GenerateExtraBodies.Result;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Guards the one script in this repository that has destroyed another agent's work.
 *
 * On 2026-08-13 GenerateExtraBodies deleted 192 entries belonging to Antigravity and printed
 * "Successfully merged". The mechanism was not the templates: it was a line-oriented regex
 * reader feeding a wholesale rewrite, so anything the reader could not classify was parsed
 * away and then written out of existence. An acknowledgement flag was added afterwards, but a
 * flag gates who runs it, not what it does once running.
 *
 * These checks assert the behaviour that makes the loss impossible rather than merely
 * inconvenient, because that behaviour is easy to remove by accident and its absence stays
 * invisible until the day it costs a corpus.
 *
 * Every part runs against a synthetic fixture in a temp directory, never the real corpus, and
 * the fixture is fixed - so this suite's check count does not move with the user's settings or
 * with the size of the corpus. Several older suites still fail that second test.
 */
public final class GenerateExtraBodiesCheck {

    private static final List<String> failures = new ArrayList<String>();
    private static int checks = 0;

    private GenerateExtraBodiesCheck() { }

    /**
     * Four entries, each proving a different thing.
     *
     * "ceres" and "transit_sun_square_natal_chiron" are keys the generator has its own template
     * for, so they detect overwriting. "sun_conjunction_moon" is one of the 3,036 the aspect
     * loop produces, for the same reason. "custom_thing" is a key the generator can never
     * produce, so it detects the deletion of a foreign entry - which is what actually happened
     * in August. planet_sign and planet_house are declared inline as empty objects, the shape
     * that matched neither original pattern and was dropped on every run until 2026-09-02.
     */
    private static final String FIXTURE =
            "{\n"
          + "  \"body_core\": {\n"
          + "    \"ceres\": \"SENTINEL body core\",\n"
          + "    \"custom_thing\": \"a key no template can produce\"\n"
          + "  },\n"
          + "  \"planet_sign\": {},\n"
          + "  \"planet_house\": {},\n"
          + "  \"aspects\": {\n"
          + "    \"sun_conjunction_moon\": \"SENTINEL aspect\"\n"
          + "  },\n"
          + "  \"transits\": {\n"
          + "    \"transit_sun_square_natal_chiron\": \"SENTINEL transit\"\n"
          + "  }\n"
          + "}\n";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Part A: it refuses without the acknowledgement ===");
        int before = failures.size();
        refusesWithoutAck();
        report("Part A", before);

        System.out.println("=== Part B: a clean run loses no key and no section ===");
        before = failures.size();
        cleanRunLosesNothing();
        report("Part B", before);

        System.out.println("=== Part C: an unparseable line aborts and changes nothing ===");
        before = failures.size();
        unparseableLineAborts();
        report("Part C", before);

        System.out.println("=== Part D: existing prose outranks every template ===");
        before = failures.size();
        neverOverwrites();
        report("Part D", before);

        System.out.println("=== Part E: a missing file aborts, it is not created ===");
        before = failures.size();
        missingFileAborts();
        report("Part E", before);

        System.out.println("=== Part F: the previous file is kept ===");
        before = failures.size();
        keepsABackup();
        report("Part F", before);

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

    /** A flag that can be forgotten is still the only thing between a user and a rewrite. */
    private static void refusesWithoutAck() throws Exception {
        Path dir = sandbox();
        File f = fixtureIn(dir);
        byte[] was = Files.readAllBytes(f.toPath());

        Result r = runOn(f, new String[0]);

        is("no acknowledgement returns REFUSED_NO_ACK", Result.REFUSED_NO_ACK, r);
        yes("a refused run leaves the file byte-identical",
                Arrays.equals(was, Files.readAllBytes(f.toPath())));
        is("a refused run writes no backup", 0, backupsIn(dir));
        clean(dir);
    }

    /** The August loss looked like a clean run. Every original key must survive one. */
    private static void cleanRunLosesNothing() throws Exception {
        Path dir = sandbox();
        File f = fixtureIn(dir);

        Result r = runOn(f, new String[] { GenerateExtraBodies.ACK });
        String after = read(f);

        is("an acknowledged run on a good file returns WROTE", Result.WROTE, r);
        yes("a body_core entry survives", after.contains("\"ceres\""));
        yes("a key no template can produce survives", after.contains("\"custom_thing\""));
        yes("an aspect key survives", after.contains("\"sun_conjunction_moon\""));
        yes("a transit key survives", after.contains("\"transit_sun_square_natal_chiron\""));
        yes("the empty planet_sign section survives", after.contains("\"planet_sign\": {}"));
        yes("the empty planet_house section survives", after.contains("\"planet_house\": {}"));
        yes("the run still adds the templates it exists to add",
                after.contains("\"sun_sextile_moon\""));
        clean(dir);
    }

    /**
     * The regression that matters most. A two-line value is what another agent's editor
     * produces, and it is exactly what the old reader parsed away before rewriting.
     */
    private static void unparseableLineAborts() throws Exception {
        Path dir = sandbox();
        File f = fixtureIn(dir);
        String injected = FIXTURE.replace(
                "  \"body_core\": {\n",
                "  \"body_core\": {\n"
              + "    \"multiline\": \"this value opens here\n"
              + "      and finishes on a second line\",\n");
        Files.write(f.toPath(), injected.getBytes("UTF-8"));
        byte[] was = Files.readAllBytes(f.toPath());

        Result r = runOn(f, new String[] { GenerateExtraBodies.ACK });

        is("a line the reader cannot classify returns ABORTED_UNPARSED",
                Result.ABORTED_UNPARSED, r);
        yes("an aborted run leaves the file byte-identical",
                Arrays.equals(was, Files.readAllBytes(f.toPath())));
        yes("the unparseable entry is still in the file", read(f).contains("\"multiline\""));
        is("an aborted run writes no backup", 0, backupsIn(dir));
        clean(dir);
    }

    /** put() overwrote hand-written prose with boilerplate. Every write is putIfAbsent now. */
    private static void neverOverwrites() throws Exception {
        Path dir = sandbox();
        File f = fixtureIn(dir);

        Result r = runOn(f, new String[] { GenerateExtraBodies.ACK });
        String after = read(f);

        is("the run completes", Result.WROTE, r);
        yes("a hand-written body_core entry is not replaced", after.contains("SENTINEL body core"));
        yes("a hand-written aspect entry is not replaced", after.contains("SENTINEL aspect"));
        yes("a hand-written transit entry is not replaced", after.contains("SENTINEL transit"));
        clean(dir);
    }

    /** An absent corpus is a mounting problem, not an invitation to generate 3,036 stubs. */
    private static void missingFileAborts() throws Exception {
        Path dir = sandbox();
        File f = new File(dir.toFile(), "extra_bodies.json");

        Result r = runOn(f, new String[] { GenerateExtraBodies.ACK });

        is("a missing file returns NO_FILE", Result.NO_FILE, r);
        yes("a missing file is not created from templates", !f.exists());
        clean(dir);
    }

    /** Recovery in August was impossible because no copy existed anywhere. */
    private static void keepsABackup() throws Exception {
        Path dir = sandbox();
        File f = fixtureIn(dir);
        byte[] was = Files.readAllBytes(f.toPath());

        runOn(f, new String[] { GenerateExtraBodies.ACK });

        is("a successful run leaves exactly one backup", 1, backupsIn(dir));
        File bak = firstBackupIn(dir);
        yes("the backup holds what the file held before the run",
                bak != null && Arrays.equals(was, Files.readAllBytes(bak.toPath())));
        clean(dir);
    }

    // ---------------------------------------------------------------- harness

    private static Result runOn(File target, String[] args) throws Exception {
        String previous = System.getProperty(GenerateExtraBodies.PATH_PROPERTY);
        System.setProperty(GenerateExtraBodies.PATH_PROPERTY, target.getAbsolutePath());
        try {
            return GenerateExtraBodies.run(args);
        } finally {
            if (previous == null) System.clearProperty(GenerateExtraBodies.PATH_PROPERTY);
            else System.setProperty(GenerateExtraBodies.PATH_PROPERTY, previous);
        }
    }

    private static Path sandbox() throws Exception {
        return Files.createTempDirectory("geb-check");
    }

    private static File fixtureIn(Path dir) throws Exception {
        File f = new File(dir.toFile(), "extra_bodies.json");
        Files.write(f.toPath(), FIXTURE.getBytes("UTF-8"));
        return f;
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    private static int backupsIn(Path dir) {
        File[] all = dir.toFile().listFiles();
        if (all == null) return 0;
        int n = 0;
        for (File f : all) if (f.getName().contains(".bak-")) n++;
        return n;
    }

    private static File firstBackupIn(Path dir) {
        File[] all = dir.toFile().listFiles();
        if (all == null) return null;
        for (File f : all) if (f.getName().contains(".bak-")) return f;
        return null;
    }

    private static void clean(Path dir) {
        File[] all = dir.toFile().listFiles();
        if (all != null) for (File f : all) f.delete();
        dir.toFile().delete();
    }

    private static void yes(String label, boolean condition) {
        checks++;
        if (!condition) failures.add(label);
    }

    private static void is(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }
}
