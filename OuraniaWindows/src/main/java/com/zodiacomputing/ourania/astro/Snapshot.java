package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * L9: the output tiers.
 *
 * The three tiers are three truncations of one ranked list, not three generators. That
 * is what makes the watch-face target tractable - it is this same engine with N = 1. If
 * a second, simpler summarizer ever gets written for the small format, the ranking in
 * L3 and L7 is not doing its job.
 *
 * Text here is generated from structure. The interpretation library is not wired in yet;
 * when it is, only phraseBody() needs to change - everything else is selection and
 * ordering, which is where it belongs.
 *
 * Two independent axes, deliberately not collapsed into one:
 *
 *   TIER     how much - oneLine, paragraph, report. Three truncations of one ranked list.
 *   REGISTER what vocabulary - PLAIN or TECHNICAL. Same facts, same claims, different words.
 *
 * A register is not a licence to say more. Both registers make exactly the same
 * assertions; TECHNICAL says "malefic contrary to sect" where PLAIN says "the chart's
 * hardest edge". Nothing here states a verdict about a person, and nothing claims one
 * placement mitigates another - the engine computes no such thing, and a sentence like
 * "Jupiter tempers the harder placements" would be invention dressed as a conclusion.
 */
public final class Snapshot {

    /** How many ranked bodies the paragraph tier mentions. */
    public static int paragraphBodies = 3;

    /**
     * How far below the leader a body can sit and still be called joint-loudest.
     *
     * Prominence is normalised per chart, so the leader is always 1.0 and this is a gap
     * in that scale. Measured across 288 charts (two days, 10-minute steps, two
     * locations) the 1st-to-2nd gap runs: p25 0.077, median 0.153, p75 0.292. Setting
     * the margin at 0.08 therefore hedges on roughly the closest quarter of charts and
     * names a single body in the other three.
     *
     * What this buys is continuity, NOT fewer changes - and the distinction matters
     * because the naive reading is wrong. Counting how often the named set changes at
     * all, a margin makes things worse, because bodies crossing the boundary become a
     * new source of change on top of the leader swapping:
     *
     *   margin   set changes   readings sharing no body   names one body
     *   0.00              66                        66             100%
     *   0.08             114                        22              73%
     *   0.15             136                         9              52%
     *
     * The middle column is the one a reader feels. At margin 0 every change is a wholly
     * different answer; at 0.08 two thirds of those become "the same body, plus or minus
     * a companion", which reads as the chart shifting rather than the engine guessing.
     */
    public static double dominanceMargin = 0.08;

    /** Never call more than this many bodies joint-loudest; past three it says nothing. */
    public static int maxLeadCluster = 3;

    /**
     * How tight an aspect must be, as a fraction of its orb, before the paragraph names
     * it. At 0.5 that is within half the orb - close enough to be doing something, rather
     * than the technically-in-orb pairs that clutter a reading without informing it.
     */
    public static double aspectMentionTightness = 0.5;

    /** How many justifying clauses a single body may carry before it starts to list. */
    public static int reasonsPerBody = 2;

    /**
     * How many of the three chart-level sentences - repeated theme, rulership structure,
     * contradiction - the paragraph may carry.
     *
     * These are the only unbounded part of the tier: the body sentences are already
     * capped by paragraphBodies and maxLeadCluster, but a chart that happened to have a
     * theme AND a dispositor loop AND a contradiction produced eight sentences, which is
     * the report tier under another name. Two keeps the tiers distinct while leaving room
     * for one themes statement plus one of a different kind.
     */
    public static int maxStructuralSentences = 2;

    /** Vocabulary, not content. See the class note. */
    public enum Register { PLAIN, TECHNICAL }

    /** Default for the paragraph and one-line tiers; report() forces TECHNICAL. */
    public static Register register = Register.PLAIN;

    private Snapshot() { }

    /**
     * What each angle governs.
     *
     * This is the single biggest reason generated text reads thinner than a person's:
     * BodyScore knows a body is 5 degrees off the Descendant, but nothing downstream
     * knew what a Descendant is for, so the reading could state the geometry and not
     * its meaning. Four rows close most of that gap.
     *
     * Deliberately phrased as angles rather than houses - the Descendant is only the
     * 7th cusp under quadrant systems, and the house system here is user-selectable.
     */
    private static String angleTopic(String angle) {
        if (angle == null) {
            return null;
        }
        switch (angle) {
            case "Ascendant":  return "the angle of the body and first impressions";
            case "Descendant": return "the angle of one-to-one dealings";
            case "MC":         return "the angle of public standing";
            case "IC":         return "the angle of roots and the private base";
            default:           return null;
        }
    }

    /**
     * ", 5° off the Descendant" or ", on the Descendant", or empty when out of orb.
     *
     * One definition, because oneLine and phraseBody both need it and each having its
     * own copy is how one of them ends up still saying "0° off" after the other is fixed.
     * Inside half a degree the rounded form reads "0° off the Descendant", which is a
     * fussy way of saying it is on it.
     */
    private static String anglePhrase(BodyScore.Vector v) {
        if (v.angularity <= 0.0 || v.nearestAngle == null) {
            return "";
        }
        return v.degreesToAngle < 0.5
            ? String.format(", on the %s", v.nearestAngle)
            : String.format(", %.0f° off the %s", v.degreesToAngle, v.nearestAngle);
    }

    /**
     * "the Sun", "the Moon", "the North Node" - the bodies that read as broken English
     * without an article. ChartFrame stores bare names, which is right for data and wrong
     * for prose: "a trine to Sun" is not a sentence anyone writes.
     */
    private static String withArticle(String body) {
        if (body == null) {
            return "";
        }
        switch (body) {
            case "Sun": case "Moon": case "North Node": return "the " + body;
            default: return body;
        }
    }

    /**
     * "a" or "an". Of the five Ptolemaic aspects only "opposition" takes "an", but the
     * test is on the initial vowel rather than that one word, so nothing new needs
     * remembering when a quincunx or an inconjunct is added.
     */
    private static String indefiniteArticle(String word) {
        if (word == null || word.isEmpty()) {
            return "a";
        }
        // Ordinals are spoken, not spelled: "8th" is "eighth" and takes an, "1st" is
        // "first" and does not. A vowel test alone yields "a 8th-house year", because the
        // first character is a digit and no digit is a vowel.
        if (Character.isDigit(word.charAt(0))) {
            return word.startsWith("8") || word.startsWith("11") || word.startsWith("18")
                ? "an" : "a";
        }
        return "aeiou".indexOf(Character.toLowerCase(word.charAt(0))) >= 0 ? "an" : "a";
    }

    /** The humoral name for a dominant element. TECHNICAL register only. */
    private static String temperament(String element) {
        if (element == null) {
            return null;
        }
        switch (element.toLowerCase()) {
            case "fire":  return "choleric";
            case "earth": return "melancholic";
            case "air":   return "sanguine";
            case "water": return "phlegmatic";
            default:      return null;
        }
    }

    // ------------------------------------------------------------------ tiers

    /**
     * The bodies that lead the chart: the top-ranked one, plus any sitting within
     * dominanceMargin of it. Usually one. Never more than maxLeadCluster.
     */
    public static List<BodyScore.Vector> leadCluster(List<BodyScore.Vector> ranked) {
        List<BodyScore.Vector> out = new ArrayList<>();
        if (ranked.isEmpty()) {
            return out;
        }
        double top = ranked.get(0).prominence;
        for (BodyScore.Vector v : ranked) {
            if (out.size() >= maxLeadCluster || top - v.prominence > dominanceMargin) {
                break;
            }
            out.add(v);
        }
        return out;
    }

    /**
     * One clause. The watch face.
     *
     * Still names one body - there is no room for two - but says when the choice was
     * close, because a watch face that reads Mars now and Jupiter in ten minutes with no
     * hint that they were tied looks broken rather than dynamic.
     */
    public static String oneLine(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked) {
        if (ranked.isEmpty()) {
            return "No chart.";
        }
        BodyScore.Vector top = ranked.get(0);
        int tied = leadCluster(ranked).size();
        return String.format("%s in %s%s - %s%s.",
            withArticle(top.body),
            capitalise(Zodiac.signName(top.longitude)),
            anglePhrase(top),
            shortWhy(top, g),
            tied > 1 ? ", barely ahead of " + withArticle(ranked.get(1).body) : "");
    }

    /**
     * The snapshot milestone. Five to seven sentences typically, three at the sparsest.
     *
     * Measured over 144 charts spanning 1960-2030: 3 to 7 sentences, most commonly 6.
     * The original note here said four to six, which stopped being true once the aspect
     * and dispositor sentences were added - uncapped it reached eight. maxStructuralSentences
     * is what holds the ceiling; see its note for why the cap is two rather than one.
     */
    public static String paragraph(ChartFrame f, Gestalt.Result g,
                                   List<BodyScore.Vector> ranked, Themes.Result t) {
        List<String> out = new ArrayList<>();

        out.add(opener(g));

        List<BodyScore.Vector> lead = leadCluster(ranked);
        if (lead.size() == 1) {
            out.add("The weight falls on " + phraseBody(lead.get(0), g, true) + ".");
            // Only for a single leader: the sentence opens with "It", which has no
            // referent once two or three bodies have just been named together.
            String asp = aspectSentence(lead.get(0));
            if (asp != null) {
                out.add(asp);
            }
        } else if (lead.size() > 1) {
            // Each gets one clause, not the lead body's two - three bodies at two clauses
            // apiece is a paragraph on its own, and the point of this sentence is that no
            // one of them outranks the others, not to characterise each in full.
            List<String> parts = new ArrayList<>();
            for (BodyScore.Vector v : lead) {
                parts.add(phraseBody(v, g, false));
            }
            String joined = String.join("; ", parts.subList(0, parts.size() - 1))
                + "; and " + parts.get(parts.size() - 1);
            out.add((lead.size() == 2 ? "Two things" : "Three things")
                + " carry the weight here, near enough to equally: " + joined + ".");
        }

        // Supporting picks up after the cluster, so a body is never named twice.
        List<String> supporting = new ArrayList<>();
        for (int i = lead.size(); i < ranked.size() && supporting.size() < paragraphBodies - lead.size(); i++) {
            if (ranked.get(i).prominence > 0.0) {
                supporting.add(phraseBody(ranked.get(i), g, false));
            }
        }
        String behind = lead.size() > 1 ? "Behind them" : "Behind it";
        if (supporting.size() == 1) {
            out.add(behind + ", " + supporting.get(0) + ".");
        } else if (supporting.size() > 1) {
            // Semicolons, not "and" - each of these is a clause with its own commas in
            // it, and joining them with "and" produces a sentence you have to re-read.
            out.add(behind + ": " + String.join("; ", supporting) + ".");
        }

        // The chart-level sentences, gathered in priority order and then capped at
        // maxStructuralSentences. Order is set by measured availability, not by which
        // felt most important - over 444 charts each has something to say this often:
        //
        //   theme 99%    contradiction 86%    stellium 81%    dispositor 79%
        //
        // and 53% of charts can offer all four. With a cap of two, theme takes the first
        // slot almost always, so the second is the only real contest and it should go to
        // the rarest strong claim rather than the most frequent one. Hence:
        //
        //   1. theme            - quantified, the whole point of L7, and near-universal
        //   2. sole dispositor  - the rarest thing here and the strongest structural
        //                         claim available: every chain terminates at one body
        //   3. stellium         - concrete, and names bodies visible on the wheel
        //   4. dispositor loop  - common and more abstract than a stellium
        //   5. contradiction    - also out of Themes, so once the theme sentence has run
        //                         this is the most redundant in kind and loses last place
        List<String> structural = new ArrayList<>();

        List<Themes.Signature> sigs = Themes.signaturesAtThreshold(t);
        if (!sigs.isEmpty()) {
            Themes.Signature s = sigs.get(0);
            structural.add(String.format(
                "The %s note repeats across %d independent parts of the chart, which is "
                + "what makes it a theme rather than a detail.",
                s.theme, s.witnessCount));
        }

        // dispositorSentence returns the sole-dispositor form when there is one and the
        // loop form otherwise, so asking it at two different priorities splits the strong
        // case from the weak one without duplicating the phrasing.
        if (g.soleDispositor != null) {
            structural.add(dispositorSentence(g));
        }

        // Contradiction sits here, above stellium and the loop, because the availability
        // figures above put it second and nothing argued for demoting it. It had been
        // last of five under a cap of two, which meant a chart's named tension - the one
        // output that most distinguishes this engine from a cookbook - could never reach
        // the paragraph tier at all. The sole-dispositor promotion above is deliberate
        // and stays; that is a stronger claim than a tension. The loop is not.
        //
        // Measured over 300 random charts, share of paragraphs containing each:
        //
        //                    before   after
        //   contradiction      ~0%     71.3%
        //   stellium          ~80%      0.0%
        //
        // Sentence count is unchanged - the cap did not move, so this is a swap rather
        // than an addition. Stellium is still in the report tier. Raising the cap to
        // three to keep both was the other option and was rejected: it pushes the
        // typical paragraph to seven sentences, which stops being a snapshot.
        if (!t.contradictions.isEmpty()) {
            Themes.Contradiction c = t.contradictions.get(0);
            structural.add(String.format(
                "It also pulls two ways at once - %s and %s both hold real weight here, "
                + "and that tension is structural rather than something to resolve.",
                c.themeA, c.themeB));
        }

        String stel = stelliumSentence(g);
        if (stel != null) {
            structural.add(stel);
        }

        if (g.soleDispositor == null) {
            String loops = dispositorSentence(g);
            if (loops != null) {
                structural.add(loops);
            }
        }

        for (int i = 0; i < Math.min(maxStructuralSentences, structural.size()); i++) {
            out.add(structural.get(i));
        }

        return String.join(" ", out);
    }

    /**
     * Everything, in the order L9 specifies.
     *
     * Forced to TECHNICAL: this tier's reader wants "malefic contrary to sect", not a
     * gloss of it, and the tables below are already technical. Restores the caller's
     * register afterwards so the setting is not silently changed underneath them.
     */
    public static String report(ChartFrame f, Gestalt.Result g,
                                List<BodyScore.Vector> ranked, Themes.Result t) {
        Register caller = register;
        register = Register.TECHNICAL;
        try {
            return buildReport(f, g, ranked, t);
        } finally {
            register = caller;
        }
    }

    private static String buildReport(ChartFrame f, Gestalt.Result g,
                                      List<BodyScore.Vector> ranked, Themes.Result t) {
        StringBuilder sb = new StringBuilder();
        sb.append(wrap(paragraph(f, g, ranked, t), 76)).append("\n\n");

        sb.append("THE SHAPE OF IT\n");
        for (String h : g.headlines) {
            sb.append("  ").append(h).append('\n');
        }

        // The angles get their own section rather than a place in the ranking. They used to
        // appear in the table below because BodyScore.rank included them, which was wrong -
        // an angle scored for its distance from itself came out above the Sun. Excluding
        // them from the ranking would have dropped them off the report entirely, so they
        // are printed here instead: the frame, stated before the bodies measured against it.
        sb.append("\nTHE ANGLES\n");
        String[] angleNames = {"Ascendant", "MC", "Descendant", "IC"};
        double[] angleLons = {f.asc, f.mc, f.dsc, f.ic};
        com.zodiacomputing.ourania.gui.InterpretationService asvc =
            com.zodiacomputing.ourania.gui.InterpretationService.getInstance();
        for (int ai = 0; ai < angleNames.length; ai++) {
            sb.append(String.format("  %-11s %s%n", angleNames[ai], Zodiac.format(angleLons[ai])));
            String aSign = Zodiac.signName(angleLons[ai]);
            String aText = com.zodiacomputing.ourania.gui.Prose.plainText(
                asvc.getAngleInterpretation(angleNames[ai], aSign));
            if (!aText.isEmpty()) {
                sb.append("      ").append(wrap(aText, 76).replace("\n", "\n      ")).append("\n");
            }
        }
        sb.append("\n");

        // Everything below prints into a PLAIN TEXT report, and every value coming out of
        // InterpretationService is HTML the loader never unescaped. Pasting one straight in
        // put 351 literal tags into a 97,000-character report. Route new ones through
        // Prose.plainText as well - nothing throws when you forget, the reader just gets
        // markup.
        sb.append("\nPLANETARY PLACEMENTS (RANKED BY PROMINENCE)\n");
        com.zodiacomputing.ourania.gui.InterpretationService svc = com.zodiacomputing.ourania.gui.InterpretationService.getInstance();
        for (BodyScore.Vector v : ranked) {
            if (v.prominence <= 0.0) {
                continue;
            }
            sb.append(String.format("  %-11s %-16s  %s%n",
                v.body, Zodiac.format(v.longitude), String.join("; ", v.allReasons())));
                
            String core = com.zodiacomputing.ourania.gui.Prose.plainText(svc.getBodyCore(v.body));
            if (core != null && !core.isEmpty()) {
                sb.append("      Core: ").append(wrap(core, 76).replace("\n", "\n      ")).append("\n");
            }
            
            String sign = Zodiac.signName(v.longitude);
            String signText = com.zodiacomputing.ourania.gui.Prose.plainText(svc.getPlanetInSign(v.body, sign));
            if (!signText.isEmpty()) {
                sb.append("      In ").append(Character.toUpperCase(sign.charAt(0))).append(sign.substring(1)).append(": ")
                  .append(wrap(signText, 76).replace("\n", "\n      ")).append("\n");
            }
            
            if (v.house > 0) {
                String houseText = com.zodiacomputing.ourania.gui.Prose.plainText(svc.getPlanetInHouse(v.body, v.house));
                if (!houseText.isEmpty()) {
                    sb.append("      In House ").append(v.house).append(": ")
                      .append(wrap(houseText, 76).replace("\n", "\n      ")).append("\n");
                }
            }
            
            if (v.nearestAngle != null && v.angularity > 0.5) {
                String angleText = com.zodiacomputing.ourania.gui.Prose.plainText(svc.getAngleInterpretation(v.nearestAngle, sign));
                if (!angleText.isEmpty()) {
                    sb.append("      On ").append(v.nearestAngle).append(": ")
                      .append(wrap(angleText, 76).replace("\n", "\n      ")).append("\n");
                }
            }
            
            if (v.aspects != null && !v.aspects.isEmpty()) {
                for (int i = 0; i < Math.min(3, v.aspects.size()); i++) {
                    Aspects.Hit h = v.aspects.get(i);
                    String other = v.body.equals(h.a) ? h.b : h.a;
                    String aspectText = com.zodiacomputing.ourania.gui.Prose.plainText(svc.getAspect(v.body, other, h.type.label));
                    if (!aspectText.isEmpty()) {
                        sb.append("      Aspect (").append(h.type.label).append(" to ").append(other).append("): ")
                          .append(wrap(aspectText, 76).replace("\n", "\n      ")).append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        List<Themes.Signature> sigs = Themes.signaturesAtThreshold(t);
        if (!sigs.isEmpty()) {
            sb.append("\nREPEATED THEMES\n");
            for (Themes.Signature s : sigs) {
                sb.append(String.format("  %-14s %d independent witnesses (%d statements)%n",
                    s.theme, s.witnessCount, s.candidateCount));
                for (List<Themes.Candidate> comp : s.components) {
                    sb.append("      - ").append(comp.get(0).text).append('\n');
                }
            }
        }

        if (!t.contradictions.isEmpty()) {
            sb.append("\nTENSIONS - NAMED, NOT RESOLVED\n");
            for (Themes.Contradiction c : t.contradictions) {
                sb.append(String.format("  %s (%d) against %s (%d)%n",
                    c.themeA, c.witnessesA, c.themeB, c.witnessesB));
                sb.append("      - ").append(c.strongestA.text).append('\n');
                sb.append("      - ").append(c.strongestB.text).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * L6's topics, for the report tier.
     *
     * Takes the topics already computed rather than calling {@link Topics#analyse} here, for
     * the same reason the convergence ranking is passed in: an L9 renderer should not be
     * running an L6 analysis to lay out a page.
     *
     * Only the topics L6 judged worth reporting are printed, and each is described from its
     * agreement pattern. <b>The topic strength is never rendered</b> - it exists for ranking
     * and the spec forbids printing it, on the grounds that the disagreement between the
     * witnesses carries more than their average does.
     */
    public static String topicLayer(List<Topics.Topic> topics) {
        if (topics == null || topics.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\nTHE TOPICS THAT SAY SOMETHING\n");
        int shown = 0;
        for (Topics.Topic t : Topics.inHouseOrder(topics)) {
            if (!t.worthReporting) {
                continue;
            }
            shown++;
            sb.append("  ").append(Topics.describe(t)).append('\n');
            sb.append(String.format("      %s | %s | %s%n",
                t.houseWitness, t.rulerWitness, t.significatorWitness));
            for (String tension : t.tensions) {
                sb.append("      tension: ").append(tension).append('\n');
            }
        }
        if (shown == 0) {
            sb.append("  Nothing in the houses stands out.\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ L8: the time layer

    /**
     * One sentence of timing for the paragraph tier: the profection year, and the single
     * loudest thing currently landing on it.
     *
     * One, not a list. The paragraph tier already caps its structural sentences precisely
     * because a reading that names everything ranks nothing, and transits are the easiest
     * place in the whole engine to produce a wall of true, useless statements.
     */
    public static String timeSentence(Profection prof, List<Transits.Hit> hits) {
        StringBuilder sb = new StringBuilder();
        String ord = ordinal(prof.house);
        sb.append(String.format("This is %s %s-house year, so %s is the lord of it",
            indefiniteArticle(ord), ord, withArticle(prof.lord)));

        Transits.Hit loudest = null;
        for (Transits.Hit h : hits) {
            // Prefer something applying and landing on the lord itself; that is the one
            // fact where the two techniques agree, which is L7's convergence rule in time.
            boolean onLord = prof.lord.equals(h.natal);
            if (!h.applying) {
                continue;
            }
            if (loudest == null
                || (onLord && !prof.lord.equals(loudest.natal))
                || (onLord == prof.lord.equals(loudest.natal) && h.offBy < loudest.offBy)) {
                loudest = h;
            }
        }
        if (loudest == null) {
            return sb.append('.').toString();
        }
        boolean onLord = prof.lord.equals(loudest.natal);
        sb.append(String.format(", and %s is %s %s by %s, %.1f° off exact.",
            withArticle(loudest.transiting),
            loudest.transitRetrograde ? "retrograding back over" : "closing on",
            // Say what it is landing on. "it" is only unambiguous when that IS the lord.
            onLord ? "it" : "natal " + withArticle(loudest.natal),
            loudest.type.label.toLowerCase(), loudest.offBy));
        return sb.toString();
    }

    /**
     * The report tier's time section: the profection year with its lord's natal condition,
     * then every transit that survived the natal-significance filter.
     */
    public static String timeLayer(Profection prof, List<BodyScore.Vector> ranked,
                                   List<Transits.Hit> hits) {
        return timeLayer(prof, ranked, hits, java.util.Collections.emptyList());
    }

    /**
     * As above, plus the dated moments - eclipses and stations - landing on natal points.
     *
     * Those are a different kind of statement from a transit and are printed apart from
     * them. A transit is a condition holding now, with an orb that tightens and loosens; an
     * eclipse or a station is a date. Merging them into one list would force one of the two
     * to be described in the other's terms.
     */
    public static String timeLayer(Profection prof, List<BodyScore.Vector> ranked,
                                   List<Transits.Hit> hits,
                                   List<Transits.EventHit> events) {
        return timeLayer(prof, ranked, hits, events, java.util.Collections.emptyList());
    }

    /**
     * As above, plus the convergence ranking.
     *
     * The ranking arrives already computed rather than being built here. It needs an
     * ephemeris and a date window to widen transits into dated perfections, and an L9
     * renderer having to open the ephemeris to lay out a page is the wrong shape - the
     * earlier version did exactly that and could only be given the instantaneous transit
     * list, which is what made the spans mismatch in the first place.
     */
    public static String timeLayer(Profection prof, List<BodyScore.Vector> ranked,
                                   List<Transits.Hit> hits,
                                   List<Transits.EventHit> events,
                                   List<Convergence.Target> targets) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nTHE YEAR\n");
        sb.append(String.format("  %s%n", prof));

        for (BodyScore.Vector v : ranked) {
            if (v.body.equals(prof.lord)) {
                sb.append(String.format("  lord natally: %-16s %s%n",
                    Zodiac.format(v.longitude), String.join("; ", v.allReasons())));
                break;
            }
        }

        if (hits.isEmpty()) {
            sb.append("\nNOTHING SIGNIFICANT IS BEING TRANSITED\n");
        } else {
            sb.append(transitSection(hits));
        }
        sb.append(eventSection(events));
        sb.append(convergenceSection(targets));
        return sb.toString();
    }

    /**
     * The convergence rule's output: natal points ranked by how many independent techniques
     * name them.
     *
     * Printed last because it is the summary of the two sections above it - the reader has
     * just seen the transits and the dates, and this says which point they keep landing on.
     *
     * The score is stated as a count of families rather than as a confidence percentage.
     * With four families available, three of which can be echoes of each other, a number
     * like "78% confident" would be a precision this has not earned.
     */
    private static String convergenceSection(List<Convergence.Target> targets) {
        if (targets == null || targets.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\nWHERE THE TECHNIQUES AGREE\n");
        List<Convergence.Target> loudest = Convergence.loudest(targets);
        if (!loudest.isEmpty() && loudest.get(0).score >= 0.10) { // e.g., > 10% probability
            StringBuilder names = new StringBuilder();
            for (Convergence.Target t : loudest) {
                names.append(names.length() == 0 ? "" : " and ").append(t.natal);
            }
            sb.append(String.format("  Loudest: natal %s, converged with %.1f%% probability.%n",
                names, loudest.get(0).score * 100.0));
        }
        for (Convergence.Target t : targets) {
            sb.append(String.format("  %-11s %4.1f%% %-38s (%s)%n",
                t.natal, t.score * 100.0, familyList(t), t.why));
            for (Convergence.Witness w : t.witnesses) {
                sb.append(String.format("      %s%n", w));
            }
        }
        return sb.toString();
    }

    private static String familyList(Convergence.Target t) {
        StringBuilder sb = new StringBuilder();
        for (Convergence.Family f : t.families) {
            sb.append(sb.length() == 0 ? "" : "+").append(f.toString().toLowerCase());
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    /**
     * The dated moments, chronological, grouped by event.
     *
     * Grouped by event rather than by natal target - the opposite of the transit section -
     * because these are answers to "what happens when", and one eclipse landing on three
     * natal points is one date to know about, not three.
     */
    private static String eventSection(List<Transits.EventHit> events) {
        if (events == null || events.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\nDATED MOMENTS ON WHAT MATTERS\n");
        Map<String, List<Transits.EventHit>> byEvent = new java.util.LinkedHashMap<>();
        for (Transits.EventHit h : events) {
            byEvent.computeIfAbsent(dateOf(h.event.jd) + "  " + h.event,
                k -> new ArrayList<>()).add(h);
        }
        for (Map.Entry<String, List<Transits.EventHit>> e : byEvent.entrySet()) {
            sb.append(String.format("  %s%n", e.getKey()));
            for (Transits.EventHit h : e.getValue()) {
                sb.append(String.format("      %-12s natal %-11s %5.2f° (%s)%n",
                    h.type.label.toLowerCase(), h.natal, h.offBy, h.why));
            }
        }
        return sb.toString();
    }

    /** Calendar date of a Julian day, in UT, which is what the almanac works in. */
    private static String dateOf(double jd) {
        de.thmac.swisseph.SweDate sd = new de.thmac.swisseph.SweDate();
        sd.setJulDay(jd);
        return String.format("%04d-%02d-%02d", sd.getYear(), sd.getMonth(), sd.getDay());
    }

    private static String transitSection(List<Transits.Hit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nWHAT IS TRANSITING WHAT MATTERS\n");
        // Group by natal target rather than emitting a header whenever the target changes.
        // Two targets can tie on tightness - the Ascendant and Descendant always do, being
        // the same axis - and the interleaved sort then repeats both headers down the page.
        java.util.Map<String, List<Transits.Hit>> byTarget = new java.util.LinkedHashMap<>();
        for (Transits.Hit h : hits) {
            byTarget.computeIfAbsent(h.natal, k -> new java.util.ArrayList<>()).add(h);
        }
        for (java.util.Map.Entry<String, List<Transits.Hit>> e : byTarget.entrySet()) {
            sb.append(String.format("  natal %s (%s)%n", e.getKey(), e.getValue().get(0).why));
            for (Transits.Hit h : e.getValue()) {
                sb.append(String.format("      %-11s %-12s %5.2f° %s%n",
                    h.transiting + (h.transitRetrograde ? " Rx" : ""),
                    h.type.label.toLowerCase(), h.offBy,
                    h.applying ? "applying" : "separating"));
            }
        }
        return sb.toString();
    }

    /** Delegates: one spelling of this, in Zodiac. */
    private static String ordinal(int n) {
        return Zodiac.ordinal(n);
    }

    // ------------------------------------------------------------------ phrasing

    /**
     * Sect, then elemental weighting, chained rather than listed.
     *
     * The sect clause says what follows from the sect rather than only stating it: the
     * out-of-sect malefic is the one the other half of the day would have steadied, and
     * saying so is the difference between a fact and a reading.
     */
    private static String opener(Gestalt.Result g) {
        boolean tech = register == Register.TECHNICAL;
        StringBuilder sb = new StringBuilder();

        if (tech) {
            sb.append(g.diurnal
                ? "A diurnal chart: Jupiter is the benefic of sect, Mars the malefic contrary to sect"
                : "A nocturnal chart: Venus is the benefic of sect, Saturn the malefic contrary to sect");
        } else {
            sb.append(g.diurnal
                ? "A day chart, so Jupiter does the most good here and Mars the most harm"
                : "A night chart, so Venus does the most good here and Saturn the most harm");
            sb.append(g.diurnal
                ? " - the same Mars a night chart would have steadied"
                : " - the same Saturn a day chart would have steadied");
        }
        sb.append('.');

        if (!g.missingElements.isEmpty()) {
            sb.append(" There is essentially no ").append(join(g.missingElements))
              .append(" in it at all, which shapes the chart by what it has to do without.");
        } else if (!g.dominantElements.isEmpty()) {
            sb.append(" The weighting runs heavily to ").append(join(g.dominantElements));
            String hum = tech && g.dominantElements.size() == 1
                ? temperament(g.dominantElements.get(0)) : null;
            sb.append(hum == null ? "." : ", a " + hum + " emphasis.");
        }
        return sb.toString();
    }

    /**
     * The seam where the interpretation library will plug in. Everything above this is
     * selection and ordering; this is the only place that produces descriptive prose.
     *
     * Shape: body in sign, then where it sits relative to an angle *and what that angle
     * governs*, then up to reasonsPerBody justifying clauses. The angle gloss is what
     * stops "5° off the Descendant" from being geometry the reader has to interpret
     * themselves. Exact degrees are kept rather than rounded to "on the Descendant" -
     * the engine measured it, and the measurement is the part no template can fake.
     */
    private static String phraseBody(BodyScore.Vector v, Gestalt.Result g, boolean lead) {
        StringBuilder sb = new StringBuilder();
        sb.append(withArticle(v.body)).append(" in ").append(capitalise(Zodiac.signName(v.longitude)));

        boolean angleStated = false;
        String angle = anglePhrase(v);
        if (!angle.isEmpty()) {
            sb.append(angle);
            String topic = angleTopic(v.nearestAngle);
            if (topic != null) {
                sb.append(", ").append(topic);
            }
            angleStated = true;
        }

        // The lead sentence carries the detail; supporting bodies get one clause each.
        // Two apiece reads as a list, and in a sentence that already has an angle and a
        // topic in it the commas stop separating anything.
        List<String> why = reasonsFor(v, g, lead ? reasonsPerBody : 1, angleStated);
        if (!why.isEmpty()) {
            // A dash once the angle clause has used the comma slot, or the reasons run
            // on from the topic gloss as though they were more of it.
            sb.append(lead || angleStated ? " - " : ", ").append(String.join(", and ", why));
        }

        // A getBodyCore() append stood here from 2026-08-19 to 2026-08-19, commented
        // "Add interpretation core to provide narrative substance". It did the opposite.
        // This method builds PLAIN TEXT and getBodyCore returns HTML that the hand-rolled
        // loader never unescapes, so every reader got literal <b> tags and — escapes
        // dropped mid-sentence, followed by ninety words of encyclopedia per body. It also
        // pushed the paragraph to ten sentences against SnapshotOutputCheck's bound of
        // eight, which is how the suite caught it.
        //
        // Do not re-add it here. Plain prose from the interpretation data is what
        // PlainSnapshot is for, and it converts at the boundary instead of pasting.
        return sb.toString();
    }

    /**
     * Up to max clauses of justification, most telling first.
     *
     * The old version returned exactly one and stopped, so a body that was domicile and
     * angular and chart ruler reported only the first of the three - the data was there
     * and the phrasing threw it away. Still capped, because past two clauses a sentence
     * stops being a reading and becomes a list.
     *
     * Conditional register throughout: describe function, not verdict.
     *
     * @param angleStated whether the caller already named the angle, so the angularity
     *                    clause is not repeated a few words after the degrees
     */
    private static List<String> reasonsFor(BodyScore.Vector v, Gestalt.Result g,
                                           int max, boolean angleStated) {
        boolean tech = register == Register.TECHNICAL;
        List<String> out = new ArrayList<>();

        if (v.isOutOfSectMalefic) {
            out.add(tech ? "the malefic contrary to sect"
                         : "the chart's hardest edge, being the malefic out of sect");
        }
        // The solar conditions sit with the strong dignities because that is their weight:
        // combustion is one of the heaviest accidental debilities in the tradition, and
        // cazimi is rare enough to be worth a clause whenever it happens - 3 occurrences
        // in 1728 body-vectors sampled. Both outrank role for the same reason detriment
        // does: they change what the body can do, not merely what it is responsible for.
        if (v.solar == Aspects.Solar.CAZIMI) {
            out.add(tech ? "cazimi"
                         : "cazimi, riding in the heart of the Sun rather than burned by it");
        } else if (v.solar == Aspects.Solar.COMBUST) {
            out.add(tech ? "combust"
                         : "combust, its own light lost in the Sun's");
        }

        // Strong dignities outrank role; peregrine does not. "Rules the Ascendant" tells
        // you more about a body than "has no dignity" does, and when only one clause fits
        // it is the one worth spending it on - so peregrine sits below the roles, where
        // it acts as the fallback it is.
        if (v.dignity != null && v.dignity.detriment && v.dignity.fall) {
            out.add(tech ? "in both detriment and fall"
                         : "working against the grain of its own sign in two ways at once");
        } else if (v.dignity != null && v.dignity.domicile) {
            out.add(tech ? "in domicile" : "entirely at home in its own sign");
        } else if (v.dignity != null && v.dignity.exaltation) {
            out.add(tech ? "in exaltation" : "exalted, which tends to show");
        }
        if (v.isChartRuler) {
            out.add(tech ? "chart ruler, lord of the Ascendant"
                         : "the ruler of the Ascendant, so it colours everything");
        }
        if (v.isSectLight) {
            out.add(tech ? "the luminary of sect" : "the light this chart belongs to");
        }
        // Weaker than combustion by a good margin, so it sits below the roles: a body
        // under the beams is obscured, not disabled.
        if (v.underBeams) {
            out.add(tech ? "under the beams"
                         : "under the Sun's beams, working half out of sight");
        }
        // Reception sits just below peregrine's opposite number in weight: it is the
        // classical answer TO peregrination, so when both are true saying the body has
        // no dignity of its own but is held by another is the whole statement.
        if (v.reception != null && v.reception.strength >= Dignity.PTS_EXALTATION) {
            String other = v.reception.mutual
                ? (v.body.equals(v.reception.a) ? v.reception.b : v.reception.a)
                : v.reception.receiver();
            if (v.reception.mutual) {
                out.add(tech
                    ? "in mutual reception with " + other + " by " + v.reception.dignity()
                    : "in mutual reception with " + withArticle(other)
                        + ", each sitting in the other's " + v.reception.dignity());
            } else if (!v.body.equals(v.reception.receiver())) {
                out.add(tech
                    ? "received by " + other + " by " + v.reception.dignity()
                    : "a guest in " + withArticle(other) + "'s " + v.reception.dignity()
                        + ", which lends it some footing");
            }
        }
        if (v.dignity != null && v.dignity.peregrine) {
            out.add(tech ? "peregrine" : "peregrine, with no dignity anywhere to steady it");
        }
        if (!angleStated && v.angularity > 0.5) {
            out.add(tech ? "strongly angular"
                         : "close enough to the angle that it tends to be read from outside");
        }
        if (v.isStationary) {
            out.add(tech ? "stationary" : "stationary, which tends to magnify whatever it does");
        } else if (v.isRetrograde) {
            out.add(tech ? "retrograde" : "retrograde, so it works inward before it works outward");
        }
        if (v.valence != null && v.valence.score >= 2 && !v.isOutOfSectMalefic) {
            out.add(tech ? "the benefic of sect"
                         : "the benefic of sect, which is where the help comes from");
        }

        return out.size() > max ? out.subList(0, max) : out;
    }

    /** One clause only, for the watch-face tier. */
    private static String shortWhy(BodyScore.Vector v, Gestalt.Result g) {
        List<String> r = reasonsFor(v, g, 1, false);
        return r.isEmpty() ? "" : r.get(0);
    }

    /**
     * Concentration: three or more bodies sharing a sign.
     *
     * Chart-level rather than a clause on the body, because the informative part is which
     * bodies are piled together - "Saturn is in a stellium" tells you almost nothing on
     * its own, while naming the companions tells you what the concentration is made of.
     * The report tier has carried this in its headlines all along; the paragraph has not.
     */
    private static String stelliumSentence(Gestalt.Result g) {
        if (g.stelliumMembers.isEmpty()) {
            return null;
        }
        boolean tech = register == Register.TECHNICAL;

        if (g.stelliumMembers.size() > 1) {
            List<String> signs = new ArrayList<>();
            for (String s : g.stelliumMembers.keySet()) {
                signs.add(capitalise(s));
            }
            return tech
                ? "Stelliums in " + join(signs) + "."
                : "The chart concentrates in more than one place - " + join(signs)
                    + " each hold three bodies or more.";
        }

        Map.Entry<String, List<String>> only = g.stelliumMembers.entrySet().iterator().next();
        List<String> members = new ArrayList<>();
        for (String b : only.getValue()) {
            members.add(withArticle(b));
        }
        String sign = capitalise(only.getKey());
        String count = numberWord(members.size());

        return tech
            ? String.format("Stellium in %s: %s.", sign, String.join(", ", only.getValue()))
            : String.format("%s bodies sit together in %s - %s - so that one sign carries a "
                + "concentration nothing else in the chart matches.",
                capitalise(count), sign, join(members));
    }

    /** Small numbers read better as words in prose than as digits. */
    private static String numberWord(int n) {
        switch (n) {
            case 3:  return "three";
            case 4:  return "four";
            case 5:  return "five";
            case 6:  return "six";
            case 7:  return "seven";
            default: return String.valueOf(n);
        }
    }

    /**
     * Rulership structure, when it amounts to a chart-level claim.
     *
     * Deliberately silent about ordinary final dispositors. "Jupiter is in its own sign"
     * is already said at body level by reasonsFor(), and repeating it here as though it
     * were structural would be the same fact twice in one paragraph. Only two shapes earn
     * a sentence: one planet that every chain terminates at, and a closed loop that
     * nothing resolves.
     *
     * The loop members ARE in traversal order. This comment previously said they were
     * alphabetical and that naming them as a group was the only honest option - that was
     * true once, and stopped being true when Gestalt moved the sort onto the dedupe key
     * alone. The order is now real, so the sentence walks the actual circuit rather than
     * listing a set.
     */
    private static String dispositorSentence(Gestalt.Result g) {
        boolean tech = register == Register.TECHNICAL;

        if (g.soleDispositor != null) {
            return tech
                ? g.soleDispositor + " is the sole final dispositor: every chain of "
                    + "rulership in the chart terminates there."
                : "Every chain of rulership here ends at " + g.soleDispositor
                    + ", so it is what the rest of the chart ultimately answers to.";
        }

        if (g.dispositorLoops.isEmpty()) {
            return null;
        }
        if (g.dispositorLoops.size() > 1) {
            return tech
                ? String.format("%d dispositor loops; no final dispositor resolves them.",
                    g.dispositorLoops.size())
                : String.format("Rulership closes into %d separate loops here, so nothing "
                    + "in the chart has the last word.", g.dispositorLoops.size());
        }

        // From the typed membership, not by splitting the display string. That parse was
        // already fragile and would now be wrong outright: the display form closes the
        // ring, so splitting it repeats the first planet.
        List<String> loop = g.dispositorLoopMembers.get(0);
        if (tech) {
            return "Dispositor loop: " + g.dispositorLoops.get(0)
                + " - no final dispositor resolves it.";
        }

        // Walk the circuit as it actually runs. Each member is disposed by the next, and
        // the last hands back to the first, which is what makes it closed.
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < loop.size(); i++) {
            String from = loop.get(i);
            String to = loop.get((i + 1) % loop.size());
            steps.add(withArticle(from) + " rules " + withArticle(to)
                + (i == loop.size() - 1 ? " again" : ""));
        }
        // Sentence-case the opening, since withArticle lowercases "the Moon" mid-clause.
        String first = steps.get(0);
        steps.set(0, Character.toUpperCase(first.charAt(0)) + first.substring(1));
        return join(steps) + " - a closed circuit, so no single planet resolves the chain.";
    }

    /**
     * The lead body's relationship to the rest of the chart, as its own sentence.
     *
     * An aspect is structurally different from what reasonsFor() handles: those are
     * conditions a body is in, this is something it is doing with another body. Folding
     * it into the same comma-list would make it compete for the two-clause budget against
     * dignity and role, and a reading that drops "rules the Ascendant" to fit a wide trine
     * has traded down.
     *
     * Returns null when there is nothing worth a sentence, which is most bodies most of
     * the time and is the point. Only the tightest aspect is named; report() lists them all.
     */
    private static String aspectSentence(BodyScore.Vector v) {
        boolean tech = register == Register.TECHNICAL;

        if (v.isUnaspected) {
            // isUnaspected means no aspect to the TRADITIONAL SEVEN, which is not the
            // same as no aspects at all - a body can carry several to the outer planets
            // and still set the flag. Saying "it answers to nothing" in that case is
            // simply untrue, and the difference is worth the extra clause.
            if (v.aspectCount == 0) {
                return tech
                    ? "Unaspected."
                    : "It makes no aspect to anything else in the chart, which leaves it "
                        + "running unchecked.";
            }
            return tech
                ? String.format("Unaspected by the traditional seven; its %d aspects are "
                    + "all to bodies outside them.", v.aspectCount)
                : "It makes no aspect to any of the traditional seven - its only contacts "
                    + "are with the outer bodies.";
        }
        if (v.aspects.isEmpty()) {
            return null;
        }
        // A cazimi or combust body's tightest aspect is necessarily its conjunction to the
        // Sun, and the solar condition says the same thing more precisely - cazimi means
        // within 17 arcminutes, so following it with "a near-exact conjunction to the Sun,
        // inside a degree" is a weaker restatement of what was just said. Step past it to
        // whatever the body is doing with the rest of the chart.
        Aspects.Hit h = null;
        for (Aspects.Hit cand : v.aspects) {          // sorted tightest first
            String other = v.body.equals(cand.a) ? cand.b : cand.a;
            if (v.solar != null && cand.type == Aspects.Type.CONJUNCTION && "Sun".equals(other)) {
                continue;
            }
            h = cand;
            break;
        }
        if (h == null || h.tightness < aspectMentionTightness) {
            return null;
        }

        String other = withArticle(v.body.equals(h.a) ? h.b : h.a);
        String type = h.type.label.toLowerCase();

        if (h.partile) {
            return tech
                ? String.format("Partile %s to %s, %.1f° from exact.", type, other, h.offBy)
                : String.format("It holds a near-exact %s to %s, inside a degree.", type, other);
        }
        // Applying and separating are not the same statement - one is arriving, the other
        // already spent - so the verb carries it rather than a tacked-on label.
        if (tech) {
            return String.format("%s %s to %s, %.1f° from exact.",
                h.applying ? "Applying" : "Separating", type, other, h.offBy);
        }
        return String.format("It is %s %s %s to %s, %.0f° off exact.",
            h.applying ? "closing on" : "pulling away from", indefiniteArticle(type),
            type, other, h.offBy);
    }

    private static String join(List<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1))
            + " and " + items.get(items.size() - 1);
    }

    private static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Soft-wraps to a width so the paragraph tier is readable wherever it lands. */
    public static String wrap(String s, int width) {
        StringBuilder sb = new StringBuilder();
        int line = 0;
        for (String word : s.split(" ")) {
            if (line > 0 && line + word.length() > width) {
                sb.append('\n');
                line = 0;
            }
            sb.append(word).append(' ');
            line += word.length() + 1;
        }
        return sb.toString().trim();
    }

    /**
     * L8: Interpretive text for a Zodiacal Releasing period.
     */
    public static String releasingPeriod(ChartFrame f, Gestalt.Result g, List<BodyScore.Vector> ranked,
                                         com.zodiacomputing.ourania.astro.ZodiacalReleasing.Period p, boolean isSpirit) {
        StringBuilder sb = new StringBuilder();
        String signName = Zodiac.SIGNS[p.sign];
        String rulerName = Dignity.domicileRulerOf(p.sign);
        
        sb.append("<html><body style='font-family: sans-serif; font-size: 14px; margin: 15px; color: #E0E0E0; background-color: #000000;'>");
        sb.append("<h2 style='margin-top: 0; color: #FFFFFF;'>Level ").append(p.level).append(" Period</h2>");
        
        // Context
        sb.append("<p>Releasing from the <b>Lot of ").append(isSpirit ? "Spirit" : "Fortune").append("</b>. ");
        if (isSpirit) {
            sb.append("This tracks the unfolding chapters of career, action, and what you do in the world.</p>");
        } else {
            sb.append("This tracks the unfolding chapters of physical circumstance, health, and what happens to you.</p>");
        }
        
        // Peak / Bond
        if (p.peak || p.afterBond) {
            sb.append("<p>");
            if (p.peak) {
                sb.append("<span style='color: #FFD700;'>⭐ <b>PEAK PERIOD:</b></span> This period is angular to the Lot of Fortune, representing a high point of activity, visibility, or culmination for the current chapter.<br>");
            }
            if (p.afterBond) {
                sb.append("<span style='color: #FF5555;'>🔗 <b>LOOSING OF THE BOND:</b></span> The sequence jumps to the opposite sign instead of the adjacent one, marking a major pivot, transition, or reversal in the overarching narrative.<br>");
            }
            sb.append("</p>");
        }
        
        // Ruler condition
        sb.append("<p>This period occurs in <b>").append(capitalise(signName)).append("</b>, governed by its ruler <b>").append(rulerName).append("</b>. ");
        
        BodyScore.Vector ruler = null;
        for (BodyScore.Vector v : ranked) {
            if (v.body.equals(rulerName)) {
                ruler = v;
                break;
            }
        }
        
        if (ruler != null) {
            sb.append("In the natal chart, ").append(phraseBody(ruler, g, true)).append(".");
            String asp = aspectSentence(ruler);
            if (asp != null) {
                sb.append(" ").append(asp);
            }
            
            // Topic
            sb.append(" As ").append(rulerName).append(" sits in the ").append(ruler.house).append(ordinalSuffix(ruler.house)).append(" house, the events of this period will likely draw upon topics related to that area of life.");
        }
        
        sb.append("</p></body></html>");
        return sb.toString();
    }
    
    private static String ordinalSuffix(int n) {
        if (n >= 11 && n <= 13) return "th";
        switch (n % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
}
