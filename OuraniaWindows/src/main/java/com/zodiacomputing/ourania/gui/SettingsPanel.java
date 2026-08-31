package com.zodiacomputing.ourania.gui;

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
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.BLACK);
        body.setBorder(BorderFactory.createEmptyBorder(0, 30, 20, 30));

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
            panel.add(box);
        }
        return panel;
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
        if (bulkUpdate) return;
        boolean[] enabled = new boolean[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            enabled[i] = boxes[i].isSelected();
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
