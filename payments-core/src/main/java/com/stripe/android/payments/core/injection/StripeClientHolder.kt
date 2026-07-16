package com.stripe.android.payments.core.injection

import com.stripe.android.StripeClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StripeClientHolder @Inject constructor() {
    @Volatile
    var stripeClient: StripeClient? = null
}
