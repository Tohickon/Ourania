package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

/**
 * The Life areas page: the chart grouped by the parts of a life, not by house number.
 *
 * <b>David, 2026-10-13: the tabs "only show the user the back end math side".</b> The
 * Placements page lists bodies by degree; a reader who is not an astrologer wants to know what
 * the chart says about their work, their home, their partner. So each occupied house becomes a
 * card headed with its {@link LifeAreas} phrase, each body in it gets the plain line its house
 * reading opens with, and the placements go in the card's why box for Astrologer view. Houses
 * with nothing in them are named together at the end, because "quiet" is a finding too.
 *
 * <b>The bodies are the ones the wheel shows.</b> Read from the natal ring's own valid flags,
 * the same switch the Reading page uses, so this page and that one never disagree about what
 * the chart contains.
 */
final class LifeAreasPanel extends JPanel {

    private final SkymapPanel skymap;
    private final JEditorPane pane;

    LifeAreasPanel(SkymapPanel skymap, OuraniaWindow window) {
        super(new java.awt.BorderLayout());
        this.skymap = skymap;
        this.pane = HtmlPanes.darkPane();
        this.pane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED
                    && window != null) {
                window.handlePlacementClick(e.getDescription());
            }
        });
        setBackground(Theme.SURFACE);
        add(HtmlPanes.scroller(this.pane), java.awt.BorderLayout.CENTER);
        refresh();
    }

    /** Rebuilt on every open, for the reason OverviewPanel.refresh gives. */
    void refresh() {
        pane.setText(html());
        pane.setCaretPosition(0);
    }

    String html() {
        ChartFrame f = skymap == null ? null : skymap.getCurrentChart();
        StringBuilder sb = new StringBuilder("<html><body>");
        if (f == null || f.cusps == null) {
            return sb.append("<p>No chart is cast yet. Set a date and place, and this page will "
                + "say which parts of life the chart puts its weight on.</p></body></html>")
                .toString();
        }
        sb.append("<h2>Life areas</h2>");
        sb.append(ReadingCard.html(cards(f), Settings.astrologerView()));
        List<String> quiet = quietAreas(f);
        if (!quiet.isEmpty()) {
            sb.append("<p style='color:#9AA5B1; font-size:12px;'><b>Quieter parts of life:</b> ")
              .append(esc(String.join("; ", quiet)))
              .append(". Nothing in the chart sits there, which usually means these run on "
                  + "habit rather than drawing your attention.</p>");
        }
        return sb.append("</body></html>").toString();
    }

    /** One card per occupied house, in house order. Package-visible for a check. */
    List<ReadingCard> cards(ChartFrame f) {
        InterpretationService svc = InterpretationService.getInstance();
        List<ReadingCard> out = new ArrayList<>();
        for (int house = 1; house <= 12; house++) {
            List<String> lines = new ArrayList<>();
            List<String> why = new ArrayList<>();
            for (int i = 0; i < f.bodies.length; i++) {
                ChartFrame.Body b = f.bodies[i];
                Bodies.Def def = Bodies.at(i);
                if (b == null || !b.ok || def.isAngle() || !shown(i)
                        || Zodiac.houseOf(b.lon, f.cusps) != house) {
                    continue;
                }
                PlainSnapshot.Line line = PlainSnapshot.plain(svc.getPlanetInHouse(def.name, house));
                lines.add(line == null ? def.name + " is here."
                    : line.whole ? PlacementText.capitalise(line.text) + "."
                    : def.name + ": " + line.text + ".");
                why.add(def.name + " in " + PlacementText.capitalise(Zodiac.signName(b.lon)) + ", "
                    + Zodiac.ordinal(house) + " house");
            }
            if (!lines.isEmpty()) {
                out.add(new ReadingCard(lines.size() > 2 ? "A BUSY PART OF YOUR LIFE" : null,
                    PlacementText.capitalise(LifeAreas.of(house)), String.join(" ", lines), why));
            }
        }
        return out;
    }

    private List<String> quietAreas(ChartFrame f) {
        boolean[] busy = new boolean[13];
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b != null && b.ok && !Bodies.at(i).isAngle() && shown(i)) {
                int h = Zodiac.houseOf(b.lon, f.cusps);
                if (h >= 1 && h <= 12) {
                    busy[h] = true;
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (int h = 1; h <= 12; h++) {
            if (!busy[h]) {
                out.add(LifeAreas.of(h));
            }
        }
        return out;
    }

    /** Whether the wheel has this body switched on. */
    private boolean shown(int index) {
        return skymap == null || skymap.natalRing == null || skymap.natalRing.shows(index);
    }



    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
