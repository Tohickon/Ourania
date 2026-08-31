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
     * Both are now closed: the aspect loop uses putIfAbsent, and main() refuses to run
     * without an explicit acknowledgement argument. The file is kept because it is the
     * only record of where the boilerplate came from.
     */
    public static final String ACK = "--yes-i-want-to-rewrite-extra-bodies-json";

    public static void main(String[] args) throws Exception {
        boolean acked = false;
        for (String a : args) {
            if (ACK.equals(a)) acked = true;
        }
        if (!acked) {
            System.err.println("GenerateExtraBodies is retired - it rewrites extra_bodies.json");
            System.err.println("wholesale from templates and has destroyed hand-written prose before.");
            System.err.println("Re-run with " + ACK + " only if that is genuinely what you want.");
            return;
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

        // Load existing data
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        data.put("body_core", new LinkedHashMap<>());
        data.put("aspects", new LinkedHashMap<>());
        data.put("transits", new LinkedHashMap<>());
        
        try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/data/extra_bodies.json"))) {
            Pattern sectionStart = Pattern.compile("^\\s*\"([a-z_0-9]+)\"\\s*:\\s*\\{\\s*$");
            Pattern keyLine = Pattern.compile("^\\s*\"([a-z_0-9]+)\"\\s*:\\s*\"(.*)\"[,]?\\s*$");
            
            String currentSection = null;
            String line;
            while ((line = br.readLine()) != null) {
                Matcher sm = sectionStart.matcher(line);
                if (sm.find()) {
                    currentSection = sm.group(1);
                    if (!data.containsKey(currentSection)) {
                        data.put(currentSection, new LinkedHashMap<>());
                    }
                    continue;
                }
                
                Matcher km = keyLine.matcher(line);
                if (km.find() && currentSection != null) {
                    data.get(currentSection).put(km.group(1), km.group(2));
                }
            }
        } catch (Exception e) {
            System.out.println("Could not read existing file, generating from scratch.");
        }

        // Generate and merge Body Cores
        Map<String, String> bodyCore = data.get("body_core");
        bodyCore.put("ceres", "<b>Ceres — The Great Mother.</b><br><br>Ceres rules the harvest: unconditional love, sustenance, grief and loss, and the terms on which you were fed. She is the archetype of nurture, and she governs both how you care for others and what you require in order to feel cared for. Because her myth is a myth of abduction and return, she also rules the bargain struck around love — what is withheld, what is grieved, and what is grown back.");
        bodyCore.put("pallas", "<b>Pallas Athena — The Warrior Queen.</b><br><br>Pallas is creative intelligence: pattern recognition, strategy, and the capacity to fight for a belief without brute force. Where Mars charges, Pallas out-thinks. She governs your problem-solving style, the shape of your craft, and the arena in which you see the whole board while others see only their own piece. Born fully armed from Jupiter's head, she is intellect that arrives ready for use.");
        bodyCore.put("juno", "<b>Juno — The Divine Consort.</b><br><br>Venus is what attracts you; Juno is what you actually need in order to sustain a commitment. She governs marriage and long-term partnership of every kind, business included: the terms of the contract, the loyalty you expect, and the injustice you will not tolerate. Her myth is one of a binding vow repeatedly betrayed, so she also rules what you do when a promise is broken.");
        bodyCore.put("vesta", "<b>Vesta — The Keeper of the Flame.</b><br><br>Vesta is what you are devoted to. She is the archetype of the priestess: focus, sacred purpose, and the willingness to sacrifice the merely pleasant for the genuinely central. She governs how you concentrate, what restores you, and where you tend a flame that must not go out. Because devotion narrows, she also rules what you have set aside in order to keep it burning.");
        bodyCore.put("north_node", "<b>North Node — the karmic path forward.</b><br><br>The lunar nodes are not bodies but the two points where the Moon's orbit crosses the ecliptic, so they describe a direction rather than a drive. The North Node marks what your life is asking you to develop: the unfamiliar, initially uncomfortable capacity that growth runs through. Nothing here comes naturally, and that is the point — this is the work, not the talent. Read it always with the South Node opposite it.");
        bodyCore.put("south_node", "<b>South Node — innate talent, and the comfort zone to evolve beyond.</b><br><br>Exactly opposite the North Node, the South Node marks what you already have: the skills that arrived without effort, the responses you reach for under stress, and the place you retreat to when the growth ahead feels like too much. It is not a fault to be corrected. It is the ground you stand on, and the trap is standing on it exclusively when the North Node is asking you to move.");
        bodyCore.put("fortune", "<b>Part of Fortune — where success and joy land.</b><br><br>An Arabic lot rather than a body, calculated from the Sun, the Moon and the Ascendant, and reversed by sect — the formula runs one way for a day chart and the other for a night chart. It marks where the three most personal factors in the chart agree, and so where worldly success, ease and genuine enjoyment are most available to you. Not luck exactly: the place where effort meets the least resistance.");
        bodyCore.put("lilith", "<b>Black Moon Lilith — the disowned and the uncompliant.</b><br><br>Not a body but the lunar apogee, the furthest point of the Moon's orbit from Earth, and a mathematical point rather than a rock. Lilith marks what you have exiled: the desire judged unacceptable, the rage with nowhere to go, the refusal to be made convenient. Where she falls, you were told to be smaller and something in you declined. Handled badly she is compulsion and sabotage; handled well she is the part of you that cannot be bought.");
        
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
        transits.put("transit_sun_quincunx_natal_south_node", "<b>Sun Quincunx South Node</b><br><i>Tension between current purpose and old defaults.</i><br><br>When the transiting Sun forms a quincunx to your natal South Node, there is a temporary but distinct tension between where you are being called to shine today, and the comfortable, habitual patterns of your past. You may feel an awkward adjustment is required to integrate your current sense of purpose with old karmic defaults.");
        transits.put("transit_sun_square_natal_chiron", "<b>Sun Square Chiron</b><br><i>A brief illumination of an old wound.</i><br><br>A transit of the Sun squaring natal Chiron often brings a brief but sharp illumination to an old wound. For a day or two, circumstances or interactions might poke at a sensitive vulnerability or insecurity. The square demands action: you are challenged to consciously integrate this awareness rather than react from a place of hurt.");
        transits.put("transit_sun_square_natal_ceres", "<b>Sun Square Ceres</b><br><i>Friction between ego and nurturing.</i><br><br>When the transiting Sun squares your natal Ceres, issues around nurturing, caretaking, and sustenance may temporarily clash with your ego needs or vitality. You might feel a friction between what you want to do for yourself and the responsibilities of caring for others (or your need to be cared for).");
        transits.put("transit_sun_sextile_natal_pallas", "<b>Sun Sextile Pallas</b><br><i>A smooth flow of strategic thinking.</i><br><br>The transiting Sun sextiling natal Pallas offers a smooth, cooperative flow of creative intelligence and strategic thinking. This is an excellent couple of days for problem-solving, planning, and seeing the &quot;big picture&quot; patterns with clarity. Your vitality supports your wisdom.");
        transits.put("transit_sun_trine_natal_juno", "<b>Sun Trine Juno</b><br><i>Harmonious energy in committed partnerships.</i><br><br>A trine from the transiting Sun to your natal Juno brings a harmonious, supportive energy to your committed relationships and partnerships. It's a favorable time for cooperation, mutual appreciation, and aligning your personal goals with the needs of a significant other.");
        transits.put("transit_sun_opposition_natal_vesta", "<b>Sun Opposition Vesta</b><br><i>Tug-of-war between external drive and inner devotion.</i><br><br>As the transiting Sun opposes your natal Vesta, you may experience a tug-of-war between your external, ego-driven activities and your internal need for focus, devotion, and solitary dedication. Finding a balance between shining out in the world and tending your inner sacred flame is the task at hand.");
        transits.put("transit_sun_conjunction_natal_part_of_fortune", "<b>Sun Conjunction Part of Fortune</b><br><i>Illumination of joy and prosperity.</i><br><br>When the transiting Sun conjuncts your natal Part of Fortune, it briefly illuminates your personal point of joy, prosperity, and natural alignment. You may feel a heightened sense of well-being, serendipity, or a clearer understanding of what truly brings you fulfillment.");

        // Write out merged JSON
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        boolean firstSection = true;
        for (String section : data.keySet()) {
            if (!firstSection) json.append(",\n");
            json.append("  \"").append(section).append("\": {\n");
            Map<String, String> sectionData = data.get(section);
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

        FileWriter fw = new FileWriter("src/main/resources/data/extra_bodies.json");
        fw.write(json.toString());
        fw.close();
        System.out.println("Successfully merged and generated data into extra_bodies.json");
    }
}
