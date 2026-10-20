package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Convergence;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.ThemeConvergence;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

/**
 * The chart reveals the reading, stage 1 (2026-10-16): a headline theme's card forms on the
 * wheel from the chart factors that produced it. Stage 2 (Parts F and G): a placement's card
 * forms at its own planet, from that placement's own reading, with nothing counted.
 *
 * <p><b>Part A is the one that matters most.</b> A reveal meant to earn a reader's trust in the
 * Rule of Three must show what the engine counted and nothing else. So the plan is checked
 * against ThemeConvergence's own families: one tick per family, an echo never a tick, and
 * "confirmed" exactly when the Rule of Three is met.
 *
 * <p><b>Parts B and C paint moments and read pixels</b>, the way the wheel's ring blooms are
 * tested: the painter is a function of one number, so each stage can be drawn exactly, with no
 * clock. Part D plays the real overlay on a Swing timer and interrupts it. It polls for an
 * observed state rather than sleeping a fixed time, for the reason BloomCheck gives.
 */
public final class RevealCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final Color GREY = new Color(128, 128, 128);

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jd = new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay();
        ChartFrame f = ChartFrame.compute(sw, jd, 41.8781, -87.6298, 'P', false, 0.0);

        part("Part A: the plan is the engine's verdict", () -> thePlan(f));
        part("Part B: the stages, painted and read", RevealCheck::theStages);
        part("Part C: plain view names no technique", RevealCheck::theLabels);
        part("Part D: interrupted, unasked, and with motion off", RevealCheck::thePlayback);
        part("Part E: only a request plays it", RevealCheck::theTrigger);
        part("Part F: a placement forms at its planet, from its own reading", RevealCheck::thePlacement);
        part("Part G: the real wheel's placement factors and focal point", RevealCheck::theRealWheel);
        part("Part H: the card lands on a real card that stays", RevealCheck::theRealCard);
        part("Part I: transit cards pop up when time stops", () -> theTransitPopups(sw));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("RevealCheck: ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("RevealCheck: FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String s : failures) {
                System.out.println("  - " + s);
            }
            System.exit(1);
        }
        System.exit(0);
    }

    // ------------------------------------------------------------------ A

    private static List<ThemeConvergence.Result> threeForCareer(ChartFrame f) {
        // The Midheaven gets a transit and a solar arc; the solar arc wakes it, so the transit
        // counts. Saturn gets a progression. A second transit to the Midheaven is an echo, and
        // a transit to Saturn repeats a family already counted.
        return ThemeConvergence.themes(f, null, List.of(
            target("MC", w(Convergence.Family.TRANSIT, true, "Pluto"),
                w(Convergence.Family.SOLAR_ARC, true, "Sun"),
                w(Convergence.Family.STATION, false, "Pluto")),
            target("Saturn", w(Convergence.Family.PROGRESSION, true, "Mars"),
                w(Convergence.Family.TRANSIT, true, "Uranus"))));
    }

    private static ThemeConvergence.Result career(List<ThemeConvergence.Result> themes) {
        for (ThemeConvergence.Result r : themes) {
            if (r.theme == ThemeConvergence.Theme.CAREER) {
                return r;
            }
        }
        throw new IllegalStateException("no Career theme");
    }

    private static void thePlan(ChartFrame f) {
        ThemeConvergence.Result r = career(threeForCareer(f));
        yes("the fixture is a headline, " + r.families, r.headline());

        java.util.Set<Convergence.Family> voted = java.util.EnumSet.noneOf(Convergence.Family.class);
        int first = 0;
        for (ThemeConvergence.Vote v : r.votes) {
            voted.add(v.family());
            first += v.first() ? 1 : 0;
        }
        eq("the votes name exactly the families", r.families.toString(), voted.toString());
        eq("one first vote per family", r.families.size(), first);
        yes("an echo never votes", r.votes.stream().noneMatch(
            v -> v.family() == Convergence.Family.STATION));
        eq("a repeated family votes but is not first", 4, r.votes.size());

        RevealPlan astro = RevealPlan.forTheme(r, true);
        RevealPlan plain = RevealPlan.forTheme(r, false);
        eq("one tick per family", r.families.size(), astro.ticks().size());
        eq("in the order the engine counted them", "[Transit, Solar arc, Progression]",
            astro.ticks().toString());
        yes("confirmed is the Rule of Three", astro.confirmed() == r.headline());
        eq("the factors are the points voted on", "[MC, Saturn]", astro.points().toString());
        eq("the title is the theme's plain name", ThemeConvergence.Theme.CAREER.label, astro.title());
        InterpretationService svc = InterpretationService.getInstance();
        eq("the body is what the year asks, the life area the app already names, and what to do",
            svc.getTransitCardPart("year.career.asks") + " Shows up in: " + LifeAreas.of(10)
                + ".\n\n" + RevealPlan.WHAT_TO_DO_THIS_YEAR
                + svc.getTransitCardPart("year.career.todo"), astro.body());
        yes("plain view carries no technique names", !plain.labelled());
        eq("but the same number of ticks", astro.ticks().size(), plain.ticks().size());

        // Two families is a background trend: no reveal, and the first headline is skipped to.
        List<ThemeConvergence.Result> two = ThemeConvergence.themes(f, null, List.of(
            target("MC", w(Convergence.Family.SOLAR_ARC, true, "Sun"),
                w(Convergence.Family.TRANSIT, true, "Pluto"))));
        ThemeConvergence.Result trend = career(two);
        yes("a two-family theme is not a headline", !trend.headline());
        yes("and has no plan", RevealPlan.forTheme(trend, true) == null);
        yes("so a year of trends reveals nothing", RevealPlan.firstHeadline(two, true) == null);
        yes("no themes at all reveals nothing", RevealPlan.firstHeadline(null, true) == null);
        RevealPlan firstOfThree = RevealPlan.firstHeadline(threeForCareer(f), false);
        yes("the first headline is the one revealed",
            firstOfThree != null && firstOfThree.title().equals(ThemeConvergence.Theme.CAREER.label));
    }

    // ------------------------------------------------------------------ B

    private static final int W = 720;
    private static final int H = 400;
    private static final Point CENTRE = new Point(170, 200);
    private static final Point C1 = new Point(80, 200);
    private static final Point C2 = new Point(260, 200);
    /** Inside the wheel and away from every contributor, thread, counter and card. */
    private static final Point ELSEWHERE = new Point(170, 330);
    private static final Rectangle TARGET = new Rectangle(440, 30, 260, RevealPainter.CARD_H);

    private static RevealPlan plan(boolean astrologer) {
        return new RevealPlan("Career and status", "Shows up in: career, reputation and public standing.",
            astrologer ? List.of("Transit", "Solar arc", "Profection")
                : java.util.Arrays.asList(null, null, null), true, List.of("MC", "Saturn"));
    }

    private static RevealPainter.Scene scene(RevealPlan p) {
        return new RevealPainter.Scene(CENTRE, 150, List.of(C1, C2), CENTRE, TARGET, p, Color.BLACK);
    }

    private static BufferedImage at(double ms, RevealPlan p) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(GREY);
        g.fillRect(0, 0, W, H);
        RevealPainter.paint(g, scene(p), ms / RevealPainter.TOTAL_MS, 1.0);
        g.dispose();
        return img;
    }

    private static void theStages() {
        eq("the forming is the storyboard's 2.2 s", 2200, RevealPainter.FORM_MS);
        RevealPlan p = plan(true);

        BufferedImage start = at(0, p);
        eq("at the start nothing is painted", GREY.getRGB(), start.getRGB(ELSEWHERE.x, ELSEWHERE.y));

        BufferedImage lit = at(RevealPainter.AT_THREADS, p);
        near("a non-contributor draws at 60%", 77, red(lit, ELSEWHERE), 3);
        near("a contributor keeps its full brightness", 128, red(lit, C1), 2);
        near("outside the wheel is untouched", 128, red(lit, new Point(400, 380)), 0);

        BufferedImage threads = at(RevealPainter.AT_ORB - 1, p);
        Point onThread = new Point((C1.x + CENTRE.x) / 2, CENTRE.y);
        yes("a thread runs from a contributor to the focal point, saw " + hex(threads, onThread),
            red(threads, onThread) > 200 && blue(threads, onThread) < 160);
        BufferedImage halfway = at(RevealPainter.AT_THREADS + 40, p);
        yes("and it grows rather than appearing whole",
            red(halfway, new Point(CENTRE.x - 12, CENTRE.y)) < 150);

        BufferedImage orb = at(RevealPainter.AT_CRYSTAL, p);
        yes("an orb forms at the focal point, saw " + hex(orb, CENTRE),
            red(orb, CENTRE) > 200 && green(orb, CENTRE) > 160);

        BufferedImage card = at(RevealPainter.AT_TRAVEL, p);
        Point inCard = new Point(CENTRE.x + RevealPainter.CARD_W / 2 - 12, CENTRE.y);
        eq("the card has emerged at full size", RevealPainter.CARD_FILL.getRGB(),
            card.getRGB(inCard.x, inCard.y));
        BufferedImage overshoot = at(RevealPainter.AT_EMERGE + (int) (RevealPainter.EMERGE_MS * 0.6), p);
        Point justOutside = new Point(CENTRE.x + RevealPainter.CARD_W / 2 + 4, CENTRE.y);
        eq("and overshoots to 1.05 on the way", RevealPainter.CARD_FILL.getRGB(),
            overshoot.getRGB(justOutside.x, justOutside.y));
        yes("before settling at 1.0", card.getRGB(justOutside.x, justOutside.y)
            != RevealPainter.CARD_FILL.getRGB());

        Point2D mid = RevealPainter.arc(CENTRE, new Point2D.Double(TARGET.getCenterX(),
            TARGET.getCenterY()), 0.5);
        yes("the flight bows upward rather than sliding", mid.getY() < Math.min(CENTRE.y, TARGET.getCenterY()));

        BufferedImage landed = at(RevealPainter.AT_TEXT, p);
        Point inTarget = new Point(TARGET.x + TARGET.width - 20, TARGET.y + TARGET.height - 12);
        eq("the card lands on the reading", RevealPainter.CARD_FILL.getRGB(),
            landed.getRGB(inTarget.x, inTarget.y));
        near("and the wheel is itself again", 128, red(landed, ELSEWHERE), 1);
        near("with the focal point clear", 128, red(landed, CENTRE), 1);
        eq("no text before the text stage", 0, lit(landed, TARGET));
        int titleOnly = lit(at(RevealPainter.AT_TEXT + RevealPainter.TEXT_MS / 2, p), TARGET);
        int all = lit(at(RevealPainter.AT_HOLD, p), TARGET);
        yes("the title fades in first, saw " + titleOnly, titleOnly > 30);
        yes("then the body, saw " + all, all > titleOnly + 30);

        // The landed card holds, readable, and only then fades.
        int held = lit(at(RevealPainter.AT_OUT - 1, p), TARGET);
        eq("the card holds, all its text still there", all, held);
        yes("it holds for at least two seconds", RevealPainter.HOLD_MS >= 2000);
        eq("the hold card still fills the landing", RevealPainter.CARD_FILL.getRGB(),
            at(RevealPainter.AT_OUT - 1, p).getRGB(inTarget.x, inTarget.y));
        BufferedImage gone = at(RevealPainter.TOTAL_MS, p);
        eq("then fades away entirely", GREY.getRGB(), gone.getRGB(inTarget.x, inTarget.y));
        yes("and the reading lands as the card starts to fly, so it has a real card to land on",
            Math.abs(RevealPainter.LANDS_AT * RevealPainter.TOTAL_MS - RevealPainter.AT_TRAVEL) < 1e-6);
    }

    // ------------------------------------------------------------------ C

    private static void theLabels() {
        int ms = RevealPainter.AT_CRYSTAL - 1;
        // The counter's row, stopping above where the emerged card's top edge will be.
        Rectangle band = new Rectangle(0, CENTRE.y - 90,
            2 * CENTRE.x, 90 - RevealPainter.CARD_H / 2 - 2);
        BufferedImage astro = at(ms, plan(true));
        BufferedImage plain = at(ms, plan(false));
        int a = lit(astro, band);
        int p = lit(plain, band);
        yes("the counter is drawn in both views, saw " + p, p > 30);
        yes("plain view draws far less of it: no technique names, saw " + p + " against " + a,
            a > 2 * p);
        BufferedImage early = at(RevealPainter.AT_THREADS + 20, plan(true));
        yes("the ticks arrive one by one", lit(early, band) < a / 2);
        BufferedImage gone = at(RevealPainter.AT_TRAVEL, plan(true));
        eq("and the counter is gone before the flight", 0, lit(gone, band));
    }

    // ------------------------------------------------------------------ D

    private static void thePlayback() throws Exception {
        Settings.setAnimateRings(true);
        Settings.setAnimateReadings(true);
        RevealPlan p = plan(false);
        yes("a request with a plan plays", ReadingReveal.shouldPlay(true, p));
        yes("a rebuild never does", !ReadingReveal.shouldPlay(false, p));
        yes("nor a reading with no headline", !ReadingReveal.shouldPlay(true, null));
        Settings.setAnimateReadings(false);
        yes("with the reveal turned off, nothing plays", !ReadingReveal.shouldPlay(true, p));
        Settings.setAnimateReadings(true);
        Settings.setAnimateRings(false);
        yes("nor with the app's reduced motion on", !ReadingReveal.shouldPlay(true, p));
        Settings.setAnimateRings(true);

        JLayeredPane layer = new JLayeredPane();
        layer.setSize(W, H);
        ReadingReveal[] reveal = new ReadingReveal[1];
        AtomicInteger landed = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            reveal[0] = new ReadingReveal(layer);
            reveal[0].play(scene(p), landed::incrementAndGet);
        });
        yes("playing puts the overlay on the layered pane", layer.getComponentCount() == 1);
        double partway = 0.0;
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            double v = reveal[0].progress();
            if (v > 0.05 && v < 0.6 * RevealPainter.LANDS_AT) {
                partway = v;
                break;
            }
            Thread.sleep(10);
        }
        yes("the reveal passes through the middle, saw " + partway, partway > 0.0);
        eq("and has not landed yet", 0, landed.get());
        double[] after = new double[1];
        SwingUtilities.invokeAndWait(() -> {
            reveal[0].interrupt();
            after[0] = reveal[0].progress();
        });
        eq("interrupting lands the reading at once", 1, landed.get());
        yes("and folds from where it was, not from the end",
            after[0] <= partway + 0.05 && after[0] > 0.0);
        long folding = System.currentTimeMillis();
        while (reveal[0].playing() && System.currentTimeMillis() < folding + 3000) {
            Thread.sleep(10);
        }
        long took = System.currentTimeMillis() - folding;
        yes("the fold finishes and takes the overlay away", !reveal[0].playing()
            && layer.getComponentCount() == 0);
        yes("quickly, in " + took + " ms", took < ReadingReveal.FOLD_MS + 600);
        eq("and the reading landed once, not again", 1, landed.get());

        // A whole reveal, uninterrupted, lands once at its end.
        AtomicInteger second = new AtomicInteger();
        long began = System.currentTimeMillis();
        boolean[] landedWhilePlaying = new boolean[1];
        SwingUtilities.invokeAndWait(() -> reveal[0].play(scene(p), second::incrementAndGet));
        while (reveal[0].playing() && System.currentTimeMillis() < began + 12000) {
            if (second.get() == 1) {
                landedWhilePlaying[0] = true;
            }
            Thread.sleep(10);
        }
        long whole = System.currentTimeMillis() - began;
        yes("the reading landed while the card was still on screen", landedWhilePlaying[0]);
        eq("an uninterrupted reveal lands once", 1, second.get());
        yes("after its whole sequence, " + whole + " ms", whole >= RevealPainter.TOTAL_MS - 100);
        yes("and leaves nothing on the window", layer.getComponentCount() == 0);

        // A click during the hold takes the card away at once, with no flight back.
        AtomicInteger h = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> reveal[0].play(scene(p), h::incrementAndGet));
        long holdBy = System.currentTimeMillis() + 8000;
        while (h.get() == 0 && System.currentTimeMillis() < holdBy) {
            Thread.sleep(10);
        }
        yes("the reading lands, and the card holds", h.get() == 1 && reveal[0].playing());
        SwingUtilities.invokeAndWait(() -> reveal[0].interrupt());
        yes("a click during the hold removes the card at once",
            !reveal[0].playing() && layer.getComponentCount() == 0);
        eq("without landing the reading again", 1, h.get());

        // A second request while the first plays: the first lands, then the second plays.
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            reveal[0].play(scene(p), a::incrementAndGet);
            reveal[0].play(scene(p), b::incrementAndGet);
        });
        eq("a new request lands the old reading first", 1, a.get());
        yes("and plays its own", reveal[0].playing() && b.get() == 0);
        eq("with one overlay, not two", 1, layer.getComponentCount());
        SwingUtilities.invokeAndWait(() -> reveal[0].interrupt());
        eq("which lands when interrupted", 1, b.get());
    }

    // ------------------------------------------------------------------ E

    private static void theTrigger() throws Exception {
        Path src = Path.of("src/main/java/com/zodiacomputing/ourania/gui/SkymapPanel.java");
        if (!Files.exists(src)) {
            System.out.println("  (source not found from this directory; Part E skipped)");
            return;
        }
        String s = Files.readString(src);
        String refresh = body(s, "private void refreshReadingIfShown()");
        yes("the rebuild interrupts a reveal", refresh.contains("this.reveal.interrupt()"));
        yes("and never asks for one", !refresh.contains("askForReading"));
        yes("the Synthesize button asks",
            s.contains("jButton9.addActionListener(actionEvent -> this.askForReading(ReadingTier.SYNTHESIZE))"));
        yes("so does the sidebar", body(s, "public void runReading(String name)")
            .contains("this.askForReading(ReadingTier.SYNTHESIZE)"));
        String show = body(s, "void showReading(final ReadingTier readingTier)");
        yes("a request is read once and cleared", show.contains("this.readingAsked = false;"));
        yes("the reveal is gated by shouldPlay", s.contains("ReadingReveal.shouldPlay(asked, plan)"));
        yes("and the batch reports do not ask",
            !Files.readString(src.resolveSibling("BatchReports.java")).contains("askForReading"));

        // Stage 2: a single click on the wheel asks; nothing else does.
        String clicked = body(s, "public void mouseClicked(MouseEvent mouseEvent)");
        yes("a single click on the wheel asks for its placement",
            clicked.contains("SkymapPanel.this.askForPlacement(at.x, at.y)"));
        yes("the hover never asks", !body(s, "public void mouseMoved(MouseEvent").contains("askFor"));
        yes("nor does the rebuild", !refresh.contains("askForPlacement"));
        String ask = body(s, "void askForPlacement(int x, int y)");
        yes("the click's request is cleared on every exit",
            ask.contains("finally") && ask.contains("this.readingAsked = false;"));
        String place = body(s, "private boolean revealPlacement(int body, String card)");
        yes("a placement is gated by shouldPlay",
            place.contains("ReadingReveal.shouldPlay(asked, plan)"));
        yes("and forms at its planet, not at the centre",
            place.contains("this.revealScene(plan, body)"));
        yes("and lands the very card the click built",
            place.contains("this.window.showSelection(card, true)"));
        yes("only the placement exit reads the request",
            !body(s, "private void handleChartClick(int n, int n2)").contains("readingAsked"));
    }

    // ------------------------------------------------------------------ F

    /** A placement on the Part B wheel: it forms at P, away from the centre, from P and C1. */
    private static final Point P = new Point(240, 260);

    private static final String READING = "<html><body>"
        + "<p style='font-size:14px;'>Venus is what you value. It draws people near.</p>"
        + "<h2 style='color:#E0E0E0;'><b>Venus in Libra</b></h2>"
        + "<p>Venus in Libra seeks balance. It weighs.</p>"
        + "<h2><b>Active Aspects</b></h2><h3><i>Square</i> to Natal Mars</h3><p>Friction.</p>"
        + "</body></html>";

    private static RevealPlan placement() {
        return RevealPlan.forPlacement("Venus", List.of(
            new String[] {"Mars", "Square", "Applying"},
            new String[] {"Moon", "Trine", ""},
            new String[] {"Mars", "Square", "Applying"}), READING);
    }

    private static RevealPainter.Scene placed(RevealPlan p) {
        return new RevealPainter.Scene(CENTRE, 150, List.of(P, C1), P, TARGET, p, Color.BLACK);
    }

    private static BufferedImage placedAt(double ms, RevealPlan p) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(GREY);
        g.fillRect(0, 0, W, H);
        RevealPainter.paint(g, placed(p), ms / RevealPainter.TOTAL_MS, 1.0);
        g.dispose();
        return img;
    }

    private static void thePlacement() throws Exception {
        RevealPlan p = placement();
        eq("the title is the reading's own heading", "Venus in Libra", p.title());
        eq("the body is the first sentence under that heading", "Venus in Libra seeks balance.",
            p.body());
        eq("the factors are the planet and the reading's aspects, each once",
            "[Venus, Mars, Moon]", p.points().toString());
        yes("nothing is counted", p.ticks().isEmpty());
        yes("and nothing is confirmed", !p.confirmed());
        yes("no reading, no plan", RevealPlan.forPlacement("Venus", List.of(), "") == null);
        yes("a reading with no heading has no plan",
            RevealPlan.forPlacement("Venus", List.of(), "<p>Only words.</p>") == null);
        eq("a placement with no aspects is the planet alone", "[Venus]",
            RevealPlan.forPlacement("Venus", null, READING).points().toString());

        Settings.setAnimateRings(true);
        Settings.setAnimateReadings(true);
        yes("a clicked placement plays", ReadingReveal.shouldPlay(true, p));
        yes("a placement never plays unasked", !ReadingReveal.shouldPlay(false, p));
        Settings.setAnimateReadings(false);
        yes("nor with the reveal turned off", !ReadingReveal.shouldPlay(true, p));
        Settings.setAnimateReadings(true);
        Settings.setAnimateRings(false);
        yes("nor with reduced motion", !ReadingReveal.shouldPlay(true, p));
        Settings.setAnimateRings(true);

        // Pixels at fixed moments.
        BufferedImage lit = placedAt(RevealPainter.AT_THREADS, p);
        near("the planet itself stays bright", 128, red(lit, P), 2);
        near("the wheel dims round it", 77, red(lit, ELSEWHERE), 3);
        BufferedImage threads = placedAt(RevealPainter.AT_ORB - 1, p);
        Point onThread = new Point((C1.x + P.x) / 2, (C1.y + P.y) / 2);
        yes("a thread runs from the aspecting body to the planet, saw " + hex(threads, onThread),
            red(threads, onThread) > 200 && blue(threads, onThread) < 160);
        BufferedImage orb = placedAt(RevealPainter.AT_CRYSTAL, p);
        yes("the orb forms at the planet, saw " + hex(orb, P),
            red(orb, P) > 200 && green(orb, P) > 160);
        near("and not at the chart's centre", 77, red(orb, CENTRE), 3);
        BufferedImage card = placedAt(RevealPainter.AT_TRAVEL, p);
        eq("the card emerges round the planet", RevealPainter.CARD_FILL.getRGB(),
            card.getRGB(P.x + RevealPainter.CARD_W / 2 - 12, P.y));
        BufferedImage landed = placedAt(RevealPainter.AT_TEXT, p);
        eq("and lands on the reading", RevealPainter.CARD_FILL.getRGB(),
            landed.getRGB(TARGET.x + TARGET.width - 20, TARGET.y + TARGET.height - 12));
        int words = lit(placedAt(RevealPainter.FORM_MS, p), TARGET);
        yes("with the reading's words on it, saw " + words, words > 60);
        int held = lit(placedAt(RevealPainter.AT_OUT - 1, p), TARGET);
        yes("and it holds like a theme's card, saw " + held, held > 60);

        // No counter: the band Part C reads the ticks in stays empty for a whole placement.
        Rectangle band = new Rectangle(0, CENTRE.y - 90,
            2 * CENTRE.x, 90 - RevealPainter.CARD_H / 2 - 2);
        int themeTicks = lit(at(RevealPainter.AT_CRYSTAL - 1, plan(false)), band);
        yes("a theme draws its counter there, saw " + themeTicks, themeTicks > 30);
        int most = 0;
        for (int ms = RevealPainter.AT_THREADS; ms < RevealPainter.AT_EMERGE; ms += 50) {
            most = Math.max(most, lit(placedAt(ms, p), band));
        }
        eq("a placement draws no counter at any moment", 0, most);

        // The click that asks must not land its own reveal; the next press must.
        JLayeredPane layer = new JLayeredPane();
        layer.setSize(W, H);
        javax.swing.JComponent wheel = new javax.swing.JPanel();
        wheel.setBounds(0, 0, W, H);
        layer.add(wheel, JLayeredPane.DEFAULT_LAYER);
        ReadingReveal reveal = new ReadingReveal(layer);
        AtomicInteger landedCount = new AtomicInteger();
        wheel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                reveal.play(placed(p), landedCount::incrementAndGet);
            }
        });
        SwingUtilities.invokeAndWait(() -> {
            press(wheel, java.awt.event.MouseEvent.MOUSE_PRESSED);
            press(wheel, java.awt.event.MouseEvent.MOUSE_RELEASED);
            press(wheel, java.awt.event.MouseEvent.MOUSE_CLICKED);
        });
        SwingUtilities.invokeAndWait(() -> { });
        yes("the click that asked starts the reveal", reveal.playing());
        eq("and does not land it", 0, landedCount.get());
        SwingUtilities.invokeAndWait(() -> press(wheel, java.awt.event.MouseEvent.MOUSE_PRESSED));
        eq("the next press lands it", 1, landedCount.get());
        SwingUtilities.invokeAndWait(() -> reveal.interrupt());
        eq("once", 1, landedCount.get());
    }

    private static void press(java.awt.Component c, int id) {
        c.dispatchEvent(new java.awt.event.MouseEvent(c, id, System.currentTimeMillis(), 0, P.x,
            P.y, 1, false, java.awt.event.MouseEvent.BUTTON1));
    }

    // ------------------------------------------------------------------ G

    /**
     * The real wheel, not shown: for each classical planet, the plan its click makes and the
     * scene it is drawn in. The window is never put on the screen, so the click path itself
     * shows the card at once here, which is checked too.
     */
    private static void theRealWheel() throws Exception {
        OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SkymapPanel sky = (SkymapPanel) CheckReflect.get(w[0], "skymapPanel");
            java.awt.Component chart = (java.awt.Component) CheckReflect.get(sky, "chartPanel");
            SwingUtilities.invokeAndWait(() -> chart.setSize(900, 820));
            JLayeredPane lp = w[0].getLayeredPane();
            java.lang.reflect.Method bodyAt = SkymapPanel.class.getDeclaredMethod("bodyAt",
                int.class, int.class);
            bodyAt.setAccessible(true);
            String[] names = {"Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn"};
            int planets = 0;
            int factors = 0;
            for (String name : names) {
                int i = com.zodiacomputing.ourania.astro.Bodies.indexOfName(name);
                if (i < 0 || !sky.natalRing.valid[i]) {
                    continue;
                }
                planets++;
                String[] reading = new String[1];
                SwingUtilities.invokeAndWait(() -> {
                    sky.selectionHtml(i, sky.natalRing.lon[i], sky.natalRing.speed[i], false);
                    try {
                        reading[0] = (String) CheckReflect.get(sky, "selectionReading");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                List<String[]> aspects = sky.getActiveAspectsFor(i, false);
                RevealPlan plan = RevealPlan.forPlacement(SkymapPanel.BODY_NAMES[i], aspects,
                    reading[0]);
                if (plan == null) {
                    yes(name + " has a plan", false);
                    continue;
                }
                String text = squash(reading[0].replaceAll("<[^>]+>", " "));
                eq(name + ": the first factor is the planet", SkymapPanel.BODY_NAMES[i],
                    plan.points().get(0));
                yes(name + ": the title is in its reading, " + plan.title(),
                    text.contains(squash(plan.title())));
                yes(name + ": so is the body, " + plan.body(), plan.body().isEmpty()
                    || squash(reading[0].replaceAll("<[^>]+>", "")).contains(squash(plan.body())));
                yes(name + ": nothing is counted", plan.ticks().isEmpty() && !plan.confirmed());

                // Each factor is a body the wheel lights for this focus, by the same gate.
                sky.focusBody = i;
                sky.focusTransit = false;
                List<String> unlit = new ArrayList<>();
                List<String> unnamed = new ArrayList<>();
                for (String f : plan.points().subList(1, plan.points().size())) {
                    factors++;
                    int j = com.zodiacomputing.ourania.astro.Bodies.indexOfName(f);
                    if (j < 0) {
                        unlit.add(f);
                        continue;
                    }
                    double sep = com.zodiacomputing.ourania.astro.Aspects.separation(
                        sky.natalRing.lon[i], sky.natalRing.lon[j]);
                    if (sky.glyphWeight(j, false) != 1.0
                            || sky.visibleAspect(sep, i, j, sky.profileForPair(false)) == null) {
                        unlit.add(f);
                    }
                    if (!text.contains(squash(f))) {
                        unnamed.add(f);
                    }
                }
                eq(name + ": every factor is lit by glyphWeight's gate", "[]", unlit.toString());
                eq(name + ": and named in the reading", "[]", unnamed.toString());
                eq(name + ": and they are the reading's aspects, each once", aspects.stream()
                    .map(a -> a[0]).distinct().count(), (long) plan.points().size() - 1);
                sky.focusBody = -1;

                // The scene: its focal point is the planet's glyph, which the hit test finds.
                RevealPainter.Scene[] scene = new RevealPainter.Scene[1];
                SwingUtilities.invokeAndWait(() -> scene[0] = sky.sceneOn(plan, i, lp));
                if (scene[0] == null) {
                    yes(name + " has a scene", false);
                    continue;
                }
                Point focal = SwingUtilities.convertPoint(lp, scene[0].focal(), chart);
                Point wheelAt = sky.toWheel(focal.x, focal.y);
                int hit = (Integer) bodyAt.invoke(sky, wheelAt.x, wheelAt.y);
                eq(name + ": the card forms on its own glyph", i, hit);
                yes(name + ": not at the centre", !scene[0].focal().equals(scene[0].centre()));
                eq(name + ": and the planet is the first contributor", scene[0].focal(),
                    scene[0].contributors().get(0));
                eq(name + ": every factor has a place on the wheel", plan.points().size(),
                    scene[0].contributors().size());
            }
            yes("the planets were read, " + planets, planets >= 5);
            yes("and between them had aspects to light, " + factors, factors > 0);

            // Zoomed in and off centre, the card still forms on the glyph the reader sees.
            int sun = com.zodiacomputing.ourania.astro.Bodies.indexOfName("Sun");
            SwingUtilities.invokeAndWait(() -> {
                Point s = SwingUtilities.convertPoint(lp, sky.sceneOn(placement(), sun, lp).focal(),
                    chart);
                // A little off the glyph, so the zoom also pans the wheel.
                sky.zoomAt(s.x + 40, s.y + 30, -5);
            });
            yes("the wheel is zoomed for this, " + sky.view.zoom(), sky.view.zoom() > 2.0
                && !sky.viewIsFit());
            int seen = 0;
            List<String> missed = new ArrayList<>();
            for (String name : names) {
                int i = com.zodiacomputing.ourania.astro.Bodies.indexOfName(name);
                if (i < 0 || !sky.natalRing.valid[i]) {
                    continue;
                }
                RevealPainter.Scene[] scene = new RevealPainter.Scene[1];
                SwingUtilities.invokeAndWait(() -> scene[0] = sky.sceneOn(placement(), i, lp));
                Point focal = SwingUtilities.convertPoint(lp, scene[0].focal(), chart);
                if (!new Rectangle(0, 0, chart.getWidth(), chart.getHeight()).contains(focal)) {
                    continue;
                }
                seen++;
                Point wheelAt = sky.toWheel(focal.x, focal.y);
                if ((Integer) bodyAt.invoke(sky, wheelAt.x, wheelAt.y) != i) {
                    missed.add(name);
                }
            }
            yes("some planets are still on screen, " + seen, seen > 0);
            eq("each forms on its zoomed glyph", "[]", missed.toString());
            SwingUtilities.invokeAndWait(() -> sky.fitView());

            // A synthesis still forms at the centre: the focal point is a parameter, not a change.
            RevealPainter.Scene[] centred = new RevealPainter.Scene[1];
            SwingUtilities.invokeAndWait(() -> centred[0] = sky.sceneOn(plan(false),
                SkymapPanel.REVEAL_AT_CENTRE, lp));
            yes("a headline theme still forms at the centre", centred[0] != null
                && centred[0].focal().equals(centred[0].centre()));

            // Off screen there is nothing to watch, so the click shows its card at once.
            Settings.setAnimateRings(true);
            Settings.setAnimateReadings(true);
            int venus = com.zodiacomputing.ourania.astro.Bodies.indexOfName("Venus");
            javax.swing.JEditorPane selection = (javax.swing.JEditorPane)
                CheckReflect.get(w[0], "selectionPane");
            String[] shown = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                selection.setText("<html><body>rc-nothing-opened</body></html>");
                RevealPainter.Scene none = sky.revealScene(placement(), venus);
                Point v = SwingUtilities.convertPoint(lp,
                    sky.sceneOn(placement(), venus, lp).focal(), chart);
                Point at = sky.toWheel(v.x, v.y);
                sky.askForPlacement(at.x, at.y);
                shown[0] = none == null ? selection.getText() : "a scene off screen";
            });
            yes("a wheel that is not on screen has no scene, and the card opens at once",
                !shown[0].contains("rc-nothing-opened") && !shown[0].contains("off screen"));
            yes("and the click left no request behind",
                !(Boolean) CheckReflect.get(sky, "readingAsked"));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    // ------------------------------------------------------------------ H

    /**
     * David, 2026-10-17: the card "disappears too soon and makes no sense". It landed on a
     * page that said something else. Now the page opens on the same card, as real HTML, and
     * the flying card lands on that card's own box.
     */
    private static void theRealCard() throws Exception {
        RevealPlan p = placement();
        ReadingCard c = p.card();
        eq("the card's label is the title, as the painter draws it", "VENUS IN LIBRA", c.label());
        eq("its headline is the title", p.title(), c.headline());
        eq("its body is the plan's", p.body(), c.body());
        String page = p.onTop("<html><body style='color:white;'><p>The rest.</p></body></html>");
        int card = page.indexOf("id='" + RevealPlan.CARD_ID + "'");
        yes("the page opens on the card", card > 0 && card < page.indexOf("The rest."));
        yes("straight after its body tag", page.indexOf("<body style='color:white;'><div id=") > 0);
        yes("rendered by the cards' own renderer",
            page.contains(ReadingCard.html(List.of(c), false)));
        yes("a page with no body tag still gets it first", p.onTop("<p>x</p>").startsWith("<div id="));

        // The painter: the wheel is clear before the flight, and the slot is held from landing.
        BufferedImage flying = placedAt(RevealPainter.AT_TRAVEL + 60, p);
        near("the wheel is itself again when the card flies", 128, red(flying, ELSEWHERE), 1);
        Point corner = new Point(TARGET.x + 3, TARGET.y + 3);
        eq("before landing the page is untouched", GREY.getRGB(),
            placedAt(RevealPainter.AT_TRAVEL - 20, p).getRGB(corner.x, corner.y));
        eq("from landing, the real card's slot is held empty", RevealPainter.CARD_FILL.getRGB(),
            flying.getRGB(corner.x, corner.y));
        eq("no words in the slot until the card arrives", 0,
            lit(flying, new Rectangle(TARGET.x, TARGET.y, 120, TARGET.height)));

        // The real card, found on a real page, and its box is the card's own.
        javax.swing.JFrame[] frame = new javax.swing.JFrame[1];
        javax.swing.JEditorPane[] pane = new javax.swing.JEditorPane[1];
        Rectangle[] found = new Rectangle[3];
        BufferedImage[] shot = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            frame[0] = new javax.swing.JFrame();
            frame[0].setUndecorated(true);
            pane[0] = new javax.swing.JEditorPane("text/html", "");
            pane[0].setEditable(false);
            pane[0].setBackground(Color.BLACK);
            pane[0].setText(p.onTop("<html><body style='color:white; font-family:Arial;'>"
                + "<p>Below the card.</p></body></html>"));
            frame[0].getContentPane().add(pane[0]);
            frame[0].setSize(320, 420);
            frame[0].setLocation(-3000, -3000);
            frame[0].setVisible(true);
        });
        try {
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> {
                JLayeredPane lp = frame[0].getLayeredPane();
                found[0] = ReadingReveal.cardOnPage(lp, p.title());
                found[1] = ReadingReveal.cardOnPage(lp, "Mars in Aries");
                shot[0] = new BufferedImage(pane[0].getWidth(), pane[0].getHeight(),
                    BufferedImage.TYPE_INT_RGB);
                Graphics2D g = shot[0].createGraphics();
                pane[0].paint(g);
                g.dispose();
            });
            Rectangle r = found[0];
            yes("the real card is found on the page, " + r, r != null);
            if (r != null) {
                Rectangle inPane = SwingUtilities.convertRectangle(frame[0].getLayeredPane(), r,
                    pane[0]);
                yes("at the top of the page, " + inPane, inPane.y < 30);
                yes("as wide as the page, " + inPane, inPane.width > pane[0].getWidth() - 40);
                yes("and as tall as a card, " + inPane, inPane.height > 40 && inPane.height < 200);
                eq("its box is the card on screen, by its colour", RevealPainter.CARD_FILL.getRGB(),
                    shot[0].getRGB(inPane.x + 4, inPane.y + inPane.height - 4));
                yes("and the page below it is not", shot[0].getRGB(inPane.x + 4,
                    inPane.y + inPane.height + 30) != RevealPainter.CARD_FILL.getRGB());
            }
            yes("a card with another heading is not taken for it", found[1] == null);
            SwingUtilities.invokeAndWait(() -> frame[0].setVisible(false));
            SwingUtilities.invokeAndWait(() -> found[2] =
                ReadingReveal.cardOnPage(frame[0].getLayeredPane(), p.title()));
            yes("a page that is not on screen is not landed on", found[2] == null);

            // The reveal takes the measured box as its landing once the page is there.
            SwingUtilities.invokeAndWait(() -> frame[0].setVisible(true));
            Settings.setAnimateRings(true);
            Settings.setAnimateReadings(true);
            JLayeredPane lp = frame[0].getLayeredPane();
            ReadingReveal reveal = new ReadingReveal(lp);
            Rectangle real = new Rectangle(20, 30, 200, 90);
            AtomicInteger landed = new AtomicInteger();
            SwingUtilities.invokeAndWait(() -> reveal.play(placed(p), landed::incrementAndGet,
                () -> landed.get() > 0 ? real : null));
            eq("before landing it aims at the scene's own target", TARGET,
                reveal.scene().target());
            long deadline = System.currentTimeMillis() + 4000;
            while (reveal.progress() < RevealPainter.LANDS_AT + 0.05
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            SwingUtilities.invokeAndWait(() -> { });
            eq("the reading has landed", 1, landed.get());
            eq("and the card now flies to the real card", real, reveal.scene().target());
            SwingUtilities.invokeAndWait(() -> reveal.interrupt());
        } finally {
            SwingUtilities.invokeAndWait(() -> frame[0].dispose());
        }
    }

    // ------------------------------------------------------------------ I

    /**
     * David, 2026-10-17: the transit cards should pop up "when a wheel or chart is progressed
     * and stops on a time", highlighted on the chart, "in order of occurrance". He chose the
     * exact time for the order and three cards for each stop.
     */
    private static void theTransitPopups(SwissEph sw) throws Exception {
        // The order: nearest exact first, before or after; only contacts with a plain card.
        NowCards.TransitHit far = hit("Saturn", "Moon", com.zodiacomputing.ourania.astro.Aspects.Type.SQUARE, 0.2);
        NowCards.TransitHit near = hit("Mars", "Sun", com.zodiacomputing.ourania.astro.Aspects.Type.TRINE, 1.9);
        NowCards.TransitHit mid = hit("Venus", "Mars", com.zodiacomputing.ourania.astro.Aspects.Type.SEXTILE, 0.9);
        NowCards.TransitHit tie = hit("Sun", "Venus", com.zodiacomputing.ourania.astro.Aspects.Type.SQUARE, 0.4);
        NowCards.TransitHit minor = hit("Sun", "Uranus", com.zodiacomputing.ourania.astro.Aspects.Type.DECILE, 0.01);
        java.util.Map<NowCards.TransitHit, Double> days = new java.util.HashMap<>();
        days.put(far, 40.0);
        days.put(near, 0.5);
        days.put(mid, 3.0);
        days.put(tie, 3.0);
        days.put(minor, 0.0);
        List<NowCards.TransitHit> pop = TransitCard.firstToPop(List.of(far, near, mid, tie, minor),
            days::get, 3);
        eq("three cards, nearest their exact moment first, a tie by orb",
            List.of(near, tie, mid), pop);
        yes("a contact with no plain card never pops up", !pop.contains(minor));
        eq("David chose three", 3, TransitCard.POP_COUNT);

        // The exact moment, from the ephemeris: a natal point where Mars will be in seven hours
        // is that far from exact, and one where Mars was five days ago, five days. (Not "now"
        // exactly: the search finds a change of sign, and a zero on its own sample is no change.)
        double jd = new SweDate(2026, 10, 17, 12.0).getJulDay();
        double marsNow = com.zodiacomputing.ourania.astro.Almanac.bodyLongitude(sw, jd + 0.3, "Mars");
        double marsBefore = com.zodiacomputing.ourania.astro.Almanac.bodyLongitude(sw, jd - 5, "Mars");
        NowCards.TransitHit exactNow = new NowCards.TransitHit("Mars", marsNow, 0.7, 1, "Sun",
            marsNow, 1, com.zodiacomputing.ourania.astro.Aspects.Type.CONJUNCTION, 0.0, false, true);
        NowCards.TransitHit fiveAgo = new NowCards.TransitHit("Mars", marsNow, 0.7, 1, "Sun",
            marsBefore, 1, com.zodiacomputing.ourania.astro.Aspects.Type.CONJUNCTION, 3.5, false, true);
        near("a contact exact in seven hours is 0.3 days away", 0.3, TransitCard.daysFromExact(sw, exactNow, jd), 0.02);
        near("one exact five days ago is five days away", 5.0, TransitCard.daysFromExact(sw, fiveAgo, jd), 0.1);

        // The page makes a card for every contact that can pop up, each with its own id.
        WheelRing sky = WheelRing.of(WheelRing.Kind.SKY);
        WheelRing natal = WheelRing.of(WheelRing.Kind.CHART_A);
        int mars = com.zodiacomputing.ourania.astro.Bodies.indexOfName("Mars");
        int saturn = com.zodiacomputing.ourania.astro.Bodies.indexOfName("Saturn");
        int sun = com.zodiacomputing.ourania.astro.Bodies.indexOfName("Sun");
        int moon = com.zodiacomputing.ourania.astro.Bodies.indexOfName("Moon");
        natal.valid[sun] = true;
        natal.lon[sun] = 120.0;
        natal.valid[moon] = true;
        natal.lon[moon] = 210.0;
        sky.valid[mars] = true;
        sky.lon[mars] = 120.3;
        sky.speed[mars] = 0.6;
        sky.valid[saturn] = true;
        sky.lon[saturn] = 300.5;
        sky.speed[saturn] = 0.05;
        double[] cusps = new double[13];
        for (int i = 1; i <= 12; i++) {
            cusps[i] = (i - 1) * 30.0;
        }
        List<NowCards.TransitHit> found = NowCards.findHits(sky, natal, cusps);
        List<NowCards.TransitHit> shown = NowCards.shown(found, false);
        String page = NowCards.html(sky, natal, cusps, false, "");
        List<String> missing = new ArrayList<>();
        for (NowCards.TransitHit h : TransitCard.firstToPop(shown, h -> h.orb(), 3)) {
            if (!page.contains("<div id='" + TransitCard.id(h) + "'>")) {
                missing.add(TransitCard.id(h));
            }
        }
        yes("the fixture has contacts that pop up, " + shown.size(), !shown.isEmpty());
        eq("every card that can pop up has its own card on the page", "[]", missing.toString());
        yes("the astrologer page names its cards the same way", NowCards.html(sky, natal, cusps,
            true, "").contains("<div id='" + TransitCard.id(shown.get(0)) + "'>"));

        // The plan is the card on the page, word for word, forming at the planet in the sky.
        NowCards.TransitHit pluto = new NowCards.TransitHit("Pluto", 304.0, 0.01, 2, "Mars",
            214.8, 11, com.zodiacomputing.ourania.astro.Aspects.Type.SQUARE, 1.2, true, false);
        ReadingCard card = TransitCard.of(pluto);
        RevealPlan plan = RevealPlan.forTransit(pluto, card);
        eq("the painted card is the page's card, word for word",
            List.of(card.label(), card.headline(), card.body()),
            List.of(plan.card().label(), plan.card().headline(), plan.card().body()));
        eq("its factors are the planet in the sky, then the natal point",
            List.of(RevealPlan.SKY + "Pluto", "Mars"), plan.points());
        yes("and nothing is counted", plan.ticks().isEmpty() && !plan.confirmed());

        // A transit's headline is a sentence: wrapped, it stays inside its card.
        Rectangle real = new Rectangle(440, 30, 240, 300);
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(GREY);
        g.fillRect(0, 0, W, H);
        RevealPainter.paint(g, new RevealPainter.Scene(CENTRE, 150, List.of(P, C1), P, real, plan,
            Color.BLACK), (RevealPainter.AT_HOLD + 100.0) / RevealPainter.TOTAL_MS, 1.0);
        g.dispose();
        int inside = lit(img, real);
        int outside = lit(img, new Rectangle(real.x + real.width + 2, real.y, W - real.x - real.width - 2, real.height));
        yes("the card's words are drawn, " + inside, inside > 200);
        eq("and none of them runs past its right edge", 0, outside);

        // A chain: the next card plays when one ends by itself; a click ends the chain.
        JLayeredPane layer = new JLayeredPane();
        layer.setSize(W, H);
        ReadingReveal reveal = new ReadingReveal(layer);
        AtomicInteger played = new AtomicInteger();
        Runnable[] second = new Runnable[1];
        second[0] = () -> reveal.play(placed(placement()), played::incrementAndGet, null, 300, null);
        SwingUtilities.invokeAndWait(() -> reveal.play(placed(placement()), played::incrementAndGet,
            null, 300, second[0]));
        long until = System.currentTimeMillis() + 4000;
        while (played.get() < 2 && System.currentTimeMillis() < until) {
            Thread.sleep(10);
        }
        eq("a card that ends by itself plays the next", 2, played.get());
        while (reveal.playing() && System.currentTimeMillis() < until) {
            Thread.sleep(10);
        }
        AtomicInteger chained = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> reveal.play(placed(placement()), null, null, 2000,
            chained::incrementAndGet));
        SwingUtilities.invokeAndWait(() -> reveal.interrupt());
        Thread.sleep(500);
        eq("an interrupted card ends the chain", 0, chained.get());

        // On the real wheel in Transit mode, a card forms on its planet's glyph in the sky.
        OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SkymapPanel wheel = (SkymapPanel) CheckReflect.get(w[0], "skymapPanel");
            java.awt.Component chart = (java.awt.Component) CheckReflect.get(wheel, "chartPanel");
            SwingUtilities.invokeAndWait(() -> {
                chart.setSize(900, 820);
                wheel.applyChartMode(ChartMode.TRANSIT, true);
            });
            long open = System.currentTimeMillis() + 4000;
            while (!wheel.outerRingDrawn() && System.currentTimeMillis() < open) {
                Thread.sleep(20);
            }
            yes("the transit ring is drawn", wheel.outerRingDrawn());
            yes("and it is the ring the Now page reads", wheel.nowRing() == wheel.outerRing);
            java.lang.reflect.Method bodyAt = SkymapPanel.class.getDeclaredMethod("bodyAt",
                int.class, int.class);
            bodyAt.setAccessible(true);
            JLayeredPane lp = w[0].getLayeredPane();
            int tried = 0;
            List<String> wrong = new ArrayList<>();
            double[] cuspsNow = (double[]) CheckReflect.get(wheel, "activeCusps");
            for (NowCards.TransitHit h : NowCards.shown(NowCards.findHits(wheel.nowRing(),
                    wheel.natalRing, cuspsNow), false)) {
                RevealPlan tp = RevealPlan.forTransit(h, TransitCard.of(h));
                RevealPainter.Scene[] sc = new RevealPainter.Scene[1];
                SwingUtilities.invokeAndWait(() -> sc[0] = wheel.sceneOn(tp,
                    SkymapPanel.REVEAL_AT_FIRST, lp));
                if (sc[0] == null) {
                    wrong.add(h.transiting() + " has no scene");
                    continue;
                }
                tried++;
                Point f = SwingUtilities.convertPoint(lp, sc[0].focal(), chart);
                Point at = wheel.toWheel(f.x, f.y);
                int got = (Integer) bodyAt.invoke(wheel, at.x, at.y);
                int want = com.zodiacomputing.ourania.astro.Bodies.indexOfName(h.transiting())
                    | WheelLayout.TRANSIT_BIT;
                if (got != want) {
                    wrong.add(h.transiting() + " formed at " + got);
                }
                if (sc[0].contributors().size() != 2) {
                    wrong.add(h.transiting() + " has " + sc[0].contributors().size() + " factors");
                }
            }
            yes("the default sky has cards to form, " + tried, tried > 0);
            eq("each forms on its planet in the sky, with the natal point as its other factor",
                "[]", wrong.toString());
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }

        // Only a stop of time plays them, never a tick of Play.
        Path src = Path.of("src/main/java/com/zodiacomputing/ourania/gui/SkymapPanel.java");
        if (Files.exists(src)) {
            String s = Files.readString(src);
            yes("Pause pops them up", body(s, "void playPause()").contains("this.timeSettled()"));
            yes("so does the end of a scrub", body(s, "void endScrub()").contains("this.timeSettled()"));
            yes("and the Now button", body(s, "void jumpToNow()").contains("this.timeSettled()"));
            yes("a tick of Play never does", !body(s, "private void stepTime()").contains("timeSettled")
                && !body(s, "private void stepTime()").contains("popTransits"));
            yes("they respect reduced motion",
                body(s, "private void timeSettled()").contains("ReadingReveal.allowed()"));
            yes("they choose from the cards the page shows",
                body(s, "private void playTransits()").contains("NowCards.visible("));
            yes("the page reads the wheel's own ring rule", Files.readString(
                src.resolveSibling("OuraniaWindow.java")).contains("skymapPanel.nowRing()"));
        }
    }

    private static NowCards.TransitHit hit(String sky, String natal,
            com.zodiacomputing.ourania.astro.Aspects.Type type, double orb) {
        return new NowCards.TransitHit(sky, 10, 1, 1, natal, 10, 1, type, orb, true, false);
    }

    private static String squash(String s) {
        return s.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    /** The text of a method from its signature to the next blank-line-separated declaration. */
    private static String body(String s, String signature) {
        int at = s.indexOf(signature);
        if (at < 0) {
            return "";
        }
        int depth = 0;
        int open = s.indexOf('{', at);
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return s.substring(at, i + 1);
            }
        }
        return s.substring(at);
    }

    // ------------------------------------------------------------------ helpers

    private static Convergence.Target target(String natal, Convergence.Witness... ws) {
        Convergence.Target t = new Convergence.Target();
        t.natal = natal;
        for (Convergence.Witness w : ws) {
            t.witnesses.add(w);
            if (w.independent) {
                t.families.add(w.family);
            }
        }
        t.score = 0.1 * ws.length;
        return t;
    }

    private static Convergence.Witness w(Convergence.Family family, boolean independent, String body) {
        Convergence.Witness w = new Convergence.Witness();
        w.family = family;
        w.independent = independent;
        w.movingBody = body;
        w.detail = family.name().toLowerCase() + " witness";
        return w;
    }

    /** Pixels in a region that differ clearly from the card fill and the grey. */
    private static int lit(BufferedImage img, Rectangle r) {
        int n = 0;
        for (int y = Math.max(0, r.y); y < Math.min(img.getHeight(), r.y + r.height); y++) {
            for (int x = Math.max(0, r.x); x < Math.min(img.getWidth(), r.x + r.width); x++) {
                int c = img.getRGB(x, y);
                int rr = (c >> 16) & 0xff;
                int gg = (c >> 8) & 0xff;
                int bb = c & 0xff;
                if (rr + gg + bb > 3 * 175) {
                    n++;
                }
            }
        }
        return n;
    }

    private static int red(BufferedImage img, Point p) {
        return (img.getRGB(p.x, p.y) >> 16) & 0xff;
    }

    private static int green(BufferedImage img, Point p) {
        return (img.getRGB(p.x, p.y) >> 8) & 0xff;
    }

    private static int blue(BufferedImage img, Point p) {
        return img.getRGB(p.x, p.y) & 0xff;
    }

    private static String hex(BufferedImage img, Point p) {
        return String.format("#%06X", img.getRGB(p.x, p.y) & 0xffffff);
    }

    private interface Part {
        void run() throws Exception;
    }

    private static void part(String name, Part p) throws Exception {
        System.out.println("=== " + name + " ===");
        int before = failures.size();
        int checksBefore = checks;
        p.run();
        System.out.println("  " + (checks - checksBefore) + " checks, "
            + (failures.size() - before) + " failures");
    }

    private static void yes(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(what);
            System.out.println("  FAIL " + what);
        }
    }

    private static void eq(String what, Object want, Object got) {
        checks++;
        if (!java.util.Objects.equals(want, got)) {
            failures.add(what + ": wanted " + want + ", got " + got);
            System.out.println("  FAIL " + what + ": wanted " + want + ", got " + got);
        }
    }

    private static void near(String what, double want, double got, double tol) {
        checks++;
        if (Math.abs(want - got) > tol) {
            failures.add(what + ": wanted " + want + " +/- " + tol + ", got " + got);
            System.out.println("  FAIL " + what + ": wanted " + want + " +/- " + tol + ", got " + got);
        }
    }
}
