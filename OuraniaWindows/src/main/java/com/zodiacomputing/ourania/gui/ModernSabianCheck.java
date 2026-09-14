package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * The modern Sabian tiers: all 360 degrees load, and the reading says when they were written
 * from a different symbol than the one it prints.
 */
public final class ModernSabianCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final String[] SIGNS = {"Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"};

    public static void main(String[] args) {
        Settings.useScratchFile();
        InterpretationService svc = InterpretationService.getInstance();

        System.out.println("=== Part A: every degree, every tier ===");
        int before = failures.size();
        int complete = 0;
        int flagged = 0;
        List<String> gaps = new ArrayList<>();
        for (String sign : SIGNS) {
            for (int d = 1; d <= 30; d++) {
                String[] t = svc.getModernSabian(sign, d);
                boolean ok = t != null && nonEmpty(t[2]) && nonEmpty(t[3]) && nonEmpty(t[4])
                    && nonEmpty(t[1]) && ("true".equals(t[5]) || "false".equals(t[5]));
                if (ok) {
                    complete++;
                    flagged += "true".equals(t[5]) ? 1 : 0;
                } else if (gaps.size() < 5) {
                    gaps.add(sign + " " + d);
                }
            }
        }
        ok("all 360 degrees carry a modern image, archetype and meaning: " + complete
            + ", first gaps " + gaps, complete == 360);
        // The second set David supplied: 147 differ from the app's symbol (the first had 178).
        ok("147 degrees are flagged as written from a different symbol, got " + flagged,
            flagged == 147);
        ok("a degree that does not exist has none", svc.getModernSabian("Aries", 31) == null);
        ok("the sign's case does not matter",
            svc.getModernSabian("aries", 1) != null && svc.getModernSabian("ARIES", 1) != null);
        // The loader must not truncate at a quote inside the text.
        boolean quotesSurvive = true;
        for (String sign : SIGNS) {
            for (int d = 1; d <= 30; d++) {
                String[] t = svc.getModernSabian(sign, d);
                if (t != null) {
                    for (int i = 0; i < 5; i++) {
                        quotesSurvive &= t[i] == null || !t[i].endsWith("\\");
                    }
                }
            }
        }
        ok("no tier ends in a stray escape", quotesSurvive);
        report("Part A", before);

        System.out.println("=== Part B: the reading shows them, and the flag only where due ===");
        before = failures.size();
        String aries1 = InterpretationPanel.modernSabianHtml("Aries", 1);
        ok("Aries 1 shows its modern image", aries1.contains("swimmer stepping onto a sunlit beach"));
        ok("and its archetype and meaning", aries1.contains("Core archetype")
            && aries1.contains("What it means now"));
        ok("and is not flagged, since its symbol matches", !aries1.contains("differently worded"));
        String aqu30 = InterpretationPanel.modernSabianHtml("Aquarius", 30);
        String[] t30 = svc.getModernSabian("Aquarius", 30);
        ok("Aquarius 30 is flagged", aqu30.contains("differently worded"));
        ok("and shows the symbol its modern tiers were written from",
            t30 != null && aqu30.contains(t30[1]));
        ok("a degree with nothing shows nothing",
            InterpretationPanel.modernSabianHtml("Aries", 31).isEmpty());
        ok("text is escaped for the HTML pane",
            !InterpretationPanel.modernSabianHtml("Aries", 2).contains("<script"));
        report("Part B", before);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    private static boolean nonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static void report(String part, int before) {
        int added = failures.size() - before;
        System.out.println(part + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
