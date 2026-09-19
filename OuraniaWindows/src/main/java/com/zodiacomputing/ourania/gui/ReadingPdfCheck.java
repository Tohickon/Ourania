package com.zodiacomputing.ourania.gui;

import java.awt.Component;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The PDF report (B3), after review.
 *
 * <p>Antigravity built the export on 2026-09-16. Reviewed the same day at David's request, it had
 * four defects, each measured on a real reading rather than inferred from the code:
 *
 * <ol>
 *   <li><b>Every glyph was dropped.</b> The PDF text path maps each font to Helvetica, which has
 *       no astrological symbols: the wheel page had no signs, blank planets and empty decan and
 *       bound rings, and the reading lost the ruler glyph beside each name.</li>
 *   <li><b>Every export ended on a blank page</b> - 7 pages for a 6-page report.</li>
 *   <li><b>The launcher had no lib\* on its classpath</b>, so the button threw
 *       NoClassDefFoundError, which a catch of Exception does not catch: it did nothing.</li>
 *   <li><b>The documented compile-in-place command could no longer compile the tree</b>, so the
 *       running app never had the feature - and would not have had anyone else's change.</li>
 * </ol>
 *
 * <p>This suite writes real PDFs through {@link ChartExporter#writeReadingPdf} and reads them back
 * with the same library, so each claim is about the file a reader would open.
 */
public final class ReadingPdfCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        // The half of Part C that runs in a JVM launched WITHOUT the jar: it prints the answer.
        if (args.length > 0 && "probe".equals(args[0])) {
            String p = ChartExporter.pdfLibraryProblem();
            System.out.println("PROBLEM=" + (p == null ? "none" : p));
            System.exit(0);
        }
        File dir = Files.createTempDirectory("ourania-pdfcheck").toFile();

        part("A: the glyphs reach the page", () -> glyphs(dir));

        final OuraniaWindow[] hold = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            Thread.sleep(5000);
            part("B: a real report, and no blank page", () -> report(hold[0], dir));
        } finally {
            SwingUtilities.invokeAndWait(() -> hold[0].dispose());
        }
        part("C: without the jar the export says so", ReadingPdfCheck::withoutJar);
        part("D: the launcher and the docs carry lib", ReadingPdfCheck::launcher);
        part("E: an empty reading is recognised as empty", ReadingPdfCheck::emptiness);

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

    // ------------------------------------------------------------------ A

    /**
     * The same one-line reading, once as twenty-two glyphs and once as twenty-two spaces.
     *
     * <b>Glyphs that reach the page are ink; glyphs that were dropped are nothing.</b> So the
     * glyph file has to be substantially larger than the blank one - drawn as outlines, each
     * symbol is a path. With the PDF text path the symbols vanish, the two files come out almost
     * the same size, and there is a Helvetica font in the file to say why.
     */
    private static void glyphs(File dir) throws Exception {
        String symbols = "♈♉♊♋♌♍♎♏♐♑♒♓"
            + "☉☽☿♀♂♃♄♅♆♇";
        StringBuilder blank = new StringBuilder();
        for (int i = 0; i < symbols.length(); i++) {
            blank.append(' ');
        }
        File withGlyphs = new File(dir, "glyphs.pdf");
        File withBlanks = new File(dir, "blanks.pdf");
        write(withGlyphs, "<html><body><p>x" + symbols + "x</p></body></html>");
        write(withBlanks, "<html><body><p>x" + blank + "x</p></body></html>");

        long g = withGlyphs.length();
        long b = withBlanks.length();
        System.out.printf("  22 glyphs: %d bytes; 22 spaces: %d bytes; difference %d%n", g, b, g - b);
        ok("twenty-two glyphs add real ink to the page: " + (g - b) + " bytes", g - b > 3000);

        String raw = new String(Files.readAllBytes(withGlyphs.toPath()), StandardCharsets.ISO_8859_1);
        ok("no standard font was used to stand in for the glyphs", !raw.contains("/BaseFont"));
        ok("the file says outlines are the setting", ChartExporter.PDF_TEXT_AS_OUTLINES);
    }

    /** A report whose wheel is a small blank panel and whose reading is the given HTML. */
    private static void write(File out, String html) throws Exception {
        final Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                JPanel chart = new JPanel();
                chart.setSize(200, 200);
                JEditorPane pane = new JEditorPane("text/html", html);
                pane.setSize(500, 200);
                ChartExporter.writeReadingPdf(chart, pane, out);
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
    }

    // ------------------------------------------------------------------ B

    /**
     * A real reading with a real wheel, read back page by page.
     *
     * <b>Each page places one template, and a blank page is a template with nothing in it.</b> So
     * every page is asked how much its templates actually draw; the blank trailing page the first
     * version produced would be a page drawing nothing, and so would a template placed before the
     * printable said whether its page existed.
     */
    private static void report(OuraniaWindow window, File dir) throws Exception {
        java.lang.reflect.Field fi = OuraniaWindow.class.getDeclaredField("interpretationPanel");
        fi.setAccessible(true);
        final InterpretationPanel panel = (InterpretationPanel) fi.get(window);
        java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
        fs.setAccessible(true);
        final SkymapPanel sky = (SkymapPanel) fs.get(window);
        SwingUtilities.invokeAndWait(() -> panel.showPlanetInterpretation(
            "Venus", "scorpio", 12, 2, 8, new ArrayList<>(), Double.NaN));
        Thread.sleep(1500);
        final Component chart = sky.chartComponent();
        SwingUtilities.invokeAndWait(() -> chart.setSize(900, 820));
        final JEditorPane pane = panel.getEditorPane();
        ok("the reading is there to export", ChartExporter.hasReading(pane));

        final File out = new File(dir, "report.pdf");
        final Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                ChartExporter.writeReadingPdf(chart, pane, out);
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        ok("a real report writes without error: " + failed[0], failed[0] == null);
        if (failed[0] != null) {
            return;
        }

        com.lowagie.text.pdf.PdfReader reader =
            new com.lowagie.text.pdf.PdfReader(out.getAbsolutePath());
        int pages = reader.getNumberOfPages();
        long[] drawn = new long[pages];
        for (int i = 1; i <= pages; i++) {
            drawn[i - 1] = templateBytes(reader, i);
        }
        reader.close();
        StringBuilder sizes = new StringBuilder();
        for (long d : drawn) {
            sizes.append(d).append(' ');
        }
        System.out.println("  " + pages + " pages; bytes drawn per page: " + sizes.toString().trim());

        ok("the report has the wheel and at least one page of reading", pages >= 2);
        for (int i = 0; i < pages; i++) {
            ok("page " + (i + 1) + " of " + pages + " draws something: " + drawn[i] + " bytes",
                drawn[i] > 2000);
        }
        // There was an assertion here that the wheel is the heaviest page. It is not, and the
        // first run said so: with every letter drawn as an outline, a page of reading is 600 to
        // 800 KB of paths against the wheel's 270 KB. It guarded none of the four defects - it
        // encoded a guess - so it was taken out rather than bent to pass.
        String raw = new String(Files.readAllBytes(out.toPath()), StandardCharsets.ISO_8859_1);
        ok("the real report uses no standard font either", !raw.contains("/BaseFont"));
    }

    /** Bytes of drawing in the form templates a page places. */
    private static long templateBytes(com.lowagie.text.pdf.PdfReader reader, int page)
            throws Exception {
        com.lowagie.text.pdf.PdfDictionary res = reader.getPageN(page)
            .getAsDict(com.lowagie.text.pdf.PdfName.RESOURCES);
        if (res == null) {
            return 0;
        }
        com.lowagie.text.pdf.PdfDictionary xo = res.getAsDict(com.lowagie.text.pdf.PdfName.XOBJECT);
        if (xo == null) {
            return 0;
        }
        long total = 0;
        for (Object k : xo.getKeys()) {
            com.lowagie.text.pdf.PdfObject o = com.lowagie.text.pdf.PdfReader.getPdfObject(
                xo.get((com.lowagie.text.pdf.PdfName) k));
            if (o instanceof com.lowagie.text.pdf.PRStream) {
                total += com.lowagie.text.pdf.PdfReader.getStreamBytes(
                    (com.lowagie.text.pdf.PRStream) o).length;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ C

    /**
     * The export asked from a JVM that has no jar, the way the old launcher started the app.
     *
     * The same classes, the same main, only the classpath differs - so what it prints is what the
     * reader's copy would have said, rather than what this JVM, which has the jar, would say.
     */
    private static void withoutJar() throws Exception {
        ok("with the jar present there is no problem to report",
            ChartExporter.pdfLibraryProblem() == null);

        String java = ProcessHandle.current().info().command().orElse("java");
        StringBuilder cp = new StringBuilder();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            String e = entry.toLowerCase();
            if (e.endsWith(".jar") || e.endsWith("*")) {
                continue;
            }
            if (cp.length() > 0) {
                cp.append(File.pathSeparator);
            }
            cp.append(entry);
        }
        Process p = new ProcessBuilder(java, "-cp", cp.toString(),
            ReadingPdfCheck.class.getName(), "probe").redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        String line = "";
        for (String l : out.split("\\R")) {
            if (l.startsWith("PROBLEM=")) {
                line = l.substring("PROBLEM=".length());
            }
        }
        System.out.println("  without the jar: " + line);
        ok("without the jar the export reports a problem instead of doing nothing",
            !line.isEmpty() && !"none".equals(line));
        ok("and the problem names the jar the reader is missing", line.contains("openpdf.jar"));
    }

    // ------------------------------------------------------------------ D

    private static void launcher() throws Exception {
        File bat = new File("../Run_Ourania.bat");
        ok("the launcher is where the check expects it", bat.isFile());
        if (bat.isFile()) {
            String javaLine = "";
            for (String l : Files.readAllLines(bat.toPath(), StandardCharsets.ISO_8859_1)) {
                if (l.contains("java.exe") && l.contains("-cp")) {
                    javaLine = l;
                }
            }
            ok("the launcher puts lib\\* on the classpath: " + javaLine.trim(),
                javaLine.contains("lib\\*"));
        }
        File doc = new File("CLAUDE.md");
        if (doc.isFile()) {
            String text = new String(Files.readAllBytes(doc.toPath()), StandardCharsets.UTF_8);
            ok("CLAUDE.md's run command carries lib\\*",
                text.contains("-cp \"src\\main\\java;lib\\*\" com.zodiacomputing"));
        }
    }

    // ------------------------------------------------------------------ E

    private static void emptiness() throws Exception {
        final boolean[] r = new boolean[3];
        SwingUtilities.invokeAndWait(() -> {
            JEditorPane empty = new JEditorPane("text/html", "");
            JEditorPane blankBody = new JEditorPane("text/html", "<html><body> </body></html>");
            JEditorPane full = new JEditorPane("text/html", "<html><body>Venus</body></html>");
            r[0] = ChartExporter.hasReading(empty);
            r[1] = ChartExporter.hasReading(blankBody);
            r[2] = ChartExporter.hasReading(full);
        });
        ok("an empty pane has no reading, though its HTML is not empty", !r[0]);
        ok("a body of whitespace has no reading", !r[1]);
        ok("a pane with words in it has a reading", r[2]);
        ok("no pane at all has no reading", !ChartExporter.hasReading(null));
    }

    // ------------------------------------------------------------------ harness

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private ReadingPdfCheck() { }
}
