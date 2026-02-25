package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.twostepcheckout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.paymentsheet.example.samples.model.CartProduct
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

internal class TwoStepCheckoutViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val hotDogCartState = CartState(
        products = listOf(CartProduct.hotDog),
        isSubscription = false,
        subtotal = 99,
        salesTax = 8,
        total = 107,
    )

    private val _state = MutableStateFlow(
        value = TwoStepCheckoutViewState(cartState = hotDogCartState),
    )
    val state: StateFlow<TwoStepCheckoutViewState> = _state

    init {
        FeatureFlags.nativeLinkEnabled.setEnabled(true)
        FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)
        FeatureFlags.showInlineOtpInWalletButtons.setEnabled(true)
    }

    fun statusDisplayed() {
        _state.update {
            it.copy(status = null)
        }
    }

    fun retry() {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }
    }

    fun handleFlowControllerConfigured(success: Boolean, error: Throwable?) {
        _state.update {
            it.copy(
                isProcessing = false,
                isFlowControllerConfigured = success,
                status = error?.let { e -> "Failed to configure\n$e" },
            )
        }
    }

    fun handlePaymentOptionChanged(paymentOption: PaymentOption?) {
        viewModelScope.launch {
            _state.update { currentState ->
                val newShippingAddress = currentState.shippingAddress
                    ?: paymentOption?.let { convertBillingDetailsToAddressDetails(it.billingDetails) }

                currentState.copy(
                    paymentOption = paymentOption,
                    showFinalCheckout = paymentOption != null,
                    shippingAddress = newShippingAddress
                )
            }
        }
    }

    fun handlePaymentSheetResult(paymentResult: PaymentSheetResult) {
        val status = when (paymentResult) {
            is PaymentSheetResult.Canceled -> null
            is PaymentSheetResult.Completed -> "Payment completed successfully!"
            is PaymentSheetResult.Failed -> paymentResult.error.message
        }

        _state.update {
            it.copy(
                isProcessing = false,
                status = status,
                didComplete = paymentResult is PaymentSheetResult.Completed,
                // Reset to payment method selection if payment was canceled or failed
                showFinalCheckout = paymentResult is PaymentSheetResult.Completed,
                paymentOption = if (paymentResult is PaymentSheetResult.Completed) it.paymentOption else null
            )
        }
    }

    private suspend fun prepareCheckout() {
        val currentState = _state.updateAndGet {
            it.copy(isProcessing = true, isError = false)
        }

        val request = ExampleCheckoutRequest(
            hotDogCount = currentState.cartState.countOf(CartProduct.Id.HotDog),
            saladCount = currentState.cartState.countOf(CartProduct.Id.Salad),
            isSubscribing = currentState.cartState.isSubscription
        )
        val requestBody = Json.encodeToString(ExampleCheckoutRequest.serializer(), request)

        val apiResult = Fuel
            .post("$backendUrl/checkout")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleCheckoutResponse.serializer())

        when (apiResult) {
            is ApiResult.Success -> {
                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = apiResult.value.publishableKey,
                )

                val paymentInfo = TwoStepCheckoutViewState.PaymentInfo(
                    clientSecret = apiResult.value.paymentIntent,
                    customerConfiguration = apiResult.value.makeCustomerConfig(),
                    merchantDisplayName = "Hot Dog Stand",
                    customerEmail = currentState.customerEmail.takeIf { it.isNotBlank() },
                )

                _state.update {
                    it.copy(paymentInfo = paymentInfo)
                }
            }
            is ApiResult.Failure -> {
                _state.update {
                    it.copy(isProcessing = false, isError = true)
                }
            }
        }
    }

    fun handleBuyButtonPressed() {
        _state.update {
            it.copy(isProcessing = true)
        }
    }

    fun handleAddressLauncherResult(result: AddressLauncherResult) {
        when (result) {
            is AddressLauncherResult.Succeeded -> {
                _state.update {
                    it.copy(shippingAddress = result.address)
                }
            }
            is AddressLauncherResult.Canceled -> {
                // User canceled, no action needed
            }
        }
    }

    fun updateCustomerEmail(email: String) {
        _state.update {
            it.copy(customerEmail = email)
        }
    }

    fun completeConfiguration() {
        _state.update { it.copy(showConfiguration = false) }

        // Now prepare checkout with the configured settings
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }
    }

    private fun convertBillingDetailsToAddressDetails(billingDetails: PaymentSheet.BillingDetails?): AddressDetails? {
        return billingDetails?.let { billing ->
            AddressDetails(
                name = billing.name,
                address = billing.address,
                phoneNumber = billing.phone,
                isCheckboxSelected = null
            )
        }
    }

    companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet.stripedemos.com"
    }
}
