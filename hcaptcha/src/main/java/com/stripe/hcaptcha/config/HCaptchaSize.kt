package com.stripe.hcaptcha.config

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The hCaptcha checkbox size
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class HCaptchaSize {

    /**
     * Checkbox is hidden and challenge is automatically displayed.
     */
    @SerialName("invisible")
    INVISIBLE,

    /**
     * Checkbox has a 'normal' size and user must press it to show the challenge.
     */
    @SerialName("normal")
    NORMAL,

    /**
     * Checkbox has a 'compact' size and user must press it to show the challenge.
     */
    @SerialName("compact")
    COMPACT;
}
