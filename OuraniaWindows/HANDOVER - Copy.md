# HANDOVER

**Last updated:** this session by **Claude** — tri-wheel implemented, sky-to-Chart-B fixed, all suites green
**Suites: all 22 run to completion — 170,648 checks, 0 failures.** Clean compile (javac exit 0).
AspectGridCheck **6,417** · DataCheck **82,479** · SynastryCheck **41,206** · TensionReleaseCheck 12,172 · JoyCheck 5,610 · LunarMansionCheck 4,759 · ZodiacSelfTest 3,838 · TransitCheck 3,827 · QuincunxCheck 2,241 · SnapshotOutputCheck 1,529 · ReturnsCheck 1,379 · AspectPatternCheck 1,359 · BodyCheck 1,128 · SnapshotCheck 763 · DignityCheck 583 · HarmonicCheck 454 · TopicCheck 389 · CompositeCheck 260 · PhaseCheck 167 · SectCheck 80 · FittingHarnessCheck 8.
`CalCheck` is a calibration report, not a pass/fail suite — it prints a step table and no ALL CLEAR line. Do not read its silence as a failure.

---

## What was done this session — Tri-Wheel

**The tri-wheel draws the current sky as a third ring around a synastry bi-wheel.**
Chart A = inner (silver spheres), Chart B = middle ring (gold cubes, unchanged), Sky = outer ring (blue cubes).

Enabled by: Synastry mode + tick "Include transits" in Chart Setup.  
The checkbox was previously greyed in synastry with the tooltip "no third wheel yet". It is now enabled with a descriptive tooltip. No new ChartMode was added.

**Files changed:**

| File | What changed |
|---|---|
| `gui/SkymapPanel.java` | `showTriWheel` flag; `cLon/cSpeed/cValid/cOk/triCusps/triAscendant` arrays; `triWheelShown()` helper; `triWheelRadii()`; `ringRadii()` extended to 6 elements with new `RING_TRI` index; 3-arg overload kept for backward compat; paint, hover and click all updated |
| `gui/ChartSetupPanel.java` | `syncTransitsCheck()` — enabled checkbox + new tooltip in SYNASTRY case |
| `gui/AspectGridCheck.java` | Part J updated: asserts 6-element array instead of 5; added tri-wheel geometry assertions (756 new checks) |
| `gui/InterpretationPanel.java` | aspect rows may carry an optional 4th element naming whose chart the other body is in; absent means "Natal" |

**No data files were touched. No shared files were rewritten.**

**Follow-up fix in the same session — the sky ring only aspected half the chart.**
The tri-wheel's body click handler built its aspect list from `bValid`/`bLon` alone, so clicking a sky glyph listed sky-to-Chart-A contacts and silently omitted every sky-to-Chart-B one. It now walks both. Two things fell out of that:

- **`getAspectType` was being handed a bare `false`.** Its javadoc forbids literals there — "ask `isSynastryPair` rather than passing a literal". Both passes now ask `isSynastryPair(false)`. **The value was already right and this is not a behaviour change**, but the reasoning is worth keeping: `chartMode` **is** `SYNASTRY` in a tri-wheel, so `isSynastryPair(true)` would answer yes and halve the orb. The halved orb is for one person's chart against another's. The sky is not a person — sky-to-either-chart is a transit and takes the full orb.
- **`InterpretationPanel` hardcoded `" to Natal "`** in every aspect heading. The tri-wheel is the first panel with two people in it, where "Natal" names neither. Rows now take an optional 4th element for the chart name; absent means "Natal", so every caller written before the tri-wheel renders exactly as it did. `aspectData[0]` stays the bare body name — it is the key `getTransitAspect` looks prose up by, and a decorated name misses.

**Architecture note — why `compositeTransitTime` is reused:**
That field already existed to hold "a third moment that is neither person A nor person B" for composite transits. The same slot serves the synastry tri-wheel sky. The javadoc on the field says exactly this. No new time fields were needed.

---

## Current state — Ourania Windows

Code: `OuraniaWindows/src/main/java`. Runs from there, **not** `out\`.
No JDK on PATH — use `C:\Program Files\Android\Android Studio\jbr\bin\javac.exe`.
Do **not** glob `astro\*.java` alone — compile both packages together with `-cp src\main\java`.

```
java -cp src\main\java com.zodiacomputing.ourania.gui.OuraniaWindow
```

- All ten layers L0–L9 are built.
- 23 selectable chart points from one registry (`astro/Bodies.java`).
- Interpretation prose complete for placements — 19/19 non-angle points at 12/12 signs and 12/12 houses; aspect grid 330/330 cells.
- Synastry, both composites, and now **tri-wheel** wired in.
- `DataCheck` is 82,479 of the total. That number is a build fingerprint — compare a suite to its own previous value, never to another suite.

---

## Open items

1. **Tri-wheel aspect lines** — the sky ring draws glyphs but no aspect lines to Chart A or B yet. `drawAspectLine` plumbing is in place; wiring `cLon` into the aspect-filter loop is the next step when David decides he wants them.
2. **Tri-wheel side-panel listing** — sky bodies are not yet listed in the planet-placements panel. Follows the same pattern as the existing transit block.
3. **Convergence weighting** — more voters stopped improving discrimination; needs weighting rather than more families. See WORK-PLAN.
4. **~~`CompositeCheck` Part D does not cover the tri-wheel~~ FIXED 2026-08-28.** The table now carries a third expectation column for `triWheelShown` alongside `outerWheelShown`, plus a row-wise assertion that a tri-wheel always implies an outer wheel (stated as a relationship between the two flags, not a third copy of the rule). The stale paragraph claiming the app "does not draw one yet" is rewritten. 260 → 280 checks.
5. **~~`showAngleAt` aspects Chart A only~~ FIXED 2026-08-28.** The earlier wording of this item was wrong and is corrected here, because the wrong version made the job look bigger than it was. It claimed the one-sidedness was "pre-existing and not tri-wheel-specific: it affects plain transit-mode angle clicks too." **It does not.** In every mode but one there is exactly one other chart and `bLon` is correctly it:
   - natal — angle against its own bodies;
   - **composite — against the composite's own bodies, because a composite mode loads the composite INTO the base arrays** (`updateChartData`: "in a composite mode the INNER wheel is the composite itself") rather than special-casing it through the drawing, the grid, the hit test and the panel in turn;
   - transit — transit angle against natal, which is the point of a transit;
   - synastry — chart B's angle against chart A.

   Only the tri-wheel has two other charts under one angle, and only there was chart B dropped. `showAngleAt` now takes a `fromSky` flag; when it is set and a tri-wheel is up, both passes run and rows are tagged. Every other caller still emits three-element rows and reads exactly as before.
6. ~~`computeMidpointComposite`'s equal-house fallback is written but never exercised by a test.~~ **FIXED 2026-08-28.** `CompositeCheck` Part F exercises it via `sw=null` and asserts 12 equal-house 30° cusps and `syntheticNote` documentation. 280 → 284 checks.
7. ~~**Cosmetic:** `ringRadii` dead ternary~~ **FIXED.** Collapsed `showTri ? outer-20 : outer-20` to a plain `int tri = outer - 20;` with an updated comment. AspectGridCheck Part J: ALL CLEAR — 6,429 checks, 0 failures.
8. **Synastry angle clicks are asymmetric, and nothing records the choice.** Clicking **chart A's** angle lists A-angle-to-A's-own-bodies — a natal reading. Clicking **chart B's** angle lists B-angle-to-chart-A's-bodies — a cross-chart reading. Same chart type, two meanings depending on which ring you click. Each is defensible alone; together they are inconsistent. **Left alone deliberately: this is a reading decision, not a code one.** David's call.
9. **`showAngleAt`'s chart-A pass still passes a literal `false` to `getAspectType`.** That method's javadoc forbids literals — "ask `isSynastryPair`". The new sky pass complies; the original does not. It is not a silent no-op to change: in SYNASTRY the honest answer is `true`, which **halves the orb**, so chart B's angle would gain and lose aspect rows. That changes what a user reads, so it needs David's say-so rather than a drive-by fix. Related to item 8 — settle both together.
10. ~~**Stale `.class` hazard** — two agents compiling into `src/main/java`.~~ **FIXED.** `build.ps1` compiles to `out/` (wiping it first), runs suites from `out/` only, and prints the `.class` source location at startup. Build fingerprint + mtime stamps in `build.ps1` output make stale-build runs visible. Verified end-to-end by Antigravity; `CompositeCheck` Part F mutation test confirmed running from `out/` by Bob/IBM.

## Assertions are mutation-tested now, and one of them was a lie

**Part P's four sky-click assertions were verified by breaking the code on purpose and checking the suite went red.** Two mutations were run:

| Mutation | Expected | First result |
|---|---|---|
| Empty the chart-B pass in `showAngleAt` | angle assertion fails | **caught** — exactly one failure, the right one |
| Empty the chart-B loop in `handleChartClick` | body assertion fails | **NOT caught — suite stayed green** |

The second one matters. The body assertion drove `triggerPlanetInterpretation`, which is the **placements-link** path. The fix it was supposed to be guarding lives in `handleChartClick`, the **wheel-click** path. Two different functions; covering one covers nothing of the other. The assertion looked right, passed, and guarded nothing.

`skyWheelClickTally` now drives the wheel path directly, computing glyph coordinates from `ringRadii` → `triWheelRadii` → pin — the same chain the painter uses, never a second copy. Re-running mutation two now fails correctly.

**The lesson worth keeping: a passing assertion is not evidence until you have seen it fail.** The suites are the only channel between two agents who never talk, and an assertion that cannot fail is worse than none, because it reads as coverage.

---

**Corrected 2026-08-28:** a previous revision of this file listed "Synastry has no check suite" as open item 4. **That was wrong.** `astro/SynastryCheck.java` exists, runs **41,206 checks**, and is the second-largest suite in the project. The vault HANDOVER has recorded it since 2026-08-18 ("Synastry has a suite now — the fourteenth"). Anyone acting on the old item would have written a duplicate. **When this file and the vault HANDOVER disagree, the vault is authoritative** — see CLAUDE.md.

**David's calls, not ours:** whether solar returns are read whole or only by angles; whether asteroid-to-asteroid aspect prose is worth writing; whether transit aspect lines on the tri-wheel are wanted.

---

## Who owns which file

| File | Holds | Owner |
|---|---|---|
| `extra_bodies.json` | body cores, aspects, transits | Antigravity |
| `quincunx_gap.json` | the 49 classical quincunx pairs | Antigravity |
| `body_placements.json` | sign + house prose | Claude |
| `Descendant.json`, `Ascendent.json`, `ic.json`, `mc.json` | angle prose | shared, guarded by `DataCheck` Part D |
| `interpretations.json` | the original 615 KB dataset | neither — read-mostly |

Add a **new file** rather than growing one of these, and register it in `InterpretationService.EXTRA_FILES`.
