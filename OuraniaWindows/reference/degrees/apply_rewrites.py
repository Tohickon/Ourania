"""Splice the drafted degree readings into the app's degree file.

    python apply_rewrites.py          # report what would change, write nothing
    python apply_rewrites.py --write  # replace summary and fullText for each drafted degree

rewrites.json holds a draft for each degree whose reading in degree_interpretations.json was
the pasted tool answer (see README.md). Only those keys are touched, only their summary and
fullText, and the title and every other degree are left exactly as they are. The file is
written back in the layout it already has, so the diff is the readings and nothing else.
Splices; does not rebuild the file - an entry that is in the app but not in rewrites.json
cannot be lost by running this.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "..", "src", "main", "resources", "data",
                    "degree_interpretations.json")


def main():
    write = "--write" in sys.argv[1:]
    raw = open(DATA, "rb").read()
    bom = raw.startswith(b"\xef\xbb\xbf")
    text = raw.decode("utf-8-sig")
    newline = "\r\n" if "\r\n" in text else "\n"
    app = json.loads(text)
    same_layout = json.dumps(app, ensure_ascii=False, indent=2).replace("\n", newline)
    if same_layout.rstrip() != text.rstrip():
        sys.exit("degree_interpretations.json is not in the layout this script writes; "
                 "stopping so that nothing but the readings can change.")
    drafts = json.load(open(os.path.join(HERE, "rewrites.json"), encoding="utf-8"))
    unknown = [k for k in drafts if k not in app]
    if unknown:
        sys.exit("rewrites.json has degrees the app does not: " + ", ".join(unknown))
    changed = 0
    for key, draft in drafts.items():
        for field in ("summary", "fullText"):
            if app[key][field] != draft[field]:
                app[key][field] = draft[field]
                changed += 1
    out = json.dumps(app, ensure_ascii=False, indent=2).replace("\n", newline)
    out += text[len(text.rstrip()):]
    print(f"{len(drafts)} degrees drafted, {changed} fields differ from the app's file, "
          f"{len(app)} degrees in the file before and after.")
    if write:
        with open(DATA, "wb") as f:
            f.write((b"\xef\xbb\xbf" if bom else b"") + out.encode("utf-8"))
        print("written.")
    else:
        print("nothing written (pass --write).")


if __name__ == "__main__":
    main()
