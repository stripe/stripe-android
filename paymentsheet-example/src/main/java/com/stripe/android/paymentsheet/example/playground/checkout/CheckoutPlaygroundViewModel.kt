package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.checkout.Address
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPlaygroundViewModel(
    application: Application,
    checkoutState: Checkout.State,
) : AndroidViewModel(application) {
    val checkout: Checkout = Checkout.createWithState(
        context = application,
        state = checkoutState,
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun applyPromotionCode(promotionCode: String) = performWhileLoading {
        checkout.applyPromotionCode(promotionCode)
    }

    fun updateLineItemQuantity(lineItemId: String, quantity: Int) = performWhileLoading {
        checkout.updateLineItemQuantity(lineItemId, quantity)
    }

    fun removePromotionCode() = performWhileLoading {
        checkout.removePromotionCode()
    }

    fun selectShippingRate(shippingRateId: String) = performWhileLoading {
        checkout.selectShippingRate(shippingRateId)
    }

    fun updatePostalCode(postalCode: String) = performWhileLoading {
        val address = Address().postalCode(postalCode).country("US")
        checkout.updateShippingAddress(address)
    }

    fun refresh() = performWhileLoading {
        checkout.refresh()
    }

    private fun performWhileLoading(block: suspend () -> Result<CheckoutSession>) {
        viewModelScope.launch {
            _isLoading.value = true
            block().onFailure { exception ->
                Log.e("CheckoutPlaygroundViewModel", "Failed to perform request", exception)
                _errorMessage.value = exception.message
            }
            _isLoading.value = false
        }
    }

    companion object {
        const val CHECKOUT_STATE_KEY = "CHECKOUT_STATE_KEY"

        fun factory(checkoutState: Checkout.State) = viewModelFactory {
            initializer {
                CheckoutPlaygroundViewModel(
                    application = this[APPLICATION_KEY] as Application,
                    checkoutState = checkoutState,
                )
            }
        }
    }
}
