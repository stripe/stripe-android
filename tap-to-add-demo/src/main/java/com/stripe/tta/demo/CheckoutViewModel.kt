package com.stripe.tta.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.tta.demo.catalog.MockCatalog
import com.stripe.tta.demo.network.CheckoutRequester
import com.stripe.tta.demo.network.CreateSetupIntentRequester
import com.stripe.tta.demo.network.model.CheckoutRequest
import com.stripe.tta.demo.network.model.CheckoutResponse
import com.stripe.tta.demo.network.model.CreateSetupIntentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

internal enum class IntegrationMode {
    PaymentSheet,
    FlowController,
    Embedded,
}

internal sealed interface CheckoutLoadState {
    data object Loading : CheckoutLoadState
    data class Ready(
        val checkout: CheckoutResponse,
    ) : CheckoutLoadState

    data class Failed(
        val message: String,
    ) : CheckoutLoadState
}

@OptIn(TapToAddPreview::class)
internal class CheckoutViewModel(
    application: Application,
) : AndroidViewModel(application),
    CreateIntentWithConfirmationTokenCallback,
    CreateCardPresentSetupIntentCallback {

    private val ioDispatcher = Dispatchers.IO
    private val checkoutRequester = CheckoutRequester(application.applicationContext, ioDispatcher)
    private val createSetupIntentRequester = CreateSetupIntentRequester(ioDispatcher)

    private val _loadState = MutableStateFlow<CheckoutLoadState>(CheckoutLoadState.Loading)
    val loadState: StateFlow<CheckoutLoadState> = _loadState.asStateFlow()

    private val _navigateToSummary = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToSummary: SharedFlow<Unit> = _navigateToSummary.asSharedFlow()

    private val _completedOrderTotal = MutableStateFlow<String?>(null)
    val completedOrderTotal: StateFlow<String?> = _completedOrderTotal.asStateFlow()

    private val _paymentErrorMessage = MutableStateFlow<String?>(null)
    val paymentErrorMessage: StateFlow<String?> = _paymentErrorMessage.asStateFlow()

    private val _integrationMode = MutableStateFlow(IntegrationMode.PaymentSheet)
    val integrationMode: StateFlow<IntegrationMode> = _integrationMode.asStateFlow()

    private val _flowControllerReady = MutableStateFlow(false)
    val flowControllerReady: StateFlow<Boolean> = _flowControllerReady.asStateFlow()

    private val _flowControllerPaymentOption = MutableStateFlow<PaymentOption?>(null)
    val flowControllerPaymentOption: StateFlow<PaymentOption?> =
        _flowControllerPaymentOption.asStateFlow()

    private val _embeddedReady = MutableStateFlow(false)
    val embeddedReady: StateFlow<Boolean> = _embeddedReady.asStateFlow()

    private val _flowControllerConfirmInProgress = MutableStateFlow(false)
    val flowControllerConfirmInProgress: StateFlow<Boolean> =
        _flowControllerConfirmInProgress.asStateFlow()

    private val _embeddedConfirmInProgress = MutableStateFlow(false)
    val embeddedConfirmInProgress: StateFlow<Boolean> = _embeddedConfirmInProgress.asStateFlow()

    private var customerId: String? = null
    private var activeCheckout: CheckoutResponse? = null

    private val _cartQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cartQuantities: StateFlow<Map<String, Int>> = _cartQuantities.asStateFlow()

    fun cartSubtotalCents(): Long = MockCatalog.subtotalCents(_cartQuantities.value)

    init {
        FeatureFlags.nativeLinkEnabled.setEnabled(true)
        FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)
    }

    fun incrementQuantity(productId: String) {
        val next = _cartQuantities.value.toMutableMap()
        next[productId] = (next[productId] ?: 0) + 1
        _cartQuantities.value = next
    }

    fun decrementQuantity(productId: String) {
        val next = _cartQuantities.value.toMutableMap()
        val current = next[productId] ?: 0
        if (current <= 1) {
            next.remove(productId)
        } else {
            next[productId] = current - 1
        }
        _cartQuantities.value = next
    }

    fun clearCart() {
        _cartQuantities.value = emptyMap()
    }

    fun loadCheckout() {
        _loadState.value = CheckoutLoadState.Loading
        viewModelScope.launch(ioDispatcher) {
            getCheckout().fold(
                onSuccess = { response ->
                    activeCheckout = response
                    _loadState.value = CheckoutLoadState.Ready(response)
                },
                onFailure = { error ->
                    _loadState.value = CheckoutLoadState.Failed(
                        error.message ?: "Could not load checkout",
                    )
                },
            )
        }
    }

    fun reset() {
        customerId = null
        PaymentSheet.resetCustomer(application)
        restart()
    }

    /**
     * After a successful purchase: clear the cart, reset payment state, and return the user to the
     * catalog without starting a new checkout request until they open checkout again.
     */
    fun afterSuccessfulCheckoutShopAgain() {
        clearCart()
        customerId = null
        activeCheckout = null
        _loadState.value = CheckoutLoadState.Loading
        _completedOrderTotal.value = null
        _paymentErrorMessage.value = null
        _flowControllerReady.value = false
        _flowControllerPaymentOption.value = null
        _embeddedReady.value = false
        _flowControllerConfirmInProgress.value = false
        _embeddedConfirmInProgress.value = false
        PaymentSheet.resetCustomer(application)
    }

    fun restart() {
        _completedOrderTotal.value = null
        _paymentErrorMessage.value = null
        _flowControllerReady.value = false
        _flowControllerPaymentOption.value = null
        _embeddedReady.value = false
        _flowControllerConfirmInProgress.value = false
        _embeddedConfirmInProgress.value = false
        PaymentSheet.resetCustomer(application)
        loadCheckout()
    }

    fun setIntegrationMode(mode: IntegrationMode) {
        _integrationMode.value = mode
        _flowControllerReady.value = false
        _flowControllerPaymentOption.value = null
        _embeddedReady.value = false
        _flowControllerConfirmInProgress.value = false
        _embeddedConfirmInProgress.value = false
        _paymentErrorMessage.value = null
    }

    fun onPaymentOption(paymentOption: PaymentOption?) {
        _flowControllerPaymentOption.value = paymentOption
    }

    fun onFlowControllerConfigureResult(success: Boolean, error: Throwable?) {
        _flowControllerReady.value = success
        if (success) {
            _paymentErrorMessage.value = null
        } else if (error != null) {
            _paymentErrorMessage.value = error.message ?: "Could not configure payment"
        }
    }

    fun clearPaymentError() {
        _paymentErrorMessage.value = null
    }

    fun paymentConfiguration(checkout: CheckoutResponse): PaymentSheet.Configuration {
        val customer = checkout.makeCustomerConfig(CheckoutRequest.CustomerKeyType.CustomerSession)
        return PaymentSheet.Configuration.Builder(merchantDisplayName = "Stripe")
            .customer(customer)
            .build()
    }

    fun embeddedConfiguration(checkout: CheckoutResponse): EmbeddedPaymentElement.Configuration {
        val customer = checkout.makeCustomerConfig(CheckoutRequest.CustomerKeyType.CustomerSession)
        return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName = "Stripe")
            .customer(customer)
            .build()
    }

    fun onEmbeddedConfigurationStarted() {
        _embeddedReady.value = false
    }

    fun onEmbeddedConfigureResult(result: EmbeddedPaymentElement.ConfigureResult) {
        when (result) {
            is EmbeddedPaymentElement.ConfigureResult.Succeeded -> {
                _embeddedReady.value = true
                _paymentErrorMessage.value = null
            }
            is EmbeddedPaymentElement.ConfigureResult.Failed -> {
                _embeddedReady.value = false
                _paymentErrorMessage.value = result.error.message
                    ?: "Could not set up payment"
            }
        }
    }

    fun onFlowControllerConfirmStarted() {
        _flowControllerConfirmInProgress.value = true
    }

    fun onEmbeddedConfirmStarted() {
        _embeddedConfirmInProgress.value = true
    }

    fun intentConfiguration(checkout: CheckoutResponse): PaymentSheet.IntentConfiguration {
        val amount = checkout.amount ?: cartSubtotalCents()
        val currency = "usd"
        return PaymentSheet.IntentConfiguration(
            paymentMethodTypes = checkout.paymentMethodTypes.orEmpty().split(","),
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = amount,
                currency = currency,
            ),
        )
    }

    fun onPaymentSheetResult(result: PaymentSheetResult) {
        _flowControllerConfirmInProgress.value = false
        when (result) {
            is PaymentSheetResult.Completed -> {
                val formatted = MockCatalog.formatUsd(
                    activeCheckout?.amount ?: cartSubtotalCents(),
                )
                _completedOrderTotal.value = formatted
                _navigateToSummary.tryEmit(Unit)
            }

            is PaymentSheetResult.Canceled -> Unit

            is PaymentSheetResult.Failed -> {
                _paymentErrorMessage.value = result.error.message ?: "Payment failed"
            }
        }
    }

    fun onEmbeddedPaymentResult(result: EmbeddedPaymentElement.Result) {
        _embeddedConfirmInProgress.value = false
        when (result) {
            is EmbeddedPaymentElement.Result.Completed -> {
                val formatted = MockCatalog.formatUsd(
                    activeCheckout?.amount ?: cartSubtotalCents(),
                )
                _completedOrderTotal.value = formatted
                _navigateToSummary.tryEmit(Unit)
            }

            is EmbeddedPaymentElement.Result.Canceled -> Unit

            is EmbeddedPaymentElement.Result.Failed -> {
                _paymentErrorMessage.value = result.error.message ?: "Payment failed"
            }
        }
    }

    override suspend fun onCreateIntent(confirmationToken: ConfirmationToken): CreateIntentResult {
        return getCheckout(activeCheckout).fold(
            onSuccess = { response -> CreateIntentResult.Success(response.clientSecret) },
            onFailure = { error ->
                CreateIntentResult.Failure(
                    cause = Exception(error),
                    displayMessage = error.message,
                )
            },
        )
    }

    override suspend fun createCardPresentSetupIntent(): CreateIntentResult {
        val checkout = activeCheckout
            ?: return CreateIntentResult.Failure(
                cause = IllegalStateException("Checkout not loaded"),
                displayMessage = "Checkout is not ready. Try again.",
            )

        val customerId = checkout.customerId
            ?: return CreateIntentResult.Failure(
                cause = IllegalStateException("Missing customer"),
                displayMessage = "Tap to add requires a customer.",
            )

        return createSetupIntentRequester.fetch(
            CreateSetupIntentRequest(
                customerId = customerId,
                merchantCountryCode = MERCHANT_COUNTRY,
                paymentMethodTypes = listOf("card_present"),
            ),
        ).fold(
            onSuccess = { response -> CreateIntentResult.Success(response.clientSecret) },
            onFailure = { error ->
                CreateIntentResult.Failure(
                    cause = Exception(error),
                    displayMessage = error.message,
                )
            },
        )
    }

    private suspend fun getCheckout(
        prevCheckoutResponse: CheckoutResponse? = null,
    ): Result<CheckoutResponse> {
        val request = CheckoutRequest.Builder()
            .initialization("Deferred SSC")
            .isConfirmationToken(false)
            .allowsTapToAdd(true)
            .automaticPaymentMethods(false)
            .customer(prevCheckoutResponse?.customerId ?: customerId ?:  "new")
            .apply {
                if (prevCheckoutResponse?.customerId == null) {
                    customerEmail(uniqueDemoCustomerEmail())
                }
            }
            .customerKeyType(CheckoutRequest.CustomerKeyType.CustomerSession)
            .supportedPaymentMethods(listOf("card"))
            .merchant("US")
            .mode(PAYMENT_MODE)
            .currency("usd")
            .amount(cartSubtotalCents())
            .build()

        return checkoutRequester.fetch(request).onSuccess {
            customerId = it.customerId
        }
    }

    private companion object {
        const val MERCHANT_COUNTRY = "US"
        const val PAYMENT_MODE = "payment"

        private val demoFirstNames = listOf(
            "alex", "blake", "casey", "drew", "eden", "finley", "gray", "harper", "indigo", "jordan",
            "kai", "logan", "morgan", "noah", "oakley", "parker", "quinn", "riley", "rowan", "sage",
            "taylor", "avery", "reese", "skylar", "james", "sam", "cameron", "dakota", "emery", "frankie",
            "jules", "kendall", "london", "max", "marlowe", "nico", "olive", "phoenix", "regan", "sloane",
            "ellis", "adrian", "blair", "charlie", "devin", "elliot", "remy", "sydney", "tatum", "wren",
        )
        private val demoLastNames = listOf(
            "rivera", "patel", "nguyen", "collins", "murphy", "foster", "reed", "hayes", "brooks", "ellis",
            "chen", "kim", "wright", "lopez", "ross", "cole", "west", "shaw", "park", "grant",
            "sullivan", "bennett", "scott", "price", "watson", "coleman", "ramirez", "griffin", "porter",
            "freeman", "chavez", "owens", "silva", "meyer", "boyd", "hansen", "hoffman", "weaver",
            "gardner", "stephens", "payne", "long", "bishop", "oliver", "barrett", "briggs", "manning",
            "walters", "daniels", "chapman", "vaughn",
        )

        private fun uniqueDemoCustomerEmail(): String {
            val first = demoFirstNames[Random.nextInt(demoFirstNames.size)]
            val last = demoLastNames[Random.nextInt(demoLastNames.size)]
            val digits = Random.nextInt(10_000)
            return "$first.$last.${digits.toString().padStart(4, '0')}@email.com"
        }
    }
}
