package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.Atlas;
import de.thmac.swisseph.SwissEph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.imageio.ImageIO;

/**
 * Which way round the wheel goes, drawn from the phone's OWN geometry and labelled in words.
 *
 * <b>It calls PhoneWheel.point and PhoneWheel.housePoint directly</b> - this harness is in the
 * same package - so what it draws is where the phone puts things, not a reconstruction. The
 * labels are the point: Ascendant, IC, Descendant, MC and the house numbers in their own
 * sectors, so "the houses go the wrong way" can be settled by looking instead of arguing.
 */
public final class WheelDirection {

    private static final int SIZE = 1000;

    public static void main(String[] args) throws Exception {
        Atlas.Place place = null;
        for (Atlas.Place p : Atlas.search("Sydney", 8)) {
            if (p.country != null && p.country.contains("Australia")) {
                place = p;
                break;
            }
        }
        ChartFrame f = PhoneChart.cast(new SwissEph(Ephemeris.PATH),
            LocalDate.of(1975, 7, 4), LocalTime.of(14, 15), place).frame;
        PhoneWheel w = PhoneWheel.of(f, SIZE, SIZE);

        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(12, 14, 20));
        g.fillRect(0, 0, SIZE, SIZE);

        int outer = w.rings[com.zodiacomputing.ourania.gui.WheelLayout.RING_SIGN_OUTER];
        int inner = w.rings[com.zodiacomputing.ourania.gui.WheelLayout.RING_SIGN_INNER];

        // The sign band, each sign named at its middle.
        g.setStroke(new BasicStroke(1.5f));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        for (int s = 0; s < 12; s++) {
            g.setColor(new Color(80, 86, 102));
            float[] a = w.point(s * 30.0, inner);
            float[] b = w.point(s * 30.0, outer);
            g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
            g.setColor(new Color(170, 180, 200));
            float[] m = w.point(s * 30.0 + 15.0, (outer + inner) / 2.0);
            String name = Zodiac.SIGNS[s];
            g.drawString(name.substring(0, 3), (int) m[0] - 16, (int) m[1] + 7);
        }
        g.setColor(new Color(80, 86, 102));
        circle(g, w.cx, w.cy, outer);
        circle(g, w.cx, w.cy, inner);
        circle(g, w.cx, w.cy, w.natalTop);
        circle(g, w.cx, w.cy, w.aspectDisc);

        // Every cusp, with the four angles heavy, then each house number where the phone puts it.
        for (int h = 1; h <= 12; h++) {
            boolean angle = h == 1 || h == 4 || h == 7 || h == 10;
            g.setColor(angle ? new Color(235, 235, 245) : new Color(70, 76, 90));
            g.setStroke(new BasicStroke(angle ? 3f : 1.5f));
            float[] a = w.point(f.cusps[h], w.aspectDisc);
            float[] b = w.point(f.cusps[h], inner);
            g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        for (int h = 1; h <= 12; h++) {
            float[] n = w.housePoint(h);
            g.setColor(new Color(255, 230, 140));
            g.drawString(String.valueOf(h), (int) n[0] - 9, (int) n[1] + 10);
        }

        // The four angles named in words, just outside the wheel.
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.setColor(new Color(130, 220, 170));
        label(g, w, f.cusps[1], outer + 34, "ASC (1st)");
        label(g, w, f.cusps[4], outer + 34, "IC (4th)");
        label(g, w, f.cusps[7], outer + 34, "DSC (7th)");
        label(g, w, f.cusps[10], outer + 34, "MC (10th)");

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("Phone geometry. Asc " + String.format("%.1f", f.asc)
            + "  MC " + String.format("%.1f", f.cusps[10]), 20, 32);
        g.dispose();
        File out = new File(args.length > 0 ? args[0] : ".", "direction.png");
        ImageIO.write(img, "png", out);

        System.out.println("Ascendant longitude : " + String.format("%.2f", f.asc));
        for (int h : new int[] {1, 4, 7, 10}) {
            float[] p = w.point(f.cusps[h], 300);
            String where = describe(p[0] - w.cx, p[1] - w.cy);
            System.out.printf("cusp %-2d  lon %7.2f  ->  %s%n", h, f.cusps[h], where);
        }
        System.out.println("wrote " + out.getAbsolutePath());
    }

    private static String describe(double dx, double dy) {
        String v = dy < -60 ? "TOP" : dy > 60 ? "BOTTOM" : "";
        String hz = dx < -60 ? "LEFT" : dx > 60 ? "RIGHT" : "";
        String s = (v + " " + hz).trim();
        return s.isEmpty() ? "centre" : s;
    }

    private static void label(Graphics2D g, PhoneWheel w, double lon, double r, String text) {
        float[] p = w.point(lon, r);
        g.drawString(text, (int) p[0] - 48, (int) p[1] + 8);
    }

    private static void circle(Graphics2D g, int cx, int cy, int r) {
        g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    private WheelDirection() { }
}
