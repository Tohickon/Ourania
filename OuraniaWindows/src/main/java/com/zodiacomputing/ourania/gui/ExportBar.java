package com.zodiacomputing.ourania.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * The three buttons under the Full report tab: Copy Markdown, Export HTML, Print / PDF.
 *
 * <b>Reading framework spec, section 7: the same three controls in every mode.</b> Export had been
 * wired to the Reading page's own pane, so the Dossier, the Themes, the Year and Coming up
 * could not be exported at all. The bar belongs to the group, not to a page, and it acts on
 * whichever view is in front (see OuraniaWindow#frontPane).
 */
final class ExportBar extends JPanel {

    static final String COPY_MARKDOWN = "Copy Markdown";
    static final String EXPORT_HTML = "Export HTML";
    static final String PRINT_PDF = "Print / PDF";

    private final List<JButton> buttons = new ArrayList<>();

    ExportBar(Runnable copyMarkdown, Runnable exportHtml, Runnable printPdf) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setBackground(Color.BLACK);
        add(button(COPY_MARKDOWN, "Copy this page to the clipboard as Markdown.", copyMarkdown));
        add(button(EXPORT_HTML, "Save this page as a web page.", exportHtml));
        add(button(PRINT_PDF, "Print this page with the chart, or save it as a PDF.", printPdf));
    }

    private JButton button(String label, String tip, Runnable action) {
        JButton b = new JButton(label);
        Widgets.styleButton(b, Widgets.Role.TRANSPORT);
        b.setToolTipText(tip);
        b.addActionListener(e -> action.run());
        buttons.add(b);
        return b;
    }

    /** The labels, in order, for the suites. */
    List<String> labels() {
        List<String> out = new ArrayList<>();
        for (JButton b : buttons) {
            out.add(b.getText());
        }
        return out;
    }

    /** Press a button by its label, as a click would, for the suites. */
    void press(String label) {
        for (JButton b : buttons) {
            if (b.getText().equals(label)) {
                b.doClick();
            }
        }
    }
}
