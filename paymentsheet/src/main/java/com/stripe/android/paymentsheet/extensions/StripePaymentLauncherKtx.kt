package com.stripe.android.paymentsheet.extensions

import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingAuthenticator

internal fun StripePaymentLauncher.registerPollingAuthenticator() {
    authenticatorRegistry.registerAuthenticator(
        key = StripeIntent.NextActionData.UpiAwaitNotification::class.java,
        authenticator = PollingAuthenticator(),
    )
    authenticatorRegistry.registerAuthenticator(
        key = StripeIntent.NextActionData.BlikAuthorize::class.java,
        authenticator = PollingAuthenticator(),
    )
}

internal fun StripePaymentLauncher.unregisterPollingAuthenticator() {
    authenticatorRegistry.unregisterAuthenticator(
        key = StripeIntent.NextActionData.UpiAwaitNotification::class.java,
    )
    authenticatorRegistry.unregisterAuthenticator(
        key = StripeIntent.NextActionData.BlikAuthorize::class.java,
    )
}
