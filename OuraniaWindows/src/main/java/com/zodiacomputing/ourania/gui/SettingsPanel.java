package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.Bodies;

import javax.swing.*;
import java.awt.*;

/**
 * Application settings. Right now that means one thing: which points the chart shows.
 *
 * The checkboxes are generated from {@link Bodies#ALL}, not listed here. A screen that
 * hand-lists the bodies is a second registry, and it would be the copy that silently goes
 * stale - a point added to the registry would compute, draw, aspect and interpret while
 * being unreachable from the only screen that can switch it off.
 *
 * Changes apply immediately rather than behind an Apply button. Every point is computed on
 * every frame regardless of the selection, so switching one on is a repaint and nothing
 * more; making the user confirm a free operation would only invite the state where the
 * checkbox and the wheel disagree.
 *
 * This file is UTF-8 without a BOM and contains astrological glyphs, so it must be
 * compiled with -encoding UTF-8. See ourania-build-and-run.
 */
public class SettingsPanel extends JPanel {

    /** The aspect boxes, so a change to any one can write the whole set. */
    private JCheckBox[] aspectBoxRefs;

    /** True while every aspect box is being set at once. See aspectBulkButton. */
    private boolean aspectBulkUpdate;

    /**
     * True while a control is being seeded from stored settings rather than pressed.
     *
     * <b>Building this screen was writing to settings.properties.</b> Rendering it three times
     * walked the stored template Classic - Cool - Black &amp; White without anyone touching a
     * control: a combo fires on {@code setSelectedItem} as readily as on a click, and the
     * listener could not tell the difference. Seeding a control is not a user action, and the
     * guard says so explicitly rather than relying on the order the listener happens to be
     * attached in - an order any later edit could reverse without noticing.
     */
    private boolean seeding;

    /**
     * True until the constructor has finished.
     *
     * <b>Building this screen was writing to settings.properties, twice over.</b> The stored
     * colour template walked Classic - Cool - Black &amp; White across three renders, and worse,
     * the body selection was rewritten to whichever checkboxes happened to exist at the moment
     * a listener fired mid-construction - twenty-nine points reduced to five, in the user's real
     * settings file, with no action taken.
     *
     * The per-control {@code seeding} guard fixed the combos and missed the checkboxes, because
     * it guarded the places I knew about. This guards the whole of construction instead: until
     * the constructor returns, nothing here may persist anything. A settings screen that has
     * not been touched must not change settings.
     */
    private boolean constructing = true;

    /** The template chooser's row, so it can be rebuilt when the list of templates changes. */
    private JPanel templateRow;

    /** How to redraw every colour chip on this screen. See {@link #chip}. */
    private final java.util.List<Runnable> chipRefreshers = new java.util.ArrayList<>();

    private static final Color TEXT = new Color(220, 220, 220);
    private static final Color DIM = new Color(150, 150, 150);
    private static final Color ACCENT = new Color(60, 120, 200);

    private final OuraniaWindow window;
    private final JCheckBox[] boxes = new JCheckBox[Bodies.count()];
    private final JLabel status = new JLabel(" ");

    /**
     * Guards the listeners while the All/None/Defaults buttons move the boxes.
     *
     * setSelected fires the item listener, so without this a click on "None" would write
     * the file and repaint the wheel once per checkbox - 23 saves for one button, each one
     * reading a partially-updated set of boxes.
     */
    private boolean bulkUpdate;

    public SettingsPanel(OuraniaWindow window) {
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        JLabel title = new JLabel("Settings", SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 6, 0));
        // <b>A way out, which this screen did not have.</b> Settings is a CardLayout card
        // with no back control of any kind - the only exit was the side drawer's Menu, so with
        // the drawer shut a reader was simply stuck on it. Every other full-screen card in
        // this app has a Close; this one was missed because it is reached from a menu rather
        // than from the chart.
        JButton back = new JButton("Back to Chart");
        back.setToolTipText("Return to the chart wheel");
        Widgets.styleButton(back, Widgets.Role.PRIMARY);
        back.addActionListener(e -> {
            if (window != null) {
                window.switchScreen("SKYMAP");
            }
        });
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(Theme.pad(Theme.GAP_L, Theme.GAP_L, Theme.GAP, Theme.GAP_L));
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.setBackground(Theme.BG);
        backRow.add(back);
        header.add(backRow, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.BLACK);
        body.setBorder(BorderFactory.createEmptyBorder(0, 30, 20, 30));

        // <b>The chart's own dropdowns, moved here off the wheel's control strip.</b> Seven
        // labelled combos sat under the chart beside the play button - harmonic, animate
        // target, aspect filter, house alignment, pin and house system - which is a settings
        // panel laid across the bottom of the page. They are settings, so they are on the
        // settings screen; the strip kept the step interval, which is the one of the seven
        // that belongs next to a transport control.
        //
        // <b>The same components, re-parented - not rebuilt.</b> Each carries its own listener
        // wired into the wheel's state, so building second copies here would give two controls
        // for one setting and no mechanism keeping them in step. That is the defect this
        // project logs most, and the Step dropdown has already been on the wrong side of it.
        JPanel chartControls = window == null ? null : window.chartControlsPanel();
        if (chartControls != null) {
            body.add(heading("Chart Settings"));
            body.add(note("Harmonic, what the animation moves, which aspects are drawn, how "
                + "the houses are aligned, what the wheel is pinned to, and the house system."));
            chartControls.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(chartControls);
            body.add(Box.createRigidArea(new Dimension(0, 18)));
        }

        body.add(heading("Aspects Shown"));
        body.add(note("Every aspect ticked here is drawn on the wheel and listed in the "
            + "aspect grid. Unticking one removes it from both - they read the same gate, so "
            + "they cannot disagree about which aspects exist."));
        body.add(aspectBulkButtons());
        body.add(Box.createRigidArea(new Dimension(0, 6)));
        body.add(aspectBoxes());
        body.add(Box.createRigidArea(new Dimension(0, 18)));

        body.add(heading("Colour Template"));
        body.add(note("A template sets the whole chart at once - every aspect, the four "
            + "elements, the mansion ring and the wheel's own background. Choosing one clears "
            + "any individual colours picked below, so Classic is always the way back to the "
            + "chart this app has always drawn."));
        body.add(templateChooser());
        body.add(Box.createRigidArea(new Dimension(0, 18)));

        body.add(heading("Element, Ring & Wheel Colours"));
        body.add(note("Bodies take their element colour unless given one of their own below, "
            + "and the sign glyphs read these directly. The wheel chip is the ground the chart "
            + "is drawn on."));
        body.add(paletteRow());
        body.add(Box.createRigidArea(new Dimension(0, 8)));

        JCheckBox spheres = new JCheckBox("Draw planets on spheres",
            Settings.showPlanetSpheres());
        spheres.setForeground(TEXT);
        spheres.setBackground(Color.BLACK);
        spheres.setFont(Theme.BODY);
        spheres.setFocusPainted(false);
        spheres.setAlignmentX(Component.LEFT_ALIGNMENT);
        spheres.setToolTipText("<html>On: each glyph sits on a shaded bead, the way this app "
            + "has always drawn it.<br>Off: the bare glyph on the ring, with the degree ticks "
            + "and house lines visible behind it - the traditional look.</html>");
        spheres.addItemListener(e -> {
            Settings.setShowPlanetSpheres(spheres.isSelected());
            applyPalette();
        });
        body.add(spheres);

        JCheckBox planets = new JCheckBox("On the globe, draw the planets themselves",
            Settings.globePlanets());
        planets.setForeground(TEXT);
        planets.setBackground(Color.BLACK);
        planets.setFont(Theme.BODY);
        planets.setFocusPainted(false);
        planets.setAlignmentX(Component.LEFT_ALIGNMENT);
        planets.setToolTipText("<html>On: the natal bodies are drawn as themselves - a banded "
            + "Jupiter, Saturn with its rings, a Sun with a corona.<br>Off: the plain beads, "
            + "which stay easier to read with every point switched on.<br>"
            + "<i>The globe only; the flat wheel is unaffected.</i></html>");
        planets.addItemListener(e -> {
            Settings.setGlobePlanets(planets.isSelected());
            applyPalette();
        });
        body.add(planets);

        JCheckBox planetsAll = new JCheckBox("    ...on Chart B and the sky as well",
            Settings.globePlanetsAllRings());
        planetsAll.setForeground(TEXT);
        planetsAll.setBackground(Color.BLACK);
        planetsAll.setFont(Theme.BODY);
        planetsAll.setFocusPainted(false);
        planetsAll.setAlignmentX(Component.LEFT_ALIGNMENT);
        planetsAll.setToolTipText("<html>On: all three rings draw the planets, each circled in "
            + "its own chart's colour - gold for Chart A, blue for Chart B, silver for the "
            + "sky.<br>Off: only Chart A, so a partner's Jupiter cannot be mistaken for "
            + "yours.<br><i>Needs the setting above.</i></html>");
        planetsAll.setEnabled(planets.isSelected());
        planets.addItemListener(e -> planetsAll.setEnabled(planets.isSelected()));
        planetsAll.addItemListener(e -> {
            Settings.setGlobePlanetsAllRings(planetsAll.isSelected());
            applyPalette();
        });
        body.add(planetsAll);

        // <b>A shape per chart, because a tri-wheel draws three at once.</b> These say which
        // chart a body belongs to without the reader counting rings outward from the centre.
        // The checkbox above is the master switch: off, none of these are drawn.
        body.add(Box.createRigidArea(new Dimension(0, 6)));
        body.add(note("A synastry with transits puts three sets of bodies on one wheel. Give "
            + "each a different shape and whose Mars you are looking at stops being a "
            + "question about which shade of grey the bead is."));
        body.add(markerRow("This chart:", Settings.natalMarker(), Settings::setNatalMarker,
            "<html>The bodies of the chart at the centre of the wheel - yours.</html>"));
        body.add(markerRow("The other chart:", Settings.synastryMarker(),
            Settings::setSynastryMarker,
            "<html>Chart B of a synastry: the second person, on the outer wheel."
            + "<br>No other kind of chart has a second person, so no other kind uses "
            + "this.</html>"));
        body.add(markerRow("Transits:", Settings.transitMarker(), Settings::setTransitMarker,
            "<html>The sky at the transit moment - the outer wheel of a natal-and-transit "
            + "chart, and the outermost ring of a synastry tri-wheel.</html>"));
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        JCheckBox degreeLines = new JCheckBox("Draw a line from each body to its exact degree",
            Settings.showDegreeLines());
        degreeLines.setForeground(TEXT);
        degreeLines.setBackground(Color.BLACK);
        degreeLines.setFont(Theme.BODY);
        degreeLines.setFocusPainted(false);
        degreeLines.setAlignmentX(Component.LEFT_ALIGNMENT);
        degreeLines.setToolTipText("<html>Bodies are spread outward when they crowd, so a "
            + "glyph is often not sitting on the degree it names.<br>This leader line says "
            + "where it actually is. Its colour is the chip beside this box.</html>");
        degreeLines.addItemListener(e -> {
            Settings.setShowDegreeLines(degreeLines.isSelected());
            applyPalette();
        });
        JPanel degreeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        degreeRow.setBackground(Color.BLACK);
        degreeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        degreeRow.add(chip(
            () -> ChartPalette.colorOr(ChartPalette.leaderHex(null), Color.WHITE),
            hex -> ChartPalette.setLeaderColor(hex),
            "Degree line colour",
            "The colour of the line from a body to its degree. Right-click to reset.",
            null));
        degreeRow.add(degreeLines);
        body.add(degreeRow);
        body.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel ringRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ringRow.setBackground(Color.BLACK);
        ringRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ringLabel = new JLabel("Bodies sit:");
        ringLabel.setForeground(TEXT);
        ringLabel.setFont(Theme.BODY);
        final JComboBox<String> ringCombo = new JComboBox<>(Settings.BODY_RINGS);
        seeding = true;
        ringCombo.setSelectedItem(Settings.bodyRing());
        seeding = false;
        Widgets.styleCombo(ringCombo);
        // The stored value and the shown name are not the same string for every placement -
        // see Settings.bodyRingLabel. Rendering rather than renaming keeps settings files
        // that were written before the rings were reordered working.
        final javax.swing.ListCellRenderer<? super String> ringBase = ringCombo.getRenderer();
        ringCombo.setRenderer((list, value, index, sel, focus) -> ringBase.getListCellRendererComponent(
            list, value == null ? null : Settings.bodyRingLabel(String.valueOf(value)),
            index, sel, focus));
        ringCombo.setToolTipText("<html>Where the glyphs are drawn. <b>Against the ring above</b> "
            + "pushes them as far out as the natal wheel goes; <b>in the centre</b> pulls them "
            + "in, leaving the bands above them completely clear.</html>");
        ringCombo.addActionListener(e -> {
            Object picked = ringCombo.getSelectedItem();
            if (!constructing && !seeding && picked != null) {
                Settings.setBodyRing(String.valueOf(picked));
                applyPalette();
            }
        });
        ringRow.add(ringLabel);
        ringRow.add(ringCombo);
        body.add(ringRow);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        // The decan band. Visual only - see Settings.decanRing for why this is not, and
        // cannot be, a global choice of decan system.
        JPanel decanRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        decanRow.setBackground(Color.BLACK);
        decanRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel decanLabel = new JLabel("Decan ring shows:");
        decanLabel.setForeground(TEXT);
        decanLabel.setFont(Theme.BODY);
        final JComboBox<String> decanCombo = new JComboBox<>(Settings.DECAN_RINGS);
        seeding = true;
        decanCombo.setSelectedItem(Settings.decanRing());
        seeding = false;
        Widgets.styleCombo(decanCombo);
        decanCombo.setToolTipText("<html>Which scheme the decan band draws, and <b>only</b> "
            + "that band. This app carries two decan rulerships that disagree for 30 of the "
            + "36 decans, each tied to different data: <b>triplicity</b> rules the decan "
            + "prose, the <b>Chaldean face</b> rules the Golden Dawn tarot cards and the "
            + "Sabian decan ruler. Changing this moves glyphs on the wheel; it never remaps "
            + "a reading, and both rulers stay named wherever one is reported.</html>");
        decanCombo.addActionListener(e -> {
            Object picked = decanCombo.getSelectedItem();
            if (!constructing && !seeding && picked != null) {
                Settings.setDecanRing(String.valueOf(picked));
                applyPalette();
            }
        });
        decanRow.add(decanLabel);
        decanRow.add(decanCombo);
        body.add(decanRow);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        // What the outer wheel carries when a chart has one.
        JPanel outerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        outerRow.setBackground(Color.BLACK);
        outerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel outerLabel = new JLabel("Outer wheel shows:");
        outerLabel.setForeground(TEXT);
        outerLabel.setFont(Theme.BODY);
        final JComboBox<String> outerCombo = new JComboBox<>(Settings.OUTER_WHEELS);
        seeding = true;
        outerCombo.setSelectedItem(Settings.outerWheel());
        seeding = false;
        Widgets.styleCombo(outerCombo);
        outerCombo.setToolTipText("<html>What the outer ring carries when a chart has one. "
            + "<b>Transits</b> is the sky at the transit moment. <b>Progressions</b> advances "
            + "the chart one day for each year of life and draws it round the natal frame - "
            + "the houses stay natal, because a progressed bi-wheel is progressed bodies in "
            + "the birth frame. Progressed bodies are labelled and read as placements, never "
            + "as transits.</html>");
        outerCombo.addActionListener(e -> {
            Object picked = outerCombo.getSelectedItem();
            if (!constructing && !seeding && picked != null) {
                Settings.setOuterWheel(String.valueOf(picked));
                applyPalette();
            }
        });
        outerRow.add(outerLabel);
        outerRow.add(outerCombo);
        body.add(outerRow);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        // How much of the geometry becomes a line. Drawing only; nothing is dropped.
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modeRow.setBackground(Color.BLACK);
        modeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel modeLabel = new JLabel("Aspect lines:");
        modeLabel.setForeground(TEXT);
        modeLabel.setFont(Theme.BODY);
        final JComboBox<String> modeCombo = new JComboBox<>(Settings.ASPECT_MODES);
        seeding = true;
        modeCombo.setSelectedItem(Settings.aspectMode());
        seeding = false;
        Widgets.styleCombo(modeCombo);
        modeCombo.setToolTipText("<html>Which pairs get a line drawn across the wheel. "
            + "<b>Essential</b> draws the ten classical planets only. <b>Manifestation</b> adds "
            + "the four angles, where inner pressure becomes an outward event. <b>Esoteric</b> "
            + "draws everything. This is about legibility, not significance: every mode "
            + "computes the same aspects, and the ones not drawn are still in the aspect grid, "
            + "the placements and the reading.</html>");
        modeCombo.addActionListener(e -> {
            Object picked = modeCombo.getSelectedItem();
            if (!constructing && !seeding && picked != null) {
                Settings.setAspectMode(String.valueOf(picked));
                applyPalette();
            }
        });
        modeRow.add(modeLabel);
        modeRow.add(modeCombo);
        body.add(modeRow);
        body.add(Box.createRigidArea(new Dimension(0, 18)));

        body.add(heading("Chart Points"));
        body.add(note("Everything ticked here is drawn on the wheel, listed in the "
            + "placements panel and included in the aspect grid. Everything unticked is "
            + "left out of all three."));
        body.add(Box.createRigidArea(new Dimension(0, 10)));
        body.add(bulkButtons());
        body.add(Box.createRigidArea(new Dimension(0, 14)));

        boolean[] enabled = Settings.loadBodySelection();

        JPanel groups = new JPanel(new GridLayout(0, 2, 24, 14));
        groups.setBackground(Color.BLACK);
        groups.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Bodies.Group group : Bodies.Group.values()) {
            groups.add(groupPanel(group, enabled));
        }
        groups.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(groups);

        body.add(Box.createRigidArea(new Dimension(0, 16)));
        body.add(variantToggles());
        body.add(Box.createRigidArea(new Dimension(0, 8)));
        body.add(note("Aspect lines are drawn between bodies, not to the four angles. Click "
            + "an angle on the wheel to see what it currently contacts."));

        status.setForeground(DIM);
        status.setFont(new Font("Arial", Font.ITALIC, 12));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(Box.createRigidArea(new Dimension(0, 14)));
        body.add(status);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        // Three separate surfaces paint here: the scroll pane, its viewport, and the panel
        // inside. Colouring only the viewport left the pane's own white showing as a border
        // all the way round a black screen.
        scroll.setOpaque(true);
        scroll.setBackground(Color.BLACK);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.BLACK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refreshStatus();

        // Everything is built; from here a change is a real one.
        constructing = false;
    }

    private JPanel groupPanel(Bodies.Group group, boolean[] enabled) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.BLACK);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel name = new JLabel(group.title);
        name.setForeground(new Color(173, 216, 230));
        name.setFont(new Font("Arial", Font.BOLD, 14));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        name.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        panel.add(name);

        // "body style=width" rather than a div: it is the form Swing's HTML renderer
        // actually honours for wrapping a JLabel, and a div is silently ignored - which is
        // why the intro note came out as one clipped line the first time this was drawn.
        JLabel blurb = new JLabel("<html><body style='width:260px'>" + group.blurb + "</body></html>");
        blurb.setForeground(DIM);
        blurb.setFont(new Font("Arial", Font.PLAIN, 11));
        blurb.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(blurb);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            if (d.group != group) {
                continue;
            }
            final int index = i;
            // The MC's "glyph" is the text MC, so glyph-then-name would read "MC   MC".
            String label = d.glyph.equals(d.name) ? d.name : d.glyph + "   " + d.name;
            JCheckBox box = new JCheckBox(label, enabled[i]);
            box.setForeground(TEXT);
            box.setBackground(Color.BLACK);
            box.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
            box.setFocusPainted(false);
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.setToolTipText(d.meaning);
            box.addItemListener(e -> {
                if (!bulkUpdate) {
                    save();
                }
            });
            boxes[index] = box;

            // <b>The box wears the colour too.</b> A chip that recolours only the chart makes a
            // reader check the wheel after every pick; showing it on the glyph and the name
            // right here means the setting reads back where it was made.
            box.setForeground(bodyDisplayColor(index));

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setBackground(Color.BLACK);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(bodySwatch(index, box));
            row.add(box);
            panel.add(row);
        }
        return panel;
    }

    /**
     * One box per {@link Aspects.Type}, in the order the engine declares them.
     *
     * <b>Driven from the enum, never a hand-written list.</b> The wheel's legend was written by
     * hand once and had no quincunx entry for months, so the grid drew a symbol nothing
     * explained; the same list written twice here would drift the same way. A new aspect
     * constant appears in this panel automatically.
     */
    private JPanel aspectBoxes() {
        boolean[] on = Settings.loadAspectSelection();
        aspectBoxRefs = new JCheckBox[Aspects.Type.values().length];

        JPanel panel = new JPanel(new GridLayout(0, 3, 8, 2));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(Theme.card(Theme.EDGE, Theme.GAP_L));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (final Aspects.Type t : Aspects.Type.values()) {
            final JCheckBox box = new JCheckBox(t.label, on[t.ordinal()]);
            // <b>The name wears the colour the chart draws it in.</b> The list was fifteen
            // identical white labels beside a coloured wheel, which makes a reader match name
            // to line by counting rather than by looking. As on the chart, so in the list.
            box.setForeground(ChartPalette.colorFor(t.label));
            box.setBackground(Theme.SURFACE);
            box.setFont(Theme.BODY);
            box.setFocusPainted(false);
            box.setToolTipText(t.label + " - " + t.exactAngle + " degrees, 1/" + t.harmonic
                + " of the circle");
            box.addItemListener(e -> saveAspects());
            aspectBoxRefs[t.ordinal()] = box;

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setBackground(Theme.SURFACE);
            row.add(chip(() -> ChartPalette.colorFor(t.label),
                hex -> ChartPalette.setOverride(t.label, hex),
                t.label + " colour",
                "<html>The colour " + t.label + " is drawn in."
                    + "<br>Click to choose another; right-click to go back to the "
                    + "template.</html>",
                () -> box.setForeground(ChartPalette.colorFor(t.label))));
            row.add(box);
            panel.add(row);
        }
        return panel;
    }

    /**
     * The colour chip beside an aspect: shows what it is drawn in, and changes it.
     *
     * <b>Beside the checkbox rather than in a separate colour screen</b>, because the two
     * questions a reader has about an aspect are "do I want to see it" and "what does it look
     * like", and splitting those across two places means finding the same fifteen-row list
     * twice. Right-click clears the override and returns that aspect to the template, which is
     * the only way back once a colour has been picked - a chip with no way to undo it is a
     * one-way door.
     */

    /**
     * Select all / none for the aspect list.
     *
     * <b>The bodies have had these since that section was written</b> and the aspects did not,
     * which meant unticking fourteen of fifteen to look at one aspect alone - the exact thing
     * a reader wants this list for.
     */
    private JPanel aspectBulkButtons() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(aspectBulkButton("Select all", true));
        row.add(aspectBulkButton("Select none", false));
        return row;
    }

    private JButton aspectBulkButton(String text, final boolean select) {
        JButton b = new JButton(text);
        Widgets.styleButton(b, Widgets.Role.TRANSPORT);
        b.addActionListener(e -> {
            if (aspectBoxRefs == null) {
                return;
            }
            // Set every box first, then write once. Each box's own listener would otherwise
            // save fifteen times and redraw the chart on every one of them.
            aspectBulkUpdate = true;
            for (JCheckBox box : aspectBoxRefs) {
                if (box != null) {
                    box.setSelected(select);
                }
            }
            aspectBulkUpdate = false;
            saveAspects();
        });
        return b;
    }

    /**
     * The template chooser.
     *
     * <b>A dropdown rather than six buttons</b>, because the templates are mutually exclusive
     * and exactly one is always in force - which is what a dropdown says and a row of buttons
     * does not.
     */
    private JPanel templateChooser() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(Theme.SURFACE);
        row.setBorder(Theme.card(Theme.EDGE, Theme.GAP_L));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Built-ins first, then the reader's own - so a saved template is offered in the
        // same place as the ones it was built from, not in a separate list of its own.
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String t : ChartPalette.TEMPLATES) {
            names.add(t);
        }
        names.addAll(ChartPalette.userTemplateNames());
        final JComboBox<String> combo = new JComboBox<>(names.toArray(new String[0]));
        seeding = true;
        combo.setSelectedItem(ChartPalette.currentTemplate());
        seeding = false;
        Widgets.styleCombo(combo);
        combo.setToolTipText("Sets every colour in the chart at once");
        combo.addActionListener(e -> {
            Object picked = combo.getSelectedItem();
            if (constructing || seeding || picked == null
                    || picked.equals(ChartPalette.currentTemplate())) {
                return;
            }
            String name = String.valueOf(picked);
            // A saved template loads its stored colours; a built-in clears back to itself.
            if (ChartPalette.isUserTemplate(name)) {
                ChartPalette.applyUserTemplate(name);
            } else {
                ChartPalette.setTemplate(name);
            }
            // Every chip on this screen now shows a stale colour, so all of them are asked to
            // redraw before the chart is.
            for (Runnable r : chipRefreshers) {
                r.run();
            }
            applyPalette();
        });
        row.add(combo);

        JButton save = new JButton("Save as template");
        save.setToolTipText("Store every colour currently in force under a name of your own");
        Widgets.styleButton(save, Widgets.Role.PRIMARY);
        save.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(SettingsPanel.this,
                "Name for this colour template:", "Save template",
                JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) {
                return;
            }
            // <b>A built-in's name cannot be taken.</b> Saving over "Classic" would leave no
            // way back to the chart the app has always drawn, which is the one thing that
            // template is for.
            for (String builtIn : ChartPalette.TEMPLATES) {
                if (builtIn.equalsIgnoreCase(name.trim())) {
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                        builtIn + " is a built-in template - choose another name.",
                        "Name in use", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            ChartPalette.saveUserTemplate(name);
            ChartPalette.applyUserTemplate(name.trim());
            refreshTemplateChooser();
        });
        row.add(save);

        JButton delete = new JButton("Delete");
        delete.setToolTipText("Forget the selected saved template. Built-ins cannot be deleted.");
        Widgets.styleButton(delete, Widgets.Role.TRANSPORT);
        delete.addActionListener(e -> {
            Object picked = combo.getSelectedItem();
            String name = picked == null ? "" : String.valueOf(picked);
            if (!ChartPalette.isUserTemplate(name)) {
                JOptionPane.showMessageDialog(SettingsPanel.this,
                    "Only your own saved templates can be deleted.",
                    "Built-in template", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            ChartPalette.deleteUserTemplate(name);
            ChartPalette.setTemplate(ChartPalette.CLASSIC);
            refreshTemplateChooser();
            for (Runnable r : chipRefreshers) {
                r.run();
            }
            applyPalette();
        });
        row.add(delete);

        this.templateRow = row;
        return row;
    }

    /**
     * Rebuilds the chooser after the list of templates changes.
     *
     * The combo's model is built once from the names that existed then, so saving or deleting
     * one leaves it stale - a template you just saved would not be in the list you saved it
     * from.
     */
    private void refreshTemplateChooser() {
        if (templateRow == null) {
            return;
        }
        java.awt.Container parent = templateRow.getParent();
        if (parent == null) {
            return;
        }
        int index = -1;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == templateRow) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        parent.remove(index);
        parent.add(templateChooser(), index);
        parent.revalidate();
        parent.repaint();
    }

    /**
     * The chip beside one body.
     *
     * <b>A body has never had a colour of its own.</b> Every one takes its element colour -
     * four colours across twenty-nine points, so Sun and Mars have always been the same red.
     * That stays the default and the chip shows it; picking here adds an override that wins
     * over the element, and right-click gives the element back.
     */
    /**
     * One "what shape does this chart's bodies wear" row.
     *
     * <b>Built rather than written out three times.</b> The three differ only in their label,
     * their current value and where the choice is stored; three hand-written copies is where
     * the third one keeps a tooltip that describes the second one's combo.
     */
    private JPanel markerRow(String label, String current,
                             java.util.function.Consumer<String> save, String tip) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel text = new JLabel(label);
        text.setForeground(TEXT);
        text.setFont(Theme.BODY);
        final JComboBox<String> combo = new JComboBox<>(Settings.MARKER_SHAPES);
        seeding = true;
        combo.setSelectedItem(current);
        seeding = false;
        Widgets.styleCombo(combo);
        combo.setToolTipText(tip);
        combo.addActionListener(e -> {
            Object picked = combo.getSelectedItem();
            if (!constructing && !seeding && picked != null) {
                save.accept(String.valueOf(picked));
                applyPalette();
            }
        });
        row.add(text);
        row.add(combo);
        return row;
    }

    private JButton bodySwatch(final int bodyIndex, final JCheckBox box) {
        final String id = Bodies.at(bodyIndex).id;
        return chip(() -> bodyDisplayColor(bodyIndex),
            hex -> ChartPalette.setBodyOverride(id, hex),
            Bodies.at(bodyIndex).name + " colour",
            "<html>The colour " + Bodies.at(bodyIndex).name + " is drawn in, on the wheel and "
                + "in the tables.<br>Default is its element colour."
                + "<br>Click to choose; right-click to go back to the element.</html>",
            () -> box.setForeground(bodyDisplayColor(bodyIndex)));
    }

    /** What this body is currently drawn in: its own colour, else its element colour. */
    private Color bodyDisplayColor(int bodyIndex) {
        String own = ChartPalette.bodyHex(Bodies.at(bodyIndex).id);
        return own != null
            ? ChartPalette.colorOr(own, Color.WHITE)
            : elementDisplayColor(Bodies.at(bodyIndex).element);
    }

    /**
     * What the wheel currently paints this element in.
     *
     * <b>No local default list.</b> There was one here and another in SkymapPanel, and they
     * disagreed - #FF6B57 against #FF4500 and so on down all four - so these chips and the
     * names beside them advertised colours the chart never drew. The palette is the one answer
     * now; this asks it and nothing else.
     */
    private Color elementDisplayColor(int element) {
        return ChartPalette.colorOr(ChartPalette.elementHex(element, "#E2E8F0"), Color.WHITE);
    }

    /**
     * The four element colours, the mansion ring, and the wheel itself.
     *
     * The element chips are the ones that move the most at once: the sign glyphs read them
     * directly, and every body without a colour of its own falls back to them. Said in the
     * tooltip rather than left to be discovered.
     */
    private JPanel paletteRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        row.setBackground(Theme.SURFACE);
        row.setBorder(Theme.card(Theme.EDGE, Theme.GAP_L));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] names = {"Fire", "Earth", "Air", "Water"};
        for (int i = 0; i < names.length; i++) {
            final int element = i;
            row.add(labelledChip(names[i],
                () -> elementDisplayColor(element),
                hex -> ChartPalette.setElementColor(element, hex),
                names[i] + " colour",
                "<html><b>" + names[i] + "</b> - this one moves a lot at once: the sign glyphs "
                    + "read it directly, and every body without its own colour falls back to "
                    + "it.<br>Right-click to reset.</html>"));
        }
        row.add(labelledChip("Mansions",
            () -> ChartPalette.colorOr(ChartPalette.mansionHex(null), new Color(181, 160, 227)),
            hex -> ChartPalette.setMansionColor(hex),
            "Lunar mansion ring colour",
            "The band of the 28 lunar mansions. Right-click to reset."));
        row.add(labelledChip("Lines",
            () -> ChartPalette.colorOr(ChartPalette.inkHex("#DCDCDC"), Color.LIGHT_GRAY),
            hex -> ChartPalette.setInkColor(hex),
            "Chart line colour",
            "<html>The rings, spokes and centre crosshair.<br>This has to move with the wheel "
                + "background - light lines on a light ground are an invisible chart.</html>"));
        row.add(labelledChip("Page",
            () -> ChartPalette.colorOr(ChartPalette.backgroundHex(null), Color.BLACK),
            hex -> ChartPalette.setBackgroundColor(hex),
            "Page background colour",
            "Everything behind the wheel. Right-click to go back to the template."));
        row.add(labelledChip("Wheel",
            () -> ChartPalette.colorOr(ChartPalette.wheelHex(null),
                ChartPalette.colorOr(ChartPalette.backgroundHex(null), Color.BLACK)),
            hex -> ChartPalette.setWheelColor(hex),
            "Wheel disc colour",
            "<html>The disc the chart is drawn on, as distinct from the page behind it."
                + "<br>Set it a shade off the page to lift the wheel off the "
                + "background.</html>"));
        return row;
    }

    private JPanel labelledChip(String name, java.util.function.Supplier<Color> current,
                                java.util.function.Consumer<String> store,
                                String dialogTitle, String tip) {
        JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        cell.setBackground(Theme.SURFACE);
        JLabel label = new JLabel(name);
        label.setForeground(TEXT);
        label.setFont(Theme.BODY);
        cell.add(chip(current, store, dialogTitle, tip, () -> label.setForeground(TEXT)));
        cell.add(label);
        return cell;
    }

    /**
     * The shared colour chip: shows a colour, picks a new one, right-click clears.
     *
     * <b>One implementation for aspects, bodies, elements, the mansion ring and the wheel.</b>
     * Five chips written five times is how three of them end up without a right-click and the
     * fourth forgets to repaint.
     */
    private JButton chip(final java.util.function.Supplier<Color> current,
                         final java.util.function.Consumer<String> store,
                         final String dialogTitle, String tip, final Runnable after) {
        final JButton chip = new JButton();
        chip.setPreferredSize(new Dimension(16, 16));
        chip.setMaximumSize(new Dimension(16, 16));
        Widgets.styleSwatch(chip, current.get());
        chip.setCursor(new Cursor(Cursor.HAND_CURSOR));
        chip.setToolTipText(tip);
        // <b>Every chip records how to redraw itself.</b> Choosing a template changes all
        // forty-odd colours at once; without this the chips would keep showing the old palette
        // until the screen was rebuilt, which is a settings page lying about the setting.
        chipRefreshers.add(() -> {
            chip.setBackground(current.get());
            if (after != null) {
                after.run();
            }
        });
        chip.addActionListener(e -> {
            Color picked = JColorChooser.showDialog(SettingsPanel.this, dialogTitle,
                current.get());
            if (picked != null) {
                store.accept(String.format("#%02X%02X%02X", picked.getRed(), picked.getGreen(),
                    picked.getBlue()));
                chip.setBackground(current.get());
                if (after != null) {
                    after.run();
                }
                applyPalette();
            }
        });
        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    store.accept(null);
                    chip.setBackground(current.get());
                    if (after != null) {
                        after.run();
                    }
                    applyPalette();
                }
            }
        });
        return chip;
    }

    /** A colour changed: redraw the wheel, and every surface that reads the same palette. */
    private void applyPalette() {
        if (window != null) {
            window.applyAspectSelection();
        }
    }

    private void saveAspects() {
        if (constructing || aspectBoxRefs == null || aspectBulkUpdate) {
            return;
        }
        boolean[] on = new boolean[aspectBoxRefs.length];
        for (int i = 0; i < aspectBoxRefs.length; i++) {
            on[i] = aspectBoxRefs[i] != null && aspectBoxRefs[i].isSelected();
        }
        Settings.saveAspectSelection(on);
        if (window != null) {
            window.applyAspectSelection();
        }
    }

    private JPanel bulkButtons() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(bulkButton("Select all", () -> setAll(true)));
        row.add(bulkButton("Select none", () -> setAll(false)));
        row.add(bulkButton("Restore defaults", this::restoreDefaults));
        return row;
    }

    private JButton bulkButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.PLAIN, 12));
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Sets every box, then applies once. See the bulkUpdate field. */
    private void setAll(boolean on) {
        bulkUpdate = true;
        for (JCheckBox box : boxes) {
            box.setSelected(on);
        }
        bulkUpdate = false;
        save();
    }

    private void restoreDefaults() {
        boolean[] defaults = Bodies.defaults();
        bulkUpdate = true;
        for (int i = 0; i < boxes.length; i++) {
            boxes[i].setSelected(defaults[i]);
        }
        bulkUpdate = false;
        save();
    }

    /** Writes the selection and asks the wheel to pick it up. */
    private void save() {
        // <b>constructing is the important half of this guard.</b> boxes is sized to the
        // registry up front and filled group by group, so a listener firing mid-build reads a
        // half-populated array - and the entries not yet created count as unticked. That is how
        // the stored selection went from twenty-nine points to five with nobody touching a
        // control. The null check below is the same defect's other face: it would have thrown
        // rather than truncated, which at least would have been noticed.
        if (constructing || bulkUpdate) {
            return;
        }
        boolean[] enabled = new boolean[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            enabled[i] = boxes[i] != null && boxes[i].isSelected();
        }
        Settings.saveBodySelection(enabled);
        if (window != null) {
            window.applyBodySelection();
        }
        refreshStatus();
    }

    private JPanel variantToggles() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.BLACK);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(heading("Calculation Variants"));
        p.add(note("These apply to the chart and all calculations including interpretations."));
        p.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] nodeOpts = {"True Node", "Mean Node"};
        JComboBox<String> nodeCombo = new JComboBox<>(nodeOpts);
        nodeCombo.setSelectedItem(Settings.useTrueNode() ? "True Node" : "Mean Node");
        nodeCombo.addActionListener(e -> {
            Settings.setTrueNode("True Node".equals(nodeCombo.getSelectedItem()));
            status.setText("Saved");
            if (window != null) window.applyBodySelection();
        });

        String[] lilithOpts = {"True (Osculating) Lilith", "Mean Lilith"};
        JComboBox<String> lilithCombo = new JComboBox<>(lilithOpts);
        lilithCombo.setSelectedItem(Settings.useTrueLilith() ? "True (Osculating) Lilith" : "Mean Lilith");
        lilithCombo.addActionListener(e -> {
            Settings.setTrueLilith("True (Osculating) Lilith".equals(lilithCombo.getSelectedItem()));
            status.setText("Saved");
            if (window != null) window.applyBodySelection();
        });

        JLabel lNode = new JLabel("Lunar Node: ");
        lNode.setForeground(TEXT);
        JLabel lLilith = new JLabel("Black Moon Lilith: ");
        lLilith.setForeground(TEXT);

        row.add(lNode);
        row.add(nodeCombo);
        row.add(lLilith);
        row.add(lilithCombo);
        
        p.add(row);
        return p;
    }

    private void refreshStatus() {
        int on = 0;
        for (JCheckBox box : boxes) {
            if (box != null && box.isSelected()) {
                on++;
            }
        }
        status.setText(on + " of " + boxes.length
            + " points shown. Saved to settings.properties; applies to the chart at once.");
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(new Font("Arial", Font.BOLD, 17));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Two pixels of slack on the right. A JLabel gets exactly its preferred width in a
        // BoxLayout, and a one-pixel disagreement between the metrics that computed it and
        // the metrics that draw it takes the last character off the end - which it did, to
        // every heading here that ends in an "s".
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        return l;
    }

    private JLabel note(String text) {
        JLabel l = new JLabel("<html><body style='width:600px'>" + text + "</body></html>");
        l.setForeground(DIM);
        l.setFont(new Font("Arial", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
