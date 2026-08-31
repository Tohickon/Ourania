package com.zodiacomputing.ourania.astro;

import de.thmac.swisseph.SweConst;

/**
 * The one registry of every point a chart can show, and the only place that says what
 * each point is called, how it is drawn, and how its longitude is obtained.
 *
 * Before this existed the wheel carried four parallel arrays - the Swiss Ephemeris body
 * numbers, the glyphs, the element colours and the display names - each hand-written and
 * each indexed by the same integer. Adding a body meant editing four literals in step,
 * and the arrays were already inconsistent with L1: the GUI's tenth body was Chiron while
 * ChartFrame's was the North Node, so an index-keyed lookup crossing the two layers
 * handed Chiron's orb to the Node. One array of records makes that class of mistake
 * unrepresentable - a new body is one entry, and nothing is indexed by hand.
 *
 * Deliberately free of Swing and of the ephemeris call itself. It names the ipl number
 * but never calls swe_calc_ut, so BodyCheck can exercise the registry, the parsing and
 * the derived-point arithmetic without starting the app.
 *
 * <b>Orbs are not here.</b> They live in {@link Aspects#orbFor}, keyed by the same names
 * this file defines, because the reading engine needs the same answers the wheel does and
 * two tables of orbs disagreeing is the defect this whole file exists to prevent. Adding
 * an entry here means adding a case there; BodyCheck asserts every name has one.
 *
 * This file is UTF-8 without a BOM and is full of astrological glyphs, so it must be
 * compiled with -encoding UTF-8. See ourania-build-and-run.
 */
public final class Bodies {

    /** What kind of thing a point is, which is what decides how the wheel draws it. */
    public enum Kind {
        /** Sun and Moon. */
        LUMINARY,
        /** Mercury out to Pluto. */
        PLANET,
        /** The lunar nodes: mathematical, but conventionally drawn as bodies. */
        NODE,
        /** Ceres, Pallas, Juno, Vesta, and Chiron the centaur. */
        ASTEROID,
        /** Ascendant, Descendant, MC, IC - drawn as labelled objects, not glyphs. */
        ANGLE,
        /** Part of Fortune, Black Moon Lilith: calculated points that take a glyph. */
        POINT
    }

    /** The headings the settings screen groups the checkboxes under. */
    public enum Group {
        LUMINARIES("Luminaries & Personal Planets",
            "Core personality, ego, emotions, and direct interactions with the world."),
        SOCIAL("Social & Outer Planets",
            "Slower movers: Jupiter and Saturn shape your relation to society, "
            + "the outer three define generational themes."),
        NODES("The Lunar Nodes",
            "Not bodies - the points where the Moon's orbit crosses the ecliptic."),
        ASTEROIDS("Major Asteroids & Centaurs",
            "The \"Big Four\" and Chiron, each adding one specific nuance."),
        POINTS("Calculated Points & Angles",
            "Mathematical intersections. These change minute by minute as the Earth turns.");

        public final String title;
        public final String blurb;

        Group(String title, String blurb) {
            this.title = title;
            this.blurb = blurb;
        }
    }

    /**
     * Where a longitude comes from. Only EPHEMERIS carries a usable ipl; the rest are
     * derived from values the chart already holds, and the deriving is done once in
     * {@link #derive} rather than at each call site.
     */
    public enum Source {
        /** swe_calc_ut on {@link Def#ipl}. */
        EPHEMERIS,
        /** The north node's opposition. */
        SOUTH_NODE,
        /** Cusp 1. */
        ASC,
        /** Cusp 1's opposition. */
        DSC,
        /** Cusp 10. */
        MC,
        /** Cusp 10's opposition. */
        IC,
        /** The sect-reversed lot; see {@link Sect#lotOfFortune}. */
        FORTUNE,
        /**
         * Fortune's companion lot, reflected across the Ascendant; see
         * {@link Sect#lotOfSpirit}. Computed on ChartFrame since before it was a registry
         * point, and guarded by BodyCheck the whole time - registering it here is what gives
         * it a glyph, aspects and prose rather than what makes it exist.
         */
        SPIRIT
    }

    /** A point with no elemental nature of its own. Renders in the neutral grey. */
    public static final int NO_ELEMENT = -1;

    /** One selectable point. Immutable; there is exactly one instance per point. */
    public static final class Def {
        /** Stable key written into settings.properties. Never change a published one. */
        public final String id;
        /** Display name, and the key {@link Aspects#orbFor} is looked up by. */
        public final String name;
        /** The glyph drawn on the wheel, or the text label for an angle. */
        public final String glyph;
        /** Shown instead of the glyph on a font that cannot display it. */
        public final String fallback;
        public final Kind kind;
        public final Group group;
        public final Source source;
        /** Swiss Ephemeris body number, or -1 when the source is derived. */
        private final int defaultIpl;
        /** 0=fire 1=earth 2=air 3=water, matching Zodiac.ELEMENTS, or NO_ELEMENT. */
        public final int element;
        /** On for a chart nobody has configured yet. */
        public final boolean defaultOn;
        /** One line, for the settings checkbox tooltip and the interpretation header. */
        public final String meaning;

        private Def(String id, String name, String glyph, String fallback, Kind kind,
                    Group group, Source source, int ipl, int element, boolean defaultOn,
                    String meaning) {
            this.id = id;
            this.name = name;
            this.glyph = glyph;
            this.fallback = fallback;
            this.kind = kind;
            this.group = group;
            this.source = source;
            this.defaultIpl = ipl;
            this.element = element;
            this.defaultOn = defaultOn;
            this.meaning = meaning;
        }

        public int getIpl() {
            if ("north_node".equals(id)) {
                return com.zodiacomputing.ourania.gui.Settings.useTrueNode() ? SweConst.SE_TRUE_NODE : SweConst.SE_MEAN_NODE;
            }
            if ("lilith".equals(id)) {
                return com.zodiacomputing.ourania.gui.Settings.useTrueLilith() ? SweConst.SE_OSCU_APOG : SweConst.SE_MEAN_APOG;
            }
            return defaultIpl;
        }

        /** True for the four angles, which are drawn as labelled objects. */
        public boolean isAngle() {
            return kind == Kind.ANGLE;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static Def eph(String id, String name, String glyph, String fallback, Kind kind,
                           Group group, int ipl, int element, boolean on, String meaning) {
        return new Def(id, name, glyph, fallback, kind, group, Source.EPHEMERIS, ipl,
            element, on, meaning);
    }

    private static Def derived(String id, String name, String glyph, String fallback,
                               Kind kind, Group group, Source source, int element,
                               boolean on, String meaning) {
        return new Def(id, name, glyph, fallback, kind, group, source, -1, element, on,
            meaning);
    }

    /*
     * Element assignments are the body's own nature, not the sign it happens to occupy:
     * the Moon is watery wherever it sits. The four asteroids take the element their
     * mythology carries - Ceres earth (the harvest), Pallas air (strategy), Juno water
     * (the bond), Vesta fire (the hearth flame) - and Chiron keeps the earth it was
     * already drawn with.
     *
     * The nodes, the angles, Fortune and Lilith take NO_ELEMENT rather than a made-up one.
     * They are intersections, not bodies, and inventing an element for them so the colour
     * table would be full would be asserting something no source says. They draw grey.
     *
     * <b>The node and Lilith variants are a user setting, and both surfaces honour it.</b>
     * {@link Def#getIpl()} resolves the node against Settings.useTrueNode() and Lilith against
     * Settings.useTrueLilith(), and ChartFrame.getBodiesIpl() reads the same settings for the
     * L1 sequence - so the wheel and the reading engine cannot disagree about which node they
     * mean.
     *
     * This comment previously said the opposite: that only the true node was supported, because
     * making it a setting would need the choice threaded through 24 ChartFrame.compute call
     * sites and would otherwise leave the wheel and a report quoting different positions. The
     * first half was a mistake about the code - the body list is one array, so one edit reaches
     * every caller - and Antigravity found it. The second half was the real requirement, and it
     * still holds: <b>if you add another variant, change both resolution points together or
     * neither.</b> Note both are read per call rather than cached, deliberately; a static array
     * would freeze the choice at class-load and only the wheel would appear to respond.
     */

    /** Every selectable point, in display order. This order is the index everything uses. */
    public static final Def[] ALL = {
        eph("sun", "Sun", "☉", "Su", Kind.LUMINARY, Group.LUMINARIES,
            SweConst.SE_SUN, 0, true,
            "Core identity, ego, and life purpose."),
        eph("moon", "Moon", "☽", "Mo", Kind.LUMINARY, Group.LUMINARIES,
            SweConst.SE_MOON, 3, true,
            "Emotional inner world, instincts, and the subconscious."),
        eph("mercury", "Mercury", "☿", "Me", Kind.PLANET, Group.LUMINARIES,
            SweConst.SE_MERCURY, 2, true,
            "Communication, intellect, and information processing."),
        eph("venus", "Venus", "♀", "Ve", Kind.PLANET, Group.LUMINARIES,
            SweConst.SE_VENUS, 1, true,
            "Love, aesthetics, values, and how you relate to others."),
        eph("mars", "Mars", "♂", "Ma", Kind.PLANET, Group.LUMINARIES,
            SweConst.SE_MARS, 0, true,
            "Drive, aggression, ambition, and how you assert yourself."),

        eph("jupiter", "Jupiter", "♃", "Ju", Kind.PLANET, Group.SOCIAL,
            SweConst.SE_JUPITER, 0, true,
            "Expansion, luck, philosophy, and growth."),
        eph("saturn", "Saturn", "♄", "Sa", Kind.PLANET, Group.SOCIAL,
            SweConst.SE_SATURN, 1, true,
            "Restriction, discipline, structure, and karmic lessons."),
        eph("uranus", "Uranus", "♅", "Ur", Kind.PLANET, Group.SOCIAL,
            SweConst.SE_URANUS, 2, true,
            "Rebellion, innovation, sudden change, and originality."),
        eph("neptune", "Neptune", "♆", "Ne", Kind.PLANET, Group.SOCIAL,
            SweConst.SE_NEPTUNE, 3, true,
            "Dreams, illusion, spirituality, and the collective unconscious."),
        eph("pluto", "Pluto", "♇", "Pl", Kind.PLANET, Group.SOCIAL,
            SweConst.SE_PLUTO, 3, true,
            "Transformation, power dynamics, death, and rebirth."),

        eph("north_node", "North Node", "☊", "NN", Kind.NODE, Group.NODES,
            SweConst.SE_TRUE_NODE, NO_ELEMENT, true,
            "The karmic path forward: your purpose, and what the soul is here to learn."),
        derived("south_node", "South Node", "☋", "SN", Kind.NODE, Group.NODES,
            Source.SOUTH_NODE, NO_ELEMENT, true,
            "Past-life karma, innate talents, and the comfort zone to evolve beyond."),

        eph("chiron", "Chiron", "⚷", "Ch", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_CHIRON, 1, true,
            "The wounded healer: the deepest wound, and how you heal others through it."),
        eph("ceres", "Ceres", "⚳", "Ce", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_CERES, 1, false,
            "Nurturing, sustenance, and how you care for yourself and others."),
        eph("pallas", "Pallas", "⚴", "Pa", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_PALLAS, 2, false,
            "Wisdom, strategy, pattern recognition, and creative intelligence."),
        eph("juno", "Juno", "⚵", "Jn", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_JUNO, 3, false,
            "Marriage, deep commitment, and long-term partnership."),
        eph("vesta", "Vesta", "⚶", "Vs", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_VESTA, 0, false,
            "Devotion, focus, spiritual dedication, and the inner flame."),
        eph("eris", "Eris", "⯰", "Er", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_AST_OFFSET + 136199, NO_ELEMENT, false,
            "Disruption, truth-telling, and radical authenticity."),
        eph("eros", "Eros", "♡", "Er", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_AST_OFFSET + 433, NO_ELEMENT, false,
            "Passionate love, desire, and creative vitality."),
        // PSYCHE IS NOT HERE, AND THE REASON IS DATA, NOT OMISSION.
        //
        // Asteroid 16 would be `SE_AST_OFFSET + 16`, needing `ast0/se00016s.se1`. The
        // ephemeris shipped with this app carries exactly four asteroid files -
        // `ast0/se00010s.se1` (Hygiea), `ast0/se00433s.se1` (Eros), `ast7/se07066s.se1`
        // (Nessus) and `ast136/s136199s.se1` (Eris) - and 16 is not among them. Registering
        // it without the file turns BodyCheck red on every chart from 1900 to 2050 with
        // "SwissEph file 'se00016s.se1' not found", which is what happened when it was tried
        // on 2026-08-23.
        //
        // <b>To add Psyche: drop se00016s.se1 into decoded_apk/assets/ast0 first</b>, from
        // the Swiss Ephemeris asteroid set, then restore the eph(...) line. Prose for it is
        // already being written - see the quincunx batches in the session scratchpad.
        //
        // <b>2026-08-27: do not spend time hunting for that file online. It was tried.</b>
        // Astrodienst has retired its FTP tree - /ftp/swisseph/ephe/ now redirects to
        // github.com/aloistr/swisseph/tree/master/ephe, which carries the seas_*.se1 set and
        // astlistn.md but <b>no ast0 directory at all</b>. Four candidate URLs, all 404.
        //
        // And the four asteroid files this app does have were <b>not extracted from the
        // APK</b>, which is the natural assumption and is wrong: unzip -l on all three APKs
        // shows assets/seas_18.se1 and nothing else. They appeared on 2026-08-21 from a
        // source nobody recorded. So there is no known-good way to obtain a fifth.
        // Confirmed by measurement, not inference: SE_AST_OFFSET + 16 returns rc=-1.
        // See Resources/wiki/ourania-ephemeris-fallback.md.
        eph("hygiea", "Hygiea", "⚕", "Hy", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_AST_OFFSET + 10, NO_ELEMENT, false,
            "Health, hygiene, and holistic purification."),
        eph("nessus", "Nessus", "Ns", "Ne", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_AST_OFFSET + 7066, NO_ELEMENT, false,
            "Generational trauma, abuse, and breaking the cycle."),
        // SE_PHOLUS, not SE_AST_OFFSET + 5145. Pholus is one of the six bodies with a
        // built-in number (Chiron 15, Pholus 16, Ceres 17, Pallas 18, Juno 19, Vesta 20),
        // so it reads from the seas_18.se1 already shipped in decoded_apk/assets. Asked
        // for by minor-planet number instead, swe_calc_ut wants se05145s.se1, which is not
        // there, and returns rc=-1 - measured both ways, 2026-08-19.
        eph("pholus", "Pholus", "Ph", "Ph", Kind.ASTEROID, Group.ASTEROIDS,
            SweConst.SE_PHOLUS, NO_ELEMENT, false,
            "Catalyst, small actions with immense consequences."),

        derived("ascendant", "Ascendant", "ASC", "AC", Kind.ANGLE, Group.POINTS,
            Source.ASC, NO_ELEMENT, true,
            "The sign rising in the east: outward demeanour and physical presence."),
        derived("descendant", "Descendant", "DSC", "DC", Kind.ANGLE, Group.POINTS,
            Source.DSC, NO_ELEMENT, true,
            "The western horizon: one-to-one relationships, and what you seek in a partner."),
        derived("mc", "MC", "MC", "MC", Kind.ANGLE, Group.POINTS,
            Source.MC, NO_ELEMENT, true,
            "The highest point: career, public image, and highest aspirations."),
        derived("ic", "IC", "IC", "IC", Kind.ANGLE, Group.POINTS,
            Source.IC, NO_ELEMENT, true,
            "The lowest point: roots, family, private life, psychological foundations."),
        derived("fortune", "Part of Fortune", "⊗", "PF", Kind.POINT, Group.POINTS,
            Source.FORTUNE, NO_ELEMENT, false,
            "An Arabic lot from Sun, Moon and Ascendant: where worldly success and joy fall."),
        derived("spirit", "Part of Spirit", "⊕", "PS", Kind.POINT, Group.POINTS,
            Source.SPIRIT, NO_ELEMENT, false,
            "Fortune's opposite number: deliberate action, intellect, and what the soul intends."),
        eph("lilith", "Black Moon Lilith", "⚸", "Li", Kind.POINT, Group.POINTS,
            SweConst.SE_MEAN_APOG, NO_ELEMENT, false,
            "The lunar apogee: repressed desire, raw feminine power, and refusal."),
    };

    private Bodies() { }

    public static int count() {
        return ALL.length;
    }

    public static Def at(int index) {
        return ALL[index];
    }

    /** Registry index for a settings id, or -1. */
    public static int indexOf(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Registry index for a display name, case-insensitively, or -1.
     *
     * Also accepts "Midheaven" for the MC, because the interpretation data and
     * InterpretationService's angle branch both use that spelling.
     */
    public static int indexOfName(String name) {
        if (name == null) {
            return -1;
        }
        String want = name.trim();
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].name.equalsIgnoreCase(want)) {
                return i;
            }
        }
        if (want.equalsIgnoreCase("Midheaven")) {
            return indexOf("mc");
        }
        return -1;
    }

    /** The Def for a display name, or null. */
    public static Def byName(String name) {
        int i = indexOfName(name);
        return i < 0 ? null : ALL[i];
    }

    /** The one-line meaning for a display name, or "" if the name is unknown. */
    public static String meaningOf(String name) {
        Def d = byName(name);
        return d == null ? "" : d.meaning;
    }

    /**
     * The index of the point this one is defined as the opposition of, or -1.
     *
     * The south node is the north node plus 180 by definition, and so are the Descendant
     * and the IC to the Ascendant and the MC. A pair like that is always exactly opposite,
     * so reporting an "opposition" between them is not a finding about the chart, it is the
     * definition restated - and drawn on the wheel it is a line that is always there and
     * never means anything. Callers use this to skip such pairs.
     */
    public static int oppositeOf(int index) {
        if (index < 0 || index >= ALL.length) {
            return -1;
        }
        switch (ALL[index].source) {
            case SOUTH_NODE: return indexOf("north_node");
            case DSC:        return indexOf("ascendant");
            case IC:         return indexOf("mc");
            default:         return -1;
        }
    }

    /** True when two indices name a point and its own opposition, in either order. */
    public static boolean isOppositePair(int a, int b) {
        return oppositeOf(a) == b || oppositeOf(b) == a;
    }

    /** The default selection: the classical chart, without the minor points. */
    public static boolean[] defaults() {
        boolean[] on = new boolean[ALL.length];
        for (int i = 0; i < ALL.length; i++) {
            on[i] = ALL[i].defaultOn;
        }
        return on;
    }

    /**
     * Parses the settings value: a comma-separated list of ids, where present means shown.
     *
     * <b>Null and empty are different answers, and that distinction is the whole contract.</b>
     * Null means the key has never been written - nobody has configured this - and yields
     * {@link #defaults()}. An empty string means somebody wrote an empty list, which is
     * "show nothing" and is a perfectly legitimate thing to ask for. An earlier version
     * treated both as "never configured", with the result that pressing Select none saved
     * correctly, emptied the wheel, and then silently restored all seventeen defaults at the
     * next launch. Properties.getProperty already distinguishes the two: absent is null, and
     * {@code bodies.enabled=} reads back as "". This method must not throw that away.
     *
     * Once a list has been written it is authoritative, so an id absent from it is off -
     * which also means a point added to this registry later arrives switched off for anyone
     * who has already saved a selection. That is deliberate: the alternative is a new body
     * silently appearing on a chart the user had already tuned.
     *
     * Unknown ids are ignored rather than rejected, so hand-editing the file or rolling
     * back to an older build cannot make the app refuse to start.
     */
    public static boolean[] parse(String csv) {
        if (csv == null) {
            return defaults();
        }
        boolean[] on = new boolean[ALL.length];
        for (String part : csv.split(",")) {
            int i = indexOf(part.trim());
            if (i >= 0) {
                on[i] = true;
            }
        }
        return on;
    }

    /** The settings value for a selection. Round-trips through {@link #parse}. */
    public static String format(boolean[] on) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ALL.length && i < on.length; i++) {
            if (on[i]) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(ALL[i].id);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ derived points

    /**
     * The longitude of a derived point, given the values a chart already holds.
     *
     * Every rule that turns other numbers into a point lives here, once. The wheel needs
     * these and so does L1, and the opposite-point arithmetic in particular is the sort of
     * one-liner that gets written out four times and then disagrees in one of them.
     *
     * @param source     which point to derive; EPHEMERIS is not a valid argument
     * @param asc        cusp 1
     * @param mc         cusp 10
     * @param sunLon     the Sun's longitude, for the sect that reverses the lot
     * @param moonLon    the Moon's longitude
     * @param northNode  the true node's longitude
     * @return the longitude in [0, 360), or NaN if source is EPHEMERIS
     */
    public static double derive(Source source, double asc, double mc,
                                double sunLon, double moonLon, double northNode) {
        switch (source) {
            case ASC:        return Zodiac.normalise(asc);
            case DSC:        return Zodiac.opposite(asc);
            case MC:         return Zodiac.normalise(mc);
            case IC:         return Zodiac.opposite(mc);
            case SOUTH_NODE: return Zodiac.opposite(northNode);
            case FORTUNE:    return Sect.lotOfFortune(Sect.isDiurnal(sunLon, asc),
                                                      asc, sunLon, moonLon);
            case SPIRIT:     return Sect.lotOfSpirit(Sect.isDiurnal(sunLon, asc),
                                                     asc, sunLon, moonLon);
            default:         return Double.NaN;
        }
    }
}
