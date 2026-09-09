package com.zodiacomputing.ourania.astro;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * One thing a chart can be cast for: a person, an event, or a moment of sky.
 *
 * <b>The input to a {@link ChartFrame}, which is the output.</b> A frame is what the ephemeris
 * returns; a subject is what you asked it about. Keeping the question in its own type is what
 * this class is for, and the reason is a defect rather than a preference.
 *
 * <p><b>The defect.</b> The wheel's three charts were nine loose fields on the panel and eleven
 * positional strings across the boundary that filled them - and which of those strings meant
 * what depended on the chart mode. One row of the setup form carried Chart B's birth data in a
 * synastry and the moment of the sky everywhere else, so the ring chips could switch the
 * meaning of a field without switching the value in it. The sky ring drew a birth chart; the
 * partner ring drew this moment; the sky cast its houses for the second person's birthplace;
 * and pressing Play walked person B's birth time forward a day at a time. Four faults, one
 * cause, and every one of them was writeable because nothing in the type system knew that
 * "Chart B" and "the sky" were different things.
 *
 * <p>With a subject you cannot hand Chart B's record to the sky by accident. That is the whole
 * argument for it: not tidiness, but that the class of bug stops being expressible.
 *
 * <p><b>Every subject is the same shape.</b> The old fields were asymmetric - Chart A had a
 * time zone override and a Rodden rating, Chart B and the sky had neither, and the sky had no
 * place of its own at all until it was given one. A moment is a moment and a place is a place,
 * whoever they belong to. This mirrors the model the established libraries settled on: in
 * kerykeion a transit is built by the same factory as a person, with the same fields, and a
 * chart is a function of subjects rather than a mode that reinterprets fields.
 *
 * <p><b>Immutable.</b> The transport moves the sky by producing a new subject, not by editing
 * one - so a stale reference cannot quietly become a different chart while something is
 * reading it.
 */
public final class ChartSubject {

    /** What this subject is called on screen: "Chart A", "Chart B", "Sky". */
    public final String label;

    /**
     * The moment, in its own zone, or null when nothing has been entered.
     *
     * <b>Null is the honest answer for an empty form, and "now" is not.</b> The panel used to
     * default this to the present, so a chart nobody had entered was indistinguishable from a
     * chart of this instant - which is how the app came to open on a natal wheel showing
     * today's sky while the setup screen beside it held a birth date all along.
     */
    public final ZonedDateTime moment;

    /** The place as the reader typed it, for labels. May be empty, never null. */
    public final String placeName;

    /** Degrees north. */
    public final double latitude;

    /** Degrees east. */
    public final double longitude;

    /** The IANA zone the moment is read in. Never null. */
    public final String zoneId;

    /**
     * Whether the birth time is unknown, in which case the chart is cast for noon and the
     * angles are withheld. See {@link ChartFrame#computeTimeUnknown}.
     */
    public final boolean timeUnknown;

    private ChartSubject(String label, ZonedDateTime moment, String placeName,
            double latitude, double longitude, String zoneId, boolean timeUnknown) {
        this.label = label == null ? "" : label;
        this.moment = moment;
        this.placeName = placeName == null ? "" : placeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoneId = zoneId == null || zoneId.isEmpty()
            ? ZoneId.systemDefault().getId() : zoneId;
        this.timeUnknown = timeUnknown;
    }

    /** A subject nobody has entered: it has a name and nothing else. */
    public static ChartSubject empty(String label) {
        return new ChartSubject(label, null, "", 0.0, 0.0,
            ZoneId.systemDefault().getId(), false);
    }

    /** A subject with everything known. */
    public static ChartSubject of(String label, ZonedDateTime moment, String placeName,
            double latitude, double longitude, String zoneId, boolean timeUnknown) {
        return new ChartSubject(label, moment, placeName, latitude, longitude, zoneId,
            timeUnknown);
    }

    /**
     * The same subject at a different moment.
     *
     * <b>This is how the transport moves the sky.</b> It used to step whichever
     * ZonedDateTime field the mode happened to point at, which is how pressing Play on a
     * synastry walked the second person's birth moment forward a day at a time. Moving a
     * subject returns a new subject, so the thing that moves is named.
     */
    public ChartSubject at(ZonedDateTime when) {
        return new ChartSubject(this.label, when, this.placeName, this.latitude,
            this.longitude, this.zoneId, this.timeUnknown);
    }

    /** The same subject somewhere else. */
    public ChartSubject movedTo(String placeName, double latitude, double longitude,
            String zoneId) {
        return new ChartSubject(this.label, this.moment, placeName, latitude, longitude,
            zoneId, this.timeUnknown);
    }

    /** The same subject with its time-unknown flag set. */
    public ChartSubject withTimeUnknown(boolean unknown) {
        return new ChartSubject(this.label, this.moment, this.placeName, this.latitude,
            this.longitude, this.zoneId, unknown);
    }

    /** Whether there is a chart here at all. */
    public boolean entered() {
        return this.moment != null;
    }

    /** This subject's moment now, in its own zone - for a sky that follows the clock. */
    public ChartSubject atNow() {
        return this.at(ZonedDateTime.now(ZoneId.of(this.zoneId)));
    }

    /**
     * One short line naming this chart, for a control that has to say what it will draw.
     *
     * <b>Not toString.</b> That one is for a log and prints the coordinates; this is for a
     * tooltip and prints what a reader would recognise - the date they typed and the place
     * they named. A subject nobody has entered says so rather than showing a default.
     */
    public String summary() {
        if (this.moment == null) {
            return "not entered";
        }
        String when = this.moment.format(
            java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"));
        return this.placeName.isEmpty() ? when : when + "  \u00b7  " + this.placeName;
    }

    @Override
    public String toString() {
        return this.label + (this.moment == null ? " (not entered)"
            : " " + this.moment + " at " + this.placeName
                + String.format(" (%.4f, %.4f)", this.latitude, this.longitude)
                + (this.timeUnknown ? " time unknown" : ""));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChartSubject)) {
            return false;
        }
        ChartSubject o = (ChartSubject) other;
        return this.label.equals(o.label)
            && (this.moment == null ? o.moment == null : this.moment.equals(o.moment))
            && this.placeName.equals(o.placeName)
            && Double.compare(this.latitude, o.latitude) == 0
            && Double.compare(this.longitude, o.longitude) == 0
            && this.zoneId.equals(o.zoneId)
            && this.timeUnknown == o.timeUnknown;
    }

    @Override
    public int hashCode() {
        int h = this.label.hashCode();
        h = 31 * h + (this.moment == null ? 0 : this.moment.hashCode());
        h = 31 * h + this.placeName.hashCode();
        h = 31 * h + Double.hashCode(this.latitude);
        h = 31 * h + Double.hashCode(this.longitude);
        h = 31 * h + this.zoneId.hashCode();
        return 31 * h + (this.timeUnknown ? 1 : 0);
    }
}
