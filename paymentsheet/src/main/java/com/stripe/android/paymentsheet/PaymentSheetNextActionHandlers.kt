package com.stripe.android.paymentsheet

import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingNextActionHandler
import com.stripe.android.paymentsheet.paymentdatacollection.upi.UpiNextActionHandler

// This class is used via reflection in DefaultPaymentNextActionHandlerRegistry.
@Keep
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentSheetNextActionHandlers {
    fun get(): Map<Class<out StripeIntent.NextActionData>, PaymentNextActionHandler<StripeIntent>> {
        return mapOf(
            StripeIntent.NextActionData.UpiRedirect::class.java to UpiNextActionHandler(
                errorReporterFactory = { context, configuration ->
                    ErrorReporter.createFallbackInstance(context, { configuration })
                },
            ),
            StripeIntent.NextActionData.BlikAuthorize::class.java to PollingNextActionHandler(),
            StripeIntent.NextActionData.DisplayPayNowDetails::class.java to PollingNextActionHandler(),
            StripeIntent.NextActionData.DisplayPromptPayDetails::class.java to PollingNextActionHandler(),
            StripeIntent.NextActionData.DisplayPixDetails::class.java to PollingNextActionHandler(),
            StripeIntent.NextActionData.AwaitAuthorization::class.java to PollingNextActionHandler(),
            StripeIntent.NextActionData.MbWayAwaitAuthorization::class.java to PollingNextActionHandler(),
        )
    }
}
