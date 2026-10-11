"""NotebookLM round trip for the composite readings: prompts out, pasted answers in.

    python notebook.py prompts     # write notebook_prompts/: paste-ready requests, small batches
    python notebook.py import      # read pasted/<set>/*.txt, write clean files into the draft folders

WHY THIS EXISTS. build.py reads a strict three-line format, and a notebook chat does not
return one: it adds bold, bullets, numbering, citation marks like [3] and curly quotes, and
it answers in a chat box, a screenful at a time. So the request is cut into batches a single
answer can hold, and what comes back is cleaned here before build.py ever sees it.

THE ROUND TRIP
  1. Add BRIEF.md to the notebook as a source (upload it, or paste its text).
  2. Open a file in notebook_prompts/. Copy ONE block between the ===== lines into the chat.
  3. Copy the whole answer and paste it at the end of a text file in pasted/composite/ or
     pasted/transit/. Any file name; many answers can go in one file.
  4. python notebook.py import      then      python build.py
     build.py says which readings fail and why; ask the notebook to redo just those.

WHAT THE IMPORT ACCEPTS. A heading naming the reading, in almost any dress - "=eris square
moon", "**Eris square Moon**", "3. Transit Saturn trine Sun:", "### Black Moon Lilith
opposition the Midheaven" - then the lead sentence, then the paragraph. If the lead and the
paragraph arrive as one block, the first sentence is taken as the lead. Citation marks, bold,
bullets and "Lead:"/"Body:" labels are removed; double quotes become apostrophes and dashes
become commas, because DataCheck forbids the first and the brief the second. Anything it
cannot place is listed, never guessed at.
"""
import collections
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)   # so this runs from any folder, and under python -I
import build  # noqa: E402
BATCH = {"composite": 10, "transit": 15}

NAMES = {
    "sun": "sun", "moon": "moon", "mercury": "mercury", "venus": "venus", "mars": "mars",
    "jupiter": "jupiter", "saturn": "saturn", "uranus": "uranus", "neptune": "neptune",
    "pluto": "pluto", "chiron": "chiron", "north node": "north_node", "north_node": "north_node",
    "south node": "south_node", "south_node": "south_node", "ascendant": "ascendant",
    "asc": "ascendant", "descendant": "descendant", "dsc": "descendant", "mc": "mc",
    "midheaven": "mc", "ic": "ic", "imum coeli": "ic", "ceres": "ceres", "pallas": "pallas",
    "juno": "juno", "vesta": "vesta", "lilith": "lilith", "black moon lilith": "lilith",
    "eris": "eris", "eros": "eros", "hygiea": "hygiea", "nessus": "nessus", "pholus": "pholus",
    "fortune": "fortune", "part of fortune": "fortune", "spirit": "spirit",
    "part of spirit": "spirit",
}
BODY = "|".join(sorted((re.escape(n) for n in NAMES), key=len, reverse=True))
HEADING = re.compile(
    r"^(?P<t>transit(?:ing)?\s+)?(?:the\s+)?(?P<a>" + BODY + r")\s+(?:in\s+)?(?P<asp>"
    + "|".join(build.ASPECTS) + r")\s+(?:to\s+)?(?:the\s+)?(?:composite\s+|natal\s+)?(?:the\s+)?(?P<b>"
    + BODY + r")$", re.I)


def clean(line):
    s = line.strip()
    s = re.sub(r"\s*\[\s*\d+(?:\s*[-,–]\s*\d+)*\s*\]", "", s)      # [3], [1, 2], [1-3]
    s = re.sub(r"[*_`]+", "", s)                                          # bold, italics, code
    s = re.sub(r"^\s*(?:[-•>]+|#+|\d+[.)])\s*", "", s)               # bullets, headings, numbers
    s = re.sub(r"^(?:lead|body|reading|heading|key)\s*[:.]\s*", "", s, flags=re.I)
    s = s.replace("’", "'").replace("‘", "'")
    s = re.sub("[\"“”]", "'", s)
    s = re.sub(r"\s*[—–]\s*", ", ", s)
    s = s.replace("&", "and").replace("<", "").replace(">", "")
    return re.sub(r"\s+", " ", s).strip()


def heading(line):
    s = clean(line).lstrip("=").strip().rstrip(":.").strip()
    m = HEADING.match(s)
    if not m:
        return None
    return bool(m.group("t")), NAMES[m.group("a").lower()], m.group("asp").lower(), NAMES[m.group("b").lower()]


def do_import():
    for name, cfg in build.SETS.items():
        where = build.app_keys(cfg)
        already = build.read_drafts(cfg)
        got = collections.OrderedDict()
        unplaced = []
        files = sorted(glob.glob(os.path.join(HERE, "pasted", name, "*.txt")))
        for path in files:
            key = None
            buf = []

            def flush():
                if key is None:
                    return
                text = [t for t in buf if t]
                if not text:
                    unplaced.append(f"{key}: a heading with nothing under it")
                    return
                lead, rest = text[0], " ".join(text[1:])
                if not rest:
                    parts = re.split(r"(?<=[.!?])\s+", lead, maxsplit=1)
                    if len(parts) == 2:
                        lead, rest = parts
                if not rest:
                    unplaced.append(f"{key}: only one sentence")
                    return
                got[key] = (lead, rest)

            for raw in open(path, encoding="utf-8").read().split("\n"):
                h = heading(raw)
                if h:
                    flush()
                    buf = []
                    is_transit, a, asp, b = h
                    k = cfg["prefix"] + f"{a}_{asp}_{b}"
                    if k not in where:
                        unplaced.append(f"not a reading in the {name} set: {raw.strip()[:60]}")
                        key = None
                    else:
                        key = k
                elif key is not None:
                    buf.append(clean(raw))
            flush()
        fresh = collections.OrderedDict((k, v) for k, v in got.items() if k not in already)
        by_body = collections.OrderedDict()
        for k, v in fresh.items():
            by_body.setdefault(build.split_key(k)[1], []).append((k, v))
        folder = os.path.join(HERE, cfg["drafts"])
        os.makedirs(folder, exist_ok=True)
        for a, items in by_body.items():
            with open(os.path.join(folder, f"nb_{a}.txt"), "a", encoding="utf-8") as fh:
                for k, (lead, body) in items:
                    _, x, asp, b = build.split_key(k)
                    head = "transit " if cfg["prefix"] else ""
                    fh.write(f"={head}{x} {asp} {b}\n{lead}\n{body}\n\n")
        print(f"{name}: {len(files)} pasted file(s), {len(got)} readings recognised, "
              f"{len(fresh)} new ones written to {cfg['drafts']}/, "
              f"{len(got) - len(fresh)} already drafted and left alone.")
        for u in unplaced[:30]:
            print("  could not place - " + u)


def do_prompts():
    for name, cfg in build.SETS.items():
        where = build.app_keys(cfg)
        d = build.read_drafts(cfg)
        folder = os.path.join(HERE, "notebook_prompts", name)
        os.makedirs(folder, exist_ok=True)
        for old in glob.glob(os.path.join(folder, "*.txt")):
            os.remove(old)
        by_body = collections.OrderedDict()
        for k in where:
            if k in d:
                continue
            _, a, asp, b = build.split_key(k)
            by_body.setdefault(a, collections.OrderedDict()).setdefault(b, []).append(asp)
        total = 0
        for n, (a, seconds) in enumerate(by_body.items(), 1):
            batches, cur = [], []
            for b, aspects in seconds.items():
                aspects.sort(key=build.ASPECTS.index)
                group = [f"{'transit ' if cfg['prefix'] else ''}{a} {asp} {b}" for asp in aspects]
                if cur and len(cur) + len(group) > BATCH[name]:
                    batches.append(cur)
                    cur = []
                cur += group
            if cur:
                batches.append(cur)
            out = [f"{name}: {a} - {sum(len(x) for x in batches)} readings in {len(batches)} prompts.",
                   "Copy ONE block (the lines between two ===== rows) into the notebook chat.", ""]
            for i, batch in enumerate(batches, 1):
                kind = ("a period of time: a moving body in the sky acting on a point in the "
                        "relationship's chart" if cfg["prefix"]
                        else "a permanent feature of the relationship's own chart")
                out += [f"===== {a} {i} of {len(batches)} =====",
                        f"Using the source titled Ourania writing brief, write the {len(batch)} readings "
                        f"listed below. Each describes {kind}.",
                        "For each reading give exactly three parts, in this order, as plain text:",
                        "1. the heading, copied exactly as I list it, on a line by itself",
                        "2. one lead sentence on its own line that states the whole dynamic plainly",
                        "3. one paragraph of three to five sentences",
                        "Rules: 65 to 90 words per reading. No bullet points, no bold, no quotation "
                        "marks, no dashes, no citations. Readings that share the same two bodies must "
                        "use completely different sentences from one another: do not repeat a "
                        "definition or a closing line. Use the meanings of the bodies and of each "
                        "aspect given in the brief. Write nothing before the first heading or after "
                        "the last paragraph.",
                        ""] + batch + [""]
            out.append("=====")
            with open(os.path.join(folder, f"{n:02d}_{a}.txt"), "w", encoding="utf-8") as fh:
                fh.write("\n".join(out) + "\n")
            total += len(batches)
        print(f"{name}: {total} prompts in {len(by_body)} files under notebook_prompts/{name}/")


if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else ""
    if what == "prompts":
        do_prompts()
    elif what == "import":
        do_import()
    else:
        print(__doc__)
