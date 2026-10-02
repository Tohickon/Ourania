package com.zodiacomputing.ourania.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.zodiacomputing.ourania.gui.Glossary;

import org.junit.Test;

/** The glossary on the phone's side of the build, read from the desktop's data folder. */
public class PhoneGlossaryTest {

    @Test
    public void theGlossaryLoadsOnThePhonesPath() {
        assertNull(Glossary.failure());
        assertEquals(827, Glossary.all().size());
        assertEquals(19, Glossary.chapters().size());
    }

    @Test
    public void aSearchFindsTheTermAndShowsWhereItSits() {
        Glossary.Term t = Glossary.search("cazimi", 5).get(0);
        assertEquals("Cazimi", t.term);
        String h = PhoneGlossary.html(t);
        assertTrue(h, h.startsWith("<p><small><i>A to Z Essentials</i></small></p>")
            && h.contains("17 minutes of arc"));
    }

    @Test
    public void aChaptersAmpersandIsEscaped() {
        Glossary.Term t = Glossary.chapter("Houses, Angles & Systems of Domification").get(0);
        assertTrue(PhoneGlossary.html(t).contains("Houses, Angles &amp; Systems"));
    }
}
