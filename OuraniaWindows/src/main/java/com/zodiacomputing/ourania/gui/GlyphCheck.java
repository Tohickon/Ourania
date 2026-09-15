package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Master list E11: "Glyph font coverage - several UI glyphs (swap arrow, drawer chevrons) render
 * as empty boxes in the bundled font."
 *
 * <p>The cause was not a glyph or two but every font a component was set in: new Font with a
 * physical face has no fallback, so all of the app's symbols came out as boxes wherever a label,
 * button or handle used Theme's type. See {@link Theme#font(String, int, int)}.
 *
 * <p>Part A does not trust canDisplay alone. It draws the glyph and compares the ink with a
 * codepoint no font has, which is what "an empty box" means on screen.
 */
public final class GlyphCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** Symbols the interface itself draws, outside HTML: triangles, the swap arrow, the releasing marks. */
    static final int[] UI_SYMBOLS = {0x25B8, 0x25BE, 0x25C2, 0x25B4, 0x21C4, 0x2B50, 0x1F517, 0x2630};

    /** Unassigned in every Unicode version, so its rendering is the missing glyph itself. */
    private static final int NO_SUCH_GLYPH = 0x10FFFD;

    private static final String[] LOGICAL = {"Dialog", "DialogInput", "Serif", "SansSerif", "Monospaced"};

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: Theme's fonts draw the symbols", GlyphCheck::themeFonts);
        part("B: no text in the window is a box", GlyphCheck::window);
        part("C: no component font is built around Theme", GlyphCheck::source);

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

    private static void themeFonts() {
        Font[] fonts = {Theme.TITLE, Theme.HEADING, Theme.BODY, Theme.SMALL,
            Theme.font("Arial", Font.BOLD, 16), Theme.font("Segoe UI Symbol", Font.PLAIN, 14)};
        String[] names = {"TITLE", "HEADING", "BODY", "SMALL", "Arial bold 16", "Segoe UI Symbol"};
        List<Integer> symbols = new ArrayList<>();
        for (int cp : UI_SYMBOLS) {
            symbols.add(cp);
        }
        for (int cp = 0x2648; cp <= 0x2653; cp++) {
            symbols.add(cp);
        }
        for (int i = 0; i < Bodies.count(); i++) {
            String g = Bodies.at(i).glyph;
            if (g != null && !g.isEmpty() && g.codePointAt(0) > 0x2000) {
                symbols.add(g.codePointAt(0));
            }
        }
        for (int f = 0; f < fonts.length; f++) {
            List<String> missing = new ArrayList<>();
            List<String> boxes = new ArrayList<>();
            for (int cp : symbols) {
                if (!fonts[f].canDisplay(cp)) {
                    missing.add(Integer.toHexString(cp));
                }
                if (sameInk(fonts[f], cp, NO_SUCH_GLYPH)) {
                    boxes.add(Integer.toHexString(cp));
                }
            }
            ok(names[f] + " can display all " + symbols.size() + " symbols, missing " + missing,
                missing.isEmpty());
            ok(names[f] + " draws none of them as the missing glyph, boxes " + boxes, boxes.isEmpty());
            ok(names[f] + " is not a UIResource, so a look-and-feel refresh leaves it",
                !(fonts[f] instanceof javax.swing.plaf.UIResource));
        }
        ok("TITLE is bold 17, as declared", Theme.TITLE.isBold() && Theme.TITLE.getSize() == 17);
        ok("SMALL is plain 11, as declared", !Theme.SMALL.isBold() && Theme.SMALL.getSize() == 11);

        // The fallback must not cost the face: Latin text measures exactly as the physical font.
        Font physical = new Font("Segoe UI", Font.BOLD, 12);
        if ("Segoe UI".equals(physical.getFamily())) {
            BufferedImage im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = im.createGraphics();
            int want = g.getFontMetrics(physical).stringWidth("Aries 12° Swap A / B");
            int got = g.getFontMetrics(Theme.HEADING).stringWidth("Aries 12° Swap A / B");
            g.dispose();
            ok("HEADING sets Latin text in Segoe UI's own widths, " + got + " vs " + want, got == want);
            // And the reason for all this still holds, so Part A is measuring something.
            ok("a physical Segoe UI still draws the Aries glyph as a box",
                !physical.canDisplay(0x2648) && sameInk(physical, 0x2648, NO_SUCH_GLYPH));
        }
    }

    private static int inspected;

    private static void window() throws Exception {
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            List<String> boxes = new ArrayList<>();
            boolean[] swap = {false};
            int[] chevrons = {0};
            SwingUtilities.invokeAndWait(() -> {
                inspected = 0;
                walk(w[0], boxes, swap, chevrons);
            });
            ok("the walk reached the window's text, " + inspected + " strings", inspected > 200);
            ok("every string is drawable in its component's font, boxes " + boxes, boxes.isEmpty());
            ok("the swap button has its arrow back", swap[0]);
            ok("the accordion headers have their triangles back, " + chevrons[0], chevrons[0] > 0);
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
    }

    private static void walk(Component c, List<String> boxes, boolean[] swap, int[] chevrons) {
        if (c instanceof JLabel) {
            String t = ((JLabel) c).getText();
            text(t, c.getFont(), c, boxes);
            if (Accordion.Section.CLOSED.equals(t) || Accordion.Section.OPEN.equals(t)) {
                chevrons[0]++;
            }
        } else if (c instanceof AbstractButton) {
            String t = ((AbstractButton) c).getText();
            text(t, c.getFont(), c, boxes);
            if (t != null && t.contains("⇄") && t.contains("Swap")) {
                swap[0] = true;
            }
        } else if (c instanceof JTextComponent && !(c instanceof javax.swing.JEditorPane)) {
            text(((JTextComponent) c).getText(), c.getFont(), c, boxes);
        } else if (c instanceof JComboBox) {
            JComboBox<?> box = (JComboBox<?>) c;
            for (int i = 0; i < box.getItemCount(); i++) {
                Object item = box.getItemAt(i);
                text(item == null ? null : item.toString(), c.getFont(), c, boxes);
            }
        }
        if (c instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) c;
            for (int i = 0; i < tabs.getTabCount(); i++) {
                text(tabs.getTitleAt(i), c.getFont(), c, boxes);
            }
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                walk(child, boxes, swap, chevrons);
            }
        }
    }

    /** HTML text is skipped: the HTML renderer finds its own fallback, per span. */
    private static void text(String t, Font font, Component c, List<String> boxes) {
        if (t == null || t.isEmpty() || t.regionMatches(true, 0, "<html", 0, 5) || font == null) {
            return;
        }
        inspected++;
        int bad = font.canDisplayUpTo(t);
        if (bad >= 0) {
            boxes.add(c.getClass().getSimpleName() + " \"" + t + "\" U+"
                + Integer.toHexString(t.codePointAt(bad)) + " in " + font.getFontName());
        }
    }

    private static void source() throws Exception {
        Path gui = Paths.get("src/main/java/com/zodiacomputing/ourania/gui");
        if (!Files.isDirectory(gui)) {
            gui = Paths.get("com/zodiacomputing/ourania/gui");
        }
        ok("the gui sources are found from " + Paths.get("").toAbsolutePath(), Files.isDirectory(gui));
        if (!Files.isDirectory(gui)) {
            return;
        }
        Pattern named = Pattern.compile("new\\s+Font\\(\\s*\"([^\"]+)\"");
        List<String> physical = new ArrayList<>();
        int logical = 0;
        int files = 0;
        try (java.util.stream.Stream<Path> s = Files.list(gui)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".java") || name.endsWith("Check.java") || name.equals("Theme.java")) {
                    continue;
                }
                files++;
                String[] lines = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.startsWith("*") || line.startsWith("//")) {
                        continue;
                    }
                    Matcher m = named.matcher(line);
                    while (m.find()) {
                        if (isLogical(m.group(1))) {
                            logical++;
                        } else {
                            physical.add(name + ":" + (i + 1) + " " + m.group(1));
                        }
                    }
                }
            }
        }
        ok("the scan read the gui package, " + files + " files", files > 30);
        ok("it saw the logical fonts it should allow, " + logical, logical > 5);
        ok("no physical face is constructed outside Theme.font, found " + physical, physical.isEmpty());
    }

    private static boolean isLogical(String family) {
        for (String l : LOGICAL) {
            if (l.equals(family)) {
                return true;
            }
        }
        return false;
    }

    /** Whether two codepoints leave identical pixels in this font - a box looks like a box. */
    private static boolean sameInk(Font font, int a, int b) {
        return java.util.Arrays.equals(ink(font, a), ink(font, b));
    }

    private static int[] ink(Font font, int cp) {
        BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(Color.WHITE);
        g.setFont(font.deriveFont(32f));
        g.drawString(new String(Character.toChars(cp)), 8, 48);
        g.dispose();
        return im.getRGB(0, 0, 64, 64, null, 0, 64);
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
