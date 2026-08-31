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

    private MainMenuPanel mainMenu;

    public OuraniaWindow() {
        setTitle("Ourania+ (Windows Edition)");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout());

        // 1. Add the Sidebar Menu (West)
        mainMenu = new MainMenuPanel(this);
        add(mainMenu, BorderLayout.WEST);

        // 2. Set up the Content Area (Center) with a CardLayout to swap screens
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.BLACK);
        add(contentPanel, BorderLayout.CENTER);

        // 3. Initialize Placeholder Screens
        initScreens();
        
        // Start by showing the Skymap screen
        switchScreen("SKYMAP");
    }

    private void initScreens() {
        // Create simple placeholder panels for now
        
        ChartSetupPanel chartSetupPanel = new ChartSetupPanel(this);
        contentPanel.add(chartSetupPanel, "SEARCH");
        
        // Add our newly ported Skymap Rendering Panel!
        skymapPanel = new SkymapPanel(this);
        contentPanel.add(skymapPanel, "SKYMAP");

        nameListPanel = new NameListPanel(skymapPanel, this);
        contentPanel.add(nameListPanel, "NAME_LIST");
        
        interpretationPanel = new InterpretationPanel(skymapPanel, this);
        interpretationPanel.setPreferredSize(new Dimension(350, 0));
        interpretationPanel.setVisible(false);
        add(interpretationPanel, BorderLayout.EAST);
        
        releasingPanel = new ReleasingPanel(this);
        contentPanel.add(releasingPanel, "RELEASING");
        
        contentPanel.add(createPlaceholder("Share..."), "SHARE");
        contentPanel.add(createPlaceholder("Syncing Data..."), "SYNC");
        contentPanel.add(createPlaceholder("Connect / Store..."), "CONNECT");
        contentPanel.add(createPlaceholder("Help Documentation..."), "HELP");

        // After skymapPanel, because applying a selection calls straight into it.
        contentPanel.add(new SettingsPanel(this), "SETTINGS");
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
        label.setFont(new Font("Arial", Font.BOLD, 24));
        
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // This method is called by the MainMenuPanel buttons
    public void switchScreen(String screenName) {
        if ("NAME_LIST".equals(screenName) && nameListPanel != null) {
            nameListPanel.updateData();
        }
        if ("INTERPRETATION".equals(screenName) && interpretationPanel != null) {
            interpretationPanel.updateInterpretations(); // The default fallback if clicked from menu
            interpretationPanel.setVisible(!interpretationPanel.isVisible());
            revalidate();
            repaint();
            return;
        }
        
        if ("RELEASING".equals(screenName) && releasingPanel != null && skymapPanel != null) {
            releasingPanel.setChart(skymapPanel.getCurrentChart());
        }
        
        cardLayout.show(contentPanel, screenName);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    public void applyChartSettings(String bDate, String bTime, String bLoc, ChartMode mode, String tDate, String tTime, String tLoc, boolean transits) {
        if (skymapPanel != null) {
            skymapPanel.applyChartSettings(bDate, bTime, bLoc, mode, tDate, tTime, tLoc, transits);
            switchScreen("SKYMAP");
        }
    }
    
    public void showInterpretationForPlanet(String planetName, String signName, int degree, int decanNum, int houseNum, java.util.List<String[]> activeAspects) {
        if (interpretationPanel != null) {
            interpretationPanel.showPlanetInterpretation(planetName, signName, degree, decanNum, houseNum, activeAspects);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void showInterpretationForAngle(String angleName, String signName, int degree, java.util.List<String[]> activeAspects) {
        if (interpretationPanel != null) {
            interpretationPanel.showAngleInterpretation(angleName, signName, degree, activeAspects);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void updatePlanetPlacements(String html) {
        if (mainMenu != null) {
            mainMenu.updatePlanetPlacements(html);
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
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
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
                interpretationPanel.showPatternDetail(pattern[0], pattern[1]);
                interpretationPanel.setVisible(true);
                revalidate();
                repaint();
            }
            return;
        }
        if (skymapPanel != null) {
            skymapPanel.triggerPlanetInterpretation(command);
        }
    }

    public void showInterpretationForSign(String signName) {
        if (interpretationPanel != null) {
            interpretationPanel.showSignInterpretation(signName);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }
    
    /** Clicking the mansion ring on the wheel. */
    public void showInterpretationForMansion(int number) {
        if (interpretationPanel != null) {
            interpretationPanel.showMansionDetail(number);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void showInterpretationForDecan(String signName, int decanNum) {
        if (interpretationPanel != null) {
            interpretationPanel.showDecanInterpretation(signName, decanNum);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void showInterpretationForSabianSymbol(String signName, int degree) {
        if (interpretationPanel != null) {
            interpretationPanel.showSabianInterpretation(signName, degree);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }
    
    /** The L9 snapshot paragraph for the natal chart, from SkymapPanel's Snapshot button. */
    public void showSnapshot(String when, String where, String paragraph) {
        if (interpretationPanel != null) {
            interpretationPanel.showSnapshot(when, where, paragraph);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    /** The full L9 report tier, from SkymapPanel's Report button. */
    public void showReport(String when, String where, String report) {
        if (interpretationPanel != null) {
            interpretationPanel.showReport(when, where, report);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void showSynthesis(String html) {
        if (interpretationPanel != null) {
            interpretationPanel.showSynthesis(html);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    /** The annual almanac, from SkymapPanel's Calendar button. */
    public void showCalendar(int year, String body) {
        if (interpretationPanel != null) {
            interpretationPanel.showCalendar(year, body);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void closeInterpretationPanel() {
        if (interpretationPanel != null) {
            interpretationPanel.setVisible(false);
            revalidate();
            repaint();
        }
    }

    // These two used to call cardLayout.show(contentPanel, "INTERPRETATION"), but no card
    // by that name was ever registered - the interpretation panel lives in the frame's
    // EAST region, not in contentPanel. CardLayout.show is a silent no-op for an unknown
    // name, so the text was generated and then never shown: clicking an aspect in the
    // grid, or a house on the wheel, did nothing at all unless the panel already happened
    // to be open. Every other handler here calls setVisible(true); these now match.

    public void showInterpretationForHouse(int houseNum) {
        if (interpretationPanel != null) {
            interpretationPanel.showHouseInterpretation(houseNum);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void showInterpretationForAspect(String planet1, String planet2, String aspectType) {
        if (interpretationPanel != null) {
            interpretationPanel.showAspectInterpretation(planet1, planet2, aspectType);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
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
            interpretationPanel.showSynastryAspectInterpretation(chartA, chartB, aspectType);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
        }
    }

    public void showInterpretationForTransitAspect(String transiting, String natal, String aspectType) {
        if (interpretationPanel != null) {
            interpretationPanel.showTransitAspectInterpretation(transiting, natal, aspectType);
            interpretationPanel.setVisible(true);
            revalidate();
            repaint();
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
        // Run the GUI creation on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Try to use the native Windows look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            OuraniaWindow window = new OuraniaWindow();
            window.setVisible(true);
        });
    }
}
