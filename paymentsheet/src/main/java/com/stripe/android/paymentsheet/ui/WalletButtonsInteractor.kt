package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.asGooglePayButtonType
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal interface WalletButtonsInteractor {
    val state: StateFlow<State>

    class State(
        val walletButtons: List<WalletButton>,
        val buttonsEnabled: Boolean,
    )

    fun handleViewAction(action: ViewAction)

    sealed interface WalletButton {
        fun createSelection(): PaymentSelection

        @Immutable
        @Stable
        data class Link(
            val email: String?,
        ) : WalletButton {
            override fun createSelection(): PaymentSelection {
                return PaymentSelection.Link(useLinkExpress = false)
            }
        }

        @Immutable
        @Stable
        data class GooglePay private constructor(
            val googlePayButtonType: GooglePayButtonType,
            val billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters,
            val allowCreditCards: Boolean,
            val cardBrandFilter: CardBrandFilter,
        ) : WalletButton {
            constructor(
                buttonType: PaymentSheet.GooglePayConfiguration.ButtonType?,
                billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
                allowCreditCards: Boolean,
                cardBrandFilter: CardBrandFilter,
            ) : this(
                googlePayButtonType = buttonType.asGooglePayButtonType,
                billingAddressParameters = billingDetailsCollectionConfiguration.toBillingAddressParameters(),
                allowCreditCards = allowCreditCards,
                cardBrandFilter = cardBrandFilter,
            )

            override fun createSelection(): PaymentSelection {
                return PaymentSelection.GooglePay
            }
        }
    }

    sealed interface ViewAction {
        data class OnButtonPressed(val button: WalletButton) : ViewAction
        data object OnRendered : ViewAction
        data object OnUnRendered : ViewAction
    }
}

internal class DefaultWalletButtonsInteractor(
    private val arguments: StateFlow<Arguments?>,
    private val confirmationHandler: ConfirmationHandler,
    private val coroutineScope: CoroutineScope,
    private val errorReporter: ErrorReporter,
    private val onWalletButtonsRenderStateChanged: (isRendered: Boolean) -> Unit
) : WalletButtonsInteractor {
    override val state: StateFlow<WalletButtonsInteractor.State> = combineAsStateFlow(
        arguments,
        confirmationHandler.state,
    ) { arguments, confirmationState ->
        val walletButtons = arguments?.run {
            arguments.paymentMethodMetadata.availableWallets.map { wallet ->
                when (wallet) {
                    WalletType.GooglePay -> WalletButtonsInteractor.WalletButton.GooglePay(
                        allowCreditCards = true,
                        buttonType = configuration.googlePay?.buttonType,
                        cardBrandFilter = PaymentSheetCardBrandFilter(
                            cardBrandAcceptance = configuration.cardBrandAcceptance,
                        ),
                        billingDetailsCollectionConfiguration = configuration
                            .billingDetailsCollectionConfiguration,
                    )
                    WalletType.Link -> WalletButtonsInteractor.WalletButton.Link(
                        email = linkEmail,
                    )
                }
            }
        } ?: emptyList()

        WalletButtonsInteractor.State(
            walletButtons = walletButtons,
            buttonsEnabled = confirmationState !is ConfirmationHandler.State.Confirming,
        )
    }

    override fun handleViewAction(action: WalletButtonsInteractor.ViewAction) {
        when (action) {
            is WalletButtonsInteractor.ViewAction.OnButtonPressed -> {
                arguments.value?.let { arguments ->
                    confirmationArgs(action.button.createSelection(), arguments)?.let {
                        coroutineScope.launch {
                            confirmationHandler.start(it)
                        }
                    } ?: run {
                        errorReporter.report(
                            ErrorReporter.UnexpectedErrorEvent.WALLET_BUTTONS_NULL_CONFIRMATION_ARGS_ON_CONFIRM
                        )
                    }
                } ?: run {
                    errorReporter.report(
                        ErrorReporter.UnexpectedErrorEvent.WALLET_BUTTONS_NULL_WALLET_ARGUMENTS_ON_CONFIRM
                    )
                }
            }
            is WalletButtonsInteractor.ViewAction.OnRendered -> onWalletButtonsRenderStateChanged(true)
            is WalletButtonsInteractor.ViewAction.OnUnRendered -> onWalletButtonsRenderStateChanged(false)
        }
    }

    private fun confirmationArgs(
        selection: PaymentSelection,
        arguments: Arguments,
    ): ConfirmationHandler.Args? {
        val confirmationOption = selection.toConfirmationOption(
            configuration = arguments.configuration,
            linkConfiguration = arguments.paymentMethodMetadata.linkState?.configuration,
        ) ?: return null

        return ConfirmationHandler.Args(
            intent = arguments.paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
            initializationMode = arguments.initializationMode,
            appearance = arguments.appearance,
            shippingDetails = arguments.configuration.shippingDetails,
        )
    }

    data class Arguments(
        val linkEmail: String?,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val configuration: CommonConfiguration,
        val appearance: PaymentSheet.Appearance,
        val initializationMode: PaymentElementLoader.InitializationMode,
    )

    companion object {
        fun create(
            flowControllerViewModel: FlowControllerViewModel
        ): WalletButtonsInteractor {
            val linkHandler = flowControllerViewModel.flowControllerStateComponent.linkHandler

            return DefaultWalletButtonsInteractor(
                errorReporter = flowControllerViewModel.flowControllerStateComponent.errorReporter,
                arguments = combineAsStateFlow(
                    linkHandler.linkConfigurationCoordinator.emailFlow,
                    flowControllerViewModel.stateFlow,
                    flowControllerViewModel.configureRequest,
                ) { linkEmail, flowControllerState, configureRequest ->
                    if (flowControllerState != null && configureRequest != null) {
                        Arguments(
                            linkEmail = linkEmail,
                            configuration = flowControllerState.paymentSheetState.config,
                            paymentMethodMetadata = flowControllerState.paymentSheetState.paymentMethodMetadata,
                            appearance = configureRequest.configuration.appearance,
                            initializationMode = configureRequest.initializationMode,
                        )
                    } else {
                        null
                    }
                },
                confirmationHandler = flowControllerViewModel.flowControllerStateComponent.confirmationHandler,
                coroutineScope = flowControllerViewModel.viewModelScope,
                onWalletButtonsRenderStateChanged = { isRendered ->
                    flowControllerViewModel.walletButtonsRendered = isRendered
                }
            )
        }
    }
}
