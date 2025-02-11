package com.stripe.android.utils

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.channels.Channel

internal class RelayingPaymentElementLoader : PaymentElementLoader {

    private val results = Channel<Result<PaymentElementLoader.State>>(capacity = 1)

    fun enqueueSuccess(
        stripeIntent: StripeIntent = PaymentIntentFactory.create(),
        validationError: PaymentSheetLoadingException? = null,
    ) {
        enqueue(
            Result.success(
                PaymentElementLoader.State(
                    customer = null,
                    config = PaymentSheet.Configuration("Example").asCommonConfiguration(),
                    paymentSelection = null,
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

    private fun enqueue(result: Result<PaymentElementLoader.State>) {
        results.trySend(result)
    }

    override suspend fun load(
        initializationMode: PaymentElementLoader.InitializationMode,
        onfiguration: CommonConfiguration,
        isReloadingAfterProcessDeath: Boolean,
        initializedViaCompose: Boolean,
    ): Result<PaymentElementLoader.State> {
        return results.receive()
    }
}
