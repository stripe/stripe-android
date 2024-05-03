package com.stripe.hcaptcha

import android.os.Handler
import androidx.annotation.RestrictTo

/**
 * Token response which contains the token string to be verified
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HCaptchaTokenResponse(
    val tokenResult: String,
    private val handler: Handler
) {

    /**
     * This method will signal SDK to not fire [HCaptchaError.TOKEN_TIMEOUT]
     */
    fun markUsed() {
        handler.removeCallbacksAndMessages(null)
    }
}
