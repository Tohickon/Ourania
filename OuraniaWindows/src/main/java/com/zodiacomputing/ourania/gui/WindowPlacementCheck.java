package com.zodiacomputing.ourania.gui;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Master list E: "Resize / full-screen mode - window fixed at 1024x768".
 */
public final class WindowPlacementCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: where a window opens", WindowPlacementCheck::choosing);
        part("B: what a session remembers", WindowPlacementCheck::remembering);
        part("C: full screen and back", WindowPlacementCheck::fullScreen);
        part("D: a window built by a check moves nothing and writes nothing", WindowPlacementCheck::inert);

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

    private static void choosing() {
        Rectangle screen = new Rectangle(0, 0, 2560, 1440);
        Rectangle work = new Rectangle(0, 0, 2560, 1400);
        List<Rectangle> one = List.of(screen);

        Rectangle first = WindowPlacement.choose(null, one, work);
        eq("the first launch opens at 85% of the work area's width", 2176, first.width);
        eq("and height", 1190, first.height);
        ok("centred in it, " + first, first.x == (2560 - 2176) / 2 && first.y == (1400 - 1190) / 2);
        ok("which is no longer 1024 by 768", first.width != 1024 || first.height != 768);

        Rectangle saved = new Rectangle(300, 120, 1500, 1000);
        eq("a saved place on a screen is where it opens", saved, WindowPlacement.choose(saved, one, work));

        Rectangle unplugged = new Rectangle(2800, 100, 1500, 1000);
        eq("a place on a monitor that is no longer there opens as the first launch does",
            first, WindowPlacement.choose(unplugged, one, work));
        eq("the same place opens there while that monitor is connected", unplugged,
            WindowPlacement.choose(unplugged, List.of(screen, new Rectangle(2560, 0, 1920, 1080)), work));

        Rectangle sliver = new Rectangle(2500, 100, 1500, 1000);
        eq("a place with only a 60 px sliver on screen is not visible enough to use",
            first, WindowPlacement.choose(sliver, one, work));
        Rectangle tiny = new Rectangle(100, 100, 400, 300);
        eq("a place smaller than the minimum is not used", first, WindowPlacement.choose(tiny, one, work));

        Rectangle small = new Rectangle(0, 0, 800, 600);
        Rectangle onSmall = WindowPlacement.choose(null, List.of(small), small);
        ok("on a screen smaller than the minimum the window still fits it, " + onSmall,
            onSmall.width <= 800 && onSmall.height <= 600);

        eq("bounds round-trip through the settings string", saved,
            WindowPlacement.parse(WindowPlacement.format(saved)));
        ok("a string that does not read is no bounds at all",
            WindowPlacement.parse("12,x,3") == null && WindowPlacement.parse(null) == null
                && WindowPlacement.parse("1,2,3") == null);
    }

    private static void remembering() throws Exception {
        final Rectangle[] got = new Rectangle[1];
        final boolean[] max = new boolean[1];
        SwingUtilities.invokeAndWait(() -> {
            Rectangle work = WindowPlacement.workArea();
            Rectangle want = new Rectangle(work.x + 40, work.y + 30, Math.max(900, work.width / 2), Math.max(640, work.height / 2));
            Settings.update(p -> {
                p.setProperty(WindowPlacement.BOUNDS, WindowPlacement.format(want));
                p.setProperty(WindowPlacement.MAXIMIZED, "false");
            });
            JFrame f = new JFrame();
            WindowPlacement.restore(f);
            got[0] = f.getBounds();
            eq("restore puts the window at the saved place", want, got[0]);
            ok("and sets the minimum size", f.getMinimumSize().equals(WindowPlacement.MINIMUM));

            // Saved again as it closes, then a new session restores it.
            WindowPlacement.save(f);
            f.dispose();
            JFrame g = new JFrame();
            WindowPlacement.restore(g);
            eq("what a session saves the next one restores", want, g.getBounds());
            g.dispose();

            Settings.set(WindowPlacement.MAXIMIZED, "true");
            JFrame h = new JFrame();
            WindowPlacement.restore(h);
            max[0] = (h.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
            ok("a window that closed maximised opens maximised", max[0]);
            eq("with its ordinary size kept behind the maximise", want, h.getBounds());
            h.dispose();
        });
    }

    private static void fullScreen() throws Exception {
        final JFrame[] f = new JFrame[1];
        final Rectangle[] before = new Rectangle[1];
        SwingUtilities.invokeAndWait(() -> {
            f[0] = new JFrame("placement check");
            WindowPlacement.installKeys(f[0]);
            f[0].setBounds(120, 90, 960, 680);
            f[0].setVisible(true);
            before[0] = f[0].getBounds();
        });
        try {
            Thread.sleep(400);
            SwingUtilities.invokeAndWait(() -> {
                javax.swing.Action toggle = f[0].getRootPane().getActionMap().get(
                    f[0].getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
                ok("F11 is bound to full screen", toggle != null);
                if (toggle != null) {
                    toggle.actionPerformed(null);
                }
            });
            Thread.sleep(400);
            SwingUtilities.invokeAndWait(() -> {
                Rectangle screen = f[0].getGraphicsConfiguration().getBounds();
                ok("F11 takes the window full screen", WindowPlacement.isFullScreen(f[0]));
                ok("with no title bar", f[0].isUndecorated());
                eq("covering the whole screen", screen, f[0].getBounds());
                ok("and still showing", f[0].isShowing());
                javax.swing.Action leave = f[0].getRootPane().getActionMap().get(
                    f[0].getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));
                ok("Esc is bound", leave != null);
                if (leave != null) {
                    leave.actionPerformed(null);
                }
            });
            Thread.sleep(400);
            SwingUtilities.invokeAndWait(() -> {
                ok("Esc leaves full screen", !WindowPlacement.isFullScreen(f[0]));
                ok("the title bar is back", !f[0].isUndecorated());
                eq("at the size and place it had before", before[0], f[0].getBounds());
                javax.swing.Action leave = f[0].getRootPane().getActionMap().get("ourania.leaveFullScreen");
                leave.actionPerformed(null);
                ok("Esc when not full screen does nothing", !f[0].isUndecorated() && before[0].equals(f[0].getBounds()));
                // Saved while full screen, the ordinary bounds are what is kept.
                WindowPlacement.toggleFullScreen(f[0]);
            });
            Thread.sleep(400);
            SwingUtilities.invokeAndWait(() -> {
                WindowPlacement.save(f[0]);
                eq("closing while full screen saves the ordinary size, not the screen's", before[0],
                    WindowPlacement.parse(Settings.get(WindowPlacement.BOUNDS, null)));
                WindowPlacement.toggleFullScreen(f[0]);
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> f[0].dispose());
        }
    }

    private static void inert() throws Exception {
        Settings.update(p -> {
            p.remove(WindowPlacement.BOUNDS);
            p.remove(WindowPlacement.MAXIMIZED);
        });
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            ok("the application's window has F11", w[0].getRootPane()
                .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)) != null);
            eq("constructing it restores nothing: it keeps the plain default size", 1024, w[0].getWidth());
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
        }
        ok("and writes nothing", Settings.get(WindowPlacement.BOUNDS, null) == null);
        // The application does restore and remember, in main, before the window is shown.
        String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
            "src/main/java/com/zodiacomputing/ourania/gui/OuraniaWindow.java")), java.nio.charset.StandardCharsets.UTF_8);
        int main = src.indexOf("public static void main");
        int restore = src.indexOf("WindowPlacement.restore(window)", main);
        int remember = src.indexOf("WindowPlacement.remember(window)", main);
        int visible = src.indexOf("window.setVisible(true)", main);
        ok("main restores and remembers the window before showing it",
            main > 0 && restore > main && remember > main && visible > restore && visible > remember);
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": " + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void eq(String label, Object want, Object got) {
        ok(label + ": got " + got + ", expected " + want, want == null ? got == null : want.equals(got));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }
}
