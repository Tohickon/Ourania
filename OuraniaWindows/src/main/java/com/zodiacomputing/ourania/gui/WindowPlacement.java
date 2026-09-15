package com.zodiacomputing.ourania.gui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the window opens, and full screen.
 *
 * <p>Master list E: "Resize / full-screen mode - window fixed at 1024x768". The frame could be
 * dragged larger, but every launch put it back at 1024 by 768 in the middle of the screen - a
 * chart wheel a third the size of a modern monitor - and nothing could take it full screen.
 *
 * <ul>
 * <li><b>The first launch</b> opens at 85% of the screen's work area, centred.</li>
 * <li><b>Every launch after</b> opens where the last one closed: the same size and place, or
 * maximised if it was - unless that place is no longer on any screen (a monitor unplugged), in
 * which case it opens as the first launch does rather than somewhere nobody can see.</li>
 * <li><b>F11</b> takes the window full screen and back; <b>Esc</b> also leaves it.</li>
 * </ul>
 *
 * <p><b>Restored and remembered by the application, not by the window.</b> {@link OuraniaWindow}
 * is built by a dozen check suites to be inspected, and a constructor that moved the frame to a
 * saved place or wrote the settings file on close would drag every one of them into it. main()
 * starts the application; it is main that calls {@link #restore} and {@link #remember}. The key
 * binding is harmless in a check and goes on in the constructor.
 */
final class WindowPlacement {

    private WindowPlacement() { }

    static final String BOUNDS = "window.bounds";
    static final String MAXIMIZED = "window.maximized";
    /** Below this the drawers and the wheel stop fitting beside each other. */
    static final Dimension MINIMUM = new Dimension(900, 640);
    /** How much of a saved rectangle must still be on a screen for it to count as visible. */
    static final int VISIBLE_W = 160;
    static final int VISIBLE_H = 80;

    private static final String FULL_SCREEN = "ourania.fullScreen";
    private static final String NORMAL_BOUNDS = "ourania.normalBounds";

    static String format(Rectangle r) {
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    /** A saved rectangle, or null when there is none or it does not read. */
    static Rectangle parse(String s) {
        if (s == null) {
            return null;
        }
        String[] p = s.trim().split(",");
        if (p.length != 4) {
            return null;
        }
        try {
            return new Rectangle(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()),
                Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Where the window should open, from what was saved, the screens there are, and the work
     * area of the main one. Pure, so the check can put a window on an unplugged monitor.
     */
    static Rectangle choose(Rectangle saved, List<Rectangle> screens, Rectangle work) {
        if (saved != null && saved.width >= MINIMUM.width && saved.height >= MINIMUM.height) {
            for (Rectangle s : screens) {
                Rectangle seen = s.intersection(saved);
                if (!seen.isEmpty() && seen.width >= VISIBLE_W && seen.height >= VISIBLE_H) {
                    return new Rectangle(saved);
                }
            }
        }
        int w = Math.max(Math.min(MINIMUM.width, work.width), (int) Math.round(work.width * 0.85));
        int h = Math.max(Math.min(MINIMUM.height, work.height), (int) Math.round(work.height * 0.85));
        return new Rectangle(work.x + (work.width - w) / 2, work.y + (work.height - h) / 2, w, h);
    }

    static List<Rectangle> screens() {
        List<Rectangle> out = new ArrayList<>();
        if (GraphicsEnvironment.isHeadless()) {
            return out;
        }
        for (GraphicsDevice d : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            out.add(d.getDefaultConfiguration().getBounds());
        }
        return out;
    }

    static Rectangle workArea() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    /** Puts the window where the last session left it, before it is shown. */
    static void restore(JFrame frame) {
        frame.setMinimumSize(MINIMUM);
        Rectangle at = choose(parse(Settings.get(BOUNDS, null)), screens(), workArea());
        frame.setBounds(at);
        frame.getRootPane().putClientProperty(NORMAL_BOUNDS, new Rectangle(at));
        if (Boolean.parseBoolean(Settings.get(MAXIMIZED, "false"))) {
            frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
        }
    }

    /**
     * Keeps the window's ordinary size and place as it changes, and writes them when it closes.
     *
     * Only the ordinary bounds are kept: a maximised or full-screen window's bounds are the
     * screen's, and restoring those as a plain window would open it covering everything with no
     * way to un-maximise it to anything smaller.
     */
    static void remember(JFrame frame) {
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                note(frame);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                note(frame);
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                save(frame);
            }
        });
    }

    private static void note(JFrame frame) {
        if (isFullScreen(frame) || (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0 || !frame.isShowing()) {
            return;
        }
        frame.getRootPane().putClientProperty(NORMAL_BOUNDS, frame.getBounds());
    }

    /** Writes the ordinary bounds and whether the window was maximised. */
    static void save(JFrame frame) {
        Object normal = frame.getRootPane().getClientProperty(NORMAL_BOUNDS);
        final Rectangle bounds = normal instanceof Rectangle ? (Rectangle) normal : frame.getBounds();
        final boolean maximized = isFullScreen(frame)
            ? Boolean.TRUE.equals(frame.getRootPane().getClientProperty(FULL_SCREEN + ".wasMax"))
            : (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
        Settings.update(p -> {
            p.setProperty(BOUNDS, format(bounds));
            p.setProperty(MAXIMIZED, String.valueOf(maximized));
        });
    }

    static boolean isFullScreen(JFrame frame) {
        return Boolean.TRUE.equals(frame.getRootPane().getClientProperty(FULL_SCREEN));
    }

    /**
     * Full screen and back.
     *
     * <b>Borderless over the whole screen, not exclusive full-screen mode.</b> GraphicsDevice's
     * exclusive mode changes how Windows composites the window and drops it to the desktop the
     * moment another window takes focus, which is hostile to a reader checking something in a
     * browser. A frame has to be taken off screen to lose its title bar, so it is disposed and
     * shown again; Swing keeps every component, and the chart does not recompute.
     */
    static void toggleFullScreen(JFrame frame) {
        boolean going = !isFullScreen(frame);
        javax.swing.JRootPane root = frame.getRootPane();
        if (going) {
            note(frame);
            root.putClientProperty(FULL_SCREEN + ".wasMax", (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);
            Rectangle screen = frame.getGraphicsConfiguration().getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            frame.setExtendedState(Frame.NORMAL);
            frame.setBounds(screen);
            root.putClientProperty(FULL_SCREEN, Boolean.TRUE);
            frame.setVisible(true);
        } else {
            boolean wasMax = Boolean.TRUE.equals(root.getClientProperty(FULL_SCREEN + ".wasMax"));
            Object normal = root.getClientProperty(NORMAL_BOUNDS);
            frame.dispose();
            frame.setUndecorated(false);
            root.putClientProperty(FULL_SCREEN, Boolean.FALSE);
            if (normal instanceof Rectangle) {
                frame.setBounds((Rectangle) normal);
            }
            frame.setVisible(true);
            if (wasMax) {
                frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
            }
        }
    }

    /** F11 toggles full screen; Esc leaves it. On the window's root pane, so any focus hears them. */
    static void installKeys(JFrame frame) {
        javax.swing.InputMap in = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap act = frame.getRootPane().getActionMap();
        in.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "ourania.toggleFullScreen");
        act.put("ourania.toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFullScreen(frame);
            }
        });
        in.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ourania.leaveFullScreen");
        act.put("ourania.leaveFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isFullScreen(frame)) {
                    toggleFullScreen(frame);
                }
            }
        });
    }
}
