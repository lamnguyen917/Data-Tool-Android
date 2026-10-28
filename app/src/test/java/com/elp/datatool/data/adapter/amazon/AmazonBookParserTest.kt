package com.elp.datatool.data.adapter.amazon

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AmazonBookParserTest {

    private val normalizedUrl = "https://www.amazon.com/dp/1098119517"

    @Test
    fun parseProduct_readsBookMetadata() {
        val result = AmazonBookParser.parseProduct(fixture(), "1098119517", normalizedUrl)

        assertEquals(DataSource.AMAZON_BOOKS, result.source)
        assertEquals("1098119517", result.productId)
        assertEquals(ResultStatus.COMPLETED, result.status)
        assertEquals(
            "Flutter and Dart Cookbook: Developing Full-Stack Applications for the Cloud",
            result.title,
        )
        assertEquals(listOf("Richard Rose"), result.creators)
        assertEquals(listOf("Computers & Technology", "Programming", "Mobile Apps"), result.categories)
        assertEquals(normalizedUrl, result.canonicalUrl)
        assertEquals("https://images.example/large.jpg", result.imageUrl)
        assertEquals(
            "Build full-stack Flutter applications for mobile, web, and the cloud.",
            result.description,
        )
    }

    @Test
    fun parseProduct_readsDetailsAndRemovesDirectionMarks() {
        val attributes = AmazonBookParser.parseProduct(fixture(), "1098119517", normalizedUrl).attributes

        assertEquals("O'Reilly Media", attributes[ProductAttributes.PUBLISHER])
        assertEquals("January 24, 2023", attributes[ProductAttributes.PUBLICATION_DATE])
        assertEquals("English", attributes[ProductAttributes.LANGUAGE])
        assertEquals("307 pages", attributes[ProductAttributes.PRINT_LENGTH])
        assertEquals("1098119517", attributes[ProductAttributes.ISBN_10])
        assertEquals("978-1098119515", attributes[ProductAttributes.ISBN_13])
        assertEquals("4.1", attributes[ProductAttributes.RATING])
        assertEquals("23", attributes[ProductAttributes.RATING_COUNT])
    }

    @Test
    fun parseProduct_detectsCaptchaPage() {
        val html = """<html><head><title>Robot Check</title></head><body><form action="/errors/validateCaptcha"></form></body></html>"""

        assertError(FetchErrorKind.BLOCKED, html)
    }

    @Test
    fun parseProduct_detectsSmallSoftBlockPage() {
        assertError(FetchErrorKind.BLOCKED, "<html><body>Automated access</body></html>")
    }

    private fun assertError(expected: FetchErrorKind, html: String) {
        try {
            AmazonBookParser.parseProduct(html, "1098119517", normalizedUrl)
            fail("Expected FetchException")
        } catch (e: FetchException) {
            assertEquals(expected, e.kind)
        }
    }

    private fun fixture(): String =
        checkNotNull(javaClass.classLoader?.getResource("amazon/amazon_book_1098119517.html")).readText()
}
