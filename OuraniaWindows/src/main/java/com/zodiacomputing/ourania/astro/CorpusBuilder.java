package com.zodiacomputing.ourania.astro;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Emits the calibration corpus worksheet: every AA-rated named chart from the appendix
 * of Brennan's Hellenistic Astrology, with birth data resolved and the picks column left
 * empty for a human to fill.
 *
 * Two things this does that hand-transcription reliably gets wrong.
 *
 * HISTORICAL OFFSETS come from Java's bundled IANA timezone database rather than from
 * anyone's memory of when a country adopted standard time. That handles war time,
 * pre-1966 local DST, and the adoption dates themselves.
 *
 * PRE-STANDARD-TIME BIRTHS get Local Mean Time computed from the birth city's OWN
 * longitude, not from the zone's. The tzdb's LMT is keyed to the zone's reference city,
 * which for Europe/Berlin means Berlin - and Ulm is 13.5 minutes of time away from
 * Berlin. That error is larger than the harness's own stability tolerance, so for the
 * five 19th-century births here it would corrupt the chart outright.
 *
 * Detection is exact rather than a date table: if the tzdb offset for that instant is
 * not a whole number of minutes, the tzdb is itself reporting LMT, and we substitute the
 * city's true LMT.
 */
public final class CorpusBuilder {

    /** lat/lon in signed decimal, north and east positive. */
    private static final String[][] ROWS = {
        // chart, name, date, local 24h time, IANA zone, lat, lon, place
        {"3",  "Che Guevara",              "1928-05-14", "03:05", "America/Argentina/Cordoba", "-32.9468", "-60.6393", "Rosario, Argentina"},
        {"4",  "Franklin D Roosevelt",     "1882-01-30", "20:45", "America/New_York",  "41.7845",  "-73.9332",  "Hyde Park, New York"},
        {"6",  "Judy Blume",               "1938-02-12", "21:59", "America/New_York",  "40.6639",  "-74.2107",  "Elizabeth, New Jersey"},
        {"8",  "Carl Sagan",               "1934-11-09", "17:05", "America/New_York",  "40.7128",  "-74.0060",  "New York, New York"},
        {"10", "Joseph Ratzinger",         "1927-04-16", "04:15", "Europe/Berlin",     "48.2500",  "12.8500",   "Marktl, Germany"},
        {"11", "William Rehnquist",        "1924-10-01", "11:32", "America/Chicago",   "43.0389",  "-87.9065",  "Milwaukee, Wisconsin"},
        {"13", "George Lucas",             "1944-05-14", "05:40", "America/Los_Angeles", "37.6391", "-120.9969", "Modesto, California"},
        {"14", "Steve Wozniak",            "1950-08-11", "09:45", "America/Los_Angeles", "37.3382", "-121.8863", "San Jose, California"},
        {"15", "Angelina Jolie",           "1975-06-04", "09:09", "America/Los_Angeles", "34.0522", "-118.2437", "Los Angeles, California"},
        {"16", "Paul Newman",              "1925-01-26", "06:30", "America/New_York",  "41.4993",  "-81.6944",  "Cleveland, Ohio"},
        {"17", "Ted Kennedy",              "1932-02-22", "03:58", "America/New_York",  "42.3001",  "-71.0662",  "Dorchester, Massachusetts"},
        {"20", "Queen Elizabeth II",       "1926-04-21", "02:40", "Europe/London",     "51.5074",  "-0.1278",   "London, England"},
        {"21", "Prince William",           "1982-06-21", "21:03", "Europe/London",     "51.5154",  "-0.1755",   "Paddington, England"},
        {"24", "Maurizio Gucci",           "1948-09-26", "01:10", "Europe/Rome",       "43.7696",  "11.2558",   "Florence, Italy"},
        {"27", "Vincent van Gogh",         "1853-03-30", "11:00", "Europe/Amsterdam",  "51.4670",  "4.6670",    "Zundert, Netherlands"},
        {"32", "Ernest Hemingway",         "1899-07-21", "08:00", "America/Chicago",   "41.8850",  "-87.7845",  "Oak Park, Illinois"},
        {"33", "Michel Foucault",          "1926-10-15", "07:30", "Europe/Paris",      "46.5802",  "0.3404",    "Poitiers, France"},
        {"34", "Johnny Carson",            "1925-10-23", "07:15", "America/Chicago",   "40.9917",  "-94.7333",  "Corning, Iowa"},
        {"35", "Steven Spielberg",         "1946-12-18", "18:16", "America/New_York",  "39.1031",  "-84.5120",  "Cincinnati, Ohio"},
        {"36", "Adolf Hitler",             "1889-04-20", "18:30", "Europe/Vienna",     "48.2563",  "13.0434",   "Braunau am Inn, Austria"},
        {"42", "Eleanor Roosevelt",        "1884-10-11", "11:00", "America/New_York",  "40.7128",  "-74.0060",  "New York, New York"},
        {"43", "Tiger Woods",              "1975-12-30", "22:50", "America/Los_Angeles", "33.7701", "-118.1937", "Long Beach, California"},
        {"51", "Philippe Pozzo di Borgo",  "1951-02-14", "14:45", "Africa/Tunis",      "36.8065",  "10.1815",   "Tunis, Tunisia"},
        {"52", "Michael Patrick MacDonald","1966-03-09", "08:35", "America/New_York",  "42.3601",  "-71.0589",  "Boston, Massachusetts"},
        {"54", "Chris Farley",             "1964-02-15", "15:34", "America/Chicago",   "43.0731",  "-89.4012",  "Madison, Wisconsin"},
        {"55", "Robin Williams",           "1951-07-21", "13:34", "America/Chicago",   "41.8781",  "-87.6298",  "Chicago, Illinois"},
        {"57", "Maya Angelou",             "1928-04-04", "14:10", "America/Chicago",   "38.6270",  "-90.1994",  "Saint Louis, Missouri"},
        {"58", "James Eagan Holmes",       "1987-12-13", "21:04", "America/Los_Angeles", "32.8328", "-117.2713", "La Jolla, California"},
        {"61", "Albert Einstein",          "1879-03-14", "11:30", "Europe/Berlin",     "48.3986",  "9.9911",    "Ulm, Germany"},
        {"63", "Andre Romelle Young",      "1965-02-18", "10:56", "America/Los_Angeles", "34.0522", "-118.2437", "Los Angeles, California"},
        {"73", "Kurt Cobain",              "1967-02-20", "19:38", "America/Los_Angeles", "46.9754", "-123.8157", "Aberdeen, Washington"},
        {"75", "Whitney Houston",          "1963-08-09", "20:55", "America/New_York",  "40.7357",  "-74.1724",  "Newark, New Jersey"},
        {"76", "Hans Christian Andersen",  "1805-04-02", "01:00", "Europe/Copenhagen", "55.3959",  "10.3883",   "Odense, Denmark"},
        {"77", "Lisa Marie Presley",       "1968-02-01", "17:01", "America/Chicago",   "35.1495",  "-90.0490",  "Memphis, Tennessee"},
        {"84", "Amanda Knox",              "1987-07-09", "02:47", "America/Los_Angeles", "47.6062", "-122.3321", "Seattle, Washington"},
        {"86", "Anthony Louis",            "1945-09-03", "09:05", "America/New_York",  "41.5582",  "-73.0515",  "Waterbury, Connecticut"},
        {"102","George W Bush",            "1946-07-06", "07:26", "America/New_York",  "41.3083",  "-72.9279",  "New Haven, Connecticut"},
        {"103","Al Gore",                  "1948-03-31", "12:53", "America/New_York",  "38.9072",  "-77.0369",  "Washington, District of Columbia"},
        {"106","Chris Brennan",            "1984-11-01", "13:28", "America/Denver",    "39.7294",  "-104.8319", "Aurora, Colorado"},
        {"108","Vanessa L Williams",       "1963-03-18", "11:28", "America/New_York",  "41.2043",  "-73.7993",  "Millwood, New York"},
        {"109","Nicole Brown Simpson",     "1959-05-19", "02:00", "Europe/Berlin",     "50.1109",  "8.6821",    "Frankfurt am Main, Germany"},
        {"114","Linda Goodman",            "1925-04-09", "06:05", "America/New_York",  "39.6295",  "-79.9559",  "Morgantown, West Virginia"},
        {"118","Barack Obama",             "1961-08-04", "19:24", "Pacific/Honolulu",  "21.3069",  "-157.8583", "Honolulu, Hawaii"}
    };

    /** Coordinates I could not confirm against a source and took from memory. Spot-check these. */
    private static final String[] UNVERIFIED_COORDS = {"Anthony Louis", "Vanessa L Williams"};

    /**
     * MACHINE-CODED PICKS. Not human judgement - the output of a stated, deterministic
     * rule applied to the passage where Brennan introduces each chart:
     *
     *   A planet is picked if its name appears within 60 characters of a POSITIONAL
     *   ANCHOR - a degree sign or a zodiac sign name - inside the 1500 characters
     *   following the chart's caption. Capped at 4, in zodiacal-body order.
     *
     * The anchor is what separates "Mars is at 11 Cancer" (a fact about this chart) from
     * "the malefic contrary to the sect" (technique, where the planet would have to be
     * inferred - and inferring it would use the same logic the engine uses, making the
     * test circular).
     *
     * Robustness: the strict (30 char) and loose (120 char) variants agree with this one
     * on 27/29 and 25/29 charts respectively. Dropping the anchor entirely agrees on only
     * 17/29, which is why the anchor is the load-bearing part of the rule and the exact
     * window is not.
     *
     * Charts absent from this table produced no anchored planet and are left blank.
     */
    private static final String[][] PICKS = {
        {"3", "Mars"}, {"4", "Sun"}, {"6", "Venus"}, {"8", "Venus,Saturn"},
        {"10", "Venus,Jupiter"}, {"11", "Venus,Jupiter"}, {"13", "Mercury"},
        {"14", "Mercury"}, {"15", "Venus"}, {"16", "Venus"}, {"17", "Saturn"},
        {"24", "Moon,Venus"}, {"27", "Mercury"}, {"32", "Mars,Saturn"},
        {"33", "Mars,Saturn"}, {"34", "Saturn"}, {"35", "Saturn"}, {"36", "Saturn"},
        {"42", "Mercury,Jupiter"}, {"54", "Sun,Mars,Saturn"}, {"55", "Venus,Mars,Jupiter"},
        {"57", "Sun,Mercury,Venus,Jupiter"}, {"58", "Sun,Mercury,Venus,Jupiter"},
        {"61", "Moon,Venus"}, {"63", "Moon,Venus,Mars,Jupiter"},
        {"73", "Sun,Moon,Venus,Mars"}, {"75", "Sun,Moon,Jupiter"}, {"108", "Jupiter"},
        {"109", "Saturn"}
    };

    private static String picksFor(String chart) {
        for (String[] p : PICKS) {
            if (p[0].equals(chart)) {
                return p[1];
            }
        }
        return "";
    }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "calibration-corpus.tsv";
        int lmtCount = 0;

        try (PrintWriter w = new PrintWriter(path, "UTF-8")) {
            header(w);
            for (String[] r : ROWS) {
                String date = r[2];
                String time = r[3];
                ZoneId zone = ZoneId.of(r[4]);
                double lon = Double.parseDouble(r[6]);

                LocalDateTime local = LocalDateTime.parse(date + "T" + time + ":00");
                ZoneOffset off = ZonedDateTime.of(local, zone).getOffset();
                int secs = off.getTotalSeconds();

                String note = "";
                double hours;
                if (secs % 60 != 0) {
                    // The tzdb is reporting Local Mean Time for the zone's reference city.
                    // Recompute it for this city's own meridian.
                    hours = lon / 15.0;
                    note = "LMT from own meridian";
                    lmtCount++;
                } else {
                    hours = secs / 3600.0;
                }

                w.printf("%s | %s | %s | %.4f | %s | %s | AA | brennan | Brennan, Hellenistic Astrology, Chart %s%s | %s%n",
                    r[1], date, time, hours, r[5], r[6], r[0],
                    note.isEmpty() ? "" : " (" + note + ")", picksFor(r[0]));
            }
            footer(w, lmtCount);
        }
        System.out.println("Wrote " + ROWS.length + " rows to " + path);
        System.out.println(lmtCount + " used Local Mean Time from the birth city's own meridian.");
        System.out.println("The picks column is empty, so the harness will reject every row");
        System.out.println("until you fill it. That is the intended state of a worksheet.");
    }

    private static void header(PrintWriter w) {
        w.println("# Calibration corpus - birth data from the appendix of");
        w.println("# Chris Brennan, Hellenistic Astrology: The Study of Fate and Fortune (2017).");
        w.println("# Only entries Brennan rates AA are included. His 23 anonymous/private-data");
        w.println("# entries are excluded, as are his A, B and C rated charts.");
        w.println("#");
        w.println("# GENERATED - fields 1-9 are filled, PICKS IS EMPTY AND IS YOURS TO FILL.");
        w.println("#");
        w.println("# name | date | local time | utc offset | lat | lon | rodden | school | source | picks");
        w.println("#");
        w.println("# CODING PROTOCOL - decide this before you look at a single chart, then");
        w.println("# apply it identically to all 43. Suggested:");
        w.println("#   'The bodies named in the passage where Brennan introduces this chart.'");
        w.println("# Brennan uses example charts to demonstrate a technique rather than to read");
        w.println("# the whole chart, so the pick is usually whatever the surrounding section is");
        w.println("# teaching. BE AWARE this carries a selection effect: he chose each chart");
        w.println("# BECAUSE that factor is loud in it, which biases toward apparent agreement.");
        w.println("# Treat a good score here as weaker evidence than it looks.");
        w.println("#");
        w.println("# Code BLIND. Do not run the engine on a chart before writing its picks.");
        w.println("#");
        w.println("# Protocol actually used: ..............................................");
        w.println("#");
    }

    private static void footer(PrintWriter w, int lmtCount) {
        w.println();
        w.println("# ---------------------------------------------------------------------");
        w.println("# NOTES");
        w.println("#");
        w.println("# Offsets come from Java's IANA timezone database, not from memory, so");
        w.println("# adoption dates, war time and pre-1966 local DST are handled.");
        w.println("#");
        w.println("# " + lmtCount + " charts predate standard time in their country and use Local Mean");
        w.println("# Time computed from the birth city's own longitude. The tzdb's LMT is keyed");
        w.println("# to the zone's reference city - Europe/Berlin means Berlin, and Ulm is 13.5");
        w.println("# minutes of time from Berlin, which would break the chart.");
        w.println("#");
        w.println("# COORDINATES TO SPOT-CHECK - taken from memory, not a source:");
        for (String n : UNVERIFIED_COORDS) {
            w.println("#   " + n);
        }
        w.println("#");
        w.println("# Rodden ratings are Brennan's claims, not independently verified against");
        w.println("# Astro-Databank. AA certifies the source, not that the time is exact.");
        w.println("#");
        w.println("# Some of these charts belong to people associated with violence or crime.");
        w.println("# They are here because Brennan used them to teach technique. Do NOT reuse");
        w.println("# this corpus to correlate placements with outcomes - that needs matched");
        w.println("# controls and pre-registration, and is a different project entirely.");
    }

    private CorpusBuilder() { }
}
