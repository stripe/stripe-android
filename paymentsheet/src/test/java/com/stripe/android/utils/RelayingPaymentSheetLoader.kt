package com.stripe.android.utils

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.channels.Channel

internal class RelayingPaymentSheetLoader : PaymentSheetLoader {

    private val results = Channel<Result<PaymentSheetState.Full>>(capacity = 1)

    fun enqueueSuccess(
        stripeIntent: StripeIntent = PaymentIntentFactory.create(),
        validationError: PaymentSheetLoadingException? = null,
    ) {
        enqueue(
            Result.success(
                PaymentSheetState.Full(
                    customerPaymentMethods = emptyList(),
                    config = PaymentSheet.Configuration("Example"),
                    isGooglePayReady = false,
                    paymentSelection = null,
                    linkState = null,
                    isEligibleForCardBrandChoice = false,
                    validationError = validationError,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(stripeIntent = stripeIntent),
                ),
            )
        )
    }

    fun enqueueFailure() {
        val error = RuntimeException("whoops")
        enqueue(Result.failure(error))
    }

    private fun enqueue(result: Result<PaymentSheetState.Full>) {
        results.trySend(result)
    }

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration,
        isReloadingAfterProcessDeath: Boolean,
    ): Result<PaymentSheetState.Full> {
        return results.receive()
    }
}
