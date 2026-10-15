package com.stripe.android.elements.ece

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal interface ExpressCheckoutElementInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val expressButtons: List<ExpressButton>,
        val buttonLayout: ExpressCheckoutElement.Configuration.Appearance.ButtonLayout.State,
        val enabled: Boolean,
    )

    sealed class ViewAction {
        data object OnDisplayed : ViewAction()

        data class OnWalletTapped(
            val expressButton: ExpressButton,
        ) : ViewAction()
    }
}

internal class DefaultExpressCheckoutElementInteractor @Inject constructor(
    linkAccountHolder: LinkAccountHolder,
    private val integration: ExpressCheckoutElementIntegration,
) : ExpressCheckoutElementInteractor {
    override val state: StateFlow<ExpressCheckoutElementInteractor.State> = combineAsStateFlow(
        linkAccountHolder.linkAccountInfo,
        integration.state,
    ) { linkAccountInfo, integrationState ->
        val content = integrationState.content
        ExpressCheckoutElementInteractor.State(
            expressButtons = content?.let {
                it.availableExpressButtonTypes.map { type ->
                    when (type) {
                        ExpressButtonType.Link -> ExpressButton.Link.create(
                            paymentMethodMetadata = it.paymentMethodMetadata,
                            linkAccountInfo = linkAccountInfo,
                            buttonTheme = it.configuration.appearance.buttonTheme,
                        )
                        is ExpressButtonType.GooglePay -> ExpressButton.GooglePay.create(
                            paymentMethodMetadata = it.paymentMethodMetadata,
                            googlePayConfiguration = type.googlePayConfiguration,
                            shippingAddressRequired = it.requiresShippingAddress,
                            buttonTheme = it.configuration.appearance.buttonTheme,
                        )
                    }
                }
            }.orEmpty(),
            buttonLayout = content?.configuration?.appearance?.buttonLayout
                ?: ExpressCheckoutElement.Configuration.Appearance.ButtonLayout().build(),
            enabled = integrationState.enabled,
        )
    }

    override fun handleViewAction(viewAction: ExpressCheckoutElementInteractor.ViewAction) {
        when (viewAction) {
            ExpressCheckoutElementInteractor.ViewAction.OnDisplayed -> integration.onDisplayed()
            is ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped -> {
                val currentState = state.value
                if (currentState.enabled && viewAction.expressButton in currentState.expressButtons) {
                    integration.confirm(viewAction.expressButton)
                }
            }
        }
    }
}
