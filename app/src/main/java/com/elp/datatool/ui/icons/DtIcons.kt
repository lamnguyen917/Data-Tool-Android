package com.elp.datatool.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Stroke icons used by the wireframes (Lucide, ISC license), converted to [ImageVector].
 * The stroke is drawn in black and tinted by `Icon`.
 */
object DtIcons {
    val Search by lazy { icon("Search", circle(11f, 11f, 8f), "m21 21-4.3-4.3") }
    val SearchX by lazy {
        icon("SearchX", circle(11f, 11f, 8f), "m21 21-4.3-4.3", "m13.5 8.5-5 5", "m8.5 8.5 5 5")
    }
    val Help by lazy {
        icon("Help", circle(12f, 12f, 10f), "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01")
    }
    val Clipboard by lazy {
        icon(
            "Clipboard",
            rect(8f, 2f, 8f, 4f, 1f),
            "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2",
        )
    }
    val Trash by lazy {
        icon(
            "Trash",
            "M3 6h18",
            "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6",
            "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",
            "M10 11v6",
            "M14 11v6",
        )
    }
    val Package by lazy { icon("Package", "M21 8 12 3 3 8v8l9 5 9-5z", "m3 8 9 5 9-5", "M12 13v8") }
    val Alert by lazy {
        icon(
            "Alert",
            "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3",
            "M12 9v4",
            "M12 17h.01",
        )
    }
    val Inbox by lazy {
        icon(
            "Inbox",
            "M22 12h-6l-2 3h-4l-2-3H2",
            "M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z",
        )
    }
    val LayoutList by lazy {
        icon(
            "LayoutList",
            rect(3f, 3f, 7f, 7f, 1f),
            rect(3f, 14f, 7f, 7f, 1f),
            "M14 4h7",
            "M14 9h7",
            "M14 15h7",
            "M14 20h7",
        )
    }
    val Settings by lazy {
        icon(
            "Settings",
            "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
            circle(12f, 12f, 3f),
        )
    }
    val ArrowLeft by lazy { icon("ArrowLeft", "m12 19-7-7 7-7", "M19 12H5") }
    val Close by lazy { icon("Close", "M18 6 6 18", "m6 6 12 12") }
    val Check by lazy { icon("Check", "M20 6 9 17l-5-5", strokeWidth = 2.5f) }
    val ChevronDown by lazy { icon("ChevronDown", "m6 9 6 6 6-6") }
    val ChevronRight by lazy { icon("ChevronRight", "m9 18 6-6-6-6") }
    val Sort by lazy { icon("Sort", "m21 16-4 4-4-4", "M17 20V4", "m3 8 4-4 4 4", "M7 4v16") }
    val Image by lazy {
        icon(
            "Image",
            rect(3f, 3f, 18f, 18f, 2f),
            circle(9f, 9f, 2f),
            "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21",
        )
    }
    val Hash by lazy { icon("Hash", "M4 9h16", "M4 15h16", "M10 3 8 21", "M16 3l-2 18") }
    val User by lazy { icon("User", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", circle(12f, 7f, 4f)) }
    val Retry by lazy { icon("Retry", "M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8", "M21 3v5h-5") }
    val Share by lazy {
        icon(
            "Share",
            circle(18f, 5f, 3f),
            circle(6f, 12f, 3f),
            circle(18f, 19f, 3f),
            "m8.59 13.51 6.83 3.98",
            "m15.41 6.51-6.82 3.98",
        )
    }
    val Download by lazy {
        icon("Download", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5", "M12 15V3")
    }
    val Copy by lazy {
        icon(
            "Copy",
            rect(8f, 8f, 14f, 14f, 2f),
            "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2",
        )
    }
    val ExternalLink by lazy {
        icon(
            "ExternalLink",
            "M15 3h6v6",
            "M10 14 21 3",
            "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6",
        )
    }
}

private fun icon(name: String, vararg paths: String, strokeWidth: Float = 2f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { path ->
            addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** SVG path for a circle, drawn as two arcs. */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

/** SVG path for a rounded rectangle. */
private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float): String {
    val iw = w - 2 * rx
    val ih = h - 2 * rx
    return "M${x + rx} ${y}h${iw}a$rx $rx 0 0 1 $rx ${rx}v${ih}a$rx $rx 0 0 1 ${-rx} ${rx}" +
        "h${-iw}a$rx $rx 0 0 1 ${-rx} ${-rx}v${-ih}a$rx $rx 0 0 1 $rx ${-rx}z"
}
