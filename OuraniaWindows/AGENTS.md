# AGENTS.md — read this before changing anything here

_Agent-neutral. `CLAUDE.md` beside this file is the Claude Code equivalent and says the same
things; this one exists because a file named `CLAUDE.md` gives no other tool a reason to open it._

## 1. There is more than one of you

This project is worked by **two agents** — Claude Code and Google Antigravity — editing the same
tree, usually not at the same time, and never able to see each other's changes in flight. David
switches between them.

**The handover file is the channel:**

```
C:\Users\daver\Documents\BRAIN\Brain\HANDOVER.md
```

**Read it before you start. Update it and sign the stamp before you finish.** It carries live
state, an ownership table for the shared data files, and the genuinely open work. Full protocol:
`Resources/wiki/two-agents-one-repo.md` in that same vault.

## 2. Do not regenerate shared data files

**`GenerateExtraBodies.java` in this directory rewrites `src/main/resources/data/extra_bodies.json`
from scratch** with a truncating `FileWriter`. It emits `body_core`, `aspects` and `transits` and
nothing else. Running it has already destroyed data once: on 2026-08-13 it removed 192 sign and
house entries that another agent had written into that file. Nothing looked broken afterward — the
file parsed, the app ran, and the loss showed up only as "Interpretation not found".

**It will do it again, and it will now also destroy work that this project's own JS generators
added.** `add_missing_aspects.js`, `add_missing_ic_desc.js`, `add_missing_lilith_fortune.js` and
`generate_cross.js` all read-modify-write, and together they contributed roughly 928 of the
current 3,036 aspect entries. A re-run of the Java generator drops every one of them.

**Before running it:** back the file up, or better, change it to merge rather than overwrite. The
JS scripts are the pattern to copy — they load the existing JSON, add keys, and write it back.

## 3. Where prose lives

| File | Holds | Written by |
|---|---|---|
| `data/extra_bodies.json` | body cores, aspects, transits | Antigravity's generators |
| `data/body_placements.json` | sign + house prose | Claude |
| `data/interpretations.json` | the original 615 KB dataset | neither — read-mostly |

Add a **new file** rather than growing one of these, and register it in
`InterpretationService.EXTRA_FILES`. The loader takes a list precisely so that two agents can own
different files.

Format rules are strict and unforgiving — the parser is hand-rolled and line-oriented. One entry
per line; a literal double quote must be `&quot;` because the loader does not unescape; placement
values are `<b>summary</b><br><br>text`; aspect and transit values are
`<b>title</b><br><i>summary</i><br><br>text`. See `Resources/wiki/ourania-data-pipeline.md`.

## 4. Run the checks — they are the only thing that catches this

Twelve suites, currently **47,329 checks, 0 failures**. They are the sole communication channel
between two agents who never speak, and a check suite is the only reason the 2026-08-13 data loss
was ever found.

```bash
java -cp src\main\java com.zodiacomputing.ourania.gui.DataCheck
java -cp src\main\java com.zodiacomputing.ourania.astro.ZodiacSelfTest
```

The other ten are in `com.zodiacomputing.ourania.astro`: `BodyCheck`, `JoyCheck`, `TransitCheck`,
`SnapshotOutputCheck`, `DignityCheck`, `TopicCheck`, `SnapshotCheck`, `PhaseCheck`, `SectCheck`,
`FittingHarnessCheck`.

**If one goes red and you cannot fix it, write that in `HANDOVER.md` rather than fixing it blind.**
A described failure is more useful to the next agent than a guessed repair.

## 5. Build

No build file, no JDK on PATH. Runs from `src\main\java`, **not** `out\` — which is stale and is
not what executes.

```bash
"C:\Program Files\Android\Android Studio\jbr\bin\javac.exe" -encoding UTF-8 -cp src\main\java -d src\main\java <files>
java -cp src\main\java com.zodiacomputing.ourania.gui.OuraniaWindow
```

Compile both packages together. **Do not glob `astro\*.java` alone** — it fails with ~89 errors.
Source files are UTF-8 **without** a BOM and contain astrological glyphs, so `-encoding UTF-8` is
required and PowerShell's `Set-Content -Encoding utf8` will break them (it writes a BOM; use
`[System.IO.File]::WriteAllText` with `UTF8Encoding $false`).
