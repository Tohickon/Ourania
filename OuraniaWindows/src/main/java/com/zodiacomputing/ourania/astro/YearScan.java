package com.zodiacomputing.ourania.astro;

import java.util.Collections;
import java.util.List;

/**
 * What one profection year holds, found by scanning it: the transits that fire, the
 * perfections, solar arc and progressed contacts, returns, the progressed Moon's clock, the
 * mutual progressions, and the Sun and Mars touches used for dating.
 *
 * <p>Plain data, filled by the panel's year scan and read by the synthesized reading. It was
 * {@code SkymapPanel.YearScan} until 30 Sep, and that one reference is what tied the reading
 * - and through it the engine - to the whole desktop app. Every field is an engine type, so it
 * lives with them (M1).
 */
public final class YearScan {
    public List<Transits.EventHit> events = Collections.emptyList();
    public List<Transits.Perfection> perfections = Collections.emptyList();
    public List<SolarArc.Contact> arcs = Collections.emptyList();
    public List<Progressions.Contact> progressions = Collections.emptyList();
    public List<Returns.Contact> returns = Collections.emptyList();
    /** K12 stage 2: the progressed Moon's tenancies of the natal houses across the year. */
    public List<Progressions.Tenancy> moonClock = Collections.emptyList();
    /** K12 stage 2: aspects between two progressed bodies perfecting in the year. */
    public List<Progressions.Mutual> mutuals = Collections.emptyList();
    /** K12 stage 3: Sun and Mars touches on the themes' points, for dating - never voting. */
    public List<Transits.Perfection> catalysts = Collections.emptyList();
}
