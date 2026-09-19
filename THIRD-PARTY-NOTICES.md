# Third-party notices

Recorded 2026-09-02. This file exists because the vendored Swiss Ephemeris source
refers to "the file named LICENSE" for its copyright terms, and no such file was
present in this repository. Preserving these notices is required under **both**
of the licence options below, so this file is needed regardless of which is chosen.

---

## Swiss Ephemeris (Java port)

**Vendored at:** `OuraniaWindows/src/main/java/de/thmac/swisseph/` — 49 `.java` files,
compiled into the application, not a managed dependency.

| | |
|---|---|
| Upstream | Swiss Ephemeris **Free Edition, version 2.00.00** |
| Copyright | © 1997–2008 Astrodienst AG, Switzerland. All rights reserved. |
| Authors | Dieter Koch and Alois Treindl (Astrodienst, Zurich) |
| Java port | Thomas Mack, 23 April 2001 |
| Licence | **Dual**: GNU GPL v2-or-later, **or** Swiss Ephemeris Professional Licence |

Questions about the *port* go to Thomas Mack, not Astrodienst; Astrodienst is not
involved in the port.

### The choice, verbatim from the source header

> The choice must be made before the software developer distributes software
> containing parts of Swiss Ephemeris to others, and before any public
> service using the developed software is activated.

**Option (a) — GNU GPL v2 or later.** The header states the GPL route "includes the
obligation to place his or her whole software project under the GNU GPL or a
compatible license." Ourania's own source would have to be released under GPL-compatible
terms. This costs nothing and forecloses a closed-source commercial release.

**Option (b) — Swiss Ephemeris Professional Licence.** Purchased from Astrodienst
with a signed contract, per <http://www.astro.com/swisseph/>. This permits a
closed-source product. It has a price.

### Obligations that apply either way

- Copyright notices and the licence notice must be preserved on all copies.
  *Verified 2026-09-02: the per-file headers are intact in all 49 vendored files.*
- The names of the authors and of Astrodienst **must not** be used to promote any
  software, product or service that uses Swiss Ephemeris. The source states its own
  header is the only place those names may legally appear.
- The trademarks *Swiss Ephemeris* and *Swiss Ephemeris inside* **may** be used
  for promotion.
- Distributed with no warranty of any kind.

### Ephemeris data files

No `.se1` data files are tracked in this repository (verified: 0 tracked). The
application therefore runs on the built-in Moshier approximation unless a data
directory is supplied locally. Bundling `.se1` files in any future distribution
raises a **separate** licensing question from the source code above, and is not
covered by this notice.

---

## OpenPDF

Added 2026-09-18, with the PDF report (master list B3). The project's first library that is a
dependency rather than vendored source.

**Bundled at:** `OuraniaWindows/lib/openpdf.jar` — the unmodified binary jar, on the classpath of
`build.ps1`, `Run_Ourania.bat` and the in-place compile.

| | |
|---|---|
| Upstream | OpenPDF **1.3.30** (`com.github.librepdf:openpdf`), a fork of iText 4 |
| Licence | **Dual**: GNU LGPL 2.1, **or** Mozilla Public Licence 2.0 — as declared in the jar's own `Bundle-License` manifest entry |
| Integrity | SHA-1 `349778353ca669596888d4bff55fb05595c5a497`, identical to the checksum Maven Central publishes for this version (verified 2026-09-16) |

The jar carries its own licence texts under `META-INF/` (`LICENSES.md`, `misc_licenses.txt`,
and `APACHE-LICENSE-2.0.txt` for components it includes). What the licences require of a
distribution is part of the same undecided question as the section above: nothing is distributed
yet, and this records the facts rather than settling anything.

---

## Status

**UNDECIDED — and not yet urgent, because nothing has been distributed.**

As of 2026-09-02 the application is not distributed: the GitHub repository is
private, there is no jar, no installer, and no public service. The licence choice
becomes binding at the moment any of those changes.

Decision owner: David Sauerwald. Nothing here is legal advice; the terms above are
quoted from the source header so the choice can be made on the actual wording.
