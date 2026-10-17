package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import java.util.Locale;

/**
 * A transit as a plain card: which part of your life it touches, what the sky does to which
 * of your needs, how that feels, and what to do now (2026-10-17).
 *
 * <b>Why it exists.</b> David, 2026-10-17: the cards should be "intuitive directions on what to
 * do in areas of life", and "they are now cards explaining transits over a natal chart". The
 * Now page's cards quoted the transit corpus as it stands ("In the transiting structure,
 * transiting Sun in Decile..."), with no part of life and no direction. In plain view this
 * card replaces that one; Astrologer view keeps it.
 *
 * <b>Built from parts, not one entry per pairing.</b> {@code transit_cards.json} holds what each
 * sky planet does (the trigger), how each major aspect feels, which need each natal point
 * stands for (David's psychological framework), and one "what to do now" line for each sky
 * planet and aspect - 55 lines rather than the 650 a sentence per pairing would take. The part
 * of life is {@link LifeAreas}, the names the Life areas page already uses.
 *
 * <b>Only the five major aspects, and only points with a written need.</b> The file has lines
 * for the majors and for nothing else, so a minor aspect or a point with no line finds a part
 * missing and gives no card here at all, rather than a card with a hole in it. They stay in
 * Astrologer view. There is no separate list of majors to agree with the file: the file is
 * the list, and NowCardsCheck asserts it holds exactly the five.
 */
final class TransitCard {

    /** The line that starts a card's direction, on its own paragraph. */
    static final String WHAT_TO_DO = "What to do now: ";

    private TransitCard() {
    }

    /** The card for one transit, or null when it is not one a plain card is made for. */
    static ReadingCard of(NowCards.TransitHit hit) {
        String sky = idOf(hit.transiting());
        String natal = idOf(hit.natal());
        String aspect = hit.aspect().label.toLowerCase(Locale.ROOT);
        InterpretationService svc = InterpretationService.getInstance();
        String trigger = svc.getTransitCardPart("trigger." + sky);
        String acts = svc.getTransitCardPart("acts." + sky);
        String verb = svc.getTransitCardPart("aspect." + aspect);
        String feel = svc.getTransitCardPart("feel." + aspect);
        String need = svc.getTransitCardPart("need." + natal);
        String todo = svc.getTransitCardPart("todo." + sky + "." + aspect);
        if (trigger == null || acts == null || verb == null || feel == null || need == null
                || todo == null) {
            return null;
        }
        String area = LifeAreas.of(hit.natalHouse());
        String when = timing(hit).toUpperCase(Locale.ROOT);
        String label = area == null ? when : area.toUpperCase(Locale.ROOT) + " · " + when;
        return new ReadingCard(label, trigger + " " + verb + " " + need,
            acts + " " + feel + "\n\n" + WHAT_TO_DO + todo, NowCards.whyLines(hit));
    }

    /** How far along the contact is, in plain words, and over how long. */
    static String timing(NowCards.TransitHit hit) {
        String phase = switch (hit.timingBadge()) {
            case "PEAK" -> "at its peak";
            case "BUILDING" -> "building";
            default -> "fading";
        };
        return phase + (hit.isFast() ? " this week" : " this season");
    }

    private static String idOf(String name) {
        Bodies.Def d = Bodies.byName(name);
        return d == null ? "" : d.id;
    }
}
