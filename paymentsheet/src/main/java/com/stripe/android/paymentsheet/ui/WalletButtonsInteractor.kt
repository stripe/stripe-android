package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.link.verification.LinkInlineInteractor
import com.stripe.android.link.verification.VerificationState
import com.stripe.android.link.verification.VerificationState.Render2FA
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.allowedWalletTypes
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.ViewAction.OnButtonPressed
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.ViewAction.OnHidden
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.ViewAction.OnResendCode
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.ViewAction.OnResendCodeNotificationSent
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.ViewAction.OnShown
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
        val link2FAState: LinkOtpState?,
        val walletButtons: List<WalletButton>,
        val buttonsEnabled: Boolean,
    ) {
        data class LinkOtpState(
            val viewState: VerificationViewState,
            val otpElement: OTPElement
        )

        val hasContent: Boolean
            get() = walletButtons.isNotEmpty() || link2FAState != null
    }

    fun handleViewAction(action: ViewAction)

    sealed interface WalletButton {
        fun createSelection(): PaymentSelection

        @Immutable
        @Stable
        data class Link(
            val state: LinkButtonState,
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

        data object ShopPay : WalletButton {
            override fun createSelection(): PaymentSelection {
                return PaymentSelection.ShopPay
            }
        }
    }

    sealed interface ViewAction {
        data class OnButtonPressed(val button: WalletButton) : ViewAction
        data object OnShown : ViewAction
        data object OnHidden : ViewAction
        data object OnResendCode : ViewAction
        data object OnResendCodeNotificationSent : ViewAction
    }
}

internal class DefaultWalletButtonsInteractor(
    private val arguments: StateFlow<Arguments?>,
    private val confirmationHandler: ConfirmationHandler,
    private val coroutineScope: CoroutineScope,
    private val errorReporter: ErrorReporter,
    private val linkInlineInteractor: LinkInlineInteractor,
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkAccountHolder: LinkAccountHolder,
    private val onWalletButtonsRenderStateChanged: (isRendered: Boolean) -> Unit
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
        linkInlineInteractor.state,
        linkAccountHolder.linkAccountInfo
    ) { arguments, confirmationState, linkEmbeddedState, linkAccountInfo ->
        val walletButtons = arguments?.run {
            arguments.paymentMethodMetadata.availableWallets.mapNotNull { wallet ->
                when (wallet) {
                    WalletType.GooglePay -> WalletButton.GooglePay(
                        allowCreditCards = true,
                        buttonType = configuration.googlePay?.buttonType,
                        cardBrandFilter = PaymentSheetCardBrandFilter(
                            cardBrandAcceptance = configuration.cardBrandAcceptance,
                        ),
                        billingDetailsCollectionConfiguration = configuration
                            .billingDetailsCollectionConfiguration,
                    ).takeIf {
                        walletsAllowedByMerchant.contains(WalletType.GooglePay)
                    }
                    WalletType.Link -> {
                        WalletButton.Link(
                            state = LinkButtonState.create(
                                linkEmail = arguments.linkEmail,
                                paymentDetails = linkAccountInfo.account?.displayablePaymentDetails
                            )
                        ).takeIf {
                            // Only show Link button if the Link verification state is resolved.
                            linkEmbeddedState.verificationState is VerificationState.RenderButton &&
                                walletsAllowedByMerchant.contains(WalletType.Link)
                        }
                    }
                    WalletType.ShopPay -> {
                        WalletButton.ShopPay.takeIf {
                            walletsAllowedByMerchant.contains(WalletType.ShopPay)
                        }
                    }
                }
            }
        } ?: emptyList()

        val linkOTPState = arguments?.run {
            (linkEmbeddedState.verificationState as? Render2FA)?.viewState?.let {
                WalletButtonsInteractor.State.LinkOtpState(
                    viewState = it,
                    otpElement = linkInlineInteractor.otpElement
                )
            }?.takeIf {
                walletsAllowedByMerchant.contains(WalletType.Link)
            }
        }

        WalletButtonsInteractor.State(
            link2FAState = linkOTPState,
            walletButtons = walletButtons,
            buttonsEnabled = confirmationState !is ConfirmationHandler.State.Confirming,
        )
    }

    private fun setupLink(args: Arguments) {
        linkInlineInteractor.setup(
            paymentMethodMetadata = args.paymentMethodMetadata
        )
    }

    override fun handleViewAction(action: WalletButtonsInteractor.ViewAction) {
        when (action) {
            is OnButtonPressed -> {
                arguments.value?.let { arguments ->
                    when (action.button) {
                        is WalletButton.Link -> handleLinkButtonPressed(arguments)
                        else -> handleButtonPressed(action.button, arguments)
                    }
                } ?: run {
                    errorReporter.report(
                        ErrorReporter.UnexpectedErrorEvent.WALLET_BUTTONS_NULL_WALLET_ARGUMENTS_ON_CONFIRM
                    )
                }
            }
            is OnShown -> onWalletButtonsRenderStateChanged(true)
            is OnHidden -> onWalletButtonsRenderStateChanged(false)
            is OnResendCode -> linkInlineInteractor.resendCode()
            is OnResendCodeNotificationSent -> linkInlineInteractor.didShowCodeSentNotification()
        }
    }

    private fun handleLinkButtonPressed(arguments: Arguments) {
        val linkConfiguration = arguments.paymentMethodMetadata.linkState?.configuration
        if (linkConfiguration != null) {
            // Launch Link payment selection instead of starting confirmation
            linkPaymentLauncher.present(
                configuration = linkConfiguration,
                linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                launchMode = LinkLaunchMode.PaymentMethodSelection(null),
                useLinkExpress = true
            )
        } else {
            handleButtonPressed(
                WalletButton.Link(
                    state = LinkButtonState.Default
                ),
                arguments
            )
        }
    }

    private fun handleButtonPressed(button: WalletButton, arguments: Arguments) {
        confirmationArgs(button.createSelection(), arguments)?.let {
            coroutineScope.launch {
                confirmationHandler.start(it)
            }
        } ?: run {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.WALLET_BUTTONS_NULL_CONFIRMATION_ARGS_ON_CONFIRM
            )
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
        val walletsAllowedByMerchant: List<WalletType>,
    )

    companion object {
        fun create(
            flowControllerViewModel: FlowControllerViewModel,
            walletsButtonLinkLauncher: LinkPaymentLauncher
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
                            walletsAllowedByMerchant = configureRequest
                                .configuration
                                .walletButtons
                                .allowedWalletTypes
                        )
                    } else {
                        null
                    }
                },
                confirmationHandler = flowControllerViewModel.flowControllerStateComponent.confirmationHandler,
                coroutineScope = flowControllerViewModel.viewModelScope,
                linkInlineInteractor = flowControllerViewModel.flowControllerStateComponent.linkInlineInteractor,
                linkPaymentLauncher = walletsButtonLinkLauncher,
                linkAccountHolder = flowControllerViewModel.flowControllerStateComponent.linkAccountHolder,
                onWalletButtonsRenderStateChanged = { isRendered ->
                    flowControllerViewModel.walletButtonsRendered = isRendered
                }
            )
        }

        fun create(
            linkInlineInteractor: LinkInlineInteractor,
            embeddedLinkHelper: EmbeddedLinkHelper,
            confirmationStateHolder: EmbeddedConfirmationStateHolder,
            confirmationHandler: ConfirmationHandler,
            coroutineScope: CoroutineScope,
            errorReporter: ErrorReporter,
            linkPaymentLauncher: LinkPaymentLauncher,
            linkAccountHolder: LinkAccountHolder,
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
                            walletsAllowedByMerchant = WalletType.entries
                        )
                    }
                },
                confirmationHandler = confirmationHandler,
                coroutineScope = coroutineScope,
                linkInlineInteractor = linkInlineInteractor,
                linkPaymentLauncher = linkPaymentLauncher,
                linkAccountHolder = linkAccountHolder,
                onWalletButtonsRenderStateChanged = {
                    // No-op, not supported for Embedded
                }
            )
        }
    }
}
