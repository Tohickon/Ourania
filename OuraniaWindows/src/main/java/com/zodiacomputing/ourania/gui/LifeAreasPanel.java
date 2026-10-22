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
    private final OuraniaWindow window;
    /** The tab's voice, set each time the page is built. */
    private ReadingVoice voice = ReadingVoice.PERSON;

    LifeAreasPanel(SkymapPanel skymap, OuraniaWindow window) {
        super(new java.awt.BorderLayout());
        this.skymap = skymap;
        this.window = window;
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
        // One page on every tab (2026-10-22): the chart the tab reads, in its voice - the
        // person's own houses, the place's collective life, or the sky in the person's houses.
        this.voice = ReadingVoice.of(skymap, window);
        ChartFrame f = this.voice == ReadingVoice.PERSON
            ? (skymap == null ? null : skymap.getCurrentChart()) : this.voice.frame(skymap);
        StringBuilder sb = new StringBuilder("<html><body>");
        if (f == null || f.cusps == null) {
            return sb.append("<p>No chart is cast yet. Set a date and place, and this page will "
                + "say which parts of life the chart puts its weight on.</p></body></html>")
                .toString();
        }
        sb.append("<h2>Life areas</h2>").append(voice == ReadingVoice.TRANSIT
            ? SkyOnChart.sectionHeading(SkyOnChart.heading(skymap.whoseLabel()))
            : WhoseChart.html(skymap));
        for (Group g : GROUPS) {
            List<ReadingCard> in = new ArrayList<>();
            for (int house : g.houses()) {
                ReadingCard c = card(f, house);
                if (c != null) {
                    in.add(c);
                }
            }
            if (in.isEmpty()) {
                continue;
            }
            sb.append("<h3 style='color:#7FB3FF; margin-top:14px; margin-bottom:6px;'>")
              .append(esc(voice == ReadingVoice.SKY ? g.skyTitle() : g.title())).append("</h3>");
            sb.append(ReadingCard.html(in, Settings.astrologerView()));
        }
        List<String> quiet = quietAreas(f);
        if (!quiet.isEmpty()) {
            sb.append("<p style='color:#9AA5B1; font-size:12px;'><b>Quieter parts of life:</b> ")
              .append(esc(String.join("; ", quiet)))
              .append(voice == ReadingVoice.PERSON
                  ? ". Nothing in the chart sits there, which usually means these run on "
                      + "habit rather than drawing your attention.</p>"
                  : ". No planet in the sky is passing through these now.</p>");
        }
        return sb.append("</body></html>").toString();
    }

    /** One heading of the page and the parts of life under it. */
    record Group(String title, String skyTitle, int[] houses) { }

    /**
     * The four headings of the tab spec (2026-10-20), each over the houses it is about.
     *
     * <b>Twelve cards kept, four headings over them.</b> The spec groups life areas into
     * career, love, money and home, and the inner life; a reader who wants one of those finds it
     * by its heading, and nothing the twelve cards say is merged away. Each house is under one
     * heading only: the sixth (daily work and health) is under work, and the eighth (shared
     * money and intimacy) under love, which is where the spec puts intimacy.
     */
    static final List<Group> GROUPS = List.of(
        new Group("Career and work", "Leaders and workers", new int[] {10, 6}),
        new Group("Love and the people close to you", "Relations, media and public life",
            new int[] {7, 5, 8, 3, 11}),
        new Group("Money and home", "The economy and the land", new int[] {2, 4}),
        new Group("Body, mind and soul", "The public mood, beliefs and hidden matters",
            new int[] {1, 9, 12}));

    /** One card per occupied house, in house order. Package-visible for a check. */
    List<ReadingCard> cards(ChartFrame f) {
        List<ReadingCard> out = new ArrayList<>();
        for (int house = 1; house <= 12; house++) {
            ReadingCard c = card(f, house);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    /** The card for one house, or null when nothing the wheel shows sits there. */
    private ReadingCard card(ChartFrame f, int house) {
        InterpretationService svc = InterpretationService.getInstance();
        List<String> lines = new ArrayList<>();
        List<String> why = new ArrayList<>();
        for (int i = 0; i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            Bodies.Def def = Bodies.at(i);
            if (b == null || !b.ok || def.isAngle() || !shown(i)
                    || Zodiac.houseOf(b.lon, f.cusps) != house) {
                continue;
            }
            // The line each body brings: its house reading for a person; for the sky, its
            // weather; on Transits, what the sky's planet does passing through that house.
            String said = voice == ReadingVoice.SKY
                ? SkyWeather.weatherFor(f, def.name)
                : null;
            if (said != null) {
                lines.add(said);
            } else {
                // On Transits each planet brings its own line: the house text for a transit is
                // the house's, the same for every planet, and three planets in one house read
                // as one sentence three times.
                String acts = voice == ReadingVoice.TRANSIT
                    ? svc.getTransitCardPart("acts." + def.id) : null;
                if (acts != null) {
                    lines.add(acts);
                    why.add(def.name + " in " + PlacementText.capitalise(Zodiac.signName(b.lon))
                        + ", in your " + Zodiac.ordinal(house) + " house");
                    continue;
                }
                // A point with no line of its own in this voice is named, not given another
                // voice's text: the sky's Ceres read "you actively seek...", and every Lot on
                // Transits read the house's own "Transit through the 10th House".
                if (voice != ReadingVoice.PERSON) {
                    lines.add(def.name + (voice == ReadingVoice.SKY ? " is here."
                        : " is passing through here."));
                    why.add(def.name + " in " + PlacementText.capitalise(Zodiac.signName(b.lon))
                        + ", " + Zodiac.ordinal(house) + " house");
                    continue;
                }
                PlainSnapshot.Line line = PlainSnapshot.plain(svc.getPlanetInHouse(def.name,
                    house));
                lines.add(line == null ? def.name + " is here."
                    : line.whole ? PlacementText.capitalise(line.text) + "."
                    : def.name + ": " + line.text + ".");
            }
            why.add(def.name + " in " + PlacementText.capitalise(Zodiac.signName(b.lon)) + ", "
                + Zodiac.ordinal(house) + " house");
        }
        if (lines.isEmpty()) {
            return null;
        }
        String area = voice.area(house);
        return new ReadingCard(lines.size() > 2 ? (voice == ReadingVoice.SKY
                ? "A BUSY PART OF PUBLIC LIFE" : "A BUSY PART OF YOUR LIFE") : null,
            PlacementText.capitalise(area == null ? LifeAreas.of(house) : area),
            String.join(" ", lines), why);
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
                String area = voice.area(h);
                out.add(area == null ? LifeAreas.of(h) : area);
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
