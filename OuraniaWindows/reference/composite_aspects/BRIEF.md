# Writing brief: Ourania composite readings

This brief is for whoever writes the readings, person or model. It is complete without any
other file. Give it to the writer together with one skeleton file from `todo/` at a time.

## The job

Ourania is an astrology app. A **composite chart** is one chart made from two people's charts,
read as the chart of the relationship itself. The app shows a short reading for each aspect
between two points in that chart.

The readings it has now were assembled by a template. A square and a trine of the same pair
differ by one clause, and the text has grammar errors left by the template ("the capacity to
secures", "the urge to draws"). Each one is to be replaced by a reading that was written for
that pair and that aspect.

There are two sets.

| Set | What a reading describes | To write |
|---|---|---|
| **composite** | An aspect between two points in the relationship's own chart. Permanent: it describes what the relationship is like. | 1,045 (255 of 1,300 are done) |
| **transit** | A moving body in the sky making an aspect to a point in the relationship's chart. Temporary: it describes a period the relationship passes through. | 8,841 |

The skeleton files in `todo/composite/` and `todo/transit/` list every reading still needed,
one file per first body. `python build.py --todo` regenerates them from what is still missing.

## Format

Plain text. Three lines per reading, then a blank line. Do not change the `=` line.

```
=juno square venus
What the two of you have promised and what the two of you enjoy pull in different directions.
Juno is the contract: vows, fairness, who carries what. Venus is the affection and pleasure that made the contract worth signing. With a square between them, keeping the terms can feel like it costs the warmth. ...
```

- Line 1: the key. `=first aspect second` for composite, `=transit first aspect second` for
  transit. In a transit key the first body is the one moving in the sky and the second is the
  point in the relationship's chart.
- Line 2: the **lead**. One sentence, shown in bold. It states the whole dynamic plainly.
- Line 3: the **body**. One paragraph of three to six sentences.
- Lines beginning `#` are notes and are ignored.

## Hard rules (the checker enforces these)

1. **50 to 120 words** per reading, lead and body together. Aim for 65 to 90.
2. **No two aspects of the same pair may share half their wording.** Do not write one
   paragraph and change a clause. Each aspect is a different situation and needs different
   sentences. Do not repeat the same definition sentence for a body across a pair's aspects.
3. **No sentence of six or more words may appear in three readings.** No stock closing lines.
4. **No double quotes, no angle brackets, no ampersands, no dashes used as punctuation.** Use
   commas, colons, full stops and parentheses. Apostrophes are fine.
5. The lead ends with a full stop. The body ends with a full stop or question mark.
6. Write every aspect listed for a pair. A pair with one aspect missing is refused.

Check with `python build.py composite` or `python build.py transit`. Put finished files in
`drafts/` (composite) or `transit_drafts/` (transit). `python build.py --write` splices them
into the app once every check passes.

## Voice

- Plain, observant and dry. A knowledgeable person talking, with no mystical vocabulary.
- Speak about "this relationship", "the pair" or "the two of you". Never "the native".
- Say what the two bodies stand for in a few words, worked into the sentence. Then say what
  this aspect between them looks like in ordinary life. Then name the usual way it goes wrong
  and what helps.
- Be concrete: meals, money, arguments, in-laws, work, bed. Avoid abstractions such as
  "energies", "vibrations", "evolutionary", "alchemical", "cosmic", "sacred journey".
- No flattery and no doom. A hard aspect is workable and an easy one has a cost.
- Do not invent facts about astronomy or cite authors. Stay with the meanings given below.
- No rhetorical questions as leads, no exclamation marks, no lists inside a reading.

## What each aspect means

Use this to make each aspect of a pair a different situation.

| Aspect | Angle | Character | Typical shape of the reading |
|---|---|---|---|
| conjunction | 0° | Fused. The two things act as one and cannot be told apart. | Strength of the blend, and what gets lost because there is no distance. |
| sextile | 60° | Opportunity. Helpful, but only when used on purpose. | What becomes possible if the pair makes the effort, and that it lies idle otherwise. |
| square | 90° | Friction. Each gets in the other's way. | The specific collision, how it shows, and the practical way to work it. |
| trine | 120° | Ease. The two support each other without effort. | What comes naturally, and the complacency or blind spot that ease brings. |
| opposition | 180° | Polarity. The two ends get divided between the partners, or between the pair and other people. | Who ends up holding which end, and how to share both. |
| quincunx | 150° | Mismatch. The two have nothing in common and keep needing adjustment. | An awkward fit that never settles; small constant corrections. |
| semisextile | 30° | Neighbours. A slight, nagging link between unlike things. | A minor friction or small step that is easy to overlook. |
| semisquare | 45° | Irritation. A smaller, more nervous square. | Low-level chafing that builds if ignored. |
| sesquiquadrate | 135° | Agitation. Friction that breaks out under strain. | Overreaction at pressure points; needs a steadier hand. |
| quintile | 72° | Talent. A creative, deliberate knack. | Something the pair can make or do well if it applies skill. |
| biquintile | 144° | Mastery. Talent refined by practice. | A skill that matures with repetition. |
| sesquiquintile | 108° | Craft under effort. Creative work that takes persistence. | A gift that only shows through steady application. |
| septile | 51.4° | Fate and inspiration. An irrational, compelling pull. | Something that feels meant; hard to explain and hard to refuse. |
| novile | 40° | Completion and inner ripening. | A quiet period or theme of finishing and readiness. |
| decile | 36° | Resourcefulness. A small practical aptitude. | A modest, useful skill or support. |

For **transit** readings the same characters apply, but the reading describes a stretch of
time: what the period brings, how it feels, what tends to go wrong, and what to do with it.
Fast bodies (Moon, Sun, Mercury, Venus, Mars, the moving Ascendant and Midheaven) mark hours
to weeks. Slow bodies (Jupiter, Saturn, Chiron, Uranus, Neptune, Pluto, the nodes and most
asteroids) mark months to years. Let the wording fit the speed without quoting durations.

## What each body means

Taken from the app's own text. In a composite reading these describe a function of the
relationship. In a transit reading the first body is an outside influence acting on that
function for a time.

| Key | Name | Stands for | Goes wrong as |
|---|---|---|---|
| sun | Sun | The relationship's identity, vitality and purpose | Pride, one partner dominating the shared identity |
| moon | Moon | Its emotional home, safety and habits of feeling | Dependence, moodiness, retreat into comfort |
| mercury | Mercury | How the two talk and think together | Rationalising feelings, nervous chatter, sparring |
| venus | Venus | Affection, pleasure, shared taste, what holds them together | Peace at any price, shallow harmony, self-indulgence |
| mars | Mars | Drive, anger, sexual energy, initiative | Ego clashes, haste, defensive fights |
| jupiter | Jupiter | Shared meaning, growth, optimism, generosity | Excess, overpromising, escape from limits |
| saturn | Saturn | Structure, limits, responsibility, staying power | Fear, heaviness, rigid walls |
| uranus | Uranus | Freedom, surprise, originality, sudden change | Erratic rebellion, instability, disconnection |
| neptune | Neptune | Ideals, imagination, compassion, loss of boundaries | Escapism, confusion, playing the victim, deception |
| pluto | Pluto | Power, crisis, deep change, what is hidden | Control, obsession, power struggles |
| chiron | Chiron | The shared wound and the understanding that comes from it | Self-pity, feeling rejected, repeating the injury |
| north_node | North Node | The direction the relationship is meant to grow in | Resisting growth, fear of the unfamiliar |
| south_node | South Node | Old habits, the familiar default, what is already known | Stagnation, repeating the past |
| ascendant | Ascendant | How the pair comes across; its outward manner and first impression | A mask, performance, hiding division |
| descendant | Descendant | Other people: partners, close associates, open rivals; what the pair meets in others | Projecting onto outsiders, living through others |
| mc | Midheaven | Public standing, shared ambition, reputation, vocation | Status-chasing, neglecting home |
| ic | IC (Imum Coeli) | Home, family, roots, private foundations | Withdrawal, being ruled by family or the past |
| ceres | Ceres | Practical care: food, health, daily tending; loss and return | Smothering, hoarding, fear of abandonment |
| pallas | Pallas | Strategy, skill, clear judgment, fairness | Coldness, using logic as a weapon |
| juno | Juno | Commitment as a contract between equals; loyalty | Score-keeping, staying from duty, betrayal |
| vesta | Vesta | Focus, dedication, what is held sacred | Cold withdrawal, self-denial, burnout |
| lilith | Black Moon Lilith | The untamed part; raw honesty; refusal to be owned | Outbursts, power struggles, fear of rejection |
| eris | Eris | The voice of whoever was left out; exposing unfairness | Provoking for its own sake, alienation |
| eros | Eros | Passionate attraction and desire | Jealousy, fixation, infatuation |
| hygiea | Hygiea | Health, hygiene, prevention, wholesome habits | Health anxiety, perfectionism |
| nessus | Nessus | Harm passed down, and the place it can be stopped | Revenge, manipulation, victim and offender roles |
| pholus | Pholus | A small cause with large effects; the lid coming off | Runaway chain reactions, overwhelm |
| fortune | Part of Fortune | Where ease, luck and material wellbeing come from | Complacency, relying on luck |
| spirit | Part of Spirit | Conscious intention; what the pair sets out to do | Forcing outcomes, ambition without feeling |

Geometry worth knowing, so that oppositions make sense:
- Opposite the Ascendant is the Descendant. Opposite the Midheaven is the IC.
- Opposite the North Node is the South Node. A body conjunct one is opposite the other, and a
  body square one is square both.

## Worked example: one pair, five aspects

Read these for the differences between them. No sentence is reused.

```
=ceres conjunction moon
Feeling and feeding are almost the same thing in this relationship.
The Moon is the emotional home and Ceres the practical care. Joined, they make a deeply nurturing bond, in which love is shown through food, comfort and constant small attentions. The two of you sense each other's needs before they are spoken. Home matters a great deal. The closeness can become dependence, and any separation, even a short one, may be felt more sharply than seems reasonable.

=ceres sextile moon
Comfort is easy to give and receive here once someone asks.
The Moon holds the pair's feelings and Ceres its practical tending. The sextile lets one answer the other without strain. When either of you is low, the right kind of looking-after is close to hand. It helps to make the need known. The arrangement is responsive more than automatic, and it works best for those who speak up.

=ceres square moon
Looking after each other and feeling safe with each other do not line up easily.
Ceres is practical care: food, health, the daily tending, and also the pain of letting go. The Moon is the relationship's emotional home. Squared, care can arrive in a form the other person cannot take in. One feeds when the other wanted comfort, or holds on when the other needed room. Over-protection, quiet resentment about who gives more, and fear of being left are the forms it takes. It eases when each of you says what care looks like to you.

=ceres trine moon
Care comes naturally in this relationship and lands where it is needed.
Ceres is practical tending and the Moon is the emotional home. With a trine between them, the practical and the emotional kinds of support run together, and the two of you tend to feel looked after without having to ask. Separations are easier to bear than in most pairings, because the return is trusted. The weak point is comfort itself. A relationship this good at soothing can become a place to hide from anything hard.

=ceres opposition moon
One of you needs and the other provides, and it is hard to change places.
The Moon's feelings face Ceres's care. In this relationship the roles of the one who is looked after and the one who looks after can become fixed. The provider's own needs go unseen, and the one provided for may feel like a child. Both lose something. It rebalances when the carer says what would comfort them, and the other takes a turn at giving it.
```

## Worked example: a transit pair

```
=transit saturn square sun
A period that tests whether this relationship knows what it is for.
Saturn brings limits, delays and accountability, and the composite Sun is the relationship's purpose and vitality. During a square, outside pressure lands directly on the central aim: time is short, obligations pile up, and plans that rested on optimism are shown up. It can feel like the relationship itself is being blocked. More often it is being asked to drop what it cannot sustain. What gets through is a realistic plan, fewer commitments, and each of you doing what you said you would.

=transit saturn trine sun
A steady period in which the relationship can build something that lasts.
Saturn brings structure and patience, and the composite Sun is the relationship's purpose and vitality. During a trine, discipline comes without much resistance: routines hold, long-term decisions feel clear, and effort put in now tends to stay put. Nothing dramatic marks the time, which is why it is often wasted. This is the window for the unglamorous work, such as agreeing on money, making a commitment formal, or finishing what was started.
```

## How to work through it

1. Take one skeleton file from `todo/`. Each is one first body against every second body.
2. Write one pair at a time: all of its aspects together, so they can be made different.
3. Save the finished file into `drafts/` or `transit_drafts/` under the same name.
4. Run `python build.py`. Fix whatever it reports. The commonest failures are two aspects of
   a pair sharing too much wording, a stock sentence used three times, and a double quote.
5. When a file passes, run `python build.py --write` and update the count for `CorpusCheck`
   in `OuraniaWindows/known-red.txt` (it falls by one for each square/trine pair replaced).

Twenty to thirty readings per request is a workable batch for a model. Asking for a whole
skeleton file at once tends to produce repeated sentences by the end.

## What is already done

`drafts/01` to `05`: Black Moon Lilith and Ceres against every other body in the composite
set, 255 readings. They are in the app's files on branch `c5-composite-aspects` and can serve
as further examples of the voice.
