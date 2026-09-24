package com.zodiacomputing.ourania.astro;

import com.zodiacomputing.ourania.gui.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Master list H1: the natal orbs as the reader's, per point.
 *
 * <p>The transit orb has been a setting since F3. The natal widths were a table in
 * {@link Aspects} with no way to reach them, which is what H1 names - and the setting most often
 * asked for by anyone who works to a house style.
 *
 * <p><b>What is actually at risk here is reach.</b> An orb that the aspect list honours and the
 * wheel does not, or that the reading uses and the grid ignores, is worse than no setting: the
 * reader sets a width, sees some of it take effect, and cannot tell which surface is lying. So
 * the assertions below are mostly about the width arriving everywhere one number is supposed to
 * go, rather than about arithmetic.
 *
 * <ul>
 * <li><b>A, nothing set changes nothing</b> - the table stands, to the last point in the
 * registry.</li>
 * <li><b>B, a width the reader sets is the width that judges</b>, through the pair rule and the
 * synastry halving, and clearing it puts the table back.</li>
 * <li><b>C, the bounds are refused rather than bent</b>, and a hand-edited file cannot stop the
 * app starting.</li>
 * <li><b>D, the settings round-trip</b> - what is stored is keyed by the registry's stable id and
 * comes back as the same number.</li>
 * </ul>
 */
public final class OrbCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        try {
            part("A: the table stands when nothing is set", OrbCheck::untouched);
            part("B: a width the reader sets is the one that judges", OrbCheck::inForce);
            part("C: the bounds are refused, not bent", OrbCheck::bounds);
            part("D: the setting round-trips", OrbCheck::roundTrip);
            part("E: the aspect ceilings", OrbCheck::caps);
        } finally {
            Aspects.setCustomOrbs(null);
            Aspects.setCustomCaps(null);
        }

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

    /**
     * <b>Every point in the registry, not a sample.</b> The failure this guards against is a
     * point whose width stops coming from the table - and the points most likely to be missed
     * are the ones nobody thinks about, which is exactly what a sample leaves out.
     */
    private static void untouched() {
        Aspects.setCustomOrbs(null);
        boolean all = true;
        String first = "";
        for (int i = 0; i < Bodies.count(); i++) {
            String name = Bodies.at(i).name;
            if (Aspects.orbOf(name) != Aspects.defaultBodyOrb(name)) {
                all = false;
                if (first.isEmpty()) {
                    first = name;
                }
            }
        }
        ok("with nothing set every point is judged at its built-in width" + first, all);
        ok("and nothing is reported as customised", Aspects.customOrbs().isEmpty());

        // The angles are in the table under their own names rather than falling to the default,
        // which is the distinction hasExplicitOrb exists to keep.
        ok("an angle has its own entry", Aspects.hasExplicitOrb("Ascendant"));
        ok("and something unheard of does not", !Aspects.hasExplicitOrb("Vulcan"));
        ok("which still gets a width to be judged at",
            Aspects.orbOf("Vulcan") == Aspects.ANGLE_ORB);
    }

    /**
     * <b>Through the pair rule, not just the getter.</b> A width that only {@code orbOf} can see
     * has reached nothing: every aspect in the app is judged by {@link Aspects#orbFor}, which
     * takes the larger of the two points and halves it for a cross-chart reading.
     */
    private static void inForce() {
        double sunWas = Aspects.defaultBodyOrb("Sun");
        double plutoWas = Aspects.defaultBodyOrb("Pluto");

        Map<String, Double> mine = new HashMap<>();
        mine.put("Pluto", 12.0);
        Aspects.setCustomOrbs(mine);

        near("the point the reader widened is judged at their width", 12.0,
            Aspects.orbOf("Pluto"), 1e-9);
        near("and a point they left alone is not", sunWas, Aspects.orbOf("Sun"), 1e-9);
        ok("which is a real change, not the width it already had", Math.abs(12.0 - plutoWas) > 1);

        // The pair rule takes the larger, so widening the smaller of a pair has to show.
        near("a pair takes the wider of the two", 12.0, Aspects.orbFor("Sun", "Pluto"), 1e-9);
        near("whichever way round it is asked", 12.0, Aspects.orbFor("Pluto", "Sun"), 1e-9);
        near("and a cross-chart pair is still halved from there", 6.0,
            Aspects.orbFor("Sun", "Pluto", true), 1e-9);

        // <b>And the aspect's own ceiling still caps it.</b> A reader widening Pluto to 12 has
        // not widened a semisextile to 12; the minor aspects keep their 1 degree.
        double minorCap = Aspects.Type.SEMISEXTILE.maxOrb;
        ok("a minor aspect keeps its own ceiling over the reader's width",
            Aspects.orbFor("Sun", "Pluto") > minorCap);

        Aspects.setCustomOrbs(null);
        near("clearing puts the table back", plutoWas, Aspects.orbOf("Pluto"), 1e-9);
    }

    /** <b>Refused, not clamped.</b> A number the engine cannot honour must not be reported as taken. */
    private static void bounds() {
        double plutoWas = Aspects.defaultBodyOrb("Pluto");
        Map<String, Double> silly = new HashMap<>();
        silly.put("Pluto", 400.0);
        silly.put("Sun", -3.0);
        silly.put("Mars", Double.NaN);
        silly.put(null, 5.0);
        Aspects.setCustomOrbs(silly);

        near("a width past the maximum is refused", plutoWas, Aspects.orbOf("Pluto"), 1e-9);
        near("a negative one is refused", Aspects.defaultBodyOrb("Sun"),
            Aspects.orbOf("Sun"), 1e-9);
        near("a NaN is refused", Aspects.defaultBodyOrb("Mars"), Aspects.orbOf("Mars"), 1e-9);
        ok("and none of them is reported as in force", Aspects.customOrbs().isEmpty());

        // A zero orb would switch a point off entirely rather than narrow it, which is a
        // different feature and not this one.
        ok("the minimum is above zero", Aspects.MIN_BODY_ORB > 0.0);
        ok("and the maximum is below a whole sign", Aspects.MAX_BODY_ORB < 30.0);
    }

    /**
     * <b>Stored under the registry's id, read back by name.</b> Bodies.Def.id calls itself the
     * stable key written into settings.properties; the display name is what Aspects looks an orb
     * up by. Storing under the name would tie a saved file to a label that may be reworded.
     */
    private static void roundTrip() {
        Settings.resetBodyOrbs();
        ok("a fresh install has no widths of its own", Settings.bodyOrbs().isEmpty());
        near("and reports the built-in width", Aspects.defaultBodyOrb("Venus"),
            Settings.bodyOrb("Venus"), 1e-9);

        Settings.setBodyOrb("Venus", 9.5);
        near("what was set comes back", 9.5, Settings.bodyOrb("Venus"), 1e-9);
        near("and is in force in the engine at once", 9.5, Aspects.orbOf("Venus"), 1e-9);
        ok("stored under the registry's id",
            !Settings.get(Settings.BODY_ORB_PREFIX + Bodies.byName("Venus").id, "").isEmpty());

        // Setting it back to the built-in width is the same state as never having touched it.
        Settings.setBodyOrb("Venus", Aspects.defaultBodyOrb("Venus"));
        ok("putting it back to the default forgets the setting", Settings.bodyOrbs().isEmpty());
        near("and the engine is back on the table", Aspects.defaultBodyOrb("Venus"),
            Aspects.orbOf("Venus"), 1e-9);

        Settings.setBodyOrb("Venus", 9.5);
        Settings.setBodyOrb("Mars", 4.0);
        ok("two widths are both remembered", Settings.bodyOrbs().size() == 2);
        Settings.resetBodyOrbs();
        ok("and reset forgets both", Settings.bodyOrbs().isEmpty());
        near("leaving the engine on the table", Aspects.defaultBodyOrb("Venus"),
            Aspects.orbOf("Venus"), 1e-9);

        // Out of range from the screen is clamped rather than refused - the screen is a spinner
        // with bounds, so a value past them is the reader dragging to the end, not a bad file.
        Settings.setBodyOrb("Venus", 999.0);
        near("a width past the maximum is clamped when it comes from the screen",
            Aspects.MAX_BODY_ORB, Settings.bodyOrb("Venus"), 1e-9);
        Settings.resetBodyOrbs();
    }

    /**
     * <b>The ceiling and the family are two questions about one field.</b> Type.maxOrb is a
     * width for the six capped aspects and Double.MAX_VALUE for the five Ptolemaic ones, so it
     * also answers "is this a major" - and until 2026-09-24 three places asked that by comparing
     * it against a literal, two of them against 1000.0, a number that works only because
     * MAX_VALUE is so much larger. Narrowing a trine would have moved it between families.
     */
    private static void caps() {
        Aspects.setCustomCaps(null);

        Aspects.Type minor = Aspects.Type.SEMISEXTILE;
        Aspects.Type major = Aspects.Type.TRINE;
        ok("a minor aspect declares a ceiling of its own", minor.isMinor());
        ok("and a Ptolemaic one does not", !major.isMinor());

        // <b>But it still has a default, and the default is a number.</b> David, 2026-09-24:
        // every aspect carries an orb beside it on the screen, and a spinner cannot show
        // Double.MAX_VALUE. MAX_BODY_ORB is what "no ceiling" is written as.
        ok("a Ptolemaic aspect's default ceiling is a real number, not infinity",
            !Double.isInfinite(Aspects.defaultCapOf(major))
                && Aspects.defaultCapOf(major) < Double.MAX_VALUE);
        near("and it is the widest a point may be set to", Aspects.MAX_BODY_ORB,
            Aspects.defaultCapOf(major), 1e-9);
        near("an untouched ceiling is the declared one", Aspects.defaultCapOf(minor),
            Aspects.capOf(minor), 1e-9);

        Map<Aspects.Type, Double> mine = new java.util.EnumMap<>(Aspects.Type.class);
        mine.put(minor, 0.5);
        Aspects.setCustomCaps(mine);
        near("a ceiling the reader tightened is the one in force", 0.5,
            Aspects.capOf(minor), 1e-9);

        // <b>Which is the point: the cap decides, not the bodies.</b> Two luminaries allow ten
        // degrees between them; a semisextile at half a degree has to override that.
        near("and it caps the pair, not the other way round", 0.5,
            Aspects.effectiveOrb("Sun", "Moon", minor, false), 1e-9);
        ok("while an uncapped aspect is still the bodies' to decide",
            Aspects.effectiveOrb("Sun", "Moon", major, false) > 1.0);

        // <b>And it decides what IS the aspect, not only how wide one may be.</b> typeWithin
        // is what turns a separation into an aspect; a ceiling that reached effectiveOrb and
        // not that would narrow what gets reported while leaving what gets detected alone, so
        // the reader would see the old aspects listed at the new width. Mutation-tested on
        // 2026-09-24: reading the declared field here instead of the ceiling in force
        // <b>survived</b> every other assertion in this part.
        near("the aspect is still found inside the tightened ceiling", 30.0,
            Aspects.Type.SEMISEXTILE.exactAngle, 1e-9);
        ok("a separation inside the tightened ceiling is still that aspect",
            Aspects.typeWithin(30.3, "Sun", "Moon", 10.0) == minor);
        ok("and one outside it is no longer that aspect",
            Aspects.typeWithin(30.8, "Sun", "Moon", 10.0) != minor);

        // <b>Narrowing does not change what kind of aspect it is.</b>
        ok("a tightened aspect is still the same family", minor.isMinor());
        ok("and a Ptolemaic one is still a Ptolemaic one", !major.isMinor());

        // <b>A Ptolemaic aspect takes a ceiling now, and it binds.</b> Before 2026-09-24 one
        // was refused, on the grounds that its width was the bodies' and a second control over
        // one number would be free to disagree. David's answer is that they are not two controls
        // over one number: the points set the width and the aspect sets a ceiling over it, which
        // is exactly what the six minors always did.
        Map<Aspects.Type, Double> onMajor = new java.util.EnumMap<>(Aspects.Type.class);
        onMajor.put(major, 3.0);
        Aspects.setCustomCaps(onMajor);
        near("a ceiling on a Ptolemaic aspect is kept", 3.0, Aspects.capOf(major), 1e-9);
        near("and it caps the pair below what the bodies allow", 3.0,
            Aspects.effectiveOrb("Sun", "Moon", major, false), 1e-9);
        ok("and one outside it is no longer that aspect",
            Aspects.typeWithin(124.0, "Sun", "Moon", 10.0) != major);

        // <b>Lifting one is still refused.</b> MAX_BODY_ORB is a bound now, not a formality -
        // this is the assertion that would have caught H1's hole, where an uncapped aspect's
        // default of Double.MAX_VALUE admitted every finite value the guard was meant to stop.
        Map<Aspects.Type, Double> liftMajor = new java.util.EnumMap<>(Aspects.Type.class);
        liftMajor.put(major, Aspects.MAX_BODY_ORB + 5.0);
        Aspects.setCustomCaps(liftMajor);
        ok("a Ptolemaic ceiling above the widest a point may be is refused",
            Aspects.customCaps().isEmpty());

        // <b>And the default cannot bind, which is the whole case for the number.</b> Walked
        // over every ordered pair rather than argued: no point may be set wider than
        // MAX_BODY_ORB and orbFor is the wider of two points, so capping there is the width.
        Aspects.setCustomCaps(null);
        int pairs = 0;
        boolean everNarrowed = false;
        for (int i = 0; i < Bodies.count(); i++) {
            for (int j = 0; j < Bodies.count(); j++) {
                double width = Aspects.orbFor(Bodies.at(i).name, Bodies.at(j).name);
                if (Math.min(width, Aspects.defaultCapOf(major)) < width - 1e-9) {
                    everNarrowed = true;
                }
                pairs++;
            }
        }
        ok("every ordered pair was walked, so the claim below is not vacuous", pairs > 100);
        ok("a Ptolemaic aspect at its default ceiling narrows no pair at all", !everNarrowed);

        // <b>Widened is refused too.</b> Only tightening, or the ceiling stops being one.
        Map<Aspects.Type, Double> wider = new java.util.EnumMap<>(Aspects.Type.class);
        wider.put(minor, Aspects.defaultCapOf(minor) + 4.0);
        Aspects.setCustomCaps(wider);
        near("a ceiling wider than the declared one is refused", Aspects.defaultCapOf(minor),
            Aspects.capOf(minor), 1e-9);

        Aspects.setCustomCaps(null);
        near("clearing puts the declared ceiling back", Aspects.defaultCapOf(minor),
            Aspects.capOf(minor), 1e-9);

        // ---- and the same through Settings
        Settings.resetAspectCaps();
        ok("a fresh install has no ceilings of its own", Settings.aspectCaps().isEmpty());
        Settings.setAspectCap(minor, 0.5);
        near("what was set comes back", 0.5, Settings.aspectCap(minor), 1e-9);
        near("and is in force at once", 0.5, Aspects.capOf(minor), 1e-9);
        Settings.setAspectCap(major, 2.0);
        ok("a Ptolemaic aspect can be given one from the screen too",
            Settings.aspectCaps().size() == 2);
        near("and it is in force at once", 2.0, Aspects.capOf(major), 1e-9);
        Settings.setAspectCap(major, Aspects.defaultCapOf(major));
        ok("putting it back to its default forgets that one",
            Settings.aspectCaps().size() == 1);
        Settings.setAspectCap(minor, Aspects.defaultCapOf(minor));
        ok("putting it back to the declared ceiling forgets the setting",
            Settings.aspectCaps().isEmpty());
        Settings.resetAspectCaps();
    }

    private static void part(String title, Runnable body) {
        System.out.println("=== Part " + title + " ===");
        int before = failures.size();
        body.run();
        System.out.println("Part " + title.charAt(0)
            + (failures.size() == before ? ": clear"
                : ": " + (failures.size() - before) + " FAILURE(S)"));
        System.out.println();
    }

    private static void ok(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures.add(label);
        }
    }

    private static void near(String label, double expect, double got, double tol) {
        checks++;
        if (!(Math.abs(expect - got) <= tol)) {
            failures.add(label + ": expected " + expect + ", got " + got);
        }
    }
}
