package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.checkout.Address
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPlaygroundViewModel(
    val checkout: Checkout,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private val _lastAddressDetails = MutableStateFlow<AddressDetails?>(null)
    val lastAddressDetails: StateFlow<AddressDetails?> = _lastAddressDetails.asStateFlow()

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

    fun updateShippingAddress(addressDetails: AddressDetails) = performWhileLoading {
        val country = addressDetails.address?.country
            ?: return@performWhileLoading Result.failure(IllegalStateException("Country is required"))
        val address = Address()
            .city(addressDetails.address?.city)
            .country(country)
            .line1(addressDetails.address?.line1)
            .line2(addressDetails.address?.line2)
            .postalCode(addressDetails.address?.postalCode)
            .state(addressDetails.address?.state)
        checkout.updateShippingAddress(address).also {
            _lastAddressDetails.value = addressDetails
        }
    }

    fun updatePostalCode(postalCode: String) = performWhileLoading {
        val address = Address().postalCode(postalCode).country("US")
        checkout.updateShippingAddress(address)
    }

    fun clearShippingAddress() = performWhileLoading {
        checkout.updateShippingAddress(Address().country("US")).also {
            _lastAddressDetails.value = null
        }
    }

    fun refresh() = performWhileLoading {
        checkout.refresh()
    }

    private fun performWhileLoading(block: suspend () -> Result<CheckoutSession>) {
        viewModelScope.launch {
            _isLoading.value = true
            block().onSuccess {
                savedStateHandle[CHECKOUT_STATE_KEY] = checkout.state
            }.onFailure { exception ->
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
                val savedStateHandle = createSavedStateHandle()
                val restoredState = savedStateHandle.get<Checkout.State>(CHECKOUT_STATE_KEY)
                    ?: checkoutState
                CheckoutPlaygroundViewModel(
                    checkout = Checkout.createWithState(
                        context = this[APPLICATION_KEY] as Application,
                        state = restoredState,
                    ),
                    savedStateHandle = savedStateHandle,
                )
            }
        }
    }
}
