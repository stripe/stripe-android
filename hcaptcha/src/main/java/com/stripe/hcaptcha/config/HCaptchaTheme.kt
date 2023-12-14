package com.stripe.hcaptcha.config

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The hCaptcha checkbox theme
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class HCaptchaTheme {
    /**
     * Dark theme
     */
    @SerialName("dark")
    DARK,

    /**
     * Light theme
     */
    @SerialName("light")
    LIGHT,

    /**
     * Contrast theme
     */
    @SerialName("contrast")
    CONTRAST;
}
