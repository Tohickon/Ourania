package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Aspects;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The chart's aspect colours: a named template, plus whatever the reader has overridden.
 *
 * <p><b>One place decides what colour an aspect is.</b> Before this, the answer lived in a
 * fifteen-case switch inside {@code SkymapPanel} that six callers read - the wheel, the grid,
 * the grid's legend, the interpretation panel, the hover card and {@code Prose}. That was
 * already the right shape (one switch, six readers); what it could not do is change. A reader
 * who finds red squares on green trines hard to tell apart had no recourse at all.
 *
 * <h2>Templates, then overrides</h2>
 *
 * A template is a complete set - every aspect has a colour in it, so choosing one can never
 * leave an aspect undefined. An override is a single aspect the reader has recoloured, and it
 * wins over the template. Clearing the overrides returns to the template exactly; there is no
 * third state where a chart is half one thing and half another.
 *
 * <h2>Why the templates are what they are</h2>
 *
 * <b>Classic</b> is what the app has always drawn, kept unchanged and kept as the default: a
 * reader who never opens Settings must see the chart they had yesterday.
 *
 * <b>Cosmic</b> is the palette from the reference mockups - cyan, violet and gold on a dark
 * ground, with the hard aspects warm and the soft ones cool. It is the only template that
 * changes the relationship between the colours rather than just their hue.
 *
 * <b>Muted</b> drops the saturation across the board. The wheel draws every aspect at once and
 * fades them by orb; at full saturation a chart with forty contacts is a bright tangle whatever
 * the fade does.
 */
public final class ChartPalette {

    private ChartPalette() { }

    /** Settings key for the chosen template. */
    public static final String TEMPLATE_KEY = "chart.palette";

    /** Settings key for per-aspect overrides, as {@code Label:#RRGGBB} pairs. */
    public static final String OVERRIDES_KEY = "chart.palette.custom";

    public static final String CLASSIC = "Classic";
    public static final String COSMIC = "Cosmic";
    public static final String COOL = "Cool";
    public static final String WARM = "Warm";
    public static final String MONO_DARK = "Black & White";  // black ink, white ground
    public static final String MONO_LIGHT = "White & Black";  // white ink, black ground

    /** The templates on offer, in the order the chooser lists them. */
    public static final String[] TEMPLATES = {
        CLASSIC, COSMIC, COOL, WARM, MONO_DARK, MONO_LIGHT};

    /**
     * A whole palette: every aspect, the four elements, the mansion ring and the wheel ground.
     *
     * <b>A template has to be complete or it is not a template.</b> The first version covered
     * only the fifteen aspect colours, which meant choosing "White & Black" gave black lines on
     * a black wheel - the ground was not part of what was being chosen. A partial template is
     * worse than none: it leaves the chart in a state no one designed.
     */
    private static final class Palette {
        final Map<String, String> aspects = new LinkedHashMap<>();
        final String[] elements = new String[4];
        String mansion;
        String wheel;
        /** Rings, spokes, the crosshair - the chart's structural lines. */
        String ink;
    }

    private static final Map<String, Palette> PALETTES = new LinkedHashMap<>();

    /** The fifteen aspect labels, in the order a palette lists its colours. */
    private static final String[] LABELS = {
        "Conjunction", "Sextile", "Square", "Trine", "Opposition", "Quincunx", "Semisextile",
        "Semisquare", "Quintile", "Sesquiquintile", "Sesquiquadrate", "Septile", "Novile",
        "Decile", "Biquintile"};

    static {
        // <b>Classic is exactly what the switch in SkymapPanel returned</b>, element colours
        // included, so selecting it is a no-op against every chart drawn before this class
        // existed. It is the way back from any other choice, which is the whole reason it is
        // spelled out here rather than being "whatever the code used to do".
        define(CLASSIC,
            new String[] {"#FFD700", "#00C8FF", "#FF3232", "#32FF32", "#FF6400", "#B36AE2",
                "#6FA8DC", "#E06666", "#F6C453", "#C9A227", "#CC7A5C", "#8E7CC3", "#76A5AF",
                "#A2C4A9", "#B8860B"},
            // Exactly SkymapPanel's FIRE/EARTH/AIR/WATER. They were a different four here,
            // so the Settings chips advertised colours the wheel never drew.
            new String[] {"#FF4500", "#32CD32", "#FFD700", "#1E90FF"},
            "#B5A0E3", "#000000", "#DCDCDC");

        // The reference mockups: cyan, violet and gold on a deep navy ground.
        define(COSMIC,
            new String[] {"#F5C451", "#38E0D0", "#FF5C8A", "#3FC8F5", "#FF8A5C", "#A78BFA",
                "#5EC8E8", "#E86A9A", "#E8C46A", "#C9A85A", "#E8926A", "#9B8AE0", "#6AC0C8",
                "#8AC8A8", "#C8A85A"},
            new String[] {"#FF7A6B", "#5FD8A4", "#F5C451", "#3FC8F5"},
            "#A78BFA", "#0B1020", "#4A5A80");

        // Cool: everything in the blue-violet half of the wheel, on a near-black blue ground.
        // The elements are cool too - a warm Fire glyph would be the only warm thing on screen
        // and would read as an error rather than as information.
        define(COOL,
            new String[] {"#9FE8F5", "#4FD8C0", "#5A7FE8", "#5FB8F5", "#7A6AE0", "#A78BFA",
                "#6FC8E8", "#6A8FE0", "#7FD8C8", "#6AB8C8", "#8A9FE8", "#9B8AE8", "#5FC0C8",
                "#8FD8C0", "#8A9FD8"},
            new String[] {"#8A7FE8", "#4FC8B0", "#7FD8F0", "#4F8FE8"},
            "#9B8AE8", "#080A18", "#3A4A78");

        define(WARM,
            new String[] {"#FFD98A", "#F2A65A", "#E05A3A", "#E8B45F", "#D86A2A", "#D88A5F",
                "#E8C08A", "#E07A4A", "#F2C46A", "#C89A4A", "#D8825A", "#C87A6A", "#D8A87A",
                "#E0C09A", "#C0904A"},
            new String[] {"#E8502A", "#C89A5A", "#F2C46A", "#D8825A"},
            "#D8A07A", "#140A06", "#6A4A38");

        // <b>Black & White is black ON white</b>, which is what the name says and the opposite
        // of what the first version did. Its counterpart is white on black. They were the wrong
        // way round, which is only visible once the ink and the ground move together.
        define(MONO_DARK,
            new String[] {"#000000", "#3A3A3A", "#141414", "#4A4A4A", "#242424", "#5E5E5E",
                "#727272", "#545454", "#404040", "#727272", "#5E5E5E", "#4F4F4F", "#686868",
                "#7C7C7C", "#595959"},
            new String[] {"#1A1A1A", "#404040", "#2C2C2C", "#545454"},
            "#5E5E5E", "#FFFFFF", "#333333");

        define(MONO_LIGHT,
            new String[] {"#FFFFFF", "#C8C8C8", "#E8E8E8", "#B4B4B4", "#DCDCDC", "#9A9A9A",
                "#8C8C8C", "#A0A0A0", "#B4B4B4", "#8C8C8C", "#9A9A9A", "#A8A8A8", "#8C8C8C",
                "#7E7E7E", "#969696"},
            new String[] {"#E8E8E8", "#BEBEBE", "#D2D2D2", "#A0A0A0"},
            "#9A9A9A", "#000000", "#C8C8C8");
    }

    private static void define(String name, String[] aspects, String[] elements,
                               String mansion, String wheel, String ink) {
        Palette p = new Palette();
        for (int i = 0; i < LABELS.length && i < aspects.length; i++) {
            p.aspects.put(LABELS[i], aspects[i]);
        }
        System.arraycopy(elements, 0, p.elements, 0, Math.min(4, elements.length));
        p.mansion = mansion;
        p.wheel = wheel;
        p.ink = ink;
        PALETTES.put(name, p);
    }

    /** Settings key for the structural lines. */
    public static final String INK_KEY = "chart.palette.ink";

    /**
     * The rings, spokes and crosshair.
     *
     * <b>Without this a template could not actually change the chart.</b> The structure was a
     * hardcoded light grey, so "Black &amp; White" - black glyphs on a white ground - drew its
     * rings in near-white on white and the wheel vanished, leaving glyphs floating in space.
     * A template that sets the background has to set the ink or it is not a template.
     */
    /**
     * The four angles - Ascendant, MC, Descendant, IC - and the bead they sit on.
     *
     * <b>Gold at two hardcoded call sites.</b> Gold on a white ground is barely visible, so
     * "Black &amp; White" drew four invisible angle markers; and gold in a cool palette is the
     * one warm thing on screen. Derived per template, with the historic gold as Classic.
     */
    public static String angleHex(String fallback) {
        String hex = Settings.get(ANGLE_KEY, "");
        if (isHex(hex)) {
            return hex;
        }
        String name = currentTemplate();
        if (MONO_DARK.equals(name)) {
            return "#2A2A2A";
        }
        if (MONO_LIGHT.equals(name)) {
            return "#D8D8D8";
        }
        if (COOL.equals(name)) {
            return "#8A9FE8";
        }
        if (COSMIC.equals(name)) {
            return "#F5C451";
        }
        if (WARM.equals(name)) {
            return "#E8A050";
        }
        return fallback;
    }

    public static void setAngleColor(String hex) {
        Settings.set(ANGLE_KEY, hex == null ? "" : hex);
    }

    /** Settings key for the page behind the wheel. */
    public static final String BACKGROUND_KEY = "chart.palette.background";

    /**
     * The page the wheel sits on, as distinct from the wheel itself.
     *
     * <b>These were one colour and should not have been.</b> "Wheel" filled the whole panel,
     * so choosing it repainted the entire page and the disc was never separately addressable -
     * there was no way to have, say, a near-black page with a slightly lifted wheel on it,
     * which is what every one of the reference mockups actually shows.
     */
    public static String backgroundHex(String fallback) {
        String hex = Settings.get(BACKGROUND_KEY, "");
        if (isHex(hex)) {
            return hex;
        }
        // A template's "wheel" value has always been its page colour, so it stays the default
        // here and the disc is what gets the new, lighter treatment below.
        String fromTemplate = palette(currentTemplate()).wheel;
        return fromTemplate != null ? fromTemplate : fallback;
    }

    public static void setBackgroundColor(String hex) {
        Settings.set(BACKGROUND_KEY, hex == null ? "" : hex);
    }

    /** Settings key for the angle markers. */
    public static final String ANGLE_KEY = "chart.palette.angle";

    public static String inkHex(String fallback) {
        String hex = Settings.get(INK_KEY, "");
        if (isHex(hex)) {
            return hex;
        }
        String fromTemplate = palette(currentTemplate()).ink;
        return fromTemplate != null ? fromTemplate : fallback;
    }

    public static void setInkColor(String hex) {
        Settings.set(INK_KEY, hex == null ? "" : hex);
    }

    /**
     * The colour for an aspect: the reader's override, else the template, else white.
     *
     * <b>White is the same last resort the switch had</b>, and it is reached the same way - by
     * a label no template knows. A new constant on {@code Aspects.Type} lands there until it is
     * given a colour, which is visible rather than silent.
     */
    public static String aspectHex(String aspectLabel) {
        String override = overrides().get(aspectLabel);
        if (override != null) {
            return override;
        }
        String hex = templateMap(currentTemplate()).get(aspectLabel);
        return hex != null ? hex : "#FFFFFF";
    }

    /** The template a template chooser should show as selected. */
    public static String currentTemplate() {
        String name = Settings.get(TEMPLATE_KEY, CLASSIC);
        for (String t : TEMPLATES) {
            if (t.equalsIgnoreCase(name)) {
                return t;
            }
        }
        // A saved template is a name too. It resolves through the override keys rather than
        // through PALETTES, so lookups still answer - see applyUserTemplate.
        for (String t : userTemplateNames()) {
            if (t.equals(name)) {
                return t;
            }
        }
        return CLASSIC;
    }

    /**
     * Chooses a template, <b>and clears every override</b>.
     *
     * The first version kept them, on the reasoning that an override is the reader's and a
     * template is the app's. That is wrong here: overrides are stored per aspect and per body,
     * so a reader who had recoloured six things and then picked "White & Black" would get a
     * white chart with six leftover colours from the old one and no way to tell which. Picking
     * a template means "make the chart look like this", and it now does.
     *
     * This is also what makes Classic a way back: selecting it restores exactly the chart the
     * app has always drawn, whatever was done in between.
     */
    public static void setTemplate(String name) {
        Settings.set(TEMPLATE_KEY, name);
        Settings.set(OVERRIDES_KEY, "");
        Settings.set(BODY_KEY, "");
        Settings.set(ELEMENT_KEY, "");
        Settings.set(MANSION_KEY, "");
        Settings.set(WHEEL_KEY, "");
    }

    /** What this template says, ignoring overrides - for showing a swatch's default. */
    public static String templateHex(String template, String aspectLabel) {
        String hex = templateMap(template).get(aspectLabel);
        return hex != null ? hex : "#FFFFFF";
    }

    private static Palette palette(String name) {
        Palette p = PALETTES.get(name);
        return p != null ? p : PALETTES.get(CLASSIC);
    }

    private static Map<String, String> templateMap(String name) {
        return palette(name).aspects;
    }

    /** The reader's per-aspect colours. Empty when nothing has been overridden. */
    public static Map<String, String> overrides() {
        Map<String, String> out = new LinkedHashMap<>();
        String csv = Settings.get(OVERRIDES_KEY, "");
        if (csv == null || csv.trim().isEmpty()) {
            return out;
        }
        for (String pair : csv.split(",")) {
            int colon = pair.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String label = pair.substring(0, colon).trim();
            String hex = pair.substring(colon + 1).trim();
            if (isHex(hex) && known(label)) {
                out.put(label, hex.toUpperCase());
            }
        }
        return out;
    }

    /** Overrides one aspect's colour, or clears it when {@code hex} is null. */
    public static void setOverride(String aspectLabel, String hex) {
        Map<String, String> map = overrides();
        if (hex == null) {
            map.remove(aspectLabel);
        } else if (isHex(hex)) {
            map.put(aspectLabel, hex.toUpperCase());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        Settings.set(OVERRIDES_KEY, sb.toString());
    }

    /** Drops every override, returning the chart to its template exactly. */
    public static void clearOverrides() {
        Settings.set(OVERRIDES_KEY, "");
    }

    // ---------------------------------------------------------------- bodies and elements

    /** Settings key for per-body colours, as {@code id:#RRGGBB} pairs. */
    public static final String BODY_KEY = "chart.palette.bodies";

    /** Settings key for the four element colours, as {@code index:#RRGGBB} pairs. */
    public static final String ELEMENT_KEY = "chart.palette.elements";

    /** Settings key for the lunar mansion ring. */
    public static final String MANSION_KEY = "chart.palette.mansion";

    /**
     * The colour a body is drawn in, or null to use its element's.
     *
     * <b>Per body is a new idea in this app.</b> Every body has always taken the colour of its
     * element - four colours doing the work of twenty-nine - so Sun and Mars were the same red
     * and always had been. That is a defensible scheme and it stays the default; this is the
     * layer above it, for a reader who wants Chiron to stop looking like Mars.
     */
    public static String bodyHex(String bodyId) {
        return pairs(BODY_KEY).get(bodyId);
    }

    public static void setBodyOverride(String bodyId, String hex) {
        putPair(BODY_KEY, bodyId, hex);
    }

    /**
     * One of the four element colours.
     *
     * <b>This is the control that actually moves the chart</b>, because bodies fall back to it
     * AND the sign glyphs read it directly - a sign has an element and nothing else. Changing
     * Fire here recolours Aries, Leo and Sagittarius along with every body in a fire sign,
     * which is worth knowing before wondering why one chip changed so much.
     */
    public static String elementHex(int elementIndex, String fallback) {
        String hex = pairs(ELEMENT_KEY).get(String.valueOf(elementIndex));
        if (hex != null) {
            return hex;
        }
        String[] fromTemplate = palette(currentTemplate()).elements;
        if (elementIndex >= 0 && elementIndex < fromTemplate.length
                && fromTemplate[elementIndex] != null) {
            return fromTemplate[elementIndex];
        }
        return fallback;
    }

    public static void setElementColor(int elementIndex, String hex) {
        putPair(ELEMENT_KEY, String.valueOf(elementIndex), hex);
    }

    /** Settings key for the wheel's own ground. */
    public static final String WHEEL_KEY = "chart.palette.wheel";

    /**
     * The ground the wheel is drawn on.
     *
     * <b>Black since the app existed</b>, and the last surface not on the theme - everything
     * around the chart moved to a navy ground, which left the wheel reading as a hole cut in
     * the window rather than the page it is.
     */
    /**
     * The disc the chart is drawn on.
     *
     * <b>A shade off the page rather than the same colour.</b> Each template's stored value is
     * its page; the disc is derived a step lighter on dark grounds and a step darker on light
     * ones, which is what lifts the wheel off the background instead of leaving it a hole in
     * it. Overridable, like everything else here.
     */
    public static String wheelHex(String fallback) {
        String hex = Settings.get(WHEEL_KEY, "");
        if (isHex(hex)) {
            return hex;
        }
        String page = palette(currentTemplate()).wheel;
        if (page == null) {
            return fallback;
        }
        return lift(page);
    }

    /** One step away from the page: lighter on a dark ground, darker on a light one. */
    private static String lift(String pageHex) {
        try {
            Color c = Color.decode(pageHex);
            double luma = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
            int step = luma > 140 ? -14 : 16;
            return String.format("#%02X%02X%02X",
                Math.max(0, Math.min(255, c.getRed() + step)),
                Math.max(0, Math.min(255, c.getGreen() + step)),
                Math.max(0, Math.min(255, c.getBlue() + step)));
        } catch (NumberFormatException e) {
            return pageHex;
        }
    }

    public static void setWheelColor(String hex) {
        Settings.set(WHEEL_KEY, hex == null ? "" : hex);
    }

    /** Settings key for the leader line from a body to its degree. */
    public static final String LEADER_KEY = "chart.palette.leader";

    /** The line joining a body to its exact degree. White by default, as it has always been. */
    public static String leaderHex(String fallback) {
        String hex = Settings.get(LEADER_KEY, "");
        return isHex(hex) ? hex : fallback;
    }

    public static void setLeaderColor(String hex) {
        Settings.set(LEADER_KEY, hex == null ? "" : hex);
    }

    /** The lunar mansion ring, which has been one hardcoded colour since it was drawn. */
    public static String mansionHex(String fallback) {
        String hex = Settings.get(MANSION_KEY, "");
        if (isHex(hex)) {
            return hex;
        }
        String fromTemplate = palette(currentTemplate()).mansion;
        return fromTemplate != null ? fromTemplate : fallback;
    }

    public static void setMansionColor(String hex) {
        Settings.set(MANSION_KEY, hex == null ? "" : hex);
    }

    private static Map<String, String> pairs(String key) {
        Map<String, String> out = new LinkedHashMap<>();
        String csv = Settings.get(key, "");
        if (csv == null || csv.trim().isEmpty()) {
            return out;
        }
        for (String pair : csv.split(",")) {
            int colon = pair.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String hex = pair.substring(colon + 1).trim();
            if (isHex(hex)) {
                out.put(pair.substring(0, colon).trim(), hex.toUpperCase());
            }
        }
        return out;
    }

    private static void putPair(String key, String id, String hex) {
        Map<String, String> map = pairs(key);
        if (hex == null) {
            map.remove(id);
        } else if (isHex(hex)) {
            map.put(id, hex.toUpperCase());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        Settings.set(key, sb.toString());
    }

    /** Any stored hex, as a Color, or the fallback when it is absent or malformed. */
    // ---------------------------------------------------------------- saved templates

    /** Settings key holding the names of the reader's own templates. */
    public static final String USER_LIST_KEY = "chart.palette.saved";

    /** Settings key prefix for one saved template's contents. */
    public static final String USER_PREFIX = "chart.palette.saved.";

    /** The reader's own templates, in the order they were saved. */
    public static java.util.List<String> userTemplateNames() {
        java.util.List<String> out = new java.util.ArrayList<>();
        String csv = Settings.get(USER_LIST_KEY, "");
        if (csv == null || csv.trim().isEmpty()) {
            return out;
        }
        for (String name : csv.split("\\|")) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /**
     * Saves everything currently in force under a name.
     *
     * <b>Stores the resolved colours, not the overrides.</b> A saved template that recorded
     * only what had been overridden would mean something different later: reload it after
     * switching the base template and the un-overridden half would come from somewhere else.
     * Every aspect, every element, the ring and the wheel are written out explicitly, so a
     * saved template reproduces the chart it was saved from whatever else has happened since.
     *
     * <b>Bodies are the exception and are stored as overrides</b>, because a body without one
     * is not a colour - it is the instruction "follow your element", and flattening that to a
     * hex would freeze it against later element changes.
     */
    public static void saveUserTemplate(String name) {
        String clean = name == null ? "" : name.trim().replace("|", " ").replace("=", " ");
        if (clean.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String label : LABELS) {
            sb.append("a:").append(label).append('=').append(aspectHex(label)).append(';');
        }
        for (int i = 0; i < 4; i++) {
            sb.append("e:").append(i).append('=').append(elementHex(i, "#FFFFFF")).append(';');
        }
        for (Map.Entry<String, String> e : pairs(BODY_KEY).entrySet()) {
            sb.append("b:").append(e.getKey()).append('=').append(e.getValue()).append(';');
        }
        sb.append("m:=").append(mansionHex("#B5A0E3")).append(';');
        sb.append("w:=").append(wheelHex("#000000")).append(';');
        Settings.set(USER_PREFIX + clean, sb.toString());

        java.util.List<String> names = userTemplateNames();
        if (!names.contains(clean)) {
            names.add(clean);
            Settings.set(USER_LIST_KEY, String.join("|", names));
        }
    }

    /**
     * Loads a saved template by writing its colours into the override keys.
     *
     * <b>Deliberately reuses the override machinery rather than adding a second lookup
     * path.</b> Everything downstream already resolves override-then-template, so a saved
     * template that lands in the overrides needs no new code anywhere - and cannot disagree
     * with the built-ins about precedence.
     */
    public static void applyUserTemplate(String name) {
        String blob = Settings.get(USER_PREFIX + name, "");
        if (blob == null || blob.trim().isEmpty()) {
            return;
        }
        Settings.set(OVERRIDES_KEY, "");
        Settings.set(BODY_KEY, "");
        Settings.set(ELEMENT_KEY, "");
        Settings.set(MANSION_KEY, "");
        Settings.set(WHEEL_KEY, "");
        for (String part : blob.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 1 || part.length() < 3) {
                continue;
            }
            String kind = part.substring(0, 2);
            String key = part.substring(2, eq);
            String hex = part.substring(eq + 1).trim();
            if (!isHex(hex)) {
                continue;
            }
            if ("a:".equals(kind)) {
                setOverride(key, hex);
            } else if ("e:".equals(kind)) {
                try {
                    setElementColor(Integer.parseInt(key), hex);
                } catch (NumberFormatException ignored) {
                    // A hand-edited settings file; skip the entry rather than refuse the load.
                }
            } else if ("b:".equals(kind)) {
                setBodyOverride(key, hex);
            } else if ("m:".equals(kind)) {
                setMansionColor(hex);
            } else if ("w:".equals(kind)) {
                setWheelColor(hex);
            }
        }
        Settings.set(TEMPLATE_KEY, name);
    }

    /** Forgets a saved template. The built-ins cannot be deleted. */
    public static void deleteUserTemplate(String name) {
        java.util.List<String> names = userTemplateNames();
        if (names.remove(name)) {
            Settings.set(USER_LIST_KEY, String.join("|", names));
            Settings.set(USER_PREFIX + name, "");
        }
    }

    /** True when this name is one of the reader's rather than a built-in. */
    public static boolean isUserTemplate(String name) {
        return userTemplateNames().contains(name);
    }

    public static Color colorOr(String hex, Color fallback) {
        try {
            return hex == null ? fallback : Color.decode(hex);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static Color colorFor(String aspectLabel) {
        try {
            return Color.decode(aspectHex(aspectLabel));
        } catch (NumberFormatException e) {
            // A malformed stored value must not take the chart down; white is the same answer
            // an unknown label gets.
            return Color.WHITE;
        }
    }

    /** #RRGGBB, and nothing else - this string is written into HTML and parsed by decode. */
    static boolean isHex(String s) {
        if (s == null || s.length() != 7 || s.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < 7; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean known(String label) {
        for (Aspects.Type t : Aspects.Type.values()) {
            if (t.label.equals(label)) {
                return true;
            }
        }
        return false;
    }

}
