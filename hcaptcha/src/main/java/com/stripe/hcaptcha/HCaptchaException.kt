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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private const val serialVersionUID: Long = -6219797459363514791L
    }
}
