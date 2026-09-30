package com.zodiacomputing.ourania.gui;

import com.zodiacomputing.ourania.astro.AppPaths;
import com.zodiacomputing.ourania.astro.DataFiles;
import com.zodiacomputing.ourania.astro.PlainText;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The interpretation corpus loads from wherever {@link DataFiles} says, and from nowhere else
 * (M1).
 *
 * <p><b>The phone has no data folder.</b> Its 83 MB of prose is packed inside the app and opened
 * by name, so every loader has to ask {@code DataFiles} rather than build a path under
 * {@code DATA_DIR}. A loader that still opened a path would work on every desktop and read
 * nothing on the phone - so this sets a trap: {@code DATA_DIR} is pointed at a folder that does
 * not exist, the real files are served only through an installed {@code Source}, and the
 * readings have to come back anyway. Every name the source is asked for is recorded, so a file
 * the loader stopped asking for shows up too.
 *
 * <p><b>And the other way round:</b> a second corpus built over a source that holds nothing reads
 * nothing, so the readings above cannot be coming from somewhere this part does not know about.
 */
public final class DataFilesCheck {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Settings.useScratchFile();
        // Where the files really are - asked before the trap is set, and served from here only
        // through the recording source.
        final String real = AppPaths.dataDir();
        String nowhere = new java.io.File(System.getProperty("java.io.tmpdir"),
            "ourania-no-data-" + System.nanoTime()).getPath();
        System.setProperty(AppPaths.DATA_PROPERTY, nowhere);

        final Set<String> asked = new TreeSet<>();
        final DataFiles.Folder folder = new DataFiles.Folder(real);
        DataFiles.use(new DataFiles.Source() {
            @Override
            public boolean exists(String name) {
                return folder.exists(name);
            }

            @Override
            public InputStream open(String name) throws IOException {
                synchronized (asked) {
                    asked.add(name);
                }
                return folder.open(name);
            }

            @Override
            public String where() {
                return "recording:" + folder.where();
            }
        });

        System.out.println("=== Part A: the trap is set ===");
        yes("DATA_DIR points at the folder that does not exist: " + InterpretationService.DATA_DIR,
            InterpretationService.DATA_DIR.startsWith(nowhere));
        yes("and it does not exist", !new java.io.File(InterpretationService.DATA_DIR).exists());
        yes("the real data is where it was: " + real, new java.io.File(real).isDirectory());

        System.out.println("=== Part B: every file is opened through the source ===");
        InterpretationService svc = InterpretationService.getInstance();
        svc.loadEveryLazySection();
        int present = 0;
        for (String name : InterpretationService.everyFileName()) {
            if (!folder.exists(name)) {
                continue;
            }
            present++;
            yes(name + " was opened through the source", asked.contains(name));
        }
        yes("the corpus names files to open (" + present + ")", present >= 20);

        System.out.println("=== Part C: and the readings come back ===");
        String[][] readings = {
            {"the first decan of Aries", svc.getDecan("Aries", 1)},
            {"Venus in Libra", svc.getPlanetInSign("Venus", "Libra")},
            {"Mars in the 10th", svc.getPlanetInHouse("Mars", 10)},
            {"Sun square Moon", svc.getAspect("Sun", "Moon", "Square")},
            {"the Sabian symbol for 1 Aries", svc.getSabianSymbol("Aries", 1)},
            {"the sign Scorpio", svc.getSign("Scorpio")},
            {"the 7th house", svc.getHouse(7)},
        };
        for (String[] r : readings) {
            yes(r[0] + " reads from the source: \"" + clip(r[1]) + "\"",
                !PlainText.plainText(r[1]).isEmpty());
        }

        System.out.println("=== Part D: a source with nothing gives nothing ===");
        DataFiles.use(new DataFiles.Source() {
            @Override
            public boolean exists(String name) {
                return false;
            }

            @Override
            public InputStream open(String name) throws IOException {
                throw new java.io.FileNotFoundException(name);
            }

            @Override
            public String where() {
                return "empty:";
            }
        });
        java.lang.reflect.Constructor<InterpretationService> make =
            InterpretationService.class.getDeclaredConstructor();
        make.setAccessible(true);
        InterpretationService empty = make.newInstance();
        yes("with nothing to open, the sign Scorpio is not there: \""
                + clip(empty.getSign("Scorpio")) + "\"",
            PlainText.plainText(empty.getSign("Scorpio")).isEmpty());
        yes("nor Venus in Libra", PlainText.plainText(empty.getPlanetInSign("Venus", "Libra")).isEmpty());

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("ALL CLEAR - " + checks + " checks, 0 failures.");
            System.exit(0);
        }
        System.out.println("FAILURES (" + failures.size() + " of " + checks + " checks):");
        for (String f : failures) {
            System.out.println("  " + f);
        }
        System.exit(1);
    }

    private static String clip(String s) {
        if (s == null) {
            return "null";
        }
        String t = s.replaceAll("\\s+", " ");
        return t.length() > 60 ? t.substring(0, 60) + "..." : t;
    }

    private static void yes(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures.add(label);
        }
    }
}
