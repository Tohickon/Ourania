package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.Settings;
import com.zodiacomputing.ourania.gui.WheelLayout;
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
 * The WHOLE composition, not just the band - which is what the first harness got wrong.
 *
 * <b>It drew the natal band and the discs and nothing else</b>, so it reported a success
 * ("flush inside the band, no overlaps") for a change that crushed the aspect circle from
 * radius 164 to 123 and squeezed the house numbers into a thin ring. The thing being judged
 * is a composition; a harness that renders one part of it will approve of starving the rest.
 *
 * So this draws every ring the phone draws, at the radii the phone computes, and sweeps the
 * disc scale so the trade - bigger planets against a readable centre - can be SEEN rather
 * than argued. Still no gradients and no faces: this is about proportion.
 *
 * Not a second painter: every radius comes from WheelLayout, the same calls PhoneWheel makes.
 */
public final class WheelComposition {

    private static final int SIZE = 1080;

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

        float[] scales = {1.7f, 1.4f, 1.2f, 1.0f};
        for (float scale : scales) {
            render(f, scale, new File(args.length > 0 ? args[0] : ".",
                "comp-" + String.format("%.1f", scale) + ".png"));
        }
    }

    private static void render(ChartFrame f, float scale, File out) throws Exception {
        int[] rings = WheelLayout.ringRadii(SIZE, SIZE, false);
        int natalTop = WheelLayout.bodyBase(rings, Settings.bodyRing());
        int disc = Math.round(WheelLayout.natalSize(0).radius * scale);

        // The same rule PhoneWheel.phoneBandDepth applies: deep enough for BAND_TIERS tiers of
        // discs that cannot touch. Repeated here rather than called because the harness is
        // sweeping the scale, and PhoneWheel's is compiled against one.
        int depth = Math.min(Math.max(Math.round(6.5f * disc),
            WheelLayout.natalBandDepth(Math.max(1, natalTop))), Math.max(1, natalTop / 2));
        int natalFloor = Math.max(1, natalTop - depth);
        int aspectDisc = WheelLayout.aspectDiscs(natalFloor)[0];

        double[] lon = new double[Bodies.count()];
        boolean[] drawn = new boolean[Bodies.count()];
        for (int i = 0; i < PhoneWheel.PLANETS && i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b != null && b.ok) {
                lon[i] = b.lon;
                drawn[i] = true;
            }
        }
        int[] radii = WheelLayout.bandRadii(lon, drawn, natalTop, natalFloor, disc, 2.0 * disc);

        int cx = SIZE / 2;
        int cy = SIZE / 2;
        double pin = f.timeUnknown ? 0.0 : f.asc;

        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(12, 14, 20));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setStroke(new BasicStroke(2f));

        g.setColor(new Color(90, 96, 112));
        circle(g, cx, cy, rings[WheelLayout.RING_OUTER]);
        circle(g, cx, cy, rings[WheelLayout.RING_SIGN_OUTER]);
        circle(g, cx, cy, rings[WheelLayout.RING_SIGN_INNER]);
        g.setColor(new Color(120, 90, 90));
        circle(g, cx, cy, natalTop);
        circle(g, cx, cy, natalFloor);

        // The aspect circle and a sample of the chords across it - the part the last harness
        // could not see, and the part that got crushed.
        g.setColor(new Color(70, 110, 170));
        circle(g, cx, cy, aspectDisc);
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            for (int j = i + 1; j < PhoneWheel.PLANETS; j++) {
                if (!drawn[i] || !drawn[j]) {
                    continue;
                }
                double sep = Math.abs(lon[i] - lon[j]) % 360.0;
                sep = Math.min(sep, 360.0 - sep);
                for (double asp : new double[] {0, 60, 90, 120, 180}) {
                    if (Math.abs(sep - asp) <= 6.0) {
                        double[] a = point(cx, cy, pin, lon[i], aspectDisc);
                        double[] b = point(cx, cy, pin, lon[j], aspectDisc);
                        g.setColor(asp == 90 || asp == 180
                            ? new Color(200, 90, 90, 190) : new Color(90, 140, 210, 190));
                        g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
                        break;
                    }
                }
            }
        }

        // The house numbers ride between the aspect circle and the planets.
        g.setStroke(new BasicStroke(2f));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        if (!f.timeUnknown) {
            g.setColor(new Color(150, 160, 180));
            for (int h = 1; h <= 12; h++) {
                double mid = f.cusps[h] + 15;
                double[] n = point(cx, cy, pin, mid, (aspectDisc + natalFloor) / 2.0);
                g.drawString(String.valueOf(h), (int) n[0] - 8, (int) n[1] + 9);
            }
        }

        for (int i = 0; i < PhoneWheel.PLANETS; i++) {
            if (!drawn[i]) {
                continue;
            }
            int r = Math.round(WheelLayout.natalSize(i).radius * scale);
            double[] p = point(cx, cy, pin, lon[i], radii[i]);
            g.setColor(new Color(170, 150, 200, 40));          // the halo, at 1.26
            g.fillOval((int) (p[0] - r * 1.26), (int) (p[1] - r * 1.26),
                (int) (r * 2.52), (int) (r * 2.52));
            g.setColor(new Color(210, 190, 120, 190));
            g.fillOval((int) p[0] - r, (int) p[1] - r, 2 * r, 2 * r);
            g.setColor(new Color(255, 240, 190));
            g.drawOval((int) p[0] - r, (int) p[1] - r, 2 * r, 2 * r);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString("disc x" + String.format("%.1f", scale) + "  r=" + disc
            + "  band=" + depth + "  aspectDisc=" + aspectDisc, 24, 44);
        g.dispose();
        ImageIO.write(img, "png", out);
        System.out.printf("x%.1f  discR=%-3d band=%-4d natalFloor=%-4d aspectDisc=%-4d%n",
            scale, disc, depth, natalFloor, aspectDisc);
    }

    private static double[] point(int cx, int cy, double pin, double lon, double radius) {
        double a = Math.toRadians(180.0 + pin - lon);
        return new double[] {cx + radius * Math.cos(a), cy - radius * Math.sin(a)};
    }

    private static void circle(Graphics2D g, int cx, int cy, int r) {
        g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    private WheelComposition() { }
}
