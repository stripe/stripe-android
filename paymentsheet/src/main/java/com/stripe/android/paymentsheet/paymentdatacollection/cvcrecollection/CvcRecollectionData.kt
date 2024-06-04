package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal data class CvcRecollectionData(
    val lastFour: String?,
    val brand: CardBrand
) {
    companion object {
        fun fromPaymentSelection(
            paymentSelection: PaymentMethod.Card?
        ): CvcRecollectionData? {
            return if (paymentSelection != null) {
                CvcRecollectionData(paymentSelection.last4, paymentSelection.brand)
            } else {
                null
            }
        }
    }
}
