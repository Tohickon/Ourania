package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.astro.Ephemeris;
import com.zodiacomputing.ourania.astro.Rodden;
import com.zodiacomputing.ourania.gui.Atlas;
import com.zodiacomputing.ourania.gui.SavedCharts;

import de.thmac.swisseph.SwissEph;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

/**
 * The phone's chart book (M6), on the JVM: charts saved and opened again, written in the
 * desktop's own format, and books moving between the two apps. Each test gets its own book
 * through the redirect the desktop's own check suites use.
 */
public class PhoneBookTest {

    private static final String BOOK_PROPERTY = "ourania.savedCharts";
    private static final LocalDate DAY = LocalDate.of(1975, 7, 4);
    private static final LocalTime TIME = LocalTime.of(14, 15);

    private File book;

    @Before
    public void aFreshBook() throws Exception {
        this.book = File.createTempFile("book", ".properties");
        this.book.delete();
        System.setProperty(BOOK_PROPERTY, this.book.getPath());
    }

    @After
    public void throwItAway() {
        this.book.delete();
        new File(this.book.getPath() + ".legacy").delete();
        System.clearProperty(BOOK_PROPERTY);
    }

    private static Atlas.Place sydney() {
        for (Atlas.Place p : Atlas.search("Sydney", 8)) {
            if (p.country != null && p.country.contains("Australia")) {
                return p;
            }
        }
        throw new AssertionError("Sydney");
    }

    private static PhoneChart.Cast cast(LocalDate d, LocalTime t, Atlas.Place p) {
        return PhoneChart.cast(new SwissEph(Ephemeris.PATH), d, t, p);
    }

    @Test
    public void aSavedChartOpensAsTheSameChart() {
        assertTrue(PhoneBook.save("Jane", DAY, TIME, sydney()));
        assertEquals(java.util.Arrays.asList("Jane"), PhoneBook.names());
        PhoneBook.Birth b = PhoneBook.open("Jane");
        assertNotNull(b);
        assertEquals(DAY, b.date);
        assertEquals(TIME, b.time);
        assertEquals("the same zone", sydney().zoneId, b.place.zoneId);
        PhoneChart.Cast before = cast(DAY, TIME, sydney());
        PhoneChart.Cast after = cast(b.date, b.time, b.place);
        // Four decimals of a degree is eleven metres: the Ascendant moves by far less than this.
        assertEquals("the Ascendant", before.frame.asc, after.frame.asc, 0.01);
        assertEquals("the Moon", before.frame.bodies[1].lon, after.frame.bodies[1].lon, 1e-9);
    }

    @Test
    public void itIsWrittenInTheDesktopsFormat() throws Exception {
        PhoneBook.save("Jane", DAY, TIME, sydney());
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(this.book)) {
            p.load(in);
        }
        assertEquals("1975-07-04", p.getProperty("Jane.date"));
        assertEquals("14:15", p.getProperty("Jane.time"));
        assertTrue(p.getProperty("Jane.location"),
            p.getProperty("Jane.location").matches("-33\\.\\d{4}, 151\\.\\d{4}"));
        assertEquals("A", p.getProperty("Jane.rodden"));
    }

    @Test
    public void anUnknownTimeIsSavedAsRoddenXAndOpensUnknown() {
        PhoneBook.save("No time", DAY, null, sydney());
        assertEquals(Rodden.X, SavedCharts.get("No time").rodden);
        assertEquals("", SavedCharts.get("No time").time);
        assertNull(PhoneBook.open("No time").time);
    }

    @Test
    public void aChartTheDesktopSavedByPlaceNameOpens() {
        // The desktop's form accepts a typed name, and its book holds charts saved that way.
        SavedCharts.put("Typed", "1980-01-01", "12:00", "London");
        PhoneBook.Birth b = PhoneBook.open("Typed");
        assertNotNull(b);
        assertEquals("Europe/London", b.place.zoneId);
        assertEquals(LocalTime.NOON, b.time);
    }

    @Test
    public void whatCannotBeReadOfflineIsRefusedNotGuessed() {
        SavedCharts.put("Nowhere", "1980-01-01", "12:00", "Qxzvtplk Wbfrr");
        SavedCharts.put("Bad date", "1st of June", "12:00", "London");
        assertNull(PhoneBook.open("Nowhere"));
        assertNull(PhoneBook.open("Bad date"));
        assertNull(PhoneBook.open("Never saved"));
    }

    @Test
    public void reSavingKeepsTheDesktopsNotesAndTags() {
        SavedCharts.put("Jane", "1975-07-04", "14:00", "London", Rodden.AA, "from her mother",
            "family");
        PhoneBook.save("Jane", DAY, TIME, sydney());
        SavedCharts.Entry e = SavedCharts.get("Jane");
        assertEquals("from her mother", e.notes);
        assertEquals("family", e.tags);
        assertEquals("a birth certificate stays one", Rodden.AA, e.rodden);
        assertEquals("14:15", e.time);
    }

    @Test
    public void aBookExportedIsImportedAddingButNeverReplacing() throws Exception {
        PhoneBook.save("Jane", DAY, TIME, sydney());
        PhoneBook.save("John", LocalDate.of(1980, 1, 1), null, sydney());
        File exported = File.createTempFile("export", ".properties");
        try {
            assertTrue(PhoneBook.export(exported));
            // Another phone, or the desktop: a book with its own Jane.
            this.book.delete();
            PhoneBook.save("Jane", LocalDate.of(2000, 2, 2), TIME, sydney());
            assertEquals("only John is new", 1, PhoneBook.importFrom(exported));
            assertEquals(java.util.Arrays.asList("Jane", "John"), PhoneBook.names());
            assertEquals("the Jane already here is kept", LocalDate.of(2000, 2, 2),
                PhoneBook.open("Jane").date);
            assertNull("and John's unknown time came across", PhoneBook.open("John").time);
        } finally {
            exported.delete();
        }
    }

    @Test
    public void deletingRemovesIt() {
        PhoneBook.save("Jane", DAY, TIME, sydney());
        assertTrue(PhoneBook.remove("Jane"));
        assertTrue(PhoneBook.names().isEmpty());
        assertNull(PhoneBook.open("Jane"));
    }
}
