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
     * Bodies sitting on the midpoints of the Sun, Moon, Ascendant and Midheaven.
     *
     * The four points the L4 spec names. Delegates, so the midpoint arithmetic below has one
     * implementation rather than one per caller.
     */
    public static List<Hit> findDirectMidpoints(ChartFrame f, double orb) {
        return scan(f, orb, new String[] {"Sun", "Moon", "ASC", "MC"});
    }

    /**
     * The full midpoint tree: every pair in the chart whose midpoint some body occupies.
     *
     * <b>This is the Ebertin reading, and it is a different question from the four-point
     * version above.</b> That one asks which bodies sit on the axes of the Sun, Moon and the
     * angles - six axes. This asks it of every pair, so with 29 points there are 406 axes and
     * a body can be found on a dozen of them. Grouped by the body that occupies them, the
     * result is the tree: Venus = Sun/Moon, = Mars/Saturn, and so on, which is how the
     * technique is written down and read.
     *
     * Orb is the caller's, and it wants to be tight - a degree or so. The count rises roughly
     * with the square of the point list, so a loose orb here does not find more, it finds
     * everything, which is the same as finding nothing.
     */
    public static List<Hit> tree(ChartFrame f, double orb) {
        return scan(f, orb, null);
    }

    /** True when this point is the derived half of an axis whose primary the chart has. */
    private static boolean mirrors(ChartFrame f, String name) {
        int primary = Bodies.oppositeOf(Bodies.indexOfName(name));
        if (primary < 0) {
            return false;
        }
        try {
            ChartFrame.Body p = f.body(Bodies.at(primary).name);
            return p != null && p.ok;
        } catch (IllegalArgumentException e) {
            return false;   // no primary to report it, so this half must
        }
    }

    /** Adds a named point unless the chart already carries one under that name. */
    private static void addUnlessNamed(List<Point> axes, String name, double lon) {
        for (Point p : axes) {
            if (p.name.equalsIgnoreCase(name)) {
                return;
            }
        }
        axes.add(new Point(name, lon));
    }

    /** One point on the wheel, named, for the axis list. */
    private static final class Point {
        final String name;
        final double lon;

        Point(String name, double lon) {
            this.name = name;
            this.lon = lon;
        }
    }

    /**
     * Every body within orb of the midpoint of any listed pair.
     *
     * @param axisNames the points allowed to form an axis, or null for all of them. The
     *                  bodies tested against those axes are always the whole chart: a tree
     *                  restricted at both ends would miss the contacts it exists to find.
     */
    private static List<Hit> scan(ChartFrame f, double orb, String[] axisNames) {
        List<Hit> hits = new ArrayList<>();
        if (f == null) {
            return hits;
        }
        List<Point> axes = new ArrayList<>();
        if (axisNames == null) {
            for (ChartFrame.Body b : f.bodies) {
                if (b != null && b.ok) {
                    axes.add(new Point(b.name, b.lon));
                }
            }
            // ASC and MC are chart bodies in their own right, so adding them again produced
            // the axis "MC/MC" - a pair with itself, separation zero, and every body in the
            // chart sitting on it.
            addUnlessNamed(axes, "ASC", f.asc);
            addUnlessNamed(axes, "MC", f.mc);
        } else {
            for (String name : axisNames) {
                if ("ASC".equals(name)) {
                    axes.add(new Point("ASC", f.asc));
                } else if ("MC".equals(name)) {
                    axes.add(new Point("MC", f.mc));
                } else {
                    try {
                        ChartFrame.Body b = f.body(name);
                        if (b != null && b.ok) {
                            axes.add(new Point(name, b.lon));
                        }
                    } catch (IllegalArgumentException e) {
                        // A point this chart does not carry is simply not an axis.
                    }
                }
            }
        }

        for (int i = 0; i < axes.size(); i++) {
            for (int j = i + 1; j < axes.size(); j++) {
                Point a = axes.get(i);
                Point b = axes.get(j);
                // <b>A pair has to be two places to have a midpoint between them.</b> Two
                // points at the same degree have no axis, and two at 180 have two equal arcs
                // and so no settled midpoint - ChartFrame already flags that instability for
                // composites. Both cases produce an axis every body appears to sit on, which
                // is how MC/IC and the nodal axis flooded the tree.
                double apart = ChartFrame.separation(a.lon, b.lon);
                if (apart < 0.5 || apart > 179.5) {
                    continue;
                }
                double mNear = Math.abs(a.lon - b.lon) <= 180.0
                    ? (a.lon + b.lon) / 2.0
                    : Zodiac.normalise((a.lon + b.lon) / 2.0 + 180.0);

                for (ChartFrame.Body body : f.bodies) {
                    if (body == null || !body.ok) {
                        continue;
                    }
                    if (body.name.equals(a.name) || body.name.equals(b.name)) {
                        continue;
                    }
                    // <b>The derived half of an axis restates its partner's whole tree.</b>
                    // The IC is opposite the MC by construction, so it is opposite every
                    // midpoint the MC sits on, and the table listed the same axes twice - once
                    // conjunct under MC and once opposite under IC. Same for the Descendant
                    // and the South Node. Bodies.oppositeOf names exactly these three, so the
                    // derived half is dropped when its primary is in the chart to report it.
                    if (mirrors(f, body.name)) {
                        continue;
                    }
                    double sepNear = ChartFrame.separation(body.lon, mNear);
                    double sepFar = Math.abs(180.0 - sepNear);
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
