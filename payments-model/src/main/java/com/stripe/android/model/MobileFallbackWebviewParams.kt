package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class MobileFallbackWebviewParams(
    @SerialName("webview_requirement_type")
    val webViewRequirementType: WebviewRequirementType,
    @SerialName("webview_open_url")
    val webviewOpenUrl: String? = null,
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    enum class WebviewRequirementType(val value: String) {
        Unknown(""),
        Required("required"),
        NotRequired("notrequired");

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            fun fromValue(value: String): WebviewRequirementType =
                entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
        }
    }
}
