package com.stripe.hcaptcha.task

import com.stripe.hcaptcha.HCaptchaException

/**
 * A failure listener class
 */
interface OnFailureListener {
    /**
     * Called whenever there is a hCaptcha error or user closed the challenge dialog
     *
     * @param exception the exception
     */
    fun onFailure(exception: HCaptchaException)
}
