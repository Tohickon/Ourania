"""Vary the subject of closing sentences that all start 'The pair' or 'The couple'.

    python vary_subjects.py drafts/nb_pallas.txt

The notebook, told not to close on 'You ...', closed 26 of 60 Pallas readings on 'The pair'
or 'The couple' instead. This keeps one such closing in five and gives the others a second
person subject, which means taking the s off the verb: 'The pair alternates between' becomes
'The two of you alternate between'. It changes no other word, and build.py still decides
whether the result passes. Anything it cannot turn safely it leaves alone and prints.
"""
import re
import sys

SUBJECTS = [None, "The two of you", "Both of you", "You", "Together you"]
HEAD = re.compile(r"^The (?:pair|couple) ((?:\w+ly )?)(\w+)(.*)$")


def plural(verb):
    if verb.endswith("ies"):
        return verb[:-3] + "y"
    if re.search(r"(ss|sh|ch|x)es$", verb):
        return verb[:-2]
    return verb[:-1] if verb.endswith("s") and not verb.endswith("ss") else None


def main(path):
    lines = open(path, encoding="utf-8").read().split("\n")
    turn = changed = 0
    for i, line in enumerate(lines):
        if not line.strip() or line.startswith("=") or (i and lines[i - 1].startswith("=")):
            continue                      # only bodies: not headings, not leads
        sents = re.split(r"(?<=[.!?])\s+", line.strip())
        m = HEAD.match(sents[-1])
        if not m:
            continue
        subject = SUBJECTS[turn % len(SUBJECTS)]
        turn += 1
        if subject is None:
            continue
        verb = plural(m.group(2))
        if verb is None:
            print("left alone: " + sents[-1])
            continue
        rest = re.sub(r" and (\w+)s ", lambda x: " and " + (plural(x.group(1) + "s") or x.group(1) + "s") + " ",
                      m.group(3), count=1) if re.search(r" and (stays|keeps|earns|enjoys) ", m.group(3)) else m.group(3)
        sents[-1] = f"{subject} {m.group(1)}{verb}{rest}"
        lines[i] = " ".join(sents)
        changed += 1
    open(path, "w", encoding="utf-8").write("\n".join(lines))
    print(f"{changed} closing sentences given a different subject")


if __name__ == "__main__":
    main(sys.argv[1])
