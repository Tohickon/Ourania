package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Zodiac;

/**
 * "Every chart with Moon in Scorpio", asked of the chart book (G7).
 *
 * <p><b>The row's own example, and it was the thing the book could not be asked.</b> The search
 * over saved charts matched a name, a note, a location and a tag - all of them things somebody
 * typed. What a practitioner actually looks for is a placement, which nothing had ever computed
 * for a chart sitting in the book.
 *
 * <p><b>It parses, or it does not, and saying which is the whole design.</b> {@link #parse} returns
 * null for anything that is not a placement query, and the book then searches text as it always
 * did. A parser that guessed would turn a search for a client called Mars into a sweep of the
 * ephemeris, and a search box that sometimes takes seconds for no visible reason is worse than one
 * that cannot answer the question at all.
 *
 * <p><b>The grammar is small on purpose.</b> Three forms, all of them how the question is actually
 * asked out loud: a body in a sign, a body in a house, and a body retrograde. "in" is optional
 * because nobody types it consistently, and the ordinal endings are accepted because "7th" is what
 * a person writes.
 */
final class PlacementQuery {

    /** What is being asked about: a body, always. */
    final Bodies.Def body;

    /** The sign index 0-11, or -1 when the question is not about a sign. */
    final int sign;

    /** The house 1-12, or -1 when the question is not about a house. */
    final int house;

    /** True when the question is "which charts have this retrograde". */
    final boolean retrograde;

    private PlacementQuery(Bodies.Def body, int sign, int house, boolean retrograde) {
        this.body = body;
        this.sign = sign;
        this.house = house;
        this.retrograde = retrograde;
    }

    /**
     * A placement question, or null when the text is not one.
     *
     * <b>Null is the common answer and is not a failure.</b> Nearly everything typed into the
     * book's search box is a name.
     */
    static PlacementQuery parse(String text) {
        if (text == null) {
            return null;
        }
        String q = text.trim().toLowerCase().replaceAll("\\s+", " ");
        if (q.isEmpty()) {
            return null;
        }
        Bodies.Def body = bodyIn(q);
        if (body == null) {
            return null;
        }
        String rest = q.substring(nameOf(body, q).length()).trim();
        if (rest.startsWith("in ")) {
            rest = rest.substring(3).trim();
        }
        if (rest.startsWith("the ")) {
            rest = rest.substring(4).trim();
        }
        if (rest.equals("retrograde") || rest.equals("rx")) {
            return new PlacementQuery(body, -1, -1, true);
        }
        int s = signIndex(rest);
        if (s >= 0) {
            return new PlacementQuery(body, s, -1, false);
        }
        int h = houseNumber(rest);
        if (h >= 1) {
            return new PlacementQuery(body, -1, h, false);
        }
        return null;
    }

    /**
     * Whether a chart answers the question.
     *
     * <b>A body the chart could not compute is not a match rather than an error.</b> A saved
     * chart from before the asteroid files existed has no Chiron, and a search for Chiron in
     * Pisces should leave it out, not refuse to search.
     */
    boolean matches(ChartFrame f) {
        if (f == null) {
            return false;
        }
        int index = Bodies.indexOf(body.id);
        if (index < 0 || index >= f.bodies.length) {
            return false;
        }
        ChartFrame.Body b = f.bodies[index];
        if (b == null || !b.ok) {
            return false;
        }
        if (retrograde) {
            return b.retrograde;
        }
        if (sign >= 0) {
            return Zodiac.signIndex(b.lon) == sign;
        }
        if (house >= 1) {
            return Zodiac.houseOf(b.lon, f.cusps) == house;
        }
        return false;
    }

    /** How the question reads back, for the line that says what was searched for. */
    String describe() {
        if (retrograde) {
            return body.name + " retrograde";
        }
        if (sign >= 0) {
            return body.name + " in " + capitalise(Zodiac.SIGNS[sign]);
        }
        return body.name + " in house " + house;
    }

    // ------------------------------------------------------------------ parsing

    /**
     * The registry body the text begins with, longest name first.
     *
     * <b>The reason first given for this was wrong, and measuring it is how I found out.</b> The
     * comment here said a shorter-first walk would match "Node" inside "North Node" and lose the
     * query. Swept on 28 September: <b>no registry name, id or spaced id is a prefix of any
     * other - zero pairs out of 108 strings</b>, so today the order cannot change any answer, and
     * a mutation reversing it passes every assertion.
     *
     * <b>It is kept anyway, and watched.</b> Registry names are not chosen with this in mind and
     * a body whose name begins another's is one commit away; {@code PlacementSearchCheck} Part A
     * asserts that there is still no such pair, so whoever adds one is told that this ordering has
     * started to matter rather than finding out from a search that quietly answers nothing.
     */
    private static Bodies.Def bodyIn(String q) {
        Bodies.Def best = null;
        int longest = 0;
        for (int i = 0; i < Bodies.count(); i++) {
            Bodies.Def d = Bodies.at(i);
            for (String name : new String[] {d.name.toLowerCase(), d.id.toLowerCase(),
                    d.id.toLowerCase().replace('_', ' ')}) {
                if (!name.isEmpty() && q.startsWith(name) && name.length() > longest) {
                    best = d;
                    longest = name.length();
                }
            }
        }
        return best;
    }

    private static String nameOf(Bodies.Def d, String q) {
        String longest = "";
        for (String name : new String[] {d.name.toLowerCase(), d.id.toLowerCase(),
                d.id.toLowerCase().replace('_', ' ')}) {
            if (q.startsWith(name) && name.length() > longest.length()) {
                longest = name;
            }
        }
        return longest;
    }

    private static int signIndex(String word) {
        for (int i = 0; i < Zodiac.SIGNS.length; i++) {
            if (Zodiac.SIGNS[i].equalsIgnoreCase(word)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * "7", "7th", "house 7", "7th house" - the ways a person writes a house.
     *
     * <b>Empty text answers -1, and that is what makes a bare body name a name search.</b> There
     * was an explicit guard above for {@code "moon"} with nothing after it; removing it changed
     * nothing any assertion could see, because this method already refuses. A guard no failure
     * can reach is not a guard, so the refusal is documented here instead of duplicated there.
     */
    private static int houseNumber(String rest) {
        String s = rest.replace("house", " ").replaceAll("(st|nd|rd|th)\\b", " ").trim();
        s = s.replaceAll("\\s+", "");
        try {
            int n = Integer.parseInt(s);
            return n >= 1 && n <= 12 ? n : -1;
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }

    private static String capitalise(String s) {
        return s == null || s.isEmpty() ? s
            : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
