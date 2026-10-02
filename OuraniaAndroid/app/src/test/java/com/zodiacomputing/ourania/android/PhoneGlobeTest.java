package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.gui.Globe;

import org.junit.Test;

/**
 * The globe's camera and projection, on the phone's side of the build (M11, stage 1): the
 * desktop's {@link Globe}, compiled into the app from the desktop's own source, turning and
 * projecting at a phone's size.
 */
public class PhoneGlobeTest {

    private static final int W = 1080;

    @Test
    public void theCentreOfTheGlobeIsTheCentreOfTheScreen() {
        Globe g = new Globe();
        Globe.Projected p = g.project(0, 0, 0, W, W);
        assertTrue(p.visible);
        assertEquals(W / 2.0, p.x, 1e-9);
        assertEquals(W / 2.0, p.y, 1e-9);
    }

    @Test
    public void aPointOnTheSignRingProjectsInsideTheScreen() {
        Globe g = new Globe();
        for (int lon = 0; lon < 360; lon += 30) {
            double[] w = Globe.onShell(lon, 0.0, Globe.SHELL_SIGN_OUTER, 0.0);
            Globe.Projected p = g.project(w[0], w[1], w[2], W, W);
            assertTrue(lon + " visible", p.visible);
            assertTrue(lon + " on screen: " + p.x + "," + p.y,
                p.x > 0 && p.x < W && p.y > 0 && p.y < W);
        }
    }

    @Test
    public void aFingerDragTurnsTheGlobe() {
        Globe g = new Globe();
        double yaw = g.yaw;
        g.drag(W / 4.0, 0, W);                       // a quarter of the screen sideways
        g.settleFully();
        assertEquals("a quarter turn of the screen is a quarter of a revolution",
            yaw + Math.PI / 2.0, g.yaw, 1e-4);            // it eases to a stop
    }

    @Test
    public void zoomIsBounded() {
        Globe g = new Globe();
        g.zoom(1000);
        double one = g.distance;
        g.zoom(-1000);
        double other = g.distance;
        assertEquals("the camera stops at its two limits, whichever way",
            3.2 + 12.0, one + other, 1e-9);
        assertFalse("pitch is clamped short of straight down",
            Globe.clampPitch(-10.0, 0.0) < -Globe.MAX_PITCH);
    }
}
