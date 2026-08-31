# CLAUDE.md — Ourania Windows (pointer)

**This is a pointer, not the project doc.** The real documentation lives in the Obsidian
vault and does not auto-load when a session starts in this directory. Read it first.

Vault root: `C:\Users\daver\Documents\BRAIN\Brain`

| Read this | Path in vault |
|---|---|
| **Start here — live state, and who touched what last** | `HANDOVER.md` (vault root) |
| **The project doc** | `Projects/astrology-app/CLAUDE.md` |
| **The open work** | `Projects/astrology-app/Process/WORK-PLAN.md` |
| Build and run gotchas | `Resources/wiki/ourania-build-and-run.md` |
| Layer specs L0–L9 | `Projects/astrology-app/Process/` |
| Session logs | `Resources/sessions/2026-08-*-ourania-*.md` |

The `obsidian-vault` MCP server reads these directly (`vault_read`), and it is the better
path — the notes carry links and backlinks that the filesystem does not. If that server
isn't connected, the paths above are plain files on disk.

## You are not the only agent here

This project is also worked by **Google Antigravity**, editing the same tree and the same vault,
with no visibility of your changes or you of its. **Read `HANDOVER.md` first and update it last.**

Three rules, in full at `Resources/wiki/two-agents-one-repo.md`:

1. **Never rewrite a shared data file — splice into it**, or put new bulk content in its own file
   and add it to `InterpretationService.EXTRA_FILES`. A wholesale rewrite of `extra_bodies.json`
   on 2026-08-13 deleted 192 entries belonging to the other agent, silently.
2. **Run the check suites after touching anything shared** — twelve of them, 47,329 checks. They
   are the only channel between two agents who never talk. A check suite caught that deletion;
   nothing else would have.
3. **Read `Resources/sessions/` before logging anything as open or unbuilt.** It may already be
   done.

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
java -cp src\main\java com.zodiacomputing.ourania.gui.OuraniaWindow
```

- The app runs from `src\main\java`, **not** `out\`. The `out\` directory is stale and is
  not what executes.
- No JDK on PATH. The only one is `C:\Program Files\Android\Android Studio\jbr\bin\javac.exe`.
- **Do not glob** `astro\*.java` alone — it fails with ~89 errors. Compile both packages
  together with `-cp src\main\java`.
- Writing source files with PowerShell's `Set-Content -Encoding utf8` emits a BOM and
  `javac` rejects it. Use `[System.IO.File]::WriteAllText` with `UTF8Encoding $false`.
