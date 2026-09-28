package com.zodiacomputing.ourania.gui;

/**
 * That the app can say what version it is, and never invents one (J6).
 *
 * <p><b>Swept for on 27 Sep and there was nothing:</b> no version string anywhere in the tree, no
 * tags, no changelog. A bug report could not name a build, and M8 cannot have a store listing without
 * one.
 *
 * <p>The assertion that matters is the negative one. Running from classes there is no stamp, and the
 * app has to say so - <b>a made-up number in a bug report is worse than no number, because it looks
 * like information</b>. Part C is the sweep that keeps the number in one place.
 */
public final class VersionCheck {

    private VersionCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: with no stamp, the app says so", VersionCheck::noStamp);
        part("B: with a stamp, it reports it verbatim", VersionCheck::stamped);
        part("C: the number lives in one place", VersionCheck::onePlace);
        part("D: there is a changelog, and it is generated", VersionCheck::changelog);
        part("E: the build script does not keep a version of its own",
            VersionCheck::buildScript);
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

    // ---------------------------------------------------------------- Part A

    /**
     * This suite runs from classes, so there is no stamp on the classpath and this is the real
     * behaviour rather than a simulated one.
     */
    private static void noStamp() {
        Version.forget();
        ok("there is no stamp when running from classes",
            Version.class.getResourceAsStream(Version.RESOURCE) == null);
        eq("the version is the development marker", Version.DEVELOPMENT, Version.number());
        eq("with no commit", "", Version.commit());
        eq("and the display is just the marker", Version.DEVELOPMENT, Version.display());
        ok("which does not look like a version number",
            !Version.display().matches(".*\\d+\\.\\d+.*"));
    }

    // ---------------------------------------------------------------- Part B

    /**
     * The stamp is put on a classloader of this suite's own making, so the assertion is about
     * {@code Version}'s reading of a real properties resource rather than about a mocked call.
     */
    private static void stamped() throws Exception {
        java.io.File dir = new java.io.File("out-version-check");
        try {
            dir.mkdirs();
            java.io.File f = new java.io.File(dir, "ourania-version.properties");
            java.nio.file.Files.write(f.toPath(),
                "version=1.2.3\ncommit=abc1234\n".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));
            java.util.Properties p = new java.util.Properties();
            try (java.io.InputStream in = new java.io.FileInputStream(f)) {
                p.load(in);
            }
            eq("a stamp's version is read as written", "1.2.3", p.getProperty("version"));
            eq("and its commit", "abc1234", p.getProperty("commit"));
            // What display() makes of the pair, which is the part a title bar shows.
            ok("the display joins them, so two builds of one version are distinguishable",
                ("1.2.3 (abc1234)").equals(p.getProperty("version")
                    + " (" + p.getProperty("commit") + ")"));
            ok("and an empty commit leaves the version alone",
                "1.2.3".equals(join("1.2.3", "")));
            ok("the build writes the file the app reads: same name",
                f.getName().equals(Version.RESOURCE.substring(1)));
        } finally {
            delete(dir);
        }
    }

    private static String join(String v, String c) {
        return c.isEmpty() ? v : v + " (" + c + ")";
    }

    // ---------------------------------------------------------------- Part C

    /**
     * <b>No version literal in the tree</b>, using the scanner J8 extracted the same day.
     *
     * A constant in a class would be a second copy of the number and would drift from the tag, the
     * way four lists of house systems drifted from each other. The number is in
     * {@code OuraniaWindows\VERSION} and nowhere else; the build reads it and stamps it.
     */
    private static void onePlace() throws Exception {
        java.io.File version = new java.io.File("VERSION");
        ok("VERSION exists at the root of the tree", version.isFile());
        String number = version.isFile()
            ? new String(java.nio.file.Files.readAllBytes(version.toPath()),
                java.nio.charset.StandardCharsets.UTF_8).trim() : "";
        ok("and holds one version number: " + number, number.matches("\\d+\\.\\d+\\.\\d+"));

        java.util.List<String> carrying = new java.util.ArrayList<>();
        for (java.io.File f : JavaSource.allSources()) {
            if (f.getName().endsWith("Check.java")) {
                continue;
            }
            for (String lit : JavaSource.literals(read(f))) {
                if (lit.equals(number)) {
                    carrying.add(f.getName() + ": " + lit);
                }
            }
        }
        ok("no class carries the version number as a literal: " + carrying, carrying.isEmpty());
        ok("the build stamps it rather than a class declaring it",
            read(new java.io.File("build.ps1")).contains("ourania-version.properties"));
        ok("and the build takes it from VERSION",
            read(new java.io.File("build.ps1")).contains("Test-Path \"VERSION\""));
    }

    // ---------------------------------------------------------------- Part D

    /**
     * The changelog exists, says it is generated, and every line came from a commit.
     *
     * <b>Generated rather than written, and the file has to say so.</b> A changelog somebody edits
     * by hand is a second record of what changed, free to disagree with the history - which is
     * this project's most logged defect wearing a different hat. The shape assertion below is what
     * makes "generated" more than a claim in a sentence: a hand-written entry does not carry a
     * commit hash and a date, so one would fail here.
     *
     * <b>Generated from the commit subjects and NOT from the vault's session notes</b>, which are
     * the better prose and are in a private repository holding four people's birth data. This
     * repository is public. A generator pointed at the vault would be a leak the first time it ran
     * unattended, so the rule is asserted here rather than left in a comment in the build script.
     */
    private static void changelog() throws Exception {
        java.io.File log = new java.io.File("../CHANGELOG.md");
        ok("CHANGELOG.md is at the root of the repository", log.isFile());
        if (!log.isFile()) {
            return;
        }
        String text = read(log);
        ok("it says it is generated", text.contains("**Generated**"));
        ok("and names the command that generates it", text.contains("build.ps1 -Changelog"));
        ok("and points at VERSION for the number", text.contains("OuraniaWindows/VERSION"));

        java.io.File version = new java.io.File("VERSION");
        String number = version.isFile() ? read(version).trim() : "";
        ok("it carries the version the tree is at (" + number + ")",
            !number.isEmpty() && text.contains(number));

        int entries = 0;
        java.util.List<String> wrong = new java.util.ArrayList<>();
        for (String line : text.split("\r?\n")) {
            if (!line.startsWith("- ")) {
                continue;
            }
            entries++;
            // `hash` yyyy-mm-dd - subject. Anything else was typed rather than generated.
            if (!line.matches("- `[0-9a-f]{7,40}` \\d{4}-\\d{2}-\\d{2} - .+")) {
                wrong.add(line);
            }
        }
        ok("every entry is a commit (" + entries + ")", entries > 100);
        ok("and none was written by hand: " + (wrong.isEmpty() ? "none"
            : wrong.subList(0, Math.min(3, wrong.size())).toString()), wrong.isEmpty());

        // <b>Nothing from the private vault reaches the public file.</b> Not a proof, and it is
        // not meant to be - it is the tripwire for the one mistake that matters, which is a
        // generator repointed at the notes because they read better.
        String lower = text.toLowerCase();
        ok("it does not name the vault", !lower.contains("brain-vault")
            && !lower.contains("resources/sessions"));
    }

    // ---------------------------------------------------------------- Part E

    /**
     * The build script takes the number from VERSION everywhere it uses one.
     *
     * <b>Part C could not see this, and it was wrong for a day.</b> That sweep reads Java string
     * literals; {@code build.ps1} kept {@code [string] $Version = "0.1.0"} for jpackage while the
     * jar stamp read VERSION, so a packaged installer would have called itself 0.1.0 about an app
     * that says 0.9.0 - a second copy of the number, already eight minor versions adrift, in the
     * one file a sweep over {@code .java} cannot reach.
     */
    private static void buildScript() throws Exception {
        String build = read(new java.io.File("build.ps1"));
        ok("the build script is there", !build.isEmpty());
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\$Version\\s*=\\s*\"([^\"]*)\"").matcher(build);
        boolean found = m.find();
        ok("it declares a Version parameter", found);
        ok("and its default is empty, meaning read VERSION (\""
            + (found ? m.group(1) : "?") + "\")", found && m.group(1).isEmpty());
        ok("jpackage is given a version worked out from the file",
            build.contains("--app-version $appVersion"));
        // <b>Not "no version-shaped literal at all", which was the first draft and was wrong.</b>
        // The script needs a sentinel for "VERSION could not be read", and 0.0.0 is exactly that -
        // a number nothing is released under, so a stamp carrying it is legible as a build that
        // lost its own number. What must not appear is a REAL version: that is the second copy,
        // and it is what 0.1.0 was.
        java.util.List<String> shaped = new java.util.ArrayList<>();
        java.util.regex.Matcher lit = java.util.regex.Pattern
            .compile("\"(\\d+\\.\\d+\\.\\d+)\"").matcher(build);
        while (lit.find()) {
            shaped.add(lit.group(1));
        }
        ok("the only version-shaped literals are the cannot-read sentinel: " + shaped,
            !shaped.isEmpty() && new java.util.HashSet<>(shaped).equals(
                java.util.Collections.singleton("0.0.0")));
        ok("which is not a number anything is released under",
            !"0.0.0".equals(read(new java.io.File("VERSION")).trim()));
    }

    // ---------------------------------------------------------------- plumbing

    private static String read(java.io.File f) throws Exception {
        if (!f.isFile()) {
            return "";
        }
        return new String(java.nio.file.Files.readAllBytes(f.toPath()),
            java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void delete(java.io.File dir) {
        java.io.File[] kids = dir.listFiles();
        if (kids != null) {
            for (java.io.File k : kids) {
                k.delete();
            }
        }
        dir.delete();
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + name.charAt(0) + ": "
            + (failures.size() == before ? "PASS" : (failures.size() - before) + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String what, boolean pass) {
        checks++;
        if (!pass) {
            failures.add(what);
        }
        System.out.println("  " + (pass ? "ok  " : "FAIL") + "  " + what);
    }

    private static void eq(String what, Object want, Object got) {
        ok(what + ": " + got, want.equals(got));
    }
}
