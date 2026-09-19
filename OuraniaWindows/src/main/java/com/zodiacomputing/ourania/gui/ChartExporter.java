package com.zodiacomputing.ourania.gui;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Everything that lets a chart leave the application.
 *
 * <b>Nothing did, before this.</b> An audit on 2 Sep 2026 looked for ImageIO.write, Printable
 * and any PDF path in this package and found none of the three: a corpus of 600,000
 * interpretations, a wheel a reader spends an hour on, and no way to keep any of it. That caps
 * what the app is for regardless of how good the engine is.
 *
 * The wheel is painted into an image rather than screen-grabbed, so the output does not depend
 * on the window being visible, unobscured, or on this machine's scaling. Everything here takes
 * a Component and asks it to paint - the same call the printer path uses.
 */
public final class ChartExporter {

    /**
     * Multipliers offered for a saved image: the screen's own, then 2x and 4x for print, once
     * each and in order.
     *
     * <b>"Screen" is the screen's scale, not 1.</b> The first option read "1x (screen)", which was
     * true only at 100% Windows scaling; at David's 300% it saved a picture a third the size of
     * the wheel on screen. See HiDpi.
     */
    static double[] scales(double screen) {
        java.util.TreeSet<Double> set = new java.util.TreeSet<>();
        set.add(HiDpi.round(screen));
        set.add(2.0);
        set.add(4.0);
        double[] out = new double[set.size()];
        int i = 0;
        for (double d : set) {
            out[i++] = d;
        }
        return out;
    }

    private ChartExporter() { }

    // ------------------------------------------------------------------ images

    /**
     * Paints a component into an image at a multiple of its on-screen size.
     *
     * <b>Scaling is applied to the graphics, not to the finished bitmap.</b> Painting at 1x and
     * enlarging gives soft glyphs and stepped aspect lines; scaling the Graphics2D first makes
     * the wheel redraw at the larger size, so the text and the strokes are sharp at 4x.
     */
    static BufferedImage render(Component chart, int scale) {
        return render(chart, (double) scale);
    }

    static BufferedImage render(Component chart, double scale) {
        int w = (int) Math.round(Math.max(1, chart.getWidth()) * scale);
        int h = (int) Math.round(Math.max(1, chart.getHeight()) * scale);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(scale, scale);
        paintWhole(chart, g);
        g.dispose();
        return img;
    }

    /** Client property a chart reads to draw the whole wheel whatever the screen is zoomed to. */
    static final String EXPORTING = "ourania.exporting";

    /**
     * Paints the chart as a saved image or a print: all of it.
     *
     * <b>The screen's zoom is the reader's view, not the chart.</b> A PNG saved while zoomed in
     * on one sign would be a picture of one sign. Marked on the component for the length of the
     * paint rather than detected from the Graphics, because Swing's own screen painting goes
     * through offscreen buffers too and a device test cannot reliably tell the two apart.
     */
    static void paintWhole(Component chart, Graphics2D g) {
        javax.swing.JComponent jc = chart instanceof javax.swing.JComponent
            ? (javax.swing.JComponent) chart : null;
        if (jc != null) {
            jc.putClientProperty(EXPORTING, Boolean.TRUE);
        }
        try {
            chart.paint(g);
        } finally {
            if (jc != null) {
                jc.putClientProperty(EXPORTING, null);
            }
        }
    }

    /** B1. Saves the wheel as a PNG, at a scale the reader picks. */
    static void saveChartImage(Component parent, Component chart) {
        if (chart == null || chart.getWidth() <= 0) {
            warn(parent, "The chart has not been drawn yet.");
            return;
        }
        double screen = HiDpi.scale(chart);
        double[] scales = scales(screen);
        Object[] options = new Object[scales.length];
        int preferred = 0;
        for (int i = 0; i < scales.length; i++) {
            String tag = scales[i] == HiDpi.round(screen) ? " (as on screen)" : scales[i] == 4.0 ? " (print)" : "";
            options[i] = HiDpi.label(scales[i]) + tag;
            if (scales[i] == HiDpi.round(screen)) {
                preferred = i;
            }
        }
        int pick = JOptionPane.showOptionDialog(parent, "Resolution for the saved image:",
            "Save chart image", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
            null, options, options[preferred]);
        if (pick < 0) {
            return;
        }
        File file = chooseSaveFile(parent, "ourania-chart.png", "PNG image", "png");
        if (file == null) {
            return;
        }
        try {
            ImageIO.write(render(chart, scales[pick]), "png", file);
        } catch (IOException e) {
            warn(parent, "Could not write the image: " + e.getMessage());
        }
    }

    /** B4b. The wheel to the clipboard, at the size it is on screen - in real pixels. */
    static void copyChartImage(Component parent, Component chart) {
        if (chart == null || chart.getWidth() <= 0) {
            warn(parent, "The chart has not been drawn yet.");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new ImageSelection(render(chart, HiDpi.scale(chart))), null);
    }

    /** An image on the clipboard. Swing ships no Transferable for one. */
    private static final class ImageSelection implements Transferable {
        private final BufferedImage image;

        ImageSelection(BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            return this.image;
        }
    }

    // ----------------------------------------------------------------- printing

    /**
     * B2. Prints the wheel, fitted to the page and centred.
     *
     * Scaled to the smaller of the two page ratios so a square wheel stays square - fitting
     * width and height independently would print an oval, which on a chart is not a cosmetic
     * problem: the aspect angles would be wrong on paper.
     */
    static void printChart(Component parent, final Component chart) {
        if (chart == null || chart.getWidth() <= 0) {
            warn(parent, "The chart has not been drawn yet.");
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Ourania chart");
        job.setPrintable(new Printable() {
            @Override
            public int print(java.awt.Graphics g, PageFormat page, int index) {
                if (index > 0) {
                    return Printable.NO_SUCH_PAGE;
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(page.getImageableX(), page.getImageableY());
                double sx = page.getImageableWidth() / chart.getWidth();
                double sy = page.getImageableHeight() / chart.getHeight();
                double s = Math.min(sx, sy);
                g2.translate((page.getImageableWidth() - chart.getWidth() * s) / 2.0,
                             (page.getImageableHeight() - chart.getHeight() * s) / 2.0);
                g2.scale(s, s);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                paintWhole(chart, g2);
                return Printable.PAGE_EXISTS;
            }
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                warn(parent, "Could not print: " + e.getMessage());
            }
        }
    }

    /**
     * The chart wheel on the first page, then the reading paginated after it.
     *
     * Added by Antigravity on 2026-09-16 for B3. Page 0 fits the wheel into the imageable area;
     * every later page delegates to the editor pane's own printable, which lays the reading out
     * at the page's width.
     */
    private static class ReportPrintable implements Printable {
        private final Component chart;
        private final Printable textPrintable;

        ReportPrintable(Component chart, JEditorPane editorPane) {
            this.chart = chart;
            this.textPrintable = editorPane.getPrintable(null, null);
        }

        @Override
        public int print(java.awt.Graphics g, PageFormat page, int index) throws PrinterException {
            if (index == 0) {
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(page.getImageableX(), page.getImageableY());
                double sx = page.getImageableWidth() / chart.getWidth();
                double sy = page.getImageableHeight() / chart.getHeight();
                double s = Math.min(sx, sy);
                g2.translate((page.getImageableWidth() - chart.getWidth() * s) / 2.0,
                             (page.getImageableHeight() - chart.getHeight() * s) / 2.0);
                g2.scale(s, s);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                paintWhole(chart, g2);
                return Printable.PAGE_EXISTS;
            } else {
                return textPrintable.print(g, page, index - 1);
            }
        }
    }

    /** True when the pane holds any reading text, rather than the empty skeleton of one. */
    static boolean hasReading(JEditorPane editorPane) {
        if (editorPane == null) {
            return false;
        }
        // <b>Not getText().isEmpty().</b> An HTML pane with nothing in it still returns
        // "<html><head></head><body></body></html>", so that test was never true and an empty
        // reading would have exported as a wheel with nothing after it. The document's own text
        // is the honest answer.
        javax.swing.text.Document doc = editorPane.getDocument();
        try {
            return !doc.getText(0, doc.getLength()).trim().isEmpty();
        } catch (javax.swing.text.BadLocationException e) {
            return false;
        }
    }

    /**
     * Prints the chart wheel followed by the reading, through the system print dialog.
     *
     * <b>This is the paper half of B3, not B7.</b> It arrived labelled B7, which on the master
     * list is report templates and branding. The printer path is Java2D end to end, so the glyphs
     * survive here without the outline treatment the PDF needs.
     */
    static void printReading(Component parent, final Component chart, final JEditorPane editorPane) {
        if (!hasReading(editorPane)) {
            warn(parent, "There is no reading to print.");
            return;
        }
        if (chart == null || chart.getWidth() <= 0) {
            warn(parent, "The chart has not been drawn yet.");
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Ourania Report");
        job.setPrintable(new ReportPrintable(chart, editorPane));
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                warn(parent, "Could not print: " + e.getMessage());
            }
        }
    }

    /**
     * Whether the report's pages are drawn into the PDF as outlines rather than as PDF text.
     *
     * <b>Outlines, because PDF text lost every glyph.</b> The PDF's own text path maps each AWT
     * font to one standard face - Helvetica - which has no astrological symbols, so they were
     * dropped without a trace. Measured 2026-09-16 on a real reading: the wheel page had no sign
     * glyph, no planet glyph and no decan or bound glyph, and the reading lost the ruler glyph
     * beside each name. Outlines are Java's own rendering of the same composite font the screen
     * uses, so the page shows what the reader saw.
     *
     * The cost, also measured, on a six-page reading: 1.38 MB rather than 250 KB, and text that
     * cannot be selected or searched. False keeps selectable text and loses the glyphs again.
     */
    static final boolean PDF_TEXT_AS_OUTLINES = true;

    /** A4 in PDF points, and the half-inch margin the report is laid out inside. */
    static final float PDF_PAGE_W = 595.0f;
    static final float PDF_PAGE_H = 842.0f;
    private static final double PDF_MARGIN = 36.0;

    /**
     * Why this JVM cannot write a PDF, or null when it can.
     *
     * <b>The library is a jar in lib/, and the app was launched without it.</b> Run_Ourania.bat
     * started the app with only src/main/java on the classpath, so the first PDF class touched
     * threw NoClassDefFoundError - an Error, which a catch of Exception lets through, so the
     * button did nothing at all. Asked up front instead, and answered in words.
     */
    static String pdfLibraryProblem() {
        try {
            Class.forName("com.lowagie.text.Document", false, ChartExporter.class.getClassLoader());
            return null;
        } catch (ClassNotFoundException | LinkageError e) {
            return "The PDF library could not be found. Ourania needs lib\\openpdf.jar on its "
                + "classpath - start it with Run_Ourania.bat, which includes lib\\*.";
        }
    }

    /** B3. Saves the chart wheel and the reading as a PDF, through a save dialog. */
    static void saveReadingPdf(Component parent, Component chart, JEditorPane editorPane) {
        if (!hasReading(editorPane)) {
            warn(parent, "There is no reading to save.");
            return;
        }
        if (chart == null || chart.getWidth() <= 0) {
            warn(parent, "The chart has not been drawn yet.");
            return;
        }
        String problem = pdfLibraryProblem();
        if (problem != null) {
            warn(parent, problem);
            return;
        }
        File file = chooseSaveFile(parent, "ourania-report.pdf", "PDF document", "pdf");
        if (file == null) {
            return;
        }
        try {
            writeReadingPdf(chart, editorPane, file);
        } catch (Exception | LinkageError e) {
            // A PDF that failed half way is not a shorter PDF, it is a broken file under the name
            // the reader chose; leaving it behind would look like success.
            file.delete();
            warn(parent, "Could not write the PDF: " + e.getMessage());
        }
    }

    /**
     * The report as a PDF file: the wheel, then the reading, and never a blank page.
     *
     * <b>Each page is drawn into a template and placed only if it exists.</b> The first version
     * opened a new page before asking the printable whether there was one, so every export ended
     * on an empty page - 7 pages for a 6-page report. Asking first with a throwaway graphics was
     * tried and rejected: the reading's layout is fixed by whichever graphics lays it out first,
     * and a 1-pixel image's font metrics ran words into each other wherever the text colour
     * changed. A template is laid out by the PDF's own graphics and simply not placed when the
     * printable says the page does not exist.
     *
     * Split from the dialog so it can be driven directly, by a check or by anything else.
     */
    static void writeReadingPdf(Component chart, JEditorPane editorPane, File file)
            throws Exception {
        ReportPrintable printable = new ReportPrintable(chart, editorPane);
        PageFormat pf = new PageFormat();
        java.awt.print.Paper paper = new java.awt.print.Paper();
        paper.setSize(PDF_PAGE_W, PDF_PAGE_H);
        paper.setImageableArea(PDF_MARGIN, PDF_MARGIN,
            PDF_PAGE_W - 2 * PDF_MARGIN, PDF_PAGE_H - 2 * PDF_MARGIN);
        pf.setPaper(paper);

        Document document = new Document(new com.lowagie.text.Rectangle(PDF_PAGE_W, PDF_PAGE_H));
        try (java.io.OutputStream out = new java.io.FileOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            for (int page = 0; ; page++) {
                PdfTemplate template = cb.createTemplate(PDF_PAGE_W, PDF_PAGE_H);
                Graphics2D g2 = PDF_TEXT_AS_OUTLINES
                    ? template.createGraphicsShapes(PDF_PAGE_W, PDF_PAGE_H)
                    : template.createGraphics(PDF_PAGE_W, PDF_PAGE_H);
                int drawn;
                try {
                    drawn = printable.print(g2, pf, page);
                } finally {
                    g2.dispose();
                }
                if (drawn == Printable.NO_SUCH_PAGE) {
                    break;
                }
                if (page > 0) {
                    document.newPage();
                }
                cb.addTemplate(template, 0, 0);
            }
            document.close();
        }
    }

    // -------------------------------------------------------------------- text

    /** B4a. Any text to the clipboard. */
    static void copyText(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(text == null ? "" : text), null);
    }

    /**
     * B5 and B6. Writes text to a file the reader picks.
     *
     * UTF-8 explicitly. The default encoding on this machine is not UTF-8, and a chart is all
     * degree signs and astrological glyphs - the one kind of document where the platform
     * default silently ruins every line.
     */
    static void saveText(Component parent, String content, String suggested,
                         String description, String extension) {
        if (content == null || content.isEmpty()) {
            warn(parent, "There is nothing to save yet.");
            return;
        }
        File file = chooseSaveFile(parent, suggested, description, extension);
        if (file == null) {
            return;
        }
        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            warn(parent, "Could not write the file: " + e.getMessage());
        }
    }

    /**
     * Strips tags for a plain-text export, keeping the line breaks the markup implied.
     *
     * Not a general HTML parser and not trying to be. The documents it is given are written by
     * this app, so the only structure it has to honour is what those writers emit.
     */
    static String toPlainText(String html) {
        if (html == null) {
            return "";
        }
        String s = html.replaceAll("(?i)<br\s*/?>", "\n")
                       .replaceAll("(?i)</(p|div|tr|h1|h2|h3)>", "\n")
                       .replaceAll("(?i)</t[dh]>", "\t");
        StringBuilder out = new StringBuilder();
        boolean inTag = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                inTag = true;
            } else if (c == '>') {
                inTag = false;
            } else if (!inTag) {
                out.append(c);
            }
        }
        return out.toString()
            .replace("&nbsp;", " ").replace("&middot;", "-").replace("&deg;", " deg")
            .replace("&mdash;", "-").replace("&ndash;", "-").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"").replace("&#9776;", "")
            .replaceAll("[ \t]+\n", "\n").replaceAll("\n{3,}", "\n\n").trim();
    }

    // ------------------------------------------------------------------ shared

    private static File chooseSaveFile(Component parent, String suggested,
                                       String description, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save");
        chooser.setSelectedFile(new File(suggested));
        chooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith("." + extension)) {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        if (file.exists() && JOptionPane.showConfirmDialog(parent,
                file.getName() + " already exists. Replace it?", "Replace file",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return null;
        }
        return file;
    }

    private static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Ourania",
            JOptionPane.INFORMATION_MESSAGE);
    }
}
