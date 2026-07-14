package com.stripe.android.paymentelement.embedded.content

import android.os.Bundle
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElement.ConfigureResult
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

internal interface EmbeddedConfigurationCoordinator {
    suspend fun configure(
        configuration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): ConfigureResult
}

@Singleton
internal class DefaultEmbeddedConfigurationCoordinator @Inject constructor(
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val configurationHandler: EmbeddedConfigurationHandler,
    private val stateHelper: EmbeddedStateHelper,
    @ViewModelScope private val viewModelScope: CoroutineScope,
) : EmbeddedConfigurationCoordinator {
    override suspend fun configure(
        configuration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): ConfigureResult {
        return viewModelScope.async {
            confirmationStateHolder.state = null
            configurationHandler.configure(
                configuration = configuration,
                initializationMode = initializationMode,
            ).fold(
                onSuccess = { state ->
                    handleLoadedState(
                        state = state,
                        configuration = configuration,
                    )
                    ConfigureResult.Succeeded()
                },
                onFailure = { error ->
                    ConfigureResult.Failed(error)
                },
            )
        }.await()
    }

    private fun handleLoadedState(
        state: PaymentElementLoader.State,
        configuration: EmbeddedPaymentElement.Configuration,
    ) {
        // Selection resolution (preserving the customer's previous selection when still valid) is
        // applied by [EmbeddedConfigurationHandler] outside its cache, so [state.paymentSelection]
        // is already the resolved selection.
        stateHelper.state = EmbeddedPaymentElement.State(
            confirmationState = EmbeddedConfirmationStateHolder.State(
                paymentMethodMetadata = state.paymentMethodMetadata,
                selection = state.paymentSelection,
                configuration = configuration,
            ),
            customer = state.customer,
            previousNewSelections = Bundle(),
        )
    }
}
