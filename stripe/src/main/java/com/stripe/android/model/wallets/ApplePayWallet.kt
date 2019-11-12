package com.stripe.android.model.wallets

import kotlinx.android.parcel.Parcelize

@Parcelize
data class ApplePayWallet internal constructor(
    val dynamicLast4: String?
) : Wallet(Type.ApplePay) {

    internal class Builder : Wallet.Builder<ApplePayWallet>() {
        override fun build(): ApplePayWallet {
            return ApplePayWallet(dynamicLast4)
        }
    }

    internal companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }
    }
}
