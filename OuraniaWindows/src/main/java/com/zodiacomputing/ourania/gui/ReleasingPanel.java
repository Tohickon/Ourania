package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.astro.ZodiacalReleasing;
import de.thmac.swisseph.SweDate;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

public class ReleasingPanel extends JPanel {
    private OuraniaWindow parentWindow;
    private ChartFrame currentChart;
    
    private JComboBox<String> lotSelector;
    private JTree tree;
    private DefaultTreeModel treeModel;
    
    public ReleasingPanel(OuraniaWindow window) {
        this.parentWindow = window;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(Color.BLACK);
        
        JLabel titleLabel = new JLabel("Zodiacal Releasing");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(Theme.font("Arial", Font.BOLD, 24));
        
        String[] lots = {"Lot of Spirit (Career/Action)", "Lot of Fortune (Body/Circumstance)"};
        lotSelector = new JComboBox<>(lots);
        lotSelector.addActionListener(e -> refreshTree());
        
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        JLabel releaseLabel = new JLabel("Release from: ");
        releaseLabel.setForeground(Color.WHITE);
        headerPanel.add(releaseLabel);
        headerPanel.add(lotSelector);
        
        add(headerPanel, BorderLayout.NORTH);
        
        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("No Data"));
        tree = new JTree(treeModel);
        tree.setBackground(new Color(20, 20, 20));
        tree.setCellRenderer(new PeriodCellRenderer());
        tree.setRowHeight(35);
        tree.setShowsRootHandles(true);
        
        JScrollPane treeScrollPane = new JScrollPane(tree);
        treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        JEditorPane detailsArea = new JEditorPane();
        detailsArea.setContentType("text/html");
        detailsArea.setEditable(false);
        detailsArea.setBackground(Color.BLACK);
        detailsArea.setForeground(Color.WHITE);
        detailsArea.setText("<html><body style='font-family: sans-serif; font-size: 14px; margin: 15px; color: #E0E0E0; background-color: #000000;'>Select a period to see interpretation.</body></html>");
        
        JScrollPane detailsScrollPane = new JScrollPane(detailsArea);
        detailsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, detailsScrollPane);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(4);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        add(splitPane, BorderLayout.CENTER);
        
        tree.addTreeSelectionListener(e -> {
            if (currentChart == null) return;
            javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null || !(node.getUserObject() instanceof ZodiacalReleasing.Period)) return;
            
            ZodiacalReleasing.Period p = (ZodiacalReleasing.Period) node.getUserObject();
            boolean isSpirit = lotSelector.getSelectedIndex() == 0;
            
            com.zodiacomputing.ourania.astro.Gestalt.Result g = com.zodiacomputing.ourania.astro.Gestalt.compute(currentChart);
            java.util.List<com.zodiacomputing.ourania.astro.BodyScore.Vector> ranked = com.zodiacomputing.ourania.astro.BodyScore.rank(currentChart, g);
            
            String html = com.zodiacomputing.ourania.astro.Snapshot.releasingPeriod(currentChart, g, ranked, p, isSpirit);
            detailsArea.setText(html);
            detailsArea.setCaretPosition(0);
        });
    }
    
    public void setChart(ChartFrame chart) {
        this.currentChart = chart;
        refreshTree();
    }
    
    private void refreshTree() {
        if (currentChart == null) return;
        
        boolean useSpirit = lotSelector.getSelectedIndex() == 0;
        double spiritLon = currentChart.lotOfSpirit;
        double fortuneLon = currentChart.lotOfFortune;
        double lotLon = useSpirit ? spiritLon : fortuneLon;
        
        // Calculate up to Level 4, spanning 100 years.
        double untilJd = currentChart.julianDayUt + (100 * 365.2422); 
        List<ZodiacalReleasing.Period> periods = ZodiacalReleasing.release(
                currentChart.julianDayUt, lotLon, spiritLon, untilJd, 4);
                
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Zodiacal Releasing (100 Years)");
        for (ZodiacalReleasing.Period p : periods) {
            root.add(buildNode(p));
        }
        
        treeModel.setRoot(root);
        tree.expandRow(0);
    }
    
    private DefaultMutableTreeNode buildNode(ZodiacalReleasing.Period p) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(p);
        for (ZodiacalReleasing.Period child : p.children) {
            node.add(buildNode(child));
        }
        return node;
    }
    
    private static class PeriodCellRenderer extends DefaultTreeCellRenderer {
        private JPanel panel = new JPanel(new BorderLayout());
        private JLabel textLabel = new JLabel();
        private JPanel colorBar = new JPanel();
        
        public PeriodCellRenderer() {
            panel.setOpaque(false);
            textLabel.setForeground(Color.WHITE);
            textLabel.setFont(Theme.font("Arial", Font.PLAIN, 14));
            
            colorBar.setPreferredSize(new Dimension(15, 25));
            colorBar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            
            panel.add(colorBar, BorderLayout.WEST);
            panel.add(textLabel, BorderLayout.CENTER);
        }
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (node.getUserObject() instanceof ZodiacalReleasing.Period) {
                ZodiacalReleasing.Period p = (ZodiacalReleasing.Period) node.getUserObject();
                
                SweDate sd = new SweDate(p.startJd);
                String dateStr = String.format("%04d-%02d-%02d", sd.getYear(), sd.getMonth(), sd.getDay());
                
                String label = String.format("  [%s] L%d %s (%.1f %s)%s%s",
                        dateStr, p.level, capitalize(Zodiac.SIGNS[p.sign]), p.units, 
                        (p.level == 1 ? "yr" : (p.level == 2 ? "mo" : "d")),
                        p.peak ? " ⭐ PEAK" : "",
                        p.afterBond ? " 🔗 BOND LOOSED" : "");
                        
                textLabel.setText(label);
                
                Color bg = getSignColor(p.sign);
                colorBar.setBackground(bg);
                colorBar.setVisible(true);
                
                if (p.peak) {
                    textLabel.setForeground(Color.YELLOW);
                } else if (p.afterBond) {
                    textLabel.setForeground(new Color(255, 80, 80));
                } else {
                    textLabel.setForeground(Color.WHITE);
                }
            } else {
                textLabel.setText("  " + value.toString());
                textLabel.setForeground(Color.LIGHT_GRAY);
                colorBar.setVisible(false);
            }
            
            if (sel) {
                panel.setBackground(new Color(60, 120, 200, 100));
                panel.setOpaque(true);
            } else {
                panel.setOpaque(false);
            }
            
            return panel;
        }
        
        private Color getSignColor(int sign) {
            switch (sign % 4) {
                case 0: return new Color(200, 50, 50); // Fire
                case 1: return new Color(100, 200, 50); // Earth
                case 2: return new Color(200, 200, 50); // Air
                case 3: return new Color(50, 100, 200); // Water
            }
            return Color.GRAY;
        }
        
        private String capitalize(String s) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }
}
