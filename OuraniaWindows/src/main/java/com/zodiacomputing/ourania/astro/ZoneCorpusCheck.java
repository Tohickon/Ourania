package com.zodiacomputing.ourania.astro;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The runtime's time zone database, held against the historical record (D5).
 *
 * <p><b>What this suite is for.</b> Every other suite in this project checks code this project
 * wrote. This one checks something the app depends on and does not own: the zone database inside
 * whatever Java runtime it happens to be running on. That database decides what instant a birth
 * time means, and a wrong instant is the one defect nothing downstream can catch - the chart is
 * cast, drawn, read and saved, and every figure in it is confidently wrong.
 *
 * <p><b>Part C is the unusual one.</b> It asserts that the cases this project has measured the
 * runtime to get <i>wrong</i> are still wrong in exactly the way recorded. That reads backwards
 * until you ask what the alternative is: a suite that only asserted the good cases would say
 * nothing when a runtime upgrade fixed the Amsterdam aliasing, and nothing when it broke a zone
 * that used to work. This one goes red either way, and says which.
 *
 * <p>The expectations come from {@link ZoneCorpus}, which takes them from the historical record.
 * A corpus read out of the runtime would agree with the runtime by construction.
 */
public final class ZoneCorpusCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) {
        com.zodiacomputing.ourania.gui.Settings.useScratchFile();

        System.out.println("runtime: Java " + System.getProperty("java.version")
            + ", zone database " + java.time.zone.ZoneRulesProvider.getVersions("UTC").keySet());
        System.out.println();

        part("A: the history this runtime gets right", ZoneCorpusCheck::sound);
        part("B: the shape of an ambiguous morning", ZoneCorpusCheck::shapes);
        part("C: the history this runtime gets wrong, still wrong the same way",
            ZoneCorpusCheck::known);
        part("D: Moments is what the app actually asks", ZoneCorpusCheck::throughMoments);
        part("E: the corpus is worth having", ZoneCorpusCheck::corpusItself);
        part("F: the doubt reaches the chart, not just the corpus",
            ZoneCorpusCheck::reachesMoments);
        part("G: how much of this runtime's zone history is missing",
            ZoneCorpusCheck::mergedZones);
        part("H: before standard time, the place itself is the authority",
            ZoneCorpusCheck::meanTime);

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

    // ------------------------------------------------------------------ A

    private static void sound() {
        for (ZoneCorpus.Case c : ZoneCorpus.sound()) {
            // A skipped local time has no offset of its own; Java shifts it forward, and that
            // case is Part B's business rather than this one's.
            if (c.shape == ZoneCorpus.Shape.SKIPPED) {
                continue;
            }
            String got = c.actual().getId();
            ok(c.place + " (" + c.local + ") is " + c.expected
                + (got.equals(c.expected) ? "" : " but this runtime says " + got),
                got.equals(c.expected));
        }
        ok("there were sound cases to check", ZoneCorpus.sound().size() >= 14);
    }

    // ------------------------------------------------------------------ B

    private static void shapes() {
        for (ZoneCorpus.Case c : ZoneCorpus.CASES) {
            int valid = c.validOffsets();
            int want = c.shape == ZoneCorpus.Shape.SKIPPED ? 0
                : c.shape == ZoneCorpus.Shape.REPEATED ? 2 : 1;
            ok(c.place + " (" + c.local + ") has " + want + " valid offsets, not " + valid,
                valid == want);
        }
        // A corpus with no ambiguous cases would leave Moments' whole reason for existing
        // unexercised by real history.
        int repeated = 0;
        int skipped = 0;
        for (ZoneCorpus.Case c : ZoneCorpus.CASES) {
            if (c.shape == ZoneCorpus.Shape.REPEATED) {
                repeated++;
            }
            if (c.shape == ZoneCorpus.Shape.SKIPPED) {
                skipped++;
            }
        }
        ok("the corpus contains repeated hours", repeated >= 2);
        ok("and a skipped one", skipped >= 1);
    }

    // ------------------------------------------------------------------ C

    private static void known() {
        List<ZoneCorpus.Case> bad = ZoneCorpus.known();
        ok("there are recorded disagreements", !bad.isEmpty());
        for (ZoneCorpus.Case c : bad) {
            String got = c.actual().getId();
            // <b>Red when it is fixed, too.</b> If a runtime upgrade makes this right, this
            // assertion fails and says so - which is the signal to delete the entry, not a
            // defect. The message has to carry that, because the person reading it will be
            // reading it a year from now.
            ok("STILL WRONG as recorded: " + c.place + " should be " + c.expected
                + " and this runtime says " + got
                + (got.equals(c.expected)
                    ? " - THE RUNTIME NOW AGREES WITH HISTORY. Remove this entry from"
                        + " ZoneCorpus; it is no longer a disagreement."
                    : ""),
                !got.equals(c.expected));
            ok("and the disagreement is written down", c.runtimeDisagrees != null
                && c.runtimeDisagrees.length() > 20);
            ok("with its source in the record", c.source != null && c.source.length() > 20);
        }

        // The aliasing itself, named rather than inferred from one date. This is the cause; the
        // two dated cases above are its symptoms.
        boolean aliased = ZoneId.of("Europe/Amsterdam").getRules()
            .equals(ZoneId.of("Europe/Brussels").getRules());
        ok("Europe/Amsterdam is still aliased to Europe/Brussels in this runtime"
            + (aliased ? "" : " - IT IS NOT ANY MORE, so the two Amsterdam entries in ZoneCorpus"
                + " should be re-measured"),
            aliased);

        // And the cost, so nobody has to work out whether it matters.
        double outBy = 19.0 + 32.0 / 60.0;
        double degrees = ZoneCorpus.ascendantDegreesFor(outBy);
        ok("which costs about " + String.format("%.1f", degrees)
            + " degrees of Ascendant on a Dutch chart before 1940",
            degrees > 4.0 && degrees < 6.0);
    }

    // ------------------------------------------------------------------ D

    private static void throughMoments() {
        // <b>The corpus has to reach what the app uses.</b> Asserting ZoneId directly would
        // leave Moments - the thing the chart actually calls - unchecked against any of this.
        // That is the mistake this session has already made three times: the rule and its only
        // consumer, checked apart.
        for (ZoneCorpus.Case c : ZoneCorpus.CASES) {
            LocalDate d = c.local.toLocalDate();
            LocalTime t = c.local.toLocalTime();
            Moments.Resolved r = Moments.resolve(d, t, ZoneId.of(c.zoneId));
            ok(c.place + ": Moments resolves it at all", r != null && r.when != null);
            if (r == null || r.when == null) {
                continue;
            }
            switch (c.shape) {
                case SINGLE:
                    ok(c.place + ": Moments calls it NORMAL", r.kind == Moments.Kind.NORMAL);
                    // <b>Where the runtime is right, Moments agrees with it; where it is known
                    // wrong, Moments corrects it to what history says.</b> Asserting one rule
                    // for both would mean asserting that the correction does not happen.
                    ZoneCorpus.Doubt fix = ZoneCorpus.doubtAbout(ZoneId.of(c.zoneId), c.local);
                    if (fix != null && fix.correctionSeconds != 0) {
                        ok(c.place + ": Moments corrects it to the historical offset",
                            c.expected.equals(r.when.getOffset().getId()));
                        ok(c.place + ": which is NOT what the runtime gives",
                            !r.when.getOffset().equals(c.actual()));
                    } else {
                        ok(c.place + ": and agrees with the offset the zone gives",
                            r.when.getOffset().equals(c.actual()));
                        ok(c.place + ": and says nothing needed assuming", r.note == null);
                    }
                    break;
                case REPEATED:
                    ok(c.place + ": Moments calls it REPEATED",
                        r.kind == Moments.Kind.REPEATED);
                    ok(c.place + ": and offers the other reading", r.other != null);
                    ok(c.place + ": which is an hour of real time away",
                        r.other != null && Math.abs(java.time.Duration.between(
                            r.when.toInstant(), r.other.toInstant()).toMinutes()) == 60);
                    ok(c.place + ": and warns in words", r.note != null && r.uncertain());
                    break;
                case SKIPPED:
                    ok(c.place + ": Moments calls it SKIPPED", r.kind == Moments.Kind.SKIPPED);
                    ok(c.place + ": and warns in words", r.note != null && r.uncertain());
                    ok(c.place + ": and has no second reading to offer", r.other == null);
                    break;
                default:
                    ok(c.place + ": has a shape", false);
                    break;
            }
        }

        // The 1947 London case is the one that makes this part worth having: an ambiguous hour
        // from the historical record rather than a modern October Sunday. A Moments that only
        // handled present-day rules would pass everything else here.
        Moments.Resolved r = Moments.resolve(LocalDate.of(1947, 8, 10), LocalTime.of(2, 30),
            ZoneId.of("Europe/London"));
        ok("the end of double summer time is a repeated hour",
            r.kind == Moments.Kind.REPEATED);
        // <b>Guarded, because a check that throws is worse than one that fails.</b> A mutation
        // that made every hour look ordinary left r.other null here, and the NullPointerException
        // took the whole suite down - so it reported no failures at all, and every assertion
        // after this line went unrun. A red suite says what is wrong; a crashed one says nothing.
        ok("and the two readings are +02:00 and +01:00",
            r.other != null
                && "+02:00".equals(r.when.getOffset().getId())
                && "+01:00".equals(r.other.getOffset().getId()));
    }

    // ------------------------------------------------------------------ E

    private static void corpusItself() {
        ok("the corpus is not trivially small", ZoneCorpus.CASES.length >= 16);

        // Every case carries its authority. A corpus entry with no source is a number somebody
        // typed, and in a year nobody will know which.
        for (ZoneCorpus.Case c : ZoneCorpus.CASES) {
            ok(c.place + " cites where the figure comes from",
                c.source != null && c.source.length() > 15);
            ok(c.place + " names a real zone", ZoneId.getAvailableZoneIds().contains(c.zoneId));
        }

        // The corpus must span the things that actually go wrong, or it is a set of easy cases.
        boolean warYears = false;
        boolean subMinute = false;
        boolean notWholeHour = false;
        boolean beforeStandardTime = false;
        for (ZoneCorpus.Case c : ZoneCorpus.CASES) {
            int y = c.local.getYear();
            if (y >= 1940 && y <= 1947) {
                warYears = true;
            }
            if (c.expected.length() > 6) {
                subMinute = true;
            }
            if (c.expected.endsWith(":30") || c.expected.endsWith(":45")
                    || c.expected.endsWith(":20")) {
                notWholeHour = true;
            }
            if (y < 1900) {
                beforeStandardTime = true;
            }
        }
        ok("it covers the war years", warYears);
        ok("it covers offsets with seconds in them", subMinute);
        ok("it covers zones that are not whole hours", notWholeHour);
        ok("it reaches back before standard time", beforeStandardTime);

        // The arithmetic that says why any of it matters.
        ok("an hour out is about fifteen degrees of Ascendant",
            Math.abs(ZoneCorpus.ascendantDegreesFor(60) - 15.0) < 0.001);
        ok("and a half hour zone got wrong is about seven and a half",
            Math.abs(ZoneCorpus.ascendantDegreesFor(30) - 7.5) < 0.001);
    }

    // ------------------------------------------------------------------


    /**
     * <b>The warning has to arrive somewhere a reader will see it.</b>
     *
     * Everything above proves the runtime is wrong about the Netherlands. None of it would notice
     * if that finding stayed inside {@link ZoneCorpus} and never reached the chart - which is the
     * failure mode this session has hit four separate times: the rule and its only consumer,
     * checked apart. So this asks {@link Moments}, which is what the wheel actually calls.
     */
    private static void reachesMoments() {
        for (ZoneCorpus.Case c : ZoneCorpus.known()) {
            Moments.Resolved r = Moments.resolve(c.local.toLocalDate(), c.local.toLocalTime(),
                ZoneId.of(c.zoneId));
            ok(c.place + ": Moments reports the zone data as doubtful",
                r.zoneDataDoubt != null);
            ok(c.place + ": and says so in a reader's words",
                r.zoneDataDoubt != null && r.zoneDataDoubt.length() > 60);
            ok(c.place + ": and counts as uncertain", r.uncertain());
            // <b>The chart is cast, corrected, and the correction is declared.</b> Refusing
            // would lose the chart; correcting in silence would move it without saying so; and
            // leaving it wrong was the earlier design, which David overruled on 25 Sep - an
            // ambiguous hour is a fact about the world the software cannot settle, but a wrong
            // zone database is a fact about the software, which it can.
            ok(c.place + ": and the chart is cast anyway", r.when != null);
            ok(c.place + ": at the offset HISTORY gives, corrected",
                c.expected.equals(r.when.getOffset().getId()));
            ok(c.place + ": which is not the offset this runtime has",
                !r.when.getOffset().equals(c.actual()));

            // <b>The local time is what a reader knows, so it must not move.</b> What was wrong
            // is the conversion from the clock in the room to real time, not the clock.
            ok(c.place + ": the written local time is untouched",
                r.when.toLocalDateTime().equals(c.local));

            // And the instant moved by exactly the correction, in the right direction: a larger
            // east offset means the same local time is EARLIER in UT.
            ZoneCorpus.Doubt d = ZoneCorpus.doubtAbout(ZoneId.of(c.zoneId), c.local);
            long moved = java.time.Duration.between(
                r.when.toInstant(),
                java.time.ZonedDateTime.of(c.local, ZoneId.of(c.zoneId)).toInstant()).getSeconds();
            ok(c.place + ": the instant moved by the correction, earlier in UT ("
                + moved + "s)", d != null && moved == d.correctionSeconds);
        }

        // <b>The control.</b> Every assertion above would pass just as well against a doubtAbout
        // that flagged every moment ever cast, which would train a reader to ignore the notice.
        Moments.Resolved fine = Moments.resolve(
            java.time.LocalDate.of(1990, 6, 1), LocalTime.NOON, ZoneId.of("Europe/Amsterdam"));
        ok("a modern Dutch birth is not flagged", fine.zoneDataDoubt == null);
        ok("and is not uncertain", !fine.uncertain());
        for (ZoneCorpus.Case c : ZoneCorpus.sound()) {
            Moments.Resolved r = Moments.resolve(c.local.toLocalDate(), c.local.toLocalTime(),
                ZoneId.of(c.zoneId));
            ok(c.place + ": a sound case carries no zone doubt", r.zoneDataDoubt == null);
        }

        // And the span, not just the two dated cases: a birth anywhere inside the aliasing is
        // flagged, and one the day after it ends is not.
        ok("a Dutch birth in 1901 is flagged", Moments.resolve(
            java.time.LocalDate.of(1901, 3, 3), LocalTime.NOON,
            ZoneId.of("Europe/Amsterdam")).zoneDataDoubt != null);
        ok("one in 1945, after the occupation imposed CET, is not", Moments.resolve(
            java.time.LocalDate.of(1945, 3, 3), LocalTime.NOON,
            ZoneId.of("Europe/Amsterdam")).zoneDataDoubt == null);
        ok("and a Belgian birth in the same years is not, because Brussels is right",
            Moments.resolve(java.time.LocalDate.of(1930, 1, 15), LocalTime.NOON,
                ZoneId.of("Europe/Brussels")).zoneDataDoubt == null);
    }


    /**
     * <b>How wide the problem is, measured rather than guessed at.</b>
     *
     * The Dutch defect was found by accident. One lucky find says nothing about the next chart, so
     * this sweeps every zone this runtime knows and asks two questions: how many of them share a
     * history with another zone, and how many carry a local mean time that does not match their
     * own city's longitude.
     *
     * <p><b>The geographic test needs no external authority.</b> A zone named for a city should
     * carry that city's local mean time before standard time arrived, because local mean time is
     * defined by longitude - the sun over that meridian. Where it carries someone else's, the zone
     * has been merged onto a canonical neighbour and its own early history is gone. Ourania's
     * atlas supplies the longitude, so the check is against the earth rather than against another
     * copy of the same database.
     *
     * <p><b>This is IANA policy, not a bug in one zone.</b> Zones that have agreed since
     * 1970-01-01 are merged in the main distribution; the pre-1970 detail lives in a separate
     * backzone file that most runtimes do not compile in. The database's own documentation says it
     * attempts to be correct from 1970 on. So the right way to hold this is not to list the broken
     * zones - it is to measure the shape and be told when it moves.
     *
     * <p>The figures are asserted exactly. If a runtime upgrade changes them this part goes red
     * and says to re-measure, which is the same discipline as Part C.
     */
    private static void mergedZones() {
        java.util.List<String> ids = new java.util.ArrayList<>(ZoneId.getAvailableZoneIds());
        java.util.Collections.sort(ids);
        ok("the runtime knows a realistic number of zones", ids.size() > 500);

        // <b>Grouped by the whole transition history, not by toString().</b> ZoneRules.toString()
        // reports only the current standard offset, so it puts Paris and Amsterdam in one bucket
        // although they differ by ten minutes before 1911 - measured, and it is why this loop is
        // quadratic instead of a HashMap.
        java.util.List<java.time.zone.ZoneRules> reps = new java.util.ArrayList<>();
        java.util.List<Integer> sizes = new java.util.ArrayList<>();
        for (String id : ids) {
            java.time.zone.ZoneRules r = ZoneId.of(id).getRules();
            int at = -1;
            for (int i = 0; i < reps.size(); i++) {
                if (reps.get(i).equals(r)) {
                    at = i;
                    break;
                }
            }
            if (at < 0) {
                reps.add(r);
                sizes.add(Integer.valueOf(1));
            } else {
                sizes.set(at, Integer.valueOf(sizes.get(at).intValue() + 1));
            }
        }
        int sharing = 0;
        for (Integer n : sizes) {
            if (n.intValue() > 1) {
                sharing += n.intValue();
            }
        }
        ok("most zones do NOT have a history of their own (" + sharing + " of " + ids.size()
            + " share one, in " + reps.size() + " distinct histories) - if this moved, the "
            + "runtime's zone database changed and these figures want re-measuring",
            sharing > 300 && reps.size() < 400);

        // The two groups this project has had to care about, named so the next reader does not
        // have to re-derive them.
        ok("Europe/Amsterdam still has Belgium's history",
            ZoneId.of("Europe/Amsterdam").getRules()
                .equals(ZoneId.of("Europe/Brussels").getRules()));
        ok("and Scandinavia still has Berlin's",
            ZoneId.of("Europe/Stockholm").getRules()
                .equals(ZoneId.of("Europe/Berlin").getRules()));
        // The control: a shared history is not automatically wrong. Monaco really did keep Paris
        // time, so those two agreeing is history rather than data loss.
        ok("but Paris and Monaco agreeing is real history, not a merge",
            ZoneId.of("Europe/Paris").getRules().equals(ZoneId.of("Europe/Monaco").getRules()));
        ok("and Paris is NOT merged with Amsterdam, whatever ZoneRules.toString() suggests",
            !ZoneId.of("Europe/Paris").getRules()
                .equals(ZoneId.of("Europe/Amsterdam").getRules()));

        // <b>The geographic sweep.</b> Against the earth, not against another copy of tzdb.
        int compared = 0;
        int adrift = 0;
        int worst = 0;
        String worstZone = "";
        for (String id : ids) {
            int slash = id.lastIndexOf('/');
            if (slash < 0) {
                continue;
            }
            String city = id.substring(slash + 1).replace('_', ' ');
            com.zodiacomputing.ourania.gui.Atlas.Place p =
                com.zodiacomputing.ourania.gui.Atlas.resolve(city);
            // Only trust a hit that is actually in this zone: Rome, Victoria and Phoenix all
            // exist in several countries.
            if (p == null || !id.equals(p.zoneId)) {
                continue;
            }
            java.util.List<java.time.zone.ZoneOffsetTransition> ts =
                ZoneId.of(id).getRules().getTransitions();
            if (ts.isEmpty()) {
                continue;
            }
            compared++;
            int lmt = ts.get(0).getOffsetBefore().getTotalSeconds();
            int fromSun = (int) Math.round(p.longitude * 240.0);
            int off = Math.abs(lmt - fromSun);
            if (off > 120) {
                adrift++;
            }
            if (off > worst) {
                worst = off;
                worstZone = id + " (" + p.name + ")";
            }
        }
        ok("the atlas could place enough zones to sweep (" + compared + ")", compared > 200);
        ok("and a large share carry a local mean time that is not their own city's ("
            + adrift + " of " + compared + " out by over two minutes) - this is the pre-1970 "
            + "merging, not one broken zone", adrift > 40);
        ok("the worst is over half an hour out: " + worstZone + " by " + worst + "s", worst > 1800);

        // <b>And the Dutch case is one instance of that, not a special case.</b> Tying the two
        // together is the point: a reader of this suite in a year should not conclude that the
        // Netherlands is uniquely unlucky.
        com.zodiacomputing.ourania.gui.Atlas.Place ams =
            com.zodiacomputing.ourania.gui.Atlas.resolve("Amsterdam");
        ok("Amsterdam is in the atlas", ams != null && "Europe/Amsterdam".equals(ams.zoneId));
        if (ams != null) {
            int fromSun = (int) Math.round(ams.longitude * 240.0);
            ok("and the sun over Amsterdam gives about the offset the Dutch kept ("
                + fromSun + "s against " + ZoneCorpus.AMSTERDAM_MEAN_TIME + "s)",
                Math.abs(fromSun - ZoneCorpus.AMSTERDAM_MEAN_TIME) < 30);
        }
    }


    /**
     * <b>The general half of D5: a birth before standard time is placed by its own longitude.</b>
     *
     * The hand-sourced spans fix one country in one period. This covers every place in the period
     * where the answer is derivable rather than sourced - before a zone's first recorded
     * transition, the offset in force is local mean time, and local mean time is the sun over the
     * meridian the birth happened on.
     *
     * <p>The controls matter more than the corrections here. A zone that kept its own history must
     * be left alone and must say nothing, or the notice becomes noise a reader learns to skip.
     */
    private static void meanTime() {
        // Zones this runtime merged: the correction fires, and lands on the real figure.
        // Stockholm's own mean time is 18.069 East = +01:12:17; the runtime offers Berlin's.
        check("Europe/Stockholm", 18.069, 1890, "+01:12:17", true);
        check("Europe/Oslo", 10.746, 1880, "+00:42:59", true);
        check("Europe/Amsterdam", 4.890, 1880, "+00:19:34", true);

        // <b>Zones that kept their own history are untouched and silent.</b> This is the half
        // that stops the feature becoming noise, and it is the half a careless version breaks.
        check("Europe/London", -0.1276, 1840, "-00:01:15", false);
        check("America/New_York", -74.006, 1880, "-04:56:02", false);
        check("Europe/Paris", 2.3522, 1890, "+00:09:21", false);

        // After the first transition the offset is political, and longitude says nothing about
        // which zone a country chose to join. The correction must not reach there.
        Moments.Resolved late = Moments.resolve(java.time.LocalDate.of(1990, 6, 1),
            LocalTime.NOON, ZoneId.of("Europe/Stockholm"), 18.069);
        ok("a modern Stockholm birth is not touched", late.zoneDataDoubt == null);
        ok("and keeps the runtime's offset", "+02:00".equals(late.when.getOffset().getId()));

        // No longitude, no geographic claim: a caller that does not know where the birth was
        // must not have one invented for it.
        Moments.Resolved blind = Moments.resolve(java.time.LocalDate.of(1880, 6, 1),
            LocalTime.NOON, ZoneId.of("Europe/Stockholm"));
        ok("with no longitude there is no mean-time correction", blind.zoneDataDoubt == null);

        // <b>The sourced span wins where both could apply.</b> A Dutch birth in 1930 is after the
        // first transition, so only the hand-written span reaches it - and it is the better
        // figure, because it knows what the Netherlands legislated rather than only where the
        // sun was.
        Moments.Resolved dutch = Moments.resolve(java.time.LocalDate.of(1930, 1, 15),
            LocalTime.NOON, ZoneId.of("Europe/Amsterdam"), 4.890);
        ok("the sourced Dutch span still applies in 1930", dutch.zoneDataDoubt != null);
        ok("and gives the legislated offset, not the geographic one",
            "+00:19:32".equals(dutch.when.getOffset().getId()));
    }

    /** One place, before or after its own standard time, corrected or left alone. */
    private static void check(String zone, double lon, int year, String expected,
                              boolean shouldMove) {
        Moments.Resolved r = Moments.resolve(java.time.LocalDate.of(year, 6, 1), LocalTime.NOON,
            ZoneId.of(zone), lon);
        java.time.ZonedDateTime raw = java.time.ZonedDateTime.of(
            java.time.LocalDateTime.of(year, 6, 1, 12, 0), ZoneId.of(zone));
        String got = r.when.getOffset().getId();
        ok(zone + " " + year + " reads " + expected + (expected.equals(got) ? "" : " but got "
            + got), expected.equals(got));
        ok(zone + " " + year + (shouldMove ? " is corrected" : " is left exactly as cast"),
            shouldMove != r.when.getOffset().equals(raw.getOffset()));
        ok(zone + " " + year + (shouldMove ? " says so" : " says nothing"),
            shouldMove == (r.zoneDataDoubt != null));
        ok(zone + " " + year + " keeps the written local time",
            r.when.getHour() == 12 && r.when.getMinute() == 0);
    }

    private interface Body {
        void run();
    }

    private static void part(String name, Body body) {
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
}
