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
 * The menu: what the app can DO, behind the Menu tab of the right-hand rail.
 *
 * <p><b>Two sidebars became one.</b> Navigation and the saved-chart directory were on the left,
 * the chart's own data on the right, and the chart sat between them with a handle down each
 * edge. That is two things to learn where nothing needed to be in two places: the wheel is the
 * page, and everything else is a panel you pull out beside it.
 *
 * <p>What is left here is the six screens as a plain column, then <b>Tables</b> and
 * <b>Export</b> as accordion sections. Everything about the chart itself - its placements,
 * the transits, the selection, the readings - went to the left rail, and the aspect grid is
 * the other tab of the rail this sits in. Sections expand and retract independently; see
 * {@link Accordion} for why not one-at-a-time.
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

    static final String EXPORT = "Export";
    static final String TABLES = "Tables";

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
        {"Transit Search", "TRANSIT_SEARCH",
            "When a transiting planet aspects a point in Chart A, over a span of years"},
        {"Dial", "DIAL",
            "The 90, 45 and 22.5 degree dials: hard aspects and midpoint pictures under one pointer"},
        {"Settings", "SETTINGS",
            "Which points the chart shows, house system defaults and the rest"},
    };

    /**
     * How wide the rail opens this panel.
     *
     * <b>Narrower than it was.</b> 310px was sized for the placements and the aspect grid,
     * which have both moved off this panel; what is left is a column of short rows, and the
     * panel was carrying half a panel of empty space at the chart's expense every time it
     * opened.
     */
    static final int MENU_WIDTH = 196;

    private final OuraniaWindow window;
    private final Accordion accordion;
    private JPanel screens;

    public SidePanel(OuraniaWindow window) {
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        accordion = new Accordion();
        accordion.setBorder(Theme.pad(Theme.GAP, Theme.GAP, Theme.GAP, Theme.GAP));

        // <b>The Menu section is gone and its six screens sit at the drawer's own level.</b>
        // Reaching Settings used to be: open the drawer, open Menu, click Settings - three
        // acts for one destination, with the middle one existing only to hold the other two.
        // A wrapper whose whole content is a list of links is a level of nesting that earns
        // nothing, and this app has removed several of those already.
        screens = column();
        for (String[] screen : SCREENS) {
            screens.add(screenButton(screen[0], screen[1], screen[2]));
        }


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
        tables.add(tableButton("Declinations", "DECLINATIONS",
            "North and south of the equator: planets out of bounds, parallels and contraparallels"));
        tables.add(tableButton("Antiscia", "ANTISCIA",
            "Each point mirrored across the solstice and equinox axes, and who stands on the mirrors"));
        tables.add(tableButton("Fixed Stars", "FIXED_STARS",
            "The Behenian, royal and bright stars, and which chart points stand on them"));
        tables.add(tableButton("Progressed", "PROGRESSED",
            "The secondary progressed chart: a day of ephemeris for a year of life"));
        tables.add(tableButton("Solar Arc", "SOLARARC",
            "Every point moved forward by the arc the progressed Sun has travelled"));
        tables.add(tableButton("Draconic", "DRACONIC",
            "The same sky measured from the Moon's node, beside the tropical chart"));
        tables.add(tableButton("Returns", "RETURNS",
            "The year's returns: solar and lunar as charts, the annual three as triggers"));
        accordion.addSection(TABLES, tables);

        // Section B. Until 3 Sep 2026 nothing could leave this application at all - no image,
        // no print, no clipboard, no file - which caps what it is for however good the engine
        // behind it is.
        JPanel export = column();
        export.add(exportButton("Save Chart Image", "SAVE_IMAGE",
            "The wheel as a PNG, at screen, 2x or 4x resolution"));
        export.add(exportButton("Print Chart", "PRINT",
            "Print the wheel, fitted to the page"));
        export.add(exportButton("Copy Chart", "COPY_IMAGE",
            "The wheel to the clipboard as an image"));
        export.add(exportButton("Copy Positions", "COPY_POSITIONS",
            "Every drawn position to the clipboard, tab separated for a spreadsheet"));
        export.add(exportButton("Save Positions", "SAVE_POSITIONS",
            "The same positions as a .tsv file"));
        export.add(exportButton("Save Aspect Grid", "SAVE_GRID",
            "The aspect grid as an HTML table"));
        export.add(exportButton("Save Reading", "SAVE_READING",
            "The reading on screen, as an HTML document"));
        export.add(exportButton("Save Reading as Text", "SAVE_READING_TEXT",
            "The reading on screen, as plain text"));
        export.add(exportButton("Copy Reading", "COPY_READING",
            "The reading on screen to the clipboard, as plain text"));
        accordion.addSection(EXPORT, export);

        JLabel heading = new JLabel("Ourania+", SwingConstants.CENTER);
        heading.setForeground(Theme.TEXT);
        heading.setFont(Theme.TITLE);
        heading.setBorder(Theme.pad(Theme.GAP_L, 0, Theme.GAP, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BG);
        // The six destinations first, then the sections that hold content. Where you can go
        // is a shorter list than what you can read, and it does not change with the chart.
        JPanel stack = new JPanel(new BorderLayout());
        stack.setBackground(Theme.BG);
        stack.add(screens, BorderLayout.NORTH);
        stack.add(accordion, BorderLayout.CENTER);
        // NORTH, not CENTER: the accordion's height is the sum of whatever is open, and given
        // CENTER it would be stretched to the drawer's full height with the closed sections
        // drifting apart down the column.
        top.add(stack, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Widgets.styleScrollPane(scroll);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Theme.BG);
        inner.add(heading, BorderLayout.NORTH);
        inner.add(scroll, BorderLayout.CENTER);

        // <b>This is a page in the right-hand rail, not a drawer of its own.</b> Wrapping
        // itself in one was right while it was the only thing on this edge. The aspect grid
        // is on that edge now, and two drawers side by side put one strip in front of the
        // other rather than beside it - so the rail owns the sliding, the handle and the
        // opening-on-launch, and this class is only what sits behind the Menu tab.
        add(inner, BorderLayout.CENTER);
    }

    /**
     * Refreshes what this panel still owns when the chart changes.
     *
     * The natal, transit and grid panes moved to the left rail and the grid tab, so this no
     * longer fills them - it keeps the saved-chart list current and re-measures the open
     * sections.
     */
    public void updateChartSections() {
        // Re-measures whatever is open, so new content is not clipped at the old height: a
        // chart change can turn three placements into thirty.
        for (Accordion.Section section : accordion.sections()) {
            if (section.isOpen()) {
                section.setOpen(true);
            }
        }
    }

    /** The screens column, for a check that needs to walk the destinations. */
    JPanel screensColumn() {
        return screens;
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

    private JButton exportButton(String label, final String what, String tip) {
        JButton b = row(label, tip, Widgets.Role.TRANSPORT);
        b.addActionListener(e -> {
            if (window != null) {
                window.runExport(what);
            }
        });
        return b;
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
