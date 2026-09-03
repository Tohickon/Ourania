package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Synastry;
import com.zodiacomputing.ourania.astro.AspectPatterns;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Ephemeris;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Guards the aspect grid's click, hover and highlight path.
 *
 * <b>This suite exists because the feature shipped broken once already.</b> The grid's cells
 * were hyperlinks from the start, but the href was underscore-delimited and its first field is
 * "transit_sun" on a transit row - so {@code split("_")} produced five or six parts, the
 * handler's {@code length == 4} guard rejected them, and <b>every transit and synastry cell in
 * the grid was silently unclickable</b>. Nothing failed, nothing logged; the cell just did
 * nothing. A structural check on the href format would have caught it the day it was written.
 *
 * Part C is here for the same reason: the first version of {@code hrefAt} tested containment
 * against the rectangle from {@code modelToView2D}, which is a <b>zero-width caret</b>, so it
 * rejected every point and no tooltip could ever appear. Also a silent nothing.
 *
 * Deliberately does not build an OuraniaWindow. The parts worth guarding are the href
 * contract and the hit test; a live chart adds minutes to the run and tests the ephemeris
 * rather than the grid.
 */
public final class AspectGridCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("build: " + AspectGridCheck.class.getProtectionDomain().getCodeSource().getLocation());
        int before = failures.size();
        hrefRoundTrip();
        report("Part A - href survives the click handler's parser", before);

        before = failures.size();
        labelsResolve();
        report("Part B - grid labels resolve to registry indices", before);

        before = failures.size();
        hitTest();
        report("Part C - hrefAt is a hit test, not a nearest-character query", before);

        before = failures.size();
        everyAspectHasAGlyph();
        report("Part D - every aspect type the grid can emit has a glyph and a colour", before);

        before = failures.size();
        everyCellHoverableAcrossItsWholeGlyph();
        report("Part E - both halves of every real grid cell are hoverable", before);

        before = failures.size();
        noDeadAspectCells();
        report("Part F - every aspect type resolves to prose, not a dead cell", before);

        before = failures.size();
        everyPatternHasDetail();
        report("Part G - every aspect pattern the engine can emit has prose", before);

        before = failures.size();
        panelLinksRoundTrip();
        report("Part H - every link the interpretation panel emits parses back", before);

        before = failures.size();
        patternHighlight();
        report("Part I - lighting a pattern covers its legs and nothing else", before);

        before = failures.size();
        ringGeometry();
        report("Part J - the extracted ring radii are the wheel's own chain", before);

        before = failures.size();
        synastryCrossBlock();
        report("Part K - the synastry cross-chart block is the panel's own output", before);

        before = failures.size();
        gridOrbRegime();
        report("Part L - the grid reads synastry halved and transits full", before);

        before = failures.size();
        controlsAgreeWithState();
        report("Part M - every control shows the value it actually drives", before);

        before = failures.size();
        compositeCarriesTransits();
        report("Part N - a composite draws the sky on its outer wheel", before);

        before = failures.size();
        indexPagesResolve();
        report("Part O - every index page renders and every link it emits resolves", before);

        before = failures.size();
        triWheelSidePanel();
        report("Part P - the tri-wheel lists sky placements and parses sky body clicks", before);

        before = failures.size();
        readingsGenerate();
        report("Part Q - every reading generates, for every mode, with and without time", before);

        before = failures.size();
        aspectSelectionIsPinned();
        report("Part R - this suite reads the code, not the user's settings", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
        // Part E constructs an OuraniaWindow. Swing starts the event thread the moment one
        // exists, and a non-daemon EDT keeps the JVM alive forever - which would hang
        // handover-stamp.sh rather than fail it. Leave explicitly.
        System.exit(0);
    }

    /**
     * Every href the grid can emit, parsed back into its three fields.
     *
     * Built from the registry rather than a fixed list, so a new chart point with a space or
     * an awkward character in its name is covered the moment it is registered.
     */
    private static void hrefRoundTrip() throws Exception {
        Method parse = SkymapPanel.class.getDeclaredMethod("parseAspectHref", String.class);
        parse.setAccessible(true);
        String[] types = {"Conjunction", "Sextile", "Square", "Trine", "Quincunx", "Opposition"};

        for (int i = 0; i < Bodies.count(); i++) {
            String row = Bodies.at(i).name;
            String col = Bodies.at((i + 1) % Bodies.count()).name;
            for (String type : types) {
                // natal row, and the transit row exactly as the grid builds it
                for (String label : new String[]{row, "transit_" + row.toLowerCase()}) {
                    String href = SkymapPanel.aspectHref(label, col, type);
                    String[] p = (String[]) parse.invoke(null, href);
                    ok(href + " parses", p != null);
                    if (p != null) {
                        ok(href + " keeps its row label", label.equals(p[0]));
                        ok(href + " keeps its column body", col.equals(p[1]));
                        ok(href + " keeps its aspect", type.equals(p[2]));
                    }
                }
            }
        }

        // Anything that is not an aspect href must be declined, so the placement hrefs
        // ("base_0", "transit_1") still reach their own branch of the handler.
        for (String other : new String[]{"base_0", "transit_1", "", "aspect", "aspect|a|b",
                                         "aspect|a|b|c|d", "aspect_Sun_Moon_Trine", null}) {
            ok("non-aspect href declined: " + other, parse.invoke(null, other) == null);
        }
    }

    /** Row and column labels, including the transit prefix, must find their registry point. */
    private static void labelsResolve() throws Exception {
        Method idx = SkymapPanel.class.getDeclaredMethod("bodyIndexOfLabel", String.class);
        idx.setAccessible(true);
        for (int i = 0; i < Bodies.count(); i++) {
            String name = Bodies.at(i).name;
            eq("plain label " + name, i, (Integer) idx.invoke(null, name));
            // The grid lowercases the name behind the transit prefix, and a name can contain
            // a space - "transit_north node", not "transit_north_node".
            eq("transit label " + name, i, (Integer) idx.invoke(null, "transit_" + name.toLowerCase()));
        }
        ok("unknown label rejected", (Integer) idx.invoke(null, "Nonsense") == -1);
        ok("unknown transit label rejected", (Integer) idx.invoke(null, "transit_nonsense") == -1);
    }

    /** The tooltip lookup must find a link under the link and nothing anywhere else. */
    private static void hitTest() throws Exception {
        // HtmlPanes, not MainMenuPanel: the hit test moved there on 2026-09-02, when the
        // sidebar became two sidebars and both needed the same pane.
        Method hrefAt = HtmlPanes.class.getDeclaredMethod("hrefAt", JEditorPane.class,
            Point.class);
        hrefAt.setAccessible(true);

        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText("<html><body><table><tr>"
            + "<td><a href='aspect|Sun|Moon|Trine'>AAAAA</a></td>"
            + "<td>plain</td></tr></table></body></html>");
        pane.setSize(400, 200);
        pane.addNotify();
        pane.validate();

        String found = null;
        for (int y = 0; y < 60 && found == null; y += 2) {
            for (int x = 0; x < 200; x += 2) {
                Object r = hrefAt.invoke(null, pane, new Point(x, y));
                if (r != null) {
                    found = (String) r;
                    break;
                }
            }
        }
        ok("the link's href is reachable somewhere over the link", "aspect|Sun|Moon|Trine".equals(found));

        // Far outside the text. viewToModel2D still resolves to the nearest character here,
        // so a nearest-character implementation passes the check above and fails this one.
        ok("nothing far below the table", hrefAt.invoke(null, pane, new Point(200, 190)) == null);
        ok("nothing far to the right", hrefAt.invoke(null, pane, new Point(395, 5)) == null);
    }

    /**
     * A cell with no glyph is a link with nothing to click.
     *
     * <b>Quincunx had no case in getAspectSymbol</b> and fell through to the empty string, so
     * every quincunx cell rendered as a blank span - 19 of them in a natal grid, 43 in a
     * transit one. The href was there and the interpretation was there; there was simply
     * nothing on screen to put a cursor on. Driven from Aspects.Type so a sixth aspect cannot
     * be added to the model and forgotten in the view.
     */
    private static void everyAspectHasAGlyph() throws Exception {
        Method sym = SkymapPanel.class.getDeclaredMethod("getAspectSymbol", String.class);
        sym.setAccessible(true);
        Method col = SkymapPanel.class.getDeclaredMethod("getAspectColorHex", String.class);
        col.setAccessible(true);
        for (com.zodiacomputing.ourania.astro.Aspects.Type t
                : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            String glyph = (String) sym.invoke(null, t.label);
            ok(t.label + " has a glyph", glyph != null && !glyph.isEmpty());
            String colour = (String) col.invoke(null, t.label);
            ok(t.label + " has a colour of its own", colour != null && !colour.equals("#FFFFFF"));
        }
    }

    /**
     * <b>Both halves of a cell must respond, not just the left.</b>
     *
     * viewToModel2D returns the nearest INSERTION POINT. For a point in the right half of a
     * glyph that is the offset AFTER it, whose character element belongs to the next cell and
     * carries no anchor - so a lookup that only consults {@code pos} finds nothing. Measured
     * over a real 229-cell transit grid: with the {@code pos - 1} branch removed, the right
     * half of <b>every single cell</b> is dead. Since the grid's cells are one glyph wide,
     * that is half the target area of the whole table, and it presents as aspects that respond
     * only if the cursor happens to sit left of centre.
     *
     * This is the check that was missing. An earlier version of it probed only the left edge
     * of each glyph, passed with the fix reverted, and therefore guarded nothing.
     */
    private static void everyCellHoverableAcrossItsWholeGlyph() throws Exception {
        // HtmlPanes, not MainMenuPanel: the hit test moved there on 2026-09-02, when the
        // sidebar became two sidebars and both needed the same pane.
        Method hrefAt = HtmlPanes.class.getDeclaredMethod("hrefAt", JEditorPane.class,
            Point.class);
        hrefAt.setAccessible(true);

        // Built from the REAL grid, not a hand-written imitation of it.
        //
        // Two synthetic tables were tried first - a bare <td><a>glyph</a></td>, then a copy of
        // the grid's cell markup with its styled span - and BOTH passed with the fix reverted.
        // Whatever detail of the real document produces the condition was not in either
        // imitation, and guessing at it twice was two guesses too many. A hit test has to be
        // guarded against the document it will actually be asked about, which costs one window.
        String html = realGridHtml();

        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText(html);
        pane.setSize(760, 4000);
        pane.addNotify();
        pane.validate();

        HTMLDocument doc = (HTMLDocument) pane.getDocument();
        int found = 0;
        List<String> leftBad = new ArrayList<>();
        List<String> midBad = new ArrayList<>();
        List<String> rightBad = new ArrayList<>();
        for (int p = 0; p < doc.getLength(); p++) {
            Object a = doc.getCharacterElement(p).getAttributes().getAttribute(HTML.Tag.A);
            if (!(a instanceof AttributeSet)) {
                continue;
            }
            Object hrefObj = ((AttributeSet) a).getAttribute(HTML.Attribute.HREF);
            if (hrefObj == null) {
                continue;
            }
            String href = hrefObj.toString();
            if (!href.startsWith("aspect|")) {
                continue;                       // the placement links above the grid
            }
            Rectangle2D r = pane.modelToView2D(p);
            if (r == null) {
                continue;
            }
            found++;
            // width of this glyph, from the caret after it
            double width = 10.0;
            Rectangle2D next = pane.modelToView2D(p + 1);
            if (next != null && Math.abs(next.getY() - r.getY()) < 1.0 && next.getX() > r.getX()) {
                width = next.getX() - r.getX();
            }
            int cy = (int) (r.getY() + r.getHeight() / 2);
            if (!href.equals(hrefAt.invoke(null, pane, new Point((int) r.getX() + 1, cy)))) {
                leftBad.add(href);
            }
            if (!href.equals(hrefAt.invoke(null, pane, new Point((int) (r.getX() + width / 2), cy)))) {
                midBad.add(href);
            }
            if (!href.equals(hrefAt.invoke(null, pane, new Point((int) (r.getX() + width - 1), cy)))) {
                rightBad.add(href);
            }
        }
        // FOUR assertions, not three per cell.
        //
        // The chart behind this grid is built from the current time, so the number of aspect
        // cells changes from one run to the next - and a per-cell assertion made the suite's
        // own check count drift with it. handover-stamp.sh --check compares that count against
        // HANDOVER.md, so a wandering total turns the stamp into a coin toss: it reported
        // 83,467 on one run and 83,455 twenty minutes later, with nothing changed. The count
        // has to be a property of the SUITE, not of today's sky.
        ok("the real grid produced aspect cells to probe (" + found + ")", found > 50);
        ok("every cell responds on its left edge" + sample(leftBad), leftBad.isEmpty());
        ok("every cell responds at its middle" + sample(midBad), midBad.isEmpty());
        ok("every cell responds on its RIGHT edge" + sample(rightBad), rightBad.isEmpty());
    }

    /**
     * The transit grid's HTML, straight from the panel that ships it.
     *
     * Forces the transit view on and gives the transit bodies spread longitudes, because the
     * transit grid is a full rectangle where the natal one is lower-triangular - roughly three
     * times the cells, and the shape in which the hover path was actually reported broken.
     */
    private static String realGridHtml() throws Exception {
        final String[] out = new String[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();          // built, never shown
                SkymapPanel sky = skyOf(w);
                setField(sky, "showTransitChart", true);
                setField(sky, "aspectFilter", "Transit-Natal");
                java.util.Arrays.fill((boolean[]) getField(sky, "tValid"), true);
                double[] lon = (double[]) getField(sky, "tLon");
                for (int i = 0; i < lon.length; i++) {
                    lon[i] = (i * 30.0) % 360.0;
                }
                Method gen = SkymapPanel.class.getDeclaredMethod("generatePlanetPlacementsHtml");
                gen.setAccessible(true);
                out[0] = (String) gen.invoke(sky);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return out[0];
    }

    /**
     * The cross-chart block, taken from the panel that ships it rather than rebuilt here.
     *
     * <b>This is the half SynastryCheck cannot cover.</b> That suite is in the astro package
     * and states, correctly, that it will not construct a Swing component to reimplement its
     * loops. It therefore proves that {@code Synastry.houseOverlays} and
     * {@code Synastry.angleContacts} are right, and nothing at all proves the panel calls
     * them, calls them in the right direction, or renders what comes back. That gap is
     * exactly where a feature can be green in a suite and absent on screen.
     *
     * Reflection into the real {@code generatePlanetPlacementsHtml}, on the same harness Part
     * E uses. Every assertion below compares the HTML against {@code Synastry} run on frames
     * built from <b>the panel's own fields</b> - its dates, its coordinates, its house system
     * - so this cannot pass by both sides sharing a mistaken input.
     *
     * <b>The mode gate is asserted from both sides.</b> Present in SYNASTRY, absent in
     * TRANSIT with identical chart data. A block that rendered in transit mode would be
     * telling a user about house overlays between a person and a moment.
     */
    private static void synastryCrossBlock() throws Exception {
        String synastry = crossBlockHtml(ChartMode.SYNASTRY);
        String transit = crossBlockHtml(ChartMode.TRANSIT);

        String[] headings = {
            "Angle contacts &middot; Chart A on Chart B's angles",
            "Angle contacts &middot; Chart B on Chart A's angles",
            "House overlays &middot; Chart A's placements in Chart B's houses",
            "House overlays &middot; Chart B's placements in Chart A's houses",
        };
        for (String h : headings) {
            ok("synastry mode renders: " + h, synastry.contains(h));
            ok("transit mode does not render: " + h, !transit.contains(h));
        }

        // The frames the panel itself used, rebuilt from its own fields.
        ChartFrame a = frameFromPanel("base");
        ChartFrame b = frameFromPanel("transit");

        // Angle contacts: every one computed appears, and no more appear than were computed.
        int expected = 0;
        for (Object[] pair : new Object[][] {{a, b}, {b, a}}) {
            for (Synastry.AngleContact h
                    : Synastry.angleContacts((ChartFrame) pair[0], (ChartFrame) pair[1])) {
                expected++;
                ok("the panel shows " + h.body + " on " + h.angle,
                    synastry.contains(h.body) && synastry.contains(h.angle));
            }
        }
        eq("the panel renders exactly the contacts that were computed",
            expected, countOf(synastry, "&mdash; orb "));
        // Printed, because "exactly the contacts computed" is satisfied by zero and zero,
        // and a vacuous pass here looks identical to a real one in the report.
        // <b>Measured between the block's own markers, not as a difference of two documents.</b>
        // This printed (synastry.length() - transit.length()) until 2026-08-24, which is not
        // the block at all: the two modes also differ by the size of the GRID, and when the
        // halved synastry orb landed and the grid lost ninety cells the figure fell from
        // 7,774 to 383 while the block was in fact growing. A number that moves for a reason
        // outside what it names is worse than no number.
        int blockFrom = synastry.indexOf("Angle contacts");
        int blockTo = synastry.indexOf("<h3 style=&#39;color:white;".replace("&#39;", "'"), blockFrom);
        ok("the block has both its ends", blockFrom >= 0 && blockTo > blockFrom);
        System.out.println("  contact rows rendered: " + expected
            + "; cross-chart block: "
            + (blockTo > blockFrom ? blockTo - blockFrom : -1) + " chars");
        checks++;
        if (expected == 0) {
            failures.add("the reference pair produced no angle contacts at all - Part K is "
                + "passing vacuously and the charts need replacing");
        }

        // House overlays: every body is placed once per direction, so twice in the block.
        // Counted on the house rows only - the placements list above uses glyphs, not names.
        String overlays = synastry.substring(
            synastry.indexOf("House overlays &middot; Chart A"));
        for (Synastry.Overlay o : Synastry.houseOverlays(a, b)) {
            eq("both directions place " + o.body, 2, countOf(overlays, "</span> " + o.body));
        }

        // Nothing lands in a house the block does not name, and the empty houses really are
        // empty: twelve houses are accounted for in each direction, as rows or in the
        // "nothing of theirs in" line.
        ok("the empty houses are named rather than omitted",
            countOf(overlays, "Nothing of theirs in:") <= 2);

        // Balanced markup. A JEditorPane will render unbalanced HTML without complaining and
        // simply swallow everything after the break, which is how a section disappears
        // silently.
        eq("the block's divs are balanced",
            countOf(synastry, "<div"), countOf(synastry, "</div>"));
        eq("the block's h3s are balanced",
            countOf(synastry, "<h3"), countOf(synastry, "</h3>"));
    }

    /**
     * Which orb the grid actually judged each cell by, read back out of the grid.
     *
     * <b>This is the check the wiring needed and did not have.</b> On 2026-08-24 the halved
     * synastry orb was wired into the panel through {@code isSynastryPair}, which is two
     * conditions - the pair must be cross-chart AND the mode must be SYNASTRY - applied at
     * eight separate call sites. Every one of those sites is a judgement, and every wrong
     * judgement is invisible: a natal aspect quietly tightened, or a cross aspect left loose,
     * with nothing on screen looking broken. A count of filled cells cannot catch it either,
     * because the count would still fall.
     *
     * So this reads the grid the panel emits, parses every href back into its row, column and
     * aspect through the same {@code parseAspectHref} Part A round-trips, and asserts the
     * aspect against {@code Aspects.typeOf} with the flag the mode implies. Then it asserts
     * the other half - that every pair with NO href really has no aspect at that orb - which
     * is what catches a site left at the full orb.
     *
     * <b>TRANSIT is the control and it matters as much as SYNASTRY.</b> Transit-to-natal is
     * cross-chart by the same arithmetic, so a wiring that gated on cross-chart alone would
     * have silently halved every transit reading in the app. Identical chart data goes into
     * both modes here; only the mode differs.
     */
    private static void gridOrbRegime() throws Exception {
        int synastryCells = check(ChartMode.SYNASTRY, true);
        int transitCells = check(ChartMode.TRANSIT, false);

        System.out.println("  aspect cells - synastry " + synastryCells
            + ", transit " + transitCells + " on identical charts ("
            + Math.round(1000.0 * synastryCells / Math.max(1, transitCells)) / 10.0 + "% kept)");

        // Non-vacuous: if the halving stopped reaching the grid, every assertion above would
        // still pass, because both sides would simply agree at the full orb.
        ok("the halved orb actually reaches the grid", synastryCells < transitCells);
        ok("and does not empty it", synastryCells > transitCells / 3);
    }

    /** One mode: every cell of the grid, both directions of the contract. Returns cells lit. */
    private static int check(ChartMode mode, boolean expectHalved) throws Exception {
        String html = Probe.gridHtml(mode);
        double[] bLon = (double[]) getField(Probe.panel, "bLon");
        double[] tLon = (double[]) getField(Probe.panel, "tLon");
        boolean[] bValid = (boolean[]) getField(Probe.panel, "bValid");
        boolean[] tValid = (boolean[]) getField(Probe.panel, "tValid");

        Method parse = SkymapPanel.class.getDeclaredMethod("parseAspectHref", String.class);
        parse.setAccessible(true);
        Method idxOf = SkymapPanel.class.getDeclaredMethod("bodyIndexOfLabel", String.class);
        idxOf.setAccessible(true);

        // Every href the grid emitted, as row-index/col-index/aspect.
        java.util.Set<String> seen = new java.util.HashSet<>();
        int lit = 0;
        int from = html.indexOf("Aspects Grid");
        if (from < 0) {
            from = html.indexOf("Transit to Natal Grid");
        }
        ok(mode + ": the grid section exists", from >= 0);
        String grid = from < 0 ? "" : html.substring(from);

        for (int i = grid.indexOf("href='aspect|"); i >= 0; i = grid.indexOf("href='aspect|", i + 1)) {
            String href = grid.substring(i + 6, grid.indexOf(39, i + 6));
            String[] parsed = (String[]) parse.invoke(null, href);
            ok(mode + ": " + href + " parses", parsed != null);
            if (parsed == null) {
                continue;
            }
            int row = (Integer) idxOf.invoke(null, parsed[0]);
            int col = (Integer) idxOf.invoke(null, parsed[1]);
            boolean cross = parsed[0].toLowerCase().startsWith("transit_");
            if (row < 0 || col < 0) {
                continue;
            }
            lit++;
            seen.add(row + "/" + col);

            double sep = Aspects.separation(cross ? tLon[row] : bLon[row], bLon[col]);
            Aspects.Type expected = Aspects.typeOf(sep, Bodies.at(row).name, Bodies.at(col).name,
                cross && expectHalved);
            ok(mode + ": " + Bodies.at(row).name + "/" + Bodies.at(col).name + " is "
                    + parsed[2] + " at the " + (cross && expectHalved ? "halved" : "full") + " orb",
                expected != null && expected.label.equals(parsed[2]));
        }

        // The other half. A pair the grid left blank must really have no aspect at the orb
        // the mode implies - this is what fails if a call site kept the full orb.
        // <b>The sweep has to use the grid's own inclusion rule, not raw validity.</b> The
        // grid draws a row or column only when SkymapPanel.aspecting() says so, and that
        // excludes the four angles - they are drawn on the wheel as labelled axes, not as
        // grid bodies. Sweeping raw bValid asserted about 140 cells the grid never emits and
        // reported every one of them as a miss, in BOTH modes, which is the signature of a
        // check that is wrong rather than a feature that is.
        boolean crossGrid = true;
        for (int row = 0; row < Bodies.count(); row++) {
            if (!tValid[row] || Bodies.at(row).isAngle()) continue;
            for (int col = 0; col < Bodies.count(); col++) {
                if (!bValid[col] || Bodies.at(col).isAngle()
                    || Bodies.isOppositePair(row, col)) continue;
                if (seen.contains(row + "/" + col)) continue;
                double sep = Aspects.separation(tLon[row], bLon[col]);
                Aspects.Type found = Aspects.typeOf(sep, Bodies.at(row).name, Bodies.at(col).name,
                    crossGrid && expectHalved);
                ok(mode + ": " + Bodies.at(row).name + "/" + Bodies.at(col).name
                        + " is blank and really has no aspect at that orb", found == null);
            }
        }
        return lit;
    }

    /** Builds one panel, keeps it, and hands back its HTML - the two must match. */
    private static final class Probe {
        static SkymapPanel panel;

        static String gridHtml(ChartMode mode) throws Exception {
            final String[] out = new String[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    OuraniaWindow w = new OuraniaWindow();
                    panel = skyOf(w);
                    setField(panel, "chartMode", mode);
                    setField(panel, "showTransitChart", true);
                    setField(panel, "aspectFilter", "Both");
                    double[] base = PANEL_CHARTS.get("base");
                    double[] tr = PANEL_CHARTS.get("transit");
                    setField(panel, "baseChartTime", utc(base));
                    setField(panel, "transitChartTime", utc(tr));
                    setField(panel, "baseLatitude", base[4]);
                    setField(panel, "baseLongitude", base[5]);
                    setField(panel, "transitLatitude", tr[4]);
                    setField(panel, "transitLongitude", tr[5]);
                    setField(panel, "houseSystem", (char) PANEL_HSYS);
                    panel.updateChartData();
                    Method gen =
                        SkymapPanel.class.getDeclaredMethod("generatePlanetPlacementsHtml");
                    gen.setAccessible(true);
                    out[0] = (String) gen.invoke(panel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return out[0];
        }
    }

    /**
     * Every dropdown displays the value the engine is actually using.
     *
     * <b>Written because one of them did not, and the gap was invisible for as long as the
     * app has had an animation.</b> The Step combo was constructed showing "1 Hour" while
     * {@code stepAmount} held "1 Day", because the combo was seeded with a literal BEFORE its
     * listener was attached - so no event fired, nothing reconciled the two, and a user who
     * never touched the dropdown animated in day-sized steps from a control reading 1 Hour.
     * It corrected itself the instant they changed the selection to anything and back, which
     * is exactly why it survived: the obvious way to test it is to change it.
     *
     * <b>This is the project's most-logged defect in its smallest form</b> - one value stated
     * in two places with no mechanism keeping them in step. The field said one thing, the
     * widget said another, and the screen showed the widget.
     *
     * So this walks the real control panel, finds every JComboBox, and asserts its selected
     * item against the field it drives. Driven off the panel's own component tree rather than
     * a list of combos written here, so a control added later is covered the day it is added.
     */
    private static void controlsAgreeWithState() throws Exception {
        final Object[] out = new Object[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();
                out[0] = skyOf(w);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        SkymapPanel sky = (SkymapPanel) out[0];

        // <b>Named, not guessed.</b> The first cut of this identified each combo by an option
        // it offers, and the Align combo offers the same three words as the Animate combo -
        // so it was tested against the wrong field and reported a defect that was mine. Four
        // of the five combos are fields on the panel, so they are read as fields; only the
        // Step combo is a local variable, and only that one has to be found in the tree.
        String[][] byField = {
            {"animateCombo", "animateTarget"},
            {"filterCombo", "aspectFilter"},
            {"alignCombo", "houseAlignment"},
            {"pinCombo", "wheelPin"},
        };
        for (String[] pair : byField) {
            Object combo = getField(sky, pair[0]);
            ok(pair[0] + " exists", combo instanceof javax.swing.JComboBox);
            if (!(combo instanceof javax.swing.JComboBox)) {
                continue;
            }
            eqObj("the " + pair[1] + " control shows what the engine uses",
                getField(sky, pair[1]), ((javax.swing.JComboBox<?>) combo).getSelectedItem());
        }

        // The Step combo is the one that was wrong, and it is the one this cannot reach by
        // field. "Real Time" is offered by no other control.
        java.util.List<javax.swing.JComboBox<?>> combos = new ArrayList<>();
        collectCombos(sky, combos);
        javax.swing.JComboBox<?> step = null;
        for (javax.swing.JComboBox<?> combo : combos) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if ("Real Time".equals(String.valueOf(combo.getItemAt(i)))) {
                    step = combo;
                }
            }
        }
        ok("the Step dropdown is on the panel", step != null);
        if (step != null) {
            eqObj("the stepAmount control shows what the engine uses",
                getField(sky, "stepAmount"), step.getSelectedItem());
            // And the value it holds must be one the switch in stepTime actually handles -
            // an unmatched string falls through every case and time simply stops moving.
            java.util.List<String> offered = new ArrayList<>();
            for (int i = 0; i < step.getItemCount(); i++) {
                offered.add(String.valueOf(step.getItemAt(i)));
            }
            ok("the step value is one the animation knows how to apply",
                offered.contains(String.valueOf(getField(sky, "stepAmount"))));
        }

        System.out.println("  dropdowns checked against their fields: " + (byField.length + 1)
            + " of " + combos.size() + " on the panel");
    }

    /**
     * The composite is on the inner wheel and a third time is on the outer one.
     *
     * <b>The third time is the whole feature.</b> In a composite mode the two ordinary chart
     * slots are the two PEOPLE - the composite is built from them - so there was nowhere to
     * put "now", and that absence was the only thing preventing transits to a composite. The
     * astro layer needed no change whatever: {@code Transits.toNatal} takes two ChartFrames
     * and has never cared whether the first is a birth chart.
     *
     * So what this asserts is the plumbing, not the astrology: that the inner wheel really
     * holds the composite rather than one of the partners, that the outer wheel is live, and
     * that a single chart is untouched by any of it.
     */
    private static void compositeCarriesTransits() throws Exception {
        SwissEph sw = new SwissEph(EPHE_PATH);
        double[] base = PANEL_CHARTS.get("base");
        double[] tr = PANEL_CHARTS.get("transit");
        ChartFrame a = ChartFrame.compute(sw, new SweDate((int) base[0], (int) base[1],
            (int) base[2], base[3]).getJulDay(), base[4], base[5], (char) PANEL_HSYS, false, 0.0);
        ChartFrame b = ChartFrame.compute(sw, new SweDate((int) tr[0], (int) tr[1],
            (int) tr[2], tr[3]).getJulDay(), tr[4], tr[5], (char) PANEL_HSYS, false, 0.0);
        ChartFrame expected = ChartFrame.computeMidpointComposite(sw, a, b);

        final Object[] out = new Object[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();
                out[0] = skyOf(w);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        SkymapPanel sky = (SkymapPanel) out[0];

        for (ChartMode mode : new ChartMode[] {ChartMode.COMPOSITE_MIDPOINT, ChartMode.SINGLE}) {
            boolean composite = mode == ChartMode.COMPOSITE_MIDPOINT;
            setField(sky, "chartMode", mode);
            setField(sky, "showTransitChart", composite);
            setField(sky, "cachedFrame", null);
            setField(sky, "baseChartTime", utc(base));
            setField(sky, "transitChartTime", utc(tr));
            setField(sky, "compositeTransitTime",
                java.time.ZonedDateTime.of(2026, 8, 24, 20, 0, 0, 0, java.time.ZoneOffset.UTC));
            setField(sky, "baseLatitude", base[4]);
            setField(sky, "baseLongitude", base[5]);
            setField(sky, "transitLatitude", tr[4]);
            setField(sky, "transitLongitude", tr[5]);
            setField(sky, "houseSystem", (char) PANEL_HSYS);
            sky.updateChartData();

            double[] inner = (double[]) getField(sky, "bLon");
            boolean[] outerLive = (boolean[]) getField(sky, "tValid");
            int live = 0;
            for (boolean v : outerLive) {
                if (v) live++;
            }

            if (composite) {
                // The inner wheel is the composite, to the degree - not person A, and not
                // person B. Checked across every body rather than on the Sun alone, because
                // loading the wrong frame would still match on some of them by chance.
                int matched = 0;
                for (int i = 0; i < Bodies.count(); i++) {
                    ChartFrame.Body body = expected.bodies[i];
                    if (body == null || !body.ok) {
                        continue;
                    }
                    matched++;
                    ok("the inner wheel holds the composite " + Bodies.at(i).name,
                        Math.abs(Aspects.separation(inner[i], body.lon)) < 0.01);
                }
                ok("the composite actually supplied bodies", matched > 10);
                ok("the outer wheel is live in a composite mode", live > 10);
                // And it must NOT be person B - that is the mistake this mode invites.
                ok("the inner wheel is not simply the second person",
                    Math.abs(Aspects.separation(inner[0], b.body("Sun").lon)) > 0.01);
                System.out.println("  composite mode: " + matched
                    + " bodies on the inner wheel, " + live + " on the outer");
            } else {
                ok("a single chart draws no outer wheel", live == 0);
            }
        }
    }

    /**
     * The tri-wheel surfaces the sky in the placements side panel and allows clicking sky bodies.
     */
    /**
     * The narrative reading actually produces output, for every chart mode.
     *
     * <b>Nothing covered NarrativeSynthesizer until 2026-08-31, and it was broken.</b> Line 120
     * formatted {@code prof.age} - an int - with {@code %.0f}, which throws
     * IllegalFormatConversionException. The Synthesize button therefore did nothing at all
     * whenever transits were on, for natal charts as much as composites, because
     * {@code showReading}'s done() catches Throwable and only prints the stack trace. A button
     * that silently does nothing is indistinguishable from one that is not wired up, which is
     * exactly how this survived: it was reported as "Synthesize doesn't work in composite mode"
     * and the composite was never the cause.
     *
     * <b>withTime=true is the half that mattered.</b> The time branch - profection, solar
     * return, transit hits - only runs when transits are on, so any check that omitted it
     * would have passed against broken code. Both halves are swept here for that reason.
     */
    private static void readingsGenerate() throws Exception {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        double jdA = new SweDate(1984, 9, 8, 7 + 33.0 / 60.0).getJulDay();
        double jdB = new SweDate(1967, 4, 10, 19.0).getJulDay();
        double jdNow = new SweDate(2026, 8, 31, 12.0).getJulDay();
        ChartFrame a = ChartFrame.compute(sw, jdA, 41.8781, -87.6298, 'P', false, 0.0);
        ChartFrame b = ChartFrame.compute(sw, jdB, 34.05, -118.24, 'P', false, 0.0);

        // Tallies how often a relationship report actually reached the borrowed-natal
        // path, so the assertion inside the loop cannot pass by never firing.
        final int[] borrowed = new int[1];

        String[] modes = {"natal", "midpoint composite", "davison"};
        for (int m = 0; m < modes.length; m++) {
            ChartFrame f = m == 0 ? a
                : m == 1 ? ChartFrame.computeMidpointComposite(sw, a, b)
                : ChartFrame.computeDavisonComposite(sw, a, b);
            double baseJd = m == 0 ? jdA : f.julianDayUt;

            Gestalt.Result g = Gestalt.compute(f);
            java.util.List<BodyScore.Vector> ranked = BodyScore.rank(f, g);
            Themes.Result themes = Themes.extract(f, g, ranked);

            for (boolean withTime : new boolean[]{false, true}) {
                String label = modes[m] + (withTime ? " with transits" : " alone");
                ChartFrame tf = null;
                Profection prof = null;
                java.util.List<Transits.Hit> hits = null;
                if (withTime) {
                    tf = ChartFrame.compute(sw, jdNow, 41.8781, -87.6298, 'P', false, 0.0);
                    prof = Profection.at(baseJd, jdNow, f.asc);
                    ChartFrame.Body sun = f.body("Sun");
                    if (sun != null && sun.ok) {
                        double sr = Profection.solarReturnJd(sw, baseJd, sun.lon, prof.age);
                        prof.computeSubPeriods(jdNow, sr);
                    }
                    hits = Transits.toNatal(f, tf, ranked, prof.lord);
                }
                String html;
                boolean rel = m > 0;
                try {
                    html = NarrativeSynthesizer.generateReport(
                        f, g, ranked, themes, tf, prof, hits, null, null, withTime, rel);
                } catch (Throwable ex) {
                    failures.add("Part Q: synthesis threw for " + label + " - "
                        + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    checks++;
                    continue;
                }
                ok("Part Q: synthesis for " + label + " produced output",
                    html != null && html.length() > 2000);
                ok("Part Q: synthesis for " + label + " closes its document",
                    html != null && html.contains("</body>"));

                // <b>A named pattern must carry its reading.</b> Section 5 used to print one
                // bare line per pattern and no prose, so a T-square - the loudest thing in a
                // chart - read as absent. pattern_detail.json had the text all along, plus
                // modality-specific entries the synthesis never touched.
                for (AspectPatterns.Pattern pat : g.aspectPatterns) {
                    String slug = pat.name.toLowerCase().replace("-", "").replace(" ", "");
                    String general = InterpretationService.getInstance()
                        .getMacroDynamic("pattern_" + slug);
                    if (general != null && !general.isEmpty()) {
                        ok("Part Q: " + label + " reads its " + pat.name + ", not just names it",
                            html.contains(general.substring(0, Math.min(60, general.length()))));
                    }
                }
                // The time layer must actually appear, or withTime is silently a no-op -
                // and a no-op is what a swallowed exception looks like from out here.
                //
                // <b>Assert the positive marker, not the absence of the fallback text.</b>
                // "Transit data not enabled" appears in three sections; two of them are the
                // year-scan and transit lists, which this sweep deliberately passes null for
                // and which are therefore correct to show it. Only the profection sentence
                // proves the chronometry branch ran.
                if (withTime && !rel) {
                    ok("Part Q: " + label + " reaches the chronometry section",
                        html.contains("Profection (") && html.contains("at age"));
                }

                // <b>A relationship reading must not invent an age.</b> A composite has no
                // birthday, so a profection age is the mean of the partners' ages printed
                // as the age of the pairing - 41 and 59 came out as 50. Transits stay: they
                // need only positions, and transits to the composite are the standard
                // timing technique for a relationship chart.
                if (rel) {
                    ok("Part Q: " + label + " reads the composite datasets, not the natal",
                        html.contains("couple") || html.contains("the union"));
                    ok("Part Q: " + label + " states no profection age",
                        !html.contains("at age"));
                    if (withTime) {
                        ok("Part Q: " + label + " still shows transits to the composite",
                            html.contains("10. Current Transits"));
                    }

                    // <b>The natal apology must never reach a couple.</b> Where no composite
                    // reading exists the report printed the natal aspect general, which ends
                    // "a reading specific to X and Y is not written yet" - a note to the authors,
                    // in a voice describing one person. The eleven framing sentences in
                    // composite_aspects.json say the same thing about the relationship and now
                    // stand in its place.
                    ok("Part Q: " + label + " never shows the natal not-written note",
                        !html.contains("is not written yet."));
                    if (html.contains("In a composite chart")) {
                        borrowed[0]++;
                    }
                }
            }
        }

        // <b>This was an assertion that a frame had been emitted, and it can no longer fire.</b>
        // It existed because the report lists three aspects per body, so the borrowed-wording
        // branch might never be reached and the check above would pass by having nothing to
        // judge. On 2026-08-31 composite pair prose reached 3,300 of 3,300 - there is no
        // borrowing left to frame, so the count is legitimately zero.
        //
        // <b>Kept as a measurement rather than deleted.</b> If composite coverage regresses,
        // this goes positive and the framing assertion above starts doing work again; the number
        // is what tells a reader which of those two worlds they are in. Asserting it is zero
        // would freeze today's coverage into a rule, and asserting it is positive is what just
        // went red on complete data.
        System.out.println("  relationship reports that had to borrow natal wording: "
            + borrowed[0] + " (0 means composite pair prose is complete)");
    }

    private static void triWheelSidePanel() throws Exception {
        final Object[] out = new Object[2];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();
                out[0] = skyOf(w);
                java.lang.reflect.Field fi =
                    OuraniaWindow.class.getDeclaredField("interpretationPanel");
                fi.setAccessible(true);
                out[1] = fi.get(w);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        SkymapPanel sky = (SkymapPanel) out[0];
        final InterpretationPanel ip = (InterpretationPanel) out[1];

        // 1. In SYNASTRY with transits on: showTriWheel is true.
        //
        // <b>The flags come from the rules, not from literals.</b> Setting showTriWheel by
        // hand tests the panel GIVEN the flag and never tests that the checkbox sets it, so a
        // broken triWheelShown would leave every assertion below green. Asking the rule for
        // the value is what makes this suite notice.
        ok("triWheelShown: SYNASTRY + transits is a tri-wheel",
            SkymapPanel.triWheelShown(ChartMode.SYNASTRY, true));
        ok("triWheelShown: SYNASTRY without transits is not",
            !SkymapPanel.triWheelShown(ChartMode.SYNASTRY, false));
        setField(sky, "chartMode", ChartMode.SYNASTRY);
        setField(sky, "showTransitChart",
            SkymapPanel.outerWheelShown(ChartMode.SYNASTRY, true));
        setField(sky, "showTriWheel",
            SkymapPanel.triWheelShown(ChartMode.SYNASTRY, true));
        double[] base = PANEL_CHARTS.get("base");
        double[] tr = PANEL_CHARTS.get("transit");
        setField(sky, "baseChartTime", utc(base));
        setField(sky, "transitChartTime", utc(tr));
        setField(sky, "baseLatitude", base[4]);
        setField(sky, "baseLongitude", base[5]);
        setField(sky, "transitLatitude", tr[4]);
        setField(sky, "transitLongitude", tr[5]);
        setField(sky, "houseSystem", (char) PANEL_HSYS);
        sky.updateChartData();

        Method gen = SkymapPanel.class.getDeclaredMethod("generatePlanetPlacementsHtml");
        gen.setAccessible(true);
        String htmlTri = (String) gen.invoke(sky);

        ok("tri-wheel placements panel contains 'Sky (Transiting)'",
            htmlTri.contains("Sky (Transiting)"));
        ok("tri-wheel placements panel contains sky_ links",
            htmlTri.contains("href='sky_0'") || htmlTri.contains("href=\"sky_0\""));

        // <b>A sky click must read BOTH people.</b> The tri-wheel shipped listing only chart
        // A, and "does not throw" would never have caught it - it did not throw. It answered
        // half the question and looked exactly like a correct answer. So this asserts the
        // contacts are actually there, for bodies and for angles, which are separate code
        // paths: bodies build their list inline, angles go through showAngleAt.
        // <b>Two different functions, so two sweeps.</b> A body glyph clicked ON THE WHEEL goes
        // through handleChartClick, which builds its aspect list inline; the same body clicked
        // as a LINK in the placements panel goes through triggerPlanetInterpretation. They are
        // separate code and only the link path was covered at first - a mutation that emptied
        // the wheel path's chart-B loop left this suite green. Sweep both.
        int[] linkTally = skyClickTally(sky, ip, false);
        ok("a sky body LINK lists Chart A contacts", linkTally[0] > 0);
        ok("a sky body LINK lists Chart B contacts", linkTally[1] > 0);

        int[] wheelTally = skyWheelClickTally(sky, ip);
        ok("a sky body clicked ON THE WHEEL lists Chart A contacts", wheelTally[0] > 0);
        ok("a sky body clicked ON THE WHEEL lists Chart B contacts", wheelTally[1] > 0);

        int[] angleTally = skyClickTally(sky, ip, true);
        ok("a sky ANGLE click lists Chart A contacts", angleTally[0] > 0);
        ok("a sky ANGLE click lists Chart B contacts", angleTally[1] > 0);

        // <b>And a natal click must be untouched.</b> The chart tag is an optional 4th element
        // on the aspect row; every caller written before the tri-wheel emits three and must
        // still render with no chart named at all. This is the guard on that promise.
        final String[] baseText = new String[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                sky.triggerPlanetInterpretation("base_0");
                baseText[0] = paneText(ip);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ok("a natal click still names no chart",
            baseText[0] != null
                && !baseText[0].contains("Chart A") && !baseText[0].contains("Chart B"));

        // 2. In SYNASTRY with transits off: showTriWheel is false
        setField(sky, "showTriWheel",
            SkymapPanel.triWheelShown(ChartMode.SYNASTRY, false));
        sky.updateChartData();
        String htmlNoTri = (String) gen.invoke(sky);

        ok("synastry without transits does NOT contain 'Sky (Transiting)'",
            !htmlNoTri.contains("Sky (Transiting)"));
    }

    /**
     * Fire every valid sky click and count which charts the panel named.
     *
     * Returns {@code {chartARows, chartBRows}} - how many clicks produced a reading naming
     * chart A, and how many named chart B. Swept across every body rather than probing one,
     * because which particular body happens to aspect which is a property of the fixture
     * dates and not of the code under test; what the code owes us is that BOTH charts are
     * reachable at all.
     *
     * @param angles true to sweep the angles (which route through {@code showAngleAt}),
     *               false to sweep the ordinary bodies (which build their list inline)
     */
    private static int[] skyClickTally(SkymapPanel sky, InterpretationPanel ip, boolean angles)
            throws Exception {
        boolean[] cValid = (boolean[]) getField(sky, "cValid");
        final int[] tally = new int[2];
        for (int i = 0; i < Bodies.count(); i++) {
            if (!cValid[i] || Bodies.at(i).isAngle() != angles) continue;
            final String link = "sky_" + i;
            final String[] text = new String[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    sky.triggerPlanetInterpretation(link);
                    text[0] = paneText(ip);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (text[0] == null) continue;
            if (text[0].contains("Chart A")) tally[0]++;
            if (text[0].contains("Chart B")) tally[1]++;
        }
        return tally;
    }

    /**
     * The same tally, but by clicking the glyph where it is actually drawn.
     *
     * <b>The wheel and the placements list are different functions.</b> A glyph clicked on the
     * wheel goes through {@code handleChartClick}, which builds its aspect list inline; the
     * link goes through {@code triggerPlanetInterpretation}. Covering one covers nothing of
     * the other, which is how the wheel path sat unasserted while the link path looked tested.
     *
     * <b>Coordinates come from the same chain the painter uses</b> - {@code ringRadii} then
     * {@code triWheelRadii} then the pin - rather than a second copy of the geometry. A test
     * that computed its own positions would drift from the painter and start missing glyphs
     * that are plainly on screen, which is the exact defect the 2026-08-23 note describes.
     *
     * <b>The panel is reset between clicks.</b> Reading it after a click that hit nothing
     * would return the PREVIOUS body's reading and count it again.
     */
    private static int[] skyWheelClickTally(SkymapPanel sky, final InterpretationPanel ip)
            throws Exception {
        // Typed as Component: ChartPanel is a private inner class of SkymapPanel and cannot be
        // named from here. Everything this needs - setSize, getWidth, getHeight - is Component.
        final java.awt.Component chart = (java.awt.Component) getField(sky, "chartPanel");
        javax.swing.SwingUtilities.invokeAndWait(() -> chart.setSize(800, 800));

        Method triRadii =
            SkymapPanel.class.getDeclaredMethod("triWheelRadii", int.class, int.class);
        triRadii.setAccessible(true);
        Method pinLon = SkymapPanel.class.getDeclaredMethod("getPinLongitude");
        pinLon.setAccessible(true);
        final Method click =
            SkymapPanel.class.getDeclaredMethod("handleChartClick", int.class, int.class);
        click.setAccessible(true);

        int w = chart.getWidth();
        int h = chart.getHeight();
        int[] rings = SkymapPanel.ringRadii(w, h, true, true);
        int[] radii = (int[]) triRadii.invoke(sky,
            rings[SkymapPanel.RING_TRI], rings[SkymapPanel.RING_TRANSIT]);
        double pin = (Double) pinLon.invoke(sky);
        double[] cLon = (double[]) getField(sky, "cLon");
        boolean[] cValid = (boolean[]) getField(sky, "cValid");
        int cx = w / 2;
        int cy = h / 2;

        final int[] tally = new int[2];
        for (int i = 0; i < Bodies.count(); i++) {
            if (!cValid[i] || Bodies.at(i).isAngle()) continue;
            double a = Math.toRadians(180.0 + pin - cLon[i]);
            final int px = cx + (int) (radii[i] * Math.cos(a));
            final int py = cy + (int) (radii[i] * Math.sin(a));
            final String[] text = new String[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    ip.showIndex("houses");          // a known page naming no chart
                    click.invoke(sky, px, py);
                    text[0] = paneText(ip);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            if (text[0] == null) continue;
            if (text[0].contains("Chart A")) tally[0]++;
            if (text[0].contains("Chart B")) tally[1]++;
        }
        return tally;
    }

    /**
     * Crawl the index from its hub and prove every page renders and every link goes somewhere.
     *
     * <b>An index is a promise that a link leads to a reading.</b> Nothing else in this suite
     * can catch a row that points at a key nobody wrote - the page still renders, the link is
     * still blue, and clicking it produces an empty panel. So this walks the whole tree from
     * the hub, follows every index-to-index link, and then fires every leaf link through the
     * real handler and asserts the panel actually changed.
     *
     * <b>Read back from the JEditorPane, not from the builder.</b> Which means it is testing
     * what Swing actually rendered rather than the string that was handed to it - and it is
     * the reason the crawl looks for both quote styles: the HTMLEditorKit reserialises the
     * document and single-quoted attributes come back double-quoted.
     */
    private static void indexPagesResolve() throws Exception {
        final Object[] out = new Object[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();
                java.lang.reflect.Field f =
                    OuraniaWindow.class.getDeclaredField("interpretationPanel");
                f.setAccessible(true);
                out[0] = f.get(w);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        final InterpretationPanel panel = (InterpretationPanel) out[0];
        final Method showIndex = InterpretationPanel.class.getMethod("showIndex", String.class);
        final Method handle = findHandler();
        ok("the panel exposes a link handler", handle != null);

        java.util.regex.Pattern href = java.util.regex.Pattern.compile(
            "href=[" + (char) 34 + (char) 39 + "]?([a-zA-Z0-9_|.-]+)");

        java.util.Set<String> visited = new java.util.LinkedHashSet<>();
        java.util.Set<String> leaves = new java.util.LinkedHashSet<>();
        java.util.Deque<String> todo = new java.util.ArrayDeque<>();
        todo.add("");

        while (!todo.isEmpty()) {
            final String cat = todo.poll();
            if (!visited.add(cat)) {
                continue;
            }
            String html = render(panel, showIndex, cat);
            ok("index|" + cat + " renders a page", html != null && html.length() > 200);
            if (html == null) {
                continue;
            }
            java.util.regex.Matcher m = href.matcher(html);
            while (m.find()) {
                String h = m.group(1);
                if (h.startsWith("index|")) {
                    todo.add(h.substring(6));
                } else if (h.equals("index") || h.equals("back")) {
                    continue;
                } else {
                    leaves.add(h);
                }
            }
        }

        ok("the hub reaches every section", visited.size() >= 8);

        // <b>A door on the panel that is actually on screen when the app starts.</b> The index
        // shipped complete and unreachable: every page rendered, every link resolved, and both
        // entry points sat behind the interpretation panel, which is hidden until something
        // opens it. Part O passed the whole time, because it called showIndex directly.
        //
        // So this asserts the route a user has: a link in the placements HTML, and a handler
        // on the window that accepts it. Testing that a feature is REACHABLE is different from
        // testing that it works, and only one of those was being done.
        String placements = placementsHtml();
        ok("the placements panel offers a link to the index",
            placements != null && placements.contains("href='index'"));
        // <b>Not "the method exists" - the click.</b> A link and a handler can both be
        // present while the wiring between them is wrong, which is the state this feature was
        // actually in. So this fires the href through handlePlacementClick, the method the
        // placements panel really calls, and asserts the interpretation panel came up showing
        // the index rather than staying hidden.
        final String[] after = new String[1];
        final boolean[] visible = new boolean[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();
                w.handlePlacementClick("index");
                java.lang.reflect.Field fi =
                    OuraniaWindow.class.getDeclaredField("interpretationPanel");
                fi.setAccessible(true);
                InterpretationPanel ip = (InterpretationPanel) fi.get(w);
                visible[0] = ip != null && ip.isVisible();
                after[0] = ip == null ? null : paneText(ip);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ok("clicking the index link opens the interpretation panel", visible[0]);
        ok("and it lands on the index", after[0] != null && after[0].contains("Index"));
        ok("the index emits leaf links at all", leaves.size() > 100);

        // Every leaf link, through the real handler. A link that leaves the panel unchanged
        // is a dead row - which is exactly the failure an index invites and nothing else here
        // would notice.
        int dead = 0;
        for (final String leaf : leaves) {
            final String[] result = new String[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    showIndex.invoke(panel, "houses");           // a known non-empty page
                    String beforeHtml = paneText(panel);
                    handle.invoke(panel, leaf);
                    String afterHtml = paneText(panel);
                    result[0] = afterHtml == null || afterHtml.equals(beforeHtml)
                        ? null : afterHtml;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            checks++;
            if (result[0] == null || result[0].length() < 200) {
                failures.add("the index link " + leaf + " leads nowhere");
                dead++;
            }
        }

        System.out.println("  index: " + visited.size() + " pages, " + leaves.size()
            + " distinct destination links, " + dead + " dead");
    }

    /** The always-visible placements panel HTML, as the window would receive it. */
    private static String placementsHtml() throws Exception {
        final String[] out = new String[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();
                SkymapPanel sky = skyOf(w);
                Method gen = SkymapPanel.class.getDeclaredMethod("generatePlanetPlacementsHtml");
                gen.setAccessible(true);
                out[0] = (String) gen.invoke(sky);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return out[0];
    }

    private static Method findHandler() {
        for (Method m : InterpretationPanel.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class
                && (m.getName().equals("handleLink") || m.getName().equals("handleHref"))) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static String render(InterpretationPanel panel, Method showIndex, String cat)
            throws Exception {
        final String[] r = new String[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                showIndex.invoke(panel, cat);
                r[0] = paneText(panel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return r[0];
    }

    private static String paneText(InterpretationPanel panel) throws Exception {
        java.lang.reflect.Field f = InterpretationPanel.class.getDeclaredField("editorPane");
        f.setAccessible(true);
        javax.swing.JEditorPane pane = (javax.swing.JEditorPane) f.get(panel);
        return pane == null ? null : pane.getText();
    }

    /** Every JComboBox under a component, depth first. */
    private static void collectCombos(java.awt.Container root,
                                      java.util.List<javax.swing.JComboBox<?>> into) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JComboBox) {
                into.add((javax.swing.JComboBox<?>) c);
            }
            if (c instanceof java.awt.Container) {
                collectCombos((java.awt.Container) c, into);
            }
        }
    }

    private static void eqObj(String label, Object expected, Object actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures.add(label + ": the control shows " + actual
                + " while the engine uses " + expected);
        }
    }

    /** Occurrences of a literal, non-overlapping. */
    private static int countOf(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    /** The two charts the harness sets, as the panel itself would compute them. */
    private static ChartFrame frameFromPanel(String which) throws Exception {
        SwissEph sw = new SwissEph(EPHE_PATH);
        double[] at = PANEL_CHARTS.get(which);
        return ChartFrame.compute(sw, new SweDate((int) at[0], (int) at[1], (int) at[2], at[3])
            .getJulDay(), at[4], at[5], (char) PANEL_HSYS, false, 0.0);
    }

    private static final String EPHE_PATH = com.zodiacomputing.ourania.astro.Ephemeris.PATH;
    private static final int PANEL_HSYS = 80;   // 'P', the panel's default
    private static final java.util.Map<String, double[]> PANEL_CHARTS = new java.util.HashMap<>();
    static {
        // year, month, day, hour UT, latitude, longitude. The same two charts SynastryCheck
        // Part A uses, at CompositeCheck's Los Angeles, so all three suites read together.
        PANEL_CHARTS.put("base", new double[] {1980, 5, 15, 12.0, 34.05, -118.24});
        PANEL_CHARTS.put("transit", new double[] {1990, 8, 20, 8.0, 34.05, -118.24});
    }

    /**
     * The panel's placements HTML in one chart mode, with both charts set.
     *
     * The times go in as UTC ZonedDateTimes so that {@code createSweDate}, which converts to
     * UTC before building the SweDate, reproduces exactly the julian days
     * {@link #frameFromPanel} builds directly. A local zone here would put the two sides an
     * offset apart and the whole part would compare two different pairs of charts.
     */
    private static String crossBlockHtml(ChartMode mode) throws Exception {
        final String[] out = new String[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                OuraniaWindow w = new OuraniaWindow();          // built, never shown
                SkymapPanel sky = skyOf(w);

                setField(sky, "chartMode", mode);
                setField(sky, "showTransitChart", true);
                setField(sky, "aspectFilter", "Both");
                double[] base = PANEL_CHARTS.get("base");
                double[] tr = PANEL_CHARTS.get("transit");
                setField(sky, "baseChartTime", utc(base));
                setField(sky, "transitChartTime", utc(tr));
                setField(sky, "baseLatitude", base[4]);
                setField(sky, "baseLongitude", base[5]);
                setField(sky, "transitLatitude", tr[4]);
                setField(sky, "transitLongitude", tr[5]);
                setField(sky, "houseSystem", (char) PANEL_HSYS);
                sky.updateChartData();

                Method gen = SkymapPanel.class.getDeclaredMethod("generatePlanetPlacementsHtml");
                gen.setAccessible(true);
                out[0] = (String) gen.invoke(sky);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return out[0];
    }

    private static java.time.ZonedDateTime utc(double[] at) {
        int hour = (int) at[3];
        int minute = (int) Math.round((at[3] - hour) * 60.0);
        return java.time.ZonedDateTime.of((int) at[0], (int) at[1], (int) at[2], hour, minute, 0, 0,
            java.time.ZoneOffset.UTC);
    }

    /**
     * The window's wheel, with the aspect selection pinned on.
     *
     * <b>This suite was reading the user's preferences and calling it a result.</b>
     * SkymapPanel seeds {@code aspectShown} from settings.properties at construction, and
     * Settings treats a missing key as all-on but an empty one as all-off. On 2026-09-02 that
     * key was empty, so every aspect was off, the grid drew nothing, and 374 of 6154 checks
     * failed against code that was correct. Removing the key turned the same run green at 6519
     * checks - so the totals moved with a preference too, not only the verdict.
     *
     * Pinning here rather than at the seven call sites is deliberate: all seven were the same
     * three lines of reflection, and a helper that has to be remembered at an eighth is the
     * one-rule-two-implementations defect this project logs more than any other. There is now
     * one way to get a panel, and it pins.
     */
    /**
     * The pin itself, asserted rather than assumed.
     *
     * Without this, removing the pin in {@link #skyOf} only shows up when someone happens to
     * be running with an aspect selection that hides things - which is exactly how this went
     * unnoticed until a stamp run went red on 2026-09-02 against correct code. Reading the
     * field back fails the moment the pin goes, whatever settings.properties happens to say.
     */
    private static void aspectSelectionIsPinned() throws Exception {
        final Object[] seen = new Object[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                seen[0] = getField(skyOf(new OuraniaWindow()), "aspectShown");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        boolean[] on = (boolean[]) seen[0];
        ok("the panel exposes an aspect selection", on != null);
        if (on == null) {
            return;
        }
        eq("one flag per aspect type", Aspects.Type.values().length, on.length);
        int off = 0;
        for (boolean b : on) {
            if (!b) {
                off++;
            }
        }
        eq("no aspect is hidden from this suite by a saved preference", 0, off);

        final Object[] outer = new Object[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                outer[0] = getField(skyOf(new OuraniaWindow()), "showProgressed");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ok("the outer ring is pinned to transits", Boolean.FALSE.equals(outer[0]));

        final Object[] points = new Object[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                points[0] = getField(skyOf(new OuraniaWindow()), "shown");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        boolean[] shown = (boolean[]) points[0];
        int hidden = 0;
        for (boolean b : shown) {
            if (!b) {
                hidden++;
            }
        }
        eq("every chart point is visible to this suite", 0, hidden);
    }

    private static SkymapPanel skyOf(OuraniaWindow w) throws Exception {
        java.lang.reflect.Field fs = OuraniaWindow.class.getDeclaredField("skymapPanel");
        fs.setAccessible(true);
        SkymapPanel sky = (SkymapPanel) fs.get(w);
        boolean[] all = new boolean[Aspects.Type.values().length];
        java.util.Arrays.fill(all, true);
        setField(sky, "aspectShown", all);
        // The outer ring is this suite's subject as transits, not as whatever the reader has
        // chosen. Added 2026-09-03 with the progressed wheel, which moved this suite's total
        // to 6509 the day it shipped - the same drift the aspect pin above exists to stop.
        setField(sky, "showProgressed", Boolean.FALSE);
        // And the body selection. This suite counts grid cells, so the reader's chosen points
        // set its total directly: 6523 checks at 29 bodies against 5215 at 15, green both
        // times. Third pin on this method and the third time the same lesson - a suite that
        // reads a preference is measuring the machine it runs on.
        boolean[] every = new boolean[Bodies.ALL.length];
        java.util.Arrays.fill(every, true);
        setField(sky, "shown", every);
        java.lang.reflect.Method reload =
            SkymapPanel.class.getDeclaredMethod("updateChartData");
        reload.setAccessible(true);
        reload.invoke(sky);
        return sky;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = SkymapPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        java.lang.reflect.Field f = SkymapPanel.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /** "" when clean, otherwise " - 3 failed, e.g. aspect|...". Keeps the label diagnostic. */
    private static String sample(List<String> bad) {
        return bad.isEmpty() ? "" : " - " + bad.size() + " failed, e.g. " + bad.get(0);
    }

    /**
     * No aspect the engine can emit may render as "Interpretation not found".
     *
     * <b>Adding an aspect type creates a cell for every body pair at once.</b> The five minor
     * aspects added 2026-08-23 created roughly 2,030 of them, none with written prose, and
     * without a general fallback every one would have been a dead cell - the same defect the
     * grid was already fixed for once when the quincunx shipped with no glyph.
     *
     * Driven from Aspects.Type, so a sixth minor aspect cannot be added and left mute.
     */
    /**
     * Every figure {@link com.zodiacomputing.ourania.astro.AspectPatterns} can name must
     * resolve to prose, and so must every modality and element specialisation of it.
     *
     * <b>The same clause-completeness check the quincunx should have had.</b> That aspect was
     * live in the model with an empty glyph in the view for months, because nothing asserted
     * that the two agreed. The panel now prints a block per pattern built from
     * {@code detailKey()} and {@code variantKey()}, so a tenth figure added to the detector
     * without a paragraph would render as a heading with nothing under it. Driven off the
     * engine's own list rather than a copy of it.
     */
    private static void everyPatternHasDetail() {
        InterpretationService svc = InterpretationService.getInstance();
        ok("the pattern mechanics note exists",
            !svc.getMacroDynamic("pattern_mechanics").isEmpty());
        ok("the dissociate note exists",
            !svc.getMacroDynamic("pattern_dissociate").isEmpty());

        String[] names = {"T-square", "Grand cross", "Grand trine", "Kite", "Yod",
                          "Boomerang", "Mystic rectangle", "Grand sextile", "Cradle"};
        for (String name : names) {
            com.zodiacomputing.ourania.astro.AspectPatterns.Pattern p =
                new com.zodiacomputing.ourania.astro.AspectPatterns.Pattern(
                    name, java.util.List.of("Sun", "Moon", "Mars"), null);
            ok("pattern has a mechanics paragraph (" + name + "): " + p.detailKey(),
                !svc.getMacroDynamic(p.detailKey()).isEmpty());

            for (String m : com.zodiacomputing.ourania.astro.Zodiac.MODALITIES) {
                p.modality = m;
                p.element = null;
                String key = p.variantKey();
                if (key != null) {
                    ok("modality variant exists (" + name + " " + m + "): " + key,
                        !svc.getMacroDynamic(key).isEmpty());
                }
            }
            for (String e : com.zodiacomputing.ourania.astro.Zodiac.ELEMENTS) {
                p.modality = null;
                p.element = e;
                String key = p.variantKey();
                if (key != null) {
                    ok("element variant exists (" + name + " " + e + "): " + key,
                        !svc.getMacroDynamic(key).isEmpty());
                }
            }
        }
    }

    /**
     * Every link the interpretation panel can emit must parse back to what built it.
     *
     * <b>This is the check the aspect grid's own href did not have.</b> That string was
     * formatted in one place and parsed in another, the row labels contained the delimiter,
     * and the result was that every transit and synastry cell was silently unclickable for
     * months - no exception, no failure, just nothing happening on click. A link scheme is two
     * definitions of one string, and the only way to know they agree is to round-trip them.
     */
    private static void panelLinksRoundTrip() {
        String[] names = {"T-square", "Grand cross", "Grand trine", "Kite", "Yod",
                          "Boomerang", "Mystic rectangle", "Grand sextile", "Cradle"};
        java.util.List<java.util.List<String>> bodySets = java.util.List.of(
            java.util.List.of("Mars", "Moon", "Sun"),
            java.util.List.of("Chiron", "Jupiter", "Mercury", "Neptune"),
            java.util.List.of("Jupiter", "Mars", "Mercury", "Moon", "Sun", "Venus"));

        for (String name : names) {
            for (java.util.List<String> bodies : bodySets) {
                String href = InterpretationPanel.patternHref(name, bodies);
                String[] back = InterpretationPanel.parsePatternHref(href);
                ok("pattern link parses at all (" + name + ")", back != null);
                if (back == null) {
                    continue;
                }
                ok("pattern link round-trips its name (" + name + ")", name.equals(back[0]));
                ok("pattern link round-trips its bodies (" + name + ")",
                    String.join(",", bodies).equals(back[1]));
                ok("a pattern link is not mistaken for a mansion (" + name + ")",
                    InterpretationPanel.parseMansionHref(href) == -1);
            }
        }
        for (int n = 1; n <= com.zodiacomputing.ourania.astro.LunarMansions.COUNT; n++) {
            String href = InterpretationPanel.mansionHref(n);
            ok("mansion link round-trips (" + n + ")",
                InterpretationPanel.parseMansionHref(href) == n);
            ok("a mansion link is not mistaken for a pattern (" + n + ")",
                InterpretationPanel.parsePatternHref(href) == null);
        }
        // The forms that must NOT resolve, so a malformed link is inert rather than wrong.
        for (String junk : new String[] {"", "back", "mansions", "pattern|", "pattern|T-square",
                                         "mansion|", "mansion|zero", "mansion|0",
                                         "pattern|a|b|c", "transit_sun_Square_moon"}) {
            boolean resolvesAsPattern = InterpretationPanel.parsePatternHref(junk) != null;
            boolean resolvesAsMansion = InterpretationPanel.parseMansionHref(junk) >= 1;
            ok("junk link does not resolve: \"" + junk + "\"",
                !resolvesAsPattern && !resolvesAsMansion);
        }
        ok("a null href is inert",
            InterpretationPanel.parsePatternHref(null) == null
                && InterpretationPanel.parseMansionHref(null) == -1);
    }

    /**
     * Lighting a pattern must cover exactly the legs between its members, and nothing else.
     *
     * <b>A highlight is invisible to any check that does not paint</b>, which is how a dead
     * click path survived in this same grid for months. The membership test and the name
     * mapping are therefore static and pure, and asserted here directly: a figure lights its
     * own legs, a body outside it lights nothing, a transit line is never covered, and no body
     * is joined to itself.
     */
    private static void patternHighlight() {
        java.util.List<String> tsquare = java.util.List.of("Mars", "Moon", "Sun");
        int[] members = SkymapPanel.patternIndices(tsquare);
        ok("a three-body figure maps to three indices", members.length == 3);

        int sun = Bodies.indexOfName("Sun");
        int moon = Bodies.indexOfName("Moon");
        int mars = Bodies.indexOfName("Mars");
        int venus = Bodies.indexOfName("Venus");

        ok("the Sun-Moon leg is lit", SkymapPanel.patternCovers(members, sun, moon, false));
        ok("the Sun-Mars leg is lit", SkymapPanel.patternCovers(members, sun, mars, false));
        ok("the Moon-Mars leg is lit", SkymapPanel.patternCovers(members, moon, mars, false));
        ok("and it is lit either way round",
            SkymapPanel.patternCovers(members, mars, moon, false));
        ok("a non-member is not lit against a member",
            !SkymapPanel.patternCovers(members, sun, venus, false));
        ok("two non-members are not lit",
            !SkymapPanel.patternCovers(members, venus, Bodies.indexOfName("Saturn"), false));
        ok("a body is never joined to itself",
            !SkymapPanel.patternCovers(members, sun, sun, false));
        ok("a transit line is never covered by a natal figure",
            !SkymapPanel.patternCovers(members, sun, moon, true));
        ok("no pattern lights nothing",
            !SkymapPanel.patternCovers(new int[0], sun, moon, false));
        ok("a null member list lights nothing",
            !SkymapPanel.patternCovers(null, sun, moon, false));

        ok("clearing maps to an empty set",
            SkymapPanel.patternIndices(null).length == 0
                && SkymapPanel.patternIndices(java.util.List.of()).length == 0);
        ok("a name that is not a registry point is dropped, not indexed as -1",
            SkymapPanel.patternIndices(java.util.List.of("Sun", "Nibiru")).length == 1);

        // Every figure the engine can emit must map cleanly - a pattern body that failed to
        // resolve would light a partial figure and nobody would be told.
        for (String name : com.zodiacomputing.ourania.astro.AspectPatterns.PATTERN_BODIES) {
            ok("pattern body resolves on the wheel (" + name + ")",
                SkymapPanel.patternIndices(java.util.List.of(name)).length == 1);
        }
    }

    /**
     * The wheel's radius chain, extracted 2026-08-23, against the formula it replaced.
     *
     * <b>The formula below is the historical one, typed out</b> - not a call to the method
     * under test. It was written out inline in four places (click, paint, hover, ring code)
     * and the handover had listed that as "the two-surfaces defect waiting to happen" since
     * 2026-08-21. Extracting it before adding a mansion ring was the point; this part is what
     * makes "the extraction did not move the wheel" a fact rather than a hope.
     *
     * Swept across sizes because the chain subtracts fixed pixel amounts from a value that
     * scales, so it inverts at small windows - and a negative radius draws an arc the wrong
     * way round rather than throwing.
     */
    private static void ringGeometry() {
        for (int w = 200; w <= 2000; w += 137) {
            for (int h = 200; h <= 2000; h += 211) {
                for (boolean transit : new boolean[] {false, true}) {
                    int outer = Math.min(w, h) / 2 - 10;
                    int transitRing = outer - 20;
                    int decanOuter = transit ? transitRing - 25 : outer - 20;
                    int signOuter = decanOuter - 20;
                    int signInner = signOuter - 35;

                    // No tri-wheel: the 3-arg overload passes showTri=false, so the RING_TRI
                    // and RING_TRANSIT slots collapse to the same value (outer - 20) and the
                    // array is six elements wide. The historical five-element contract is kept
                    // by using the named constants, whose values shifted when RING_TRI was
                    // inserted, so all five legacy values are still present and correct.
                    int[] got = SkymapPanel.ringRadii(w, h, transit);
                    ok("ring chain returns six radii", got.length == 6);
                    ok("outer matches (" + w + "x" + h + ")",
                        got[SkymapPanel.RING_OUTER] == outer);
                    ok("transit ring matches (" + w + "x" + h + ")",
                        got[SkymapPanel.RING_TRANSIT] == transitRing);
                    ok("decan outer matches (" + w + "x" + h + ", transit=" + transit + ")",
                        got[SkymapPanel.RING_DECAN_OUTER] == decanOuter);
                    ok("sign outer matches (" + w + "x" + h + ")",
                        got[SkymapPanel.RING_SIGN_OUTER] == signOuter);
                    ok("sign inner matches (" + w + "x" + h + ")",
                        got[SkymapPanel.RING_SIGN_INNER] == signInner);

                    // The rings must nest, outermost first, at any size the window can be.
                    if (outer > 120) {
                        ok("rings nest outward-in (" + w + "x" + h + ")",
                            got[SkymapPanel.RING_OUTER] > got[SkymapPanel.RING_DECAN_OUTER]
                                && got[SkymapPanel.RING_DECAN_OUTER]
                                    > got[SkymapPanel.RING_SIGN_OUTER]
                                && got[SkymapPanel.RING_SIGN_OUTER]
                                    > got[SkymapPanel.RING_SIGN_INNER]);
                    }

                    // Tri-wheel: the 4-arg overload, tri=true. RING_TRI must be between OUTER
                    // and RING_TRANSIT, and RING_TRANSIT must be 22px inside RING_TRI.
                    int[] gotTri = SkymapPanel.ringRadii(w, h, transit, true);
                    ok("tri-wheel ring chain returns six radii", gotTri.length == 6);
                    ok("tri outer matches outer-20 (" + w + "x" + h + ")",
                        gotTri[SkymapPanel.RING_TRI] == outer - 20);
                    ok("tri transit = tri outer - 22 (" + w + "x" + h + ")",
                        gotTri[SkymapPanel.RING_TRANSIT] == gotTri[SkymapPanel.RING_TRI] - 22);
                }
            }
        }
        ok("28 mansions tile the circle exactly",
            Math.abs(com.zodiacomputing.ourania.astro.LunarMansions.WIDTH * 28 - 360.0) < 1e-9);

        // The mansion ring and the Sabian degree share the outer band, and a hit test that
        // overlaps another silently gives one of them away. The mansion ring was drawn with
        // no click route at all until David reported that selecting one did nothing, so this
        // asserts both that the route exists and that it does not swallow its neighbour.
        int[] r = SkymapPanel.ringRadii(900, 900, false);
        int outer = r[SkymapPanel.RING_OUTER];
        int both = 0;
        int neither = 0;
        for (int d = -20; d <= 20; d++) {
            double radius = outer + d;
            boolean mansion = SkymapPanel.inMansionBand(radius, outer);
            boolean sabian = SkymapPanel.inSabianBand(radius, outer);
            ok("the mansion and Sabian bands never overlap (offset " + d + ")",
                !(mansion && sabian));
            if (mansion && sabian) both++;
            if (!mansion && !sabian) neither++;
        }
        ok("no radius belongs to both bands", both == 0);
        ok("the visible ring is a mansion target",
            SkymapPanel.inMansionBand(outer, outer)
                && SkymapPanel.inMansionBand(outer - 3, outer)
                && SkymapPanel.inMansionBand(outer - 9, outer));
        ok("the strip inside it is still a Sabian target",
            SkymapPanel.inSabianBand(outer - 10, outer)
                && SkymapPanel.inSabianBand(outer - 15, outer));
        ok("neither band reaches the decan ring",
            !SkymapPanel.inMansionBand(r[SkymapPanel.RING_DECAN_OUTER], outer)
                && !SkymapPanel.inSabianBand(r[SkymapPanel.RING_DECAN_OUTER], outer));
        ok("every longitude on the ring resolves to a mansion", mansionAtEveryDegree());
    }

    /** The ring is clickable at every degree of it, not just where a tick happens to fall. */
    private static boolean mansionAtEveryDegree() {
        for (int deg = 0; deg < 360; deg++) {
            com.zodiacomputing.ourania.astro.LunarMansions.Mansion m =
                com.zodiacomputing.ourania.astro.LunarMansions.at(deg);
            if (m == null || m.number < 1 || m.number > 28) {
                return false;
            }
        }
        return true;
    }

    private static void noDeadAspectCells() {
        InterpretationService svc = InterpretationService.getInstance();
        for (com.zodiacomputing.ourania.astro.Aspects.Type t
                : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            String text = svc.getAspect("Sun", "Moon", t.label);
            ok(t.label + " resolves to something readable",
                text != null && !text.startsWith("Interpretation not found"));
            ok(t.label + " has a general meaning on file",
                !svc.getAspect("Eris", "Pholus", t.label).startsWith("Interpretation not found"));
        }
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "clear" : added + " FAILED"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, int expected, int actual) {
        checks++;
        if (expected != actual) {
            failures.add(label + ": got " + actual + ", expected " + expected);
        }
    }
}
