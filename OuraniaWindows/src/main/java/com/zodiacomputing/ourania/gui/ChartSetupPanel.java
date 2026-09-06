package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Rodden;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ChartSetupPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Where you normally are. Seeds both location fields; changed with "Set Home". */
    private String homeLocation = "Los Angeles, CA";

    private OuraniaWindow parentWindow;
    /** The saved-chart directory, living beside the forms it fills. */
    private ProfileListPanel profiles;

    // Natal Chart Fields
    private JTextField baseDateField;
    private JTextField baseTimeField;
    private JTextField baseLocationField;
    
    // Transit Chart Fields
    private JCheckBox transitsCheck;
    private JTextField transitDateField;
    private JTextField transitTimeField;
    private JTextField transitLocationField;
    /**
     * Where a midpoint composite's houses are derived. Blank means the couple's own
     * geographic midpoint, which is the default and what the app did before this existed.
     */
    private JTextField compositeRefField;

    /**
     * How well Chart A's birth time is known, on Lois Rodden's scale.
     *
     * <b>Most charts people actually have are not AA.</b> Nothing in this app recorded the
     * difference, so a time from a birth certificate and a time somebody half-remembers
     * produced charts that looked identical and invited the same confidence. Choosing X casts
     * the chart for noon and withholds the angles, which is the only entry on the scale that
     * changes the calculation rather than the reading.
     */
    private JComboBox<Rodden> baseRodden;

    /**
     * A time zone chosen by hand, overriding the one inferred from the place.
     *
     * <b>Because the geocoder is a guess.</b> Ambiguous place names - a dozen Springfields,
     * Boston in two countries - resolve to the wrong zone often enough to matter, and an hour
     * of error is about fifteen degrees of Ascendant. The first entry keeps the inferred zone,
     * which is right nearly always and stays the default.
     */
    private JComboBox<String> baseZone;

    /**
     * Where to recast the houses, leaving the moment alone. Blank means the birthplace.
     *
     * A relocated chart is the standard technique for "what does this place do to me" - the
     * planets are untouched and the angles move, which is the whole content of the reading.
     */
    private JTextField relocateField;

    /**
     * Step 1 of the setup drawer: who the session is about.
     *
     * <b>A separate field from the mode rather than derived from it</b>, because two subjects
     * legitimately share one {@link ChartMode}: "Myself / Natal" and "The Sky Now" are both
     * {@code SINGLE}, differing only in what the base fields were filled with. Deriving the
     * subject from the mode would make the Sky Now button un-highlight itself the moment it
     * was pressed.
     */
    private enum Subject {
        MYSELF("Myself", ChartMode.SINGLE),
        PARTNERSHIP("Us / Partnership", ChartMode.SYNASTRY),
        SKY_NOW("The Sky Now", ChartMode.SINGLE);

        final String label;
        /** Where this subject lands when it is first chosen. */
        final ChartMode fallback;

        Subject(String label, ChartMode fallback) {
            this.label = label;
            this.fallback = fallback;
        }
    }

    /**
     * The selected mode, and <b>the only copy of it.</b>
     *
     * This replaced a five-entry {@code JComboBox} that listed every {@link ChartMode} flat.
     * Everything that used to read {@code chartModeCombo.getSelectedItem()} reads this, and
     * <b>{@link #setMode} is the single path that writes it</b> - the panel has shipped the
     * other arrangement once, in the Step dropdown, where a control was seeded with a literal
     * beside a listener that never ran and so displayed a value the engine did not have.
     */
    private ChartMode selectedMode = ChartMode.SINGLE;

    private Subject subject = Subject.MYSELF;

    /**
     * The engine each subject was last left on.
     *
     * <b>Real memory, not inferred from {@link #selectedMode}.</b> The first version worked
     * out what to return to by looking at the current mode - if it was a partnership mode,
     * keep it - which cannot work, because leaving Us for Myself has already overwritten the
     * mode with a one-person one. Going Us/Davison -> Myself -> Us landed back on Synastry,
     * and the only visible sign would have been a different chart on Generate.
     * {@code ChartSetupCheck} Part A caught it and now pins it.
     */
    private final java.util.Map<Subject, ChartMode> lastEngine =
        new java.util.EnumMap<>(Subject.class);

    private final java.util.Map<Subject, JButton> subjectButtons =
        new java.util.EnumMap<>(Subject.class);
    private final java.util.Map<ChartMode, JButton> engineButtons =
        new java.util.EnumMap<>(ChartMode.class);

    /** Step 2, one card per subject; the card shown is what that subject can choose. */
    private CardLayout engineLayout;
    private JPanel engineCards;

    /** Names the right-hand column for what it currently holds - a partner, or the sky. */
    private JLabel transitTitle;

    /** Chart A's contents while The Sky Now is borrowing those fields. Null otherwise. */
    private String[] stashedBase;

    public ChartSetupPanel(OuraniaWindow parentWindow) {
        this.parentWindow = parentWindow;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        
        JLabel titleLabel = new JLabel("Chart Setup", SwingConstants.CENTER);
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setFont(Theme.TITLE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setBackground(Theme.BG);
        northPanel.add(titleLabel);
        northPanel.add(buildStepChooser());
        add(northPanel, BorderLayout.NORTH);
        
        JPanel formPanel = new JPanel(new GridLayout(1, 2, Theme.GAP_L, 0));
        formPanel.setBackground(Theme.BG);
        formPanel.setBorder(Theme.pad(Theme.GAP, Theme.GAP_L, Theme.GAP_L, Theme.GAP_L));
        
        // --- BASE CHART FORM ---
        JPanel basePanel = new JPanel();
        basePanel.setLayout(new BoxLayout(basePanel, BoxLayout.Y_AXIS));
        basePanel.setBackground(Theme.SURFACE);
        basePanel.setBorder(Theme.card(Theme.EDGE, Theme.GAP_L));
        
        // <b>Capped to its own height, and this is what the dead band above the fields was.</b>
        // BoxLayout lays its children out along the axis using their MAXIMUM size, and a
        // JPanel's default maximum is unbounded - so this header row stretched to fill the
        // whole card and shoved the fields to the bottom. It reads as a rendering fault and is
        // the same root cause as the wheel's control strip clipping its last dropdown: in both
        // places the maximum, not the preferred, is the number that binds.
        JPanel baseHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        baseHeader.setBackground(Theme.SURFACE);
        
        JLabel baseTitle = new JLabel("Chart A (Base)");
        baseTitle.setForeground(Theme.TEXT);
        baseTitle.setFont(Theme.TITLE);
        baseHeader.add(baseTitle);
        
        JButton setNowBtn = new JButton("Set to Now");
        Widgets.styleButton(setNowBtn, Widgets.Role.TRANSPORT);
        setNowBtn.setFont(Theme.SMALL);
        setNowBtn.setBorder(Theme.pad(3, 8, 3, 8));
        setNowBtn.addActionListener(e -> setBaseToNow());
        
        JPanel baseFiller = new JPanel();
        baseFiller.setBackground(Color.BLACK);
        baseFiller.setPreferredSize(new Dimension(15, 10));
        baseHeader.add(baseFiller);
        baseHeader.add(setNowBtn);

        // Save/Load on this side too. The natal chart is the one worth keeping - it is
        // birth data, entered once - and until now only the transit side could store one.
        JButton baseSaveBtn = smallButton("Save");
        baseSaveBtn.setToolTipText("Save this natal chart to the shared chart book");
        baseSaveBtn.addActionListener(e -> saveNatal());

        JButton baseLoadBtn = smallButton("Load");
        baseLoadBtn.setToolTipText("Load a saved chart into the natal wheel");
        baseLoadBtn.addActionListener(e -> loadNatal());

        JPanel bFiller2 = new JPanel();
        bFiller2.setBackground(Color.BLACK);
        bFiller2.setPreferredSize(new Dimension(5, 10));
        baseHeader.add(bFiller2);
        baseHeader.add(baseSaveBtn);

        JPanel bFiller3 = new JPanel();
        bFiller3.setBackground(Color.BLACK);
        bFiller3.setPreferredSize(new Dimension(5, 10));
        baseHeader.add(bFiller3);
        baseHeader.add(baseLoadBtn);

        basePanel.add(baseHeader);
        basePanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        baseDateField = createField(basePanel, "Date (YYYY-MM-DD):", "1990-01-01");
        baseTimeField = createField(basePanel, "Time (HH:MM):", "12:00");
        baseRodden = roddenBox(basePanel, baseTimeField);
        baseZone = zoneBox(basePanel);
        relocateField = createField(basePanel, "Relocate to (blank = birthplace):", "");
        relocateField.setToolTipText("<html><b>The same birth moment, seen from somewhere "
            + "else.</b><br>Every planet stays in the degree it was in - they depend on time "
            + "alone.<br>The Ascendant, Midheaven and all twelve houses are recast for the new "
            + "place.<br><i>The birth time is not re-read in the new zone: relocating moves "
            + "where you were, not when.</i></html>");
        // Location field is initialized later to load defaults
        
        // --- TRANSIT CHART FORM ---
        JPanel transitPanel = new JPanel();
        transitPanel.setLayout(new BoxLayout(transitPanel, BoxLayout.Y_AXIS));
        transitPanel.setBackground(Theme.SURFACE);
        transitPanel.setBorder(Theme.card(Theme.EDGE, Theme.GAP_L));
        
        // <b>Capped to its own height, and this is what the dead band above the fields was.</b>
        // BoxLayout lays its children out along the axis using their MAXIMUM size, and a
        // JPanel's default maximum is unbounded - so this header row stretched to fill the
        // whole card and shoved the fields to the bottom. It reads as a rendering fault and is
        // the same root cause as the wheel's control strip clipping its last dropdown: in both
        // places the maximum, not the preferred, is the number that binds.
        JPanel transitHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        transitHeader.setBackground(Theme.SURFACE);
        
        // The mode control used to live here as a flat five-entry combo. It is now the
        // two-step subject/engine chooser at the top of the panel - see buildStepChooser -
        // and this column just says what it is currently holding.
        transitTitle = new JLabel("Chart B (Transit)");
        transitTitle.setForeground(Theme.TEXT);
        transitTitle.setFont(Theme.TITLE);
        transitHeader.add(transitTitle);

        // Transits as an explicit choice rather than something a chart mode implies.
        //
        // <b>It is disabled where it would not do anything, and that is deliberate.</b> A
        // composite draws its outer wheel from a third moment, so transits are a real option
        // there; synastry's outer wheel is already the second person, and putting transits on
        // top of that needs a third wheel this app does not draw yet. A checkbox that sits
        // enabled and changes nothing is the Step dropdown defect again - a control saying one
        // thing while the engine does another - so it is greyed with a reason instead.
        transitsCheck = new JCheckBox("Include transits");
        transitsCheck.setForeground(Color.WHITE);
        transitsCheck.setBackground(Color.BLACK);
        transitsCheck.setFont(Theme.BODY);
        transitsCheck.setFocusPainted(false);
        transitHeader.add(Box.createRigidArea(new Dimension(12, 0)));
        transitHeader.add(transitsCheck);




        JButton saveBtn = new JButton("Save");
        Widgets.styleButton(saveBtn, Widgets.Role.TRANSPORT);
        saveBtn.setFont(Theme.SMALL);
        saveBtn.setBorder(Theme.pad(3, 8, 3, 8));
        saveBtn.addActionListener(e -> saveTransit());

        JButton loadBtn = new JButton("Load");
        Widgets.styleButton(loadBtn, Widgets.Role.TRANSPORT);
        loadBtn.setFont(Theme.SMALL);
        loadBtn.setBorder(Theme.pad(3, 8, 3, 8));
        loadBtn.addActionListener(e -> loadTransit());

        JPanel tFiller = new JPanel();
        tFiller.setBackground(Color.BLACK);
        tFiller.setPreferredSize(new Dimension(10, 10));
        transitHeader.add(tFiller);
        transitHeader.add(saveBtn);
        
        JButton nowBtn = smallButton("Now");
        nowBtn.setToolTipText("Set to the current time at the transit location");
        nowBtn.addActionListener(e -> setTransitToNow());

        JButton homeBtn = smallButton("Set Home");
        homeBtn.setToolTipText("Remember this location as where you normally are");
        homeBtn.addActionListener(e -> setHomeLocation());

        JPanel tFiller2 = new JPanel();
        tFiller2.setBackground(Color.BLACK);
        tFiller2.setPreferredSize(new Dimension(5, 10));
        transitHeader.add(tFiller2);
        transitHeader.add(loadBtn);

        JPanel tFiller3 = new JPanel();
        tFiller3.setBackground(Color.BLACK);
        tFiller3.setPreferredSize(new Dimension(5, 10));
        transitHeader.add(tFiller3);
        transitHeader.add(nowBtn);

        JPanel tFiller4 = new JPanel();
        tFiller4.setBackground(Color.BLACK);
        tFiller4.setPreferredSize(new Dimension(5, 10));
        transitHeader.add(tFiller4);
        transitHeader.add(homeBtn);

        transitPanel.add(transitHeader);
        transitPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        transitDateField = createField(transitPanel, "Date (YYYY-MM-DD):", "");
        transitTimeField = createField(transitPanel, "Time (HH:MM):", "");
        transitLocationField = createField(transitPanel, "Location:", "");

        // The reference place for a midpoint composite. Enabled only in that mode - see
        // syncTransitsCheck - because it means nothing in any other, and a control that
        // changes nothing is the defect this panel already had once with the Step dropdown.
        compositeRefField = createField(transitPanel,
            "Composite houses for (blank = midpoint):", "");
        compositeRefField.setToolTipText("<html>Where the relationship happens. The composite's"
            + " house cusps are derived at this place's LATITUDE.<br>Leave blank to use the"
            + " midpoint of the two birthplaces.<br><i>Longitude has no effect on a midpoint"
            + " composite - only latitude moves the cusps.</i></html>");
        compositeRefField.addActionListener(e -> applyCompositeReference());
        compositeRefField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                applyCompositeReference();
            }
        });
        
        // Load the home location from settings. It falls back to the old
        // default.transit.location key so an existing settings.properties still works.
        java.util.Properties settings = new java.util.Properties();
        java.io.File settingsFile = new java.io.File("settings.properties");
        if (settingsFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(settingsFile)) {
                settings.load(fis);
                homeLocation = settings.getProperty("home.location",
                    settings.getProperty("default.transit.location", homeLocation));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Both sides open at home rather than at whatever place was last generated,
        // which is what left the form showing one city beside another city's clock.
        baseLocationField = createField(basePanel, "Location:", homeLocation);
        transitLocationField.setText(homeLocation);

        // Restore the last natal chart. Only overwrite the placeholder defaults when a
        // real value was stored, so a fresh install still shows the empty-form hints.
        String savedNatalDate = settings.getProperty("natal.date", "");
        if (!savedNatalDate.trim().isEmpty()) {
            baseDateField.setText(savedNatalDate);
            baseTimeField.setText(settings.getProperty("natal.time", "12:00"));
            baseLocationField.setText(settings.getProperty("natal.location", homeLocation));
        }

        setTransitToNow();

        // <b>Seeded through the same method the buttons call, not by hand.</b> The Step
        // dropdown was built with a literal before its listener existed, so it displayed a
        // value the engine never used until someone changed it. One path sets this control -
        // setMode - and it is what toggles the transit fields and syncs the transits box.
        setSubject(Subject.MYSELF);
        setMode(ChartMode.SINGLE);

        // <b>Glue at the bottom of each card, which is what fixes the dead band.</b> BoxLayout
        // distributes leftover height between its children, so a column of fixed-height fields
        // in a tall card drifted to the middle with a gap above them that looked like a
        // rendering fault. Glue takes the slack, and the fields sit under their heading.
        basePanel.add(Box.createVerticalGlue());
        transitPanel.add(Box.createVerticalGlue());

        formPanel.add(basePanel);
        formPanel.add(transitPanel);
        
        // <b>The saved charts belong beside the fields they fill.</b> They were a section in
        // the menu drawer, which meant choosing whose chart to draw happened in one place and
        // entering a chart happened in another - and the drawer had to be wide enough to hold
        // a name plus two slot buttons on every row. Here, Chart A and Chart B sit next to the
        // Chart A and Chart B forms they load into, which is what those buttons mean.
        JPanel setupWithProfiles = new JPanel(new BorderLayout());
        setupWithProfiles.setBackground(Theme.BG);
        setupWithProfiles.add(formPanel, BorderLayout.CENTER);
        profiles = new ProfileListPanel(parentWindow);
        JScrollPane profileScroll = new JScrollPane(profiles);
        profileScroll.setBorder(BorderFactory.createEmptyBorder());
        profileScroll.getViewport().setBackground(Theme.BG);
        profileScroll.setPreferredSize(new Dimension(250, 0));
        profileScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Widgets.styleScrollPane(profileScroll);
        setupWithProfiles.add(profileScroll, BorderLayout.EAST);
        add(setupWithProfiles, BorderLayout.CENTER);
        
        // --- BUTTONS ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Theme.BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 30, 0));
        
        JButton swapBtn = new JButton("Swap A / B");
        swapBtn.setFont(new Font("Arial", Font.BOLD, 16));
        swapBtn.setBackground(new Color(90, 90, 100));
        swapBtn.setForeground(Color.WHITE);
        swapBtn.setContentAreaFilled(false);
        swapBtn.setOpaque(true);
        swapBtn.setFocusPainted(false);
        swapBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        swapBtn.setToolTipText("Move the natal chart into the transit slot and the transit into the natal slot");
        swapBtn.addActionListener(e -> swapBaseAndTransit());

        JButton generateBtn = new JButton("Generate Skymap");
        // The one call to action on the screen, and the only PRIMARY on it.
        Widgets.styleButton(generateBtn, Widgets.Role.PRIMARY);
        generateBtn.setFont(Theme.TITLE);
        generateBtn.setBorder(Theme.pad(10, 26, 10, 26));
        generateBtn.addActionListener(e -> generateChart());

        buttonPanel.add(swapBtn);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(generateBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------ the step chooser

    /**
     * The two-step chooser that replaced the flat {@link ChartMode} combo.
     *
     * <p>Step 1 asks who the session is about; step 2 shows only the engines that subject can
     * use. The five modes did not map onto the obvious tree without two corrections, and both
     * are load-bearing:
     *
     * <ul>
     *   <li><b>Transits are NOT a sibling of "Natal" outside the Myself branch.</b> Under
     *       Myself they honestly are the choice - {@code SINGLE} forces the transits box off
     *       and disabled, {@code TRANSIT} forces it on and disabled, so there the mode <i>is</i>
     *       the flag. Everywhere else the flag is independent:
     *       {@code SkymapPanel.outerWheelShown(mode, transits)} has drawn a sky ring around a
     *       composite since 2026-08-25, and synastry takes a third ring. So "Include transits"
     *       stays where it is, beside the second column, rather than becoming a step-2 button.
     *   <li><b>Composite is two charts, not one.</b> {@code COMPOSITE_MIDPOINT} and
     *       {@code COMPOSITE_DAVISON} are different techniques that answer different questions,
     *       so they get a button each - a single "Composite" would silently pick one. They are
     *       also not interchangeable in the form: the reference-place field applies to the
     *       midpoint only, which {@link #syncTransitsCheck} already enforces.
     * </ul>
     */
    private JPanel buildStepChooser() {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBackground(Color.BLACK);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 40, 12, 40));

        JPanel step1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        step1.setBackground(Color.BLACK);
        step1.add(stepLabel("1.  This session is about"));
        for (final Subject s : Subject.values()) {
            JButton b = segmentButton(s.label);
            b.setToolTipText(subjectHelp(s));
            b.addActionListener(e -> setSubject(s));
            subjectButtons.put(s, b);
            step1.add(b);
        }
        wrap.add(step1);

        engineLayout = new CardLayout();
        engineCards = new JPanel(engineLayout);
        engineCards.setBackground(Color.BLACK);
        engineCards.add(engineRow(ChartMode.SINGLE, ChartMode.TRANSIT), Subject.MYSELF.name());
        engineCards.add(engineRow(ChartMode.SYNASTRY, ChartMode.COMPOSITE_MIDPOINT,
            ChartMode.COMPOSITE_DAVISON), Subject.PARTNERSHIP.name());
        engineCards.add(skyNowRow(), Subject.SKY_NOW.name());
        wrap.add(engineCards);
        return wrap;
    }

    private JPanel engineRow(ChartMode... modes) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        row.setBackground(Color.BLACK);
        row.add(stepLabel("2.  Show me"));
        for (final ChartMode m : modes) {
            JButton b = segmentButton(engineLabel(m));
            b.setToolTipText(engineHelp(m));
            b.addActionListener(e -> setMode(m));
            engineButtons.put(m, b);
            row.add(b);
        }
        return row;
    }

    /**
     * The Sky Now has nothing to choose - it is one chart of one moment - so step 2 explains
     * what pressing it did rather than offering an engine button that would be the only one.
     */
    private JPanel skyNowRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        row.setBackground(Color.BLACK);
        JLabel note = stepLabel("2.  Chart A has been set to now, where you are. "
            + "Press Generate Skymap.");
        note.setToolTipText("<html><b>The Sky Now</b> needs no birth data - it is a chart of "
            + "this moment at your home location.<br>Edit Chart A if you want a different "
            + "moment or place; that simply makes it an ordinary single chart.</html>");
        row.add(note);
        return row;
    }

    private JLabel stepLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.TEXT_DIM);
        l.setFont(Theme.BODY);
        return l;
    }

    /** One segment of a segmented control. Styling is applied by {@link #restyleSegments}. */
    private JButton segmentButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.HEADING);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(5, 12, 5, 12));
        b.setBorder(BorderFactory.createLineBorder(Theme.EDGE, 1));
        return b;
    }

    /** What step 2 calls each mode. Not {@code ChartMode.label} - that names the engine flatly,
     *  and inside the partnership branch "Composite (Midpoint)" reads better split. */
    private static String engineLabel(ChartMode m) {
        switch (m) {
            case SINGLE:             return "Natal Chart";
            case TRANSIT:            return "Transits Overlay";
            case SYNASTRY:           return "Synastry";
            case COMPOSITE_MIDPOINT: return "Composite · Midpoint";
            case COMPOSITE_DAVISON:  return "Composite · Davison";
            default:                 return m.label;
        }
    }

    /**
     * Selects a subject, reveals its engines, and lands on a mode.
     *
     * <b>Returning to a subject keeps the engine you last used there</b> rather than resetting
     * to the fallback, so flipping between Myself and Us to compare does not quietly turn a
     * Davison back into a synastry.
     */
    private void setSubject(Subject s) {
        Subject previous = this.subject;
        this.subject = s;
        engineLayout.show(engineCards, s.name());

        // <b>The Sky Now overwrites Chart A, so it borrows those fields rather than taking
        // them.</b> Filling them with this moment is the point of the button - nobody should
        // have to type today's date into a form headed "birth data" - but the data it lands on
        // is birth data, entered once and not easily retyped. Stashed on the way in and put
        // back on the way out. See generateChart for the other half: a Sky Now chart is not
        // written to settings as the user's natal chart.
        if (s == Subject.SKY_NOW && previous != Subject.SKY_NOW) {
            stashedBase = new String[] {baseDateField.getText(), baseTimeField.getText(),
                baseLocationField.getText()};
            setBaseToNow();
            baseLocationField.setText(homeLocation);
        } else if (s != Subject.SKY_NOW && previous == Subject.SKY_NOW
                && stashedBase != null) {
            baseDateField.setText(stashedBase[0]);
            baseTimeField.setText(stashedBase[1]);
            baseLocationField.setText(stashedBase[2]);
            stashedBase = null;
        }

        ChartMode remembered = lastEngine.get(s);
        setMode(remembered != null && allows(s, remembered) ? remembered : s.fallback);
    }

    /**
     * Whether pressing Generate should store Chart A as the user's birth data.
     *
     * <b>A named method rather than a condition inside {@code generateChart}</b>, for the
     * reason {@code SkymapPanel.outerWheelShown} is one: a check can call it, and the rule
     * cannot then drift away from what the test believes it is. It is false for exactly one
     * subject - The Sky Now fills Chart A with the present moment, and persisting that would
     * overwrite birth data nobody can retype.
     */
    private boolean savesNatalData() {
        return subject != Subject.SKY_NOW;
    }

    /** Which engines a subject's step 2 offers. The memory is only written through this. */
    /**
     * Re-reads the saved-chart directory.
     *
     * A chart saved here lands in the store this list reads, and generating is what follows a
     * save - so a new name should appear then. The call used to live in the sidebar, which is
     * where the list used to live.
     */
    public void refreshProfiles() {
        if (profiles != null) {
            profiles.rebuild();
        }
    }

    /**
     * The time-accuracy control, and the time field it governs.
     *
     * Choosing "no time" disables the time box rather than clearing it: a half-remembered
     * time is worth keeping on screen while the reader decides, and clearing it would punish
     * an honest answer.
     */
    private JComboBox<Rodden> roddenBox(JPanel column, final JTextField timeField) {
        final JComboBox<Rodden> box = new JComboBox<>(Rodden.values());
        box.setSelectedItem(Rodden.A);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            box.getPreferredSize().height));
        box.setAlignmentX(0.0f);
        Widgets.styleCombo(box);
        Runnable sync = () -> {
            Rodden r = (Rodden) box.getSelectedItem();
            if (r == null) {
                return;
            }
            timeField.setEnabled(!r.timeUnknown());
            box.setToolTipText("<html><b>" + r.code + " &middot; " + r.label + "</b><br>"
                + r.meaning + "</html>");
        };
        box.addActionListener(e -> sync.run());
        sync.run();

        JLabel label = new JLabel("Time accuracy:");
        label.setForeground(Theme.TEXT_DIM);
        label.setFont(Theme.SMALL);
        label.setAlignmentX(0.0f);
        column.add(label);
        column.add(box);
        return box;
    }

    /** The first entry: keep whatever the place resolved to. */
    static final String ZONE_FROM_PLACE = "From the location";

    /**
     * The time-zone control.
     *
     * <b>Every zone Java knows, with the sensible answer first.</b> A list of six hundred
     * identifiers is not a control a person uses, so the default sits at the top and is what
     * the reader keeps unless they have a reason not to. Sorted, because "Africa/Abidjan"
     * first and "America/New_York" somewhere in the middle is the same list either way - but
     * only one of them can be scanned.
     */
    private JComboBox<String> zoneBox(JPanel column) {
        java.util.List<String> ids = new java.util.ArrayList<>(ZoneId.getAvailableZoneIds());
        java.util.Collections.sort(ids);
        JComboBox<String> box = new JComboBox<>();
        box.addItem(ZONE_FROM_PLACE);
        for (String id : ids) {
            box.addItem(id);
        }
        box.setSelectedItem(ZONE_FROM_PLACE);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, box.getPreferredSize().height));
        box.setAlignmentX(0.0f);
        box.setToolTipText("<html>The zone the birth time is read in.<br>"
            + "<b>" + ZONE_FROM_PLACE + "</b> uses whatever the place resolves to, which is "
            + "right nearly always.<br>Override it when the place name is ambiguous - an hour "
            + "of error moves the Ascendant about fifteen degrees.</html>");
        Widgets.styleCombo(box);

        JLabel label = new JLabel("Time zone:");
        label.setForeground(Theme.TEXT_DIM);
        label.setFont(Theme.SMALL);
        label.setAlignmentX(0.0f);
        column.add(label);
        column.add(box);
        return box;
    }

    /** The chosen zone id, or "" to keep the one the location gave. */
    String baseZoneOverride() {
        Object v = baseZone == null ? null : baseZone.getSelectedItem();
        return v == null || ZONE_FROM_PLACE.equals(v) ? "" : String.valueOf(v);
    }

    /** Chart A's time rating, for the engine and the chart book. */
    Rodden baseRodden() {
        Rodden r = baseRodden == null ? null : (Rodden) baseRodden.getSelectedItem();
        return r == null ? Rodden.A : r;
    }

    /** True for the modes in which the second chart is a person rather than a moment. */
    static boolean isRelationship(ChartMode m) {
        return m == ChartMode.SYNASTRY
            || m == ChartMode.COMPOSITE_MIDPOINT
            || m == ChartMode.COMPOSITE_DAVISON;
    }

    private static boolean allows(Subject s, ChartMode m) {
        switch (s) {
            case MYSELF:      return m == ChartMode.SINGLE || m == ChartMode.TRANSIT;
            case PARTNERSHIP: return m == ChartMode.SYNASTRY
                                  || m == ChartMode.COMPOSITE_MIDPOINT
                                  || m == ChartMode.COMPOSITE_DAVISON;
            default:          return m == ChartMode.SINGLE;
        }
    }

    /**
     * <b>The one path that changes the mode.</b> Everything the old combo listener did happens
     * here, so a mode set from a Load, from a subject change or from a button press cannot
     * leave the transit fields or the transits box describing a different chart.
     */
    private void setMode(ChartMode m) {
        this.selectedMode = m;
        // Recorded against the subject that can actually reach it, so a mode set from outside
        // the chooser cannot poison another branch's memory.
        if (allows(subject, m)) {
            lastEngine.put(subject, m);
        }
        toggleTransitFields(m != ChartMode.SINGLE);
        syncTransitsCheck();
        restyleSegments();
        updateColumnTitles();
    }

    private void restyleSegments() {
        for (java.util.Map.Entry<Subject, JButton> e : subjectButtons.entrySet()) {
            paintSegment(e.getValue(), e.getKey() == subject);
        }
        for (java.util.Map.Entry<ChartMode, JButton> e : engineButtons.entrySet()) {
            paintSegment(e.getValue(), e.getKey() == selectedMode);
        }
    }

    private void paintSegment(JButton b, boolean on) {
        b.setBackground(on ? new Color(60, 120, 200) : new Color(35, 35, 40));
        b.setForeground(on ? Color.WHITE : Theme.TEXT_DIM);
        b.setBorder(BorderFactory.createLineBorder(
            on ? new Color(120, 170, 240) : Theme.EDGE, 1));
    }

    /**
     * Names the second column for what it actually holds.
     *
     * In synastry and both composites that column is <b>the other person</b>, not a transit
     * moment - the fields were always used that way and the heading said "Transit" regardless,
     * which is the same class of defect as a control that changes nothing: a label describing
     * something other than what the engine reads.
     */
    private void updateColumnTitles() {
        boolean partner = selectedMode == ChartMode.SYNASTRY
            || selectedMode == ChartMode.COMPOSITE_MIDPOINT
            || selectedMode == ChartMode.COMPOSITE_DAVISON;
        transitTitle.setText(partner ? "Chart B (Partner)" : "Chart B (Transit)");
        transitTitle.setToolTipText(partner
            ? "The second person's birth date, time and place."
            : "The moment and place the outer wheel is drawn for.");
    }

    private static String subjectHelp(Subject s) {
        switch (s) {
            case MYSELF:
                return "<html><b>Myself</b> - one person's chart.<br>"
                    + "Needs Chart A only: your birth date, time and place.</html>";
            case PARTNERSHIP:
                return "<html><b>Us / Partnership</b> - two people compared.<br>"
                    + "Needs Chart A <i>and</i> Chart B, each with a birth date, time and "
                    + "place.<br>Then choose Synastry, Composite Midpoint or Composite "
                    + "Davison - hover each for what it does.</html>";
            default:
                return "<html><b>The Sky Now</b> - no birth data needed.<br>"
                    + "Fills Chart A with this moment at your home location and draws it as a "
                    + "single chart.</html>";
        }
    }

    /**
     * The hover text for each engine.
     *
     * <b>The two composites carry step-by-step instructions</b> because they are the pair a
     * reader cannot choose between from the name alone, and choosing wrong produces a chart
     * that looks perfectly reasonable and answers a different question. The wording is taken
     * from what the engine actually does - {@code ChartFrame.computeDavisonComposite} and the
     * midpoint path's own notes - rather than from the general literature.
     */
    private static String engineHelp(ChartMode m) {
        switch (m) {
            case SINGLE:
                return "<html><b>Natal Chart</b> - one wheel, the chart as cast.<br>"
                    + "<b>To use it:</b> fill Chart A, press Generate Skymap.</html>";
            case TRANSIT:
                return "<html><b>Transits Overlay</b> - a bi-wheel: where the planets are now "
                    + "ringed around your natal chart.<br><b>To use it:</b> fill Chart A with "
                    + "your birth data, set Chart B to the moment you want (the <b>Now</b> "
                    + "button fills it), press Generate Skymap.</html>";
            case SYNASTRY:
                return "<html><b>Synastry</b> - the two charts side by side, one ringed around "
                    + "the other, so you can read the contacts between them.<br>"
                    + "<b>Both people stay themselves</b> - nothing is averaged.<br>"
                    + "<b>To use it:</b><br>1. Fill Chart A with one person's birth data.<br>"
                    + "2. Fill Chart B with the other's.<br>3. Press Generate Skymap.<br>"
                    + "<i>Tick Include transits to wrap a third ring of the current sky around "
                    + "the pair.</i></html>";
            case COMPOSITE_MIDPOINT:
                return "<html><b>Composite (Midpoint)</b> - the relationship as a third "
                    + "entity.<br>Every planet is placed halfway between the two people's own: "
                    + "the composite Sun sits at the midpoint of the two Suns, and so on.<br>"
                    + "<b>It is not a real moment.</b> Nothing in the sky ever looked like "
                    + "this, so lunar phase, the prenatal syzygy, void-of-course and the lots "
                    + "are not shown for it.<br>"
                    + "<b>To use it:</b><br>1. Fill Chart A with one person's birth data.<br>"
                    + "2. Fill Chart B with the other's.<br>"
                    + "3. Optional - type where the relationship lives into <b>Composite "
                    + "houses for</b>. Blank uses the midpoint of the two birthplaces; only "
                    + "the latitude moves the cusps.<br>"
                    + "4. Optional - tick <b>Include transits</b> to ring it with the current "
                    + "sky.<br>5. Press Generate Skymap.<br>"
                    + "<i>Watch for an UNSTABLE flag on a body: where two planets are nearly "
                    + "opposite, the midpoint takes the shorter arc, and a few minutes' "
                    + "difference in a birth time can flip it to the far side of the "
                    + "chart.</i></html>";
            case COMPOSITE_DAVISON:
                return "<html><b>Composite (Davison)</b> - the moment between you.<br>"
                    + "A <b>real chart</b>, cast for the midpoint in time <i>and</i> space: "
                    + "the instant halfway between the two births, at the halfway place. "
                    + "Because it is a real moment, every field means what it always means - "
                    + "lunar phase, syzygy, retrogradation, all of it.<br>"
                    + "<b>To use it:</b><br>1. Fill Chart A with one person's birth data.<br>"
                    + "2. Fill Chart B with the other's.<br>"
                    + "3. Optional - tick <b>Include transits</b>.<br>"
                    + "4. Press Generate Skymap.<br>"
                    + "The <b>Composite houses for</b> box stays greyed out here: a Davison is "
                    + "cast for a real place, so its houses come from that place and there is "
                    + "nothing to choose.<br>"
                    + "<i>Midpoint and Davison are never blended and can disagree sharply. A "
                    + "midpoint composite averages two people; a Davison is a moment that sits "
                    + "between them.</i></html>";
            default:
                return null;
        }
    }

    /** The small dark-on-blue buttons used across the transit header. */
    /**
     * The small header buttons.
     *
     * <b>Through Widgets, not by hand.</b> This built its own blue and its own square fill,
     * which is why Save/Load/Now/Set Home stayed flat rectangles after every other button in
     * the app became rounded. Fourth hand-rolled button style in this file's history.
     */
    private JButton smallButton(String text) {
        JButton b = new JButton(text);
        Widgets.styleButton(b, Widgets.Role.TRANSPORT);
        b.setFont(Theme.SMALL);
        b.setBorder(Theme.pad(3, 8, 3, 8));
        return b;
    }

    /**
     * Exchanges the two columns wholesale - date, time and location together.
     *
     * The enable-bi-wheel checkbox is deliberately left as it was. Swapping is a valid
     * move with the transit side switched off (it promotes a transit chart to the base
     * slot), and silently turning the bi-wheel on would change what Generate produces.
     */
    private void swapBaseAndTransit() {
        String d = baseDateField.getText();
        String t = baseTimeField.getText();
        String l = baseLocationField.getText();

        baseDateField.setText(transitDateField.getText());
        baseTimeField.setText(transitTimeField.getText());
        baseLocationField.setText(transitLocationField.getText());

        transitDateField.setText(d);
        transitTimeField.setText(t);
        transitLocationField.setText(l);
    }

    /**
     * Fills the transit date and time with the present moment *at the transit location*.
     *
     * The old code used the machine's wall clock, so choosing a location in another zone
     * left the form claiming a local time that belonged to a different place - the chart
     * was then built for an instant hours away from the real one. The clock is seeded
     * from the system zone first so the field is never blank, then corrected once the
     * location's zone is known.
     */
    private void setTransitToNow() {
        applyTransitInstant(ZonedDateTime.now());

        final String loc = transitLocationField.getText().trim();
        if (loc.isEmpty()) {
            return;
        }
        final String seededDate = transitDateField.getText();
        final String seededTime = transitTimeField.getText();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                Geocoder.Result r = Geocoder.lookup(loc);
                return r == null ? null : r.tzId;
            }

            @Override
            protected void done() {
                try {
                    String tz = get();
                    if (tz == null) {
                        return;
                    }
                    // Leave the fields alone if they were edited while the lookup ran.
                    if (!transitDateField.getText().equals(seededDate)
                        || !transitTimeField.getText().equals(seededTime)) {
                        return;
                    }
                    applyTransitInstant(Instant.now().atZone(ZoneId.of(tz)));
                } catch (Exception ex) {
                    // An unreachable geocoder just leaves the system-clock seed in place.
                }
            }
        }.execute();
    }

    private void applyTransitInstant(ZonedDateTime when) {
        transitDateField.setText(when.format(DATE_FMT));
        transitTimeField.setText(when.format(TIME_FMT));
    }

    /** Pins the current transit location as home so both fields open there next time. */
    private void setHomeLocation() {
        String loc = transitLocationField.getText().trim();
        if (loc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a location first.",
                "Set Home", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        homeLocation = loc;
        writeSettings(settings -> settings.setProperty("home.location", loc));
        JOptionPane.showMessageDialog(this, "Home location set to:\n" + loc,
            "Set Home", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Read-modify-write of settings.properties, so callers never clobber other keys.
     *
     * The body of this moved to {@link Settings#update}; it stays as a name because a dozen
     * call sites in this file read better with it. There were three hand-written copies of
     * the load/mutate/store sequence across the GUI and they had already diverged over
     * whether a missing file was an error.
     */
    private void writeSettings(java.util.function.Consumer<Properties> mutation) {
        Settings.update(mutation);
    }
    
    private void toggleTransitFields(boolean enabled) {
        transitDateField.setEnabled(enabled);
        transitTimeField.setEnabled(enabled);
        transitLocationField.setEnabled(enabled);
    }

    /**
     * Resolve whatever is typed in the reference field and hand it to the wheel.
     *
     * <b>Blank clears back to the couple's midpoint</b> rather than being ignored, so there is
     * a way back out of a choice once made.
     *
     * <b>A name that will not geocode leaves the previous setting alone and says so.</b> The
     * alternative - silently falling back to the midpoint - would show a different chart from
     * the one the field claims, which is the failure this whole setting exists to fix.
     */
    private void applyCompositeReference() {
        String place = compositeRefField.getText().trim();
        if (place.isEmpty()) {
            parentWindow.applyCompositeReference(Double.NaN, Double.NaN, null);
            compositeRefField.setToolTipText("Using the midpoint of the two birthplaces.");
            return;
        }
        Geocoder.Result r = Geocoder.lookup(place);
        if (r == null) {
            compositeRefField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 80, 80)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            compositeRefField.setToolTipText("Could not find \"" + place
                + "\". The previous reference place is still in use.");
            return;
        }
        compositeRefField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        compositeRefField.setToolTipText(String.format(
            "Houses derived at %.4f N latitude (%s). Longitude %.4f is recorded but does not "
            + "affect a midpoint composite's cusps.", r.lat, r.name, r.lon));
        parentWindow.applyCompositeReference(r.lat, r.lon, r.name);
    }

    /**
     * Puts the transits box into the only state that is true for the selected mode.
     *
     * <b>This javadoc sat above {@code applyCompositeReference} until 2026-09-01</b>, detached
     * from the method it describes, and its synastry bullet had gone stale - it said transits
     * were disabled there, which stopped being true when the tri-wheel arrived. Reattached and
     * corrected because {@link #buildStepChooser} cites these four behaviours as the reason
     * "Include transits" is not a step-2 button.
     *
     * The modes want different things and none of them is "whatever it said last time", which
     * is why this runs on every mode change and at construction:
     *
     * <ul>
     *   <li><b>Single</b> - there is no outer wheel at all. Off and disabled.</li>
     *   <li><b>Transit</b> - the outer wheel IS the transits, so the box is what the mode
     *       means. On and disabled: unticking it would leave a transit chart with no
     *       transits, which is a single chart under another name.</li>
     *   <li><b>Composite</b> - either kind - the outer wheel is a third moment and the
     *       composite stands perfectly well without it. <b>Enabled and off by default</b>,
     *       which is the "separate yet combinable" David asked for and a change from the old
     *       behaviour, where a composite always forced an outer wheel on.</li>
     *   <li><b>Synastry</b> - the outer wheel is already the second person, so transits are a
     *       <b>third</b> ring. <b>Enabled</b>: the tri-wheel draws inner Chart A, middle
     *       Chart B, outer sky.</li>
     * </ul>
     *
     * It also owns the reference-place field, which applies to a midpoint composite only - see
     * the note at the bottom of the method.
     */
    private void syncTransitsCheck() {
        ChartMode mode = selectedMode;
        boolean composite = mode == ChartMode.COMPOSITE_MIDPOINT
            || mode == ChartMode.COMPOSITE_DAVISON;
        if (mode == ChartMode.TRANSIT) {
            transitsCheck.setSelected(true);
            transitsCheck.setEnabled(false);
            transitsCheck.setToolTipText("A transit chart is transits - this is what the mode is.");
        } else if (composite) {
            transitsCheck.setEnabled(true);
            transitsCheck.setToolTipText("Draw an outer wheel of current transits around the "
                + "composite. Off leaves the composite on its own.");
        } else if (mode == ChartMode.SYNASTRY) {
            transitsCheck.setEnabled(true);
            transitsCheck.setToolTipText("Wrap a third (sky) ring around the synastry bi-wheel. "
                + "The inner wheel is Chart A, the middle ring is Chart B, and the outer "
                + "ring shows the sky at the transit date/time.");
        } else {
            transitsCheck.setSelected(false);
            transitsCheck.setEnabled(false);
            transitsCheck.setToolTipText("A single chart has no outer wheel.");
        }

        // The reference place derives the house frame of a MIDPOINT composite only. A Davison
        // is a real chart of a real place and takes its houses from that; every other mode has
        // its own location already. Enabling it elsewhere would be a control that changes
        // nothing, which is the Step-dropdown defect this panel has had once before.
        boolean refApplies = mode == ChartMode.COMPOSITE_MIDPOINT;
        compositeRefField.setEnabled(refApplies);
        if (!refApplies) {
            compositeRefField.setToolTipText(mode == ChartMode.COMPOSITE_DAVISON
                ? "A Davison is cast for a real place - its houses come from the midpoint "
                    + "location itself, so there is nothing to choose here."
                : "Only a midpoint composite derives its houses at a reference place.");
        }
    }
    
    /**
     * One labelled field, in the app's own style rather than the toolkit's.
     *
     * <b>The label is the quiet half.</b> It was 14pt in full-strength body colour, the same
     * weight as the value beneath it, so every form row read as two equally important lines.
     * A field label names the box; the value is the content. Small and dim is the whole fix.
     *
     * The grey stock border went with it - a 1px {@code Color.GRAY} rectangle is the single
     * most toolkit-looking thing Swing draws, and there were six of them down each column.
     */
    private JTextField createField(JPanel parent, String labelText, String defaultVal) {
        JLabel label = new JLabel(labelText);
        label.setForeground(Theme.TEXT_DIM);
        label.setFont(Theme.SMALL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createRigidArea(new Dimension(0, Theme.GAP_S)));

        JTextField field = new JTextField(defaultVal);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        field.setBackground(Theme.SURFACE_3);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.ACCENT);
        field.setFont(Theme.BODY);
        field.setBorder(Theme.card(Theme.EDGE, Theme.GAP_S + 2));
        parent.add(field);
        parent.add(Box.createRigidArea(new Dimension(0, Theme.GAP_L)));
        return field;
    }
    
    private void generateChart() {
        String bDate = baseDateField.getText().trim();
        String bTime = baseTimeField.getText().trim();
        String bLoc = baseLocationField.getText().trim();
        
        ChartMode mode = selectedMode;
        String tDate = transitDateField.getText().trim();
        String tTime = transitTimeField.getText().trim();
        String tLoc = transitLocationField.getText().trim();
        
        // Remember the last-used places. home.location is deliberately not touched here:
        // generating a chart for another city should not move where you live.
        writeSettings(settings -> {
            settings.setProperty("default.base.location", bLoc);
            settings.setProperty("default.transit.location", tLoc);
            // The natal chart is birth data, not a place you happened to look at. Keeping
            // only its location meant every launch reopened on the 1990-01-01 default, so
            // any transit-to-natal reading was against a chart nobody had entered.
            //
            // <b>Not written from The Sky Now, which is the other half of the stash in
            // setSubject.</b> That subject fills Chart A with the present moment on purpose;
            // persisting it here would store today as the user's birth data and every later
            // launch would open on it. Birth data is entered once and is the one thing in
            // this form nobody can retype from memory.
            if (savesNatalData()) {
                settings.setProperty("natal.date", bDate);
                settings.setProperty("natal.time", bTime);
                settings.setProperty("natal.location", bLoc);
            }
        });
        
        // The rating is not decoration: X casts for noon and withholds the angles.
        parentWindow.applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc,
            transitsCheck.isSelected(), baseRodden().timeUnknown(), baseZoneOverride(),
            relocateField == null ? "" : relocateField.getText().trim());
    }

    /** Base-side "Set to Now", corrected to the base location's zone the same way. */
    private void setBaseToNow() {
        baseDateField.setText(ZonedDateTime.now().format(DATE_FMT));
        baseTimeField.setText(ZonedDateTime.now().format(TIME_FMT));

        final String loc = baseLocationField.getText().trim();
        if (loc.isEmpty()) {
            return;
        }
        final String seededDate = baseDateField.getText();
        final String seededTime = baseTimeField.getText();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                Geocoder.Result r = Geocoder.lookup(loc);
                return r == null ? null : r.tzId;
            }

            @Override
            protected void done() {
                try {
                    String tz = get();
                    if (tz == null) {
                        return;
                    }
                    if (!baseDateField.getText().equals(seededDate)
                        || !baseTimeField.getText().equals(seededTime)) {
                        return;
                    }
                    ZonedDateTime when = Instant.now().atZone(ZoneId.of(tz));
                    baseDateField.setText(when.format(DATE_FMT));
                    baseTimeField.setText(when.format(TIME_FMT));
                } catch (Exception ex) {
                    // Unreachable geocoder: keep the system-clock seed.
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------- the chart book
    //
    // Both sides read and write one shared store via SavedCharts, so a chart saved from
    // either wheel is loadable into either wheel. That is the shape synastry and composite
    // work needs, and it keeps the properties-file handling in one place instead of two
    // copies drifting apart.

    private void saveTransit() {
        saveChartFrom("transit", transitDateField, transitTimeField, transitLocationField);
    }

    private void saveNatal() {
        saveChartFrom("natal", baseDateField, baseTimeField, baseLocationField);
    }

    private void saveChartFrom(String which, JTextField dateF, JTextField timeF,
                               JTextField locF) {
        String name = JOptionPane.showInputDialog(this,
            "Name for this " + which + " chart:", "Save Chart", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        name = name.trim();
        if (SavedCharts.get(name) != null) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                "\"" + name + "\" already exists. Replace it?", "Save Chart",
                JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }
        // The rating is part of the chart, so it is saved with it - see SavedCharts.Entry.
        SavedCharts.Entry prior = SavedCharts.get(name);
        boolean ok = SavedCharts.put(name, dateF.getText().trim(), timeF.getText().trim(),
            locF.getText().trim(),
            "natal".equals(which) ? baseRodden() : (prior == null ? Rodden.A : prior.rodden),
            prior == null ? "" : prior.notes,
            prior == null ? "" : prior.tags);
        JOptionPane.showMessageDialog(this,
            ok ? "Saved \"" + name + "\"." : "Could not write the chart book.",
            ok ? "Saved" : "Error",
            ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    


    private void loadTransit() {
        SavedCharts.Entry e = pickChart("Load into the transit wheel");
        if (e == null) {
            return;
        }
        transitDateField.setText(e.date);
        transitTimeField.setText(e.time);
        transitLocationField.setText(e.location);
        // Through the chooser, not around it. Setting the mode alone would leave step 1 still
        // highlighting whatever subject was showing - and if that was Us / Partnership, the
        // panel would read "Chart B (Partner)" over a transit moment.
        setSubject(Subject.MYSELF);
        setMode(ChartMode.TRANSIT);
    }

    /**
     * Loads a saved chart into one of the two slots and draws it.
     *
     * <b>Routed through this panel rather than straight into the wheel</b>, because this is
     * where the form's other fields live: the wheel needs all eight values on every call, so
     * a caller that only knows a name would otherwise have to invent the rest. Loading into
     * Chart B also switches the subject to the partnership branch, since a second chart with
     * the mode left on SINGLE would be entered and then not drawn.
     */
    public void applySavedProfile(String name, boolean asPartner) {
        SavedCharts.Entry e = SavedCharts.get(name);
        if (e == null) {
            return;
        }
        if (asPartner) {
            transitDateField.setText(e.date);
            transitTimeField.setText(e.time);
            transitLocationField.setText(e.location);
            // <b>A saved profile is a person, so Chart B is a second chart and never a
            // transit moment.</b> This used to switch to synastry only from SINGLE, so
            // loading someone into Chart B while the mode was Natal & Transit left the mode
            // alone and their birth data went into the transit fields - the app then drew
            // their nativity as the sky over Chart A, which is a different claim entirely and
            // looked like a working chart. Any non-relationship mode now becomes synastry;
            // a composite is left as it is, being already a relationship reading.
            if (!isRelationship(selectedMode)) {
                setSubject(Subject.PARTNERSHIP);
                setMode(ChartMode.SYNASTRY);
            }
        } else {
            // Through the one filler, so this route and the Load button cannot disagree.
            fillChartA(e, name);
        }
        // <b>Loading fills the form; Generate draws the chart.</b> This used to generate
        // immediately, which threw the reader straight out of Chart Setup and onto the wheel
        // the instant they picked a chart - so loading Chart A and then Chart B was
        // impossible without navigating back in between, and there was no moment in which to
        // correct a birth time, set a relocation, or tick the time as unknown. The two acts
        // are separate: choosing whose chart this is, and asking for it to be drawn.
    }

    private void loadNatal() {
        SavedCharts.Entry e = pickChart("Load into the natal wheel");
        if (e == null) {
            return;
        }
        fillChartA(e, lastPicked);
    }

    /**
     * Puts a saved chart into Chart A's fields, rating included.
     *
     * <b>One method because there are two doors.</b> The Load button and the saved-chart
     * directory both fill this form, and they were filling it differently - the directory
     * restored the rating and Load did not, so the same chart opened as "no time" from one
     * route and as an ordinary A-rated chart from the other, casting angles it never had.
     * That is the two-surfaces defect this project keeps finding, in the one place where the
     * two answers are a different chart rather than a different label.
     */
    private void fillChartA(SavedCharts.Entry e, String name) {
        baseDateField.setText(e.date);
        baseTimeField.setText(e.time);
        baseLocationField.setText(e.location);
        if (baseRodden != null) {
            baseRodden.setSelectedItem(e.rodden);
            for (java.awt.event.ActionListener l : baseRodden.getActionListeners()) {
                l.actionPerformed(new java.awt.event.ActionEvent(baseRodden, 0, ""));
            }
        }
        if (parentWindow != null) {
            parentWindow.setChartName(name);
        }
    }

    /**
     * The name chosen in the last {@link #pickChart} dialog.
     *
     * The dialog picks by name and then throws the name away, returning only the dates. The
     * wheel's heading needs the name, so it is kept here rather than by asking the user to
     * choose twice.
     */
    private String lastPicked = "";

    private SavedCharts.Entry pickChart(String title) {
        java.util.List<String> names = SavedCharts.names();
        if (names.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved charts yet.", title,
                JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        String[] options = names.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(this, "Select a chart:",
            title, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (selected != null) {
            lastPicked = selected;
        }
        return selected == null ? null : SavedCharts.get(selected);
    }
}
