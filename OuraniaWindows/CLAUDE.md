# CLAUDE.md — Ourania Windows (pointer)

**This is a pointer, not the project doc.** The real documentation lives in the Obsidian
vault and does not auto-load when a session starts in this directory. Read it first.

Vault root: `C:\Users\daver\Documents\BRAIN\Brain`

| Read this | Path in vault |
|---|---|
| **Start here — where the app stands, and what changed last** | `HANDOVER.md` (vault root), its first section |
| **Progress, item by item** | the master checklist: https://claude.ai/code/artifact/eebb7efe-e625-4f24-a8d4-8c51d71986d4 |
| **The project doc** | `Projects/astrology-app/CLAUDE.md` |
| **The open work** | `Projects/astrology-app/Process/WORK-PLAN.md` |
| Build and run gotchas | `Resources/wiki/ourania-build-and-run.md` |
| Layer specs L0–L9 | `Projects/astrology-app/Process/` |
| Session notes, one per change | `Resources/sessions/*-ourania-handover-*.md` |
| How sessions work alongside each other | `Resources/wiki/two-agents-one-repo.md` |

The `obsidian-vault` MCP server reads these directly (`vault_read`), and it is the better
path — the notes carry links and backlinks that the filesystem does not. If that server
isn't connected, the paths above are plain files on disk.

## Other sessions work here too

David sometimes runs more than one Claude session, and no session can see what another did except
through git, the vault's `HANDOVER.md` and the notes in `Resources/sessions/`. (Google Antigravity
worked this tree until 2026-09-16 and was retired on 2026-09-19.) So:

- **Read `HANDOVER.md` first and update it last.** Its first section, *Where the app stands*, is
  kept current for exactly this reader. Give every change its own session note.
- **Uncommitted changes you did not make are someone's work in progress.** Look before building
  on them, and commit only your own files, by name.
- **Never rewrite a shared data file — splice into it**, or put new bulk content in its own file
  and add it to `InterpretationService.EXTRA_FILES`.
- **Run the check suites after touching anything shared, and measure a red suite against the
  previous commit before believing it is yours.** Since J14 (2026-09-19) every suite starts from
  a fresh install, so a red suite is red on any machine - but some totals still drift with the
  clock, so compare failure sets, not counts. `known-red.txt` lists the reds already explained.

The rules, and the incidents that earned each one, are in `Resources/wiki/two-agents-one-repo.md`.

## Why the split

The vault is for planning and tracking; this tree is the source of truth for code. It
stays out of the vault because it is ~24,500 files and 338 MB — mostly decompiled `.smali`
and Java sources, and exactly 2 markdown files — and because `javac` writes `.class` files
alongside the sources here, which would churn the vault's file watcher on every compile.

## Do not duplicate vault content into this file

Keep this a pointer. The project's most common defect is one rule implemented in two
places and the copies drifting apart — the work plan has a whole section on it. A second
copy of the build instructions or the layer status here would be exactly that defect.

## The minimum to not break something

Summarized from `ourania-build-and-run.md`, which is authoritative if these disagree:

```bash
java -cp "src\main\java;lib\*" com.zodiacomputing.ourania.gui.OuraniaWindow
```

- The app runs from `src\main\java`, **not** `out\`. The `out\` directory is stale and is
  not what executes.
- **`.\build.ps1` is the build** and finds a JDK 21+ by itself (`JAVA_HOME`, PATH, then Android
  Studio's). `-All` runs every suite against `known-red.txt`; `-Jar` and `-Package` make the
  app. CI runs `-All` on every push (`.github/workflows/ci.yml`).
- No JDK on PATH on David's machine. The only one is `C:\Program Files\Android\Android Studio\jbr\bin\javac.exe`,
  and it has no `jpackage`.
- **Do not glob** `astro\*.java` alone — it fails with ~89 errors. Compile both packages
  together with `-cp "src\main\java;lib\*"`.
- **`lib\*` is on every classpath** since the PDF report brought `lib\openpdf.jar`
  (2026-09-18). A `javac` without it fails on `ChartExporter`; a `java` without it runs,
  but the PDF export can only say the library is missing.
- Writing source files with PowerShell's `Set-Content -Encoding utf8` emits a BOM and
  `javac` rejects it. Use `[System.IO.File]::WriteAllText` with `UTF8Encoding $false`.
