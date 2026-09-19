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

## K7 — Synastry angle clicks · **CLOSED 2026-09-06 (labelling built)**

**Decision.** Keep the asymmetry. It is a relational law, not an inconsistency: **the Law of
the Sovereign Host.** Chart A owns the house frame on screen, so clicking Chart A's angle reads
natal; Chart B is visiting Chart A's houses, so clicking Chart B's angle reads cross-chart.
Forcing symmetry would break it in either direction - "both natal" makes the reader work out
by hand where B's Ascendant lands, and "both cross-chart" denies A their own baseline.

**Status.** The behaviour was correct and stays. The labelling that makes the direction visible
was built on 2026-09-06 and this decision is now closed.

**Built.** `SkymapPanel.AngleRole` names the three cases - ANCHOR, BRIDGE, SKY - and the angle
card is headed accordingly: Chart A's angle reads *"Chart A · natal anchor, the sovereign
host"*, Chart B's reads *"Chart B → Chart A · ASCENDANT OVERLAY — interpersonal bridge"* and
names which of Chart A's houses the visiting angle lands in, and a sky angle says it is a
moment rather than a person.

**Silent on a single chart.** One set of angles raises no question of whose they are, and a
badge on every card would teach the reader to ignore badges - which would cost exactly the
case this exists for.

**One rule, two doors.** The wheel click and the placements-panel link both ask
`angleRoleFor`, rather than each carrying its own ternary. Two copies do not diverge the day
they are written; they diverge the day one is edited.

**Found while building it.** The first guard used `isRelationshipChart()`, which in
`SkymapPanel` means a *composite* specifically - synastry has its own predicate - so the
banner was silent in precisely the case K7 is about. The method reads as though it means "a
chart about a relationship"; a probe caught it, reading would not have.

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

## K11 — The interpretive hierarchy · **BUILT 2026-09-19 (`045495f5`)**

> Built as written below. `InterpretationPanel.natalPieces` splits the reading into thirteen
> pieces; `generatePlanetHtml` joins them in the old order (byte-identical over 288 readings, so
> the selection pane is unchanged) and `hierarchyHtml` arranges them for the Interpretation tab.
> The synthesis needed no change - measured, it already ranks with no Level 3/4 input and reads
> sign, house, aspects - so it got a guard instead. `InterpretationHierarchyCheck` holds all of
> it; five mutations, all caught.

**Decision (David, 2026-09-19).** Every interpretation is built from the macro foundation down
to the micro nuance, in four levels:

| Level | Holds | Scope | Job |
|---|---|---|---|
| **1. Foundation** | The **planet** (the actor), the **house** (the arena) and the **sign** (the costume: element, modality, essential dignity - domicile or exaltation). **Major Arcana** card for the planet and sign. | 30° | What drive, where in life, and in what style |
| **2. Geometry** | **Aspects** to other bodies, and contacts with the **angles** (Ascendant, Midheaven) | orbs | The conversations and triggers |
| **3. Refinement** | **Triplicity** and **terms/bounds**, then the **decan** (10°) with its sub-ruler - "middle management", a sub-flavour that never changes the sign. The decan's **Minor Arcana pip** card (2-10) sits here. | 10° and the bounds | Sub-rulers that refine the expression |
| **4. Fine detail** | The **Sabian symbol** and **degree meaning** (1°), and the **lunar mansion** (~12°51') | 1° / 12°51' | Poetic imagery for the exact degree; the ambient setting |

**Tarot is inside the hierarchy, not beside it** (Golden Dawn): the Major Arcana map to planets
and signs (Level 1), the 36 numbered pips to the 36 decans (Level 3).

**Lunar mansions are atmosphere, not character.** They are chiefly activated by the transiting
Moon (electional and horary timing - Avelar & Ribeiro, *On the Heavenly Spheres*). So:
- **the natal Moon** - its mansion is the instinctual habitat and emotional baseline, and carries
  real weight;
- **every other natal body** - its mansion is the landscape the story takes place in, a faint
  thematic colour that never alters the planet's function or dignity;
- **a transiting Moon** - the mansion leads, as the weather of the day.

**Where it applies: the Interpretation tab only** (David: "all the info should stay the same when
an object or area is selected ... the hierarchy should just be in interpretation tab"). The
selection pane keeps every section and its current order.

That matters because the two share their source: the selection pane is cut from
`InterpretationPanel.generatePlanetHtml` (through `OuraniaWindow.planetReadingHtml`), the same
method that renders the tab. So `generatePlanetHtml` itself is **not reordered**. The hierarchy
is a layer over it in the tab alone - it takes the same sections and arranges them, the way
`SkymapPanel.selectionHtml` already cuts and folds them - so the two surfaces can never disagree
about a word of prose, only about its order. Anything added for the hierarchy (element, modality,
dignity, triplicity, bounds) is added in that layer, not in the shared method.

**How the reading presents it.** No level headings in the main reading - a stack of labelled
tiers is tiring to read. The main view is **one continuous account in hierarchy order**, and a
collapsible **Layer breakdown** below it shows the four levels explicitly for anyone inspecting
the stack.

**Two things built differently from how they were first put, and why:**
- *"One synthesised paragraph."* The app will not **write** new prose to join the layers: 600,000+
  entries already make authored and generated text hard to tell apart, which is what C7 is about.
  The main account is **assembled from the opening sentences of each layer's own entry**, in
  hierarchy order; every sentence in it is still one somebody wrote.
- *"Mansion +1, domicile +5, angularity +5."* Those figures assume a points system this engine
  does not have. The **principle** is adopted - a non-Moon mansion is a faint colour that can never
  outvote a Level 1 or 2 fact - and is set against K8's actual weights (lights and angles 5.0,
  conjunction 3.0, and so on), measured, not guessed.

**Synthesis weighting follows the same order**: what the synthesis says is ranked Level 1 first,
Level 4 last, and no quantity of Level 4 detail can outrank a single Level 1 or 2 fact - the same
guarantee K8 gives minor bodies against the major ones.

**Already in place above and beyond the planet.** The chart-wide layer - hemispheres, element and
modality balance, chart shape, sect, the chart ruler - is L5's Gestalt and opens the synthesis;
aspect patterns (T-squares, grand trines, kites) are computed and lead the placements list; the
timing layer - transits, progressions, solar arcs, profections, releasing - is F1-F7. K11 orders
the planet reading inside that frame; it does not rebuild the frame.

**What the planet reading lacks today, measured 2026-09-19** (`InterpretationPanel.generatePlanetHtml`):
the house and the aspects come last, after all the Level 4 detail; the sign section states no
element, modality or essential dignity; triplicity and bounds are not in it at all (they have
their own pages); the mansion is written as a trait for every body; and the tarot sits after
everything as its own section.

---

## K12 — The predictive pipeline and the Rule of Three · **STAGE 4 BUILT 2026-09-19; stages 1-3 to come**

> **Stage 4 built:** `astro.ThemeConvergence` pools `Convergence.collect`'s per-point targets into
> the four themes (named points, the rulers of the theme's houses, and those houses' occupants),
> counts independent technique families per theme - echoes excluded, a technique on several
> points counted once - adds the profection family when the year falls on a theme's house, and
> makes a theme a **headline only at three**. The synthesis's timing section opens with it,
> headlines with their testimonies listed, the rest as background trends. `ThemeConvergenceCheck`
> 18; three mutations caught. **Next:** the activation filter (stages 1 and 3), the progressed
> Moon clock (stage 2), then Mars/Sun dating.

**Decision (David, 2026-09-19).** Prediction is a noise filter. On any day there are dozens of
minor transits, and treating each as a prediction produces contradictions daily. A major event
is only predicted when **at least three independent techniques name the same theme in the same
window** - the classical Rule of Three - through four stages:

1. **Macro time-lord filter (annual profections).** The Ascendant advances one sign a year; the
   ruler of that sign is **Lord of the Year** and its house is the year's **topic**. The lord
   and its houses carry a **3x** multiplier. A transit involving the lord, or through the
   profected house, is a **primary driver**; an **un-activated transit is background noise**.
2. **Internal growth (secondary progressions)** - a day for a year; the **progressed Moon** as
   the mid-term clock; progressed-to-natal and progressed-to-progressed aspects at a **tight
   orb (1° or less)**. An active progression is the "loaded gun".
3. **External catalyst (transits).** Slow bodies (Jupiter to Pluto) making conjunctions, squares
   and trines to **active** natal or progressed points; **fast catalysts** (Mars, the Sun,
   stations, eclipses) to **date** the event. A transit to an inactive point passes with
   little notice; one to a point already active in progressions or profections triggers.
4. **Theme convergence matrix.** Everything is gathered into **theme buckets** - Career and
   status (10th, MC, Saturn, Sun, the 10th's lord), Relationship (7th, DSC, Venus, Lot of Eros,
   the 7th's lord), Health and vitality (1st, 6th, ASC, Mars, Sun, Moon), Home and relocation
   (4th, IC, Moon, Mercury, 3rd/9th) - and a headline is only promoted when **three
   testimonies** agree; otherwise it is a background trend.

**Measured against the engine, 2026-09-19.** Much of this exists as L8 (`astro.Convergence`,
K8) and L6 (`astro.Topics`); the pipeline is a reorganisation and three additions, not a rewrite.

| Stage | Already built | Missing |
|---|---|---|
| 1 | `Profection`: lord of the year and house; the lord is a witness, and a witness whose moving body is the lord is multiplied (K8). Zodiacal releasing gates every witness: L1 lord x3, L2 x2, peak x2.5, loosing x4. | Transits **through the profected house**, and transits **to** the lord, as primary; un-activated transits **demoted** rather than voting equally. |
| 2 | `Progressions` and `SolarArc` vote at their **dated moment of exactness** - stricter than a 1° orb, and chosen because a contact stays within orb about two years. | The **progressed Moon** as its own clock; progressed-to-progressed aspects. |
| 3 | Slow-body transits vote when they **perfect** in the window (the orb is weather, the perfection is the event); eclipses and stations are families; a station duplicating a transit is discounted, not double-counted. | "Transit to an **active** point triggers": today every perfection votes whether or not its target is active. **Mars and the Sun as date-pinpointers** - excluded as voters on purpose (the Sun conjuncts every natal point every year and so agrees with everything), and should stay excluded as voters but be used to date a converged event. |
| 4 | Convergence counts distinct **families** per natal point ("two transits to the same point are one witness, not two"), with thresholds relative to the chart's own distribution. `Topics` groups houses, rulers and significators into topics by three witnesses - for the natal chart. | **Theme buckets** for timing (grouping by theme, not by single natal point) and the **Rule of Three gate** as the headline test, with its testimonies listed. |

**Built differently from how the spec puts it, and why:**
- *The score (profection +1.5, progression +1.0, transit +1.0; confidence >= 3).* The rule is
  adopted - **three independent testimonies**, counted by family as now - but the points are not
  bolted on beside K8's weights; the gate is on the count of independent families agreeing on a
  theme, and K8's weights keep ranking within it.
- *The headline text* ("Major Career Elevation & Recognition Expected"). A converged theme will
  be **named and its testimonies listed**, but the app will not write new predictive prose: the
  same C7 reason as K11.

**Order of work, when it is taken up:** theme buckets over Convergence's per-point targets
(stage 4), then the activation filter (stages 1 and 3), then the progressed Moon clock (stage 2),
then Mars/Sun dating of converged themes.

---

## K13 — Momentary horoscopy: horary and electional · **DECIDED 2026-09-19, NOT YET BUILT (F10)**

**Decision (David, 2026-09-19).** A section for charts of a single instant, in its two branches:
**horary** (a question judged by the chart of the moment it is asked) and **electional /
inceptional** (*katarche*: the quality of a moment to begin something). It is a deterministic
judgement over significators, applying aspects and the dynamics of light - not a transit window.

**Horary, four steps.**
1. **Significators.** The querent: the Ascendant, its domicile ruler, and the Moon. The quesited:
   the ruler of the house of the matter - 2nd money and lost things; 3rd communications and
   siblings; 4th home and property; 5th romance, children, speculation; 6th health, work,
   service; 7th partners, relationships, lawsuits; 10th career and status.
2. **Radicality.** Whether the chart is fit to judge; the planetary-hour test - the hour ruler
   (Chaldean order from sunrise) sharing the Ascendant ruler's nature or triplicity.
3. **Perfection and the dynamics of light**, applying aspects only, **before either significator
   changes sign**: direct perfection (conjunction, trine, sextile) is yes; **translation**
   (a faster body, often the Moon, separating from one and applying to the other);
   **collection** (both applying to a slower third); **prohibition / frustration** (a third body
   perfecting with one first); **refranation** (a significator stationing before perfection);
   the **void-of-course Moon** (no further applying major aspect in its sign - nothing comes of it).
4. **The outcome**, with the reasons that produced it.

**Electional, two measures.** **Planetary day and hour** (Sunday Sun ... Saturday Saturn; the
hour from sunrise in Chaldean order) and the kind of work each hour favours; and **angularity**:
Jupiter or Venus on an angle (above all the 1st and 10th) or aspecting the Moon raises a moment;
Mars or Saturn undignified on the Ascendant or Midheaven, or the South Node on a significator,
lowers it.

**Measured against the engine, 2026-09-19.** The apparatus is mostly here, as F10 has said since
2 September; nothing presents it as a judgement.

| Piece | Status |
|---|---|
| Applying vs separating aspects | built - `Aspects.Hit.applying` |
| Translation and collection of light | built - `TransferOfLight`, chart-wide; not yet restricted to two named significators or to "before a sign change" |
| Void-of-course Moon | built - `ChartFrame.moonVoidOfCourse` (A9) |
| Essential dignity, sect, domicile rulers of houses | built - `Dignity`, `Sect` |
| Mansions with electional clauses | built - `LunarMansions` |
| Planetary day and hour | **missing** - needs sunrise and sunset (check `astro.Horizon` first) |
| Significators from a question's house | **missing** |
| Prohibition, frustration, refranation | **missing** |
| Radicality check | **missing** |
| Angular benefic/malefic scoring for a moment | **missing** |
| A screen: ask a question now, or rate a moment | **missing** |

**Built differently from how the spec puts it, and why:**
- *The confidence arithmetic* (+3 perfection, +2 translation, -2 void Moon; >= 2 is positive). Horary
  is judged by the *first decisive* testimony - a prohibition overrides a perfection, refranation
  undoes it - so the engine returns the **verdict with its chain of reasons in order**, not a sum
  that lets a void Moon and a translation cancel out. It can still expose a score for ranking
  elections, where comparing moments is the point.
- *"Mercury hour: optimal for code commits."* The hour and day are computed and named with their
  traditional significations; recommendations phrased for particular modern tasks are not
  written by the app (the C7 reason again), though David may supply them as prose.

---

## What these change about the audit

Three items close with no work, one of them only needing a label. Six become buildable. Nothing here is still waiting on a
decision, which is the first time that has been true since the audit was written.
