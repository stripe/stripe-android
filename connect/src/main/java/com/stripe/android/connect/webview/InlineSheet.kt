package com.stripe.android.connect.webview

import android.net.Uri

/**
 * A native sheet showing a window that an inline component's Connect.js opened for one of its dialogs.
 */
internal data class InlineSheet(
    val id: String,
    val state: State,
    /**
     * Height of the sheet's content in CSS pixels, or null until Connect.js first reports it.
     */
    val height: Float?,
    /**
     * False while Connect.js needs to confirm before its dialog closes, so the user can only ask to close it.
     */
    val isDismissible: Boolean,
) {
    enum class State {
        /**
         * Laid out off screen so Connect.js can draw and measure its dialog, but not shown.
         */
        STAGED,

        /**
         * Shown to the user.
         */
        SHOWING,

        /**
         * Connect.js closed the window, and the sheet should animate away.
         */
        CLOSING,
    }
}

internal object InlineContract {
    private const val MAX_HEIGHT = 10_000.0
    private const val OS_SHEET_PARAM = "os_sheet"
    private const val OS_SHEET_ID_PARAM = "os_sheet_id"

    private val SHEET_ID = Regex("^[A-Za-z0-9-]{1,64}$")
    private val ACCESSORY_LAYER_PATH = Regex("^/accessory_layer_[A-Za-z0-9]+\\.html$")

    /**
     * Returns [height] in CSS pixels clamped to the range Connect.js sends, or null if it isn't a number.
     */
    fun clampHeight(height: Double): Float? {
        return height.takeIf { it.isFinite() }?.coerceIn(0.0, MAX_HEIGHT)?.toFloat()
    }

    /**
     * Returns the sheet id if [url] is a window Connect.js opened to show as a native sheet, or null if it isn't.
     * The window must be an accessory layer page on the exact scheme, host, and port of [componentUrl].
     */
    fun sheetIdFromWindowUrl(url: Uri, componentUrl: Uri): String? {
        if (!isSameOrigin(url, componentUrl) || !isAccessoryLayerPath(url)) return null

        val params = parseHashParams(url.encodedFragment) ?: return null
        if (params[OS_SHEET_PARAM] != "true") return null
        return params[OS_SHEET_ID_PARAM]?.takeIf(SHEET_ID::matches)
    }

    private fun isSameOrigin(url: Uri, componentUrl: Uri): Boolean {
        val scheme = url.scheme ?: return false
        val host = url.host ?: return false
        return scheme.equals(componentUrl.scheme, ignoreCase = true) &&
            host.equals(componentUrl.host, ignoreCase = true) &&
            url.port == componentUrl.port &&
            url.encodedUserInfo == null
    }

    // Connect.js serves accessory layers from its origin root, not the SDK page's versioned folder.
    private fun isAccessoryLayerPath(url: Uri): Boolean {
        return url.encodedPath?.let(ACCESSORY_LAYER_PATH::matches) == true
    }

    /**
     * Parses a hash formatted like a query string, or returns null if it's missing or repeats a name.
     */
    private fun parseHashParams(encodedFragment: String?): Map<String, String>? {
        if (encodedFragment.isNullOrEmpty()) return null
        val params = mutableMapOf<String, String>()
        encodedFragment.split('&').filter { it.isNotEmpty() }.forEach { pair ->
            val name = decodeParam(pair.substringBefore('='))
            val value = decodeParam(pair.substringAfter('=', missingDelimiterValue = ""))
            if (params.put(name, value) != null) return null
        }
        return params
    }

    private fun decodeParam(value: String): String = Uri.decode(value)
}
