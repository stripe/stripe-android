package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface PrefsRepository {
    suspend fun getDefaultPaymentMethodId(): String?
    fun savePaymentSelection(paymentSelection: PaymentSelection?)

    class Noop : PrefsRepository {
        override suspend fun getDefaultPaymentMethodId(): String? = null
        override fun savePaymentSelection(paymentSelection: PaymentSelection?) {}
    }
}
