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
import com.zodiacomputing.ourania.gui.Settings;

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
    private LinearLayout readings;
    private LinearLayout timing;
    /** M10: the second chart, how the two are shown, and the composite when it is. */
    private static final int BIRTH = 0;
    private static final int SYNASTRY = 1;
    private static final int COMPOSITE = 2;
    private int mode = BIRTH;
    private LinearLayout relRow;
    private Button partnerButton;
    private android.widget.Spinner modePicker;
    private String partnerName;
    private PhoneChart.Cast partner;
    private com.zodiacomputing.ourania.astro.ChartFrame compositeFrame;
    /** The shown sky with its year scanned, made on first asking; null until then. */
    private PhoneTransits.Sky scannedSky;
    private LinearLayout skyRow;
    private CheckBox skyOn;
    private TextView skyDateField;
    private TextView skyCaption;
    /** The day the sky is cast for, or null for now. */
    private LocalDate skyDate;
    private PhoneTransits.Sky shownSky;
    private Button save;
    /** The birth the shown chart was cast from, for saving it. */
    private LocalDate castDate;
    private LocalTime castTime;
    private Atlas.Place castPlace;
    /** The saved name the shown chart was opened from, offered again when it is saved. */
    private String openedName;

    private static final int EXPORT = 1;
    private static final int IMPORT = 2;
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
        // The reader's zodiac, orbs and the rest into the engine before anything is cast (M9) -
        // the list the desktop's window applies at startup.
        Settings.applyToEngine();

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 64, 48, 64);

        // The title, and the chart book beside it (M6).
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Ourania");
        title.setTextSize(26f);
        top.addView(title, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button book = new Button(this);
        book.setText("Chart book");
        book.setOnClickListener(v -> this.showBook());
        top.addView(book);
        Button settings = new Button(this);
        settings.setText("Settings");
        settings.setOnClickListener(v -> this.showSettings());
        top.addView(settings);
        form.addView(top);

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

        // The sky over the chart (M7): on or off, and on which day.
        this.skyRow = new LinearLayout(this);
        this.skyRow.setOrientation(LinearLayout.HORIZONTAL);
        this.skyRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        this.skyRow.setVisibility(View.GONE);
        this.skyOn = new CheckBox(this);
        this.skyOn.setText("Sky on");
        this.skyOn.setOnCheckedChangeListener((b, on) -> {
            if (on && this.mode != BIRTH) {
                this.modePicker.setSelection(BIRTH);    // one outer ring: the sky or a partner
            }
            this.refreshSky();
        });
        this.skyRow.addView(this.skyOn);
        this.skyDateField = field("today");
        this.skyDateField.setPadding(24, 12, 24, 12);
        this.skyDateField.setOnClickListener(v -> this.chooseSkyDate());
        this.skyRow.addView(this.skyDateField, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button skyNow = new Button(this);
        skyNow.setText("Now");
        skyNow.setOnClickListener(v -> {
            this.skyDate = null;
            this.skyOn.setChecked(true);
            this.refreshSky();
        });
        this.skyRow.addView(skyNow);
        form.addView(this.skyRow);
        this.skyCaption = new TextView(this);
        this.skyCaption.setTextSize(14f);
        this.skyCaption.setVisibility(View.GONE);
        form.addView(this.skyCaption);

        // Two charts (M10): whose chart to compare with, and how.
        LinearLayout rel = new LinearLayout(this);
        rel.setOrientation(LinearLayout.HORIZONTAL);
        rel.setGravity(android.view.Gravity.CENTER_VERTICAL);
        this.partnerButton = new Button(this);
        this.partnerButton.setText("Compare with...");
        this.partnerButton.setOnClickListener(v -> this.choosePartner());
        rel.addView(this.partnerButton, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        this.modePicker = this.spinner(new String[] {"Birth chart", "Synastry", "Composite"},
            "Birth chart");
        this.modePicker.setOnItemSelectedListener(
            new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                        int pos, long id) {
                    MainActivity.this.setMode(pos);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
        rel.addView(this.modePicker, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        this.relRow = rel;
        this.relRow.setVisibility(View.GONE);
        form.addView(this.relRow);
        this.tapped = new TextView(this);
        this.tapped.setTextSize(16f);
        this.tapped.setPadding(0, 16, 0, 0);
        this.tapped.setVisibility(View.GONE);
        form.addView(this.tapped);

        // The whole chart's readings (M5): the synthesis, and the aspect patterns.
        this.readings = new LinearLayout(this);
        this.readings.setOrientation(LinearLayout.HORIZONTAL);
        this.readings.setVisibility(View.GONE);
        Button whole = new Button(this);
        whole.setText("Whole-chart reading");
        whole.setOnClickListener(v -> this.showReading(false));
        Button patterns = new Button(this);
        patterns.setText("Patterns");
        patterns.setOnClickListener(v -> this.showReading(true));
        Button transits = new Button(this);
        transits.setText("Transits");
        transits.setOnClickListener(v -> this.showTransits());
        this.readings.addView(whole, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        this.readings.addView(patterns, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        form.addView(this.readings);
        // Timing (M7): the transits now, and the calendar of the year ahead.
        this.timing = new LinearLayout(this);
        this.timing.setOrientation(LinearLayout.HORIZONTAL);
        this.timing.setVisibility(View.GONE);
        Button calendar = new Button(this);
        calendar.setText("Calendar");
        calendar.setOnClickListener(v -> this.showCalendar());
        this.timing.addView(transits, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        this.timing.addView(calendar, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button releasing = new Button(this);
        releasing.setText("Releasing");
        releasing.setOnClickListener(v -> this.showReleasing());
        this.timing.addView(releasing, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        form.addView(this.timing);
        this.save = new Button(this);
        this.save.setText("Save this chart");
        this.save.setVisibility(View.GONE);
        this.save.setOnClickListener(v -> this.saveChart());
        form.addView(this.save);

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
                this.readings.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                this.timing.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                this.relRow.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                this.skyRow.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                this.skyCaption.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                this.save.setVisibility(cast == null ? View.GONE : View.VISIBLE);
                this.castDate = d;
                this.castTime = t;
                this.castPlace = p;
                if (cast != null) {
                    this.shownSky = null;
                    this.compositeFrame = null;
                    this.wheel.show(cast.frame);
                    showBody(-1);
                    this.redraw();
                }
                this.result.setText(shown);
                this.cast.setEnabled(true);
            });
        }).start();
    }

    /**
     * The settings (M9): house system, zodiac, transit orb, each planet's orb and the aspects
     * drawn. Saved through {@link PhoneSettings}; the chart on screen is cast again with them.
     */
    private void showSettings() {
        final PhoneSettings.Values v = PhoneSettings.read();
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(48, 16, 48, 16);

        f.addView(label("House system"));
        final android.widget.Spinner houses = spinner(PhoneSettings.houseSystems(), v.houseSystem);
        f.addView(houses);
        f.addView(label("Zodiac"));
        final android.widget.Spinner zodiac = spinner(PhoneSettings.zodiacs(), v.zodiac);
        f.addView(zodiac);

        f.addView(label("Transit orb, in degrees (how close a transit must be to count)"));
        final android.widget.EditText transitOrb = number(v.transitOrb);
        f.addView(transitOrb);

        f.addView(label("Orb of each planet in the birth chart, in degrees"));
        final android.widget.EditText[] orbs = new android.widget.EditText[v.orbs.length];
        for (int i = 0; i < orbs.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = new TextView(this);
            name.setText(PhoneSettings.planet(i));
            name.setTextSize(16f);
            row.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            orbs[i] = number(v.orbs[i]);
            row.addView(orbs[i], new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            f.addView(row);
        }

        f.addView(label("Aspects drawn on the wheel and read for each planet"));
        final CheckBox[] aspects = new CheckBox[com.zodiacomputing.ourania.astro.Aspects.Type
            .values().length];
        for (com.zodiacomputing.ourania.astro.Aspects.Type t
                : com.zodiacomputing.ourania.astro.Aspects.Type.values()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(t.label);
            cb.setChecked(t.ordinal() < v.aspects.length && v.aspects[t.ordinal()]);
            aspects[t.ordinal()] = cb;
            f.addView(cb);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(f);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Settings")
            .setView(scroll)
            .setPositiveButton("Save", (d, w) -> {
                v.houseSystem = (String) houses.getSelectedItem();
                v.zodiac = (String) zodiac.getSelectedItem();
                v.transitOrb = parse(transitOrb, v.transitOrb);
                for (int i = 0; i < orbs.length; i++) {
                    v.orbs[i] = parse(orbs[i], v.orbs[i]);
                }
                boolean[] on = new boolean[aspects.length];
                for (int i = 0; i < on.length; i++) {
                    on[i] = aspects[i].isChecked();
                }
                v.aspects = on;
                PhoneSettings.save(v);
                this.settingsChanged("Saved.");
            })
            .setNeutralButton("Reset all", (d, w) -> new android.app.AlertDialog.Builder(this)
                .setMessage("Put every setting on this screen back to its default?")
                .setPositiveButton("Reset", (d2, w2) -> {
                    PhoneSettings.reset();
                    this.settingsChanged("Settings reset.");
                })
                .setNegativeButton("Cancel", null)
                .show())
            .setNegativeButton("Cancel", null)
            .show();
    }

    /** Casts the chart on screen again, so a new house system or orb shows at once. */
    private void settingsChanged(String message) {
        toast(message);
        this.scannedSky = null;
        if (this.shownCast != null && this.date != null && this.place != null) {
            this.castChart();
        }
    }

    private android.widget.Spinner spinner(String[] items, String selected) {
        android.widget.Spinner sp = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> a = new android.widget.ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(selected)) {
                sp.setSelection(i);
            }
        }
        return sp;
    }

    private android.widget.EditText number(double value) {
        android.widget.EditText e = new android.widget.EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setText(String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "")
            .replaceAll("\\.$", ""));
        return e;
    }

    /** A typed number, or the old value when the box does not hold one. */
    private static double parse(android.widget.EditText e, double fallback) {
        try {
            return Double.parseDouble(e.getText().toString().trim().replace(',', '.'));
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    /** Names the shown chart and saves it into the book, asking before replacing one. */
    private void saveChart() {
        if (this.castDate == null || this.castPlace == null) {
            return;
        }
        final android.widget.EditText name = new android.widget.EditText(this);
        name.setHint("Name, e.g. Jane Smith");
        name.setSingleLine(true);
        if (this.openedName != null) {
            name.setText(this.openedName);
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Save this chart")
            .setView(name)
            .setPositiveButton("Save", (dlg, w) -> {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) {
                    toast("A saved chart needs a name.");
                } else if (PhoneBook.exists(n)) {
                    new android.app.AlertDialog.Builder(this)
                        .setMessage("\"" + n + "\" is already in the book. Replace it?")
                        .setPositiveButton("Replace", (d2, w2) -> this.writeChart(n))
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    this.writeChart(n);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void writeChart(String name) {
        boolean ok = PhoneBook.save(name, this.castDate, this.castTime, this.castPlace);
        if (ok) {
            this.openedName = name;
        }
        toast(ok ? "Saved \"" + name + "\"." : "The chart book could not be written.");
    }

    /** The book: tap a name to open or delete it; export and import the whole book. */
    private void showBook() {
        final java.util.List<String> names = PhoneBook.names();
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this)
            .setTitle("Chart book (" + names.size() + ")")
            .setPositiveButton("Export", (d, w) -> this.pickFile(true))
            .setNeutralButton("Import", (d, w) -> this.pickFile(false))
            .setNegativeButton("Close", null);
        if (names.isEmpty()) {
            b.setMessage("No saved charts yet. Cast a chart and tap \"Save this chart\", or "
                + "import a book exported from the desktop.");
        } else {
            b.setItems(names.toArray(new String[0]), (d, i) -> this.chartActions(names.get(i)));
        }
        b.show();
    }

    private void chartActions(String name) {
        new android.app.AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(new String[] {"Open", "Delete"}, (d, i) -> {
                if (i == 0) {
                    this.openChart(name);
                } else {
                    new android.app.AlertDialog.Builder(this)
                        .setMessage("Delete \"" + name + "\" from the book?")
                        .setPositiveButton("Delete", (d2, w2) -> {
                            toast(PhoneBook.remove(name) ? "Deleted." : "Could not delete.");
                            if (name.equals(this.openedName)) {
                                this.openedName = null;
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            })
            .show();
    }

    /** Fills the form from a saved chart and casts it. */
    private void openChart(String name) {
        PhoneBook.Birth b = PhoneBook.open(name);
        if (b == null) {
            toast("\"" + name + "\" could not be opened: its date or place cannot be read "
                + "offline. Open it on the desktop and save it again with a place from the list.");
            return;
        }
        this.openedName = name;
        this.date = b.date;
        this.dateField.setText(b.date.toString());
        this.timeUnknown.setChecked(b.time == null);
        if (b.time != null) {
            this.time = b.time;
            this.timeField.setText(String.format("%02d:%02d", b.time.getHour(),
                b.time.getMinute()));
        }
        this.place = b.place;
        this.placeField.setText(PhoneChart.label(b.place), false);
        this.castChart();
    }

    /** Asks Android for a file to export the book to, or one to import. */
    private void pickFile(boolean export) {
        android.content.Intent i = new android.content.Intent(export
            ? android.content.Intent.ACTION_CREATE_DOCUMENT
            : android.content.Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        i.setType(export ? "text/plain" : "*/*");
        if (export) {
            i.putExtra(android.content.Intent.EXTRA_TITLE, "ourania-chart-book.properties");
        }
        startActivityForResult(i, export ? EXPORT : IMPORT);
    }

    /**
     * The chosen file. The book's own export and import work on files, so the phone's
     * document goes through a file in the app's cache on the way.
     */
    @Override
    protected void onActivityResult(int request, int result, android.content.Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        android.net.Uri uri = data.getData();
        File temp = new File(getCacheDir(), "chart-book.properties");
        try {
            if (request == EXPORT) {
                if (!PhoneBook.export(temp)) {
                    toast("The book could not be exported.");
                    return;
                }
                try (InputStream in = new java.io.FileInputStream(temp);
                     OutputStream out = getContentResolver().openOutputStream(uri)) {
                    copy(in, out);
                }
                toast("Exported " + PhoneBook.names().size() + " charts.");
            } else if (request == IMPORT) {
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(temp)) {
                    copy(in, out);
                }
                int added = PhoneBook.importFrom(temp);
                toast(added == 0 ? "No new charts: the file had none, or all were already here."
                    : "Imported " + added + (added == 1 ? " chart." : " charts."));
            }
        } catch (IOException | RuntimeException e) {
            toast("That did not work: " + e.getMessage());
        } finally {
            temp.delete();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1 << 14];
        for (int n; (n = in.read(buf)) > 0; ) {
            out.write(buf, 0, n);
        }
    }

    private void toast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }

    /**
     * The whole-chart synthesis, or every aspect pattern, in the reading panel under the wheel.
     * Computed off the main thread: the synthesis ranks and scores every body.
     */
    private void showReading(boolean patterns) {
        final PhoneChart.Cast c = this.shownCast;
        if (c == null) {
            return;
        }
        this.tapped.setText(patterns ? "Finding the patterns..." : "Reading the whole chart...");
        new Thread(() -> {
            String html;
            try {
                com.zodiacomputing.ourania.astro.ChartFrame comp = this.compositeFrame;
                PhoneChart.Cast other = this.partner;
                if (patterns) {
                    html = this.mode == COMPOSITE && comp != null ? PhoneReading.patterns(comp)
                        : PhoneReading.patterns(c);
                } else if (this.mode == COMPOSITE && comp != null) {
                    html = PhoneRelationship.compositeSynthesis(comp);
                } else if (this.mode == SYNASTRY && other != null) {
                    html = PhoneRelationship.synastry(this.chartName(), c.frame, this.partnerName,
                        other.frame, InterpretationService.getInstance());
                } else if (c.timeUnknown) {
                    html = "<p><i>Without a birth time there is no Ascendant to count the years "
                        + "from, so this reading is the birth chart alone.</i></p>"
                        + PhoneReading.synthesis(c);
                } else {
                    // With the timing sections, at the sky's moment (now unless a day is set).
                    html = PhoneTransits.synthesis(c, this.scanned(c));
                }
                if (html.isEmpty()) {
                    html = "<p>This chart has no aspect patterns - no T-squares, grand trines, "
                        + "yods or the like among its planets.</p>";
                } else if (patterns) {
                    html = "<h2>Aspect patterns</h2>" + html;
                }
            } catch (Throwable e) {
                html = "<p>The reading could not be made: " + Html.escapeHtml(e.toString()) + "</p>";
            }
            final String shown = html;
            runOnUiThread(() -> {
                if (this.shownCast == c) {
                    this.tapped.setText(Html.fromHtml(shown, Html.FROM_HTML_MODE_COMPACT));
                }
            });
        }).start();
    }

    /** The name the chart on screen goes by in a comparison: its saved name, or "You". */
    private String chartName() {
        return this.openedName != null ? this.openedName : "You";
    }

    /** Picks the second chart from the book, casts it, and shows the two as synastry. */
    private void choosePartner() {
        final java.util.List<String> names = PhoneBook.names();
        if (names.isEmpty()) {
            toast("Save the other person's chart first: cast it, tap \"Save this chart\", then "
                + "come back to your own.");
            return;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Compare with")
            .setItems(names.toArray(new String[0]), (d, i) -> this.loadPartner(names.get(i)))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadPartner(String name) {
        final PhoneBook.Birth b = PhoneBook.open(name);
        if (b == null) {
            toast("\"" + name + "\" could not be opened offline.");
            return;
        }
        new Thread(() -> {
            PhoneChart.Cast other;
            try {
                other = PhoneChart.cast(new SwissEph(Ephemeris.PATH), b.date, b.time, b.place);
            } catch (Throwable e) {
                other = null;
            }
            final PhoneChart.Cast shown = other;
            runOnUiThread(() -> {
                if (shown == null) {
                    toast("That chart could not be cast.");
                    return;
                }
                this.partner = shown;
                this.partnerName = name;
                this.compositeFrame = null;
                this.partnerButton.setText("With " + name);
                if (this.mode == BIRTH) {
                    this.modePicker.setSelection(SYNASTRY);   // calls setMode
                } else {
                    this.redraw();
                }
            });
        }).start();
    }

    private void setMode(int m) {
        if (m != BIRTH && this.partner == null) {
            if (this.mode == BIRTH) {
                this.modePicker.setSelection(BIRTH);
                if (this.shownCast != null) {
                    toast("Choose whose chart to compare with first.");
                }
                return;
            }
        }
        this.mode = m;
        if (m != BIRTH && this.skyOn.isChecked()) {
            this.skyOn.setChecked(false);
        }
        this.redraw();
    }

    /** Puts the right chart or charts on the wheel for the mode. */
    private void redraw() {
        final PhoneChart.Cast c = this.shownCast;
        if (c == null) {
            return;
        }
        if (this.mode == SYNASTRY && this.partner != null) {
            this.shownSky = null;
            this.wheel.show(c.frame, this.partner.frame, PhoneRelationship.crosses(
                PhoneRelationship.contacts(c.frame, this.partner.frame)));
            this.skyCaption.setText(PhoneRelationship.possessive(this.partnerName).trim()
                + " planets are on the outer ring. Tap one, or Whole-chart reading for the "
                + "synastry.");
            showBody(-1);
        } else if (this.mode == COMPOSITE && this.partner != null) {
            this.shownSky = null;
            final PhoneChart.Cast other = this.partner;
            this.skyCaption.setText("Casting the composite...");
            new Thread(() -> {
                com.zodiacomputing.ourania.astro.ChartFrame comp;
                try {
                    comp = PhoneRelationship.composite(new SwissEph(Ephemeris.PATH), c.frame,
                        other.frame);
                } catch (Throwable e) {
                    comp = null;
                }
                final com.zodiacomputing.ourania.astro.ChartFrame shown = comp;
                runOnUiThread(() -> {
                    if (this.shownCast != c || this.partner != other) {
                        return;
                    }
                    if (shown == null) {
                        this.skyCaption.setText("The composite could not be cast.");
                        return;
                    }
                    this.compositeFrame = shown;
                    this.wheel.show(shown);
                    this.skyCaption.setText("The composite of " + this.chartName() + " and "
                        + this.partnerName + ": the relationship as a chart of its own.");
                    showBody(-1);
                });
            }).start();
        } else {
            this.refreshSky();
        }
    }

    /**
     * Casts the sky for the chosen day (or now) and puts it on the wheel, or takes it off.
     * Off the main thread: the contacts rank the chart's bodies first.
     */
    private void refreshSky() {
        final PhoneChart.Cast c = this.shownCast;
        if (c == null) {
            return;
        }
        if (!this.skyOn.isChecked()) {
            this.shownSky = null;
            this.wheel.show(c.frame);
            this.skyCaption.setText("Turn the sky on to see today's planets around the chart.");
            return;
        }
        final LocalDate day = this.skyDate;
        this.skyCaption.setText("Casting the sky...");
        new Thread(() -> {
            PhoneTransits.Sky sky;
            try {
                java.time.ZonedDateTime now = PhoneTransits.now(c.place.zoneId);
                java.time.ZonedDateTime when = day == null ? now
                    : day.atTime(now.toLocalTime()).atZone(now.getZone());
                sky = PhoneTransits.at(new SwissEph(Ephemeris.PATH), c, when);
            } catch (Throwable e) {
                sky = null;
            }
            final PhoneTransits.Sky shown = sky;
            runOnUiThread(() -> {
                if (this.shownCast != c || this.mode != BIRTH || !this.skyOn.isChecked()) {
                    return;                             // the view moved on while it was cast
                }
                if (shown == null) {
                    this.skyCaption.setText("The sky could not be cast.");
                    return;
                }
                this.shownSky = shown;
                this.wheel.show(c.frame, shown.frame,
                    PhoneWheel.crosses(shown.hits, shown.frame, c.frame));
                this.skyDateField.setText(day == null ? "now" : day.toString());
                this.skyCaption.setText(PhoneTransits.headline(shown)
                    + ". Tap a planet on the outer ring, or Transits for the full reading.");
                showBody(-1);
            });
        }).start();
    }

    private void chooseSkyDate() {
        LocalDate start = this.skyDate != null ? this.skyDate : LocalDate.now();
        new DatePickerDialog(this, (picker, y, m, d) -> {
            this.skyDate = LocalDate.of(y, m + 1, d);
            this.skyOn.setChecked(true);
            this.refreshSky();
        }, start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth()).show();
    }

    /**
     * The sky at the shown moment with its year scanned, cast once and kept until the chart or
     * the moment changes. Called off the main thread.
     */
    private PhoneTransits.Sky scanned(PhoneChart.Cast c) {
        PhoneTransits.Sky shown = this.shownSky;
        java.time.ZonedDateTime when = shown != null ? shown.when
            : PhoneTransits.now(c.place.zoneId);
        PhoneTransits.Sky have = this.scannedSky;
        if (have != null && have.when.equals(when) && this.shownCast == c) {
            return have;
        }
        have = PhoneTransits.at(new SwissEph(Ephemeris.PATH), c, when, true);
        this.scannedSky = have;
        return have;
    }

    /** Zodiacal releasing from both Lots, at the sky's moment (now unless a day is set). */
    private void showReleasing() {
        final PhoneChart.Cast c = this.shownCast;
        if (c == null) {
            return;
        }
        if (c.timeUnknown) {
            this.tapped.setText("Releasing starts from the Lots, which are measured from the "
                + "Ascendant, so it needs a birth time.");
            return;
        }
        PhoneTransits.Sky sky = this.shownSky;
        java.time.ZonedDateTime when = sky != null ? sky.when : PhoneTransits.now(c.place.zoneId);
        this.tapped.setText(Html.fromHtml(PhoneTransits.releasing(c, when),
            Html.FROM_HTML_MODE_COMPACT));
    }

    /** The year ahead, month by month: what perfects on this chart, and when. */
    private void showCalendar() {
        final PhoneChart.Cast c = this.shownCast;
        if (c == null) {
            return;
        }
        if (c.timeUnknown) {
            this.tapped.setText("The calendar counts the year from the birthday's Ascendant, "
                + "so it needs a birth time.");
            return;
        }
        this.tapped.setText("Scanning the year ahead...");
        new Thread(() -> {
            String html;
            try {
                html = PhoneTransits.calendar(this.scanned(c));
            } catch (Throwable e) {
                html = "<p>The calendar could not be made: " + Html.escapeHtml(e.toString())
                    + "</p>";
            }
            final String shown = html;
            runOnUiThread(() -> {
                if (this.shownCast == c) {
                    this.tapped.setText(Html.fromHtml(shown, Html.FROM_HTML_MODE_COMPACT));
                }
            });
        }).start();
    }

    /** Every transit contact now (or on the chosen day), with the year and their readings. */
    private void showTransits() {
        if (this.shownSky == null) {
            this.skyOn.setChecked(true);                // casts it; tap Transits again to read
            this.tapped.setText("Casting the sky - tap Transits again in a moment.");
            return;
        }
        final PhoneChart.Cast c = this.shownCast;
        final PhoneTransits.Sky sky = this.shownSky;
        this.tapped.setText(Html.fromHtml(
            PhoneTransits.reading(c, sky, InterpretationService.getInstance()),
            Html.FROM_HTML_MODE_COMPACT));
    }

    /** A tapped planet's full reading under the wheel (M5); a hint when none is tapped. */
    private void showBody(int body) {
        InterpretationService svc = InterpretationService.getInstance();
        String html = this.shownCast == null || body < 0 ? ""
            : this.mode == COMPOSITE && this.compositeFrame != null
                ? PhoneRelationship.compositePlanet(this.compositeFrame, body, svc)
            : this.mode == SYNASTRY && this.partner != null && body >= PhoneWheel.SKY
                ? PhoneRelationship.partnerPlanet(this.chartName(), this.shownCast.frame,
                    this.partnerName, this.partner.frame, body - PhoneWheel.SKY, svc)
            : body >= PhoneWheel.SKY && this.shownSky != null
                ? PhoneTransits.skyPlanet(this.shownCast, this.shownSky, body - PhoneWheel.SKY,
                    InterpretationService.getInstance())
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
