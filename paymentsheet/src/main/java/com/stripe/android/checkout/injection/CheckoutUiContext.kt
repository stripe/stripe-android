package com.stripe.android.checkout.injection

import javax.inject.Qualifier

/** Qualifies the context used for Checkout's observable state updates. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class CheckoutUiContext
