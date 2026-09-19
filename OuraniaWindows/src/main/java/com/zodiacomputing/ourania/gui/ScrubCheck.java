package com.zodiacomputing.ourania.gui;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Master list E8: scrubbing through time, by Shift+drag on the wheel and by the Scrub slider.
 *
 * <p>Neither half calls the scrub methods to test them where a real input path exists: the
 * wheel is sent mouse events and the slider is moved as a slider is.
 */
public final class ScrubCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final ZonedDateTime ANCHOR = ZonedDateTime.of(2026, 1, 31, 12, 0, 0, 0, ZoneId.of("UTC"));

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        final OuraniaWindow[] w = new OuraniaWindow[1];
        SwingUtilities.invokeAndWait(() -> w[0] = new OuraniaWindow());
        try {
            SkymapPanel sky = (SkymapPanel) field(w[0], "skymapPanel");
            Component chart = (Component) field(sky, "chartPanel");
            SwingUtilities.invokeAndWait(() -> chart.setSize(900, 820));

            part("A: the offset, in words", ScrubCheck::labels);
            part("B: Shift+drag on the wheel moves time, and back again", () -> drag(w[0], sky, chart));
            part("C: an offset, not a run of steps", () -> months(sky));
            part("D: a synastry scrub moves the sky and neither birth", () -> synastry(sky));
            part("E: the slider springs back and the chart stays", () -> slider(sky));
            part("F: the wheel is redrawn at the scrubbed moment", () -> redraw(sky, chart));
            part("G: Play moves the chart the app opens onto", () -> play(sky));
            part("H: a bar per chart, each moving only its own", () -> bars(sky));
            part("I: a bar held pulled keeps time going until it is let go", () -> shuttle(sky));
        } finally {
            SwingUtilities.invokeAndWait(() -> w[0].dispose());
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

    private static void labels() {
        eq("three days forward", "+3 days", SkymapPanel.scrubLabel(3, "1 Day"));
        eq("one month back", "−1 month", SkymapPanel.scrubLabel(-1, "1 Month"));
        eq("no offset", "±0 hours", SkymapPanel.scrubLabel(0, "1 Hour"));
        eq("a year", "+1 year", SkymapPanel.scrubLabel(1, "1 Year"));
    }

    /** Puts the panel in a single natal chart at a known moment with a known step. */
    private static void reset(SkymapPanel sky, String step) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (sky.isScrubbing()) {
                    sky.endScrub();
                }
                set(sky, "chartMode", ChartMode.SINGLE);
                set(sky, "natalRing.time", ANCHOR);
                set(sky, "skyRing.time", ANCHOR);
                set(sky, "stepAmount", step);
                set(sky, "isPlaying", false);
                sky.fitView();
                sky.updateChartData();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static ZonedDateTime time(SkymapPanel sky, String name) throws Exception {
        return (ZonedDateTime) field(sky, name);
    }

    private static void drag(OuraniaWindow w, SkymapPanel sky, Component chart) throws Exception {
        javax.swing.JEditorPane selection = (javax.swing.JEditorPane) field(w, "selectionPane");
        reset(sky, "1 Day");
        SwingUtilities.invokeAndWait(() -> {
            selection.setText("<html><body>scrub-nothing-opened</body></html>");
            press(chart, MouseEvent.MOUSE_PRESSED, 400, 400, true);
            press(chart, MouseEvent.MOUSE_DRAGGED, 406, 401, true);
        });
        ok("under the threshold a Shift+drag has not started a scrub", !sky.isScrubbing());
        // Play switched on in the same turn of the event queue as the drag that starts the scrub:
        // set any earlier, the animation timer ticks the sky on before the scrub can pause it,
        // and under load that moved the anchor by a day or two.
        SwingUtilities.invokeAndWait(() -> {
            try {
                set(sky, "isPlaying", true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            press(chart, MouseEvent.MOUSE_DRAGGED, 435, 402, true);
        });
        ok("a Shift+drag scrubs", sky.isScrubbing());
        ok("and pauses the animation", !(Boolean) field(sky, "isPlaying"));
        eq("35 px right is three steps", 3, sky.scrubSteps);
        ZonedDateTime base = time(sky, "natalRing.time");
        ZonedDateTime skyT = time(sky, "skyRing.time");
        boolean moved = base.equals(ANCHOR.plusDays(3)) || skyT.equals(ANCHOR.plusDays(3));
        ok("the transport's chart moved exactly three days: base " + base + ", sky " + skyT, moved);
        ok("nothing moved by any other amount",
            (base.equals(ANCHOR) || base.equals(ANCHOR.plusDays(3)))
                && (skyT.equals(ANCHOR) || skyT.equals(ANCHOR.plusDays(3))));

        SwingUtilities.invokeAndWait(() -> press(chart, MouseEvent.MOUSE_DRAGGED, 340, 402, true));
        eq("60 px left of the press is six steps back", -6, sky.scrubSteps);
        SwingUtilities.invokeAndWait(() -> press(chart, MouseEvent.MOUSE_DRAGGED, 404, 402, true));
        ok("back at the press, both times are exactly where the scrub began",
            time(sky, "natalRing.time").equals(ANCHOR) && time(sky, "skyRing.time").equals(ANCHOR));

        SwingUtilities.invokeAndWait(() -> {
            press(chart, MouseEvent.MOUSE_DRAGGED, 452, 402, true);
            press(chart, MouseEvent.MOUSE_RELEASED, 452, 402, true);
            press(chart, MouseEvent.MOUSE_CLICKED, 452, 402, true);
        });
        ok("letting go ends the scrub", !sky.isScrubbing());
        ok("and the chart stays where it was let go, +5 days",
            time(sky, "natalRing.time").equals(ANCHOR.plusDays(5))
                || time(sky, "skyRing.time").equals(ANCHOR.plusDays(5)));
        ok("the click that ends a scrub opens nothing", selection.getText().contains("scrub-nothing-opened"));

        // Without Shift, at fit, a drag is still nothing; zoomed, it pans and does not scrub.
        reset(sky, "1 Day");
        SwingUtilities.invokeAndWait(() -> {
            press(chart, MouseEvent.MOUSE_PRESSED, 400, 400, false);
            press(chart, MouseEvent.MOUSE_DRAGGED, 480, 400, false);
            press(chart, MouseEvent.MOUSE_RELEASED, 480, 400, false);
        });
        ok("a plain drag at fit moves no time",
            time(sky, "natalRing.time").equals(ANCHOR) && time(sky, "skyRing.time").equals(ANCHOR));
        sky.zoomAt(450, 410, -5);
        SwingUtilities.invokeAndWait(() -> {
            press(chart, MouseEvent.MOUSE_PRESSED, 400, 400, false);
            press(chart, MouseEvent.MOUSE_DRAGGED, 480, 400, false);
            press(chart, MouseEvent.MOUSE_RELEASED, 480, 400, false);
        });
        ok("a plain drag while zoomed pans and moves no time",
            !sky.viewIsFit() && time(sky, "natalRing.time").equals(ANCHOR) && time(sky, "skyRing.time").equals(ANCHOR));

        // The globe scrubs the same way.
        reset(sky, "1 Hour");
        SwingUtilities.invokeAndWait(() -> sky.setGlobeMode(true));
        SwingUtilities.invokeAndWait(() -> {
            press(chart, MouseEvent.MOUSE_PRESSED, 400, 400, true);
            press(chart, MouseEvent.MOUSE_DRAGGED, 425, 400, true);
            press(chart, MouseEvent.MOUSE_RELEASED, 425, 400, true);
        });
        ok("on the globe, Shift+drag scrubs too: two hours",
            time(sky, "natalRing.time").equals(ANCHOR.plusHours(2)) || time(sky, "skyRing.time").equals(ANCHOR.plusHours(2)));
        SwingUtilities.invokeAndWait(() -> sky.setGlobeMode(false));

        // Real Time has no step of its own; a scrub moves by the hour.
        reset(sky, "Real Time");
        eq("with Step on Real Time a scrub moves by the hour", "1 Hour", sky.scrubUnit());
        SwingUtilities.invokeAndWait(() -> {
            sky.beginScrub();
            sky.scrubTo(4);
            sky.endScrub();
        });
        ok("four steps on Real Time is four hours, not the clock",
            time(sky, "natalRing.time").equals(ANCHOR.plusHours(4)) || time(sky, "skyRing.time").equals(ANCHOR.plusHours(4)));
        try {
            eq("and the Step setting is left as it was", "Real Time", field(sky, "stepAmount"));
        } catch (Exception e) {
            ok("stepAmount readable", false);
        }
    }

    /** From 31 January a month at a time would creep to the 28th; an offset does not. */
    private static void months(SkymapPanel sky) throws Exception {
        reset(sky, "1 Month");
        List<ZonedDateTime> seen = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                sky.beginScrub();
                for (int k : new int[] {1, 2, 3}) {
                    sky.scrubTo(k);
                    ZonedDateTime b = (ZonedDateTime) field(sky, "natalRing.time");
                    ZonedDateTime s = (ZonedDateTime) field(sky, "skyRing.time");
                    seen.add(b.equals(ANCHOR) ? s : b);
                }
                sky.endScrub();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        eq("one month from 31 January", ANCHOR.plusMonths(1), seen.get(0));
        eq("two months is 31 March, not the 28th a run of steps reaches", ANCHOR.plusMonths(2), seen.get(1));
        eq("three months is 30 April", ANCHOR.plusMonths(3), seen.get(2));
    }

    private static void synastry(SkymapPanel sky) throws Exception {
        reset(sky, "1 Week");
        final ZonedDateTime personB = ZonedDateTime.of(1985, 3, 2, 14, 30, 0, 0, ZoneId.of("UTC"));
        SwingUtilities.invokeAndWait(() -> {
            try {
                set(sky, "chartMode", ChartMode.SYNASTRY);
                // Both people have a birth chart here; with no Chart A the inner time is the sky.
                set(sky, "innerIsBirthChart", true);
                set(sky, "showTransitChart", true);
                set(sky, "outerRing.time", personB);
                set(sky, "animateTarget", "Both");
                sky.beginScrub();
                sky.scrubTo(2);
                sky.endScrub();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ok("on a synastry this is a synastry", sky.isSynastryChart());
        eq("the sky moved two weeks", ANCHOR.plusWeeks(2), time(sky, "skyRing.time"));
        eq("person A's birth did not move", ANCHOR, time(sky, "natalRing.time"));
        eq("person B's birth did not move", personB, time(sky, "outerRing.time"));
        SwingUtilities.invokeAndWait(() -> {
            try {
                set(sky, "animateTarget", "Transit");
                set(sky, "chartMode", ChartMode.SINGLE);
                set(sky, "innerIsBirthChart", false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void slider(SkymapPanel sky) throws Exception {
        reset(sky, "1 Day");
        javax.swing.JSlider slider = sky.scrubSlider;
        ok("the transport strip has a Scrub slider", slider != null);
        if (slider == null) {
            return;
        }
        ok("it reaches sixty steps each way from the middle",
            slider.getMinimum() == -SkymapPanel.SCRUB_SLIDER_REACH && slider.getMaximum() == SkymapPanel.SCRUB_SLIDER_REACH
                && slider.getValue() == 0);
        SwingUtilities.invokeAndWait(() -> {
            slider.setValueIsAdjusting(true);
            slider.setValue(4);
            slider.setValue(7);
        });
        ok("dragging the knob scrubs", sky.isScrubbing());
        eq("to where the knob is", 7, sky.scrubSteps);
        SwingUtilities.invokeAndWait(() -> slider.setValueIsAdjusting(false));
        ok("letting go ends the scrub", !sky.isScrubbing());
        eq("the knob springs back to the middle", 0, slider.getValue());
        ZonedDateTime after = time(sky, "natalRing.time").equals(ANCHOR) ? time(sky, "skyRing.time") : time(sky, "natalRing.time");
        eq("and the chart stays seven days on", ANCHOR.plusDays(7), after);
        // An arrow key moves a focused slider one notch without adjusting.
        SwingUtilities.invokeAndWait(() -> slider.setValue(-1));
        ZonedDateTime nudged = time(sky, "natalRing.time").equals(ANCHOR) ? time(sky, "skyRing.time") : time(sky, "natalRing.time");
        eq("a single notch is a single step, from where the chart now is", ANCHOR.plusDays(6), nudged);
        eq("and the knob is back in the middle", 0, slider.getValue());
        ok("with no scrub left running", !sky.isScrubbing());
    }

    private static void redraw(SkymapPanel sky, Component chart) throws Exception {
        reset(sky, "1 Day");
        double[] b0 = ((double[]) field(sky, "natalRing.lon")).clone();
        SwingUtilities.invokeAndWait(() -> {
            press(chart, MouseEvent.MOUSE_PRESSED, 300, 400, true);
            for (int x = 305; x <= 350; x += 1) {
                press(chart, MouseEvent.MOUSE_DRAGGED, x, 400, true);
            }
        });
        // The one coalesced recompute runs on the next turn of the queue.
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        double[] b1 = ((double[]) field(sky, "natalRing.lon")).clone();
        ok("mid-scrub the wheel has already moved", !Arrays.equals(b0, b1));
        final double[][] fresh = new double[1][];
        SwingUtilities.invokeAndWait(() -> {
            try {
                sky.updateChartData();
                fresh[0] = ((double[]) field(sky, "natalRing.lon")).clone();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ok("to exactly the positions of the scrubbed moment", Arrays.equals(b1, fresh[0]));
        int moon = com.zodiacomputing.ourania.astro.Bodies.indexOf("moon");
        double moved = com.zodiacomputing.ourania.astro.Aspects.separation(b0[moon], b1[moon]);
        ok("five days on, the Moon has moved about 65 degrees, " + Math.round(moved), moved > 50 && moved < 80);
        SwingUtilities.invokeAndWait(() -> press(chart, MouseEvent.MOUSE_RELEASED, 350, 400, true));
        ok("released", !sky.isScrubbing());
    }

    /**
     * <b>The transport stood still on the cold-open chart.</b> With no Chart A the inner wheel
     * is the sky and updateChartData copies the sky's time over Chart A's; stepTime moved Chart
     * A's time, so every step was undone by the recompute that followed it. Found while writing
     * the scrub, which steps through the same method.
     */
    private static void play(SkymapPanel sky) throws Exception {
        reset(sky, "1 Day");
        // The cold open exactly: no Chart A, no transits, Play aimed at "Transit" as it starts.
        set(sky, "showTransitChart", false);
        set(sky, "animateTarget", "Transit");
        ok("this is the cold-open chart: no Chart A, the sky on the inner wheel, no transits",
            !(Boolean) field(sky, "innerIsBirthChart") && !(Boolean) field(sky, "showTransitChart"));
        final java.lang.reflect.Method step = SkymapPanel.class.getDeclaredMethod("stepTime");
        step.setAccessible(true);
        double[] before = ((double[]) field(sky, "natalRing.lon")).clone();
        SwingUtilities.invokeAndWait(() -> {
            try {
                set(sky, "animationDirection", 1);
                // What the animation timer does on each tick while playing.
                step.invoke(sky);
                sky.updateChartData();
                step.invoke(sky);
                sky.updateChartData();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        eq("two ticks of Play at one day a step move the sky two days", ANCHOR.plusDays(2), time(sky, "skyRing.time"));
        int moon = com.zodiacomputing.ourania.astro.Bodies.indexOf("moon");
        double moved = com.zodiacomputing.ourania.astro.Aspects.separation(before[moon],
            ((double[]) field(sky, "natalRing.lon"))[moon]);
        ok("and the wheel's Moon went with it, " + Math.round(moved) + " degrees", moved > 20 && moved < 32);
    }

    /**
     * David: "could we have different scrub bars that control different charts in the wheel and
     * globe". One bar per chart, shown while that chart is on the wheel, each moving only its own
     * moment; a birth time moved by its bar can be put back.
     */
    private static void bars(SkymapPanel sky) throws Exception {
        final ZonedDateTime personB = ZonedDateTime.of(1985, 3, 2, 14, 30, 0, 0, ZoneId.of("UTC"));
        ok("there is a bar for Chart A, Chart B and the sky",
            sky.scrubSliders.containsKey(SkymapPanel.ScrubTarget.CHART_A)
                && sky.scrubSliders.containsKey(SkymapPanel.ScrubTarget.CHART_B)
                && sky.scrubSliders.containsKey(SkymapPanel.ScrubTarget.SKY));

        mode(sky, ChartMode.SINGLE, false, false, personB, "1 Hour");
        ok("the cold open shows the sky's bar alone", shown(sky) .equals("SKY"));

        mode(sky, ChartMode.SINGLE, true, false, personB, "1 Hour");
        ok("a natal chart shows Chart A's bar and the sky's, " + shown(sky), shown(sky).equals("CHART_A SKY"));
        slide(sky, SkymapPanel.ScrubTarget.CHART_A, 3);
        eq("Chart A's bar moves Chart A's birth three hours", ANCHOR.plusHours(3), time(sky, "natalRing.time"));
        eq("and leaves the sky where it was", ANCHOR, time(sky, "skyRing.time"));
        ok("and offers the birth time back", sky.scrubResets.get(SkymapPanel.ScrubTarget.CHART_A).isEnabled());
        slide(sky, SkymapPanel.ScrubTarget.CHART_A, 2);
        eq("a second slide goes on from there", ANCHOR.plusHours(5), time(sky, "natalRing.time"));
        SwingUtilities.invokeAndWait(() -> sky.scrubResets.get(SkymapPanel.ScrubTarget.CHART_A).doClick());
        eq("the reset puts the birth time back where it was before either slide", ANCHOR, time(sky, "natalRing.time"));
        ok("and has nothing more to reset", !sky.scrubResets.get(SkymapPanel.ScrubTarget.CHART_A).isEnabled());

        mode(sky, ChartMode.SYNASTRY, true, true, personB, "1 Day");
        ok("a synastry shows all three bars, " + shown(sky), shown(sky).equals("CHART_A CHART_B SKY"));
        slide(sky, SkymapPanel.ScrubTarget.CHART_B, -2);
        eq("Chart B's bar moves Chart B's birth back two days", personB.minusDays(2), time(sky, "outerRing.time"));
        eq("and not Chart A's", ANCHOR, time(sky, "natalRing.time"));
        eq("nor the sky", ANCHOR, time(sky, "skyRing.time"));
        slide(sky, SkymapPanel.ScrubTarget.SKY, 4);
        eq("the sky's bar moves the sky four days", ANCHOR.plusDays(4), time(sky, "skyRing.time"));
        eq("and neither birth", personB.minusDays(2), time(sky, "outerRing.time"));
        SwingUtilities.invokeAndWait(() -> sky.scrubResets.get(SkymapPanel.ScrubTarget.CHART_B).doClick());
        eq("Chart B's reset puts that birth back", personB, time(sky, "outerRing.time"));

        mode(sky, ChartMode.COMPOSITE_MIDPOINT, true, true, personB, "1 Day");
        ok("a composite shows both people's bars, " + shown(sky), shown(sky).equals("CHART_A CHART_B SKY"));

        // The globe reads the same moments.
        mode(sky, ChartMode.SINGLE, true, false, personB, "1 Day");
        SwingUtilities.invokeAndWait(() -> sky.setGlobeMode(true));
        double[] before = ((double[]) field(sky, "natalRing.lon")).clone();
        slide(sky, SkymapPanel.ScrubTarget.CHART_A, 5);
        int moon = com.zodiacomputing.ourania.astro.Bodies.indexOf("moon");
        double moved = com.zodiacomputing.ourania.astro.Aspects.separation(before[moon], ((double[]) field(sky, "natalRing.lon"))[moon]);
        ok("on the globe, Chart A's bar moves Chart A's Moon five days, " + Math.round(moved) + " degrees",
            moved > 50 && moved < 80);
        SwingUtilities.invokeAndWait(() -> {
            sky.setGlobeMode(false);
            sky.scrubOrigins.clear();
            sky.refreshScrubBars();
        });
        mode(sky, ChartMode.SINGLE, false, false, personB, "1 Hour");
    }

    /**
     * David: "for the scrubbers when you pull them forward or backward can you have the time keep
     * going until released". Held for real time on the real Swing timer, not by calling the rate.
     */
    private static void shuttle(SkymapPanel sky) throws Exception {
        eq("the middle does not run", 0.0, SkymapPanel.shuttleRate(0));
        eq("nor the edge of the dead zone", 0.0, SkymapPanel.shuttleRate(SkymapPanel.SHUTTLE_DEAD_ZONE));
        int runsInside = 0;
        for (int v = -SkymapPanel.SHUTTLE_DEAD_ZONE; v <= SkymapPanel.SHUTTLE_DEAD_ZONE; v++) {
            runsInside += SkymapPanel.shuttleRate(v) == 0.0 ? 0 : 1;
        }
        eq("no notch inside the dead zone runs on", 0, runsInside);
        eq("a full pull forward is the top rate", SkymapPanel.SHUTTLE_MAX_RATE,
            SkymapPanel.shuttleRate(SkymapPanel.SCRUB_SLIDER_REACH));
        eq("and back, the same rate backwards", -SkymapPanel.SHUTTLE_MAX_RATE,
            SkymapPanel.shuttleRate(-SkymapPanel.SCRUB_SLIDER_REACH));
        boolean climbs = true;
        for (int v = SkymapPanel.SHUTTLE_DEAD_ZONE + 1; v <= SkymapPanel.SCRUB_SLIDER_REACH; v++) {
            climbs &= SkymapPanel.shuttleRate(v) > SkymapPanel.shuttleRate(v - 1);
        }
        ok("the further the pull, the faster it runs", climbs);

        final ZonedDateTime personB = ZonedDateTime.of(1985, 3, 2, 14, 30, 0, 0, ZoneId.of("UTC"));
        mode(sky, ChartMode.SINGLE, false, false, personB, "1 Hour");
        javax.swing.JSlider bar = sky.scrubSliders.get(SkymapPanel.ScrubTarget.SKY);
        final int reach = SkymapPanel.SCRUB_SLIDER_REACH;

        SwingUtilities.invokeAndWait(() -> {
            bar.setValueIsAdjusting(true);
            bar.setValue(reach);
        });
        Thread.sleep(600);
        ZonedDateTime early = time(sky, "skyRing.time");
        // Then a busy event thread, as a slow redraw makes it: three quarter-second stalls, which
        // Swing's timer answers by coalescing the ticks it could not deliver.
        for (int i = 0; i < 3; i++) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Thread.sleep(50);
        }
        ZonedDateTime later = time(sky, "skyRing.time");
        long earlyHours = java.time.Duration.between(ANCHOR, early).toHours();
        long laterHours = java.time.Duration.between(ANCHOR, later).toHours();
        ok("held all the way forward, the sky runs on past where the knob sits: +" + earlyHours + "h",
            earlyHours > reach);
        ok("and keeps going while it is held: +" + earlyHours + "h then +" + laterHours + "h",
            laterHours > earlyHours + 5);
        ok("at about the top rate, not faster: +" + laterHours + "h in 1.5 s",
            laterHours <= reach + Math.ceil(SkymapPanel.SHUTTLE_MAX_RATE * 3.0));
        // Not slower either: the run is by the clock, so a busy event thread that coalesces the
        // timer's ticks cannot shorten it. Six tenths of the nominal distance, for scheduling slack.
        ok("and not far under it, however busy the redraw: +" + laterHours + "h in 1.5 s",
            laterHours >= reach + SkymapPanel.SHUTTLE_MAX_RATE * 1.5 * 0.6);

        SwingUtilities.invokeAndWait(() -> bar.setValueIsAdjusting(false));
        ZonedDateTime released = time(sky, "skyRing.time");
        ok("letting go ends the scrub", !sky.isScrubbing());
        eq("and the knob springs back", 0, bar.getValue());
        ok("where it was let go, the sky stays: +" + java.time.Duration.between(ANCHOR, released).toHours() + "h",
            java.time.Duration.between(ANCHOR, released).toHours() >= laterHours);
        Thread.sleep(400);
        eq("and does not run on after", released, time(sky, "skyRing.time"));

        SwingUtilities.invokeAndWait(() -> {
            bar.setValueIsAdjusting(true);
            bar.setValue(-reach);
        });
        Thread.sleep(1000);
        SwingUtilities.invokeAndWait(() -> bar.setValueIsAdjusting(false));
        long back = java.time.Duration.between(released, time(sky, "skyRing.time")).toHours();
        ok("held back, it runs backwards past the knob: " + back + "h", back < -reach);

        // Inside the dead zone the knob is an offset and nothing more.
        ZonedDateTime from = time(sky, "skyRing.time");
        SwingUtilities.invokeAndWait(() -> {
            bar.setValueIsAdjusting(true);
            bar.setValue(SkymapPanel.SHUTTLE_DEAD_ZONE);
        });
        Thread.sleep(800);
        SwingUtilities.invokeAndWait(() -> bar.setValueIsAdjusting(false));
        eq("a small pull held still is just that many steps", from.plusHours(SkymapPanel.SHUTTLE_DEAD_ZONE),
            time(sky, "skyRing.time"));

        // Chart A's bar runs Chart A's birth, and the sky holds still.
        mode(sky, ChartMode.SINGLE, true, false, personB, "1 Hour");
        javax.swing.JSlider a = sky.scrubSliders.get(SkymapPanel.ScrubTarget.CHART_A);
        SwingUtilities.invokeAndWait(() -> {
            a.setValueIsAdjusting(true);
            a.setValue(reach);
        });
        Thread.sleep(1000);
        SwingUtilities.invokeAndWait(() -> a.setValueIsAdjusting(false));
        long birth = java.time.Duration.between(ANCHOR, time(sky, "natalRing.time")).toHours();
        ok("Chart A's bar held runs Chart A's birth on: +" + birth + "h", birth > reach);
        eq("and leaves the sky where it was", ANCHOR, time(sky, "skyRing.time"));
        SwingUtilities.invokeAndWait(() -> {
            sky.scrubOrigins.clear();
            sky.refreshScrubBars();
        });
        mode(sky, ChartMode.SINGLE, false, false, personB, "1 Hour");
    }

    private static void mode(SkymapPanel sky, ChartMode m, boolean natal, boolean transits,
                             ZonedDateTime personB, String step) throws Exception {
        reset(sky, step);
        SwingUtilities.invokeAndWait(() -> {
            try {
                set(sky, "chartMode", m);
                set(sky, "innerIsBirthChart", natal);
                set(sky, "showTransitChart", transits);
                set(sky, "outerRing.time", personB);
                sky.scrubOrigins.clear();
                sky.updateChartData();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Which bars are showing, in order. */
    private static String shown(SkymapPanel sky) {
        StringBuilder sb = new StringBuilder();
        for (SkymapPanel.ScrubTarget t : sky.scrubBars.keySet()) {
            if (sky.scrubBars.get(t).isVisible()) {
                sb.append(sb.length() == 0 ? "" : " ").append(t.name());
            }
        }
        return sb.toString();
    }

    /** Drags one bar's knob to a value and lets go, as a reader does. */
    private static void slide(SkymapPanel sky, SkymapPanel.ScrubTarget t, int value) throws Exception {
        javax.swing.JSlider slider = sky.scrubSliders.get(t);
        SwingUtilities.invokeAndWait(() -> {
            slider.setValueIsAdjusting(true);
            slider.setValue(value);
            slider.setValueIsAdjusting(false);
        });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void press(Component c, int id, int x, int y, boolean shift) {
        int mods = (shift ? InputEvent.SHIFT_DOWN_MASK : 0)
            | (id == MouseEvent.MOUSE_DRAGGED ? InputEvent.BUTTON1_DOWN_MASK : 0);
        c.dispatchEvent(new MouseEvent(c, id, System.currentTimeMillis(), mods, x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static Object field(Object o, String name) throws Exception {
        return CheckReflect.get(o, name);
    }

    private static void set(Object o, String name, Object value) throws Exception {
        CheckReflect.set(o, name, value);
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
