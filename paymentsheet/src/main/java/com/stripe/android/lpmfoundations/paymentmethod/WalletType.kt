package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.ElementsSession

internal enum class WalletType(val code: String) {
    GooglePay(code = "google_pay"),
    Link(code = "link");

    companion object {
        fun listFrom(elementsSession: ElementsSession): List<WalletType> {
            return WalletType.entries.filter {
                elementsSession.orderedPaymentMethodTypesAndWallets.contains(it.code)
            }
        }
    }
}
