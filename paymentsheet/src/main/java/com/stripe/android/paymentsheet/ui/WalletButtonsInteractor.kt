package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.verification.LinkEmbeddedManager
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.WalletButton
import com.stripe.android.paymentsheet.utils.asGooglePayButtonType
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
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
            val preservedPaymentMethod: LinkPaymentMethod? = null,
        ) : WalletButton {
            override fun createSelection(): PaymentSelection {
                return PaymentSelection.Link(
                    selectedPayment = preservedPaymentMethod,
                    useLinkExpress = false
                )
            }
        }

        /**
         * Button type for Link 2FA verification
         */
        @Immutable
        @Stable
        data class Link2FA(
            val verificationViewState: com.stripe.android.link.ui.verification.VerificationViewState,
            val otpElement: OTPElement,
            val baseEmail: String?,
        ) : WalletButton {
            override fun createSelection(): PaymentSelection {
                // 2FA button doesn't directly create a selection
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
    }
}

internal class DefaultWalletButtonsInteractor(
    private val arguments: StateFlow<Arguments?>,
    private val confirmationHandler: ConfirmationHandler,
    private val coroutineScope: CoroutineScope,
    private val errorReporter: ErrorReporter,
    private val linkEmbeddedManager: LinkEmbeddedManager,
) : WalletButtonsInteractor {

    init {
        coroutineScope.launch {
            // Perform Link verification setup at initialization time
            arguments.filterNotNull().collect { setupLink(it) }
        }
    }

    override val state: StateFlow<WalletButtonsInteractor.State> = combineAsStateFlow(
        arguments,
        confirmationHandler.state,
        linkEmbeddedManager.state,
    ) { arguments, confirmationState, linkEmbeddedState ->
        val walletButtons = arguments?.run {
            arguments.paymentMethodMetadata.availableWallets.map { wallet ->
                when (wallet) {
                    WalletType.GooglePay -> WalletButton.GooglePay(
                        allowCreditCards = true,
                        buttonType = configuration.googlePay?.buttonType,
                        cardBrandFilter = PaymentSheetCardBrandFilter(
                            cardBrandAcceptance = configuration.cardBrandAcceptance,
                        ),
                        billingDetailsCollectionConfiguration = configuration
                            .billingDetailsCollectionConfiguration,
                    )
                    WalletType.Link -> {
                        val verificationState = linkEmbeddedState.verificationState
                        when {
                            // Case 1: Show verification if needed
                            verificationState != null -> WalletButton.Link2FA(
                                verificationViewState = verificationState,
                                otpElement = linkEmbeddedManager.otpElement,
                                baseEmail = linkEmail
                            )

                            // Case 2: Show Link button
                            else -> WalletButton.Link(
                                email = linkEmail,
                                preservedPaymentMethod = linkEmbeddedState.preservedPaymentMethod
                            )
                        }
                    }
                }
            }.sortedWith { a, b ->
                // Make Link2FA appear first
                if (a is WalletButton.Link2FA) -1
                else if (b is WalletButton.Link2FA) 1
                else 0 // Keep original order for other wallet types
            }
        } ?: emptyList()

        WalletButtonsInteractor.State(
            walletButtons = walletButtons,
            buttonsEnabled = confirmationState !is ConfirmationHandler.State.Confirming
        )
    }

    private fun setupLink(args: Arguments) {
        if (args.paymentMethodMetadata.availableWallets.contains(WalletType.Link)) {
            linkEmbeddedManager.setup(
                paymentMethodMetadata = args.paymentMethodMetadata,
                onVerificationSucceeded = { defaultPaymentMethod ->
                    // When verification is successful, create a Link selection with the default payment method
                    val selection = linkEmbeddedManager.createLinkSelection()
                    confirmationArgs(selection, args)?.let {
                        coroutineScope.launch { confirmationHandler.start(it) }
                    }
                }
            )
        }
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
            viewModel: FlowControllerViewModel
        ): WalletButtonsInteractor {
            val coroutineScope = viewModel.viewModelScope
            val component = viewModel.flowControllerStateComponent
            val linkEmbeddedManager = component.linkEmbeddedManagerFactory.create(coroutineScope)
            return DefaultWalletButtonsInteractor(
                errorReporter = component.errorReporter,
                arguments = combineAsStateFlow(
                    component.linkHandler.linkConfigurationCoordinator.emailFlow,
                    viewModel.stateFlow,
                    viewModel.configureRequest,
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
                confirmationHandler = component.confirmationHandler,
                coroutineScope = coroutineScope,
                linkEmbeddedManager = linkEmbeddedManager,
            )
        }

        @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
        fun create(
            linkEmbeddedManager: LinkEmbeddedManager,
            embeddedLinkHelper: EmbeddedLinkHelper,
            confirmationStateHolder: EmbeddedConfirmationStateHolder,
            confirmationHandler: ConfirmationHandler,
            coroutineScope: CoroutineScope,
            errorReporter: ErrorReporter,
        ): WalletButtonsInteractor {
            return DefaultWalletButtonsInteractor(
                errorReporter = errorReporter,
                arguments = combineAsStateFlow(
                    embeddedLinkHelper.linkEmail,
                    confirmationStateHolder.stateFlow,
                ) { linkEmail, confirmationState ->
                    confirmationState?.let { state ->
                        Arguments(
                            linkEmail = linkEmail,
                            configuration = state.configuration.asCommonConfiguration(),
                            paymentMethodMetadata = state.paymentMethodMetadata,
                            appearance = state.configuration.appearance,
                            initializationMode = state.initializationMode,
                        )
                    }
                },
                confirmationHandler = confirmationHandler,
                coroutineScope = coroutineScope,
                linkEmbeddedManager = linkEmbeddedManager
            )
        }
    }
}
