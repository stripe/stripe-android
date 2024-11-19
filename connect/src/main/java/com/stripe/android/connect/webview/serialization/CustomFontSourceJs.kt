package com.stripe.android.connect.webview.serialization

import android.content.Context
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import kotlinx.serialization.Serializable
import java.io.IOException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@PrivateBetaConnectSDK
@Serializable
internal data class CustomFontSourceJs(
    val family: String,
    val src: String,
)

@PrivateBetaConnectSDK
internal fun CustomFontSource.toJs(context: Context): CustomFontSourceJs {
    val base64FontData = convertFontFileToBase64String(context, assetsFilePath)
    return CustomFontSourceJs(
        family = name,
        src = "url('data:font/ttf;base64,$base64FontData')",
    )
}

/**
 * Reads the font file located in the assets folder at [fontLocation] and converts it to a
 * base64 string.
 * @throws [IOException] if the file cannot be read.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun convertFontFileToBase64String(context: Context, fontLocation: String): String {
    return context.assets.open(fontLocation).use { inputStream ->
        val bytes = inputStream.readBytes()
        Base64.encode(bytes)
    }
}
