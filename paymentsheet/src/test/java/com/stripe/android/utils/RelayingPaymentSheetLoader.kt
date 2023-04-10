package com.stripe.android.utils

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.channels.Channel

internal class RelayingPaymentSheetLoader : PaymentSheetLoader {

    private val results = Channel<PaymentSheetLoader.Result>(capacity = 1)

    fun enqueueSuccess(
        stripeIntent: StripeIntent = PaymentIntentFactory.create(),
    ) {
        enqueue(
            PaymentSheetLoader.Result.Success(
                state = PaymentSheetState.Full(
                    stripeIntent = stripeIntent,
                    customerPaymentMethods = emptyList(),
                    config = null,
                    isGooglePayReady = false,
                    paymentSelection = null,
                    linkState = null,
                ),
            )
        )
    }

    fun enqueueFailure() {
        val error = RuntimeException("whoops")
        enqueue(PaymentSheetLoader.Result.Failure(error))
    }

    private fun enqueue(result: PaymentSheetLoader.Result) {
        results.trySend(result)
    }

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration?
    ): PaymentSheetLoader.Result {
        return results.receive()
    }
}
