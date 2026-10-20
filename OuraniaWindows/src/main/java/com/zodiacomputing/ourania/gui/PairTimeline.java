package com.zodiacomputing.ourania.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coming up for two people: what the sky does to both charts, and to each (2026-10-20).
 *
 * <b>Three layers, in the order a couple asks.</b> First what reaches both of them at about the
 * same time - the same planet touching each chart within {@link #SHARED_DAYS} days - because that
 * is the part they will live through together. Then each person's own timeline, as the Coming up
 * page draws it for one. See the vault note transits-in-synastry-model.
 *
 * <b>Built from two Now timelines, not a third engine.</b> Each person's list is
 * {@link NowTimeline#build} against their own chart; this only pairs them. The composite's own
 * timeline is not here: a composite is a separate wheel mode, and its Coming up page already
 * reads the composite when that mode is on.
 */
final class PairTimeline {

    private PairTimeline() { }

    /** Two contacts from the same planet this close together count as one shared stretch. */
    static final double SHARED_DAYS = 7.0;

    /**
     * The Moon reaches every chart every few days, so the Moon touching both is not news. Left
     * out of the shared list; it stays in each person's own timeline.
     */
    static final Set<String> NOT_SHARED = Set.of("Moon");

    /** One planet reaching both charts at about the same time. */
    record Shared(NowTimeline.Entry a, NowTimeline.Entry b) { }

    /**
     * The shared stretches, in A's time order. Each contact is used once: a planet that touches
     * A twice in a week pairs with B's nearest contact once, not twice.
     */
    static List<Shared> shared(List<NowTimeline.Entry> a, List<NowTimeline.Entry> b) {
        List<Shared> out = new ArrayList<>();
        Set<NowTimeline.Entry> used = new HashSet<>();
        for (NowTimeline.Entry x : a) {
            String planet = x.hit().transiting();
            if (NOT_SHARED.contains(planet)) {
                continue;
            }
            NowTimeline.Entry best = null;
            double bestGap = Double.MAX_VALUE;
            for (NowTimeline.Entry y : b) {
                if (used.contains(y) || !planet.equals(y.hit().transiting())) {
                    continue;
                }
                double gap = gap(x, y);
                if (gap <= SHARED_DAYS && gap < bestGap) {
                    best = y;
                    bestGap = gap;
                }
            }
            if (best != null) {
                used.add(best);
                out.add(new Shared(x, best));
            }
        }
        return out;
    }

    /** Days between two contacts; "right now" is the moment itself, so two of them are 0 apart. */
    private static double gap(NowTimeline.Entry x, NowTimeline.Entry y) {
        boolean xNow = Double.isNaN(x.jd());
        boolean yNow = Double.isNaN(y.jd());
        if (xNow && yNow) {
            return 0.0;
        }
        if (xNow || yNow) {
            return Double.MAX_VALUE;
        }
        return Math.abs(x.jd() - y.jd());
    }

    /** The card for a shared stretch: one planet, two people, what it means for each. */
    static ReadingCard card(Shared s, String nameA, String nameB) {
        List<String> why = new ArrayList<>();
        why.add(nameA + ": " + String.join("; ", s.a().card().why()));
        why.add(nameB + ": " + String.join("; ", s.b().card().why()));
        return new ReadingCard("FOR YOU BOTH · " + s.a().window().title.toUpperCase(),
            s.a().hit().transiting() + " reaches both of you",
            "For " + nameA + ": " + s.a().card().headline() + ".\n\nFor " + nameB + ": "
                + s.b().card().headline() + ".",
            why);
    }

    /** The whole page. */
    static String html(List<NowTimeline.Entry> a, String nameA, List<NowTimeline.Entry> b,
            String nameB, boolean showWhy) {
        StringBuilder sb = ReadingCard.page("Coming up together",
            "What the sky does to both of you: first what reaches you both at about the same"
                + " time, then each of you.");
        List<Shared> both = shared(a, b);
        sb.append("<h3 style='color:#FFD166; margin-top:14px; margin-bottom:6px;'>For you both"
            + "</h3>");
        if (both.isEmpty()) {
            sb.append("<p style='color:#9AA5B1; font-size:12px;'>Nothing in the coming year"
                + " reaches you both within " + (int) SHARED_DAYS + " days.</p>");
        } else {
            List<ReadingCard> cards = new ArrayList<>();
            for (Shared s : both) {
                cards.add(card(s, nameA, nameB));
            }
            sb.append(ReadingCard.html(cards, showWhy));
        }
        sb.append("<h2 style='color:#ffa500; margin-top:20px;'>").append(esc(nameA))
          .append("</h2>");
        sb.append(NowCards.windowsHtml(a, NowCards.visible(a), showWhy));
        sb.append("<h2 style='color:#ffa500; margin-top:20px;'>").append(esc(nameB))
          .append("</h2>");
        sb.append(NowCards.windowsHtml(b, NowCards.visible(b), showWhy));
        return sb.append("</body></html>").toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
