package com.stripe.android.paymentsheet.example.playground

import android.net.Uri
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

internal object PaymentSheetPlaygroundUrlHelper {
    fun createUri(settingsJson: String): Uri {
        val base64Settings = settingsJson.encodeToByteArray().toByteString().base64Url()
            .trimEnd('=')
        return Uri.parse(
            "stripepaymentsheetexample://paymentsheetplayground?settings=" +
                base64Settings
        )
    }

    fun settingsFromUri(uri: Uri?): PlaygroundSettings? {
        val settingsJson = uri?.getQueryParameter("settings")
            ?.decodeBase64()?.utf8() ?: return null
        return PlaygroundSettings.createFromJsonString(settingsJson)
    }
}
