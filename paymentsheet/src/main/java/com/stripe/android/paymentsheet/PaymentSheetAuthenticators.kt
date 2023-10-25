package com.stripe.android.paymentsheet

import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingAuthenticator

// This class is used via reflection in DefaultPaymentAuthenticatorRegistry.
@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentSheetAuthenticators {
    fun get(): Map<Class<out StripeIntent.NextActionData>, PaymentAuthenticator<StripeIntent>> {
        return mapOf(
            StripeIntent.NextActionData.UpiAwaitNotification::class.java to PollingAuthenticator(),
            StripeIntent.NextActionData.BlikAuthorize::class.java to PollingAuthenticator(),
        )
    }
}
