package com.zodiacomputing.ourania.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateExtraBodies {
    /**
     * <b>This generator is retired, and running it is a data-loss event.</b>
     *
     * It emits 23 bodies x 22 partners x 6 aspects = 3,036 aspect keys from six
     * String.format templates and a one-line gloss per body, then <b>rewrites
     * extra_bodies.json wholesale</b>. Every one of those 3,036 values is boilerplate:
     * six closing sentences shared 154 ways each, saying nothing about the pair.
     *
     * Two separate hazards, both realised at least once:
     *   - The wholesale rewrite silently deleted 192 of the other agent's entries on
     *     2026-08-13. See Resources/wiki/two-agents-one-repo.md rule 1.
     *   - put() overwrote hand-written prose with template text. The Uranus, Neptune and
     *     Pluto splice of 2026-08-21 and all the asteroid prose would be lost on a re-run.
     *
     * Both are now closed, and closed structurally rather than by convention:
     *   - Nothing overwrites. Every write into the loaded data is putIfAbsent, including the
     *     body_core and transits blocks, which used plain put until 2026-09-02 and would have
     *     reverted any hand-edit to those eight bodies and seven transits on a re-run.
     *   - Nothing is dropped. A line the reader cannot classify aborts the run before any
     *     write, instead of being parsed away and then written back out of existence. That
     *     silent drop was the 2026-08-13 mechanism.
     *   - The output is proved a superset of the input before it is allowed near the file.
     *   - The previous file is copied to a timestamped sibling, and the new one is written to
     *     a temp file and moved into place, so an interrupted run cannot truncate the corpus.
     *
     * One live defect was found by that work and is fixed here: "planet_sign" and
     * "planet_house" are declared inline as empty objects, which matched neither the old
     * section pattern nor the old key pattern, so both were dropped on every rewrite. They are
     * live routing sections in InterpretationService.
     *
     * main() still refuses to run without an explicit acknowledgement argument. The file is
     * kept because it is the only record of where the boilerplate came from.
     */
    /**
     * What a run did. Returned rather than printed so a check suite can assert on the
     * outcome instead of scraping stdout, and so every abort path is distinguishable from
     * every other. {@link #WROTE} is the only value that means the file changed.
     */
    public enum Result { REFUSED_NO_ACK, NO_FILE, ABORTED_UNPARSED, ABORTED_WOULD_LOSE, WROTE }

    /** Overridable so a check can point a run at a scratch copy instead of the real corpus. */
    public static final String PATH_PROPERTY = "ourania.extraBodies.path";

    static java.io.File targetFile() {
        return new java.io.File(System.getProperty(PATH_PROPERTY,
                "src/main/resources/data/extra_bodies.json"));
    }

    public static final String ACK = "--yes-i-want-to-rewrite-extra-bodies-json";

    public static void main(String[] args) throws Exception {
        run(args);
    }

    public static Result run(String[] args) throws Exception {
        boolean acked = false;
        for (String a : args) {
            if (ACK.equals(a)) acked = true;
        }
        if (!acked) {
            System.err.println("GenerateExtraBodies is retired - it rewrites extra_bodies.json");
            System.err.println("wholesale from templates and has destroyed hand-written prose before.");
            System.err.println("Re-run with " + ACK + " only if that is genuinely what you want.");
            return Result.REFUSED_NO_ACK;
        }
        Map<String, String> themes = new LinkedHashMap<>();
        themes.put("sun", "core identity and vital energy");
        themes.put("moon", "emotional needs and unconscious responses");
        themes.put("mercury", "intellect and communication style");
        themes.put("venus", "values, relationships, and aesthetic tastes");
        themes.put("mars", "drive, ambition, and assertive energy");
        themes.put("jupiter", "capacity for growth, optimism, and expansion");
        themes.put("saturn", "sense of discipline, restriction, and maturity");
        themes.put("uranus", "urge for rebellion, innovation, and freedom");
        themes.put("neptune", "dreams, illusions, and spiritual empathy");
        themes.put("pluto", "deepest power, transformation, and primal intensity");
        themes.put("chiron", "deepest wound and potential for healing");
        themes.put("north_node", "soul's evolutionary path and future growth");
        themes.put("south_node", "past karmic patterns and innate comfort zones");
        themes.put("ceres", "capacity for nurturing and self-care");
        themes.put("pallas", "strategic intelligence and creative problem-solving");
        themes.put("juno", "approach to committed partnerships and loyalty");
        themes.put("vesta", "sense of devotion, focus, and sacred service");
        themes.put("ascendant", "outward personality and physical vitality");
        themes.put("descendant", "expectations of others and partnership dynamics");
        themes.put("mc", "public reputation, career, and life direction");
        themes.put("ic", "private life, roots, and emotional foundations");
        themes.put("fortune", "greatest source of joy, flow, and worldly success");
        themes.put("lilith", "raw, untamed nature and shadow desires");
        
        Map<String, String> names = new HashMap<>();
        for(String k : themes.keySet()) {
            String[] parts = k.split("_");
            StringBuilder sb = new StringBuilder();
            for(String p : parts) {
                if(p.equals("mc")) sb.append("MC ");
                else if(p.equals("ic")) sb.append("IC ");
                else sb.append(p.substring(0,1).toUpperCase()).append(p.substring(1)).append(" ");
            }
            if(k.equals("fortune")) names.put(k, "Part of Fortune");
            else if(k.equals("lilith")) names.put(k, "Black Moon Lilith");
            else names.put(k, sb.toString().trim());
        }

        String[] aspectNames = {"conjunction", "sextile", "square", "trine", "opposition", "quincunx"};
        String[] aspectTemplates = {
            "<b>%s Conjunction %s:</b> Your %s merges powerfully with your %s. This creates a fused, intense dynamic where these two areas of your life cannot operate independently.",
            "<b>%s Sextile %s:</b> Your %s harmonizes with your %s. This creates a supportive flow that encourages these two areas of your life.",
            "<b>%s Square %s:</b> Your %s creates tension and friction with your %s. This creates a challenging but highly motivating dynamic that demands action and resolution between these two areas of your life.",
            "<b>%s Trine %s:</b> Your %s flows effortlessly with your %s. This creates a natural talent and ease between these two areas of your life, though it can sometimes lead to complacency.",
            "<b>%s Opposition %s:</b> Your %s is polarized against your %s. This creates a tug-of-war dynamic where you must learn to balance and integrate these two competing areas of your life.",
            "<b>%s Quincunx %s:</b> Your %s is awkwardly misaligned with your %s. This creates a need for constant adjustment and compromise between these two areas of your life."
        };

        // Load existing data. Section order is taken from the file, not declared here, so a
        // section this generator has never heard of survives the round trip in its own place.
        Map<String, Map<String, String>> data = new LinkedHashMap<>();

        java.io.File target = targetFile();
        if (!target.isFile()) {
            System.err.println("ABORT: " + target.getPath() + " not found. Refusing to create it");
            System.err.println("from templates - a blank starting point is how the 2026-08-13 loss began.");
            return Result.NO_FILE;
        }

        // Every line the reader cannot classify. The old reader dropped these silently and then
        // rewrote the file without them, which is precisely the deletion mechanism. Now a single
        // unclassified line aborts the run before anything is written.
        java.util.List<String> unparsed = new java.util.ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(target))) {
            // Keys widened to [A-Za-z_0-9]. The old [a-z_0-9] silently skipped any key carrying a
            // capital, and a key this reader skips is a key the writer deletes.
            Pattern sectionStart = Pattern.compile("^\\s*\"([A-Za-z_0-9]+)\"\\s*:\\s*\\{\\s*$");
            Pattern sectionEmpty = Pattern.compile("^\\s*\"([A-Za-z_0-9]+)\"\\s*:\\s*\\{\\s*\\}\\s*,?\\s*$");
            Pattern keyLine = Pattern.compile("^\\s*\"([A-Za-z_0-9]+)\"\\s*:\\s*\"(.*)\"\\s*,?\\s*$");
            String currentSection = null;
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String t = line.trim();

                // An inline empty section - "planet_sign": {} - matched neither old pattern, so
                // planet_sign and planet_house were dropped on every rewrite. Both are live
                // routing sections in InterpretationService.
                Matcher em = sectionEmpty.matcher(line);
                if (em.matches()) {
                    data.computeIfAbsent(em.group(1), k -> new LinkedHashMap<>());
                    currentSection = null;
                    continue;
                }

                Matcher sm = sectionStart.matcher(line);
                if (sm.matches()) {
                    currentSection = sm.group(1);
                    data.computeIfAbsent(currentSection, k -> new LinkedHashMap<>());
                    continue;
                }

                Matcher km = keyLine.matcher(line);
                if (km.matches() && currentSection != null) {
                    data.get(currentSection).put(km.group(1), km.group(2));
                    continue;
                }

                // Structural punctuation carries no data and needs no preserving.
                if (t.isEmpty() || t.equals("{") || t.equals("}") || t.equals("},")
                        || t.equals("]") || t.equals("],")) {
                    continue;
                }

                unparsed.add("line " + lineNo + ": " + (t.length() > 90 ? t.substring(0, 90) + "..." : t));
            }
        }

        if (!unparsed.isEmpty()) {
            System.err.println("ABORT: " + unparsed.size() + " line(s) of extra_bodies.json were not");
            System.err.println("understood by this reader. Rewriting now would delete them. That is the");
            System.err.println("2026-08-13 failure exactly: parse what you understand, write back only that.");
            System.err.println("Nothing has been written. The offending lines:");
            for (int i = 0; i < Math.min(unparsed.size(), 20); i++) {
                System.err.println("  " + unparsed.get(i));
            }
            if (unparsed.size() > 20) System.err.println("  ... and " + (unparsed.size() - 20) + " more");
            return Result.ABORTED_UNPARSED;
        }

        // What the file held before this run touched anything - the yardstick the generated
        // output is measured against before it is allowed to replace the file.
        Map<String, java.util.Set<String>> beforeKeys = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> e : data.entrySet()) {
            beforeKeys.put(e.getKey(), new java.util.LinkedHashSet<>(e.getValue().keySet()));
        }

        for (String required : new String[] { "body_core", "aspects", "transits" }) {
            data.computeIfAbsent(required, k -> new LinkedHashMap<>());
        }

        // Generate and merge Body Cores
        Map<String, String> bodyCore = data.get("body_core");
        bodyCore.putIfAbsent("ceres", "<b>Ceres — The Great Mother.</b><br><br>Ceres rules the harvest: unconditional love, sustenance, grief and loss, and the terms on which you were fed. She is the archetype of nurture, and she governs both how you care for others and what you require in order to feel cared for. Because her myth is a myth of abduction and return, she also rules the bargain struck around love — what is withheld, what is grieved, and what is grown back.");
        bodyCore.putIfAbsent("pallas", "<b>Pallas Athena — The Warrior Queen.</b><br><br>Pallas is creative intelligence: pattern recognition, strategy, and the capacity to fight for a belief without brute force. Where Mars charges, Pallas out-thinks. She governs your problem-solving style, the shape of your craft, and the arena in which you see the whole board while others see only their own piece. Born fully armed from Jupiter's head, she is intellect that arrives ready for use.");
        bodyCore.putIfAbsent("juno", "<b>Juno — The Divine Consort.</b><br><br>Venus is what attracts you; Juno is what you actually need in order to sustain a commitment. She governs marriage and long-term partnership of every kind, business included: the terms of the contract, the loyalty you expect, and the injustice you will not tolerate. Her myth is one of a binding vow repeatedly betrayed, so she also rules what you do when a promise is broken.");
        bodyCore.putIfAbsent("vesta", "<b>Vesta — The Keeper of the Flame.</b><br><br>Vesta is what you are devoted to. She is the archetype of the priestess: focus, sacred purpose, and the willingness to sacrifice the merely pleasant for the genuinely central. She governs how you concentrate, what restores you, and where you tend a flame that must not go out. Because devotion narrows, she also rules what you have set aside in order to keep it burning.");
        bodyCore.putIfAbsent("north_node", "<b>North Node — the karmic path forward.</b><br><br>The lunar nodes are not bodies but the two points where the Moon's orbit crosses the ecliptic, so they describe a direction rather than a drive. The North Node marks what your life is asking you to develop: the unfamiliar, initially uncomfortable capacity that growth runs through. Nothing here comes naturally, and that is the point — this is the work, not the talent. Read it always with the South Node opposite it.");
        bodyCore.putIfAbsent("south_node", "<b>South Node — innate talent, and the comfort zone to evolve beyond.</b><br><br>Exactly opposite the North Node, the South Node marks what you already have: the skills that arrived without effort, the responses you reach for under stress, and the place you retreat to when the growth ahead feels like too much. It is not a fault to be corrected. It is the ground you stand on, and the trap is standing on it exclusively when the North Node is asking you to move.");
        bodyCore.putIfAbsent("fortune", "<b>Part of Fortune — where success and joy land.</b><br><br>An Arabic lot rather than a body, calculated from the Sun, the Moon and the Ascendant, and reversed by sect — the formula runs one way for a day chart and the other for a night chart. It marks where the three most personal factors in the chart agree, and so where worldly success, ease and genuine enjoyment are most available to you. Not luck exactly: the place where effort meets the least resistance.");
        bodyCore.putIfAbsent("lilith", "<b>Black Moon Lilith — the disowned and the uncompliant.</b><br><br>Not a body but the lunar apogee, the furthest point of the Moon's orbit from Earth, and a mathematical point rather than a rock. Lilith marks what you have exiled: the desire judged unacceptable, the rage with nowhere to go, the refusal to be made convenient. Where she falls, you were told to be smaller and something in you declined. Handled badly she is compulsion and sabotage; handled well she is the part of you that cannot be bought.");
        
        // Generate and merge Aspects
        Map<String, String> aspects = data.get("aspects");
        for (String b1 : themes.keySet()) {
            for (String b2 : themes.keySet()) {
                if (b1.equals(b2)) continue;
                for (int i=0; i<aspectNames.length; i++) {
                    String asp = aspectNames[i];
                    String key = b1 + "_" + asp + "_" + b2;
                    String text = String.format(aspectTemplates[i], names.get(b1), names.get(b2), themes.get(b1), themes.get(b2));
                    // putIfAbsent, never put: hand-written prose already in the file
                    // outranks anything this loop can produce.
                    aspects.putIfAbsent(key, text);
                }
            }
        }

        // Generate and merge Transits
        Map<String, String> transits = data.get("transits");
        transits.putIfAbsent("transit_sun_quincunx_natal_south_node", "<b>Sun Quincunx South Node</b><br><i>Tension between current purpose and old defaults.</i><br><br>When the transiting Sun forms a quincunx to your natal South Node, there is a temporary but distinct tension between where you are being called to shine today, and the comfortable, habitual patterns of your past. You may feel an awkward adjustment is required to integrate your current sense of purpose with old karmic defaults.");
        transits.putIfAbsent("transit_sun_square_natal_chiron", "<b>Sun Square Chiron</b><br><i>A brief illumination of an old wound.</i><br><br>A transit of the Sun squaring natal Chiron often brings a brief but sharp illumination to an old wound. For a day or two, circumstances or interactions might poke at a sensitive vulnerability or insecurity. The square demands action: you are challenged to consciously integrate this awareness rather than react from a place of hurt.");
        transits.putIfAbsent("transit_sun_square_natal_ceres", "<b>Sun Square Ceres</b><br><i>Friction between ego and nurturing.</i><br><br>When the transiting Sun squares your natal Ceres, issues around nurturing, caretaking, and sustenance may temporarily clash with your ego needs or vitality. You might feel a friction between what you want to do for yourself and the responsibilities of caring for others (or your need to be cared for).");
        transits.putIfAbsent("transit_sun_sextile_natal_pallas", "<b>Sun Sextile Pallas</b><br><i>A smooth flow of strategic thinking.</i><br><br>The transiting Sun sextiling natal Pallas offers a smooth, cooperative flow of creative intelligence and strategic thinking. This is an excellent couple of days for problem-solving, planning, and seeing the &quot;big picture&quot; patterns with clarity. Your vitality supports your wisdom.");
        transits.putIfAbsent("transit_sun_trine_natal_juno", "<b>Sun Trine Juno</b><br><i>Harmonious energy in committed partnerships.</i><br><br>A trine from the transiting Sun to your natal Juno brings a harmonious, supportive energy to your committed relationships and partnerships. It's a favorable time for cooperation, mutual appreciation, and aligning your personal goals with the needs of a significant other.");
        transits.putIfAbsent("transit_sun_opposition_natal_vesta", "<b>Sun Opposition Vesta</b><br><i>Tug-of-war between external drive and inner devotion.</i><br><br>As the transiting Sun opposes your natal Vesta, you may experience a tug-of-war between your external, ego-driven activities and your internal need for focus, devotion, and solitary dedication. Finding a balance between shining out in the world and tending your inner sacred flame is the task at hand.");
        transits.putIfAbsent("transit_sun_conjunction_natal_part_of_fortune", "<b>Sun Conjunction Part of Fortune</b><br><i>Illumination of joy and prosperity.</i><br><br>When the transiting Sun conjuncts your natal Part of Fortune, it briefly illuminates your personal point of joy, prosperity, and natural alignment. You may feel a heightened sense of well-being, serendipity, or a clearer understanding of what truly brings you fulfillment.");

        // Write out merged JSON
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        boolean firstSection = true;
        for (String section : data.keySet()) {
            if (!firstSection) json.append(",\n");
            Map<String, String> sectionData = data.get(section);
            // An empty section is emitted inline, the way the file already carries planet_sign
            // and planet_house, so the round trip is byte-comparable rather than merely valid.
            if (sectionData.isEmpty()) {
                json.append("  \"").append(section).append("\": {}");
                firstSection = false;
                continue;
            }
            json.append("  \"").append(section).append("\": {\n");
            boolean firstKey = true;
            for (Map.Entry<String, String> entry : sectionData.entrySet()) {
                if (!firstKey) json.append(",\n");
                json.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                firstKey = false;
            }
            json.append("\n  }");
            firstSection = false;
        }
        json.append("\n}\n");

        // Prove the output is a superset of the input before letting it near the file. This
        // generator has deleted 192 entries belonging to another agent once already; the rule
        // it broke is that a script editing a shared file must not remove what it did not
        // explicitly intend to remove. Verify, then write - never the other way round.
        java.util.List<String> lost = new java.util.ArrayList<>();
        for (Map.Entry<String, java.util.Set<String>> e : beforeKeys.entrySet()) {
            Map<String, String> now = data.get(e.getKey());
            if (now == null) {
                lost.add("whole section \"" + e.getKey() + "\" (" + e.getValue().size() + " entries)");
                continue;
            }
            for (String k : e.getValue()) {
                if (!now.containsKey(k)) lost.add(e.getKey() + "." + k);
            }
        }
        if (!lost.isEmpty()) {
            System.err.println("ABORT: the generated file would drop " + lost.size() + " thing(s) that");
            System.err.println("extra_bodies.json currently holds. Nothing has been written.");
            for (int i = 0; i < Math.min(lost.size(), 20); i++) System.err.println("  " + lost.get(i));
            if (lost.size() > 20) System.err.println("  ... and " + (lost.size() - 20) + " more");
            return Result.ABORTED_WOULD_LOSE;
        }

        // Keep the previous file. Recovery from the 2026-08-13 loss was impossible because no
        // copy existed anywhere; one timestamped sibling is the whole cost of never repeating it.
        java.io.File backup = new java.io.File(target.getParentFile(),
                "extra_bodies.json.bak-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss")
                        .format(new java.util.Date()));
        java.nio.file.Files.copy(target.toPath(), backup.toPath());

        // Write to a sibling and move it into place, so an interrupted run cannot leave a
        // half-written corpus behind.
        java.io.File tmp = new java.io.File(target.getParentFile(), "extra_bodies.json.tmp");
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write(json.toString());
        }
        java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        int total = 0;
        for (Map<String, String> m : data.values()) total += m.size();
        System.out.println("extra_bodies.json rewritten: " + data.size() + " sections, "
                + total + " entries, 0 lost.");
        System.out.println("Previous file kept at " + backup.getName());
        return Result.WROTE;
    }
}
