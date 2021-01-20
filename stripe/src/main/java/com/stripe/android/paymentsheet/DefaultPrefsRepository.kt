package com.stripe.android.paymentsheet

import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class DefaultPrefsRepository(
    private val customerId: String,
    private val paymentSessionPrefs: PaymentSessionPrefs
) : PrefsRepository {
    override suspend fun getDefaultPaymentMethodId(): String? {
        return paymentSessionPrefs.getPaymentMethodId(customerId)
    }

    override fun savePaymentSelection(paymentSelection: PaymentSelection?) {
        if (paymentSelection is PaymentSelection.Saved) {
            paymentSessionPrefs.savePaymentMethodId(customerId, paymentSelection.paymentMethod.id)
        }
    }
}
