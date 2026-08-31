package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

public final class Midpoints {

    public static final class Hit {
        public String p1;
        public String p2;
        public String activatingBody;
        public double separation; // how close to exact
        public boolean isOpposition; // true if activating the far midpoint

        public Hit(String p1, String p2, String activatingBody, double separation, boolean isOpposition) {
            this.p1 = p1;
            this.p2 = p2;
            this.activatingBody = activatingBody;
            this.separation = separation;
            this.isOpposition = isOpposition;
        }
        
        @Override
        public String toString() {
            return String.format("%s = %s/%s (%.2f°)", activatingBody, p1, p2, separation);
        }
    }

    private Midpoints() { }

    /**
     * Finds bodies conjunct or opposite the direct midpoints of key chart factors.
     * The L4 spec asks for Sun, Moon, ASC, and MC.
     */
    public static List<Hit> findDirectMidpoints(ChartFrame f, double orb) {
        List<Hit> hits = new ArrayList<>();

        // Collect key points
        class Point {
            String name;
            double lon;
            Point(String name, double lon) { this.name = name; this.lon = lon; }
        }

        List<Point> keys = new ArrayList<>();
        try {
            ChartFrame.Body sun = f.body("Sun");
            if (sun != null && sun.ok) keys.add(new Point("Sun", sun.lon));
        } catch (IllegalArgumentException e) { }
        
        try {
            ChartFrame.Body moon = f.body("Moon");
            if (moon != null && moon.ok) keys.add(new Point("Moon", moon.lon));
        } catch (IllegalArgumentException e) { }
        
        keys.add(new Point("ASC", f.asc));
        keys.add(new Point("MC", f.mc));

        // Find midpoints between key points
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                Point a = keys.get(i);
                Point b = keys.get(j);

                // Near midpoint
                double diff = ChartFrame.separation(a.lon, b.lon);
                double mNear;
                if (Math.abs(a.lon - b.lon) <= 180.0) {
                    mNear = (a.lon + b.lon) / 2.0;
                } else {
                    mNear = Zodiac.normalise((a.lon + b.lon) / 2.0 + 180.0);
                }

                // Check all bodies
                for (ChartFrame.Body body : f.bodies) {
                    if (body == null || !body.ok) continue;
                    if (body.name.equals(a.name) || body.name.equals(b.name)) continue;

                    double sepNear = ChartFrame.separation(body.lon, mNear);
                    double sepFar = Math.abs(180.0 - sepNear); // if sepNear > 90, sepFar < 90

                    if (sepNear <= orb) {
                        hits.add(new Hit(a.name, b.name, body.name, sepNear, false));
                    } else if (sepFar <= orb) {
                        hits.add(new Hit(a.name, b.name, body.name, sepFar, true));
                    }
                }
            }
        }
        return hits;
    }
}
