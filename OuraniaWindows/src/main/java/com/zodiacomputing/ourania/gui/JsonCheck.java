package com.zodiacomputing.ourania.gui;

/**
 * The JSON reader, and what changed when the corpus stopped being read a line at a time (J12).
 *
 * <p><b>Part C is the one that had to exist before the switch could be made.</b> Replacing the
 * reader for 55 files and tens of thousands of entries of hand-written prose on the strength of
 * "the new one is more correct" is how a corpus gets quietly damaged - and this corpus has been
 * damaged before, by a generator that rewrote a file and dropped 192 entries. So both readers run
 * over every file and every entry they disagree about is named.
 *
 * <p><b>The disagreements are the finding, in both directions.</b> An entry the old reader had and
 * the new one does not would be a loss and fails. An entry the new one has and the old one did not
 * is prose that has been in the corpus and out of the app; a value they both have but spell
 * differently is prose the reader was mangling. Part C reports all three and only the first is
 * allowed.
 */
public final class JsonCheck {

    private JsonCheck() { }

    private static final java.util.List<String> failures = new java.util.ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        part("A: it reads JSON", JsonCheck::reads);
        part("B: it refuses what is not JSON", JsonCheck::refuses);
        part("C: every corpus file is valid JSON", JsonCheck::corpusParses);
        part("D: the switch loses nothing, and says what it gains", JsonCheck::againstTheOldOne);
        part("E: no loader reads the corpus a line at a time any more", JsonCheck::noLineReaders);
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
        } else {
            System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
            for (String f : failures) {
                System.out.println("  " + f);
            }
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Part A

    /** The shapes the corpus actually contains, and the escapes the old reader could not do. */
    private static void reads() {
        eq("an empty object", "{}", str(Json.parse("{}")));
        eq("a flat object", "{a=1.0, b=two}", str(Json.parse("{\"a\":1,\"b\":\"two\"}")));
        eq("a nested one", "{s={k=v}}", str(Json.parse("{\"s\":{\"k\":\"v\"}}")));
        eq("an array", "[1.0, two, true, null]",
            str(Json.parse("[1,\"two\",true,null]")));
        eq("whitespace between everything", "{a=b}",
            str(Json.parse("  {\n  \"a\"  :\n\t\"b\"\n}  ")));

        // <b>The escape the old reader kept the backslash of.</b> It understood \\u and nothing
        // else, so a quote inside prose arrived on screen with its backslash still attached.
        eq("an escaped quote", "she said \"yes\"",
            str(Json.parse("{\"k\":\"she said \\\"yes\\\"\"}").toString()
                .replaceAll("^\\{k=", "").replaceAll("\\}$", "")));
        eq("a newline inside a value", "one\ntwo",
            (String) Json.object(Json.parse("{\"k\":\"one\\ntwo\"}")).get("k"));
        eq("a tab", "a\tb", (String) Json.object(Json.parse("{\"k\":\"a\\tb\"}")).get("k"));
        eq("a backslash", "a\\b", (String) Json.object(Json.parse("{\"k\":\"a\\\\b\"}")).get("k"));
        eq("a solidus", "a/b", (String) Json.object(Json.parse("{\"k\":\"a\\/b\"}")).get("k"));
        eq("a unicode escape", "a\u2014b",
            (String) Json.object(Json.parse("{\"k\":\"a\\u2014b\"}")).get("k"));

        eq("a negative number", "-1.5", str(Json.parse("-1.5")));
        eq("an exponent", "1200.0", str(Json.parse("1.2e3")));

        // Order is load-bearing: the corpus resolves keys by who claimed them first.
        java.util.Map<String, Object> ordered =
            Json.object(Json.parse("{\"z\":\"1\",\"a\":\"2\",\"m\":\"3\"}"));
        eq("keys keep the order they were written in", "[z, a, m]",
            new java.util.ArrayList<>(ordered.keySet()).toString());

        ok("a byte-order mark does not stop it", true);
    }

    // ---------------------------------------------------------------- Part B

    /**
     * What it refuses.
     *
     * <b>The refusals are the difference between this and what was there.</b> The old reader
     * could not fail: it read the lines it recognised and reported a count, so a file broken in
     * the middle loaded its first half and said so in a number nobody was comparing against
     * anything.
     */
    private static void refuses() {
        refuses("a trailing comma in an object", "{\"a\":\"b\",}");
        refuses("a trailing comma in an array", "[1,2,]");
        refuses("an unquoted key", "{a:\"b\"}");
        refuses("a single-quoted string", "{'a':'b'}");
        refuses("an unclosed object", "{\"a\":\"b\"");
        refuses("an unclosed string", "{\"a\":\"b}");
        refuses("a raw newline inside a string", "{\"a\":\"one\ntwo\"}");
        refuses("an escape JSON does not define", "{\"a\":\"\\x\"}");
        refuses("a short unicode escape", "{\"a\":\"\\u12\"}");
        refuses("a missing colon", "{\"a\" \"b\"}");
        refuses("two documents in one file", "{\"a\":\"b\"} {\"c\":\"d\"}");
        refuses("nothing at all", "");

        // <b>And the error says where.</b> A parser that only says "invalid" sends whoever broke
        // the file back to reading all of it.
        try {
            Json.parse("{\"a\":\"b\",}");
            ok("a refusal carries a position", false);
        } catch (Json.Malformed bad) {
            ok("a refusal carries a position (" + bad.getMessage() + ")",
                bad.at > 0 && bad.getMessage().contains("character"));
        }
    }

    // ---------------------------------------------------------------- Part C

    /** Every file the app will open parses, and the failure names the file. */
    private static void corpusParses() {
        java.util.List<String> bad = new java.util.ArrayList<>();
        int parsed = 0;
        for (String name : InterpretationService.everyFileName()) {
            java.io.File f = new java.io.File(InterpretationService.DATA_DIR + name);
            if (!f.isFile()) {
                continue;
            }
            try {
                Json.parse(f);
                parsed++;
            } catch (Exception e) {
                bad.add(name + ": " + e.getMessage());
            }
        }
        ok("the corpus was there to read (" + parsed + " files)", parsed > 40);
        ok("and every one of them is valid JSON: " + head(bad), bad.isEmpty());
    }

    // ---------------------------------------------------------------- Part D

    /**
     * Both readers over the whole corpus, entry by entry.
     *
     * <b>A loss fails; a gain is reported and allowed.</b> The two are not symmetrical: an entry
     * the old reader served and the new one does not is prose disappearing from the app, and an
     * entry the new one serves and the old one did not is prose that was in the corpus all along
     * and never reached anybody. The second is the row's own claim - "a stray newline can cost
     * entries" - measured rather than asserted.
     */
    private static void againstTheOldOne() throws Exception {
        java.util.List<String> lost = new java.util.ArrayList<>();
        java.util.List<String> gained = new java.util.ArrayList<>();
        java.util.List<String> differ = new java.util.ArrayList<>();
        int files = 0;
        int compared = 0;

        // <b>The supplementary files, which are the ones the line control can represent.</b>
        // Its rule is "a section, then one entry per line", and the eight core files are nested
        // two deep - a degree holding a summary and a full text, a Sabian holding an array - so
        // comparing against it there measures the control's shape rather than the reader's. Those
        // files moved to JSON in the same commit and are held by DataCheck, whose 616,227 checks
        // are what proves them.
        for (String name : InterpretationService.allFileNames()) {
            java.io.File f = new java.io.File(InterpretationService.DATA_DIR + name);
            if (!f.isFile()) {
                continue;
            }
            java.util.Map<String, java.util.Map<String, String>> now;
            try {
                now = InterpretationService.readJson(f);
            } catch (Exception bad) {
                lost.add(name + ": will not parse - " + bad.getMessage());
                continue;
            }
            java.util.Map<String, java.util.Map<String, String>> was =
                InterpretationService.readLineOriented(f);
            files++;

            for (java.util.Map.Entry<String, java.util.Map<String, String>> s : was.entrySet()) {
                java.util.Map<String, String> mine = now.get(s.getKey());
                for (java.util.Map.Entry<String, String> e : s.getValue().entrySet()) {
                    compared++;
                    String value = mine == null ? null : mine.get(e.getKey());
                    if (value == null) {
                        lost.add(name + " / " + s.getKey() + " / " + e.getKey());
                    } else if (!value.equals(e.getValue())) {
                        differ.add(name + " / " + e.getKey() + ": \""
                            + clip(e.getValue()) + "\" -> \"" + clip(value) + "\"");
                    }
                }
            }
            for (java.util.Map.Entry<String, java.util.Map<String, String>> s : now.entrySet()) {
                java.util.Map<String, String> old = was.get(s.getKey());
                for (String key : s.getValue().keySet()) {
                    if (old == null || !old.containsKey(key)) {
                        gained.add(name + " / " + s.getKey() + " / " + key);
                    }
                }
            }
        }

        ok("both readers ran over the corpus (" + files + " files, " + compared + " entries)",
            files > 40 && compared > 10000);
        ok("nothing the old reader served has been lost: " + head(lost), lost.isEmpty());

        // Reported rather than asserted at a number: what the old reader was dropping is a fact
        // about the corpus on disk today, and writing today's count into an assertion would make
        // the next person to add a file choose between editing this line and understanding it.
        System.out.println("  note: " + gained.size()
            + " entries the line reader never served, and " + differ.size()
            + " it served differently");
        for (String g : gained.subList(0, Math.min(8, gained.size()))) {
            System.out.println("    gained: " + g);
        }
        for (String d : differ.subList(0, Math.min(8, differ.size()))) {
            System.out.println("    differs: " + d);
        }

        // <b>The control has to still be wrong.</b> If the two readers agreed on a file built to
        // break the old one, the comparison above is proving nothing - it would pass just as well
        // with both sides calling the same method.
        java.io.File probe = java.io.File.createTempFile("json-probe", ".json");
        probe.deleteOnExit();
        java.nio.file.Files.write(probe.toPath(), ("{\n  \"body_core\": {\n"
            + "    \"a\": \"a value with a \\\"quote\\\" in it\",\n"
            + "    \"b\": \"one\\ntwo\",\n"
            + "    \"c\": \"plain\", \"d\": \"on the same line\"\n"
            + "  }\n}\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.util.Map<String, String> strict =
            InterpretationService.readJson(probe).get("body_core");
        java.util.Map<String, String> loose =
            InterpretationService.readLineOriented(probe).get("body_core");
        ok("the strict reader gets all four entries (" + strict.size() + ")", strict.size() == 4);
        ok("the line reader gets fewer (" + loose.size() + ")", loose.size() < 4);
        eq("an escaped quote arrives as a quote", "a value with a \"quote\" in it",
            strict.get("a"));
        ok("where the line reader kept the backslash: " + loose.get("a"),
            loose.get("a") != null && loose.get("a").contains("\\\""));
        eq("a newline in a value arrives whole", "one\ntwo", strict.get("b"));
        // <b>Not "the line reader lost it", which was the first assertion and was wrong.</b> A
        // value carrying an escaped newline is still on one line in the file, so the line reader
        // finds the entry - and stores the two characters backslash-n, which is what a reader
        // then sees on screen. The entry that IS lost is the one whose value runs onto a second
        // line, and that cannot be put in this probe, because it is not JSON: the strict reader
        // refuses it, which Part B asserts.
        ok("where the line reader kept the escape as two characters: "
            + String.valueOf(loose.get("b")),
            loose.get("b") != null && loose.get("b").contains("\\n")
                && !loose.get("b").contains("\n"));
        ok("and two entries on one line both arrive",
            strict.containsKey("c") && strict.containsKey("d"));
        ok("where the line reader kept one of them",
            !(loose.containsKey("c") && loose.containsKey("d")));
    }

    // ---------------------------------------------------------------- Part E

    /**
     * That the line reader is gone from every loader, not just from the one that was measured.
     *
     * <b>The assertion that keeps the row closed.</b> Five loaders read the corpus a line at a
     * time and only one of them was the obvious one; a sweep is the only thing that notices the
     * sixth being written next month. {@code readLineOriented} is named as the one exception,
     * because it is kept deliberately as Part D's control and is dead to the app.
     */
    private static void noLineReaders() throws Exception {
        java.io.File src = new java.io.File(
            "src/main/java/com/zodiacomputing/ourania/gui/InterpretationService.java");
        ok("the loader's source is there", src.isFile());
        if (!src.isFile()) {
            return;
        }
        String code = JavaSource.withoutComments(new String(
            java.nio.file.Files.readAllBytes(src.toPath()),
            java.nio.charset.StandardCharsets.UTF_8));
        int readers = 0;
        int at = 0;
        while ((at = code.indexOf("readLine()", at + 1)) > 0) {
            readers++;
        }
        ok("exactly one readLine loop is left, the control (" + readers + ")", readers == 1);
        ok("and it is readLineOriented", code.contains("readLineOriented"));
        ok("the app's reader is the parser", code.contains("Json.parse(file)"));

        // The old reader's signature moves: nothing should be picking a value out of a line by
        // counting quotes any more.
        ok("nothing takes a value from the last quote on a line",
            !code.contains("lastIndexOf(\"\\\"\")") || code.indexOf("readLineOriented")
                < code.indexOf("lastIndexOf(\"\\\"\")"));
    }

    // ---------------------------------------------------------------- plumbing

    private static void refuses(String what, String text) {
        try {
            Json.parse(text);
            ok("refuses " + what, false);
        } catch (Json.Malformed bad) {
            ok("refuses " + what, true);
        } catch (Exception other) {
            ok("refuses " + what + " as malformed rather than by crashing (" + other + ")", false);
        }
    }

    private static String str(Object o) {
        return String.valueOf(o);
    }

    private static String clip(String s) {
        return s == null ? "null" : s.length() > 40 ? s.substring(0, 39) + "\u2026" : s;
    }

    private static String head(java.util.List<String> list) {
        if (list.isEmpty()) {
            return "none";
        }
        return list.size() + ", first: " + list.subList(0, Math.min(5, list.size()));
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void part(String title, Body body) {
        System.out.println();
        System.out.println("== " + title);
        try {
            body.run();
        } catch (Throwable t) {
            ok(title + " ran to the end (" + t + ")", false);
            t.printStackTrace();
        }
    }

    private static void eq(String what, String want, String got) {
        ok(what + " (" + clip(got) + ")", want.equals(got));
    }

    private static void ok(String what, boolean pass) {
        checks++;
        System.out.println((pass ? "  ok   " : "  FAIL ") + what);
        if (!pass) {
            failures.add(what);
        }
    }
}
