package com.stripe.android.connect

import android.os.Parcelable
import com.stripe.android.connect.webview.serialization.ConnectJson
import com.stripe.android.connect.webview.serialization.toJs
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

internal fun Any.toComponentPropsJsonObject(): JsonObject {
    return when (this) {
        EmptyProps -> JsonObject(emptyMap())
        is AccountOnboardingProps -> ConnectJson.encodeToJsonElement(toJs()).jsonObject
        else -> throw IllegalArgumentException("Unsupported props type: $this")
    }
}

/**
 * Empty props.
 */
@Parcelize
internal data object EmptyProps : Parcelable
