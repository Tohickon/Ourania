# The degree readings' source

`360_degrees_interpretations.pasted.txt` is where `data/degree_interpretations.json` came from.
It sat in the app's data directory from 24 Sep (`1f49900`) to 10 Oct 2026 under the name
`360_degrees_interpretations.json`, opened by nothing, and was moved here so that it is kept
as evidence and no longer ships with the app. Its contents are unchanged.

## What it is

A pasted answer from a chat or notebook tool, in two batches, which is why it is not JSON: it
opens with a ```json fence and a second `{` begins at Cancer 21. 354 degrees; Aries 3, 4, 6, 15,
24 and 25 are missing.

- **Aries 1 to Virgo 19 (165 entries)** name their sources in every entry: Henson (148
  entries), Kozminsky (120), Muir (120), Charubel (119), Sepharial (99), Janduz (97), Weber (37),
  Matthews (7). These are degree-symbol authors. None is the Sabian set.
- **Virgo 20 to Pisces 30 (189 entries)** name nobody. They carry citation marks like `[18]`
  whose list was not pasted with them, so which author each sentence came from is lost. Five of
  them share wording with `Sabian_interpretations.json` (Sagittarius 15 and 18, Pisces 22, 25
  and 30), so some Sabian material is mixed into this half.

## How the app's file relates to it

Measured by `compare_degrees.py` in this directory (run it again after any edit to either file):

| The pasted entry | App entries | Relation |
|---|---|---|
| names its authors | 165 | **Rewritten.** None identical; the median app entry shares 5% of its three-word runs with the pasted one (most 18%) and 35% of its content words - the same images and traits, in the project's own sentences, with the authors' names removed. |
| names nobody | 189 | **Copied.** 181 are word for word the pasted text with the citation marks taken out; the other 8 share at least 78% of their three-word runs. |
| absent | 6 | No counterpart here. Where those six came from is not recorded. |

## What is not known

How close the pasted text is to the books it summarises. That needs the books. The copied half
is the part to read first, because there the app prints the tool's sentences unchanged, and
because the Sabian wording in it (Pisces 30's summary is the keynote line PROVENANCE.md item 2
is about) reaches the reader through the degree file as well as the Sabian one.

## The drafted replacements (10 Oct 2026)

`rewrites.json` holds a new summary and reading for each of the 189 copied degrees, drafted in
the voice of the half that was already rewritten. **They are drafts and are not in the app.**
David approved the voice on a sample of ten; the rest are waiting on his read.
`apply_rewrites.py` splices them into `data/degree_interpretations.json` and touches nothing
else (run it with no arguments first: it reports and writes nothing).

Measured on the drafts: the median reading shares 4% of its three-word runs with the pasted
text and the most any shares is 15% - the same range as the rewritten half (5%, 18%). None
shares a five-word run with the Sabian file. With the drafts applied on a trial basis,
`DataCheck`, `JsonCheck` and `DataFilesCheck` are clear and `CorpusCheck` measures the file
written at 0.000; the trial was then undone.

What a rewrite does not fix: each draft keeps the traits of the reading it replaces, so it is
only as true to the degree-symbol authors as the tool's summary was. And the ideas in some of
the later degrees are Sabian in origin even with the wording gone - Sagittarius 11 to 20 and
Pisces 21 to 30 most plainly (the groundhog, the Easter service, the harvest moon, the stone
face). If the degree file is meant to be free of Sabian material, those need other sources,
not other sentences.
