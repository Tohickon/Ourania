package com.zodiacomputing.ourania.gui;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

/**
 * The Index, as a tab on the right rail (G11).
 *
 * <p><b>The index already existed and had no door.</b> Nine sections - every one of the 36 registered
 * points grouped as the registry groups them, the signs, the houses, every aspect the engine can find,
 * the dignities and their scoring, 36 decans, 360 Sabian symbols, the tarot attributions and the 28
 * lunar mansions - and the only way to reach it was a link inside a reading a reader was already
 * looking at. That is the exact shape {@code ChartTables}' header describes as this project's
 * signature defect, and the index itself was the last thing still in it. David, 27 Sep: "so create a
 * glossary tab on the right".
 *
 * <p><b>It renders the interpretation panel's own index, not a copy.</b> {@code showIndex} was split
 * into {@link InterpretationPanel#indexHtml} so the string has one author; writing a second index page
 * for this tab is the defect this project has paid for most, and {@code IndexPageCheck} asserts the
 * two surfaces produce the same string for every category.
 *
 * <p><b>Section links move within this tab; reading links go where readings live.</b> Clicking
 * <i>Bodies</i> opens the bodies section here on the right. Clicking <i>Mars</i> opens Mars's reading
 * on the Interpretation page on the left, because that is where every other reading in the app
 * appears - rendering it here as well would make the right rail a second reading surface, which is
 * the same mistake one level up.
 */
final class IndexPanel extends JPanel {

    private final OuraniaWindow window;
    private final InterpretationPanel source;
    private final JEditorPane pane;

    /** Where the reader is, so Back has somewhere to go that is not the chart. */
    private String category = "";

    IndexPanel(OuraniaWindow window, InterpretationPanel source) {
        super(new java.awt.BorderLayout());
        this.window = window;
        this.source = source;
        // Taken from HtmlPanes rather than dressed here: that is where the app's one stylesheet is
        // installed, and H4a was three screens black on near-black for exactly this reason.
        this.pane = HtmlPanes.darkPane();
        this.pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                follow(e.getDescription());
            }
        });
        setBackground(Theme.SURFACE);
        add(HtmlPanes.scroller(this.pane), java.awt.BorderLayout.CENTER);
        show("");
    }

    /** Shows a section, or the list of sections when the category is empty. */
    void show(String category) {
        this.category = category == null ? "" : category;
        this.pane.setText(this.source.indexHtml(this.category));
        this.pane.setCaretPosition(0);
    }

    /** Which section is showing, for the check and for Back. */
    String category() {
        return this.category;
    }

    /**
     * A link inside the index.
     *
     * <b>Three cases, and the third is the one worth being careful about.</b> A section link stays
     * here. Back goes up a level within the index rather than out of it, because a reader who opened
     * the Index tab asked for the index and not for the chart. Anything else is a reading, and
     * readings are the window's business - handled by the same
     * {@code handlePlacementClick} every other surface calls, so a body opened from here is the same
     * body opened from anywhere.
     */
    void follow(String href) {
        if (href == null) {
            return;
        }
        if (href.equals("index")) {
            show("");
            return;
        }
        if (href.startsWith("index|")) {
            show(href.substring("index|".length()));
            return;
        }
        if (href.equals("back")) {
            show(this.category.isEmpty() ? "" : "");
            return;
        }
        if (this.window != null) {
            this.window.handlePlacementClick(href);
        }
    }

    /** The pane, for the check. */
    JEditorPane pane() {
        return this.pane;
    }
}
