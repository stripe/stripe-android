package com.stripe.android.elements.ece

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.elements.ElementsControllerState
import com.stripe.android.elements.ElementsControllerStateHolder
import com.stripe.android.elements.ElementsOperationCoordinator
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Projects the shared intent session into Express Checkout's rendering and wallet actions. */
internal class ElementsExpressCheckoutElementIntegration @Inject constructor(
    private val stateHolder: ElementsControllerStateHolder,
    private val operationCoordinator: ElementsOperationCoordinator,
    private val savedStateHandle: SavedStateHandle,
    buttonTypesFactory: DefaultAvailableExpressButtonTypesFactory,
    private val eventReporter: EventReporter,
    private val expressCheckoutElementEventReporter: ExpressCheckoutElementEventReporter,
    @ViewModelScope coroutineScope: CoroutineScope,
) : ExpressCheckoutElementIntegration {
    private var contentDisplayed = false

    override val state = combineAsStateFlow(
        stateHolder.stateFlow,
        operationCoordinator.isUpdating,
    ) { loaded, updating ->
        val configuration = loaded?.expressCheckoutElementConfiguration()
        ExpressCheckoutElementIntegration.State(
            content = if (loaded != null && configuration != null) {
                ExpressCheckoutElementIntegration.Content(
                    configuration = configuration,
                    paymentMethodMetadata = loaded.paymentMethodMetadata,
                    availableExpressButtonTypes = buttonTypesFactory.create(
                        paymentMethodMetadata = loaded.paymentMethodMetadata,
                        expressCheckoutElementConfiguration = configuration,
                        requiresShippingAddress = loaded.configuration.shippingAddressRequired,
                    ),
                    requiresShippingAddress = loaded.configuration.shippingAddressRequired,
                )
            } else {
                null
            },
            enabled = loaded != null && !updating,
        )
    }

    init {
        coroutineScope.launch {
            stateHolder.stateFlow.collect {
                if (contentDisplayed) reportDisplayed()
            }
        }
    }

    override fun onDisplayed() {
        contentDisplayed = true
        reportDisplayed()
    }

    override fun confirm(expressButton: ExpressButton) {
        val loaded = stateHolder.state ?: return
        if (operationCoordinator.isUpdating.value) return
        val configuration = loaded.expressCheckoutElementConfiguration() ?: return
        val selection = expressButton.toSelection()
        val option = expressButton.toConfirmationOption(
            configuration = loaded.confirmationConfiguration(configuration),
            paymentMethodMetadata = loaded.paymentMethodMetadata,
            googlePayShippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
                isRequired = loaded.configuration.shippingAddressRequired,
                allowedCountryCodes = loaded.configuration.allowedShippingCountryCodes,
                phoneNumberRequired = false,
            ),
        ) ?: return
        eventReporter.onWalletButtonTapped(expressButton.toWalletType().code)
        operationCoordinator.confirm(selection, option)
    }

    private fun reportDisplayed() {
        val loaded = stateHolder.state ?: return
        if (loaded.configuration.expressCheckoutElementConfiguration == null) return
        if (savedStateHandle.get<String>(DISPLAYED_CONFIGURATION_KEY) == loaded.configurationId) return
        savedStateHandle[DISPLAYED_CONFIGURATION_KEY] = loaded.configurationId
        expressCheckoutElementEventReporter.onEceDisplayed(state.value.content)
    }

    private companion object {
        const val DISPLAYED_CONFIGURATION_KEY = "elements.ece.displayed_configuration"
    }
}

private fun ElementsControllerState.expressCheckoutElementConfiguration(): ExpressCheckoutElement.Configuration.State? {
    val elementConfiguration = configuration.expressCheckoutElementConfiguration ?: return null
    return elementConfiguration.copy(
        googlePayConfiguration = elementConfiguration.googlePayConfiguration.copy(
            additionalEnabledNetworks = (
                elementConfiguration.googlePayConfiguration.additionalEnabledNetworks +
                    commonConfiguration.googlePay?.additionalEnabledNetworks.orEmpty()
                ).distinct(),
        ),
    )
}

private fun ElementsControllerState.confirmationConfiguration(
    elementConfiguration: ExpressCheckoutElement.Configuration.State,
): CommonConfiguration {
    val googlePayConfiguration = elementConfiguration.googlePayConfiguration
    return commonConfiguration.copy(
        googlePay = commonConfiguration.googlePay?.let { googlePay ->
            PaymentSheet.GooglePayConfiguration(
                environment = googlePay.environment,
                countryCode = googlePay.countryCode,
                currencyCode = googlePay.currencyCode,
                amount = googlePay.amount,
                label = googlePayConfiguration.label ?: googlePay.label,
                buttonType = googlePay.buttonType,
                additionalEnabledNetworks = googlePayConfiguration.additionalEnabledNetworks,
            )
        },
    )
}
