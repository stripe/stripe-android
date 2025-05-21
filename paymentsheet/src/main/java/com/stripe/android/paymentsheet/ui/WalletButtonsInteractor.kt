package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.stripe.android.CardBrandFilter
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
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

    sealed interface WalletButton {
        val onPressed: () -> Unit
    }
}

internal class DefaultWalletButtonsInteractor(
    arguments: StateFlow<Arguments?>,
    private val confirmationHandler: ConfirmationHandler,
    private val coroutineScope: CoroutineScope,
) : WalletButtonsInteractor {
    override val state: StateFlow<WalletButtonsInteractor.State> = combineAsStateFlow(
        arguments,
        confirmationHandler.state,
    ) { arguments, confirmationState ->
        val walletButtons = mutableListOf<WalletButtonsInteractor.WalletButton>()

        arguments?.run {
            if (isLinkEnabled) {
                walletButtons.add(
                    LinkWalletButton(
                        email = linkEmail,
                        onPressed = createOnPressed(PaymentSelection.Link(useLinkExpress = false)),
                    )
                )
            }

            if (arguments.paymentMethodMetadata.isGooglePayReady) {
                walletButtons.add(
                    GooglePayWalletButton(
                        allowCreditCards = true,
                        buttonType = configuration.googlePay?.buttonType,
                        cardBrandFilter = PaymentSheetCardBrandFilter(configuration.cardBrandAcceptance),
                        billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
                        onPressed = createOnPressed(PaymentSelection.GooglePay),
                    )
                )
            }
        }

        WalletButtonsInteractor.State(
            walletButtons = walletButtons,
            buttonsEnabled = confirmationState !is ConfirmationHandler.State.Confirming,
        )
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

    private fun Arguments.createOnPressed(
        selection: PaymentSelection
    ): () -> Unit = {
        confirmationArgs(selection, this)?.let {
            coroutineScope.launch {
                confirmationHandler.start(it)
            }
        }
    }

    data class Arguments(
        val isLinkEnabled: Boolean,
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
                arguments = combineAsStateFlow(
                    linkHandler.isLinkEnabled,
                    linkHandler.linkConfigurationCoordinator.emailFlow,
                    flowControllerViewModel.stateFlow,
                    flowControllerViewModel.configureRequest,
                ) { isLinkEnabled, linkEmail, flowControllerState, configureRequest ->
                    if (flowControllerState != null && configureRequest != null) {
                        Arguments(
                            isLinkEnabled = isLinkEnabled == true,
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
            )
        }
    }
}

@Immutable
internal class GooglePayWalletButton(
    buttonType: PaymentSheet.GooglePayConfiguration.ButtonType?,
    billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val allowCreditCards: Boolean,
    val cardBrandFilter: CardBrandFilter,
    override val onPressed: () -> Unit,
) : WalletButtonsInteractor.WalletButton {
    val googlePayButtonType = buttonType.asGooglePayButtonType

    val billingAddressParameters by lazy {
        billingDetailsCollectionConfiguration.toBillingAddressParameters()
    }
}

@Immutable
internal class LinkWalletButton(
    val email: String?,
    override val onPressed: () -> Unit,
) : WalletButtonsInteractor.WalletButton
