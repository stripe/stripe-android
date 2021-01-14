package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.StripePaymentController
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import kotlinx.coroutines.Dispatchers

internal class PaymentSheetFlowControllerFactory(
    private val activity: ComponentActivity,
    private val config: PaymentConfiguration,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback
) {
    fun create(): PaymentSheet.FlowController {
        val sessionId = SessionId()

        val stripeRepository = StripeApiRepository(
            activity,
            config.publishableKey
        )

        val paymentController = StripePaymentController(
            activity,
            config.publishableKey,
            stripeRepository,
            true
        )

        return DefaultPaymentSheetFlowController(
            activity = activity,
            flowControllerInitializer = DefaultFlowControllerInitializer(
                stripeRepository,
                PaymentSessionPrefs.Default(activity),
                publishableKey = config.publishableKey,
                stripeAccountId = config.stripeAccountId,
                workContext = Dispatchers.IO
            ),
            paymentController = paymentController,
            eventReporter = DefaultEventReporter(
                mode = EventReporter.Mode.Custom,
                sessionId,
                activity
            ),
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
            sessionId = sessionId,
            initScope = activity.lifecycleScope,
            paymentOptionCallback = paymentOptionCallback,
            paymentResultCallback = paymentResultCallback
        )
    }
}
