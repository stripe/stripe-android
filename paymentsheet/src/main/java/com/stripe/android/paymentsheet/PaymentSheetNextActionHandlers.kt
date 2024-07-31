package com.stripe.android.paymentsheet

import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingNextActionHandler

// This class is used via reflection in DefaultPaymentNextActionHandlerRegistry.
@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentSheetNextActionHandlers {
    fun get(): Map<Class<out StripeIntent.NextActionData>, PaymentNextActionHandler<StripeIntent>> {
        return mapOf(
            StripeIntent.NextActionData.UpiAwaitNotification::class.java to PollingNextActionHandler(),
            StripeIntent.NextActionData.BlikAuthorize::class.java to PollingNextActionHandler(),
        )
    }
}
