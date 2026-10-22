package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.ChartFrame;

/**
 * Which chart a page reads and how it speaks, for the tab in front (2026-10-22).
 *
 * <b>Why it exists.</b> David, 2026-10-22: "make sure each tab looks the same and has the same
 * structure under one chart as it should under another like synthesize under chart a should
 * have the same structure as synthesize under chart b and sky chart and transit. same for all
 * the tabs". So each page has one builder, and only two things change from tab to tab: the
 * chart it reads, and the voice - "Charts A and B should always be read from a personal
 * standpoint while sky chart should be read as more like a weather report and transits should
 * read like how that weather effects a person". This is the one place that decides both, so
 * no page can read one tab's chart in another tab's voice.
 */
enum ReadingVoice {
    /** Chart A or Chart B: the person's own chart, said to them. */
    PERSON,
    /** The sky alone, at the Sky row's place: a weather report for everyone there. */
    SKY,
    /** The Transits tab: the current sky placed in the person's houses, said to them. */
    TRANSIT;

    /** The voice for the tab in front. */
    static ReadingVoice of(SkymapPanel skymap, OuraniaWindow window) {
        if (window != null && window.readingTransits()) {
            return TRANSIT;
        }
        return skymap != null && skymap.skyAlone() ? SKY : PERSON;
    }

    /** The chart this voice reads: the sky in the person's houses on Transits, else the wheel's. */
    ChartFrame frame(SkymapPanel skymap) {
        if (skymap == null) {
            return null;
        }
        return this == TRANSIT ? skymap.skyInChartHouses() : skymap.radixChart();
    }

    /** "your", "the sky's" - the possessive a sentence about the chart takes. */
    String possessive() {
        return this == SKY ? "the sky's" : "your";
    }

    /** As above, at the start of a sentence. */
    String Possessive() {
        return this == SKY ? "The sky's" : "Your";
    }

    /** The chart, named: "your chart", "the sky", "the sky over your chart". */
    String chart() {
        return switch (this) {
            case PERSON -> "your chart";
            case SKY -> "the sky";
            case TRANSIT -> "the sky over your chart";
        };
    }

    /** The part of life a house stands for: a person's, or the place's collective one. */
    String area(int house) {
        return this == SKY ? SkyWeather.domain(house) : LifeAreas.of(house);
    }
}
