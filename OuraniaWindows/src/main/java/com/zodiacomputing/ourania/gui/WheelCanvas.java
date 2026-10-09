package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Almanac;
import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Dignity;
import com.zodiacomputing.ourania.astro.Convergence;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Profection;
import com.zodiacomputing.ourania.astro.Progressions;
import com.zodiacomputing.ourania.astro.Returns;
import com.zodiacomputing.ourania.astro.Snapshot;
import com.zodiacomputing.ourania.astro.Synastry;
import com.zodiacomputing.ourania.astro.SolarArc;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import com.zodiacomputing.ourania.astro.Transits;
import com.zodiacomputing.ourania.astro.Zodiac;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * The flat wheel's canvas: the component the chart is painted on and hovered over.
 *
 * <h3>Why this is its own file (J13, step 5b)</h3>
 *
 * <p>It was {@code SkymapPanel.ChartPanel}, an inner class, so every one of its reads of the
 * panel was invisible - an inner class reaches its outer instance's fields without saying
 * which. Step 5b made it a top-level class holding the panel through one named field, which
 * made those reads countable; step 6 wrote them down as {@link WheelSource} and typed the
 * field as that, so the compiler now holds the painter to the list. It reads; it does not
 * write, bar {@link WheelSource#clampView}.
 *
 * <p>The pieces it is drawn from are {@link WheelShapes}'s (step 5a). Moved rather than
 * changed, and measured as such: the wheel rendered in 49 chart states gives the same pixels.
 */
final class WheelCanvas extends JPanel {

    /** The chart this canvas draws, and everything it may read about it. */
    private final WheelSource panel;

    /** A glyph size's font, made once per point size. {@link WheelLayout.GlyphSize} holds the number. */
    private static final java.util.Map<Integer, Font> GLYPH_FONTS = new java.util.HashMap<>();

    static Font glyphFont(WheelLayout.GlyphSize size) {
        return GLYPH_FONTS.computeIfAbsent(size.fontPoints, p -> new Font("SansSerif", 0, p));
    }

    WheelCanvas(WheelSource panel) {
        this.panel = panel;
    }

    

    /**
     * The hover card, asked for rather than pushed.
     *
     * <b>This is why hovering a body showed nothing.</b> mouseMoved used to call
     * setToolTipText with whatever hoverTextAt returned, and a comment here said that
     * passing null over empty space "unregisters the component, which is exactly the
     * behaviour wanted". It is not: ToolTipManager.unregisterComponent removes the
     * listeners it uses to track the pointer. The wheel is mostly empty space, so the
     * first move over blank chart unregistered the panel; moving on to a glyph set the
     * text again, but no mouseEntered can fire while the cursor is already inside the
     * component, so the manager never woke up and no card was ever shown.
     *
     * Overriding this instead keeps the panel registered for the life of the window and
     * lets the answer be per position. Returning null here suppresses the card without
     * touching registration, which is the behaviour that comment actually wanted.
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        if (panel.globeMode()) {
            return panel.hoverTextAt(event.getX(), event.getY());
        }
        java.awt.Point at = panel.toWheel(event.getX(), event.getY());
        return panel.hoverTextAt(at.x, at.y);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Object object;
        int n;
        int n2;
        int n3;
        double d;
        int n4;
        int n5;
        int n6;
        int n7;
        double d2;
        double d3;
        int n8;
        int n9;
        super.paintComponent(graphics);
        // <b>Jet black, deliberately flat.</b> A radial gradient was tried here on
        // 2026-08-31 to suggest depth and removed the same day: against the muted
        // element palette the lifted centre greyed the ground and the copper and sage
        // glyphs lost contrast. Pure black is the strongest backing those colours have.
        // <b>The wheel's ground, and the last surface that was not on the theme.</b>
        // Black since the app existed - which left the chart reading as a hole cut in the
        // window once everything around it moved to a navy ground. Overridable in
        // Settings; black is still what it falls back to, so a reader who never opens
        // Settings sees the chart they had yesterday.
        graphics.setColor(AwtPen.colorOr(ChartPalette.backgroundHex(null),
            Color.BLACK));
        graphics.fillRect(0, 0, this.getWidth(), this.getHeight());
        if (!panel.chartReady()) {
            return;
        }
        Graphics2D graphics2D = (Graphics2D)graphics;
        // <b>The other view of the same chart.</b> Everything below this line draws the
        // flat wheel; the globe reads the same arrays and paints them as shells. One
        // branch, at the top, because the two share no drawing at all - the alternative
        // is a flag threaded through five thousand lines of painter.
        if (panel.globeMode()) {
            panel.paintGlobe(graphics2D, this.getWidth(), this.getHeight());
            SkymapPanel.paintZodiacTag(graphics2D, this.getWidth(), this.getHeight());
            panel.paintScrubTag(graphics2D, this.getWidth());
            return;
        }
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int n10 = this.getWidth();
        int n11 = this.getHeight();
        // The zoom and pan, for the screen only - see viewTransform. Everything drawn from
        // here to the hover swell is in the wheel's own coordinates.
        final java.awt.geom.AffineTransform screenTx = graphics2D.getTransform();
        final boolean onScreen = this.getClientProperty(ChartExporter.EXPORTING) == null;
        if (onScreen) {
            // Re-clamped at the size being painted: a window made smaller while zoomed would
            // otherwise keep a pan that now runs past the wheel's edge.
            panel.clampView();
            graphics2D.transform(panel.viewTransform(n10, n11));
        }
        // The same geometry both hit tests and the click dispatcher use. This painter is
        // the site that diverged: it drew bodies at WheelLayout.RING_SIGN_INNER while bodyAt tested
        // bodyBaseRadius, so with the reader's placement anywhere but the default the
        // glyphs and the clicks were tens of pixels apart.
        SkymapPanel.Geometry g = panel.geometry(n10, n11);
        if (g == null) {
            return;
        }
        int n12 = g.cx;
        int n13 = g.cy;
        int[] rings = g.rings;
        int n14 = n9 = rings[WheelLayout.RING_OUTER];
        int nTriOuter = rings[WheelLayout.RING_TRI];
        int n15 = rings[WheelLayout.RING_TRANSIT];
        int n16 = rings[WheelLayout.RING_DECAN_OUTER];
        int n17 = rings[WheelLayout.RING_SIGN_OUTER];
        int n18 = rings[WheelLayout.RING_SIGN_INNER];
        int nBodyTop = rings[WheelLayout.RING_BODY_TOP];
        int nTermInner = rings[WheelLayout.RING_TERM_INNER];
        int nDegreeInner = rings[WheelLayout.RING_DEGREE_INNER];
        // The rim scale's outer edge: the rim itself with the mansions folded, and the
        // underside of the mansion band once they open.
        int nMansionInner = rings[WheelLayout.RING_MANSION_INNER];
        // <b>The disc, filled separately from the page.</b> One colour used to do both -
        // "Wheel" repainted the whole panel - so the chart could never sit ON anything.
        // Filled before any ring is drawn, so every stroke below lands on top of it.
        Color disc = AwtPen.colorOr(ChartPalette.wheelHex(null), null);
        if (disc != null) {
            graphics2D.setColor(disc);
            graphics2D.fillOval(n12 - n14, n13 - n14, n14 * 2, n14 * 2);
        }
        double[] dArray = panel.activeCusps();
        double d4 = panel.getPinLongitude();
        graphics2D.setColor(SkymapPanel.inkColor());
        graphics2D.setStroke(new BasicStroke(1.0f));
        graphics2D.drawLine(n12 - 10, n13, n12 + 10, n13);
        graphics2D.drawLine(n12, n13 - 10, n12, n13 + 10);
        graphics2D.setColor(SkymapPanel.inkColor());
        graphics2D.setStroke(new BasicStroke(2.0f));
        if (panel.layerShown(Layer.SIGNS)) {
            graphics2D.drawOval(n12 - n18, n13 - n18, n18 * 2, n18 * 2);
            graphics2D.drawOval(n12 - n17, n13 - n17, n17 * 2, n17 * 2);
        }
        if (panel.layerShown(Layer.DECANS)) {
            graphics2D.drawOval(n12 - n16, n13 - n16, n16 * 2, n16 * 2);
        }
        graphics2D.drawOval(n12 - nTermInner, n13 - nTermInner,
            nTermInner * 2, nTermInner * 2);
        graphics2D.drawOval(n12 - nDegreeInner, n13 - nDegreeInner,
            nDegreeInner * 2, nDegreeInner * 2);
        if (panel.outerRingDrawn()) {
            // Both boundaries of the partner band. Since the reorder it hangs below the
            // zodiac rather than outside it, so its floor is a line of its own - without
            // it the band bleeds into the natal wheel and stops reading as a field.
            graphics2D.setColor(new Color(80, 40, 80));
            graphics2D.drawOval(n12 - n15, n13 - n15, n15 * 2, n15 * 2);
            graphics2D.drawOval(n12 - nBodyTop, n13 - nBodyTop,
                nBodyTop * 2, nBodyTop * 2);
        }
        // Tri-wheel: an additional ring outside the synastry ring.
        if (panel.triRingDrawn()) {
            graphics2D.setColor(new Color(30, 60, 80));
            graphics2D.drawOval(n12 - nTriOuter, n13 - nTriOuter, nTriOuter * 2, nTriOuter * 2);
        }
        graphics2D.setFont(new Font("SansSerif", 0, 12));
        for (n8 = 0; panel.layerShown(Layer.DECANS) && n8 < 36; ++n8) {
            d3 = (double)n8 * 10.0;
            d2 = Math.toRadians(180.0 + d4 - d3);
            n7 = n12 + (int)((double)n17 * Math.cos(d2));
            n6 = n13 + (int)((double)n17 * Math.sin(d2));
            n5 = n12 + (int)((double)n16 * Math.cos(d2));
            n4 = n13 + (int)((double)n16 * Math.sin(d2));
            graphics2D.setColor(Color.LIGHT_GRAY);
            graphics2D.setStroke(new BasicStroke(1.0f));
            graphics2D.drawLine(n7, n6, n5, n4);
            d = Math.toRadians(180.0 + d4 - (d3 + 5.0));
            n3 = n12 + (int)((double)(n16 - 10) * Math.cos(d));
            n2 = n13 + (int)((double)(n16 - 10) * Math.sin(d));
            // <b>One band, two traditions, and the reader picks which one it draws.</b>
            // Visual only: the prose and the tarot keep their own schemes whatever this
            // says, because they are bound to datasets that cannot be remapped. See
            // Settings.decanRing for why a global decan-system toggle is not on offer.
            int n19 = n8 / 3;
            if (Settings.DECAN_RING_CHALDEAN.equals(Settings.decanRing())) {
                String faceRuler = Zodiac.chaldeanDecanRuler(Zodiac.SIGNS[n19], n8 % 3 + 1);
                int fi = Bodies.indexOfName(faceRuler);
                if (fi >= 0 && fi < SkymapPanel.BODY_GLYPHS.length) {
                    graphics2D.setColor(panel.bodyColor(fi));
                    graphics2D.drawString(SkymapPanel.BODY_GLYPHS[fi], n3 - 4, n2 + 4);
                    continue;
                }
                // An unresolvable ruler falls through to the sign glyph rather than
                // leaving a gap in the band, which would read as a rendering fault.
            }
            int n20 = Zodiac.triplicityDecanSignIndex(n19, n8 % 3 + 1);
            graphics2D.setColor(panel.getElementColor(Zodiac.elementIndex(n20)));
            graphics2D.drawString(SkymapPanel.ZODIAC_SYMBOLS[n20], n3 - 4, n2 + 4);
        }
        // <b>The Egyptian bounds, one glyph per segment.</b> Dignity has scored bound
        // placements since it was written and nothing drew them, so a reader comparing
        // this wheel against another program found the terms simply missing. Sixty
        // segments, five to a sign, each ruled by one of the five non-luminary planets -
        // read from Dignity rather than from a second copy of the table.
        if (panel.layerShown(Layer.BOUNDS)) {
            WheelShapes.drawBoundRing(graphics2D, n12, n13, n18, nTermInner, d4,
                panel::bodyColor);
        }

        // <b>The second degree scale, sitting directly above the wheels.</b> The outer
        // ticks are at the rim beside the lunar mansions, too far from any glyph to read
        // a body against; this one is where a body's leader line lands, so a reader can
        // follow a glyph out to the degree it actually occupies.
        if (panel.layerShown(Layer.DEGREES)) {
            WheelShapes.drawInnerDegreeRing(graphics2D, n12, n13, nTermInner,
                nDegreeInner, d4);
        }

        // The 28 lunar mansions, in a band of their own at the rim.
        //
        // <b>It used to share the rim with the degree ticks and cover them.</b> The band is
        // now carved out of the rim by the chain - WheelLayout.RING_MANSION_INNER - and the ticks below
        // read from the same number, so the two cannot overlap. Carving it here rather than
        // inside the zodiac is what keeps the bodies where they are: every radius from
        // WheelLayout.RING_DECAN_OUTER inward is untouched.
        if (panel.layerShown(Layer.MANSIONS)) {
            WheelShapes.drawMansionRing(graphics2D, n12, n13, n14, nMansionInner, d4,
                panel.moonMansion());
        }

        graphics2D.setFont(new Font("SansSerif", 0, 22));
        for (n8 = 0; panel.layerShown(Layer.SIGNS) && n8 < 12; ++n8) {
            d3 = (double)n8 * 30.0;
            d2 = Math.toRadians(180.0 + d4 - d3);
            n7 = n12 + (int)((double)n18 * Math.cos(d2));
            n6 = n13 + (int)((double)n18 * Math.sin(d2));
            n5 = n12 + (int)((double)n17 * Math.cos(d2));
            n4 = n13 + (int)((double)n17 * Math.sin(d2));
            graphics2D.setColor(SkymapPanel.inkColor());
            graphics2D.setStroke(new BasicStroke(2.0f));
            graphics2D.drawLine(n7, n6, n5, n4);
            d = Math.toRadians(180.0 + d4 - (d3 + 15.0));
            n3 = n12 + (int)((double)(n17 - 18) * Math.cos(d));
            n2 = n13 + (int)((double)(n17 - 18) * Math.sin(d));
            graphics2D.setColor(panel.getElementColor(Zodiac.elementIndex(n8)));
            graphics2D.drawString(SkymapPanel.ZODIAC_SYMBOLS[n8], n3 - 9, n2 + 6);
        }
        graphics2D.setColor(new Color(150, 150, 150));
        for (n8 = 0; panel.layerShown(Layer.DEGREES) && n8 < 360; ++n8) {
            d3 = Math.toRadians(180.0 + d4 - (double)n8);
            // Hanging from the mansion band's underside rather than from the rim, so the
            // two scales and the mansions are three separate bands the reader can tell
            // apart. With the mansions folded this is the rim, exactly as before.
            int n21 = nMansionInner;
            n = n8 % 10 == 0 ? n21 - 6 : (n8 % 5 == 0 ? n21 - 4 : n21 - 2);
            n7 = n12 + (int)((double)n * Math.cos(d3));
            n6 = n13 + (int)((double)n * Math.sin(d3));
            n5 = n12 + (int)((double)n21 * Math.cos(d3));
            n4 = n13 + (int)((double)n21 * Math.sin(d3));
            graphics2D.setStroke(new BasicStroke(n8 % 10 == 0 ? 1.5f : 0.5f));
            graphics2D.drawLine(n7, n6, n5, n4);
        }
        for (n8 = 1; panel.layerShown(Layer.HOUSES) && n8 <= 12; ++n8) {
            d3 = dArray[n8];
            double d5 = Math.toRadians(180.0 + d4 - d3);
            n7 = n12;
            n6 = n13;
            n5 = n12 + (int)((double)n18 * Math.cos(d5));
            n4 = n13 + (int)((double)n18 * Math.sin(d5));
            if (n8 == 1 || n8 == 4 || n8 == 7 || n8 == 10) {
                graphics2D.setColor(SkymapPanel.inkColor());
                graphics2D.setStroke(new BasicStroke(3.0f));
            } else {
                graphics2D.setColor(Color.DARK_GRAY);
                graphics2D.setStroke(new BasicStroke(2.0f));
            }
            graphics2D.drawLine(n7, n6, n5, n4);
            double d6 = d = n8 == 12 ? dArray[1] : dArray[n8 + 1];
            if (d < d3) {
                d += 360.0;
            }
            double d7 = d3 + (d - d3) / 2.0;
            double d8 = Math.toRadians(180.0 + d4 - d7);
            // <b>Just inside the aspect field's rim, not under the sign ring.</b> The number
            // sat 15 px inside the sign ring's inner edge, which was open space when it was
            // placed; the bounds ring and the degree scale have since grown into exactly that
            // band, and an 11 px digit among the bound glyphs and the 0/10/20 labels read as
            // no house numbers at all (reported 2026-09-14). The rim of the aspect disc is
            // the one ring on the wheel nothing else is drawn on.
            int houseR = g.natalFloor - 16;
            int n22 = n12 + (int)((double)houseR * Math.cos(d8));
            int n23 = n13 + (int)((double)houseR * Math.sin(d8));
            graphics2D.setColor(panel.getElementColor(Zodiac.elementIndex(n8 - 1)));
            graphics2D.setFont(Theme.font("Arial", 1, 15));
            java.awt.FontMetrics houseFm = graphics2D.getFontMetrics();
            String houseText = String.valueOf(n8);
            graphics2D.drawString(houseText, n22 - houseFm.stringWidth(houseText) / 2,
                n23 + houseFm.getAscent() / 2 - 1);
        }
        if (panel.showTransitChart() && "Both".equals(panel.houseAlignment())) {
            Stroke stroke = graphics2D.getStroke();
            graphics2D.setStroke(new BasicStroke(2.0f, 0, 0, 10.0f, new float[]{4.0f, 4.0f}, 0.0f));
            for (int i = 1; i <= 12; ++i) {
                // Across the partner band, which is where those cusps belong - not from
                // the decan ring, which the reorder moved to the other side of the wheel.
                double d9 = Math.toRadians(180.0 + d4 - panel.outerRing().cusps[i]);
                n = n12 + (int)((double)nBodyTop * Math.cos(d9));
                n7 = n13 + (int)((double)nBodyTop * Math.sin(d9));
                n6 = n12 + (int)((double)n15 * Math.cos(d9));
                n5 = n13 + (int)((double)n15 * Math.sin(d9));
                n4 = i == 1 || i == 4 || i == 7 || i == 10 ? 1 : 0;
                graphics2D.setColor(n4 != 0 ? new Color(210, 150, 230) : new Color(120, 80, 140));
                graphics2D.drawLine(n, n7, n6, n5);
            }
            graphics2D.setStroke(stroke);
        }
        // Body radii come from the shared geometry, not from n18. n18 is WheelLayout.RING_SIGN_INNER
        // and this painter used to draw bodies there whatever placement the reader chose,
        // while bodyAt tested bodyBaseRadius - 325 against 201 in the centre at 820px, so
        // nothing on the wheel could be clicked. n18 keeps WheelLayout.RING_SIGN_INNER for the sign
        // circle it strokes and the spokes it ends, because those are the sign ring.
        int[] nArray = g.natalRadii();
        int[] nArray2 = g.transitRadii();
        int[] nArrayC = g.triRadii();
        // The three aspect fields. Lines are drawn on these, not between the glyphs -
        // see SkymapPanel.Geometry.aspectDisc for why, and note that the hit test reads the same
        // three numbers, because a line you can see has to be a line you can hover.
        int discNatal = g.aspectDisc(0);
        int discOuter = g.aspectDisc(1);
        int discSky = g.aspectDisc(2);
        // <b>One circle, where the lines stop and the bodies start.</b> Three nested
        // fields of chords with nothing around them read as weather in the middle of the
        // wheel; a single boundary at the natal band's floor turns them into a thing with
        // an edge. Nothing has to be clipped to it: every chord has both ends on a disc
        // inside this radius, so the widest line the wheel can draw is a diameter of the
        // outermost field and still falls short of the circle around it.
        //
        // Alpha 190 rather than the 120 it was first drawn at. A circle sampled across ten
        // spokes of the rendered wheel registered on six of them at 120, where every other
        // ring line registered on ten - present in the file and absent to a reader, which
        // is the one outcome a boundary cannot have. Still below the zodiac's full-weight
        // strokes, because it marks an edge rather than another ring.
        Color edge = SkymapPanel.inkColor();
        graphics2D.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 190));
        graphics2D.setStroke(new BasicStroke(1.5f));
        graphics2D.drawOval(n12 - g.natalFloor, n13 - g.natalFloor,
            g.natalFloor * 2, g.natalFloor * 2);
        int n24 = n18 - 60;
        graphics2D.setStroke(new BasicStroke(0.5f));
        // <b>The layer folds the lines; the filter chooses which families.</b> Two
        // different questions, and a reader who folded the aspects away expects all of
        // them gone whatever the filter says.
        boolean aspectLayer = panel.layerShown(Layer.ASPECTS);
        boolean bl = aspectLayer && panel.drawsNatalAspects();
        int n25 = n = aspectLayer && panel.drawsCrossAspects() ? 1 : 0;
        if (bl) {
            for (n7 = 0; n7 < SkymapPanel.BODY_COUNT; ++n7) {
                if (!WheelLayout.aspecting(n7, panel.natalRing().valid)) continue;
                for (n6 = n7 + 1; n6 < SkymapPanel.BODY_COUNT; ++n6) {
                    if (!WheelLayout.aspecting(n6, panel.natalRing().valid) || Bodies.isOppositePair(n7, n6)) continue;
                    this.drawAspectLine(graphics2D, panel.natalRing().lon[n7], panel.natalRing().lon[n6], d4, n12, n13, discNatal, discNatal, WheelLayout.WHEEL_NATAL, n7, n6);
                }
            }
        }
        if (n != 0) {
            for (n7 = 0; n7 < SkymapPanel.BODY_COUNT; ++n7) {
                if (!WheelLayout.aspecting(n7, panel.outerRing().valid)) continue;
                for (n6 = 0; n6 < SkymapPanel.BODY_COUNT; ++n6) {
                    if (!WheelLayout.aspecting(n6, panel.natalRing().valid)) continue;
                    this.drawAspectLine(graphics2D, panel.outerRing().lon[n7], panel.natalRing().lon[n6], d4, n12, n13, discOuter, discOuter, WheelLayout.WHEEL_OUTER, n7, n6);
                }
            }
        }
        // <b>The sky ring had no aspect lines at all.</b> It could be drawn, hovered and
        // read, and the one thing the wheel is for - showing what is in aspect to what -
        // stopped at the ring below it. With a field of its own there is now somewhere to
        // put them where they do not cross the other two.
        if (n != 0 && panel.triRingDrawn()) {
            for (n7 = 0; n7 < SkymapPanel.BODY_COUNT; ++n7) {
                if (!WheelLayout.aspecting(n7, panel.skyRing().valid)) continue;
                for (n6 = 0; n6 < SkymapPanel.BODY_COUNT; ++n6) {
                    if (!WheelLayout.aspecting(n6, panel.natalRing().valid)) continue;
                    this.drawAspectLine(graphics2D, panel.skyRing().lon[n7],
                        panel.natalRing().lon[n6], d4, n12, n13, discSky, discSky,
                        WheelLayout.WHEEL_SKY, n7, n6);
                }
            }
        }
        // Draw the hovered line once more, last, so it sits on top of the weave. Emphasis
        // alone is not enough: with 28 points switched on, a highlighted line drawn in
        // registry order disappears under every pair that comes after it.
        // The lit pattern goes down before the hovered line, for the same reason the
        // hovered line goes down after the weave: emphasis alone loses to draw order once
        // there are 28 points on the wheel. A figure is several lines, so all of its
        // member pairs are redrawn here - drawAspectLine draws nothing for a pair that
        // holds no aspect, so the loop can be over members rather than over the figure's
        // own legs, which the Pattern does not record.
        java.util.List<int[]> litFigures = new java.util.ArrayList<>();
        for (int[] members : panel.autoPatterns()) {
            litFigures.add(members);
        }
        if (panel.highlightPattern().length > 0) {
            litFigures.add(panel.highlightPattern());
        }
        for (int[] lit : litFigures) {
            for (int li = 0; li < lit.length; li++) {
                for (int lj = li + 1; lj < lit.length; lj++) {
                    int pa = lit[li];
                    int pb = lit[lj];
                    if (!WheelLayout.aspecting(pa, panel.natalRing().valid)
                        || !WheelLayout.aspecting(pb, panel.natalRing().valid)) {
                        continue;
                    }
                    this.drawAspectLine(graphics2D, panel.natalRing().lon[pa],
                        panel.natalRing().lon[pb], d4, n12, n13, discNatal, discNatal,
                        WheelLayout.WHEEL_NATAL, pa, pb);
                }
            }
        }

        int hlA = panel.highlightA();
        int hlB = panel.highlightB();
        int hlWheel = panel.highlightWheel();
        if (hlA >= 0 && hlB >= 0) {
            // <b>Off the ring it belongs to.</b> This redraw picked the outer wheel for
            // every cross-chart line, so a hovered sky aspect was drawn again on the
            // partner ring - a bright chord in the wrong field, next to the faint one it
            // was meant to be.
            int hlDisc = hlWheel == WheelLayout.WHEEL_SKY ? discSky
                : (hlWheel == WheelLayout.WHEEL_OUTER ? discOuter : discNatal);
            if (WheelLayout.aspecting(hlA, panel.wheelValid(hlWheel))
                && WheelLayout.aspecting(hlB, panel.natalRing().valid)) {
                this.drawAspectLine(graphics2D,
                    panel.wheelLon(hlWheel)[hlA], panel.natalRing().lon[hlB],
                    d4, n12, n13, hlDisc, hlDisc, hlWheel, hlA, hlB);
            }
        }
        // The transform the bodies are drawn in, so a hovered glyph's swell is undone
        // before anything else is painted - every loop below resets to it.
        final java.awt.geom.AffineTransform bodyTx = graphics2D.getTransform();
        for (n7 = 0; panel.layerShown(Layer.NATAL) && n7 < SkymapPanel.BODY_COUNT; ++n7) {
            if (!panel.natalRing().valid[n7]) continue;
            double d10 = Math.toRadians(180.0 + d4 - panel.natalRing().lon[n7]);
            n4 = n12 + (int)((double)nArray[n7] * Math.cos(d10));
            int n26 = n13 + (int)((double)nArray[n7] * Math.sin(d10));
            SkymapPanel.bulge(graphics2D, bodyTx, n4, n26, panel.hoverBody() == n7);
            if (panel.onHighlightedLine(n7, WheelLayout.WHEEL_NATAL)) {
                WheelShapes.drawHighlightHalo(graphics2D, n4, n26,
                    Bodies.at(n7).isAngle() ? 13 : WheelLayout.natalSize(n7).radius);
            }
            if (Bodies.at(n7).isAngle()) {
                graphics2D.setFont(SkymapPanel.ANGLE_FONT);
                // The angle marker follows the template: gold is invisible on a white
                // ground and is the only warm thing in a cool palette.
                Color angle = AwtPen.colorOr(ChartPalette.angleHex("#D4AF37"),
                    new Color(212, 175, 55));
                WheelShapes.drawBodyMarker(graphics2D, n4, n26, 13, angle,
                    Settings.natalMarker());
                // The glyph reads against its own bead rather than against a fixed dark
                // brown, which disappears on a pale marker.
                graphics2D.setColor(SkymapPanel.readableOn(angle));
                object = Bodies.at((int)n7).glyph;
                graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n26 + 4);
                continue;
            }
            // A body recedes with its lines unless it IS the focus or aspects it. Without
            // this the web dims and the glyphs stay bright, which reads as the lines being
            // broken rather than as one body being singled out.
            java.awt.Composite priorComposite = graphics2D.getComposite();
            if (!panel.noFocus()) {
                graphics2D.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER,
                    (float) panel.glyphWeight(n7, false)));
            }
            WheelLayout.GlyphSize glyphSize = WheelLayout.natalSize(n7);
            graphics2D.setFont(glyphFont(glyphSize));
            WheelShapes.drawNatalBody(graphics2D, n7, n4, n26, glyphSize.radius);
            if (n7 == SkymapPanel.MOON && panel.natalRing().valid[SkymapPanel.SUN]) {
                double d11 = (panel.natalRing().lon[SkymapPanel.MOON] - panel.natalRing().lon[SkymapPanel.SUN]) % 360.0;
                if (d11 < 0.0) {
                    d11 += 360.0;
                }
                WheelShapes.drawMoonPhase(graphics2D, n4, n26, Math.round((float)glyphSize.radius * 0.47f), d11 / 360.0);
            } else {
                // <b>The ink is chosen against what the glyph is standing on.</b> While every
                // bead was the same neutral grey the body's element colour read on all of
                // them; on the body's own face it does not - the Sun's glyph is fire red, and
                // red on a bright orange disc is a smudge rather than a symbol. Bodies with
                // no face keep the element ink, which is where that colour does its work.
                graphics2D.setColor(WheelShapes.natalGlyphInk(n7, panel.bodyColor(n7)));
                object = SkymapPanel.glyphFor(n7, glyphFont(glyphSize));
                WheelShapes.drawBodyLabel(graphics2D, (String)object, n4, n26, glyphSize.baseline);
            }
            graphics2D.setTransform(bodyTx);
            // The leader from the glyph to the degree it actually occupies. Bodies are
            // spread outward when they crowd, so without this a reader cannot tell which
            // degree a glyph belongs to. Colour and visibility are both settings; the
            // alpha stays here because a solid leader would compete with the aspect lines.
            graphics2D.setComposite(priorComposite);
            if (Settings.showDegreeLines()) {
                // <b>To the far scale, at the reader's asking.</b> These ran to the inner
                // degree scale on the argument that a leader crossing the decans and the
                // bounds is a line the reader has to trace rather than read. David,
                // 2026-09-16: "can we extend the line all the way to the second sabian ring
                // degree" - so they now reach the rim scale, which is the outer of the two.
                // The inner scale is still drawn and still readable; what changed is where
                // the line ends, and that is his call to make while looking at it.
                //
                // <b>The hovered body's leader is drawn to be seen.</b> At alpha 60 every
                // leader is a hint and none of them is an answer; the one the cursor is on
                // is the reader asking which degree this glyph occupies, so it is drawn
                // solid and the rest stay a background.
                boolean lit = panel.focusBody() == n7
                    && !panel.focusTransit();
                Color leader = AwtPen.colorOr(ChartPalette.leaderHex(null),
                    Color.WHITE);
                Stroke priorLeader = graphics2D.getStroke();
                graphics2D.setColor(new Color(leader.getRed(), leader.getGreen(),
                    leader.getBlue(), lit ? 235 : 60));
                graphics2D.setStroke(new BasicStroke(lit ? 1.8f : 1.0f));
                graphics2D.drawLine(n4, n26,
                    n12 + (int)((double)nMansionInner * Math.cos(d10)),
                    n13 + (int)((double)nMansionInner * Math.sin(d10)));
                graphics2D.setStroke(priorLeader);
            }
        }
        graphics2D.setTransform(bodyTx);
        if (panel.outerRingDrawn()) {
            Composite outerWas = graphics2D.getComposite();
            float outerA = SkymapPanel.ringAlpha(panel.outerOpenFraction());
            if (outerA < 0.999f) {
                graphics2D.setComposite(
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, outerA));
            }
            // The same question the angle cards ask, asked once for the whole ring: in a
            // synastry this wheel is a second person, anywhere else it is a moment.
            final AngleRole outerRole = panel.angleRoleFor(false, true);
            for (n7 = 0; n7 < SkymapPanel.BODY_COUNT; ++n7) {
                if (!panel.outerRing().valid[n7]) continue;
                double d12 = Math.toRadians(180.0 + d4 - panel.outerRing().lon[n7]);
                n4 = n12 + (int)((double)nArray2[n7] * Math.cos(d12));
                int n27 = n13 + (int)((double)nArray2[n7] * Math.sin(d12));
                SkymapPanel.bulge(graphics2D, bodyTx, n4, n27,
                    panel.hoverBody() == (n7 | WheelLayout.TRANSIT_BIT));
                if (panel.onHighlightedLine(n7, WheelLayout.WHEEL_OUTER)) {
                    WheelShapes.drawHighlightHalo(graphics2D, n4, n27,
                        Bodies.at(n7).isAngle() ? 13
                            : WheelLayout.transitSize(n7).radius);
                }
                if (Bodies.at(n7).isAngle()) {
                    graphics2D.setFont(SkymapPanel.ANGLE_FONT);
                    WheelShapes.drawBodyMarker(graphics2D, n4, n27, 13,
                        SkymapPanel.ringBead(outerRole),
                        SkymapPanel.outerRingMarker(panel.chartMode()));
                    graphics2D.setColor(panel.ringAngleInk(outerRole));
                    object = Bodies.at((int)n7).glyph;
                    graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n27 + 4);
                    continue;
                }
                WheelLayout.GlyphSize glyphSize2 = WheelLayout.transitSize(n7);
                graphics2D.setFont(glyphFont(glyphSize2));
                object = panel.ringInk(n7, outerRole);
                WheelShapes.drawBodyMarker(graphics2D, n4, n27, glyphSize2.radius,
                    SkymapPanel.ringBead(outerRole),
                    SkymapPanel.outerRingMarker(panel.chartMode()));
                if (n7 == SkymapPanel.MOON && panel.outerRing().valid[SkymapPanel.SUN]) {
                    double d13 = (panel.outerRing().lon[SkymapPanel.MOON] - panel.outerRing().lon[SkymapPanel.SUN]) % 360.0;
                    if (d13 < 0.0) {
                        d13 += 360.0;
                    }
                    WheelShapes.drawMoonPhase(graphics2D, n4, n27, Math.round((float)glyphSize2.radius * 0.44f), d13 / 360.0);
                    continue;
                }
                graphics2D.setColor((Color)object);
                String string = SkymapPanel.glyphFor(n7, glyphFont(glyphSize2));
                WheelShapes.drawBodyLabel(graphics2D, string, n4, n27, glyphSize2.baseline);
            }
            graphics2D.setComposite(outerWas);
        }
        // <b>The directed band (G17 step 3).</b> Every natal point moved by one arc, in a band
        // of its own between the middle ring and the sky - so a reader can see the progressed
        // chart and the directed chart at the same time, which is the comparison Convergence
        // scores a period on and the one the wheel could not draw until now.
        //
        // <b>No moon phase here, deliberately.</b> The outer ring draws one because its Sun and
        // Moon have moved at their own speeds; a directed Sun and Moon have both moved by the
        // SAME arc, so the angle between them is the natal angle exactly. Drawing it would be
        // the birth chart's phase wearing a directed ring's clothes.
        //
        // <b>No halo and no bulge either.</b> Both are keyed to a wheel index, and this band
        // has none - see WheelSource.arcRing on why it is draw-only.
        graphics2D.setTransform(bodyTx);
        if (panel.arcRingDrawn()) {
            Composite arcWas = graphics2D.getComposite();
            float arcA = SkymapPanel.ringAlpha(panel.arcOpenFraction());
            if (arcA < 0.999f) {
                graphics2D.setComposite(
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, arcA));
            }
            // A directed chart is never a person and never a moment, so the role it draws its
            // angles in is the same one the middle ring uses outside a synastry.
            final AngleRole arcRole = panel.angleRoleFor(false, true);
            int[] arcR = g.arcRadii();
            for (int ai = 0; ai < SkymapPanel.BODY_COUNT; ++ai) {
                if (!panel.arcRing().valid[ai]) continue;
                double arcAngle = Math.toRadians(180.0 + d4 - panel.arcRing().lon[ai]);
                int ax = n12 + (int) ((double) arcR[ai] * Math.cos(arcAngle));
                int ay = n13 + (int) ((double) arcR[ai] * Math.sin(arcAngle));
                if (Bodies.at(ai).isAngle()) {
                    graphics2D.setFont(SkymapPanel.ANGLE_FONT);
                    WheelShapes.drawBodyMarker(graphics2D, ax, ay, 13,
                        SkymapPanel.ringBead(arcRole),
                        SkymapPanel.outerRingMarker(panel.chartMode()));
                    graphics2D.setColor(panel.ringAngleInk(arcRole));
                    String arcGlyph = Bodies.at(ai).glyph;
                    graphics2D.drawString(arcGlyph,
                        ax - graphics2D.getFontMetrics().stringWidth(arcGlyph) / 2, ay + 4);
                    continue;
                }
                WheelLayout.GlyphSize arcSize = WheelLayout.transitSize(ai);
                graphics2D.setFont(glyphFont(arcSize));
                WheelShapes.drawBodyMarker(graphics2D, ax, ay, arcSize.radius,
                    SkymapPanel.ringBead(arcRole),
                    SkymapPanel.outerRingMarker(panel.chartMode()));
                graphics2D.setColor(panel.ringInk(ai, arcRole));
                WheelShapes.drawBodyLabel(graphics2D,
                    SkymapPanel.glyphFor(ai, glyphFont(arcSize)), ax, ay, arcSize.baseline);
            }
            graphics2D.setComposite(arcWas);
        }
        // Tri-wheel: sky positions in the outermost ring. Blue-tinted, and by default a
        // different shape from the synastry ring inside it - see Settings.MARKER_SHAPES
        // for why the tint alone was not enough.
        graphics2D.setTransform(bodyTx);
        if (panel.triRingDrawn()) {
            Composite triWas = graphics2D.getComposite();
            float triA = SkymapPanel.ringAlpha(panel.triOpenFraction());
            if (triA < 0.999f) {
                graphics2D.setComposite(
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, triA));
            }
            for (n7 = 0; n7 < SkymapPanel.BODY_COUNT; ++n7) {
                if (!panel.skyRing().valid[n7]) continue;
                double d14 = Math.toRadians(180.0 + d4 - panel.skyRing().lon[n7]);
                n4 = n12 + (int)((double)nArrayC[n7] * Math.cos(d14));
                int n28 = n13 + (int)((double)nArrayC[n7] * Math.sin(d14));
                // The sky ring never had this at all: its chords could light and the two
                // glyphs at their ends stayed dark, so a reader could see a sky aspect
                // and still have to work out which points it joined.
                if (panel.onHighlightedLine(n7, WheelLayout.WHEEL_SKY)) {
                    WheelShapes.drawHighlightHalo(graphics2D, n4, n28,
                        Bodies.at(n7).isAngle() ? 13
                            : WheelLayout.transitSize(n7).radius);
                }
                if (Bodies.at(n7).isAngle()) {
                    graphics2D.setFont(SkymapPanel.ANGLE_FONT);
                    WheelShapes.drawBodyMarker(graphics2D, n4, n28, 13,
                        SkymapPanel.ringBead(AngleRole.SKY), Settings.transitMarker());
                    graphics2D.setColor(panel.ringAngleInk(AngleRole.SKY));
                    object = Bodies.at((int)n7).glyph;
                    graphics2D.drawString((String)object, n4 - graphics2D.getFontMetrics().stringWidth((String)object) / 2, n28 + 4);
                    continue;
                }
                WheelLayout.GlyphSize glyphSize3 = WheelLayout.transitSize(n7);
                graphics2D.setFont(glyphFont(glyphSize3));
                Color cColor = panel.ringInk(n7, AngleRole.SKY);
                WheelShapes.drawBodyMarker(graphics2D, n4, n28, glyphSize3.radius,
                    SkymapPanel.ringBead(AngleRole.SKY), Settings.transitMarker());
                if (n7 == SkymapPanel.MOON && panel.skyRing().valid[SkymapPanel.SUN]) {
                    double d15 = (panel.skyRing().lon[SkymapPanel.MOON] - panel.skyRing().lon[SkymapPanel.SUN]) % 360.0;
                    if (d15 < 0.0) d15 += 360.0;
                    WheelShapes.drawMoonPhase(graphics2D, n4, n28, Math.round((float)glyphSize3.radius * 0.44f), d15 / 360.0);
                    continue;
                }
                graphics2D.setColor(cColor);
                String stringC = SkymapPanel.glyphFor(n7, glyphFont(glyphSize3));
                WheelShapes.drawBodyLabel(graphics2D, stringC, n4, n28, glyphSize3.baseline);
            }
            graphics2D.setComposite(triWas);
        }
        graphics2D.setTransform(bodyTx);
        panel.paintHover(graphics2D, g);
        graphics2D.setTransform(screenTx);
        SkymapPanel.paintZodiacTag(graphics2D, n10, n11);
        if (onScreen) {
            panel.paintFitChip(graphics2D);
            panel.paintScrubTag(graphics2D, n10);
        }
    }

    /**
     * How many widening passes make the glow.
     *
     * Three is where it stops being worth it: a fourth pass is wide enough to overlap its
     * neighbours and turns a chart with forty aspects into a haze. Measured by eye on a
     * chart with every point switched on, which is the case that breaks first.
     */
    private static final int GLOW_PASSES = 3;

    private void drawAspectLine(Graphics2D graphics2D, double d, double d2, double d3, int n, int n2, int n3, int n4, int wheel, int n5, int n6) {
        double d4 = Math.abs(d - d2);
        if (d4 > 180.0) {
            d4 = 360.0 - d4;
        }
        // wheel says which ring the first body is on, and used to be a boolean meaning
        // "the outer one" - which could not tell the partner ring from the sky ring, so
        // the two lit each other's lines. Only the partner ring is ever a synastry pair;
        // the sky ring is a moment and is judged at natal orbs, the same as the grid and
        // the hit test judge it.
        boolean bl = wheel != WheelLayout.WHEEL_NATAL;
        Aspects.Profile syn = panel.profileForPair(
            wheel == WheelLayout.WHEEL_OUTER);
        double d5 = panel.getOrbFor(n5, n6, syn);
        Color color = null;
        double d6 = 0.0;
        // Which aspect this is, and what colour it takes, both come from the one place
        // that already knows.
        //
        // <b>This used to be a hand-written if/else ladder</b> re-testing 0/60/90/120/180
        // with the colours inlined as RGB triples - a third copy of Aspects.typeOf and a
        // second copy of getAspectColorHex. It had no 150-degree branch for months, so the
        // grid listed quincunxes the wheel could not draw, and adding five more aspects to
        // a hand-written ladder would have been five more chances at the same silence.
        // Now a new constant on Aspects.Type appears here automatically.
        // Through the same gate the grid uses, so a switched-off aspect disappears from
        // both or from neither.
        Aspects.Type drawn = panel.visibleAspect(d4, n5, n6, syn);
        // The reader's aspect mode applies to the line and not to the aspect. A pair the
        // mode excludes is still computed, still in the grid, still in the reading - it
        // simply does not cross the middle of the wheel. See drawsPair.
        if (drawn != null && !panel.drawsPair(n5, n6)) {
            drawn = null;
        }
        if (drawn != null) {
            color = Color.decode(SkymapPanel.getAspectColorHex(drawn.label));
            d6 = drawn.exactAngle;
            // The alpha ramp below fades a line as it widens, and it has to fade against
            // the orb this aspect was actually judged by. Against the raw body orb a
            // 1-degree semisextile would render at nearly full strength however loose it is.
            d5 = Aspects.effectiveOrb(SkymapPanel.planetName(n5),
                SkymapPanel.planetName(n6), drawn, syn);
        }
        if (color != null) {
            // <b>What "close" is measured against.</b> Relative to the orb in force by
            // default, which is what the wheel has always done; in absolute mode, degrees
            // from exact, so that widening an orb adds lines without brightening the ones
            // already drawn. David, 25 Sep.
            double reach = Settings.aspectWeightAbsolute()
                ? Settings.ABSOLUTE_FADE_DEGREES : d5;
            double d7 = Math.min(1.0, Math.abs(d4 - d6) / Math.max(0.0001, reach));
            float f = (float)Math.pow(1.0 - d7, 2.0);
            int n7 = (int)(35.0f + 220.0f * f);
            n7 = Math.max(20, Math.min(255, n7));
            // Focus, applied on top of the orb fade rather than instead of it: a loose
            // aspect to the focused body is still a loose aspect and should still look
            // like one.
            double focus = panel.focusWeight(n5, n6, bl);
            n7 = Math.max(6, (int)(n7 * focus));
            // <b>Weight is the aspect's own force times how close it is to exact.</b> Before
            // this the width carried only proximity, so a semisextile at 0 degrees inked
            // exactly as heavily as a conjunction at 0 degrees - the two lines said the same
            // thing about very different aspects. Amplitude belongs to the harmonic family
            // and lives on Aspects.Type beside the harmonic it comes from.
            double amplitude = drawn == null ? 1.0 : drawn.amplitude();
            // A hovered line is drawn at full strength regardless of how wide its orb is.
            // The normal alpha ramp fades a loose aspect almost to nothing, which is right
            // for the background weave and useless for "show me the one I am pointing at".
            boolean highlighted = panel.isHighlighted(n5, n6, wheel);
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                highlighted ? 255 : n7);
            // <b>Scaled into a range a reader can actually see.</b> 0.3 to 1.0 was under
            // one pixel of variation, so the tightness rule was true and unreadable.
            double lo = Settings.aspectWeightMin();
            double hi = Settings.aspectWeightMax();
            float f2 = highlighted ? (float)(hi * 1.4)
                : (float)(lo + (hi - lo) * f * amplitude);
            double d8 = Math.toRadians(180.0 + d3 - d);
            double d9 = Math.toRadians(180.0 + d3 - d2);
            // <b>Where the ends sit, decided in one place.</b> Six call sites pass these
            // radii and deciding it at each of them would be six chances to disagree - the
            // defect this file has supplied all day. The first body is on `wheel`; the
            // second is always on the natal ring, which is what every call site does.
            int ra = panel.endpointRadius(wheel, n5, n3);
            int rb = panel.endpointRadius(WheelLayout.WHEEL_NATAL, n6, n4);
            int n8 = n + (int)((double)ra * Math.cos(d8));
            int n9 = n2 + (int)((double)ra * Math.sin(d8));
            int n10 = n + (int)((double)rb * Math.cos(d9));
            int n11 = n2 + (int)((double)rb * Math.sin(d9));
            Stroke stroke = graphics2D.getStroke();
            if (highlighted) {
                // White halo underneath, so the line reads against whichever aspect colour
                // it is and against the wheel's own spokes.
                graphics2D.setColor(new Color(255, 255, 255, 90));
                graphics2D.setStroke(new BasicStroke(f2 + 4.0f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
                graphics2D.drawLine(n8, n9, n10, n11);
            }
            // <b>The glow: a wide, faint pass of the same colour under the crisp line.</b>
            // Real bloom needs a blur, which Java2D will not do cheaply on every frame -
            // but two or three widening strokes at low alpha read as light spilling off a
            // strand, which is the whole effect. The passes are driven by the SAME f as the
            // line, so a tight aspect glows and a loose one barely does: the fade already
            // in this method is what makes the weave legible, and the glow must not undo it
            // by lighting up the aspects the ramp is busy hiding.
            if (!bl) {
                for (int pass = GLOW_PASSES; pass >= 1; pass--) {
                    int glowAlpha = (int) (n7 * 0.10f * f / pass);
                    if (glowAlpha < 3) {
                        continue;
                    }
                    graphics2D.setColor(new Color(color.getRed(), color.getGreen(),
                        color.getBlue(), Math.min(60, glowAlpha * 3)));
                    graphics2D.setStroke(new BasicStroke(f2 + pass * 2.4f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    graphics2D.drawLine(n8, n9, n10, n11);
                }
            }

            graphics2D.setColor(color);
            // <b>Which chart this line belongs to, from the ring itself.</b> This was
            // `if (bl)` - not the natal ring - so a partner's line, a transit and the sky
            // were dashed identically and a reader could not tell them apart. The ring knows
            // what it is since 383408c8; the pattern is its own.
            float[] dash = panel.ringAt(wheel).kind.dashPattern();
            if (dash != null) {
                graphics2D.setStroke(new BasicStroke(f2, 0, 0, 10.0f, dash, 0.0f));
            } else {
                // Thinner core than before (was f2 alone at up to 1.0). A strand reads as
                // light when the bright part is narrow and the spill is wide; a thick core
                // with a glow around it just looks like a thick line.
                graphics2D.setStroke(new BasicStroke(Math.max(0.6f, f2 * 0.8f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            graphics2D.drawLine(n8, n9, n10, n11);
            graphics2D.setStroke(stroke);
        }
    }
}
