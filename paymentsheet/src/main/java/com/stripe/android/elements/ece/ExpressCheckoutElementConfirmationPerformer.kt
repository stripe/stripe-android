package com.stripe.android.elements.ece

internal interface ExpressCheckoutElementConfirmationPerformer {
    fun confirm(expressButton: ExpressButton)
}
