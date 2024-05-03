package com.stripe.hcaptcha.task

import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.HCaptchaException

/**
 * A failure listener class
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface OnFailureListener {
    /**
     * Called whenever there is a hCaptcha error or user closed the challenge dialog
     *
     * @param exception the exception
     */
    fun onFailure(exception: HCaptchaException)
}
