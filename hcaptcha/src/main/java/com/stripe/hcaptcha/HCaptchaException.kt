package com.stripe.hcaptcha

import androidx.annotation.RestrictTo


/**
 * A checked exception which contains an [HCaptchaError] id and message.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HCaptchaException @JvmOverloads constructor(
    /**
     * The [HCaptchaError] error object
     */
    val hCaptchaError: HCaptchaError,
    val hCaptchaMessage: String? = null
) : Exception() {

    override val message: String
        get() = hCaptchaMessage ?: hCaptchaError.message

    val statusCode: Int
        get() = hCaptchaError.errorId
}
