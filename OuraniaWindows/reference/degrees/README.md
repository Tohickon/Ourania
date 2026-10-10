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

## How the app's file related to it, before 10 Oct

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
the voice of the half that was already rewritten. **They are in the app since 10 Oct 2026**: David approved the voice on a sample of ten, then
the whole set ("your rewrites are good so use them"), and they were spliced in the same day.
`apply_rewrites.py` is what spliced them into `data/degree_interpretations.json`; it touches
nothing else, and run again it reports 0 fields differing.

Measured on the drafts: the median reading shares 4% of its three-word runs with the pasted
text and the most any shares is 15% - the same range as the rewritten half (5%, 18%). None
shares a five-word run with the Sabian file. With the drafts applied on a trial basis,
`DataCheck`, `JsonCheck` and `DataFilesCheck` are clear and `CorpusCheck` measures the file
written at 0.000; the same holds now that they are applied.

What a rewrite does not fix: each draft keeps the traits of the reading it replaces, so it is
only as true to the degree-symbol authors as the tool's summary was. And the ideas in some of
the later degrees are Sabian in origin even with the wording gone - Sagittarius 11 to 20 and
Pisces 21 to 30 most plainly (the groundhog, the Easter service, the harvest moon, the stone
face). If the degree file is meant to be free of Sabian material, those need other sources,
not other sentences.

## The second source (10 Oct 2026, later)

David supplied other material for the twenty Sabian-in-idea degrees and for Aries:
`second_source.pasted.txt`, kept here as he attached it. It is again a chat or notebook
tool's answer (it ends by offering more help) and, as first sent, named no author; its vocabulary - the
south node of Uranus, the Midas touch, the forty days in the wilderness - is not the Sabian
set's. See the last point below for the source David then gave.

`rewrites_second_source.json` holds 26 readings written from it and spliced in the same day:
Sagittarius 11-20 and Pisces 21-30, replacing the redrafts above (those twenty keys were taken
out of `rewrites.json` so that running the script again cannot put them back; they are in git
at `5cbd92a`), and Aries 4, 6, 15, 24 and 25.

- **The five Aries readings they replace were filler.** Each reasoned from where the degree
  sits in the sign ("the middle of Aries stabilises the sign's fire") and described no image
  or tradition. That is what "no counterpart in the pasted file" turned out to mean.
- **Aries 3 followed later the same day**, sent by David on its own, and was filler until then.
  Aries 5 already had a reading from the first source and was left alone.
- **The source has a name now.** David's Aries 3 entry ends `Degrees_Of_The_Zodiac_Esther V
  Leinbach`, which reads as the notebook's source file: Esther V. Leinbach, *Degrees of the
  Zodiac*. That is stated for Aries 3 and assumed for the other 25, which came from the same
  notebook; David to confirm. Whether the book is in copyright has not been checked.
- **These 25 are shorter** - 41 to 60 words where the rest run 57 to 89 - because the source
  gives a sentence or two per degree and nothing was added to pad them.
- Measured: at most 10% of a reading's three-word runs are in the new pasted text, and none
  shares a five-word run with the Sabian file.
