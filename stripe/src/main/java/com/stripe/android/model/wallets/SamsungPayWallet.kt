package com.stripe.android.model.wallets

import kotlinx.android.parcel.Parcelize

@Parcelize
data class SamsungPayWallet internal constructor(
    val dynamicLast4: String?
) : Wallet(Type.SamsungPay) {
    internal class Builder : Wallet.Builder<SamsungPayWallet>() {
        override fun build(): SamsungPayWallet {
            return SamsungPayWallet(dynamicLast4)
        }
    }

    companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }
    }
}
