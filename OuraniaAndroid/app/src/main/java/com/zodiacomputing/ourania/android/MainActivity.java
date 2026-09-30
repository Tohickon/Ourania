package com.zodiacomputing.ourania.android;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.zodiacomputing.ourania.astro.AppPaths;
import com.zodiacomputing.ourania.astro.DataFiles;
import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.InterpretationService;

import de.thmac.swisseph.SwissEph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Casting a chart on the phone: a birth date, a time (or "unknown"), and a place found in the
 * offline atlas as it is typed; then the wheel (M4) and the placements (M3).
 *
 * <p>Everything this screen does with the engine goes through {@link PhoneChart}, which has no
 * Android in it and is tested on the JVM. This class gathers the three answers and shows text.
 *
 * <p>Where the engine would look for a folder it is told where the phone keeps things, once, at
 * the top of {@link #onCreate}: the corpus and the atlas are opened from the app's assets
 * through {@link DataFiles}, the reader's own files live in the app's private storage, and the
 * Swiss Ephemeris files - when the build packed them - are copied out of the assets once,
 * because the ephemeris seeks inside its files and a packed asset cannot be sought.
 */
public final class MainActivity extends Activity {

    private LocalDate date;
    private LocalTime time = LocalTime.of(12, 0);
    private Atlas.Place place;

    private TextView dateField;
    private TextView timeField;
    private CheckBox timeUnknown;
    private AutoCompleteTextView placeField;
    private TextView result;
    private WheelView wheel;
    private TextView tapped;
    private Button cast;
    private PhoneChart.Cast shownCast;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String home = getFilesDir().getPath();
        File ephe = new File(getFilesDir(), "ephe");
        System.setProperty(AppPaths.HOME_PROPERTY, home);
        System.setProperty(AppPaths.DATA_PROPERTY, home);
        System.setProperty(Ephemeris.PROPERTY, ephe.getPath());
        DataFiles.use(new Assets(getAssets()));

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 64, 48, 64);

        TextView title = new TextView(this);
        title.setText("Ourania");
        title.setTextSize(26f);
        form.addView(title);

        this.dateField = field("Birth date - tap to choose");
        this.dateField.setOnClickListener(v -> this.chooseDate());
        form.addView(label("Date"));
        form.addView(this.dateField);

        this.timeField = field("12:00");
        this.timeField.setOnClickListener(v -> this.chooseTime());
        form.addView(label("Time, as on the birth certificate"));
        form.addView(this.timeField);
        this.timeUnknown = new CheckBox(this);
        this.timeUnknown.setText("Birth time unknown");
        this.timeUnknown.setOnCheckedChangeListener((b, on) -> this.timeField.setEnabled(!on));
        form.addView(this.timeUnknown);

        this.placeField = new AutoCompleteTextView(this);
        this.placeField.setHint("Start typing a town or city");
        this.placeField.setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        this.placeField.setThreshold(2);
        final Places places = new Places();
        this.placeField.setAdapter(places);
        this.placeField.setOnItemClickListener((parent, view, pos, id) -> {
            this.place = places.getItem(pos);
            this.placeField.setText(PhoneChart.label(this.place), false);
            this.placeField.dismissDropDown();
        });
        form.addView(label("Place of birth"));
        form.addView(this.placeField);

        this.cast = new Button(this);
        this.cast.setText("Cast the chart");
        this.cast.setOnClickListener(v -> this.castChart());
        form.addView(this.cast);

        // The wheel (M4), hidden until there is a chart; a tapped planet's lines under it.
        this.wheel = new WheelView(this);
        this.wheel.setVisibility(View.GONE);
        this.wheel.setOnBody(this::showBody);
        form.addView(this.wheel);
        this.tapped = new TextView(this);
        this.tapped.setTextSize(16f);
        this.tapped.setPadding(0, 16, 0, 0);
        this.tapped.setVisibility(View.GONE);
        form.addView(this.tapped);

        this.result = new TextView(this);
        this.result.setTextSize(15f);
        this.result.setPadding(0, 32, 0, 0);
        this.result.setTextIsSelectable(true);
        form.addView(this.result);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        setContentView(scroll);

        // The ephemeris copy and the corpus are seconds of work; neither belongs on the main
        // thread, and both are needed before the first chart, so they start now.
        new Thread(() -> {
            copyEphemeris(ephe);
            InterpretationService.getInstance();
        }).start();
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setPadding(0, 28, 0, 4);
        return t;
    }

    private TextView field(String hint) {
        TextView t = new TextView(this);
        t.setHint(hint);
        t.setTextSize(18f);
        t.setPadding(0, 12, 0, 12);
        return t;
    }

    private void chooseDate() {
        LocalDate start = this.date != null ? this.date : LocalDate.of(1990, 1, 1);
        new DatePickerDialog(this, (picker, y, m, d) -> {
            this.date = LocalDate.of(y, m + 1, d);
            this.dateField.setText(this.date.toString());
        }, start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth()).show();
    }

    private void chooseTime() {
        new TimePickerDialog(this, (picker, h, m) -> {
            this.time = LocalTime.of(h, m);
            this.timeField.setText(String.format("%02d:%02d", h, m));
        }, this.time.getHour(), this.time.getMinute(), true).show();
    }

    private void castChart() {
        if (this.date == null) {
            this.result.setText("Choose the birth date first.");
            return;
        }
        if (this.place == null) {
            this.result.setText("Choose the place from the list that appears as you type - "
                + "the chart needs its latitude, longitude and time zone.");
            return;
        }
        final LocalDate d = this.date;
        final LocalTime t = this.timeUnknown.isChecked() ? null : this.time;
        final Atlas.Place p = this.place;
        this.cast.setEnabled(false);
        this.result.setText("Casting...");
        new Thread(() -> {
            String out;
            PhoneChart.Cast c = null;
            try {
                c = PhoneChart.cast(new SwissEph(Ephemeris.PATH), d, t, p);
                out = PhoneChart.describe(c, InterpretationService.getInstance());
            } catch (Throwable e) {
                c = null;
                out = "The chart could not be cast: " + e;
            }
            final String shown = out;
            final PhoneChart.Cast cast = c;
            runOnUiThread(() -> {
                this.shownCast = cast;
                this.wheel.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                this.tapped.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                if (cast != null) {
                    this.wheel.show(cast.frame);
                    showBody(-1);
                }
                this.result.setText(shown);
                this.cast.setEnabled(true);
            });
        }).start();
    }

    /** A tapped planet's full reading under the wheel (M5); a hint when none is tapped. */
    private void showBody(int body) {
        String html = this.shownCast == null || body < 0 ? ""
            : PhoneReading.planet(this.shownCast, body, InterpretationService.getInstance());
        if (html.isEmpty()) {
            this.tapped.setText(
                "Tap a planet on the wheel to read it. Pinch to zoom; double-tap to reset.");
        } else {
            this.tapped.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        }
    }

    /**
     * The Swiss Ephemeris files out of the assets and into private storage, once.
     *
     * Skipped for a file already there at the same size, so a relaunch copies nothing. With none
     * packed the folder stays empty and the engine uses its built-in Moshier ephemeris.
     */
    private void copyEphemeris(File dir) {
        try {
            String[] names = getAssets().list("ephe");
            if (names == null || names.length == 0) {
                return;
            }
            dir.mkdirs();
            byte[] buf = new byte[1 << 16];
            for (String name : names) {
                File out = new File(dir, name);
                try (InputStream in = getAssets().open("ephe/" + name)) {
                    if (out.isFile() && out.length() == in.available()) {
                        continue;
                    }
                    try (OutputStream o = new FileOutputStream(out)) {
                        for (int n; (n = in.read(buf)) > 0; ) {
                            o.write(buf, 0, n);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Without the files the engine falls back to Moshier, which it says in the chart.
        }
    }

    /** The atlas as the place box's suggestions, searched off the main thread as it is typed. */
    private final class Places extends BaseAdapter implements Filterable {
        private List<Atlas.Place> shown = new ArrayList<>();

        @Override
        public int getCount() {
            return this.shown.size();
        }

        @Override
        public Atlas.Place getItem(int position) {
            return this.shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convert, ViewGroup parent) {
            TextView t = convert instanceof TextView ? (TextView) convert
                : new TextView(MainActivity.this);
            t.setPadding(32, 24, 32, 24);
            t.setTextSize(16f);
            t.setText(PhoneChart.label(getItem(position)));
            return t;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence typed) {
                    FilterResults r = new FilterResults();
                    List<Atlas.Place> found = typed == null || typed.length() < 2
                        ? new ArrayList<>() : Atlas.search(typed.toString(), 8);
                    r.values = found;
                    r.count = found.size();
                    return r;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence typed, FilterResults r) {
                    Places.this.shown = r.values == null ? new ArrayList<>()
                        : (List<Atlas.Place>) r.values;
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object value) {
                    return value instanceof Atlas.Place ? PhoneChart.label((Atlas.Place) value) : "";
                }
            };
        }
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
