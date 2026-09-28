package com.zodiacomputing.ourania.gui;

/**
 * A JSON reader that reads JSON (J12).
 *
 * <p><b>What it replaces read one entry per line.</b> {@code loadExtraFile} took the text between
 * the first quote after {@code ":} and the <i>last</i> quote on the same line, which works
 * perfectly for a file a generator wrote one field to a line and fails in ways nobody sees for
 * anything else. A value carrying a newline loses the entry <i>and</i> whatever shares its
 * continuation line; two entries on one line loses one of them; and an escaped quote inside a
 * value is stored with its backslash still in it, because the only escape that reader understood
 * was {@code \}{@code u}.
 *
 * <p><b>Written here rather than depended on.</b> This project ships one third-party jar and adding
 * another for 200 lines is a licence, a download and a supply chain for something the corpus does
 * not need; the whole of RFC 8259 is small.
 *
 * <p><b>It is strict on purpose.</b> A tolerant reader is what the corpus already had, and a
 * tolerant reader's defect is silence: {@code DataCheck} exists because entries were being dropped
 * without anything erroring. This one refuses a trailing comma, an unquoted key, a control
 * character in a string and a number JSON does not define, and says where. A corpus file that
 * stops parsing is a file somebody broke, and that is worth knowing on the spot.
 *
 * <p>Values come back as {@code Map<String,Object>} (insertion-ordered - the corpus depends on
 * order, because the first file to claim a key wins), {@code List<Object>}, {@code String},
 * {@code Double}, {@code Boolean} or null.
 */
final class Json {

    private Json() { }

    /** What a file that is not JSON gets you: where it stopped, and what was expected. */
    static final class Malformed extends RuntimeException {
        final int at;

        Malformed(String message, int at) {
            super(message + " at character " + at);
            this.at = at;
        }
    }

    /** Parses a whole document. Anything after the top-level value is an error. */
    static Object parse(String text) {
        Reader r = new Reader(text);
        r.skipSpace();
        Object v = r.value();
        r.skipSpace();
        if (!r.done()) {
            throw new Malformed("trailing content after the document", r.i);
        }
        return v;
    }

    /** Parses a file as UTF-8, with the byte-order mark tolerated. */
    static Object parse(java.io.File file) throws java.io.IOException {
        String text = new String(java.nio.file.Files.readAllBytes(file.toPath()),
            java.nio.charset.StandardCharsets.UTF_8);
        // A BOM is not whitespace and is not a value; several of this corpus's files were
        // written by tools that emit one, and the line reader never noticed because it only
        // ever looked at lines beginning with a quote.
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            text = text.substring(1);
        }
        return parse(text);
    }

    /** The document as an object, or an empty map when it is not one. */
    @SuppressWarnings("unchecked")
    static java.util.Map<String, Object> object(Object parsed) {
        return parsed instanceof java.util.Map
            ? (java.util.Map<String, Object>) parsed : new java.util.LinkedHashMap<>();
    }

    // ------------------------------------------------------------------ the walk

    private static final class Reader {
        private final String s;
        private int i;

        Reader(String s) {
            this.s = s;
        }

        boolean done() {
            return i >= s.length();
        }

        void skipSpace() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    i++;
                } else {
                    return;
                }
            }
        }

        Object value() {
            if (done()) {
                throw new Malformed("a value was expected", i);
            }
            char c = s.charAt(i);
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': return literal("true", Boolean.TRUE);
                case 'f': return literal("false", Boolean.FALSE);
                case 'n': return literal("null", null);
                default: return number();
            }
        }

        private Object literal(String word, Object as) {
            if (!s.startsWith(word, i)) {
                throw new Malformed("expected " + word, i);
            }
            i += word.length();
            return as;
        }

        private java.util.Map<String, Object> object() {
            // Insertion-ordered: the corpus loads under putIfAbsent, so which entry is seen
            // first decides which wins, and a HashMap here would make that arbitrary.
            java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
            i++;
            skipSpace();
            if (!done() && s.charAt(i) == '}') {
                i++;
                return out;
            }
            while (true) {
                skipSpace();
                if (done() || s.charAt(i) != '"') {
                    throw new Malformed("a quoted key was expected", i);
                }
                String key = string();
                skipSpace();
                if (done() || s.charAt(i) != ':') {
                    throw new Malformed("a colon was expected after the key", i);
                }
                i++;
                skipSpace();
                out.put(key, value());
                skipSpace();
                if (done()) {
                    throw new Malformed("the object was never closed", i);
                }
                char c = s.charAt(i++);
                if (c == '}') {
                    return out;
                }
                if (c != ',') {
                    throw new Malformed("a comma or a closing brace was expected", i - 1);
                }
                // Deliberately not tolerated: a trailing comma is the commonest way a
                // hand-edited corpus file goes wrong, and accepting it means the next tool to
                // read the file disagrees with this one.
            }
        }

        private java.util.List<Object> array() {
            java.util.List<Object> out = new java.util.ArrayList<>();
            i++;
            skipSpace();
            if (!done() && s.charAt(i) == ']') {
                i++;
                return out;
            }
            while (true) {
                skipSpace();
                out.add(value());
                skipSpace();
                if (done()) {
                    throw new Malformed("the array was never closed", i);
                }
                char c = s.charAt(i++);
                if (c == ']') {
                    return out;
                }
                if (c != ',') {
                    throw new Malformed("a comma or a closing bracket was expected", i - 1);
                }
            }
        }

        private String string() {
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (done()) {
                    throw new Malformed("the string was never closed", i);
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    // A raw newline or tab inside a string is not JSON, and it is exactly the
                    // shape that cost the line reader entries - so it is named rather than
                    // quietly accepted.
                    if (c < 0x20) {
                        throw new Malformed("an unescaped control character in a string", i - 1);
                    }
                    sb.append(c);
                    continue;
                }
                if (done()) {
                    throw new Malformed("the escape was never completed", i);
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) {
                            throw new Malformed("a short unicode escape", i);
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        } catch (NumberFormatException bad) {
                            throw new Malformed("a unicode escape that is not hexadecimal", i);
                        }
                        i += 4;
                        break;
                    default:
                        throw new Malformed("an escape JSON does not define: \\" + e, i - 1);
                }
            }
        }

        private Double number() {
            int from = i;
            if (!done() && s.charAt(i) == '-') {
                i++;
            }
            while (!done() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.'
                    || s.charAt(i) == 'e' || s.charAt(i) == 'E'
                    || ((s.charAt(i) == '+' || s.charAt(i) == '-')
                        && (s.charAt(i - 1) == 'e' || s.charAt(i - 1) == 'E')))) {
                i++;
            }
            if (i == from) {
                throw new Malformed("a value was expected", from);
            }
            try {
                return Double.valueOf(s.substring(from, i));
            } catch (NumberFormatException bad) {
                throw new Malformed("a number JSON does not define: " + s.substring(from, i),
                    from);
            }
        }
    }
}
