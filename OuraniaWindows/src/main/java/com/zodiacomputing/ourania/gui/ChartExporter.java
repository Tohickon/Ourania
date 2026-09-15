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
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

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

    /** Multipliers offered for a saved image. Screen, then two for print and for a page. */
    static final int[] SCALES = {1, 2, 4};

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
        int w = Math.max(1, chart.getWidth()) * scale;
        int h = Math.max(1, chart.getHeight()) * scale;
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
        Object[] options = {"1x (screen)", "2x", "4x (print)"};
        int pick = JOptionPane.showOptionDialog(parent, "Resolution for the saved image:",
            "Save chart image", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
            null, options, options[1]);
        if (pick < 0) {
            return;
        }
        File file = chooseSaveFile(parent, "ourania-chart.png", "PNG image", "png");
        if (file == null) {
            return;
        }
        try {
            ImageIO.write(render(chart, SCALES[pick]), "png", file);
        } catch (IOException e) {
            warn(parent, "Could not write the image: " + e.getMessage());
        }
    }

    /** B4b. The wheel to the clipboard, at screen size. */
    static void copyChartImage(Component parent, Component chart) {
        if (chart == null || chart.getWidth() <= 0) {
            warn(parent, "The chart has not been drawn yet.");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new ImageSelection(render(chart, 1)), null);
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
