package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPlaygroundViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    val promotionCode: StateFlow<String> = savedStateHandle.getStateFlow(PROMOTION_CODE_KEY, "")

    val checkout: Checkout = Checkout.createWithState(
        context = application,
        state = requireNotNull(savedStateHandle.get<Checkout.State>(CHECKOUT_STATE_KEY)),
    )

    fun setPromotionCode(code: String) {
        savedStateHandle[PROMOTION_CODE_KEY] = code
    }

    fun applyPromotionCode() {
        viewModelScope.launch {
            checkout.applyPromotionCode(promotionCode.value)
            savedStateHandle[CHECKOUT_STATE_KEY] = checkout.state
        }
    }

    companion object {
        const val CHECKOUT_STATE_KEY = "CHECKOUT_STATE_KEY"
        private const val PROMOTION_CODE_KEY = "PROMOTION_CODE_KEY"
    }
}
