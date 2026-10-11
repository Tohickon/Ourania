"""Composite aspect readings (C5): drafts in, checked, and spliced into the app's two files.

    python build.py            # check every draft, report, write nothing
    python build.py --write    # splice the drafts into the data files

A draft file in drafts/ is plain text, three lines a reading and a blank line between:

    =juno square venus
    The lead sentence, shown in bold.
    The rest of the reading, one paragraph.

Checks, because these replace text a template assembled and must not become a template
themselves: within a pair no two aspects share half their wording (the measure is
CorpusCheck's - twice the longest common word sequence over the two lengths - and CorpusCheck
only compares square with trine, where this compares all ten combinations); every reading is
50 to 120 words; no sentence of six words or more appears in three readings.
Splices: only keys that have a draft are touched, and a key the app does not have is refused.
"""
import glob, json, os, re, sys, collections, html

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "..", "src", "main", "resources", "data")
FILES = ["composite_aspects_expanded.json", "composite_asteroid_pairs.json"]
ASPECTS = ["conjunction", "sextile", "square", "trine", "opposition"]


def drafts():
    out = collections.OrderedDict()
    for path in sorted(glob.glob(os.path.join(HERE, "drafts", "*.txt"))):
        block = []
        for line in open(path, encoding="utf-8").read().split("\n") + [""]:
            if line.strip():
                block.append(line.strip())
                continue
            if not block:
                continue
            if len(block) != 3 or not block[0].startswith("="):
                sys.exit(f"{os.path.basename(path)}: not key, lead, body: {block[:1]}")
            a, asp, b = block[0][1:].split()
            key = f"{a}_{asp}_{b}"
            if key in out:
                sys.exit(f"drafted twice: {key}")
            out[key] = (block[1], block[2])
            block = []
    return out


def words(s):
    return re.findall(r"[a-z']+", re.sub(r"<[^>]+>", " ", s).lower())


def similarity(x, y):
    a, b = words(x), words(y)
    prev = [0] * (len(b) + 1)
    for i in range(1, len(a) + 1):
        cur = [0] * (len(b) + 1)
        for j in range(1, len(b) + 1):
            cur[j] = prev[j - 1] + 1 if a[i - 1] == b[j - 1] else max(prev[j], cur[j - 1])
        prev = cur
    return 2.0 * prev[len(b)] / max(1, len(a) + len(b))


def as_html(lead, body):
    return "<b>" + html.escape(lead, quote=False) + "</b><br><br>" + html.escape(body, quote=False)


def main():
    write = "--write" in sys.argv[1:]
    d = drafts()
    docs = {f: json.loads(open(os.path.join(DATA, f), encoding="utf-8-sig").read()) for f in FILES}
    where = {}
    for f in FILES:
        for k in docs[f]["composite_aspect"]:
            where[k] = f
    problems = []
    for k in d:
        if k not in where:
            problems.append(f"no such key in the app: {k}")
    pairs = collections.OrderedDict()
    for k, (lead, body) in d.items():
        m = re.match(r"(.+)_(" + "|".join(ASPECTS) + r")_(.+)$", k)
        pairs.setdefault((m.group(1), m.group(3)), {})[m.group(2)] = lead + " " + body
        n = len(words(lead + " " + body))
        if not 50 <= n <= 120:
            problems.append(f"{k}: {n} words")
        # A double quote is legal JSON once escaped, but DataCheck forbids one inside a value:
        # the app's first reader of these files went line by line and a quote ended the line.
        for bad in ("\u2014", "\u2013", "the native", "  ", chr(34), "<", ">", "&"):
            if bad in lead + body:
                problems.append(f"{k}: contains {bad!r}")
        if not lead.endswith(".") or not body.rstrip().endswith((".", "?")):
            problems.append(f"{k}: lead or body does not end a sentence")
    worst = 0.0
    for (a, b), got in pairs.items():
        missing = [x for x in ASPECTS if x not in got]
        if missing:
            problems.append(f"{a}/{b}: no draft for {', '.join(missing)}")
        names = list(got)
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                s = similarity(got[names[i]], got[names[j]])
                worst = max(worst, s)
                if s >= 0.5:
                    problems.append(f"{a}/{b}: {names[i]} and {names[j]} share {s:.0%}")
    seen = collections.Counter()
    for k, (lead, body) in d.items():
        for s in set(x.strip() for x in re.split(r"(?<=[.!?])\s+", lead + " " + body)):
            if len(s.split()) >= 6:
                seen[s] += 1
    for s, n in seen.items():
        if n >= 3:
            problems.append(f"a sentence used in {n} readings: {s[:70]}")
    total = sum(len(v["composite_aspect"]) for v in docs.values())
    print(f"{len(d)} readings drafted for {len(pairs)} pairs, of {total} in the two files; "
          f"closest two aspects of one pair share {worst:.0%}.")
    if problems:
        print(f"{len(problems)} problem(s):")
        for p in problems[:40]:
            print("  " + p)
        sys.exit(1)
    if not write:
        print("all checks pass; nothing written (pass --write).")
        return
    # The two files are laid out one reading to a line, which json.dumps does not reproduce,
    # so the splice is textual: a drafted key's line is replaced and every other byte is left.
    for f in FILES:
        path = os.path.join(DATA, f)
        raw = open(path, "rb").read()
        text = raw.decode("utf-8-sig")
        nl = "\r\n" if "\r\n" in text else "\n"
        lines = text.split(nl)
        changed = 0
        for n, line in enumerate(lines):
            m = re.match(r'^"([a-z_]+)": (".*")(,?)$', line)
            if not m or m.group(1) not in d or where[m.group(1)] != f:
                continue
            fresh = json.dumps(as_html(*d[m.group(1)]), ensure_ascii=False)
            if fresh != m.group(2):
                lines[n] = f'"{m.group(1)}": {fresh}{m.group(3)}'
                changed += 1
        if changed:
            out = nl.join(lines)
            json.loads(out)   # still one valid document, or nothing is written
            with open(path, "wb") as fh:
                fh.write((b"\xef\xbb\xbf" if raw.startswith(b"\xef\xbb\xbf") else b"") + out.encode("utf-8"))
        print(f"{f}: {changed} readings replaced")


if __name__ == "__main__":
    main()
