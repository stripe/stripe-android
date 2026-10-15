package com.stripe.android.elements

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.PaymentIntent
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import java.util.UUID
import javax.inject.Inject

internal class ElementsStateLoader @Inject constructor(
    private val loader: PaymentElementLoader,
    private val stateHolder: ElementsControllerStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    @PaymentElementCallbackIdentifier private val callbackIdentifier: String,
) {
    suspend fun load(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: ElementsController.Configuration.State,
        initializedViaCompose: Boolean,
    ) {
        stateHolder.state = null
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration)
        initializationMode.validate()
        require(PaymentElementCallbackReferences.getExact(callbackIdentifier)?.createIntentCallback != null) {
            "A createIntentCallback is required for deferred intent configuration."
        }
        val loaded = loader.load(
            initializationMode = initializationMode,
            integrationConfiguration = PaymentElementLoader.Configuration.Elements(
                commonConfiguration = configuration.toCommonConfiguration(),
            ),
            metadata = PaymentElementLoader.Metadata(
                isReloadingAfterProcessDeath = false,
                initializedViaCompose = initializedViaCompose,
            ),
        ).getOrThrow()
        loaded.validationError?.let { throw it }
        confirmationHandler.bootstrap(loaded.paymentMethodMetadata)
        stateHolder.state = ElementsControllerState(
            configurationId = UUID.randomUUID().toString(),
            configuration = configuration,
            paymentElementState = loaded,
            walletState = ElementsWalletState(
                amount = (loaded.paymentMethodMetadata.stripeIntent as? PaymentIntent)?.amount
                    ?: configuration.googlePay?.amount ?: 0L,
                lineItems = emptyList(),
            ),
        )
    }
}

private fun ElementsController.Configuration.State.toCommonConfiguration(): CommonConfiguration {
    return PaymentSheet.Configuration(
        merchantDisplayName = merchantDisplayName,
        googlePay = googlePay,
        defaultBillingDetails = defaultBillingDetails,
    ).asCommonConfiguration()
}
