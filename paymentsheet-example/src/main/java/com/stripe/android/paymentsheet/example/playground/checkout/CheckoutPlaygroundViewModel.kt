package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
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

    fun applyPromotionCode(promotionCode: String) {
        viewModelScope.launch {
            checkout.applyPromotionCode(promotionCode)
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
