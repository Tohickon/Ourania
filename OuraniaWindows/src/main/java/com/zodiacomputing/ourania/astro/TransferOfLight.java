package com.zodiacomputing.ourania.astro;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * L4 Relational Structure: Transfer of Light.
 *
 * Identifies classical translation and collection of light using actual astronomical
 * planetary speeds rather than generic order.
 *
 * <b>It returns what it found, and the sentence is built from that.</b> This used to return
 * {@code List<String>} - the sentence INSTEAD of the finding - so everything downstream had
 * one formatted line and no way back to the parts. A reader's card wants to say "Moon
 * translates light from Sun to Mercury" and then, on a tap, which aspects, how far from exact,
 * and which end it is leaving; none of that survived the format call. {@link #findTransfers}
 * still answers in sentences for the report, but it answers by asking {@link Transfer#sentence}
 * rather than by formatting a second time, so the two cannot drift.
 */
public final class TransferOfLight {

    private TransferOfLight() { }

    /** Which classical figure this is. */
    public enum Kind {
        /** One fast body leaves one slower body and carries the light to another. */
        TRANSLATION,
        /** Two faster bodies both apply to one slower body, which holds their light together. */
        COLLECTION
    }

    /**
     * One transfer, with the bodies that make it and the two aspects that carry it.
     *
     * <b>The two ends are not interchangeable in a translation and are in a collection.</b>
     * For a translation {@code from} is the body the mediator is separating from and {@code to}
     * is the one it is applying to, which is the whole direction of the figure. For a collection
     * both ends apply to the mediator, and the order is only the order they were found in.
     */
    public static final class Transfer {
        public final Kind kind;
        /** The translator, or the body that collects - the one the light passes through. */
        public final String mediator;
        public final String from;
        public final String to;
        /** The aspect at the {@code from} end: separating in a translation, applying in a collection. */
        public final Aspects.Hit fromHit;
        /** The aspect at the {@code to} end, always applying. */
        public final Aspects.Hit toHit;
        /**
         * Whether the two ends aspect each other, which is what makes a translation strict.
         *
         * <b>The tradition's translation carries light between two bodies that cannot reach each
         * other.</b> This class has always surfaced the geometry whether or not that held - the
         * old code said so in a comment and went ahead - so the report lists translations that a
         * traditional astrologer would not count. Recording it rather than dropping it keeps the
         * finding and the judgement apart: nothing is filtered here, and a reader's card, or a
         * later decision of David's, can say "strict" where this is false.
         */
        public final boolean endsAspectEachOther;

        Transfer(Kind kind, String mediator, String from, String to,
                 Aspects.Hit fromHit, Aspects.Hit toHit, boolean endsAspectEachOther) {
            this.kind = kind;
            this.mediator = mediator;
            this.from = from;
            this.to = to;
            this.fromHit = fromHit;
            this.toHit = toHit;
            this.endsAspectEachOther = endsAspectEachOther;
        }

        /**
         * The one wording, so a caller that wants prose and a caller that wants parts agree.
         *
         * The text is what this class emitted before it kept records, word for word, because a
         * refactor that also reworded would be two changes wearing one commit and the report
         * would have no way to show which had moved.
         */
        public String sentence() {
            return kind == Kind.TRANSLATION
                ? mediator + " translates light from " + from + " to " + to
                : mediator + " collects light from " + from + " and " + to;
        }

        /**
         * What makes two findings the same finding, and so what deduplication is done on.
         *
         * <b>A collection's two ends are the same finding in either order; a translation's are
         * not.</b> "Jupiter collects light from Mars and Venus" and the same sentence with the
         * ends the other way round are one figure seen twice - both bodies apply to Jupiter and
         * neither is first. A translation leaves one end and reaches the other, so its order is
         * the figure itself and swapping it would fold two different findings into one.
         *
         * <p>The i&lt;j loop hides this on a single hit list, which is why it was invisible:
         * each pair is offered once, already ordered. Hand this two overlapping lists - a
         * transit set and a natal set, where the same pair is in aspect in both - and each
         * collection arrives twice, once each way round. Found by TransferOfLightCheck on
         * 2026-09-27, by an assertion written because a mutation of this deduplication had
         * survived: the dedup was unreachable on the reference chart and wrong underneath.
         */
        String identity() {
            String one = from;
            String other = to;
            if (kind == Kind.COLLECTION && one.compareTo(other) > 0) {
                String swap = one;
                one = other;
                other = swap;
            }
            return kind + "|" + mediator + "|" + one + "|" + other;
        }
    }

    /**
     * Every transfer in the chart, deduplicated on {@link Transfer#identity}.
     *
     * The same mediator and ends can be reached by more than one pairing of hits; the first
     * pairing found is the one kept, which is the behaviour the sentence-level deduplication
     * had before, expressed on the finding instead of on its text.
     */
    public static List<Transfer> find(ChartFrame f, List<Aspects.Hit> hits) {
        List<Transfer> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<Aspects.Hit> applying = new ArrayList<>();
        List<Aspects.Hit> separating = new ArrayList<>();

        for (Aspects.Hit h : hits) {
            // Ignore angles/points that don't have speeds for light transfer
            if (f.body(h.a) == null || f.body(h.b) == null) continue;

            if (h.applying) {
                applying.add(h);
            } else {
                separating.add(h);
            }
        }

        // Translation of Light:
        // A faster planet separating from one slower planet and applying to another slower planet.
        for (Aspects.Hit sep : separating) {
            ChartFrame.Body sepA = f.body(sep.a);
            ChartFrame.Body sepB = f.body(sep.b);

            ChartFrame.Body fasterSep = Math.abs(sepA.lonSpeed) > Math.abs(sepB.lonSpeed) ? sepA : sepB;
            ChartFrame.Body slowerSep = fasterSep == sepA ? sepB : sepA;

            for (Aspects.Hit app : applying) {
                ChartFrame.Body appA = f.body(app.a);
                ChartFrame.Body appB = f.body(app.b);

                ChartFrame.Body fasterApp = Math.abs(appA.lonSpeed) > Math.abs(appB.lonSpeed) ? appA : appB;
                ChartFrame.Body slowerApp = fasterApp == appA ? appB : appA;

                if (fasterSep.name.equals(fasterApp.name) && !slowerSep.name.equals(slowerApp.name)) {
                    // Strictly, translation implies the two slow planets aren't aspecting each
                    // other. The geometry is surfaced regardless, as it always has been, and
                    // whether the strict form holds is recorded on the finding instead.
                    add(results, seen, new Transfer(Kind.TRANSLATION, fasterSep.name,
                        slowerSep.name, slowerApp.name, sep, app,
                        aspected(hits, slowerSep.name, slowerApp.name)));
                }
            }
        }

        // Collection of Light:
        // Two faster planets apply to the same slower planet.
        for (int i = 0; i < applying.size(); i++) {
            for (int j = i + 1; j < applying.size(); j++) {
                Aspects.Hit h1 = applying.get(i);
                Aspects.Hit h2 = applying.get(j);

                ChartFrame.Body h1A = f.body(h1.a);
                ChartFrame.Body h1B = f.body(h1.b);
                ChartFrame.Body h2A = f.body(h2.a);
                ChartFrame.Body h2B = f.body(h2.b);

                ChartFrame.Body faster1 = Math.abs(h1A.lonSpeed) > Math.abs(h1B.lonSpeed) ? h1A : h1B;
                ChartFrame.Body slower1 = faster1 == h1A ? h1B : h1A;

                ChartFrame.Body faster2 = Math.abs(h2A.lonSpeed) > Math.abs(h2B.lonSpeed) ? h2A : h2B;
                ChartFrame.Body slower2 = faster2 == h2A ? h2B : h2A;

                if (slower1.name.equals(slower2.name) && !faster1.name.equals(faster2.name)) {
                    add(results, seen, new Transfer(Kind.COLLECTION, slower1.name,
                        faster1.name, faster2.name, h1, h2,
                        aspected(hits, faster1.name, faster2.name)));
                }
            }
        }

        return results;
    }

    /**
     * The same findings as sentences, for the report.
     *
     * <b>Built from the records, not alongside them.</b> A second format call here is how the
     * card and the report would come to describe the same transfer differently.
     */
    public static List<String> findTransfers(ChartFrame f, List<Aspects.Hit> hits) {
        List<String> unique = new ArrayList<>();
        for (Transfer t : find(f, hits)) {
            if (!unique.contains(t.sentence())) {
                unique.add(t.sentence());
            }
        }
        return unique;
    }

    private static void add(List<Transfer> results, Set<String> seen, Transfer t) {
        if (seen.add(t.identity())) {
            results.add(t);
        }
    }

    /** Whether these two are in aspect at all, by the same hit list the transfers came from. */
    private static boolean aspected(List<Aspects.Hit> hits, String one, String other) {
        for (Aspects.Hit h : hits) {
            if ((h.a.equals(one) && h.b.equals(other)) || (h.a.equals(other) && h.b.equals(one))) {
                return true;
            }
        }
        return false;
    }
}
