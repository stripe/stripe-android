package com.stripe.hcaptcha.task

/**
 * A hCaptcha open listener class
 */
interface OnOpenListener {
    /**
     * Called when the hCaptcha visual challenge is displayed on the html page
     */
    fun onOpen()
}
