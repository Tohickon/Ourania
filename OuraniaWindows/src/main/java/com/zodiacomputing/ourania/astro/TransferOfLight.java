package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;

/**
 * L4 Relational Structure: Transfer of Light.
 * 
 * Identifies classical translation and collection of light using actual astronomical
 * planetary speeds rather than generic order.
 */
public final class TransferOfLight {

    private TransferOfLight() { }

    public static List<String> findTransfers(ChartFrame f, List<Aspects.Hit> hits) {
        List<String> results = new ArrayList<>();
        List<Aspects.Hit> applying = new ArrayList<>();
        List<Aspects.Hit> separating = new ArrayList<>();
        
        for (Aspects.Hit h : hits) {
            // Ignore angles/points that don't have speeds for light transfer
            if (f.body(h.a) == null || f.body(h.b) == null) continue;
            
            if (h.applying) {
                applying.add(h);
            } else {
                separating.add(h);
            }
        }

        // Translation of Light:
        // A faster planet separating from one slower planet and applying to another slower planet.
        for (Aspects.Hit sep : separating) {
            ChartFrame.Body sepA = f.body(sep.a);
            ChartFrame.Body sepB = f.body(sep.b);
            
            ChartFrame.Body fasterSep = Math.abs(sepA.lonSpeed) > Math.abs(sepB.lonSpeed) ? sepA : sepB;
            ChartFrame.Body slowerSep = fasterSep == sepA ? sepB : sepA;

            for (Aspects.Hit app : applying) {
                ChartFrame.Body appA = f.body(app.a);
                ChartFrame.Body appB = f.body(app.b);

                ChartFrame.Body fasterApp = Math.abs(appA.lonSpeed) > Math.abs(appB.lonSpeed) ? appA : appB;
                ChartFrame.Body slowerApp = fasterApp == appA ? appB : appA;

                if (fasterSep.name.equals(fasterApp.name) && !slowerSep.name.equals(slowerApp.name)) {
                    // Strictly, translation implies the two slow planets aren't aspecting each other, 
                    // but we'll surface the geometry regardless.
                    results.add(String.format("%s translates light from %s to %s", 
                        fasterSep.name, slowerSep.name, slowerApp.name));
                }
            }
        }
        
        // Collection of Light:
        // Two faster planets apply to the same slower planet.
        for (int i = 0; i < applying.size(); i++) {
            for (int j = i + 1; j < applying.size(); j++) {
                Aspects.Hit h1 = applying.get(i);
                Aspects.Hit h2 = applying.get(j);
                
                ChartFrame.Body h1A = f.body(h1.a);
                ChartFrame.Body h1B = f.body(h1.b);
                ChartFrame.Body h2A = f.body(h2.a);
                ChartFrame.Body h2B = f.body(h2.b);

                ChartFrame.Body faster1 = Math.abs(h1A.lonSpeed) > Math.abs(h1B.lonSpeed) ? h1A : h1B;
                ChartFrame.Body slower1 = faster1 == h1A ? h1B : h1A;
                
                ChartFrame.Body faster2 = Math.abs(h2A.lonSpeed) > Math.abs(h2B.lonSpeed) ? h2A : h2B;
                ChartFrame.Body slower2 = faster2 == h2A ? h2B : h2A;
                
                if (slower1.name.equals(slower2.name) && !faster1.name.equals(faster2.name)) {
                    results.add(String.format("%s collects light from %s and %s", 
                        slower1.name, faster1.name, faster2.name));
                }
            }
        }
        
        // Deduplicate
        List<String> unique = new ArrayList<>();
        for (String r : results) {
            if (!unique.contains(r)) unique.add(r);
        }

        return unique;
    }
}
