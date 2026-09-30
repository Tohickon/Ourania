package com.zodiacomputing.ourania.android;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import com.zodiacomputing.ourania.astro.AppPaths;
import com.zodiacomputing.ourania.astro.Bodies;
import com.zodiacomputing.ourania.astro.ChartFrame;
import com.zodiacomputing.ourania.astro.DataFiles;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.PlainText;
import com.zodiacomputing.ourania.astro.Zodiac;
import com.zodiacomputing.ourania.gui.InterpretationService;

import de.thmac.swisseph.SweDate;
import de.thmac.swisseph.SwissEph;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * The phone's first screen: the sky now, cast by the desktop's own engine (M2).
 *
 * <p>Not the app yet - that is M3 onward. This proves the thing M2 exists to prove: the engine,
 * the readings and their data run on the device, built from the same source as the desktop.
 * Where the engine would look for a folder, it is told where the phone keeps things:
 * {@link DataFiles} opens the corpus from the app's assets, and the reader's own files live in
 * the app's private storage.
 */
public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        // Before anything in the engine is touched: AppPaths and Ephemeris read these once.
        String home = getFilesDir().getPath();
        System.setProperty(AppPaths.HOME_PROPERTY, home);
        System.setProperty(AppPaths.DATA_PROPERTY, home);
        System.setProperty(Ephemeris.PROPERTY, new java.io.File(getFilesDir(), "ephe").getPath());
        DataFiles.use(new Assets(getAssets()));

        TextView text = new TextView(this);
        text.setPadding(40, 60, 40, 60);
        text.setTextSize(15f);
        text.setText("Casting the sky...");
        ScrollView scroll = new ScrollView(this);
        scroll.addView(text);
        setContentView(scroll);

        // The corpus is a few seconds of reading; off the main thread so the screen is not frozen.
        new Thread(() -> {
            String out;
            try {
                out = skyNow();
            } catch (Throwable t) {
                out = "The engine did not run: " + t;
            }
            final String shown = out;
            runOnUiThread(() -> text.setText(shown));
        }).start();
    }

    /** The planets now, from Greenwich, each with the first line of its reading. */
    private static String skyNow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        double hour = now.getHour() + now.getMinute() / 60.0;
        double jd = new SweDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), hour)
            .getJulDay();
        ChartFrame f = ChartFrame.compute(new SwissEph(Ephemeris.PATH), jd, 51.4779, 0.0,
            'P', false, 0.0);
        InterpretationService svc = InterpretationService.getInstance();

        StringBuilder sb = new StringBuilder();
        sb.append("Ourania - the sky now\n")
          .append(now.toLocalDate()).append(' ')
          .append(String.format("%02d:%02d", now.getHour(), now.getMinute()))
          .append(" UT, Greenwich\n\n");
        for (int i = 0; i < Bodies.count() && i < f.bodies.length; i++) {
            ChartFrame.Body b = f.bodies[i];
            if (b == null || !b.ok || i > 9) {
                continue;                       // the ten planets, for a first screen
            }
            sb.append(b.name).append("  ").append(Zodiac.format(b.lon))
              .append(b.retrograde ? "  R" : "").append('\n');
            String reading = PlainText.summary(svc.getPlanetInSign(b.name, Zodiac.signName(b.lon)));
            if (!reading.isEmpty()) {
                sb.append("   ").append(reading).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Ascendant  ").append(Zodiac.format(f.asc)).append('\n');
        boolean files = new java.io.File(Ephemeris.PATH).isDirectory();
        sb.append("\nEphemeris: ").append(files ? "Swiss Ephemeris files"
            : "built-in (Moshier) - the Swiss Ephemeris files are not bundled yet");
        return sb.toString();
    }

    /** The app's packed assets, as the engine's data files (M1). */
    private static final class Assets implements DataFiles.Source {
        private final AssetManager assets;

        Assets(AssetManager assets) {
            this.assets = assets;
        }

        @Override
        public boolean exists(String name) {
            try (InputStream in = this.open(name)) {
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * <b>A .gz file is packed decompressed and renamed.</b> The APK packager stores
         * {@code atlas.tsv.gz} as {@code atlas.tsv} - found in the first APK, 30 Sep - so a name
         * that is not there is tried without the suffix. Atlas reads either form.
         */
        @Override
        public InputStream open(String name) throws IOException {
            try {
                return this.assets.open(name);
            } catch (IOException missing) {
                if (!name.endsWith(".gz")) {
                    throw missing;
                }
                return this.assets.open(name.substring(0, name.length() - 3));
            }
        }

        @Override
        public String where() {
            return "assets:";
        }
    }
}
