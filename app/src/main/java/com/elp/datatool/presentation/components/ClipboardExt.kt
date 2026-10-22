package com.elp.datatool.presentation.components

import android.content.ClipData
import android.os.Build
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

/** Puts plain text on the system clipboard. */
suspend fun Clipboard.copyText(label: String, text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText(label, text)))
}

/**
 * Android 13+ shows its own confirmation when content is copied, so the app only confirms
 * on older versions to avoid duplicate feedback.
 */
val shouldConfirmCopy: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
