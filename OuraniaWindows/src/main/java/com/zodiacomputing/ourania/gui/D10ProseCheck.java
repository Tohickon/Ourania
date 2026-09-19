package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Zodiac;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Master list C9: prose for the seven points D10 shipped.
 *
 * <p><b>Why this suite exists.</b> D10 registered the Vertex, the East Point and the five
 * Hermetic lots, computed them and drew them on the wheel - with no prose behind any of them.
 * A reader who switched them on got a glyph and a one-line registry meaning. The cost showed
 * up somewhere else: <b>DataCheck's 168 standing failures were exactly these seven points'
 * missing composite entries</b>, a red number that had been read past in every regression run
 * for a month because it was always the same number.
 *
 * <p>The 336 paragraphs are David's, supplied 2026-09-15 as a dictionary. What this suite holds
 * is not whether the prose is good - that is his judgement - but the three ways a bulk set like
 * this fails silently:
 *
 * <ol>
 *   <li><b>Loaded but unreachable.</b> 42% of a supplied composite set once landed under keys
 *       nothing asks for, spelled naively rather than through the registry. So Part A asks
 *       through the same public getters the panels call, not the maps.</li>
 *   <li><b>The key and the text disagree.</b> A generated set can put the Scorpio paragraph
 *       under the Sagittarius key and read perfectly until someone with that placement looks.
 *       Part B holds every entry to naming its own point and its own placement.</li>
 *   <li><b>One voice serving two questions.</b> The natal entry describes a person and the
 *       composite entry describes what a pairing does with the placement; a set that reuses one
 *       for the other is the defect C5 names, and it is invisible unless measured. Part C
 *       measures it.</li>
 * </ol>
 *
 * <p><b>What Part C also reports, and does not fail.</b> This set is template-composed: two
 * different houses of the same point share up to 76% of their words, because the house sentence
 * has one shape with the house's own significations dropped in. That is the same shortfall C5
 * records for squares and trines, it is editorial rather than structural, and it is David's to
 * revise. The suite prints the figure so nobody has to rediscover it.
 *
 * <p>Part E is the question that had to be answered before anyone wrote 24 paragraphs a point:
 * the Vertex and East Point are horizon-derived, so their houses may be geometrically confined,
 * and prose for a placement that cannot occur is an orphan key (item C6). It is measured over a
 * spread of latitudes and hours rather than argued from the definition.
 */
public final class D10ProseCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    /** The seven points C9 covers, by registry id. */
    private static final String[] POINTS = {
        "vertex", "east_point", "lot_eros", "lot_necessity",
        "lot_courage", "lot_victory", "lot_nemesis",
    };

    /**
     * The words a reader would use for a house, so an entry can be held to naming its own.
     * The supplied set writes "8th House"; the ordinal alone would match "18th" too.
     */
    private static final String[] ORDINALS = {
        "", "1st", "2nd", "3rd", "4th", "5th", "6th",
        "7th", "8th", "9th", "10th", "11th", "12th",
    };

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        InterpretationService svc = InterpretationService.getInstance();

        part("A: every placement resolves through the getters the panels call",
            () -> reachable(svc));
        part("B: each entry names its own point and its own placement",
            () -> keysAgree(svc));
        part("C: natal and composite are not the same paragraph twice",
            () -> voices(svc));
        part("D: the frames a composite reading needs before it shows anything",
            () -> frames(svc));
        part("E: the houses these points can actually reach", () -> geometry(svc));
        part("F: the outer-planet conjunctions reach a reader", () -> transits(svc));

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    /** The registry entry for an id, or null. Bodies has indexOf(id) rather than a byId. */
    private static Bodies.Def def(String id) {
        int i = Bodies.indexOf(id);
        return i < 0 ? null : Bodies.at(i);
    }

    // ------------------------------------------------------------------ A

    /**
     * <b>Asked through the public getters, by registry NAME.</b> The natal getters take the
     * name a panel has in hand and normalise it through Bodies.byName; the composite getters
     * take the id. A set keyed naively - "part_of_fortune" for fortune, "black_moon_lilith"
     * for lilith - loads, counts, and answers nothing, which is how 165 of 390 rows went
     * missing in August. Both routes are exercised here for all seven points.
     */
    private static void reachable(InterpretationService svc) {
        for (String id : POINTS) {
            Bodies.Def d = def(id);
            ok(id + " is in the registry", d != null);
            if (d == null) {
                continue;
            }
            for (String sign : Zodiac.SIGNS) {
                String natal = svc.getPlanetInSign(d.name, sign);
                ok("natal " + id + " in " + sign + " resolves",
                    !natal.startsWith("Interpretation not found"));
                ok("natal " + id + " in " + sign + " has the bold summary",
                    natal.contains("</b><br><br>"));
                String comp = svc.getCompositePlanetSign(id, sign);
                ok("composite " + id + " in " + sign + " resolves", comp != null);
                ok("composite " + id + " in " + sign + " is not blank",
                    comp != null && comp.trim().length() > 120);
            }
            for (int h = 1; h <= 12; h++) {
                String natal = svc.getPlanetInHouse(d.name, h);
                ok("natal " + id + " in house " + h + " resolves",
                    !natal.startsWith("Interpretation not found"));
                ok("natal " + id + " in house " + h + " has the bold summary",
                    natal.contains("</b><br><br>"));
                String comp = svc.getCompositePlanetHouse(id, h);
                ok("composite " + id + " in house " + h + " resolves", comp != null);
                ok("composite " + id + " in house " + h + " is not blank",
                    comp != null && comp.trim().length() > 120);
            }
        }
    }

    // ------------------------------------------------------------------ B

    /**
     * The entry and its key have to be about the same thing.
     *
     * <b>This is the assertion that would have caught the defect the corpus already suffered</b>
     * - a supplied file whose key field disagreed with its own contents in 42% of rows. Naming
     * is the only evidence available from outside: the Scorpio paragraph says Scorpio, the 8th
     * house paragraph says 8th House, and every paragraph says which point it is about. A
     * generator that shifted its output by one placement fails here on 22 of 24 entries a point
     * rather than reading plausibly forever.
     */
    private static void keysAgree(InterpretationService svc) {
        for (String id : POINTS) {
            Bodies.Def d = def(id);
            if (d == null) {
                continue;
            }
            for (int s = 0; s < Zodiac.SIGNS.length; s++) {
                String sign = Zodiac.SIGNS[s];
                String proper = Character.toUpperCase(sign.charAt(0)) + sign.substring(1);
                for (boolean composite : new boolean[] {false, true}) {
                    String text = composite ? svc.getCompositePlanetSign(id, sign)
                                            : svc.getPlanetInSign(d.name, sign);
                    String where = (composite ? "composite " : "natal ") + id + "_" + sign;
                    if (text == null) {
                        continue;
                    }
                    ok(where + " names its own sign", text.contains(proper));
                    ok(where + " names no other sign", othersNamed(text, proper, Zodiac.SIGNS) == 0);
                    ok(where + " names its point", text.contains(d.name));
                    ok(where + " speaks in the right voice",
                        text.contains(composite ? "composite chart" : "natal chart"));
                }
            }
            for (int h = 1; h <= 12; h++) {
                for (boolean composite : new boolean[] {false, true}) {
                    String text = composite ? svc.getCompositePlanetHouse(id, h)
                                            : svc.getPlanetInHouse(d.name, h);
                    String where = (composite ? "composite " : "natal ") + id + "_" + h;
                    if (text == null) {
                        continue;
                    }
                    ok(where + " names its own house", text.contains(ORDINALS[h] + " House"));
                    ok(where + " names no other house", othersNamedHouse(text, h) == 0);
                    ok(where + " names its point", text.contains(d.name));
                    ok(where + " speaks in the right voice",
                        text.contains(composite ? "composite chart" : "natal chart"));
                }
            }
        }
    }

    /** How many signs other than its own a paragraph names. */
    private static int othersNamed(String text, String own, String[] all) {
        int n = 0;
        for (String other : all) {
            String proper = Character.toUpperCase(other.charAt(0)) + other.substring(1);
            if (!proper.equals(own) && text.contains(proper)) {
                n++;
            }
        }
        return n;
    }

    /** How many houses other than its own a paragraph names. */
    private static int othersNamedHouse(String text, int own) {
        int n = 0;
        for (int h = 1; h <= 12; h++) {
            if (h != own && text.contains(ORDINALS[h] + " House")) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ C

    /**
     * Natal and composite answer different questions, so they must not be the same words.
     *
     * <b>Held on a measured bound, not a guess.</b> Across all 168 natal/composite pairs the
     * word overlap runs 0.258 to 0.328, so 0.50 sits far above every real pair and far below
     * the 1.0 a reused paragraph would score. The cross-placement figure is printed rather than
     * asserted tightly: it reaches 0.760 because the set is template-composed, which is a
     * shortfall of the writing and not a fault in the wiring.
     */
    private static void voices(InterpretationService svc) {
        double worstVoice = 0.0;
        String worstVoiceAt = "";
        double worstPlacement = 0.0;
        String worstPlacementAt = "";
        Set<String> everyParagraph = new HashSet<>();
        int paragraphs = 0;

        for (String id : POINTS) {
            Bodies.Def d = def(id);
            if (d == null) {
                continue;
            }
            List<String> natalHouses = new ArrayList<>();
            for (int h = 1; h <= 12; h++) {
                String natal = svc.getPlanetInHouse(d.name, h);
                String comp = svc.getCompositePlanetHouse(id, h);
                natalHouses.add(natal);
                paragraphs += 2;
                everyParagraph.add(natal);
                everyParagraph.add(comp);
                ok("natal and composite " + id + " house " + h + " are not identical",
                    !natal.equals(comp));
                double j = overlap(natal, comp);
                if (j > worstVoice) {
                    worstVoice = j;
                    worstVoiceAt = id + " house " + h;
                }
                ok("natal and composite " + id + " house " + h + " are different paragraphs",
                    j < 0.50);
            }
            for (String sign : Zodiac.SIGNS) {
                String natal = svc.getPlanetInSign(d.name, sign);
                String comp = svc.getCompositePlanetSign(id, sign);
                paragraphs += 2;
                everyParagraph.add(natal);
                everyParagraph.add(comp);
                ok("natal and composite " + id + " in " + sign + " are not identical",
                    !natal.equals(comp));
                double j = overlap(natal, comp);
                if (j > worstVoice) {
                    worstVoice = j;
                    worstVoiceAt = id + " in " + sign;
                }
                ok("natal and composite " + id + " in " + sign + " are different paragraphs",
                    j < 0.50);
            }
            // Two houses of the same point, in the same voice: the template's own seam.
            for (int i = 0; i < natalHouses.size(); i++) {
                for (int j = i + 1; j < natalHouses.size(); j++) {
                    double o = overlap(natalHouses.get(i), natalHouses.get(j));
                    if (o > worstPlacement) {
                        worstPlacement = o;
                        worstPlacementAt = id + " houses " + (i + 1) + " and " + (j + 1);
                    }
                    ok("natal " + id + " houses " + (i + 1) + " and " + (j + 1)
                        + " are not the same paragraph", o < 0.85);
                }
            }
        }

        // 350 entries, 336 of them placements: every one a different paragraph.
        eq("no paragraph is duplicated across the set", paragraphs, everyParagraph.size());
        System.out.printf("  worst natal/composite overlap  %.3f  (%s)%n",
            worstVoice, worstVoiceAt);
        System.out.printf("  worst same-voice placement overlap  %.3f  (%s)"
            + "  - the template showing through, editorial%n",
            worstPlacement, worstPlacementAt);
    }

    /** Word overlap, intersection over union, on lowercase letter runs. */
    private static double overlap(String a, String b) {
        Set<String> wa = words(a);
        Set<String> wb = words(b);
        Set<String> union = new HashSet<>(wa);
        union.addAll(wb);
        Set<String> both = new HashSet<>(wa);
        both.retainAll(wb);
        return union.isEmpty() ? 0.0 : (double) both.size() / union.size();
    }

    private static final Pattern WORD = Pattern.compile("[a-z]+");

    private static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        Matcher m = WORD.matcher(text == null ? "" : text.toLowerCase());
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    // ------------------------------------------------------------------ D

    /**
     * The frame is load-bearing, which is not obvious from the data.
     *
     * <b>InterpretationPanel returns before it shows anything when the composite frame is
     * null.</b> So 168 composite paragraphs for these points would have loaded, satisfied
     * DataCheck, and still rendered nothing at all - the getter is reached only after the
     * frame is found. The seven frames are what make the rest of the file visible, and that
     * dependency is asserted here rather than left as a comment in the panel.
     */
    private static void frames(InterpretationService svc) {
        for (String id : POINTS) {
            Bodies.Def d = def(id);
            if (d == null) {
                continue;
            }
            String frame = svc.getCompositeBody(id);
            ok(id + " has a composite frame, without which the panel shows nothing",
                frame != null && frame.length() > 40);
            ok(id + " composite frame opens with a bold lead",
                frame != null && frame.startsWith("<b>") && frame.contains("</b><br><br>"));
            ok(id + " composite frame names the point", frame != null && frame.contains(d.name));

            String core = svc.getBodyCore(d.name);
            ok(id + " has a body core for the natal reading", !core.isEmpty());
            ok(id + " body core names the point", core.contains(d.name));
            // The registry one-liner stays the fallback, and it has to still be there: it is
            // what a reader sees for a point whose prose has not been written yet.
            ok(id + " keeps its registry meaning to fall back on", !d.meaning.isEmpty());
        }
    }

    // ------------------------------------------------------------------ E

    /**
     * Which houses can these points actually occupy?
     *
     * <b>The Vertex and East Point are horizon-derived, so this is a geometry question and not
     * an editorial one.</b> If the Vertex can only fall in a handful of houses, the paragraphs
     * for the others are orphan keys - written, loaded, and never asked for, which is item C6
     * measured at 14% of five files. Sampled across latitudes from the tropics to the polar
     * circle and around the clock, because a point confined at one latitude is not confined at
     * another.
     *
     * The assertion is the direction that matters: <b>every house a point is measured to reach
     * has prose behind it.</b> A gap there is a reader looking at a placement the app can
     * produce and cannot describe.
     */
    private static void geometry(InterpretationService svc) {
        SwissEph sw = new SwissEph(Ephemeris.PATH);
        // <b>Sized down deliberately, after measuring that the answer does not change.</b> The
        // first version swept 4,320 charts - nine latitudes, three longitudes, eight hours, four
        // dates, five years - and took about three minutes, which is a tax on every regression
        // run from here on. The confinement it finds was identical at 4,320, at 1,080 and at
        // this sample, because it is geometry rather than statistics: what the sweep needs is a
        // spread of latitudes and the ascendant taken all the way round, not more of them.
        double[] lats = {-45.0, 0.0, 34.0, 51.5, 64.1};
        double[] lons = {-87.6, 0.0};
        int[] years = {1984, 2026};

        // <b>These seven ship switched off, and that is deliberate.</b> D10 appended them with
        // every one unselected so nobody's chart changed under them, and ChartFrame clears the
        // ok flag of an unselected point - so the first thing this part establishes is that the
        // prose is unreachable until the reader asks for the point, and the second is what
        // happens once they do. The sampling below would otherwise measure nothing at all,
        // which is exactly what it did on the first run.
        //
        // <b>Asked of the registry's defaults, never of the reader's saved selection.</b> This
        // first read the selection out of the scratch settings file, which is a copy of the
        // reader's own - and on 2026-09-16 David switched these seven on to look at their new
        // prose, so seven assertions about a fresh install failed on his machine. That is item
        // J14, the defect this session had just diagnosed in ChartSetupCheck, written into a
        // new suite the same day. The claim is about what ships, so it is asked of what ships.
        boolean[] shipped = Bodies.defaults();
        for (String id : POINTS) {
            int i = Bodies.indexOf(id);
            ok(id + " ships switched off", i >= 0 && !shipped[i]);
        }
        Settings.saveBodySelection(shipped);
        double fresh = new SweDate(1984, 9, 8, 7.55).getJulDay();
        ChartFrame offByDefault;
        synchronized (sw) {
            offByDefault = ChartFrame.compute(sw, fresh, 41.8781, -87.6298, 'P', false, 0.0);
        }
        for (String id : POINTS) {
            Bodies.Def d = def(id);
            if (d == null) {
                continue;
            }
            ChartFrame.Body b = offByDefault.body(d.name);
            ok(id + " is not placed on a fresh install's chart", b == null || !b.ok);
        }

        boolean[] all = new boolean[Bodies.count()];
        Arrays.fill(all, true);
        Settings.saveBodySelection(all);

        // One chart, every point read off it: seven times fewer ephemeris calls, which is what
        // pays for four dates through the year instead of one midsummer.
        java.util.Map<String, Set<Integer>> houses = new java.util.LinkedHashMap<>();
        java.util.Map<String, Set<String>> signs = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> counted = new java.util.LinkedHashMap<>();
        for (String id : POINTS) {
            houses.put(id, new TreeSet<>());
            signs.put(id, new TreeSet<>());
            counted.put(id, 0);
        }
        int[][] dates = {{3, 20}, {6, 21}, {9, 22}, {12, 21}};
        int charts = 0;
        synchronized (sw) {
            for (int year : years) {
                for (int[] date : dates) {
                    for (double lat : lats) {
                        for (double lon : lons) {
                            for (int hour = 0; hour < 24; hour += 3) {
                                double jd = new SweDate(year, date[0], date[1], hour).getJulDay();
                                ChartFrame f = ChartFrame.compute(
                                    sw, jd, lat, lon, 'P', false, 0.0);
                                charts++;
                                for (String id : POINTS) {
                                    Bodies.Def d = def(id);
                                    ChartFrame.Body b = d == null ? null : f.body(d.name);
                                    if (b == null || !b.ok) {
                                        continue;
                                    }
                                    counted.merge(id, 1, Integer::sum);
                                    int h = Zodiac.houseOf(b.lon, f.cusps);
                                    if (h >= 1 && h <= 12) {
                                        houses.get(id).add(h);
                                    }
                                    signs.get(id).add(Zodiac.signName(b.lon));
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("  " + charts + " charts, latitudes " + lats[0] + " to "
            + lats[lats.length - 1] + ", " + dates.length + " dates a year across "
            + years.length + " years, every three hours");

        int orphaned = 0;
        for (String id : POINTS) {
            Bodies.Def d = def(id);
            if (d == null) {
                continue;
            }
            Set<Integer> housesSeen = houses.get(id);
            Set<String> signsSeen = signs.get(id);
            Set<Integer> never = new TreeSet<>();
            for (int h = 1; h <= 12; h++) {
                if (!housesSeen.contains(h)) {
                    never.add(h);
                }
            }
            orphaned += never.size() * 2;      // the natal entry and the composite one
            System.out.printf("  %-16s %5d charts  reaches houses %-30s signs %2d/12  "
                + "never %s%n",
                id, counted.get(id), housesSeen.toString(), signsSeen.size(),
                never.isEmpty() ? "-" : never.toString());
            ok(id + " was computed on the sample at all", counted.get(id) > 0);

            for (int house : housesSeen) {
                ok("natal " + id + " reaches house " + house + " and has prose for it",
                    !svc.getPlanetInHouse(d.name, house).startsWith("Interpretation not found"));
                ok("composite " + id + " reaches house " + house + " and has prose for it",
                    svc.getCompositePlanetHouse(id, house) != null);
            }
            for (String sign : signsSeen) {
                ok("natal " + id + " reaches " + sign + " and has prose for it",
                    !svc.getPlanetInSign(d.name, sign).startsWith("Interpretation not found"));
                ok("composite " + id + " reaches " + sign + " and has prose for it",
                    svc.getCompositePlanetSign(id, sign) != null);
            }
        }

        // <b>The Vertex cannot be near the Ascendant, and the East Point cannot be far from it.</b>
        // Both follow from the construction rather than from this sample: the Vertex is the
        // ecliptic point on the prime vertical in the WEST, so it sits around the descendant, and
        // the East Point is an ascendant of the equatorial horizon, so it sits around the first.
        // Asserted as the two halves that hold at every latitude sampled, which is the part a
        // wrong ascmc index would break - Bodies.derive takes the Vertex from ascmc[3] and the
        // East Point from ascmc[4], and swapping them is a one-character mistake.
        ok("the Vertex never lands on the Ascendant's own houses",
            !houses.get("vertex").contains(1) && !houses.get("vertex").contains(12)
                && !houses.get("vertex").contains(2));
        ok("the Vertex stays around the descendant half",
            houses.get("vertex").contains(6) || houses.get("vertex").contains(7));
        ok("the East Point stays around the Ascendant",
            houses.get("east_point").contains(1) || houses.get("east_point").contains(12));
        ok("the East Point never reaches the descendant houses",
            !houses.get("east_point").contains(6) && !houses.get("east_point").contains(7));
        for (String lot : new String[] {"lot_eros", "lot_necessity", "lot_courage",
                                        "lot_victory", "lot_nemesis"}) {
            eq(lot + " is longitude arithmetic and reaches every house",
                12, houses.get(lot).size());
        }

        // Reported, not failed. These entries are inert rather than wrong, and deleting prose
        // David wrote is not this suite's call - but the number belongs in the record, because
        // it is item C6 being created rather than closed.
        System.out.println("  entries for placements the sample never produced: " + orphaned
            + " of 336 - inert, and the Vertex and East Point account for all of them");
    }

    // ------------------------------------------------------------------ F

    /**
     * The 35 outer-planet conjunctions, and the key format that nearly lost them.
     *
     * <b>Every one of the 35 supplied rule ids was wrong for this app.</b> They read
     * "transit_jupiter_conjunction_vertex"; getTransitAspect asks for
     * "transit_jupiter_conjunction_natal_vertex", where the "natal_" segment says the target is
     * a birth placement rather than a second transiting body. Loaded as supplied, all 35 would
     * have parsed, counted, and answered nothing - so the keys were derived from the planet and
     * target fields instead, and the last loop here fails if anyone ever splices the file in its
     * original shape.
     *
     * <b>What "resolves" means is stricter than non-null.</b> getTransitAspect never returns
     * null: it falls back to the natal meaning of the pair under a parenthetical label, which is
     * a substitute a reader can see through. Everything in this part insists on a real recorded
     * reading, which is what none of these 35 had before today.
     */
    private static void transits(InterpretationService svc) {
        String[] planets = {"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"};

        // The set is written for exact conjunctions within one degree, which is the orb F3
        // settled on for every transit surface. If that constant moves, these readings describe
        // a contact the engine no longer calls exact.
        eq("the supplied set's 1 degree premise is still the engine's transit orb",
            1.0, com.zodiacomputing.ourania.astro.Transits.DEFAULT_ORB);

        for (String planet : planets) {
            for (String id : POINTS) {
                Bodies.Def d = def(id);
                if (d == null) {
                    continue;
                }
                String text = svc.getTransitAspect(planet, d.name, "Conjunction");
                String where = planet + " conjunction " + id;
                ok(where + " has a recorded reading rather than the labelled fallback",
                    text != null && !text.startsWith("<i>("));
                ok(where + " names the transiting planet", text != null && text.contains(planet));
                ok(where + " names the point it is about", text != null && text.contains(d.name));
                ok(where + " carries the heading, the gist and the reading",
                    text != null && text.contains("</b><br><i>") && text.contains("</i><br><br>"));

                // The trap: the supplied id, which is not this app's key.
                ok(where + " was not loaded under the supplied rule id",
                    !svc.hasEntry("transits",
                        "transit_" + planet.toLowerCase() + "_conjunction_" + id));
            }
        }
    }

    // ------------------------------------------------------------------ harness

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String name, Body body) throws Exception {
        System.out.println("=== Part " + name + " ===");
        int before = failures.size();
        body.run();
        int added = failures.size() - before;
        System.out.println("Part " + name.substring(0, 1) + ": "
            + (added == 0 ? "PASS" : added + " FAILURE(S)"));
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void eq(String label, Object expect, Object got) {
        ok(label + ": expected " + expect + ", got " + got,
            expect == null ? got == null : expect.equals(got));
    }

    private D10ProseCheck() { }
}
