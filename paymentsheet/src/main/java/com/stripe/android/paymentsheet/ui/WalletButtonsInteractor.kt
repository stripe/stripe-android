package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.link.LinkExpressMode
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
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.WalletButtonsViewClickHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.ButtonThemes.LinkButtonTheme
import com.stripe.android.paymentsheet.configType
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
import javax.inject.Provider

@OptIn(WalletButtonsPreview::class)
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
        val walletType: WalletType
        fun createSelection(): PaymentSelection

        @Immutable
        @Stable
        data class Link(
            val state: LinkButtonState,
            val theme: LinkButtonTheme = LinkButtonTheme.DEFAULT,
        ) : WalletButton {
            override val walletType = WalletType.Link

            override fun createSelection(): PaymentSelection {
                return PaymentSelection.Link(linkExpressMode = LinkExpressMode.DISABLED)
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
            override val walletType = WalletType.GooglePay

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
            override val walletType = WalletType.ShopPay

            override fun createSelection(): PaymentSelection {
                return PaymentSelection.ShopPay
            }
        }
    }

    sealed interface ViewAction {
        data class OnButtonPressed constructor(
            val button: WalletButton,
            val clickHandler: WalletButtonsViewClickHandler,
        ) : ViewAction
        data object OnShown : ViewAction
        data object OnHidden : ViewAction
        data object OnResendCode : ViewAction
        data object OnResendCodeNotificationSent : ViewAction
    }
}

@OptIn(ExperimentalAnalyticEventCallbackApi::class, WalletButtonsPreview::class)
internal class DefaultWalletButtonsInteractor constructor(
    private val arguments: StateFlow<Arguments?>,
    private val confirmationHandler: ConfirmationHandler,
    private val coroutineScope: CoroutineScope,
    private val errorReporter: ErrorReporter,
    private val linkInlineInteractor: LinkInlineInteractor,
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkAccountHolder: LinkAccountHolder,
    private val analyticsCallbackProvider: Provider<AnalyticEventCallback?>,
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
        val walletsAllowedByMerchant = arguments?.let(::visibleWallets) ?: emptyList()
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
                        val linkConfiguration = arguments.paymentMethodMetadata.linkState?.configuration
                        WalletButton.Link(
                            state = LinkButtonState.create(
                                enableDefaultValues = linkConfiguration?.enableDisplayableDefaultValuesInEce == true,
                                linkEmail = arguments.linkEmail,
                                paymentDetails = linkAccountInfo.account?.displayablePaymentDetails
                            ),
                            theme = arguments.configuration.walletButtons?.buttonThemes?.link ?: LinkButtonTheme.DEFAULT
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

    @OptIn(WalletButtonsPreview::class)
    override fun handleViewAction(action: WalletButtonsInteractor.ViewAction) {
        when (action) {
            is OnButtonPressed -> {
                analyticsCallbackProvider.get()?.onEvent(
                    AnalyticEvent.TapsButtonInWalletsButtonsView(action.button.walletType.code)
                )

                val isHandled = action.clickHandler.onWalletButtonClick(
                    wallet = action.button.walletType.code
                )

                if (isHandled) {
                    return
                }

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
            val selectedPayment = (arguments.paymentSelection as? PaymentSelection.Link)
                ?.selectedPayment
            // Launch Link payment selection instead of starting confirmation
            linkPaymentLauncher.present(
                configuration = linkConfiguration,
                paymentMethodMetadata = arguments.paymentMethodMetadata,
                linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment?.details),
                linkExpressMode = LinkExpressMode.ENABLED,
            )
        } else {
            handleButtonPressed(
                WalletButton.Link(
                    state = LinkButtonState.Default,
                    theme = arguments.configuration.walletButtons?.buttonThemes?.link
                        ?: LinkButtonTheme.DEFAULT
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

    private fun visibleWallets(arguments: Arguments): List<WalletType> {
        val walletVisibility = arguments.configuration.walletButtons?.visibility?.walletButtonsView ?: emptyMap()

        return WalletType.entries.filter { walletType ->
            val configuredVisibility = walletVisibility[walletType.configType]

            configuredVisibility == null || configuredVisibility ==
                PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always
        }
    }

    private fun confirmationArgs(
        selection: PaymentSelection,
        arguments: Arguments,
    ): ConfirmationHandler.Args? {
        val confirmationOption = selection.toConfirmationOption(
            configuration = arguments.configuration,
            linkConfiguration = arguments.paymentMethodMetadata.linkState?.configuration,
            passiveCaptchaParams = arguments.paymentMethodMetadata.passiveCaptchaParams,
            clientAttributionMetadata = arguments.paymentMethodMetadata.clientAttributionMetadata,
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            initializationMode = arguments.initializationMode,
            appearance = arguments.appearance,
            paymentMethodMetadata = arguments.paymentMethodMetadata,
        )
    }

    data class Arguments(
        val linkEmail: String?,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val configuration: CommonConfiguration,
        val appearance: PaymentSheet.Appearance,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val paymentSelection: PaymentSelection?,
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
                            paymentSelection = flowControllerViewModel.paymentSelection
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
                analyticsCallbackProvider =
                flowControllerViewModel.flowControllerStateComponent.analyticEventCallbackProvider,
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
            analyticsCallbackProvider: Provider<AnalyticEventCallback?>,
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
                            paymentSelection = state.selection
                        )
                    }
                },
                confirmationHandler = confirmationHandler,
                coroutineScope = coroutineScope,
                linkInlineInteractor = linkInlineInteractor,
                linkPaymentLauncher = linkPaymentLauncher,
                linkAccountHolder = linkAccountHolder,
                analyticsCallbackProvider = analyticsCallbackProvider,
                onWalletButtonsRenderStateChanged = {
                    // No-op, not supported for Embedded
                }
            )
        }
    }
}
