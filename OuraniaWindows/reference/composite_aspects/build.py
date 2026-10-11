"""Composite readings (C5): drafts in, checked, and spliced into the app's data files.

    python build.py                    # check every draft in both sets, write nothing
    python build.py --write            # splice the drafts that pass into the data files
    python build.py --todo             # (re)write todo/: one skeleton per body, undrafted keys only
    python build.py composite          # limit any of the above to one set: composite | transit

TWO SETS, one rule each for where the text lives:

    composite   drafts/*.txt           composite_aspect in 2 files         1,300 readings
    transit     transit_drafts/*.txt   composite_transit_aspect in 7 files 8,841 readings

A draft file is plain text. Three lines a reading, a blank line between readings:

    =juno square venus
    The lead sentence, shown in bold.
    The rest of the reading, one paragraph.

    =transit saturn square sun
    The lead sentence.
    The rest.

The first line is the key with spaces for underscores between its three parts; a body or
point whose own name has an underscore keeps it (north_node, south_node). See BRIEF.md for
what to write. Nothing here writes prose.

CHECKS, because these replace text a template assembled and must not become one:
  - within a pair, no two aspects share half their wording. The measure is CorpusCheck's -
    twice the longest common word sequence over the two lengths - and where CorpusCheck
    compares only square with trine, this compares every aspect drafted for the pair;
  - every reading is 50 to 120 words;
  - no sentence of six words or more appears in three readings of a set;
  - no double quote, angle bracket or ampersand (DataCheck forbids a quote inside a value),
    no dash used as punctuation, and each part ends its sentence;
  - a key the app does not have is refused.
A pair is only spliced complete: --write refuses while any aspect of a drafted pair is missing.

THE SPLICE is textual. These files are laid out one reading to a line, which json.dumps does
not reproduce, so a drafted key's line is replaced and every other byte is left alone.
"""
import collections
import glob
import html
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "..", "src", "main", "resources", "data")

SETS = {
    "composite": {
        "section": "composite_aspect",
        "files": ["composite_aspects_expanded.json", "composite_asteroid_pairs.json"],
        "drafts": "drafts",
        "prefix": "",
    },
    "transit": {
        "section": "composite_transit_aspect",
        "files": [
            "composite_transit_angles.json",
            "composite_transit_everything_else.json",
            "composite_transit_minor_aspects_asteroids_angles.json",
            "composite_transit_ptolemaic_asteroids_angles_classical.json",
            "composite_transits_new_aspects.json",
            "composite_transits_new_aspects_b.json",
            "composite_transits_ptolemaic_t.json",
        ],
        "drafts": "transit_drafts",
        "prefix": "transit_",
    },
}

ASPECTS = ["conjunction", "sextile", "square", "trine", "opposition", "quincunx",
           "semisextile", "semisquare", "sesquiquadrate", "quintile", "biquintile",
           "sesquiquintile", "septile", "novile", "decile"]
KEY = re.compile(r"^(transit_)?(.+)_(" + "|".join(ASPECTS) + r")_(.+)$")


def split_key(key):
    """(prefix, first body, aspect, second body) for an app key."""
    m = KEY.match(key)
    if not m:
        sys.exit(f"a key this script cannot read: {key}")
    return (m.group(1) or ""), m.group(2), m.group(3), m.group(4)


def app_keys(cfg):
    """key -> file, in file order, for one set."""
    where = collections.OrderedDict()
    for f in cfg["files"]:
        doc = json.loads(open(os.path.join(DATA, f), encoding="utf-8-sig").read())
        for k in doc[cfg["section"]]:
            where[k] = f
    return where


def read_drafts(cfg):
    out = collections.OrderedDict()
    for path in sorted(glob.glob(os.path.join(HERE, cfg["drafts"], "*.txt"))):
        block = []
        for line in open(path, encoding="utf-8").read().split("\n") + [""]:
            if line.startswith("#"):
                continue   # a note to the writer, not part of any reading
            if line.strip():
                block.append(line.strip())
                continue
            if not block:
                continue
            name = os.path.basename(path)
            if len(block) != 3 or not block[0].startswith("="):
                sys.exit(f"{name}: a reading is a key line, a lead and a body: {block[:1]}")
            parts = block[0][1:].split()
            if parts and parts[0] == "transit":
                parts = parts[1:]
            if len(parts) != 3:
                sys.exit(f"{name}: key line is not 'body aspect body': {block[0]}")
            key = cfg["prefix"] + "_".join(parts)
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


def check(name, cfg, d, where):
    problems = []
    pairs = collections.OrderedDict()
    expected = collections.defaultdict(set)
    for k in where:
        _, a, asp, b = split_key(k)
        expected[(a, b)].add(asp)
    for k, (lead, body) in d.items():
        if k not in where:
            problems.append(f"no such key in the app: {k}")
            continue
        _, a, asp, b = split_key(k)
        pairs.setdefault((a, b), {})[asp] = lead + " " + body
        n = len(words(lead + " " + body))
        if not 50 <= n <= 120:
            problems.append(f"{k}: {n} words")
        for bad in ("—", "–", "the native", "  ", chr(34), "<", ">", "&"):
            if bad in lead + body:
                problems.append(f"{k}: contains {bad!r}")
        if not lead.endswith(".") or not body.rstrip().endswith((".", "?")):
            problems.append(f"{k}: lead or body does not end a sentence")
    worst = 0.0
    for (a, b), got in pairs.items():
        missing = sorted(expected[(a, b)] - set(got))
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
    print(f"{name}: {len(d)} of {len(where)} readings drafted ({len(pairs)} pairs); "
          f"closest two aspects of one pair share {worst:.0%}.")
    if problems:
        print(f"  {len(problems)} problem(s):")
        for p in problems[:40]:
            print("    " + p)
    return problems


def splice(cfg, d, where):
    for f in cfg["files"]:
        path = os.path.join(DATA, f)
        raw = open(path, "rb").read()
        text = raw.decode("utf-8-sig")
        nl = "\r\n" if "\r\n" in text else "\n"
        lines = text.split(nl)
        changed = 0
        for n, line in enumerate(lines):
            m = re.match(r'^(\s*)"([a-z_]+)": (".*")(,?)\s*$', line)
            if not m or m.group(2) not in d or where.get(m.group(2)) != f:
                continue
            fresh = json.dumps(as_html(*d[m.group(2)]), ensure_ascii=False)
            if fresh != m.group(3):
                lines[n] = f'{m.group(1)}"{m.group(2)}": {fresh}{m.group(4)}'
                changed += 1
        if changed:
            out = nl.join(lines)
            json.loads(out)   # still one valid document, or nothing is written
            with open(path, "wb") as fh:
                fh.write((b"\xef\xbb\xbf" if raw.startswith(b"\xef\xbb\xbf") else b"")
                         + out.encode("utf-8"))
        print(f"  {f}: {changed} readings replaced")


def todo(name, cfg, d, where):
    """One skeleton file per first body: every key still to write, in the draft format."""
    folder = os.path.join(HERE, "todo", name)
    os.makedirs(folder, exist_ok=True)
    for old in glob.glob(os.path.join(folder, "*.txt")):
        os.remove(old)
    by_body = collections.OrderedDict()
    for k in where:
        if k in d:
            continue
        _, a, asp, b = split_key(k)
        by_body.setdefault(a, collections.OrderedDict()).setdefault(b, []).append(asp)
    total = 0
    for n, (a, seconds) in enumerate(by_body.items(), 1):
        lines = []
        count = 0
        for b, aspects in seconds.items():
            aspects.sort(key=ASPECTS.index)
            lines.append(f"# {a} with {b}: {len(aspects)} readings, one per aspect. "
                         f"Each must read differently from the others in this group.")
            for asp in aspects:
                head = "transit " if cfg["prefix"] else ""
                lines += [f"={head}{a} {asp} {b}", "LEAD", "BODY", ""]
                count += 1
        with open(os.path.join(folder, f"{n:02d}_{a}.txt"), "w", encoding="utf-8") as fh:
            fh.write(f"# {name}: {a} - {count} readings to write. Replace LEAD and BODY; "
                     f"keep the = lines exactly. Lines starting # are notes and are ignored.\n\n")
            fh.write("\n".join(lines))
        total += count
    print(f"{name}: {total} readings still to write, in {len(by_body)} files under todo/{name}/")


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    flags = [a for a in sys.argv[1:] if a.startswith("--")]
    names = [a for a in args if a in SETS] or list(SETS)
    failed = False
    for name in names:
        cfg = SETS[name]
        where = app_keys(cfg)
        d = read_drafts(cfg)
        if "--todo" in flags:
            todo(name, cfg, d, where)
            continue
        problems = check(name, cfg, d, where)
        if problems:
            failed = True
            continue
        if "--write" in flags:
            splice(cfg, d, where)
    if failed:
        sys.exit(1)
    if "--write" not in flags and "--todo" not in flags:
        print("nothing written (pass --write to splice, --todo to list what is left).")


if __name__ == "__main__":
    main()
