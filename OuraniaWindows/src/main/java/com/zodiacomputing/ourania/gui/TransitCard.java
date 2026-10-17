package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.Transits;
import de.thmac.swisseph.SwissEph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

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

    /** How many cards pop up when the reader stops time on a moment. David chose three. */
    static final int POP_COUNT = 3;

    /** How long each pop-up takes, from the first dim to gone; the card holds for part of it. */
    static final int POP_MS = 1800;

    /** The line that starts a card's direction, on its own paragraph. */
    static final String WHAT_TO_DO = "What to do now: ";

    private TransitCard() {
    }

    /** The card for one transit, or null when it is not one a plain card is made for. */
    static ReadingCard of(NowCards.TransitHit hit) {
        return of(hit, timing(hit));
    }

    /**
     * As above, with the timing said in the caller's words: the timeline's "strongest at
     * 3:40 pm" rather than this page's "building this season".
     */
    static ReadingCard of(NowCards.TransitHit hit, String when) {
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
        String upper = when.toUpperCase(Locale.ROOT);
        String label = area == null ? upper : area.toUpperCase(Locale.ROOT) + " · " + upper;
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

    /** The id of a contact's card on the Now page, so its pop-up lands on that card. */
    static String id(NowCards.TransitHit hit) {
        return "transit-" + idOf(hit.transiting()) + "-"
            + hit.aspect().label.toLowerCase(Locale.ROOT) + "-" + idOf(hit.natal());
    }

    /**
     * The cards to pop up when time stops: the contacts nearest their exact moment, before or
     * after it, up to {@code n}. David, 2026-10-17: "in order of occurrence"; he chose exact
     * time over the weight of the contact. Only contacts that have a plain card take part.
     *
     * @param daysFromExact how many days the contact is from exact; infinite for none found
     */
    static List<NowCards.TransitHit> firstToPop(List<NowCards.TransitHit> hits,
            ToDoubleFunction<NowCards.TransitHit> daysFromExact, int n) {
        Map<NowCards.TransitHit, Double> days = new HashMap<>();
        List<NowCards.TransitHit> out = new ArrayList<>();
        for (NowCards.TransitHit h : hits) {
            if (of(h) != null) {
                days.put(h, daysFromExact.applyAsDouble(h));
                out.add(h);
            }
        }
        out.sort(Comparator.comparingDouble((NowCards.TransitHit h) -> days.get(h))
            .thenComparingDouble(NowCards.TransitHit::orb));
        return out.size() > n ? new ArrayList<>(out.subList(0, n)) : out;
    }

    /**
     * Days between {@code jd} and the nearest moment this contact is exact, from
     * {@link Transits#exactDates}, which counts a retrograde body's extra passes. Searched over
     * a window that suits the planet's speed; infinite when no pass falls inside it.
     */
    static double daysFromExact(SwissEph sw, NowCards.TransitHit hit, double jd) {
        double w = windowDays(hit.transiting());
        double best = Double.POSITIVE_INFINITY;
        for (double d : Transits.exactDates(sw, hit.transiting(), hit.natalLon(), hit.aspect(),
                jd - w, jd + w)) {
            best = Math.min(best, Math.abs(d - jd));
        }
        return best;
    }

    /** How far either side of now to look for an exact pass: a few of the planet's orbs. */
    private static double windowDays(String body) {
        return switch (body) {
            case "Moon" -> 3.0;
            case "Sun", "Mercury", "Venus" -> 30.0;
            case "Mars" -> 60.0;
            case "Jupiter" -> 240.0;
            case "Saturn", "Chiron" -> 400.0;
            default -> 800.0;
        };
    }

    /** Whether a natal point has a written need, so a card can be made for it. */
    static boolean hasNeed(String name) {
        return InterpretationService.getInstance().getTransitCardPart("need." + idOf(name)) != null;
    }

    private static String idOf(String name) {
        Bodies.Def d = Bodies.byName(name);
        return d == null ? "" : d.id;
    }
}
