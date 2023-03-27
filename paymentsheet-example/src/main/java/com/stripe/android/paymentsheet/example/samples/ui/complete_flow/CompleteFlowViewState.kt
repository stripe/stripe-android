package com.stripe.android.paymentsheet.example.samples.ui.complete_flow

import android.graphics.drawable.Drawable
import com.stripe.android.paymentsheet.example.samples.model.CartState

data class CompleteFlowViewState(
    val isProcessing: Boolean = false,
    val cartState: CartState = CartState.default,
    val status: String? = null,
    val didComplete: Boolean = false,
)
