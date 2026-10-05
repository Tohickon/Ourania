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
    private OverviewPanel overviewPanel;
    private javax.swing.JEditorPane natalPane;
    private javax.swing.JEditorPane transitPane;
    private javax.swing.JEditorPane chartBPane;
    private javax.swing.JEditorPane synastryPane;
    private javax.swing.JEditorPane selectionPane;
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
    public static final String SYNASTRY_PAGE = "Synastry";
    public static final String SELECTION_PAGE = "Selection";
    /** What kind of chart this is, before any of the detail. */
    public static final String OVERVIEW_PAGE = "Overview";
    public static final String READING_PAGE = "Interpretation";

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
        {"Synthesize", "SYNTHESIZE"},
        {"Predict", "TIMELINE"},
        {"Calendar", "CALENDAR"},
    };

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
        // <b>The two charts, then what they do to each other, then the moment.</b> Chart B sits
        // beside Chart A because it is the same kind of thing - a person's chart in their own
        // houses - while Synastry is a question about the pair and Transits is a moment passing
        // over them. Read in that order a reader meets one fact at a time; with Chart B's
        // placements on the Transits page, which is where they were, the order said that a
        // second person was a kind of transit.
        chartRail.addPage(CHART_PAGE, HtmlPanes.scroller(natalPane));
        chartRail.addPage(CHART_B_PAGE, HtmlPanes.scroller(chartBPane));
        chartRail.addPage(SYNASTRY_PAGE, HtmlPanes.scroller(synastryPane));
        chartRail.addPage(TRANSITS_PAGE, HtmlPanes.scroller(transitPane));
        // <b>Selection and the reading join the chart's own side.</b> All four answer "what
        // am I looking at" - the chart, the sky over it, the body just clicked, and what that
        // means - so they belong on one edge, leaving the right for what the app can do.
        chartRail.addPage(SELECTION_PAGE, HtmlPanes.scroller(selectionPane));
        // Open on Chart at launch, without animating - the Natal section used to be the one
        // section open when the app started, and this is where that content went.
        chartRail.prepare(CHART_PAGE);
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
        contentPanel.add(skymapPanel, "SKYMAP");

        nameListPanel = new NameListPanel(skymapPanel, this);
        contentPanel.add(nameListPanel, "NAME_LIST");
        
        // <b>Before the reading, not inside it.</b> Sect, the chart's shape, the prominence
        // ranking and the transfers of light were all computed for Synthesize and appeared
        // only there - a reader who wanted to know what kind of chart this is had to open a
        // long document to find out. Added here rather than beside the other rail pages above
        // because it reads the wheel, and the wheel is built two lines up.
        overviewPanel = new OverviewPanel(skymapPanel, this);
        chartRail.addPage(OVERVIEW_PAGE, overviewPanel);

        interpretationPanel = new InterpretationPanel(skymapPanel, this);
        // <b>The rail owns this panel's visibility, and nothing else may touch it.</b> The rail
        // is a CardLayout, which shows a page by hiding every other card. Fourteen calls to
        // interpretationPanel.setVisible(true), left from when the panel was a frame region of
        // its own, kept this card visible under whichever page was chosen: on the pages added
        // after it - Snapshot through Calendar - it painted over the page and took its scroll
        // wheel, and on every page its own repaints could land on top. David, 2026-09-14:
        // "make sure the information panel on the left is scrollable it was giving me quite a
        // few glitches". NavigationCheck Part R holds the rail to one visible card.
        chartRail.addPage(READING_PAGE, interpretationPanel);
        for (String[] r : READINGS) {
            javax.swing.JEditorPane pane = HtmlPanes.chartPane(this);
            readingPanes.put(r[0], pane);
            chartRail.addPage(r[0], readingPage(r[1], HtmlPanes.scroller(pane)));
        }

        // <b>Opening a tab has to produce the page, not just reveal it.</b> The readings were
        // buttons that ran something; turned into tabs they revealed panes nothing had ever
        // written to, so five of them opened onto blank panels and Interpretation opened onto
        // an empty one. A card swap is not a door - the thing behind the door has to be made.
        chartRail.setOnSelect(page -> {
            // The same rule as the readings below: a card swap is not a door, and a page built
            // once would go on describing a chart the transport buttons have already left.
            if (OVERVIEW_PAGE.equals(page)) {
                if (overviewPanel != null) {
                    overviewPanel.refresh();
                }
                return;
            }
            if (READING_PAGE.equals(page)) {
                if (interpretationPanel != null) {
                    interpretationPanel.updateInterpretations();
                }
                return;
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
        });

        // Nothing has been clicked yet, so Selection has nothing to show.
        chartRail.setPageEnabled(SELECTION_PAGE, false);

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
                chartRail.select(READING_PAGE);
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
        chartRail.setPageLabel(CHART_PAGE, pageTitleOf(skymapPanel.natalRing.kind));
        // <b>A person is no longer one of the rings this page can hold.</b> Chart B has a page
        // of its own now, so the outer ring reaches this one only when it is a moment - and
        // {@code secondPageTitle} is left exactly as it was, because it is a general rule about
        // naming a page after the rings on it and Part P of NavigationCheck pins it. What
        // changed is which rings are handed to it.
        chartRail.setPageLabel(TRANSITS_PAGE, secondPageTitle(
            skymapPanel.outerRing.kind,
            outerReachesSecondPage(skymapPanel.outerRing.kind, skymapPanel.outerRingDrawn()),
            skymapPanel.skyRing.kind, skymapPanel.triRingDrawn()));
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
        return held.length() == 0 ? TRANSITS_PAGE : held.toString();
    }

    /**
     * A ring's own word, as a tab heading rather than as running prose.
     *
     * <b>The kind knows this now.</b> This was a switch with a default, which is a place where
     * a ring kind added later is silently called "Chart" - and one was added the very next day.
     * {@code WheelRing.Kind.heading} is a constructor argument, so the compiler asks.
     */
    static String pageTitleOf(WheelRing.Kind kind) {
        return kind == null ? CHART_PAGE : kind.heading;
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
        if (chartBPane != null) {
            HtmlPanes.setHtml(chartBPane, chartBHtml);
        }
        if (chartRail != null) {
            // Greyed without a second person, for the same reason Transits is greyed without
            // transits: a tab that opens onto nothing reads as broken rather than as empty.
            chartRail.setPageEnabled(CHART_B_PAGE, hasContent(chartBHtml));
        }
        if (synastryPane != null) {
            HtmlPanes.setHtml(synastryPane, synastryHtml);
        }
        if (chartRail != null) {
            // Greyed unless there are two people, for the reason the Transits page is greyed
            // without transits: a tab that opens onto nothing reads as broken rather than empty.
            chartRail.setPageEnabled(SYNASTRY_PAGE, hasContent(synastryHtml));
        }
        if (natalPane != null) {
            HtmlPanes.setHtml(natalPane, natalHtml);
        }
        if (transitPane != null) {
            HtmlPanes.setHtml(transitPane, transitHtml);
        }
        // <b>Greyed out on a chart drawn without transits.</b> The wheel still emits a transit
        // section for a single chart - the document wrapper with nothing in it - so the tab
        // opened onto a blank panel, which reads as broken rather than as empty. Measured on
        // the markup rather than asked of the wheel, because this method is the only thing
        // that sees the section and it already has it in hand.
        if (chartRail != null) {
            chartRail.setPageEnabled(TRANSITS_PAGE, hasContent(transitHtml));
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

    DrawerRail chartRail() {
        return chartRail;
    }

    /** Opens the left rail on a page, for a caller that needs it visible. */
    public void showChartPage(String page) {
        if (chartRail != null) {
            chartRail.reveal(page);
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
            chartRail.reveal(READING_PAGE);
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
        if (chartRail != null) {
            // There is something selected now, so the tab stops being greyed out.
            chartRail.setPageEnabled(SELECTION_PAGE, true);
            chartRail.reveal(SELECTION_PAGE);
        }
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

    /** B6. The reading on screen, as markup. */
    public void exportReadingHtml() {
        if (interpretationPanel != null) {
            ChartExporter.saveText(this, interpretationPanel.currentHtml(),
                "ourania-reading.html", "HTML document", "html");
        }
    }

    /** B6. The reading on screen, as plain text. */
    public void exportReadingText() {
        if (interpretationPanel != null) {
            ChartExporter.saveText(this,
                ChartExporter.toPlainText(interpretationPanel.currentHtml()),
                "ourania-reading.txt", "Text file", "txt");
        }
    }

    /** B4. The reading on screen, to the clipboard as plain text. */
    public void copyReading() {
        if (interpretationPanel != null) {
            ChartExporter.copyText(
                ChartExporter.toPlainText(interpretationPanel.currentHtml()));
        }
    }

    /** B3. The reading on screen with the chart wheel, as a PDF document. */
    public void exportReadingPdf() {
        if (interpretationPanel != null && skymapPanel != null) {
            // <b>Asked of the subject, which is the one thing that knows.</b> label is what
            // the reader called this chart and summary() is the line a control uses to say what
            // it will draw - writing either out again here would be a second copy of a rule this
            // project has already had six copies of once.
            com.zodiacomputing.ourania.astro.ChartSubject who = skymapPanel.chartASubject();
            ChartExporter.saveReadingPdf(this, skymapPanel.chartComponent(),
                interpretationPanel.getEditorPane(),
                who == null ? "" : who.label,
                who == null ? "" : who.summary());
        }
    }

    /** B3, on paper. Print the reading on screen with the chart wheel. */
    public void printReading() {
        if (interpretationPanel != null && skymapPanel != null) {
            ChartExporter.printReading(this, skymapPanel.chartComponent(), interpretationPanel.getEditorPane());
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
                chartRail.reveal(READING_PAGE);
            }
            revalidate();
            repaint();
        }
    }

    /** The full L9 report tier, from SkymapPanel's Report button. */
    public void showReport(String when, String where, String report) {
        if (interpretationPanel != null) {
            interpretationPanel.showReport(when, where, report);
            // <b>Open the rail on the reading, not just fill it.</b> The same fix
            // showSynthesis carries, and for the same reason: as a rail page, a reading
            // written into a shut rail is never seen, so the button reads as doing nothing.
            // It was applied to one of the three doors when the panel became a rail page and
            // not to the other two, which is why Synthesize worked and these did not.
            if (chartRail != null) {
                chartRail.reveal(READING_PAGE);
            }
            revalidate();
            repaint();
        }
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
                chartRail.reveal(READING_PAGE);
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
            chartRail.reveal(page);
        }
        return true;
    }

    /**
     * The reading's Close button: puts the rail away.
     *
     * It hid the card itself, which inside the rail left the Interpretation tab opening onto a
     * blank page for the rest of the session - the card stayed hidden when the tab showed it.
     */
    public void closeInterpretationPanel() {
        if (chartRail != null && READING_PAGE.equals(chartRail.selected())) {
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
        }
    }

    public void showInterpretationForAspect(String planet1, String planet2, String aspectType) {
        if (interpretationPanel != null) {
            selectReading(() -> interpretationPanel.showAspectInterpretation(planet1, planet2, aspectType));
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
        }
    }

    /** Draws the saved Chart A, if there is one. Called when the application starts. */
    public void drawSavedChart() {
        if (chartSetupPanel != null) {
            chartSetupPanel.drawSavedChartA();
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
