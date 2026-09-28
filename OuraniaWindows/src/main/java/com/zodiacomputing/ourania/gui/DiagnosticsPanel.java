package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AppPaths;
import com.zodiacomputing.ourania.astro.Ephemeris;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The door for what start-up found, and for sending a problem on (J8, J9).
 *
 * <p><b>Written because the two rows above it could not close without one.</b> J9 had nothing
 * checking the ephemeris, the data files or the settings before the window opens; J8 had a log
 * nobody was ever told about and nothing to hand to a second person. Building the checks and the
 * report without a way in would have been this project's most logged finding volunteering for
 * another repeat - the thing exists and has no door.
 *
 * <p><b>It re-inspects every time it is shown, rather than displaying what start-up found.</b> A
 * reader who drops the missing ephemeris file into place and comes back here should see it change,
 * and a panel showing a snapshot from before they acted would tell them their fix had not worked.
 * {@link Startup#inspect} changes nothing, so there is no cost to asking again.
 *
 * <p><b>Its own file rather than a sixth section of {@code SettingsPanel}</b>, which is 3,000
 * lines and is on J13's list for being one.
 */
final class DiagnosticsPanel extends JPanel {

    private static final Color TEXT = new Color(220, 220, 220);
    private static final Color DIM = new Color(150, 150, 150);
    private static final Color BAD = new Color(232, 131, 112);

    /** Where the findings are rebuilt into, so re-inspecting replaces them and nothing else. */
    private final JPanel findings = new JPanel();

    /** What the last action said, under the buttons. Empty until something is done. */
    private final JLabel said = new JLabel(" ");

    DiagnosticsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 30, 20, 30));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        add(heading("This build"));
        add(note("Version <b>" + esc(Version.display()) + "</b>"
            + "<br>Running " + (AppPaths.packaged() ? "from Ourania.jar" : "from classes")
            + "<br>Ephemeris: " + esc(Ephemeris.PATH)
            + "<br>Data: " + esc(InterpretationService.DATA_DIR)
            + "<br>Your files: " + esc(new java.io.File(AppPaths.userDir()).getAbsolutePath())
            + "<br>Problems are written to: " + esc(ErrorLog.file().getPath())));
        add(Box.createRigidArea(new Dimension(0, 14)));

        add(heading("What start-up found"));
        findings.setLayout(new BoxLayout(findings, BoxLayout.Y_AXIS));
        findings.setBackground(Color.BLACK);
        findings.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(findings);
        refresh();

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(Color.BLACK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton again = new JButton("Check again");
        again.setToolTipText("Look at the ephemeris, the data files and your settings again, now. "
            + "Nothing is changed by looking.");
        Widgets.styleButton(again, Widgets.Role.TRANSPORT);
        again.addActionListener(e -> {
            refresh();
            say("Checked again.", false);
        });

        JButton report = new JButton("Save a problem report");
        report.setToolTipText("Write one file holding this build's version, where it is reading "
            + "from, what start-up found and the end of the log - to send to somebody who can "
            + "look at it. It holds no birth data, no saved charts and no place names.");
        Widgets.styleButton(report, Widgets.Role.PRIMARY);
        report.addActionListener(e -> saveReport());

        JButton open = new JButton("Open the folder");
        open.setToolTipText("Show the folder holding the log and any reports you have saved.");
        Widgets.styleButton(open, Widgets.Role.TRANSPORT);
        open.addActionListener(e -> openFolder());

        row.add(again);
        row.add(Box.createRigidArea(new Dimension(8, 0)));
        row.add(report);
        row.add(Box.createRigidArea(new Dimension(8, 0)));
        row.add(open);
        row.add(Box.createHorizontalGlue());
        add(Box.createRigidArea(new Dimension(0, 12)));
        add(heading("If something goes wrong"));
        add(note("A problem report is a file on this machine. Nothing is sent anywhere unless "
            + "you attach it to something yourself."));
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(row);

        said.setForeground(DIM);
        said.setFont(Theme.font("Arial", Font.ITALIC, 12));
        said.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(said);
    }

    /** Runs the inspection again and redraws the list. */
    void refresh() {
        findings.removeAll();
        java.util.List<Startup.Finding> found;
        try {
            found = Startup.inspect();
        } catch (Throwable t) {
            ErrorLog.record("diagnostics inspection failed", t);
            found = java.util.Collections.emptyList();
        }
        if (found.isEmpty()) {
            findings.add(note("Nothing to report."));
        }
        for (Startup.Finding f : found) {
            boolean bad = f.level == Startup.Level.DEGRADED;
            JLabel l = note("<b>" + (bad ? "&#9888; " : "") + esc(f.what) + "</b><br>"
                + esc(f.detail));
            l.setForeground(bad ? BAD : DIM);
            findings.add(l);
            findings.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        findings.revalidate();
        findings.repaint();
    }

    /**
     * Writes a report and says where it went.
     *
     * <b>It says the path rather than opening the file.</b> A reader is about to attach this to
     * something, so what they need is where it is; opening a text editor over the app is a second
     * window nobody asked for.
     */
    private void saveReport() {
        java.io.File f = CrashReport.write("Saved from Diagnostics.", null);
        if (f == null) {
            say("The report could not be written. " + ErrorLog.file().getPath()
                + " says why.", true);
        } else {
            say("Saved " + f.getPath(), false);
        }
    }

    /** Shows the folder holding the log and any reports, where the platform allows it. */
    private void openFolder() {
        java.io.File dir = ErrorLog.file().getAbsoluteFile().getParentFile();
        try {
            if (dir != null && dir.isDirectory()
                && java.awt.Desktop.isDesktopSupported()
                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(dir);
                say("Opened " + dir.getPath(), false);
                return;
            }
        } catch (Throwable t) {
            ErrorLog.record("could not open the log folder", t);
        }
        // <b>The path, when the folder will not open.</b> A button that does nothing and says
        // nothing is the defect this screen has carried twice.
        say(dir == null ? "There is no folder to open." : dir.getPath(), true);
    }

    private void say(String text, boolean bad) {
        said.setText(text);
        said.setForeground(bad ? BAD : DIM);
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(Theme.font("Arial", Font.BOLD, 17));
        // Marked so Settings search files the controls below under this heading rather than
        // under whatever heading opens the tab; see SettingsPanel.heading.
        l.putClientProperty(SettingsSearch.HEADING, Boolean.TRUE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 12));
        return l;
    }

    private JLabel note(String html) {
        JLabel l = new JLabel("<html><body style='width:600px'>" + html + "</body></html>");
        l.setForeground(DIM);
        l.setFont(Theme.font("Arial", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** These labels are HTML, and a Windows path is not. */
    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
