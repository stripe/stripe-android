package com.stripe.hcaptcha.config

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The hCaptcha challenge orientation
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class HCaptchaOrientation {
    @SerialName("portrait")
    PORTRAIT,

    @SerialName("landscape")
    LANDSCAPE;
}
