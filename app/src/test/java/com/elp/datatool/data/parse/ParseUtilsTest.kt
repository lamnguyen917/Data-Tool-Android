package com.elp.datatool.data.parse

import org.junit.Assert.assertEquals
import org.junit.Test

class ParseUtilsTest {

    @Test
    fun findJsonEnd_skipsBracketsInsideStrings() {
        val text = """call({"a":"}]\"x","b":[1,{"c":2}]},next)"""
        val start = text.indexOf('{')
        assertEquals("""{"a":"}]\"x","b":[1,{"c":2}]}""", text.substring(start, findJsonEnd(text, start)))
    }

    @Test
    fun findJsonEnd_returnsMinusOneWhenUnclosed() {
        assertEquals(-1, findJsonEnd("""{"a":[1,2""", 0))
    }

    @Test
    fun htmlToPlainText_keepsParagraphsAndListItems() {
        val html = "<p><strong>Title</strong> intro &amp; more.</p></br><ul><li>One</li><li>Two</li></ul><p>End<br>line</p>"
        assertEquals("Title intro & more.\n\n• One\n• Two\n\nEnd\nline", htmlToPlainText(html))
    }
}
