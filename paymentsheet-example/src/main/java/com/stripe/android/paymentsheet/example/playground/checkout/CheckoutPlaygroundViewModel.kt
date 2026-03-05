package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.checkout.Checkout
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

    fun applyPromotionCode(promotionCode: String) = performWhileLoading {
        checkout.applyPromotionCode(promotionCode)
    }

    fun removePromotionCode() = performWhileLoading {
        checkout.removePromotionCode()
    }

    private fun performWhileLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            block()
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
