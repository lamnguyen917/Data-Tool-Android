package com.elp.datatool.data.parse

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

/**
 * Returns the index just after the JSON value (object, array or string) that starts at [start],
 * or -1 if the value is not closed. Strings and escapes are respected.
 */
fun findJsonEnd(text: String, start: Int): Int {
    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until text.length) {
        val c = text[i]
        if (inString) {
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> {
                    inString = false
                    if (depth == 0) return i + 1
                }
            }
            continue
        }
        when (c) {
            '"' -> inString = true
            '{', '[' -> depth++
            '}', ']' -> {
                depth--
                if (depth == 0) return i + 1
            }
        }
    }
    return -1
}

fun JsonElement?.obj(): JsonObject? = this as? JsonObject

fun JsonElement?.arr(): JsonArray? = this as? JsonArray

/** Primitive content as a string; null for JSON null, objects and arrays. */
fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

fun JsonObject.obj(name: String): JsonObject? = this[name].obj()

fun JsonObject.arr(name: String): JsonArray? = this[name].arr()

/** Non-blank string field. */
fun JsonObject.text(name: String): String? = this[name].str()?.takeIf { it.isNotBlank() }

/** Converts an HTML fragment to readable plain text with paragraph and list breaks. */
fun htmlToPlainText(html: String): String {
    if (html.isBlank()) return ""
    val body = Jsoup.parseBodyFragment(html).body()
    val builder = StringBuilder()
    NodeTraversor.traverse(object : NodeVisitor {
        override fun head(node: Node, depth: Int) {
            when {
                node is TextNode -> builder.append(node.text())
                node is Element && node.normalName() == "br" -> builder.append('\n')
                node is Element && node.normalName() == "li" -> builder.ensureBreak(1).append("• ")
                node is Element && node.isBlockLike() -> builder.ensureBreak(2)
            }
        }

        override fun tail(node: Node, depth: Int) {
            if (node is Element && (node.isBlockLike() || node.normalName() == "li")) builder.ensureBreak(1)
        }
    }, body)
    return builder.toString()
        .lines()
        .joinToString("\n") { it.trim() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private val BLOCK_TAGS = setOf("p", "div", "ul", "ol", "h1", "h2", "h3", "h4", "h5", "h6", "section", "table", "tr")

private fun Element.isBlockLike() = normalName() in BLOCK_TAGS

/** Ensures the text ends with at least [count] line breaks (no-op at the start). */
private fun StringBuilder.ensureBreak(count: Int): StringBuilder {
    if (isEmpty()) return this
    var trailing = 0
    var i = length - 1
    while (i >= 0 && (this[i] == '\n' || this[i] == ' ')) {
        if (this[i] == '\n') trailing++
        i--
    }
    repeat(count - trailing) { append('\n') }
    return this
}
