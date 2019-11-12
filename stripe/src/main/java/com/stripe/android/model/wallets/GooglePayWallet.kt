package com.stripe.android.model.wallets

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GooglePayWallet internal constructor(
    val dynamicLast4: String?
) : Wallet(Type.GooglePay), Parcelable {
    internal class Builder : Wallet.Builder<GooglePayWallet>() {
        override fun build(): GooglePayWallet {
            return GooglePayWallet(dynamicLast4)
        }
    }

    internal companion object {
        internal fun fromJson(): Builder {
            return Builder()
        }
    }
}
