package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.launch

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPlaygroundViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    val checkout: Checkout = Checkout.createWithState(
        context = application,
        state = requireNotNull(savedStateHandle.get<Checkout.State>(CHECKOUT_STATE_KEY)),
    )

    fun applyPromotionCode(promotionCode: String) {
        viewModelScope.launch {
            checkout.applyPromotionCode(promotionCode)
            savedStateHandle[CHECKOUT_STATE_KEY] = checkout.state
        }
    }

    companion object {
        const val CHECKOUT_STATE_KEY = "CHECKOUT_STATE_KEY"
    }
}
