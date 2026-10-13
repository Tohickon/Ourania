# Ourania — three-day work order for Antigravity

**Written** 2026-10-13 · **For** 13–16 Oct 2026 · **Master at** `a9db702c`
**Author of this note** Claude Opus 5, out of budget until ~16 Oct.

Read `CLAUDE.md` in full before the first edit. It is not boilerplate; every rule in it
was written after the defect it prevents had already happened once. The five that matter
most for these three days are restated inline below.

---

## 0. Ground rules — read before touching anything

**The build.** From `OuraniaWindows`:

```powershell
.\build.ps1 -All -Out C:\Temp\ourania-ag-01
```

It auto-discovers suites by filename (`*Check.java` plus `ZodiacSelfTest.java`). Master
currently has **115 suites**. The only JDK on this machine is
`C:\Program Files\Android\Android Studio\jbr\bin`.

**The app does not run from `out\`.** It runs from `src/main/java` with the `.class` files
sitting beside the sources. An edit without a rebuild changes nothing on screen. Launch via
`Run_Ourania_Current.bat` at the repo root — it prints branch, commit, uncommitted count and
whether any source is newer than the compiled classes, which is exactly the trap that cost an
hour on 10 Oct.

**Compare failure SETS, not counts.** Several suites' totals drift with the wall clock
(see task 2). A red suite is yours only if its *named failures* differ from the same suite's
named failures on the previous commit. `known-red.txt` and `known-red-ci.txt` list the reds
that are already explained; measure against them, never against a remembered number.

**Never rewrite a shared data file — splice into it.** New bulk content goes in its own file,
added to `InterpretationService.EXTRA_FILES`. Load order matters:
`interpretations.json` loads first with plain `put` and is authoritative; everything in
`EXTRA_FILES` uses `putIfAbsent`, so earlier files win. Any new file also needs a row in
`provenance.tsv` or `CorpusCheck` goes red by name.

**Commit only your own files, by name.** `git add -A` is forbidden on this repo. There are
untracked files in the tree right now (`TestPdf.java`, `test.pdf`, `Run_Ourania_Current.bat`,
a modified `error_log.txt`) that are not yours and must stay uncommitted unless a task below
says otherwise.

**Writing Java from PowerShell.** `Set-Content -Encoding utf8` emits a BOM that `javac`
rejects. Use `[System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding $false))`.
And javac **argfiles treat `\` as an escape** — `C:\Users\daver\...` silently collapses to
`C:Usersdaver...`. Use forward slashes in argfiles. (Task 4 is to write this second one down.)

**Attribution.** End every commit message with:

```
Co-Authored-By: Antigravity <noreply@google.com>
```

Do not sign commits as Claude.

---

## 1. Merge `glyph-sizes` — the only unmerged branch

Head `3ae2db80`, branched from `2072a566`, which is an older base than everything already in
master. It touches `SkymapPanel.java`, which master has since changed twice (the transport fix
`b5ab815b` and the `chart-tabs` merge `47ae5309`). **The merge gets harder every day it waits.**

Its last green run was on **10 Oct, before `semo_18.se1` was installed**. That file changes the
Moon's position and the North Node by about 24 arcseconds, so every chart-maths suite now
computes slightly different numbers. Its green run is stale evidence, not wrong evidence — and
stale is what you do not merge on.

**Do, in this order:**

1. `git checkout glyph-sizes`, then `git merge master`. Resolve conflicts in `SkymapPanel.java`
   **by reading both sides**, not by taking one wholesale. The project's most common defect is
   one rule implemented in two places and the copies drifting apart — a `--theirs` resolution
   manufactures exactly that.
2. Regress. Record the full failure set.
3. Regress **master alone**, freshly built, in a separate output directory. Record its failure set.
4. Merge to master only if the two sets differ by nothing that names a `glyph-sizes` file.
5. Push.

**Do not** `git checkout --theirs` or `--ours` on `SkymapPanel.java`. **Do not** force-push.
**Do not** delete the branch after merging — leave it for David to delete.

---

## 2. `NavigationCheck` is clock-dependent — master's CI fails intermittently until this is fixed

This is the highest-value fix in the list, because it is currently making CI untrustworthy for
everything else.

**The evidence** (commit `60f94b2a`): one unchanged tree, the same compiled classes, **10 of 822
failures and then 12 of 815 ninety minutes apart.** Totals have been observed at 815, 816, 817,
821, 822 and 823.

**The cause:** `chartPageLinksShowTheirPage` casts a scratch chart **for the current moment**, and
whether that chart happens to contain a pattern varies — roughly four times in ten. So the number
of navigable pages, and therefore the number of assertions, depends on the time of day the suite
runs.

**The fix:** the test already has an injected-banner path that does not depend on a live chart.
Use it **always**, instead of only as a fallback. Then re-measure the count and update the
`NavigationCheck` entry in `known-red.txt` — which currently says 10 with a long CLOCK-DEPENDENT
note attached. Replace that note with the real, stable number.

**Do not** pin the whole scratch chart to a fixed date. That was considered and rejected as
over-engineering: it changes what a dozen other assertions are measuring in order to fix one.

---

## 3. Name the chart pages from the saved chart at startup

**The report, verbatim:** *"i dont see David or Erika for chart names"*.

**What is happening.** `nameTheChartPages()` (`OuraniaWindow.java:886`, called at line 1072) runs on
every chart update and asks `whoseChart(...)` for each page's label. `whoseChart` returns a name
only when `who.moment != null` — i.e. only once a chart has actually been **cast**, not merely when
the setup form has been pre-filled from saved data. Until then the pages fall back to "Placements"
and "Chart B".

**The gate itself is correct and must stay.** An unentered `ChartSubject` still carries the
placeholder label "Chart A"; naming a tab from it would print a name that is not a name, and the
reader would see "Chart A" twice over — once as the tab and once as its own page heading.

**What is wrong is the moment it is applied.** On a cold open the app already holds David's birth
data in the form (`ChartSetupPanel` reads `savedNatalDate` around line 414) and still shows a
generic page name, which reads as the feature being broken.

**Do:** find the saved-chart load path, and when it produces a subject with a real (non-placeholder,
non-empty) label, call `nameTheChartPages()` then — before Generate is pressed. The page should read
"David" the moment the app opens.

**Do not** relax the `moment != null` test inside `whoseChart` to achieve this. That would put
"Chart A" on the tab in the genuinely-unentered case, which is the bug the gate exists to prevent.
The change belongs at the call site, not in the predicate.

**Verify by running the app**, not only by the suites. `NavigationCheck` will want attention if the
set of named pages changes.

---

## 4. Write the argfile rule into `CLAUDE.md`

One paragraph, next to the existing BOM warning: **javac argfiles treat `\` as an escape
character**, so a Windows path written into one collapses (`C:\Users\daver\Projects` →
`C:UsersdaverProjects`) and the error you get back names a file that was never asked for. Use
forward slashes inside argfiles.

This cost two compile failures on 12 Oct. It is the one rule from that day that never got
recorded.

---

## 5. Refresh the master checklist artifact

It is **stale by eleven days** — it stops at 2 Oct, and none of the five branches merged since
appear in it. Regenerate it from commit subjects on master.

**Do not** generate it from the vault's session notes. The public changelog and checklist are
built from **commit subjects only**. (Reason under "Hard prohibitions" below.)

---

## 6. Three small open questions, if time remains

In priority order. Each is an investigation whose output is a written finding, not necessarily a fix.

- **`SynastryCheck` reports 112 failures and has never been investigated.** Find out whether it is
  one systematic cause or 112 separate ones. One sentence of conclusion is worth more than a
  partial fix.
- **`CalCheck` reports "pass / no verdict"** — it finishes without asserting anything. Establish
  what it was meant to assert. A suite that cannot fail is worse than no suite, because it reports
  confidence it has not earned.
- **`WheelRing.Kind.isPerson`'s javadoc is broader than its implementation.** Decide which is
  right and bring the other into line.

---

## Hard prohibitions

These are not preferences. Each one has a specific reason.

1. **Do not touch `C:\Users\daver\Projects\BRAIN\Brain` (the vault) at all.** It is a separate
   private repo. It holds birth data — dates, times and birthplaces — for David and **three other
   named living people**. Do not make it public, do not add collaborators, do not copy any of its
   contents into `Tohickon/Ourania`, **which is a PUBLIC repository.** If a task seems to need
   something from the vault, stop and leave a note instead.

2. **Do not generate `CHANGELOG.md` or any public document from the vault's session notes.**
   Commit subjects only. Same reason as (1): the notes quote birth data.

3. **Do not change the hardcoded test birth data** that appears in 25 files of the app repo. It
   has been reviewed and the **decision not to act on it is already made.** It is David's call and
   it is not open.

4. **Do not extend the `SA Body = A/B` catalogue**, and do not write any new astrological
   interpretation prose. Thirteen dial points make 78 midpoints and 1,014 pictures; the four written
   are the four David sourced to Ebertin, Tyl and Brady. Everything beyond them would be invention,
   and a plausible invented paragraph is indistinguishable from a sourced one once it is on screen.
   The composition architecture exists precisely so that nothing has to be invented.

5. **Do not decide Layer 7's two open questions.** They are David's calls, not build tasks:
   whether to keep ranking or filter at three witnesses, and whether to re-key `Convergence` from
   natal point to life domain.

6. **Do not install `PleasePrompto/notebooklm-mcp`.** It was archived 2026-09-10, drives Chrome
   through Patchright to evade Google's bot detection, and caches Google credentials unencrypted.
   It was deliberately not installed. Ask before reconsidering.

7. **No history rewriting.** No force-push, no rebase of a pushed branch, no amending a pushed
   commit, no branch deletion.

8. **Do not change a `known-red` count you have not measured on this machine.** CI lacks the four
   numbered-asteroid files (`ast0`, `ast7`, `ast136`) that this machine has and the runner cannot
   download, so a green run here predicts nothing about there. Setting a number by guess puts
   master's CI at stake: too low fails the run, too high hides the next defect.

9. **Do not write an assertion that cannot fail.** This repo has produced four of them —
   `!lacksMoon || ...`, `templatedServed <= 42` (which passes on 0 as happily as on 42), an
   exactness check that recovered a longitude as `(midpoint − arc)` and so measured `0.000000` by
   construction, and a presence-only check on a fusion reading that never compared the text.
   Each one protected a false belief for a while. Before accepting a new green assertion, mutate
   the thing it guards and confirm it goes red.

---

## What to return

Write it to `ANTIGRAVITY-REPORT-2026-10-16.md` at the repo root. Leave it **uncommitted** —
David decides whether it lands in a public repo.

For **each** of tasks 1–6:

| field | what it must say |
| :--- | :--- |
| status | done / partial / not attempted / **declined, and why** |
| commits | the hashes, with their subjects |
| regression before | suite count, and the **named** failure set |
| regression after | suite count, and the **named** failure set |
| what changed in the diff of those two sets | the actual answer to "did I break anything" |
| files touched | by name |

Plus, at the top:

- **Anything you decided not to do, and the reason.** This is the most valuable part of the
  report. A declined task with a stated reason is a result; a silently skipped one is a loss.
- **Any rule in `CLAUDE.md` you found to be wrong or out of date.** Say so rather than working
  around it.
- **Anything you changed that was not on this list**, with its justification.
- **The state of each branch** at the end: `master`, `glyph-sizes`, and anything you created.
- **Whether the app launches and the three-tab rail ("Chart", "Placements", "Transits") appears**,
  confirmed by running it, not by reading the code.

If you get stuck on a task, **stop and write the finding** rather than working around it. A
precise account of why task 2 resisted is worth more to the next session than a workaround that
changes what the suite measures.
