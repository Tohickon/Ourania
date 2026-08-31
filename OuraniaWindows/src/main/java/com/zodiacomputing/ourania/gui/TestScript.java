package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.*;
import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.List;

public class TestScript {
    public static void main(String[] args) throws Exception {
        SwissEph sw = new SwissEph("src/main/resources/ephe");
        double jd = SweDate.getJulDay(2023, 1, 1, 12.0); // Random JD
        ChartFrame f = ChartFrame.compute(sw, jd, 51.5, -0.1, 'P', false, 0.0);
        Gestalt.Result g = Gestalt.compute(f);
        List<BodyScore.Vector> ranked = BodyScore.rank(f, g);
        Themes.Result t = Themes.extract(f, g, ranked);
        
        System.out.println("Running synthesis...");
        try {
            String s = NarrativeSynthesizer.generateReport(f, g, ranked, t, null, null, null, null, null, false);
            System.out.println("Success! Output size: " + s.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}