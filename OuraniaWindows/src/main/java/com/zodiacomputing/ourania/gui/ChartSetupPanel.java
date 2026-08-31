package com.zodiacomputing.ourania.gui;

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

    // Natal Chart Fields
    private JTextField baseDateField;
    private JTextField baseTimeField;
    private JTextField baseLocationField;
    
    // Transit Chart Fields
    private JComboBox<ChartMode> chartModeCombo;
    private JCheckBox transitsCheck;
    private JTextField transitDateField;
    private JTextField transitTimeField;
    private JTextField transitLocationField;

    public ChartSetupPanel(OuraniaWindow parentWindow) {
        this.parentWindow = parentWindow;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        
        JLabel titleLabel = new JLabel("Chart Setup", SwingConstants.CENTER);
        titleLabel.setForeground(new Color(220, 220, 220));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);
        
        JPanel formPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        formPanel.setBackground(Color.BLACK);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        
        // --- BASE CHART FORM ---
        JPanel basePanel = new JPanel();
        basePanel.setLayout(new BoxLayout(basePanel, BoxLayout.Y_AXIS));
        basePanel.setBackground(Color.BLACK);
        basePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel baseHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        baseHeader.setBackground(Color.BLACK);
        
        JLabel baseTitle = new JLabel("Chart A (Base)");
        baseTitle.setForeground(new Color(220, 220, 220));
        baseTitle.setFont(new Font("Arial", Font.BOLD, 18));
        baseHeader.add(baseTitle);
        
        JButton setNowBtn = new JButton("Set to Now");
        setNowBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        setNowBtn.setBackground(new Color(60, 120, 200));
        setNowBtn.setForeground(Color.WHITE);
        setNowBtn.setContentAreaFilled(false);
        setNowBtn.setOpaque(true);
        setNowBtn.setFocusPainted(false);
        setNowBtn.setMargin(new Insets(2, 5, 2, 5));
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
        // Location field is initialized later to load defaults
        
        // --- TRANSIT CHART FORM ---
        JPanel transitPanel = new JPanel();
        transitPanel.setLayout(new BoxLayout(transitPanel, BoxLayout.Y_AXIS));
        transitPanel.setBackground(Color.BLACK);
        transitPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel transitHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        transitHeader.setBackground(Color.BLACK);
        
        chartModeCombo = new JComboBox<>(ChartMode.values());
        chartModeCombo.setForeground(Color.BLACK);
        chartModeCombo.setBackground(Color.WHITE);
        chartModeCombo.setFont(new Font("Arial", Font.BOLD, 14));
        transitHeader.add(chartModeCombo);

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
        transitsCheck.setFont(new Font("Arial", Font.PLAIN, 12));
        transitsCheck.setFocusPainted(false);
        transitHeader.add(Box.createRigidArea(new Dimension(12, 0)));
        transitHeader.add(transitsCheck);




        JButton saveBtn = new JButton("Save");
        saveBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        saveBtn.setBackground(new Color(60, 120, 200));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setContentAreaFilled(false);
        saveBtn.setOpaque(true);
        saveBtn.setFocusPainted(false);
        saveBtn.setMargin(new Insets(2, 5, 2, 5));
        saveBtn.addActionListener(e -> saveTransit());

        JButton loadBtn = new JButton("Load");
        loadBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        loadBtn.setBackground(new Color(60, 120, 200));
        loadBtn.setForeground(Color.WHITE);
        loadBtn.setContentAreaFilled(false);
        loadBtn.setOpaque(true);
        loadBtn.setFocusPainted(false);
        loadBtn.setMargin(new Insets(2, 5, 2, 5));
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

        // Toggle transit fields based on combobox
        chartModeCombo.addActionListener(e -> {
            toggleTransitFields(chartModeCombo.getSelectedItem() != ChartMode.SINGLE);
            syncTransitsCheck();
        });
        toggleTransitFields(false); // Default off
        // <b>Seeded through the same method the listener calls, not by hand.</b> The Step
        // dropdown was built with a literal before its listener existed, so it displayed a
        // value the engine never used until someone changed it. One path sets this control.
        syncTransitsCheck();
        
        formPanel.add(basePanel);
        formPanel.add(transitPanel);
        
        add(formPanel, BorderLayout.CENTER);
        
        // --- BUTTONS ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 30, 0));
        
        JButton swapBtn = new JButton("⇄  Swap Natal / Transit");
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
        generateBtn.setFont(new Font("Arial", Font.BOLD, 18));
        generateBtn.setBackground(new Color(60, 120, 200));
        generateBtn.setForeground(Color.WHITE);
        generateBtn.setContentAreaFilled(false);
        generateBtn.setOpaque(true);
        generateBtn.setFocusPainted(false);
        generateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        generateBtn.addActionListener(e -> generateChart());

        buttonPanel.add(swapBtn);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(generateBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /** The small dark-on-blue buttons used across the transit header. */
    private JButton smallButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.PLAIN, 12));
        b.setBackground(new Color(60, 120, 200));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setMargin(new Insets(2, 5, 2, 5));
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
     * Puts the transits box into the only state that is true for the selected mode.
     *
     * The four modes want four different things and none of them is "whatever it said last
     * time", which is why this runs on every mode change and at construction:
     *
     * <ul>
     *   <li><b>Single</b> - there is no outer wheel at all. Off and disabled.</li>
     *   <li><b>Transit</b> - the outer wheel IS the transits, so the box is what the mode
     *       means. On and disabled: unticking it would leave a transit chart with no
     *       transits, which is a single chart under another name.</li>
     *   <li><b>Composite</b> - the outer wheel is a third moment and the composite stands
     *       perfectly well without it. <b>Enabled and off by default</b>, which is the
     *       "separate yet combinable" David asked for and a change from the old behaviour,
     *       where a composite always forced an outer wheel on.</li>
     *   <li><b>Synastry</b> - the outer wheel is the second person. Transits would be a
     *       third wheel and there is no third wheel yet, so off and disabled with the reason
     *       in the tooltip rather than pretending.</li>
     * </ul>
     */
    private void syncTransitsCheck() {
        ChartMode mode = (ChartMode) chartModeCombo.getSelectedItem();
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
    }
    
    private JTextField createField(JPanel parent, String labelText, String defaultVal) {
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(220, 220, 220));
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        parent.add(label);
        parent.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JTextField field = new JTextField(defaultVal);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        field.setBackground(Color.BLACK);
        field.setForeground(new Color(220, 220, 220));
        field.setCaretColor(new Color(220, 220, 220));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        parent.add(field);
        parent.add(Box.createRigidArea(new Dimension(0, 15)));
        return field;
    }
    
    private void generateChart() {
        String bDate = baseDateField.getText().trim();
        String bTime = baseTimeField.getText().trim();
        String bLoc = baseLocationField.getText().trim();
        
        ChartMode mode = (ChartMode) chartModeCombo.getSelectedItem();
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
            settings.setProperty("natal.date", bDate);
            settings.setProperty("natal.time", bTime);
            settings.setProperty("natal.location", bLoc);
        });
        
        parentWindow.applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc,
            transitsCheck.isSelected());
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
        boolean ok = SavedCharts.put(name, dateF.getText().trim(), timeF.getText().trim(),
            locF.getText().trim());
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
        chartModeCombo.setSelectedItem(ChartMode.TRANSIT);
        toggleTransitFields(true);
    }

    private void loadNatal() {
        SavedCharts.Entry e = pickChart("Load into the natal wheel");
        if (e == null) {
            return;
        }
        baseDateField.setText(e.date);
        baseTimeField.setText(e.time);
        baseLocationField.setText(e.location);
    }

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
        return selected == null ? null : SavedCharts.get(selected);
    }
}
