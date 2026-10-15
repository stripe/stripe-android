package com.stripe.android.elements.ece

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.StandaloneExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.reportPaymentResult
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class StandaloneExpressCheckoutElementController @Inject constructor(
    private val loader: PaymentElementLoader,
    val confirmationHandler: ConfirmationHandler,
    private val savedStateHandle: SavedStateHandle,
    private val buttonTypesFactory: DefaultAvailableExpressButtonTypesFactory,
    private val eventReporter: EventReporter,
    @PaymentElementCallbackIdentifier private val callbackIdentifier: String,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : ExpressCheckoutElementIntegration {
    private val loadedState = savedStateHandle.getStateFlow<LoadedState?>(LOADED_STATE_KEY, null)
    private val isBusy = MutableStateFlow(confirmationHandler.state.value is ConfirmationHandler.State.Confirming)
    private var contentDisplayed = false
    private val results = Channel<PaymentSheetResult>(Channel.BUFFERED)
    val result = results.receiveAsFlow()

    override val state = combineAsStateFlow(loadedState, isBusy) { loaded, busy ->
        ExpressCheckoutElementIntegration.State(
            content = loaded?.let {
                ExpressCheckoutElementIntegration.Content(
                    configuration = it.elementConfiguration,
                    paymentMethodMetadata = it.paymentMethodMetadata,
                    availableExpressButtonTypes = buttonTypesFactory.create(
                        paymentMethodMetadata = it.paymentMethodMetadata,
                        expressCheckoutElementConfiguration = it.elementConfiguration,
                        requiresShippingAddress = it.shippingAddressRequired,
                    ),
                    requiresShippingAddress = it.shippingAddressRequired,
                )
            },
            enabled = loaded != null && !busy,
        )
    }

    init {
        coroutineScope.launch {
            confirmationHandler.state.collect { confirmationState ->
                when (confirmationState) {
                    is ConfirmationHandler.State.Complete -> {
                        isBusy.value = false
                        eventReporter.reportPaymentResult(confirmationState.result, selection)
                        results.send(
                            when (val result = confirmationState.result) {
                                is ConfirmationHandler.Result.Succeeded -> PaymentSheetResult.Completed()
                                is ConfirmationHandler.Result.Canceled -> PaymentSheetResult.Canceled()
                                is ConfirmationHandler.Result.Failed -> PaymentSheetResult.Failed(result.cause)
                            }
                        )
                    }
                    is ConfirmationHandler.State.Confirming -> isBusy.value = true
                    ConfirmationHandler.State.Idle -> Unit
                }
            }
        }
    }

    suspend fun configure(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: StandaloneExpressCheckoutElement.Configuration,
    ): Result<Unit> = withContext(Dispatchers.Main.immediate) {
        if (isBusy.value) {
            return@withContext Result.failure(IllegalStateException("StandaloneExpressCheckoutElement is busy."))
        }
        isBusy.value = true
        savedStateHandle[LOADED_STATE_KEY] = null
        try {
            initializationMode.validate()
            if (initializationMode is PaymentElementLoader.InitializationMode.DeferredIntent) {
                require(PaymentElementCallbackReferences.getExact(callbackIdentifier)?.createIntentCallback != null) {
                    "A createIntentCallback is required for deferred intent configuration."
                }
            }
            val elementState = configuration.elementConfiguration.build()
            val elementConfiguration = elementState.copy(
                googlePayConfiguration = elementState.googlePayConfiguration.copy(
                    additionalEnabledNetworks = (
                        elementState.googlePayConfiguration.additionalEnabledNetworks +
                            configuration.googlePay?.additionalEnabledNetworks.orEmpty()
                        ).distinct(),
                ),
            )
            val commonConfiguration = configuration.toCommonConfiguration(elementConfiguration)
            val shippingAddressParameters = configuration.shippingAddressParameters
            val shippingAddressRequired = configuration.shippingAddressRequired
            val loaded = loader.load(
                initializationMode = initializationMode,
                integrationConfiguration = PaymentElementLoader.Configuration.ExpressCheckoutElement(
                    commonConfiguration = commonConfiguration,
                ),
                metadata = PaymentElementLoader.Metadata(
                    isReloadingAfterProcessDeath = false,
                    initializedViaCompose = initializedViaCompose,
                ),
            ).getOrThrow()
            loaded.validationError?.let { throw it }
            savedStateHandle[LOADED_STATE_KEY] = LoadedState(
                commonConfiguration = loaded.config,
                elementConfiguration = elementConfiguration,
                paymentMethodMetadata = loaded.paymentMethodMetadata,
                shippingAddressParameters = shippingAddressParameters,
                shippingAddressRequired = shippingAddressRequired,
            )
            savedStateHandle[DISPLAYED_KEY] = false
            confirmationHandler.bootstrap(loaded.paymentMethodMetadata)
            if (contentDisplayed) reportDisplayed()
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Result.failure(error)
        } finally {
            isBusy.value = false
        }
    }

    var initializedViaCompose: Boolean = false

    override fun onDisplayed() {
        contentDisplayed = true
        reportDisplayed()
    }

    override fun confirm(expressButton: ExpressButton) {
        val loaded = loadedState.value ?: return
        if (isBusy.value) return
        val option = expressButton.toSelection().toConfirmationOption(
            configuration = loaded.commonConfiguration,
            linkConfiguration = loaded.paymentMethodMetadata.linkState?.configuration,
            cardFundingFilter = loaded.paymentMethodMetadata.cardFundingFilter,
            googlePayShippingAddressParameters = loaded.shippingAddressParameters,
            showSecondaryButtonForLink = false,
        ) ?: return
        isBusy.value = true
        selection = expressButton.toSelection()
        eventReporter.onWalletButtonTapped(expressButton.toWalletType().code)
        coroutineScope.launch {
            try {
                confirmationHandler.start(
                    ConfirmationHandler.Args(
                        confirmationOption = option,
                        paymentMethodMetadata = loaded.paymentMethodMetadata,
                        statusBarColor = statusBarColor,
                    )
                )
            } catch (error: CancellationException) {
                isBusy.value = false
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                isBusy.value = false
                results.send(PaymentSheetResult.Failed(error))
            }
        }
    }

    private var selection: PaymentSelection?
        get() = savedStateHandle[SELECTION_KEY]
        set(value) {
            savedStateHandle[SELECTION_KEY] = value
        }

    private fun reportDisplayed() {
        if (loadedState.value == null) return
        if (savedStateHandle.get<Boolean>(DISPLAYED_KEY) == true) return
        savedStateHandle[DISPLAYED_KEY] = true
        eventReporter.onAnalyticsEvent(object : AnalyticsEvent {
            override val eventName = "elements.express_checkout_element.init"
        })
    }

    fun paymentMethodMetadata(): PaymentMethodMetadata? = loadedState.value?.paymentMethodMetadata

    @Parcelize
    private data class LoadedState(
        val commonConfiguration: CommonConfiguration,
        val elementConfiguration: ExpressCheckoutElement.Configuration.State,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val shippingAddressParameters: GooglePayJsonFactory.ShippingAddressParameters?,
        val shippingAddressRequired: Boolean,
    ) : Parcelable

    private companion object {
        const val LOADED_STATE_KEY = "ece.loaded_state"
        const val DISPLAYED_KEY = "ece.displayed"
        const val SELECTION_KEY = "ece.selection"
    }
}

private fun StandaloneExpressCheckoutElement.Configuration.toCommonConfiguration(
    elementConfiguration: ExpressCheckoutElement.Configuration.State,
): CommonConfiguration {
    val googlePayConfiguration = elementConfiguration.googlePayConfiguration
    return PaymentSheet.Configuration(
        merchantDisplayName = merchantDisplayName,
        googlePay = googlePay?.let { googlePay ->
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
        defaultBillingDetails = billingDetails,
        link = PaymentSheet.LinkConfiguration(
            display = when (elementConfiguration.linkConfiguration.display) {
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic ->
                    PaymentSheet.LinkConfiguration.Display.Automatic
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never ->
                    PaymentSheet.LinkConfiguration.Display.Never
            },
        ),
    ).asCommonConfiguration()
}
