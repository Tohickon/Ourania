package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Two things the globe was missing, both reported by David on 2026-09-15.
 *
 * <ul>
 * <li><b>The leader lines.</b> "When turned on to have a line go from the object to the degree its
 * in it doesnt do that for globe mode." The flat wheel has drawn them since bodies began being
 * spread outward where they crowd; the globe stacks them up the shell for the same reason and drew
 * no leader at all, so a stacked glyph pointed at nothing.</li>
 * <li><b>The sign shell.</b> "Could we have it be an option to toggle on or off that transluscent
 * color shell globe that is with the signs in globe mode." It had no setting.</li>
 * </ul>
 *
 * <p>Both are held by painting the globe twice and comparing the frames, because both are drawing
 * and the question is whether the drawing changed - and by counting the ink each one owns, so a
 * change somewhere else in the frame cannot pass for the feature under test.
 */
public final class GlobeOptionsCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;
    private static final int SIZE = 700;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] hold = new OuraniaWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new OuraniaWindow());
        try {
            java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
            fs.setAccessible(true);
            SkymapPanel panel = (SkymapPanel) fs.get(hold[0]);
            Thread.sleep(2500);
            panel.updateChartData();
            Thread.sleep(1200);

            part("A: the sign shell is a setting", () -> signShell(panel));
            part("B: the leader lines reach the globe", () -> leaders(panel));
            part("C: the controls", () -> controls());
        } finally {
            Settings.setGlobeSignPlane(true);
            Settings.setShowDegreeLines(true);
            final OuraniaWindow w = hold[0];
            javax.swing.SwingUtilities.invokeAndWait(() -> w.dispose());
        }

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

    /** Waits up to two seconds for a bloom to finish, so a frame is of a settled chart. */
    private static void settle(java.util.function.BooleanSupplier done) {
        for (int i = 0; i < 100 && !done.getAsBoolean(); i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** One frame of the globe on a flat ground, so what differs between two is the globe's own ink. */
    private static BufferedImage frame(SkymapPanel panel) {
        BufferedImage im = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(10, 12, 16));
        g.fillRect(0, 0, SIZE, SIZE);
        GlobeRenderer.paint(g, new Globe(), SIZE, SIZE, panel, false);
        g.dispose();
        return im;
    }

    /**
     * A frame of a chart that has stopped moving.
     *
     * <b>Every layer on this chart opens and folds through a bloom</b>, so a frame painted straight
     * after a setting changes is a frame of an animation - which is how "turning it back on
     * restores the frame" failed the first time it ran, comparing a settled globe against one still
     * unfolding. Painting until two frames in a row agree is the honest way to ask for the
     * after picture, and it needs no knowledge of which animations are in flight.
     */
    private static BufferedImage stableFrame(SkymapPanel panel) {
        BufferedImage last = frame(panel);
        for (int i = 0; i < 100; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return last;
            }
            BufferedImage next = frame(panel);
            if (differingPixels(last, next) == 0) {
                return next;
            }
            last = next;
        }
        return last;
    }

    private static int differingPixels(BufferedImage a, BufferedImage b) {
        int n = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    /** How much of the frame is not the ground: the globe's own ink, however faint. */
    private static int inkedPixels(BufferedImage im) {
        int ground = new Color(10, 12, 16).getRGB();
        int n = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (im.getRGB(x, y) != ground) {
                    n++;
                }
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ A

    private static void signShell(SkymapPanel panel) {
        Settings.setShowDegreeLines(false);
        Settings.setGlobeSignPlane(true);
        BufferedImage on = stableFrame(panel);
        Settings.setGlobeSignPlane(false);
        BufferedImage off = stableFrame(panel);

        int moved = differingPixels(on, off);
        ok("turning the sign shell off changes the globe, " + moved + " pixels", moved > 2000);
        ok("and it is the shell that goes: the frame with it has more ink, "
                + inkedPixels(on) + " against " + inkedPixels(off),
            inkedPixels(on) > inkedPixels(off) + 2000);

        // <b>Off is not the same as hiding the signs.</b> The boundaries, the band at the equator
        // and the degree scale stay; only the wash goes. Hiding the layer takes all of them, so
        // the two frames must differ.
        boolean signsOn = panel.layerShown(SkymapPanel.Layer.SIGNS);
        panel.setLayer(SkymapPanel.Layer.SIGNS, false);
        // <b>Folding a layer is animated.</b> Painted straight after the call, the signs are still
        // most of the way open and the frame is the one we already have - which is how this read
        // "0 pixels apart" the first time it ran. Wait for the fold to finish before looking.
        settle(() -> !panel.layerShown(SkymapPanel.Layer.SIGNS));
        BufferedImage hidden = stableFrame(panel);
        panel.setLayer(SkymapPanel.Layer.SIGNS, signsOn);
        settle(() -> panel.layerShown(SkymapPanel.Layer.SIGNS) == signsOn);
        ok("and the shell off still draws the signs, unlike hiding them, "
                + differingPixels(off, hidden) + " pixels apart",
            differingPixels(off, hidden) > 500);

        Settings.setGlobeSignPlane(true);
        BufferedImage back = stableFrame(panel);
        ok("turning it back on restores the frame", differingPixels(on, back) == 0);
    }

    // ------------------------------------------------------------------ B

    private static void leaders(SkymapPanel panel) {
        Settings.setGlobeSignPlane(false); // the shell would wash over the thing being counted
        Settings.setShowDegreeLines(false);
        BufferedImage without = stableFrame(panel);
        Settings.setShowDegreeLines(true);
        BufferedImage with = stableFrame(panel);

        int moved = differingPixels(without, with);
        ok("the leaders draw on the globe, " + moved + " pixels", moved > 200);
        ok("and they add ink rather than move it, "
                + inkedPixels(with) + " against " + inkedPixels(without),
            inkedPixels(with) > inkedPixels(without));

        // They are leaders, not a wash: a line from each body to its degree is a small share of
        // a frame that already holds the shells, the bands and the aspect network.
        ok("and they are lines, not a fill: " + moved + " pixels of a "
                + inkedPixels(with) + " pixel globe", moved < inkedPixels(with) / 4);

        Settings.setShowDegreeLines(false);
        BufferedImage again = stableFrame(panel);
        ok("turning them off puts the globe back", differingPixels(without, again) == 0);
        Settings.setShowDegreeLines(true);
        Settings.setGlobeSignPlane(true);
    }

    // ------------------------------------------------------------------ C

    private static void controls() throws Exception {
        // Stored, and read back as stored - the setting a reader actually touches.
        Settings.setGlobeSignPlane(false);
        ok("the shell setting is saved", !Settings.globeSignPlane());
        Settings.setGlobeSignPlane(true);
        ok("and saved back on", Settings.globeSignPlane());
        Settings.update(p -> p.remove(Settings.GLOBE_SIGN_PLANE_KEY));
        ok("a fresh install has the shell on", Settings.globeSignPlane());

        final SettingsPanel[] sp = new SettingsPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> sp[0] = new SettingsPanel(null));
        javax.swing.JCheckBox box = boxSaying(sp[0], "coloured shell");
        ok("the Settings screen offers the shell as a tick box", box != null);
        if (box != null) {
            ok("which opens ticked, because the shell is on", box.isSelected());
            final javax.swing.JCheckBox b = box;
            javax.swing.SwingUtilities.invokeAndWait(() -> b.setSelected(false));
            ok("and unticking it turns the shell off", !Settings.globeSignPlane());
            javax.swing.SwingUtilities.invokeAndWait(() -> b.setSelected(true));
            ok("and ticking it back on", Settings.globeSignPlane());
        }
    }

    /** The first check box on a panel whose text contains this, at any depth. */
    private static javax.swing.JCheckBox boxSaying(java.awt.Container root, String text) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JCheckBox
                    && ((javax.swing.JCheckBox) c).getText().contains(text)) {
                return (javax.swing.JCheckBox) c;
            }
            if (c instanceof java.awt.Container) {
                javax.swing.JCheckBox found = boxSaying((java.awt.Container) c, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
}
