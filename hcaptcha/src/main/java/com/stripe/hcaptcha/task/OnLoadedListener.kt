package com.stripe.hcaptcha.task

import androidx.annotation.RestrictTo

/**
 * A hCaptcha loader listener class
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface OnLoadedListener {
    /**
     * Called when the hCaptcha challenge is loaded to the html page
     */
    fun onLoaded()
}
