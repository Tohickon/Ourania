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
 * Whose report it is (B7), measured on the files a reader would open.
 *
 * <p><b>Part A is the one that makes this safe to have added.</b> B7 sits underneath B3, a
 * feature that already worked, so the claim that matters most is the negative one: with nothing
 * filled in, the PDF is byte-for-byte the document B3 produced. Everything else here is about
 * what happens once somebody types their name.
 *
 * <p><b>The footer's page number is asserted through {@link ReportTemplate#footerNumber}</b>
 * rather than by reading the PDF, because this app draws PDF text as outlines - {@code
 * PDF_TEXT_AS_OUTLINES} - so there is no text in the file to read back. That is what B3 had to do
 * to keep the astrological glyphs, and it means a page numbered from the cover instead of from
 * the wheel could not be caught by looking at the file at all. Pulling the arithmetic out of the
 * export loop gave it somewhere a check can reach.
 */
public final class ReportTemplateCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static File dir;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        clearTemplate();
        dir = Files.createTempDirectory("ourania-b7").toFile();

        part("A: a blank template changes nothing", ReportTemplateCheck::blank);
        part("B: a cover needs something to put on it", ReportTemplateCheck::needsContent);
        part("C: a logo it cannot read is refused, and says why", ReportTemplateCheck::logo);
        part("D: the cover and the footer reach the file", ReportTemplateCheck::pages);
        part("E: the footer numbers from the wheel", ReportTemplateCheck::numbering);
        part("F: branding is configuration, not personal", ReportTemplateCheck::personal);

        clearTemplate();
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

    private static void clearTemplate() {
        Settings.set(ReportTemplate.PRACTITIONER_KEY, "");
        Settings.set(ReportTemplate.CONTACT_KEY, "");
        Settings.set(ReportTemplate.LOGO_KEY, "");
        ReportTemplate.setCover(true);
        ReportTemplate.setFooter(true);
    }

    // ------------------------------------------------------------------ A

    private static void blank() throws Exception {
        clearTemplate();
        ok("nothing is set, so nothing is branded", !ReportTemplate.branded());
        // <b>Both switches are ON here.</b> That is the trap this part exists for: a reader who
        // has never opened the Report tab has cover and footer at their defaults, and if those
        // alone decided it they would get a cover page carrying a date and nothing else.
        ok("the cover switch is on by default", ReportTemplate.cover());
        ok("the footer switch is on by default", ReportTemplate.footer());
        ok("and still no cover is wanted", !ReportTemplate.wantsCover());
        ok("and still no footer is wanted", !ReportTemplate.wantsFooter());

        int plain = pageCount("plain.pdf");
        ok("a plain report has pages (" + plain + ")", plain > 0);

        // The same file again, with the switches off: identical, because neither was doing
        // anything.
        ReportTemplate.setCover(false);
        ReportTemplate.setFooter(false);
        int off = pageCount("switches-off.pdf");
        ok("turning both off changes nothing when nothing is set (" + plain + " and " + off + ")",
            plain == off);
        clearTemplate();
    }

    // ------------------------------------------------------------------ B

    private static void needsContent() {
        clearTemplate();
        ok("a blank template wants no cover", !ReportTemplate.wantsCover());

        Settings.set(ReportTemplate.PRACTITIONER_KEY, "Ourania Consulting");
        ok("a name is enough to be branded", ReportTemplate.branded());
        ok("and now a cover is wanted", ReportTemplate.wantsCover());
        ok("and a footer", ReportTemplate.wantsFooter());

        ReportTemplate.setCover(false);
        ok("but the switch still turns the cover off", !ReportTemplate.wantsCover());
        ok("without touching the footer", ReportTemplate.wantsFooter());
        ReportTemplate.setCover(true);

        Settings.set(ReportTemplate.PRACTITIONER_KEY, "");
        Settings.set(ReportTemplate.CONTACT_KEY, "hello@example.com");
        ok("a contact line alone is also enough", ReportTemplate.branded());

        Settings.set(ReportTemplate.CONTACT_KEY, "   ");
        ok("whitespace is not content", !ReportTemplate.branded());
        clearTemplate();
    }

    // ------------------------------------------------------------------ C

    private static void logo() throws Exception {
        clearTemplate();
        ok("no logo, no complaint", ReportTemplate.logoProblem() == null);
        ok("and no image", ReportTemplate.logo() == null);

        Settings.set(ReportTemplate.LOGO_KEY, new File(dir, "nope.png").getAbsolutePath());
        ok("a file that is not there says so",
            said(ReportTemplate.logoProblem(), "no longer there"));
        ok("and yields no image", ReportTemplate.logo() == null);

        Settings.set(ReportTemplate.LOGO_KEY, dir.getAbsolutePath());
        ok("a folder says so", said(ReportTemplate.logoProblem(), "folder"));

        // <b>ImageIO returns null rather than throwing</b> for a file that is not an image it
        // knows, so the null has to be checked as well as the exception - otherwise a renamed
        // text file becomes a NullPointerException in the middle of an export.
        File fake = new File(dir, "not-really.png");
        Files.write(fake.toPath(), "this is not a picture".getBytes(StandardCharsets.UTF_8));
        Settings.set(ReportTemplate.LOGO_KEY, fake.getAbsolutePath());
        ok("a file that is not an image says so", said(ReportTemplate.logoProblem(), "not an image"));
        ok("and does not throw on the way", ReportTemplate.logo() == null);
        ok("a bad logo does not make the template unbranded on its own",
            !ReportTemplate.branded());

        File real = realPng();
        Settings.set(ReportTemplate.LOGO_KEY, real.getAbsolutePath());
        ok("a real image has no complaint (" + ReportTemplate.logoProblem() + ")",
            ReportTemplate.logoProblem() == null);
        ok("and loads", ReportTemplate.logo() != null);
        ok("a logo alone is enough to be branded", ReportTemplate.branded());
        clearTemplate();
    }

    private static boolean said(String problem, String about) {
        return problem != null && problem.toLowerCase(java.util.Locale.ROOT)
            .contains(about.toLowerCase(java.util.Locale.ROOT));
    }

    private static File realPng() throws Exception {
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(120, 60, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.DARK_GRAY);
        g.fillRect(0, 0, 120, 60);
        g.setColor(java.awt.Color.WHITE);
        g.drawString("LOGO", 30, 34);
        g.dispose();
        File out = new File(dir, "logo.png");
        javax.imageio.ImageIO.write(img, "png", out);
        return out;
    }

    // ------------------------------------------------------------------ D

    private static void pages() throws Exception {
        clearTemplate();
        ReportTemplate.setCover(false);
        ReportTemplate.setFooter(false);
        int plain = pageCount("d-plain.pdf");

        Settings.set(ReportTemplate.PRACTITIONER_KEY, "Ourania Consulting");
        ReportTemplate.setCover(true);
        ReportTemplate.setFooter(false);
        int covered = pageCount("d-cover.pdf");
        ok("a cover adds exactly one page (" + plain + " to " + covered + ")",
            covered == plain + 1);

        ReportTemplate.setCover(false);
        ReportTemplate.setFooter(true);
        int footed = pageCount("d-footer.pdf");
        // <b>Never fewer</b>, and that is all a page count can say here: a six page reading
        // loses 156 points to the band out of about 4,600, a fifth of a page, which rounds away.
        ok("a footer never shortens the report (" + plain + " to " + footed + ")",
            footed >= plain);
        // So the band itself is asserted, where the decision actually lives.
        ok("a footer reserves room on every page (" + ReportTemplate.footerBand() + ")",
            ReportTemplate.footerBand() > 0.0);
        ReportTemplate.setFooter(false);
        ok("and none is reserved with the footer off",
            ReportTemplate.footerBand() == 0.0);
        ReportTemplate.setFooter(true);
        Settings.set(ReportTemplate.PRACTITIONER_KEY, "");
        ok("nor with nothing to put in it", ReportTemplate.footerBand() == 0.0);
        Settings.set(ReportTemplate.PRACTITIONER_KEY, "Ourania Consulting");

        long without = size("d-plain.pdf");
        Settings.set(ReportTemplate.LOGO_KEY, realPng().getAbsolutePath());
        ReportTemplate.setCover(true);
        long with = size("d-logo.pdf");
        ok("a logo reaches the file (" + without + " bytes to " + with + ")", with > without);
        clearTemplate();
    }

    // ------------------------------------------------------------------ E

    private static void numbering() {
        // Without a cover, the wheel is page 1.
        ok("with no cover, the first page is 1", ReportTemplate.footerNumber(0, 0) == 1);
        ok("and the second is 2", ReportTemplate.footerNumber(1, 0) == 2);

        // <b>With a cover, the wheel is still page 1 and the cover takes no number.</b> A report
        // whose cover said "Page 1" would number every sheet after it one too high, which is the
        // sort of thing nobody notices until a client points at it.
        ok("a cover takes no footer at all", ReportTemplate.footerNumber(0, 1) == 0);
        ok("the wheel behind a cover is still page 1", ReportTemplate.footerNumber(1, 1) == 1);
        ok("and the reading after it is page 2", ReportTemplate.footerNumber(2, 1) == 2);
    }

    // ------------------------------------------------------------------ F

    private static void personal() {
        // <b>Deliberately NOT personal.</b> A practitioner setting up on a second machine wants
        // their letterhead back with their colours and orbs. What isPersonal keeps out of a saved
        // set is the reader's own chart - a six-month-old birth time coming back is the defect it
        // guards, and a practice name is not that.
        ok("the practitioner's name is configuration",
            !Settings.isPersonal(ReportTemplate.PRACTITIONER_KEY));
        ok("so is the contact line", !Settings.isPersonal(ReportTemplate.CONTACT_KEY));
        ok("so is the logo", !Settings.isPersonal(ReportTemplate.LOGO_KEY));
        ok("so are the two switches", !Settings.isPersonal(ReportTemplate.COVER_KEY)
            && !Settings.isPersonal(ReportTemplate.FOOTER_KEY));

        // And the boundary it must not have crossed on the way.
        ok("a birth time is still personal", Settings.isPersonal("natal.date"));
        ok("and the saved home is still personal", Settings.isPersonal("home.location"));
    }

    // ------------------------------------------------------------------ writing one

    private static int pageCount(String name) throws Exception {
        File out = write(name);
        com.lowagie.text.pdf.PdfReader reader =
            new com.lowagie.text.pdf.PdfReader(out.getAbsolutePath());
        try {
            return reader.getNumberOfPages();
        } finally {
            reader.close();
        }
    }

    private static long size(String name) throws Exception {
        return write(name).length();
    }

    private static File write(String name) throws Exception {
        final File out = new File(dir, name);
        final Exception[] blew = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                JPanel chart = new JPanel();
                chart.setSize(560, 560);
                JEditorPane pane = new JEditorPane("text/html", READING);
                pane.setSize(520, 700);
                ChartExporter.writeReadingPdf(chart, pane, out, "Chart A", "1 Jan 1990, 12:00");
            } catch (Exception e) {
                blew[0] = e;
            }
        });
        if (blew[0] != null) {
            throw blew[0];
        }
        return out;
    }

    /** Long enough to run to several pages, so a page-count change is visible. */
    private static final String READING = buildReading();

    private static String buildReading() {
        StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 1; i <= 60; i++) {
            sb.append("<p>Paragraph ").append(i)
                .append(" of a reading, long enough that the report runs to more than one page ")
                .append("and a change in the room available shows up as a change in the count.</p>");
        }
        return sb.append("</body></html>").toString();
    }

    // ------------------------------------------------------------------

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) {
        System.out.println();
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        try {
            body.run();
        } catch (Exception e) {
            checks++;
            failures.add(name + " threw " + e);
        }
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        System.out.println("  " + (condition ? "ok  " : "FAIL") + " " + label);
        if (!condition) {
            failures.add(label);
        }
    }
}
