package com.zodiacomputing.ourania.gui;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Turns a plain location box into one that offers the places it knows.
 *
 * <b>A place name had to be spelled the way a geocoder wanted it.</b> The field was free text
 * that went out to Nominatim on submit, so "Philadelpia" was not a typo the form could catch -
 * it was a chart that failed to cast, or worse, one that cast somewhere else. And a reader had
 * no way to tell which London or which Springfield they were about to be given.
 *
 * Now the atlas answers as they type, biggest place first, each one saying which region and
 * country it is in - so choosing between two Londons is a choice the reader makes rather than
 * one made for them by a ranking they never saw.
 *
 * <b>Attached to a field rather than replacing it.</b> The three location boxes are built by
 * one factory and read by name elsewhere; a new component type would have meant changing every
 * one of those readers, and free text still has to work for the places the atlas does not hold.
 * This only adds a list under the box.
 */
final class PlaceField {

    /** How many suggestions a reader can usefully choose between without scanning. */
    private static final int SHOWN = 8;

    private final JTextField field;
    private final JPopupMenu popup = new JPopupMenu();
    private final javax.swing.DefaultListModel<Atlas.Place> model =
        new javax.swing.DefaultListModel<>();
    private final JList<Atlas.Place> list = new JList<>(model);
    /** True while the code is setting the text, so the edit does not reopen the list. */
    private boolean settingText;

    private PlaceField(JTextField field) {
        this.field = field;

        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.SURFACE_3);
        list.setForeground(Theme.TEXT);
        list.setFont(Theme.SMALL);
        list.setSelectionBackground(Theme.ACCENT);
        list.setSelectionForeground(java.awt.Color.WHITE);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> l, Object value,
                    int index, boolean selected, boolean focused) {
                java.awt.Component c = super.getListCellRendererComponent(l, value, index,
                    selected, focused);
                if (value instanceof Atlas.Place) {
                    setText(((Atlas.Place) value).label());
                }
                setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return c;
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                take();
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(360, 26 * SHOWN));
        popup.setBorder(javax.swing.BorderFactory.createLineBorder(Theme.EDGE));
        popup.add(scroll);
        popup.setFocusable(false);          // the caret stays in the field while choosing

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                offer();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                offer();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                offer();
            }
        });

        // <b>The keyboard drives the list without the list taking focus.</b> A reader typing a
        // place name should not have to reach for the mouse, and should not lose the caret.
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popup.isVisible()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    move(1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    move(-1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (list.getSelectedIndex() >= 0) {
                        take();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                    e.consume();
                }
            }
        });
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                popup.setVisible(false);
            }
        });
    }

    /** Offers suggestions under this field. Safe to call on a field the reader never uses. */
    static void attach(JTextField field) {
        if (field != null) {
            new PlaceField(field);
        }
    }

    private void move(int by) {
        int n = model.getSize();
        if (n == 0) {
            return;
        }
        int next = list.getSelectedIndex() + by;
        if (next < 0) {
            next = n - 1;
        }
        if (next >= n) {
            next = 0;
        }
        list.setSelectedIndex(next);
        list.ensureIndexIsVisible(next);
    }

    private void take() {
        Atlas.Place p = list.getSelectedValue();
        if (p == null) {
            return;
        }
        settingText = true;
        try {
            // <b>The coordinates, not the name.</b> A chosen place is unambiguous, and writing
            // it back as a name would send it out to be geocoded again - the round trip this
            // whole thing exists to avoid, and a chance for the answer to come back as a
            // different Springfield. The field already accepts "lat, lon", and the label goes
            // beside it so the reader can still see what they picked.
            field.setText(String.format(java.util.Locale.ROOT, "%.5f, %.5f",
                p.latitude, p.longitude));
            field.setToolTipText(p.label() + "  ·  " + p.zoneId);
            chosen = p;
        } finally {
            settingText = false;
        }
        popup.setVisible(false);
    }

    /** The place the reader picked from the list, if they picked one. */
    private Atlas.Place chosen;

    private void offer() {
        if (settingText) {
            return;
        }
        chosen = null;
        String text = field.getText();
        // Two characters before offering anything: one letter matches thousands of towns, and a
        // list of eight arbitrary ones is noise in front of somebody still typing.
        if (text == null || text.trim().length() < 2 || text.indexOf(',') >= 0
                && text.matches("\\s*-?\\d.*")) {
            popup.setVisible(false);
            return;
        }
        List<Atlas.Place> hits = Atlas.search(text, SHOWN);
        model.clear();
        for (Atlas.Place p : hits) {
            model.addElement(p);
        }
        if (model.isEmpty()) {
            popup.setVisible(false);
            return;
        }
        list.setSelectedIndex(0);
        if (!field.isShowing()) {
            return;
        }
        popup.setPreferredSize(new Dimension(Math.max(320, field.getWidth()),
            Math.min(SHOWN, model.getSize()) * 26 + 6));
        popup.show(field, 0, field.getHeight());
        // The popup grabs no focus, so put the caret back where the reader is typing.
        field.requestFocusInWindow();
    }

    /** For a check: what a field is currently offering, without opening a window. */
    static List<Atlas.Place> suggestionsFor(String typed) {
        return Atlas.search(typed, SHOWN);
    }

    /** Whether a JComponent is one of these boxes, for a check to find them. */
    static boolean isPlaceField(JComponent c) {
        return c instanceof JTextField;
    }
}
