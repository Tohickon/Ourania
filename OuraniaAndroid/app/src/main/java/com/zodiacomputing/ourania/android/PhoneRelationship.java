package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.astro.Aspects;
import com.zodiacomputing.ourania.astro.BodyScore;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.Gestalt;
import com.zodiacomputing.ourania.astro.Synastry;
import com.zodiacomputing.ourania.astro.Themes;
import com.zodiacomputing.ourania.astro.Topics;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.InterpretationService;
import com.zodiacomputing.ourania.gui.NarrativeSynthesizer;

import de.thmac.swisseph.SwissEph;

import java.util.ArrayList;
import java.util.List;

/**
 * Two people's charts on the phone (M10): synastry - one chart read against the other - and
 * the midpoint composite, the relationship as a chart of its own.
 *
 * <p><b>The desktop's engine and corpus, the phone's pages.</b> The contacts are
 * {@link Aspects#typeOf} under {@link Aspects.Profile#SYNASTRY} (the desktop's halved
 * cross-chart orbs), {@link Synastry#angleContacts} and {@link Synastry#houseOverlays}; the
 * composite is {@link ChartFrame#computeMidpointComposite}, the desktop's default composite;
 * and the words are the synastry and composite entries of the same corpus. The desktop's own
 * synastry page is built from desktop-only helpers, so the phone lays the same facts out for
 * a small screen instead of reusing it.
 *
 * <p>No Android in here, like {@link PhoneChart}: {@code PhoneRelationshipTest} runs it on the
 * JVM.
 */
final class PhoneRelationship {

    private PhoneRelationship() { }

    /** An aspect between one of A's planets and one of B's. */
    static final class Contact {
        /** A planet of the first chart (the wheel's inner ring), and one of the second's. */
        final int a;
        final int b;
        final Aspects.Type type;
        final double orb;

        Contact(int a, int b, Aspects.Type type, double orb) {
            this.a = a;
            this.b = b;
            this.type = type;
            this.orb = orb;
        }
    }

    /**
     * Every aspect between A's ten planets and B's, tightest first, at the synastry orbs and
     * leaving out the aspect types switched off in the settings, as the wheel does.
     */
    static List<Contact> contacts(ChartFrame a, ChartFrame b) {
        boolean[] shown = com.zodiacomputing.ourania.gui.Settings.loadAspectSelection();
        List<Contact> out = new ArrayList<>();
        for (int i = 0; i < PhoneChart.PLANETS && i < a.bodies.length; i++) {
            for (int j = 0; j < PhoneChart.PLANETS && j < b.bodies.length; j++) {
                ChartFrame.Body x = a.bodies[i];
                ChartFrame.Body y = b.bodies[j];
                if (x == null || y == null || !x.ok || !y.ok) {
                    continue;
                }
                double sep = Math.abs(x.lon - y.lon) % 360.0;
                if (sep > 180.0) {
                    sep = 360.0 - sep;
                }
                Aspects.Type t = Aspects.typeOf(sep, x.name, y.name, Aspects.Profile.SYNASTRY);
                if (t != null && (t.ordinal() >= shown.length || shown[t.ordinal()])) {
                    out.add(new Contact(i, j, t, Math.abs(sep - t.exactAngle)));
                }
            }
        }
        out.sort((p, q) -> Double.compare(p.orb, q.orb));
        return out;
    }

    /** The contacts as the wheel's dashed lines: the partner's planet outside, A's inside. */
    static List<PhoneWheel.Cross> crosses(List<Contact> contacts) {
        List<PhoneWheel.Cross> out = new ArrayList<>();
        for (Contact c : contacts) {
            out.add(new PhoneWheel.Cross(c.b, c.a, c.type));
        }
        return out;
    }

    /**
     * The synastry page: the aspects between the two, each read; then each chart's planets on
     * the other's angles; then where each one's planets fall in the other's houses.
     */
    static String synastry(String nameA, ChartFrame a, String nameB, ChartFrame b,
            InterpretationService svc) {
        StringBuilder h = new StringBuilder();
        h.append("<h2>").append(nameA).append(" and ").append(nameB).append("</h2>");
        List<Contact> contacts = contacts(a, b);
        h.append("<h3>Aspects between you (").append(contacts.size()).append(")</h3>");
        if (contacts.isEmpty()) {
            h.append("<p>No planet of one chart aspects a planet of the other within the "
                + "synastry orbs.</p>");
        }
        for (Contact c : contacts) {
            String pa = a.bodies[c.a].name;
            String pb = b.bodies[c.b].name;
            h.append("<h4>").append(possessive(nameA)).append(pa).append(' ')
                .append(c.type.label.toLowerCase()).append(' ').append(possessive(nameB))
                .append(pb).append(String.format(" <small>(%.1f&deg;)</small></h4>", c.orb));
            String text = svc.getSynastryInteraspect(pa, pb, c.type.label);
            if (PhoneReading.usable(text)) {
                h.append("<p>").append(text).append("</p>");
            }
        }
        angles(h, nameA, a, nameB, b, svc);
        angles(h, nameB, b, nameA, a, svc);
        overlays(h, nameA, a, nameB, b, svc);
        overlays(h, nameB, b, nameA, a, svc);
        return h.toString();
    }

    private static void angles(StringBuilder h, String visitor, ChartFrame v, String host,
            ChartFrame o, InterpretationService svc) {
        if (o.timeUnknown) {
            return;                                     // no angles to land on
        }
        List<Synastry.AngleContact> list = Synastry.angleContacts(v, o);
        if (list.isEmpty()) {
            return;
        }
        h.append("<h3>").append(possessive(visitor)).append("planets on ")
            .append(possessive(host)).append("angles</h3>");
        for (Synastry.AngleContact c : list) {
            h.append("<h4>").append(c.body).append(" on the ").append(c.angle)
                .append(String.format(" <small>(%.1f&deg;)</small></h4>", c.orb));
            String text = svc.getOverlayPlanetAngle(c.body, c.angle);
            if (!PhoneReading.usable(text)) {
                text = svc.getAngleContact(c.angle.toLowerCase());
            }
            if (PhoneReading.usable(text)) {
                h.append("<p>").append(text).append("</p>");
            }
        }
    }

    private static void overlays(StringBuilder h, String visitor, ChartFrame v, String host,
            ChartFrame o, InterpretationService svc) {
        if (o.timeUnknown) {
            return;                                     // no houses to fall in
        }
        StringBuilder rows = new StringBuilder();
        for (Synastry.Overlay ov : Synastry.houseOverlays(v, o)) {
            if (ov.bodyIndex < 0 || ov.bodyIndex >= PhoneChart.PLANETS || ov.house < 1) {
                continue;
            }
            rows.append("<h4>").append(ov.body).append(" in house ").append(ov.house)
                .append("</h4>");
            String text = svc.getOverlayPlanetHouse(ov.body, ov.house);
            if (PhoneReading.usable(text)) {
                rows.append("<p>").append(text).append("</p>");
            }
        }
        if (rows.length() > 0) {
            h.append("<h3>").append(possessive(visitor)).append("planets in ")
                .append(possessive(host)).append("houses</h3>").append(rows);
        }
    }

    /** One of B's planets, tapped on the outer ring: its place in A's chart and its contacts. */
    static String partnerPlanet(String nameA, ChartFrame a, String nameB, ChartFrame b, int j,
            InterpretationService svc) {
        if (j < 0 || j >= PhoneChart.PLANETS || j >= b.bodies.length || b.bodies[j] == null
                || !b.bodies[j].ok) {
            return "";
        }
        ChartFrame.Body p = b.bodies[j];
        String sign = capital(Zodiac.signName(p.lon));
        int house = a.timeUnknown ? 0 : Zodiac.houseOf(p.lon, a.cusps);
        StringBuilder h = new StringBuilder();
        h.append("<h2>").append(possessive(nameB)).append(p.name).append(" in ").append(sign);
        if (house > 0) {
            h.append(", ").append(possessive(nameA)).append("house ").append(house);
        }
        h.append("</h2><p><i>").append(Zodiac.format(p.lon)).append("</i></p>");
        if (house > 0) {
            String text = svc.getOverlayPlanetHouse(p.name, house);
            if (PhoneReading.usable(text)) {
                h.append("<h3>In ").append(possessive(nameA)).append("house ").append(house)
                    .append("</h3><p>").append(text).append("</p>");
            }
        }
        StringBuilder mine = new StringBuilder();
        for (Contact c : contacts(a, b)) {
            if (c.b != j) {
                continue;
            }
            String pa = a.bodies[c.a].name;
            mine.append("<h4>").append(c.type.label).append(' ').append(possessive(nameA))
                .append(pa).append(String.format(" <small>(%.1f&deg;)</small></h4>", c.orb));
            String text = svc.getSynastryInteraspect(pa, p.name, c.type.label);
            if (PhoneReading.usable(text)) {
                mine.append("<p>").append(text).append("</p>");
            }
        }
        if (mine.length() > 0) {
            h.append("<h3>Aspects to ").append(possessive(nameA)).append("planets</h3>")
                .append(mine);
        }
        return h.toString();
    }

    /**
     * The two charts' midpoint composite, with its houses derived at the reader's chosen place
     * ({@link com.zodiacomputing.ourania.gui.Settings#compositeReference}) or, by default, the
     * midpoint of the two birthplaces - as the desktop casts it.
     */
    static ChartFrame composite(SwissEph sw, ChartFrame a, ChartFrame b) {
        double[] ref = com.zodiacomputing.ourania.gui.Settings.compositeReference();
        return ref == null ? ChartFrame.computeMidpointComposite(sw, a, b)
            : ChartFrame.computeMidpointComposite(sw, a, b, ref[0], ref[1]);
    }

    /**
     * The Davison chart: a real chart cast for the moment and place halfway between the two
     * births. Not an average of two charts, so it takes no reference place.
     */
    static ChartFrame davison(SwissEph sw, ChartFrame a, ChartFrame b) {
        return ChartFrame.computeDavisonComposite(sw, a, b);
    }

    /** Where the composite's houses are cast, in words, for the screen. */
    static String compositePlace() {
        if (com.zodiacomputing.ourania.gui.Settings.compositeReference() == null) {
            return "the midpoint of the two birthplaces";
        }
        String n = com.zodiacomputing.ourania.gui.Settings.compositeReferenceName();
        return n != null ? n : "a chosen place";
    }

    /** A planet of the composite: the relationship's, read from the composite corpus. */
    static String compositePlanet(ChartFrame c, int i, InterpretationService svc) {
        if (i < 0 || i >= PhoneChart.PLANETS || i >= c.bodies.length || c.bodies[i] == null
                || !c.bodies[i].ok) {
            return "";
        }
        ChartFrame.Body p = c.bodies[i];
        String sign = capital(Zodiac.signName(p.lon));
        int house = c.timeUnknown ? 0 : Zodiac.houseOf(p.lon, c.cusps);
        StringBuilder h = new StringBuilder();
        h.append("<h2>The relationship's ").append(p.name).append(" in ").append(sign);
        if (house > 0) {
            h.append(", house ").append(house);
        }
        h.append("</h2><p><i>").append(Zodiac.format(p.lon)).append(" in the composite</i></p>");
        String body = svc.getCompositeBody(com.zodiacomputing.ourania.astro.Bodies.at(i).id);
        if (PhoneReading.usable(body)) {
            h.append("<p>").append(body).append("</p>");
        }
        String inSign = svc.getCompositePlanetSign(p.name, sign);
        if (PhoneReading.usable(inSign)) {
            h.append("<h3>").append(p.name).append(" in ").append(sign).append("</h3><p>")
                .append(inSign).append("</p>");
        }
        if (house > 0) {
            String inHouse = svc.getCompositePlanetHouse(p.name, house);
            if (PhoneReading.usable(inHouse)) {
                h.append("<h3>In house ").append(house).append("</h3><p>").append(inHouse)
                    .append("</p>");
            }
        }
        StringBuilder aspects = new StringBuilder();
        for (PhoneWheel.Line l : PhoneWheel.aspectsOf(c, PhoneChart.PLANETS)) {
            if (l.a != i && l.b != i) {
                continue;
            }
            String other = c.bodies[l.a == i ? l.b : l.a].name;
            aspects.append("<h4>").append(l.type.label).append(' ').append(other).append("</h4>");
            String text = svc.getCompositeAspect(p.name, other, l.type.label);
            if (PhoneReading.usable(text)) {
                aspects.append("<p>").append(text).append("</p>");
            }
        }
        if (aspects.length() > 0) {
            h.append("<h3>Aspects</h3>").append(aspects);
        }
        return h.toString();
    }

    /** The desktop's Synthesize for a composite: the relationship read as a chart of its own. */
    static String compositeSynthesis(ChartFrame c) {
        Gestalt.Result g = Gestalt.compute(c);
        List<BodyScore.Vector> ranked = BodyScore.rank(c, g);
        Themes.Result themes = Themes.extract(c, g, ranked, Topics.analyse(c, ranked));
        return PhoneReading.forPhone(NarrativeSynthesizer.generateReport(c, g, ranked, themes,
            null, null, null, null, null, false, true));
    }

    /** "Jane's " - and "your " for the chart on screen when it has no name. */
    static String possessive(String name) {
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("you")) {
            return "your ";
        }
        return name + (name.endsWith("s") ? "' " : "'s ");
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
