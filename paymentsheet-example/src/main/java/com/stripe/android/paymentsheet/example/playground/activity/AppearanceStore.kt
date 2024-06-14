package com.stripe.android.paymentsheet.example.playground.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stripe.android.paymentsheet.PaymentSheet

internal object AppearanceStore {
    var state by mutableStateOf(PaymentSheet.Appearance())

    fun reset() {
        state = PaymentSheet.Appearance()
    }
}
