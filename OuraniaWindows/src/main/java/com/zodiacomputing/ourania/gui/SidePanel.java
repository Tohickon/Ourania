package com.zodiacomputing.ourania.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * The side panel: one drawer, on the right, holding everything.
 *
 * <p><b>Two sidebars became one.</b> Navigation and the saved-chart directory were on the left,
 * the chart's own data on the right, and the chart sat between them with a handle down each
 * edge. That is two things to learn where nothing needed to be in two places: the wheel is the
 * page, and everything else is a panel you pull out beside it.
 *
 * <p>Sections, top to bottom: <b>Menu, Saved Charts, Natal Chart, Transits, Aspect Grids,
 * Readings</b>. They expand and retract independently - see {@link Accordion} for why not
 * one-at-a-time.
 *
 * <h2>What was removed on the way, and why</h2>
 *
 * <b>Share Skymap, Sync, Connect and Help are gone from the menu.</b> All four were
 * {@code createPlaceholder(...)} cards reading "(Under Construction)" - a menu that offers ten
 * destinations and delivers six teaches a reader that this app's menu cannot be trusted, which
 * is a worse cost than the missing features. They come back when there is something behind
 * them.
 *
 * <b>A "Tools" section is gone too.</b> It held Interpretation Panel, Zodiacal Releasing and
 * Chart Data - all three of which are rows in Menu. With one drawer instead of two there is
 * nowhere for that duplication to hide: the same three screens were reachable twice from one
 * column, which is how a reader ends up believing the two routes do different things.
 */
public final class SidePanel extends JPanel {

    static final String SELECTION = "Selection";
    static final String MENU = "Menu";
    static final String PROFILES = "Saved Charts";
    static final String NATAL = "Natal Chart";
    static final String TRANSITS = "Transits";
    static final String GRIDS = "Aspect Grids";
    static final String TABLES = "Tables";
    static final String READINGS = "Readings";

    /**
     * The screens the menu offers, and every one of them is real.
     *
     * Paired label to card name so the two cannot drift, and so a check can read the list
     * rather than infer it from the buttons.
     */
    static final String[][] SCREENS = {
        {"Chart Setup", "SEARCH",
            "Enter birth data and choose what to draw - natal, transits, synastry or a composite"},
        {"Grid Skymap", "SKYMAP", "The chart wheel"},
        {"Chart Data", "NAME_LIST",
            "The active chart as tables: longitudes, house cusps and the aspect list"},
        {"Interpretation", "INTERPRETATION", "Show or hide the reading panel"},
        {"Zodiacal Releasing", "RELEASING", "Periods of Fortune and Spirit for this chart"},
        {"Settings", "SETTINGS",
            "Which points the chart shows, house system defaults and the rest"},
    };

    private final OuraniaWindow window;
    private final Accordion accordion;
    private final Drawer drawer;
    private final ProfileListPanel profiles;
    private final JEditorPane selectionPane;
    private final JEditorPane natalPane;
    private final JEditorPane transitPane;
    private final JEditorPane gridPane;

    public SidePanel(OuraniaWindow window) {
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        accordion = new Accordion();
        accordion.setBorder(Theme.pad(Theme.GAP, Theme.GAP, Theme.GAP, Theme.GAP));

        // <b>First in the list, because it is the only section about what you just did.</b>
        // Everything below it describes the whole chart; this describes the one body under the
        // cursor when you clicked, and it is filled on demand rather than on every chart update.
        selectionPane = HtmlPanes.chartPane(window);
        Accordion.Section selectionSection = accordion.addSection(SELECTION, selectionPane);
        selectionSection.setMaxOpenHeight(360);

        JPanel screens = column();
        for (String[] screen : SCREENS) {
            screens.add(screenButton(screen[0], screen[1], screen[2]));
        }
        accordion.addSection(MENU, screens);

        profiles = new ProfileListPanel(window);
        accordion.addSection(PROFILES, profiles);

        natalPane = HtmlPanes.chartPane(window);
        accordion.addSection(NATAL, natalPane);
        transitPane = HtmlPanes.chartPane(window);
        accordion.addSection(TRANSITS, transitPane);
        gridPane = HtmlPanes.chartPane(window);
        Accordion.Section gridSection = accordion.addSection(GRIDS, gridPane);
        // The grid is the tallest thing this drawer holds and the one a reader scans rather
        // than reads, so it gets more room before it starts scrolling.
        gridSection.setMaxOpenHeight(520);

        // Three engines that were computed, checked and unreachable until 2026-09-02. A
        // capability with no door is invisible in exactly the way a missing one is not: the
        // suites stay green, so nothing reports it. See ChartTables.
        JPanel tables = column();
        tables.add(tableButton("Midpoints", "MIDPOINTS",
            "Bodies sitting on the midpoints of the Sun, Moon, Ascendant and Midheaven"));
        tables.add(tableButton("Dispositors", "DISPOSITORS",
            "Which planet rules each house, where it sits, and any receptions between them"));
        tables.add(tableButton("Resonance", "RESONANCE",
            "The tightest harmonic contacts the classical aspects do not show"));
        accordion.addSection(TABLES, tables);

        JPanel readings = column();
        readings.add(readingButton("Snapshot", "SNAPSHOT",
            "Read the natal chart as it currently stands"));
        readings.add(readingButton("Report", "REPORT",
            "The full reading: shape, weights, repeated themes, tensions"));
        readings.add(readingButton("Synthesize", "SYNTHESIZE",
            "Generate a narrative reading based on the chart"));
        readings.add(readingButton("Predict", "TIMELINE",
            "Generate a timeline prediction based on the chart"));
        readings.add(readingButton("Calendar", "CALENDAR",
            "Ingresses, stations, lunations and mundane aspects for the year"));
        accordion.addSection(READINGS, readings);

        JLabel heading = new JLabel("Ourania+", SwingConstants.CENTER);
        heading.setForeground(Theme.TEXT);
        heading.setFont(Theme.TITLE);
        heading.setBorder(Theme.pad(Theme.GAP_L, 0, Theme.GAP, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BG);
        // NORTH, not CENTER: the accordion's height is the sum of whatever is open, and given
        // CENTER it would be stretched to the drawer's full height with the closed sections
        // drifting apart down the column.
        top.add(accordion, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Widgets.styleScrollPane(scroll);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Theme.BG);
        inner.add(heading, BorderLayout.NORTH);
        inner.add(scroll, BorderLayout.CENTER);

        drawer = new Drawer(Drawer.Side.RIGHT, "Menu", inner, 310);
        // Open on launch, and without animating: an app that slides its own panel out on every
        // start is animating at the one moment nobody asked it to.
        drawer.openImmediately();
        Accordion.Section natalSection = accordion.section(NATAL);
        if (natalSection != null) {
            natalSection.setOpen(true);
        }
        add(drawer, BorderLayout.CENTER);
    }

    /**
     * Shows one body's detail, opening the drawer and the section to do it.
     *
     * <b>Opens what it needs rather than assuming.</b> A click that filled a section inside a
     * shut drawer would do nothing visible at all, and the reader would conclude clicking
     * bodies is not a thing this app does.
     */
    public void showSelection(String html) {
        HtmlPanes.setHtml(selectionPane, html);
        Accordion.Section section = accordion.section(SELECTION);
        if (section != null) {
            section.setOpen(true);
        }
        if (!drawer.isOpen()) {
            drawer.setOpen(true);
        }
    }

    /** Fills the three chart sections, each from its own part of the wheel's HTML. */
    public void updateChartSections(String natalHtml, String transitHtml, String gridHtml) {
        HtmlPanes.setHtml(natalPane, natalHtml);
        HtmlPanes.setHtml(transitPane, transitHtml);
        HtmlPanes.setHtml(gridPane, gridHtml);
        // A chart saved from Chart Setup lands in the store the directory reads, and
        // generating is what follows a save - so this is when a new name should appear.
        if (profiles != null) {
            profiles.rebuild();
        }
        // Re-measures whatever is open, so new content is not clipped at the old height: a
        // chart change can turn three placements into thirty.
        for (Accordion.Section section : accordion.sections()) {
            if (section.isOpen()) {
                section.setOpen(true);
            }
        }
    }

    /** The accordion, for a check that needs to walk the sections. */
    Accordion accordion() {
        return accordion;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.SURFACE);
        p.setBorder(Theme.pad(Theme.GAP_S, Theme.GAP_S, Theme.GAP_S, Theme.GAP_S));
        return p;
    }

    private JButton tableButton(String label, final String kind, String tip) {
        JButton b = row(label, tip, Widgets.Role.READING);
        b.addActionListener(e -> {
            if (window != null) {
                window.showTable(kind);
            }
        });
        return b;
    }

    private JButton readingButton(String label, final String reading, String tip) {
        JButton b = row(label, tip, Widgets.Role.READING);
        b.addActionListener(e -> {
            if (window != null) {
                window.runReading(reading);
            }
        });
        return b;
    }

    private JButton screenButton(String label, final String screenName, String tip) {
        JButton b = row(label, tip, Widgets.Role.TRANSPORT);
        b.addActionListener(e -> {
            if (window != null) {
                window.switchScreen(screenName);
            }
        });
        return b;
    }

    /** The shared shape of every row: full width, left aligned, one style. */
    private static JButton row(String label, String tip, Widgets.Role role) {
        JButton button = new JButton(label);
        button.setToolTipText(tip);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        Widgets.styleButton(button, role);
        return button;
    }
}
