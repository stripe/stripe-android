package com.stripe.android.model.wallets

import kotlinx.android.parcel.Parcelize

@Parcelize
data class AmexExpressCheckoutWallet internal constructor(
    val dynamicLast4: String?
) : Wallet(Type.AmexExpressCheckout) {

    internal class Builder : Wallet.Builder<AmexExpressCheckoutWallet>() {
        override fun build(): AmexExpressCheckoutWallet {
            return AmexExpressCheckoutWallet(dynamicLast4)
        }
    }

    internal companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }
    }
}
