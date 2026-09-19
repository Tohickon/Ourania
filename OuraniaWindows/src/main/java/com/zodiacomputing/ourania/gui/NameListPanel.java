package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class NameListPanel extends JPanel {

    private SkymapPanel skymapPanel;
    private OuraniaWindow window;
    private JEditorPane editorPane;
    private JScrollPane scrollPane;

    public NameListPanel(SkymapPanel skymapPanel, OuraniaWindow window) {
        this.skymapPanel = skymapPanel;
        this.window = window;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerPanel.setBackground(Color.BLACK);
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(205, 92, 92));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(true);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> {
            if (this.window != null) {
                this.window.switchScreen("SKYMAP");
            }
        });
        headerPanel.add(closeBtn);
        add(headerPanel, BorderLayout.NORTH);

        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setBackground(Color.BLACK);

        scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateData() {
        if (skymapPanel == null) return;
        
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='color:#E0E0E0; font-family:Arial; padding: 20px;'>");
        html.append("<h1 style='color:#00BFFF;'>Name List</h1>");

        boolean isTransit = skymapPanel.showTransitChart;
        html.append(buildChartData("Natal Chart", false));

        if (isTransit) {
            html.append("<hr style='border: 1px solid #444; margin-top: 30px; margin-bottom: 30px;'/>");
            html.append(buildChartData("Transit Chart", true));
        }

        html.append("</body></html>");
        editorPane.setText(html.toString());
        editorPane.setCaretPosition(0);
    }

    private String buildChartData(String title, boolean isTransit) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2 style='color:#add8e6;'>").append(title).append("</h2>");

        double[] lonArray = isTransit ? skymapPanel.outerRing.lon : skymapPanel.natalRing.lon;
        double[] speedArray = isTransit ? skymapPanel.outerRing.speed : skymapPanel.natalRing.speed;
        boolean[] validArray = isTransit ? skymapPanel.outerRing.valid : skymapPanel.natalRing.valid;
        double[] cusps = skymapPanel.activeCusps;

        // Placements Table
        sb.append("<h3 style='color:#E0E0E0;'>Placements</h3>");
        sb.append("<table border='1' cellspacing='0' cellpadding='5' style='border-collapse: collapse; border-color: #555; width: 100%; text-align: left;'>");
        sb.append("<tr style='background-color:#333;'><th width='25%'>Body</th><th width='25%'>Longitude</th><th width='25%'>House</th><th width='25%'>Speed</th></tr>");

        for (int i = 0; i < SkymapPanel.BODY_COUNT; i++) {
            if (!validArray[i]) continue;
            Bodies.Def def = Bodies.at(i);
            
            double lon = lonArray[i];
            int signIndex = (int) (lon / 30);
            int degree = (int) (lon % 30);
            int minutes = (int) ((lon - Math.floor(lon)) * 60);
            
            int houseNum = 1;
            for (int h = 1; h <= 12; h++) {
                double cusp = cusps[h];
                double nextCusp = (h == 12) ? cusps[1] : cusps[h+1];
                if (nextCusp < cusp) nextCusp += 360;
                double checkLon = lon;
                if (checkLon < cusp && nextCusp > 360) checkLon += 360;
                if (checkLon >= cusp && checkLon < nextCusp) {
                    houseNum = h;
                    break;
                }
            }

            String signName = SkymapPanel.SIGN_NAMES[signIndex];
            String position = String.format("%d&deg; %s %02d'", degree, signName, minutes);
            
            String speedStr = "-";
            if (!def.isAngle() && def.kind != Bodies.Kind.POINT && def.kind != Bodies.Kind.NODE) {
                speedStr = speedArray[i] < 0 ? "Retrograde" : "Direct";
            }
            
            sb.append("<tr>");
            sb.append("<td>").append(def.name).append(" (").append(def.glyph).append(")</td>");
            sb.append("<td>").append(position).append("</td>");
            sb.append("<td>").append(houseNum).append("</td>");
            sb.append("<td>").append(speedStr).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");

        // Houses Table (only for Natal Chart)
        if (!isTransit) {
            sb.append("<h3 style='color:#E0E0E0; margin-top: 20px;'>Houses</h3>");
            sb.append("<table border='1' cellspacing='0' cellpadding='5' style='border-collapse: collapse; border-color: #555; width: 50%; text-align: left;'>");
            sb.append("<tr style='background-color:#333;'><th width='30%'>House</th><th>Cusp Longitude</th></tr>");
            for (int h = 1; h <= 12; h++) {
                double lon = cusps[h];
                int signIndex = ((int) (lon / 30)) % 12;
                int degree = (int) (lon % 30);
                int minutes = (int) ((lon - Math.floor(lon)) * 60);
                String signName = SkymapPanel.SIGN_NAMES[signIndex];
                String position = String.format("%d&deg; %s %02d'", degree, signName, minutes);
                
                sb.append("<tr>");
                sb.append("<td>House ").append(h).append("</td>");
                sb.append("<td>").append(position).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        // Aspects Table
        sb.append("<h3 style='color:#E0E0E0; margin-top: 20px;'>Active Aspects</h3>");
        sb.append("<table border='1' cellspacing='0' cellpadding='5' style='border-collapse: collapse; border-color: #555; width: 100%; text-align: left;'>");
        sb.append("<tr style='background-color:#333;'><th>Body 1</th><th>Aspect</th><th>Body 2</th></tr>");

        boolean hasAspects = false;
        for (int i = 0; i < SkymapPanel.BODY_COUNT; i++) {
            if (!validArray[i]) continue;
            List<String[]> activeAspects = skymapPanel.getActiveAspectsFor(i, isTransit);
            for (String[] aspect : activeAspects) {
                String otherBody = aspect[0];
                int j = Bodies.indexOfName(otherBody);
                if (j > i) { 
                    hasAspects = true;
                    String aspectType = aspect[1];
                    String state = aspect[2];
                    String displayType = state.isEmpty() ? aspectType : state + " " + aspectType;
                    sb.append("<tr>");
                    sb.append("<td>").append(Bodies.at(i).name).append("</td>");
                    sb.append("<td>").append(displayType).append("</td>");
                    sb.append("<td>").append(otherBody).append("</td>");
                    sb.append("</tr>");
                }
            }
        }
        
        if (!hasAspects) {
            sb.append("<tr><td colspan='3'><i>No active aspects</i></td></tr>");
        }
        sb.append("</table>");

        return sb.toString();
    }
}