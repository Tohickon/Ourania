package com.zodiacomputing.ourania.gui;

/**
 * The layers of the chart the reader can fold away, each with its own bloom.
 *
 * <p><b>Partner and Sky are not here, and that is the distinction.</b> Those two change what
 * the chart <i>is</i> - opening the partner ring makes it a synastry, and the engine has to
 * be told - so they go through ChartMode and keep the blooms that already drive the bands.
 * These change only what is <i>drawn</i>. Mixing the two would put a second writer on the
 * mode, which is the defect this panel has shipped twice.
 *
 * <p>Natal is here rather than fixed because David asked for it to fold like the others, and
 * it can: hiding the natal glyphs does not stop the chart being a natal chart.
 *
 * <h3>Why this is its own file (M11, stage 4)</h3>
 *
 * <p>It was {@code SkymapPanel.Layer}, and {@link GlobeRenderer} asks for it forty-four times -
 * more than it asks anything else of the panel. The renderer is meant to draw the globe on the
 * phone as well as the desktop, and it cannot while the name of a layer lives inside a Swing
 * component. Nothing about the enum changed; it is eight constants either way.
 *
 * <p>Listed in {@code EngineIsolationCheck.SHARED}, which the Android build reads out of that
 * file with a regex - one list, no second copy.
 */
public enum Layer {
    NATAL, DEGREES, SIGNS, DECANS, BOUNDS, MANSIONS, HOUSES, ASPECTS
}
