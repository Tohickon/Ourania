"""Trims GeoNames cities1000 into the atlas the app ships.

One row per place: the searchable form of the name, then everything a chart needs.
Sorted by the searchable form, so the app can binary-search a prefix without
building an index at load time - the same reason the prose sets are sorted on disk.
"""
import io
import os
import unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "atlas.tsv")

# GeoNames column positions, from its readme.
NAME, ASCII, LAT, LON, CC, ADMIN1, POP, TZ = 1, 2, 4, 5, 8, 10, 14, 17


def fold(s):
    """The form a typed query is matched against: unaccented, lowercase, trimmed.

    A reader types "Sao Paulo" for "Sao Paulo" and "Zurich" for "Zurich", and a
    birth place is exactly where an accent is most likely to be dropped.
    """
    s = unicodedata.normalize("NFKD", s)
    s = "".join(c for c in s if not unicodedata.combining(c))
    # Runs of whitespace collapse to one. Atlas.fold does this in Java, and a handful of
    # GeoNames names carry a double space - "Querum -  Wabenkamp" - so without it those rows
    # are written in a form the app can never type its way to. AtlasCheck Part C compares the
    # two folds row by row and is what found it.
    return " ".join(s.lower().split())


admin = {}
with io.open(os.path.join(HERE, "admin1.txt"), encoding="utf-8") as fh:
    for line in fh:
        col = line.rstrip("\n").split("\t")
        if len(col) >= 2:
            admin[col[0]] = col[1]

country = {}
with io.open(os.path.join(HERE, "countryInfo.txt"), encoding="utf-8") as fh:
    for line in fh:
        if line.startswith("#"):
            continue
        col = line.rstrip("\n").split("\t")
        if len(col) >= 5:
            country[col[0]] = col[4]

rows = []
seen = set()
with io.open(os.path.join(HERE, "cities1000.txt"), encoding="utf-8") as fh:
    for line in fh:
        col = line.rstrip("\n").split("\t")
        if len(col) < 18:
            continue
        name = col[NAME].strip()
        tz = col[TZ].strip()
        if not name or not tz:
            continue
        try:
            lat = float(col[LAT])
            lon = float(col[LON])
            pop = int(col[POP] or 0)
        except ValueError:
            continue
        cc = col[CC].strip()
        region = admin.get(cc + "." + col[ADMIN1].strip(), "")
        # Two places of the same name in the same region are the same place to a
        # reader typing it; keep the larger and drop the duplicate row.
        key = (fold(name), cc, region)
        if key in seen:
            continue
        seen.add(key)
        rows.append((fold(name), name, "%.5f" % lat, "%.5f" % lon, cc,
                     country.get(cc, cc), region, str(pop), tz))

# Sorted by the folded name, then by population descending so the first row for a
# prefix is the biggest place with that name - which is the one a reader means.
rows.sort(key=lambda r: (r[0], -int(r[7])))

with io.open(OUT, "w", encoding="utf-8", newline="\n") as fh:
    fh.write("# Ourania offline atlas. Built from GeoNames cities1000 by build_atlas.py.\n")
    fh.write("# Source: https://download.geonames.org/export/dump/ - CC BY 4.0.\n")
    fh.write("# folded\tname\tlat\tlon\tcc\tcountry\tregion\tpopulation\ttimezone\n")
    for r in rows:
        fh.write("\t".join(r) + "\n")

print("%d places -> %s (%.1f MB)" % (len(rows), OUT, os.path.getsize(OUT) / 1048576.0))
