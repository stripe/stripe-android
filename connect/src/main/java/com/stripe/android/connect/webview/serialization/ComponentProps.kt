package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Indicates a class is contains props for an embedded component.
 */
internal interface ComponentProps

internal inline fun <reified T : ComponentProps> T.toJsonObject(): JsonObject {
    return ConnectJson.encodeToJsonElement(this).jsonObject
}
