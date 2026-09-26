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

    /**
     * One ceiling spinner an aspect, indexed by {@link Aspects.Type#ordinal}.
     *
     * <b>Beside the aspect rather than in a table of its own.</b> David, 2026-09-24: the width
     * belongs next to the tick and the colour chip, because the three are one decision about one
     * aspect. H1 built the six capped ones as a separate grid lower down the screen, which meant
     * reading a name in one place and setting its number in another.
     *
     * <b>Held as a field because the Reset button is elsewhere.</b> It sits with the point widths
     * in {@link #natalOrbs}, since it puts both sets back.
     */
    private javax.swing.JSpinner[] aspectCapSpinners;

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

    /**
     * One orb spinner a point, indexed like {@link #boxes}.
     *
     * <b>Beside the point rather than in a table of its own.</b> David, 2026-09-24: the width
     * belongs next to the tick and the colour chip, because the three are one decision about one
     * point - whether it is drawn, in what colour, and how wide it aspects. The same arrangement
     * the aspect list uses, so the screen answers the question the same way twice.
     *
     * <b>Held as a field because two places need it.</b> The spinners are made in
     * {@link #groupPanel}, a group at a time, and the Reset that puts them all back is further
     * down in {@link #natalOrbs}.
     */
    private final javax.swing.JSpinner[] orbSpinners =
        new javax.swing.JSpinner[Bodies.count()];

    /**
     * Which of the four tables every orb control on this screen is showing.
     *
     * <b>The screen's state, not a setting.</b> It is not stored and not restored: a reader who
     * leaves the panel on Synastry and comes back expects their natal widths, because natal is
     * what the app reads unless it is told otherwise. Storing it would let a reader edit synastry
     * without having chosen to.
     */
    private com.zodiacomputing.ourania.astro.Aspects.Profile shownProfile =
        com.zodiacomputing.ourania.astro.Aspects.Profile.NATAL;

    /**
     * Every column header that names the shown profile in its tooltip.
     *
     * <b>Lists, because these are made per group and not once.</b> {@code groupPanel} runs for
     * each body group and {@code aspectBoxes} for each aspect column, so the screen carries
     * several of each header. Held in a single field, only the last one built would ever be
     * updated and the rest would go on describing a profile that is no longer shown - which is
     * this project's recurring defect wearing a new hat.
     */
    private final java.util.List<JLabel> orbColumnHeaders = new java.util.ArrayList<>();
    private final java.util.List<JLabel> capColumnHeaders = new java.util.ArrayList<>();

    /** The sentence under the Orbs heading. */
    private JLabel orbProfileNote;

    /**
     * Every preset bar and every Reset button on the screen.
     *
     * <b>There are two of each, one on Aspects & Orbs and one on Bodies & Points</b>, because a
     * Swing component cannot be in two containers and the presets belong on both tabs that have
     * orb controls. Two controls over one setting is this project's most logged defect, so they
     * are built by one method, held here, and moved together in {@link #showProfile}.
     */
    private final java.util.List<javax.swing.JToggleButton> orbToggles =
        new java.util.ArrayList<>();
    private final java.util.List<javax.swing.JButton> orbResetButtons =
        new java.util.ArrayList<>();

    private javax.swing.JTabbedPane tabs;
    private final JLabel status = new JLabel(" ");
    /** Settings > Calculation Variants: the one transit orb. */
    javax.swing.JSpinner transitOrb;
    /** Settings > Calculation Variants: which rule progresses the angles. */
    JComboBox<com.zodiacomputing.ourania.astro.ProgressedAngles.Method> progressedAngles;

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
        title.setFont(Theme.font("Arial", Font.BOLD, 24));
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

        // <b>One column per tab, and `body` points at whichever is being filled.</b> Every
        // section below is added to `body` in sequence, as it was when there was one column;
        // what changed in the move to tabs is where `body` points, not a single add() call. A
        // section therefore cannot be dropped or duplicated by the restructure without the
        // boundary that did it being visible in the diff.
        JPanel display = column();
        JPanel aspects = column();
        JPanel points = column();
        JPanel engine = column();

        // Chart Settings first, and it is an engine rule: harmonic, house system, what the
        // animation moves.
        JPanel body = engine;

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

        body = aspects;
        body.add(presetRow());
        body.add(heading("Aspects Shown"));
        body.add(note("Every aspect ticked here is drawn on the wheel and listed in the "
            + "aspect grid. Unticking one removes it from both - they read the same gate, so "
            + "they cannot disagree about which aspects exist."));
        body.add(aspectBulkButtons());
        body.add(Box.createRigidArea(new Dimension(0, 6)));
        body.add(fixed(aspectBoxes()));
        body.add(Box.createRigidArea(new Dimension(0, 18)));

        body = display;
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

        JCheckBox arcs = new JCheckBox("On the globe, arc the aspect lines over the centre",
            Settings.globeAspectArcs());
        arcs.setForeground(TEXT);
        arcs.setBackground(Color.BLACK);
        arcs.setFont(Theme.BODY);
        arcs.setFocusPainted(false);
        arcs.setAlignmentX(Component.LEFT_ALIGNMENT);
        arcs.setToolTipText("<html>On: an aspect bows up and over the middle of the globe, so "
            + "an opposition is a span across the top rather than a line through the centre "
            + "where every other line already is.<br>Off: the straight chords, which stay "
            + "easier to trace when only two or three are drawn.<br>"
            + "<i>The globe only; the flat wheel is unaffected.</i></html>");
        arcs.addItemListener(e -> {
            Settings.setGlobeAspectArcs(arcs.isSelected());
            applyPalette();
        });
        body.add(arcs);

        // <b>H3's four settings, which had no control at all until now.</b> They were readable
        // and had defaults and refused nonsense, and nothing on this screen showed them - which
        // is the built-but-unreachable defect this project has closed three times this week, so
        // it does not get to be opened a fourth.
        body.add(heading("Aspect Lines"));
        body.add(note("How the lines across the wheel are drawn. Colour says which aspect; "
            + "these say how strong it is and which chart it belongs to."));

        JCheckBox toBodies = new JCheckBox("Draw aspect lines out to the bodies",
            Settings.aspectLinesToBodies());
        styleCheck(toBodies);
        toBodies.setToolTipText("<html><b>On:</b> a line touches the two bodies it joins."
            + "<br><b>Off:</b> it spans an inner disc, keeping each body's longitude but stopping"
            + "<br>short of the glyph - so the three rings' networks stay in three bands"
            + "<br>instead of running through one another."
            + "<br><br><i>Off reads better the more aspects are drawn; on is easier to follow"
            + "<br>when there are few.</i></html>");
        toBodies.addItemListener(e -> {
            Settings.set(Settings.ASPECT_LINES_TO_BODIES_KEY,
                toBodies.isSelected() ? "true" : "false");
            status.setText("Saved");
            applyPalette();
        });
        body.add(toBodies);

        JCheckBox absolute = new JCheckBox("Measure strength in degrees, not as a share of the orb",
            Settings.aspectWeightAbsolute());
        styleCheck(absolute);
        absolute.setToolTipText("<html><b>Off (the usual):</b> an aspect's strength is how much of"
            + "<br>its allowed orb it uses. A 3&deg; conjunction reads tighter under a 10&deg;"
            + "<br>orb than under a 5&deg; one - so widening your orbs makes every line"
            + "<br>already drawn heavier."
            + "<br><b>On:</b> strength is degrees from exact, so the picture holds still"
            + "<br>while you are tuning orbs."
            + "<br><br><i>Off keeps the minor aspects visible at their own scale.</i></html>");
        absolute.addItemListener(e -> {
            Settings.set(Settings.ASPECT_WEIGHT_ABSOLUTE_KEY,
                absolute.isSelected() ? "true" : "false");
            status.setText("Saved");
            applyPalette();
        });
        body.add(absolute);

        body.add(weightRow("Thinnest line", Settings.ASPECT_WEIGHT_MIN_KEY,
            Settings.aspectWeightMin(),
            "The width of the faintest aspect drawn - a loose minor one."));
        body.add(weightRow("Thickest line", Settings.ASPECT_WEIGHT_MAX_KEY,
            Settings.aspectWeightMax(),
            "The width of an exact conjunction or opposition. Everything else falls between "
                + "the two by how close it is and how much force the aspect carries."));

        JCheckBox signPlane = new JCheckBox("On the globe, fill the signs as a coloured shell",
            Settings.globeSignPlane());
        signPlane.setForeground(TEXT);
        signPlane.setBackground(Color.BLACK);
        signPlane.setFont(Theme.BODY);
        signPlane.setFocusPainted(false);
        signPlane.setAlignmentX(Component.LEFT_ALIGNMENT);
        signPlane.setToolTipText("<html><b>The translucent sign shell.</b><br>On: each sign is "
            + "washed in its element's colour, so a body seen through it is in a sign and in a "
            + "house at once - the one thing the flat wheel cannot show.<br>Off: the boundaries, "
            + "the band at the equator and the degree scale stay; only the wash goes, which "
            + "clears the aspect network underneath.<br><i>The globe only; the flat wheel is "
            + "unaffected.</i></html>");
        signPlane.addItemListener(e -> {
            Settings.setGlobeSignPlane(signPlane.isSelected());
            applyPalette();
        });
        body.add(signPlane);

        // The same switch for the other bands that have a fill to switch.
        //
        // <b>There is no decan box, deliberately.</b> The decan band on the globe is glyph
        // billboards and nothing else - no shell, no wash, no hover fill - so a control for it
        // would sit here governing nothing, which is the placeholder defect this screen has
        // already had removed from it twice.
        body.add(globeFillBox("On the globe, fill the current house",
            Settings.globeHouseFill(), Settings::setGlobeHouseFill,
            "<html><b>The house wash.</b><br>On: the house a body stands in is washed in "
            + "parchment across the plane, and the house asked for by its number is cut through "
            + "the sphere as well.<br>Off: the cusp spokes, the numbers and the glyphs all stay "
            + "where they are; only the wash goes.<br><i>The globe only.</i></html>"));
        body.add(globeFillBox("On the globe, fill the degree under the cursor",
            Settings.globeDegreeFill(), Settings::setGlobeDegreeFill,
            "<html><b>The Sabian scale's own fill.</b><br>On: pointing at a tick lights the "
            + "one degree behind it, across the plane and through the sphere, so the tick names "
            + "a place rather than a mark.<br>Off: the 360 ticks, the scale line they hang from "
            + "and the sign marks stay; the slice behind them does not.<br><i>The globe "
            + "only.</i></html>"));
        body.add(globeFillBox("On the globe, fill the lunar mansion stations",
            Settings.globeMansionFill(), Settings::setGlobeMansionFill,
            "<html><b>The mansion band's tint.</b><br>On: the Moon's own station is washed in "
            + "lavender and the station under the cursor more brightly.<br>Off: the band's two "
            + "edges, its 28 divisions and its numbers stay, and the Moon's station is still "
            + "picked out by a brighter boundary and number - the stations stay countable, they "
            + "stop being tinted.<br><i>The globe only; the flat wheel's mansion ring is "
            + "unaffected.</i></html>"));

        JCheckBox stacked = new JCheckBox("On the globe, stack the rings instead of crossing "
            + "them", Settings.globeStackedRings());
        stacked.setForeground(TEXT);
        stacked.setBackground(Color.BLACK);
        stacked.setFont(Theme.BODY);
        stacked.setFocusPainted(false);
        stacked.setAlignmentX(Component.LEFT_ALIGNMENT);
        stacked.setToolTipText("<html>On: the sky sits just above this chart and Chart B just "
            + "below it, all three parallel - so a transit conjunct a natal planet is directly "
            + "over it, at every degree of the wheel.<br>Off: the two outer rings tip opposite "
            + "ways and meet this chart at the Ascendant, which shows three distinct planes but "
            + "only lines the degrees up where they cross.<br>"
            + "<i>The globe only; the flat wheel is unaffected.</i></html>");
        stacked.addItemListener(e -> {
            Settings.setGlobeStackedRings(stacked.isSelected());
            applyPalette();
        });
        body.add(stacked);

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

        body = points;
        body.add(presetRow());
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
        body.add(fixed(groups));

        body.add(Box.createRigidArea(new Dimension(0, 16)));
        body = engine;
        body.add(variantToggles());
        body = aspects;
        body.add(natalOrbs());
        body.add(Box.createRigidArea(new Dimension(0, 8)));
        body.add(note("Aspect lines are drawn between bodies, not to the four angles. Click "
            + "an angle on the wheel to see what it currently contacts."));

        // <b>A footer, not the bottom of one tab.</b> "Saved" is about whatever the reader
        // just changed, and on a tabbed screen the tab they changed it on may not be showing.
        status.setForeground(DIM);
        status.setFont(Theme.font("Arial", Font.ITALIC, 12));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);

        // <b>What governs every tab sits above them.</b> The preset bar decides which orb
        // table the Aspects and Bodies tabs are showing, and Reset puts that whole preset back -
        // widths and ceilings together. On a tab, either would be a control silently reaching
        // into another one. A single instance up here also means there is no second copy of the
        // bar to keep in step, which is the defect this project logs most.
        // <b>Nothing sits above the tab strip.</b> The preset bar did, hidden on the two tabs
        // it governs nothing on, which made the strip jump as you moved between them; and Saved
        // Settings did, pushing the tabs down for the least used thing on the screen. The presets
        // are on their own tabs now and Saved Settings folds out of the bottom - David, 25 Sep.
        this.tabs = new javax.swing.JTabbedPane();
        this.tabs.setFont(Theme.font("Arial", Font.PLAIN, 13));
        this.tabs.addTab("Globe & Display", tabScroll(display));
        this.tabs.addTab("Aspects & Orbs", tabScroll(aspects));
        this.tabs.addTab("Bodies & Points", tabScroll(points));
        this.tabs.addTab("Engine Rules", tabScroll(engine));
        this.tabs.setToolTipTextAt(0, "<html>What the wheel and the globe look like:"
            + "<br>colours, rings, markers and the globe's own layers.</html>");
        this.tabs.setToolTipTextAt(1, "<html>Which aspects are drawn, and how wide each may be"
            + "<br>before it stops counting. The preset above says which chart context.</html>");
        this.tabs.setToolTipTextAt(2, "<html>Which points the chart shows, their colours, and"
            + "<br>how wide an aspect to each may be. The preset above applies here too.</html>");
        this.tabs.setToolTipTextAt(3, "<html>What the engine computes rather than how it is"
            + "<br>drawn: house system, harmonic, zodiac, transit orb and the variants.</html>");

        // <b>Painted here, not by the platform.</b> Windows draws a selected tab almost white,
        // which against this screen's light text makes the tab you are on the one you cannot read.
        Widgets.styleTabs(this.tabs);

        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(Color.BLACK);
        centre.add(this.tabs, BorderLayout.CENTER);

        JPanel foot = new JPanel();
        foot.setLayout(new BoxLayout(foot, BoxLayout.Y_AXIS));
        foot.setBackground(Color.BLACK);
        foot.setBorder(BorderFactory.createEmptyBorder(4, 30, 6, 30));
        foot.add(savedSetsFold());
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        foot.add(Box.createRigidArea(new Dimension(0, 4)));
        foot.add(status);
        centre.add(foot, BorderLayout.SOUTH);
        add(centre, BorderLayout.CENTER);

        refreshStatus();
        constructing = false;
    }

    /**
     * A panel that is its own size and no larger.
     *
     * <b>BoxLayout shares spare height among everything with an unbounded maximum</b>, and both
     * the aspect grid and the body groups have one - so on a tab with room to spare they took a
     * share of it and centred their contents in the middle of the result. A glue alone only ever
     * got a fraction of the slack. Width too: a GridBagLayout centres its contents in whatever
     * width it is handed, which is why the aspect list sat in the middle of the screen.
     */
    private static JPanel fixed(JPanel p) {
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(p.getPreferredSize());
        return p;
    }

    /** One tab's column, laid out exactly as the single column was. */
    private JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.BLACK);
        p.setBorder(BorderFactory.createEmptyBorder(8, 30, 20, 30));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /**
     * Saved Settings, folded shut at the foot of the screen.
     *
     * <b>It was the first thing on the screen and is the least used thing on it</b>, costing a
     * heading and two lines of explanation before a reader reached anything they had come for.
     * Shut it costs one row.
     *
     * <p>A button rather than a styled label, because it is a control: it takes focus, it works
     * from the keyboard, and it says which way it will go.
     */
    private JPanel savedSetsFold() {
        final JPanel body = savedSetsRow();
        body.setVisible(false);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);

        final javax.swing.JButton toggle = new javax.swing.JButton();
        toggle.setFocusPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setForeground(TEXT);
        toggle.setFont(Theme.font("Arial", Font.BOLD, 13));
        toggle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        toggle.setMargin(new java.awt.Insets(2, 0, 2, 0));
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.setToolTipText("<html>Store everything on this screen under a name of your own,"
            + "<br>and put it back later. Your chart, your saved places and your window"
            + "<br>are never part of a set.</html>");
        toggle.setText("\u25B8  Saved Settings");
        toggle.addActionListener(e -> {
            boolean open = !body.isVisible();
            body.setVisible(open);
            toggle.setText((open ? "\u25BE" : "\u25B8") + "  Saved Settings");
            revalidate();
            repaint();
        });

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.BLACK);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(toggle);
        p.add(body);
        p.add(resetAllRow());
        return p;
    }

    /**
     * Put every setting on this screen back to what it ships as (H4a).
     *
     * <b>It asks first, and says what it will not touch.</b> There is no undo on this screen, and
     * a reader who has spent an evening on their orbs should not lose it to a stray click. The
     * dialog names the boundary rather than leaving it to be discovered: their chart, their saved
     * places and their window are not configuration and are never cleared.
     *
     * <p>Beside Saved Settings on purpose. A reader about to reset is a reader who might rather
     * save first, and the two controls together say so.
     */
    private JPanel resetAllRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton b = bulkButton("Reset everything on this screen", () -> {
            int answer = javax.swing.JOptionPane.showConfirmDialog(this,
                "<html><b>Put every setting on this screen back to its default?</b><br><br>"
                    + "That is the orbs and their presets, the aspects shown, the points, the"
                    + "<br>palette, the house system and the engine rules.<br><br>"
                    + "<b>Not touched:</b> your chart, your saved places, your home location"
                    + "<br>and your window. Saved settings sets are kept too.<br><br>"
                    + "There is no undo - save a set first if you might want this back.</html>",
                "Reset settings", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
            if (answer != javax.swing.JOptionPane.OK_OPTION) {
                status.setText("Left alone");
                return;
            }
            int cleared = Settings.resetAll();
            status.setText("Reset " + cleared + (cleared == 1 ? " setting" : " settings"));
            if (window != null) {
                window.applyBodySelection();
            }
            applyPalette();
        });
        b.setToolTipText("<html>Every setting on this screen back to its default."
            + "<br>Your chart, your places and your window are not settings"
            + "<br>and are left alone. Asks before it does anything.</html>");
        row.add(b);
        row.setMaximumSize(row.getPreferredSize());
        return row;
    }

    /** One tab's preset bar and its Reset, with a little room under the tab strip. */
    private JPanel presetRow() {
        JPanel p = profileBar();
        p.add(Box.createRigidArea(new Dimension(12, 0)));
        p.add(orbReset());
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(p.getPreferredSize());
        return p;
    }

    /**
     * One tab's column in a scroller.
     *
     * <b>The column goes at NORTH, which is what stops everything on it being stretched.</b>
     * BoxLayout hands spare height to every component whose maximum allows it, and on this screen
     * that is nearly all of them - so on a tab with room to spare the aspect grid floated in the
     * middle of an enormous box and the body groups had a hole through them. At NORTH the column
     * gets exactly its preferred height, so there is no spare height to hand out. Fixing it here
     * rather than capping panels one at a time also means a section added later arrives right
     * without anyone remembering this.
     */
    private JScrollPane tabScroll(JPanel body) {
        JPanel hold = new JPanel(new BorderLayout());
        hold.setBackground(Color.BLACK);
        hold.add(body, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(hold);
        scroll.setBorder(null);
        // Three separate surfaces paint here: the scroll pane, its viewport, and the panel
        // inside. Colouring only the viewport left the pane's own white showing as a border
        // all the way round a black screen.
        scroll.setOpaque(true);
        scroll.setBackground(Color.BLACK);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.BLACK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
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
        name.setFont(Theme.font("Arial", Font.BOLD, 14));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        name.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        panel.add(name);

        // "body style=width" rather than a div: it is the form Swing's HTML renderer
        // actually honours for wrapping a JLabel, and a div is silently ignored - which is
        // why the intro note came out as one clipped line the first time this was drawn.
        JLabel blurb = new JLabel("<html><body style='width:260px'>" + group.blurb + "</body></html>");
        blurb.setForeground(DIM);
        blurb.setFont(Theme.font("Arial", Font.PLAIN, 11));
        blurb.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(blurb);

        // <b>Per section, because the whole-screen pair is not the same offer.</b> David,
        // 2026-09-16: a select-all for each section, "like for asteroids etc." The buttons at
        // the foot of the screen move all 32 points at once, which is no use to a reader who
        // wants every asteroid and none of the lots - they would have to click through a
        // section one point at a time to get there. Scoped to this group's own boxes, and
        // through the same bulkUpdate guard the screen-wide pair uses, so 12 toggles are one
        // write of the settings file rather than 12.
        JPanel bulk = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bulk.setBackground(Color.BLACK);
        bulk.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulk.add(groupButton("All", group, true));
        bulk.add(groupButton("None", group, false));
        panel.add(bulk);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        // <b>One grid, so the three controls read as columns.</b> A FlowLayout a row puts every
        // spinner at the end of a name of a different length, which is the ragged edge that made
        // the old orb grid look the way it did. Comparing widths down a column is what the
        // control is for.
        JPanel rows = new JPanel(new GridBagLayout());
        rows.setBackground(Color.BLACK);
        rows.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(1, 0, 1, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.NONE;

        // <b>A header over each column.</b> Three controls in a row is three questions about one
        // point, and until now only the colour chip announced itself.
        gc.gridy = 0;
        gc.gridx = 0;
        rows.add(columnHeader("Colour",
            "<html>The colour this point is drawn in, on the wheel, the globe and the tables."
                + "<br>Click the chip to choose another; right-click it to go back to the "
                + "colour template.</html>", Color.BLACK), gc);
        gc.gridx = 1;
        rows.add(columnHeader("Point",
            "<html>Tick a point to draw it on the wheel, list it in the placements panel"
                + "<br>and include it in the aspect grid. Everything unticked is left out of"
                + "<br>all three.</html>", Color.BLACK), gc);
        gc.gridx = 2;
        // Collected, not held: groupPanel runs once per body group, so there are several.
        JLabel orbHead = columnHeader("Orb\u00b0", orbHeaderTip(), Color.BLACK);
        orbColumnHeaders.add(orbHead);
        rows.add(orbHead, gc);
        int gridRow = 1;

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
            box.setFont(Theme.font("Segoe UI Symbol", Font.PLAIN, 14));
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

            gc.gridy = gridRow++;
            gc.weightx = 0.0;
            gc.gridx = 0;
            rows.add(bodySwatch(index, box), gc);
            gc.gridx = 1;
            rows.add(box, gc);
            gc.gridx = 2;
            rows.add(orbSpinner(index), gc);

            // The slack goes to a fourth column, so the three real ones keep their own width and
            // the spinners line up instead of being pushed to the right edge.
            gc.gridx = 3;
            gc.weightx = 1.0;
            rows.add(Box.createHorizontalGlue(), gc);
        }
        // <b>Its own height, not a share of the box's.</b> The groups sit in a GridLayout,
        // which gives every box the height of the tallest, and a GridBagLayout centres its
        // rows in whatever height it is handed - so a short group had its points floating in
        // the middle with a hole above them. Pre-dates the tabs; visible on any of them.
        panel.add(fixed(rows));
        return panel;
    }

    /**
     * Keep the configuration you have, and get it back later.
     *
     * <b>A set is the configuration, not you.</b> {@code Settings.saveSet} leaves out the chart,
     * the places you have named and where the window was - see {@code Settings.isPersonal} for
     * the list and the reason. A button called "save my settings" that quietly put a
     * six-month-old birth time back would be the worst kind of defect: the chart would change and
     * this screen would be the last place anyone looked.
     *
     * <b>Restoring replaces rather than merges</b>, so what comes back is the configuration as it
     * was saved and not a hybrid of it and whatever has been changed since. The screen is rebuilt
     * afterwards because forty controls are then showing values that are no longer true.
     */
    private JPanel savedSetsRow() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.BLACK);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(note("Store everything on this screen under a name of your own, and put it back "
            + "later - useful before trying a different set of orbs, or when an update changes "
            + "something. Your chart, your saved places and your window are never part of a set."));
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        final javax.swing.JComboBox<String> chooser = new javax.swing.JComboBox<>();
        for (String name : Settings.savedSets()) {
            chooser.addItem(name);
        }
        chooser.setToolTipText("<html>The sets you have saved.<br>Choose one and press Restore to "
            + "put it back in force.</html>");
        chooser.setEnabled(chooser.getItemCount() > 0);

        javax.swing.JButton save = new javax.swing.JButton("Save current settings...");
        save.setToolTipText("<html>Store everything on this screen under a name of your own."
            + "<br>Your chart, your saved places and your window position are <b>not</b> included."
            + "</html>");
        Widgets.styleButton(save, Widgets.Role.PRIMARY);
        save.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(SettingsPanel.this,
                "Name for this set of settings:", "Save settings", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) {
                return;
            }
            if (Settings.savedSets().contains(name.trim())
                && JOptionPane.showConfirmDialog(SettingsPanel.this,
                    "Replace the set called " + name.trim() + "?", "Name in use",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!Settings.saveSet(name)) {
                JOptionPane.showMessageDialog(SettingsPanel.this,
                    "That set could not be written.", "Not saved", JOptionPane.WARNING_MESSAGE);
                return;
            }
            status.setText("Saved");
            if (window != null) {
                window.rebuildSettings();
            }
        });

        javax.swing.JButton restore = new javax.swing.JButton("Restore");
        restore.setToolTipText("<html>Put the chosen set back in force, replacing everything on "
            + "this screen.<br>Your chart and your saved places are left alone.</html>");
        restore.setEnabled(chooser.getItemCount() > 0);
        restore.addActionListener(e -> {
            Object picked = chooser.getSelectedItem();
            if (picked == null) {
                return;
            }
            if (JOptionPane.showConfirmDialog(SettingsPanel.this,
                "Replace every setting on this screen with the set called " + picked + "?",
                "Restore settings", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            if (!Settings.restoreSet(picked.toString())) {
                JOptionPane.showMessageDialog(SettingsPanel.this,
                    "That set could not be read.", "Not restored", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (window != null) {
                window.applyBodySelection();
                // Last, because it replaces this panel - and therefore this listener's own
                // component - so nothing may follow it.
                window.rebuildSettings();
            }
        });

        javax.swing.JButton forget = new javax.swing.JButton("Forget");
        forget.setToolTipText("Delete the chosen set. What is in force now does not change.");
        forget.setEnabled(chooser.getItemCount() > 0);
        forget.addActionListener(e -> {
            Object picked = chooser.getSelectedItem();
            if (picked == null) {
                return;
            }
            if (JOptionPane.showConfirmDialog(SettingsPanel.this,
                "Forget the set called " + picked + "? This cannot be undone.",
                "Forget set", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            Settings.deleteSet(picked.toString());
            if (window != null) {
                window.rebuildSettings();
            }
        });

        row.add(save);
        row.add(chooser);
        row.add(restore);
        row.add(forget);
        p.add(row);
        p.add(Box.createRigidArea(new Dimension(0, 16)));
        return p;
    }

    /**
     * One column header: what the column is, with the instruction on hover.
     *
     * <b>One helper for both lists.</b> The points and the aspects carry the same three columns -
     * a colour, a tick and a number - and two hand-written sets of headings would be free to
     * describe them differently, which is the shape of defect this project keeps finding. The
     * words differ per list because the things differ; the styling and the behaviour do not.
     *
     * <b>Small and dim on purpose.</b> A header row as loud as the rows under it competes with
     * the thing a reader came to find. It names the column and gets out of the way, and the
     * sentence that actually explains the control is on the hover, where it costs nothing until
     * it is wanted.
     */
    /**
     * The preset switch: which chart context the orb controls below are showing.
     *
     * <b>Driven from the enum, never a hand-written list of four.</b> A fifth profile would
     * appear here without anyone coming back for it, and could not appear spelled differently -
     * which is exactly how the wheel's legend lost its quincunx for months.
     */
    private JPanel profileBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setBackground(Color.BLACK);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel caption = new JLabel("Orbs shown for:");
        caption.setForeground(DIM);
        caption.setFont(Theme.font("Arial", Font.PLAIN, 12));
        p.add(caption);

        javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
        for (final com.zodiacomputing.ourania.astro.Aspects.Profile profile
                : com.zodiacomputing.ourania.astro.Aspects.Profile.values()) {
            javax.swing.JToggleButton b = new javax.swing.JToggleButton(profileLabel(profile));
            b.setFont(Theme.font("Arial", Font.PLAIN, 11));
            b.setFocusPainted(false);
            b.setToolTipText(profileTip(profile));
            b.setSelected(profile == shownProfile);
            b.setActionCommand(profile.name());
            b.addActionListener(e -> showProfile(profile));
            group.add(b);
            p.add(b);
            this.orbToggles.add(b);
        }
        return p;
    }

    /** What a profile is called on the screen. One place, so the four cannot drift apart. */
    private static String profileLabel(
            com.zodiacomputing.ourania.astro.Aspects.Profile profile) {
        switch (profile) {
            case TRANSIT: return "Transits";
            case SYNASTRY: return "Synastry";
            case COMPOSITE: return "Composite";
            default: return "Natal";
        }
    }

    /** What each preset governs, and where its numbers come from when nobody has set them. */
    private static String profileTip(
            com.zodiacomputing.ourania.astro.Aspects.Profile profile) {
        switch (profile) {
            case TRANSIT:
                return "<html><b>Transits</b><br>The event lists: what the sky is doing to a"
                    + " chart, and when.<br>Every point starts at one flat width, because the"
                    + " natal table puts<br>21.6 transits in orb at any moment and keeps a Pluto"
                    + "<br>conjunction open for over fourteen years."
                    + "<br><br><b>The wheel is not this.</b> It draws sky-to-person aspects at"
                    + " the<br>natal widths, so a slow contact stays visible while it"
                    + " lasts.</html>";
            case SYNASTRY:
                return "<html><b>Synastry</b><br>One person's chart read against another's."
                    + "<br>Every point starts at <b>half its natal width</b> - half of whatever"
                    + "<br>you have set, not half of the built-in table - because a"
                    + "<br>cross-chart contact wants to be closer to count."
                    + "<br><br>Set one here and it stops following natal. The rest go on"
                    + " following.</html>";
            case COMPOSITE:
                return "<html><b>Composite</b><br>The single chart derived from two people's"
                    + " midpoints."
                    + "<br>Every point starts at <b>its natal width</b>, because a composite is"
                    + "<br>one chart and not a comparison between two.</html>";
            default:
                return "<html><b>Natal</b><br>The chart itself: the wheel, the aspect grid and"
                    + " the readings."
                    + "<br>The widths here are the built-in table until you change them,"
                    + "<br>and the other three presets are measured from them.</html>";
        }
    }

    /**
     * Point every orb control on the screen at one profile.
     *
     * <b>The assignment below must stay above the loop.</b> Setting a spinner fires its change
     * listener and that listener writes to {@code Settings} under {@code shownProfile}. Assigned
     * first, every write lands on the profile being switched TO, carrying the value that profile
     * already holds - a no-op. Assigned after the loop, every write lands on the profile being
     * switched FROM, carrying the new one's numbers, and the reader's natal table is quietly
     * overwritten with synastry widths. That mutation takes 14 assertions red in
     * {@code PresetBarCheck}; removing the {@code seeding} guard takes none, which is the
     * opposite of what the first version of this comment claimed.
     */
    private void showProfile(com.zodiacomputing.ourania.astro.Aspects.Profile profile) {
        if (profile == null || profile == shownProfile) {
            return;
        }
        shownProfile = profile;   // before the loop - see above; moving it corrupts the tables
        seeding = true;
        try {
            for (int i = 0; i < orbSpinners.length; i++) {
                javax.swing.JSpinner s = orbSpinners[i];
                if (s == null) {
                    continue;
                }
                String name = Bodies.at(i).name;
                s.setValue(Settings.bodyOrb(name, profile));
                s.setToolTipText(orbTip(name));
                markChanged(s, name);
            }
            if (aspectCapSpinners != null) {
                for (com.zodiacomputing.ourania.astro.Aspects.Type type
                        : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
                    javax.swing.JSpinner s = aspectCapSpinners[type.ordinal()];
                    if (s == null) {
                        continue;
                    }
                    retuneCapSpinner(s, type);
                    s.setToolTipText(capTip(type));
                    markCapChanged(s, type);
                }
            }
        } finally {
            seeding = false;
        }
        refreshProfileWording();
        status.setText("Showing " + profileLabel(profile) + " orbs");
    }

    /**
     * Move a ceiling spinner onto the shown profile, bound and all.
     *
     * <b>The maximum moves with the profile.</b> A synastry ceiling may not exceed half the
     * declared one, so leaving the model's bound where natal left it would let a reader type a
     * number {@code Settings.setAspectCap} then silently clamps - the screen and the file
     * disagreeing, which is the shape of defect this session has spent its time closing.
     */
    private void retuneCapSpinner(javax.swing.JSpinner s,
                                  com.zodiacomputing.ourania.astro.Aspects.Type type) {
        if (!(s.getModel() instanceof javax.swing.SpinnerNumberModel)) {
            return;
        }
        javax.swing.SpinnerNumberModel m = (javax.swing.SpinnerNumberModel) s.getModel();
        double top = com.zodiacomputing.ourania.astro.Aspects.defaultCapOf(type, shownProfile);
        // Down to the new ceiling BEFORE the bound moves, or the model holds a value outside it.
        if (((Number) m.getValue()).doubleValue() > top) {
            m.setValue(top);
        }
        m.setMaximum(top);
        m.setValue(Settings.aspectCap(type, shownProfile));
    }

    /** Everything on the screen that names the shown profile in words, from one source. */
    private void refreshProfileWording() {
        for (JLabel h : orbColumnHeaders) {
            h.setToolTipText(orbHeaderTip());
        }
        for (JLabel h : capColumnHeaders) {
            h.setToolTipText(capHeaderTip());
        }
        if (orbProfileNote != null) {
            orbProfileNote.setText("<html><body style='width:600px'>" + profileSentence()
                + "</body></html>");
        }
        String name = profileLabel(shownProfile);
        for (javax.swing.JButton b : this.orbResetButtons) {
            b.setText("Reset " + name + " to defaults");
            b.setToolTipText("Forget every width and ceiling you have set under " + name
                + ", and go back to what it starts at. The other three presets are left alone.");
        }
        // <b>Every bar shows the same preset.</b> Two bars with different toggles lit would be
        // two answers to one question, which is the whole risk of having two of them.
        for (javax.swing.JToggleButton b : this.orbToggles) {
            b.setSelected(shownProfile.name().equals(b.getActionCommand()));
        }
    }

    private JLabel columnHeader(String text, String tip, Color background) {
        JLabel l = new JLabel(text);
        l.setForeground(DIM);
        l.setFont(Theme.font("Arial", Font.PLAIN, 10));
        l.setBackground(background);
        l.setToolTipText(tip);
        return l;
    }

    /**
     * The orb spinner for one point, beside its tick and its colour.
     *
     * <b>Reads the registry and {@link Aspects}, and lists nothing of its own.</b> The built-in
     * width comes from {@code Aspects.defaultBodyOrb} and the current one from {@code Settings},
     * so a point added to the registry gets a spinner without anyone coming back for it.
     *
     * <b>A point left at its built-in width stores nothing.</b> {@code Settings.setBodyOrb}
     * clears the key when the value matches the default, so a reader who nudges Neptune and puts
     * it back has the file they started with, and a later change to the built-in table still
     * reaches them.
     */
    private javax.swing.JSpinner orbSpinner(final int index) {
        final String name = Bodies.at(index).name;
        final javax.swing.JSpinner s = new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(Settings.bodyOrb(name, shownProfile),
                Aspects.MIN_BODY_ORB, Aspects.MAX_BODY_ORB, 0.25));
        s.setToolTipText(orbTip(name));
        java.awt.Dimension size = new java.awt.Dimension(64, s.getPreferredSize().height);
        s.setPreferredSize(size);
        s.setMaximumSize(size);
        s.addChangeListener(e -> {
            // <b>What this guard is and is not for.</b> showProfile sets every one of these
            // and each set fires this listener, so without it a profile switch runs 51 writes -
            // 36 points and 15 aspects - each calling applyBodySelection and rebuilding the
            // chart. It is NOT what keeps the tables apart: a mutation removing it left every
            // assertion in PresetBarCheck green, because showProfile assigns shownProfile before
            // the loop and each listener therefore writes the value already correct for the new
            // profile, which setBodyOrb stores as "" for matching its default. The ordering is
            // what protects the data; this protects the frame rate.
            if (constructing || seeding) {
                return;
            }
            Settings.setBodyOrb(name, ((Number) s.getValue()).doubleValue(), shownProfile);
            markChanged(s, name);
            status.setText("Saved");
            if (window != null) {
                window.applyBodySelection();
            }
        });
        markChanged(s, name);
        orbSpinners[index] = s;
        return s;
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

        aspectCapSpinners = new javax.swing.JSpinner[Aspects.Type.values().length];

        // <b>One GridBag, three aspects across, three columns each.</b> A GridLayout of
        // FlowLayouts put every spinner at the end of a label of a different length, and the
        // widths then read as a ragged edge - which is no use to a reader comparing them, and
        // comparing them is what this control is for. Nine grid columns, five rows.
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(Theme.card(Theme.EDGE, Theme.GAP_L));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(1, 0, 1, 6);
        int across = 3;
        int seen = 0;

        // <b>Once per group across, not once per panel.</b> Three aspects sit side by side, so a
        // single set of headings on the left would label the first group and leave the other two
        // to be guessed at.
        gc.gridy = 0;
        for (int group = 0; group < across; group++) {
            int at = group * 3;
            gc.gridx = at;
            panel.add(columnHeader("Colour",
                "<html>The colour this aspect's lines are drawn in, on the wheel, the globe"
                    + "<br>and the aspect grid. Click the chip to choose another; right-click"
                    + "<br>it to go back to the colour template.</html>", Theme.SURFACE), gc);
            gc.gridx = at + 1;
            panel.add(columnHeader("Aspect",
                "<html>Tick an aspect to draw it and include it in the grid and the readings."
                    + "<br>Unticked, it is not drawn, not listed and not interpreted.</html>",
                Theme.SURFACE), gc);
            gc.gridx = at + 2;
            gc.insets = new Insets(1, 0, 1, 18);
            JLabel capHead = columnHeader("Ceiling\u00b0", capHeaderTip(), Theme.SURFACE);
            capColumnHeaders.add(capHead);
            panel.add(capHead, gc);
            gc.insets = new Insets(1, 0, 1, 6);
        }

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

            gc.gridy = seen / across + 1;   // row 0 is the headings
            int base = (seen % across) * 3;
            seen++;

            gc.gridx = base;
            panel.add(chip(() -> ChartPalette.colorFor(t.label),
                hex -> ChartPalette.setOverride(t.label, hex),
                t.label + " colour",
                "<html>The colour " + t.label + " is drawn in."
                    + "<br>Click to choose another; right-click to go back to the "
                    + "template.</html>",
                () -> box.setForeground(ChartPalette.colorFor(t.label))), gc);
            gc.gridx = base + 1;
            panel.add(box, gc);
            gc.gridx = base + 2;
            gc.insets = new Insets(1, 0, 1, 18);
            panel.add(capSpinner(t), gc);
            gc.insets = new Insets(1, 0, 1, 6);
        }
        return panel;
    }

    /**
     * The orb spinner beside one aspect: the ceiling it is judged under.
     *
     * <b>A ceiling over the points' width, not a second copy of it.</b> A pair is judged at the
     * wider of its two points - that is the H1 grid lower down - and this is the most that width
     * may reach for this aspect. Narrowing it is the only direction: {@code Settings.setAspectCap}
     * clamps, and {@code Aspects.setCustomCaps} refuses outright, which is the same split between
     * screen and engine the point widths use.
     *
     * <b>The Ptolemaic five sit at {@code MAX_BODY_ORB} by default and do not bind there.</b> No
     * point may be set wider than that, so leaving one alone is the same as the ceiling it never
     * had. {@code OrbCheck} walks every ordered pair to hold that, because it is the whole reason
     * the five could be given a number at all.
     */
    private javax.swing.JSpinner capSpinner(final Aspects.Type t) {
        final javax.swing.JSpinner s = new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(Settings.aspectCap(t, shownProfile),
                Aspects.MIN_BODY_ORB, Aspects.defaultCapOf(t, shownProfile), 0.25));
        s.setToolTipText(capTip(t));
        java.awt.Dimension size = new java.awt.Dimension(64, s.getPreferredSize().height);
        s.setPreferredSize(size);
        s.setMaximumSize(size);
        s.addChangeListener(e -> {
            if (constructing || seeding) {
                return;
            }
            Settings.setAspectCap(t, ((Number) s.getValue()).doubleValue(), shownProfile);
            markCapChanged(s, t);
            status.setText("Saved");
            if (window != null) {
                window.applyBodySelection();
            }
        });
        markCapChanged(s, t);
        aspectCapSpinners[t.ordinal()] = s;
        return s;
    }

    /** The Orb column's own tooltip, naming the preset the column is showing. */
    private String orbHeaderTip() {
        return "<html>How close an aspect to this point must be to exact, in degrees."
            + "<br><br>A pair is judged at the <b>wider</b> of its two points, under whatever"
            + "<br>ceiling the aspect itself carries - so widening Pluto does not widen"
            + "<br>a semisextile."
            + "<br><br>Showing <b>" + profileLabel(shownProfile) + "</b>. "
            + profileSentence() + "</html>";
    }

    /** The Ceiling column's own tooltip, naming the preset the column is showing. */
    private String capHeaderTip() {
        return "<html>The most a pair may be apart and still count as this aspect."
            + "<br><br>It is a <b>ceiling over the points' own width</b>, not a second copy of"
            + "<br>it: the pair is judged at the wider of its two points, and then held to"
            + "<br>this. Tightening only - it cannot be lifted."
            + "<br><br>Showing <b>" + profileLabel(shownProfile) + "</b>. "
            + profileSentence() + "</html>";
    }

    /** One point's tooltip, in whichever profile is on screen. */
    private String orbTip(String name) {
        return "<html><b>" + name + "</b> orb, in degrees, under <b>"
            + profileLabel(shownProfile) + "</b>"
            + "<br>How close an aspect to " + name + " must be to exact."
            + "<br>Starts at " + degreeText(Aspects.defaultBodyOrb(name, shownProfile))
            + "&deg; here. A pair is judged at the wider of its"
            + "<br>two points, under whatever ceiling the aspect itself carries."
            + "<br><br>" + profileSentence() + "</html>";
    }

    /** One aspect's tooltip, in whichever profile is on screen. */
    private String capTip(Aspects.Type t) {
        double top = Aspects.defaultCapOf(t, shownProfile);
        return "<html><b>" + t.label + "</b> ceiling, in degrees, under <b>"
            + profileLabel(shownProfile) + "</b>"
            + "<br>The most a pair may be apart and still count as this aspect."
            + "<br>Starts at " + degreeText(top) + "&deg; here, and may only be tightened."
            + (t.isMinor()
                ? "<br><br>A minor aspect carries its own narrow ceiling."
                : "<br><br>A Ptolemaic aspect takes its width from the two points, so"
                    + "<br>at " + degreeText(top) + "&deg; this ceiling only binds where a point is"
                    + " set wider.")
            + "</html>";
    }

    /**
     * What the shown profile governs, in one sentence.
     *
     * <b>Written once and used three times</b> - the tooltips, the column header and the note
     * under the Orbs heading - because this project's recurring defect is one rule spelled out in
     * several places and the copies drifting apart. isMinor was written three times and ringWord
     * six, and both were found only when a suite went red.
     */
    private String profileSentence() {
        switch (shownProfile) {
            case TRANSIT:
                return "These are the <b>transit</b> widths: the event lists, what the sky is "
                    + "doing to a chart and when. Every point starts at one flat "
                    + degreeText(com.zodiacomputing.ourania.astro.Transits.DEFAULT_ORB) + "&deg;, "
                    + "because the natal table keeps a Pluto conjunction open for over fourteen "
                    + "years. <b>The wheel does not use these</b> - it draws sky-to-person "
                    + "aspects at the natal widths, so a slow contact stays visible while it "
                    + "lasts.";
            case SYNASTRY:
                return "These are the <b>synastry</b> widths, for one chart read against "
                    + "another. Each starts at <b>half its natal width</b> - half of whatever "
                    + "you have set, not half of the built-in table - and setting one here "
                    + "stops that point following natal. The rest go on following.";
            case COMPOSITE:
                return "These are the <b>composite</b> widths, for the single chart derived "
                    + "from two people's midpoints. Each starts at <b>its natal width</b>, "
                    + "because a composite is one chart and not a comparison between two. "
                    + "Setting one here stops that point following natal.";
            default:
                return "These are the <b>natal</b> widths: the wheel, the aspect grid and the "
                    + "readings. The other three presets are measured from them, so widening a "
                    + "point here widens it everywhere that has not been set on its own.";
        }
    }

    /** A width as a reader would write it - 6 rather than 6.0, 4.5 left alone. */
    private static String degreeText(double degrees) {
        return Math.abs(degrees - Math.rint(degrees)) < 1e-9
            ? String.valueOf((long) Math.rint(degrees))
            : String.valueOf(degrees);
    }

    /** Bold while an aspect is not at its built-in ceiling, as the point widths are. */
    private void markCapChanged(javax.swing.JSpinner spinner, Aspects.Type t) {
        boolean changed = Math.abs(Settings.aspectCap(t, shownProfile)
            - Aspects.defaultCapOf(t, shownProfile)) > 1e-9;
        java.awt.Component editor = spinner.getEditor();
        if (editor instanceof javax.swing.JSpinner.DefaultEditor) {
            javax.swing.JTextField field =
                ((javax.swing.JSpinner.DefaultEditor) editor).getTextField();
            field.setFont(field.getFont().deriveFont(changed ? java.awt.Font.BOLD
                : java.awt.Font.PLAIN));
        }
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

    /** A labelled width spinner for one of the aspect-line weights. */
    private JPanel weightRow(String label, String key, double now, String hover) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setForeground(TEXT);
        l.setFont(Theme.BODY);
        final javax.swing.JSpinner s = new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(now, 0.1, 8.0, 0.1));
        s.setToolTipText("<html>" + hover + "<br><i>In pixels.</i></html>");
        s.addChangeListener(e -> {
            Settings.set(key, String.valueOf(((Number) s.getValue()).doubleValue()));
            status.setText("Saved");
            applyPalette();
        });
        row.add(l);
        row.add(s);
        row.setMaximumSize(row.getPreferredSize());
        return row;
    }

    /** The one place a checkbox on this screen is dressed. */
    private void styleCheck(JCheckBox b) {
        b.setForeground(TEXT);
        b.setBackground(Color.BLACK);
        b.setFont(Theme.BODY);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        b.setFont(Theme.font("Arial", Font.PLAIN, 12));
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    /**
     * One of the globe's fill switches, built the way the sign-shell box beside it is built.
     *
     * Written once rather than three times because the three differ only in their words and the
     * setting they write, and a fourth band with a fill would otherwise be a fourth copy.
     */
    private JCheckBox globeFillBox(String label, boolean on,
                                   java.util.function.Consumer<Boolean> setter, String tip) {
        final JCheckBox box = new JCheckBox(label, on);
        box.setForeground(TEXT);
        box.setBackground(Color.BLACK);
        box.setFont(Theme.BODY);
        box.setFocusPainted(false);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setToolTipText(tip);
        box.addItemListener(e -> {
            setter.accept(box.isSelected());
            applyPalette();
        });
        return box;
    }

    /** A section's own All or None, moving only that section's boxes. */
    private JButton groupButton(String text, final Bodies.Group group, final boolean on) {
        JButton b = bulkButton(text, () -> setGroup(group, on));
        b.setFont(Theme.font("Arial", Font.PLAIN, 11));
        b.setToolTipText((on ? "Select every point in " : "Clear every point in ")
            + group.title);
        return b;
    }

    /**
     * Sets every box in one section, then applies once.
     *
     * Matched on the registry's group rather than on which panel a box happens to sit in, so
     * the section and the button cannot disagree about membership: the panel is built from the
     * same test.
     */
    void setGroup(Bodies.Group group, boolean on) {
        bulkUpdate = true;
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] != null && Bodies.at(i).group == group) {
                boxes[i].setSelected(on);
            }
        }
        bulkUpdate = false;
        save();
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

        // The zodiac: every position in the app moves with it, not only the wheel - see
        // Ephemeris.flags. Saved and applied at once, like the node and Lilith beside it.
        String[] zodiacOpts = new String[com.zodiacomputing.ourania.astro.Ephemeris.ZODIACS.length];
        for (int i = 0; i < zodiacOpts.length; i++) {
            zodiacOpts[i] = com.zodiacomputing.ourania.astro.Ephemeris.ZODIACS[i][0];
        }
        JComboBox<String> zodiacCombo = new JComboBox<>(zodiacOpts);
        zodiacCombo.setSelectedItem(com.zodiacomputing.ourania.astro.Ephemeris.zodiacLabel());
        zodiacCombo.setToolTipText("<html>Tropical measures the signs from the equinox; sidereal "
            + "from the stars, which have drifted about 24&deg; since the two agreed.<br>"
            + "Changes every position in the app: the wheel, readings, transits and searches.</html>");
        zodiacCombo.addActionListener(e -> {
            Settings.setZodiac((String) zodiacCombo.getSelectedItem());
            status.setText("Saved");
            if (window != null) window.applyBodySelection();
        });
        JLabel lZodiac = new JLabel("Zodiac: ");
        lZodiac.setForeground(TEXT);
        row.add(lZodiac);
        row.add(zodiacCombo);

        // <b>The Transits preset's default width</b>, and the only control the Transit Search
        // and Calendar read. Stage 3: the per-point spinners on the Transits preset override it
        // one point at a time, so this is a default with exceptions and not a second control over
        // the same number. Saved and applied at once, like the zodiac.
        transitOrb = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(
            Settings.transitOrb(), Settings.TRANSIT_ORB_MIN, Settings.TRANSIT_ORB_MAX, 0.25));
        transitOrb.setToolTipText("<html>How close a transit must be to exact to count, in degrees "
            + "either side.<br>The width <b>every</b> transiting point uses: the Report and "
            + "Synthesis lists,<br>the Transit Search and the Transit Calendar."
            + "<br><br>To give one point a wider transit orb than the rest, set it on the"
            + "<br><b>Transits</b> preset under Orbs; this stays the default for everything"
            + "<br>you have not set there."
            + "<br><br>Default 1&deg;. A convention, not a measured value: see the note on "
            + "Transits.orb.</html>");
        transitOrb.addChangeListener(e -> {
            Settings.setTransitOrb(((Number) transitOrb.getValue()).doubleValue());
            status.setText("Saved");
            if (window != null) window.applyBodySelection();
        });
        JLabel lOrb = new JLabel("Transit orb, all points (°): ");
        lOrb.setForeground(TEXT);
        row.add(lOrb);
        row.add(transitOrb);

        // Which rule progresses the Ascendant and MC. Named in the reading that uses it, so the
        // three answers are the reader's choice rather than a hidden convention.
        progressedAngles = new JComboBox<>(
            com.zodiacomputing.ourania.astro.ProgressedAngles.Method.values());
        progressedAngles.setSelectedItem(Settings.progressedAngleMethod());
        progressedAngles.setToolTipText("<html><b>How the progressed Ascendant and MC move.</b><br>"
            + "Solar arc: by the arc the progressed Sun has travelled, about a degree a year.<br>"
            + "Naibod: by the Sun's mean motion, 59'08\" a year, an even rate.<br>"
            + "Quotidian: the real angles of the progressed moment, a degree a day of life - shown "
            + "as positions only, because they aspect everything several times a year.</html>");
        progressedAngles.addActionListener(e -> {
            Settings.setProgressedAngleMethod(
                (com.zodiacomputing.ourania.astro.ProgressedAngles.Method)
                    progressedAngles.getSelectedItem());
            status.setText("Saved");
            if (window != null) window.applyBodySelection();
        });
        JLabel lProg = new JLabel("Progressed angles: ");
        lProg.setForeground(TEXT);
        row.add(lProg);
        row.add(progressedAngles);

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

    /**
     * What is left of H1's screen once every width sits beside what it belongs to: the notes,
     * and the one Reset.
     *
     * <b>No control of its own any more.</b> The point widths are beside each point's tick and
     * colour chip (see {@link #orbSpinner}) and the aspect ceilings are beside each aspect (see
     * {@link #capSpinner}), because in both cases those are one decision about one thing. What
     * stays here is the sentence explaining how the two combine, which belongs to neither list,
     * and the button that puts both back - which is why the spinners are reached through fields.
     */
    private JPanel natalOrbs() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.BLACK);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(heading("Orbs"));
        p.add(note("Every width is set beside the thing it belongs to. Each point's own orb is "
            + "the spinner next to its tick and colour, above; each aspect's ceiling is the "
            + "spinner next to that aspect. A pair is judged at the wider of its two points, "
            + "under whatever ceiling the aspect carries - so widening Pluto does not widen a "
            + "semisextile. Bold means you have changed it."));
        p.add(note("A Ptolemaic aspect sits at "
            + com.zodiacomputing.ourania.astro.Aspects.MAX_BODY_ORB
            + "\u00b0, which cannot bind, so its width is whatever the two points allow."));
        orbProfileNote = note(profileSentence());
        p.add(orbProfileNote);
        p.add(Box.createRigidArea(new Dimension(0, 8)));

        // <b>The Reset button is not here.</b> It puts a whole preset back - the point
        // widths AND the aspect ceilings - and those live on two different tabs, so on either
        // one it would be a control silently reaching into the other. It is built in
        // profileBar(), beside the toggles that decide which preset it acts on.
        refreshProfileWording();
        p.add(Box.createRigidArea(new Dimension(0, 14)));
        return p;
    }

    /** The button that puts one preset back, built where it belongs: beside the preset bar. */
    private javax.swing.JButton orbReset() {
        final javax.swing.JButton orbResetButton =
            new javax.swing.JButton("Reset Natal to defaults");
        this.orbResetButtons.add(orbResetButton);
        orbResetButton.addActionListener(e -> {
            Settings.resetBodyOrbs(shownProfile);
            Settings.resetAspectCaps(shownProfile);
            seeding = true;
            try {
                for (int i = 0; i < orbSpinners.length; i++) {
                    if (orbSpinners[i] == null) {
                        continue;
                    }
                    String name = Bodies.at(i).name;
                    orbSpinners[i].setValue(Settings.bodyOrb(name, shownProfile));
                    markChanged(orbSpinners[i], name);
                }
                if (aspectCapSpinners != null) {
                    for (com.zodiacomputing.ourania.astro.Aspects.Type t
                            : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
                        javax.swing.JSpinner s = aspectCapSpinners[t.ordinal()];
                        if (s == null) {
                            continue;
                        }
                        retuneCapSpinner(s, t);
                        markCapChanged(s, t);
                    }
                }
            } finally {
                seeding = false;
            }
            status.setText(profileLabel(shownProfile) + " orbs reset");
            if (window != null) {
                window.applyBodySelection();
            }
        });
        return orbResetButton;
    }

    /**
     * Bold while a point is not at its built-in width, so a changed set is readable at a glance.
     *
     * <b>On the spinner rather than on a label.</b> There is no label any more - the checkbox
     * carries the name - and bolding that would read as "selected" beside a tick that means
     * exactly that. The mark belongs on the control it describes, as it does for the aspects.
     */
    private void markChanged(javax.swing.JSpinner spinner, String name) {
        // <b>Against the shown profile's default, not natal's.</b> Synastry starts at half
        // the natal width, so comparing with defaultBodyOrb would show every untouched point in
        // bold the moment the reader switched - "you changed this" about a table they had never
        // opened.
        boolean changed = Math.abs(Settings.bodyOrb(name, shownProfile)
            - Aspects.defaultBodyOrb(name, shownProfile)) > 1e-9;
        java.awt.Component editor = spinner.getEditor();
        if (editor instanceof javax.swing.JSpinner.DefaultEditor) {
            javax.swing.JTextField field =
                ((javax.swing.JSpinner.DefaultEditor) editor).getTextField();
            field.setFont(field.getFont().deriveFont(changed ? java.awt.Font.BOLD
                : java.awt.Font.PLAIN));
        }
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(Theme.font("Arial", Font.BOLD, 17));
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
        l.setFont(Theme.font("Arial", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
