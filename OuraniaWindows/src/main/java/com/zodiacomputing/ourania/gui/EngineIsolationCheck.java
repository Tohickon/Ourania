package com.zodiacomputing.ourania.gui;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The engine stands on its own: it can be built without the desktop app (M1).
 *
 * <p><b>Why this is a check and not a comment.</b> The phone app (M1-M8) builds from the same
 * engine as the desktop, never a copied tree. Measured on 30 Sep, the engine was one reference
 * away from that: four uses of {@code SkymapPanel} - three colour lookups in {@code Prose} and
 * {@code SkymapPanel.YearScan} in {@code NarrativeSynthesizer} - and through them the engine's
 * reach closed over 81 desktop classes. Those were moved ({@code PlainText}, {@code YearScan}).
 * Nothing stops the next one being added in an afternoon and noticed in a month, which is the
 * gap this closes.
 *
 * <ul>
 *   <li><b>Part A</b> - no engine source uses Swing, AWT or ImageIO, which Android does not
 *       have. Read with comments and literals stripped: {@code SweDate} mentions
 *       {@code java.awt.Calendar} four times, in comments.</li>
 *   <li><b>Part B</b> - every desktop-package class an engine source names is itself one of
 *       the engine's shared classes, listed below by hand so adding one is a visible act.</li>
 *   <li><b>Part C</b> - the engine compiles alone, at {@code --release 17}, with nothing on
 *       the source path. This is the one that cannot be argued with: A and B are sweeps and a
 *       sweep can miss a reference; the compiler cannot.</li>
 * </ul>
 *
 * <p>The engine is every non-check source in {@code astro} and the vendored Swiss Ephemeris,
 * plus {@link #SHARED}. The checks themselves are left out: they are tests, and several build
 * windows on purpose.
 */
public final class EngineIsolationCheck {

    /**
     * The desktop-package classes the engine may use. Each is itself clean - Part B holds
     * them to the same rule - and each is here because an engine source or the reading
     * needs it: settings and the chart book, the interpretation corpus and its loader, the
     * error log, the synthesized reading, and the offline atlas the phone's place search reads (M3).
     */
    static final Set<String> SHARED = new TreeSet<>(List.of(
        "Settings", "SavedCharts", "InterpretationService", "ErrorLog", "Json",
        "NarrativeSynthesizer", "Atlas"));

    /** The Java level Android builds accept. */
    static final String RELEASE = "17";

    private static final String ROOT = "src/main/java";
    private static final Pattern DESKTOP =
        Pattern.compile("\\b(java\\.awt|javax\\.swing|javax\\.imageio)\\b");

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        List<File> engine = engineSources();
        System.out.println("engine: " + engine.size() + " sources");
        // 116 on 30 Sep: 61 in astro, 49 in the Swiss Ephemeris, 6 shared. A floor rather than
        // the number, so adding an engine class does not turn this red.
        yes("the engine was found - " + engine.size() + " sources", engine.size() >= 100);

        System.out.println("=== Part A: no desktop library ===");
        for (File f : engine) {
            String code = JavaSource.codeOnly(read(f));
            yes(f.getName() + " uses no Swing, AWT or ImageIO", !DESKTOP.matcher(code).find());
        }

        System.out.println("=== Part B: only the shared desktop classes ===");
        Set<String> desktop = new TreeSet<>();
        for (File f : JavaSource.guiSources()) {
            desktop.add(f.getName().replace(".java", ""));
        }
        for (String name : SHARED) {
            yes(name + " is a class in the desktop package", desktop.contains(name));
        }
        for (File f : engine) {
            String code = JavaSource.codeOnly(read(f));
            // A name the file declares itself is its own: ThemeConvergence has an enum called
            // Theme, which is not the desktop's colour theme.
            Set<String> own = new TreeSet<>();
            java.util.regex.Matcher decl = Pattern.compile(
                "\\b(?:class|enum|interface|record)\\s+(\\w+)").matcher(code);
            while (decl.find()) {
                own.add(decl.group(1));
            }
            for (String name : desktop) {
                if (own.contains(name) || SHARED.contains(name)) {
                    continue;
                }
                if (Pattern.compile("\\b" + name + "\\b").matcher(code).find()) {
                    yes(f.getName() + " names desktop class " + name
                        + ", which is not one of the engine's shared classes", false);
                }
            }
        }

        System.out.println("=== Part C: it compiles alone, at --release " + RELEASE + " ===");
        compilesAlone(engine);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    /** The engine's sources: astro and the Swiss Ephemeris without their checks, and SHARED. */
    static List<File> engineSources() {
        List<File> out = new ArrayList<>();
        for (String dir : new String[] {"com/zodiacomputing/ourania/astro", "de/thmac/swisseph"}) {
            File[] files = new File(ROOT, dir).listFiles((d, n) -> n.endsWith(".java"));
            if (files == null) {
                continue;
            }
            for (File f : files) {
                String n = f.getName();
                if (!n.endsWith("Check.java") && !n.endsWith("SelfTest.java")) {
                    out.add(f);
                }
            }
        }
        for (String name : SHARED) {
            out.add(new File(ROOT, "com/zodiacomputing/ourania/gui/" + name + ".java"));
        }
        out.sort(java.util.Comparator.comparing(File::getPath));
        return out;
    }

    private static void compilesAlone(List<File> engine) throws Exception {
        javax.tools.JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            // A runtime without a compiler cannot answer, and saying so is not a pass.
            yes("a Java compiler is available to build the engine alone", false);
            return;
        }
        File out = Files.createTempDirectory("ourania-engine").toFile();
        javax.tools.DiagnosticCollector<javax.tools.JavaFileObject> diags =
            new javax.tools.DiagnosticCollector<>();
        try (javax.tools.StandardJavaFileManager fm =
                 javac.getStandardFileManager(diags, null, StandardCharsets.UTF_8)) {
            List<String> options = List.of("--release", RELEASE, "-encoding", "UTF-8",
                "-nowarn", "-implicit:none", "-sourcepath", "", "-classpath", "",
                "-d", out.getPath());
            Boolean ok = javac.getTask(null, fm, diags, options, null,
                fm.getJavaFileObjectsFromFiles(engine)).call();
            List<String> errors = new ArrayList<>();
            for (javax.tools.Diagnostic<?> d : diags.getDiagnostics()) {
                if (d.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
                    Object src = d.getSource();
                    String where = src instanceof javax.tools.JavaFileObject
                        ? new File(((javax.tools.JavaFileObject) src).getName()).getName() : "?";
                    errors.add(where + ":" + d.getLineNumber() + " " + d.getMessage(null));
                }
            }
            for (String e : errors.subList(0, Math.min(10, errors.size()))) {
                System.out.println("  " + e);
            }
            yes("the engine compiles on its own at --release " + RELEASE + ": "
                + errors.size() + " errors" + (errors.isEmpty() ? "" : ", first " + errors.get(0)),
                Boolean.TRUE.equals(ok) && errors.isEmpty());
        } finally {
            deleteTree(out);
        }
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteTree(k);
            }
        }
        f.delete();
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }
}
