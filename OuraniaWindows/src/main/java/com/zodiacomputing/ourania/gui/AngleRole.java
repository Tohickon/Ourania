package com.zodiacomputing.ourania.gui;

/**
 * Which chart a clicked angle belongs to, and therefore what the card is claiming.
 *
 * <p><b>The K7 decision: the asymmetry is a relational law, not a bug.</b> Chart A owns the
 * twelve houses on screen, so clicking Chart A's angle reads natally - it is the baseline the
 * whole session is framed by. Chart B has no house boundaries here; its angles are visiting,
 * falling into Chart A's houses, so clicking one is inherently a cross-chart event. Forcing
 * symmetry breaks it either way: "both natal" makes the reader work out by hand where B's
 * Ascendant lands, and "both cross-chart" denies A their own baseline.
 *
 * <p><b>The behaviour was already right; what was missing was saying so.</b> The two cards were
 * identical in appearance while making different claims, which is the one thing a deliberate
 * asymmetry cannot afford - indistinguishable, it reads as inconsistency.
 *
 * <h3>Why this is its own file (M11, stage 4)</h3>
 *
 * <p>It was {@code SkymapPanel.AngleRole}, read by seven files including {@link GlobeRenderer},
 * which has to draw on the phone and cannot while the name of a role lives inside a Swing
 * component. Moved, not changed: three constants either way, and the rule above is the reason
 * there are three rather than two.
 *
 * <p>Listed in {@code EngineIsolationCheck.SHARED}, which the Android build reads out of that
 * file with a regex - one list, no second copy.
 */
public enum AngleRole {
    /** Chart A's own angle: the frame everything else is measured against. */
    ANCHOR,
    /** Chart B's angle, projected into Chart A's houses. */
    BRIDGE,
    /** A transit or sky angle - a moment passing over the chart, not a person. */
    SKY
}
