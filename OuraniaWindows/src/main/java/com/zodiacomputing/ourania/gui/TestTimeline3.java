package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;
import de.thmac.swisseph.SwissEph;

public class TestTimeline3 {
    public static void main(String[] args) {
        try {
            System.out.println("Starting test...");
            SwissEph sw = new SwissEph("src/main/resources/ephe");
            ChartFrame cf = ChartFrame.compute(sw, 2460123.5, 34.0, -118.0, 'P', false, 0.0);
            System.out.println("Chart computed.");
            
            String html = TimelinePredictor.generate(cf, null);
            System.out.println("Generated OK. Length: " + html.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
