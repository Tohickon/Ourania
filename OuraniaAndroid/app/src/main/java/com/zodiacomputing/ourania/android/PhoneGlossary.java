package com.zodiacomputing.ourania.android;

import com.zodiacomputing.ourania.gui.Glossary;

/**
 * A glossary entry as the phone shows it: where it sits, then its definition. No Android in
 * here, like {@link PhoneChart}: {@code PhoneGlossaryTest} runs it on the JVM.
 */
final class PhoneGlossary {

    private PhoneGlossary() { }

    static String html(Glossary.Term t) {
        StringBuilder h = new StringBuilder();
        h.append("<p><small><i>").append(escape(t.chapter));
        if (!t.section.isEmpty()) {
            h.append(" &middot; ").append(escape(t.section));
        }
        h.append("</i></small></p><p>").append(t.text).append("</p>");
        return h.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
