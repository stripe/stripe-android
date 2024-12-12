package com.stripe.android.connect

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.serialization.ConnectJson
import com.stripe.android.connect.webview.serialization.toJs
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Indicates a class is contains props for an embedded component.
 */
@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
sealed interface ComponentProps : Parcelable

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Parcelize
data object EmptyProps : ComponentProps

@OptIn(PrivateBetaConnectSDK::class)
internal fun ComponentProps.toJson(): JsonObject {
    return when (this) {
        EmptyProps -> JsonObject(emptyMap())
        is AccountOnboardingProps -> toJs().toJsonObject()
    }
}

private inline fun <reified T> T.toJsonObject(): JsonObject {
    return ConnectJson.encodeToJsonElement(this).jsonObject
}
