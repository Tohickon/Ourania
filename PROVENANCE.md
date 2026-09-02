# Corpus provenance

Recorded 2026-09-02. The interpretation corpus is this project's most valuable and
least documented asset: **52 JSON files, 71 MB, 600,000+ entries validated by
`DataCheck`** — and, before this file existed, not one recorded statement about
where any of it came from.

This is a record of what is *established*, what is *inferred*, and what is
*unknown*. It deliberately keeps those three apart.

---

## What is established by measurement

| Finding | Evidence |
|---|---|
| **No file carries provenance metadata** | 0 of 52 files contain a `source`, `author`, `licence`, `attribution` or `citation` field. The one apparent hit was the word "source" inside a `keywords` array. |
| **The corpus did not come from the Android app** | The decoded APK (`com.zodiacomputing.ourania` 1.7.5) contains **no JSON assets at all**. The corpus originated in this Windows project. |
| **It entered in bulk, then grew** | First appearance is commit `7a227621` (2026-08-30), message "Ourania". Twelve commits touch the corpus, the latest `7ef8cbfc` (2026-09-01). |
| **Git cannot attribute authorship** | All 12 commits carry the single author "David Sauerwald". That identity is shared by both AI agents working this tree, so git records the committer, not the writer. |
| **Sabian file scale** | 361 symbol entries in `Sabian_interpretations.json`. |

## What is inferred, and how confidently

- **Most prose appears to be written in-project.** The house voice is distinctive and
  consistent ("A relationship can survive a difficult composite Sun, but it cannot
  survive not knowing what it is for"). Session records describe prose being composed
  in bulk by both agents. *Confidence: high for the bulk of the corpus, but it is an
  inference from style, not a record.*

- **The corpus was written by two AI agents.** If so, authorship and rights sit with
  the operator under current terms of service, which is the simple case. *This should
  be confirmed rather than assumed, because the exceptions below are real.*

## What is unknown — and where the actual risk sits

**1. The Sabian symbol texts are not original to this project.**
The `"symbol"` field carries the canonical phrasings — Aries 1 reads *"A woman has
risen out of the ocean, a seal is embracing her."* These are the Jones/Wheeler
symbols first published in **1925**, which places them in the US public domain
(anything published before 1930 is PD as of 2026). **That specific field is
probably safe.**

**2. The `"summary"` keynote lines are the real question.**
They read in the manner of Dane Rudhyar's keynotes from *An Astrological Mandala*
(1973), which is **firmly in copyright**. Aries 1's summary is "Emergence of new
forms and leaving the past behind to enter a new arena of existence." Whether these
were independently composed, paraphrased, or adapted is not recorded anywhere.
**This is the single highest-risk item in the corpus and it needs a human read of a
sample against the 1973 text.**

**3. Attribution was deliberately stripped from some material.**
The vault records a "Sabian attribution scrub" in which names were removed. Removing
attribution from sourced material is the opposite of establishing provenance: it
destroys the only evidence of where the text came from. Any file touched by that
scrub cannot now be cleared from the repository alone.

**4. `relationship_prose.json` is described in project notes as "sourced, 900+
entries".** The word *sourced* implies an external origin that is not named. This
needs the same treatment as the Sabian summaries.

---

## Why this matters, concretely

This gate is quieter than the Swiss Ephemeris licence (see `THIRD-PARTY-NOTICES.md`)
and harder to fix late. Licence terms for code can be read off a header; corpus
provenance, once unrecorded, can only be reconstructed by reading the text. At
600,000+ entries that reconstruction is not feasible — which is precisely why the
sampling has to happen while the corpus is still the size it is, and why every file
added from here on should record its origin at the time of writing.

**Nothing is distributed yet.** The repository is private, there is no build artifact
and no public service, so no rights have been exercised against a third party. That
is the window in which this is cheap to settle.

## What would close this

1. Read a sample of ~20 Sabian `"summary"` lines against Rudhyar's 1973 keynotes.
   Rewrite any that are adaptations rather than independent composition.
2. Establish the origin of `relationship_prose.json`'s "sourced" entries.
3. Record the origin of anything the attribution scrub touched, or rewrite it.
4. Add a `_meta` block carrying origin and date to every new corpus file, so this
   file never has to be written retrospectively again.

Decision owner: David Sauerwald. Nothing here is legal advice.
