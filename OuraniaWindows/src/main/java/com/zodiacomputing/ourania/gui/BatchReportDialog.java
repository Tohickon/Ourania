package com.zodiacomputing.ourania.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Choose charts, choose a folder, get a report for each (B8).
 *
 * <p><b>The list is the chart book, and it is asked every time this opens</b> rather than held -
 * a dialog showing charts that were deleted while it was closed would offer work it cannot do,
 * and {@link BatchReports} would then have to explain a row the reader can see in front of them.
 *
 * <p><b>Cancel stops before the next chart, not in the middle of one.</b> Interrupting a report
 * half way through writing leaves a broken PDF under a name the reader will later open; stopping
 * between charts leaves a folder of complete reports and a list of what was not reached.
 */
final class BatchReportDialog extends JDialog {

    private final OuraniaWindow window;
    private final JList<String> list;
    private final JLabel folderLabel = new JLabel("No folder chosen");
    private final JProgressBar bar = new JProgressBar();
    private final JTextArea log = new JTextArea(8, 44);
    private final JButton start = new JButton("Write the reports");
    private final JButton stop = new JButton("Stop");

    private File folder;
    private volatile boolean cancelled;
    private volatile boolean running;

    BatchReportDialog(OuraniaWindow window) {
        super(window, "Reports for many charts", true);
        this.window = window;

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String name : SavedCharts.names()) {
            model.addElement(name);
        }
        this.list = new JList<>(model);
        this.list.setVisibleRowCount(12);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel top = new JLabel(model.isEmpty()
            ? "There are no charts in the chart book yet."
            : "Choose the charts to write a report for:");
        top.setAlignmentX(LEFT_ALIGNMENT);
        body.add(top);
        body.add(Box.createRigidArea(new Dimension(0, 6)));

        JScrollPane charts = new JScrollPane(this.list);
        charts.setAlignmentX(LEFT_ALIGNMENT);
        charts.setPreferredSize(new Dimension(420, 190));
        body.add(charts);
        body.add(Box.createRigidArea(new Dimension(0, 4)));

        JPanel pick = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pick.setAlignmentX(LEFT_ALIGNMENT);
        JButton all = new JButton("Select all");
        all.addActionListener(e -> this.list.setSelectionInterval(0, model.getSize() - 1));
        all.setEnabled(!model.isEmpty());
        JButton none = new JButton("Select none");
        none.addActionListener(e -> this.list.clearSelection());
        JButton choose = new JButton("Folder…");
        choose.addActionListener(e -> chooseFolder());
        pick.add(all);
        pick.add(none);
        pick.add(choose);
        pick.add(this.folderLabel);
        this.folderLabel.setFont(this.folderLabel.getFont().deriveFont(Font.ITALIC));
        body.add(pick);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        this.bar.setStringPainted(true);
        this.bar.setAlignmentX(LEFT_ALIGNMENT);
        body.add(this.bar);
        body.add(Box.createRigidArea(new Dimension(0, 6)));

        this.log.setEditable(false);
        this.log.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(this.log);
        logScroll.setAlignmentX(LEFT_ALIGNMENT);
        body.add(logScroll);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        this.stop.setEnabled(false);
        this.stop.addActionListener(e -> {
            this.cancelled = true;
            this.stop.setEnabled(false);
            say("Stopping after this one…");
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> {
            if (!this.running) {
                dispose();
            }
        });
        this.start.addActionListener(e -> begin());
        buttons.add(this.stop);
        buttons.add(this.start);
        buttons.add(close);

        setLayout(new BorderLayout());
        add(body, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(window);
    }

    private void chooseFolder() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Where should the reports go?");
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            this.folder = fc.getSelectedFile();
            this.folderLabel.setText(this.folder.getAbsolutePath());
        }
    }

    private void begin() {
        final List<String> names = new ArrayList<>(this.list.getSelectedValuesList());
        if (names.isEmpty()) {
            say("Choose at least one chart first.");
            return;
        }
        if (this.folder == null || !this.folder.isDirectory()) {
            say("Choose a folder for the reports first.");
            return;
        }
        this.cancelled = false;
        this.running = true;
        this.start.setEnabled(false);
        this.stop.setEnabled(true);
        this.bar.setMaximum(names.size());
        this.bar.setValue(0);
        say("Writing " + names.size() + (names.size() == 1 ? " report…" : " reports…"));

        // <b>Off the event thread.</b> The batch waits on two asynchronous stages per chart; run
        // on the EDT it would block the very dialog it is reporting into, and a frozen progress
        // bar is how a working batch looks like a hung one.
        Thread worker = new Thread(() -> {
            BatchReports.Run run = BatchReports.run(this.window, names, this.folder,
                new BatchReports.Progress() {
                    @Override
                    public boolean cancelled() {
                        return BatchReportDialog.this.cancelled;
                    }

                    @Override
                    public void starting(int done, int total, String chart) {
                        SwingUtilities.invokeLater(() -> {
                            bar.setValue(done);
                            bar.setString(chart + "  (" + (done + 1) + " of " + total + ")");
                        });
                    }

                    @Override
                    public void finished(BatchReports.Outcome outcome) {
                        SwingUtilities.invokeLater(() -> {
                            say("  " + outcome);
                            bar.setValue(bar.getValue() + 1);
                        });
                    }
                });
            SwingUtilities.invokeLater(() -> {
                this.running = false;
                this.start.setEnabled(true);
                this.stop.setEnabled(false);
                this.bar.setString(run.summary());
                say(run.summary());
                if (run.written() > 0) {
                    say("In " + this.folder.getAbsolutePath());
                }
            });
        }, "ourania-batch-reports");
        worker.setDaemon(true);
        worker.start();
    }

    private void say(String line) {
        this.log.append(line + "\n");
        this.log.setCaretPosition(this.log.getDocument().getLength());
    }

    /** Opens it, unless the chart book is empty and there is nothing to offer. */
    static void open(OuraniaWindow window) {
        if (SavedCharts.names().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(window,
                "There are no saved charts to write reports for yet.\n"
                    + "Charts are saved from the Chart Setup screen.",
                "Reports for many charts", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new BatchReportDialog(window).setVisible(true);
    }
}
