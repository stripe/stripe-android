package com.stripe.hcaptcha.task

/**
 * A hCaptcha loader listener class
 */
interface OnLoadedListener {
    /**
     * Called when the hCaptcha challenge is loaded to the html page
     */
    fun onLoaded()
}
