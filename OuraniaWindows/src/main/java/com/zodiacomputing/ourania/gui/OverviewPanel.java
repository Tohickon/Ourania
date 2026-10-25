package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Sect;
import com.zodiacomputing.ourania.astro.TransferOfLight;
import com.zodiacomputing.ourania.astro.Zodiac;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Overview page: what this chart is, in one screen, before any of the detail.
 *
 * <p><b>Every card here is an engine that already ran and had nowhere to appear.</b> Sect, the
 * chart's shape, the prominence ranking and the transfers of light are all computed for the
 * Synthesize reading, which is a long document a reader opens deliberately. A reader who wants
 * to know what kind of chart this is had to read a report to find out. That is the shape this
 * project keeps finding - the thing exists and has no door - and it is the fourth time this
 * month.
 *
 * <p><b>Each card says one plain sentence, and keeps the degrees behind a tap.</b> The
 * collapsed card is for somebody who wants to know what is going on; expanding it gives the
 * classical name, the bodies, their degrees and how far from exact each aspect is. Both come
 * from the same finding, so the plain sentence cannot say something the degrees contradict.
 *
 * <p><b>The engines are called, not copied.</b> {@code Gestalt.compute}, {@code BodyScore.rank}
 * and {@code TransferOfLight.find} are the same entry points {@code SkymapPanel}'s reading
 * worker uses. Recomputing any of this here differently is the defect that has cost this
 * project the most, and a summary that disagreed with the full reading would be the worst
 * version of it - two pages about one chart, both confident.
 *
 * <p><b>What is deliberately not here:</b> prohibition of light and refranation. The engine
 * computes translation and collection only, so cards for those two would be captions with
 * nothing behind them.
 */
final class OverviewPanel extends JPanel {

    /** How many transfers a card lists before it stops; the rest are counted, not printed. */
    private static final int SHOWN = 4;

    private final SkymapPanel skymap;
    private final OuraniaWindow window;
    /** The tab's voice, set each time the cards are built. */
    private ReadingVoice voice = ReadingVoice.PERSON;
    private final JEditorPane pane;

    /** Which cards the reader has opened. Empty is the page's resting state. */
    private final Set<String> expanded = new LinkedHashSet<>();

    /** One card, built once so the collapsed and expanded views cannot disagree. */
    static final class Card {
        final String id;
        final String icon;
        final String title;
        final String subtitle;
        final String plain;
        final List<String> detail = new ArrayList<>();

        Card(String id, String icon, String title, String subtitle, String plain) {
            this.id = id;
            this.icon = icon;
            this.title = title;
            this.subtitle = subtitle;
            this.plain = plain;
        }
    }

    OverviewPanel(SkymapPanel skymap, OuraniaWindow window) {
        super(new java.awt.BorderLayout());
        this.skymap = skymap;
        this.window = window;
        // From HtmlPanes, where the app's one stylesheet is installed - H4a was three screens
        // of black on near-black for exactly the reason a panel should not dress itself.
        this.pane = HtmlPanes.darkPane();
        this.pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                follow(e.getDescription());
            }
        });
        setBackground(Theme.SURFACE);
        add(HtmlPanes.scroller(this.pane), java.awt.BorderLayout.CENTER);
        refresh();
    }

    /**
     * Rebuild the page from the chart as it stands now.
     *
     * <b>Called when the tab is opened, not when the chart changes.</b> A card is a statement
     * about a moment, and the transport buttons move the moment; a page built once would go on
     * describing a chart that has left the screen. The rail's own comment makes the same point
     * the other way round: opening a tab has to produce the page, not just reveal it.
     */
    void refresh() {
        this.pane.setText(html());
        this.pane.setCaretPosition(0);
    }

    /** A link on this page. Cards toggle here; anything else is the app's ordinary click. */
    void follow(String href) {
        if (href == null) {
            return;
        }
        if (href.startsWith("toggle:")) {
            String id = href.substring("toggle:".length());
            if (!expanded.remove(id)) {
                expanded.add(id);
            }
            refresh();
            return;
        }
        if (this.window != null) {
            this.window.handlePlacementClick(href);
        }
    }

    /** Whether a card is open, for the check and for the rendering. */
    boolean isExpanded(String id) {
        return expanded.contains(id);
    }

    // ------------------------------------------------------------------ the cards

    /**
     * Every card this chart has to show, in the order a reader meets them.
     *
     * Sect and shape first because they frame everything after; the ranking next, because it
     * says which body to read first; the light mechanics last, because they are the finest
     * grained and the least likely to be what somebody opened this page for.
     */
    List<Card> cards() {
        List<Card> cards = new ArrayList<>();
        // One page on every tab (2026-10-22): the chart the tab reads, in its voice.
        this.voice = ReadingVoice.of(this.skymap, this.window);
        ChartFrame f = this.voice.frame(this.skymap);
        if (f == null) {
            return cards;
        }
        Gestalt.Result g = Gestalt.compute(f);
        List<BodyScore.Vector> ranked = BodyScore.rank(f, g);

        cards.add(sectCard(g));
        Card shape = shapeCard(f, g);
        if (shape != null) {
            cards.add(shape);
        }
        Card prominence = prominenceCard(f, ranked);
        if (prominence != null) {
            cards.add(prominence);
        }

        List<TransferOfLight.Transfer> transfers =
            TransferOfLight.find(f, Aspects.betweenBodies(f));
        Card translations = lightCard(f, transfers, TransferOfLight.Kind.TRANSLATION);
        if (translations != null) {
            cards.add(translations);
        }
        Card collections = lightCard(f, transfers, TransferOfLight.Kind.COLLECTION);
        if (collections != null) {
            cards.add(collections);
        }
        return cards;
    }

    /**
     * The plain sentence for day or night, in the voice the page speaks. One home for it: the
     * Chart shape card and the At a glance page (GlanceBlocks) both say it, and two copies of
     * the wording would drift.
     */
    static String sectSentence(Gestalt.Result g, ReadingVoice voice) {
        String benefic = Sect.beneficOfSect(g.diurnal);
        return (voice == ReadingVoice.PERSON
            ? "Because you were born " + (g.diurnal ? "in daylight" : "at night")
            : voice == ReadingVoice.SKY
                ? "Because it is " + (g.diurnal ? "daytime" : "night-time") + " here"
                : "Because the Sun is " + (g.diurnal ? "above" : "below")
                    + " your horizon now")
            + ", " + g.sectLight + " and " + benefic + " work with the fewest obstacles in "
            + voice.chart() + ", while " + g.outOfSectMalefic
            + " is the one that needs handling.";
    }

    private Card sectCard(Gestalt.Result g) {
        String benefic = Sect.beneficOfSect(g.diurnal);
        Card c = new Card("sect", g.diurnal ? "☀" : "☽",
            "Who Has the Easy Run",
            voice == ReadingVoice.PERSON ? subtitleOfSect(g.diurnal)
                : (g.diurnal ? "Daytime sky" : "Night sky"),
            sectSentence(g, voice));
        c.detail.add("Classical figure: " + (g.diurnal ? "diurnal" : "nocturnal") + " sect");
        c.detail.add("Light of the sect: " + g.sectLight);
        c.detail.add("Benefic of sect: " + benefic);
        c.detail.add("Malefic contrary to sect: " + g.outOfSectMalefic);
        return c;
    }

    private Card shapeCard(ChartFrame f, Gestalt.Result g) {
        if (g.shape == null) {
            return null;
        }
        String handle = g.shapeHandle == null || g.shapeHandle.isEmpty() ? "" : g.shapeHandle;
        String where = "";
        if (!handle.isEmpty()) {
            ChartFrame.Body b = f.body(handle);
            if (b != null) {
                where = " in " + Zodiac.signName(b.lon);
            }
        }
        String name = g.shape.name().charAt(0) + g.shape.name().substring(1).toLowerCase();
        Card c = new Card("shape", "◔", voice == ReadingVoice.PERSON
                ? "How Your Chart Is Laid Out" : "How the Sky Is Laid Out",
            plainShape(g.shape) + (handle.isEmpty() ? "" : ", led by " + handle + where),
            (voice == ReadingVoice.PERSON ? "Your planets are " : "The sky's planets are ")
                + plainShape(g.shape).toLowerCase()
                + (handle.isEmpty() ? "." : ", with " + handle + " out in front - the placement "
                    + "that pulls the rest along and the one to read first."));
        c.detail.add("Classical figure: " + name + " pattern");
        if (!handle.isEmpty()) {
            c.detail.add("Leading body: " + handle + where);
        }
        if (!g.dominantElements.isEmpty()) {
            c.detail.add("Dominant element: " + String.join(", ", g.dominantElements));
        }
        if (!g.missingElements.isEmpty()) {
            c.detail.add("Missing element: " + String.join(", ", g.missingElements));
        }
        return c;
    }

    private Card prominenceCard(ChartFrame f, List<BodyScore.Vector> ranked) {
        if (ranked == null || ranked.isEmpty()) {
            return null;
        }
        BodyScore.Vector top = ranked.get(0);
        Card c = new Card("prominence", "📢", "The Loudest Voice",
            top.body,
            top.body + " holds the microphone here. It is the placement that speaks over the "
                + "others, and the one whose themes keep turning up whatever the chart is "
                + "asked about.");
        c.detail.add("Relative amplitude: " + String.format("%.1f%%", top.prominence * 100));
        for (int i = 0; i < Math.min(SHOWN, ranked.size()); i++) {
            BodyScore.Vector v = ranked.get(i);
            ChartFrame.Body b = f.body(v.body);
            c.detail.add(String.format("%d. %s %s - %.1f%%", i + 1, v.body,
                b == null ? "" : at(b.lon), v.prominence * 100));
        }
        return c;
    }

    /**
     * The messenger and the mediator, each as one card rather than one card per finding.
     *
     * A chart can carry thirty translations. Thirty cards is a list, not an overview, so the
     * card states how many there are and shows the first few; the reading is where every one
     * of them is written out.
     */
    private Card lightCard(ChartFrame f, List<TransferOfLight.Transfer> all,
                           TransferOfLight.Kind kind) {
        List<TransferOfLight.Transfer> mine = new ArrayList<>();
        for (TransferOfLight.Transfer t : all) {
            if (t.kind == kind) {
                mine.add(t);
            }
        }
        if (mine.isEmpty()) {
            return null;
        }
        boolean translation = kind == TransferOfLight.Kind.TRANSLATION;
        TransferOfLight.Transfer first = mine.get(0);
        Card c = new Card(translation ? "translation" : "collection",
            translation ? "🏃" : "🧲",
            translation ? "Go-Betweens" : "Shared Meeting Points",
            countOf(mine.size(), kind),
            translation
                ? "One planet links two others that would not otherwise meet, like a relay "
                    + "runner passing a baton: " + first.mediator + " has just left "
                    + first.from + " and is on its way to " + first.to + "."
                : "Two parts of " + voice.chart() + " that never meet directly both reach the"
                    + " same third "
                    + "planet, the way two strangers share a friend: "
                    + first.from + " and " + first.to + " both reach " + first.mediator
                    + ".");
        c.detail.add("Classical figure: " + (translation ? "translation of light"
            : "collection of light"));
        if (translation) {
            int strict = 0;
            for (TransferOfLight.Transfer t : mine) {
                if (!t.endsAspectEachOther) {
                    strict++;
                }
            }
            // The strict form is the tradition's: the two ends cannot reach each other, so the
            // light only crosses through the mediator. The engine surfaces both and always has.
            c.detail.add("Strict (the two ends cannot reach each other): " + strict
                + " of " + mine.size());
        }
        for (int i = 0; i < Math.min(SHOWN, mine.size()); i++) {
            TransferOfLight.Transfer t = mine.get(i);
            c.detail.add(t.sentence() + " — " + leg(f, t.fromHit) + ", " + leg(f, t.toHit));
        }
        if (mine.size() > SHOWN) {
            c.detail.add("and " + (mine.size() - SHOWN) + " more, written out in Synthesize.");
        }
        return c;
    }

    /**
     * The card subtitles in a reader's words (2026-10-13). The classical names - sect, the
     * Jones pattern, translation and collection of light - stay in each card's detail, which
     * is where an astrologer looks for them. Package-visible so OverviewPageCheck asks the
     * same rule rather than a copy of its wording.
     */
    static String subtitleOfSect(boolean diurnal) {
        return diurnal ? "Born in daylight" : "Born at night";
    }

    static String countOf(int n, TransferOfLight.Kind kind) {
        boolean one = n == 1;
        return n + (kind == TransferOfLight.Kind.TRANSLATION
            ? (one ? " link made through a go-between" : " links made through a go-between")
            : (one ? " meeting point" : " meeting points"));
    }

    static String plainShape(Gestalt.Shape shape) {
        switch (shape) {
            case BUNDLE: return "Packed into one part of life";
            case BOWL: return "Gathered on one half, with the other half open";
            case BUCKET: return "Gathered on one side, poured through one point";
            case LOCOMOTIVE: return "Driven by an engine at the front";
            case SEESAW: return "Pulled between two sides";
            case SPLASH: return "Spread across every part of life";
            default: return "Spread out in a few strong clusters";
        }
    }

    /** One aspect of a transfer, with how far from exact it is and which way it is going. */
    private static String leg(ChartFrame f, Aspects.Hit h) {
        if (h == null) {
            return "";
        }
        ChartFrame.Body a = f.body(h.a);
        ChartFrame.Body b = f.body(h.b);
        return h.a + (a == null ? "" : " " + at(a.lon)) + " " + h.type + " "
            + h.b + (b == null ? "" : " " + at(b.lon))
            + String.format(", %.2f° from exact, ", h.offBy)
            + (h.applying ? "applying" : "separating");
    }

    /**
     * A longitude as a reader reads it: 26°05' Aries.
     *
     * Zodiac does the arithmetic. Written out here first, this produced "30°00' cancer" - a
     * degree no sign has - by carrying a rounded 60 minutes upward without moving the sign,
     * which is exactly the fault ChartTables' own copy of these six lines had been carrying.
     */
    private static String at(double lon) {
        Zodiac.Position p = Zodiac.position(lon);
        return String.format("%d°%02d' %s", p.degree, p.minute, p.signTitled());
    }

    // ------------------------------------------------------------------ rendering

    String html() {
        StringBuilder sb = new StringBuilder("<html><body>");
        List<Card> cards = cards();
        if (cards.isEmpty()) {
            sb.append("<p>No chart is cast yet. Set a date and place, and this page will say "
                + "what kind of chart it is.</p></body></html>");
            return sb.toString();
        }
        sb.append("<h2>Chart shape</h2>").append(voice == ReadingVoice.TRANSIT
            ? SkyOnChart.sectionHeading(SkyOnChart.heading(skymap.whoseLabel()))
            : WhoseChart.html(skymap));
        for (Card c : cards) {
            // In Astrologer view every card starts open, and a click closes it instead.
            boolean open = expanded.contains(c.id) != Settings.astrologerView();
            sb.append("<p><b>").append(c.icon).append(' ').append(esc(c.title))
              .append("</b><br>").append(esc(c.subtitle)).append("<br>")
              .append(esc(c.plain)).append("<br>")
              .append("<a href='toggle:").append(c.id).append("'>")
              .append(open ? "Hide the detail" : "Show the detail").append("</a></p>");
            if (open) {
                sb.append("<ul>");
                for (String line : c.detail) {
                    sb.append("<li>").append(esc(line)).append("</li>");
                }
                sb.append("</ul>");
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
