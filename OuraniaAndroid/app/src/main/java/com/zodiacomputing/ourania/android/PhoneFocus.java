package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.InterpretationService;

/**
 * What the reader has drilled into, and the text that belongs to it.
 *
 * <h3>Why this is its own class, with no Android in it</h3>
 *
 * <p>The drill-down is chart, then house, then a planet in that house, then the degree it
 * stands on - four levels, each with its own reading. Two things could have owned that: the
 * view that draws the wheel, or the activity that owns the screen. Both are Android, and
 * neither can be tested here.
 *
 * <p>So the stack and the text live in a class with no Android import, which makes the whole
 * question testable on the JVM: that going in and coming back out lands where it should, that
 * each level asks the engine for the right thing, and that a level with nothing to say says so
 * rather than showing an empty panel.
 *
 * <h3>The levels are a path, not a mode</h3>
 *
 * <p>A focus is the deepest thing chosen plus everything above it - house 1, Mars, 14 Scorpio
 * is one focus, not three - because the reading at each level needs its parents: a planet's
 * text is its sign AND its house, and a degree's is its sign's degree. Keeping the path means
 * going back up is dropping the last step rather than recomputing where the reader was.
 */
final class PhoneFocus {

    /** How deep the reader has gone. */
    enum Level { CHART, HOUSE, PLANET, DEGREE }

    private final ChartFrame frame;
    private Level level = Level.CHART;
    private int house = -1;
    private int body = -1;
    private int degree = -1;
    private int sign = -1;

    PhoneFocus(ChartFrame frame) {
        this.frame = frame;
    }

    Level level() {
        return this.level;
    }

    int house() {
        return this.house;
    }

    int body() {
        return this.body;
    }

    int degree() {
        return this.degree;
    }

    int sign() {
        return this.sign;
    }

    /** True when there is somewhere to go back to. */
    boolean canAscend() {
        return this.level != Level.CHART;
    }

    /** Drop the deepest step. Chart is the floor and stays there. */
    void ascend() {
        switch (this.level) {
            case DEGREE:
                this.level = this.body >= 0 ? Level.PLANET : Level.HOUSE;
                this.degree = -1;
                this.sign = -1;
                break;
            case PLANET:
                this.level = this.house > 0 ? Level.HOUSE : Level.CHART;
                this.body = -1;
                break;
            case HOUSE:
                this.level = Level.CHART;
                this.house = -1;
                break;
            default:
                break;
        }
    }

    /** All the way out. */
    void reset() {
        this.level = Level.CHART;
        this.house = -1;
        this.body = -1;
        this.degree = -1;
        this.sign = -1;
    }

    void toHouse(int h) {
        if (h < 1 || h > 12) {
            return;
        }
        this.house = h;
        this.body = -1;
        this.degree = -1;
        this.sign = -1;
        this.level = Level.HOUSE;
    }

    /**
     * Focus a body, and take its house with it.
     *
     * <p>The house is read from the body's own longitude rather than kept from whatever was
     * focused before: a reader can tap a planet straight from the whole chart, and a planet's
     * house is a fact about the planet, not about how it was reached.
     */
    void toBody(int i) {
        if (i < 0 || i >= Bodies.count() || i >= this.frame.bodies.length) {
            return;
        }
        ChartFrame.Body b = this.frame.bodies[i];
        if (b == null || !b.ok) {
            return;
        }
        this.body = i;
        this.house = houseOf(this.frame, b.lon);
        this.degree = -1;
        this.sign = -1;
        this.level = Level.PLANET;
    }

    /** Focus a degree of a sign. A degree can be reached from a planet or from the wheel. */
    void toDegree(int signIndex, int whole) {
        if (signIndex < 0 || signIndex > 11 || whole < 1 || whole > 30) {
            return;
        }
        this.sign = signIndex;
        this.degree = whole;
        this.level = Level.DEGREE;
    }

    /** The house a longitude falls in, or -1 without a birth time. */
    static int houseOf(ChartFrame f, double lon) {
        if (f.timeUnknown) {
            return -1;
        }
        for (int h = 1; h <= 12; h++) {
            double from = f.cusps[h];
            double to = f.cusps[h == 12 ? 1 : h + 1];
            double span = (((to - from) % 360.0) + 360.0) % 360.0;
            double into = (((lon - from) % 360.0) + 360.0) % 360.0;
            if (into < span) {
                return h;
            }
        }
        return -1;
    }

    /** The breadcrumb, deepest last: "Chart - House 1 - Mars - 14 Scorpio". */
    String trail() {
        StringBuilder s = new StringBuilder("Chart");
        if (this.house > 0) {
            s.append("  ›  House ").append(this.house);
        }
        if (this.body >= 0) {
            s.append("  ›  ").append(Bodies.at(this.body).name);
        }
        if (this.degree > 0 && this.sign >= 0) {
            s.append("  ›  ").append(this.degree).append(' ')
                .append(titled(Zodiac.SIGNS[this.sign]));
        }
        return s.toString();
    }

    /**
     * The reading for wherever the reader is standing, as HTML.
     *
     * <p><b>Every level asks the engine, and none of them writes prose here.</b> The corpus
     * already holds a text for each: a house, a planet in its house and in its sign, a degree's
     * Sabian symbol and its technical reading. A phrase composed in this file would be a second
     * voice in an app whose whole content is one.
     *
     * <p>A level with nothing to say returns an empty string rather than a heading over
     * nothing, and the caller decides what to put in the space - which is the honest answer
     * when the corpus has no entry, and better than inventing one.
     */
    String reading(InterpretationService svc) {
        if (svc == null) {
            return "";
        }
        switch (this.level) {
            case HOUSE:
                return section("House " + this.house, svc.getHouse(this.house))
                    + planetsIn(svc);
            case PLANET:
                return planetText(svc);
            case DEGREE:
                return degreeText(svc);
            default:
                return "";
        }
    }

    /** The planets standing in the focused house, so a house reads as what is actually in it. */
    private String planetsIn(InterpretationService svc) {
        if (this.house < 1) {
            return "";
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < PhoneWheel.PLANETS && i < this.frame.bodies.length; i++) {
            ChartFrame.Body b = this.frame.bodies[i];
            if (b == null || !b.ok || houseOf(this.frame, b.lon) != this.house) {
                continue;
            }
            s.append(section(Bodies.at(i).name + " in house " + this.house,
                svc.getPlanetInHouse(Bodies.at(i).name, this.house)));
        }
        if (s.length() == 0) {
            return "<p><i>No planet stands in this house.</i></p>";
        }
        return s.toString();
    }

    private String planetText(InterpretationService svc) {
        if (this.body < 0 || this.body >= this.frame.bodies.length) {
            return "";
        }
        ChartFrame.Body b = this.frame.bodies[this.body];
        if (b == null || !b.ok) {
            return "";
        }
        String name = Bodies.at(this.body).name;
        String signName = Zodiac.SIGNS[(int) (b.lon / 30.0) % 12];
        StringBuilder s = new StringBuilder();
        s.append(section(name + " in " + titled(signName),
            svc.getPlanetInSign(name, signName)));
        if (this.house > 0) {
            s.append(section(name + " in house " + this.house,
                svc.getPlanetInHouse(name, this.house)));
        }
        return s.toString();
    }

    private String degreeText(InterpretationService svc) {
        if (this.sign < 0 || this.degree < 1) {
            return "";
        }
        String signName = Zodiac.SIGNS[this.sign];
        StringBuilder s = new StringBuilder();
        s.append(section(this.degree + " " + titled(signName),
            svc.getSabianFullText(signName, this.degree)));
        s.append(section("The degree", svc.getDegreeFullText(signName, this.degree)));
        return s.toString();
    }

    /**
     * A sign's name as a reader should see it.
     *
     * <p>{@code Zodiac.SIGNS} is lower case because it is a set of CORPUS KEYS - the
     * interpretation files are indexed by them, and {@code getPlanetInSign} would find nothing
     * under "Aries". So the capital is added here, for display, and never on the way to the
     * engine: the two uses are a heading and a lookup, and only one of them is prose.
     */
    private static String titled(String sign) {
        if (sign == null || sign.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(sign.charAt(0)) + sign.substring(1);
    }

    /** A heading and its text, or nothing at all when the corpus has no entry. */
    private static String section(String heading, String body) {
        if (body == null || body.trim().isEmpty()) {
            return "";
        }
        return "<h3>" + heading + "</h3><p>" + body.trim() + "</p>";
    }
}
