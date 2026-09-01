package com.zodiacomputing.ourania.gui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class InterpretationService {
    
    private static InterpretationService instance;
    
    private Map<String, String> planetSigns = new HashMap<>();
    private Map<String, String> planetHouses = new HashMap<>();
    private Map<String, String> aspects = new HashMap<>();
    private Map<String, String> transits = new HashMap<>();
    private Map<String, String> signs = new HashMap<>();
    private Map<String, String> houses = new HashMap<>();
    private Map<String, String> decans = new HashMap<>();
    private Map<String, String> sabian = new HashMap<>();
    private Map<String, String> tarotPlanets = new HashMap<>();
    private Map<String, String> tarotSigns = new HashMap<>();
    private Map<String, String> tarotDecans = new HashMap<>();
    private Map<String, String> transitHouses = new HashMap<>();
    private Map<String, String> degreeSummaries = new HashMap<>();
    private Map<String, String> degreeFullTexts = new HashMap<>();
    private Map<String, String> ascendantData = new HashMap<>();
    private Map<String, String> icData = new HashMap<>();
    private Map<String, String> descendantData = new HashMap<>();
    private Map<String, String> mcData = new HashMap<>();
    private Map<String, String> sabianFullTexts = new HashMap<>();
    private Map<String, String> sabianShadows = new HashMap<>();
    private Map<String, String> sabianKeywords = new HashMap<>();
    private Map<String, String> macroDynamics = new HashMap<>();

    /** Core one-paragraph descriptions for bodies the main dataset never covered. */
    private Map<String, String> bodyCores = new HashMap<>();

    private InterpretationService() {
        loadData();
        loadDegreesData();
        loadAnglesData();
        loadSabianDetailData();
        loadExtraBodiesData();
    }

    /**
     * Prose for the points the original dataset does not cover, from its own file.
     *
     * <b>A separate file on purpose.</b> `interpretations.json` is 615 KB read by a
     * hand-rolled line parser, and nine of the fifteen original data files did not parse -
     * see [[llm-json-corruption-patterns]] in the vault. Adding a couple of hundred entries
     * by editing that file risks the whole dataset to extend a corner of it; a new file
     * risks only itself, and the angle files already establish the precedent of loading more
     * than one. Merged into the same maps, so every getter downstream is unchanged.
     *
     * Loaded LAST and non-destructively: an entry already present in the main dataset wins,
     * so this file can never silently redefine an existing interpretation.
     */
    /**
     * The supplementary prose files, loaded in order.
     *
     * <b>Two files, and the split is load-bearing.</b> `extra_bodies.json` carries the core
     * descriptions, the aspect prose and the transit prose; `body_placements.json` carries
     * the sign and house prose. They were a single file until 2026-08-13, when a wholesale
     * rewrite of it dropped all 192 placement entries at once. Two agents work this project
     * and neither can see the other's edits in flight, so separate files per concern is the
     * only thing that actually prevents one rewrite from taking out the other's work.
     * <b>Add a new file to this list rather than growing either of these.</b>
     */
    /*
     * aspect_prose_v2.json is FIRST on purpose. Earlier files win under putIfAbsent, and
     * that file exists to replace aspect prose that is already present further down the
     * list - the GenerateExtraBodies template output, which said the same six things 154
     * ways each. Putting the replacements in their own file rather than editing
     * extra_bodies.json in place is rule 1: that file is Antigravity's, and a wholesale
     * rewrite of it once deleted 192 entries. Removing this one line reverts the whole
     * prose replacement.
     *
     * Note it still cannot outrank interpretations.json, which loads before any of these
     * and uses put() rather than putIfAbsent. That is correct here - the 270 classical
     * planet pairs in that file are hand-written and better than anything replacing them.
     */
    private static final String[] EXTRA_FILES = {
        "aspect_prose_v2.json",
        "spirit_prose.json",
        "minor_aspect_prose.json",
        // Semisextile, semisquare and quincunx have no pair readings at all - the file above
        // covers quintile, sesquiquadrate and sesquiquintile only. Its own file so a generator
        // filling the other three cannot overwrite the 1,071 already there.
        "minor_aspect_prose_b.json",
        "semisquare_prose.json",
        "pattern_detail.json",
        "aspect_general.json",
        "quincunx_gap.json",
        "extra_bodies.json",
        "body_placements.json",
        "macro_dynamics.json",
        "relationship_prose.json",
        "synastry_interaspects.json",
        "composite_placements.json",
        "composite_aspects.json",
        "composite_aspect_pairs.json",
        "composite_signs.json",
        "composite_placements_b.json",
        "composite_minor_aspects.json",
        "synastry_overlays.json",
        "synastry_angle_contacts.json",
        "composite_angles.json",
        "composite_sabian.json",
        "composite_transits_ptolemaic_t.json",
        "composite_transit_angles.json",
        "composite_minor_aspects_classical.json",
        "composite_transit_everything_else.json",
        "composite_transit_minor_aspects_asteroids_angles.json",
        "composite_transit_ptolemaic_asteroids_angles_classical.json",
        // The five Ptolemaic aspects for every asteroid, lot and node in composite -
        // 910 entries, David 2026-08-31. Registered LAST on purpose: putIfAbsent means
        // the first file to claim a key wins, so this fills gaps and shadows nothing.
        // <b>The supplied filename said "minor_aspects" and the contents are the five
        // MAJORS</b> - conjunction, opposition, square, trine, sextile. Renamed here to
        // match what is actually in it, because a file named for the wrong aspect set is
        // how the next person writes a duplicate.
        "composite_aspects_expanded.json",
        // The 78 asteroid-to-asteroid composite pairs, five majors each - 390 entries,
        // David 2026-08-31. The class the file above deliberately left empty.
        // <b>Its supplied "key" field was wrong for this app in 165 of 390 rows</b> and was
        // discarded: it spelled the registry ids naively - black_moon_lilith for lilith,
        // part_of_fortune for fortune, part_of_spirit for spirit. Keys here are derived from
        // the body NAMES through Bodies.byName, the same route bodyKey takes, so there is one
        // normalisation rather than two. Trusting the supplied field would have loaded 42% of
        // the file under keys nothing ever asks for - present, counted, and unreachable.
        "composite_asteroid_pairs.json",
        // The six minor aspects this engine computes, for every pair, in all four voices -
        // natal, composite, synastry and transit. 14,964 entries, David 2026-08-31.
        //
        // <b>The supplied set carried ten aspects and four of them do not exist here</b>:
        // septile, novile, biquintile and decile are not on Aspects.Type, so 9,976 entries
        // were dropped rather than loaded. They would have parsed, counted and never been
        // asked for. Whether to add those four to the engine is a judgement about what a
        // reading should carry, and it is David's - the prose is waiting if he takes it.
        //
        // <b>Eager, unlike the decan and mansion sets.</b> Those own their sections outright
        // and are read only when someone opens a body, so deferring them costs nothing. These
        // four share sections with files already loaded and are consulted on every reading, so
        // deferring would move the same cost from startup to the first click and add a way for
        // load order to matter. Last in the list, so every earlier file still wins its keys.
        "minor_aspects_natal.json",
        "minor_aspects_composite.json",
        "minor_aspects_synastry.json",
        "minor_aspects_transit.json",
        // Framing sentences for all fifteen aspect types, and the composite-transit readings
        // for the four added the same day. David 2026-08-31.
        //
        // <b>Registered after the files that already hold eleven of those frames</b>, so
        // putIfAbsent keeps the authored originals and only the four newcomers are taken from
        // here. Adding an aspect to the enum is not finished when the pair prose exists: the
        // frame is a separate entry per type, and SynastryCheck went red in eight places
        // saying exactly that.
        "aspect_frames_v2.json",
        "composite_transits_new_aspects.json",
        "composite_transits_new_aspects_b.json",
        "tarot_bodies.json"
    };


    /**
     * Decode backslash-u escapes in a prose value.
     *
     * <b>The line parser is not a JSON parser</b> - it takes the text between the first and
     * last quote on the line and stores it verbatim - so an escape written by a generator
     * arrived in the UI as six literal characters. Measured 2026-08-23: 44 of them across four
     * data files, including Ceres' body core, which displayed the escape rather than an em
     * dash. They had been on screen for weeks.
     *
     * Fixed here rather than in the data because the data is written by several generators and
     * by two agents; the loader is the one place all of it passes through.
     *
     * <b>Do not write the escape sequence itself into this file's comments.</b> Java decodes
     * those before the compiler sees them, in javadoc as well as in code, and the first
     * attempt at this method failed to compile with "illegal unicode escape" pointing at
     * line 1.
     */
    static String unescapeUnicode(String value) {
        if (value.indexOf('\\') < 0) {
            return value;                       // the overwhelming majority of lines
        }
        StringBuilder sb = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            if (value.charAt(i) == '\\' && i + 5 < value.length() && value.charAt(i + 1) == 'u') {
                try {
                    sb.append((char) Integer.parseInt(value.substring(i + 2, i + 6), 16));
                    i += 6;
                    continue;
                } catch (NumberFormatException e) {
                    // Not an escape after all - fall through and keep the backslash as text.
                }
            }
            sb.append(value.charAt(i));
            i++;
        }
        return sb.toString();
    }

    /** Where every loader in this class reads from. */
    public static final String DATA_DIR = "src/main/resources/data/";

    /**
     * The supplementary files, as full paths, for anything that needs to check them.
     *
     * <b>Exists so that there is exactly one list.</b> DataCheck used to carry its own copy with
     * a comment asking whoever edited one to edit the other, and within a day a third file
     * (`quincunx_gap.json`) had been added here and not there - so the new file was loaded by the
     * app and validated by nothing. A comment is not a mechanism. Returns a copy so a caller
     * cannot reorder the real list, which is load-bearing: earlier files win under putIfAbsent.
     */
    public static String[] extraFilePaths() {
        String[] names = allFileNames();
        String[] out = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = DATA_DIR + names[i];
        }
        return out;
    }

    private void loadExtraBodiesData() {
        for (String name : EXTRA_FILES) {
            loadExtraFile(new File(DATA_DIR + name));
        }
    }

    /**
     * Files big enough that loading them at startup costs more than they are worth.
     *
     * <b>Measured, not assumed.</b> The corpus loaded in about 200 ms until 2026-08-31,
     * when a body-in-decan and body-in-mansion set arrived - 3,712 entries - and took it to
     * 475 ms. A Sabian set of 20,880 projected to roughly two seconds. A reader opens one
     * body at a time and will see perhaps twenty-nine of those entries in a session, so the
     * cost is paid entirely for prose nobody asks for.
     *
     * <b>One section per file, and the section belongs to that file alone.</b> The eager
     * pass uses putIfAbsent, where the first file to claim a key wins, so load ORDER is
     * load-bearing. A lazy file arrives whenever it is first asked for, which is always
     * after every eager file - the same position it would have held at the end of
     * EXTRA_FILES. That equivalence only holds while no eager file writes to a lazy
     * section, and DataCheck asserts exactly that rather than trusting this comment.
     */
    private static final String[][] LAZY_FILES = {
        {"body_decan",   "body_decans.json"},
        {"body_mansion", "body_mansions.json"},
    };

    /** Lazy sections already pulled in. Guarded because the suites call off the EDT. */
    private final java.util.Set<String> lazyLoaded = new java.util.HashSet<>();

    /**
     * Pull in the file that owns this section, once.
     *
     * Called by every getter that reads a lazy section. A getter that forgets returns null
     * forever and looks exactly like missing data, so each one is asserted separately.
     */
    private synchronized void ensureLazy(String section) {
        if (!lazyLoaded.add(section)) {
            return;
        }
        for (String[] row : LAZY_FILES) {
            if (row[0].equals(section)) {
                loadExtraFile(new File(DATA_DIR + row[1]));
                return;
            }
        }
    }

    /**
     * Pull in every lazy section, for the checks.
     *
     * <b>DataCheck validates each key of each registered file against the map it landed
     * in</b>, and a deferred file has landed nowhere until something asks for it - so
     * without this the suite reports 3,712 keys missing and is right to. The app never
     * calls this: deferring the load is the entire point, and a check that quietly made
     * production eager would be testing an arrangement nobody ships.
     */
    public void loadEveryLazySection() {
        for (String[] row : LAZY_FILES) {
            ensureLazy(row[0]);
        }
    }

    /** Every data file this build reads, eager first then lazy - the order keys resolve in. */
    public static String[] allFileNames() {
        String[] out = new String[EXTRA_FILES.length + LAZY_FILES.length];
        System.arraycopy(EXTRA_FILES, 0, out, 0, EXTRA_FILES.length);
        for (int i = 0; i < LAZY_FILES.length; i++) {
            out[EXTRA_FILES.length + i] = LAZY_FILES[i][1];
        }
        return out;
    }

    /** The section each lazy file owns, for the checks that guard the arrangement. */
    public static String[][] lazyFiles() {
        String[][] out = new String[LAZY_FILES.length][];
        for (int i = 0; i < LAZY_FILES.length; i++) {
            out[i] = new String[] {LAZY_FILES[i][0], LAZY_FILES[i][1]};
        }
        return out;
    }


    private void loadExtraFile(File file) {
        if (!file.exists()) {
            System.err.println(file.getName() + " not found - some chart points will show "
                + "their one-line meanings only.");
            return;
        }
        int added = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            Map<String, String> currentMap = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                Map<String, String> section = sectionFor(line);
                if (section != null) {
                    currentMap = section;
                } else if (isSectionHeader(line)) {
                    // A section this build does not know about. Target null rather than
                    // leaving the previous section in place, because otherwise every entry
                    // of an unrecognised section is filed silently under whichever section
                    // happened to precede it. That is not hypothetical: `transits` was
                    // unhandled here, so its entries were being served as natal aspects.
                    currentMap = null;
                    System.out.println("  (unknown section in " + file.getName() + ": "
                        + line + " - entries skipped)");
                } else if (line.startsWith("\"") && currentMap != null) {
                    int colonIdx = line.indexOf("\":");
                    if (colonIdx == -1) {
                        continue;
                    }
                    String key = line.substring(1, colonIdx);
                    int valStart = line.indexOf("\"", colonIdx + 1);
                    int valEnd = line.lastIndexOf("\"");
                    if (valStart != -1 && valEnd > valStart) {
                        String value = unescapeUnicode(line.substring(valStart + 1, valEnd));
                        if (currentMap.putIfAbsent(key, value) == null) {
                            added++;
                        }
                    }
                }
            }
            System.out.println("Loaded " + file.getName() + ": " + added + " entries.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The relationship sections, added 2026-08-24.
     *
     * <b>Every one of these exists because the natal dataset is the wrong voice for a
     * relationship, not merely a thin one.</b> A composite Sun is not a person&#39;s identity,
     * and a cross-chart aspect is not a transit. Before this file the app served natal
     * placement prose for composite bodies and TRANSIT prose for synastry grid cells - so a
     * synastry cell about two people rendered under the heading &quot;Transiting Mars square
     * natal Venus&quot;.
     *
     * <b>These are framing, deliberately, and not a second cookbook.</b> A full composite
     * dataset is 29 bodies by 12 signs by 12 houses and a synastry one is worse; what is
     * here says what each factor MEANS when the chart is a relationship, and the existing
     * sign and house prose still supplies the character of the placement underneath it. Two
     * layers, one of them new, rather than a duplicate of 3,000 entries that would drift.
     */
    private final Map<String, String> compositeBodies = new HashMap<>();
    private final Map<String, String> compositeHouses = new HashMap<>();
    private final Map<String, String> overlayHouses = new HashMap<>();
    private final Map<String, String> angleContacts = new HashMap<>();
    private final Map<String, String> synastryAspects = new HashMap<>();

    /**
     * Per-pair cross-chart readings: 756 of them, one per body pair per aspect.
     *
     * <b>Distinct from {@link #synastryAspects}, which is the eleven-entry framing.</b> That
     * one says what a square MEANS between two people; this one says what THIS pair&#39;s
     * square means. The names are close because the things are close, and the two are used
     * together - frame first, then the pair.
     *
     * <b>Unordered, because the source is.</b> The reading for A&#39;s Saturn on B&#39;s Sun
     * and B&#39;s Saturn on A&#39;s Sun is the same entry, which is a real limitation of the
     * data rather than of the lookup: the tradition reads those two as different, and this
     * dataset does not distinguish them. The prose names the roles inside itself - &quot;the
     * Sun person&quot;, &quot;the Saturn person&quot; - so a reader can still tell which end
     * is which; nothing in the app can.
     */
    private final Map<String, String> synastryInteraspects = new HashMap<>();

    /**
     * Composite planet in composite house: the specific reading, keyed {@code sun_7}.
     *
     * <b>Houses, not signs, and that follows the technique rather than the dataset.</b> The
     * standard composite literature reads a composite planet by its HOUSE - the department of
     * the shared life it operates in - and treats the sign as secondary, because a composite
     * chart&#39;s signs are an artefact of two birth dates while its houses describe an actual
     * shared arrangement. So there is no composite planet-in-sign section, and the existing
     * natal sign prose continues to supply the character of the placement.
     *
     * <b>Twelve bodies.</b> Sun through Pluto, plus Chiron and the North Node. The South
     * Node, the asteroids, the angles and the calculated points are not covered and fall back
     * to the composite BODY framing in relationship_prose.json, which says what each of them
     * means in a relationship without claiming a house-by-house reading nobody has written.
     * SynastryCheck Part G asserts that every one of those fallbacks exists.
     */
    private final Map<String, String> compositePlanetHouses = new HashMap<>();
    /** Composite body in sign. Added 2026-08-30; the house map long predates it. */
    private final Map<String, String> compositePlanetSigns = new HashMap<>();
    /** Composite angle in sign, composite Sabian, composite transit. Added 2026-08-31. */
    private final Map<String, String> compositeAngles = new HashMap<>();
    /**
     * A body in a decan, and a body in a lunar mansion - natal and composite in one map
     * each, distinguished by the key prefix. Added 2026-08-31.
     *
     * <b>Both were already computed and neither was ever read.</b> The wheel has drawn a
     * decan ring since August and a mansion ring since 2026-08-23; clicking either told
     * you which one a body stood in and nothing about what that meant for the body.
     */
    private final Map<String, String> bodyDecans = new HashMap<>();
    private final Map<String, String> bodyMansions = new HashMap<>();
    private final Map<String, String> compositeSabian = new HashMap<>();
    private final Map<String, String> compositeTransits = new HashMap<>();
    /** Their body in your house, and their body on your angle. Added 2026-08-31. */
    private final Map<String, String> overlayPlanetHouses = new HashMap<>();
    private final Map<String, String> overlayPlanetAngles = new HashMap<>();

    /**
     * What an aspect means when both ends belong to the relationship rather than to a person.
     *
     * Eleven entries, one per {@code Aspects.Type}. The parallel to {@link #synastryAspects},
     * which does the same job for a contact BETWEEN two charts - and the distinction is the
     * whole point of having both. A synastry square is his function against her function; a
     * composite square is two functions of the bond against each other, and the pair will
     * distribute the ends between themselves without either of them owning one.
     */
    private final Map<String, String> compositeAspectFrames = new HashMap<>();

    /**
     * Composite aspect readings for a specific pair, keyed {@code mars_square_saturn}.
     *
     * <b>455 entries: 91 body pairs by the five Ptolemaic aspects.</b> Sun through Pluto plus
     * Chiron, the North Node, the Ascendant and the Midheaven. Anything outside that - the
     * minor aspects, the asteroids, the calculated points - falls back to the natal aspect
     * prose, which describes the same two principles in the same fixed relation and is a true
     * statement about a composite chart. What it cannot say is that both principles belong to
     * a relationship rather than to a person, and the frame above it says exactly that.
     *
     * <b>Its own file, and that is not incidental.</b> composite_aspects.json carries the
     * eleven frames; a generator pointed at that filename would take them out.
     */
    private final Map<String, String> compositeAspects = new HashMap<>();

    /**
     * Tarot for the bodies the Golden Dawn scheme never covered.
     *
     * <b>A separate section from {@code tarot_planets}, deliberately.</b> That one holds the
     * ten classical attributions - Sun to the Sun, Moon to the High Priestess, and so on - and
     * they are a closed, coherent system that nothing here should edit. The asteroids and
     * centaurs postdate it by two thousand years and have attributions of a different
     * character: some named in modern decks, some carried only by myth, one reasoned out.
     *
     * <b>Each value says which it is.</b> The trailing italic marks an entry Attested in a
     * deck, attested by myth, or Inferred - and the weakest of them says in its own text that
     * it was reasoned rather than sourced. This app now serves a great deal of prose with no
     * citation behind it; where a distinction was available it is kept rather than flattened.
     *
     * <b>These collide with tarot_planets and tarot_signs on purpose.</b> Ceres takes the
     * Empress that Venus holds, Pholus the Tower that Mars holds, Eros the Lovers that Gemini
     * holds. Shared resonance is ordinary in tarot; what changes is that the card-to-body map
     * is no longer one to one, and a chart can name the same card twice.
     */
    private final Map<String, String> tarotBodies = new HashMap<>();

    /** The map a section header names, or null when the line is not a known section. */
    private Map<String, String> sectionFor(String line) {
        if (line.startsWith("\"body_core\""))    return bodyCores;
        if (line.startsWith("\"planet_sign\""))  return planetSigns;
        if (line.startsWith("\"planet_house\"")) return planetHouses;
        if (line.startsWith("\"aspects\""))      return aspects;
        if (line.startsWith("\"transits\""))     return transits;
        if (line.startsWith("\"macro_dynamics\"")) return macroDynamics;
        if (line.startsWith("\"composite_body\""))  return compositeBodies;
        if (line.startsWith("\"composite_house\"")) return compositeHouses;
        if (line.startsWith("\"overlay_house\""))   return overlayHouses;
        if (line.startsWith("\"overlay_planet_house\"")) return overlayPlanetHouses;
        if (line.startsWith("\"overlay_planet_angle\"")) return overlayPlanetAngles;
        if (line.startsWith("\"angle_contact\""))   return angleContacts;
        if (line.startsWith("\"synastry_interaspect\"")) return synastryInteraspects;
        if (line.startsWith("\"composite_planet_house\"")) return compositePlanetHouses;
        if (line.startsWith("\"composite_planet_sign\"")) return compositePlanetSigns;
        if (line.startsWith("\"composite_angle\"")) return compositeAngles;
        if (line.startsWith("\"composite_sabian\"")) return compositeSabian;
        if (line.startsWith("\"composite_transit_aspect\"")) return compositeTransits;
        if (line.startsWith("\"body_decan\"")) return bodyDecans;
        if (line.startsWith("\"body_mansion\"")) return bodyMansions;
        if (line.startsWith("\"tarot_body\"")) return tarotBodies;
        if (line.startsWith("\"composite_aspect_frame\"")) return compositeAspectFrames;
        if (line.startsWith("\"composite_aspect\"")) return compositeAspects;
        if (line.startsWith("\"synastry_aspect\"")) return synastryAspects;
        return null;
    }

    /** A quoted line that opens an object: a section header rather than an entry. */
    private static boolean isSectionHeader(String line) {
        return line.startsWith("\"") && line.endsWith("{");
    }

    /**
     * Whether a raw key actually landed in the named section's map.
     *
     * Exists for DataCheck. The dataset is loaded by a tolerant line parser that drops
     * entries without erroring, and it is now written by two agents editing different files,
     * so "the load succeeded" is not evidence that anything arrived. This lets a check
     * compare the keys in a file against the keys the service can actually serve, per
     * section, which is the comparison that has caught every silent loss here so far.
     *
     * @param section one of body_core, planet_sign, planet_house, aspects, transits
     */
    public boolean hasEntry(String section, String key) {
        Map<String, String> m = sectionFor("\"" + section + "\"");
        return m != null && m.containsKey(key);
    }

    public String getMacroDynamic(String key) {
        return macroDynamics.getOrDefault(key, "");
    }

    /**
     * The dataset key for a body's display name.
     *
     * Goes through the Bodies registry so that a multi-word name keys cleanly: "North Node"
     * becomes `north_node`, not `north node`, and "Part of Fortune" becomes `fortune`. The
     * ten classical planets are unaffected - their registry ids are already exactly the
     * lowercase names the existing 132 entries use - so this changes no existing lookup.
     * Falls back to lowercasing for anything not in the registry.
     */
    private static String bodyKey(String name) {
        com.zodiacomputing.ourania.astro.Bodies.Def d =
            com.zodiacomputing.ourania.astro.Bodies.byName(name);
        return d != null ? d.id : String.valueOf(name).toLowerCase();
    }

    /** The core description for a body, or "" when the dataset has none. */
    public String getBodyCore(String bodyName) {
        return bodyCores.getOrDefault(bodyKey(bodyName), "");
    }

    /** True when this body has sign-by-sign prose, whichever file supplied it. */
    public boolean hasSignProse(String bodyName) {
        return planetSigns.containsKey(bodyKey(bodyName) + "_aries");
    }

    public static InterpretationService getInstance() {
        if (instance == null) {
            instance = new InterpretationService();
        }
        return instance;
    }

    private void loadData() {
        try {
            File file = new File("src/main/resources/data/interpretations.json");
            if (!file.exists()) {
                System.err.println("Interpretations JSON not found!");
                return;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            Map<String, String> currentMap = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\"planet_sign\"")) {
                    currentMap = planetSigns;
                } else if (line.startsWith("\"planet_house\"")) {
                    currentMap = planetHouses;
                } else if (line.startsWith("\"aspects\"")) {
                    currentMap = aspects;
                } else if (line.startsWith("\"aspects\"")) {
                    currentMap = aspects;
                } else if (line.startsWith("\"transits\"")) {
                    currentMap = transits;
                } else if (line.startsWith("\"signs\"")) {
                    currentMap = signs;
                } else if (line.startsWith("\"houses\"")) {
                    currentMap = houses;
                } else if (line.startsWith("\"decans\"")) {
                    currentMap = decans;
                } else if (line.startsWith("\"sabian\"")) {
                    currentMap = sabian;
                } else if (line.startsWith("\"tarot_planets\"")) {
                    currentMap = tarotPlanets;
                } else if (line.startsWith("\"tarot_signs\"")) {
                    currentMap = tarotSigns;
                } else if (line.startsWith("\"tarot_decans\"")) {
                    currentMap = tarotDecans;
                } else if (line.startsWith("\"transit_house\"")) {
                    currentMap = transitHouses;
                } else if (line.startsWith("\"") && currentMap != null) {
                    int colonIdx = line.indexOf("\":");
                    if (colonIdx != -1) {
                        String key = line.substring(1, colonIdx);
                        int valStart = line.indexOf("\"", colonIdx + 1);
                        int valEnd = line.lastIndexOf("\"");
                        if (valStart != -1 && valEnd > valStart) {
                            String value = unescapeUnicode(line.substring(valStart + 1, valEnd));
                            currentMap.put(key, value);
                        }
                    }
                }
            }
            reader.close();
            System.out.println("Loaded interpretations: " + planetSigns.size() + " signs, " + planetHouses.size() + " houses, " + aspects.size() + " aspects.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDegreesData() {
        try {
            File file = new File("src/main/resources/data/degree_interpretations.json");
            if (!file.exists()) {
                System.err.println("Degree Interpretations JSON not found!");
                return;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            String currentDegree = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\"") && (line.endsWith(":{") || line.endsWith(": {"))) {
                    int quoteEnd = line.indexOf("\"", 1);
                    if (quoteEnd != -1) {
                        currentDegree = line.substring(1, quoteEnd);
                    }
                } else if (currentDegree != null) {
                    if (line.startsWith("\"summary\":")) {
                        int start = line.indexOf("\"", 10) + 1;
                        int end = line.lastIndexOf("\"");
                        if (start > 0 && end > start) {
                            degreeSummaries.put(currentDegree, line.substring(start, end).replace("\\\"", "\""));
                        }
                    } else if (line.startsWith("\"fullText\":")) {
                        int start = line.indexOf("\"", 11) + 1;
                        int end = line.lastIndexOf("\"");
                        if (start > 0 && end > start) {
                            degreeFullTexts.put(currentDegree, line.substring(start, end).replace("\\\"", "\""));
                        }
                    }
                }
            }
            reader.close();
            System.out.println("Loaded degree interpretations: " + degreeSummaries.size() + " summaries.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAnglesData() {
        try {
            File[] files = {new File("src/main/resources/data/Ascendent.json"),
                            new File("src/main/resources/data/ic.json"),
                            new File("src/main/resources/data/mc.json"),
                            new File("src/main/resources/data/Descendant.json")};
            for (File file : files) {
                if (!file.exists()) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                String line;
                String currentKey = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("\"") && (line.endsWith(":{") || line.endsWith(": {"))) {
                        int quoteEnd = line.indexOf("\"", 1);
                        if (quoteEnd != -1) {
                            currentKey = line.substring(1, quoteEnd).toLowerCase();
                        }
                    } else if (currentKey != null) {
                        if (line.startsWith("\"fullText\":")) {
                            int start = line.indexOf("\"", 11) + 1;
                            int end = line.lastIndexOf("\"");
                            if (start > 0 && end > start) {
                                String text = line.substring(start, end).replace("\\\"", "\"");
                                // Descendant FIRST, and that order is load-bearing: it is the
                                // only key here that contains another angle's name as a
                                // substring, so a looser test above it would swallow it.
                                if (currentKey.startsWith("descendant") || currentKey.endsWith("descendant")) {
                                    descendantData.put(currentKey, text);
                                } else if (currentKey.startsWith("ascendant") || currentKey.endsWith("ascendant")) {
                                    ascendantData.put(currentKey, text);
                                } else if (currentKey.startsWith("mc") || currentKey.endsWith("mc")) {
                                    mcData.put(currentKey, text);
                                } else if (currentKey.startsWith("ic") || currentKey.endsWith("ic")) {
                                    icData.put(currentKey, text);
                                }
                            }
                        }
                    }
                }
                reader.close();
            }
            System.out.println("Loaded angles: " + ascendantData.size() + " Ascendant, "
                + icData.size() + " IC, " + mcData.size() + " MC.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the expanded Sabian data (fullText, shadow, keywords). The file is written
     * with one field per line and arrays inline so this line-oriented reader can parse it.
     * Absence of the file is non-fatal: the getters simply return empty strings.
     */
    private void loadSabianDetailData() {
        try {
            File file = new File("src/main/resources/data/Sabian_interpretations.json");
            if (!file.exists()) {
                System.err.println("Sabian detail JSON not found - shadow/keywords unavailable.");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            String currentKey = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\"") && (line.endsWith(":{") || line.endsWith(": {"))) {
                    int quoteEnd = line.indexOf("\"", 1);
                    if (quoteEnd != -1) {
                        currentKey = line.substring(1, quoteEnd).toLowerCase();
                    }
                } else if (currentKey != null) {
                    if (line.startsWith("\"fullText\":")) {
                        String v = extractQuotedValue(line, 11);
                        if (v != null) sabianFullTexts.put(currentKey, v);
                    } else if (line.startsWith("\"shadow\":")) {
                        String v = extractQuotedValue(line, 9);
                        if (v != null) sabianShadows.put(currentKey, v);
                    } else if (line.startsWith("\"keywords\":")) {
                        int start = line.indexOf("[");
                        int end = line.lastIndexOf("]");
                        if (start != -1 && end > start) {
                            String inner = line.substring(start + 1, end).replace("\"", "").trim();
                            if (!inner.isEmpty()) {
                                sabianKeywords.put(currentKey, inner.replaceAll("\\s*,\\s*", ", "));
                            }
                        }
                    }
                }
            }
            reader.close();
            System.out.println("Loaded Sabian detail: " + sabianFullTexts.size() + " fullText, "
                + sabianShadows.size() + " shadow, " + sabianKeywords.size() + " keyword sets.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Extracts a JSON string value beginning after fieldOffset, unescaping inner quotes. */
    private String extractQuotedValue(String line, int fieldOffset) {
        int start = line.indexOf("\"", fieldOffset) + 1;
        int end = line.lastIndexOf("\"");
        if (start > 0 && end > start) {
            return line.substring(start, end).replace("\\\"", "\"");
        }
        return null;
    }

    /**
     * What a body means when the chart is a relationship rather than a person.
     *
     * Keyed by the {@link com.zodiacomputing.ourania.astro.Bodies} id - the same stable key
     * settings.properties uses - so a body cannot be renamed on screen and lose its prose.
     * Returns null rather than a not-found string: the caller decides whether to omit the
     * section or fall back, and a composite reading with a missing frame should show the
     * natal prose alone rather than an apology.
     */
    public String getCompositeBody(String bodyId) {
        return bodyId == null ? null : compositeBodies.get(bodyId.toLowerCase());
    }

    /** What a composite house is the department of, for a pair. 1..12, else null. */
    public String getCompositeHouse(int house) {
        return house < 1 || house > 12 ? null : compositeHouses.get(String.valueOf(house));
    }

    /** One person&#39;s bodies landing in another&#39;s house. 1..12, else null. */
    /**
     * Their body in your house, or null.
     *
     * <b>Specific where {@link #getOverlayHouse} is generic.</b> That one returns the same
     * paragraph for every body landing in a house - their Mars and their Neptune in your
     * seventh read identically. This is the per-body entry; callers should try it first and
     * fall back to the generic one, the same shape as composite placements.
     */
    public String getOverlayPlanetHouse(String bodyName, int house) {
        if (bodyName == null || house < 1 || house > 12) {
            return null;
        }
        return overlayPlanetHouses.get(bodyKey(bodyName) + "_" + house);
    }

    /** Their body on your angle, or null. Keys are {body}_{angle}, both lowercase. */
    public String getOverlayPlanetAngle(String bodyName, String angle) {
        if (bodyName == null || angle == null) {
            return null;
        }
        return overlayPlanetAngles.get(bodyKey(bodyName) + "_" + bodyKey(angle));
    }

    public String getOverlayHouse(int house) {
        return house < 1 || house > 12 ? null : overlayHouses.get(String.valueOf(house));
    }

    /** A body of one chart sitting on an angle of the other. Keyed by angle id. */
    public String getAngleContact(String angleId) {
        return angleId == null ? null : angleContacts.get(angleId.toLowerCase());
    }

    /**
     * What an aspect means when it falls between two people&#39;s charts.
     *
     * <b>The frame only.</b> The two bodies involved are described by the ordinary natal
     * aspect prose, which reads the pair of energies rather than a moment in time and is
     * therefore correct here - unlike the transit prose the synastry grid used to serve,
     * which describes something passing.
     */
    public String getSynastryAspect(String aspectLabel) {
        return aspectLabel == null ? null : synastryAspects.get(aspectLabel.toLowerCase());
    }

    /**
     * The reading for one body pair in one aspect, across two charts, or null.
     *
     * <b>Both orders are tried, exactly as {@link #getAspect} does.</b> The dataset stores one
     * entry per unordered pair, so Sun-conjunction-Moon is filed under whichever way round it
     * was written and the caller must not have to know which.
     *
     * Returns null rather than a not-found string. The caller falls back to the natal aspect
     * reading, which is a real reading of the same two energies - so a pair this dataset does
     * not cover degrades to something correct rather than to an apology.
     */
    /**
     * The composite reading for one planet in one composite house, or null.
     *
     * Null rather than a not-found string, on the same terms as the rest of this group: the
     * caller falls back to the composite BODY framing, which is written for all 29 registry
     * points, so an uncovered body degrades to a shorter true statement rather than to an
     * apology in a highlighted box.
     */
    /** What this aspect means inside a composite chart, or null. One per aspect type. */
    /**
     * The tarot entry for a body outside the classical ten, or null.
     *
     * Keyed by {@link com.zodiacomputing.ourania.astro.Bodies} id. Null rather than
     * "Unknown": the caller has a real card from {@code tarot_planets} to prefer, and only
     * reaches here when there is none.
     */
    public String getTarotBody(String bodyId) {
        return bodyId == null ? null : tarotBodies.get(bodyId.toLowerCase());
    }

    public String getCompositeAspectFrame(String aspectLabel) {
        return aspectLabel == null ? null : compositeAspectFrames.get(aspectLabel.toLowerCase());
    }

    /**
     * The composite reading for one specific pair in one aspect, or null.
     *
     * Both orders tried, as {@link #getAspect} and {@link #getSynastryInteraspect} do - a
     * composite Mars-Saturn conjunction is the same configuration whichever way the grid
     * happens to name it, and unlike synastry there is no second person for the order to
     * mean anything about.
     */
    public String getCompositeAspect(String body1, String body2, String aspect) {
        if (body1 == null || body2 == null || aspect == null) {
            return null;
        }
        String a = aspect.toLowerCase();
        String hit = compositeAspects.get(bodyKey(body1) + "_" + a + "_" + bodyKey(body2));
        return hit != null ? hit : compositeAspects.get(bodyKey(body2) + "_" + a + "_" + bodyKey(body1));
    }

    /**
     * Composite body in sign, or null.
     *
     * <b>Mirrors {@link #getCompositePlanetHouse}</b> - same bodyKey normalisation, so a
     * caller may pass a display name or a registry id. Sign names are lowercase, matching
     * {@code Zodiac.signName}, which is what the JSON keys use.
     */
    public String getCompositePlanetSign(String bodyName, String sign) {
        if (bodyName == null || sign == null) {
            return null;
        }
        return compositePlanetSigns.get(bodyKey(bodyName) + "_" + sign.toLowerCase());
    }

    /** Composite angle in sign, or null. Keys are {angle}_{sign}, both lowercase. */
    public String getCompositeAngle(String angleName, String sign) {
        if (angleName == null || sign == null) {
            return null;
        }
        return compositeAngles.get(bodyKey(angleName) + "_" + sign.toLowerCase());
    }

    /** Composite Sabian symbol, or null. Keys are {sign}_{degree}, degree 1-30. */
    public String getCompositeSabian(String signName, int degree) {
        if (signName == null || degree < 1 || degree > 30) {
            return null;
        }
        return compositeSabian.get(signName.toLowerCase() + "_" + degree);
    }

    /**
     * A transit to a composite point, or null.
     *
     * <b>Ordered, unlike the composite aspect getter.</b> Transiting Mars to a composite Venus
     * is not the same event as transiting Venus to a composite Mars, so there is no reversed
     * lookup here. The natal transit map keys on {@code transit_a_aspect_natal_b}; this one
     * omits the "natal" segment because the target is a composite point, not a natal one.
     */
    public String getCompositeTransitAspect(String transiting, String target, String aspect) {
        if (transiting == null || target == null || aspect == null) {
            return null;
        }
        return compositeTransits.get("transit_" + bodyKey(transiting) + "_"
            + aspect.toLowerCase() + "_" + bodyKey(target));
    }

    public String getCompositePlanetHouse(String bodyName, int house) {
        if (bodyName == null || house < 1 || house > 12) {
            return null;
        }
        return compositePlanetHouses.get(bodyKey(bodyName) + "_" + house);
    }

    public String getSynastryInteraspect(String body1, String body2, String aspect) {
        if (body1 == null || body2 == null || aspect == null) {
            return null;
        }
        String a = aspect.toLowerCase();
        String k1 = bodyKey(body1) + "_" + a + "_" + bodyKey(body2);
        String k2 = bodyKey(body2) + "_" + a + "_" + bodyKey(body1);
        String hit = synastryInteraspects.get(k1);
        return hit != null ? hit : synastryInteraspects.get(k2);
    }

    public String getPlanetInSign(String planet, String sign) {
        String key = bodyKey(planet) + "_" + sign.toLowerCase();
        return planetSigns.getOrDefault(key, "Interpretation not found for " + planet + " in " + sign + ". Please add to JSON.");
    }

    public String getPlanetInHouse(String planet, int house) {
        String key = bodyKey(planet) + "_" + house;
        return planetHouses.getOrDefault(key, "Interpretation not found for " + planet + " in House " + house + ". Please add to JSON.");
    }

    /**
     * The general meaning of an aspect, with no bodies attached, or null.
     *
     * The three-argument getAspect already falls back to this when a pair has no specific
     * reading; this exposes it directly for the aspect index, where there are no bodies to
     * name and the general text IS the entry rather than a substitute for one.
     */
    public String getAspectGeneral(String aspectLabel) {
        return aspectLabel == null ? null : aspects.get(aspectLabel.toLowerCase());
    }

    public String getAspect(String planet1, String planet2, String aspect) {
        String p1 = bodyKey(planet1);
        String p2 = bodyKey(planet2);
        String a = aspect.toLowerCase();
        
        String specificKey1 = p1 + "_" + a + "_" + p2;
        String specificKey2 = p2 + "_" + a + "_" + p1;
        
        if (aspects.containsKey(specificKey1)) return aspects.get(specificKey1);
        if (aspects.containsKey(specificKey2)) return aspects.get(specificKey2);
        
        // No reading for this pair. Fall back to the aspect's own meaning if one is written.
        //
        // <b>This became the common path on 2026-08-23</b>, when five minor aspects were added
        // to the engine. Five types across 406 body pairs is about 2,030 readings that do not
        // exist yet, and without this every one of those cells would have said "Interpretation
        // not found" - the dead-cell defect the aspect grid had already been fixed for once.
        // The old wording here appended "(Specific text ... not found)", which reads as a
        // failure rather than as an honest gap.
        if (aspects.containsKey(a)) {
            return aspects.get(a)
                + "<br><br><i>This is the general meaning of the " + aspect.toLowerCase()
                + ". A reading specific to " + planet1 + " and " + planet2
                + " is not written yet.</i>";
        }

        return "Interpretation not found for " + aspect;
    }

    /**
     * A body in its decan, or null. Natal and composite prose are different entries.
     *
     * The decan number is 1-3 and comes from the caller, which already computed it to draw
     * the glyph - it is not re-derived here.
     */
    public String getBodyDecan(String bodyName, String sign, int decan, boolean relationship) {
        if (bodyName == null || sign == null) {
            return null;
        }
        ensureLazy("body_decan");
        return bodyDecans.get((relationship ? "composite_" : "natal_") + bodyKey(bodyName)
            + "_" + sign.toLowerCase() + "_decan_" + decan);
    }

    /**
     * A body in its lunar mansion, or null. Natal and composite prose are different entries.
     *
     * <b>The mansion must be resolved from a longitude, not from a sign and a degree.</b>
     * A mansion is 12.857 degrees wide and does not align to sign boundaries, so a whole
     * degree rounded out of a sign position can fall in the neighbouring mansion - which is
     * the boundary bug LunarMansionCheck caught on its first run. Callers pass the number
     * they got from LunarMansions.at().
     */
    public String getBodyMansion(String bodyName, int mansion, boolean relationship) {
        if (bodyName == null) {
            return null;
        }
        ensureLazy("body_mansion");
        return bodyMansions.get((relationship ? "composite_" : "natal_") + bodyKey(bodyName)
            + "_mansion_" + mansion);
    }

    public String getSign(String signName) {
        String key = signName.toLowerCase();
        return signs.getOrDefault(key, "General meaning for " + signName + " goes here. (Add to JSON)");
    }

    public String getHouse(int houseNum) {
        String key = String.valueOf(houseNum);
        return houses.getOrDefault(key, "General meaning for House " + houseNum + " goes here. (Add to JSON)");
    }

    public String getDecan(String signName, int decanNum) {
        String key = signName.toLowerCase() + "_" + decanNum;
        if (decans.containsKey(key)) {
            return decans.get(key);
        }
        return "Interpretation not found for " + signName + " Decan " + decanNum;
    }

    public String getSabianSymbol(String signName, int degree) {
        String key = signName.toLowerCase() + "_" + degree;
        if (sabian.containsKey(key)) {
            return sabian.get(key);
        }
        return "Sabian symbol not found for " + signName + " " + degree;
    }

    public String getSabianFullText(String signName, int degree) {
        return sabianFullTexts.getOrDefault(signName.toLowerCase() + "_" + degree, "");
    }

    public String getSabianShadow(String signName, int degree) {
        return sabianShadows.getOrDefault(signName.toLowerCase() + "_" + degree, "");
    }

    public String getSabianKeywords(String signName, int degree) {
        return sabianKeywords.getOrDefault(signName.toLowerCase() + "_" + degree, "");
    }

    public String getDegreeSummary(String signName, int degree) {
        String key = signName.toLowerCase() + "_" + degree;
        if (degreeSummaries.isEmpty()) {
            return "";
        }
        return degreeSummaries.getOrDefault(key, "");
    }
    
    public String getDegreeFullText(String signName, int degree) {
        String key = signName.toLowerCase() + "_" + degree;
        if (degreeFullTexts.isEmpty()) {
            return "";
        }
        return degreeFullTexts.getOrDefault(key, "");
    }
    public String getTarotPlanetCard(String planet) {
        String key = planet.toLowerCase();
        return tarotPlanets.getOrDefault(key, "Unknown");
    }

    public String getTarotSignCard(String signName) {
        String key = signName.toLowerCase();
        return tarotSigns.getOrDefault(key, "Unknown");
    }

    public String getTarotDecanCard(String signName, int decanNum) {
        String key = signName.toLowerCase() + "_" + decanNum;
        return tarotDecans.getOrDefault(key, "Unknown");
    }

    public String getTransitInHouse(String planet, int house) {
        String key = "transit_house_" + house;
        return transitHouses.getOrDefault(key, "Interpretation not found for transiting house " + house);
    }

    public String getTransit(String planet, String sign) {
        String key = "transit_" + planet.toLowerCase() + "_in_" + sign.toLowerCase();
        return transits.getOrDefault(key, "Interpretation not found for " + planet + " in " + sign);
    }

    public String getTransitAspect(String planet1, String planet2, String aspect) {
        String key = "transit_" + bodyKey(planet1) + "_" + aspect.toLowerCase() + "_natal_" + bodyKey(planet2);
        if (transits.containsKey(key)) {
            return transits.get(key);
        }
        
        // Try the general natal aspect interpretation as fallback
        String natalKey1 = bodyKey(planet1) + "_" + aspect.toLowerCase() + "_" + bodyKey(planet2);
        String natalKey2 = bodyKey(planet2) + "_" + aspect.toLowerCase() + "_" + bodyKey(planet1);
        if (aspects.containsKey(natalKey1)) {
            return "<i>(Transit triggering natal aspect meaning)</i><br>" + aspects.get(natalKey1);
        }
        if (aspects.containsKey(natalKey2)) {
            return "<i>(Transit triggering natal aspect meaning)</i><br>" + aspects.get(natalKey2);
        }
        
        // Final fallback to general aspect
        String a = aspect.toLowerCase();
        if (aspects.containsKey(a)) {
            return "<i>(General transit meaning)</i><br>When transiting " + planet1 + " forms a " + aspect + " to your natal " + planet2 + ", it triggers the general themes of this aspect.<br><br><b>General " + aspect + " meaning:</b> " + aspects.get(a) + " (Specific text for " + planet1 + " and " + planet2 + " not found)";
        }
        
        return "Transit aspect interpretation not found for " + planet1 + " " + aspect + " " + planet2;
    }

    public String getAngleInterpretation(String angleName, String sign) {
        String key = bodyKey(angleName) + "_" + sign.toLowerCase();
        if (angleName.equalsIgnoreCase("Ascendant")) {
            return ascendantData.getOrDefault(key, "Interpretation not found for Ascendant in " + sign + ".");
        } else if (angleName.equalsIgnoreCase("IC")) {
            return icData.getOrDefault(key, "Interpretation not found for IC in " + sign + ".");
        } else if (angleName.equalsIgnoreCase("MC") || angleName.equalsIgnoreCase("Midheaven")) {
            return mcData.getOrDefault(key, "Interpretation not found for MC in " + sign + ".");
        } else if (angleName.equalsIgnoreCase("Descendant")) {
            return descendantData.getOrDefault(key, "Interpretation not found for Descendant in " + sign + ".");
        }
        return "";
    }

    /**
     * The angle-file maps, for the check that proves they were actually populated.
     *
     * `Descendant.json` was written, added to the loader's file list, and then dropped entirely
     * on load: the routing had branches for ascendant, mc and ic only, so every
     * `descendant_*` key fell through all three and went nowhere. Nothing looked wrong, because
     * the panel falls back to the registry one-liner and that reads like a designed fallback.
     * The angle files had no check at all, which is why a whole file could be inert.
     *
     * @param angle one of ascendant, descendant, mc, ic
     */
    public int angleEntryCount(String angle) {
        switch (angle.toLowerCase()) {
            case "ascendant":  return ascendantData.size();
            case "descendant": return descendantData.size();
            case "mc":         return mcData.size();
            case "ic":         return icData.size();
            default:           return -1;
        }
    }

    public String getAngleAspectInterpretation(String planetName, String aspect, String angleName) {
        String key = planetName.toLowerCase() + "_" + aspect.toLowerCase() + "_" + angleName.toLowerCase();
        String result = "";
        if (angleName.equalsIgnoreCase("Ascendant")) {
            result = ascendantData.getOrDefault(key, "");
        } else if (angleName.equalsIgnoreCase("IC")) {
            result = icData.getOrDefault(key, "");
        } else if (angleName.equalsIgnoreCase("MC") || angleName.equalsIgnoreCase("Midheaven")) {
            result = mcData.getOrDefault(key, "");
        }
        
        if (result != null && !result.isEmpty()) return result;
        
        String fallback = getAspect(planetName, angleName, aspect);
        if (!fallback.startsWith("Interpretation not found for")) {
            return fallback;
        }
        
        return "Aspect interpretation not found for " + planetName + " " + aspect + " " + angleName + ".";
    }
}

