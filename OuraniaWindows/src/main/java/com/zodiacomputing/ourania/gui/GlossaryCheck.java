package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * David's glossary (2 Oct 2026): that the file loads whole, that every entry came through the
 * import readable, that search finds the term before the passing mention, and that the index
 * page lists it.
 *
 * <p>Part B is the import's own check. The text is Markdown with LaTeX in places, and an entry
 * carrying a stray {@code **} or {@code \(} is one the importer misread; this is what would
 * have caught the two entries whose numbered sub-points it first dropped.
 */
public final class GlossaryCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures.add(what);
        }
    }

    public static void main(String[] args) {
        try {
            loads();
            readable();
            search();
            index();
        } catch (Throwable t) {
            failures.add("the suite threw before it could report: " + t);
            t.printStackTrace(System.out);
        }
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

    private static void loads() {
        System.out.println("=== Part A: the glossary loads whole ===");
        check(Glossary.failure() == null, "it loaded: " + Glossary.failure());
        check(Glossary.all().size() == 827, "827 terms, found " + Glossary.all().size());
        check(Glossary.chapters().size() == 19, "19 chapters, found " + Glossary.chapters().size());
        check(Glossary.chapters().get(0).equals("A to Z Essentials"),
            "the file's own order: " + Glossary.chapters().get(0));
        int total = 0;
        for (String c : Glossary.chapters()) {
            total += Glossary.chapter(c).size();
        }
        check(total == Glossary.all().size(), "every term is in one chapter");
    }

    private static void readable() {
        System.out.println("=== Part B: every entry came through the import readable ===");
        int bad = 0;
        for (Glossary.Term t : Glossary.all()) {
            boolean ok = !t.term.isEmpty() && !t.text.trim().isEmpty()
                && !t.term.contains("**") && !t.text.contains("**")
                && !t.text.contains("\\(") && !t.text.contains("\\\\")
                && t.text.replaceAll("</?(b|i)>|<br>|&(amp|lt|gt|bull);", "").indexOf('<') < 0;
            if (!ok && bad++ < 5) {
                check(false, "entry \"" + t.term + "\" is not clean: " + t.text);
            }
        }
        check(bad == 0, bad + " entries not clean");
        Glossary.Term spheroth = Glossary.find("The 10 Sephiroth on the Tree of Life");
        check(spheroth != null && spheroth.text.contains("1. <i>Kether"),
            "numbered sub-points are kept: " + (spheroth == null ? null : spheroth.text));
        Glossary.Term mid = Glossary.find("Midpoints");
        check(mid != null && mid.text.contains("(Planet A + Planet B)/2"),
            "LaTeX read as text: " + (mid == null ? null : mid.text));
        Glossary.Term mansion = Glossary.find("Al-Sharatain");
        check(mansion != null && mansion.text.contains("<b>Tropical Span:</b> 0°00' Aries"),
            "a mansion's fields kept as fields: " + (mansion == null ? null : mansion.text));
    }

    private static void search() {
        System.out.println("=== Part C: search finds the term first ===");
        List<Glossary.Term> c = Glossary.search("cazimi", 10);
        check(!c.isEmpty() && c.get(0).term.equals("Cazimi"), "cazimi -> " + c);
        List<Glossary.Term> asc = Glossary.search("Ascendant", 30);
        check(!asc.isEmpty() && asc.get(0).term.startsWith("Ascendant"), "Ascendant -> " + asc);
        check(Glossary.search("sharatain", 5).get(0).term.startsWith("1. Al-Sharatain"),
            "a numbered mansion is found by its name");
        check(Glossary.search("ÇAZIMI", 5).size() == Glossary.search("cazimi", 5).size(),
            "accents and case ignored");
        check(Glossary.search("solar glare", 50).stream().anyMatch(t -> t.term.equals("Combust")),
            "and a definition's words find it too");
        check(Glossary.search("", 10).isEmpty() && Glossary.search("zzqxj", 10).isEmpty(),
            "nothing for nothing");
        check(Glossary.search("e", 7).size() == 7, "the limit holds");
        Glossary.Term a = Glossary.find("ascendant");
        check(a != null && a.term.equals("Ascendant (Rising Sign)"),
            "find ignores case and the bracketed alias: " + a);
    }

    private static void index() throws Exception {
        System.out.println("=== Part D: the index page lists it ===");
        final InterpretationPanel[] hold = new InterpretationPanel[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> hold[0] = new InterpretationPanel(null, null));
        InterpretationPanel panel = hold[0];
        String top = panel.indexHtml("");
        check(top.contains("index|glossary"), "the index links the glossary");
        String list = panel.indexHtml("glossary");
        check(list.contains("index|glossary|18"), "all 19 chapters are listed");
        String one = panel.indexHtml("glossary|0");
        check(one.contains("<b>Cazimi</b>"), "a chapter shows its terms");
        check(one.contains("index|glossary'"), "and a way back to the chapters");
    }
}
