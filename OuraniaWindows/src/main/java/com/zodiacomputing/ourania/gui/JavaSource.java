package com.zodiacomputing.ourania.gui;

/**
 * Java source, read two ways: the code without its literals, and the literals without the code.
 *
 * <p><b>One scanner, because there are two sweeps now.</b> {@code NavigationCheck} has asked since
 * 25 Sep whether the interface names each engine, and it has to ignore string literals - a word
 * inside a sentence is not a door, and {@code Electional} passed that sweep for a whole day on a
 * prose blurb about al-Biruni. {@code ErrorLogCheck} asks the opposite question, whether any string
 * literal in the tree is an absolute path, and it has to ignore the code.
 *
 * <p>Those are the same scan with opposite outputs, so they are the same method. Writing the second
 * one separately is how this project acquired six copies of {@code ringWord} and four lists of house
 * systems, and the copies always drift in the direction that makes a check weaker.
 *
 * <p><b>Deliberately a small scanner rather than a regex.</b> A regex over Java literals has to get
 * escapes right or it runs off the end of one and eats the code after it - and it would fail
 * silently, in the same direction as the defect each sweep exists to catch.
 */
final class JavaSource {

    private JavaSource() { }

    /** Every {@code .java} file in the gui package. */
    static java.io.File[] guiSources() {
        return new java.io.File("src/main/java/com/zodiacomputing/ourania/gui")
            .listFiles((d, n) -> n.endsWith(".java"));
    }

    /** Every {@code .java} file under {@code src/main/java/com/zodiacomputing/ourania}. */
    static java.util.List<java.io.File> allSources() {
        java.util.List<java.io.File> out = new java.util.ArrayList<>();
        collect(new java.io.File("src/main/java/com/zodiacomputing/ourania"), out);
        out.sort(java.util.Comparator.comparing(java.io.File::getPath));
        return out;
    }

    private static void collect(java.io.File dir, java.util.List<java.io.File> out) {
        java.io.File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (java.io.File f : kids) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (f.getName().endsWith(".java")) {
                out.add(f);
            }
        }
    }

    /** A source file with its string literals, character literals and comments removed. */
    static String codeOnly(String src) {
        return scan(src, false);
    }

    /**
     * The contents of every string literal in a source file, one per entry.
     *
     * <p>Character literals are left out: a single character cannot be a path, and including them
     * would mean deciding what an escape sequence stands for. Comments are left out too, which
     * matters for the absolute-path sweep - {@code Ephemeris}' own comment quotes the bad path it
     * is explaining, and a sweep that flagged the explanation would be teaching people to delete
     * documentation.
     */
    static java.util.List<String> literals(String src) {
        java.util.List<String> out = new java.util.ArrayList<>();
        scan(src, true, out);
        return out;
    }

    private static String scan(String src, boolean wantLiterals) {
        return scan(src, wantLiterals, new java.util.ArrayList<>());
    }

    /**
     * The one pass. {@code wantLiterals} chooses which half is collected; the walk is identical,
     * which is the whole point of having one method.
     */
    private static String scan(String src, boolean wantLiterals, java.util.List<String> literals) {
        StringBuilder out = new StringBuilder(src.length());
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                while (i < n && src.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
            } else if (c == '"' || c == '\'') {
                char quote = c;
                boolean isString = c == '"';
                i++;
                StringBuilder body = new StringBuilder();
                while (i < n && src.charAt(i) != quote) {
                    // An escaped quote does not end the literal; skipping the pair is what keeps
                    // this from running off the end of one and eating the code after it.
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        body.append(src.charAt(i)).append(src.charAt(i + 1));
                        i += 2;
                    } else {
                        body.append(src.charAt(i));
                        i++;
                    }
                }
                i++;
                if (wantLiterals && isString) {
                    literals.add(body.toString());
                }
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
