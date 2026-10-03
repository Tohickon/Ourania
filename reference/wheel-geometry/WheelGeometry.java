package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.WheelLayout;
import de.thmac.swisseph.SwissEph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.imageio.ImageIO;

/**
 * A scratch harness, under reference/ and not compiled with either app: how big the phone's
 * planet discs are against the band that holds them, and how much they overlap each other.
 *
 * <b>It is a measuring tool, not a second painter.</b> It draws flat circles at the radii and
 * longitudes PhoneWheel computes - no gradients, no halos, no faces - because the question it
 * answers is about SIZE AND CROWDING, which those would only obscure. There is deliberately
 * no copy of WheelView's drawing here; a second painter is this project's most-logged defect.
 */
public final class WheelGeometry {

    private static final int SIZE = 1080;

    public static void main(String[] args) throws Exception {
        Atlas.Place place = null;
        for (Atlas.Place p : Atlas.search("Sydney", 8)) {
            if (p.country != null && p.country.contains("Australia")) {
                place = p;
                break;
            }
        }
        if (place == null) {
            System.out.println("Sydney not in the atlas - is ourania.data set?");
            return;
        }
        ChartFrame f = PhoneChart.cast(new SwissEph(Ephemeris.PATH),
            LocalDate.of(1975, 7, 4), LocalTime.of(14, 15), place).frame;
        PhoneWheel w = PhoneWheel.of(f, SIZE, SIZE);

        int depth = w.natalTop - w.natalFloor;
        System.out.println("band: natalTop=" + w.natalTop + " natalFloor=" + w.natalFloor
            + " depth=" + depth + "   NATAL_SPACING=" + WheelLayout.NATAL_SPACING);
        System.out.println("aspectDisc=" + w.aspectDisc
            + "   EMPTY between natalFloor and aspectDisc = "
            + (w.natalFloor - w.aspectDisc) + "px");
        java.util.TreeSet<Integer> subrings = new java.util.TreeSet<>();
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            if (w.bodyRadius[i] > 0) { subrings.add(w.bodyRadius[i]); }
        }
        System.out.println("sub-rings used: " + subrings
            + "   outermost disc top = " + (subrings.last() + w.planetRadius(0))
            + " vs natalTop " + w.natalTop);
        System.out.println();
        System.out.printf("%-9s %7s %7s %7s %8s%n",
            "body", "lon", "radius", "disc", "disc/band");
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            if (w.bodyRadius[i] <= 0) {
                continue;
            }
            int r = w.planetRadius(i);
            System.out.printf("%-9s %7.1f %7d %7d %8.0f%%%n", Bodies.at(i).name,
                f.bodies[i].lon, w.bodyRadius[i], 2 * r, 100.0 * (2 * r) / depth);
        }

        // Every pair that overlaps, and by how much of a diameter.
        System.out.println();
        int worst = 0;
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            for (int j = i + 1; j < PhoneWheel.PLANETS; j++) {
                if (w.bodyRadius[i] <= 0 || w.bodyRadius[j] <= 0) {
                    continue;
                }
                float[] a = w.body(i);
                float[] b = w.body(j);
                double d = Math.hypot(a[0] - b[0], a[1] - b[1]);
                int ri = w.planetRadius(i);
                int rj = w.planetRadius(j);
                double gap = d - ri - rj;
                if (gap < 0) {
                    worst++;
                    System.out.printf("OVERLAP  %-9s %-9s centres %.0f apart, radii %d+%d,"
                        + " buried %.0f (%.0f%% of the smaller disc)%n",
                        Bodies.at(i).name, Bodies.at(j).name, d, ri, rj, -gap,
                        100.0 * -gap / (2 * Math.min(ri, rj)));
                }
            }
        }
        System.out.println(worst + " overlapping pairs");

        // A picture of it: the band's two edges, and a flat disc per planet.
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(12, 14, 20));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(70, 76, 90));
        circle(g, w.cx, w.cy, w.rings[WheelLayout.RING_SIGN_INNER]);
        circle(g, w.cx, w.cy, w.rings[WheelLayout.RING_SIGN_OUTER]);
        g.setColor(new Color(120, 90, 90));
        circle(g, w.cx, w.cy, w.natalTop);
        circle(g, w.cx, w.cy, w.natalFloor);
        g.setColor(new Color(60, 70, 90));
        circle(g, w.cx, w.cy, w.aspectDisc);
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            if (w.bodyRadius[i] <= 0) {
                continue;
            }
            float[] p = w.body(i);
            int r = w.planetRadius(i);
            g.setColor(new Color(210, 190, 120, 150));
            g.fillOval((int) p[0] - r, (int) p[1] - r, 2 * r, 2 * r);
            g.setColor(new Color(255, 240, 190));
            g.drawOval((int) p[0] - r, (int) p[1] - r, 2 * r, 2 * r);
        }
        g.dispose();
        File out = new File(args.length > 0 ? args[0] : "wheel-geometry.png");
        ImageIO.write(img, "png", out);
        System.out.println();
        System.out.println("wrote " + out.getAbsolutePath());
    }

    private static void circle(Graphics2D g, int cx, int cy, int r) {
        g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    private WheelGeometry() { }
}
