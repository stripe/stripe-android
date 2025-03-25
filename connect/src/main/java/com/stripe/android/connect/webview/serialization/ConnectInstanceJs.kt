package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class ConnectInstanceJs(
    val appearance: AppearanceJs? = null,
    val fonts: List<CustomFontSourceJs>? = null,
) {
    /**
     * Helper function similar to [toString], intended for printing
     * this class to the debugger/stdout/etc.
     */
    @Suppress("MagicNumber")
    fun toDebugString(): String {
        // the fonts.src field is potentially very long due to base64 font data, so when printing
        // the string for debugging (ex. stdout) we truncate to a max of 100 characters.
        return copy(
            fonts = fonts?.map { font ->
                if (font.src.length <= 100) {
                    font
                } else {
                    val truncatedSrc = font.src.take(50) + "..." + font.src.takeLast(50)
                    font.copy(src = truncatedSrc)
                }
            }
        ).toString()
    }
}
