package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;

/**
 * A body drawn as itself: the Sun's corona, Jupiter's belts, Saturn's rings, and the plain
 * worlds in their own colours.
 *
 * <p><b>This was private to GlobeRenderer, and the wheel drew grey balls.</b> Every natal bead
 * on the wheel was painted {@code new Color(192, 192, 192)} - the Sun, Jupiter and Vesta were
 * the same object in three sizes, and the only thing that distinguished them was the glyph
 * stamped on top. Meanwhile the globe had all of this, and the phone drew planet pictures from
 * the same code. The desktop wheel was the one surface of three still drawing beads, and the
 * renderer it needed was already written, already shared, and already behind a {@link Pen} so
 * it could not care which screen it was on. It had no door, so this is the door.
 *
 * <p><b>Pen, not Graphics2D</b>, which is what makes it shareable: the desktop passes an
 * AwtPen wrapping its Graphics2D, the phone passes its own. This class is on
 * {@code EngineIsolationCheck.SHARED} and must stay free of AWT - see that suite for what
 * happens to the Android build otherwise.
 *
 * <p><b>Not every body has a face, and that is the point.</b> Much of the registry - the
 * nodes, the Lots, the angles, most asteroids - is not a thing anyone has a picture of.
 * Inventing one would be worse than the glyph. Those answer false to {@link #hasFace} and
 * keep the bead they already had, and the two kinds sit on the ring together without the
 * reader needing to be told which is which.
 */
public final class Planets {

    private Planets() { }

    private static final int FACE_NONE = 0;
    private static final int FACE_SUN = 1;
    private static final int FACE_BANDED = 2;
    private static final int FACE_RINGED = 3;
    private static final int FACE_PLAIN = 4;

    /**
     * Which bodies are drawn as themselves, and how.
     *
     * <b>Only the ones that look like something.</b> A Sun with a corona, a banded Jupiter,
     * Saturn with its rings and the plain worlds are recognisable at nine pixels.
     *
     * Built against the registry rather than written as a switch at the call site, so a body
     * added later lands here as FACE_NONE and keeps its glyph instead of falling through to
     * whatever the last case happened to be.
     *
     * <b>The Moon is deliberately absent.</b> Both surfaces draw it as its own phase disc,
     * which is a truer picture than a lit grey ball would be, and a face here would be drawn
     * underneath and never seen.
     */
    private static final int[] PLANET_FACE = buildFaces();

    private static int[] buildFaces() {
        int[] faces = new int[Bodies.count()];
        for (int i = 0; i < faces.length; i++) {
            switch (Bodies.at(i).name) {
                case "Sun":
                    faces[i] = FACE_SUN;
                    break;
                case "Jupiter":
                    faces[i] = FACE_BANDED;
                    break;
                case "Saturn":
                    faces[i] = FACE_RINGED;
                    break;
                case "Mercury":
                case "Venus":
                case "Mars":
                case "Uranus":
                case "Neptune":
                case "Pluto":
                    faces[i] = FACE_PLAIN;
                    break;
                default:
                    faces[i] = FACE_NONE;
                    break;
            }
        }
        return faces;
    }

    /**
     * True for a body this class can draw as itself.
     *
     * <b>Asked before drawing rather than discovered during it.</b> A caller that cannot draw
     * a bead as a planet has to draw it some other way, and finding that out halfway through
     * {@link #draw} would mean either a wasted disc underneath or a branch in every caller
     * that looks like this one anyway.
     */
    public static boolean hasFace(int body) {
        return body >= 0 && body < PLANET_FACE.length && PLANET_FACE[body] != FACE_NONE;
    }

    /**
     * The face colour of a body, warm to cold.
     *
     * <b>Public because a glyph drawn on top of a face has to be legible against it.</b> The
     * wheel stamps the body's glyph on its bead, and the element ink that was readable on a
     * neutral grey is not readable on the body's own colour - the Sun's glyph is red, and red
     * on a bright orange disc is a smudge. The caller asks what it is drawing on and picks an
     * ink that reads; see SkymapPanel.readableOn, which the angles have used for the same
     * reason since their beads turned gold.
     */
    public static Ink faceColour(int body) {
        switch (Bodies.at(body).name) {
            case "Sun":
                return new Ink(255, 196, 84);
            case "Mercury":
                return new Ink(178, 172, 160);
            case "Venus":
                return new Ink(226, 200, 148);
            case "Mars":
                return new Ink(198, 96, 66);
            case "Jupiter":
                return new Ink(206, 176, 138);
            case "Saturn":
                return new Ink(214, 194, 146);
            case "Uranus":
                return new Ink(150, 206, 208);
            case "Neptune":
                return new Ink(104, 138, 214);
            case "Pluto":
                return new Ink(164, 146, 132);
            default:
                return new Ink(190, 190, 196);
        }
    }

    /**
     * A body's halo: pixels added to its radius, and the fraction of its alpha at that reach.
     *
     * <b>The same shape as GlobeRenderer's rimPasses, and asserted by the same rule</b> - each
     * pass tighter than the last and brighter than the last, so the stack falls off outward
     * and reads as light rather than as a fat ring round a disc. Shared with the suite rather
     * than restated in it, for the reason a surviving mutation taught on 2026-09-21: a check
     * holding its own copy of a rule can only prove the copy agrees with itself.
     *
     * The numbers are small on purpose. A body is 9 to 11 pixels; a halo that reached as far as
     * the Sun's five-ring corona would make twenty-eight of them into one wash of light and the
     * ribbon they sit on would be gone behind it.
     */
    public static float[][] bodyHaloPasses() {
        return new float[][] {
            {10.0f, 0.06f},
            {6.0f, 0.10f},
            {3.0f, 0.16f},
        };
    }

    /**
     * One body drawn as itself.
     *
     * <b>Lit from the upper left, all of them.</b> A disc needs a light to read as a sphere,
     * and the direction has to be the same for every body - a ring of objects lit from
     * different places reads as a mistake even when the reader could not say what is wrong.
     *
     * @param pen   where to draw; the caller's own surface, already positioned
     * @param body  registry index; callers check {@link #hasFace} first
     * @param rad   the disc's radius. The corona and the halo reach BEYOND it - up to twice
     *              it for the Sun - so a caller laying bodies in a band has to keep clearance
     *              for the light as well as for the disc, or the Sun's corona washes over the
     *              ring it is drawn on.
     * @param alpha 0..255, applied to every pass so a fading body fades whole
     * @param lit   draw the selected-body rim: brighter, and twice the width
     * @param halo  draw the soft halo under plain bodies. False while the globe is being
     *              dragged, where it costs more than it gives.
     */
    public static void draw(Pen pen, int body, int x, int y, int rad, int alpha, boolean lit,
                            boolean halo) {
        int face = PLANET_FACE[body];
        Ink base = faceColour(body);
        int d = rad * 2;

        if (face == FACE_SUN) {
            // <b>The corona reaches further and the core is white.</b> It read as one more
            // orange bead: the glow was three faint rings inside the disc's own radius, which
            // is not a corona, it is a soft edge. Five rings out to twice the radius, and a
            // near-white centre, so the Sun is the brightest thing on the ring - which is the
            // one fact about it nobody has to be taught.
            for (int i = 5; i >= 1; i--) {
                int glow = rad + i * 5;
                pen.color(new Ink(255, 186, 82,
                    Math.max(0, Math.min(255, (int) (alpha * 0.16 / i)))));
                pen.fillOval(x - glow, y - glow, glow * 2, glow * 2);
            }
            pen.fillOvalRadial(x - rad, y - rad, d, d, x - rad * 0.22f, y - rad * 0.22f,
                Math.max(1f, rad * 1.5f), new float[] {0f, 0.35f, 0.75f, 1f},
                new Ink[] {new Ink(255, 255, 246).withAlpha(alpha),
                    new Ink(255, 232, 158).withAlpha(alpha),
                    new Ink(255, 168, 56).withAlpha(alpha),
                    new Ink(214, 96, 28).withAlpha(alpha)});
            pen.color(new Ink(255, 214, 130).withAlpha(alpha));
            pen.stroke(lit ? 2.0f : 1.0f);
            pen.drawOval(x - rad, y - rad, d, d);
            return;
        }

        int rw = (int) Math.round(rad * 2.15);
        int rh = Math.max(2, (int) Math.round(rad * 0.52));
        if (face == FACE_RINGED) {
            // Behind the planet, then the planet, then in front - which is the whole reason
            // Saturn reads as Saturn rather than as a disc with a line through it.
            pen.color(new Ink(206, 186, 142).withAlpha((int) (alpha * 0.72)));
            pen.stroke(1.4f);
            pen.drawArc(x - rw, y - rh, rw * 2, rh * 2, 0, 180);
        }

        // <b>A halo in the body's own colour, before the disc.</b> The planets were already
        // spheres - a radial gradient lit from the upper left, Jupiter's belts, Saturn's rings -
        // but only the Sun glowed, so on a lit globe every other body read as a painted bead
        // sitting on the band rather than a light sitting above it. The Sun keeps its own,
        // stronger corona above: it should stay the brightest thing on the ring, which is the
        // one fact about it nobody has to be taught.
        if (halo) {
            for (float[] pass : bodyHaloPasses()) {
                int glow = rad + Math.round(pass[0]);
                pen.color(base.lighter(0.35).withAlpha((int) Math.round(alpha * pass[1])));
                pen.fillOval(x - glow, y - glow, glow * 2, glow * 2);
            }
        }

        pen.fillOvalRadial(x - rad, y - rad, d, d, x - rad * 0.35f, y - rad * 0.35f,
            Math.max(1f, rad * 1.5f), new float[] {0f, 1f},
            new Ink[] {base.lighter(0.45).withAlpha(alpha), base.darker(0.5).withAlpha(alpha)});

        if (face == FACE_BANDED) {
            // Jupiter's belts, clipped to the disc so they end where it does.
            pen.clipOval(x - rad, y - rad, d, d);
            pen.stroke(1.0f);
            for (int i = -2; i <= 2; i++) {
                if (i == 0) {
                    continue;
                }
                int by = y + (int) Math.round(i * rad * 0.34);
                pen.color((i % 2 == 0 ? base.darker(0.72) : base.lighter(0.25))
                    .withAlpha((int) (alpha * 0.85)));
                pen.drawLine(x - rad, by, x + rad, by);
            }
            pen.unclip();
        }

        if (face == FACE_RINGED) {
            pen.color(new Ink(232, 214, 170).withAlpha(alpha));
            pen.stroke(1.6f);
            pen.drawArc(x - rw, y - rh, rw * 2, rh * 2, 180, 180);
        }

        pen.color((lit ? new Ink(255, 238, 170) : base.darker(0.5)).withAlpha(alpha));
        pen.stroke(lit ? 2.0f : 0.8f);
        pen.drawOval(x - rad, y - rad, d, d);
    }
}
