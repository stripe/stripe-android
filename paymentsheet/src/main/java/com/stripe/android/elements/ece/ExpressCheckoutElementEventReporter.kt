package com.stripe.android.elements.ece

internal interface ExpressCheckoutElementEventReporter {
    fun onEceDisplayed(content: ExpressCheckoutElementIntegration.Content?)
}
