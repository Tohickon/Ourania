package com.zodiacomputing.ourania.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Paints one moment of a reading's reveal: the wheel dims, its contributors light, threads run
 * to a focal point, an orb forms there and becomes a card, and the card flies to the reading.
 *
 * <b>A pure function of a scene and one number.</b> Nothing here keeps time or state: {@code t}
 * runs from 0 to 1 across {@link #TOTAL_MS}, and every stage is a window of it. That is what
 * lets {@code RevealCheck} paint any moment into an image and read its pixels, the way the
 * wheel's ring blooms are tested, and what lets {@link ReadingReveal} interrupt the sequence by
 * reversing the one number rather than by unwinding a state machine.
 *
 * <b>It paints over the wheel and never into it.</b> The dimming is a veil with a hole at each
 * contributor, so the wheel's own painter is not told about the reveal at all and cannot draw a
 * different chart because of it.
 */
final class RevealPainter {

    // The storyboard's budget, in milliseconds, in order.
    static final int HIGHLIGHT_MS = 200;
    static final int THREADS_MS = 400;
    static final int ORB_MS = 300;
    static final int CRYSTAL_MS = 300;
    static final int EMERGE_MS = 350;
    static final int TRAVEL_MS = 400;
    static final int TEXT_MS = 250;
    /** The storyboard's sequence: from the first dim to the card's text in place. */
    static final int FORM_MS = HIGHLIGHT_MS + THREADS_MS + ORB_MS + CRYSTAL_MS + EMERGE_MS
        + TRAVEL_MS + TEXT_MS;
    /**
     * How long the landed card stays readable before it fades.
     *
     * <b>Not in the storyboard, and the first build showed why it is needed.</b> The text
     * finished fading in at the very end and the overlay went in the same frame, so David saw
     * the card "for half a second" and could not read it. The real reading lands under the
     * card when it forms; the card then stays on top for this long, as the summary of what
     * just formed, and a click or a key still takes it away at once.
     */
    static final int HOLD_MS = 2500;
    static final int OUT_MS = 300;
    static final int TOTAL_MS = FORM_MS + HOLD_MS + OUT_MS;

    /** Where each stage starts, in ms. */
    static final int AT_THREADS = HIGHLIGHT_MS;
    static final int AT_ORB = AT_THREADS + THREADS_MS;
    static final int AT_CRYSTAL = AT_ORB + ORB_MS;
    static final int AT_EMERGE = AT_CRYSTAL + CRYSTAL_MS;
    static final int AT_TRAVEL = AT_EMERGE + EMERGE_MS;
    static final int AT_TEXT = AT_TRAVEL + TRAVEL_MS;
    static final int AT_HOLD = AT_TEXT + TEXT_MS;
    static final int AT_OUT = AT_HOLD + HOLD_MS;

    /**
     * Where the reading lands, as a fraction of the whole timeline: as the card starts to fly.
     *
     * <b>Before the flight, not after it,</b> because the card flies to the real card on the
     * page, and that card has a place only once the page is there. {@link ReadingReveal}
     * measures it each frame and the painter covers it until the flying card arrives, so the
     * reader sees one card travel into its slot, never two.
     */
    static final double LANDS_AT = AT_TRAVEL / (double) TOTAL_MS;

    /** How far the wheel recedes: a non-contributor draws at 60% of its brightness. */
    static final float DIM = 0.40f;
    /** The clear circle left round each contributor. */
    static final int HOLE_RADIUS = 18;
    static final int ORB_RADIUS = 26;
    static final int CARD_W = 280;
    static final int CARD_H = 110;

    static final Color CARD_FILL = new Color(0x1B, 0x22, 0x30);
    static final Color GOLD = new Color(0xFF, 0xD1, 0x66);
    static final Color LABEL = new Color(0x7F, 0xA7, 0xC9);

    private RevealPainter() {
    }

    /**
     * Everything one reveal is drawn from, in the overlay's own coordinates.
     *
     * @param centre       the wheel's centre
     * @param radius       the wheel's radius: the veil covers this circle
     * @param contributors the screen positions of the chart factors, in the plan's order
     * @param focal        where the orb forms - the centre for a headline theme, the planet's
     *                     own glyph for a placement
     * @param target       where the card lands: the top of the reading pane
     * @param background   the colour the veil darkens towards
     */
    record Scene(Point centre, int radius, List<Point> contributors, Point focal,
            Rectangle target, RevealPlan plan, Color background) {
        Scene {
            contributors = List.copyOf(contributors);
        }

        /** The same scene, landing on {@code r}: where the real card turned out to be. */
        Scene landingOn(Rectangle r) {
            return new Scene(centre, radius, contributors, focal, new Rectangle(r), plan,
                background);
        }
    }

    /** A stage's own progress, 0 before it starts and 1 once it ends. */
    static double stage(double t, int startMs, int lengthMs) {
        double ms = t * TOTAL_MS;
        return Math.max(0.0, Math.min(1.0, (ms - startMs) / lengthMs));
    }

    /**
     * Paints the reveal at {@code t}.
     *
     * @param fade 1 normally; less while an interrupted reveal folds away, so what is left of it
     *             recedes rather than playing backwards at full strength over the real card
     */
    static void paint(Graphics2D g0, Scene s, double t, double fade) {
        fade *= 1.0 - Bloom.smoothstep(0, 1, stage(t, AT_OUT, OUT_MS));
        if (t <= 0.0 || fade <= 0.0) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            double in = Bloom.smoothstep(0, 1, stage(t, 0, HIGHLIGHT_MS));
            double travel = stage(t, AT_TRAVEL, TRAVEL_MS);
            double crystal = stage(t, AT_CRYSTAL, CRYSTAL_MS);

            // Gone before the flight: the page lands then, and opening the rail can move the
            // wheel out from under a veil drawn where it was.
            double emerged = Bloom.smoothstep(0, 1, stage(t, AT_EMERGE, EMERGE_MS));
            veil(g, s, in * (1.0 - emerged) * fade);
            glows(g, s, in * (1.0 - crystal) * fade);
            threads(g, s, t, (1.0 - crystal) * fade);
            counter(g, s, t, fade);
            orbAndCard(g, s, t, fade);
        } finally {
            g.dispose();
        }
    }

    /** The wheel at 60%, with every contributor left clear. */
    private static void veil(Graphics2D g, Scene s, double k) {
        if (k <= 0.0) {
            return;
        }
        Area a = new Area(new Ellipse2D.Double(s.centre.x - s.radius, s.centre.y - s.radius,
            2.0 * s.radius, 2.0 * s.radius));
        for (Point p : s.contributors) {
            a.subtract(new Area(new Ellipse2D.Double(p.x - HOLE_RADIUS, p.y - HOLE_RADIUS,
                2.0 * HOLE_RADIUS, 2.0 * HOLE_RADIUS)));
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (DIM * k)));
        g.setColor(s.background);
        g.fill(a);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static void glows(Graphics2D g, Scene s, double k) {
        if (k <= 0.0) {
            return;
        }
        g.setStroke(new BasicStroke(2f));
        g.setColor(alpha(GOLD, k));
        for (Point p : s.contributors) {
            g.draw(new Ellipse2D.Double(p.x - HOLE_RADIUS + 2, p.y - HOLE_RADIUS + 2,
                2.0 * HOLE_RADIUS - 4, 2.0 * HOLE_RADIUS - 4));
        }
    }

    /** Each contributor's thread grows towards the focal point, one after another. */
    private static void threads(Graphics2D g, Scene s, double t, double k) {
        double p = stage(t, AT_THREADS, THREADS_MS);
        if (p <= 0.0 || k <= 0.0) {
            return;
        }
        int n = s.contributors.size();
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(alpha(GOLD, 0.85 * k));
        for (int i = 0; i < n; i++) {
            double q = Bloom.stagger(p, i, n, 0.5);
            Point a = s.contributors.get(i);
            g.draw(new Line2D.Double(a.x, a.y, a.x + (s.focal.x - a.x) * q,
                a.y + (s.focal.y - a.y) * q));
        }
    }

    /**
     * The Rule of Three, counted where the reader can see it: one tick per technique that
     * counted, then the verdict. Technique names only when the plan carries them. A placement
     * has no ticks and is never confirmed, so it draws nothing here.
     */
    private static void counter(Graphics2D g, Scene s, double t, double fade) {
        double p = stage(t, AT_THREADS, THREADS_MS + ORB_MS);
        double out = stage(t, AT_EMERGE, EMERGE_MS);
        double k = (1.0 - out) * fade;
        if (p <= 0.0 || k <= 0.0) {
            return;
        }
        List<String> ticks = s.plan.ticks();
        boolean labelled = s.plan.labelled();
        // Through Theme.font, like every font in the app: a physical face made directly has no
        // fallback, and the first version drew each check mark as an empty box.
        Font f = Theme.font("Arial", Font.BOLD, 12);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String mark = "✔";
        List<String> parts = new ArrayList<>();
        for (String tick : ticks) {
            parts.add(labelled && tick != null ? tick + " " + mark : mark);
        }
        String verdict = labelled ? "THEME CONFIRMED" : "Confirmed";
        int gap = labelled ? 14 : 8;
        int width = 0;
        for (String part : parts) {
            width += fm.stringWidth(part) + gap;
        }
        int x = s.centre.x - width / 2;
        int y = s.centre.y - Math.max(40, (int) (s.radius * 0.42));
        int shown = (int) Math.floor(p * (ticks.size() + 1) + 1e-9);
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (i < shown) {
                g.setColor(alpha(Color.WHITE, k));
                g.drawString(part, x, y);
            }
            x += fm.stringWidth(part) + gap;
        }
        if (s.plan.confirmed() && shown > ticks.size()) {
            g.setColor(alpha(GOLD, k));
            g.drawString(verdict, s.centre.x - fm.stringWidth(verdict) / 2, y + fm.getHeight() + 2);
        }
    }

    /** The orb, its crystallizing, the card emerging from it, its flight and its text. */
    private static void orbAndCard(Graphics2D g, Scene s, double t, double fade) {
        double orb = stage(t, AT_ORB, ORB_MS);
        if (orb <= 0.0) {
            return;
        }
        double crystal = Bloom.smoothstep(0, 1, stage(t, AT_CRYSTAL, CRYSTAL_MS));
        double emerge = stage(t, AT_EMERGE, EMERGE_MS);
        double travel = Bloom.smoothstep(0, 1, stage(t, AT_TRAVEL, TRAVEL_MS));
        double fx = s.focal.x;
        double fy = s.focal.y;

        if (emerge <= 0.0) {
            // An orb, then a rounded tablet at 70% of the card.
            double r = ORB_RADIUS * Bloom.smoothstep(0, 1, orb);
            double w = lerp(2 * r, CARD_W * 0.7, crystal);
            double h = lerp(2 * r, CARD_H * 0.7, crystal);
            double arc = lerp(2 * r, 20, crystal);
            g.setColor(alpha(lerp(GOLD, CARD_FILL, crystal), fade));
            g.fill(new RoundRectangle2D.Double(fx - w / 2, fy - h / 2, w, h, arc, arc));
            g.setStroke(new BasicStroke(2f));
            g.setColor(alpha(GOLD, fade * (0.6 + 0.4 * (1 - crystal))));
            g.draw(new RoundRectangle2D.Double(fx - w / 2, fy - h / 2, w, h, arc, arc));
            return;
        }

        // 0.7 to 1.05 over the first 60% of the emergence, then settling to 1.0.
        double scale = emerge < 0.6
            ? lerp(0.7, 1.05, Bloom.smoothstep(0, 1, emerge / 0.6))
            : lerp(1.05, 1.0, Bloom.smoothstep(0, 1, (emerge - 0.6) / 0.4));
        Rectangle land = s.target;
        if (stage(t, AT_TRAVEL, TRAVEL_MS) > 0.0) {
            // The real card is on the page from here on. Its slot stays empty until this one
            // arrives in it, and fades with it, so what is left is the real card.
            g.setColor(alpha(CARD_FILL, fade));
            g.fill(land);
        }
        Point2D at = arc(new Point2D.Double(fx, fy),
            new Point2D.Double(land.getCenterX(), land.getCenterY()), travel);
        double w = lerp(CARD_W * scale, land.width, travel);
        double h = lerp(CARD_H * scale, land.height, travel);
        RoundRectangle2D card = new RoundRectangle2D.Double(at.getX() - w / 2, at.getY() - h / 2,
            w, h, 14, 14);
        g.setColor(alpha(CARD_FILL, fade));
        g.fill(card);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(alpha(GOLD, fade * (1.0 - travel)));
        g.draw(card);

        double title = stage(t, AT_TEXT, TEXT_MS / 2);
        double body = stage(t, AT_TEXT + TEXT_MS / 2, TEXT_MS - TEXT_MS / 2);
        // Laid out as ReadingCard.html lays out the real card it lands on: the same three
        // sizes at the page's scale, the label and the headline wrapped, and a blank line in
        // the body a new paragraph. A transit's headline is a sentence; drawn on one line it ran
        // off the card and across the wheel.
        int x = (int) card.getX() + 10;
        int width = (int) w - 20;
        int[] y = {(int) card.getY() + 10};
        double bottom = card.getMaxY() - 4;
        ReadingCard c = s.plan.card();
        text(g, c.label(), Font.BOLD, 10, LABEL, title * fade, x, y, width, bottom);
        y[0] += 4;
        text(g, c.headline(), Font.BOLD, 15, Color.WHITE, title * fade, x, y, width, bottom);
        for (String para : c.body().split("\n\\s*\n")) {
            y[0] += 6;
            text(g, para.trim(), Font.PLAIN, 13, Color.WHITE, body * fade, x, y, width, bottom);
        }
    }

    /**
     * How much larger the reading pane draws a CSS size than a font of the same number: it
     * takes a size in pixels as points at the screen's 96 dpi. Without it the painted card's
     * words were a quarter smaller than the real card's, and jumped when it took over.
     */
    static final float HTML_SCALE = 96f / 72f;

    /**
     * One wrapped block of a card's text at {@code y}, which it moves down. Laid out even
     * while it is invisible, so the body sits where it will be when the title fades in first.
     */
    private static void text(Graphics2D g, String s, int style, int cssSize, Color color,
            double k, int x, int[] y, int width, double bottom) {
        if (s == null || s.isEmpty()) {
            return;
        }
        g.setFont(Theme.font("Arial", style, Math.round(cssSize * HTML_SCALE)));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(alpha(color, k));
        for (String line : wrap(s, fm, width)) {
            y[0] += fm.getHeight();
            if (y[0] > bottom) {
                return;
            }
            if (k > 0.0) {
                g.drawString(line, x, y[0] - fm.getDescent());
            }
        }
    }

    /**
     * A point on the card's flight: a quadratic curve that bows upwards, so the card arcs to
     * the reading rather than sliding across the window in a straight line.
     */
    static Point2D arc(Point2D from, Point2D to, double u) {
        double cx = (from.getX() + to.getX()) / 2.0;
        double cy = Math.min(from.getY(), to.getY()) - 120.0;
        double a = (1 - u) * (1 - u);
        double b = 2 * (1 - u) * u;
        double c = u * u;
        return new Point2D.Double(a * from.getX() + b * cx + c * to.getX(),
            a * from.getY() + b * cy + c * to.getY());
    }

    private static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(next) > width && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }

    private static double lerp(double a, double b, double u) {
        return a + (b - a) * u;
    }

    private static Color lerp(Color a, Color b, double u) {
        return new Color((int) Math.round(lerp(a.getRed(), b.getRed(), u)),
            (int) Math.round(lerp(a.getGreen(), b.getGreen(), u)),
            (int) Math.round(lerp(a.getBlue(), b.getBlue(), u)));
    }

    private static Color alpha(Color c, double k) {
        int a = (int) Math.round(255 * Math.max(0.0, Math.min(1.0, k)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
