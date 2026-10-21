package com.zodiacomputing.ourania.gui;

/**
 * The left rail's tabs since 2026-10-20: one per chart, with the same pages under each.
 *
 * <b>David, 2026-10-20:</b> "it shouldnt be separated by placements, predictions, and
 * interpretations it should be separated by charts with placements, interpretations and
 * predictions going under the chart tabs." A tab says which chart is read; the pages say what
 * is read about it. Choosing a tab puts its chart on the wheel, through the same door the ring
 * chips use (ChartSetupPanel.showChart), and a chip that changes the chart moves the tab.
 */
enum ChartTab {
    CHART_A("door.chart_a", "Chart A"),
    CHART_B("door.chart_b", "Chart B"),
    SKY("door.sky", "Sky"),
    SYNASTRY("door.synastry", "Synastry"),
    COMPOSITE("door.composite", "Composite");

    /** The rail's key for the tab. Not a page key, so no page can share it. */
    final String door;
    /** What the tab says when it has no person to be named for. */
    final String label;

    ChartTab(String door, String label) {
        this.door = door;
        this.label = label;
    }

    /** The tab for a rail key, or null. */
    static ChartTab ofDoor(String door) {
        for (ChartTab t : values()) {
            if (t.door.equals(door)) {
                return t;
            }
        }
        return null;
    }

    /**
     * The tab that describes what the wheel holds: a composite, two people, one of them, or the
     * sky. The one place this is decided, for a chip press and a start-up alike.
     */
    static ChartTab of(ChartMode mode, boolean innerIsBirthChart, boolean chartAIn,
            boolean chartBIn) {
        if (mode == ChartMode.COMPOSITE_MIDPOINT || mode == ChartMode.COMPOSITE_DAVISON) {
            return COMPOSITE;
        }
        if (mode == ChartMode.SYNASTRY) {
            return SYNASTRY;
        }
        if (!innerIsBirthChart) {
            return SKY;
        }
        return chartAIn ? CHART_A : chartBIn ? CHART_B : SKY;
    }
}
