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

## K1 — Return charts, and the calendar's stations · **BUILT 2026-09-03**

**Decision.** One principle answers both: selective hierarchy.
- Solar and Lunar returns: full standalone wheel, own house cusps, relocatable.
- Mercury, Venus and Mars returns: return angles only, as trigger nodes over the natal wheel.
- Calendar stations: slow bodies (Jupiter through Pluto, plus Chiron) by default; Mercury kept
  as an explicitly badged exception; Venus and Mars hidden by default.

**Why the Mercury exception is not an inconsistency.** It is a cultural one, badged as such.
The audit flagged Mercury's presence beside the slow bodies as a contradiction; it is now a
declared exception instead.

**Built — returns.** `Returns.Scope` and `Returns.scopeOf(body)` are the one place the rule
lives, and `contacts()` reads it, so policy and rendering cannot drift apart. An angles-only
return contributes its Ascendant and MC and nothing else: measured on a 1990 chart, Mercury,
Venus and Mars gave 25 contacts across three returns each, every one an angle, while the solar
return kept 23 contacts over 8 distinct points. `Return` now records the `lat`/`lon` it was
cast for, so a relocated return can say so — the engine always accepted a place, but the
result did not carry it, which made a relocated return and a natal-place one the same object
with different numbers inside.

**Jupiter and Saturn the decision does not name.** They read as full wheels: a Saturn return
is the canonical return-as-chapter, and at one every 12 and 29 years there is no crowding
argument. Flagged rather than assumed — it is a one-line change in `scopeOf` if that is wrong.

**Built — stations.** `Almanac.STATION_DEFAULT` admits Mercury, Jupiter, Saturn, Uranus,
Neptune, Pluto and Chiron; Venus and Mars need `annualCalendar(..., includeFastStations)`.
Measured for 2026: default gives Chiron 2, Jupiter 2, Mercury 6, Neptune 2, Pluto 2, Saturn 2,
Uranus 2 — 119 calendar entries, rising to 121 with the fast stations. All six Mercury entries
carry `MERCURY_STATION_NOTE`; nothing else is badged.

**The calendar and the degree-contact feed deliberately disagree.** `datedMoments` still keeps
all nine retrograders, because a Venus station landing on a natal degree is a real contact and
K8's weighting already decides how loudly it speaks. The calendar is a list a human reads top
to bottom, so it admits only what is rare enough to date a year by. Making the two agree would
be the easy mistake.

**Door.** Side panel → Tables → **Returns**, rendering through `ChartTables.returns`.
NavigationCheck picked it up unprompted, 93 → 94.

**Suites.** Narrowing the annual three took ReturnsCheck from 1,431 checks to 1,033 — it kept
passing and simply verified less, and reverting `scopeOf` would have restored the count and
stayed green. Part F now asserts the rule directly (1,069 checks). TransitCheck gained 8
checks pinning the station split.

**Follow-on: the return contacts were ordered by tightness, which read as importance.** The
first render of the table opened with Pholus exactly on the Sun, Chiron on the Ascendant and
Eris on the Moon — all a tenth of a degree, all above Mars conjunct the Sun two degrees off.
`Contact.weight` now carries K8's hierarchy (body × body × aspect × orb slack) and the sort
reads it. `Convergence.bodyWeight` and `aspectWeight` were made public rather than copied, so
the return surface and the convergence engine cannot be tuned apart.

**Precision comes off `orbUsed`, never `type.maxOrb`.** A conjunction's `maxOrb` is
effectively unbounded; dividing by it scores every conjunction as exact and restores the
defect. This is the same trap K5's measurement fell into once already.

**The table splits on the return end alone**, not on both ends as the reading's aspect lists
do. A return is directional: the return chart is what arrives, the natal point is what it
arrives at, so a return Pholus on the natal Sun is background however important the Sun is.
Keyed on both ends the split never fired once on the default chart.

**Part G pins it, mutation-tested.** Reverting the sort to orb-only fails 31 of 1,233 checks,
including "no return is led by a minor body while a planetary contact sits below it".

**One rule for every surface.** The reading's aspect lists, the aspect card and the returns
table were each deciding what counted as background on their own, and two had already drifted:
the reading asked whether *both* ends were minor, the returns table whether the *arriving* end
was. Both answers were defensible; the pair of them was not, because nothing said which
question the app was asking.

`Bodies.hasPrimaryActor(acting, receiving, directional)` is now that question, asked once. What
differs between the surfaces is not policy but the contact: two natal bodies belong to one
person and aspect each other mutually, so either end can carry it; a return arrives from
outside the chart it lands on, so the arriving end is what is making the statement. A return
Pholus on the natal Sun is background however important the Sun is — the Sun is not what is
happening, it is what is being happened to.

Pure refactor: all three surfaces render exactly as before. BodyCheck Part E pins both
branches and the reason they differ; collapsing them to one fails 4 of 1,140 checks.

**The same defect was in the transit list, and worse.** I judged it milder by reading the
comparator instead of rendering the output — the same mistake as measuring a function rather
than the mechanism. The sort ranked natal targets and then fell straight through to orb, so
the arriving body never entered it: on the default chart, position one was Eros septile the
Ascendant at 0.2°, and the whole angular block — the most prominent group in the reading — ran
Eros, Pholus, Vesta, Chiron, Juno, Eris, Ceres, Pallas before a single transiting planet.
53 of 102 contacts had a minor body as the arriving end.

`Transits.Hit.weight` now carries the hierarchy and sits between the target ranking and the
orb tie-break. The grouping is deliberately kept — angular first, then targets by prominence —
because a reading that gathers every contact to one natal point together is easier to read
than a flat score. Only the tie-break inside each group changed. Eros moved from #1 to #28.
`Snapshot.timeLayer` splits each target's block on the shared rule.

**`intensity` had to stay untouched.** Convergence multiplies its own body and aspect weights
over it, so folding the hierarchy in would count it twice and re-tune the predictive engine as
a side effect of a display fix. Part L guards this structurally.

**Two mistakes of mine caught by writing the assertion rather than by reasoning.** The weight
was computed one line above the ×5 station boost, so a stationing transit — a body that has
stopped and sits on a degree for weeks — lost that from its ranking. And my first purity check
used 1.3 as intensity's ceiling, which the station boost takes to 6.5. Both fixed.

Mutation-tested: removing the weight from the sort fails 48 checks, folding the hierarchy into
intensity fails 1. TransitCheck 3,968 → 4,256; twelve suites clear.

## K3 — Anaretic degree · **BUILT 2026-09-03 (closes F11)**

**Decision.** Anaretic is exactly 29°00'00" to 29°59'59", zero tolerance either side. 28°59'59"
is not anaretic. 0° is a **separate** condition, not the same one: 0° is a phase beginning,
29° a phase ending.

**Sources.** March and McEvers on 0 and 29 as opposite ends of one boundary; Boland on the
0-to-29 convention; Leinbach on the final degree as a concentrated challenge.

**Note.** The corpus already stores these under `_30` keys, since the 30th degree is 29°.

## K4 — Asteroid-to-asteroid prose · **BUILT, PREMISE CORRECTED**

**Decision.** Leave the 60 cells empty. Keep the geometry in the aspect grid; suppress the
narrative. Clicking one shows a data badge naming the orb and saying plainly that the contact
sits below the threshold of character analysis.

**Sources.** Burk on the ten planets as the only primary actors; Cunningham on brain clutter;
Tompkins on the back-row violinist turning a page.

**The premise was out of date.** The decision rests on those 60 cells being empty. Measured
2026-09-03: all 60 are written. Following the letter would have meant deleting 60 entries
somebody composed, and the project's rule is never to rewrite a shared data file.

**Built instead — the reasoning, which is about weight rather than existence.** Burk's claim is
that a contact between two minor bodies has no primary actor to express it, not that no text
should exist for it. So the prose stays and the claims made *above* it change:

- `Bodies.isMinor(String)` — one definition of the class (asteroid, node, Arabic lot, Lilith).
  Angles are `Kind.ANGLE` and correctly excluded; verified against all 10 planets and 4 angles.
- `InterpretationPanel` — a minor-to-minor aspect card carries a "Subtle esoteric resonance"
  note saying the contact has no primary actor and works as background. Verified: fires for
  Vesta/Ceres and Juno/Pallas, silent for Sun/Ceres and Sun/Saturn, prose kept in all four.
- `NarrativeSynthesizer` — these contacts no longer print under a heading reading "Major
  Contacts", which was the app asserting something false about its own weight. They move to
  "Background Resonance". The geometry stays in the aspect grid throughout, as decided.

**Found while verifying.** SynastryCheck was still asserting the pre-Burk contract and failed
42 of 65,554 — K5 shipped without re-running it. The suite now knows a calculated point cannot
reach an angle. All 25 suites clear.

## K5 — What the wheel draws · **BUILT 2026-09-03**

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
