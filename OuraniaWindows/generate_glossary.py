"""
Builds src/main/resources/data/glossary.json from reference/glossary/Master_Glossary.txt.

The glossary is David's (added 2 Oct 2026). It is written as Markdown chapters - "### " headings,
"* **Term**: definition" entries with indented sub-points, numbered "1. **Term**: ..." entries,
and "##### **Name**" entries whose bullets are fields (the lunar mansions, the decans).

What is left out, and why:
  - The block from "COLLECTED ARCHIVAL OVERVIEW" to the Pythagorean numerology catalogue is a
    copy of a PDF: its lines are broken at page width and across page footers, and its terms
    are a shorter summary of entries the Markdown chapters carry in full.
  - Tables ("| ..."), which summarise the entries above them.
  - The "#### **A**" letter headings of the alphabetical part.

Run from OuraniaWindows:  python3 generate_glossary.py
Each entry: {"term", "chapter", "section", "text"}; text is simple HTML (b, i, br).
"""
import json
import re

SRC = 'reference/glossary/Master_Glossary.txt'
OUT = 'src/main/resources/data/glossary.json'

LATEX = {r'\rightarrow': '→', r'\alpha': 'α', r'\beta': 'β', r'\delta': 'δ', r'\lambda': 'λ',
         r'\div': '÷', r'^\circ': '°'}


def latex(m):
    t = m.group(1)
    t = re.sub(r'\\frac\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}\{([^{}]*)\}', r'(\1)/(\2)', t)
    t = re.sub(r'\\text\{([^{}]*)\}', r'\1', t)
    t = re.sub(r'/\((\w+)\)$', r'/\1', t)              # "(A + B)/(2)" reads as "(A + B)/2"
    for k, v in LATEX.items():
        t = t.replace(k, v)
    t = t.replace('(', '(').replace('{', '').replace('}', '')
    t = re.sub(r'^\((\w+)\)/\((\w+)\)$', r'\1/\2', t)
    return t.strip()


def inline(s):
    s = re.sub(r'\\\\\((.*?)\\\\\)', latex, s)
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    s = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', s)
    s = re.sub(r'(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])', r'<i>\1</i>', s)
    return s.strip()


def plain(s):
    t = re.sub(r'<[^>]+>', '', inline(s)).strip()
    return t.replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&')


# The file names its subject areas only in its PDF part's "Structural Domain Index"; in the
# Markdown each area just restarts its sections at "1.". These are that index's names, in the
# order the areas restart.
DOMAINS = [
    'The Zodiac, Modalities & Elements',
    'Planets, Luminaries & Celestial Bodies',
    'Minor Bodies, Lots & Shadow Points',
    'Houses, Angles & Systems of Domification',
    'Essential Dignities, Debilities & Planetary Conditions',
    'Aspects, Angles & Major Configurations',
    'Chart Gestalt, Shapes & Structural Patterns',
    'Arabic Parts / Lots & Sensitive Formulas',
    'Chaldean Faces & The 36 Decans',
    'Chaldean Faces & The 36 Decans',            # the file catalogues the decans twice
    'The 28 Mansions of the Moon',
    'Sabian Symbols, Fixed Stars & Degrees',
    'Predictive Systems, Timing & Directional Arcs',
    'Esoteric, Psychological & Shadow Astrology',
]

# The file's own chapter headings, shortened for a list on a phone.
CHAPTERS = {
    'The Master Encyclopedic Glossary of Astrology': 'A to Z Essentials',
    'The Master Catalogue of Foundational & Astronomical Architecture Terms':
        'Foundational & Astronomical Architecture',
    'The Master Catalogue of Specialized Systems & Related Esoteric Disciplines':
        'Specialized Systems & Related Disciplines',
    'The Complete Master Archive of Remaining Astrological & Esoteric Domains':
        'Branches: Horary, Synastry, Mundane, Medical & More',
    'The Master Archive of Advanced, Harmonic & Esoteric Astrological Lexicon':
        'Advanced, Harmonic & Esoteric Lexicon',
}


def heading(line):
    """'### 🔥 2. The Four Elements (Triplicities)' -> 'The Four Elements (Triplicities)'."""
    t = line.lstrip('#').strip()
    t = re.sub(r'^[^\w*(]+', '', t)                     # the emoji
    t = re.sub(r'^Chapter \d+:\s*', '', t)
    t = re.sub(r'^\d+\.\s*', '', t)
    return plain(t)


# The few terms the skipped PDF summary defines and the Markdown does not, transcribed from it
# word for word (its page-width line breaks joined). Everything else it defines, the Markdown
# carries in full - checked 2 Oct 2026 term by term.
SPECIALIZED = 'Specialized Systems & Related Disciplines'
BRANCHES = 'Branches: Horary, Synastry, Mundane, Medical & More'
PDF_ONLY = [
    ('Bestial (Four-Footed) Signs', 'The Zodiac, Modalities & Elements', 'From the summary',
     'Aries, Taurus, Leo, Sagittarius (second half), and Capricorn—signs representing physical '
     'instincts, raw power, and animal drive.'),
    ('Electional Use of Mansions', 'The 28 Mansions of the Moon', 'From the summary',
     'Selecting specific lunar mansions to timing activities (e.g., launching businesses, '
     'planting crops, entering partnerships).'),
    ('Human Design', SPECIALIZED, 'From the summary',
     'A modern synthesis created by Ra Uru Hu combining Astrology, I Ching, Hindu-Brahmin Chakra '
     'system, Quantum Physics, and Kabbalah.'),
    ('Human Design Centers & Type', SPECIALIZED, 'From the summary',
     'The 9 energy centers defining bodygraph mechanics and the 4 primary Types: Manifestors, '
     'Generators, Projectors, and Reflectors.'),
    ('Astrological Tarot Correspondences', SPECIALIZED, 'From the summary',
     'System linking Major Arcana cards to planets/signs (e.g., The Emperor = Aries) and Minor '
     'Arcana pip cards to the 36 Decans.'),
    ('Numerology & Astrology', SPECIALIZED, 'From the summary',
     'Connecting numbers 1–9 to planetary archetypes (e.g., 1 = Sun, 2 = Moon, 3 = Jupiter, '
     '4 = Rahu, 5 = Mercury, 6 = Venus, 7 = Ketu, 8 = Saturn, 9 = Mars).'),
    ('Astrocartography (Relocation)', SPECIALIZED, 'From the summary',
     'Mapping planetary horizon and meridian lines across world geography to identify optimal '
     'location power lines.'),
    ('Mundane Astrology', BRANCHES, 'From the summary',
     'Branch of astrology interpreting charts for nations, global politics, financial markets, '
     'political leaders, and world events.'),
    ('Horary Astrology', BRANCHES, 'From the summary',
     'Traditional branch of divination casting a chart for the exact moment a specific, urgent '
     'question is asked to derive an answer.'),
    ('Electional Astrology (Inceptions)', BRANCHES, 'From the summary',
     'The art of selecting an auspicious future date and time to launch an enterprise, '
     'marriage, contract, or voyage.'),
    ('Synastry & Composite Charts', BRANCHES, 'From the summary',
     'Synastry overlays two individual charts to assess dynamic attraction; Composite charts '
     'create a new midpoint chart for the relationship itself.'),
]


def main():
    lines = open(SRC, encoding='utf-8').read().split('\n')
    entries = []
    chapter = section = ''
    cur = None                 # the entry sub-points attach to
    field_entry = None         # a ##### entry collecting its fields
    skipping = False
    restarts = 0
    in_domains = False
    for raw in lines:
        line = raw.rstrip()
        if line.startswith('COLLECTED ARCHIVAL OVERVIEW'):
            skipping = True
            continue
        if skipping:
            if 'master catalogue of **Pythagorean Numerology' in line:
                skipping = False
                chapter = 'Pythagorean Numerology & The Science of Numbers'
            continue
        if not line.strip() or line.startswith('|') or line.strip() == '---':
            continue
        if line.startswith('### '):
            h = heading(line)
            numbered = re.search(r'\d+\.|Chapter', line)
            if not numbered:
                chapter = CHAPTERS.get(h, h)
                section = ''
                # The thirteen unheaded areas follow the foundational chapter.
                in_domains = chapter == 'Foundational & Astronomical Architecture'
            else:
                if in_domains and re.search(r'###\s*\S*\s*1\.', line):
                    if restarts < len(DOMAINS):
                        chapter = DOMAINS[restarts]
                    restarts += 1
                section = h
            cur = field_entry = None
            continue
        if line.startswith('#### '):
            t = heading(line)
            if len(t) > 2:                               # not a letter heading
                section = t
            cur = field_entry = None
            continue
        if line.startswith('##### '):
            field_entry = {'term': plain(line.lstrip('#')), 'chapter': chapter,
                           'section': section, 'text': ''}
            entries.append(field_entry)
            cur = field_entry
            continue
        m = re.match(r'^(\* |\d+\. )\*\*(.+?)\*\*:?\s*(.*)$', line)
        if m and field_entry is not None and m.group(1) == '* ':
            # A field of a ##### entry: "Tropical Span: ...".
            sep = '<br>' if field_entry['text'] else ''
            field_entry['text'] += sep + '<b>' + plain(m.group(2)).rstrip(':') + ':</b> ' \
                + inline(m.group(3).lstrip(': '))
            continue
        if m:
            field_entry = None
            term = plain(m.group(2)).rstrip(':').strip()
            text = inline(m.group(3).lstrip(':').strip())
            cur = {'term': term, 'chapter': chapter, 'section': section, 'text': text}
            entries.append(cur)
            continue
        sub = re.match(r'^\s+(?:[*-]|(\d+)\.) (.*)$', line)
        if sub and cur is not None:
            mark = (sub.group(1) + '.') if sub.group(1) else '&bull;'
            cur['text'] += ('<br>' if cur['text'] else '') + mark + ' ' + inline(sub.group(2))
            continue
        # Prose between entries: a section's introduction. Not an entry.
    for term, chap, sect, text in PDF_ONLY:
        entries.append({'term': term, 'chapter': chap, 'section': sect, 'text': inline(text)})
    # Exact repeats (the same term and words in two chapters) once; differing ones both kept.
    seen = set()
    out = []
    for e in entries:
        key = (e['term'].lower(), re.sub(r'\W+', '', e['text'].lower()))
        if key in seen or not e['term']:
            continue
        seen.add(key)
        out.append(e)
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump({'source': 'reference/glossary/Master_Glossary.txt',
                   'generator': 'generate_glossary.py', 'entries': out}, f, ensure_ascii=False,
                  indent=1)
    print(len(entries), 'entries read,', len(out), 'written,',
          len({e['chapter'] for e in out}), 'chapters')


if __name__ == '__main__':
    main()
