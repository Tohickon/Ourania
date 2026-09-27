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
