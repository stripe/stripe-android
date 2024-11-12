package com.stripe.android.paymentelement.embedded

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

@ExperimentalEmbeddedPaymentElementApi
internal interface EmbeddedConfigurationHandler {
    suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Result<PaymentElementLoader.State>
}

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfigurationHandler @Inject constructor(
    private val paymentElementLoader: PaymentElementLoader,
) : EmbeddedConfigurationHandler {
    override suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Result<PaymentElementLoader.State> {
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration)
        try {
            initializationMode.validate()
            configuration.asCommonConfiguration().validate()
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        }

        return paymentElementLoader.load(
            initializationMode = initializationMode,
            configuration = configuration.asCommonConfiguration(),
            isReloadingAfterProcessDeath = false,
            initializedViaCompose = true,
        )
    }
}
