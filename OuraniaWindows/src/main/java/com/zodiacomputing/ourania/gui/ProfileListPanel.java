package com.zodiacomputing.ourania.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * The saved-chart directory: the people this app knows, and a way to load one.
 *
 * <p><b>This is what "Name List" was supposed to be.</b> The screen carrying that name renders
 * the ACTIVE chart's coordinates - a Body / Longitude / House / Speed table, plus houses and
 * aspects - which is chart data, not a list of names. It duplicates what the sidebar's Natal
 * Chart section already shows, and it never once touched {@link SavedCharts}.
 *
 * <p>Meanwhile {@link SavedCharts} has been a working profile store the whole time, backing
 * Chart Setup's Save and Load buttons and living in {@code saved_charts.properties}. <b>The
 * database existed and had no door.</b> That is the same defect this project logged for the
 * browsable index, whose only two entry points were both inside a panel that had to be open
 * first - a feature that is built, correct, and unreachable.
 *
 * <h2>Loading as A or as B</h2>
 *
 * Each profile offers <b>Chart A</b> and <b>Chart B</b> rather than one Load button, because
 * which slot a person belongs in is the question the relationship modes ask. Chart B is
 * offered always rather than only in a two-person mode: filling it is how a reader gets to a
 * two-person mode in the first place, and a button that appears only once you no longer need
 * it is not a shortcut.
 */
public final class ProfileListPanel extends JPanel {

    private static final Color CARD_BG = Theme.SURFACE_2;
    private static final Color CARD_EDGE = Theme.EDGE;
    private static final Color TEXT = Theme.TEXT;
    private static final Color DIM = Theme.TEXT_DIM;

    private final OuraniaWindow window;
    private final JPanel list;
    private final JTextField search;

    public ProfileListPanel(OuraniaWindow window) {
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);

        search = new JTextField();
        search.setBackground(Theme.SURFACE_3);
        search.setForeground(Color.WHITE);
        search.setCaretColor(Color.WHITE);
        search.setBorder(Theme.card(CARD_EDGE, Theme.GAP_S));
        search.setToolTipText("Filter the saved charts by name");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                rebuild();
            }

            public void removeUpdate(DocumentEvent e) {
                rebuild();
            }

            public void changedUpdate(DocumentEvent e) {
                rebuild();
            }
        });

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setBackground(Theme.SURFACE);
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setForeground(DIM);
        searchLabel.setFont(Theme.SMALL);
        top.add(searchLabel, BorderLayout.NORTH);
        top.add(search, BorderLayout.CENTER);

        list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.SURFACE);
        list.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));

        add(top, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
        rebuild();
    }

    /** Re-reads the store. Called on construction, on every keystroke, and after a save. */
    public void rebuild() {
        list.removeAll();
        String filter = search == null ? "" : search.getText().trim().toLowerCase();

        List<String> names = SavedCharts.names();
        int shown = 0;
        for (String name : names) {
            if (!filter.isEmpty() && !name.toLowerCase().contains(filter)) {
                continue;
            }
            SavedCharts.Entry entry = SavedCharts.get(name);
            if (entry == null) {
                continue;
            }
            list.add(card(name, entry));
            list.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));
            shown++;
        }

        if (shown == 0) {
            // Said out loud, and the two cases are told apart. An empty store and a filter
            // that matches nothing look identical otherwise, and a reader with one saved
            // chart and a typo would conclude their charts were gone.
            JLabel empty = new JLabel(names.isEmpty()
                ? "<html><div style='padding:6px;'>No saved charts yet.<br><br>"
                    + "Chart Setup has a <b>Save</b> button on each side - saving a chart "
                    + "there puts it here.</div></html>"
                : "<html><div style='padding:6px;'>No saved chart matches \"" + filter
                    + "\".<br><br>" + names.size() + " saved in total.</div></html>");
            empty.setForeground(DIM);
            empty.setFont(Theme.SMALL);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(empty);
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel card(final String name, SavedCharts.Entry entry) {
        JPanel card = new JPanel(new BorderLayout(0, 3));
        card.setBackground(CARD_BG);
        card.setBorder(Theme.card(CARD_EDGE, Theme.GAP));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));

        JLabel title = new JLabel(name);
        title.setForeground(TEXT);
        title.setFont(Theme.HEADING);

        JLabel detail = new JLabel(entry.date + "  " + entry.time
            + (entry.location == null || entry.location.trim().isEmpty()
                ? "" : "  -  " + entry.location));
        detail.setForeground(DIM);
        detail.setFont(Theme.SMALL);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        actions.setBackground(CARD_BG);
        actions.add(loadButton("Chart A", name, false,
            "Load " + name + " as the base chart"));
        actions.add(loadButton("Chart B", name, true,
            "Load " + name + " as the second chart - the partner in a synastry or composite, "
                + "or the moment a transit wheel is drawn for"));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(CARD_BG);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(title);
        text.add(detail);

        card.add(text, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JButton loadButton(String label, final String name, final boolean asPartner,
                               String tip) {
        JButton b = new JButton(label);
        b.setToolTipText(tip);
        b.setFont(new Font("Arial", Font.BOLD, 11));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setMargin(new java.awt.Insets(2, 8, 2, 8));
        Widgets.styleButton(b, asPartner ? Widgets.Role.TRANSPORT : Widgets.Role.PRIMARY);
        b.addActionListener(e -> {
            if (window != null) {
                window.loadSavedProfile(name, asPartner);
            }
        });
        return b;
    }

    /** Wrapped for the sidebar, which puts every section in a scroller of its own. */
    public JScrollPane inScroller() {
        JScrollPane sp = new JScrollPane(this);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Widgets.styleScrollPane(sp);
        return sp;
    }
}
