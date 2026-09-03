# Settled decisions

Recorded 2026-09-03 from David's nine decision documents. Every item here was previously
listed as blocked in the completion audit, which meant the work could not be scoped, not that
it was hard. Each now has a decision, a source, and a status.

Status is measured against the code, not assumed: two of the eight were already implemented
and needed only the source that authorised them.

---

## K2 — Minor aspect orb cap · **ALREADY IMPLEMENTED**

**Decision.** All six minor aspects capped at 1.0 degree, halved to 0.5 in synastry.

**Sources.** Cunningham (a functional minor is active only inside 1 degree); Tompkins (2
degrees is "quite wide"; 1 degree for the quincunx and quintile series); Burk after Kepler
(higher harmonics need the smallest orbs, and the 5th, 8th and 12th harmonics are exactly what
these six are); Teal (1.0 predictive standard, 1.5 for the lights); March and McEvers (1 degree
maximum for calculated points).

**Status.** `Aspects.Type` already caps all six at 1.0 and `orbFor` halves for synastry. Ten
alternative schemes were measured before that was chosen, including Cunningham's own per-aspect
table, which came out worst at 35% minor share. No code changes. What this decision supplies is
the named source the audit said was missing.

## K9 — Tri-wheel aspect lines · **ALREADY SATISFIED**

**Decision.** The outer sky ring stays glyphs-only. Lines are drawn between the inner rings.
Transit contacts are shown on hover, not statically.

**Sources.** Cunningham on the tension of three-ringed printouts; Marks on the chart becoming a
"dense jungle of twisted vines" without hierarchy.

**Status.** The tri ring already draws glyphs and no lines, and hover focus already lights a
body's web while dimming the rest. The decision confirms the existing behaviour rather than
asking for new work.

## K7 — Synastry angle clicks · **ALREADY CORRECT (needs labelling)**

**Decision.** Keep the asymmetry. It is a relational law, not an inconsistency: **the Law of
the Sovereign Host.** Chart A owns the house frame on screen, so clicking Chart A's angle reads
natal; Chart B is visiting Chart A's houses, so clicking Chart B's angle reads cross-chart.
Forcing symmetry would break it in either direction - "both natal" makes the reader work out
by hand where B's Ascendant lands, and "both cross-chart" denies A their own baseline.

**Status.** The behaviour is correct and stays. What is missing is the labelling that makes the
direction visible: a natal card should say it is the anchor, a cross-chart card should show the
B to A direction.

**This also settles the `showAngleAt` question.** The audit flagged the Chart A pass handing a
literal `false` to `getAspectType` as a suspected bug. Under this decision it is right: Chart
A's angle is read natally, so it is not a synastry pair, and `false` is the correct argument.
Closed as intended behaviour.

---

## K1 — Return charts, and the calendar's stations · **TO BUILD**

**Decision.** One principle answers both: selective hierarchy.
- Solar and Lunar returns: full standalone wheel, own house cusps, relocatable.
- Mercury, Venus and Mars returns: return angles only, as trigger nodes over the natal wheel.
- Calendar stations: slow bodies (Jupiter through Pluto, plus Chiron) by default; Mercury kept
  as an explicitly badged exception; Venus and Mars hidden by default.

**Why the Mercury exception is not an inconsistency.** It is a cultural one, badged as such.
The audit flagged Mercury's presence beside the slow bodies as a contradiction; it is now a
declared exception instead.

## K3 — Anaretic degree · **BUILT 2026-09-03 (closes F11)**

**Decision.** Anaretic is exactly 29°00'00" to 29°59'59", zero tolerance either side. 28°59'59"
is not anaretic. 0° is a **separate** condition, not the same one: 0° is a phase beginning,
29° a phase ending.

**Sources.** March and McEvers on 0 and 29 as opposite ends of one boundary; Boland on the
0-to-29 convention; Leinbach on the final degree as a concentrated challenge.

**Note.** The corpus already stores these under `_30` keys, since the 30th degree is 29°.

## K4 — Asteroid-to-asteroid prose · **TO BUILD (suppression, not prose)**

**Decision.** Leave the 60 cells empty. Keep the geometry in the aspect grid; suppress the
narrative. Clicking one shows a data badge naming the orb and saying plainly that the contact
sits below the threshold of character analysis.

**Sources.** Burk on the ten planets as the only primary actors; Cunningham on brain clutter;
Tompkins on the back-row violinist turning a page.

## K5 — What the wheel draws · **TO BUILD**

**Decision.** Three rules.
1. No aspects between two calculated points. Burk: such points "do not make aspects, they can
   only receive aspects" - no node-to-angle, no lot-to-lot, no node-to-lot lines.
2. Asteroid and point aspects are computed and listed, not drawn.
3. A Visual Aspect Mode with three settings: Essential (ten planets only, default),
   Manifestation (adds the four angles), Esoteric (everything, minors at low opacity).

## K6 — How much of a chart should read as unremarkable · **BUILT 2026-09-03**

**Decision.** Target 30-35%, against 4.5% today. Rename the state from "unremarkable" to
**Integrated Ground** and write it as a real finding - a place that needs no defending - rather
than an absence.

**Why not 44.3%.** The measured upper end reads as an unfinished product to a lay reader.

## K8 — Convergence weighting · **BUILT 2026-09-03**

**Decision.** Replace flat `atLeast(n)` counting with a hierarchical weighted model.
- Base weight = body weight x aspect weight x precision, where precision falls off across the
  orb, so a minor at 0.99 of its 1.0 cap scores near zero.
- Body weights: lights and angles 5.0, personal 3.0, social 2.0, outer 1.5, centaurs and major
  asteroids 0.5, esoteric points 0.2.
- Aspect weights: conjunction and opposition 3.0, square and trine 2.0, sextile 1.0, minors 0.3.
- **Zodiacal releasing becomes a multiplier, not a voter**: L1 time-lord x3.0, L2 x2.0, peak
  period x2.5, loosing of the bond x4.0, dormant x0.3.
- Thresholds become relative to the distribution rather than a fixed count.

**Why this solves "more voters worsened discrimination".** Minor bodies and wide minor aspects
carry fractional weights, so they cannot accumulate into a signal however many of them there
are. It also wires releasing into the engine, which the audit listed separately as blocked
behind this same decision.

---

## What these change about the audit

Three items close with no work, one of them only needing a label. Six become buildable. Nothing here is still waiting on a
decision, which is the first time that has been true since the audit was written.
