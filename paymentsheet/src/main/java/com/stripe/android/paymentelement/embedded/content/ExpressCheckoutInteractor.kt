package com.stripe.android.paymentelement.embedded.content

import androidx.annotation.VisibleForTesting
import com.stripe.android.CardBrandFilter
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal interface ExpressCheckoutInteractor {
    val state: StateFlow<State>

    data class State(
        val walletsState: WalletsState?,
        val cardBrandFilter: CardBrandFilter?,
        val selection: EmbeddedPaymentElement.ExpressCheckoutType?,
        val isProcessing: Boolean,
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class DefaultExpressCheckoutInteractor(
    isLinkEnabled: StateFlow<Boolean?>,
    linkEmail: StateFlow<String?>,
    confirmationState: StateFlow<EmbeddedConfirmationStateHolder.State?>,
    confirmationHandler: ConfirmationHandler,
    coroutineScope: CoroutineScope,
) : ExpressCheckoutInteractor {
    private val selection = MutableStateFlow<EmbeddedPaymentElement.ExpressCheckoutType?>(null)

    override val state: StateFlow<ExpressCheckoutInteractor.State> = combineAsStateFlow(
        confirmationState,
        isLinkEnabled,
        linkEmail,
        confirmationHandler.state,
        selection,
    ) { confirmationState, isLinkAvailable, linkEmail, confirmationHandlerState, selection ->
        ExpressCheckoutInteractor.State(
            walletsState = confirmationState?.run {
                WalletsState.create(
                    isLinkAvailable = isLinkAvailable?.takeIf {
                        configuration.expressCheckoutTypes.contains(EmbeddedPaymentElement.ExpressCheckoutType.Link)
                    },
                    linkEmail = linkEmail,
                    isGooglePayReady = paymentMethodMetadata.isGooglePayReady &&
                        configuration.expressCheckoutTypes.contains(
                            EmbeddedPaymentElement.ExpressCheckoutType.GooglePay
                        ),
                    buttonsEnabled = confirmationHandlerState !is ConfirmationHandler.State.Confirming,
                    paymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes(),
                    googlePayLauncherConfig = googlePayLauncherConfig(
                        configuration = configuration,
                        initializationMode = initializationMode,
                    ),
                    googlePayButtonType = googlePayButtonType(configuration),
                    onGooglePayPressed = {
                        this@DefaultExpressCheckoutInteractor.selection.value =
                            EmbeddedPaymentElement.ExpressCheckoutType.GooglePay

                        confirmationArgs(PaymentSelection.GooglePay, confirmationState)?.let {
                            coroutineScope.launch {
                                confirmationHandler.start(it)
                            }
                        }
                    },
                    onLinkPressed = {
                        this@DefaultExpressCheckoutInteractor.selection.value =
                            EmbeddedPaymentElement.ExpressCheckoutType.Link

                        confirmationArgs(PaymentSelection.Link(useLinkExpress = false), confirmationState)?.let {
                            coroutineScope.launch {
                                confirmationHandler.start(it)
                            }
                        }
                    },
                    isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent
                )
            },
            selection = selection,
            cardBrandFilter = confirmationState?.run {
                paymentMethodMetadata.cardBrandFilter
            },
            isProcessing = confirmationHandlerState is ConfirmationHandler.State.Confirming,
        )
    }

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest {
                if (it is ConfirmationHandler.State.Complete) {
                    selection.value = null
                }
            }
        }
    }

    private fun googlePayButtonType(
        configuration: EmbeddedPaymentElement.Configuration,
    ): GooglePayButtonType =
        when (configuration.googlePay?.buttonType) {
            PaymentSheet.GooglePayConfiguration.ButtonType.Buy -> GooglePayButtonType.Buy
            PaymentSheet.GooglePayConfiguration.ButtonType.Book -> GooglePayButtonType.Book
            PaymentSheet.GooglePayConfiguration.ButtonType.Checkout -> GooglePayButtonType.Checkout
            PaymentSheet.GooglePayConfiguration.ButtonType.Donate -> GooglePayButtonType.Donate
            PaymentSheet.GooglePayConfiguration.ButtonType.Order -> GooglePayButtonType.Order
            PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe -> GooglePayButtonType.Subscribe
            PaymentSheet.GooglePayConfiguration.ButtonType.Plain -> GooglePayButtonType.Plain
            PaymentSheet.GooglePayConfiguration.ButtonType.Pay,
            null -> GooglePayButtonType.Pay
        }

    @VisibleForTesting
    private fun googlePayLauncherConfig(
        configuration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): GooglePayPaymentMethodLauncher.Config? =
        configuration.googlePay?.let { config ->
            if (config.currencyCode == null && !initializationMode.isProcessingPayment) {
                null
            } else {
                GooglePayPaymentMethodLauncher.Config(
                    environment = when (config.environment) {
                        PaymentSheet.GooglePayConfiguration.Environment.Production ->
                            GooglePayEnvironment.Production
                        else ->
                            GooglePayEnvironment.Test
                    },
                    merchantCountryCode = config.countryCode,
                    merchantName = configuration.merchantDisplayName,
                    isEmailRequired = configuration.billingDetailsCollectionConfiguration.collectsEmail,
                    billingAddressConfig = configuration.billingDetailsCollectionConfiguration
                        .toBillingAddressConfig(),
                )
            }
        }

    private fun confirmationArgs(
        selection: PaymentSelection,
        confirmationState: EmbeddedConfirmationStateHolder.State?,
    ): ConfirmationHandler.Args? {
        if (confirmationState == null) {
            return null
        }

        val confirmationOption = selection.toConfirmationOption(
            configuration = confirmationState.configuration.asCommonConfiguration(),
            linkConfiguration = confirmationState.paymentMethodMetadata.linkState?.configuration,
        ) ?: return null

        return ConfirmationHandler.Args(
            intent = confirmationState.paymentMethodMetadata.stripeIntent,
            confirmationOption = confirmationOption,
            initializationMode = confirmationState.initializationMode,
            appearance = confirmationState.configuration.appearance,
            shippingDetails = confirmationState.configuration.shippingDetails,
        )
    }
}

private val PaymentElementLoader.InitializationMode.isProcessingPayment: Boolean
    get() = when (this) {
        is PaymentElementLoader.InitializationMode.PaymentIntent -> true
        is PaymentElementLoader.InitializationMode.SetupIntent -> false
        is PaymentElementLoader.InitializationMode.DeferredIntent -> {
            intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
        }
    }