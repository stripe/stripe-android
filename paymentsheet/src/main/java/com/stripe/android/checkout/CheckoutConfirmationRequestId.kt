package com.stripe.android.checkout

import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle

@get:MainThread
@set:MainThread
internal var SavedStateHandle.checkoutConfirmationRequestId: String?
    get() = get(CHECKOUT_CONFIRMATION_REQUEST_ID_KEY)
    set(value) {
        this[CHECKOUT_CONFIRMATION_REQUEST_ID_KEY] = value
    }

private const val CHECKOUT_CONFIRMATION_REQUEST_ID_KEY = "Checkout.confirmationRequestId"
