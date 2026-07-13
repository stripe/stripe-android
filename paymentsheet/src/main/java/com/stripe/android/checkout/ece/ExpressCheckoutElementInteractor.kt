package com.stripe.android.checkout.ece

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.ExpressCheckoutElement
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayBillingEmailOverrideProvider
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayIsEmailRequiredProvider
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.ButtonThemes.LinkButtonTheme
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.asGooglePayButtonType
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.orEmpty

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementInteractor private constructor(
    val commonConfiguration: CommonConfiguration,
    val configuration: ExpressCheckoutElement.Configuration.State,
    val paymentMethodMetadata: PaymentMethodMetadata,
    val linkAccountHolder: LinkAccountHolder,
    private val errorReporter: ErrorReporter,
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val confirmationHandler: ConfirmationHandler,
    private val coroutineScope: CoroutineScope,
) {

    val state = confirmationHandler.state.mapAsStateFlow { confirmationHandlerState ->
        createInitialState(
            buttonsEnabled = confirmationHandlerState !is ConfirmationHandler.State.Confirming,
        )
    }

    data class State(
        val walletButtons: List<ExpressButton>,
        val buttonsEnabled: Boolean,
    )

    sealed interface ViewAction {
        data class OnButtonPressed(val button: ExpressButton) : ViewAction
    }

    fun handleViewAction(action: ViewAction) {
        when (action) {
            is ViewAction.OnButtonPressed -> {
                when (action.button) {
                    is ExpressButton.Link -> handleLinkButtonPressed()
                    is ExpressButton.GooglePay -> handleButtonPressed(action.button)
                }
            }
        }
    }

    private fun handleLinkButtonPressed() {
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration
        if (linkConfiguration != null) {
            linkPaymentLauncher.present(
                configuration = linkConfiguration,
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                launchMode = LinkLaunchMode.PaymentMethodSelection(null),
                linkExpressMode = LinkExpressMode.ENABLED,
            )
        } else {
            handleButtonPressed(
                ExpressButton.Link(
                    state = LinkButtonState.Default,
                    theme = LinkButtonTheme.DEFAULT,
                    linkBrand = LinkBrand.Link,
                )
            )
        }
    }

    private fun handleButtonPressed(button: ExpressButton) {
        confirmationArgs(button.createSelection())?.let {
            coroutineScope.launch {
                confirmationHandler.start(it)
            }
        } ?: run {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.WALLET_BUTTONS_NULL_CONFIRMATION_ARGS_ON_CONFIRM
            )
        }
    }

    private fun confirmationArgs(selection: PaymentSelection): ConfirmationHandler.Args? {
        val confirmationOption = selection.toConfirmationOption(
            configuration = commonConfiguration,
            linkConfiguration = paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
            googlePayDisplayItems = GooglePayDisplayItemsFactory.create(paymentMethodMetadata),
            googlePayIsEmailRequired = GooglePayIsEmailRequiredProvider.get(
                configuration = commonConfiguration,
                paymentMethodMetadata = paymentMethodMetadata,
            ),
            googlePayBillingEmailOverride = GooglePayBillingEmailOverrideProvider.get(
                configuration = commonConfiguration,
                paymentMethodMetadata = paymentMethodMetadata,
            ),
        ) ?: return null

        return ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = paymentMethodMetadata,
        )
    }

    sealed interface ExpressButton {
        val walletType: WalletType
        fun createSelection(): PaymentSelection

        @Immutable
        @Stable
        data class Link(
            val state: LinkButtonState,
            val linkBrand: LinkBrand,
            val theme: LinkButtonTheme = LinkButtonTheme.DEFAULT,
        ) : ExpressButton {
            override val walletType = WalletType.Link

            override fun createSelection(): PaymentSelection {
                return PaymentSelection.Link(
                    brand = linkBrand,
                    linkExpressMode = LinkExpressMode.DISABLED,
                )
            }
        }

        @Immutable
        @Stable
        data class GooglePay private constructor(
            val googlePayButtonType: GooglePayButtonType,
            val billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters,
            val allowCreditCards: Boolean,
            val cardBrandFilter: CardBrandFilter,
            val cardFundingFilter: CardFundingFilter,
            val additionalEnabledNetworks: List<String>
        ) : ExpressButton {
            override val walletType = WalletType.GooglePay

            constructor(
                buttonType: PaymentSheet.GooglePayConfiguration.ButtonType?,
                billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
                allowCreditCards: Boolean,
                cardBrandFilter: CardBrandFilter,
                cardFundingFilter: CardFundingFilter,
                additionalEnabledNetworks: List<String>
            ) : this(
                googlePayButtonType = buttonType.asGooglePayButtonType,
                billingAddressParameters = billingDetailsCollectionConfiguration.toBillingAddressParameters(),
                allowCreditCards = allowCreditCards,
                cardBrandFilter = cardBrandFilter,
                cardFundingFilter = cardFundingFilter,
                additionalEnabledNetworks = additionalEnabledNetworks
            )

            override fun createSelection(): PaymentSelection {
                return PaymentSelection.GooglePay
            }
        }
    }

    private fun createInitialState(
        buttonsEnabled: Boolean,
    ): State {
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration
        return State(
            walletButtons = paymentMethodMetadata.availableWallets.mapNotNull { walletType ->
                when (walletType) {
                    WalletType.GooglePay -> ExpressButton.GooglePay(
                        allowCreditCards = true,
                        // TODO: ECE should probably have its own button type config.
                        buttonType = commonConfiguration.googlePay?.buttonType,
                        // TODO: what do we need to do to account for the fact that this isn't supported in CS?
                        cardBrandFilter = PaymentSheetCardBrandFilter(
                            cardBrandAcceptance = commonConfiguration.cardBrandAcceptance,
                        ),
                        cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
                        billingDetailsCollectionConfiguration = commonConfiguration
                            .billingDetailsCollectionConfiguration,
                        additionalEnabledNetworks = commonConfiguration.googlePay?.additionalEnabledNetworks.orEmpty()
                    ).takeIf {
                        configuration.visibility[ExpressCheckoutElement.ExpressButton.GooglePay] != ExpressCheckoutElement.ExpressButtonVisibility.Never
                    }
                    WalletType.Link -> ExpressButton.Link(
                        state = LinkButtonState.create(
                            // TODO: what's this?
                            enableDefaultValues = linkConfiguration?.enableDisplayableDefaultValuesInEce == true,
                            linkEmail = "", // TODO
                            paymentDetails = linkAccountHolder.linkAccountInfo.value.account?.displayablePaymentDetails,
                        ),
                        theme = LinkButtonTheme.DEFAULT,
                        linkBrand = LinkBrand.Link, // TODO
                    ).takeIf {
                        configuration.visibility[ExpressCheckoutElement.ExpressButton.Link] != ExpressCheckoutElement.ExpressButtonVisibility.Never
                    }
                }
            },
            buttonsEnabled = buttonsEnabled,
        )
    }



    internal class ExpressCheckoutElementInteractorFactory @Inject constructor(
        private val linkAccountHolder: LinkAccountHolder,
        private val errorReporter: ErrorReporter,
        private val linkPaymentLauncher: LinkPaymentLauncher,
        private val confirmationHandlerFactory: ConfirmationHandler.Factory,
        @ViewModelScope private val coroutineScope: CoroutineScope,
    ) {
        fun create(
            commonConfiguration: CommonConfiguration,
            configuration: ExpressCheckoutElement.Configuration.State,
            paymentMethodMetadata: PaymentMethodMetadata,
        ): ExpressCheckoutElementInteractor {
            return ExpressCheckoutElementInteractor(
                configuration = configuration,
                commonConfiguration = commonConfiguration,
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountHolder = linkAccountHolder,
                errorReporter = errorReporter,
                linkPaymentLauncher = linkPaymentLauncher,
                confirmationHandler = confirmationHandlerFactory.create(coroutineScope),
                coroutineScope = coroutineScope,
            )
        }
    }
}