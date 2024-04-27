package com.stripe.android.paymentsheet.example.playground.customersheet

import android.net.Uri
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.CustomerSheetPlaygroundSettings
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

internal object CustomerSheetPlaygroundUrlHelper {
    fun createUri(settingsJson: String): Uri {
        val base64Settings = settingsJson.encodeToByteArray().toByteString().base64Url()
            .trimEnd('=')
        return Uri.parse(
            "stripepaymentsheetexample://customersheetplayground?settings=" +
                base64Settings
        )
    }

    fun settingsFromUri(uri: Uri?): CustomerSheetPlaygroundSettings? {
        val settingsJson = uri?.getQueryParameter("settings")
            ?.decodeBase64()?.utf8() ?: return null
        return CustomerSheetPlaygroundSettings.createFromJsonString(settingsJson)
    }
}
