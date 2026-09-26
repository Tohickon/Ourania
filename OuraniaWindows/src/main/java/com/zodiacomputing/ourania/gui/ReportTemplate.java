package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.File;

/**
 * Whose report this is (B7).
 *
 * <p><b>A reading a practitioner gives a client should have their name on it, not this app's.</b>
 * Until now the PDF opened on the wheel and ended on the last line of the reading, with nothing
 * anywhere saying who prepared it. B3 built the document; this makes it theirs.
 *
 * <p><b>Branding is configuration, not personal data</b>, so these keys are deliberately outside
 * {@code Settings.isPersonal} and travel in a saved settings set. A practitioner setting up on a
 * second machine wants their letterhead to come back with their colours and orbs. What
 * {@code isPersonal} exists to keep out of a set is the reader's own chart - a birth time coming
 * back from a six-month-old file is the defect it guards, and a practice name is not that.
 *
 * <p><b>Everything here is optional and the report is correct without any of it.</b> A blank
 * template produces exactly the document B3 produced, which is what makes this safe to add
 * underneath an existing feature: {@link #branded()} is false until somebody types something, and
 * until then no cover page, no footer and no logo are drawn.
 */
public final class ReportTemplate {

    public static final String PRACTITIONER_KEY = "report.practitioner";
    public static final String CONTACT_KEY = "report.contact";
    public static final String LOGO_KEY = "report.logo";
    public static final String COVER_KEY = "report.cover";
    public static final String FOOTER_KEY = "report.footer";

    /**
     * Points reserved at the foot of every page when a footer is drawn.
     *
     * <b>Taken out of the imageable area rather than drawn over it.</b> The reading is laid out
     * by {@code JEditorPane}'s own printable against whatever height it is given; drawing a
     * footer into that height puts the footer through the last line of text on every page. A
     * shorter page means one or two more pages in a long report, which is the correct cost.
     */
    public static final double FOOTER_BAND = 26.0;

    /** How big the logo may be on a cover, in points. */
    private static final double LOGO_MAX_W = 180.0;
    private static final double LOGO_MAX_H = 90.0;

    private static final Color INK = new Color(20, 20, 20);
    private static final Color FAINT = new Color(120, 120, 120);
    private static final Color RULE = new Color(180, 180, 180);

    private ReportTemplate() { }

    // ------------------------------------------------------------------ what is set

    public static String practitioner() {
        return Settings.get(PRACTITIONER_KEY, "").trim();
    }

    public static String contact() {
        return Settings.get(CONTACT_KEY, "").trim();
    }

    public static String logoPath() {
        return Settings.get(LOGO_KEY, "").trim();
    }

    // The project's idiom for a boolean setting: absent means the default, and only the exact
    // string "false" turns it off. See Settings.globeHouseFill.
    public static boolean cover() {
        return !"false".equals(Settings.get(COVER_KEY, "true"));
    }

    public static boolean footer() {
        return !"false".equals(Settings.get(FOOTER_KEY, "true"));
    }

    public static void setCover(boolean on) {
        Settings.set(COVER_KEY, on ? "true" : "false");
    }

    public static void setFooter(boolean on) {
        Settings.set(FOOTER_KEY, on ? "true" : "false");
    }

    /**
     * True once there is anything to put on a report.
     *
     * <b>The two switches are not enough on their own.</b> Both default to on, so a reader who
     * has never opened the Report tab would otherwise get a cover page carrying nothing but a
     * date and a footer reading "Page 1" - worse than the plain document they had before. A name,
     * a contact line or a logo is what turns them on in practice.
     */
    public static boolean branded() {
        return !practitioner().isEmpty() || !contact().isEmpty() || logo() != null;
    }

    public static boolean wantsCover() {
        return cover() && branded();
    }

    public static boolean wantsFooter() {
        return footer() && branded();
    }

    // ------------------------------------------------------------------ the logo

    /**
     * The logo, or null.
     *
     * <b>Read every time rather than cached.</b> A practitioner who fixes a wrong path, or edits
     * the image, should get the new one on the next export without restarting; and a cached image
     * of a file that has since been deleted would keep printing on reports after the reason for
     * it was gone. Reports are not drawn often enough for the read to matter.
     */
    public static Image logo() {
        String path = logoPath();
        if (path.isEmpty()) {
            return null;
        }
        File f = new File(path);
        if (!f.isFile() || !f.canRead()) {
            return null;
        }
        try {
            BufferedImage img = javax.imageio.ImageIO.read(f);
            // <b>ImageIO returns null rather than throwing</b> for a file that is not an image it
            // knows - a PDF or a renamed text file - so the null has to be checked as well as the
            // exception, or a bad path becomes a NullPointerException in the middle of an export.
            return img;
        } catch (Exception | OutOfMemoryError notAnImage) {
            return null;
        }
    }

    /**
     * Why the logo is not being used, in the reader's words, or null when it is fine.
     *
     * <b>The screen has to be able to say this.</b> A logo that silently does not appear on a
     * report sends the practitioner looking at the export code; the setting that chose the file
     * is where the problem belongs.
     */
    public static String logoProblem() {
        String path = logoPath();
        if (path.isEmpty()) {
            return null;
        }
        File f = new File(path);
        if (!f.exists()) {
            return "That file is no longer there.";
        }
        if (!f.isFile()) {
            return "That is a folder, not an image.";
        }
        if (!f.canRead()) {
            return "That file cannot be read.";
        }
        if (logo() == null) {
            return "That file is not an image this app can read - PNG, JPEG or GIF.";
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing

    /**
     * The cover page: the practitioner above, the chart below.
     *
     * <b>Drawn inside the imageable area</b>, so it keeps the same margins as every other page
     * and a printer that cannot reach the edge of the sheet does not clip it.
     */
    public static void drawCover(Graphics2D g2, PageFormat pf, String chartName, String castLine) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double x = pf.getImageableX();
        double y = pf.getImageableY();
        double w = pf.getImageableWidth();
        double h = pf.getImageableHeight();
        double cursor = y + h * 0.22;

        Image mark = logo();
        if (mark != null) {
            double iw = mark.getWidth(null);
            double ih = mark.getHeight(null);
            if (iw > 0 && ih > 0) {
                double s = Math.min(LOGO_MAX_W / iw, LOGO_MAX_H / ih);
                s = Math.min(s, 1.0);
                double dw = iw * s;
                double dh = ih * s;
                g2.drawImage(mark, (int) Math.round(x + (w - dw) / 2.0),
                    (int) Math.round(cursor - dh), (int) Math.round(dw),
                    (int) Math.round(dh), null);
            }
        }

        cursor += 34;
        String who = practitioner();
        if (!who.isEmpty()) {
            cursor = centred(g2, who, new Font("Serif", Font.PLAIN, 22), INK, x, w, cursor);
        }
        String how = contact();
        if (!how.isEmpty()) {
            cursor += 6;
            cursor = centred(g2, how, new Font("SansSerif", Font.PLAIN, 10), FAINT, x, w, cursor);
        }

        cursor += 26;
        g2.setColor(RULE);
        g2.drawLine((int) Math.round(x + w * 0.30), (int) Math.round(cursor),
            (int) Math.round(x + w * 0.70), (int) Math.round(cursor));
        cursor += 34;

        if (chartName != null && !chartName.isEmpty()) {
            cursor = centred(g2, chartName, new Font("Serif", Font.PLAIN, 17), INK, x, w, cursor);
        }
        if (castLine != null && !castLine.isEmpty()) {
            cursor += 6;
            centred(g2, castLine, new Font("SansSerif", Font.PLAIN, 11), FAINT, x, w, cursor);
        }

        String when = java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"));
        centred(g2, "Prepared " + when, new Font("SansSerif", Font.PLAIN, 9), FAINT,
            x, w, y + h - 8);
    }

    private static double centred(Graphics2D g2, String text, Font font, Color colour,
                                  double x, double w, double baseline) {
        g2.setFont(font);
        g2.setColor(colour);
        Rectangle2D box = g2.getFontMetrics().getStringBounds(text, g2);
        g2.drawString(text, (float) (x + (w - box.getWidth()) / 2.0), (float) baseline);
        return baseline + box.getHeight();
    }

    /**
     * The footer: who prepared it on the left, which page on the right.
     *
     * <b>"Page 4", not "Page 4 of 11".</b> The export writes pages as it discovers them - the
     * loop asks the printable for page n until it says there is no such page - so the total is
     * not known while any page but the last is being drawn. Counting first would mean laying the
     * reading out twice, and the layout is fixed by whichever graphics lays it out first: the
     * blank-page defect B3 closed came from exactly that kind of dry run. A number without a
     * total is worth less than one with, and is the honest thing this loop can say.
     */
    /**
     * Which number goes at the foot of the page at this index.
     *
     * <b>A rule with one home, and reachable from a check.</b> It lived inline in the export
     * loop, where the only way to test it was to read a number off a PDF - and this app draws
     * PDF text as outlines, so there is no text in the file to read. A page numbered from the
     * cover rather than from the wheel is a defect nobody would see until they printed one.
     *
     * @return the number to print, or 0 when this page takes no footer at all
     */
    /**
     * How much of each page the footer takes, in points. Zero when none is drawn.
     *
     * <b>Asserted directly because the page count cannot see it.</b> The first version of this
     * check inferred the band from a report getting longer, and measured: a six page reading
     * loses 156 points to the band out of about 4,600, which is a fifth of a page and rounds to
     * nothing. So the assertion passed whether the band was 26 points or 0 - it was testing that
     * the report had not got SHORTER, which nothing was threatening to do. Reserving the band is
     * a decision, so it is a method, and the check asks it.
     */
    public static double footerBand() {
        return wantsFooter() ? FOOTER_BAND : 0.0;
    }

    public static int footerNumber(int pageIndex, int coverPages) {
        if (pageIndex < coverPages) {
            return 0;
        }
        return pageIndex - coverPages + 1;
    }

    public static void drawFooter(Graphics2D g2, PageFormat pf, int pageNumber) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double x = pf.getImageableX();
        double w = pf.getImageableWidth();
        double base = pf.getImageableY() + pf.getImageableHeight() + FOOTER_BAND - 8;

        g2.setColor(RULE);
        g2.drawLine((int) Math.round(x), (int) Math.round(base - 12),
            (int) Math.round(x + w), (int) Math.round(base - 12));

        g2.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g2.setColor(FAINT);
        String who = practitioner();
        if (!who.isEmpty()) {
            g2.drawString(who, (float) x, (float) base);
        }
        String page = "Page " + pageNumber;
        Rectangle2D box = g2.getFontMetrics().getStringBounds(page, g2);
        g2.drawString(page, (float) (x + w - box.getWidth()), (float) base);
    }
}
