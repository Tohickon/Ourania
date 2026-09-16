package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Horizon;
import de.thmac.swisseph.DblObj;
import de.thmac.swisseph.SweConst;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.SwingUtilities;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The Sky View: master list E, "a real horizon / celestial-sphere view does not exist".
 *
 * <p>Held to facts that do not come from the code. Part A recomputes every altitude and bearing
 * from right ascension, declination and a sidereal time by the spherical-astronomy formulae,
 * written out here. Part B holds the sky to the chart: the Ascendant is by definition the ecliptic
 * degree on the eastern horizon, the Descendant on the western, and the MC on the meridian - and
 * to the Sun, which at an equinox noon stands at 90 degrees less the latitude.
 */
public final class SkyViewCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        part("A: altitude and bearing, against the formulae", () -> formulae(sw));
        part("B: the sky agrees with the chart and with the Sun", () -> facts(sw));
        part("C: the dome and the horizon draw where they should", SkyViewCheck::projection);
        part("D: the screen, its time and its door", SkyViewCheck::screen);
        part("E: the sky shows what the reader has chosen", () -> selection(sw));

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

    private static void formulae(SwissEph sw) {
        eq("north is 0", 0.0, Horizon.compass(180.0));
        eq("west is 270", 270.0, Horizon.compass(90.0));
        eq("winds", "N NE E SSW W NNW", Horizon.wind(0) + " " + Horizon.wind(44) + " " + Horizon.wind(91) + " "
            + Horizon.wind(202) + " " + Horizon.wind(270) + " " + Horizon.wind(335));

        java.util.Random rnd = new java.util.Random(7);
        double worstAlt = 0;
        double worstAz = 0;
        int n = 0;
        for (int k = 0; k < 300; k++) {
            // 1900 to 2050. Past 2050 Swiss Ephemeris moves to its long-term model of sidereal time,
            // which parts from the simple mean-sidereal formula below by 0.09 degrees by 2079 - and
            // there swe_azalt still equals these formulae exactly when fed its own sidereal time
            // (measured 2026-09-15: Jupiter in 2079, 38.5688 both ways). The disagreement is between
            // two models of the Earth's turning, not an error in the view, so the comparison is
            // held to the years both models are fitted to.
            double jd = SweDate.getJulDay(1900, 1, 1, 0.0) + rnd.nextDouble() * 54786.0;
            double lat = rnd.nextDouble() * 130.0 - 65.0;
            double lon = rnd.nextDouble() * 360.0 - 180.0;
            for (Horizon.Place p : Horizon.bodies(sw, jd, lat, lon)) {
                int ipl = Bodies.byName(p.name).getIpl();
                if (ipl < 0) {
                    // The South Node has no ephemeris number: it is a degree on the ecliptic, and
                    // Part E holds it to the North Node's opposite instead.
                    continue;
                }
                double[] xx = new double[6];
                sw.swe_calc_ut(jd, ipl, SweConst.SEFLG_SWIEPH | SweConst.SEFLG_EQUATORIAL, xx, new StringBuffer());
                double[] mine = altAz(jd, lat, lon, xx[0], xx[1]);
                worstAlt = Math.max(worstAlt, Math.abs(mine[1] - p.altitude));
                if (Math.abs(p.altitude) < 85.0) {
                    worstAz = Math.max(worstAz, sep(mine[0], p.azimuth));
                }
                n++;
            }
        }
        ok("the sample is 300 skies of Sun, Moon and planets, " + n + " places", n >= 3000);
        ok("every altitude matches the formula, worst " + String.format("%.4f", worstAlt) + " degrees", worstAlt < 0.05);
        ok("every bearing matches the formula, worst " + String.format("%.4f", worstAz) + " degrees", worstAz < 0.05);
    }

    /**
     * Azimuth (compass) and altitude from right ascension and declination, by the formulae: the
     * hour angle from mean sidereal time, sin(alt) = sin(lat) sin(dec) + cos(lat) cos(dec) cos(H).
     */
    private static double[] altAz(double jdUt, double lat, double lon, double ra, double dec) {
        double gmst = norm(280.46061837 + 360.98564736629 * (jdUt - 2451545.0));
        double h = Math.toRadians(norm(gmst + lon - ra));
        double phi = Math.toRadians(lat);
        double d = Math.toRadians(dec);
        double alt = Math.toDegrees(Math.asin(Math.sin(phi) * Math.sin(d) + Math.cos(phi) * Math.cos(d) * Math.cos(h)));
        double fromSouth = Math.toDegrees(Math.atan2(Math.sin(h), Math.cos(h) * Math.sin(phi) - Math.tan(d) * Math.cos(phi)));
        return new double[] {norm(fromSouth + 180.0), alt};
    }

    private static double norm(double a) {
        double v = a % 360.0;
        return v < 0 ? v + 360.0 : v;
    }

    private static double sep(double a, double b) {
        double d = Math.abs(norm(a) - norm(b)) % 360.0;
        return Math.min(d, 360.0 - d);
    }

    private static void facts(SwissEph sw) {
        java.util.Random rnd = new java.util.Random(31);
        double worstAsc = 0;
        double worstDsc = 0;
        double worstMc = 0;
        int ascEast = 0;
        int dscWest = 0;
        int charts = 300;
        for (int k = 0; k < charts; k++) {
            double jd = SweDate.getJulDay(1950, 1, 1, 0.0) + rnd.nextDouble() * 36500.0;
            double lat = rnd.nextDouble() * 120.0 - 60.0;
            double lon = rnd.nextDouble() * 360.0 - 180.0;
            List<Horizon.Place> angles = Horizon.angleplaces(sw, jd, lat, lon);
            Horizon.Place asc = angles.get(0);
            Horizon.Place mc = angles.get(1);
            Horizon.Place dsc = angles.get(2);
            worstAsc = Math.max(worstAsc, Math.abs(asc.altitude));
            worstDsc = Math.max(worstDsc, Math.abs(dsc.altitude));
            if (asc.azimuth > 0 && asc.azimuth < 180) {
                ascEast++;
            }
            if (dsc.azimuth > 180 && dsc.azimuth < 360) {
                dscWest++;
            }
            if (Math.abs(mc.altitude) < 89.0) {
                worstMc = Math.max(worstMc, Math.min(sep(mc.azimuth, 0.0), sep(mc.azimuth, 180.0)));
            }
        }
        ok("the Ascendant's degree is on the horizon in every chart, worst " + String.format("%.4f", worstAsc), worstAsc < 0.02);
        ok("and in the east, " + ascEast + " of " + charts, ascEast == charts);
        ok("the Descendant's degree is on the horizon, worst " + String.format("%.4f", worstDsc), worstDsc < 0.02);
        ok("and in the west, " + dscWest + " of " + charts, dscWest == charts);
        ok("the MC is on the meridian, due north or south, worst " + String.format("%.4f", worstMc), worstMc < 0.05);

        // The Sun at an equinox noon: 90 less the latitude. 20 March 2024, the equinox that morning.
        for (double lat : new double[] {40.0, 0.0, -33.9}) {
            double[] geo = {0.0, lat, 0.0};
            DblObj t = new DblObj();
            sw.swe_rise_trans(SweDate.getJulDay(2024, 3, 20, 0.0), SweConst.SE_SUN, null, SweConst.SEFLG_SWIEPH,
                SweConst.SE_CALC_MTRANSIT, geo, 0, 0, t, new StringBuffer());
            Horizon.Place sun = Horizon.place(sw, t.val, lat, 0.0, "Sun", SweConst.SE_SUN);
            double want = 90.0 - Math.abs(lat);
            ok(String.format("at an equinox noon at latitude %.1f the Sun stands %.1f high, got %.2f", lat, want, sun.altitude),
                Math.abs(sun.altitude - want) < 0.6);
            Horizon.Place before = Horizon.place(sw, t.val - 1.0 / 24, lat, 0.0, "Sun", SweConst.SE_SUN);
            Horizon.Place after = Horizon.place(sw, t.val + 1.0 / 24, lat, 0.0, "Sun", SweConst.SE_SUN);
            if (lat != 0.0) {
                ok(String.format("at latitude %.1f the Sun climbs before noon and sinks after", lat), before.climbing && !after.climbing);
            }
        }
        // At sunrise, by the disc's centre and without refraction, the Sun is on the eastern horizon.
        double[] geo = {-75.16, 39.95, 0.0};
        DblObj rise = new DblObj();
        sw.swe_rise_trans(SweDate.getJulDay(2026, 6, 21, 0.0), SweConst.SE_SUN, null, SweConst.SEFLG_SWIEPH,
            SweConst.SE_CALC_RISE | SweConst.SE_BIT_DISC_CENTER | SweConst.SE_BIT_NO_REFRACTION, geo, 0, 0, rise, new StringBuffer());
        Horizon.Place sunrise = Horizon.place(sw, rise.val, 39.95, -75.16, "Sun", SweConst.SE_SUN);
        ok("at sunrise the Sun is on the horizon, altitude " + String.format("%.4f", sunrise.altitude), Math.abs(sunrise.altitude) < 0.05);
        ok("in the north-east at midsummer in Philadelphia, bearing " + String.format("%.1f", sunrise.azimuth),
            sunrise.azimuth > 50 && sunrise.azimuth < 70);
        ok("and refraction already lifts it into view", sunrise.apparent > 0.3 && sunrise.visible());
    }

    private static void projection() throws Exception {
        final SkyViewPanel[] v = new SkyViewPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            v[0] = new SkyViewPanel(null);
            v[0].canvas.setSize(600, 500);
            v[0].setMoment(new double[] {SweDate.getJulDay(2026, 9, 15, 3.0), 39.95, -75.16});
        });
        SkyViewPanel p = v[0];
        SkyViewPanel.Canvas c = p.canvas;
        SwingUtilities.invokeAndWait(() -> p.view.setSelectedItem(SkyViewPanel.DOME));
        Point2D.Double zen = c.onDome(123, 90);
        ok("on the dome the zenith is the centre", zen.distance(c.cx(), c.cy()) < 0.5);
        ok("the horizon is the rim", Math.abs(c.onDome(40, 0).distance(c.cx(), c.cy()) - c.radius()) < 0.5);
        ok("north is up", c.onDome(0, 0).y < c.cy() - c.radius() + 1);
        ok("east is on the left, as a sky seen from beneath", c.onDome(90, 0).x < c.cx() - c.radius() + 1);
        ok("altitude is even from rim to centre", Math.abs(c.onDome(200, 45).distance(c.cx(), c.cy()) - c.radius() / 2.0) < 0.5);
        int drawnDome = 0;
        int hit = 0;
        int hidden = 0;
        for (Horizon.Place b : p.bodies) {
            if (!c.drawn(b)) {
                hidden++;
                continue;
            }
            drawnDome++;
            Point2D.Double q = c.at(b.azimuth, b.apparent);
            Horizon.Place found = c.placeAt((int) Math.round(q.x), (int) Math.round(q.y));
            if (found != null && (found == b || c.at(found.azimuth, found.apparent).distance(q) < 1.0)) {
                hit++;
            }
        }
        ok("the dome draws what is up and hides what is down, " + drawnDome + " up, " + hidden + " down",
            drawnDome + hidden == p.bodies.size() && hidden == p.bodies.stream().filter(b -> b.apparent < 0).count());
        ok("hovering each planet on the dome names it, " + hit + " of " + drawnDome, hit == drawnDome);

        SwingUtilities.invokeAndWait(() -> p.view.setSelectedItem(SkyViewPanel.HORIZON));
        ok("on the horizon view north is at the left edge", c.onStrip(0, 0).x < c.onStrip(90, 0).x);
        ok("and south in the middle", Math.abs(c.onStrip(180, 0).x - (c.onStrip(0, 0).x + c.onStrip(360, 0).x) / 2) < 0.5);
        ok("higher is higher", c.onStrip(10, 60).y < c.onStrip(10, 0).y && c.onStrip(10, 0).y < c.onStrip(10, -20).y);
        int strip = 0;
        int stripHit = 0;
        for (Horizon.Place b : p.bodies) {
            if (b.apparent < SkyViewPanel.Canvas.BOTTOM_ALT) {
                continue;
            }
            strip++;
            Point2D.Double q = c.at(b.azimuth, b.apparent);
            Horizon.Place found = c.placeAt((int) Math.round(q.x), (int) Math.round(q.y));
            if (found != null && (found == b || c.at(found.azimuth, found.apparent).distance(q) < 1.0)) {
                stripHit++;
            }
        }
        ok("the horizon view shows planets below the horizon too, " + strip + " of " + p.bodies.size(), strip > drawnDome);
        ok("hovering each names it, " + stripHit + " of " + strip, stripHit == strip);
        String tip = c.getToolTipText(new java.awt.event.MouseEvent(c, 0, 0, 0, -50, -50, 0, false));
        ok("empty sky has no card", tip == null);
    }

    /** How many registry points the sky can place out of a selection. */
    private static int selectable(boolean[] on) {
        int n = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            if (i < on.length && on[i] && (Bodies.at(i).source == Bodies.Source.EPHEMERIS
                    || Bodies.at(i).source == Bodies.Source.SOUTH_NODE)) {
                n++;
            }
        }
        return n;
    }

    /**
     * David, 2026-09-15: "it only shows the planets and not the asteroids when they are selected".
     *
     * The sky view drew the ten planets and never read the body selection, so an asteroid switched
     * on in Settings appeared on the wheel and nowhere in the sky.
     */
    private static void selection(SwissEph sw) throws Exception {
        double jd = SweDate.getJulDay(2026, 9, 15, 3.0);
        double lat = 39.95;
        double lon = -75.16;

        boolean[] planetsOnly = new boolean[Bodies.count()];
        for (int i = 0; i < Bodies.count(); i++) {
            planetsOnly[i] = Bodies.at(i).kind == Bodies.Kind.LUMINARY
                || Bodies.at(i).kind == Bodies.Kind.PLANET;
        }
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (Horizon.Place b : Horizon.bodies(sw, jd, lat, lon, planetsOnly)) {
            names.add(b.name);
        }
        eq("with the planets alone chosen, the sky holds the ten", 10, names.size());
        ok("and no asteroid among them", !names.contains("Ceres") && !names.contains("Eros"));

        boolean[] withRocks = planetsOnly.clone();
        for (String id : new String[] {"ceres", "pallas", "juno", "vesta", "chiron", "eros", "eris",
                                       "north_node", "south_node"}) {
            withRocks[Bodies.indexOf(id)] = true;
        }
        java.util.Set<String> wider = new java.util.TreeSet<>();
        for (Horizon.Place b : Horizon.bodies(sw, jd, lat, lon, withRocks)) {
            wider.add(b.name);
        }
        eq("choosing the asteroids and the nodes puts all of them in the sky",
            selectable(withRocks), wider.size());
        for (String name : new String[] {"Ceres", "Pallas", "Juno", "Vesta", "Chiron", "Eros",
                                         "Eris", "North Node", "South Node"}) {
            ok("the sky holds " + name, wider.contains(name));
        }

        // A lot has no place in the sky of its own - it is built from a chart - so choosing one
        // adds nothing, and must not throw either.
        boolean[] withLots = withRocks.clone();
        withLots[Bodies.indexOf("fortune")] = true;
        withLots[Bodies.indexOf("vertex")] = true;
        eq("a lot or the Vertex adds nothing to the sky", wider.size(),
            Horizon.bodies(sw, jd, lat, lon, withLots).size());

        // The South Node is the degree opposite the North Node, so it stands there in the sky too.
        Horizon.Place north = null;
        Horizon.Place south = null;
        for (Horizon.Place b : Horizon.bodies(sw, jd, lat, lon, withRocks)) {
            if ("North Node".equals(b.name)) {
                north = b;
            } else if ("South Node".equals(b.name)) {
                south = b;
            }
        }
        double[] opposite = Horizon.fromEcliptic(sw, jd, lat, lon,
            com.zodiacomputing.ourania.astro.Zodiac.normalise(
                com.zodiacomputing.ourania.astro.Almanac.bodyLongitude(sw, jd, "North Node") + 180.0));
        ok("the South Node stands at the North Node's opposite degree",
            south != null && Math.abs(south.altitude - opposite[1]) < 1.0e-9
                && sep(south.azimuth, opposite[0]) < 1.0e-9);
        ok("and the two nodes are on opposite sides of the sky, "
                + (north == null || south == null ? "missing" : String.format("%.0f degrees apart",
                    sep(north.azimuth, south.azimuth))),
            north != null && south != null && Math.abs(north.altitude + south.altitude) < 40.0);

        // And the screen reads the saved selection, not a list of its own.
        String keep = Settings.get(Settings.BODIES_KEY, null);
        try {
            Settings.saveBodySelection(withRocks);
            final SkyViewPanel[] v = new SkyViewPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                v[0] = new SkyViewPanel(null);
                v[0].setMoment(new double[] {jd, lat, lon});
            });
            java.util.Set<String> shown = new java.util.TreeSet<>();
            for (Horizon.Place b : v[0].bodies) {
                shown.add(b.name);
            }
            ok("the screen draws the chosen asteroids, " + shown.size() + " points",
                shown.contains("Ceres") && shown.contains("Eros") && shown.equals(wider));
            String html = v[0].table.getText();
            ok("and names them in the table", html.contains("Ceres") && html.contains("Eros"));
        } finally {
            if (keep == null) {
                Settings.update(props -> props.remove(Settings.BODIES_KEY));
            } else {
                Settings.set(Settings.BODIES_KEY, keep);
            }
        }
    }

    private static void screen() throws Exception {
        final SkyViewPanel[] v = new SkyViewPanel[1];
        double jd = SweDate.getJulDay(2026, 9, 15, 3.0);
        SwingUtilities.invokeAndWait(() -> {
            v[0] = new SkyViewPanel(null);
            v[0].setMoment(new double[] {jd, 39.95, -75.16});
        });
        SkyViewPanel p = v[0];
        eq("every selected point the sky can place is in the list", selectable(Settings.loadBodySelection()),
            p.bodies.size());
        ok("and the four angles", p.angles.size() == 4);
        ok("the ecliptic is a ring of 180 points", p.ecliptic.size() == 180);
        String html = p.table.getText();
        int named = 0;
        for (Horizon.Place b : p.bodies) {
            if (html.contains(b.name)) {
                named++;
            }
        }
        ok("the table names every body, " + named, named == p.bodies.size());
        double sunBefore = sun(p).azimuth;
        SwingUtilities.invokeAndWait(() -> p.offset.setValue(120));
        near("the slider moves the moment two hours", jd + 120.0 / 1440.0, p.jd());
        double moved = sep(sunBefore, sun(p).azimuth);
        ok("and the Sun moves across the sky with it, " + String.format("%.1f", moved) + " degrees of bearing", moved > 15.0);
        ok("the time readout says how far it has moved", p.when.getText().contains("+2h 00m"));
        SwingUtilities.invokeAndWait(() -> p.offset.setValue(0));
        near("and back", jd, p.jd());

        ok("the menu offers the Sky View", java.util.Arrays.stream(SidePanel.SCREENS).anyMatch(s -> s[1].equals("SKY_VIEW")));
        ok("and calls the wheel what it is", java.util.Arrays.stream(SidePanel.SCREENS)
            .anyMatch(s -> s[1].equals("SKYMAP") && s[0].equals("Chart Wheel")));
        ok("nothing is called Grid Skymap", java.util.Arrays.stream(SidePanel.SCREENS).noneMatch(s -> s[0].contains("Grid Skymap")));

        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SwingUtilities.invokeAndWait(() -> w[0].switchScreen("SKY_VIEW"));
            java.lang.reflect.Field f = OuraniaWindow.class.getDeclaredField("skyViewPanel");
            f.setAccessible(true);
            SkyViewPanel panel = (SkyViewPanel) f.get(w[0]);
            double[] wheel = w[0].skymapForViews().skyMoment(false);
            ok("opening it draws the sky the wheel holds",
                panel != null && Math.abs(panel.moment[0] - wheel[0]) < 1e-9 && panel.moment[1] == wheel[1] && panel.moment[2] == wheel[2]);
            ok("and it is the screen showing", panel != null && panel.isVisible());
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static Horizon.Place sun(SkyViewPanel p) {
        return p.bodies.stream().filter(b -> b.name.equals("Sun")).findFirst().orElseThrow();
    }

    private static void near(String label, double want, double got) {
        ok(label + String.format(": got %.6f, expected %.6f", got, want), Math.abs(want - got) < 1e-6);
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void eq(String label, Object want, Object got) {
        ok(label + ": got " + got + ", expected " + want, want.equals(got));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
