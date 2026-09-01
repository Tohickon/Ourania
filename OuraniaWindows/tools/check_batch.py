#!/usr/bin/env python3
"""Pre-flight check for a supplied interpretation batch.

    python tools/check_batch.py "path/to/whatever_they_sent.json"

Run this BEFORE handing a file over. Every defect found in the eleven batches
integrated on 2026-08-31 was mechanical and would have been caught here in about
a second: citation markers left mid-sentence, a SQL escape doubling every quote,
body ids the app never queries, and prose that opened with the same twelve words
ten thousand times.

Exit code is 0 when the file is usable, 1 when something needs fixing, so this
can gate a script.

<b>The registry ids are read out of Bodies.java, never copied.</b> A hardcoded
list here would be a second definition of the thing that has been wrong in five
separate files, and it would drift the first time a body is added.
"""

import json
import os
import re
import sys
import collections

HERE = os.path.dirname(os.path.abspath(__file__))
BODIES = os.path.join(HERE, "..", "src", "main", "java", "com",
                      "zodiacomputing", "ourania", "astro", "Bodies.java")
CORPUS = os.path.join(HERE, "..", "src", "main", "resources", "data",
                      "interpretations.json")

SIGNS = ["aries", "taurus", "gemini", "cancer", "leo", "virgo",
         "libra", "scorpio", "sagittarius", "capricorn", "aquarius", "pisces"]


def registry():
    """{id: DisplayName} straight out of the registry source."""
    src = open(BODIES, encoding="utf-8", errors="replace").read()
    pairs = re.findall(r'\w+\(\s*"([a-z_]+)",\s*"([^"]+)"', src)
    return dict(pairs)


def real_symbols():
    """The 360 Sabian symbols the app actually serves, keyed sign_degree."""
    try:
        d = json.load(open(CORPUS, encoding="utf-8", errors="replace"))
        return d.get("sabian", {})
    except Exception:
        return {}


def words(t):
    return {w for w in re.findall(r"[a-z]{5,}", (t or "").lower())}


def load(path):
    """A supplied batch, however it happens to be shaped."""
    raw = open(path, encoding="utf-8", errors="replace").read()
    if path.lower().endswith(".sql"):
        rows = re.findall(r"VALUES\s*\((.*?)\);\s*$", raw, re.M | re.S)
        return [{"_sql": r} for r in rows], "sql"
    try:
        d = json.loads(raw)
        if isinstance(d, list):
            return d, "json array"
        if isinstance(d, dict):
            # already in loader shape: {"section": {key: value}}
            out = []
            for section, entries in d.items():
                if isinstance(entries, dict):
                    for k, v in entries.items():
                        out.append({"key": k, "interpretation": v})
            return out, "loader-shaped object"
    except ValueError:
        pass
    return [json.loads(l) for l in raw.splitlines() if l.strip()], "jsonl"


def text_of(rec):
    for f in ("interpretation", "text", "description", "prose", "_sql"):
        if f in rec and isinstance(rec[f], str):
            return rec[f]
    return ""


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    path = sys.argv[1]
    if not os.path.exists(path):
        print("no such file: " + path)
        return 1

    rows, shape = load(path)
    ids = registry()
    names = {v.lower(): k for k, v in ids.items()}
    problems = []
    notes = []

    print("file    : %s" % os.path.basename(path))
    print("shape   : %s" % shape)
    print("entries : %d" % len(rows))
    if not rows:
        print("\nFAIL: nothing parsed out of this file.")
        return 1

    texts = [text_of(r) for r in rows]
    if not any(texts):
        print("\nFAIL: no interpretation text found. Fields seen: %s"
              % sorted({k for r in rows for k in r}))
        return 1

    # ---- mechanical defects, each one seen for real on 2026-08-31 ----
    cites = sum(1 for t in texts if "[cite" in t)
    if cites:
        problems.append("%d entries carry [cite: N] markers - the generator was "
                        "reaching for sources instead of writing" % cites)

    sql_quotes = sum(1 for t in texts if "''" in t)
    if sql_quotes:
        problems.append("%d entries have doubled single quotes ('') - a SQL escape "
                        "leaked into the JSON; they should be one quote" % sql_quotes)

    doubled = sum(1 for t in texts if re.search(r"[a-z]\.\.\s", t))
    if doubled:
        problems.append("%d entries have a doubled full stop mid-text - a "
                        "concatenation seam" % doubled)

    markdown = sum(1 for t in texts if "**" in t)
    if markdown:
        notes.append("%d entries use markdown ** for bold; it becomes <b> on import "
                     "(not a defect, just so you know)" % markdown)

    unbalanced = sum(1 for t in texts if t.count("**") % 2)
    if unbalanced:
        problems.append("%d entries have an odd number of ** markers - unbalanced "
                        "bold will import wrong" % unbalanced)

    # ---- the one that loses data silently ----
    bad_ids = collections.Counter()
    for r in rows:
        for field in ("body", "body_1", "body_2", "transit_body", "target_body"):
            v = r.get(field)
            if not isinstance(v, str):
                continue
            if v in ids:
                continue                      # already a registry id
            if v.lower() in names:
                continue                      # a display name, which bodyKey resolves
            bad_ids[v] += 1
    if bad_ids:
        lines = []
        for v, n in bad_ids.most_common(8):
            # snake(display) -> id is exactly the mistake five files made:
            # "Black Moon Lilith" snaked to black_moon_lilith where the id is lilith.
            snake = {d.lower().replace(" ", "_"): r for r, d in ids.items()}
            snake["midheaven"] = "mc"
            snake["imum_coeli"] = "ic"
            rid = snake.get(v.lower().replace(" ", "_"))
            guess = "  (the registry calls it '%s')" % rid if rid else "  (not a chart point at all?)"
            lines.append("      %-22s %4d entries%s" % (v, n, guess))
        problems.append("body names the app will never ask for, in %d entries:\n%s"
                        % (sum(bad_ids.values()), "\n".join(lines)))

    # ---- completeness, when the batch is per-sign ----
    degs = [r.get("degree") for r in rows if isinstance(r.get("degree"), int)]
    if degs:
        if len({r.get("sign") for r in rows if r.get("sign")}) <= 1:
            missing = [n for n in range(1, 31) if n not in degs]
            if missing:
                problems.append("degrees missing from this sign: %s" % missing)
        signs_here = {r.get("sign") for r in rows if r.get("sign")}
        if len(signs_here) <= 1:
            dupes = [d for d, n in collections.Counter(degs).items() if n > 1]
            if dupes:
                problems.append("degrees supplied more than once: %s" % sorted(dupes))

    keys = [r.get("key") for r in rows if r.get("key")]
    if keys:
        dupes = [k for k, n in collections.Counter(keys).items() if n > 1]
        if dupes:
            problems.append("duplicate keys: %s" % dupes[:5])

    # ---- <b>invented symbols: the defect that reads perfectly and is entirely wrong.</b>
    # A file arrived on 2026-08-31 carrying thirty symbol names repeated round all twelve
    # signs, so Cancer 1 and Capricorn 1 were both "The First Spark of Manifestation" and
    # 330 of 360 entries interpreted a degree that does not exist. Nothing in the prose
    # gives it away - it is fluent, confident and about the wrong image. The only thing
    # that catches it is comparing against the 360 symbols already on file.
    truth = real_symbols()
    if truth and any(r.get("sign") and isinstance(r.get("degree"), int) for r in rows):
        invented, checked, sample = 0, 0, None
        claimed_names = {}
        for r, t in zip(rows, texts):
            sign, deg = r.get("sign"), r.get("degree")
            if not sign or not isinstance(deg, int):
                continue
            plain = t.replace("&quot;", "'").replace("''", "'")
            lead = re.search(r"(?:represented as|image for [A-Za-z]+ \d+[^']*|degree of|"
                             r"symbol for [A-Za-z]+ \d+[^']*)[^']*'([^']{6,90})'", plain)
            if lead:
                claimed = lead.group(1).strip()
            else:
                quotes = re.findall(r"'([^']{6,90})'", plain)
                if not quotes:
                    continue
                claimed = max(quotes, key=len).strip()
            checked += 1
            claimed_names.setdefault(deg, set()).add(claimed.lower())
            actual = truth.get("%s_%d" % (str(sign).lower(), deg), "")
            if not actual:
                continue
            if not (words(claimed) & words(actual)):
                invented += 1
                if sample is None:
                    sample = (sign, deg, claimed, actual.strip().strip('"'))
        if checked:
            reused = sum(1 for deg, names_ in claimed_names.items() if len(names_) == 1)
            if invented:
                msg = ("%d of %d entries name a symbol that is NOT the Sabian symbol "
                       "for that degree." % (invented, checked)
                       + chr(10) + "      %s %d claims  '%s'" % (sample[0], sample[1], sample[2])
                       + chr(10) + "      the real symbol is  '%s'" % sample[3])
                signs_seen = {str(r.get("sign")).lower() for r in rows if r.get("sign")}
                if len(signs_seen) >= 6 and reused == len(claimed_names):
                    msg += (chr(10) + "      Every degree carries ONE name across all "
                            "signs - the generator invented %d names and pasted them "
                            "round the zodiac." % len(claimed_names))
                problems.append(msg)
            else:
                notes.append("symbols verified against the corpus: %d of %d name the right "
                             "degree" % (checked - invented, checked))

    # ---- <b>coverage without discrimination.</b> Measured 2026-08-31: across two composite
    # batches, a SQUARE and a TRINE of the same pair shared two thirds of their text, whole
    # sentences appearing verbatim in both. Those aspects mean close to opposite things - a
    # square is a fault line the couple works with, a trine is the comfort they hide behind -
    # so wording that is 66% identical is not saying either. The natal corpus, as a control,
    # shares 0% and 0 of 409 pairs exceed half, so this is a solvable problem and not the
    # nature of the material.
    pairs = collections.defaultdict(dict)
    for r, t in zip(rows, texts):
        b1, b2, asp = r.get("body_1"), r.get("body_2"), r.get("aspect")
        if b1 and b2 and asp:
            pairs[tuple(sorted((str(b1), str(b2))))][str(asp).lower()] = t

    def sentences(t):
        t = re.sub(r"<[^>]+>", " ", t or "").replace("! ", ". ").replace("? ", ". ")
        return [x.strip() for x in t.split(". ") if len(x.strip()) > 25]

    scores, worst = [], None
    for pair, m in pairs.items():
        asps = sorted(m)
        for i in range(len(asps)):
            for j in range(i + 1, len(asps)):
                a, b = sentences(m[asps[i]]), sentences(m[asps[j]])
                if not a or not b:
                    continue
                shared = sum(len(x) for x in a if x in b)
                total = sum(len(x) for x in a)
                if not total:
                    continue
                ratio = shared / float(total)
                scores.append(ratio)
                if worst is None or ratio > worst[0]:
                    worst = (ratio, pair, asps[i], asps[j],
                             next((x for x in a if x in b), ""))
    if scores:
        scores.sort()
        med = scores[len(scores) // 2]
        over = sum(1 for x in scores if x > 0.5)
        print("overlap : %d same-pair aspect comparisons; median %.0f%% of one reading "
              "appears verbatim in the other" % (len(scores), 100 * med))
        if med > 0.5:
            problems.append("different aspects of the SAME pair share %.0f%% of their text "
                            "(median over %d comparisons, %d above half). A square and a "
                            "trine of one pair should not read alike - they mean close to "
                            "opposite things.%s"
                            % (100 * med, len(scores), over,
                               (chr(10) + "      shared verbatim by " + worst[2] + " and "
                                + worst[3] + " of " + "/".join(worst[1]) + ":"
                                + chr(10) + "      \"" + worst[4][:150] + "...\"")
                               if worst else ""))
        elif med > 0.25:
            notes.append("%.0f%% median overlap between aspects of the same pair. This "
                         "counts every aspect pairing, so neighbours like septile and novile "
                         "drag it up honestly; the figure to worry about is square against "
                         "trine, and above 50%% is where that goes wrong." % (100 * med))

    # ---- the quality measure this project actually uses ----
    openings = collections.Counter(" ".join(t.split()[:10]) for t in texts if t)
    top, n = openings.most_common(1)[0]
    pct = 100.0 * n / len(texts)
    print("openings: %d distinct; commonest is %.1f%% of the batch" % (len(openings), pct))
    print("          \"%s...\"" % top[:88])
    lens = sorted(len(t) for t in texts)
    print("length  : %d / %d / %d  (min / median / max)"
          % (lens[0], lens[len(lens) // 2], lens[-1]))

    # <b>Calibrated against batches that were actually accepted, not guessed.</b> The
    # per-sign files rotate four openings, so ~25% share the commonest one and they read
    # fine. The batch that was genuinely unusable sat at 100%. A threshold of 25 would
    # have rejected Cancer, Taurus and Gemini, all of which shipped.
    if pct > 50.0:
        problems.append("%.0f%% of entries open with the same ten words. One template "
                        "with the slots filled in - the batch rejected on 2026-08-31 "
                        "was at 100%%, its usable replacement at 1.7%%." % pct)
    elif pct > 30.0:
        notes.append("%.1f%% share an opening. Rotating templates read acceptably around "
                     "25%%; above 30%% is worth a second look." % pct)

    print()
    for s in notes:
        print("note : " + s)
    if problems:
        print()
        for s in problems:
            print("FIX  : " + s)
        print("\n%d problem(s). Worth regenerating before sending this on." % len(problems))
        return 1
    print("OK - nothing mechanical wrong with this batch.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
