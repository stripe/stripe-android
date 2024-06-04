package com.stripe.hcaptcha.task

import androidx.annotation.RestrictTo

/**
 * A hCaptcha open listener class
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface OnOpenListener {
    /**
     * Called when the hCaptcha visual challenge is displayed on the html page
     */
    fun onOpen()
}
