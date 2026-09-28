package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.Rodden;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bringing charts in from other software (G1).
 *
 * <p><b>The row called this "the single biggest barrier to anyone adopting the app", and it was
 * right</b>: a practitioner with three hundred clients in Solar Fire is not going to retype them.
 *
 * <p><b>CSV, and deliberately only CSV.</b> Solar Fire, Astro-Databank and every other program in
 * this field can write one, and a CSV whose columns are named is a format this can read without
 * anybody's specification. AAF is not implemented, and that is a decision rather than an oversight:
 * it is a binary-ish record format I do not have the specification for, and <b>a parser written
 * from a guess at a birth-data format does not fail loudly - it produces charts that are quietly
 * wrong</b>, which is the worst thing this program can do. It is named as not-done on the master
 * list instead.
 *
 * <p><b>An ambiguous date is refused, not guessed.</b> {@code 03/04/1985} is the third of April in
 * Britain and the fourth of March in America, and no amount of looking at one row tells you which.
 * Guessing splits a practitioner's book silently down the middle. So a slash date is taken only
 * when the day is above twelve and therefore unambiguous, or when the whole file agrees on an
 * order that its unambiguous rows establish; anything else is reported as a problem with its line
 * number and left out.
 *
 * <p><b>Nothing is written until the caller asks.</b> {@link #read} parses and reports; {@link #add}
 * is what touches the chart book, and it never overwrites: a name already in the book comes in
 * beside it, numbered.
 */
final class ChartImport {

    private ChartImport() { }

    /** One chart read out of a file, with nothing decided about it yet. */
    static final class Candidate {
        String name = "";
        String date = "";
        String time = "";
        String place = "";
        Rodden rodden = Rodden.A;
        String notes = "";
        String tags = "";
        int line;

        @Override
        public String toString() {
            return name + " " + date + " " + time + " " + place;
        }
    }

    /** What a file turned into: the charts, and everything that could not be read. */
    static final class Result {
        final List<Candidate> charts = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        /** Which order the file's slash dates were taken in, for the report. */
        String dateOrder = "none";
    }

    /**
     * The column names other programs use, mapped to what this one calls them.
     *
     * <b>Aliases rather than a fixed order, because the order is whatever the exporter felt
     * like.</b> Every name here is lowercased and stripped of spaces and underscores before
     * matching, so "Birth Date", "birth_date" and "BIRTHDATE" are one column.
     */
    private static final String[][] COLUMNS = {
        {"name", "name", "chartname", "subject", "person", "fullname"},
        {"date", "date", "birthdate", "bdate", "dob", "dateofbirth"},
        {"time", "time", "birthtime", "btime", "tob", "timeofbirth"},
        {"place", "place", "birthplace", "location", "city", "town", "bplace", "placeofbirth"},
        {"rodden", "rodden", "rr", "roddenrating", "rating", "accuracy"},
        {"notes", "notes", "note", "comment", "comments", "memo"},
        {"tags", "tags", "tag", "category", "categories", "keywords"},
    };

    /** Reads a file. Never throws: every problem is a line in the result. */
    static Result read(java.io.File file) {
        Result r = new Result();
        List<String> lines;
        try {
            lines = java.nio.file.Files.readAllLines(file.toPath(),
                java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception cannotRead) {
            r.problems.add("The file could not be read: " + cannotRead);
            return r;
        }
        if (lines.isEmpty()) {
            r.problems.add("The file is empty.");
            return r;
        }
        char sep = separatorOf(lines.get(0));
        List<String> header = split(lines.get(0), sep);
        Map<String, Integer> where = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = normalise(header.get(i));
            for (String[] column : COLUMNS) {
                for (int a = 1; a < column.length; a++) {
                    if (column[a].equals(key) && !where.containsKey(column[0])) {
                        where.put(column[0], i);
                    }
                }
            }
        }
        if (!where.containsKey("name") || !where.containsKey("date")) {
            r.problems.add("The first line must name the columns, and must include at least a "
                + "name and a date. Found: " + header);
            return r;
        }

        // <b>Two passes, because the second needs what the first learned.</b> Which way round a
        // file writes its slash dates is a property of the FILE, and the only evidence for it is
        // the rows whose day is above twelve. So the unambiguous rows are read first and the rest
        // are read in the order they establish.
        List<String> raw = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            raw.add(lines.get(i));
        }
        int order = dateOrder(raw, where.get("date"), sep);
        r.dateOrder = order == DAY_FIRST ? "day first" : order == MONTH_FIRST ? "month first"
            : "none (only unambiguous dates were taken)";

        for (int i = 0; i < raw.size(); i++) {
            String line = raw.get(i);
            int number = i + 2;
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> cells = split(line, sep);
            Candidate c = new Candidate();
            c.line = number;
            c.name = cell(cells, where.get("name"));
            if (c.name.isEmpty()) {
                r.problems.add("Line " + number + ": no name.");
                continue;
            }
            String date = normaliseDate(cell(cells, where.get("date")), order);
            if (date == null) {
                r.problems.add("Line " + number + ": \"" + cell(cells, where.get("date"))
                    + "\" could be two different dates and the file does not say which. "
                    + "Left out rather than guessed.");
                continue;
            }
            c.date = date;
            c.time = normaliseTime(cell(cells, where.get("time")));
            c.place = cell(cells, where.get("place"));
            c.rodden = Rodden.of(cell(cells, where.get("rodden")));
            c.notes = cell(cells, where.get("notes"));
            c.tags = cell(cells, where.get("tags"));
            if (c.place.isEmpty()) {
                r.problems.add("Line " + number + ": " + c.name + " has no birth place. "
                    + "Imported, but the chart cannot be cast until one is entered.");
            }
            r.charts.add(c);
        }
        return r;
    }

    /**
     * Adds what was read to the chart book, and answers how many arrived.
     *
     * <b>Never overwrites.</b> A practitioner importing a file that overlaps their book would
     * otherwise lose whichever version they had corrected by hand, and would not be told. A name
     * already in use gets a number.
     */
    static int add(Result r, List<String> renamed) {
        int added = 0;
        for (Candidate c : r.charts) {
            String name = c.name;
            for (int n = 2; SavedCharts.get(name) != null && n < 1000; n++) {
                name = c.name + " (" + n + ")";
            }
            if (!name.equals(c.name) && renamed != null) {
                renamed.add(c.name + " → " + name);
            }
            if (SavedCharts.put(name, c.date, c.time, c.place, c.rodden, c.notes, c.tags)) {
                added++;
            }
        }
        return added;
    }

    // ------------------------------------------------------------------ dates

    private static final int UNKNOWN = 0;
    private static final int DAY_FIRST = 1;
    private static final int MONTH_FIRST = 2;

    /**
     * Which way round this file writes a slash date, from the rows that can only be read one way.
     *
     * <b>The file decides, not the machine's locale.</b> A book exported in Britain and opened in
     * America must not change everybody's birthday, which is what taking the system's date format
     * would do.
     */
    private static int dateOrder(List<String> rows, Integer column, char sep) {
        if (column == null) {
            return UNKNOWN;
        }
        boolean day = false;
        boolean month = false;
        for (String row : rows) {
            String[] parts = cell(split(row, sep), column).split("[/.-]");
            if (parts.length != 3) {
                continue;
            }
            try {
                int a = Integer.parseInt(parts[0].trim());
                int b = Integer.parseInt(parts[1].trim());
                if (parts[0].trim().length() == 4) {
                    continue;                       // already ISO, says nothing about order
                }
                if (a > 12 && b <= 12) {
                    day = true;
                } else if (b > 12 && a <= 12) {
                    month = true;
                }
            } catch (NumberFormatException notANumber) {
                continue;
            }
        }
        // Both, which means the file is inconsistent and neither reading is safe.
        if (day && month) {
            return UNKNOWN;
        }
        return day ? DAY_FIRST : month ? MONTH_FIRST : UNKNOWN;
    }

    /** yyyy-MM-dd, or null when the text could mean two dates. */
    static String normaliseDate(String text, int order) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            String[] p = s.split("-");
            return iso(p[0], p[1], p[2]);
        }
        String[] p = s.split("[/.-]");
        if (p.length != 3) {
            return null;
        }
        try {
            int a = Integer.parseInt(p[0].trim());
            int b = Integer.parseInt(p[1].trim());
            String year = p[2].trim();
            if (year.length() == 2) {
                // A two-digit year is its own guess, and one nobody can make safely for birth
                // data that spans a century.
                return null;
            }
            if (a > 12 && b <= 12) {
                return iso(year, String.valueOf(b), String.valueOf(a));
            }
            if (b > 12 && a <= 12) {
                return iso(year, String.valueOf(a), String.valueOf(b));
            }
            if (order == DAY_FIRST) {
                return iso(year, String.valueOf(b), String.valueOf(a));
            }
            if (order == MONTH_FIRST) {
                return iso(year, String.valueOf(a), String.valueOf(b));
            }
            return null;
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private static String iso(String y, String m, String d) {
        try {
            int year = Integer.parseInt(y.trim());
            int month = Integer.parseInt(m.trim());
            int day = Integer.parseInt(d.trim());
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }
            return String.format("%04d-%02d-%02d", year, month, day);
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /** HH:mm, with an empty or unreadable time becoming noon rather than refusing the chart. */
    static String normaliseTime(String text) {
        String s = text == null ? "" : text.trim().toLowerCase();
        if (s.isEmpty()) {
            return "12:00";
        }
        boolean pm = s.contains("pm");
        boolean am = s.contains("am");
        s = s.replace("am", "").replace("pm", "").trim();
        String[] p = s.split("[:.]");
        try {
            int h = Integer.parseInt(p[0].trim());
            int m = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
            if (pm && h < 12) {
                h += 12;
            }
            if (am && h == 12) {
                h = 0;
            }
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                return "12:00";
            }
            return String.format("%02d:%02d", h, m);
        } catch (Exception notATime) {
            return "12:00";
        }
    }

    // ------------------------------------------------------------------ csv

    /** Comma, semicolon or tab - whichever the header line has most of. */
    static char separatorOf(String header) {
        char best = ',';
        int most = 0;
        for (char c : new char[] {',', ';', '\t'}) {
            int n = 0;
            for (int i = 0; i < header.length(); i++) {
                if (header.charAt(i) == c) {
                    n++;
                }
            }
            if (n > most) {
                most = n;
                best = c;
            }
        }
        return best;
    }

    /**
     * One line into cells, honouring quotes.
     *
     * <b>Quotes matter here more than in most CSV.</b> A birth place is "Springfield, Illinois",
     * with the comma inside it, so a split on the separator alone moves every column after the
     * place one to the left - and the chart still imports, with the rating in the notes.
     */
    static List<String> split(String line, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == sep && !quoted) {
                out.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        out.add(cell.toString().trim());
        return out;
    }

    private static String cell(List<String> cells, Integer at) {
        return at == null || at < 0 || at >= cells.size() ? "" : cells.get(at).trim();
    }

    /**
     * A column name reduced to what identifies it: letters and digits, lowercased.
     *
     * <b>This is also what handles a byte-order mark, which is why there is no code for one.</b>
     * Exporters emit them and the first version stripped one explicitly at the top of the file;
     * removing that changed nothing any assertion could see, because a BOM is not a letter or a
     * digit and never survives this line. A guard no failure can reach is not a guard, and
     * {@code ChartImportCheck} Part A keeps a BOM fixture so the behaviour stays held here.
     */
    private static String normalise(String header) {
        return header == null ? ""
            : header.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
