package com.zodiacomputing.ourania.gui;

import javax.swing.*;
import java.awt.*;

public class OuraniaWindow extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private SkymapPanel skymapPanel;
    private InterpretationPanel interpretationPanel;
    private NameListPanel nameListPanel;
    private ReleasingPanel releasingPanel;
    private TransitSearchPanel transitSearchPanel;
    private DialPanel dialPanel;
    private SkyViewPanel skyViewPanel;
    private TransitCalendarPanel transitCalendarPanel;
    /** The month grid on the Calendar tab: the same panel as the screen, in its narrow shape. */
    private TransitCalendarPanel calendarMonth;
    private ReturnsPanel returnsPanel;
    private HoraryPanel horaryPanel;
    private ElectionalPanel electionalPanel;
    private HeliocentricPanel heliocentricPanel;

    private ChartSetupPanel chartSetupPanel;

    // <b>Package-private, for B8 and for the suites.</b> A batch has to load a chart through the
    // setup panel, cast it through the wheel and read the result out of the interpretation pane -
    // the same three it would if a reader were clicking. Exposed by accessor rather than by
    // loosening the fields, so nothing outside this package can reach them.
    SkymapPanel skymap() {
        return this.skymapPanel;
    }

    InterpretationPanel interpretation() {
        return this.interpretationPanel;
    }

    ChartSetupPanel chartSetup() {
        return this.chartSetupPanel;
    }
    private SidePanel sidePanel;
    /** The left rail: what this chart is, opposite the menu that says what the app does. */
    private DrawerRail chartRail;
    /** The pages, one strip shared by every chart tab: it moves into the tab in front. */
    private SubRail pages;
    /** The mode pill and the charts line above the four tabs; see {@link ModeBadge}. */
    private ModeBadge modeBadge;
    /** The badge over the strip. This, not the strip, is what each chart tab holds. */
    private JPanel readingColumn;
    /** Copy Markdown, Export HTML, Print / PDF - under the Full report group only. */
    private ExportBar exportBar;
    /** Each chart tab's body, which holds the strip while that tab is in front. */
    private final java.util.Map<String, JPanel> chartHolders = new java.util.LinkedHashMap<>();
    private LifeAreasPanel lifeAreasPanel;
    private OverviewPanel overviewPanel;
    private javax.swing.JEditorPane natalPane;
    private javax.swing.JEditorPane transitPane;
    private javax.swing.JEditorPane chartBPane;
    /** Where you meet. Kept under its old name; it is the pane the Synastry page always was. */
    private javax.swing.JEditorPane synastryPane;
    private javax.swing.JEditorPane chemistryPane;
    private javax.swing.JEditorPane synastryGlancePane;
    /** Coming up together: the sky over both charts, when there are two (PairTimeline). */
    private javax.swing.JEditorPane pairComingPane;
    /** True while the Transits tab is in front, so the shared pages read the sky on the chart. */
    boolean readingTransits() {
        return chartRail != null && ChartTab.TRANSITS.door.equals(chartRail.selected());
    }

    /** The Transits tab's pages, by key (SKY_PAGES). */
    private final java.util.Map<String, javax.swing.JEditorPane> skyPanes =
        new java.util.LinkedHashMap<>();
    private javax.swing.JEditorPane selectionPane;
    /** Where selectionPane is shown: a drawer under the wheel, since 2026-10-20. */
    private SelectionDrawer selectionDrawer;
    /** Every "Show the astrology" box on a plain page, so ticking one shows in all of them. */
    private final java.util.List<javax.swing.JCheckBox> astrologerChecks = new java.util.ArrayList<>();
    private String lastTransitHtml = "";
    private String lastSynastryHtml = "";
    /** The right rail: what the app can do, and the grid that is read against the wheel. */
    private DrawerRail menuRail;
    /** The Index tab's content (G11). Package-private so the check can ask what it is showing. */
    IndexPanel indexPanel;
    private javax.swing.JEditorPane gridPane;

    /** Right-rail page names. Two tabs on one strip, not two strips. */
    public static final String MENU_PAGE = "Menu";
    public static final String GRID_PAGE = "Aspect Grids";
    /**
     * The reference index, on the right rail (G11).
     *
     * <b>The tab and the page it opens say the same word.</b> David, asked whether the tab should be
     * called Glossary while the page heading said Index: "they can bith say index". Two names for one
     * thing is how a label comes to claim something the code does not do.
     */
    public static final String INDEX_PAGE = "Index";

    /** Left-rail page names, shared with the wheel and with NavigationCheck. */
    public static final String CHART_PAGE = "Chart";
    public static final String TRANSITS_PAGE = "Transits";
    /**
     * What Chart A and Chart B do to each other.
     *
     * <b>Its own page since 26 Sep.</b> The cross-chart contacts and house overlays used to sit
     * at the foot of the Transits panel, which is a panel about the outer ring - so the one
     * thing that belongs to neither chart alone was filed under one of them. David asked for
     * the tab; the astrology was already there.
     */
    /**
     * The second person's own chart, in their own houses.
     *
     * <b>Not the same page as Synastry and not the same page as Transits.</b> Their placements
     * used to be emitted onto the Transits page whatever the outer ring was carrying, and the
     * tab was renamed "Chart B" to cover for it - which is how David came to ask, on 27 Sep,
     * "where did the transit tab go in a synastry transits chart". One tab was doing two jobs.
     */
    public static final String CHART_B_PAGE = "Chart B";
    /**
     * The Together door, and also the first page behind it, Where you meet - the same string
     * for both, as Chart is for Blueprint, so every caller that held this name still lands on
     * the pair's first page. Write {@link #MEET_PAGE} where the page is meant, not the door.
     *
     * <b>The trap this carries, as Blueprint's does:</b> {@code setPageLabel(SYNASTRY_PAGE)}
     * finds the page inside the strip first and renames that. Rename the door on
     * {@code chartRail} directly.
     */
    public static final String SYNASTRY_PAGE = "Synastry";
    /** The Where you meet page, by the key it shares with its door; see {@link #SYNASTRY_PAGE}. */
    public static final String MEET_PAGE = SYNASTRY_PAGE;
    /** What the Where you meet page is called on its tab. */
    static final String MEET_LABEL = "Where you meet";
    /** The second page behind Together: the aspects between two charts (2026-10-16). */
    public static final String CHEMISTRY_PAGE = "Chemistry";
    /** The pair's first page (reading framework spec, 2026-10-24): the tone of the whole. */
    public static final String SYNASTRY_GLANCE_PAGE = "Synastry at a glance";
    /** What the sky does to both charts and to each, behind Prediction (2026-10-20). */
    public static final String PAIR_COMING_PAGE = "Coming up together";
    /**
     * The Transits tab's own pages (2026-10-22): the current sky on each selected chart. Their
     * own keys and panes, labelled like the pages they mirror, so a person's Placements and the
     * sky's placements in that person's chart never write over each other. See TransitReadings.
     */
    static final String SKY_PLACEMENTS_PAGE = "Sky placements";
    static final String SKY_GLANCE_PAGE = "Sky at a glance";
    static final String SKY_READING_PAGE = "Sky interpretation";
    static final java.util.List<String> SKY_PAGES =
        java.util.List.of(SKY_PLACEMENTS_PAGE, SKY_GLANCE_PAGE, SKY_READING_PAGE);
    /** What kind of chart this is, before any of the detail. */
    public static final String OVERVIEW_PAGE = "Overview";
    public static final String READING_PAGE = "Interpretation";
    /** The chart grouped by the parts of a life (2026-10-13). See {@link LifeAreasPanel}. */
    public static final String LIFE_AREAS_PAGE = "Life areas";
    /**
     * Your year: the next twelve months - the profection, the year scan and what converges in
     * it. The engine's kind is still SYNTHESIZE, which is NarrativeSynthesizer's own name for it.
     */
    public static final String YEAR_PAGE = "Year";

    /**
     * The three rail tabs, each holding a handful of pages.
     *
     * <b>Twelve tabs in one column was the complaint, and grouping them did not fix it.</b>
     * An earlier pass put rules between the three kinds (caa68f9c) - charts, views of them,
     * documents about them - which was the right reading at the wrong granularity: twelve
     * items in three groups is still twelve items, and the rail still asks the reader to
     * choose among all of them at once. David, 2026-10-09: "i just think the current tab
     * structure is a bit bloated and confusing".
     *
     * Three doors, named for the question behind each rather than for the feature inside:
     * who this person is, how two of them meet, and what time it is in a life. The pages
     * themselves are unchanged and keep their own constants - they moved down a level, they
     * did not go away.
     *
     * <b>There is no Lab tab, deliberately.</b> The fourth tab of the architecture David
     * brought back would hold Horary, Electional, Rectify and the harmonics - and those are
     * full centre-stack screens reached from the Menu, not rail panes. A Lab tab here would
     * open onto nothing or duplicate the Menu, and this window has already learned what a
     * door with no room behind it teaches a reader: see the note on the four placeholder
     * cards pulled on 2 September.
     */
    /**
     * <b>The left rail's tabs are the charts since 2026-10-20</b> - see {@link ChartTab}, which
     * holds their keys. For three hours that day they were Placements, Interpretation and
     * Prediction, from a tab spec David pasted; on seeing it he asked for the charts instead,
     * with those three kinds of page under each.
     *
     * <b>Which pages each chart shows.</b> Your year is read from a birthday, which the sky and
     * a composite do not have. Chart B's own placements page belongs to the synastry, where both
     * charts are on the wheel; on a person's own tab the Placements page is theirs. The pair's
     * pages need a pair.
     *
     * <b>A person's timing lives on the Transits tab (David, 2026-10-22):</b> "since under
     * chart a and b there is already a transits tab coming up, yearly and calendar can all be
     * moved under the new transits tab". Chart A and Chart B read the birth chart; Coming up,
     * Your year and Calendar read time against it, and the Transits tab is where time is read.
     * The sky keeps its own Coming up and Calendar, and the pair keeps theirs.
     */
    static final java.util.Map<ChartTab, java.util.List<String>> CHART_TAB_PAGES;
    static {
        java.util.List<String> reading = java.util.List.of(OVERVIEW_PAGE, "Snapshot",
            LIFE_AREAS_PAGE, READING_PAGE, "Synthesize", "Report");
        java.util.Map<ChartTab, java.util.List<String>> m = new java.util.EnumMap<>(ChartTab.class);
        for (ChartTab t : ChartTab.values()) {
            java.util.List<String> l = new java.util.ArrayList<>();
            if (t == ChartTab.TRANSITS) {
                // The same pages as the rest, each reading the sky on the selected charts.
                // Coming up is transits already; with two people their shared one joins it.
                // The same pages in the same order as every other tab (David, 2026-10-22: "make
                // sure each tab looks the same and has the same structure"). Placements, At a
                // glance and Reading are its own; the rest are the shared pages, which read the
                // sky on the chart while this tab is in front (ReadingVoice).
                l.add(SKY_PLACEMENTS_PAGE);
                l.add(OVERVIEW_PAGE);
                l.add(SKY_GLANCE_PAGE);
                l.add(LIFE_AREAS_PAGE);
                l.add(SKY_READING_PAGE);
                l.add("Synthesize");
                l.add("Report");
                l.add(TRANSITS_PAGE);
                l.add(PAIR_COMING_PAGE);
                l.add(YEAR_PAGE);
                l.add("Calendar");
                m.put(t, java.util.List.copyOf(l));
                continue;
            }
            l.add(CHART_PAGE);
            if (t == ChartTab.SYNASTRY) {
                l.add(CHART_B_PAGE);
                l.add(SYNASTRY_GLANCE_PAGE);
                l.add(MEET_PAGE);
                l.add(CHEMISTRY_PAGE);
                l.add(TRANSITS_PAGE);
                l.add(PAIR_COMING_PAGE);
            } else {
                l.addAll(reading);
            }
            if (t != ChartTab.CHART_A && t != ChartTab.CHART_B) {
                if (t != ChartTab.SYNASTRY) {
                    l.add(TRANSITS_PAGE);
                }
                l.add("Calendar");
            }
            m.put(t, java.util.List.copyOf(l));
        }
        CHART_TAB_PAGES = java.util.Collections.unmodifiableMap(m);
    }

    /**
     * The four pages every chart tab has (the reading framework spec, 2026-10-24): At a glance,
     * Placements, Patterns, Full report. David: "this document should supersede" the 3-tab
     * spec of 2026-10-20. The structure never changes between modes; what a group holds does.
     *
     * <b>A group is a set of views, not a new page.</b> Every old page keeps its key, its card and
     * its builder; {@link CHART_TAB_PAGES} still says which of them a chart can answer, and the
     * strip draws the groups on top and, below, the views of the group in front when there is
     * more than one. So "By planet" and "By life area" are the Placements and Life areas pages,
     * and the report's views are the old Report, Reading, Themes, Year, Coming up and Calendar.
     *
     * <b>Every page is in exactly one group</b> - SubRail.setGroups refuses anything else, so a
     * page added later cannot end up on the card stack and nowhere on the strip.
     */
    static final String GLANCE_TAB = "At a glance";
    static final String PLACEMENTS_TAB = "Placements";
    static final String PATTERNS_TAB = "Patterns";
    static final String REPORT_TAB = "Full report";
    static final java.util.Map<String, java.util.List<String>> READING_GROUPS;
    static {
        java.util.Map<String, java.util.List<String>> g = new java.util.LinkedHashMap<>();
        g.put(GLANCE_TAB, java.util.List.of("Snapshot", SKY_GLANCE_PAGE,
            SYNASTRY_GLANCE_PAGE));
        g.put(PLACEMENTS_TAB, java.util.List.of(CHART_PAGE, CHART_B_PAGE, SKY_PLACEMENTS_PAGE,
            CHEMISTRY_PAGE, LIFE_AREAS_PAGE));
        g.put(PATTERNS_TAB, java.util.List.of(OVERVIEW_PAGE, MEET_PAGE));
        g.put(REPORT_TAB, java.util.List.of("Report", READING_PAGE, SKY_READING_PAGE,
            "Synthesize", YEAR_PAGE, TRANSITS_PAGE, PAIR_COMING_PAGE, "Calendar"));
        READING_GROUPS = java.util.Collections.unmodifiableMap(g);
    }

    /**
     * The five readings, each its own tab rather than rows inside a Readings section.
     *
     * <b>The wrapper is gone for the same reason the Menu section went.</b> A section whose
     * whole content is five buttons costs a click on the way to each of them. And once every
     * reading has a page of its own they can be flipped between without regenerating, which a
     * shared panel could never do - the last reading overwrote the one before it.
     *
     * Label first, then the kind {@code SkymapPanel.runReading} answers to.
     */
    public static final String[][] READINGS = {
        {"Snapshot", "SNAPSHOT"},
        {"Report", "REPORT"},
        // The chart's own themes, three witnesses each (ThemeCards). Until 2026-10-20 this key
        // was the year's reading, labelled Your year; the year has its own key now.
        {"Synthesize", "THEMES"},
        {YEAR_PAGE, "SYNTHESIZE"},
        {"Calendar", "CALENDAR"},
    };

    /**
     * The readings that describe time rather than a person, and so sit behind Chronometry.
     *
     * <b>Named, not indexed.</b> READINGS is an ordered table and the obvious split is "the
     * last two" - which would silently re-home Synthesize the first time a sixth reading was
     * appended. A reading belongs to a tab because of what it is about.
     */
    static final java.util.List<String> TIMED_READINGS =
        java.util.List.of(YEAR_PAGE, "Calendar");

    private final java.util.Map<String, javax.swing.JEditorPane> readingPanes =
        new java.util.LinkedHashMap<>();

    /**
     * Which reading page the answer belongs on.
     *
     * The reading runs on a worker and comes back through the same door every reading uses,
     * carrying no clue as to which one asked. Set when the request goes out, read when it
     * returns. Two readings started at once would both land on the second one's page - a
     * worse outcome than a wrong tab only if readings were cheap, and they are not: each
     * takes long enough that starting a second before the first lands is a deliberate act.
     */
    private String pendingReadingPage;

    /**
     * Whose chart is on the wheel, when it came from the saved directory.
     *
     * <b>A loaded chart stops being "a natal chart" and becomes a person's.</b> The heading
     * said "Natal Chart" whichever profile was open, so the one fact a reader most needs when
     * several charts are saved - which one am I looking at - was the fact the panel would not
     * state. Empty when the wheel is showing hand-entered data, which genuinely has no name.
     */
    private String chartName = "";

    /** The name of the loaded chart, or "" when the data was typed rather than loaded. */
    public String chartName() {
        return chartName;
    }

    /** Records whose chart is on the wheel. Cleared by passing null or blank. */
    public void setChartName(String name) {
        this.chartName = name == null ? "" : name.trim();
    }

    /**
     * "David" becomes "David's"; a name already ending in s takes the bare apostrophe.
     *
     * Returns "" for no name, so a caller can prepend it unconditionally.
     */
    public static String possessive(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String n = name.trim();
        char last = n.charAt(n.length() - 1);
        return last == 's' || last == 'S' ? n + "' " : n + "'s ";
    }

    public OuraniaWindow() {
        // Before anything casts a chart: the zodiac, the orbs and the rest the engine reads -
        // the one list the phone applies too.
        Settings.applyToEngine();
        // J6: what version this is, so a bug report can name it. "development build" when running
        // from classes, because a made-up number looks like information.
        setTitle("Ourania+ (Windows Edition) - " + Version.display());
        // A size for a window nobody restores - a check suite's. The application's window is put
        // where the last session left it by main, through WindowPlacement.restore.
        setSize(1024, 768);
        // G13: every shortcut in the app, from the one table that declares them. The window rows
        // behave as they did; the transport rows reach the wheel, which is why this is installed here
        // rather than in WindowPlacement - that class knows about frames, not about charts.
        Shortcuts.install(this, new Shortcuts.Actions() {
            @Override
            public void fullScreen() {
                WindowPlacement.toggleFullScreen(OuraniaWindow.this);
            }

            @Override
            public void leaveFullScreen() {
                if (WindowPlacement.isFullScreen(OuraniaWindow.this)) {
                    WindowPlacement.toggleFullScreen(OuraniaWindow.this);
                }
            }

            @Override
            public void playPause() {
                if (skymapPanel != null) {
                    skymapPanel.playPause();
                }
            }

            @Override
            public void run(int direction) {
                if (skymapPanel != null) {
                    skymapPanel.runTime(direction, SkymapPanel.SLOW_DELAY_MS);
                }
            }

            @Override
            public void now() {
                if (skymapPanel != null) {
                    skymapPanel.jumpToNow();
                }
            }

            // <b>The window's Ctrl+Z, which is not the one a text field gets.</b> A focused
            // component is offered its keys first, so typing in a date field undoes the typing
            // and this fires everywhere else - the behaviour every other application has, and it
            // falls out of the binding order rather than being arranged. G13.
            @Override
            public void undo() {
                if (chartSetupPanel != null) {
                    chartSetupPanel.undo();
                }
            }

            @Override
            public void redo() {
                if (chartSetupPanel != null) {
                    chartSetupPanel.redo();
                }
            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout());

        // <b>Nothing on the west edge any more.</b> Navigation was on the left and the
        // chart's data on the right, so the wheel sat between two panels with a handle down
        // each side - two places to look for one idea. Everything is in SidePanel now.

        // 2. Set up the Content Area (Center) with a CardLayout to swap screens
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.BG);
        add(contentPanel, BorderLayout.CENTER);

        // 3. Initialize Placeholder Screens
        initScreens();
        
        // Start by showing the Skymap screen
        switchScreen("SKYMAP");
    }

    private void initScreens() {
        // Create simple placeholder panels for now
        
        chartSetupPanel = new ChartSetupPanel(this);
        contentPanel.add(chartSetupPanel, "SEARCH");

        // <b>Before SkymapPanel, and that is load-bearing.</b> The wheel's constructor runs a
        // chart update, which calls straight back into updateChartSections - so a right-hand
        // drawer built after the wheel is null for the first update and opens on an empty
        // Natal section. Nothing throws; the panel is simply blank until something moves the
        // chart, which reads as a chart with nothing to report. mainMenu has always been
        // built before the wheel for the same reason.
        sidePanel = new SidePanel(this);

        // <b>The chart's own data moved to the left, opposite the menu.</b> What a chart IS -
        // its name, its moment, its placements - is a different question from what you can do
        // with the app, and the two were sharing one drawer. The rail is a sibling of the
        // content area, so opening it takes width from the wheel and closing gives it back,
        // exactly as the right-hand drawer behaves.
        natalPane = HtmlPanes.chartPane(this);
        chartBPane = HtmlPanes.chartPane(this);
        transitPane = HtmlPanes.chartPane(this);
        synastryPane = HtmlPanes.chartPane(this);
        selectionPane = HtmlPanes.chartPane(this);
        chartRail = new DrawerRail(Drawer.Side.LEFT, 340);
        // <b>One tab per chart, the same pages under each (2026-10-20).</b> David: "it should be
        // separated by charts with placements, interpretations and predictions going under the
        // chart tabs". A tab says which chart is read (ChartTab); the strip says what is read
        // about it. There is one strip, and it moves into whichever tab is in front, because
        // each page is one component and a component has one parent (DrawerRail.setOnShow).
        pages = new SubRail();
        modeBadge = new ModeBadge();
        readingColumn = new JPanel(new BorderLayout());
        readingColumn.setBackground(Theme.BG);
        readingColumn.add(modeBadge, BorderLayout.NORTH);
        readingColumn.add(pages, BorderLayout.CENTER);
        // The same three controls on the Full report group in every mode (reading framework
        // spec, section 7). They act on the view in front, so they are shown by the group.
        exportBar = new ExportBar(this::copyReadingMarkdown, this::exportReadingHtml,
            this::printOrSaveReading);
        exportBar.setVisible(false);
        readingColumn.add(exportBar, BorderLayout.SOUTH);
        pages.setOnChange(this::updateExportBar);
        pages.addPage(CHART_PAGE, HtmlPanes.scroller(natalPane));
        pages.addPage(CHART_B_PAGE, HtmlPanes.scroller(chartBPane));
        // <b>The pair's pages are built here, before the wheel</b>, whose constructor already
        // writes to them; the readings that come before them on the strip are put in front of
        // them further down (SubRail.addPageBefore).
        chemistryPane = HtmlPanes.chartPane(this);
        String pairTip = "<html>Astrologer view: on, show the houses, signs, angles and orbs "
            + "behind each card.<br>Off: the plain reading only.</html>";
        synastryGlancePane = HtmlPanes.chartPane(this);
        pages.addPage(SYNASTRY_GLANCE_PAGE, astrologerPage(synastryGlancePane, pairTip));
        pages.setPageLabel(SYNASTRY_GLANCE_PAGE, GLANCE_TAB);
        pages.addPage(MEET_PAGE, astrologerPage(synastryPane, pairTip));
        pages.addPage(CHEMISTRY_PAGE, astrologerPage(chemistryPane, pairTip));
        pages.setPageLabel(MEET_PAGE, MEET_LABEL);
        // <b>The Now page is Coming up.</b> It is the timeline of transits to the chart, from
        // right now to a year ahead (David, 2026-10-17), and the pop-ups land on its cards - so
        // it moved whole rather than being rebuilt as a reading.
        pages.addPage(TRANSITS_PAGE, astrologerPage(transitPane, "<html>Astrologer view: "
            + "on, under each card the placements it is read from, and the planets' positions"
            + " at the end.<br>Off: the plain cards only.</html>"));
        // The pair's timeline sits next to the one-person timeline it is built from.
        pairComingPane = HtmlPanes.chartPane(this);
        pages.addPage(PAIR_COMING_PAGE, astrologerPage(pairComingPane, "<html>Astrologer "
            + "view: on, show the placements behind each card.<br>Off: the plain cards only."
            + "</html>"));
        for (ChartTab tab : ChartTab.values()) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setBackground(Theme.BG);
            chartHolders.put(tab.door, holder);
            chartRail.addPage(tab.door, holder);
            chartRail.setPageLabel(tab.door, tab.label);
        }
        chartRail.setOnShow(this::showChartTab);
        // <b>Their plain identity, before any chart exists to name them.</b> A SubRail tab
        // shows its own key until something renames it, and updateChartSections - which sets
        // these from the ring headings - returns early while skymapPanel is still being
        // built. So the strip opened reading "Chart > Chart" on a cold start: the key leaking
        // through as a label, in the one state a new reader sees first.
        pages.setPageLabel(CHART_PAGE, BY_PLANET_LABEL);
        pages.setPageLabel(TRANSITS_PAGE, COMING_UP_LABEL);
        // The app opens on the sky (drawSavedChart), so its tab is the one in front.
        chartRail.prepare(ChartTab.SKY.door);
        pages.prepare(CHART_PAGE);
        add(chartRail, BorderLayout.WEST);

        // Built here rather than beside the rail below, because SkymapPanel's constructor
        // calls straight back into updateChartSections and this pane has to exist by then.
        gridPane = HtmlPanes.wideChartPane(this);
        
        // Add our newly ported Skymap Rendering Panel!
        skymapPanel = new SkymapPanel(this);
        // <b>The one component outside Settings a reader cannot do without a name for</b> (G14).
        // The wheel is the app: to a screen reader it is an unnamed drawing surface, and every
        // placement it shows is reachable in words through the Placements page beside it, which
        // is what the description says. Named here rather than in the panel's constructor because
        // four check suites build that panel to inspect it and none of them is a reader.
        Accessibility.name(skymapPanel, "Chart wheel",
            "The chart, drawn. Every position it shows is also listed in words on the "
            + "Placements page of the left rail.");
        // The wheel goes in with the Selected drawer on its left (SelectionDrawer).
        selectionDrawer = new SelectionDrawer(skymapPanel, HtmlPanes.scroller(selectionPane));
        contentPanel.add(selectionDrawer.withWheel(), "SKYMAP");

        nameListPanel = new NameListPanel(skymapPanel, this);
        contentPanel.add(nameListPanel, "NAME_LIST");
        
        // <b>Before the reading, not inside it.</b> Sect, the chart's shape, the prominence
        // ranking and the transfers of light were all computed for Synthesize and appeared
        // only there - a reader who wanted to know what kind of chart this is had to open a
        // long document to find out. Built here rather than beside the other rail pages above
        // because it reads the wheel, and the wheel is built two lines up.
        overviewPanel = new OverviewPanel(skymapPanel, this);
        lifeAreasPanel = new LifeAreasPanel(skymapPanel, this);
        interpretationPanel = new InterpretationPanel(skymapPanel, this);
        // <b>The rail owns the Reading panel's visibility, and nothing else may touch it.</b> The
        // rail is a CardLayout, which shows a page by hiding every other card. Fourteen calls to
        // interpretationPanel.setVisible(true), left from when the panel was a frame region of
        // its own, kept this card visible under whichever page was chosen: on the pages added
        // after it - Snapshot through Calendar - it painted over the page and took its scroll
        // wheel, and on every page its own repaints could land on top. David, 2026-09-14:
        // "make sure the information panel on the left is scrollable it was giving me quite a
        // few glitches". NavigationCheck Part R holds the rail to one visible card.
        // <b>A document is a page that is written on open.</b> Each reading has its own pane, so
        // one can be flipped back to without regenerating; setOnSelect below rewrites it on open.
        java.util.Map<String, JComponent> readings = new java.util.HashMap<>();
        for (String[] r : READINGS) {
            javax.swing.JEditorPane pane = HtmlPanes.chartPane(this);
            readingPanes.put(r[0], pane);
            readings.put(r[0], readingPage(r[1], HtmlPanes.scroller(pane)));
        }
        // <b>The order on the strip: the chart, then the reading, then the timing.</b>
        // Placements (and Chart B's, in a synastry), Chart shape, At a glance, Life areas,
        // Reading, Synthesize, Full report, the pair's two pages, Coming up, Coming up together,
        // Your year, Calendar. Each tab shows the ones its chart can answer (CHART_TAB_PAGES).
        pages.addPageBefore(OVERVIEW_PAGE, overviewPanel, MEET_PAGE);
        pages.addPageBefore("Snapshot", readings.get("Snapshot"), MEET_PAGE);
        pages.addPageBefore(LIFE_AREAS_PAGE, lifeAreasPanel, MEET_PAGE);
        pages.addPageBefore(READING_PAGE, interpretationPanel, MEET_PAGE);
        pages.addPageBefore("Synthesize", readings.get("Synthesize"), MEET_PAGE);
        pages.addPageBefore("Report", readings.get("Report"), MEET_PAGE);
        // Each beside the page it mirrors, so the Transits tab lists its pages in the order
        // every other tab does.
        for (String[] sky : new String[][] {
                {SKY_PLACEMENTS_PAGE, BY_PLANET_LABEL, OVERVIEW_PAGE},
                {SKY_GLANCE_PAGE, "At a glance", LIFE_AREAS_PAGE},
                {SKY_READING_PAGE, PLAIN_LABELS.get(READING_PAGE), "Synthesize"}}) {
            javax.swing.JEditorPane pane = HtmlPanes.chartPane(this);
            skyPanes.put(sky[0], pane);
            pages.addPageBefore(sky[0], astrologerPage(pane, "<html>Astrologer view: on, show"
                + " the placements behind each reading.<br>Off: the plain reading only.</html>"),
                sky[2]);
            pages.setPageLabel(sky[0], sky[1]);
        }
        for (String timed : TIMED_READINGS) {
            pages.addPage(timed, readings.get(timed));
        }
        pages.setGroups(READING_GROUPS);
        PLAIN_LABELS.forEach(this::setPageLabel);
        showChartTab(ChartTab.SKY.door);
        // The wheel's first chart update arrives while it is still being built, before this
        // window has a skymapPanel to ask, so the tabs are named and greyed once here.
        nameTheChartTabs();

        // <b>Opening a tab has to produce the page, not just reveal it.</b> The readings were
        // buttons that ran something; turned into tabs they revealed panes nothing had ever
        // written to, so five of them opened onto blank panels and Interpretation opened onto
        // an empty one. A card swap is not a door - the thing behind the door has to be made.
        // <b>A chart tab puts its chart on the wheel, then re-opens the page in front</b>,
        // which is now a page about that chart. The chips go through the same door.
        chartRail.setOnSelect(door -> {
            ChartTab tab = ChartTab.ofDoor(door);
            if (tab == ChartTab.TRANSITS && chartSetupPanel != null) {
                // The people already chosen stay chosen; with nobody chosen, Chart A, or
                // Chart B when there is no Chart A. The sky goes on over them.
                boolean hasA = skymapPanel.hasChartA();
                boolean hasB = skymapPanel.chartBSubject() != null
                    && skymapPanel.chartBSubject().entered();
                boolean aIn = hasA && skymapPanel.chartAIn() && skymapPanel.innerIsBirthChart();
                boolean bIn = hasB && skymapPanel.chartBIn() && skymapPanel.innerIsBirthChart();
                if (!aIn && !bIn) {
                    aIn = hasA;
                    bIn = !hasA && hasB;
                }
                chartSetupPanel.showTransits(aIn, bIn);
            } else if (tab != null && chartSetupPanel != null
                    && tab != ChartTab.of(chartSetupPanel.currentMode(),
                        skymapPanel.innerIsBirthChart(), skymapPanel.chartAIn(),
                        skymapPanel.chartBIn())) {
                chartSetupPanel.showChart(tab);
            }
            pages.refire();
        });
        pages.setOnSelect(page -> rebuildPage(page, true));

        // <b>One strip on the east edge, with a tab each.</b> The grid was tried across the
        // top, where its handle was a bar lying between two vertical strips and read as a
        // title rather than as a drawer; then as a second drawer on this edge, which put one
        // handle in front of the other instead of beside it. A rail is the shape both attempts
        // were reaching for - the tabs sit next to each other in one column, and one body
        // opens behind whichever is chosen - and it is what the left edge already does, so the
        // window now has one idea on both sides rather than two.
        //
        // Each page opens to its own width; see DrawerRail.addPage for why that is per-page.
        menuRail = new DrawerRail(Drawer.Side.RIGHT, SidePanel.MENU_WIDTH);
        menuRail.addPage(MENU_PAGE, sidePanel, SidePanel.MENU_WIDTH);
        menuRail.addPage(GRID_PAGE, HtmlPanes.scroller(gridPane), 430);
        // G11: the index's door. It had none - nine sections of reference material reachable only
        // from a link inside a reading a reader was already looking at, which is the shape
        // ChartTables' header calls this project's signature defect. Same width as the grid: both
        // are reference tables rather than a column of prose.
        indexPanel = new IndexPanel(this, interpretationPanel);
        menuRail.addPage(INDEX_PAGE, indexPanel, 430);
        // Open on Menu at launch and without animating, which is what the drawer it replaced
        // did: an app that slides its own panel out on every start is animating at the one
        // moment nobody asked it to.
        menuRail.prepare(MENU_PAGE);
        add(menuRail, BorderLayout.EAST);
        
        releasingPanel = new ReleasingPanel(this);
        contentPanel.add(releasingPanel, "RELEASING");

        transitSearchPanel = new TransitSearchPanel(this);
        contentPanel.add(transitSearchPanel, "TRANSIT_SEARCH");
        dialPanel = new DialPanel(this);
        contentPanel.add(dialPanel, "DIAL");
        skyViewPanel = new SkyViewPanel(this);
        contentPanel.add(skyViewPanel, "SKY_VIEW");
        transitCalendarPanel = new TransitCalendarPanel(this);
        contentPanel.add(transitCalendarPanel, "TRANSIT_CALENDAR");

        returnsPanel = new ReturnsPanel(this);
        contentPanel.add(returnsPanel, "RETURNS");

        horaryPanel = new HoraryPanel(this);
        contentPanel.add(horaryPanel, "HORARY");

        electionalPanel = new ElectionalPanel(this);
        contentPanel.add(electionalPanel, "ELECTIONAL");
        heliocentricPanel = new HeliocentricPanel(this);
        contentPanel.add(heliocentricPanel, "HELIOCENTRIC");
        
        // <b>Help is back, with something behind it</b> (G11). It and three others were
        // placeholder cards reading "(Under Construction)" behind live menu entries, pulled on
        // 2 September because a menu that offers ten destinations and delivers six teaches a
        // reader that the menu cannot be trusted. The note then said they come back when there
        // is something behind them, and this is that: a page generated from SidePanel's own
        // arrays and the shortcut table rather than written about them.
        //
        // Share, Sync and Connect are still gone. SidePanel.SCREENS is the list, and
        // NavigationCheck asserts the two agree.
        contentPanel.add(new RectifyPanel(this), "RECTIFY");

        javax.swing.JEditorPane helpPane = HtmlPanes.darkPane();
        HtmlPanes.setHtml(helpPane, Help.html());
        helpPane.setCaretPosition(0);
        Accessibility.name(helpPane, "Help",
            "What each screen does, every keyboard shortcut, and a glossary.");
        contentPanel.add(HtmlPanes.scroller(helpPane), "HELP");

        // After skymapPanel, because applying a selection calls straight into it.
        contentPanel.add(new SettingsPanel(this), "SETTINGS");

        // Sync the wheel's ring bar so chips reflect whether charts exist on cold open
        if (skymapPanel != null) {
            syncRingBar(skymapPanel.ringBarComponent());
        }
    }

    /**
     * Throw the settings screen away and build a new one, showing it.
     *
     * <b>For when the file changed underneath it</b> - restoring a saved set rewrites most of
     * settings.properties at once, and every control on that screen is then showing a value that
     * is no longer true.
     *
     * <b>Rebuilding rather than refreshing, and that is safe for a specific reason.</b> Pushing
     * new values back into forty controls by hand would mean a second copy of what each control
     * reads, free to drift from the first. A fresh panel reads the file the same way the first
     * one did. It is only safe because {@code SettingsPanel} carries a {@code constructing}
     * guard - added after building this screen was found to be writing to settings.properties
     * mid-construction, once reducing twenty-nine points to five - so construction reads and
     * never writes.
     */
    public void rebuildSettings() {
        for (java.awt.Component c : contentPanel.getComponents()) {
            if (c instanceof SettingsPanel) {
                contentPanel.remove(c);
                break;
            }
        }
        contentPanel.add(new SettingsPanel(this), "SETTINGS");
        contentPanel.revalidate();
        contentPanel.repaint();
        cardLayout.show(contentPanel, "SETTINGS");
    }

    /**
     * Opens the settings screen on Diagnostics (J8, J9).
     *
     * <b>The one door, used by both ways in.</b> The start-up notice sends a reader here and so
     * does the side panel; a second route that selected the tab itself would be a second copy of
     * which tab it is.
     */
    public void showDiagnostics() {
        switchScreen("SETTINGS");
        for (java.awt.Component c : contentPanel.getComponents()) {
            if (c instanceof SettingsPanel) {
                ((SettingsPanel) c).showDiagnostics();
                return;
            }
        }
    }

    /**
     * Tells the reader what start-up found, once (J9).
     *
     * <b>What the app can no longer answer properly, and where to look</b> - not a stack trace and
     * not a list of filenames. Every one of these conditions has a working fallback, so the reader
     * is being told their chart is quietly different from what it should be, which is a sentence
     * rather than an error code. Modeless: it must not stand between a reader and the chart they
     * opened the app for.
     */
    public void announce(java.util.List<Startup.Finding> found) {
        StringBuilder sb = new StringBuilder("<html><body style='width:420px'>");
        sb.append("<b>Ourania started with something missing.</b><br><br>");
        for (Startup.Finding f : found) {
            if (f.level != Startup.Level.DEGRADED) {
                continue;
            }
            sb.append("&#9888; ").append(f.what).append("<br><span style='color:#999'>")
                .append(f.detail).append("</span><br><br>");
        }
        sb.append("The app works. Settings &rarr; Diagnostics has the current list and can save "
            + "a report.</body></html>");

        final javax.swing.JOptionPane pane = new javax.swing.JOptionPane(new JLabel(sb.toString()),
            javax.swing.JOptionPane.WARNING_MESSAGE, javax.swing.JOptionPane.DEFAULT_OPTION,
            null, new Object[] {"Open Diagnostics", "Not now"}, "Not now");
        final javax.swing.JDialog dialog = pane.createDialog(this, "Ourania");
        dialog.setModal(false);
        pane.addPropertyChangeListener(javax.swing.JOptionPane.VALUE_PROPERTY, e -> {
            Object picked = pane.getValue();
            dialog.dispose();
            if ("Open Diagnostics".equals(picked)) {
                showDiagnostics();
            }
        });
        dialog.setVisible(true);
    }

    /**
     * Opens or folds a ring, from the wheel.
     *
     * Delegates to Chart Setup rather than touching the mode, so there is still exactly one
     * writer - see {@code ChartSetupPanel.applyRings}.
     */
    public void setRings(boolean chartARing, boolean partnerRing, boolean skyRing) {
        if (chartSetupPanel != null) {
            chartSetupPanel.applyRings(chartARing, partnerRing, skyRing);
        }
    }

    /**
     * What the outer bands carry: the sky, the same person moved on, or the chart directed.
     *
     * <b>A door onto something that was already built.</b> Progressions have ridden the outer
     * ring since the setting existed, and the only way to ask for them was a dropdown three
     * tabs into the Settings screen - so the capability was complete, checked, and invisible
     * from the wheel a reader was looking at. That is this project's most-logged defect, and
     * this is the door for it.
     *
     * <b>And the door only opened one at a time until G17.</b> This method sets the whole set
     * to the one technique named, which is what the setting used to mean and is still what the
     * Settings screen's dropdown asks for. {@link #setOuterWheelOn} is the one to call to add
     * a technique to the wheel rather than replace what is on it.
     */
    public void setOuterWheel(String which) {
        Settings.setOuterWheel(which);
        this.redrawForOuterWheel();
    }

    /** Switch one technique on the outer bands without disturbing the others (G17). */
    public void setOuterWheelOn(String which, boolean on) {
        Settings.setOuterWheelOn(which, on);
        this.redrawForOuterWheel();
    }

    /** The redraw both of the above need, written once rather than twice. */
    private void redrawForOuterWheel() {
        if (skymapPanel != null) {
            skymapPanel.reloadAspectSelection();
            skymapPanel.updateChartData();
            skymapPanel.repaint();
        }
    }

    /** Lets the wheel's chips show what is actually drawn rather than what was last clicked. */
    public void syncRingBar(RingBar bar) {
        if (bar == null) {
            return;
        }
        // <b>The subjects the wheel is actually holding, not the text in the form.</b> A chip
        // says what it will draw, so it has to read what is drawn - a form can hold a half-typed
        // Chart B that no wheel has ever seen.
        if (chartSetupPanel != null && skymapPanel != null) {
            bar.syncFrom(chartSetupPanel.currentMode(), chartSetupPanel.skyWanted(),
                skymapPanel.chartASubject(), skymapPanel.chartBSubject(),
                skymapPanel.skySubject(),
                skymapPanel.chartAIn(), skymapPanel.chartBIn());
        }
        if (skymapPanel != null) {
            bar.syncView(skymapPanel.isGlobeMode());
        }
    }

    /** Folds or unfolds one drawn layer of the chart. */
    public void toggleLayer(Layer layer) {
        if (skymapPanel != null) {
            skymapPanel.setLayer(layer, !skymapPanel.layerWanted(layer));
        }
    }

    /** Whether a drawn layer is currently open, for the chip that folds it. */
    public boolean isLayerOpen(Layer layer) {
        return skymapPanel == null || skymapPanel.layerWanted(layer);
    }

    /**
     * Shows the chart flat or as a globe.
     *
     * <b>A view, so it goes nowhere near ChartMode.</b> Which rings are open is a fact about
     * the chart and is written in exactly one place; how the chart is drawn is not, and
     * routing it through the setup panel would have made the view a fourth thing that can
     * change the mode.
     */
    public void setGlobeView(boolean globe) {
        if (skymapPanel != null) {
            skymapPanel.setGlobeMode(globe);
            syncRingBar(skymapPanel.ringBarComponent());
        }
    }

    /**
     * The chart currently drawn, for a screen that reads it rather than casts its own (G8).
     *
     * <b>Through the window rather than by handing a panel the wheel.</b> SkymapPanel already
     * answers this; what a second panel must not have is a reference to the wheel, which is how
     * two screens end up disagreeing about which chart is showing.
     */
    public com.zodiacomputing.ourania.astro.ChartFrame currentChartFrame() {
        return skymapPanel == null ? null : skymapPanel.getCurrentChart();
    }

    /** The wheel's chart-setting dropdowns, for the Settings screen to host. */
    public javax.swing.JPanel chartControlsPanel() {
        return skymapPanel == null ? null : skymapPanel.chartControlsPanel();
    }

    /** The settings screen changed which aspects are drawn. */
    public void applyAspectSelection() {
        if (skymapPanel != null) {
            skymapPanel.reloadAspectSelection();
        }
    }

    /** The settings screen changed which points the chart shows. */
    public void applyBodySelection() {
        if (skymapPanel != null) {
            skymapPanel.reloadBodySelection();
        }
    }

    private JPanel createPlaceholder(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK);
        
        JLabel label = new JLabel(text + " (Under Construction)", SwingConstants.CENTER);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(Theme.font("Arial", Font.BOLD, 24));
        
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // This method is called by the MainMenuPanel buttons
    public void switchScreen(String screenName) {
        if ("NAME_LIST".equals(screenName) && nameListPanel != null) {
            nameListPanel.updateData();
        }
        if ("INTERPRETATION".equals(screenName) && interpretationPanel != null) {
            // <b>The rail's tab owns showing and hiding this now.</b> The panel used to sit on
            // the east edge with its own visibility flag, and this row flipped it; as a rail
            // page, flipping that flag would empty the tab while leaving the rail open. So the
            // menu row asks the rail for the page, and select() gives the same toggle the tab
            // does - press it again and the rail closes.
            interpretationPanel.updateInterpretations();
            if (chartRail != null) {
                revealPage(READING_PAGE);
            }
            return;
        }
        
        if ("RELEASING".equals(screenName) && releasingPanel != null && skymapPanel != null) {
            releasingPanel.setChart(skymapPanel.getCurrentChart());
        }
        if ("TRANSIT_SEARCH".equals(screenName) && transitSearchPanel != null) {
            transitSearchPanel.refreshChart();
        }
        // The dial reads the chart on the wheel each time it is opened, so changing the chart
        // and coming back cannot show the old one.
        if ("DIAL".equals(screenName) && dialPanel != null) {
            dialPanel.refreshChart();
        }
        if ("SKY_VIEW".equals(screenName) && skyViewPanel != null) {
            skyViewPanel.refreshChart();
        }
        if ("RETURNS".equals(screenName) && returnsPanel != null) {
            returnsPanel.refreshChart();
        }
        if ("TRANSIT_CALENDAR".equals(screenName) && transitCalendarPanel != null) {
            transitCalendarPanel.refreshChart();
        }

        cardLayout.show(contentPanel, screenName);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    /**
     * Set where a midpoint composite's houses are derived, or clear it back to the midpoint.
     *
     * Routed through here rather than handing ChartSetupPanel a SkymapPanel reference, which
     * is how every other setting on that panel reaches the wheel.
     */
    public void applyCompositeReference(double lat, double lon, String name) {
        if (skymapPanel != null) {
            skymapPanel.setCompositeReferencePlace(lat, lon, name);
        }
    }

    public String getCompositeReference() {
        return skymapPanel == null ? null : skymapPanel.getCompositeReferencePlace();
    }

    public void applyChartSettings(String bDate, String bTime, String bLoc, ChartMode mode, String tDate, String tTime, String tLoc, boolean transits) {
        applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc, transits, false);
    }

    /** With Chart A's time-unknown flag; see {@code ChartFrame.computeTimeUnknown}. */
    public void applyChartSettings(String bDate, String bTime, String bLoc, ChartMode mode,
                                   String tDate, String tTime, String tLoc, boolean transits,
                                   boolean baseUnknown) {
        applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc, transits,
            baseUnknown, "");
    }

    /** @param zoneOverride an IANA zone id chosen by hand, or "" to trust the location. */
    public void applyChartSettings(String bDate, String bTime, String bLoc, ChartMode mode,
                                   String tDate, String tTime, String tLoc, boolean transits,
                                   boolean baseUnknown, String zoneOverride) {
        applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc, transits,
            baseUnknown, zoneOverride, "");
    }

    /** @param relocate a place to recast the houses for, or "" for the birthplace. */
    public void applyChartSettings(String bDate, String bTime, String bLoc, ChartMode mode,
                                   String tDate, String tTime, String tLoc, boolean transits,
                                   boolean baseUnknown, String zoneOverride, String relocate) {
        applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc, transits,
            baseUnknown, zoneOverride, relocate, "");
    }

    /** As above, with Chart B's own zone override - every subject carries one. */
    public void applyChartSettings(String bDate, String bTime, String bLoc, ChartMode mode,
                                   String tDate, String tTime, String tLoc, boolean transits,
                                   boolean baseUnknown, String zoneOverride, String relocate,
                                   String tZoneOverride) {
        if (skymapPanel != null) {
            skymapPanel.applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc,
                transits, baseUnknown, zoneOverride, relocate, tZoneOverride);
            // Generating a chart shows the chart. Dropped for a moment when this method was
            // split in two, which would have left Generate looking like it did nothing.
            switchScreen("SKYMAP");
        }
    }
    
    public void showInterpretationForPlanet(String planetName, String signName, int degree, int decanNum, int houseNum, java.util.List<String[]> activeAspects) {
        showInterpretationForPlanet(planetName, signName, degree, decanNum, houseNum, activeAspects, Double.NaN);
    }

    public void showInterpretationForPlanet(String planetName, String signName, int degree, int decanNum, int houseNum, java.util.List<String[]> activeAspects, double lon) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showPlanetInterpretation(planetName, signName, degree, decanNum, houseNum, activeAspects, lon));
            int body = java.util.Arrays.asList(SkymapPanel.BODY_NAMES).indexOf(planetName);
            followClick(currentChartTab() == ChartTab.TRANSITS ? SKY_PLACEMENTS_PAGE : CHART_PAGE,
                body < 0 ? null : "base_" + body, null);
        }
    }

    /** The chart tab in front, or null before the rail exists. */
    private ChartTab currentChartTab() {
        return chartRail == null ? null : ChartTab.ofDoor(chartRail.selected());
    }

    /**
     * The drawer answers a click on the wheel (reading framework spec, section 4): a planet
     * brings up its entry on the Placements tab, a house its part of life, an aspect the Patterns.
     *
     * <b>Only a drawer that is already open, and only to another page.</b> The Selected drawer
     * under the wheel still opens on every click, as David asked on 2026-10-20 ("a click opens
     * Selected at once"); this adds to it and never opens the left rail on its own, which would
     * take width from the wheel the reader is clicking. A click on a link inside the page in
     * front is not a click on the wheel's answer, so a page that is already in front is left
     * where the reader put it - the scroll would move the list under their cursor.
     *
     * @param anchor the name of a link on the page to scroll to, or null
     * @param text   text to find and scroll to when there is no anchor, or null
     */
    private void followClick(String page, String anchor, String text) {
        if (chartRail == null || !chartRail.isOpen() || pages == null
                || !pages.isPageShown(page) || !pages.isPageEnabled(page)
                || page.equals(currentPage())) {
            return;
        }
        revealPage(page);
        // A reveal does not rebuild, and some pages are written on open.
        rebuildPage(page, false);
        JEditorPane pane = paneOf(page);
        if (pane == null || (anchor == null && text == null)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (anchor != null) {
                pane.scrollToReference(anchor);
            } else {
                scrollToText(pane, text);
            }
        });
    }

    /** Scroll a pane so the first place its text says {@code text} is at the top. */
    static void scrollToText(JEditorPane pane, String text) {
        try {
            javax.swing.text.Document doc = pane.getDocument();
            int at = doc.getText(0, doc.getLength()).indexOf(text);
            if (at < 0) {
                return;
            }
            java.awt.Rectangle r = pane.modelToView2D(at).getBounds();
            pane.scrollRectToVisible(new java.awt.Rectangle(r.x, r.y, 1,
                Math.max(r.height, pane.getVisibleRect().height)));
        } catch (javax.swing.text.BadLocationException e) {
            // no such place; the reader keeps their position
        }
    }

    public void showInterpretationForAngle(String angleName, String signName, int degree, java.util.List<String[]> activeAspects) {
        showInterpretationForAngle(angleName, signName, degree, activeAspects, "ANCHOR", -1);
    }

    /** With the K7 role, so the card can say whose angle this is and which way it reads. */
    public void showInterpretationForAngle(String angleName, String signName, int degree,
                                           java.util.List<String[]> activeAspects,
                                           String role, int hostHouse) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showAngleInterpretation(angleName, signName, degree, activeAspects, role, hostHouse));
        }
    }

    /**
     * The wheel's three chart sections, each to the edge that now owns it.
     *
     * Natal and transits to the left rail, the grid to the top drawer, and the right-hand
     * drawer told only that the chart changed so it can refresh its saved-chart list.
     */
    /**
     * Give the two chart panels the names of the rings they are actually showing.
     *
     * <b>"Transits" was telling two readers in three the wrong thing.</b> The outer wheel is a
     * second person in a synastry and this person moved on when progressed, and a fixed tab name
     * cannot be honest about all three - so David opened a panel headed Transits and found Chart
     * B in it, 26 Sep. The inner wheel has the same problem in a composite, where it belongs to
     * nobody.
     *
     * <b>The names come from {@code WheelRing.Kind}</b>, which is the one place that already
     * knows what each ring holds. A seventh copy of that rule, written out here as a ternary on
     * the chart mode, is exactly how the previous six got out of step.
     */
    private void nameTheChartPages() {
        if (chartRail == null || skymapPanel == null) {
            return;
        }
        // <b>The tabs wear the names now (2026-10-20).</b> A person's chart tab is named for
        // them; the Placements page under it says Placements, and in a synastry the two
        // placements pages are named for the two people (showChartTab).
        nameTheChartTabs();
        followTheWheel();
        if (chartRail.selected() != null) {
            showChartTab(chartRail.selected());
        }
    }

    /**
     * What to call the second chart page, which can hold two rings at once.
     *
     * <b>A tri-wheel puts two rings behind one tab, and the name could only hold one.</b>
     * David, 27 Sep: "where did the transit tab go in a synastry transits chart". Nowhere - it
     * had been renamed Chart B. The rule named the outer ring and looked at the sky ring only
     * when there was no outer ring, so on the one chart that draws both, the ring the reader
     * had gone looking for was the one left out of the name. That is the 26 Sep mislabelling
     * again, one case along: the fix replaced a fixed wrong name with a derived one, and the
     * page it renames can hold more rings than the rule could name.
     *
     * <b>The page prints a heading for each ring it holds</b> - Chart B's placements and then
     * "Sky (Transiting)" - so the tab names each ring it holds, for the same reason and out of
     * the same {@code WheelRing.Kind} words.
     *
     * <p>Static, and given its rings rather than reading the panel, so the naming can be
     * measured without building a window - the shape {@code SkymapPanel.triWheelShown} already
     * uses for the neighbouring rule about which of these rings are drawn at all. Nothing
     * asserted this rule when it was written, which is why the missing case shipped; Part P of
     * {@link NavigationCheck} asserts it now.
     */
    /**
     * Whether the outer ring is one of the rings the second page can hold.
     *
     * <b>A person has a page of their own, so they do not reach this one.</b> Chart B's
     * placements used to be emitted onto the second page whatever the outer ring was carrying,
     * and this method's neighbour renamed the tab to cover for it - which is how one tab came
     * to be doing two jobs and how the Transits tab came to look, to David on 27 Sep, as
     * though it had disappeared.
     *
     * <p>Static and given its ring, for the reason {@link #secondPageTitle} is: the rule can be
     * asserted without building a window, and the one line that was NOT extracted this way
     * when this split was first written had nothing holding it.
     */
    static boolean outerReachesSecondPage(WheelRing.Kind outer, boolean outerDrawn) {
        return outerDrawn && outer != null && !outer.isPerson;
    }

    static String secondPageTitle(WheelRing.Kind outer, boolean outerDrawn,
                                  WheelRing.Kind sky, boolean skyDrawn) {
        StringBuilder held = new StringBuilder();
        if (outerDrawn) {
            held.append(pageTitleOf(outer));
        }
        if (skyDrawn) {
            if (held.length() > 0) {
                held.append(" · ");
            }
            held.append(pageTitleOf(sky));
        }
        // No second ring drawn at all: the page is greyed out, and the name it wears while
        // unavailable is its own identity rather than whatever was last on the wheel.
        return held.length() == 0 ? PLACEMENTS_LABEL : held.toString();
    }

    /**
     * A ring's own word, as a tab heading rather than as running prose.
     *
     * <b>The kind knows this now.</b> This was a switch with a default, which is a place where
     * a ring kind added later is silently called "Chart" - and one was added the very next day.
     * {@code WheelRing.Kind.heading} is a constructor argument, so the compiler asks.
     */
    /**
     * What the chart's own page is called: the ring's heading, or its plain identity.
     *
     * <b>"Placements" rather than CHART_PAGE, because the tab above it is now called Chart.</b>
     * A strip reading "Chart > Chart" tells a reader nothing twice. Placements is not a new
     * word either - the wheel's own accessibility description has said "the Placements page of
     * the left rail" since G14, so the app already called it that everywhere except on the tab.
     *
     * The key is untouched: CHART_PAGE is how every caller addresses this page and how the
     * card is registered. Only the shown text changes. See SubRail.setPageLabel for why those
     * are different things.
     */
    static final String PLACEMENTS_LABEL = "Placements";
    /** The first view of the Placements tab; the second is "By life area" (LIFE_AREAS_PAGE). */
    static final String BY_PLANET_LABEL = "By planet";

    /**
     * The pages, as a reader who is not an astrologer would name them (2026-10-13, plan:
     * 2026-10-13-ourania-plain-english-tabs-plan), in the spec's words since 2026-10-20. Labels
     * only: the keys are how callers and suites address the pages, and they do not change.
     */
    /** Chart A's page before it has a person to be named for. Not "Placements": that is the tab. */
    static final String CHART_A_LABEL = "Chart A";
    static final String COMING_UP_LABEL = "Coming up";
    static final java.util.Map<String, String> PLAIN_LABELS = java.util.Map.of(
        "Snapshot", "At a glance",
        "Report", "Dossier",
        "Synthesize", "Themes",
        YEAR_PAGE, "Your year",
        OVERVIEW_PAGE, "Chart shape",
        LIFE_AREAS_PAGE, "By life area",
        READING_PAGE, "Reading");

    /**
     * A chart page is named for whose chart it is.
     *
     * <b>David, 2026-10-10: "under Chart have it named the name of the person whose chart it
     * is".</b> The tab above it already says Chart; repeating that is a word spent saying
     * nothing, and the one fact the reader of a two-person app actually needs from a tab is
     * which of them they are looking at.
     *
     * <b>Three things have to be true before a name is used, and each has a failure behind
     * it.</b> The ring must be somebody - a composite is a chart of a relationship and not of
     * a person, so it keeps its own heading rather than borrowing one partner's name. The
     * subject must have been entered - {@code moment == null} is how ChartSubject says nobody
     * has, and an unentered subject still carries the placeholder label "Chart A", which
     * would put a name on the tab that is not a name. And the label must have something in
     * it, because a chart saved with a blank name would otherwise leave the tab empty, which
     * reads as a broken tab rather than an unnamed one.
     *
     * @param fallback what the page is called when it has no person to be named for
     */
    static String whoseChart(com.zodiacomputing.ourania.astro.ChartSubject who,
                             WheelRing.Kind kind, String fallback) {
        // <b>Not kind.isPerson, however much it reads like the right question.</b> That flag
        // is documented "true when this ring is a person rather than a moment" and CHART_A
        // carries FALSE - it is only ever asked of the OUTER ring, where being a person and
        // being Chart B are the same thing, so its values were never made true of the inner
        // wheel. Using it here named Chart B after Erika and left Chart A as "Chart A".
        // The flag is left alone: SkymapPanel's outer-ring routing and RingIdentityCheck
        // depend on exactly these values.
        //
        // The question actually being asked is whether this chart belongs to somebody, and
        // the composite is the one ring that does not - it is a chart OF a relationship, so
        // naming it after either partner would be a false claim about whose it is.
        if (kind == WheelRing.Kind.COMPOSITE) {
            return kind.heading;
        }
        if (who != null && who.moment != null && who.label != null
                && !who.label.trim().isEmpty()) {
            return who.label.trim();
        }
        return fallback;
    }

    static String pageTitleOf(WheelRing.Kind kind) {
        return kind == null ? PLACEMENTS_LABEL : kind.heading;
    }

    /** Kept so anything still calling the three-part form goes on working. */
    public void updateChartSections(String natalHtml, String transitHtml, String gridHtml) {
        updateChartSections(natalHtml, transitHtml, gridHtml, "");
    }

    /** Kept so anything still calling the four-part form goes on working. */
    public void updateChartSections(String natalHtml, String transitHtml, String gridHtml,
            String synastryHtml) {
        updateChartSections(natalHtml, transitHtml, gridHtml, synastryHtml, "");
    }

    public void updateChartSections(String natalHtml, String transitHtml, String gridHtml,
            String synastryHtml, String chartBHtml) {
        this.lastTransitHtml = transitHtml;
        if (chartBPane != null) {
            HtmlPanes.setHtml(chartBPane, chartBHtml);
        }
        if (chartRail != null) {
            // Greyed without a second person, for the same reason Transits is greyed without
            // transits: a tab that opens onto nothing reads as broken rather than as empty.
            setPageEnabled(CHART_B_PAGE, hasContent(chartBHtml));
        }
        this.lastSynastryHtml = synastryHtml;
        refreshTogetherPanes();
        if (chartRail != null) {
            // Greyed unless there are two people, for the reason the Transits page is greyed
            // without transits: a tab that opens onto nothing reads as broken rather than empty.
            // Both pages, so the door greys too - setPageEnabled greys a tab only when
            // everything behind it is greyed.
            setPageEnabled(SYNASTRY_GLANCE_PAGE, hasContent(synastryHtml));
            setPageEnabled(MEET_PAGE, hasContent(synastryHtml));
            setPageEnabled(CHEMISTRY_PAGE, hasContent(synastryHtml));
            setPageEnabled(PAIR_COMING_PAGE, hasContent(synastryHtml));
        }
        if (natalPane != null) {
            HtmlPanes.setHtml(natalPane, natalHtml);
        }
        refreshTransitsPane();
        refreshSkyPages();
        lightTheChartTabs();
        // <b>Greyed out on a chart drawn without transits.</b> The wheel still emits a transit
        // section for a single chart - the document wrapper with nothing in it - so the tab
        // opened onto a blank panel, which reads as broken rather than as empty. Measured on
        // the markup rather than asked of the wheel, because this method is the only thing
        // that sees the section and it already has it in hand.
        if (chartRail != null) {
            // The sky alone has a Coming up of its own (SkyTimeline), so only a wheel of birth
            // charts with no sky over them greys it (David, 2026-10-20).
            setPageEnabled(TRANSITS_PAGE, hasContent(transitHtml) || skyOnly());
            nameTheChartPages();
        }
        if (gridPane != null) {
            HtmlPanes.setHtml(gridPane, gridHtml);
        }
        if (sidePanel != null) {
            sidePanel.updateChartSections();
        }
        // The saved-chart list moved to Chart Setup, so the rebuild follows it there.
        if (chartSetupPanel != null) {
            chartSetupPanel.refreshProfiles();
        }
    }

    /**
     * Redraws the Now page as plain cards from the last transit section the wheel sent, in the
     * current Astrologer view. <b>The one place that decides which ring is "now":</b> an event
     * chart in the outer ring is the moment being read; otherwise the live sky, if it has one.
     * An empty section is written as it came, so the greyed-tab state is unchanged.
     */
    public void refreshTransitsPane() {
        syncAstrologerChecks();
        if (transitPane == null) {
            return;
        }
        if (skyOnly()) {
            refreshSkyComing(Settings.astrologerView());
            return;
        }
        if (skymapPanel == null || !hasContent(lastTransitHtml)) {
            HtmlPanes.setHtml(transitPane, lastTransitHtml);
            return;
        }
        // The wheel's rule, so the page and the pop-ups that land on it read the same ring.
        WheelRing now = skymapPanel.nowRing();
        // The timeline's dates cost a walk of the ephemeris, which the page does not take
        // while time is running; it shows what is happening right now, and the rest when
        // time stops (SkymapPanel.timeSettled).
        boolean moving = skymapPanel.isPlaying() || skymapPanel.isScrubbing();
        HtmlPanes.setHtml(transitPane, NowCards.html(now, skymapPanel.natalRing,
            skymapPanel.activeCusps, Settings.astrologerView(), lastTransitHtml,
            moving ? null : skymapPanel.sw));
    }

    /** True when the wheel holds the sky and no birth chart. */
    private boolean skyOnly() {
        return skymapPanel != null && !skymapPanel.innerIsBirthChart()
            && skymapPanel.natalRing.sd != null;
    }

    /** The sky's year, kept for the hour of sky time it was worked out for. */
    private java.util.List<SkyTimeline.Entry> skyComing;
    private long skyComingHour = Long.MIN_VALUE;

    /**
     * Coming up for the sky alone (SkyTimeline).
     *
     * <b>On this thread, only when the page is open, and kept for an hour of sky time.</b> A
     * year of sky events takes about a second. It ran on a worker first, and that broke two
     * suites that never open this page: the ephemeris library and Almanac's scan step are
     * shared state, so a second thread casting at the same moment as the window gave other
     * charts wrong points (lots at no longitude). So nothing is cast unless the reader is
     * looking at the page, and the events of a year are reused within the hour, in the view
     * the reader has now.
     */
    private void refreshSkyComing(boolean why) {
        if (!TRANSITS_PAGE.equals(currentPage())) {
            return;
        }
        WheelRing sky = skymapPanel.natalRing;
        double now = sky.sd.getJulDay();
        long hour = (long) Math.floor(now * 24.0);
        if (skyComing == null || hour != skyComingHour) {
            java.time.ZoneId zone = sky.time != null ? sky.time.getZone()
                : java.time.ZoneId.systemDefault();
            skyComing = SkyTimeline.build(skymapPanel.sw, now, zone);
            skyComingHour = hour;
        }
        HtmlPanes.setHtml(transitPane, SkyTimeline.html(skyComing, why));
    }

    /**
     * Redraws the two Together pages as plain cards from the pair on the wheel, in the current
     * Astrologer view. Without a pair - no second chart, or the wheel not built yet - the
     * section is written as it came, so the greyed-tab state is unchanged.
     */
    private void refreshTogetherPanes() {
        syncAstrologerChecks();
        if (synastryPane == null || chemistryPane == null || synastryGlancePane == null) {
            return;
        }
        TogetherCards.Pair pair = synastryPair();
        if (pair == null || !hasContent(lastSynastryHtml)) {
            HtmlPanes.setHtml(synastryPane, lastSynastryHtml);
            HtmlPanes.setHtml(chemistryPane, lastSynastryHtml);
            HtmlPanes.setHtml(synastryGlancePane, lastSynastryHtml);
            return;
        }
        boolean why = Settings.astrologerView();
        HtmlPanes.setHtml(synastryGlancePane, TogetherCards.glanceHtml(pair, why));
        HtmlPanes.setHtml(synastryPane, TogetherCards.meetHtml(pair, why));
        HtmlPanes.setHtml(chemistryPane, TogetherCards.chemistryHtml(pair, why));
        refreshPairComing(why);
    }

    /**
     * Coming up together, from each person's own timeline (PairTimeline).
     *
     * Built only when the page is in front: two timelines are two walks of the ephemeris, and
     * the Together pages are redrawn on every chart change whichever page is showing.
     */
    private void refreshPairComing(boolean why) {
        if (pairComingPane == null || skymapPanel == null || skymapPanel.sw == null
                || !PAIR_COMING_PAGE.equals(currentPage())) {
            return;
        }
        WheelRing sky = skymapPanel.nowRing();
        String a = whoseChart(skymapPanel.chartASubject(), WheelRing.Kind.CHART_A, "Chart A");
        String b = whoseChart(skymapPanel.chartBSubject(), WheelRing.Kind.CHART_B, "Chart B");
        HtmlPanes.setHtml(pairComingPane, PairTimeline.html(
            NowTimeline.build(skymapPanel.sw, sky, skymapPanel.natalRing, skymapPanel.activeCusps),
            a,
            NowTimeline.build(skymapPanel.sw, sky, skymapPanel.outerRing,
                skymapPanel.outerRing.cusps),
            b, why));
    }

    /** Every page drawn as plain cards, after Astrologer view changes. */
    void refreshPlainPages() {
        // The Placements cards show more in Astrologer view, and that page is otherwise written
        // only when the chart changes.
        if (skymapPanel != null) {
            skymapPanel.pushPlacementPages();
        }
        refreshTransitsPane();
        refreshTogetherPanes();
        refreshSkyPages();
    }

    /**
     * Rebuilds the Transits tab's page in front, if one is: the sky on each selected chart.
     *
     * Cast here, on the event thread, from the people and the sky as subjects - a handful of
     * frames and one contact search each, which is what a chip press already costs the wheel.
     */
    void refreshSkyPages() {
        String page = currentPage();
        if (page == null || !SKY_PAGES.contains(page) || skymapPanel == null) {
            return;
        }
        javax.swing.JEditorPane pane = skyPanes.get(page);
        com.zodiacomputing.ourania.astro.ChartFrame sky =
            skymapPanel.frameOf(skymapPanel.skySubject());
        java.util.List<TransitReadings.Person> people = new java.util.ArrayList<>();
        if (skymapPanel.chartAIn() && skymapPanel.hasChartA()) {
            people.add(new TransitReadings.Person(whoseChart(skymapPanel.chartASubject(),
                WheelRing.Kind.CHART_A, "Chart A"),
                skymapPanel.frameOf(skymapPanel.chartASubject())));
        }
        com.zodiacomputing.ourania.astro.ChartSubject b = skymapPanel.chartBSubject();
        if (skymapPanel.chartBIn() && b != null && b.entered()) {
            people.add(new TransitReadings.Person(whoseChart(b, WheelRing.Kind.CHART_B,
                "Chart B"), skymapPanel.frameOf(b)));
        }
        people.removeIf(p -> p.frame() == null);
        boolean why = Settings.astrologerView();
        if (SKY_PLACEMENTS_PAGE.equals(page)) {
            // <b>The usual Placements list (David, 2026-10-22: "Should be the usual list").</b>
            // The wheel's own section for the sky ring around the chosen chart, its houses that
            // chart's, under a heading that says which chart the sky is on.
            String heading = people.isEmpty() ? "" : SkyOnChart.sectionHeading(
                SkyOnChart.heading(people.get(0).whose()));
            String list = hasContent(lastTransitHtml) ? lastTransitHtml : TransitReadings.nobody();
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<body[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(list);
            HtmlPanes.setHtml(pane, m.find()
                ? list.substring(0, m.end()) + heading + list.substring(m.end())
                : "<html><body>" + heading + list + "</body></html>");
            return;
        }
        if (SKY_READING_PAGE.equals(page)) {
            // <b>The Reading page's own structure, read from the sky over the chart (David,
            // 2026-10-22):</b> "what is there right now could just move right over under
            // transits reading". Under a heading that says which chart the sky is on.
            String reading = interpretationPanel == null || people.isEmpty()
                ? "<html><body>" + TransitReadings.nobody() + "</body></html>"
                : interpretationPanel.capture(interpretationPanel::showTransitReading);
            String heading = people.isEmpty() ? "" : SkyOnChart.sectionHeading(
                SkyOnChart.heading(people.get(0).whose()));
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<body[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(reading == null ? "" : reading);
            // <b>Then the Lots (David's model, 2026-10-22):</b> a planet on a natal Lot, the
            // sky's Lot on a natal planet, and a planet on the ruler of the Lot's sign, for each
            // chosen chart under its own heading. See LotTransits.
            StringBuilder lots = new StringBuilder();
            for (TransitReadings.Person p : people) {
                java.util.List<ReadingCard> cards = LotTransits.cards(p.frame(), sky);
                lots.append(SkyOnChart.sectionHeading("The Lots on "
                    + SkyOnChart.chartHeading(p.whose())));
                lots.append(cards.isEmpty()
                    ? "<p style='color:#AAAAAA;'>Nothing in the sky is reaching a Lot in this"
                        + " chart right now.</p>"
                    : ReadingCard.html(cards, why));
            }
            String done = reading != null && m.find()
                ? reading.substring(0, m.end()) + heading + reading.substring(m.end())
                : "<html><body>" + heading + "</body></html>";
            int close = done.toLowerCase(java.util.Locale.ROOT).lastIndexOf("</body>");
            HtmlPanes.setHtml(pane, close < 0 ? done + lots
                : done.substring(0, close) + lots + done.substring(close));
            return;
        }
        // The soonest contacts first (reading framework spec, Mode 3): what turns exact in the
        // next two weeks, for the first chart chosen, above the contacts that are strong now.
        String body = comingUpNext(people) + TransitReadings.glance(people, sky, why);
        HtmlPanes.setHtml(pane, "<html><body style='font-family: sans-serif; font-size: 13px;"
            + " margin: 12px; color: #E0E0E0; background-color: #000000;'>" + body
            + "</body></html>");
    }

    /**
     * "Coming up next" for the first chart chosen, or nothing when there is no one to read or no
     * ephemeris. The timeline is the one the Coming up page walks ({@link NowTimeline}), read
     * from the same rings as {@link #refreshPairComing}: Chart A is the inner ring, and Chart B,
     * chosen alone, is the outer ring in its own houses.
     */
    private String comingUpNext(java.util.List<TransitReadings.Person> people) {
        if (people.isEmpty() || skymapPanel == null || skymapPanel.sw == null) {
            return "";
        }
        WheelRing sky = skymapPanel.nowRing();
        if (sky == null || sky.sd == null) {
            return "";
        }
        boolean chartA = skymapPanel.chartAIn() && skymapPanel.hasChartA();
        WheelRing natal = chartA ? skymapPanel.natalRing : skymapPanel.outerRing;
        double[] cusps = chartA ? skymapPanel.activeCusps : skymapPanel.outerRing.cusps;
        return ComingUpNext.html(people.get(0).whose(),
            ComingUpNext.cards(NowTimeline.build(skymapPanel.sw, sky, natal, cusps),
                sky.sd.getJulDay()), Settings.astrologerView());
    }

    /**
     * The two charts on the wheel when it is a synastry, else null.
     *
     * <b>Filtered by the grid's own switches:</b> the bodies are the ones the inner ring shows
     * ({@link WheelRing#shows}, as Life areas asks it) and the aspects the ones the reader has
     * on (the panel's selection, which {@link AspectGate} reads for the grid).
     */
    private TogetherCards.Pair synastryPair() {
        if (skymapPanel == null || !skymapPanel.isSynastryChart()) {
            return null;
        }
        com.zodiacomputing.ourania.astro.ChartFrame you = skymapPanel.synastryChartA();
        com.zodiacomputing.ourania.astro.ChartFrame them = skymapPanel.synastryChartB();
        if (you == null || them == null) {
            return null;
        }
        WheelRing ring = skymapPanel.natalRing;
        return new TogetherCards.Pair(you, them, ring == null ? null : ring::shows,
            skymapPanel.aspectSelection());
    }

    /** Ticks every page's "Show the astrology" box to match the setting. */
    private void syncAstrologerChecks() {
        for (javax.swing.JCheckBox box : astrologerChecks) {
            if (box.isSelected() != Settings.astrologerView()) {
                box.setSelected(Settings.astrologerView());
            }
        }
    }

    /**
     * A plain page: the "Show the astrology" box above the pane.
     *
     * <b>One box per page, one setting behind all of them.</b> The pane is HTML 3.2 and cannot
     * collapse a drawer itself (see {@link ReadingCard}), so the choice is made before the page
     * is built - and a reader who ticks it on Now expects Together to have changed too.
     */
    private JComponent astrologerPage(javax.swing.JEditorPane pane, String tip) {
        javax.swing.JCheckBox box = new javax.swing.JCheckBox("Show the astrology",
            Settings.astrologerView());
        box.setForeground(Theme.TEXT);
        box.setBackground(Color.BLACK);
        box.setFont(Theme.BODY);
        box.setFocusPainted(false);
        box.setToolTipText(tip);
        box.addItemListener(e -> {
            // Guarded, because syncAstrologerChecks sets the other boxes and each would
            // otherwise redraw every page again.
            if (box.isSelected() != Settings.astrologerView()) {
                Settings.setAstrologerView(box.isSelected());
                refreshPlainPages();
            }
        });
        astrologerChecks.add(box);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        strip.setBackground(Color.BLACK);
        strip.add(box);
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Color.BLACK);
        page.add(strip, BorderLayout.NORTH);
        page.add(HtmlPanes.scroller(pane), BorderLayout.CENTER);
        return page;
    }

    /**
     * Whether a section of chart markup actually says anything.
     *
     * Strips the tags and asks whether words are left. An empty section is not an empty
     * string - it is a full html/body wrapper with nothing between the tags - so a length
     * test on the markup would call every section populated.
     */
    static boolean hasContent(String html) {
        if (html == null) {
            return false;
        }
        String text = html.replaceAll("(?s)<[^>]*>", " ")
            .replace("&nbsp;", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return !text.isEmpty();
    }

    /** The aspect grid exactly as the top drawer is showing it, for export. */
    String gridHtml() {
        return gridPane == null ? "" : gridPane.getText();
    }

    /** The right rail, for a check that needs to walk its tabs. */
    DrawerRail menuRail() {
        return menuRail;
    }

    /** The left rail, for a check that needs to walk its pages. */
    /** The Overview page, for the check. */
    OverviewPanel overviewPanel() {
        return overviewPanel;
    }

    /**
     * Which strip a page lives on, or null when the page is a rail tab in its own right.
     *
     * <b>Asked of the strips rather than listed here.</b> A second table saying which page is
     * behind which tab would be the same list written twice, and the first edit that added a
     * page to a strip without updating the table would send every caller holding that page's
     * name to the wrong tab - silently, because revealing the wrong tab still reveals
     * something.
     */
    private SubRail homeOf(String page) {
        for (SubRail strip : strips().values()) {
            if (strip.pages().contains(page)) {
                return strip;
            }
        }
        return null;
    }

    /**
     * The rail tab a page opens under: the chart tab in front, for a page on the strip.
     *
     * <b>Opening a page never changes which chart is read (2026-10-20).</b> Every chart tab holds
     * the same strip, so "which tab is this page behind" has five answers, and choosing another
     * one would move the strip without moving the wheel - a page about one chart under the tab
     * of another. A page this chart does not show stays where it is; see revealPage.
     */
    private String tabOf(String page) {
        if (homeOf(page) != null && chartRail != null && chartRail.selected() != null) {
            return chartRail.selected();
        }
        return page;
    }

    /**
     * Each door and the strip behind it, in rail order.
     *
     * <b>The one place that pairs a door with its strip.</b> Each method below used to name
     * Blueprint and Chronometry itself, so a third door with a strip (Together, 2026-10-16)
     * would have been five edits, and a missed one would route a page to no tab at all.
     */
    private java.util.Map<String, SubRail> strips() {
        java.util.Map<String, SubRail> out = new java.util.LinkedHashMap<>();
        if (pages != null) {
            for (ChartTab t : ChartTab.values()) {
                out.put(t.door, pages);
            }
        }
        return out;
    }

    /**
     * A chart tab came to the front: the strip moves into it, and shows that chart's pages.
     *
     * Called on every showing - a click, a reveal, a prepare - so a tab is never empty. The
     * chart on the wheel is not changed here: a click changes it (setOnSelect), and a chip
     * changes it and then moves the tab (followTheWheel).
     */
    private void showChartTab(String door) {
        ChartTab tab = ChartTab.ofDoor(door);
        JPanel holder = chartHolders.get(door);
        if (tab == null || holder == null || pages == null) {
            return;
        }
        if (readingColumn.getParent() != holder) {
            holder.add(readingColumn, BorderLayout.CENTER);
            holder.revalidate();
            holder.repaint();
        }
        java.util.List<String> shown = CHART_TAB_PAGES.get(tab);
        for (String page : pages.pages()) {
            pages.setPageShown(page, shown.contains(page));
        }
        // In a synastry the two Placements pages are the two people; on one chart it is theirs.
        if (tab == ChartTab.SYNASTRY && skymapPanel != null) {
            pages.setPageLabel(CHART_PAGE, whoseChart(skymapPanel.chartASubject(),
                WheelRing.Kind.CHART_A, CHART_A_LABEL));
            pages.setPageLabel(CHART_B_PAGE, whoseChart(skymapPanel.chartBSubject(),
                WheelRing.Kind.CHART_B, CHART_B_PAGE));
        } else {
            pages.setPageLabel(CHART_PAGE, BY_PLANET_LABEL);
        }
        updateModeBadge(tab);
    }

    /**
     * Says which kind of reading the tab in front is and of which charts (reading framework
     * spec, 2026-10-24). Called whenever the tab changes and whenever the charts do, because the
     * line carries names and the pill depends on whether the sky is on the wheel.
     */
    private void updateModeBadge(ChartTab tab) {
        if (modeBadge == null || tab == null || skymapPanel == null) {
            return;
        }
        com.zodiacomputing.ourania.astro.ChartSubject sa = skymapPanel.chartASubject();
        com.zodiacomputing.ourania.astro.ChartSubject sb = skymapPanel.chartBSubject();
        boolean aIn = skymapPanel.hasChartA() && skymapPanel.chartAIn();
        boolean bIn = sb != null && sb.entered() && skymapPanel.chartBIn();
        boolean skyOn = chartSetupPanel != null && chartSetupPanel.skyWanted();
        String a = whoseChart(sa, WheelRing.Kind.CHART_A, CHART_A_LABEL);
        String b = whoseChart(sb, WheelRing.Kind.CHART_B, ChartTab.CHART_B.label);
        // On the Transits tab the line names the people under the sky, and no one who is off.
        String lineA = a;
        String lineB = b;
        if (tab == ChartTab.TRANSITS && !(aIn && bIn)) {
            lineA = bIn ? b : a;
            lineB = null;
        }
        modeBadge.show(ModeBadge.modeOf(tab, skyOn, aIn, bIn),
            ModeBadge.contextOf(tab, lineA, sa == null ? null : sa.placeName, lineB,
                sb == null ? null : sb.placeName,
                chartSetupPanel == null ? null : chartSetupPanel.currentMode(),
                skymapPanel.nowRing() == null ? null : skymapPanel.nowRing().time));
    }

    /** The badge over the tabs, for the suites. */
    ModeBadge modeBadge() {
        return modeBadge;
    }

    /**
     * Moves the chart tab to the chart the wheel holds, after a chip or the setup screen
     * changed it. Neither opens nor closes the rail, and does not re-cast anything.
     */
    private void followTheWheel() {
        if (chartRail == null || skymapPanel == null || chartSetupPanel == null) {
            return;
        }
        // <b>The Transits tab stays while it has something to read (2026-10-22):</b> the sky,
        // and at least one person under it. Choosing who with the chips is what it is for.
        if (ChartTab.TRANSITS.door.equals(chartRail.selected()) && chartSetupPanel.skyWanted()
                && skymapPanel.innerIsBirthChart() && !skymapPanel.isRelationshipChart()) {
            return;
        }
        ChartTab tab = ChartTab.of(chartSetupPanel.currentMode(),
            skymapPanel.innerIsBirthChart(), skymapPanel.chartAIn(), skymapPanel.chartBIn());
        if (!tab.door.equals(chartRail.selected())) {
            chartRail.choose(tab.door);
        }
    }

    /**
     * A person's tab is lit only while their chart is on the wheel (David, 2026-10-22: "even
     * when chart a is off its still on in the tabs"). Dimmed, not greyed: it can still be
     * clicked, and clicking it puts the chart back on.
     */
    private void lightTheChartTabs() {
        if (chartRail == null || skymapPanel == null) {
            return;
        }
        com.zodiacomputing.ourania.astro.ChartSubject b = skymapPanel.chartBSubject();
        chartRail.setPageLit(ChartTab.CHART_A.door,
            skymapPanel.hasChartA() && skymapPanel.chartAIn() && !skymapPanel.skyAlone());
        chartRail.setPageLit(ChartTab.CHART_B.door,
            b != null && b.entered() && skymapPanel.chartBIn() && !skymapPanel.skyAlone());
    }

    /**
     * Each chart tab's name and whether it can be opened: a person's tab wears their name
     * (David, 2026-10-20: Chart A and B "should take the names of the person the chart is
     * for"), a tab for a chart that is not entered is greyed, and the sky is always there.
     */
    private void nameTheChartTabs() {
        if (chartRail == null || skymapPanel == null) {
            return;
        }
        boolean a = skymapPanel.hasChartA();
        boolean b = skymapPanel.chartBSubject() != null && skymapPanel.chartBSubject().entered();
        chartRail.setPageLabel(ChartTab.CHART_A.door, whoseChart(skymapPanel.chartASubject(),
            WheelRing.Kind.CHART_A, ChartTab.CHART_A.label));
        chartRail.setPageLabel(ChartTab.CHART_B.door, whoseChart(skymapPanel.chartBSubject(),
            WheelRing.Kind.CHART_B, ChartTab.CHART_B.label));
        chartRail.setPageEnabled(ChartTab.CHART_A.door, a);
        chartRail.setPageEnabled(ChartTab.CHART_B.door, b);
        chartRail.setPageEnabled(ChartTab.SYNASTRY.door, a && b);
        // The sky needs someone to be read against.
        chartRail.setPageEnabled(ChartTab.TRANSITS.door, a || b);
        chartRail.setPageEnabled(ChartTab.COMPOSITE.door, a && b);
        updateModeBadge(ChartTab.ofDoor(chartRail.selected()));
    }

    /**
     * Open a page, wherever it lives.
     *
     * <b>Every caller names a page and none of them should know the rail has two levels.</b>
     * There were thirty-odd {@code chartRail.reveal(X)} calls across this window before the
     * rail became three tabs, and rewriting each of them to open a tab and then a strip would
     * be the structure written thirty times. One method knows; the callers go on naming pages.
     */
    void revealPage(String page) {
        if (chartRail == null) {
            return;
        }
        chartRail.reveal(tabOf(page));
        SubRail home = homeOf(page);
        if (home != null) {
            home.reveal(page);
        }
    }

    /** The page actually in front of the reader, a level down where there is one. */
    String currentPage() {
        if (chartRail == null) {
            return null;
        }
        String tab = chartRail.selected();
        SubRail inside = strips().get(tab);
        return inside != null ? inside.selected() : tab;
    }

    /**
     * Grey a page out, wherever it lives - and the tab above it when nothing inside is left.
     *
     * <b>A tab whose every page is greyed is a door to an empty room.</b> That is the defect
     * the placeholder cards were pulled for on 2 September, arriving by a new route: the page
     * greying was honest and the tab above it was not.
     */
    void setPageEnabled(String page, boolean on) {
        SubRail home = homeOf(page);
        if (home == null) {
            if (chartRail != null) {
                chartRail.setPageEnabled(page, on);
            }
            return;
        }
        // A chart tab is greyed by whether its chart exists (nameTheChartTabs), not by its pages.
        home.setPageEnabled(page, on);
    }

    boolean isPageEnabled(String page) {
        SubRail home = homeOf(page);
        if (home != null) {
            return home.isPageEnabled(page);
        }
        return chartRail != null && chartRail.isPageEnabled(page);
    }

    /** Rename a tab, wherever it lives. The page's own name never changes; see SubRail. */
    void setPageLabel(String page, String shown) {
        SubRail home = homeOf(page);
        if (home != null) {
            home.setPageLabel(page, shown);
        } else if (chartRail != null) {
            chartRail.setPageLabel(page, shown);
        }
    }

    String pageLabel(String page) {
        SubRail home = homeOf(page);
        return home != null ? home.pageLabel(page)
            : chartRail != null ? chartRail.pageLabel(page) : null;
    }

    /**
     * Every page a reader can reach on the left, across both levels.
     *
     * <b>For callers whose claim is "this page exists", which is most of them.</b> Asking the
     * rail alone answers three names now, and a suite that checked the rail for Snapshot would
     * report the reading missing when it had merely moved down a level - a false alarm that
     * teaches the next reader to loosen the assertion.
     */
    java.util.List<String> allPages() {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (chartRail == null) {
            return out;
        }
        // Every chart tab holds the same strip, so its pages are listed once, not once a tab.
        java.util.Map<String, SubRail> strips = strips();
        java.util.Set<SubRail> listed = new java.util.HashSet<>();
        for (String tab : chartRail.pageNames()) {
            SubRail inside = strips.get(tab);
            if (inside == null) {
                out.add(tab);
            } else if (listed.add(inside)) {
                out.addAll(inside.pages());
            }
        }
        return out;
    }

    /** The component behind a page, wherever it lives. */
    JComponent pageComponent(String page) {
        SubRail home = homeOf(page);
        return home != null ? home.page(page)
            : chartRail != null ? chartRail.page(page) : null;
    }

    /** The pages' strip, the one every chart tab holds, for the suites. */
    SubRail pagesRail() {
        return pages;
    }

    DrawerRail chartRail() {
        return chartRail;
    }

    /** Opens the left rail on a page, for a caller that needs it visible. */
    public void showChartPage(String page) {
        if (chartRail != null) {
            revealPage(page);
        }
    }

    /**
     * Loads a saved chart from the profile directory into Chart A or Chart B.
     *
     * The store behind it - {@link SavedCharts} - has backed Chart Setup's Save and Load
     * buttons all along; until now nothing else could reach it.
     */
    public void loadSavedProfile(String name, boolean asPartner) {
        if (chartSetupPanel != null) {
            chartSetupPanel.applySavedProfile(name, asPartner);
        }
    }

    /**
     * One body's detail, from a click on the wheel, onto the left rail's Selection page.
     *
     * <b>Reveals rather than toggles.</b> A click on a body is a request to see that body, so
     * this uses reveal and not select - select would close the rail when the page it wants is
     * already the one showing, and clicking a second planet would put the panel away.
     */
    /**
     * Brings the Interpretation tab forward for a reading a click just wrote.
     *
     * <b>Every wheel click wrote its reading into a tab nobody could see.</b> A body click
     * reveals Selection; a sign, decan, house, degree, mansion, aspect or angle click wrote into
     * Interpretation and left the rail as it was - closed on a cold open, or showing the
     * Selection card after any body had been clicked. Measured with a scripted click test on
     * 2026-09-14: every such click changed the hidden page and nothing on screen, which David
     * reported as "if a decan is selected no other decan can be selected after it" and "some
     * things just won't click". {@code reveal}, not {@code select}: select toggles the rail shut
     * when the tab is already showing, and regenerates the page through onSelect, which would
     * overwrite the reading that was just written.
     */
    private void revealReading() {
        if (chartRail != null) {
            revealPage(READING_PAGE);
        }
    }

    /**
     * A clicked thing's reading, onto the Selection page, whole and from the top.
     *
     * <b>Interpretation reads the chart; Selection says what was clicked.</b> David, 2026-09-14:
     * "interpretation was meant to be a tab to interpret the entire chart, selection was meant
     * to define and show what was selected". Every wheel, grid and placement click - a sign, a
     * decan, a house, a Sabian degree, a mansion, a bound, an aspect, a body from another ring -
     * wrote its reading over the Interpretation page instead, so the whole-chart reading was
     * replaced by whatever was last clicked. The panel still builds the page, once, through
     * the same generator; {@link InterpretationPanel#capture} hands it here instead of to the
     * panel's own pane.
     */
    private void selectReading(Runnable show) {
        String html = interpretationPanel.capture(show);
        if (html != null) {
            showSelection(html, true);
        }
    }

    public void showSelection(String html) {
        showSelection(html, false);
    }

    /**
     * @param fromTop start the page at its top. A new selection is read from its heading; the
     *                selection card's own expanders keep the reader's place instead.
     */
    void showSelection(String html, boolean fromTop) {
        if (selectionPane != null) {
            HtmlPanes.setHtml(selectionPane, html);
            if (fromTop) {
                // Queued behind setHtml's own deferred restore of the old position.
                final javax.swing.JScrollPane scroll = (javax.swing.JScrollPane)
                    javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JScrollPane.class,
                        selectionPane);
                if (scroll != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> javax.swing.SwingUtilities.invokeLater(
                        () -> scroll.getVerticalScrollBar().setValue(0)));
                }
            }
        }
        // <b>Left of the wheel, and the rail is left as it was.</b> The reader keeps the page
        // they were reading; what they clicked opens beside it rather than in its place.
        if (selectionDrawer != null) {
            selectionDrawer.open();
        }
    }

    /** Whether the Selected drawer is open beside the wheel. */
    boolean selectionShowing() {
        return selectionDrawer != null && selectionDrawer.isOpen();
    }

    /** Puts the Selected drawer away. */
    void closeSelection() {
        if (selectionDrawer != null) {
            selectionDrawer.close();
        }
    }

    /**
     * The Calendar tab: this month's grid first, and the year's sky events a click away.
     *
     * <b>The spec's calendar is a month grid of the transits to the chart (2026-10-20).</b> The
     * tab held only the year's list of sky events - ingresses, stations, lunations - which say
     * nothing about the reader. TransitCalendarPanel already shades each day by the transits to
     * Chart A, but only a Menu entry opened it, so the Calendar tab and the calendar never met.
     * Two buttons, not a checkbox, because they choose between two views and keep no setting.
     */
    private JComponent calendarPage(JComponent yearList) {
        calendarMonth = new TransitCalendarPanel(this, true);
        java.awt.CardLayout views = new java.awt.CardLayout();
        JPanel body = new JPanel(views);
        body.add(calendarMonth, "month");
        body.add(yearList, "year");
        JButton month = new JButton("Your month");
        JButton year = new JButton("The year's sky");
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        strip.setBackground(Color.BLACK);
        for (JButton b : new JButton[] {month, year}) {
            Widgets.styleButton(b, Widgets.Role.TRANSPORT);
            strip.add(b);
        }
        month.setToolTipText("Each day of the month, shaded by what reaches your chart.");
        year.setToolTipText("The sky's own events this year: sign changes, stations, new and full"
            + " moons.");
        month.addActionListener(e -> views.show(body, "month"));
        year.addActionListener(e -> views.show(body, "year"));
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Color.BLACK);
        page.add(strip, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    /**
     * A reading's page: the scrolled pane, with a strip of controls above it where it has any.
     *
     * <b>Only Synthesize has one, and only because a Swing HTML pane cannot collapse a
     * section.</b> The reading is four tiers of progressive disclosure and its fourth - the
     * architecture, every placement, the patterns, the houses, the rankings and the transits -
     * is the audit trail, which belongs behind a disclosure triangle. HTML 3.2 has no
     * {@code <details>} and the pane runs no script, so the tier is chosen before the page is
     * built and the triangle is this checkbox.
     *
     * <b>It writes to Settings and re-runs the reading rather than hiding anything.</b> The tab
     * regenerates on every open - a reading is a snapshot of a moment, and the moment moves -
     * so a choice held in the widget would be lost as soon as the reader looked elsewhere.
     *
     * Every other reading is returned exactly as it was, so this cannot become the place a
     * control quietly appears on five pages.
     */
    private JComponent readingPage(String kind, JComponent body) {
        if ("CALENDAR".equals(kind)) {
            return calendarPage(body);
        }
        // Synthesize's cards carry their witnesses in the same "why" box as At a glance's.
        if ("SNAPSHOT".equals(kind) || "THEMES".equals(kind)) {
            // The same strip as Your year's mechanics box, for the same HTML 3.2 reason:
            // the choice is made before the page is built, so it is a setting.
            JCheckBox why = new JCheckBox("Show the astrology", Settings.astrologerView());
            why.setForeground(Theme.TEXT);
            why.setBackground(Color.BLACK);
            why.setFont(Theme.BODY);
            why.setFocusPainted(false);
            why.setToolTipText("<html>Astrologer view, for every page: under each card here, the placements it "
                + "was read from; on Chart shape, every card's detail open; on Reading, the "
                + "degrees, decans, houses and Sabian symbols.<br>Off: the plain reading "
                + "only.</html>");
            // <b>One setting, every box (2026-10-20).</b> This box was not in astrologerChecks,
            // so ticking the box on Coming up turned the setting on while this one still showed
            // it off - and Synthesize showed the astrology under an empty box. Guarded as
            // astrologerPage's is, because syncAstrologerChecks sets every box.
            why.addItemListener(e -> {
                if (why.isSelected() != Settings.astrologerView()) {
                    Settings.setAstrologerView(why.isSelected());
                    refreshPlainPages();
                    runReading(kind);
                }
            });
            astrologerChecks.add(why);
            JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            strip.setBackground(Color.BLACK);
            strip.add(why);
            JPanel page = new JPanel(new BorderLayout());
            page.setBackground(Color.BLACK);
            page.add(strip, BorderLayout.NORTH);
            page.add(body, BorderLayout.CENTER);
            return page;
        }
        if (!"SYNTHESIZE".equals(kind)) {
            return body;
        }
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Color.BLACK);
        JCheckBox mechanics = new JCheckBox("Show the mechanics",
            Settings.readingMechanics());
        mechanics.setForeground(Theme.TEXT);
        mechanics.setBackground(Color.BLACK);
        mechanics.setFont(Theme.BODY);
        mechanics.setFocusPainted(false);
        mechanics.setToolTipText("<html>On: the whole reading &mdash; the summary, the year's "
            + "themes and dates, then tier 4, which is the architecture, every placement, the "
            + "patterns, the houses, the convergence rankings and the current transits.<br>"
            + "Off: tiers 1 to 3 only. The same answers, without the working.</html>");
        mechanics.addItemListener(e -> {
            Settings.setReadingMechanics(mechanics.isSelected());
            runReading("SYNTHESIZE");
        });
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        strip.setBackground(Color.BLACK);
        strip.add(mechanics);
        page.add(strip, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    /** One reading, from the sidebar's Readings section. See {@code SkymapPanel.runReading}. */
    /**
     * When the reading page in front is the one of this kind, records it as the page the next
     * answer belongs on, as {@link #runReading} does (2026-10-24). The wheel re-runs a reading
     * after a chart change without asking for it, so no reveal plays; this only routes it.
     *
     * @param kind the reading's kind as READINGS names it: "SNAPSHOT", "SYNTHESIZE", ...
     */
    void routeRerunToFront(String kind) {
        String page = currentPage();
        for (String[] r : READINGS) {
            if (r[0].equals(page) && r[1].equals(kind)) {
                pendingReadingPage = page;
                return;
            }
        }
    }

    public void runReading(String name) {
        // Remember which tab asked, so the answer lands there rather than on whichever page
        // the reading panel happens to be showing.
        pendingReadingPage = null;
        for (String[] r : READINGS) {
            if (r[1].equals(name)) {
                pendingReadingPage = r[0];
                break;
            }
        }
        if (skymapPanel != null) {
            skymapPanel.runReading(name);
        }
    }

    /**
     * Open the browsable index, from anywhere.
     *
     * <b>The index needed a door on the always-visible panel and did not have one.</b> Its
     * first two entry points were the link in {@code appendBack} and one at the top of the
     * full-interpretation view - and BOTH of those are only reachable once the interpretation
     * panel is already open, which it is not when the app starts. So the feature existed,
     * every page rendered, every link resolved, and there was no way in.
     */
    public void showIndexPanel(String category) {
        if (interpretationPanel != null) {
            interpretationPanel.showIndex(category == null ? "" : category);
            revealReading();
            revalidate();
            repaint();
        }
    }

    /**
     * The body reading as HTML, without showing it anywhere.
     *
     * The selection card needs the reading's depth in its own column, and the one thing it
     * must not do is compose that prose a second time - two renderers of one corpus is how
     * surfaces drift. This hands back exactly what the reading panel would display, for the
     * card to cut into sections.
     */
    public String planetReadingHtml(String planetName, String signName, int degree,
                                    int decanNum, int houseNum,
                                    java.util.List<String[]> activeAspects, double lon) {
        if (interpretationPanel == null) {
            return "";
        }
        return interpretationPanel.generatePlanetHtml(planetName, signName, degree,
            decanNum, houseNum, activeAspects, lon);
    }

    // ============================================================ export (Section B)
    //
    // Nothing left this application before 3 Sep 2026. Each of these is a door onto
    // ChartExporter, which does the work; the window's job is only to know which component
    // holds the thing being exported.

    /** B1. The wheel as a PNG, at a chosen resolution. */
    public void exportChartImage() {
        if (skymapPanel != null) {
            ChartExporter.saveChartImage(this, skymapPanel.chartComponent());
        }
    }

    /** B2. The wheel to a printer. */
    public void printChart() {
        if (skymapPanel != null) {
            ChartExporter.printChart(this, skymapPanel.chartComponent());
        }
    }

    /** B4. The wheel to the clipboard as an image. */
    public void copyChartImage() {
        if (skymapPanel != null) {
            ChartExporter.copyChartImage(this, skymapPanel.chartComponent());
        }
    }

    /** B4. Positions to the clipboard, tab separated for a spreadsheet. */
    public void copyPositions() {
        if (skymapPanel != null) {
            ChartExporter.copyText(skymapPanel.positionsText());
        }
    }

    /** B4. Positions to a file, same data as the clipboard copy. */
    public void savePositions() {
        if (skymapPanel != null) {
            ChartExporter.saveText(this, skymapPanel.positionsText(),
                "ourania-positions.tsv", "Tab-separated values", "tsv");
        }
    }

    /** B5. The aspect grid as its own HTML - it is already a table. */
    public void exportAspectGrid() {
        if (sidePanel != null) {
            ChartExporter.saveText(this, gridHtml(),
                "ourania-aspect-grid.html", "HTML document", "html");
        }
    }

    /**
     * The text page the reader is looking at, or null when the page in front is a picture.
     *
     * <b>Export used to read the Reading panel's own pane whatever was in front</b> (reading
     * framework spec, section 7): the Dossier, the Themes, the Year and Coming up could not be
     * exported, and the Reading could be exported from any of them. The page asked is the one
     * {@link #currentPage()} names, and only a component that is itself showing in its card
     * stack counts - the Calendar holds a month grid and a year list, and only one is in front.
     */
    JEditorPane frontPane() {
        String page = currentPage();
        if (page == null) {
            return null;
        }
        if (READING_PAGE.equals(page) && interpretationPanel != null) {
            return interpretationPanel.getEditorPane();
        }
        return paneIn(pageComponent(page), true);
    }

    /** A page's text pane whatever is showing, for the suites; null for a page with none. */
    JEditorPane paneOf(String page) {
        if (READING_PAGE.equals(page) && interpretationPanel != null) {
            return interpretationPanel.getEditorPane();
        }
        return paneIn(pageComponent(page), false);
    }

    private static JEditorPane paneIn(java.awt.Component c, boolean visibleOnly) {
        if (c == null || (visibleOnly && !c.isVisible())) {
            return null;
        }
        if (c instanceof JEditorPane) {
            return (JEditorPane) c;
        }
        // The month grid is a picture. It carries a small pane for the selected day's note, and
        // exporting that would save one day while the reader is looking at a month.
        if (c instanceof TransitCalendarPanel) {
            return null;
        }
        if (c instanceof java.awt.Container) {
            for (java.awt.Component k : ((java.awt.Container) c).getComponents()) {
                JEditorPane found = paneIn(k, visibleOnly);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Said when the page in front is the month grid, which has no text to copy or save. */
    private void noText() {
        JOptionPane.showMessageDialog(this, "This view is a picture of the month, not text. "
            + "Open The year's sky for text you can copy or save.", "Ourania",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /** Show the export bar exactly while the Full report group is in front. */
    private void updateExportBar() {
        if (exportBar != null && pages != null) {
            exportBar.setVisible(REPORT_TAB.equals(pages.selectedGroup()));
        }
    }

    /** The export bar, for the suites. */
    ExportBar exportBar() {
        return exportBar;
    }

    /** B6. The page in front, as markup. */
    public void exportReadingHtml() {
        JEditorPane pane = frontPane();
        if (pane == null) {
            noText();
            return;
        }
        ChartExporter.saveText(this, pane.getText(),
            "ourania-reading.html", "HTML document", "html");
    }

    /** B6. The page in front, as plain text. */
    public void exportReadingText() {
        JEditorPane pane = frontPane();
        if (pane == null) {
            noText();
            return;
        }
        ChartExporter.saveText(this, ChartExporter.toPlainText(pane.getText()),
            "ourania-reading.txt", "Text file", "txt");
    }

    /** B4. The page in front, to the clipboard as plain text. */
    public void copyReading() {
        JEditorPane pane = frontPane();
        if (pane == null) {
            noText();
            return;
        }
        ChartExporter.copyText(ChartExporter.toPlainText(pane.getText()));
    }

    /** The page in front, to the clipboard as Markdown (the Full report's first button). */
    public void copyReadingMarkdown() {
        JEditorPane pane = frontPane();
        if (pane == null) {
            noText();
            return;
        }
        ChartExporter.copyText(ChartExporter.toMarkdown(pane.getText()));
    }

    /**
     * The Full report's third button: save the page as a PDF with the wheel, or print it.
     * The month grid is a picture, so it can only be printed, and is printed as it looks.
     */
    public void printOrSaveReading() {
        if (frontPane() == null) {
            if (calendarMonth != null && "Calendar".equals(currentPage())) {
                ChartExporter.printChart(this, calendarMonth);
            } else {
                noText();
            }
            return;
        }
        Object[] options = {"Save as PDF", "Print", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "What would you like to do with this page?",
            "Print / PDF", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
            options[0]);
        if (choice == 0) {
            exportReadingPdf();
        } else if (choice == 1) {
            printReading();
        }
    }

    /** B3. The page in front with the chart wheel, as a PDF document. */
    public void exportReadingPdf() {
        if (frontPane() != null && skymapPanel != null) {
            // <b>Asked of the subject, which is the one thing that knows.</b> label is what
            // the reader called this chart and summary() is the line a control uses to say what
            // it will draw - writing either out again here would be a second copy of a rule this
            // project has already had six copies of once.
            // The chart the reading is about, which Chart B's chip can change.
            com.zodiacomputing.ourania.astro.ChartSubject who = skymapPanel.describedSubject();
            if (who == null) {
                who = skymapPanel.chartASubject();
            }
            ChartExporter.saveReadingPdf(this, skymapPanel.chartComponent(),
                frontPane(),
                who == null ? "" : who.label,
                who == null ? "" : who.summary());
        }
    }

    /** B3, on paper. Print the page in front with the chart wheel. */
    public void printReading() {
        if (frontPane() != null && skymapPanel != null) {
            ChartExporter.printReading(this, skymapPanel.chartComponent(), frontPane());
        }
    }

    /**
     * One export, from the drawer's Export section.
     *
     * A string switch rather than nine listeners wired individually, for the same reason the
     * readings use one: the sidebar names what it wants and this decides where it comes from,
     * so a button cannot end up pointing at nothing without the name being wrong here too.
     */
    public void runExport(String what) {
        if (what == null) {
            return;
        }
        switch (what) {
            case "SAVE_IMAGE":         exportChartImage(); break;
            case "PRINT":              printChart(); break;
            case "COPY_IMAGE":         copyChartImage(); break;
            case "COPY_POSITIONS":     copyPositions(); break;
            case "SAVE_POSITIONS":     savePositions(); break;
            case "SAVE_GRID":          exportAspectGrid(); break;
            case "SAVE_READING_PDF":   exportReadingPdf(); break;
            case "PRINT_READING":      printReading(); break;
            case "BATCH_REPORTS":      BatchReportDialog.open(this); break;
            case "SAVE_READING":       exportReadingHtml(); break;
            case "SAVE_READING_TEXT":  exportReadingText(); break;
            case "COPY_READING":       copyReading(); break;
            default: break;
        }
    }

    public void handlePlacementClick(String command) {
        // The index, before anything tries to read it as a body label.
        if (command != null && (command.equals("index") || command.startsWith("index|"))) {
            showIndexPanel(command.equals("index") ? "" : command.substring(6));
            return;
        }
        // The aspect chart's pattern banner links here too. Checked before the placement
        // path, because a pattern href is not a body label and triggerPlanetInterpretation
        // would quietly do nothing with it - which is exactly how every transit cell in this
        // grid came to be unclickable for months.
        // A figure between the charts, from the Transits page: what it means (2026-10-18).
        if (command != null && command.startsWith("figure:")) {
            if (skymapPanel != null) {
                try {
                    skymapPanel.showFigure(Integer.parseInt(command.substring("figure:".length())));
                } catch (NumberFormatException e) {
                    // not a figure index; nothing to open
                }
            }
            return;
        }
        String[] pattern = InterpretationPanel.parsePatternHref(command);
        if (pattern != null) {
            if (interpretationPanel != null) {
                // <b>Onto Selection, whole and from the top.</b> The banner sits on the Chart
                // page and wrote the figure behind it, onto Interpretation. David, 2026-09-14:
                // "selection was meant to define and show what was selected", and a clicked
                // figure should bloom out there in full. Interpretation stays the reading of the
                // whole chart.
                showSelection(interpretationPanel.patternDetailHtml(pattern[0], pattern[1]), true);
            }
            return;
        }
        // The Together page's "Show details" links, for the same reason as the one below.
        if (command != null && command.startsWith(SynastryPage.DETAIL_LINK)) {
            if (skymapPanel != null) {
                skymapPanel.toggleSynastryDetail(command.substring(SynastryPage.DETAIL_LINK.length()));
            }
            return;
        }
        // A selection-card expander, before the body path: "selexp|house" is not a body
        // label and triggerPlanetInterpretation would silently do nothing with it, which is
        // how the transit grid cells were dead for months.
        if (command != null && command.startsWith(SkymapPanel.SELECT_EXPAND)) {
            if (skymapPanel != null) {
                skymapPanel.toggleSelectionSection(
                    command.substring(SkymapPanel.SELECT_EXPAND.length()));
            }
            return;
        }
        // One of the reading's own links - a glossary term, a sign, a forecast, "Turn it off" -
        // clicked where a reading is shown on the Selection page. Asked of the panel, which owns
        // that scheme, before the command is read as a body label. What it opens is itself a
        // selection and stays on that page; "back" asks for the whole chart, which is the
        // Interpretation page's.
        if (interpretationPanel != null) {
            if ("back".equals(command)) {
                interpretationPanel.followLink(command);
                revealReading();
                return;
            }
            final boolean[] known = {false};
            String html = interpretationPanel.capture(
                () -> known[0] = interpretationPanel.followLink(command));
            if (known[0]) {
                if (html != null) {
                    showSelection(html, true);
                }
                return;
            }
        }
        if (skymapPanel != null) {
            skymapPanel.triggerPlanetInterpretation(command);
        }
    }

    public void showInterpretationForSign(String signName) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showSignInterpretation(signName));
        }
    }
    
    /** Clicking the mansion ring on the wheel. */
    public void showInterpretationForMansion(int number) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showMansionDetail(number));
        }
    }

    public void showInterpretationForDecan(String signName, int decanNum) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showDecanInterpretation(signName, decanNum));
        }
    }

    /** Clicking the bounds ring: the Egyptian bound at this longitude. */
    public void showInterpretationForBound(double longitude) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showBoundInterpretation(longitude));
        }
    }

    public void showInterpretationForSabianSymbol(String signName, int degree) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showSabianInterpretation(signName, degree));
        }
    }
    
    /** The L9 snapshot paragraph for the natal chart, from SkymapPanel's Snapshot button. */
    public void showSnapshot(String when, String where, String paragraph) {
        if (interpretationPanel != null) {
            interpretationPanel.showSnapshot(when, where, paragraph);
            // <b>Open the rail on the reading, not just fill it.</b> The same fix
            // showSynthesis carries, and for the same reason: as a rail page, a reading
            // written into a shut rail is never seen, so the button reads as doing nothing.
            // It was applied to one of the three doors when the panel became a rail page and
            // not to the other two, which is why Synthesize worked and these did not.
            if (chartRail != null) {
                revealPage(READING_PAGE);
            }
            revalidate();
            repaint();
        }
    }

    /**
     * The snapshot as plain cards, onto the At a glance tab.
     *
     * <b>The tab never showed its own reading.</b> Opening it ran the snapshot, and
     * {@link #showSnapshot} wrote the result onto the Reading page and moved the rail there -
     * the behaviour from before the readings had tabs, which NavigationCheck still pins for
     * that door and which is left alone. When the tab asked, the answer goes to the tab.
     */
    public void showSnapshotCards(String when, String where, String cardsHtml) {
        if (interpretationPanel == null) {
            return;
        }
        if (pendingReadingPage != null) {
            String html = interpretationPanel.capture(
                () -> interpretationPanel.showSnapshotCards(when, where, cardsHtml));
            if (html != null && routeToReadingPage(html)) {
                return;
            }
        }
        interpretationPanel.showSnapshotCards(when, where, cardsHtml);
        if (chartRail != null) {
            revealPage(READING_PAGE);
        }
        revalidate();
        repaint();
    }

    /** The full L9 report tier, from SkymapPanel's Report button. */
    public void showReport(String when, String where, String report) {
        // <b>When the Full report tab asked, the report goes on that tab (2026-10-22)</b>, as At
        // a glance's does. It went to the Reading page and moved the rail there, which on the
        // Transits tab - where the Reading page is the tab's own - put it on a hidden page.
        if (interpretationPanel != null && pendingReadingPage != null) {
            String html = interpretationPanel.capture(
                () -> interpretationPanel.showReport(when, where, report));
            if (html != null && routeToReadingPage(html)) {
                return;
            }
        }
        if (interpretationPanel != null) {
            interpretationPanel.showReport(when, where, report);
            // <b>Open the rail on the reading, not just fill it.</b> The same fix
            // showSynthesis carries, and for the same reason: as a rail page, a reading
            // written into a shut rail is never seen, so the button reads as doing nothing.
            // It was applied to one of the three doors when the panel became a rail page and
            // not to the other two, which is why Synthesize worked and these did not.
            if (chartRail != null) {
                revealPage(READING_PAGE);
            }
            revalidate();
            repaint();
        }
    }

    /**
     * Where a revealed reading's card lands, in the layered pane's coordinates: the top of the
     * left rail, where {@link #showSynthesis} puts the reading. A rail that is shut has no
     * bounds yet, so the card lands where the rail opens - showSynthesis opens it on landing.
     */
    java.awt.Rectangle readingLanding(javax.swing.JLayeredPane lp) {
        int h = RevealPainter.CARD_H;
        if (chartRail != null && chartRail.isShowing() && chartRail.getWidth() > 60) {
            java.awt.Rectangle r = javax.swing.SwingUtilities.convertRectangle(
                chartRail.getParent(), chartRail.getBounds(), lp);
            return new java.awt.Rectangle(r.x + 8, r.y + 48, Math.max(200, r.width - 16), h);
        }
        return new java.awt.Rectangle(8, 96, 324, h);
    }

    public void showSynthesis(String html) {
        // A reading asked for by tab goes back to that tab. Anything else - a body clicked on
        // the wheel, a chart table - goes to the general Interpretation page.
        if (routeToReadingPage(html)) {
            return;
        }
        if (interpretationPanel != null) {
            interpretationPanel.showSynthesis(html);
            // <b>Open the rail on the reading, not just fill it.</b> Setting the panel
            // visible was enough when it had the east edge to itself; as a rail page, a
            // reading generated into a shut rail is written and never shown, which reads as
            // a Readings button that does nothing.
            if (chartRail != null) {
                revealPage(READING_PAGE);
            }
            revalidate();
            repaint();
        }
    }

    /**
     * Prepared HTML into the reading panel, for a caller that is not a synthesis.
     *
     * <b>Delegates rather than duplicating.</b> {@code showSynthesis} is the same door under an
     * older name - it does nothing synthesis-specific, it sets the panel's HTML and reveals it -
     * and a second copy of those four lines is exactly the one-rule-two-implementations defect
     * this project logs more than any other. Used by the wheel's Aspect Table drawer.
     */
    public void showInterpretationHtml(String html) {
        showSynthesis(html);
    }

    /**
     * One chart table, from the sidebar's Tables section. See {@link ChartTables}.
     *
     * Delegates to the wheel for the same reason {@code runReading} does: SkymapPanel holds
     * the ephemeris handle and the cast parameters, and a second copy of those here would be
     * one rule with two implementations.
     */
    public void showTable(String kind) {
        if (skymapPanel != null) {
            skymapPanel.showTable(kind);
        }
    }

    /** The annual almanac, from SkymapPanel's Calendar button. */
    public void showCalendar(int year, String body) {
        // The calendar is one of the five and has a tab of its own, so it is routed like the
        // rest rather than borrowing the reading panel. It arrives as plain text, so it is
        // escaped and wrapped rather than treated as markup.
        if (pendingReadingPage != null) {
            StringBuilder h = new StringBuilder();
            h.append("<html><body style=\"font-family:Arial; color:#E0E0E0;\">");
            h.append("<h2 style=\"color:#00BFFF;\">").append(year).append("</h2>");
            h.append("<pre style=\"font-family:Consolas,monospace; font-size:11px;\">");
            h.append(escape(body)).append("</pre></body></html>");
            if (routeToReadingPage(h.toString())) {
                return;
            }
        }
        if (interpretationPanel != null) {
            interpretationPanel.showCalendar(year, body);
            revalidate();
            repaint();
        }
    }

    /** Minimal escaping: the calendar is plain text going into a pre block. */
    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;")
            .replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Puts a finished reading on the tab that asked for it.
     *
     * @return true when it was handled here, so the caller does not also write it elsewhere
     */
    private boolean routeToReadingPage(String html) {
        if (pendingReadingPage == null) {
            return false;
        }
        javax.swing.JEditorPane pane = readingPanes.get(pendingReadingPage);
        String page = pendingReadingPage;
        pendingReadingPage = null;
        if (pane == null) {
            return false;
        }
        HtmlPanes.setHtml(pane, html);
        if (chartRail != null) {
            revealPage(page);
        }
        return true;
    }

    /**
     * The reading's Close button: puts the rail away.
     *
     * It hid the card itself, which inside the rail left the Interpretation tab opening onto a
     * blank page for the rest of the session - the card stayed hidden when the tab showed it.
     */
    /** Puts both side drawers away: a click on the chart's background (2026-10-18). */
    void closeSideDrawers() {
        if (chartRail != null) {
            chartRail.setOpen(false);
        }
        if (menuRail != null) {
            menuRail.setOpen(false);
        }
    }

    public void closeInterpretationPanel() {
        if (chartRail != null && READING_PAGE.equals(currentPage())) {
            chartRail.setOpen(false);
        }
    }

    // These two used to call cardLayout.show(contentPanel, "INTERPRETATION"), but no card
    // by that name was ever registered - the interpretation panel lives in the frame's
    // EAST region, not in contentPanel. CardLayout.show is a silent no-op for an unknown
    // name, so the text was generated and then never shown: clicking an aspect in the
    // grid, or a house on the wheel, did nothing at all unless the panel already happened
    // to be open. They open on the Selection page now, like every clicked reading.

    public void showInterpretationForHouse(int houseNum) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showHouseInterpretation(houseNum));
            // A house is a question about a part of life, so it opens the page of the parts of
            // life, at the domain that house belongs to.
            LifeAreaMapper.Domain d = LifeAreaMapper.of(houseNum);
            followClick(LIFE_AREAS_PAGE, null, d == null ? null : d.label);
        }
    }

    public void showInterpretationForAspect(String planet1, String planet2, String aspectType) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showAspectInterpretation(planet1, planet2, aspectType));
            // An aspect line is a pattern in the making: the Patterns tab is where its figure is.
            followClick(OVERVIEW_PAGE, null, null);
        }
    }

    /**
     * A cross-chart aspect between two people.
     *
     * Distinct from {@link #showInterpretationForTransitAspect} on purpose, and the two are
     * one keystroke apart in the caller - see the note there. A transit is a moment acting on
     * a chart; a synastry contact is a standing relationship between two charts, and the
     * prose, the heading and the framing all differ.
     */
    public void showInterpretationForSynastryAspect(String chartA, String chartB, String aspectType) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showSynastryAspectInterpretation(chartA, chartB, aspectType));
        }
    }

    /**
     * A reading for a contact between a moving body and the chart being read.
     *
     * @param movingWord what ring the moving body came from - "sky", "transiting", "progressed".
     *     {@code SkymapPanel.ringWord} is where that is decided; nothing here should guess it.
     * @param natalOwner what the inner wheel's body belongs to - "natal", or "Chart A's" in a
     *     synastry, where "natal" alone does not say which of the two people is meant.
     */
    public void showInterpretationForTransitAspect(String transiting, String natal,
                                                   String aspectType, String movingWord,
                                                   String natalOwner) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showTransitAspectInterpretation(
                transiting, natal, aspectType, movingWord, natalOwner));
        }
    }

    /** Changes which rings are drawn, leaving the three subjects as they are. */
    public void applyChartMode(ChartMode mode, boolean transits) {
        this.applyChartMode(mode, transits, true, true);
    }

    /** Passes what had to be assumed about a chart's time back to the form that set it. */
    public void showClockNotice(com.zodiacomputing.ourania.astro.ChartSubject a,
            com.zodiacomputing.ourania.astro.ChartSubject b) {
        if (chartSetupPanel != null) {
            chartSetupPanel.showClockNotice(a, b);
        }
    }

    /**
     * The chart a transit search reads: Chart A as cast, or the composite in a composite mode.
     *
     * The radix and not the current chart - the current chart carries the harmonic, and a
     * transit to an H5 position is a transit to a degree nothing occupies.
     */
    /** The wheel, for a view that reads its moment and place - the Sky View. */
    SkymapPanel skymapForViews() {
        return skymapPanel;
    }

    com.zodiacomputing.ourania.astro.ChartFrame radixChartForSearch() {
        return skymapPanel == null ? null : skymapPanel.radixChart();
    }

    /** Passes what the ephemeris could not do for the drawn charts back to the form. */
    public void showPrecisionNotice(java.util.List<String> notes) {
        if (chartSetupPanel != null) {
            chartSetupPanel.showPrecisionNotice(notes);
        }
    }

    /**
     * Draws a return chart around Chart A, on the wheel's outer ring - master list F5.
     *
     * <b>The wheel needs no new mode for this.</b> A return is a chart for a moment at a place, and
     * the sky ring is exactly that; handing it the return's moment and the place the return was cast
     * for gives the bi-wheel the technique asks for - the return outside, the natal inside - through
     * the same door the transport already uses. Teaching SkymapPanel a RETURN mode would mean
     * editing the decompiled wheel and the setup form to reach a picture it can already draw.
     *
     * The subject is labelled so the readout says what is on the ring rather than "Sky".
     */
    /** Where the sky is read from, from the Sky row's location field. */
    public void setSkyPlace(String place) {
        if (skymapPanel != null) {
            skymapPanel.setSkyPlace(place);
        }
    }

    void showReturnOnWheel(com.zodiacomputing.ourania.astro.Returns.Return r) {
        if (skymapPanel == null || r == null) {
            return;
        }
        java.time.ZonedDateTime when = java.time.Instant
            .ofEpochMilli(Math.round((r.jd - 2440587.5) * 86400000.0))
            .atZone(java.time.ZoneOffset.UTC);
        String label = Character.toUpperCase(r.kind.charAt(0)) + r.kind.substring(1) + " return";
        com.zodiacomputing.ourania.astro.ChartSubject sky =
            com.zodiacomputing.ourania.astro.ChartSubject.of(label, when, label, r.lat, r.lon,
                "UTC", false);
        skymapPanel.installSubjects(skymapPanel.chartASubject(), skymapPanel.chartBSubject(), sky);
        applyChartMode(ChartMode.TRANSIT, true, wheelHasChartA(), false);
        switchScreen("SKYMAP");
    }

    /** Whether the wheel is actually holding a Chart A - the entered chart, not the typing. */
    public boolean wheelHasChartA() {
        return skymapPanel != null && skymapPanel.chartASubject() != null
            && skymapPanel.chartASubject().entered();
    }

    /** Whether the wheel is actually holding a Chart B. */
    public boolean wheelHasChartB() {
        return skymapPanel != null && skymapPanel.chartBSubject() != null
            && skymapPanel.chartBSubject().entered();
    }

    /** The mode and which of the two charts are in it, so the wheel never casts a mismatch. */
    public void applyChartMode(ChartMode mode, boolean transits, boolean chartAIn,
            boolean chartBIn) {
        if (skymapPanel != null) {
            skymapPanel.applyChartMode(mode, transits, chartAIn, chartBIn);
            // A chip or the setup screen chose the chart: the chart tab follows it.
            followTheWheel();
            lightTheChartTabs();
            // <b>A chip changes who the pages are about, so the page in front must say so now.</b>
            // These pages are built when they are opened, and a chip press does not open one -
            // so Chart B could be selected while the Overview, Life areas and Reading pages
            // went on describing Chart A until the reader left and came back.
            rebuildOpenPage();
        }
    }

    /**
     * Builds one page from the chart now on the wheel.
     *
     * <b>The one place a page is made on open</b>, shared by the rail (a reader opening it) and a
     * chip (the chart under it changed). A card swap is not a door, and a page built once would
     * go on describing a chart the reader has already left.
     *
     * @param runReadings whether the Snapshot/Report/Synthesize tabs are re-run. A chip press
     *     leaves them out: the wheel re-runs the reading on screen itself, and re-running it here
     *     would replay its reveal for a reading nobody asked for.
     */
    private void rebuildPage(String page, boolean runReadings) {
        if (SKY_PAGES.contains(page)) {
            refreshSkyPages();
            return;
        }
        if (OVERVIEW_PAGE.equals(page)) {
            if (overviewPanel != null) {
                overviewPanel.refresh();
            }
            return;
        }
        if (READING_PAGE.equals(page)) {
            // A reading on this page is refreshed by the wheel; the general page is not.
            if (interpretationPanel != null && (runReadings || !skymapPanel.readingOpen())) {
                interpretationPanel.updateInterpretations();
            }
            return;
        }
        if (LIFE_AREAS_PAGE.equals(page)) {
            if (lifeAreasPanel != null) {
                lifeAreasPanel.refresh();
            }
            return;
        }
        if (TRANSITS_PAGE.equals(page)) {
            refreshTransitsPane();
            return;
        }
        if (MEET_PAGE.equals(page) || CHEMISTRY_PAGE.equals(page)
                || SYNASTRY_GLANCE_PAGE.equals(page)
                || PAIR_COMING_PAGE.equals(page)) {
            refreshTogetherPanes();
            return;
        }
        if (!runReadings) {
            return;
        }
        if ("Calendar".equals(page) && calendarMonth != null) {
            // The grid reads Chart A off the wheel, which may have changed since it was drawn.
            calendarMonth.refreshChart();
        }
        for (String[] r : READINGS) {
            if (r[0].equals(page)) {
                // Regenerated on each open. A reading is a snapshot of a moment, and the
                // moment moves whenever the transport buttons do, so a cached page would
                // quietly describe a chart that is no longer on screen.
                runReading(r[1]);
                return;
            }
        }
    }

    /** Rebuilds the page the reader is looking at, if the left rail is open on one. */
    private void rebuildOpenPage() {
        if (chartRail == null || !chartRail.isOpen()) {
            return;
        }
        String page = currentPage();
        if (page != null) {
            rebuildPage(page, false);
        }
    }

    /** Loads the saved Chart A, if there is one, and opens the wheel on the sky. At start-up. */
    public void drawSavedChart() {
        if (chartSetupPanel != null) {
            chartSetupPanel.drawSavedChartA();
            // <b>The app opens on the sky (David, 2026-10-20).</b> "the chart default should
            // always open up to the current sky". Chart A is still loaded - its chip is ready and
            // one press puts it back on the wheel - but what the reader meets first is now.
            // The same call the chips make, so the form, the chips and the wheel agree.
            chartSetupPanel.applyRings(false, false, true);
        }
        if (skymapPanel != null) {
            syncRingBar(skymapPanel.ringBarComponent());
        }
    }

    /** Where and when the sky is, from the Sky row of the setup form. */
    public void applySkySettings(String date, String time, String location,
            String zoneOverride) {
        if (skymapPanel != null) {
            skymapPanel.applySkySettings(date, time, location, zoneOverride);
        }
    }

    /** The hover card for an aspect-grid cell, or null when the cursor is not over one. */
    public String aspectHoverHtml(String href) {
        return skymapPanel != null ? skymapPanel.aspectHoverHtml(href) : null;
    }

    /** Point the wheel's highlight at an aspect-grid cell. Pass null to clear it. */
    public void highlightAspect(String href) {
        if (skymapPanel != null) {
            skymapPanel.setHighlightedAspect(href);
        }
    }

    /**
     * Light a whole aspect pattern on the wheel. Pass null or an empty list to clear it.
     *
     * Kept beside {@link #highlightAspect} because they are the same kind of thing to a
     * caller - "show me this on the chart" - and because the panel should not be reaching
     * into SkymapPanel directly for either.
     */
    public void highlightPattern(java.util.List<String> bodyNames) {
        if (skymapPanel != null) {
            skymapPanel.setHighlightedPattern(bodyNames);
        }
    }


    public static void main(String[] args) {
        // <b>First, so that anything below it is written down.</b> An exception escaping
        // a listener used to print to a console a packaged app does not have, and then
        // vanish. J8.
        ErrorLog.install();
        // <b>Then look at what this launch is actually equipped with.</b> Before this, nothing
        // verified the ephemeris directory, the data files or the settings before the window
        // opened - and the first thing it measured was that the Moon's ephemeris file has never
        // been on this machine, with the North Node quietly approximated on every chart ever
        // cast here. Off the event thread and before the window, because it reads files; it
        // never throws and it never stops the app. J9.
        final java.util.List<Startup.Finding> found = Startup.runAndRecord();
        // Run the GUI creation on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Try to use the native Windows look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            OuraniaWindow window = new OuraniaWindow();
            // Where the last session closed, or 85% of the screen the first time; and remembered
            // again on the way out. See WindowPlacement for why this is main's and not the
            // constructor's.
            WindowPlacement.restore(window);
            WindowPlacement.remember(window);
            window.setVisible(true);

            // <b>Draw the chart the reader already has.</b> The saved Chart A was restored
            // into the setup form at startup and never handed to the wheel, so the app opened
            // on a natal wheel holding this moment while the form beside it showed the real
            // birth data - the form right, the wheel wrong, and the two never introduced.
            //
            // Here rather than in the constructor, and that is the point: constructing a
            // window is not starting an application. Four check suites build windows to
            // inspect them, one of them a subclass that only records what it is asked, and a
            // constructor that generates a chart drags every one of them into casting an
            // ephemeris they never wanted. Starting the app is a thing main does.
            window.drawSavedChart();

            // <b>Said once per set of findings, not once per launch.</b> A notice that returns
            // every morning is one a reader learns to dismiss without reading, which would cost
            // exactly the case it exists for. The signature is the degraded headlines, so the
            // same problem tomorrow is silent and a new one is not; Diagnostics always has the
            // current list. J9.
            if (Startup.unannounced(found)) {
                Startup.announced(found);
                window.announce(found);
            }
        });
    }
}
